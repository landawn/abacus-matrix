package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.NoSuchElementException;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("base-test")
class NumericExceptionReviewTest {

    @Test
    void copyFactoriesValidateRowsAndKeepDuplicateSourceRowsIndependent() {
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.copyOf(new int[] { 1 }, null));
        assertThrows(IllegalArgumentException.class, () -> LongMatrix.copyOf(new long[] { 1 }, new long[] { 2, 3 }));
        assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.copyOf(null, new double[] { 1 }));
        assertThrows(IllegalArgumentException.class, () -> FloatMatrix.copyOf(new float[] { 1 }, null));

        final int[] ints = { 1 };
        final long[] longs = { 1 };
        final double[] doubles = { 1 };
        final float[] floats = { 1 };
        final IntMatrix intCopy = IntMatrix.copyOf(ints, ints);
        final LongMatrix longCopy = LongMatrix.copyOf(longs, longs);
        final DoubleMatrix doubleCopy = DoubleMatrix.copyOf(doubles, doubles);
        final FloatMatrix floatCopy = FloatMatrix.copyOf(floats, floats);
        intCopy.set(0, 0, 2);
        longCopy.set(0, 0, 2);
        doubleCopy.set(0, 0, 2);
        floatCopy.set(0, 0, 2);
        assertEquals(1, intCopy.get(1, 0));
        assertEquals(1L, longCopy.get(1, 0));
        assertEquals(1d, doubleCopy.get(1, 0));
        assertEquals(1f, floatCopy.get(1, 0));
    }

    @Test
    void mappingRejectsInvalidTypesBeforeInvokingCallbacksEvenForEmptyMatrices() {
        final Class<?> excessiveArrayType = Array.newInstance(Object.class, new int[254]).getClass();
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.empty().mapToObj(value -> null, void.class));
        assertThrows(IllegalArgumentException.class, () -> LongMatrix.empty().mapToObj(value -> null, void.class));
        assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.empty().mapToObj(value -> null, void.class));
        assertThrows(IllegalArgumentException.class, () -> FloatMatrix.empty().mapToObj(value -> null, void.class));
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.empty().mapToObj(value -> null, excessiveArrayType));
        assertThrows(IllegalArgumentException.class, () -> LongMatrix.empty().mapToObj(value -> null, excessiveArrayType));
        assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.empty().mapToObj(value -> null, excessiveArrayType));
        assertThrows(IllegalArgumentException.class, () -> FloatMatrix.empty().mapToObj(value -> null, excessiveArrayType));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void mappingReportsIncompatibleResultValuesAndPreservesCheckedFailures() {
        final Class incompatibleType = String.class;
        assertThrows(ArrayStoreException.class, () -> IntMatrix.wrap(new int[] { 1 }).mapToObj(value -> value, incompatibleType));
        assertThrows(ArrayStoreException.class, () -> LongMatrix.wrap(new long[] { 1 }).mapToObj(value -> value, incompatibleType));
        assertThrows(ArrayStoreException.class, () -> DoubleMatrix.wrap(new double[] { 1 }).mapToObj(value -> value, incompatibleType));
        assertThrows(ArrayStoreException.class, () -> FloatMatrix.wrap(new float[] { 1 }).mapToObj(value -> value, incompatibleType));

        final IOException failure = new IOException("mapper failed");
        assertSame(failure, assertThrows(IOException.class, () -> IntMatrix.wrap(new int[] { 1 }).mapToObj(value -> {
            throw failure;
        }, String.class)));
        assertEquals(1, IntMatrix.wrap(new int[] { 1 }).mapToObj(value -> value, int.class).get(0, 0));
    }

    @Test
    void generatorFailuresPropagateOnlyWhenThereAreCellsToGenerate() {
        final IllegalStateException failure = new IllegalStateException("generator failed");
        final RandomGenerator generator = () -> {
            throw failure;
        };
        assertSame(failure, assertThrows(IllegalStateException.class, () -> IntMatrix.randomRow(1, generator)));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> LongMatrix.randomRow(1, generator)));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> DoubleMatrix.randomRow(1, generator)));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> FloatMatrix.randomRow(1, generator)));
        assertTrue(IntMatrix.random(0, 1, generator).isEmpty());
        assertTrue(LongMatrix.random(0, 1, generator).isEmpty());
        assertTrue(DoubleMatrix.random(0, 1, generator).isEmpty());
        assertTrue(FloatMatrix.random(0, 1, generator).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.random(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> LongMatrix.random(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.random(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> FloatMatrix.random(0, 0, null));
    }

    @Test
    void exhaustedTraversalReportsFailureDuringConsumption() {
        final var intIterator = IntMatrix.wrap(new int[] { 1 }).rowMajorStream().iterator();
        final var longIterator = LongMatrix.wrap(new long[] { 1 }).columnMajorStream().iterator();
        final var doubleIterator = DoubleMatrix.wrap(new double[] { 1 }).mainDiagonalStream().iterator();
        final var floatIterator = FloatMatrix.wrap(new float[] { 1 }).antiDiagonalStream().iterator();
        intIterator.nextInt();
        longIterator.nextLong();
        doubleIterator.nextDouble();
        floatIterator.nextFloat();
        assertThrows(NoSuchElementException.class, intIterator::nextInt);
        assertThrows(NoSuchElementException.class, longIterator::nextLong);
        assertThrows(NoSuchElementException.class, doubleIterator::nextDouble);
        assertThrows(NoSuchElementException.class, floatIterator::nextFloat);
    }
}
