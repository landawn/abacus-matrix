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
 * validated backing array. Constructors and {@link #of(long[]...)} generally wrap the supplied storage
 * directly, while factories, conversions, and mapping operations allocate new arrays.</p>
 *
 * <p>Cells introduced by growth or reshaping default to {@code 0L} unless an overload accepts an
 * explicit fill value. Arithmetic operations (e.g. {@link #add(LongMatrix)}, {@link #subtract(LongMatrix)},
 * {@link #matrixMultiply(LongMatrix)}) follow standard Java {@code long} semantics: overflow silently wraps
 * around modulo 2<sup>64</sup>.</p>
 *
 * <p><b>Aggregations:</b> this class does not provide dedicated reduction methods such as
 * {@code sum()}, {@code min()}, {@code max()} or {@code average()}. Compute such aggregations
 * through the streaming API instead &mdash; for example {@code rowMajorStream().sum()} over all
 * elements, or {@code rowStreams()} / {@code columnStreams()} for per-row or per-column reductions.</p>
 *
 * @see IntMatrix
 * @see DoubleMatrix
 * @see FloatMatrix
 * @see ShortMatrix
 * @see ByteMatrix
 * @see CharMatrix
 * @see BooleanMatrix
 * @see Matrix
 */
public final class LongMatrix extends AbstractMatrix<long[], LongList, LongStream, Stream<LongStream>, LongMatrix> {

    private static final LongMatrix EMPTY_LONG_MATRIX = new LongMatrix(new long[0][0]);

    /**
     * Constructs a {@code LongMatrix} backed by the supplied two-dimensional array.
     *
     * <p>The supplied array is used directly after rectangular-shape validation, so later modifications to either the input
     * array or the matrix remain visible through the other view. Call {@link #copy()} if you need an
     * independently owned matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[][] data = {{1L, 2L, 3L}, {4L, 5L, 6L}};
     * LongMatrix matrix = new LongMatrix(data);
     * matrix.get(0, 0);                       // returns 1L
     * data[0][0] = 10L;
     * matrix.get(0, 0);                       // returns 10L (backing array is shared)
     *
     * new LongMatrix(null);               // throws IllegalArgumentException
     * new LongMatrix(new long[][] {{1L}, {2L, 3L}}); // throws IllegalArgumentException (non-rectangular)
     * }</pre>
     *
     * @param a the two-dimensional long array to wrap, must not be {@code null}
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public LongMatrix(final long[][] a) {
        super(N.checkArgNotNull(a, "Matrix array cannot be null"), long.class);
    }

    /**
     * Returns the shared empty {@code 0x0} matrix instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.empty();
     * matrix.rowCount();                          // returns 0
     * matrix.columnCount();                       // returns 0
     * matrix.isEmpty();                           // returns true
     * LongMatrix.empty() == LongMatrix.empty();   // true (shared singleton)
     * }</pre>
     *
     * @return the canonical empty {@code LongMatrix} (singleton)
     */
    public static LongMatrix empty() {
        return EMPTY_LONG_MATRIX;
    }

    /**
     * Creates a {@code LongMatrix} from a two-dimensional long array.
     *
     * <p><b>Important:</b> The provided array is used directly without defensive copying.
     * Changes to the input array are reflected in the returned matrix, and vice versa.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.get(0, 1);                       // returns 2L
     * matrix.rowCount();                      // returns 2
     *
     * LongMatrix.of((long[][]) null);     // throws IllegalArgumentException
     * LongMatrix.of().isEmpty();                    // returns true (no rows)
     * LongMatrix.of(new long[][] {{1L, 2L}, {3L}}); // throws IllegalArgumentException (non-rectangular)
     * }</pre>
     *
     * @param a the two-dimensional long array to wrap, or empty for an empty matrix; must not be {@code null}
     * @return a new {@code LongMatrix} backed by {@code a}, or the shared empty matrix if {@code a} is empty
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public static LongMatrix of(final long[]... a) {
        N.checkArgNotNull(a, "Matrix array cannot be null");
        return a.length == 0 ? EMPTY_LONG_MATRIX : new LongMatrix(a);
    }

    /**
     * Creates a {@code LongMatrix} that owns a defensive deep copy of the supplied two-dimensional array.
     *
     * <p>Unlike {@link #of(long[][])}, which wraps the caller's array without copying, this factory clones
     * every row into a freshly-allocated backing array. Subsequent modifications to {@code a} (or its rows)
     * are therefore <b>not</b> visible through the returned matrix, and vice versa.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[][] data = {{1, 2}, {3, 4}};
     * LongMatrix matrix = LongMatrix.copyOf(data);
     * data[0][0] = 10;
     * matrix.get(0, 0);                       // returns 1 (copy is independent)
     *
     * LongMatrix.copyOf((long[][]) null);  // throws IllegalArgumentException
     * LongMatrix.copyOf(new long[][] {{1, 2}, {3}}); // throws IllegalArgumentException (non-rectangular)
     * }</pre>
     *
     * @param a the two-dimensional long array to copy, or empty for an empty matrix; must not be {@code null}
     * @return a new {@code LongMatrix} backed by a deep copy of {@code a}, or the shared empty matrix if {@code a} is empty
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     * @see #of(long[][])
     * @see #copy()
     */
    public static LongMatrix copyOf(final long[]... a) {
        N.checkArgNotNull(a, "Matrix array cannot be null");

        if (a.length == 0) {
            return EMPTY_LONG_MATRIX;
        }

        final long[][] c = new long[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            c[i] = a[i] == null ? null : a[i].clone();
        }

        return new LongMatrix(c);
    }

    /**
     * Creates a {@code LongMatrix} from a two-dimensional {@code int} array by widening each
     * {@code int} to {@code long} (the conversion preserves the exact numeric value, with no data loss).
     *
     * <p>All rows must have the same length as the first row (rectangular array required). The
     * returned matrix owns a freshly-allocated backing array; modifications to {@code a} after
     * construction do not affect it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.from(new int[][] {{1, 2}, {3, 4}});
     * matrix.get(0, 0);                       // returns 1L (int 1 widened to long)
     * matrix.get(1, 1);                       // returns 4L
     *
     * LongMatrix.from((int[][]) null);                 // throws IllegalArgumentException
     * LongMatrix.from(new int[][] {{1}, {2, 3}});      // throws IllegalArgumentException (jagged rows)
     * }</pre>
     *
     * @param a the two-dimensional int array to convert, or empty for an empty matrix; must not be {@code null}
     * @return a new {@code LongMatrix} with the widened values, or the shared empty matrix if {@code a} is empty
     * @throws IllegalArgumentException if the first row is {@code null}, or if any other row is {@code null}
     *         or has a length different from the first row
     * @see IntMatrix#toLongMatrix()
     */
    public static LongMatrix from(final int[]... a) {
        N.checkArgNotNull(a, "Matrix array cannot be null");

        if (a.length == 0) {
            return EMPTY_LONG_MATRIX;
        }

        N.checkArgument(a[0] != null, "Row 0 cannot be null");

        final int columnCount = a[0].length;

        // Validate all rows have the same length
        for (int i = 1; i < a.length; i++) {
            N.checkArgument(a[i] != null, "Row {} cannot be null", i);
            N.checkArgument(a[i].length == columnCount, MSG_NOT_RECTANGULAR, columnCount, i, a[i].length);
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
     * Creates a new {@code 1 x length} matrix filled with pseudo-random {@code long} values
     * drawn uniformly from the entire {@code long} range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.randomRow(5);
     * matrix.rowCount();          // returns 1
     * matrix.columnCount();       // returns 5
     *
     * LongMatrix.randomRow(0).columnCount();   // returns 0 (1x0 matrix)
     * LongMatrix.randomRow(-1);                // throws IllegalArgumentException (negative length)
     * }</pre>
     *
     * @param length the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code LongMatrix} of dimensions {@code 1 x length} filled with random values
     * @throws IllegalArgumentException if {@code length} is negative
     * @see #random(int, int)
     */
    public static LongMatrix randomRow(final int length) {
        N.checkArgument(length >= 0, MSG_NEGATIVE_DIMENSION, "length", length);

        return random(1, length);
    }

    /**
     * Creates a new matrix of the specified dimensions filled with pseudo-random {@code long} values
     * drawn uniformly from the entire {@code long} range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.random(2, 3);
     * matrix.rowCount();          // returns 2
     * matrix.columnCount();       // returns 3
     *
     * LongMatrix.random(0, 0).isEmpty();    // returns true
     * LongMatrix.random(2, -1);             // throws IllegalArgumentException (negative columnCount)
     * LongMatrix.random(-1, 2);             // throws IllegalArgumentException (negative rowCount)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code LongMatrix} of dimensions {@code rowCount x columnCount} filled with random values
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
     * Creates a new matrix of the specified dimensions where every cell holds {@code element}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.repeat(2, 3, 1L);
     * matrix.get(0, 0);                    // returns 1L
     * matrix.get(1, 2);                    // returns 1L
     * // matrix is [[1, 1, 1], [1, 1, 1]]
     *
     * LongMatrix.repeat(0, 0, 5L).isEmpty(); // returns true
     * LongMatrix.repeat(-1, 3, 7L);          // throws IllegalArgumentException (negative rowCount)
     * LongMatrix.repeat(2, -1, 7L);          // throws IllegalArgumentException (negative columnCount)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @param element the long value to fill every cell with
     * @return a new {@code LongMatrix} of dimensions {@code rowCount x columnCount} filled with {@code element}
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
     * Creates a 1-row {@code LongMatrix} containing the half-open range
     * {@code [startInclusive, endExclusive)} with step {@code 1}.
     * If {@code startInclusive >= endExclusive}, a {@code 1x0} matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix.range(0L, 5L).columnCount();   // returns 5 -> [[0, 1, 2, 3, 4]]
     * LongMatrix.range(2L, 5L).get(0, 0);       // returns 2L -> [[2, 3, 4]]
     *
     * LongMatrix.range(5L, 0L).columnCount();   // returns 0 (start >= end, 1x0 matrix)
     * LongMatrix.range(3L, 3L).columnCount();   // returns 0 (empty half-open range)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new {@code 1xn} {@code LongMatrix} where {@code n = max(0, endExclusive - startInclusive)}
     */
    public static LongMatrix range(final long startInclusive, final long endExclusive) {
        return new LongMatrix(new long[][] { Array.range(startInclusive, endExclusive) });
    }

    /**
     * Creates a 1-row {@code LongMatrix} containing the half-open range
     * {@code [startInclusive, endExclusive)} stepped by {@code step}.
     * The step size can be positive (for ascending sequences) or negative (for descending sequences).
     * If the step direction does not advance from {@code startInclusive} toward {@code endExclusive},
     * a {@code 1x0} matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix.range(0L, 10L, 2L).columnCount();   // returns 5 -> [[0, 2, 4, 6, 8]]
     * LongMatrix.range(10L, 0L, -2L).get(0, 0);      // returns 10L -> [[10, 8, 6, 4, 2]]
     *
     * LongMatrix.range(0L, 10L, -1L).columnCount();  // returns 0 (step is wrong direction)
     * LongMatrix.range(0L, 10L, 0L);                 // throws IllegalArgumentException (step is zero)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param step the step size (must not be zero; positive for ascending, negative for descending)
     * @return a new {@code 1xn} {@code LongMatrix} of values from {@code startInclusive} stepped by {@code step}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static LongMatrix range(final long startInclusive, final long endExclusive, final long step) {
        return new LongMatrix(new long[][] { Array.range(startInclusive, endExclusive, step) });
    }

    /**
     * Creates a 1-row {@code LongMatrix} containing the closed range
     * {@code [startInclusive, endInclusive]} with step {@code 1}.
     * Unlike {@link #range(long, long)} this includes {@code endInclusive}.
     * If {@code startInclusive > endInclusive}, a {@code 1x0} matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix.rangeClosed(0L, 4L).columnCount();   // returns 5 -> [[0, 1, 2, 3, 4]]
     * LongMatrix.rangeClosed(5L, 5L).get(0, 0);       // returns 5L -> [[5]] (endpoint included)
     *
     * LongMatrix.rangeClosed(5L, 0L).columnCount();   // returns 0 (start > end, 1x0 matrix)
     * LongMatrix.rangeClosed(3L, 2L).columnCount();   // returns 0 (descending without step)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new {@code 1xn} {@code LongMatrix} where {@code n = max(0, endInclusive - startInclusive + 1)}
     */
    public static LongMatrix rangeClosed(final long startInclusive, final long endInclusive) {
        return new LongMatrix(new long[][] { Array.rangeClosed(startInclusive, endInclusive) });
    }

    /**
     * Creates a 1-row {@code LongMatrix} containing the closed range
     * {@code [startInclusive, endInclusive]} stepped by {@code step}.
     * The step size can be positive (for ascending sequences) or negative (for descending sequences).
     * {@code endInclusive} is included only if it is reachable from {@code startInclusive} via {@code step};
     * otherwise the last element is the reachable value nearest to {@code endInclusive} without stepping past it. If the step direction does not
     * advance toward {@code endInclusive}, a {@code 1x0} matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix.rangeClosed(0L, 8L, 2L).columnCount();    // returns 5 -> [[0, 2, 4, 6, 8]]
     * LongMatrix.rangeClosed(0L, 9L, 2L).columnCount();    // returns 5 -> [[0, 2, 4, 6, 8]] (9 not reachable)
     *
     * LongMatrix.rangeClosed(10L, 0L, -2L).get(0, 0);      // returns 10L -> [[10, 8, 6, 4, 2, 0]]
     * LongMatrix.rangeClosed(0L, 8L, 0L);                  // throws IllegalArgumentException (step is zero)
     * }</pre>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive, if reachable by stepping)
     * @param step the step size (must not be zero; positive for ascending, negative for descending)
     * @return a new {@code 1xn} {@code LongMatrix} of values from {@code startInclusive} stepped by {@code step}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static LongMatrix rangeClosed(final long startInclusive, final long endInclusive, final long step) {
        return new LongMatrix(new long[][] { Array.rangeClosed(startInclusive, endInclusive, step) });
    }

    /**
     * Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
     * All other elements are set to zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.mainDiagonal(new long[] {1L, 2L, 3L});
     * matrix.get(0, 0);                       // returns 1L
     * matrix.get(1, 1);                       // returns 2L
     * matrix.get(0, 1);                       // returns 0L (off-diagonal)
     * // matrix is [[1, 0, 0], [0, 2, 0], [0, 0, 3]]
     *
     * LongMatrix.mainDiagonal(null);            // throws IllegalArgumentException (null array)
     * LongMatrix.mainDiagonal(new long[0]).isEmpty();   // returns true
     * }</pre>
     *
     * @param mainDiagonal the array of main-diagonal elements; must not be {@code null}, but may be empty
     * @return a new {@code n x n} {@code LongMatrix} (where {@code n = mainDiagonal.length}) with
     *         the supplied values on the main diagonal and {@code 0} elsewhere; the shared empty
     *         matrix if {@code mainDiagonal} is empty
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null}
     * @see #antiDiagonal(long[])
     * @see #diagonals(long[], long[])
     */
    public static LongMatrix mainDiagonal(final long[] mainDiagonal) {
        N.checkArgNotNull(mainDiagonal, "mainDiagonal");

        return diagonals(mainDiagonal, null);
    }

    /**
     * Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
     * All other elements are set to zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.antiDiagonal(new long[] {1L, 2L, 3L});
     * matrix.get(0, 2);                       // returns 1L
     * matrix.get(2, 0);                       // returns 3L
     * matrix.get(0, 0);                       // returns 0L (off anti-diagonal)
     * // matrix is [[0, 0, 1], [0, 2, 0], [3, 0, 0]]
     *
     * LongMatrix.antiDiagonal(null);            // throws IllegalArgumentException (null array)
     * LongMatrix.antiDiagonal(new long[0]).isEmpty();   // returns true
     * }</pre>
     *
     * @param antiDiagonal the array of anti-diagonal elements; must not be {@code null}, but may be empty
     * @return a new {@code n x n} {@code LongMatrix} (where {@code n = antiDiagonal.length}) with
     *         the supplied values on the anti-diagonal and {@code 0} elsewhere; the shared empty
     *         matrix if {@code antiDiagonal} is empty
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null}
     * @see #mainDiagonal(long[])
     * @see #diagonals(long[], long[])
     */
    public static LongMatrix antiDiagonal(final long[] antiDiagonal) {
        N.checkArgNotNull(antiDiagonal, "antiDiagonal");

        return diagonals(null, antiDiagonal);
    }

    /**
     * Creates a square matrix from the specified main diagonal and anti-diagonal elements.
     * All other elements are set to zero. At least one array must be non-{@code null}; if both arrays
     * contain elements, they must have the same length.
     * The resulting matrix has dimensions {@code n x n}, where {@code n} is the length of the
     * non-empty diagonal array. If neither input contains any elements, the shared empty matrix is returned.
     * When both diagonals are provided and they overlap (at the center element of odd-sized matrices),
     * the main diagonal value takes precedence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.diagonals(new long[] {1L, 2L, 3L}, new long[] {4L, 5L, 6L});
     * matrix.get(0, 0);                       // returns 1L (main diagonal)
     * matrix.get(0, 2);                       // returns 4L (anti-diagonal)
     * matrix.get(1, 1);                       // returns 2L (overlap: main takes precedence)
     * // matrix is [[1, 0, 4], [0, 2, 0], [6, 0, 3]]
     *
     * LongMatrix.diagonals(null, null);                                   // throws IllegalArgumentException (both null)
     * LongMatrix.diagonals(new long[] {1L, 2L}, new long[] {3L, 4L, 5L}); // throws IllegalArgumentException (length mismatch)
     * }</pre>
     *
     * @param mainDiagonal the array of main-diagonal elements; may be {@code null} if {@code antiDiagonal} is non-{@code null};
     *        may be empty
     * @param antiDiagonal the array of anti-diagonal elements; may be {@code null} if {@code mainDiagonal} is non-{@code null};
     *        may be empty
     * @return a square matrix with the specified diagonals, or an empty matrix when both supplied diagonals are empty or one is {@code null} and the other is empty
     * @throws IllegalArgumentException if both {@code mainDiagonal} and {@code antiDiagonal} are {@code null}, or if both arrays are non-empty and have different lengths
     * @see #mainDiagonal(long[])
     * @see #antiDiagonal(long[])
     */
    public static LongMatrix diagonals(final long[] mainDiagonal, final long[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(mainDiagonal != null || antiDiagonal != null, "Both 'mainDiagonal' and 'antiDiagonal' can't be null");

        N.checkArgument(N.isEmpty(mainDiagonal) || N.isEmpty(antiDiagonal) || mainDiagonal.length == antiDiagonal.length,
                "The lengths of 'mainDiagonal' and 'antiDiagonal' must be the same: mainDiagonal length={}, antiDiagonal length={}", N.len(mainDiagonal),
                N.len(antiDiagonal));

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
     * Converts a boxed {@link Matrix Matrix&lt;Long&gt;} to a primitive {@code LongMatrix}.
     * {@code null} elements in the source are converted to {@code 0L}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Long> boxed = Matrix.of(new Long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix primitive = LongMatrix.unbox(boxed);
     * primitive.get(0, 1);                    // returns 2L
     * primitive.get(1, 0);                    // returns 3L
     *
     * Matrix<Long> withNull = Matrix.of(new Long[][] {{1L, null}});
     * LongMatrix.unbox(withNull).get(0, 1);     // returns 0L (null becomes 0L)
     * LongMatrix.unbox((Matrix<Long>) null);    // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the boxed {@code Long} matrix to convert; must not be {@code null}
     * @return a new {@code LongMatrix} with primitive long values
     * @throws IllegalArgumentException if {@code x} is {@code null}
     * @see #boxed()
     */
    public static LongMatrix unbox(final Matrix<Long> x) {
        N.checkArgNotNull(x, "x");

        return LongMatrix.of(Array.unbox(x.a));
    }

    /**
     * Returns the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.get(0, 1);                       // returns 2L
     * matrix.get(1, 0);                       // returns 3L
     *
     * matrix.get(2, 0);                       // throws ArrayIndexOutOfBoundsException (row out of range)
     * matrix.get(0, -1);                      // throws ArrayIndexOutOfBoundsException (column out of range)
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
     * matrix.get(Point.of(0, 1));             // returns 2L
     * matrix.get(Point.of(1, 1));             // returns 4L
     *
     * matrix.get((Point) null);               // throws IllegalArgumentException
     * matrix.get(Point.of(5, 0));             // throws ArrayIndexOutOfBoundsException (row out of range)
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
     * matrix.get(0, 1);                       // returns 9L (was 2L)
     * matrix.set(1, 0, -5L);
     * matrix.get(1, 0);                       // returns -5L
     *
     * matrix.set(2, 0, 7L);                   // throws ArrayIndexOutOfBoundsException (row out of range)
     * matrix.set(0, -1, 7L);                  // throws ArrayIndexOutOfBoundsException (column out of range)
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
     * matrix.get(0, 1);                       // returns 9L
     * matrix.set(Point.of(1, 1), 0L);
     * matrix.get(1, 1);                       // returns 0L
     *
     * matrix.set((Point) null, 1L);           // throws IllegalArgumentException
     * matrix.set(Point.of(5, 0), 1L);         // throws ArrayIndexOutOfBoundsException (row out of range)
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
     * Returns the element directly above the specified position, or an empty {@link OptionalLong}
     * if the position is on the top edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalLong} is returned for the top
     * row instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.valueAbove(1, 0).getAsLong();    // returns 1L
     * matrix.valueAbove(1, 1).getAsLong();    // returns 2L
     *
     * matrix.valueAbove(0, 0).isPresent();    // returns false (top row, no cell above)
     * matrix.valueAbove(2, 0);                // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalLong} containing the element at position {@code (rowIndex - 1, columnIndex)},
     *         or empty if {@code rowIndex == 0}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalLong valueAbove(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? OptionalLong.empty() : OptionalLong.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, or an empty {@link OptionalLong}
     * if the position is on the bottom edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalLong} is returned for the
     * bottom row instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.valueBelow(0, 0).getAsLong();    // returns 3L
     * matrix.valueBelow(0, 1).getAsLong();    // returns 4L
     *
     * matrix.valueBelow(1, 0).isPresent();    // returns false (bottom row, no cell below)
     * matrix.valueBelow(2, 0);                // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalLong} containing the element at position {@code (rowIndex + 1, columnIndex)},
     *         or empty if {@code rowIndex == rowCount - 1}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalLong valueBelow(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? OptionalLong.empty() : OptionalLong.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, or an empty
     * {@link OptionalLong} if the position is on the leftmost edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalLong} is returned for the
     * leftmost column instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.valueLeft(0, 1).getAsLong();     // returns 1L
     * matrix.valueLeft(1, 1).getAsLong();     // returns 3L
     *
     * matrix.valueLeft(0, 0).isPresent();     // returns false (leftmost column, no cell to the left)
     * matrix.valueLeft(0, 2);                 // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalLong} containing the element at position {@code (rowIndex, columnIndex - 1)},
     *         or empty if {@code columnIndex == 0}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalLong valueLeft(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? OptionalLong.empty() : OptionalLong.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, or an empty
     * {@link OptionalLong} if the position is on the rightmost edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalLong} is returned for the
     * rightmost column instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.valueRight(0, 0).getAsLong();    // returns 2L
     * matrix.valueRight(1, 0).getAsLong();    // returns 4L
     *
     * matrix.valueRight(0, 1).isPresent();    // returns false (rightmost column, no cell to the right)
     * matrix.valueRight(0, 2);                // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalLong} containing the element at position {@code (rowIndex, columnIndex + 1)},
     *         or empty if {@code columnIndex == columnCount - 1}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalLong valueRight(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? OptionalLong.empty() : OptionalLong.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as a live reference to the underlying {@code long[]} storage.
     *
     * <p><b>Note:</b> This method returns the internal array, not a copy. Modifications to the
     * returned array will affect the matrix and vice versa. Use {@link #rowCopy(int)} if you need
     * an independent copy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.rowView(0);                      // returns [1, 2, 3]
     * matrix.rowView(1);                      // returns [4, 5, 6]
     *
     * long[] firstRow = matrix.rowView(0);
     * firstRow[0] = 10L;
     * matrix.get(0, 0);                       // returns 10L (live view is shared)
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
    public long[] rowView(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return a[rowIndex];
    }

    /**
     * Returns a defensive copy of the specified row as a new {@code long[]}.
     * Changes to the returned array do not affect this matrix and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.rowCopy(0);                      // returns [1, 2, 3]
     * matrix.rowCopy(1);                      // returns [4, 5, 6]
     *
     * long[] firstRow = matrix.rowCopy(0);
     * firstRow[0] = 10L;
     * matrix.get(0, 0);                       // returns 1L (copy is independent)
     *
     * matrix.rowCopy(-1);                     // throws IndexOutOfBoundsException
     * matrix.rowCopy(2);                      // throws IndexOutOfBoundsException (rowIndex >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new long array of length {@code columnCount} containing the values of the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowView(int)
     * @see #columnCopy(int)
     */
    @Override
    public long[] rowCopy(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return N.copyOf(a[rowIndex], columnCount);
    }

    /**
     * Returns a defensive copy of the specified column as a new {@code long[]}.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously in memory. Modifications to the returned array
     * will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.columnCopy(0);                   // returns [1, 4]
     * matrix.columnCopy(2);                   // returns [3, 6]
     *
     * long[] firstColumn = matrix.columnCopy(0);
     * firstColumn[0] = 10L;
     * matrix.get(0, 0);                       // returns 1L (copy is independent)
     *
     * matrix.columnCopy(-1);                  // throws IndexOutOfBoundsException
     * matrix.columnCopy(3);                   // throws IndexOutOfBoundsException (columnIndex >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new long array of length {@code rowCount} containing the values of the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     * @see #rowCopy(int)
     * @see #rowView(int)
     */
    @Override
    public long[] columnCopy(final int columnIndex) throws IndexOutOfBoundsException {
        checkColumnIndex(columnIndex);

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
     * matrix.rowCopy(0);                      // returns [7, 8, 9]
     *
     * matrix.setRow(1, new long[] {0L, 0L, 0L});
     * matrix.get(1, 1);                       // returns 0L
     *
     * matrix.setRow(0, new long[] {1L, 2L});      // throws IllegalArgumentException (length != columnCount)
     * matrix.setRow(5, new long[] {1L, 2L, 3L});  // throws IndexOutOfBoundsException (rowIndex out of bounds)
     * }</pre>
     *
     * @param rowIndex the index of the row to set (0-based)
     * @param row the array of values to copy into the row; must be non-{@code null} and of length {@code columnCount}
     * @throws IndexOutOfBoundsException if {@code rowIndex} is out of bounds
     * @throws IllegalArgumentException if {@code row} is {@code null} or if {@code row.length != columnCount}
     */
    public void setRow(final int rowIndex, final long[] row) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.setColumn(0, new long[] {7L, 8L});
     * matrix.columnCopy(0);                   // returns [7, 8]
     *
     * matrix.setColumn(2, new long[] {0L, 0L});
     * matrix.get(1, 2);                       // returns 0L
     *
     * matrix.setColumn(0, new long[] {1L, 2L, 3L}); // throws IllegalArgumentException (length != rowCount)
     * matrix.setColumn(5, new long[] {1L, 2L});     // throws IndexOutOfBoundsException (columnIndex out of bounds)
     * }</pre>
     *
     * @param columnIndex the index of the column to set (0-based)
     * @param column the array of values to copy into the column; must be non-{@code null} and of length {@code rowCount}
     * @throws IndexOutOfBoundsException if {@code columnIndex} is out of bounds
     * @throws IllegalArgumentException if {@code column} is {@code null} or if {@code column.length != rowCount}
     */
    public void setColumn(final int columnIndex, final long[] column) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.updateRow(0, x -> x * 2);
     * matrix.rowCopy(0);                      // returns [2, 4, 6]
     *
     * matrix.updateRow(1, x -> 0);
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
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.LongUnaryOperator<E> operator)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        N.checkArgNotNull(operator, "operator");

        checkRowIndex(rowIndex);

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
     * matrix.updateColumn(0, x -> x + 10L);
     * matrix.columnCopy(0);                   // returns [11, 13, 15]
     *
     * matrix.updateColumn(1, x -> -x);
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
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.LongUnaryOperator<E> operator)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        N.checkArgNotNull(operator, "operator");

        checkColumnIndex(columnIndex);

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = operator.applyAsLong(a[i][columnIndex]);
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * matrix.mainDiagonalCopy();              // returns [1, 5, 9]
     *
     * LongMatrix small = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * small.mainDiagonalCopy();               // returns [1, 4]
     *
     * LongMatrix.empty().mainDiagonalCopy();   // returns [] (0x0 is square)
     * LongMatrix nonSquare = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * nonSquare.mainDiagonalCopy();           // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new long array containing a copy of the main diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public long[] mainDiagonalCopy() throws IllegalStateException {
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
     * matrix.mainDiagonalCopy();              // returns [9, 8]
     * matrix.get(1, 1);                      // returns 8L (diagonal element updated)
     *
     * matrix.setMainDiagonal(new long[] {1L}); // throws IllegalArgumentException (length != rowCount)
     * LongMatrix nonSquare = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * nonSquare.setMainDiagonal(new long[] {1L, 2L}); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must be non-{@code null} and of length {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null} or its length is not equal to {@code rowCount}
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
     * matrix.updateMainDiagonal(x -> x * x);
     * matrix.mainDiagonalCopy();              // returns [1, 16]
     * matrix.get(0, 1);                      // returns 2L (off-diagonal unchanged)
     *
     * matrix.updateMainDiagonal(null);       // throws IllegalArgumentException (operator is null)
     * LongMatrix nonSquare = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * nonSquare.updateMainDiagonal(x -> x);  // throws IllegalStateException (not square)
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
     * Returns a copy of the anti-diagonal elements (upper-right to lower-left) as an array.
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>This method extracts the anti-diagonal (secondary diagonal) elements from
     * upper-right to lower-left, at positions (0,n-1), (1,n-2), (2,n-3), etc.
     * The returned array is a copy; modifications to it will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * matrix.antiDiagonalCopy();              // returns [3, 5, 7]
     *
     * LongMatrix small = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * small.antiDiagonalCopy();               // returns [2, 3]
     *
     * LongMatrix.empty().antiDiagonalCopy();   // returns [] (0x0 is square)
     * LongMatrix nonSquare = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * nonSquare.antiDiagonalCopy();           // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new long array containing a copy of the anti-diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public long[] antiDiagonalCopy() throws IllegalStateException {
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
     * matrix.antiDiagonalCopy();              // returns [9, 8]
     * matrix.get(0, 1);                      // returns 9L (anti-diagonal cell)
     *
     * matrix.setAntiDiagonal(new long[] {1L}); // throws IllegalArgumentException (length != rowCount)
     * LongMatrix nonSquare = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * nonSquare.setAntiDiagonal(new long[] {1L, 2L}); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must be non-{@code null} and of length {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null} or its length is not equal to {@code rowCount}
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
     * matrix.updateAntiDiagonal(x -> -x);
     * matrix.antiDiagonalCopy();              // returns [-2, -3]
     * matrix.get(0, 0);                      // returns 1L (off anti-diagonal unchanged)
     *
     * matrix.updateAntiDiagonal(null);       // throws IllegalArgumentException (operator is null)
     * LongMatrix nonSquare = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * nonSquare.updateAntiDiagonal(x -> x);  // throws IllegalStateException (not square)
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
     * matrix.updateAll(x -> x * 2);
     * matrix.get(0, 0);                       // returns 2L
     * matrix.get(1, 1);                       // returns 8L
     *
     * matrix.updateAll(x -> 0);
     * matrix.get(0, 1);                       // returns 0L
     *
     * LongMatrix.empty().updateAll(x -> x);                                    // no-op on empty matrix (no elements)
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

        if (Matrices.shouldRunInParallel(this)) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = operator.applyAsLong(a[i][j]);
            Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
        } else {
            for (int i = 0; i < rowCount; i++) {
                final long[] row = a[i];

                for (int j = 0; j < columnCount; j++) {
                    row[j] = operator.applyAsLong(row[j]);
                }
            }
        }
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
     * matrix.updateAll((i, j) -> (long) (i + j));
     * matrix.get(0, 2);                       // returns 2L
     * matrix.get(1, 2);                       // returns 3L
     *
     * matrix.updateAll((i, j) -> i * 10L + j);
     * matrix.get(1, 1);                       // returns 11L
     *
     * LongMatrix.empty().updateAll((i, j) -> (long) i);                          // no-op on empty matrix
     * matrix.updateAll((Throwables.IntBiFunction<Long, RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the function that receives row index and column index (0-based) and returns
     *             the new value for that position; the returned {@code Long} is unboxed, so it
     *             must not be {@code null}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws NullPointerException if {@code mapper} returns {@code null} for any position
     * @throws E if the mapper throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Long, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = mapper.apply(i, j);
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));
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
     * matrix.replaceIf(x -> x < 0, 0L);
     * matrix.get(0, 0);                       // returns 0L (-1 replaced)
     * matrix.get(1, 0);                       // returns 4L (unchanged)
     *
     * matrix.replaceIf(x -> x == 0, 99L);
     * matrix.get(0, 2);                       // returns 99L (was 0)
     *
     * LongMatrix.empty().replaceIf(x -> true, 1L);                             // no-op on empty matrix
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
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = predicate.test(a[i][j]) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));
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
     * matrix.replaceIf((i, j) -> i == j, 0L);
     * matrix.get(0, 0);                       // returns 0L (diagonal)
     * matrix.get(0, 1);                       // returns 2L (unchanged)
     *
     * matrix.replaceIf((i, j) -> i == 0 || j == 0, -1L);
     * matrix.get(0, 1);                       // returns -1L (first row)
     * matrix.get(2, 0);                       // returns -1L (first column)
     *
     * LongMatrix.empty().replaceIf((i, j) -> true, 1L);                         // no-op on empty matrix
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
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = predicate.test(i, j) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));
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
     * squared.get(1, 1);                      // returns 16L
     * matrix.get(1, 1);                       // returns 4L (original unchanged)
     *
     * LongMatrix negated = matrix.map(x -> -x);
     * negated.get(0, 0);                      // returns -1L
     *
     * LongMatrix.empty().map(x -> x).isEmpty();                          // returns true
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
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsLong(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return LongMatrix.of(result);
    }

    /**
     * Creates a new IntMatrix by applying a function that converts long values to int.
     * This operation may be executed in parallel for better performance on large matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{123L, 256L}, {300L, 401L}});
     * IntMatrix intMatrix = matrix.mapToInt(x -> (int) (x % 100));
     * intMatrix.get(0, 0);                    // returns 23
     * intMatrix.get(1, 0);                    // returns 0 ((int)(300 % 100))
     *
     * LongMatrix big = LongMatrix.of(new long[][] {{Long.MAX_VALUE}});
     * big.mapToInt(x -> (int) x).get(0, 0);   // returns -1 (narrowing keeps low 32 bits)
     *
     * LongMatrix.empty().mapToInt(x -> (int) x).isEmpty();                    // returns true
     * matrix.mapToInt((Throwables.LongToIntFunction<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to convert long values to int
     * @return a new {@link IntMatrix} with the converted values
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     * @see #toIntMatrix()
     */
    public <E extends Exception> IntMatrix mapToInt(final Throwables.LongToIntFunction<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final int[][] result = new int[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsInt(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return IntMatrix.of(result);
    }

    /**
     * Creates a new DoubleMatrix by applying a function that converts long values to double.
     * This operation may be executed in parallel for better performance on large matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{4L, 9L}, {16L, 25L}});
     * DoubleMatrix doubleMatrix = matrix.mapToDouble(x -> Math.sqrt(x));
     * doubleMatrix.get(0, 0);                 // returns 2.0
     * doubleMatrix.get(1, 1);                 // returns 5.0
     *
     * matrix.mapToDouble(x -> x / 2.0).get(0, 0); // returns 2.0
     *
     * LongMatrix.empty().mapToDouble(x -> (double) x).isEmpty();                    // returns true
     * matrix.mapToDouble((Throwables.LongToDoubleFunction<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to convert long values to double
     * @return a new {@link DoubleMatrix} with the converted values
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     * @see #toDoubleMatrix()
     */
    public <E extends Exception> DoubleMatrix mapToDouble(final Throwables.LongToDoubleFunction<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final double[][] result = new double[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsDouble(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return DoubleMatrix.of(result);
    }

    /**
     * Creates a new Matrix by applying a function that converts long values to objects of type R.
     * This operation may be executed in parallel for better performance on large matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * Matrix<String> stringMatrix = matrix.mapToObj(x -> String.valueOf(x), String.class);
     * stringMatrix.get(0, 1);                 // returns "2"
     * stringMatrix.get(1, 0);                 // returns "3"
     *
     * Matrix<String> labeled = matrix.mapToObj(x -> "v" + x, String.class);
     * labeled.get(0, 0);                      // returns "v1"
     *
     * LongMatrix.empty().mapToObj(x -> "" + x, String.class).isEmpty();                        // returns true
     * matrix.mapToObj((Throwables.LongFunction<String, RuntimeException>) null, String.class); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <R> the element type of the resulting matrix
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to convert long values to type {@code R}
     * @param targetElementType the {@code Class} object for type {@code R} (used to allocate the
     *        {@code R[][]} backing array); must not be {@code null}
     * @return a new {@link Matrix Matrix&lt;R&gt;} containing the mapped values
     * @throws IllegalArgumentException if {@code mapper} or {@code targetElementType} is {@code null}
     * @throws E if the function throws an exception
     */
    public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.LongFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E {
        N.checkArgNotNull(mapper, "mapper");
        N.checkArgNotNull(targetElementType, "targetElementType");
        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.apply(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

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
     * matrix.get(0, 0);                       // returns 5L
     * matrix.get(1, 1);                       // returns 5L
     *
     * matrix.fill(0L);
     * matrix.get(0, 1);                       // returns 0L
     *
     * matrix.fill(Long.MIN_VALUE);
     * matrix.get(0, 0);                      // returns -9223372036854775808 (boundary value)
     * LongMatrix.empty().fill(7L);           // no-op on empty matrix
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
     * Fills this matrix with values from another two-dimensional array, starting at position {@code (0, 0)}.
     * Equivalent to {@code fill(0, 0, source)}.
     * The source array can be smaller than this matrix; only the overlapping region is copied.
     * If the source array is larger, only the portion that fits is copied. {@code null} rows in
     * {@code source} are skipped (the corresponding row of this matrix is left unchanged).
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{0L, 0L, 0L}, {0L, 0L, 0L}});
     * matrix.fill(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.get(0, 0);                       // returns 1L
     * matrix.get(0, 2);                       // returns 0L (source row is narrower, so this column is not overwritten)
     * // matrix is [[1, 2, 0], [3, 4, 0]]
     *
     * LongMatrix big = LongMatrix.of(new long[][] {{0L, 0L}, {0L, 0L}});
     * big.fill(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * big.get(1, 1);                          // returns 5L (only overlapping region copied)
     *
     * matrix.fill((long[][]) null);          // throws IllegalArgumentException (source is null)
     * }</pre>
     *
     * @param source the two-dimensional array to copy values from; must not be {@code null}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @see #fill(int, int, long[][])
     */
    public void fill(final long[][] source) {
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{0L, 0L, 0L}, {0L, 0L, 0L}, {0L, 0L, 0L}});
     * matrix.fill(1, 1, new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.get(1, 1);                       // returns 1L
     * matrix.get(2, 2);                       // returns 4L
     * matrix.get(0, 0);                       // returns 0L (outside filled region)
     * // matrix is [[0, 0, 0], [0, 1, 2], [0, 3, 4]]
     *
     * matrix.fill(0, 0, (long[][]) null);                // throws IllegalArgumentException (source is null)
     * matrix.fill(-1, 0, new long[][] {{1L}});           // throws IndexOutOfBoundsException (destRowIndex < 0)
     * matrix.fill(0, 5, new long[][] {{1L}});            // throws IndexOutOfBoundsException (destColumnIndex > columnCount)
     * }</pre>
     *
     * @param destRowIndex the target row index in this matrix (0-based, must satisfy {@code 0 <= destRowIndex <= rowCount})
     * @param destColumnIndex the target column index in this matrix (0-based, must satisfy {@code 0 <= destColumnIndex <= columnCount})
     * @param source the source array to copy values from; must not be {@code null}
     * @throws IndexOutOfBoundsException if {@code destRowIndex < 0} or {@code destRowIndex > rowCount},
     *         or if {@code destColumnIndex < 0} or {@code destColumnIndex > columnCount}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final long[][] source) throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkArgNotNull(source, "source");
        if (destRowIndex < 0 || destRowIndex > rowCount) {
            throw new IndexOutOfBoundsException(formatMsg("destRowIndex({}) must be in [0, rowCount({})]", destRowIndex, rowCount));
        }
        if (destColumnIndex < 0 || destColumnIndex > columnCount) {
            throw new IndexOutOfBoundsException(formatMsg("destColumnIndex({}) must be in [0, columnCount({})]", destColumnIndex, columnCount));
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
     * LongMatrix original = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix copy = original.copy();
     * copy.get(0, 0);                         // returns 1L
     * copy.equals(original);                  // returns true
     *
     * copy.set(0, 0, 99L);
     * original.get(0, 0);                     // returns 1L (original unchanged)
     * copy.get(0, 0);                         // returns 99L (copy modified)
     *
     * LongMatrix.empty().copy().isEmpty();    // returns true
     * }</pre>
     *
     * @return a new matrix that is a copy of this matrix with full independence guarantee
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
     * LongMatrix subset = matrix.copy(1, 3);
     * subset.rowCount();                      // returns 2
     * subset.get(0, 0);                       // returns 3L -> {{3, 4}, {5, 6}}
     *
     * matrix.copy(1, 1).rowCount();          // returns 0 (empty range)
     *
     * matrix.copy(-1, 2);                     // throws IndexOutOfBoundsException (fromRowIndex < 0)
     * matrix.copy(0, 5);                      // throws IndexOutOfBoundsException (toRowIndex > rowCount)
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
     * LongMatrix submatrix = matrix.copy(0, 2, 1, 3);
     * submatrix.get(0, 0);                    // returns 2L
     * submatrix.get(1, 1);                    // returns 6L -> {{2, 3}, {5, 6}}
     *
     * matrix.copy(0, 1, 0, 1).get(0, 0);     // returns 1L (single-cell submatrix)
     *
     * matrix.copy(0, 2, 1, 5);               // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.copy(-1, 2, 0, 2);              // throws IndexOutOfBoundsException (fromRowIndex < 0)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new {@code LongMatrix} containing the specified submatrix
     * @throws IndexOutOfBoundsException if any range is invalid (e.g. {@code fromRowIndex < 0},
     *         {@code toRowIndex > rowCount}, {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code from > to} for either range)
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
     * grown.get(3, 3);                        // returns 0L (new cell)
     * grown.get(0, 0);                        // returns 1L (preserved)
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * LongMatrix truncated = matrix.resize(2, 2);
     * truncated.columnCount();                // returns 2
     * truncated.get(1, 1);                    // returns 5L
     *
     * // Mixed: grow rows, truncate columns
     * LongMatrix mixed = matrix.resize(4, 2);
     * mixed.get(3, 0);                        // returns 0L (new row)
     * matrix.resize(-1, 2);                   // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new LongMatrix with the specified dimensions
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
     * <p><b>Comparison with {@link #extend(int, int, int, int, long)}:</b>
     * {@code resize} takes <em>absolute</em> target dimensions and may truncate existing content.
     * {@code extend} takes <em>relative</em> padding amounts per edge and <em>never truncates</em>.
     * Use {@code extend} when the entire original content must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     *
     * // Grow: fill new cells with 9L
     * LongMatrix grown = matrix.resize(4, 4, 9L);
     * grown.get(3, 3);                        // returns 9L (new cell uses defaultValue)
     * grown.get(0, 0);                        // returns 1L (preserved)
     *
     * // Truncate: defaultValue is ignored when shrinking
     * LongMatrix truncated = matrix.resize(2, 2, 9L);
     * truncated.get(1, 1);                    // returns 5L (no new cells, default unused)
     *
     * matrix.resize(0, 0, 9L).isEmpty();     // returns true
     * matrix.resize(2, -1, 9L);              // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the value used to fill cells that are added when a dimension grows;
     *        ignored when a dimension shrinks
     * @return a new LongMatrix with the specified dimensions
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
     * Returns a new matrix formed by surrounding this matrix with padding on all four edges.
     * New cells are filled with {@code 0L}.
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     *
     * // Uniform 1-cell border of 0L
     * LongMatrix bordered = matrix.extend(1, 1, 1, 1);
     * bordered.rowCount();                    // returns 4
     * bordered.get(0, 0);                     // returns 0L (border cell)
     * bordered.get(1, 1);                     // returns 1L (original top-left)
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * LongMatrix shifted = matrix.extend(0, 0, 2, 0);
     * shifted.get(0, 2);                      // returns 1L (original shifted right)
     *
     * matrix.extend(0, 0, 0, 0).equals(matrix); // returns true (no padding -> copy)
     * matrix.extend(-1, 0, 0, 0);               // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @return a new LongMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         if the resulting dimensions would overflow {@code Integer.MAX_VALUE},
     *         or if the resulting shape is not representable (zero rows with a non-zero column count)
     * @see #extend(int, int, int, int, long)
     * @see #resize(int, int)
     */
    @Override
    public LongMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight) {
        return extend(padTop, padBottom, padLeft, padRight, 0);
    }

    /**
     * Returns a new matrix formed by surrounding this matrix with padding on all four edges.
     * New cells are filled with {@code defaultValue}.
     *
     * <p>Unlike {@link #resize(int, int, long)}, this method <b>never truncates</b>: the entire
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     *
     * // Uniform 1-cell border filled with 9L
     * LongMatrix bordered = matrix.extend(1, 1, 1, 1, 9L);
     * bordered.get(0, 0);                     // returns 9L (border cell)
     * bordered.get(1, 1);                     // returns 1L (original top-left)
     * bordered.get(2, 2);                     // returns 4L (original bottom-right)
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * LongMatrix shifted = matrix.extend(0, 0, 2, 0, 7L);
     * shifted.get(0, 0);                      // returns 7L (left padding)
     * shifted.get(0, 2);                      // returns 1L (original shifted right)
     *
     * matrix.extend(-1, 0, 0, 0, 9L);        // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @param defaultValue the value to fill all new padding cells with
     * @return a new LongMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         if the resulting dimensions would overflow {@code Integer.MAX_VALUE},
     *         or if the resulting shape is not representable (zero rows with a non-zero column count)
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
     * Reverses the order of elements in each row in-place (horizontal flip).
     * This modifies the current matrix; each row is reversed left-to-right.
     *
     * <p>This is an in-place operation that modifies the current matrix.
     * For a non-destructive version that returns a new matrix, use {@link #flipHorizontally()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.flipHorizontallyInPlace();
     * matrix.rowCopy(0);                      // returns [3, 2, 1]
     * matrix.get(1, 0);                       // returns 6L
     *
     * LongMatrix single = LongMatrix.of(new long[][] {{1L}});
     * single.flipHorizontallyInPlace();
     * single.get(0, 0);                             // returns 1L (single column unchanged)
     * LongMatrix.empty().flipHorizontallyInPlace(); // no-op on empty matrix
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}, {5L, 6L}});
     * matrix.flipVerticallyInPlace();
     * matrix.rowCopy(0);                      // returns [5, 6]
     * matrix.rowCopy(2);                      // returns [1, 2]
     *
     * LongMatrix single = LongMatrix.of(new long[][] {{1L, 2L}});
     * single.flipVerticallyInPlace();
     * single.rowCopy(0);                          // returns [1, 2] (single row unchanged)
     * LongMatrix.empty().flipVerticallyInPlace(); // no-op on empty matrix
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
     * Returns a new matrix that is a horizontal flip of this matrix (columns in reversed order).
     * Each row is reversed left-to-right (the leftmost element becomes rightmost).
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * LongMatrix flipped = matrix.flipHorizontally();
     * flipped.rowCopy(0);                     // returns [3, 2, 1]
     * matrix.get(0, 0);                       // returns 1L (original unchanged)
     *
     * LongMatrix single = LongMatrix.of(new long[][] {{5L}});
     * single.flipHorizontally().get(0, 0);             // returns 5L (single column unchanged)
     * LongMatrix.empty().flipHorizontally().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new LongMatrix with each row reversed
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
     * The topmost row becomes bottommost.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * LongMatrix flipped = matrix.flipVertically();
     * flipped.rowCopy(0);                     // returns [4, 5, 6]
     * matrix.rowCopy(0);                      // returns [1, 2, 3] (original unchanged)
     *
     * LongMatrix single = LongMatrix.of(new long[][] {{1L, 2L}});
     * single.flipVertically().rowCopy(0);            // returns [1, 2] (single row unchanged)
     * LongMatrix.empty().flipVertically().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new LongMatrix with rows reversed
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
     * row of the result being the first column of the original read from bottom to top.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * LongMatrix rotated = matrix.rotate90();
     * rotated.rowCopy(0);                     // returns [7, 4, 1]
     * rotated.get(2, 2);                      // returns 3L
     *
     * LongMatrix wide = LongMatrix.of(new long[][] {{1L, 2L, 3L}});  // 1x3
     * LongMatrix tall = wide.rotate90();
     * tall.rowCount();                         // returns 3 (dimensions swapped to 3x1)
     * tall.get(0, 0);                          // returns 1L
     * LongMatrix.empty().rotate90().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix rotated 90 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         or an empty matrix if this matrix has zero columns
     * @see #rotate180()
     * @see #rotate270()
     * @see #transpose()
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * LongMatrix rotated = matrix.rotate180();
     * rotated.rowCopy(0);                     // returns [9, 8, 7]
     * rotated.get(2, 2);                      // returns 1L
     *
     * LongMatrix rect = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * rect.rotate180().rowCopy(0);              // returns [4, 3] (same dimensions)
     * LongMatrix.empty().rotate180().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 180 degrees
     * @see #rotate90()
     * @see #rotate270()
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
     * This is equivalent to rotating 90 degrees counter-clockwise.
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * row of the result being the last column of the original read from top to bottom.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * LongMatrix rotated = matrix.rotate270();
     * rotated.rowCopy(0);                     // returns [3, 6, 9]
     * rotated.get(2, 2);                      // returns 7L
     *
     * LongMatrix wide = LongMatrix.of(new long[][] {{1L, 2L, 3L}});  // 1x3
     * LongMatrix tall = wide.rotate270();
     * tall.rowCount();                          // returns 3 (dimensions swapped to 3x1)
     * tall.get(0, 0);                           // returns 3L
     * LongMatrix.empty().rotate270().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix rotated 270 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         or an empty matrix if this matrix has zero columns
     * @see #rotate90()
     * @see #rotate180()
     * @see #transpose()
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
     * Returns a new matrix that is the transpose of this matrix.
     * The element at position {@code (i, j)} in this matrix appears at position {@code (j, i)}
     * in the result. The resulting matrix has dimensions swapped: {@code columnCount x rowCount}.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * LongMatrix transposed = matrix.transpose();
     * transposed.rowCount();                  // returns 3 (2x3 becomes 3x2)
     * transposed.get(0, 1);                   // returns 4L (was at (1,0))
     * transposed.rowCopy(2);                  // returns [3, 6]
     *
     * matrix.transpose().transpose().equals(matrix); // returns true (involution)
     * LongMatrix.empty().transpose().isEmpty();      // returns true
     * }</pre>
     *
     * @return a new {@code LongMatrix} of shape {@code columnCount x rowCount} that is the transpose of this matrix;
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
     * Reshapes this matrix to have the specified dimensions.
     * Elements are taken in row-major order from this matrix and placed into the new shape.
     * The new shape must have at least as many total cells as the original
     * ({@code (long) newRowCount * newColumnCount >= elementCount()}).
     * Any extra trailing cells in the new shape are filled with {@code 0L}.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * LongMatrix reshaped = matrix.reshape(3, 2);
     * reshaped.rowCopy(0);                    // returns [1, 2] -> [[1, 2], [3, 4], [5, 6]]
     * reshaped.get(2, 1);                     // returns 6L
     *
     * LongMatrix extended = matrix.reshape(2, 4);
     * extended.get(1, 2);                     // returns 0L (extra trailing cell) -> [[1,2,3,4],[5,6,0,0]]
     *
     * matrix.reshape(0, 0);                   // throws IllegalArgumentException (too small for 6 elements)
     * matrix.reshape(-1, 6);                  // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix; must be {@code >= 0}
     * @param newColumnCount the number of columns in the reshaped matrix; must be {@code >= 0}
     * @return a new {@code LongMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative, if the resulting shape is not
     *         representable (zero rows with a non-zero column count), if the total cell count {@code (long) newRowCount * newColumnCount}
     *         exceeds {@code Integer.MAX_VALUE}, or if the new shape is too small to hold every existing element
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public LongMatrix reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        checkMaterializableShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final long[][] c = new long[newRowCount][newColumnCount];

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return new LongMatrix(c);
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

        return new LongMatrix(c);
    }

    /**
     * Repeats elements in both row and column directions.
     * Each element is repeated to form a block of size rowRepeats x columnRepeats.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}});
     * LongMatrix repeated = matrix.repeatElements(2, 3);
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
     * @return a new LongMatrix with repeated elements
     * @throws IllegalArgumentException if rowRepeats or columnRepeats is not positive,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repelem.html">MATLAB repelem function</a>
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
     * Repeats the entire matrix in a tiled pattern.
     * The matrix is repeated as a whole rowRepeats times vertically and columnRepeats times horizontally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix repeated = matrix.repeatMatrix(2, 3);
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
     * @return a new LongMatrix with the tiled pattern
     * @throws IllegalArgumentException if rowRepeats or columnRepeats is not positive,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repmat.html">MATLAB repmat function</a>
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
     * Returns a new {@link LongList} containing all elements of this matrix in row-major order.
     * The returned list owns its data; modifications to it do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongList list = matrix.flatten();
     * list.size();                            // returns 4
     * list.get(0);                            // returns 1L
     * list.get(3);                            // returns 4L (row-major order)
     *
     * LongMatrix.empty().flatten().size();                 // returns 0
     * LongMatrix.of(new long[][] {{7L}}).flatten().get(0); // returns 7L (single element)
     * }</pre>
     *
     * @return a new {@link LongList} of all elements in row-major order
     * @throws IllegalStateException if {@code (long) rowCount * columnCount > Integer.MAX_VALUE}
     * @see #rowMajorStream()
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
     * Exposes the elements of this matrix to {@code action} as a single one-dimensional array
     * laid out in row-major order, then propagates any modifications back into the matrix.
     *
     * <p>This enables operations that need a global view of all matrix elements (e.g., sorting all
     * elements across the entire matrix). The shape of this matrix is preserved; only element
     * values change. See {@link Arrays#mutateFlattened(long[][], Throwables.Consumer)} for the exact
     * semantics of the underlying operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{5L, 3L}, {4L, 1L}});
     * matrix.mutateFlattened(arr -> java.util.Arrays.sort(arr));
     * matrix.rowCopy(0);                      // returns [1, 3]
     * matrix.rowCopy(1);                      // returns [4, 5] (sorted globally, placed back row-major)
     *
     * long[] captured = new long[1];
     * matrix.mutateFlattened(arr -> captured[0] = arr.length);
     * captured[0];                            // returns 4 (flat view length)
     *
     * LongMatrix.empty().mutateFlattened(arr -> { });  // no-op on empty matrix
     * }</pre>
     *
     * @param <E> the type of exception that the operation may throw
     * @param action the operation to apply to the flattened array
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the operation throws an exception
     * @see Arrays#mutateFlattened(long[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateFlattened(final Throwables.Consumer<? super long[], E> action) throws E {
        N.checkArgNotNull(action, "action");

        Arrays.mutateFlattened(a, action);
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
     * LongMatrix a = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});      // 2x3
     * LongMatrix b = LongMatrix.of(new long[][] {{7L, 8L, 9L}, {10L, 11L, 12L}});   // 2x3
     * LongMatrix c = a.stackVertically(b);
     * c.rowCount();                           // returns 4
     * c.rowCopy(2);                           // returns [7, 8, 9]
     * c.get(3, 2);                            // returns 12L
     *
     * LongMatrix mismatch = LongMatrix.of(new long[][] {{1L, 2L}});
     * a.stackVertically(mismatch);           // throws IllegalArgumentException (column count differs)
     * a.stackVertically((LongMatrix) null);  // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to stack below this matrix (must have the same column count)
     * @return a new LongMatrix with dimensions (this.rowCount + other.rowCount) x this.columnCount
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.columnCount != other.columnCount},
     *         or if the merged row count would exceed {@code Integer.MAX_VALUE}
     * @see #stackHorizontally(LongMatrix)
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
     * Stacks this matrix horizontally with another matrix (horizontal concatenation).
     * The matrices must have the same number of rows. The result has columns from this matrix
     * on the left and columns from the other matrix on the right.
     *
     * <p>This operation is also known as horizontal concatenation or cbind (bind by columns).
     * Creates a new matrix; the original matrices are not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix a = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});      // 2x3
     * LongMatrix b = LongMatrix.of(new long[][] {{7L, 8L, 9L}, {10L, 11L, 12L}});   // 2x3
     * LongMatrix c = a.stackHorizontally(b);
     * c.columnCount();                        // returns 6
     * c.rowCopy(0);                           // returns [1, 2, 3, 7, 8, 9]
     * c.get(1, 5);                            // returns 12L
     *
     * LongMatrix mismatch = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * a.stackHorizontally(mismatch);          // throws IllegalArgumentException (row count differs)
     * a.stackHorizontally((LongMatrix) null); // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to stack to the right of this matrix (must have the same row count)
     * @return a new LongMatrix with dimensions this.rowCount x (this.columnCount + other.columnCount)
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.rowCount != other.rowCount},
     *         or if the merged column count would exceed {@code Integer.MAX_VALUE}
     * @see #stackVertically(LongMatrix)
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
     * Performs element-wise addition with another matrix.
     * The matrices must have the same dimensions. The original matrices are not modified.
     *
     * <p><b>Overflow:</b> arithmetic uses standard Java {@code long} addition, which silently
     * wraps around modulo 2<sup>64</sup> on overflow.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix a = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix b = LongMatrix.of(new long[][] {{5L, 6L}, {7L, 8L}});
     * LongMatrix sum = a.add(b);
     * sum.get(0, 0);                          // returns 6L
     * sum.get(1, 1);                          // returns 12L -> [[6, 8], [10, 12]]
     *
     * LongMatrix big = LongMatrix.of(new long[][] {{Long.MAX_VALUE}});
     * big.add(LongMatrix.of(new long[][] {{1L}})).get(0, 0); // returns -9223372036854775808 (long overflow wraps)
     *
     * LongMatrix wrongShape = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * a.add(wrongShape);                     // throws IllegalArgumentException (different shapes)
     * a.add((LongMatrix) null);              // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to add to this matrix; must not be {@code null} and must have the same shape
     * @return a new {@code LongMatrix} containing the element-wise sum
     * @throws IllegalArgumentException if {@code other} is {@code null}, or if the matrices have different shapes
     * @see #subtract(LongMatrix)
     * @see #zipWith(LongMatrix, Throwables.LongBinaryOperator)
     */
    public LongMatrix add(final LongMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot add matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final long[][] otherArray = other.a;
        final long[][] result = new long[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = a[i][j] + otherArray[i][j];

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return LongMatrix.of(result);
    }

    /**
     * Performs element-wise subtraction ({@code this - other}).
     * The matrices must have the same dimensions. The original matrices are not modified.
     *
     * <p><b>Overflow:</b> arithmetic uses standard Java {@code long} subtraction, which silently
     * wraps around modulo 2<sup>64</sup> on overflow.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix a = LongMatrix.of(new long[][] {{5L, 6L}, {7L, 8L}});
     * LongMatrix b = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix diff = a.subtract(b);
     * diff.get(0, 0);                         // returns 4L
     * diff.get(1, 1);                         // returns 4L -> [[4, 4], [4, 4]]
     *
     * LongMatrix min = LongMatrix.of(new long[][] {{Long.MIN_VALUE}});
     * min.subtract(LongMatrix.of(new long[][] {{1L}})).get(0, 0); // returns 9223372036854775807 (long overflow wraps)
     *
     * LongMatrix wrongShape = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * a.subtract(wrongShape);                // throws IllegalArgumentException (different shapes)
     * a.subtract((LongMatrix) null);         // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to subtract from this matrix; must not be {@code null} and must have the same shape
     * @return a new {@code LongMatrix} containing the element-wise difference {@code this - other}
     * @throws IllegalArgumentException if {@code other} is {@code null}, or if the matrices have different shapes
     * @see #add(LongMatrix)
     */
    public LongMatrix subtract(final LongMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot subtract matrices with different shapes: this is {}x{} but other is {}x{}", rowCount,
                columnCount, other.rowCount, other.columnCount);

        final long[][] otherArray = other.a;
        final long[][] result = new long[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = a[i][j] - otherArray[i][j];

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return LongMatrix.of(result);
    }

    /**
     * Performs matrix multiplication (Cayley product) with another matrix.
     * The number of columns in this matrix must equal the number of rows in {@code other}.
     * Result has shape {@code this.rowCount x other.columnCount}. The original matrices are not modified.
     *
     * <p><b>Note:</b> This is the linear-algebra matrix product, not element-wise multiplication.
     * For element-wise multiplication use {@link #zipWith(LongMatrix, Throwables.LongBinaryOperator)}.</p>
     *
     * <p><b>Overflow:</b> both the per-step products {@code a[i][k] * other[k][j]} and the
     * accumulating sums use standard Java {@code long} arithmetic, which silently wraps modulo
     * 2<sup>64</sup>.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix a = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix b = LongMatrix.of(new long[][] {{5L, 6L}, {7L, 8L}});
     * LongMatrix product = a.matrixMultiply(b);
     * product.get(0, 0);                      // returns 19L (1*5 + 2*7)
     * product.get(1, 1);                      // returns 50L -> [[19, 22], [43, 50]]
     *
     * LongMatrix m2x3 = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});      // 2x3
     * LongMatrix m3x2 = LongMatrix.of(new long[][] {{7L, 8L}, {9L, 10L}, {11L, 12L}}); // 3x2
     * m2x3.matrixMultiply(m3x2).rowCount();                                                    // returns 2 (result is 2x2)
     *
     * a.matrixMultiply(m3x2);                        // throws IllegalArgumentException (a.columnCount=2 != m3x2.rowCount=3)
     * a.matrixMultiply((LongMatrix) null);           // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to multiply with; must not be {@code null}
     * @return a new {@code LongMatrix} of shape {@code this.rowCount x other.columnCount} containing the matrix product
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.columnCount != other.rowCount}, or if this matrix has zero rows while {@code other} has a non-zero column count (the resulting shape is not representable)
     */
    public LongMatrix matrixMultiply(final LongMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.rowCount,
                "Matrix dimensions incompatible for multiplication: this is {}x{}, other is {}x{} (this.columnCount must equal other.rowCount)", rowCount,
                columnCount, other.rowCount, other.columnCount);

        checkRepresentableShape(rowCount, other.columnCount);

        final long[][] otherArray = other.a;
        final long[][] result = new long[rowCount][other.columnCount];
        final Throwables.IntTriConsumer<RuntimeException> multiplyAction = (i, j, k) -> result[i][j] += a[i][k] * otherArray[k][j];

        Matrices.forEachCartesianIndices(this, other, multiplyAction);

        return LongMatrix.of(result);
    }

    /**
     * Converts this primitive long matrix to a boxed {@link Matrix Matrix&lt;Long&gt;}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix primitive = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * Matrix<Long> boxed = primitive.boxed();
     * boxed.get(0, 1);                        // returns Long 2
     * boxed.get(1, 0);                        // returns Long 3
     *
     * LongMatrix.unbox(primitive.boxed()).equals(primitive); // returns true (round-trip)
     * LongMatrix.empty().boxed().isEmpty();                  // returns true
     * }</pre>
     *
     * @return a new {@link Matrix Matrix&lt;Long&gt;} containing the same values as boxed {@code Long} instances
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
     * Converts this {@code long} matrix to an {@link IntMatrix}.
     * Each long value is narrowed to int by a Java primitive narrowing cast, which discards
     * all but the low-order 32 bits (per JLS §5.1.3).
     *
     * <p><b>Warning:</b> This is a narrowing conversion that may lose information.
     * Values outside the int range ({@code Integer.MIN_VALUE} to {@code Integer.MAX_VALUE})
     * wrap around modulo 2<sup>32</sup> rather than being clamped; the resulting int may have a
     * different sign than the original long (e.g. {@code (int) Long.MAX_VALUE} is {@code -1}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix longMatrix = LongMatrix.of(new long[][] {{1L, 2L}});
     * IntMatrix intMatrix = longMatrix.toIntMatrix();
     * intMatrix.get(0, 0);                    // returns 1
     * intMatrix.get(0, 1);                    // returns 2
     *
     * LongMatrix max = LongMatrix.of(new long[][] {{Long.MAX_VALUE}});
     * max.toIntMatrix().get(0, 0);                // returns -1 (narrowing keeps low 32 bits)
     * LongMatrix.empty().toIntMatrix().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new {@link IntMatrix} with the narrowed values
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
     * Converts this {@code long} matrix to a {@link FloatMatrix}.
     * Each long value is converted to float by Java's standard {@code long}-to-{@code float} widening
     * primitive conversion (which may lose precision despite being a widening conversion).
     *
     * <p><b>Warning:</b> Precision loss may occur for large long values. The {@code float} type has
     * only 24 bits of mantissa precision, so long values with absolute values greater than 2<sup>24</sup>
     * ({@code 16_777_216}) may be rounded.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix longMatrix = LongMatrix.of(new long[][] {{1L, 2L}});
     * FloatMatrix floatMatrix = longMatrix.toFloatMatrix();
     * floatMatrix.get(0, 0);                  // returns 1.0f
     * floatMatrix.get(0, 1);                  // returns 2.0f
     *
     * LongMatrix big = LongMatrix.of(new long[][] {{16_777_217L}});
     * big.toFloatMatrix().get(0, 0);                // returns 1.6777216E7f (precision lost beyond 2^24)
     * LongMatrix.empty().toFloatMatrix().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new {@link FloatMatrix} with the converted values
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
     * Converts this {@code long} matrix to a {@link DoubleMatrix}.
     * Each long value is converted to double by Java's standard {@code long}-to-{@code double} widening
     * primitive conversion (which may lose precision despite being a widening conversion).
     *
     * <p><b>Warning:</b> Precision loss may occur for large long values. The {@code double} type has
     * only 53 bits of mantissa precision, so long values with absolute values greater than 2<sup>53</sup>
     * ({@code 9_007_199_254_740_992}) may be rounded.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix longMatrix = LongMatrix.of(new long[][] {{1L, 2L}});
     * DoubleMatrix doubleMatrix = longMatrix.toDoubleMatrix();
     * doubleMatrix.get(0, 0);                 // returns 1.0
     * doubleMatrix.get(0, 1);                 // returns 2.0
     *
     * LongMatrix max = LongMatrix.of(new long[][] {{Long.MAX_VALUE}});
     * max.toDoubleMatrix().get(0, 0);                // returns 9.223372036854776E18 (precision lost beyond 2^53)
     * LongMatrix.empty().toDoubleMatrix().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new {@link DoubleMatrix} with the converted values
     * @see #mapToDouble(Throwables.LongToDoubleFunction)
     * @see DoubleMatrix#from(long[][])
     */
    public DoubleMatrix toDoubleMatrix() {
        return DoubleMatrix.from(a);
    }

    /**
     * Performs element-wise operation on two matrices using a binary operator.
     * The matrices must have the same dimensions. Corresponding elements from both matrices
     * are combined using the provided function to produce the result matrix.
     *
     * <p>This is a generalized element-wise operation. For the common element-wise operations of addition and
     * subtraction, consider using the dedicated methods {@link #add(LongMatrix)} and {@link #subtract(LongMatrix)};
     * for the linear-algebra matrix product (which is not an element-wise operation), use {@link #matrixMultiply(LongMatrix)}.</p>
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
     * Creates a new matrix; the original matrices are not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix a = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix b = LongMatrix.of(new long[][] {{5L, 6L}, {7L, 8L}});
     *
     * LongMatrix product = a.zipWith(b, (x, y) -> x * y);
     * product.get(0, 1);                      // returns 12L -> [[5, 12], [21, 32]]
     *
     * LongMatrix max = a.zipWith(b, Math::max);
     * max.get(0, 0);                          // returns 5L -> [[5, 6], [7, 8]]
     *
     * LongMatrix wrongShape = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * a.zipWith(wrongShape, (x, y) -> x + y);                               // throws IllegalArgumentException (different shapes)
     * a.zipWith(b, (Throwables.LongBinaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null function)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param zipFunction the binary operator to apply to corresponding elements; receives the
     *                    element from this matrix as first argument and the element from
     *                    {@code other} as second argument
     * @return a new {@code LongMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if {@code other} or {@code zipFunction} is {@code null},
     *         or if the matrices have different shapes
     * @throws E if the zip function throws an exception
     * @see #zipWith(LongMatrix, LongMatrix, Throwables.LongTernaryOperator)
     */
    public <E extends Exception> LongMatrix zipWith(final LongMatrix other, final Throwables.LongBinaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final long[][] b = other.a;
        final long[][] result = new long[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsLong(a[i][j], b[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return LongMatrix.of(result);
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
     * LongMatrix a = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix b = LongMatrix.of(new long[][] {{5L, 6L}, {7L, 8L}});
     * LongMatrix c = LongMatrix.of(new long[][] {{9L, 10L}, {11L, 12L}});
     *
     * LongMatrix sum = a.zipWith(b, c, (x, y, z) -> x + y + z);
     * sum.get(0, 0);                          // returns 15L -> [[15, 18], [21, 24]]
     *
     * LongMatrix weighted = a.zipWith(b, c, (x, y, z) -> x * 2 + y * 3 + z * 5);
     * weighted.get(0, 0);                     // returns 62L -> [[62, 72], [82, 92]]
     *
     * LongMatrix wrongShape = LongMatrix.of(new long[][] {{1L, 2L, 3L}});
     * a.zipWith(wrongShape, c, (x, y, z) -> x);                                 // throws IllegalArgumentException (different shapes)
     * a.zipWith(b, c, (Throwables.LongTernaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null function)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param third the third matrix (must have the same dimensions as this matrix)
     * @param zipFunction the ternary operator to apply to corresponding elements; receives the
     *                    element from this matrix as first argument, the element from
     *                    {@code other} as second argument, and the element from {@code third}
     *                    as third argument
     * @return a new {@code LongMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if any of {@code other}, {@code third}, or {@code zipFunction}
     *         is {@code null}, or if any of the matrices have different shapes
     * @throws E if the zip function throws an exception
     * @see #zipWith(LongMatrix, Throwables.LongBinaryOperator)
     */
    public <E extends Exception> LongMatrix zipWith(final LongMatrix other, final LongMatrix third, final Throwables.LongTernaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(third, "third");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other) && isSameShape(third), "Cannot zip matrices with different shapes: this is {}x{}, other is {}x{}, third is {}x{}",
                rowCount, columnCount, other.rowCount, other.columnCount, third.rowCount, third.columnCount);

        final long[][] b = other.a;
        final long[][] c = third.a;
        final long[][] result = new long[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsLong(a[i][j], b[i][j], c[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return LongMatrix.of(result);
    }

    /**
     * Returns a stream of elements on the main diagonal (upper-left to lower-right).
     * The matrix must be square.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * matrix.mainDiagonalStream().toArray();  // returns [1, 5, 9]
     * matrix.mainDiagonalStream().sum();      // returns 15L
     *
     * LongMatrix.empty().mainDiagonalStream().count(); // returns 0 (empty stream)
     * LongMatrix nonSquare = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * nonSquare.mainDiagonalStream();         // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a LongStream of main-diagonal elements, or an empty stream if this is the empty {@code 0x0} matrix
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
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
     * Returns a stream of elements on the anti-diagonal (upper-right to lower-left).
     * The matrix must be square.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     * matrix.antiDiagonalStream().toArray();  // returns [3, 5, 7]
     * matrix.antiDiagonalStream().sum();      // returns 15L
     *
     * LongMatrix.empty().antiDiagonalStream().count(); // returns 0 (empty stream)
     * LongMatrix nonSquare = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * nonSquare.antiDiagonalStream();         // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a LongStream of anti-diagonal elements, or an empty stream if this is the empty {@code 0x0} matrix
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
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
     * Returns a stream of all elements in this matrix, traversed in row-major order (left to right, top to bottom).
     * Elements are streamed row by row from the top-left corner to the bottom-right corner.
     *
     * <p>This method is useful for processing all matrix elements sequentially
     * without concern for their row/column positions. The stream supports all
     * standard LongStream operations including sum, average, filter, map, etc.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.rowMajorStream().toArray();    // returns [1, 2, 3, 4]
     * matrix.rowMajorStream().sum();        // returns 10L
     *
     * LongMatrix.empty().rowMajorStream().count();               // returns 0 (empty stream)
     * LongMatrix.of(new long[][] {{7L}}).rowMajorStream().sum(); // returns 7L (single element)
     * }</pre>
     *
     * @return a LongStream of all elements in row-major order, or an empty stream if the matrix is empty
     */
    @Override
    public LongStream rowMajorStream() {
        return rowMajorStream(0, rowCount);
    }

    /**
     * Returns a stream of elements from a single row.
     * The elements are streamed from left to right within the specified row.
     *
     * <p>This method is particularly useful when you need to process or analyze
     * a specific row of the matrix independently. The returned stream can be
     * used with all standard LongStream operations.</p>
     *
     * <p>This streams the elements of the single specified row, flattened into one stream. To
     * instead obtain every row as its own stream (a stream of streams), use {@link #rowStreams()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.rowMajorStream(0).toArray();   // returns [1, 2, 3]
     * matrix.rowMajorStream(1).sum();       // returns 15L (sum of second row)
     *
     * matrix.rowMajorStream(-1);            // throws IndexOutOfBoundsException
     * matrix.rowMajorStream(2);             // throws IndexOutOfBoundsException (rowIndex >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to stream (0-based)
     * @return a {@link LongStream} of elements from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowStreams()
     */
    @Override
    public LongStream rowMajorStream(final int rowIndex) {
        checkRowIndex(rowIndex);

        return rowMajorStream(rowIndex, rowIndex + 1);
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
     * matrix.rowMajorStream(1, 3).toArray(); // returns [3, 4, 5, 6]
     * matrix.rowMajorStream(0, 2).toArray(); // returns [1, 2, 3, 4]
     *
     * matrix.rowMajorStream(1, 1).count();  // returns 0 (empty range)
     * matrix.rowMajorStream(0, 5);          // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a LongStream of elements from the specified row range, or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public LongStream rowMajorStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
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
     * Returns a stream of all elements in this matrix, traversed in column-major order (top to bottom, left to right).
     * Elements are streamed column by column from the top-left corner to the bottom-right corner.
     *
     * <p>This method provides an alternative way to iterate through matrix
     * elements compared to the row-major order of {@link #rowMajorStream()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.columnMajorStream().toArray();      // returns [1, 3, 2, 4] (column-major)
     * matrix.columnMajorStream().sum();          // returns 10L
     *
     * LongMatrix.empty().columnMajorStream().count();               // returns 0 (empty stream)
     * LongMatrix.of(new long[][] {{7L}}).columnMajorStream().sum(); // returns 7L (single element)
     * }</pre>
     *
     * @return a LongStream of all elements in column-major order, or an empty stream if the matrix is empty
     */
    @Override
    public LongStream columnMajorStream() {
        return columnMajorStream(0, columnCount);
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.columnMajorStream(1).toArray();     // returns [2, 5]
     * matrix.columnMajorStream(0).sum();         // returns 5L (sum of first column)
     *
     * matrix.columnMajorStream(-1);              // throws IndexOutOfBoundsException
     * matrix.columnMajorStream(3);               // throws IndexOutOfBoundsException (columnIndex >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to stream (0-based)
     * @return a {@link LongStream} of elements from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    @Override
    public LongStream columnMajorStream(final int columnIndex) {
        checkColumnIndex(columnIndex);

        return columnMajorStream(columnIndex, columnIndex + 1);
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.columnMajorStream(1, 3).toArray();  // returns [2, 5, 3, 6]
     * matrix.columnMajorStream(0, 2).toArray();  // returns [1, 4, 2, 5]
     *
     * matrix.columnMajorStream(1, 1).count();    // returns 0 (empty range)
     * matrix.columnMajorStream(0, 5);            // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a LongStream of elements from the specified column range in column-major order,
     *         or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    public LongStream columnMajorStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
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
     * Returns a stream of LongStream objects, where each LongStream represents a complete row.
     * This creates a stream of streams, allowing for row-by-row processing of the matrix.
     *
     * <p>This method is useful for operations that need to process entire rows as units,
     * such as row-wise transformations, filtering rows based on conditions, or mapping
     * rows to other values.</p>
     *
     * <p>This yields one stream per row. To instead stream the elements of a single row as one
     * flat stream, use {@link #rowMajorStream(int)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}, {5L, 6L}});
     * matrix.rowStreams().count();            // returns 3 (one stream per row)
     * matrix.rowStreams()
     *     .mapToLong(row -> row.sum())
     *     .toArray();                         // returns [3, 7, 11]
     *
     * LongMatrix.empty().rowStreams().count();                     // returns 0 (no rows)
     * LongMatrix.of(new long[][] {{7L, 8L}}).rowStreams().count(); // returns 1 (single row)
     * }</pre>
     *
     * @return a Stream of LongStream objects, one for each row in the matrix
     * @see #rowMajorStream(int)
     */
    @Override
    public Stream<LongStream> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Returns a stream of LongStream objects for a range of rows.
     * Each LongStream in the result represents a complete row within the specified range.
     *
     * <p>This method allows for processing a subset of rows while maintaining the
     * ability to work with complete rows as individual streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}, {5L, 6L}});
     * matrix.rowStreams(1, 3).count();        // returns 2 (rows 1 and 2)
     * matrix.rowStreams(0, 2)
     *     .mapToLong(row -> row.max().orElse(0))
     *     .toArray();                         // returns [2, 4]
     *
     * matrix.rowStreams(1, 1).count();        // returns 0 (empty range)
     * matrix.rowStreams(0, 5);                // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of LongStream objects for the specified row range
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
     * Returns a stream of LongStream objects, where each LongStream represents a complete column.
     * This creates a stream of streams, allowing for column-by-column processing of the matrix.
     *
     * <p>This method is useful for operations that need to process
     * entire columns as units, such as column-wise statistics, transformations, or filtering
     * columns based on conditions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.columnStreams().count();         // returns 3 (one stream per column)
     * matrix.columnStreams()
     *     .mapToLong(col -> col.sum())
     *     .toArray();                         // returns [5, 7, 9]
     *
     * LongMatrix.empty().columnStreams().count();                       // returns 0 (no columns)
     * LongMatrix.of(new long[][] {{7L}, {8L}}).columnStreams().count(); // returns 1 (single column)
     * }</pre>
     *
     * @return a Stream of LongStream objects, one for each column in the matrix,
     *         or an empty stream if the matrix is empty
     */
    @Override
    public Stream<LongStream> columnStreams() {
        return columnStreams(0, columnCount);
    }

    /**
     * Returns a stream of LongStream objects for a range of columns.
     * Each LongStream in the result represents a complete column within the specified range.
     *
     * <p>This method allows for processing a subset of columns
     * while maintaining the ability to work with complete columns as individual streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}});
     * matrix.columnStreams(1, 3).count();     // returns 2 (columns 1 and 2)
     * matrix.columnStreams(0, 2)
     *     .mapToLong(col -> col.sum())
     *     .toArray();                         // returns [5, 7]
     *
     * matrix.columnStreams(1, 1).count();     // returns 0 (empty range)
     * matrix.columnStreams(0, 5);             // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of LongStream objects for the specified column range,
     *         or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
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
     * Returns the length of the given row array.
     * This is a hook called by {@link AbstractMatrix} during construction to determine the column
     * count of each row when validating the rectangular shape of the backing array.
     *
     * @param a the row array to measure; may be {@code null}
     * @return the length of {@code a}, or {@code 0} if {@code a} is {@code null}
     */
    @Override
    protected int length(@SuppressWarnings("hiding") final long[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Performs the specified action for each element in this matrix.
     * Elements are processed in row-major order (row by row, left to right) when executed sequentially.
     *
     * <p>The operation may be parallelized internally for large matrices to improve performance,
     * based on internal heuristics. If parallelized, the order of execution is not guaranteed,
     * but all elements will be processed exactly once. If parallelized, {@code action} must be
     * thread-safe.</p>
     *
     * <p><b>Note:</b> This method is for side-effect operations only (like printing, collecting,
     * or accumulating). For transformations that create new matrices, use {@link #map(Throwables.LongUnaryOperator)}
     * or {@link #updateAll(Throwables.LongUnaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     *
     * java.util.concurrent.atomic.AtomicLong sum = new java.util.concurrent.atomic.AtomicLong();
     * matrix.forEach(sum::addAndGet);
     * sum.get();                              // 10L (sum of all elements)
     *
     * java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger();
     * matrix.forEach(value -> count.incrementAndGet());
     * count.get();                            // 4 (number of elements)
     *
     * java.util.concurrent.atomic.AtomicLong emptySum = new java.util.concurrent.atomic.AtomicLong();
     * LongMatrix.empty().forEach(emptySum::addAndGet);
     * emptySum.get();                                                   // 0 (no elements visited)
     * matrix.forEach((Throwables.LongConsumer<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element; receives each element value
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     * @see #forEach(int, int, int, int, Throwables.LongConsumer)
     */
    public <E extends Exception> void forEach(final Throwables.LongConsumer<E> action) throws E {
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
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L, 3L}, {4L, 5L, 6L}, {7L, 8L, 9L}});
     *
     * long[] center = {0L};
     * matrix.forEach(1, 2, 1, 2, value -> center[0] = value);
     * center[0];                              // 5L (center element only)
     *
     * long[] subSum = {0L};
     * matrix.forEach(0, 2, 1, 3, value -> subSum[0] += value);
     * subSum[0];                              // 16L (2 + 3 + 5 + 6)
     *
     * matrix.forEach(0, 5, 0, 3, value -> { });                                     // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.forEach(0, 2, 0, 2, (Throwables.LongConsumer<RuntimeException>) null); // throws IllegalArgumentException
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
            final Throwables.LongConsumer<E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        if (Matrices.shouldRunInParallel(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, elementAction, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                final long[] currentRow = a[i];

                for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                    action.accept(currentRow[j]);
                }
            }
        }
    }

    /**
     * Renders this matrix as a multi-line string (one row per line, e.g. {@code "[1, 2]\n[3, 4]"}); a
     * zero-row matrix renders {@code "[]"}. Backs {@link #println()} and {@link #appendTo(Appendable)}.
     *
     * @return the formatted multi-line representation of this matrix
     */
    @Override
    String toMultilineString() {
        if (a.length == 0) {
            return "[]";
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

            return str;
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
     * LongMatrix matrix1 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix matrix2 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix1.hashCode() == matrix2.hashCode(); // returns true (equal content)
     *
     * LongMatrix different = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 5L}});
     * matrix1.hashCode() == different.hashCode(); // returns false (different content, typically)
     * LongMatrix.empty().hashCode();              // returns a stable hash for the empty matrix
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
     * Returns {@code true} if the given object is also a {@code LongMatrix} with the same dimensions
     * and all corresponding elements are equal. Returns {@code false} for any other type
     * (including primitive matrices of different element types).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix m1 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * LongMatrix m2 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * m1.equals(m2);                          // returns true (same shape and elements)
     *
     * LongMatrix m3 = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 5L}});
     * m1.equals(m3);                          // returns false (different element)
     * m1.equals(null);                        // returns false
     * m1.equals("not a matrix");              // returns false (different type)
     * }</pre>
     *
     * @param obj the object to compare with; may be {@code null}
     * @return {@code true} if {@code obj} is a {@code LongMatrix} with identical shape and elements,
     *         {@code false} otherwise
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
     * Returns a string representation of this matrix in a compact two-dimensional array format.
     * The output shows all matrix elements with rows enclosed in brackets and
     * elements separated by commas and spaces.
     *
     * <p>The format is suitable for debugging and logging. For pretty-printed output
     * with each row on a separate line, use {@link #println()} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongMatrix matrix = LongMatrix.of(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.toString();                      // returns "[[1, 2], [3, 4]]"
     *
     * LongMatrix single = LongMatrix.of(new long[][] {{7L}});
     * single.toString();                     // returns "[[7]]"
     * LongMatrix.empty().toString();         // returns "[]"
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
