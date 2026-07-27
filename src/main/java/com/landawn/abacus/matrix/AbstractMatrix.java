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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
 * <p>Several APIs intentionally cross the usual defensive-copy boundary for performance-sensitive code:
 * {@link #unsafeBackingArray()} and {@link #rowView(int)} expose live storage, while
 * {@link #mutateFlattened(Throwables.Consumer)} lets callers mutate the matrix through a temporary
 * flattened array that is copied back afterward.
 * Callers that need isolation should prefer
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
 * <p><b>Reader/writer naming:</b> methods that read a row, column, or diagonal are named
 * {@code <artifact>View} when they expose live backing storage (for example {@link #rowView(int)}) and
 * {@code <artifact>Copy} when they return an independent copy (for example {@link #rowCopy(int)},
 * {@link #columnCopy(int)}, {@link #mainDiagonalCopy()}); there is deliberately no {@code get*} form, so
 * that the copy-versus-view distinction is always explicit at the call site. The corresponding writers
 * are named {@code set<Artifact>} for wholesale replacement (for example {@code setRow}, {@code setColumn},
 * {@code setMainDiagonal}) and {@code update<Artifact>} for operator-based, in-place transformation
 * (for example {@code updateRow}, {@code updateColumn}, {@code updateAll}).</p>
 *
 * <p><b>Element-wise zipping:</b> the instance {@code zipWith} methods combine this matrix with one or two
 * others of compatible shape and, for {@link Matrix}, an optional target element type. Their static
 * counterparts live in {@link Matrices}: {@code Matrices.zip} combines two, three, or a collection of
 * same-typed matrices, while {@code Matrices.zipToInt}, {@code zipToLong}, {@code zipToDouble}, and
 * {@code zipToObj} produce a differently typed result. Use the instance {@code zipWith} for fluent
 * two- or three-matrix combinations (including {@link Matrix}'s target-type overloads), and the
 * {@link Matrices} helpers when combining a collection or using a primitive cross-type specialization.</p>
 *
 * <p><b>Primitive conversions:</b> each numeric matrix converts to the {@code int}, {@code long},
 * {@code float}, and {@code double} matrix types (every such type except its own) through
 * {@code toIntMatrix}, {@code toLongMatrix}, {@code toFloatMatrix}, and {@code toDoubleMatrix}.
 * Conversions targeting the narrower {@code byte}, {@code char}, and {@code short} types, or
 * {@code boolean}, are intentionally omitted (they would be lossy or ill-defined); obtain those with an
 * explicit {@code map}/{@code mapToObj} step or by constructing the target matrix directly.</p>
 *
 * @param <A> the array type used for internal row storage (for example {@code int[]}, {@code double[]}, or {@code Object[]})
 * @param <PL> the flattened list type returned by {@link #flatten()} (for example {@code IntList} or {@code List<T>})
 * @param <ES> the element stream type returned by element-streaming methods such as {@link #rowMajorStream()}
 * @param <RS> the stream-of-streams type returned by {@link #rowStreams()} and {@link #columnStreams()}
 * @param <M> the concrete matrix type used for fluent return values
 */
public abstract sealed class AbstractMatrix<A, PL, ES, RS, M extends AbstractMatrix<A, PL, ES, RS, M>>
        permits BooleanMatrix, CharMatrix, ByteMatrix, ShortMatrix, DoubleMatrix, FloatMatrix, IntMatrix, LongMatrix, Matrix {

    /**
     * Row separator inserted between rows when a matrix is rendered as text, for example by
     * {@link #println()} and {@link #appendTo(Appendable)}.
     * Fixed to the Unix line separator ({@code "\n"}) so that printed output is consistent
     * across platforms.
     */
    protected static final String ARRAY_PRINT_SEPARATOR = IOUtil.LINE_SEPARATOR_UNIX;

    /**
     * Shared random source used by primitive matrix factories that produce randomized data
     * (for example {@code IntMatrix.randomRow(int)}). Backed by {@link SecureRandom} for higher-quality
     * sequences than the default {@link Random}.
     */
    protected static final Random RAND = new SecureRandom();

    /** The {@code char} default value ({@code '\0'}), used to fill newly introduced cells. */
    static final char CHAR_0 = (char) 0;

    /** The {@code byte} default value ({@code 0}), used to fill newly introduced cells. */
    static final byte BYTE_0 = (byte) 0;

    /** The {@code short} default value ({@code 0}), used to fill newly introduced cells. */
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
     * Exposed via {@link #unsafeBackingArray()}.
     */
    final A[] a;

    /**
     * Memoized result of {@link #hasAliasedRows()}: {@code 0} = not yet computed, {@code 1} = aliased,
     * {@code 2} = not aliased. The answer is invariant for the lifetime of the instance because {@link #a}
     * is {@code final} and the only structural mutation performed by this package is the row swap in
     * {@code flipVerticallyInPlace}, which permutes the same set of row references. Matrices are not
     * thread-safe, so this needs no synchronization.
     */
    private byte aliasedRowsState;

    /**
     * The element type tracked for this matrix. For primitive matrices this is the matching
     * primitive class (e.g. {@code int.class}); for {@link Matrix} it is the backing array
     * component type, or an explicit element type selected by the factory.
     * Read via {@link #elementType()}.
     */
    final Class<?> elementType;

    /**
     * Constructs a new {@code AbstractMatrix} with the specified two-dimensional array and element type.
     * The constructor validates that all rows are non-{@code null} and have the same length.
     * The supplied array is retained by reference and not defensively copied.
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

    /** Returns {@code source}, or a snapshot when it is one of this matrix's live backing rows. */
    final A snapshotIfBackingRow(final A source) {
        for (final A row : a) {
            if (row == source) {
                return cloneArray(source);
            }
        }

        return source;
    }

    /** Returns {@code source}, or a copy in which rows aliasing this matrix's live backing rows are replaced by snapshots. */
    final A[] snapshotRowsIfBackingRows(final A[] source) {
        if (a.length == 0) {
            return source;
        }

        if (source == a) {
            final A[] snapshot = source.clone();

            for (int i = 0; i < snapshot.length; i++) {
                if (snapshot[i] != null) {
                    snapshot[i] = cloneArray(snapshot[i]);
                }
            }

            return snapshot;
        }

        final Map<A, Boolean> backingRows = new IdentityHashMap<>(a.length);

        for (final A row : a) {
            backingRows.put(row, Boolean.TRUE);
        }

        A[] snapshot = source;

        for (int i = 0; i < source.length; i++) {
            if (backingRows.containsKey(source[i])) {
                if (snapshot == source) {
                    snapshot = source.clone();
                }

                snapshot[i] = cloneArray(source[i]);
            }
        }

        return snapshot;
    }

    /** Returns whether two or more logical rows share the same backing array. */
    final boolean hasAliasedRows() {
        if (aliasedRowsState == 0) {
            aliasedRowsState = computeHasAliasedRows() ? (byte) 1 : (byte) 2;
        }

        return aliasedRowsState == 1;
    }

    private boolean computeHasAliasedRows() {
        if (a.length < 2) {
            return false;
        }

        final Map<A, Boolean> seenRows = new IdentityHashMap<>(a.length);

        for (final A row : a) {
            if (seenRows.put(row, Boolean.TRUE) != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Performs {@code action} once for each distinct backing row, in first-occurrence order.
     * This prevents a row-wise in-place transformation from being applied repeatedly when the
     * outer backing array contains the same row reference more than once.
     *
     * @param <E> the type of exception that the action may throw
     * @param action the row transformation to perform
     * @throws E if the action throws an exception
     */
    final <E extends Exception> void forEachDistinctRow(final Throwables.Consumer<? super A, E> action) throws E {
        if (!hasAliasedRows()) {
            // Every row is distinct, so the identity set below would visit all of them anyway.
            // Skipping it keeps this the same cost as a plain loop for the overwhelmingly common case.
            for (final A row : a) {
                action.accept(row);
            }

            return;
        }

        final Map<A, Boolean> seenRows = new IdentityHashMap<>(a.length);

        for (final A row : a) {
            if (seenRows.put(row, Boolean.TRUE) == null) {
                action.accept(row);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <R> R cloneArray(final R source) {
        final int length = java.lang.reflect.Array.getLength(source);
        final Object copy = java.lang.reflect.Array.newInstance(source.getClass().getComponentType(), length);
        System.arraycopy(source, 0, copy, 0, length);
        return (R) copy;
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
        if (count < 0) {
            throw new IllegalStateException("Matrix stream element count cannot be negative: " + count);
        }

        if (count > Integer.MAX_VALUE) {
            throw new IllegalStateException("Matrix stream too large to convert to array: " + count + " elements");
        }

        return (int) count;
    }

    /**
     * Validates that the specified shape is representable by this matrix implementation.
     * Because dimensions are encoded by row arrays, a matrix with zero rows can only have zero columns.
     *
     * @param rowCount the row count; must be non-negative
     * @param columnCount the column count; must be non-negative
     * @throws IllegalArgumentException if {@code rowCount} or {@code columnCount} is negative, or if {@code rowCount == 0} while
     *         {@code columnCount != 0} (zero rows with a non-zero column count is not representable)
     */
    protected static void checkRepresentableShape(final int rowCount, final int columnCount) {
        N.checkArgument(rowCount >= 0, MSG_NEGATIVE_DIMENSION, "rowCount", rowCount);
        N.checkArgument(columnCount >= 0, MSG_NEGATIVE_DIMENSION, "columnCount", columnCount);
        N.checkArgument(rowCount > 0 || columnCount == 0, MSG_UNREPRESENTABLE_SHAPE, rowCount, columnCount);
    }

    /**
     * Validates that a newly materialized shape stays within the flat-cardinality limit used by
     * operations such as {@code reshape} that intentionally require compatibility with a single
     * flat array or list. Row-array operations that do not require flat materialization may support
     * a larger total cell count as long as each individual dimension is representable.
     *
     * @param rowCount the row count
     * @param columnCount the column count
     * @throws IllegalArgumentException if the total cell count {@code (long) rowCount * columnCount} exceeds {@code Integer.MAX_VALUE}
     */
    protected static void checkMaterializableShape(final int rowCount, final int columnCount) {
        if ((long) rowCount * columnCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Matrix dimensions overflow: " + rowCount + " x " + columnCount + " exceeds Integer.MAX_VALUE");
        }
    }

    /**
     * Returns {@code ceil(dividend / divisor)} for a non-negative {@code dividend} and a positive {@code divisor};
     * that is, the smallest non-negative integer {@code n} such that {@code n * divisor >= dividend}.
     *
     * <p>Used by reshape operations to compute the number of rows required to hold every element for a fixed
     * column count.</p>
     *
     * @param dividend the non-negative dividend (for example the matrix element count)
     * @param divisor the positive divisor (for example the target column count)
     * @return {@code ceil(dividend / divisor)}
     * @throws ArithmeticException if {@code divisor} is zero
     */
    protected static long ceilDiv(final long dividend, final long divisor) {
        return dividend % divisor == 0 ? dividend / divisor : dividend / divisor + 1;
    }

    /**
     * Formats matrix error message templates that use {@code "{}"} placeholders.
     * Each {@code "{}"} occurrence is replaced by the next argument's {@code String} representation,
     * processing arguments in order until either runs out.
     *
     * @param template the message template containing {@code "{}"} placeholders
     * @param args the arguments to substitute into the placeholders
     * @return the formatted message string; the {@code template} is returned unchanged if it is
     *         {@code null}, or if {@code args} is {@code null} or empty. Surplus placeholders that
     *         have no corresponding argument are left in the result as-is, and surplus arguments
     *         beyond the placeholder count are ignored.
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
     * For object matrices, this returns the backing array component type, or an explicit element type selected by the factory;
     * it may be a supertype of individual stored values.
     *
     * <p>This method is useful for reflection-based operations and type checking at runtime.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix intMatrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * intMatrix.elementType();                                  // returns int.class
     *
     * DoubleMatrix dblMatrix = DoubleMatrix.of(new double[][] {{1.0}});
     * dblMatrix.elementType();                                  // returns double.class
     *
     * Matrix<String> strMatrix = Matrix.of(new String[][] {{"a", "b"}, {"c", "d"}});
     * strMatrix.elementType();                                  // returns String.class
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.elementType();                                      // returns int.class (independent of size)
     * }</pre>
     *
     * @return the {@link Class} object representing the element type of this matrix
     */
    public Class<?> elementType() {
        return elementType;
    }

    /**
     * Returns the underlying two-dimensional array of this matrix.
     * This method exposes the internal array representation for performance reasons and should be used with caution
     * as modifications to the returned array will directly affect the matrix.
     *
     * <p><b>&#9888;&#65039; Unsafe API boundary:</b> This method returns the actual internal array, not a copy.
     * Any changes made to the returned array (including reassigning row references or mutating row contents)
     * will be reflected in this matrix. Reassigned rows must remain non-{@code null} and keep the original
     * {@link #columnCount()}; violating those shape invariants leaves the matrix in an invalid state because
     * its dimensions are cached at construction. If you need an independent matrix instance, use {@link #copy()}.
     * If you only need the data flattened into a single one-dimensional array, use {@link #flatten()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * int[][] array = matrix.unsafeBackingArray();
     * int rowArrayCount = array.length;                         // 2 (one entry per row)
     * array[0][0] = 10;                                         // WILL modify the matrix
     * matrix.get(0, 0);                                         // returns 10 (mutation visible through the matrix)
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * int emptyRowArrayCount = empty.unsafeBackingArray().length;   // 0 (zero-row matrix yields zero-length array)
     *
     * IntMatrix rowsNoCols = IntMatrix.of(new int[3][0]);
     * int zeroColumnRowCount = rowsNoCols.unsafeBackingArray().length; // 3 (3 x 0 matrix keeps 3 empty rows)
     * }</pre>
     *
     * @return the underlying two-dimensional array (not a copy); its length equals {@code rowCount}
     *         (so a {@code 0}-row matrix yields a zero-length array, but a {@code rowCount × 0} matrix
     *         yields a {@code rowCount}-length array of zero-length rows)
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public A[] unsafeBackingArray() {
        return a;
    }

    /**
     * Returns the specified row as a direct view backed by internal storage.
     * Changes to the returned array will modify this matrix.
     *
     * <p><b>&#9888;&#65039; Unsafe API boundary:</b> the returned row is a mutable alias to internal storage. Prefer
     * {@link #rowCopy(int)} unless you intentionally need to mutate the matrix through the row view.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * int[] row0 = matrix.rowView(0);                          // returns [1, 2, 3] (live reference)
     * row0[0] = 99;                                            // also changes matrix element at (0, 0)
     * matrix.get(0, 0);                                        // returns 99 (mutation visible through the matrix)
     * int[] row1 = matrix.rowView(1);                          // returns [4, 5, 6]
     *
     * matrix.rowView(-1);                                      // throws IndexOutOfBoundsException (negative index)
     * matrix.rowView(2);                                       // throws IndexOutOfBoundsException (index >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return the specified row array (direct reference to internal storage)
     * @throws IndexOutOfBoundsException if {@code rowIndex} is negative or {@code >= rowCount()}
     */
    public abstract A rowView(int rowIndex) throws IndexOutOfBoundsException;

    /**
     * Returns a defensive copy of the specified row.
     * Changes to the returned array do not affect this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * int[] rowCopy = matrix.rowCopy(0);                       // returns [1, 2, 3] (independent copy)
     * rowCopy[0] = 99;                                         // does NOT affect the original matrix
     * matrix.get(0, 0);                                        // returns 1 (original unchanged)
     * int[] last = matrix.rowCopy(1);                          // returns [4, 5, 6]
     *
     * matrix.rowCopy(-1);                                      // throws IndexOutOfBoundsException (negative index)
     * matrix.rowCopy(2);                                       // throws IndexOutOfBoundsException (index >= rowCount)
     * }</pre>
     *
     * @param rowIndex the index of the row to retrieve (0-based)
     * @return a new array containing the values from the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex} is negative or {@code >= rowCount()}
     */
    public abstract A rowCopy(int rowIndex) throws IndexOutOfBoundsException;

    /**
     * Returns a defensive copy of the specified column.
     * Changes to the returned array do not affect this matrix.
     *
     * <p><b>Note on the rowView/columnView asymmetry:</b> this class exposes
     * {@link #rowView(int)} but no {@code columnView(int)} counterpart. Matrices store
     * elements as one array per row, so a row is exactly one backing array and can be
     * aliased directly. A column is interleaved across rows
     * and cannot be returned as a live, single-array view without either copying or
     * synthesizing a wrapper. {@code columnCopy} is the supported accessor; for
     * element-by-element iteration over a column use {@link #columnMajorStream(int)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * int[] colCopy = matrix.columnCopy(1);                    // returns [2, 5] (independent copy)
     * colCopy[0] = 99;                                         // does NOT affect the original matrix
     * matrix.get(0, 1);                                        // returns 2 (original unchanged)
     * int[] col0 = matrix.columnCopy(0);                       // returns [1, 4]
     *
     * matrix.columnCopy(-1);                                   // throws IndexOutOfBoundsException (negative index)
     * matrix.columnCopy(3);                                    // throws IndexOutOfBoundsException (index >= columnCount)
     * }</pre>
     *
     * @param columnIndex the index of the column to retrieve (0-based)
     * @return a new array containing the values from the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex} is negative or {@code >= columnCount()}
     */
    public abstract A columnCopy(int columnIndex) throws IndexOutOfBoundsException;

    /**
     * Returns the number of rows in this matrix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.rowCount();                                       // returns 2
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{7, 8, 9}});
     * single.rowCount();                                       // returns 1
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.rowCount();                                        // returns 0
     *
     * IntMatrix rowsNoCols = IntMatrix.of(new int[3][0]);
     * rowsNoCols.rowCount();                                   // returns 3 (rows can exist with zero columns)
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
     * matrix.columnCount();                                    // returns 3
     *
     * IntMatrix tall = IntMatrix.of(new int[][] {{1}, {2}, {3}});
     * tall.columnCount();                                      // returns 1
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.columnCount();                                     // returns 0
     *
     * IntMatrix rowsNoCols = IntMatrix.of(new int[3][0]);
     * rowsNoCols.columnCount();                                // returns 0 (3 x 0 matrix)
     * }</pre>
     *
     * @return the number of columns
     */
    public int columnCount() {
        return columnCount;
    }

    /**
     * Returns the total number of elements in this matrix (rows × columns).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.elementCount();                                   // returns 6
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{42}});
     * single.elementCount();                                   // returns 1
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.elementCount();                                    // returns 0
     *
     * IntMatrix rowsNoCols = IntMatrix.of(new int[3][0]);
     * rowsNoCols.elementCount();                               // returns 0 (3 rows x 0 columns)
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
     * empty.isEmpty();                                         // returns true (0 x 0)
     *
     * IntMatrix rowsNoCols = IntMatrix.of(new int[3][0]);
     * rowsNoCols.isEmpty();                                    // returns true (3 x 0 still has 0 elements)
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{1}});
     * single.isEmpty();                                        // returns false (1 x 1)
     *
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * matrix.isEmpty();                                        // returns false (2 x 2)
     * }</pre>
     *
     * @return {@code true} if the matrix has no elements (count == 0), {@code false} otherwise
     */
    public boolean isEmpty() {
        return elementCount == 0;
    }

    /**
     * Returns a structural copy of this matrix.
     * The returned matrix has its own row arrays, so replacing elements or mutating primitive
     * cells through one matrix does not modify the other's storage.
     *
     * <p>For object matrices, element references are copied, not the referenced objects
     * themselves; mutating a shared mutable element can still be observed through both matrices.</p>
     *
     * <p>This method creates new array instances and copies all element values.
     * For large matrices, this operation can be memory and time intensive.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix original = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntMatrix copy = original.copy();
     * copy.equals(original);                                   // returns true (same shape and values)
     * copy.set(0, 0, 10);                                      // original matrix remains unchanged
     * copy.get(0, 0);                                          // returns 10
     * original.get(0, 0);                                      // returns 1 (independent storage)
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.copy().isEmpty();                                  // returns true (empty copy stays empty)
     * }</pre>
     *
     * @return a new matrix that is a copy of this matrix with the same dimensions and values
     */
    public abstract M copy();

    /**
     * Returns a copy of a row range from this matrix.
     * The returned matrix contains only the specified rows (with all columns) and has its own row arrays.
     * For object matrices, element references are copied, not the referenced objects themselves.
     *
     * <p>This is equivalent to calling {@code copy(fromRowIndex, toRowIndex, 0, columnCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}, {5, 6}});
     * IntMatrix sub = matrix.copy(0, 2);                       // returns {{1, 2}, {3, 4}}
     * sub.rowCount();                                          // returns 2
     *
     * IntMatrix lastRow = matrix.copy(2, 3);                   // returns {{5, 6}}
     *
     * IntMatrix none = matrix.copy(1, 1);                      // returns an empty 0 x 0 matrix
     * none.rowCount();                                         // returns 0
     *
     * matrix.copy(0, 4);                                       // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.copy(2, 1);                                       // throws IndexOutOfBoundsException (fromRowIndex > toRowIndex)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a new matrix containing the specified rows with dimensions
     *         {@code (toRowIndex - fromRowIndex) × columnCount}; when the row range is empty
     *         ({@code fromRowIndex == toRowIndex}) the result is an empty {@code 0 x 0} matrix
     *         (the column count is not preserved)
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount},
     *         or {@code fromRowIndex > toRowIndex}
     */
    public abstract M copy(int fromRowIndex, int toRowIndex);

    /**
     * Returns a copy of a rectangular region from this matrix.
     * The returned matrix contains only the specified rows and columns and has its own row arrays.
     * For object matrices, element references are copied, not the referenced objects themselves.
     *
     * <p>This method allows you to extract any rectangular subregion of the matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * IntMatrix sub = matrix.copy(0, 2, 1, 3);                 // returns {{2, 3}, {5, 6}} (rows 0-1, cols 1-2)
     * IntMatrix center = matrix.copy(1, 2, 1, 2);              // returns {{5}} (just the center element)
     *
     * IntMatrix emptyCols = matrix.copy(0, 2, 1, 1);           // returns a 2 x 0 matrix (empty column range)
     * emptyCols.rowCount();                                    // returns 2
     * emptyCols.columnCount();                                 // returns 0
     *
     * matrix.copy(0, 2, 0, 4);                                 // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.copy(2, 0, 0, 2);                                 // throws IndexOutOfBoundsException (fromRowIndex > toRowIndex)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a new matrix containing the specified region with dimensions
     *         {@code (toRowIndex - fromRowIndex) × (toColumnIndex - fromColumnIndex)}; when the row range is empty
     *         ({@code fromRowIndex == toRowIndex}) the result is an empty {@code 0 x 0} matrix
     *         (the column count is not preserved), whereas an empty column range with a non-empty row range
     *         correctly yields {@code (toRowIndex - fromRowIndex) x 0}
     * @throws IndexOutOfBoundsException if any index is out of bounds, {@code fromRowIndex > toRowIndex},
     *         or {@code fromColumnIndex > toColumnIndex}
     */
    public abstract M copy(int fromRowIndex, int toRowIndex, int fromColumnIndex, int toColumnIndex);

    /**
     * Returns a new matrix that is this matrix rotated 90 degrees clockwise.
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * row of the result being the first column of the original matrix read from bottom to top.
     * The original matrix is not modified.
     *
     * <p>Rotation formula: element at position {@code (i, j)} in the original matrix
     * moves to position {@code (j, rowCount - 1 - i)} in the rotated matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix original = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntMatrix rotated = original.rotate90();                 // returns {{4, 1}, {5, 2}, {6, 3}}
     * rotated.rowCount();                                      // returns 3 (2 x 3 becomes 3 x 2)
     * rotated.columnCount();                                   // returns 2
     * rotated.get(0, 0);                                       // returns 4
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{42}});
     * single.rotate90().get(0, 0);                             // returns 42 (1 x 1 unchanged)
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.rotate90().isEmpty();                              // returns true (empty rotates to empty)
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 90 degrees clockwise, with dimensions {@code columnCount × rowCount};
     *         a matrix with zero columns (an {@code N x 0} shape) rotates to the empty {@code 0 x 0} matrix, because the
     *         swapped shape {@code 0 x N} (zero rows with a non-zero column count) is not representable
     */
    public abstract M rotate90();

    /**
     * Returns a new matrix that is this matrix rotated 180 degrees.
     * This is equivalent to flipping both horizontally and vertically, reversing the
     * order of all elements. The resulting matrix has the same dimensions as the original.
     * The original matrix is not modified.
     *
     * <p>Rotation formula: element at position {@code (i, j)} in the original matrix
     * moves to position {@code (rowCount - 1 - i, columnCount - 1 - j)} in the rotated matrix.</p>
     *
     * <p>For non-degenerate matrices this operation is equivalent to calling {@code rotate90().rotate90()}.
     * The equivalence does not hold for {@code N x 0} shapes ({@code N > 0}): {@code rotate180()} preserves the
     * {@code N x 0} shape, whereas {@code rotate90().rotate90()} collapses it to {@code 0 x 0} because the
     * intermediate {@code 0 x N} shape is not representable.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix original = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntMatrix rotated = original.rotate180();                // returns {{6, 5, 4}, {3, 2, 1}}
     * rotated.rowCount();                                      // returns 2 (dimensions unchanged)
     * rotated.get(0, 0);                                       // returns 6
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{42}});
     * single.rotate180().get(0, 0);                            // returns 42 (1 x 1 unchanged)
     *
     * IntMatrix rowsNoCols = IntMatrix.of(new int[3][0]);
     * rowsNoCols.rotate180().rowCount();                       // returns 3 (3 x 0 shape preserved)
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 180 degrees, with the same dimensions ({@code rowCount × columnCount})
     */
    public abstract M rotate180();

    /**
     * Returns a new matrix that is this matrix rotated 270 degrees clockwise (or equivalently, 90 degrees counter-clockwise).
     * The resulting matrix has dimensions swapped (rows become columns), with the first
     * column of the result being the first row of the original matrix in reverse order.
     * The original matrix is not modified.
     *
     * <p>Rotation formula: element at position {@code (i, j)} in the original matrix
     * moves to position {@code (columnCount - 1 - j, i)} in the rotated matrix.</p>
     *
     * <p>This operation is equivalent to calling {@code rotate90().rotate90().rotate90()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix original = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntMatrix rotated = original.rotate270();                // returns {{3, 6}, {2, 5}, {1, 4}}
     * rotated.rowCount();                                      // returns 3 (2 x 3 becomes 3 x 2)
     * rotated.columnCount();                                   // returns 2
     * rotated.get(0, 0);                                       // returns 3
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{42}});
     * single.rotate270().get(0, 0);                            // returns 42 (1 x 1 unchanged)
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.rotate270().isEmpty();                             // returns true (empty rotates to empty)
     * }</pre>
     *
     * @return a new matrix that is this matrix rotated 270 degrees clockwise, with dimensions {@code columnCount × rowCount};
     *         a matrix with zero columns (an {@code N x 0} shape) rotates to the empty {@code 0 x 0} matrix, because the
     *         swapped shape {@code 0 x N} (zero rows with a non-zero column count) is not representable
     */
    public abstract M rotate270();

    /**
     * Returns a new matrix that is the transpose of this matrix.
     * The transpose operation swaps rows and columns, so the element at position {@code (i, j)}
     * in the original matrix appears at position {@code (j, i)} in the transposed matrix. The resulting
     * matrix has dimensions swapped ({@code rowCount × columnCount} becomes {@code columnCount × rowCount}).
     * The original matrix is not modified.
     *
     * <p>Transpose formula: element at position {@code (i, j)} in the original matrix
     * moves to position {@code (j, i)} in the transposed matrix.</p>
     *
     * <p>The transpose is a fundamental matrix operation used in linear algebra,
     * statistics, and many matrix algorithms.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix original = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntMatrix transposed = original.transpose();            // returns {{1, 4}, {2, 5}, {3, 6}}
     * transposed.rowCount();                                  // returns 3 (2 x 3 becomes 3 x 2)
     * transposed.columnCount();                               // returns 2
     * transposed.get(2, 1);                                   // returns 6
     *
     * IntMatrix square = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * square.transpose().get(0, 1);                          // returns 3
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.transpose().isEmpty();                           // returns true (empty transposes to empty)
     * }</pre>
     *
     * @return a new matrix that is the transpose of this matrix, with dimensions {@code columnCount × rowCount};
     *         a matrix with zero columns (an {@code N x 0} shape) transposes to the empty {@code 0 x 0} matrix, because the
     *         swapped shape {@code 0 x N} (zero rows with a non-zero column count) is not representable
     */
    public abstract M transpose();

    /**
     * Returns a new matrix with the elements of this matrix rearranged into the specified number of columns.
     * The number of rows is automatically calculated based on the total element count.
     * Elements are taken in row-major order from the original matrix and placed into the
     * new shape. If the total element count is not evenly divisible by the new column count,
     * the last row will be padded with default values ({@code 0} for numeric types, {@code false} for boolean, {@code null} for objects).
     * The original matrix is not modified.
     *
     * <p>The new row count is calculated as: {@code ceiling(elementCount / newColumnCount)}</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntMatrix reshaped = matrix.reshapeByColumnCount(2);                  // returns {{1, 2}, {3, 4}, {5, 6}}
     * reshaped.rowCount();                                                  // returns 3 (ceil(6 / 2))
     *
     * IntMatrix padded = matrix.reshapeByColumnCount(4);                    // returns {{1, 2, 3, 4}, {5, 6, 0, 0}}
     * padded.get(1, 3);                                                     // returns 0 (trailing cell padded)
     *
     * matrix.reshapeByColumnCount(0);                                       // throws IllegalArgumentException (newColumnCount <= 0)
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.reshapeByColumnCount(2);                                        // throws IllegalArgumentException (0 rows with positive column count is not representable)
     * }</pre>
     *
     * @param newColumnCount the number of columns in the reshaped matrix (must be positive)
     * @return a new matrix with the specified number of columns
     * @throws IllegalArgumentException if {@code newColumnCount <= 0}, if the implied row count
     *         {@code ceil(elementCount / newColumnCount)} exceeds {@code Integer.MAX_VALUE}, if the
     *         resulting shape is not representable (which occurs when this matrix is empty, since the
     *         implied row count is then {@code 0} while {@code newColumnCount} is positive), or if the
     *         total cell count {@code (long) newRowCount * newColumnCount} exceeds {@code Integer.MAX_VALUE}
     */
    public M reshapeByColumnCount(final int newColumnCount) {
        N.checkArgument(newColumnCount > 0, "newColumnCount must be positive, but got: {}", newColumnCount);

        final long newRowCount = ceilDiv(elementCount, newColumnCount);

        N.checkArgument(newRowCount <= Integer.MAX_VALUE, "Reshaped row count overflow: ceil({} / {}) = {} exceeds Integer.MAX_VALUE", elementCount,
                newColumnCount, newRowCount);

        checkRepresentableShape((int) newRowCount, newColumnCount);

        return reshape((int) newRowCount, newColumnCount);
    }

    /**
     * Returns a new matrix with the elements of this matrix rearranged into the specified dimensions.
     * Elements are taken in row-major order from the original matrix and placed into the
     * new shape. The new shape must have at least as many total elements as the original
     * ({@code (long) newRowCount * newColumnCount >= elementCount()}).
     * If the new shape has more elements, the extra positions are filled with
     * default values ({@code 0} for numeric types, {@code false} for boolean, {@code null} for objects).
     * The original matrix is not modified.
     *
     * <p>This is a fundamental operation for restructuring matrix data without changing
     * the underlying element values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntMatrix reshaped = matrix.reshape(3, 2);              // returns {{1, 2}, {3, 4}, {5, 6}}
     * reshaped.get(2, 1);                                     // returns 6
     *
     * IntMatrix extended = matrix.reshape(2, 4);             // returns {{1, 2, 3, 4}, {5, 6, 0, 0}}
     * extended.get(1, 3);                                    // returns 0 (extra cell padded with default)
     *
     * matrix.reshape(-1, 6);                                 // throws IllegalArgumentException (negative dimension)
     * matrix.reshape(1, 4);                                  // throws IllegalArgumentException (4 cells cannot hold 6 elements)
     * }</pre>
     *
     * @param newRowCount the number of rows in the reshaped matrix; must be non-negative
     * @param newColumnCount the number of columns in the reshaped matrix; must be non-negative
     * @return a new matrix with the specified dimensions ({@code newRowCount × newColumnCount})
     * @throws IllegalArgumentException if {@code newRowCount < 0} or {@code newColumnCount < 0}, if the
     *         requested shape is not representable (zero rows with a non-zero column count), if the total
     *         cell count {@code (long) newRowCount * newColumnCount} exceeds {@code Integer.MAX_VALUE}, or if the
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
     * m1.isSameShape(m2);                                      // returns true (both 2 x 2, values ignored)
     *
     * IntMatrix m3 = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * m1.isSameShape(m3);                                      // returns false (2 x 2 vs 2 x 3)
     *
     * IntMatrix e1 = IntMatrix.of(new int[0][0]);
     * IntMatrix e2 = IntMatrix.of(new int[0][0]);
     * e1.isSameShape(e2);                                      // returns true (both 0 x 0)
     *
     * m1.isSameShape((IntMatrix) null);                       // throws IllegalArgumentException (null argument)
     * }</pre>
     *
     * @param other the matrix to compare with
     * @return {@code true} if both matrices have the same dimensions, {@code false} otherwise
     * @throws IllegalArgumentException if {@code other} is {@code null}
     */
    public boolean isSameShape(final M other) {
        N.checkArgNotNull(other, "other");
        return rowCount == other.rowCount && columnCount == other.columnCount;
    }

    /**
     * Returns a new matrix with each element repeated the specified number of times in both dimensions.
     * Each element is expanded into a block of size {@code rowRepeats × columnRepeats}.
     * The resulting matrix has dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}.
     * The original matrix is not modified.
     *
     * <p>This operation is similar to MATLAB's {@code repelem} function. Each element becomes a block,
     * effectively creating a "zoomed in" version of the matrix where each original element
     * occupies multiple positions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntMatrix repeated = matrix.repeatElements(2, 2);       // returns {{1,1,2,2},{1,1,2,2},{3,3,4,4},{3,3,4,4}}
     * repeated.rowCount();                                    // returns 4 (2 * 2)
     * repeated.columnCount();                                 // returns 4 (2 * 2)
     * repeated.get(0, 1);                                     // returns 1
     *
     * IntMatrix sameMatrix = matrix.repeatElements(1, 1);    // returns a copy with identical values
     * sameMatrix.get(1, 1);                                  // returns 4
     *
     * matrix.repeatElements(0, 2);                           // throws IllegalArgumentException (rowRepeats < 1)
     * matrix.repeatElements(2, 0);                           // throws IllegalArgumentException (columnRepeats < 1)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat each element in the row direction (must be positive)
     * @param columnRepeats number of times to repeat each element in the column direction (must be positive)
     * @return a new matrix with repeated elements, with dimensions
     *         {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
     *         or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
     * @see <a href="https://www.mathworks.com/help/matlab/ref/repelem.html">MATLAB repelem function</a>
     */
    public abstract M repeatElements(int rowRepeats, int columnRepeats);

    /**
     * Returns a new matrix formed by tiling this matrix the specified number of times in both dimensions.
     * The matrix is tiled {@code rowRepeats} times vertically and {@code columnRepeats} times horizontally.
     * The resulting matrix has dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}.
     * The original matrix is not modified.
     *
     * <p>This operation is similar to MATLAB's {@code repmat} function. The entire matrix pattern
     * is replicated, creating a tiled arrangement.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntMatrix tiled = matrix.repeatMatrix(2, 2);           // returns {{1,2,1,2},{3,4,3,4},{1,2,1,2},{3,4,3,4}}
     * tiled.rowCount();                                      // returns 4 (2 * 2)
     * tiled.columnCount();                                   // returns 4 (2 * 2)
     * tiled.get(0, 2);                                       // returns 1 (pattern tiled, not element repeated)
     *
     * IntMatrix sameMatrix = matrix.repeatMatrix(1, 1);     // returns a copy with identical values
     * sameMatrix.get(1, 0);                                 // returns 3
     *
     * matrix.repeatMatrix(0, 2);                            // throws IllegalArgumentException (rowRepeats < 1)
     * matrix.repeatMatrix(2, 0);                            // throws IllegalArgumentException (columnRepeats < 1)
     * }</pre>
     *
     * @param rowRepeats number of times to repeat the matrix in the row direction (must be positive)
     * @param columnRepeats number of times to repeat the matrix in the column direction (must be positive)
     * @return a new matrix with this matrix tiled, with dimensions
     *         {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
     * @throws IllegalArgumentException if {@code rowRepeats} or {@code columnRepeats} is not positive,
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
     * {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntMatrix padded = matrix.extend(1, 1, 1, 1);          // 4 x 4 with the 2 x 2 centered and 0-padded
     * padded.rowCount();                                     // returns 4 (1 + 2 + 1)
     * padded.columnCount();                                  // returns 4 (1 + 2 + 1)
     * padded.get(0, 0);                                      // returns 0 (top-left padding)
     * padded.get(1, 1);                                      // returns 1 (original (0,0) shifted to center)
     *
     * IntMatrix unchanged = matrix.extend(0, 0, 0, 0);      // returns an identical 2 x 2 matrix
     * unchanged.get(1, 1);                                  // returns 4
     *
     * matrix.extend(-1, 0, 0, 0);                           // throws IllegalArgumentException (negative pad)
     * }</pre>
     *
     * @param padTop number of rows to add above the matrix (must be {@code >= 0})
     * @param padBottom number of rows to add below the matrix (must be {@code >= 0})
     * @param padLeft number of columns to add to the left of the matrix (must be {@code >= 0})
     * @param padRight number of columns to add to the right of the matrix (must be {@code >= 0})
     * @return a new matrix grown by the specified pad widths, with new cells filled with the type's default value
     * @throws IllegalArgumentException if any pad value is negative, if the resulting dimensions overflow {@code Integer.MAX_VALUE},
     *         or if the resulting shape is not representable (zero rows with a non-zero column count)
     */
    public abstract M extend(int padTop, int padBottom, int padLeft, int padRight);

    /**
     * Returns a new matrix that is a horizontal flip (mirror across the vertical axis) of this matrix.
     * Element at position {@code (i, j)} in this matrix appears at position {@code (i, columnCount - 1 - j)}
     * in the result. This matrix is not modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntMatrix flipped = matrix.flipHorizontally();          // returns {{3, 2, 1}, {6, 5, 4}}
     * flipped.get(0, 0);                                      // returns 3
     * matrix.get(0, 0);                                       // returns 1 (original unchanged)
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{42}});
     * single.flipHorizontally().get(0, 0);                    // returns 42 (1 x 1 unchanged)
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.flipHorizontally().isEmpty();                     // returns true
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * IntMatrix flipped = matrix.flipVertically();            // returns {{4, 5, 6}, {1, 2, 3}}
     * flipped.get(0, 0);                                      // returns 4
     * matrix.get(0, 0);                                       // returns 1 (original unchanged)
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{1}, {2}, {3}});
     * single.flipVertically().get(0, 0);                      // returns 3 (row order reversed)
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.flipVertically().isEmpty();                       // returns true
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.flipHorizontallyInPlace();                       // matrix becomes {{3, 2, 1}, {6, 5, 4}}
     * matrix.get(0, 0);                                       // returns 3 (this matrix mutated)
     * matrix.get(1, 2);                                       // returns 4
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.flipHorizontallyInPlace();                        // no-op; empty.isEmpty() stays true
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.flipVerticallyInPlace();                         // matrix becomes {{4, 5, 6}, {1, 2, 3}}
     * matrix.get(0, 0);                                       // returns 4 (this matrix mutated)
     * matrix.get(1, 0);                                       // returns 1
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.flipVerticallyInPlace();                          // no-op; empty.isEmpty() stays true
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix top = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntMatrix bottom = IntMatrix.of(new int[][] {{5, 6}});
     * IntMatrix stacked = top.stackVertically(bottom);        // returns {{1, 2}, {3, 4}, {5, 6}}
     * stacked.rowCount();                                     // returns 3 (2 + 1)
     * stacked.columnCount();                                  // returns 2 (unchanged)
     * stacked.get(2, 0);                                      // returns 5
     *
     * IntMatrix mismatch = IntMatrix.of(new int[][] {{7, 8, 9}});
     * top.stackVertically(mismatch);                          // throws IllegalArgumentException (column count mismatch)
     * top.stackVertically((IntMatrix) null);                  // throws IllegalArgumentException (null argument)
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix left = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * IntMatrix right = IntMatrix.of(new int[][] {{5}, {6}});
     * IntMatrix stacked = left.stackHorizontally(right);      // returns {{1, 2, 5}, {3, 4, 6}}
     * stacked.rowCount();                                     // returns 2 (unchanged)
     * stacked.columnCount();                                  // returns 3 (2 + 1)
     * stacked.get(0, 2);                                      // returns 5
     *
     * IntMatrix mismatch = IntMatrix.of(new int[][] {{7}, {8}, {9}});
     * left.stackHorizontally(mismatch);                       // throws IllegalArgumentException (row count mismatch)
     * left.stackHorizontally((IntMatrix) null);               // throws IllegalArgumentException (null argument)
     * }</pre>
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
     * IntList flat = matrix.flatten();                        // returns [1, 2, 3, 4] (row-major order)
     * flat.size();                                            // returns 4
     * flat.get(0);                                            // returns 1
     *
     * IntMatrix matrix2 = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix2.flatten();                                      // returns [1, 2, 3, 4, 5, 6]
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.flatten().size();                                 // returns 0 (empty matrix yields empty list)
     * }</pre>
     *
     * @return a new list containing all elements in row-major order with size equal to {@code elementCount}
     * @throws IllegalStateException if the element count exceeds {@code Integer.MAX_VALUE} and therefore
     *         cannot be materialized into a flat array
     */
    public abstract PL flatten();

    /**
     * Applies the specified operation to a temporary flattened (row-major order) representation of this matrix.
     * The operation receives a single one-dimensional array containing all elements in row-major order. If the
     * operation returns normally, the array is copied back into the matrix in row-major order; if the operation
     * throws, copy-back is not started.
     *
     * <p>This is useful for operations that are easier to perform on a flat array representation,
     * such as sorting all elements, applying statistical transformations, or batch updates.</p>
     *
     * <p><b>&#9888;&#65039; Unsafe API boundary:</b> the supplied action receives a mutable temporary array whose
     * contents can replace matrix state. If logical rows share a backing row array, their flattened segments cannot
     * remain independent during copy-back: rows are written in logical row order, so a later aliased row overwrites
     * values written through an earlier alias.</p>
     *
     * <p>A zero-row matrix does not invoke {@code action}. A matrix with one or more rows but zero columns invokes
     * {@code action} once with a zero-length array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{3, 1, 4}, {1, 5, 9}});
     * matrix.mutateFlattened(flat -> java.util.Arrays.sort(flat));   // sorts all elements in row-major order
     * matrix.get(0, 0);                                              // returns 1 (matrix becomes {{1, 1, 3}, {4, 5, 9}})
     * matrix.get(1, 2);                                              // returns 9
     *
     * matrix.mutateFlattened(flat -> { for (int i = 0; i < flat.length; i++) flat[i] *= 2; });   // doubles all elements
     * matrix.get(0, 0);                                                                          // returns 2
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * int[] zeroRowCalls = {0};
     * empty.mutateFlattened(flat -> zeroRowCalls[0]++);              // action is not invoked
     * int callbackCount = zeroRowCalls[0];                           // 0
     *
     * int[] shared = {1, 2};
     * IntMatrix aliased = IntMatrix.of(new int[][] {shared, shared});
     * aliased.mutateFlattened(flat -> { flat[0] = 10; flat[1] = 20; flat[2] = 30; flat[3] = 40; });
     * aliased.rowCopy(0);                                            // returns [30, 40] (later aliased row wins)
     *
     * // Checked exceptions propagate to the caller (do not wrap in try/catch inside the block)
     * matrix.mutateFlattened(flat -> { throw new java.io.IOException(); });   // throws IOException
     * }</pre>
     *
     * @param <E> the type of exception that the operation might throw
     * @param action the operation to apply to the one-dimensional flattened array (for example {@code int[]} for {@code IntMatrix})
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws ArithmeticException if {@code elementCount()} exceeds {@code Integer.MAX_VALUE} and therefore
     *         cannot be represented by one Java array
     * @throws E if the operation throws an exception
     */
    public abstract <E extends Exception> void mutateFlattened(Throwables.Consumer<? super A, E> action) throws E;

    /**
     * Performs the specified action for each element position in the matrix.
     * The action receives the row and column indices for each element.
     * Elements are processed in row-major order (row by row from left to right) when executed sequentially.
     * For large matrices the operation may be automatically parallelized, in which case the order in which
     * positions are visited is unspecified and the supplied action must be thread-safe; every position is
     * still visited exactly once. When logical rows share a backing row array, the operation runs sequentially
     * to avoid concurrent access to shared row storage and preserve deterministic row-major visitation.
     *
     * <p>This method is useful when you need to access matrix positions without caring about
     * the actual element values, or when the element access logic is handled inside the action.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     *
     * // Count every visited position
     * AtomicInteger visited = new AtomicInteger(0);
     * matrix.forEachIndices((i, j) -> visited.incrementAndGet());
     * visited.get();                                          // returns 9 (3 x 3 = 9 positions)
     *
     * // Count elements on the main diagonal
     * AtomicInteger diagonalCount = new AtomicInteger(0);
     * matrix.forEachIndices((i, j) -> { if (i == j) diagonalCount.incrementAndGet(); });
     * diagonalCount.get();                                    // returns 3
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.forEachIndices((i, j) -> visited.incrementAndGet());   // no positions; action never invoked
     *
     * matrix.forEachIndices((Throwables.IntBiConsumer<RuntimeException>) null);   // throws IllegalArgumentException (null action)
     * }</pre>
     *
     * @param <E> the type of exception that the action might throw
     * @param action the action to perform for each position, receives (rowIndex, columnIndex)
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEachIndices(final Throwables.IntBiConsumer<E> action) throws E {
        N.checkArgNotNull(action, "action");

        if (Matrices.shouldRunInParallel(this) && !hasAliasedRows()) {
            Matrices.forEachIndices(rowCount, columnCount, action, true);
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
     * Elements are processed in row-major order within the specified region when executed sequentially.
     * For large regions the operation may be automatically parallelized, in which case the order in which
     * positions are visited is unspecified and the supplied action must be thread-safe; every position is
     * still visited exactly once. When logical rows share a backing row array, the operation runs sequentially
     * to avoid concurrent access to shared row storage and preserve deterministic row-major visitation.
     *
     * <p>This allows selective processing of matrix subregions without creating a copy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * // Process only a 2x2 subregion starting at (1,1)
     * AtomicInteger visited = new AtomicInteger(0);
     * matrix.forEachIndices(1, 3, 1, 3, (i, j) -> visited.incrementAndGet());
     * visited.get();                                          // returns 4 (2 rows x 2 cols)
     *
     * // Process only the first column
     * AtomicInteger colCount = new AtomicInteger(0);
     * matrix.forEachIndices(0, matrix.rowCount(), 0, 1, (i, j) -> colCount.incrementAndGet());
     * colCount.get();                                         // returns 3 (one per row)
     *
     * // Empty range visits nothing
     * matrix.forEachIndices(1, 1, 0, 5, (i, j) -> visited.incrementAndGet());   // action never invoked
     *
     * matrix.forEachIndices(0, 4, 0, 5, (i, j) -> {});        // throws IndexOutOfBoundsException (toRowIndex > rowCount)
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
        N.checkArgNotNull(action, "action");
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        if (Matrices.shouldRunInParallel(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex)) && !hasAliasedRows()) {
            Matrices.forEachIndices(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex, action, true);
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
     * Elements are processed in row-major order (row by row from left to right) when executed sequentially.
     * For large matrices the operation may be automatically parallelized, in which case the order in which
     * positions are visited is unspecified and the supplied action must be thread-safe; every position is
     * still visited exactly once. When logical rows share a backing row array, the operation runs sequentially
     * to avoid concurrent access to shared row storage and preserve deterministic row-major visitation.
     *
     * <p>This variant is useful when the action needs access to matrix elements or methods,
     * allowing you to read/write values or use matrix operations within the action.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     *
     * // Sum every element via the supplied matrix reference
     * AtomicInteger sum = new AtomicInteger(0);
     * matrix.forEachIndices((i, j, m) -> sum.addAndGet(m.get(i, j)));
     * sum.get();                                              // returns 10 (1 + 2 + 3 + 4)
     *
     * // Force row-major execution for an in-place update whose order should be explicit
     * Matrices.runWithParallelMode(ParallelMode.FORCE_OFF,
     *         () -> matrix.forEachIndices((i, j, m) -> m.set(i, j, i + j)));
     * matrix.get(1, 1);                                       // returns 2 (1 + 1)
     * matrix.get(0, 0);                                       // returns 0
     *
     * matrix.forEachIndices((Throwables.BiIntObjConsumer<IntMatrix, RuntimeException>) null);   // throws IllegalArgumentException (null action)
     * }</pre>
     *
     * @param <E> the type of exception that the action might throw
     * @param action the action to perform, receiving (rowIndex, columnIndex, matrix)
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void forEachIndices(final Throwables.BiIntObjConsumer<M, E> action) throws E {
        N.checkArgNotNull(action, "action");

        final M matrix = (M) this;

        if (Matrices.shouldRunInParallel(this) && !hasAliasedRows()) {
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
     * Elements are processed in row-major order within the specified region when executed sequentially.
     * For large regions the operation may be automatically parallelized, in which case the order in which
     * positions are visited is unspecified and the supplied action must be thread-safe; every position is
     * still visited exactly once. When logical rows share a backing row array, the operation runs sequentially
     * to avoid concurrent access to shared row storage and preserve deterministic row-major visitation.
     *
     * <p>This combines region-based iteration with matrix access, allowing you to process
     * a subregion while having access to the entire matrix.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * // Sum the 2x2 subregion (rows 1-2, cols 1-2): 7 + 8 + 12 + 13
     * AtomicInteger regionSum = new AtomicInteger(0);
     * matrix.forEachIndices(1, 3, 1, 3, (i, j, m) -> regionSum.addAndGet(m.get(i, j)));
     * regionSum.get();                                        // returns 40
     *
     * // Negate every element in the first column with explicit row-major execution
     * Matrices.runWithParallelMode(ParallelMode.FORCE_OFF,
     *         () -> matrix.forEachIndices(0, matrix.rowCount(), 0, 1,
     *                 (i, j, m) -> m.set(i, j, -m.get(i, j))));
     * matrix.get(0, 0);                                       // returns -1
     * matrix.get(2, 0);                                       // returns -11
     *
     * matrix.forEachIndices(0, 3, 0, 6, (i, j, m) -> {});     // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
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
        N.checkArgNotNull(action, "action");
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);
        N.checkFromToIndex(fromColumnIndex, toColumnIndex, columnCount);

        final M matrix = (M) this;

        if (Matrices.shouldRunInParallel(this, ((long) (toRowIndex - fromRowIndex)) * (toColumnIndex - fromColumnIndex)) && !hasAliasedRows()) {
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
     * matrix.adjacent4Points(0, 0).count();                   // returns 2 (only right and down exist at a corner)
     * // points are Point.of(0, 1) and Point.of(1, 0)
     *
     * // Center position has all 4 neighbors (up, right, down, left)
     * IntMatrix larger = IntMatrix.of(new int[3][3]);
     * larger.adjacent4Points(1, 1).count();                   // returns 4: (0,1), (1,2), (2,1), (1,0)
     *
     * // Edge (non-corner) position has 3 neighbors
     * larger.adjacent4Points(0, 1).count();                   // returns 3 (no up)
     *
     * matrix.adjacent4Points(2, 0);                           // throws IndexOutOfBoundsException (row out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return a stream of adjacent points in the four cardinal directions (0 to 4 points depending on position)
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
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
     * IntMatrix matrix = IntMatrix.of(new int[3][3]);
     * matrix.adjacent8Points(1, 1).count();                   // returns 8 (center has all neighbors)
     *
     * // Corner position has only 3 neighbors: (0,1), (1,1), (1,0)
     * matrix.adjacent8Points(0, 0).count();                   // returns 3
     *
     * // Edge (non-corner) position has 5 neighbors
     * matrix.adjacent8Points(0, 1).count();                   // returns 5
     *
     * matrix.adjacent8Points(0, 3);                           // throws IndexOutOfBoundsException (column out of bounds)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @param columnIndex the column index (0-based)
     * @return a stream of adjacent points in all 8 directions (0 to 8 points depending on position)
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
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
     * matrix.mainDiagonalPoints().toList();                   // returns [(0,0), (1,1), (2,2)]
     * matrix.mainDiagonalPoints().count();                    // returns 3
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{42}});
     * single.mainDiagonalPoints().count();                    // returns 1: [(0,0)]
     *
     * IntMatrix nonSquare = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.mainDiagonalPoints();                         // throws IllegalStateException (not square)
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
     * The anti-diagonal consists of the elements where {@code rowIndex + columnIndex == rowCount - 1}.
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>The anti-diagonal runs from the upper-right corner to the lower-left corner.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.antiDiagonalPoints().toList();                   // returns [(0,2), (1,1), (2,0)]
     * matrix.antiDiagonalPoints().count();                    // returns 3
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{42}});
     * single.antiDiagonalPoints().count();                    // returns 1: [(0,0)]
     *
     * IntMatrix nonSquare = IntMatrix.of(new int[][] {{1, 2}, {3, 4}, {5, 6}});
     * nonSquare.antiDiagonalPoints();                         // throws IllegalStateException (not square)
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * int[] diag = matrix.mainDiagonalCopy();                  // returns [1, 5, 9]
     * diag[0] = 99;                                            // copy; does NOT affect the matrix
     * matrix.get(0, 0);                                        // returns 1 (original unchanged)
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{42}});
     * single.mainDiagonalCopy();                               // returns [42]
     *
     * IntMatrix nonSquare = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.mainDiagonalCopy();                            // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new array containing the main diagonal values
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    public abstract A mainDiagonalCopy();

    /**
     * Sets the elements on the main diagonal (upper-left to lower-right).
     * The matrix must be square (rowCount == columnCount), and the supplied array must contain
     * exactly {@code rowCount} elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.setMainDiagonal(new int[] {10, 20, 30});
     * matrix.get(0, 0);                                       // returns 10
     * matrix.get(2, 2);                                       // returns 30
     * matrix.get(0, 1);                                       // returns 2 (off-diagonal unchanged)
     *
     * matrix.setMainDiagonal(new int[] {1, 2});              // throws IllegalArgumentException (length != rowCount)
     *
     * IntMatrix nonSquare = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.setMainDiagonal(new int[] {1, 2});           // throws IllegalStateException (not square)
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * int[] diag = matrix.antiDiagonalCopy();                  // returns [3, 5, 7]
     * diag[0] = 99;                                            // copy; does NOT affect the matrix
     * matrix.get(0, 2);                                        // returns 3 (original unchanged)
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{42}});
     * single.antiDiagonalCopy();                               // returns [42]
     *
     * IntMatrix nonSquare = IntMatrix.of(new int[][] {{1, 2}, {3, 4}, {5, 6}});
     * nonSquare.antiDiagonalCopy();                            // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a new array containing the anti-diagonal values
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    public abstract A antiDiagonalCopy();

    /**
     * Sets the elements on the anti-diagonal (upper-right to lower-left).
     * The matrix must be square (rowCount == columnCount), and the supplied array must contain
     * exactly {@code rowCount} elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.setAntiDiagonal(new int[] {10, 20, 30});
     * matrix.get(0, 2);                                       // returns 10 (top-right corner)
     * matrix.get(2, 0);                                       // returns 30 (bottom-left corner)
     * matrix.get(0, 0);                                       // returns 1 (off-anti-diagonal unchanged)
     *
     * matrix.setAntiDiagonal(new int[] {1, 2});              // throws IllegalArgumentException (length != rowCount)
     *
     * IntMatrix nonSquare = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.setAntiDiagonal(new int[] {1, 2});           // throws IllegalStateException (not square)
     * }</pre>
     *
     * @param antiDiagonal the new values for the anti-diagonal; must be non-{@code null} and have length equal to {@code rowCount}
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     * @throws IllegalArgumentException if {@code antiDiagonal} is {@code null} or its length does not equal {@code rowCount}
     */
    public abstract void setAntiDiagonal(A antiDiagonal);

    /**
     * Returns a stream of all points in the matrix in row-major order.
     * Points are generated row by row from left to right, top to bottom.
     *
     * <p>This is equivalent to calling {@code rowMajorPoints(0, rowCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * matrix.rowMajorPoints().toList();                     // returns [(0,0), (0,1), (1,0), (1,1)]
     * matrix.rowMajorPoints().count();                      // returns 4
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{1, 2, 3}});
     * single.rowMajorPoints().count();                      // returns 3
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.rowMajorPoints().count();                       // returns 0
     * }</pre>
     *
     * @return a stream of all {@link Point} objects in row-major order
     */
    public Stream<Point> rowMajorPoints() {
        return rowMajorPoints(0, rowCount);
    }

    /**
     * Returns a stream of points for a specific row in row-major order (left to right).
     *
     * <p>This is equivalent to calling {@code rowMajorPoints(rowIndex, rowIndex + 1)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     * matrix.rowMajorPoints(1).toList();                    // returns [(1,0), (1,1), (1,2), (1,3), (1,4)]
     * matrix.rowMajorPoints(1).count();                     // returns 5 (one per column)
     *
     * matrix.rowMajorPoints(0).count();                     // returns 5 (first row)
     *
     * matrix.rowMajorPoints(3);                             // throws IndexOutOfBoundsException (rowIndex >= rowCount)
     * matrix.rowMajorPoints(-1);                            // throws IndexOutOfBoundsException (negative index)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @return a stream of {@link Point} objects for all columns in the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    public Stream<Point> rowMajorPoints(final int rowIndex) {
        checkRowIndex(rowIndex);

        return rowMajorPoints(rowIndex, rowIndex + 1);
    }

    /**
     * Returns a stream of points for a range of rows in row-major order.
     * Points are generated row by row from left to right for the specified row range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * // Get points from rows 1 and 2 (indices 1 and 2, not including 3)
     * matrix.rowMajorPoints(1, 3).count();                  // returns 10 (2 rows x 5 columns)
     * // points: (1,0), (1,1), (1,2), (1,3), (1,4), (2,0), (2,1), (2,2), (2,3), (2,4)
     *
     * matrix.rowMajorPoints(1, 1).count();                  // returns 0 (empty range)
     *
     * matrix.rowMajorPoints(0, 4);                          // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.rowMajorPoints(2, 1);                          // throws IndexOutOfBoundsException (fromRowIndex > toRowIndex)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a stream of {@link Point} objects in the specified row range, in row-major order
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex}
     */
    @SuppressWarnings("resource")
    public Stream<Point> rowMajorPoints(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromRowIndex, toRowIndex, rowCount);

        return IntStream.range(fromRowIndex, toRowIndex)
                .flatMapToObj(rowIndex -> IntStream.range(0, columnCount).mapToObj(columnIndex -> Point.of(rowIndex, columnIndex)));
    }

    /**
     * Returns a stream of all points in the matrix in column-major order.
     * Points are generated column by column from top to bottom, left to right.
     *
     * <p>This is equivalent to calling {@code columnMajorPoints(0, columnCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * matrix.columnMajorPoints().toList();                       // returns [(0,0), (1,0), (0,1), (1,1)]
     * matrix.columnMajorPoints().count();                        // returns 4
     *
     * IntMatrix wide = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * wide.columnMajorPoints().count();                          // returns 6
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.columnMajorPoints().count();                         // returns 0
     * }</pre>
     *
     * @return a stream of all {@link Point} objects in column-major order
     */
    public Stream<Point> columnMajorPoints() {
        return columnMajorPoints(0, columnCount);
    }

    /**
     * Returns a stream of points for a specific column in column-major order (top to bottom).
     *
     * <p>This is equivalent to calling {@code columnMajorPoints(columnIndex, columnIndex + 1)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     * matrix.columnMajorPoints(2).toList();                      // returns [(0,2), (1,2), (2,2)]
     * matrix.columnMajorPoints(2).count();                       // returns 3 (one per row)
     *
     * matrix.columnMajorPoints(0).count();                       // returns 3 (first column)
     *
     * matrix.columnMajorPoints(5);                               // throws IndexOutOfBoundsException (columnIndex >= columnCount)
     * matrix.columnMajorPoints(-1);                              // throws IndexOutOfBoundsException (negative index)
     * }</pre>
     *
     * @param columnIndex the column index (0-based)
     * @return a stream of {@link Point} objects for all rows in the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    public Stream<Point> columnMajorPoints(final int columnIndex) {
        checkColumnIndex(columnIndex);

        return columnMajorPoints(columnIndex, columnIndex + 1);
    }

    /**
     * Returns a stream of points for a range of columns in column-major order.
     * Points are generated column by column from top to bottom for the specified column range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}});
     *
     * // Get points from columns 1 through 3 (indices 1, 2, 3, not including 4)
     * matrix.columnMajorPoints(1, 4).count();                    // returns 9 (3 columns x 3 rows)
     * // points: (0,1), (1,1), (2,1), (0,2), (1,2), (2,2), (0,3), (1,3), (2,3)
     *
     * matrix.columnMajorPoints(2, 2).count();                    // returns 0 (empty range)
     *
     * matrix.columnMajorPoints(0, 6);                            // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.columnMajorPoints(3, 1);                            // throws IndexOutOfBoundsException (fromColumnIndex > toColumnIndex)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a stream of {@link Point} objects in the specified column range, in column-major order
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount}, or {@code fromColumnIndex > toColumnIndex}
     */
    @SuppressWarnings("resource")
    public Stream<Point> columnMajorPoints(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException {
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
     * matrix.rowPoints().count();                             // returns 3 (one inner stream per row)
     *
     * // Each inner stream has columnCount points
     * List<List<Point>> rows = matrix.rowPoints().map(Stream::toList).toList();
     * rows.get(0).size();                                     // returns 5
     * rows.get(0).get(0);                                     // returns Point.of(0, 0)
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.rowPoints().count();                              // returns 0
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
     * matrix.rowPoints(1, 3).count();                         // returns 2 (one inner stream per selected row)
     * List<List<Point>> rows = matrix.rowPoints(1, 3).map(Stream::toList).toList();
     * rows.get(0).get(0);                                     // returns Point.of(1, 0)
     *
     * matrix.rowPoints(1, 1).count();                         // returns 0 (empty range)
     *
     * matrix.rowPoints(0, 4);                                 // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a stream of streams, where each inner stream contains {@link Point} objects for one row
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex}
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
     * matrix.columnPoints().count();                          // returns 5 (one inner stream per column)
     *
     * // Each inner stream has rowCount points
     * List<List<Point>> cols = matrix.columnPoints().map(Stream::toList).toList();
     * cols.get(0).size();                                     // returns 3
     * cols.get(0).get(1);                                     // returns Point.of(1, 0)
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.columnPoints().count();                           // returns 0
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
     * matrix.columnPoints(2, 5).count();                      // returns 3 (one inner stream per selected column)
     * List<List<Point>> cols = matrix.columnPoints(2, 5).map(Stream::toList).toList();
     * cols.get(0).get(0);                                     // returns Point.of(0, 2)
     *
     * matrix.columnPoints(2, 2).count();                      // returns 0 (empty range)
     *
     * matrix.columnPoints(0, 6);                              // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a stream of streams, where each inner stream contains {@link Point} objects for one column
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount}, or {@code fromColumnIndex > toColumnIndex}
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
     * matrix.mainDiagonalStream().toArray();                  // returns [1, 5, 9]
     * matrix.mainDiagonalStream().sum();                      // returns 15
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{42}});
     * single.mainDiagonalStream().sum();                      // returns 42
     *
     * IntMatrix nonSquare = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * nonSquare.mainDiagonalStream();                         // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a stream of diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    public abstract ES mainDiagonalStream();

    /**
     * Returns a stream of elements along the anti-diagonal (upper-right to lower-left).
     * The anti-diagonal consists of the elements where {@code rowIndex + columnIndex == rowCount - 1}.
     * The matrix must be square (rowCount == columnCount) for this operation.
     *
     * <p>The anti-diagonal runs from the upper-right corner to the lower-left corner.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
     * matrix.antiDiagonalStream().toArray();                  // returns [3, 5, 7]
     * matrix.antiDiagonalStream().sum();                      // returns 15
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{42}});
     * single.antiDiagonalStream().sum();                      // returns 42
     *
     * IntMatrix nonSquare = IntMatrix.of(new int[][] {{1, 2}, {3, 4}, {5, 6}});
     * nonSquare.antiDiagonalStream();                         // throws IllegalStateException (not square)
     * }</pre>
     *
     * @return a stream of anti-diagonal elements
     * @throws IllegalStateException if the matrix is not square (rowCount != columnCount)
     */
    public abstract ES antiDiagonalStream();

    /**
     * Returns a stream of all elements in row-major order.
     * Elements are streamed row by row from left to right, top to bottom.
     *
     * <p>This is equivalent to calling {@code rowMajorStream(0, rowCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * matrix.rowMajorStream().toArray();                    // returns [1, 2, 3, 4] (row-major)
     * matrix.rowMajorStream().sum();                        // returns 10
     *
     * IntMatrix wide = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * wide.rowMajorStream().count();                        // returns 6
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.rowMajorStream().count();                       // returns 0
     * }</pre>
     *
     * @return a stream of all elements in row-major order
     */
    public abstract ES rowMajorStream();

    /**
     * Returns a stream of elements from a specific row.
     * Elements are streamed from left to right within the row.
     *
     * <p>This streams the elements of the single specified row, flattened into one stream. To
     * instead obtain every row as its own stream (a stream of streams), use {@link #rowStreams()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.rowMajorStream(1).toArray();                  // returns [4, 5, 6]
     * matrix.rowMajorStream(1).max().orElse(0);            // returns 6
     *
     * matrix.rowMajorStream(0).sum();                       // returns 6 (1 + 2 + 3)
     *
     * matrix.rowMajorStream(2);                             // throws IndexOutOfBoundsException (rowIndex >= rowCount)
     * matrix.rowMajorStream(-1);                            // throws IndexOutOfBoundsException (negative index)
     * }</pre>
     *
     * @param rowIndex the row index (0-based)
     * @return a stream of elements in the specified row
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     * @see #rowStreams()
     */
    public abstract ES rowMajorStream(final int rowIndex);

    /**
     * Returns a stream of elements from a range of rows in row-major order.
     * Elements are streamed row by row from left to right for the specified row range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}, {5, 6}});
     * matrix.rowMajorStream(1, 3).toArray();                // returns [3, 4, 5, 6]
     * matrix.rowMajorStream(1, 3).count();                  // returns 4
     *
     * matrix.rowMajorStream(1, 1).count();                  // returns 0 (empty range)
     *
     * matrix.rowMajorStream(0, 4);                          // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * matrix.rowMajorStream(2, 1);                          // throws IndexOutOfBoundsException (fromRowIndex > toRowIndex)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a stream of elements in the specified row range
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex}
     */
    public abstract ES rowMajorStream(final int fromRowIndex, final int toRowIndex);

    /**
     * Returns a stream of all elements in column-major order.
     * Elements are streamed column by column from top to bottom, left to right.
     *
     * <p>This is equivalent to calling {@code columnMajorStream(0, columnCount)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * matrix.columnMajorStream().toArray();                      // returns [1, 3, 2, 4] (column-major)
     * matrix.columnMajorStream().sum();                          // returns 10
     *
     * IntMatrix wide = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * wide.columnMajorStream().count();                          // returns 6
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.columnMajorStream().count();                         // returns 0
     * }</pre>
     *
     * @return a stream of all elements in column-major order
     */
    public abstract ES columnMajorStream();

    /**
     * Returns a stream of elements from a specific column.
     * Elements are streamed from top to bottom within the column.
     *
     * <p>This streams the elements of the single specified column, flattened into one stream. To
     * instead obtain every column as its own stream (a stream of streams), use {@link #columnStreams()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.columnMajorStream(1).toArray();                    // returns [2, 5]
     * matrix.columnMajorStream(1).min().orElse(0);              // returns 2
     *
     * matrix.columnMajorStream(0).sum();                         // returns 5 (1 + 4)
     *
     * matrix.columnMajorStream(3);                               // throws IndexOutOfBoundsException (columnIndex >= columnCount)
     * matrix.columnMajorStream(-1);                              // throws IndexOutOfBoundsException (negative index)
     * }</pre>
     *
     * @param columnIndex the column index (0-based)
     * @return a stream of elements in the specified column
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     * @see #columnStreams()
     */
    public abstract ES columnMajorStream(final int columnIndex);

    /**
     * Returns a stream of elements from a range of columns in column-major order.
     * Elements are streamed column by column from top to bottom for the specified column range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.columnMajorStream(1, 3).toArray();                 // returns [2, 5, 3, 6] (column-major)
     * matrix.columnMajorStream(1, 3).average().orElse(0);       // returns 4.0
     *
     * matrix.columnMajorStream(1, 1).count();                    // returns 0 (empty range)
     *
     * matrix.columnMajorStream(0, 4);                            // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * matrix.columnMajorStream(2, 1);                            // throws IndexOutOfBoundsException (fromColumnIndex > toColumnIndex)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a stream of elements in the specified column range
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount}, or {@code fromColumnIndex > toColumnIndex}
     */
    public abstract ES columnMajorStream(final int fromColumnIndex, final int toColumnIndex);

    /**
     * Returns a stream of row streams.
     * Each element in the outer stream is a stream representing one row of the matrix.
     * This allows for per-row processing using stream operations.
     *
     * <p>This is equivalent to calling {@code rowStreams(0, rowCount)}.</p>
     *
     * <p>This yields one stream per row. To instead stream the elements of a single row as one
     * flat stream, use {@link #rowMajorStream(int)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2, 3}, {4, 5, 6}});
     * matrix.rowStreams().count();                            // returns 2 (one inner stream per row)
     *
     * // Sum each row into a list
     * List<Integer> rowSums = matrix.rowStreams().map(s -> s.sum()).toList();
     * rowSums.get(0);                                         // returns 6
     * rowSums.get(1);                                         // returns 15
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.rowStreams().count();                             // returns 0
     * }</pre>
     *
     * @return a stream of row streams
     * @see #rowMajorStream(int)
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
     * matrix.rowStreams(1, 3).count();                        // returns 2 (one inner stream per selected row)
     * List<Integer> maxes = matrix.rowStreams(1, 3).map(s -> s.max().orElse(0)).toList();
     * maxes.get(0);                                           // returns 10 (max of row 1)
     *
     * matrix.rowStreams(1, 1).count();                        // returns 0 (empty range)
     *
     * matrix.rowStreams(0, 4);                                // throws IndexOutOfBoundsException (toRowIndex > rowCount)
     * }</pre>
     *
     * @param fromRowIndex the starting row index (inclusive, 0-based)
     * @param toRowIndex the ending row index (exclusive)
     * @return a stream of row streams for the specified range
     * @throws IndexOutOfBoundsException if {@code fromRowIndex < 0}, {@code toRowIndex > rowCount}, or {@code fromRowIndex > toRowIndex}
     */
    public abstract RS rowStreams(final int fromRowIndex, final int toRowIndex);

    /**
     * Returns a stream of column streams.
     * Each element in the outer stream is a stream representing one column of the matrix.
     * This allows for per-column processing using stream operations.
     *
     * <p>This is equivalent to calling {@code columnStreams(0, columnCount)}.</p>
     *
     * <p>This yields one stream per column. To instead stream the elements of a single column as one
     * flat stream, use {@link #columnMajorStream(int)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleMatrix matrix = DoubleMatrix.of(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
     * matrix.columnStreams().count();                         // returns 2 (one inner stream per column)
     *
     * // Average each column into a list
     * List<Double> colAvgs = matrix.columnStreams().map(s -> s.average().orElse(0)).toList();
     * colAvgs.get(0);                                         // returns 2.0 (avg of column [1.0, 3.0])
     * colAvgs.get(1);                                         // returns 3.0 (avg of column [2.0, 4.0])
     *
     * DoubleMatrix empty = DoubleMatrix.of(new double[0][0]);
     * empty.columnStreams().count();                          // returns 0
     * }</pre>
     *
     * @return a stream of column streams
     * @see #columnMajorStream(int)
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
     * matrix.columnStreams(2, 5).count();                     // returns 3 (one inner stream per selected column)
     * List<Integer> colSums = matrix.columnStreams(2, 5).map(s -> s.sum()).toList();
     * colSums.get(0);                                         // returns 24 (column 2: 3 + 8 + 13)
     *
     * matrix.columnStreams(2, 2).count();                     // returns 0 (empty range)
     *
     * matrix.columnStreams(0, 6);                             // throws IndexOutOfBoundsException (toColumnIndex > columnCount)
     * }</pre>
     *
     * @param fromColumnIndex the starting column index (inclusive, 0-based)
     * @param toColumnIndex the ending column index (exclusive)
     * @return a stream of column streams for the specified range
     * @throws IndexOutOfBoundsException if {@code fromColumnIndex < 0}, {@code toColumnIndex > columnCount}, or {@code fromColumnIndex > toColumnIndex}
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
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     *
     * // Capture a value via side effect
     * int[] holder = new int[1];
     * matrix.accept(m -> holder[0] = m.rowCount() * m.columnCount());
     * int elementCount = holder[0];                           // 4
     *
     * // Modify matrix elements in place
     * matrix.accept(m -> { for (int i = 0; i < m.rowCount(); i++) m.set(i, 0, 0); });   // zero the first column
     * matrix.get(0, 0);                                                                 // returns 0
     * matrix.get(1, 0);                                                                 // returns 0
     *
     * // A non-empty matrix passes a validation guard without throwing
     * matrix.accept(m -> { if (m.isEmpty()) throw new IllegalStateException(); });   // no exception
     *
     * matrix.accept((Throwables.Consumer<IntMatrix, RuntimeException>) null);   // throws IllegalArgumentException (null action)
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
     * long count = matrix.apply(AbstractMatrix::elementCount);               // count == 6L
     * matrix.apply(m -> "Matrix " + m.rowCount() + "x" + m.columnCount());   // returns "Matrix 2x3"
     *
     * // Transform matrix into a different representation
     * matrix.apply(IntMatrix::flatten).size();               // returns 6
     *
     * matrix.apply((Throwables.Function<IntMatrix, Long, RuntimeException>) null);   // throws IllegalArgumentException (null mapper)
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
     * Appends this matrix's formatted, multi-line rendering to the given {@code output}.
     *
     * <p>This is the non-printing counterpart of {@link #println()}: the same row-per-line rendering
     * (for example {@code "[1, 2]\n[3, 4]"}) is written to {@code output} instead of standard output,
     * with no trailing line separator added. To capture the rendering as a {@code String}, pass a
     * {@link StringBuilder}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * StringBuilder sb = new StringBuilder();
     * matrix.appendTo(sb);                                     // sb now holds something such as "[1, 2]\n[3, 4]"
     * }</pre>
     *
     * @param output the destination to append the rendering to; must not be {@code null}
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws UncheckedIOException if {@code output} throws an {@link IOException} while appending
     * @see #println()
     */
    public void appendTo(final Appendable output) {
        N.checkArgNotNull(output, "output");

        try {
            output.append(toMultilineString());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Prints this matrix to standard output in a formatted, human-readable manner.
     * Each concrete implementation provides its own formatting based on the element type.
     * This method is primarily intended for debugging and logging purposes.
     *
     * <p>The exact output format depends on the matrix type:
     * <ul>
     *   <li>Numeric matrices typically display values aligned in rows and columns</li>
     *   <li>Object matrices display using the {@code toString()} method of elements</li>
     * </ul>
     *
     * <p>To capture the formatted rendering instead of printing it, use {@link #appendTo(Appendable)}.</p>
     *
     * <p><b>Usage Examples:</b> (the exact rendering is implementation-defined; the strings below are
     * only an illustration of one possible format)</p>
     * <pre>{@code
     * IntMatrix matrix = IntMatrix.of(new int[][] {{1, 2}, {3, 4}});
     * matrix.println();                                        // prints something such as "[1, 2]\n[3, 4]"
     *
     * IntMatrix single = IntMatrix.of(new int[][] {{1, 2, 3}});
     * single.println();                                        // prints for example "[1, 2, 3]"
     *
     * IntMatrix empty = IntMatrix.of(new int[0][0]);
     * empty.println();                                         // prints for example "[]"
     *
     * IntMatrix rowsNoCols = IntMatrix.of(new int[2][0]);
     * rowsNoCols.println();                                    // prints for example "[]\n[]" (two empty rows)
     * }</pre>
     *
     * @see #appendTo(Appendable)
     */
    public void println() {
        N.println(toMultilineString());
    }

    /**
     * Renders this matrix as a multi-line string (one row per line, e.g. {@code "[1, 2]\n[3, 4]"}); a
     * zero-row matrix renders {@code "[]"}. Backs {@link #println()} and {@link #appendTo(Appendable)}.
     *
     * @return the formatted multi-line representation of this matrix
     */
    abstract String toMultilineString();

    /**
     * Returns the length of the given row array.
     * This abstract method must be implemented by concrete subclasses to return the length
     * of their specific array type (e.g., {@code int[]}, {@code double[]}, or {@code Object[]}).
     *
     * @param row the row array whose length is to be determined
     * @return the length of the array
     */
    protected abstract int length(A row);

    /**
     * Validates that this matrix has the same shape (dimensions) as the specified matrix.
     * This helper is provided for subclasses to enforce shape compatibility before
     * operations that require matrices of the same dimensions (for example element-wise
     * addition); it is an extension point, not an invariant enforced by this class.
     *
     * @param other the matrix to compare shape with; must not be {@code null}
     * @throws IllegalArgumentException if {@code other} is {@code null}, or if the matrices have
     *         different row counts or column counts
     */
    protected void checkSameShape(final M other) {
        N.checkArgNotNull(other, "other");
        N.checkArgument(isSameShape(other), MSG_SHAPE_MISMATCH, rowCount, columnCount, other.rowCount, other.columnCount);
    }

    /**
     * Validates that the specified row index is within the bounds of this matrix (range {@code [0, rowCount)}).
     * This is a helper method used internally to enforce index validity before row access or mutation.
     *
     * @param rowIndex the row index to validate (must be in range [0, rowCount))
     * @throws IndexOutOfBoundsException if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
     */
    protected void checkRowIndex(final int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IndexOutOfBoundsException(formatMsg(MSG_ROW_INDEX_OUT_OF_BOUNDS, rowIndex, rowCount));
        }
    }

    /**
     * Validates that the specified column index is within the bounds of this matrix (range {@code [0, columnCount)}).
     * This is a helper method used internally to enforce index validity before column access or mutation.
     *
     * @param columnIndex the column index to validate (must be in range [0, columnCount))
     * @throws IndexOutOfBoundsException if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
     */
    protected void checkColumnIndex(final int columnIndex) {
        if (columnIndex < 0 || columnIndex >= columnCount) {
            throw new IndexOutOfBoundsException(formatMsg(MSG_COLUMN_INDEX_OUT_OF_BOUNDS, columnIndex, columnCount));
        }
    }

    /**
     * Validates that the specified row and column indices are within the bounds of this matrix.
     * This is a helper method used internally to enforce index validity before element access
     * or neighbor lookup operations.
     *
     * @param rowIndex the row index to validate (must be in range [0, rowCount))
     * @param columnIndex the column index to validate (must be in range [0, columnCount))
     * @throws IndexOutOfBoundsException if {@code rowIndex} or {@code columnIndex} is out of bounds
     */
    protected void checkRowColumnIndex(final int rowIndex, final int columnIndex) {
        checkRowIndex(rowIndex);
        checkColumnIndex(columnIndex);
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
