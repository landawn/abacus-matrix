/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.matrix;

import java.util.NoSuchElementException;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Arrays;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.ShortIteratorEx;
import com.landawn.abacus.util.stream.ShortStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * Matrix implementation backed by a rectangular {@code short[][]}.
 *
 * <p>This type specializes {@link AbstractMatrix} for {@code short} values while keeping the data in
 * a validated backing array. The {@link #ShortMatrix(short[][]) constructor} and {@link #of(short[][]) of(...)}
 * wrap the supplied storage directly (no defensive copy), so later mutations to either the input
 * array or the matrix remain visible through the other view; call {@link #copy()} to obtain an
 * independently owned matrix. Factories, conversions, and mapping operations always allocate new arrays.</p>
 *
 * <p>Cells introduced by growth or reshaping default to {@code 0} unless an overload accepts an
 * explicit fill value.</p>
 *
 * <p><b>Short arithmetic:</b> all element-wise arithmetic ({@link #add}, {@link #subtract},
 * {@link #matmul}, and the {@code zipWith}/{@code map} variants) computes intermediate results using
 * Java's standard numeric promotion to {@code int} and stores the result back into {@code short} cells, so values
 * outside {@code [Short.MIN_VALUE, Short.MAX_VALUE]} wrap modulo 65536. {@link #add} and {@link #subtract}
 * narrow via an explicit {@code (short)} cast on the library side, and {@link #matmul} narrows via the
 * implicit narrowing of a compound assignment into the {@code short} result cell. For the {@code zipWith}
 * and {@code map} variants the supplied operator itself returns a {@code short}, so any narrowing of an
 * out-of-range result is performed by the caller's lambda. To preserve the full magnitude, widen first
 * via {@link #toIntMatrix()} or {@link #toLongMatrix()}.</p>
 *
 * @see IntMatrix
 * @see ByteMatrix
 * @see LongMatrix
 */
public final class ShortMatrix extends AbstractMatrix<short[], ShortList, ShortStream, Stream<ShortStream>, ShortMatrix> {

    static final int BOUND = Short.MAX_VALUE - Short.MIN_VALUE + 1;
    private static final ShortMatrix EMPTY_SHORT_MATRIX = new ShortMatrix(new short[0][0]);

    /**
     * Constructs a {@code ShortMatrix} backed by the supplied two-dimensional array.
     *
     * <p>If {@code a} is {@code null}, this creates an empty {@code 0x0} matrix. Otherwise the array
     * is used directly after rectangular-shape validation, so later modifications to either the input
     * array or the matrix remain visible through the other view. Call {@link #copy()} if you need an
     * independently owned matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[][] data = {{1, 2}, {3, 4}};
     * ShortMatrix matrix = new ShortMatrix(data);
     * matrix.get(0, 0);                            // returns (short) 1
     * data[0][0] = 99;                             // shared storage: matrix.get(0, 0) now returns (short) 99
     *
     * new ShortMatrix((short[][]) null).rowCount();   // returns 0 (null -> empty 0x0 matrix)
     * new ShortMatrix(new short[0][0]).columnCount(); // returns 0
     * new ShortMatrix(new short[][] {{1, 2}, {3}});   // throws IllegalArgumentException (jagged rows)
     * }</pre>
     *
     * @param a the two-dimensional short array to wrap, or {@code null} for an empty matrix
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public ShortMatrix(final short[][] a) {
        super(a == null ? new short[0][0] : a, short.class);
    }

    /**
     * Creates an empty matrix with zero rows and zero columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.empty();
     * matrix.rowCount();      // returns 0
     * matrix.columnCount();   // returns 0
     * matrix.isEmpty();       // returns true
     * matrix.elementCount();  // returns 0L
     * }</pre>
     *
     * @return an empty short matrix
     */
    public static ShortMatrix empty() {
        return EMPTY_SHORT_MATRIX;
    }

    /**
     * Creates a ShortMatrix from a two-dimensional short array.
     *
     * <p><b>Important:</b> The provided array is used directly without defensive copying.
     * Changes to the input array are reflected in the returned matrix, and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.get(0, 1);                          // returns (short) 2
     * matrix.rowCount();                         // returns 2
     *
     * ShortMatrix.of((short[][]) null).isEmpty();   // returns true (null -> empty matrix)
     * ShortMatrix.of(new short[0][0]).isEmpty();    // returns true (empty input -> empty matrix)
     * ShortMatrix.of(new short[][] {{1, 2}, {3}});  // throws IllegalArgumentException (jagged rows)
     * }</pre>
     *
     * @param a the two-dimensional short array to create the matrix from, or {@code null}/empty for an empty matrix
     * @return a new ShortMatrix containing the provided data, or an empty ShortMatrix if input is {@code null} or empty
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public static ShortMatrix of(final short[]... a) {
        return N.isEmpty(a) ? EMPTY_SHORT_MATRIX : new ShortMatrix(a);
    }

    /**
     * Creates a new {@code 1 x length} matrix filled with random short values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.random(5);
     * matrix.rowCount();             // returns 1
     * matrix.columnCount();          // returns 5 (values are random shorts)
     *
     * ShortMatrix.random(0).columnCount();   // returns 0 (empty 1x0 row)
     * ShortMatrix.random(-1);                // throws IllegalArgumentException (negative length)
     * }</pre>
     *
     * @param length the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code 1 x length} {@code ShortMatrix} filled with random values
     * @throws IllegalArgumentException if {@code length} is negative
     */
    public static ShortMatrix random(final int length) {
        return random(1, length);
    }

    /**
     * Creates a new matrix of the specified dimensions filled with random short values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.random(2, 3);
     * matrix.rowCount();            // returns 2
     * matrix.columnCount();         // returns 3 (values are random shorts)
     *
     * ShortMatrix.random(0, 0).isEmpty();   // returns true
     * ShortMatrix.random(-1, 3);            // throws IllegalArgumentException (negative rowCount)
     * ShortMatrix.random(0, 3);             // throws IllegalArgumentException (0 rows with non-zero columns)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code rowCount x columnCount} {@code ShortMatrix} filled with random values
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if the resulting shape cannot be represented (e.g. zero rows with non-zero columns)
     */
    public static ShortMatrix random(final int rowCount, final int columnCount) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final short[][] a = new short[rowCount][columnCount];

        for (short[] ea : a) {
            for (int i = 0; i < columnCount; i++) {
                ea[i] = (short) (RAND.nextInt(BOUND) + Short.MIN_VALUE);
            }
        }

        return new ShortMatrix(a);
    }

    /**
     * Creates a new matrix of the specified dimensions where every element is the provided {@code element}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.repeat(2, 3, (short) 1);
     * matrix.get(0, 0);                         // returns (short) 1
     * matrix.elementCount();                    // returns 6L (matrix is [[1, 1, 1], [1, 1, 1]])
     *
     * ShortMatrix.repeat(0, 0, (short) 7).isEmpty();   // returns true
     * ShortMatrix.repeat(-1, 3, (short) 7);            // throws IllegalArgumentException (negative rowCount)
     * ShortMatrix.repeat(0, 3, (short) 7);             // throws IllegalArgumentException (0 rows with non-zero columns)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @param element the short value to fill the matrix with
     * @return a new {@code rowCount x columnCount} {@code ShortMatrix} filled with the specified element
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if the resulting shape cannot be represented (e.g. zero rows with non-zero columns)
     */
    public static ShortMatrix repeat(final int rowCount, final int columnCount, final short element) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final short[][] a = new short[rowCount][columnCount];

        for (short[] ea : a) {
            N.fill(ea, element);
        }

        return new ShortMatrix(a);
    }

    /**
     * Creates a 1-row ShortMatrix with values from startInclusive to endExclusive.
     * The values are generated with a step of 1. If {@code startInclusive >= endExclusive}, a 1×0 matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.range((short) 0, (short) 5);
     * matrix.rowCopy(0);                                   // returns [0, 1, 2, 3, 4]
     * matrix.columnCount();                                // returns 5
     *
     * ShortMatrix.range((short) 5, (short) 0).columnCount();   // returns 0 (start >= end -> 1x0 matrix)
     * ShortMatrix.range((short) 3, (short) 4).columnCount();   // returns 1 (single value [[3]])
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new 1×n ShortMatrix where n = max(0, endExclusive - startInclusive)
     */
    public static ShortMatrix range(final short startInclusive, final short endExclusive) {
        return new ShortMatrix(new short[][] { Array.range(startInclusive, endExclusive) });
    }

    /**
     * Creates a 1-row ShortMatrix with values from startInclusive to endExclusive with the specified step.
     * The step size can be positive (for ascending sequences) or negative (for descending sequences).
     * If the step would not reach endExclusive from startInclusive, a 1×0 matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix.range((short) 0, (short) 10, (short) 2).rowCopy(0);    // returns [0, 2, 4, 6, 8]
     * ShortMatrix.range((short) 10, (short) 0, (short) -2).rowCopy(0);   // returns [10, 8, 6, 4, 2]
     *
     * ShortMatrix.range((short) 0, (short) 10, (short) -1).columnCount();   // returns 0 (step is wrong direction)
     * ShortMatrix.range((short) 0, (short) 10, (short) 0);                  // throws IllegalArgumentException (zero step)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param step the step size (must not be zero; can be positive or negative)
     * @return a new 1×n ShortMatrix with values incremented by the step size
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static ShortMatrix range(final short startInclusive, final short endExclusive, final short step) {
        return new ShortMatrix(new short[][] { Array.range(startInclusive, endExclusive, step) });
    }

    /**
     * Creates a 1-row ShortMatrix with values from startInclusive to endInclusive.
     * This method includes the end value, unlike {@link #range(short, short)}.
     * If {@code startInclusive > endInclusive}, a 1×0 matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix.rangeClosed((short) 0, (short) 4).rowCopy(0);    // returns [0, 1, 2, 3, 4]
     * ShortMatrix.rangeClosed((short) 5, (short) 5).rowCopy(0);    // returns [5] (single element)
     *
     * ShortMatrix.rangeClosed((short) 5, (short) 0).columnCount();   // returns 0 (start > end -> 1x0 matrix)
     * ShortMatrix.rangeClosed((short) 0, (short) 0).columnCount();   // returns 1 (single element [[0]])
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new 1×n ShortMatrix where n = max(0, endInclusive - startInclusive + 1)
     */
    public static ShortMatrix rangeClosed(final short startInclusive, final short endInclusive) {
        return new ShortMatrix(new short[][] { Array.rangeClosed(startInclusive, endInclusive) });
    }

    /**
     * Creates a 1-row ShortMatrix with values from startInclusive to endInclusive with the specified step.
     * The step size can be positive (for ascending sequences) or negative (for descending sequences).
     * The end value is included only if it is reachable by stepping from start. If the step would not
     * reach endInclusive from startInclusive, a 1×0 matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix.rangeClosed((short) 0, (short) 8, (short) 2).rowCopy(0);    // returns [0, 2, 4, 6, 8]
     * ShortMatrix.rangeClosed((short) 0, (short) 9, (short) 2).rowCopy(0);    // returns [0, 2, 4, 6, 8] (9 not reachable)
     * ShortMatrix.rangeClosed((short) 10, (short) 0, (short) -2).rowCopy(0);  // returns [10, 8, 6, 4, 2, 0]
     *
     * ShortMatrix.rangeClosed((short) 0, (short) 8, (short) -2).columnCount();   // returns 0 (wrong direction)
     * ShortMatrix.rangeClosed((short) 0, (short) 8, (short) 0);                  // throws IllegalArgumentException (zero step)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive, if reachable by stepping)
     * @param step the step size (must not be zero; can be positive or negative)
     * @return a new 1×n ShortMatrix with values incremented by the step size
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static ShortMatrix rangeClosed(final short startInclusive, final short endInclusive, final short step) {
        return new ShortMatrix(new short[][] { Array.rangeClosed(startInclusive, endInclusive, step) });
    }

    /**
     * Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
     * All other elements are set to zero. The resulting matrix has dimensions n×n where n is the length
     * of the diagonal array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.mainDiagonal(new short[] {1, 2, 3});
     * matrix.get(0, 0);                       // returns (short) 1
     * matrix.get(2, 2);                       // returns (short) 3
     * matrix.get(0, 1);                       // returns (short) 0 (off-diagonal cell)
     * // matrix is [[1, 0, 0], [0, 2, 0], [0, 0, 3]]
     *
     * ShortMatrix.mainDiagonal((short[]) null).isEmpty();   // returns true
     * ShortMatrix.mainDiagonal(new short[0]).isEmpty();     // returns true
     * }</pre>
     *
     * @param mainDiagonal the array of diagonal elements; may be {@code null} or empty
     * @return a square {@code n x n} matrix with the specified main diagonal,
     *         or an empty matrix if {@code mainDiagonal} is {@code null} or empty
     * @see #antiDiagonal(short[])
     * @see #diagonals(short[], short[])
     */
    public static ShortMatrix mainDiagonal(final short[] mainDiagonal) {
        return diagonals(mainDiagonal, null);
    }

    /**
     * Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
     * All other elements are set to zero. The matrix size is n×n where n is the length
     * of the diagonal array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.antiDiagonal(new short[] {1, 2, 3});
     * matrix.get(0, 2);                       // returns (short) 1
     * matrix.get(2, 0);                       // returns (short) 3
     * matrix.get(0, 0);                       // returns (short) 0 (off anti-diagonal cell)
     * // matrix is [[0, 0, 1], [0, 2, 0], [3, 0, 0]]
     *
     * ShortMatrix.antiDiagonal((short[]) null).isEmpty();   // returns true
     * ShortMatrix.antiDiagonal(new short[0]).isEmpty();     // returns true
     * }</pre>
     *
     * @param antiDiagonal the array of anti-diagonal elements; may be {@code null} or empty
     * @return a square {@code n x n} matrix with the specified anti-diagonal,
     *         or an empty matrix if {@code antiDiagonal} is {@code null} or empty
     * @see #mainDiagonal(short[])
     * @see #diagonals(short[], short[])
     */
    public static ShortMatrix antiDiagonal(final short[] antiDiagonal) {
        return diagonals(null, antiDiagonal);
    }

    /**
     * Creates a square matrix from the specified main diagonal and anti-diagonal elements.
     * All other elements are set to zero. If both arrays are provided, they must have the same length.
     * The resulting matrix has dimensions n×n where n is the length of the non-empty diagonal array.
     * When both diagonals are provided and they overlap (at the center element of odd-sized matrices),
     * the main diagonal value takes precedence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.diagonals(new short[] {1, 2, 3}, new short[] {4, 5, 6});
     * matrix.get(0, 0);                       // returns (short) 1 (main diagonal)
     * matrix.get(0, 2);                       // returns (short) 4 (anti-diagonal)
     * matrix.get(1, 1);                       // returns (short) 2 (center: main wins over anti)
     * // matrix is [[1, 0, 4], [0, 2, 0], [6, 0, 3]]
     *
     * ShortMatrix.diagonals((short[]) null, (short[]) null).isEmpty();   // returns true
     * ShortMatrix.diagonals(new short[] {1, 2}, new short[] {4, 5, 6});  // throws IllegalArgumentException (length mismatch)
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements (can be {@code null} or empty)
     * @param antiDiagonal the array of anti-diagonal elements (can be {@code null} or empty)
     * @return a square matrix with the specified diagonals, or an empty matrix if both inputs are {@code null} or empty
     * @throws IllegalArgumentException if both arrays are non-empty and have different lengths
     */
    public static ShortMatrix diagonals(final short[] mainDiagonal, final short[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(N.isEmpty(mainDiagonal) || N.isEmpty(antiDiagonal) || mainDiagonal.length == antiDiagonal.length,
                "The length of 'mainDiagonal' and 'antiDiagonal' must be same");

        if (N.isEmpty(mainDiagonal) && N.isEmpty(antiDiagonal)) {
            return EMPTY_SHORT_MATRIX;
        }

        final int matrixSize = N.max(N.len(mainDiagonal), N.len(antiDiagonal));
        final short[][] result = new short[matrixSize][matrixSize];

        if (N.notEmpty(antiDiagonal)) {
            for (int i = 0, j = matrixSize - 1; i < matrixSize; i++, j--) {
                result[i][j] = antiDiagonal[i];
            }
        }

        if (N.notEmpty(mainDiagonal)) {
            for (int i = 0; i < matrixSize; i++) {
                result[i][i] = mainDiagonal[i]; // NOSONAR
            }
        }

        return new ShortMatrix(result);
    }

    /**
     * Converts a boxed {@code Matrix<Short>} to a primitive {@code ShortMatrix}.
     * {@code null} cell values in the input matrix are converted to {@code 0}.
     *
     * <p>This method unboxes all {@code Short} wrapper objects to primitive {@code short} values for more efficient
     * storage and operations. This is particularly beneficial when working with large matrices, as primitive
     * arrays have less memory overhead and better cache locality than arrays of wrapper objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Short> boxed = Matrix.of(new Short[][] {{1, 2}, {3, 4}});
     * ShortMatrix primitiveMatrix = ShortMatrix.unbox(boxed);
     * primitiveMatrix.get(0, 1);                // returns (short) 2
     *
     * Matrix<Short> withNull = Matrix.of(new Short[][] {{1, null}, {3, 4}});
     * ShortMatrix.unbox(withNull).get(0, 1);    // returns (short) 0 (null cell -> 0)
     *
     * ShortMatrix.unbox(Matrix.of(new Short[0][0])).isEmpty();   // returns true
     * ShortMatrix.unbox(null);                                   // throws NullPointerException
     * }</pre>
     *
     * @param x the boxed Short matrix to convert; must not be {@code null}
     * @return a new ShortMatrix with unboxed primitive values
     * @throws NullPointerException if {@code x} is {@code null}
     * @see #boxed()
     */
    public static ShortMatrix unbox(final Matrix<Short> x) {
        return ShortMatrix.of(Array.unbox(x.a));
    }

    /**
     * Returns the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.get(0, 1);   // returns (short) 2
     * matrix.get(1, 0);   // returns (short) 3
     * matrix.get(2, 0);   // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * matrix.get(0, 5);   // throws ArrayIndexOutOfBoundsException (column out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return the element at position (rowIndex, columnIndex)
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public short get(final int rowIndex, final int columnIndex) {
        return a[rowIndex][columnIndex];
    }

    /**
     * Returns the element at the specified point.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.get(Point.of(0, 1));   // returns (short) 2
     * matrix.get(Point.of(1, 1));   // returns (short) 4
     * matrix.get(Point.of(5, 0));   // throws ArrayIndexOutOfBoundsException (out of bounds)
     * matrix.get((Point) null);     // throws IllegalArgumentException (null point)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @return the short element at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #get(int, int)
     */
    public short get(final Point point) {
        N.checkArgNotNull(point, "point");

        return a[point.rowIndex()][point.columnIndex()];
    }

    /**
     * Sets the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.set(0, 1, (short) 9);
     * matrix.get(0, 1);                   // returns (short) 9 (cell updated)
     * matrix.set(1, 0, (short) -5);
     * matrix.get(1, 0);                   // returns (short) -5
     * matrix.set(2, 0, (short) 1);        // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * matrix.set(0, 5, (short) 1);        // throws ArrayIndexOutOfBoundsException (column out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @param value the value to set
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public void set(final int rowIndex, final int columnIndex, final short value) {
        a[rowIndex][columnIndex] = value;
    }

    /**
     * Sets the element at the specified point to the given value.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.set(Point.of(0, 1), (short) 9);
     * matrix.get(Point.of(0, 1));               // returns (short) 9 (cell updated)
     * matrix.set(Point.of(1, 1), (short) 7);
     * matrix.get(1, 1);                         // returns (short) 7
     * matrix.set(Point.of(5, 0), (short) 1);    // throws ArrayIndexOutOfBoundsException (out of bounds)
     * matrix.set((Point) null, (short) 1);      // throws IllegalArgumentException (null point)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @param value the new short value to set at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #set(int, int, short)
     */
    public void set(final Point point, final short value) {
        N.checkArgNotNull(point, "point");

        a[point.rowIndex()][point.columnIndex()] = value;
    }

    /**
     * Returns the element directly above the specified position, if it exists.
     * This method provides safe access to the element directly above the given position
     * without throwing an exception when at the top edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.valueAbove(1, 0).get();         // returns (short) 1
     * matrix.valueAbove(1, 1).get();         // returns (short) 2
     * matrix.valueAbove(0, 0).isPresent();   // returns false (no row above row 0)
     * matrix.valueAbove(5, 0);               // throws ArrayIndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalShort containing the element at position (rowIndex - 1, columnIndex), or empty if rowIndex == 0
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalShort valueAbove(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? OptionalShort.empty() : OptionalShort.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, if it exists.
     * This method provides safe access to the element directly below the given position
     * without throwing an exception when at the bottom edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.valueBelow(0, 0).get();         // returns (short) 3
     * matrix.valueBelow(0, 1).get();         // returns (short) 4
     * matrix.valueBelow(1, 0).isPresent();   // returns false (no row below last row)
     * matrix.valueBelow(5, 0);               // throws ArrayIndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalShort containing the element at position (rowIndex + 1, columnIndex), or empty if rowIndex == rowCount - 1
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalShort valueBelow(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? OptionalShort.empty() : OptionalShort.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, if it exists.
     * This method provides safe access to the element directly to the left of the given position
     * without throwing an exception when at the leftmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.valueLeft(0, 1).get();         // returns (short) 1
     * matrix.valueLeft(1, 1).get();         // returns (short) 3
     * matrix.valueLeft(0, 0).isPresent();   // returns false (no column left of column 0)
     * matrix.valueLeft(0, 5);               // throws ArrayIndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalShort containing the element at position (rowIndex, columnIndex - 1), or empty if columnIndex == 0
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalShort valueLeft(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? OptionalShort.empty() : OptionalShort.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, if it exists.
     * This method provides safe access to the element directly to the right of the given position
     * without throwing an exception when at the rightmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.valueRight(0, 0).get();         // returns (short) 2
     * matrix.valueRight(1, 0).get();         // returns (short) 4
     * matrix.valueRight(0, 1).isPresent();   // returns false (no column right of last column)
     * matrix.valueRight(0, 5);               // throws ArrayIndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalShort containing the element at position (rowIndex, columnIndex + 1), or empty if columnIndex == columnCount - 1
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalShort valueRight(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? OptionalShort.empty() : OptionalShort.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as a short array.
     *
     * <p><b>Note:</b> This method returns a reference to the internal array, not a copy.
     * Modifications to the returned array will affect the matrix. If you need an independent
     * copy, use {@code Arrays.copyOf(matrix.rowView(i), matrix.columnCount())}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * short[] firstRow = matrix.rowView(0);   // returns [1, 2, 3]
     * matrix.rowView(1);                      // returns [4, 5, 6]
     *
     * firstRow[0] = 10;                       // shared storage: matrix.get(0, 0) now returns (short) 10
     * matrix.rowView(2);                      // throws IllegalArgumentException (row out of bounds)
     * matrix.rowView(-1);                     // throws IllegalArgumentException (negative index)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return the specified row array (direct reference to internal storage)
     * @throws IllegalArgumentException if rowIndex &lt; 0 or rowIndex &gt;= rowCount
     */
    @Override
    public short[] rowView(final int rowIndex) throws IllegalArgumentException {
        N.checkArgument(rowIndex >= 0 && rowIndex < rowCount, MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount);

        return a[rowIndex];
    }

    /**
     * Returns a defensive copy of the specified row.
     * Changes to the returned array do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * short[] firstRow = matrix.rowCopy(0);   // returns [1, 2, 3]
     * matrix.rowCopy(1);                      // returns [4, 5, 6]
     *
     * firstRow[0] = 10;                       // copy: matrix.get(0, 0) still returns (short) 1
     * matrix.rowCopy(2);                      // throws IllegalArgumentException (row out of bounds)
     * matrix.rowCopy(-1);                     // throws IllegalArgumentException (negative index)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new short array containing the values from the specified row
     * @throws IllegalArgumentException if rowIndex &lt; 0 or rowIndex &gt;= rowCount
     * @see #rowView(int)
     */
    @Override
    public short[] rowCopy(final int rowIndex) throws IllegalArgumentException {
        N.checkArgument(rowIndex >= 0 && rowIndex < rowCount, MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount);

        return N.copyOf(a[rowIndex], columnCount);
    }

    /**
     * Returns a copy of the specified column as a new short array.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously in memory. Modifications to the returned array
     * will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * short[] firstColumn = matrix.columnCopy(0);   // returns [1, 4]
     * matrix.columnCopy(2);                         // returns [3, 6]
     *
     * firstColumn[0] = 10;                          // copy: matrix.get(0, 0) still returns (short) 1
     * matrix.columnCopy(3);                         // throws IllegalArgumentException (column out of bounds)
     * matrix.columnCopy(-1);                        // throws IllegalArgumentException (negative index)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new array containing the values from the specified column
     * @throws IllegalArgumentException if columnIndex &lt; 0 or columnIndex &gt;= columnCount
     */
    @Override
    public short[] columnCopy(final int columnIndex) throws IllegalArgumentException {
        N.checkArgument(columnIndex >= 0 && columnIndex < columnCount, MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount);

        final short[] result = new short[rowCount];

        for (int i = 0; i < rowCount; i++) {
            result[i] = a[i][columnIndex];
        }

        return result;
    }

    /**
     * Sets the values of the specified row by copying from the provided array.
     * All elements in the row are replaced with values from the provided array.
     *
     * <p>The values from the source array are copied into the matrix row.
     * The source array must have exactly the same length as the number of columns in the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.setRow(0, new short[] {7, 8, 9});
     * matrix.rowCopy(0);                               // returns [7, 8, 9] (row replaced)
     *
     * matrix.setRow(0, new short[] {1, 2});           // throws IllegalArgumentException (length != columnCount)
     * matrix.setRow(5, new short[] {1, 2, 3});        // throws IllegalArgumentException (row out of bounds)
     * matrix.setRow(0, (short[]) null);               // throws NullPointerException
     * }</pre>
     *
     * @param rowIndex the index of the row to set (0-based)
     * @param row the array of values to copy into the row; must have length equal to the number of columns
     * @throws NullPointerException if {@code row} is {@code null}
     * @throws IllegalArgumentException if rowIndex is out of bounds or row length does not match column count
     */
    public void setRow(final int rowIndex, final short[] row) throws IllegalArgumentException {
        N.checkArgument(rowIndex >= 0 && rowIndex < rowCount, MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount);
        N.checkArgument(row.length == columnCount, MSG_ROW_LENGTH_MISMATCH, columnCount, row.length);

        N.copy(row, 0, a[rowIndex], 0, columnCount);
    }

    /**
     * Sets the values of the specified column by copying from the provided array.
     * All elements in the column are replaced with values from the provided array.
     *
     * <p>The values from the source array are copied into the matrix column.
     * The source array must have exactly the same length as the number of rows in the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.setColumn(0, new short[] {7, 8});
     * matrix.columnCopy(0);                            // returns [7, 8] (column replaced)
     *
     * matrix.setColumn(0, new short[] {1, 2, 3});     // throws IllegalArgumentException (length != rowCount)
     * matrix.setColumn(5, new short[] {1, 2});        // throws IllegalArgumentException (column out of bounds)
     * matrix.setColumn(0, (short[]) null);            // throws NullPointerException
     * }</pre>
     *
     * @param columnIndex the index of the column to set (0-based)
     * @param column the array of values to copy into the column; must have length equal to the number of rows
     * @throws NullPointerException if {@code column} is {@code null}
     * @throws IllegalArgumentException if columnIndex is out of bounds or column length does not match row count
     */
    public void setColumn(final int columnIndex, final short[] column) throws IllegalArgumentException {
        N.checkArgument(columnIndex >= 0 && columnIndex < columnCount, MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount);
        N.checkArgument(column.length == rowCount, MSG_COLUMN_LENGTH_MISMATCH, rowCount, column.length);

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = column[i];
        }
    }

    /**
     * Updates all elements in the specified row by applying the given operator to each element.
     * The matrix is modified in-place. Each element in the row is transformed by the operator
     * and replaced with the result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.updateRow(0, x -> (short) (x * 2));
     * matrix.rowCopy(0);                              // returns [2, 4, 6]
     *
     * matrix.updateRow(1, x -> (short) 0);
     * matrix.rowCopy(1);                              // returns [0, 0, 0]
     * matrix.updateRow(5, x -> x);                    // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * matrix.updateRow(0, null);                      // throws IllegalArgumentException (null operator)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param rowIndex the index of the row to update (0-based)
     * @param operator the unary operator to apply to each element in the row, taking a short and returning a short
     * @throws ArrayIndexOutOfBoundsException if rowIndex is out of bounds
     * @throws IllegalArgumentException if operator is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.ShortUnaryOperator<E> operator) throws E {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new ArrayIndexOutOfBoundsException(formatMsg(MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount));
        }

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < columnCount; i++) {
            a[rowIndex][i] = operator.applyAsShort(a[rowIndex][i]);
        }
    }

    /**
     * Updates all elements in the specified column by applying the given operator to each element.
     * The matrix is modified in-place. Each element in the column is transformed by the operator
     * and replaced with the result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.updateColumn(1, x -> (short) (x + 10));
     * matrix.columnCopy(1);                              // returns [12, 15]
     *
     * matrix.updateColumn(0, x -> (short) 0);
     * matrix.columnCopy(0);                              // returns [0, 0]
     * matrix.updateColumn(5, x -> x);                    // throws ArrayIndexOutOfBoundsException (column out of bounds)
     * matrix.updateColumn(0, null);                      // throws IllegalArgumentException (null operator)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param columnIndex the index of the column to update (0-based)
     * @param operator the unary operator to apply to each element in the column, taking a short and returning a short
     * @throws ArrayIndexOutOfBoundsException if columnIndex is out of bounds
     * @throws IllegalArgumentException if operator is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.ShortUnaryOperator<E> operator) throws E {
        if (columnIndex < 0 || columnIndex >= columnCount) {
            throw new ArrayIndexOutOfBoundsException(formatMsg(MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount));
        }

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = operator.applyAsShort(a[i][columnIndex]);
        }
    }

    /**
     * Returns a copy of the main diagonal elements (upper-left to lower-right).
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>This method extracts the main diagonal elements at positions (0,0), (1,1), (2,2), etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.getMainDiagonal();                              // returns [1, 5, 9]
     *
     * ShortMatrix single = ShortMatrix.of(new short[][] {{5}});
     * single.getMainDiagonal();                             // returns [5]
     *
     * ShortMatrix rect = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * rect.getMainDiagonal();                               // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new short array containing the main diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public short[] getMainDiagonal() throws IllegalStateException {
        checkIsSquare();

        final short[] result = new short[rowCount];

        for (int i = 0; i < rowCount; i++) {
            result[i] = a[i][i]; // NOSONAR
        }

        return result;
    }

    /**
     * Sets the elements on the main diagonal (upper-left to lower-right).
     * The matrix must be square (rowCount == columnCount), and the diagonal array must have
     * exactly as many elements as the matrix has rows.
     *
     * <p>This method sets the main diagonal elements at positions (0,0), (1,1), (2,2), etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.setMainDiagonal(new short[] {9, 8});
     * matrix.getMainDiagonal();                          // returns [9, 8]
     * matrix.get(0, 1);                                  // returns (short) 2 (off-diagonal unchanged)
     *
     * matrix.setMainDiagonal(new short[] {1});           // throws IllegalArgumentException (length != rowCount)
     *
     * ShortMatrix rect = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * rect.setMainDiagonal(new short[] {1, 2});          // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must have length equal to rowCount
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if mainDiagonal array length does not equal rowCount
     */
    @Override
    public void setMainDiagonal(final short[] mainDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgument(N.len(mainDiagonal) == rowCount, MSG_DIAGONAL_LENGTH_MISMATCH, rowCount, N.len(mainDiagonal));

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = mainDiagonal[i];
        }
    }

    /**
     * Updates all elements on the main diagonal from upper-left to lower-right by applying the given operator.
     * The matrix must be square (same number of rows and columns).
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.updateMainDiagonal(x -> (short) (x * 2));
     * matrix.getMainDiagonal();                          // returns [2, 10, 18]
     * matrix.get(0, 1);                                  // returns (short) 2 (off-diagonal unchanged)
     *
     * matrix.updateMainDiagonal(null);                   // throws IllegalArgumentException (null operator)
     *
     * ShortMatrix rect = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * rect.updateMainDiagonal(x -> x);                   // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each diagonal element
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateMainDiagonal(final Throwables.ShortUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = operator.applyAsShort(a[i][i]);
        }
    }

    /**
     * Returns a copy of the anti-diagonal elements (upper-right to lower-left).
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>This method extracts the anti-diagonal (secondary diagonal) elements from
     * upper-right to lower-left, at positions (0,n-1), (1,n-2), (2,n-3), etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.getAntiDiagonal();                              // returns [3, 5, 7]
     *
     * ShortMatrix single = ShortMatrix.of(new short[][] {{5}});
     * single.getAntiDiagonal();                             // returns [5]
     *
     * ShortMatrix rect = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * rect.getAntiDiagonal();                               // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new short array containing the anti-diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public short[] getAntiDiagonal() throws IllegalStateException {
        checkIsSquare();

        final short[] result = new short[rowCount];

        for (int i = 0; i < rowCount; i++) {
            result[i] = a[i][columnCount - i - 1];
        }

        return result;
    }

    /**
     * Sets the elements on the anti-diagonal (upper-right to lower-left).
     * The matrix must be square (rowCount == columnCount), and the diagonal array must have
     * exactly as many elements as the matrix has rows.
     *
     * <p>This method sets the anti-diagonal (secondary diagonal) elements from
     * top-right to bottom-left, at positions (0,n-1), (1,n-2), (2,n-3), etc.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.setAntiDiagonal(new short[] {9, 8});
     * matrix.getAntiDiagonal();                          // returns [9, 8]
     * matrix.get(0, 0);                                  // returns (short) 1 (off anti-diagonal unchanged)
     *
     * matrix.setAntiDiagonal(new short[] {1});           // throws IllegalArgumentException (length != rowCount)
     *
     * ShortMatrix rect = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * rect.setAntiDiagonal(new short[] {1, 2});          // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must have length equal to rowCount
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if antiDiagonal array length does not equal rowCount
     */
    @Override
    public void setAntiDiagonal(final short[] antiDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgument(N.len(antiDiagonal) == rowCount, MSG_DIAGONAL_LENGTH_MISMATCH, rowCount, N.len(antiDiagonal));

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = antiDiagonal[i];
        }
    }

    /**
     * Updates all elements on the anti-diagonal from upper-right to lower-left by applying the given operator.
     * The matrix must be square (same number of rows and columns).
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.updateAntiDiagonal(x -> (short) (x + 1));
     * matrix.getAntiDiagonal();                          // returns [4, 6, 8]
     * matrix.get(0, 0);                                  // returns (short) 1 (off anti-diagonal unchanged)
     *
     * matrix.updateAntiDiagonal(null);                   // throws IllegalArgumentException (null operator)
     *
     * ShortMatrix rect = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * rect.updateAntiDiagonal(x -> x);                   // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each anti-diagonal element
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAntiDiagonal(final Throwables.ShortUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = operator.applyAsShort(a[i][columnCount - i - 1]);
        }
    }

    /**
     * Updates all elements in the matrix by applying the given operator to each element.
     * The matrix is modified in-place. This operation may be performed in parallel for large matrices
     * to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.updateAll(x -> (short) (x * 2));
     * matrix.get(1, 1);                          // returns (short) 8 (matrix is [[2, 4], [6, 8]])
     *
     * // short overflow: result wraps modulo 65536
     * ShortMatrix big = ShortMatrix.of(new short[][] {{32767}});
     * big.updateAll(x -> (short) (x + 1));
     * big.get(0, 0);                             // returns (short) -32768 (wraparound)
     *
     * ShortMatrix.empty().updateAll(x -> (short) 1);   // no-op on empty matrix, stays empty
     * matrix.updateAll(null);                          // throws IllegalArgumentException (null operator)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the unary operator to apply to each element, taking a short and returning a short
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.ShortUnaryOperator<E> operator) throws E {
        N.checkArgNotNull(operator, "operator");
        final Throwables.IntBiConsumer<E> operation = (i, j) -> a[i][j] = operator.applyAsShort(a[i][j]);
        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));
    }

    /**
     * Updates all elements in the matrix based on their position by applying the given mapper.
     * The mapper receives the row and column indices (0-based) and returns the new value for that position.
     * The matrix is modified in-place. This operation may be performed in parallel for large matrices
     * to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.updateAll((i, j) -> (short) (i + j));
     * matrix.get(0, 0);                              // returns (short) 0 (matrix is [[0, 1], [1, 2]])
     * matrix.get(1, 1);                              // returns (short) 2
     *
     * ShortMatrix.empty().updateAll((i, j) -> (short) 1);                           // no-op on empty matrix, stays empty
     * matrix.updateAll((Throwables.IntBiFunction<Short, RuntimeException>) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the bi-function that takes (rowIndex, columnIndex) and returns the new short value
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws NullPointerException if the mapper returns {@code null} for any cell
     * @throws E if the mapper throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Short, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final Throwables.IntBiConsumer<E> operation = (i, j) -> a[i][j] = mapper.apply(i, j);
        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));
    }

    /**
     * Conditionally replaces elements in the matrix based on a predicate.
     * Each element that satisfies the predicate is replaced with the specified new value.
     * The matrix is modified in-place. This operation may be performed in parallel for large matrices
     * to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.replaceIf(x -> x > 3, (short) 0);
     * matrix.get(1, 0);                            // returns (short) 0 (matrix is [[1, 2, 3], [0, 0, 0]])
     * matrix.get(0, 0);                            // returns (short) 1 (1 not > 3, unchanged)
     *
     * ShortMatrix none = ShortMatrix.of(new short[][] {{1, 2}});
     * none.replaceIf(x -> x > 100, (short) 9);
     * none.get(0, 0);                                                                    // returns (short) 1 (no element matched)
     * matrix.replaceIf((Throwables.ShortPredicate<RuntimeException>) null, (short) 0);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to test each element; returns {@code true} if the element should be replaced
     * @param newValue the value to replace matching elements with
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.ShortPredicate<E> predicate, final short newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> operation = (i, j) -> a[i][j] = predicate.test(a[i][j]) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));
    }

    /**
     * Conditionally replaces elements in the matrix based on their position.
     * The predicate receives the row and column indices (0-based) and returns {@code true} if the element
     * at that position should be replaced with the new value. The matrix is modified in-place.
     * This operation may be performed in parallel for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.replaceIf((i, j) -> i == j, (short) 0);
     * matrix.get(1, 1);                                  // returns (short) 0 (diagonal replaced)
     * matrix.get(0, 1);                                  // returns (short) 2 (off-diagonal unchanged)
     *
     * ShortMatrix other = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * other.replaceIf((i, j) -> j > 5, (short) 9);
     * other.get(0, 0);                                                                   // returns (short) 1 (no position matched)
     * matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, (short) 0);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the bi-predicate that takes (rowIndex, columnIndex) and returns {@code true} if element should be replaced
     * @param newValue the value to replace matching elements with
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final short newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> operation = (i, j) -> a[i][j] = predicate.test(i, j) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));
    }

    /**
     * Creates a new matrix by applying the given function to each element of this matrix.
     * The original matrix is not modified. This operation may be performed in parallel for large matrices
     * to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix squared = matrix.map(x -> (short) (x * x));
     * squared.get(1, 1);                          // returns (short) 16 (result is [[1, 4], [9, 16]])
     * matrix.get(1, 1);                           // returns (short) 4 (original unchanged)
     *
     * // short overflow narrows in the caller's lambda
     * ShortMatrix big = ShortMatrix.of(new short[][] {{200}});
     * big.map(x -> (short) (x * x)).get(0, 0);    // returns (short) -25536 (40000 wraps)
     *
     * ShortMatrix.empty().map(x -> x).isEmpty();  // returns true
     * matrix.map(null);                           // throws IllegalArgumentException (null mapper)
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param mapper the unary operator to apply to each element, taking a short and returning a short
     * @return a new ShortMatrix with the transformed values; the original matrix is unchanged
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> ShortMatrix map(final Throwables.ShortUnaryOperator<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final short[][] result = new short[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.applyAsShort(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return ShortMatrix.of(result);
    }

    /**
     * Creates a new object matrix by applying the given function to each element of this matrix.
     * The function transforms each primitive short value to an object of the specified type.
     * The original matrix is not modified. This operation may be performed in parallel for large matrices
     * to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * Matrix<String> stringMatrix = matrix.mapToObj(x -> "Value: " + x, String.class);
     * stringMatrix.get(0, 0);                          // returns "Value: 1"
     * stringMatrix.get(1, 1);                          // returns "Value: 4"
     *
     * Matrix<Integer> ints = matrix.mapToObj(x -> (int) (x * 10), Integer.class);
     * ints.get(0, 1);                                  // returns 20
     *
     * ShortMatrix.empty().mapToObj(x -> "x", String.class).isEmpty();   // returns true
     * matrix.mapToObj(null, String.class);                              // throws IllegalArgumentException (null mapper)
     * }</pre>
     *
     * @param <R> the type of elements in the resulting matrix
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to transform each short to an object of type {@code R}
     * @param targetElementType the class of the target element type, used to allocate the backing array
     * @return a new {@code Matrix<R>} with the transformed object values; the original matrix is unchanged
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.ShortFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.apply(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return Matrix.of(result);
    }

    /**
     * Fills all elements of the matrix with the specified value.
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.fill((short) 5);
     * matrix.get(0, 0);            // returns (short) 5 (matrix is [[5, 5], [5, 5]])
     * matrix.get(1, 1);            // returns (short) 5
     *
     * matrix.fill((short) 0);
     * matrix.get(0, 1);                      // returns (short) 0
     * ShortMatrix.empty().fill((short) 7);   // no-op on empty matrix, stays empty
     * }</pre>
     *
     * @param value the value to fill the matrix with
     */
    public void fill(final short value) {
        for (int i = 0; i < rowCount; i++) {
            N.fill(a[i], value);
        }
    }

    /**
     * Fills the matrix with values from another two-dimensional array, starting at position (0, 0).
     * The source array can be smaller than this matrix; only the overlapping region is copied.
     * If the source array is larger, only the portion that fits is copied. The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{0, 0, 0}, {0, 0, 0}});
     * matrix.fill(new short[][] {{1, 2}, {3, 4}});
     * matrix.get(0, 0);                                  // returns (short) 1 (matrix is [[1, 2, 0], [3, 4, 0]])
     * matrix.get(0, 2);                                  // returns (short) 0 (untouched column)
     *
     * // larger source: only the overlapping region is copied
     * ShortMatrix small = ShortMatrix.of(new short[][] {{0, 0}});
     * small.fill(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * small.get(0, 1);                                   // returns (short) 2 (extra row/column ignored)
     * matrix.fill((short[][]) null);                     // throws IllegalArgumentException (null source)
     * }</pre>
     *
     * @param source the two-dimensional array to copy values from
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final short[][] source) {
        fill(0, 0, source);
    }

    /**
     * Fills a region of the matrix with values from another two-dimensional array, starting at the specified position.
     * The source array can extend beyond this matrix's bounds; only the overlapping region is copied.
     * The matrix is modified in-place. Elements outside the matrix bounds are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}});
     * matrix.fill(1, 1, new short[][] {{1, 2}, {3, 4}});
     * matrix.get(1, 1);                                  // returns (short) 1 (matrix is [[0,0,0],[0,1,2],[0,3,4]])
     * matrix.get(2, 2);                                  // returns (short) 4
     * matrix.get(0, 0);                                  // returns (short) 0 (top-left untouched)
     *
     * matrix.fill(0, 0, (short[][]) null);             // throws IllegalArgumentException (null source)
     * matrix.fill(4, 0, new short[][] {{1}});          // throws IllegalArgumentException (destRowIndex > rowCount)
     * }</pre>
     *
     * @param destRowIndex the target row index in this matrix (0-based, must be 0 &lt;= destRowIndex &lt;= rowCount)
     * @param destColumnIndex the target column index in this matrix (0-based, must be 0 &lt;= destColumnIndex &lt;= columnCount)
     * @param source the source array to copy values from; must not be {@code null}
     * @throws IllegalArgumentException if {@code source} is {@code null}, if destRowIndex &lt; 0 or &gt; rowCount,
     *         or if destColumnIndex &lt; 0 or &gt; columnCount
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final short[][] source) throws IllegalArgumentException {
        N.checkArgNotNull(source, "source");
        N.checkArgument(destRowIndex >= 0 && destRowIndex <= rowCount, "destRowIndex({}) must be between 0 and rowCount({})", destRowIndex, rowCount);
        N.checkArgument(destColumnIndex >= 0 && destColumnIndex <= columnCount, "destColumnIndex({}) must be between 0 and columnCount({})", destColumnIndex,
                columnCount);

        for (int i = 0, minLen = N.min(rowCount - destRowIndex, source.length); i < minLen; i++) {
            if (source[i] != null) {
                N.copy(source[i], 0, a[i + destRowIndex], destColumnIndex, N.min(source[i].length, columnCount - destColumnIndex));
            }
        }
    }

    /**
     * Returns a copy of this matrix.
     *
     * <p>All elements are copied into a new matrix, so modifications to the copy
     * will not affect the original matrix and vice versa. This method performs a copy,
     * meaning both the outer array and all inner row arrays are cloned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix original = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix copy = original.copy();
     * copy.set(0, 0, (short) 99);
     * copy.get(0, 0);                     // returns (short) 99
     * original.get(0, 0);                 // returns (short) 1 (original unchanged)
     * copy.equals(original);              // returns false (after mutation)
     *
     * ShortMatrix.empty().copy().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new ShortMatrix that is an independent copy of this matrix
     */
    @Override
    public ShortMatrix copy() {
        final short[][] result = new short[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            result[i] = a[i].clone();
        }

        return new ShortMatrix(result);
    }

    /**
     * Creates a copy of a subset of rows from this matrix.
     *
     * <p>The returned matrix contains only the specified rows and is completely independent from the original matrix.
     * All columns from the selected rows are included in the copy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}, {5, 6}});
     * ShortMatrix subset = matrix.copy(1, 3);
     * subset.rowCount();                       // returns 2 (subset is [[3, 4], [5, 6]])
     * subset.get(0, 0);                        // returns (short) 3
     *
     * matrix.copy(0, 0).isEmpty();            // returns true (empty range)
     * matrix.copy(0, 5);                      // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.copy(2, 1);                      // throws IndexOutOfBoundsException (from > to)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new ShortMatrix containing an independent copy of the specified rows
     * @throws IndexOutOfBoundsException if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
     */
    @Override
    public ShortMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        final short[][] result = new short[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            result[i - fromRowIndex] = a[i].clone();
        }

        return new ShortMatrix(result);
    }

    /**
     * Creates a copy of a rectangular sub-region from this matrix.
     *
     * <p>The specified row and column ranges define the sub-matrix to copy.
     * The returned matrix is completely independent from the original.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * ShortMatrix region = matrix.copy(0, 2, 1, 3);
     * region.get(0, 0);                            // returns (short) 2 (region is [[2, 3], [5, 6]])
     * region.get(1, 1);                            // returns (short) 6
     * region.columnCount();                        // returns 2
     *
     * matrix.copy(0, 3, 0, 5);                     // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.copy(0, 2, 2, 1);                     // throws IndexOutOfBoundsException (from > to)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new ShortMatrix containing an independent copy of the specified rectangular region
     * @throws IndexOutOfBoundsException if any index is out of bounds or fromIndex &gt; toIndex
     */
    @Override
    public ShortMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        final short[][] result = new short[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            result[i - fromRowIndex] = N.copyOfRange(a[i], fromColumnIndex, toColumnIndex);
        }

        return new ShortMatrix(result);
    }

    /**
     * Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount},
     * anchored at the top-left corner of this matrix. New cells are filled with {@code 0}.
     *
     * <ul>
     *   <li><b>If a dimension shrinks</b> — elements beyond the new boundary are discarded
     *       (excess rows removed from the bottom, excess columns removed from the right).</li>
     *   <li><b>If a dimension grows</b> — new cells are filled with {@code 0}.</li>
     *   <li><b>Mixed case</b> — each dimension is treated independently, so it is valid
     *       to grow rows while truncating columns, or vice versa.</li>
     * </ul>
     *
     * <p>The original matrix is never modified; a new matrix is always returned.</p>
     *
     * <p><b>Comparison with {@link #extend(int, int, int, int)}:</b>
     * {@code resize} takes <em>absolute</em> target dimensions and may truncate existing content.
     * {@code extend} takes <em>relative</em> padding amounts per edge and <em>never truncates</em>.
     * Use {@code extend} when the entire original content must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     *
     * // Grow: both dimensions larger — new cells filled with 0
     * ShortMatrix grown = matrix.resize(4, 4);
     * grown.get(0, 3);                            // returns (short) 0 (new column)
     * grown.get(3, 3);                            // returns (short) 0 (new corner)
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * ShortMatrix truncated = matrix.resize(2, 2);
     * truncated.rowCount();                       // returns 2 (truncated is [[1, 2], [4, 5]])
     * truncated.get(1, 1);                        // returns (short) 5
     *
     * // Mixed: grow rows, truncate columns
     * matrix.resize(4, 2).get(3, 0);             // returns (short) 0 (new bottom row)
     * matrix.resize(-1, 2);                      // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new ShortMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int, short)
     * @see #extend(int, int, int, int)
     */
    public ShortMatrix resize(final int newRowCount, final int newColumnCount) {
        return resize(newRowCount, newColumnCount, SHORT_0);
    }

    /**
     * Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount},
     * anchored at the top-left corner of this matrix.
     *
     * <ul>
     *   <li><b>If a dimension shrinks</b> — elements beyond the new boundary are discarded.
     *       {@code defaultValue} is <em>not</em> used in this case.</li>
     *   <li><b>If a dimension grows</b> — new cells are filled with {@code defaultValue}.</li>
     *   <li><b>Mixed case</b> — each dimension is treated independently, so it is valid
     *       to grow rows while truncating columns, or vice versa.</li>
     * </ul>
     *
     * <p>The original matrix is never modified; a new matrix is always returned.</p>
     *
     * <p><b>Comparison with {@link #extend(int, int, int, int, short)}:</b>
     * {@code resize} takes <em>absolute</em> target dimensions and may truncate existing content.
     * {@code extend} takes <em>relative</em> padding amounts per edge and <em>never truncates</em>.
     * Use {@code extend} when the entire original content must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     *
     * // Grow: fill new cells with 9
     * ShortMatrix grown = matrix.resize(4, 4, (short) 9);
     * grown.get(0, 3);                            // returns (short) 9 (new column filled)
     * grown.get(3, 3);                            // returns (short) 9 (new corner)
     * grown.get(0, 0);                            // returns (short) 1 (original preserved)
     *
     * // Truncate: defaultValue is ignored when shrinking
     * ShortMatrix truncated = matrix.resize(2, 2, (short) 9);
     * truncated.get(1, 1);                        // returns (short) 5 (truncated is [[1, 2], [4, 5]])
     * matrix.resize(2, -1, (short) 9);            // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the value used to fill cells that are added when a dimension grows;
     *        ignored when a dimension shrinks
     * @return a new ShortMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int)
     * @see #extend(int, int, int, int, short)
     */
    public ShortMatrix resize(final int newRowCount, final int newColumnCount, final short defaultValue) throws IllegalArgumentException {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        // Check for overflow before allocation
        if ((long) newRowCount * newColumnCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Matrix dimensions overflow: " + newRowCount + " x " + newColumnCount + " exceeds Integer.MAX_VALUE");
        }

        if (newRowCount <= rowCount && newColumnCount <= columnCount) {
            return copy(0, newRowCount, 0, newColumnCount);
        } else {
            final boolean fillDefaultValue = defaultValue != SHORT_0;
            final short[][] result = new short[newRowCount][];

            for (int i = 0; i < newRowCount; i++) {
                result[i] = i < rowCount ? N.copyOf(a[i], newColumnCount) : new short[newColumnCount];

                if (fillDefaultValue) {
                    if (i >= rowCount) {
                        N.fill(result[i], defaultValue);
                    } else if (columnCount < newColumnCount) {
                        N.fill(result[i], columnCount, newColumnCount, defaultValue);
                    }
                }
            }

            return new ShortMatrix(result);
        }
    }

    /**
     * Returns a new matrix formed by surrounding this matrix with padding on all four edges.
     * New cells are filled with {@code 0}.
     *
     * <p>Unlike {@link #resize(int, int)}, this method <b>never truncates</b>: the entire content
     * of this matrix is always present in the result. Each parameter specifies how many rows or
     * columns of padding to add on that edge. The original matrix occupies the interior starting
     * at row {@code padTop}, column {@code padLeft}.</p>
     *
     * <p>Result dimensions:
     * <ul>
     *   <li>Rows: {@code padTop + this.rowCount + padBottom}</li>
     *   <li>Columns: {@code padLeft + this.columnCount + padRight}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     *
     * // Uniform 1-cell border of 0
     * ShortMatrix bordered = matrix.extend(1, 1, 1, 1);
     * bordered.rowCount();                        // returns 4
     * bordered.get(0, 0);                         // returns (short) 0 (new border)
     * bordered.get(1, 1);                         // returns (short) 1 (original content shifted)
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * ShortMatrix shifted = matrix.extend(0, 0, 2, 0);
     * shifted.get(0, 2);                         // returns (short) 1 (content shifted right by 2)
     * matrix.extend(0, 0, 0, 0).equals(matrix);  // returns true (no padding -> equal copy)
     * matrix.extend(-1, 0, 0, 0);                // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @return a new ShortMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #extend(int, int, int, int, short)
     * @see #resize(int, int)
     */
    @Override
    public ShortMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight) {
        return extend(padTop, padBottom, padLeft, padRight, SHORT_0);
    }

    /**
     * Returns a new matrix formed by surrounding this matrix with padding on all four edges.
     * New cells are filled with {@code defaultValue}.
     *
     * <p>Unlike {@link #resize(int, int, short)}, this method <b>never truncates</b>: the entire
     * content of this matrix is always present in the result. Each parameter specifies how many
     * rows or columns of padding to add on that edge. The original matrix occupies the interior
     * starting at row {@code padTop}, column {@code padLeft}.</p>
     *
     * <p>Result dimensions:
     * <ul>
     *   <li>Rows: {@code padTop + this.rowCount + padBottom}</li>
     *   <li>Columns: {@code padLeft + this.columnCount + padRight}</li>
     * </ul>
     *
     * <p><b>Typical uses:</b> border padding in image/grid processing, adding margins around
     * a data region, creating convolution-safe buffers.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     *
     * // Uniform 1-cell border filled with 9
     * ShortMatrix bordered = matrix.extend(1, 1, 1, 1, (short) 9);
     * bordered.get(0, 0);                         // returns (short) 9 (filled border)
     * bordered.get(1, 1);                         // returns (short) 1 (original content shifted)
     * bordered.get(3, 3);                         // returns (short) 9 (filled corner)
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * ShortMatrix shifted = matrix.extend(0, 0, 2, 0, (short) 0);
     * shifted.get(0, 0);                         // returns (short) 0 (filled)
     * shifted.get(0, 3);                         // returns (short) 2 (content shifted right by 2)
     * matrix.extend(0, -1, 0, 0, (short) 9);     // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @param defaultValue the value to fill all new padding cells with
     * @return a new ShortMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #extend(int, int, int, int)
     * @see #resize(int, int, short)
     */
    public ShortMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final short defaultValue)
            throws IllegalArgumentException {
        N.checkArgument(padTop >= 0, MSG_NEGATIVE_DIMENSION, "padTop", padTop);
        N.checkArgument(padBottom >= 0, MSG_NEGATIVE_DIMENSION, "padBottom", padBottom);
        N.checkArgument(padLeft >= 0, MSG_NEGATIVE_DIMENSION, "padLeft", padLeft);
        N.checkArgument(padRight >= 0, MSG_NEGATIVE_DIMENSION, "padRight", padRight);

        if (padTop == 0 && padBottom == 0 && padLeft == 0 && padRight == 0) {
            return copy();
        } else {
            if ((long) padTop + rowCount + padBottom > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Result row count overflow: " + padTop + " + " + rowCount + " + " + padBottom + " exceeds Integer.MAX_VALUE");
            }

            if ((long) padLeft + columnCount + padRight > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Result column count overflow: " + padLeft + " + " + columnCount + " + " + padRight + " exceeds Integer.MAX_VALUE");
            }

            final int newRowCount = padTop + rowCount + padBottom;
            final int newColumnCount = padLeft + columnCount + padRight;
            checkRepresentableShape(newRowCount, newColumnCount);
            final boolean fillDefaultValue = defaultValue != SHORT_0;
            final short[][] result = new short[newRowCount][newColumnCount];

            for (int i = 0; i < newRowCount; i++) {
                if (i >= padTop && i < padTop + rowCount) {
                    N.copy(a[i - padTop], 0, result[i], padLeft, columnCount);
                }

                if (fillDefaultValue) {
                    if (i < padTop || i >= padTop + rowCount) {
                        N.fill(result[i], defaultValue);
                    } else if (columnCount < newColumnCount) {
                        if (padLeft > 0) {
                            N.fill(result[i], 0, padLeft, defaultValue);
                        }

                        if (padRight > 0) {
                            N.fill(result[i], columnCount + padLeft, newColumnCount, defaultValue);
                        }
                    }
                }
            }

            return new ShortMatrix(result);
        }
    }

    /**
     * Reverses the order of elements in each row (horizontal flip in-place).
     * This operation modifies the matrix directly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.flipHorizontallyInPlace();
     * matrix.rowCopy(0);                    // returns [3, 2, 1] (each row reversed in place)
     * matrix.rowCopy(1);                    // returns [6, 5, 4]
     *
     * ShortMatrix single = ShortMatrix.of(new short[][] {{7}});
     * single.flipHorizontallyInPlace();
     * single.get(0, 0);                                // returns (short) 7 (single column unchanged)
     * ShortMatrix.empty().flipHorizontallyInPlace();   // no-op on empty matrix
     * }</pre>
     *
     * @see #flipHorizontally()
     */
    @Override
    public void flipHorizontallyInPlace() {
        for (int i = 0; i < rowCount; i++) {
            N.reverse(a[i]);
        }
    }

    /**
     * Reverses the order of rows in the matrix (vertical flip in-place).
     * This operation modifies the matrix directly by reversing the row order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.flipVerticallyInPlace();
     * matrix.rowCopy(0);                    // returns [7, 8, 9] (row order reversed in place)
     * matrix.rowCopy(2);                    // returns [1, 2, 3]
     *
     * ShortMatrix single = ShortMatrix.of(new short[][] {{1, 2}});
     * single.flipVerticallyInPlace();
     * single.rowCopy(0);                             // returns [1, 2] (single row unchanged)
     * ShortMatrix.empty().flipVerticallyInPlace();   // no-op on empty matrix
     * }</pre>
     *
     * @see #flipVertically()
     */
    @Override
    public void flipVerticallyInPlace() {
        for (int l = 0, h = rowCount - 1; l < h; l++, h--) {
            final short[] tmp = a[l];
            a[l] = a[h];
            a[h] = tmp;
        }
    }

    /**
     * Returns a new matrix that is a horizontal flip of this matrix (each row reversed).
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * ShortMatrix flipped = matrix.flipHorizontally();
     * flipped.rowCopy(0);                   // returns [3, 2, 1]
     * flipped.rowCopy(1);                   // returns [6, 5, 4]
     * matrix.get(0, 0);                     // returns (short) 1 (original unchanged)
     *
     * ShortMatrix.empty().flipHorizontally().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new matrix with each row reversed
     * @see #flipHorizontallyInPlace()
     * @see #flipVertically()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public ShortMatrix flipHorizontally() {
        final ShortMatrix res = this.copy();
        res.flipHorizontallyInPlace();
        return res;
    }

    /**
     * Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * ShortMatrix flipped = matrix.flipVertically();
     * flipped.rowCopy(0);                   // returns [4, 5, 6]
     * flipped.rowCopy(1);                   // returns [1, 2, 3]
     * matrix.rowCopy(0);                    // returns [1, 2, 3] (original unchanged)
     *
     * ShortMatrix.empty().flipVertically().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new matrix with rows in reversed order
     * @see #flipVerticallyInPlace()
     * @see #flipHorizontally()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public ShortMatrix flipVertically() {
        final ShortMatrix res = this.copy();
        res.flipVerticallyInPlace();
        return res;
    }

    /**
     * Returns a new matrix that is this matrix rotated 90 degrees clockwise.
     *
     * <p>The resulting matrix has dimensions swapped (rows x columns becomes columns x rows).
     * The element at position (i, j) in the original matrix appears at position (j, rows-1-i)
     * in the rotated matrix. The original matrix is not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix rotated = matrix.rotate90();
     * rotated.rowCopy(0);                    // returns [3, 1] (result is [[3, 1], [4, 2]])
     * rotated.rowCopy(1);                    // returns [4, 2]
     *
     * ShortMatrix rect = ShortMatrix.of(new short[][] {{1, 2, 3}});
     * ShortMatrix r = rect.rotate90();            // 1x3 becomes 3x1
     * r.rowCount();                               // returns 3
     * ShortMatrix.empty().rotate90().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new ShortMatrix rotated 90 degrees clockwise with dimensions columnCount × rowCount
     */
    @Override
    public ShortMatrix rotate90() {
        if (columnCount == 0) {
            return EMPTY_SHORT_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final short[][] result = new short[columnCount][rowCount];

        if (rowCount <= columnCount) {
            for (int j = 0; j < rowCount; j++) {
                for (int i = 0; i < columnCount; i++) {
                    result[i][j] = a[rowCount - j - 1][i];
                }
            }
        } else {
            for (int i = 0; i < columnCount; i++) {
                for (int j = 0; j < rowCount; j++) {
                    result[i][j] = a[rowCount - j - 1][i];
                }
            }
        }

        return new ShortMatrix(result);
    }

    /**
     * Returns a new matrix that is this matrix rotated 180 degrees.
     *
     * <p>The resulting matrix has the same dimensions as the original. The element at position (i, j)
     * in the original matrix appears at position (rows-1-i, columns-1-j) in the rotated matrix.
     * This is equivalent to reversing both row order and element order within each row.
     * The original matrix is not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix rotated = matrix.rotate180();
     * rotated.rowCopy(0);                    // returns [4, 3] (result is [[4, 3], [2, 1]])
     * rotated.rowCopy(1);                    // returns [2, 1]
     *
     * matrix.rotate180().rotate180().equals(matrix);   // returns true (two 180-rotations restore)
     * ShortMatrix.empty().rotate180().isEmpty();       // returns true
     * }</pre>
     *
     * @return a new ShortMatrix rotated 180 degrees with the same dimensions
     */
    @Override
    public ShortMatrix rotate180() {
        final short[][] result = new short[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            result[i] = a[rowCount - i - 1].clone();
            N.reverse(result[i]);
        }

        return new ShortMatrix(result);
    }

    /**
     * Returns a new matrix that is this matrix rotated 270 degrees clockwise.
     *
     * <p>This is equivalent to rotating 90 degrees counter-clockwise.
     * The resulting matrix has dimensions swapped (rows x columns becomes columns x rows).
     * The element at position (i, j) in the original matrix appears at position (columns-1-j, i)
     * in the rotated matrix. The original matrix is not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix rotated = matrix.rotate270();
     * rotated.rowCopy(0);                    // returns [2, 4] (result is [[2, 4], [1, 3]])
     * rotated.rowCopy(1);                    // returns [1, 3]
     *
     * ShortMatrix rect = ShortMatrix.of(new short[][] {{1, 2, 3}});
     * rect.rotate270().rowCount();                 // returns 3 (1x3 becomes 3x1)
     * ShortMatrix.empty().rotate270().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new ShortMatrix rotated 270 degrees clockwise with dimensions columnCount × rowCount
     */
    @Override
    public ShortMatrix rotate270() {
        if (columnCount == 0) {
            return EMPTY_SHORT_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final short[][] result = new short[columnCount][rowCount];

        if (rowCount <= columnCount) {
            for (int j = 0; j < rowCount; j++) {
                for (int i = 0; i < columnCount; i++) {
                    result[i][j] = a[j][columnCount - i - 1];
                }
            }
        } else {
            for (int i = 0; i < columnCount; i++) {
                for (int j = 0; j < rowCount; j++) {
                    result[i][j] = a[j][columnCount - i - 1];
                }
            }
        }

        return new ShortMatrix(result);
    }

    /**
     * Returns the transpose of this matrix by swapping rows and columns.
     *
     * <p>The transpose operation converts each row into a column, so element at position (i, j)
     * in the original matrix appears at position (j, i) in the transposed matrix. The resulting
     * matrix has dimensions swapped (rows x columns becomes columns x rows).
     * The original matrix is not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * ShortMatrix transposed = matrix.transpose();   // 2x3 becomes 3x2
     * transposed.rowCount();                         // returns 3
     * transposed.rowCopy(0);                         // returns [1, 4]
     * transposed.rowCopy(2);                         // returns [3, 6]
     *
     * matrix.transpose().transpose().equals(matrix);   // returns true (double transpose restores)
     * ShortMatrix.empty().transpose().isEmpty();       // returns true
     * }</pre>
     *
     * @return a new ShortMatrix that is the transpose with dimensions columnCount × rowCount;
     *         an {@code N x 0} matrix transposes to the empty {@code 0 x 0} matrix, because the swapped shape
     *         {@code 0 x N} (zero rows with a non-zero column count) is not representable
     */
    @Override
    public ShortMatrix transpose() {
        if (columnCount == 0) {
            return EMPTY_SHORT_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final short[][] result = new short[columnCount][rowCount];

        if (rowCount <= columnCount) {
            for (int j = 0; j < rowCount; j++) {
                for (int i = 0; i < columnCount; i++) {
                    result[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < columnCount; i++) {
                for (int j = 0; j < rowCount; j++) {
                    result[i][j] = a[j][i];
                }
            }
        }

        return new ShortMatrix(result);
    }

    /**
     * Reshapes the matrix to new dimensions while preserving element order in row-major layout.
     *
     * <p>Elements are read in row-major order from the original matrix and placed into the new shape.
     * The new shape must have at least as many total elements as the original
     * ({@code newRowCount * newColumnCount >= elementCount()}).
     * If the new shape has more total elements, the additional positions are filled with zeros (default value for short).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * ShortMatrix reshaped = matrix.reshape(3, 2);
     * reshaped.rowCopy(0);                  // returns [1, 2] (result is [[1, 2], [3, 4], [5, 6]])
     * reshaped.rowCopy(2);                  // returns [5, 6]
     *
     * ShortMatrix extended = matrix.reshape(2, 4);
     * extended.rowCopy(1);                  // returns [5, 6, 0, 0] (extra cells filled with 0)
     * matrix.reshape(1, 2);                 // throws IllegalArgumentException (too small for 6 elements)
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix (must be non-negative)
     * @param newColumnCount the number of columns in the reshaped matrix (must be non-negative)
     * @return a new ShortMatrix with the specified shape containing this matrix's elements in row-major order
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape cannot be represented (zero rows with non-zero columns),
     *         or if the new shape is too small to hold all elements
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public ShortMatrix reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final short[][] result = new short[newRowCount][newColumnCount];

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return new ShortMatrix(result);
        }

        final int rowLen = (int) N.min(newRowCount, elementCount % newColumnCount == 0 ? elementCount / newColumnCount : elementCount / newColumnCount + 1);

        if (a.length == 1) {
            for (int i = 0; i < rowLen; i++) {
                N.copy(a[0], i * newColumnCount, result[i], 0, (int) N.min(newColumnCount, elementCount - (long) i * newColumnCount));
            }
        } else {
            long cnt = 0;

            for (int i = 0; i < rowLen; i++) {
                for (int j = 0, col = (int) N.min(newColumnCount, elementCount - (long) i * newColumnCount); j < col; j++, cnt++) {
                    result[i][j] = a[(int) (cnt / columnCount)][(int) (cnt % columnCount)];
                }
            }
        }

        return new ShortMatrix(result);
    }

    /**
     * Repeats each element in the matrix by the specified factors.
     *
     * <p>Each element is repeated {@code rowRepeats} times in the row direction and {@code columnRepeats}
     * times in the column direction. This creates a new matrix where each original element becomes
     * a block of size rowRepeats × columnRepeats. The resulting matrix has dimensions
     * (rowCount * rowRepeats) × (columnCount * columnRepeats). The original matrix is not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix repeated = matrix.repeatElements(2, 3);
     * repeated.rowCount();                 // returns 4
     * repeated.columnCount();              // returns 6
     * repeated.rowCopy(0);                 // returns [1, 1, 1, 2, 2, 2]
     * repeated.rowCopy(2);                 // returns [3, 3, 3, 4, 4, 4]
     *
     * matrix.repeatElements(0, 3);        // throws IllegalArgumentException (repeats must be positive)
     * }</pre>
     *
     * @param rowRepeats the number of times to repeat each element in the row direction (must be positive)
     * @param columnRepeats the number of times to repeat each element in the column direction (must be positive)
     * @return a new ShortMatrix with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is less than or equal to 0,
     *         or if either result dimension would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#repeatElements(int, int)
     */
    @Override
    public ShortMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final short[][] result = new short[rowCount * rowRepeats][columnCount * columnRepeats];

        for (int i = 0; i < rowCount; i++) {
            final short[] sourceRow = a[i];
            final short[] firstRepeatedRow = result[i * rowRepeats];

            for (int j = 0; j < columnCount; j++) {
                N.copy(Array.repeat(sourceRow[j], columnRepeats), 0, firstRepeatedRow, j * columnRepeats, columnRepeats);
            }

            for (int k = 1; k < rowRepeats; k++) {
                N.copy(firstRepeatedRow, 0, result[i * rowRepeats + k], 0, firstRepeatedRow.length);
            }
        }

        return new ShortMatrix(result);
    }

    /**
     * Repeats the entire matrix as a tile pattern.
     *
     * <p>The whole matrix is repeated {@code rowRepeats} times in the row direction and {@code columnRepeats}
     * times in the column direction, creating a tiled pattern. The resulting matrix has dimensions
     * (rowCount * rowRepeats) × (columnCount * columnRepeats). This is different from {@link #repeatElements(int, int)} which
     * repeats individual elements. The original matrix is not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix tiled = matrix.repeatMatrix(2, 3);
     * tiled.rowCount();                   // returns 4
     * tiled.columnCount();                // returns 6
     * tiled.rowCopy(0);                   // returns [1, 2, 1, 2, 1, 2]
     * tiled.rowCopy(2);                   // returns [1, 2, 1, 2, 1, 2] (whole matrix tiled)
     *
     * matrix.repeatMatrix(2, 0);         // throws IllegalArgumentException (repeats must be positive)
     * }</pre>
     *
     * @param rowRepeats the number of times to repeat the matrix in the row direction (must be positive)
     * @param columnRepeats the number of times to repeat the matrix in the column direction (must be positive)
     * @return a new ShortMatrix with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)} containing the tiled pattern
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is less than or equal to 0,
     *         or if either result dimension would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#repeatMatrix(int, int)
     * @see #repeatElements(int, int)
     */
    @Override
    public ShortMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final short[][] result = new short[rowCount * rowRepeats][columnCount * columnRepeats];

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnRepeats; j++) {
                N.copy(a[i], 0, result[i], j * columnCount, columnCount);
            }
        }

        for (int i = 1; i < rowRepeats; i++) {
            for (int j = 0; j < rowCount; j++) {
                N.copy(result[j], 0, result[i * rowCount + j], 0, result[j].length);
            }
        }

        return new ShortMatrix(result);
    }

    /**
     * Flattens the matrix into a one-dimensional list in row-major order.
     *
     * <p>Elements are read row by row from left to right, top to bottom, and collected into a single
     * ShortList. The original matrix is not modified. This operation is useful for converting the
     * two-dimensional structure into a linear sequence for processing or transmission.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortList list = matrix.flatten();
     * list.size();                         // returns 4
     * list.get(0);                         // returns (short) 1 (list is [1, 2, 3, 4])
     * list.get(3);                         // returns (short) 4
     *
     * ShortMatrix.empty().flatten().size();   // returns 0
     * }</pre>
     *
     * @return a new ShortList containing all elements in row-major order
     * @throws IllegalStateException if the matrix is too large to flatten (more than Integer.MAX_VALUE elements)
     */
    @Override
    public ShortList flatten() {
        // Check for overflow before allocation
        if ((long) rowCount * columnCount > Integer.MAX_VALUE) {
            throw new IllegalStateException("Matrix too large to flatten: " + rowCount + " x " + columnCount);
        }

        final short[] result = new short[rowCount * columnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, result, i * columnCount, columnCount);
        }

        return ShortList.of(result);
    }

    /**
     * Flattens all elements of this matrix into a single one-dimensional array, applies the given
     * operation to that flattened array, and then copies the modified elements back into the matrix.
     *
     * <p>This enables operations that need a global view of all matrix elements (e.g., sorting all
     * elements across the entire matrix). The operation receives a temporary flattened copy; after
     * the operation completes, the modified values are written back into the matrix row by row.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{5, 3}, {4, 1}});
     * matrix.mutateAsFlat(arr -> java.util.Arrays.sort(arr));
     * matrix.rowCopy(0);                   // returns [1, 3] (sorted globally, placed back row by row)
     * matrix.rowCopy(1);                   // returns [4, 5]
     *
     * // negate every element via the flat view
     * ShortMatrix m2 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * m2.mutateAsFlat(arr -> { for (int i = 0; i < arr.length; i++) arr[i] = (short) -arr[i]; });
     * m2.get(0, 0);                                  // returns (short) -1
     * ShortMatrix.empty().mutateAsFlat(arr -> {});   // no-op on empty matrix
     * }</pre>
     *
     * @param <E> the type of exception that the operation may throw
     * @param action the operation to apply to the flattened array
     * @throws E if the operation throws an exception
     * @see Arrays#mutateAsFlat(short[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super short[], E> action) throws E {
        Arrays.mutateAsFlat(a, action);
    }

    /**
     * Vertically stacks this matrix with another matrix.
     * The two matrices must have the same number of columns.
     * The result is a new matrix where the rows of the specified matrix are appended below the rows of this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix1 = ShortMatrix.of(new short[][] {{1, 2, 3}});
     * ShortMatrix matrix2 = ShortMatrix.of(new short[][] {{4, 5, 6}, {7, 8, 9}});
     * ShortMatrix stacked = matrix1.stackVertically(matrix2);
     * stacked.rowCount();                 // returns 3
     * stacked.rowCopy(0);                 // returns [1, 2, 3]
     * stacked.rowCopy(2);                 // returns [7, 8, 9]
     *
     * ShortMatrix wrong = ShortMatrix.of(new short[][] {{1, 2}});
     * matrix1.stackVertically(wrong);                // throws IllegalArgumentException (column count mismatch)
     * matrix1.stackVertically((ShortMatrix) null);   // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * @param other the matrix to stack below this matrix; must not be {@code null}
     * @return a new matrix with rows from both matrices stacked vertically
     * @throws IllegalArgumentException if {@code other} is {@code null}, if the matrices don't have
     *         the same number of columns, or if the merged row count would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#stackVertically(IntMatrix)
     */
    @Override
    public ShortMatrix stackVertically(final ShortMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.columnCount, MSG_VSTACK_COLUMN_MISMATCH, columnCount, other.columnCount);
        final long mergedRowCount = (long) rowCount + other.rowCount;
        N.checkArgument(mergedRowCount <= Integer.MAX_VALUE, "Merged row count overflow: {} + {} = {}", rowCount, other.rowCount, mergedRowCount);

        final short[][] result = new short[(int) mergedRowCount][];
        int resultRowIndex = 0;

        for (int i = 0; i < rowCount; i++) {
            result[resultRowIndex++] = a[i].clone();
        }

        for (int i = 0; i < other.rowCount; i++) {
            result[resultRowIndex++] = other.a[i].clone();
        }

        return ShortMatrix.of(result);
    }

    /**
     * Horizontally stacks this matrix with another matrix.
     * The two matrices must have the same number of rows.
     * The result is a new matrix where the columns of the specified matrix are appended to the right of this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix1 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix matrix2 = ShortMatrix.of(new short[][] {{5}, {6}});
     * ShortMatrix stacked = matrix1.stackHorizontally(matrix2);
     * stacked.columnCount();               // returns 3
     * stacked.rowCopy(0);                  // returns [1, 2, 5]
     * stacked.rowCopy(1);                  // returns [3, 4, 6]
     *
     * ShortMatrix wrong = ShortMatrix.of(new short[][] {{5}});
     * matrix1.stackHorizontally(wrong);               // throws IllegalArgumentException (row count mismatch)
     * matrix1.stackHorizontally((ShortMatrix) null);  // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * @param other the matrix to stack to the right of this matrix; must not be {@code null}
     * @return a new matrix with columns from both matrices stacked horizontally
     * @throws IllegalArgumentException if {@code other} is {@code null}, if the matrices don't have
     *         the same number of rows, or if the merged column count would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#stackHorizontally(IntMatrix)
     */
    @Override
    public ShortMatrix stackHorizontally(final ShortMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(rowCount == other.rowCount, MSG_HSTACK_ROW_MISMATCH, rowCount, other.rowCount);
        final long mergedColumnCount = (long) columnCount + other.columnCount;
        N.checkArgument(mergedColumnCount <= Integer.MAX_VALUE, "Merged column count overflow: {} + {} = {}", columnCount, other.columnCount,
                mergedColumnCount);

        final short[][] result = new short[rowCount][(int) mergedColumnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, result[i], 0, columnCount);
            N.copy(other.a[i], 0, result[i], columnCount, other.columnCount);
        }

        return ShortMatrix.of(result);
    }

    /**
     * Performs element-wise addition of this matrix with another matrix.
     * The two matrices must have the same dimensions (same number of rows and columns).
     * The original matrices are not modified.
     * <p><b>Note:</b> Each pair of elements is added as {@code int} (Java numeric promotion) and the
     * result is narrowed back to {@code short} via an explicit cast, so values overflowing the short
     * range {@code [-32768, 32767]} wrap modulo 65536. If non-wrapping arithmetic is required, widen
     * via {@link #toIntMatrix()} (or {@link #toLongMatrix()}) before adding.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix1 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix matrix2 = ShortMatrix.of(new short[][] {{5, 6}, {7, 8}});
     * ShortMatrix sum = matrix1.add(matrix2);
     * sum.get(0, 0);                       // returns (short) 6 (result is [[6, 8], [10, 12]])
     * sum.get(1, 1);                       // returns (short) 12
     *
     * // short overflow wraps modulo 65536
     * ShortMatrix a = ShortMatrix.of(new short[][] {{32767}});
     * ShortMatrix b = ShortMatrix.of(new short[][] {{1}});
     * a.add(b).get(0, 0);                  // returns (short) -32768 (wraparound)
     *
     * ShortMatrix wrong = ShortMatrix.of(new short[][] {{1, 2, 3}});
     * matrix1.add(wrong);                  // throws IllegalArgumentException (shape mismatch)
     * }</pre>
     *
     * @param other the matrix to add to this matrix (must have same dimensions and not be {@code null})
     * @return a new matrix containing the element-wise sum
     * @throws IllegalArgumentException if {@code other} is {@code null}, or if the matrices don't have
     *         the same shape (same rows and columns)
     */
    public ShortMatrix add(final ShortMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot add matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final short[][] otherArray = other.a;
        final short[][] result = new short[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> operation = (i, j) -> result[i][j] = (short) (a[i][j] + otherArray[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return ShortMatrix.of(result);
    }

    /**
     * Performs element-wise subtraction of another matrix from this matrix.
     * The two matrices must have the same dimensions (same number of rows and columns).
     * The original matrices are not modified.
     * <p><b>Note:</b> Each pair of elements is subtracted as {@code int} (Java numeric promotion) and
     * the result is narrowed back to {@code short} via an explicit cast, so values outside the short
     * range {@code [-32768, 32767]} wrap modulo 65536. If non-wrapping arithmetic is required, widen
     * via {@link #toIntMatrix()} (or {@link #toLongMatrix()}) before subtracting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix1 = ShortMatrix.of(new short[][] {{5, 6}, {7, 8}});
     * ShortMatrix matrix2 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix diff = matrix1.subtract(matrix2);
     * diff.get(0, 0);                      // returns (short) 4 (result is [[4, 4], [4, 4]])
     * diff.get(1, 1);                      // returns (short) 4
     *
     * // short overflow wraps modulo 65536
     * ShortMatrix a = ShortMatrix.of(new short[][] {{-32768}});
     * ShortMatrix b = ShortMatrix.of(new short[][] {{1}});
     * a.subtract(b).get(0, 0);             // returns (short) 32767 (wraparound)
     *
     * ShortMatrix wrong = ShortMatrix.of(new short[][] {{1, 2, 3}});
     * matrix1.subtract(wrong);             // throws IllegalArgumentException (shape mismatch)
     * }</pre>
     *
     * @param other the matrix to subtract from this matrix (must have same dimensions and not be {@code null})
     * @return a new matrix containing the element-wise difference (this - other)
     * @throws IllegalArgumentException if {@code other} is {@code null}, or if the matrices don't have
     *         the same shape (same rows and columns)
     */
    public ShortMatrix subtract(final ShortMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot subtract matrices with different shapes: this is {}x{} but other is {}x{}", rowCount,
                columnCount, other.rowCount, other.columnCount);

        final short[][] otherArray = other.a;
        final short[][] result = new short[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> operation = (i, j) -> result[i][j] = (short) (a[i][j] - otherArray[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return ShortMatrix.of(result);
    }

    /**
     * Performs standard matrix multiplication with another matrix.
     * The number of columns in this matrix must equal the number of rows in the specified matrix.
     * The result is a new matrix with dimensions {@code (this.rowCount × other.columnCount)}.
     * The original matrices are not modified. This is standard matrix multiplication, not element-wise.
     *
     * <p><b>Note:</b> Short overflow may occur during multiplication and accumulation. Each partial
     * product {@code a[i][k] * other[k][j]} is computed as an {@code int} (via Java's numeric promotion),
     * but it is then accumulated into the {@code short} result cell with implicit narrowing, so
     * intermediate sums wrap modulo 65536 and the final result is always in the short range
     * {@code [-32768, 32767]}. If a non-wrapping product is required, widen via {@link #toIntMatrix()}
     * (or {@link #toLongMatrix()}) and multiply there.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix1 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix matrix2 = ShortMatrix.of(new short[][] {{5, 6}, {7, 8}});
     * ShortMatrix product = matrix1.matmul(matrix2);
     * product.get(0, 0);                   // returns (short) 19 (result is [[19, 22], [43, 50]])
     * product.get(1, 1);                   // returns (short) 50
     *
     * // non-square compatible shapes: (2x3) * (3x2) -> 2x2
     * ShortMatrix a = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * ShortMatrix b = ShortMatrix.of(new short[][] {{1, 0}, {0, 1}, {1, 1}});
     * a.matmul(b).get(0, 0);               // returns (short) 4 (1*1 + 2*0 + 3*1)
     *
     * ShortMatrix bad = ShortMatrix.of(new short[][] {{1, 2}});
     * matrix1.matmul(bad);                 // throws IllegalArgumentException (this.columnCount != other.rowCount)
     * matrix1.matmul((ShortMatrix) null);  // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * <p><b>Note:</b> This is the linear-algebra matrix product, not element-wise multiplication.
     * For element-wise multiplication use
     * {@link #zipWith(ShortMatrix, com.landawn.abacus.util.Throwables.ShortBinaryOperator)}.</p>
     *
     * @param other the matrix to multiply with this matrix ({@code this.columnCount} must equal {@code other.rowCount}); must not be {@code null}
     * @return a new matrix of dimension {@code (this.rowCount × other.columnCount)} containing the matrix product
     * @throws IllegalArgumentException if {@code other} is {@code null}, if
     *         {@code this.columnCount != other.rowCount} (incompatible dimensions for multiplication),
     *         or if this matrix has zero rows while {@code other} has a non-zero column count
     *         (the resulting shape is not representable)
     */
    public ShortMatrix matmul(final ShortMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.rowCount,
                "Matrix dimensions incompatible for multiplication: this is {}x{}, other is {}x{} (this.columnCount must equal other.rowCount)", rowCount,
                columnCount, other.rowCount, other.columnCount);

        checkRepresentableShape(rowCount, other.columnCount);

        final short[][] otherArray = other.a;
        final short[][] result = new short[rowCount][other.columnCount];
        final Throwables.IntTriConsumer<RuntimeException> cmd = (i, j, k) -> result[i][j] += a[i][k] * otherArray[k][j];

        Matrices.forEachCartesianIndices(this, other, cmd);

        return ShortMatrix.of(result);
    }

    /**
     * Converts this primitive short matrix to a boxed {@code Matrix<Short>}.
     * Each primitive short value is boxed into a {@code Short} wrapper object.
     * This is the inverse operation of {@link #unbox(Matrix)}.
     *
     * <p><b>Note:</b> Boxing creates wrapper objects which have additional memory overhead compared to primitives.
     * Use this method only when you need to work with generic Matrix API or when {@code null} values are required.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix primitive = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * Matrix<Short> boxed = primitive.boxed();
     * boxed.get(0, 1);                     // returns Short.valueOf((short) 2)
     * boxed.rowCount();                    // returns 2
     *
     * ShortMatrix.unbox(boxed).equals(primitive);   // returns true (round-trips back)
     * ShortMatrix.empty().boxed().isEmpty();        // returns true
     * }</pre>
     *
     * @return a new {@code Matrix<Short>} containing boxed values
     * @see #unbox(Matrix)
     */
    public Matrix<Short> boxed() {
        final Short[][] c = new Short[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final short[] aa = a[i];
                final Short[] cc = c[i];

                for (int j = 0; j < columnCount; j++) {
                    cc[j] = aa[j]; // NOSONAR
                }
            }
        } else {
            for (int j = 0; j < columnCount; j++) {
                for (int i = 0; i < rowCount; i++) {
                    c[i][j] = a[i][j];
                }
            }
        }

        return new Matrix<>(c);
    }

    /**
     * Converts this short matrix to an int matrix.
     * Each short value is promoted to an int value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix shortMatrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * IntMatrix intMatrix = shortMatrix.toIntMatrix();
     * intMatrix.get(0, 1);                 // returns 2
     * intMatrix.get(1, 1);                 // returns 4
     *
     * // widen first to avoid short overflow in subsequent arithmetic
     * ShortMatrix big = ShortMatrix.of(new short[][] {{30000}});
     * big.toIntMatrix().get(0, 0);                   // returns 30000
     * ShortMatrix.empty().toIntMatrix().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new {@code IntMatrix} with values converted from short to int
     */
    public IntMatrix toIntMatrix() {
        return IntMatrix.from(a);
    }

    /**
     * Converts this short matrix to a long matrix.
     * Each short value is promoted to a long value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix shortMatrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * LongMatrix longMatrix = shortMatrix.toLongMatrix();
     * longMatrix.get(0, 1);               // returns 2L
     * longMatrix.get(1, 1);               // returns 4L
     *
     * ShortMatrix neg = ShortMatrix.of(new short[][] {{-5}});
     * neg.toLongMatrix().get(0, 0);                   // returns -5L (sign preserved)
     * ShortMatrix.empty().toLongMatrix().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new {@code LongMatrix} with values converted from short to long
     */
    public LongMatrix toLongMatrix() {
        final long[][] c = new long[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final short[] aa = a[i];
                final long[] cc = c[i];

                for (int j = 0; j < columnCount; j++) {
                    cc[j] = aa[j]; // NOSONAR
                }
            }
        } else {
            for (int j = 0; j < columnCount; j++) {
                for (int i = 0; i < rowCount; i++) {
                    c[i][j] = a[i][j];
                }
            }
        }

        return new LongMatrix(c);
    }

    /**
     * Converts this short matrix to a float matrix.
     * Each short value is converted to a float value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix shortMatrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * FloatMatrix floatMatrix = shortMatrix.toFloatMatrix();
     * floatMatrix.get(0, 1);              // returns 2.0f
     * floatMatrix.get(1, 1);              // returns 4.0f
     *
     * ShortMatrix neg = ShortMatrix.of(new short[][] {{-7}});
     * neg.toFloatMatrix().get(0, 0);                   // returns -7.0f
     * ShortMatrix.empty().toFloatMatrix().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new {@code FloatMatrix} with values converted from short to float
     */
    public FloatMatrix toFloatMatrix() {
        final float[][] c = new float[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final short[] aa = a[i];
                final float[] cc = c[i];

                for (int j = 0; j < columnCount; j++) {
                    cc[j] = aa[j]; // NOSONAR
                }
            }
        } else {
            for (int j = 0; j < columnCount; j++) {
                for (int i = 0; i < rowCount; i++) {
                    c[i][j] = a[i][j];
                }
            }
        }

        return new FloatMatrix(c);
    }

    /**
     * Converts this short matrix to a double matrix.
     * Each short value is converted to a double value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix shortMatrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * DoubleMatrix doubleMatrix = shortMatrix.toDoubleMatrix();
     * doubleMatrix.get(0, 1);             // returns 2.0
     * doubleMatrix.get(1, 1);             // returns 4.0
     *
     * ShortMatrix neg = ShortMatrix.of(new short[][] {{-7}});
     * neg.toDoubleMatrix().get(0, 0);                   // returns -7.0
     * ShortMatrix.empty().toDoubleMatrix().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new {@code DoubleMatrix} with values converted from short to double
     */
    public DoubleMatrix toDoubleMatrix() {
        final double[][] c = new double[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final short[] aa = a[i];
                final double[] cc = c[i];

                for (int j = 0; j < columnCount; j++) {
                    cc[j] = aa[j]; // NOSONAR
                }
            }
        } else {
            for (int j = 0; j < columnCount; j++) {
                for (int i = 0; i < rowCount; i++) {
                    c[i][j] = a[i][j];
                }
            }
        }

        return new DoubleMatrix(c);
    }

    /**
     * Applies a binary operation element-wise to this matrix and another matrix.
     * The two matrices must have the same dimensions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix1 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix matrix2 = ShortMatrix.of(new short[][] {{5, 6}, {7, 8}});
     * ShortMatrix max = matrix1.zipWith(matrix2, (a, b) -> (short) Math.max(a, b));
     * max.get(0, 0);                       // returns (short) 5 (result is [[5, 6], [7, 8]])
     * max.get(1, 1);                       // returns (short) 8
     *
     * // element-wise product
     * ShortMatrix prod = matrix1.zipWith(matrix2, (a, b) -> (short) (a * b));
     * prod.get(0, 0);                      // returns (short) 5 (1 * 5)
     *
     * ShortMatrix wrong = ShortMatrix.of(new short[][] {{1, 2, 3}});
     * matrix1.zipWith(wrong, (a, b) -> a); // throws IllegalArgumentException (shape mismatch)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix to zip with this matrix; must not be {@code null}
     * @param zipFunction the binary operation to apply to corresponding elements; must not be {@code null}
     * @return a new matrix with the results of the zip operation
     * @throws IllegalArgumentException if {@code other} or {@code zipFunction} is {@code null},
     *         or if the matrices do not have the same shape
     * @throws E if the zip function throws an exception
     */
    public <E extends Exception> ShortMatrix zipWith(final ShortMatrix other, final Throwables.ShortBinaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);
        N.checkArgNotNull(zipFunction, "zipFunction");

        final short[][] arrayB = other.a;
        final short[][] result = new short[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = zipFunction.applyAsShort(a[i][j], arrayB[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return ShortMatrix.of(result);
    }

    /**
     * Applies a ternary operation element-wise to this matrix and two other matrices.
     * All three matrices must have the same dimensions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix1 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix matrix2 = ShortMatrix.of(new short[][] {{5, 6}, {7, 8}});
     * ShortMatrix matrix3 = ShortMatrix.of(new short[][] {{9, 10}, {11, 12}});
     * ShortMatrix average = matrix1.zipWith(matrix2, matrix3, (a, b, c) -> (short) ((a + b + c) / 3));
     * average.get(0, 0);                   // returns (short) 5 (result is [[5, 6], [7, 8]])
     * average.get(1, 1);                   // returns (short) 8
     *
     * // sum of three matrices, element-wise
     * ShortMatrix sum = matrix1.zipWith(matrix2, matrix3, (a, b, c) -> (short) (a + b + c));
     * sum.get(0, 0);                       // returns (short) 15 (1 + 5 + 9)
     *
     * ShortMatrix wrong = ShortMatrix.of(new short[][] {{1, 2, 3}});
     * matrix1.zipWith(matrix2, wrong, (a, b, c) -> a);   // throws IllegalArgumentException (shape mismatch)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix to zip with; must not be {@code null}
     * @param third the third matrix to zip with; must not be {@code null}
     * @param zipFunction the ternary operation to apply to corresponding elements from all three matrices; must not be {@code null}
     * @return a new matrix with the results of the zip operation
     * @throws IllegalArgumentException if {@code other}, {@code third}, or {@code zipFunction} is {@code null},
     *         or if the three matrices do not all have the same shape
     * @throws E if the zip function throws an exception
     */
    public <E extends Exception> ShortMatrix zipWith(final ShortMatrix other, final ShortMatrix third, final Throwables.ShortTernaryOperator<E> zipFunction)
            throws E {
        N.checkArgument(isSameShape(other) && isSameShape(third), "Cannot zip matrices with different shapes: all matrices must be {}x{}", rowCount,
                columnCount);
        N.checkArgNotNull(zipFunction, "zipFunction");

        final short[][] arrayB = other.a;
        final short[][] arrayC = third.a;
        final short[][] result = new short[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = zipFunction.applyAsShort(a[i][j], arrayB[i][j], arrayC[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return ShortMatrix.of(result);
    }

    /**
     * Returns a stream of elements on the main diagonal from upper-left to lower-right.
     *
     * <p>The matrix must be square (same number of rows and columns). The stream contains elements
     * at positions (0,0), (1,1), (2,2), ..., (n-1,n-1) where n is the matrix dimension.
     * This is the primary diagonal running from top-left to bottom-right.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.mainDiagonalStream().toArray();   // returns [1, 5, 9]
     * matrix.mainDiagonalStream().count();     // returns 3L
     *
     * ShortMatrix.empty().mainDiagonalStream().count();   // returns 0L (empty -> empty stream)
     *
     * ShortMatrix rect = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * rect.mainDiagonalStream();               // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a ShortStream of diagonal elements from upper-left to lower-right
     * @throws IllegalStateException if the matrix is non-empty and not square (rowCount != columnCount)
     */
    @Override
    public ShortStream mainDiagonalStream() {
        if (isEmpty()) {
            return ShortStream.empty();
        }

        checkIsSquare();

        return ShortStream.of(new ShortIteratorEx() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public short nextShort() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor][cursor++];
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor; // NOSONAR
            }
        });
    }

    /**
     * Returns a stream of elements on the anti-diagonal from upper-right to lower-left.
     *
     * <p>The matrix must be square (same number of rows and columns). The stream contains elements
     * at positions (0,n-1), (1,n-2), (2,n-3), ..., (n-1,0) where n is the matrix dimension.
     * This is the secondary diagonal running from upper-right to lower-left.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.antiDiagonalStream().toArray();   // returns [3, 5, 7]
     * matrix.antiDiagonalStream().count();     // returns 3L
     *
     * ShortMatrix.empty().antiDiagonalStream().count();   // returns 0L (empty -> empty stream)
     *
     * ShortMatrix rect = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * rect.antiDiagonalStream();               // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a ShortStream of anti-diagonal elements from upper-right to lower-left
     * @throws IllegalStateException if the matrix is non-empty and not square (rowCount != columnCount)
     */
    @Override
    public ShortStream antiDiagonalStream() {
        if (isEmpty()) {
            return ShortStream.empty();
        }

        checkIsSquare();

        return ShortStream.of(new ShortIteratorEx() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public short nextShort() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final short result = a[cursor][columnCount - cursor - 1];
                cursor++;
                return result;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor; // NOSONAR
            }
        });
    }

    /**
     * Returns a stream of all elements in this matrix, traversed horizontally (left to right, top to bottom).
     *
     * <p>Elements are streamed row by row from left to right, top to bottom. This is the most common
     * streaming order for matrix traversal and corresponds to the natural iteration order of the
     * underlying two-dimensional array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.horizontalStream().toArray();   // returns [1, 2, 3, 4, 5, 6]
     * matrix.horizontalStream().count();     // returns 6L
     *
     * ShortMatrix.empty().horizontalStream().count();   // returns 0L
     * ShortMatrix single = ShortMatrix.of(new short[][] {{7}});
     * single.horizontalStream().toArray();   // returns [7]
     * }</pre>
     *
     * @return a ShortStream of all matrix elements in row-major order
     */
    @Override
    public ShortStream horizontalStream() {
        return horizontalStream(0, rowCount);
    }

    /**
     * Returns a stream of elements from a specific row.
     *
     * <p>All elements in the specified row are streamed from left to right (column index 0 to columnCount-1).
     * This is equivalent to calling {@code horizontalStream(rowIndex, rowIndex + 1)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.horizontalStream(1).toArray();   // returns [4, 5, 6]
     * matrix.horizontalStream(0).toArray();   // returns [1, 2, 3]
     *
     * matrix.horizontalStream(2);             // throws IndexOutOfBoundsException (row out of bounds)
     * matrix.horizontalStream(-1);            // throws IndexOutOfBoundsException (negative index)
     * }</pre>
     *
     * @param rowIndex the index of the row to stream (0-based)
     * @return a ShortStream of elements from the specified row
     * @throws IndexOutOfBoundsException if the row index is out of bounds
     */
    @Override
    public ShortStream horizontalStream(final int rowIndex) {
        return horizontalStream(rowIndex, rowIndex + 1);
    }

    /**
     * Returns a stream of elements from a range of rows in row-major order.
     *
     * <p>Elements from the specified rows are streamed row by row from left to right, top to bottom.
     * Each complete row is streamed before moving to the next row within the specified range.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.horizontalStream(1, 3).toArray();   // returns [3, 4, 5, 6]
     * matrix.horizontalStream(0, 1).toArray();   // returns [1, 2]
     *
     * matrix.horizontalStream(1, 1).count();     // returns 0L (empty range)
     * matrix.horizontalStream(0, 5);             // throws IndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a ShortStream of elements from the specified row range in row-major order
     * @throws IndexOutOfBoundsException if the row indices are out of bounds or fromRowIndex &gt; toRowIndex
     */
    @Override
    public ShortStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        if (isEmpty()) {
            return ShortStream.empty();
        }

        return ShortStream.of(new ShortIteratorEx() {
            private int i = fromRowIndex;
            private int j = 0;

            @Override
            public boolean hasNext() {
                return i < toRowIndex;
            }

            @Override
            public short nextShort() {
                if (i >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final short result = a[i][j++];

                if (j >= columnCount) {
                    i++;
                    j = 0;
                }

                return result;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (n >= (long) (toRowIndex - i) * columnCount - j) {
                    i = toRowIndex;
                    j = 0;
                } else {
                    i += (int) ((n + j) / columnCount);
                    j = (int) ((n + j) % columnCount);
                }
            }

            @Override
            public long count() {
                return (long) (toRowIndex - i) * columnCount - j;
            }

            @Override
            public short[] toArray() {
                final int arrayLength = toArrayLength(count());
                final short[] result = new short[arrayLength];

                for (int k = 0; k < arrayLength; k++) {
                    result[k] = a[i][j++];

                    if (j >= columnCount) {
                        i++;
                        j = 0;
                    }
                }

                return result;
            }
        });
    }

    /**
     * Returns a stream of all elements in this matrix, traversed vertically (top to bottom, left to right).
     *
     * <p>Elements are streamed column by column from top to bottom, left to right. This traversal
     * order processes all elements in the first column, then all elements in the second column, and so on.
     * This is the opposite of the more common row-major order used by {@link #horizontalStream()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.verticalStream().toArray();   // returns [1, 4, 2, 5, 3, 6]
     * matrix.verticalStream().count();     // returns 6L
     *
     * ShortMatrix.empty().verticalStream().count();   // returns 0L
     * ShortMatrix single = ShortMatrix.of(new short[][] {{7}});
     * single.verticalStream().toArray();   // returns [7]
     * }</pre>
     *
     * @return a ShortStream of all matrix elements in column-major order
     */
    @Override
    @Beta
    public ShortStream verticalStream() {
        return verticalStream(0, columnCount);
    }

    /**
     * Returns a stream of elements from a specific column.
     *
     * <p>All elements in the specified column are streamed from top to bottom (row index 0 to rows-1).
     * This is equivalent to calling {@code verticalStream(columnIndex, columnIndex + 1)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.verticalStream(1).toArray();   // returns [2, 5]
     * matrix.verticalStream(0).toArray();   // returns [1, 4]
     *
     * matrix.verticalStream(3);             // throws IndexOutOfBoundsException (column out of bounds)
     * matrix.verticalStream(-1);            // throws IndexOutOfBoundsException (negative index)
     * }</pre>
     *
     * @param columnIndex the index of the column to stream (0-based)
     * @return a ShortStream of elements from the specified column
     * @throws IndexOutOfBoundsException if the column index is out of bounds
     */
    @Override
    public ShortStream verticalStream(final int columnIndex) {
        return verticalStream(columnIndex, columnIndex + 1);
    }

    /**
     * Returns a stream of elements from a range of columns in column-major order.
     *
     * <p>Elements from the specified columns are streamed column by column from top to bottom, left to right.
     * Each complete column is streamed before moving to the next column within the specified range.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.verticalStream(1, 3).toArray();   // returns [2, 5, 3, 6]
     * matrix.verticalStream(0, 1).toArray();   // returns [1, 4]
     *
     * matrix.verticalStream(1, 1).count();     // returns 0L (empty range)
     * matrix.verticalStream(0, 5);             // throws IndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a ShortStream of elements from the specified column range in column-major order
     * @throws IndexOutOfBoundsException if the column indices are out of bounds or fromColumnIndex &gt; toColumnIndex
     */
    @Override
    @Beta
    public ShortStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        if (isEmpty()) {
            return ShortStream.empty();
        }

        return ShortStream.of(new ShortIteratorEx() {
            private int i = 0;
            private int j = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return j < toColumnIndex;
            }

            @Override
            public short nextShort() {
                if (j >= toColumnIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final short result = a[i++][j];

                if (i >= rowCount) {
                    i = 0;
                    j++;
                }

                return result;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (n >= (long) (toColumnIndex - j) * ShortMatrix.this.rowCount - i) {
                    i = 0;
                    j = toColumnIndex;
                } else {
                    final long offset = n + i;
                    i = (int) (offset % ShortMatrix.this.rowCount);
                    j += (int) (offset / ShortMatrix.this.rowCount);
                }
            }

            @Override
            public long count() {
                return (long) (toColumnIndex - j) * rowCount - i; // NOSONAR
            }

            @Override
            public short[] toArray() {
                final int arrayLength = toArrayLength(count());
                final short[] result = new short[arrayLength];

                for (int k = 0; k < arrayLength; k++) {
                    result[k] = a[i++][j];

                    if (i >= rowCount) {
                        i = 0;
                        j++;
                    }
                }

                return result;
            }
        });
    }

    /**
     * Returns a stream of row streams, where each element is a stream representing a complete row.
     *
     * <p>Rows are streamed in order from top to bottom. This method is useful for processing the matrix
     * row-by-row where each row needs to be handled as a separate stream. Each row stream contains
     * all elements in that row from left to right.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.rowStreams().count();                                     // returns 2L (one stream per row)
     * matrix.rowStreams().map(ShortStream::toArray).toList().get(0);   // returns [1, 2, 3]
     *
     * ShortMatrix.empty().rowStreams().count();   // returns 0L
     * ShortMatrix single = ShortMatrix.of(new short[][] {{7, 8}});
     * single.rowStreams().count();                // returns 1L
     * }</pre>
     *
     * @return a Stream of ShortStream objects, one for each row
     */
    @Override
    public Stream<ShortStream> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Returns a stream of row streams from a range of rows.
     *
     * <p>Each element in the returned stream is a ShortStream representing a complete row within the
     * specified range. Rows are streamed in order within the range. Each row stream contains all
     * elements in that row from left to right.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.rowStreams(1, 3).count();                                     // returns 2L
     * matrix.rowStreams(1, 3).map(ShortStream::toArray).toList().get(0);   // returns [3, 4]
     *
     * matrix.rowStreams(1, 1).count();   // returns 0L (empty range)
     * matrix.rowStreams(0, 4);           // throws IndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of ShortStream objects for rows in the specified range
     * @throws IndexOutOfBoundsException if the row indices are out of bounds or fromRowIndex &gt; toRowIndex
     */
    @Override
    public Stream<ShortStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public ShortStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return ShortStream.of(a[cursor++]);
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor; // NOSONAR
            }
        });
    }

    /**
     * Returns a stream of column streams, where each element is a stream representing a complete column.
     *
     * <p>Columns are streamed in order from left to right. This method is useful for processing the matrix
     * column-by-column where each column needs to be handled as a separate stream. Each column stream
     * contains all elements in that column from top to bottom.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.columnStreams().count();                                     // returns 3L (one stream per column)
     * matrix.columnStreams().map(ShortStream::toArray).toList().get(0);   // returns [1, 4]
     *
     * ShortMatrix.empty().columnStreams().count();   // returns 0L
     * ShortMatrix single = ShortMatrix.of(new short[][] {{7}, {8}});
     * single.columnStreams().count();                // returns 1L
     * }</pre>
     *
     * @return a Stream of ShortStream objects, one for each column
     */
    @Override
    @Beta
    public Stream<ShortStream> columnStreams() {
        return columnStreams(0, columnCount);
    }

    /**
     * Returns a stream of column streams from a range of columns.
     *
     * <p>Each element in the returned stream is a ShortStream representing a complete column within the
     * specified range. Columns are streamed in order within the range. Each column stream contains all
     * elements in that column from top to bottom.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.columnStreams(1, 3).count();                                     // returns 2L
     * matrix.columnStreams(1, 3).map(ShortStream::toArray).toList().get(0);   // returns [2, 5]
     *
     * matrix.columnStreams(1, 1).count();   // returns 0L (empty range)
     * matrix.columnStreams(0, 5);           // throws IndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of ShortStream objects for columns in the specified range
     * @throws IndexOutOfBoundsException if the column indices are out of bounds or fromColumnIndex &gt; toColumnIndex
     */
    @Override
    @Beta
    public Stream<ShortStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        if (isEmpty()) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toColumnIndex;
            private int cursor = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public ShortStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return ShortStream.of(new ShortIteratorEx() {
                    private final int columnIndex = cursor++;
                    private final int toIndex2 = rowCount;
                    private int cursor2 = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor2 < toIndex2;
                    }

                    @Override
                    public short nextShort() {
                        if (cursor2 >= toIndex2) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return a[cursor2++][columnIndex];
                    }

                    @Override
                    public void advance(final long n) {
                        if (n <= 0) {
                            return;
                        }

                        cursor2 = n < toIndex2 - cursor2 ? cursor2 + (int) n : toIndex2;
                    }

                    @Override
                    public long count() {
                        return toIndex2 - cursor2; // NOSONAR
                    }
                });
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long count() {
                return toIndex - cursor; // NOSONAR
            }
        });
    }

    /**
     * Returns the length of the specified array.
     * This is an internal helper method.
     *
     * @param a the array to get the length of
     * @return the length of the array, or 0 if the array is {@code null}
     */
    @Override
    protected int length(@SuppressWarnings("hiding") final short[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Performs the specified action for each element in this matrix.
     * Elements are processed in row-major order (left to right, top to bottom).
     * This operation may be performed in parallel for large matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * int[] sum = {0};
     * matrix.forEach(value -> sum[0] += value);
     * // sum[0] == 10 (1 + 2 + 3 + 4, visited in row-major order)
     *
     * int[] count = {0};
     * ShortMatrix.empty().forEach(value -> count[0]++);
     * // count[0] == 0 (no elements in an empty matrix)
     * matrix.forEach(null);   // throws IllegalArgumentException (null action)
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the consumer to apply to each element
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final Throwables.ShortConsumer<E> action) throws E {
        forEach(0, rowCount, 0, columnCount, action);
    }

    /**
     * Performs the specified action for each element in a rectangular sub-region of this matrix.
     * Elements are processed in row-major order (left to right, top to bottom) within the specified bounds when executed sequentially.
     *
     * <p>This operation may be parallelized internally if the sub-matrix is large enough
     * to benefit from parallel processing; if parallelized, the order in which elements are
     * visited is unspecified and the action must be thread-safe, but every element is still
     * visited exactly once.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * int[] sum = {0};
     * matrix.forEach(1, 3, 1, 3, value -> sum[0] += value);
     * // sum[0] == 28 (processes 5, 6, 8, 9 in rows 1-2, columns 1-2)
     *
     * int[] count = {0};
     * matrix.forEach(0, 0, 0, 3, value -> count[0]++);
     * // count[0] == 0 (empty row range)
     * matrix.forEach(0, 5, 0, 3, value -> {});   // throws IndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @param action the consumer to apply to each element in the region
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws IndexOutOfBoundsException if any index is out of bounds or fromIndex &gt; toIndex
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex,
            final Throwables.ShortConsumer<E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        if (Matrices.isParallelizable(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> operation = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, operation, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                final short[] aa = a[i];

                for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                    action.accept(aa[j]);
                }
            }
        }
    }

    /**
     * Prints this matrix to standard output and returns the formatted string.
     *
     * <p>Each row is formatted as {@code [e1, e2, ...]} and rows are separated by
     * {@link #ARRAY_PRINT_SEPARATOR}. If the matrix is empty, {@code []} is printed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.println();              // returns "[1, 2, 3]" + separator + "[4, 5, 6]" (and prints it)
     *
     * ShortMatrix single = ShortMatrix.of(new short[][] {{7}});
     * single.println();                // returns "[7]"
     * ShortMatrix.empty().println();   // returns "[]" (prints "[]" for an empty matrix)
     * }</pre>
     *
     * @return the formatted string representation of the matrix
     */
    @Override
    public String println() {
        if (a.length == 0) {
            return N.println("[]");
        } else {
            final StringBuilder sb = Objectory.createStringBuilder();
            final int len = a.length;
            String str = "";

            try {
                for (int i = 0; i < len; i++) {
                    if (i > 0) {
                        sb.append(ARRAY_PRINT_SEPARATOR);
                    }

                    final short[] ai = a[i];
                    sb.append('[');

                    for (int j = 0, rowLen = ai.length; j < rowLen; j++) {
                        if (j > 0) {
                            sb.append(", ");
                        }

                        sb.append(ai[j]);
                    }

                    sb.append(']');
                }

                str = sb.toString();
            } finally {
                Objectory.recycle(sb);
            }

            return N.println(str);
        }
    }

    /**
     * Returns a hash code value for this matrix.
     * The hash code is computed based on the deep contents of the internal two-dimensional array.
     * Matrices with the same dimensions and element values will have equal hash codes,
     * consistent with the {@link #equals(Object)} method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix m1 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix m2 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * m1.hashCode() == m2.hashCode();   // returns true (equal matrices share a hash code)
     *
     * ShortMatrix m3 = ShortMatrix.of(new short[][] {{1, 2}, {3, 5}});
     * m1.hashCode() == m3.hashCode();   // typically returns false (different contents)
     * ShortMatrix.empty().hashCode();   // returns a stable hash for the empty matrix
     * }</pre>
     *
     * @return a hash code value for this matrix
     */
    @Override
    public int hashCode() {
        return N.deepHashCode(a);
    }

    /**
     * Compares this matrix to the specified object for equality.
     * Returns {@code true} if the given object is also a ShortMatrix with the same dimensions
     * and all corresponding elements are equal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix m1 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix m2 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * m1.equals(m2);                                          // returns true (same shape and values)
     * m1.equals(m1);                                          // returns true (same instance)
     *
     * m1.equals(ShortMatrix.of(new short[][] {{1, 2}, {3, 5}}));   // returns false (different value)
     * m1.equals(ShortMatrix.of(new short[][] {{1, 2, 3}}));        // returns false (different shape)
     * m1.equals("not a matrix");                                   // returns false (different type)
     * m1.equals(null);                                             // returns false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final ShortMatrix another) {
            return columnCount == another.columnCount && rowCount == another.rowCount && N.deepEquals(a, another.a);
        }

        return false;
    }

    /**
     * Returns a string representation of this matrix.
     * The format consists of matrix elements in a two-dimensional array format with rows enclosed in brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.toString();                  // returns "[[1, 2], [3, 4]]"
     *
     * ShortMatrix single = ShortMatrix.of(new short[][] {{7}});
     * single.toString();                 // returns "[[7]]"
     * ShortMatrix.empty().toString();    // returns "[]"
     * }</pre>
     *
     * @return a string representation of this matrix
     */
    @Override
    public String toString() {
        return N.deepToString(a);
    }
}
