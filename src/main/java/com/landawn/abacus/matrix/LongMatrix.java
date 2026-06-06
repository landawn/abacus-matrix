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
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.LongIteratorEx;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * Matrix implementation backed by a rectangular {@code long[][]}.
 *
 * <p>This type specializes {@link AbstractMatrix} for {@code long} values while keeping the data in a
 * validated backing array. The constructor and {@link #of(long[]...)} generally wrap the supplied storage
 * directly, while factories, conversions, and mapping operations allocate new arrays.</p>
 *
 * <p>Cells introduced by growth, reshaping, or padding default to {@code 0L} unless an overload accepts
 * an explicit fill value. Arithmetic operations on {@code long} values use Java's standard two's-complement
 * wrap-around semantics on overflow. Narrowing conversions
 * (for example {@link #toIntMatrix()}) may discard high-order bits.</p>
 *
 * @see IntMatrix
 * @see DoubleMatrix
 * @see FloatMatrix
 */
public final class LongMatrix extends AbstractMatrix<long[], LongList, LongStream, Stream<LongStream>, LongMatrix> {

    private static final LongMatrix EMPTY_LONG_MATRIX = new LongMatrix(new long[0][0]);

    /**
     * Constructs a {@code LongMatrix} backed by the supplied two-dimensional array.
     *
     * <p>If {@code a} is {@code null}, this creates an empty {@code 0x0} matrix. Otherwise the array
     * is used directly after rectangular-shape validation, so later modifications to either the input
     * array or the matrix remain visible through the other view. Call {@link #copy()} if you need an
     * independently owned matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[][] data = {{1L, 2L}, {3L, 4L}};
     * LongMatrix matrix = new LongMatrix(data);
     * matrix.get(0, 0);                         // returns 1L
     * data[0][0] = 99L;                         // mutates the shared backing array
     * matrix.get(0, 0);                         // returns 99L (the matrix sees the change)
     *
     * new LongMatrix((long[][]) null).rowCount();    // returns 0 (null becomes an empty 0x0 matrix)
     * new LongMatrix(new long[0][0]).columnCount();  // returns 0
     * new LongMatrix(new long[][] {{1L}, {2L, 3L}}); // throws IllegalArgumentException (not rectangular)
     * }</pre>
     *
     * @param a the two-dimensional long array to wrap, or {@code null} for an empty matrix
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public LongMatrix(final long[][] a) {
        super(a == null ? new long[0][0] : a, long.class);
    }

    /**
     * Returns a shared empty matrix with zero rows and zero columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.empty();
     * matrix.rowCount();                          // returns 0
     * matrix.columnCount();                       // returns 0
     * matrix.isEmpty();                           // returns true
     * LongMatrix.empty() == LongMatrix.empty();   // true (same shared singleton)
     * }</pre>
     *
     * @return a shared empty {@code LongMatrix} singleton
     */
    public static LongMatrix empty() {
        return EMPTY_LONG_MATRIX;
    }

    /**
     * Creates a {@code LongMatrix} from a two-dimensional long array.
     *
     * <p><b>Important:</b> The provided array is used directly without defensive copying.
     * Changes to the input array are reflected in the returned matrix, and vice versa.
     * The array is validated to be rectangular by the constructor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.get(1, 1);                       // returns 4L
     * matrix.rowCount();                      // returns 2
     *
     * LongMatrix.of().isEmpty();                    // returns true (no rows supplied)
     * LongMatrix.of((long[][]) null).isEmpty();     // returns true
     * LongMatrix.of(new long[][] {{1L}, {2L, 3L}}); // throws IllegalArgumentException (not rectangular)
     * }</pre>
     *
     * @param a the two-dimensional long array to create the matrix from, or {@code null}/empty for an empty matrix
     * @return a new {@code LongMatrix} wrapping the provided data, or the shared empty matrix if input is {@code null} or empty
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public static LongMatrix of(final long[]... a) {
        return N.isEmpty(a) ? EMPTY_LONG_MATRIX : new LongMatrix(a);
    }

    /**
     * Creates a {@code LongMatrix} from a two-dimensional int array by widening each {@code int} to {@code long}.
     * The widening conversion preserves the exact numeric value (no data loss).
     *
     * <p>All rows must have the same length as the first row (rectangular array required).
     * The method validates array structure and throws an exception if the array is jagged (rows of different lengths).
     * The result is a freshly allocated matrix; subsequent modifications to {@code a} do not affect it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.from(new int[][] {{1, 2}, {3, 4}});
     * matrix.get(0, 0);                       // returns 1L (int 1 widened to long)
     * matrix.get(1, 1);                       // returns 4L
     *
     * LongMatrix.from((int[][]) null).isEmpty();   // returns true
     * LongMatrix.from(new int[0][0]).isEmpty();    // returns true
     * LongMatrix.from(new int[][] {{1}, {2, 3}});  // throws IllegalArgumentException (not rectangular)
     * }</pre>
     *
     * @param a the two-dimensional {@code int} array to convert to a long matrix, or {@code null}/empty for an empty matrix
     * @return a new {@code LongMatrix} with widened values, or the shared empty matrix if input is {@code null} or empty
     * @throws IllegalArgumentException if {@code a} is non-empty and its first row is {@code null},
     *         or if any subsequent row is {@code null} or has a length different from the first row
     *         (non-rectangular array)
     */
    public static LongMatrix from(final int[]... a) {
        if (N.isEmpty(a)) {
            return EMPTY_LONG_MATRIX;
        }

        N.checkArgument(a[0] != null, "First row cannot be null");

        final int columnCount = a[0].length;

        // Validate all rows have the same length
        for (int i = 1; i < a.length; i++) {
            N.checkArgument(a[i] != null && a[i].length == columnCount, "All rows must have the same length. Row 0 has length {} but row {} has length {}",
                    columnCount, i, a[i] == null ? 0 : a[i].length);
        }

        final long[][] result = new long[a.length][columnCount];

        for (int i = 0, rowCount = a.length; i < rowCount; i++) {
            final int[] sourceRow = a[i];
            final long[] targetRow = result[i];

            for (int j = 0; j < columnCount; j++) {
                targetRow[j] = sourceRow[j]; // NOSONAR
            }
        }

        return new LongMatrix(result);
    }

    /**
     * Creates a new {@code 1 x length} matrix filled with random {@code long} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.random(5);
     * matrix.rowCount();            // returns 1
     * matrix.columnCount();         // returns 5 (values are random)
     *
     * LongMatrix.random(0).columnCount();   // returns 0 (1x0 matrix)
     * LongMatrix.random(-1);                // throws IllegalArgumentException
     * }</pre>
     *
     * @param length the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code LongMatrix} of dimensions {@code 1 x length} filled with random {@code long} values
     * @throws IllegalArgumentException if {@code length} is negative
     */
    public static LongMatrix random(final int length) {
        return random(1, length);
    }

    /**
     * Creates a new matrix of the specified dimensions filled with random {@code long} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.random(2, 3);
     * matrix.rowCount();            // returns 2
     * matrix.columnCount();         // returns 3 (values are random)
     *
     * LongMatrix.random(0, 0).isEmpty();   // returns true
     * LongMatrix.random(-1, 3);            // throws IllegalArgumentException
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code LongMatrix} of dimensions {@code rowCount x columnCount} filled with random {@code long} values
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if the resulting shape is not representable
     */
    public static LongMatrix random(final int rowCount, final int columnCount) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final long[][] a = new long[rowCount][columnCount];

        for (long[] ea : a) {
            for (int i = 0; i < columnCount; i++) {
                ea[i] = RAND.nextLong();
            }
        }

        return new LongMatrix(a);
    }

    /**
     * Creates a new matrix of the specified dimensions where every element is the provided {@code element}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.repeat(2, 3, 1L);
     * matrix.get(0, 0);             // returns 1L
     * matrix.flatten().toArray();   // returns [1L, 1L, 1L, 1L, 1L, 1L]
     *
     * LongMatrix.repeat(0, 0, 7L).isEmpty();    // returns true
     * LongMatrix.repeat(0, 5, 7L);              // throws IllegalArgumentException (0 rows require 0 columns)
     * LongMatrix.repeat(-1, 3, 0L);             // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @param element the {@code long} value to fill the matrix with
     * @return a new {@code LongMatrix} of dimensions {@code rowCount x columnCount} filled with the specified element
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if the resulting shape is not representable
     */
    public static LongMatrix repeat(final int rowCount, final int columnCount, final long element) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final long[][] a = new long[rowCount][columnCount];

        for (long[] ea : a) {
            N.fill(ea, element);
        }

        return new LongMatrix(a);
    }

    /**
     * Creates a single-row {@code LongMatrix} with sequential values from {@code startInclusive} to {@code endExclusive}.
     * The values are generated with a step of 1. If {@code startInclusive >= endExclusive}, a 1×0 matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix.range(0L, 5L).flatten().toArray();   // returns [0L, 1L, 2L, 3L, 4L]
     * LongMatrix.range(0L, 5L).columnCount();         // returns 5
     *
     * LongMatrix.range(5L, 0L).columnCount();         // returns 0 (1x0 matrix, start >= end)
     * LongMatrix.range(3L, 3L).columnCount();         // returns 0 (empty range)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new {@code 1×n} {@code LongMatrix} where {@code n = max(0, endExclusive - startInclusive)}
     */
    public static LongMatrix range(final long startInclusive, final long endExclusive) {
        return new LongMatrix(new long[][] { Array.range(startInclusive, endExclusive) });
    }

    /**
     * Creates a single-row {@code LongMatrix} with values from {@code startInclusive} to {@code endExclusive}
     * with the specified step.
     * The step size can be positive (for ascending sequences) or negative (for descending sequences).
     * If the step does not move from {@code startInclusive} toward {@code endExclusive}, a 1×0 matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix.range(0L, 10L, 2L).flatten().toArray();   // returns [0L, 2L, 4L, 6L, 8L]
     * LongMatrix.range(10L, 0L, -2L).flatten().toArray();  // returns [10L, 8L, 6L, 4L, 2L]
     *
     * LongMatrix.range(0L, 10L, -1L).columnCount();        // returns 0 (step is wrong direction)
     * LongMatrix.range(0L, 10L, 0L);                       // throws IllegalArgumentException (step is zero)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param step the step size (must not be zero; can be positive or negative)
     * @return a new {@code 1×n} {@code LongMatrix} with values incremented by the step size
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static LongMatrix range(final long startInclusive, final long endExclusive, final long step) {
        return new LongMatrix(new long[][] { Array.range(startInclusive, endExclusive, step) });
    }

    /**
     * Creates a single-row {@code LongMatrix} with sequential values from {@code startInclusive} to {@code endInclusive}.
     * This method includes the end value, unlike {@link #range(long, long)}.
     * If {@code startInclusive > endInclusive}, a 1×0 matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix.rangeClosed(0L, 4L).flatten().toArray();   // returns [0L, 1L, 2L, 3L, 4L]
     * LongMatrix.rangeClosed(5L, 5L).flatten().toArray();   // returns [5L] (end is included)
     *
     * LongMatrix.rangeClosed(5L, 0L).columnCount();         // returns 0 (1x0 matrix, start > end)
     * LongMatrix.rangeClosed(0L, 4L).rowCount();            // returns 1
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new {@code 1×n} {@code LongMatrix} where {@code n = max(0, endInclusive - startInclusive + 1)},
     *         or a {@code 1×0} matrix if {@code startInclusive > endInclusive}
     */
    public static LongMatrix rangeClosed(final long startInclusive, final long endInclusive) {
        return new LongMatrix(new long[][] { Array.rangeClosed(startInclusive, endInclusive) });
    }

    /**
     * Creates a single-row {@code LongMatrix} with values from {@code startInclusive} to {@code endInclusive}
     * with the specified step.
     * The step size can be positive (for ascending sequences) or negative (for descending sequences).
     * The end value is included only if it is reachable by stepping from start. If the step does not move
     * from {@code startInclusive} toward {@code endInclusive}, a 1×0 matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix.rangeClosed(0L, 8L, 2L).flatten().toArray();    // returns [0L, 2L, 4L, 6L, 8L]
     * LongMatrix.rangeClosed(0L, 9L, 2L).flatten().toArray();    // returns [0L, 2L, 4L, 6L, 8L] (9 not reachable)
     *
     * LongMatrix.rangeClosed(10L, 0L, -2L).flatten().toArray();  // returns [10L, 8L, 6L, 4L, 2L, 0L]
     * LongMatrix.rangeClosed(0L, 8L, 0L);                        // throws IllegalArgumentException (step is zero)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive, if reachable by stepping)
     * @param step the step size (must not be zero; can be positive or negative)
     * @return a new {@code 1×n} {@code LongMatrix} with values incremented by the step size
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static LongMatrix rangeClosed(final long startInclusive, final long endInclusive, final long step) {
        return new LongMatrix(new long[][] { Array.rangeClosed(startInclusive, endInclusive, step) });
    }

    /**
     * Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
     * All other elements (off-diagonal) are set to zero. The matrix size is n×n where n is the length
     * of the diagonal array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.mainDiagonal(new long[] {1L, 2L, 3L});
     * // Creates a 3x3 matrix:
     * // [[1, 0, 0],
     * //  [0, 2, 0],
     * //  [0, 0, 3]]
     * matrix.get(0, 0);                              // returns 1L
     * matrix.get(0, 1);                              // returns 0L (off-diagonal)
     * matrix.rowCount();                             // returns 3
     *
     * LongMatrix.mainDiagonal((long[]) null).isEmpty();   // returns true
     * LongMatrix.mainDiagonal(new long[0]).isEmpty();     // returns true
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements (from upper-left to lower-right);
     *        if {@code null} or empty, an empty matrix is returned
     * @return a square {@code n×n} matrix with the specified main diagonal, where {@code n} is the array length,
     *         or the empty matrix if the input is {@code null} or empty
     */
    public static LongMatrix mainDiagonal(final long[] mainDiagonal) {
        return diagonals(mainDiagonal, null);
    }

    /**
     * Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
     * All other elements (off-diagonal) are set to zero. The matrix size is n×n where n is the length
     * of the diagonal array. The anti-diagonal runs from upper-right to lower-left.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.antiDiagonal(new long[] {1L, 2L, 3L});
     * // Creates a 3x3 matrix:
     * // [[0, 0, 1],
     * //  [0, 2, 0],
     * //  [3, 0, 0]]
     * matrix.get(0, 2);                              // returns 1L
     * matrix.get(2, 0);                              // returns 3L
     * matrix.get(0, 0);                              // returns 0L (off anti-diagonal)
     *
     * LongMatrix.antiDiagonal((long[]) null).isEmpty();   // returns true
     * LongMatrix.antiDiagonal(new long[0]).isEmpty();     // returns true
     * }</pre>
     *
     * @param antiDiagonal the array of anti-diagonal elements (from upper-right to lower-left);
     *        if {@code null} or empty, an empty matrix is returned
     * @return a square {@code n×n} matrix with the specified anti-diagonal, where {@code n} is the array length,
     *         or the empty matrix if the input is {@code null} or empty
     */
    public static LongMatrix antiDiagonal(final long[] antiDiagonal) {
        return diagonals(null, antiDiagonal);
    }

    /**
     * Creates a square matrix from the specified main diagonal and anti-diagonal elements.
     * All other elements are set to zero. If both arrays are non-empty, they must have the same length.
     * The resulting matrix has dimensions {@code n×n} where {@code n} is the length of the non-empty diagonal array.
     * When both diagonals are provided and they overlap (at the center element of odd-sized matrices),
     * the main diagonal value takes precedence (the main diagonal is written after the anti-diagonal).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.diagonals(new long[] {1L, 2L, 3L}, new long[] {4L, 5L, 6L});
     * // Resulting 3x3 matrix:
     * //   {1, 0, 4},
     * //   {0, 2, 0},
     * //   {6, 0, 3}
     * matrix.get(0, 0);                              // returns 1L (main diagonal)
     * matrix.get(0, 2);                              // returns 4L (anti-diagonal)
     * matrix.get(1, 1);                              // returns 2L (center: main wins over anti)
     *
     * LongMatrix.diagonals((long[]) null, (long[]) null).isEmpty();       // returns true
     * LongMatrix.diagonals(new long[] {1L, 2L}, new long[] {1L, 2L, 3L}); // throws IllegalArgumentException (length mismatch)
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements (can be {@code null} or empty)
     * @param antiDiagonal the array of anti-diagonal elements (can be {@code null} or empty)
     * @return a square matrix with the specified diagonals, or an empty matrix if both inputs are {@code null} or empty
     * @throws IllegalArgumentException if both arrays are non-empty and have different lengths
     */
    public static LongMatrix diagonals(final long[] mainDiagonal, final long[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(N.isEmpty(mainDiagonal) || N.isEmpty(antiDiagonal) || mainDiagonal.length == antiDiagonal.length,
                "The length of 'mainDiagonal' and 'antiDiagonal' must be same");

        if (N.isEmpty(mainDiagonal) && N.isEmpty(antiDiagonal)) {
            return EMPTY_LONG_MATRIX;
        }

        final int len = N.max(N.len(mainDiagonal), N.len(antiDiagonal));
        final long[][] result = new long[len][len];

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

        return new LongMatrix(result);
    }

    /**
     * Converts a boxed {@code Matrix<Long>} to a primitive {@code LongMatrix}.
     * This method unboxes all {@code Long} wrapper objects to primitive {@code long} values for more efficient
     * storage and operations. This is particularly beneficial when working with large matrices, as primitive
     * arrays have less memory overhead and better cache locality than arrays of wrapper objects.
     * {@code null} elements in the input matrix are converted to {@code 0L}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Long> boxedMatrix = Matrix.of(new Long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix primitiveMatrix = LongMatrix.unbox(boxedMatrix);
     * primitiveMatrix.get(0, 0);                     // returns 1L
     * primitiveMatrix.get(1, 1);                     // returns 4L
     *
     * Matrix<Long> withNull = Matrix.of(new Long[][] {{null, 2L}});
     * LongMatrix.unbox(withNull).get(0, 0);          // returns 0L (null becomes 0L)
     * LongMatrix.unbox((Matrix<Long>) null);         // throws NullPointerException
     * }</pre>
     *
     * @param x the boxed {@code Matrix<Long>} to convert; must not be {@code null}
     * @return a new {@code LongMatrix} with unboxed primitive values
     * @throws NullPointerException if {@code x} is {@code null}
     * @see #boxed()
     */
    public static LongMatrix unbox(final Matrix<Long> x) {
        return LongMatrix.of(Array.unbox(x.a));
    }

    /**
     * Returns the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.get(0, 1);     // returns 2L
     * matrix.get(1, 0);     // returns 3L
     *
     * matrix.get(5, 0);     // throws ArrayIndexOutOfBoundsException (row out of range)
     * matrix.get(0, 9);     // throws ArrayIndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return the element at position {@code (rowIndex, columnIndex)}
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public long get(final int rowIndex, final int columnIndex) {
        return a[rowIndex][columnIndex];
    }

    /**
     * Returns the element at the specified point.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.get(Point.of(0, 1));     // returns 2L
     * matrix.get(Point.of(1, 1));     // returns 4L
     *
     * matrix.get((Point) null);       // throws IllegalArgumentException
     * matrix.get(Point.of(5, 0));     // throws ArrayIndexOutOfBoundsException
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @return the long element at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #get(int, int)
     */
    public long get(final Point point) {
        N.checkArgNotNull(point, "point");

        return a[point.rowIndex()][point.columnIndex()];
    }

    /**
     * Sets the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.set(0, 1, 9L);
     * matrix.get(0, 1);            // returns 9L (element updated in place)
     * matrix.set(1, 0, 7L);
     * matrix.get(1, 0);            // returns 7L
     *
     * matrix.set(5, 0, 1L);        // throws ArrayIndexOutOfBoundsException (row out of range)
     * matrix.set(0, 9, 1L);        // throws ArrayIndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @param value the value to set
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public void set(final int rowIndex, final int columnIndex, final long value) {
        a[rowIndex][columnIndex] = value;
    }

    /**
     * Sets the element at the specified point to the given value.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.set(Point.of(0, 1), 9L);
     * matrix.get(0, 1);                  // returns 9L (element updated in place)
     * matrix.set(Point.of(1, 0), 7L);
     * matrix.get(1, 0);                  // returns 7L
     *
     * matrix.set((Point) null, 1L);      // throws IllegalArgumentException
     * matrix.set(Point.of(5, 0), 1L);    // throws ArrayIndexOutOfBoundsException
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @param value the new long value to set at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #set(int, int, long)
     */
    public void set(final Point point, final long value) {
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.valueAbove(1, 0).getAsLong();   // returns 1L
     * matrix.valueAbove(1, 1).getAsLong();   // returns 2L
     *
     * matrix.valueAbove(0, 0).isPresent();   // returns false (no row above)
     * matrix.valueAbove(5, 0);               // throws ArrayIndexOutOfBoundsException
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalLong containing the element at position (rowIndex - 1, columnIndex), or empty if rowIndex == 0
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalLong valueAbove(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? OptionalLong.empty() : OptionalLong.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, if it exists.
     * This method provides safe access to the element directly below the given position
     * without throwing an exception when at the bottom edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.valueBelow(0, 0).getAsLong();   // returns 3L
     * matrix.valueBelow(0, 1).getAsLong();   // returns 4L
     *
     * matrix.valueBelow(1, 0).isPresent();   // returns false (no row below)
     * matrix.valueBelow(5, 0);               // throws ArrayIndexOutOfBoundsException
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalLong containing the element at position (rowIndex + 1, columnIndex), or empty if rowIndex == rowCount - 1
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalLong valueBelow(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? OptionalLong.empty() : OptionalLong.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, if it exists.
     * This method provides safe access to the element directly to the left of the given position
     * without throwing an exception when at the leftmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.valueLeft(0, 1).getAsLong();    // returns 1L
     * matrix.valueLeft(1, 1).getAsLong();    // returns 3L
     *
     * matrix.valueLeft(0, 0).isPresent();    // returns false (no column to the left)
     * matrix.valueLeft(0, 9);                // throws ArrayIndexOutOfBoundsException
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalLong containing the element at position (rowIndex, columnIndex - 1), or empty if columnIndex == 0
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalLong valueLeft(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? OptionalLong.empty() : OptionalLong.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, if it exists.
     * This method provides safe access to the element directly to the right of the given position
     * without throwing an exception when at the rightmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.valueRight(0, 0).getAsLong();   // returns 2L
     * matrix.valueRight(1, 0).getAsLong();   // returns 4L
     *
     * matrix.valueRight(0, 1).isPresent();   // returns false (no column to the right)
     * matrix.valueRight(0, 9);               // throws ArrayIndexOutOfBoundsException
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalLong containing the element at position (rowIndex, columnIndex + 1), or empty if columnIndex == columnCount - 1
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalLong valueRight(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? OptionalLong.empty() : OptionalLong.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as a long array.
     *
     * <p><b>Note:</b> This method returns a reference to the internal array, not a copy.
     * Modifications to the returned array will affect the matrix. Use {@link #rowCopy(int)}
     * if you need an independent copy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.rowView(0);          // returns [1L, 2L, 3L]
     * matrix.rowView(1);          // returns [4L, 5L, 6L]
     *
     * long[] firstRow = matrix.rowView(0);
     * firstRow[0] = 99L;          // mutates the shared backing array
     * matrix.get(0, 0);           // returns 99L (the matrix sees the change)
     *
     * matrix.rowView(5);          // throws IllegalArgumentException (row out of range)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return the specified row array (direct reference to internal storage)
     * @throws IllegalArgumentException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowCopy(int)
     */
    @Override
    public long[] rowView(final int rowIndex) throws IllegalArgumentException {
        N.checkArgument(rowIndex >= 0 && rowIndex < rowCount, MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount);

        return a[rowIndex];
    }

    /**
     * Returns a defensive copy of the specified row.
     * Changes to the returned array do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.rowCopy(0);          // returns [1L, 2L, 3L]
     * matrix.rowCopy(1);          // returns [4L, 5L, 6L]
     *
     * long[] firstRow = matrix.rowCopy(0);
     * firstRow[0] = 10L;          // modifies the copy only
     * matrix.get(0, 0);           // returns 1L (matrix unchanged)
     *
     * matrix.rowCopy(5);          // throws IllegalArgumentException (row out of range)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new long array containing the values from the specified row
     * @throws IllegalArgumentException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowView(int)
     */
    @Override
    public long[] rowCopy(final int rowIndex) throws IllegalArgumentException {
        N.checkArgument(rowIndex >= 0 && rowIndex < rowCount, MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount);

        return N.copyOf(a[rowIndex], columnCount);
    }

    /**
     * Returns a copy of the specified column as a new long array.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously in memory. Modifications to the returned array
     * will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.columnCopy(0);          // returns [1L, 4L]
     * matrix.columnCopy(2);          // returns [3L, 6L]
     *
     * long[] firstColumn = matrix.columnCopy(0);
     * firstColumn[0] = 99L;          // modifies the copy only
     * matrix.get(0, 0);              // returns 1L (matrix unchanged)
     *
     * matrix.columnCopy(9);          // throws IllegalArgumentException (column out of range)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new array containing the values from the specified column
     * @throws IllegalArgumentException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    @Override
    public long[] columnCopy(final int columnIndex) throws IllegalArgumentException {
        N.checkArgument(columnIndex >= 0 && columnIndex < columnCount, MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount);

        final long[] c = new long[rowCount];

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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.setRow(0, new long[] {7L, 8L, 9L});
     * matrix.rowCopy(0);                            // returns [7L, 8L, 9L]
     *
     * matrix.setRow(5, new long[] {1L, 2L, 3L});    // throws IllegalArgumentException (row out of range)
     * matrix.setRow(0, new long[] {1L, 2L});        // throws IllegalArgumentException (length != columnCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to set (0-based)
     * @param row the array of values to copy into the row; must have length equal to the number of columns
     * @throws IllegalArgumentException if {@code rowIndex} is out of bounds, or if {@code row.length}
     *         does not equal {@code columnCount}
     * @throws NullPointerException if {@code row} is {@code null}
     */
    public void setRow(final int rowIndex, final long[] row) throws IllegalArgumentException {
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.setColumn(0, new long[] {7L, 8L});
     * matrix.columnCopy(0);                         // returns [7L, 8L]
     *
     * matrix.setColumn(9, new long[] {1L, 2L});     // throws IllegalArgumentException (column out of range)
     * matrix.setColumn(0, new long[] {1L, 2L, 3L}); // throws IllegalArgumentException (length != rowCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to set (0-based)
     * @param column the array of values to copy into the column; must have length equal to the number of rows
     * @throws IllegalArgumentException if {@code columnIndex} is out of bounds, or if {@code column.length}
     *         does not equal {@code rowCount}
     * @throws NullPointerException if {@code column} is {@code null}
     */
    public void setColumn(final int columnIndex, final long[] column) throws IllegalArgumentException {
        N.checkArgument(columnIndex >= 0 && columnIndex < columnCount, MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount);
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.updateRow(0, x -> x * 2);   // doubles every value in row 0
     * matrix.rowCopy(0);                 // returns [2L, 4L, 6L]
     * matrix.rowCopy(1);                 // returns [4L, 5L, 6L] (row 1 untouched)
     *
     * matrix.updateRow(5, x -> x);       // throws ArrayIndexOutOfBoundsException (row out of range)
     * matrix.updateRow(0, null);         // throws IllegalArgumentException (operator is null)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param rowIndex the index of the row to update (0-based)
     * @param operator the operator to apply to each element in the row; receives the current
     *             element value and returns the new value
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} is out of bounds
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.LongUnaryOperator<E> operator) throws E {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new ArrayIndexOutOfBoundsException(formatMsg(MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount));
        }

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < columnCount; i++) {
            a[rowIndex][i] = operator.applyAsLong(a[rowIndex][i]);
        }
    }

    /**
     * Updates all elements in a column in-place by applying the specified operator to each element.
     * This modifies the matrix directly.
     *
     * <p>The operator is applied to each element in the specified column sequentially
     * from top to bottom (row 0 to row rowCount-1).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}, {5L, 6L}});
     * matrix.updateColumn(0, x -> x + 10L);   // adds 10 to every value in column 0
     * matrix.columnCopy(0);                   // returns [11L, 13L, 15L]
     * matrix.columnCopy(1);                   // returns [2L, 4L, 6L] (column 1 untouched)
     *
     * matrix.updateColumn(9, x -> x);         // throws ArrayIndexOutOfBoundsException (column out of range)
     * matrix.updateColumn(0, null);           // throws IllegalArgumentException (operator is null)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param columnIndex the index of the column to update (0-based)
     * @param operator the operator to apply to each element in the column; receives the current
     *             element value and returns the new value
     * @throws ArrayIndexOutOfBoundsException if {@code columnIndex} is out of bounds
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.LongUnaryOperator<E> operator) throws E {
        if (columnIndex < 0 || columnIndex >= columnCount) {
            throw new ArrayIndexOutOfBoundsException(formatMsg(MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount));
        }

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = operator.applyAsLong(a[i][columnIndex]);
        }
    }

    /**
     * Returns a copy of the main diagonal elements (upper-left to lower-right).
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>This method extracts the main diagonal elements at positions (0,0), (1,1), (2,2), etc.
     * The returned array is a copy; modifications to it will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * matrix.getMainDiagonal();   // returns [1L, 5L, 9L]
     *
     * LongMatrix single = LongMatrix.of(new long[][] {{42L}});
     * single.getMainDiagonal();   // returns [42L]
     *
     * LongMatrix rect = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * rect.getMainDiagonal();                 // throws IllegalStateException (not square)
     * LongMatrix.empty().getMainDiagonal();   // returns [] (empty is treated as square)
     * }</pre>
     *
     * @return a new long array containing a copy of the main diagonal elements
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     */
    @Override
    public long[] getMainDiagonal() throws IllegalStateException {
        checkIsSquare();

        final long[] res = new long[rowCount];

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
     * <p>This method sets the main diagonal elements at positions (0,0), (1,1), (2,2), etc.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.setMainDiagonal(new long[] {9L, 8L});
     * matrix.getMainDiagonal();                     // returns [9L, 8L]
     * matrix.get(0, 1);                             // returns 2L (off-diagonal untouched)
     *
     * matrix.setMainDiagonal(new long[] {1L});      // throws IllegalArgumentException (length != rowCount)
     * LongMatrix rect = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * rect.setMainDiagonal(new long[] {1L, 2L});    // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must have length equal to {@code rowCount}
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     * @throws IllegalArgumentException if {@code mainDiagonal} array length does not equal {@code rowCount}
     */
    @Override
    public void setMainDiagonal(final long[] mainDiagonal) throws IllegalStateException, IllegalArgumentException {
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.updateMainDiagonal(x -> x * x);   // squares each diagonal value
     * matrix.getMainDiagonal();                // returns [1L, 16L]
     * matrix.get(0, 1);                        // returns 2L (off-diagonal untouched)
     *
     * matrix.updateMainDiagonal(null);         // throws IllegalArgumentException (operator is null)
     * LongMatrix rect = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * rect.updateMainDiagonal(x -> x);         // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each diagonal element; receives current element value and returns new value
     * @throws IllegalStateException if the matrix is not square
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateMainDiagonal(final Throwables.LongUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = operator.applyAsLong(a[i][i]);
        }
    }

    /**
     * Returns a copy of the elements on the anti-diagonal from upper-right to lower-left.
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>This method extracts the anti-diagonal (secondary diagonal) elements from
     * upper-right to lower-left, at positions (0,n-1), (1,n-2), (2,n-3), etc.
     * The returned array is a copy; modifications to it will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * matrix.getAntiDiagonal();   // returns [3L, 5L, 7L]
     *
     * LongMatrix single = LongMatrix.of(new long[][] {{42L}});
     * single.getAntiDiagonal();   // returns [42L]
     *
     * LongMatrix rect = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * rect.getAntiDiagonal();                 // throws IllegalStateException (not square)
     * LongMatrix.empty().getAntiDiagonal();   // returns [] (empty is treated as square)
     * }</pre>
     *
     * @return a new long array containing a copy of the anti-diagonal elements
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     */
    @Override
    public long[] getAntiDiagonal() throws IllegalStateException {
        checkIsSquare();

        final long[] res = new long[rowCount];

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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.setAntiDiagonal(new long[] {9L, 8L});
     * matrix.getAntiDiagonal();                     // returns [9L, 8L]
     * matrix.get(0, 0);                             // returns 1L (off anti-diagonal untouched)
     *
     * matrix.setAntiDiagonal(new long[] {1L});      // throws IllegalArgumentException (length != rowCount)
     * LongMatrix rect = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * rect.setAntiDiagonal(new long[] {1L, 2L});    // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must have length equal to {@code rowCount}
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     * @throws IllegalArgumentException if {@code antiDiagonal} array length does not equal {@code rowCount}
     */
    @Override
    public void setAntiDiagonal(final long[] antiDiagonal) throws IllegalStateException, IllegalArgumentException {
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.updateAntiDiagonal(x -> -x);   // negates each anti-diagonal value
     * matrix.getAntiDiagonal();             // returns [-2L, -3L]
     * matrix.get(0, 0);                     // returns 1L (off anti-diagonal untouched)
     *
     * matrix.updateAntiDiagonal(null);      // throws IllegalArgumentException (operator is null)
     * LongMatrix rect = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * rect.updateAntiDiagonal(x -> x);      // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each anti-diagonal element; receives current element value and returns new value
     * @throws IllegalStateException if the matrix is not square
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAntiDiagonal(final Throwables.LongUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = operator.applyAsLong(a[i][columnCount - i - 1]);
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.updateAll(x -> x * 2);   // doubles every element
     * matrix.flatten().toArray();     // returns [2L, 4L, 6L, 8L]
     *
     * LongMatrix.empty().updateAll(x -> x + 1L);                               // no-op on an empty matrix
     * matrix.updateAll((Throwables.LongUnaryOperator<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each element; receives the current element value
     *             and returns the new value
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.LongUnaryOperator<E> operator) throws E {
        N.checkArgNotNull(operator, "operator");
        final Throwables.IntBiConsumer<E> operation = (i, j) -> a[i][j] = operator.applyAsLong(a[i][j]);
        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{0L, 0L, 0L}, {0L, 0L, 0L}});
     * matrix.updateAll((i, j) -> (long) (i + j));   // each element = sum of its indices
     * matrix.flatten().toArray();                   // returns [0L, 1L, 2L, 1L, 2L, 3L]
     *
     * matrix.updateAll((i, j) -> i * 10L + j);      // position encoding
     * matrix.flatten().toArray();                   // returns [0L, 1L, 2L, 10L, 11L, 12L]
     *
     * matrix.updateAll((Throwables.IntBiFunction<Long, RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the function that receives row index and column index (0-based) and returns
     *             the new value for that position
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the mapper throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Long, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final Throwables.IntBiConsumer<E> operation = (i, j) -> a[i][j] = mapper.apply(i, j);
        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{-1L, 2L, -3L}, {4L, -5L, 6L}});
     * matrix.replaceIf(x -> x < 0, 0L);   // replaces every negative value with 0L
     * matrix.flatten().toArray();         // returns [0L, 2L, 0L, 4L, 0L, 6L]
     *
     * matrix.replaceIf(x -> x > 100L, 9L);                                     // no element matches; matrix unchanged
     * matrix.flatten().toArray();                                              // returns [0L, 2L, 0L, 4L, 0L, 6L]
     * matrix.replaceIf((Throwables.LongPredicate<RuntimeException>) null, 0L); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to test each element; elements for which this returns
     *                  {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.LongPredicate<E> predicate, final long newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> operation = (i, j) -> a[i][j] = predicate.test(a[i][j]) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * matrix.replaceIf((i, j) -> i == j, 0L);   // zero the main diagonal
     * matrix.flatten().toArray();               // returns [0L, 2L, 3L, 4L, 0L, 6L, 7L, 8L, 0L]
     *
     * matrix.replaceIf((i, j) -> i == 0 || j == 0, -1L);                        // set first row and column to -1
     * matrix.flatten().toArray();                                               // returns [-1L, -1L, -1L, -1L, 0L, 6L, -1L, 8L, 0L]
     * matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0L); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition that tests row index and column index (0-based); elements
     *                  at positions for which this returns {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final long newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> operation = (i, j) -> a[i][j] = predicate.test(i, j) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));
    }

    /**
     * Creates a new LongMatrix by applying a transformation function to each element.
     * The original matrix is not modified; a new matrix with transformed values is returned.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
     * This is the immutable counterpart to {@link #updateAll(Throwables.LongUnaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix squared = matrix.map(x -> x * x);
     * squared.flatten().toArray();    // returns [1L, 4L, 9L, 16L]
     * matrix.get(0, 0);               // returns 1L (original unchanged)
     *
     * matrix.map(x -> -x).flatten().toArray();                           // returns [-1L, -2L, -3L, -4L]
     * matrix.map((Throwables.LongUnaryOperator<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to apply to each element; receives the current element value
     *             and returns the transformed value
     * @return a new {@code LongMatrix} with transformed values
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     * @see #updateAll(Throwables.LongUnaryOperator)
     */
    public <E extends Exception> LongMatrix map(final Throwables.LongUnaryOperator<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final long[][] result = new long[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.applyAsLong(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return LongMatrix.of(result);
    }

    /**
     * Creates a new {@code IntMatrix} by applying the specified function to each element of this matrix.
     * The original matrix is not modified. Each {@code long} element is independently converted to an {@code int}
     * by the function, and the results are collected into a new {@code IntMatrix} with the same dimensions.
     * The operation may be performed in parallel for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{123L, 256L}, {300L, 401L}});
     * IntMatrix intMatrix = matrix.mapToInt(x -> (int) (x % 100));
     * intMatrix.get(0, 0);            // returns 23
     * intMatrix.get(1, 0);            // returns 0 ((int)(300 % 100))
     *
     * matrix.mapToInt(x -> (int) x).rowCount();                               // returns 2 (same dimensions)
     * matrix.mapToInt((Throwables.LongToIntFunction<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the exception type that the function may throw
     * @param mapper the mapping function that converts each {@code long} element to an {@code int}; must not be {@code null}
     * @return a new {@code IntMatrix} with the mapped values (same dimensions as the original)
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> IntMatrix mapToInt(final Throwables.LongToIntFunction<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final int[][] result = new int[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.applyAsInt(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return IntMatrix.of(result);
    }

    /**
     * Creates a new {@code DoubleMatrix} by applying the specified function to each element of this matrix.
     * The original matrix is not modified. Each {@code long} element is independently converted to a {@code double}
     * by the function, and the results are collected into a new {@code DoubleMatrix} with the same dimensions.
     * The operation may be performed in parallel for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{4L, 9L}, {16L, 25L}});
     * DoubleMatrix doubleMatrix = matrix.mapToDouble(x -> Math.sqrt(x));
     * doubleMatrix.get(0, 0);            // returns 2.0
     * doubleMatrix.get(1, 1);            // returns 5.0
     *
     * matrix.mapToDouble(x -> (double) x).columnCount();                            // returns 2 (same dimensions)
     * matrix.mapToDouble((Throwables.LongToDoubleFunction<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the exception type that the function may throw
     * @param mapper the mapping function that converts each {@code long} element to a {@code double}; must not be {@code null}
     * @return a new {@code DoubleMatrix} with the mapped values (same dimensions as the original)
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> DoubleMatrix mapToDouble(final Throwables.LongToDoubleFunction<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final double[][] result = new double[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.applyAsDouble(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return DoubleMatrix.of(result);
    }

    /**
     * Creates a new object {@code Matrix} by applying the specified function to each element of this matrix.
     * The original matrix is not modified. Each {@code long} element is independently converted to an object
     * of type {@code R} by the function, and the results are collected into a new {@code Matrix} with the same dimensions.
     * The operation may be performed in parallel for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{123L, 456L}, {789L, 12L}});
     * Matrix<String> stringMatrix = matrix.mapToObj(Long::toString, String.class);
     * stringMatrix.get(0, 0);            // returns "123"
     * stringMatrix.get(1, 1);            // returns "12"
     *
     * matrix.mapToObj(x -> "v" + x, String.class).get(0, 1);   // returns "v456"
     * matrix.mapToObj(null, String.class);                     // throws IllegalArgumentException (mapper is null)
     * }</pre>
     *
     * @param <R> the type of elements in the resulting matrix
     * @param <E> the exception type that the function may throw
     * @param mapper the mapping function that converts each {@code long} element to type {@code R}; must not be {@code null}
     * @param targetElementType the class object representing the target element type (used for array creation); must not be {@code null}
     * @return a new {@code Matrix<R>} with the mapped values (same dimensions as the original)
     * @throws IllegalArgumentException if {@code mapper} or {@code targetElementType} is {@code null}
     * @throws E if the function throws an exception
     */
    public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.LongFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E {
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.fill(5L);
     * matrix.flatten().toArray();   // returns [5L, 5L, 5L, 5L]
     *
     * matrix.fill(0L);
     * matrix.get(1, 1);             // returns 0L
     * LongMatrix.empty().fill(7L);  // no-op on an empty matrix
     * }</pre>
     *
     * @param value the value to fill the matrix with
     */
    public void fill(final long value) {
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{0L, 0L, 0L}, {0L, 0L, 0L}});
     * matrix.fill(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.flatten().toArray();   // returns [1L, 2L, 0L, 3L, 4L, 0L] (only overlap copied)
     *
     * matrix.fill(new long[][] {{9L, 9L, 9L, 9L}});   // source row longer than width
     * matrix.rowCopy(0);                              // returns [9L, 9L, 9L] (excess ignored)
     * matrix.rowCopy(1);                              // returns [3L, 4L, 0L] (only row 0 overwritten)
     * }</pre>
     *
     * @param source the two-dimensional array to copy values from
     */
    public void fill(final long[][] source) {
        fill(0, 0, source);
    }

    /**
     * Fills a region of the matrix with values from another two-dimensional array, starting at the specified position.
     * The source array can extend beyond this matrix's bounds; only the overlapping region is copied.
     * The matrix is modified in-place. Elements outside the matrix bounds are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{0L, 0L, 0L}, {0L, 0L, 0L}, {0L, 0L, 0L}});
     * matrix.fill(1, 1, new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.flatten().toArray();   // returns [0L, 0L, 0L, 0L, 1L, 2L, 0L, 3L, 4L]
     *
     * matrix.fill(2, 2, new long[][] {{7L, 8L}});   // clipped at the right edge
     * matrix.get(2, 2);                             // returns 7L (8L falls outside, ignored)
     *
     * matrix.fill(0, 0, (long[][]) null);          // throws IllegalArgumentException (source is null)
     * matrix.fill(5, 0, new long[][] {{1L}});      // throws IllegalArgumentException (destRowIndex out of range)
     * }</pre>
     *
     * @param destRowIndex the target row index in this matrix (0-based, must be {@code 0 <= destRowIndex <= rowCount})
     * @param destColumnIndex the target column index in this matrix (0-based, must be {@code 0 <= destColumnIndex <= columnCount})
     * @param source the source array to copy values from; must not be {@code null}.
     *        Individual rows ({@code source[i]}) may be {@code null} and are skipped during copy.
     * @throws IllegalArgumentException if {@code source} is {@code null}, if {@code destRowIndex} is not in
     *         {@code [0, rowCount]}, or if {@code destColumnIndex} is not in {@code [0, columnCount]}
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final long[][] source) throws IllegalArgumentException {
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
     * <p>The returned matrix is completely independent from the original. All elements
     * are copied into a new two-dimensional array, ensuring that modifications to either
     * the copy or the original will not affect the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix original = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix copy = original.copy();
     * copy.equals(original);       // returns true (same values)
     *
     * copy.set(0, 0, 99L);
     * original.get(0, 0);                    // returns 1L (original unchanged)
     * copy.get(0, 0);                        // returns 99L (copy modified independently)
     * LongMatrix.empty().copy().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new matrix that is a copy of this matrix
     */
    @Override
    public LongMatrix copy() {
        final long[][] c = new long[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[i].clone();
        }

        return new LongMatrix(c);
    }

    /**
     * Creates a copy of a row range from this matrix.
     * The returned matrix contains only the specified rows and is completely independent from the original matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}, {5L, 6L}});
     * LongMatrix subset = matrix.copy(1, 3);   // rows 1 and 2 (exclusive end)
     * subset.flatten().toArray();              // returns [3L, 4L, 5L, 6L]
     * subset.rowCount();                       // returns 2
     *
     * matrix.copy(0, 0).rowCount();            // returns 0 (empty range)
     * matrix.copy(0, 5);                       // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new {@code LongMatrix} containing the specified rows
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public LongMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        final long[][] c = new long[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = a[i].clone();
        }

        return new LongMatrix(c);
    }

    /**
     * Creates a copy of a submatrix defined by row and column ranges.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * LongMatrix submatrix = matrix.copy(0, 2, 1, 3);   // rows 0-1, columns 1-2
     * submatrix.flatten().toArray();                    // returns [2L, 3L, 5L, 6L]
     * submatrix.rowCount();                             // returns 2
     *
     * matrix.copy(1, 2, 0, 1).get(0, 0);                // returns 4L (single-cell submatrix)
     * matrix.copy(0, 2, 1, 9);                          // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new {@code LongMatrix} containing the specified submatrix
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount}, or if either
     *         {@code from} index exceeds its corresponding {@code to} index
     */
    @Override
    public LongMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        final long[][] c = new long[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = N.copyOfRange(a[i], fromColumnIndex, toColumnIndex);
        }

        return new LongMatrix(c);
    }

    /**
     * Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount},
     * anchored at the top-left corner of this matrix. New cells are filled with {@code 0L}.
     *
     * <ul>
     *   <li><b>If a dimension shrinks</b> — elements beyond the new boundary are discarded
     *       (excess rows removed from the bottom, excess columns removed from the right).</li>
     *   <li><b>If a dimension grows</b> — new cells are filled with {@code 0L}.</li>
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     *
     * // Grow: both dimensions larger — new cells filled with 0L
     * LongMatrix grown = matrix.resize(4, 4);
     * // Result: [[1, 2, 3, 0],
     * //          [4, 5, 6, 0],
     * //          [7, 8, 9, 0],
     * //          [0, 0, 0, 0]]
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * LongMatrix truncated = matrix.resize(2, 2);
     * // Result: [[1, 2],
     * //          [4, 5]]
     *
     * // Mixed: grow rows, truncate columns
     * LongMatrix mixed = matrix.resize(4, 2);
     * // Result: [[1, 2],
     * //          [4, 5],
     * //          [7, 8],
     * //          [0, 0]]
     *
     * grown.flatten().toArray();      // returns [1L, 2L, 3L, 0L, 4L, 5L, 6L, 0L, 7L, 8L, 9L, 0L, 0L, 0L, 0L, 0L]
     * truncated.flatten().toArray();  // returns [1L, 2L, 4L, 5L]
     * matrix.resize(0, 0).isEmpty();  // returns true
     * matrix.resize(-1, 2);           // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new {@code LongMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int, long)
     * @see #extend(int, int, int, int)
     */
    public LongMatrix resize(final int newRowCount, final int newColumnCount) {
        return resize(newRowCount, newColumnCount, 0);
    }

    /**
     * Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount},
     * anchored at the top-left corner of this matrix. New cells are filled with {@code defaultValue}.
     *
     * <ul>
     *   <li><b>If a dimension shrinks</b> — elements beyond the new boundary are discarded
     *       (excess rows removed from the bottom, excess columns removed from the right).</li>
     *   <li><b>If a dimension grows</b> — new cells are filled with {@code defaultValue}.</li>
     *   <li><b>Mixed case</b> — each dimension is treated independently, so it is valid
     *       to grow rows while truncating columns, or vice versa.</li>
     * </ul>
     *
     * <p>The original matrix is never modified; a new matrix is always returned.</p>
     *
     * <p><b>Comparison with {@link #extend(int, int, int, int, long)}:</b>
     * {@code resize} takes <em>absolute</em> target dimensions and may truncate existing content.
     * {@code extend} takes <em>relative</em> padding amounts per edge and <em>never truncates</em>.
     * Use {@code extend} when the entire original content must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     *
     * // Grow: both dimensions larger — new cells filled with 9L
     * LongMatrix grown = matrix.resize(4, 4, 9L);
     * // Result: [[1, 2, 3, 9],
     * //          [4, 5, 6, 9],
     * //          [7, 8, 9, 9],
     * //          [9, 9, 9, 9]]
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * LongMatrix truncated = matrix.resize(2, 2, 9L);
     * // Result: [[1, 2],
     * //          [4, 5]]
     *
     * // Mixed: grow rows, truncate columns
     * LongMatrix mixed = matrix.resize(4, 2, 9L);
     * // Result: [[1, 2],
     * //          [4, 5],
     * //          [7, 8],
     * //          [9, 9]]
     *
     * grown.flatten().toArray();                     // returns [1L, 2L, 3L, 9L, 4L, 5L, 6L, 9L, 7L, 8L, 9L, 9L, 9L, 9L, 9L, 9L]
     * mixed.rowCopy(3);                              // returns [9L, 9L]
     * matrix.resize(2, 2, 9L).flatten().toArray();   // returns [1L, 2L, 4L, 5L]
     * matrix.resize(-1, 2, 9L);                      // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the long value used to fill any newly created cells
     * @return a new {@code LongMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int)
     * @see #extend(int, int, int, int, long)
     */
    public LongMatrix resize(final int newRowCount, final int newColumnCount, final long defaultValue) throws IllegalArgumentException {
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
            final boolean fillDefaultValue = defaultValue != 0;
            final long[][] b = new long[newRowCount][];

            for (int i = 0; i < newRowCount; i++) {
                b[i] = i < rowCount ? N.copyOf(a[i], newColumnCount) : new long[newColumnCount];

                if (fillDefaultValue) {
                    if (i >= rowCount) {
                        N.fill(b[i], defaultValue);
                    } else if (columnCount < newColumnCount) {
                        N.fill(b[i], columnCount, newColumnCount, defaultValue);
                    }
                }
            }

            return new LongMatrix(b);
        }
    }

    /**
     * Returns a new matrix formed by adding {@code 0L}-filled padding around every edge of this matrix.
     * The original content is preserved in its entirety at the interior of the result.
     *
     * <p>The result dimensions are:
     * <ul>
     *   <li>Rows: {@code padTop + this.rowCount + padBottom}</li>
     *   <li>Columns: {@code padLeft + this.columnCount + padRight}</li>
     * </ul>
     *
     * <p><b>Unlike {@link #resize(int, int)}, this method never truncates existing content.</b>
     * All elements of the original matrix appear unchanged in the result.</p>
     *
     * <p><b>Comparison with {@link #resize(int, int)}:</b>
     * {@code extend} takes <em>relative</em> padding amounts per edge and never truncates.
     * {@code resize} takes <em>absolute</em> target dimensions and may discard content.
     * Use {@code resize} when you need exact output dimensions regardless of the original size.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}});
     *
     * // Uniform border of 1 cell on every side
     * LongMatrix bordered = matrix.extend(1, 1, 1, 1);
     * // Result: [[0, 0, 0, 0],
     * //          [0, 1, 2, 0],
     * //          [0, 0, 0, 0]]
     * bordered.rowCount();            // returns 3
     * bordered.columnCount();         // returns 4
     * bordered.get(1, 1);             // returns 1L (original content preserved)
     *
     * matrix.extend(0, 0, 0, 0).equals(matrix);   // returns true (no padding returns a copy)
     * matrix.extend(-1, 0, 0, 0);                 // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of rows to add above; must be {@code >= 0}
     * @param padBottom number of rows to add below; must be {@code >= 0}
     * @param padLeft number of columns to add to the left; must be {@code >= 0}
     * @param padRight number of columns to add to the right; must be {@code >= 0}
     * @return a new {@code LongMatrix} with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #extend(int, int, int, int, long)
     * @see #resize(int, int)
     */
    @Override
    public LongMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight) {
        return extend(padTop, padBottom, padLeft, padRight, 0);
    }

    /**
     * Returns a new matrix formed by adding {@code defaultValue}-filled padding around every edge
     * of this matrix. The original content is preserved in its entirety at the interior of the result.
     *
     * <p>The result dimensions are:
     * <ul>
     *   <li>Rows: {@code padTop + this.rowCount + padBottom}</li>
     *   <li>Columns: {@code padLeft + this.columnCount + padRight}</li>
     * </ul>
     *
     * <p><b>Unlike {@link #resize(int, int, long)}, this method never truncates existing content.</b>
     * All elements of the original matrix appear unchanged in the result.</p>
     *
     * <p><b>Typical uses:</b> zero-padding before convolution, adding sentinel borders, or creating
     * asymmetric margins (e.g. more padding on one side than another).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}});
     *
     * // Asymmetric padding: 2 columns on the left, 1 on the right
     * LongMatrix padded = matrix.extend(1, 1, 2, 1, 9L);
     * // Result: [[9, 9, 9, 9, 9],
     * //          [9, 9, 1, 2, 9],
     * //          [9, 9, 9, 9, 9]]
     * padded.rowCount();            // returns 3
     * padded.columnCount();         // returns 5
     * padded.get(1, 2);             // returns 1L (original content preserved)
     * padded.get(0, 0);             // returns 9L (padding cell)
     *
     * matrix.extend(0, 0, 0, 0, 9L).equals(matrix);   // returns true (no padding returns a copy)
     * matrix.extend(1, 1, -1, 1, 9L);                 // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of rows to add above; must be {@code >= 0}
     * @param padBottom number of rows to add below; must be {@code >= 0}
     * @param padLeft number of columns to add to the left; must be {@code >= 0}
     * @param padRight number of columns to add to the right; must be {@code >= 0}
     * @param defaultValue the long value used to fill all newly added cells
     * @return a new {@code LongMatrix} with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #extend(int, int, int, int)
     * @see #resize(int, int, long)
     */
    public LongMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final long defaultValue)
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
            final boolean fillDefaultValue = defaultValue != 0;
            final long[][] b = new long[newRowCount][newColumnCount];

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

            return new LongMatrix(b);
        }
    }

    /**
     * Reverses the order of elements in each row (horizontal flip in-place).
     * This operation modifies the matrix directly. For a non-destructive version, use {@link #flipHorizontally()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.flipHorizontallyInPlace();
     * matrix.rowCopy(0);            // returns [3L, 2L, 1L]
     * matrix.rowCopy(1);            // returns [6L, 5L, 4L]
     *
     * matrix.flipHorizontallyInPlace();               // flipping again restores the original
     * matrix.rowCopy(0);                              // returns [1L, 2L, 3L]
     * LongMatrix.empty().flipHorizontallyInPlace();   // no-op on an empty matrix
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
     * Reverses the order of rows in the matrix (vertical flip in-place).
     * This operation modifies the matrix directly by reversing the row order. For a non-destructive version, use {@link #flipVertically()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * matrix.flipVerticallyInPlace();
     * matrix.rowCopy(0);            // returns [7L, 8L, 9L]
     * matrix.rowCopy(2);            // returns [1L, 2L, 3L]
     *
     * matrix.flipVerticallyInPlace();               // flipping again restores the original
     * matrix.rowCopy(0);                            // returns [1L, 2L, 3L]
     * LongMatrix.empty().flipVerticallyInPlace();   // no-op on an empty matrix
     * }</pre>
     *
     * @see #flipVertically()
     * @see #flipHorizontallyInPlace()
     */
    @Override
    public void flipVerticallyInPlace() {
        for (int l = 0, h = rowCount - 1; l < h; l++, h--) {
            final long[] tmp = a[l];
            a[l] = a[h];
            a[h] = tmp;
        }
    }

    /**
     * Creates a new matrix that is horizontally flipped (each row reversed).
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * LongMatrix flipped = matrix.flipHorizontally();
     * flipped.rowCopy(0);          // returns [3L, 2L, 1L]
     * flipped.rowCopy(1);          // returns [6L, 5L, 4L]
     * matrix.rowCopy(0);           // returns [1L, 2L, 3L] (original unchanged)
     *
     * LongMatrix.empty().flipHorizontally().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new {@code LongMatrix} with each row reversed
     * @see #flipHorizontallyInPlace()
     * @see #flipVertically()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public LongMatrix flipHorizontally() {
        final LongMatrix res = this.copy();
        res.flipHorizontallyInPlace();
        return res;
    }

    /**
     * Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * LongMatrix flipped = matrix.flipVertically();
     * flipped.rowCopy(0);          // returns [4L, 5L, 6L]
     * flipped.rowCopy(1);          // returns [1L, 2L, 3L]
     * matrix.rowCopy(0);           // returns [1L, 2L, 3L] (original unchanged)
     *
     * LongMatrix.empty().flipVertically().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new {@code LongMatrix} with rows in reversed order
     * @see #flipVerticallyInPlace()
     * @see #flipHorizontally()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public LongMatrix flipVertically() {
        final LongMatrix res = this.copy();
        res.flipVerticallyInPlace();
        return res;
    }

    /**
     * Returns a new matrix that is this matrix rotated 90 degrees clockwise.
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * column of the result being the last row of the original, reading upward.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    Rotated 90° clockwise:
     * // 1 2          3 1
     * // 3 4     =>   4 2
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.rotate90().flatten().toArray();   // returns [3L, 1L, 4L, 2L]
     *
     * LongMatrix rect = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * rect.rotate90().flatten().toArray();     // returns [1L, 2L, 3L] (1x3 becomes 3x1)
     * rect.rotate90().rowCount();              // returns 3
     * LongMatrix.empty().rotate90().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new {@code LongMatrix} rotated 90 degrees clockwise
     */
    @Override
    public LongMatrix rotate90() {
        if (columnCount == 0) {
            return EMPTY_LONG_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final long[][] c = new long[columnCount][rowCount];

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

        return new LongMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 180 degrees.
     * This is equivalent to flipping both horizontally and vertically, reversing the
     * order of all elements. The resulting matrix has the same dimensions as the original.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    Rotated 180°:
     * // 1 2          4 3
     * // 3 4     =>   2 1
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.rotate180().flatten().toArray();   // returns [4L, 3L, 2L, 1L]
     *
     * LongMatrix rect = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * rect.rotate180().flatten().toArray();     // returns [6L, 5L, 4L, 3L, 2L, 1L] (same shape)
     * LongMatrix.empty().rotate180().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new {@code LongMatrix} rotated 180 degrees
     */
    @Override
    public LongMatrix rotate180() {
        final long[][] c = new long[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[rowCount - i - 1].clone();
            N.reverse(c[i]);
        }

        return new LongMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 270 degrees clockwise.
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * column of the result being the first row of the original, reading downward.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    Rotated 270° clockwise:
     * // 1 2          2 4
     * // 3 4     =>   1 3
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.rotate270().flatten().toArray();   // returns [2L, 4L, 1L, 3L]
     *
     * LongMatrix rect = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * rect.rotate270().flatten().toArray();     // returns [3L, 2L, 1L] (1x3 becomes 3x1)
     * LongMatrix.empty().rotate270().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new {@code LongMatrix} rotated 270 degrees clockwise
     */
    @Override
    public LongMatrix rotate270() {
        if (columnCount == 0) {
            return EMPTY_LONG_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final long[][] c = new long[columnCount][rowCount];

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

        return new LongMatrix(c);
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
     * // 1L 2L 3L   1L 4L
     * // 4L 5L 6L   2L 5L
     * //            3L 6L
     *
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * LongMatrix transposed = matrix.transpose();   // 2x3 becomes 3x2
     * transposed.rowCount();                        // returns 3
     * transposed.flatten().toArray();               // returns [1L, 4L, 2L, 5L, 3L, 6L]
     *
     * matrix.transpose().transpose().equals(matrix);   // returns true (double transpose)
     * LongMatrix.empty().transpose().isEmpty();        // returns true
     * }</pre>
     *
     * @return a new {@code LongMatrix} that is the transpose of this matrix with dimensions {@code columnCount × rowCount};
     *         an {@code N x 0} matrix transposes to the empty {@code 0 x 0} matrix, because the swapped shape
     *         {@code 0 x N} (zero rows with a non-zero column count) is not representable
     */
    @Override
    public LongMatrix transpose() {
        if (columnCount == 0) {
            return EMPTY_LONG_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final long[][] c = new long[columnCount][rowCount];

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

        return new LongMatrix(c);
    }

    /**
     * Reshapes the matrix to new dimensions while preserving element order.
     * Elements are read in row-major order from the original matrix and placed into the new shape.
     *
     * <p>The new shape must have at least as many total elements as the original
     * ({@code newRowCount * newColumnCount >= elementCount()}).
     * If the new shape has more total elements, the additional positions are filled with zeros.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.reshape(3, 2).flatten().toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L]
     * matrix.reshape(3, 2).rowCount();            // returns 3
     *
     * matrix.reshape(2, 4).flatten().toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L, 0L, 0L] (extra cells are 0)
     * matrix.reshape(1, 4);                       // throws IllegalArgumentException (too small to hold 6 elements)
     * matrix.reshape(-1, 6);                      // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix (must be non-negative)
     * @param newColumnCount the number of columns in the reshaped matrix (must be non-negative)
     * @return a new {@code LongMatrix} with the specified shape containing this matrix's elements
     * @throws IllegalArgumentException if either dimension is negative, if the dimensions are not a representable shape,
     *         or if the new shape is too small to hold all elements
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public LongMatrix reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final long[][] c = new long[newRowCount][newColumnCount];

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return new LongMatrix(c);
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

        return new LongMatrix(c);
    }

    /**
     * Repeats elements in the matrix by the specified factors in both row and column directions.
     * Each element is repeated {@code rowRepeats} times in the row direction and {@code columnRepeats}
     * times in the column direction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix repeated = matrix.repeatElements(2, 3);
     * // Result: [[1, 1, 1, 2, 2, 2],
     * //          [1, 1, 1, 2, 2, 2],
     * //          [3, 3, 3, 4, 4, 4],
     * //          [3, 3, 3, 4, 4, 4]]
     * repeated.rowCount();            // returns 4
     * repeated.columnCount();         // returns 6
     * repeated.rowCopy(0);            // returns [1L, 1L, 1L, 2L, 2L, 2L]
     *
     * matrix.repeatElements(1, 1).equals(matrix);   // returns true (no expansion)
     * matrix.repeatElements(0, 2);                  // throws IllegalArgumentException (repeats must be > 0)
     * }</pre>
     *
     * @param rowRepeats the number of times to repeat each element in the row direction
     * @param columnRepeats the number of times to repeat each element in the column direction
     * @return a new {@code LongMatrix} with repeated elements
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is less than or equal to 0,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#repeatElements(int, int)
     */
    @Override
    public LongMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final long[][] c = new long[rowCount * rowRepeats][columnCount * columnRepeats];

        for (int i = 0; i < rowCount; i++) {
            final long[] aa = a[i];
            final long[] fr = c[i * rowRepeats];

            for (int j = 0; j < columnCount; j++) {
                N.copy(Array.repeat(aa[j], columnRepeats), 0, fr, j * columnRepeats, columnRepeats);
            }

            for (int k = 1; k < rowRepeats; k++) {
                N.copy(fr, 0, c[i * rowRepeats + k], 0, fr.length);
            }
        }

        return new LongMatrix(c);
    }

    /**
     * Repeats the entire matrix as a tile pattern by the specified factors in both row and column directions.
     * The whole matrix is repeated {@code rowRepeats} times in the row direction and {@code columnRepeats}
     * times in the column direction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix tiled = matrix.repeatMatrix(2, 3);
     * // Result: [[1, 2, 1, 2, 1, 2],
     * //          [3, 4, 3, 4, 3, 4],
     * //          [1, 2, 1, 2, 1, 2],
     * //          [3, 4, 3, 4, 3, 4]]
     * tiled.rowCount();            // returns 4
     * tiled.columnCount();         // returns 6
     * tiled.rowCopy(0);            // returns [1L, 2L, 1L, 2L, 1L, 2L]
     *
     * matrix.repeatMatrix(1, 1).equals(matrix);   // returns true (single tile)
     * matrix.repeatMatrix(2, 0);                  // throws IllegalArgumentException (repeats must be > 0)
     * }</pre>
     *
     * @param rowRepeats the number of times to repeat the matrix in the row direction
     * @param columnRepeats the number of times to repeat the matrix in the column direction
     * @return a new {@code LongMatrix} with the original matrix repeated as tiles
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is less than or equal to 0,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#repeatMatrix(int, int)
     */
    @Override
    public LongMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final long[][] c = new long[rowCount * rowRepeats][columnCount * columnRepeats];

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

        return new LongMatrix(c);
    }

    /**
     * Returns a list containing all matrix elements in row-major order (row by row, left to right).
     * The elements are flattened into a single-dimensional list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.flatten().toArray();   // returns [1L, 2L, 3L, 4L]
     * matrix.flatten().size();      // returns 4
     *
     * LongMatrix row = LongMatrix.of(new long[][] {{5L, 6L, 7L}});
     * row.flatten().toArray();               // returns [5L, 6L, 7L]
     * LongMatrix.empty().flatten().size();   // returns 0
     * }</pre>
     *
     * @return a new {@code LongList} containing all elements in row-major order
     * @throws IllegalStateException if the matrix is too large to flatten (total size exceeds {@code Integer.MAX_VALUE})
     */
    @Override
    public LongList flatten() {
        // Check for overflow before allocation
        if ((long) rowCount * columnCount > Integer.MAX_VALUE) {
            throw new IllegalStateException("Matrix too large to flatten: " + rowCount + " x " + columnCount);
        }

        final long[] c = new long[rowCount * columnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c, i * columnCount, columnCount);
        }

        return LongList.of(c);
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{5L, 3L}, {4L, 1L}});
     * matrix.mutateAsFlat(arr -> java.util.Arrays.sort(arr));   // sort all elements globally
     * matrix.flatten().toArray();                               // returns [1L, 3L, 4L, 5L]
     *
     * matrix.mutateAsFlat(arr -> { for (int i = 0; i < arr.length; i++) arr[i] *= 10L; });
     * matrix.flatten().toArray();                                           // returns [10L, 30L, 40L, 50L]
     * LongMatrix.empty().mutateAsFlat(arr -> java.util.Arrays.sort(arr));   // no-op on an empty matrix
     * }</pre>
     *
     * @param <E> the type of exception that the operation may throw
     * @param action the operation to apply to the flattened array
     * @throws E if the operation throws an exception
     * @see Arrays#mutateAsFlat(long[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super long[], E> action) throws E {
        Arrays.mutateAsFlat(a, action);
    }

    /**
     * Vertically stacks this matrix with another matrix.
     * The two matrices must have the same number of columns.
     * The result is a new matrix where the rows of the specified matrix are appended below the rows of this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix1 = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * LongMatrix matrix2 = LongMatrix.of(new long[][] {{4L, 5L, 6L}, {7L, 8L, 9L}});
     * LongMatrix stacked = matrix1.stackVertically(matrix2);
     * stacked.rowCount();             // returns 3
     * stacked.flatten().toArray();    // returns [1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L]
     *
     * LongMatrix wide = LongMatrix.of(new long[][] {{1L, 2L}});
     * matrix1.stackVertically(wide);                // throws IllegalArgumentException (column count mismatch)
     * matrix1.stackVertically((LongMatrix) null);   // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to stack below this matrix
     * @return a new {@code LongMatrix} with rows from both matrices stacked vertically
     * @throws IllegalArgumentException if {@code other} is {@code null}, if the matrices don't have the same number of columns,
     *         or if the merged row count would exceed {@code Integer.MAX_VALUE}
     * @see IntMatrix#stackVertically(IntMatrix)
     */
    @Override
    public LongMatrix stackVertically(final LongMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.columnCount, MSG_VSTACK_COLUMN_MISMATCH, columnCount, other.columnCount);
        final long mergedRowCount = (long) rowCount + other.rowCount;
        N.checkArgument(mergedRowCount <= Integer.MAX_VALUE, "Merged row count overflow: {} + {} = {}", rowCount, other.rowCount, mergedRowCount);

        final long[][] c = new long[(int) mergedRowCount][];
        int j = 0;

        for (int i = 0; i < rowCount; i++) {
            c[j++] = a[i].clone();
        }

        for (int i = 0; i < other.rowCount; i++) {
            c[j++] = other.a[i].clone();
        }

        return LongMatrix.of(c);
    }

    /**
     * Horizontally stacks this matrix with another matrix.
     * The two matrices must have the same number of rows.
     * The result is a new matrix where the columns of the specified matrix are appended to the right of this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix1 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix matrix2 = LongMatrix.of(new long[][] {{5L}, {6L}});
     * LongMatrix stacked = matrix1.stackHorizontally(matrix2);
     * stacked.columnCount();          // returns 3
     * stacked.flatten().toArray();    // returns [1L, 2L, 5L, 3L, 4L, 6L]
     *
     * LongMatrix tall = LongMatrix.of(new long[][] {{5L}, {6L}, {7L}});
     * matrix1.stackHorizontally(tall);                // throws IllegalArgumentException (row count mismatch)
     * matrix1.stackHorizontally((LongMatrix) null);   // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to stack to the right of this matrix
     * @return a new {@code LongMatrix} with columns from both matrices stacked horizontally
     * @throws IllegalArgumentException if {@code other} is {@code null}, if the matrices don't have the same number of rows,
     *         or if the merged column count would exceed {@code Integer.MAX_VALUE}
     * @see IntMatrix#stackHorizontally(IntMatrix)
     */
    @Override
    public LongMatrix stackHorizontally(final LongMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(rowCount == other.rowCount, MSG_HSTACK_ROW_MISMATCH, rowCount, other.rowCount);
        final long mergedColumnCount = (long) columnCount + other.columnCount;
        N.checkArgument(mergedColumnCount <= Integer.MAX_VALUE, "Merged column count overflow: {} + {} = {}", columnCount, other.columnCount,
                mergedColumnCount);

        final long[][] c = new long[rowCount][(int) mergedColumnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c[i], 0, columnCount);
            N.copy(other.a[i], 0, c[i], columnCount, other.columnCount);
        }

        return LongMatrix.of(c);
    }

    /**
     * Performs element-wise addition of this matrix with another matrix.
     * The two matrices must have the same dimensions.
     * <p><b>Note:</b> Long overflow may occur during addition.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix1 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix matrix2 = LongMatrix.of(new long[][] {{5L, 6L}, {7L, 8L}});
     * matrix1.add(matrix2).flatten().toArray();   // returns [6L, 8L, 10L, 12L]
     *
     * LongMatrix big = LongMatrix.of(new long[][] {{Long.MAX_VALUE}});
     * big.add(LongMatrix.of(new long[][] {{1L}})).get(0, 0);   // returns Long.MIN_VALUE (overflow wraps)
     *
     * LongMatrix wrongShape = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * matrix1.add(wrongShape);            // throws IllegalArgumentException (shape mismatch)
     * matrix1.add((LongMatrix) null);     // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to add to this matrix; must not be {@code null}
     * @return a new {@code LongMatrix} containing the element-wise sum
     * @throws IllegalArgumentException if {@code other} is {@code null} or the matrices don't have the same shape
     */
    public LongMatrix add(final LongMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot add matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final long[][] otherArray = other.a;
        final long[][] result = new long[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> cmd = (i, j) -> result[i][j] = a[i][j] + otherArray[i][j];

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return LongMatrix.of(result);
    }

    /**
     * Performs element-wise subtraction of another matrix from this matrix.
     * The two matrices must have the same dimensions.
     * <p><b>Note:</b> Long overflow may occur during subtraction.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix1 = LongMatrix.of(new long[][] {{5L, 6L}, {7L, 8L}});
     * LongMatrix matrix2 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix1.subtract(matrix2).flatten().toArray();   // returns [4L, 4L, 4L, 4L]
     *
     * LongMatrix small = LongMatrix.of(new long[][] {{Long.MIN_VALUE}});
     * small.subtract(LongMatrix.of(new long[][] {{1L}})).get(0, 0);   // returns Long.MAX_VALUE (underflow wraps)
     *
     * LongMatrix wrongShape = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * matrix1.subtract(wrongShape);            // throws IllegalArgumentException (shape mismatch)
     * matrix1.subtract((LongMatrix) null);     // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to subtract from this matrix; must not be {@code null}
     * @return a new {@code LongMatrix} containing the element-wise difference
     * @throws IllegalArgumentException if {@code other} is {@code null} or the matrices don't have the same shape
     */
    public LongMatrix subtract(final LongMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot subtract matrices with different shapes: this is {}x{} but other is {}x{}", rowCount,
                columnCount, other.rowCount, other.columnCount);

        final long[][] otherArray = other.a;
        final long[][] result = new long[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> cmd = (i, j) -> result[i][j] = a[i][j] - otherArray[i][j];

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return LongMatrix.of(result);
    }

    /**
     * Performs matrix multiplication (Cayley product) with another matrix.
     * The number of columns in this matrix must equal the number of rows in the specified matrix.
     * The result is a new matrix with dimensions (this.rowCount × other.columnCount).
     * This implements standard matrix multiplication where each element (i,j) of the result is the
     * dot product of row i from this matrix and column j from the other matrix.
     * <p><b>Note:</b> This is the linear-algebra matrix product, not element-wise multiplication.
     * For element-wise multiplication use
     * {@link #zipWith(LongMatrix, com.landawn.abacus.util.Throwables.LongBinaryOperator)}.
     * Long overflow may occur during multiplication.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix1 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix matrix2 = LongMatrix.of(new long[][] {{5L, 6L}, {7L, 8L}});
     * matrix1.matmul(matrix2).flatten().toArray();   // returns [19L, 22L, 43L, 50L]
     *
     * LongMatrix a = LongMatrix.of(new long[][] {{1L, 2L, 3L}});       // 1x3
     * LongMatrix b = LongMatrix.of(new long[][] {{1L}, {2L}, {3L}});   // 3x1
     * a.matmul(b).get(0, 0);                                           // returns 14L (1*1 + 2*2 + 3*3)
     *
     * LongMatrix bad = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * matrix1.matmul(bad);               // throws IllegalArgumentException (this.columnCount != other.rowCount)
     * matrix1.matmul((LongMatrix) null); // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to multiply with this matrix; must not be {@code null}
     * @return a new {@code LongMatrix} containing the matrix product
     * @throws IllegalArgumentException if {@code other} is {@code null}, if the matrix dimensions are
     *         incompatible ({@code this.columnCount != other.rowCount}), or if this matrix has zero rows
     *         while {@code other} has a non-zero column count (the resulting shape is not representable)
     */
    public LongMatrix matmul(final LongMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.rowCount,
                "Matrix dimensions incompatible for multiplication: this is {}x{}, other is {}x{} (this.columnCount must equal other.rowCount)", rowCount,
                columnCount, other.rowCount, other.columnCount);

        checkRepresentableShape(rowCount, other.columnCount);

        final long[][] otherArray = other.a;
        final long[][] result = new long[rowCount][other.columnCount];
        final Throwables.IntTriConsumer<RuntimeException> cmd = (i, j, k) -> result[i][j] += a[i][k] * otherArray[k][j];

        Matrices.forEachCartesianIndices(this, other, cmd);

        return LongMatrix.of(result);
    }

    /**
     * Converts this primitive long matrix to a boxed {@code Matrix<Long>}.
     * Each primitive long value is boxed into a {@code Long} wrapper object.
     * This is the inverse operation of {@link #unbox(Matrix)}.
     *
     * <p><b>Note:</b> Boxing creates wrapper objects which have additional memory overhead compared to primitives.
     * Use this method only when you need to work with generic Matrix API or when {@code null} values are required.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix primitive = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * Matrix<Long> boxed = primitive.boxed();
     * boxed.get(0, 0);            // returns Long.valueOf(1L)
     * boxed.get(1, 1);            // returns Long.valueOf(4L)
     * boxed.rowCount();           // returns 2
     *
     * LongMatrix.unbox(boxed).equals(primitive);   // returns true (round-trip)
     * LongMatrix.empty().boxed().isEmpty();        // returns true
     * }</pre>
     *
     * @return a new {@code Matrix<Long>} containing boxed values
     * @see #unbox(Matrix)
     */
    public Matrix<Long> boxed() {
        final Long[][] c = new Long[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final long[] aa = a[i];
                final Long[] cc = c[i];

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
     * Converts this long matrix to an int matrix.
     * Each long value is narrowed to int by a Java primitive narrowing cast, which discards
     * all but the low-order 32 bits (per JLS §5.1.3).
     *
     * <p><b>Warning:</b> This is a narrowing conversion that may lose information.
     * Values outside the int range ({@code Integer.MIN_VALUE} to {@code Integer.MAX_VALUE})
     * wrap around modulo 2^32 rather than being clamped; the resulting int may have a
     * different sign than the original long (e.g. {@code (int) Long.MAX_VALUE} is {@code -1}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix longMatrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * IntMatrix intMatrix = longMatrix.toIntMatrix();
     * intMatrix.get(0, 0);             // returns 1
     * intMatrix.get(1, 1);             // returns 4
     *
     * LongMatrix big = LongMatrix.of(new long[][] {{Long.MAX_VALUE}});
     * big.toIntMatrix().get(0, 0);                  // returns -1 (narrowing keeps low 32 bits)
     * LongMatrix.empty().toIntMatrix().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new {@code IntMatrix} with values converted from long to int
     * @see #mapToInt(Throwables.LongToIntFunction)
     */
    public IntMatrix toIntMatrix() {
        final int[][] c = new int[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final long[] aa = a[i];
                final int[] cc = c[i];

                for (int j = 0; j < columnCount; j++) {
                    cc[j] = (int) aa[j];
                }
            }
        } else {
            for (int j = 0; j < columnCount; j++) {
                for (int i = 0; i < rowCount; i++) {
                    c[i][j] = (int) a[i][j];
                }
            }
        }

        return new IntMatrix(c);
    }

    /**
     * Converts this long matrix to a float matrix.
     * Each long value is converted to a float value.
     *
     * <p><b>Warning:</b> This conversion may lose precision. The float type has only 24 bits
     * of precision in its mantissa, so long values with absolute values greater than 2^24 (16,777,216)
     * may not be represented exactly. For example, {@code 16777217L} becomes {@code 16777216.0f}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix longMatrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * FloatMatrix floatMatrix = longMatrix.toFloatMatrix();
     * floatMatrix.get(0, 0);             // returns 1.0f
     * floatMatrix.get(1, 1);             // returns 4.0f
     *
     * LongMatrix large = LongMatrix.of(new long[][] {{16777217L}});
     * large.toFloatMatrix().get(0, 0);                // returns 16777216.0f (precision loss above 2^24)
     * LongMatrix.empty().toFloatMatrix().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new {@code FloatMatrix} with values converted from long to float
     */
    public FloatMatrix toFloatMatrix() {
        final float[][] c = new float[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final long[] aa = a[i];
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
     * Converts this long matrix to a double matrix.
     * Each long value is promoted to a double value.
     * <p><b>Note:</b> Very large long values (with absolute value greater than 2^53)
     * may lose precision when converted to double, since double has only 53 bits of precision
     * in its mantissa. For example, {@code Long.MAX_VALUE} (9223372036854775807L) cannot be
     * exactly represented as a double.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix longMatrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * DoubleMatrix doubleMatrix = longMatrix.toDoubleMatrix();
     * doubleMatrix.get(0, 0);             // returns 1.0
     * doubleMatrix.get(1, 1);             // returns 4.0
     *
     * doubleMatrix.rowCount();                         // returns 2 (same dimensions)
     * LongMatrix.empty().toDoubleMatrix().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new {@code DoubleMatrix} with values converted from long to double
     * @see #mapToDouble(Throwables.LongToDoubleFunction)
     */
    public DoubleMatrix toDoubleMatrix() {
        return DoubleMatrix.from(a);
    }

    /**
     * Applies a binary operation element-wise to this matrix and another matrix.
     * The two matrices must have the same dimensions (same number of rows and columns).
     * For each position (i, j), the result contains {@code zipFunction.applyAsLong(this.get(i,j), other.get(i,j))}.
     * The operation may be performed in parallel for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix1 = LongMatrix.of(new long[][] {{1L, 6L}, {3L, 8L}});
     * LongMatrix matrix2 = LongMatrix.of(new long[][] {{5L, 2L}, {7L, 4L}});
     * matrix1.zipWith(matrix2, Math::max).flatten().toArray();         // returns [5L, 6L, 7L, 8L]
     * matrix1.zipWith(matrix2, (x, y) -> x + y).flatten().toArray();   // returns [6L, 8L, 10L, 12L]
     *
     * LongMatrix wrongShape = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * matrix1.zipWith(wrongShape, Math::max);   // throws IllegalArgumentException (shape mismatch)
     * matrix1.zipWith(matrix2, null);           // throws IllegalArgumentException (zipFunction is null)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix to zip with this matrix; must have the same dimensions
     * @param zipFunction the binary operation to apply to corresponding elements from this and {@code other}
     * @return a new {@code LongMatrix} with the results of the zip operation
     * @throws IllegalArgumentException if the matrices don't have the same shape, or if {@code zipFunction} is {@code null}
     * @throws E if the zip function throws an exception
     */
    public <E extends Exception> LongMatrix zipWith(final LongMatrix other, final Throwables.LongBinaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);
        N.checkArgNotNull(zipFunction, "zipFunction");

        final long[][] b = other.a;
        final long[][] result = new long[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> cmd = (i, j) -> result[i][j] = zipFunction.applyAsLong(a[i][j], b[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return LongMatrix.of(result);
    }

    /**
     * Applies a ternary operation element-wise to this matrix and two other matrices.
     * All three matrices must have the same dimensions (same number of rows and columns).
     * The function receives corresponding elements from all three matrices at each position.
     * For each position (i, j), the result contains {@code zipFunction.applyAsLong(this.get(i,j), other.get(i,j), third.get(i,j))}.
     * The operation may be performed in parallel for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix1 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix matrix2 = LongMatrix.of(new long[][] {{5L, 6L}, {7L, 8L}});
     * LongMatrix matrix3 = LongMatrix.of(new long[][] {{9L, 10L}, {11L, 12L}});
     * matrix1.zipWith(matrix2, matrix3, (a, b, c) -> (a + b + c) / 3).flatten().toArray();
     * // returns [5L, 6L, 7L, 8L]
     * matrix1.zipWith(matrix2, matrix3, (a, b, c) -> a + b + c).flatten().toArray();
     * // returns [15L, 18L, 21L, 24L]
     *
     * LongMatrix wrongShape = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * matrix1.zipWith(matrix2, wrongShape, (a, b, c) -> a);   // throws IllegalArgumentException (shape mismatch)
     * matrix1.zipWith(matrix2, matrix3, null);                // throws IllegalArgumentException (zipFunction is null)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix to zip with; must have the same dimensions as this matrix
     * @param third the third matrix to zip with; must have the same dimensions as this matrix
     * @param zipFunction the ternary operation to apply to corresponding elements from this, {@code other}, and {@code third}
     * @return a new {@code LongMatrix} with the results of the zip operation
     * @throws IllegalArgumentException if the matrices don't have the same shape, or if {@code zipFunction} is {@code null}
     * @throws E if the zip function throws an exception
     */
    public <E extends Exception> LongMatrix zipWith(final LongMatrix other, final LongMatrix third, final Throwables.LongTernaryOperator<E> zipFunction)
            throws E {
        N.checkArgument(isSameShape(other) && isSameShape(third), "Cannot zip matrices with different shapes: all matrices must be {}x{}", rowCount,
                columnCount);
        N.checkArgNotNull(zipFunction, "zipFunction");

        final long[][] b = other.a;
        final long[][] c = third.a;
        final long[][] result = new long[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> cmd = (i, j) -> result[i][j] = zipFunction.applyAsLong(a[i][j], b[i][j], c[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return LongMatrix.of(result);
    }

    /**
     * Returns a stream of elements on the diagonal from upper-left to lower-right.
     * The matrix must be square (same number of rows and columns).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * matrix.mainDiagonalStream().toArray();   // returns [1L, 5L, 9L]
     * matrix.mainDiagonalStream().sum();       // returns 15L
     *
     * LongMatrix.empty().mainDiagonalStream().count();   // returns 0 (empty stream)
     * LongMatrix rect = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * rect.mainDiagonalStream();               // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a stream of diagonal elements from upper-left to lower-right,
     *         or an empty stream if the matrix is empty
     * @throws IllegalStateException if the matrix is not square
     */
    @Override
    public LongStream mainDiagonalStream() {
        checkIsSquare();

        if (isEmpty()) {
            return LongStream.empty();
        }

        return LongStream.of(new LongIteratorEx() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public long nextLong() {
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
     * The matrix must be square (same number of rows and columns).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * matrix.antiDiagonalStream().toArray();   // returns [3L, 5L, 7L]
     * matrix.antiDiagonalStream().sum();       // returns 15L
     *
     * LongMatrix.empty().antiDiagonalStream().count();   // returns 0 (empty stream)
     * LongMatrix rect = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * rect.antiDiagonalStream();               // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a stream of diagonal elements from upper-right to lower-left,
     *         or an empty stream if the matrix is empty
     * @throws IllegalStateException if the matrix is not square
     */
    @Override
    public LongStream antiDiagonalStream() {
        checkIsSquare();

        if (isEmpty()) {
            return LongStream.empty();
        }

        return LongStream.of(new LongIteratorEx() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public long nextLong() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final long result = a[cursor][columnCount - cursor - 1];
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
     * The stream includes all elements from all rows, proceeding from left to right within each row,
     * and from the first row to the last row.
     *
     * <p>This method is useful for processing all matrix elements sequentially. The returned
     * stream can be used with all standard LongStream operations including sum, average, filter, map, etc.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.horizontalStream().toArray();   // returns [1L, 2L, 3L, 4L, 5L, 6L]
     * matrix.horizontalStream().sum();       // returns 21L
     *
     * matrix.horizontalStream().filter(x -> x > 3L).count();   // returns 3
     * LongMatrix.empty().horizontalStream().count();           // returns 0 (empty stream)
     * }</pre>
     *
     * @return a stream of all matrix elements in row-major order, or an empty stream if the matrix is empty
     */
    @Override
    public LongStream horizontalStream() {
        return horizontalStream(0, rowCount);
    }

    /**
     * Returns a stream of elements from a single row.
     * The elements are streamed from left to right within the specified row.
     *
     * <p>This method is particularly useful when you need to process or analyze
     * a specific row of the matrix independently. The returned stream can be
     * used with all standard LongStream operations.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.horizontalStream(1).toArray();   // returns [4L, 5L, 6L]
     * matrix.horizontalStream(1).sum();       // returns 15L
     * matrix.horizontalStream(0).toArray();   // returns [1L, 2L, 3L]
     *
     * matrix.horizontalStream(5);             // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the index of the row to stream (0-based)
     * @return a stream of elements from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    @Override
    public LongStream horizontalStream(final int rowIndex) {
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}, {5L, 6L}});
     * matrix.horizontalStream(1, 3).toArray();   // returns [3L, 4L, 5L, 6L]
     * matrix.horizontalStream(0, 2).toArray();   // returns [1L, 2L, 3L, 4L]
     *
     * matrix.horizontalStream(1, 1).count();     // returns 0 (empty range)
     * matrix.horizontalStream(0, 5);             // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a stream of elements from the specified row range, or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public LongStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        if (isEmpty()) {
            return LongStream.empty();
        }

        return LongStream.of(new LongIteratorEx() {
            private int i = fromRowIndex;
            private int j = 0;

            @Override
            public boolean hasNext() {
                return i < toRowIndex;
            }

            @Override
            public long nextLong() {
                if (i >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final long result = a[i][j++];

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
            public long[] toArray() {
                final int len = toArrayLength(count());
                final long[] c = new long[len];

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
     * Creates a stream of all elements in the matrix in column-major order (vertically).
     * Elements are streamed column by column from top to bottom.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.verticalStream().toArray();   // returns [1L, 4L, 2L, 5L, 3L, 6L]
     * matrix.verticalStream().sum();       // returns 21L
     *
     * matrix.verticalStream().count();               // returns 6
     * LongMatrix.empty().verticalStream().count();   // returns 0 (empty stream)
     * }</pre>
     *
     * @return a stream of all matrix elements in column-major order, or an empty stream if the matrix is empty
     */
    @Override
    @Beta
    public LongStream verticalStream() {
        return verticalStream(0, columnCount);
    }

    /**
     * Creates a stream of elements from a specific column.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.verticalStream(1).toArray();   // returns [2L, 5L]
     * matrix.verticalStream(0).toArray();   // returns [1L, 4L]
     * matrix.verticalStream(1).sum();       // returns 7L
     *
     * matrix.verticalStream(9);             // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param columnIndex the index of the column to stream (0-based)
     * @return a stream of elements from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    @Override
    public LongStream verticalStream(final int columnIndex) {
        return verticalStream(columnIndex, columnIndex + 1);
    }

    /**
     * Creates a stream of elements from a range of columns in column-major order.
     * Elements are streamed column by column from top to bottom.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.verticalStream(1, 3).toArray();   // returns [2L, 5L, 3L, 6L]
     * matrix.verticalStream(0, 1).toArray();   // returns [1L, 4L]
     *
     * matrix.verticalStream(1, 1).count();     // returns 0 (empty range)
     * matrix.verticalStream(0, 9);             // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a stream of elements from the specified column range, or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    @Beta
    public LongStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        if (isEmpty()) {
            return LongStream.empty();
        }

        return LongStream.of(new LongIteratorEx() {
            private int i = 0;
            private int j = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return j < toColumnIndex;
            }

            @Override
            public long nextLong() {
                if (j >= toColumnIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final long result = a[i++][j];

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

                if (n >= (long) (toColumnIndex - j) * LongMatrix.this.rowCount - i) {
                    i = 0;
                    j = toColumnIndex;
                } else {
                    final long offset = n + i;
                    i = (int) (offset % LongMatrix.this.rowCount);
                    j += (int) (offset / LongMatrix.this.rowCount);
                }
            }

            @Override
            public long count() {
                return (long) (toColumnIndex - j) * rowCount - i; // NOSONAR
            }

            @Override
            public long[] toArray() {
                final int len = toArrayLength(count());
                final long[] c = new long[len];

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
     * Creates a stream of row streams, where each element is a stream of a complete row.
     * Rows are streamed in order from top to bottom.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.rowStreams().count();                                 // returns 2 (one stream per row)
     * matrix.rowStreams().toList().get(0).toArray();               // returns [1L, 2L, 3L]
     * matrix.rowStreams().toList().get(1).toArray();               // returns [4L, 5L, 6L]
     *
     * matrix.rowStreams().mapToLong(LongStream::sum).toArray();    // returns [6L, 15L]
     * LongMatrix.empty().rowStreams().count();                     // returns 0
     * }</pre>
     *
     * @return a stream of row streams
     */
    @Override
    public Stream<LongStream> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Creates a stream of row streams from a range of rows.
     * Each element in the returned stream is a stream of a complete row.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}, {5L, 6L}});
     * matrix.rowStreams(1, 3).count();                   // returns 2
     * matrix.rowStreams(1, 3).toList().get(0).toArray(); // returns [3L, 4L]
     * matrix.rowStreams(1, 3).toList().get(1).toArray(); // returns [5L, 6L]
     *
     * matrix.rowStreams(1, 1).count();                   // returns 0 (empty range)
     * matrix.rowStreams(0, 5);                           // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a stream of row streams from the specified range
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public Stream<LongStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public LongStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return LongStream.of(a[cursor++]);
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
     * Creates a stream of column streams, where each element is a stream of a complete column.
     * Columns are streamed in order from left to right.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.columnStreams().count();                       // returns 3 (one stream per column)
     * matrix.columnStreams().toList().get(0).toArray();     // returns [1L, 4L]
     * matrix.columnStreams().toList().get(2).toArray();     // returns [3L, 6L]
     *
     * matrix.columnStreams().mapToLong(LongStream::sum).toArray();   // returns [5L, 7L, 9L]
     * LongMatrix.empty().columnStreams().count();                    // returns 0
     * }</pre>
     *
     * @return a stream of column streams
     */
    @Override
    @Beta
    public Stream<LongStream> columnStreams() {
        return columnStreams(0, columnCount);
    }

    /**
     * Creates a stream of column streams from a range of columns.
     * Each element in the returned stream is a stream of a complete column.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.columnStreams(1, 3).count();                   // returns 2
     * matrix.columnStreams(1, 3).toList().get(0).toArray(); // returns [2L, 5L]
     * matrix.columnStreams(1, 3).toList().get(1).toArray(); // returns [3L, 6L]
     *
     * matrix.columnStreams(1, 1).count();                   // returns 0 (empty range)
     * matrix.columnStreams(0, 9);                           // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a stream of column streams from the specified range
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    @Beta
    public Stream<LongStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
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
            public LongStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return LongStream.of(new LongIteratorEx() {
                    private final int columnIndex = cursor++;
                    private final int toIndex2 = rowCount;
                    private int cursor2 = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor2 < toIndex2;
                    }

                    @Override
                    public long nextLong() {
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
     * This is an internal helper method used by the abstract matrix framework to determine
     * the length of a row array. It is part of the template method pattern implementation
     * in the abstract base class.
     *
     * @param a the array to get the length of, may be {@code null}
     * @return the length of the array, or 0 if the array is {@code null}
     */
    @Override
    protected int length(@SuppressWarnings("hiding") final long[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Performs the specified action for each element in this matrix.
     * Elements are processed in row-major order (row by row, left to right) when executed
     * sequentially; the operation may be performed in parallel for large matrices.
     * This is equivalent to calling {@link #forEach(int, int, int, int, Throwables.LongConsumer)}
     * with the full matrix bounds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * long[] sum = {0L};
     * matrix.forEach(value -> sum[0] += value);
     * // sum[0] == 10L (1 + 2 + 3 + 4)
     *
     * int[] count = {0};
     * LongMatrix.empty().forEach(value -> count[0]++);                  // action never invoked, count[0] stays 0
     * matrix.forEach((Throwables.LongConsumer<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to apply to each element; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final Throwables.LongConsumer<E> action) throws E {
        forEach(0, rowCount, 0, columnCount, action);
    }

    /**
     * Performs the specified action for each element in a sub-region of this matrix.
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * long[] sum = {0L};
     * matrix.forEach(1, 3, 1, 3, value -> sum[0] += value);
     * // sum[0] == 28L (5 + 6 + 8 + 9, the rows 1-2 x columns 1-2 sub-region)
     *
     * matrix.forEach(0, 0, 0, 3, value -> sum[0]++);   // empty row range, action never invoked
     * matrix.forEach(0, 5, 0, 3, value -> {});         // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.forEach(0, 3, 0, 3, null);                // throws IllegalArgumentException (action is null)
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @param action the action to apply to each element in the specified region; must not be {@code null}
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount}, or if either
     *         {@code from} index exceeds its corresponding {@code to} index
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex,
            final Throwables.LongConsumer<E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        if (Matrices.isParallelizable(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> cmd = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, cmd, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                final long[] aa = a[i];

                for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                    action.accept(aa[j]);
                }
            }
        }
    }

    /**
     * Prints this matrix to standard output and returns the formatted string.
     * Each row is printed on a separate line with elements separated by commas
     * and enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.println();   // returns "[1, 2, 3]\n[4, 5, 6]" (rows joined by a newline; also printed to stdout)
     *
     * LongMatrix single = LongMatrix.of(new long[][] {{7L}});
     * single.println();               // returns "[7]"
     * LongMatrix.empty().println();   // returns "[]"
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

                    final long[] row = a[i];
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
     * LongMatrix m1 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix m2 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * m1.hashCode() == m2.hashCode();   // returns true (equal matrices share a hash code)
     *
     * LongMatrix m3 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 9L}});
     * m1.hashCode() == m3.hashCode();   // typically returns false (different contents)
     * LongMatrix.empty().hashCode();    // returns a stable value for the empty matrix
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
     * Returns {@code true} if the given object is also a LongMatrix with the same dimensions
     * and all corresponding elements are equal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix m1 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix m2 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * m1.equals(m2);                                  // returns true (same contents)
     * m1.equals(m1);                                  // returns true (same reference)
     *
     * m1.equals(LongMatrix.of(new long[][] {{1L, 2L}}));   // returns false (different shape)
     * m1.equals((Object) "not a matrix");                  // returns false (different type)
     * m1.equals(null);                                     // returns false
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

        if (obj instanceof final LongMatrix another) {
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.toString();   // returns "[[1, 2], [3, 4]]"
     *
     * LongMatrix single = LongMatrix.of(new long[][] {{7L}});
     * single.toString();               // returns "[[7]]"
     * LongMatrix.empty().toString();   // returns "[]"
     * }</pre>
     *
     * @return a string representation of this matrix
     */
    @Override
    public String toString() {
        return N.deepToString(a);
    }
}
