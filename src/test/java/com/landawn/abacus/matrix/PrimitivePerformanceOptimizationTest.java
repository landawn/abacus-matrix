package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.ByteIteratorEx;
import com.landawn.abacus.util.stream.CharIteratorEx;
import com.landawn.abacus.util.stream.ShortIteratorEx;

class PrimitivePerformanceOptimizationTest extends TestBase {

    @Test
    void exactShortMultiplicationChecksCompleteDotsAndResetsItsScratchRow() {
        final ShortMatrix left = ShortMatrix.wrap(new short[][] { { 30_000, 30_000 }, { 100, 100 }, { -30_000, -30_000 } });
        final ShortMatrix right = ShortMatrix.wrap(new short[][] { { 2, 1, 0 }, { -1, -1, 0 } });
        final ShortMatrix result = left.matrixMultiplyExact(right);

        assertArrayEquals(new short[] { 30_000, 0, 0 }, result.rowCopy(0));
        assertArrayEquals(new short[] { 100, 0, 0 }, result.rowCopy(1));
        assertArrayEquals(new short[] { -30_000, 0, 0 }, result.rowCopy(2));
        assertArrayEquals(new short[] { 30_000, 30_000 }, left.rowCopy(0));
        assertArrayEquals(new short[] { 2, 1, 0 }, right.rowCopy(0));

        final short min = Short.MIN_VALUE;
        final ShortMatrix largeIntermediate = ShortMatrix.wrap(new short[][] { { min, min, min, min, min, min } });
        final ShortMatrix cancellation = ShortMatrix.wrap(new short[][] { { min }, { min }, { Short.MAX_VALUE }, { Short.MAX_VALUE }, { 1 }, { 1 } });
        assertEquals((short) 0, largeIntermediate.matrixMultiplyExact(cancellation).get(0, 0));

        final ArithmeticException overflow = assertThrows(ArithmeticException.class,
                () -> left.matrixMultiplyExact(ShortMatrix.wrap(new short[][] { { 2, 3 }, { 0, 0 } })));
        assertEquals("short overflow: 60000", overflow.getMessage());
    }

    @Test
    void exactShortMultiplicationPreservesEmptyShapesAndArgumentFailures() {
        final ShortMatrix emptyDots = new ShortMatrix(new short[3][0]).matrixMultiplyExact(new ShortMatrix(new short[0][], 4));
        assertEquals(3, emptyDots.rowCount());
        assertEquals(4, emptyDots.columnCount());

        for (int row = 0; row < emptyDots.rowCount(); row++) {
            assertArrayEquals(new short[4], emptyDots.rowCopy(row));
        }

        final ShortMatrix noColumns = new ShortMatrix(new short[3][2]).matrixMultiplyExact(new ShortMatrix(new short[2][0]));
        assertEquals(3, noColumns.rowCount());
        assertEquals(0, noColumns.columnCount());

        // A logical width with no rows must not allocate a width-sized accumulator.
        final ShortMatrix noRows = ShortMatrix.empty().matrixMultiplyExact(new ShortMatrix(new short[0][], Integer.MAX_VALUE));
        assertEquals(0, noRows.rowCount());
        assertEquals(Integer.MAX_VALUE, noRows.columnCount());
        assertThrows(IllegalArgumentException.class, () -> ShortMatrix.empty().matrixMultiplyExact(null));
        assertThrows(IllegalArgumentException.class, () -> new ShortMatrix(new short[2][1]).matrixMultiplyExact(new ShortMatrix(new short[2][3])));
    }

