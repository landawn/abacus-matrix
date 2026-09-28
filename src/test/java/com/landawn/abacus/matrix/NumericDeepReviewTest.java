package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class NumericDeepReviewTest extends TestBase {

    @Test
    void intClosedRangeIncludesEqualEndpointsForEitherStepDirection() {
        for (int endpoint : new int[] { Integer.MIN_VALUE, 0, Integer.MAX_VALUE }) {
            for (int step : new int[] { Integer.MIN_VALUE, -1, 1, Integer.MAX_VALUE }) {
                final IntMatrix result = IntMatrix.rangeClosed(endpoint, endpoint, step);
                assertEquals(1, result.rowCount());
                assertArrayEquals(new int[] { endpoint }, result.rowCopy(0));
            }
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.rangeClosed(endpoint, endpoint, 0));
        }
        assertEquals(0, IntMatrix.rangeClosed(1, 2, -1).columnCount());
        assertEquals(0, IntMatrix.rangeClosed(2, 1, 1).columnCount());
    }

    @Test
    void longClosedRangeIncludesEqualEndpointsForEitherStepDirection() {
        for (long endpoint : new long[] { Long.MIN_VALUE, 0L, Long.MAX_VALUE }) {
            for (long step : new long[] { Long.MIN_VALUE, -1L, 1L, Long.MAX_VALUE }) {
                final LongMatrix result = LongMatrix.rangeClosed(endpoint, endpoint, step);
                assertEquals(1, result.rowCount());
                assertArrayEquals(new long[] { endpoint }, result.rowCopy(0));
            }
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.rangeClosed(endpoint, endpoint, 0L));
        }
        assertEquals(0, LongMatrix.rangeClosed(1L, 2L, -1L).columnCount());
        assertEquals(0, LongMatrix.rangeClosed(2L, 1L, 1L).columnCount());
    }
}
