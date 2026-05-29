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
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * Matrix implementation backed by a rectangular {@code boolean[][]}.
 *
 * <p>This type specializes {@link AbstractMatrix} for {@code boolean} values while keeping the data in
 * a validated backing array. Constructors and {@code of(...)} generally wrap the supplied storage
 * directly, while factories, conversions, and mapping operations allocate new arrays.</p>
 *
 * <p>Cells introduced by growth or reshaping default to {@code false} unless an overload accepts an
 * explicit fill value. Optional return values use {@link OptionalBoolean}.</p>
 *
 * <p>This is the {@code boolean} sibling of {@link ByteMatrix}, {@link IntMatrix}, {@link LongMatrix},
 * and the other primitive-element matrix classes in this package.</p>
 */
public final class BooleanMatrix extends AbstractMatrix<boolean[], BooleanList, Stream<Boolean>, Stream<Stream<Boolean>>, BooleanMatrix> {

    private static final BooleanMatrix EMPTY_BOOLEAN_MATRIX = new BooleanMatrix(new boolean[0][0]);

    /**
     * Constructs a {@code BooleanMatrix} backed by the supplied two-dimensional array.
     *
     * <p>If {@code a} is {@code null}, this creates an empty {@code 0x0} matrix. Otherwise the array
     * is used directly after rectangular-shape validation, so later modifications to either the input
     * array or the matrix remain visible through the other view. Call {@link #copy()} if you need an
     * independently owned matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[][] data = {{true, false, true}, {false, true, false}};
     * BooleanMatrix matrix = new BooleanMatrix(data);
     * data[0][0] = false;  // This will also modify the matrix
     * }</pre>
     *
     * @param a the two-dimensional boolean array to wrap, or {@code null} for an empty matrix
     * @throws IllegalArgumentException if {@code a} contains a {@code null} row or rows of differing lengths
     */
    public BooleanMatrix(final boolean[][] a) {
        super(a == null ? new boolean[0][0] : a, boolean.class);
    }

    /**
     * Returns a shared empty {@code 0 × 0} matrix instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.empty();
     * // matrix.rowCount() returns 0
     * // matrix.columnCount() returns 0
     * }</pre>
     *
     * @return the shared empty {@code BooleanMatrix} singleton (zero rows, zero columns)
     */
    public static BooleanMatrix empty() {
        return EMPTY_BOOLEAN_MATRIX;
    }

    /**
     * Creates a {@code BooleanMatrix} from a two-dimensional boolean array.
     *
     * <p><b>Important:</b> The provided array is used directly without defensive copying.
     * Changes to the input array are reflected in the returned matrix, and vice versa.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * // matrix.get(0, 1) returns false
     * }</pre>
     *
     * @param a the two-dimensional boolean array to wrap; may be {@code null} or empty, in which case the empty matrix singleton is returned
     * @return a new {@code BooleanMatrix} backed by {@code a}, or the empty {@code BooleanMatrix} if {@code a} is {@code null} or empty
     * @throws IllegalArgumentException if {@code a} contains a {@code null} row or rows of differing lengths
     */
    public static BooleanMatrix of(final boolean[]... a) {
        return N.isEmpty(a) ? EMPTY_BOOLEAN_MATRIX : new BooleanMatrix(a);
    }

    /**
     * Creates a new {@code 1 × length} matrix filled with pseudo-randomly generated boolean values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.random(5);
     * // Result: a 1x5 matrix with random boolean values
     * }</pre>
     *
     * @param length the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code BooleanMatrix} of dimensions {@code 1 × length} filled with random values
     * @throws IllegalArgumentException if {@code length} is negative
     * @see #random(int, int)
     */
    public static BooleanMatrix random(final int length) {
        return random(1, length);
    }

    /**
     * Creates a new matrix of the specified dimensions filled with pseudo-randomly generated boolean values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.random(2, 3);
     * // Result: a 2x3 matrix with random boolean values
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code BooleanMatrix} of dimensions {@code rowCount × columnCount} filled with random values
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative
     */
    public static BooleanMatrix random(final int rowCount, final int columnCount) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final boolean[][] a = new boolean[rowCount][columnCount];

        for (boolean[] ea : a) {
            for (int i = 0; i < columnCount; i++) {
                ea[i] = RAND.nextInt() % 2 == 0;
            }
        }