    @Test
    void trustedFactoriesKeepCopiedAndConvertedRowsIndependent() {
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
                shorts.mapToObj(value -> value, short.class), emptyBooleans.boxed(), emptyBooleans.mapToObj(value -> value, boolean.class), emptyBytes.boxed(),
                emptyBytes.mapToObj(value -> value, byte.class), emptyChars.boxed(), emptyChars.mapToObj(value -> value, char.class), emptyShorts.boxed(),
                emptyShorts.mapToObj(value -> value, short.class));

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
    void boundedCopiesAndAntiDiagonalsKeepOriginalAliasedValues() {
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

    @Test
    void byteIteratorMaterializationConsumesOnlyTheRemainingLiveValues() {
        for (final boolean asList : new boolean[] { false, true }) {
            final ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final ByteIteratorEx main = (ByteIteratorEx) matrix.mainDiagonalStream().iterator();
            assertEquals((byte) 1, main.nextByte());
            matrix.set(1, 1, (byte) 50);
            final byte[] mainRemaining = asList ? main.toList().toArray() : main.toArray();
            assertArrayEquals(new byte[] { 50, 9 }, mainRemaining);
            assertFalse(main.hasNext());
            assertEquals(0L, main.count());
            assertArrayEquals(new byte[0], main.toArray());
            mainRemaining[0] = (byte) 99;
            assertEquals((byte) 50, matrix.get(1, 1));
            matrix.set(1, 1, (byte) 5);

            final ByteIteratorEx anti = (ByteIteratorEx) matrix.antiDiagonalStream().iterator();
            anti.advance(1);
            matrix.set(2, 0, (byte) 70);
            assertArrayEquals(new byte[] { 5, 70 }, asList ? anti.toList().toArray() : anti.toArray());
            assertFalse(anti.hasNext());
            assertEquals(0L, anti.count());
            assertEquals(0, anti.toList().size());

            final ByteIteratorEx column = (ByteIteratorEx) matrix.columnStreams(1, 2).iterator().next().iterator();
            assertEquals((byte) 2, column.nextByte());
            matrix.flipVerticallyInPlace();
            matrix.set(1, 1, (byte) 55);
            assertArrayEquals(new byte[] { 55, 2 }, asList ? column.toList().toArray() : column.toArray());
            assertFalse(column.hasNext());
            assertEquals(0L, column.count());
            assertArrayEquals(new byte[0], column.toArray());

            final ByteIteratorEx rows = (ByteIteratorEx) matrix.rowMajorStream().iterator();
            assertEquals((byte) 70, rows.nextByte());
            rows.advance(3);
            matrix.set(1, 1, (byte) 56);
            assertArrayEquals(new byte[] { 56, 6, 1, 2, 3 }, asList ? rows.toList().toArray() : rows.toArray());
            assertFalse(rows.hasNext());
            assertEquals(0L, rows.count());
            assertEquals(0, rows.toList().size());

            final ByteIteratorEx columns = (ByteIteratorEx) matrix.columnMajorStream().iterator();
            assertEquals((byte) 70, columns.nextByte());
            columns.advance(3);
            matrix.set(1, 1, (byte) 57);
            assertArrayEquals(new byte[] { 57, 2, 9, 6, 3 }, asList ? columns.toList().toArray() : columns.toArray());
            assertFalse(columns.hasNext());
            assertEquals(0L, columns.count());
            columns.advance(Long.MAX_VALUE);
            assertEquals(0, columns.toList().size());
        }
    }

    @Test
    void charIteratorMaterializationConsumesOnlyTheRemainingLiveValues() {
        for (final boolean asList : new boolean[] { false, true }) {
            final CharMatrix matrix = CharMatrix.wrap(new char[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final CharIteratorEx main = (CharIteratorEx) matrix.mainDiagonalStream().iterator();
            assertEquals((char) 1, main.nextChar());
            matrix.set(1, 1, (char) 50);
            final char[] mainRemaining = asList ? main.toList().toArray() : main.toArray();
            assertArrayEquals(new char[] { 50, 9 }, mainRemaining);
            assertFalse(main.hasNext());
            assertEquals(0L, main.count());
            assertArrayEquals(new char[0], main.toArray());
            mainRemaining[0] = (char) 99;
            assertEquals((char) 50, matrix.get(1, 1));
            matrix.set(1, 1, (char) 5);

            final CharIteratorEx anti = (CharIteratorEx) matrix.antiDiagonalStream().iterator();
            anti.advance(1);
            matrix.set(2, 0, (char) 70);
            assertArrayEquals(new char[] { 5, 70 }, asList ? anti.toList().toArray() : anti.toArray());
            assertFalse(anti.hasNext());
            assertEquals(0L, anti.count());
            assertEquals(0, anti.toList().size());

            final CharIteratorEx column = (CharIteratorEx) matrix.columnStreams(1, 2).iterator().next().iterator();
            assertEquals((char) 2, column.nextChar());
            matrix.flipVerticallyInPlace();
            matrix.set(1, 1, (char) 55);
            assertArrayEquals(new char[] { 55, 2 }, asList ? column.toList().toArray() : column.toArray());
            assertFalse(column.hasNext());
            assertEquals(0L, column.count());
            assertArrayEquals(new char[0], column.toArray());

            final CharIteratorEx rows = (CharIteratorEx) matrix.rowMajorStream().iterator();
            assertEquals((char) 70, rows.nextChar());
            rows.advance(3);
            matrix.set(1, 1, (char) 56);
            assertArrayEquals(new char[] { 56, 6, 1, 2, 3 }, asList ? rows.toList().toArray() : rows.toArray());
            assertFalse(rows.hasNext());
            assertEquals(0L, rows.count());
            assertEquals(0, rows.toList().size());

            final CharIteratorEx columns = (CharIteratorEx) matrix.columnMajorStream().iterator();
            assertEquals((char) 70, columns.nextChar());
            columns.advance(3);
            matrix.set(1, 1, (char) 57);
            assertArrayEquals(new char[] { 57, 2, 9, 6, 3 }, asList ? columns.toList().toArray() : columns.toArray());
            assertFalse(columns.hasNext());
            assertEquals(0L, columns.count());
            columns.advance(Long.MAX_VALUE);
            assertEquals(0, columns.toList().size());
        }
    }

    @Test
    void shortIteratorMaterializationConsumesOnlyTheRemainingLiveValues() {
        for (final boolean asList : new boolean[] { false, true }) {
            final ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final ShortIteratorEx main = (ShortIteratorEx) matrix.mainDiagonalStream().iterator();
            assertEquals((short) 1, main.nextShort());
            matrix.set(1, 1, (short) 50);
            final short[] mainRemaining = asList ? main.toList().toArray() : main.toArray();
            assertArrayEquals(new short[] { 50, 9 }, mainRemaining);
            assertFalse(main.hasNext());
            assertEquals(0L, main.count());
            assertArrayEquals(new short[0], main.toArray());
            mainRemaining[0] = (short) 99;
            assertEquals((short) 50, matrix.get(1, 1));
            matrix.set(1, 1, (short) 5);

            final ShortIteratorEx anti = (ShortIteratorEx) matrix.antiDiagonalStream().iterator();
            anti.advance(1);
            matrix.set(2, 0, (short) 70);
            assertArrayEquals(new short[] { 5, 70 }, asList ? anti.toList().toArray() : anti.toArray());
            assertFalse(anti.hasNext());
            assertEquals(0L, anti.count());
            assertEquals(0, anti.toList().size());

            final ShortIteratorEx column = (ShortIteratorEx) matrix.columnStreams(1, 2).iterator().next().iterator();
            assertEquals((short) 2, column.nextShort());
            matrix.flipVerticallyInPlace();
            matrix.set(1, 1, (short) 55);
            assertArrayEquals(new short[] { 55, 2 }, asList ? column.toList().toArray() : column.toArray());
            assertFalse(column.hasNext());
            assertEquals(0L, column.count());
            assertArrayEquals(new short[0], column.toArray());

            final ShortIteratorEx rows = (ShortIteratorEx) matrix.rowMajorStream().iterator();
            assertEquals((short) 70, rows.nextShort());
            rows.advance(3);
            matrix.set(1, 1, (short) 56);
            assertArrayEquals(new short[] { 56, 6, 1, 2, 3 }, asList ? rows.toList().toArray() : rows.toArray());
            assertFalse(rows.hasNext());
            assertEquals(0L, rows.count());
            assertEquals(0, rows.toList().size());

            final ShortIteratorEx columns = (ShortIteratorEx) matrix.columnMajorStream().iterator();
            assertEquals((short) 70, columns.nextShort());
            columns.advance(3);
            matrix.set(1, 1, (short) 57);
            assertArrayEquals(new short[] { 57, 2, 9, 6, 3 }, asList ? columns.toList().toArray() : columns.toArray());
            assertFalse(columns.hasNext());
            assertEquals(0L, columns.count());
            columns.advance(Long.MAX_VALUE);
            assertEquals(0, columns.toList().size());
        }
    }

    @Test
    void booleanEmptyStreamRangesKeepBoundsValidationAndLogicalShapeCounts() {
        final BooleanMatrix matrix = new BooleanMatrix(new boolean[2][2]);
        assertEquals(0L, matrix.rowMajorStream(1, 1).count());
        assertEquals(0L, matrix.columnMajorStream(1, 1).count());
        assertEquals(0L, matrix.rowStreams(1, 1).count());
        assertEquals(0L, matrix.columnStreams(1, 1).count());
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(3, 3));

        final BooleanMatrix noColumns = new BooleanMatrix(new boolean[2][0]);
        assertEquals(2L, noColumns.rowStreams().count());
        noColumns.rowStreams().forEach(row -> assertEquals(0L, row.count()));
        assertEquals(0L, noColumns.mainDiagonalStream().count());
        assertEquals(0L, noColumns.antiDiagonalStream().count());

        final BooleanMatrix noRows = new BooleanMatrix(new boolean[0][], 3);
        assertEquals(3L, noRows.columnStreams().count());
        noRows.columnStreams().forEach(column -> assertEquals(0L, column.count()));
        assertEquals(0L, noRows.mainDiagonalStream().count());
        assertEquals(0L, noRows.antiDiagonalStream().count());
    }

    @Test
    void byteEmptyStreamRangesKeepBoundsValidationAndLogicalShapeCounts() {
        final ByteMatrix matrix = new ByteMatrix(new byte[2][2]);
        assertEquals(0L, matrix.rowMajorStream(1, 1).count());
        assertEquals(0L, matrix.columnMajorStream(1, 1).count());
        assertEquals(0L, matrix.rowStreams(1, 1).count());
        assertEquals(0L, matrix.columnStreams(1, 1).count());
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(3, 3));

        final ByteMatrix noColumns = new ByteMatrix(new byte[2][0]);
        assertEquals(2L, noColumns.rowStreams().count());
        noColumns.rowStreams().forEach(row -> assertEquals(0L, row.count()));
        assertEquals(0L, noColumns.mainDiagonalStream().count());
        assertEquals(0L, noColumns.antiDiagonalStream().count());

        final ByteMatrix noRows = new ByteMatrix(new byte[0][], 3);
        assertEquals(3L, noRows.columnStreams().count());
        noRows.columnStreams().forEach(column -> assertEquals(0L, column.count()));
        assertEquals(0L, noRows.mainDiagonalStream().count());
        assertEquals(0L, noRows.antiDiagonalStream().count());
    }

    @Test
    void charEmptyStreamRangesKeepBoundsValidationAndLogicalShapeCounts() {
        final CharMatrix matrix = new CharMatrix(new char[2][2]);
        assertEquals(0L, matrix.rowMajorStream(1, 1).count());
        assertEquals(0L, matrix.columnMajorStream(1, 1).count());
        assertEquals(0L, matrix.rowStreams(1, 1).count());
        assertEquals(0L, matrix.columnStreams(1, 1).count());
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(3, 3));

        final CharMatrix noColumns = new CharMatrix(new char[2][0]);
        assertEquals(2L, noColumns.rowStreams().count());
        noColumns.rowStreams().forEach(row -> assertEquals(0L, row.count()));
        assertEquals(0L, noColumns.mainDiagonalStream().count());
        assertEquals(0L, noColumns.antiDiagonalStream().count());

        final CharMatrix noRows = new CharMatrix(new char[0][], 3);
        assertEquals(3L, noRows.columnStreams().count());
        noRows.columnStreams().forEach(column -> assertEquals(0L, column.count()));
        assertEquals(0L, noRows.mainDiagonalStream().count());
        assertEquals(0L, noRows.antiDiagonalStream().count());
    }

