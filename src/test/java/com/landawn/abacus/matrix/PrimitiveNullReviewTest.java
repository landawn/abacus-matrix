package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class PrimitiveNullReviewTest extends TestBase {

    @Test
    void resultFactoriesRejectNullArraysBeforeInspectingTheirShape() {
        assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.wrapResult(null, 0));
        assertThrows(IllegalArgumentException.class, () -> ByteMatrix.wrapResult(null, 0));
        assertThrows(IllegalArgumentException.class, () -> CharMatrix.wrapResult(null, 0));
        assertThrows(IllegalArgumentException.class, () -> ShortMatrix.wrapResult(null, 0));

        final String nullArrayMessage = assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.wrapResult(null, 0)).getMessage();
        assertEquals(nullArrayMessage, assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.wrapResult(null, -1)).getMessage());
        assertEquals(nullArrayMessage, assertThrows(IllegalArgumentException.class, () -> ByteMatrix.wrapResult(null, -1)).getMessage());
        assertEquals(nullArrayMessage, assertThrows(IllegalArgumentException.class, () -> CharMatrix.wrapResult(null, -1)).getMessage());
        assertEquals(nullArrayMessage, assertThrows(IllegalArgumentException.class, () -> ShortMatrix.wrapResult(null, -1)).getMessage());
    }

    @Test
    void characterOutputChecksDestinationThenRequiredCharacterWithoutWriting() {
        final CharMatrix matrix = CharMatrix.empty();
        final String nullOutputMessage = assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(null, 'a')).getMessage();
        assertEquals(nullOutputMessage, assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(null, null)).getMessage());
        assertEquals(nullOutputMessage,
                assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(null, "wrong type")).getMessage());

        final StringBuilder output = new StringBuilder("untouched");
        assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(output, null));
        assertThrows(ClassCastException.class, () -> matrix.appendElementForOutput(output, "wrong type"));
        assertEquals("untouched", output.toString());
    }

    @Test
    void iteratorArrayNullContractRejectsWithoutConsumingElements() {
        final BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[] { true, false });
        final var rows = matrix.rowMajorStream().iterator();
        final var columns = matrix.columnMajorStream().iterator();

        assertThrows(NullPointerException.class, () -> rows.toArray((Boolean[]) null));
        assertThrows(NullPointerException.class, () -> columns.toArray((Boolean[]) null));
        assertTrue(rows.next());
        assertTrue(columns.next());
    }

    @Test
    void nullCoordinateMapperResultsKeepTheirUnboxingFailure() {
        assertThrows(NullPointerException.class, () -> BooleanMatrix.wrap(new boolean[] { true }).updateAll((row, column) -> null));
        assertThrows(NullPointerException.class, () -> ByteMatrix.wrap(new byte[] { 1 }).updateAll((row, column) -> null));
        assertThrows(NullPointerException.class, () -> CharMatrix.wrap(new char[] { 'a' }).updateAll((row, column) -> null));
        assertThrows(NullPointerException.class, () -> ShortMatrix.wrap(new short[] { 1 }).updateAll((row, column) -> null));
    }

    @Test
    void objectMappingCanStillProduceNullElements() {
        assertNull(BooleanMatrix.wrap(new boolean[] { true }).mapToObj(value -> null, String.class).get(0, 0));
        assertNull(ByteMatrix.wrap(new byte[] { 1 }).mapToObj(value -> null, String.class).get(0, 0));
        assertNull(CharMatrix.wrap(new char[] { 'a' }).mapToObj(value -> null, String.class).get(0, 0));
        assertNull(ShortMatrix.wrap(new short[] { 1 }).mapToObj(value -> null, String.class).get(0, 0));
    }

    @Test
    void diagonalFactoriesStillAllowOneMissingDiagonal() {
        assertTrue(BooleanMatrix.ofDiagonals(null, new boolean[] { true }).get(0, 0));
        assertTrue(BooleanMatrix.ofDiagonals(new boolean[] { true }, null).get(0, 0));
        assertEquals((byte) 1, ByteMatrix.ofDiagonals(null, new byte[] { 1 }).get(0, 0));
        assertEquals((byte) 1, ByteMatrix.ofDiagonals(new byte[] { 1 }, null).get(0, 0));
        assertEquals('a', CharMatrix.ofDiagonals(null, new char[] { 'a' }).get(0, 0));
        assertEquals('a', CharMatrix.ofDiagonals(new char[] { 'a' }, null).get(0, 0));
        assertEquals((short) 1, ShortMatrix.ofDiagonals(null, new short[] { 1 }).get(0, 0));
        assertEquals((short) 1, ShortMatrix.ofDiagonals(new short[] { 1 }, null).get(0, 0));
    }

    @Test
    void unboxingStillMapsNullElementsToPrimitiveDefaults() {
        assertFalse(BooleanMatrix.unbox(new Matrix<>(new Boolean[][] { { null } })).get(0, 0));
        assertEquals((byte) 0, ByteMatrix.unbox(new Matrix<>(new Byte[][] { { null } })).get(0, 0));
        assertEquals('\0', CharMatrix.unbox(new Matrix<>(new Character[][] { { null } })).get(0, 0));
        assertEquals((short) 0, ShortMatrix.unbox(new Matrix<>(new Short[][] { { null } })).get(0, 0));
    }

    @Test
    void copyingStillSkipsNullRows() {
        final BooleanMatrix booleans = BooleanMatrix.wrap(new boolean[] { true });
        final ByteMatrix bytes = ByteMatrix.wrap(new byte[] { 1 });
        final CharMatrix chars = CharMatrix.wrap(new char[] { 'a' });
        final ShortMatrix shorts = ShortMatrix.wrap(new short[] { 1 });
        booleans.copyFrom(new boolean[][] { null });
        bytes.copyFrom(new byte[][] { null });
        chars.copyFrom(new char[][] { null });
        shorts.copyFrom(new short[][] { null });
        assertTrue(booleans.get(0, 0));
        assertEquals((byte) 1, bytes.get(0, 0));
        assertEquals('a', chars.get(0, 0));
        assertEquals((short) 1, shorts.get(0, 0));
    }
}
