package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.stream.Stream;

class SharedPerformanceOptimizationTest extends TestBase {
    @Test
    void overlappingCopiesMatchAnIndependentSourceSnapshotForEveryMatrixType() throws Exception {
        final Random random = new Random(0x5A17L);

        for (int type = 0; type < 9; type++) {
            for (int trial = 0; trial < 100; trial++) {
                final int rowCount = random.nextInt(6);
                final int columnCount = random.nextInt(6);
                final AbstractMatrix<?, ?, ?, ?, ?> matrix = matrix(type, rowCount, columnCount);
                final Object[] rows = matrix.unsafeBackingArray();
                final Class<?> elementType = rows.getClass().getComponentType().getComponentType();

                for (int i = 0; i < rowCount; i++) {
                    for (int j = 0; j < columnCount; j++) {
                        Array.set(rows[i], j, value(type, i * 10 + j));
                    }
                }

                final Object[] source = Arrays.copyOf(rows, random.nextInt(9));
                for (int i = 0; i < source.length; i++) {
                    final int choice = random.nextInt(4);
                    if (choice == 0) {
                        source[i] = null;
                    } else if (choice <= 2 && rowCount > 0) {
                        source[i] = rows[random.nextInt(rowCount)];
                    } else {
                        source[i] = Array.newInstance(elementType, random.nextInt(8));
                        for (int j = 0; j < Array.getLength(source[i]); j++) {
                            Array.set(source[i], j, value(type, 50 + j));
                        }
                    }
                }

                final Object[] expected = cloneRows(rows);
                final Object[] sourceSnapshot = cloneRows(source);
                final int destinationRow = random.nextInt(rowCount + 1);
                final int destinationColumn = random.nextInt(columnCount + 1);
                for (int i = 0; i < Math.min(source.length, rowCount - destinationRow); i++) {
                    if (sourceSnapshot[i] != null) {
                        System.arraycopy(sourceSnapshot[i], 0, expected[destinationRow + i], destinationColumn,
                                Math.min(Array.getLength(sourceSnapshot[i]), columnCount - destinationColumn));
                    }
                }

                final Class<?> sourceType = matrix instanceof Matrix<?> ? Object[][].class : source.getClass();
                matrix.getClass().getMethod("copyFrom", int.class, int.class, sourceType).invoke(matrix, destinationRow, destinationColumn, source);
                assertTrue(Arrays.deepEquals(expected, matrix.unsafeBackingArray()), "type=" + type + ", trial=" + trial);
            }
        }
    }

