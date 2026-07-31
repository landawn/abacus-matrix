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
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.stream.CharIteratorEx;
import com.landawn.abacus.util.stream.CharStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * Matrix implementation backed by a rectangular {@code char[][]}.
 *
 * <p>This type specializes {@link AbstractMatrix} for {@code char} values while keeping the data in a
 * validated backing array. The constructor and {@link #of(char[]...)} wrap the supplied storage
 * directly. {@link #copyOf(char[]...)}, conversions, and mapping operations do not share mutable cell
 * storage with a non-empty source; operations producing an empty matrix may return the canonical empty
 * singleton.</p>
 *
 * <p>Cells introduced by growth or reshaping default to {@code (char) 0} (the NUL character) unless an overload accepts an
 * explicit fill value. Arithmetic operations (e.g. {@link #add(CharMatrix)}, {@link #subtract(CharMatrix)},
 * {@link #matrixMultiply(CharMatrix)}) compute results on the unsigned 16-bit code unit as {@code int} and cast
 * back to {@code char}, so values wrap modulo {@code 65536} (the range {@code [0, 65535]}); for example
 * {@code 'a' + 1 == 'b'} and adding {@code 1} to a cell holding {@code (char) 65535} wraps the cell to {@code 0}.</p>
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
 * @see ByteMatrix
 * @see BooleanMatrix
 * @see Matrix
 */
public final class CharMatrix extends AbstractMatrix<char[], CharList, CharStream, Stream<CharStream>, CharMatrix> {

    /**
     * Exclusive upper bound for random value generation: the size of the unsigned 16-bit
     * {@code char} range ({@code Character.MAX_VALUE + 1}, i.e. {@code 65536}).
     */
    static final int BOUND = Character.MAX_VALUE + 1;
    private static final CharMatrix EMPTY_CHAR_MATRIX = new CharMatrix(new char[0][0]);

    /**
     * Constructs a {@code CharMatrix} backed by the supplied two-dimensional array.
     *
     * <p><b>&#9888;&#65039; Shared backing:</b> The supplied array is used directly after rectangular-shape validation, so later modifications to either the input
     * array or the matrix remain visible through the other view. Call {@link #copy()} if you need an
     * independently owned matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[][] data = {{'a', 'b'}, {'c', 'd'}};
     * CharMatrix matrix = new CharMatrix(data);
     * matrix.get(0, 0);                  // returns 'a'
     * data[0][0] = 'x';                  // backing array is shared
     * matrix.get(0, 0);                  // returns 'x' (mutation is visible)
     *
     * new CharMatrix((char[][]) null);                  // throws IllegalArgumentException
     * new CharMatrix(new char[][] {{'a', 'b'}, {'c'}}); // throws IllegalArgumentException (jagged rows)
     * }</pre>
     *
     * @param a the two-dimensional char array to wrap, must not be {@code null}
     * @throws IllegalArgumentException if {@code a} is {@code null}, if any row of {@code a} is {@code null}, or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public CharMatrix(final char[][] a) {
        super(N.checkArgNotNull(a, "Matrix array cannot be null"), char.class);
    }

    /**
     * Returns the shared empty {@code 0x0} matrix instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.empty();
     * matrix.rowCount();                                                // returns 0
     * matrix.columnCount();                                             // returns 0
     * matrix.isEmpty();                                                 // returns true
     * boolean sameSingleton = CharMatrix.empty() == CharMatrix.empty(); // true (shared singleton)
     * }</pre>
     *
     * @return the canonical empty {@code CharMatrix} (singleton)
     */
    public static CharMatrix empty() {
        return EMPTY_CHAR_MATRIX;
    }

    /**
     * Creates a {@code CharMatrix} from a two-dimensional char array.
     *
     * <p><b>&#9888;&#65039; Shared backing:</b> When the input has at least one row, the provided array is used directly without defensive copying.
     * Changes to the input array are reflected in the returned matrix, and vice versa. A zero-row input is instead canonicalized to the shared empty matrix,
     * so its outer-array identity is not retained.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.get(1, 0);                  // returns 'c'
     * matrix.get(0, 1);                  // returns 'b'
     *
     * CharMatrix.of((char[][]) null);          // throws IllegalArgumentException
     * CharMatrix.of(new char[0][0]).isEmpty(); // returns true
     * }</pre>
     *
     * @param a the two-dimensional char array to wrap, or empty for an empty matrix; must not be {@code null}
     * @return a new {@code CharMatrix} backed by {@code a}, or the shared empty matrix if {@code a} is empty
     * @throws IllegalArgumentException if {@code a} is {@code null}, if any row of {@code a} is {@code null}, or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public static CharMatrix of(final char[]... a) {
        N.checkArgNotNull(a, "Matrix array cannot be null");
        return a.length == 0 ? EMPTY_CHAR_MATRIX : new CharMatrix(a);
    }

    /**
     * Creates a {@code CharMatrix} that owns a defensive deep copy of the supplied two-dimensional array.
     *
     * <p>For a non-empty input, unlike {@link #of(char[][])}, which wraps the caller's array without copying,
     * this factory allocates a new outer array and clones every row. Subsequent modifications to {@code a}
     * (or its rows) are therefore <b>not</b> visible through the returned matrix, and vice versa. A zero-row
     * input is canonicalized to the shared empty matrix, so its outer-array identity is not retained.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[][] data = {{'a', 'b'}, {'c', 'd'}};
     * CharMatrix matrix = CharMatrix.copyOf(data);
     * data[0][0] = 'x';
     * matrix.get(0, 0);                       // returns 'a' (copy is independent)
     *
     * CharMatrix.copyOf((char[][]) null);                  // throws IllegalArgumentException
     * CharMatrix.copyOf(new char[][] {{'a', 'b'}, {'c'}}); // throws IllegalArgumentException (non-rectangular)
     * }</pre>
     *
     * @param a the two-dimensional char array to copy, or empty for an empty matrix; must not be {@code null}
     * @return a new {@code CharMatrix} backed by a deep copy of {@code a}, or the shared empty matrix if {@code a} is empty
     * @throws IllegalArgumentException if {@code a} is {@code null}, if any row of {@code a} is {@code null}, or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     * @see #of(char[][])
     * @see #copy()
     */
    public static CharMatrix copyOf(final char[]... a) {
        N.checkArgNotNull(a, "Matrix array cannot be null");

        if (a.length == 0) {
            return EMPTY_CHAR_MATRIX;
        }

        final char[][] c = new char[a.length][];

        for (int i = 0, len = a.length; i < len; i++) {
            c[i] = a[i] == null ? null : a[i].clone();
        }

        return new CharMatrix(c);
    }

    /**
     * Creates a new {@code 1 x length} matrix filled with random char values drawn uniformly from
     * the full unsigned 16-bit range {@code [0, 65535]}. Values are not constrained to printable
     * characters and may include surrogates and control codes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.randomRow(5);
     * matrix.rowCount();            // returns 1
     * matrix.columnCount();         // returns 5 (values are random)
     *
     * CharMatrix.randomRow(0).columnCount(); // returns 0 (empty single row)
     * CharMatrix.randomRow(-1);              // throws IllegalArgumentException
     * }</pre>
     *
     * @param length the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code CharMatrix} of dimensions {@code 1 x length} filled with random values
     * @throws IllegalArgumentException if {@code length} is negative
     * @see #random(int, int)
     */
    public static CharMatrix randomRow(final int length) {
        N.checkArgument(length >= 0, MSG_NEGATIVE_DIMENSION, "length", length);

        return random(1, length);
    }

    /**
     * Creates a new matrix of the specified dimensions filled with random char values drawn
     * uniformly from the full unsigned 16-bit range {@code [0, 65535]}. Values are not
     * constrained to printable characters and may include surrogates and control codes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.random(2, 3);
     * matrix.rowCount();            // returns 2
     * matrix.columnCount();         // returns 3 (values are random)
     *
     * CharMatrix.random(0, 0).isEmpty(); // returns true
     * CharMatrix.random(-1, 3);          // throws IllegalArgumentException
     * CharMatrix.random(0, 3);           // throws IllegalArgumentException (0 rows, 3 columns not representable)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code CharMatrix} of dimensions {@code rowCount x columnCount} filled with random values
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative, or
     *         if the resulting shape cannot be represented (for example {@code rowCount == 0} with
     *         {@code columnCount > 0})
     */
    public static CharMatrix random(final int rowCount, final int columnCount) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final char[][] a = new char[rowCount][columnCount];

        for (char[] ea : a) {
            for (int i = 0; i < columnCount; i++) {
                ea[i] = (char) RAND.nextInt(BOUND);
            }
        }

        return new CharMatrix(a);
    }

    /**
     * Creates a new matrix of the specified dimensions where every element is the provided {@code element}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.repeat(2, 3, 'a');
     * matrix.get(0, 0);            // returns 'a'
     * matrix.get(1, 2);            // returns 'a'
     * matrix.rowCount();           // returns 2
     *
     * CharMatrix.repeat(0, 0, 'a').isEmpty(); // returns true
     * CharMatrix.repeat(-1, 3, 'a');          // throws IllegalArgumentException
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @param element the char value to fill the matrix with
     * @return a new {@code CharMatrix} of dimensions {@code rowCount x columnCount} filled with the specified element
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative, or
     *         if the resulting shape cannot be represented (for example {@code rowCount == 0} with
     *         {@code columnCount > 0})
     */
    public static CharMatrix repeat(final int rowCount, final int columnCount, final char element) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final char[][] a = new char[rowCount][columnCount];

        for (char[] ea : a) {
            N.fill(ea, element);
        }

        return new CharMatrix(a);
    }

    /**
     * Creates a single-row {@code CharMatrix} containing a half-open range of char values.
     * The range is {@code [startInclusive, endExclusive)} with an implicit step of {@code +1}.
     * If {@code endExclusive <= startInclusive}, the result is an empty matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix.range('a', 'e').rowView(0);   // returns ['a', 'b', 'c', 'd']
     * CharMatrix.range('a', 'b').rowView(0);   // returns ['a']
     *
     * int equalBoundsLength = CharMatrix.range('a', 'a').rowView(0).length;      // 0 (empty: start == end)
     * int descendingBoundsLength = CharMatrix.range('e', 'a').rowView(0).length; // 0 (empty: end < start)
     * }</pre>
     *
     * @param startInclusive the starting char value (inclusive)
     * @param endExclusive the ending char value (exclusive)
     * @return a single-row {@code CharMatrix} containing the range of values
     */
    public static CharMatrix range(final char startInclusive, final char endExclusive) {
        return new CharMatrix(new char[][] { Array.range(startInclusive, endExclusive) });
    }

    /**
     * Creates a single-row {@code CharMatrix} containing the half-open range
     * {@code [startInclusive, endExclusive)} stepped by {@code step}.
     * Supports both ascending (positive step) and descending (negative step) sequences.
     * If the step direction does not advance from {@code startInclusive} toward {@code endExclusive},
     * a {@code 1x0} matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix.range('a', 'g', 2).rowView(0);   // returns ['a', 'c', 'e']
     * CharMatrix.range('z', 'u', -2).rowView(0);  // returns ['z', 'x', 'v']
     *
     * int wrongDirectionLength = CharMatrix.range('a', 'z', -1).rowView(0).length; // 0 (step direction wrong)
     * CharMatrix.range('a', 'z', 0);                                               // throws IllegalArgumentException (step is zero)
     * }</pre>
     *
     * @param startInclusive the starting char value (inclusive)
     * @param endExclusive the ending char value (exclusive)
     * @param step the step size (must not be zero; can be positive or negative)
     * @return a new {@code 1xn} {@code CharMatrix} of values from {@code startInclusive} stepped by {@code step}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static CharMatrix range(final char startInclusive, final char endExclusive, final int step) {
        return new CharMatrix(new char[][] { Array.range(startInclusive, endExclusive, step) });
    }

    /**
     * Creates a single-row {@code CharMatrix} containing a closed range of char values.
     * The range is {@code [startInclusive, endInclusive]} with an implicit step of {@code +1}.
     * If {@code endInclusive < startInclusive}, the result is an empty matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix.rangeClosed('a', 'd').rowView(0);   // returns ['a', 'b', 'c', 'd']
     * CharMatrix.rangeClosed('a', 'a').rowView(0);   // returns ['a'] (single element)
     *
     * int descendingBoundsLength = CharMatrix.rangeClosed('d', 'a').rowView(0).length;         // 0 (end < start)
     * int adjacentDescendingBoundsLength = CharMatrix.rangeClosed('b', 'a').rowView(0).length; // 0 (end < start)
     * }</pre>
     *
     * @param startInclusive the starting char value (inclusive)
     * @param endInclusive the ending char value (inclusive)
     * @return a single-row {@code CharMatrix} containing the range of values
     */
    public static CharMatrix rangeClosed(final char startInclusive, final char endInclusive) {
        return new CharMatrix(new char[][] { Array.rangeClosed(startInclusive, endInclusive) });
    }

    /**
     * Creates a single-row {@code CharMatrix} containing the closed range
     * {@code [startInclusive, endInclusive]} stepped by {@code step}.
     * Supports both ascending (positive step) and descending (negative step) sequences.
     * If the step direction does not advance from {@code startInclusive} toward {@code endInclusive},
     * a {@code 1x0} matrix is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix.rangeClosed('a', 'g', 2).rowView(0);    // returns ['a', 'c', 'e', 'g']
     * CharMatrix.rangeClosed('a', 'i', 2).rowView(0);    // returns ['a', 'c', 'e', 'g', 'i']
     * CharMatrix.rangeClosed('z', 'u', -2).rowView(0);   // returns ['z', 'x', 'v']
     *
     * int wrongDirectionLength = CharMatrix.rangeClosed('a', 'z', -1).rowView(0).length; // 0 (step direction wrong)
     * CharMatrix.rangeClosed('a', 'z', 0);                                               // throws IllegalArgumentException (step is zero)
     * }</pre>
     *
     * @param startInclusive the starting char value (inclusive)
     * @param endInclusive the ending char value (inclusive, if reachable by stepping)
     * @param step the step size (must not be zero; can be positive or negative)
     * @return a new {@code 1xn} {@code CharMatrix} of values from {@code startInclusive} stepped by {@code step}
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static CharMatrix rangeClosed(final char startInclusive, final char endInclusive, final int step) {
        return new CharMatrix(new char[][] { Array.rangeClosed(startInclusive, endInclusive, step) });
    }

    /**
     * Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
     * All other elements (off-diagonal) are set to zero (the null character U+0000). The matrix size is n×n where n is the length
     * of the diagonal array. The main diagonal runs from top-left to bottom-right.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.mainDiagonal(new char[] {'a', 'b', 'c'});
     * matrix.get(0, 0);                                         // returns 'a'
     * matrix.get(2, 2);                                         // returns 'c'
     * boolean offDiagonalIsZero = matrix.get(0, 1) == (char) 0; // true (off-diagonal default)
     * CharMatrix.mainDiagonal((char[]) null);                   // throws IllegalArgumentException (null array)
     * CharMatrix.mainDiagonal(new char[0]).isEmpty();           // returns true
     * // Resulting 3x3 matrix:
     * //   {'a', (char) 0, (char) 0},
     * //   {(char) 0, 'b', (char) 0},
     * //   {(char) 0, (char) 0, 'c'}
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements; must not be {@code null}, but may be empty,
     *        in which case the empty matrix is returned
     * @return a square matrix with the specified main diagonal (n×n where n = diagonal length),
     *         or an empty matrix if {@code mainDiagonal} is empty
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null}
     * @see #antiDiagonal(char[])
     * @see #diagonals(char[], char[])
     */
    public static CharMatrix mainDiagonal(final char[] mainDiagonal) {
        N.checkArgNotNull(mainDiagonal, "mainDiagonal");

        return diagonals(mainDiagonal, null);
    }

    /**
     * Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
     * All other elements (off-diagonal) are set to zero (the null character U+0000). The matrix size is n×n where n is the length
     * of the diagonal array. The anti-diagonal runs from top-right to bottom-left.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.antiDiagonal(new char[] {'a', 'b', 'c'});
     * matrix.get(0, 2);                                             // returns 'a'
     * matrix.get(2, 0);                                             // returns 'c'
     * boolean offAntiDiagonalIsZero = matrix.get(0, 0) == (char) 0; // true (off-anti-diagonal default)
     * CharMatrix.antiDiagonal((char[]) null);                       // throws IllegalArgumentException (null array)
     * CharMatrix.antiDiagonal(new char[0]).isEmpty();               // returns true
     * // Resulting 3x3 matrix:
     * //   {(char) 0, (char) 0, 'a'},
     * //   {(char) 0, 'b', (char) 0},
     * //   {'c', (char) 0, (char) 0}
     * }</pre>
     *
     * @param antiDiagonal the array of anti-diagonal elements; must not be {@code null}, but may be empty,
     *        in which case the empty matrix is returned
     * @return a square matrix with the specified anti-diagonal (n×n where n = diagonal length),
     *         or an empty matrix if {@code antiDiagonal} is empty
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null}
     * @see #mainDiagonal(char[])
     * @see #diagonals(char[], char[])
     */
    public static CharMatrix antiDiagonal(final char[] antiDiagonal) {
        N.checkArgNotNull(antiDiagonal, "antiDiagonal");

        return diagonals(null, antiDiagonal);
    }

    /**
     * Creates a square matrix from the specified main diagonal and anti-diagonal elements.
     * All other elements are set to zero (the null character U+0000). If both arrays are non-empty, they must have the same length.
     * The resulting matrix has dimensions n×n where n is the length of the non-empty diagonal array.
     * When both diagonals are provided and they overlap (at the center element of odd-sized matrices),
     * the main diagonal value takes precedence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.diagonals(new char[] {'a', 'b', 'c'}, new char[] {'x', 'y', 'z'});
     * matrix.get(0, 0);            // returns 'a' (main diagonal)
     * matrix.get(0, 2);            // returns 'x' (anti-diagonal)
     * matrix.get(1, 1);            // returns 'b' (overlap: main takes precedence)
     * matrix.get(2, 0);            // returns 'z'
     * // Resulting 3x3 matrix:
     * //   {'a', (char) 0, 'x'},
     * //   {(char) 0, 'b', (char) 0},
     * //   {'z', (char) 0, 'c'}
     *
     * CharMatrix.diagonals((char[]) null, (char[]) null);                      // throws IllegalArgumentException (both null)
     * CharMatrix.diagonals(new char[] {'a', 'b'}, new char[] {'x', 'y', 'z'}); // throws IllegalArgumentException (length mismatch)
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements; may be {@code null} if {@code antiDiagonal} is non-{@code null};
     *        may be empty
     * @param antiDiagonal the array of anti-diagonal elements; may be {@code null} if {@code mainDiagonal} is non-{@code null};
     *        may be empty
     * @return a square matrix with the specified diagonals, or an empty matrix when both supplied diagonals are empty or one is {@code null} and the other is empty
     * @throws IllegalArgumentException if both {@code mainDiagonal} and {@code antiDiagonal} are {@code null}, or if both arrays are non-empty and have different lengths
     * @see #mainDiagonal(char[])
     * @see #antiDiagonal(char[])
     */
    public static CharMatrix diagonals(final char[] mainDiagonal, final char[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(mainDiagonal != null || antiDiagonal != null, "Both 'mainDiagonal' and 'antiDiagonal' can't be null");

        N.checkArgument(N.isEmpty(mainDiagonal) || N.isEmpty(antiDiagonal) || mainDiagonal.length == antiDiagonal.length,
                "The lengths of 'mainDiagonal' and 'antiDiagonal' must be the same: mainDiagonal length={}, antiDiagonal length={}", N.len(mainDiagonal),
                N.len(antiDiagonal));

        if (N.isEmpty(mainDiagonal) && N.isEmpty(antiDiagonal)) {
            return EMPTY_CHAR_MATRIX;
        }

        final int len = N.max(N.len(mainDiagonal), N.len(antiDiagonal));
        final char[][] result = new char[len][len];

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

        return new CharMatrix(result);
    }

    /**
     * Converts a boxed {@link Matrix Matrix&lt;Character&gt;} to a primitive {@code CharMatrix}.
     * {@code null} values in the input matrix are converted to {@code (char) 0} (the null character).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Character> boxedMatrix = Matrix.of(new Character[][] {{'a', 'b'}, {null, 'c'}});
     * CharMatrix primitive = CharMatrix.unbox(boxedMatrix);
     * primitive.get(0, 0);                        // returns 'a'
     * primitive.get(1, 1);                        // returns 'c'
     * primitive.get(1, 0);                        // returns the null character (null mapped to default)
     * CharMatrix.unbox((Matrix<Character>) null); // throws IllegalArgumentException
     * // null is converted to (char) 0: [['a', 'b'], [(char) 0, 'c']]
     * }</pre>
     *
     * @param x the boxed {@code Character} matrix to convert; must not be {@code null}
     * @return a new {@code CharMatrix} with primitive char values
     * @throws IllegalArgumentException if {@code x} is {@code null}
     * @see #boxed()
     */
    public static CharMatrix unbox(final Matrix<Character> x) {
        N.checkArgNotNull(x, "x");

        return CharMatrix.of(Array.unbox(x.a));
    }

    /**
     * Returns the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.get(0, 1);            // returns 'b'
     * matrix.get(1, 0);            // returns 'c'
     *
     * matrix.get(5, 0);            // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * matrix.get(0, 5);            // throws ArrayIndexOutOfBoundsException (column out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return the element at position (rowIndex, columnIndex)
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public char get(final int rowIndex, final int columnIndex) {
        return a[rowIndex][columnIndex];
    }

    /**
     * Returns the element at the specified point.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.get(Point.of(0, 1));      // returns 'b'
     * matrix.get(Point.of(1, 1));      // returns 'd'
     *
     * matrix.get((Point) null);        // throws IllegalArgumentException (null point)
     * matrix.get(Point.of(5, 0));      // throws ArrayIndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @return the char element at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #get(int, int)
     */
    public char get(final Point point) {
        N.checkArgNotNull(point, "point");

        return a[point.rowIndex()][point.columnIndex()];
    }

    /**
     * Sets the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.set(0, 1, 'x');
     * matrix.get(0, 1);            // returns 'x' (was 'b')
     * matrix.set(1, 0, 'y');
     * matrix.get(1, 0);            // returns 'y' (was 'c')
     *
     * matrix.set(5, 0, 'z');       // throws ArrayIndexOutOfBoundsException (row out of bounds)
     * matrix.set(0, 5, 'z');       // throws ArrayIndexOutOfBoundsException (column out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @param value the value to set
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public void set(final int rowIndex, final int columnIndex, final char value) {
        a[rowIndex][columnIndex] = value;
    }

    /**
     * Sets the element at the specified point to the given value.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.set(Point.of(0, 1), 'x');
     * matrix.get(0, 1);                  // returns 'x' (was 'b')
     * matrix.set(Point.of(1, 1), 'y');
     * matrix.get(1, 1);                  // returns 'y' (was 'd')
     *
     * matrix.set((Point) null, 'z');     // throws IllegalArgumentException (null point)
     * matrix.set(Point.of(5, 0), 'z');   // throws ArrayIndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @param value the new char value to set at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #set(int, int, char)
     */
    public void set(final Point point, final char value) {
        N.checkArgNotNull(point, "point");

        a[point.rowIndex()][point.columnIndex()] = value;
    }

    /**
     * Returns the element directly above the specified position, or an empty {@link OptionalChar}
     * if the position is on the top edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalChar} is returned for the top
     * row instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.valueAbove(1, 0).get();         // returns 'a'
     * matrix.valueAbove(1, 1).get();         // returns 'b'
     *
     * matrix.valueAbove(0, 0).isPresent();   // returns false (top row, no cell above)
     * matrix.valueAbove(2, 0);               // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalChar} containing the element at position {@code (rowIndex - 1, columnIndex)},
     *         or empty if {@code rowIndex == 0}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalChar valueAbove(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? OptionalChar.empty() : OptionalChar.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, or an empty {@link OptionalChar}
     * if the position is on the bottom edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalChar} is returned for the
     * bottom row instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.valueBelow(0, 0).get();         // returns 'c'
     * matrix.valueBelow(0, 1).get();         // returns 'd'
     *
     * matrix.valueBelow(1, 0).isPresent();   // returns false (bottom row, no cell below)
     * matrix.valueBelow(2, 0);               // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalChar} containing the element at position {@code (rowIndex + 1, columnIndex)},
     *         or empty if {@code rowIndex == rowCount - 1}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalChar valueBelow(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? OptionalChar.empty() : OptionalChar.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, or an empty
     * {@link OptionalChar} if the position is on the leftmost edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalChar} is returned for the
     * leftmost column instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.valueLeft(0, 1).get();          // returns 'a'
     * matrix.valueLeft(1, 1).get();          // returns 'c'
     *
     * matrix.valueLeft(0, 0).isPresent();    // returns false (leftmost column, no cell to the left)
     * matrix.valueLeft(0, 2);                // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalChar} containing the element at position {@code (rowIndex, columnIndex - 1)},
     *         or empty if {@code columnIndex == 0}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalChar valueLeft(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? OptionalChar.empty() : OptionalChar.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, or an empty
     * {@link OptionalChar} if the position is on the rightmost edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalChar} is returned for the
     * rightmost column instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.valueRight(0, 0).get();         // returns 'b'
     * matrix.valueRight(1, 0).get();         // returns 'd'
     *
     * matrix.valueRight(0, 1).isPresent();   // returns false (rightmost column, no cell to the right)
     * matrix.valueRight(0, 2);               // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalChar} containing the element at position {@code (rowIndex, columnIndex + 1)},
     *         or empty if {@code columnIndex == columnCount - 1}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalChar valueRight(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? OptionalChar.empty() : OptionalChar.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as a live reference to the underlying {@code char[]} storage.
     *
     * <p><b>&#9888;&#65039; Live view:</b> This method returns the internal array, not a copy. Modifications to the
     * returned array will affect the matrix and vice versa. Use {@link #rowCopy(int)} if you need
     * an independent copy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.rowView(0);                      // returns ['a', 'b', 'c']
     * matrix.rowView(1);                      // returns ['d', 'e', 'f']
     *
     * char[] firstRow = matrix.rowView(0);
     * firstRow[0] = 'x';
     * matrix.get(0, 0);                       // returns 'x' (live view is shared)
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
    public char[] rowView(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return a[rowIndex];
    }

    /**
     * Returns a defensive copy of the specified row as a new {@code char[]}.
     * Changes to the returned array do not affect this matrix and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.rowCopy(0);                      // returns ['a', 'b', 'c']
     * matrix.rowCopy(1);                      // returns ['d', 'e', 'f']
     *
     * char[] firstRow = matrix.rowCopy(0);
     * firstRow[0] = 'x';
     * matrix.get(0, 0);                       // returns 'a' (copy is independent)
     *
     * matrix.rowCopy(-1);                     // throws IndexOutOfBoundsException
     * matrix.rowCopy(2);                      // throws IndexOutOfBoundsException (rowIndex >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new char array of length {@code columnCount} containing the values of the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowView(int)
     * @see #columnCopy(int)
     */
    @Override
    public char[] rowCopy(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return N.copyOf(a[rowIndex], columnCount);
    }

    /**
     * Returns a defensive copy of the specified column as a new {@code char[]}.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously in memory. Modifications to the returned array
     * will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.columnCopy(0);                   // returns ['a', 'd']
     * matrix.columnCopy(2);                   // returns ['c', 'f']
     *
     * char[] firstColumn = matrix.columnCopy(0);
     * firstColumn[0] = 'x';
     * matrix.get(0, 0);                       // returns 'a' (copy is independent)
     *
     * matrix.columnCopy(-1);                  // throws IndexOutOfBoundsException
     * matrix.columnCopy(3);                   // throws IndexOutOfBoundsException (columnIndex >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new char array of length {@code rowCount} containing the values of the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     * @see #rowCopy(int)
     * @see #rowView(int)
     */
    @Override
    public char[] columnCopy(final int columnIndex) throws IndexOutOfBoundsException {
        checkColumnIndex(columnIndex);

        final char[] c = new char[rowCount];

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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.setRow(0, new char[] {'x', 'y', 'z'});
     * matrix.rowView(0);          // returns ['x', 'y', 'z']
     * matrix.rowView(1);          // returns ['d', 'e', 'f'] (unchanged)
     *
     * matrix.setRow(0, new char[] {'x', 'y'});       // throws IllegalArgumentException (length mismatch)
     * matrix.setRow(5, new char[] {'x', 'y', 'z'});  // throws IndexOutOfBoundsException (row out of bounds)
     * }</pre>
     *
     * @param rowIndex the index of the row to set (0-based)
     * @param row the array of values to copy into the row; must be non-{@code null} and of length {@code columnCount}
     * @throws IndexOutOfBoundsException if {@code rowIndex} is out of bounds
     * @throws IllegalArgumentException if {@code row} is {@code null} or if {@code row.length != columnCount}
     */
    public void setRow(final int rowIndex, final char[] row) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.setColumn(0, new char[] {'x', 'y'});
     * matrix.get(0, 0);           // returns 'x'
     * matrix.get(1, 0);           // returns 'y'
     * // Matrix is now: [['x', 'b', 'c'], ['y', 'e', 'f']]
     *
     * matrix.setColumn(0, new char[] {'x'});       // throws IllegalArgumentException (length mismatch)
     * matrix.setColumn(5, new char[] {'x', 'y'});  // throws IndexOutOfBoundsException (column out of bounds)
     * }</pre>
     *
     * @param columnIndex the index of the column to set (0-based)
     * @param column the array of values to copy into the column; must be non-{@code null} and of length {@code rowCount}
     * @throws IndexOutOfBoundsException if {@code columnIndex} is out of bounds
     * @throws IllegalArgumentException if {@code column} is {@code null} or if {@code column.length != rowCount}
     */
    public void setColumn(final int columnIndex, final char[] column) throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkArgNotNull(column, "column");
        checkColumnIndex(columnIndex);
        N.checkArgument(column.length == rowCount, MSG_COLUMN_LENGTH_MISMATCH, rowCount, column.length);
        final char[] values = snapshotIfBackingRow(column);

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = values[i];
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.updateRow(0, c -> Character.toUpperCase(c));
     * matrix.rowView(0);          // returns ['A', 'B', 'C']
     * matrix.rowView(1);          // returns ['d', 'e', 'f'] (unchanged)
     *
     * matrix.updateRow(0, c -> (char) (c + 1)); // shifts row 0 by +1 -> ['B', 'C', 'D']
     * matrix.updateRow(5, c -> c);              // throws IndexOutOfBoundsException (row out of bounds)
     * matrix.updateRow(0, null);                // throws IllegalArgumentException (operator is null)
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
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.CharUnaryOperator<E> operator)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        N.checkArgNotNull(operator, "operator");

        checkRowIndex(rowIndex);

        final char[] row = a[rowIndex];

        for (int i = 0; i < columnCount; i++) {
            row[i] = operator.applyAsChar(row[i]);
        }
    }

    /**
     * Updates all elements in a column in-place by applying the specified operator to each element.
     * This modifies the matrix directly.
     *
     * <p>The operator is applied to each element in the specified column sequentially
     * from top to bottom (row 0 to row rowCount-1).</p>
     *
     * <p>If multiple logical rows share the same backing array, the shared cell at
     * {@code columnIndex} is transformed exactly once, when that backing row is first encountered.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'A', 'B'}, {'C', 'D'}});
     * matrix.updateColumn(1, c -> Character.toLowerCase(c));
     * matrix.get(0, 1);           // returns 'b'
     * matrix.get(1, 1);           // returns 'd'
     * matrix.get(0, 0);           // returns 'A' (unchanged)
     *
     * matrix.updateColumn(0, c -> (char) (c + 1)); // shifts column 0 by +1
     * matrix.updateColumn(5, c -> c);              // throws IndexOutOfBoundsException (column out of bounds)
     * matrix.updateColumn(0, null);                // throws IllegalArgumentException (operator is null)
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
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.CharUnaryOperator<E> operator)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        N.checkArgNotNull(operator, "operator");

        checkColumnIndex(columnIndex);

        forEachDistinctRow(row -> row[columnIndex] = operator.applyAsChar(row[columnIndex]));
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}, {'g', 'h', 'i'}});
     * matrix.mainDiagonalCopy();   // returns ['a', 'e', 'i']
     *
     * CharMatrix single = CharMatrix.of(new char[][] {{'x'}});
     * single.mainDiagonalCopy();   // returns ['x']
     *
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.mainDiagonalCopy(); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new char array containing a copy of the main diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public char[] mainDiagonalCopy() throws IllegalStateException {
        checkIsSquare();

        final char[] res = new char[rowCount];

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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'},
     *                                                 {'d', 'e', 'f'},
     *                                                 {'g', 'h', 'i'}});
     * matrix.setMainDiagonal(new char[] {'x', 'y', 'z'});
     * matrix.mainDiagonalCopy();   // returns ['x', 'y', 'z']
     * matrix.get(0, 1);            // returns 'b' (off-diagonal unchanged)
     *
     * matrix.setMainDiagonal(new char[] {'x', 'y'}); // throws IllegalArgumentException (length mismatch)
     *
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.setMainDiagonal(new char[] {'x', 'y'}); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must be non-{@code null} and of length {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null} or its length is not equal to {@code rowCount}
     */
    @Override
    public void setMainDiagonal(final char[] mainDiagonal) throws IllegalStateException, IllegalArgumentException {
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.updateMainDiagonal(c -> Character.toUpperCase(c));
     * matrix.mainDiagonalCopy();   // returns ['A', 'D']
     * matrix.get(0, 1);            // returns 'b' (off-diagonal unchanged)
     * matrix.get(1, 0);            // returns 'c' (off-diagonal unchanged)
     *
     * matrix.updateMainDiagonal(null);      // throws IllegalArgumentException (operator is null)
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.updateMainDiagonal(c -> c); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each diagonal element; receives current element value and returns new value
     * @throws IllegalStateException if the matrix is not square
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateMainDiagonal(final Throwables.CharUnaryOperator<E> operator)
            throws IllegalStateException, IllegalArgumentException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = operator.applyAsChar(a[i][i]);
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'},
     *                                                 {'d', 'e', 'f'},
     *                                                 {'g', 'h', 'i'}});
     * matrix.antiDiagonalCopy();   // returns ['c', 'e', 'g']
     *
     * CharMatrix single = CharMatrix.of(new char[][] {{'x'}});
     * single.antiDiagonalCopy();   // returns ['x']
     *
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.antiDiagonalCopy(); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new char array containing a copy of the anti-diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public char[] antiDiagonalCopy() throws IllegalStateException {
        checkIsSquare();

        final char[] res = new char[rowCount];

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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'},
     *                                                 {'d', 'e', 'f'},
     *                                                 {'g', 'h', 'i'}});
     * matrix.setAntiDiagonal(new char[] {'x', 'y', 'z'});
     * matrix.antiDiagonalCopy();   // returns ['x', 'y', 'z']
     * matrix.get(0, 0);            // returns 'a' (off-anti-diagonal unchanged)
     *
     * matrix.setAntiDiagonal(new char[] {'x', 'y'}); // throws IllegalArgumentException (length mismatch)
     *
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.setAntiDiagonal(new char[] {'x', 'y'}); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must be non-{@code null} and of length {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null} or its length is not equal to {@code rowCount}
     */
    @Override
    public void setAntiDiagonal(final char[] antiDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgNotNull(antiDiagonal, "antiDiagonal");
        N.checkArgument(N.len(antiDiagonal) == rowCount, MSG_DIAGONAL_LENGTH_MISMATCH, rowCount, N.len(antiDiagonal));
        final char[] values = snapshotIfBackingRow(antiDiagonal);

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = values[i];
        }
    }

    /**
     * Updates the values on the anti-diagonal (upper-right to lower-left) by applying the specified operator.
     * The matrix must be square.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.updateAntiDiagonal(c -> Character.toUpperCase(c));
     * matrix.antiDiagonalCopy();   // returns ['B', 'C']
     * matrix.get(0, 0);            // returns 'a' (off-anti-diagonal unchanged)
     * matrix.get(1, 1);            // returns 'd' (off-anti-diagonal unchanged)
     *
     * matrix.updateAntiDiagonal(null);      // throws IllegalArgumentException (operator is null)
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.updateAntiDiagonal(c -> c); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each anti-diagonal element; receives current element value and returns new value
     * @throws IllegalStateException if the matrix is not square
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAntiDiagonal(final Throwables.CharUnaryOperator<E> operator)
            throws IllegalStateException, IllegalArgumentException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = operator.applyAsChar(a[i][columnCount - i - 1]);
        }
    }

    /**
     * Updates all elements in the matrix in-place by applying the specified operator.
     * This modifies the matrix directly.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance. If parallelized, the supplied function must be thread-safe.
     * When this operation is not parallelized, elements are processed in first-occurrence row-major order;
     * when it is parallelized, the encounter order is unspecified.</p>
     *
     * <p>If multiple logical rows share the same backing array, each backing row is updated only
     * once, at its first occurrence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.updateAll(c -> Character.toUpperCase(c));
     * matrix.rowView(0);          // returns ['A', 'B']
     * matrix.rowView(1);          // returns ['C', 'D']
     *
     * CharMatrix shift = CharMatrix.of(new char[][] {{'a', 'b'}});
     * shift.updateAll(c -> (char) (c + 1));
     * shift.rowView(0);           // returns ['b', 'c']
     *
     * CharMatrix.empty().updateAll(c -> c);                                    // no-op on empty matrix (no elements)
     * matrix.updateAll((Throwables.CharUnaryOperator<RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each element; receives the current element value
     *             and returns the new value
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.CharUnaryOperator<E> operator) throws IllegalArgumentException, E {
        N.checkArgNotNull(operator, "operator");

        if (columnCount == 0) {
            return;
        }

        final boolean runInParallel = Matrices.shouldRunInParallel(this);

        if (runInParallel && !hasAliasedRows()) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = operator.applyAsChar(a[i][j]);
            Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
        } else if (runInParallel) {
            final char[][] distinctRows = new char[rowCount][];
            final int[] distinctRowCount = { 0 };
            forEachDistinctRow(row -> distinctRows[distinctRowCount[0]++] = row);

            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> distinctRows[i][j] = operator.applyAsChar(distinctRows[i][j]);
            final long distinctElementCount = (long) distinctRowCount[0] * columnCount;
            Matrices.forEachIndices(distinctRowCount[0], columnCount, elementAction, Matrices.shouldRunInParallel(this, distinctElementCount));
        } else {
            forEachDistinctRow(row -> {
                for (int j = 0; j < columnCount; j++) {
                    row[j] = operator.applyAsChar(row[j]);
                }
            });
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
     * <p>If logical rows share a backing array, every logical coordinate is still visited, but
     * traversal is kept sequential so later aliases overwrite earlier ones deterministically.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[3][4]);
     * matrix.updateAll((i, j) -> (char)('a' + i * 4 + j));
     * // Result: [['a', 'b', 'c', 'd'],
     * //          ['e', 'f', 'g', 'h'],
     * //          ['i', 'j', 'k', 'l']]
     * matrix.get(0, 0);           // returns 'a'
     * matrix.get(2, 3);           // returns 'l'
     *
     * CharMatrix diag = CharMatrix.of(new char[2][2]);
     * diag.updateAll((i, j) -> i == j ? 'X' : '.');
     * diag.rowView(0);            // returns ['X', '.']
     *
     * CharMatrix.empty().updateAll((i, j) -> 'z');                                    // no-op on empty matrix
     * matrix.updateAll((Throwables.IntBiFunction<Character, RuntimeException>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the function that receives row index and column index (0-based) and returns
     *             the new value for that position; the returned {@code Character} is unboxed, so it
     *             must not be {@code null}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws NullPointerException if {@code mapper} returns {@code null} for any position
     * @throws E if the mapper throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Character, E> mapper) throws IllegalArgumentException, E {
        N.checkArgNotNull(mapper, "mapper");
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = mapper.apply(i, j);
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this) && !hasAliasedRows());
    }

    /**
     * Conditionally replaces elements in-place based on a predicate.
     * All elements that satisfy the predicate are replaced with the specified new value.
     * This modifies the matrix directly.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance. If parallelized, the supplied function must be thread-safe.</p>
     *
     * <p>If multiple logical rows share the same backing array, the predicate is evaluated once
     * per physical cell and that backing row is updated once.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'e', 'c'}, {'b', 'f', 'd'}});
     * matrix.replaceIf(c -> c < 'd', 'x');
     * matrix.rowView(0);          // returns ['x', 'e', 'x']
     * matrix.rowView(1);          // returns ['x', 'f', 'd']
     *
     * CharMatrix none = CharMatrix.of(new char[][] {{'a', 'b'}});
     * none.replaceIf(c -> c > 'z', '?'); // matches nothing
     * none.rowView(0);                   // returns ['a', 'b'] (unchanged)
     *
     * CharMatrix.empty().replaceIf(c -> true, 'x');                             // no-op on empty matrix
     * matrix.replaceIf((Throwables.CharPredicate<RuntimeException>) null, 'x'); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to test each element; elements for which this returns
     *                  {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.CharPredicate<E> predicate, final char newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");

        if (Matrices.shouldRunInParallel(this)) {
            if (hasAliasedRows()) {
                final char[][] distinctRows = new char[rowCount][];
                final int[] distinctRowCount = { 0 };
                forEachDistinctRow(row -> distinctRows[distinctRowCount[0]++] = row);

                final Throwables.IntBiConsumer<E> elementAction = (i, j) -> {
                    if (predicate.test(distinctRows[i][j])) {
                        distinctRows[i][j] = newValue;
                    }
                };
                final long distinctElementCount = (long) distinctRowCount[0] * columnCount;
                Matrices.forEachIndices(distinctRowCount[0], columnCount, elementAction, Matrices.shouldRunInParallel(this, distinctElementCount));
            } else {
                final Throwables.IntBiConsumer<E> elementAction = (i, j) -> {
                    if (predicate.test(a[i][j])) {
                        a[i][j] = newValue;
                    }
                };
                Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
            }
        } else {
            forEachDistinctRow(row -> {
                for (int j = 0; j < columnCount; j++) {
                    if (predicate.test(row[j])) {
                        row[j] = newValue;
                    }
                }
            });
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
     * <p>Nonmatching positions perform no write. If logical rows share a backing array, a
     * replacement made through any matching coordinate is therefore visible through every alias.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'},
     *                                                 {'d', 'e', 'f'},
     *                                                 {'g', 'h', 'i'}});
     * matrix.replaceIf((i, j) -> i == j, 'X');   // replace main diagonal positions
     * matrix.rowView(0);                         // returns ['X', 'b', 'c']
     * matrix.rowView(2);                         // returns ['g', 'h', 'X']
     *
     * CharMatrix firstCol = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * firstCol.replaceIf((i, j) -> j == 0, '*'); // replace first column
     * firstCol.rowView(0);                       // returns ['*', 'b']
     *
     * CharMatrix.empty().replaceIf((i, j) -> true, 'x');                         // no-op on empty matrix
     * matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 'x'); // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition that tests row index and column index (0-based); elements
     *                  at positions for which this returns {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final char newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> {
            if (predicate.test(i, j)) {
                a[i][j] = newValue;
            }
        };
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));
    }

    /**
     * Creates a new CharMatrix by applying a transformation function to each element.
     * The original matrix is not modified; a new matrix with transformed values is returned.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance. If parallelized, the supplied function must be thread-safe.
     * This is the immutable counterpart to {@link #updateAll(Throwables.CharUnaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * CharMatrix upper = matrix.map(c -> Character.toUpperCase(c));
     * upper.rowView(0);           // returns ['A', 'B']
     * matrix.get(0, 0);           // returns 'a' (original unchanged)
     *
     * CharMatrix shift = matrix.map(c -> (char) (c + 1));
     * shift.rowView(0);           // returns ['b', 'c']
     *
     * CharMatrix.empty().map(c -> c).isEmpty(); // returns true
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to apply to each element; receives the current element value
     *             and returns the transformed value
     * @return a new CharMatrix with transformed values
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     * @see #updateAll(Throwables.CharUnaryOperator)
     */
    public <E extends Exception> CharMatrix map(final Throwables.CharUnaryOperator<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final char[][] result = new char[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsChar(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return CharMatrix.of(result);
    }

    /**
     * Creates a new Matrix by applying a function that converts char values to objects of type R.
     * This operation may be executed in parallel for better performance on large matrices. If parallelized, the supplied function must be thread-safe.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * Matrix<String> stringMatrix = matrix.mapToObj(c -> String.valueOf(c), String.class);
     * stringMatrix.get(0, 0);     // returns "a"
     * stringMatrix.get(1, 1);     // returns "d"
     *
     * Matrix<Integer> codeUnits = matrix.mapToObj(c -> (int) c, Integer.class);
     * codeUnits.get(0, 0);        // returns 97
     * codeUnits.get(1, 1);        // returns 100
     *
     * CharMatrix.empty().mapToObj(c -> "" + c, String.class).isEmpty(); // returns true
     * }</pre>
     *
     * @param <R> the element type of the resulting matrix
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to convert char values to type {@code R}
     * @param targetElementType the {@code Class} object for type {@code R} (used to allocate the
     *        {@code R[][]} backing array); must not be {@code null}
     * @return a new {@link Matrix Matrix&lt;R&gt;} containing the mapped values
     * @throws IllegalArgumentException if {@code mapper} or {@code targetElementType} is {@code null}
     * @throws E if the function throws an exception
     */
    public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.CharFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E {
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.fill('x');
     * matrix.rowView(0);          // returns ['x', 'x']
     * matrix.rowView(1);          // returns ['x', 'x']
     *
     * CharMatrix single = CharMatrix.of(new char[][] {{'a'}});
     * single.fill('z');
     * single.get(0, 0);           // returns 'z'
     *
     * CharMatrix.empty().fill('x'); // no-op on an empty matrix
     * }</pre>
     *
     * @param value the value to fill the matrix with
     */
    public void fill(final char value) {
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
     * CharMatrix matrix = CharMatrix.of(new char[3][3]);
     * matrix.fill(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * // Top-left 2x2 region is filled; the rest keeps the null character (char) 0:
     * //   [['a', 'b', (char) 0], ['c', 'd', (char) 0], [(char) 0, (char) 0, (char) 0]]
     * matrix.get(0, 0);           // returns 'a'
     * matrix.get(1, 1);           // returns 'd'
     *
     * // A source larger than the matrix is silently truncated to fit.
     * CharMatrix small = CharMatrix.of(new char[1][1]);
     * small.fill(new char[][] {{'x', 'y'}, {'z', 'w'}});
     * small.get(0, 0);            // returns 'x' (only the top-left cell that fits is copied)
     *
     * matrix.fill((char[][]) null); // throws IllegalArgumentException (null source)
     * }</pre>
     *
     * @param source the two-dimensional array to copy values from; must not be {@code null}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @see #fill(int, int, char[][])
     */
    public void fill(final char[][] source) {
        fill(0, 0, source);
    }

    /**
     * Fills a region of this matrix with values from another two-dimensional array, starting at the
     * specified destination position.
     * The source array can extend beyond this matrix's bounds; only the overlapping region is copied.
     * The matrix is modified in-place. {@code null} rows in {@code source} are skipped (the
     * corresponding destination row is left unchanged). Elements outside the matrix bounds are ignored.
     * Any source row that aliases this matrix's backing storage is snapshotted before copying, so
     * overlapping copies read the source values as they were when this method was called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[3][3]);
     * matrix.fill(1, 1, new char[][] {{'a', 'b'}, {'c', 'd'}});
     * // Result; cells outside the copied region keep the null character (char) 0:
     * //   [[(char) 0, (char) 0, (char) 0], [(char) 0, 'a', 'b'], [(char) 0, 'c', 'd']]
     * matrix.get(1, 1);           // returns 'a'
     * matrix.get(2, 2);           // returns 'd'
     *
     * // A source that overruns the bounds from the start position is clipped.
     * CharMatrix clip = CharMatrix.of(new char[2][2]);
     * clip.fill(1, 1, new char[][] {{'x', 'y'}, {'z', 'w'}});
     * clip.get(1, 1);             // returns 'x' (only the in-bounds top-left cell is copied)
     *
     * matrix.fill(0, 0, (char[][]) null); // throws IllegalArgumentException (null source)
     * }</pre>
     *
     * @param destRowIndex the target row index in this matrix (0-based, must satisfy {@code 0 <= destRowIndex <= rowCount})
     * @param destColumnIndex the target column index in this matrix (0-based, must satisfy {@code 0 <= destColumnIndex <= columnCount})
     * @param source the source array to copy values from; must not be {@code null}
     * @throws IndexOutOfBoundsException if {@code destRowIndex < 0} or {@code destRowIndex > rowCount},
     *         or if {@code destColumnIndex < 0} or {@code destColumnIndex > columnCount}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final char[][] source) throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkArgNotNull(source, "source");
        if (destRowIndex < 0 || destRowIndex > rowCount) {
            throw new IndexOutOfBoundsException(formatMsg("destRowIndex({}) must be in [0, rowCount({})]", destRowIndex, rowCount));
        }
        if (destColumnIndex < 0 || destColumnIndex > columnCount) {
            throw new IndexOutOfBoundsException(formatMsg("destColumnIndex({}) must be in [0, columnCount({})]", destColumnIndex, columnCount));
        }
        final char[][] sourceSnapshot = snapshotRowsIfBackingRows(source);

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
     * CharMatrix original = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * CharMatrix copy = original.copy();
     * copy.equals(original);      // returns true (same content)
     *
     * copy.set(0, 0, 'x');        // mutating the copy ...
     * original.get(0, 0);         // returns 'a' (... does not affect the original)
     *
     * CharMatrix.empty().copy().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix that is a copy of this matrix with full independence guarantee
     */
    @Override
    public CharMatrix copy() {
        final char[][] c = new char[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[i].clone();
        }

        return new CharMatrix(c);
    }

    /**
     * Creates a copy of a row range from this matrix.
     * The returned matrix contains only the specified rows and is completely independent from the original matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}, {'e', 'f'}});
     * CharMatrix subset = matrix.copy(1, 3);
     * subset.rowCount();          // returns 2
     * subset.rowView(0);          // returns ['c', 'd'] -> {{'c', 'd'}, {'e', 'f'}}
     *
     * matrix.copy(1, 1).rowCount(); // returns 0 (empty range)
     *
     * matrix.copy(-1, 2);           // throws IndexOutOfBoundsException (fromRowIndex < 0)
     * matrix.copy(0, 5);            // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new {@code CharMatrix} containing the specified rows
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public CharMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        final char[][] c = new char[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = a[i].clone();
        }

        return new CharMatrix(c);
    }

    /**
     * Creates a copy of a submatrix defined by row and column ranges.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}, {'g', 'h', 'i'}});
     * CharMatrix submatrix = matrix.copy(0, 2, 1, 3);
     * submatrix.get(0, 0);                    // returns 'b'
     * submatrix.get(1, 1);                    // returns 'f' -> {{'b', 'c'}, {'e', 'f'}}
     *
     * matrix.copy(0, 1, 0, 1).get(0, 0);     // returns 'a' (single-cell submatrix)
     *
     * matrix.copy(0, 2, 1, 5);               // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.copy(-1, 2, 0, 2);              // throws IndexOutOfBoundsException (fromRowIndex < 0)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new {@code CharMatrix} containing the specified submatrix
     * @throws IndexOutOfBoundsException if any range is invalid (e.g. {@code fromRowIndex < 0},
     *         {@code toRowIndex > rowCount}, {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code from > to} for either range)
     */
    @Override
    public CharMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        final char[][] c = new char[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = N.copyOfRange(a[i], fromColumnIndex, toColumnIndex);
        }

        return new CharMatrix(c);
    }

    /**
     * Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount},
     * anchored at the top-left corner of this matrix. New cells are filled with {@code (char) 0}.
     *
     * <ul>
     *   <li><b>If a dimension shrinks</b> — elements beyond the new boundary are discarded
     *       (excess rows removed from the bottom, excess columns removed from the right).</li>
     *   <li><b>If a dimension grows</b> — new cells are filled with {@code (char) 0}.</li>
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}, {'g', 'h', 'i'}});
     *
     * // Grow: both dimensions larger — new cells filled with (char) 0
     * CharMatrix grown = matrix.resize(4, 4);
     * // Result: [['a', 'b', 'c', (char) 0],
     * //          ['d', 'e', 'f', (char) 0],
     * //          ['g', 'h', 'i', (char) 0],
     * //          [(char) 0, (char) 0, (char) 0, (char) 0]]
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * CharMatrix truncated = matrix.resize(2, 2);
     * // Result: [['a', 'b'],
     * //          ['d', 'e']]
     *
     * // Mixed: grow rows, truncate columns
     * CharMatrix mixed = matrix.resize(4, 2);
     * // Result: [['a', 'b'],
     * //          ['d', 'e'],
     * //          ['g', 'h'],
     * //          [(char) 0, (char) 0]]
     *
     * matrix.resize(0, 0).isEmpty(); // returns true
     * matrix.resize(-1, 4);          // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new CharMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int, char)
     * @see #extend(int, int, int, int)
     */
    public CharMatrix resize(final int newRowCount, final int newColumnCount) {
        return resize(newRowCount, newColumnCount, CHAR_0);
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
     * <p>The original matrix is never modified; a new matrix is always returned.</p>
     *
     * <p><b>Comparison with {@link #extend(int, int, int, int, char)}:</b>
     * {@code resize} takes <em>absolute</em> target dimensions and may truncate existing content.
     * {@code extend} takes <em>relative</em> padding amounts per edge and <em>never truncates</em>.
     * Use {@code extend} when the entire original content must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}, {'g', 'h', 'i'}});
     *
     * // Grow: fill new cells with 'x'
     * CharMatrix grown = matrix.resize(4, 4, 'x');
     * // Result: [['a', 'b', 'c', 'x'],
     * //          ['d', 'e', 'f', 'x'],
     * //          ['g', 'h', 'i', 'x'],
     * //          ['x', 'x', 'x', 'x']]
     *
     * // Truncate: defaultValue is ignored when shrinking
     * CharMatrix truncated = matrix.resize(2, 2, 'x');
     * // Result: [['a', 'b'],
     * //          ['d', 'e']]
     *
     * matrix.resize(0, 0, 'x').isEmpty(); // returns true
     * matrix.resize(3, -1, 'x');          // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the value used to fill cells that are added when a dimension grows;
     *        ignored when neither dimension grows
     * @return a new CharMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int)
     * @see #extend(int, int, int, int, char)
     */
    public CharMatrix resize(final int newRowCount, final int newColumnCount, final char defaultValue) throws IllegalArgumentException {
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
            final boolean fillDefaultValue = defaultValue != CHAR_0;
            final char[][] b = new char[newRowCount][];

            for (int i = 0; i < newRowCount; i++) {
                b[i] = i < rowCount ? N.copyOf(a[i], newColumnCount) : new char[newColumnCount];

                if (fillDefaultValue) {
                    if (i >= rowCount) {
                        N.fill(b[i], defaultValue);
                    } else if (columnCount < newColumnCount) {
                        N.fill(b[i], columnCount, newColumnCount, defaultValue);
                    }
                }
            }

            return new CharMatrix(b);
        }
    }

    /**
     * Returns a new matrix formed by surrounding this matrix with padding on all four edges.
     * New cells are filled with {@code (char) 0}.
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     *
     * // Uniform 1-cell border of (char) 0
     * CharMatrix bordered = matrix.extend(1, 1, 1, 1);
     * // Result: [[(char) 0, (char) 0, (char) 0, (char) 0],
     * //          [(char) 0, 'a',      'b',      (char) 0],
     * //          [(char) 0, 'c',      'd',      (char) 0],
     * //          [(char) 0, (char) 0, (char) 0, (char) 0]]
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * CharMatrix shifted = matrix.extend(0, 0, 2, 0);
     * // Result: [[(char) 0, (char) 0, 'a', 'b'],
     * //          [(char) 0, (char) 0, 'c', 'd']]
     *
     * matrix.extend(0, 0, 0, 0).equals(matrix); // returns true (no padding -> equal copy)
     * matrix.extend(-1, 0, 0, 0);               // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @return a new CharMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         if the resulting dimensions would overflow {@code Integer.MAX_VALUE},
     *         or if the resulting shape is not representable (zero rows with a non-zero column count)
     * @see #extend(int, int, int, int, char)
     * @see #resize(int, int)
     */
    @Override
    public CharMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight) {
        return extend(padTop, padBottom, padLeft, padRight, CHAR_0);
    }

    /**
     * Returns a new matrix formed by surrounding this matrix with padding on all four edges.
     * New cells are filled with {@code defaultValue}.
     *
     * <p>Unlike {@link #resize(int, int, char)}, this method <b>never truncates</b>: the entire
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     *
     * // Uniform 1-cell border filled with 'x'
     * CharMatrix bordered = matrix.extend(1, 1, 1, 1, 'x');
     * // Result: [['x', 'x', 'x', 'x'],
     * //          ['x', 'a', 'b', 'x'],
     * //          ['x', 'c', 'd', 'x'],
     * //          ['x', 'x', 'x', 'x']]
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * CharMatrix shifted = matrix.extend(0, 0, 2, 0, ' ');
     * // Result: [[' ', ' ', 'a', 'b'],
     * //          [' ', ' ', 'c', 'd']]
     *
     * matrix.extend(0, 0, 0, 0, 'x').equals(matrix); // returns true (no padding -> equal copy)
     * matrix.extend(0, -1, 0, 0, 'x');               // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @param defaultValue the value to fill all new padding cells with
     * @return a new CharMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         if the resulting dimensions would overflow {@code Integer.MAX_VALUE},
     *         or if the resulting shape is not representable (zero rows with a non-zero column count)
     * @see #extend(int, int, int, int)
     * @see #resize(int, int, char)
     */
    public CharMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final char defaultValue)
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
            final boolean fillDefaultValue = defaultValue != CHAR_0;
            final char[][] b = new char[newRowCount][newColumnCount];

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

            return new CharMatrix(b);
        }
    }

    /**
     * Reverses the order of elements in each row horizontally (in-place).
     * This modifies the matrix directly. Each row is reversed independently.
     * If multiple logical rows share the same backing array, that backing row is reversed only
     * once, preserving the alias relationship.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.flipHorizontallyInPlace();
     * matrix.rowView(0);          // returns ['c', 'b', 'a']
     * matrix.rowView(1);          // returns ['f', 'e', 'd']
     *
     * CharMatrix single = CharMatrix.of(new char[][] {{'x'}});
     * single.flipHorizontallyInPlace();
     * single.get(0, 0);           // returns 'x' (single column unchanged)
     *
     * CharMatrix.empty().flipHorizontallyInPlace(); // no-op on an empty matrix
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

        forEachDistinctRow(N::reverse);
    }

    /**
     * Reverses the order of rows in the matrix (vertical flip in-place).
     * This modifies the matrix directly. The first row becomes the last, second becomes second-to-last, etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}, {'e', 'f'}});
     * matrix.flipVerticallyInPlace();
     * matrix.rowView(0);          // returns ['e', 'f']
     * matrix.rowView(2);          // returns ['a', 'b']
     *
     * CharMatrix single = CharMatrix.of(new char[][] {{'x', 'y'}});
     * single.flipVerticallyInPlace();
     * single.rowView(0);          // returns ['x', 'y'] (single row unchanged)
     *
     * CharMatrix.empty().flipVerticallyInPlace(); // no-op on an empty matrix
     * }</pre>
     *
     * @see #flipVertically()
     * @see #flipHorizontallyInPlace()
     */
    @Override
    public void flipVerticallyInPlace() {
        for (int l = 0, h = rowCount - 1; l < h; l++, h--) {
            final char[] tmp = a[l];
            a[l] = a[h];
            a[h] = tmp;
        }
    }

    /**
     * Creates a new matrix that is horizontally flipped (each row reversed).
     * The original matrix is not modified. This is equivalent to reversing each row.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}});
     * CharMatrix flipped = matrix.flipHorizontally();
     * flipped.rowView(0);         // returns ['c', 'b', 'a']
     * matrix.rowView(0);          // returns ['a', 'b', 'c'] (original unchanged)
     *
     * CharMatrix multi = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * multi.flipHorizontally().rowView(1); // returns ['d', 'c']
     *
     * CharMatrix.empty().flipHorizontally().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new CharMatrix with each row reversed
     * @see #flipHorizontallyInPlace()
     * @see #flipVertically()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public CharMatrix flipHorizontally() {
        final CharMatrix res = this.copy();
        res.flipHorizontallyInPlace();
        return res;
    }

    /**
     * Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
     * The original matrix is not modified. The first row becomes the last row, etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}, {'e', 'f'}});
     * CharMatrix flipped = matrix.flipVertically();
     * flipped.rowView(0);         // returns ['e', 'f']
     * flipped.rowView(2);         // returns ['a', 'b']
     * matrix.rowView(0);          // returns ['a', 'b'] (original unchanged)
     *
     * CharMatrix.empty().flipVertically().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix that is a vertical flip of this matrix (rows in reversed order)
     * @see #flipVerticallyInPlace()
     * @see #flipHorizontally()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public CharMatrix flipVertically() {
        final CharMatrix res = this.copy();
        res.flipVerticallyInPlace();
        return res;
    }

    /**
     * Returns a new matrix that is this matrix rotated 90 degrees clockwise.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * CharMatrix rotated = matrix.rotate90();
     * rotated.rowView(0);         // returns ['c', 'a']
     * rotated.rowView(1);         // returns ['d', 'b']
     *
     * // Non-square: a 2x3 matrix rotates to 3x2.
     * CharMatrix wide = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * wide.rotate90().rowView(0); // returns ['d', 'a']
     *
     * CharMatrix.empty().rotate90().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix rotated 90 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         or an empty matrix if this matrix has zero columns
     * @see #rotate180()
     * @see #rotate270()
     * @see #transpose()
     */
    @Override
    public CharMatrix rotate90() {
        if (columnCount == 0) {
            return EMPTY_CHAR_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final char[][] c = new char[columnCount][rowCount];

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

        return new CharMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 180 degrees.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * CharMatrix rotated = matrix.rotate180();
     * rotated.rowView(0);         // returns ['d', 'c']
     * rotated.rowView(1);         // returns ['b', 'a']
     *
     * CharMatrix wide = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * wide.rotate180().rowView(0); // returns ['f', 'e', 'd']
     *
     * CharMatrix.empty().rotate180().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 180 degrees
     * @see #rotate90()
     * @see #rotate270()
     */
    @Override
    public CharMatrix rotate180() {
        final char[][] c = new char[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[rowCount - i - 1].clone();
            N.reverse(c[i]);
        }

        return new CharMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 270 degrees clockwise.
     * This is equivalent to rotating 90 degrees counter-clockwise.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * CharMatrix rotated = matrix.rotate270();
     * rotated.rowView(0);         // returns ['b', 'd']
     * rotated.rowView(1);         // returns ['a', 'c']
     *
     * CharMatrix wide = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * wide.rotate270().rowView(0); // returns ['c', 'f']
     *
     * CharMatrix.empty().rotate270().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix rotated 270 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         or an empty matrix if this matrix has zero columns
     * @see #rotate90()
     * @see #rotate180()
     * @see #transpose()
     */
    @Override
    public CharMatrix rotate270() {
        if (columnCount == 0) {
            return EMPTY_CHAR_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final char[][] c = new char[columnCount][rowCount];

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

        return new CharMatrix(c);
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
     * // a b c      a d
     * // d e f      b e
     * //            c f
     *
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * CharMatrix transposed = matrix.transpose();   // 2x3 becomes 3x2
     * transposed.rowCount();                        // returns 3
     * transposed.rowView(0);                        // returns ['a', 'd']
     * transposed.rowView(2);                        // returns ['c', 'f']
     *
     * // An N x 0 matrix transposes to the empty 0 x 0 matrix.
     * CharMatrix.empty().transpose().isEmpty();            // returns true
     * CharMatrix.of(new char[2][0]).transpose().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix that is the transpose of this matrix with dimensions columnCount × rowCount;
     *         an {@code N x 0} matrix transposes to the empty {@code 0 x 0} matrix, because the swapped shape
     *         {@code 0 x N} (zero rows with a non-zero column count) is not representable
     */
    @Override
    public CharMatrix transpose() {
        if (columnCount == 0) {
            return EMPTY_CHAR_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final char[][] c = new char[columnCount][rowCount];

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

        return new CharMatrix(c);
    }

    /**
     * Reshapes this matrix to have the specified dimensions.
     * Elements are taken in row-major order from this matrix and placed into the new shape.
     * The new shape must have at least as many total cells as the original
     * ({@code (long) newRowCount * newColumnCount >= elementCount()}).
     * Any extra trailing cells in the new shape are filled with {@code (char) 0}.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * CharMatrix reshaped = matrix.reshape(3, 2);
     * reshaped.rowView(0);                    // returns ['a', 'b'] -> {{'a', 'b'}, {'c', 'd'}, {'e', 'f'}}
     * reshaped.get(2, 1);                     // returns 'f'
     *
     * CharMatrix extended = matrix.reshape(2, 4);
     * extended.get(1, 2);                     // returns (char) 0 (extra trailing cell) -> [['a','b','c','d'],['e','f',(char) 0,(char) 0]]
     *
     * matrix.reshape(0, 0);                   // throws IllegalArgumentException (too small for 6 elements)
     * matrix.reshape(-1, 6);                  // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix; must be {@code >= 0}
     * @param newColumnCount the number of columns in the reshaped matrix; must be {@code >= 0}
     * @return a new {@code CharMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative, if the resulting shape is not
     *         representable (zero rows with a non-zero column count), if the total cell count {@code (long) newRowCount * newColumnCount}
     *         exceeds {@code Integer.MAX_VALUE}, or if the new shape is too small to hold every existing element
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public CharMatrix reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        checkMaterializableShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final char[][] c = new char[newRowCount][newColumnCount];

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return new CharMatrix(c);
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

        return new CharMatrix(c);
    }

    /**
     * Repeats each element in both row and column directions.
     * Each element is repeated rowRepeats times vertically and columnRepeats times horizontally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}});
     * CharMatrix repeated = matrix.repeatElements(2, 3);
     * repeated.rowView(0);        // returns ['a', 'a', 'a', 'b', 'b', 'b']
     * repeated.rowView(1);        // returns ['a', 'a', 'a', 'b', 'b', 'b']
     *
     * matrix.repeatElements(1, 1).rowView(0); // returns ['a', 'b'] (1x1 repeat is a copy)
     * matrix.repeatElements(0, 1);            // throws IllegalArgumentException (repeats must be positive)
     * }</pre>
     *
     * @param rowRepeats the number of times to repeat each element vertically (down the row axis);
     *        must be {@code > 0}
     * @param columnRepeats the number of times to repeat each element horizontally (across the column axis);
     *        must be {@code > 0}
     * @return a new {@code CharMatrix} with repeated elements and dimensions
     *         {@code (rowCount * rowRepeats) x (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or the resulting dimensions would exceed {@link Integer#MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repelem.html">MATLAB repelem function</a>
     */
    @Override
    public CharMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        if (rowRepeats == 1 && columnRepeats == 1) {
            return copy();
        }

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final char[][] c = new char[rowCount * rowRepeats][columnCount * columnRepeats];

        for (int i = 0; i < rowCount; i++) {
            final char[] aa = a[i];
            final char[] fr = c[i * rowRepeats];

            for (int j = 0; j < columnCount; j++) {
                N.fill(fr, j * columnRepeats, (j + 1) * columnRepeats, aa[j]);
            }

            for (int k = 1; k < rowRepeats; k++) {
                N.copy(fr, 0, c[i * rowRepeats + k], 0, fr.length);
            }
        }

        return new CharMatrix(c);
    }

    /**
     * Repeats the entire matrix in both row and column directions.
     * The matrix is tiled rowRepeats times vertically and columnRepeats times horizontally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * CharMatrix repeated = matrix.repeatMatrix(2, 3);
     * repeated.rowView(0);        // returns ['a', 'b', 'a', 'b', 'a', 'b']
     * repeated.rowView(1);        // returns ['c', 'd', 'c', 'd', 'c', 'd']
     * repeated.rowCount();        // returns 4
     *
     * matrix.repeatMatrix(1, 1).equals(matrix); // returns true (1x1 tiling is a copy)
     * matrix.repeatMatrix(0, 1);                // throws IllegalArgumentException (repeats must be positive)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat the matrix vertically; must be {@code > 0}
     * @param columnRepeats number of times to repeat the matrix horizontally; must be {@code > 0}
     * @return a new CharMatrix with the repeated pattern
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or the resulting dimensions would exceed {@link Integer#MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repmat.html">MATLAB repmat function</a>
     */
    @Override
    public CharMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        if (rowRepeats == 1 && columnRepeats == 1) {
            return copy();
        }

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final char[][] c = new char[rowCount * rowRepeats][columnCount * columnRepeats];

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

        return new CharMatrix(c);
    }

    /**
     * Returns a new {@link CharList} containing all elements of this matrix in row-major order.
     * The returned list owns its data; modifications to it do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}, {'e', 'f'}});
     * matrix.flatten().toArray();   // returns ['a', 'b', 'c', 'd', 'e', 'f']
     *
     * CharMatrix single = CharMatrix.of(new char[][] {{'x', 'y', 'z'}});
     * single.flatten().toArray();   // returns ['x', 'y', 'z']
     *
     * CharMatrix.empty().flatten().size(); // returns 0
     * }</pre>
     *
     * @return a new {@link CharList} of all elements in row-major order
     * @throws IllegalStateException if {@code (long) rowCount * columnCount > Integer.MAX_VALUE}
     * @see #rowMajorStream()
     */
    @Override
    public CharList flatten() {
        // Check for overflow before allocation
        if ((long) rowCount * columnCount > Integer.MAX_VALUE) {
            throw new IllegalStateException("Matrix too large to flatten: " + rowCount + " x " + columnCount);
        }

        final char[] c = new char[rowCount * columnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c, i * columnCount, columnCount);
        }

        return CharList.of(c);
    }

    /**
     * Exposes the elements of this matrix to {@code action} as a temporary one-dimensional array
     * laid out in row-major order, then propagates any modifications back into the matrix if the
     * action completes normally.
     *
     * <p>This enables operations that need to process all matrix elements together (e.g., sorting all
     * elements across the entire matrix). The shape of this matrix is preserved; only element
     * values change. If the action throws, none of its changes to the temporary array are copied
     * back. If logical rows share a backing array, each logical row still occupies its own segment
     * in the temporary array; copy-back proceeds in row-major order, so a later logical row wins
     * where aliases target the same backing cells.</p>
     *
     * <p>For a matrix with no rows, the action is not invoked. For a matrix with one or more
     * zero-length rows, the action is invoked once with an empty array. See
     * {@link Arrays#mutateFlattened(char[][], Throwables.Consumer)} for the exact semantics of the
     * underlying operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'d', 'b'}, {'c', 'a'}});
     * matrix.mutateFlattened(arr -> java.util.Arrays.sort(arr));
     * matrix.rowView(0);          // returns ['a', 'b']
     * matrix.rowView(1);          // returns ['c', 'd']
     *
     * CharMatrix m = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * m.mutateFlattened(arr -> { for (int i = 0; i < arr.length; i++) arr[i] = Character.toUpperCase(arr[i]); });
     * m.rowView(0);               // returns ['A', 'B']
     *
     * CharMatrix.empty().mutateFlattened(arr -> java.util.Arrays.sort(arr)); // no-op on an empty matrix
     * }</pre>
     *
     * @param <E> the exception type that the operation may throw
     * @param action the operation to apply to the temporary flattened array
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws ArithmeticException if the number of matrix elements exceeds {@link Integer#MAX_VALUE}
     * @throws E if the operation throws an exception
     * @see Arrays#mutateFlattened(char[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateFlattened(final Throwables.Consumer<? super char[], E> action) throws E {
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
     * CharMatrix a = CharMatrix.of(new char[][] {{'a', 'b'}});
     * CharMatrix b = CharMatrix.of(new char[][] {{'c', 'd'}});
     * CharMatrix stacked = a.stackVertically(b);
     * stacked.rowView(0);         // returns ['a', 'b']
     * stacked.rowView(1);         // returns ['c', 'd']
     *
     * CharMatrix wrong = CharMatrix.of(new char[][] {{'x'}});
     * a.stackVertically(wrong);             // throws IllegalArgumentException (column count mismatch)
     * a.stackVertically((CharMatrix) null); // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * @param other the matrix to stack below this matrix (must have the same column count)
     * @return a new CharMatrix with dimensions (this.rowCount + other.rowCount) x this.columnCount
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.columnCount != other.columnCount},
     *         or if the merged row count would exceed {@code Integer.MAX_VALUE}
     * @see #stackHorizontally(CharMatrix)
     */
    @Override
    public CharMatrix stackVertically(final CharMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.columnCount, MSG_VSTACK_COLUMN_MISMATCH, columnCount, other.columnCount);
        final long mergedRowCount = (long) rowCount + other.rowCount;
        N.checkArgument(mergedRowCount <= Integer.MAX_VALUE, "Merged row count overflow: {} + {} = {}", rowCount, other.rowCount, mergedRowCount);

        final char[][] c = new char[(int) mergedRowCount][];
        int j = 0;

        for (int i = 0; i < rowCount; i++) {
            c[j++] = a[i].clone();
        }

        for (int i = 0; i < other.rowCount; i++) {
            c[j++] = other.a[i].clone();
        }

        return CharMatrix.of(c);
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
     * CharMatrix a = CharMatrix.of(new char[][] {{'a'}, {'b'}});
     * CharMatrix b = CharMatrix.of(new char[][] {{'c'}, {'d'}});
     * CharMatrix stacked = a.stackHorizontally(b);
     * stacked.rowView(0);         // returns ['a', 'c']
     * stacked.rowView(1);         // returns ['b', 'd']
     *
     * CharMatrix wrong = CharMatrix.of(new char[][] {{'x'}});
     * a.stackHorizontally(wrong);             // throws IllegalArgumentException (row count mismatch)
     * a.stackHorizontally((CharMatrix) null); // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * @param other the matrix to stack to the right of this matrix (must have the same row count)
     * @return a new CharMatrix with dimensions this.rowCount x (this.columnCount + other.columnCount)
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.rowCount != other.rowCount},
     *         or if the merged column count would exceed {@code Integer.MAX_VALUE}
     * @see #stackVertically(CharMatrix)
     */
    @Override
    public CharMatrix stackHorizontally(final CharMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(rowCount == other.rowCount, MSG_HSTACK_ROW_MISMATCH, rowCount, other.rowCount);
        final long mergedColumnCount = (long) columnCount + other.columnCount;
        N.checkArgument(mergedColumnCount <= Integer.MAX_VALUE, "Merged column count overflow: {} + {} = {}", columnCount, other.columnCount,
                mergedColumnCount);

        final char[][] c = new char[rowCount][(int) mergedColumnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c[i], 0, columnCount);
            N.copy(other.a[i], 0, c[i], columnCount, other.columnCount);
        }

        return CharMatrix.of(c);
    }

    /**
     * Performs element-wise addition with another matrix.
     * The matrices must have the same dimensions. The original matrices are not modified.
     *
     * <p><b>&#9888;&#65039; Numeric semantics:</b> this operates on the underlying UTF-16 code-unit values of each
     * {@code char}, not on text (e.g. {@code 'a' + 1} yields {@code 'b'}). If you want explicit integer
     * arithmetic, convert with {@link #toIntMatrix()} first.</p>
     *
     * <p><b>Overflow:</b> each result element is computed as {@code (char) (this[i][j] + other[i][j])},
     * so values wrap modulo {@code 65536} on overflow. If you need a non-wrapping result, call
     * {@link #toIntMatrix()} first and add there.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix a = CharMatrix.of(new char[][] {{'a', 'b'}});
     * CharMatrix b = CharMatrix.of(new char[][] {{1, 2}});
     * a.add(b).rowView(0);        // returns ['b', 'd'] ('a'+1, 'b'+2)
     *
     * // Wraparound: 65535 + 1 wraps modulo 65536 back to 0.
     * CharMatrix max = CharMatrix.of(new char[][] {{(char) 65535}});
     * CharMatrix one = CharMatrix.of(new char[][] {{1}});
     * int wrappedValue = max.add(one).get(0, 0); // 0 (wrapped)
     *
     * CharMatrix wrong = CharMatrix.of(new char[][] {{'x', 'y', 'z'}});
     * a.add(wrong);               // throws IllegalArgumentException (shape mismatch)
     * a.add((CharMatrix) null);   // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * @param other the matrix to add to this matrix; must not be {@code null} and must have the same shape
     * @return a new {@code CharMatrix} containing the element-wise sum
     * @throws IllegalArgumentException if {@code other} is {@code null}, or if the matrices have different shapes
     * @see #subtract(CharMatrix)
     * @see #zipWith(CharMatrix, Throwables.CharBinaryOperator)
     */
    public CharMatrix add(final CharMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(isSameShape(other), "Cannot add matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final char[][] otherArray = other.a;
        final char[][] result = new char[rowCount][columnCount];

        if (Matrices.shouldRunInParallel(this)) {
            final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = (char) (a[i][j] + otherArray[i][j]);
            Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
        } else {
            for (int i = 0; i < rowCount; i++) {
                final char[] row = a[i];
                final char[] otherRow = otherArray[i];
                final char[] resultRow = result[i];

                for (int j = 0; j < columnCount; j++) {
                    resultRow[j] = (char) (row[j] + otherRow[j]);
                }
            }
        }

        return CharMatrix.of(result);
    }

    /**
     * Performs element-wise subtraction ({@code this - other}).
     * The matrices must have the same dimensions. The original matrices are not modified.
     *
     * <p><b>&#9888;&#65039; Numeric semantics:</b> this operates on the underlying UTF-16 code-unit values of each
     * {@code char}, not on text (e.g. {@code 'd' - 1} yields {@code 'c'}). If you want explicit integer
     * arithmetic, convert with {@link #toIntMatrix()} first.</p>
     *
     * <p><b>Overflow:</b> each result element is computed as {@code (char) (this[i][j] - other[i][j])},
     * so values wrap modulo {@code 65536} on underflow (a negative {@code int} difference becomes a
     * large {@code char}). If you need a non-wrapping result, call {@link #toIntMatrix()} first and
     * subtract there.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix a = CharMatrix.of(new char[][] {{'d', 'e'}});
     * CharMatrix b = CharMatrix.of(new char[][] {{1, 2}});
     * a.subtract(b).rowView(0);   // returns ['c', 'c'] ('d'-1, 'e'-2)
     *
     * // Underflow wraps modulo 65536: 0 - 1 becomes 65535.
     * CharMatrix zero = CharMatrix.of(new char[][] {{0}});
     * CharMatrix one = CharMatrix.of(new char[][] {{1}});
     * int wrappedValue = zero.subtract(one).get(0, 0); // 65535 (wrapped)
     *
     * CharMatrix wrong = CharMatrix.of(new char[][] {{'x', 'y', 'z'}});
     * a.subtract(wrong);               // throws IllegalArgumentException (shape mismatch)
     * a.subtract((CharMatrix) null);   // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * @param other the matrix to subtract from this matrix; must not be {@code null} and must have the same shape
     * @return a new {@code CharMatrix} containing the element-wise difference {@code this - other}
     * @throws IllegalArgumentException if {@code other} is {@code null}, or if the matrices have different shapes
     * @see #add(CharMatrix)
     * @see #zipWith(CharMatrix, Throwables.CharBinaryOperator)
     */
    public CharMatrix subtract(final CharMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(isSameShape(other), "Cannot subtract matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final char[][] otherArray = other.a;
        final char[][] result = new char[rowCount][columnCount];

        if (Matrices.shouldRunInParallel(this)) {
            final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = (char) (a[i][j] - otherArray[i][j]);
            Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
        } else {
            for (int i = 0; i < rowCount; i++) {
                final char[] row = a[i];
                final char[] otherRow = otherArray[i];
                final char[] resultRow = result[i];

                for (int j = 0; j < columnCount; j++) {
                    resultRow[j] = (char) (row[j] - otherRow[j]);
                }
            }
        }

        return CharMatrix.of(result);
    }

    /**
     * Performs matrix multiplication (Cayley product) with another matrix.
     * The number of columns in this matrix must equal the number of rows in {@code other}.
     * Result has shape {@code this.rowCount x other.columnCount}. The original matrices are not modified.
     *
     * <p><b>&#9888;&#65039; Numeric semantics:</b> this operates on the underlying UTF-16 code-unit values of each
     * {@code char}, not on text. If you want explicit integer arithmetic, convert with
     * {@link #toIntMatrix()} first.</p>
     *
     * <p><b>Note:</b> This is the linear-algebra matrix product, not element-wise multiplication.
     * For element-wise multiplication use {@link #zipWith(CharMatrix, Throwables.CharBinaryOperator)}.</p>
     *
     * <p><b>Overflow:</b> each accumulation step is performed with {@code char +=}, which promotes to
     * {@code int} for the multiply-add and then casts the running sum back to {@code char}, so any
     * intermediate or final value outside {@code [0, 65535]} wraps modulo {@code 65536}. For inputs
     * that may overflow, convert to int first via {@link #toIntMatrix()} and multiply there.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix a = CharMatrix.of(new char[][] {{2, 3}, {4, 5}});
     * CharMatrix b = CharMatrix.of(new char[][] {{1, 2}, {3, 4}});
     * CharMatrix product = a.matrixMultiply(b); // standard matrix multiplication
     * int topLeft = product.get(0, 0);          // 11 (2*1 + 3*3)
     * int topRight = product.get(0, 1);         // 16 (2*2 + 3*4)
     * int bottomRight = product.get(1, 1);      // 28 (4*2 + 5*4)
     *
     * CharMatrix wrong = CharMatrix.of(new char[][] {{1, 2, 3}}); // 1x3, this.columnCount is 2
     * a.matrixMultiply(wrong);                                    // throws IllegalArgumentException (this.columnCount != other.rowCount)
     * a.matrixMultiply((CharMatrix) null);                        // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * @param other the matrix to multiply with; must not be {@code null}
     * @return a new {@code CharMatrix} of shape {@code this.rowCount x other.columnCount} containing the matrix product
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.columnCount != other.rowCount}, or if this matrix has zero rows while {@code other} has a non-zero column count (the resulting shape is not representable)
     */
    public CharMatrix matrixMultiply(final CharMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.rowCount,
                "Matrix dimensions incompatible for multiplication: this is {}x{}, other is {}x{} (this.columnCount must equal other.rowCount)", rowCount,
                columnCount, other.rowCount, other.columnCount);

        checkRepresentableShape(rowCount, other.columnCount);

        final char[][] otherArray = other.a;
        final int newColumnCount = other.columnCount;
        final char[][] result = new char[rowCount][newColumnCount];

        if (Matrices.shouldRunMatrixMultiplyInParallel(this, newColumnCount)) {
            final Throwables.IntTriConsumer<RuntimeException> multiplyAction = (i, j, k) -> result[i][j] += a[i][k] * otherArray[k][j];
            Matrices.forEachCartesianIndices(this, other, multiplyAction, true);
        } else {
            // i-k-j loop order with hoisted rows: accumulates each result cell in ascending k order,
            // matching the accumulation order of Matrices.forEachCartesianIndices exactly.
            for (int i = 0; i < rowCount; i++) {
                final char[] row = a[i];
                final char[] resultRow = result[i];

                for (int k = 0; k < columnCount; k++) {
                    final char aik = row[k];
                    final char[] otherRow = otherArray[k];

                    for (int j = 0; j < newColumnCount; j++) {
                        resultRow[j] += aik * otherRow[j];
                    }
                }
            }
        }

        return CharMatrix.of(result);
    }

    /**
     * Converts this primitive char matrix to a boxed {@link Matrix Matrix&lt;Character&gt;}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix primitive = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * Matrix<Character> boxed = primitive.boxed();
     * boxed.get(0, 0);            // returns Character 'a'
     * boxed.get(1, 1);            // returns Character 'd'
     *
     * CharMatrix.unbox(primitive.boxed()).equals(primitive); // returns true (round-trip)
     * CharMatrix.empty().boxed().isEmpty();                  // returns true
     * }</pre>
     *
     * @return a new {@link Matrix Matrix&lt;Character&gt;} containing the same values as boxed {@code Character} instances
     * @see #unbox(Matrix)
     */
    public Matrix<Character> boxed() {
        final Character[][] c = new Character[rowCount][columnCount];

        for (int i = 0; i < rowCount; i++) {
            final char[] aa = a[i];
            final Character[] cc = c[i];

            for (int j = 0; j < columnCount; j++) {
                cc[j] = aa[j]; // NOSONAR
            }
        }

        return new Matrix<>(c);
    }

    /**
     * Converts this CharMatrix to an IntMatrix.
     * Each char value is widened to its numeric int value (the unsigned 16-bit code unit, in the range 0..65535).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix charMatrix = CharMatrix.of(new char[][] {{'a', 'b'}});
     * IntMatrix intMatrix = charMatrix.toIntMatrix();
     * intMatrix.get(0, 0);        // returns 97
     * intMatrix.get(0, 1);        // returns 98
     *
     * CharMatrix.of(new char[][] {{'0'}}).toIntMatrix().get(0, 0); // returns 48 (code unit of '0')
     * CharMatrix.empty().toIntMatrix().isEmpty();                  // returns true
     * }</pre>
     *
     * @return a new IntMatrix with the same dimensions containing the int values of the characters
     * @see IntMatrix#from(char[][])
     */
    public IntMatrix toIntMatrix() {
        return IntMatrix.from(a);
    }

    /**
     * Converts this CharMatrix to a LongMatrix.
     * Each char value is widened to its numeric long value (the unsigned 16-bit code unit, in the range 0..65535).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix charMatrix = CharMatrix.of(new char[][] {{'a', 'b'}});
     * LongMatrix longMatrix = charMatrix.toLongMatrix();
     * longMatrix.get(0, 0);       // returns 97L
     * longMatrix.get(0, 1);       // returns 98L
     *
     * CharMatrix.of(new char[][] {{'0'}}).toLongMatrix().get(0, 0); // returns 48L
     * CharMatrix.empty().toLongMatrix().isEmpty();                  // returns true
     * }</pre>
     *
     * @return a new LongMatrix with the same dimensions containing the long values of the characters
     */
    public LongMatrix toLongMatrix() {
        final long[][] c = new long[rowCount][columnCount];

        for (int i = 0; i < rowCount; i++) {
            final char[] aa = a[i];
            final long[] cc = c[i];

            for (int j = 0; j < columnCount; j++) {
                cc[j] = aa[j]; // NOSONAR
            }
        }

        return new LongMatrix(c);
    }

    /**
     * Converts this CharMatrix to a FloatMatrix.
     * Each char value is widened to its numeric float value (the unsigned 16-bit code unit, in the range 0..65535).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix charMatrix = CharMatrix.of(new char[][] {{'a', 'b'}});
     * FloatMatrix floatMatrix = charMatrix.toFloatMatrix();
     * floatMatrix.get(0, 0);      // returns 97.0f
     * floatMatrix.get(0, 1);      // returns 98.0f
     *
     * CharMatrix.of(new char[][] {{'0'}}).toFloatMatrix().get(0, 0); // returns 48.0f
     * CharMatrix.empty().toFloatMatrix().isEmpty();                  // returns true
     * }</pre>
     *
     * @return a new FloatMatrix with the same dimensions containing the float values of the characters
     */
    public FloatMatrix toFloatMatrix() {
        final float[][] c = new float[rowCount][columnCount];

        for (int i = 0; i < rowCount; i++) {
            final char[] aa = a[i];
            final float[] cc = c[i];

            for (int j = 0; j < columnCount; j++) {
                cc[j] = aa[j]; // NOSONAR
            }
        }

        return new FloatMatrix(c);
    }

    /**
     * Converts this CharMatrix to a DoubleMatrix.
     * Each char value is widened to its numeric double value (the unsigned 16-bit code unit, in the range 0..65535).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix charMatrix = CharMatrix.of(new char[][] {{'a', 'b'}});
     * DoubleMatrix doubleMatrix = charMatrix.toDoubleMatrix();
     * doubleMatrix.get(0, 0);     // returns 97.0
     * doubleMatrix.get(0, 1);     // returns 98.0
     *
     * CharMatrix.of(new char[][] {{'0'}}).toDoubleMatrix().get(0, 0); // returns 48.0
     * CharMatrix.empty().toDoubleMatrix().isEmpty();                  // returns true
     * }</pre>
     *
     * @return a new DoubleMatrix with the same dimensions containing the double values of the characters
     */
    public DoubleMatrix toDoubleMatrix() {
        final double[][] c = new double[rowCount][columnCount];

        for (int i = 0; i < rowCount; i++) {
            final char[] aa = a[i];
            final double[] cc = c[i];

            for (int j = 0; j < columnCount; j++) {
                cc[j] = aa[j]; // NOSONAR
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
     * subtraction, consider using the dedicated methods {@link #add(CharMatrix)} and {@link #subtract(CharMatrix)};
     * for the linear-algebra matrix product (which is not an element-wise operation), use {@link #matrixMultiply(CharMatrix)}.</p>
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance. If parallelized, the supplied function must be thread-safe.
     * Creates a new matrix; the original matrices are not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix a = CharMatrix.of(new char[][] {{'a', 'b'}});
     * CharMatrix b = CharMatrix.of(new char[][] {{'A', 'B'}});
     * a.zipWith(b, (x, y) -> (char) Math.max(x, y)).rowView(0); // returns ['a', 'b'] (lowercase code units are larger)
     * a.zipWith(b, (x, y) -> (char) Math.min(x, y)).rowView(0); // returns ['A', 'B'] (min of each pair)
     *
     * CharMatrix wrong = CharMatrix.of(new char[][] {{'x', 'y', 'z'}});
     * a.zipWith(wrong, (x, y) -> x);                                        // throws IllegalArgumentException (different shapes)
     * a.zipWith(b, (Throwables.CharBinaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null function)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param zipFunction the binary operator to apply to corresponding elements; receives the
     *                    element from this matrix as first argument and the element from
     *                    {@code other} as second argument
     * @return a new {@code CharMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if {@code other} or {@code zipFunction} is {@code null},
     *         or if the matrices have different shapes
     * @throws E if the zip function throws an exception
     * @see #zipWith(CharMatrix, CharMatrix, Throwables.CharTernaryOperator)
     */
    public <E extends Exception> CharMatrix zipWith(final CharMatrix other, final Throwables.CharBinaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final char[][] otherData = other.a;
        final char[][] result = new char[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsChar(a[i][j], otherData[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return CharMatrix.of(result);
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
     * CharMatrix a = CharMatrix.of(new char[][] {{'a', 'b'}});
     * CharMatrix b = CharMatrix.of(new char[][] {{'c', 'd'}});
     * CharMatrix c = CharMatrix.of(new char[][] {{'e', 'f'}});
     * a.zipWith(b, c, (x, y, z) -> (char) Math.max(Math.max(x, y), z)).rowView(0); // returns ['e', 'f'] (max of each triple)
     * a.zipWith(b, c, (x, y, z) -> (char) Math.min(Math.min(x, y), z)).rowView(0); // returns ['a', 'b'] (min of each triple)
     *
     * CharMatrix wrong = CharMatrix.of(new char[][] {{'x', 'y', 'z'}});
     * a.zipWith(b, wrong, (x, y, z) -> x);                                      // throws IllegalArgumentException (different shapes)
     * a.zipWith(b, c, (Throwables.CharTernaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null function)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param third the third matrix (must have the same dimensions as this matrix)
     * @param zipFunction the ternary operator to apply to corresponding elements; receives the
     *                    element from this matrix as first argument, the element from
     *                    {@code other} as second argument, and the element from {@code third}
     *                    as third argument
     * @return a new {@code CharMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if any of {@code other}, {@code third}, or {@code zipFunction}
     *         is {@code null}, or if any of the matrices have different shapes
     * @throws E if the zip function throws an exception
     * @see #zipWith(CharMatrix, Throwables.CharBinaryOperator)
     */
    public <E extends Exception> CharMatrix zipWith(final CharMatrix other, final CharMatrix third, final Throwables.CharTernaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(third, "third");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other) && isSameShape(third), "Cannot zip matrices with different shapes: this is {}x{}, other is {}x{}, third is {}x{}",
                rowCount, columnCount, other.rowCount, other.columnCount, third.rowCount, third.columnCount);

        final char[][] otherData = other.a;
        final char[][] thirdData = third.a;
        final char[][] result = new char[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsChar(a[i][j], otherData[i][j], thirdData[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.shouldRunInParallel(this));

        return CharMatrix.of(result);
    }

    /**
     * Returns a stream of elements on the main diagonal (upper-left to lower-right).
     * The matrix must be square.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}, {'g', 'h', 'i'}});
     * matrix.mainDiagonalStream().toArray();  // returns ['a', 'e', 'i']
     * matrix.mainDiagonalStream().sum();      // returns 303
     *
     * CharMatrix.empty().mainDiagonalStream().count(); // returns 0 (empty stream)
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.mainDiagonalStream();         // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a CharStream of main-diagonal elements, or an empty stream if the matrix is empty
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public CharStream mainDiagonalStream() {
        checkIsSquare();

        if (isEmpty()) {
            return CharStream.empty();
        }

        return CharStream.of(new CharIteratorEx() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public char nextChar() {
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}, {'g', 'h', 'i'}});
     * matrix.antiDiagonalStream().toArray();  // returns ['c', 'e', 'g']
     * matrix.antiDiagonalStream().sum();      // returns 303
     *
     * CharMatrix.empty().antiDiagonalStream().count(); // returns 0 (empty stream)
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.antiDiagonalStream();         // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a CharStream of anti-diagonal elements, or an empty stream if the matrix is empty
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public CharStream antiDiagonalStream() {
        checkIsSquare();

        if (isEmpty()) {
            return CharStream.empty();
        }

        return CharStream.of(new CharIteratorEx() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public char nextChar() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final char result = a[cursor][columnCount - cursor - 1];
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
     * standard CharStream operations including sum, average, filter, map, etc.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.rowMajorStream().toArray();    // returns ['a', 'b', 'c', 'd']
     * matrix.rowMajorStream().sum();        // returns 394
     *
     * CharMatrix.empty().rowMajorStream().count();                // returns 0 (empty stream)
     * CharMatrix.of(new char[][] {{'x'}}).rowMajorStream().sum(); // returns 120 (single element)
     * }</pre>
     *
     * @return a CharStream of all elements in row-major order, or an empty stream if the matrix is empty
     */
    @Override
    public CharStream rowMajorStream() {
        return rowMajorStream(0, rowCount);
    }

    /**
     * Returns a stream of elements from a single row.
     * The elements are streamed from left to right within the specified row.
     *
     * <p>This method is particularly useful when you need to process or analyze
     * a specific row of the matrix independently. The returned stream can be
     * used with all standard CharStream operations.</p>
     *
     * <p>This streams the elements of the single specified row, flattened into one stream. To
     * instead obtain every row as its own stream (a stream of streams), use {@link #rowStreams()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.rowMajorStream(0).toArray();   // returns ['a', 'b', 'c']
     * matrix.rowMajorStream(1).sum();       // returns 303 (sum of second row)
     *
     * matrix.rowMajorStream(-1);            // throws IndexOutOfBoundsException
     * matrix.rowMajorStream(2);             // throws IndexOutOfBoundsException (rowIndex >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to stream (0-based)
     * @return a {@link CharStream} of elements from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowStreams()
     */
    @Override
    public CharStream rowMajorStream(final int rowIndex) {
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}, {'e', 'f'}});
     * matrix.rowMajorStream(1, 3).toArray(); // returns ['c', 'd', 'e', 'f']
     * matrix.rowMajorStream(0, 2).toArray(); // returns ['a', 'b', 'c', 'd']
     *
     * matrix.rowMajorStream(1, 1).count();  // returns 0 (empty range)
     * matrix.rowMajorStream(0, 5);          // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a CharStream of elements from the specified row range, or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public CharStream rowMajorStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        if (isEmpty()) {
            return CharStream.empty();
        }

        return CharStream.of(new CharIteratorEx() {
            private int i = fromRowIndex;
            private int j = 0;

            @Override
            public boolean hasNext() {
                return i < toRowIndex;
            }

            @Override
            public char nextChar() {
                if (i >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final char result = a[i][j++];

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
            public char[] toArray() {
                final int len = toArrayLength(count());
                final char[] c = new char[len];
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.columnMajorStream().toArray();      // returns ['a', 'c', 'b', 'd'] (column-major)
     * matrix.columnMajorStream().sum();          // returns 394
     *
     * CharMatrix.empty().columnMajorStream().count();                // returns 0 (empty stream)
     * CharMatrix.of(new char[][] {{'x'}}).columnMajorStream().sum(); // returns 120 (single element)
     * }</pre>
     *
     * @return a CharStream of all elements in column-major order, or an empty stream if the matrix is empty
     */
    @Override
    public CharStream columnMajorStream() {
        return columnMajorStream(0, columnCount);
    }

    /**
     * Returns a stream of elements from a single column.
     * The elements are streamed from top to bottom within the specified column.
     *
     * <p>This method is useful for column-wise operations such as calculating
     * column sums, finding column maximums, or filtering column values.</p>
     *
     * <p>This streams the elements of the single specified column, flattened into one stream. To
     * instead obtain every column as its own stream (a stream of streams), use {@link #columnStreams()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.columnMajorStream(1).toArray();     // returns ['b', 'e']
     * matrix.columnMajorStream(0).sum();         // returns 197 (sum of first column)
     *
     * matrix.columnMajorStream(-1);              // throws IndexOutOfBoundsException
     * matrix.columnMajorStream(3);               // throws IndexOutOfBoundsException (columnIndex >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to stream (0-based)
     * @return a {@link CharStream} of elements from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     * @see #columnStreams()
     */
    @Override
    public CharStream columnMajorStream(final int columnIndex) {
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.columnMajorStream(1, 3).toArray();  // returns ['b', 'e', 'c', 'f']
     * matrix.columnMajorStream(0, 2).toArray();  // returns ['a', 'd', 'b', 'e']
     *
     * matrix.columnMajorStream(1, 1).count();    // returns 0 (empty range)
     * matrix.columnMajorStream(0, 5);            // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a CharStream of elements from the specified column range in column-major order,
     *         or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    public CharStream columnMajorStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        if (isEmpty()) {
            return CharStream.empty();
        }

        return CharStream.of(new CharIteratorEx() {
            private int i = 0;
            private int j = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return j < toColumnIndex;
            }

            @Override
            public char nextChar() {
                if (j >= toColumnIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final char result = a[i++][j];

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

                if (n >= (long) (toColumnIndex - j) * CharMatrix.this.rowCount - i) {
                    i = 0;
                    j = toColumnIndex;
                } else {
                    final long offset = n + i;
                    i = (int) (offset % CharMatrix.this.rowCount);
                    j += (int) (offset / CharMatrix.this.rowCount);
                }
            }

            @Override
            public long count() {
                return (long) (toColumnIndex - j) * rowCount - i; // NOSONAR
            }

            @Override
            public char[] toArray() {
                final int len = toArrayLength(count());
                final char[] c = new char[len];

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
     * Returns a stream of CharStream objects, where each CharStream represents a complete row.
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}, {'e', 'f'}});
     * matrix.rowStreams().count();            // returns 3 (one stream per row)
     * matrix.rowStreams()
     *     .mapToInt(row -> row.sum())
     *     .toArray();                         // returns [195, 199, 203]
     *
     * CharMatrix.empty().rowStreams().count();                       // returns 0 (no rows)
     * CharMatrix.of(new char[][] {{'a', 'b'}}).rowStreams().count(); // returns 1 (single row)
     * }</pre>
     *
     * @return a Stream of CharStream objects, one for each row in the matrix
     * @see #rowMajorStream(int)
     */
    @Override
    public Stream<CharStream> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Returns a stream of CharStream objects for a range of rows.
     * Each CharStream in the result represents a complete row within the specified range.
     *
     * <p>This method allows for processing a subset of rows while maintaining the
     * ability to work with complete rows as individual streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}, {'e', 'f'}});
     * matrix.rowStreams(1, 3).count();        // returns 2 (rows 1 and 2)
     * matrix.rowStreams(0, 2)
     *     .mapToInt(row -> row.max().orElse((char) 0))
     *     .toArray();                         // returns [98, 100]
     *
     * matrix.rowStreams(1, 1).count();        // returns 0 (empty range)
     * matrix.rowStreams(0, 5);                // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of CharStream objects for the specified row range
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public Stream<CharStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public CharStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return CharStream.of(a[cursor++]);
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
     * Returns a stream of CharStream objects, where each CharStream represents a complete column.
     * This creates a stream of streams, allowing for column-by-column processing of the matrix.
     *
     * <p>This method is useful for operations that need to process
     * entire columns as units, such as column-wise statistics, transformations, or filtering
     * columns based on conditions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.columnStreams().count();         // returns 3 (one stream per column)
     * matrix.columnStreams()
     *     .mapToInt(col -> col.sum())
     *     .toArray();                         // returns [197, 199, 201]
     *
     * CharMatrix.empty().columnStreams().count();                          // returns 0 (no columns)
     * CharMatrix.of(new char[][] {{'a'}, {'b'}}).columnStreams().count();  // returns 1 (single column)
     * }</pre>
     *
     * @return a Stream of CharStream objects, one for each column in the matrix,
     *         or an empty stream if the matrix is empty
     */
    @Override
    public Stream<CharStream> columnStreams() {
        return columnStreams(0, columnCount);
    }

    /**
     * Returns a stream of CharStream objects for a range of columns.
     * Each CharStream in the result represents a complete column within the specified range.
     *
     * <p>This method allows for processing a subset of columns
     * while maintaining the ability to work with complete columns as individual streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.columnStreams(1, 3).count();     // returns 2 (columns 1 and 2)
     * matrix.columnStreams(0, 2)
     *     .mapToInt(col -> col.sum())
     *     .toArray();                         // returns [197, 199]
     *
     * matrix.columnStreams(1, 1).count();     // returns 0 (empty range)
     * matrix.columnStreams(0, 5);             // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of CharStream objects for the specified column range,
     *         or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    public Stream<CharStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
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
            public CharStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return CharStream.of(new CharIteratorEx() {
                    private final int columnIndex = cursor++;
                    private final int toIndex2 = rowCount;
                    private int cursor2 = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor2 < toIndex2;
                    }

                    @Override
                    public char nextChar() {
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
     * @param row the row array to measure; may be {@code null}
     * @return the length of {@code row}, or {@code 0} if {@code row} is {@code null}
     */
    @Override
    protected int length(final char[] row) {
        return row == null ? 0 : row.length;
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
     * or accumulating). For transformations that create new matrices, use {@link #map(Throwables.CharUnaryOperator)}
     * or {@link #updateAll(Throwables.CharUnaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger();
     * matrix.forEach(ch -> count.incrementAndGet());
     * count.get();                // returns 4
     *
     * java.util.concurrent.atomic.AtomicInteger codeUnitSum = new java.util.concurrent.atomic.AtomicInteger();
     * matrix.forEach(codeUnitSum::addAndGet);
     * codeUnitSum.get();          // returns 394 ('a' + 'b' + 'c' + 'd')
     *
     * CharMatrix.empty().forEach(ch -> count.incrementAndGet());
     * count.get();                // still returns 4 (no elements visited)
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element; receives each element value
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     * @see #forEach(int, int, int, int, Throwables.CharConsumer)
     */
    public <E extends Exception> void forEach(final Throwables.CharConsumer<E> action) throws E {
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'},
     *                                                 {'d', 'e', 'f'},
     *                                                 {'g', 'h', 'i'}});
     *
     * // Process only the bottom-right 2x2 sub-region (rows 1-2, columns 1-2).
     * StringBuilder sb = new StringBuilder();
     * Matrices.runWithParallelMode(ParallelMode.FORCE_OFF,
     *         () -> matrix.forEach(1, 3, 1, 3, sb::append));
     * sb.toString();              // returns "efhi"
     *
     * // Process the first two rows, last two columns.
     * StringBuilder sb2 = new StringBuilder();
     * Matrices.runWithParallelMode(ParallelMode.FORCE_OFF,
     *         () -> matrix.forEach(0, 2, 1, 3, sb2::append));
     * sb2.toString();             // returns "bcef"
     *
     * matrix.forEach(0, 5, 0, 3, ch -> {}); // throws IndexOutOfBoundsException (toRowIndex > rowCount)
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
            final Throwables.CharConsumer<E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        if (Matrices.shouldRunInParallel(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, elementAction, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                final char[] currentRow = a[i];

                for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                    action.accept(currentRow[j]);
                }
            }
        }
    }

    /**
     * Renders this matrix as a multi-line string (one row per line, e.g. {@code "[a, b]\n[c, d]"}); a
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

                    final char[] row = a[i];
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
     * CharMatrix m1 = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * CharMatrix m2 = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * boolean sameHash = m1.hashCode() == m2.hashCode(); // true (equal matrices share a hash code)
     *
     * CharMatrix m3 = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'x'}});
     * boolean sameHashForDifferentContent = m1.hashCode() == m3.hashCode(); // false for these values
     * CharMatrix.empty().hashCode();                                        // returns a stable hash for the empty matrix
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
     * Returns {@code true} if the given object is also a {@code CharMatrix} with the same dimensions
     * and all corresponding elements are equal. Returns {@code false} for any other type
     * (including primitive matrices of different element types).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix m1 = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * CharMatrix m2 = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * m1.equals(m2);                          // returns true (same shape and elements)
     *
     * CharMatrix m3 = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'x'}});
     * m1.equals(m3);                          // returns false (different element)
     * m1.equals(null);                        // returns false
     * m1.equals("not a matrix");              // returns false (different type)
     * }</pre>
     *
     * @param obj the object to compare with; may be {@code null}
     * @return {@code true} if {@code obj} is a {@code CharMatrix} with identical shape and elements,
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final CharMatrix another) {
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.toString();          // returns "[[a, b], [c, d]]"
     *
     * CharMatrix single = CharMatrix.of(new char[][] {{'x'}});
     * single.toString();          // returns "[[x]]"
     *
     * CharMatrix.empty().toString(); // returns "[]"
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