        return new BooleanMatrix(a);
    }

    /**
     * Creates a new matrix of the specified dimensions where every element is the provided {@code element}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.repeat(2, 3, true);
     * // Result: [[true, true, true], [true, true, true]]
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @param element the boolean value to fill the matrix with
     * @return a new {@code BooleanMatrix} of dimensions {@code rowCount × columnCount} with every element set to {@code element}
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative
     */
    public static BooleanMatrix repeat(final int rowCount, final int columnCount, final boolean element) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final boolean[][] a = new boolean[rowCount][columnCount];

        for (boolean[] ea : a) {
            N.fill(ea, element);
        }

        return new BooleanMatrix(a);
    }

    /**
     * Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
     * All other elements are set to {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.mainDiagonal(new boolean[] {true, false, true});
     * // Creates 3x3 matrix with diagonal [true, false, true] and false elsewhere
     * // Resulting matrix:
     * //   {true, false, false},
     * //   {false, false, false},
     * //   {false, false, true}
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements; may be {@code null} or empty,
     *        in which case an empty matrix is returned
     * @return a square matrix with the specified main diagonal ({@code n × n} where {@code n}
     *         is the diagonal length), or an empty matrix if {@code mainDiagonal} is {@code null} or empty
     * @see #antiDiagonal(boolean[])
     * @see #diagonals(boolean[], boolean[])
     */
    public static BooleanMatrix mainDiagonal(final boolean[] mainDiagonal) {
        return diagonals(mainDiagonal, null);
    }

    /**
     * Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
     * All other elements are set to {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.antiDiagonal(new boolean[] {true, false, true});
     * // Creates 3x3 matrix with anti-diagonal [true, false, true] and false elsewhere
     * // Resulting matrix:
     * //   {false, false, true},
     * //   {false, false, false},
     * //   {true, false, false}
     * }</pre>
     *
     * @param antiDiagonal the array of anti-diagonal elements; may be {@code null} or empty,
     *        in which case an empty matrix is returned
     * @return a square matrix with the specified anti-diagonal ({@code n × n} where {@code n}
     *         is the diagonal length), or an empty matrix if {@code antiDiagonal} is {@code null} or empty
     * @see #mainDiagonal(boolean[])
     * @see #diagonals(boolean[], boolean[])
     */
    public static BooleanMatrix antiDiagonal(final boolean[] antiDiagonal) {
        return diagonals(null, antiDiagonal);
    }

    /**
     * Creates a square matrix from the specified main diagonal and anti-diagonal elements.
     * All other elements are set to {@code false}. If both arrays are non-empty, they must have the
     * same length. The resulting matrix has dimensions {@code n × n} where {@code n} is the length
     * of the non-empty diagonal array. When both diagonals are provided and they overlap (at the
     * center element of an odd-sized matrix), the main diagonal value takes precedence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.diagonals(new boolean[] {true, true, true}, new boolean[] {true, false, true});
     * // Creates 3x3 matrix with both diagonals set
     * // Resulting matrix (main diagonal takes precedence at center):
     * //   {true, false, true},
     * //   {false, true, false},
     * //   {true, false, true}
     *
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements; may be {@code null} or empty
     * @param antiDiagonal the array of anti-diagonal elements; may be {@code null} or empty
     * @return a square matrix with the specified diagonals, or an empty matrix if both inputs are {@code null} or empty
     * @throws IllegalArgumentException if both arrays are non-empty and have different lengths
     */
    public static BooleanMatrix diagonals(final boolean[] mainDiagonal, final boolean[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(N.isEmpty(mainDiagonal) || N.isEmpty(antiDiagonal) || mainDiagonal.length == antiDiagonal.length,
                "The length of 'mainDiagonal' and 'antiDiagonal' must be same");

        if (N.isEmpty(mainDiagonal) && N.isEmpty(antiDiagonal)) {
            return EMPTY_BOOLEAN_MATRIX;
        }

        final int len = N.max(N.len(mainDiagonal), N.len(antiDiagonal));
        final boolean[][] result = new boolean[len][len];

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

        return new BooleanMatrix(result);
    }

    /**
     * Converts a boxed {@code Matrix<Boolean>} to a primitive {@code BooleanMatrix}.
     * {@code null} values in the input matrix are converted to {@code false}.
     *
     * <p>This method performs the opposite operation of {@link #boxed()}, converting
     * from object-based {@code Boolean} values to primitive {@code boolean} values. This conversion
     * improves memory efficiency and performance when working with large matrices.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Boolean> boxed = Matrix.of(new Boolean[][] {{true, false}, {null, true}});
     * BooleanMatrix primitive = BooleanMatrix.unbox(boxed);
     * // null is converted to false: [[true, false], [false, true]]
     * }</pre>
     *
     * @param x the boxed {@code Matrix<Boolean>} to convert; must not be {@code null}
     * @return a new {@code BooleanMatrix} with primitive boolean values
     * @throws NullPointerException if {@code x} is {@code null}
     * @see #boxed()
     */
    public static BooleanMatrix unbox(final Matrix<Boolean> x) {
        return BooleanMatrix.of(Array.unbox(x.a));
    }

    /**
     * Returns the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * boolean value = matrix.get(0, 1);   // Returns false
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return the boolean element at position {@code (rowIndex, columnIndex)}
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public boolean get(final int rowIndex, final int columnIndex) { // NOSONAR
        return a[rowIndex][columnIndex];
    }

    /**
     * Returns the element at the specified point.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * Sheet.Point point = Sheet.Point.of(0, 1);
     * boolean value = matrix.get(point);   // Returns false
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be null)
     * @return the boolean element at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #get(int, int)
     */
    public boolean get(final Point point) { // NOSONAR
        N.checkArgNotNull(point, "point");

        return a[point.rowIndex()][point.columnIndex()];
    }

    /**
     * Sets the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.set(0, 1, true);   // Sets element at row 0, column 1 to true
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @param value the value to set
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public void set(final int rowIndex, final int columnIndex, final boolean value) {
        a[rowIndex][columnIndex] = value;
    }

    /**
     * Sets the element at the specified point to the given value.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * Sheet.Point point = Sheet.Point.of(1, 0);
     * matrix.set(point, true);
     * assert matrix.get(point) == true;
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be null)
     * @param value the new boolean value to set at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #set(int, int, boolean)
     */
    public void set(final Point point, final boolean value) {
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * OptionalBoolean value = matrix.valueAbove(1, 0);   // Returns OptionalBoolean.of(true)
     * OptionalBoolean empty = matrix.valueAbove(0, 0);   // Returns OptionalBoolean.empty() - no row above
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an {@link OptionalBoolean} containing the element at position {@code (rowIndex - 1, columnIndex)},
     *         or empty if {@code rowIndex == 0}
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalBoolean valueAbove(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? OptionalBoolean.empty() : OptionalBoolean.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, if it exists.
     * This method provides safe access to the element directly below the given position
     * without throwing an exception when at the bottom edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * OptionalBoolean value = matrix.valueBelow(0, 0);   // Returns OptionalBoolean.of(false)
     * OptionalBoolean empty = matrix.valueBelow(1, 0);   // Returns OptionalBoolean.empty() - no row below
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an {@link OptionalBoolean} containing the element at position {@code (rowIndex + 1, columnIndex)},
     *         or empty if {@code rowIndex == rowCount - 1}
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalBoolean valueBelow(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? OptionalBoolean.empty() : OptionalBoolean.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, if it exists.
     * This method provides safe access to the element directly to the left of the given position
     * without throwing an exception when at the leftmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * OptionalBoolean value = matrix.valueLeft(0, 1);   // Returns OptionalBoolean.of(true)
     * OptionalBoolean empty = matrix.valueLeft(0, 0);   // Returns OptionalBoolean.empty() - no column to the left
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an {@link OptionalBoolean} containing the element at position {@code (rowIndex, columnIndex - 1)},
     *         or empty if {@code columnIndex == 0}
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalBoolean valueLeft(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? OptionalBoolean.empty() : OptionalBoolean.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, if it exists.
     * This method provides safe access to the element directly to the right of the given position
     * without throwing an exception when at the rightmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * OptionalBoolean value = matrix.valueRight(0, 0);   // Returns OptionalBoolean.of(false)
     * OptionalBoolean empty = matrix.valueRight(0, 1);   // Returns OptionalBoolean.empty() - no column to the right
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an {@link OptionalBoolean} containing the element at position {@code (rowIndex, columnIndex + 1)},
     *         or empty if {@code columnIndex == columnCount - 1}
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalBoolean valueRight(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? OptionalBoolean.empty() : OptionalBoolean.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as a boolean array.
     *
     * <p><b>Note:</b> This method returns a reference to the internal array, not a copy.
     * Modifications to the returned array will affect the matrix. If you need an independent
     * copy, use {@link #rowCopy(int)} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {false, true, false}});
     * boolean[] firstRow = matrix.rowView(0);   // Returns [true, false, false]
     *
     * // Direct modification affects the matrix
     * firstRow[0] = false;  // matrix now has false at position (0,0)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return the specified row array (direct reference to internal storage)
     * @throws IllegalArgumentException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    @Override
    public boolean[] rowView(final int rowIndex) throws IllegalArgumentException {
        N.checkArgument(rowIndex >= 0 && rowIndex < rowCount, MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount);

        return a[rowIndex];
    }

    /**
     * Returns a defensive copy of the specified row.
     * Changes to the returned array do not affect this matrix and vice versa.
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new boolean array containing the values from the specified row
     * @throws IllegalArgumentException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    @Override
    public boolean[] rowCopy(final int rowIndex) throws IllegalArgumentException {
        N.checkArgument(rowIndex >= 0 && rowIndex < rowCount, MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount);

        return N.copyOf(a[rowIndex], columnCount);
    }

    /**
     * Returns a copy of the specified column as a new boolean array.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously in memory. Modifications to the returned array
     * will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {false, true, false}});
     * boolean[] firstColumn = matrix.columnCopy(0);   // Returns [true, false]
     *
     * // Modification does NOT affect the matrix (it's a copy)
     * firstColumn[0] = false;  // matrix still has true at position (0,0)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new boolean array containing the values from the specified column
     * @throws IllegalArgumentException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    @Override
    public boolean[] columnCopy(final int columnIndex) throws IllegalArgumentException {
        N.checkArgument(columnIndex >= 0 && columnIndex < columnCount, MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount);

        final boolean[] c = new boolean[rowCount];

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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {false, true, false}});
     * matrix.setRow(0, new boolean[] {false, false, false});   // First row is now [false, false, false]
     * }</pre>
     *
     * @param rowIndex the index of the row to set (0-based)
     * @param row the array of values to copy into the row; must have length equal to the number of columns
     * @throws NullPointerException if {@code row} is {@code null}
     * @throws IllegalArgumentException if {@code rowIndex} is out of bounds, or {@code row.length} does not match the column count
     */
    public void setRow(final int rowIndex, final boolean[] row) throws IllegalArgumentException {
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {false, true, false}});
     * matrix.setColumn(0, new boolean[] {false, false});   // First column is now [false, false]
     * }</pre>
     *
     * @param columnIndex the index of the column to set (0-based)
     * @param column the array of values to copy into the column; must have length equal to the number of rows
     * @throws NullPointerException if {@code column} is {@code null}
     * @throws IllegalArgumentException if {@code columnIndex} is out of bounds, or {@code column.length} does not match the row count
     */
    public void setColumn(final int columnIndex, final boolean[] column) throws IllegalArgumentException {
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
     * from left to right (column {@code 0} to column {@code columnCount - 1}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, true}, {false, true, false}});
     * matrix.updateRow(0, val -> !val);   // Inverts all values in row 0
     * // Row 0 is now [false, true, false]
     *
     * // Set all to true
     * matrix.updateRow(1, val -> true);
     * // Row 1 is now [true, true, true]
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
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.BooleanUnaryOperator<E> operator) throws E {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new ArrayIndexOutOfBoundsException(formatMsg(MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount));
        }

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < columnCount; i++) {
            a[rowIndex][i] = operator.applyAsBoolean(a[rowIndex][i]);
        }
    }

    /**
     * Updates all elements in the specified column in-place by applying the specified operator to each element.
     * This modifies the matrix directly.
     *
     * <p>The operator is applied to each element in the specified column sequentially
     * from top to bottom (row {@code 0} to row {@code rowCount - 1}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, true}, {false, true, false}});
     * matrix.updateColumn(1, val -> !val);   // Inverts all values in column 1
     * // Column 1 is now [true, false]
     *
     * // Set all to false
     * matrix.updateColumn(0, val -> false);
     * // Column 0 is now [false, false]
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
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.BooleanUnaryOperator<E> operator) throws E {
        if (columnIndex < 0 || columnIndex >= columnCount) {
            throw new ArrayIndexOutOfBoundsException(formatMsg(MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount));
        }

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = operator.applyAsBoolean(a[i][columnIndex]);
        }
    }

    /**
     * Returns a copy of the main diagonal elements (upper-left to lower-right).
     * The matrix must be square ({@code rowCount == columnCount}) for this operation.
     *
     * <p>This method extracts the main diagonal elements at positions {@code (0,0), (1,1), (2,2), ...}.
     * The returned array is a copy; modifications to it will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, false},
     *     {false, true, false},
     *     {false, false, true}
     * });
     * boolean[] diagonal = matrix.getMainDiagonal();   // Returns [true, true, true]
     * }</pre>
     *
     * @return a new boolean array containing a copy of the main diagonal elements
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     */
    @Override
    public boolean[] getMainDiagonal() throws IllegalStateException {
        checkIsSquare();

        final boolean[] res = new boolean[rowCount];

        for (int i = 0; i < rowCount; i++) {
            res[i] = a[i][i]; // NOSONAR
        }

        return res;
    }

    /**
     * Sets the elements on the main diagonal (upper-left to lower-right).
     * The matrix must be square ({@code rowCount == columnCount}), and the diagonal array must have
     * exactly as many elements as the matrix has rows.
     *
     * <p>This method sets the main diagonal elements at positions {@code (0,0), (1,1), (2,2), ...}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, false},
     *     {false, true, false},
     *     {false, false, true}
     * });
     * matrix.setMainDiagonal(new boolean[] {false, false, false});
     * // Diagonal is now all false
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must have length equal to {@code rowCount}
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     * @throws IllegalArgumentException if {@code mainDiagonal} array length does not equal {@code rowCount}
     */
    @Override
    public void setMainDiagonal(final boolean[] mainDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgument(N.len(mainDiagonal) == rowCount, MSG_DIAGONAL_LENGTH_MISMATCH, rowCount, N.len(mainDiagonal));

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = mainDiagonal[i];
        }
    }

    /**
     * Updates the values on the main diagonal (upper-left to lower-right) by applying the specified operator.
     * The matrix must be square ({@code rowCount == columnCount}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, false},
     *     {false, true, false},
     *     {false, false, false}
     * });
     * matrix.updateMainDiagonal(val -> !val);   // Invert diagonal
     * // Diagonal is now [false, false, true]
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each diagonal element; receives the current element value and returns the new value
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateMainDiagonal(final Throwables.BooleanUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = operator.applyAsBoolean(a[i][i]);
        }
    }

    /**
     * Returns a copy of the elements on the anti-diagonal (upper-right to lower-left).
     * The matrix must be square ({@code rowCount == columnCount}) for this operation.
     *
     * <p>This method extracts the anti-diagonal (secondary diagonal) elements from
     * top-right to bottom-left, at positions {@code (0,n-1), (1,n-2), (2,n-3), ...}.
     * The returned array is a copy; modifications to it will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, true},
     *     {false, true, false},
     *     {true, false, false}
     * });
     * boolean[] antiDiag = matrix.getAntiDiagonal();   // Returns [true, true, true]
     * }</pre>
     *
     * @return a new boolean array containing a copy of the anti-diagonal elements
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     */
    @Override
    public boolean[] getAntiDiagonal() throws IllegalStateException {
        checkIsSquare();

        final boolean[] res = new boolean[rowCount];

        for (int i = 0; i < rowCount; i++) {
            res[i] = a[i][columnCount - i - 1];
        }

        return res;
    }

    /**
     * Sets the elements on the anti-diagonal (upper-right to lower-left).
     * The matrix must be square ({@code rowCount == columnCount}), and the diagonal array must have
     * exactly as many elements as the matrix has rows.
     *
     * <p>This method sets the anti-diagonal (secondary diagonal) elements from
     * top-right to bottom-left, at positions {@code (0,n-1), (1,n-2), (2,n-3), ...}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, false},
     *     {false, true, false},
     *     {false, false, true}
     * });
     * matrix.setAntiDiagonal(new boolean[] {true, true, true});
     * // Anti-diagonal is now all true
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must have length equal to {@code rowCount}
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     * @throws IllegalArgumentException if {@code antiDiagonal} array length does not equal {@code rowCount}
     */
    @Override
    public void setAntiDiagonal(final boolean[] antiDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgument(N.len(antiDiagonal) == rowCount, MSG_DIAGONAL_LENGTH_MISMATCH, rowCount, N.len(antiDiagonal));

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = antiDiagonal[i];
        }
    }

    /**
     * Updates the values on the anti-diagonal (upper-right to lower-left) by applying the specified operator.
     * The matrix must be square ({@code rowCount == columnCount}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, true},
     *     {false, true, false},
     *     {true, false, false}
     * });
     * matrix.updateAntiDiagonal(val -> !val);   // Invert anti-diagonal
     * // Anti-diagonal is now [false, false, false]
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each anti-diagonal element; receives the current element value and returns the new value
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAntiDiagonal(final Throwables.BooleanUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = operator.applyAsBoolean(a[i][columnCount - i - 1]);
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.updateAll(val -> !val);   // Inverts all values in the matrix
     * // Matrix is now [[false, true], [true, false]]
     *
     * // Set all to true
     * matrix.updateAll(val -> true);
     * // Matrix is now [[true, true], [true, true]]
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each element; receives the current element value
     *             and returns the new value
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.BooleanUnaryOperator<E> operator) throws E {
        N.checkArgNotNull(operator, "operator");
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = operator.applyAsBoolean(a[i][j]);
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[3][3]);
     * matrix.updateAll((i, j) -> i == j);   // Sets main diagonal to true, others to false
     * // Matrix is now [[true, false, false], [false, true, false], [false, false, true]]
     *
     * // Create a checkerboard pattern
     * matrix.updateAll((i, j) -> (i + j) % 2 == 0);
     * // Matrix is now [[true, false, true], [false, true, false], [true, false, true]]
     * }</pre>
     *
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the function that receives row index and column index (0-based) and returns
     *             the new value for that position
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the mapper throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Boolean, E> mapper) throws E {
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.replaceIf(val -> val == false, true);   // Replace all false values with true
     * // Matrix is now [[true, true], [true, true]]
     *
     * // Replace all true values with false
     * BooleanMatrix matrix2 = BooleanMatrix.of(new boolean[][] {{true, false}, {true, false}});
     * matrix2.replaceIf(val -> val, false);
     * // Matrix2 is now [[false, false], [false, false]]
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to test each element; elements for which this returns
     *                  {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.BooleanPredicate<E> predicate, final boolean newValue) throws E {
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[3][3]);
     * matrix.replaceIf((i, j) -> i == j, true);   // Set main diagonal to true
     * // Matrix is now [[true, false, false], [false, true, false], [false, false, true]]
     *
     * // Set upper triangle to true
     * matrix.replaceIf((i, j) -> i < j, true);
     * // Matrix is now [[true, true, true], [false, true, true], [false, false, true]]
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition that tests row index and column index (0-based); elements
     *                  at positions for which this returns {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final boolean newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = predicate.test(i, j) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));
    }

    /**
     * Creates a new BooleanMatrix by applying a transformation function to each element.
     * The original matrix is not modified; a new matrix with transformed values is returned.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
     * This is the immutable counterpart to {@link #updateAll(Throwables.BooleanUnaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * BooleanMatrix inverted = matrix.map(val -> !val);   // Creates new matrix with inverted values
     * // inverted is [[false, true], [true, false]]
     * // original matrix remains [[true, false], [false, true]]
     *
     * // Set all to false in a new matrix
     * BooleanMatrix allFalse = matrix.map(val -> false);
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to apply to each element; receives the current element value
     *             and returns the transformed value
     * @return a new {@code BooleanMatrix} with transformed values
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     * @see #updateAll(Throwables.BooleanUnaryOperator)
     */
    public <E extends Exception> BooleanMatrix map(final Throwables.BooleanUnaryOperator<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final boolean[][] result = new boolean[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsBoolean(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return BooleanMatrix.of(result);
    }

    /**
     * Creates a new Matrix by applying a function that converts boolean values to objects of type R.
     * This operation may be executed in parallel for better performance on large matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     *
     * // Convert to String matrix
     * Matrix<String> stringMatrix = matrix.mapToObj(val -> val ? "YES" : "NO", String.class);
     * // Result: [["YES", "NO"], ["NO", "YES"]]
     *
     * // Convert to Integer matrix (0/1)
     * Matrix<Integer> intMatrix = matrix.mapToObj(val -> val ? 1 : 0, Integer.class);
     * // Result: [[1, 0], [0, 1]]
     * }</pre>
     *
     * @param <R> the type of elements in the resulting matrix
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to convert boolean values to type {@code R}
     * @param targetElementType the {@code Class} object for type {@code R}; used to create the result's backing array
     * @return a new {@code Matrix<R>} containing the converted values
     * @throws IllegalArgumentException if {@code mapper} or {@code targetElementType} is {@code null}
     * @throws E if the function throws an exception
     */
    public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.BooleanFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.apply(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return Matrix.of(result);
    }

    /**
     * Fills all elements in the matrix with the specified value.
     * This method modifies the matrix in-place.
     *
     * <p>This is a fast operation that sets every element in the matrix to the same value,
     * effectively creating a uniform matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.fill(true);   // Sets all elements to true
     * // Matrix is now [[true, true], [true, true]]
     *
     * matrix.fill(false);   // Sets all elements to false
     * // Matrix is now [[false, false], [false, false]]
     * }</pre>
     *
     * @param value the boolean value to fill the matrix with
     */
    public void fill(final boolean value) {
        for (int i = 0; i < rowCount; i++) {
            N.fill(a[i], value);
        }
    }

    /**
     * Fills the matrix with values from the provided two-dimensional array, starting from position (0, 0).
     * The copy continues for the size of the input array or until the matrix boundaries are reached.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[3][3]);   // 3x3 of false
     * matrix.fill(new boolean[][] {{true, true}, {true, true}});
     * // Top-left 2x2 region is now true, rest remains false
     * }</pre>
     *
     * @param source the two-dimensional boolean array to copy values from; must not be null
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final boolean[][] source) {
        fill(0, 0, source);
    }

    /**
     * Fills a portion of the matrix with values from the provided two-dimensional array.
     * Copying starts at the specified position and continues for the size of the input array
     * or until the matrix boundaries are reached. If the input array extends beyond the matrix
     * boundaries, only the overlapping portion is copied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[4][4]);   // 4x4 of false
     * matrix.fill(1, 1, new boolean[][] {{true, true}, {true, true}});
     * // 2x2 region starting at (1,1) is now true, rest remains false
     * }</pre>
     *
     * @param destRowIndex the target row index in this matrix (0-based)
     * @param destColumnIndex the target column index in this matrix (0-based)
     * @param source the source array to copy values from; must not be null
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if the target indices are negative or exceed matrix dimensions
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final boolean[][] source) throws IllegalArgumentException {
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
     * BooleanMatrix original = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * BooleanMatrix copy = original.copy();
     *
     * // Modifying the copy does NOT affect the original
     * copy.set(0, 0, false);
     * assert original.get(0, 0) == true;   // Original unchanged
     * assert copy.get(0, 0)     == false;  // Copy modified
     * }</pre>
     *
     * @return a new matrix that is a copy of this matrix with full independence guarantee
     */
    @Override
    public BooleanMatrix copy() {
        final boolean[][] c = new boolean[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[i].clone();
        }

        return new BooleanMatrix(c);
    }

    /**
     * Creates a copy of a row range from this matrix.
     * The returned matrix contains only the specified rows and is completely independent from the original matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false},
     *     {false, true},
     *     {true, true}
     * });
     * BooleanMatrix subset = matrix.copy(1, 3);   // Rows 1 and 2
     * // Result: [[false, true], [true, true]]
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new {@code BooleanMatrix} containing the specified rows
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public BooleanMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        final boolean[][] c = new boolean[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = a[i].clone();
        }

        return new BooleanMatrix(c);
    }

    /**
     * Creates a copy of a rectangular region from this matrix.
     * The returned matrix contains only the elements in the specified row and column range,
     * preserving their relative positions from the original matrix.
     *
     * <p>This method is useful for extracting sub-matrices or working with specific regions
     * of a larger matrix. The copy is independent of the original matrix - modifications to
     * either will not affect the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, true, false},
     *     {false, true, false, true},
     *     {true, true, false, false}
     * });
     * BooleanMatrix subMatrix = matrix.copy(0, 2, 1, 3);   // Copy rows 0-1, columns 1-2
     * // Result: [[false, true], [true, false]]
     *
     * // Extract a single column as a matrix
     * BooleanMatrix col = matrix.copy(0, 3, 2, 3);   // All rows, column 2 only
     * // Result: [[true], [false], [false]]
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new {@code BooleanMatrix} containing the specified rectangular region
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         {@code fromRowIndex > toRowIndex}, or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    public BooleanMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        final boolean[][] c = new boolean[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = N.copyOfRange(a[i], fromColumnIndex, toColumnIndex);
        }

        return new BooleanMatrix(c);
    }

    /**
     * Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount},
     * anchored at the top-left corner of this matrix. New cells are filled with {@code false}.
     *
     * <ul>
     *   <li><b>If a dimension shrinks</b> — elements beyond the new boundary are discarded
     *       (excess rows removed from the bottom, excess columns removed from the right).</li>
     *   <li><b>If a dimension grows</b> — new cells are filled with {@code false}.</li>
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true,  false, true},
     *     {false, true,  false},
     *     {true,  false, true}
     * });
     *
     * // Grow: both dimensions larger — new cells filled with false
     * BooleanMatrix grown = matrix.resize(4, 4);
     * // Result: [[true,  false, true,  false],
     * //          [false, true,  false, false],
     * //          [true,  false, true,  false],
     * //          [false, false, false, false]]
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * BooleanMatrix truncated = matrix.resize(2, 2);
     * // Result: [[true,  false],
     * //          [false, true]]
     *
     * // Mixed: grow rows, truncate columns
     * BooleanMatrix mixed = matrix.resize(4, 2);
     * // Result: [[true,  false],
     * //          [false, true],
     * //          [true,  false],
     * //          [false, false]]
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new {@code BooleanMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int, boolean)
     * @see #extend(int, int, int, int)
     */
    public BooleanMatrix resize(final int newRowCount, final int newColumnCount) {
        return resize(newRowCount, newColumnCount, false);
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
     * <p><b>Comparison with {@link #extend(int, int, int, int, boolean)}:</b>
     * {@code resize} takes <em>absolute</em> target dimensions and may truncate existing content.
     * {@code extend} takes <em>relative</em> padding amounts per edge and <em>never truncates</em>.
     * Use {@code extend} when the entire original content must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true,  false, true},
     *     {false, true,  false},
     *     {true,  false, true}
     * });
     *
     * // Grow: fill new cells with true
     * BooleanMatrix grown = matrix.resize(4, 4, true);
     * // Result: [[true,  false, true,  true],
     * //          [false, true,  false, true],
     * //          [true,  false, true,  true],
     * //          [true,  true,  true,  true]]
     *
     * // Truncate: defaultValue is ignored when shrinking
     * BooleanMatrix truncated = matrix.resize(2, 2, true);
     * // Result: [[true,  false],
     * //          [false, true]]
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the value used to fill cells that are added when a dimension grows;
     *        ignored when a dimension shrinks
     * @return a new {@code BooleanMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int)
     * @see #extend(int, int, int, int, boolean)
     */
    public BooleanMatrix resize(final int newRowCount, final int newColumnCount, final boolean defaultValue) throws IllegalArgumentException {
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
            // NOSONAR
            final boolean[][] b = new boolean[newRowCount][];

            for (int i = 0; i < newRowCount; i++) {
                b[i] = i < rowCount ? N.copyOf(a[i], newColumnCount) : new boolean[newColumnCount];

                if (defaultValue) {
                    if (i >= rowCount) {
                        N.fill(b[i], true);
                    } else if (columnCount < newColumnCount) {
                        N.fill(b[i], columnCount, newColumnCount, true);
                    }
                }
            }

            return new BooleanMatrix(b);
        }
    }

    /**
     * Returns a new matrix formed by surrounding this matrix with padding on all four edges.
     * New cells are filled with {@code false}.
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, true}, {true, true}});
     *
     * // Uniform 1-cell border of false
     * BooleanMatrix bordered = matrix.extend(1, 1, 1, 1);
     * // Result: [[false, false, false, false],
     * //          [false, true,  true,  false],
     * //          [false, true,  true,  false],
     * //          [false, false, false, false]]
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * BooleanMatrix shifted = matrix.extend(0, 0, 2, 0);
     * // Result: [[false, false, true, true],
     * //          [false, false, true, true]]
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @return a new {@code BooleanMatrix} with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any parameter is negative,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #extend(int, int, int, int, boolean)
     * @see #resize(int, int)
     */
    @Override
    public BooleanMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight) {
        return extend(padTop, padBottom, padLeft, padRight, false);
    }

    /**
     * Returns a new matrix formed by surrounding this matrix with padding on all four edges.
     * New cells are filled with {@code defaultValue}.
     *
     * <p>Unlike {@link #resize(int, int, boolean)}, this method <b>never truncates</b>: the entire
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
     * a data region, marking regions with sentinel values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, true}, {true, true}});
     *
     * // Uniform 1-cell border filled with false
     * BooleanMatrix bordered = matrix.extend(1, 1, 1, 1, false);
     * // Result: [[false, false, false, false],
     * //          [false, true,  true,  false],
     * //          [false, true,  true,  false],
     * //          [false, false, false, false]]
     *
     * // Asymmetric: 2 columns on the left only, no row padding, fill with true
     * BooleanMatrix shifted = matrix.extend(0, 0, 2, 0, true);
     * // Result: [[true, true, true, true],
     * //          [true, true, true, true]]
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @param defaultValue the value to fill all new padding cells with
     * @return a new {@code BooleanMatrix} with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #extend(int, int, int, int)
     * @see #resize(int, int, boolean)
     */
    public BooleanMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final boolean defaultValue)
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
            // NOSONAR
            final boolean[][] b = new boolean[newRowCount][newColumnCount];

            for (int i = 0; i < newRowCount; i++) {
                if (i >= padTop && i < padTop + rowCount) {
                    N.copy(a[i - padTop], 0, b[i], padLeft, columnCount);
                }

                if (defaultValue) {
                    if (i < padTop || i >= padTop + rowCount) {
                        N.fill(b[i], true);
                    } else if (columnCount < newColumnCount) {
                        if (padLeft > 0) {
                            N.fill(b[i], 0, padLeft, true);
                        }

                        if (padRight > 0) {
                            N.fill(b[i], columnCount + padLeft, newColumnCount, true);
                        }
                    }
                }
            }

            return new BooleanMatrix(b);
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, true, false}, {false, true, true}});
     * matrix.flipHorizontallyInPlace();
     * // matrix is now [[false, true, true], [true, true, false]]
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {true, true}, {false, true}});
     * matrix.flipVerticallyInPlace();
     * // matrix is now [[false, true], [true, true], [true, false]]
     * }</pre>
     *
     * @see #flipVertically()
     * @see #flipHorizontallyInPlace()
     */
    @Override
    public void flipVerticallyInPlace() {
        for (int l = 0, h = rowCount - 1; l < h; l++, h--) {
            final boolean[] tmp = a[l];
            a[l] = a[h];
            a[h] = tmp;
        }
    }

    /**
     * Creates a horizontally flipped copy of this matrix.
     * Each row is reversed left-to-right (the leftmost element becomes rightmost).
     * Creates a new matrix; the original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {true, true, false}});
     * BooleanMatrix flipped = matrix.flipHorizontally();
     * // flipped is: {{false, false, true}, {false, true, true}}
     * }</pre>
     *
     * @return a new {@code BooleanMatrix} with each row reversed
     * @see #flipHorizontallyInPlace()
     * @see #flipVertically()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public BooleanMatrix flipHorizontally() {
        final BooleanMatrix res = this.copy();
        res.flipHorizontallyInPlace();
        return res;
    }

    /**
     * Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
     * The rows are reversed top-to-bottom (the topmost row becomes bottommost).
     * Creates a new matrix; the original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {true, true, false}});
     * BooleanMatrix flipped = matrix.flipVertically();
     * // flipped is: {{true, true, false}, {true, false, false}}
     * }</pre>
     *
     * @return a new {@code BooleanMatrix} with rows reversed
     * @see #flipVerticallyInPlace()
     * @see #flipHorizontally()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public BooleanMatrix flipVertically() {
        final BooleanMatrix res = this.copy();
        res.flipVerticallyInPlace();
        return res;
    }

    /**
     * Returns a new matrix that is this matrix rotated 90 degrees clockwise.
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * column of the result being the last row of the original, reading upward.
     * Creates a new matrix; the original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:             Rotated 90 clockwise:
     * // true  false false     true  true
     * // true  true  false     true  false
     * //                       false false
     * }</pre>
     *
     * @return a new matrix rotated 90 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         or an empty matrix if this matrix has zero columns
     */
    @Override
    public BooleanMatrix rotate90() {
        if (columnCount == 0) {
            return EMPTY_BOOLEAN_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final boolean[][] c = new boolean[columnCount][rowCount];

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

        return new BooleanMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 180 degrees clockwise.
     * This is equivalent to flipping both horizontally and vertically, reversing the
     * order of all elements. The resulting matrix has the same dimensions as the original.
     * Creates a new matrix; the original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:             Rotated 180:
     * // true  false false     false true  true
     * // true  true  false     false false true
     * }</pre>
     *
     * @return a new matrix rotated 180 degrees clockwise
     */
    @Override
    public BooleanMatrix rotate180() {
        final boolean[][] c = new boolean[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[rowCount - i - 1].clone();
            N.reverse(c[i]);
        }

        return new BooleanMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 270 degrees clockwise (or 90 degrees counter-clockwise).
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * column of the result being the first row of the original, reading downward.
     * Creates a new matrix; the original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:             Rotated 270 clockwise:
     * // true  false false     false false
     * // true  true  false     false true
     * //                       true  true
     * }</pre>
     *
     * @return a new matrix rotated 270 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         or an empty matrix if this matrix has zero columns
     */
    @Override
    public BooleanMatrix rotate270() {
        if (columnCount == 0) {
            return EMPTY_BOOLEAN_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final boolean[][] c = new boolean[columnCount][rowCount];

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

        return new BooleanMatrix(c);
    }

    /**
     * Creates the transpose of this matrix by swapping rows and columns.
     * The transpose operation converts each row into a column, so element at position (i, j)
     * in the original matrix appears at position (j, i) in the transposed matrix. The resulting
     * matrix has dimensions swapped (rowCount x columnCount becomes columnCount x rowCount).
     * Creates a new matrix; the original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:        Transposed:
     * // true false false true  false
     * // false true false false true
     * //                  false false
     *
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {false, true, false}});
     * BooleanMatrix transposed = matrix.transpose();   // 2×3 becomes 3×2
     * }</pre>
     *
     * @return a new matrix that is the transpose of this matrix with dimensions {@code columnCount × rowCount};
     *         an {@code N x 0} matrix transposes to the empty {@code 0 x 0} matrix, because the swapped shape
     *         {@code 0 x N} (zero rows with a non-zero column count) is not representable
     */
    @Override
    public BooleanMatrix transpose() {
        if (columnCount == 0) {
            return EMPTY_BOOLEAN_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final boolean[][] c = new boolean[columnCount][rowCount];

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

        return new BooleanMatrix(c);
    }

    /**
     * Reshapes this matrix to the specified dimensions.
     * Elements are read from the original matrix in row-major order (row by row, left to right)
     * and placed into the new matrix shape in the same order. The new shape must have at least
     * as many total elements as the original ({@code (long) newRowCount * newColumnCount >= elementCount()}).
     *
     * <p>If the new shape has greater capacity than the number of source elements, trailing positions
     * in the result are left as {@code false}. The original matrix is not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, true, false}});
     * BooleanMatrix reshaped = matrix.reshape(2, 2);
     * // Result: [[true, false],
     * //          [true, false]]
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix; must be non-negative
     * @param newColumnCount the number of columns in the reshaped matrix; must be non-negative
     * @return a new {@code BooleanMatrix} with the specified shape
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         or if the new shape is too small to hold all elements of this matrix
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public BooleanMatrix reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final boolean[][] c = new boolean[newRowCount][newColumnCount];

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return new BooleanMatrix(c);
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

        return new BooleanMatrix(c);
    }

    /**
     * Repeats each element in the matrix the specified number of times in both dimensions.
     * Each element is expanded into a {@code rowRepeats × columnRepeats} block.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // [[true, false]] with repeatElements(2, 3) becomes:
     * // [[true, true, true, false, false, false],
     * //  [true, true, true, false, false, false]]
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}});
     * BooleanMatrix repeated = matrix.repeatElements(2, 3);
     * }</pre>
     *
     * @param rowRepeats number of times to repeat each element vertically; must be {@code > 0}
     * @param columnRepeats number of times to repeat each element horizontally; must be {@code > 0}
     * @return a new {@code BooleanMatrix} with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
     * @see #repeatMatrix(int, int)
     */
    @Override
    public BooleanMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final boolean[][] c = new boolean[rowCount * rowRepeats][columnCount * columnRepeats];

        for (int i = 0; i < rowCount; i++) {
            final boolean[] aa = a[i];
            final boolean[] fr = c[i * rowRepeats];

            for (int j = 0; j < columnCount; j++) {
                N.copy(Array.repeat(aa[j], columnRepeats), 0, fr, j * columnRepeats, columnRepeats);
            }

            for (int k = 1; k < rowRepeats; k++) {
                N.copy(fr, 0, c[i * rowRepeats + k], 0, fr.length);
            }
        }

        return new BooleanMatrix(c);
    }

    /**
     * Repeats the entire matrix the specified number of times in both dimensions.
     * The matrix is tiled {@code rowRepeats} times vertically and {@code columnRepeats} times horizontally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // [[true, false]] with repeatMatrix(2, 3) becomes:
     * // [[true, false, true, false, true, false],
     * //  [true, false, true, false, true, false]]
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}});
     * BooleanMatrix tiled = matrix.repeatMatrix(2, 3);
     * }</pre>
     *
     * @param rowRepeats number of times to repeat the matrix vertically; must be {@code > 0}
     * @param columnRepeats number of times to repeat the matrix horizontally; must be {@code > 0}
     * @return a new {@code BooleanMatrix} with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
     * @see #repeatElements(int, int)
     */
    @Override
    public BooleanMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final boolean[][] c = new boolean[rowCount * rowRepeats][columnCount * columnRepeats];

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

        return new BooleanMatrix(c);
    }

    /**
     * Returns a {@link BooleanList} containing all matrix elements in row-major order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * BooleanList list = matrix.flatten();   // Returns BooleanList of [true, false, false, true]
     * }</pre>
     *
     * @return a new {@code BooleanList} of all elements in row-major order
     * @throws IllegalStateException if the matrix is too large to flatten
     *         (i.e. {@code (long) rowCount * columnCount > Integer.MAX_VALUE})
     */
    @Override
    public BooleanList flatten() {
        // Check for overflow before allocation
        if ((long) rowCount * columnCount > Integer.MAX_VALUE) {
            throw new IllegalStateException("Matrix too large to flatten: " + rowCount + " x " + columnCount);
        }

        final boolean[] c = new boolean[rowCount * columnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c, i * columnCount, columnCount);
        }

        return BooleanList.of(c);
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.mutateAsFlat(arr -> java.util.Arrays.fill(arr, true));
     * // matrix is now [[true, true], [true, true]] (all elements set globally, then placed back row by row)
     * }</pre>
     *
     * @param <E> the type of exception that the operation may throw
     * @param action the operation to apply to the flattened array; must not be {@code null}
     * @throws E if the operation throws an exception
     * @see Arrays#mutateAsFlat(boolean[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super boolean[], E> action) throws E {
        Arrays.mutateAsFlat(a, action);
    }

    /**
     * Performs element-wise logical AND of this matrix with another matrix.
     * The matrices must have the same dimensions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix a = BooleanMatrix.of(new boolean[][] {{true, false}, {true, true}});
     * BooleanMatrix b = BooleanMatrix.of(new boolean[][] {{true, true}, {false, true}});
     * BooleanMatrix result = a.and(b);   // Result: [[true, false], [false, true]]
     * }</pre>
     *
     * @param other the matrix to AND with this matrix
     * @return a new {@code BooleanMatrix} containing the element-wise logical AND
     * @throws IllegalArgumentException if {@code other} is {@code null} or the matrices have different dimensions
     */
    public BooleanMatrix and(final BooleanMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot AND matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final boolean[][] otherData = other.a;
        final boolean[][] result = new boolean[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = a[i][j] && otherData[i][j];

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return BooleanMatrix.of(result);
    }

    /**
     * Performs element-wise logical OR of this matrix with another matrix.
     * The matrices must have the same dimensions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix a = BooleanMatrix.of(new boolean[][] {{true, false}, {false, false}});
     * BooleanMatrix b = BooleanMatrix.of(new boolean[][] {{false, true}, {false, true}});
     * BooleanMatrix result = a.or(b);   // Result: [[true, true], [false, true]]
     * }</pre>
     *
     * @param other the matrix to OR with this matrix
     * @return a new {@code BooleanMatrix} containing the element-wise logical OR
     * @throws IllegalArgumentException if {@code other} is {@code null} or the matrices have different dimensions
     */
    public BooleanMatrix or(final BooleanMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot OR matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final boolean[][] otherData = other.a;
        final boolean[][] result = new boolean[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = a[i][j] || otherData[i][j];

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return BooleanMatrix.of(result);
    }

    /**
     * Performs element-wise logical XOR of this matrix with another matrix.
     * The matrices must have the same dimensions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix a = BooleanMatrix.of(new boolean[][] {{true, false}, {true, true}});
     * BooleanMatrix b = BooleanMatrix.of(new boolean[][] {{true, true}, {false, true}});
     * BooleanMatrix result = a.xor(b);   // Result: [[false, true], [true, false]]
     * }</pre>
     *
     * @param other the matrix to XOR with this matrix
     * @return a new {@code BooleanMatrix} containing the element-wise logical XOR
     * @throws IllegalArgumentException if {@code other} is {@code null} or the matrices have different dimensions
     */
    public BooleanMatrix xor(final BooleanMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot XOR matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final boolean[][] otherData = other.a;
        final boolean[][] result = new boolean[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = a[i][j] ^ otherData[i][j];

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return BooleanMatrix.of(result);
    }

    /**
     * Counts the number of {@code true} elements in this matrix.
     *
     * <p>The result is a {@code long} because a matrix can contain up to
     * {@code rowCount * columnCount} elements, a product that may exceed
     * {@link Integer#MAX_VALUE}. An {@code int} accumulator would silently
     * overflow for sufficiently large matrices.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, true}, {false, true, false}});
     * long count = matrix.countTrue();   // Returns 3
     * }</pre>
     *
     * @return the number of {@code true} elements in this matrix, as a non-negative {@code long}
     */
    public long countTrue() {
        long count = 0;

        for (int i = 0; i < rowCount; i++) {
            final boolean[] row = a[i];

            for (int j = 0; j < columnCount; j++) {
                if (row[j]) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Returns {@code true} if all elements in this matrix are {@code true}.
     * Returns {@code true} for an empty matrix (vacuous truth).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix allTrue = BooleanMatrix.of(new boolean[][] {{true, true}, {true, true}});
     * allTrue.allTrue();   // Returns true
     *
     * BooleanMatrix mixed = BooleanMatrix.of(new boolean[][] {{true, false}, {true, true}});
     * mixed.allTrue();     // Returns false
     * }</pre>
     *
     * @return {@code true} if every element is {@code true}, or if the matrix is empty
     */
    public boolean allTrue() {
        for (int i = 0; i < rowCount; i++) {
            final boolean[] row = a[i];

            for (int j = 0; j < columnCount; j++) {
                if (!row[j]) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns {@code true} if any element in this matrix is {@code true}.
     * Returns {@code false} for an empty matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix allFalse = BooleanMatrix.of(new boolean[][] {{false, false}, {false, false}});
     * allFalse.anyTrue();   // Returns false
     *
     * BooleanMatrix mixed = BooleanMatrix.of(new boolean[][] {{false, true}, {false, false}});
     * mixed.anyTrue();      // Returns true
     * }</pre>
     *
     * @return {@code true} if at least one element is {@code true}
     */
    public boolean anyTrue() {
        for (int i = 0; i < rowCount; i++) {
            final boolean[] row = a[i];

            for (int j = 0; j < columnCount; j++) {
                if (row[j]) {
                    return true;
                }
            }
        }

        return false;
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
     * BooleanMatrix top = BooleanMatrix.of(new boolean[][] {{true, false}});
     * BooleanMatrix bottom = BooleanMatrix.of(new boolean[][] {{false, true}});
     * BooleanMatrix stacked = top.stackVertically(bottom);
     * // Result: [[true, false],
     * //          [false, true]]
     *
     * // Stack multiple matrices
     * BooleanMatrix m1 = BooleanMatrix.of(new boolean[][] {{true, true}});
     * BooleanMatrix m2 = BooleanMatrix.of(new boolean[][] {{false, false}});
     * BooleanMatrix m3 = BooleanMatrix.of(new boolean[][] {{true, false}});
     * BooleanMatrix combined = m1.stackVertically(m2).stackVertically(m3);   // 3x2 matrix
     * }</pre>
     *
     * @param other the matrix to stack below this matrix (must have the same column count)
     * @return a new {@code BooleanMatrix} with dimensions {@code (this.rowCount + other.rowCount) × this.columnCount}
     * @throws IllegalArgumentException if {@code other} is {@code null}, if
     *         {@code this.columnCount != other.columnCount}, or if the merged row count would
     *         overflow {@code Integer.MAX_VALUE}
     * @see #stackHorizontally(BooleanMatrix)
     */
    @Override
    public BooleanMatrix stackVertically(final BooleanMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.columnCount, MSG_VSTACK_COLUMN_MISMATCH, columnCount, other.columnCount);
        final long mergedRowCount = (long) rowCount + other.rowCount;
        N.checkArgument(mergedRowCount <= Integer.MAX_VALUE, "Merged row count overflow: {} + {} = {}", rowCount, other.rowCount, mergedRowCount);

        final boolean[][] c = new boolean[(int) mergedRowCount][];
        int j = 0;

        for (int i = 0; i < rowCount; i++) {
            c[j++] = a[i].clone();
        }

        for (int i = 0; i < other.rowCount; i++) {
            c[j++] = other.a[i].clone();
        }

        return BooleanMatrix.of(c);
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
     * BooleanMatrix left = BooleanMatrix.of(new boolean[][] {{true}, {false}});
     * BooleanMatrix right = BooleanMatrix.of(new boolean[][] {{false}, {true}});
     * BooleanMatrix stacked = left.stackHorizontally(right);
     * // Result: [[true, false],
     * //          [false, true]]
     *
     * // Create a wider matrix by stacking multiple columns
     * BooleanMatrix col1 = BooleanMatrix.of(new boolean[][] {{true}, {true}, {false}});
     * BooleanMatrix col2 = BooleanMatrix.of(new boolean[][] {{false}, {true}, {true}});
     * BooleanMatrix wide = col1.stackHorizontally(col2);   // 3x2 matrix
     * }</pre>
     *
     * @param other the matrix to stack to the right of this matrix (must have the same row count)
     * @return a new {@code BooleanMatrix} with dimensions {@code this.rowCount × (this.columnCount + other.columnCount)}
     * @throws IllegalArgumentException if {@code other} is {@code null}, if
     *         {@code this.rowCount != other.rowCount}, or if the merged column count would
     *         overflow {@code Integer.MAX_VALUE}
     * @see #stackVertically(BooleanMatrix)
     */
    @Override
    public BooleanMatrix stackHorizontally(final BooleanMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(rowCount == other.rowCount, MSG_HSTACK_ROW_MISMATCH, rowCount, other.rowCount);
        final long mergedColumnCount = (long) columnCount + other.columnCount;
        N.checkArgument(mergedColumnCount <= Integer.MAX_VALUE, "Merged column count overflow: {} + {} = {}", columnCount, other.columnCount,
                mergedColumnCount);

        final boolean[][] c = new boolean[rowCount][(int) mergedColumnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c[i], 0, columnCount);
            N.copy(other.a[i], 0, c[i], columnCount, other.columnCount);
        }

        return BooleanMatrix.of(c);
    }

    /**
     * Converts this primitive boolean matrix to a boxed Boolean Matrix.
     * Each boolean value is converted to its corresponding Boolean wrapper object.
     *
     * <p>This conversion is useful when you need to work with APIs that require
     * object types rather than primitives, or when you need null values in the matrix.
     * Note that boxing incurs memory overhead and may impact performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix primitive = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * Matrix<Boolean> boxed = primitive.boxed();
     *
     * // Now you can use methods that work with generic types
     * Stream<Boolean> stream = boxed.horizontalStream();
     * boxed.set(0, 0, null);   // Can use null values
     * }</pre>
     *
     * @return a new {@code Matrix<Boolean>} with the same dimensions and values as this matrix
     * @see #unbox(Matrix)
     */
    public Matrix<Boolean> boxed() {
        final Boolean[][] c = new Boolean[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final boolean[] aa = a[i];
                final Boolean[] cc = c[i];

                for (int j = 0; j < columnCount; j++) {
                    cc[j] = aa[j]; // NOSONAR
                }
            }
        } else {
            for (int j = 0; j < columnCount; j++) {
                for (int i = 0; i < rowCount; i++) {
                    c[i][j] = a[i][j]; // NOSONAR
                }
            }
        }

        return new Matrix<>(c);
    }

    /**
     * Performs element-wise operation on two matrices using a binary operator.
     * The matrices must have the same dimensions. Corresponding elements from both matrices
     * are combined using the provided function to produce the result matrix.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
     * Creates a new matrix; the original matrices are not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix a = BooleanMatrix.of(new boolean[][] {{true, false}, {true, true}});
     * BooleanMatrix b = BooleanMatrix.of(new boolean[][] {{true, true}, {false, true}});
     *
     * // Element-wise AND
     * BooleanMatrix and = a.zipWith(b, (x, y) -> x && y);
     * // Result: [[true, false], [false, true]]
     *
     * // Element-wise OR
     * BooleanMatrix or = a.zipWith(b, (x, y) -> x || y);
     * // Result: [[true, true], [true, true]]
     *
     * // Element-wise XOR
     * BooleanMatrix xor = a.zipWith(b, (x, y) -> x ^ y);
     * // Result: [[false, true], [true, false]]
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param zipFunction the binary operator to apply to corresponding elements; receives
     *                    element from this matrix as first argument and element from {@code other}
     *                    as second argument
     * @return a new {@code BooleanMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if the matrices have different dimensions (shape mismatch),
     *         or if {@code zipFunction} is {@code null}
     * @throws E if the zip function throws an exception
     * @see #zipWith(BooleanMatrix, BooleanMatrix, Throwables.BooleanTernaryOperator)
     */
    public <E extends Exception> BooleanMatrix zipWith(final BooleanMatrix other, final Throwables.BooleanBinaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);
        N.checkArgNotNull(zipFunction, "zipFunction");

        final boolean[][] otherData = other.a;
        final boolean[][] result = new boolean[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsBoolean(a[i][j], otherData[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return BooleanMatrix.of(result);
    }

    /**
     * Performs element-wise operation on three matrices using a ternary operator.
     * All matrices must have the same dimensions. Corresponding elements from all three matrices
     * are combined using the provided function to produce the result matrix.
     *
     * <p>This is useful for operations that combine three matrices, such as majority vote,
     * conditional selection, or mathematical formulas involving three variables.</p>
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
     * Creates a new matrix; the original matrices are not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix a = BooleanMatrix.of(new boolean[][] {{true, false}, {true, true}});
     * BooleanMatrix b = BooleanMatrix.of(new boolean[][] {{true, true}, {false, true}});
     * BooleanMatrix c = BooleanMatrix.of(new boolean[][] {{false, true}, {true, false}});
     *
     * // Majority vote: true if at least 2 out of 3 are true
     * BooleanMatrix majority = a.zipWith(b, c, (x, y, z) ->
     *     (x && y) || (x && z) || (y && z));
     *
     * // Conditional operation: if a then b else c
     * BooleanMatrix conditional = a.zipWith(b, c, (x, y, z) -> x ? y : z);
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param third the third matrix (must have the same dimensions as this matrix)
     * @param zipFunction the ternary operator to apply to corresponding elements; receives
     *                    element from this matrix as first argument, element from {@code other} as
     *                    second argument, and element from {@code third} as third argument
     * @return a new {@code BooleanMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if any of the matrices have different dimensions (shape mismatch),
     *         or if {@code zipFunction} is {@code null}
     * @throws E if the zip function throws an exception
     * @see #zipWith(BooleanMatrix, Throwables.BooleanBinaryOperator)
     */
    public <E extends Exception> BooleanMatrix zipWith(final BooleanMatrix other, final BooleanMatrix third,
            final Throwables.BooleanTernaryOperator<E> zipFunction) throws IllegalArgumentException, E {
        N.checkArgument(isSameShape(other) && isSameShape(third), "Cannot zip matrices with different shapes: all matrices must be {}x{}", rowCount,
                columnCount);
        N.checkArgNotNull(zipFunction, "zipFunction");

        final boolean[][] otherData = other.a;
        final boolean[][] thirdData = third.a;
        final boolean[][] result = new boolean[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsBoolean(a[i][j], otherData[i][j], thirdData[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return BooleanMatrix.of(result);
    }

    /**
     * Returns a stream of Boolean values from the main diagonal (upper-left to lower-right).
     * The matrix must be square (same number of rows and columns).
     *
     * <p>This method streams the diagonal elements starting from position (0,0) and
     * proceeding to position (n-1,n-1) where n is the dimension of the square matrix.
     * This is useful for operations on diagonal matrices or extracting diagonal elements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, false},
     *     {false, true, false},
     *     {false, false, true}
     * });
     * List<Boolean> diagonal = matrix.mainDiagonalStream().toList();   // [true, true, true]
     *
     * // Check if it's an identity-like matrix
     * boolean allTrue = matrix.mainDiagonalStream().allMatch(b -> b);
     * }</pre>
     *
     * @return a {@code Stream<Boolean>} containing the diagonal elements from top-left to bottom-right,
     *         or an empty stream if the matrix is empty (0 × 0)
     * @throws IllegalStateException if the matrix is non-empty and not square ({@code rowCount != columnCount})
     */
    @Override
    public Stream<Boolean> mainDiagonalStream() {
        if (isEmpty()) {
            return Stream.empty();
        }

        checkIsSquare();

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Boolean next() {
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
     * Returns a stream of Boolean values from the anti-diagonal (upper-right to lower-left).
     * The matrix must be square (same number of rows and columns).
     *
     * <p>This method streams the anti-diagonal elements starting from position (0,n-1)
     * and proceeding to position (n-1,0) where n is the dimension of the square matrix.
     * This is useful for operations involving the secondary diagonal of a matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, true},
     *     {false, true, false},
     *     {true, false, false}
     * });
     * List<Boolean> antiDiagonal = matrix.antiDiagonalStream().toList();   // [true, true, true]
     *
     * // Count true values on anti-diagonal
     * long trueCount = matrix.antiDiagonalStream().filter(b -> b).count();
     * }</pre>
     *
     * @return a {@code Stream<Boolean>} containing the anti-diagonal elements from top-right to bottom-left,
     *         or an empty stream if the matrix is empty (0 × 0)
     * @throws IllegalStateException if the matrix is non-empty and not square ({@code rowCount != columnCount})
     */
    @Override
    public Stream<Boolean> antiDiagonalStream() {
        if (isEmpty()) {
            return Stream.empty();
        }

        checkIsSquare();

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Boolean next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Boolean result = a[cursor][columnCount - cursor - 1];
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
     * Elements are streamed row by row from left to right, starting from the
     * top-left corner and proceeding to the bottom-right corner.
     *
     * <p>This method is useful for processing all matrix elements sequentially
     * without concern for their row/column positions. Because there is no primitive
     * {@code BooleanStream}, this returns a {@code Stream<Boolean>} with boxed values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * Stream<Boolean> stream = matrix.horizontalStream();   // Stream of [true, false, false, true]
     *
     * // Count true values
     * long trueCount = matrix.horizontalStream().filter(b -> b).count();   // Returns 2
     *
     * // Convert to list
     * List<Boolean> list = matrix.horizontalStream().toList();   // [true, false, false, true]
     * }</pre>
     *
     * @return a {@code Stream<Boolean>} of all elements in row-major order, or an empty stream if the matrix is empty
     */
    @Override
    public Stream<Boolean> horizontalStream() {
        return horizontalStream(0, rowCount);
    }

    /**
     * Returns a stream of elements from a single row.
     * The elements are streamed from left to right within the specified row.
     *
     * <p>This method is particularly useful when you need to process or analyze
     * a specific row of the matrix independently.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, true},
     *     {false, true, false}
     * });
     * Stream<Boolean> firstRow = matrix.horizontalStream(0);   // Stream of [true, false, true]
     *
     * // Check if any value in the second row is true
     * boolean hasTrue = matrix.horizontalStream(1).anyMatch(b -> b);   // Returns true
     * }</pre>
     *
     * @param rowIndex the index of the row to stream (0-based)
     * @return a {@code Stream<Boolean>} of elements from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    @Override
    public Stream<Boolean> horizontalStream(final int rowIndex) {
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false},
     *     {false, true},
     *     {true, true}
     * });
     * Stream<Boolean> middleRows = matrix.horizontalStream(1, 3);   // Stream rows 1 and 2: [false, true, true, true]
     *
     * // Process subset of rows
     * int[] subset = matrix.horizontalStream(0, 2)
     *     .mapToInt(b -> b ? 1 : 0)
     *     .toArray();   // [1, 0, 0, 1]
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a {@code Stream<Boolean>} of elements from the specified row range, or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public Stream<Boolean> horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        if (isEmpty()) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private int i = fromRowIndex;
            private int j = 0;

            @Override
            public boolean hasNext() {
                return i < toRowIndex;
            }

            @Override
            public Boolean next() {
                if (i >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final boolean result = a[i][j++];

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
            public <A> A[] toArray(A[] c) {
                final int len = toArrayLength(count());

                if (c.length < len) {
                    c = N.copyOf(c, len);
                }

                for (int k = 0; k < len; k++) {
                    c[k] = (A) (Boolean) a[i][j++];

                    if (j >= columnCount) {
                        i++;
                        j = 0;
                    }
                }

                if (c.length > len) {
                    c[len] = null;
                }

                return c;
            }
        });
    }

    /**
     * Returns a stream of all elements in column-major order (vertical).
     * Elements are streamed column by column from top to bottom, starting from
     * the leftmost column and proceeding to the rightmost column.
     *
     * <p>This method is marked as @Beta, indicating it may be subject to change
     * in future versions. It provides an alternative way to iterate through matrix
     * elements compared to the row-major order of horizontalStream().</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * Stream<Boolean> stream = matrix.verticalStream();   // Stream of [true, false, false, true]
     *
     * // Process in column order
     * List<Boolean> colMajor = matrix.verticalStream().toList();   // [true, false, false, true]
     * }</pre>
     *
     * @return a {@code Stream<Boolean>} of all elements in column-major order, or an empty stream if the matrix is empty
     */
    @Override
    @Beta
    public Stream<Boolean> verticalStream() {
        return verticalStream(0, columnCount);
    }

    /**
     * Returns a stream of elements from a single column.
     * The elements are streamed from top to bottom within the specified column.
     *
     * <p>This method is useful for column-wise operations such as checking
     * column properties or extracting column data.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, true},
     *     {true, true, false}
     * });
     * Stream<Boolean> firstCol = matrix.verticalStream(0);   // Stream of [true, true]
     *
     * // Check if all values in a column are true
     * boolean allTrue = matrix.verticalStream(0).allMatch(b -> b);   // Returns true
     * }</pre>
     *
     * @param columnIndex the index of the column to stream (0-based)
     * @return a {@code Stream<Boolean>} of elements from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    @Override
    public Stream<Boolean> verticalStream(final int columnIndex) {
        return verticalStream(columnIndex, columnIndex + 1);
    }

    /**
     * Returns a stream of elements from a range of columns in column-major order.
     * Elements are streamed column by column from the starting column (inclusive)
     * to the ending column (exclusive), with each column streamed from top to bottom.
     *
     * <p>This method is marked as @Beta and allows for efficient processing of a
     * subset of matrix columns in column-major order.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, true},
     *     {false, true, false}
     * });
     * Stream<Boolean> lastTwoCols = matrix.verticalStream(1, 3);   // Stream columns 1 and 2: [false, true, true, false]
     *
     * // Count true values in column subset
     * long trueCount = matrix.verticalStream(0, 2).filter(b -> b).count();
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a {@code Stream<Boolean>} of elements from the specified column range in column-major order,
     *         or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    @Beta
    public Stream<Boolean> verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        if (isEmpty()) {
            return Stream.empty();
        }

        return Stream.of(new ObjIteratorEx<>() {
            private int i = 0;
            private int j = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return j < toColumnIndex;
            }

            @Override
            public Boolean next() {
                if (j >= toColumnIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final boolean result = a[i++][j];

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

                if (n >= (long) (toColumnIndex - j) * BooleanMatrix.this.rowCount - i) {
                    i = 0;
                    j = toColumnIndex;
                } else {
                    final long offset = n + i;
                    i = (int) (offset % BooleanMatrix.this.rowCount);
                    j += (int) (offset / BooleanMatrix.this.rowCount);
                }
            }

            @Override
            public long count() {
                return (long) (toColumnIndex - j) * rowCount - i; // NOSONAR
            }

            @Override
            public <A> A[] toArray(A[] c) {
                final int len = toArrayLength(count());

                if (c.length < len) {
                    c = N.copyOf(c, len);
                }

                for (int k = 0; k < len; k++) {
                    c[k] = (A) (Boolean) a[i++][j];

                    if (i >= rowCount) {
                        i = 0;
                        j++;
                    }
                }

                if (c.length > len) {
                    c[len] = null;
                }

                return c;
            }
        });
    }

    /**
     * Returns a stream of Stream&lt;Boolean&gt; objects, where each inner stream represents a complete row.
     * This creates a stream of streams, allowing for row-by-row processing of the matrix.
     *
     * <p>This method is useful for operations that need to process entire rows as units,
     * such as row-wise transformations, filtering rows based on conditions, or mapping
     * rows to other values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, true},
     *     {false, false, false},
     *     {true, true, true}
     * });
     *
     * // Count rows that contain at least one true value
     * long rowsWithTrue = matrix.rowStreams()
     *     .filter(row -> row.anyMatch(b -> b))
     *     .count();   // Returns 2
     *
     * // Get row sums (count of true values per row)
     * int[] rowTrueCounts = matrix.rowStreams()
     *     .mapToInt(row -> (int) row.filter(b -> b).count())
     *     .toArray();   // [2, 0, 3]
     * }</pre>
     *
     * @return a {@code Stream<Stream<Boolean>>}, one inner stream per row in the matrix
     */
    @Override
    public Stream<Stream<Boolean>> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Returns a stream of Stream&lt;Boolean&gt; objects for a range of rows.
     * Each inner stream in the result represents a complete row within the specified range.
     *
     * <p>This method allows for processing a subset of rows while maintaining the
     * ability to work with complete rows as individual streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, true, false},
     *     {false, true, true},
     *     {true, false, true}
     * });
     *
     * // Process middle rows only
     * List<Boolean> hasPattern = matrix.rowStreams(1, 3)
     *     .map(row -> {
     *         List<Boolean> list = row.toList();
     *         return list.get(0) != list.get(2);   // Check if first != last
     *     })
     *     .toList();   // [true, false]
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a {@code Stream<Stream<Boolean>>} for the specified row range
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public Stream<Stream<Boolean>> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<Boolean> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return Stream.of(a[cursor++]);
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
     * Returns a stream of Stream&lt;Boolean&gt; objects, where each inner stream represents a complete column.
     * This creates a stream of streams, allowing for column-by-column processing of the matrix.
     *
     * <p>This method is marked as @Beta and is useful for operations that need to process
     * entire columns as units, such as column-wise statistics, transformations, or filtering
     * columns based on conditions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, true},
     *     {true, true, false}
     * });
     *
     * // Check which columns have all true values
     * List<Boolean> allTrueColumns = matrix.columnStreams()
     *     .map(col -> col.allMatch(b -> b))
     *     .toList();   // [true, false, false]
     *
     * // Count true values per column
     * long[] colTrueCounts = matrix.columnStreams()
     *     .mapToLong(col -> col.filter(b -> b).count())
     *     .toArray();   // [2, 1, 1]
     * }</pre>
     *
     * @return a {@code Stream<Stream<Boolean>>}, one inner stream per column in the matrix,
     *         or an empty stream if the matrix is empty
     */
    @Override
    @Beta
    public Stream<Stream<Boolean>> columnStreams() {
        return columnStreams(0, columnCount);
    }

    /**
     * Returns a stream of Stream&lt;Boolean&gt; objects for a range of columns.
     * Each inner stream in the result represents a complete column within the specified range.
     *
     * <p>This method is marked as @Beta and allows for processing a subset of columns
     * while maintaining the ability to work with complete columns as individual streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, true, false},
     *     {false, true, false, true}
     * });
     *
     * // Process last two columns
     * List<String> patterns = matrix.columnStreams(2, 4)
     *     .map(col -> col.map(b -> b ? "1" : "0")
     *                    .collect(java.util.stream.Collectors.joining()))
     *     .toList();   // ["10", "01"]
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a {@code Stream<Stream<Boolean>>} for the specified column range,
     *         or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    @Beta
    public Stream<Stream<Boolean>> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
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
            public Stream<Boolean> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return Stream.of(new ObjIteratorEx<>() {
                    private final int columnIndex = cursor++;
                    private final int toIndex2 = rowCount;
                    private int cursor2 = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor2 < toIndex2;
                    }

                    @Override
                    public Boolean next() {
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
     *
     * <p>This is a hook used by {@link AbstractMatrix} during shape validation to determine
     * the column count of an individual row.</p>
     *
     * @param a the row array to measure; may be {@code null}
     * @return the length of {@code a}, or {@code 0} if {@code a} is {@code null}
     */
    @Override
    protected int length(@SuppressWarnings("hiding") final boolean[] a) {
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
     * or accumulating). For transformations that create new matrices, use {@link #map(Throwables.BooleanUnaryOperator)}
     * or {@link #updateAll(Throwables.BooleanUnaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     *
     * // Count true values
     * int[] trueCount = {0};
     * matrix.forEach(value -> {
     *     if (value) trueCount[0]++;
     * });
     * // trueCount[0] is now 2
     *
     * // Print all values
     * matrix.forEach(value -> System.out.print(value ? "T" : "F"));
     * // Prints: TFFT
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element; receives each element value
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     * @see #forEach(int, int, int, int, Throwables.BooleanConsumer)
     */
    public <E extends Exception> void forEach(final Throwables.BooleanConsumer<E> action) throws E {
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, true},
     *     {false, true, false},
     *     {true, true, true}
     * });
     *
     * // Process only the top-left 2x2 sub-matrix
     * List<Boolean> center = new ArrayList<>();
     * matrix.forEach(0, 2, 0, 2, value -> center.add(value));
     * // center contains [true, false, false, true]
     *
     * // Count true values in bottom row
     * int[] bottomRowTrue = {0};
     * matrix.forEach(2, 3, 0, 3, value -> {
     *     if (value) bottomRowTrue[0]++;
     * });
     * // bottomRowTrue[0] is 3
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @param action the action to be performed for each element in the sub-matrix
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws IndexOutOfBoundsException if any index is out of bounds or {@code fromRowIndex > toRowIndex}
     *         or {@code fromColumnIndex > toColumnIndex}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex,
            final Throwables.BooleanConsumer<E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        if (Matrices.isParallelizable(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, elementAction, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                final boolean[] currentRow = a[i];

                for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                    action.accept(currentRow[j]);
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.println();
     * // Output:
     * // [true, false]
     * // [false, true]
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

                    final boolean[] row = a[i];
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
     * BooleanMatrix matrix1 = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * BooleanMatrix matrix2 = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * // Same content yields same hash code
     * int hash1 = matrix1.hashCode();
     * int hash2 = matrix2.hashCode();
     * assert hash1 == hash2;
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
     * Returns {@code true} if and only if the given object is also a {@code BooleanMatrix} with the
     * same dimensions and all corresponding elements are equal. Consistent with {@link #hashCode()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix m1 = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * BooleanMatrix m2 = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * m1.equals(m2);   // true
     * }</pre>
     *
     * @param obj the object to compare with; may be {@code null}
     * @return {@code true} if {@code obj} is a {@code BooleanMatrix} of the same shape and content,
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final BooleanMatrix another) {
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * System.out.println(matrix.toString());   // [[true, false], [false, true]]
     * }</pre>
     *
     * @return a string representation of this matrix
     */
    @Override
    public String toString() {
        return N.deepToString(a);
    }
}
