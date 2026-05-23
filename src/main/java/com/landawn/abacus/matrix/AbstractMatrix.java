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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.Stream;

/**
 * Shared implementation base for the matrix types in this package.
 *
 * <p>An {@code AbstractMatrix} wraps a validated rectangular row-array backing structure and
 * centralizes behavior common to both primitive and object matrices, including shape validation,
 * coordinate navigation, row and column access, reshaping, and stream-oriented traversal.</p>
 *
 * <p>Several APIs intentionally expose live storage for performance-sensitive code, notably
 * {@link #internalArray()}, {@link #rowView(int)}, and
 * {@link #mutateAsFlat(Throwables.Consumer)}. Callers that need isolation should prefer
 * copy-producing operations such as {@link #copy()}, {@link #flatten()}, and {@link #rowCopy(int)}.</p>
 *
 * <p>Per-element iteration uses two complementary entry points. The index-only
 * {@link #forEachIndices(Throwables.IntBiConsumer)} is declared here because its action signature
 * is the same for every matrix type. The value-receiving {@code forEach(Consumer)} variant is
 * declared on each concrete subclass instead, because the consumer type is primitive-specialized
 * (for example {@link IntMatrix#forEach(Throwables.IntConsumer)},
 * {@link Matrix#forEach(Throwables.Consumer)}). When iterating by element use the subclass
 * {@code forEach}; when iterating by position use {@code forEachIndices}.</p>
 *
 * @param <A> the array type used for internal row storage (for example {@code int[]}, {@code double[]}, or {@code Object[]})
 * @param <PL> the flattened-view list type
 * @param <ES> the element stream type
 * @param <RS> the row or column stream type
 * @param <M> the concrete matrix type used for fluent return values
 */
