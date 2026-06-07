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
 * @see LongMatrix
 * @see DoubleMatrix
 * @see FloatMatrix
 * @see ByteMatrix
 * @see CharMatrix
 * @see BooleanMatrix
 * @see Matrix
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
     * Returns the shared empty matrix with zero rows and zero columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.empty();
     * matrix.rowCount();                           // returns 0
     * matrix.columnCount();                        // returns 0
     * matrix.isEmpty();                            // returns true
     * matrix.elementCount();                       // returns 0L
     * ShortMatrix.empty() == ShortMatrix.empty();  // true (same shared singleton)
     * }</pre>
     *
     * @return the shared empty {@code ShortMatrix} singleton
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
     * @return a new {@code ShortMatrix} wrapping the provided data, or the shared empty {@code ShortMatrix} if input is {@code null} or empty
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public static ShortMatrix of(final short[]... a) {
        return N.isEmpty(a) ? EMPTY_SHORT_MATRIX : new ShortMatrix(a);
    }

    /**
     * Creates a new {@code 1 x length} matrix filled with pseudo-random {@code short} values
     * drawn uniformly from the entire {@code short} range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.random(5);
     * matrix.rowCount();          // returns 1
     * matrix.columnCount();       // returns 5
     *
     * ShortMatrix.random(0).columnCount();   // returns 0 (1x0 matrix)
     * ShortMatrix.random(-1);                // throws IllegalArgumentException (negative length)
     * }</pre>
     *
     * @param length the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code ShortMatrix} of dimensions {@code 1 x length} filled with random values
     * @throws IllegalArgumentException if {@code length} is negative
     * @see #random(int, int)
     */
    public static ShortMatrix random(final int length) {
        N.checkArgument(length >= 0, MSG_NEGATIVE_DIMENSION, "length", length);

        return random(1, length);
    }

    /**
     * Creates a new matrix of the specified dimensions filled with pseudo-random {@code short} values
     * drawn uniformly from the entire {@code short} range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.random(2, 3);
     * matrix.rowCount();          // returns 2
     * matrix.columnCount();       // returns 3
     *
     * ShortMatrix.random(0, 0).isEmpty();    // returns true
     * ShortMatrix.random(2, -1);             // throws IllegalArgumentException (negative columnCount)
     * ShortMatrix.random(-1, 2);             // throws IllegalArgumentException (negative rowCount)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code ShortMatrix} of dimensions {@code rowCount x columnCount} filled with random values
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if the resulting shape is not representable
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
     * All other elements are set to zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.mainDiagonal(new short[] {1, 2, 3});
     * matrix.get(0, 0);                       // returns (short) 1
     * matrix.get(1, 1);                       // returns (short) 2
     * matrix.get(0, 1);                       // returns (short) 0 (off-diagonal)
     * // matrix is [[1, 0, 0], [0, 2, 0], [0, 0, 3]]
     *
     * ShortMatrix.mainDiagonal((short[]) null);             // throws IllegalArgumentException (null array)
     * ShortMatrix.mainDiagonal(new short[0]).isEmpty();     // returns true
     * }</pre>
     *
     * @param mainDiagonal the array of main-diagonal elements; must not be {@code null}, but may be empty
     * @return a new {@code n x n} {@code ShortMatrix} (where {@code n = mainDiagonal.length}) with
     *         the supplied values on the main diagonal and {@code 0} elsewhere; the shared empty
     *         matrix if {@code mainDiagonal} is empty
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null}
     * @see #antiDiagonal(short[])
     * @see #diagonals(short[], short[])
     */
    public static ShortMatrix mainDiagonal(final short[] mainDiagonal) {
        return diagonals(mainDiagonal, null);
    }

    /**
     * Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
     * All other elements are set to zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.antiDiagonal(new short[] {1, 2, 3});
     * matrix.get(0, 2);                       // returns (short) 1
     * matrix.get(2, 0);                       // returns (short) 3
     * matrix.get(0, 0);                       // returns (short) 0 (off anti-diagonal)
     * // matrix is [[0, 0, 1], [0, 2, 0], [3, 0, 0]]
     *
     * ShortMatrix.antiDiagonal((short[]) null);             // throws IllegalArgumentException (null array)
     * ShortMatrix.antiDiagonal(new short[0]).isEmpty();     // returns true
     * }</pre>
     *
     * @param antiDiagonal the array of anti-diagonal elements; must not be {@code null}, but may be empty
     * @return a new {@code n x n} {@code ShortMatrix} (where {@code n = antiDiagonal.length}) with
     *         the supplied values on the anti-diagonal and {@code 0} elsewhere; the shared empty
     *         matrix if {@code antiDiagonal} is empty
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null}
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
     * ShortMatrix.diagonals((short[]) null, (short[]) null);            // throws IllegalArgumentException (both null)
     * ShortMatrix.diagonals(new short[] {1, 2}, new short[] {3, 4, 5});  // throws IllegalArgumentException (length mismatch)
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements; may be {@code null} or empty if {@code antiDiagonal} is non-{@code null}
     * @param antiDiagonal the array of anti-diagonal elements; may be {@code null} or empty if {@code mainDiagonal} is non-{@code null}
     * @return a square matrix with the specified diagonals, or an empty matrix when both arrays are empty (at least one being a non-{@code null} zero-length array)
     * @throws IllegalArgumentException if both {@code mainDiagonal} and {@code antiDiagonal} are {@code null}, or if both arrays are non-empty and have different lengths
     * @see #mainDiagonal(short[])
     * @see #antiDiagonal(short[])
     */
    public static ShortMatrix diagonals(final short[] mainDiagonal, final short[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(mainDiagonal != null || antiDiagonal != null, "Both 'mainDiagonal' and 'antiDiagonal' can't be null");

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
     * Converts a boxed {@link Matrix Matrix&lt;Short&gt;} to a primitive {@code ShortMatrix}.
     * {@code null} elements in the source are converted to {@code 0}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Short> boxed = Matrix.of(new Short[][] {{1, 2}, {3, 4}});
     * ShortMatrix primitive = ShortMatrix.unbox(boxed);
     * primitive.get(0, 1);                    // returns 2
     * primitive.get(1, 0);                    // returns 3
     *
     * Matrix<Short> withNull = Matrix.of(new Short[][] {{1, null}});
     * ShortMatrix.unbox(withNull).get(0, 1);     // returns 0 (null becomes 0)
     * ShortMatrix.unbox((Matrix<Short>) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the boxed {@code Short} matrix to convert; must not be {@code null}
     * @return a new {@code ShortMatrix} with primitive short values
     * @throws IllegalArgumentException if {@code x} is {@code null}
     * @see #boxed()
     */
    public static ShortMatrix unbox(final Matrix<Short> x) {
        N.checkArgNotNull(x, "x");

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
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
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
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
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
     * Returns the element directly above the specified position, or an empty {@link OptionalShort}
     * if the position is on the top edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalShort} is returned for the top
     * row instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.valueAbove(1, 0).get();          // returns (short) 1
     * matrix.valueAbove(1, 1).get();          // returns (short) 2
     *
     * matrix.valueAbove(0, 0).isPresent();    // returns false (top row, no cell above)
     * matrix.valueAbove(2, 0);                // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalShort} containing the element at position {@code (rowIndex - 1, columnIndex)},
     *         or empty if {@code rowIndex == 0}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalShort valueAbove(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? OptionalShort.empty() : OptionalShort.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, or an empty {@link OptionalShort}
     * if the position is on the bottom edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalShort} is returned for the
     * bottom row instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.valueBelow(0, 0).get();          // returns (short) 3
     * matrix.valueBelow(0, 1).get();          // returns (short) 4
     *
     * matrix.valueBelow(1, 0).isPresent();    // returns false (bottom row, no cell below)
     * matrix.valueBelow(2, 0);                // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalShort} containing the element at position {@code (rowIndex + 1, columnIndex)},
     *         or empty if {@code rowIndex == rowCount - 1}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalShort valueBelow(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? OptionalShort.empty() : OptionalShort.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, or an empty
     * {@link OptionalShort} if the position is on the leftmost edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalShort} is returned for the
     * leftmost column instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.valueLeft(0, 1).get();           // returns (short) 1
     * matrix.valueLeft(1, 1).get();           // returns (short) 3
     *
     * matrix.valueLeft(0, 0).isPresent();     // returns false (leftmost column, no cell to the left)
     * matrix.valueLeft(0, 2);                 // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalShort} containing the element at position {@code (rowIndex, columnIndex - 1)},
     *         or empty if {@code columnIndex == 0}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalShort valueLeft(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? OptionalShort.empty() : OptionalShort.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, or an empty
     * {@link OptionalShort} if the position is on the rightmost edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalShort} is returned for the
     * rightmost column instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.valueRight(0, 0).get();          // returns (short) 2
     * matrix.valueRight(1, 0).get();          // returns (short) 4
     *
     * matrix.valueRight(0, 1).isPresent();    // returns false (rightmost column, no cell to the right)
     * matrix.valueRight(0, 2);                // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalShort} containing the element at position {@code (rowIndex, columnIndex + 1)},
     *         or empty if {@code columnIndex == columnCount - 1}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalShort valueRight(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? OptionalShort.empty() : OptionalShort.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as a live reference to the underlying {@code short[]} storage.
     *
     * <p><b>Note:</b> This method returns the internal array, not a copy. Modifications to the
     * returned array will affect the matrix and vice versa. Use {@link #rowCopy(int)} if you need
     * an independent copy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.rowView(0);                      // returns [1, 2, 3]
     * matrix.rowView(1);                      // returns [4, 5, 6]
     *
     * short[] firstRow = matrix.rowView(0);
     * firstRow[0] = 10;
     * matrix.get(0, 0);                       // returns 10 (live view is shared)
     *
     * matrix.rowView(-1);                     // throws IndexOutOfBoundsException
     * matrix.rowView(2);                      // throws IndexOutOfBoundsException (rowIndex >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return the specified row as a direct reference to internal storage
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowCopy(int)
     */
    @Override
    public short[] rowView(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return a[rowIndex];
    }

    /**
     * Returns a defensive copy of the specified row as a new {@code short[]}.
     * Changes to the returned array do not affect this matrix and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.rowCopy(0);                      // returns [1, 2, 3]
     * matrix.rowCopy(1);                      // returns [4, 5, 6]
     *
     * short[] firstRow = matrix.rowCopy(0);
     * firstRow[0] = 10;
     * matrix.get(0, 0);                       // returns 1 (copy is independent)
     *
     * matrix.rowCopy(-1);                     // throws IndexOutOfBoundsException
     * matrix.rowCopy(2);                      // throws IndexOutOfBoundsException (rowIndex >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new short array of length {@code columnCount} containing the values of the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowView(int)
     * @see #columnCopy(int)
     */
    @Override
    public short[] rowCopy(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return N.copyOf(a[rowIndex], columnCount);
    }

    /**
     * Returns a defensive copy of the specified column as a new {@code short[]}.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously in memory. Modifications to the returned array
     * will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.columnCopy(0);                   // returns [1, 4]
     * matrix.columnCopy(2);                   // returns [3, 6]
     *
     * short[] firstColumn = matrix.columnCopy(0);
     * firstColumn[0] = 10;
     * matrix.get(0, 0);                       // returns 1 (copy is independent)
     *
     * matrix.columnCopy(-1);                  // throws IndexOutOfBoundsException
     * matrix.columnCopy(3);                   // throws IndexOutOfBoundsException (columnIndex >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new short array of length {@code rowCount} containing the values of the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     * @see #rowCopy(int)
     * @see #rowView(int)
     */
    @Override
    public short[] columnCopy(final int columnIndex) throws IndexOutOfBoundsException {
        checkColumnIndex(columnIndex);

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
     * matrix.rowCopy(0);                      // returns [7, 8, 9]
     *
     * matrix.setRow(1, new short[] {0, 0, 0});
     * matrix.get(1, 1);                       // returns 0
     *
     * matrix.setRow(0, new short[] {1, 2});     // throws IllegalArgumentException (length != columnCount)
     * matrix.setRow(5, new short[] {1, 2, 3});  // throws IndexOutOfBoundsException (rowIndex out of bounds)
     * }</pre>
     *
     * @param rowIndex the index of the row to set (0-based)
     * @param row the array of values to copy into the row; must be non-{@code null} and of length {@code columnCount}
     * @throws IndexOutOfBoundsException if {@code rowIndex} is out of bounds
     * @throws IllegalArgumentException if {@code row} is {@code null} or if {@code row.length != columnCount}
     */
    public void setRow(final int rowIndex, final short[] row) throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkArgNotNull(row, "row");
        checkRowIndex(rowIndex);
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
     * matrix.columnCopy(0);                   // returns [7, 8]
     *
     * matrix.setColumn(2, new short[] {0, 0});
     * matrix.get(1, 2);                       // returns 0
     *
     * matrix.setColumn(0, new short[] {1, 2, 3}); // throws IllegalArgumentException (length != rowCount)
     * matrix.setColumn(5, new short[] {1, 2});    // throws IndexOutOfBoundsException (columnIndex out of bounds)
     * }</pre>
     *
     * @param columnIndex the index of the column to set (0-based)
     * @param column the array of values to copy into the column; must be non-{@code null} and of length {@code rowCount}
     * @throws IndexOutOfBoundsException if {@code columnIndex} is out of bounds
     * @throws IllegalArgumentException if {@code column} is {@code null} or if {@code column.length != rowCount}
     */
    public void setColumn(final int columnIndex, final short[] column) throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkArgNotNull(column, "column");
        checkColumnIndex(columnIndex);
        N.checkArgument(column.length == rowCount, MSG_COLUMN_LENGTH_MISMATCH, rowCount, column.length);

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = column[i];
        }
    }

    /**
     * Updates all elements in a row in-place by applying the specified operator to each element.
     * This modifies the matrix directly.
     *
     * <p>The operator is applied to each element in the specified row sequentially
     * from left to right (column 0 to column columnCount-1).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.updateRow(0, x -> (short) (x * 2));
     * matrix.rowCopy(0);                      // returns [2, 4, 6]
     *
     * matrix.updateRow(1, x -> (short) 0);
     * matrix.rowCopy(1);                      // returns [0, 0, 0]
     *
     * matrix.updateRow(5, x -> x);           // throws IndexOutOfBoundsException (rowIndex out of bounds)
     * matrix.updateRow(0, null);             // throws IllegalArgumentException (operator is null)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param rowIndex the index of the row to update (0-based)
     * @param operator the operator to apply to each element in the row; receives the current
     *             element value and returns the new value
     * @throws IndexOutOfBoundsException if {@code rowIndex} is out of bounds
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.ShortUnaryOperator<E> operator)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        N.checkArgNotNull(operator, "operator");

        checkRowIndex(rowIndex);

        for (int i = 0; i < columnCount; i++) {
            a[rowIndex][i] = operator.applyAsShort(a[rowIndex][i]);
        }
    }

    /**
     * Updates all elements in the specified column in-place by applying the specified operator to each element.
     * This modifies the matrix directly.
     *
     * <p>The operator is applied to each element in the specified column sequentially
     * from top to bottom (row 0 to row rowCount-1).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.updateColumn(0, x -> (short) (x + 10));
     * matrix.columnCopy(0);                   // returns [11, 13, 15]
     *
     * matrix.updateColumn(1, x -> (short) -x);
     * matrix.columnCopy(1);                   // returns [-2, -4, -6]
     *
     * matrix.updateColumn(5, x -> x);        // throws IndexOutOfBoundsException (columnIndex out of bounds)
     * matrix.updateColumn(0, null);          // throws IllegalArgumentException (operator is null)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param columnIndex the index of the column to update (0-based)
     * @param operator the operator to apply to each element in the column; receives the current
     *             element value and returns the new value
     * @throws IndexOutOfBoundsException if {@code columnIndex} is out of bounds
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.ShortUnaryOperator<E> operator)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        N.checkArgNotNull(operator, "operator");

        checkColumnIndex(columnIndex);

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = operator.applyAsShort(a[i][columnIndex]);
        }
    }

    /**
     * Returns a copy of the main diagonal elements (upper-left to lower-right) as an array.
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>This method extracts the main diagonal elements at positions (0,0), (1,1), (2,2), etc.
     * The returned array is a copy; modifications to it will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.getMainDiagonal();              // returns [1, 5, 9]
     *
     * ShortMatrix small = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * small.getMainDiagonal();               // returns [1, 4]
     *
     * ShortMatrix.empty().getMainDiagonal();   // returns [] (0x0 is square)
     * ShortMatrix nonSquare = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.getMainDiagonal();           // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new short array containing a copy of the main diagonal elements
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
     * <p>This method sets the main diagonal elements at positions (0,0), (1,1), (2,2), etc.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.setMainDiagonal(new short[] {9, 8});
     * matrix.getMainDiagonal();              // returns [9, 8]
     * matrix.get(1, 1);                      // returns 8 (diagonal element updated)
     *
     * matrix.setMainDiagonal(new short[] {1}); // throws IllegalArgumentException (length != rowCount)
     * ShortMatrix nonSquare = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.setMainDiagonal(new short[] {1, 2}); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must be non-{@code null} and of length {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null} or its length is not equal to {@code rowCount}
     */
    @Override
    public void setMainDiagonal(final short[] mainDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgNotNull(mainDiagonal, "mainDiagonal");
        N.checkArgument(N.len(mainDiagonal) == rowCount, MSG_DIAGONAL_LENGTH_MISMATCH, rowCount, N.len(mainDiagonal));

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = mainDiagonal[i];
        }
    }

    /**
     * Updates the values on the main diagonal (upper-left to lower-right) by applying the specified operator.
     * The matrix must be square.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.updateMainDiagonal(x -> (short) (x * x));
     * matrix.getMainDiagonal();              // returns [1, 16]
     * matrix.get(0, 1);                      // returns 2 (off-diagonal unchanged)
     *
     * matrix.updateMainDiagonal(null);       // throws IllegalArgumentException (operator is null)
     * ShortMatrix nonSquare = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.updateMainDiagonal(x -> x);  // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each diagonal element; receives current element value and returns new value
     * @throws IllegalStateException if the matrix is not square
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
     * Returns a copy of the anti-diagonal elements (upper-right to lower-left) as an array.
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>This method extracts the anti-diagonal (secondary diagonal) elements from
     * upper-right to lower-left, at positions (0,n-1), (1,n-2), (2,n-3), etc.
     * The returned array is a copy; modifications to it will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.getAntiDiagonal();              // returns [3, 5, 7]
     *
     * ShortMatrix small = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * small.getAntiDiagonal();               // returns [2, 3]
     *
     * ShortMatrix.empty().getAntiDiagonal();   // returns [] (0x0 is square)
     * ShortMatrix nonSquare = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.getAntiDiagonal();           // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new short array containing a copy of the anti-diagonal elements
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
     * matrix.getAntiDiagonal();              // returns [9, 8]
     * matrix.get(0, 1);                      // returns 9 (anti-diagonal cell)
     *
     * matrix.setAntiDiagonal(new short[] {1}); // throws IllegalArgumentException (length != rowCount)
     * ShortMatrix nonSquare = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.setAntiDiagonal(new short[] {1, 2}); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must be non-{@code null} and of length {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null} or its length is not equal to {@code rowCount}
     */
    @Override
    public void setAntiDiagonal(final short[] antiDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgNotNull(antiDiagonal, "antiDiagonal");
        N.checkArgument(N.len(antiDiagonal) == rowCount, MSG_DIAGONAL_LENGTH_MISMATCH, rowCount, N.len(antiDiagonal));

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = antiDiagonal[i];
        }
    }

    /**
     * Updates the values on the anti-diagonal (upper-right to lower-left) by applying the specified operator.
     * The matrix must be square.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.updateAntiDiagonal(x -> (short) -x);
     * matrix.getAntiDiagonal();              // returns [-2, -3]
     * matrix.get(0, 0);                      // returns 1 (off anti-diagonal unchanged)
     *
     * matrix.updateAntiDiagonal(null);       // throws IllegalArgumentException (operator is null)
     * ShortMatrix nonSquare = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.updateAntiDiagonal(x -> x);  // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each anti-diagonal element; receives current element value and returns new value
     * @throws IllegalStateException if the matrix is not square
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
     * Updates all elements in the matrix in-place by applying the specified operator.
     * This modifies the matrix directly.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
     * Elements are processed in row-major order when executed sequentially.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.updateAll(x -> (short) (x * 2));
     * matrix.get(0, 0);                       // returns 2
     * matrix.get(1, 1);                       // returns 8
     *
     * matrix.updateAll(x -> (short) 0);
     * matrix.get(0, 1);                       // returns 0
     *
     * ShortMatrix.empty().updateAll(x -> x);                                      // no-op on empty matrix (no elements)
     * matrix.updateAll((Throwables.ShortUnaryOperator<RuntimeException>) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each element; receives the current element value
     *             and returns the new value
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.ShortUnaryOperator<E> operator) throws E {
        N.checkArgNotNull(operator, "operator");
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = operator.applyAsShort(a[i][j]);
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));
    }

    /**
     * Updates all elements in the matrix in-place based on their position (row and column indices).
     * This modifies the matrix directly.
     *
     * <p>The mapper receives the row and column indices for each element and returns the new value
     * for that position. This is useful for initializing matrices based on position patterns or
     * mathematical formulas. The operation may be performed in parallel for large matrices.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{0, 0, 0}, {0, 0, 0}});
     * matrix.updateAll((i, j) -> (short) (i + j));
     * matrix.get(0, 2);                       // returns 2
     * matrix.get(1, 2);                       // returns 3
     *
     * matrix.updateAll((i, j) -> (short) (i * 10 + j));
     * matrix.get(1, 1);                       // returns 11
     *
     * ShortMatrix.empty().updateAll((i, j) -> (short) i);                             // no-op on empty matrix
     * matrix.updateAll((Throwables.IntBiFunction<Short, RuntimeException>) null);     // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the function that receives row index and column index (0-based) and returns
     *             the new value for that position; the returned {@code Short} is unboxed, so it
     *             must not be {@code null}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws NullPointerException if {@code mapper} returns {@code null} for any position
     * @throws E if the mapper throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Short, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = mapper.apply(i, j);
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));
    }

    /**
     * Conditionally replaces elements in-place based on a predicate.
     * All elements that satisfy the predicate are replaced with the specified new value.
     * This modifies the matrix directly.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{-1, 2, -3}, {4, -5, 6}});
     * matrix.replaceIf(x -> x < 0, (short) 0);
     * matrix.get(0, 0);                       // returns 0 (-1 replaced)
     * matrix.get(1, 0);                       // returns 4 (unchanged)
     *
     * matrix.replaceIf(x -> x == 0, (short) 99);
     * matrix.get(0, 2);                       // returns 99 (was 0)
     *
     * ShortMatrix.empty().replaceIf(x -> true, (short) 1);                             // no-op on empty matrix
     * matrix.replaceIf((Throwables.ShortPredicate<RuntimeException>) null, (short) 0); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to test each element; elements for which this returns
     *                  {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.ShortPredicate<E> predicate, final short newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = predicate.test(a[i][j]) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));
    }

    /**
     * Conditionally replaces elements in-place based on their position (row and column indices).
     * Elements at positions that satisfy the predicate are replaced with the specified new value.
     * This modifies the matrix directly.
     *
     * <p>This is useful for position-based replacements such as setting diagonals, borders,
     * or specific regions. The operation may be performed in parallel for large matrices.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.replaceIf((i, j) -> i == j, (short) 0);
     * matrix.get(0, 0);                       // returns 0 (diagonal)
     * matrix.get(0, 1);                       // returns 2 (unchanged)
     *
     * matrix.replaceIf((i, j) -> i == 0 || j == 0, (short) -1);
     * matrix.get(0, 1);                       // returns -1 (first row)
     * matrix.get(2, 0);                       // returns -1 (first column)
     *
     * ShortMatrix.empty().replaceIf((i, j) -> true, (short) 1);                        // no-op on empty matrix
     * matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, (short) 0); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition that tests row index and column index (0-based); elements
     *                  at positions for which this returns {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final short newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = predicate.test(i, j) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));
    }

    /**
     * Creates a new ShortMatrix by applying a transformation function to each element.
     * The original matrix is not modified; a new matrix with transformed values is returned.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
     * This is the immutable counterpart to {@link #updateAll(Throwables.ShortUnaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix squared = matrix.map(x -> (short) (x * x));
     * squared.get(1, 1);                      // returns 16
     * matrix.get(1, 1);                       // returns 4 (original unchanged)
     *
     * ShortMatrix negated = matrix.map(x -> (short) -x);
     * negated.get(0, 0);                      // returns -1
     *
     * ShortMatrix.empty().map(x -> x).isEmpty();                          // returns true
     * matrix.map((Throwables.ShortUnaryOperator<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to apply to each element; receives the current element value
     *             and returns the transformed value
     * @return a new ShortMatrix with transformed values
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     * @see #updateAll(Throwables.ShortUnaryOperator)
     */
    public <E extends Exception> ShortMatrix map(final Throwables.ShortUnaryOperator<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final short[][] result = new short[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsShort(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return ShortMatrix.of(result);
    }

    /**
     * Creates a new Matrix by applying a function that converts short values to objects of type R.
     * This operation may be executed in parallel for better performance on large matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * Matrix<String> stringMatrix = matrix.mapToObj(x -> String.valueOf(x), String.class);
     * stringMatrix.get(0, 1);                 // returns "2"
     * stringMatrix.get(1, 0);                 // returns "3"
     *
     * Matrix<String> labeled = matrix.mapToObj(x -> "v" + x, String.class);
     * labeled.get(0, 0);                      // returns "v1"
     *
     * ShortMatrix.empty().mapToObj(x -> "" + x, String.class).isEmpty();                        // returns true
     * matrix.mapToObj((Throwables.ShortFunction<String, RuntimeException>) null, String.class); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <R> the element type of the resulting matrix
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to convert short values to type {@code R}
     * @param targetElementType the {@code Class} object for type {@code R} (used to allocate the
     *        {@code R[][]} backing array); must not be {@code null}
     * @return a new {@link Matrix Matrix&lt;R&gt;} containing the mapped values
     * @throws IllegalArgumentException if {@code mapper} or {@code targetElementType} is {@code null}
     * @throws E if the function throws an exception
     */
    public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.ShortFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E {
        N.checkArgNotNull(mapper, "mapper");
        N.checkArgNotNull(targetElementType, "targetElementType");
        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.apply(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

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
     * matrix.get(0, 0);                       // returns 5
     * matrix.get(1, 1);                       // returns 5
     *
     * matrix.fill((short) 0);
     * matrix.get(0, 1);                       // returns 0
     *
     * matrix.fill(Short.MIN_VALUE);
     * matrix.get(0, 0);                      // returns -32768 (boundary value)
     * ShortMatrix.empty().fill((short) 7);   // no-op on empty matrix
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
     * Fills this matrix with values from another two-dimensional array, starting at position {@code (0, 0)}.
     * Equivalent to {@code fill(0, 0, source)}.
     * The source array can be smaller than this matrix; only the overlapping region is copied.
     * If the source array is larger, only the portion that fits is copied. {@code null} rows in
     * {@code source} are skipped (the corresponding row of this matrix is left unchanged).
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{0, 0, 0}, {0, 0, 0}});
     * matrix.fill(new short[][] {{1, 2}, {3, 4}});
     * matrix.get(0, 0);                       // returns 1
     * matrix.get(0, 2);                       // returns 0 (source row is narrower, so this column is not overwritten)
     * // matrix is [[1, 2, 0], [3, 4, 0]]
     *
     * ShortMatrix big = ShortMatrix.of(new short[][] {{0, 0}, {0, 0}});
     * big.fill(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * big.get(1, 1);                          // returns 5 (only overlapping region copied)
     *
     * matrix.fill((short[][]) null);         // throws IllegalArgumentException (source is null)
     * }</pre>
     *
     * @param source the two-dimensional array to copy values from; must not be {@code null}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @see #fill(int, int, short[][])
     */
    public void fill(final short[][] source) {
        fill(0, 0, source);
    }

    /**
     * Fills a region of this matrix with values from another two-dimensional array, starting at the
     * specified destination position.
     * The source array can extend beyond this matrix's bounds; only the overlapping region is copied.
     * The matrix is modified in-place. {@code null} rows in {@code source} are skipped (the
     * corresponding destination row is left unchanged). Elements outside the matrix bounds are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}});
     * matrix.fill(1, 1, new short[][] {{1, 2}, {3, 4}});
     * matrix.get(1, 1);                       // returns 1
     * matrix.get(2, 2);                       // returns 4
     * matrix.get(0, 0);                       // returns 0 (outside filled region)
     * // matrix is [[0, 0, 0], [0, 1, 2], [0, 3, 4]]
     *
     * matrix.fill(0, 0, (short[][]) null);               // throws IllegalArgumentException (source is null)
     * matrix.fill(-1, 0, new short[][] {{1}});           // throws IndexOutOfBoundsException (destRowIndex < 0)
     * matrix.fill(0, 5, new short[][] {{1}});            // throws IndexOutOfBoundsException (destColumnIndex > columnCount)
     * }</pre>
     *
     * @param destRowIndex the target row index in this matrix (0-based, must satisfy {@code 0 <= destRowIndex <= rowCount})
     * @param destColumnIndex the target column index in this matrix (0-based, must satisfy {@code 0 <= destColumnIndex <= columnCount})
     * @param source the source array to copy values from; must not be {@code null}
     * @throws IndexOutOfBoundsException if {@code destRowIndex < 0} or {@code destRowIndex > rowCount},
     *         or if {@code destColumnIndex < 0} or {@code destColumnIndex > columnCount}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final short[][] source) throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkArgNotNull(source, "source");
        if (destRowIndex < 0 || destRowIndex > rowCount) {
            throw new IndexOutOfBoundsException(formatMsg("destRowIndex({}) must be between 0 and rowCount({})", destRowIndex, rowCount));
        }
        if (destColumnIndex < 0 || destColumnIndex > columnCount) {
            throw new IndexOutOfBoundsException(formatMsg("destColumnIndex({}) must be between 0 and columnCount({})", destColumnIndex, columnCount));
        }

        for (int i = 0, minLen = N.min(rowCount - destRowIndex, source.length); i < minLen; i++) {
            if (source[i] != null) {
                N.copy(source[i], 0, a[i + destRowIndex], destColumnIndex, N.min(source[i].length, columnCount - destColumnIndex));
            }
        }
    }

    /**
     * Returns a copy of this matrix.
     * The returned matrix is a completely independent copy; modifications to one
     * do not affect the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix original = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix copy = original.copy();
     * copy.get(0, 0);                         // returns 1
     * copy.equals(original);                  // returns true
     *
     * copy.set(0, 0, (short) 99);
     * original.get(0, 0);                     // returns 1 (original unchanged)
     * copy.get(0, 0);                         // returns 99 (copy modified)
     *
     * ShortMatrix.empty().copy().isEmpty();    // returns true
     * }</pre>
     *
     * @return a new matrix that is a copy of this matrix with full independence guarantee
     */
    @Override
    public ShortMatrix copy() {
        final short[][] c = new short[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[i].clone();
        }

        return new ShortMatrix(c);
    }

    /**
     * Creates a copy of a row range from this matrix.
     * The returned matrix contains only the specified rows and is completely independent from the original matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}, {5, 6}});
     * ShortMatrix subset = matrix.copy(1, 3);
     * subset.rowCount();                      // returns 2
     * subset.get(0, 0);                       // returns 3 -> {{3, 4}, {5, 6}}
     *
     * matrix.copy(1, 1).rowCount();          // returns 0 (empty range)
     *
     * matrix.copy(-1, 2);                     // throws IndexOutOfBoundsException (fromRowIndex < 0)
     * matrix.copy(0, 5);                      // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new {@code ShortMatrix} containing the specified rows
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public ShortMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        final short[][] c = new short[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = a[i].clone();
        }

        return new ShortMatrix(c);
    }

    /**
     * Creates a copy of a submatrix defined by row and column ranges.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * ShortMatrix submatrix = matrix.copy(0, 2, 1, 3);
     * submatrix.get(0, 0);                    // returns 2
     * submatrix.get(1, 1);                    // returns 6 -> {{2, 3}, {5, 6}}
     *
     * matrix.copy(0, 1, 0, 1).get(0, 0);     // returns 1 (single-cell submatrix)
     *
     * matrix.copy(0, 2, 1, 5);               // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.copy(-1, 2, 0, 2);              // throws IndexOutOfBoundsException (fromRowIndex < 0)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new {@code ShortMatrix} containing the specified submatrix
     * @throws IndexOutOfBoundsException if any range is invalid (e.g. {@code fromRowIndex < 0},
     *         {@code toRowIndex > rowCount}, {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code from > to} for either range)
     */
    @Override
    public ShortMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        final short[][] c = new short[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = N.copyOfRange(a[i], fromColumnIndex, toColumnIndex);
        }

        return new ShortMatrix(c);
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
     * grown.get(3, 3);                        // returns 0 (new cell)
     * grown.get(0, 0);                        // returns 1 (preserved)
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * ShortMatrix truncated = matrix.resize(2, 2);
     * truncated.columnCount();                // returns 2
     * truncated.get(1, 1);                    // returns 5
     *
     * // Mixed: grow rows, truncate columns
     * ShortMatrix mixed = matrix.resize(4, 2);
     * mixed.get(3, 0);                        // returns 0 (new row)
     * matrix.resize(-1, 2);                   // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new ShortMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
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
     * grown.get(3, 3);                        // returns 9 (new cell uses defaultValue)
     * grown.get(0, 0);                        // returns 1 (preserved)
     *
     * // Truncate: defaultValue is ignored when shrinking
     * ShortMatrix truncated = matrix.resize(2, 2, (short) 9);
     * truncated.get(1, 1);                    // returns 5 (no new cells, default unused)
     *
     * matrix.resize(0, 0, (short) 9).isEmpty();  // returns true
     * matrix.resize(2, -1, (short) 9);           // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the value used to fill cells that are added when a dimension grows;
     *        ignored when a dimension shrinks
     * @return a new ShortMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
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
     * bordered.rowCount();                    // returns 4
     * bordered.get(0, 0);                     // returns 0 (border cell)
     * bordered.get(1, 1);                     // returns 1 (original top-left)
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * ShortMatrix shifted = matrix.extend(0, 0, 2, 0);
     * shifted.get(0, 2);                      // returns 1 (original shifted right)
     *
     * matrix.extend(0, 0, 0, 0).equals(matrix); // returns true (no padding -> copy)
     * matrix.extend(-1, 0, 0, 0);               // throws IllegalArgumentException (negative padding)
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
     * bordered.get(0, 0);                     // returns 9 (border cell)
     * bordered.get(1, 1);                     // returns 1 (original top-left)
     * bordered.get(2, 2);                     // returns 4 (original bottom-right)
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * ShortMatrix shifted = matrix.extend(0, 0, 2, 0, (short) 7);
     * shifted.get(0, 0);                      // returns 7 (left padding)
     * shifted.get(0, 2);                      // returns 1 (original shifted right)
     *
     * matrix.extend(-1, 0, 0, 0, (short) 9); // throws IllegalArgumentException (negative padding)
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
            final short[][] b = new short[newRowCount][newColumnCount];

            for (int i = 0; i < newRowCount; i++) {
                if (i >= padTop && i < padTop + rowCount) {
                    N.copy(a[i - padTop], 0, b[i], padLeft, columnCount);
                }

                if (fillDefaultValue) {
                    if (i < padTop || i >= padTop + rowCount) {
                        N.fill(b[i], defaultValue);
                    } else if (columnCount < newColumnCount) {
                        if (padLeft > 0) {
                            N.fill(b[i], 0, padLeft, defaultValue);
                        }

                        if (padRight > 0) {
                            N.fill(b[i], columnCount + padLeft, newColumnCount, defaultValue);
                        }
                    }
                }
            }

            return new ShortMatrix(b);
        }
    }

    /**
     * Reverses the order of elements in each row in-place (horizontal flip).
     * This modifies the current matrix; each row is reversed left-to-right.
     *
     * <p>This is an in-place operation that modifies the current matrix.
     * For a non-destructive version that returns a new matrix, use {@link #flipHorizontally()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.flipHorizontallyInPlace();
     * matrix.rowCopy(0);                      // returns [3, 2, 1]
     * matrix.get(1, 0);                       // returns 6
     *
     * ShortMatrix single = ShortMatrix.of(new short[][] {{1}});
     * single.flipHorizontallyInPlace();
     * single.get(0, 0);                              // returns 1 (single column unchanged)
     * ShortMatrix.empty().flipHorizontallyInPlace(); // no-op on empty matrix
     * }</pre>
     *
     * @see #flipHorizontally()
     * @see #flipVerticallyInPlace()
     */
    @Override
    public void flipHorizontallyInPlace() {
        for (int i = 0; i < rowCount; i++) {
            N.reverse(a[i]);
        }
    }

    /**
     * Reverses the order of rows in-place (vertical flip).
     * This modifies the current matrix; the order of rows is reversed top-to-bottom
     * while the order of elements within each row remains unchanged.
     *
     * <p>This is an in-place operation that modifies the current matrix.
     * For a non-destructive version that returns a new matrix, use {@link #flipVertically()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.flipVerticallyInPlace();
     * matrix.rowCopy(0);                      // returns [5, 6]
     * matrix.rowCopy(2);                      // returns [1, 2]
     *
     * ShortMatrix single = ShortMatrix.of(new short[][] {{1, 2}});
     * single.flipVerticallyInPlace();
     * single.rowCopy(0);                           // returns [1, 2] (single row unchanged)
     * ShortMatrix.empty().flipVerticallyInPlace(); // no-op on empty matrix
     * }</pre>
     *
     * @see #flipVertically()
     * @see #flipHorizontallyInPlace()
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
     * Returns a new matrix that is a horizontal flip of this matrix (columns in reversed order).
     * Each row is reversed left-to-right (the leftmost element becomes rightmost).
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * ShortMatrix flipped = matrix.flipHorizontally();
     * flipped.rowCopy(0);                     // returns [3, 2, 1]
     * matrix.get(0, 0);                       // returns 1 (original unchanged)
     *
     * ShortMatrix single = ShortMatrix.of(new short[][] {{5}});
     * single.flipHorizontally().get(0, 0);              // returns 5 (single column unchanged)
     * ShortMatrix.empty().flipHorizontally().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new ShortMatrix with each row reversed
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
     * The topmost row becomes bottommost.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * ShortMatrix flipped = matrix.flipVertically();
     * flipped.rowCopy(0);                     // returns [4, 5, 6]
     * matrix.rowCopy(0);                      // returns [1, 2, 3] (original unchanged)
     *
     * ShortMatrix single = ShortMatrix.of(new short[][] {{1, 2}});
     * single.flipVertically().rowCopy(0);             // returns [1, 2] (single row unchanged)
     * ShortMatrix.empty().flipVertically().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new ShortMatrix with rows reversed
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
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * row of the result being the first column of the original read from bottom to top.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * ShortMatrix rotated = matrix.rotate90();
     * rotated.rowCopy(0);                     // returns [7, 4, 1]
     * rotated.get(2, 2);                      // returns 3
     *
     * ShortMatrix wide = ShortMatrix.of(new short[][] {{1, 2, 3}});  // 1x3
     * ShortMatrix tall = wide.rotate90();
     * tall.rowCount();                          // returns 3 (dimensions swapped to 3x1)
     * tall.get(0, 0);                           // returns 1
     * ShortMatrix.empty().rotate90().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix rotated 90 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         or an empty matrix if this matrix has zero columns
     * @see #rotate180()
     * @see #rotate270()
     * @see #transpose()
     */
    @Override
    public ShortMatrix rotate90() {
        if (columnCount == 0) {
            return EMPTY_SHORT_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final short[][] c = new short[columnCount][rowCount];

        if (rowCount <= columnCount) {
            for (int j = 0; j < rowCount; j++) {
                for (int i = 0; i < columnCount; i++) {
                    c[i][j] = a[rowCount - j - 1][i];
                }
            }
        } else {
            for (int i = 0; i < columnCount; i++) {
                for (int j = 0; j < rowCount; j++) {
                    c[i][j] = a[rowCount - j - 1][i];
                }
            }
        }

        return new ShortMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 180 degrees.
     * This is equivalent to flipping both horizontally and vertically, reversing the
     * order of all elements. The resulting matrix has the same dimensions as the original.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * ShortMatrix rotated = matrix.rotate180();
     * rotated.rowCopy(0);                     // returns [9, 8, 7]
     * rotated.get(2, 2);                      // returns 1
     *
     * ShortMatrix rect = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * rect.rotate180().rowCopy(0);               // returns [4, 3] (same dimensions)
     * ShortMatrix.empty().rotate180().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 180 degrees
     * @see #rotate90()
     * @see #rotate270()
     */
    @Override
    public ShortMatrix rotate180() {
        final short[][] c = new short[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[rowCount - i - 1].clone();
            N.reverse(c[i]);
        }

        return new ShortMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 270 degrees clockwise.
     * This is equivalent to rotating 90 degrees counter-clockwise.
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * row of the result being the last column of the original read from top to bottom.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * ShortMatrix rotated = matrix.rotate270();
     * rotated.rowCopy(0);                     // returns [3, 6, 9]
     * rotated.get(2, 2);                      // returns 7
     *
     * ShortMatrix wide = ShortMatrix.of(new short[][] {{1, 2, 3}});  // 1x3
     * ShortMatrix tall = wide.rotate270();
     * tall.rowCount();                           // returns 3 (dimensions swapped to 3x1)
     * tall.get(0, 0);                            // returns 3
     * ShortMatrix.empty().rotate270().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix rotated 270 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         or an empty matrix if this matrix has zero columns
     * @see #rotate90()
     * @see #rotate180()
     * @see #transpose()
     */
    @Override
    public ShortMatrix rotate270() {
        if (columnCount == 0) {
            return EMPTY_SHORT_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final short[][] c = new short[columnCount][rowCount];

        if (rowCount <= columnCount) {
            for (int j = 0; j < rowCount; j++) {
                for (int i = 0; i < columnCount; i++) {
                    c[i][j] = a[j][columnCount - i - 1];
                }
            }
        } else {
            for (int i = 0; i < columnCount; i++) {
                for (int j = 0; j < rowCount; j++) {
                    c[i][j] = a[j][columnCount - i - 1];
                }
            }
        }

        return new ShortMatrix(c);
    }

    /**
     * Returns a new matrix that is the transpose of this matrix.
     * The element at position {@code (i, j)} in this matrix appears at position {@code (j, i)}
     * in the result. The resulting matrix has dimensions swapped: {@code columnCount x rowCount}.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * ShortMatrix transposed = matrix.transpose();
     * transposed.rowCount();                  // returns 3 (2x3 becomes 3x2)
     * transposed.get(0, 1);                   // returns 4 (was at (1,0))
     * transposed.rowCopy(2);                  // returns [3, 6]
     *
     * matrix.transpose().transpose().equals(matrix); // returns true (involution)
     * ShortMatrix.empty().transpose().isEmpty();     // returns true
     * }</pre>
     *
     * @return a new {@code ShortMatrix} of shape {@code columnCount x rowCount} that is the transpose of this matrix;
     *         an {@code N x 0} matrix transposes to the empty {@code 0 x 0} matrix, because the swapped shape
     *         {@code 0 x N} (zero rows with a non-zero column count) is not representable
     */
    @Override
    public ShortMatrix transpose() {
        if (columnCount == 0) {
            return EMPTY_SHORT_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final short[][] c = new short[columnCount][rowCount];

        if (rowCount <= columnCount) {
            for (int j = 0; j < rowCount; j++) {
                for (int i = 0; i < columnCount; i++) {
                    c[i][j] = a[j][i];
                }
            }
        } else {
            for (int i = 0; i < columnCount; i++) {
                for (int j = 0; j < rowCount; j++) {
                    c[i][j] = a[j][i];
                }
            }
        }

        return new ShortMatrix(c);
    }

    /**
     * Reshapes this matrix to have the specified dimensions.
     * Elements are taken in row-major order from this matrix and placed into the new shape.
     * The new shape must have at least as many total cells as the original
     * ({@code (long) newRowCount * newColumnCount >= elementCount()}).
     * Any extra trailing cells in the new shape are filled with {@code 0}.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * ShortMatrix reshaped = matrix.reshape(3, 2);
     * reshaped.rowCopy(0);                    // returns [1, 2] -> [[1, 2], [3, 4], [5, 6]]
     * reshaped.get(2, 1);                     // returns 6
     *
     * ShortMatrix extended = matrix.reshape(2, 4);
     * extended.get(1, 2);                     // returns 0 (extra trailing cell) -> [[1,2,3,4],[5,6,0,0]]
     *
     * matrix.reshape(0, 0);                   // throws IllegalArgumentException (too small for 6 elements)
     * matrix.reshape(-1, 6);                  // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix; must be {@code >= 0}
     * @param newColumnCount the number of columns in the reshaped matrix; must be {@code >= 0}
     * @return a new {@code ShortMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative, if the resulting shape is not
     *         representable (zero rows with a non-zero column count), if the total cell count {@code (long) newRowCount * newColumnCount}
     *         exceeds {@code Integer.MAX_VALUE}, or if the new shape is too small to hold every existing element
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public ShortMatrix reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        checkMaterializableShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final short[][] c = new short[newRowCount][newColumnCount];

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return new ShortMatrix(c);
        }

        final int rowLen = (int) N.min(newRowCount, ceilDiv(elementCount, newColumnCount));

        if (a.length == 1) {
            for (int i = 0; i < rowLen; i++) {
                N.copy(a[0], i * newColumnCount, c[i], 0, (int) N.min(newColumnCount, elementCount - (long) i * newColumnCount));
            }
        } else {
            long cnt = 0;

            for (int i = 0; i < rowLen; i++) {
                for (int j = 0, col = (int) N.min(newColumnCount, elementCount - (long) i * newColumnCount); j < col; j++, cnt++) {
                    c[i][j] = a[(int) (cnt / columnCount)][(int) (cnt % columnCount)];
                }
            }
        }

        return new ShortMatrix(c);
    }

    /**
     * Repeats elements in both row and column directions.
     * Each element is repeated to form a block of size rowRepeats x columnRepeats.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}});
     * ShortMatrix repeated = matrix.repeatElements(2, 3);
     * repeated.rowCount();                    // returns 2
     * repeated.rowCopy(0);                    // returns [1, 1, 1, 2, 2, 2]
     *
     * matrix.repeatElements(1, 2).rowCopy(0); // returns [1, 1, 2, 2]
     *
     * matrix.repeatElements(0, 3);           // throws IllegalArgumentException (not positive)
     * matrix.repeatElements(2, -1);          // throws IllegalArgumentException (not positive)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat each element in row direction
     * @param columnRepeats number of times to repeat each element in column direction
     * @return a new ShortMatrix with repeated elements
     * @throws IllegalArgumentException if rowRepeats or columnRepeats is not positive,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repelem.html">MATLAB repelem function</a>
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

        final short[][] c = new short[rowCount * rowRepeats][columnCount * columnRepeats];

        for (int i = 0; i < rowCount; i++) {
            final short[] sourceRow = a[i];
            final short[] firstRepeatedRow = c[i * rowRepeats];

            for (int j = 0; j < columnCount; j++) {
                N.copy(Array.repeat(sourceRow[j], columnRepeats), 0, firstRepeatedRow, j * columnRepeats, columnRepeats);
            }

            for (int k = 1; k < rowRepeats; k++) {
                N.copy(firstRepeatedRow, 0, c[i * rowRepeats + k], 0, firstRepeatedRow.length);
            }
        }

        return new ShortMatrix(c);
    }

    /**
     * Repeats the entire matrix in a tiled pattern.
     * The matrix is repeated as a whole rowRepeats times vertically and columnRepeats times horizontally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix repeated = matrix.repeatMatrix(2, 3);
     * repeated.rowCount();                    // returns 4
     * repeated.rowCopy(0);                    // returns [1, 2, 1, 2, 1, 2]
     *
     * matrix.repeatMatrix(1, 2).rowCopy(0);  // returns [1, 2, 1, 2]
     *
     * matrix.repeatMatrix(0, 3);             // throws IllegalArgumentException (not positive)
     * matrix.repeatMatrix(2, -1);            // throws IllegalArgumentException (not positive)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat the matrix vertically
     * @param columnRepeats number of times to repeat the matrix horizontally
     * @return a new ShortMatrix with the tiled pattern
     * @throws IllegalArgumentException if rowRepeats or columnRepeats is not positive,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repmat.html">MATLAB repmat function</a>
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

        final short[][] c = new short[rowCount * rowRepeats][columnCount * columnRepeats];

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnRepeats; j++) {
                N.copy(a[i], 0, c[i], j * columnCount, columnCount);
            }
        }

        for (int i = 1; i < rowRepeats; i++) {
            for (int j = 0; j < rowCount; j++) {
                N.copy(c[j], 0, c[i * rowCount + j], 0, c[j].length);
            }
        }

        return new ShortMatrix(c);
    }

    /**
     * Returns a new {@link ShortList} containing all elements of this matrix in row-major order.
     * The returned list owns its data; modifications to it do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortList list = matrix.flatten();
     * list.size();                            // returns 4
     * list.get(0);                            // returns 1
     * list.get(3);                            // returns 4 (row-major order)
     *
     * ShortMatrix.empty().flatten().size();                 // returns 0
     * ShortMatrix.of(new short[][] {{7}}).flatten().get(0); // returns 7 (single element)
     * }</pre>
     *
     * @return a new {@link ShortList} of all elements in row-major order
     * @throws IllegalStateException if {@code (long) rowCount * columnCount > Integer.MAX_VALUE}
     * @see #horizontalStream()
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
     * Exposes the elements of this matrix to {@code action} as a single one-dimensional array
     * laid out in row-major order, then propagates any modifications back into the matrix.
     *
     * <p>This enables operations that need a global view of all matrix elements (e.g., sorting all
     * elements across the entire matrix). The shape of this matrix is preserved; only element
     * values change. See {@link Arrays#mutateAsFlat(short[][], Throwables.Consumer)} for the exact
     * semantics of the underlying operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{5, 3}, {4, 1}});
     * matrix.mutateAsFlat(arr -> java.util.Arrays.sort(arr));
     * matrix.rowCopy(0);                      // returns [1, 3]
     * matrix.rowCopy(1);                      // returns [4, 5] (sorted globally, placed back row-major)
     *
     * int[] captured = new int[1];
     * matrix.mutateAsFlat(arr -> captured[0] = arr.length);
     * captured[0];                            // returns 4 (flat view length)
     *
     * ShortMatrix.empty().mutateAsFlat(arr -> { });  // no-op on empty matrix
     * }</pre>
     *
     * @param <E> the type of exception that the operation may throw
     * @param action the operation to apply to the flattened array
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the operation throws an exception
     * @see Arrays#mutateAsFlat(short[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super short[], E> action) throws E {
        N.checkArgNotNull(action, "action");

        Arrays.mutateAsFlat(a, action);
    }

    /**
     * Stacks this matrix vertically with another matrix (vertical concatenation).
     * The matrices must have the same number of columns. The result has rows from this matrix
     * on top and rows from the other matrix below.
     *
     * <p>This operation is also known as vertical concatenation or rbind (bind by rows).
     * Creates a new matrix; the original matrices are not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix a = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});      // 2x3
     * ShortMatrix b = ShortMatrix.of(new short[][] {{7, 8, 9}, {10, 11, 12}});   // 2x3
     * ShortMatrix c = a.stackVertically(b);
     * c.rowCount();                           // returns 4
     * c.rowCopy(2);                           // returns [7, 8, 9]
     * c.get(3, 2);                            // returns 12
     *
     * ShortMatrix mismatch = ShortMatrix.of(new short[][] {{1, 2}});
     * a.stackVertically(mismatch);           // throws IllegalArgumentException (column count differs)
     * a.stackVertically((ShortMatrix) null); // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to stack below this matrix (must have the same column count)
     * @return a new ShortMatrix with dimensions (this.rowCount + other.rowCount) x this.columnCount
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.columnCount != other.columnCount},
     *         or if the merged row count would exceed {@code Integer.MAX_VALUE}
     * @see #stackHorizontally(ShortMatrix)
     */
    @Override
    public ShortMatrix stackVertically(final ShortMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.columnCount, MSG_VSTACK_COLUMN_MISMATCH, columnCount, other.columnCount);
        final long mergedRowCount = (long) rowCount + other.rowCount;
        N.checkArgument(mergedRowCount <= Integer.MAX_VALUE, "Merged row count overflow: {} + {} = {}", rowCount, other.rowCount, mergedRowCount);

        final short[][] c = new short[(int) mergedRowCount][];
        int j = 0;

        for (int i = 0; i < rowCount; i++) {
            c[j++] = a[i].clone();
        }

        for (int i = 0; i < other.rowCount; i++) {
            c[j++] = other.a[i].clone();
        }

        return ShortMatrix.of(c);
    }

    /**
     * Stacks this matrix horizontally with another matrix (horizontal concatenation).
     * The matrices must have the same number of rows. The result has columns from this matrix
     * on the left and columns from the other matrix on the right.
     *
     * <p>This operation is also known as horizontal concatenation or cbind (bind by columns).
     * Creates a new matrix; the original matrices are not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix a = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});      // 2x3
     * ShortMatrix b = ShortMatrix.of(new short[][] {{7, 8, 9}, {10, 11, 12}});   // 2x3
     * ShortMatrix c = a.stackHorizontally(b);
     * c.columnCount();                        // returns 6
     * c.rowCopy(0);                           // returns [1, 2, 3, 7, 8, 9]
     * c.get(1, 5);                            // returns 12
     *
     * ShortMatrix mismatch = ShortMatrix.of(new short[][] {{1, 2, 3}});
     * a.stackHorizontally(mismatch);           // throws IllegalArgumentException (row count differs)
     * a.stackHorizontally((ShortMatrix) null); // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to stack to the right of this matrix (must have the same row count)
     * @return a new ShortMatrix with dimensions this.rowCount x (this.columnCount + other.columnCount)
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.rowCount != other.rowCount},
     *         or if the merged column count would exceed {@code Integer.MAX_VALUE}
     * @see #stackVertically(ShortMatrix)
     */
    @Override
    public ShortMatrix stackHorizontally(final ShortMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(rowCount == other.rowCount, MSG_HSTACK_ROW_MISMATCH, rowCount, other.rowCount);
        final long mergedColumnCount = (long) columnCount + other.columnCount;
        N.checkArgument(mergedColumnCount <= Integer.MAX_VALUE, "Merged column count overflow: {} + {} = {}", columnCount, other.columnCount,
                mergedColumnCount);

        final short[][] c = new short[rowCount][(int) mergedColumnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c[i], 0, columnCount);
            N.copy(other.a[i], 0, c[i], columnCount, other.columnCount);
        }

        return ShortMatrix.of(c);
    }

    /**
     * Performs element-wise addition with another matrix.
     * The matrices must have the same dimensions. The original matrices are not modified.
     *
     * <p><b>Overflow:</b> each pair of elements is added as {@code int} (Java numeric promotion) and
     * the result is narrowed back to {@code short} via an explicit cast, so values overflowing the
     * short range {@code [-32768, 32767]} wrap modulo 65536. If you need a wider result, call
     * {@link #toIntMatrix()} (or {@link #toLongMatrix()}) first.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix a = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix b = ShortMatrix.of(new short[][] {{5, 6}, {7, 8}});
     * ShortMatrix sum = a.add(b);
     * sum.get(0, 0);                          // returns 6
     * sum.get(1, 1);                          // returns 12 -> [[6, 8], [10, 12]]
     *
     * ShortMatrix big = ShortMatrix.of(new short[][] {{32767}});
     * big.add(ShortMatrix.of(new short[][] {{1}})).get(0, 0); // returns -32768 (short overflow wraps)
     *
     * ShortMatrix wrongShape = ShortMatrix.of(new short[][] {{1, 2, 3}});
     * a.add(wrongShape);                     // throws IllegalArgumentException (different shapes)
     * a.add((ShortMatrix) null);             // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to add to this matrix; must not be {@code null} and must have the same shape
     * @return a new {@code ShortMatrix} containing the element-wise sum
     * @throws IllegalArgumentException if {@code other} is {@code null}, or if the matrices have different shapes
     * @see #subtract(ShortMatrix)
     * @see #zipWith(ShortMatrix, Throwables.ShortBinaryOperator)
     */
    public ShortMatrix add(final ShortMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot add matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final short[][] otherArray = other.a;
        final short[][] result = new short[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = (short) (a[i][j] + otherArray[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return ShortMatrix.of(result);
    }

    /**
     * Performs element-wise subtraction ({@code this - other}).
     * The matrices must have the same dimensions. The original matrices are not modified.
     *
     * <p><b>Overflow:</b> each pair of elements is subtracted as {@code int} (Java numeric promotion)
     * and the result is narrowed back to {@code short} via an explicit cast, so values outside the
     * short range {@code [-32768, 32767]} wrap modulo 65536. If you need a wider result, call
     * {@link #toIntMatrix()} (or {@link #toLongMatrix()}) first.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix a = ShortMatrix.of(new short[][] {{5, 6}, {7, 8}});
     * ShortMatrix b = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix diff = a.subtract(b);
     * diff.get(0, 0);                         // returns 4
     * diff.get(1, 1);                         // returns 4 -> [[4, 4], [4, 4]]
     *
     * ShortMatrix min = ShortMatrix.of(new short[][] {{-32768}});
     * min.subtract(ShortMatrix.of(new short[][] {{1}})).get(0, 0); // returns 32767 (short overflow wraps)
     *
     * ShortMatrix wrongShape = ShortMatrix.of(new short[][] {{1, 2, 3}});
     * a.subtract(wrongShape);                // throws IllegalArgumentException (different shapes)
     * a.subtract((ShortMatrix) null);        // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to subtract from this matrix; must not be {@code null} and must have the same shape
     * @return a new {@code ShortMatrix} containing the element-wise difference {@code this - other}
     * @throws IllegalArgumentException if {@code other} is {@code null}, or if the matrices have different shapes
     * @see #add(ShortMatrix)
     */
    public ShortMatrix subtract(final ShortMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot subtract matrices with different shapes: this is {}x{} but other is {}x{}", rowCount,
                columnCount, other.rowCount, other.columnCount);

        final short[][] otherArray = other.a;
        final short[][] result = new short[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = (short) (a[i][j] - otherArray[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return ShortMatrix.of(result);
    }

    /**
     * Performs matrix multiplication (Cayley product) with another matrix.
     * The number of columns in this matrix must equal the number of rows in {@code other}.
     * Result has shape {@code this.rowCount x other.columnCount}. The original matrices are not modified.
     *
     * <p><b>Note:</b> This is the linear-algebra matrix product, not element-wise multiplication.
     * For element-wise multiplication use {@link #zipWith(ShortMatrix, Throwables.ShortBinaryOperator)}.</p>
     *
     * <p><b>Overflow:</b> each partial product {@code a[i][k] * other[k][j]} is computed as an
     * {@code int} (via Java's numeric promotion), but it is then accumulated into the {@code short}
     * result cell with implicit narrowing, so intermediate sums wrap modulo 65536 and the final
     * result is always in the short range {@code [-32768, 32767]}. For inputs that may overflow,
     * widen via {@link #toIntMatrix()} (or {@link #toLongMatrix()}) and multiply there.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix a = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix b = ShortMatrix.of(new short[][] {{5, 6}, {7, 8}});
     * ShortMatrix product = a.matmul(b);
     * product.get(0, 0);                      // returns 19 (1*5 + 2*7)
     * product.get(1, 1);                      // returns 50 -> [[19, 22], [43, 50]]
     *
     * ShortMatrix m2x3 = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});      // 2x3
     * ShortMatrix m3x2 = ShortMatrix.of(new short[][] {{7, 8}, {9, 10}, {11, 12}}); // 3x2
     * m2x3.matmul(m3x2).rowCount();                                                 // returns 2 (result is 2x2)
     *
     * a.matmul(m3x2);                        // throws IllegalArgumentException (a.columnCount=2 != m3x2.rowCount=3)
     * a.matmul((ShortMatrix) null);          // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to multiply with; must not be {@code null}
     * @return a new {@code ShortMatrix} of shape {@code this.rowCount x other.columnCount} containing the matrix product
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.columnCount != other.rowCount}, or if this matrix has zero rows while {@code other} has a non-zero column count (the resulting shape is not representable)
     */
    public ShortMatrix matmul(final ShortMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.rowCount,
                "Matrix dimensions incompatible for multiplication: this is {}x{}, other is {}x{} (this.columnCount must equal other.rowCount)", rowCount,
                columnCount, other.rowCount, other.columnCount);

        checkRepresentableShape(rowCount, other.columnCount);

        final short[][] otherArray = other.a;
        final short[][] result = new short[rowCount][other.columnCount];
        final Throwables.IntTriConsumer<RuntimeException> multiplyAction = (i, j, k) -> result[i][j] += a[i][k] * otherArray[k][j];

        Matrices.forEachCartesianIndices(this, other, multiplyAction);

        return ShortMatrix.of(result);
    }

    /**
     * Converts this primitive short matrix to a boxed {@link Matrix Matrix&lt;Short&gt;}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix primitive = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * Matrix<Short> boxed = primitive.boxed();
     * boxed.get(0, 1);                        // returns Short 2
     * boxed.get(1, 0);                        // returns Short 3
     *
     * ShortMatrix.unbox(primitive.boxed()).equals(primitive); // returns true (round-trip)
     * ShortMatrix.empty().boxed().isEmpty();                  // returns true
     * }</pre>
     *
     * @return a new {@link Matrix Matrix&lt;Short&gt;} containing the same values as boxed {@code Short} instances
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
     * Performs element-wise operation on two matrices using a binary operator.
     * The matrices must have the same dimensions. Corresponding elements from both matrices
     * are combined using the provided function to produce the result matrix.
     *
     * <p>This is a generalized element-wise operation. For the common element-wise operations of addition and
     * subtraction, consider using the dedicated methods {@link #add(ShortMatrix)} and {@link #subtract(ShortMatrix)};
     * for the linear-algebra matrix product (which is not an element-wise operation), use {@link #matmul(ShortMatrix)}.</p>
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
     * Creates a new matrix; the original matrices are not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix a = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix b = ShortMatrix.of(new short[][] {{5, 6}, {7, 8}});
     *
     * ShortMatrix product = a.zipWith(b, (x, y) -> (short) (x * y));
     * product.get(0, 1);                      // returns 12 -> [[5, 12], [21, 32]]
     *
     * ShortMatrix max = a.zipWith(b, (x, y) -> (short) Math.max(x, y));
     * max.get(0, 0);                          // returns 5 -> [[5, 6], [7, 8]]
     *
     * ShortMatrix wrongShape = ShortMatrix.of(new short[][] {{1, 2, 3}});
     * a.zipWith(wrongShape, (x, y) -> x);                                    // throws IllegalArgumentException (different shapes)
     * a.zipWith(b, (Throwables.ShortBinaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null function)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param zipFunction the binary operator to apply to corresponding elements; receives the
     *                    element from this matrix as first argument and the element from
     *                    {@code other} as second argument
     * @return a new {@code ShortMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if {@code other} or {@code zipFunction} is {@code null},
     *         or if the matrices have different shapes
     * @throws E if the zip function throws an exception
     * @see #zipWith(ShortMatrix, ShortMatrix, Throwables.ShortTernaryOperator)
     */
    public <E extends Exception> ShortMatrix zipWith(final ShortMatrix other, final Throwables.ShortBinaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final short[][] arrayB = other.a;
        final short[][] result = new short[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsShort(a[i][j], arrayB[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return ShortMatrix.of(result);
    }

    /**
     * Performs element-wise operation on three matrices using a ternary operator.
     * All matrices must have the same dimensions. Corresponding elements from all three matrices
     * are combined using the provided function to produce the result matrix.
     *
     * <p>This is useful for operations that combine three matrices, such as weighted averages,
     * conditional selection, or mathematical formulas involving three variables.</p>
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
     * Creates a new matrix; the original matrices are not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix a = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix b = ShortMatrix.of(new short[][] {{5, 6}, {7, 8}});
     * ShortMatrix c = ShortMatrix.of(new short[][] {{9, 10}, {11, 12}});
     *
     * ShortMatrix sum = a.zipWith(b, c, (x, y, z) -> (short) (x + y + z));
     * sum.get(0, 0);                          // returns 15 -> [[15, 18], [21, 24]]
     *
     * ShortMatrix weighted = a.zipWith(b, c, (x, y, z) -> (short) (x * 2 + y * 3 + z * 5));
     * weighted.get(0, 0);                     // returns 62 -> [[62, 72], [82, 92]]
     *
     * ShortMatrix wrongShape = ShortMatrix.of(new short[][] {{1, 2, 3}});
     * a.zipWith(wrongShape, c, (x, y, z) -> x);                                  // throws IllegalArgumentException (different shapes)
     * a.zipWith(b, c, (Throwables.ShortTernaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null function)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param third the third matrix (must have the same dimensions as this matrix)
     * @param zipFunction the ternary operator to apply to corresponding elements; receives the
     *                    element from this matrix as first argument, the element from
     *                    {@code other} as second argument, and the element from {@code third}
     *                    as third argument
     * @return a new {@code ShortMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if any of {@code other}, {@code third}, or {@code zipFunction}
     *         is {@code null}, or if any of the matrices have different shapes
     * @throws E if the zip function throws an exception
     * @see #zipWith(ShortMatrix, Throwables.ShortBinaryOperator)
     */
    public <E extends Exception> ShortMatrix zipWith(final ShortMatrix other, final ShortMatrix third, final Throwables.ShortTernaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(third, "third");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other) && isSameShape(third), "Cannot zip matrices with different shapes: all matrices must be {}x{}", rowCount,
                columnCount);

        final short[][] arrayB = other.a;
        final short[][] arrayC = third.a;
        final short[][] result = new short[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsShort(a[i][j], arrayB[i][j], arrayC[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return ShortMatrix.of(result);
    }

    /**
     * Returns a stream of elements on the main diagonal (upper-left to lower-right).
     * The matrix must be square.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.mainDiagonalStream().toArray();  // returns [1, 5, 9]
     * matrix.mainDiagonalStream().sum();      // returns 15
     *
     * ShortMatrix.empty().mainDiagonalStream().count(); // returns 0 (empty stream)
     * ShortMatrix nonSquare = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.mainDiagonalStream();         // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a ShortStream of main-diagonal elements, or an empty stream if the matrix is empty
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public ShortStream mainDiagonalStream() {
        checkIsSquare();

        if (isEmpty()) {
            return ShortStream.empty();
        }

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
     * Returns a stream of elements on the anti-diagonal (upper-right to lower-left).
     * The matrix must be square.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.antiDiagonalStream().toArray();  // returns [3, 5, 7]
     * matrix.antiDiagonalStream().sum();      // returns 15
     *
     * ShortMatrix.empty().antiDiagonalStream().count(); // returns 0 (empty stream)
     * ShortMatrix nonSquare = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.antiDiagonalStream();         // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a ShortStream of anti-diagonal elements, or an empty stream if the matrix is empty
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public ShortStream antiDiagonalStream() {
        checkIsSquare();

        if (isEmpty()) {
            return ShortStream.empty();
        }

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
     * Elements are streamed row by row from the top-left corner to the bottom-right corner.
     *
     * <p>This method is useful for processing all matrix elements sequentially
     * without concern for their row/column positions. The stream supports all
     * standard ShortStream operations including sum, average, filter, map, etc.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.horizontalStream().toArray();    // returns [1, 2, 3, 4]
     * matrix.horizontalStream().sum();        // returns 10
     *
     * ShortMatrix.empty().horizontalStream().count();               // returns 0 (empty stream)
     * ShortMatrix.of(new short[][] {{7}}).horizontalStream().sum(); // returns 7 (single element)
     * }</pre>
     *
     * @return a ShortStream of all elements in row-major order, or an empty stream if the matrix is empty
     */
    @Override
    public ShortStream horizontalStream() {
        return horizontalStream(0, rowCount);
    }

    /**
     * Returns a stream of elements from a single row.
     * The elements are streamed from left to right within the specified row.
     *
     * <p>This method is particularly useful when you need to process or analyze
     * a specific row of the matrix independently. The returned stream can be
     * used with all standard ShortStream operations.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.horizontalStream(0).toArray();   // returns [1, 2, 3]
     * matrix.horizontalStream(1).sum();       // returns 15 (sum of second row)
     *
     * matrix.horizontalStream(-1);            // throws IndexOutOfBoundsException
     * matrix.horizontalStream(2);             // throws IndexOutOfBoundsException (rowIndex >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to stream (0-based)
     * @return a {@link ShortStream} of elements from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    @Override
    public ShortStream horizontalStream(final int rowIndex) {
        checkRowIndex(rowIndex);

        return horizontalStream(rowIndex, rowIndex + 1);
    }

    /**
     * Returns a stream of elements from a range of rows in row-major order.
     * Elements are streamed row by row from the starting row (inclusive) to
     * the ending row (exclusive), with each row streamed from left to right.
     *
     * <p>This method allows for efficient processing of a subset of matrix rows.
     * The stream maintains the row-major order, meaning all elements from one row
     * are streamed before moving to the next row.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.horizontalStream(1, 3).toArray(); // returns [3, 4, 5, 6]
     * matrix.horizontalStream(0, 2).toArray(); // returns [1, 2, 3, 4]
     *
     * matrix.horizontalStream(1, 1).count();  // returns 0 (empty range)
     * matrix.horizontalStream(0, 5);          // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a ShortStream of elements from the specified row range, or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex}
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
                final int len = toArrayLength(count());
                final short[] c = new short[len];

                for (int k = 0; k < len; k++) {
                    c[k] = a[i][j++];

                    if (j >= columnCount) {
                        i++;
                        j = 0;
                    }
                }

                return c;
            }
        });
    }

    /**
     * Returns a stream of all elements in this matrix, traversed vertically (top to bottom, left to right).
     * Elements are streamed column by column from the top-left corner to the bottom-right corner.
     *
     * <p>It provides an alternative way to iterate through matrix
     * elements compared to the row-major order of {@link #horizontalStream()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.verticalStream().toArray();      // returns [1, 3, 2, 4] (column-major)
     * matrix.verticalStream().sum();          // returns 10
     *
     * ShortMatrix.empty().verticalStream().count();               // returns 0 (empty stream)
     * ShortMatrix.of(new short[][] {{7}}).verticalStream().sum(); // returns 7 (single element)
     * }</pre>
     *
     * @return a ShortStream of all elements in column-major order, or an empty stream if the matrix is empty
     */
    @Override
    @Beta
    public ShortStream verticalStream() {
        return verticalStream(0, columnCount);
    }

    /**
     * Returns a stream of elements from a single column.
     * The elements are streamed from top to bottom within the specified column.
     *
     * <p>This method is useful for column-wise operations such as calculating
     * column sums, finding column maximums, or filtering column values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.verticalStream(1).toArray();     // returns [2, 5]
     * matrix.verticalStream(0).sum();         // returns 5 (sum of first column)
     *
     * matrix.verticalStream(-1);              // throws IndexOutOfBoundsException
     * matrix.verticalStream(3);               // throws IndexOutOfBoundsException (columnIndex >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to stream (0-based)
     * @return a {@link ShortStream} of elements from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    @Override
    public ShortStream verticalStream(final int columnIndex) {
        checkColumnIndex(columnIndex);

        return verticalStream(columnIndex, columnIndex + 1);
    }

    /**
     * Returns a stream of elements from a range of columns in column-major order.
     * Elements are streamed column by column from the starting column (inclusive)
     * to the ending column (exclusive), with each column streamed from top to bottom.
     *
     * <p>This method allows for efficient processing of a
     * subset of matrix columns in column-major order.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.verticalStream(1, 3).toArray();  // returns [2, 5, 3, 6]
     * matrix.verticalStream(0, 2).toArray();  // returns [1, 4, 2, 5]
     *
     * matrix.verticalStream(1, 1).count();    // returns 0 (empty range)
     * matrix.verticalStream(0, 5);            // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a ShortStream of elements from the specified column range in column-major order,
     *         or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
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
                final int len = toArrayLength(count());
                final short[] c = new short[len];

                for (int k = 0; k < len; k++) {
                    c[k] = a[i++][j];

                    if (i >= rowCount) {
                        i = 0;
                        j++;
                    }
                }

                return c;
            }
        });
    }

    /**
     * Returns a stream of ShortStream objects, where each ShortStream represents a complete row.
     * This creates a stream of streams, allowing for row-by-row processing of the matrix.
     *
     * <p>This method is useful for operations that need to process entire rows as units,
     * such as row-wise transformations, filtering rows based on conditions, or mapping
     * rows to other values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.rowStreams().count();            // returns 3 (one stream per row)
     * matrix.rowStreams()
     *     .mapToInt(row -> row.sum())
     *     .toArray();                         // returns [3, 7, 11]
     *
     * ShortMatrix.empty().rowStreams().count();                    // returns 0 (no rows)
     * ShortMatrix.of(new short[][] {{7, 8}}).rowStreams().count(); // returns 1 (single row)
     * }</pre>
     *
     * @return a Stream of ShortStream objects, one for each row in the matrix
     */
    @Override
    public Stream<ShortStream> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Returns a stream of ShortStream objects for a range of rows.
     * Each ShortStream in the result represents a complete row within the specified range.
     *
     * <p>This method allows for processing a subset of rows while maintaining the
     * ability to work with complete rows as individual streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.rowStreams(1, 3).count();        // returns 2 (rows 1 and 2)
     * matrix.rowStreams(0, 2)
     *     .mapToInt(row -> row.max().orElse((short) 0))
     *     .toArray();                         // returns [2, 4]
     *
     * matrix.rowStreams(1, 1).count();        // returns 0 (empty range)
     * matrix.rowStreams(0, 5);                // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of ShortStream objects for the specified row range
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
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
     * Returns a stream of ShortStream objects, where each ShortStream represents a complete column.
     * This creates a stream of streams, allowing for column-by-column processing of the matrix.
     *
     * <p>This method is useful for operations that need to process
     * entire columns as units, such as column-wise statistics, transformations, or filtering
     * columns based on conditions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.columnStreams().count();         // returns 3 (one stream per column)
     * matrix.columnStreams()
     *     .mapToInt(col -> col.sum())
     *     .toArray();                         // returns [5, 7, 9]
     *
     * ShortMatrix.empty().columnStreams().count();                      // returns 0 (no columns)
     * ShortMatrix.of(new short[][] {{7}, {8}}).columnStreams().count(); // returns 1 (single column)
     * }</pre>
     *
     * @return a Stream of ShortStream objects, one for each column in the matrix,
     *         or an empty stream if the matrix is empty
     */
    @Override
    @Beta
    public Stream<ShortStream> columnStreams() {
        return columnStreams(0, columnCount);
    }

    /**
     * Returns a stream of ShortStream objects for a range of columns.
     * Each ShortStream in the result represents a complete column within the specified range.
     *
     * <p>This method allows for processing a subset of columns
     * while maintaining the ability to work with complete columns as individual streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.columnStreams(1, 3).count();     // returns 2 (columns 1 and 2)
     * matrix.columnStreams(0, 2)
     *     .mapToInt(col -> col.sum())
     *     .toArray();                         // returns [5, 7]
     *
     * matrix.columnStreams(1, 1).count();     // returns 0 (empty range)
     * matrix.columnStreams(0, 5);             // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of ShortStream objects for the specified column range,
     *         or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
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
     * Returns the length of the given row array.
     * This is a hook called by {@link AbstractMatrix} during construction to determine the column
     * count of each row when validating the rectangular shape of the backing array.
     *
     * @param a the row array to measure; may be {@code null}
     * @return the length of {@code a}, or {@code 0} if {@code a} is {@code null}
     */
    @Override
    protected int length(@SuppressWarnings("hiding") final short[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Performs the specified action for each element in this matrix.
     * Elements are processed in row-major order (row by row, left to right) when executed sequentially.
     *
     * <p>The operation may be parallelized internally for large matrices to improve performance,
     * based on internal heuristics. If parallelized, the order of execution is not guaranteed,
     * but all elements will be processed exactly once.</p>
     *
     * <p><b>Note:</b> This method is for side-effect operations only (like printing, collecting,
     * or accumulating). For transformations that create new matrices, use {@link #map(Throwables.ShortUnaryOperator)}
     * or {@link #updateAll(Throwables.ShortUnaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     *
     * int[] sum = {0};
     * matrix.forEach(value -> sum[0] += value);
     * sum[0];                                 // 10 (sum of all elements)
     *
     * int[] count = {0};
     * matrix.forEach(value -> count[0]++);
     * count[0];                               // 4 (number of elements)
     *
     * int[] emptySum = {0};
     * ShortMatrix.empty().forEach(value -> emptySum[0] += value);
     * emptySum[0];                                                       // 0 (no elements visited)
     * matrix.forEach((Throwables.ShortConsumer<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element; receives each element value
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     * @see #forEach(int, int, int, int, Throwables.ShortConsumer)
     */
    public <E extends Exception> void forEach(final Throwables.ShortConsumer<E> action) throws E {
        forEach(0, rowCount, 0, columnCount, action);
    }

    /**
     * Performs the specified action for each element in the specified sub-matrix region.
     * Elements are processed in row-major order within the specified bounds when executed sequentially.
     *
     * <p>This method allows for processing a rectangular subset of the matrix.
     * The operation may be parallelized internally if the sub-matrix is large enough
     * to benefit from parallel processing; if parallelized, the order in which elements are
     * visited is unspecified and the action must be thread-safe, but every element is still
     * visited exactly once.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     *
     * int[] center = {0};
     * matrix.forEach(1, 2, 1, 2, value -> center[0] = value);
     * center[0];                              // 5 (center element only)
     *
     * int[] subSum = {0};
     * matrix.forEach(0, 2, 1, 3, value -> subSum[0] += value);
     * subSum[0];                              // 16 (2 + 3 + 5 + 6)
     *
     * matrix.forEach(0, 5, 0, 3, value -> { });                                      // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.forEach(0, 2, 0, 2, (Throwables.ShortConsumer<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @param action the action to be performed for each element in the sub-matrix
     * @throws IndexOutOfBoundsException if any index is out of bounds
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex,
            final Throwables.ShortConsumer<E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        if (Matrices.isParallelizable(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, elementAction, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                final short[] currentRow = a[i];

                for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                    action.accept(currentRow[j]);
                }
            }
        }
    }

    /**
     * Prints this matrix to standard output and returns the formatted string that was printed.
     * Each row is printed on a separate line with elements separated by commas and enclosed in
     * square brackets. An empty matrix prints {@code []}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.println();                       // returns "[1, 2, 3]\n[4, 5, 6]" (and prints it)
     *
     * ShortMatrix single = ShortMatrix.of(new short[][] {{7}});
     * single.println();                      // returns "[7]"
     * ShortMatrix.empty().println();         // returns "[]"
     * }</pre>
     *
     * @return the formatted string representation of the matrix that was printed
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

                    final short[] row = a[i];
                    sb.append('[');

                    for (int j = 0, rowLen = row.length; j < rowLen; j++) {
                        if (j > 0) {
                            sb.append(", ");
                        }

                        sb.append(row[j]);
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
     * ShortMatrix matrix1 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix matrix2 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix1.hashCode() == matrix2.hashCode(); // returns true (equal content)
     *
     * ShortMatrix different = ShortMatrix.of(new short[][] {{1, 2}, {3, 5}});
     * matrix1.hashCode() == different.hashCode(); // returns false (different content, typically)
     * ShortMatrix.empty().hashCode();             // returns a stable hash for the empty matrix
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
     * Returns {@code true} if the given object is also a {@code ShortMatrix} with the same dimensions
     * and all corresponding elements are equal. Returns {@code false} for any other type
     * (including primitive matrices of different element types).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix m1 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * ShortMatrix m2 = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * m1.equals(m2);                          // returns true (same shape and elements)
     *
     * ShortMatrix m3 = ShortMatrix.of(new short[][] {{1, 2}, {3, 5}});
     * m1.equals(m3);                          // returns false (different element)
     * m1.equals(null);                        // returns false
     * m1.equals("not a matrix");              // returns false (different type)
     * }</pre>
     *
     * @param obj the object to compare with; may be {@code null}
     * @return {@code true} if {@code obj} is a {@code ShortMatrix} with identical shape and elements,
     *         {@code false} otherwise
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
     * Returns a string representation of this matrix in a compact two-dimensional array format.
     * The output shows all matrix elements with rows enclosed in brackets and
     * elements separated by commas and spaces.
     *
     * <p>The format is suitable for debugging and logging. For pretty-printed output
     * with each row on a separate line, use {@link #println()} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortMatrix matrix = ShortMatrix.of(new short[][] {{1, 2}, {3, 4}});
     * matrix.toString();                      // returns "[[1, 2], [3, 4]]"
     *
     * ShortMatrix single = ShortMatrix.of(new short[][] {{7}});
     * single.toString();                     // returns "[[7]]"
     * ShortMatrix.empty().toString();        // returns "[]"
     * }</pre>
     *
     * @return a string representation of this matrix in two-dimensional array format
     * @see #println()
     */
    @Override
    public String toString() {
        return N.deepToString(a);
    }
}
