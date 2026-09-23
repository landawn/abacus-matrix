package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;

/**
 * Pins the validation order: an invalid index/range argument is reported before a {@code null}
 * later argument, and the operands of a three-matrix zip are validated one argument at a time.
 */
class ValidationOrderTest extends TestBase {

    @Test
    void indexIsCheckedBeforeNullRowOrOperator() {
        final BooleanMatrix bm = BooleanMatrix.wrap(new boolean[][] { { true, false } });
        assertThrows(IndexOutOfBoundsException.class, () -> bm.setRow(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> bm.setColumn(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> bm.updateRow(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> bm.updateColumn(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> bm.copyFrom(5, 0, null));
        assertThrows(IndexOutOfBoundsException.class, () -> bm.forEach(0, 9, 0, 1, null));

        final ByteMatrix by = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
        assertThrows(IndexOutOfBoundsException.class, () -> by.setRow(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> by.updateColumn(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> by.forEach(0, 9, 0, 1, null));

        final CharMatrix cm = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
        assertThrows(IndexOutOfBoundsException.class, () -> cm.setColumn(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> cm.copyFrom(0, 5, null));

        final ShortMatrix sm = ShortMatrix.wrap(new short[][] { { 1, 2 } });
        assertThrows(IndexOutOfBoundsException.class, () -> sm.updateRow(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> sm.forEach(0, 1, 0, 9, null));

        final IntMatrix im = IntMatrix.wrap(new int[][] { { 1, 2 } });
        assertThrows(IndexOutOfBoundsException.class, () -> im.setRow(-1, null));
        assertThrows(IndexOutOfBoundsException.class, () -> im.setColumn(2, null));
        assertThrows(IndexOutOfBoundsException.class, () -> im.updateRow(1, null));
        assertThrows(IndexOutOfBoundsException.class, () -> im.updateColumn(-1, null));
        assertThrows(IndexOutOfBoundsException.class, () -> im.copyFrom(-1, 0, null));
        assertThrows(IndexOutOfBoundsException.class, () -> im.forEach(1, 0, 0, 1, null));

        final LongMatrix lm = LongMatrix.wrap(new long[][] { { 1, 2 } });
        assertThrows(IndexOutOfBoundsException.class, () -> lm.setRow(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> lm.copyFrom(5, 0, null));

        final FloatMatrix fm = FloatMatrix.wrap(new float[][] { { 1, 2 } });
        assertThrows(IndexOutOfBoundsException.class, () -> fm.setColumn(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> fm.updateRow(5, null));

        final DoubleMatrix dm = DoubleMatrix.wrap(new double[][] { { 1, 2 } });
        assertThrows(IndexOutOfBoundsException.class, () -> dm.updateColumn(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> dm.forEach(0, 9, 0, 1, null));

        final Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" } });
        assertThrows(IndexOutOfBoundsException.class, () -> m.setRow(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> m.updateRow(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> m.updateColumn(5, null));
        assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(5, 0, null));
        assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 9, 0, 1, null));
    }

    @Test
    void validIndexWithNullArgumentStillThrowsIllegalArgumentException() {
        final IntMatrix im = IntMatrix.wrap(new int[][] { { 1, 2 } });
        assertThrows(IllegalArgumentException.class, () -> im.setRow(0, null));
        assertThrows(IllegalArgumentException.class, () -> im.updateColumn(1, null));
        assertThrows(IllegalArgumentException.class, () -> im.forEach(0, 1, 0, 2, null));
    }

    @Test
    void rangeIsCheckedBeforeNullCallbackInIndexHelpers() {
        final IntMatrix im = IntMatrix.wrap(new int[][] { { 1, 2 } });
        assertThrows(IndexOutOfBoundsException.class, () -> im.forEachIndices(0, 9, 0, 1, (Throwables.IntBiConsumer<RuntimeException>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> Matrices.forEachIndices(1, 0, 0, 1, null, false));
        assertThrows(IndexOutOfBoundsException.class, () -> Matrices.mapIndices(1, 0, 0, 1, null, false));
        assertThrows(IndexOutOfBoundsException.class, () -> Matrices.mapIndicesToInt(1, 0, 0, 1, null, false));
    }

    @Test
    void threeMatrixZipValidatesEachOperandInArgumentOrder() {
        final IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2 } });
        final IntMatrix mismatched = IntMatrix.wrap(new int[][] { { 1 } });

        // other's shape is reported before a null third
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> a.zipWith(mismatched, null, (x, y, z) -> x));
        assertTrue(e1.getMessage().contains("other is 1x1"), e1.getMessage());

        // third's shape is reported on its own
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> a.zipWith(a, mismatched, (x, y, z) -> x));
        assertTrue(e2.getMessage().contains("third is 1x1"), e2.getMessage());

        // a shape mismatch is reported before a null zip function
        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class,
                () -> a.zipWith(mismatched, (Throwables.IntBinaryOperator<RuntimeException>) null));
        assertTrue(e3.getMessage().contains("different shapes"), e3.getMessage());

        final DoubleMatrix d = DoubleMatrix.wrap(new double[][] { { 1, 2 } });
        final IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class,
                () -> d.zipWith(DoubleMatrix.wrap(new double[][] { { 1 } }), null, (x, y, z) -> x));
        assertTrue(e4.getMessage().contains("other is 1x1"), e4.getMessage());

        final IllegalArgumentException e5 = assertThrows(IllegalArgumentException.class, () -> Matrices.zip(a, mismatched, null, (x, y, z) -> x));
        assertTrue(e5.getMessage().contains("second is 1x1"), e5.getMessage());
    }

    @Test
    void genericThreeMatrixZipValidatesEachOperandInArgumentOrderLikePrimitives() {
        final Matrix<Integer> a = Matrix.wrap(Integer.class, new Integer[][] { { 1, 2 } });
        final Matrix<Integer> mismatched = Matrix.wrap(Integer.class, new Integer[][] { { 1 } });

        // other's shape is reported before a null third (previously the null third was reported first)
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> a.zipWith(mismatched, null, (x, y, z) -> x));
        assertTrue(e1.getMessage().contains("but other is 1x1"), e1.getMessage());

        final IllegalArgumentException e1Typed = assertThrows(IllegalArgumentException.class,
                () -> a.zipWith(mismatched, null, (x, y, z) -> x, Integer.class));
        assertTrue(e1Typed.getMessage().contains("but other is 1x1"), e1Typed.getMessage());

        // third's shape is reported on its own, in the same wording as the primitive variants
        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> a.zipWith(a, mismatched, (x, y, z) -> x));
        assertEquals("Cannot zip matrices with different shapes: this is 1x2 but third is 1x1", e2.getMessage());

        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> a.zipWith(a, null, (x, y, z) -> x));
        assertTrue(e3.getMessage().contains("third"), e3.getMessage());
    }
}
