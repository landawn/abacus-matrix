package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.NoSuchElementException;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.util.Throwables;

/** Shared contracts intentionally exercised across matrix implementations. */
class MatrixValidationContractTest extends MatrixTestSupport {

    @Nested
    class NumericFailures {

        @Test
        public void testCopyFactoriesValidateRowsAndKeepDuplicateSourceRowsIndependent() {
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
        public void testMappingRejectsInvalidTypesBeforeInvokingCallbacksEvenForEmptyMatrices() {
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
        public void testMappingReportsIncompatibleResultValuesAndPreservesCheckedFailures() {
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
        public void testGeneratorFailuresPropagateOnlyWhenThereAreCellsToGenerate() {
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
        public void testExhaustedTraversalReportsFailureDuringConsumption() {
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

    @Nested
    class PrimitiveRangeEndpoints {

        @Test
        public void testClosedRangesWithEqualBoundsContainOneValueForEitherStepDirection() {
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
    }

    @Nested
    class PrimitiveFailures {

        private static void assertSameArgumentFailure(final Executable constructor, final Executable copy) {
            final IllegalArgumentException expected = assertThrows(IllegalArgumentException.class, constructor);
            final IllegalArgumentException actual = assertThrows(IllegalArgumentException.class, copy);
            assertEquals(expected.getMessage(), actual.getMessage());
        }

        @Test
        public void testCopyFactoriesPreserveValidationFailureOrderAndMessages() {
            final boolean[][] booleans = { { true }, { false, true }, null };
            final byte[][] bytes = { { 1 }, { 2, 3 }, null };
            final char[][] chars = { { 'a' }, { 'b', 'c' }, null };
            final short[][] shorts = { { 1 }, { 2, 3 }, null };

            assertSameArgumentFailure(() -> BooleanMatrix.wrap(booleans), () -> BooleanMatrix.copyOf(booleans));
            assertSameArgumentFailure(() -> ByteMatrix.wrap(bytes), () -> ByteMatrix.copyOf(bytes));
            assertSameArgumentFailure(() -> CharMatrix.wrap(chars), () -> CharMatrix.copyOf(chars));
            assertSameArgumentFailure(() -> ShortMatrix.wrap(shorts), () -> ShortMatrix.copyOf(shorts));

            assertSameArgumentFailure(() -> BooleanMatrix.wrap((boolean[]) null), () -> BooleanMatrix.copyOf((boolean[]) null));
            assertSameArgumentFailure(() -> ByteMatrix.wrap((byte[]) null), () -> ByteMatrix.copyOf((byte[]) null));
            assertSameArgumentFailure(() -> CharMatrix.wrap((char[]) null), () -> CharMatrix.copyOf((char[]) null));
            assertSameArgumentFailure(() -> ShortMatrix.wrap((short[]) null), () -> ShortMatrix.copyOf((short[]) null));
        }

        @Test
        public void testCopyFactoriesStillAcceptRepeatedRowsAndOwnIndependentCopies() {
            final boolean[] booleans = { true };
            final byte[] bytes = { 1 };
            final char[] chars = { 'a' };
            final short[] shorts = { 1 };
            final BooleanMatrix booleanCopy = BooleanMatrix.copyOf(booleans, booleans);
            final ByteMatrix byteCopy = ByteMatrix.copyOf(bytes, bytes);
            final CharMatrix charCopy = CharMatrix.copyOf(chars, chars);
            final ShortMatrix shortCopy = ShortMatrix.copyOf(shorts, shorts);

            assertNotSame(booleans, booleanCopy.rowView(0));
            assertNotSame(booleanCopy.rowView(0), booleanCopy.rowView(1));
            assertNotSame(bytes, byteCopy.rowView(0));
            assertNotSame(byteCopy.rowView(0), byteCopy.rowView(1));
            assertNotSame(chars, charCopy.rowView(0));
            assertNotSame(charCopy.rowView(0), charCopy.rowView(1));
            assertNotSame(shorts, shortCopy.rowView(0));
            assertNotSame(shortCopy.rowView(0), shortCopy.rowView(1));
        }

        @Test
        public void testSuppliedRandomGeneratorFailuresPropagateOnlyWhenGeneratingElements() {
            final IllegalStateException failure = new IllegalStateException("random source unavailable");
            final RandomGenerator generator = () -> {
                throw failure;
            };

            assertEquals(3, BooleanMatrix.random(0, 3, generator).columnCount());
            assertEquals(3, ByteMatrix.random(0, 3, generator).columnCount());
            assertEquals(3, CharMatrix.random(0, 3, generator).columnCount());
            assertEquals(3, ShortMatrix.random(0, 3, generator).columnCount());
            assertSame(failure, assertThrows(IllegalStateException.class, () -> BooleanMatrix.randomRow(1, generator)));
            assertSame(failure, assertThrows(IllegalStateException.class, () -> ByteMatrix.randomRow(1, generator)));
            assertSame(failure, assertThrows(IllegalStateException.class, () -> CharMatrix.randomRow(1, generator)));
            assertSame(failure, assertThrows(IllegalStateException.class, () -> ShortMatrix.randomRow(1, generator)));

            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.random(0, 3, null));
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.random(0, 3, null));
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.random(0, 3, null));
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.random(0, 3, null));
        }

        @Test
        public void testObjectMappingRejectsVoidEvenWhenThereAreNoCells() {
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.empty().mapToObj(value -> null, void.class));
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.empty().mapToObj(value -> null, void.class));
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.empty().mapToObj(value -> null, void.class));
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.empty().mapToObj(value -> null, void.class));
        }
    }

    @Nested
    class ArgumentPrecedence {

        @Test
        public void testIndexIsCheckedBeforeNullRowOrOperator() {
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
        public void testValidIndexWithNullArgumentStillThrowsIllegalArgumentException() {
            final IntMatrix im = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> im.setRow(0, null));
            assertThrows(IllegalArgumentException.class, () -> im.updateColumn(1, null));
            assertThrows(IllegalArgumentException.class, () -> im.forEach(0, 1, 0, 2, null));
        }

        @Test
        public void testRangeIsCheckedBeforeNullCallbackInIndexHelpers() {
            final IntMatrix im = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> im.forEachIndices(0, 9, 0, 1, (Throwables.IntBiConsumer<RuntimeException>) null));
            assertThrows(IndexOutOfBoundsException.class, () -> Matrices.forEachIndices(1, 0, 0, 1, null, false));
            assertThrows(IndexOutOfBoundsException.class, () -> Matrices.mapIndices(1, 0, 0, 1, null, false));
            assertThrows(IndexOutOfBoundsException.class, () -> Matrices.mapIndicesToInt(1, 0, 0, 1, null, false));
        }

        @Test
        public void testThreeMatrixZipValidatesEachOperandInArgumentOrder() {
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
        public void testGenericThreeMatrixZipValidatesEachOperandInArgumentOrderLikePrimitives() {
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
}
