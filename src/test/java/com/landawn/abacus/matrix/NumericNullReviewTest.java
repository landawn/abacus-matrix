package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Throwables;

@Tag("base-test")
class NumericNullReviewTest {

    @Test
    void resultFactoriesRejectNullArraysConsistentlyWithPublicFactories() {
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrapResult(null, 0));
        assertThrows(IllegalArgumentException.class, () -> LongMatrix.wrapResult(null, 0));
        assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.wrapResult(null, 0));
        assertThrows(IllegalArgumentException.class, () -> FloatMatrix.wrapResult(null, 0));

        assertSame(IntMatrix.empty(), IntMatrix.wrapResult(new int[0][], 0));
        assertSame(LongMatrix.empty(), LongMatrix.wrapResult(new long[0][], 0));
        assertSame(DoubleMatrix.empty(), DoubleMatrix.wrapResult(new double[0][], 0));
        assertSame(FloatMatrix.empty(), FloatMatrix.wrapResult(new float[0][], 0));
        assertEquals(3, IntMatrix.wrapResult(new int[0][], 3).columnCount());
        assertEquals(3, LongMatrix.wrapResult(new long[0][], 3).columnCount());
        assertEquals(3, DoubleMatrix.wrapResult(new double[0][], 3).columnCount());
        assertEquals(3, FloatMatrix.wrapResult(new float[0][], 3).columnCount());
    }

    @Test
    void nullCallbackResultsStillFailOnlyWhenUnboxed() {
        assertThrows(NullPointerException.class, () -> IntMatrix.wrap(new int[] { 1 }).updateAll((i, j) -> (Integer) null));
        assertThrows(NullPointerException.class, () -> LongMatrix.wrap(new long[] { 1 }).updateAll((i, j) -> (Long) null));
        assertThrows(NullPointerException.class, () -> DoubleMatrix.wrap(new double[] { 1 }).updateAll((i, j) -> (Double) null));
        assertThrows(NullPointerException.class, () -> FloatMatrix.wrap(new float[] { 1 }).updateAll((i, j) -> (Float) null));
        assertDoesNotThrow(() -> IntMatrix.empty().updateAll((i, j) -> (Integer) null));
        assertDoesNotThrow(() -> LongMatrix.empty().updateAll((i, j) -> (Long) null));
        assertDoesNotThrow(() -> DoubleMatrix.empty().updateAll((i, j) -> (Double) null));
        assertDoesNotThrow(() -> FloatMatrix.empty().updateAll((i, j) -> (Float) null));

        assertThrows(IllegalArgumentException.class, () -> IntMatrix.empty().updateAll((Throwables.IntBiFunction<Integer, RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> LongMatrix.empty().updateAll((Throwables.IntBiFunction<Long, RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.empty().updateAll((Throwables.IntBiFunction<Double, RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> FloatMatrix.empty().updateAll((Throwables.IntBiFunction<Float, RuntimeException>) null));
    }

    @Test
    void objectMappingAndUnboxingKeepTheirDifferentNullValueContracts() {
        assertNull(IntMatrix.wrap(new int[] { 1 }).mapToObj(value -> null, Object.class).get(0, 0));
        assertNull(LongMatrix.wrap(new long[] { 1 }).mapToObj(value -> null, Object.class).get(0, 0));
        assertNull(DoubleMatrix.wrap(new double[] { 1 }).mapToObj(value -> null, Object.class).get(0, 0));
        assertNull(FloatMatrix.wrap(new float[] { 1 }).mapToObj(value -> null, Object.class).get(0, 0));
        assertEquals(0, IntMatrix.unbox(Matrix.wrap(new Integer[][] { { null } })).get(0, 0));
        assertEquals(0L, LongMatrix.unbox(Matrix.wrap(new Long[][] { { null } })).get(0, 0));
        assertEquals(0d, DoubleMatrix.unbox(Matrix.wrap(new Double[][] { { null } })).get(0, 0));
        assertEquals(0f, FloatMatrix.unbox(Matrix.wrap(new Float[][] { { null } })).get(0, 0));
    }

    @Test
    void optionalDiagonalsAndSkippedSourceRowsStillAcceptNull() {
        final IntMatrix ints = IntMatrix.ofDiagonals(null, new int[] { 7 });
        final LongMatrix longs = LongMatrix.ofDiagonals(null, new long[] { 7 });
        final DoubleMatrix doubles = DoubleMatrix.ofDiagonals(new double[] { 7 }, null);
        final FloatMatrix floats = FloatMatrix.ofDiagonals(new float[] { 7 }, null);
        ints.copyFrom(new int[][] { null });
        longs.copyFrom(new long[][] { null });
        doubles.copyFrom(new double[][] { null });
        floats.copyFrom(new float[][] { null });
        assertEquals(7, ints.get(0, 0));
        assertEquals(7L, longs.get(0, 0));
        assertEquals(7d, doubles.get(0, 0));
        assertEquals(7f, floats.get(0, 0));
    }

    @Test
    void nullableObjectAndRowHooksKeepTheirExistingResults() {
        assertFalse(IntMatrix.empty().equals(null));
        assertFalse(LongMatrix.empty().equals(null));
        assertFalse(DoubleMatrix.empty().equals(null));
        assertFalse(FloatMatrix.empty().equals(null));
        assertEquals(0, IntMatrix.empty().length(null));
        assertEquals(0, LongMatrix.empty().length(null));
        assertEquals(0, DoubleMatrix.empty().length(null));
        assertEquals(0, FloatMatrix.empty().length(null));
    }
}
