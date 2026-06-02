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
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.stream.ByteIteratorEx;
import com.landawn.abacus.util.stream.ByteStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * Matrix implementation backed by a rectangular {@code byte[][]}.
 *
 * <p>This type specializes {@link AbstractMatrix} for {@code byte} values while keeping the data in a
 * validated backing array. Constructors and {@code of(...)} generally wrap the supplied storage
 * directly, while factories, conversions, and mapping operations allocate new arrays.</p>
 *
 * <p>Cells introduced by growth or reshaping default to {@code 0} unless an overload accepts an
 * explicit fill value.</p>
 *
 * <p><b>Byte arithmetic:</b> the built-in element-wise arithmetic ({@link #add(ByteMatrix)},
 * {@link #subtract(ByteMatrix)}, and {@link #matmul(ByteMatrix)}) is performed using Java's standard
 * numeric promotion to {@code int} and the result is narrowed back to {@code byte} (via an explicit
 * cast for {@code add}/{@code subtract}, or via the implicit narrowing of the {@code +=} accumulation
 * in {@code matmul}), so values outside {@code [Byte.MIN_VALUE, Byte.MAX_VALUE]} wrap modulo 256.
 * The {@code zipWith}/{@code map} variants instead store whatever {@code byte} the supplied operator
 * returns, so any narrowing of an {@code int} computation must be performed inside the operator
 * itself. To preserve the full magnitude, widen first via {@link #toIntMatrix()} or
 * {@link #toLongMatrix()}.</p>
 *
 * @see IntMatrix
 * @see ShortMatrix
 * @see LongMatrix
 */
public final class ByteMatrix extends AbstractMatrix<byte[], ByteList, ByteStream, Stream<ByteStream>, ByteMatrix> {

    static final int BOUND = Byte.MAX_VALUE - Byte.MIN_VALUE + 1;
    private static final ByteMatrix EMPTY_BYTE_MATRIX = new ByteMatrix(new byte[0][0]);

    /**
     * Constructs a {@code ByteMatrix} backed by the supplied two-dimensional array.
     *
     * <p>If {@code a} is {@code null}, this creates an empty {@code 0x0} matrix. Otherwise the array
     * is used directly after rectangular-shape validation, so later modifications to either the input
     * array or the matrix remain visible through the other view. Call {@link #copy()} if you need an
     * independently owned matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[][] data = {{1, 2, 3}, {4, 5, 6}};
     * ByteMatrix matrix = new ByteMatrix(data);
     * matrix.get(0, 0);                                  // returns (byte) 1
     * data[0][0] = (byte) 99;                            // also mutates the matrix (no copy)
     * matrix.get(0, 0);                                  // returns (byte) 99
     *
     * ByteMatrix empty = new ByteMatrix((byte[][]) null);
     * empty.rowCount();                                  // returns 0 (null becomes a 0x0 matrix)
     *
     * new ByteMatrix(new byte[][] {{1, 2}, {3}});        // throws IllegalArgumentException (not rectangular)
     * }</pre>
     *
     * @param a the two-dimensional byte array to wrap, or {@code null} for an empty matrix
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public ByteMatrix(final byte[][] a) {
        super(a == null ? new byte[0][0] : a, byte.class);
    }

    /**
     * Creates an empty matrix with zero rows and zero columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.empty();
     * matrix.rowCount();          // returns 0
     * matrix.columnCount();       // returns 0
     * matrix.elementCount();      // returns 0L
     * matrix.isEmpty();           // returns true
     * }</pre>
     *
     * @return an empty byte matrix
     */
    public static ByteMatrix empty() {
        return EMPTY_BYTE_MATRIX;
    }

    /**
     * Creates a {@code ByteMatrix} from a two-dimensional byte array.
     *
     * <p><b>Important:</b> When {@code a} is non-empty, the provided array is used directly
     * without defensive copying. Changes to the input array are reflected in the returned matrix,
     * and vice versa. Call {@link #copy()} if you need an independently owned matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.rowCount();                                 // returns 2
     * matrix.get(1, 2);                                  // returns (byte) 6
     *
     * ByteMatrix empty = ByteMatrix.of((byte[][]) null);
     * empty.isEmpty();                                   // returns true
     *
     * ByteMatrix none = ByteMatrix.of(new byte[0][0]);
     * none.rowCount();                                   // returns 0
     *
     * ByteMatrix.of(new byte[][] {{1, 2}, {3}});         // throws IllegalArgumentException (not rectangular)
     * }</pre>
     *
     * @param a the two-dimensional byte array to wrap; may be {@code null} or empty
     * @return a new {@code ByteMatrix} wrapping the provided data, or an empty {@code ByteMatrix}
     *         if {@code a} is {@code null} or empty
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public static ByteMatrix of(final byte[]... a) {
        return N.isEmpty(a) ? EMPTY_BYTE_MATRIX : new ByteMatrix(a);
    }

    /**
     * Creates a new {@code 1 x length} matrix filled with random byte values uniformly distributed
     * across the full byte range {@code [Byte.MIN_VALUE, Byte.MAX_VALUE]}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.random(5);
     * matrix.rowCount();             // returns 1
     * matrix.columnCount();          // returns 5 (values are random in [-128, 127])
     *
     * ByteMatrix none = ByteMatrix.random(0);
     * none.columnCount();            // returns 0 (1x0 matrix)
     *
     * ByteMatrix.random(-1);         // throws IllegalArgumentException (negative length)
     * }</pre>
     *
     * @param length the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code ByteMatrix} of dimensions {@code 1 x length} filled with random values
     * @throws IllegalArgumentException if {@code length} is negative
     * @see #random(int, int)
     */
    public static ByteMatrix random(final int length) {
        return random(1, length);
    }

    /**
     * Creates a new matrix of the specified dimensions filled with random byte values uniformly
     * distributed across the full byte range {@code [Byte.MIN_VALUE, Byte.MAX_VALUE]}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.random(2, 3);
     * matrix.rowCount();             // returns 2
     * matrix.columnCount();          // returns 3 (values are random in [-128, 127])
     *
     * ByteMatrix none = ByteMatrix.random(0, 0);
     * none.isEmpty();                // returns true
     *
     * ByteMatrix.random(-1, 3);      // throws IllegalArgumentException (negative rowCount)
     * ByteMatrix.random(0, 3);       // throws IllegalArgumentException (0 rows but 3 columns is unrepresentable)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code ByteMatrix} of dimensions {@code rowCount x columnCount} filled with random values
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if {@code rowCount} is {@code 0} while {@code columnCount} is positive (an unrepresentable shape)
     */
    public static ByteMatrix random(final int rowCount, final int columnCount) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final byte[][] a = new byte[rowCount][columnCount];

        for (byte[] ea : a) {
            for (int i = 0; i < columnCount; i++) {
                ea[i] = (byte) (RAND.nextInt(BOUND) + Byte.MIN_VALUE);
            }
        }

        return new ByteMatrix(a);
    }

    /**
     * Creates a new matrix of the specified dimensions where every element is the provided {@code element}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.repeat(2, 3, (byte) 1);
     * matrix.get(1, 2);                  // returns (byte) 1
     * matrix.elementCount();             // returns 6L ([[1, 1, 1], [1, 1, 1]])
     *
     * ByteMatrix none = ByteMatrix.repeat(0, 0, (byte) 9);
     * none.isEmpty();                    // returns true
     *
     * ByteMatrix.repeat(-1, 3, (byte) 1);   // throws IllegalArgumentException (negative rowCount)
     * ByteMatrix.repeat(0, 3, (byte) 1);    // throws IllegalArgumentException (0 rows but 3 columns)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @param element the byte value to fill the matrix with
     * @return a new {@code ByteMatrix} of dimensions {@code rowCount x columnCount} with every cell set to {@code element}
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if {@code rowCount} is {@code 0} while {@code columnCount} is positive (an unrepresentable shape)
     */
    public static ByteMatrix repeat(final int rowCount, final int columnCount, final byte element) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final byte[][] a = new byte[rowCount][columnCount];

        for (byte[] ea : a) {
            N.fill(ea, element);
        }

        return new ByteMatrix(a);
    }

    /**
     * Creates a 1-row ByteMatrix containing a range of byte values from startInclusive to endExclusive.
     * The range increments by 1 for each element. If {@code startInclusive >= endExclusive}, a 1×0
     * matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix range = ByteMatrix.range((byte) 1, (byte) 5);
     * range.rowView(0);                                  // returns [1, 2, 3, 4]
     * range.columnCount();                               // returns 4
     *
     * ByteMatrix single = ByteMatrix.range((byte) 3, (byte) 4);
     * single.rowView(0);                                  // returns [3]
     *
     * ByteMatrix empty = ByteMatrix.range((byte) 5, (byte) 5);
     * empty.columnCount();                               // returns 0 (start == end -> 1x0)
     *
     * ByteMatrix reversed = ByteMatrix.range((byte) 5, (byte) 1);
     * reversed.columnCount();                            // returns 0 (start > end -> 1x0)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new 1×n ByteMatrix where n = max(0, endExclusive - startInclusive)
     */
    public static ByteMatrix range(final byte startInclusive, final byte endExclusive) {
        return new ByteMatrix(new byte[][] { Array.range(startInclusive, endExclusive) });
    }

    /**
     * Creates a 1-row ByteMatrix containing a range of byte values with a specified step.
     * The step size can be positive (for ascending sequences) or negative (for descending sequences).
     * If the step would not reach endExclusive from startInclusive, a 1×0 matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix range = ByteMatrix.range((byte) 0, (byte) 10, (byte) 2);
     * range.rowView(0);                                          // returns [0, 2, 4, 6, 8]
     *
     * ByteMatrix desc = ByteMatrix.range((byte) 10, (byte) 0, (byte) -2);
     * desc.rowView(0);                                           // returns [10, 8, 6, 4, 2]
     *
     * ByteMatrix wrongDir = ByteMatrix.range((byte) 0, (byte) 10, (byte) -1);
     * wrongDir.columnCount();                                   // returns 0 (step points away from end -> 1x0)
     *
     * ByteMatrix.range((byte) 0, (byte) 10, (byte) 0);          // throws IllegalArgumentException (step is zero)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param step the step size (must not be zero; can be positive or negative)
     * @return a new 1×n ByteMatrix with values incremented by the step size
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static ByteMatrix range(final byte startInclusive, final byte endExclusive, final byte step) {
        return new ByteMatrix(new byte[][] { Array.range(startInclusive, endExclusive, step) });
    }

    /**
     * Creates a 1-row ByteMatrix containing a closed range of byte values from startInclusive to endInclusive.
     * The range increments by 1 for each element and includes the end value. If
     * {@code startInclusive > endInclusive}, a 1×0 matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix range = ByteMatrix.rangeClosed((byte) 1, (byte) 4);
     * range.rowView(0);                                   // returns [1, 2, 3, 4]
     *
     * ByteMatrix single = ByteMatrix.rangeClosed((byte) 7, (byte) 7);
     * single.rowView(0);                                  // returns [7] (end is inclusive)
     *
     * ByteMatrix empty = ByteMatrix.rangeClosed((byte) 5, (byte) 1);
     * empty.columnCount();                               // returns 0 (start > end -> 1x0)
     *
     * ByteMatrix.rangeClosed((byte) 0, (byte) 3).columnCount();   // returns 4
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new 1×n ByteMatrix where n = max(0, endInclusive - startInclusive + 1)
     */
    public static ByteMatrix rangeClosed(final byte startInclusive, final byte endInclusive) {
        return new ByteMatrix(new byte[][] { Array.rangeClosed(startInclusive, endInclusive) });
    }

    /**
     * Creates a 1-row ByteMatrix containing a closed range of byte values with a specified step.
     * The step size can be positive (for ascending sequences) or negative (for descending sequences).
     * The end value is included only if it is reachable by stepping from start. If the step would not
     * reach endInclusive from startInclusive, a 1×0 matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix range = ByteMatrix.rangeClosed((byte) 0, (byte) 8, (byte) 2);
     * range.rowView(0);                                          // returns [0, 2, 4, 6, 8]
     *
     * ByteMatrix partial = ByteMatrix.rangeClosed((byte) 0, (byte) 9, (byte) 2);
     * partial.rowView(0);                                        // returns [0, 2, 4, 6, 8] (9 not reachable)
     *
     * ByteMatrix desc = ByteMatrix.rangeClosed((byte) 10, (byte) 0, (byte) -2);
     * desc.rowView(0);                                           // returns [10, 8, 6, 4, 2, 0]
     *
     * ByteMatrix.rangeClosed((byte) 0, (byte) 8, (byte) 0);     // throws IllegalArgumentException (step is zero)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive, if reachable by stepping)
     * @param step the step size (must not be zero; can be positive or negative)
     * @return a new 1×n ByteMatrix with values incremented by the step size
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static ByteMatrix rangeClosed(final byte startInclusive, final byte endInclusive, final byte step) {
        return new ByteMatrix(new byte[][] { Array.rangeClosed(startInclusive, endInclusive, step) });
    }

    /**
     * Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
     * All other elements are set to zero. The resulting matrix has dimensions n×n where n is the length
     * of the diagonal array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.mainDiagonal(new byte[] {1, 2, 3});
     * matrix.rowCount();                                 // returns 3
     * matrix.get(1, 1);                                  // returns (byte) 2
     * matrix.get(0, 2);                                  // returns (byte) 0 (off-diagonal)
     *
     * ByteMatrix empty = ByteMatrix.mainDiagonal((byte[]) null);
     * empty.isEmpty();                                   // returns true
     *
     * ByteMatrix none = ByteMatrix.mainDiagonal(new byte[0]);
     * none.rowCount();                                   // returns 0
     * }</pre>
     *
     * @param mainDiagonal the array of diagonal elements; may be {@code null} or empty
     * @return a square {@code n×n} matrix with the specified main diagonal, where {@code n} is the
     *         array length, or an empty matrix if {@code mainDiagonal} is {@code null} or empty
     * @see #antiDiagonal(byte[])
     * @see #diagonals(byte[], byte[])
     */
    public static ByteMatrix mainDiagonal(final byte[] mainDiagonal) {
        return diagonals(mainDiagonal, null);
    }

    /**
     * Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
     * All other elements (off-diagonal) are set to zero. The matrix size is n×n where n is the length
     * of the diagonal array. The anti-diagonal runs from top-right to bottom-left.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.antiDiagonal(new byte[] {1, 2, 3});
     * // Resulting matrix:
     * //   {0, 0, 1},
     * //   {0, 2, 0},
     * //   {3, 0, 0}
     * matrix.get(0, 2);                                  // returns (byte) 1
     * matrix.get(2, 0);                                  // returns (byte) 3
     * matrix.get(0, 0);                                  // returns (byte) 0 (off anti-diagonal)
     *
     * ByteMatrix empty = ByteMatrix.antiDiagonal((byte[]) null);
     * empty.isEmpty();                                   // returns true
     *
     * ByteMatrix none = ByteMatrix.antiDiagonal(new byte[0]);
     * none.rowCount();                                   // returns 0
     * }</pre>
     *
     * @param antiDiagonal the array of anti-diagonal elements; may be {@code null} or empty
     * @return a square {@code n×n} matrix with the specified anti-diagonal, where {@code n} is the
     *         array length, or an empty matrix if {@code antiDiagonal} is {@code null} or empty
     * @see #mainDiagonal(byte[])
     * @see #diagonals(byte[], byte[])
     */
    public static ByteMatrix antiDiagonal(final byte[] antiDiagonal) {
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
     * ByteMatrix matrix = ByteMatrix.diagonals(new byte[] {1, 2, 3}, new byte[] {4, 5, 6});
     * // Resulting matrix:
     * //   {1, 0, 4},
     * //   {0, 2, 0},
     * //   {6, 0, 3}
     * matrix.get(0, 0);                                  // returns (byte) 1 (main diagonal)
     * matrix.get(0, 2);                                  // returns (byte) 4 (anti-diagonal)
     * matrix.get(1, 1);                                  // returns (byte) 2 (overlap: main wins)
     *
     * ByteMatrix empty = ByteMatrix.diagonals((byte[]) null, (byte[]) null);
     * empty.isEmpty();                                   // returns true
     *
     * ByteMatrix.diagonals(new byte[] {1, 2}, new byte[] {3, 4, 5});   // throws IllegalArgumentException (different lengths)
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements; may be {@code null} or empty
     * @param antiDiagonal the array of anti-diagonal elements; may be {@code null} or empty
     * @return a square matrix with the specified diagonals, or an empty matrix if both inputs are
     *         {@code null} or empty
     * @throws IllegalArgumentException if both arrays are non-empty and have different lengths
     */
    public static ByteMatrix diagonals(final byte[] mainDiagonal, final byte[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(N.isEmpty(mainDiagonal) || N.isEmpty(antiDiagonal) || mainDiagonal.length == antiDiagonal.length,
                "The length of 'mainDiagonal' and 'antiDiagonal' must be same");

        if (N.isEmpty(mainDiagonal) && N.isEmpty(antiDiagonal)) {
            return EMPTY_BYTE_MATRIX;
        }

        final int len = N.max(N.len(mainDiagonal), N.len(antiDiagonal));
        final byte[][] result = new byte[len][len];

        if (N.notEmpty(antiDiagonal)) {
            for (int i = 0, j = len - 1; i < len; i++, j--) {
                result[i][j] = antiDiagonal[i];
            }
        }

        if (N.notEmpty(mainDiagonal)) {
            for (int i = 0; i < len; i++) {
                result[i][i] = mainDiagonal[i]; // NOSONAR
            }
        }

        return new ByteMatrix(result);
    }

    /**
     * Converts a boxed {@code Matrix<Byte>} to a primitive {@code ByteMatrix}.
     * {@code null} entries in the input matrix are converted to {@code 0}.
     *
     * <p>This method performs the opposite operation of {@link #boxed()}, converting
     * from object-based {@code Byte} values to primitive {@code byte} values. This conversion
     * improves memory efficiency and performance when working with large matrices.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Byte> boxed = Matrix.of(new Byte[][] {{1, 2}, {null, 4}});
     * ByteMatrix primitive = ByteMatrix.unbox(boxed);
     * primitive.get(0, 1);                               // returns (byte) 2
     * primitive.get(1, 0);                               // returns (byte) 0 (null becomes 0)
     *
     * Matrix<Byte> emptyBoxed = Matrix.of(new Byte[0][0]);
     * ByteMatrix.unbox(emptyBoxed).isEmpty();            // returns true
     *
     * ByteMatrix.unbox((Matrix<Byte>) null);             // throws NullPointerException
     * }</pre>
     *
     * @param x the boxed {@code Matrix<Byte>} to convert; must not be {@code null}
     * @return a new {@code ByteMatrix} with primitive byte values
     * @throws NullPointerException if {@code x} is {@code null}
     * @see #boxed()
     */
    public static ByteMatrix unbox(final Matrix<Byte> x) {
        return ByteMatrix.of(Array.unbox(x.a));
    }

    /**
     * Returns the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * matrix.get(0, 1);                  // returns (byte) 2
     * matrix.get(1, 0);                  // returns (byte) 3
     * matrix.get(2, 0);                  // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * matrix.get(0, 5);                  // throws ArrayIndexOutOfBoundsException (column out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return the byte element at position {@code (rowIndex, columnIndex)}
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     * @see #get(Point)
     */
    public byte get(final int rowIndex, final int columnIndex) {
        return a[rowIndex][columnIndex];
    }

    /**
     * Returns the element at the specified point.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * matrix.get(Point.of(0, 1));        // returns (byte) 2
     * matrix.get(Point.of(1, 1));        // returns (byte) 4
     * matrix.get(Point.of(5, 0));        // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * matrix.get((Point) null);          // throws IllegalArgumentException (point is null)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @return the byte element at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #get(int, int)
     */
    public byte get(final Point point) {
        N.checkArgNotNull(point, "point");

        return a[point.rowIndex()][point.columnIndex()];
    }

    /**
     * Sets the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * matrix.set(0, 1, (byte) 9);
     * matrix.get(0, 1);                  // returns (byte) 9
     * matrix.set(1, 0, (byte) -5);
     * matrix.get(1, 0);                  // returns (byte) -5
     * matrix.set(2, 0, (byte) 1);        // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * matrix.set(0, 9, (byte) 1);        // throws ArrayIndexOutOfBoundsException (column out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @param value the new byte value to store at the specified position
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     * @see #set(Point, byte)
     */
    public void set(final int rowIndex, final int columnIndex, final byte value) {
        a[rowIndex][columnIndex] = value;
    }

    /**
     * Sets the element at the specified point to the given value.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * matrix.set(Point.of(0, 1), (byte) 9);
     * matrix.get(Point.of(0, 1));        // returns (byte) 9
     * matrix.set(Point.of(1, 0), (byte) 7);
     * matrix.get(1, 0);                       // returns (byte) 7
     * matrix.set(Point.of(5, 0), (byte) 1);   // throws ArrayIndexOutOfBoundsException (out of bounds)
     * matrix.set((Point) null, (byte) 1);     // throws IllegalArgumentException (point is null)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @param value the new byte value to set at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #set(int, int, byte)
     */
    public void set(final Point point, final byte value) {
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
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * matrix.valueAbove(1, 0).get();        // returns (byte) 1
     * matrix.valueAbove(0, 0).isPresent();  // returns false (no row above row 0)
     * matrix.valueAbove(1, 1).get();        // returns (byte) 2
     * matrix.valueAbove(5, 0);              // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalByte containing the element at position (rowIndex - 1, columnIndex), or empty if rowIndex == 0
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalByte valueAbove(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? OptionalByte.empty() : OptionalByte.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, if it exists.
     * This method provides safe access to the element directly below the given position
     * without throwing an exception when at the bottom edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * matrix.valueBelow(0, 0).get();        // returns (byte) 3
     * matrix.valueBelow(1, 0).isPresent();  // returns false (no row below last row)
     * matrix.valueBelow(0, 1).get();        // returns (byte) 4
     * matrix.valueBelow(0, 9);              // throws ArrayIndexOutOfBoundsException (column out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalByte containing the element at position (rowIndex + 1, columnIndex), or empty if rowIndex == rowCount - 1
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalByte valueBelow(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? OptionalByte.empty() : OptionalByte.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, if it exists.
     * This method provides safe access to the element directly to the left of the given position
     * without throwing an exception when at the leftmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * matrix.valueLeft(0, 1).get();         // returns (byte) 1
     * matrix.valueLeft(0, 0).isPresent();   // returns false (no column left of column 0)
     * matrix.valueLeft(1, 1).get();         // returns (byte) 3
     * matrix.valueLeft(9, 1);               // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalByte containing the element at position (rowIndex, columnIndex - 1), or empty if columnIndex == 0
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalByte valueLeft(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? OptionalByte.empty() : OptionalByte.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, if it exists.
     * This method provides safe access to the element directly to the right of the given position
     * without throwing an exception when at the rightmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * matrix.valueRight(0, 0).get();        // returns (byte) 2
     * matrix.valueRight(0, 1).isPresent();  // returns false (no column right of last column)
     * matrix.valueRight(1, 0).get();        // returns (byte) 4
     * matrix.valueRight(0, 9);              // throws ArrayIndexOutOfBoundsException (column out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalByte containing the element at position (rowIndex, columnIndex + 1), or empty if columnIndex == columnCount - 1
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalByte valueRight(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? OptionalByte.empty() : OptionalByte.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as a byte array.
     *
     * <p><b>Note:</b> This method returns a reference to the internal array, not a copy.
     * Modifications to the returned array will affect the matrix. If you need an independent
     * copy, use {@link #rowCopy(int)} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * byte[] firstRow = matrix.rowView(0);   // returns [1, 2, 3] (live reference)
     * firstRow[0] = (byte) 99;               // also mutates the matrix (no copy)
     * matrix.get(0, 0);                      // returns (byte) 99
     *
     * matrix.rowView(1);                     // returns [4, 5, 6]
     * matrix.rowView(-1);                    // throws IllegalArgumentException (negative index)
     * matrix.rowView(5);                     // throws IllegalArgumentException (index >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return the specified row array (direct reference to internal storage)
     * @throws IllegalArgumentException if rowIndex &lt; 0 or rowIndex &gt;= rowCount
     */
    @Override
    public byte[] rowView(final int rowIndex) throws IllegalArgumentException {
        N.checkArgument(rowIndex >= 0 && rowIndex < rowCount, MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount);

        return a[rowIndex];
    }

    /**
     * Returns a defensive copy of the specified row.
     * Changes to the returned array do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * byte[] firstRow = matrix.rowCopy(0);   // returns [1, 2, 3] (independent copy)
     * firstRow[0] = (byte) 99;               // does NOT affect the matrix
     * matrix.get(0, 0);                      // returns (byte) 1
     *
     * matrix.rowCopy(1);                     // returns [4, 5, 6]
     * matrix.rowCopy(-1);                    // throws IllegalArgumentException (negative index)
     * matrix.rowCopy(5);                     // throws IllegalArgumentException (index >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new byte array containing the values from the specified row
     * @throws IllegalArgumentException if rowIndex &lt; 0 or rowIndex &gt;= rowCount
     */
    @Override
    public byte[] rowCopy(final int rowIndex) throws IllegalArgumentException {
        N.checkArgument(rowIndex >= 0 && rowIndex < rowCount, MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount);

        return N.copyOf(a[rowIndex], columnCount);
    }

    /**
     * Returns a copy of the specified column as a new byte array.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously. Modifications to the returned array
     * will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * byte[] firstColumn = matrix.columnCopy(0);   // returns [1, 4] (independent copy)
     * firstColumn[0] = (byte) 99;                  // does NOT affect the matrix
     * matrix.get(0, 0);                            // returns (byte) 1
     *
     * matrix.columnCopy(2);                        // returns [3, 6]
     * matrix.columnCopy(-1);                       // throws IllegalArgumentException (negative index)
     * matrix.columnCopy(5);                        // throws IllegalArgumentException (index >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new array containing the values from the specified column
     * @throws IllegalArgumentException if columnIndex &lt; 0 or columnIndex &gt;= columnCount
     */
    @Override
    public byte[] columnCopy(final int columnIndex) throws IllegalArgumentException {
        N.checkArgument(columnIndex >= 0 && columnIndex < columnCount, MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount);

        final byte[] c = new byte[rowCount];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[i][columnIndex];
        }

        return c;
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
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.setRow(0, new byte[] {7, 8, 9});
     * matrix.rowView(0);                            // returns [7, 8, 9]
     *
     * matrix.setRow(1, new byte[] {0, 0, 0});
     * matrix.get(1, 2);                            // returns (byte) 0
     *
     * matrix.setRow(0, new byte[] {1, 2});         // throws IllegalArgumentException (length != columnCount)
     * matrix.setRow(5, new byte[] {1, 2, 3});      // throws IllegalArgumentException (row index out of bounds)
     * }</pre>
     *
     * @param rowIndex the index of the row to set (0-based)
     * @param row the array of values to copy into the row; must have length equal to the number of columns
     * @throws NullPointerException if {@code row} is {@code null}
     * @throws IllegalArgumentException if rowIndex is out of bounds or row length does not match columnCount
     */
    public void setRow(final int rowIndex, final byte[] row) throws IllegalArgumentException {
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
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.setColumn(0, new byte[] {7, 8});
     * matrix.columnCopy(0);                        // returns [7, 8]
     *
     * matrix.setColumn(2, new byte[] {0, 0});
     * matrix.get(1, 2);                            // returns (byte) 0
     *
     * matrix.setColumn(0, new byte[] {1});         // throws IllegalArgumentException (length != rowCount)
     * matrix.setColumn(9, new byte[] {1, 2});      // throws IllegalArgumentException (column index out of bounds)
     * }</pre>
     *
     * @param columnIndex the index of the column to set (0-based)
     * @param column the array of values to copy into the column; must have length equal to the number of rows
     * @throws NullPointerException if {@code column} is {@code null}
     * @throws IllegalArgumentException if columnIndex is out of bounds or column length does not match rowCount
     */
    public void setColumn(final int columnIndex, final byte[] column) throws IllegalArgumentException {
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
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.updateRow(0, b -> (byte) (b * 2));
     * matrix.rowView(0);                          // returns [2, 4, 6]
     *
     * matrix.updateRow(1, b -> (byte) (b + 10));
     * matrix.rowView(1);                          // returns [14, 15, 16]
     *
     * matrix.updateRow(5, b -> b);               // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * matrix.updateRow(0, null);                 // throws IllegalArgumentException (operator is null)
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the operator
     * @param rowIndex the index of the row to update (0-based)
     * @param operator the unary operator to apply to each element in the row, taking a byte and returning a byte; must not be {@code null}
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} is out of bounds
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.ByteUnaryOperator<E> operator) throws E {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new ArrayIndexOutOfBoundsException(formatMsg(MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount));
        }

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < columnCount; i++) {
            a[rowIndex][i] = operator.applyAsByte(a[rowIndex][i]);
        }
    }

    /**
     * Updates all elements in the specified column by applying the given operator to each element.
     * The matrix is modified in-place. Each element in the column is transformed by the operator
     * and replaced with the result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.updateColumn(1, b -> (byte) (b + 10));
     * matrix.columnCopy(1);                       // returns [12, 15]
     *
     * matrix.updateColumn(0, b -> (byte) (b * 0));
     * matrix.columnCopy(0);                       // returns [0, 0]
     *
     * matrix.updateColumn(9, b -> b);            // throws ArrayIndexOutOfBoundsException (column out of bounds)
     * matrix.updateColumn(0, null);              // throws IllegalArgumentException (operator is null)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param columnIndex the index of the column to update (0-based)
     * @param operator the unary operator to apply to each element in the column, taking a byte and returning a byte; must not be {@code null}
     * @throws ArrayIndexOutOfBoundsException if {@code columnIndex} is out of bounds
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.ByteUnaryOperator<E> operator) throws E {
        if (columnIndex < 0 || columnIndex >= columnCount) {
            throw new ArrayIndexOutOfBoundsException(formatMsg(MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount));
        }

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = operator.applyAsByte(a[i][columnIndex]);
        }
    }

    /**
     * Returns a copy of the main diagonal elements from upper-left to lower-right.
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>This method extracts the main diagonal elements at positions (0,0), (1,1), (2,2), etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.getMainDiagonal();                   // returns [1, 5, 9]
     *
     * ByteMatrix single = ByteMatrix.of(new byte[][] {{42}});
     * single.getMainDiagonal();                   // returns [42]
     *
     * ByteMatrix nonSquare = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.getMainDiagonal();                // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new byte array containing the main diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public byte[] getMainDiagonal() throws IllegalStateException {
        checkIsSquare();

        final byte[] res = new byte[rowCount];

        for (int i = 0; i < rowCount; i++) {
            res[i] = a[i][i]; // NOSONAR
        }

        return res;
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
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.setMainDiagonal(new byte[] {10, 11, 12});
     * matrix.getMainDiagonal();                   // returns [10, 11, 12]
     * matrix.get(0, 0);                           // returns (byte) 10
     *
     * matrix.setMainDiagonal(new byte[] {1, 2});  // throws IllegalArgumentException (length != rowCount)
     *
     * ByteMatrix nonSquare = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.setMainDiagonal(new byte[] {1, 2});   // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must have length equal to rowCount
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if mainDiagonal array length does not equal rowCount
     */
    @Override
    public void setMainDiagonal(final byte[] mainDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgument(N.len(mainDiagonal) == rowCount, MSG_DIAGONAL_LENGTH_MISMATCH, rowCount, N.len(mainDiagonal));

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = mainDiagonal[i];
        }
    }

    /**
     * Updates all elements on the main diagonal (upper-left to lower-right) by applying the given operator.
     * The matrix must be square (same number of rows and columns).
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.updateMainDiagonal(b -> (byte) (b * 2));
     * matrix.getMainDiagonal();                   // returns [2, 10, 18]
     *
     * matrix.updateMainDiagonal(b -> (byte) 0);
     * matrix.getMainDiagonal();                   // returns [0, 0, 0]
     *
     * ByteMatrix nonSquare = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.updateMainDiagonal(b -> b);       // throws IllegalStateException (not square)
     * matrix.updateMainDiagonal(null);            // throws IllegalArgumentException (operator is null)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each diagonal element; must not be {@code null}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateMainDiagonal(final Throwables.ByteUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = operator.applyAsByte(a[i][i]);
        }
    }

    /**
     * Returns the elements on the anti-diagonal from upper-right to lower-left.
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>This method extracts the anti-diagonal (secondary diagonal) elements from
     * top-right to bottom-left, at positions (0,n-1), (1,n-2), (2,n-3), etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.getAntiDiagonal();                   // returns [3, 5, 7]
     *
     * ByteMatrix single = ByteMatrix.of(new byte[][] {{42}});
     * single.getAntiDiagonal();                   // returns [42]
     *
     * ByteMatrix nonSquare = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.getAntiDiagonal();                // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new byte array containing the anti-diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public byte[] getAntiDiagonal() throws IllegalStateException {
        checkIsSquare();

        final byte[] res = new byte[rowCount];

        for (int i = 0; i < rowCount; i++) {
            res[i] = a[i][columnCount - i - 1];
        }

        return res;
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
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.setAntiDiagonal(new byte[] {10, 11, 12});
     * matrix.getAntiDiagonal();                   // returns [10, 11, 12]
     * matrix.get(0, 2);                           // returns (byte) 10
     *
     * matrix.setAntiDiagonal(new byte[] {1, 2});  // throws IllegalArgumentException (length != rowCount)
     *
     * ByteMatrix nonSquare = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.setAntiDiagonal(new byte[] {1, 2});   // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must have length equal to rowCount
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if antiDiagonal array length does not equal rowCount
     */
    @Override
    public void setAntiDiagonal(final byte[] antiDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgument(N.len(antiDiagonal) == rowCount, MSG_DIAGONAL_LENGTH_MISMATCH, rowCount, N.len(antiDiagonal));

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = antiDiagonal[i];
        }
    }

    /**
     * Updates all elements on the anti-diagonal (upper-right to lower-left) by applying the given operator.
     * The matrix must be square (same number of rows and columns).
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.updateAntiDiagonal(b -> (byte) (b + 1));
     * matrix.getAntiDiagonal();                   // returns [4, 6, 8]
     *
     * matrix.updateAntiDiagonal(b -> (byte) 0);
     * matrix.getAntiDiagonal();                   // returns [0, 0, 0]
     *
     * ByteMatrix nonSquare = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.updateAntiDiagonal(b -> b);       // throws IllegalStateException (not square)
     * matrix.updateAntiDiagonal(null);            // throws IllegalArgumentException (operator is null)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each anti-diagonal element; must not be {@code null}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAntiDiagonal(final Throwables.ByteUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = operator.applyAsByte(a[i][columnCount - i - 1]);
        }
    }

    /**
     * Updates all elements in the matrix by applying the given operator to each element.
     * The matrix is modified in-place. This operation may be performed in parallel for large matrices
     * to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * matrix.updateAll(b -> (byte) (b * 2));
     * matrix.rowView(0);                          // returns [2, 4]
     * matrix.rowView(1);                          // returns [6, 8]
     *
     * // Byte overflow wraps modulo 256
     * ByteMatrix big = ByteMatrix.of(new byte[][] {{127}});
     * big.updateAll(b -> (byte) (b + 1));
     * big.get(0, 0);                              // returns (byte) -128 (127 + 1 wraps)
     *
     * ByteMatrix.empty().updateAll(b -> (byte) 9);                               // no-op, leaves empty matrix unchanged
     * matrix.updateAll((Throwables.ByteUnaryOperator<RuntimeException>) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the unary operator to apply to each element, taking a byte and returning a byte; must not be {@code null}
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.ByteUnaryOperator<E> operator) throws E {
        N.checkArgNotNull(operator, "operator");
        final Throwables.IntBiConsumer<E> cmd = (i, j) -> a[i][j] = operator.applyAsByte(a[i][j]);
        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));
    }

    /**
     * Updates all elements in the matrix based on their position by applying the given mapper.
     * The mapper receives the row and column indices (0-based) and returns the new value for that position.
     * The matrix is modified in-place. This operation may be performed in parallel for large matrices
     * to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{0, 0}, {0, 0}});
     * matrix.updateAll((i, j) -> (byte) (i + j));
     * matrix.rowView(0);                          // returns [0, 1]
     * matrix.rowView(1);                          // returns [1, 2]
     *
     * ByteMatrix idx = ByteMatrix.of(new byte[][] {{0, 0, 0}});
     * idx.updateAll((i, j) -> (byte) j);
     * idx.rowView(0);                             // returns [0, 1, 2]
     *
     * ByteMatrix.empty().updateAll((i, j) -> (byte) 9);                            // no-op on empty matrix
     * matrix.updateAll((Throwables.IntBiFunction<Byte, RuntimeException>) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the bi-function that takes {@code (rowIndex, columnIndex)} and returns the new byte value; must not be {@code null}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws NullPointerException if {@code mapper} returns {@code null} for any cell
     * @throws E if the mapper throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Byte, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final Throwables.IntBiConsumer<E> cmd = (i, j) -> a[i][j] = mapper.apply(i, j);
        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));
    }

    /**
     * Conditionally replaces elements in the matrix based on a predicate.
     * Each element that satisfies the predicate is replaced with the specified new value.
     * The matrix is modified in-place. This operation may be performed in parallel for large matrices
     * to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.replaceIf(b -> b % 2 == 0, (byte) 0);
     * matrix.rowView(0);                          // returns [1, 0, 3]
     * matrix.rowView(1);                          // returns [0, 5, 0]
     *
     * ByteMatrix m2 = ByteMatrix.of(new byte[][] {{5, 5}, {5, 5}});
     * m2.replaceIf(b -> b > 100, (byte) 0);      // no element matches
     * m2.rowView(0);                             // returns [5, 5] (unchanged)
     *
     * ByteMatrix.empty().replaceIf(b -> true, (byte) 0);                               // no-op on empty matrix
     * matrix.replaceIf((Throwables.BytePredicate<RuntimeException>) null, (byte) 0);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate the condition to test each element; returns {@code true} if the element should be replaced; must not be {@code null}
     * @param newValue the value to use as replacement
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.BytePredicate<E> predicate, final byte newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> cmd = (i, j) -> a[i][j] = predicate.test(a[i][j]) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));
    }

    /**
     * Conditionally replaces elements in the matrix based on their position.
     * Elements at positions where the predicate returns {@code true} are replaced with the new value.
     * The matrix is modified in-place. This operation may be performed in parallel for large matrices
     * to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * matrix.replaceIf((i, j) -> i == j, (byte) 0);     // replace the main diagonal
     * matrix.rowView(0);                                // returns [0, 2]
     * matrix.rowView(1);                                // returns [3, 0]
     *
     * ByteMatrix m2 = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * m2.replaceIf((i, j) -> j == 0, (byte) 9);  // replace the first column
     * m2.columnCopy(0);                          // returns [9, 9]
     *
     * ByteMatrix.empty().replaceIf((i, j) -> true, (byte) 0);                           // no-op on empty matrix
     * matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, (byte) 0);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the predicate
     * @param predicate the bi-predicate that takes {@code (rowIndex, columnIndex)} and returns {@code true} if the element should be replaced; must not be {@code null}
     * @param newValue the value to use as replacement
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final byte newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> cmd = (i, j) -> a[i][j] = predicate.test(i, j) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));
    }

    /**
     * Creates a new ByteMatrix by applying the given function to each element of this matrix.
     * The original matrix is not modified. This operation may be performed in parallel for large matrices
     * to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix doubled = matrix.map(b -> (byte) (b * 2));
     * doubled.rowView(0);                         // returns [2, 4]
     * doubled.rowView(1);                         // returns [6, 8]
     * matrix.rowView(0);                          // returns [1, 2] (original unchanged)
     *
     * // Byte overflow wraps modulo 256
     * ByteMatrix big = ByteMatrix.of(new byte[][] {{100}});
     * big.map(b -> (byte) (b + 50)).get(0, 0);   // returns (byte) -106 (150 wraps)
     *
     * ByteMatrix.empty().map(b -> b).isEmpty();  // returns true
     * matrix.map(null);                          // throws IllegalArgumentException (mapper is null)
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the function
     * @param mapper the unary operator to apply to each element, taking a byte and returning a byte; must not be {@code null}
     * @return a new {@code ByteMatrix} with the transformed values; the original matrix is unchanged
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> ByteMatrix map(final Throwables.ByteUnaryOperator<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final byte[][] result = new byte[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> cmd = (i, j) -> result[i][j] = mapper.applyAsByte(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return ByteMatrix.of(result);
    }

    /**
     * Creates a new object matrix by applying the given function to each element of this matrix.
     * The function transforms each primitive byte value to an object of the specified type.
     * The original matrix is not modified. This operation may be performed in parallel for large matrices
     * to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix byteMatrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * Matrix<String> stringMatrix = byteMatrix.mapToObj(b -> "Value: " + b, String.class);
     * stringMatrix.get(0, 0);                     // returns "Value: 1"
     * stringMatrix.get(1, 1);                     // returns "Value: 4"
     *
     * Matrix<Integer> squares = byteMatrix.mapToObj(b -> b * b, Integer.class);
     * squares.get(1, 0);                          // returns 9 (3 * 3)
     *
     * byteMatrix.mapToObj(b -> b + "", null);     // throws IllegalArgumentException (targetElementType is null)
     * byteMatrix.mapToObj(null, String.class);    // throws IllegalArgumentException (mapper is null)
     * }</pre>
     *
     * @param <R> the type of elements in the resulting matrix
     * @param <E> the type of exception that may be thrown by the function
     * @param mapper the function to transform each byte to an object of type {@code R}; must not be {@code null}
     * @param targetElementType the class of the target element type (used for array creation); must not be {@code null}
     * @return a new {@code Matrix<R>} with the transformed object values; the original matrix is unchanged
     * @throws IllegalArgumentException if {@code mapper} or {@code targetElementType} is {@code null}
     * @throws E if the function throws an exception
     */
    public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.ByteFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);
        final Throwables.IntBiConsumer<E> cmd = (i, j) -> result[i][j] = mapper.apply(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return Matrix.of(result);
    }

    /**
     * Fills all elements of the matrix with the specified value.
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * matrix.fill((byte) 5);
     * matrix.rowView(0);                          // returns [5, 5]
     * matrix.rowView(1);                          // returns [5, 5]
     *
     * matrix.fill((byte) 0);
     * matrix.get(0, 0);                           // returns (byte) 0
     *
     * ByteMatrix.empty().fill((byte) 9);         // no-op on empty matrix
     * }</pre>
     *
     * @param value the value to fill the matrix with
     */
    public void fill(final byte value) {
        for (int i = 0; i < rowCount; i++) {
            N.fill(a[i], value);
        }
    }

    /**
     * Fills this matrix with values from another two-dimensional byte array, starting from position {@code [0,0]}.
     * Only the overlapping region is written. If the source array is smaller than this matrix,
     * only the overlapping portion is modified (cells outside the source remain unchanged).
     * If the source array is larger, only the portion that fits within this matrix is copied.
     * Source rows that are {@code null} are skipped (the corresponding destination row is left untouched).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{0, 0, 0}, {0, 0, 0}});
     * matrix.fill(new byte[][] {{1, 2}, {3, 4}});       // source smaller than this matrix
     * matrix.rowView(0);                                // returns [1, 2, 0] (third column untouched)
     * matrix.rowView(1);                                // returns [3, 4, 0]
     *
     * ByteMatrix big = ByteMatrix.of(new byte[][] {{0, 0}});
     * big.fill(new byte[][] {{1, 2, 3}, {4, 5, 6}});    // source larger than this matrix
     * big.rowView(0);                                   // returns [1, 2] (only the fitting part is copied)
     *
     * matrix.fill((byte[][]) null);              // throws IllegalArgumentException (source is null)
     * }</pre>
     *
     * @param source the source array to copy values from; must not be {@code null}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @see #fill(int, int, byte[][])
     */
    public void fill(final byte[][] source) {
        fill(0, 0, source);
    }

    /**
     * Fills a portion of this matrix with values from another two-dimensional byte array.
     * The filling starts at the specified position and copies as many values as possible
     * without exceeding the bounds of either array. Source rows that are {@code null} are
     * skipped (the corresponding destination row is left untouched).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}});
     * matrix.fill(1, 1, new byte[][] {{1, 2}, {3, 4}});
     * matrix.rowView(0);                          // returns [0, 0, 0]
     * matrix.rowView(1);                          // returns [0, 1, 2]
     * matrix.rowView(2);                          // returns [0, 3, 4]
     *
     * ByteMatrix m2 = ByteMatrix.of(new byte[][] {{0, 0}, {0, 0}});
     * m2.fill(1, 0, new byte[][] {{7, 8}, {9, 9}});    // only the fitting part is copied
     * m2.rowView(1);                                   // returns [7, 8]
     *
     * matrix.fill(-1, 0, new byte[][] {{1}});    // throws IllegalArgumentException (negative destRowIndex)
     * matrix.fill(0, 0, (byte[][]) null);        // throws IllegalArgumentException (source is null)
     * }</pre>
     *
     * @param destRowIndex the target row index in this matrix; must be in {@code [0, rowCount]}
     * @param destColumnIndex the target column index in this matrix; must be in {@code [0, columnCount]}
     * @param source the source array to copy values from; must not be {@code null}
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if {@code destRowIndex}
     *         or {@code destColumnIndex} is negative or exceeds the corresponding matrix dimension
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final byte[][] source) throws IllegalArgumentException {
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
     * The returned matrix is a completely independent copy; modifications to one
     * do not affect the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix original = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix copy = original.copy();
     * copy.equals(original);                     // returns true (same contents)
     * copy.set(0, 0, (byte) 10);                 // mutating the copy...
     * original.get(0, 0);                        // returns (byte) 1 (original unchanged)
     * copy.get(0, 0);                            // returns (byte) 10
     *
     * ByteMatrix.empty().copy().isEmpty();       // returns true
     * }</pre>
     *
     * @return a copy of this matrix
     */
    @Override
    public ByteMatrix copy() {
        final byte[][] c = new byte[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[i].clone();
        }

        return new ByteMatrix(c);
    }

    /**
     * Creates a copy of a row range from this matrix.
     * The returned matrix contains only the specified rows and is completely independent from the original matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}, {5, 6}});
     * ByteMatrix subset = matrix.copy(0, 2);
     * subset.rowCount();                          // returns 2
     * subset.rowView(0);                          // returns [1, 2]
     * subset.rowView(1);                          // returns [3, 4]
     *
     * ByteMatrix empty = matrix.copy(1, 1);
     * empty.rowCount();                           // returns 0 (from == to)
     *
     * matrix.copy(0, 5);                          // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new ByteMatrix containing the specified rows
     * @throws IndexOutOfBoundsException if the indices are out of bounds
     */
    @Override
    public ByteMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        final byte[][] c = new byte[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = a[i].clone();
        }

        return new ByteMatrix(c);
    }

    /**
     * Creates a copy of a rectangular region from this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * ByteMatrix subset = matrix.copy(0, 2, 1, 3);
     * subset.rowView(0);                          // returns [2, 3]
     * subset.rowView(1);                          // returns [5, 6]
     *
     * ByteMatrix corner = matrix.copy(2, 3, 0, 1);
     * corner.rowView(0);                          // returns [7]
     *
     * matrix.copy(0, 2, 1, 9);                    // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.copy(0, 5, 0, 2);                    // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new ByteMatrix containing the specified region
     * @throws IndexOutOfBoundsException if any indices are out of bounds
     */
    @Override
    public ByteMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        final byte[][] c = new byte[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = N.copyOfRange(a[i], fromColumnIndex, toColumnIndex);
        }

        return new ByteMatrix(c);
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
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     *
     * // Grow: both dimensions larger — new cells filled with 0
     * ByteMatrix grown = matrix.resize(4, 4);
     * // Result: [[1, 2, 3, 0],
     * //          [4, 5, 6, 0],
     * //          [7, 8, 9, 0],
     * //          [0, 0, 0, 0]]
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * ByteMatrix truncated = matrix.resize(2, 2);
     * // Result: [[1, 2],
     * //          [4, 5]]
     *
     * // Mixed: grow rows, truncate columns
     * ByteMatrix mixed = matrix.resize(4, 2);
     * // Result: [[1, 2],
     * //          [4, 5],
     * //          [7, 8],
     * //          [0, 0]]
     * grown.get(3, 3);                            // returns (byte) 0 (newly added cell)
     * truncated.rowCount();                       // returns 2
     * mixed.get(3, 0);                            // returns (byte) 0 (newly added row)
     *
     * matrix.resize(-1, 2);                       // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new ByteMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int, byte)
     * @see #extend(int, int, int, int)
     */
    public ByteMatrix resize(final int newRowCount, final int newColumnCount) {
        return resize(newRowCount, newColumnCount, BYTE_0);
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
     * <p><b>Comparison with {@link #extend(int, int, int, int, byte)}:</b>
     * {@code resize} takes <em>absolute</em> target dimensions and may truncate existing content.
     * {@code extend} takes <em>relative</em> padding amounts per edge and <em>never truncates</em>.
     * Use {@code extend} when the entire original content must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     *
     * // Grow: fill new cells with 9
     * ByteMatrix grown = matrix.resize(4, 4, (byte) 9);
     * // Result: [[1, 2, 3, 9],
     * //          [4, 5, 6, 9],
     * //          [7, 8, 9, 9],
     * //          [9, 9, 9, 9]]
     *
     * // Truncate: defaultValue is ignored when shrinking
     * ByteMatrix truncated = matrix.resize(2, 2, (byte) 9);
     * // Result: [[1, 2],
     * //          [4, 5]]
     * grown.get(3, 3);                            // returns (byte) 9 (newly added cell)
     * grown.get(0, 0);                            // returns (byte) 1 (existing cell preserved)
     * truncated.get(1, 1);                        // returns (byte) 5 (defaultValue not used)
     *
     * matrix.resize(2, -1, (byte) 9);             // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the value used to fill cells that are added when a dimension grows;
     *        ignored when a dimension shrinks
     * @return a new ByteMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int)
     * @see #extend(int, int, int, int, byte)
     */
    public ByteMatrix resize(final int newRowCount, final int newColumnCount, final byte defaultValue) throws IllegalArgumentException {
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
            final boolean fillDefaultValue = defaultValue != BYTE_0;
            final byte[][] b = new byte[newRowCount][];

            for (int i = 0; i < newRowCount; i++) {
                b[i] = i < rowCount ? N.copyOf(a[i], newColumnCount) : new byte[newColumnCount];

                if (fillDefaultValue) {
                    if (i >= rowCount) {
                        N.fill(b[i], defaultValue);
                    } else if (columnCount < newColumnCount) {
                        N.fill(b[i], columnCount, newColumnCount, defaultValue);
                    }
                }
            }

            return new ByteMatrix(b);
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
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     *
     * // Uniform 1-cell border of 0
     * ByteMatrix bordered = matrix.extend(1, 1, 1, 1);
     * // Result: [[0, 0, 0, 0],
     * //          [0, 1, 2, 0],
     * //          [0, 3, 4, 0],
     * //          [0, 0, 0, 0]]
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * ByteMatrix shifted = matrix.extend(0, 0, 2, 0);
     * // Result: [[0, 0, 1, 2],
     * //          [0, 0, 3, 4]]
     * bordered.rowCount();                        // returns 4
     * bordered.get(1, 1);                         // returns (byte) 1 (original content preserved)
     * bordered.get(0, 0);                         // returns (byte) 0 (new border cell)
     * shifted.get(0, 2);                          // returns (byte) 1
     *
     * matrix.extend(0, 0, 0, 0).equals(matrix);  // returns true (no padding -> copy)
     * matrix.extend(-1, 0, 0, 0);                // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @return a new ByteMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any parameter is negative,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #extend(int, int, int, int, byte)
     * @see #resize(int, int)
     */
    @Override
    public ByteMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight) {
        return extend(padTop, padBottom, padLeft, padRight, BYTE_0);
    }

    /**
     * Returns a new matrix formed by surrounding this matrix with padding on all four edges.
     * New cells are filled with {@code defaultValue}.
     *
     * <p>Unlike {@link #resize(int, int, byte)}, this method <b>never truncates</b>: the entire
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
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     *
     * // Uniform 1-cell border filled with 9
     * ByteMatrix bordered = matrix.extend(1, 1, 1, 1, (byte) 9);
     * // Result: [[9, 9, 9, 9],
     * //          [9, 1, 2, 9],
     * //          [9, 3, 4, 9],
     * //          [9, 9, 9, 9]]
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * ByteMatrix shifted = matrix.extend(0, 0, 2, 0, (byte) 0);
     * // Result: [[0, 0, 1, 2],
     * //          [0, 0, 3, 4]]
     * bordered.get(0, 0);                         // returns (byte) 9 (new border cell)
     * bordered.get(1, 1);                         // returns (byte) 1 (original content preserved)
     * shifted.get(0, 0);                          // returns (byte) 0
     * shifted.get(0, 2);                          // returns (byte) 1
     *
     * matrix.extend(0, 0, 0, 0, (byte) 9).equals(matrix);   // returns true (no padding -> copy)
     * matrix.extend(0, -1, 0, 0, (byte) 9);                 // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @param defaultValue the value to fill all new padding cells with
     * @return a new ByteMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #extend(int, int, int, int)
     * @see #resize(int, int, byte)
     */
    public ByteMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final byte defaultValue)
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
            final boolean fillDefaultValue = defaultValue != BYTE_0;
            final byte[][] b = new byte[newRowCount][newColumnCount];

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

            return new ByteMatrix(b);
        }
    }

    /**
     * Reverses the order of elements in each row horizontally in-place.
     * This modifies the matrix directly. For a non-destructive version, use {@link #flipHorizontally()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.flipHorizontallyInPlace();
     * matrix.rowView(0);                          // returns [3, 2, 1]
     * matrix.rowView(1);                          // returns [6, 5, 4]
     *
     * ByteMatrix single = ByteMatrix.of(new byte[][] {{7}});
     * single.flipHorizontallyInPlace();
     * single.get(0, 0);                           // returns (byte) 7 (single column unchanged)
     *
     * ByteMatrix.empty().flipHorizontallyInPlace();   // no-op on empty matrix
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
     * This modifies the matrix directly. For a non-destructive version, use {@link #flipVertically()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.flipVerticallyInPlace();
     * matrix.rowView(0);                          // returns [5, 6]
     * matrix.rowView(2);                          // returns [1, 2]
     *
     * ByteMatrix single = ByteMatrix.of(new byte[][] {{7, 8}});
     * single.flipVerticallyInPlace();
     * single.rowView(0);                          // returns [7, 8] (single row unchanged)
     *
     * ByteMatrix.empty().flipVerticallyInPlace();   // no-op on empty matrix
     * }</pre>
     *
     * @see #flipVertically()
     */
    @Override
    public void flipVerticallyInPlace() {
        for (int l = 0, h = rowCount - 1; l < h; l++, h--) {
            final byte[] tmp = a[l];
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
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * ByteMatrix flipped = matrix.flipHorizontally();
     * flipped.rowView(0);                         // returns [3, 2, 1]
     * flipped.rowView(1);                         // returns [6, 5, 4]
     * matrix.rowView(0);                          // returns [1, 2, 3] (original unchanged)
     *
     * ByteMatrix.empty().flipHorizontally().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new ByteMatrix that is a horizontal flip of this matrix (each row reversed)
     * @see #flipHorizontallyInPlace()
     * @see #flipVertically()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public ByteMatrix flipHorizontally() {
        final ByteMatrix res = this.copy();
        res.flipHorizontallyInPlace();
        return res;
    }

    /**
     * Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * ByteMatrix flipped = matrix.flipVertically();
     * flipped.rowView(0);                         // returns [4, 5, 6]
     * flipped.rowView(1);                         // returns [1, 2, 3]
     * matrix.rowView(0);                          // returns [1, 2, 3] (original unchanged)
     *
     * ByteMatrix.empty().flipVertically().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new ByteMatrix that is a vertical flip of this matrix (rows in reversed order)
     * @see #flipVerticallyInPlace()
     * @see #flipHorizontally()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public ByteMatrix flipVertically() {
        final ByteMatrix res = this.copy();
        res.flipVerticallyInPlace();
        return res;
    }

    /**
     * Returns a new matrix that is this matrix rotated 90 degrees clockwise.
     * The dimensions are transposed: a matrix with dimensions (rowCount x columnCount) becomes (columnCount x rowCount).
     *
     * <p>Rotation rules:
     * <ul>
     * <li>Element at position [i][j] moves to position [j][rowCount-1-i]</li>
     * <li>The first row becomes the last column</li>
     * <li>The last row becomes the first column</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix rotated = matrix.rotate90();
     * rotated.rowView(0);                         // returns [3, 1]
     * rotated.rowView(1);                         // returns [4, 2]
     *
     * ByteMatrix wide = ByteMatrix.of(new byte[][] {{1, 2, 3}});   // 1x3
     * ByteMatrix tall = wide.rotate90();                           // becomes 3x1
     * tall.rowView(0);                                             // returns [1]
     * tall.rowCount();                                             // returns 3
     *
     * ByteMatrix.empty().rotate90().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new matrix rotated 90 degrees clockwise with dimensions {@code (columnCount x rowCount)}
     * @throws IllegalArgumentException if the transposed shape {@code (columnCount x rowCount)} is not representable
     * @see #rotate180()
     * @see #rotate270()
     */
    @Override
    public ByteMatrix rotate90() {
        if (columnCount == 0) {
            return EMPTY_BYTE_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final byte[][] c = new byte[columnCount][rowCount];

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

        return new ByteMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 180 degrees clockwise.
     * The dimensions remain the same: a matrix with dimensions (rowCount x columnCount) stays (rowCount x columnCount).
     * This is equivalent to reversing both rows and columns.
     *
     * <p>Rotation rules:
     * <ul>
     * <li>Element at position [i][j] moves to position [rowCount-1-i][columnCount-1-j]</li>
     * <li>The first row becomes the last row reversed</li>
     * <li>The last row becomes the first row reversed</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix rotated = matrix.rotate180();
     * rotated.rowView(0);                         // returns [4, 3]
     * rotated.rowView(1);                         // returns [2, 1]
     *
     * ByteMatrix row = ByteMatrix.of(new byte[][] {{1, 2, 3}});
     * row.rotate180().rowView(0);                 // returns [3, 2, 1]
     *
     * ByteMatrix.empty().rotate180().isEmpty();  // returns true
     * }</pre>
     *
     * @return a new matrix rotated 180 degrees with the same dimensions
     * @see #rotate90()
     * @see #rotate270()
     */
    @Override
    public ByteMatrix rotate180() {
        final byte[][] c = new byte[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[rowCount - i - 1].clone();
            N.reverse(c[i]);
        }

        return new ByteMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 270 degrees clockwise (or 90 degrees counter-clockwise).
     * The dimensions are transposed: a matrix with dimensions (rowCount x columnCount) becomes (columnCount x rowCount).
     *
     * <p>Rotation rules:
     * <ul>
     * <li>Element at position [i][j] moves to position [columnCount-1-j][i]</li>
     * <li>The first row becomes the first column</li>
     * <li>The last row becomes the last column</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix rotated = matrix.rotate270();
     * rotated.rowView(0);                         // returns [2, 4]
     * rotated.rowView(1);                         // returns [1, 3]
     *
     * ByteMatrix wide = ByteMatrix.of(new byte[][] {{1, 2, 3}});   // 1x3
     * ByteMatrix tall = wide.rotate270();                          // becomes 3x1
     * tall.rowView(0);                                             // returns [3]
     * tall.rowCount();                                             // returns 3
     *
     * ByteMatrix.empty().rotate270().isEmpty();  // returns true
     * }</pre>
     *
     * @return a new matrix rotated 270 degrees clockwise with dimensions {@code (columnCount x rowCount)}
     * @throws IllegalArgumentException if the transposed shape {@code (columnCount x rowCount)} is not representable
     * @see #rotate90()
     * @see #rotate180()
     */
    @Override
    public ByteMatrix rotate270() {
        if (columnCount == 0) {
            return EMPTY_BYTE_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final byte[][] c = new byte[columnCount][rowCount];

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

        return new ByteMatrix(c);
    }

    /**
     * Creates the transpose of this matrix by swapping rows and columns.
     * The transpose operation converts each row into a column, so element at position (i, j)
     * in the original matrix appears at position (j, i) in the transposed matrix. The resulting
     * matrix has dimensions swapped (rowCount × columnCount becomes columnCount × rowCount).
     * Creates a new matrix; the original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:  Transposed:
     * // 1 2 3      1 4
     * // 4 5 6      2 5
     * //            3 6
     *
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * ByteMatrix transposed = matrix.transpose();   // 2x3 becomes 3x2
     * transposed.rowCount();                        // returns 3
     * transposed.rowView(0);                        // returns [1, 4]
     * transposed.rowView(2);                        // returns [3, 6]
     *
     * ByteMatrix square = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * square.transpose().rowView(0);              // returns [1, 3]
     *
     * ByteMatrix.empty().transpose().isEmpty();  // returns true
     * }</pre>
     *
     * @return a new matrix that is the transpose of this matrix with dimensions {@code columnCount × rowCount};
     *         an {@code N x 0} matrix transposes to the empty {@code 0 x 0} matrix, because the swapped shape
     *         {@code 0 x N} (zero rows with a non-zero column count) is not representable
     * @throws IllegalArgumentException if the transposed shape {@code (columnCount × rowCount)} is not representable
     */
    @Override
    public ByteMatrix transpose() {
        if (columnCount == 0) {
            return EMPTY_BYTE_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final byte[][] c = new byte[columnCount][rowCount];

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

        return new ByteMatrix(c);
    }

    /**
     * Reshapes the matrix to new dimensions while preserving element order.
     * Elements are read in row-major order from the original matrix and placed into the new shape.
     *
     * <p>The reshaping process follows these rules:
     * <ul>
     * <li>Elements are extracted from the original matrix in row-major order (left to right, top to bottom)</li>
     * <li>Elements are placed into the new matrix in row-major order</li>
     * <li>The new shape must have at least as many total elements as the original ({@code newRowCount * newColumnCount >= elementCount()})</li>
     * <li>If the new shape has more total elements, the additional positions are filled with zeros</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * ByteMatrix reshaped = matrix.reshape(3, 2);    // Becomes [[1, 2], [3, 4], [5, 6]]
     * reshaped.rowView(0);                           // returns [1, 2]
     * reshaped.rowView(2);                           // returns [5, 6]
     *
     * ByteMatrix extended = matrix.reshape(2, 4);    // Becomes [[1, 2, 3, 4], [5, 6, 0, 0]]
     * extended.rowView(1);                           // returns [5, 6, 0, 0] (extra cells filled with 0)
     *
     * matrix.reshape(1, 5);                       // throws IllegalArgumentException (too small for 6 elements)
     * matrix.reshape(-1, 2);                      // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix; must be {@code >= 0}
     * @param newColumnCount the number of columns in the reshaped matrix; must be {@code >= 0}
     * @return a new {@code ByteMatrix} with the specified shape containing this matrix's elements
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the requested shape is not representable (i.e. {@code newRowCount} is {@code 0} while
     *         {@code newColumnCount} is positive), or if the new shape is too small to hold all elements
     *         of this matrix
     * @see #resize(int, int)
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public ByteMatrix reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final byte[][] c = new byte[newRowCount][newColumnCount];

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return new ByteMatrix(c);
        }

        final int rowLen = (int) N.min(newRowCount, elementCount % newColumnCount == 0 ? elementCount / newColumnCount : elementCount / newColumnCount + 1);

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

        return new ByteMatrix(c);
    }

    /**
     * Creates a new matrix by repeating each element multiple times.
     * Each element is repeated rowRepeats times vertically and columnRepeats times horizontally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix repeated = matrix.repeatElements(2, 3);
     * // repeated is: [[1, 1, 1, 2, 2, 2],
     * //               [1, 1, 1, 2, 2, 2],
     * //               [3, 3, 3, 4, 4, 4],
     * //               [3, 3, 3, 4, 4, 4]]
     * repeated.rowCount();                        // returns 4
     * repeated.columnCount();                     // returns 6
     * repeated.rowView(0);                        // returns [1, 1, 1, 2, 2, 2]
     *
     * matrix.repeatElements(1, 1).equals(matrix); // returns true (1x1 repeat is identity)
     * matrix.repeatElements(0, 2);                // throws IllegalArgumentException (repeats not positive)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat each element vertically
     * @param columnRepeats number of times to repeat each element horizontally
     * @return a new matrix with repeated elements
     * @throws IllegalArgumentException if rowRepeats or columnRepeats is not positive,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#repeatElements(int, int)
     */
    @Override
    public ByteMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final byte[][] c = new byte[rowCount * rowRepeats][columnCount * columnRepeats];

        for (int i = 0; i < rowCount; i++) {
            final byte[] aa = a[i];
            final byte[] fr = c[i * rowRepeats];

            for (int j = 0; j < columnCount; j++) {
                N.copy(Array.repeat(aa[j], columnRepeats), 0, fr, j * columnRepeats, columnRepeats);
            }

            for (int k = 1; k < rowRepeats; k++) {
                N.copy(fr, 0, c[i * rowRepeats + k], 0, fr.length);
            }
        }

        return new ByteMatrix(c);
    }

    /**
     * Creates a new matrix by repeating the entire matrix multiple times.
     * The matrix is tiled rowRepeats times vertically and columnRepeats times horizontally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix repeated = matrix.repeatMatrix(2, 3);
     * // repeated is: [[1, 2, 1, 2, 1, 2],
     * //               [3, 4, 3, 4, 3, 4],
     * //               [1, 2, 1, 2, 1, 2],
     * //               [3, 4, 3, 4, 3, 4]]
     * repeated.rowCount();                        // returns 4
     * repeated.columnCount();                     // returns 6
     * repeated.rowView(0);                        // returns [1, 2, 1, 2, 1, 2]
     *
     * matrix.repeatMatrix(1, 1).equals(matrix);  // returns true (1x1 tile is identity)
     * matrix.repeatMatrix(2, 0);                 // throws IllegalArgumentException (repeats not positive)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat the matrix vertically
     * @param columnRepeats number of times to repeat the matrix horizontally
     * @return a new matrix with the original matrix repeated
     * @throws IllegalArgumentException if rowRepeats or columnRepeats is not positive,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#repeatMatrix(int, int)
     */
    @Override
    public ByteMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final byte[][] c = new byte[rowCount * rowRepeats][columnCount * columnRepeats];

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

        return new ByteMatrix(c);
    }

    /**
     * Returns a list containing all matrix elements in row-major order.
     * This effectively converts the two-dimensional matrix into a one-dimensional list.
     *
     * <p>Elements are extracted row by row from left to right, starting from the first row.
     * This is useful for bulk operations or when you need all matrix values as a flat collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteList list = matrix.flatten();   // ByteList of [1, 2, 3, 4]
     * list.size();                        // returns 4
     * list.get(0);                        // returns (byte) 1
     * list.get(3);                        // returns (byte) 4
     *
     * ByteMatrix.empty().flatten().size();                  // returns 0
     * ByteMatrix.of(new byte[][] {{7}}).flatten().get(0);   // returns (byte) 7
     * }</pre>
     *
     * @return a new ByteList containing all elements in row-major order
     * @throws IllegalStateException if the matrix is too large to flatten (rowCount * columnCount &gt; Integer.MAX_VALUE)
     * @see #horizontalStream()
     */
    @Override
    public ByteList flatten() {
        // Check for overflow before allocation
        if ((long) rowCount * columnCount > Integer.MAX_VALUE) {
            throw new IllegalStateException("Matrix too large to flatten: " + rowCount + " x " + columnCount);
        }

        final byte[] c = new byte[rowCount * columnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c, i * columnCount, columnCount);
        }

        return ByteList.of(c);
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
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{5, 3}, {4, 1}});
     * matrix.mutateAsFlat(arr -> java.util.Arrays.sort(arr));
     * matrix.rowView(0);                          // returns [1, 3] (sorted globally, placed back row by row)
     * matrix.rowView(1);                          // returns [4, 5]
     *
     * ByteMatrix doubled = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * doubled.mutateAsFlat(arr -> { for (int i = 0; i < arr.length; i++) arr[i] *= 2; });
     * doubled.get(1, 1);                          // returns (byte) 8
     *
     * ByteMatrix.empty().mutateAsFlat(arr -> java.util.Arrays.sort(arr));   // no-op on empty matrix
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the operation
     * @param action the operation to apply to the flattened array
     * @throws E if the operation throws an exception
     * @see Arrays#mutateAsFlat(byte[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super byte[], E> action) throws E {
        Arrays.mutateAsFlat(a, action);
    }

    /**
     * Stacks this matrix vertically with another matrix (row-wise concatenation).
     * The matrices must have the same number of columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix1 = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix matrix2 = ByteMatrix.of(new byte[][] {{5, 6}, {7, 8}});
     * ByteMatrix stacked = matrix1.stackVertically(matrix2);
     * stacked.rowCount();                         // returns 4
     * stacked.rowView(0);                         // returns [1, 2]
     * stacked.rowView(3);                         // returns [7, 8]
     *
     * ByteMatrix mismatch = ByteMatrix.of(new byte[][] {{9, 9, 9}});
     * matrix1.stackVertically(mismatch);            // throws IllegalArgumentException (column count differs)
     * matrix1.stackVertically((ByteMatrix) null);   // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to stack below this matrix
     * @return a new ByteMatrix with other appended below this matrix
     * @throws IllegalArgumentException if {@code other} is {@code null}, has a different column count,
     *         or if the merged row count would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#stackVertically(IntMatrix)
     */
    @Override
    public ByteMatrix stackVertically(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.columnCount, MSG_VSTACK_COLUMN_MISMATCH, columnCount, other.columnCount);
        final long mergedRowCount = (long) rowCount + other.rowCount;
        N.checkArgument(mergedRowCount <= Integer.MAX_VALUE, "Merged row count overflow: {} + {} = {}", rowCount, other.rowCount, mergedRowCount);

        final byte[][] c = new byte[(int) mergedRowCount][];
        int j = 0;

        for (int i = 0; i < rowCount; i++) {
            c[j++] = a[i].clone();
        }

        for (int i = 0; i < other.rowCount; i++) {
            c[j++] = other.a[i].clone();
        }

        return ByteMatrix.of(c);
    }

    /**
     * Stacks this matrix horizontally with another matrix (column-wise concatenation).
     * The matrices must have the same number of rows.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix1 = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix matrix2 = ByteMatrix.of(new byte[][] {{5, 6}, {7, 8}});
     * ByteMatrix stacked = matrix1.stackHorizontally(matrix2);
     * stacked.columnCount();                      // returns 4
     * stacked.rowView(0);                         // returns [1, 2, 5, 6]
     * stacked.rowView(1);                         // returns [3, 4, 7, 8]
     *
     * ByteMatrix mismatch = ByteMatrix.of(new byte[][] {{9, 9}});
     * matrix1.stackHorizontally(mismatch);            // throws IllegalArgumentException (row count differs)
     * matrix1.stackHorizontally((ByteMatrix) null);   // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to concatenate to the right of this matrix
     * @return a new ByteMatrix with other appended to the right of this matrix
     * @throws IllegalArgumentException if {@code other} is {@code null}, has a different row count,
     *         or if the merged column count would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#stackHorizontally(IntMatrix)
     */
    @Override
    public ByteMatrix stackHorizontally(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(rowCount == other.rowCount, MSG_HSTACK_ROW_MISMATCH, rowCount, other.rowCount);
        final long mergedColumnCount = (long) columnCount + other.columnCount;
        N.checkArgument(mergedColumnCount <= Integer.MAX_VALUE, "Merged column count overflow: {} + {} = {}", columnCount, other.columnCount,
                mergedColumnCount);

        final byte[][] c = new byte[rowCount][(int) mergedColumnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c[i], 0, columnCount);
            N.copy(other.a[i], 0, c[i], columnCount, other.columnCount);
        }

        return ByteMatrix.of(c);
    }

    /**
     * Performs element-wise addition with another matrix of the same dimensions.
     * The operation may be parallelized for large matrices to improve performance.
     *
     * <p><b>Important:</b> Byte overflow may occur during addition. If the sum exceeds the byte
     * range (-128 to 127), the result will wrap around. For example, (byte)127 + (byte)1 = (byte)-128.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix1 = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix matrix2 = ByteMatrix.of(new byte[][] {{5, 6}, {7, 8}});
     * ByteMatrix sum = matrix1.add(matrix2);
     * sum.rowView(0);                             // returns [6, 8]
     * sum.rowView(1);                             // returns [10, 12]
     *
     * // Byte overflow wraps modulo 256
     * ByteMatrix a = ByteMatrix.of(new byte[][] {{127}});
     * ByteMatrix b = ByteMatrix.of(new byte[][] {{1}});
     * a.add(b).get(0, 0);                         // returns (byte) -128 (127 + 1 wraps)
     *
     * ByteMatrix mismatch = ByteMatrix.of(new byte[][] {{1, 2, 3}});
     * matrix1.add(mismatch);                     // throws IllegalArgumentException (different shapes)
     * matrix1.add((ByteMatrix) null);            // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to add to this matrix; must have the same dimensions
     * @return a new ByteMatrix containing the element-wise sum
     * @throws IllegalArgumentException if {@code other} is {@code null} or has different dimensions (rows or columns don't match)
     * @see #subtract(ByteMatrix)
     */
    public ByteMatrix add(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot add matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final byte[][] otherArray = other.a;
        final byte[][] result = new byte[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> cmd = (i, j) -> result[i][j] = (byte) (a[i][j] + otherArray[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return ByteMatrix.of(result);
    }

    /**
     * Performs element-wise subtraction with another matrix of the same dimensions.
     * The operation may be parallelized for large matrices to improve performance.
     *
     * <p><b>Important:</b> Byte underflow may occur during subtraction. If the difference goes below
     * the byte range (-128 to 127), the result will wrap around. For example, (byte)-128 - (byte)1 = (byte)127.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix1 = ByteMatrix.of(new byte[][] {{5, 6}, {7, 8}});
     * ByteMatrix matrix2 = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix diff = matrix1.subtract(matrix2);
     * diff.rowView(0);                            // returns [4, 4]
     * diff.rowView(1);                            // returns [4, 4]
     *
     * // Byte underflow wraps modulo 256
     * ByteMatrix a = ByteMatrix.of(new byte[][] {{-128}});
     * ByteMatrix b = ByteMatrix.of(new byte[][] {{1}});
     * a.subtract(b).get(0, 0);                    // returns (byte) 127 (-128 - 1 wraps)
     *
     * ByteMatrix mismatch = ByteMatrix.of(new byte[][] {{1, 2, 3}});
     * matrix1.subtract(mismatch);                // throws IllegalArgumentException (different shapes)
     * matrix1.subtract((ByteMatrix) null);       // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to subtract from this matrix; must have the same dimensions
     * @return a new ByteMatrix containing the element-wise difference
     * @throws IllegalArgumentException if {@code other} is {@code null} or has different dimensions (rows or columns don't match)
     * @see #add(ByteMatrix)
     */
    public ByteMatrix subtract(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot subtract matrices with different shapes: this is {}x{} but other is {}x{}", rowCount,
                columnCount, other.rowCount, other.columnCount);

        final byte[][] otherArray = other.a;
        final byte[][] result = new byte[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> cmd = (i, j) -> result[i][j] = (byte) (a[i][j] - otherArray[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return ByteMatrix.of(result);
    }

    /**
     * Multiplies this matrix by another matrix (matrix multiplication).
     * The number of columns in this matrix must equal the number of rows in the other matrix.
     * The resulting matrix will have dimensions {@code (this.rowCount x other.columnCount)}.
     *
     * <p>This operation is computationally intensive and may be parallelized for large matrices.
     * Matrix multiplication is not commutative (A*B != B*A).</p>
     *
     * <p><b>Important:</b> Byte overflow may occur during multiplication and accumulation. Each
     * partial product {@code a[i][k] * other[k][j]} is computed as an {@code int} (via Java's numeric
     * promotion), but it is then accumulated into the {@code byte} result cell with implicit narrowing,
     * so intermediate sums wrap modulo 256 and the final result is always in the byte range
     * {@code [-128, 127]}. If a non-wrapping product is required, widen via {@link #toIntMatrix()}
     * (or {@link #toLongMatrix()}) and multiply there.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix a = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix b = ByteMatrix.of(new byte[][] {{5, 6}, {7, 8}});
     * ByteMatrix product = a.matmul(b);
     * product.rowView(0);                         // returns [19, 22]
     * product.rowView(1);                         // returns [43, 50]
     *
     * // Non-square: (2x3) x (3x2) -> (2x2)
     * ByteMatrix m = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * ByteMatrix n = ByteMatrix.of(new byte[][] {{1, 0}, {0, 1}, {1, 1}});
     * m.matmul(n).rowView(0);                     // returns [4, 5] (1+3, 2+3)
     *
     * ByteMatrix bad = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}, {5, 6}});
     * a.matmul(bad);                             // throws IllegalArgumentException (this.columnCount != other.rowCount)
     * a.matmul((ByteMatrix) null);               // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * <p><b>Note:</b> This is the linear-algebra matrix product, not element-wise multiplication.
     * For element-wise multiplication use
     * {@link #zipWith(ByteMatrix, com.landawn.abacus.util.Throwables.ByteBinaryOperator)}.</p>
     *
     * @param other the matrix to multiply with; must have row count equal to this matrix's column count
     * @return a new ByteMatrix containing the matrix product with dimensions {@code (this.rowCount x other.columnCount)}
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.columnCount != other.rowCount} (incompatible dimensions for multiplication), or if this matrix has zero rows while {@code other} has a non-zero column count (the resulting shape is not representable)
     */
    public ByteMatrix matmul(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.rowCount,
                "Matrix dimensions incompatible for multiplication: this is {}x{}, other is {}x{} (this.columnCount must equal other.rowCount)", rowCount,
                columnCount, other.rowCount, other.columnCount);

        checkRepresentableShape(rowCount, other.columnCount);

        final byte[][] otherArray = other.a;
        final byte[][] result = new byte[rowCount][other.columnCount];
        final Throwables.IntTriConsumer<RuntimeException> multiplyAction = (i, j, k) -> result[i][j] += a[i][k] * otherArray[k][j];

        Matrices.forEachCartesianIndices(this, other, multiplyAction);

        return ByteMatrix.of(result);
    }

    /**
     * Converts this primitive byte matrix to a boxed Byte Matrix.
     * Each byte value is converted to its corresponding Byte wrapper object.
     *
     * <p>This conversion is useful when you need to work with APIs that require
     * object types rather than primitives, or when you need {@code null} values in the matrix.
     * Note that boxing incurs memory overhead and may impact performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix primitive = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * Matrix<Byte> boxed = primitive.boxed();
     * boxed.get(0, 0);                            // returns Byte 1
     * boxed.get(1, 1);                            // returns Byte 4
     * boxed.set(0, 0, null);                      // boxed matrix can hold null values
     * boxed.get(0, 0);                            // returns null
     *
     * ByteMatrix.empty().boxed().isEmpty();      // returns true
     * }</pre>
     *
     * @return a new Matrix&lt;Byte&gt; with the same dimensions and values as this matrix
     * @see #unbox(Matrix)
     */
    public Matrix<Byte> boxed() {
        final Byte[][] c = new Byte[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final byte[] aa = a[i];
                final Byte[] cc = c[i];

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
     * Converts this ByteMatrix to an IntMatrix by widening each byte value to int.
     * Each byte value is promoted to a 32-bit integer with sign extension.
     * This is a lossless conversion that preserves all values and their signs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix byteMatrix = ByteMatrix.of(new byte[][] {{1, -2}, {127, -128}});
     * IntMatrix intMatrix = byteMatrix.toIntMatrix();
     * intMatrix.get(0, 1);                        // returns -2 (sign preserved)
     * intMatrix.get(1, 1);                        // returns -128 (sign extension)
     * intMatrix.rowCount();                       // returns 2
     *
     * ByteMatrix.empty().toIntMatrix().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new IntMatrix with the same dimensions and values converted to int
     * @see #toLongMatrix()
     * @see #toFloatMatrix()
     * @see #toDoubleMatrix()
     * @see IntMatrix#from(byte[][])
     */
    public IntMatrix toIntMatrix() {
        return IntMatrix.from(a);
    }

    /**
     * Converts this ByteMatrix to a LongMatrix by widening each byte value to long.
     * Each byte value is promoted to a 64-bit long integer with sign extension.
     * This is a lossless conversion that preserves all values and their signs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix byteMatrix = ByteMatrix.of(new byte[][] {{1, -2}, {127, -128}});
     * LongMatrix longMatrix = byteMatrix.toLongMatrix();
     * longMatrix.get(0, 1);                       // returns -2L (sign preserved)
     * longMatrix.get(1, 1);                       // returns -128L (sign extension)
     * longMatrix.rowCount();                      // returns 2
     *
     * ByteMatrix.empty().toLongMatrix().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new LongMatrix with the same dimensions and values converted to long
     * @see #toIntMatrix()
     * @see #toFloatMatrix()
     * @see #toDoubleMatrix()
     */
    public LongMatrix toLongMatrix() {
        final long[][] c = new long[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final byte[] aa = a[i];
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
     * Converts this ByteMatrix to a FloatMatrix by converting each byte value to float.
     * Each byte value is converted to a 32-bit floating-point number.
     * This is a lossless conversion since all byte values can be exactly represented as floats.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix byteMatrix = ByteMatrix.of(new byte[][] {{1, -2}, {127, -128}});
     * FloatMatrix floatMatrix = byteMatrix.toFloatMatrix();
     * floatMatrix.get(0, 0);                      // returns 1.0f
     * floatMatrix.get(1, 1);                      // returns -128.0f
     * floatMatrix.rowCount();                     // returns 2
     *
     * ByteMatrix.empty().toFloatMatrix().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new FloatMatrix with the same dimensions and values converted to float
     * @see #toIntMatrix()
     * @see #toLongMatrix()
     * @see #toDoubleMatrix()
     */
    public FloatMatrix toFloatMatrix() {
        final float[][] c = new float[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final byte[] aa = a[i];
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
     * Converts this ByteMatrix to a DoubleMatrix by converting each byte value to double.
     * Each byte value is converted to a 64-bit double-precision floating-point number.
     * This is a lossless conversion since all byte values can be exactly represented as doubles.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix byteMatrix = ByteMatrix.of(new byte[][] {{1, -2}, {127, -128}});
     * DoubleMatrix doubleMatrix = byteMatrix.toDoubleMatrix();
     * doubleMatrix.get(0, 0);                     // returns 1.0
     * doubleMatrix.get(1, 1);                     // returns -128.0
     * doubleMatrix.rowCount();                    // returns 2
     *
     * ByteMatrix.empty().toDoubleMatrix().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new DoubleMatrix with the same dimensions and values converted to double
     * @see #toIntMatrix()
     * @see #toLongMatrix()
     * @see #toFloatMatrix()
     */
    public DoubleMatrix toDoubleMatrix() {
        final double[][] c = new double[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final byte[] aa = a[i];
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
     * Applies a binary operation element-wise to this matrix and another matrix of the same shape.
     * The operation is applied to corresponding elements from both matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix1 = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix matrix2 = ByteMatrix.of(new byte[][] {{5, 6}, {7, 8}});
     * ByteMatrix result = matrix1.zipWith(matrix2, (a, b) -> (byte) (a * b));
     * result.rowView(0);                          // returns [5, 12]
     * result.rowView(1);                          // returns [21, 32]
     *
     * // Element-wise max
     * matrix1.zipWith(matrix2, (a, b) -> (byte) Math.max(a, b)).rowView(0);   // returns [5, 6]
     *
     * ByteMatrix mismatch = ByteMatrix.of(new byte[][] {{1, 2, 3}});
     * matrix1.zipWith(mismatch, (a, b) -> a);    // throws IllegalArgumentException (different shapes)
     * matrix1.zipWith(matrix2, null);            // throws IllegalArgumentException (zipFunction is null)
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the operation
     * @param other the second matrix
     * @param zipFunction the binary operation to apply to corresponding elements
     * @return a new {@code ByteMatrix} containing the results
     * @throws IllegalArgumentException if {@code other} has a different shape than this matrix,
     *         or if {@code zipFunction} is {@code null}
     * @throws E if the zip function throws an exception
     */
    public <E extends Exception> ByteMatrix zipWith(final ByteMatrix other, final Throwables.ByteBinaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);
        N.checkArgNotNull(zipFunction, "zipFunction");

        final byte[][] b = other.a;
        final byte[][] result = new byte[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> cmd = (i, j) -> result[i][j] = zipFunction.applyAsByte(a[i][j], b[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return ByteMatrix.of(result);
    }

    /**
     * Applies a ternary operation element-wise to this matrix and two other matrices of the same shape.
     * The operation is applied to corresponding elements from all three matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix1 = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix matrix2 = ByteMatrix.of(new byte[][] {{5, 6}, {7, 8}});
     * ByteMatrix matrix3 = ByteMatrix.of(new byte[][] {{9, 10}, {11, 12}});
     * ByteMatrix result = matrix1.zipWith(matrix2, matrix3, (a, b, c) -> (byte) (a + b + c));
     * result.rowView(0);                          // returns [15, 18]
     * result.rowView(1);                          // returns [21, 24]
     *
     * // Pick the middle value of three
     * matrix1.zipWith(matrix2, matrix3, (a, b, c) -> b).rowView(0);   // returns [5, 6]
     *
     * ByteMatrix mismatch = ByteMatrix.of(new byte[][] {{1, 2, 3}});
     * matrix1.zipWith(matrix2, mismatch, (a, b, c) -> a);   // throws IllegalArgumentException (different shapes)
     * matrix1.zipWith(matrix2, matrix3, null);              // throws IllegalArgumentException (zipFunction is null)
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the operation
     * @param other the second matrix
     * @param third the third matrix
     * @param zipFunction the ternary operation to apply to corresponding elements
     * @return a new {@code ByteMatrix} containing the results
     * @throws IllegalArgumentException if {@code other} or {@code third} has a different shape than this matrix,
     *         or if {@code zipFunction} is {@code null}
     * @throws E if the zip function throws an exception
     */
    public <E extends Exception> ByteMatrix zipWith(final ByteMatrix other, final ByteMatrix third, final Throwables.ByteTernaryOperator<E> zipFunction)
            throws E {
        N.checkArgument(isSameShape(other) && isSameShape(third), "Cannot zip matrices with different shapes: all matrices must be {}x{}", rowCount,
                columnCount);
        N.checkArgNotNull(zipFunction, "zipFunction");

        final byte[][] b = other.a;
        final byte[][] c = third.a;
        final byte[][] result = new byte[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> cmd = (i, j) -> result[i][j] = zipFunction.applyAsByte(a[i][j], b[i][j], c[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return ByteMatrix.of(result);
    }

    /**
     * Returns a stream of elements on the main diagonal (upper-left to lower-right).
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.mainDiagonalStream().toArray();      // returns [1, 5, 9]
     * matrix.mainDiagonalStream().sum();          // returns 15L
     *
     * ByteMatrix.empty().mainDiagonalStream().count();   // returns 0L (empty allowed)
     *
     * ByteMatrix nonSquare = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.mainDiagonalStream();             // throws IllegalStateException (non-empty and not square)
     * }</pre>
     *
     * @return a ByteStream of diagonal elements
     * @throws IllegalStateException if the matrix is non-empty and not square (rowCount != columnCount)
     */
    @Override
    public ByteStream mainDiagonalStream() {
        if (isEmpty()) {
            return ByteStream.empty();
        }

        checkIsSquare();

        return ByteStream.of(new ByteIteratorEx() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public byte nextByte() {
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
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.antiDiagonalStream().toArray();      // returns [3, 5, 7]
     * matrix.antiDiagonalStream().sum();          // returns 15L
     *
     * ByteMatrix.empty().antiDiagonalStream().count();   // returns 0L (empty allowed)
     *
     * ByteMatrix nonSquare = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.antiDiagonalStream();             // throws IllegalStateException (non-empty and not square)
     * }</pre>
     *
     * @return a ByteStream of anti-diagonal elements
     * @throws IllegalStateException if the matrix is non-empty and not square (rowCount != columnCount)
     */
    @Override
    public ByteStream antiDiagonalStream() {
        if (isEmpty()) {
            return ByteStream.empty();
        }

        checkIsSquare();

        return ByteStream.of(new ByteIteratorEx() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public byte nextByte() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final byte result = a[cursor][columnCount - cursor - 1];
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
     * Elements are streamed row by row from left to right. This is the default
     * iteration order for most matrix operations.
     *
     * <p>The stream iterates through elements in the following order:
     * [0][0], [0][1], ..., [0][columnCount-1], [1][0], [1][1], ..., [rowCount-1][columnCount-1]
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.horizontalStream().toArray();        // returns [1, 2, 3, 4, 5, 6]
     * matrix.horizontalStream().count();          // returns 6L
     *
     * ByteMatrix.empty().horizontalStream().count();                    // returns 0L
     * ByteMatrix.of(new byte[][] {{7}}).horizontalStream().toArray();   // returns [7]
     * }</pre>
     *
     * @return a ByteStream of all matrix elements in row-major order
     * @see #verticalStream()
     * @see #rowStreams()
     */
    @Override
    public ByteStream horizontalStream() {
        return horizontalStream(0, rowCount);
    }

    /**
     * Returns a stream of elements from a specific row.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.horizontalStream(1).toArray();       // returns [4, 5, 6]
     * matrix.horizontalStream(0).toArray();       // returns [1, 2, 3]
     *
     * matrix.horizontalStream(5);                 // throws IndexOutOfBoundsException (row out of bounds)
     * matrix.horizontalStream(-1);                // throws IndexOutOfBoundsException (negative index)
     * }</pre>
     *
     * @param rowIndex the index of the row to stream (0-based)
     * @return a ByteStream of elements from the specified row
     * @throws IndexOutOfBoundsException if rowIndex is out of bounds
     */
    @Override
    public ByteStream horizontalStream(final int rowIndex) {
        return horizontalStream(rowIndex, rowIndex + 1);
    }

    /**
     * Returns a stream of elements from a range of rows in row-major order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.horizontalStream(0, 2).toArray();    // returns [1, 2, 3, 4]
     * matrix.horizontalStream(1, 3).toArray();    // returns [3, 4, 5, 6]
     * matrix.horizontalStream(1, 1).count();      // returns 0L (empty range)
     *
     * matrix.horizontalStream(0, 5);              // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a ByteStream of elements from the specified rows
     * @throws IndexOutOfBoundsException if the indices are out of bounds
     */
    @Override
    public ByteStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        if (isEmpty()) {
            return ByteStream.empty();
        }

        return ByteStream.of(new ByteIteratorEx() {
            private int i = fromRowIndex;
            private int j = 0;

            @Override
            public boolean hasNext() {
                return i < toRowIndex;
            }

            @Override
            public byte nextByte() {
                if (i >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final byte result = a[i][j++];

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
            public byte[] toArray() {
                final int len = toArrayLength(count());
                final byte[] c = new byte[len];

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
     * Returns a stream of all elements in column-major order (vertically).
     * Elements are streamed column by column from top to bottom.
     *
     * <p>The order of elements is:
     * [0][0], [1][0], ..., [rowCount-1][0], [0][1], [1][1], ..., [rowCount-1][columnCount-1]
     * </p>
     *
     * <p><b>Note:</b> This method is marked as @Beta and may change in future versions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.verticalStream().toArray();          // returns [1, 4, 2, 5, 3, 6]
     * matrix.verticalStream().count();            // returns 6L
     *
     * ByteMatrix.empty().verticalStream().count();                    // returns 0L
     * ByteMatrix.of(new byte[][] {{7}}).verticalStream().toArray();   // returns [7]
     * }</pre>
     *
     * @return a ByteStream of all matrix elements in column-major order
     * @see #horizontalStream()
     * @see #columnStreams()
     */
    @Override
    @Beta
    public ByteStream verticalStream() {
        return verticalStream(0, columnCount);
    }

    /**
     * Returns a stream of elements from a specific column.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.verticalStream(1).toArray();         // returns [2, 5]
     * matrix.verticalStream(0).toArray();         // returns [1, 4]
     *
     * matrix.verticalStream(5);                   // throws IndexOutOfBoundsException (column out of bounds)
     * matrix.verticalStream(-1);                  // throws IndexOutOfBoundsException (negative index)
     * }</pre>
     *
     * @param columnIndex the index of the column to stream (0-based)
     * @return a ByteStream of elements from the specified column
     * @throws IndexOutOfBoundsException if columnIndex is out of bounds
     */
    @Override
    public ByteStream verticalStream(final int columnIndex) {
        return verticalStream(columnIndex, columnIndex + 1);
    }

    /**
     * Returns a stream of elements from a range of columns in column-major order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.verticalStream(1, 3).toArray();      // returns [2, 5, 3, 6]
     * matrix.verticalStream(0, 1).toArray();      // returns [1, 4]
     * matrix.verticalStream(1, 1).count();        // returns 0L (empty range)
     *
     * matrix.verticalStream(0, 5);                // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a ByteStream of elements from the specified columns
     * @throws IndexOutOfBoundsException if the indices are out of bounds
     */
    @Override
    @Beta
    public ByteStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        if (isEmpty()) {
            return ByteStream.empty();
        }

        return ByteStream.of(new ByteIteratorEx() {
            private int i = 0;
            private int j = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return j < toColumnIndex;
            }

            @Override
            public byte nextByte() {
                if (j >= toColumnIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final byte result = a[i++][j];

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

                if (n >= (long) (toColumnIndex - j) * ByteMatrix.this.rowCount - i) {
                    i = 0;
                    j = toColumnIndex;
                } else {
                    final long offset = n + i;
                    i = (int) (offset % ByteMatrix.this.rowCount);
                    j += (int) (offset / ByteMatrix.this.rowCount);
                }
            }

            @Override
            public long count() {
                return (long) (toColumnIndex - j) * rowCount - i; // NOSONAR
            }

            @Override
            public byte[] toArray() {
                final int len = toArrayLength(count());
                final byte[] c = new byte[len];

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
     * Returns a stream where each element is a ByteStream representing a row of the matrix.
     * This provides a convenient way to process the matrix row by row.
     *
     * <p>Each ByteStream in the returned Stream represents one row of the matrix.
     * This is useful for row-wise operations or when you need to apply stream operations
     * to individual rows.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.rowStreams().count();                                    // returns 2L (one stream per row)
     * matrix.rowStreams().map(ByteStream::toArray).toList().get(0);   // returns [1, 2, 3]
     * matrix.rowStreams().map(ByteStream::toArray).toList().get(1);   // returns [4, 5, 6]
     *
     * ByteMatrix.empty().rowStreams().count();   // returns 0L
     * }</pre>
     *
     * @return a Stream of ByteStream, one for each row in the matrix
     * @see #columnStreams()
     * @see #horizontalStream()
     */
    @Override
    public Stream<ByteStream> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Returns a stream where each element is a ByteStream representing a row from the specified range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.rowStreams(1, 3).count();                                    // returns 2L
     * matrix.rowStreams(1, 3).map(ByteStream::toArray).toList().get(0);   // returns [3, 4]
     * matrix.rowStreams(1, 3).map(ByteStream::toArray).toList().get(1);   // returns [5, 6]
     * matrix.rowStreams(1, 1).count();                                    // returns 0L (empty range)
     *
     * matrix.rowStreams(0, 5);                    // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of ByteStream, one for each row in the range
     * @throws IndexOutOfBoundsException if the indices are out of bounds
     */
    @Override
    public Stream<ByteStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public ByteStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return ByteStream.of(a[cursor++]);
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
     * Returns a stream where each element is a ByteStream representing a column of the matrix.
     * This provides a convenient way to process the matrix column by column.
     *
     * <p>Each ByteStream in the returned Stream represents one column of the matrix.
     * This is useful for column-wise operations or when you need to apply stream operations
     * to individual columns.
     *
     * <p><b>Note:</b> This method is marked as @Beta and may change in future versions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.columnStreams().count();                                    // returns 3L (one stream per column)
     * matrix.columnStreams().map(ByteStream::toArray).toList().get(0);   // returns [1, 4]
     * matrix.columnStreams().map(ByteStream::toArray).toList().get(2);   // returns [3, 6]
     *
     * ByteMatrix.empty().columnStreams().count();   // returns 0L
     * }</pre>
     *
     * @return a Stream of ByteStream, one for each column in the matrix
     * @see #rowStreams()
     * @see #verticalStream()
     */
    @Override
    @Beta
    public Stream<ByteStream> columnStreams() {
        return columnStreams(0, columnCount);
    }

    /**
     * Returns a stream where each element is a ByteStream representing a column from the specified range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.columnStreams(1, 3).count();                                    // returns 2L
     * matrix.columnStreams(1, 3).map(ByteStream::toArray).toList().get(0);   // returns [2, 5]
     * matrix.columnStreams(1, 3).map(ByteStream::toArray).toList().get(1);   // returns [3, 6]
     * matrix.columnStreams(1, 1).count();                                    // returns 0L (empty range)
     *
     * matrix.columnStreams(0, 5);                 // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of ByteStream, one for each column in the range
     * @throws IndexOutOfBoundsException if the indices are out of bounds
     */
    @Override
    @Beta
    public Stream<ByteStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
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
            public ByteStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return ByteStream.of(new ByteIteratorEx() {
                    private final int columnIndex = cursor++;
                    private final int toIndex2 = rowCount;
                    private int cursor2 = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor2 < toIndex2;
                    }

                    @Override
                    public byte nextByte() {
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
     * Returns the length of the specified byte array, or 0 if the array is {@code null}.
     * This is an internal helper method used by the abstract parent class for
     * operations that need to determine array lengths safely.
     *
     * <p>This method is {@code protected} and primarily used internally by the
     * matrix implementation and should not typically be called by external code.
     *
     * @param a the byte array to measure
     * @return the length of the array, or 0 if the array is {@code null}
     */
    @Override
    protected int length(@SuppressWarnings("hiding") final byte[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Performs the specified action for each element in this matrix.
     * When sequential, elements are processed in row-major order (left to right, top to bottom).
     * This operation may be performed in parallel for large matrices, in which case the order
     * of element visits is unspecified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * int[] sum = {0};
     * matrix.forEach(value -> sum[0] += value);
     * sum[0];                                     // returns 10 (1 + 2 + 3 + 4)
     *
     * ByteList collected = new ByteList();
     * matrix.forEach(collected::add);
     * collected.size();                          // returns 4
     *
     * int[] count = {0};
     * ByteMatrix.empty().forEach(value -> count[0]++);
     * count[0];                                  // returns 0 (no elements)
     *
     * matrix.forEach((Throwables.ByteConsumer<RuntimeException>) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the action
     * @param action the consumer to apply to each element; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final Throwables.ByteConsumer<E> action) throws E {
        forEach(0, rowCount, 0, columnCount, action);
    }

    /**
     * Performs the specified action for each element in a rectangular sub-region of this matrix.
     * When sequential, elements are processed in row-major order (left to right, top to bottom)
     * within the specified bounds. This operation may be performed in parallel for large regions,
     * in which case the order of element visits is unspecified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * int[] sum = {0};
     * matrix.forEach(1, 3, 1, 3, value -> sum[0] += value);
     * sum[0];                                     // returns 28 (5 + 6 + 8 + 9)
     *
     * ByteList collected = new ByteList();
     * matrix.forEach(0, 1, 0, 3, collected::add);
     * collected.size();                          // returns 3 (first row only)
     *
     * matrix.forEach(0, 5, 0, 3, value -> {});                                        // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.forEach(0, 1, 0, 1, (Throwables.ByteConsumer<RuntimeException>) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown by the action
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @param action the consumer to apply to each element in the region; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws IndexOutOfBoundsException if any of the from/to indices are out of bounds or {@code fromIndex > toIndex}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex,
            final Throwables.ByteConsumer<E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        if (Matrices.isParallelizable(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> cmd = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, cmd, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                final byte[] aa = a[i];

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
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.println();                           // returns "[1, 2, 3]\n[4, 5, 6]" (also printed)
     *
     * ByteMatrix single = ByteMatrix.of(new byte[][] {{7}});
     * single.println();                           // returns "[7]"
     *
     * ByteMatrix.empty().println();              // returns "[]"
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

                    final byte[] row = a[i];
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
     * Returns a hash code value for this matrix based on its contents.
     * The hash code is computed using a deep hash of the internal two-dimensional array,
     * ensuring that matrices with identical dimensions and element values
     * produce the same hash code.
     *
     * <p>This implementation is consistent with the {@link #equals(Object)} method:
     * if two matrices are equal according to {@code equals()}, they will have the same hash code.
     *
     * <p><b>Note:</b> The hash code computation examines all elements in the matrix,
     * so it may be expensive for large matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix m1 = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix m2 = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix m3 = ByteMatrix.of(new byte[][] {{1, 2}, {3, 5}});
     * m1.hashCode() == m2.hashCode();                                   // returns true (equal matrices share a hash code)
     * m1.hashCode() == m3.hashCode();                                   // returns false (different contents, very likely differ)
     * ByteMatrix.empty().hashCode() == ByteMatrix.empty().hashCode();   // returns true
     * }</pre>
     *
     * @return a hash code value for this matrix based on its contents
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        return N.deepHashCode(a);
    }

    /**
     * Compares this matrix to the specified object for equality.
     * Two ByteMatrix objects are considered equal if and only if:
     * <ul>
     * <li>They have the same number of rows</li>
     * <li>They have the same number of columns</li>
     * <li>All corresponding elements are equal</li>
     * </ul>
     *
     * <p>This method performs a deep comparison of all matrix elements.
     * For large matrices, this operation may be expensive.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix m1 = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix m2 = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix m3 = ByteMatrix.of(new byte[][] {{1, 2}, {3, 5}});
     *
     * m1.equals(m2);         // returns true (same dimensions and values)
     * m1.equals(m3);         // returns false (different values)
     * m1.equals(null);       // returns false (null is not equal)
     * m1.equals("string");   // returns false (different type)
     * }</pre>
     *
     * @param obj the object to compare with this matrix
     * @return {@code true} if the objects are equal ByteMatrix instances with identical contents, {@code false} otherwise
     * @see #hashCode()
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final ByteMatrix another) {
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
     * with each row on a separate line, use {@link #println()} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.of(new byte[][] {{1, 2}, {3, 4}});
     * matrix.toString();                          // returns "[[1, 2], [3, 4]]"
     *
     * ByteMatrix single = ByteMatrix.of(new byte[][] {{7}});
     * single.toString();                          // returns "[[7]]"
     *
     * ByteMatrix.empty().toString();             // returns "[]"
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