    @Test
    void shortEmptyStreamRangesKeepBoundsValidationAndLogicalShapeCounts() {
        final ShortMatrix matrix = new ShortMatrix(new short[2][2]);
        assertEquals(0L, matrix.rowMajorStream(1, 1).count());
        assertEquals(0L, matrix.columnMajorStream(1, 1).count());
        assertEquals(0L, matrix.rowStreams(1, 1).count());
        assertEquals(0L, matrix.columnStreams(1, 1).count());
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(3, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(3, 3));

        final ShortMatrix noColumns = new ShortMatrix(new short[2][0]);
        assertEquals(2L, noColumns.rowStreams().count());
        noColumns.rowStreams().forEach(row -> assertEquals(0L, row.count()));
        assertEquals(0L, noColumns.mainDiagonalStream().count());
        assertEquals(0L, noColumns.antiDiagonalStream().count());

        final ShortMatrix noRows = new ShortMatrix(new short[0][], 3);
        assertEquals(3L, noRows.columnStreams().count());
        noRows.columnStreams().forEach(column -> assertEquals(0L, column.count()));
        assertEquals(0L, noRows.mainDiagonalStream().count());
        assertEquals(0L, noRows.antiDiagonalStream().count());
    }

    @Test
    void characterEscapingMatchesTheEstablishedFormatForEveryCodeUnit() {
        final char[] values = new char[Character.MAX_VALUE + 1];
        final StringBuilder row = new StringBuilder("[");

        for (int i = 0; i < values.length; i++) {
            final char value = (char) i;
            values[i] = value;

            if (i > 0) {
                row.append(", ");
            }

            row.append('\'');

            final String special = switch (value) {
                case '\b' -> "\\b";
                case '\t' -> "\\t";
                case '\n' -> "\\n";
                case '\f' -> "\\f";
                case '\r' -> "\\r";
                case '\'' -> "\\'";
                case '\\' -> "\\\\";
                default -> null;
            };

            if (special != null) {
                row.append(special);
            } else if (value >= 0x20 && value <= 0x7e) {
                row.append(value);
            } else {
                // An independent formatter supplies the oracle for all four uppercase hex digits.
                row.append('\\').append('u').append(String.format(Locale.ROOT, "%04X", i));
            }

            row.append('\'');
        }

        row.append(']');
        final String expectedRow = row.toString();
        final CharMatrix matrix = CharMatrix.wrap(values);
        assertEquals("[" + expectedRow + "]", matrix.toString());
        assertEquals(expectedRow, matrix.toMultilineString());

        final StringBuilder appended = new StringBuilder("prefix:");
        matrix.appendTo(appended);
        assertEquals("prefix:" + expectedRow, appended.toString());
    }
}