    @Test
    void boundedSnapshotsIgnoreUnusedRowsAndShareOnlyNeededPrefixes() {
        final IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3, 4 }, { 5, 6, 7, 8 }, { 9, 10, 11, 12 }, { 13, 14, 15, 16 } });
        final int[] shared = matrix.rowView(2);
        final int[][] source = { shared, shared, matrix.rowView(0), matrix.rowView(3) };
        final int[][] snapshot = matrix.snapshotRowsIfBackingRows(source, 2, 3);
        assertEquals(2, snapshot.length);
        assertSame(snapshot[0], snapshot[1]);
        assertNotSame(shared, snapshot[0]);
        assertArrayEquals(new int[] { 9 }, snapshot[0]);
        matrix.copyFrom(2, 3, source);
        assertEquals(9, matrix.get(2, 3));
        assertEquals(9, matrix.get(3, 3));
        assertEquals(1, matrix.get(0, 0));

        assertSame(source, matrix.snapshotRowsIfBackingRows(source, matrix.rowCount(), 0));
        assertSame(source, matrix.snapshotRowsIfBackingRows(source, 0, matrix.columnCount()));
        final int[][] oneRow = { matrix.rowView(0) };
        assertSame(oneRow, matrix.snapshotRowsIfBackingRows(oneRow, 0, 1));
        matrix.copyFrom(0, 1, oneRow);
        assertArrayEquals(new int[] { 1, 1, 2, 3 }, matrix.rowCopy(0));
    }

    @Test
    void multiRowSnapshotsReturnTheSourceWhenNoWrittenRowIsRead() {
        final IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 }, { 10, 11, 12 } });

        // Rows narrower or wider than the matrix, and null rows, can never be backing rows.
        final int[][] misfits = { { 0, 0 }, null, { 0, 0, 0, 0 } };
        assertSame(misfits, matrix.snapshotRowsIfBackingRows(misfits, 0, 0));

        // Full-width rows that belong to another array.
        final int[][] foreign = { { 0, 0, 0 }, null, { 0, 0, 0 } };
        assertSame(foreign, matrix.snapshotRowsIfBackingRows(foreign, 1, 0));

        // Backing rows that this copy reads but never writes.
        final int[][] untouched = { matrix.rowView(0), matrix.rowView(1) };
        assertSame(untouched, matrix.snapshotRowsIfBackingRows(untouched, 2, 0));
        matrix.copyFrom(2, 0, untouched);
        assertArrayEquals(new int[] { 1, 2, 3 }, matrix.rowCopy(2));
        assertArrayEquals(new int[] { 4, 5, 6 }, matrix.rowCopy(3));

        // Row 0 is the destination of a null source row, so reading it elsewhere in the block is safe.
        final int[][] skipped = { null, matrix.rowView(0), new int[0] };
        assertSame(skipped, matrix.snapshotRowsIfBackingRows(skipped, 0, 0));
        matrix.copyFrom(0, 0, skipped);
        assertArrayEquals(new int[] { 1, 2, 3 }, matrix.rowCopy(0));
        assertArrayEquals(new int[] { 1, 2, 3 }, matrix.rowCopy(1));
        assertArrayEquals(new int[] { 1, 2, 3 }, matrix.rowCopy(2));
    }

    @Test
    void boundedRowSnapshotProtectsOnlyAffectedRows() {
        final IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 }, { 7, 8 } });
        assertSame(matrix.rowView(3), matrix.snapshotIfBackingRow(matrix.rowView(3), 2));
        assertNotSame(matrix.rowView(0), matrix.snapshotIfBackingRow(matrix.rowView(0), 2));
        final int[] column = { 9, 10, 11, 12 };
        assertSame(column, matrix.snapshotIfBackingRow(column));
        matrix.setAntiDiagonal(matrix.rowView(0));
        assertArrayEquals(new int[] { 1, 1 }, matrix.rowCopy(0));
        assertArrayEquals(new int[] { 2, 4 }, matrix.rowCopy(1));
    }

    @Test
    void coordinateStreamsKeepOrderWithoutRetainingMatrixStorage() throws Exception {
        final IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        try (Stream<Point> rowMajor = matrix.rowMajorPoints();
             Stream<Point> columnMajor = matrix.columnMajorPoints();
             Stream<Point> antiDiagonal = matrix.antiDiagonalPoints();
             Stream<Stream<Point>> rows = matrix.rowPoints();
             Stream<Stream<Point>> columns = matrix.columnPoints()) {
            for (final Object stream : List.of(rowMajor, columnMajor, antiDiagonal, rows, columns)) {
                assertFalse(retains(stream, matrix), "Coordinate-only stream must not retain backing storage");
            }
            assertEquals(List.of(Point.of(0, 0), Point.of(0, 1), Point.of(0, 2), Point.of(1, 0), Point.of(1, 1), Point.of(1, 2)), rowMajor.toList());
            assertEquals(List.of(Point.of(0, 0), Point.of(1, 0), Point.of(0, 1), Point.of(1, 1), Point.of(0, 2), Point.of(1, 2)), columnMajor.toList());
            assertEquals(List.of(Point.of(0, 2), Point.of(1, 1)), antiDiagonal.toList());
            assertEquals(List.of(List.of(Point.of(0, 0), Point.of(0, 1), Point.of(0, 2)), List.of(Point.of(1, 0), Point.of(1, 1), Point.of(1, 2))),
                    rows.map(Stream::toList).toList());
            assertEquals(List.of(List.of(Point.of(0, 0), Point.of(1, 0)), List.of(Point.of(0, 1), Point.of(1, 1)), List.of(Point.of(0, 2), Point.of(1, 2))),
                    columns.map(Stream::toList).toList());
        }
    }

    @Test
    void emptyRangeStreamsRetainNoSourceAndStillValidateBounds() throws Exception {
        for (int type = 0; type < 9; type++) {
            final AbstractMatrix<?, ?, ?, ?, ?> matrix = matrix(type, 3, 4);
            for (final String method : List.of("rowMajorStream", "columnMajorStream", "rowStreams", "columnStreams")) {
                final Object stream = matrix.getClass().getMethod(method, int.class, int.class).invoke(matrix, 1, 1);
                try {
                    assertFalse(retains(stream, matrix), "type=" + type + ", method=" + method);
                } finally {
                    ((AutoCloseable) stream).close();
                }
            }
        }
        assertThrows(IndexOutOfBoundsException.class, () -> IntMatrix.empty().rowMajorStream(1, 1));
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.empty().copyFrom(0, 0, null));
        assertThrows(IndexOutOfBoundsException.class, () -> IntMatrix.empty().copyFrom(-1, 0, null));
        assertEquals(3, IntMatrix.empty().resize(3, 0).rowStreams().count());
        assertEquals(4, IntMatrix.empty().resize(0, 4).columnStreams().count());
    }

    @Test
    void emptyDiagonalStreamsRetainNoSource() throws Exception {
        for (int type = 0; type < 9; type++) {
            for (final int[] shape : new int[][] { { 3, 0 }, { 0, 3 } }) {
                final AbstractMatrix<?, ?, ?, ?, ?> matrix = matrix(type, shape[0], shape[1]);
                final String label = "type=" + type + ", shape=" + shape[0] + "x" + shape[1];
                try (AutoCloseable mainDiagonal = (AutoCloseable) matrix.mainDiagonalStream();
                     AutoCloseable antiDiagonal = (AutoCloseable) matrix.antiDiagonalStream()) {
                    assertFalse(retains(mainDiagonal, matrix), label + ", main diagonal");
                    assertFalse(retains(antiDiagonal, matrix), label + ", anti-diagonal");
                }
            }
        }
    }

    private static AbstractMatrix<?, ?, ?, ?, ?> matrix(final int type, final int rows, final int columns) {
        return switch (type) {
            case 0 -> BooleanMatrix.empty().resize(rows, columns);
            case 1 -> ByteMatrix.empty().resize(rows, columns);
            case 2 -> CharMatrix.empty().resize(rows, columns);
            case 3 -> ShortMatrix.empty().resize(rows, columns);
            case 4 -> IntMatrix.empty().resize(rows, columns);
            case 5 -> LongMatrix.empty().resize(rows, columns);
            case 6 -> FloatMatrix.empty().resize(rows, columns);
            case 7 -> DoubleMatrix.empty().resize(rows, columns);
            default -> Matrix.empty(String.class).resize(rows, columns);
        };
    }

    private static Object value(final int type, final int value) {
        return switch (type) {
            case 0 -> (value & 1) == 0;
            case 1 -> (byte) value;
            case 2 -> (char) (256 + value);
            case 3 -> (short) value;
            case 4 -> value;
            case 5 -> (long) value;
            case 6 -> value / 2.0f;
            case 7 -> value / 2.0;
            default -> "value-" + value;
        };
    }

    private static Object[] cloneRows(final Object[] rows) {
        final Object[] result = rows.clone();
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] != null) {
                final int length = Array.getLength(rows[i]);
                result[i] = Array.newInstance(rows[i].getClass().getComponentType(), length);
                System.arraycopy(rows[i], 0, result[i], 0, length);
            }
        }
        return result;
    }

    private static boolean retains(final Object root, final Object target) throws IllegalAccessException {
        final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        final ArrayDeque<Object> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            final Object value = pending.removeFirst();
            if (value == target) {
                return true;
            }
            if (!seen.add(value) || !value.getClass().getName().startsWith("com.landawn.")) {
                continue;
            }
            for (Class<?> type = value.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                for (final Field field : type.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive() && field.trySetAccessible()) {
                        final Object child = field.get(value);
                        if (child != null) {
                            pending.add(child);
                        }
                    }
                }
            }
        }
        return false;
    }
}
