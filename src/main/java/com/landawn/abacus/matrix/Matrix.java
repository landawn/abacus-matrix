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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Arrays;
import com.landawn.abacus.util.Arrays.ff;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.RowDataset;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * Object matrix backed by a rectangular {@code T[][]}.
 *
 * <p>This type provides the same shape, traversal, and transformation operations as the primitive
 * matrix variants ({@link IntMatrix}, {@link LongMatrix}, {@link DoubleMatrix}, etc.) while
 * preserving reference semantics. Constructors and {@code of(...)} generally wrap the supplied
 * backing array directly, while builders, conversions, and mapping methods allocate fresh
 * storage.</p>
 *
 * <p>{@code null} elements are permitted. {@link #equals(Object)} and {@link #hashCode()} use
 * value equality on elements (i.e. {@code Objects.equals}, with {@code null} equal only to
 * {@code null}), not reference identity.</p>
 *
 * <p>When a new backing array must be created, the implementation tracks either an explicit
 * target type or the runtime component type so array-typed results remain reifiable where
 * possible.</p>
 *
 * @param <T> the element type stored in the matrix
 */
public final class Matrix<T> extends AbstractMatrix<T[], List<T>, Stream<T>, Stream<Stream<T>>, Matrix<T>> {

    final Class<T[]> arrayType;

    final Class<T> elementType;

    /**
     * Constructs a {@code Matrix} backed by the supplied two-dimensional array.
     *
     * <p>The provided array is used directly after rectangular-shape validation, so later
     * modifications to either the input array or the matrix remain visible through the other view.</p>
     *
     * <p>The array must be rectangular (all rows must have the same length). Empty arrays are allowed
     * (for example {@code new String[0][0]} or {@code new String[5][0]}). Call {@link #copy()} if
     * you need an independently owned matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[][] data = {{"A", "B"}, {"C", "D"}};
     * Matrix<String> matrix = new Matrix<>(data);
     * data[0][0] = "X";  // This also changes the matrix
     * }</pre>
     *
     * @param a the two-dimensional array of elements (must not be {@code null})
     * @throws IllegalArgumentException if the array is {@code null}, if any row is {@code null}, or if rows have
     *         different lengths (not rectangular)
     */
    public Matrix(final T[][] a) {
        this(a, null);
    }

    @SuppressWarnings("unchecked")
    private Matrix(final T[][] a, final Class<T> explicitElementType) {
        super(N.checkArgNotNull(a, "Matrix array cannot be null"),
                explicitElementType == null ? (Class<T>) a.getClass().getComponentType().getComponentType() : explicitElementType);
        arrayType = (Class<T[]>) this.a.getClass().getComponentType();
        this.elementType = (Class<T>) arrayType.getComponentType();
    }

    /**
     * Creates an empty matrix with zero rows and zero columns.
     *
     * <p>The underlying backing array is an {@code Object[][]}, so the element type is tracked
     * as {@link Object}; subsequent operations that allocate fresh storage may therefore widen
     * to {@code Object[]} rather than the static type {@code T}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.empty();
     * // matrix.rowCount() returns 0
     * // matrix.columnCount() returns 0
     * }</pre>
     *
     * @param <T> the type of elements in the matrix
     * @return an empty matrix
     */
    @SuppressWarnings("unchecked")
    public static <T> Matrix<T> empty() {
        return new Matrix<>((T[][]) new Object[0][0]);
    }

    /**
     * Creates a Matrix from a two-dimensional array.
     *
     * <p><b>Important:</b> The matrix maintains a reference to the provided array,
     * not a copy. Modifications to the original array will affect the matrix,
     * and vice versa.</p>
     *
     * <p>All rows must have the same length as the first row (rectangular array required).
     * The array must not be {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a matrix from an existing 2D array
     * String[][] data = {{"a", "b"}, {"c", "d"}};
     * Matrix<String> matrix = Matrix.of(data);
     * String value = matrix.get(1, 0);   // Returns "c"
     *
     * // Create a matrix with varargs
     * Matrix<Integer> numbers = Matrix.of(
     *     new Integer[]{1, 2, 3},
     *     new Integer[]{4, 5, 6}
     * );
     *
     * // Note: Modifications to the original array affect the matrix
     * data[0][0] = "x";  // This also changes the matrix
     * }</pre>
     *
     * @param <T> the type of elements in the matrix
     * @param a the two-dimensional array to create the matrix from (must not be {@code null})
     * @return a new Matrix containing the provided data
     * @throws IllegalArgumentException if the array is {@code null}, if any row is {@code null}, or if rows have
     *         different lengths (non-rectangular array)
     */
    @SafeVarargs
    public static <T> Matrix<T> of(final T[]... a) {
        return new Matrix<>(a);
    }

    /**
     * Creates a new matrix of the specified dimensions where every element is the provided {@code element}.
     *
     * <p>The matrix's element type is resolved from {@code element.getClass()}, so the result
     * may have a more specific component type than the static {@code T}. Because {@code element}
     * must be non-{@code null}, this factory cannot be used to produce an empty-but-typed
     * placeholder matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.repeat(2, 3, "a");
     * // Result: [["a", "a", "a"], ["a", "a", "a"]]
     * }</pre>
     *
     * @param <T> the type of elements in the matrix
     * @param rowCount the number of rows in the new matrix (must be {@code >= 0})
     * @param columnCount the number of columns in the new matrix (must be {@code >= 0})
     * @param element the value to fill the matrix with (must not be {@code null})
     * @return a new Matrix of dimensions {@code rowCount × columnCount} filled with the specified element
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         if {@code element} is {@code null}, or if the resulting shape is unrepresentable
     *         (i.e. {@code rowCount == 0} but {@code columnCount > 0})
     */
    public static <T> Matrix<T> repeat(final int rowCount, final int columnCount, final T element) throws IllegalArgumentException {
        N.checkArgNotNull(element, "element");
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        @SuppressWarnings("unchecked")
        final T[][] a = Array.newInstance(element.getClass(), rowCount, columnCount); // (T[][]) new Object[rowCount][columnCount];

        for (T[] ea : a) {
            N.fill(ea, element);
        }

        @SuppressWarnings("unchecked")
        final Class<T> resolvedElementType = (Class<T>) element.getClass();

        return new Matrix<>(a, resolvedElementType);
    }

    /**
     * Creates a square diagonal matrix with the given values on the main diagonal (upper-left to lower-right).
     * All other elements are {@code null}. The matrix dimension is determined by the length of the diagonal array.
     *
     * <p>The main diagonal runs from upper-left to lower-right. The resulting matrix is always square
     * with size n×n where n is the length of the diagonal array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a 3×3 diagonal matrix
     * Matrix<Integer> diag = Matrix.mainDiagonal(new Integer[] {1, 2, 3});
     * // Creates: [[1, null, null],
     * //           [null, 2, null],
     * //           [null, null, 3]]
     *
     * // Create a 2×2 diagonal matrix with strings
     * Matrix<String> strDiag = Matrix.mainDiagonal(new String[] {"A", "B"});
     * // Creates: [["A", null],
     * //           [null, "B"]]
     * }</pre>
     *
     * @param <T> the type of elements in the matrix
     * @param mainDiagonal the diagonal values (must not be {@code null})
     * @return a square matrix with the given diagonal values on the main diagonal
     * @throws IllegalArgumentException if the diagonal array is {@code null}
     * @see #diagonals(Object[], Object[])
     * @see #antiDiagonal(Object[])
     */
    public static <T> Matrix<T> mainDiagonal(final T[] mainDiagonal) {
        return diagonals(mainDiagonal, null);
    }

    /**
     * Creates a square diagonal matrix with the given values on the anti-diagonal (upper-right to lower-left).
     * All other elements are {@code null}. The matrix dimension is determined by the length of the diagonal array.
     *
     * <p>The anti-diagonal runs from upper-right to lower-left. The resulting matrix is always square
     * with size n×n where n is the length of the diagonal array. The first element in the array
     * goes to the top-right corner, and subsequent elements move diagonally down-left.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a 3×3 anti-diagonal matrix
     * Matrix<Integer> diag = Matrix.antiDiagonal(new Integer[] {1, 2, 3});
     * // Creates: [[null, null, 1],
     * //           [null, 2, null],
     * //           [3, null, null]]
     *
     * // Create a 2×2 anti-diagonal matrix with strings
     * Matrix<String> strDiag = Matrix.antiDiagonal(new String[] {"X", "Y"});
     * // Creates: [[null, "X"],
     * //           ["Y", null]]
     * }</pre>
     *
     * @param <T> the type of elements in the matrix
     * @param antiDiagonal the anti-diagonal values (must not be {@code null})
     * @return a square matrix with the given anti-diagonal values
     * @throws IllegalArgumentException if the diagonal array is {@code null}
     * @see #diagonals(Object[], Object[])
     * @see #mainDiagonal(Object[])
     */
    public static <T> Matrix<T> antiDiagonal(final T[] antiDiagonal) {
        return diagonals(null, antiDiagonal);
    }

    /**
     * Creates a square matrix with values on both diagonals.
     * The main diagonal runs from upper-left to lower-right, and the anti-diagonal
     * runs from upper-right to lower-left. If diagonals intersect (odd dimension),
     * the main diagonal value takes precedence. At least one diagonal must be non-{@code null},
     * and two non-empty diagonals must have the same length.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> diag = Matrix.diagonals(
     *     new String[] {"A", "B", "C"},
     *     new String[] {"X", "Y", "Z"}
     * );
     * // Creates: [["A", null, "X"],
     * //           [null, "B", null],
     * //           ["Z", null, "C"]]
     *
     * // With intersection (odd dimension)
     * Matrix<Integer> numbers = Matrix.diagonals(
     *     new Integer[] {1, 2, 3},
     *     new Integer[] {7, 8, 9}
     * );
     * // Creates: [[1, null, 7],
     * //           [null, 2, null],  // 2 takes precedence over 8
     * //           [9, null, 3]]
     * }</pre>
     *
     * @param <T> the type of elements in the matrix
     * @param mainDiagonal the values for the main diagonal (upper-left to lower-right); may be {@code null} if
     *                     {@code antiDiagonal} is non-{@code null}
     * @param antiDiagonal the values for the anti-diagonal (upper-right to lower-left); may be {@code null} if
     *                     {@code mainDiagonal} is non-{@code null}
     * @return a square matrix with the given diagonal values
     * @throws IllegalArgumentException if both arrays are {@code null}, or if both diagonals are non-empty
     *         and have different lengths
     */
    @SuppressWarnings("null")
    public static <T> Matrix<T> diagonals(final T[] mainDiagonal, final T[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(mainDiagonal != null || antiDiagonal != null, "Both 'mainDiagonal' and 'antiDiagonal' can't be null");

        N.checkArgument(N.isEmpty(mainDiagonal) || N.isEmpty(antiDiagonal) || mainDiagonal.length == antiDiagonal.length,
                "The length of 'mainDiagonal' and 'antiDiagonal' must be same");

        final int len = N.max(N.len(mainDiagonal), N.len(antiDiagonal));
        final Class<?> leftComponentClass = mainDiagonal == null ? null : mainDiagonal.getClass().getComponentType();
        final Class<?> rightComponentClass = antiDiagonal == null ? null : antiDiagonal.getClass().getComponentType();

        final Class<?> commonType = Matrices.resolveCommonAssignableType(leftComponentClass, rightComponentClass);

        @SuppressWarnings("unchecked")
        final T[][] result = Array.newInstance(commonType, len, len);

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

        @SuppressWarnings("unchecked")
        final Class<T> resolvedElementType = (Class<T>) commonType;
        return new Matrix<>(result, resolvedElementType);
    }

    /**
     * Returns the element at the specified row and column indices.
     * Row and column indices are 0-based.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * String value = matrix.get(1, 0);    // Returns "C"
     * String corner = matrix.get(1, 1);   // Returns "D"
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return the element at position ({@code rowIndex}, {@code columnIndex}); may be {@code null}
     *         since {@code null} elements are permitted
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    @MayReturnNull
    public T get(final int rowIndex, final int columnIndex) {
        return a[rowIndex][columnIndex];
    }

    /**
     * Returns the element at the specified point.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b", "c"}, {"d", "e", "f"}});
     * Point p = Point.of(1, 2);
     * String value = matrix.get(p);   // Same as matrix.get(1, 2), returns "f"
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @return the element at the specified point; may be {@code null} since {@code null} elements
     *         are permitted
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @throws IllegalArgumentException if {@code point} is {@code null}
     */
    @MayReturnNull
    public T get(final Point point) {
        N.checkArgNotNull(point, "point");

        return a[point.rowIndex()][point.columnIndex()];
    }

    /**
     * Sets the element at the specified row and column indices.
     * Row and column indices are 0-based.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b", "c"}, {"d", "e", "f"}});
     * matrix.set(1, 2, "newValue");
     * // Element at row 1, column 2 is now "newValue"
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @param value the value to set; may be {@code null}
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     * @throws ArrayStoreException if {@code value} is non-{@code null} and not assignable to
     *         the row's runtime storage component type
     */
    public void set(final int rowIndex, final int columnIndex, final T value) {
        a[rowIndex][columnIndex] = value;
    }

    /**
     * Sets the element at the specified point.
     * This is a convenience method that takes a Point object instead of separate indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * Point p = Point.of(0, 1);
     * matrix.set(p, "X");   // Same as matrix.set(0, 1, "X")
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @param value the value to set; may be {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayStoreException if {@code value} is non-{@code null} and not assignable to
     *         the row's runtime storage component type
     */
    public void set(final Point point, final T value) {
        N.checkArgNotNull(point, "point");

        a[point.rowIndex()][point.columnIndex()] = value;
    }

    /**
     * Returns the element directly above the specified position, if it exists.
     * This method provides safe access without throwing an exception when at the top edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * Nullable<String> value = matrix.valueAbove(1, 0);   // Returns Nullable.of("A")
     * Nullable<String> empty = matrix.valueAbove(0, 0);   // Returns Nullable.empty() - no row above
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return a {@link Nullable} containing the element at position {@code (rowIndex - 1, columnIndex)},
     *         or {@link Nullable#empty()} if {@code rowIndex == 0}. Note that a non-empty {@code Nullable}
     *         may itself contain {@code null} since {@code null} elements are permitted in the matrix.
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public Nullable<T> valueAbove(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? Nullable.empty() : Nullable.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, if it exists.
     * This method provides safe access without throwing an exception when at the bottom edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * Nullable<String> value = matrix.valueBelow(0, 0);   // Returns Nullable.of("C")
     * Nullable<String> empty = matrix.valueBelow(1, 0);   // Returns Nullable.empty() - no row below
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return a {@link Nullable} containing the element at position {@code (rowIndex + 1, columnIndex)},
     *         or {@link Nullable#empty()} if {@code rowIndex == rowCount - 1}. Note that a non-empty
     *         {@code Nullable} may itself contain {@code null} since {@code null} elements are permitted
     *         in the matrix.
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public Nullable<T> valueBelow(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? Nullable.empty() : Nullable.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, if it exists.
     * This method provides safe access without throwing an exception when at the leftmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * Nullable<String> value = matrix.valueLeft(0, 1);   // Returns Nullable.of("A")
     * Nullable<String> empty = matrix.valueLeft(0, 0);   // Returns Nullable.empty() - no column to the left
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return a {@link Nullable} containing the element at position {@code (rowIndex, columnIndex - 1)},
     *         or {@link Nullable#empty()} if {@code columnIndex == 0}. Note that a non-empty
     *         {@code Nullable} may itself contain {@code null} since {@code null} elements are permitted
     *         in the matrix.
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public Nullable<T> valueLeft(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? Nullable.empty() : Nullable.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, if it exists.
     * This method provides safe access without throwing an exception when at the rightmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * Nullable<String> value = matrix.valueRight(0, 0);   // Returns Nullable.of("B")
     * Nullable<String> empty = matrix.valueRight(0, 1);   // Returns Nullable.empty() - no column to the right
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return a {@link Nullable} containing the element at position {@code (rowIndex, columnIndex + 1)},
     *         or {@link Nullable#empty()} if {@code columnIndex == columnCount - 1}. Note that a non-empty
     *         {@code Nullable} may itself contain {@code null} since {@code null} elements are permitted
     *         in the matrix.
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public Nullable<T> valueRight(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? Nullable.empty() : Nullable.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as an array.
     *
     * <p><b>Note:</b> The returned array is the live internal row, so subsequent modifications
     * are mirrored in the matrix. If you need an independent copy, use {@link #rowCopy(int)}
     * or call {@code .clone()} on the returned array.</p>
     *
     * <p><b>Note on runtime component type:</b> the returned array is the row exactly as it is
     * stored internally. For matrices created by factories such as {@link #repeat(int, int, Object)}
     * or {@link #diagonals} the backing row may have a more specific runtime component type than
     * {@code Object[]} (it is allocated from the resolved element type), but no conversion is
     * performed by this method.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * String[] rowData = matrix.rowView(0);
     * rowData[0] = "X";  // This modifies the matrix directly
     * // Matrix is now: [["X", "B"], ["C", "D"]]
     *
     * // Use clone() if you need an independent copy
     * String[] rowCopy = matrix.rowView(1).clone();
     * rowCopy[0] = "Y";  // Does not affect the matrix
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return the live internal row array
     * @throws IllegalArgumentException if {@code rowIndex} is negative or greater than or equal to {@code rowCount}
     */
    @Override
    public T[] rowView(final int rowIndex) throws IllegalArgumentException {
        N.checkArgument(rowIndex >= 0 && rowIndex < rowCount, MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount);

        return a[rowIndex];
    }

    /**
     * Returns a defensive (shallow) copy of the specified row.
     * The returned array is a new array, so replacing its slots does not affect this matrix;
     * however, the element references themselves are shared with the matrix. Contrast with
     * {@link #rowView(int)}, which returns the live internal row array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * String[] rowCopy = matrix.rowCopy(1);
     * rowCopy[0] = "X";  // Does NOT affect the matrix
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new array containing the values from the specified row
     * @throws IllegalArgumentException if {@code rowIndex} is negative or greater than or equal to {@code rowCount}
     * @see #rowView(int)
     */
    @Override
    public T[] rowCopy(final int rowIndex) throws IllegalArgumentException {
        N.checkArgument(rowIndex >= 0 && rowIndex < rowCount, MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount);

        return a[rowIndex].clone();
    }

    /**
     * Returns a copy of the specified column as a new array.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously in memory. Modifications to the returned array
     * will not affect the matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * String[] colData = matrix.columnCopy(1);   // Returns ["B", "D"]
     *
     * // Modification does NOT affect the matrix (it's a copy)
     * colData[0] = "X";  // Matrix still has "B" at position (0, 1)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new array containing the values from the specified column
     * @throws IllegalArgumentException if {@code columnIndex} is negative or greater than or equal to {@code columnCount}
     */
    @Override
    public T[] columnCopy(final int columnIndex) throws IllegalArgumentException {
        N.checkArgument(columnIndex >= 0 && columnIndex < columnCount, MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount);

        final T[] res = N.newArray(elementType, rowCount);

        for (int i = 0; i < rowCount; i++) {
            res[i] = a[i][columnIndex];
        }

        return res;
    }

    /**
     * Replaces an entire row with values from the given array.
     * The array must have the same length as the number of columns in this matrix.
     * The values are copied from the provided array, so subsequent modifications to
     * the input array will not affect the matrix.
     *
     * <p>This method modifies the matrix in-place.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * String[] newRow = {"X", "Y"};
     * matrix.setRow(0, newRow);   // Replace first row
     * // Matrix is now: [["X", "Y"], ["C", "D"]]
     * }</pre>
     *
     * @param rowIndex the row index to replace (0-based)
     * @param row the new row data (must have exactly {@code columnCount} elements)
     * @throws IllegalArgumentException if {@code row} is {@code null}, if {@code rowIndex} is out of bounds,
     *         or if {@code row.length} does not equal {@code columnCount}
     * @throws ArrayStoreException if any element of {@code row} is not assignable to the row's
     *         runtime storage component type
     */
    public void setRow(final int rowIndex, final T[] row) throws IllegalArgumentException {
        N.checkArgNotNull(row, "row");
        N.checkArgument(rowIndex >= 0 && rowIndex < rowCount, MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount);
        N.checkArgument(row.length == columnCount, MSG_ROW_LENGTH_MISMATCH, columnCount, row.length);

        N.copy(row, 0, a[rowIndex], 0, columnCount);
    }

    /**
     * Replaces an entire column with values from the given array.
     * The array must have the same length as the number of rows in this matrix.
     * Each element is copied to the corresponding row in the specified column.
     *
     * <p>This method modifies the matrix in-place.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * String[] newColumn = {"X", "Y"};
     * matrix.setColumn(1, newColumn);   // Replace second column
     * // Matrix is now: [["A", "X"], ["C", "Y"]]
     * }</pre>
     *
     * @param columnIndex the column index to replace (0-based)
     * @param column the new column data (must have exactly {@code rowCount} elements)
     * @throws IllegalArgumentException if {@code column} is {@code null}, if {@code columnIndex} is out of bounds,
     *         or if {@code column.length} does not equal {@code rowCount}
     * @throws ArrayStoreException if any element of {@code column} is not assignable to the
     *         corresponding row's runtime storage component type
     */
    public void setColumn(final int columnIndex, final T[] column) throws IllegalArgumentException {
        N.checkArgNotNull(column, "column");
        N.checkArgument(columnIndex >= 0 && columnIndex < columnCount, MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount);
        N.checkArgument(column.length == rowCount, MSG_COLUMN_LENGTH_MISMATCH, rowCount, column.length);

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = column[i];
        }
    }

    /**
     * Updates all elements in the specified row by applying the given operator.
     * The operator is applied to each element in the row, and the result
     * replaces the original value. The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * matrix.updateRow(0, String::toUpperCase);
     * // Row 0 is now {"A", "B"}
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * numMatrix.updateRow(0, x -> x * 2);
     * // Row 0 is now {2, 4}
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown by the operator
     * @param rowIndex the row index to update (0-based)
     * @param operator the operator to apply to each element (must not be {@code null})
     * @throws E if the operator throws an exception
     * @throws IndexOutOfBoundsException if {@code rowIndex} is negative or greater than or equal to {@code rowCount}
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     */
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.UnaryOperator<T, E> operator) throws E {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IndexOutOfBoundsException(formatMsg(MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount));
        }

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < columnCount; i++) {
            final T updated = operator.apply(a[rowIndex][i]);
            a[rowIndex][i] = updated;
        }
    }

    /**
     * Updates all elements in the specified column by applying the given operator.
     * The operator is applied to each element in the column, and the result
     * replaces the original value. The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * matrix.updateColumn(1, s -> s + "_suffix");
     * // Column 1 is now {"b_suffix", "d_suffix"}
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * numMatrix.updateColumn(0, n -> n * n);
     * // Column 0 is now {1, 9}
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown by the operator
     * @param columnIndex the column index to update (0-based)
     * @param operator the operator to apply to each element (must not be {@code null})
     * @throws E if the operator throws an exception
     * @throws IndexOutOfBoundsException if {@code columnIndex} is negative or greater than or equal to {@code columnCount}
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     */
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.UnaryOperator<T, E> operator) throws E {
        if (columnIndex < 0 || columnIndex >= columnCount) {
            throw new IndexOutOfBoundsException(formatMsg(MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount));
        }

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            final T updated = operator.apply(a[i][columnIndex]);
            a[i][columnIndex] = updated;
        }
    }

    /**
     * Returns the main diagonal elements (upper-left to lower-right).
     * The matrix must be square (same number of rows and columns).
     * Returns a new array containing the diagonal values, modifications to which
     * will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> m = Matrix.of(new Integer[][] {{1,2,3},{4,5,6},{7,8,9}});
     * Integer[] diag = m.getMainDiagonal();   // Returns [1, 5, 9]
     * }</pre>
     *
     * @return a new array containing the diagonal elements from top-left to bottom-right
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public T[] getMainDiagonal() throws IllegalStateException {
        checkIsSquare();

        final T[] res = N.newArray(elementType, rowCount);

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
     * Matrix<Integer> m = Matrix.of(new Integer[][] {{1,2,3},{4,5,6},{7,8,9}});
     * m.setMainDiagonal(new Integer[] {10, 20, 30});
     * // Diagonal is now [10, 20, 30]
     * // Matrix is now: {{10,2,3},{4,20,6},{7,8,30}}
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must have length equal to {@code rowCount}
     *                     (treated as length 0 if {@code null})
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code mainDiagonal} array length does not equal {@code rowCount}
     *         (including when it is {@code null} and {@code rowCount > 0})
     * @throws ArrayStoreException if any element is not assignable to the row's runtime storage component type
     */
    @Override
    public void setMainDiagonal(final T[] mainDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgument(N.len(mainDiagonal) == rowCount, MSG_DIAGONAL_LENGTH_MISMATCH, rowCount, N.len(mainDiagonal));

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = mainDiagonal[i];
        }
    }

    /**
     * Updates the main diagonal elements (upper-left to lower-right) by applying the given operator.
     * The matrix must be square (same number of rows and columns).
     * Each diagonal element is replaced by the result of the operator.
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * // Double the diagonal values
     * matrix.updateMainDiagonal(x -> x * 2);
     * // Diagonal is now [2, 10, 18]
     *
     * // Set diagonal to zeros
     * matrix.updateMainDiagonal(x -> 0);
     * // Diagonal is now [0, 0, 0]
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown by the operator
     * @param operator the operator to apply to each diagonal element (must not be {@code null})
     * @throws E if the operator throws an exception
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     */
    public <E extends Exception> void updateMainDiagonal(final Throwables.UnaryOperator<T, E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            final T updated = operator.apply(a[i][i]);
            a[i][i] = updated;
        }
    }

    /**
     * Returns the anti-diagonal elements (upper-right to lower-left).
     * The matrix must be square (same number of rows and columns).
     * Returns a new array containing the anti-diagonal values.
     * The first element is from the top-right corner, the last from the bottom-left corner.
     * Modifications to the returned array will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> m = Matrix.of(new Integer[][] {{1,2,3},{4,5,6},{7,8,9}});
     * Integer[] diag = m.getAntiDiagonal();   // Returns [3, 5, 7]
     * }</pre>
     *
     * @return a new array containing the anti-diagonal elements from top-right to bottom-left
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public T[] getAntiDiagonal() throws IllegalStateException {
        checkIsSquare();

        final T[] res = N.newArray(elementType, rowCount);

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
     * Matrix<Integer> m = Matrix.of(new Integer[][] {{1,2,3},{4,5,6},{7,8,9}});
     * m.setAntiDiagonal(new Integer[] {10, 20, 30});
     * // Anti-diagonal is now [10, 20, 30]
     * // Matrix is now: {{1,2,10},{4,20,6},{30,8,9}}
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must have length equal to {@code rowCount}
     *                     (treated as length 0 if {@code null})
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code antiDiagonal} array length does not equal {@code rowCount}
     *         (including when it is {@code null} and {@code rowCount > 0})
     * @throws ArrayStoreException if any element is not assignable to the row's runtime storage component type
     */
    @Override
    public void setAntiDiagonal(final T[] antiDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgument(N.len(antiDiagonal) == rowCount, MSG_DIAGONAL_LENGTH_MISMATCH, rowCount, N.len(antiDiagonal));

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = antiDiagonal[i];
        }
    }

    /**
     * Updates the anti-diagonal elements (upper-right to lower-left) by applying the given operator.
     * The matrix must be square (same number of rows and columns).
     * Each anti-diagonal element is replaced by the result of the operator.
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * // Negate the anti-diagonal values
     * matrix.updateAntiDiagonal(x -> -x);
     * // Anti-diagonal is now [-3, -5, -7]
     *
     * Matrix<String> strMatrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * strMatrix.updateAntiDiagonal(String::toLowerCase);
     * // Anti-diagonal is now ["b", "c"]
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown by the operator
     * @param operator the operator to apply to each anti-diagonal element (must not be {@code null})
     * @throws E if the operator throws an exception
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     */
    public <E extends Exception> void updateAntiDiagonal(final Throwables.UnaryOperator<T, E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            final T updated = operator.apply(a[i][columnCount - i - 1]);
            a[i][columnCount - i - 1] = updated;
        }
    }

    // TODO should the method name be "replaceAll"? If change the method name to replaceAll, what about updateMainDiagonal/updateAntiDiagonal?

    /**
     * Updates all elements in the matrix by applying the given operator.
     * Each element is replaced by the result of applying the operator.
     * The matrix is modified in-place.
     *
     * <p>Iteration is always performed sequentially in row-major order; this operation is
     * never parallelized.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> strMatrix = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * strMatrix.updateAll(s -> s.toUpperCase());
     * // All elements are now uppercase: {{"A", "B"}, {"C", "D"}}
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * numMatrix.updateAll(x -> x * x);
     * // All elements are now squared: {{1, 4}, {9, 16}}
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown by the operator
     * @param operator the operator to apply to each element (must not be {@code null})
     * @throws E if the operator throws an exception
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     */
    public <E extends Exception> void updateAll(final Throwables.UnaryOperator<T, E> operator) throws E {
        N.checkArgNotNull(operator, "operator");
        final Throwables.IntBiConsumer<E> operation = (i, j) -> {
            final T updated = operator.apply(a[i][j]);
            a[i][j] = updated;
        };
        // Must be sequential because ensureRowCanStore mutates shared matrix metadata/storage.
        Matrices.forEachIndices(rowCount, columnCount, operation, false);
    }

    /**
     * Updates all elements in the matrix based on their position.
     * The mapper receives the row and column indices (both 0-based) and returns the new value.
     * This is useful for position-dependent transformations. The matrix is modified in-place.
     *
     * <p>Iteration is always performed sequentially in row-major order; this operation is
     * never parallelized.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * // Create a checkerboard pattern
     * matrix.updateAll((i, j) -> (i + j) % 2 == 0 ? "black" : "white");
     * // matrix is now {{"black", "white"}, {"white", "black"}}
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{0, 0}, {0, 0}});
     * numMatrix.updateAll((i, j) -> i * 10 + j);
     * // numMatrix is now {{0, 1}, {10, 11}}
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown by the mapper
     * @param mapper the function that takes row and column indices and returns the new value (must not be {@code null})
     * @throws E if the mapper throws an exception
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends T, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final Throwables.IntBiConsumer<E> operation = (i, j) -> {
            final T updated = mapper.apply(i, j);
            a[i][j] = updated;
        };

        // Must be sequential because ensureRowCanStore mutates shared matrix metadata/storage.
        Matrices.forEachIndices(rowCount, columnCount, operation, false);
    }

    /**
     * Replaces all elements that match the predicate with the new value.
     * The predicate is tested against each element's value, not its position.
     * The matrix is modified in-place.
     *
     * <p>Iteration is always performed sequentially in row-major order; this operation is
     * never parallelized.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "", null}, {"b", "c", ""}});
     * // Replace all null values with empty string
     * matrix.replaceIf(x -> x == null, "");
     *
     * // Replace empty strings with placeholder
     * matrix.replaceIf(String::isEmpty, "N/A");
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{-1, 2}, {3, -4}});
     * // Replace negative numbers with zero
     * numMatrix.replaceIf(x -> x < 0, 0);
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown by the predicate
     * @param predicate the condition to test each element (must not be {@code null})
     * @param newValue the value to use as replacement (may be {@code null})
     * @throws E if the predicate throws an exception
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     */
    public <E extends Exception> void replaceIf(final Throwables.Predicate<? super T, E> predicate, final T newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> operation = (i, j) -> {
            if (predicate.test(a[i][j])) {
                a[i][j] = newValue;
            }
        };
        // Must be sequential because ensureRowCanStore mutates shared matrix metadata/storage.
        Matrices.forEachIndices(rowCount, columnCount, operation, false);
    }

    /**
     * Replaces elements based on their position using a predicate.
     * The predicate receives row and column indices (both 0-based), not the element value.
     * This is useful for position-based replacements. The matrix is modified in-place.
     *
     * <p>Iteration is always performed sequentially in row-major order; this operation is
     * never parallelized.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * // Replace diagonal elements with zero
     * matrix.replaceIf((i, j) -> i == j, 0);
     * // Diagonal is now [0, 0, 0]
     *
     * // Replace upper triangle with null
     * matrix.replaceIf((i, j) -> i < j, null);
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown by the predicate
     * @param predicate the condition based on position (must not be {@code null})
     * @param newValue the value to use as replacement (may be {@code null})
     * @throws E if the predicate throws an exception
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     */
    public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final T newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> operation = (i, j) -> {
            if (predicate.test(i, j)) {
                a[i][j] = newValue;
            }
        };
        // Must be sequential because ensureRowCanStore mutates shared matrix metadata/storage.
        Matrices.forEachIndices(rowCount, columnCount, operation, false);
    }

    /**
     * Creates a new matrix by applying a transformation function to each element.
     * The result matrix has the same element type as the original.
     * This is a convenience method that uses the same element type for input and output.
     *
     * <p><b>Note:</b> because the result reuses this matrix's runtime element type, an {@link ArrayStoreException}
     * is thrown if {@code mapper} returns a value that is not assignable to that type. Use
     * {@link #map(Throwables.Function, Class)} with an explicit target type to map to a wider or different type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> strMatrix = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * Matrix<String> upper = strMatrix.map(String::toUpperCase);
     * // upper is {{"A", "B"}, {"C", "D"}}
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Matrix<Integer> doubled = numMatrix.map(x -> x * 2);
     * // doubled is {{2, 4}, {6, 8}}
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown
     * @param mapper the transformation function (must not be {@code null})
     * @return a new matrix with transformed elements
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws ArrayStoreException if {@code mapper} returns a value that is not assignable to this matrix's runtime element type
     * @throws E if the function throws an exception
     */
    public <E extends Exception> Matrix<T> map(final Throwables.UnaryOperator<T, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        return map(mapper, elementType);
    }

    /**
     * Creates a new matrix by applying a transformation function to each element.
     * The result matrix can have a different element type than the original.
     * The target element type must be explicitly specified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * // Convert Integer matrix to String matrix
     * Matrix<String> strings = numMatrix.map(Object::toString, String.class);
     *
     * Matrix<String> strMatrix = Matrix.of(new String[][] {{"1.5", "2.5"}, {"3.5", "4.5"}});
     * // Convert String matrix to Double matrix
     * Matrix<Double> doubles = strMatrix.map(Double::parseDouble, Double.class);
     *
     * // Complex transformation
     * Matrix<Boolean> booleans = numMatrix.map(x -> x != null && x > 0, Boolean.class);
     * }</pre>
     *
     * @param <R> the type of elements in the result matrix
     * @param <E> the type of exception that might be thrown
     * @param mapper the transformation function (must not be {@code null})
     * @param targetElementType the class of the result element type (must not be {@code null})
     * @return a new matrix with transformed elements
     * @throws IllegalArgumentException if {@code mapper} or {@code targetElementType} is {@code null}
     * @throws E if the function throws an exception
     */
    public <R, E extends Exception> Matrix<R> map(final Throwables.Function<? super T, R, E> mapper, final Class<R> targetElementType) throws E {
        N.checkArgNotNull(mapper, "mapper");
        N.checkArgNotNull(targetElementType, "targetElementType");
        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.apply(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return Matrix.of(result);
    }

    /**
     * Creates a {@link BooleanMatrix} by applying a boolean-valued function to each element.
     * This is useful for creating masks or performing element-wise comparisons.
     * The mapper receives each element (which may be {@code null}) and must return a
     * primitive {@code boolean}. The operation may be performed in parallel for large matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", null}, {null, "b"}});
     * // Check for null values
     * BooleanMatrix nullMask = matrix.mapToBoolean(x -> x == null);
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{1, -2}, {3, -4}});
     * // Check if numbers are positive
     * BooleanMatrix positive = numMatrix.mapToBoolean(x -> x > 0);
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown
     * @param mapper the function that returns a boolean for each element (must not be {@code null})
     * @return a new {@link BooleanMatrix} with the same dimensions as this matrix
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> BooleanMatrix mapToBoolean(final Throwables.ToBooleanFunction<? super T, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final boolean[][] result = new boolean[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.applyAsBoolean(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return BooleanMatrix.of(result);
    }

    /**
     * Creates a byte matrix by applying a byte-valued function to each element.
     * Any narrowing conversion behavior depends on the mapper implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * ByteMatrix bytes = matrix.mapToByte(x -> x.byteValue());
     *
     * Matrix<String> strMatrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * ByteMatrix firstChars = strMatrix.mapToByte(s -> (byte)s.charAt(0));
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown
     * @param mapper the function that returns a byte for each element (must not be {@code null})
     * @return a new {@link ByteMatrix}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> ByteMatrix mapToByte(final Throwables.ToByteFunction<? super T, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final byte[][] result = new byte[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.applyAsByte(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return ByteMatrix.of(result);
    }

    /**
     * Creates a char matrix by applying a char-valued function to each element.
     * This is useful for character-based transformations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> strMatrix = Matrix.of(new String[][] {{"abc", "def"}, {"ghi", "jkl"}});
     * CharMatrix firstChars = strMatrix.mapToChar(s -> s.charAt(0));
     * // firstChars: {{'a', 'd'}, {'g', 'j'}}
     *
     * Matrix<Integer> scores = Matrix.of(new Integer[][] {{95, 85}, {78, 92}});
     * CharMatrix grades = scores.mapToChar(score ->
     *     score >= 90 ? 'A' : score >= 80 ? 'B' : 'C'
     * );
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown
     * @param mapper the function that returns a char for each element (must not be {@code null})
     * @return a new {@link CharMatrix}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> CharMatrix mapToChar(final Throwables.ToCharFunction<? super T, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final char[][] result = new char[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.applyAsChar(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return CharMatrix.of(result);
    }

    /**
     * Creates a short matrix by applying a short-valued function to each element.
     * Any narrowing conversion behavior depends on the mapper implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * ShortMatrix shorts = matrix.mapToShort(x -> x.shortValue());
     *
     * // Calculate hash codes as shorts
     * ShortMatrix hashes = matrix.mapToShort(x -> (short)x.hashCode());
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown
     * @param mapper the function that returns a short for each element (must not be {@code null})
     * @return a new {@link ShortMatrix}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> ShortMatrix mapToShort(final Throwables.ToShortFunction<? super T, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final short[][] result = new short[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.applyAsShort(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return ShortMatrix.of(result);
    }

    /**
     * Creates an int matrix by applying an int-valued function to each element.
     * This is one of the most commonly used primitive type conversions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> strMatrix = Matrix.of(new String[][] {{"abc", "de"}, {"f", "ghij"}});
     * IntMatrix lengths = strMatrix.mapToInt(String::length);
     * // lengths: {{3, 2}, {1, 4}}
     *
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * IntMatrix ints = matrix.mapToInt(x -> x.intValue());
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown
     * @param mapper the function that returns an int for each element (must not be {@code null})
     * @return a new {@link IntMatrix}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> IntMatrix mapToInt(final Throwables.ToIntFunction<? super T, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final int[][] result = new int[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.applyAsInt(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return IntMatrix.of(result);
    }

    /**
     * Creates a long matrix by applying a long-valued function to each element.
     * Useful for operations that require 64-bit integer precision.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * LongMatrix longs = matrix.mapToLong(x -> x.longValue());
     *
     * // Calculate large values
     * LongMatrix big = matrix.mapToLong(x -> (long) x * 1_000_000_000L);
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown
     * @param mapper the function that returns a long for each element (must not be {@code null})
     * @return a new {@link LongMatrix}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> LongMatrix mapToLong(final Throwables.ToLongFunction<? super T, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final long[][] result = new long[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.applyAsLong(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return LongMatrix.of(result);
    }

    /**
     * Creates a float matrix by applying a float-valued function to each element.
     * Useful for single-precision floating-point operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * FloatMatrix floats = matrix.mapToFloat(x -> x.floatValue());
     *
     * // Calculate percentages
     * FloatMatrix percents = matrix.mapToFloat(x -> x / 100.0f);
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown
     * @param mapper the function that returns a float for each element (must not be {@code null})
     * @return a new {@link FloatMatrix}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> FloatMatrix mapToFloat(final Throwables.ToFloatFunction<? super T, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final float[][] result = new float[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.applyAsFloat(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return FloatMatrix.of(result);
    }

    /**
     * Creates a double matrix by applying a double-valued function to each element.
     * Useful for double-precision floating-point operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 4}, {9, 16}});
     * DoubleMatrix doubles = matrix.mapToDouble(x -> x.doubleValue());
     *
     * // Calculate square roots
     * DoubleMatrix results = matrix.mapToDouble(x -> Math.sqrt(x));
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown
     * @param mapper the function that returns a double for each element (must not be {@code null})
     * @return a new {@link DoubleMatrix}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> DoubleMatrix mapToDouble(final Throwables.ToDoubleFunction<? super T, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final double[][] result = new double[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = mapper.applyAsDouble(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return DoubleMatrix.of(result);
    }

    /**
     * Fills all elements in the matrix with the specified value.
     * This replaces every element with the same value.
     *
     * <p>This method modifies the matrix in-place.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * matrix.fill("");             // Fill with empty strings
     * matrix.fill("default");      // Reset to default
     * }</pre>
     *
     * @param value the value to fill the matrix with (may be {@code null})
     */
    public void fill(final T value) {
        for (int i = 0; i < rowCount; i++) {
            N.fill(a[i], value);
        }
    }

    /**
     * Copies values into the matrix from another two-dimensional array.
     * Copies as much data as will fit, starting from the top-left corner (position 0,0).
     * If the source array is larger than this matrix, extra data is ignored.
     * If the source array is smaller than this matrix, the remaining cells are unchanged.
     *
     * <p>This method modifies the matrix in-place.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * String[][] data = {{"A", "B"}, {"C", "D"}};
     * matrix.fill(data);   // Copy from top-left
     * }</pre>
     *
     * @param source the source two-dimensional array to copy values from (must not be {@code null})
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final T[][] source) {
        fill(0, 0, source);
    }

    /**
     * Copies values into the matrix from another two-dimensional array starting at the specified position.
     * Copies as much data as will fit from the starting position.
     * If the source data extends beyond the matrix bounds, it is truncated.
     *
     * <p>This method modifies the matrix in-place.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"A", "B", "C"}, {"D", "E", "F"}, {"G", "H", "I"}});
     * String[][] patch = {{"X", "Y"}, {"Z", "W"}};
     * matrix.fill(1, 2, patch);   // Start filling at row 1, column 2
     * }</pre>
     *
     * @param destRowIndex the target row index (0-based, must be between 0 and rowCount inclusive)
     * @param destColumnIndex the target column index (0-based, must be between 0 and columnCount inclusive)
     * @param source the source two-dimensional array to copy values from (must not be {@code null})
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if the target indices are negative or exceed matrix dimensions
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final T[][] source) throws IllegalArgumentException {
        N.checkArgNotNull(source, "source");
        N.checkArgument(destRowIndex >= 0 && destRowIndex <= rowCount, "destRowIndex({}) must be between 0 and rowCount({})", destRowIndex, rowCount);
        N.checkArgument(destColumnIndex >= 0 && destColumnIndex <= columnCount, "destColumnIndex({}) must be between 0 and columnCount({})", destColumnIndex,
                columnCount);

        for (int i = 0, minLen = N.min(rowCount - destRowIndex, source.length); i < minLen; i++) {
            if (source[i] != null) {
                final int copyLen = N.min(source[i].length, columnCount - destColumnIndex);
                N.copy(source[i], 0, a[i + destRowIndex], destColumnIndex, copyLen);
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
     * Matrix<String> original = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * Matrix<String> copy = original.copy();
     * copy.set(0, 0, "X");   // Original matrix remains unchanged
     * // original: {{"A", "B"}, {"C", "D"}}
     * // copy:     {{"X", "B"}, {"C", "D"}}
     * }</pre>
     *
     * @return a new matrix that is a copy of this matrix with full independence guarantee
     */
    @Override
    public Matrix<T> copy() {
        final T[][] c = N.newArray(arrayType, rowCount);

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[i].clone();
        }

        return new Matrix<>(c, elementType);
    }

    /**
     * Creates a copy of a row range from this matrix.
     * The returned matrix contains only the specified rows and is completely independent from the original matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}, {5, 6}});
     * Matrix<Integer> subset = matrix.copy(1, 3);   // Copies rows 1 and 2 (exclusive end)
     * // subset is now {{3, 4}, {5, 6}}
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new Matrix containing the specified rows
     * @throws IndexOutOfBoundsException if {@code fromRowIndex} or {@code toRowIndex} is out of range
     *         ({@code fromRowIndex < 0 || toRowIndex > rowCount || fromRowIndex > toRowIndex})
     */
    @Override
    public Matrix<T> copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        final T[][] c = N.newArray(arrayType, toRowIndex - fromRowIndex);

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = a[i].clone();
        }

        return new Matrix<>(c, elementType);
    }

    /**
     * Creates a copy of a submatrix defined by row and column ranges.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * Matrix<Integer> submatrix = matrix.copy(0, 2, 1, 3);   // Copies rows 0-1, columns 1-2
     * // submatrix is now {{2, 3}, {5, 6}}
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new Matrix containing the specified submatrix
     * @throws IndexOutOfBoundsException if any of the row or column indices are out of range
     */
    @Override
    public Matrix<T> copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        final T[][] c = N.newArray(arrayType, toRowIndex - fromRowIndex);

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = N.copyOfRange(a[i], fromColumnIndex, toColumnIndex);
        }

        return new Matrix<>(c, elementType);
    }

    /**
     * Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount},
     * anchored at the top-left corner of this matrix. New cells are filled with {@code null}.
     *
     * <ul>
     *   <li><b>If a dimension shrinks</b> — elements beyond the new boundary are discarded
     *       (excess rows removed from the bottom, excess columns removed from the right).</li>
     *   <li><b>If a dimension grows</b> — new cells are filled with {@code null}.</li>
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
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b", "c"}, {"d", "e", "f"}, {"g", "h", "i"}});
     *
     * // Grow: both dimensions larger — new cells filled with null
     * Matrix<String> grown = matrix.resize(4, 4);
     * // Result: [["a", "b", "c", null],
     * //          ["d", "e", "f", null],
     * //          ["g", "h", "i", null],
     * //          [null, null, null, null]]
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * Matrix<String> truncated = matrix.resize(2, 2);
     * // Result: [["a", "b"],
     * //          ["d", "e"]]
     *
     * // Mixed: grow rows, truncate columns
     * Matrix<String> mixed = matrix.resize(4, 2);
     * // Result: [["a", "b"],
     * //          ["d", "e"],
     * //          ["g", "h"],
     * //          [null, null]]
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new Matrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         or if the resulting element count would overflow {@code Integer.MAX_VALUE}
     * @see #resize(int, int, Object)
     * @see #extend(int, int, int, int)
     */
    public Matrix<T> resize(final int newRowCount, final int newColumnCount) {
        return resize(newRowCount, newColumnCount, null);
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
     * <p><b>Comparison with {@link #extend(int, int, int, int, Object)}:</b>
     * {@code resize} takes <em>absolute</em> target dimensions and may truncate existing content.
     * {@code extend} takes <em>relative</em> padding amounts per edge and <em>never truncates</em>.
     * Use {@code extend} when the entire original content must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b", "c"}, {"d", "e", "f"}, {"g", "h", "i"}});
     *
     * // Grow: both dimensions larger — new cells filled with "x"
     * Matrix<String> grown = matrix.resize(4, 4, "x");
     * // Result: [["a", "b", "c", "x"],
     * //          ["d", "e", "f", "x"],
     * //          ["g", "h", "i", "x"],
     * //          ["x", "x", "x", "x"]]
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * Matrix<String> truncated = matrix.resize(2, 2, "x");
     * // Result: [["a", "b"],
     * //          ["d", "e"]]
     *
     * // Mixed: grow rows, truncate columns
     * Matrix<String> mixed = matrix.resize(4, 2, "x");
     * // Result: [["a", "b"],
     * //          ["d", "e"],
     * //          ["g", "h"],
     * //          ["x", "x"]]
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the value used to fill any newly created cells; may be {@code null}
     * @return a new Matrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         or if the resulting element count would overflow {@code Integer.MAX_VALUE}
     * @see #resize(int, int)
     * @see #extend(int, int, int, int, Object)
     */
    public Matrix<T> resize(final int newRowCount, final int newColumnCount, final T defaultValue) throws IllegalArgumentException {
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
            final boolean fillDefaultValue = defaultValue != null;
            final T[][] b = N.newArray(arrayType, newRowCount);

            for (int i = 0; i < newRowCount; i++) {
                b[i] = i < rowCount ? N.copyOf(a[i], newColumnCount) : (T[]) N.newArray(elementType, newColumnCount);

                if (fillDefaultValue) {
                    if (i >= rowCount) {
                        N.fill(b[i], defaultValue);
                    } else if (columnCount < newColumnCount) {
                        N.fill(b[i], columnCount, newColumnCount, defaultValue);
                    }
                }
            }

            return new Matrix<>(b, elementType);
        }
    }

    /**
     * Returns a new matrix formed by adding {@code null}-filled padding around every edge of this matrix.
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
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b"}});
     *
     * // Uniform border of 1 cell on every side
     * Matrix<String> bordered = matrix.extend(1, 1, 1, 1);
     * // Result: [[null, null, null, null],
     * //          [null, "a",  "b",  null],
     * //          [null, null, null, null]]
     * }</pre>
     *
     * @param padTop number of rows to add above; must be {@code >= 0}
     * @param padBottom number of rows to add below; must be {@code >= 0}
     * @param padLeft number of columns to add to the left; must be {@code >= 0}
     * @param padRight number of columns to add to the right; must be {@code >= 0}
     * @return a new Matrix with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #extend(int, int, int, int, Object)
     * @see #resize(int, int)
     */
    @Override
    public Matrix<T> extend(final int padTop, final int padBottom, final int padLeft, final int padRight) {
        return extend(padTop, padBottom, padLeft, padRight, null);
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
     * <p><b>Unlike {@link #resize(int, int, Object)}, this method never truncates existing content.</b>
     * All elements of the original matrix appear unchanged in the result.</p>
     *
     * <p><b>Typical uses:</b> adding sentinel borders, creating asymmetric margins, or embedding a smaller
     * matrix into a larger frame (e.g. more padding on one side than another).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b"}});
     *
     * // Asymmetric padding: 2 columns on the left, 1 on the right
     * Matrix<String> padded = matrix.extend(1, 1, 2, 1, "x");
     * // Result: [["x", "x", "x", "x", "x"],
     * //          ["x", "x", "a", "b", "x"],
     * //          ["x", "x", "x", "x", "x"]]
     *
     * // Uniform border of 1 cell on every side
     * Matrix<String> bordered = matrix.extend(1, 1, 1, 1, null);
     * // Result: [[null, null, null, null],
     * //          [null, "a",  "b",  null],
     * //          [null, null, null, null]]
     * }</pre>
     *
     * @param padTop number of rows to add above; must be {@code >= 0}
     * @param padBottom number of rows to add below; must be {@code >= 0}
     * @param padLeft number of columns to add to the left; must be {@code >= 0}
     * @param padRight number of columns to add to the right; must be {@code >= 0}
     * @param defaultValue the value used to fill all newly added cells; may be {@code null}
     * @return a new Matrix with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #extend(int, int, int, int)
     * @see #resize(int, int, Object)
     */
    public Matrix<T> extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final T defaultValue)
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
            final boolean fillDefaultValue = defaultValue != null;
            final T[][] b = N.newArray(arrayType, newRowCount);

            for (int i = 0; i < newRowCount; i++) {
                b[i] = N.newArray(elementType, newColumnCount);

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

            return new Matrix<>(b, elementType);
        }
    }

    /**
     * Reverses the order of elements in each row (horizontal flip).
     *
     * <p>This method modifies the matrix in-place.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.flipHorizontallyInPlace();
     * // Matrix is now: [[3, 2, 1], [6, 5, 4]]
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
     * Reverses the order of rows in the matrix (vertical flip).
     *
     * <p>This method modifies the matrix in-place. It swaps row references rather than
     * individual elements, so it remains correct even when rows have different runtime
     * component types.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.flipVerticallyInPlace();
     * // Matrix is now: [[5, 6], [3, 4], [1, 2]]
     * }</pre>
     *
     * @see #flipVertically()
     */
    @Override
    public void flipVerticallyInPlace() {
        // Swap row references rather than individual elements. This is correct even when
        // rows have different runtime component types (which is permitted by the constructor),
        // and avoids ArrayStoreException when assigning a value into an incompatibly-typed row.
        for (int l = 0, h = rowCount - 1; l < h; l++, h--) {
            final T[] tmp = a[l];
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
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * Matrix<Integer> flipped = matrix.flipHorizontally();
     * // Result: {{3, 2, 1}, {6, 5, 4}}
     * }</pre>
     *
     * @return a new horizontally flipped matrix
     * @see #flipHorizontallyInPlace()
     * @see #flipVertically()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public Matrix<T> flipHorizontally() {
        final Matrix<T> res = this.copy();
        res.flipHorizontallyInPlace();
        return res;
    }

    /**
     * Creates a vertically flipped copy of this matrix.
     * The rows are reversed top-to-bottom (the topmost row becomes bottommost).
     * Creates a new matrix; the original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}, {5, 6}});
     * Matrix<Integer> flipped = matrix.flipVertically();
     * // Result: {{5, 6}, {3, 4}, {1, 2}}
     * }</pre>
     *
     * @return a new vertically flipped matrix
     * @see #flipVerticallyInPlace()
     * @see #flipHorizontally()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public Matrix<T> flipVertically() {
        final Matrix<T> res = this.copy();
        res.flipVerticallyInPlace();
        return res;
    }

    /**
     * Returns a new matrix that is this matrix rotated 90 degrees clockwise.
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * column of the result being the last row of the original, read from left to right.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    Rotated 90° clockwise:
     * // 1 2 3        7 4 1
     * // 4 5 6   =>   8 5 2
     * // 7 8 9        9 6 3
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 90 degrees clockwise
     */
    @Override
    public Matrix<T> rotate90() {
        if (columnCount == 0) {
            return new Matrix<>(N.newArray(arrayType, 0), elementType);
        }

        checkRepresentableShape(columnCount, rowCount);

        final T[][] c = N.newArray(arrayType, columnCount);

        for (int i = 0; i < columnCount; i++) {
            c[i] = N.newArray(elementType, rowCount);
        }

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

        return new Matrix<>(c, elementType);
    }

    /**
     * Returns a new matrix that is this matrix rotated 180 degrees clockwise.
     * This is equivalent to flipping both horizontally and vertically, reversing the
     * order of all elements. The resulting matrix has the same dimensions as the original.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    Rotated 180°:
     * // 1 2 3        6 5 4
     * // 4 5 6   =>   3 2 1
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 180 degrees clockwise
     */
    @Override
    public Matrix<T> rotate180() {
        final T[][] c = N.newArray(arrayType, rowCount);

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[rowCount - i - 1].clone();
            N.reverse(c[i]);
        }

        return new Matrix<>(c, elementType);
    }

    /**
     * Returns a new matrix that is this matrix rotated 270 degrees clockwise.
     * This is equivalent to rotating 90 degrees counter-clockwise.
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * column of the result being the first row of the original, read from right to left.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    Rotated 270° clockwise:
     * // 1 2 3        3 6 9
     * // 4 5 6   =>   2 5 8
     * // 7 8 9        1 4 7
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 270 degrees clockwise
     */
    @Override
    public Matrix<T> rotate270() {
        if (columnCount == 0) {
            return new Matrix<>(N.newArray(arrayType, 0), elementType);
        }

        checkRepresentableShape(columnCount, rowCount);

        final T[][] c = N.newArray(arrayType, columnCount);

        for (int i = 0; i < columnCount; i++) {
            c[i] = N.newArray(elementType, rowCount);
        }

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

        return new Matrix<>(c, elementType);
    }

    /**
     * Returns a new matrix that is the transpose of this matrix.
     * The transpose operation converts each row into a column, so the element at position
     * {@code (i, j)} in the original matrix appears at position {@code (j, i)} in the
     * transposed matrix. The resulting matrix has dimensions {@code columnCount × rowCount}.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    Transposed:
     * // 1 2 3        1 4
     * // 4 5 6   =>   2 5
     * //              3 6
     *
     * Matrix<Integer> original = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * Matrix<Integer> transposed = original.transpose();   // 2×3 becomes 3×2
     * }</pre>
     *
     * @return a new matrix that is the transpose of this matrix, with dimensions
     *         {@code columnCount × rowCount} (an empty matrix with zero columns yields
     *         an empty {@code 0}-row matrix)
     */
    @Override
    public Matrix<T> transpose() {
        if (columnCount == 0) {
            return new Matrix<>(N.newArray(arrayType, 0), elementType);
        }

        checkRepresentableShape(columnCount, rowCount);

        final T[][] c = N.newArray(arrayType, columnCount);

        for (int i = 0; i < columnCount; i++) {
            c[i] = N.newArray(elementType, rowCount);
        }

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
        return new Matrix<>(c, elementType);
    }

    /**
     * Reshapes this matrix to have the specified dimensions.
     * Elements are taken in row-major order from the original matrix and placed into the
     * new shape. The new shape must have at least as many total elements as the original
     * ({@code newRowCount * newColumnCount >= elementCount()}).
     * If the new shape has more elements, the extra positions are filled with
     * {@code null}. Creates a new matrix; the original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * Matrix<Integer> reshaped = matrix.reshape(3, 2);   // Becomes {{1, 2}, {3, 4}, {5, 6}}
     * Matrix<Integer> extended = matrix.reshape(2, 4);   // Becomes {{1, 2, 3, 4}, {5, 6, null, null}}
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix (must be non-negative)
     * @param newColumnCount the number of columns in the reshaped matrix (must be non-negative)
     * @return a new Matrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         or if the new shape is too small to hold all elements
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public Matrix<T> reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final T[][] c = N.newArray(arrayType, newRowCount);

        for (int i = 0; i < newRowCount; i++) {
            c[i] = N.newArray(elementType, newColumnCount);
        }

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return new Matrix<>(c, elementType);
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

        return new Matrix<>(c, elementType);
    }

    /**
     * Repeats each element in the matrix by the specified number of times in both directions.
     * Each element is expanded into a block of rowRepeats×columnRepeats identical elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Matrix<Integer> repeated = matrix.repeatElements(2, 3);
     * // Result: {{1,1,1,2,2,2}, {1,1,1,2,2,2}, {3,3,3,4,4,4}, {3,3,3,4,4,4}}
     * }</pre>
     *
     * @param rowRepeats number of times to repeat each element in the row direction (must be {@code >= 1})
     * @param columnRepeats number of times to repeat each element in the column direction (must be {@code >= 1})
     * @return a new matrix with repeated elements, with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats < 1} or {@code columnRepeats < 1}, or if the resulting
     *         dimensions would overflow {@code Integer.MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repelem.html">MATLAB repelem function</a>
     */
    @Override
    public Matrix<T> repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }

        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final T[][] c = N.newArray(arrayType, rowCount * rowRepeats);

        for (int i = 0, len = c.length; i < len; i++) {
            c[i] = N.newArray(elementType, columnCount * columnRepeats);
        }

        for (int i = 0; i < rowCount; i++) {
            final T[] aa = a[i];
            final T[] fr = c[i * rowRepeats];

            for (int j = 0; j < columnCount; j++) {
                // N.copy(Array.repeat(a[i][j], columnRepeats), 0, fr, j * columnRepeats, columnRepeats);
                N.fill(fr, j * columnRepeats, j * columnRepeats + columnRepeats, aa[j]);
            }

            for (int k = 1; k < rowRepeats; k++) {
                N.copy(fr, 0, c[i * rowRepeats + k], 0, fr.length);
            }
        }

        return new Matrix<>(c, elementType);
    }

    /**
     * Repeats the entire matrix as a tile pattern by the specified number of times.
     * The matrix is repeated as a whole block rowRepeats times vertically and columnRepeats times horizontally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Matrix<Integer> tiled = matrix.repeatMatrix(2, 3);
     * // Result: {{1,2,1,2,1,2}, {3,4,3,4,3,4}, {1,2,1,2,1,2}, {3,4,3,4,3,4}}
     * }</pre>
     *
     * @param rowRepeats number of times to repeat the matrix in the row direction (must be {@code >= 1})
     * @param columnRepeats number of times to repeat the matrix in the column direction (must be {@code >= 1})
     * @return a new matrix with the original matrix repeated, with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats < 1} or {@code columnRepeats < 1}, or if the resulting
     *         dimensions would overflow {@code Integer.MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repmat.html">MATLAB repmat function</a>
     */
    @Override
    public Matrix<T> repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }

        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final T[][] c = N.newArray(arrayType, rowCount * rowRepeats);

        for (int i = 0, len = c.length; i < len; i++) {
            c[i] = N.newArray(elementType, columnCount * columnRepeats);
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

        return new Matrix<>(c, elementType);
    }

    /**
     * Returns a list containing all matrix elements in row-major order.
     * The returned list is backed by a freshly allocated array, so subsequent modifications
     * to the matrix do not affect the list and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * List<Integer> flat = matrix.flatten();   // Returns List of [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * @return a list of all elements in row-major order, with size equal to {@code rowCount * columnCount}
     * @throws IllegalStateException if the matrix is too large to flatten ({@code rowCount * columnCount > Integer.MAX_VALUE})
     */
    @Override
    public List<T> flatten() {
        // Check for overflow before allocation
        if ((long) rowCount * columnCount > Integer.MAX_VALUE) {
            throw new IllegalStateException("Matrix too large to flatten: " + rowCount + " x " + columnCount);
        }

        final T[] c = N.newArray(elementType, rowCount * columnCount);

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c, i * columnCount, columnCount);
        }

        return N.toList(c);
    }

    /**
     * Applies an operation to the flattened (row-major order) view of this matrix.
     * The operation receives a single one-dimensional array containing all elements in row-major order,
     * and any modifications to that array are reflected back in this matrix.
     *
     * <p><strong>Unsafe API boundary:</strong> the supplied action can mutate matrix state through the flattened view.
     * Prefer {@link #copy()} or other defensive APIs unless in-place mutation is intentional.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{3, 1, 2}, {6, 4, 5}});
     * matrix.mutateAsFlat(arr -> java.util.Arrays.sort(arr));
     * // Matrix becomes: [[1, 2, 3], [4, 5, 6]] (all elements sorted in row-major order)
     * }</pre>
     *
     * @param <E> the type of exception that the operation may throw
     * @param action the operation to apply to the flattened array
     * @throws E if the operation throws an exception
     * @see Arrays.ff#mutateAsFlat(Object[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super T[], E> action) throws E {
        ff.mutateAsFlat(a, action);
    }

    /**
     * Vertically stacks this matrix with another matrix.
     * The matrices must have the same number of columns.
     * The result has rows from this matrix followed by rows from the other matrix.
     * Creates a new matrix; neither input matrix is modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> m1 = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Matrix<Integer> m2 = Matrix.of(new Integer[][] {{5, 6}, {7, 8}});
     * Matrix<Integer> stacked = m1.stackVertically(m2);
     * // Result: {{1, 2}, {3, 4}, {5, 6}, {7, 8}}
     * }</pre>
     *
     * @param other the matrix to stack below this matrix (must not be {@code null})
     * @return a new vertically stacked matrix with dimensions (this.rowCount + other.rowCount) × columnCount
     * @throws IllegalArgumentException if {@code other} is {@code null}, the matrices have different column counts,
     *         or the merged row count would overflow {@code Integer.MAX_VALUE}
     * @see #stackHorizontally(Matrix)
     * @see IntMatrix#stackVertically(IntMatrix)
     */
    @Override
    public Matrix<T> stackVertically(final Matrix<T> other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.columnCount, MSG_VSTACK_COLUMN_MISMATCH, columnCount, other.columnCount);
        final long mergedRowCount = (long) rowCount + other.rowCount;
        N.checkArgument(mergedRowCount <= Integer.MAX_VALUE, "Merged row count overflow: {} + {} = {}", rowCount, other.rowCount, mergedRowCount);

        @SuppressWarnings("unchecked")
        final Class<T> mergedElementType = (Class<T>) Matrices.resolveCommonAssignableType(elementType, other.elementType);
        @SuppressWarnings("unchecked")
        final Class<T[]> mergedArrayType = (Class<T[]>) N.newArray(mergedElementType, 0).getClass();
        final T[][] c = N.newArray(mergedArrayType, (int) mergedRowCount);
        int j = 0;

        for (int i = 0; i < rowCount; i++) {
            c[j] = N.newArray(mergedElementType, columnCount);
            N.copy(a[i], 0, c[j], 0, columnCount);
            j++;
        }

        for (int i = 0; i < other.rowCount; i++) {
            c[j] = N.newArray(mergedElementType, columnCount);
            N.copy(other.a[i], 0, c[j], 0, columnCount);
            j++;
        }

        return new Matrix<>(c, mergedElementType);
    }

    /**
     * Horizontally stacks this matrix with another matrix.
     * The matrices must have the same number of rows.
     * The result has columns from this matrix followed by columns from the other matrix.
     * Creates a new matrix; neither input matrix is modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> m1 = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Matrix<Integer> m2 = Matrix.of(new Integer[][] {{5}, {6}});
     * Matrix<Integer> stacked = m1.stackHorizontally(m2);
     * // Result: {{1, 2, 5}, {3, 4, 6}}
     * }</pre>
     *
     * @param other the matrix to stack to the right of this matrix (must not be {@code null})
     * @return a new horizontally stacked matrix with dimensions rowCount × (this.columnCount + other.columnCount)
     * @throws IllegalArgumentException if {@code other} is {@code null}, the matrices have different row counts,
     *         or the merged column count would overflow {@code Integer.MAX_VALUE}
     * @see #stackVertically(Matrix)
     * @see IntMatrix#stackHorizontally(IntMatrix)
     */
    @Override
    public Matrix<T> stackHorizontally(final Matrix<T> other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(rowCount == other.rowCount, MSG_HSTACK_ROW_MISMATCH, rowCount, other.rowCount);
        final long mergedColumnCount = (long) columnCount + other.columnCount;
        N.checkArgument(mergedColumnCount <= Integer.MAX_VALUE, "Merged column count overflow: {} + {} = {}", columnCount, other.columnCount,
                mergedColumnCount);

        @SuppressWarnings("unchecked")
        final Class<T> mergedElementType = (Class<T>) Matrices.resolveCommonAssignableType(elementType, other.elementType);
        @SuppressWarnings("unchecked")
        final Class<T[]> mergedArrayType = (Class<T[]>) N.newArray(mergedElementType, 0).getClass();
        final T[][] c = N.newArray(mergedArrayType, rowCount);

        for (int i = 0; i < rowCount; i++) {
            c[i] = N.newArray(mergedElementType, (int) mergedColumnCount);
            N.copy(a[i], 0, c[i], 0, columnCount);
            N.copy(other.a[i], 0, c[i], columnCount, other.columnCount);
        }

        return new Matrix<>(c, mergedElementType);
    }

    /**
     * Combines this matrix with another matrix element-wise using the specified function.
     * The function is applied to corresponding elements at the same positions (i, j) in both matrices.
     * Both matrices must have the same dimensions. The result matrix has the same element type as this matrix.
     * The operation may be performed in parallel for large matrices.
     *
     * <p><b>Note:</b> because the result reuses this matrix's runtime element type, an {@link ArrayStoreException}
     * is thrown if {@code zipFunction} returns a value that is not assignable to that type. Use
     * {@link #zipWith(Matrix, Throwables.BiFunction, Class)} with an explicit target type to produce a wider or different type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> m1 = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Matrix<Integer> m2 = Matrix.of(new Integer[][] {{5, 6}, {7, 8}});
     * Matrix<Integer> sum = m1.zipWith(m2, (a, b) -> a + b);
     * // Result: {{6, 8}, {10, 12}}
     * }</pre>
     *
     * @param <B> the element type of the other matrix
     * @param <E> the type of exception that the zip function may throw
     * @param other the other matrix to zip with (must have the same dimensions, must not be {@code null})
     * @param zipFunction the binary function to apply to corresponding elements (must not be {@code null})
     * @return a new matrix with the results of the zip function
     * @throws IllegalArgumentException if the matrices don't have the same dimensions, or if
     *         {@code zipFunction} is {@code null}
     * @throws ArrayStoreException if {@code zipFunction} returns a value that is not assignable to this matrix's runtime element type
     * @throws E if the zip function throws an exception
     */
    public <B, E extends Exception> Matrix<T> zipWith(final Matrix<B> other, final Throwables.BiFunction<? super T, ? super B, T, E> zipFunction) throws E {
        return zipWith(other, zipFunction, elementType);
    }

    /**
     * Combines this matrix with another matrix element-wise using the specified function.
     * The function can return elements of a different type than the input matrices.
     * The matrices must have the same dimensions. The operation may be performed
     * in parallel for large matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> m1 = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Matrix<Double> m2 = Matrix.of(new Double[][] {{0.5, 1.0}, {1.5, 2.0}});
     * Matrix<String> result = m1.zipWith(m2, (a, b) -> a + ":" + b, String.class);
     * // Result: {{"1:0.5", "2:1.0"}, {"3:1.5", "4:2.0"}}
     * }</pre>
     *
     * @param <B> the element type of the other matrix
     * @param <R> the element type of the result matrix
     * @param <E> the type of exception that the zip function may throw
     * @param other the other matrix to zip with (must have the same dimensions, must not be {@code null})
     * @param zipFunction the function to apply to corresponding elements (must not be {@code null})
     * @param targetElementType the class of the result element type (must not be {@code null})
     * @return a new matrix with the results of the zip function
     * @throws IllegalArgumentException if the matrices don't have the same shape, or if
     *         {@code zipFunction} or {@code targetElementType} is {@code null}
     * @throws E if the zip function throws an exception
     */
    public <B, R, E extends Exception> Matrix<R> zipWith(final Matrix<B> other, final Throwables.BiFunction<? super T, ? super B, R, E> zipFunction,
            final Class<R> targetElementType) throws IllegalArgumentException, E {
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgNotNull(targetElementType, "targetElementType");

        final B[][] b = other.a;
        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);

        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = zipFunction.apply(a[i][j], b[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return Matrix.of(result);
    }

    /**
     * Combines three matrices element-wise using the specified ternary function.
     * The function is applied to corresponding elements from all three matrices.
     * All matrices must have the same dimensions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> m1 = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Matrix<Integer> m2 = Matrix.of(new Integer[][] {{5, 6}, {7, 8}});
     * Matrix<Integer> m3 = Matrix.of(new Integer[][] {{9, 10}, {11, 12}});
     * Matrix<Integer> result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
     * // Result: {{15, 18}, {21, 24}}
     * }</pre>
     *
     * @param <B> the element type of the second matrix
     * @param <C> the element type of the third matrix
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix to zip with (must have the same dimensions, must not be {@code null})
     * @param third the third matrix to zip with (must have the same dimensions, must not be {@code null})
     * @param zipFunction the function to apply to corresponding elements (must not be {@code null})
     * @return a new matrix with the results of the zip function
     * @throws IllegalArgumentException if the matrices don't have the same dimensions, or if
     *         {@code zipFunction} is {@code null}
     * @throws ArrayStoreException if {@code zipFunction} returns a value that is not assignable to this matrix's runtime element type
     *         (use {@link #zipWith(Matrix, Matrix, Throwables.TriFunction, Class)} with an explicit target type to avoid this)
     * @throws E if the zip function throws an exception
     */
    public <B, C, E extends Exception> Matrix<T> zipWith(final Matrix<B> other, final Matrix<C> third,
            final Throwables.TriFunction<? super T, ? super B, ? super C, T, E> zipFunction) throws E {
        return zipWith(other, third, zipFunction, elementType);
    }

    /**
     * Combines three matrices element-wise using the specified ternary function.
     * The function can return elements of a different type than the input matrices.
     * All matrices must have the same dimensions. The operation may be performed
     * in parallel for large matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> m1 = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Matrix<String> m2 = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * Matrix<Double> m3 = Matrix.of(new Double[][] {{0.1, 0.2}, {0.3, 0.4}});
     * Matrix<String> result = m1.zipWith(m2, m3,
     *     (i, s, d) -> i + s + String.format("%.1f", d), String.class);
     * // Result: {{"1a0.1", "2b0.2"}, {"3c0.3", "4d0.4"}}
     * }</pre>
     *
     * @param <B> the element type of the second matrix
     * @param <C> the element type of the third matrix
     * @param <R> the element type of the result matrix
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix to zip with (must have the same dimensions, must not be {@code null})
     * @param third the third matrix to zip with (must have the same dimensions, must not be {@code null})
     * @param zipFunction the function to apply to corresponding elements (must not be {@code null})
     * @param targetElementType the class of the result element type (must not be {@code null})
     * @return a new matrix with the results of the zip function
     * @throws IllegalArgumentException if the matrices don't have the same shape, or if
     *         {@code zipFunction} or {@code targetElementType} is {@code null}
     * @throws E if the zip function throws an exception
     */
    public <B, C, R, E extends Exception> Matrix<R> zipWith(final Matrix<B> other, final Matrix<C> third,
            final Throwables.TriFunction<? super T, ? super B, ? super C, R, E> zipFunction, final Class<R> targetElementType)
            throws IllegalArgumentException, E {
        N.checkArgument(Matrices.isSameShape(this, other, third), "Cannot zip matrices with different shapes: all matrices must be {}x{}", rowCount,
                columnCount);
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgNotNull(targetElementType, "targetElementType");

        final B[][] b = other.a;
        final C[][] c = third.a;
        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);

        final Throwables.IntBiConsumer<E> operation = (i, j) -> result[i][j] = zipFunction.apply(a[i][j], b[i][j], c[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, operation, Matrices.isParallelizable(this));

        return Matrix.of(result);
    }

    /**
     * Returns a stream of elements on the main diagonal (upper-left to lower-right).
     * The matrix must be square (same number of rows and columns).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * Stream<Integer> diagonal = matrix.mainDiagonalStream();        // Stream of [1, 5, 9]
     * Object[] diag = matrix.mainDiagonalStream().toArray();         // Returns [1, 5, 9]
     * }</pre>
     *
     * @return a {@link Stream} of diagonal elements from top-left to bottom-right, or an empty stream if the matrix is empty
     * @throws IllegalStateException if the matrix is non-empty and not square
     */
    @Override
    public Stream<T> mainDiagonalStream() {
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
            public T next() {
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
     * The matrix must be square (same number of rows and columns).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * Stream<Integer> diagonal = matrix.antiDiagonalStream();        // Stream of [3, 5, 7]
     * Object[] diag = matrix.antiDiagonalStream().toArray();         // Returns [3, 5, 7]
     * }</pre>
     *
     * @return a {@link Stream} of anti-diagonal elements from top-right to bottom-left, or an empty stream if the matrix is empty
     * @throws IllegalStateException if the matrix is non-empty and not square
     */
    @Override
    public Stream<T> antiDiagonalStream() {
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
            public T next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T result = a[cursor][columnCount - cursor - 1];
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
     * Returns a stream of all elements in row-major order (horizontal).
     * Elements are streamed row by row from left to right, starting from the
     * top-left corner and proceeding to the bottom-right corner.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Stream<Integer> stream = matrix.horizontalStream();           // Stream of [1, 2, 3, 4]
     * Object[] array = matrix.horizontalStream().toArray();         // Returns [1, 2, 3, 4]
     * }</pre>
     *
     * @return a {@link Stream} of all elements in row-major order, or an empty stream if the matrix is empty
     */
    @Override
    public Stream<T> horizontalStream() {
        return horizontalStream(0, rowCount);
    }

    /**
     * Returns a stream of elements from a single row.
     * The elements are streamed from left to right within the specified row.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * Stream<Integer> row1 = matrix.horizontalStream(1);               // Stream of [4, 5, 6]
     * Object[] firstRow = matrix.horizontalStream(0).toArray();        // Returns [1, 2, 3]
     * }</pre>
     *
     * @param rowIndex the index of the row to stream (0-based)
     * @return a {@link Stream} of elements from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex} is negative or greater than or equal to {@code rowCount}
     */
    @Override
    public Stream<T> horizontalStream(final int rowIndex) {
        return horizontalStream(rowIndex, rowIndex + 1);
    }

    /**
     * Returns a stream of elements from a range of rows in row-major order.
     * Elements are streamed row by row from left to right within the specified row range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}, {5, 6}});
     * Stream<Integer> rows = matrix.horizontalStream(1, 3);           // Stream of [3, 4, 5, 6]
     * Object[] subArray = matrix.horizontalStream(0, 2).toArray();    // Returns [1, 2, 3, 4]
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a {@link Stream} of elements from the specified row range, or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromRowIndex} or {@code toRowIndex} is out of range
     */
    @Override
    public Stream<T> horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
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
            public T next() {
                if (i >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T result = a[i][j++];

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
                    c[k] = (A) a[i][j++];

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Stream<Integer> stream = matrix.verticalStream();              // Stream of [1, 3, 2, 4]
     * Object[] colMajor = matrix.verticalStream().toArray();         // Returns [1, 3, 2, 4]
     * }</pre>
     *
     * @return a {@link Stream} of all elements in column-major order, or an empty stream if the matrix is empty
     */
    @Override
    @Beta
    public Stream<T> verticalStream() {
        return verticalStream(0, columnCount);
    }

    /**
     * Returns a stream of elements from a single column.
     * The elements are streamed from top to bottom within the specified column.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * Stream<Integer> col1 = matrix.verticalStream(1);                // Stream of [2, 5, 8]
     * Object[] secondCol = matrix.verticalStream(1).toArray();        // Returns [2, 5, 8]
     * }</pre>
     *
     * @param columnIndex the index of the column to stream (0-based)
     * @return a {@link Stream} of elements from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex} is negative or greater than or equal to {@code columnCount}
     */
    @Override
    public Stream<T> verticalStream(final int columnIndex) {
        return verticalStream(columnIndex, columnIndex + 1);
    }

    /**
     * Returns a stream of elements from a range of columns in column-major order.
     * Elements are streamed column by column from top to bottom within the specified column range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * Stream<Integer> cols = matrix.verticalStream(1, 3);            // Stream of [2, 5, 3, 6]
     * Object[] colMajor = matrix.verticalStream(0, 2).toArray();     // Returns [1, 4, 2, 5]
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a {@link Stream} of elements from the specified column range, or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex} or {@code toColumnIndex} is out of range
     */
    @Beta
    @Override
    public Stream<T> verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
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
            public T next() {
                if (j >= toColumnIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T result = a[i++][j];

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

                if (n >= (long) (toColumnIndex - j) * Matrix.this.rowCount - i) {
                    i = 0;
                    j = toColumnIndex;
                } else {
                    final long offset = n + i;
                    i = (int) (offset % Matrix.this.rowCount);
                    j += (int) (offset / Matrix.this.rowCount);
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
                    c[k] = (A) a[i++][j];

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
     * Returns a stream of streams, where each inner stream represents a row.
     * The outer stream iterates over rows from top to bottom, and each inner stream
     * provides the elements of that row from left to right.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}, {5, 6}});
     * Stream<Stream<Integer>> rows = matrix.rowStreams();
     * // Outer stream contains 3 inner streams, each with row elements
     * }</pre>
     *
     * @return a {@link Stream} of row streams, with one inner stream per row in the matrix
     */
    @Override
    public Stream<Stream<T>> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Returns a stream of streams for a range of rows.
     * The outer stream iterates over rows in the specified range, and each inner stream
     * provides the elements of that row from left to right.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}, {5, 6}});
     * Stream<Stream<Integer>> rows = matrix.rowStreams(1, 3);
     * // Outer stream contains 2 inner streams for rows 1 and 2
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a {@link Stream} of row streams for the specified range, with one inner stream per row
     * @throws IndexOutOfBoundsException if {@code fromRowIndex} or {@code toRowIndex} is out of range
     */
    @Override
    public Stream<Stream<T>> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<T> next() {
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
     * Returns a stream of streams, where each inner stream represents a column.
     * The outer stream iterates over columns from left to right, and each inner stream
     * provides the elements of that column from top to bottom.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * Stream<Stream<Integer>> columns = matrix.columnStreams();
     * // Outer stream contains 3 inner streams, each with column elements
     * }</pre>
     *
     * @return a {@link Stream} of column streams, or an empty stream if the matrix is empty
     */
    @Override
    @Beta
    public Stream<Stream<T>> columnStreams() {
        return columnStreams(0, columnCount);
    }

    /**
     * Returns a stream of streams for a range of columns.
     * The outer stream iterates over columns in the specified range, and each inner stream
     * provides the elements of that column from top to bottom.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * Stream<Stream<Integer>> columns = matrix.columnStreams(1, 3);
     * // Outer stream contains 2 inner streams for columns 1 and 2
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a {@link Stream} of column streams for the specified range, or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex} or {@code toColumnIndex} is out of range
     */
    @Override
    @Beta
    public Stream<Stream<T>> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
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
            public Stream<T> next() {
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
                    public T next() {
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
     * Returns the length of the given array.
     *
     * <p>This is an internal helper method used by the abstract base class for iteration
     * and size calculations. It handles {@code null} arrays by returning {@code 0}.</p>
     *
     * @param a the array to check (may be {@code null})
     * @return the length of the array, or {@code 0} if the array is {@code null}
     */
    @Override
    protected int length(@SuppressWarnings("hiding") final T[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Applies the given action to each element in the matrix.
     * Elements are processed in row-major order (row by row, left to right) when executed sequentially.
     *
     * <p>The operation may be parallelized internally for large matrices to improve performance,
     * based on internal heuristics. If parallelized, the order of execution is not guaranteed,
     * but all elements will be processed exactly once.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     *
     * // Collect all values
     * List<Integer> values = new ArrayList<>();
     * matrix.forEach(value -> values.add(value));
     * // values now contains [1, 2, 3, 4]
     *
     * // Print all elements
     * matrix.forEach(element -> System.out.print(element + " "));
     * // Prints: 1 2 3 4
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element; receives each element value
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     * @see #forEach(int, int, int, int, Throwables.Consumer)
     */
    public <E extends Exception> void forEach(final Throwables.Consumer<? super T, E> action) throws E {
        forEach(0, rowCount, 0, columnCount, action);
    }

    /**
     * Applies the given action to each element in the specified sub-matrix region.
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
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     *
     * // Process only the center element
     * matrix.forEach(1, 2, 1, 2, value -> System.out.println(value));   // Prints: 5
     *
     * // Process a 2x2 sub-matrix
     * List<Integer> subMatrix = new ArrayList<>();
     * matrix.forEach(0, 2, 1, 3, value -> subMatrix.add(value));
     * // subMatrix contains [2, 3, 5, 6]
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @param action the action to be performed for each element; receives each element value
     * @throws IndexOutOfBoundsException if any of the row or column indices are out of range
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex,
            final Throwables.Consumer<? super T, E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        if (Matrices.isParallelizable(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> cmd = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, cmd, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                final T[] aa = a[i];

                for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                    action.accept(aa[j]);
                }
            }
        }
    }

    /**
     * Converts this matrix to a Dataset with horizontally organized data.
     * Each row in the matrix becomes a record in the Dataset, and each column
     * is assigned the corresponding name from the provided collection.
     *
     * <p>The column names are used in the order they appear in the collection,
     * and must match the number of columns in the matrix exactly.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * List<String> columnNames = N.asList("A", "B", "C");
     *
     * Dataset dataset = matrix.toRowDataset(columnNames);
     * // Dataset with:
     * // A  B  C
     * // -------
     * // 1  2  3
     * // 4  5  6
     *
     * // Access data by column name
     * List<Integer> columnA = dataset.getColumn("A");   // [1, 4]
     * }</pre>
     *
     * @param columnNames the names to assign to each column in the resulting Dataset; size must equal {@code columnCount}
     * @return a Dataset containing the matrix data with the specified column names
     *         (one row per matrix row)
     * @throws IllegalArgumentException if {@code columnNames} is {@code null}, or if its size
     *         does not equal {@code columnCount}
     * @see Dataset
     * @see #toColumnDataset(Collection)
     */
    @Beta
    public Dataset toRowDataset(final Collection<String> columnNames) throws IllegalArgumentException {
        N.checkArgNotNull(columnNames, "columnNames");
        N.checkArgument(columnNames.size() == columnCount, "The size({}) of specified columnNames and column count({}) of this Matrix are not equals",
                columnNames.size(), columnCount);

        final List<String> newColumnNameList = new ArrayList<>(columnNames);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnNameList.size());

        for (int j = 0; j < columnCount; j++) {
            final List<Object> column = new ArrayList<>(rowCount);

            for (int i = 0; i < rowCount; i++) {
                column.add(a[i][j]);
            }

            newColumnList.add(column);
        }

        return new RowDataset(newColumnNameList, newColumnList);
    }

    /**
     * Converts this matrix to a Dataset with vertically organized data.
     * Each row in this matrix becomes a column in the resulting Dataset, so the supplied
     * names are assigned to the Dataset's columns in the order they appear in the collection
     * and must match this matrix's {@code rowCount} exactly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * List<String> columnNames = N.asList("Row1", "Row2");
     *
     * Dataset dataset = matrix.toColumnDataset(columnNames);
     * // Dataset with:
     * // Row1  Row2
     * // ----------
     * // 1     4
     * // 2     5
     * // 3     6
     * }</pre>
     *
     * @param columnNames the column names of the resulting Dataset; size must equal {@code rowCount}
     * @return a Dataset containing the matrix data organized vertically (one column per matrix row)
     * @throws IllegalArgumentException if {@code columnNames} is {@code null}, or if its size
     *         does not equal {@code rowCount}
     * @see Dataset
     * @see RowDataset
     * @see #toRowDataset(Collection)
     */
    @Beta
    public Dataset toColumnDataset(final Collection<String> columnNames) throws IllegalArgumentException {
        N.checkArgNotNull(columnNames, "columnNames");
        N.checkArgument(columnNames.size() == rowCount, "The size({}) of specified columnNames and row count({}) of this Matrix are not equals",
                columnNames.size(), rowCount);

        final List<String> newColumnNameList = new ArrayList<>(columnNames);
        final List<List<Object>> newColumnList = new ArrayList<>(newColumnNameList.size());

        for (int i = 0; i < rowCount; i++) {
            newColumnList.add(new ArrayList<>(Array.asList(a[i])));
        }

        return new RowDataset(newColumnNameList, newColumnList);
    }

    /**
     * Prints this matrix to standard output and returns the printed string.
     * Each row is printed on a separate line with elements separated by commas
     * and enclosed in square brackets. Empty matrices print as {@code "[]"}.
     * {@code null} elements are rendered as {@code "null"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.println();
     * // Output:
     * // [1, 2, 3]
     * // [4, 5, 6]
     * }</pre>
     *
     * @return the string representation that was printed to standard output
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

                    final T[] row = a[i];
                    sb.append('[');

                    for (int j = 0, rowLen = row.length; j < rowLen; j++) {
                        if (j > 0) {
                            sb.append(", ");
                        }

                        sb.append(N.toString(row[j]));
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
     * The hash code is computed via {@link N#deepHashCode(Object[])} over the internal
     * two-dimensional array ({@link java.util.Arrays#deepHashCode(Object[])} semantics): a non-array
     * element contributes its own {@code hashCode()} (with {@code null} contributing {@code 0}), while an
     * array-typed element (for example a {@code Matrix<int[]>}) is hashed deeply by content. Matrices that
     * compare equal via {@link #equals(Object)} are guaranteed to produce the same hash code.
     *
     * @return a hash code value for this matrix
     */
    @Override
    public int hashCode() {
        return N.deepHashCode(a);
    }

    /**
     * Compares this matrix to the specified object for equality.
     * Returns {@code true} if the given object is also a {@code Matrix} with the same
     * dimensions and equal contents. Element comparison uses
     * {@link N#deepEquals(Object[], Object[])} semantics, which means each pair of
     * corresponding elements is compared with {@code Objects.equals(a, b)}
     * (i.e. value equality, with {@code null} equal only to {@code null}); element arrays,
     * if any, are compared deeply.
     *
     * <p>Because of generic-type erasure, the runtime check is {@code instanceof Matrix},
     * not {@code instanceof Matrix<T>}. Two matrices with different declared element types
     * may compare equal if their concrete elements are pairwise {@code equals}-equal.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> m1 = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Matrix<Integer> m2 = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * m1.equals(m2);   // true
     * }</pre>
     *
     * @param obj the object to compare with (may be {@code null})
     * @return {@code true} if the given object is a {@code Matrix} with the same dimensions
     *         and pairwise-equal elements; {@code false} otherwise (including when {@code obj}
     *         is {@code null} or is not a {@code Matrix})
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Matrix) {
            final Matrix<T> another = (Matrix<T>) obj;

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
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * System.out.println(matrix.toString());   // [[1, 2], [3, 4]]
     * }</pre>
     *
     * @return a string representation of this matrix
     */
    @Override
    public String toString() {
        return N.deepToString(a);
    }
}
