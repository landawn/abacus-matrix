package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.Stream;

class ReviewFixRegressionTest {

    @Test
    void zeroByNShapeSurvivesNumericOperations() {
        LongMatrix longs = LongMatrix.empty().resize(0, 4);
        assertShape(longs, 0, 4);
        assertShape(longs.copy(), 0, 4);
        assertShape(longs.rotate180(), 0, 4);
        assertShape(longs.transpose(), 4, 0);
        assertShape(longs.rotate90(), 4, 0);
        assertShape(longs.map(v -> v + 1), 0, 4);
        assertShape(longs.toDoubleMatrix(), 0, 4);
        assertEquals(4L, longs.columnStreams().count());

        FloatMatrix floats = FloatMatrix.empty().resize(0, 3);
        assertShape(floats, 0, 3);
        assertShape(floats.transpose(), 3, 0);
        assertShape(floats.toLongMatrix(), 0, 3);
        assertEquals(3L, floats.columnStreams().count());

        DoubleMatrix doubles = DoubleMatrix.empty().resize(0, 2);
        assertShape(doubles, 0, 2);
        assertShape(doubles.transpose(), 2, 0);
        assertShape(doubles.toFloatMatrix(), 0, 2);
        assertEquals(2L, doubles.columnStreams().count());
    }