public abstract sealed class AbstractMatrix<A, PL, ES, RS, M extends AbstractMatrix<A, PL, ES, RS, M>>
        permits BooleanMatrix, CharMatrix, ByteMatrix, ShortMatrix, DoubleMatrix, FloatMatrix, IntMatrix, LongMatrix, Matrix {

    /**
     * Row separator used when concrete subclasses render the matrix in {@link #println()}.
     * Fixed to the Unix line separator ({@code "\n"}) so that printed output is consistent
     * across platforms.
     */
    protected static final String ARRAY_PRINT_SEPARATOR = IOUtil.LINE_SEPARATOR_UNIX;

    /**
     * Shared random source used by primitive matrix factories that produce randomized data
     * (for example {@code IntMatrix.random(int)}). Backed by {@link SecureRandom} for higher-quality
     * sequences than the default {@link Random}.
     */
    protected static final Random RAND = new SecureRandom();

    static final char CHAR_0 = (char) 0;

    static final byte BYTE_0 = (byte) 0;

    static final byte BYTE_1 = (byte) 1;

    static final short SHORT_0 = (short) 0;

    // ==================== Standardized Exception Message Constants ====================
    // These constants ensure consistent exception messages across all matrix classes.

    /** Exception message format for row index out of bounds. Arguments: rowIndex, rowCount */
    protected static final String MSG_ROW_INDEX_OUT_OF_BOUNDS = "Row index {} is out of bounds. Valid range is [0, {})";

    /** Exception message format for column index out of bounds. Arguments: columnIndex, columnCount */
    protected static final String MSG_COLUMN_INDEX_OUT_OF_BOUNDS = "Column index {} is out of bounds. Valid range is [0, {})";

    /** Exception message format for row length mismatch. Arguments: expected, actual */
    protected static final String MSG_ROW_LENGTH_MISMATCH = "Row length mismatch: expected {} columns but got {}";

    /** Exception message format for column length mismatch. Arguments: expected, actual */
    protected static final String MSG_COLUMN_LENGTH_MISMATCH = "Column length mismatch: expected {} rows but got {}";

    /** Exception message format for diagonal array length mismatch. Arguments: expected, actual */
    protected static final String MSG_DIAGONAL_LENGTH_MISMATCH = "Diagonal array length must equal matrix size: expected {} but got {}";

    /** Exception message format for non-square matrix error. Arguments: rowCount, columnCount */
    protected static final String MSG_MATRIX_NOT_SQUARE = "Matrix must be square: current dimensions are {} rows x {} columns";

    /** Exception message format for shape mismatch between two matrices. Arguments: rows1, cols1, rows2, cols2 */
    protected static final String MSG_SHAPE_MISMATCH = "Matrix shape mismatch: this matrix is {}x{} but other is {}x{}";

    /** Exception message format for non-rectangular matrix. Arguments: firstRowLength, currentRowIndex, currentRowLength */
    protected static final String MSG_NOT_RECTANGULAR = "Matrix must be rectangular: row 0 has {} columns, but row {} has {} columns";

    /** Exception message format for stackVertically column count mismatch. Arguments: thisColumnCount, otherColumnCount */
    protected static final String MSG_VSTACK_COLUMN_MISMATCH = "Column count mismatch for stackVertically: this matrix has {} columns but other has {}";

    /** Exception message format for stackHorizontally row count mismatch. Arguments: thisRowCount, otherRowCount */
    protected static final String MSG_HSTACK_ROW_MISMATCH = "Row count mismatch for stackHorizontally: this matrix has {} rows but other has {}";

    /** Exception message format for negative dimension. Arguments: paramName, value */
    protected static final String MSG_NEGATIVE_DIMENSION = "{} cannot be negative: {}";

    /** Exception message format for non-positive repeats. Arguments: rowRepeats, columnRepeats */
    protected static final String MSG_REPEATS_NOT_POSITIVE = "rowRepeats and columnRepeats must be positive: rowRepeats={}, columnRepeats={}";

    /**
     * Exception message format for matrix shapes that cannot be represented by this row-array-backed implementation.
     * Arguments: rowCount, columnCount.
     */
    protected static final String MSG_UNREPRESENTABLE_SHAPE = "Matrix shape {}x{} is not representable: zero rows require zero columns";

    // ==================== End Exception Message Constants ====================

    /**
     * The number of rows in this matrix.
     * This value is immutable after matrix creation.
     */
    final int rowCount;

    /**
     * The number of columns in this matrix.
     * This value is immutable after matrix creation.
     */
    final int columnCount;

    /**
     * The total number of elements in this matrix (rows × columns).
     * This value is cached for performance and is immutable after matrix creation.
     */
    final long elementCount;

    /**
     * The underlying two-dimensional array storing the matrix data.
     * Direct access to this array should be avoided; use the provided methods instead.
     */
    final A[] a;

    /**
     * The element type of this matrix. For primitive matrices this is the matching
     * primitive class (e.g. {@code int.class}); for {@link Matrix} it is the runtime element class.
     * Read via {@link #elementType()}.
     */
    final Class<?> elementType;

    /**
     * Constructs a new AbstractMatrix with the specified two-dimensional array.
     * The constructor validates that all rows are non-null and have the same length.
     *
     * @param a the two-dimensional array containing matrix data; must not be {@code null}
     * @param elementType the element type of the matrix (e.g. {@code int.class});
     *        must not be {@code null}
     * @throws IllegalArgumentException if {@code a} or {@code elementType} is {@code null},
     *         if any row of {@code a} is {@code null}, or if the rows have different lengths
     *         (i.e. the array is not rectangular)
     */
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    protected AbstractMatrix(final A[] a, final Class<?> elementType) {
        N.checkArgNotNull(a, "Matrix array cannot be null");
        N.checkArgNotNull(elementType, "Element type cannot be null");

        this.a = a;
        this.elementType = elementType;
        rowCount = a.length;

        if (rowCount > 0) {
            N.checkArgument(a[0] != null, "Row 0 cannot be null");
        }

        columnCount = rowCount == 0 ? 0 : length(a[0]);

        if (a.length > 1) {
            for (int i = 1, len = a.length; i < len; i++) {
                N.checkArgument(a[i] != null, "Row {} cannot be null", i);
                if (length(a[i]) != columnCount) {
                    throw new IllegalArgumentException(formatMsg(MSG_NOT_RECTANGULAR, columnCount, i, length(a[i])));
                }
            }
        }

        elementCount = (long) columnCount * rowCount;
    }

    /**
     * Converts a non-negative element count to an {@code int} array length with overflow protection.
     * This utility method ensures that the element count fits within an {@code int} range before
     * converting it, guarding against overflow when materializing streams or arrays from large matrices.
     *
     * @param count the element count to convert (must be non-negative and at most {@code Integer.MAX_VALUE})
     * @return the count cast to an {@code int} array length
     * @throws IllegalStateException if {@code count} is negative or exceeds {@code Integer.MAX_VALUE}
     */
    protected static int toArrayLength(final long count) {
        if (count < 0 || count > Integer.MAX_VALUE) {
            throw new IllegalStateException("Matrix stream too large to convert to array: " + count + " elements");
        }

        return (int) count;
    }

    /**
     * Validates that the specified shape is representable by this matrix implementation.
     * Because dimensions are encoded by row arrays, a matrix with zero rows can only have zero columns.
     *
     * @param rowCount the row count
     * @param columnCount the column count
     * @throws IllegalArgumentException if the shape is not representable
     */
    protected static void checkRepresentableShape(final int rowCount, final int columnCount) {
        N.checkArgument(rowCount > 0 || columnCount == 0, MSG_UNREPRESENTABLE_SHAPE, rowCount, columnCount);
    }

    /**
     * Formats matrix error message templates that use {@code "{}"} placeholders.
     *
     * @param template the message template containing {@code "{}"} placeholders
     * @param args the arguments to substitute into the placeholders
     * @return the formatted message string; the {@code template} is returned unchanged if it is
     *         {@code null}, or if {@code args} is {@code null} or empty. Surplus placeholders that
     *         have no corresponding argument are left in the result as-is.
     */
    protected static String formatMsg(final String template, final Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }

        final StringBuilder sb = new StringBuilder(template.length() + args.length * 8);
        int fromIndex = 0;
        int argIndex = 0;
        int placeholderIndex = 0;

        while (argIndex < args.length && (placeholderIndex = template.indexOf("{}", fromIndex)) >= 0) {
            sb.append(template, fromIndex, placeholderIndex);
            sb.append(args[argIndex++]);
            fromIndex = placeholderIndex + 2;
        }

        sb.append(template, fromIndex, template.length());

        return sb.toString();
    }

    /**
     * Returns the element type of this matrix.
     * For primitive matrices, this returns the corresponding primitive class (e.g., {@code int.class} for {@link IntMatrix}).
     * For object matrices, this returns the element's class type.
     *
     * <p>This method is useful for reflection-based operations and type checking at runtime.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix intMatrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * Class<?> type = intMatrix.elementType();   // Returns int.class
     *
     * Matrix<String> stringMatrix = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * Class<?> strType = stringMatrix.elementType();   // Returns String.class
     * }</pre>
     *
     * @return the Class object representing the element type of this matrix
     */
    public Class<?> elementType() {
        return elementType;
    }

    /**
     * Returns the underlying two-dimensional array of this matrix.
     * This method exposes the internal array representation for performance reasons and should be used with caution
     * as modifications to the returned array will directly affect the matrix.
     *
     * <p><strong>Unsafe API boundary:</strong> This method returns the actual internal array, not a copy.
     * Any changes made to the returned array (including reassigning row references or mutating row contents)
     * will be reflected in this matrix. If you need an independent matrix instance, use {@link #copy()}.
     * If you only need the data flattened into a single one-dimensional array, use {@link #flatten()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * int[][] array = matrix.internalArray();
     * array[0][0] = 10;  // This WILL modify the matrix!
     * // matrix now contains {{10, 2}, {3, 4}}
     * }</pre>
     *
     * @return the underlying two-dimensional array (not a copy); its length equals {@code rowCount}
     *         (so a {@code 0}-row matrix yields a zero-length array, but a {@code rowCount × 0} matrix
     *         yields a {@code rowCount}-length array of zero-length rows)
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public A[] internalArray() {
        return a;
    }

    /**
     * Returns the specified row as a direct view backed by internal storage.
     * Changes to the returned array will modify this matrix.
     *
     * <p><strong>Unsafe API boundary:</strong> the returned row is a mutable alias to internal storage. Prefer
     * {@link #rowCopy(int)} unless you intentionally need to mutate the matrix through the row view.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * int[] row0 = matrix.rowView(0);  // Returns [1, 2, 3] (direct reference)
     * row0[0] = 99;                // Also changes matrix element at (0, 0) to 99
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return the specified row array (direct reference to internal storage)
     * @throws IllegalArgumentException if {@code rowIndex} is negative or {@code >= rowCount}
     */
    public abstract A rowView(int rowIndex) throws IllegalArgumentException;

    /**
     * Returns a defensive copy of the specified row.
     * Changes to the returned array do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * int[] rowCopy = matrix.rowCopy(0);  // Returns [1, 2, 3] (independent copy)
     * rowCopy[0] = 99;                    // Does NOT affect the original matrix
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new array containing the values from the specified row
     * @throws IllegalArgumentException if {@code rowIndex} is negative or {@code >= rowCount}
     */
    public abstract A rowCopy(int rowIndex) throws IllegalArgumentException;

    /**
     * Returns a defensive copy of the specified column.
     * Changes to the returned array do not affect this matrix.
     *
     * <p><b>Note on the rowView/columnView asymmetry:</b> this class exposes
     * {@link #rowView(int)} but no {@code columnView(int)} counterpart. Matrices store
     * elements in row-major form, so a row corresponds to a contiguous slice of the
     * backing storage and can be aliased directly. A column is interleaved across rows
     * and cannot be returned as a live, single-array view without either copying or
     * synthesising a wrapper. {@code columnCopy} is the supported accessor; for
     * iteration without materialising a copy use {@link #verticalStream(int)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * int[] colCopy = matrix.columnCopy(1);  // Returns [2, 5] (independent copy)
     * colCopy[0] = 99;                       // Does NOT affect the original matrix
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new array containing the values from the specified column
     * @throws IllegalArgumentException if {@code columnIndex} is negative or {@code >= columnCount}
     */
    public abstract A columnCopy(int columnIndex) throws IllegalArgumentException;

    /**
     * Returns the number of rows in this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * int rows = matrix.rowCount();   // Returns 2
     * }</pre>
     *
     * @return the number of rows
     */
    public int rowCount() {
        return rowCount;
    }

    /**
     * Returns the number of columns in this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * int cols = matrix.columnCount();   // Returns 3
     * }</pre>
     *
     * @return the number of columns
     */
    public int columnCount() {
        return columnCount;
    }

    /**
     * Returns the total number of elements in this matrix (rows x columns).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * long count = matrix.elementCount();   // Returns 6
     * }</pre>
     *
     * @return the total number of elements
     */
    public long elementCount() {
        return elementCount;
    }

    /**
     * Returns {@code true} if this matrix is empty (contains no elements).
     * A matrix is considered empty if either the number of rows or columns is zero,
     * resulting in a total count of zero elements.
     *
     * <p>Note: An empty matrix has zero elements. This includes shapes such as {@code 0 × 0}
     * and {@code rowCount × 0}, so an empty matrix does not necessarily have zero rows.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * boolean result1 = empty.isEmpty();   // Returns true (0x0)
     *
     * IntMatrix notEmpty = IntMatrix.of(new int[][] {{1}});
     * boolean result2 = notEmpty.isEmpty();   // Returns false (1x1)
     * }</pre>
     *
     * @return {@code true} if the matrix has no elements (count == 0), {@code false} otherwise
     */
    public boolean isEmpty() {
        return elementCount == 0;
    }

    /**
     * Prints this matrix to standard output in a formatted, human-readable manner and returns the output string.
     * Each concrete implementation provides its own formatting based on the element type.
     * This method is primarily intended for debugging and logging purposes.
     *
     * <p>The exact output format depends on the matrix type:
     * <ul>
     *   <li>Numeric matrices typically display values aligned in rows and columns</li>
     *   <li>Object matrices display using the {@code toString()} method of elements</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * String output = matrix.println();
     * // Prints to console:
     * // [1, 2]
     * // [3, 4]
     * // And returns the same string
     * }</pre>
     *
     * @return the formatted string representation of the matrix that was printed to standard output
     */
    public abstract String println();

    /**
     * Returns a copy of this matrix.
     * The returned matrix is a completely independent copy with its own underlying array;
     * modifications to one matrix do not affect the other.
     *
     * <p>This method creates new array instances and copies all element values.
     * For large matrices, this operation can be memory and time intensive.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix original = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntMatrix copy = original.copy();
     * copy.set(0, 0, 10);   // Original matrix remains unchanged
     * // original: {{1, 2}, {3, 4}}
     * // copy:     {{10, 2}, {3, 4}}
     * }</pre>
     *
     * @return a new matrix that is a copy of this matrix with the same dimensions and values
     */
    public abstract M copy();

    /**
     * Returns a copy of a row range from this matrix.
     * The returned matrix contains only the specified rows (with all columns) and is completely
     * independent from the original matrix.
     *
     * <p>This is equivalent to calling {@code copy(fromRowIndex, toRowIndex, 0, columnCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}, {5, 6}});
     * IntMatrix subMatrix = matrix.copy(0, 2);   // Contains rows 0 and 1
     * // subMatrix: {{1, 2}, {3, 4}}
     *
     * IntMatrix lastRow = matrix.copy(2, 3);   // Contains only row 2
     * // lastRow: {{5, 6}}
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new matrix containing the specified rows with dimensions (toRowIndex - fromRowIndex) × columnCount
     * @throws IndexOutOfBoundsException if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
     */
    public abstract M copy(int fromRowIndex, int toRowIndex);

    /**
     * Returns a copy of a rectangular region from this matrix.
     * The returned matrix contains only the specified rows and columns and is completely
     * independent from the original matrix.
     *
     * <p>This method allows you to extract any rectangular subregion of the matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * IntMatrix subMatrix = matrix.copy(0, 2, 1, 3);
     * // subMatrix: {{2, 3}, {5, 6}} (rows 0-1, columns 1-2)
     *
     * IntMatrix centerElement = matrix.copy(1, 2, 1, 2);
     * // centerElement: {{5}} (just the center element)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new matrix containing the specified region with dimensions
     *         (toRowIndex - fromRowIndex) × (toColumnIndex - fromColumnIndex)
     * @throws IndexOutOfBoundsException if any index is out of bounds, fromRowIndex &gt; toRowIndex, or fromColumnIndex &gt; toColumnIndex
     */
    public abstract M copy(int fromRowIndex, int toRowIndex, int fromColumnIndex, int toColumnIndex);

    /**
     * Returns a new matrix that is this matrix rotated 90 degrees clockwise.
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * column of the result being the last row of the original matrix reading upward.
     * The original matrix is not modified.
     *
     * <p>Rotation formula: element at position (i, j) in the original matrix
     * moves to position (j, rowCount - 1 - i) in the rotated matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    Rotated 90 degrees clockwise:
     * // 1 2 3        7 4 1
     * // 4 5 6   =>   8 5 2
     * // 7 8 9        9 6 3
     *
     * IntMatrix original = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * IntMatrix rotated = original.rotate90();   // 3x3 remains 3x3
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 90 degrees clockwise, with dimensions columnCount x rowCount
     */
    public abstract M rotate90();

    /**
     * Returns a new matrix that is this matrix rotated 180 degrees.
     * This is equivalent to flipping both horizontally and vertically, reversing the
     * order of all elements. The resulting matrix has the same dimensions as the original.
     * The original matrix is not modified.
     *
     * <p>Rotation formula: element at position (i, j) in the original matrix
     * moves to position (rowCount - 1 - i, columnCount - 1 - j) in the rotated matrix.</p>
     *
     * <p>This operation is equivalent to calling {@code rotate90().rotate90()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    Rotated 180 degrees:
     * // 1 2 3        9 8 7
     * // 4 5 6   =>   6 5 4
     * // 7 8 9        3 2 1
     *
     * IntMatrix original = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * IntMatrix rotated = original.rotate180();   // Dimensions remain 3x3
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 180 degrees, with the same dimensions (rowCount x columnCount)
     */
    public abstract M rotate180();

    /**
     * Returns a new matrix that is this matrix rotated 270 degrees clockwise (or equivalently, 90 degrees counter-clockwise).
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * column of the result being the first row of the original matrix in reverse order.
     * The original matrix is not modified.
     *
     * <p>Rotation formula: element at position (i, j) in the original matrix
     * moves to position (columnCount - 1 - j, i) in the rotated matrix.</p>
     *
     * <p>This operation is equivalent to calling {@code rotate90().rotate90().rotate90()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    Rotated 270 degrees clockwise:
     * // 1 2 3        3 6 9
     * // 4 5 6   =>   2 5 8
     * // 7 8 9        1 4 7
     *
     * IntMatrix original = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * IntMatrix rotated = original.rotate270();   // 3x3 becomes 3x3
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 270 degrees clockwise, with dimensions columnCount x rowCount
     */
    public abstract M rotate270();

    /**
     * Returns a new matrix that is the transpose of this matrix.
     * The transpose operation swaps rows and columns, so element at position (i, j)
     * in the original matrix appears at position (j, i) in the transposed matrix. The resulting
     * matrix has dimensions swapped (rowCount x columnCount becomes columnCount x rowCount).
     * The original matrix is not modified.
     *
     * <p>Transpose formula: element at position (i, j) in the original matrix
     * moves to position (j, i) in the transposed matrix.</p>
     *
     * <p>The transpose is a fundamental matrix operation used in linear algebra,
     * statistics, and many matrix algorithms.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    Transposed:
     * // 1 2 3        1 4
     * // 4 5 6   =>   2 5
     * //              3 6
     *
     * IntMatrix original = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntMatrix transposed = original.transpose();   // 2x3 becomes 3x2
     * }</pre>
     *
     * @return a new matrix that is the transpose of this matrix, with dimensions columnCount x rowCount
     */
    public abstract M transpose();

    /**
     * Returns a new matrix with the elements of this matrix rearranged into the specified number of columns.
     * The number of rows is automatically calculated based on the total element count.
     * Elements are taken in row-major order from the original matrix and placed into the
     * new shape. If the total element count is not evenly divisible by the new column count,
     * the last row will be padded with default values (0 for numeric types, false for boolean, null for objects).
     * The original matrix is not modified.
     *
     * <p>The new row count is calculated as: {@code ceiling(elementCount / newColumnCount)}</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntMatrix reshaped = matrix.reshape(2);   // Becomes [[1, 2], [3, 4], [5, 6]]
     *
     * IntMatrix matrix2 = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntMatrix reshaped2 = matrix2.reshape(4);   // Becomes [[1, 2, 3, 4], [5, 6, 0, 0]]
     * }</pre>
     *
     * @param newColumnCount the number of columns in the reshaped matrix (must be positive)
     * @return a new matrix with the specified number of columns
     * @throws IllegalArgumentException if {@code newColumnCount <= 0}, if the implied row count
     *         {@code ceil(elementCount / newColumnCount)} exceeds {@code Integer.MAX_VALUE}, or if the
     *         resulting shape is not representable (which occurs when this matrix is empty, since the
     *         implied row count is then {@code 0} while {@code newColumnCount} is positive)
     */
    public M reshape(final int newColumnCount) {
        N.checkArgument(newColumnCount > 0, "newColumnCount must be positive, but got: {}", newColumnCount);

        final long newRowCount = elementCount % newColumnCount == 0 ? elementCount / newColumnCount : elementCount / newColumnCount + 1;

        N.checkArgument(newRowCount <= Integer.MAX_VALUE, "Reshaped row count overflow: ceil({} / {}) = {} exceeds Integer.MAX_VALUE", elementCount,
                newColumnCount, newRowCount);

        checkRepresentableShape((int) newRowCount, newColumnCount);

        return reshape((int) newRowCount, newColumnCount);
    }

    /**
     * Returns a new matrix with the elements of this matrix rearranged into the specified dimensions.
     * Elements are taken in row-major order from the original matrix and placed into the
     * new shape. The new shape must have at least as many total elements as the original
     * ({@code newRowCount * newColumnCount >= elementCount()}).
     * If the new shape has more elements, the extra positions are filled with
     * default values (0 for numeric types, false for boolean, null for objects).
     * The original matrix is not modified.
     *
     * <p>This is a fundamental operation for restructuring matrix data without changing
     * the underlying element values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntMatrix reshaped = matrix.reshape(3, 2);    // Becomes [[1, 2], [3, 4], [5, 6]]
     * IntMatrix extended = matrix.reshape(2, 4);    // Becomes [[1, 2, 3, 4], [5, 6, 0, 0]]
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix; must be non-negative
     * @param newColumnCount the number of columns in the reshaped matrix; must be non-negative
     * @return a new matrix with the specified dimensions ({@code newRowCount × newColumnCount})
     * @throws IllegalArgumentException if {@code newRowCount < 0} or {@code newColumnCount < 0}, if the
     *         requested shape is not representable (zero rows with a non-zero column count), or if the
     *         new shape is too small to hold all {@code elementCount()} elements
     */
    public abstract M reshape(int newRowCount, int newColumnCount);

    /**
     * Returns {@code true} if this matrix has the same shape (dimensions) as the specified matrix.
     * Two matrices have the same shape if they have the same number of rows and columns.
     * The element values are not compared, only the dimensions.
     *
     * <p>This method is useful for validating matrix compatibility before operations
     * that require same-shaped matrices (e.g., element-wise addition or subtraction).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix m1 = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntMatrix m2 = IntMatrix.of(new int[][] {{5, 6}, {7, 8}});
     * boolean sameShape = m1.isSameShape(m2);   // Returns true (both are 2×2)
     *
     * IntMatrix m3 = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * boolean sameShape2 = m1.isSameShape(m3);   // Returns false (2×2 vs 2×3)
     * }</pre>
     *
     * @param m the matrix to compare with
     * @return {@code true} if both matrices have the same dimensions, {@code false} otherwise
     * @throws IllegalArgumentException if {@code m} is {@code null}
     */
    public boolean isSameShape(final M m) {
        N.checkArgNotNull(m, "m");
        return rowCount == m.rowCount && columnCount == m.columnCount;
    }

    /**
     * Returns a new matrix with each element repeated the specified number of times in both dimensions.
     * Each element is expanded into a block of size rowRepeats x columnRepeats.
     * The resulting matrix has dimensions (rowCount x rowRepeats) x (columnCount x columnRepeats).
     * The original matrix is not modified.
     *
     * <p>This operation is similar to MATLAB's repelem function. Each element becomes a block,
     * effectively creating a "zoomed in" version of the matrix where each original element
     * occupies multiple positions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    repeatElements(2, 2):
     * // 1 2          1 1 2 2
     * // 3 4     =>   1 1 2 2
     * //              3 3 4 4
     * //              3 3 4 4
     *
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntMatrix repeated = matrix.repeatElements(2, 2);   // 2×2 becomes 4×4
     * }</pre>
     *
     * @param rowRepeats number of times to repeat each element in the row direction (must be &gt;= 1)
     * @param columnRepeats number of times to repeat each element in the column direction (must be &gt;= 1)
     * @return a new matrix with repeated elements, with dimensions (rowCount x rowRepeats) x (columnCount x columnRepeats)
     * @throws IllegalArgumentException if {@code rowRepeats < 1} or {@code columnRepeats < 1},
     *         or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repelem.html">MATLAB repelem function</a>
     */
    public abstract M repeatElements(int rowRepeats, int columnRepeats);

    /**
     * Returns a new matrix formed by tiling this matrix the specified number of times in both dimensions.
     * The matrix is tiled rowRepeats times vertically and columnRepeats times horizontally.
     * The resulting matrix has dimensions (rowCount x rowRepeats) x (columnCount x columnRepeats).
     * The original matrix is not modified.
     *
     * <p>This operation is similar to MATLAB's repmat function. The entire matrix pattern
     * is replicated, creating a tiled arrangement.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Original:    repeatMatrix(2, 2):
     * // 1 2          1 2 1 2
     * // 3 4     =>   3 4 3 4
     * //              1 2 1 2
     * //              3 4 3 4
     *
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntMatrix tiled = matrix.repeatMatrix(2, 2);   // 2×2 becomes 4×4
     * }</pre>
     *
     * @param rowRepeats number of times to repeat the matrix in the row direction (must be &gt;= 1)
     * @param columnRepeats number of times to repeat the matrix in the column direction (must be &gt;= 1)
     * @return a new matrix with this matrix tiled, with dimensions (rowCount x rowRepeats) x (columnCount x columnRepeats)
     * @throws IllegalArgumentException if {@code rowRepeats < 1} or {@code columnRepeats < 1},
     *         or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repmat.html">MATLAB repmat function</a>
     */
    public abstract M repeatMatrix(int rowRepeats, int columnRepeats);

    /**
     * Returns a new matrix grown by the specified non-negative pad widths on each side.
     * Newly introduced cells are filled with the type's default value (e.g. {@code 0} for primitives,
     * {@code null} for object matrices). The original matrix is not modified.
     *
     * <p>The resulting matrix has dimensions
     * {@code (padTop + rowCount + padBottom) x (padLeft + columnCount + padRight)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntMatrix padded = matrix.extend(1, 1, 1, 1);
     * // padded is 4x4 with original 2x2 in the center, surrounded by 0s
     * }</pre>
     *
     * @param padTop number of rows to add above the matrix (must be &gt;= 0)
     * @param padBottom number of rows to add below the matrix (must be &gt;= 0)
     * @param padLeft number of columns to add to the left of the matrix (must be &gt;= 0)
     * @param padRight number of columns to add to the right of the matrix (must be &gt;= 0)
     * @return a new matrix grown by the specified pad widths, with new cells filled with the type's default value
     * @throws IllegalArgumentException if any pad value is negative or if the resulting dimensions overflow {@code Integer.MAX_VALUE}
     */
    public abstract M extend(int padTop, int padBottom, int padLeft, int padRight);

    /**
     * Returns a new matrix that is a horizontal flip (mirror across the vertical axis) of this matrix.
     * Element at position {@code (i, j)} in this matrix appears at position {@code (i, columnCount - 1 - j)}
     * in the result. This matrix is not modified.
     *
     * @return a new matrix with columns reversed within each row
     * @see #flipHorizontallyInPlace()
     * @see #flipVertically()
     */
    public abstract M flipHorizontally();

    /**
     * Returns a new matrix that is a vertical flip (mirror across the horizontal axis) of this matrix.
     * Element at position {@code (i, j)} in this matrix appears at position {@code (rowCount - 1 - i, j)}
     * in the result. This matrix is not modified.
     *
     * @return a new matrix with rows reversed
     * @see #flipVerticallyInPlace()
     * @see #flipHorizontally()
     */
    public abstract M flipVertically();

    /**
     * Flips this matrix horizontally in place (mirror across the vertical axis).
     * Element at position {@code (i, j)} is moved to position {@code (i, columnCount - 1 - j)}.
     * This method modifies this matrix.
     *
     * @see #flipHorizontally()
     * @see #flipVerticallyInPlace()
     */
    public abstract void flipHorizontallyInPlace();

    /**
     * Flips this matrix vertically in place (mirror across the horizontal axis).
     * Element at position {@code (i, j)} is moved to position {@code (rowCount - 1 - i, j)}.
     * This method modifies this matrix.
     *
     * @see #flipVertically()
     * @see #flipHorizontallyInPlace()
     */
    public abstract void flipVerticallyInPlace();

    /**
     * Vertically stacks this matrix with the specified matrix.
     * The matrices must have the same number of columns. The result has rows from this matrix
     * followed by rows from the other matrix. Creates a new matrix; neither input is modified.
     *
     * @param other the matrix to stack below this matrix (must not be {@code null})
     * @return a new matrix with combined rows and the same column count
     * @throws IllegalArgumentException if {@code other} is {@code null}, has a different column count,
     *         or the merged row count would overflow {@code Integer.MAX_VALUE}
     * @see #stackHorizontally(AbstractMatrix)
     */
    public abstract M stackVertically(M other);

    /**
     * Horizontally stacks this matrix with the specified matrix.
     * The matrices must have the same number of rows. The result has columns from this matrix
     * followed by columns from the other matrix. Creates a new matrix; neither input is modified.
     *
     * @param other the matrix to stack to the right of this matrix (must not be {@code null})
     * @return a new matrix with combined columns and the same row count
     * @throws IllegalArgumentException if {@code other} is {@code null}, has a different row count,
     *         or the merged column count would overflow {@code Integer.MAX_VALUE}
     * @see #stackVertically(AbstractMatrix)
     */
    public abstract M stackHorizontally(M other);

    /**
     * Flattens this matrix into a one-dimensional list.
     * Elements are taken in row-major order (row by row from left to right).
     * The returned list is a new instance; modifications to it do not affect this matrix.
     *
     * <p>The flattening operation converts a two-dimensional matrix structure into a one-dimensional sequence,
     * which is useful for operations that work on linear data structures.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntList flat = matrix.flatten();   // Returns [1, 2, 3, 4]
     *
     * IntMatrix matrix2 = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntList flat2 = matrix2.flatten();   // Returns [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * @return a new list containing all elements in row-major order with size equal to {@code elementCount}
     * @throws IllegalStateException if the element count exceeds {@code Integer.MAX_VALUE} and therefore
     *         cannot be materialized into a flat array
     */
    public abstract PL flatten();

    /**
     * Applies the specified operation to the flattened (row-major order) view of this matrix.
     * The operation receives a single one-dimensional array containing all elements in row-major order,
     * and any modifications to that array are reflected back in this matrix.
     *
     * <p>This is useful for operations that are easier to perform on a flat array representation,
     * such as sorting all elements, applying statistical transformations, or batch updates.</p>
     *
     * <p><strong>Unsafe API boundary:</strong> the supplied action receives a mutable flattened view of the matrix data.
     * Any mutation performed by the action is reflected back into this matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{3, 1, 4}, {1, 5, 9}});
     * matrix.mutateAsFlat(a -> java.util.Arrays.sort(a));   // Sorts all elements
     * // Matrix becomes [[1, 1, 3], [4, 5, 9]] (elements sorted in row-major order)
     *
     * matrix.mutateAsFlat(a -> { for (int i = 0; i < a.length; i++) a[i] *= 2; });
     * // Doubles all elements
     * }</pre>
     *
     * @param <E> the type of exception that the operation might throw
     * @param action the operation to apply to the flattened array (receives array type A, not A[])
     * @throws E if the operation throws an exception
     */
    public abstract <E extends Exception> void mutateAsFlat(Throwables.Consumer<? super A, E> action) throws E;

    /**
     * Performs the specified action for each element position in the matrix.
     * The action receives the row and column indices for each element.
     * Elements are processed in row-major order (row by row from left to right).
     * For large matrices, the operation may be automatically parallelized for better performance.
     *
     * <p>This method is useful when you need to access matrix positions without caring about
     * the actual element values, or when the element access logic is handled inside the action.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     *
     * matrix.forEachIndices((i, j) -> {
     *     System.out.println("Position: (" + i + "," + j + ")");
     * });
     *
     * // Count elements on the main diagonal
     * AtomicInteger diagonalCount = new AtomicInteger(0);
     * matrix.forEachIndices((i, j) -> {
     *     if (i == j) diagonalCount.incrementAndGet();
     * });
     * }</pre>
     *
     * @param <E> the type of exception that the action might throw
     * @param action the action to perform for each position, receives (rowIndex, columnIndex)
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEachIndices(final Throwables.IntBiConsumer<E> action) throws E {
        N.checkArgNotNull(action, "action");

        if (Matrices.isParallelizable(this)) {
            //noinspection FunctionalExpressionCanBeFolded
            final Throwables.IntBiConsumer<E> elementAction = action::accept;
            Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
        } else {
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < columnCount; j++) {
                    action.accept(i, j);
                }
            }
        }
    }

    /**
     * Performs the specified action for each element position in the specified rectangular region of the matrix.
     * The action receives the row and column indices for each element in the region.
     * Elements are processed in row-major order within the specified region.
     * For large regions, the operation may be automatically parallelized for better performance.
     *
     * <p>This allows selective processing of matrix subregions without creating a copy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * // Process only a 2×2 subregion starting at (1,1)
     * matrix.forEachIndices(1, 3, 1, 3, (i, j) -> {
     *     System.out.println("Processing element at (" + i + "," + j + ")");
     * });
     *
     * // Process only the first column
     * matrix.forEachIndices(0, matrix.rowCount(), 0, 1, (i, j) -> {
     *     // Process each element in column 0
     * });
     * }</pre>
     *
     * @param <E> the type of exception that the action might throw
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @param action the action to perform for each position, receives (rowIndex, columnIndex)
     * @throws IndexOutOfBoundsException if any index is out of bounds
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEachIndices(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex,
            final Throwables.IntBiConsumer<E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        if (Matrices.isParallelizable(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            //noinspection FunctionalExpressionCanBeFolded
            final Throwables.IntBiConsumer<E> elementAction = action::accept;
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, elementAction, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                    action.accept(i, j);
                }
            }
        }
    }

    /**
     * Performs the specified action for each element position in the matrix, providing the matrix itself as a parameter.
     * The action receives the row index, column index, and the matrix instance.
     * Elements are processed in row-major order (row by row from left to right).
     * For large matrices, the operation may be automatically parallelized for better performance.
     *
     * <p>This variant is useful when the action needs access to matrix elements or methods,
     * allowing you to read/write values or use matrix operations within the action.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * matrix.forEachIndices((i, j, m) -> {
     *     int value = m.get(i, j);
     *     System.out.println("Value at (" + i + "," + j + ") is " + value);
     * });
     *
     * // Set each element to the sum of its indices
     * matrix.forEachIndices((i, j, m) -> m.set(i, j, i + j));
     * }</pre>
     *
     * @param <E> the type of exception that the action might throw
     * @param action the action to perform, receiving (rowIndex, columnIndex, matrix)
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEachIndices(final Throwables.BiIntObjConsumer<M, E> action) throws E {
        final M matrix = (M) this;
        N.checkArgNotNull(action, "action");

        if (Matrices.isParallelizable(this)) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> action.accept(i, j, matrix);
            Matrices.forEachIndices(rowCount, columnCount, elementAction, true);
        } else {
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < columnCount; j++) {
                    action.accept(i, j, matrix);
                }
            }
        }
    }

    /**
     * Performs the specified action for each element position in the specified rectangular region, providing the matrix itself.
     * The action receives the row index, column index, and the matrix instance for each position in the region.
     * Elements are processed in row-major order within the specified region.
     * For large regions, the operation may be automatically parallelized for better performance.
     *
     * <p>This combines region-based iteration with matrix access, allowing you to process
     * a subregion while having access to the entire matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * matrix.forEachIndices(1, 3, 1, 3, (i, j, m) -> {
     *     // Process only the 2×2 subregion with access to matrix
     *     int value = m.get(i, j);
     *     System.out.println("Value at (" + i + "," + j + "): " + value);
     * });
     *
     * // Update subregion based on neighboring values
     * matrix.forEachIndices(1, matrix.rowCount() - 1, 1, matrix.columnCount() - 1, (i, j, m) -> {
     *     int avg = (m.get(i-1, j) + m.get(i+1, j) + m.get(i, j-1) + m.get(i, j+1)) / 4;
     *     m.set(i, j, avg);
     * });
     * }</pre>
     *
     * @param <E> the type of exception that the action might throw
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @param action the action to perform, receiving (rowIndex, columnIndex, matrix)
     * @throws IndexOutOfBoundsException if any index is out of bounds
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEachIndices(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex,
            final Throwables.BiIntObjConsumer<M, E> action) throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);
        N.checkArgNotNull(action, "action");

        final M matrix = (M) this;

        if (Matrices.isParallelizable(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex))) {
            final Throwables.IntBiConsumer<E> elementAction = (i, j) -> action.accept(i, j, matrix);
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, elementAction, true);
        } else {
            for (int i = fromRowIndex; i < toRowIndex; i++) {
                for (int j = fromColumnIndex; j < toColumnIndex; j++) {
                    action.accept(i, j, matrix);
                }
            }
        }
    }

    /**
     * Returns a stream of points directly above, below, to the left of, and to the right of the
     * specified position (the four cardinal directions). Only includes points that are within
     * matrix bounds; positions at the edges will have fewer than 4 adjacent points.
     *
     * <p>This method is useful for grid traversal algorithms, pathfinding, and neighbor analysis
     * where only orthogonal (non-diagonal) adjacency is considered. Points are returned in the
     * order: up, right, down, left.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * Stream<Point> adjacent = matrix.adjacent4Points(0, 0);
     * // Returns stream of Point(0, 1) and Point(1, 0) - only right and down exist
     *
     * // Center position has all 4 neighbors
     * IntMatrix larger = IntMatrix.of(new int[3][3]);
     * Stream<Point> centerAdj = larger.adjacent4Points(1, 1);
     * // Returns all 4 adjacent points: (0,1), (1,2), (2,1), (1,0)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return a stream of adjacent points in the four cardinal directions (0 to 4 points depending on position)
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public Stream<Point> adjacent4Points(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        final List<Point> points = new ArrayList<>(4);

        if (rowIndex > 0) {
            points.add(Point.of(rowIndex - 1, columnIndex)); // up
        }
        if (columnIndex < columnCount - 1) {
            points.add(Point.of(rowIndex, columnIndex + 1)); // right
        }
        if (rowIndex < rowCount - 1) {
            points.add(Point.of(rowIndex + 1, columnIndex)); // down
        }
        if (columnIndex > 0) {
            points.add(Point.of(rowIndex, columnIndex - 1)); // left
        }

        return Stream.of(points);
    }

    /**
     * Returns a stream of all 8 points adjacent to the specified position, including the points
     * directly above, below, to the left of, to the right of, and diagonally adjacent to the
     * specified position. Only includes points that are within matrix bounds.
     *
     * <p>This method is useful for algorithms requiring full 8-way adjacency, such as
     * certain pathfinding algorithms, cellular automaton simulations (like Conway's Game of Life),
     * or flood fill operations. Points are returned clockwise starting from the top-left:
     * leftUp, up, rightUp, right, rightDown, down, leftDown, left.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanMatrix matrix = BooleanMatrix.of(new boolean[][] {{true, false, true}, {false, true, false}, {true, false, true}});
     * Stream<Point> adjacent = matrix.adjacent8Points(1, 1);
     * // Returns stream of all 8 surrounding points for the center position
     *
     * // Corner position has only 3 neighbors
     * Stream<Point> corner = matrix.adjacent8Points(0, 0);
     * // Returns 3 points: (0,1), (1,1), (1,0)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return a stream of adjacent points in all 8 directions (0 to 8 points depending on position)
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    public Stream<Point> adjacent8Points(final int rowIndex, final int columnIndex) {
        checkRowColumnIndex(rowIndex, columnIndex);

        final List<Point> points = new ArrayList<>(8);

        if (rowIndex > 0 && columnIndex > 0) {
            points.add(Point.of(rowIndex - 1, columnIndex - 1)); // leftUp
        }

        if (rowIndex > 0) {
            points.add(Point.of(rowIndex - 1, columnIndex)); // up
        }

        if (rowIndex > 0 && columnIndex < columnCount - 1) {
            points.add(Point.of(rowIndex - 1, columnIndex + 1)); // rightUp
        }

        if (columnIndex < columnCount - 1) {
            points.add(Point.of(rowIndex, columnIndex + 1)); // right
        }

        if (rowIndex < rowCount - 1 && columnIndex < columnCount - 1) {
            points.add(Point.of(rowIndex + 1, columnIndex + 1)); // rightDown
        }

        if (rowIndex < rowCount - 1) {
            points.add(Point.of(rowIndex + 1, columnIndex)); // down
        }

        if (rowIndex < rowCount - 1 && columnIndex > 0) {
            points.add(Point.of(rowIndex + 1, columnIndex - 1)); // leftDown
        }

        if (columnIndex > 0) {
            points.add(Point.of(rowIndex, columnIndex - 1)); // left
        }

        return Stream.of(points);
    }

    /**
     * Returns a stream of points along the main diagonal (upper-left to lower-right).
     * The main diagonal consists of elements where row index equals column index.
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>The main diagonal runs from the upper-left corner to the lower-right corner.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * Stream<Point> diagonal = matrix.mainDiagonalPoints();   // Points: (0,0), (1,1), (2,2)
     * List<Point> points = diagonal.toList();          // [(0,0), (1,1), (2,2)]
     * }</pre>
     *
     * @return a stream of {@link Point} objects representing the main diagonal positions
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    public Stream<Point> mainDiagonalPoints() {
        checkIsSquare();

        //noinspection resource
        return IntStream.range(0, rowCount).mapToObj(i -> Point.of(i, i));
    }

    /**
     * Returns a stream of points along the anti-diagonal (upper-right to lower-left).
     * The anti-diagonal consists of elements where row index + column index equals (columnCount - 1).
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>The anti-diagonal runs from the upper-right corner to the lower-left corner.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * Stream<Point> antiDiagonal = matrix.antiDiagonalPoints();   // Points: (0,2), (1,1), (2,0)
     * List<Point> points = antiDiagonal.toList();          // [(0,2), (1,1), (2,0)]
     * }</pre>
     *
     * @return a stream of {@link Point} objects representing the anti-diagonal positions
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    public Stream<Point> antiDiagonalPoints() {
        checkIsSquare();

        //noinspection resource
        return IntStream.range(0, rowCount).mapToObj(i -> Point.of(i, columnCount - i - 1));
    }

    /**
     * Returns a copy of the main diagonal elements (upper-left to lower-right) as the matrix's
     * underlying array type. The matrix must be square (rowCount == columnCount).
     *
     * <p>The returned array is a copy; modifications to it do not affect the matrix.</p>
     *
     * @return a new array containing the main diagonal values
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    public abstract A getMainDiagonal();

    /**
     * Sets the elements on the main diagonal (upper-left to lower-right).
     * The matrix must be square (rowCount == columnCount), and the supplied array must contain
     * exactly {@code rowCount} elements.
     *
     * @param mainDiagonal the new values for the main diagonal; must be non-{@code null} and have length equal to {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code mainDiagonal} is {@code null} or its length does not equal {@code rowCount}
     */
    public abstract void setMainDiagonal(A mainDiagonal);

    /**
     * Returns a copy of the anti-diagonal elements (upper-right to lower-left) as the matrix's
     * underlying array type. The matrix must be square (rowCount == columnCount).
     *
     * <p>The returned array is a copy; modifications to it do not affect the matrix.</p>
     *
     * @return a new array containing the anti-diagonal values
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    public abstract A getAntiDiagonal();

    /**
     * Sets the elements on the anti-diagonal (upper-right to lower-left).
     * The matrix must be square (rowCount == columnCount), and the supplied array must contain
     * exactly {@code rowCount} elements.
     *
     * @param antiDiagonal the new values for the anti-diagonal; must be non-{@code null} and have length equal to {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null} or its length does not equal {@code rowCount}
     */
    public abstract void setAntiDiagonal(A antiDiagonal);

    /**
     * Returns a stream of all points in the matrix in row-major order (horizontal traversal).
     * Points are generated row by row from left to right, top to bottom.
     *
     * <p>This is equivalent to calling {@code horizontalPoints(0, rowCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * Stream<Point> allPoints = matrix.horizontalPoints();
     * // Points in order: (0,0), (0,1), (1,0), (1,1)
     * allPoints.forEach(p -> System.out.println("Point: " + p));
     * }</pre>
     *
     * @return a stream of all {@link Point} objects in row-major order
     */
    public Stream<Point> horizontalPoints() {
        return horizontalPoints(0, rowCount);
    }

    /**
     * Returns a stream of points for a specific row in horizontal order (left to right).
     *
     * <p>This is equivalent to calling {@code horizontalPoints(rowIndex, rowIndex + 1)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * Stream<Point> row1Points = matrix.horizontalPoints(1);   // All points in row 1
     * // For this 5-column matrix: (1,0), (1,1), (1,2), (1,3), (1,4)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @return a stream of {@link Point} objects for all columns in the specified row
     * @throws IndexOutOfBoundsException if rowIndex &lt; 0 or rowIndex &gt;= rowCount
     */
    public Stream<Point> horizontalPoints(final int rowIndex) {
        return horizontalPoints(rowIndex, rowIndex + 1);
    }

    /**
     * Returns a stream of points for a range of rows in row-major order (horizontal traversal).
     * Points are generated row by row from left to right for the specified row range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * // Get points from rows 1 and 2 (indices 1 and 2, not including 3)
     * Stream<Point> points = matrix.horizontalPoints(1, 3);
     * // For this 5-column matrix: (1,0), (1,1), (1,2), (1,3), (1,4), (2,0), (2,1), (2,2), (2,3), (2,4)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a stream of {@link Point} objects in the specified row range, in row-major order
     * @throws IndexOutOfBoundsException if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
     */
    @SuppressWarnings("resource")
    public Stream<Point> horizontalPoints(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        return IntStream.range(fromRowIndex, toRowIndex)
                .flatMapToObj(rowIndex -> IntStream.range(0, columnCount).mapToObj(columnIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of all points in the matrix in column-major order (vertical traversal).
     * Points are generated column by column from top to bottom, left to right.
     *
     * <p>This is equivalent to calling {@code verticalPoints(0, columnCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * Stream<Point> columnOrder = matrix.verticalPoints();
     * // Points in order: (0,0), (1,0), (0,1), (1,1)
     * // For a 2×3 matrix, order would be: (0,0), (1,0), (0,1), (1,1), (0,2), (1,2)
     * }</pre>
     *
     * @return a stream of all {@link Point} objects in column-major order
     */
    public Stream<Point> verticalPoints() {
        return verticalPoints(0, columnCount);
    }

    /**
     * Returns a stream of points for a specific column in vertical order (top to bottom).
     *
     * <p>This is equivalent to calling {@code verticalPoints(columnIndex, columnIndex + 1)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * Stream<Point> col2Points = matrix.verticalPoints(2);   // All points in column 2
     * // For a 3-row matrix: (0,2), (1,2), (2,2)
     * }</pre>
     *
     * @param columnIndex the column index (0-based)
     * @return a stream of {@link Point} objects for all rows in the specified column
     * @throws IndexOutOfBoundsException if columnIndex &lt; 0 or columnIndex &gt;= columnCount
     */
    public Stream<Point> verticalPoints(final int columnIndex) {
        return verticalPoints(columnIndex, columnIndex + 1);
    }

    /**
     * Returns a stream of points for a range of columns in column-major order (vertical traversal).
     * Points are generated column by column from top to bottom for the specified column range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * // Get points from columns 1 through 3 (indices 1, 2, 3, not including 4)
     * Stream<Point> points = matrix.verticalPoints(1, 4);
     * // For this 3-row matrix: (0,1), (1,1), (2,1), (0,2), (1,2), (2,2), (0,3), (1,3), (2,3)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a stream of {@link Point} objects in the specified column range, in column-major order
     * @throws IndexOutOfBoundsException if fromColumnIndex &lt; 0, toColumnIndex &gt; columnCount, or fromColumnIndex &gt; toColumnIndex
     */
    @SuppressWarnings("resource")
    public Stream<Point> verticalPoints(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        return IntStream.range(fromColumnIndex, toColumnIndex)
                .flatMapToObj(columnIndex -> IntStream.range(0, rowCount).mapToObj(rowIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of streams where each inner stream represents a row of points.
     * This allows for row-by-row processing of matrix positions.
     *
     * <p>This is equivalent to calling {@code rowPoints(0, rowCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * matrix.rowPoints().forEach(rowStream -> {
     *     rowStream.forEach(point -> System.out.println("Point: " + point));
     * });
     *
     * // Collect each row's points separately
     * List<List<Point>> rowsOfPoints = matrix.rowPoints()
     *     .map(Stream::toList)
     *     .toList();
     * }</pre>
     *
     * @return a stream of streams, where each inner stream contains {@link Point} objects for one row
     */
    public Stream<Stream<Point>> rowPoints() {
        return rowPoints(0, rowCount);
    }

    /**
     * Returns a stream of streams for a range of rows, where each inner stream represents a row of points.
     * This allows for selective row-by-row processing of matrix positions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * // Process rows 1 and 2 separately
     * matrix.rowPoints(1, 3).forEach(rowStream -> {
     *     List<Point> rowPoints = rowStream.toList();
     *     // Process each row's points
     * });
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a stream of streams, where each inner stream contains {@link Point} objects for one row
     * @throws IndexOutOfBoundsException if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
     */
    @SuppressWarnings("resource")
    public Stream<Stream<Point>> rowPoints(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        return IntStream.range(fromRowIndex, toRowIndex)
                .mapToObj(rowIndex -> IntStream.range(0, columnCount).mapToObj(columnIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of streams where each inner stream represents a column of points.
     * This allows for column-by-column processing of matrix positions.
     *
     * <p>This is equivalent to calling {@code columnPoints(0, columnCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * matrix.columnPoints().forEach(colStream -> {
     *     colStream.forEach(point -> System.out.println("Point: " + point));
     * });
     *
     * // Collect each column's points separately
     * List<List<Point>> columnsOfPoints = matrix.columnPoints()
     *     .map(Stream::toList)
     *     .toList();
     * }</pre>
     *
     * @return a stream of streams, where each inner stream contains {@link Point} objects for one column
     */
    public Stream<Stream<Point>> columnPoints() {
        return columnPoints(0, columnCount);
    }

    /**
     * Returns a stream of streams for a range of columns, where each inner stream represents a column of points.
     * This allows for selective column-by-column processing of matrix positions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * // Process columns 2 through 4 separately
     * matrix.columnPoints(2, 5).forEach(colStream -> {
     *     List<Point> colPoints = colStream.toList();
     *     // Process each column's points
     * });
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a stream of streams, where each inner stream contains {@link Point} objects for one column
     * @throws IndexOutOfBoundsException if fromColumnIndex &lt; 0, toColumnIndex &gt; columnCount, or fromColumnIndex &gt; toColumnIndex
     */
    @SuppressWarnings("resource")
    public Stream<Stream<Point>> columnPoints(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        return IntStream.range(fromColumnIndex, toColumnIndex)
                .mapToObj(columnIndex -> IntStream.range(0, rowCount).mapToObj(rowIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of elements along the main diagonal (upper-left to lower-right).
     * The main diagonal consists of elements where row index equals column index.
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>The main diagonal runs from the upper-left corner to the lower-right corner.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * IntStream diagonal = matrix.mainDiagonalStream();   // Stream of: 1, 5, 9
     * int sum = diagonal.sum();                    // 15
     * }</pre>
     *
     * @return a stream of diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    public abstract ES mainDiagonalStream();

    /**
     * Returns a stream of elements along the anti-diagonal (upper-right to lower-left).
     * The anti-diagonal consists of elements where row index + column index equals (columnCount - 1).
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>The anti-diagonal runs from the upper-right corner to the lower-left corner.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * IntStream antiDiagonal = matrix.antiDiagonalStream();   // Stream of: 3, 5, 7
     * int sum = antiDiagonal.sum();                    // 15
     * }</pre>
     *
     * @return a stream of anti-diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    public abstract ES antiDiagonalStream();

    /**
     * Returns a stream of all elements in row-major order (horizontal traversal).
     * Elements are streamed row by row from left to right, top to bottom.
     *
     * <p>This is equivalent to calling {@code horizontalStream(0, rowCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntStream elements = matrix.horizontalStream();   // Stream of: 1, 2, 3, 4
     * int sum = elements.sum();                // 10
     * }</pre>
     *
     * @return a stream of all elements in row-major order
     */
    public abstract ES horizontalStream();

    /**
     * Returns a stream of elements from a specific row.
     * Elements are streamed from left to right within the row.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntStream row1 = matrix.horizontalStream(1);   // Stream of: 4, 5, 6
     * int max = row1.max().orElse(0);       // 6
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @return a stream of elements in the specified row
     * @throws IndexOutOfBoundsException if rowIndex &lt; 0 or rowIndex &gt;= rowCount
     */
    public abstract ES horizontalStream(final int rowIndex);

    /**
     * Returns a stream of elements from a range of rows in row-major order.
     * Elements are streamed row by row from left to right for the specified row range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}, {5, 6}});
     * IntStream rows1and2 = matrix.horizontalStream(1, 3);   // Stream of: 3, 4, 5, 6
     * long count = rows1and2.count();               // 4
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a stream of elements in the specified row range
     * @throws IndexOutOfBoundsException if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
     */
    public abstract ES horizontalStream(final int fromRowIndex, final int toRowIndex);

    /**
     * Returns a stream of all elements in column-major order (vertical traversal).
     * Elements are streamed column by column from top to bottom, left to right.
     *
     * <p>This is equivalent to calling {@code verticalStream(0, columnCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntStream elements = matrix.verticalStream();   // Stream of: 1, 3, 2, 4
     * int sum = elements.sum();                // 10
     * }</pre>
     *
     * @return a stream of all elements in column-major order
     */
    public abstract ES verticalStream();

    /**
     * Returns a stream of elements from a specific column.
     * Elements are streamed from top to bottom within the column.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntStream col1 = matrix.verticalStream(1);   // Stream of: 2, 5
     * int min = col1.min().orElse(0);       // 2
     * }</pre>
     *
     * @param columnIndex the column index (0-based)
     * @return a stream of elements in the specified column
     * @throws IndexOutOfBoundsException if columnIndex &lt; 0 or columnIndex &gt;= columnCount
     */
    public abstract ES verticalStream(final int columnIndex);

    /**
     * Returns a stream of elements from a range of columns in column-major order.
     * Elements are streamed column by column from top to bottom for the specified column range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntStream cols1and2 = matrix.verticalStream(1, 3);   // Stream of: 2, 5, 3, 6
     * double avg = cols1and2.average().orElse(0);   // 4.0
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a stream of elements in the specified column range
     * @throws IndexOutOfBoundsException if fromColumnIndex &lt; 0, toColumnIndex &gt; columnCount, or fromColumnIndex &gt; toColumnIndex
     */
    public abstract ES verticalStream(final int fromColumnIndex, final int toColumnIndex);

    /**
     * Returns a stream of row streams.
     * Each element in the outer stream is a stream representing one row of the matrix.
     * This allows for per-row processing using stream operations.
     *
     * <p>This is equivalent to calling {@code rowStreams(0, rowCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.rowStreams().forEach(rowStream -> {
     *     int sum = rowStream.sum();   // Sum each row
     *     System.out.println("Row sum: " + sum);
     * });
     * // Output: Row sum: 6
     * //         Row sum: 15
     * }</pre>
     *
     * @return a stream of row streams
     */
    public abstract RS rowStreams();

    /**
     * Returns a stream of row streams for a range of rows.
     * Each element in the outer stream is a stream representing one row of the matrix
     * within the specified range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * // Process only rows 1 and 2
     * matrix.rowStreams(1, 3).forEach(rowStream -> {
     *     int max = rowStream.max().orElse(0);
     *     System.out.println("Row max: " + max);
     * });
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a stream of row streams for the specified range
     * @throws IndexOutOfBoundsException if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
     */
    public abstract RS rowStreams(final int fromRowIndex, final int toRowIndex);

    /**
     * Returns a stream of column streams.
     * Each element in the outer stream is a stream representing one column of the matrix.
     * This allows for per-column processing using stream operations.
     *
     * <p>This is equivalent to calling {@code columnStreams(0, columnCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.columnStreams().forEach(colStream -> {
     *     double avg = colStream.average().orElse(0);
     *     System.out.println("Column average: " + avg);
     * });
     * // Output: Column average: 2.0
     * //         Column average: 3.0
     * }</pre>
     *
     * @return a stream of column streams
     */
    public abstract RS columnStreams();

    /**
     * Returns a stream of column streams for a range of columns.
     * Each element in the outer stream is a stream representing one column of the matrix
     * within the specified range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * // Process columns 2 through 4
     * matrix.columnStreams(2, 5).forEach(colStream -> {
     *     int sum = colStream.sum();
     *     System.out.println("Column sum: " + sum);
     * });
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a stream of column streams for the specified range
     * @throws IndexOutOfBoundsException if fromColumnIndex &lt; 0, toColumnIndex &gt; columnCount, or fromColumnIndex &gt; toColumnIndex
     */
    public abstract RS columnStreams(final int fromColumnIndex, final int toColumnIndex);

    /**
     * Executes the specified action with this matrix as the parameter.
     * This method enables the functional programming pattern of passing the matrix to a consumer function
     * for side-effect operations such as logging, validation, or modification.
     *
     * <p>This method is useful for performing operations on the matrix without returning a value,
     * such as printing, logging, or passing to utility methods that expect a consumer.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * // Log matrix details
     * matrix.accept(m -> {
     *     System.out.println("Matrix dimensions: " + m.rowCount() + "x" + m.columnCount());
     *     m.println();
     * });
     *
     * // Validate matrix before processing
     * matrix.accept(m -> {
     *     if (m.isEmpty()) {
     *         throw new IllegalStateException("Matrix cannot be empty");
     *     }
     * });
     *
     * // Modify matrix elements in place
     * matrix.accept(m -> {
     *     for (int i = 0; i < m.rowCount(); i++) {
     *         m.set(i, 0, 0);   // Set first column to 0
     *     }
     * });
     * }</pre>
     *
     * @param <E> the type of exception that the action might throw
     * @param action the consumer action to perform on this matrix
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void accept(final Throwables.Consumer<? super M, E> action) throws E {
        N.checkArgNotNull(action, "action");
        action.accept((M) this);
    }

    /**
     * Applies the specified function to this matrix and returns the result.
     * This method enables fluent-style transformations where the matrix needs to be passed to a function.
     * It follows the functional programming pattern of applying a function and returning its result.
     *
     * <p>This method is useful for extracting values from the matrix or transforming it
     * into a different type while maintaining a fluent interface.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * long elementCount = matrix.apply(AbstractMatrix::elementCount);
     * String info = matrix.apply(m -> "Matrix " + m.rowCount() + "x" + m.columnCount());
     *
     * // Transform matrix into a different representation
     * IntList allValues = matrix.apply(IntMatrix::flatten);
     * }</pre>
     *
     * @param <R> the result type of the function
     * @param <E> the type of exception that the function might throw
     * @param mapper the function to apply to this matrix
     * @return the result of applying the function to this matrix
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     * @throws E if the function throws an exception
     */
    public <R, E extends Exception> R apply(final Throwables.Function<? super M, R, E> mapper) throws E {
        N.checkArgNotNull(mapper, "mapper");
        return mapper.apply((M) this);
    }

    /**
     * Returns the length of the given row array.
     * This abstract method must be implemented by concrete subclasses to return the length
     * of their specific array type (e.g., {@code int[]}, {@code double[]}, or {@code Object[]}).
     *
     * @param a the row array whose length is to be determined
     * @return the length of the array
     */
    protected abstract int length(@SuppressWarnings("hiding") A a);

    /**
     * Validates that this matrix has the same shape (dimensions) as the specified matrix.
     * This is a helper method used internally to enforce shape compatibility before
     * operations that require matrices of the same dimensions (e.g., element-wise addition).
     *
     * @param x the matrix to compare shape with; must not be {@code null}
     * @throws IllegalArgumentException if {@code x} is {@code null}, or if the matrices have
     *         different row counts or column counts
     */
    protected void checkSameShape(final M x) {
        N.checkArgument(this.isSameShape(x), MSG_SHAPE_MISMATCH, rowCount, columnCount, x.rowCount, x.columnCount);
    }

    /**
     * Validates that the specified row and column indices are within the bounds of this matrix.
     * This is a helper method used internally to enforce index validity before element access
     * or neighbor lookup operations.
     *
     * @param rowIndex the row index to validate (must be in range [0, rowCount))
     * @param columnIndex the column index to validate (must be in range [0, columnCount))
     * @throws ArrayIndexOutOfBoundsException if rowIndex or columnIndex is out of bounds
     */
    protected void checkRowColumnIndex(final int rowIndex, final int columnIndex) {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new ArrayIndexOutOfBoundsException(formatMsg(MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount));
        }

        if (columnIndex < 0 || columnIndex >= columnCount) {
            throw new ArrayIndexOutOfBoundsException(formatMsg(MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount));
        }
    }

    /**
     * Validates that this matrix is square (rowCount == columnCount).
     * This is a helper method used internally to enforce the square matrix requirement
     * for diagonal operations such as {@link #mainDiagonalStream()}, {@link #antiDiagonalStream()},
     * {@link #mainDiagonalPoints()}, and {@link #antiDiagonalPoints()}.
     *
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    protected void checkIsSquare() {
        N.checkState(rowCount == columnCount, MSG_MATRIX_NOT_SQUARE, rowCount, columnCount);
    }

}
