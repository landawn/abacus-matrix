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
 * validated backing array. Constructors and {@code of(...)} generally wrap the supplied storage
 * directly, while factories, conversions, and mapping operations allocate new arrays.</p>
 *
 * <p>Cells introduced by growth or reshaping default to {@code '\u0000'} (the NUL character) unless an overload accepts an
 * explicit fill value.</p>
 *
 * <p>Each element is a Java {@code char}, an unsigned 16-bit UTF-16 code unit in the range
 * {@code [0, 65535]}. Arithmetic operations such as {@link #add(CharMatrix)},
 * {@link #subtract(CharMatrix)}, and {@link #matmul(CharMatrix)} compute results as {@code int}
 * and cast back to {@code char}, so values wrap modulo {@code 65536}. Ordering and comparisons
 * use the unsigned numeric value, not Unicode collation. Surrogate pairs are not interpreted; a
 * supplementary code point occupies two adjacent cells.</p>
 *
 * @see IntMatrix
 * @see ByteMatrix
 * @see ShortMatrix
 */
public final class CharMatrix extends AbstractMatrix<char[], CharList, CharStream, Stream<CharStream>, CharMatrix> {

    static final int BOUND = Character.MAX_VALUE + 1;
    private static final CharMatrix EMPTY_CHAR_MATRIX = new CharMatrix(new char[0][0]);

    /**
     * Constructs a {@code CharMatrix} backed by the supplied two-dimensional array.
     *
     * <p>If {@code a} is {@code null}, this creates an empty {@code 0x0} matrix. Otherwise the array
     * is used directly after rectangular-shape validation, so later modifications to either the input
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
     * CharMatrix empty = new CharMatrix((char[][]) null);
     * empty.rowCount();                                 // returns 0 (null -> empty 0x0 matrix)
     * new CharMatrix(new char[][] {{'a', 'b'}, {'c'}}); // throws IllegalArgumentException (jagged)
     * }</pre>
     *
     * @param a the two-dimensional char array to wrap, or {@code null} for an empty matrix
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public CharMatrix(final char[][] a) {
        super(a == null ? new char[0][0] : a, char.class);
    }

    /**
     * Returns an empty {@code CharMatrix} with zero rows and zero columns.
     * The same shared instance is returned on every call.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.empty();
     * matrix.rowCount();                        // returns 0
     * matrix.columnCount();                     // returns 0
     * matrix.isEmpty();                         // returns true
     * CharMatrix.empty() == CharMatrix.empty(); // true (shared singleton instance)
     * }</pre>
     *
     * @return the shared empty {@code CharMatrix}
     */
    public static CharMatrix empty() {
        return EMPTY_CHAR_MATRIX;
    }

    /**
     * Creates a {@code CharMatrix} from a two-dimensional char array.
     *
     * <p><b>Important:</b> The provided array is used directly without defensive copying.
     * Changes to the input array are reflected in the returned matrix, and vice versa.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.get(1, 0);                  // returns 'c'
     * matrix.get(0, 1);                  // returns 'b'
     *
     * CharMatrix.of((char[][]) null).isEmpty(); // returns true
     * CharMatrix.of(new char[0][0]).isEmpty();  // returns true
     * }</pre>
     *
     * @param a the two-dimensional char array to wrap; may be {@code null} or empty, in which case
     *        the shared empty {@code CharMatrix} instance is returned
     * @return a new {@code CharMatrix} backed by the provided data, or the shared empty {@code CharMatrix}
     *         if the input is {@code null} or empty
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public static CharMatrix of(final char[]... a) {
        return N.isEmpty(a) ? EMPTY_CHAR_MATRIX : new CharMatrix(a);
    }

    /**
     * Creates a new {@code 1 x length} matrix filled with random char values drawn uniformly from
     * the full unsigned 16-bit range {@code [0, 65535]}. Values are not constrained to printable
     * characters and may include surrogates and control codes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.random(5);
     * matrix.rowCount();            // returns 1
     * matrix.columnCount();         // returns 5 (values are random)
     *
     * CharMatrix.random(0).columnCount(); // returns 0 (empty single row)
     * CharMatrix.random(-1);              // throws IllegalArgumentException
     * }</pre>
     *
     * @param length the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code CharMatrix} of dimensions {@code 1 x length} filled with random values
     * @throws IllegalArgumentException if {@code length} is negative
     */
    public static CharMatrix random(final int length) {
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
     * CharMatrix.range('a', 'a').rowView(0).length; // returns 0 (empty: start == end)
     * CharMatrix.range('e', 'a').rowView(0).length; // returns 0 (empty: end < start)
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
     * Creates a single-row CharMatrix containing a range of char values with a step.
     * The range increments by the specified step size. Supports both ascending (positive step)
     * and descending (negative step) sequences.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix.range('a', 'g', 2).rowView(0);   // returns ['a', 'c', 'e']
     * CharMatrix.range('z', 'u', -2).rowView(0);  // returns ['z', 'x', 'v']
     *
     * CharMatrix.range('a', 'z', -1).rowView(0).length; // returns 0 (step direction wrong)
     * CharMatrix.range('a', 'z', 0);                    // throws IllegalArgumentException (step is zero)
     * }</pre>
     *
     * @param startInclusive the starting char value (inclusive)
     * @param endExclusive the ending char value (exclusive)
     * @param step the step size (must not be zero; can be positive or negative)
     * @return a new 1×n CharMatrix with values incremented by the step size
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
     * CharMatrix.rangeClosed('d', 'a').rowView(0).length; // returns 0 (end < start)
     * CharMatrix.rangeClosed('b', 'a').rowView(0).length; // returns 0 (end < start)
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
     * Creates a single-row CharMatrix containing a closed range of char values with a step.
     * The range is [startInclusive, endInclusive]. The range increments by the specified step size.
     * Supports both ascending (positive step) and descending (negative step) sequences.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix.rangeClosed('a', 'g', 2).rowView(0);    // returns ['a', 'c', 'e', 'g']
     * CharMatrix.rangeClosed('a', 'i', 2).rowView(0);    // returns ['a', 'c', 'e', 'g', 'i']
     * CharMatrix.rangeClosed('z', 'u', -2).rowView(0);   // returns ['z', 'x', 'v']
     *
     * CharMatrix.rangeClosed('a', 'z', -1).rowView(0).length; // returns 0 (step direction wrong)
     * CharMatrix.rangeClosed('a', 'z', 0);                    // throws IllegalArgumentException (step is zero)
     * }</pre>
     *
     * @param startInclusive the starting char value (inclusive)
     * @param endInclusive the ending char value (inclusive, if reachable by stepping)
     * @param step the step size (must not be zero; can be positive or negative)
     * @return a new 1×n CharMatrix with values incremented by the step size
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public static CharMatrix rangeClosed(final char startInclusive, final char endInclusive, final int step) {
        return new CharMatrix(new char[][] { Array.rangeClosed(startInclusive, endInclusive, step) });
    }

    /**
     * Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
     * All other elements (off-diagonal) are set to zero (the null character '\u0000'). The matrix size is n×n where n is the length
     * of the diagonal array. The main diagonal runs from top-left to bottom-right.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.mainDiagonal(new char[] {'a', 'b', 'c'});
     * matrix.get(0, 0);                                 // returns 'a'
     * matrix.get(2, 2);                                 // returns 'c'
     * matrix.get(0, 1) == '\u0000';                     // true (off-diagonal default)
     * CharMatrix.mainDiagonal((char[]) null).isEmpty(); // returns true
     * CharMatrix.mainDiagonal(new char[0]).isEmpty();   // returns true
     * // Resulting 3x3 matrix:
     * //   {'a', '\u0000', '\u0000'},
     * //   {'\u0000', 'b', '\u0000'},
     * //   {'\u0000', '\u0000', 'c'}
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements; may be {@code null} or empty,
     *        in which case the empty matrix is returned
     * @return a square matrix with the specified main diagonal (n×n where n = diagonal length),
     *         or an empty matrix if {@code mainDiagonal} is {@code null} or empty
     */
    public static CharMatrix mainDiagonal(final char[] mainDiagonal) {
        return diagonals(mainDiagonal, null);
    }

    /**
     * Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
     * All other elements (off-diagonal) are set to zero (the null character '\u0000'). The matrix size is n×n where n is the length
     * of the diagonal array. The anti-diagonal runs from top-right to bottom-left.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.antiDiagonal(new char[] {'a', 'b', 'c'});
     * matrix.get(0, 2);                                 // returns 'a'
     * matrix.get(2, 0);                                 // returns 'c'
     * matrix.get(0, 0) == '\u0000';                     // true (off-anti-diagonal default)
     * CharMatrix.antiDiagonal((char[]) null).isEmpty(); // returns true
     * CharMatrix.antiDiagonal(new char[0]).isEmpty();   // returns true
     * // Resulting 3x3 matrix:
     * //   {'\u0000', '\u0000', 'a'},
     * //   {'\u0000', 'b', '\u0000'},
     * //   {'c', '\u0000', '\u0000'}
     * }</pre>
     *
     * @param antiDiagonal the array of anti-diagonal elements; may be {@code null} or empty,
     *        in which case the empty matrix is returned
     * @return a square matrix with the specified anti-diagonal (n×n where n = diagonal length),
     *         or an empty matrix if {@code antiDiagonal} is {@code null} or empty
     */
    public static CharMatrix antiDiagonal(final char[] antiDiagonal) {
        return diagonals(null, antiDiagonal);
    }

    /**
     * Creates a square matrix from the specified main diagonal and anti-diagonal elements.
     * All other elements are set to zero (the null character '\u0000'). If both arrays are provided, they must have the same length.
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
     * //   {'a', '\u0000', 'x'},
     * //   {'\u0000', 'b', '\u0000'},
     * //   {'z', '\u0000', 'c'}
     *
     * CharMatrix.diagonals((char[]) null, (char[]) null).isEmpty();            // returns true
     * CharMatrix.diagonals(new char[] {'a', 'b'}, new char[] {'x', 'y', 'z'}); // throws IllegalArgumentException (length mismatch)
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements (can be {@code null} or empty)
     * @param antiDiagonal the array of anti-diagonal elements (can be {@code null} or empty)
     * @return a square matrix with the specified diagonals, or an empty matrix if both inputs are {@code null} or empty
     * @throws IllegalArgumentException if both arrays are non-empty and have different lengths
     */
    public static CharMatrix diagonals(final char[] mainDiagonal, final char[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(N.isEmpty(mainDiagonal) || N.isEmpty(antiDiagonal) || mainDiagonal.length == antiDiagonal.length,
                "The length of 'mainDiagonal' and 'antiDiagonal' must be same");

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
     * Converts a boxed Character Matrix to a primitive CharMatrix.
     * {@code null} values in the input matrix are converted to {@code '\u0000'} (the null character).
     *
     * <p>This method performs the opposite operation of {@link #boxed()}, converting
     * from object-based Character values to primitive char values. This conversion
     * improves memory efficiency and performance when working with large matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Character> boxedMatrix = Matrix.of(new Character[][] {{'a', 'b'}, {null, 'c'}});
     * CharMatrix primitive = CharMatrix.unbox(boxedMatrix);
     * primitive.get(0, 0);                        // returns 'a'
     * primitive.get(1, 1);                        // returns 'c'
     * primitive.get(1, 0);                        // returns the null character (null mapped to default)
     * CharMatrix.unbox((Matrix<Character>) null); // throws IllegalArgumentException
     * // null is converted to '\u0000': [['a', 'b'], ['\u0000', 'c']]
     * }</pre>
     *
     * @param x the boxed Character Matrix to convert; must not be {@code null}
     * @return a new CharMatrix with primitive char values
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
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
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
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
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
     * Returns the element directly above the specified position, if it exists.
     * This method provides safe access to the element directly above the given position
     * without throwing an exception when at the top edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.valueAbove(1, 0).get();         // returns 'a'
     * matrix.valueAbove(1, 1).get();         // returns 'b'
     *
     * matrix.valueAbove(0, 0).isPresent();   // returns false (no row above the top edge)
     * matrix.valueAbove(5, 0);               // throws IndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalChar containing the element at position (rowIndex - 1, columnIndex), or empty if rowIndex == 0
     * @throws IndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalChar valueAbove(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? OptionalChar.empty() : OptionalChar.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, if it exists.
     * This method provides safe access to the element directly below the given position
     * without throwing an exception when at the bottom edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.valueBelow(0, 0).get();         // returns 'c'
     * matrix.valueBelow(0, 1).get();         // returns 'd'
     *
     * matrix.valueBelow(1, 0).isPresent();   // returns false (no row below the bottom edge)
     * matrix.valueBelow(5, 0);               // throws IndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalChar containing the element at position (rowIndex + 1, columnIndex), or empty if rowIndex == rowCount - 1
     * @throws IndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalChar valueBelow(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? OptionalChar.empty() : OptionalChar.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, if it exists.
     * This method provides safe access to the element directly to the left of the given position
     * without throwing an exception when at the leftmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.valueLeft(0, 1).get();          // returns 'a'
     * matrix.valueLeft(1, 1).get();          // returns 'c'
     *
     * matrix.valueLeft(0, 0).isPresent();    // returns false (already at the leftmost column)
     * matrix.valueLeft(0, 5);                // throws IndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalChar containing the element at position (rowIndex, columnIndex - 1), or empty if columnIndex == 0
     * @throws IndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalChar valueLeft(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? OptionalChar.empty() : OptionalChar.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, if it exists.
     * This method provides safe access to the element directly to the right of the given position
     * without throwing an exception when at the rightmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.valueRight(0, 0).get();         // returns 'b'
     * matrix.valueRight(1, 0).get();         // returns 'd'
     *
     * matrix.valueRight(0, 1).isPresent();   // returns false (already at the rightmost column)
     * matrix.valueRight(0, 5);               // throws IndexOutOfBoundsException (out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an OptionalChar containing the element at position (rowIndex, columnIndex + 1), or empty if columnIndex == columnCount - 1
     * @throws IndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalChar valueRight(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? OptionalChar.empty() : OptionalChar.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as a char array.
     *
     * <p><b>Note:</b> This method returns a reference to the internal array, not a copy.
     * Modifications to the returned array will affect the matrix. If you need an independent
     * copy, use {@code Arrays.copyOf(matrix.rowView(i), matrix.columnCount())}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.rowView(0);          // returns ['a', 'b', 'c']
     * matrix.rowView(1);          // returns ['d', 'e', 'f']
     *
     * // Direct modification affects the matrix (shared array reference)
     * matrix.rowView(0)[0] = 'x';
     * matrix.get(0, 0);           // returns 'x'
     *
     * matrix.rowView(5);          // throws IndexOutOfBoundsException (row out of bounds)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return the specified row array (direct reference to internal storage)
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    @Override
    public char[] rowView(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return a[rowIndex];
    }

    /**
     * Returns a defensive copy of the specified row.
     * Changes to the returned array do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.rowCopy(0);          // returns ['a', 'b', 'c']
     * matrix.rowCopy(1);          // returns ['d', 'e', 'f']
     *
     * // Modification does NOT affect the matrix (it is a copy)
     * matrix.rowCopy(0)[0] = 'x';
     * matrix.get(0, 0);           // returns 'a' (unchanged)
     *
     * matrix.rowCopy(5);          // throws IndexOutOfBoundsException (row out of bounds)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new char array containing the values from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    @Override
    public char[] rowCopy(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return N.copyOf(a[rowIndex], columnCount);
    }

    /**
     * Returns a copy of the specified column as a new char array.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously in memory. Modifications to the returned array
     * will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.columnCopy(0);       // returns ['a', 'd']
     * matrix.columnCopy(2);       // returns ['c', 'f']
     *
     * // Modification does NOT affect the matrix (it is a copy)
     * matrix.columnCopy(0)[0] = 'x';
     * matrix.get(0, 0);           // returns 'a' (unchanged)
     *
     * matrix.columnCopy(5);       // throws IndexOutOfBoundsException (column out of bounds)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new array containing the values from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
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
     * @param row the array of values to copy into the row; must not be {@code null} and must have
     *        length equal to {@link #columnCount()}
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
     * @param column the array of values to copy into the column; must not be {@code null} and must
     *        have length equal to {@link #rowCount()}
     * @throws IndexOutOfBoundsException if {@code columnIndex} is out of bounds
     * @throws IllegalArgumentException if {@code column} is {@code null} or if {@code column.length != rowCount}
     */
    public void setColumn(final int columnIndex, final char[] column) throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkArgNotNull(column, "column");
        checkColumnIndex(columnIndex);
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
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.CharUnaryOperator<E> operator) throws E {
        checkRowIndex(rowIndex);

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < columnCount; i++) {
            a[rowIndex][i] = operator.applyAsChar(a[rowIndex][i]);
        }
    }

    /**
     * Updates all elements in the specified column in-place by applying the given operator to each element.
     * This modifies the matrix directly.
     *
     * <p>The operator is applied to each element in the specified column sequentially
     * from top to bottom (row 0 to row rowCount-1).</p>
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
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.CharUnaryOperator<E> operator) throws E {
        checkColumnIndex(columnIndex);

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = operator.applyAsChar(a[i][columnIndex]);
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}, {'g', 'h', 'i'}});
     * matrix.getMainDiagonal();   // returns ['a', 'e', 'i']
     *
     * CharMatrix single = CharMatrix.of(new char[][] {{'x'}});
     * single.getMainDiagonal();   // returns ['x']
     *
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.getMainDiagonal(); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new char array containing the main diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public char[] getMainDiagonal() throws IllegalStateException {
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
     * matrix.getMainDiagonal();   // returns ['x', 'y', 'z']
     * matrix.get(0, 1);           // returns 'b' (off-diagonal unchanged)
     *
     * matrix.setMainDiagonal(new char[] {'x', 'y'}); // throws IllegalArgumentException (length mismatch)
     *
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.setMainDiagonal(new char[] {'x', 'y'}); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must have length equal to rowCount
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if mainDiagonal is null or its array length does not equal rowCount
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
     * matrix.getMainDiagonal();   // returns ['A', 'D']
     * matrix.get(0, 1);           // returns 'b' (off-diagonal unchanged)
     * matrix.get(1, 0);           // returns 'c' (off-diagonal unchanged)
     *
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.updateMainDiagonal(c -> c); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each diagonal element
     * @throws IllegalStateException if the matrix is not square
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateMainDiagonal(final Throwables.CharUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = operator.applyAsChar(a[i][i]);
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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'},
     *                                                 {'d', 'e', 'f'},
     *                                                 {'g', 'h', 'i'}});
     * matrix.getAntiDiagonal();   // returns ['c', 'e', 'g']
     *
     * CharMatrix single = CharMatrix.of(new char[][] {{'x'}});
     * single.getAntiDiagonal();   // returns ['x']
     *
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.getAntiDiagonal(); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new char array containing the anti-diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public char[] getAntiDiagonal() throws IllegalStateException {
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
     * matrix.getAntiDiagonal();   // returns ['x', 'y', 'z']
     * matrix.get(0, 0);           // returns 'a' (off-anti-diagonal unchanged)
     *
     * matrix.setAntiDiagonal(new char[] {'x', 'y'}); // throws IllegalArgumentException (length mismatch)
     *
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.setAntiDiagonal(new char[] {'x', 'y'}); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must have length equal to rowCount
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if antiDiagonal is null or its array length does not equal rowCount
     */
    @Override
    public void setAntiDiagonal(final char[] antiDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgNotNull(antiDiagonal, "antiDiagonal");
        N.checkArgument(N.len(antiDiagonal) == rowCount, MSG_DIAGONAL_LENGTH_MISMATCH, rowCount, N.len(antiDiagonal));

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = antiDiagonal[i];
        }
    }

    /**
     * Updates the elements on the anti-diagonal (upper-right to lower-left) using the specified operator.
     * The matrix must be square. Each anti-diagonal element is replaced with the result of applying
     * the operator to that element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.updateAntiDiagonal(c -> Character.toUpperCase(c));
     * matrix.getAntiDiagonal();   // returns ['B', 'C']
     * matrix.get(0, 0);           // returns 'a' (off-anti-diagonal unchanged)
     * matrix.get(1, 1);           // returns 'd' (off-anti-diagonal unchanged)
     *
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.updateAntiDiagonal(c -> c); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the exception type that the operator may throw
     * @param operator the operator to apply to each anti-diagonal element
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAntiDiagonal(final Throwables.CharUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = operator.applyAsChar(a[i][columnCount - i - 1]);
        }
    }

    /**
     * Updates all elements in the matrix using the specified operator in-place.
     *
     * <p>Each element is replaced with the result of applying the operator to that element.
     * For large matrices, this operation may be performed in parallel to improve performance.
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
     * CharMatrix.empty().updateAll(c -> c); // no-op on an empty matrix
     * }</pre>
     *
     * @param <E> the exception type that the operator may throw
     * @param operator the operator to apply to each element
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.CharUnaryOperator<E> operator) throws E {
        N.checkArgNotNull(operator, "operator");
        final Throwables.IntBiConsumer<E> cmd = (i, j) -> a[i][j] = operator.applyAsChar(a[i][j]);
        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));
    }

    /**
     * Updates all elements in the matrix based on their position using a position-aware mapper.
     *
     * <p>The mapper receives the row and column indices and returns the new value for that position.
     * This is useful when the new value depends on the element's location in the matrix.
     * For large matrices, this operation may be performed in parallel.
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
     * CharMatrix.empty().updateAll((i, j) -> 'z'); // no-op on an empty matrix
     * }</pre>
     *
     * @param <E> the exception type that the mapper may throw
     * @param mapper the function that takes (rowIndex, columnIndex) and returns the new char value
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws NullPointerException if {@code mapper} returns {@code null} for any position
     * @throws E if the mapper throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Character, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final Throwables.IntBiConsumer<E> cmd = (i, j) -> a[i][j] = mapper.apply(i, j);
        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));
    }

    /**
     * Replaces all elements that match the predicate with the specified value.
     *
     * <p>This operation modifies the matrix in-place. Only elements for which the predicate
     * returns true are replaced; other elements remain unchanged.
     * For large matrices, this operation may be performed in parallel.
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
     * CharMatrix.empty().replaceIf(c -> true, 'x'); // no-op on an empty matrix
     * }</pre>
     *
     * @param <E> the exception type that the predicate may throw
     * @param predicate the predicate to test each element
     * @param newValue the value to replace matching elements with
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.CharPredicate<E> predicate, final char newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> cmd = (i, j) -> a[i][j] = predicate.test(a[i][j]) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));
    }

    /**
     * Replaces all elements at positions that match the position-based predicate with the specified value.
     *
     * <p>The predicate receives the row and column indices for each position and determines
     * whether the element at that position should be replaced. This is useful for replacing
     * elements based on their location (e.g., diagonal elements, specific rows/columns).
     * For large matrices, this operation may be performed in parallel.
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
     * CharMatrix.empty().replaceIf((i, j) -> true, 'x'); // no-op on an empty matrix
     * }</pre>
     *
     * @param <E> the exception type that the predicate may throw
     * @param predicate the predicate that takes (rowIndex, columnIndex) and returns true for positions to replace
     * @param newValue the value to replace at matching positions
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final char newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> cmd = (i, j) -> a[i][j] = predicate.test(i, j) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));
    }

    /**
     * Creates a new CharMatrix by applying a transformation function to each element.
     * The original matrix is not modified; a new matrix with transformed values is returned.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
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
        final Throwables.IntBiConsumer<E> cmd = (i, j) -> result[i][j] = mapper.applyAsChar(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return CharMatrix.of(result);
    }

    /**
     * Creates a new object Matrix by applying the specified function to each char element.
     *
     * <p>This method transforms the primitive CharMatrix into an object-based Matrix,
     * applying the mapping function to convert each char to an object of type R.
     * For large matrices, this operation may be performed in parallel.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * Matrix<String> stringMatrix = matrix.mapToObj(c -> String.valueOf(c), String.class);
     * stringMatrix.get(0, 0);     // returns "a"
     * stringMatrix.get(1, 1);     // returns "d"
     *
     * Matrix<Integer> codePoints = matrix.mapToObj(c -> (int) c, Integer.class);
     * codePoints.get(0, 0);       // returns 97
     * codePoints.get(1, 1);       // returns 100
     *
     * CharMatrix.empty().mapToObj(c -> "" + c, String.class).isEmpty(); // returns true
     * }</pre>
     *
     * @param <R> the type of elements in the resulting matrix
     * @param <E> the exception type that the function may throw
     * @param mapper the mapping function that converts each char to an object of type R
     * @param targetElementType the class object representing the target element type (required for array creation;
     *        must not be {@code null})
     * @return a new {@code Matrix<R>} with the mapped object values
     * @throws IllegalArgumentException if {@code mapper} or {@code targetElementType} is {@code null}
     * @throws E if the function throws an exception
     */
    public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.CharFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);
        final Throwables.IntBiConsumer<E> cmd = (i, j) -> result[i][j] = mapper.apply(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return Matrix.of(result);
    }

    /**
     * Fills all elements in the matrix with the specified value.
     *
     * <p>This operation modifies the matrix in-place, setting every element
     * to the same value.
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
     * Fills the matrix with values from the specified two-dimensional array in-place, starting from position (0,0).
     * Values are copied up to the minimum of the matrix size and the source array size. If the source
     * array is smaller than the matrix, only the overlapping region is filled. If the source array is
     * larger, only the portion that fits is copied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[3][3]);
     * matrix.fill(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * // Top-left 2x2 region is filled; the rest keeps the null character '\u0000':
     * //   [['a', 'b', '\u0000'], ['c', 'd', '\u0000'], ['\u0000', '\u0000', '\u0000']]
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
     * @param source the source array to copy values from (may be smaller or larger than the matrix);
     *        must not be {@code null}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final char[][] source) {
        fill(0, 0, source);
    }

    /**
     * Fills a portion of the matrix with values from the specified two-dimensional array in-place, starting from a specified position.
     * Values are copied starting from the specified row and column indices. If the source array extends
     * beyond the matrix bounds from the starting position, only the portion that fits is copied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[3][3]);
     * matrix.fill(1, 1, new char[][] {{'a', 'b'}, {'c', 'd'}});
     * // Result (' ' = the null character):
     * //   [[' ', ' ', ' '], [' ', 'a', 'b'], [' ', 'c', 'd']]
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
     * @param destRowIndex the target row index in this matrix (0-based); must satisfy {@code 0 <= destRowIndex <= rowCount}
     * @param destColumnIndex the target column index in this matrix (0-based); must satisfy {@code 0 <= destColumnIndex <= columnCount}
     * @param source the source array to copy values from; must not be {@code null} (individual rows may be {@code null} and are skipped)
     * @throws IndexOutOfBoundsException if {@code destRowIndex < 0} or {@code destRowIndex > rowCount},
     *         or if {@code destColumnIndex < 0} or {@code destColumnIndex > columnCount}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final char[][] source) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     * Modifications to the returned matrix will not affect this matrix, and vice versa.
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
     * @return a copy of this matrix
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
     * CharMatrix copy = matrix.copy(1, 3);
     * copy.rowView(0);            // returns ['c', 'd']
     * copy.rowView(1);            // returns ['e', 'f']
     *
     * matrix.copy(0, 0).isEmpty(); // returns true (empty row range)
     * matrix.copy(1, 5);           // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new CharMatrix containing the specified rows
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex}
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
     * Creates a copy of a rectangular region from this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}, {'g', 'h', 'i'}});
     * CharMatrix sub = matrix.copy(0, 2, 1, 3);   // rows 0-1, columns 1-2
     * sub.rowView(0);                             // returns ['b', 'c']
     * sub.rowView(1);                             // returns ['e', 'f']
     *
     * matrix.copy(0, 2, 1, 1).columnCount(); // returns 0 (empty column range)
     * matrix.copy(0, 2, 1, 5);               // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new CharMatrix containing the specified region
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, {@code fromRowIndex > toRowIndex},
     *         {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount}, or {@code fromColumnIndex > toColumnIndex}
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
     * anchored at the top-left corner of this matrix. New cells are filled with {@code '\u0000'}.
     *
     * <ul>
     *   <li><b>If a dimension shrinks</b> — elements beyond the new boundary are discarded
     *       (excess rows removed from the bottom, excess columns removed from the right).</li>
     *   <li><b>If a dimension grows</b> — new cells are filled with {@code '\u0000'}.</li>
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
     * // Grow: both dimensions larger — new cells filled with '\u0000'
     * CharMatrix grown = matrix.resize(4, 4);
     * // Result: [['a', 'b', 'c', '\u0000'],
     * //          ['d', 'e', 'f', '\u0000'],
     * //          ['g', 'h', 'i', '\u0000'],
     * //          ['\u0000', '\u0000', '\u0000', '\u0000']]
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
     * //          ['\u0000', '\u0000']]
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
     *       {@code defaultValue} is <em>not</em> used in this case.</li>
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
     *        ignored when a dimension shrinks
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
     * New cells are filled with {@code '\u0000'}.
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
     * // Uniform 1-cell border of '\u0000'
     * CharMatrix bordered = matrix.extend(1, 1, 1, 1);
     * // Result: [['\u0000', '\u0000', '\u0000', '\u0000'],
     * //          ['\u0000', 'a',      'b',      '\u0000'],
     * //          ['\u0000', 'c',      'd',      '\u0000'],
     * //          ['\u0000', '\u0000', '\u0000', '\u0000']]
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * CharMatrix shifted = matrix.extend(0, 0, 2, 0);
     * // Result: [['\u0000', '\u0000', 'a', 'b'],
     * //          ['\u0000', '\u0000', 'c', 'd']]
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
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
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
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
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
     */
    @Override
    public void flipHorizontallyInPlace() {
        for (int i = 0; i < rowCount; i++) {
            N.reverse(a[i]);
        }
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
     * @return a new matrix that is this matrix rotated 90 degrees clockwise
     * @throws IllegalArgumentException if the resulting (transposed) shape is not representable
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
     * @return a new matrix that is this matrix rotated 270 degrees clockwise
     * @throws IllegalArgumentException if the resulting (transposed) shape is not representable
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
     * @throws IllegalArgumentException if the resulting (transposed) shape is not representable
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
     * Reshapes the matrix to the specified dimensions.
     *
     * <p>Elements are read from the source matrix in row-major order (left to right, top to bottom)
     * and written to the new matrix in row-major order. The new shape must have at least as many
     * total elements as the original ({@code newRowCount * newColumnCount >= elementCount()}).
     * If the new shape requires more elements than available in the source matrix, the remaining
     * positions are filled with default char values ('\u0000').
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * // Original is 2x3 (6 elements)
     *
     * CharMatrix reshaped1 = matrix.reshape(3, 2);
     * reshaped1.rowView(0);       // returns ['a', 'b']
     * reshaped1.rowView(2);       // returns ['e', 'f']
     *
     * matrix.reshape(3, 1);       // throws IllegalArgumentException (3 < 6 elements: shape too small)
     *
     * CharMatrix reshaped2 = matrix.reshape(2, 4);
     * // Result: [['a', 'b', 'c', 'd'], ['e', 'f', '\u0000', '\u0000']] - 8 positions, last 2 filled with default
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix (must be {@code >= 0})
     * @param newColumnCount the number of columns in the reshaped matrix (must be {@code >= 0})
     * @return a new CharMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if the new shape is too small to hold all elements
     *         ({@code (long) newRowCount * newColumnCount < elementCount()})
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public CharMatrix reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final char[][] c = new char[newRowCount][newColumnCount];

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return new CharMatrix(c);
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
     * @see IntMatrix#repeatElements(int, int)
     */
    @Override
    public CharMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

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
                N.copy(Array.repeat(aa[j], columnRepeats), 0, fr, j * columnRepeats, columnRepeats);
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
     * @see IntMatrix#repeatMatrix(int, int)
     */
    @Override
    public CharMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

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
     * Returns a CharList containing all matrix elements in row-major order.
     *
     * <p>This method converts the two-dimensional matrix into a one-dimensional list by reading elements
     * row by row from left to right, top to bottom.
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
     * @return a new CharList containing all elements in row-major order
     * @throws IllegalStateException if the matrix is too large to flatten ({@code rowCount * columnCount > Integer.MAX_VALUE})
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
     * Flattens all elements of this matrix into a single one-dimensional array, applies the given
     * operation to that flattened array, and then copies the modified elements back into the matrix.
     *
     * <p>This enables operations that need a global view of all matrix elements (e.g., sorting all
     * elements across the entire matrix). The operation receives a temporary flattened copy; after
     * the operation completes, the modified values are written back into the matrix row by row.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'d', 'b'}, {'c', 'a'}});
     * matrix.mutateAsFlat(arr -> java.util.Arrays.sort(arr));
     * matrix.rowView(0);          // returns ['a', 'b']
     * matrix.rowView(1);          // returns ['c', 'd']
     *
     * CharMatrix m = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * m.mutateAsFlat(arr -> { for (int i = 0; i < arr.length; i++) arr[i] = Character.toUpperCase(arr[i]); });
     * m.rowView(0);               // returns ['A', 'B']
     *
     * CharMatrix.empty().mutateAsFlat(arr -> java.util.Arrays.sort(arr)); // no-op on an empty matrix
     * }</pre>
     *
     * @param <E> the exception type that the operation may throw
     * @param action the operation to apply to the flattened array
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the operation throws an exception
     * @see Arrays#mutateAsFlat(char[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super char[], E> action) throws E {
        N.checkArgNotNull(action, "action");

        Arrays.mutateAsFlat(a, action);
    }

    /**
     * Vertically stacks this matrix on top of another matrix.
     * Both matrices must have the same number of columns.
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
     * @param other the matrix to stack below this matrix; must not be {@code null}
     * @return a new CharMatrix with other appended below this matrix
     * @throws IllegalArgumentException if {@code other} is {@code null}, the matrices have different
     *         column counts, or the merged row count would exceed {@link Integer#MAX_VALUE}
     * @see IntMatrix#stackVertically(IntMatrix)
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
     * Horizontally stacks this matrix to the left of another matrix.
     * Both matrices must have the same number of rows.
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
     * @param other the matrix to stack to the right of this matrix; must not be {@code null}
     * @return a new CharMatrix with other appended to the right of this matrix
     * @throws IllegalArgumentException if {@code other} is {@code null}, the matrices have different
     *         row counts, or the merged column count would exceed {@link Integer#MAX_VALUE}
     * @see IntMatrix#stackHorizontally(IntMatrix)
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
     * Both matrices must have the same dimensions. Each result element is computed as
     * {@code (char) (a[i][j] + other[i][j])}, so values wrap modulo {@code 65536} on overflow.
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
     * (int) max.add(one).get(0, 0); // returns 0 (wrapped)
     *
     * CharMatrix wrong = CharMatrix.of(new char[][] {{'x', 'y', 'z'}});
     * a.add(wrong);               // throws IllegalArgumentException (shape mismatch)
     * a.add((CharMatrix) null);   // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * @param other the matrix to add to this matrix; must not be {@code null} and must have the same shape
     * @return a new CharMatrix containing the element-wise sum
     * @throws IllegalArgumentException if {@code other} is {@code null} or has different dimensions
     */
    public CharMatrix add(final CharMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot add matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final char[][] otherArray = other.a;
        final char[][] result = new char[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> cmd = (i, j) -> result[i][j] = (char) (a[i][j] + otherArray[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return CharMatrix.of(result);
    }

    /**
     * Performs element-wise subtraction of another matrix from this matrix.
     * Both matrices must have the same dimensions. Each result element is computed as
     * {@code (char) (this[i][j] - other[i][j])}, so values wrap modulo {@code 65536} on
     * underflow (a negative {@code int} difference becomes a large {@code char}).
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
     * (int) zero.subtract(one).get(0, 0); // returns 65535 (wrapped)
     *
     * CharMatrix wrong = CharMatrix.of(new char[][] {{'x', 'y', 'z'}});
     * a.subtract(wrong);               // throws IllegalArgumentException (shape mismatch)
     * a.subtract((CharMatrix) null);   // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * @param other the matrix to subtract from this matrix; must not be {@code null} and must have the same shape
     * @return a new CharMatrix containing the element-wise difference
     * @throws IllegalArgumentException if {@code other} is {@code null} or has different dimensions
     */
    public CharMatrix subtract(final CharMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot subtract matrices with different shapes: this is {}x{} but other is {}x{}", rowCount,
                columnCount, other.rowCount, other.columnCount);

        final char[][] otherArray = other.a;
        final char[][] result = new char[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> cmd = (i, j) -> result[i][j] = (char) (a[i][j] - otherArray[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return CharMatrix.of(result);
    }

    /**
     * Performs matrix multiplication with another matrix.
     * The number of columns in this matrix must equal the number of rows in the other matrix.
     * The resulting matrix has dimensions {@code this.rowCount x other.columnCount}.
     *
     * <p>Each accumulation step is performed with {@code char +=}, which promotes to {@code int}
     * for the multiply-add and then casts the running sum back to {@code char}. As a result,
     * any intermediate or final value outside {@code [0, 65535]} wraps modulo {@code 65536}.
     * Consider {@link IntMatrix} or {@link DoubleMatrix} for numerical computations where
     * truncation/wraparound is undesirable.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix a = CharMatrix.of(new char[][] {{2, 3}, {4, 5}});
     * CharMatrix b = CharMatrix.of(new char[][] {{1, 2}, {3, 4}});
     * CharMatrix product = a.matmul(b);   // standard matrix multiplication
     * (int) product.get(0, 0);            // returns 11  (2*1 + 3*3)
     * (int) product.get(0, 1);            // returns 16  (2*2 + 3*4)
     * (int) product.get(1, 1);            // returns 28  (4*2 + 5*4)
     *
     * CharMatrix wrong = CharMatrix.of(new char[][] {{1, 2, 3}}); // 1x3, this.columnCount is 2
     * a.matmul(wrong);                                            // throws IllegalArgumentException (this.columnCount != other.rowCount)
     * a.matmul((CharMatrix) null);                                // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * <p><b>Note:</b> This is the linear-algebra matrix product, not element-wise multiplication.
     * For element-wise multiplication use
     * {@link #zipWith(CharMatrix, com.landawn.abacus.util.Throwables.CharBinaryOperator)}.</p>
     *
     * @param other the matrix to multiply with this matrix; must not be {@code null}
     * @return a new CharMatrix containing the matrix product
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.columnCount != other.rowCount}, or if the resulting {@code rowCount x other.columnCount} shape is not representable
     */
    public CharMatrix matmul(final CharMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.rowCount,
                "Matrix dimensions incompatible for multiplication: this is {}x{}, other is {}x{} (this.columnCount must equal other.rowCount)", rowCount,
                columnCount, other.rowCount, other.columnCount);

        checkRepresentableShape(rowCount, other.columnCount);

        final char[][] otherArray = other.a;
        final char[][] result = new char[rowCount][other.columnCount];
        final Throwables.IntTriConsumer<RuntimeException> cmd = (i, j, k) -> result[i][j] += a[i][k] * otherArray[k][j];

        Matrices.forEachCartesianIndices(this, other, cmd);

        return CharMatrix.of(result);
    }

    /**
     * Converts this CharMatrix to a Matrix of Character objects.
     * Each primitive char value is boxed into a Character object.
     * This is useful when you need to work with object-based operations
     * or APIs that require Character objects instead of primitives.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix charMatrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * Matrix<Character> boxed = charMatrix.boxed();
     * boxed.get(0, 0);            // returns Character 'a'
     * boxed.get(1, 1);            // returns Character 'd'
     *
     * CharMatrix.empty().boxed().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new Matrix containing Character objects with the same values and dimensions
     * @see #unbox(Matrix)
     */
    public Matrix<Character> boxed() {
        final Character[][] c = new Character[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final char[] aa = a[i];
                final Character[] cc = c[i];

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

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final char[] aa = a[i];
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

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final char[] aa = a[i];
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

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final char[] aa = a[i];
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
     * Both matrices must have the same dimensions. The zip function is applied
     * to corresponding elements from both matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix a = CharMatrix.of(new char[][] {{'a', 'b'}});
     * CharMatrix b = CharMatrix.of(new char[][] {{'A', 'B'}});
     * a.zipWith(b, (x, y) -> (char) Math.max(x, y)).rowView(0); // returns ['a', 'b'] (lowercase code units are larger)
     * a.zipWith(b, (x, y) -> (char) Math.min(x, y)).rowView(0); // returns ['A', 'B'] (min of each pair)
     *
     * CharMatrix wrong = CharMatrix.of(new char[][] {{'x', 'y', 'z'}});
     * a.zipWith(wrong, (x, y) -> x);                                                                // throws IllegalArgumentException (shape mismatch)
     * a.zipWith(b, (com.landawn.abacus.util.Throwables.CharBinaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null function)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix to zip with this matrix
     * @param zipFunction the binary operation to apply to corresponding elements
     * @return a new CharMatrix containing the results of the zip operation
     * @throws IllegalArgumentException if {@code other} or {@code zipFunction} is {@code null}, or if the matrices have different shapes
     * @throws E if the zip function throws an exception
     */
    public <E extends Exception> CharMatrix zipWith(final CharMatrix other, final Throwables.CharBinaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final char[][] otherData = other.a;
        final char[][] result = new char[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> cmd = (i, j) -> result[i][j] = zipFunction.applyAsChar(a[i][j], otherData[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return CharMatrix.of(result);
    }

    /**
     * Applies a ternary operation element-wise to this matrix and two other matrices.
     * All three matrices must have the same dimensions. The zip function is applied
     * to corresponding elements from all three matrices.
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
     * a.zipWith(b, wrong, (x, y, z) -> x); // throws IllegalArgumentException (shape mismatch)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix to zip with
     * @param third the third matrix to zip with
     * @param zipFunction the ternary operation to apply to corresponding elements
     * @return a new CharMatrix containing the results of the zip operation
     * @throws IllegalArgumentException if any of {@code other}, {@code third}, or {@code zipFunction} is {@code null}, or if any of the matrices have different shapes
     * @throws E if the zip function throws an exception
     */
    public <E extends Exception> CharMatrix zipWith(final CharMatrix other, final CharMatrix third, final Throwables.CharTernaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(third, "third");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other) && isSameShape(third), "Cannot zip matrices with different shapes: all matrices must be {}x{}", rowCount,
                columnCount);

        final char[][] otherData = other.a;
        final char[][] thirdData = third.a;
        final char[][] result = new char[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> cmd = (i, j) -> result[i][j] = zipFunction.applyAsChar(a[i][j], otherData[i][j], thirdData[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, cmd, Matrices.isParallelizable(this));

        return CharMatrix.of(result);
    }

    /**
     * Returns a stream of elements on the diagonal from upper-left to lower-right.
     * The matrix must be square (same number of rows and columns).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'},
     *                                                 {'d', 'e', 'f'},
     *                                                 {'g', 'h', 'i'}});
     * matrix.mainDiagonalStream().toArray(); // returns ['a', 'e', 'i']
     *
     * CharMatrix.empty().mainDiagonalStream().count(); // returns 0 (empty matrix is allowed)
     *
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.mainDiagonalStream(); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a CharStream containing the diagonal elements from top-left to bottom-right
     * @throws IllegalStateException if the matrix is not square
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
     * Returns a stream of elements on the anti-diagonal from upper-right to lower-left.
     * The matrix must be square (same number of rows and columns).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'},
     *                                                 {'d', 'e', 'f'},
     *                                                 {'g', 'h', 'i'}});
     * matrix.antiDiagonalStream().toArray(); // returns ['c', 'e', 'g']
     *
     * CharMatrix.empty().antiDiagonalStream().count(); // returns 0 (empty matrix is allowed)
     *
     * CharMatrix nonSquare = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * nonSquare.antiDiagonalStream(); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a CharStream containing the diagonal elements from top-right to bottom-left
     * @throws IllegalStateException if the matrix is not square
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
     * Returns a stream of all elements in this matrix, traversed horizontally (left to right, top to bottom).
     * Elements are returned in row-major order: all elements from the first row,
     * then all elements from the second row, and so on.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.horizontalStream().toArray(); // returns ['a', 'b', 'c', 'd']
     *
     * CharMatrix single = CharMatrix.of(new char[][] {{'x', 'y', 'z'}});
     * single.horizontalStream().toArray(); // returns ['x', 'y', 'z']
     *
     * CharMatrix.empty().horizontalStream().count(); // returns 0
     * }</pre>
     *
     * @return a CharStream containing all matrix elements traversed horizontally (left to right, top to bottom)
     */
    @Override
    public CharStream horizontalStream() {
        return horizontalStream(0, rowCount);
    }

    /**
     * Returns a stream of elements from a specific row.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.horizontalStream(1).toArray(); // returns ['c', 'd']
     * matrix.horizontalStream(0).toArray(); // returns ['a', 'b']
     *
     * matrix.horizontalStream(5);  // throws IndexOutOfBoundsException (row out of bounds)
     * matrix.horizontalStream(-1); // throws IndexOutOfBoundsException (negative row)
     * }</pre>
     *
     * @param rowIndex the index of the row to stream (0-based)
     * @return a CharStream containing all elements from the specified row
     * @throws IndexOutOfBoundsException if rowIndex is negative or {@code >=} number of rows
     */
    @Override
    public CharStream horizontalStream(final int rowIndex) {
        return horizontalStream(rowIndex, rowIndex + 1);
    }

    /**
     * Returns a CharStream of elements from a range of rows, traversed horizontally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}, {'e', 'f'}});
     * matrix.horizontalStream(1, 3).toArray(); // returns ['c', 'd', 'e', 'f'] (rows 1 and 2)
     * matrix.horizontalStream(1, 1).count();   // returns 0 (empty row range)
     *
     * matrix.horizontalStream(1, 5); // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.horizontalStream(2, 1); // throws IndexOutOfBoundsException (fromRowIndex > toRowIndex)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a CharStream of elements from the specified rows
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public CharStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
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
     * Returns a stream of all elements in the matrix, traversed vertically (column by column).
     * Elements are returned in column-major order: all elements from the first column,
     * then all elements from the second column, and so on.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.verticalStream().toArray(); // returns ['a', 'c', 'b', 'd'] (column-major)
     *
     * CharMatrix single = CharMatrix.of(new char[][] {{'x'}, {'y'}, {'z'}});
     * single.verticalStream().toArray(); // returns ['x', 'y', 'z']
     *
     * CharMatrix.empty().verticalStream().count(); // returns 0
     * }</pre>
     *
     * @return a CharStream containing all matrix elements in column-major order
     */
    @Override
    @Beta
    public CharStream verticalStream() {
        return verticalStream(0, columnCount);
    }

    /**
     * Returns a stream of elements from a specific column.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * matrix.verticalStream(1).toArray(); // returns ['b', 'd']
     * matrix.verticalStream(0).toArray(); // returns ['a', 'c']
     *
     * matrix.verticalStream(5);  // throws IndexOutOfBoundsException (column out of bounds)
     * matrix.verticalStream(-1); // throws IndexOutOfBoundsException (negative column)
     * }</pre>
     *
     * @param columnIndex the index of the column to stream (0-based)
     * @return a CharStream containing all elements from the specified column
     * @throws IndexOutOfBoundsException if columnIndex is negative or {@code >=} number of columns
     */
    @Override
    public CharStream verticalStream(final int columnIndex) {
        return verticalStream(columnIndex, columnIndex + 1);
    }

    /**
     * Returns a stream of elements from a range of columns, traversed vertically.
     * Elements are returned in column-major order within the specified range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.verticalStream(1, 3).toArray(); // returns ['b', 'e', 'c', 'f'] (columns 1 and 2)
     * matrix.verticalStream(1, 1).count();   // returns 0 (empty column range)
     *
     * matrix.verticalStream(1, 5); // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.verticalStream(2, 1); // throws IndexOutOfBoundsException (fromColumnIndex > toColumnIndex)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a CharStream containing elements from the specified column range
     * @throws IndexOutOfBoundsException if the indices are out of bounds or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    @Beta
    public CharStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
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
     * Returns a stream of CharStreams, where each CharStream represents a row in the matrix.
     * This allows for row-wise operations on the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * List<CharStream> rows = matrix.rowStreams().toList();
     * rows.get(0).toArray();       // returns ['a', 'b']
     * rows.get(1).toArray();       // returns ['c', 'd']
     * matrix.rowStreams().count(); // returns 2
     *
     * CharMatrix.empty().rowStreams().count(); // returns 0
     * }</pre>
     *
     * @return a Stream of CharStreams, one for each row in the matrix
     */
    @Override
    public Stream<CharStream> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Returns a stream of CharStreams for a range of rows.
     * Each CharStream in the result represents a complete row from the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}, {'e', 'f'}});
     * List<CharStream> rows = matrix.rowStreams(1, 3).toList();
     * rows.get(0).toArray();      // returns ['c', 'd']
     * rows.get(1).toArray();      // returns ['e', 'f']
     *
     * matrix.rowStreams(1, 1).count(); // returns 0 (empty row range)
     * matrix.rowStreams(1, 5);         // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of CharStreams for the specified row range
     * @throws IndexOutOfBoundsException if the indices are out of bounds or {@code fromRowIndex > toRowIndex}
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
     * Returns a stream of CharStreams, where each CharStream represents a column in the matrix.
     * This allows for column-wise operations on the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * List<CharStream> columns = matrix.columnStreams().toList();
     * columns.get(0).toArray();       // returns ['a', 'c']
     * columns.get(1).toArray();       // returns ['b', 'd']
     * matrix.columnStreams().count(); // returns 2
     *
     * CharMatrix.empty().columnStreams().count(); // returns 0
     * }</pre>
     *
     * @return a Stream of CharStreams, one for each column in the matrix
     */
    @Override
    @Beta
    public Stream<CharStream> columnStreams() {
        return columnStreams(0, columnCount);
    }

    /**
     * Returns a stream of CharStreams for a range of columns.
     * Each CharStream in the result represents a complete column from the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * List<CharStream> columns = matrix.columnStreams(1, 3).toList();
     * columns.get(0).toArray();   // returns ['b', 'e']
     * columns.get(1).toArray();   // returns ['c', 'f']
     *
     * matrix.columnStreams(1, 1).count(); // returns 0 (empty column range)
     * matrix.columnStreams(1, 5);         // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of CharStreams for the specified column range
     * @throws IndexOutOfBoundsException if the indices are out of bounds or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    @Beta
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
     * Returns the length of the specified char array.
     *
     * <p>This is an internal helper method used by the abstract parent class
     * for various matrix operations to determine row lengths.
     *
     * @param a the char array to measure
     * @return the length of the array, or 0 if the array is {@code null}
     */
    @Override
    protected int length(@SuppressWarnings("hiding") final char[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Performs the specified action for each element in this matrix.
     *
     * <p>The action is performed on all elements in row-major order (left to right, top to bottom).
     * For large matrices, the operation may be parallelized automatically to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * StringBuilder sb = new StringBuilder();
     * matrix.forEach(ch -> sb.append(ch));
     * sb.toString();              // returns "abcd" (row-major order)
     *
     * List<Character> chars = new ArrayList<>();
     * matrix.forEach(chars::add);
     * chars.size();               // returns 4
     *
     * StringBuilder empty = new StringBuilder();
     * CharMatrix.empty().forEach(empty::append);
     * empty.length();             // returns 0 (no elements visited)
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed on each element
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final Throwables.CharConsumer<E> action) throws E {
        forEach(0, rowCount, 0, columnCount, action);
    }

    /**
     * Performs the specified action for each element in a sub-region of this matrix.
     *
     * <p>The action is performed on elements within the specified row and column ranges
     * in row-major order when executed sequentially. This allows you to operate on a rectangular
     * portion of the matrix without affecting other elements. For large sub-regions, the operation
     * may be parallelized automatically to improve performance; if parallelized, the order in which
     * elements are visited is unspecified and the action must be thread-safe, but every element is
     * still visited exactly once.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'},
     *                                                 {'d', 'e', 'f'},
     *                                                 {'g', 'h', 'i'}});
     *
     * // Process only the bottom-right 2x2 sub-region (rows 1-2, columns 1-2).
     * StringBuilder sb = new StringBuilder();
     * matrix.forEach(1, 3, 1, 3, sb::append);
     * sb.toString();              // returns "efhi"
     *
     * // Process the first two rows, last two columns.
     * StringBuilder sb2 = new StringBuilder();
     * matrix.forEach(0, 2, 1, 3, sb2::append);
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
     * @param action the action to be performed on each element in the sub-region
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws IndexOutOfBoundsException if {@code fromRowIndex}/{@code toRowIndex} or
     *         {@code fromColumnIndex}/{@code toColumnIndex} are out of range, or if either
     *         {@code fromRowIndex > toRowIndex} or {@code fromColumnIndex > toColumnIndex}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex,
            final Throwables.CharConsumer<E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        if (Matrices.isParallelizable(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> cmd = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, cmd, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                final char[] aa = a[i];

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
     * CharMatrix matrix = CharMatrix.of(new char[][] {{'a', 'b', 'c'}, {'d', 'e', 'f'}});
     * matrix.println();           // returns (and prints) "[a, b, c]\n[d, e, f]"
     *
     * CharMatrix single = CharMatrix.of(new char[][] {{'x'}});
     * single.println();           // returns (and prints) "[x]"
     *
     * CharMatrix.empty().println(); // returns (and prints) "[]"
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
     * CharMatrix m1 = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * CharMatrix m2 = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * m1.hashCode() == m2.hashCode();   // returns true (equal matrices share a hash code)
     *
     * CharMatrix m3 = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'x'}});
     * m1.hashCode() == m3.hashCode();   // typically returns false (different content)
     * CharMatrix.empty().hashCode();    // returns a stable hash for the empty matrix
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
     * and all corresponding elements are equal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharMatrix m1 = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * CharMatrix m2 = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'd'}});
     * m1.equals(m2);              // returns true (same shape and values)
     *
     * CharMatrix m3 = CharMatrix.of(new char[][] {{'a', 'b'}, {'c', 'x'}});
     * m1.equals(m3);                                       // returns false (one element differs)
     * m1.equals(CharMatrix.of(new char[][] {{'a', 'b'}})); // returns false (different shape)
     * m1.equals("not a matrix");                           // returns false (different type)
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if {@code obj} is a {@code CharMatrix} with the same shape and equal
     *         element values; {@code false} otherwise
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
     * Returns a string representation of this matrix.
     * The format consists of matrix elements in a two-dimensional array format with rows enclosed in brackets.
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
     * @return a string representation of this matrix
     */
    @Override
    public String toString() {
        return N.deepToString(a);
    }
}
