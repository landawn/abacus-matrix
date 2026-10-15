package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.stream.Stream;

/** Shared contracts intentionally exercised across matrix implementations. */
class MatrixStorageContractTest extends MatrixTestSupport {

    @Nested
    class NumericStorage {

        private static List<AbstractMatrix<?, ?, ?, ?, ?>> matrices(final int rows, final int columns) {
            final IntMatrix ints = new IntMatrix(new int[rows][columns], columns);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++)
                    ints.set(i, j, i * columns + j + 1);
            }
            return List.of(ints, ints.toLongMatrix(), ints.toFloatMatrix(), ints.toDoubleMatrix());
        }

        private static void assertShape(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int rows, final int columns) {
            assertEquals(rows, matrix.rowCount());
            assertEquals(columns, matrix.columnCount());
        }

        private static double cell(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int row, final int column) {
            return ((Number) Array.get(matrix.rowView(row), column)).doubleValue();
        }

        private static void setCell(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int row, final int column, final int value) {
            Array.setInt(matrix.rowView(row), column, value);
        }

        private static Object backingRows(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int... indices) {
            final Object result = Array.newInstance(matrix.rowView(0).getClass(), indices.length);
            for (int i = 0; i < indices.length; i++)
                Array.set(result, i, matrix.rowView(indices[i]));
            return result;
        }

        private static void copyFrom(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int row, final int column, final Object source) {
            if (matrix instanceof IntMatrix m)
                m.copyFrom(row, column, (int[][]) source);
            else if (matrix instanceof LongMatrix m)
                m.copyFrom(row, column, (long[][]) source);
            else if (matrix instanceof FloatMatrix m)
                m.copyFrom(row, column, (float[][]) source);
            else if (matrix instanceof DoubleMatrix m)
                m.copyFrom(row, column, (double[][]) source);
            else
                throw new AssertionError(matrix.getClass());
        }

        private static void setAntiDiagonal(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final Object values) {
            if (matrix instanceof IntMatrix m)
                m.setAntiDiagonal((int[]) values);
            else if (matrix instanceof LongMatrix m)
                m.setAntiDiagonal((long[]) values);
            else if (matrix instanceof FloatMatrix m)
                m.setAntiDiagonal((float[]) values);
            else if (matrix instanceof DoubleMatrix m)
                m.setAntiDiagonal((double[]) values);
            else
                throw new AssertionError(matrix.getClass());
        }

        private static Matrix<?> boxed(final AbstractMatrix<?, ?, ?, ?, ?> matrix) {
            if (matrix instanceof IntMatrix m)
                return m.boxed();
            if (matrix instanceof LongMatrix m)
                return m.boxed();
            if (matrix instanceof FloatMatrix m)
                return m.boxed();
            if (matrix instanceof DoubleMatrix m)
                return m.boxed();
            throw new AssertionError(matrix.getClass());
        }

        private static Matrix<?> mapped(final AbstractMatrix<?, ?, ?, ?, ?> matrix) {
            if (matrix instanceof IntMatrix m)
                return m.mapToObj(value -> value + 1, int.class);
            if (matrix instanceof LongMatrix m)
                return m.mapToObj(value -> value + 1, long.class);
            if (matrix instanceof FloatMatrix m)
                return m.mapToObj(value -> value + 1, float.class);
            if (matrix instanceof DoubleMatrix m)
                return m.mapToObj(value -> value + 1, double.class);
            throw new AssertionError(matrix.getClass());
        }

        private static void assertValues(final double[] expected, final Object actual) {
            assertEquals(expected.length, Array.getLength(actual));
            for (int i = 0; i < expected.length; i++)
                assertEquals(expected[i], ((Number) Array.get(actual, i)).doubleValue(), "element " + i);
        }

        @Test
        public void testTrustedCopyAndWideningFactoriesStillOwnIndependentRows() {
            final int[] ints = { 7, 8 };
            final long[] longs = { 7, 8 };
            final float[] floats = { 7, 8 };
            final double[] doubles = { 7, 8 };
            final List<AbstractMatrix<?, ?, ?, ?, ?>> copies = List.of(IntMatrix.copyOf(ints, ints), LongMatrix.copyOf(longs, longs),
                    FloatMatrix.copyOf(floats, floats), DoubleMatrix.copyOf(doubles, doubles));
            for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : copies) {
                assertNotSame(matrix.rowView(0), matrix.rowView(1));
                setCell(matrix, 0, 0, 99);
                assertEquals(7d, cell(matrix, 1, 0));
            }
            assertEquals(7, ints[0]);
            assertEquals(7L, longs[0]);
            assertEquals(7f, floats[0]);
            assertEquals(7d, doubles[0]);

            final char[] chars = { 1, 2 };
            final byte[] bytes = { 1, 2 };
            final short[] shorts = { 1, 2 };
            final int[] smallInts = { 1, 2 };
            final long[] smallLongs = { 1, 2 };
            final float[] smallFloats = { 1, 2 };
            final List<AbstractMatrix<?, ?, ?, ?, ?>> widened = List.of(IntMatrix.from(chars, chars), IntMatrix.from(bytes, bytes),
                    IntMatrix.from(shorts, shorts), LongMatrix.from(smallInts, smallInts), FloatMatrix.from(smallInts, smallInts),
                    DoubleMatrix.from(smallInts, smallInts), DoubleMatrix.from(smallLongs, smallLongs), DoubleMatrix.from(smallFloats, smallFloats));
            chars[0] = 9;
            bytes[0] = 9;
            shorts[0] = 9;
            smallInts[0] = 9;
            smallLongs[0] = 9;
            smallFloats[0] = 9;
            for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : widened) {
                assertEquals(1d, cell(matrix, 0, 0));
                assertNotSame(matrix.rowView(0), matrix.rowView(1));
                setCell(matrix, 0, 0, 99);
                assertEquals(1d, cell(matrix, 1, 0));
            }
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(ints, ints));
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.wrap(longs, longs));
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.wrap(floats, floats));
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.wrap(doubles, doubles));
        }

        @Test
        public void testTrustedDiagonalFactoriesRetainPrecedenceAndIndependentStorage() {
            final int[] ints = { 1, 2, 3 };
            final long[] longs = { 1, 2, 3 };
            final float[] floats = { 1, 2, 3 };
            final double[] doubles = { 1, 2, 3 };
            final List<AbstractMatrix<?, ?, ?, ?, ?>> matrices = List.of(IntMatrix.ofDiagonals(ints, new int[] { 4, 5, 6 }),
                    LongMatrix.ofDiagonals(longs, new long[] { 4, 5, 6 }), FloatMatrix.ofDiagonals(floats, new float[] { 4, 5, 6 }),
                    DoubleMatrix.ofDiagonals(doubles, new double[] { 4, 5, 6 }));
            ints[0] = 99;
            longs[0] = 99;
            floats[0] = 99;
            doubles[0] = 99;
            for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices) {
                assertEquals(1d, cell(matrix, 0, 0));
                assertEquals(2d, cell(matrix, 1, 1));
                assertEquals(4d, cell(matrix, 0, 2));
                assertEquals(6d, cell(matrix, 2, 0));
                assertNotSame(matrix.rowView(0), matrix.rowView(1));
            }
        }

        @Test
        public void testTrustedBoxedAndMappedResultsKeepTypesShapesAndIndependentRows() {
            for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(4, 3)) {
                final Matrix<?> boxed = boxed(matrix);
                final Matrix<?> mapped = mapped(matrix);
                assertShape(boxed, 4, 3);
                assertShape(mapped, 4, 3);
                assertEquals(boxed.get(0, 0).getClass(), boxed.elementType());
                assertEquals(boxed.elementType(), mapped.elementType());
                assertNotSame(boxed.rowView(0), boxed.rowView(1));
                assertNotSame(mapped.rowView(0), mapped.rowView(1));
                Array.set(boxed.rowView(0), 0, null);
                Array.set(mapped.rowView(0), 0, null);
                assertEquals(1d, cell(matrix, 0, 0));
                assertEquals(4d, ((Number) boxed.get(1, 0)).doubleValue());
                assertEquals(5d, ((Number) mapped.get(1, 0)).doubleValue());
            }
            for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(0, 7)) {
                final Matrix<?> boxed = boxed(matrix);
                final Matrix<?> mapped = mapped(matrix);
                assertShape(boxed, 0, 7);
                assertShape(mapped, 0, 7);
                assertEquals(boxed.elementType(), mapped.elementType());
                assertNotSame(Object.class, boxed.elementType());
            }
        }

        @Test
        public void testBoundedCopyFromKeepsOverlappingSourceValuesAndNoOpValidation() {
            for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(4, 3)) {
                final Object source = backingRows(matrix, 1, 0);
                copyFrom(matrix, 0, 1, source);
                assertValues(new double[] { 1, 4, 5 }, matrix.rowView(0));
                assertValues(new double[] { 4, 1, 2 }, matrix.rowView(1));
                copyFrom(matrix, matrix.rowCount(), 0, source);
                copyFrom(matrix, 0, matrix.columnCount(), source);
                assertValues(new double[] { 1, 4, 5 }, matrix.rowView(0));
                assertThrows(IllegalArgumentException.class, () -> copyFrom(matrix, matrix.rowCount(), 0, null));
            }
        }

        @Test
        public void testBoundedAntiDiagonalSnapshotProtectsAliasedRowsInsideTheWriteRegion() {
            for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(4, 3)) {
                setAntiDiagonal(matrix, matrix.rowView(0));
                assertValues(new double[] { 1, 2, 3 }, matrix.antiDiagonalCopy());
                setAntiDiagonal(matrix, matrix.rowView(3));
                assertValues(new double[] { 10, 11, 12 }, matrix.antiDiagonalCopy());
            }
        }
    }

    @Nested
    class PrimitiveStorage {

        @Test
        public void testTrustedFactoriesKeepCopiedAndConvertedRowsIndependent() {
            final boolean[] booleanRow = { true };
            final byte[] byteRow = { 1 };
            final char[] charRow = { 'a' };
            final short[] shortRow = { 1 };
            final BooleanMatrix booleans = BooleanMatrix.copyOf(booleanRow, booleanRow);
            final ByteMatrix bytes = ByteMatrix.copyOf(byteRow, byteRow);
            final CharMatrix chars = CharMatrix.copyOf(charRow, charRow);
            final ShortMatrix shorts = ShortMatrix.copyOf(shortRow, shortRow);

            assertNotSame(booleans.rowView(0), booleans.rowView(1));
            assertNotSame(bytes.rowView(0), bytes.rowView(1));
            assertNotSame(chars.rowView(0), chars.rowView(1));
            assertNotSame(shorts.rowView(0), shorts.rowView(1));
            booleans.set(0, 0, false);
            bytes.set(0, 0, (byte) 9);
            chars.set(0, 0, 'z');
            shorts.set(0, 0, (short) 9);
            assertEquals(true, booleanRow[0]);
            assertEquals((byte) 1, byteRow[0]);
            assertEquals('a', charRow[0]);
            assertEquals((short) 1, shortRow[0]);
            assertEquals(true, booleans.get(1, 0));
            assertEquals((byte) 1, bytes.get(1, 0));
            assertEquals('a', chars.get(1, 0));
            assertEquals((short) 1, shorts.get(1, 0));

            final BooleanMatrix emptyBooleans = BooleanMatrix.copyOf(new boolean[0], new boolean[0]);
            final ByteMatrix emptyBytes = ByteMatrix.copyOf(new byte[0], new byte[0]);
            final CharMatrix emptyChars = CharMatrix.copyOf(new char[0], new char[0]);
            final ShortMatrix emptyShorts = ShortMatrix.copyOf(new short[0], new short[0]);
            assertNotSame(emptyBooleans.rowView(0), emptyBooleans.rowView(1));
            assertNotSame(emptyBytes.rowView(0), emptyBytes.rowView(1));
            assertNotSame(emptyChars.rowView(0), emptyChars.rowView(1));
            assertNotSame(emptyShorts.rowView(0), emptyShorts.rowView(1));

            final List<Matrix<?>> converted = List.of(booleans.boxed(), booleans.mapToObj(value -> value, boolean.class), bytes.boxed(),
                    bytes.mapToObj(value -> value, byte.class), chars.boxed(), chars.mapToObj(value -> value, char.class), shorts.boxed(),
                    shorts.mapToObj(value -> value, short.class), emptyBooleans.boxed(), emptyBooleans.mapToObj(value -> value, boolean.class),
                    emptyBytes.boxed(), emptyBytes.mapToObj(value -> value, byte.class), emptyChars.boxed(), emptyChars.mapToObj(value -> value, char.class),
                    emptyShorts.boxed(), emptyShorts.mapToObj(value -> value, short.class));

            for (final Matrix<?> matrix : converted) {
                assertNotSame(matrix.rowView(0), matrix.rowView(1));

                if (matrix.columnCount() > 0) {
                    final Object untouched = matrix.get(1, 0);
                    matrix.rowView(0)[0] = null;
                    assertEquals(untouched, matrix.get(1, 0));
                }
            }

            assertEquals(false, booleans.get(0, 0));
            assertEquals((byte) 9, bytes.get(0, 0));
            assertEquals('z', chars.get(0, 0));
            assertEquals((short) 9, shorts.get(0, 0));

            final ShortMatrix diagonal = ShortMatrix.ofDiagonals(new short[] { 1, 2, 3 }, new short[] { 4, 5, 6 });
            assertArrayEquals(new short[] { 1, 0, 4 }, diagonal.rowCopy(0));
            assertArrayEquals(new short[] { 0, 2, 0 }, diagonal.rowCopy(1));
            assertArrayEquals(new short[] { 6, 0, 3 }, diagonal.rowCopy(2));
            assertNotSame(diagonal.rowView(0), diagonal.rowView(1));
        }

        @Test
        public void testBoundedCopiesAndAntiDiagonalsKeepOriginalAliasedValues() {
            final BooleanMatrix booleans = BooleanMatrix.wrap(new boolean[][] { { false, false, true }, { true, true, false }, { false, false, false } });
            final ByteMatrix bytes = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final CharMatrix chars = CharMatrix.wrap(new char[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final ShortMatrix shorts = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            booleans.copyFrom(1, 1, new boolean[][] { booleans.rowView(0), booleans.rowView(1), booleans.rowView(0) });
            bytes.copyFrom(1, 1, new byte[][] { bytes.rowView(0), bytes.rowView(1), bytes.rowView(0) });
            chars.copyFrom(1, 1, new char[][] { chars.rowView(0), chars.rowView(1), chars.rowView(0) });
            shorts.copyFrom(1, 1, new short[][] { shorts.rowView(0), shorts.rowView(1), shorts.rowView(0) });
            assertArrayEquals(new boolean[] { false, true, true }, booleans.rowCopy(2));
            assertArrayEquals(new byte[] { 7, 4, 5 }, bytes.rowCopy(2));
            assertArrayEquals(new char[] { 7, 4, 5 }, chars.rowCopy(2));
            assertArrayEquals(new short[] { 7, 4, 5 }, shorts.rowCopy(2));

            booleans.setAntiDiagonal(booleans.rowView(0));
            bytes.setAntiDiagonal(bytes.rowView(0));
            chars.setAntiDiagonal(chars.rowView(0));
            shorts.setAntiDiagonal(shorts.rowView(0));
            assertArrayEquals(new boolean[] { false, false, true }, booleans.antiDiagonalCopy());
            assertArrayEquals(new byte[] { 1, 2, 3 }, bytes.antiDiagonalCopy());
            assertArrayEquals(new char[] { 1, 2, 3 }, chars.antiDiagonalCopy());
            assertArrayEquals(new short[] { 1, 2, 3 }, shorts.antiDiagonalCopy());

            final ByteMatrix before = bytes.copy();
            bytes.copyFrom(bytes.rowCount(), 0, new byte[][] { bytes.rowView(0) });
            bytes.copyFrom(0, bytes.columnCount(), new byte[][] { bytes.rowView(0) });
            bytes.copyFrom(0, 0, new byte[0][]);
            assertEquals(before, bytes);
            assertThrows(IllegalArgumentException.class, () -> bytes.copyFrom(bytes.rowCount(), 0, null));
            assertThrows(IndexOutOfBoundsException.class, () -> bytes.copyFrom(-1, 0, null));
        }
    }

    @Nested
    class SharedStorageAndCursors {

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

        @Test
        public void testOverlappingCopiesMatchAnIndependentSourceSnapshotForEveryMatrixType() throws Exception {
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
        public void testBoundedSnapshotsIgnoreUnusedRowsAndShareOnlyNeededPrefixes() {
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
        public void testMultiRowSnapshotsReturnTheSourceWhenNoWrittenRowIsRead() {
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
        public void testBoundedRowSnapshotProtectsOnlyAffectedRows() {
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
        public void testCoordinateStreamsKeepOrderWithoutRetainingMatrixStorage() throws Exception {
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
        public void testEmptyRangeStreamsRetainNoSourceAndStillValidateBounds() throws Exception {
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
        public void testEmptyDiagonalStreamsRetainNoSource() throws Exception {
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
    }
}
