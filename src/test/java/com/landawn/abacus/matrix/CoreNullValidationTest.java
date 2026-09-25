package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class CoreNullValidationTest extends TestBase {

    @Test
    void rowSnapshotRejectsNullOnlyWhenThereAreBackingRows() {
        assertNull(IntMatrix.empty().snapshotRowsIfBackingRows(null));
        assertNull(IntMatrix.empty().resize(0, 3).snapshotRowsIfBackingRows(null));

        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> IntMatrix.wrap(new int[] { 1 }).snapshotRowsIfBackingRows(null));
        assertTrue(failure.getMessage().contains(cs.source));
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(new int[0]).snapshotRowsIfBackingRows(null));

        final IntMatrix matrix = IntMatrix.wrap(new int[] { 1 });
        final int[][] rows = { null, new int[] { 2 } };
        assertSame(rows, matrix.snapshotRowsIfBackingRows(rows));
        assertNull(matrix.snapshotIfBackingRow(null));
    }

    @Test
    void elementOutputValidatesDestinationBeforeRenderingAndPreservesNullableValues() throws Exception {
        final Matrix<Object> matrix = Matrix.empty();
        final NullPointerException renderingFailure = new NullPointerException("element rendering failed");
        final Object value = new Object() {
            @Override
            public String toString() {
                throw renderingFailure;
            }
        };

        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> matrix.appendElementForOutput(null, value));
        assertTrue(failure.getMessage().contains(cs.output));
        assertSame(renderingFailure,
                assertThrows(NullPointerException.class, () -> matrix.appendElementForOutput(new StringBuilder(), value)));

        final StringBuilder output = new StringBuilder();
        matrix.appendElementForOutput(output, null);
        assertEquals("null", output.toString());
    }

    @Test
    void outputImplementationNullPointerExceptionIsNotTranslated() {
        final NullPointerException failure = new NullPointerException("output implementation failed");
        final Appendable output = new Appendable() {
            @Override
            public Appendable append(final CharSequence value) {
                throw failure;
            }

            @Override
            public Appendable append(final CharSequence value, final int start, final int end) {
                throw failure;
            }

            @Override
            public Appendable append(final char value) {
                throw failure;
            }
        };

        assertSame(failure, assertThrows(NullPointerException.class, () -> Matrix.empty().appendElementForOutput(output, null)));
        assertSame(failure, assertThrows(NullPointerException.class, () -> IntMatrix.empty().appendTo(output)));
    }
}