    @Test
    void randomFactoriesAcceptReproducibleGeneratorsAndPreserveZeroByN() {
        assertEquals(LongMatrix.random(3, 4, new Random(17)), LongMatrix.random(3, 4, new Random(17)));
        assertEquals(FloatMatrix.random(3, 4, new Random(17)), FloatMatrix.random(3, 4, new Random(17)));
        assertEquals(DoubleMatrix.random(3, 4, new Random(17)), DoubleMatrix.random(3, 4, new Random(17)));

        assertShape(LongMatrix.random(0, 5, new Random(1)), 0, 5);
        assertShape(FloatMatrix.random(0, 5, new Random(1)), 0, 5);
        assertShape(DoubleMatrix.random(0, 5, new Random(1)), 0, 5);

        assertThrows(IllegalArgumentException.class, () -> LongMatrix.randomRow(1, (RandomGenerator) null));
        assertThrows(IllegalArgumentException.class, () -> FloatMatrix.random(1, 1, null));
        assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.random(1, 1, null));
    }

    @Test
    void rectangularDiagonalsUseTheShorterDimension() {
        LongMatrix wide = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        assertArrayEquals(new long[] { 1, 5 }, wide.mainDiagonalCopy());
        assertArrayEquals(new long[] { 3, 5 }, wide.antiDiagonalCopy());
        assertEquals(List.of(Point.of(0, 0), Point.of(1, 1)), wide.mainDiagonalPoints().toList());
        assertEquals(List.of(Point.of(0, 2), Point.of(1, 1)), wide.antiDiagonalPoints().toList());
        wide.setMainDiagonal(new long[] { 10, 20 });
        wide.setAntiDiagonal(new long[] { 30, 40 });
        assertArrayEquals(new long[] { 10, 40 }, wide.mainDiagonalStream().toArray());
        assertArrayEquals(new long[] { 30, 40 }, wide.antiDiagonalStream().toArray());

        LongMatrix tall = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
        assertArrayEquals(new long[] { 1, 4 }, tall.mainDiagonalCopy());
        assertArrayEquals(new long[] { 2, 3 }, tall.antiDiagonalCopy());

        FloatMatrix floatWide = FloatMatrix.wrap(new float[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        assertArrayEquals(new float[] { 1, 5 }, floatWide.mainDiagonalCopy());
        assertArrayEquals(new float[] { 3, 5 }, floatWide.antiDiagonalCopy());

        DoubleMatrix doubleTall = DoubleMatrix.wrap(new double[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
        assertArrayEquals(new double[] { 1, 4 }, doubleTall.mainDiagonalCopy());
        assertArrayEquals(new double[] { 2, 3 }, doubleTall.antiDiagonalCopy());
    }

    @Test
    void longExactArithmeticReportsEveryOverflowSite() {
        LongMatrix one = LongMatrix.wrap(new long[][] { { 1 } });
        assertEquals(LongMatrix.wrap(new long[][] { { 3 } }), one.addExact(LongMatrix.wrap(new long[][] { { 2 } })));
        assertEquals(LongMatrix.wrap(new long[][] { { -1 } }), one.subtractExact(LongMatrix.wrap(new long[][] { { 2 } })));
        assertEquals(LongMatrix.wrap(new long[][] { { 11 } }),
                LongMatrix.wrap(new long[][] { { 1, 2 } }).matrixMultiplyExact(LongMatrix.wrap(new long[][] { { 3 }, { 4 } })));

        assertThrows(ArithmeticException.class,
                () -> LongMatrix.wrap(new long[][] { { Long.MAX_VALUE } }).addExact(one));
        assertThrows(ArithmeticException.class,
                () -> LongMatrix.wrap(new long[][] { { Long.MIN_VALUE } }).subtractExact(one));
        assertThrows(ArithmeticException.class, () -> LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, 0 } })
                .matrixMultiplyExact(LongMatrix.wrap(new long[][] { { 2 }, { 0 } })));
        assertThrows(ArithmeticException.class, () -> LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, 1 } })
                .matrixMultiplyExact(LongMatrix.wrap(new long[][] { { 1 }, { 1 } })));
    }

    @Test
    void iteratorCountConsumesTheRemainingElementsAndToArrayStillWorks() {
        LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        assertCountExhausts(matrix.mainDiagonalStream().iterator(), 2);
        assertCountExhausts(matrix.antiDiagonalStream().iterator(), 2);
        assertCountExhausts(matrix.rowMajorStream().iterator(), 6);
        assertCountExhausts(matrix.columnMajorStream().iterator(), 6);
        assertCountExhausts(matrix.rowStreams().iterator(), 2);
        assertCountExhausts(matrix.columnStreams().iterator(), 3);

        LongIterator remaining = matrix.rowMajorStream().iterator();
        assertEquals(1L, remaining.nextLong());
        assertArrayEquals(new long[] { 2, 3, 4, 5, 6 }, remaining.toArray());

        FloatIterator floatIterator = FloatMatrix.wrap(new float[][] { { 1, 2 } }).rowMajorStream().iterator();
        assertEquals(2L, floatIterator.count());
        assertFalse(floatIterator.hasNext());

        DoubleIterator doubleIterator = DoubleMatrix.wrap(new double[][] { { 1, 2 } }).rowMajorStream().iterator();
        assertEquals(2L, doubleIterator.count());
        assertFalse(doubleIterator.hasNext());
    }

    @Test
    void mappedIndexStreamsAreLazyAndSequentialOrderIsRowMajor() {
        AtomicInteger calls = new AtomicInteger();
        Stream<String> mapped = Matrices.mapIndices(3, 2, (i, j) -> {
            calls.incrementAndGet();
            return i + "," + j;
        }, false);
        assertEquals(0, calls.get());
        assertEquals(List.of("0,0"), mapped.limit(1).toList());
        assertEquals(1, calls.get());

        assertEquals(List.of("0,0", "0,1", "1,0", "1,1", "2,0", "2,1"),
                Matrices.mapIndices(3, 2, (i, j) -> i + "," + j, false).toList());

        AtomicInteger intCalls = new AtomicInteger();
        com.landawn.abacus.util.stream.IntStream ints = Matrices.mapIndicesToInt(3, 2, (i, j) -> {
            intCalls.incrementAndGet();
            return i * 10 + j;
        }, false);
        assertEquals(0, intCalls.get());
        assertArrayEquals(new int[] { 0 }, ints.limit(1).toArray());
        assertEquals(1, intCalls.get());

        Stream<Integer> deferredFailure = Matrices.mapIndices(1, 1,
                (Throwables.IntBiFunction<Integer, IOException>) (i, j) -> { throw new IOException("boom"); }, false);
        assertThrows(RuntimeException.class, deferredFailure::toList);
    }

    @Test
    void typedGenericCollectionZipUsesDeclaredArrayTypesAndPreservesShape() {
        Matrix<Number> first = Matrix.wrap(Number.class, new Number[][] { { 1, 2 } });
        Matrix<Number> second = Matrix.wrap(Number.class, new Number[][] { { 0.5, 1.5 } });

        Matrix<Number> sums = Matrices.zip(List.of(first, second), (left, right) -> left.doubleValue() + right.doubleValue(), Number.class);
        assertEquals(Number.class, sums.elementType());
        assertArrayEquals(new Number[] { 1.5, 3.5 }, sums.rowCopy(0));

        Matrix<String> joined = Matrices.zip(List.of(first, second), values -> values[0] + ":" + values[1], Number.class, String.class);
        assertEquals(String.class, joined.elementType());
        assertArrayEquals(new String[] { "1:0.5", "2:1.5" }, joined.rowCopy(0));

        Matrix<Number> zeroByThree = Matrix.empty(Number.class, 3);
        assertShape(Matrices.zip(List.of(zeroByThree), (left, right) -> left, Number.class), 0, 3);
        assertShape(Matrices.zip(List.of(zeroByThree), values -> "unused", Number.class, String.class), 0, 3);
    }

    @Test
    void naryStackAllocatesIndependentRowsAndPreservesZeroByNShapes() {
        IntMatrix first = IntMatrix.wrap(new int[][] { { 1, 2 } });
        IntMatrix second = IntMatrix.wrap(new int[][] { { 3, 4 } });
        IntMatrix vertical = Matrices.stackVertically(List.of(first, second));
        first.set(0, 0, 99);
        vertical.set(1, 0, 88);
        assertArrayEquals(new int[] { 1, 2 }, vertical.rowCopy(0));
        assertArrayEquals(new int[] { 3, 4 }, second.rowCopy(0));

        IntMatrix zeroByThree = IntMatrix.empty().resize(0, 3);
        assertShape(Matrices.stackVertically(List.of(zeroByThree, zeroByThree)), 0, 3);
        assertShape(Matrices.stackHorizontally(List.of(IntMatrix.empty().resize(0, 2), zeroByThree)), 0, 5);
    }

    @Test
    void appendToWritesIncrementallyAndWrapsSinkFailures() {
        CountingAppendable output = new CountingAppendable(false);
        LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } }).appendTo(output);
        assertEquals("[1, 2]\n[3, 4]", output.toString());
        assertTrue(output.appendCalls > 1);

        assertThrows(UncheckedIOException.class,
                () -> LongMatrix.wrap(new long[][] { { 1 } }).appendTo(new CountingAppendable(true)));
    }

    @Test
    void newThreadsDefaultToSequentialPolicy() throws InterruptedException {
        AtomicReference<ParallelMode> observed = new AtomicReference<>();
        Thread thread = new Thread(() -> observed.set(Matrices.getParallelMode()));
        thread.start();
        thread.join();
        assertEquals(ParallelMode.FORCE_OFF, observed.get());
    }

    private static void assertCountExhausts(final LongIterator iterator, final long expected) {
        assertEquals(expected, iterator.count());
        assertFalse(iterator.hasNext());
    }

    private static void assertCountExhausts(final ObjIterator<?> iterator, final long expected) {
        assertEquals(expected, iterator.count());
        assertFalse(iterator.hasNext());
    }

    private static void assertShape(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int rows, final int columns) {
        assertEquals(rows, matrix.rowCount());
        assertEquals(columns, matrix.columnCount());
    }

    private static final class CountingAppendable implements Appendable {
        private final StringBuilder value = new StringBuilder();
        private final boolean fail;
        private int appendCalls;

        private CountingAppendable(final boolean fail) {
            this.fail = fail;
        }

        @Override
        public Appendable append(final CharSequence csq) throws IOException {
            beforeAppend();
            value.append(csq);
            return this;
        }

        @Override
        public Appendable append(final CharSequence csq, final int start, final int end) throws IOException {
            beforeAppend();
            value.append(csq, start, end);
            return this;
        }

        @Override
        public Appendable append(final char c) throws IOException {
            beforeAppend();
            value.append(c);
            return this;
        }

        private void beforeAppend() throws IOException {
            appendCalls++;

            if (fail) {
                throw new IOException("sink failed");
            }
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }
}
