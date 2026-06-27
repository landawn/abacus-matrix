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
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.stream.FloatIteratorEx;
import com.landawn.abacus.util.stream.FloatStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * Matrix implementation backed by a rectangular {@code float[][]}.
 *
 * <p>This type specializes {@link AbstractMatrix} for {@code float} values while keeping the data in
 * a validated backing array. Constructors and {@link #of(float[]...)} generally wrap the supplied storage
 * directly, while factories, conversions, and mapping operations allocate new arrays.</p>
 *
 * <p>Cells introduced by growth or reshaping default to {@code 0.0f} unless an overload accepts an
 * explicit fill value.</p>
 *
 * <p><b>IEEE 754 semantics:</b> elements are single-precision floats. Be aware that
 * {@code NaN != NaN} under {@code ==}, that {@code +0.0f} and {@code -0.0f} compare equal under
 * {@code ==} but have different bit patterns, and that {@code +Infinity}/{@code -Infinity}
 * propagate through arithmetic. Equality on this matrix uses
 * {@link Float#floatToIntBits(float)} semantics (so {@code NaN} equals {@code NaN} and
 * {@code +0.0f} does <em>not</em> equal {@code -0.0f}); see {@link #equals(Object)}.</p>
 *
 * @see IntMatrix
 * @see LongMatrix
 * @see DoubleMatrix
 * @see ShortMatrix
 * @see ByteMatrix
 * @see CharMatrix
 * @see BooleanMatrix
 * @see Matrix
 */
public final class FloatMatrix extends AbstractMatrix<float[], FloatList, FloatStream, Stream<FloatStream>, FloatMatrix> {

    private static final FloatMatrix EMPTY_FLOAT_MATRIX = new FloatMatrix(new float[0][0]);

    /**
     * Constructs a {@code FloatMatrix} backed by the supplied two-dimensional array.
     *
     * <p>If {@code a} is {@code null}, this creates an empty {@code 0x0} matrix. Otherwise the array
     * is used directly after rectangular-shape validation, so later modifications to either the input
     * array or the matrix remain visible through the other view. Call {@link #copy()} if you need an
     * independently owned matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[][] data = {{1.0f, 2.0f}, {3.0f, 4.0f}};
     * FloatMatrix matrix = new FloatMatrix(data);
     * matrix.get(0, 0);                          // returns 1.0f
     * data[0][0] = 9.0f;                         // backing array is shared
     * matrix.get(0, 0);                          // returns 9.0f (change is visible)
     *
     * new FloatMatrix(null).rowCount();                      // returns 0 (empty matrix)
     * new FloatMatrix(new float[0][0]).isEmpty();            // returns true
     * new FloatMatrix(new float[][] {{1.0f}, {2.0f, 3.0f}}); // throws IllegalArgumentException (not rectangular)
     * }</pre>
     *
     * @param a the two-dimensional float array to wrap, or {@code null} for an empty matrix
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public FloatMatrix(final float[][] a) {
        super(a == null ? new float[0][0] : a, float.class);
    }

    /**
     * Returns the shared empty {@code 0x0} matrix instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.empty();
     * matrix.rowCount();                            // returns 0
     * matrix.columnCount();                         // returns 0
     * matrix.isEmpty();                             // returns true
     * FloatMatrix.empty() == FloatMatrix.empty();   // true (shared singleton)
     * }</pre>
     *
     * @return the shared empty {@code FloatMatrix} singleton
     */
    public static FloatMatrix empty() {
        return EMPTY_FLOAT_MATRIX;
    }

    /**
     * Creates a {@code FloatMatrix} from a two-dimensional float array.
     *
     * <p><b>Important:</b> When the input is non-empty the provided array is used directly without
     * defensive copying after rectangular-shape validation. Changes to the input array are reflected
     * in the returned matrix, and vice versa.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.get(0, 1);                         // returns 2.0f
     * matrix.rowCount();                        // returns 2
     *
     * FloatMatrix.of((float[][]) null).isEmpty();           // returns true
     * FloatMatrix.of(new float[0][0]).columnCount();        // returns 0
     * FloatMatrix.of(new float[][] {{1.0f}, {2.0f, 3.0f}}); // throws IllegalArgumentException (not rectangular)
     * }</pre>
     *
     * @param a the two-dimensional float array to create the matrix from, or {@code null}/empty for an empty matrix
     * @return a new {@code FloatMatrix} wrapping the provided data, or the shared empty {@code FloatMatrix} if input is {@code null} or empty
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public static FloatMatrix of(final float[]... a) {
        return N.isEmpty(a) ? EMPTY_FLOAT_MATRIX : new FloatMatrix(a);
    }

    /**
     * Creates a FloatMatrix from a two-dimensional int array by converting int values to float.
     * Each int value is converted to the nearest representable float value.
     *
     * <p><b>Note:</b> Int values with more than 24 significant bits may lose precision when
     * converted to float, since float has a 23-bit mantissa. For example,
     * {@code Integer.MAX_VALUE} (2147483647) cannot be represented exactly as a float.</p>
     *
     * <p><b>Requirements:</b></p>
     * <ul>
     *   <li>All rows must be non-{@code null} and have the same length as the first row (rectangular array required)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.from(new int[][] {{1, 2}, {3, 4}});
     * matrix.get(1, 0);                              // returns 3.0f
     * matrix.rowCount();                             // returns 2
     *
     * // Precision: 16_777_217 has 25 significant bits and rounds when stored as float
     * FloatMatrix.from(new int[][] {{16777217}}).get(0, 0); // returns 1.6777216E7f (rounded)
     * FloatMatrix.from((int[][]) null).isEmpty();           // returns true
     * FloatMatrix.from(new int[0][0]).columnCount();        // returns 0
     * FloatMatrix.from(new int[][] {{1}, {2, 3}});          // throws IllegalArgumentException (rows differ in length)
     * }</pre>
     *
     * @param a the two-dimensional int array to convert to a float matrix, or {@code null}/empty for an empty matrix
     * @return a new {@code FloatMatrix} with converted values, or the shared empty {@code FloatMatrix} if input is {@code null} or empty
     * @throws IllegalArgumentException if any row is {@code null} or if rows have different lengths (non-rectangular array)
     */
    public static FloatMatrix from(final int[]... a) {
        if (N.isEmpty(a)) {
            return EMPTY_FLOAT_MATRIX;
        }

        N.checkArgument(a[0] != null, "Row 0 cannot be null");

        final int columnCount = a[0].length;

        // Validate all rows have the same length
        for (int i = 1; i < a.length; i++) {
            N.checkArgument(a[i] != null, "Row {} cannot be null", i);
            N.checkArgument(a[i].length == columnCount, MSG_NOT_RECTANGULAR, columnCount, i, a[i].length);
        }

        final float[][] result = new float[a.length][columnCount];

        for (int i = 0, rowCount = a.length; i < rowCount; i++) {
            final int[] sourceRow = a[i];
            final float[] targetRow = result[i];

            for (int j = 0; j < columnCount; j++) {
                targetRow[j] = sourceRow[j]; // NOSONAR
            }
        }

        return new FloatMatrix(result);
    }

    /**
     * Creates a new {@code 1 x length} matrix filled with random float values
     * uniformly distributed in {@code [0.0f, 1.0f)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.random(5);
     * matrix.rowCount();                                   // returns 1
     * matrix.columnCount();                                // returns 5
     * matrix.get(0, 0) >= 0.0f && matrix.get(0, 0) < 1.0f; // returns true
     *
     * FloatMatrix.random(0).columnCount(); // returns 0 (empty row)
     * FloatMatrix.random(-1);              // throws IllegalArgumentException (negative length)
     * }</pre>
     *
     * @param length the number of columns in the new matrix
     * @return a new {@code FloatMatrix} of dimensions {@code 1 x length} filled with random values in {@code [0.0f, 1.0f)}
     * @throws IllegalArgumentException if {@code length} is negative
     * @see #random(int, int)
     */
    public static FloatMatrix random(final int length) {
        N.checkArgument(length >= 0, MSG_NEGATIVE_DIMENSION, "length", length);

        return random(1, length);
    }

    /**
     * Creates a new matrix of the specified dimensions filled with random float values
     * uniformly distributed in {@code [0.0f, 1.0f)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.random(2, 3);
     * matrix.rowCount();                                   // returns 2
     * matrix.columnCount();                                // returns 3
     * matrix.get(1, 2) >= 0.0f && matrix.get(1, 2) < 1.0f; // returns true
     *
     * FloatMatrix.random(0, 0).isEmpty(); // returns true
     * FloatMatrix.random(-1, 3);          // throws IllegalArgumentException (negative rowCount)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix
     * @param columnCount the number of columns in the new matrix
     * @return a new {@code FloatMatrix} of dimensions {@code rowCount x columnCount} filled with random values in {@code [0.0f, 1.0f)}
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if the resulting shape is not representable
     */
    public static FloatMatrix random(final int rowCount, final int columnCount) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final float[][] a = new float[rowCount][columnCount];

        for (float[] ea : a) {
            for (int i = 0; i < columnCount; i++) {
                ea[i] = RAND.nextFloat();
            }
        }

        return new FloatMatrix(a);
    }

    /**
     * Creates a new matrix of the specified dimensions where every element is the provided {@code element}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.repeat(2, 3, 1.0f);
     * matrix.get(1, 2);                              // returns 1.0f
     * matrix.rowCount();                             // returns 2
     *
     * FloatMatrix nans = FloatMatrix.repeat(1, 2, Float.NaN);
     * Float.isNaN(nans.get(0, 0));                   // returns true
     * FloatMatrix.repeat(0, 0, 5.0f).isEmpty();      // returns true
     * FloatMatrix.repeat(-1, 3, 1.0f);               // throws IllegalArgumentException (negative rowCount)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix
     * @param columnCount the number of columns in the new matrix
     * @param element the float value to fill the matrix with (may be {@code NaN}, {@code +/-Infinity}, or {@code -0.0f})
     * @return a new {@code FloatMatrix} of dimensions {@code rowCount x columnCount} with every cell equal to {@code element}
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if the resulting shape is not representable
     */
    public static FloatMatrix repeat(final int rowCount, final int columnCount, final float element) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final float[][] a = new float[rowCount][columnCount];

        for (float[] ea : a) {
            N.fill(ea, element);
        }

        return new FloatMatrix(a);
    }

    /**
     * Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
     * All other elements are set to zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.mainDiagonal(new float[] {1.0f, 2.0f, 3.0f});
     * // Creates 3x3 matrix:
     * // [[1.0, 0.0, 0.0],
     * //  [0.0, 2.0, 0.0],
     * //  [0.0, 0.0, 3.0]]
     * matrix.get(1, 1);                                       // returns 2.0f
     * matrix.get(0, 1);                                       // returns 0.0f (off-diagonal)
     *
     * FloatMatrix.mainDiagonal(new float[] {5.0f}).get(0, 0); // returns 5.0f (1x1 matrix)
     * FloatMatrix.mainDiagonal(null);                         // throws IllegalArgumentException (null array)
     * FloatMatrix.mainDiagonal(new float[0]).isEmpty();       // returns true
     * }</pre>
     *
     * @param mainDiagonal the array of main-diagonal elements; must not be {@code null}, but may be empty
     * @return a new {@code n x n} {@code FloatMatrix} (where {@code n = mainDiagonal.length}) with
     *         the supplied values on the main diagonal and {@code 0} elsewhere; the shared empty
     *         matrix if {@code mainDiagonal} is empty
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null}
     * @see #antiDiagonal(float[])
     * @see #diagonals(float[], float[])
     */
    public static FloatMatrix mainDiagonal(final float[] mainDiagonal) {
        N.checkArgNotNull(mainDiagonal, "mainDiagonal");

        return diagonals(mainDiagonal, null);
    }

    /**
     * Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
     * All other elements are set to zero. The matrix size is n×n where n is the length
     * of the diagonal array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.antiDiagonal(new float[] {1.0f, 2.0f, 3.0f});
     * // Creates 3x3 matrix:
     * // [[0.0, 0.0, 1.0],
     * //  [0.0, 2.0, 0.0],
     * //  [3.0, 0.0, 0.0]]
     * matrix.get(0, 2);                                       // returns 1.0f
     * matrix.get(0, 0);                                       // returns 0.0f (off anti-diagonal)
     *
     * FloatMatrix.antiDiagonal(new float[] {5.0f}).get(0, 0); // returns 5.0f (1x1 matrix)
     * FloatMatrix.antiDiagonal(null);                         // throws IllegalArgumentException (null array)
     * FloatMatrix.antiDiagonal(new float[0]).isEmpty();       // returns true
     * }</pre>
     *
     * @param antiDiagonal the array of anti-diagonal elements; must not be {@code null}, but may be empty
     * @return a new {@code n x n} {@code FloatMatrix} (where {@code n = antiDiagonal.length}) with
     *         the supplied values on the anti-diagonal and {@code 0} elsewhere; the shared empty
     *         matrix if {@code antiDiagonal} is empty
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null}
     * @see #mainDiagonal(float[])
     * @see #diagonals(float[], float[])
     */
    public static FloatMatrix antiDiagonal(final float[] antiDiagonal) {
        N.checkArgNotNull(antiDiagonal, "antiDiagonal");

        return diagonals(null, antiDiagonal);
    }

    /**
     * Creates a square matrix from the specified main diagonal and anti-diagonal elements.
     * All other elements are set to zero. If both arrays are non-empty, they must have the same length.
     * The resulting matrix has dimensions n×n where n is the length of the non-empty diagonal array.
     * When both diagonals are provided and they overlap (at the center element of odd-sized matrices),
     * the main diagonal value takes precedence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.diagonals(new float[] {1.0f, 2.0f, 3.0f}, new float[] {4.0f, 5.0f, 6.0f});
     * // Creates 3x3 matrix with both diagonals set:
     * //   {1.0, 0.0, 4.0},
     * //   {0.0, 2.0, 0.0},
     * //   {6.0, 0.0, 3.0}
     * matrix.get(0, 0);    // returns 1.0f (main diagonal)
     * matrix.get(0, 2);    // returns 4.0f (anti-diagonal)
     *
     * // Odd-sized center: main diagonal wins over anti-diagonal at the shared cell (1,1)
     * FloatMatrix.diagonals(new float[] {1.0f, 2.0f, 3.0f}, new float[] {7.0f, 8.0f, 9.0f}).get(1, 1); // returns 2.0f
     * FloatMatrix.diagonals(null, null);                                                               // throws IllegalArgumentException (both null)
     * FloatMatrix.diagonals(new float[] {1.0f}, new float[] {1.0f, 2.0f});                             // throws IllegalArgumentException (length mismatch)
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements; may be {@code null} or empty if {@code antiDiagonal} is non-{@code null}
     * @param antiDiagonal the array of anti-diagonal elements; may be {@code null} or empty if {@code mainDiagonal} is non-{@code null}
     * @return a square matrix with the specified diagonals, or an empty matrix when both arrays are empty (at least one being a non-{@code null} zero-length array)
     * @throws IllegalArgumentException if both {@code mainDiagonal} and {@code antiDiagonal} are {@code null}, or if both arrays are non-empty and have different lengths
     * @see #mainDiagonal(float[])
     * @see #antiDiagonal(float[])
     */
    public static FloatMatrix diagonals(final float[] mainDiagonal, final float[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(mainDiagonal != null || antiDiagonal != null, "Both 'mainDiagonal' and 'antiDiagonal' can't be null");

        N.checkArgument(N.isEmpty(mainDiagonal) || N.isEmpty(antiDiagonal) || mainDiagonal.length == antiDiagonal.length,
                "The lengths of 'mainDiagonal' and 'antiDiagonal' must be the same: mainDiagonal length={}, antiDiagonal length={}", N.len(mainDiagonal),
                N.len(antiDiagonal));

        if (N.isEmpty(mainDiagonal) && N.isEmpty(antiDiagonal)) {
            return EMPTY_FLOAT_MATRIX;
        }

        final int diagonalLength = N.max(N.len(mainDiagonal), N.len(antiDiagonal));
        final float[][] result = new float[diagonalLength][diagonalLength];

        if (N.notEmpty(antiDiagonal)) {
            for (int i = 0, j = diagonalLength - 1; i < diagonalLength; i++, j--) {
                result[i][j] = antiDiagonal[i];
            }
        }

        if (N.notEmpty(mainDiagonal)) {
            for (int i = 0; i < diagonalLength; i++) {
                result[i][i] = mainDiagonal[i]; // NOSONAR
            }
        }

        return new FloatMatrix(result);
    }

    /**
     * Converts a boxed {@code Matrix<Float>} to a primitive {@code FloatMatrix}. This conversion
     * improves memory efficiency and performance when working with large matrices.
     * {@code null} elements in the input matrix are converted to {@code 0.0f}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Float> boxed = Matrix.of(new Float[][] {{1.0f, 2.0f}, {null, 4.0f}});
     * FloatMatrix primitive = FloatMatrix.unbox(boxed);
     * primitive.get(0, 1);                          // returns 2.0f
     * primitive.get(1, 0);                          // returns 0.0f (null converted to 0.0f)
     *
     * FloatMatrix.unbox(Matrix.of(new Float[0][0])).isEmpty(); // returns true
     * FloatMatrix.unbox(null);                                 // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the boxed {@code Matrix<Float>} to convert; must not be {@code null}
     * @return a new {@code FloatMatrix} with primitive float values, with the same shape as {@code x}
     * @throws IllegalArgumentException if {@code x} is {@code null}
     * @see #boxed()
     */
    public static FloatMatrix unbox(final Matrix<Float> x) {
        N.checkArgNotNull(x, "x");

        return FloatMatrix.of(Array.unbox(x.a));
    }

    /**
     * Returns the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.get(0, 1);    // returns 2.0f
     * matrix.get(1, 0);    // returns 3.0f
     *
     * matrix.get(2, 0);    // throws ArrayIndexOutOfBoundsException (row out of range)
     * matrix.get(0, 5);    // throws ArrayIndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return the element at position {@code (rowIndex, columnIndex)}
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public float get(final int rowIndex, final int columnIndex) {
        return a[rowIndex][columnIndex];
    }

    /**
     * Returns the element at the specified point.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.get(Point.of(0, 1));   // returns 2.0f
     * matrix.get(Point.of(1, 0));   // returns 3.0f
     *
     * matrix.get(Point.of(5, 0));   // throws ArrayIndexOutOfBoundsException (out of range)
     * matrix.get((Point) null);     // throws IllegalArgumentException (null point)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @return the float element at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #get(int, int)
     */
    public float get(final Point point) {
        N.checkArgNotNull(point, "point");

        return a[point.rowIndex()][point.columnIndex()];
    }

    /**
     * Sets the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.set(0, 1, 9.0f);
     * matrix.get(0, 1);              // returns 9.0f (value updated)
     *
     * matrix.set(1, 0, Float.NaN);
     * Float.isNaN(matrix.get(1, 0)); // returns true
     * matrix.set(5, 0, 1.0f);        // throws ArrayIndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @param value the new value to set at the specified position
     * @throws ArrayIndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public void set(final int rowIndex, final int columnIndex, final float value) {
        a[rowIndex][columnIndex] = value;
    }

    /**
     * Sets the element at the specified point to the given value.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.set(Point.of(0, 1), 9.0f);
     * matrix.get(Point.of(0, 1));       // returns 9.0f (value updated)
     *
     * matrix.set(Point.of(5, 0), 1.0f); // throws ArrayIndexOutOfBoundsException (out of range)
     * matrix.set((Point) null, 1.0f);   // throws IllegalArgumentException (null point)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @param value the new float value to set at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #set(int, int, float)
     */
    public void set(final Point point, final float value) {
        N.checkArgNotNull(point, "point");

        a[point.rowIndex()][point.columnIndex()] = value;
    }

    /**
     * Returns the element directly above the specified position, or an empty {@link OptionalFloat}
     * if the position is on the top edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalFloat} is returned for the top
     * row instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.valueAbove(1, 0).get();          // returns 1.0f
     * matrix.valueAbove(1, 1).get();          // returns 2.0f
     *
     * matrix.valueAbove(0, 0).isPresent();    // returns false (top row, no cell above)
     * matrix.valueAbove(2, 0);                // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalFloat} containing the element at position {@code (rowIndex - 1, columnIndex)},
     *         or empty if {@code rowIndex == 0}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalFloat valueAbove(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? OptionalFloat.empty() : OptionalFloat.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, or an empty {@link OptionalFloat}
     * if the position is on the bottom edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalFloat} is returned for the
     * bottom row instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.valueBelow(0, 0).get();          // returns 3.0f
     * matrix.valueBelow(0, 1).get();          // returns 4.0f
     *
     * matrix.valueBelow(1, 0).isPresent();    // returns false (bottom row, no cell below)
     * matrix.valueBelow(2, 0);                // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalFloat} containing the element at position {@code (rowIndex + 1, columnIndex)},
     *         or empty if {@code rowIndex == rowCount - 1}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalFloat valueBelow(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? OptionalFloat.empty() : OptionalFloat.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, or an empty
     * {@link OptionalFloat} if the position is on the leftmost edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalFloat} is returned for the
     * leftmost column instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.valueLeft(0, 1).get();           // returns 1.0f
     * matrix.valueLeft(1, 1).get();           // returns 3.0f
     *
     * matrix.valueLeft(0, 0).isPresent();     // returns false (leftmost column, no cell to the left)
     * matrix.valueLeft(0, 2);                 // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalFloat} containing the element at position {@code (rowIndex, columnIndex - 1)},
     *         or empty if {@code columnIndex == 0}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalFloat valueLeft(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? OptionalFloat.empty() : OptionalFloat.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, or an empty
     * {@link OptionalFloat} if the position is on the rightmost edge of the matrix.
     * This method provides safe edge handling: an empty {@code OptionalFloat} is returned for the
     * rightmost column instead of an out-of-bounds exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.valueRight(0, 0).get();          // returns 2.0f
     * matrix.valueRight(1, 0).get();          // returns 4.0f
     *
     * matrix.valueRight(0, 1).isPresent();    // returns false (rightmost column, no cell to the right)
     * matrix.valueRight(0, 2);                // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index of the reference cell (0-based)
     * @param columnIndex the column index of the reference cell (0-based)
     * @return an {@link OptionalFloat} containing the element at position {@code (rowIndex, columnIndex + 1)},
     *         or empty if {@code columnIndex == columnCount - 1}
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    public OptionalFloat valueRight(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? OptionalFloat.empty() : OptionalFloat.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as a live reference to the underlying {@code float[]} storage.
     *
     * <p><b>Note:</b> This method returns the internal array, not a copy. Modifications to the
     * returned array will affect the matrix and vice versa. Use {@link #rowCopy(int)} if you need
     * an independent copy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * matrix.rowView(0);                      // returns [1.0f, 2.0f, 3.0f]
     * matrix.rowView(1);                      // returns [4.0f, 5.0f, 6.0f]
     *
     * float[] firstRow = matrix.rowView(0);
     * firstRow[0] = 99.0f;
     * matrix.get(0, 0);                       // returns 99.0f (live view is shared)
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
    public float[] rowView(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return a[rowIndex];
    }

    /**
     * Returns a defensive copy of the specified row as a new {@code float[]}.
     * Changes to the returned array do not affect this matrix and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * matrix.rowCopy(0);                      // returns [1.0f, 2.0f, 3.0f]
     * matrix.rowCopy(1);                      // returns [4.0f, 5.0f, 6.0f]
     *
     * float[] firstRow = matrix.rowCopy(0);
     * firstRow[0] = 99.0f;
     * matrix.get(0, 0);                       // returns 1.0f (copy is independent)
     *
     * matrix.rowCopy(-1);                     // throws IndexOutOfBoundsException
     * matrix.rowCopy(2);                      // throws IndexOutOfBoundsException (rowIndex >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new float array of length {@code columnCount} containing the values of the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowView(int)
     * @see #columnCopy(int)
     */
    @Override
    public float[] rowCopy(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return N.copyOf(a[rowIndex], columnCount);
    }

    /**
     * Returns a defensive copy of the specified column as a new {@code float[]}.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously in memory. Modifications to the returned array
     * will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * matrix.columnCopy(0);                   // returns [1.0f, 4.0f]
     * matrix.columnCopy(2);                   // returns [3.0f, 6.0f]
     *
     * float[] firstColumn = matrix.columnCopy(0);
     * firstColumn[0] = 99.0f;
     * matrix.get(0, 0);                       // returns 1.0f (copy is independent)
     *
     * matrix.columnCopy(-1);                  // throws IndexOutOfBoundsException
     * matrix.columnCopy(3);                   // throws IndexOutOfBoundsException (columnIndex >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new float array of length {@code rowCount} containing the values of the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     * @see #rowCopy(int)
     * @see #rowView(int)
     */
    @Override
    public float[] columnCopy(final int columnIndex) throws IndexOutOfBoundsException {
        checkColumnIndex(columnIndex);

        final float[] columnValues = new float[rowCount];

        for (int i = 0; i < rowCount; i++) {
            columnValues[i] = a[i][columnIndex];
        }

        return columnValues;
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * matrix.setRow(0, new float[] {7.0f, 8.0f, 9.0f});
     * matrix.get(0, 0);                              // returns 7.0f
     * matrix.get(0, 2);                              // returns 9.0f
     *
     * matrix.setRow(0, new float[] {1.0f, 2.0f});       // throws IllegalArgumentException (length != columnCount)
     * matrix.setRow(5, new float[] {1.0f, 2.0f, 3.0f}); // throws IndexOutOfBoundsException (row out of range)
     * matrix.setRow(0, (float[]) null);                 // throws IllegalArgumentException
     * }</pre>
     *
     * @param rowIndex the index of the row to set (0-based)
     * @param row the array of values to copy into the row; must have length equal to the number of columns
     * @throws IndexOutOfBoundsException if {@code rowIndex} is out of bounds
     * @throws IllegalArgumentException if {@code row} is {@code null} or if {@code row.length != columnCount}
     */
    public void setRow(final int rowIndex, final float[] row) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * matrix.setColumn(0, new float[] {7.0f, 8.0f});
     * matrix.get(0, 0);                              // returns 7.0f
     * matrix.get(1, 0);                              // returns 8.0f
     *
     * matrix.setColumn(0, new float[] {1.0f});       // throws IllegalArgumentException (length != rowCount)
     * matrix.setColumn(5, new float[] {1.0f, 2.0f}); // throws IndexOutOfBoundsException (column out of range)
     * matrix.setColumn(0, (float[]) null);           // throws IllegalArgumentException
     * }</pre>
     *
     * @param columnIndex the index of the column to set (0-based)
     * @param column the array of values to copy into the column; must have length equal to the number of rows
     * @throws IndexOutOfBoundsException if {@code columnIndex} is out of bounds
     * @throws IllegalArgumentException if {@code column} is {@code null} or if {@code column.length != rowCount}
     */
    public void setColumn(final int columnIndex, final float[] column) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * matrix.updateRow(0, x -> x * 2);   // matrix row 0 is now [2.0f, 4.0f, 6.0f]
     * matrix.get(0, 1);                  // returns 4.0f
     * matrix.get(1, 0);                  // returns 4.0f (row 1 unchanged)
     *
     * matrix.updateRow(0, x -> x / 0.0f);          // row 0 becomes Infinity values
     * matrix.get(0, 0) == Float.POSITIVE_INFINITY; // returns true
     * matrix.updateRow(5, x -> x);                 // throws IndexOutOfBoundsException (row out of range)
     * matrix.updateRow(0, null);                   // throws IllegalArgumentException (operator is null)
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
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.FloatUnaryOperator<E> operator)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        N.checkArgNotNull(operator, "operator");

        checkRowIndex(rowIndex);

        for (int i = 0; i < columnCount; i++) {
            a[rowIndex][i] = operator.applyAsFloat(a[rowIndex][i]);
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}, {5.0f, 6.0f}});
     * matrix.updateColumn(0, x -> x + 10.0f);   // column 0 becomes [11.0f, 13.0f, 15.0f]
     * matrix.get(0, 0);                         // returns 11.0f
     * matrix.get(2, 0);                         // returns 15.0f
     * matrix.get(0, 1);                         // returns 2.0f (column 1 unchanged)
     *
     * matrix.updateColumn(5, x -> x);           // throws IndexOutOfBoundsException (column out of range)
     * matrix.updateColumn(0, null);             // throws IllegalArgumentException (operator is null)
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
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.FloatUnaryOperator<E> operator)
            throws IndexOutOfBoundsException, IllegalArgumentException, E {
        N.checkArgNotNull(operator, "operator");

        checkColumnIndex(columnIndex);

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = operator.applyAsFloat(a[i][columnIndex]);
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}, {7.0f, 8.0f, 9.0f}});
     * matrix.getMainDiagonal();   // returns [1.0f, 5.0f, 9.0f]
     *
     * FloatMatrix.of(new float[][] {{5.0f}}).getMainDiagonal();             // returns [5.0f] (1x1)
     * FloatMatrix.empty().getMainDiagonal();                                // returns [] (empty, square 0x0)
     * FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}}).getMainDiagonal(); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new float array containing a copy of the main diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public float[] getMainDiagonal() throws IllegalStateException {
        checkIsSquare();

        final float[] diagonal = new float[rowCount];

        for (int i = 0; i < rowCount; i++) {
            diagonal[i] = a[i][i]; // NOSONAR
        }

        return diagonal;
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.setMainDiagonal(new float[] {9.0f, 8.0f});
     * matrix.get(0, 0);                              // returns 9.0f
     * matrix.get(1, 1);                              // returns 8.0f
     * matrix.get(0, 1);                              // returns 2.0f (off-diagonal unchanged)
     *
     * matrix.setMainDiagonal(new float[] {1.0f});                                             // throws IllegalArgumentException (length != rowCount)
     * FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}}).setMainDiagonal(new float[] {1.0f}); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must be non-{@code null} and of length {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null} or its length is not equal to {@code rowCount}
     */
    @Override
    public void setMainDiagonal(final float[] mainDiagonal) throws IllegalStateException, IllegalArgumentException {
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.updateMainDiagonal(x -> x * x);   // squares the diagonal: (0,0)->1, (1,1)->16
     * matrix.get(0, 0);                        // returns 1.0f
     * matrix.get(1, 1);                        // returns 16.0f
     * matrix.get(0, 1);                        // returns 2.0f (off-diagonal unchanged)
     *
     * matrix.updateMainDiagonal(x -> x / 0.0f);                                      // 1.0f/0.0f -> Infinity at (0,0)
     * matrix.get(0, 0) == Float.POSITIVE_INFINITY;                                   // returns true
     * matrix.updateMainDiagonal(null);                                               // throws IllegalArgumentException (operator is null)
     * FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}}).updateMainDiagonal(x -> x); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each diagonal element; receives current element value and returns new value
     * @throws IllegalStateException if the matrix is not square
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateMainDiagonal(final Throwables.FloatUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = operator.applyAsFloat(a[i][i]);
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}, {7.0f, 8.0f, 9.0f}});
     * matrix.getAntiDiagonal();   // returns [3.0f, 5.0f, 7.0f]
     *
     * FloatMatrix.of(new float[][] {{5.0f}}).getAntiDiagonal();             // returns [5.0f] (1x1)
     * FloatMatrix.empty().getAntiDiagonal();                                // returns [] (empty, square 0x0)
     * FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}}).getAntiDiagonal(); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new float array containing a copy of the anti-diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public float[] getAntiDiagonal() throws IllegalStateException {
        checkIsSquare();

        final float[] diagonal = new float[rowCount];

        for (int i = 0; i < rowCount; i++) {
            diagonal[i] = a[i][columnCount - i - 1];
        }

        return diagonal;
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.setAntiDiagonal(new float[] {9.0f, 8.0f});
     * matrix.get(0, 1);                              // returns 9.0f
     * matrix.get(1, 0);                              // returns 8.0f
     * matrix.get(0, 0);                              // returns 1.0f (off anti-diagonal unchanged)
     *
     * matrix.setAntiDiagonal(new float[] {1.0f});                                             // throws IllegalArgumentException (length != rowCount)
     * FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}}).setAntiDiagonal(new float[] {1.0f}); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must be non-{@code null} and of length {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null} or its length is not equal to {@code rowCount}
     */
    @Override
    public void setAntiDiagonal(final float[] antiDiagonal) throws IllegalStateException, IllegalArgumentException {
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.updateAntiDiagonal(x -> -x);   // negates anti-diagonal: (0,1)->-2, (1,0)->-3
     * matrix.get(0, 1);                     // returns -2.0f
     * matrix.get(1, 0);                     // returns -3.0f
     * matrix.get(0, 0);                     // returns 1.0f (off anti-diagonal unchanged)
     *
     * matrix.updateAntiDiagonal(x -> x / 0.0f);                                      // -2.0f/0.0f -> -Infinity at (0,1)
     * matrix.get(0, 1) == Float.NEGATIVE_INFINITY;                                   // returns true
     * matrix.updateAntiDiagonal(null);                                               // throws IllegalArgumentException (operator is null)
     * FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}}).updateAntiDiagonal(x -> x); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each anti-diagonal element; receives current element value and returns new value
     * @throws IllegalStateException if the matrix is not square
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAntiDiagonal(final Throwables.FloatUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = operator.applyAsFloat(a[i][columnCount - i - 1]);
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.updateAll(x -> x * 2);   // matrix is now [[2.0f, 4.0f], [6.0f, 8.0f]]
     * matrix.get(0, 0);               // returns 2.0f
     * matrix.get(1, 1);               // returns 8.0f
     *
     * matrix.updateAll(x -> 0.0f / 0.0f);        // every cell becomes NaN
     * Float.isNaN(matrix.get(0, 0));             // returns true
     * FloatMatrix.empty().updateAll(x -> x * 2); // no-op on empty matrix (no exception)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each element; receives the current element value
     *             and returns the new value
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.FloatUnaryOperator<E> operator) throws E {
        N.checkArgNotNull(operator, "operator");

        if (Matrices.isParallelizable(this)) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = operator.applyAsFloat(a[i][j]);
            Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
        } else {
            for (int i = 0; i < rowCount; i++) {
                final float[] row = a[i];

                for (int j = 0; j < columnCount; j++) {
                    row[j] = operator.applyAsFloat(row[j]);
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{0.0f, 0.0f, 0.0f}, {0.0f, 0.0f, 0.0f}});
     * matrix.updateAll((i, j) -> (float) (i + j));   // matrix is now [[0.0f, 1.0f, 2.0f], [1.0f, 2.0f, 3.0f]]
     * matrix.get(1, 2);                              // returns 3.0f
     *
     * matrix.updateAll((i, j) -> i * 10.0f + j);     // position encoding: [[0.0f, 1.0f, 2.0f], [10.0f, 11.0f, 12.0f]]
     * matrix.get(1, 1);                              // returns 11.0f
     * FloatMatrix.empty().updateAll((i, j) -> 1.0f); // no-op on empty matrix (no exception)
     * matrix.updateAll((i, j) -> (Float) null);      // throws NullPointerException (null auto-unboxing)
     * }</pre>
     *
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the function that receives row index and column index (0-based) and returns
     *             the new value for that position; the returned {@code Float} is unboxed, so it
     *             must not be {@code null}
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws NullPointerException if {@code mapper} returns {@code null} for any position
     * @throws E if the mapper throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Float, E> mapper) throws E {
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
     * <p><b>Floating-point note:</b> {@code NaN} fails ordering comparisons such as {@code <},
     * {@code >}, {@code <=}, {@code >=} and is not equal to itself under {@code ==}. To match
     * {@code NaN} cells use {@link Float#isNaN(float)} (e.g. {@code x -> Float.isNaN(x)}); to
     * match {@code +/-Infinity} use {@link Float#isInfinite(float)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{-1.0f, 2.0f, -3.0f}, {4.0f, -5.0f, 6.0f}});
     * matrix.replaceIf(x -> x < 0, 0.0f);       // matrix is now [[0.0f, 2.0f, 0.0f], [4.0f, 0.0f, 6.0f]]
     * matrix.get(0, 0);                         // returns 0.0f
     *
     * matrix.replaceIf(x -> x > 3.0f, 99.0f);   // matrix is now [[0.0f, 2.0f, 0.0f], [99.0f, 0.0f, 99.0f]]
     * matrix.get(1, 0);                         // returns 99.0f
     *
     * // NaN note: ordering comparisons fail on NaN, so use Float.isNaN to match it
     * FloatMatrix nans = FloatMatrix.of(new float[][] {{Float.NaN, 1.0f}});
     * nans.replaceIf(x -> Float.isNaN(x), 0.0f);   // NaN cell replaced with 0.0f
     * nans.get(0, 0);                              // returns 0.0f
     * matrix.replaceIf(x -> x == 0.0f, Float.NaN); // matching cells set to NaN
     * Float.isNaN(matrix.get(0, 0));               // returns true
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to test each element; elements for which this returns
     *                  {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements (may be {@code NaN},
     *                 {@code +/-Infinity}, or {@code -0.0f})
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.FloatPredicate<E> predicate, final float newValue) throws E {
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}, {7.0f, 8.0f, 9.0f}});
     * matrix.replaceIf((i, j) -> i == j, 0.0f);   // main diagonal -> 0: [[0,2,3],[4,0,6],[7,8,0]]
     * matrix.get(1, 1);                           // returns 0.0f
     *
     * matrix.replaceIf((i, j) -> i == 0 || j == 0, 99.0f); // first row/column -> 99
     * matrix.get(0, 0);                                    // returns 99.0f
     * matrix.get(2, 2);                                    // returns 0.0f (unchanged by this step)
     * FloatMatrix.empty().replaceIf((i, j) -> true, 1.0f); // no-op on empty matrix (no exception)
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to test each position; receives row index and column index (0-based)
     *                  and returns {@code true} if the element at that position should be replaced
     * @param newValue the value to use for replacing at matching positions
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final float newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = predicate.test(i, j) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));
    }

    /**
     * Creates a new {@code FloatMatrix} by applying the specified function to each element of this matrix.
     * The original matrix is not modified. Each element is transformed independently by the function,
     * and the results are collected into a new matrix with the same dimensions. The operation may be
     * performed in parallel for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix squared = matrix.map(f -> f * f);
     * squared.get(1, 1);                   // returns 16.0f
     * matrix.get(1, 1);                    // returns 4.0f (original unchanged)
     *
     * FloatMatrix recip = matrix.map(f -> 1.0f / f);
     * recip.get(0, 0);                     // returns 1.0f
     * FloatMatrix divZero = matrix.map(f -> f / 0.0f);
     * divZero.get(0, 0) == Float.POSITIVE_INFINITY; // returns true
     * FloatMatrix.empty().map(f -> f).isEmpty();    // returns true
     * }</pre>
     *
     * @param <E> the exception type that the function may throw
     * @param mapper the mapping function to apply to each element; must not be {@code null}
     * @return a new {@code FloatMatrix} with the mapped values (same dimensions as the original)
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     * @see #updateAll(Throwables.FloatUnaryOperator)
     */
    public <E extends Exception> FloatMatrix map(final Throwables.FloatUnaryOperator<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final float[][] result = new float[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsFloat(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return FloatMatrix.of(result);
    }

    /**
     * Creates a new object {@code Matrix} by applying the specified function to each element of this matrix.
     * The original matrix is not modified. Each {@code float} element is independently converted to an object
     * of type {@code R} by the function, and the results are collected into a new {@code Matrix} with the same dimensions.
     * The operation may be performed in parallel for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * Matrix<String> labels = matrix.mapToObj(f -> "v" + f, String.class);
     * labels.get(0, 0);                   // returns "v1.0"
     * labels.get(1, 1);                   // returns "v4.0"
     *
     * Matrix<Integer> ints = matrix.mapToObj(f -> (int) f, Integer.class);
     * ints.get(1, 0);                                                 // returns 3
     * FloatMatrix.empty().mapToObj(f -> "x", String.class).isEmpty(); // returns true
     * }</pre>
     *
     * @param <R> the type of elements in the resulting matrix
     * @param <E> the exception type that the function may throw
     * @param mapper the mapping function that converts each {@code float} element to type {@code R}; must not be {@code null}
     * @param targetElementType the class object representing the target element type (used for array creation); must not be {@code null}
     * @return a new {@code Matrix<R>} with the mapped values (same dimensions as the original)
     * @throws IllegalArgumentException if {@code mapper} or {@code targetElementType} is {@code null}
     * @throws E if the function throws an exception
     */
    public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.FloatFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E {
        N.checkArgNotNull(mapper, "mapper");
        N.checkArgNotNull(targetElementType, "targetElementType");
        final R[][] result = Matrices.newMatrixArray(rowCount, columnCount, targetElementType);
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.apply(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return Matrix.of(result);
    }

    /**
     * Fills the entire matrix with the specified value in-place.
     * This method modifies the matrix directly, setting every element to the same value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.fill(0.0f);
     * matrix.get(0, 0);              // returns 0.0f
     * matrix.get(1, 1);              // returns 0.0f
     *
     * FloatMatrix ones = FloatMatrix.of(new float[3][3]);
     * ones.fill(1.0f);
     * ones.get(2, 2);               // returns 1.0f
     * matrix.fill(Float.NaN);
     * Float.isNaN(matrix.get(0, 0));  // returns true
     * FloatMatrix.empty().fill(1.0f); // no-op on empty matrix (no exception)
     * }</pre>
     *
     * @param value the value to fill the matrix with
     */
    public void fill(final float value) {
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
     * FloatMatrix matrix = FloatMatrix.of(new float[3][3]);
     * matrix.fill(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.get(0, 0);                              // returns 1.0f
     * matrix.get(1, 1);                              // returns 4.0f
     * matrix.get(2, 2);                              // returns 0.0f (outside the copied region)
     *
     * // Source larger than matrix: only the portion that fits is copied
     * FloatMatrix small = FloatMatrix.of(new float[1][1]);
     * small.fill(new float[][] {{7.0f, 8.0f}, {9.0f, 10.0f}});
     * small.get(0, 0);                               // returns 7.0f
     * matrix.fill((float[][]) null);                 // throws IllegalArgumentException (null source)
     * }</pre>
     *
     * @param source the two-dimensional array to copy values from; must not be {@code null}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     * @see #fill(int, int, float[][])
     */
    public void fill(final float[][] source) {
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
     * FloatMatrix matrix = FloatMatrix.of(new float[3][3]);
     * matrix.fill(1, 1, new float[][] {{9.0f, 8.0f}, {7.0f, 6.0f}});
     * matrix.get(1, 1);                              // returns 9.0f
     * matrix.get(2, 2);                              // returns 6.0f
     * matrix.get(0, 0);                              // returns 0.0f (outside the filled region)
     *
     * matrix.fill(3, 0, new float[][] {{1.0f}});    // destRowIndex == rowCount: nothing copied (no exception)
     * matrix.fill(-1, 0, new float[][] {{1.0f}});   // throws IndexOutOfBoundsException (negative destRowIndex)
     * matrix.fill(0, 0, (float[][]) null);          // throws IllegalArgumentException (null source)
     * }</pre>
     *
     * @param destRowIndex the target row index in this matrix (0-based, must satisfy {@code 0 <= destRowIndex <= rowCount})
     * @param destColumnIndex the target column index in this matrix (0-based, must satisfy {@code 0 <= destColumnIndex <= columnCount})
     * @param source the source array to copy values from; must not be {@code null}
     * @throws IndexOutOfBoundsException if {@code destRowIndex < 0} or {@code destRowIndex > rowCount},
     *         or if {@code destColumnIndex < 0} or {@code destColumnIndex > columnCount}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final float[][] source) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     * FloatMatrix original = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix copy = original.copy();
     * copy.get(0, 0);                       // returns 1.0f
     * copy.equals(original);                // returns true
     *
     * copy.set(0, 0, 99.0f);
     * original.get(0, 0);                   // returns 1.0f (original unchanged)
     * copy.get(0, 0);                       // returns 99.0f (copy modified)
     *
     * FloatMatrix.empty().copy().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix that is a copy of this matrix with full independence guarantee
     */
    @Override
    public FloatMatrix copy() {
        final float[][] c = new float[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[i].clone();
        }

        return new FloatMatrix(c);
    }

    /**
     * Creates a copy of a row range from this matrix.
     * The returned matrix contains only the specified rows and is completely independent from the original matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}, {5.0f, 6.0f}});
     * FloatMatrix subset = matrix.copy(1, 3);
     * subset.rowCount();                         // returns 2
     * subset.get(0, 0);                          // returns 3.0f -> {{3.0f, 4.0f}, {5.0f, 6.0f}}
     *
     * matrix.copy(1, 1).rowCount();              // returns 0 (empty range)
     *
     * matrix.copy(-1, 2);                        // throws IndexOutOfBoundsException (fromRowIndex < 0)
     * matrix.copy(0, 5);                         // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new {@code FloatMatrix} containing the specified rows
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public FloatMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        final float[][] c = new float[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = a[i].clone();
        }

        return new FloatMatrix(c);
    }

    /**
     * Creates a copy of a submatrix defined by row and column ranges.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}, {7.0f, 8.0f, 9.0f}});
     * FloatMatrix submatrix = matrix.copy(0, 2, 1, 3);
     * submatrix.get(0, 0);                         // returns 2.0f
     * submatrix.get(1, 1);                         // returns 6.0f -> {{2.0f, 3.0f}, {5.0f, 6.0f}}
     *
     * matrix.copy(0, 1, 0, 1).get(0, 0);           // returns 1.0f (single-cell submatrix)
     *
     * matrix.copy(0, 2, 1, 5);                     // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.copy(-1, 2, 0, 2);                    // throws IndexOutOfBoundsException (fromRowIndex < 0)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new {@code FloatMatrix} containing the specified submatrix
     * @throws IndexOutOfBoundsException if any range is invalid (e.g. {@code fromRowIndex < 0},
     *         {@code toRowIndex > rowCount}, {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code from > to} for either range)
     */
    @Override
    public FloatMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        final float[][] c = new float[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = N.copyOfRange(a[i], fromColumnIndex, toColumnIndex);
        }

        return new FloatMatrix(c);
    }

    /**
     * Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount},
     * anchored at the top-left corner of this matrix. New cells are filled with {@code 0.0f}.
     *
     * <ul>
     *   <li><b>If a dimension shrinks</b> — elements beyond the new boundary are discarded
     *       (excess rows removed from the bottom, excess columns removed from the right).</li>
     *   <li><b>If a dimension grows</b> — new cells are filled with {@code 0.0f}.</li>
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}, {7.0f, 8.0f, 9.0f}});
     *
     * // Grow: both dimensions larger — new cells filled with 0.0f
     * FloatMatrix grown = matrix.resize(4, 4);
     * // Result: [[1.0, 2.0, 3.0, 0.0],
     * //          [4.0, 5.0, 6.0, 0.0],
     * //          [7.0, 8.0, 9.0, 0.0],
     * //          [0.0, 0.0, 0.0, 0.0]]
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * FloatMatrix truncated = matrix.resize(2, 2);
     * // Result: [[1.0, 2.0],
     * //          [4.0, 5.0]]
     *
     * // Mixed: grow rows, truncate columns
     * FloatMatrix mixed = matrix.resize(4, 2);
     * // Result: [[1.0, 2.0],
     * //          [4.0, 5.0],
     * //          [7.0, 8.0],
     * //          [0.0, 0.0]]
     *
     * grown.get(3, 3);               // returns 0.0f (new cell)
     * truncated.rowCount();          // returns 2
     * matrix.resize(0, 0).isEmpty(); // returns true
     * matrix.resize(-1, 2);          // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new {@code FloatMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int, float)
     * @see #extend(int, int, int, int)
     */
    public FloatMatrix resize(final int newRowCount, final int newColumnCount) {
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
     * <p><b>Comparison with {@link #extend(int, int, int, int, float)}:</b>
     * {@code resize} takes <em>absolute</em> target dimensions and may truncate existing content.
     * {@code extend} takes <em>relative</em> padding amounts per edge and <em>never truncates</em>.
     * Use {@code extend} when the entire original content must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}, {7.0f, 8.0f, 9.0f}});
     *
     * // Grow: both dimensions larger — new cells filled with 9.0f
     * FloatMatrix grown = matrix.resize(4, 4, 9.0f);
     * // Result: [[1.0, 2.0, 3.0, 9.0],
     * //          [4.0, 5.0, 6.0, 9.0],
     * //          [7.0, 8.0, 9.0, 9.0],
     * //          [9.0, 9.0, 9.0, 9.0]]
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * FloatMatrix truncated = matrix.resize(2, 2, 9.0f);
     * // Result: [[1.0, 2.0],
     * //          [4.0, 5.0]]
     *
     * // Mixed: grow rows, truncate columns
     * FloatMatrix mixed = matrix.resize(4, 2, 9.0f);
     * // Result: [[1.0, 2.0],
     * //          [4.0, 5.0],
     * //          [7.0, 8.0],
     * //          [9.0, 9.0]]
     *
     * grown.get(3, 3);                          // returns 9.0f (new cell uses defaultValue)
     * grown.get(0, 0);                          // returns 1.0f (original content preserved)
     * matrix.resize(4, 4, Float.NaN).get(3, 3); // returns NaN (verify with Float.isNaN)
     * matrix.resize(2, -1, 9.0f);               // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the float value used to fill any newly created cells (may be {@code NaN},
     *                     {@code +/-Infinity}, or {@code -0.0f})
     * @return a new {@code FloatMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int)
     * @see #extend(int, int, int, int, float)
     */
    public FloatMatrix resize(final int newRowCount, final int newColumnCount, final float defaultValue) throws IllegalArgumentException {
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
            final boolean fillDefaultValue = Float.floatToRawIntBits(defaultValue) != 0;
            final float[][] b = new float[newRowCount][];

            for (int i = 0; i < newRowCount; i++) {
                b[i] = i < rowCount ? N.copyOf(a[i], newColumnCount) : new float[newColumnCount];

                if (fillDefaultValue) {
                    if (i >= rowCount) {
                        N.fill(b[i], defaultValue);
                    } else if (columnCount < newColumnCount) {
                        N.fill(b[i], columnCount, newColumnCount, defaultValue);
                    }
                }
            }

            return new FloatMatrix(b);
        }
    }

    /**
     * Returns a new matrix formed by adding {@code 0.0f}-filled padding around every edge of this matrix.
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}});
     *
     * // Uniform border of 1 cell on every side
     * FloatMatrix bordered = matrix.extend(1, 1, 1, 1);
     * // Result: [[0.0, 0.0, 0.0, 0.0],
     * //          [0.0, 1.0, 2.0, 0.0],
     * //          [0.0, 0.0, 0.0, 0.0]]
     * bordered.rowCount();            // returns 3
     * bordered.columnCount();         // returns 4
     * bordered.get(1, 1);             // returns 1.0f (original content preserved)
     * bordered.get(0, 0);             // returns 0.0f (new padding cell)
     *
     * matrix.extend(0, 0, 0, 0).equals(matrix); // returns true (no padding -> copy)
     * matrix.extend(-1, 0, 0, 0);               // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of rows to add above; must be {@code >= 0}
     * @param padBottom number of rows to add below; must be {@code >= 0}
     * @param padLeft number of columns to add to the left; must be {@code >= 0}
     * @param padRight number of columns to add to the right; must be {@code >= 0}
     * @return a new {@code FloatMatrix} with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         if the resulting dimensions would overflow {@code Integer.MAX_VALUE},
     *         or if the resulting shape is not representable (zero rows with a non-zero column count)
     * @see #extend(int, int, int, int, float)
     * @see #resize(int, int)
     */
    @Override
    public FloatMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight) {
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
     * <p><b>Unlike {@link #resize(int, int, float)}, this method never truncates existing content.</b>
     * All elements of the original matrix appear unchanged in the result.</p>
     *
     * <p><b>Typical uses:</b> zero-padding before convolution, adding sentinel borders, or creating
     * asymmetric margins (e.g. more padding on one side than another).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}});
     *
     * // Asymmetric padding: 2 columns on the left, 1 on the right
     * FloatMatrix padded = matrix.extend(1, 1, 2, 1, 9.0f);
     * // Result: [[9.0, 9.0, 9.0, 9.0, 9.0],
     * //          [9.0, 9.0, 1.0, 2.0, 9.0],
     * //          [9.0, 9.0, 9.0, 9.0, 9.0]]
     *
     * // Uniform border of 1 cell on every side
     * FloatMatrix bordered = matrix.extend(1, 1, 1, 1, 0.0f);
     * // Result: [[0.0, 0.0, 0.0, 0.0],
     * //          [0.0, 1.0, 2.0, 0.0],
     * //          [0.0, 0.0, 0.0, 0.0]]
     *
     * padded.get(1, 2);                               // returns 1.0f (original content preserved)
     * padded.get(0, 0);                               // returns 9.0f (padding uses defaultValue)
     * matrix.extend(1, 0, 0, 0, Float.NaN).get(0, 0); // returns NaN (verify with Float.isNaN)
     * matrix.extend(0, -1, 0, 0, 9.0f);               // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of rows to add above; must be {@code >= 0}
     * @param padBottom number of rows to add below; must be {@code >= 0}
     * @param padLeft number of columns to add to the left; must be {@code >= 0}
     * @param padRight number of columns to add to the right; must be {@code >= 0}
     * @param defaultValue the float value used to fill all newly added cells (may be {@code NaN},
     *                     {@code +/-Infinity}, or {@code -0.0f})
     * @return a new {@code FloatMatrix} with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         if the resulting dimensions would overflow {@code Integer.MAX_VALUE},
     *         or if the resulting shape is not representable (zero rows with a non-zero column count)
     * @see #extend(int, int, int, int)
     * @see #resize(int, int, float)
     */
    public FloatMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final float defaultValue)
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
            final boolean fillDefaultValue = Float.floatToRawIntBits(defaultValue) != 0;
            final float[][] b = new float[newRowCount][newColumnCount];

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

            return new FloatMatrix(b);
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}});
     * matrix.flipHorizontallyInPlace();   // row becomes [3.0f, 2.0f, 1.0f]
     * matrix.get(0, 0);                   // returns 3.0f
     * matrix.get(0, 2);                   // returns 1.0f
     *
     * FloatMatrix single = FloatMatrix.of(new float[][] {{5.0f}});
     * single.flipHorizontallyInPlace();
     * single.get(0, 0);                              // returns 5.0f (single column, unchanged)
     * FloatMatrix.empty().flipHorizontallyInPlace(); // no-op on empty matrix (no exception)
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f}, {2.0f}, {3.0f}});
     * matrix.flipVerticallyInPlace();   // rows reversed: [[3.0f], [2.0f], [1.0f]]
     * matrix.get(0, 0);                 // returns 3.0f
     * matrix.get(2, 0);                 // returns 1.0f
     *
     * FloatMatrix single = FloatMatrix.of(new float[][] {{5.0f}});
     * single.flipVerticallyInPlace();
     * single.get(0, 0);                            // returns 5.0f (single row, unchanged)
     * FloatMatrix.empty().flipVerticallyInPlace(); // no-op on empty matrix (no exception)
     * }</pre>
     *
     * @see #flipVertically()
     * @see #flipHorizontallyInPlace()
     */
    @Override
    public void flipVerticallyInPlace() {
        for (int l = 0, h = rowCount - 1; l < h; l++, h--) {
            final float[] tmp = a[l];
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * FloatMatrix flipped = matrix.flipHorizontally();  // [[3.0f, 2.0f, 1.0f], [6.0f, 5.0f, 4.0f]]
     * flipped.get(0, 0);                                // returns 3.0f
     * flipped.get(1, 2);                                // returns 4.0f
     * matrix.get(0, 0);                                 // returns 1.0f (original unchanged)
     * FloatMatrix.empty().flipHorizontally().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new {@code FloatMatrix} with each row reversed
     * @see #flipHorizontallyInPlace()
     * @see #flipVertically()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public FloatMatrix flipHorizontally() {
        final FloatMatrix res = this.copy();
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}, {5.0f, 6.0f}});
     * FloatMatrix flipped = matrix.flipVertically();  // [[5.0f, 6.0f], [3.0f, 4.0f], [1.0f, 2.0f]]
     * flipped.get(0, 0);                              // returns 5.0f
     * flipped.get(2, 1);                              // returns 2.0f
     * matrix.get(0, 0);                               // returns 1.0f (original unchanged)
     * FloatMatrix.empty().flipVertically().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new {@code FloatMatrix} with rows reversed
     * @see #flipVerticallyInPlace()
     * @see #flipHorizontally()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public FloatMatrix flipVertically() {
        final FloatMatrix res = this.copy();
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix rotated = matrix.rotate90();   // {{3.0, 1.0}, {4.0, 2.0}}
     * rotated.get(0, 0);                         // returns 3.0f
     * rotated.get(1, 1);                         // returns 2.0f
     *
     * // Non-square: 2x3 rotates to 3x2
     * FloatMatrix wide = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * wide.rotate90().rowCount();               // returns 3
     * wide.rotate90().get(0, 0);                // returns 4.0f
     * FloatMatrix.empty().rotate90().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix rotated 90 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         or an empty matrix if this matrix has zero columns
     * @see #rotate180()
     * @see #rotate270()
     * @see #transpose()
     */
    @Override
    public FloatMatrix rotate90() {
        if (columnCount == 0) {
            return EMPTY_FLOAT_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final float[][] c = new float[columnCount][rowCount];

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

        return new FloatMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 180 degrees.
     * This is equivalent to flipping both horizontally and vertically, reversing the
     * order of all elements. The resulting matrix has the same dimensions as the original.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix rotated = matrix.rotate180();   // {{4.0, 3.0}, {2.0, 1.0}}
     * rotated.get(0, 0);                          // returns 4.0f
     * rotated.get(1, 1);                          // returns 1.0f
     *
     * FloatMatrix wide = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}});
     * wide.rotate180().get(0, 0);                // returns 3.0f
     * FloatMatrix.empty().rotate180().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 180 degrees
     * @see #rotate90()
     * @see #rotate270()
     */
    @Override
    public FloatMatrix rotate180() {
        final float[][] c = new float[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[rowCount - i - 1].clone();
            N.reverse(c[i]);
        }

        return new FloatMatrix(c);
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix rotated = matrix.rotate270();   // {{2.0, 4.0}, {1.0, 3.0}}
     * rotated.get(0, 0);                          // returns 2.0f
     * rotated.get(1, 1);                          // returns 3.0f
     *
     * // Non-square: 2x3 rotates to 3x2
     * FloatMatrix wide = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * wide.rotate270().rowCount();               // returns 3
     * wide.rotate270().get(0, 0);                // returns 3.0f
     * FloatMatrix.empty().rotate270().isEmpty(); // returns true
     * }</pre>
     *
     * @return a new matrix rotated 270 degrees clockwise (dimensions {@code columnCount × rowCount}),
     *         or an empty matrix if this matrix has zero columns
     * @see #rotate90()
     * @see #rotate180()
     * @see #transpose()
     */
    @Override
    public FloatMatrix rotate270() {
        if (columnCount == 0) {
            return EMPTY_FLOAT_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final float[][] c = new float[columnCount][rowCount];

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

        return new FloatMatrix(c);
    }

    /**
     * Creates the transpose of this matrix by swapping rows and columns.
     * The transpose operation converts each row into a column, so the element at position {@code (i, j)}
     * in the original matrix appears at position {@code (j, i)} in the transposed matrix. The resulting
     * matrix has dimensions swapped ({@code rowCount × columnCount} becomes {@code columnCount × rowCount}).
     * Creates a new matrix; the original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:      Transposed:
     * // 1.0f 2.0f 3.0f  1.0f 4.0f
     * // 4.0f 5.0f 6.0f  2.0f 5.0f
     * //                 3.0f 6.0f
     *
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * FloatMatrix transposed = matrix.transpose(); // 2×3 becomes 3×2
     * transposed.rowCount();                       // returns 3
     * transposed.columnCount();                    // returns 2
     * transposed.get(0, 1);                        // returns 4.0f (was at (1,0))
     * transposed.get(2, 0);                        // returns 3.0f (was at (0,2))
     * FloatMatrix.empty().transpose().isEmpty();   // returns true
     * }</pre>
     *
     * @return a new {@code FloatMatrix} that is the transpose of this matrix with dimensions {@code columnCount × rowCount};
     *         an {@code N x 0} matrix transposes to the empty {@code 0 x 0} matrix, because the swapped shape
     *         {@code 0 x N} (zero rows with a non-zero column count) is not representable
     */
    @Override
    public FloatMatrix transpose() {
        if (columnCount == 0) {
            return EMPTY_FLOAT_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final float[][] c = new float[columnCount][rowCount];

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

        return new FloatMatrix(c);
    }

    /**
     * Reshapes this matrix to have the specified dimensions.
     * Elements are taken in row-major order from this matrix and placed into the new shape.
     * The new shape must have at least as many total cells as the original
     * ({@code (long) newRowCount * newColumnCount >= elementCount()}).
     * Any extra trailing cells in the new shape are filled with {@code 0.0f}.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * FloatMatrix reshaped = matrix.reshape(3, 2);   // [[1.0f, 2.0f], [3.0f, 4.0f], [5.0f, 6.0f]]
     * reshaped.get(1, 0);                            // returns 3.0f
     * reshaped.get(2, 1);                            // returns 6.0f
     *
     * FloatMatrix extended = matrix.reshape(2, 4);   // [[1,2,3,4], [5,6,0,0]] (extra cells are 0.0f)
     * extended.get(1, 2);                            // returns 0.0f
     * matrix.reshape(2, 2);                          // throws IllegalArgumentException (shape too small for 6 elements)
     * matrix.reshape(-1, 6);                         // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix; must be {@code >= 0}
     * @param newColumnCount the number of columns in the reshaped matrix; must be {@code >= 0}
     * @return a new {@code FloatMatrix} with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative, if the resulting shape is not
     *         representable (zero rows with a non-zero column count), if the total cell count {@code (long) newRowCount * newColumnCount}
     *         exceeds {@code Integer.MAX_VALUE}, or if the new shape is too small to hold every existing element
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public FloatMatrix reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        checkMaterializableShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final float[][] c = new float[newRowCount][newColumnCount];

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return new FloatMatrix(c);
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

        return new FloatMatrix(c);
    }

    /**
     * Repeats elements in both row and column directions.
     * Each element is repeated to form a block of size {@code rowRepeats × columnRepeats}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}});
     * FloatMatrix repeated = matrix.repeatElements(2, 3);
     * // Result: [[1.0, 1.0, 1.0, 2.0, 2.0, 2.0],
     * //          [1.0, 1.0, 1.0, 2.0, 2.0, 2.0]]
     * repeated.rowCount();               // returns 2
     * repeated.columnCount();            // returns 6
     * repeated.get(1, 3);                // returns 2.0f
     *
     * matrix.repeatElements(1, 1).equals(matrix); // returns true (no change)
     * matrix.repeatElements(0, 2);                // throws IllegalArgumentException (repeats must be positive)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat each element in row direction; must be {@code > 0}
     * @param columnRepeats number of times to repeat each element in column direction; must be {@code > 0}
     * @return a new {@code FloatMatrix} of dimensions {@code (rowCount*rowRepeats) x (columnCount*columnRepeats)} with repeated elements
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repelem.html">MATLAB repelem function</a>
     */
    @Override
    public FloatMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final float[][] c = new float[rowCount * rowRepeats][columnCount * columnRepeats];

        for (int i = 0; i < rowCount; i++) {
            final float[] sourceRow = a[i];
            final float[] firstRepeatedRow = c[i * rowRepeats];

            for (int j = 0; j < columnCount; j++) {
                N.copy(Array.repeat(sourceRow[j], columnRepeats), 0, firstRepeatedRow, j * columnRepeats, columnRepeats);
            }

            for (int k = 1; k < rowRepeats; k++) {
                N.copy(firstRepeatedRow, 0, c[i * rowRepeats + k], 0, firstRepeatedRow.length);
            }
        }

        return new FloatMatrix(c);
    }

    /**
     * Repeats the entire matrix in a tiled pattern.
     * The matrix is repeated as a whole {@code rowRepeats} times vertically and {@code columnRepeats} times horizontally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix repeated = matrix.repeatMatrix(2, 3);
     * // Result: [[1.0, 2.0, 1.0, 2.0, 1.0, 2.0],
     * //          [3.0, 4.0, 3.0, 4.0, 3.0, 4.0],
     * //          [1.0, 2.0, 1.0, 2.0, 1.0, 2.0],
     * //          [3.0, 4.0, 3.0, 4.0, 3.0, 4.0]]
     * repeated.rowCount();               // returns 4
     * repeated.columnCount();            // returns 6
     * repeated.get(2, 3);                // returns 2.0f (tiled copy)
     *
     * matrix.repeatMatrix(1, 1).equals(matrix); // returns true (single tile)
     * matrix.repeatMatrix(2, 0);                // throws IllegalArgumentException (repeats must be positive)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat the matrix vertically; must be {@code > 0}
     * @param columnRepeats number of times to repeat the matrix horizontally; must be {@code > 0}
     * @return a new {@code FloatMatrix} of dimensions {@code (rowCount*rowRepeats) x (columnCount*columnRepeats)} with the tiled pattern
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repmat.html">MATLAB repmat function</a>
     */
    @Override
    public FloatMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final float[][] c = new float[rowCount * rowRepeats][columnCount * columnRepeats];

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

        return new FloatMatrix(c);
    }

    /**
     * Returns a new {@link FloatList} containing all elements of this matrix in row-major order.
     * The returned list owns its data; modifications to it do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatList list = matrix.flatten();   // FloatList [1.0f, 2.0f, 3.0f, 4.0f]
     * list.size();                         // returns 4
     * list.get(2);                         // returns 3.0f
     *
     * FloatMatrix.empty().flatten().size();                    // returns 0
     * FloatMatrix.of(new float[][] {{5.0f}}).flatten().get(0); // returns 5.0f
     * }</pre>
     *
     * @return a new {@link FloatList} of all elements in row-major order
     * @throws IllegalStateException if {@code (long) rowCount * columnCount > Integer.MAX_VALUE}
     * @see #horizontalStream()
     */
    @Override
    public FloatList flatten() {
        // Check for overflow before allocation
        if ((long) rowCount * columnCount > Integer.MAX_VALUE) {
            throw new IllegalStateException("Matrix too large to flatten: " + rowCount + " x " + columnCount);
        }

        final float[] flattenedArray = new float[rowCount * columnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, flattenedArray, i * columnCount, columnCount);
        }

        return FloatList.of(flattenedArray);
    }

    /**
     * Flattens all elements of this matrix into a single one-dimensional array, applies the given
     * operation to that flattened array, and then copies the (possibly modified) elements back
     * into the matrix in row-major order.
     *
     * <p>This enables operations that need a global view of all matrix elements (e.g., sorting all
     * elements across the entire matrix). The action receives a flattened view of the matrix
     * elements; after the action completes, the (possibly mutated) values are written back into
     * the matrix in row-major order. When sorting with {@link java.util.Arrays#sort(float[])},
     * note that {@code NaN} is ordered greater than all other values (including {@code +Infinity})
     * and {@code -0.0f} is ordered less than {@code +0.0f}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{5.0f, 3.0f}, {4.0f, 1.0f}});
     * matrix.mutateAsFlat(arr -> java.util.Arrays.sort(arr)); // global sort, then row-major write-back
     * matrix.get(0, 0);                                       // returns 1.0f
     * matrix.get(1, 1);                                       // returns 5.0f
     *
     * FloatMatrix counts = FloatMatrix.of(new float[][] {{0.0f, 0.0f}});
     * counts.mutateAsFlat(arr -> { for (int k = 0; k < arr.length; k++) arr[k] = k; });
     * counts.get(0, 1);                                                    // returns 1.0f
     * FloatMatrix.empty().mutateAsFlat(arr -> java.util.Arrays.sort(arr)); // no-op on empty matrix (no exception)
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the operation to apply to the flattened array
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the operation throws an exception
     * @see Arrays#mutateAsFlat(float[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super float[], E> action) throws E {
        N.checkArgNotNull(action, "action");

        Arrays.mutateAsFlat(a, action);
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
     * FloatMatrix a = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix b = FloatMatrix.of(new float[][] {{5.0f, 6.0f}, {7.0f, 8.0f}});
     * FloatMatrix stacked = a.stackVertically(b);
     * // Result: [[1.0, 2.0], [3.0, 4.0], [5.0, 6.0], [7.0, 8.0]]
     * stacked.rowCount();                // returns 4
     * stacked.get(2, 0);                 // returns 5.0f
     *
     * FloatMatrix c = FloatMatrix.of(new float[][] {{9.0f, 8.0f, 7.0f}});
     * a.stackVertically(c);                  // throws IllegalArgumentException (column count mismatch: 2 vs 3)
     * a.stackVertically((FloatMatrix) null); // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * @param other the matrix to stack below this matrix (must have the same column count)
     * @return a new FloatMatrix with dimensions (this.rowCount + other.rowCount) x this.columnCount
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.columnCount != other.columnCount},
     *         or if the merged row count would exceed {@code Integer.MAX_VALUE}
     * @see #stackHorizontally(FloatMatrix)
     */
    @Override
    public FloatMatrix stackVertically(final FloatMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.columnCount, MSG_VSTACK_COLUMN_MISMATCH, columnCount, other.columnCount);
        final long mergedRowCount = (long) rowCount + other.rowCount;
        N.checkArgument(mergedRowCount <= Integer.MAX_VALUE, "Merged row count overflow: {} + {} = {}", rowCount, other.rowCount, mergedRowCount);

        final float[][] c = new float[(int) mergedRowCount][];
        int j = 0;

        for (int i = 0; i < rowCount; i++) {
            c[j++] = a[i].clone();
        }

        for (int i = 0; i < other.rowCount; i++) {
            c[j++] = other.a[i].clone();
        }

        return FloatMatrix.of(c);
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
     * FloatMatrix a = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix b = FloatMatrix.of(new float[][] {{5.0f, 6.0f}, {7.0f, 8.0f}});
     * FloatMatrix stacked = a.stackHorizontally(b);
     * // Result: [[1.0, 2.0, 5.0, 6.0], [3.0, 4.0, 7.0, 8.0]]
     * stacked.columnCount();             // returns 4
     * stacked.get(0, 2);                 // returns 5.0f
     *
     * FloatMatrix c = FloatMatrix.of(new float[][] {{9.0f, 8.0f}});
     * a.stackHorizontally(c);                  // throws IllegalArgumentException (row count mismatch: 2 vs 1)
     * a.stackHorizontally((FloatMatrix) null); // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * @param other the matrix to stack to the right of this matrix (must have the same row count)
     * @return a new FloatMatrix with dimensions this.rowCount x (this.columnCount + other.columnCount)
     * @throws IllegalArgumentException if {@code other} is {@code null}, if {@code this.rowCount != other.rowCount},
     *         or if the merged column count would exceed {@code Integer.MAX_VALUE}
     * @see #stackVertically(FloatMatrix)
     */
    @Override
    public FloatMatrix stackHorizontally(final FloatMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(rowCount == other.rowCount, MSG_HSTACK_ROW_MISMATCH, rowCount, other.rowCount);
        final long mergedColumnCount = (long) columnCount + other.columnCount;
        N.checkArgument(mergedColumnCount <= Integer.MAX_VALUE, "Merged column count overflow: {} + {} = {}", columnCount, other.columnCount,
                mergedColumnCount);

        final float[][] c = new float[rowCount][(int) mergedColumnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c[i], 0, columnCount);
            N.copy(other.a[i], 0, c[i], columnCount, other.columnCount);
        }

        return FloatMatrix.of(c);
    }

    /**
     * Performs element-wise addition of this matrix with another matrix.
     * The matrices must have the same dimensions (same number of rows and columns).
     *
     * <p>For large matrices (8192+ elements), this operation may be parallelized automatically
     * for better performance.</p>
     *
     * <p><b>Floating-point notes:</b> Adding {@code +Infinity} and {@code -Infinity} produces
     * {@code NaN}. If either operand is {@code NaN}, the result at that position is {@code NaN}.
     * No exception is thrown for these cases.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix a = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix b = FloatMatrix.of(new float[][] {{5.0f, 6.0f}, {7.0f, 8.0f}});
     * FloatMatrix sum = a.add(b);        // [[6.0, 8.0], [10.0, 12.0]]
     * sum.get(1, 1);                     // returns 12.0f
     *
     * // +Infinity + -Infinity yields NaN
     * FloatMatrix p = FloatMatrix.of(new float[][] {{Float.POSITIVE_INFINITY}});
     * FloatMatrix n = FloatMatrix.of(new float[][] {{Float.NEGATIVE_INFINITY}});
     * Float.isNaN(p.add(n).get(0, 0));                           // returns true
     * a.add(FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}})); // throws IllegalArgumentException (shape mismatch)
     * a.add((FloatMatrix) null);                                 // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * @param other the matrix to add to this matrix; must not be {@code null}
     * @return a new {@code FloatMatrix} containing the element-wise sum (same dimensions as the inputs)
     * @throws IllegalArgumentException if {@code other} is {@code null} or the matrices have different dimensions
     * @see #subtract(FloatMatrix)
     * @see #zipWith(FloatMatrix, Throwables.FloatBinaryOperator)
     */
    public FloatMatrix add(final FloatMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot add matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final float[][] otherMatrix = other.a;
        final float[][] result = new float[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = a[i][j] + otherMatrix[i][j];

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return FloatMatrix.of(result);
    }

    /**
     * Performs element-wise subtraction of another matrix from this matrix.
     * The matrices must have the same dimensions (same number of rows and columns).
     *
     * <p>For large matrices (8192+ elements), this operation may be parallelized automatically
     * for better performance.</p>
     *
     * <p><b>Floating-point notes:</b> Subtracting {@code +Infinity} from {@code +Infinity}
     * (or {@code -Infinity} from {@code -Infinity}) produces {@code NaN}. If either operand is
     * {@code NaN}, the result at that position is {@code NaN}. No exception is thrown for these cases.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix a = FloatMatrix.of(new float[][] {{5.0f, 6.0f}, {7.0f, 8.0f}});
     * FloatMatrix b = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix diff = a.subtract(b);  // [[4.0, 4.0], [4.0, 4.0]]
     * diff.get(0, 0);                    // returns 4.0f
     *
     * // +Infinity - +Infinity yields NaN
     * FloatMatrix p = FloatMatrix.of(new float[][] {{Float.POSITIVE_INFINITY}});
     * Float.isNaN(p.subtract(p).get(0, 0));               // returns true
     * a.subtract(FloatMatrix.of(new float[][] {{1.0f}})); // throws IllegalArgumentException (shape mismatch)
     * a.subtract((FloatMatrix) null);                     // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * @param other the matrix to subtract from this matrix; must not be {@code null}
     * @return a new {@code FloatMatrix} containing the element-wise difference (same dimensions as the inputs)
     * @throws IllegalArgumentException if {@code other} is {@code null} or the matrices have different dimensions
     * @see #add(FloatMatrix)
     */
    public FloatMatrix subtract(final FloatMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot subtract matrices with different shapes: this is {}x{} but other is {}x{}", rowCount,
                columnCount, other.rowCount, other.columnCount);

        final float[][] otherMatrix = other.a;
        final float[][] result = new float[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = a[i][j] - otherMatrix[i][j];

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return FloatMatrix.of(result);
    }

    /**
     * Performs matrix multiplication of this matrix with another matrix.
     * The number of columns in this matrix must equal the number of rows in the other matrix.
     * The resulting matrix has dimensions (this.rowCount × other.columnCount).
     *
     * <p>This operation uses standard matrix multiplication where each element (i,j) in the result
     * is computed as the dot product of row i from this matrix and column j from the other matrix.
     * Since float has limited precision (~7 decimal digits), accumulated rounding errors may occur
     * for large matrices. Consider using {@link #toDoubleMatrix()} for higher precision if needed.</p>
     *
     * <p>For large matrices, this operation may be parallelized automatically for better performance.</p>
     *
     * <p><b>Note:</b> This is the linear-algebra matrix product, not element-wise multiplication.
     * For element-wise multiplication use
     * {@link #zipWith(FloatMatrix, com.landawn.abacus.util.Throwables.FloatBinaryOperator)}.</p>
     *
     * <p><b>Floating-point notes:</b> Standard IEEE-754 arithmetic applies; {@code NaN} or
     * {@code Infinity} operands propagate into the corresponding result cells, and intermediate
     * sums of {@code +Infinity} and {@code -Infinity} produce {@code NaN}. No exception is
     * thrown for these cases.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix a = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix b = FloatMatrix.of(new float[][] {{5.0f, 6.0f}, {7.0f, 8.0f}});
     * FloatMatrix product = a.matmul(b); // [[19.0, 22.0], [43.0, 50.0]]
     * product.get(0, 0);                 // returns 19.0f
     * product.get(1, 1);                 // returns 50.0f
     *
     * // 2x3 times 3x2 yields a 2x2 product
     * FloatMatrix m = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * FloatMatrix n = FloatMatrix.of(new float[][] {{1.0f, 0.0f}, {0.0f, 1.0f}, {1.0f, 1.0f}});
     * m.matmul(n).get(0, 0);                                        // returns 4.0f
     * a.matmul(FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}})); // throws IllegalArgumentException (columnCount != other.rowCount)
     * a.matmul((FloatMatrix) null);                                 // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * @param other the matrix to multiply with this matrix; must not be {@code null}
     * @return a new {@code FloatMatrix} containing the matrix product with dimensions {@code this.rowCount × other.columnCount}
     * @throws IllegalArgumentException if {@code other} is {@code null}, if the matrix dimensions are incompatible for multiplication
     *         (i.e., {@code this.columnCount != other.rowCount}), or if this matrix has zero rows while {@code other} has a
     *         non-zero column count (the resulting shape is not representable)
     */
    public FloatMatrix matmul(final FloatMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.rowCount,
                "Matrix dimensions incompatible for multiplication: this is {}x{}, other is {}x{} (this.columnCount must equal other.rowCount)", rowCount,
                columnCount, other.rowCount, other.columnCount);

        checkRepresentableShape(rowCount, other.columnCount);

        final float[][] otherMatrix = other.a;
        final float[][] result = new float[rowCount][other.columnCount];
        final Throwables.IntTriConsumer<RuntimeException> multiplyAction = (i, j, k) -> result[i][j] += a[i][k] * otherMatrix[k][j];

        Matrices.forEachCartesianIndices(this, other, multiplyAction);

        return FloatMatrix.of(result);
    }

    /**
     * Converts this primitive {@code FloatMatrix} to a boxed {@code Matrix<Float>}.
     * Each primitive {@code float} element is autoboxed to a non-{@code null} {@link Float}, so the
     * returned matrix has the same dimensions and values as this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix primitive = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * Matrix<Float> boxed = primitive.boxed();
     * boxed.get(0, 1);                   // returns Float 2.0f
     * boxed.rowCount();                  // returns 2
     *
     * // Round-trips with unbox
     * FloatMatrix.unbox(boxed).equals(primitive); // returns true
     * FloatMatrix.empty().boxed().isEmpty();      // returns true
     * }</pre>
     *
     * @return a new {@code Matrix<Float>} containing boxed {@code Float} values with the same shape as this matrix
     * @see #unbox(Matrix)
     */
    public Matrix<Float> boxed() {
        final Float[][] c = new Float[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final float[] sourceRow = a[i];
                final Float[] targetRow = c[i];

                for (int j = 0; j < columnCount; j++) {
                    targetRow[j] = sourceRow[j]; // NOSONAR
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
     * Converts this float matrix to a double matrix.
     * Each float value is widened to double precision without loss of information,
     * since every float value can be represented exactly as a double.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix floatMatrix = FloatMatrix.of(new float[][] {{1.5f, 2.5f}});
     * DoubleMatrix doubleMatrix = floatMatrix.toDoubleMatrix();
     * doubleMatrix.get(0, 0);            // returns 1.5 (exact widening)
     * doubleMatrix.get(0, 1);            // returns 2.5
     *
     * // NaN is preserved across widening
     * Double.isNaN(FloatMatrix.of(new float[][] {{Float.NaN}}).toDoubleMatrix().get(0, 0)); // returns true
     * FloatMatrix.empty().toDoubleMatrix().isEmpty();                                       // returns true
     * }</pre>
     *
     * @return a new {@code DoubleMatrix} with values widened from {@code float} to {@code double}, with the same shape as this matrix
     */
    public DoubleMatrix toDoubleMatrix() {
        return DoubleMatrix.from(a);
    }

    /**
     * Converts this float matrix to an int matrix.
     * Each float value is narrowed to int by Java's float-to-int casting rules:
     * the fractional part is discarded (truncation toward zero) and special
     * values are mapped as follows:
     *
     * <ul>
     *   <li>{@code NaN} becomes {@code 0}.</li>
     *   <li>{@code +Infinity} and values greater than {@code Integer.MAX_VALUE}
     *       saturate to {@code Integer.MAX_VALUE}.</li>
     *   <li>{@code -Infinity} and values less than {@code Integer.MIN_VALUE}
     *       saturate to {@code Integer.MIN_VALUE}.</li>
     * </ul>
     *
     * <p><b>Warning:</b> This is a narrowing conversion that may lose information.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix floatMatrix = FloatMatrix.of(new float[][] {{1.9f, 2.1f}, {3.5f, 4.0f}});
     * IntMatrix intMatrix = floatMatrix.toIntMatrix();   // truncation toward zero: [[1, 2], [3, 4]]
     * intMatrix.get(0, 0);                               // returns 1
     * intMatrix.get(1, 0);                               // returns 3
     *
     * // Special values saturate; NaN becomes 0
     * FloatMatrix special = FloatMatrix.of(new float[][] {{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}});
     * IntMatrix conv = special.toIntMatrix();
     * conv.get(0, 0);                    // returns 0 (NaN -> 0)
     * conv.get(0, 1);                    // returns Integer.MAX_VALUE
     * conv.get(0, 2);                    // returns Integer.MIN_VALUE
     * }</pre>
     *
     * @return a new {@code IntMatrix} with values converted from float to int
     */
    public IntMatrix toIntMatrix() {
        final int[][] c = new int[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final float[] aa = a[i];
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
     * Converts this float matrix to a long matrix.
     * Each float value is narrowed to long by Java's float-to-long casting rules:
     * the fractional part is discarded (truncation toward zero) and special
     * values are mapped as follows:
     *
     * <ul>
     *   <li>{@code NaN} becomes {@code 0L}.</li>
     *   <li>{@code +Infinity} and values greater than {@code Long.MAX_VALUE}
     *       saturate to {@code Long.MAX_VALUE}.</li>
     *   <li>{@code -Infinity} and values less than {@code Long.MIN_VALUE}
     *       saturate to {@code Long.MIN_VALUE}.</li>
     * </ul>
     *
     * <p><b>Warning:</b> This is a narrowing conversion that may lose information.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix floatMatrix = FloatMatrix.of(new float[][] {{1.9f, 2.1f}, {3.5f, 4.0f}});
     * LongMatrix longMatrix = floatMatrix.toLongMatrix();   // truncation toward zero: [[1, 2], [3, 4]]
     * longMatrix.get(0, 0);                                 // returns 1L
     * longMatrix.get(1, 0);                                 // returns 3L
     *
     * // Special values saturate; NaN becomes 0
     * FloatMatrix special = FloatMatrix.of(new float[][] {{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}});
     * LongMatrix conv = special.toLongMatrix();
     * conv.get(0, 0);                    // returns 0L (NaN -> 0L)
     * conv.get(0, 1);                    // returns Long.MAX_VALUE
     * conv.get(0, 2);                    // returns Long.MIN_VALUE
     * }</pre>
     *
     * @return a new {@code LongMatrix} with values converted from float to long
     */
    public LongMatrix toLongMatrix() {
        final long[][] c = new long[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final float[] aa = a[i];
                final long[] cc = c[i];

                for (int j = 0; j < columnCount; j++) {
                    cc[j] = (long) aa[j];
                }
            }
        } else {
            for (int j = 0; j < columnCount; j++) {
                for (int i = 0; i < rowCount; i++) {
                    c[i][j] = (long) a[i][j];
                }
            }
        }

        return new LongMatrix(c);
    }

    /**
     * Performs element-wise operation on two matrices using a binary operator.
     * The matrices must have the same dimensions. Corresponding elements from both matrices
     * are combined using the provided function to produce the result matrix.
     *
     * <p>This is a generalized element-wise operation. For the common element-wise operations of addition and
     * subtraction, consider using the dedicated methods {@link #add(FloatMatrix)} and {@link #subtract(FloatMatrix)};
     * for the linear-algebra matrix product (which is not an element-wise operation), use {@link #matmul(FloatMatrix)}.</p>
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
     * Creates a new matrix; the original matrices are not modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix1 = FloatMatrix.of(new float[][] {{1.0f, 2.0f}});
     * FloatMatrix matrix2 = FloatMatrix.of(new float[][] {{3.0f, 4.0f}});
     * FloatMatrix product = matrix1.zipWith(matrix2, (a, b) -> a * b); // element-wise multiply: [[3.0f, 8.0f]]
     * product.get(0, 1);                                               // returns 8.0f
     *
     * FloatMatrix maxes = matrix1.zipWith(matrix2, Math::max);
     * maxes.get(0, 0);                   // returns 3.0f
     * // Division by zero yields Infinity element-wise
     * FloatMatrix zeros = FloatMatrix.of(new float[][] {{0.0f, 0.0f}});
     * matrix1.zipWith(zeros, (a, b) -> a / b).get(0, 0) == Float.POSITIVE_INFINITY; // returns true
     * matrix1.zipWith(FloatMatrix.of(new float[][] {{1.0f}}), (a, b) -> a + b);     // throws IllegalArgumentException (shape mismatch)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param zipFunction the binary operator to apply to corresponding elements; receives the
     *                    element from this matrix as first argument and the element from
     *                    {@code other} as second argument
     * @return a new {@code FloatMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if {@code other} or {@code zipFunction} is {@code null},
     *         or if the matrices have different shapes
     * @throws E if the zip function throws an exception
     * @see #zipWith(FloatMatrix, FloatMatrix, Throwables.FloatTernaryOperator)
     */
    public <E extends Exception> FloatMatrix zipWith(final FloatMatrix other, final Throwables.FloatBinaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final float[][] secondMatrix = other.a;
        final float[][] result = new float[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsFloat(a[i][j], secondMatrix[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return FloatMatrix.of(result);
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
     * FloatMatrix matrix1 = FloatMatrix.of(new float[][] {{1.0f, 2.0f}});
     * FloatMatrix matrix2 = FloatMatrix.of(new float[][] {{3.0f, 4.0f}});
     * FloatMatrix matrix3 = FloatMatrix.of(new float[][] {{5.0f, 6.0f}});
     * FloatMatrix sum = matrix1.zipWith(matrix2, matrix3, (a, b, c) -> a + b + c);
     * sum.get(0, 0);                     // returns 9.0f
     * sum.get(0, 1);                     // returns 12.0f
     *
     * FloatMatrix fma = matrix1.zipWith(matrix2, matrix3, (a, b, c) -> a * b + c);
     * fma.get(0, 0);                                                                    // returns 8.0f (1*3 + 5)
     * matrix1.zipWith(matrix2, FloatMatrix.of(new float[][] {{1.0f}}), (a, b, c) -> a); // throws IllegalArgumentException (shape mismatch)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix (must have the same dimensions as this matrix)
     * @param third the third matrix (must have the same dimensions as this matrix)
     * @param zipFunction the ternary operator to apply to corresponding elements; receives the
     *                    element from this matrix as first argument, the element from
     *                    {@code other} as second argument, and the element from {@code third}
     *                    as third argument
     * @return a new {@code FloatMatrix} with the results of the element-wise operation
     * @throws IllegalArgumentException if any of {@code other}, {@code third}, or {@code zipFunction}
     *         is {@code null}, or if any of the matrices have different shapes
     * @throws E if the zip function throws an exception
     * @see #zipWith(FloatMatrix, Throwables.FloatBinaryOperator)
     */
    public <E extends Exception> FloatMatrix zipWith(final FloatMatrix other, final FloatMatrix third, final Throwables.FloatTernaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(third, "third");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other) && isSameShape(third), "Cannot zip matrices with different shapes: this is {}x{}, other is {}x{}, third is {}x{}",
                rowCount, columnCount, other.rowCount, other.columnCount, third.rowCount, third.columnCount);

        final float[][] secondMatrix = other.a;
        final float[][] thirdMatrix = third.a;
        final float[][] result = new float[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsFloat(a[i][j], secondMatrix[i][j], thirdMatrix[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return FloatMatrix.of(result);
    }

    /**
     * Returns a stream of elements on the main diagonal (upper-left to lower-right).
     * The matrix must be square.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f},
     *                                                     {4.0f, 5.0f, 6.0f},
     *                                                     {7.0f, 8.0f, 9.0f}});
     * matrix.mainDiagonalStream().toArray();   // returns [1.0f, 5.0f, 9.0f]
     * matrix.mainDiagonalStream().sum();       // returns 15.0
     *
     * FloatMatrix.empty().mainDiagonalStream().count();                        // returns 0 (empty stream)
     * FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}}).mainDiagonalStream(); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a FloatStream of main-diagonal elements, or an empty stream if the matrix is empty
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public FloatStream mainDiagonalStream() {
        checkIsSquare();

        if (isEmpty()) {
            return FloatStream.empty();
        }

        return FloatStream.of(new FloatIteratorEx() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() {
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f},
     *                                                     {4.0f, 5.0f, 6.0f},
     *                                                     {7.0f, 8.0f, 9.0f}});
     * matrix.antiDiagonalStream().toArray();   // returns [3.0f, 5.0f, 7.0f]
     * matrix.antiDiagonalStream().sum();       // returns 15.0
     *
     * FloatMatrix.empty().antiDiagonalStream().count();                        // returns 0 (empty stream)
     * FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}}).antiDiagonalStream(); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a FloatStream of anti-diagonal elements, or an empty stream if the matrix is empty
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public FloatStream antiDiagonalStream() {
        checkIsSquare();

        if (isEmpty()) {
            return FloatStream.empty();
        }

        return FloatStream.of(new FloatIteratorEx() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final float result = a[cursor][columnCount - cursor - 1];
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
     * Elements are streamed row by row from the top-left corner to the bottom-right corner.
     *
     * <p>This method is useful for processing all matrix elements sequentially
     * without concern for their row/column positions. The stream supports all
     * standard FloatStream operations including sum, average, filter, map, etc.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.horizontalStream().toArray();   // returns [1.0f, 2.0f, 3.0f, 4.0f]
     * matrix.horizontalStream().sum();       // returns 10.0
     *
     * FloatMatrix.empty().horizontalStream().count();                      // returns 0 (empty stream)
     * FloatMatrix.of(new float[][] {{5.0f}}).horizontalStream().toArray(); // returns [5.0f]
     * }</pre>
     *
     * @return a FloatStream of all elements in row-major order, or an empty stream if the matrix is empty
     */
    @Override
    public FloatStream horizontalStream() {
        return horizontalStream(0, rowCount);
    }

    /**
     * Returns a stream of elements from a single row.
     * The elements are streamed from left to right within the specified row.
     *
     * <p>This method is particularly useful when you need to process or analyze
     * a specific row of the matrix independently. The returned stream can be
     * used with all standard FloatStream operations.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * matrix.horizontalStream(0).toArray();   // returns [1.0f, 2.0f, 3.0f]
     * matrix.horizontalStream(1).sum();       // returns 15.0 (sum of second row)
     *
     * matrix.horizontalStream(-1);            // throws IndexOutOfBoundsException
     * matrix.horizontalStream(2);             // throws IndexOutOfBoundsException (rowIndex >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to stream (0-based)
     * @return a {@link FloatStream} of elements from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    @Override
    public FloatStream horizontalStream(final int rowIndex) {
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}, {5.0f, 6.0f}});
     * matrix.horizontalStream(1, 3).toArray();   // returns [3.0f, 4.0f, 5.0f, 6.0f]
     * matrix.horizontalStream(0, 1).toArray();   // returns [1.0f, 2.0f]
     *
     * matrix.horizontalStream(1, 1).count();     // returns 0 (empty range)
     * matrix.horizontalStream(0, 5);             // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a FloatStream of elements from the specified row range, or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public FloatStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        if (isEmpty()) {
            return FloatStream.empty();
        }

        return FloatStream.of(new FloatIteratorEx() {
            private int i = fromRowIndex;
            private int j = 0;

            @Override
            public boolean hasNext() {
                return i < toRowIndex;
            }

            @Override
            public float nextFloat() {
                if (i >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final float result = a[i][j++];

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
            public float[] toArray() {
                final int len = toArrayLength(count());
                final float[] c = new float[len];

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
     * Returns a stream of all elements in this matrix, traversed vertically (top to bottom, left to right).
     * Elements are streamed column by column from the top-left corner to the bottom-right corner.
     *
     * <p>This method provides an alternative way to iterate through matrix
     * elements compared to the row-major order of {@link #horizontalStream()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.verticalStream().toArray();   // returns [1.0f, 3.0f, 2.0f, 4.0f]
     * matrix.verticalStream().sum();       // returns 10.0
     *
     * FloatMatrix.empty().verticalStream().count();                      // returns 0 (empty stream)
     * FloatMatrix.of(new float[][] {{5.0f}}).verticalStream().toArray(); // returns [5.0f]
     * }</pre>
     *
     * @return a FloatStream of all elements in column-major order, or an empty stream if the matrix is empty
     */
    @Override
    @Beta
    public FloatStream verticalStream() {
        return verticalStream(0, columnCount);
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * matrix.verticalStream(1).toArray();   // returns [2.0f, 5.0f]
     * matrix.verticalStream(0).sum();       // returns 5.0 (sum of first column)
     *
     * matrix.verticalStream(-1);            // throws IndexOutOfBoundsException
     * matrix.verticalStream(3);             // throws IndexOutOfBoundsException (columnIndex >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to stream (0-based)
     * @return a {@link FloatStream} of elements from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    @Override
    public FloatStream verticalStream(final int columnIndex) {
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * matrix.verticalStream(1, 3).toArray();   // returns [2.0f, 5.0f, 3.0f, 6.0f]
     * matrix.verticalStream(0, 1).toArray();   // returns [1.0f, 4.0f]
     *
     * matrix.verticalStream(1, 1).count();     // returns 0 (empty range)
     * matrix.verticalStream(0, 5);             // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a FloatStream of elements from the specified column range in column-major order,
     *         or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    @Beta
    public FloatStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        if (isEmpty()) {
            return FloatStream.empty();
        }

        return FloatStream.of(new FloatIteratorEx() {
            private int i = 0;
            private int j = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return j < toColumnIndex;
            }

            @Override
            public float nextFloat() {
                if (j >= toColumnIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final float result = a[i++][j];

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

                if (n >= (long) (toColumnIndex - j) * FloatMatrix.this.rowCount - i) {
                    i = 0;
                    j = toColumnIndex;
                } else {
                    final long offset = n + i;
                    i = (int) (offset % FloatMatrix.this.rowCount);
                    j += (int) (offset / FloatMatrix.this.rowCount);
                }
            }

            @Override
            public long count() {
                return (long) (toColumnIndex - j) * rowCount - i; // NOSONAR
            }

            @Override
            public float[] toArray() {
                final int len = toArrayLength(count());
                final float[] c = new float[len];

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
     * Returns a stream of FloatStream objects, where each FloatStream represents a complete row.
     * This creates a stream of streams, allowing for row-by-row processing of the matrix.
     *
     * <p>This method is useful for operations that need to process entire rows as units,
     * such as row-wise transformations, filtering rows based on conditions, or mapping
     * rows to other values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.rowStreams().count();                                   // returns 2 (one stream per row)
     * matrix.rowStreams().map(FloatStream::sum).toList();            // returns [3.0, 7.0]
     * matrix.rowStreams().map(FloatStream::toArray).toList().get(0); // returns [1.0f, 2.0f]
     *
     * FloatMatrix.empty().rowStreams().count();     // returns 0 (no rows)
     * }</pre>
     *
     * @return a Stream of FloatStream objects, one for each row in the matrix
     */
    @Override
    public Stream<FloatStream> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Returns a stream of FloatStream objects for a range of rows.
     * Each FloatStream in the result represents a complete row within the specified range.
     *
     * <p>This method allows for processing a subset of rows while maintaining the
     * ability to work with complete rows as individual streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f}, {2.0f}, {3.0f}});
     * matrix.rowStreams(1, 3).count();                                   // returns 2 (rows 1 and 2)
     * matrix.rowStreams(1, 3).map(FloatStream::toArray).toList().get(0); // returns [2.0f]
     *
     * matrix.rowStreams(1, 1).count();              // returns 0 (empty range)
     * matrix.rowStreams(0, 5);                      // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of FloatStream objects for the specified row range
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public Stream<FloatStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public FloatStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return FloatStream.of(a[cursor++]);
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
     * Returns a stream of FloatStream objects, where each FloatStream represents a complete column.
     * This creates a stream of streams, allowing for column-by-column processing of the matrix.
     *
     * <p>This method is useful for operations that need to process
     * entire columns as units, such as column-wise statistics, transformations, or filtering
     * columns based on conditions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.columnStreams().count();                                   // returns 2 (one stream per column)
     * matrix.columnStreams().map(FloatStream::sum).toList();            // returns [4.0, 6.0]
     * matrix.columnStreams().map(FloatStream::toArray).toList().get(0); // returns [1.0f, 3.0f]
     *
     * FloatMatrix.empty().columnStreams().count();  // returns 0 (no columns)
     * }</pre>
     *
     * @return a Stream of FloatStream objects, one for each column in the matrix,
     *         or an empty stream if the matrix is empty
     */
    @Override
    @Beta
    public Stream<FloatStream> columnStreams() {
        return columnStreams(0, columnCount);
    }

    /**
     * Returns a stream of FloatStream objects for a range of columns.
     * Each FloatStream in the result represents a complete column within the specified range.
     *
     * <p>This method allows for processing a subset of columns
     * while maintaining the ability to work with complete columns as individual streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * matrix.columnStreams(1, 3).count();                                   // returns 2 (columns 1 and 2)
     * matrix.columnStreams(1, 3).map(FloatStream::toArray).toList().get(0); // returns [2.0f, 5.0f]
     *
     * matrix.columnStreams(1, 1).count();           // returns 0 (empty range)
     * matrix.columnStreams(0, 5);                   // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of FloatStream objects for the specified column range,
     *         or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    @Beta
    public Stream<FloatStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
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
            public FloatStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return FloatStream.of(new FloatIteratorEx() {
                    private final int columnIndex = cursor++;
                    private final int toIndex2 = rowCount;
                    private int cursor2 = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor2 < toIndex2;
                    }

                    @Override
                    public float nextFloat() {
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
    protected int length(@SuppressWarnings("hiding") final float[] a) {
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
     * or accumulating). For transformations that create new matrices, use {@link #map(Throwables.FloatUnaryOperator)}
     * or {@link #updateAll(Throwables.FloatUnaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * java.util.concurrent.atomic.DoubleAdder sum = new java.util.concurrent.atomic.DoubleAdder();
     * matrix.forEach(value -> sum.add(value));
     * sum.sum();                          // returns 10.0
     *
     * java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger();
     * matrix.forEach(value -> count.incrementAndGet());
     * count.get();                       // returns 4
     * FloatMatrix.empty().forEach(value -> count.incrementAndGet());
     * count.get();                       // still returns 4 (no elements)
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to perform on each element; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     * @see #forEach(int, int, int, int, Throwables.FloatConsumer)
     */
    public <E extends Exception> void forEach(final Throwables.FloatConsumer<E> action) throws E {
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f},
     *                                                     {4.0f, 5.0f, 6.0f},
     *                                                     {7.0f, 8.0f, 9.0f}});
     * float[] sum = {0.0f};
     * matrix.forEach(0, 2, 0, 2, value -> sum[0] += value);   // visits 1.0, 2.0, 4.0, 5.0
     * sum[0];                                                 // returns 12.0f
     *
     * int[] count = {0};
     * matrix.forEach(1, 3, 1, 3, value -> count[0]++);
     * count[0];                                // returns 4 (2x2 sub-region)
     * matrix.forEach(0, 5, 0, 2, value -> {}); // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @param action the action to perform on each element; must not be {@code null}
     * @throws IndexOutOfBoundsException if any index is out of bounds
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex,
            final Throwables.FloatConsumer<E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        if (Matrices.isParallelizable(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, elementAction, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                final float[] currentRow = a[i];

                for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                    action.accept(currentRow[j]);
                }
            }
        }
    }

    /**
     * Prints this matrix to standard output and returns the formatted string.
     * Each row is printed on a separate line with elements separated by commas
     * and enclosed in square brackets. A matrix with zero rows prints as {@code "[]"}.
     *
     * <p>Each float element is formatted by Java's default {@link Float#toString(float)},
     * which yields {@code "NaN"}, {@code "Infinity"}, {@code "-Infinity"}, or
     * {@code "-0.0"} for the corresponding special values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f, 3.0f}, {4.0f, 5.0f, 6.0f}});
     * matrix.println();             // returns "[1.0, 2.0, 3.0]\n[4.0, 5.0, 6.0]" (and prints it)
     *
     * FloatMatrix.empty().println();                                                  // returns "[]" (and prints it)
     * FloatMatrix.of(new float[][] {{Float.NaN, Float.POSITIVE_INFINITY}}).println(); // returns "[NaN, Infinity]"
     * }</pre>
     *
     * @return the formatted string representation of the matrix that was printed
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

                    final float[] row = a[i];
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
     * FloatMatrix matrix1 = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix matrix2 = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix1.hashCode() == matrix2.hashCode();   // returns true (equal content)
     *
     * FloatMatrix matrix3 = FloatMatrix.of(new float[][] {{9.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix1.hashCode() == matrix3.hashCode();                         // returns false (different content)
     * FloatMatrix.empty().hashCode() == FloatMatrix.empty().hashCode(); // returns true
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
     * Returns {@code true} if the given object is also a {@code FloatMatrix} with the same dimensions
     * (same {@code rowCount} and {@code columnCount}) and all corresponding elements are equal.
     *
     * <p>Element comparison uses {@link Float#floatToIntBits(float)} semantics
     * (the same rule used by {@link java.util.Arrays#equals(float[], float[])}):
     * {@code NaN} <em>is</em> considered equal to {@code NaN}, and {@code +0.0f} is
     * <em>not</em> considered equal to {@code -0.0f}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatMatrix m1 = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * FloatMatrix m2 = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * m1.equals(m2);                                                         // returns true
     * m1.equals(FloatMatrix.of(new float[][] {{9.0f, 2.0f}, {3.0f, 4.0f}})); // returns false (different content)
     *
     * // floatToIntBits semantics: NaN equals NaN, but +0.0f does NOT equal -0.0f
     * FloatMatrix.of(new float[][] {{Float.NaN}}).equals(FloatMatrix.of(new float[][] {{Float.NaN}})); // returns true
     * FloatMatrix.of(new float[][] {{0.0f}}).equals(FloatMatrix.of(new float[][] {{-0.0f}}));          // returns false
     * m1.equals(null);                                                                                 // returns false
     * m1.equals("not a matrix");                                                                       // returns false
     * }</pre>
     *
     * @param obj the object to compare with (may be {@code null})
     * @return {@code true} if {@code obj} is a {@code FloatMatrix} of the same shape and content, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final FloatMatrix another) {
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
     * FloatMatrix matrix = FloatMatrix.of(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.toString();            // returns "[[1.0, 2.0], [3.0, 4.0]]"
     *
     * FloatMatrix.empty().toString();                                                  // returns "[]"
     * FloatMatrix.of(new float[][] {{Float.NaN, Float.NEGATIVE_INFINITY}}).toString(); // returns "[[NaN, -Infinity]]"
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
