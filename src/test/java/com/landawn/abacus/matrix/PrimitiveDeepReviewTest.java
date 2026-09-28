package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class PrimitiveDeepReviewTest extends TestBase {

    @Test
    void closedRangesWithEqualBoundsContainOneValueForEitherStepDirection() {
        for (final byte step : new byte[] { Byte.MIN_VALUE, -1, 1, Byte.MAX_VALUE }) {
            final ByteMatrix result = ByteMatrix.rangeClosed((byte) 7, (byte) 7, step);
            assertEquals(1, result.rowCount());
            assertArrayEquals(new byte[] { 7 }, result.rowCopy(0));
        }

        for (final short step : new short[] { Short.MIN_VALUE, -1, 1, Short.MAX_VALUE }) {
            final ShortMatrix result = ShortMatrix.rangeClosed((short) 7, (short) 7, step);
            assertEquals(1, result.rowCount());
            assertArrayEquals(new short[] { 7 }, result.rowCopy(0));
        }

        for (final int step : new int[] { Integer.MIN_VALUE, -1, 1, Integer.MAX_VALUE }) {
            final CharMatrix result = CharMatrix.rangeClosed('x', 'x', step);
            assertEquals(1, result.rowCount());
            assertArrayEquals(new char[] { 'x' }, result.rowCopy(0));
        }

        assertThrows(IllegalArgumentException.class, () -> ByteMatrix.rangeClosed((byte) 7, (byte) 7, (byte) 0));
        assertThrows(IllegalArgumentException.class, () -> ShortMatrix.rangeClosed((short) 7, (short) 7, (short) 0));
        assertThrows(IllegalArgumentException.class, () -> CharMatrix.rangeClosed('x', 'x', 0));
    }

    @Test
    void byteDiagonalStreamsAreEmptyWhenEitherDimensionIsZero() {
        for (final ByteMatrix matrix : new ByteMatrix[] { ByteMatrix.empty(), ByteMatrix.random(0, 3), ByteMatrix.random(3, 0) }) {
            assertEquals(0, matrix.mainDiagonalStream().count());
            assertEquals(0, matrix.antiDiagonalStream().count());
        }
    }
}
