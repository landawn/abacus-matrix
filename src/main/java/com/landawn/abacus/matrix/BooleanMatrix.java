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
 * a validated backing array. Constructors and {@link #of(boolean[]...)} generally wrap the supplied storage
 * directly, while factories, conversions, and mapping operations allocate new arrays.</p>
 *
 * <p>Cells introduced by growth or reshaping default to {@code false} unless an overload accepts an
 * explicit fill value. Optional return values use {@link OptionalBoolean}.</p>
 *
 * <p>This is the {@code boolean} sibling of {@link ByteMatrix}, {@link IntMatrix}, {@link LongMatrix},
 * and the other primitive-element matrix classes in this package.</p>
 *
 * @see IntMatrix
 * @see LongMatrix
 * @see DoubleMatrix
 * @see FloatMatrix
 * @see ShortMatrix
 * @see ByteMatrix
 * @see CharMatrix
 * @see Matrix
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
     * matrix.rowCount();               // returns 2
     * matrix.columnCount();            // returns 3
     * data[0][0] = false;              // also mutates the matrix (no defensive copy)
     * matrix.get(0, 0);                // returns false
     *
     * new BooleanMatrix(null).rowCount();                         // returns 0 (empty 0x0 matrix)
     * new BooleanMatrix(new boolean[0][0]).columnCount();         // returns 0
     * new BooleanMatrix(new boolean[][] {{true}, {true, false}}); // throws IllegalArgumentException (jagged rows)
     * }</pre>
     *
     * @param a the two-dimensional boolean array to wrap, or {@code null} for an empty matrix
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
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
     * matrix.rowCount();          // returns 0
     * matrix.columnCount();       // returns 0
     * matrix.isEmpty();           // returns true
     * matrix.countTrue();         // returns 0
     *
     * BooleanMatrix.empty() == BooleanMatrix.empty();   // returns true (shared singleton)
     * matrix.get(0, 0);                                 // throws ArrayIndexOutOfBoundsException (no elements)
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
     * matrix.get(0, 1);                               // returns false
     * matrix.get(1, 0);                               // returns false
     * matrix.rowCount();                              // returns 2
     *
     * BooleanMatrix.of((boolean[][]) null).isEmpty();            // returns true (null -> empty singleton)
     * BooleanMatrix.of().isEmpty();                              // returns true (no rows -> empty singleton)
     * BooleanMatrix.of(new boolean[][] {{true}, {true, false}}); // throws IllegalArgumentException (jagged rows)
     * }</pre>
     *
     * @param a the two-dimensional boolean array to wrap; may be {@code null} or empty, in which case the empty matrix singleton is returned
     * @return a new {@code BooleanMatrix} backed by {@code a}, or the empty {@code BooleanMatrix} if {@code a} is {@code null} or empty
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
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
     * matrix.rowCount();          // returns 1
     * matrix.columnCount();       // returns 5 (each cell a random boolean)
     *
     * BooleanMatrix.random(0).columnCount();   // returns 0 (empty row)
     * BooleanMatrix.random(0).rowCount();      // returns 1
     * BooleanMatrix.random(-1);                // throws IllegalArgumentException (negative length)
     * }</pre>
     *
     * @param length the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code BooleanMatrix} of dimensions {@code 1 × length} filled with random values
     * @throws IllegalArgumentException if {@code length} is negative
     * @see #random(int, int)
     */
    public static BooleanMatrix random(final int length) {
        N.checkArgument(length >= 0, MSG_NEGATIVE_DIMENSION, "length", length);

        return random(1, length);
    }

    /**
     * Creates a new matrix of the specified dimensions filled with pseudo-randomly generated boolean values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.random(2, 3);
     * matrix.rowCount();          // returns 2
     * matrix.columnCount();       // returns 3 (each cell a random boolean)
     *
     * BooleanMatrix.random(0, 0).isEmpty();        // returns true
     * BooleanMatrix.random(3, 0).columnCount();    // returns 0
     * BooleanMatrix.random(-1, 3);                 // throws IllegalArgumentException (negative rowCount)
     * BooleanMatrix.random(2, -1);                 // throws IllegalArgumentException (negative columnCount)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @return a new {@code BooleanMatrix} of dimensions {@code rowCount × columnCount} filled with random values
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if {@code rowCount} is {@code 0} while {@code columnCount} is positive (an unrepresentable shape)
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
     * matrix.get(0, 0);                          // returns true
     * matrix.countTrue();                        // returns 6 (all 2x3 cells are true)
     *
     * BooleanMatrix.repeat(2, 2, false).anyTrue();   // returns false (all cells false)
     * BooleanMatrix.repeat(0, 0, true).isEmpty();    // returns true
     * BooleanMatrix.repeat(-1, 3, true);             // throws IllegalArgumentException (negative rowCount)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix; must be {@code >= 0}
     * @param columnCount the number of columns in the new matrix; must be {@code >= 0}
     * @param element the boolean value to fill the matrix with
     * @return a new {@code BooleanMatrix} of dimensions {@code rowCount × columnCount} with every element set to {@code element}
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if {@code rowCount} is {@code 0} while {@code columnCount} is positive (an unrepresentable shape)
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
     * // Resulting 3x3 matrix:
     * //   {true,  false, false},
     * //   {false, false, false},
     * //   {false, false, true}
     * matrix.get(0, 0);          // returns true
     * matrix.get(1, 1);          // returns false
     * matrix.get(2, 2);          // returns true
     * matrix.get(0, 2);          // returns false (off-diagonal)
     *
     * BooleanMatrix.mainDiagonal((boolean[]) null);            // throws IllegalArgumentException (null array)
     * BooleanMatrix.mainDiagonal(new boolean[0]).isEmpty();     // returns true
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements; must not be {@code null}, but may be empty,
     *        in which case an empty matrix is returned
     * @return a square matrix with the specified main diagonal ({@code n × n} where {@code n}
     *         is the diagonal length), or an empty matrix if {@code mainDiagonal} is empty
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null}
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
     * // Resulting 3x3 matrix:
     * //   {false, false, true},
     * //   {false, false, false},
     * //   {true,  false, false}
     * matrix.get(0, 2);          // returns true
     * matrix.get(1, 1);          // returns false
     * matrix.get(2, 0);          // returns true
     * matrix.get(0, 0);          // returns false (off-anti-diagonal)
     *
     * BooleanMatrix.antiDiagonal((boolean[]) null);            // throws IllegalArgumentException (null array)
     * BooleanMatrix.antiDiagonal(new boolean[0]).isEmpty();     // returns true
     * }</pre>
     *
     * @param antiDiagonal the array of anti-diagonal elements; must not be {@code null}, but may be empty,
     *        in which case an empty matrix is returned
     * @return a square matrix with the specified anti-diagonal ({@code n × n} where {@code n}
     *         is the diagonal length), or an empty matrix if {@code antiDiagonal} is empty
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null}
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
     * // Resulting 3x3 matrix (main diagonal takes precedence at center):
     * //   {true,  false, true},
     * //   {false, true,  false},
     * //   {true,  false, true}
     * matrix.get(0, 0);          // returns true  (main diagonal)
     * matrix.get(0, 2);          // returns true  (anti-diagonal)
     * matrix.get(1, 1);          // returns true  (main wins over anti-diagonal's false at center)
     *
     * BooleanMatrix.diagonals(null, null);                                        // throws IllegalArgumentException (both null)
     * BooleanMatrix.diagonals(new boolean[] {true}, new boolean[] {true, false}); // throws IllegalArgumentException (length mismatch)
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements; may be {@code null} if {@code antiDiagonal} is non-{@code null};
     *        may be empty
     * @param antiDiagonal the array of anti-diagonal elements; may be {@code null} if {@code mainDiagonal} is non-{@code null};
     *        may be empty
     * @return a square matrix with the specified diagonals, or an empty matrix when both arrays are empty (at least one being a non-{@code null} zero-length array)
     * @throws IllegalArgumentException if both {@code mainDiagonal} and {@code antiDiagonal} are {@code null}, or if both arrays are non-empty and have different lengths
     */
    public static BooleanMatrix diagonals(final boolean[] mainDiagonal, final boolean[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(mainDiagonal != null || antiDiagonal != null, "Both 'mainDiagonal' and 'antiDiagonal' can't be null");

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
     * primitive.get(0, 0);          // returns true
     * primitive.get(1, 0);          // returns false (null -> false)
     * primitive.get(1, 1);          // returns true
     *
     * BooleanMatrix.unbox(Matrix.of(new Boolean[0][0])).isEmpty();   // returns true
     * BooleanMatrix.unbox((Matrix<Boolean>) null);                   // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the boxed {@code Matrix<Boolean>} to convert; must not be {@code null}
     * @return a new {@code BooleanMatrix} with primitive boolean values
     * @throws IllegalArgumentException if {@code x} is {@code null}
     * @see #boxed()
     */
    public static BooleanMatrix unbox(final Matrix<Boolean> x) {
        N.checkArgNotNull(x, "x");

        return BooleanMatrix.of(Array.unbox(x.a));
    }

    /**
     * Returns the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.get(0, 0);          // returns true
     * matrix.get(0, 1);          // returns false
     *
     * matrix.get(2, 0);          // throws ArrayIndexOutOfBoundsException (row out of range)
     * matrix.get(0, 5);          // throws ArrayIndexOutOfBoundsException (column out of range)
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
     * matrix.get(Point.of(0, 0));          // returns true
     * matrix.get(Point.of(0, 1));          // returns false
     *
     * matrix.get(Point.of(5, 0));          // throws ArrayIndexOutOfBoundsException (out of range)
     * matrix.get((Point) null);            // throws IllegalArgumentException (null point)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
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
     * matrix.set(0, 1, true);
     * matrix.get(0, 1);          // returns true (was false)
     * matrix.set(1, 1, false);
     * matrix.get(1, 1);          // returns false (was true)
     *
     * matrix.set(2, 0, true);    // throws ArrayIndexOutOfBoundsException (row out of range)
     * matrix.set(0, 5, true);    // throws ArrayIndexOutOfBoundsException (column out of range)
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
     * matrix.set(Point.of(1, 0), true);
     * matrix.get(1, 0);          // returns true (was false)
     * matrix.set(Point.of(0, 0), false);
     * matrix.get(0, 0);          // returns false (was true)
     *
     * matrix.set(Point.of(5, 0), true);   // throws ArrayIndexOutOfBoundsException (out of range)
     * matrix.set((Point) null, true);     // throws IllegalArgumentException (null point)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
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
     * Returns the element directly above the specified position, or an empty {@link OptionalBoolean}
     * if the position is on the top edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalBoolean} is returned for the top
     * row instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.valueAbove(1, 0).get();       // returns true  (element at (0, 0))
     * matrix.valueAbove(1, 1).get();       // returns false (element at (0, 1))
     * matrix.valueAbove(0, 0).isEmpty();   // returns true  (no row above the top row)
     *
     * matrix.valueAbove(5, 0);             // throws IndexOutOfBoundsException (row out of range)
     * matrix.valueAbove(1, 9);             // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalBoolean} containing the element at position {@code (rowIndex - 1, columnIndex)},
     *         or empty if {@code rowIndex == 0}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalBoolean valueAbove(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? OptionalBoolean.empty() : OptionalBoolean.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, or an empty {@link OptionalBoolean}
     * if the position is on the bottom edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalBoolean} is returned for the
     * bottom row instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.valueBelow(0, 0).get();       // returns false (element at (1, 0))
     * matrix.valueBelow(0, 1).get();       // returns true  (element at (1, 1))
     * matrix.valueBelow(1, 0).isEmpty();   // returns true  (no row below the bottom row)
     *
     * matrix.valueBelow(5, 0);             // throws IndexOutOfBoundsException (row out of range)
     * matrix.valueBelow(0, 9);             // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalBoolean} containing the element at position {@code (rowIndex + 1, columnIndex)},
     *         or empty if {@code rowIndex == rowCount - 1}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalBoolean valueBelow(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? OptionalBoolean.empty() : OptionalBoolean.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, or an empty
     * {@link OptionalBoolean} if the position is on the leftmost edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalBoolean} is returned for the
     * leftmost column instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.valueLeft(0, 1).get();        // returns true  (element at (0, 0))
     * matrix.valueLeft(1, 1).get();        // returns false (element at (1, 0))
     * matrix.valueLeft(0, 0).isEmpty();    // returns true  (no column to the left of column 0)
     *
     * matrix.valueLeft(5, 1);              // throws IndexOutOfBoundsException (row out of range)
     * matrix.valueLeft(0, 9);              // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalBoolean} containing the element at position {@code (rowIndex, columnIndex - 1)},
     *         or empty if {@code columnIndex == 0}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalBoolean valueLeft(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? OptionalBoolean.empty() : OptionalBoolean.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, or an empty
     * {@link OptionalBoolean} if the position is on the rightmost edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalBoolean} is returned for the
     * rightmost column instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.valueRight(0, 0).get();       // returns false (element at (0, 1))
     * matrix.valueRight(1, 0).get();       // returns true  (element at (1, 1))
     * matrix.valueRight(0, 1).isEmpty();   // returns true  (no column to the right of the last column)
     *
     * matrix.valueRight(5, 0);             // throws IndexOutOfBoundsException (row out of range)
     * matrix.valueRight(0, 9);             // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalBoolean} containing the element at position {@code (rowIndex, columnIndex + 1)},
     *         or empty if {@code columnIndex == columnCount - 1}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalBoolean valueRight(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? OptionalBoolean.empty() : OptionalBoolean.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as a live reference to the underlying {@code boolean[]} storage.
     *
     * <p><b>Note:</b> This method returns a reference to the internal array, not a copy.
     * Modifications to the returned array will affect the matrix. If you need an independent
     * copy, use {@link #rowCopy(int)} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {false, true, false}});
     * boolean[] firstRow = matrix.rowView(0);   // returns [true, false, false] (live reference)
     * firstRow[0] = false;                      // mutates the matrix in place
     * matrix.get(0, 0);                         // returns false
     *
     * matrix.rowView(-1);   // throws IndexOutOfBoundsException (negative index)
     * matrix.rowView(2);    // throws IndexOutOfBoundsException (index >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return the specified row array (direct reference to internal storage)
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowCopy(int)
     */
    @Override
    public boolean[] rowView(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return a[rowIndex];
    }

    /**
     * Returns a defensive copy of the specified row as a new {@code boolean[]}.
     * Changes to the returned array do not affect this matrix and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {false, true, false}});
     * boolean[] firstRow = matrix.rowCopy(0);   // returns [true, false, false] (independent copy)
     * firstRow[0] = false;                      // does NOT affect the matrix
     * matrix.get(0, 0);                         // returns true (unchanged)
     *
     * matrix.rowCopy(-1);   // throws IndexOutOfBoundsException (negative index)
     * matrix.rowCopy(2);    // throws IndexOutOfBoundsException (index >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new boolean array containing the values from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowView(int)
     * @see #columnCopy(int)
     */
    @Override
    public boolean[] rowCopy(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return N.copyOf(a[rowIndex], columnCount);
    }

    /**
     * Returns a defensive copy of the specified column as a new {@code boolean[]}.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously in memory. Modifications to the returned array
     * will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {false, true, false}});
     * boolean[] firstColumn = matrix.columnCopy(0);   // returns [true, false] (independent copy)
     * boolean[] secondColumn = matrix.columnCopy(1);  // returns [false, true]
     * firstColumn[0] = false;                         // does NOT affect the matrix
     * matrix.get(0, 0);                               // returns true (unchanged)
     *
     * matrix.columnCopy(-1);   // throws IndexOutOfBoundsException (negative index)
     * matrix.columnCopy(3);    // throws IndexOutOfBoundsException (index >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new boolean array containing the values from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     * @see #rowCopy(int)
     * @see #rowView(int)
     */
    @Override
    public boolean[] columnCopy(final int columnIndex) throws IndexOutOfBoundsException {
        checkColumnIndex(columnIndex);

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
     * matrix.setRow(0, new boolean[] {false, true, true});
     * matrix.get(0, 0);          // returns false
     * matrix.get(0, 1);          // returns true
     *
     * matrix.setRow(0, new boolean[] {true, false});      // throws IllegalArgumentException (length != columnCount)
     * matrix.setRow(5, new boolean[] {true, true, true}); // throws IndexOutOfBoundsException (row index out of bounds)
     * matrix.setRow(0, (boolean[]) null);                 // throws IllegalArgumentException (null row)
     * }</pre>
     *
     * @param rowIndex the index of the row to set (0-based)
     * @param row the array of values to copy into the row; must have length equal to the number of columns
     * @throws IndexOutOfBoundsException if {@code rowIndex} is out of bounds
     * @throws IllegalArgumentException if {@code row} is {@code null} or if {@code row.length} does not match the column count
     */
    public void setRow(final int rowIndex, final boolean[] row) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {false, true, false}});
     * matrix.setColumn(0, new boolean[] {false, true});
     * matrix.get(0, 0);          // returns false
     * matrix.get(1, 0);          // returns true
     *
     * matrix.setColumn(0, new boolean[] {true});        // throws IllegalArgumentException (length != rowCount)
     * matrix.setColumn(5, new boolean[] {true, true});  // throws IndexOutOfBoundsException (column index out of bounds)
     * matrix.setColumn(0, (boolean[]) null);            // throws IllegalArgumentException (null column)
     * }</pre>
     *
     * @param columnIndex the index of the column to set (0-based)
     * @param column the array of values to copy into the column; must have length equal to the number of rows
     * @throws IndexOutOfBoundsException if {@code columnIndex} is out of bounds
     * @throws IllegalArgumentException if {@code column} is {@code null} or if {@code column.length} does not match the row count
     */
    public void setColumn(final int columnIndex, final boolean[] column) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     * from left to right (column {@code 0} to column {@code columnCount - 1}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, true}, {false, true, false}});
     * matrix.updateRow(0, val -> !val);   // inverts all values in row 0
     * matrix.get(0, 0);                   // returns false (was true)
     * matrix.get(0, 1);                   // returns true  (was false)
     *
     * matrix.updateRow(1, val -> true);   // sets every cell in row 1 to true
     * matrix.get(1, 0);                   // returns true
     *
     * matrix.updateRow(5, val -> val);    // throws IndexOutOfBoundsException (row out of bounds)
     * matrix.updateRow(0, null);          // throws IllegalArgumentException (null operator)
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
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.BooleanUnaryOperator<E> operator)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        N.checkArgNotNull(operator, "operator");

        checkRowIndex(rowIndex);

        for (int i = 0; i < columnCount; i++) {
            a[rowIndex][i] = operator.applyAsBoolean(a[rowIndex][i]);
        }
    }

    /**
     * Updates all elements in a column in-place by applying the specified operator to each element.
     * This modifies the matrix directly.
     *
     * <p>The operator is applied to each element in the specified column sequentially
     * from top to bottom (row {@code 0} to row {@code rowCount - 1}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, true}, {false, true, false}});
     * matrix.updateColumn(1, val -> !val);   // inverts column 1 ([false, true] -> [true, false])
     * matrix.get(0, 1);                      // returns true  (was false)
     * matrix.get(1, 1);                      // returns false (was true)
     *
     * matrix.updateColumn(0, val -> false);  // sets every cell in column 0 to false
     * matrix.get(0, 0);                      // returns false
     *
     * matrix.updateColumn(5, val -> val);    // throws IndexOutOfBoundsException (column out of bounds)
     * matrix.updateColumn(0, null);          // throws IllegalArgumentException (null operator)
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
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.BooleanUnaryOperator<E> operator)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        N.checkArgNotNull(operator, "operator");

        checkColumnIndex(columnIndex);

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
     *     {true,  false, false},
     *     {false, true,  false},
     *     {false, false, false}
     * });
     * boolean[] diagonal = matrix.getMainDiagonal();   // returns [true, true, false]
     * diagonal[0] = false;                             // copy is independent; matrix unchanged
     * matrix.get(0, 0);                                // returns true
     *
     * BooleanMatrix wide = BooleanMatrix.of(new boolean[][] {{true, false, true}});
     * wide.getMainDiagonal();   // throws IllegalStateException (not square: 1x3)
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
     *     {true,  false, false},
     *     {false, true,  false},
     *     {false, false, true}
     * });
     * matrix.setMainDiagonal(new boolean[] {false, true, false});
     * matrix.get(0, 0);          // returns false
     * matrix.get(1, 1);          // returns true
     * matrix.get(2, 2);          // returns false
     *
     * matrix.setMainDiagonal(new boolean[] {true, true});   // throws IllegalArgumentException (length != rowCount)
     * BooleanMatrix wide = BooleanMatrix.of(new boolean[][] {{true, false, true}});
     * wide.setMainDiagonal(new boolean[] {true});           // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must have length equal to {@code rowCount}
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null} or its array length does not equal {@code rowCount}
     */
    @Override
    public void setMainDiagonal(final boolean[] mainDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgNotNull(mainDiagonal, "mainDiagonal");
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
     *     {true,  false, false},
     *     {false, true,  false},
     *     {false, false, false}
     * });
     * matrix.updateMainDiagonal(val -> !val);   // invert diagonal [true, true, false] -> [false, false, true]
     * matrix.get(0, 0);                         // returns false
     * matrix.get(2, 2);                         // returns true
     *
     * matrix.updateMainDiagonal((Throwables.BooleanUnaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null operator)
     * BooleanMatrix wide = BooleanMatrix.of(new boolean[][] {{true, false, true}});
     * wide.updateMainDiagonal(val -> !val);     // throws IllegalStateException (not square)
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
     *     {true,  false, true},
     *     {false, true,  false},
     *     {false, false, false}
     * });
     * boolean[] antiDiag = matrix.getAntiDiagonal();   // returns [true, true, false] (a[0][2], a[1][1], a[2][0])
     * antiDiag[0] = false;                             // copy is independent; matrix unchanged
     * matrix.get(0, 2);                                // returns true
     *
     * BooleanMatrix wide = BooleanMatrix.of(new boolean[][] {{true, false, true}});
     * wide.getAntiDiagonal();   // throws IllegalStateException (not square: 1x3)
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
     *     {true,  false, false},
     *     {false, true,  false},
     *     {false, false, true}
     * });
     * matrix.setAntiDiagonal(new boolean[] {true, false, true});
     * matrix.get(0, 2);          // returns true
     * matrix.get(1, 1);          // returns false
     * matrix.get(2, 0);          // returns true
     *
     * matrix.setAntiDiagonal(new boolean[] {true, true});   // throws IllegalArgumentException (length != rowCount)
     * BooleanMatrix wide = BooleanMatrix.of(new boolean[][] {{true, false, true}});
     * wide.setAntiDiagonal(new boolean[] {true});           // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must have length equal to {@code rowCount}
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null} or its array length does not equal {@code rowCount}
     */
    @Override
    public void setAntiDiagonal(final boolean[] antiDiagonal) throws IllegalStateException, IllegalArgumentException {
        checkIsSquare();
        N.checkArgNotNull(antiDiagonal, "antiDiagonal");
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
     *     {true,  false, true},
     *     {false, true,  false},
     *     {true,  false, false}
     * });
     * matrix.updateAntiDiagonal(val -> !val);   // invert anti-diagonal [true, true, true] -> [false, false, false]
     * matrix.get(0, 2);                         // returns false
     * matrix.get(2, 0);                         // returns false
     *
     * matrix.updateAntiDiagonal((Throwables.BooleanUnaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null operator)
     * BooleanMatrix wide = BooleanMatrix.of(new boolean[][] {{true, false, true}});
     * wide.updateAntiDiagonal(val -> !val);     // throws IllegalStateException (not square)
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
     * matrix.updateAll(val -> !val);   // inverts every element
     * matrix.get(0, 0);                // returns false (was true)
     * matrix.get(0, 1);                // returns true  (was false)
     *
     * matrix.updateAll(val -> true);   // sets every element to true
     * matrix.countTrue();              // returns 4
     *
     * matrix.updateAll((Throwables.BooleanUnaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null operator)
     * BooleanMatrix.empty().updateAll(val -> !val);                               // no-op on empty matrix
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

        if (Matrices.isParallelizable(this)) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = operator.applyAsBoolean(a[i][j]);
            Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
        } else {
            for (int i = 0; i < rowCount; i++) {
                final boolean[] row = a[i];

                for (int j = 0; j < columnCount; j++) {
                    row[j] = operator.applyAsBoolean(row[j]);
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[3][3]);
     * matrix.updateAll((i, j) -> i == j);   // sets main diagonal to true, others to false
     * matrix.get(0, 0);                     // returns true
     * matrix.get(0, 1);                     // returns false
     * matrix.countTrue();                   // returns 3
     *
     * matrix.updateAll((i, j) -> (i + j) % 2 == 0);   // checkerboard pattern
     * matrix.get(0, 0);                               // returns true
     * matrix.get(0, 1);                               // returns false
     *
     * matrix.updateAll((Throwables.IntBiFunction<Boolean, RuntimeException>) null); // throws IllegalArgumentException (null mapper)
     * }</pre>
     *
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the function that receives row index and column index (0-based) and returns
     *             the new value for that position; the returned {@code Boolean} is unboxed, so it
     *             must not be {@code null}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws NullPointerException if {@code mapper} returns {@code null} for any position
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
     * matrix.replaceIf(val -> val == false, true);   // replace every false with true
     * matrix.countTrue();                            // returns 4 (all cells now true)
     *
     * BooleanMatrix matrix2 = BooleanMatrix.of(new boolean[][] {{true, false}, {true, false}});
     * matrix2.replaceIf(val -> val, false);         // replace every true with false
     * matrix2.anyTrue();                            // returns false
     *
     * matrix.replaceIf((Throwables.BooleanPredicate<RuntimeException>) null, true); // throws IllegalArgumentException (null predicate)
     * BooleanMatrix.empty().replaceIf(val -> true, false);                          // no-op on empty matrix
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[3][3]);   // all false
     * matrix.replaceIf((i, j) -> i == j, true);                     // set main diagonal to true
     * matrix.get(1, 1);                                             // returns true
     * matrix.get(0, 1);                                             // returns false
     *
     * matrix.replaceIf((i, j) -> i < j, true);    // also set upper triangle to true
     * matrix.get(0, 2);                           // returns true
     * matrix.get(2, 0);                           // returns false (lower triangle untouched)
     *
     * matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, true); // throws IllegalArgumentException (null predicate)
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
     * Creates a new {@code BooleanMatrix} by applying a transformation function to each element.
     * The original matrix is not modified; a new matrix with transformed values is returned.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
     * This is the immutable counterpart to {@link #updateAll(Throwables.BooleanUnaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * BooleanMatrix inverted = matrix.map(val -> !val);   // new matrix with inverted values
     * inverted.get(0, 0);                                 // returns false
     * matrix.get(0, 0);                                   // returns true (original unchanged)
     *
     * BooleanMatrix allFalse = matrix.map(val -> false);
     * allFalse.anyTrue();        // returns false
     *
     * matrix.map((Throwables.BooleanUnaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null mapper)
     * BooleanMatrix.empty().map(val -> !val).isEmpty();                     // returns true
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
     * Creates a new {@code Matrix} by applying a function that converts boolean values to objects of type {@code R}.
     * This operation may be executed in parallel for better performance on large matrices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     *
     * Matrix<String> stringMatrix = matrix.mapToObj(val -> val ? "YES" : "NO", String.class);
     * stringMatrix.get(0, 0);    // returns "YES"
     * stringMatrix.get(0, 1);    // returns "NO"
     *
     * Matrix<Integer> intMatrix = matrix.mapToObj(val -> val ? 1 : 0, Integer.class);
     * intMatrix.get(1, 1);       // returns 1
     * intMatrix.get(1, 0);       // returns 0
     *
     * matrix.mapToObj(val -> val ? "Y" : "N", (Class<String>) null);      // throws IllegalArgumentException (null type)
     * BooleanMatrix.empty().mapToObj(val -> "x", String.class).isEmpty(); // returns true
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
        N.checkArgNotNull(targetElementType, "targetElementType");
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
     * matrix.fill(true);         // sets every element to true
     * matrix.countTrue();        // returns 4
     * matrix.allTrue();          // returns true
     *
     * matrix.fill(false);        // sets every element to false
     * matrix.anyTrue();          // returns false
     *
     * BooleanMatrix.empty().fill(true); // no-op on empty matrix (no exception)
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
     * Fills this matrix with values from another two-dimensional array, starting at position {@code (0, 0)}.
     * Equivalent to {@code fill(0, 0, source)}.
     * The source array can be smaller than this matrix; only the overlapping region is copied.
     * If the source array is larger, only the portion that fits is copied. {@code null} rows in
     * {@code source} are skipped (the corresponding row of this matrix is left unchanged).
     * The matrix is modified in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[3][3]);   // 3x3 of false
     * matrix.fill(new boolean[][] {{true, true}, {true, true}});    // overwrite top-left 2x2
     * matrix.get(0, 0);                                             // returns true
     * matrix.get(1, 1);                                             // returns true
     * matrix.get(2, 2);                                             // returns false (outside the copied region)
     *
     * matrix.fill((boolean[][]) null);   // throws IllegalArgumentException (null source)
     * matrix.fill(new boolean[0][0]);    // no-op (empty source copies nothing)
     * }</pre>
     *
     * @param source the two-dimensional boolean array to copy values from; must not be {@code null}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @see #fill(int, int, boolean[][])
     */
    public void fill(final boolean[][] source) {
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[4][4]);        // 4x4 of false
     * matrix.fill(1, 1, new boolean[][] {{true, true}, {true, true}});   // 2x2 block at (1,1)
     * matrix.get(1, 1);                                                  // returns true
     * matrix.get(2, 2);                                                  // returns true
     * matrix.get(0, 0);                                                  // returns false (outside the copied region)
     *
     * matrix.fill(1, 2, new boolean[][] {{true, true, true}});   // only columns 2-3 fit; the third value is clipped
     * matrix.fill(-1, 0, new boolean[][] {{true}});              // throws IndexOutOfBoundsException (negative index)
     * matrix.fill(0, 0, (boolean[][]) null);                     // throws IllegalArgumentException (null source)
     * }</pre>
     *
     * @param destRowIndex the target row index in this matrix (0-based, must satisfy {@code 0 <= destRowIndex <= rowCount})
     * @param destColumnIndex the target column index in this matrix (0-based, must satisfy {@code 0 <= destColumnIndex <= columnCount})
     * @param source the source array to copy values from; must not be {@code null}
     * @throws IndexOutOfBoundsException if {@code destRowIndex < 0} or {@code destRowIndex > rowCount},
     *         or if {@code destColumnIndex < 0} or {@code destColumnIndex > columnCount}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final boolean[][] source) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     * BooleanMatrix original = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * BooleanMatrix copy = original.copy();
     * copy.equals(original);     // returns true (same content)
     * copy.set(0, 0, false);     // mutating the copy does NOT affect the original
     * original.get(0, 0);        // returns true  (unchanged)
     * copy.get(0, 0);            // returns false (modified copy)
     *
     * BooleanMatrix.empty().copy().isEmpty();   // returns true
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
     *     {true,  false},
     *     {false, true},
     *     {true,  true}
     * });
     * BooleanMatrix subset = matrix.copy(1, 3);   // rows 1 and 2
     * subset.rowCount();                          // returns 2
     * subset.get(0, 1);                           // returns true  (original row 1, column 1)
     * subset.get(1, 0);                           // returns true  (original row 2, column 0)
     *
     * matrix.copy(0, 0).rowCount();   // returns 0 (empty range)
     * matrix.copy(1, 5);              // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.copy(2, 1);              // throws IndexOutOfBoundsException (fromRowIndex > toRowIndex)
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
     *     {true,  false, true,  false},
     *     {false, true,  false, true},
     *     {true,  true,  false, false}
     * });
     * BooleanMatrix subMatrix = matrix.copy(0, 2, 1, 3);   // rows 0-1, columns 1-2
     * subMatrix.get(0, 0);                                 // returns false (original (0,1))
     * subMatrix.get(1, 1);                                 // returns false (original (1,2))
     *
     * BooleanMatrix col = matrix.copy(0, 3, 2, 3);   // all rows, column 2 only
     * col.columnCount();                             // returns 1
     * col.get(0, 0);                                 // returns true
     *
     * matrix.copy(0, 2, 1, 9);   // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.copy(0, 2, 3, 1);   // throws IndexOutOfBoundsException (fromColumnIndex > toColumnIndex)
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
     * grown.get(0, 0);           // returns true  (preserved cell)
     * grown.get(3, 3);           // returns false (new cell)
     * truncated.rowCount();      // returns 2
     * truncated.get(1, 1);       // returns true
     *
     * // Mixed: grow rows, truncate columns
     * BooleanMatrix mixed = matrix.resize(4, 2);
     * mixed.rowCount();          // returns 4
     * mixed.columnCount();       // returns 2
     * mixed.get(3, 0);           // returns false (new row)
     *
     * matrix.resize(0, 0).isEmpty();   // returns true
     * matrix.resize(-1, 2);            // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new {@code BooleanMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
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
     * grown.get(0, 3);           // returns true  (new cell filled with defaultValue)
     * grown.get(3, 3);           // returns true  (new cell)
     * grown.get(0, 0);           // returns true  (preserved cell)
     *
     * // Truncate: defaultValue is ignored when shrinking
     * BooleanMatrix truncated = matrix.resize(2, 2, true);
     * truncated.rowCount();      // returns 2
     * truncated.get(0, 1);       // returns false (preserved, defaultValue not applied)
     *
     * matrix.resize(0, 0, true).isEmpty();   // returns true
     * matrix.resize(2, -1, true);            // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the value used to fill cells that are added when a dimension grows;
     *        ignored when a dimension shrinks
     * @return a new {@code BooleanMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
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
     * bordered.rowCount();       // returns 4
     * bordered.get(0, 0);        // returns false (padding cell)
     * bordered.get(1, 1);        // returns true  (original content)
     *
     * // Asymmetric: 2 columns on the left only, no row padding
     * BooleanMatrix shifted = matrix.extend(0, 0, 2, 0);
     * shifted.columnCount();     // returns 4
     * shifted.get(0, 0);         // returns false (padding)
     * shifted.get(0, 2);         // returns true  (original content)
     *
     * matrix.extend(0, 0, 0, 0).equals(matrix);   // returns true (no padding)
     * matrix.extend(-1, 0, 0, 0);                 // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @return a new {@code BooleanMatrix} with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any parameter is negative,
     *         if the resulting dimensions would overflow {@code Integer.MAX_VALUE},
     *         or if the resulting shape is not representable (zero rows with a non-zero column count)
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
     * bordered.get(0, 0);        // returns false (padding filled with defaultValue)
     * bordered.get(1, 1);        // returns true  (original content)
     *
     * // Asymmetric: 2 columns on the left only, no row padding, fill with true
     * BooleanMatrix shifted = matrix.extend(0, 0, 2, 0, true);
     * shifted.columnCount();     // returns 4
     * shifted.get(0, 0);         // returns true  (padding filled with true)
     * shifted.get(0, 2);         // returns true  (original content)
     *
     * matrix.extend(0, 0, 0, 0, true).equals(matrix);   // returns true (no padding)
     * matrix.extend(0, -1, 0, 0, true);                 // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of padding rows to add above the original matrix; must be {@code >= 0}
     * @param padBottom number of padding rows to add below the original matrix; must be {@code >= 0}
     * @param padLeft number of padding columns to add to the left of the original matrix; must be {@code >= 0}
     * @param padRight number of padding columns to add to the right of the original matrix; must be {@code >= 0}
     * @param defaultValue the value to fill all new padding cells with
     * @return a new {@code BooleanMatrix} with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         if the resulting dimensions would overflow {@code Integer.MAX_VALUE},
     *         or if the resulting shape is not representable (zero rows with a non-zero column count)
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
     * matrix.flipHorizontallyInPlace();   // reverses each row in place
     * matrix.get(0, 0);                   // returns false (was true)
     * matrix.get(0, 2);                   // returns true  (was false)
     * matrix.get(1, 0);                   // returns true  (was false)
     *
     * BooleanMatrix empty = BooleanMatrix.empty();
     * empty.flipHorizontallyInPlace();    // no-op on empty matrix
     * empty.isEmpty();                    // returns true
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
     * matrix.flipVerticallyInPlace();   // reverses the row order in place
     * matrix.get(0, 0);                 // returns false (was the bottom row)
     * matrix.get(0, 1);                 // returns true
     * matrix.get(2, 0);                 // returns true  (was the top row)
     *
     * BooleanMatrix empty = BooleanMatrix.empty();
     * empty.flipVerticallyInPlace();    // no-op on empty matrix
     * empty.isEmpty();                  // returns true
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
     * BooleanMatrix flipped = matrix.flipHorizontally();   // new matrix; original unchanged
     * flipped.get(0, 0);                                   // returns false (was the rightmost of row 0)
     * flipped.get(0, 2);                                   // returns true  (was the leftmost of row 0)
     * matrix.get(0, 0);                                    // returns true  (original unchanged)
     *
     * BooleanMatrix.empty().flipHorizontally().isEmpty();   // returns true
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
     * BooleanMatrix flipped = matrix.flipVertically();   // new matrix; original unchanged
     * flipped.get(0, 1);                                 // returns true  (was the bottom row)
     * flipped.get(1, 1);                                 // returns false (was the top row)
     * matrix.get(0, 1);                                  // returns false (original unchanged)
     *
     * BooleanMatrix.empty().flipVertically().isEmpty();   // returns true
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
     * // Original (2x3):          Rotated 90 clockwise (3x2):
     * // true  false false       true  true
     * // true  true  false       true  false
     * //                         false false
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {true, true, false}});
     * BooleanMatrix rotated = matrix.rotate90();
     * rotated.rowCount();        // returns 3
     * rotated.columnCount();     // returns 2
     * rotated.get(0, 0);         // returns true
     * rotated.get(2, 0);         // returns false
     *
     * BooleanMatrix.empty().rotate90().isEmpty();                 // returns true
     * BooleanMatrix.of(new boolean[3][0]).rotate90().isEmpty();   // returns true (zero columns)
     * }</pre>
     *
     * @return a new matrix rotated 90 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         or an empty matrix if this matrix has zero columns
     * @see #rotate180()
     * @see #rotate270()
     * @see #transpose()
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
     * Returns a new matrix that is this matrix rotated 180 degrees.
     * This is equivalent to flipping both horizontally and vertically, reversing the
     * order of all elements. The resulting matrix has the same dimensions as the original.
     * Creates a new matrix; the original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original (2x3):          Rotated 180 (2x3):
     * // true  false false       false true  true
     * // true  true  false       false false true
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {true, true, false}});
     * BooleanMatrix rotated = matrix.rotate180();
     * rotated.rowCount();        // returns 2
     * rotated.columnCount();     // returns 3
     * rotated.get(0, 0);         // returns false
     * rotated.get(0, 2);         // returns true
     *
     * BooleanMatrix.empty().rotate180().isEmpty();                     // returns true
     * BooleanMatrix.of(new boolean[3][0]).rotate180().columnCount();   // returns 0
     * }</pre>
     *
     * @return a new matrix rotated 180 degrees
     * @see #rotate90()
     * @see #rotate270()
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
     * // Original (2x3):          Rotated 270 clockwise (3x2):
     * // true  false false       false false
     * // true  true  false       false true
     * //                         true  true
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {true, true, false}});
     * BooleanMatrix rotated = matrix.rotate270();
     * rotated.rowCount();        // returns 3
     * rotated.columnCount();     // returns 2
     * rotated.get(0, 0);         // returns false
     * rotated.get(2, 0);         // returns true
     *
     * BooleanMatrix.empty().rotate270().isEmpty();                 // returns true
     * BooleanMatrix.of(new boolean[3][0]).rotate270().isEmpty();   // returns true (zero columns)
     * }</pre>
     *
     * @return a new matrix rotated 270 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         or an empty matrix if this matrix has zero columns
     * @see #rotate90()
     * @see #rotate180()
     * @see #transpose()
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
     * // Original (2x3):     Transposed (3x2):
     * // true  false false   true  false
     * // false true  false   false true
     * //                     false false
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, false}, {false, true, false}});
     * BooleanMatrix transposed = matrix.transpose();  // 2x3 becomes 3x2
     * transposed.rowCount();                          // returns 3
     * transposed.columnCount();                       // returns 2
     * transposed.get(0, 0);                           // returns true  (original (0,0))
     * transposed.get(1, 1);                           // returns true  (original (1,1))
     *
     * BooleanMatrix.empty().transpose().isEmpty();                 // returns true
     * BooleanMatrix.of(new boolean[3][0]).transpose().isEmpty();   // returns true (Nx0 -> empty)
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
     * BooleanMatrix reshaped = matrix.reshape(2, 2);   // row-major fill
     * reshaped.rowCount();                             // returns 2
     * reshaped.get(0, 0);                              // returns true
     * reshaped.get(1, 0);                              // returns true
     *
     * BooleanMatrix bigger = matrix.reshape(2, 3);   // 6 cells > 4 elements -> trailing false
     * bigger.get(1, 2);                              // returns false (unfilled trailing cell)
     * matrix.reshape(1, 3);                          // throws IllegalArgumentException (3 cells cannot hold 4 elements)
     * matrix.reshape(-1, 4);                         // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix; must be non-negative
     * @param newColumnCount the number of columns in the reshaped matrix; must be non-negative
     * @return a new {@code BooleanMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative, if the resulting shape is not
     *         representable (zero rows with a non-zero column count), if the total cell count {@code (long) newRowCount * newColumnCount}
     *         exceeds {@code Integer.MAX_VALUE}, or if the new shape is too small to hold every existing element
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public BooleanMatrix reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        checkMaterializableShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final boolean[][] c = new boolean[newRowCount][newColumnCount];

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return new BooleanMatrix(c);
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
     * repeated.rowCount();       // returns 2
     * repeated.columnCount();    // returns 6
     * repeated.get(0, 2);        // returns true  (block expanded from (0,0))
     * repeated.get(0, 3);        // returns false (block expanded from (0,1))
     *
     * matrix.repeatElements(0, 1);   // throws IllegalArgumentException (rowRepeats not positive)
     * matrix.repeatElements(1, 0);   // throws IllegalArgumentException (columnRepeats not positive)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat each element vertically; must be {@code > 0}
     * @param columnRepeats number of times to repeat each element horizontally; must be {@code > 0}
     * @return a new {@code BooleanMatrix} with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
     * @see #repeatMatrix(int, int)
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repelem.html">MATLAB repelem function</a>
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
     * tiled.rowCount();          // returns 2
     * tiled.columnCount();       // returns 6
     * tiled.get(0, 0);           // returns true
     * tiled.get(0, 1);           // returns false (tile boundary preserves original pattern)
     *
     * matrix.repeatMatrix(0, 1);   // throws IllegalArgumentException (rowRepeats not positive)
     * matrix.repeatMatrix(1, 0);   // throws IllegalArgumentException (columnRepeats not positive)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat the matrix vertically; must be {@code > 0}
     * @param columnRepeats number of times to repeat the matrix horizontally; must be {@code > 0}
     * @return a new {@code BooleanMatrix} with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
     * @see #repeatElements(int, int)
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repmat.html">MATLAB repmat function</a>
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
     * Returns a new {@link BooleanList} containing all elements of this matrix in row-major order.
     * The returned list owns its data; modifications to it do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * BooleanList list = matrix.flatten();   // row-major: [true, false, false, true]
     * list.size();                           // returns 4
     * list.get(0);                           // returns true
     * list.get(1);                           // returns false
     *
     * BooleanMatrix.empty().flatten().size();   // returns 0
     * }</pre>
     *
     * @return a new {@code BooleanList} of all elements in row-major order
     * @throws IllegalStateException if the matrix is too large to flatten
     *         (i.e. {@code (long) rowCount * columnCount > Integer.MAX_VALUE})
     * @see #horizontalStream()
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
     * Exposes the elements of this matrix to {@code action} as a single one-dimensional array
     * laid out in row-major order, then propagates any modifications back into the matrix.
     *
     * <p>This enables operations that need a global view of all matrix elements (e.g., sorting all
     * elements across the entire matrix). The shape of this matrix is preserved; only element
     * values change. See {@link Arrays#mutateAsFlat(boolean[][], Throwables.Consumer)} for the exact
     * semantics of the underlying operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.mutateAsFlat(arr -> java.util.Arrays.fill(arr, true));   // global view, written back row by row
     * matrix.countTrue();                                             // returns 4 (all elements now true)
     * matrix.get(0, 1);                                               // returns true (was false)
     *
     * int[] seen = {0};
     * matrix.mutateAsFlat(arr -> seen[0] = arr.length);   // flattened length equals total element count
     * // seen[0] is now 4
     *
     * BooleanMatrix.empty().mutateAsFlat(arr -> seen[0] = -1);   // no-op: action is not invoked on an empty matrix
     * // seen[0] is still 4
     * }</pre>
     *
     * @param <E> the type of exception that the operation may throw
     * @param action the operation to apply to the flattened array; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the operation throws an exception
     * @see Arrays#mutateAsFlat(boolean[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super boolean[], E> action) throws E {
        N.checkArgNotNull(action, "action");

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
     * BooleanMatrix result = a.and(b);   // element-wise AND
     * result.get(0, 0);                  // returns true  (true && true)
     * result.get(0, 1);                  // returns false (false && true)
     * result.get(1, 0);                  // returns false (true && false)
     *
     * a.and((BooleanMatrix) null);                                 // throws IllegalArgumentException (null other)
     * a.and(BooleanMatrix.of(new boolean[][] {{true}}));           // throws IllegalArgumentException (shape mismatch)
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
     * BooleanMatrix result = a.or(b);   // element-wise OR
     * result.get(0, 0);                 // returns true  (true || false)
     * result.get(0, 1);                 // returns true  (false || true)
     * result.get(1, 0);                 // returns false (false || false)
     *
     * a.or((BooleanMatrix) null);                                 // throws IllegalArgumentException (null other)
     * a.or(BooleanMatrix.of(new boolean[][] {{true}}));           // throws IllegalArgumentException (shape mismatch)
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
     * BooleanMatrix result = a.xor(b);   // element-wise XOR
     * result.get(0, 0);                  // returns false (true ^ true)
     * result.get(0, 1);                  // returns true  (false ^ true)
     * result.get(1, 1);                  // returns false (true ^ true)
     *
     * a.xor((BooleanMatrix) null);                                 // throws IllegalArgumentException (null other)
     * a.xor(BooleanMatrix.of(new boolean[][] {{true}}));           // throws IllegalArgumentException (shape mismatch)
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
     * matrix.countTrue();        // returns 3
     *
     * BooleanMatrix.of(new boolean[][] {{true, true}, {true, true}}).countTrue();   // returns 4
     * BooleanMatrix.of(new boolean[][] {{false, false}}).countTrue();               // returns 0
     * BooleanMatrix.empty().countTrue();                                            // returns 0
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
     * allTrue.allTrue();         // returns true
     *
     * BooleanMatrix mixed = BooleanMatrix.of(new boolean[][] {{true, false}, {true, true}});
     * mixed.allTrue();           // returns false
     *
     * BooleanMatrix.of(new boolean[][] {{false, false}}).allTrue();   // returns false
     * BooleanMatrix.empty().allTrue();                                // returns true (vacuous truth)
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
     * allFalse.anyTrue();        // returns false
     *
     * BooleanMatrix mixed = BooleanMatrix.of(new boolean[][] {{false, true}, {false, false}});
     * mixed.anyTrue();           // returns true
     *
     * BooleanMatrix.of(new boolean[][] {{true, true}}).anyTrue();   // returns true
     * BooleanMatrix.empty().anyTrue();                              // returns false
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
     * stacked.rowCount();        // returns 2
     * stacked.get(0, 0);         // returns true  (from top)
     * stacked.get(1, 1);         // returns true  (from bottom)
     *
     * BooleanMatrix m1 = BooleanMatrix.of(new boolean[][] {{true, true}});
     * BooleanMatrix m2 = BooleanMatrix.of(new boolean[][] {{false, false}});
     * BooleanMatrix combined = m1.stackVertically(m2).stackVertically(m1);
     * combined.rowCount();       // returns 3
     *
     * top.stackVertically((BooleanMatrix) null);                         // throws IllegalArgumentException (null other)
     * top.stackVertically(BooleanMatrix.of(new boolean[][] {{true}}));   // throws IllegalArgumentException (column mismatch)
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
     * stacked.columnCount();     // returns 2
     * stacked.get(0, 0);         // returns true  (from left)
     * stacked.get(0, 1);         // returns false (from right)
     *
     * BooleanMatrix col1 = BooleanMatrix.of(new boolean[][] {{true}, {true}, {false}});
     * BooleanMatrix col2 = BooleanMatrix.of(new boolean[][] {{false}, {true}, {true}});
     * BooleanMatrix wide = col1.stackHorizontally(col2);
     * wide.columnCount();        // returns 2
     *
     * left.stackHorizontally((BooleanMatrix) null);                          // throws IllegalArgumentException (null other)
     * left.stackHorizontally(BooleanMatrix.of(new boolean[][] {{true}}));    // throws IllegalArgumentException (row mismatch)
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
     * Converts this primitive {@code boolean} matrix to a boxed {@code Matrix<Boolean>}.
     * Each {@code boolean} value is converted to its corresponding {@code Boolean} wrapper object.
     *
     * <p>This conversion is useful when you need to work with APIs that require
     * object types rather than primitives, or when you need {@code null} values in the matrix.
     * Note that boxing incurs memory overhead and may impact performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix primitive = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * Matrix<Boolean> boxed = primitive.boxed();
     * boxed.rowCount();          // returns 2
     * boxed.get(0, 0);           // returns Boolean.TRUE
     * boxed.get(0, 1);           // returns Boolean.FALSE
     * boxed.set(0, 0, null);     // boxed form can hold null values
     * boxed.get(0, 0);           // returns null
     *
     * BooleanMatrix.empty().boxed().isEmpty();   // returns true
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
     * BooleanMatrix and = a.zipWith(b, (x, y) -> x && y);   // element-wise AND
     * and.get(0, 0);                                        // returns true
     * and.get(1, 0);                                        // returns false
     *
     * BooleanMatrix or = a.zipWith(b, (x, y) -> x || y);    // element-wise OR
     * or.allTrue();                                         // returns true
     *
     * a.zipWith(BooleanMatrix.of(new boolean[][] {{true}}), (x, y) -> x ^ y);  // throws IllegalArgumentException (shape mismatch)
     * a.zipWith(b, (Throwables.BooleanBinaryOperator<RuntimeException>) null); // throws IllegalArgumentException (null function)
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param zipFunction the binary operator to apply to corresponding elements; receives
     *                    element from this matrix as first argument and element from {@code other}
     *                    as second argument
     * @return a new {@code BooleanMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if {@code other} or {@code zipFunction} is {@code null},
     *         or if the matrices have different shapes
     * @throws E if the zip function throws an exception
     * @see #zipWith(BooleanMatrix, BooleanMatrix, Throwables.BooleanTernaryOperator)
     */
    public <E extends Exception> BooleanMatrix zipWith(final BooleanMatrix other, final Throwables.BooleanBinaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

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
     * conditional selection, or other three-operand logical expressions.</p>
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
     * BooleanMatrix majority = a.zipWith(b, c, (x, y, z) -> (x && y) || (x && z) || (y && z));
     * majority.get(0, 0);        // returns true  (true, true, false -> 2 trues)
     * majority.get(0, 1);        // returns true  (false, true, true -> 2 trues)
     *
     * BooleanMatrix conditional = a.zipWith(b, c, (x, y, z) -> x ? y : z);   // if a then b else c
     * conditional.get(0, 0);                                                 // returns true  (a is true -> b)
     * conditional.get(1, 0);                                                 // returns false (a is true -> b)
     *
     * a.zipWith(b, BooleanMatrix.of(new boolean[][] {{true}}), (x, y, z) -> x); // throws IllegalArgumentException (shape mismatch)
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param third the third matrix (must have the same dimensions as this matrix)
     * @param zipFunction the ternary operator to apply to corresponding elements; receives
     *                    element from this matrix as first argument, element from {@code other} as
     *                    second argument, and element from {@code third} as third argument
     * @return a new {@code BooleanMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if any of {@code other}, {@code third}, or {@code zipFunction} is {@code null},
     *         or if any of the matrices have different shapes
     * @throws E if the zip function throws an exception
     * @see #zipWith(BooleanMatrix, Throwables.BooleanBinaryOperator)
     */
    public <E extends Exception> BooleanMatrix zipWith(final BooleanMatrix other, final BooleanMatrix third,
            final Throwables.BooleanTernaryOperator<E> zipFunction) throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(third, "third");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other) && isSameShape(third), "Cannot zip matrices with different shapes: all matrices must be {}x{}", rowCount,
                columnCount);

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
     * This is useful for operations on diagonal matrices or extracting diagonal elements.
     * Because there is no primitive {@code BooleanStream}, this returns a {@code Stream<Boolean>}
     * with boxed values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true,  false, false},
     *     {false, true,  false},
     *     {false, false, false}
     * });
     * matrix.mainDiagonalStream().toList();                 // returns [true, true, false]
     * matrix.mainDiagonalStream().filter(b -> b).count();   // returns 2
     *
     * BooleanMatrix.empty().mainDiagonalStream().count();   // returns 0 (empty stream)
     * BooleanMatrix wide = BooleanMatrix.of(new boolean[][] {{true, false, true}});
     * wide.mainDiagonalStream();   // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a {@code Stream<Boolean>} containing the diagonal elements from top-left to bottom-right,
     *         or an empty stream if the matrix is empty (0 × 0)
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     */
    @Override
    public Stream<Boolean> mainDiagonalStream() {
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
     * This is useful for operations involving the secondary diagonal of a matrix.
     * Because there is no primitive {@code BooleanStream}, this returns a {@code Stream<Boolean>}
     * with boxed values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {false, false, true},
     *     {false, true,  false},
     *     {true,  false, false}
     * });
     * matrix.antiDiagonalStream().toList();                 // returns [true, true, true] (a[0][2], a[1][1], a[2][0])
     * matrix.antiDiagonalStream().filter(b -> b).count();   // returns 3
     *
     * BooleanMatrix.empty().antiDiagonalStream().count();   // returns 0 (empty stream)
     * BooleanMatrix wide = BooleanMatrix.of(new boolean[][] {{true, false, true}});
     * wide.antiDiagonalStream();   // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a {@code Stream<Boolean>} containing the anti-diagonal elements from top-right to bottom-left,
     *         or an empty stream if the matrix is empty (0 × 0)
     * @throws IllegalStateException if the matrix is not square ({@code rowCount != columnCount})
     */
    @Override
    public Stream<Boolean> antiDiagonalStream() {
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
     * matrix.horizontalStream().toList();                 // returns [true, false, false, true]
     * matrix.horizontalStream().filter(b -> b).count();   // returns 2
     * matrix.horizontalStream().findFirst().get();        // returns true
     *
     * BooleanMatrix.empty().horizontalStream().count();   // returns 0 (empty stream)
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
     *     {true,  false, true},
     *     {false, true,  false}
     * });
     * matrix.horizontalStream(0).toList();                 // returns [true, false, true]
     * matrix.horizontalStream(1).anyMatch(b -> b);         // returns true
     * matrix.horizontalStream(0).filter(b -> b).count();   // returns 2
     *
     * matrix.horizontalStream(5);   // throws IndexOutOfBoundsException (row >= rowCount)
     * matrix.horizontalStream(-1);  // throws IndexOutOfBoundsException (negative row)
     * }</pre>
     *
     * @param rowIndex the index of the row to stream (0-based)
     * @return a {@code Stream<Boolean>} of elements from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    @Override
    public Stream<Boolean> horizontalStream(final int rowIndex) {
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true,  false},
     *     {false, true},
     *     {true,  true}
     * });
     * matrix.horizontalStream(1, 3).toList();                 // returns [false, true, true, true] (rows 1 and 2)
     * matrix.horizontalStream(0, 2).filter(b -> b).count();   // returns 2 (rows 0 and 1)
     * matrix.horizontalStream(1, 1).count();                  // returns 0 (empty range)
     *
     * matrix.horizontalStream(0, 9);   // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.horizontalStream(2, 1);   // throws IndexOutOfBoundsException (fromRowIndex > toRowIndex)
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
     * <p>This method provides an alternative way to iterate through matrix
     * elements compared to the row-major order of horizontalStream(). Because there is no primitive
     * {@code BooleanStream}, this returns a {@code Stream<Boolean>} with boxed values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.verticalStream().toList();                // returns [true, false, false, true] (column-major)
     * matrix.verticalStream().filter(b -> b).count();  // returns 2
     * matrix.verticalStream().findFirst().get();       // returns true
     *
     * BooleanMatrix.empty().verticalStream().count();   // returns 0 (empty stream)
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
     *     {true, true,  false}
     * });
     * matrix.verticalStream(0).toList();                 // returns [true, true]
     * matrix.verticalStream(0).allMatch(b -> b);         // returns true
     * matrix.verticalStream(1).filter(b -> b).count();   // returns 1
     *
     * matrix.verticalStream(5);    // throws IndexOutOfBoundsException (column >= columnCount)
     * matrix.verticalStream(-1);   // throws IndexOutOfBoundsException (negative column)
     * }</pre>
     *
     * @param columnIndex the index of the column to stream (0-based)
     * @return a {@code Stream<Boolean>} of elements from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    @Override
    public Stream<Boolean> verticalStream(final int columnIndex) {
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
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true,  false, true},
     *     {false, true,  false}
     * });
     * matrix.verticalStream(1, 3).toList();                 // returns [false, true, true, false] (columns 1 and 2, column-major)
     * matrix.verticalStream(0, 2).filter(b -> b).count();   // returns 2
     * matrix.verticalStream(1, 1).count();                  // returns 0 (empty range)
     *
     * matrix.verticalStream(0, 9);   // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.verticalStream(3, 1);   // throws IndexOutOfBoundsException (fromColumnIndex > toColumnIndex)
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
     * Returns a stream of {@code Stream<Boolean>} objects, where each inner stream represents a complete row.
     * This creates a stream of streams, allowing for row-by-row processing of the matrix.
     *
     * <p>This method is useful for operations that need to process entire rows as units,
     * such as row-wise transformations, filtering rows based on conditions, or mapping
     * rows to other values. Because there is no primitive {@code BooleanStream}, each inner
     * stream is a {@code Stream<Boolean>} with boxed values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true,  false, true},
     *     {false, false, false},
     *     {true,  true,  true}
     * });
     * matrix.rowStreams().count();   // returns 3 (one inner stream per row)
     *
     * long rowsWithTrue = matrix.rowStreams().filter(row -> row.anyMatch(b -> b)).count();
     * // rowsWithTrue is 2
     *
     * int[] rowTrueCounts = matrix.rowStreams()
     *     .mapToInt(row -> (int) row.filter(b -> b).count())
     *     .toArray();   // [2, 0, 3]
     *
     * BooleanMatrix.empty().rowStreams().count();   // returns 0 (empty stream)
     * }</pre>
     *
     * @return a {@code Stream<Stream<Boolean>>}, one inner stream per row in the matrix
     */
    @Override
    public Stream<Stream<Boolean>> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Returns a stream of {@code Stream<Boolean>} objects for a range of rows.
     * Each inner stream in the result represents a complete row within the specified range.
     *
     * <p>This method allows for processing a subset of rows while maintaining the
     * ability to work with complete rows as individual streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true,  true,  false},
     *     {false, true,  true},
     *     {true,  false, true}
     * });
     * matrix.rowStreams(1, 3).count();   // returns 2 (rows 1 and 2)
     *
     * int[] trueCounts = matrix.rowStreams(1, 3)
     *     .mapToInt(row -> (int) row.filter(b -> b).count())
     *     .toArray();   // [2, 2]
     *
     * matrix.rowStreams(1, 1).count();   // returns 0 (empty range)
     * matrix.rowStreams(0, 9);           // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.rowStreams(2, 1);           // throws IndexOutOfBoundsException (fromRowIndex > toRowIndex)
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
     * Returns a stream of {@code Stream<Boolean>} objects, where each inner stream represents a complete column.
     * This creates a stream of streams, allowing for column-by-column processing of the matrix.
     *
     * <p>This method is useful for operations that need to process
     * entire columns as units, such as column-wise statistics, transformations, or filtering
     * columns based on conditions. Because there is no primitive {@code BooleanStream}, each inner
     * stream is a {@code Stream<Boolean>} with boxed values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true, false, true},
     *     {true, true,  false}
     * });
     * matrix.columnStreams().count();   // returns 3 (one inner stream per column)
     *
     * List<Boolean> allTrueColumns = matrix.columnStreams()
     *     .map(col -> col.allMatch(b -> b))
     *     .toList();   // [true, false, false]
     *
     * long[] colTrueCounts = matrix.columnStreams()
     *     .mapToLong(col -> col.filter(b -> b).count())
     *     .toArray();   // [2, 1, 1]
     *
     * BooleanMatrix.empty().columnStreams().count();   // returns 0 (empty stream)
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
     * Returns a stream of {@code Stream<Boolean>} objects for a range of columns.
     * Each inner stream in the result represents a complete column within the specified range.
     *
     * <p>This method allows for processing a subset of columns
     * while maintaining the ability to work with complete columns as individual streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {
     *     {true,  false, true,  false},
     *     {false, true,  false, true}
     * });
     * matrix.columnStreams(2, 4).count();   // returns 2 (columns 2 and 3)
     *
     * long[] colTrueCounts = matrix.columnStreams(2, 4)
     *     .mapToLong(col -> col.filter(b -> b).count())
     *     .toArray();   // [1, 1]
     *
     * matrix.columnStreams(1, 1).count();   // returns 0 (empty range)
     * matrix.columnStreams(0, 9);           // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.columnStreams(3, 1);           // throws IndexOutOfBoundsException (fromColumnIndex > toColumnIndex)
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
     * This is a hook called by {@link AbstractMatrix} during construction to determine the column
     * count of each row when validating the rectangular shape of the backing array.
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
     * int[] trueCount = {0};
     * matrix.forEach(value -> { if (value) trueCount[0]++; });
     * // trueCount[0] is now 2
     *
     * StringBuilder sb = new StringBuilder();
     * matrix.forEach(value -> sb.append(value ? "T" : "F"));
     * // sb.toString() is now "TFFT" (row-major order)
     *
     * matrix.forEach((Throwables.BooleanConsumer<RuntimeException>) null); // throws IllegalArgumentException (null action)
     * BooleanMatrix.empty().forEach(value -> trueCount[0]++);              // no-op on empty matrix
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
     *     {true,  false, true},
     *     {false, true,  false},
     *     {true,  true,  true}
     * });
     *
     * List<Boolean> center = new ArrayList<>();
     * matrix.forEach(0, 2, 0, 2, value -> center.add(value));
     * // center is now [true, false, false, true] (top-left 2x2, row-major)
     *
     * int[] bottomRowTrue = {0};
     * matrix.forEach(2, 3, 0, 3, value -> { if (value) bottomRowTrue[0]++; });
     * // bottomRowTrue[0] is now 3
     *
     * matrix.forEach(0, 9, 0, 3, value -> {});                                         // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.forEach(0, 2, 0, 2, (Throwables.BooleanConsumer<RuntimeException>) null); // throws IllegalArgumentException (null action)
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
     * {@link #ARRAY_PRINT_SEPARATOR}. If the matrix has zero rows, {@code []} is printed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.println();                             // prints two lines and returns "[true, false]" + separator + "[false, true]"
     * matrix.println().contains("[true, false]");   // returns true
     *
     * BooleanMatrix.empty().println();                        // prints "[]" and returns "[]"
     * BooleanMatrix.of(new boolean[][] {{true}}).println();   // prints and returns "[true]"
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
     * matrix1.hashCode() == matrix2.hashCode();   // returns true (same content)
     *
     * BooleanMatrix matrix3 = BooleanMatrix.of(new boolean[][] {{false, false}, {false, false}});
     * matrix1.hashCode() == matrix3.hashCode();   // returns false (different content, almost always)
     * BooleanMatrix.empty().hashCode();           // returns 1 (stable hash of the empty matrix)
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
     * m1.equals(m2);             // returns true (same shape and content)
     *
     * BooleanMatrix m3 = BooleanMatrix.of(new boolean[][] {{true, true}, {false, true}});
     * m1.equals(m3);                                                // returns false (different content)
     * m1.equals(BooleanMatrix.of(new boolean[][] {{true, false}})); // returns false (different shape)
     * m1.equals((Object) null);                                     // returns false
     * m1.equals("not a matrix");                                    // returns false
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
     * Returns a string representation of this matrix in a compact two-dimensional array format.
     * The output shows all matrix elements with rows enclosed in brackets and
     * elements separated by commas and spaces.
     *
     * <p>The format is suitable for debugging and logging. For pretty-printed output
     * with each row on a separate line, use {@link #println()} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false}, {false, true}});
     * matrix.toString();                                       // returns "[[true, false], [false, true]]"
     * BooleanMatrix.of(new boolean[][] {{true}}).toString();   // returns "[[true]]"
     *
     * BooleanMatrix.empty().toString();   // returns "[]"
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
