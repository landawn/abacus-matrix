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
 * value equality on elements via {@code N.deepEquals}/{@code N.deepHashCode} semantics
 * ({@code null} equal only to {@code null}; array-typed elements are compared and hashed deeply
 * by content), not reference identity.</p>
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
     * matrix.get(0, 0);                            // returns "A"
     * data[0][0] = "X";
     * matrix.get(0, 0);                            // returns "X" (backing array shared)
     *
     * new Matrix<>(new String[0][0]).rowCount();   // returns 0 (empty allowed)
     * new Matrix<>((String[][]) null);             // throws IllegalArgumentException
     * new Matrix<>(new Integer[][] {{1, 2}, {3}}); // throws IllegalArgumentException (not rectangular)
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
     * matrix.rowCount();      // returns 0
     * matrix.columnCount();   // returns 0
     * matrix.isEmpty();       // returns true
     * matrix.flatten();       // returns an empty list []
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
     * String[][] data = {{"a", "b"}, {"c", "d"}};
     * Matrix<String> matrix = Matrix.of(data);
     * matrix.get(1, 0);                            // returns "c"
     * data[0][0] = "x";
     * matrix.get(0, 0);                            // returns "x" (backing array shared)
     *
     * // Create a matrix with varargs
     * Matrix<Integer> numbers = Matrix.of(new Integer[] {1, 2, 3}, new Integer[] {4, 5, 6});
     * numbers.rowCount();                          // returns 2
     * numbers.columnCount();                       // returns 3
     *
     * Matrix.of((Integer[][]) null);                      // throws IllegalArgumentException
     * Matrix.of(new Integer[] {1, 2}, new Integer[] {3}); // throws IllegalArgumentException (not rectangular)
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
     * matrix.rowCount();             // returns 2
     * matrix.columnCount();          // returns 3
     * matrix.get(1, 2);              // returns "a"
     *
     * Matrix.repeat(1, 1, 7).get(0, 0);   // returns 7
     *
     * Matrix.repeat(2, 3, (String) null); // throws IllegalArgumentException (element is null)
     * Matrix.repeat(-1, 3, "a");          // throws IllegalArgumentException (negative dimension)
     * Matrix.repeat(0, 3, "a");           // throws IllegalArgumentException (unrepresentable shape: 0 rows, 3 columns)
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
        final T[][] a = Array.newInstance(element.getClass(), rowCount, columnCount);

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
     * diag.get(0, 0);   // returns 1
     * diag.get(1, 1);   // returns 2
     * diag.get(0, 1);   // returns null (off-diagonal)
     *
     * // Create a 2×2 diagonal matrix with strings
     * Matrix<String> strDiag = Matrix.mainDiagonal(new String[] {"A", "B"});
     * strDiag.get(0, 0);   // returns "A"
     * strDiag.get(0, 1);   // returns null (off-diagonal)
     *
     * Matrix.mainDiagonal((Integer[]) null);   // throws IllegalArgumentException
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
     * diag.get(0, 2);   // returns 1
     * diag.get(2, 0);   // returns 3
     * diag.get(0, 0);   // returns null (off anti-diagonal)
     *
     * // Create a 2×2 anti-diagonal matrix with strings
     * Matrix<String> strDiag = Matrix.antiDiagonal(new String[] {"X", "Y"});
     * strDiag.get(0, 1);   // returns "X"
     * strDiag.get(1, 0);   // returns "Y"
     *
     * Matrix.antiDiagonal((Integer[]) null);   // throws IllegalArgumentException
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
     * Matrix<String> diag = Matrix.diagonals(new String[] {"A", "B", "C"}, new String[] {"X", "Y", "Z"});
     * // Creates: [["A", null, "X"],
     * //           [null, "B", null],
     * //           ["Z", null, "C"]]
     * diag.get(0, 0);   // returns "A"
     * diag.get(0, 2);   // returns "X"
     * diag.get(1, 1);   // returns "B" (main diagonal wins at the center)
     * diag.get(2, 0);   // returns "Z"
     *
     * // With intersection (odd dimension): main diagonal takes precedence
     * Matrix<Integer> numbers = Matrix.diagonals(new Integer[] {1, 2, 3}, new Integer[] {7, 8, 9});
     * numbers.get(1, 1);   // returns 2 (main diagonal wins over anti-diagonal 8)
     * numbers.get(0, 2);   // returns 7
     *
     * Matrix.diagonals((Integer[]) null, (Integer[]) null);            // throws IllegalArgumentException (both null)
     * Matrix.diagonals(new Integer[] {1, 2}, new Integer[] {1, 2, 3}); // throws IllegalArgumentException (length mismatch)
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
     * matrix.get(1, 0);    // returns "C"
     * matrix.get(1, 1);    // returns "D"
     * matrix.get(5, 0);    // throws ArrayIndexOutOfBoundsException
     *
     * Matrix<String> withNull = Matrix.of(new String[][] {{"A", null}});
     * withNull.get(0, 1);  // returns null (null elements are permitted)
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
     * matrix.get(Point.of(1, 2));   // returns "f" (same as matrix.get(1, 2))
     * matrix.get(Point.of(0, 0));   // returns "a"
     * matrix.get(Point.of(5, 0));   // throws ArrayIndexOutOfBoundsException
     * matrix.get((Point) null);     // throws IllegalArgumentException
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
     * matrix.get(1, 2);            // returns "newValue"
     * matrix.set(0, 0, null);
     * matrix.get(0, 0);            // returns null (null elements are permitted)
     * matrix.set(5, 0, "x");       // throws ArrayIndexOutOfBoundsException
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
     * matrix.set(Point.of(0, 1), "X");   // same as matrix.set(0, 1, "X")
     * matrix.get(0, 1);                  // returns "X"
     * matrix.set(Point.of(5, 0), "x");   // throws ArrayIndexOutOfBoundsException
     * matrix.set((Point) null, "x");     // throws IllegalArgumentException
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
     * matrix.valueAbove(1, 0).get();        // returns "A"
     * matrix.valueAbove(1, 1).get();        // returns "B"
     * matrix.valueAbove(0, 0).isEmpty();    // returns true (no row above the top edge)
     * matrix.valueAbove(5, 0);              // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return a {@link Nullable} containing the element at position {@code (rowIndex - 1, columnIndex)},
     *         or {@link Nullable#empty()} if {@code rowIndex == 0}. Note that a non-empty {@code Nullable}
     *         may itself contain {@code null} since {@code null} elements are permitted in the matrix.
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
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
     * matrix.valueBelow(0, 0).get();        // returns "C"
     * matrix.valueBelow(0, 1).get();        // returns "D"
     * matrix.valueBelow(1, 0).isEmpty();    // returns true (no row below the bottom edge)
     * matrix.valueBelow(0, 5);              // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return a {@link Nullable} containing the element at position {@code (rowIndex + 1, columnIndex)},
     *         or {@link Nullable#empty()} if {@code rowIndex == rowCount - 1}. Note that a non-empty
     *         {@code Nullable} may itself contain {@code null} since {@code null} elements are permitted
     *         in the matrix.
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
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
     * matrix.valueLeft(0, 1).get();        // returns "A"
     * matrix.valueLeft(1, 1).get();        // returns "C"
     * matrix.valueLeft(0, 0).isEmpty();    // returns true (no column to the left of the leftmost edge)
     * matrix.valueLeft(5, 0);              // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return a {@link Nullable} containing the element at position {@code (rowIndex, columnIndex - 1)},
     *         or {@link Nullable#empty()} if {@code columnIndex == 0}. Note that a non-empty
     *         {@code Nullable} may itself contain {@code null} since {@code null} elements are permitted
     *         in the matrix.
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
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
     * matrix.valueRight(0, 0).get();        // returns "B"
     * matrix.valueRight(1, 0).get();        // returns "D"
     * matrix.valueRight(0, 1).isEmpty();    // returns true (no column to the right of the rightmost edge)
     * matrix.valueRight(0, 5);              // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return a {@link Nullable} containing the element at position {@code (rowIndex, columnIndex + 1)},
     *         or {@link Nullable#empty()} if {@code columnIndex == columnCount - 1}. Note that a non-empty
     *         {@code Nullable} may itself contain {@code null} since {@code null} elements are permitted
     *         in the matrix.
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
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
     * rowData[0] = "X";          // modifies the matrix directly (live view)
     * matrix.get(0, 0);          // returns "X"
     *
     * // Use clone() if you need an independent copy
     * String[] rowCopy = matrix.rowView(1).clone();
     * rowCopy[0] = "Y";          // does NOT affect the matrix
     * matrix.get(1, 0);          // returns "C"
     *
     * matrix.rowView(5);         // throws IndexOutOfBoundsException (row index out of bounds)
     * matrix.rowView(-1);        // throws IndexOutOfBoundsException (negative index)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return the live internal row array
     * @throws IndexOutOfBoundsException if {@code rowIndex} is negative or greater than or equal to {@code rowCount}
     * @see #rowCopy(int)
     */
    @Override
    public T[] rowView(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

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
     * String[] rowCopy = matrix.rowCopy(1);   // returns ["C", "D"]
     * rowCopy[0] = "X";                       // does NOT affect the matrix
     * matrix.get(1, 0);                       // returns "C"
     *
     * matrix.rowCopy(5);    // throws IndexOutOfBoundsException (row index out of bounds)
     * matrix.rowCopy(-1);   // throws IndexOutOfBoundsException (negative index)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new array containing the values from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex} is negative or greater than or equal to {@code rowCount}
     * @see #rowView(int)
     */
    @Override
    public T[] rowCopy(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

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
     * String[] colData = matrix.columnCopy(1);   // returns ["B", "D"]
     * colData[0] = "X";                          // does NOT affect the matrix (it's a copy)
     * matrix.get(0, 1);                          // returns "B"
     *
     * matrix.columnCopy(5);    // throws IndexOutOfBoundsException (column index out of bounds)
     * matrix.columnCopy(-1);   // throws IndexOutOfBoundsException (negative index)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new array containing the values from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex} is negative or greater than or equal to {@code columnCount}
     */
    @Override
    public T[] columnCopy(final int columnIndex) throws IndexOutOfBoundsException {
        checkColumnIndex(columnIndex);

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
     * matrix.setRow(0, new String[] {"X", "Y"});
     * matrix.get(0, 0);                              // returns "X"
     * matrix.get(0, 1);                              // returns "Y"
     *
     * matrix.setRow(0, (String[]) null);            // throws IllegalArgumentException (null row)
     * matrix.setRow(0, new String[] {"X"});         // throws IllegalArgumentException (length != columnCount)
     * matrix.setRow(5, new String[] {"X", "Y"});    // throws IndexOutOfBoundsException (row index out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index to replace (0-based)
     * @param row the new row data (must have exactly {@code columnCount} elements)
     * @throws IndexOutOfBoundsException if {@code rowIndex} is out of bounds
     * @throws IllegalArgumentException if {@code row} is {@code null} or if {@code row.length} does not equal {@code columnCount}
     * @throws ArrayStoreException if any element of {@code row} is not assignable to the row's
     *         runtime storage component type
     */
    public void setRow(final int rowIndex, final T[] row) throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkArgNotNull(row, "row");
        checkRowIndex(rowIndex);
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
     * matrix.setColumn(1, new String[] {"X", "Y"});
     * matrix.get(0, 1);                                 // returns "X"
     * matrix.get(1, 1);                                 // returns "Y"
     *
     * matrix.setColumn(1, (String[]) null);            // throws IllegalArgumentException (null column)
     * matrix.setColumn(1, new String[] {"X"});         // throws IllegalArgumentException (length != rowCount)
     * matrix.setColumn(5, new String[] {"X", "Y"});    // throws IndexOutOfBoundsException (column index out of bounds)
     * }</pre>
     *
     * @param columnIndex the column index to replace (0-based)
     * @param column the new column data (must have exactly {@code rowCount} elements)
     * @throws IndexOutOfBoundsException if {@code columnIndex} is out of bounds
     * @throws IllegalArgumentException if {@code column} is {@code null} or if {@code column.length} does not equal {@code rowCount}
     * @throws ArrayStoreException if any element of {@code column} is not assignable to the
     *         corresponding row's runtime storage component type
     */
    public void setColumn(final int columnIndex, final T[] column) throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkArgNotNull(column, "column");
        checkColumnIndex(columnIndex);
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
     * matrix.rowCopy(0);                            // returns ["A", "B"]
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * numMatrix.updateRow(0, x -> x * 2);
     * numMatrix.rowCopy(0);                         // returns [2, 4]
     *
     * matrix.updateRow(5, String::toUpperCase);     // throws IndexOutOfBoundsException
     * matrix.updateRow(0, null);                    // throws IllegalArgumentException (null operator)
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
        checkRowIndex(rowIndex);

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
     * matrix.columnCopy(1);                         // returns ["b_suffix", "d_suffix"]
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * numMatrix.updateColumn(0, n -> n * n);
     * numMatrix.columnCopy(0);                      // returns [1, 9]
     *
     * matrix.updateColumn(5, s -> s);               // throws IndexOutOfBoundsException
     * matrix.updateColumn(0, null);                 // throws IllegalArgumentException (null operator)
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
        checkColumnIndex(columnIndex);

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
     * Matrix<Integer> m = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * m.getMainDiagonal();   // returns [1, 5, 9]
     *
     * Matrix<Integer> single = Matrix.of(new Integer[][] {{42}});
     * single.getMainDiagonal();   // returns [42]
     *
     * Matrix<Integer> nonSquare = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.getMainDiagonal();   // throws IllegalStateException (not square)
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
     * <p>This method sets the main diagonal elements at positions (0,0), (1,1), (2,2), etc.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> m = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * m.setMainDiagonal(new Integer[] {10, 20, 30});
     * m.getMainDiagonal();   // returns [10, 20, 30]
     * m.get(0, 1);           // returns 2 (off-diagonal unchanged)
     *
     * Matrix<Integer> nonSquare = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.setMainDiagonal(new Integer[] {1, 2});   // throws IllegalStateException (not square)
     * m.setMainDiagonal(new Integer[] {1, 2});           // throws IllegalArgumentException (length != rowCount)
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must be non-{@code null} and have length equal to {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null} or its length does not equal {@code rowCount}
     * @throws ArrayStoreException if any element is not assignable to the row's runtime storage component type
     */
    @Override
    public void setMainDiagonal(final T[] mainDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgNotNull(mainDiagonal, "mainDiagonal");
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
     * matrix.updateMainDiagonal(x -> x * 2);
     * matrix.getMainDiagonal();   // returns [2, 10, 18]
     *
     * matrix.updateMainDiagonal(x -> 0);
     * matrix.getMainDiagonal();   // returns [0, 0, 0]
     *
     * Matrix<Integer> nonSquare = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.updateMainDiagonal(x -> x);   // throws IllegalStateException (not square)
     * matrix.updateMainDiagonal(null);        // throws IllegalArgumentException (null operator)
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
     * Matrix<Integer> m = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * m.getAntiDiagonal();   // returns [3, 5, 7]
     *
     * Matrix<Integer> single = Matrix.of(new Integer[][] {{42}});
     * single.getAntiDiagonal();   // returns [42]
     *
     * Matrix<Integer> nonSquare = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.getAntiDiagonal();   // throws IllegalStateException (not square)
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
     * Matrix<Integer> m = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * m.setAntiDiagonal(new Integer[] {10, 20, 30});
     * m.getAntiDiagonal();   // returns [10, 20, 30]
     * m.get(0, 2);           // returns 10
     * m.get(2, 0);           // returns 30
     *
     * Matrix<Integer> nonSquare = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.setAntiDiagonal(new Integer[] {1, 2});   // throws IllegalStateException (not square)
     * m.setAntiDiagonal(new Integer[] {1, 2});           // throws IllegalArgumentException (length != rowCount)
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must be non-{@code null} and have length equal to {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null} or its length does not equal {@code rowCount}
     * @throws ArrayStoreException if any element is not assignable to the row's runtime storage component type
     */
    @Override
    public void setAntiDiagonal(final T[] antiDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgNotNull(antiDiagonal, "antiDiagonal");
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
     * matrix.updateAntiDiagonal(x -> -x);
     * matrix.getAntiDiagonal();   // returns [-3, -5, -7]
     *
     * Matrix<String> strMatrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * strMatrix.updateAntiDiagonal(String::toLowerCase);
     * strMatrix.getAntiDiagonal();   // returns ["b", "c"]
     *
     * Matrix<Integer> nonSquare = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.updateAntiDiagonal(x -> x);   // throws IllegalStateException (not square)
     * matrix.updateAntiDiagonal(null);        // throws IllegalArgumentException (null operator)
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
     * strMatrix.get(0, 0);   // returns "A"
     * strMatrix.get(1, 1);   // returns "D"
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * numMatrix.updateAll(x -> x * x);
     * numMatrix.get(0, 0);   // returns 1
     * numMatrix.get(1, 1);   // returns 16
     *
     * numMatrix.updateAll((Throwables.UnaryOperator<Integer, RuntimeException>) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown by the operator
     * @param operator the operator to apply to each element (must not be {@code null})
     * @throws E if the operator throws an exception
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     */
    public <E extends Exception> void updateAll(final Throwables.UnaryOperator<T, E> operator) throws E {
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            final T[] currentRow = a[i];

            for (int j = 0; j < columnCount; j++) {
                currentRow[j] = operator.apply(currentRow[j]);
            }
        }
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
     * matrix.get(0, 0);   // returns "black"
     * matrix.get(0, 1);   // returns "white"
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{0, 0}, {0, 0}});
     * numMatrix.updateAll((i, j) -> i * 10 + j);
     * numMatrix.get(1, 0);   // returns 10
     * numMatrix.get(1, 1);   // returns 11
     *
     * numMatrix.updateAll((Throwables.IntBiFunction<Integer, RuntimeException>) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <E> the type of exception that might be thrown by the mapper
     * @param mapper the function that takes row and column indices and returns the new value (must not be {@code null})
     * @throws E if the mapper throws an exception
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends T, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");

        for (int i = 0; i < rowCount; i++) {
            final T[] currentRow = a[i];

            for (int j = 0; j < columnCount; j++) {
                currentRow[j] = mapper.apply(i, j);
            }
        }
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
     * matrix.replaceIf(x -> x == null, "");     // replace all null values with empty string
     * matrix.get(0, 2);                         // returns "" (was null)
     * matrix.replaceIf(String::isEmpty, "N/A"); // replace empty strings with placeholder
     * matrix.get(0, 1);                         // returns "N/A"
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{-1, 2}, {3, -4}});
     * numMatrix.replaceIf(x -> x < 0, 0);      // replace negative numbers with zero
     * numMatrix.get(0, 0);                     // returns 0 (was -1)
     * numMatrix.get(0, 1);                     // returns 2 (unchanged)
     *
     * numMatrix.replaceIf((Throwables.Predicate<Integer, RuntimeException>) null, 0);   // throws IllegalArgumentException
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

        for (int i = 0; i < rowCount; i++) {
            final T[] currentRow = a[i];

            for (int j = 0; j < columnCount; j++) {
                if (predicate.test(currentRow[j])) {
                    currentRow[j] = newValue;
                }
            }
        }
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
     * matrix.replaceIf((i, j) -> i == j, 0);   // replace diagonal elements with zero
     * matrix.getMainDiagonal();                // returns [0, 0, 0]
     *
     * matrix.replaceIf((i, j) -> i < j, null); // replace upper triangle with null
     * matrix.get(0, 1);                        // returns null
     * matrix.get(1, 0);                        // returns 4 (lower triangle unchanged)
     *
     * matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0);   // throws IllegalArgumentException
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

        for (int i = 0; i < rowCount; i++) {
            final T[] currentRow = a[i];

            for (int j = 0; j < columnCount; j++) {
                if (predicate.test(i, j)) {
                    currentRow[j] = newValue;
                }
            }
        }
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
     * upper.get(0, 0);   // returns "A"
     * upper.get(1, 1);   // returns "D"
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Matrix<Integer> doubled = numMatrix.map(x -> x * 2);
     * doubled.get(0, 0);   // returns 2
     * doubled.get(1, 1);   // returns 8
     *
     * numMatrix.map((Throwables.UnaryOperator<Integer, RuntimeException>) null);   // throws IllegalArgumentException
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
     * strings.get(0, 0);   // returns "1"
     *
     * Matrix<String> strMatrix = Matrix.of(new String[][] {{"1.5", "2.5"}, {"3.5", "4.5"}});
     * // Convert String matrix to Double matrix
     * Matrix<Double> doubles = strMatrix.map(Double::parseDouble, Double.class);
     * doubles.get(0, 0);   // returns 1.5
     *
     * Matrix<Boolean> booleans = numMatrix.map(x -> x != null && x > 0, Boolean.class);
     * booleans.get(0, 0);  // returns true
     *
     * numMatrix.map(Object::toString, null);   // throws IllegalArgumentException (null target type)
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
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.apply(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

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
     * BooleanMatrix nullMask = matrix.mapToBoolean(x -> x == null);
     * nullMask.get(0, 0);   // returns false
     * nullMask.get(0, 1);   // returns true
     *
     * Matrix<Integer> numMatrix = Matrix.of(new Integer[][] {{1, -2}, {3, -4}});
     * BooleanMatrix positive = numMatrix.mapToBoolean(x -> x > 0);
     * positive.get(0, 0);   // returns true
     * positive.get(0, 1);   // returns false
     *
     * numMatrix.mapToBoolean(null);   // throws IllegalArgumentException (null mapper)
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
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsBoolean(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

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
     * bytes.get(0, 0);   // returns (byte) 1
     *
     * Matrix<String> strMatrix = Matrix.of(new String[][] {{"A", "B"}, {"C", "D"}});
     * ByteMatrix firstChars = strMatrix.mapToByte(s -> (byte) s.charAt(0));
     * firstChars.get(0, 0);   // returns (byte) 'A' (65)
     *
     * matrix.mapToByte(null);   // throws IllegalArgumentException (null mapper)
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
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsByte(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

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
     * firstChars.get(0, 0);   // returns 'a'
     * firstChars.get(1, 1);   // returns 'j'
     *
     * Matrix<Integer> scores = Matrix.of(new Integer[][] {{95, 85}, {78, 92}});
     * CharMatrix grades = scores.mapToChar(score -> score >= 90 ? 'A' : score >= 80 ? 'B' : 'C');
     * grades.get(0, 0);   // returns 'A'
     * grades.get(1, 0);   // returns 'C'
     *
     * scores.mapToChar(null);   // throws IllegalArgumentException (null mapper)
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
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsChar(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

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
     * shorts.get(0, 0);   // returns (short) 1
     * shorts.get(1, 1);   // returns (short) 4
     *
     * // Calculate hash codes as shorts
     * ShortMatrix hashes = matrix.mapToShort(x -> (short) x.hashCode());
     *
     * matrix.mapToShort(null);   // throws IllegalArgumentException (null mapper)
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
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsShort(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

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
     * lengths.get(0, 0);   // returns 3
     * lengths.get(1, 1);   // returns 4
     *
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * IntMatrix ints = matrix.mapToInt(x -> x.intValue());
     * ints.get(0, 0);   // returns 1
     *
     * matrix.mapToInt(null);   // throws IllegalArgumentException (null mapper)
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
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsInt(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

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
     * longs.get(0, 0);   // returns 1L
     *
     * // Calculate large values
     * LongMatrix big = matrix.mapToLong(x -> (long) x * 1_000_000_000L);
     * big.get(0, 0);   // returns 1_000_000_000L
     *
     * matrix.mapToLong(null);   // throws IllegalArgumentException (null mapper)
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
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsLong(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

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
     * floats.get(0, 0);   // returns 1.0f
     *
     * // Calculate percentages
     * FloatMatrix percents = matrix.mapToFloat(x -> x / 100.0f);
     * percents.get(0, 0);   // returns 0.01f
     *
     * matrix.mapToFloat(null);   // throws IllegalArgumentException (null mapper)
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
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsFloat(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

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
     * doubles.get(0, 0);   // returns 1.0
     *
     * // Calculate square roots
     * DoubleMatrix results = matrix.mapToDouble(x -> Math.sqrt(x));
     * results.get(0, 1);   // returns 2.0
     * results.get(1, 1);   // returns 4.0
     *
     * matrix.mapToDouble(null);   // throws IllegalArgumentException (null mapper)
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
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsDouble(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

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
     * matrix.fill("");
     * matrix.get(0, 0);          // returns ""
     * matrix.fill("default");
     * matrix.get(1, 1);          // returns "default"
     * matrix.fill((String) null);
     * matrix.get(0, 0);          // returns null (null values are permitted)
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
     * Source rows that are {@code null} are skipped (the corresponding destination row is left untouched).
     *
     * <p>This method modifies the matrix in-place.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<String> matrix = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * matrix.fill(new String[][] {{"A", "B"}, {"C", "D"}});   // copy from top-left
     * matrix.get(0, 0);                                       // returns "A"
     * matrix.get(1, 1);                                       // returns "D"
     *
     * Matrix<String> partial = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * partial.fill(new String[][] {{"Z"}});   // smaller source: only (0,0) overwritten
     * partial.get(0, 0);                      // returns "Z"
     * partial.get(0, 1);                      // returns "b" (unchanged)
     *
     * matrix.fill((String[][]) null);   // throws IllegalArgumentException
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
     * matrix.fill(1, 2, new String[][] {{"X", "Y"}, {"Z", "W"}});   // start at row 1, column 2 (truncated at edges)
     * matrix.get(1, 2);                                             // returns "X"
     * matrix.get(2, 2);                                             // returns "Z"
     * matrix.get(1, 1);                                             // returns "E" (unchanged)
     *
     * matrix.fill(0, 0, (String[][]) null);            // throws IllegalArgumentException (null source)
     * matrix.fill(5, 0, new String[][] {{"X"}});       // throws IndexOutOfBoundsException (destRowIndex out of range)
     * }</pre>
     *
     * @param destRowIndex the target row index (0-based, must be between 0 and rowCount inclusive)
     * @param destColumnIndex the target column index (0-based, must be between 0 and columnCount inclusive)
     * @param source the source two-dimensional array to copy values from (must not be {@code null})
     * @throws IndexOutOfBoundsException if {@code destRowIndex < 0} or {@code destRowIndex > rowCount},
     *         or if {@code destColumnIndex < 0} or {@code destColumnIndex > columnCount}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final T[][] source) throws IndexOutOfBoundsException, IllegalArgumentException {
        N.checkArgNotNull(source, "source");
        if (destRowIndex < 0 || destRowIndex > rowCount) {
            throw new IndexOutOfBoundsException(formatMsg("destRowIndex({}) must be between 0 and rowCount({})", destRowIndex, rowCount));
        }
        if (destColumnIndex < 0 || destColumnIndex > columnCount) {
            throw new IndexOutOfBoundsException(formatMsg("destColumnIndex({}) must be between 0 and columnCount({})", destColumnIndex, columnCount));
        }

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
     * copy.set(0, 0, "X");
     * original.get(0, 0);    // returns "A" (original unchanged)
     * copy.get(0, 0);        // returns "X"
     * copy.equals(original); // returns false (after the edit)
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
     * Matrix<Integer> subset = matrix.copy(1, 3);   // copies rows 1 and 2 (exclusive end)
     * subset.rowCount();                            // returns 2
     * subset.get(0, 0);                             // returns 3
     * subset.get(1, 1);                             // returns 6
     *
     * matrix.copy(1, 5);    // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.copy(2, 1);    // throws IndexOutOfBoundsException (fromRowIndex > toRowIndex)
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
     * Matrix<Integer> submatrix = matrix.copy(0, 2, 1, 3);   // rows 0-1, columns 1-2
     * submatrix.get(0, 0);                                   // returns 2
     * submatrix.get(0, 1);                                   // returns 3
     * submatrix.get(1, 1);                                   // returns 6
     *
     * matrix.copy(0, 2, 1, 5);   // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.copy(0, 5, 0, 2);   // throws IndexOutOfBoundsException (toRowIndex > rowCount)
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
     * grown.get(0, 0);   // returns "a"
     * grown.get(0, 3);   // returns null (new cell)
     * grown.get(3, 3);   // returns null (new cell)
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * Matrix<String> truncated = matrix.resize(2, 2);
     * truncated.rowCount();   // returns 2
     * truncated.get(1, 1);    // returns "e"
     *
     * // Mixed: grow rows, truncate columns
     * Matrix<String> mixed = matrix.resize(4, 2);
     * mixed.get(2, 0);   // returns "g"
     * mixed.get(3, 0);   // returns null (new row)
     *
     * matrix.resize(-1, 2);   // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new Matrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
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
     * grown.get(0, 0);   // returns "a"
     * grown.get(0, 3);   // returns "x" (new cell)
     * grown.get(3, 3);   // returns "x" (new cell)
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * Matrix<String> truncated = matrix.resize(2, 2, "x");
     * truncated.get(1, 1);   // returns "e"
     *
     * // Mixed: grow rows, truncate columns
     * Matrix<String> mixed = matrix.resize(4, 2, "x");
     * mixed.get(2, 0);   // returns "g"
     * mixed.get(3, 0);   // returns "x" (new row)
     *
     * matrix.resize(-1, 2, "x");   // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the value used to fill any newly created cells; may be {@code null}
     * @return a new Matrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
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
            final int columnCountToCopy = N.min(columnCount, newColumnCount);

            for (int i = 0; i < newRowCount; i++) {
                b[i] = N.newArray(elementType, newColumnCount);

                if (i < rowCount && columnCountToCopy > 0) {
                    N.copy(a[i], 0, b[i], 0, columnCountToCopy);
                }

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
     * bordered.rowCount();      // returns 3
     * bordered.columnCount();   // returns 4
     * bordered.get(1, 1);       // returns "a" (original content preserved)
     * bordered.get(0, 0);       // returns null (padding cell)
     * bordered.get(2, 3);       // returns null (padding cell)
     *
     * matrix.extend(-1, 0, 0, 0);   // throws IllegalArgumentException (negative padding)
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
     * padded.rowCount();      // returns 3
     * padded.columnCount();   // returns 5
     * padded.get(1, 2);       // returns "a" (original content preserved)
     * padded.get(0, 0);       // returns "x" (padding cell)
     * padded.get(1, 0);       // returns "x" (padding cell)
     *
     * // Uniform border of 1 cell on every side, filled with null
     * Matrix<String> bordered = matrix.extend(1, 1, 1, 1, null);
     * bordered.get(0, 0);   // returns null
     * bordered.get(1, 1);   // returns "a"
     *
     * matrix.extend(-1, 0, 0, 0, "x");   // throws IllegalArgumentException (negative padding)
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
     * matrix.rowCopy(0);   // returns [3, 2, 1]
     * matrix.rowCopy(1);   // returns [6, 5, 4]
     *
     * Matrix<Integer> single = Matrix.of(new Integer[][] {{7}});
     * single.flipHorizontallyInPlace();
     * single.get(0, 0);    // returns 7 (single column unchanged)
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
     * matrix.rowCopy(0);   // returns [5, 6]
     * matrix.rowCopy(2);   // returns [1, 2]
     *
     * Matrix<Integer> single = Matrix.of(new Integer[][] {{7, 8}});
     * single.flipVerticallyInPlace();
     * single.rowCopy(0);   // returns [7, 8] (single row unchanged)
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
     * flipped.rowCopy(0);   // returns [3, 2, 1]
     * matrix.get(0, 0);     // returns 1 (original unchanged)
     *
     * Matrix<Integer> empty = Matrix.empty();
     * empty.flipHorizontally().isEmpty();   // returns true
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
     * flipped.rowCopy(0);   // returns [5, 6]
     * matrix.get(0, 0);     // returns 1 (original unchanged)
     *
     * Matrix<Integer> empty = Matrix.empty();
     * empty.flipVertically().isEmpty();   // returns true
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
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * Matrix<Integer> rotated = matrix.rotate90();
     * rotated.rowCopy(0);   // returns [7, 4, 1]
     * rotated.rowCopy(2);   // returns [9, 6, 3]
     *
     * Matrix<Integer> empty = Matrix.empty();
     * empty.rotate90().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 90 degrees clockwise
     * @throws IllegalArgumentException if the resulting (transposed) shape is not representable
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
     * Returns a new matrix that is this matrix rotated 180 degrees.
     * This is equivalent to flipping both horizontally and vertically, reversing the
     * order of all elements. The resulting matrix has the same dimensions as the original.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    Rotated 180°:
     * // 1 2 3        6 5 4
     * // 4 5 6   =>   3 2 1
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * Matrix<Integer> rotated = matrix.rotate180();
     * rotated.rowCopy(0);   // returns [6, 5, 4]
     * rotated.rowCopy(1);   // returns [3, 2, 1]
     *
     * Matrix<Integer> empty = Matrix.empty();
     * empty.rotate180().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 180 degrees
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
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * Matrix<Integer> rotated = matrix.rotate270();
     * rotated.rowCopy(0);   // returns [3, 6, 9]
     * rotated.rowCopy(2);   // returns [1, 4, 7]
     *
     * Matrix<Integer> empty = Matrix.empty();
     * empty.rotate270().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 270 degrees clockwise
     * @throws IllegalArgumentException if the resulting (transposed) shape is not representable
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
     * Matrix<Integer> original = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * Matrix<Integer> transposed = original.transpose();   // 2×3 becomes 3×2
     * transposed.rowCount();                               // returns 3
     * transposed.columnCount();                            // returns 2
     * transposed.rowCopy(0);                               // returns [1, 4]
     * transposed.rowCopy(2);                               // returns [3, 6]
     *
     * Matrix<Integer> empty = Matrix.empty();
     * empty.transpose().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new matrix that is the transpose of this matrix, with dimensions
     *         {@code columnCount × rowCount} (an empty matrix with zero columns yields
     *         an empty {@code 0}-row matrix)
     * @throws IllegalArgumentException if the resulting (transposed) shape is not representable
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
     * Matrix<Integer> reshaped = matrix.reshape(3, 2);
     * reshaped.rowCopy(0);   // returns [1, 2]
     * reshaped.rowCopy(2);   // returns [5, 6]
     *
     * Matrix<Integer> extended = matrix.reshape(2, 4);   // extra cells filled with null
     * extended.rowCopy(0);                               // returns [1, 2, 3, 4]
     * extended.rowCopy(1);                               // returns [5, 6, null, null]
     *
     * matrix.reshape(1, 2);   // throws IllegalArgumentException (new shape too small for 6 elements)
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix (must be non-negative)
     * @param newColumnCount the number of columns in the reshaped matrix (must be non-negative)
     * @return a new Matrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if the new shape is too small to hold all elements
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public Matrix<T> reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        checkMaterializableShape(newRowCount, newColumnCount);
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
     * repeated.rowCount();      // returns 4
     * repeated.columnCount();   // returns 6
     * repeated.rowCopy(0);      // returns [1, 1, 1, 2, 2, 2]
     * repeated.rowCopy(3);      // returns [3, 3, 3, 4, 4, 4]
     *
     * matrix.repeatElements(0, 1);   // throws IllegalArgumentException (rowRepeats < 1)
     * matrix.repeatElements(1, 0);   // throws IllegalArgumentException (columnRepeats < 1)
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
     * tiled.rowCount();      // returns 4
     * tiled.columnCount();   // returns 6
     * tiled.rowCopy(0);      // returns [1, 2, 1, 2, 1, 2]
     * tiled.rowCopy(3);      // returns [3, 4, 3, 4, 3, 4]
     *
     * matrix.repeatMatrix(0, 1);   // throws IllegalArgumentException (rowRepeats < 1)
     * matrix.repeatMatrix(1, 0);   // throws IllegalArgumentException (columnRepeats < 1)
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
     * matrix.flatten();   // returns [1, 2, 3, 4, 5, 6]
     *
     * Matrix<String> empty = Matrix.empty();
     * empty.flatten();           // returns [] (empty list)
     * empty.flatten().isEmpty(); // returns true
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
     * matrix.mutateAsFlat(arr -> java.util.Arrays.sort(arr));   // sort across the whole matrix
     * matrix.rowCopy(0);                                        // returns [1, 2, 3]
     * matrix.rowCopy(1);                                        // returns [4, 5, 6]
     *
     * Matrix<Integer> reversible = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * reversible.mutateAsFlat(arr -> { for (int i = 0; i < arr.length / 2; i++) { Integer t = arr[i]; arr[i] = arr[arr.length - 1 - i]; arr[arr.length - 1 - i] = t; } });
     * reversible.rowCopy(0);   // returns [4, 3]
     * }</pre>
     *
     * @param <E> the type of exception that the operation may throw
     * @param action the operation to apply to the flattened array
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the operation throws an exception
     * @see Arrays.ff#mutateAsFlat(Object[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super T[], E> action) throws E {
        N.checkArgNotNull(action, "action");

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
     * stacked.rowCount();    // returns 4
     * stacked.rowCopy(2);    // returns [5, 6]
     *
     * Matrix<Integer> mismatched = Matrix.of(new Integer[][] {{1, 2, 3}});
     * m1.stackVertically(mismatched);               // throws IllegalArgumentException (different column counts)
     * m1.stackVertically((Matrix<Integer>) null);   // throws IllegalArgumentException (null other)
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
     * stacked.columnCount();   // returns 3
     * stacked.rowCopy(0);      // returns [1, 2, 5]
     * stacked.rowCopy(1);      // returns [3, 4, 6]
     *
     * Matrix<Integer> mismatched = Matrix.of(new Integer[][] {{5}});
     * m1.stackHorizontally(mismatched);               // throws IllegalArgumentException (different row counts)
     * m1.stackHorizontally((Matrix<Integer>) null);   // throws IllegalArgumentException (null other)
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
     * sum.rowCopy(0);   // returns [6, 8]
     * sum.rowCopy(1);   // returns [10, 12]
     *
     * Matrix<Integer> mismatched = Matrix.of(new Integer[][] {{1, 2, 3}});
     * m1.zipWith(mismatched, (a, b) -> a + b);   // throws IllegalArgumentException (different shapes)
     * }</pre>
     *
     * @param <B> the element type of the other matrix
     * @param <E> the type of exception that the zip function may throw
     * @param other the other matrix to zip with (must have the same dimensions, must not be {@code null})
     * @param zipFunction the binary function to apply to corresponding elements (must not be {@code null})
     * @return a new matrix with the results of the zip function
     * @throws IllegalArgumentException if {@code other} or {@code zipFunction} is {@code null}, or if the
     *         matrices have different shapes
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
     * result.get(0, 0);   // returns "1:0.5"
     * result.get(1, 1);   // returns "4:2.0"
     *
     * Matrix<Double> mismatched = Matrix.of(new Double[][] {{0.5, 1.0, 1.5}});
     * m1.zipWith(mismatched, (a, b) -> a + ":" + b, String.class);   // throws IllegalArgumentException (different shapes)
     * }</pre>
     *
     * @param <B> the element type of the other matrix
     * @param <R> the element type of the result matrix
     * @param <E> the type of exception that the zip function may throw
     * @param other the other matrix to zip with (must have the same dimensions, must not be {@code null})
     * @param zipFunction the function to apply to corresponding elements (must not be {@code null})
     * @param targetElementType the class of the result element type (must not be {@code null})
     * @return a new matrix with the results of the zip function
     * @throws IllegalArgumentException if {@code other}, {@code zipFunction}, or {@code targetElementType}
     *         is {@code null}, or if the matrices have different shapes
     * @throws E if the zip function throws an exception
     */
    public <B, R, E extends Exception> Matrix<R> zipWith(final Matrix<B> other, final Throwables.BiFunction<? super T, ? super B, R, E> zipFunction,
            final Class<R> targetElementType) throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgNotNull(targetElementType, "targetElementType");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final B[][] b = other.a;
        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.apply(a[i][j], b[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

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
     * result.rowCopy(0);   // returns [15, 18]
     * result.rowCopy(1);   // returns [21, 24]
     *
     * Matrix<Integer> mismatched = Matrix.of(new Integer[][] {{1, 2, 3}});
     * m1.zipWith(m2, mismatched, (a, b, c) -> a + b + c);   // throws IllegalArgumentException (different shapes)
     * }</pre>
     *
     * @param <B> the element type of the second matrix
     * @param <C> the element type of the third matrix
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix to zip with (must have the same dimensions, must not be {@code null})
     * @param third the third matrix to zip with (must have the same dimensions, must not be {@code null})
     * @param zipFunction the function to apply to corresponding elements (must not be {@code null})
     * @return a new matrix with the results of the zip function
     * @throws IllegalArgumentException if any of {@code other}, {@code third}, or {@code zipFunction} is
     *         {@code null}, or if any of the matrices have different shapes
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
     * Matrix<String> result = m1.zipWith(m2, m3, (i, s, d) -> i + s + String.format("%.1f", d), String.class);
     * result.get(0, 0);   // returns "1a0.1"
     * result.get(1, 1);   // returns "4d0.4"
     *
     * Matrix<Double> mismatched = Matrix.of(new Double[][] {{0.1, 0.2, 0.3}});
     * m1.zipWith(m2, mismatched, (i, s, d) -> i + s + d, String.class);   // throws IllegalArgumentException (different shapes)
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
     * @throws IllegalArgumentException if any of {@code other}, {@code third}, {@code zipFunction}, or
     *         {@code targetElementType} is {@code null}, or if any of the matrices have different shapes
     * @throws E if the zip function throws an exception
     */
    public <B, C, R, E extends Exception> Matrix<R> zipWith(final Matrix<B> other, final Matrix<C> third,
            final Throwables.TriFunction<? super T, ? super B, ? super C, R, E> zipFunction, final Class<R> targetElementType)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(third, "third");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgNotNull(targetElementType, "targetElementType");
        N.checkArgument(Matrices.isSameShape(this, other, third), "Cannot zip matrices with different shapes: all matrices must be {}x{}", rowCount,
                columnCount);

        final B[][] b = other.a;
        final C[][] c = third.a;
        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.apply(a[i][j], b[i][j], c[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return Matrix.of(result);
    }

    /**
     * Returns a stream of elements on the main diagonal (upper-left to lower-right).
     * The matrix must be square (same number of rows and columns).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> matrix = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.mainDiagonalStream().toArray();   // returns [1, 5, 9]
     *
     * Matrix<Integer> empty = Matrix.empty();
     * empty.mainDiagonalStream().count();      // returns 0 (empty stream)
     *
     * Matrix<Integer> nonSquare = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.mainDiagonalStream();          // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a {@link Stream} of diagonal elements from top-left to bottom-right, or an empty stream if the matrix is empty
     * @throws IllegalStateException if the matrix is not square
     */
    @Override
    public Stream<T> mainDiagonalStream() {
        checkIsSquare();

        if (isEmpty()) {
            return Stream.empty();
        }

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
     * matrix.antiDiagonalStream().toArray();   // returns [3, 5, 7]
     *
     * Matrix<Integer> empty = Matrix.empty();
     * empty.antiDiagonalStream().count();      // returns 0 (empty stream)
     *
     * Matrix<Integer> nonSquare = Matrix.of(new Integer[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.antiDiagonalStream();          // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a {@link Stream} of anti-diagonal elements from top-right to bottom-left, or an empty stream if the matrix is empty
     * @throws IllegalStateException if the matrix is not square
     */
    @Override
    public Stream<T> antiDiagonalStream() {
        checkIsSquare();

        if (isEmpty()) {
            return Stream.empty();
        }

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
     * matrix.horizontalStream().toArray();   // returns [1, 2, 3, 4]
     *
     * Matrix<Integer> empty = Matrix.empty();
     * empty.horizontalStream().count();      // returns 0 (empty stream)
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
     * matrix.horizontalStream(1).toArray();   // returns [4, 5, 6]
     * matrix.horizontalStream(0).toArray();   // returns [1, 2, 3]
     * matrix.horizontalStream(5);             // throws IndexOutOfBoundsException
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
     * matrix.horizontalStream(1, 3).toArray();   // returns [3, 4, 5, 6]
     * matrix.horizontalStream(0, 2).toArray();   // returns [1, 2, 3, 4]
     * matrix.horizontalStream(0, 5);             // throws IndexOutOfBoundsException
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
     * matrix.verticalStream().toArray();   // returns [1, 3, 2, 4]
     *
     * Matrix<Integer> empty = Matrix.empty();
     * empty.verticalStream().count();      // returns 0 (empty stream)
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
     * matrix.verticalStream(1).toArray();   // returns [2, 5, 8]
     * matrix.verticalStream(0).toArray();   // returns [1, 4, 7]
     * matrix.verticalStream(5);             // throws IndexOutOfBoundsException
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
     * matrix.verticalStream(1, 3).toArray();   // returns [2, 5, 3, 6]
     * matrix.verticalStream(0, 2).toArray();   // returns [1, 4, 2, 5]
     * matrix.verticalStream(0, 5);             // throws IndexOutOfBoundsException
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
     * matrix.rowStreams().count();                              // returns 3 (one inner stream per row)
     * matrix.rowStreams().map(Stream::toList).toList().get(0);  // returns [1, 2]
     *
     * Matrix<Integer> empty = Matrix.empty();
     * empty.rowStreams().count();                              // returns 0
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
     * matrix.rowStreams(1, 3).count();                              // returns 2 (rows 1 and 2)
     * matrix.rowStreams(1, 3).map(Stream::toList).toList().get(0);  // returns [3, 4]
     * matrix.rowStreams(0, 5);                                      // throws IndexOutOfBoundsException
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
     * matrix.columnStreams().count();                              // returns 3 (one inner stream per column)
     * matrix.columnStreams().map(Stream::toList).toList().get(0);  // returns [1, 4]
     *
     * Matrix<Integer> empty = Matrix.empty();
     * empty.columnStreams().count();                              // returns 0 (empty stream)
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
     * matrix.columnStreams(1, 3).count();                              // returns 2 (columns 1 and 2)
     * matrix.columnStreams(1, 3).map(Stream::toList).toList().get(0);  // returns [2, 5]
     * matrix.columnStreams(0, 5);                                      // throws IndexOutOfBoundsException
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
     * // values is now [1, 2, 3, 4]
     *
     * // Sum all elements
     * int[] sum = {0};
     * matrix.forEach(v -> sum[0] += v);
     * // sum[0] is now 10
     *
     * matrix.forEach(null);   // throws IllegalArgumentException (null action)
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
     * List<Integer> center = new ArrayList<>();
     * matrix.forEach(1, 2, 1, 2, value -> center.add(value));
     * // center is now [5]
     *
     * // Process a 2x2 sub-matrix
     * List<Integer> subMatrix = new ArrayList<>();
     * matrix.forEach(0, 2, 1, 3, value -> subMatrix.add(value));
     * // subMatrix is now [2, 3, 5, 6]
     *
     * matrix.forEach(0, 5, 0, 2, value -> {});   // throws IndexOutOfBoundsException (toRowIndex out of range)
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
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, elementAction, true);
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
     * Dataset dataset = matrix.toRowDataset(N.asList("A", "B", "C"));
     * // Dataset with:
     * // A  B  C
     * // -------
     * // 1  2  3
     * // 4  5  6
     * dataset.getColumn("A");   // returns [1, 4]
     * dataset.size();           // returns 2 (one row per matrix row)
     *
     * matrix.toRowDataset(null);                  // throws IllegalArgumentException (null names)
     * matrix.toRowDataset(N.asList("A", "B"));    // throws IllegalArgumentException (size != columnCount)
     * }</pre>
     *
     * @param columnNames the names to assign to each column in the resulting Dataset; size must equal {@code columnCount}
     * @return a Dataset containing the matrix data with the specified column names
     *         (one row per matrix row)
     * @throws IllegalArgumentException if {@code columnNames} is {@code null}, if its size
     *         does not equal {@code columnCount}, or if this matrix has rows but no columns
     * @see Dataset
     * @see #toColumnDataset(Collection)
     */
    @Beta
    public Dataset toRowDataset(final Collection<String> columnNames) throws IllegalArgumentException {
        N.checkArgNotNull(columnNames, "columnNames");
        N.checkArgument(columnNames.size() == columnCount, "The size({}) of specified columnNames and column count({}) of this Matrix are not equal",
                columnNames.size(), columnCount);
        N.checkArgument(rowCount == 0 || columnCount > 0, "Cannot convert a matrix with rows but zero columns to a row dataset");

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
     * Dataset dataset = matrix.toColumnDataset(N.asList("Row1", "Row2"));
     * // Dataset with:
     * // Row1  Row2
     * // ----------
     * // 1     4
     * // 2     5
     * // 3     6
     * dataset.getColumn("Row1");   // returns [1, 2, 3]
     * dataset.getColumn("Row2");   // returns [4, 5, 6]
     *
     * matrix.toColumnDataset(null);                // throws IllegalArgumentException (null names)
     * matrix.toColumnDataset(N.asList("Row1"));    // throws IllegalArgumentException (size != rowCount)
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
        N.checkArgument(columnNames.size() == rowCount, "The size({}) of specified columnNames and row count({}) of this Matrix are not equal",
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
     * matrix.println();   // prints "[1, 2, 3]" then "[4, 5, 6]"; returns the printed string
     *
     * Matrix.empty().println();   // returns "[]" (empty matrix)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Integer> m1 = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * Matrix<Integer> m2 = Matrix.of(new Integer[][] {{1, 2}, {3, 4}});
     * m1.hashCode() == m2.hashCode();                         // returns true  (equal matrices, same hash)
     * m1.hashCode();                                          // returns 32833
     *
     * Matrix<Integer> m3 = Matrix.of(new Integer[][] {{1, 2}, {3, 5}});
     * m1.hashCode() == m3.hashCode();                         // returns false (different content, different hash)
     *
     * Matrix.empty().hashCode();                              // returns 1     (empty matrix)
     * Matrix.of(new String[][] {{null}}).hashCode();          // returns 62    (null element contributes 0 per element)
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
     * m1.equals(m2);                                          // returns true
     * m1.equals(Matrix.of(new Integer[][] {{1, 2}, {3, 5}})); // returns false (different content)
     * m1.equals(null);                                        // returns false
     * m1.equals("not a matrix");                              // returns false (not a Matrix)
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
     * matrix.toString();                                  // returns "[[1, 2], [3, 4]]"
     *
     * Matrix.of(new String[][] {{"a", null}}).toString(); // returns "[[a, null]]"
     * Matrix.empty().toString();                          // returns "[]"
     * }</pre>
     *
     * @return a string representation of this matrix
     */
    @Override
    public String toString() {
        return N.deepToString(a);
    }
}
