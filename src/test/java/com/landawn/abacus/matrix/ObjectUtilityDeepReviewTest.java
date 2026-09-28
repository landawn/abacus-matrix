package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class ObjectUtilityDeepReviewTest extends TestBase {

    @Test
    void voidWrapperRemainsAValidReferenceElementTypeIncludingEmptyDimensions() {
        for (final int rows : new int[] { 0, 2 }) {
            for (final int columns : new int[] { 0, 3 }) {
                final Void[][] result = Matrices.newMatrixArray(rows, columns, Void.class);

                assertSame(Void[][].class, result.getClass());
                assertEquals(rows, result.length);

                for (final Void[] row : result) {
                    assertSame(Void[].class, row.getClass());
                    assertEquals(columns, row.length);

                    for (final Void value : row) {
                        assertNull(value);
                    }
                }

                if (rows > 1) {
                    assertNotSame(result[0], result[1]);
                }
            }
        }
    }
}
