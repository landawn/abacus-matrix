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
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.DoubleIteratorEx;
import com.landawn.abacus.util.stream.DoubleStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * Matrix implementation backed by a rectangular {@code double[][]}.
 *
 * <p>This type specializes {@link AbstractMatrix} for {@code double} values while keeping the data in
 * a validated backing array. Constructors and {@link #of(double[]...)} generally wrap the supplied
 * storage directly, while factories, conversions, and mapping operations allocate new arrays.</p>
 *
 * <p>Cells introduced by growth or reshaping default to {@code 0.0d} unless an overload accepts an
 * explicit fill value.</p>
 *
 * <p><b>IEEE 754 semantics:</b> Arithmetic operations follow standard IEEE 754 rules. {@code NaN}
 * propagates through arithmetic, comparisons against {@code NaN} are always {@code false} (use
 * {@link Double#isNaN(double)} to test), and {@code +0.0} and {@code -0.0} compare equal under
 * {@code ==} but are distinguished by {@link Double#doubleToLongBits(double)}. {@link #equals(Object)}
 * and {@link #hashCode()} use {@code doubleToLongBits} semantics, so {@code NaN} equals {@code NaN}
 * and {@code +0.0} is not equal to {@code -0.0} for matrix-level comparison.</p>
 *
 * @see IntMatrix
 * @see LongMatrix
 * @see FloatMatrix
 * @see ShortMatrix
 * @see ByteMatrix
 * @see Matrix
 */
public final class DoubleMatrix extends AbstractMatrix<double[], DoubleList, DoubleStream, Stream<DoubleStream>, DoubleMatrix> {

    private static final DoubleMatrix EMPTY_DOUBLE_MATRIX = new DoubleMatrix(new double[0][0]);

    /**
     * Constructs a {@code DoubleMatrix} backed by the supplied two-dimensional array.
     *
     * <p>If {@code a} is {@code null}, this creates an empty {@code 0x0} matrix. Otherwise the array
     * is used directly after rectangular-shape validation, so later modifications to either the input
     * array or the matrix remain visible through the other view. Call {@link #copy()} if you need an
     * independently owned matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[][] data = {{1.0, 2.0}, {3.0, 4.0}};
     * DoubleMatrix matrix = new DoubleMatrix(data);
     * matrix.rowCount();           // returns 2
     * matrix.columnCount();        // returns 2
     * data[0][0] = 9.0;
     * matrix.get(0, 0);            // returns 9.0 (backing array is shared)
     *
     * DoubleMatrix empty = new DoubleMatrix((double[][]) null);
     * empty.rowCount();            // returns 0
     * empty.columnCount();         // returns 0
     *
     * new DoubleMatrix(new double[][] {{1.0, 2.0}, {3.0}}); // throws IllegalArgumentException (non-rectangular)
     * }</pre>
     *
     * @param a the two-dimensional double array to wrap, or {@code null} for an empty matrix
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public DoubleMatrix(final double[][] a) {
        super(a == null ? new double[0][0] : a, double.class);
    }

    /**
     * Creates an empty matrix with zero rows and zero columns.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.empty();
     * matrix.rowCount();           // returns 0
     * matrix.columnCount();        // returns 0
     * matrix.isEmpty();            // returns true
     * matrix.get(0, 0);            // throws ArrayIndexOutOfBoundsException (no elements)
     * }</pre>
     *
     * @return an empty double matrix
     */
    public static DoubleMatrix empty() {
        return EMPTY_DOUBLE_MATRIX;
    }

    /**
     * Creates a DoubleMatrix from a two-dimensional double array.
     *
     * <p><b>Important:</b> The provided array is used directly without defensive copying.
     * Changes to the input array are reflected in the returned matrix, and vice versa.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.get(1, 1);            // returns 4.0
     * matrix.rowCount();           // returns 2
     *
     * DoubleMatrix empty = DoubleMatrix.of((double[][]) null);
     * empty.isEmpty();             // returns true
     *
     * DoubleMatrix none = DoubleMatrix.of();
     * none.isEmpty();              // returns true (no rows supplied)
     *
     * DoubleMatrix.of(new double[][] {{1.0}, {2.0, 3.0}}); // throws IllegalArgumentException (non-rectangular)
     * }</pre>
     *
     * @param a the two-dimensional double array to create the matrix from, or {@code null}/empty for an empty matrix
     * @return a new {@code DoubleMatrix} containing the provided data, or an empty {@code DoubleMatrix} if the input is {@code null} or empty
     * @throws IllegalArgumentException if any row of {@code a} is {@code null} or if the rows have
     *         different lengths (i.e. the array is not rectangular)
     */
    public static DoubleMatrix of(final double[]... a) {
        return N.isEmpty(a) ? EMPTY_DOUBLE_MATRIX : new DoubleMatrix(a);
    }

    /**
     * Creates a DoubleMatrix from a two-dimensional int array by converting int values to double.
     *
     * <p>All rows must have the same length as the first row (rectangular array required).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.from(new int[][] {{1, 2}, {3, 4}});
     * matrix.get(0, 1);            // returns 2.0
     * matrix.get(1, 0);            // returns 3.0
     *
     * DoubleMatrix empty = DoubleMatrix.from((int[][]) null);
     * empty.isEmpty();             // returns true
     *
     * DoubleMatrix.from(new int[][] {{1, 2}, {3}}); // throws IllegalArgumentException (rows differ in length)
     * }</pre>
     *
     * @param a the two-dimensional int array to convert to a double matrix, or {@code null}/empty for an empty matrix
     * @return a new {@code DoubleMatrix} with converted values, or an empty {@code DoubleMatrix} if input is {@code null} or empty
     * @throws IllegalArgumentException if any row is {@code null} or if rows have different lengths (non-rectangular array)
     */
    public static DoubleMatrix from(final int[]... a) {
        if (N.isEmpty(a)) {
            return EMPTY_DOUBLE_MATRIX;
        }

        N.checkArgument(a[0] != null, "First row cannot be null");

        final int columnCount = a[0].length;

        // Validate all rows have the same length
        for (int i = 1; i < a.length; i++) {
            N.checkArgument(a[i] != null && a[i].length == columnCount, "All rows must have the same length. Row 0 has length {} but row {} has length {}",
                    columnCount, i, a[i] == null ? 0 : a[i].length);
        }

        final double[][] c = new double[a.length][columnCount];

        for (int i = 0, len = a.length; i < len; i++) {
            final int[] aa = a[i];
            final double[] cc = c[i];

            for (int j = 0; j < columnCount; j++) {
                cc[j] = aa[j]; // NOSONAR
            }
        }

        return new DoubleMatrix(c);
    }

    /**
     * Creates a DoubleMatrix from a two-dimensional long array by converting long values to double.
     *
     * <p>All rows must have the same length as the first row (rectangular array required).</p>
     *
     * <p><b>Note:</b> Long values with more than 53 significant bits may lose precision when
     * converted to double, since double has a 52-bit mantissa. For example,
     * {@code Long.MAX_VALUE} (9223372036854775807) cannot be represented exactly as a double.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.from(new long[][] {{1L, 2L}, {3L, 4L}});
     * matrix.get(1, 0);            // returns 3.0
     * matrix.get(0, 1);            // returns 2.0
     *
     * DoubleMatrix empty = DoubleMatrix.from((long[][]) null);
     * empty.isEmpty();             // returns true
     *
     * // Precision loss for large magnitudes (> 53 significant bits):
     * DoubleMatrix big = DoubleMatrix.from(new long[][] {{Long.MAX_VALUE}});
     * big.get(0, 0);               // returns 9.223372036854776E18 (not exact)
     *
     * DoubleMatrix.from(new long[][] {{1L}, {2L, 3L}}); // throws IllegalArgumentException (rows differ in length)
     * }</pre>
     *
     * @param a the two-dimensional long array to convert to a double matrix, or {@code null}/empty for an empty matrix
     * @return a new {@code DoubleMatrix} with converted values, or an empty {@code DoubleMatrix} if input is {@code null} or empty
     * @throws IllegalArgumentException if any row is {@code null} or if rows have different lengths (non-rectangular array)
     */
    public static DoubleMatrix from(final long[]... a) {
        if (N.isEmpty(a)) {
            return EMPTY_DOUBLE_MATRIX;
        }

        N.checkArgument(a[0] != null, "First row cannot be null");

        final int columnCount = a[0].length;

        // Validate all rows have the same length
        for (int i = 1; i < a.length; i++) {
            N.checkArgument(a[i] != null && a[i].length == columnCount, "All rows must have the same length. Row 0 has length {} but row {} has length {}",
                    columnCount, i, a[i] == null ? 0 : a[i].length);
        }

        final double[][] c = new double[a.length][columnCount];

        for (int i = 0, len = a.length; i < len; i++) {
            final long[] aa = a[i];
            final double[] cc = c[i];

            for (int j = 0; j < columnCount; j++) {
                cc[j] = aa[j]; // NOSONAR
            }
        }

        return new DoubleMatrix(c);
    }

    /**
     * Creates a DoubleMatrix from a two-dimensional float array by widening float values to double.
     * The widening conversion is exact for finite values, {@code NaN}, and {@code +/-Infinity};
     * no precision is lost since every {@code float} is representable as a {@code double}.
     *
     * <p>All rows must have the same length as the first row (rectangular array required).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.from(new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
     * matrix.get(1, 1);            // returns 4.0
     * matrix.get(0, 0);            // returns 1.0
     *
     * DoubleMatrix empty = DoubleMatrix.from((float[][]) null);
     * empty.isEmpty();             // returns true
     *
     * // Special float values widen exactly:
     * DoubleMatrix special = DoubleMatrix.from(new float[][] {{Float.NaN, Float.POSITIVE_INFINITY}});
     * Double.isNaN(special.get(0, 0));                         // returns true
     * special.get(0, 1) == Double.POSITIVE_INFINITY;           // returns true
     *
     * DoubleMatrix.from(new float[][] {{1.0f}, {2.0f, 3.0f}}); // throws IllegalArgumentException (rows differ in length)
     * }</pre>
     *
     * @param a the two-dimensional float array to convert to a double matrix, or {@code null}/empty for an empty matrix
     * @return a new {@code DoubleMatrix} with converted values, or an empty {@code DoubleMatrix} if input is {@code null} or empty
     * @throws IllegalArgumentException if any row is {@code null} or if rows have different lengths (non-rectangular array)
     */
    public static DoubleMatrix from(final float[]... a) {
        if (N.isEmpty(a)) {
            return EMPTY_DOUBLE_MATRIX;
        }

        N.checkArgument(a[0] != null, "First row cannot be null");

        final int columnCount = a[0].length;

        // Validate all rows have the same length
        for (int i = 1; i < a.length; i++) {
            N.checkArgument(a[i] != null && a[i].length == columnCount, "All rows must have the same length. Row 0 has length {} but row {} has length {}",
                    columnCount, i, a[i] == null ? 0 : a[i].length);
        }

        final double[][] c = new double[a.length][columnCount];

        for (int i = 0, len = a.length; i < len; i++) {
            final float[] aa = a[i];
            final double[] cc = c[i];

            for (int j = 0; j < columnCount; j++) {
                cc[j] = aa[j]; // NOSONAR
            }
        }

        return new DoubleMatrix(c);
    }

    /**
     * Creates a new {@code 1 x length} matrix filled with random double values uniformly distributed
     * in {@code [0.0, 1.0)} (as produced by {@link java.util.Random#nextDouble()}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.random(5);
     * matrix.rowCount();                                 // returns 1
     * matrix.columnCount();                              // returns 5
     * matrix.get(0, 0) >= 0.0 && matrix.get(0, 0) < 1.0; // returns true
     *
     * DoubleMatrix none = DoubleMatrix.random(0);
     * none.columnCount();          // returns 0
     *
     * DoubleMatrix.random(-1);     // throws IllegalArgumentException (negative length)
     * }</pre>
     *
     * @param length the number of columns in the new matrix
     * @return a new DoubleMatrix of dimensions {@code 1 x length} filled with random values
     * @throws IllegalArgumentException if {@code length} is negative
     */
    public static DoubleMatrix random(final int length) {
        return random(1, length);
    }

    /**
     * Creates a new matrix of the specified dimensions filled with random double values uniformly
     * distributed in {@code [0.0, 1.0)} (as produced by {@link java.util.Random#nextDouble()}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.random(2, 3);
     * matrix.rowCount();                                 // returns 2
     * matrix.columnCount();                              // returns 3
     * matrix.get(0, 0) >= 0.0 && matrix.get(0, 0) < 1.0; // returns true
     *
     * DoubleMatrix none = DoubleMatrix.random(0, 0);
     * none.isEmpty();              // returns true
     *
     * DoubleMatrix.random(-1, 3);  // throws IllegalArgumentException (negative rowCount)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix
     * @param columnCount the number of columns in the new matrix
     * @return a new DoubleMatrix of dimensions {@code rowCount x columnCount} filled with random values
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if the resulting shape is not representable
     */
    public static DoubleMatrix random(final int rowCount, final int columnCount) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final double[][] a = new double[rowCount][columnCount];

        for (double[] ea : a) {
            for (int i = 0; i < columnCount; i++) {
                ea[i] = RAND.nextDouble();
            }
        }

        return new DoubleMatrix(a);
    }

    /**
     * Creates a new matrix of the specified dimensions where every element is the provided {@code element}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.repeat(2, 3, 1.0);
     * matrix.get(0, 0);            // returns 1.0
     * matrix.get(1, 2);            // returns 1.0
     *
     * DoubleMatrix nan = DoubleMatrix.repeat(1, 2, Double.NaN);
     * Double.isNaN(nan.get(0, 0)); // returns true
     *
     * DoubleMatrix none = DoubleMatrix.repeat(0, 0, 7.0);
     * none.isEmpty();              // returns true
     *
     * DoubleMatrix.repeat(-1, 3, 1.0); // throws IllegalArgumentException (negative rowCount)
     * DoubleMatrix.repeat(0, 5, 1.0);  // throws IllegalArgumentException (0x5 shape not representable)
     * }</pre>
     *
     * @param rowCount the number of rows in the new matrix
     * @param columnCount the number of columns in the new matrix
     * @param element the double value to fill the matrix with (may be {@code NaN}, {@code +/-Infinity},
     *                or any other {@code double} value)
     * @return a new DoubleMatrix of dimensions {@code rowCount x columnCount} filled with the specified element
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative,
     *         or if the resulting shape is not representable
     */
    public static DoubleMatrix repeat(final int rowCount, final int columnCount, final double element) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        checkRepresentableShape(rowCount, columnCount);

        final double[][] a = new double[rowCount][columnCount];

        for (double[] ea : a) {
            N.fill(ea, element);
        }

        return new DoubleMatrix(a);
    }

    /**
     * Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
     * All other elements are set to zero. The resulting matrix has dimensions n×n where n is the length
     * of the diagonal array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.mainDiagonal(new double[] {1.0, 2.0, 3.0});
     * // Creates 3x3 matrix:
     * // [[1.0, 0.0, 0.0],
     * //  [0.0, 2.0, 0.0],
     * //  [0.0, 0.0, 3.0]]
     * matrix.get(0, 0);            // returns 1.0
     * matrix.get(2, 2);            // returns 3.0
     * matrix.get(0, 1);            // returns 0.0 (off-diagonal)
     *
     * DoubleMatrix empty = DoubleMatrix.mainDiagonal((double[]) null);
     * empty.isEmpty();             // returns true
     *
     * DoubleMatrix single = DoubleMatrix.mainDiagonal(new double[] {5.0});
     * single.get(0, 0);            // returns 5.0 (1x1 matrix)
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements, or {@code null}/empty for an empty matrix
     * @return a square matrix with the specified main diagonal, or an empty matrix if input is {@code null} or empty
     */
    public static DoubleMatrix mainDiagonal(final double[] mainDiagonal) {
        return diagonals(mainDiagonal, null);
    }

    /**
     * Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
     * All other elements are set to zero. The resulting matrix has dimensions n×n where n is the length
     * of the diagonal array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.antiDiagonal(new double[] {1.0, 2.0, 3.0});
     * // Creates 3x3 matrix:
     * // [[0.0, 0.0, 1.0],
     * //  [0.0, 2.0, 0.0],
     * //  [3.0, 0.0, 0.0]]
     * matrix.get(0, 2);            // returns 1.0
     * matrix.get(2, 0);            // returns 3.0
     * matrix.get(0, 0);            // returns 0.0 (off anti-diagonal)
     *
     * DoubleMatrix empty = DoubleMatrix.antiDiagonal((double[]) null);
     * empty.isEmpty();             // returns true
     *
     * DoubleMatrix single = DoubleMatrix.antiDiagonal(new double[] {5.0});
     * single.get(0, 0);            // returns 5.0 (1x1 matrix)
     * }</pre>
     *
     * @param antiDiagonal the array of anti-diagonal elements, or {@code null}/empty for an empty matrix
     * @return a square matrix with the specified anti-diagonal, or an empty matrix if input is {@code null} or empty
     */
    public static DoubleMatrix antiDiagonal(final double[] antiDiagonal) {
        return diagonals(null, antiDiagonal);
    }

    /**
     * Creates a square matrix from the specified main diagonal and anti-diagonal elements.
     * All other elements are set to zero. If both arrays are provided, they must have the same length.
     * The resulting matrix has dimensions n×n where n is the length of the non-empty diagonal array.
     *
     * <p><b>Note:</b> The anti-diagonal is written first, then the main diagonal. If both diagonals
     * share a position (which happens for odd-sized matrices at the center element), the main diagonal
     * value takes precedence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.diagonals(new double[] { 1.0, 2.0, 3.0 }, new double[] { 4.0, 5.0, 6.0 });
     * // Resulting 3x3 matrix:
     * //   {1.0, 0.0, 4.0},
     * //   {0.0, 2.0, 0.0},
     * //   {6.0, 0.0, 3.0}
     * matrix.get(0, 0);            // returns 1.0 (main diagonal)
     * matrix.get(0, 2);            // returns 4.0 (anti-diagonal)
     * matrix.get(1, 1);            // returns 2.0 (center, main wins over anti)
     *
     * DoubleMatrix mainOnly = DoubleMatrix.diagonals(new double[] {1.0, 2.0}, null);
     * mainOnly.get(1, 1);          // returns 2.0
     *
     * DoubleMatrix empty = DoubleMatrix.diagonals(null, null);
     * empty.isEmpty();             // returns true
     *
     * DoubleMatrix.diagonals(new double[] {1.0}, new double[] {1.0, 2.0}); // throws IllegalArgumentException (lengths differ)
     * }</pre>
     *
     * @param mainDiagonal the array of main diagonal elements (can be {@code null} or empty)
     * @param antiDiagonal the array of anti-diagonal elements (can be {@code null} or empty)
     * @return a square matrix with the specified diagonals, or an empty matrix if both inputs are {@code null} or empty
     * @throws IllegalArgumentException if both arrays are non-empty and have different lengths
     */
    public static DoubleMatrix diagonals(final double[] mainDiagonal, final double[] antiDiagonal) throws IllegalArgumentException {
        N.checkArgument(N.isEmpty(mainDiagonal) || N.isEmpty(antiDiagonal) || mainDiagonal.length == antiDiagonal.length,
                "The length of 'mainDiagonal' and 'antiDiagonal' must be same");

        if (N.isEmpty(mainDiagonal) && N.isEmpty(antiDiagonal)) {
            return EMPTY_DOUBLE_MATRIX;
        }

        final int len = N.max(N.len(mainDiagonal), N.len(antiDiagonal));
        final double[][] result = new double[len][len];

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

        return new DoubleMatrix(result);
    }

    /**
     * Converts a boxed Double matrix to a primitive DoubleMatrix.
     * {@code null} values in the input matrix are converted to {@code 0.0}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Matrix<Double> boxed = Matrix.of(new Double[][] {{1.0, 2.0}, {3.0, null}});
     * DoubleMatrix primitive = DoubleMatrix.unbox(boxed);
     * primitive.get(0, 1);         // returns 2.0
     * primitive.get(1, 1);         // returns 0.0 (null became 0.0)
     *
     * DoubleMatrix emptyResult = DoubleMatrix.unbox(Matrix.of(new Double[0][0]));
     * emptyResult.isEmpty();       // returns true
     *
     * DoubleMatrix.unbox((Matrix<Double>) null); // throws IllegalArgumentException
     * }</pre>
     *
     * @param x the boxed Double matrix to convert; must not be {@code null}
     * @return a new DoubleMatrix with unboxed values ({@code null} elements become {@code 0.0})
     * @throws IllegalArgumentException if {@code x} is {@code null}
     * @see #boxed()
     */
    public static DoubleMatrix unbox(final Matrix<Double> x) {
        N.checkArgNotNull(x, "x");

        return DoubleMatrix.of(Array.unbox(x.a));
    }

    /**
     * Returns the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.get(0, 1);            // returns 2.0
     * matrix.get(1, 0);            // returns 3.0
     * matrix.get(2, 0);            // throws ArrayIndexOutOfBoundsException (row out of range)
     * matrix.get(0, 5);            // throws ArrayIndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return the element at position (rowIndex, columnIndex)
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public double get(final int rowIndex, final int columnIndex) {
        return a[rowIndex][columnIndex];
    }

    /**
     * Returns the element at the specified point.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.get(Point.of(0, 1));  // returns 2.0
     * matrix.get(Point.of(1, 1));  // returns 4.0
     * matrix.get((Point) null);    // throws IllegalArgumentException (null point)
     * matrix.get(Point.of(5, 0));  // throws ArrayIndexOutOfBoundsException (out of range)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @return the double element at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #get(int, int)
     */
    public double get(final Point point) {
        N.checkArgNotNull(point, "point");

        return a[point.rowIndex()][point.columnIndex()];
    }

    /**
     * Sets the element at the specified row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.set(0, 1, 9.0);
     * matrix.get(0, 1);            // returns 9.0
     * matrix.set(1, 1, Double.NaN);
     * Double.isNaN(matrix.get(1, 1)); // returns true
     * matrix.set(2, 0, 5.0);          // throws ArrayIndexOutOfBoundsException (row out of range)
     * matrix.set(0, 9, 5.0);          // throws ArrayIndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @param value the value to set
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public void set(final int rowIndex, final int columnIndex, final double value) {
        a[rowIndex][columnIndex] = value;
    }

    /**
     * Sets the element at the specified point to the given value.
     * This is a convenience method that accepts a Point object instead of separate row and column indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.set(Point.of(0, 1), 9.0);
     * matrix.get(0, 1);            // returns 9.0
     * matrix.set(Point.of(1, 0), -2.0);
     * matrix.get(1, 0);                // returns -2.0
     * matrix.set((Point) null, 1.0);   // throws IllegalArgumentException (null point)
     * matrix.set(Point.of(5, 0), 1.0); // throws ArrayIndexOutOfBoundsException (out of range)
     * }</pre>
     *
     * @param point the point containing row and column indices (must not be {@code null})
     * @param value the new double value to set at the specified point
     * @throws IllegalArgumentException if {@code point} is {@code null}
     * @throws ArrayIndexOutOfBoundsException if the point coordinates are out of bounds
     * @see #set(int, int, double)
     */
    public void set(final Point point, final double value) {
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.valueAbove(1, 0).getAsDouble(); // returns 1.0
     * matrix.valueAbove(1, 1).getAsDouble(); // returns 2.0
     * matrix.valueAbove(0, 0).isPresent();   // returns false (no row above)
     * matrix.valueAbove(5, 0);               // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an {@code OptionalDouble} containing the element at position (rowIndex - 1, columnIndex), or empty if rowIndex == 0
     * @throws IndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalDouble valueAbove(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == 0 ? OptionalDouble.empty() : OptionalDouble.of(a[rowIndex - 1][columnIndex]);
    }

    /**
     * Returns the element directly below the specified position, if it exists.
     * This method provides safe access to the element directly below the given position
     * without throwing an exception when at the bottom edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.valueBelow(0, 0).getAsDouble(); // returns 3.0
     * matrix.valueBelow(0, 1).getAsDouble(); // returns 4.0
     * matrix.valueBelow(1, 0).isPresent();   // returns false (no row below)
     * matrix.valueBelow(0, 5);               // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an {@code OptionalDouble} containing the element at position (rowIndex + 1, columnIndex), or empty if rowIndex == rowCount - 1
     * @throws IndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalDouble valueBelow(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return rowIndex == rowCount - 1 ? OptionalDouble.empty() : OptionalDouble.of(a[rowIndex + 1][columnIndex]);
    }

    /**
     * Returns the element directly to the left of the specified position, if it exists.
     * This method provides safe access to the element directly to the left of the given position
     * without throwing an exception when at the leftmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.valueLeft(0, 1).getAsDouble(); // returns 1.0
     * matrix.valueLeft(1, 1).getAsDouble(); // returns 3.0
     * matrix.valueLeft(0, 0).isPresent();   // returns false (no column to the left)
     * matrix.valueLeft(5, 0);               // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an {@code OptionalDouble} containing the element at position (rowIndex, columnIndex - 1), or empty if columnIndex == 0
     * @throws IndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalDouble valueLeft(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == 0 ? OptionalDouble.empty() : OptionalDouble.of(a[rowIndex][columnIndex - 1]);
    }

    /**
     * Returns the element directly to the right of the specified position, if it exists.
     * This method provides safe access to the element directly to the right of the given position
     * without throwing an exception when at the rightmost edge of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.valueRight(0, 0).getAsDouble(); // returns 2.0
     * matrix.valueRight(1, 0).getAsDouble(); // returns 4.0
     * matrix.valueRight(0, 1).isPresent();   // returns false (no column to the right)
     * matrix.valueRight(0, 5);               // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return an {@code OptionalDouble} containing the element at position (rowIndex, columnIndex + 1), or empty if columnIndex == columnCount - 1
     * @throws IndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public OptionalDouble valueRight(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        return columnIndex == columnCount - 1 ? OptionalDouble.empty() : OptionalDouble.of(a[rowIndex][columnIndex + 1]);
    }

    /**
     * Returns the specified row as a double array.
     *
     * <p><b>Note:</b> This method returns a reference to the internal array, not a copy.
     * Modifications to the returned array will affect the matrix. If you need an independent
     * copy, use {@link #rowCopy(int)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * double[] firstRow = matrix.rowView(0);   // returns [1.0, 2.0, 3.0]
     * matrix.rowView(1)[0];                    // returns 4.0
     *
     * // Direct modification affects the matrix (shared reference)
     * firstRow[0] = 99.0;
     * matrix.get(0, 0);                         // returns 99.0
     *
     * matrix.rowView(-1);                       // throws IndexOutOfBoundsException (negative index)
     * matrix.rowView(2);                        // throws IndexOutOfBoundsException (index >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return the specified row array (direct reference to internal storage)
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    @Override
    public double[] rowView(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return a[rowIndex];
    }

    /**
     * Returns a defensive copy of the specified row.
     * Changes to the returned array do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * double[] firstRow = matrix.rowCopy(0);   // returns [1.0, 2.0, 3.0]
     * matrix.rowCopy(1)[2];                    // returns 6.0
     *
     * // Modifying the copy does NOT affect the matrix
     * firstRow[0] = 99.0;
     * matrix.get(0, 0);                         // returns 1.0 (unchanged)
     *
     * matrix.rowCopy(-1);                       // throws IndexOutOfBoundsException (negative index)
     * matrix.rowCopy(2);                        // throws IndexOutOfBoundsException (index >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new double array containing the values from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    @Override
    public double[] rowCopy(final int rowIndex) throws IndexOutOfBoundsException {
        checkRowIndex(rowIndex);

        return N.copyOf(a[rowIndex], columnCount);
    }

    /**
     * Returns a copy of the specified column as a new double array.
     *
     * <p>Unlike {@link #rowView(int)}, this method always returns a new array copy since
     * columns are not stored contiguously in memory. Modifications to the returned array
     * will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * double[] firstColumn = matrix.columnCopy(0);   // returns [1.0, 4.0]
     * matrix.columnCopy(2)[1];                       // returns 6.0
     *
     * // Modification does NOT affect the matrix (it's a copy)
     * firstColumn[0] = 99.0;
     * matrix.get(0, 0);                               // returns 1.0 (unchanged)
     *
     * matrix.columnCopy(-1);                          // throws IndexOutOfBoundsException (negative index)
     * matrix.columnCopy(3);                           // throws IndexOutOfBoundsException (index >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new array containing the values from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    @Override
    public double[] columnCopy(final int columnIndex) throws IndexOutOfBoundsException {
        checkColumnIndex(columnIndex);

        final double[] c = new double[rowCount];

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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * matrix.setRow(0, new double[] {7.0, 8.0, 9.0});
     * matrix.get(0, 0);            // returns 7.0
     * matrix.get(0, 2);            // returns 9.0
     *
     * matrix.setRow(5, new double[] {1.0, 2.0, 3.0}); // throws IndexOutOfBoundsException (row out of range)
     * matrix.setRow(0, new double[] {1.0, 2.0});      // throws IllegalArgumentException (length mismatch)
     * }</pre>
     *
     * @param rowIndex the index of the row to set (0-based)
     * @param row the array of values to copy into the row; must have length equal to the number of columns
     * @throws IndexOutOfBoundsException if {@code rowIndex} is out of bounds
     * @throws IllegalArgumentException if {@code row} is {@code null} or if {@code row.length != columnCount}
     */
    public void setRow(final int rowIndex, final double[] row) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * matrix.setColumn(0, new double[] {7.0, 8.0});
     * matrix.get(0, 0);            // returns 7.0
     * matrix.get(1, 0);            // returns 8.0
     *
     * matrix.setColumn(9, new double[] {1.0, 2.0});  // throws IndexOutOfBoundsException (column out of range)
     * matrix.setColumn(0, new double[] {1.0});       // throws IllegalArgumentException (length mismatch)
     * }</pre>
     *
     * @param columnIndex the index of the column to set (0-based)
     * @param column the array of values to copy into the column; must have length equal to the number of rows
     * @throws IndexOutOfBoundsException if {@code columnIndex} is out of bounds
     * @throws IllegalArgumentException if {@code column} is {@code null} or if {@code column.length != rowCount}
     */
    public void setColumn(final int columnIndex, final double[] column) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * matrix.updateRow(0, x -> x * 2);
     * matrix.get(0, 0);            // returns 2.0
     * matrix.get(0, 2);            // returns 6.0
     * matrix.get(1, 0);            // returns 4.0 (row 1 untouched)
     *
     * matrix.updateRow(0, x -> x + 1.0);
     * matrix.get(0, 0);            // returns 3.0
     *
     * matrix.updateRow(5, x -> x); // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param rowIndex the index of the row to update (0-based)
     * @param operator the operator to apply to each element in the row; receives the current
     *             element value and returns the new value
     * @throws IndexOutOfBoundsException if rowIndex is out of bounds
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateRow(final int rowIndex, final Throwables.DoubleUnaryOperator<E> operator) throws E {
        checkRowIndex(rowIndex);

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < columnCount; i++) {
            a[rowIndex][i] = operator.applyAsDouble(a[rowIndex][i]);
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}, {5.0, 6.0}});
     * matrix.updateColumn(0, x -> x + 10.0);
     * matrix.get(0, 0);            // returns 11.0
     * matrix.get(2, 0);            // returns 15.0
     * matrix.get(0, 1);            // returns 2.0 (column 1 untouched)
     *
     * matrix.updateColumn(1, x -> x * x);
     * matrix.get(1, 1);            // returns 16.0
     *
     * matrix.updateColumn(9, x -> x); // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param columnIndex the index of the column to update (0-based)
     * @param operator the operator to apply to each element in the column; receives the current
     *             element value and returns the new value
     * @throws IndexOutOfBoundsException if columnIndex is out of bounds
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.DoubleUnaryOperator<E> operator) throws E {
        checkColumnIndex(columnIndex);

        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnIndex] = operator.applyAsDouble(a[i][columnIndex]);
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}, {7.0, 8.0, 9.0}});
     * double[] diagonal = matrix.getMainDiagonal();   // returns [1.0, 5.0, 9.0]
     *
     * DoubleMatrix single = DoubleMatrix.of(new double[][] {{42.0}});
     * single.getMainDiagonal();                        // returns [42.0]
     *
     * DoubleMatrix rect = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * rect.getMainDiagonal();                          // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new double array containing a copy of the main diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public double[] getMainDiagonal() throws IllegalStateException {
        checkIsSquare();

        final double[] res = new double[rowCount];

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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.setMainDiagonal(new double[] {9.0, 8.0});
     * matrix.get(0, 0);            // returns 9.0
     * matrix.get(1, 1);            // returns 8.0
     * matrix.get(0, 1);            // returns 2.0 (off-diagonal unchanged)
     *
     * matrix.setMainDiagonal(new double[] {1.0}); // throws IllegalArgumentException (length != rowCount)
     *
     * DoubleMatrix rect = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * rect.setMainDiagonal(new double[] {1.0, 2.0}); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param mainDiagonal the new values for the main diagonal; must have length equal to rowCount
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if mainDiagonal is null or its array length does not equal rowCount
     */
    @Override
    public void setMainDiagonal(final double[] mainDiagonal) throws IllegalStateException, IllegalArgumentException {
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{2.0, 1.0}, {1.0, 3.0}});
     * matrix.updateMainDiagonal(x -> x * x);
     * matrix.get(0, 0);            // returns 4.0
     * matrix.get(1, 1);            // returns 9.0
     * matrix.get(0, 1);            // returns 1.0 (off-diagonal unchanged)
     *
     * DoubleMatrix rect = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * rect.updateMainDiagonal(x -> x + 1.0); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each diagonal element; receives the current
     *             element value and returns the new value
     * @throws IllegalStateException if the matrix is not square
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateMainDiagonal(final Throwables.DoubleUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][i] = operator.applyAsDouble(a[i][i]);
        }
    }

    /**
     * Returns a copy of the anti-diagonal elements (upper-right to lower-left).
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>This method extracts the anti-diagonal (secondary diagonal) elements from
     * upper-right to lower-left, at positions (0,n-1), (1,n-2), (2,n-3), etc.
     * The returned array is a copy; modifications to it will not affect the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}, {7.0, 8.0, 9.0}});
     * double[] diagonal = matrix.getAntiDiagonal();   // returns [3.0, 5.0, 7.0]
     *
     * DoubleMatrix single = DoubleMatrix.of(new double[][] {{42.0}});
     * single.getAntiDiagonal();                        // returns [42.0]
     *
     * DoubleMatrix rect = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * rect.getAntiDiagonal();                          // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new double array containing a copy of the anti-diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    @Override
    public double[] getAntiDiagonal() throws IllegalStateException {
        checkIsSquare();

        final double[] res = new double[rowCount];

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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.setAntiDiagonal(new double[] {9.0, 8.0});
     * matrix.get(0, 1);            // returns 9.0
     * matrix.get(1, 0);            // returns 8.0
     * matrix.get(0, 0);            // returns 1.0 (off anti-diagonal unchanged)
     *
     * matrix.setAntiDiagonal(new double[] {1.0}); // throws IllegalArgumentException (length != rowCount)
     *
     * DoubleMatrix rect = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * rect.setAntiDiagonal(new double[] {1.0, 2.0}); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must have length equal to rowCount
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if antiDiagonal is null or its array length does not equal rowCount
     */
    @Override
    public void setAntiDiagonal(final double[] antiDiagonal) throws IllegalStateException, IllegalArgumentException {
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.updateAntiDiagonal(x -> -x);
     * matrix.get(0, 1);            // returns -2.0
     * matrix.get(1, 0);            // returns -3.0
     * matrix.get(0, 0);            // returns 1.0 (off anti-diagonal unchanged)
     *
     * DoubleMatrix rect = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * rect.updateAntiDiagonal(x -> x + 1.0); // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each anti-diagonal element; receives the current
     *             element value and returns the new value
     * @throws IllegalStateException if the matrix is not square
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAntiDiagonal(final Throwables.DoubleUnaryOperator<E> operator) throws IllegalStateException, E {
        checkIsSquare();
        N.checkArgNotNull(operator, "operator");

        for (int i = 0; i < rowCount; i++) {
            a[i][columnCount - i - 1] = operator.applyAsDouble(a[i][columnCount - i - 1]);
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.updateAll(x -> x * 2);
     * matrix.get(0, 0);            // returns 2.0
     * matrix.get(1, 1);            // returns 8.0
     *
     * matrix.updateAll(x -> x / 0.0);               // no exception; produces Infinity
     * matrix.get(0, 0) == Double.POSITIVE_INFINITY; // returns true
     *
     * DoubleMatrix empty = DoubleMatrix.empty();
     * empty.updateAll(x -> x + 1.0);  // no-op on empty matrix
     * empty.isEmpty();                // returns true
     * }</pre>
     *
     * @param <E> the type of exception that the operator may throw
     * @param operator the operator to apply to each element; receives the current element value
     *             and returns the new value
     * @throws IllegalArgumentException if {@code operator} is {@code null}
     * @throws E if the operator throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.DoubleUnaryOperator<E> operator) throws E {
        N.checkArgNotNull(operator, "operator");
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = operator.applyAsDouble(a[i][j]);
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}});
     * matrix.updateAll((i, j) -> (double) (i + j));
     * matrix.get(0, 2);            // returns 2.0
     * matrix.get(1, 2);            // returns 3.0
     *
     * matrix.updateAll((i, j) -> i * 10.0 + j);
     * matrix.get(0, 0);            // returns 0.0
     * matrix.get(1, 2);            // returns 12.0
     *
     * DoubleMatrix empty = DoubleMatrix.empty();
     * empty.updateAll((i, j) -> 1.0); // no-op on empty matrix
     * empty.isEmpty();                // returns true
     * }</pre>
     *
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the function that receives row index and column index (0-based) and returns
     *             the new value for that position
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the mapper throws an exception
     */
    public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Double, E> mapper) throws E {
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{-1.0, 2.0, -3.0}, {4.0, -5.0, 6.0}});
     * matrix.replaceIf(x -> x < 0, 0.0);
     * matrix.get(0, 0);            // returns 0.0
     * matrix.get(0, 1);            // returns 2.0 (unchanged)
     *
     * matrix.replaceIf(x -> x > 3.0, 99.0);
     * matrix.get(1, 0);            // returns 99.0
     * matrix.get(1, 2);            // returns 99.0
     *
     * // NaN never satisfies a comparison, so it is never matched here:
     * DoubleMatrix withNan = DoubleMatrix.of(new double[][] {{Double.NaN, 1.0}});
     * withNan.replaceIf(x -> x > 0.0, 5.0);
     * Double.isNaN(withNan.get(0, 0)); // returns true (NaN > 0.0 is false)
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition to test each element; elements for which this returns
     *                  {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.DoublePredicate<E> predicate, final double newValue) throws E {
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}, {7.0, 8.0, 9.0}});
     * matrix.replaceIf((i, j) -> i == j, 0.0);
     * matrix.get(0, 0);            // returns 0.0
     * matrix.get(2, 2);            // returns 0.0
     * matrix.get(0, 1);            // returns 2.0 (unchanged)
     *
     * matrix.replaceIf((i, j) -> i == 0 || j == 0, -1.0);
     * matrix.get(0, 1);            // returns -1.0
     * matrix.get(2, 0);            // returns -1.0
     * matrix.get(1, 1);            // returns 0.0 (interior unchanged)
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the condition that tests row index and column index (0-based); elements
     *                  at positions for which this returns {@code true} will be replaced
     * @param newValue the value to use for replacing matching elements
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final double newValue) throws E {
        N.checkArgNotNull(predicate, "predicate");
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> a[i][j] = predicate.test(i, j) ? newValue : a[i][j];
        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));
    }

    /**
     * Creates a new DoubleMatrix by applying a transformation function to each element.
     * The original matrix is not modified; a new matrix with transformed values is returned.
     *
     * <p>The operation may be performed in parallel for large matrices to improve performance.
     * This is the immutable counterpart to {@link #updateAll(Throwables.DoubleUnaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix squared = matrix.map(x -> x * x);
     * squared.get(1, 1);           // returns 16.0
     * matrix.get(1, 1);            // returns 4.0 (original unchanged)
     *
     * DoubleMatrix negated = matrix.map(x -> -x);
     * negated.get(0, 0);           // returns -1.0
     *
     * // Division by zero yields Infinity (no exception):
     * DoubleMatrix inv = matrix.map(x -> 1.0 / (x - 1.0));
     * inv.get(0, 0) == Double.POSITIVE_INFINITY; // returns true (1.0/0.0)
     *
     * DoubleMatrix empty = DoubleMatrix.empty().map(x -> x + 1.0);
     * empty.isEmpty();             // returns true
     * }</pre>
     *
     * @param <E> the type of exception that the function may throw
     * @param mapper the function to apply to each element; receives the current element value
     *             and returns the transformed value
     * @return a new DoubleMatrix with transformed values
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     * @see #updateAll(Throwables.DoubleUnaryOperator)
     */
    public <E extends Exception> DoubleMatrix map(final Throwables.DoubleUnaryOperator<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final double[][] result = new double[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsDouble(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return DoubleMatrix.of(result);
    }

    /**
     * Creates a new IntMatrix by applying the specified function to each element of this matrix.
     * The original matrix is not modified. Each double element is independently converted to an int
     * by the function, and the results are collected into a new IntMatrix with the same dimensions.
     * The operation may be performed in parallel for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.6, 2.4}, {3.7, 4.2}});
     * IntMatrix rounded = matrix.mapToInt(d -> (int) Math.round(d));
     * rounded.get(0, 0);           // returns 2
     * rounded.get(1, 0);           // returns 4
     *
     * IntMatrix truncated = matrix.mapToInt(d -> (int) d);
     * truncated.get(1, 1);         // returns 4 (4.2 truncated toward zero)
     *
     * IntMatrix emptyResult = DoubleMatrix.empty().mapToInt(d -> (int) d);
     * emptyResult.isEmpty();       // returns true
     * }</pre>
     *
     * @param <E> the exception type that the function may throw
     * @param mapper the mapping function that converts each double element to an int; must not be {@code null}
     * @return a new IntMatrix with the mapped values (same dimensions as the original)
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> IntMatrix mapToInt(final Throwables.DoubleToIntFunction<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final int[][] result = new int[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsInt(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return IntMatrix.of(result);
    }

    /**
     * Creates a new LongMatrix by applying the specified function to each element of this matrix.
     * The original matrix is not modified. Each double element is independently converted to a long
     * by the function, and the results are collected into a new LongMatrix with the same dimensions.
     * The operation may be performed in parallel for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.6, 2.4}, {3.7, 4.2}});
     * LongMatrix rounded = matrix.mapToLong(d -> Math.round(d));
     * rounded.get(0, 0);           // returns 2L
     * rounded.get(1, 0);           // returns 4L
     *
     * LongMatrix truncated = matrix.mapToLong(d -> (long) d);
     * truncated.get(1, 1);         // returns 4L (4.2 truncated toward zero)
     *
     * LongMatrix emptyResult = DoubleMatrix.empty().mapToLong(d -> (long) d);
     * emptyResult.isEmpty();       // returns true
     * }</pre>
     *
     * @param <E> the exception type that the function may throw
     * @param mapper the mapping function that converts each double element to a long; must not be {@code null}
     * @return a new LongMatrix with the mapped values (same dimensions as the original)
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <E extends Exception> LongMatrix mapToLong(final Throwables.DoubleToLongFunction<E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        final long[][] result = new long[rowCount][columnCount];
        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = mapper.applyAsLong(a[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return LongMatrix.of(result);
    }

    /**
     * Creates a new {@code Matrix<R>} by applying the specified function to each element of this matrix.
     * The original matrix is not modified. Each double element is independently converted to an object
     * of type R by the function, and the results are collected into a new Matrix with the same dimensions.
     * The operation may be performed in parallel for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 4.0}, {7.0, 0.0}});
     * Matrix<String> labels = matrix.mapToObj(d -> d > 3.0 ? "high" : "low", String.class);
     * labels.get(0, 0);            // returns "low"
     * labels.get(0, 1);            // returns "high"
     * labels.get(1, 0);            // returns "high"
     *
     * Matrix<String> emptyResult = DoubleMatrix.empty().mapToObj(d -> "x", String.class);
     * emptyResult.isEmpty();       // returns true
     * }</pre>
     *
     * @param <R> the type of elements in the resulting matrix
     * @param <E> the exception type that the function may throw
     * @param mapper the mapping function that converts each double element to type R; must not be {@code null}
     * @param targetElementType the class object representing the target element type (used for array creation); must not be {@code null}
     * @return a new {@code Matrix<R>} with the mapped values (same dimensions as the original)
     * @throws IllegalArgumentException if {@code mapper} or {@code targetElementType} is {@code null}
     * @throws E if the function throws an exception
     */
    public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.DoubleFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E {
        N.checkArgNotNull(mapper, "mapper");
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.fill(0.0);
     * matrix.get(0, 0);            // returns 0.0
     * matrix.get(1, 1);            // returns 0.0
     *
     * DoubleMatrix ones = DoubleMatrix.of(new double[3][3]);
     * ones.fill(1.0);
     * ones.get(2, 2);             // returns 1.0
     *
     * DoubleMatrix nan = DoubleMatrix.of(new double[][] {{1.0}});
     * nan.fill(Double.NaN);
     * Double.isNaN(nan.get(0, 0)); // returns true
     *
     * DoubleMatrix empty = DoubleMatrix.empty();
     * empty.fill(5.0);             // no-op on empty matrix
     * empty.isEmpty();             // returns true
     * }</pre>
     *
     * @param value the value to fill the matrix with
     */
    public void fill(final double value) {
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[3][3]);
     * matrix.fill(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * // Top-left 2x2 region is filled: [[1.0, 2.0, 0.0], [3.0, 4.0, 0.0], [0.0, 0.0, 0.0]]
     * matrix.get(0, 0);            // returns 1.0
     * matrix.get(1, 1);            // returns 4.0
     * matrix.get(2, 2);            // returns 0.0 (outside source region)
     *
     * // A larger source is truncated to the matrix bounds:
     * DoubleMatrix small = DoubleMatrix.of(new double[1][1]);
     * small.fill(new double[][] {{5.0, 6.0}, {7.0, 8.0}});
     * small.get(0, 0);             // returns 5.0
     * }</pre>
     *
     * @param source the source array to copy values from (may be smaller or larger than the matrix)
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final double[][] source) {
        fill(0, 0, source);
    }

    /**
     * Fills a portion of the matrix with values from the specified two-dimensional array in-place, starting from a specified position.
     * Values are copied starting from the specified row and column indices. If the source array extends
     * beyond the matrix bounds from the starting position, only the portion that fits is copied.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[3][3]);
     * matrix.fill(1, 1, new double[][] {{9.0, 8.0}, {7.0, 6.0}});
     * // Result: [[0.0, 0.0, 0.0], [0.0, 9.0, 8.0], [0.0, 7.0, 6.0]]
     * matrix.get(1, 1);            // returns 9.0
     * matrix.get(2, 2);            // returns 6.0
     * matrix.get(0, 0);            // returns 0.0 (untouched)
     *
     * matrix.fill(0, 0, (double[][]) null);      // throws IllegalArgumentException (null source)
     * matrix.fill(5, 0, new double[][] {{1.0}}); // throws IndexOutOfBoundsException (destRowIndex out of range)
     * }</pre>
     *
     * @param destRowIndex the target row index in this matrix (0-based)
     * @param destColumnIndex the target column index in this matrix (0-based)
     * @param source the source array to copy values from; must not be {@code null}.
     *               Individual {@code null} sub-arrays in {@code source} are skipped.
     * @throws IndexOutOfBoundsException if {@code destRowIndex < 0} or {@code destRowIndex > rowCount},
     *         or if {@code destColumnIndex < 0} or {@code destColumnIndex > columnCount}
     * @throws IllegalArgumentException if {@code source} is {@code null}
     */
    public void fill(final int destRowIndex, final int destColumnIndex, final double[][] source) throws IndexOutOfBoundsException, IllegalArgumentException {
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
     *
     * <p>The returned matrix is completely independent from the original. All elements
     * are copied into a new two-dimensional array, ensuring that modifications to either
     * the copy or the original will not affect the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix original = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix copy = original.copy();
     * copy.equals(original);       // returns true (same contents)
     *
     * // Modifying the copy does NOT affect the original
     * copy.set(0, 0, 99.0);
     * original.get(0, 0);          // returns 1.0 (unchanged)
     * copy.get(0, 0);              // returns 99.0
     *
     * DoubleMatrix emptyCopy = DoubleMatrix.empty().copy();
     * emptyCopy.isEmpty();         // returns true
     * }</pre>
     *
     * @return a new matrix that is a copy of this matrix with full independence guarantee
     */
    @Override
    public DoubleMatrix copy() {
        final double[][] c = new double[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[i].clone();
        }

        return new DoubleMatrix(c);
    }

    /**
     * Returns a copy of a row range from this matrix.
     * The returned matrix contains only the specified rows and is completely independent from the original matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}, {5.0, 6.0}});
     * DoubleMatrix partial = matrix.copy(1, 3);   // returns [[3.0, 4.0], [5.0, 6.0]]
     * partial.get(0, 0);                          // returns 3.0
     * partial.rowCount();                         // returns 2
     *
     * DoubleMatrix none = matrix.copy(1, 1);       // empty row range
     * none.rowCount();                             // returns 0
     *
     * matrix.copy(0, 5);           // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new DoubleMatrix containing a copy of the specified rows
     * @throws IndexOutOfBoundsException if the indices are out of bounds
     */
    @Override
    public DoubleMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        final double[][] c = new double[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = a[i].clone();
        }

        return new DoubleMatrix(c);
    }

    /**
     * Returns a copy of a rectangular region from this matrix.
     * The returned matrix contains only the specified rows and columns and is completely
     * independent from the original matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * DoubleMatrix sub = matrix.copy(0, 2, 1, 3);   // rows 0-1, columns 1-2
     * sub.get(0, 0);                                // returns 2.0
     * sub.get(1, 1);                                // returns 6.0
     * sub.columnCount();                            // returns 2
     *
     * matrix.copy(0, 2, 1, 9);     // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.copy(0, 5, 0, 3);     // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new DoubleMatrix containing the specified region with dimensions
     *         (toRowIndex - fromRowIndex) × (toColumnIndex - fromColumnIndex)
     * @throws IndexOutOfBoundsException if any index is out of bounds, {@code fromRowIndex > toRowIndex}, or {@code fromColumnIndex > toColumnIndex}
     */
    @Override
    public DoubleMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        final double[][] c = new double[toRowIndex - fromRowIndex][];

        for (int i = fromRowIndex; i < toRowIndex; i++) {
            c[i - fromRowIndex] = N.copyOfRange(a[i], fromColumnIndex, toColumnIndex);
        }

        return new DoubleMatrix(c);
    }

    /**
     * Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount},
     * anchored at the top-left corner of this matrix. New cells are filled with {@code 0.0}.
     *
     * <ul>
     *   <li><b>If a dimension shrinks</b> — elements beyond the new boundary are discarded
     *       (excess rows removed from the bottom, excess columns removed from the right).</li>
     *   <li><b>If a dimension grows</b> — new cells are filled with {@code 0.0}.</li>
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}, {7.0, 8.0, 9.0}});
     *
     * // Grow: both dimensions larger — new cells filled with 0.0
     * DoubleMatrix grown = matrix.resize(4, 4);
     * // Result: [[1.0, 2.0, 3.0, 0.0],
     * //          [4.0, 5.0, 6.0, 0.0],
     * //          [7.0, 8.0, 9.0, 0.0],
     * //          [0.0, 0.0, 0.0, 0.0]]
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * DoubleMatrix truncated = matrix.resize(2, 2);
     * // Result: [[1.0, 2.0],
     * //          [4.0, 5.0]]
     *
     * // Mixed: grow rows, truncate columns
     * DoubleMatrix mixed = matrix.resize(4, 2);
     * // Result: [[1.0, 2.0],
     * //          [4.0, 5.0],
     * //          [7.0, 8.0],
     * //          [0.0, 0.0]]
     * mixed.get(0, 0);             // returns 1.0
     * mixed.get(3, 1);             // returns 0.0 (newly added cell)
     *
     * matrix.resize(-1, 2);        // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @return a new DoubleMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int, double)
     * @see #extend(int, int, int, int)
     */
    public DoubleMatrix resize(final int newRowCount, final int newColumnCount) {
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
     * <p><b>Comparison with {@link #extend(int, int, int, int, double)}:</b>
     * {@code resize} takes <em>absolute</em> target dimensions and may truncate existing content.
     * {@code extend} takes <em>relative</em> padding amounts per edge and <em>never truncates</em>.
     * Use {@code extend} when the entire original content must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}, {7.0, 8.0, 9.0}});
     *
     * // Grow: both dimensions larger — new cells filled with 9.0
     * DoubleMatrix grown = matrix.resize(4, 4, 9.0);
     * // Result: [[1.0, 2.0, 3.0, 9.0],
     * //          [4.0, 5.0, 6.0, 9.0],
     * //          [7.0, 8.0, 9.0, 9.0],
     * //          [9.0, 9.0, 9.0, 9.0]]
     *
     * // Truncate: both dimensions smaller — bottom rows and right columns discarded
     * DoubleMatrix truncated = matrix.resize(2, 2, 9.0);
     * // Result: [[1.0, 2.0],
     * //          [4.0, 5.0]]
     *
     * // Mixed: grow rows, truncate columns
     * DoubleMatrix mixed = matrix.resize(4, 2, 9.0);
     * // Result: [[1.0, 2.0],
     * //          [4.0, 5.0],
     * //          [7.0, 8.0],
     * //          [9.0, 9.0]]
     * mixed.get(0, 0);             // returns 1.0
     * mixed.get(3, 1);             // returns 9.0 (newly added cell)
     *
     * matrix.resize(2, -1, 9.0);   // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the row count of the returned matrix; must be {@code >= 0}
     * @param newColumnCount the column count of the returned matrix; must be {@code >= 0}
     * @param defaultValue the double value used to fill any newly created cells
     * @return a new DoubleMatrix with the specified dimensions
     * @throws IllegalArgumentException if {@code newRowCount} or {@code newColumnCount} is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
     * @see #resize(int, int)
     * @see #extend(int, int, int, int, double)
     */
    public DoubleMatrix resize(final int newRowCount, final int newColumnCount, final double defaultValue) throws IllegalArgumentException {
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
            final boolean fillDefaultValue = Double.doubleToRawLongBits(defaultValue) != 0;
            final double[][] b = new double[newRowCount][];

            for (int i = 0; i < newRowCount; i++) {
                b[i] = i < rowCount ? N.copyOf(a[i], newColumnCount) : new double[newColumnCount];

                if (fillDefaultValue) {
                    if (i >= rowCount) {
                        N.fill(b[i], defaultValue);
                    } else if (columnCount < newColumnCount) {
                        N.fill(b[i], columnCount, newColumnCount, defaultValue);
                    }
                }
            }

            return new DoubleMatrix(b);
        }
    }

    /**
     * Returns a new matrix formed by adding {@code 0.0}-filled padding around every edge of this matrix.
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}});
     *
     * // Uniform border of 1 cell on every side
     * DoubleMatrix bordered = matrix.extend(1, 1, 1, 1);
     * // Result: [[0.0, 0.0, 0.0, 0.0],
     * //          [0.0, 1.0, 2.0, 0.0],
     * //          [0.0, 0.0, 0.0, 0.0]]
     * bordered.rowCount();         // returns 3
     * bordered.columnCount();      // returns 4
     * bordered.get(1, 1);          // returns 1.0 (original content preserved)
     * bordered.get(0, 0);          // returns 0.0 (new padding cell)
     *
     * DoubleMatrix same = matrix.extend(0, 0, 0, 0); // no padding -> copy
     * same.equals(matrix);                           // returns true
     *
     * matrix.extend(-1, 0, 0, 0);  // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of rows to add above; must be {@code >= 0}
     * @param padBottom number of rows to add below; must be {@code >= 0}
     * @param padLeft number of columns to add to the left; must be {@code >= 0}
     * @param padRight number of columns to add to the right; must be {@code >= 0}
     * @return a new DoubleMatrix with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #extend(int, int, int, int, double)
     * @see #resize(int, int)
     */
    @Override
    public DoubleMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight) {
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
     * <p><b>Unlike {@link #resize(int, int, double)}, this method never truncates existing content.</b>
     * All elements of the original matrix appear unchanged in the result.</p>
     *
     * <p><b>Typical uses:</b> zero-padding before convolution, adding sentinel borders, or creating
     * asymmetric margins (e.g. more padding on one side than another).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}});
     *
     * // Asymmetric padding: 2 columns on the left, 1 on the right
     * DoubleMatrix padded = matrix.extend(1, 1, 2, 1, 9.0);
     * // Result: [[9.0, 9.0, 9.0, 9.0, 9.0],
     * //          [9.0, 9.0, 1.0, 2.0, 9.0],
     * //          [9.0, 9.0, 9.0, 9.0, 9.0]]
     * padded.get(1, 2);            // returns 1.0 (original content)
     * padded.get(0, 0);            // returns 9.0 (padding cell)
     * padded.columnCount();        // returns 5
     *
     * // Uniform border of 1 cell on every side
     * DoubleMatrix bordered = matrix.extend(1, 1, 1, 1, 0.0);
     * bordered.get(1, 1);          // returns 1.0
     * bordered.get(0, 0);          // returns 0.0
     *
     * matrix.extend(0, -1, 0, 0, 9.0); // throws IllegalArgumentException (negative padding)
     * }</pre>
     *
     * @param padTop number of rows to add above; must be {@code >= 0}
     * @param padBottom number of rows to add below; must be {@code >= 0}
     * @param padLeft number of columns to add to the left; must be {@code >= 0}
     * @param padRight number of columns to add to the right; must be {@code >= 0}
     * @param defaultValue the double value used to fill all newly added cells
     * @return a new DoubleMatrix with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
     * @throws IllegalArgumentException if any padding parameter is negative,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see #extend(int, int, int, int)
     * @see #resize(int, int, double)
     */
    public DoubleMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final double defaultValue)
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
            final boolean fillDefaultValue = Double.doubleToRawLongBits(defaultValue) != 0;
            final double[][] b = new double[newRowCount][newColumnCount];

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

            return new DoubleMatrix(b);
        }
    }

    /**
     * Reverses the order of elements in each row (horizontal flip in-place).
     * This method modifies the matrix in place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}});
     * matrix.flipHorizontallyInPlace();
     * matrix.get(0, 0);            // returns 3.0
     * matrix.get(0, 2);            // returns 1.0
     *
     * DoubleMatrix empty = DoubleMatrix.empty();
     * empty.flipHorizontallyInPlace(); // no-op on empty matrix
     * empty.isEmpty();                 // returns true
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
     * Reverses the order of rows (vertical flip in-place).
     * This method modifies the matrix in place by reversing the order of rows.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0}, {2.0}, {3.0}});
     * matrix.flipVerticallyInPlace();
     * matrix.get(0, 0);            // returns 3.0
     * matrix.get(2, 0);            // returns 1.0
     *
     * DoubleMatrix empty = DoubleMatrix.empty();
     * empty.flipVerticallyInPlace(); // no-op on empty matrix
     * empty.isEmpty();               // returns true
     * }</pre>
     *
     * @see #flipVertically()
     */
    @Override
    public void flipVerticallyInPlace() {
        for (int l = 0, h = rowCount - 1; l < h; l++, h--) {
            final double[] tmp = a[l];
            a[l] = a[h];
            a[h] = tmp;
        }
    }

    /**
     * Returns a new matrix that is a horizontal flip of this matrix (columns in reversed order within each row).
     * The original matrix is not modified. This is a non-mutating version of {@link #flipHorizontallyInPlace()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}});
     * DoubleMatrix flipped = matrix.flipHorizontally();
     * flipped.get(0, 0);           // returns 3.0
     * flipped.get(0, 2);           // returns 1.0
     * matrix.get(0, 0);            // returns 1.0 (original unchanged)
     *
     * DoubleMatrix empty = DoubleMatrix.empty().flipHorizontally();
     * empty.isEmpty();             // returns true
     * }</pre>
     *
     * @return a new matrix that is a horizontal flip of this matrix (columns in reversed order within each row)
     * @see #flipHorizontallyInPlace()
     * @see #flipVertically()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public DoubleMatrix flipHorizontally() {
        final DoubleMatrix res = this.copy();
        res.flipHorizontallyInPlace();
        return res;
    }

    /**
     * Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
     * The original matrix is not modified. This is a non-mutating version of {@link #flipVerticallyInPlace()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0}, {2.0}, {3.0}});
     * DoubleMatrix flipped = matrix.flipVertically();
     * flipped.get(0, 0);           // returns 3.0
     * flipped.get(2, 0);           // returns 1.0
     * matrix.get(0, 0);            // returns 1.0 (original unchanged)
     *
     * DoubleMatrix empty = DoubleMatrix.empty().flipVertically();
     * empty.isEmpty();             // returns true
     * }</pre>
     *
     * @return a new matrix that is a vertical flip of this matrix (rows in reversed order)
     * @see #flipVerticallyInPlace()
     * @see #flipHorizontally()
     * @see <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">MATLAB flip function</a>
     */
    @Override
    public DoubleMatrix flipVertically() {
        final DoubleMatrix res = this.copy();
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix r = matrix.rotate90();
     * // Result: [[3.0, 1.0], [4.0, 2.0]]
     * r.get(0, 0);                 // returns 3.0
     * r.get(0, 1);                 // returns 1.0
     *
     * // Dimensions swap for non-square matrices:
     * DoubleMatrix wide = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}});
     * DoubleMatrix tall = wide.rotate90();
     * tall.rowCount();             // returns 3
     * tall.columnCount();          // returns 1
     *
     * DoubleMatrix empty = DoubleMatrix.empty().rotate90();
     * empty.isEmpty();             // returns true
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 90 degrees clockwise
     * @throws IllegalArgumentException if the resulting (transposed) shape is not representable
     */
    @Override
    public DoubleMatrix rotate90() {
        if (columnCount == 0) {
            return EMPTY_DOUBLE_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final double[][] c = new double[columnCount][rowCount];

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

        return new DoubleMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 180 degrees.
     * This is equivalent to flipping both horizontally and vertically, reversing the
     * order of all elements. The resulting matrix has the same dimensions as the original.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix r = matrix.rotate180();
     * // Result: [[4.0, 3.0], [2.0, 1.0]]
     * r.get(0, 0);                 // returns 4.0
     * r.get(1, 1);                 // returns 1.0
     * r.rowCount();                // returns 2 (dimensions unchanged)
     *
     * DoubleMatrix empty = DoubleMatrix.empty().rotate180();
     * empty.isEmpty();             // returns true
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 180 degrees
     */
    @Override
    public DoubleMatrix rotate180() {
        final double[][] c = new double[rowCount][];

        for (int i = 0; i < rowCount; i++) {
            c[i] = a[rowCount - i - 1].clone();
            N.reverse(c[i]);
        }

        return new DoubleMatrix(c);
    }

    /**
     * Returns a new matrix that is this matrix rotated 270 degrees clockwise.
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * column of the result being the first row of the original, reading downward.
     * The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix r = matrix.rotate270();
     * // Result: [[2.0, 4.0], [1.0, 3.0]]
     * r.get(0, 0);                 // returns 2.0
     * r.get(1, 0);                 // returns 1.0
     *
     * // Dimensions swap for non-square matrices:
     * DoubleMatrix wide = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}});
     * DoubleMatrix tall = wide.rotate270();
     * tall.rowCount();             // returns 3
     * tall.columnCount();          // returns 1
     *
     * DoubleMatrix empty = DoubleMatrix.empty().rotate270();
     * empty.isEmpty();             // returns true
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 270 degrees clockwise
     * @throws IllegalArgumentException if the resulting (transposed) shape is not representable
     */
    @Override
    public DoubleMatrix rotate270() {
        if (columnCount == 0) {
            return EMPTY_DOUBLE_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final double[][] c = new double[columnCount][rowCount];

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

        return new DoubleMatrix(c);
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * DoubleMatrix transposed = matrix.transpose();   // 2x3 becomes 3x2
     * transposed.rowCount();                          // returns 3
     * transposed.columnCount();                       // returns 2
     * transposed.get(0, 1);                           // returns 4.0 (was at (1,0))
     * transposed.get(2, 0);                           // returns 3.0 (was at (0,2))
     *
     * DoubleMatrix empty = DoubleMatrix.empty().transpose();
     * empty.isEmpty();             // returns true
     * }</pre>
     *
     * @return a new matrix that is the transpose of this matrix with dimensions columnCount × rowCount;
     *         an {@code N x 0} matrix transposes to the empty {@code 0 x 0} matrix, because the swapped shape
     *         {@code 0 x N} (zero rows with a non-zero column count) is not representable
     * @throws IllegalArgumentException if the resulting (transposed) shape is not representable
     */
    @Override
    public DoubleMatrix transpose() {
        if (columnCount == 0) {
            return EMPTY_DOUBLE_MATRIX;
        }

        checkRepresentableShape(columnCount, rowCount);

        final double[][] c = new double[columnCount][rowCount];

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

        return new DoubleMatrix(c);
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * DoubleMatrix reshaped = matrix.reshape(3, 2);   // [[1.0, 2.0], [3.0, 4.0], [5.0, 6.0]]
     * reshaped.get(2, 1);                             // returns 6.0
     * reshaped.rowCount();                            // returns 3
     *
     * DoubleMatrix extended = matrix.reshape(2, 4);   // [[1.0, 2.0, 3.0, 4.0], [5.0, 6.0, 0.0, 0.0]]
     * extended.get(1, 3);                             // returns 0.0 (padded slot)
     *
     * matrix.reshape(1, 3);        // throws IllegalArgumentException (too small for 6 elements)
     * matrix.reshape(-1, 6);       // throws IllegalArgumentException (negative dimension)
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix; must be {@code >= 0}
     * @param newColumnCount the number of columns in the reshaped matrix; must be {@code >= 0}
     * @return a new DoubleMatrix with the specified shape containing this matrix's elements
     * @throws IllegalArgumentException if either dimension is negative,
     *         if the resulting shape is not representable (zero rows with a non-zero column count),
     *         or if the new shape is too small to hold all elements
     */
    @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
    @Override
    public DoubleMatrix reshape(final int newRowCount, final int newColumnCount) {
        N.checkArgument(newRowCount >= 0, MSG_NEGATIVE_DIMENSION, "newRowCount", newRowCount);
        N.checkArgument(newColumnCount >= 0, MSG_NEGATIVE_DIMENSION, "newColumnCount", newColumnCount);
        checkRepresentableShape(newRowCount, newColumnCount);
        checkMaterializableShape(newRowCount, newColumnCount);
        N.checkArgument((long) newRowCount * newColumnCount >= elementCount(), "New shape [{}x{}={}] is too small to hold all {} elements", newRowCount,
                newColumnCount, (long) newRowCount * newColumnCount, elementCount());

        final double[][] c = new double[newRowCount][newColumnCount];

        if (newRowCount == 0 || newColumnCount == 0 || N.isEmpty(a)) {
            return new DoubleMatrix(c);
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

        return new DoubleMatrix(c);
    }

    /**
     * Repeats elements of the matrix in both row and column directions.
     * Each element is repeated {@code rowRepeats} times vertically and {@code columnRepeats} times horizontally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix repeated = matrix.repeatElements(2, 3);
     * // Result: [[1.0, 1.0, 1.0, 2.0, 2.0, 2.0],
     * //          [1.0, 1.0, 1.0, 2.0, 2.0, 2.0],
     * //          [3.0, 3.0, 3.0, 4.0, 4.0, 4.0],
     * //          [3.0, 3.0, 3.0, 4.0, 4.0, 4.0]]
     * repeated.rowCount();         // returns 4
     * repeated.columnCount();      // returns 6
     * repeated.get(0, 2);          // returns 1.0
     * repeated.get(2, 3);          // returns 4.0
     *
     * matrix.repeatElements(0, 3); // throws IllegalArgumentException (rowRepeats not positive)
     * }</pre>
     *
     * @param rowRepeats the number of times each element is duplicated vertically (along the row axis);
     *                   must be positive
     * @param columnRepeats the number of times each element is duplicated horizontally (along the column axis);
     *                      must be positive
     * @return a new matrix with dimensions {@code (rowCount * rowRepeats) x (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#repeatElements(int, int)
     */
    @Override
    public DoubleMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final double[][] c = new double[rowCount * rowRepeats][columnCount * columnRepeats];

        for (int i = 0; i < rowCount; i++) {
            final double[] aa = a[i];
            final double[] fr = c[i * rowRepeats];

            for (int j = 0; j < columnCount; j++) {
                N.copy(Array.repeat(aa[j], columnRepeats), 0, fr, j * columnRepeats, columnRepeats);
            }

            for (int k = 1; k < rowRepeats; k++) {
                N.copy(fr, 0, c[i * rowRepeats + k], 0, fr.length);
            }
        }

        return new DoubleMatrix(c);
    }

    /**
     * Repeats the entire matrix in both row and column directions.
     * The matrix is tiled {@code rowRepeats} times vertically and {@code columnRepeats} times horizontally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix tiled = matrix.repeatMatrix(2, 3);
     * // Result: [[1.0, 2.0, 1.0, 2.0, 1.0, 2.0],
     * //          [3.0, 4.0, 3.0, 4.0, 3.0, 4.0],
     * //          [1.0, 2.0, 1.0, 2.0, 1.0, 2.0],
     * //          [3.0, 4.0, 3.0, 4.0, 3.0, 4.0]]
     * tiled.rowCount();            // returns 4
     * tiled.columnCount();         // returns 6
     * tiled.get(0, 2);             // returns 1.0 (start of second tile)
     * tiled.get(3, 5);             // returns 4.0
     *
     * matrix.repeatMatrix(2, 0);   // throws IllegalArgumentException (columnRepeats not positive)
     * }</pre>
     *
     * @param rowRepeats the number of times to tile the whole matrix vertically (along the row axis);
     *                   must be positive
     * @param columnRepeats the number of times to tile the whole matrix horizontally (along the column axis);
     *                      must be positive
     * @return a new matrix with dimensions {@code (rowCount * rowRepeats) x (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#repeatMatrix(int, int)
     */
    @Override
    public DoubleMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException {
        N.checkArgument(rowRepeats > 0 && columnRepeats > 0, MSG_REPEATS_NOT_POSITIVE, rowRepeats, columnRepeats);

        // Check for overflow before allocation
        if ((long) rowCount * rowRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result row count overflow: " + rowCount + " * " + rowRepeats + " exceeds Integer.MAX_VALUE");
        }
        if ((long) columnCount * columnRepeats > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Result column count overflow: " + columnCount + " * " + columnRepeats + " exceeds Integer.MAX_VALUE");
        }

        final double[][] c = new double[rowCount * rowRepeats][columnCount * columnRepeats];

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

        return new DoubleMatrix(c);
    }

    /**
     * Returns a DoubleList containing all matrix elements in row-major order (left-to-right, top-to-bottom).
     * The matrix is flattened into a single-dimensional list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleList list = matrix.flatten();   // DoubleList [1.0, 2.0, 3.0, 4.0]
     * list.size();                          // returns 4
     * list.get(2);                          // returns 3.0
     *
     * DoubleList emptyList = DoubleMatrix.empty().flatten();
     * emptyList.size();            // returns 0
     * }</pre>
     *
     * @return a DoubleList containing all elements in row-major order
     * @throws IllegalStateException if the matrix is too large to flatten (more than Integer.MAX_VALUE elements)
     */
    @Override
    public DoubleList flatten() {
        // Check for overflow before allocation
        if ((long) rowCount * columnCount > Integer.MAX_VALUE) {
            throw new IllegalStateException("Matrix too large to flatten: " + rowCount + " x " + columnCount);
        }

        final double[] c = new double[rowCount * columnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c, i * columnCount, columnCount);
        }

        return DoubleList.of(c);
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{5.0, 3.0}, {4.0, 1.0}});
     * matrix.mutateAsFlat(arr -> java.util.Arrays.sort(arr));
     * // all elements sorted globally, then placed back row by row
     * matrix.get(0, 0);            // returns 1.0
     * matrix.get(1, 1);            // returns 5.0
     *
     * DoubleMatrix doubled = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * doubled.mutateAsFlat(arr -> { for (int i = 0; i < arr.length; i++) arr[i] *= 2; });
     * doubled.get(1, 1);           // returns 8.0
     * }</pre>
     *
     * @param <E> the type of exception that the operation may throw
     * @param action the operation to apply to the flattened array
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the operation throws an exception
     * @see Arrays#mutateAsFlat(double[][], Throwables.Consumer)
     */
    @Override
    public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super double[], E> action) throws E {
        N.checkArgNotNull(action, "action");

        Arrays.mutateAsFlat(a, action);
    }

    /**
     * Vertically stacks this matrix with another matrix.
     * The matrices must have the same number of columns.
     * The result is a new matrix with rows from this matrix followed by rows from the other matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix a = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix b = DoubleMatrix.of(new double[][] {{5.0, 6.0}});
     * DoubleMatrix stacked = a.stackVertically(b);
     * // Result: [[1.0, 2.0], [3.0, 4.0], [5.0, 6.0]]
     * stacked.rowCount();          // returns 3
     * stacked.get(2, 1);           // returns 6.0
     *
     * DoubleMatrix mismatch = DoubleMatrix.of(new double[][] {{7.0, 8.0, 9.0}});
     * a.stackVertically(mismatch);            // throws IllegalArgumentException (column count differs)
     * a.stackVertically((DoubleMatrix) null); // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * @param other the matrix to stack below this matrix; must not be {@code null}
     * @return a new matrix with combined rows
     * @throws IllegalArgumentException if {@code other} is {@code null}, the matrices have different
     *         number of columns, or the merged row count would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#stackVertically(IntMatrix)
     */
    @Override
    public DoubleMatrix stackVertically(final DoubleMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.columnCount, MSG_VSTACK_COLUMN_MISMATCH, columnCount, other.columnCount);
        final long mergedRowCount = (long) rowCount + other.rowCount;
        N.checkArgument(mergedRowCount <= Integer.MAX_VALUE, "Merged row count overflow: {} + {} = {}", rowCount, other.rowCount, mergedRowCount);

        final double[][] c = new double[(int) mergedRowCount][];
        int j = 0;

        for (int i = 0; i < rowCount; i++) {
            c[j++] = a[i].clone();
        }

        for (int i = 0; i < other.rowCount; i++) {
            c[j++] = other.a[i].clone();
        }

        return DoubleMatrix.of(c);
    }

    /**
     * Horizontally stacks this matrix with another matrix.
     * The matrices must have the same number of rows.
     * The result is a new matrix with columns from this matrix followed by columns from the other matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix a = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix b = DoubleMatrix.of(new double[][] {{5.0}, {6.0}});
     * DoubleMatrix stacked = a.stackHorizontally(b);
     * // Result: [[1.0, 2.0, 5.0], [3.0, 4.0, 6.0]]
     * stacked.columnCount();       // returns 3
     * stacked.get(1, 2);           // returns 6.0
     *
     * DoubleMatrix mismatch = DoubleMatrix.of(new double[][] {{5.0}});
     * a.stackHorizontally(mismatch);            // throws IllegalArgumentException (row count differs)
     * a.stackHorizontally((DoubleMatrix) null); // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * @param other the matrix to stack to the right of this matrix; must not be {@code null}
     * @return a new matrix with combined columns
     * @throws IllegalArgumentException if {@code other} is {@code null}, the matrices have different
     *         number of rows, or the merged column count would overflow {@code Integer.MAX_VALUE}
     * @see IntMatrix#stackHorizontally(IntMatrix)
     */
    @Override
    public DoubleMatrix stackHorizontally(final DoubleMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(rowCount == other.rowCount, MSG_HSTACK_ROW_MISMATCH, rowCount, other.rowCount);
        final long mergedColumnCount = (long) columnCount + other.columnCount;
        N.checkArgument(mergedColumnCount <= Integer.MAX_VALUE, "Merged column count overflow: {} + {} = {}", columnCount, other.columnCount,
                mergedColumnCount);

        final double[][] c = new double[rowCount][(int) mergedColumnCount];

        for (int i = 0; i < rowCount; i++) {
            N.copy(a[i], 0, c[i], 0, columnCount);
            N.copy(other.a[i], 0, c[i], columnCount, other.columnCount);
        }

        return DoubleMatrix.of(c);
    }

    /**
     * Performs element-wise addition of this matrix with another matrix.
     * The matrices must have the same dimensions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix a = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix b = DoubleMatrix.of(new double[][] {{5.0, 6.0}, {7.0, 8.0}});
     * DoubleMatrix sum = a.add(b);
     * sum.get(0, 0);               // returns 6.0
     * sum.get(1, 1);               // returns 12.0
     *
     * // +Infinity + -Infinity yields NaN (no exception):
     * DoubleMatrix p = DoubleMatrix.of(new double[][] {{Double.POSITIVE_INFINITY}});
     * DoubleMatrix n = DoubleMatrix.of(new double[][] {{Double.NEGATIVE_INFINITY}});
     * Double.isNaN(p.add(n).get(0, 0)); // returns true
     *
     * DoubleMatrix mismatch = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}});
     * a.add(mismatch);             // throws IllegalArgumentException (different shapes)
     * a.add((DoubleMatrix) null);  // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * <p><b>Floating-point notes:</b> Adding {@code +Infinity} and {@code -Infinity} produces
     * {@code NaN}. If either operand is {@code NaN}, the result at that position is {@code NaN}.
     * No exception is thrown for these cases.</p>
     *
     * @param other the matrix to add to this matrix; must not be {@code null}
     * @return a new matrix containing the element-wise sum
     * @throws IllegalArgumentException if {@code other} is {@code null} or the matrices have different dimensions
     */
    public DoubleMatrix add(final DoubleMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot add matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final double[][] otherData = other.a;
        final double[][] result = new double[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = a[i][j] + otherData[i][j];

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return DoubleMatrix.of(result);
    }

    /**
     * Performs element-wise subtraction of another matrix from this matrix.
     * The matrices must have the same dimensions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix a = DoubleMatrix.of(new double[][] {{5.0, 6.0}, {7.0, 8.0}});
     * DoubleMatrix b = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix diff = a.subtract(b);
     * diff.get(0, 0);              // returns 4.0
     * diff.get(1, 1);              // returns 4.0
     *
     * // +Infinity - +Infinity yields NaN (no exception):
     * DoubleMatrix inf = DoubleMatrix.of(new double[][] {{Double.POSITIVE_INFINITY}});
     * Double.isNaN(inf.subtract(inf).get(0, 0)); // returns true
     *
     * DoubleMatrix mismatch = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}});
     * a.subtract(mismatch);            // throws IllegalArgumentException (different shapes)
     * a.subtract((DoubleMatrix) null); // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * <p><b>Floating-point notes:</b> Subtracting {@code +Infinity} from {@code +Infinity}
     * (or {@code -Infinity} from {@code -Infinity}) produces {@code NaN}. If either operand is
     * {@code NaN}, the result at that position is {@code NaN}. No exception is thrown for these cases.</p>
     *
     * @param other the matrix to subtract from this matrix; must not be {@code null}
     * @return a new matrix containing the element-wise difference
     * @throws IllegalArgumentException if {@code other} is {@code null} or the matrices have different dimensions
     */
    public DoubleMatrix subtract(final DoubleMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(Matrices.isSameShape(this, other), "Cannot subtract matrices with different shapes: this is {}x{} but other is {}x{}", rowCount,
                columnCount, other.rowCount, other.columnCount);

        final double[][] otherData = other.a;
        final double[][] result = new double[rowCount][columnCount];
        final Throwables.IntBiConsumer<RuntimeException> elementAction = (i, j) -> result[i][j] = a[i][j] - otherData[i][j];

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return DoubleMatrix.of(result);
    }

    /**
     * Performs matrix multiplication (Cayley product) with another matrix.
     * The number of columns in this matrix must equal the number of rows in the other matrix.
     * Results in a matrix of dimensions (this.rowCount × other.columnCount).
     *
     * <p><b>Note:</b> This is the linear-algebra matrix product, not element-wise multiplication.
     * For element-wise multiplication use
     * {@link #zipWith(DoubleMatrix, com.landawn.abacus.util.Throwables.DoubleBinaryOperator)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix a = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix b = DoubleMatrix.of(new double[][] {{5.0, 6.0}, {7.0, 8.0}});
     * DoubleMatrix product = a.matmul(b);
     * product.get(0, 0);           // returns 19.0 (1*5 + 2*7)
     * product.get(1, 1);           // returns 50.0 (3*6 + 4*8)
     *
     * // Non-square product: (2x3) * (3x1) -> (2x1)
     * DoubleMatrix m = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * DoubleMatrix v = DoubleMatrix.of(new double[][] {{1.0}, {1.0}, {1.0}});
     * DoubleMatrix mv = m.matmul(v);
     * mv.get(0, 0);                // returns 6.0
     * mv.columnCount();            // returns 1
     *
     * DoubleMatrix bad = DoubleMatrix.of(new double[][] {{1.0, 2.0}}); // 1x2, rowCount 1 != a.columnCount 2
     * a.matmul(bad);                                                   // throws IllegalArgumentException (this.columnCount != other.rowCount)
     * a.matmul((DoubleMatrix) null);                                   // throws IllegalArgumentException (null other)
     * }</pre>
     *
     * <p><b>Floating-point notes:</b> Standard IEEE-754 arithmetic applies; {@code NaN} or
     * {@code Infinity} operands propagate into the corresponding result cells, and intermediate
     * sums of {@code +Infinity} and {@code -Infinity} produce {@code NaN}. No exception is
     * thrown for these cases.</p>
     *
     * @param other the matrix to multiply with this matrix; must not be {@code null}
     * @return a new matrix containing the matrix product with dimensions (this.rowCount × other.columnCount)
     * @throws IllegalArgumentException if {@code other} is {@code null} or the matrix dimensions are incompatible for multiplication
     *         (i.e., this.columnCount != other.rowCount), or if this matrix has zero rows while {@code other} has a
     *         non-zero column count (the resulting shape is not representable)
     */
    public DoubleMatrix matmul(final DoubleMatrix other) throws IllegalArgumentException {
        N.checkArgNotNull(other, "other");
        N.checkArgument(columnCount == other.rowCount,
                "Matrix dimensions incompatible for multiplication: this is {}x{}, other is {}x{} (this.columnCount must equal other.rowCount)", rowCount,
                columnCount, other.rowCount, other.columnCount);

        checkRepresentableShape(rowCount, other.columnCount);

        final double[][] otherData = other.a;
        final double[][] result = new double[rowCount][other.columnCount];
        final Throwables.IntTriConsumer<RuntimeException> multiplyAction = (i, j, k) -> result[i][j] += a[i][k] * otherData[k][j];

        Matrices.forEachCartesianIndices(this, other, multiplyAction);

        return DoubleMatrix.of(result);
    }

    /**
     * Converts this primitive double matrix to a boxed Double matrix.
     * Each primitive double value is boxed into a Double object. The original matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix primitive = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * Matrix<Double> boxed = primitive.boxed();
     * boxed.get(0, 1);             // returns Double 2.0
     * boxed.rowCount();            // returns 2
     *
     * Matrix<Double> emptyBoxed = DoubleMatrix.empty().boxed();
     * emptyBoxed.isEmpty();        // returns true
     * }</pre>
     *
     * @return a new {@code Matrix<Double>} containing boxed Double values (same dimensions as the original)
     * @see #unbox(Matrix)
     */
    public Matrix<Double> boxed() {
        final Double[][] c = new Double[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final double[] aa = a[i];
                final Double[] cc = c[i];

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
     * Converts this double matrix to an int matrix.
     * Each double value is narrowed to int by Java's double-to-int casting rules:
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
     * DoubleMatrix doubleMatrix = DoubleMatrix.of(new double[][] {{1.9, 2.1}, {3.5, 4.0}});
     * IntMatrix intMatrix = doubleMatrix.toIntMatrix();
     * intMatrix.get(0, 0);         // returns 1 (1.9 truncated toward zero)
     * intMatrix.get(1, 0);         // returns 3 (3.5 truncated toward zero)
     *
     * // Special values saturate / map per double-to-int casting:
     * DoubleMatrix special = DoubleMatrix.of(new double[][] {{Double.NaN, Double.POSITIVE_INFINITY}});
     * IntMatrix s = special.toIntMatrix();
     * s.get(0, 0);                 // returns 0 (NaN -> 0)
     * s.get(0, 1);                 // returns Integer.MAX_VALUE (+Infinity saturates)
     * }</pre>
     *
     * @return a new {@code IntMatrix} with values converted from double to int
     */
    public IntMatrix toIntMatrix() {
        final int[][] c = new int[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final double[] aa = a[i];
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
     * Converts this double matrix to a long matrix.
     * Each double value is narrowed to long by Java's double-to-long casting rules:
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
     * DoubleMatrix doubleMatrix = DoubleMatrix.of(new double[][] {{1.9, 2.1}, {3.5, 4.0}});
     * LongMatrix longMatrix = doubleMatrix.toLongMatrix();
     * longMatrix.get(0, 0);        // returns 1L (1.9 truncated toward zero)
     * longMatrix.get(1, 0);        // returns 3L (3.5 truncated toward zero)
     *
     * // Special values saturate / map per double-to-long casting:
     * DoubleMatrix special = DoubleMatrix.of(new double[][] {{Double.NaN, Double.NEGATIVE_INFINITY}});
     * LongMatrix s = special.toLongMatrix();
     * s.get(0, 0);                 // returns 0L (NaN -> 0)
     * s.get(0, 1);                 // returns Long.MIN_VALUE (-Infinity saturates)
     * }</pre>
     *
     * @return a new {@code LongMatrix} with values converted from double to long
     */
    public LongMatrix toLongMatrix() {
        final long[][] c = new long[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final double[] aa = a[i];
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
     * Converts this double matrix to a float matrix.
     * Each double value is narrowed to float by casting.
     *
     * <p><b>Warning:</b> This is a narrowing conversion that may lose precision.
     * Double values that cannot be exactly represented as float will be rounded
     * to the nearest float value. Values whose magnitude exceeds {@link Float#MAX_VALUE}
     * become {@code Float.POSITIVE_INFINITY} or {@code Float.NEGATIVE_INFINITY},
     * and {@code NaN} doubles remain {@code NaN}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix doubleMatrix = DoubleMatrix.of(new double[][] {{1.5, 2.5}, {3.0, 4.0}});
     * FloatMatrix floatMatrix = doubleMatrix.toFloatMatrix();
     * floatMatrix.get(0, 0);       // returns 1.5f
     * floatMatrix.get(1, 1);       // returns 4.0f
     *
     * // Magnitudes beyond Float.MAX_VALUE overflow to Infinity:
     * DoubleMatrix big = DoubleMatrix.of(new double[][] {{Double.MAX_VALUE}});
     * big.toFloatMatrix().get(0, 0) == Float.POSITIVE_INFINITY; // returns true
     * }</pre>
     *
     * @return a new {@code FloatMatrix} with values converted from double to float
     */
    public FloatMatrix toFloatMatrix() {
        final float[][] c = new float[rowCount][columnCount];

        if (rowCount <= columnCount) {
            for (int i = 0; i < rowCount; i++) {
                final double[] aa = a[i];
                final float[] cc = c[i];

                for (int j = 0; j < columnCount; j++) {
                    cc[j] = (float) aa[j];
                }
            }
        } else {
            for (int j = 0; j < columnCount; j++) {
                for (int i = 0; i < rowCount; i++) {
                    c[i][j] = (float) a[i][j];
                }
            }
        }

        return new FloatMatrix(c);
    }

    /**
     * Applies a binary operation element-wise to this matrix and another matrix.
     * The matrices must have the same dimensions. The original matrices are not modified.
     * Each pair of corresponding elements from the two matrices is combined using the zip function,
     * and the results are collected into a new matrix. The operation may be performed in parallel
     * for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix a = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix b = DoubleMatrix.of(new double[][] {{5.0, 6.0}, {7.0, 8.0}});
     * DoubleMatrix max = a.zipWith(b, Math::max);
     * max.get(0, 0);               // returns 5.0
     * max.get(1, 1);               // returns 8.0
     *
     * DoubleMatrix product = a.zipWith(b, (x, y) -> x * y); // element-wise product
     * product.get(1, 1);                                    // returns 32.0
     *
     * DoubleMatrix mismatch = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}});
     * a.zipWith(mismatch, Math::max); // throws IllegalArgumentException (different shapes)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the matrix to combine with this matrix; must have the same dimensions and must not be {@code null}
     * @param zipFunction the binary operation to apply to corresponding elements; must not be {@code null}
     * @return a new matrix with the operation applied element-wise (same dimensions as the input matrices)
     * @throws IllegalArgumentException if {@code other} or {@code zipFunction} is {@code null}, or the matrices have different dimensions
     * @throws E if the zip function throws an exception
     */
    public <E extends Exception> DoubleMatrix zipWith(final DoubleMatrix other, final Throwables.DoubleBinaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other), "Cannot zip matrices with different shapes: this is {}x{} but other is {}x{}", rowCount, columnCount,
                other.rowCount, other.columnCount);

        final double[][] otherData = other.a;
        final double[][] result = new double[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsDouble(a[i][j], otherData[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return DoubleMatrix.of(result);
    }

    /**
     * Applies a ternary operation element-wise to this matrix and two other matrices.
     * All three matrices must have the same dimensions. The original matrices are not modified.
     * Each triplet of corresponding elements from the three matrices is combined using the zip function,
     * and the results are collected into a new matrix. The operation may be performed in parallel
     * for large matrices to improve performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix a = DoubleMatrix.of(new double[][] {{1.0, 2.0}});
     * DoubleMatrix b = DoubleMatrix.of(new double[][] {{3.0, 4.0}});
     * DoubleMatrix c = DoubleMatrix.of(new double[][] {{5.0, 6.0}});
     * DoubleMatrix result = a.zipWith(b, c, (x, y, z) -> x + y * z);
     * result.get(0, 0);            // returns 16.0 (1 + 3*5)
     * result.get(0, 1);            // returns 26.0 (2 + 4*6)
     *
     * DoubleMatrix sum = a.zipWith(b, c, (x, y, z) -> x + y + z);
     * sum.get(0, 0);               // returns 9.0
     *
     * DoubleMatrix mismatch = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}});
     * a.zipWith(b, mismatch, (x, y, z) -> x); // throws IllegalArgumentException (different shapes)
     * }</pre>
     *
     * @param <E> the type of exception that the zip function may throw
     * @param other the second matrix to combine; must have the same dimensions and must not be {@code null}
     * @param third the third matrix to combine; must have the same dimensions and must not be {@code null}
     * @param zipFunction the ternary operation to apply to corresponding elements; must not be {@code null}
     * @return a new matrix with the operation applied element-wise (same dimensions as the input matrices)
     * @throws IllegalArgumentException if {@code other}, {@code third}, or {@code zipFunction} is {@code null}, or the matrices have different dimensions
     * @throws E if the zip function throws an exception
     */
    public <E extends Exception> DoubleMatrix zipWith(final DoubleMatrix other, final DoubleMatrix third, final Throwables.DoubleTernaryOperator<E> zipFunction)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(other, "other");
        N.checkArgNotNull(third, "third");
        N.checkArgNotNull(zipFunction, "zipFunction");
        N.checkArgument(isSameShape(other) && isSameShape(third), "Cannot zip matrices with different shapes: all matrices must be {}x{}", rowCount,
                columnCount);

        final double[][] otherData = other.a;
        final double[][] thirdData = third.a;
        final double[][] result = new double[rowCount][columnCount];

        final Throwables.IntBiConsumer<E> elementAction = (i, j) -> result[i][j] = zipFunction.applyAsDouble(a[i][j], otherData[i][j], thirdData[i][j]);

        Matrices.forEachIndices(rowCount, columnCount, elementAction, Matrices.isParallelizable(this));

        return DoubleMatrix.of(result);
    }

    /**
     * Returns a stream of elements from the main diagonal (upper-left to lower-right).
     * The matrix must be square (same number of rows and columns).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.mainDiagonalStream().toArray();   // returns [1.0, 4.0]
     * matrix.mainDiagonalStream().sum();       // returns 5.0
     *
     * DoubleMatrix empty = DoubleMatrix.empty();
     * empty.mainDiagonalStream().count();      // returns 0 (empty allowed)
     *
     * DoubleMatrix rect = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * rect.mainDiagonalStream();               // throws IllegalStateException (non-square)
     * }</pre>
     *
     * @return a DoubleStream of diagonal elements from upper-left to lower-right
     * @throws IllegalStateException if the matrix is not square
     */
    @Override
    public DoubleStream mainDiagonalStream() {
        checkIsSquare();

        if (isEmpty()) {
            return DoubleStream.empty();
        }

        return DoubleStream.of(new DoubleIteratorEx() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public double nextDouble() {
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
     * Returns a stream of elements from the anti-diagonal (upper-right to lower-left).
     * The matrix must be square (same number of rows and columns).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.antiDiagonalStream().toArray();   // returns [2.0, 3.0]
     * matrix.antiDiagonalStream().sum();       // returns 5.0
     *
     * DoubleMatrix empty = DoubleMatrix.empty();
     * empty.antiDiagonalStream().count();      // returns 0 (empty allowed)
     *
     * DoubleMatrix rect = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * rect.antiDiagonalStream();               // throws IllegalStateException (non-square)
     * }</pre>
     *
     * @return a DoubleStream of diagonal elements from upper-right to lower-left
     * @throws IllegalStateException if the matrix is not square
     */
    @Override
    public DoubleStream antiDiagonalStream() {
        checkIsSquare();

        if (isEmpty()) {
            return DoubleStream.empty();
        }

        return DoubleStream.of(new DoubleIteratorEx() {
            private final int toIndex = rowCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public double nextDouble() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final double result = a[cursor][columnCount - cursor - 1];
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
     *
     * <p>This method is useful for processing all matrix elements sequentially. The returned
     * stream can be used with all standard DoubleStream operations including sum, average, filter, map, etc.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.horizontalStream().sum();         // returns 10.0
     * matrix.horizontalStream().toArray();     // returns [1.0, 2.0, 3.0, 4.0]
     * matrix.horizontalStream().count();       // returns 4
     *
     * DoubleMatrix empty = DoubleMatrix.empty();
     * empty.horizontalStream().count();        // returns 0 (empty stream)
     * }</pre>
     *
     * @return a DoubleStream of all matrix elements in row-major order, or an empty stream if the matrix is empty
     */
    @Override
    public DoubleStream horizontalStream() {
        return horizontalStream(0, rowCount);
    }

    /**
     * Returns a stream of elements from a single row.
     * The elements are streamed from left to right within the specified row.
     *
     * <p>This method is particularly useful when you need to process or analyze
     * a specific row of the matrix independently. The returned stream can be
     * used with all standard DoubleStream operations.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.horizontalStream(1).toArray();    // returns [3.0, 4.0]
     * matrix.horizontalStream(1).sum();        // returns 7.0
     * matrix.horizontalStream(0).toArray();    // returns [1.0, 2.0]
     *
     * matrix.horizontalStream(5);              // throws IndexOutOfBoundsException (row out of range)
     * }</pre>
     *
     * @param rowIndex the index of the row to stream (0-based)
     * @return a DoubleStream of elements in the specified row, from left to right
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    @Override
    public DoubleStream horizontalStream(final int rowIndex) {
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}, {5.0, 6.0}});
     * matrix.horizontalStream(1, 3).toArray();   // returns [3.0, 4.0, 5.0, 6.0]
     * matrix.horizontalStream(0, 2).toArray();   // returns [1.0, 2.0, 3.0, 4.0]
     * matrix.horizontalStream(1, 1).count();     // returns 0 (empty range)
     *
     * matrix.horizontalStream(0, 5);             // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a DoubleStream of elements in the specified row range, or an empty stream if the matrix is empty
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex}
     */
    @Override
    public DoubleStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        if (isEmpty()) {
            return DoubleStream.empty();
        }

        return DoubleStream.of(new DoubleIteratorEx() {
            private int i = fromRowIndex;
            private int j = 0;

            @Override
            public boolean hasNext() {
                return i < toRowIndex;
            }

            @Override
            public double nextDouble() {
                if (i >= toRowIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final double result = a[i][j++];

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
            public double[] toArray() {
                final int len = toArrayLength(count());
                final double[] c = new double[len];

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
     * Creates a stream of all elements in the matrix in column-major order.
     * Elements are streamed from top to bottom, left to right.
     * This method is marked as Beta and may change in future versions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.verticalStream().toArray();       // returns [1.0, 3.0, 2.0, 4.0]
     * matrix.verticalStream().sum();           // returns 10.0
     *
     * DoubleMatrix empty = DoubleMatrix.empty();
     * empty.verticalStream().count();          // returns 0 (empty stream)
     * }</pre>
     *
     * @return a DoubleStream of all matrix elements in column-major order
     */
    @Override
    @Beta
    public DoubleStream verticalStream() {
        return verticalStream(0, columnCount);
    }

    /**
     * Creates a stream of elements from a single column in the matrix.
     * This is equivalent to calling {@code verticalStream(columnIndex, columnIndex + 1)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.verticalStream(1).toArray();      // returns [2.0, 4.0]
     * matrix.verticalStream(0).sum();          // returns 4.0 (1.0 + 3.0)
     *
     * matrix.verticalStream(5);                // throws IndexOutOfBoundsException (column out of range)
     * }</pre>
     *
     * @param columnIndex the index of the column to stream (0-based)
     * @return a DoubleStream of elements in the specified column, from top to bottom
     * @throws IndexOutOfBoundsException if the column index is out of bounds
     */
    @Override
    public DoubleStream verticalStream(final int columnIndex) {
        return verticalStream(columnIndex, columnIndex + 1);
    }

    /**
     * Creates a stream of elements from a range of columns in column-major order.
     * Elements are streamed from top to bottom within each column, then left to right across columns.
     * This method is marked as Beta and may change in future versions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * matrix.verticalStream(1, 3).toArray();   // returns [2.0, 5.0, 3.0, 6.0]
     * matrix.verticalStream(0, 1).toArray();   // returns [1.0, 4.0]
     * matrix.verticalStream(1, 1).count();     // returns 0 (empty range)
     *
     * matrix.verticalStream(0, 9);             // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a DoubleStream of elements in the specified column range
     * @throws IndexOutOfBoundsException if the column indices are out of bounds
     */
    @Override
    @Beta
    public DoubleStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        if (isEmpty()) {
            return DoubleStream.empty();
        }

        return DoubleStream.of(new DoubleIteratorEx() {
            private int i = 0;
            private int j = fromColumnIndex;

            @Override
            public boolean hasNext() {
                return j < toColumnIndex;
            }

            @Override
            public double nextDouble() {
                if (j >= toColumnIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final double result = a[i++][j];

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

                if (n >= (long) (toColumnIndex - j) * DoubleMatrix.this.rowCount - i) {
                    i = 0;
                    j = toColumnIndex;
                } else {
                    final long offset = n + i;
                    i = (int) (offset % DoubleMatrix.this.rowCount);
                    j += (int) (offset / DoubleMatrix.this.rowCount);
                }
            }

            @Override
            public long count() {
                return (long) (toColumnIndex - j) * rowCount - i; // NOSONAR
            }

            @Override
            public double[] toArray() {
                final int len = toArrayLength(count());
                final double[] c = new double[len];

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
     * Creates a stream of streams, where each inner stream represents a complete row of the matrix.
     * This is equivalent to calling {@code rowStreams(0, rowCount)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.rowStreams().count();                    // returns 2 (one stream per row)
     * matrix.rowStreams().map(s -> s.sum()).toList(); // returns [3.0, 7.0]
     *
     * DoubleMatrix empty = DoubleMatrix.empty();
     * empty.rowStreams().count();              // returns 0
     * }</pre>
     *
     * @return a Stream of DoubleStreams, one for each row in the matrix
     */
    @Override
    public Stream<DoubleStream> rowStreams() {
        return rowStreams(0, rowCount);
    }

    /**
     * Creates a stream of streams for a range of rows.
     * Each inner stream represents a complete row of the matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}, {5.0, 6.0}});
     * matrix.rowStreams(1, 3).count();                    // returns 2
     * matrix.rowStreams(1, 3).map(s -> s.sum()).toList(); // returns [7.0, 11.0]
     * matrix.rowStreams(0, 0).count();                    // returns 0 (empty range)
     *
     * matrix.rowStreams(0, 5);                 // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive)
     * @param toRowIndex the ending row index (exclusive)
     * @return a Stream of DoubleStreams for the specified row range
     * @throws IndexOutOfBoundsException if the row indices are out of bounds
     */
    @Override
    public Stream<DoubleStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        return Stream.of(new ObjIteratorEx<>() {
            private final int toIndex = toRowIndex;
            private int cursor = fromRowIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public DoubleStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return DoubleStream.of(a[cursor++]);
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
     * Creates a stream of streams, where each inner stream represents a complete column of the matrix.
     * This method is marked as Beta and may change in future versions.
     * This is equivalent to calling {@code columnStreams(0, columnCount)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.columnStreams().count();                    // returns 2 (one stream per column)
     * matrix.columnStreams().map(s -> s.sum()).toList(); // returns [4.0, 6.0]
     *
     * DoubleMatrix empty = DoubleMatrix.empty();
     * empty.columnStreams().count();           // returns 0
     * }</pre>
     *
     * @return a Stream of DoubleStreams, one for each column in the matrix
     */
    @Override
    @Beta
    public Stream<DoubleStream> columnStreams() {
        return columnStreams(0, columnCount);
    }

    /**
     * Creates a stream of streams for a range of columns.
     * Each inner stream represents a complete column of the matrix.
     * This method is marked as Beta and may change in future versions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * matrix.columnStreams(1, 3).count();                    // returns 2
     * matrix.columnStreams(1, 3).map(s -> s.sum()).toList(); // returns [7.0, 9.0]
     * matrix.columnStreams(1, 1).count();                    // returns 0 (empty range)
     *
     * matrix.columnStreams(0, 9);              // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a Stream of DoubleStreams for the specified column range
     * @throws IndexOutOfBoundsException if the column indices are out of bounds
     */
    @Override
    @Beta
    public Stream<DoubleStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
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
            public DoubleStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return DoubleStream.of(new DoubleIteratorEx() {
                    private final int columnIndex = cursor++;
                    private final int toIndex2 = rowCount;
                    private int cursor2 = 0;

                    @Override
                    public boolean hasNext() {
                        return cursor2 < toIndex2;
                    }

                    @Override
                    public double nextDouble() {
                        if (cursor2 >= toIndex2) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return a[cursor2++][columnIndex];
                    }

                    @Override
                    public void advance(final long n) throws IllegalArgumentException {
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
     * This is an internal helper method used by the matrix framework.
     *
     * @param a the array to measure
     * @return the length of the array, or {@code 0} if the array is {@code null}
     */
    @Override
    protected int length(@SuppressWarnings("hiding") final double[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Performs the specified action for each element in this matrix.
     * For large matrices, the operation may be parallelized to improve performance,
     * so the order of execution is not guaranteed. This method does not modify the matrix
     * unless the action itself modifies external state.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * double[] total = {0.0};
     * matrix.forEach(value -> total[0] += value);
     * total[0];                    // returns 10.0 (1.0 + 2.0 + 3.0 + 4.0)
     *
     * int[] count = {0};
     * DoubleMatrix.empty().forEach(value -> count[0]++);
     * count[0];                    // returns 0 (no elements visited)
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to perform on each element; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final Throwables.DoubleConsumer<E> action) throws E {
        forEach(0, rowCount, 0, columnCount, action);
    }

    /**
     * Performs the specified action for each element in the specified sub-region of this matrix.
     * For large sub-regions, the operation may be parallelized to improve performance, so the order
     * of execution is not guaranteed. This method does not modify the matrix unless the action itself
     * modifies external state.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * double[] total = {0.0};
     * matrix.forEach(0, 2, 1, 3, value -> total[0] += value); // columns 1-2 only
     * total[0];                                               // returns 16.0 (2.0 + 3.0 + 5.0 + 6.0)
     *
     * matrix.forEach(0, 5, 0, 3, value -> {}); // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param fromRowIndex the starting row index (inclusive, 0-based, must be {@code >= 0} and {@code <= toRowIndex})
     * @param toRowIndex the ending row index (exclusive, must be {@code >= fromRowIndex} and {@code <= rowCount})
     * @param fromColumnIndex the starting column index (inclusive, 0-based, must be {@code >= 0} and {@code <= toColumnIndex})
     * @param toColumnIndex the ending column index (exclusive, must be {@code >= fromColumnIndex} and {@code <= columnCount})
     * @param action the action to perform on each element in the sub-region; must not be {@code null}
     * @throws IndexOutOfBoundsException if the indices are out of bounds or invalid
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex,
            final Throwables.DoubleConsumer<E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        if (Matrices.isParallelizable(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> action.accept(a[i][j]);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, elementAction, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                final double[] currentRow = a[i];

                for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                    action.accept(currentRow[j]);
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}});
     * String output = matrix.println();
     * // also prints to stdout; the returned string starts with the first row:
     * output.startsWith("[1.0, 2.0, 3.0]"); // returns true
     * output.contains("[4.0, 5.0, 6.0]");   // returns true
     *
     * DoubleMatrix.empty().println();        // returns "[]"
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

                    final double[] row = a[i];
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
     * DoubleMatrix m1 = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix m2 = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * m1.hashCode() == m2.hashCode(); // returns true (equal matrices share a hash code)
     *
     * DoubleMatrix m3 = DoubleMatrix.of(new double[][] {{9.0, 2.0}, {3.0, 4.0}});
     * m1.hashCode() == m3.hashCode(); // typically returns false (different contents)
     *
     * DoubleMatrix.empty().hashCode(); // returns a stable value for the empty matrix
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
     * Returns {@code true} if the given object is also a DoubleMatrix with the same dimensions
     * and all corresponding elements are equal.
     *
     * <p>Element comparison uses {@link Double#doubleToLongBits(double)} semantics
     * (the same rule used by {@link java.util.Arrays#equals(double[], double[])}):
     * {@code NaN} is considered equal to {@code NaN}, and {@code +0.0} is
     * <em>not</em> considered equal to {@code -0.0}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix m1 = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * DoubleMatrix m2 = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * m1.equals(m2);                                           // returns true
     * m1.equals(DoubleMatrix.of(new double[][] {{1.0, 2.0}})); // returns false (different shape)
     * m1.equals("not a matrix");                               // returns false (different type)
     *
     * // doubleToLongBits semantics: NaN equals NaN, +0.0 != -0.0
     * DoubleMatrix nan1 = DoubleMatrix.of(new double[][] {{Double.NaN}});
     * DoubleMatrix nan2 = DoubleMatrix.of(new double[][] {{Double.NaN}});
     * nan1.equals(nan2);           // returns true
     * DoubleMatrix pz = DoubleMatrix.of(new double[][] {{0.0}});
     * DoubleMatrix nz = DoubleMatrix.of(new double[][] {{-0.0}});
     * pz.equals(nz);               // returns false
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

        if (obj instanceof final DoubleMatrix another) {
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
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.toString();                                  // returns "[[1.0, 2.0], [3.0, 4.0]]"
     * DoubleMatrix.empty().toString();                    // returns "[]"
     * DoubleMatrix.of(new double[][] {{5.0}}).toString(); // returns "[[5.0]]"
     * }</pre>
     *
     * @return a string representation of this matrix
     */
    @Override
    public String toString() {
        return N.deepToString(a);
    }
}
