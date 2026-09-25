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
import java.util.random.RandomGenerator;

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
 * validated backing array. The constructor and {@link #wrap(byte[]...)} copy the outer array but
 * share its row arrays. {@link #copyOf(byte[]...)}, conversions, and mapping operations do not share mutable cell
 * storage with a non-empty source; operations producing a {@code 0 x 0} matrix return the shared empty
 * singleton, while {@code 0 x N} and {@code N x 0} results keep their shape.</p>
 *
 * <p>Cells introduced by growth or reshaping default to {@code 0} unless an overload accepts an
 * explicit fill value.</p>
 *
 * <p><b>Byte arithmetic:</b> the built-in byte arithmetic operations ({@link #add(ByteMatrix)},
 * {@link #subtract(ByteMatrix)}, and {@link #matrixMultiply(ByteMatrix)}) use Java's standard
 * numeric promotion to {@code int} and narrow each stored result back to {@code byte} (via an explicit
 * cast for {@code add}/{@code subtract}, or via the implicit narrowing of the {@code +=} accumulation
 * in {@code matrixMultiply}), so values outside {@code [Byte.MIN_VALUE, Byte.MAX_VALUE]} wrap modulo 256.
 * The {@code zipWith}/{@code map} variants instead store whatever {@code byte} the supplied operator
 * returns, so any narrowing of an {@code int} computation must be performed inside the operator
 * itself. Use {@link #addWidened(ByteMatrix)}, {@link #subtractWidened(ByteMatrix)}, or
 * {@link #matrixMultiplyWidened(ByteMatrix)} for widened results, and the corresponding
 * {@code *Exact} methods when a byte result is required without silent wraparound.</p>
 *
 * <p><b>Aggregations:</b> this class does not provide dedicated reduction methods such as
 * {@code sum()}, {@code min()}, {@code max()} or {@code average()}. Compute such aggregations
 * through the streaming API instead &mdash; for example {@code rowMajorStream().sum()} over all
 * elements, or {@code rowStreams()} / {@code columnStreams()} for per-row or per-column reductions.</p>
 *
 * @see IntMatrix
 * @see LongMatrix
 * @see DoubleMatrix
 * @see FloatMatrix
 * @see ShortMatrix
 * @see CharMatrix
 * @see BooleanMatrix
 * @see Matrix
 */
public final class ByteMatrix extends AbstractMatrix<byte[], ByteList, ByteStream, Stream<ByteStream>, ByteMatrix> {

    /** The number of distinct {@code byte} values, used as the bound for full-range random generation. */
    static final int BOUND = Byte.MAX_VALUE - Byte.MIN_VALUE + 1;

    /** The shared {@code 0 x 0} byte matrix returned by {@link #empty()} and public empty-array factories. */
    private static final ByteMatrix EMPTY_BYTE_MATRIX = new ByteMatrix(new byte[0][0]);

    /**
     * Constructs a {@code ByteMatrix} backed by the supplied two-dimensional array.
     *
     * <p><b>&#9888;&#65039; Shared rows:</b> The outer array is copied, but its row arrays are shared.
     * Mutating a row element through either view is visible through the other; replacing a row in the
     * caller's outer array is not. Call {@link #copy()} for independently owned rows.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[][] data = {{1, 2, 3}, {4, 5, 6}};
     * ByteMatrix matrix = new ByteMatrix(data);
     * matrix.get(0, 0);                                  // returns (byte) 1
     * data[0][0] = (byte) 99;                            // also mutates the matrix (no copy)
     * matrix.get(0, 0);                                  // returns (byte) 99
     *
     * new ByteMatrix((byte[][]) null);                   // throws IllegalArgumentException
     *
     * new ByteMatrix(new byte[][] {{1, 2}, {3}});        // throws IllegalArgumentException (not rectangular)
     * }</pre>
     *
     * @param a the two-dimensional byte array whose rows are shared, must not be {@code null}
     * @throws IllegalArgumentException if {@code a} is {@code null}, if any row of {@code a} is {@code null}, or if the rows have
     *         different lengths (i.e. the array is not rectangular), or if two positions reference the same row array
     */
    public ByteMatrix(final byte[][] a) throws IllegalArgumentException {
        super(N.checkArgNotNull(a, "Matrix array cannot be null"), byte.class);
    }

    /**
     * Constructs a matrix with an explicit column count, including a matrix with no rows.
     *
     * @param a the rectangular, non-null array of distinct, non-null row arrays
     * @param columnCount the non-negative logical column count
     * @throws IllegalArgumentException if {@code a} is {@code null}, if {@code columnCount} is negative,
     *         if a row is {@code null} or its length differs from {@code columnCount}, or if two rows are the same array
     */
    ByteMatrix(final byte[][] a, final int columnCount) throws IllegalArgumentException {
        super(N.checkArgNotNull(a, "Matrix array cannot be null"), byte.class, columnCount);
    }

    /**
     * Constructs a matrix with an optional guarantee that its row arrays are distinct.
     *
     * @param a the rectangular, non-null array of non-null row arrays
     * @param columnCount the non-negative logical column count
     * @param rowsAreKnownDistinct whether callers guarantee that no row array is repeated
     * @throws IllegalArgumentException if {@code a} is {@code null}, if {@code columnCount} is negative,
     *         if a row is {@code null} or its length differs from {@code columnCount}, or if rows are repeated
     *         when {@code rowsAreKnownDistinct} is {@code false}
     */
    private ByteMatrix(final byte[][] a, final int columnCount, final boolean rowsAreKnownDistinct) throws IllegalArgumentException {
        super(N.checkArgNotNull(a, "Matrix array cannot be null"), byte.class, columnCount, rowsAreKnownDistinct);
    }

    /**
     * Wraps a derived result whose row arrays are known to be distinct.
     *
     * @param a the non-null rectangular result array with distinct, non-null rows
     * @param columnCount the non-negative logical column count
     * @return the shared empty matrix for a {@code 0 x 0} result, otherwise a new matrix
     * @throws IllegalArgumentException if {@code a} is {@code null}, if {@code columnCount} is negative, if a row is {@code null},
     *         or if a row length differs from {@code columnCount}
     */
    static ByteMatrix wrapResult(final byte[][] a, final int columnCount) throws IllegalArgumentException {
        N.checkArgNotNull(a, cs.a);

        // Every wrapResult(...) call site (here, in Matrix and in Matrices) passes freshly allocated rows, so they are
        // identity-distinct by construction and the duplicate-row scan can be skipped.
        return a.length == 0 && columnCount == 0 ? EMPTY_BYTE_MATRIX : new ByteMatrix(a, columnCount, true);
    }

    /**
     * Returns the shared empty {@code 0x0} matrix instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.empty();
     * matrix.rowCount();                                                // returns 0
     * matrix.columnCount();                                             // returns 0
     * matrix.elementCount();                                            // returns 0L
     * matrix.isEmpty();                                                 // returns true
     * boolean sameSingleton = ByteMatrix.empty() == ByteMatrix.empty(); // true (same shared singleton)
     * }</pre>
     *
     * @return the shared empty {@code ByteMatrix} singleton
     */
    public static ByteMatrix empty() {
        return EMPTY_BYTE_MATRIX;
    }

    /**
     * Wraps the supplied two-dimensional byte array as {@code ByteMatrix}.
     *
     * <p><b>&#9888;&#65039; Shared rows:</b> For a non-empty input, the outer array is copied while its
     * row arrays remain shared. Cell changes are visible in both directions, but later row replacement
     * in the caller's outer array is not. A zero-row input is canonicalized to the shared empty matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.rowCount();                                 // returns 2
     * matrix.get(1, 2);                                  // returns (byte) 6
     *
     * ByteMatrix.wrap((byte[][]) null);                    // throws IllegalArgumentException
     *
     * ByteMatrix none = ByteMatrix.wrap(new byte[0][0]);
     * none.rowCount();                                   // returns 0
     *
     * ByteMatrix.wrap(new byte[][] {{1, 2}, {3}});         // throws IllegalArgumentException (not rectangular)
     * }</pre>
     *
     * @param a the two-dimensional byte array to wrap, or empty for an empty matrix; must not be {@code null}
     * @return a new matrix sharing {@code a}'s rows, or the shared empty matrix if {@code a} is empty
     * @throws IllegalArgumentException if {@code a} is {@code null}, if any row of {@code a} is {@code null}, or if the rows have
     *         different lengths (i.e. the array is not rectangular), or if two positions reference the same row array
     */
    public static ByteMatrix wrap(final byte[]... a) throws IllegalArgumentException {
        N.checkArgNotNull(a, "Matrix array cannot be null");
        return a.length == 0 ? EMPTY_BYTE_MATRIX : new ByteMatrix(a);
    }

    /**
     * Creates a {@code ByteMatrix} that owns a defensive deep copy of the supplied two-dimensional array.
     *
     * <p>For a non-empty input, unlike {@link #wrap(byte[][])}, which copies the outer array but shares the caller's rows,
     * this factory allocates a new outer array and clones every row. Subsequent modifications to {@code a}
     * (or its rows) are therefore <b>not</b> visible through the returned matrix, and vice versa. A zero-row
     * input is canonicalized to the shared empty matrix, so its outer-array identity is not retained.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[][] data = {{1, 2}, {3, 4}};
     * ByteMatrix matrix = ByteMatrix.copyOf(data);
     * data[0][0] = 10;
     * matrix.get(0, 0);                       // returns 1 (copy is independent)
     *
     * ByteMatrix.copyOf((byte[][]) null);            // throws IllegalArgumentException
     * ByteMatrix.copyOf(new byte[][] {{1, 2}, {3}}); // throws IllegalArgumentException (non-rectangular)
     * }</pre>
     *
     * <p>Because every row is cloned, an input that repeats the same row array is accepted here,
     * while {@code wrap(...)} would reject it.</p>
     *
     * @param a the two-dimensional byte array to copy, or empty for an empty matrix; must not be {@code null}
     * @return a new {@code ByteMatrix} backed by a deep copy of {@code a}, or the shared empty matrix if {@code a} is empty
     * @throws IllegalArgumentException if {@code a} is {@code null}, if any row of {@code a} is {@code null}, or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     * @see #wrap(byte[][])
     * @see #copy()
     */
    public static ByteMatrix copyOf(final byte[]... a) throws IllegalArgumentException {
        N.checkArgNotNull(a, "Matrix array cannot be null");

        if (a.length == 0) {
            return EMPTY_BYTE_MATRIX;
        }

        N.checkArgument(a[0] != null, "Row 0 cannot be null");
        final int columnCount = a[0].length;

        for (int i = 1; i < a.length; i++) {
            N.checkArgument(a[i] != null, "Row {} cannot be null", i);
            N.checkArgument(a[i].length == columnCount, MSG_NOT_RECTANGULAR, columnCount, i, a[i].length);
        }

        final byte[][] c = new byte[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            c[i] = a[i] == null ? null : a[i].clone();
        }

        return new ByteMatrix(c);
    }

    /**
     * Creates a new {@code 1 x columnCount} matrix filled with random byte values uniformly distributed
     * across the full byte range {@code [Byte.MIN_VALUE, Byte.MAX_VALUE]}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.randomRow(5);
     * matrix.rowCount();             // returns 1
     * matrix.columnCount();          // returns 5 (values are random in [-128, 127])
     *
     * ByteMatrix none = ByteMatrix.randomRow(0);
     * none.columnCount();            // returns 0 (1x0 matrix)
     *
     * ByteMatrix.randomRow(-1);      // throws IllegalArgumentException (negative columnCount)
     * }</pre>
     *
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code ByteMatrix} of dimensions {@code 1 x columnCount} filled with random values
     * @throws IllegalArgumentException if {@code columnCount} is negative
     * @see #random(int, int)
     */
    public static ByteMatrix randomRow(final int columnCount) throws IllegalArgumentException {
        return randomRow(columnCount, defaultRandomGenerator());
    }

    /**
     * Creates a {@code 1 x columnCount} matrix using the caller-supplied random source.
     * Supplying the source makes generation reproducible when it has a fixed seed.
     *
     * @param columnCount the number of columns; must be non-negative
     * @param randomGenerator the random source; must not be {@code null}
     * @return the generated single-row matrix
     * @throws IllegalArgumentException if {@code columnCount} is negative, or if {@code randomGenerator} is {@code null}
     * @throws RuntimeException if {@code randomGenerator.nextInt(int)} fails while generating a matrix element
     */
    public static ByteMatrix randomRow(final int columnCount, final RandomGenerator randomGenerator) throws IllegalArgumentException, RuntimeException {
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, cs.columnCount, columnCount);

        return random(1, columnCount, randomGenerator);
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
     * ByteMatrix.random(0, 3).columnCount(); // returns 3 (shape is preserved)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code ByteMatrix} of dimensions {@code rowCount x columnCount} filled with random values
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative
     */
    public static ByteMatrix random(final int rowCount, final int columnCount) throws IllegalArgumentException {
        return random(rowCount, columnCount, defaultRandomGenerator());
    }

    /**
     * Creates a matrix of full-range random byte values using the caller-supplied random source.
     *
     * @param rowCount the number of rows; must be non-negative
     * @param columnCount the number of columns; must be non-negative
     * @param randomGenerator the random source; must not be {@code null}
     * @return a matrix with the requested shape, including a shape-preserving {@code 0 x N} result
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative, or if {@code randomGenerator} is {@code null}
     * @throws RuntimeException if {@code randomGenerator.nextInt(int)} fails while generating a matrix element
     */
    public static ByteMatrix random(final int rowCount, final int columnCount, final RandomGenerator randomGenerator)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, cs.rowCount, rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, cs.columnCount, columnCount);
        N.checkArgNotNull(randomGenerator, cs.randomGenerator);
        checkNonNegativeShape(rowCount, columnCount);

        final byte[][] a = new byte[rowCount][columnCount];

        for (byte[] ea : a) {
            for (int i = 0; i < columnCount; i++) {
                ea[i] = (byte) (randomGenerator.nextInt(BOUND) + Byte.MIN_VALUE);
            }
        }

        return wrapResult(a, columnCount);
    }

    /**
     * Creates a single-row {@code ByteMatrix} containing a half-open range of byte values.
     * The range is {@code [startInclusive, endExclusive)} with an implicit step of {@code +1}.
     * If {@code endExclusive <= startInclusive}, a {@code 1x0} matrix is returned.
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
     * @param startInclusive the starting byte value (inclusive)
     * @param endExclusive the ending byte value (exclusive)
     * @return a single-row {@code ByteMatrix} containing the range of values
     */
    public static ByteMatrix range(final byte startInclusive, final byte endExclusive) {
        return new ByteMatrix(new byte[][] { Array.range(startInclusive, endExclusive) });
    }

    /**
     * Creates a single-row {@code ByteMatrix} containing the half-open range
     * {@code [startInclusive, endExclusive)} stepped by {@code step}.
     * Supports both ascending (positive step) and descending (negative step) sequences.
     * If the step direction does not advance from {@code startInclusive} toward {@code endExclusive},
     * a {@code 1x0} matrix is returned.
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
     * @param startInclusive the starting byte value (inclusive)
     * @param endExclusive the ending byte value (exclusive)
     * @param step the step size (must not be zero; can be positive or negative)
     * @return a new {@code 1 × n} {@code ByteMatrix} of values from {@code startInclusive} stepped by {@code step}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static ByteMatrix range(final byte startInclusive, final byte endExclusive, final byte step) throws IllegalArgumentException {
        return new ByteMatrix(new byte[][] { Array.range(startInclusive, endExclusive, step) });
    }

    /**
     * Creates a single-row {@code ByteMatrix} containing a closed range of byte values.
     * The range is {@code [startInclusive, endInclusive]} with an implicit step of {@code +1}.
     * If {@code endInclusive < startInclusive}, a {@code 1x0} matrix is returned.
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
     * @param startInclusive the starting byte value (inclusive)
     * @param endInclusive the ending byte value (inclusive)
     * @return a single-row {@code ByteMatrix} containing the range of values
     */
    public static ByteMatrix rangeClosed(final byte startInclusive, final byte endInclusive) {
        return new ByteMatrix(new byte[][] { Array.rangeClosed(startInclusive, endInclusive) });
    }

    /**
     * Creates a single-row {@code ByteMatrix} containing the closed range
     * {@code [startInclusive, endInclusive]} stepped by {@code step}.
     * Supports both ascending (positive step) and descending (negative step) sequences.
     * The end value is included only if it is reachable by stepping from start. If the step direction
     * does not advance from {@code startInclusive} toward {@code endInclusive}, a {@code 1x0} matrix is returned.
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
     * @param startInclusive the starting byte value (inclusive)
     * @param endInclusive the ending byte value (inclusive, if reachable by stepping)
     * @param step the step size (must not be zero; can be positive or negative)
     * @return a new {@code 1 × n} {@code ByteMatrix} of values from {@code startInclusive} stepped by {@code step}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static ByteMatrix rangeClosed(final byte startInclusive, final byte endInclusive, final byte step) throws IllegalArgumentException {
        return new ByteMatrix(new byte[][] { Array.rangeClosed(startInclusive, endInclusive, step) });
    }

    /**
     * Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
     * All other elements are set to zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.ofMainDiagonal(new byte[] {1, 2, 3});
     * matrix.get(0, 0);                       // returns (byte) 1
     * matrix.get(1, 1);                       // returns (byte) 2
     * matrix.get(0, 1);                       // returns (byte) 0 (off-diagonal)
     * // matrix is [[1, 0, 0], [0, 2, 0], [0, 0, 3]]
     *
     * ByteMatrix.ofMainDiagonal(null);                    // throws IllegalArgumentException (null array)
     * ByteMatrix.ofMainDiagonal(new byte[0]).isEmpty();   // returns true
     * }</pre>
     *
     * @param mainDiagonal the array of main-diagonal elements; must not be {@code null}, but may be empty
     * @return a new {@code n x n} {@code ByteMatrix} (where {@code n = mainDiagonal.length}) with
     *         the supplied values on the main diagonal and {@code 0} elsewhere; the shared empty
     *         matrix if {@code mainDiagonal} is empty
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null}
     * @see #ofAntiDiagonal(byte[])
     * @see #ofDiagonals(byte[], byte[])
     */
    public static ByteMatrix ofMainDiagonal(final byte[] mainDiagonal) throws IllegalArgumentException {
        N.checkArgNotNull(mainDiagonal, cs.mainDiagonal);

        return ofDiagonals(mainDiagonal, null);
    }

    /**
     * Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
     * All other elements are set to zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.ofAntiDiagonal(new byte[] {1, 2, 3});
     * matrix.get(0, 2);                       // returns (byte) 1
     * matrix.get(2, 0);                       // returns (byte) 3
     * matrix.get(0, 0);                       // returns (byte) 0 (off anti-diagonal)
     * // matrix is [[0, 0, 1], [0, 2, 0], [3, 0, 0]]
     *
     * ByteMatrix.ofAntiDiagonal(null);                    // throws IllegalArgumentException (null array)
     * ByteMatrix.ofAntiDiagonal(new byte[0]).isEmpty();   // returns true
     * }</pre>
     *
     * @param antiDiagonal the array of anti-diagonal elements; must not be {@code null}, but may be empty
     * @return a new {@code n x n} {@code ByteMatrix} (where {@code n = antiDiagonal.length}) with
     *         the supplied values on the anti-diagonal and {@code 0} elsewhere; the shared empty
     *         matrix if {@code antiDiagonal} is empty
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null}
     * @see #ofMainDiagonal(byte[])
     * @see #ofDiagonals(byte[], byte[])
     */
    public static ByteMatrix ofAntiDiagonal(final byte[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgNotNull(antiDiagonal, cs.antiDiagonal);

        return ofDiagonals(null, antiDiagonal);
    }

    /**
     * Creates a square matrix from the specified main diagonal and anti-diagonal elements.
     * All other elements are set to zero. At least one array must be non-{@code null}; if both arrays
     * contain elements, they must have the same length.
     * The resulting matrix has dimensions n×n where n is the length of the non-empty diagonal array.
     * When both diagonals are provided and they overlap (at the center element of odd-sized matrices),
     * the main diagonal value takes precedence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.ofDiagonals(new byte[] {1, 2, 3}, new byte[] {4, 5, 6});
     * matrix.get(0, 0);                       // returns (byte) 1 (main diagonal)
     * matrix.get(0, 2);                       // returns (byte) 4 (anti-diagonal)
     * matrix.get(1, 1);                       // returns (byte) 2 (overlap: main takes precedence)
     * // matrix is [[1, 0, 4], [0, 2, 0], [6, 0, 3]]
     *
     * ByteMatrix.ofDiagonals(null, null);                              // throws IllegalArgumentException (both null)
     * ByteMatrix.ofDiagonals(new byte[] {1, 2}, new byte[] {3, 4, 5}); // throws IllegalArgumentException (length mismatch)
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements; may be {@code null} if {@code antiDiagonal} is non-{@code null};
     *        may be empty
     * @param antiDiagonal the array of anti-diagonal elements; may be {@code null} if {@code mainDiagonal} is non-{@code null};
     *        may be empty
     * @return a square matrix with the specified diagonals, or an empty matrix when both supplied diagonals are empty or one is {@code null} and the other is empty
     * @throws IllegalArgumentException if both {@code mainDiagonal} and {@code antiDiagonal} are {@code null}, or if both arrays are non-empty and have different lengths
     * @see #ofMainDiagonal(byte[])
     * @see #ofAntiDiagonal(byte[])
     */
    public static ByteMatrix ofDiagonals(final byte[] mainDiagonal, final byte[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(mainDiagonal != null || antiDiagonal != null, "Both 'mainDiagonal' and 'antiDiagonal' can't be null");

        N.checkArgument(N.isEmpty(mainDiagonal) || N.isEmpty(antiDiagonal) || mainDiagonal.length == antiDiagonal.length,
                "The lengths of 'mainDiagonal' and 'antiDiagonal' must be the same: mainDiagonal length={}, antiDiagonal length={}", N.len(mainDiagonal),
                N.len(antiDiagonal));

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

        return wrapResult(result, len);
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
     * Matrix<Byte> boxed = Matrix.wrap(new Byte[][] {{1, 2}, {null, 4}});
     * ByteMatrix primitive = ByteMatrix.unbox(boxed);
     * primitive.get(0, 1);                               // returns (byte) 2
     * primitive.get(1, 0);                               // returns (byte) 0 (null becomes 0)
     *
     * Matrix<Byte> emptyBoxed = Matrix.wrap(new Byte[0][0]);
     * ByteMatrix.unbox(emptyBoxed).isEmpty();            // returns true
     *
     * ByteMatrix.unbox((Matrix<Byte>) null);             // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the boxed {@code Matrix<Byte>} to convert; must not be {@code null}
     * @return a new {@code ByteMatrix} with primitive byte values
     * @throws IllegalArgumentException if {@code x} is {@code null}
     * @see #boxed()
     */
    public static ByteMatrix unbox(final Matrix<Byte> x) throws IllegalArgumentException {
        N.checkArgNotNull(x, cs.x);

        return x.mapToByte(value -> value == null ? BYTE_0 : value);
    }

    /**
     * Returns the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.get(0, 1);                  // returns (byte) 2
     * matrix.get(1, 0);                  // returns (byte) 3
     * matrix.get(2, 0);                  // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * matrix.get(0, 5);                  // throws ArrayIndexOutOfBoundsException (column out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return the byte element at position {@code (rowIndex, columnIndex)}
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount},
     *         or if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    public byte get(final int rowIndex, final int columnIndex) throws ArrayIndexOutOfBoundsException {
        return a[rowIndex][columnIndex];
    }

    /**
     * Returns the element at the specified point.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.get(Point.of(0, 1));        // returns (byte) 2
     * matrix.get(Point.of(1, 1));        // returns (byte) 4
     * matrix.get(Point.of(5, 0));        // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * matrix.get((Point) null);          // throws IllegalArgumentException (point is null)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @return the byte element at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if {@code point.rowIndex()} is outside {@code [0, rowCount)},
     *         or if {@code point.columnIndex()} is outside {@code [0, columnCount)}
     * @see #get(int, int)
     */
    public byte get(final Point point) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        N.checkArgNotNull(point, cs.point);

        return a[point.rowIndex()][point.columnIndex()];
    }

    /**
     * Sets the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
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
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount},
     *         or if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    public void set(final int rowIndex, final int columnIndex, final byte value) throws ArrayIndexOutOfBoundsException {
        a[rowIndex][columnIndex] = value;
    }

    /**
     * Sets the element at the specified point to the given value.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
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
     * @throws ArrayIndexOutOfBoundsException if {@code point.rowIndex()} is outside {@code [0, rowCount)},
     *         or if {@code point.columnIndex()} is outside {@code [0, columnCount)}
     * @see #set(int, int, byte)
     */
    public void set(final Point point, final byte value) throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        N.checkArgNotNull(point, cs.point);

        a[point.rowIndex()][point.columnIndex()] = value;
    }

    /**
     * Returns the element directly above the specified position, or an empty {@link OptionalByte}
     * if the position is on the top edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalByte} is returned for the top
     * row instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.valueAbove(1, 0).get();        // returns (byte) 1
     * matrix.valueAbove(1, 1).get();        // returns (byte) 2
     *
     * matrix.valueAbove(0, 0).isPresent();  // returns false (top row, no cell above)
     * matrix.valueAbove(2, 0);              // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalByte} containing the element at position {@code (rowIndex - 1, columnIndex)},
     *         or empty if {@code rowIndex == 0}
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount},
     *         or if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    public OptionalByte valueAbove(final int rowIndex, final int columnIndex) throws IndexOutOfBoundsException {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? OptionalByte.empty() : OptionalByte.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, or an empty {@link OptionalByte}
     * if the position is on the bottom edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalByte} is returned for the
     * bottom row instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.valueBelow(0, 0).get();        // returns (byte) 3
     * matrix.valueBelow(0, 1).get();        // returns (byte) 4
     *
     * matrix.valueBelow(1, 0).isPresent();  // returns false (bottom row, no cell below)
     * matrix.valueBelow(2, 0);              // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalByte} containing the element at position {@code (rowIndex + 1, columnIndex)},
     *         or empty if {@code rowIndex == rowCount - 1}
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount},
     *         or if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    public OptionalByte valueBelow(final int rowIndex, final int columnIndex) throws IndexOutOfBoundsException {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? OptionalByte.empty() : OptionalByte.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, or an empty
     * {@link OptionalByte} if the position is on the leftmost edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalByte} is returned for the
     * leftmost column instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.valueLeft(0, 1).get();         // returns (byte) 1
     * matrix.valueLeft(1, 1).get();         // returns (byte) 3
     *
     * matrix.valueLeft(0, 0).isPresent();   // returns false (leftmost column, no cell to the left)
     * matrix.valueLeft(0, 2);               // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalByte} containing the element at position {@code (rowIndex, columnIndex - 1)},
     *         or empty if {@code columnIndex == 0}
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount},
     *         or if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    public OptionalByte valueLeft(final int rowIndex, final int columnIndex) throws IndexOutOfBoundsException {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? OptionalByte.empty() : OptionalByte.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, or an empty
     * {@link OptionalByte} if the position is on the rightmost edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalByte} is returned for the
     * rightmost column instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.valueRight(0, 0).get();        // returns (byte) 2
     * matrix.valueRight(1, 0).get();        // returns (byte) 4
     *
     * matrix.valueRight(0, 1).isPresent();  // returns false (rightmost column, no cell to the right)
     * matrix.valueRight(0, 2);              // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalByte} containing the element at position {@code (rowIndex, columnIndex + 1)},
     *         or empty if {@code columnIndex == columnCount - 1}
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount},
     *         or if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    public OptionalByte valueRight(final int rowIndex, final int columnIndex) throws IndexOutOfBoundsException {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? OptionalByte.empty() : OptionalByte.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as a live reference to the underlying {@code byte[]} storage.
     *
     * <p><b>&#9888;&#65039; Live view:</b> This method returns the internal array, not a copy. Modifications to the
     * returned array will affect the matrix and vice versa. Use {@link #rowCopy(int)} if you need
     * an independent copy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.rowView(0);                      // returns [1, 2, 3]
     * matrix.rowView(1);                      // returns [4, 5, 6]
     *
     * byte[] firstRow = matrix.rowView(0);
     * firstRow[0] = (byte) 10;
     * matrix.get(0, 0);                       // returns (byte) 10 (live view is shared)
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
    public byte[] rowView(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return a[rowIndex];
    }

    /**
     * Returns a defensive copy of the specified row as a new {@code byte[]}.
     * Changes to the returned array do not affect this matrix and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.rowCopy(0);                      // returns [1, 2, 3]
     * matrix.rowCopy(1);                      // returns [4, 5, 6]
     *
     * byte[] firstRow = matrix.rowCopy(0);
     * firstRow[0] = (byte) 10;
     * matrix.get(0, 0);                       // returns (byte) 1 (copy is independent)
     *
     * matrix.rowCopy(-1);                     // throws IndexOutOfBoundsException
     * matrix.rowCopy(2);                      // throws IndexOutOfBoundsException (rowIndex >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new byte array of length {@code columnCount} containing the values of the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowView(int)
     * @see #columnCopy(int)
     */
    @Override
    public byte[] rowCopy(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return N.copyOf(a[rowIndex], columnCount);
    }

    /**
     * Returns a defensive copy of the specified column as a new {@code byte[]}.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously in memory. Modifications to the returned array
     * will not affect the matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.columnCopy(0);                   // returns [1, 4]
     * matrix.columnCopy(2);                   // returns [3, 6]
     *
     * byte[] firstColumn = matrix.columnCopy(0);
     * firstColumn[0] = (byte) 10;
     * matrix.get(0, 0);                       // returns (byte) 1 (copy is independent)
     *
     * matrix.columnCopy(-1);                  // throws IndexOutOfBoundsException
     * matrix.columnCopy(3);                   // throws IndexOutOfBoundsException (columnIndex >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new byte array of length {@code rowCount} containing the values of the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     * @see #rowCopy(int)
     * @see #rowView(int)
     */
    @Override
    public byte[] columnCopy(final int columnIndex) throws IndexOutOfBoundsException {
        checkColumnIndex(columnIndex);

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
     * The source array must have exactly the same length as the number of columns in the matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.setRow(0, new byte[] {7, 8, 9});
     * matrix.rowCopy(0);                      // returns [7, 8, 9]
     *
     * matrix.setRow(1, new byte[] {0, 0, 0});
     * matrix.get(1, 1);                       // returns (byte) 0
     *
     * matrix.setRow(0, new byte[] {1, 2});     // throws IllegalArgumentException (length != columnCount)
     * matrix.setRow(5, new byte[] {1, 2, 3});  // throws IndexOutOfBoundsException (rowIndex out of bounds)
     * }</pre>
     *
     * @param rowIndex the index of the row to set (0-based)
     * @param row the array of values to copy into the row; must be non-{@code null} and of length {@code columnCount}
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @throws IllegalArgumentException if {@code row} is {@code null} or if {@code row.length != columnCount}
     */
    public void setRow(final int rowIndex, final byte[] row) throws IndexOutOfBoundsException, IllegalArgumentException {
        checkRowIndex(rowIndex);
        N.checkArgNotNull(row, cs.row);
        N.checkArgument(row.length == columnCount, MSG_ROW_LENGTH_MISMATCH, columnCount, row.length);

        N.copy(row, 0, a[rowIndex], 0, columnCount);
    }

    /**
     * Sets the values of the specified column by copying from the provided array.
     * All elements in the column are replaced with values from the provided array.
     *
     * <p>The values from the source array are copied into the matrix column.
     * The source array must have exactly the same length as the number of rows in the matrix.</p>
     *
     * <p>If the supplied array is one of this matrix's live backing rows (for example a value returned
     * by {@code rowView(int)}), it is snapshotted first, so the values read are the ones in place when
     * this method was called.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.setColumn(0, new byte[] {7, 8});
     * matrix.columnCopy(0);                   // returns [7, 8]
     *
     * matrix.setColumn(2, new byte[] {0, 0});
     * matrix.get(1, 2);                       // returns (byte) 0
     *
     * matrix.setColumn(0, new byte[] {1, 2, 3}); // throws IllegalArgumentException (length != rowCount)
     * matrix.setColumn(5, new byte[] {1, 2});    // throws IndexOutOfBoundsException (columnIndex out of bounds)
     * }</pre>
     *
     * @param columnIndex the index of the column to set (0-based)
     * @param column the array of values to copy into the column; must be non-{@code null} and of length {@code rowCount}
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     * @throws IllegalArgumentException if {@code column} is {@code null} or if {@code column.length != rowCount}
     */
    public void setColumn(final int columnIndex, final byte[] column) throws IndexOutOfBoundsException, IllegalArgumentException {
        checkColumnIndex(columnIndex);
        N.checkArgNotNull(column, cs.column);
        N.checkArgument(column.length == rowCount, MSG_COLUMN_LENGTH_MISMATCH, rowCount, column.length);
        final byte[] values = snapshotIfBackingRow(column);

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = values[i];
        }
    }

    /**
     * Updates all elements in a row in-place by applying the specified operator to each element.
     * This modifies the matrix directly.
     *
     * <p>The operator is applied to each element in the specified row sequentially
     * from left to right (column {@code 0} to column {@code columnCount - 1}).</p>
     *
     * <p>If {@code operator} throws, updates already completed are retained; this method does not
     * roll back changes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.updateRow(0, b -> (byte) (b * 2));
     * matrix.rowView(0);                          // returns [2, 4, 6]
     *
     * matrix.updateRow(1, b -> (byte) (b + 10));
     * matrix.rowView(1);                          // returns [14, 15, 16]
     *
     * matrix.updateRow(5, b -> b);               // throws IndexOutOfBoundsException (row out of bounds)
     * matrix.updateRow(0, null);                 // throws IllegalArgumentException (operator is null)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param rowIndex the index of the row to update (0-based)
     * @param operator the operator to apply to each element in the row; receives the current
     *             element value and returns the new value
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if {@code operator} throws while transforming a matrix element
     */
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.ByteUnaryOperator<E> operator)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        checkRowIndex(rowIndex);
        N.checkArgNotNull(operator, cs.operator);

        final byte[] row = a[rowIndex];

        for (int i = 0; i < columnCount; i++) {
            row[i] = operator.applyAsByte(row[i]);
        }
    }

    /**
     * Updates all elements in a column in-place by applying the specified operator to each element.
     * This modifies the matrix directly.
     *
     * <p>The operator is applied to each element in the specified column sequentially
     * from top to bottom (row {@code 0} to row {@code rowCount - 1}).</p>
     *
     * <p>If {@code operator} throws, updates already completed are retained; this method does not
     * roll back changes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.updateColumn(1, b -> (byte) (b + 10));
     * matrix.columnCopy(1);                       // returns [12, 15]
     *
     * matrix.updateColumn(0, b -> (byte) (b * 0));
     * matrix.columnCopy(0);                       // returns [0, 0]
     *
     * matrix.updateColumn(9, b -> b);            // throws IndexOutOfBoundsException (column out of bounds)
     * matrix.updateColumn(0, null);              // throws IllegalArgumentException (operator is null)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param columnIndex the index of the column to update (0-based)
     * @param operator the operator to apply to each element in the column; receives the current
     *             element value and returns the new value
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if {@code operator} throws while transforming a matrix element
     */
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.ByteUnaryOperator<E> operator)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        checkColumnIndex(columnIndex);
        N.checkArgNotNull(operator, cs.operator);

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = operator.applyAsByte(a[i][columnIndex]);
        }
    }

    /**
     * Returns a copy of the main diagonal elements (upper-left to lower-right) as an array.
     * Rectangular matrices are supported; the returned length is {@code min(rowCount, columnCount)}.
     *
     * <p>This method extracts the main diagonal elements at positions (0,0), (1,1), (2,2), etc.
     * The returned array is a copy; modifications to it will not affect the matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.mainDiagonalCopy();                   // returns [1, 5, 9]
     *
     * ByteMatrix small = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * small.mainDiagonalCopy();                    // returns [1, 4]
     *
     * ByteMatrix.empty().mainDiagonalCopy();       // returns []
     * ByteMatrix nonSquare = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.mainDiagonalCopy();                // returns [1, 5]
     * }</pre>
     *
     * @return a new byte array containing a copy of the main diagonal elements
     */
    @Override
    public byte[] mainDiagonalCopy() {
        final int diagonalLength = diagonalLength();
        final byte[] res = new byte[diagonalLength];

        for (int i = 0; i < diagonalLength; i++) {
            res[i] = a[i][i]; // NOSONAR
        }

        return res;
    }

    /**
     * Sets the elements on the main diagonal (upper-left to lower-right).
     * The diagonal array must have exactly {@code min(rowCount, columnCount)} elements.
     *
     * <p>This method sets the main diagonal elements at positions (0,0), (1,1), (2,2), etc.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.setMainDiagonal(new byte[] {9, 8});
     * matrix.mainDiagonalCopy();              // returns [9, 8]
     * matrix.get(1, 1);                       // returns (byte) 8 (diagonal element updated)
     *
     * matrix.setMainDiagonal(new byte[] {1}); // throws IllegalArgumentException (wrong length)
     * ByteMatrix nonSquare = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.setMainDiagonal(new byte[] {1, 2}); // supported for a 2x3 matrix
     * }</pre>
     *
     * @param mainDiagonal the new values; length must equal {@code min(rowCount, columnCount)}
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null} or its length differs from
     *         {@code min(rowCount, columnCount)}
     */
    @Override
    public void setMainDiagonal(final byte[] mainDiagonal) throws IllegalArgumentException {
        N.checkArgNotNull(mainDiagonal, cs.mainDiagonal);
        final int diagonalLength = diagonalLength();
        N.checkArgument(mainDiagonal.length == diagonalLength, MSG_DIAGONAL_LENGTH_MISMATCH, diagonalLength, mainDiagonal.length);

        for (int i = 0; i < diagonalLength; i++) {
            a[i][i] = mainDiagonal[i];
        }
    }

    /**
     * Updates the values on the main diagonal (upper-left to lower-right) by applying the specified operator.
     * Rectangular matrices are supported; exactly {@code min(rowCount, columnCount)} cells are updated.
     *
     * <p>If {@code operator} throws, updates already completed are retained; this method does not
     * roll back changes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.updateMainDiagonal(b -> (byte) (b * b));
     * matrix.mainDiagonalCopy();                   // returns [1, 16]
     * matrix.get(0, 1);                            // returns (byte) 2 (off-diagonal unchanged)
     *
     * matrix.updateMainDiagonal(null);            // throws IllegalArgumentException (operator is null)
     * ByteMatrix nonSquare = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.updateMainDiagonal(b -> b);       // supported for a rectangular matrix
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each diagonal element; receives current element value and returns new value
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if {@code operator} throws while transforming a matrix element
     */
    public <E extends Exception> void updateMainDiagonal(final Throwables.ByteUnaryOperator<E> operator) throws IllegalArgumentException, E {
        N.checkArgNotNull(operator, cs.operator);

        final int diagonalLength = diagonalLength();

        for (int i = 0; i < diagonalLength; i++) {
            a[i][i] = operator.applyAsByte(a[i][i]);
        }
    }

    /**
     * Returns a copy of the anti-diagonal elements (upper-right to lower-left) as an array.
     * Rectangular matrices are supported; the returned length is {@code min(rowCount, columnCount)}.
     *
     * <p>This method extracts the anti-diagonal (secondary diagonal) elements from
     * upper-right toward the lower-left, beginning at {@code (0,columnCount-1)}.
     * The returned array is a copy; modifications to it will not affect the matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.antiDiagonalCopy();                   // returns [3, 5, 7]
     *
     * ByteMatrix small = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * small.antiDiagonalCopy();                    // returns [2, 3]
     *
     * ByteMatrix.empty().antiDiagonalCopy();       // returns []
     * ByteMatrix nonSquare = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.antiDiagonalCopy();                // returns [3, 5]
     * }</pre>
     *
     * @return a new byte array containing a copy of the anti-diagonal elements
     */
    @Override
    public byte[] antiDiagonalCopy() {
        final int diagonalLength = diagonalLength();
        final byte[] res = new byte[diagonalLength];

        for (int i = 0; i < diagonalLength; i++) {
            res[i] = a[i][columnCount - i - 1];
        }

        return res;
    }

    /**
     * Sets the elements on the anti-diagonal (upper-right to lower-left).
     * The diagonal array must have exactly {@code min(rowCount, columnCount)} elements.
     *
     * <p>This method sets the anti-diagonal (secondary diagonal) elements from
     * top-right toward the lower-left, beginning at {@code (0,columnCount-1)}.</p>
     *
     * <p>If the supplied array is one of this matrix's live backing rows (for example a value returned
     * by {@code rowView(int)}), it is snapshotted first, so the values read are the ones in place when
     * this method was called.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.setAntiDiagonal(new byte[] {9, 8});
     * matrix.antiDiagonalCopy();              // returns [9, 8]
     * matrix.get(0, 1);                       // returns (byte) 9 (anti-diagonal cell)
     *
     * matrix.setAntiDiagonal(new byte[] {1}); // throws IllegalArgumentException (wrong length)
     * ByteMatrix nonSquare = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.setAntiDiagonal(new byte[] {1, 2}); // supported for a 2x3 matrix
     * }</pre>
     *
     * @param antiDiagonal the new values; length must equal {@code min(rowCount, columnCount)}
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null} or its length differs from
     *         {@code min(rowCount, columnCount)}
     */
    @Override
    public void setAntiDiagonal(final byte[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgNotNull(antiDiagonal, cs.antiDiagonal);
        final int diagonalLength = diagonalLength();
        N.checkArgument(antiDiagonal.length == diagonalLength, MSG_DIAGONAL_LENGTH_MISMATCH, diagonalLength, antiDiagonal.length);
        final byte[] values = snapshotIfBackingRow(antiDiagonal);

        for (int i = 0; i < diagonalLength; i++) {
            a[i][columnCount - i - 1] = values[i];
        }
    }

    /**
     * Updates the values on the anti-diagonal (upper-right to lower-left) by applying the specified operator.
     * Rectangular matrices are supported; exactly {@code min(rowCount, columnCount)} cells are updated.
     *
     * <p>If {@code operator} throws, updates already completed are retained; this method does not
     * roll back changes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.updateAntiDiagonal(b -> (byte) -b);
     * matrix.antiDiagonalCopy();                   // returns [-2, -3]
     * matrix.get(0, 0);                            // returns (byte) 1 (off anti-diagonal unchanged)
     *
     * matrix.updateAntiDiagonal(null);            // throws IllegalArgumentException (operator is null)
     * ByteMatrix nonSquare = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.updateAntiDiagonal(b -> b);       // supported for a rectangular matrix
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each anti-diagonal element; receives current element value and returns new value
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if {@code operator} throws while transforming a matrix element
     */
    public <E extends Exception> void updateAntiDiagonal(final Throwables.ByteUnaryOperator<E> operator) throws IllegalArgumentException, E {
        N.checkArgNotNull(operator, cs.operator);

        final int diagonalLength = diagonalLength();

        for (int i = 0; i < diagonalLength; i++) {
            a[i][columnCount - i - 1] = operator.applyAsByte(a[i][columnCount - i - 1]);
        }
    }

    /**
     * Updates all elements in the matrix in-place by applying the specified operator.
     * This modifies the matrix directly.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance. If parallelized, the supplied function must be thread-safe.
     * When this operation is not parallelized, elements are processed in row-major order; when it is
     * parallelized, the encounter order is unspecified.</p>
     *
     * <p>If {@code operator} throws, updates already completed are retained; this method does not
     * roll back changes. Sequential traversal stops at the failing call. During parallel execution,
     * other callbacks may already be running when a failure occurs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.updateAll(b -> (byte) (b * 2));
     * matrix.rowView(0);                          // returns [2, 4]
     * matrix.rowView(1);                          // returns [6, 8]
     *
     * // Byte overflow wraps modulo 256
     * ByteMatrix big = ByteMatrix.wrap(new byte[][] {{127}});
     * big.updateAll(b -> (byte) (b + 1));
     * big.get(0, 0);                              // returns (byte) -128 (127 + 1 wraps)
     *
     * ByteMatrix.empty().updateAll(b -> (byte) 9);                               // no-op on empty matrix (no elements)
     * matrix.updateAll((Throwables.ByteUnaryOperator<RuntimeException>) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each element; receives the current element value
     *             and returns the new value
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if {@code operator} throws while transforming a matrix element
     */
    public <E extends Exception> void updateAll(final Throwables.ByteUnaryOperator<E> operator) throws IllegalArgumentException, E {
        N.checkArgNotNull(operator, cs.operator);

        if (columnCount == 0) {
            return;
        }

        if (Matrices.shouldRunInParallel(this)) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = operator.applyAsByte(a[i][j]);
            Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
        } else {
            for (final byte[] row : a) {
                for (int j = 0; j < columnCount; j++) {
                    row[j] = operator.applyAsByte(row[j]);
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
     * mathematical formulas. The operation may be performed in parallel for large matrices. If parallelized, the supplied function must be thread-safe.</p>
     *
     * <p>If {@code mapper} throws, updates already completed are retained; this method does not
     * roll back changes. Sequential traversal stops at the failing call. During parallel execution,
     * other callbacks may already be running when a failure occurs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{0, 0}, {0, 0}});
     * matrix.updateAll((i, j) -> (byte) (i + j));
     * matrix.rowView(0);                          // returns [0, 1]
     * matrix.rowView(1);                          // returns [1, 2]
     *
     * ByteMatrix idx = ByteMatrix.wrap(new byte[][] {{0, 0, 0}});
     * idx.updateAll((i, j) -> (byte) j);
     * idx.rowView(0);                             // returns [0, 1, 2]
     *
     * ByteMatrix.empty().updateAll((i, j) -> (byte) 9);                            // no-op on empty matrix
     * matrix.updateAll((Throwables.IntBiFunction<Byte, RuntimeException>) null);   // throws IllegalArgumentException
     * matrix.updateAll((i, j) -> (Byte) null);                                     // throws NullPointerException (null auto-unboxing)
     * }</pre>
     *
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the function that receives row index and column index (0-based) and returns
     *             the new value for that position; the returned {@code Byte} is unboxed, so it
     *             must not be {@code null}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if {@code mapper} throws while mapping matrix coordinates
     * @throws NullPointerException if {@code mapper} returns {@code null} for any position
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Byte, E> mapper)
            throws IllegalArgumentException, E, NullPointerException {
        N.checkArgNotNull(mapper, cs.mapper);

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = mapper.apply(i, j);
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));
    }

    /**
     * Conditionally replaces elements in-place based on a predicate.
     * All elements that satisfy the predicate are replaced with the specified new value.
     * This modifies the matrix directly.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance. If parallelized, the supplied function must be thread-safe.</p>
     *
     * <p>If {@code predicate} throws, updates already completed are retained; this method does not
     * roll back changes. Sequential traversal stops at the failing call. During parallel execution,
     * other callbacks may already be running when a failure occurs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{-1, 2, -3}, {4, -5, 6}});
     * matrix.replaceIf(b -> b < 0, (byte) 0);
     * matrix.get(0, 0);                       // returns (byte) 0 (-1 replaced)
     * matrix.get(1, 0);                       // returns (byte) 4 (unchanged)
     *
     * matrix.replaceIf(b -> b == 0, (byte) 99);
     * matrix.get(0, 2);                       // returns (byte) 99 (was 0)
     *
     * ByteMatrix.empty().replaceIf(b -> true, (byte) 1);                             // no-op on empty matrix
     * matrix.replaceIf((Throwables.BytePredicate<RuntimeException>) null, (byte) 0); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to test each element; elements for which this returns
     *                  {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if {@code predicate} throws while testing a matrix element
     */
    public <E extends Exception> void replaceIf(final Throwables.BytePredicate<E> predicate, final byte newValue) throws IllegalArgumentException, E {
        N.checkArgNotNull(predicate, cs.predicate);

        if (Matrices.shouldRunInParallel(this)) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> {
                if (predicate.test(a[i][j])) {
                    a[i][j] = newValue;
                }
            };
            Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
        } else {
            for (final byte[] row : a) {
                for (int j = 0; j < columnCount; j++) {
                    if (predicate.test(row[j])) {
                        row[j] = newValue;
                    }
                }
            }
        }
    }

    /**
     * Conditionally replaces elements in-place based on their position (row and column indices).
     * Elements at positions that satisfy the predicate are replaced with the specified new value.
     * This modifies the matrix directly.
     *
     * <p>This is useful for position-based replacements such as setting diagonals, borders,
     * or specific regions. The operation may be performed in parallel for large matrices. If parallelized, the supplied function must be thread-safe.</p>
     *
     * <p>Nonmatching positions perform no write.</p>
     *
     * <p>If {@code predicate} throws, updates already completed are retained; this method does not
     * roll back changes. Sequential traversal stops at the failing call. During parallel execution,
     * other callbacks may already be running when a failure occurs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.replaceIf((i, j) -> i == j, (byte) 0);
     * matrix.get(0, 0);                       // returns (byte) 0 (diagonal)
     * matrix.get(0, 1);                       // returns (byte) 2 (unchanged)
     *
     * matrix.replaceIf((i, j) -> i == 0 || j == 0, (byte) -1);
     * matrix.get(0, 1);                       // returns (byte) -1 (first row)
     * matrix.get(2, 0);                       // returns (byte) -1 (first column)
     *
     * ByteMatrix.empty().replaceIf((i, j) -> true, (byte) 1);                          // no-op on empty matrix
     * matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, (byte) 0);  // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition that tests row index and column index (0-based); elements
     *                  at positions for which this returns {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if {@code predicate} throws while testing matrix coordinates
     */
    public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final byte newValue) throws IllegalArgumentException, E {
        N.checkArgNotNull(predicate, cs.predicate);

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> {
            if (predicate.test(i, j)) {
                a[i][j] = newValue;
            }
        };
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));
    }

    /**
     * Creates a new ByteMatrix by applying a transformation function to each element.
     * The original matrix is not modified; a new matrix with transformed values is returned.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance. If parallelized, the supplied function must be thread-safe.
     * This is the non-mutating counterpart to {@link #updateAll(Throwables.ByteUnaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix doubled = matrix.map(b -> (byte) (b * 2));
     * doubled.get(1, 1);                      // returns (byte) 8
     * matrix.get(1, 1);                       // returns (byte) 4 (original unchanged)
     *
     * // Byte overflow wraps modulo 256
     * ByteMatrix big = ByteMatrix.wrap(new byte[][] {{100}});
     * big.map(b -> (byte) (b + 50)).get(0, 0);   // returns (byte) -106 (150 wraps)
     *
     * ByteMatrix.empty().map(b -> b).isEmpty();                          // returns true
     * matrix.map((Throwables.ByteUnaryOperator<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to apply to each element; receives the current element value
     *             and returns the transformed value
     * @return a new ByteMatrix with transformed values
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if {@code mapper} throws while mapping a matrix element
     * @see #updateAll(Throwables.ByteUnaryOperator)
     */
    public <E extends Exception> ByteMatrix map(final Throwables.ByteUnaryOperator<E> mapper) throws IllegalArgumentException, E {
        N.checkArgNotNull(mapper, cs.mapper);

        final byte[][] result = new byte[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsByte(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return wrapResult(result, columnCount);
    }

    /**
     * Creates a new Matrix by applying a function that converts byte values to objects of type R.
     * This operation may be executed in parallel for better performance on large matrices. If parallelized, the supplied function must be thread-safe.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * Matrix<String> stringMatrix = matrix.mapToObj(b -> String.valueOf(b), String.class);
     * stringMatrix.get(0, 1);                 // returns "2"
     * stringMatrix.get(1, 0);                 // returns "3"
     *
     * Matrix<String> labeled = matrix.mapToObj(b -> "v" + b, String.class);
     * labeled.get(0, 0);                      // returns "v1"
     *
     * ByteMatrix.empty().mapToObj(b -> "" + b, String.class).isEmpty();                         // returns true
     * matrix.mapToObj((Throwables.ByteFunction<String, RuntimeException>) null, String.class);  // throws IllegalArgumentException
     * }</pre>
     *
     * @param <R> the element type of the resulting matrix
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to convert byte values to type {@code R}
     * @param targetElementType the {@code Class} object for type {@code R} (used to allocate the
     *        {@code R[][]} backing array); must not be {@code null} or {@code void.class}
     * @return a new {@link Matrix Matrix&lt;R&gt;} containing the mapped values
     * @throws IllegalArgumentException if {@code mapper} or {@code targetElementType} is {@code null},
     *         or if {@code targetElementType} is {@code void.class} or an array type with 254 or more dimensions
     * @throws E if {@code mapper} throws while mapping a matrix element
     * @throws ArrayStoreException if a mapped value cannot be stored in an array of {@code targetElementType}
     *         (or its wrapper type for a primitive class)
     */
    public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.ByteFunction<? extends R, E> mapper, final Class<R> targetElementType)
            throws IllegalArgumentException, E, ArrayStoreException {
        N.checkArgNotNull(mapper, cs.mapper);
        N.checkArgNotNull(targetElementType, cs.targetElementType);

        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.apply(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return new Matrix<>(result, targetElementType, columnCount);
    }

    /**
     * Fills all elements of the matrix with the specified value.
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.fill((byte) 5);
     * matrix.get(0, 0);                       // returns (byte) 5
     * matrix.get(1, 1);                       // returns (byte) 5
     *
     * matrix.fill((byte) 0);
     * matrix.get(0, 1);                       // returns (byte) 0
     *
     * matrix.fill(Byte.MIN_VALUE);
     * matrix.get(0, 0);                      // returns (byte) -128 (boundary value)
     * ByteMatrix.empty().fill((byte) 7);     // no-op on empty matrix
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
     * Copies values into this matrix from another two-dimensional array, starting at position {@code (0, 0)}.
     * Equivalent to {@code copyFrom(0, 0, source)}.
     * The source array can be smaller than this matrix; only the overlapping region is copied.
     * If the source array is larger, only the portion that fits is copied. {@code null} rows in
     * {@code source} are skipped (the corresponding row of this matrix is left unchanged).
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{0, 0, 0}, {0, 0, 0}});
     * matrix.copyFrom(new byte[][] {{1, 2}, {3, 4}});
     * matrix.get(0, 0);                       // returns (byte) 1
     * matrix.get(0, 2);                       // returns (byte) 0 (source row is narrower, so this column is not overwritten)
     * // matrix is [[1, 2, 0], [3, 4, 0]]
     *
     * ByteMatrix big = ByteMatrix.wrap(new byte[][] {{0, 0}, {0, 0}});
     * big.copyFrom(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * big.get(1, 1);                          // returns (byte) 5 (only overlapping region copied)
     *
     * matrix.copyFrom((byte[][]) null);           // throws IllegalArgumentException (source is null)
     * }</pre>
     *
     * @param source the two-dimensional array to copy values from; must not be {@code null}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @see #copyFrom(int, int, byte[][])
     */
    public void copyFrom(final byte[][] source) throws IllegalArgumentException {
        copyFrom(0, 0, source);
    }

    /**
     * Copies values into a region of this matrix from another two-dimensional array, starting at the
     * specified destination position.
     * The source array can extend beyond this matrix's bounds; only the overlapping region is copied.
     * The matrix is modified in-place. {@code null} rows in {@code source} are skipped (the
     * corresponding destination row is left unchanged). Elements outside the matrix bounds are ignored.
     * Any source row that aliases this matrix's backing storage is snapshotted before copying, so
     * overlapping copies read the source values as they were when this method was called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}});
     * matrix.copyFrom(1, 1, new byte[][] {{1, 2}, {3, 4}});
     * matrix.get(1, 1);                       // returns (byte) 1
     * matrix.get(2, 2);                       // returns (byte) 4
     * matrix.get(0, 0);                       // returns (byte) 0 (outside the copied region)
     * // matrix is [[0, 0, 0], [0, 1, 2], [0, 3, 4]]
     *
     * matrix.copyFrom(0, 0, (byte[][]) null);                // throws IllegalArgumentException (source is null)
     * matrix.copyFrom(-1, 0, new byte[][] {{1}});            // throws IndexOutOfBoundsException (destRowIndex < 0)
     * matrix.copyFrom(0, 5, new byte[][] {{1}});             // throws IndexOutOfBoundsException (destColumnIndex > columnCount)
     * }</pre>
     *
     * @param destRowIndex the target row index in this matrix (0-based, must satisfy {@code 0 <= destRowIndex <= rowCount})
     * @param destColumnIndex the target column index in this matrix (0-based, must satisfy {@code 0 <= destColumnIndex <= columnCount})
     * @param source the source array to copy values from; must not be {@code null}
     * @throws IndexOutOfBoundsException if {@code destRowIndex < 0} or {@code destRowIndex > rowCount},
     *         or if {@code destColumnIndex < 0} or {@code destColumnIndex > columnCount}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void copyFrom(final int destRowIndex, final int destColumnIndex, final byte[][] source) throws IndexOutOfBoundsException, IllegalArgumentException {
        if (destRowIndex < 0 || destRowIndex > rowCount) {
            throw new IndexOutOfBoundsException(formatMsg("destRowIndex({}) must be in [0, rowCount({})]", destRowIndex, rowCount));
        }
        if (destColumnIndex < 0 || destColumnIndex > columnCount) {
            throw new IndexOutOfBoundsException(formatMsg("destColumnIndex({}) must be in [0, columnCount({})]", destColumnIndex, columnCount));
        }
        N.checkArgNotNull(source, cs.source);
        final byte[][] sourceSnapshot = snapshotRowsIfBackingRows(source);

        for (int i = 0, minLen = N.min(rowCount - destRowIndex, sourceSnapshot.length); i < minLen; i++) {
            if (sourceSnapshot[i] != null) {
                N.copy(sourceSnapshot[i], 0, a[i + destRowIndex], destColumnIndex, N.min(sourceSnapshot[i].length, columnCount - destColumnIndex));
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
     * ByteMatrix original = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix copy = original.copy();
     * copy.get(0, 0);                         // returns (byte) 1
     * copy.equals(original);                  // returns true
     *
     * copy.set(0, 0, (byte) 99);
     * original.get(0, 0);                     // returns (byte) 1 (original unchanged)
     * copy.get(0, 0);                         // returns (byte) 99 (copy modified)
     *
     * ByteMatrix.empty().copy().isEmpty();    // returns true
     * }</pre>
     *
     * @return a new matrix that is a copy of this matrix with full independence guarantee
     */
    @Override
    public ByteMatrix copy() {
        final byte[][] c = new byte[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[i].clone();
        }

        return wrapResult(c, columnCount);
    }

    /**
     * Creates a copy of a row range from this matrix.
     * The returned matrix contains only the specified rows and is completely independent from the original matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}, {5, 6}});
     * ByteMatrix subset = matrix.copyRows(1, 3);
     * subset.rowCount();                      // returns 2
     * subset.get(0, 0);                       // returns (byte) 3 -> {{3, 4}, {5, 6}}
     *
     * matrix.copyRows(1, 1).rowCount();          // returns 0 (empty range)
     *
     * matrix.copyRows(-1, 2);                     // throws IndexOutOfBoundsException (fromRowIndex < 0)
     * matrix.copyRows(0, 5);                      // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new {@code ByteMatrix} containing the specified rows
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public ByteMatrix copyRows(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        final byte[][] c = new byte[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = a[i].clone();
        }

        return wrapResult(c, columnCount);
    }

    /**
     * Creates a copy of a submatrix defined by row and column ranges.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * ByteMatrix submatrix = matrix.copyRegion(0, 2, 1, 3);
     * submatrix.get(0, 0);                    // returns (byte) 2
     * submatrix.get(1, 1);                    // returns (byte) 6 -> {{2, 3}, {5, 6}}
     *
     * matrix.copyRegion(0, 1, 0, 1).get(0, 0);     // returns (byte) 1 (single-cell submatrix)
     *
     * matrix.copyRegion(0, 2, 1, 5);               // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.copyRegion(-1, 2, 0, 2);              // throws IndexOutOfBoundsException (fromRowIndex < 0)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new {@code ByteMatrix} containing the specified submatrix
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         {@code fromRowIndex > toRowIndex}, {@code fromColumnIndex < 0},
     *         {@code toColumnIndex > columnCount}, or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    public ByteMatrix copyRegion(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        final byte[][] c = new byte[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = N.copyOfRange(a[i], fromColumnIndex, toColumnIndex);
        }

        return wrapResult(c, toColumnIndex - fromColumnIndex);
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
     * <p>The original matrix is never modified; the result has independent cell storage.</p>
     *
     * <p><b>Comparison with {@link #pad(int, int, int, int)}:</b>
     * {@code resize} takes <em>absolute</em> target dimensions and may truncate existing content.
     * {@code pad} takes <em>relative</em> padding amounts per edge and <em>never truncates</em>.
     * Use {@code pad} when the entire original content must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     *
     * // Grow: both dimensions larger — new cells filled with 0
     * ByteMatrix grown = matrix.resize(4, 4);
     * grown.get(3, 3);                        // returns (byte) 0 (new cell)
     * grown.get(0, 0);                        // returns (byte) 1 (preserved)
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * ByteMatrix truncated = matrix.resize(2, 2);
     * truncated.columnCount();                // returns 2
     * truncated.get(1, 1);                    // returns (byte) 5
     *
     * // Mixed: grow rows, truncate columns
     * ByteMatrix mixed = matrix.resize(4, 2);
     * mixed.get(3, 0);                        // returns (byte) 0 (new row)
     * matrix.resize(-1, 2);                   // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new ByteMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative
     * @see #resize(int, int, byte)
     * @see #pad(int, int, int, int)
     */
    @Override
    public ByteMatrix resize(final int newRowCount, final int newColumnCount) throws IllegalArgumentException {
        return resize(newRowCount, newColumnCount, BYTE_0);
    }

    /**
     * Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount},
     * anchored at the top-left corner of this matrix.
     *
     * <ul>
     *   <li><b>If a dimension shrinks</b> — elements beyond the new boundary are discarded.
     *       If neither dimension grows, {@code defaultValue} is not used.</li>
     *   <li><b>If a dimension grows</b> — new cells are filled with {@code defaultValue}.</li>
     *   <li><b>Mixed case</b> — each dimension is treated independently, so it is valid
     *       to grow rows while truncating columns, or vice versa.</li>
     * </ul>
     *
     * <p>The original matrix is never modified; the result has independent cell storage.</p>
     *
     * <p><b>Comparison with {@link #pad(int, int, int, int, byte)}:</b>
     * {@code resize} takes <em>absolute</em> target dimensions and may truncate existing content.
     * {@code pad} takes <em>relative</em> padding amounts per edge and <em>never truncates</em>.
     * Use {@code pad} when the entire original content must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     *
     * // Grow: fill new cells with 9
     * ByteMatrix grown = matrix.resize(4, 4, (byte) 9);
     * grown.get(3, 3);                        // returns (byte) 9 (new cell uses defaultValue)
     * grown.get(0, 0);                        // returns (byte) 1 (preserved)
     *
     * // Truncate: defaultValue is ignored when shrinking
     * ByteMatrix truncated = matrix.resize(2, 2, (byte) 9);
     * truncated.get(1, 1);                    // returns (byte) 5 (no new cells, default unused)
     *
     * matrix.resize(0, 0, (byte) 9).isEmpty(); // returns true
     * matrix.resize(2, -1, (byte) 9);          // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the value used to fill cells that are added when a dimension grows;
     *        ignored when neither dimension grows
     * @return a new ByteMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative
     * @see #resize(int, int)
     * @see #pad(int, int, int, int, byte)
     */
    public ByteMatrix resize(final int newRowCount, final int newColumnCount, final byte defaultValue) throws IllegalArgumentException {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, cs.newRowCount, newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, cs.newColumnCount, newColumnCount);
        checkNonNegativeShape(newRowCount, newColumnCount);
        if (newRowCount <= rowCount && newColumnCount <= columnCount) {
            return copyRegion(0, newRowCount, 0, newColumnCount);
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

            return wrapResult(b, newColumnCount);
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
     * <p>Result dimensions:</p>
     * <ul>
     *   <li>Rows: {@code padTop + this.rowCount + padBottom}</li>
     *   <li>Columns: {@code padLeft + this.columnCount + padRight}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     *
     * // Uniform 1-cell border of 0
     * ByteMatrix bordered = matrix.pad(1, 1, 1, 1);
     * bordered.rowCount();                    // returns 4
     * bordered.get(0, 0);                     // returns (byte) 0 (border cell)
     * bordered.get(1, 1);                     // returns (byte) 1 (original top-left)
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * ByteMatrix shifted = matrix.pad(0, 0, 2, 0);
     * shifted.get(0, 2);                      // returns (byte) 1 (original shifted right)
     *
     * matrix.pad(0, 0, 0, 0).equals(matrix); // returns true (no padding -> copy)
     * matrix.pad(-1, 0, 0, 0);               // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @return a new ByteMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative or a resulting dimension overflows {@code int}
     * @see #pad(int, int, int, int, byte)
     * @see #resize(int, int)
     */
    @Override
    public ByteMatrix pad(final int padTop, final int padBottom, final int padLeft, final int padRight) throws IllegalArgumentException {
        return pad(padTop, padBottom, padLeft, padRight, BYTE_0);
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
     * <p>Result dimensions:</p>
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
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     *
     * // Uniform 1-cell border filled with 9
     * ByteMatrix bordered = matrix.pad(1, 1, 1, 1, (byte) 9);
     * bordered.get(0, 0);                     // returns (byte) 9 (border cell)
     * bordered.get(1, 1);                     // returns (byte) 1 (original top-left)
     * bordered.get(2, 2);                     // returns (byte) 4 (original bottom-right)
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * ByteMatrix shifted = matrix.pad(0, 0, 2, 0, (byte) 7);
     * shifted.get(0, 0);                      // returns (byte) 7 (left padding)
     * shifted.get(0, 2);                      // returns (byte) 1 (original shifted right)
     *
     * matrix.pad(-1, 0, 0, 0, (byte) 9);  // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @param defaultValue the value to fill all new padding cells with
     * @return a new ByteMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative or a resulting dimension overflows {@code int}
     * @see #pad(int, int, int, int)
     * @see #resize(int, int, byte)
     */
    public ByteMatrix pad(final int padTop, final int padBottom, final int padLeft, final int padRight, final byte defaultValue)
            throws IllegalArgumentException {
        N.checkArgument(padTop >= 0, MSG_NEGATIVE_DIMENSION, cs.padTop, padTop);
        N.checkArgument(padBottom >= 0, MSG_NEGATIVE_DIMENSION, cs.padBottom, padBottom);
        N.checkArgument(padLeft >= 0, MSG_NEGATIVE_DIMENSION, cs.padLeft, padLeft);
        N.checkArgument(padRight >= 0, MSG_NEGATIVE_DIMENSION, cs.padRight, padRight);

        if ((long) padTop + rowCount + padBottom > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + padTop + " + " + rowCount + " + " + padBottom + " exceeds Integer.MAX_VALUE");
        }

        if ((long) padLeft + columnCount + padRight > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Result column count overflow: " + padLeft + " + " + columnCount + " + " + padRight + " exceeds Integer.MAX_VALUE");
        }

        if (padTop == 0 && padBottom == 0 && padLeft == 0 && padRight == 0) {
            return copy();
        } else {
            final int newRowCount = padTop + rowCount + padBottom;
            final int newColumnCount = padLeft + columnCount + padRight;
            checkNonNegativeShape(newRowCount, newColumnCount);
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

            return wrapResult(b, newColumnCount);
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
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.flipHorizontallyInPlace();
     * matrix.rowView(0);                      // returns [3, 2, 1]
     * matrix.get(1, 0);                       // returns (byte) 6
     *
     * ByteMatrix single = ByteMatrix.wrap(new byte[][] {{1}});
     * single.flipHorizontallyInPlace();
     * single.get(0, 0);                             // returns (byte) 1 (single column unchanged)
     * ByteMatrix.empty().flipHorizontallyInPlace(); // no-op on empty matrix
     * }</pre>
     *
     * @see #flipHorizontally()
     * @see #flipVerticallyInPlace()
     */
    @Override
    public void flipHorizontallyInPlace() {
        if (columnCount < 2) {
            return;
        }

        for (final byte[] row : a) {
            N.reverse(row);
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
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.flipVerticallyInPlace();
     * matrix.rowView(0);                      // returns [5, 6]
     * matrix.rowView(2);                      // returns [1, 2]
     *
     * ByteMatrix single = ByteMatrix.wrap(new byte[][] {{1, 2}});
     * single.flipVerticallyInPlace();
     * single.rowView(0);                          // returns [1, 2] (single row unchanged)
     * ByteMatrix.empty().flipVerticallyInPlace(); // no-op on empty matrix
     * }</pre>
     *
     * @see #flipVertically()
     * @see #flipHorizontallyInPlace()
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
     * Returns a new matrix that is a horizontal flip of this matrix (columns in reversed order).
     * Each row is reversed left-to-right (the leftmost element becomes rightmost).
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * ByteMatrix flipped = matrix.flipHorizontally();
     * flipped.rowView(0);                     // returns [3, 2, 1]
     * matrix.get(0, 0);                       // returns (byte) 1 (original unchanged)
     *
     * ByteMatrix single = ByteMatrix.wrap(new byte[][] {{5}});
     * single.flipHorizontally().get(0, 0);             // returns (byte) 5 (single column unchanged)
     * ByteMatrix.empty().flipHorizontally().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new ByteMatrix with each row reversed
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
     * The topmost row becomes bottommost.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * ByteMatrix flipped = matrix.flipVertically();
     * flipped.rowView(0);                     // returns [4, 5, 6]
     * matrix.rowView(0);                      // returns [1, 2, 3] (original unchanged)
     *
     * ByteMatrix single = ByteMatrix.wrap(new byte[][] {{1, 2}});
     * single.flipVertically().rowView(0);            // returns [1, 2] (single row unchanged)
     * ByteMatrix.empty().flipVertically().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new ByteMatrix with rows reversed
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
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * row of the result being the first column of the original read from bottom to top.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * ByteMatrix rotated = matrix.rotate90();
     * rotated.rowView(0);                     // returns [7, 4, 1]
     * rotated.get(2, 2);                      // returns (byte) 3
     *
     * ByteMatrix wide = ByteMatrix.wrap(new byte[][] {{1, 2, 3}});  // 1x3
     * ByteMatrix tall = wide.rotate90();
     * tall.rowCount();                         // returns 3 (dimensions swapped to 3x1)
     * tall.get(0, 0);                          // returns (byte) 1
     * ByteMatrix.empty().rotate90().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix rotated 90 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         an {@code N x 0} matrix becomes {@code 0 x N}, retaining {@code N} as the result's column count
     * @see #rotate180()
     * @see #rotate270()
     * @see #transpose()
     */
    @Override
    public ByteMatrix rotate90() {
        if (columnCount == 0) {
            return wrapResult(new byte[0][], rowCount);
        }

        checkNonNegativeShape(columnCount, rowCount);

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

        return wrapResult(c, rowCount);
    }

    /**
     * Returns a new matrix that is this matrix rotated 180 degrees.
     * This is equivalent to flipping both horizontally and vertically, reversing the
     * order of all elements. The resulting matrix has the same dimensions as the original.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * ByteMatrix rotated = matrix.rotate180();
     * rotated.rowView(0);                     // returns [9, 8, 7]
     * rotated.get(2, 2);                      // returns (byte) 1
     *
     * ByteMatrix rect = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * rect.rotate180().rowView(0);              // returns [4, 3] (same dimensions)
     * ByteMatrix.empty().rotate180().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 180 degrees
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

        return wrapResult(c, columnCount);
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
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * ByteMatrix rotated = matrix.rotate270();
     * rotated.rowView(0);                     // returns [3, 6, 9]
     * rotated.get(2, 2);                      // returns (byte) 7
     *
     * ByteMatrix wide = ByteMatrix.wrap(new byte[][] {{1, 2, 3}});  // 1x3
     * ByteMatrix tall = wide.rotate270();
     * tall.rowCount();                          // returns 3 (dimensions swapped to 3x1)
     * tall.get(0, 0);                           // returns (byte) 3
     * ByteMatrix.empty().rotate270().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix rotated 270 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         an {@code N x 0} matrix becomes {@code 0 x N}, retaining {@code N} as the result's column count
     * @see #rotate90()
     * @see #rotate180()
     * @see #transpose()
     */
    @Override
    public ByteMatrix rotate270() {
        if (columnCount == 0) {
            return wrapResult(new byte[0][], rowCount);
        }

        checkNonNegativeShape(columnCount, rowCount);

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

        return wrapResult(c, rowCount);
    }

    /**
     * Returns a new matrix that is the transpose of this matrix.
     * The element at position {@code (i, j)} in this matrix appears at position {@code (j, i)}
     * in the result. The resulting matrix has dimensions swapped: {@code columnCount x rowCount}.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * ByteMatrix transposed = matrix.transpose();
     * transposed.rowCount();                  // returns 3 (2x3 becomes 3x2)
     * transposed.get(0, 1);                   // returns (byte) 4 (was at (1,0))
     * transposed.rowView(2);                  // returns [3, 6]
     *
     * matrix.transpose().transpose().equals(matrix); // returns true (involution)
     * ByteMatrix.empty().transpose().isEmpty();      // returns true
     * }</pre>
     *
     * @return a new matrix with dimensions {@code columnCount x rowCount}; an {@code N x 0} matrix becomes {@code 0 x N}
     */
    @Override
    public ByteMatrix transpose() {
        if (columnCount == 0) {
            return wrapResult(new byte[0][], rowCount);
        }

        checkNonNegativeShape(columnCount, rowCount);

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

        return wrapResult(c, rowCount);
    }

    /**
     * Reshapes this matrix to the specified dimensions and pads any extra trailing cells.
     * Elements are taken in row-major order from this matrix and placed into the new shape.
     * The new shape must have at least as many total cells as the original
     * ({@code (long) newRowCount * newColumnCount >= elementCount()}).
     * Any extra trailing cells in the new shape are filled with {@code (byte) 0}.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * ByteMatrix reshaped = matrix.reshapeAndPad(3, 2);
     * reshaped.rowView(0);                    // returns [1, 2] -> [[1, 2], [3, 4], [5, 6]]
     * reshaped.get(2, 1);                     // returns (byte) 6
     *
     * ByteMatrix extended = matrix.reshapeAndPad(2, 4);
     * extended.get(1, 2);                     // returns (byte) 0 (extra trailing cell) -> [[1,2,3,4],[5,6,0,0]]
     *
     * matrix.reshapeAndPad(0, 0);                   // throws IllegalArgumentException (too small for 6 elements)
     * matrix.reshapeAndPad(-1, 6);                  // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix; must be {@code >= 0}
     * @param newColumnCount the number of columns in the reshaped matrix; must be {@code >= 0}
     * @return a new {@code ByteMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative, or if the new shape is too small
     *         to hold every existing element
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public ByteMatrix reshapeAndPad(final int newRowCount, final int newColumnCount) throws IllegalArgumentException {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, cs.newRowCount, newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, cs.newColumnCount, newColumnCount);
        checkNonNegativeShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final byte[][] c = new byte[newRowCount][newColumnCount];

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return wrapResult(c, newColumnCount);
        }

        final int rowLen = (int) N.min(newRowCount, ceilDiv(elementCount, newColumnCount));

        if (a.length == 1) {
            for (int i = 0; i < rowLen; i++) {
                N.copy(a[0], i * newColumnCount, c[i], 0, (int) N.min(newColumnCount, elementCount - (long) i * newColumnCount));
            }
        } else {
            // Both sides advance in row-major order, so the relayout is a sequence of
            // contiguous-run copies tracked by a (srcRow, srcColumn) cursor.
            int srcRow = 0;
            int srcColumn = 0;

            for (int i = 0; i < rowLen; i++) {
                final int rowLength = (int) N.min(newColumnCount, elementCount - (long) i * newColumnCount);
                int copied = 0;

                while (copied < rowLength) {
                    final int chunk = N.min(columnCount - srcColumn, rowLength - copied);
                    N.copy(a[srcRow], srcColumn, c[i], copied, chunk);
                    copied += chunk;
                    srcColumn += chunk;

                    if (srcColumn == columnCount) {
                        srcColumn = 0;
                        srcRow++;
                    }
                }
            }
        }

        return wrapResult(c, newColumnCount);
    }

    /**
     * Repeats elements in both row and column directions.
     * Each element is repeated to form a block of size rowRepeats x columnRepeats.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}});
     * ByteMatrix repeated = matrix.repeatElements(2, 3);
     * repeated.rowCount();                    // returns 2
     * repeated.rowView(0);                    // returns [1, 1, 1, 2, 2, 2]
     *
     * matrix.repeatElements(1, 2).rowView(0); // returns [1, 1, 2, 2]
     *
     * matrix.repeatElements(0, 3);           // throws IllegalArgumentException (not positive)
     * matrix.repeatElements(2, -1);          // throws IllegalArgumentException (not positive)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat each element in row direction; must be {@code > 0}
     * @param columnRepeats number of times to repeat each element in column direction; must be {@code > 0}
     * @return a new {@code ByteMatrix} with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #repeatMatrix(int, int)
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repelem.html">MATLAB repelem function</a>
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

        if (rowRepeats == 1 && columnRepeats == 1) {
            return copy();
        }

        final byte[][] c = new byte[rowCount * rowRepeats][columnCount * columnRepeats];

        for (int i = 0; i < rowCount; i++) {
            final byte[] aa = a[i];
            final byte[] fr = c[i * rowRepeats];

            for (int j = 0; j < columnCount; j++) {
                N.fill(fr, j * columnRepeats, (j + 1) * columnRepeats, aa[j]);
            }

            for (int k = 1; k < rowRepeats; k++) {
                N.copy(fr, 0, c[i * rowRepeats + k], 0, fr.length);
            }
        }

        return wrapResult(c, columnCount * columnRepeats);
    }

    /**
     * Repeats the entire matrix in a repeated pattern.
     * The matrix is repeated as a whole rowRepeats times vertically and columnRepeats times horizontally.
     *
     * <p>Zero-row or zero-column matrices retain the multiplied dimensions, even though they contain no cells.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix repeated = matrix.repeatMatrix(2, 3);
     * repeated.rowCount();                    // returns 4
     * repeated.rowView(0);                    // returns [1, 2, 1, 2, 1, 2]
     *
     * matrix.repeatMatrix(1, 2).rowView(0);  // returns [1, 2, 1, 2]
     *
     * matrix.repeatMatrix(0, 3);             // throws IllegalArgumentException (not positive)
     * matrix.repeatMatrix(2, -1);            // throws IllegalArgumentException (not positive)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat the matrix vertically; must be {@code > 0}
     * @param columnRepeats number of times to repeat the matrix horizontally; must be {@code > 0}
     * @return a new {@code ByteMatrix} with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #repeatElements(int, int)
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repmat.html">MATLAB repmat function</a>
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

        if (rowRepeats == 1 && columnRepeats == 1) {
            return copy();
        }

        final byte[][] c = new byte[rowCount * rowRepeats][columnCount * columnRepeats];

        if (isEmpty()) {
            return wrapResult(c, columnCount * columnRepeats);
        }

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

        return wrapResult(c, columnCount * columnRepeats);
    }

    /**
     * Returns a new {@link ByteList} containing all elements of this matrix in row-major order.
     * The returned list owns its data; modifications to it do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * ByteList list = matrix.flatten();
     * list.size();                            // returns 4
     * list.get(0);                            // returns (byte) 1
     * list.get(3);                            // returns (byte) 4 (row-major order)
     *
     * ByteMatrix.empty().flatten().size();                // returns 0
     * ByteMatrix.wrap(new byte[][] {{7}}).flatten().get(0); // returns (byte) 7 (single element)
     * }</pre>
     *
     * @return a new {@link ByteList} of all elements in row-major order
     * @throws IllegalStateException if {@code (long) rowCount * columnCount > Integer.MAX_VALUE}
     * @see #rowMajorStream()
     */
    @Override
    public ByteList flatten() throws IllegalStateException {
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
     * Exposes the elements of this matrix to {@code action} as a single one-dimensional array
     * laid out in row-major order, then propagates any modifications back into the matrix.
     *
     * <p>This enables operations that need all matrix elements together (e.g., sorting all
     * elements across the entire matrix). The shape of this matrix is preserved; only element
     * values change. See {@link Arrays#mutateViaFlatArray(byte[][], Throwables.Consumer)} for the exact
     * semantics of the underlying operation.</p>
     *
     * <p>The action receives a temporary array. Its mutations are copied back only if the action
     * returns normally; if the action throws, those mutations are discarded. An action is not invoked
     * when this matrix has zero rows, while an {@code n x 0} matrix with {@code n > 0} invokes it once
     * with an empty array. Write-back proceeds in row-major order.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{5, 3}, {4, 1}});
     * matrix.mutateViaFlatArray(arr -> java.util.Arrays.sort(arr));
     * matrix.rowView(0);                      // returns [1, 3]
     * matrix.rowView(1);                      // returns [4, 5] (sorted globally, placed back row-major)
     *
     * int[] captured = new int[1];
     * matrix.mutateViaFlatArray(arr -> captured[0] = arr.length);
     * int flattenedLength = captured[0];      // 4 (temporary array length)
     *
     * ByteMatrix.empty().mutateViaFlatArray(arr -> { });  // zero rows: action is not invoked
     * }</pre>
     *
     * @param <E> the type of exception that the operation may throw
     * @param action the operation to apply to the temporary flattened array
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws ArithmeticException if the element count exceeds {@link Integer#MAX_VALUE}
     * @throws E if {@code action} throws while modifying the temporary flattened array
     * @see Arrays#mutateViaFlatArray(byte[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateViaFlatArray(final Throwables.Consumer<? super byte[], E> action)
            throws IllegalArgumentException, ArithmeticException, E {
        N.checkArgNotNull(action, cs.action);

        Arrays.mutateViaFlatArray(a, action);
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
     * ByteMatrix a = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});      // 2x3
     * ByteMatrix b = ByteMatrix.wrap(new byte[][] {{7, 8, 9}, {10, 11, 12}});   // 2x3
     * ByteMatrix c = a.stackVertically(b);
     * c.rowCount();                           // returns 4
     * c.rowView(2);                           // returns [7, 8, 9]
     * c.get(3, 2);                            // returns (byte) 12
     *
     * ByteMatrix mismatch = ByteMatrix.wrap(new byte[][] {{1, 2}});
     * a.stackVertically(mismatch);           // throws IllegalArgumentException (column count differs)
     * a.stackVertically((ByteMatrix) null);  // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to stack below this matrix (must have the same column count)
     * @return a new ByteMatrix with dimensions (this.rowCount + other.rowCount) x this.columnCount
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.columnCount != other.columnCount},
     *         or if the merged row count would exceed {@code Integer.MAX_VALUE}
     * @see #stackHorizontally(ByteMatrix)
     */
    @Override
    public ByteMatrix stackVertically(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);
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

        return wrapResult(c, columnCount);
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
     * ByteMatrix a = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});      // 2x3
     * ByteMatrix b = ByteMatrix.wrap(new byte[][] {{7, 8, 9}, {10, 11, 12}});   // 2x3
     * ByteMatrix c = a.stackHorizontally(b);
     * c.columnCount();                        // returns 6
     * c.rowView(0);                           // returns [1, 2, 3, 7, 8, 9]
     * c.get(1, 5);                            // returns (byte) 12
     *
     * ByteMatrix mismatch = ByteMatrix.wrap(new byte[][] {{1, 2, 3}});
     * a.stackHorizontally(mismatch);          // throws IllegalArgumentException (row count differs)
     * a.stackHorizontally((ByteMatrix) null); // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to stack to the right of this matrix (must have the same row count)
     * @return a new ByteMatrix with dimensions this.rowCount x (this.columnCount + other.columnCount)
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.rowCount != other.rowCount},
     *         or if the merged column count would exceed {@code Integer.MAX_VALUE}
     * @see #stackVertically(ByteMatrix)
     */
    @Override
    public ByteMatrix stackHorizontally(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgument(rowCount == other.rowCount, MSG_HSTACK_ROW_MISMATCH, rowCount, other.rowCount);
        final long mergedColumnCount = (long) columnCount + other.columnCount;
        N.checkArgument(mergedColumnCount <= Integer.MAX_VALUE, "Merged column count overflow: {} + {} = {}", columnCount, other.columnCount,
                mergedColumnCount);

        final byte[][] c = new byte[rowCount][(int) mergedColumnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c[i], 0, columnCount);
            N.copy(other.a[i], 0, c[i], columnCount, other.columnCount);
        }

        return wrapResult(c, (int) mergedColumnCount);
    }

    /**
     * Performs element-wise addition with another matrix.
     * The matrices must have the same dimensions. The original matrices are not modified.
     *
     * <p><b>Overflow:</b> the sum {@code a[i][j] + other[i][j]} is computed as an {@code int} and
     * narrowed back to {@code byte} via an explicit cast, so values outside the byte range
     * {@code [-128, 127]} wrap modulo 256. For example,
     * {@code (byte) ((byte) 127 + (byte) 1) == (byte) -128}.
     * Use {@link #addWidened(ByteMatrix)} for an exact {@code int} result or
     * {@link #addExact(ByteMatrix)} to reject values outside the byte range.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix a = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix b = ByteMatrix.wrap(new byte[][] {{5, 6}, {7, 8}});
     * ByteMatrix sum = a.add(b);
     * sum.get(0, 0);                          // returns (byte) 6
     * sum.get(1, 1);                          // returns (byte) 12 -> [[6, 8], [10, 12]]
     *
     * // Byte overflow wraps modulo 256
     * ByteMatrix big = ByteMatrix.wrap(new byte[][] {{127}});
     * big.add(ByteMatrix.wrap(new byte[][] {{1}})).get(0, 0); // returns (byte) -128 (127 + 1 wraps)
     *
     * ByteMatrix wrongShape = ByteMatrix.wrap(new byte[][] {{1, 2, 3}});
     * a.add(wrongShape);                     // throws IllegalArgumentException (different shapes)
     * a.add((ByteMatrix) null);              // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to add to this matrix; must not be {@code null} and must have the same shape
     * @return a new {@code ByteMatrix} containing the element-wise sum
     * @throws IllegalArgumentException if {@code other} is {@code null}, or if the matrices have different shapes
     * @see #subtract(ByteMatrix)
     * @see #zipWith(ByteMatrix, Throwables.ByteBinaryOperator)
     * @see #addWidened(ByteMatrix)
     * @see #addExact(ByteMatrix)
     */
    public ByteMatrix add(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgument(isSameShape(other), "Cannot add matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final byte[][] otherArray = other.a;
        final byte[][] result = new byte[rowCount][columnCount];

        if (Matrices.shouldRunInParallel(this)) {
            final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = (byte) (a[i][j] + otherArray[i][j]);
            Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
        } else {
            for (int i = 0; i < rowCount; i++) {
                final byte[] row = a[i];
                final byte[] otherRow = otherArray[i];
                final byte[] resultRow = result[i];

                for (int j = 0; j < columnCount; j++) {
                    resultRow[j] = (byte) (row[j] + otherRow[j]);
                }
            }
        }

        return wrapResult(result, columnCount);
    }

    /**
     * Adds corresponding elements without narrowing the result to {@code byte}.
     *
     * @param other the matrix to add; must be non-{@code null} and have the same shape
     * @return an {@link IntMatrix} containing the exact element-wise sums
     * @throws IllegalArgumentException if {@code other} is {@code null} or has a different shape
     */
    public IntMatrix addWidened(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgument(isSameShape(other), "Cannot add matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final int[][] result = new int[rowCount][columnCount];

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                result[i][j] = a[i][j] + other.a[i][j];
            }
        }

        return IntMatrix.wrapResult(result, columnCount);
    }

    /**
     * Adds corresponding elements and throws if any mathematical sum is outside the {@code byte} range.
     *
     * @param other the matrix to add; must be non-{@code null} and have the same shape
     * @return a {@code ByteMatrix} containing the exact element-wise sums
     * @throws IllegalArgumentException if {@code other} is {@code null} or has a different shape
     * @throws ArithmeticException if any sum is outside {@code [-128, 127]}
     */
    public ByteMatrix addExact(final ByteMatrix other) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgument(isSameShape(other), "Cannot add matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final byte[][] result = new byte[rowCount][columnCount];

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                result[i][j] = toByteExact(a[i][j] + other.a[i][j]);
            }
        }

        return wrapResult(result, columnCount);
    }

    /**
     * Performs element-wise subtraction ({@code this - other}).
     * The matrices must have the same dimensions. The original matrices are not modified.
     *
     * <p><b>Overflow:</b> the difference {@code a[i][j] - other[i][j]} is computed as an {@code int}
     * and narrowed back to {@code byte} via an explicit cast, so values outside the byte range
     * {@code [-128, 127]} wrap modulo 256. For example,
     * {@code (byte) ((byte) -128 - (byte) 1) == (byte) 127}. Use
     * {@link #subtractWidened(ByteMatrix)} or {@link #subtractExact(ByteMatrix)} to avoid silent wraparound.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix a = ByteMatrix.wrap(new byte[][] {{5, 6}, {7, 8}});
     * ByteMatrix b = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix diff = a.subtract(b);
     * diff.get(0, 0);                         // returns (byte) 4
     * diff.get(1, 1);                         // returns (byte) 4 -> [[4, 4], [4, 4]]
     *
     * // Byte underflow wraps modulo 256
     * ByteMatrix min = ByteMatrix.wrap(new byte[][] {{-128}});
     * min.subtract(ByteMatrix.wrap(new byte[][] {{1}})).get(0, 0); // returns (byte) 127 (-128 - 1 wraps)
     *
     * ByteMatrix wrongShape = ByteMatrix.wrap(new byte[][] {{1, 2, 3}});
     * a.subtract(wrongShape);                // throws IllegalArgumentException (different shapes)
     * a.subtract((ByteMatrix) null);         // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to subtract from this matrix; must not be {@code null} and must have the same shape
     * @return a new {@code ByteMatrix} containing the element-wise difference {@code this - other}
     * @throws IllegalArgumentException if {@code other} is {@code null}, or if the matrices have different shapes
     * @see #add(ByteMatrix)
     * @see #zipWith(ByteMatrix, Throwables.ByteBinaryOperator)
     * @see #subtractWidened(ByteMatrix)
     * @see #subtractExact(ByteMatrix)
     */
    public ByteMatrix subtract(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgument(isSameShape(other), "Cannot subtract matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final byte[][] otherArray = other.a;
        final byte[][] result = new byte[rowCount][columnCount];

        if (Matrices.shouldRunInParallel(this)) {
            final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = (byte) (a[i][j] - otherArray[i][j]);
            Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
        } else {
            for (int i = 0; i < rowCount; i++) {
                final byte[] row = a[i];
                final byte[] otherRow = otherArray[i];
                final byte[] resultRow = result[i];

                for (int j = 0; j < columnCount; j++) {
                    resultRow[j] = (byte) (row[j] - otherRow[j]);
                }
            }
        }

        return wrapResult(result, columnCount);
    }

    /**
     * Subtracts corresponding elements without narrowing the result to {@code byte}.
     *
     * @param other the matrix to subtract; must be non-{@code null} and have the same shape
     * @return an {@link IntMatrix} containing the exact element-wise differences
     * @throws IllegalArgumentException if {@code other} is {@code null} or has a different shape
     */
    public IntMatrix subtractWidened(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgument(isSameShape(other), "Cannot subtract matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final int[][] result = new int[rowCount][columnCount];

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                result[i][j] = a[i][j] - other.a[i][j];
            }
        }

        return IntMatrix.wrapResult(result, columnCount);
    }

    /**
     * Subtracts corresponding elements and throws if any mathematical difference is outside the {@code byte} range.
     *
     * @param other the matrix to subtract; must be non-{@code null} and have the same shape
     * @return a {@code ByteMatrix} containing the exact element-wise differences
     * @throws IllegalArgumentException if {@code other} is {@code null} or has a different shape
     * @throws ArithmeticException if any difference is outside {@code [-128, 127]}
     */
    public ByteMatrix subtractExact(final ByteMatrix other) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgument(isSameShape(other), "Cannot subtract matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final byte[][] result = new byte[rowCount][columnCount];

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                result[i][j] = toByteExact(a[i][j] - other.a[i][j]);
            }
        }

        return wrapResult(result, columnCount);
    }

    /**
     * Performs matrix multiplication (Cayley product) with another matrix.
     * The number of columns in this matrix must equal the number of rows in {@code other}.
     * Result has shape {@code this.rowCount x other.columnCount}. The original matrices are not modified.
     *
     * <p><b>Note:</b> This is the linear-algebra matrix product, not element-wise multiplication.
     * For element-wise multiplication use {@link #zipWith(ByteMatrix, Throwables.ByteBinaryOperator)}.</p>
     *
     * <p><b>Overflow:</b> each partial product {@code a[i][k] * other[k][j]} is computed as an
     * {@code int} (via Java's numeric promotion), but it is then accumulated into the {@code byte}
     * result cell with implicit narrowing, so intermediate sums wrap modulo 256 and the final result
     * is always in the byte range {@code [-128, 127]}. Use {@link #matrixMultiplyWidened(ByteMatrix)}
     * for a {@code long} result or {@link #matrixMultiplyExact(ByteMatrix)} to reject an out-of-range byte result.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix a = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix b = ByteMatrix.wrap(new byte[][] {{5, 6}, {7, 8}});
     * ByteMatrix product = a.matrixMultiply(b);
     * product.get(0, 0);                      // returns (byte) 19 (1*5 + 2*7)
     * product.get(1, 1);                      // returns (byte) 50 -> [[19, 22], [43, 50]]
     *
     * ByteMatrix m2x3 = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});      // 2x3
     * ByteMatrix m3x2 = ByteMatrix.wrap(new byte[][] {{7, 8}, {9, 10}, {11, 12}}); // 3x2
     * m2x3.matrixMultiply(m3x2).rowCount();                                      // returns 2 (result is 2x2)
     *
     * a.matrixMultiply(m3x2);                        // throws IllegalArgumentException (a.columnCount=2 != m3x2.rowCount=3)
     * a.matrixMultiply((ByteMatrix) null);           // throws IllegalArgumentException (other is null)
     * }</pre>
     *
     * @param other the matrix to multiply with; must not be {@code null}
     * @return a new {@code ByteMatrix} of shape {@code this.rowCount x other.columnCount} containing the matrix product
     * @throws IllegalArgumentException if {@code other} is {@code null} or {@code this.columnCount != other.rowCount}
     * @see #matrixMultiplyWidened(ByteMatrix)
     * @see #matrixMultiplyExact(ByteMatrix)
     */
    public ByteMatrix matrixMultiply(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgument(columnCount == other.rowCount,
                "Matrix dimensions incompatible for multiplication: this is {}x{}, other is {}x{} (this.columnCount must equal other.rowCount)", rowCount,
                columnCount, other.rowCount, other.columnCount);

        checkNonNegativeShape(rowCount, other.columnCount);

        final byte[][] otherArray = other.a;
        final int newColumnCount = other.columnCount;
        final byte[][] result = new byte[rowCount][newColumnCount];

        if (Matrices.shouldRunMatrixMultiplyInParallel(this, newColumnCount)) {
            final Throwables.IntTriConsumer<RuntimeException> multiplyAction = (i, j, k) -> result[i][j] += a[i][k] * otherArray[k][j];
            Matrices.forEachCartesianIndices(this, other, multiplyAction, true);
        } else {
            // i-k-j loop order with hoisted rows: accumulates each result cell in ascending k order,
            // matching the accumulation order of Matrices.forEachCartesianIndices exactly.
            for (int i = 0; i < rowCount; i++) {
                final byte[] row = a[i];
                final byte[] resultRow = result[i];

                for (int k = 0; k < columnCount; k++) {
                    final byte aik = row[k];
                    final byte[] otherRow = otherArray[k];

                    for (int j = 0; j < newColumnCount; j++) {
                        resultRow[j] += aik * otherRow[j];
                    }
                }
            }
        }

        return wrapResult(result, newColumnCount);
    }

    /**
     * Computes the matrix product in {@code long} precision instead of narrowing each result to {@code byte}.
     * A dot product of two {@code byte} vectors whose length is representable by Java arrays cannot
     * overflow {@code long}, so every result cell is exact.
     *
     * @param other the right operand; must be non-{@code null} and dimensionally compatible
     * @return a {@link LongMatrix} containing the widened matrix product
     * @throws IllegalArgumentException if {@code other} is {@code null} or {@code this.columnCount != other.rowCount}
     */
    public LongMatrix matrixMultiplyWidened(final ByteMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgument(columnCount == other.rowCount,
                "Matrix dimensions incompatible for multiplication: this is {}x{}, other is {}x{} (this.columnCount must equal other.rowCount)", rowCount,
                columnCount, other.rowCount, other.columnCount);

        final int newColumnCount = other.columnCount;
        final long[][] result = new long[rowCount][newColumnCount];

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < newColumnCount; j++) {
                long sum = 0;

                for (int k = 0; k < columnCount; k++) {
                    sum += (long) a[i][k] * other.a[k][j];
                }

                result[i][j] = sum;
            }
        }

        return LongMatrix.wrapResult(result, newColumnCount);
    }

    /**
     * Computes the mathematical matrix product and throws if any final cell is outside the {@code byte} range.
     * The dot products are accumulated before the range check, so cancellation is handled correctly.
     *
     * @param other the right operand; must be non-{@code null} and dimensionally compatible
     * @return a {@code ByteMatrix} containing the exact matrix product
     * @throws IllegalArgumentException if {@code other} is {@code null} or {@code this.columnCount != other.rowCount}
     * @throws ArithmeticException if any result cell is outside {@code [-128, 127]}
     */
    public ByteMatrix matrixMultiplyExact(final ByteMatrix other) throws IllegalArgumentException, ArithmeticException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgument(columnCount == other.rowCount,
                "Matrix dimensions incompatible for multiplication: this is {}x{}, other is {}x{} (this.columnCount must equal other.rowCount)", rowCount,
                columnCount, other.rowCount, other.columnCount);

        final int newColumnCount = other.columnCount;
        final byte[][] result = new byte[rowCount][newColumnCount];

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < newColumnCount; j++) {
                long sum = 0;

                // Accumulate the whole dot product before narrowing. Checking each partial sum would
                // incorrectly reject valid results whose positive and negative terms cancel.
                for (int k = 0; k < columnCount; k++) {
                    sum += (long) a[i][k] * other.a[k][j];
                }

                result[i][j] = toByteExact(sum);
            }
        }

        return wrapResult(result, newColumnCount);
    }

    /**
     * Narrows a value to {@code byte} without overflow.
     *
     * @param value the value to narrow
     * @return the value as a {@code byte}
     * @throws ArithmeticException if {@code value} is less than {@link Byte#MIN_VALUE}
     *         or greater than {@link Byte#MAX_VALUE}
     */
    private static byte toByteExact(final long value) throws ArithmeticException {
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw new ArithmeticException("byte overflow: " + value);
        }

        return (byte) value;
    }

    /**
     * Converts this primitive byte matrix to a boxed {@link Matrix Matrix&lt;Byte&gt;}.
     * Each byte value is converted to its corresponding Byte wrapper object.
     *
     * <p>This conversion is useful when you need to work with APIs that require
     * object types rather than primitives, or when you need {@code null} values in the matrix.
     * Note that boxing incurs memory overhead and may impact performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix primitive = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * Matrix<Byte> boxed = primitive.boxed();
     * boxed.get(0, 0);                            // returns Byte 1
     * boxed.get(1, 1);                            // returns Byte 4
     * boxed.set(0, 0, null);                      // boxed matrix can hold null values
     * boxed.get(0, 0);                            // returns null
     *
     * ByteMatrix.empty().boxed().isEmpty();      // returns true
     * }</pre>
     *
     * @return a new {@code Matrix<Byte>} with the same dimensions and values as this matrix
     * @see #unbox(Matrix)
     */
    public Matrix<Byte> boxed() {
        final Byte[][] c = new Byte[rowCount][columnCount];

        for (int i = 0; i < rowCount; i++) {
            final byte[] aa = a[i];
            final Byte[] cc = c[i];

            for (int j = 0; j < columnCount; j++) {
                cc[j] = aa[j]; // NOSONAR
            }
        }

        return new Matrix<>(c, Byte.class, columnCount);
    }

    /**
     * Converts this ByteMatrix to an IntMatrix by widening each byte value to int.
     * Each byte value is promoted to a 32-bit integer with sign extension.
     * This is a lossless conversion that preserves all values and their signs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix byteMatrix = ByteMatrix.wrap(new byte[][] {{1, -2}, {127, -128}});
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
        if (rowCount == 0) {
            return IntMatrix.wrapResult(new int[0][], columnCount);
        }

        return IntMatrix.from(a);
    }

    /**
     * Converts this ByteMatrix to a LongMatrix by widening each byte value to long.
     * Each byte value is promoted to a 64-bit long integer with sign extension.
     * This is a lossless conversion that preserves all values and their signs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix byteMatrix = ByteMatrix.wrap(new byte[][] {{1, -2}, {127, -128}});
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

        for (int i = 0; i < rowCount; i++) {
            final byte[] aa = a[i];
            final long[] cc = c[i];

            for (int j = 0; j < columnCount; j++) {
                cc[j] = aa[j]; // NOSONAR
            }
        }

        return LongMatrix.wrapResult(c, columnCount);
    }

    /**
     * Converts this ByteMatrix to a FloatMatrix by converting each byte value to float.
     * Each byte value is converted to a 32-bit floating-point number.
     * This is a lossless conversion since all byte values can be exactly represented as floats.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix byteMatrix = ByteMatrix.wrap(new byte[][] {{1, -2}, {127, -128}});
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

        for (int i = 0; i < rowCount; i++) {
            final byte[] aa = a[i];
            final float[] cc = c[i];

            for (int j = 0; j < columnCount; j++) {
                cc[j] = aa[j]; // NOSONAR
            }
        }

        return FloatMatrix.wrapResult(c, columnCount);
    }

    /**
     * Converts this ByteMatrix to a DoubleMatrix by converting each byte value to double.
     * Each byte value is converted to a 64-bit double-precision floating-point number.
     * This is a lossless conversion since all byte values can be exactly represented as doubles.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix byteMatrix = ByteMatrix.wrap(new byte[][] {{1, -2}, {127, -128}});
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

        for (int i = 0; i < rowCount; i++) {
            final byte[] aa = a[i];
            final double[] cc = c[i];

            for (int j = 0; j < columnCount; j++) {
                cc[j] = aa[j]; // NOSONAR
            }
        }

        return DoubleMatrix.wrapResult(c, columnCount);
    }

    /**
     * Performs element-wise operation on two matrices using a binary operator.
     * The matrices must have the same dimensions. Corresponding elements from both matrices
     * are combined using the provided function to produce the result matrix.
     *
     * <p>This is a generalized element-wise operation. For the common element-wise operations of addition and
     * subtraction, consider using the dedicated methods {@link #add(ByteMatrix)} and {@link #subtract(ByteMatrix)};
     * for the linear-algebra matrix product (which is not an element-wise operation), use {@link #matrixMultiply(ByteMatrix)}.</p>
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance. If parallelized, the supplied function must be thread-safe.
     * Creates a new matrix; the original matrices are not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix a = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix b = ByteMatrix.wrap(new byte[][] {{5, 6}, {7, 8}});
     *
     * ByteMatrix product = a.zipWith(b, (x, y) -> (byte) (x * y));
     * product.get(0, 1);                      // returns (byte) 12 -> [[5, 12], [21, 32]]
     *
     * ByteMatrix max = a.zipWith(b, (x, y) -> (byte) Math.max(x, y));
     * max.get(0, 0);                          // returns (byte) 5 -> [[5, 6], [7, 8]]
     *
     * ByteMatrix wrongShape = ByteMatrix.wrap(new byte[][] {{1, 2, 3}});
     * a.zipWith(wrongShape, (x, y) -> (byte) (x + y));                       // throws IllegalArgumentException (different shapes)
     * a.zipWith(b, (Throwables.ByteBinaryOperator<RuntimeException>) null);  // throws IllegalArgumentException (null function)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param zipFunction the binary operator to apply to corresponding elements; receives the
     *                    element from this matrix as first argument and the element from
     *                    {@code other} as second argument
     * @return a new {@code ByteMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if {@code other} is {@code null}, if the matrices have different shapes,
     *         or if {@code zipFunction} is {@code null}
     * @throws E if {@code zipFunction} throws while combining corresponding matrix elements
     * @see #zipWith(ByteMatrix, ByteMatrix, Throwables.ByteTernaryOperator)
     */
    public <E extends Exception> ByteMatrix zipWith(final ByteMatrix other, final Throwables.ByteBinaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, cs.other);
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);
        N.checkArgNotNull(zipFunction, cs.zipFunction);

        final byte[][] b = other.a;
        final byte[][] result = new byte[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsByte(a[i][j], b[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return wrapResult(result, columnCount);
    }

    /**
     * Performs element-wise operation on three matrices using a ternary operator.
     * All matrices must have the same dimensions. Corresponding elements from all three matrices
     * are combined using the provided function to produce the result matrix.
     *
     * <p>This is useful for operations that combine three matrices, such as weighted averages,
     * conditional selection, or mathematical formulas involving three variables.</p>
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance. If parallelized, the supplied function must be thread-safe.
     * Creates a new matrix; the original matrices are not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix a = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix b = ByteMatrix.wrap(new byte[][] {{5, 6}, {7, 8}});
     * ByteMatrix c = ByteMatrix.wrap(new byte[][] {{9, 10}, {11, 12}});
     *
     * ByteMatrix sum = a.zipWith(b, c, (x, y, z) -> (byte) (x + y + z));
     * sum.get(0, 0);                          // returns (byte) 15 -> [[15, 18], [21, 24]]
     *
     * ByteMatrix middle = a.zipWith(b, c, (x, y, z) -> y);
     * middle.get(0, 0);                       // returns (byte) 5 -> [[5, 6], [7, 8]]
     *
     * ByteMatrix wrongShape = ByteMatrix.wrap(new byte[][] {{1, 2, 3}});
     * a.zipWith(wrongShape, c, (x, y, z) -> x);                                 // throws IllegalArgumentException (different shapes)
     * a.zipWith(b, c, (Throwables.ByteTernaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null function)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param third the third matrix (must have the same dimensions as this matrix)
     * @param zipFunction the ternary operator to apply to corresponding elements; receives the
     *                    element from this matrix as first argument, the element from
     *                    {@code other} as second argument, and the element from {@code third}
     *                    as third argument
     * @return a new {@code ByteMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if {@code other} is {@code null} or its shape differs from this matrix's shape,
     *         if {@code third} is {@code null} or its shape differs from this matrix's shape,
     *         or if {@code zipFunction} is {@code null}
     * @throws E if {@code zipFunction} throws while combining corresponding matrix elements
     * @see #zipWith(ByteMatrix, Throwables.ByteBinaryOperator)
     */
    public <E extends Exception> ByteMatrix zipWith(final ByteMatrix other, final ByteMatrix third, final Throwables.ByteTernaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, cs.other);
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);
        N.checkArgNotNull(third, cs.third);
        N.checkArgument(isSameShape(third), "Cannot zip matrices with different shapes: this is {}x{} but third is {}x{}", rowCount, columnCount,
                third.rowCount, third.columnCount);
        N.checkArgNotNull(zipFunction, cs.zipFunction);

        final byte[][] b = other.a;
        final byte[][] c = third.a;
        final byte[][] result = new byte[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsByte(a[i][j], b[i][j], c[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return wrapResult(result, columnCount);
    }

    /**
     * Returns a stream of elements on the main diagonal (upper-left to lower-right).
     * Rectangular matrices are supported; the stream length is {@code min(rowCount, columnCount)}.
     *
     * <p>The stream reads this matrix's live backing storage as elements are consumed; it does not
     * snapshot cell values. Changes made before an element is read are visible in the traversal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.mainDiagonalStream().toArray();  // returns [1, 5, 9]
     * matrix.mainDiagonalStream().sum();      // returns 15
     *
     * ByteMatrix.empty().mainDiagonalStream().count(); // returns 0 (empty stream)
     * ByteMatrix nonSquare = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.mainDiagonalStream().toArray(); // returns [1, 5]
     * }</pre>
     *
     * @return a ByteStream of main-diagonal elements, or an empty stream if this is the empty 0x0 matrix
     */
    @Override
    public ByteStream mainDiagonalStream() {
        final int diagonalLength = diagonalLength();

        if (diagonalLength == 0) {
            return ByteStream.empty();
        }

        return ByteStream.of(new ByteIteratorEx() {
            private final int toIndex = diagonalLength;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            /**
             * {@inheritDoc}
             *
             * @throws NoSuchElementException if no elements remain in this iterator
             */
            @Override
            public byte nextByte() throws NoSuchElementException {
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
                final long remaining = toIndex - cursor;
                cursor = toIndex;
                return remaining;
            }
        });
    }

    /**
     * Returns a stream of elements on the anti-diagonal (upper-right to lower-left).
     * Rectangular matrices are supported; the stream length is {@code min(rowCount, columnCount)}.
     *
     * <p>The stream reads this matrix's live backing storage as elements are consumed; it does not
     * snapshot cell values. Changes made before an element is read are visible in the traversal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.antiDiagonalStream().toArray();  // returns [3, 5, 7]
     * matrix.antiDiagonalStream().sum();      // returns 15
     *
     * ByteMatrix.empty().antiDiagonalStream().count(); // returns 0 (empty stream)
     * ByteMatrix nonSquare = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.antiDiagonalStream().toArray(); // returns [3, 5]
     * }</pre>
     *
     * @return a ByteStream of anti-diagonal elements, or an empty stream if this is the empty 0x0 matrix
     */
    @Override
    public ByteStream antiDiagonalStream() {
        final int diagonalLength = diagonalLength();

        if (diagonalLength == 0) {
            return ByteStream.empty();
        }

        return ByteStream.of(new ByteIteratorEx() {
            private final int toIndex = diagonalLength;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            /**
             * {@inheritDoc}
             *
             * @throws NoSuchElementException if no elements remain in this iterator
             */
            @Override
            public byte nextByte() throws NoSuchElementException {
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
                final long remaining = toIndex - cursor;
                cursor = toIndex;
                return remaining;
            }
        });
    }

    /**
     * Returns a stream of all elements in this matrix, traversed in row-major order (left to right, top to bottom).
     * Elements are streamed row by row from the top-left corner to the bottom-right corner.
     *
     * <p>This method is useful for processing all matrix elements sequentially
     * without concern for their row/column positions. The stream supports all
     * standard ByteStream operations including sum, average, filter, map, etc.</p>
     *
     * <p>The stream reads this matrix's live backing storage as elements are consumed; it does not
     * snapshot cell values. Changes made before an element is read are visible in the traversal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.rowMajorStream().toArray();    // returns [1, 2, 3, 4]
     * matrix.rowMajorStream().sum();        // returns 10
     *
     * ByteMatrix.empty().rowMajorStream().count();              // returns 0 (empty stream)
     * ByteMatrix.wrap(new byte[][] {{7}}).rowMajorStream().sum(); // returns 7 (single element)
     * }</pre>
     *
     * @return a ByteStream of all elements in row-major order, or an empty stream if the matrix is empty
     */
    @Override
    public ByteStream rowMajorStream() {
        return rowMajorStream(0, rowCount);
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
     * <p>The stream reads this matrix's live backing storage as elements are consumed; it does not
     * snapshot cell values. Changes made before an element is read are visible in the traversal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.rowMajorStream(1, 3).toArray(); // returns [3, 4, 5, 6]
     * matrix.rowMajorStream(0, 2).toArray(); // returns [1, 2, 3, 4]
     *
     * matrix.rowMajorStream(1, 1).count();  // returns 0 (empty range)
     * matrix.rowMajorStream(0, 5);          // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a ByteStream of elements from the specified row range, or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public ByteStream rowMajorStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
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

            /**
             * {@inheritDoc}
             *
             * @throws NoSuchElementException if no elements remain in this iterator
             */
            @Override
            public byte nextByte() throws NoSuchElementException {
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
                final long remaining = (long) (toRowIndex - i) * columnCount - j;
                i = toRowIndex;
                j = 0;
                return remaining;
            }

            /**
             * {@inheritDoc}
             *
             * @throws IllegalStateException if the number of remaining elements exceeds {@link Integer#MAX_VALUE};
             *         no elements are consumed
             */
            @Override
            public byte[] toArray() throws IllegalStateException {
                final int len = toArrayLength((long) (toRowIndex - i) * columnCount - j);
                final byte[] c = new byte[len];
                int k = 0;

                while (k < len) {
                    final int chunk = N.min(columnCount - j, len - k);
                    N.copy(a[i], j, c, k, chunk);
                    k += chunk;
                    j += chunk;

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
     * <p>The stream reads this matrix's live backing storage as elements are consumed; it does not
     * snapshot cell values. Changes made before an element is read are visible in the traversal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.columnMajorStream().toArray();      // returns [1, 3, 2, 4] (column-major)
     * matrix.columnMajorStream().sum();          // returns 10
     *
     * ByteMatrix.empty().columnMajorStream().count();              // returns 0 (empty stream)
     * ByteMatrix.wrap(new byte[][] {{7}}).columnMajorStream().sum(); // returns 7 (single element)
     * }</pre>
     *
     * @return a ByteStream of all elements in column-major order, or an empty stream if the matrix is empty
     */
    @Override
    public ByteStream columnMajorStream() {
        return columnMajorStream(0, columnCount);
    }

    /**
     * Returns a stream of elements from a range of columns in column-major order.
     * Elements are streamed column by column from the starting column (inclusive)
     * to the ending column (exclusive), with each column streamed from top to bottom.
     *
     * <p>This method allows for efficient processing of a
     * subset of matrix columns in column-major order.</p>
     *
     * <p>The stream reads this matrix's live backing storage as elements are consumed; it does not
     * snapshot cell values. Changes made before an element is read are visible in the traversal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.columnMajorStream(1, 3).toArray();  // returns [2, 5, 3, 6]
     * matrix.columnMajorStream(0, 2).toArray();  // returns [1, 4, 2, 5]
     *
     * matrix.columnMajorStream(1, 1).count();    // returns 0 (empty range)
     * matrix.columnMajorStream(0, 5);            // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a ByteStream of elements from the specified column range in column-major order,
     *         or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    public ByteStream columnMajorStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
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

            /**
             * {@inheritDoc}
             *
             * @throws NoSuchElementException if no elements remain in this iterator
             */
            @Override
            public byte nextByte() throws NoSuchElementException {
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
                final long remaining = (long) (toColumnIndex - j) * rowCount - i;
                i = 0;
                j = toColumnIndex;
                return remaining;
            }

            /**
             * {@inheritDoc}
             *
             * @throws IllegalStateException if the number of remaining elements exceeds {@link Integer#MAX_VALUE};
             *         no elements are consumed
             */
            @Override
            public byte[] toArray() throws IllegalStateException {
                final int len = toArrayLength((long) (toColumnIndex - j) * rowCount - i);
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
     * Returns a stream of ByteStream objects, where each ByteStream represents a complete row.
     * This creates a stream of streams, allowing for row-by-row processing of the matrix.
     *
     * <p>This method is useful for operations that need to process entire rows as units,
     * such as row-wise transformations, filtering rows based on conditions, or mapping
     * rows to other values.</p>
     *
     * <p>This yields one stream per row. To instead stream the elements of a single row as one
     * flat stream, use {@link #rowMajorStream(int, int) rowMajorStream(rowIndex, rowIndex + 1)}.</p>
     *
     * <p>Each inner stream binds to the backing row selected when that inner stream is produced.
     * Later row swaps do not retarget an already produced row stream; cell changes in its backing
     * row remain visible.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.rowStreams().count();            // returns 3 (one stream per row)
     * matrix.rowStreams()
     *     .mapToInt(row -> row.sum())
     *     .toArray();                         // returns [3, 7, 11]
     *
     * ByteMatrix.empty().rowStreams().count();                   // returns 0 (no rows)
     * ByteMatrix.wrap(new byte[][] {{7, 8}}).rowStreams().count(); // returns 1 (single row)
     * }</pre>
     *
     * @return a Stream of ByteStream objects, one for each row in the matrix,
     *         or an empty stream when there are no rows (an {@code N x 0} matrix still yields one
     *         empty stream per row)
     * @see #rowMajorStream(int, int)
     */
    @Override
    public Stream<ByteStream> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Returns a stream of ByteStream objects for a range of rows.
     * Each ByteStream in the result represents a complete row within the specified range.
     *
     * <p>This method allows for processing a subset of rows while maintaining the
     * ability to work with complete rows as individual streams.</p>
     *
     * <p>Each inner stream binds to the backing row selected when that inner stream is produced.
     * Later row swaps do not retarget an already produced row stream; cell changes in its backing
     * row remain visible.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.rowStreams(1, 3).count();        // returns 2 (rows 1 and 2)
     * matrix.rowStreams(0, 2)
     *     .mapToInt(row -> row.max().orElse((byte) 0))
     *     .toArray();                         // returns [2, 4]
     *
     * matrix.rowStreams(1, 1).count();        // returns 0 (empty range)
     * matrix.rowStreams(0, 5);                // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of ByteStream objects for the specified row range
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
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

            /**
             * {@inheritDoc}
             *
             * @throws NoSuchElementException if no elements remain in this iterator
             */
            @Override
            public ByteStream next() throws NoSuchElementException {
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
                final long remaining = toIndex - cursor;
                cursor = toIndex;
                return remaining;
            }
        });
    }

    /**
     * Returns a stream of ByteStream objects, where each ByteStream represents a complete column.
     * This creates a stream of streams, allowing for column-by-column processing of the matrix.
     *
     * <p>This method is useful for operations that need to process
     * entire columns as units, such as column-wise statistics, transformations, or filtering
     * columns based on conditions.</p>
     *
     * <p>This yields one stream per column. To instead stream the elements of a single column as one
     * flat stream, use {@link #columnMajorStream(int, int) columnMajorStream(columnIndex, columnIndex + 1)}.</p>
     *
     * <p>Each inner stream reads its column from this matrix as it is consumed, so later cell changes
     * and row swaps are visible.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.columnStreams().count();         // returns 3 (one stream per column)
     * matrix.columnStreams()
     *     .mapToInt(col -> col.sum())
     *     .toArray();                         // returns [5, 7, 9]
     *
     * ByteMatrix.empty().columnStreams().count();                     // returns 0 (no columns)
     * ByteMatrix.wrap(new byte[][] {{7}, {8}}).columnStreams().count(); // returns 1 (single column)
     * }</pre>
     *
     * @return a Stream of ByteStream objects, one for each column in the matrix,
     *         or an empty stream when there are no columns (a {@code 0 x N} matrix still yields one
     *         empty stream per column)
     */
    @Override
    public Stream<ByteStream> columnStreams() {
        return columnStreams(0, columnCount);
    }

    /**
     * Returns a stream of ByteStream objects for a range of columns.
     * Each ByteStream in the result represents a complete column within the specified range.
     *
     * <p>This method allows for processing a subset of columns
     * while maintaining the ability to work with complete columns as individual streams.</p>
     *
     * <p>Each inner stream reads its column from this matrix as it is consumed, so later cell changes
     * and row swaps are visible.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
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
     * @return a Stream of ByteStream objects for the specified column range,
     *         with empty inner streams when the matrix has no rows
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    public Stream<ByteStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toColumnIndex;
            private int cursor = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            /**
             * {@inheritDoc}
             *
             * @throws NoSuchElementException if no elements remain in this iterator
             */
            @Override
            public ByteStream next() throws NoSuchElementException {
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

                    /**
                     * {@inheritDoc}
                     *
                     * @throws NoSuchElementException if no elements remain in this iterator
                     */
                    @Override
                    public byte nextByte() throws NoSuchElementException {
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
                        final long remaining = toIndex2 - cursor2;
                        cursor2 = toIndex2;
                        return remaining;
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
                final long remaining = toIndex - cursor;
                cursor = toIndex;
                return remaining;
            }
        });
    }

    /**
     * Returns the length of the given row array.
     * This is a hook called by {@link AbstractMatrix} during construction to determine the column
     * count of each row when validating the rectangular shape of the backing array.
     *
     * @param row the row array to measure; may be {@code null}
     * @return the length of {@code row}, or {@code 0} if {@code row} is {@code null}
     */
    @Override
    protected int length(final byte[] row) {
        return row == null ? 0 : row.length;
    }

    /**
     * Performs the specified action for each element in this matrix.
     * Elements are processed in row-major order (row by row, left to right) when executed sequentially.
     *
     * <p>The operation may be parallelized internally for large matrices to improve performance,
     * based on internal heuristics. If parallelized, the order of execution is not guaranteed,
     * but all elements are processed exactly once on successful completion. If parallelized,
     * {@code action} must be thread-safe.</p>
     *
     * <p>If {@code action} throws, sequential traversal stops at the failing call. During parallel
     * execution, other callbacks may already be running when a failure occurs.</p>
     *
     * <p><b>Note:</b> This method is for side-effect operations only (like printing, collecting,
     * or accumulating). Use {@link #map(Throwables.ByteUnaryOperator)} to create a new matrix,
     * or {@link #updateAll(Throwables.ByteUnaryOperator)} to modify this matrix in place.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     *
     * java.util.concurrent.atomic.AtomicInteger sum = new java.util.concurrent.atomic.AtomicInteger();
     * matrix.forEach(sum::addAndGet);
     * sum.get();                              // 10 (sum of all elements)
     *
     * java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger();
     * matrix.forEach(value -> count.incrementAndGet());
     * count.get();                            // 4 (number of elements)
     *
     * java.util.concurrent.atomic.AtomicInteger emptySum = new java.util.concurrent.atomic.AtomicInteger();
     * ByteMatrix.empty().forEach(emptySum::addAndGet);
     * emptySum.get();                                                   // 0 (no elements visited)
     * matrix.forEach((Throwables.ByteConsumer<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element; receives each element value
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if {@code action} throws while processing a matrix element
     * @see #forEach(int, int, int, int, Throwables.ByteConsumer)
     */
    public <E extends Exception> void forEach(final Throwables.ByteConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action, cs.action);

        forEach(0, rowCount, 0, columnCount, action);
    }

    /**
     * Performs the specified action for each element in the specified sub-matrix region.
     * Elements are processed in row-major order within the specified bounds when executed sequentially.
     *
     * <p>This method allows for processing a rectangular subset of the matrix.
     * The operation may be parallelized internally if the sub-matrix is large enough
     * to benefit from parallel processing; if parallelized, the order in which elements are
     * visited is unspecified and the action must be thread-safe, but every selected element is
     * visited exactly once on successful completion.</p>
     *
     * <p>If {@code action} throws, sequential traversal stops at the failing call. During parallel
     * execution, other callbacks may already be running when a failure occurs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     *
     * java.util.concurrent.atomic.AtomicInteger center = new java.util.concurrent.atomic.AtomicInteger();
     * matrix.forEach(1, 2, 1, 2, center::set);
     * int centerValue = center.get();          // 5 (center element only)
     *
     * java.util.concurrent.atomic.AtomicInteger subSum = new java.util.concurrent.atomic.AtomicInteger();
     * matrix.forEach(0, 2, 1, 3, subSum::addAndGet);
     * int selectedSum = subSum.get();          // 16 (2 + 3 + 5 + 6)
     *
     * matrix.forEach(0, 5, 0, 3, value -> { });                                     // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.forEach(0, 2, 0, 2, (Throwables.ByteConsumer<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @param action the action to be performed for each element in the sub-matrix
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex},
     *         or if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount}, or {@code fromColumnIndex > toColumnIndex}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if {@code action} throws while processing a matrix element
     */
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex,
            final Throwables.ByteConsumer<E> action) throws IndexOutOfBoundsException, IllegalArgumentException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, cs.action);

        if (Matrices.shouldRunInParallel(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, elementAction, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                final byte[] currentRow = a[i];

                for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                    action.accept(currentRow[j]);
                }
            }
        }
    }

    /**
     * Renders this matrix as a multi-line string (one row per line, e.g. {@code "[1, 2]\n[3, 4]"}); a
     * zero-row matrix renders {@code "[]"}. Supplies the rendering for {@link #println()}.
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

            return str;
        }
    }

    /**
     * Returns a hash code value for this matrix.
     * The hash code is computed from the deep contents of the internal two-dimensional array. A matrix
     * with no rows has no contents to hash, so its column count participates instead, keeping
     * {@code 0 x 3} and {@code 0 x 5} -- which {@code equals} distinguishes -- apart.
     * Matrices with the same dimensions and element values will have equal hash codes,
     * consistent with the {@link #equals(Object)} method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix1 = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix matrix2 = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * boolean sameHash = matrix1.hashCode() == matrix2.hashCode(); // true (equal content)
     *
     * ByteMatrix different = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 5}});
     * boolean sameHashForDifferentContent = matrix1.hashCode() == different.hashCode(); // false for these values
     * ByteMatrix.empty().hashCode();                                                    // returns a stable hash for the empty matrix
     * }</pre>
     *
     * @return a hash code value for this matrix
     */
    @Override
    public int hashCode() {
        // A zero-row matrix carries its column count outside the backing array, and equals() compares
        // it, so it must take part in the hash; every other shape is already distinguished by the data.
        return rowCount == 0 && columnCount > 0 ? 961 + columnCount : N.deepHashCode(a);
    }

    /**
     * Compares this matrix to the specified object for equality.
     * Returns {@code true} if the given object is also a {@code ByteMatrix} with the same dimensions
     * and all corresponding elements are equal. Returns {@code false} for any other type
     * (including primitive matrices of different element types).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix m1 = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * ByteMatrix m2 = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * m1.equals(m2);                          // returns true (same shape and elements)
     *
     * ByteMatrix m3 = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 5}});
     * m1.equals(m3);                          // returns false (different element)
     * m1.equals(null);                        // returns false
     * m1.equals("not a matrix");              // returns false (different type)
     * }</pre>
     *
     * @param obj the object to compare with; may be {@code null}
     * @return {@code true} if {@code obj} is a {@code ByteMatrix} with identical shape and elements,
     *         {@code false} otherwise
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
     * with each row on a separate line, use {@link #println()} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2}, {3, 4}});
     * matrix.toString();                      // returns "[[1, 2], [3, 4]]"
     *
     * ByteMatrix single = ByteMatrix.wrap(new byte[][] {{7}});
     * single.toString();                     // returns "[[7]]"
     * ByteMatrix.empty().toString();         // returns "[]"
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
