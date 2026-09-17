package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.Stream;

class ReviewFixRegressionTest extends TestBase {

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

    // ================================================================================
    // 2026-09-16 review cycle 1 - shape preservation, empty canonicalisation, exact arithmetic
    // ================================================================================

    /** C-001: an N x 0 matrix must transpose/rotate to 0 x N in every variant. */
    @Test
    void nByZeroTransposeAndRotationPreserveTheColumnCountInEveryVariant() {
        assertShape(BooleanMatrix.wrap(new boolean[2][0]).transpose(), 0, 2);
        assertShape(ByteMatrix.wrap(new byte[2][0]).transpose(), 0, 2);
        assertShape(CharMatrix.wrap(new char[2][0]).transpose(), 0, 2);
        assertShape(ShortMatrix.wrap(new short[2][0]).transpose(), 0, 2);
        assertShape(IntMatrix.wrap(new int[2][0]).transpose(), 0, 2);
        assertShape(LongMatrix.wrap(new long[2][0]).transpose(), 0, 2);
        assertShape(FloatMatrix.wrap(new float[2][0]).transpose(), 0, 2);
        assertShape(DoubleMatrix.wrap(new double[2][0]).transpose(), 0, 2);
        assertShape(Matrix.wrap(new String[2][0]).transpose(), 0, 2);

        for (final AbstractMatrix<?, ?, ?, ?, ?> m : List.of(LongMatrix.wrap(new long[3][0]), FloatMatrix.wrap(new float[3][0]),
                DoubleMatrix.wrap(new double[3][0]), IntMatrix.wrap(new int[3][0]), ByteMatrix.wrap(new byte[3][0]))) {
            assertShape(m.rotate90(), 0, 3);
            assertShape(m.rotate270(), 0, 3);
            assertShape(m.rotate180(), 3, 0);
            // Two 90-degree rotations must agree with one 180-degree rotation.
            assertShape(m.rotate90().rotate90(), 3, 0);
        }
    }

    /** C-008 / C-009: conversions and mapToObj must keep the column count of a 0 x N source. */
    @Test
    void zeroByNShapeSurvivesConversionsAndMapToObj() {
        assertShape(LongMatrix.random(0, 5).toDoubleMatrix(), 0, 5);
        assertShape(LongMatrix.random(0, 5).toIntMatrix(), 0, 5);
        assertShape(LongMatrix.random(0, 5).toFloatMatrix(), 0, 5);
        assertShape(FloatMatrix.random(0, 5).toDoubleMatrix(), 0, 5);
        assertShape(IntMatrix.random(0, 5).toDoubleMatrix(), 0, 5);
        assertShape(ByteMatrix.random(0, 5).toIntMatrix(), 0, 5);
        assertShape(CharMatrix.random(0, 5).toIntMatrix(), 0, 5);

        assertShape(LongMatrix.random(0, 5).mapToObj(v -> "" + v, String.class), 0, 5);
        assertShape(FloatMatrix.random(0, 5).mapToObj(v -> "" + v, String.class), 0, 5);
        assertShape(DoubleMatrix.random(0, 5).mapToObj(v -> "" + v, String.class), 0, 5);
        assertShape(IntMatrix.random(0, 5).mapToObj(v -> "" + v, String.class), 0, 5);
    }

    /** C-010: a 0 x N matrix has N columns, so it must yield N (empty) column streams. */
    @Test
    void columnStreamsOfAZeroRowMatrixYieldOneEmptyStreamPerColumn() {
        assertEquals(3L, CharMatrix.random(0, 3).columnStreams().count());
        assertEquals(3L, BooleanMatrix.random(0, 3).columnStreams().count());
        assertEquals(3L, ByteMatrix.random(0, 3).columnStreams().count());
        assertEquals(3L, ShortMatrix.random(0, 3).columnStreams().count());
        assertEquals(3L, IntMatrix.random(0, 3).columnStreams().count());
        assertEquals(3L, LongMatrix.random(0, 3).columnStreams().count());
        assertEquals(3L, FloatMatrix.random(0, 3).columnStreams().count());
        assertEquals(3L, DoubleMatrix.random(0, 3).columnStreams().count());

        // The mirror image: an N x 0 matrix still yields one empty stream per row.
        assertEquals(3L, CharMatrix.wrap(new char[3][0]).rowStreams().count());
        assertEquals(3L, ByteMatrix.wrap(new byte[3][0]).rowStreams().count());
    }

    /** C-011: every zip result must carry the column count of its 0 x N inputs. */
    @Test
    void zipResultsKeepTheColumnCountOfZeroRowInputs() {
        final IntMatrix ints = IntMatrix.wrap(new int[][] { { 1, 2, 3, 4, 5 } }).copyRows(0, 0);
        assertShape(ints, 0, 5);
        assertEquals(5, Matrices.zipToLong(ints, ints, (p, q) -> (long) p + q).columnCount());
        assertEquals(5, Matrices.zipToDouble(ints, ints, (p, q) -> (double) p + q).columnCount());
        assertEquals(5, Matrices.zip(ints, ints, (p, q) -> p + q).columnCount());
        // The 2-input and 3-input forms must not disagree on the same data.
        assertEquals(5, Matrices.zip(List.of(ints, ints), (p, q) -> p + q).columnCount());
        assertEquals(5, Matrices.zip(List.of(ints, ints, ints), (p, q) -> p + q).columnCount());

        final ByteMatrix bytes = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } }).copyRows(0, 0);
        assertEquals(3, Matrices.zipToInt(bytes, bytes, (p, q) -> p + q).columnCount());
        assertEquals(3, bytes.zipWith(bytes, (p, q) -> (byte) (p + q)).columnCount());

        final Matrix<String> strings = Matrix.wrap(new String[][] { { "a", "b", "c" } }).copyRows(0, 0);
        assertEquals(3, Matrices.zip(List.of(strings, strings, strings), (p, q) -> p + q).columnCount());
    }

    /** C-002: a genuinely 0 x 0 result is the shared empty singleton in every variant. */
    @Test
    void emptyResultsAreTheSharedSingletonInEveryVariant() {
        assertSame(ByteMatrix.empty(), ByteMatrix.unbox(Matrix.empty(Byte.class)));
        assertSame(ShortMatrix.empty(), ShortMatrix.unbox(Matrix.empty(Short.class)));
        assertSame(IntMatrix.empty(), IntMatrix.unbox(Matrix.empty(Integer.class)));
        assertSame(LongMatrix.empty(), LongMatrix.unbox(Matrix.empty(Long.class)));
        assertSame(FloatMatrix.empty(), FloatMatrix.unbox(Matrix.empty(Float.class)));
        assertSame(DoubleMatrix.empty(), DoubleMatrix.unbox(Matrix.empty(Double.class)));
        assertSame(BooleanMatrix.empty(), BooleanMatrix.unbox(Matrix.empty(Boolean.class)));

        assertSame(LongMatrix.empty(), LongMatrix.empty().matrixMultiply(LongMatrix.empty()));
        assertSame(ByteMatrix.empty(), ByteMatrix.empty().matrixMultiply(ByteMatrix.empty()));
        assertSame(IntMatrix.empty(), IntMatrix.empty().copy());
        assertSame(DoubleMatrix.empty(), DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } }).copyRegion(1, 1, 1, 1));

        // Canonicalisation must never swallow a degenerate-but-shaped result.
        assertShape(IntMatrix.random(0, 3).copy(), 0, 3);
        assertShape(IntMatrix.wrap(new int[3][0]).copy(), 3, 0);
    }

    /** C-012: an exact product whose intermediate terms cancel must be accepted, not rejected. */
    @Test
    void matrixMultiplyExactAcceptsCancellingIntermediateTerms() {
        final IntMatrix a = IntMatrix.wrap(new int[][] { { 2_000_000_000, 2_000_000_000, -2_000_000_000 } });
        final IntMatrix b = IntMatrix.wrap(new int[][] { { 1 }, { 1 }, { 1 } });
        assertEquals(2_000_000_000, a.matrixMultiplyExact(b).get(0, 0));

        final IntMatrix c = IntMatrix.wrap(new int[][] { { 100_000, 100_000 } });
        final IntMatrix d = IntMatrix.wrap(new int[][] { { 100_000 }, { -100_000 } });
        assertEquals(0, c.matrixMultiplyExact(d).get(0, 0));

        // A genuinely unrepresentable cell still throws.
        final IntMatrix big = IntMatrix.wrap(new int[][] { { 50_000, 50_000 } });
        final IntMatrix big2 = IntMatrix.wrap(new int[][] { { 50_000 }, { 50_000 } });
        assertThrows(ArithmeticException.class, () -> big.matrixMultiplyExact(big2));

        // Byte and Short already behaved this way; they must keep doing so.
        assertEquals((byte) 0,
                ByteMatrix.wrap(new byte[][] { { 100, 100 } }).matrixMultiplyExact(ByteMatrix.wrap(new byte[][] { { 1 }, { -1 } })).get(0, 0));
        assertEquals((short) 0,
                ShortMatrix.wrap(new short[][] { { 30_000, 30_000 } }).matrixMultiplyExact(ShortMatrix.wrap(new short[][] { { 1 }, { -1 } })).get(0, 0));
    }

    /** C-013 / C-003: the outer array is a private snapshot; the row arrays stay shared. */
    @Test
    void wrapSnapshotsTheOuterArrayButSharesTheRows() {
        final int[] row0 = { 1, 2 };
        final int[] row1 = { 3, 4 };
        final int[][] outer = { row0, row1 };
        final IntMatrix m = IntMatrix.wrap(outer);

        assertNotSame(outer, m.unsafeBackingArray());
        assertSame(row0, m.unsafeBackingArray()[0]);
        assertSame(row1, m.unsafeBackingArray()[1]);

        // A cell write through the caller's row is visible; a row *replacement* is not.
        row0[0] = 99;
        assertEquals(99, m.get(0, 0));
        outer[0] = new int[] { -1, -1 };
        assertEquals(99, m.get(0, 0));

        // Duplicate row identities are rejected by wrap but accepted by copyOf, which clones.
        final int[] shared = { 1, 2 };
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(shared, shared));
        final IntMatrix copied = IntMatrix.copyOf(shared, shared);
        assertNotSame(copied.rowView(0), copied.rowView(1));
    }

    /** C-029: equals() compares the column count, so hashCode() must too for zero-row shapes. */
    @Test
    void zeroRowMatricesOfDifferentWidthsDoNotCollideInHash() {
        final IntMatrix zeroByZero = IntMatrix.empty();
        final IntMatrix zeroByThree = IntMatrix.random(0, 3);
        final IntMatrix zeroByFive = IntMatrix.random(0, 5);

        assertNotEquals(zeroByThree, zeroByFive);
        assertNotEquals(zeroByThree, zeroByZero);
        assertNotEquals(zeroByThree.hashCode(), zeroByFive.hashCode());
        assertNotEquals(zeroByThree.hashCode(), zeroByZero.hashCode());

        // The documented hash of the empty matrix is unchanged.
        assertEquals(1, zeroByZero.hashCode());
        assertEquals(1, LongMatrix.empty().hashCode());
        assertEquals(1, DoubleMatrix.empty().hashCode());

        // Equal matrices still agree, in every variant.
        assertEquals(IntMatrix.random(0, 4).hashCode(), IntMatrix.random(0, 4).hashCode());
        assertEquals(CharMatrix.random(0, 4).hashCode(), CharMatrix.random(0, 4).hashCode());
        assertEquals(Matrix.wrap(new String[2][3]).hashCode(), Matrix.wrap(new String[2][3]).hashCode());

        // A genuine N x 0 shape keeps hashing off its rows.
        assertEquals(IntMatrix.wrap(new int[3][0]).hashCode(), IntMatrix.wrap(new int[3][0]).hashCode());
        assertNotEquals(IntMatrix.wrap(new int[3][0]).hashCode(), zeroByZero.hashCode());
    }

    /** S-006: a collection zip does elementCount x size work, so the parallel decision must say so. */
    @Test
    void collectionZipEstimatesWorkAcrossEveryInput() {
        final int size = 8;
        final int side = 32;
        final List<IntMatrix> inputs = new ArrayList<>();
        for (int k = 0; k < size; k++) {
            inputs.add(IntMatrix.random(side, side));
        }
        final IntMatrix first = inputs.get(0);

        // One input on its own is below the threshold...
        assertEquals((long) side * side * size, first.elementCount() * size);
        assertFalse(Matrices.shouldRunInParallel(first));
        // ...but the work actually performed spans all `size` inputs and does reach it.
        assertTrue(Matrices.shouldRunInParallel(first, first.elementCount() * size));

        final Set<Thread> autoThreads = ConcurrentHashMap.newKeySet();
        final AtomicReference<IntMatrix> sum = new AtomicReference<>();
        Matrices.runWithParallelMode(ParallelMode.AUTO, () -> sum.set(Matrices.zip(inputs, (x, y) -> {
            autoThreads.add(Thread.currentThread());
            return x + y;
        })));
        final IntMatrix summed = sum.get();

        assertEquals(side, summed.rowCount());
        assertEquals(side, summed.columnCount());
        int expected = 0;
        for (final IntMatrix input : inputs) {
            expected += input.get(0, 0);
        }
        assertEquals(expected, summed.get(0, 0));

        // Only compare against what this machine can actually do: if a forced-parallel control run
        // uses more than one thread, the AUTO run must now do so too.
        final Set<Thread> forcedThreads = ConcurrentHashMap.newKeySet();
        Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> Matrices.zip(inputs, (x, y) -> {
            forcedThreads.add(Thread.currentThread());
            return x + y;
        }));
        if (forcedThreads.size() > 1) {
            assertTrue(autoThreads.size() > 1, "AUTO should parallelize once the combined work reaches the threshold");
        }
    }

    /** M-003: a primitive Class token must not leave elementType disagreeing with the backing rows. */
    @Test
    void mapToObjNormalisesAPrimitiveElementTypeToken() {
        // int.class has static type Class<Integer>, so this compiles; the backing array really holds
        // Integers, and storing the unwrapped token made every copy-producing method fail.
        final Matrix<Integer> viaPrimitiveToken = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } }).mapToObj(i -> i, int.class);
        assertEquals(Integer.class, viaPrimitiveToken.elementType());
        assertShape(viaPrimitiveToken.copy(), 2, 2);
        assertShape(viaPrimitiveToken.transpose(), 2, 2);
        assertShape(viaPrimitiveToken.rotate90(), 2, 2);
        assertShape(viaPrimitiveToken.rotate180(), 2, 2);
        assertShape(viaPrimitiveToken.resize(3, 3), 3, 3);
        assertEquals(1, viaPrimitiveToken.copy().get(0, 0));

        // Every variant takes the same path.
        assertEquals(Long.class, LongMatrix.wrap(new long[][] { { 1L } }).mapToObj(v -> v, long.class).elementType());
        assertEquals(Double.class, DoubleMatrix.wrap(new double[][] { { 1.0 } }).mapToObj(v -> v, double.class).elementType());
        assertEquals(Boolean.class, BooleanMatrix.wrap(new boolean[][] { { true } }).mapToObj(v -> v, boolean.class).elementType());
        assertEquals(Character.class, CharMatrix.wrap(new char[][] { { 'a' } }).mapToObj(v -> v, char.class).elementType());
        assertEquals(Byte.class, ByteMatrix.wrap(new byte[][] { { 1 } }).mapToObj(v -> v, byte.class).elementType());
        assertEquals(Short.class, ShortMatrix.wrap(new short[][] { { 1 } }).mapToObj(v -> v, short.class).elementType());
        assertEquals(Float.class, FloatMatrix.wrap(new float[][] { { 1f } }).mapToObj(v -> v, float.class).elementType());

        // The wrapper token still behaves the same.
        assertEquals(Integer.class, IntMatrix.wrap(new int[][] { { 1 } }).mapToObj(i -> i, Integer.class).elementType());
    }

    /** M-001 / NEW-A: every transform must yield rows writable with any legal element of T. */
    @Test
    void transformsProduceUniformlyWritableRowsForCovariantStorage() {
        final Number[][] backing = new Number[][] { new Long[] { 1L, 2L }, new Long[] { 3L, 4L } };
        final Matrix<Number> m = Matrix.wrap(backing);
        assertEquals(Number.class, m.elementType());

        // rotate180 used to clone the source row and so kept Long[], unlike every sibling transform.
        m.copy().set(0, 0, 2.5d);
        m.transpose().set(0, 0, 2.5d);
        m.rotate90().set(0, 0, 2.5d);
        m.rotate180().set(0, 0, 2.5d);
        m.rotate270().set(0, 0, 2.5d);
        m.flipHorizontally().set(0, 0, 2.5d);
        m.flipVertically().set(0, 0, 2.5d);
        assertEquals(Number[].class, m.rotate180().unsafeBackingArray()[0].getClass());
        // and it still reverses in both directions
        assertArrayEquals(new Number[] { 4L, 3L }, m.rotate180().rowCopy(0));

        // The n-ary stack used to disagree with the pairwise one.
        final Matrix<Number> pairwise = m.stackVertically(m.copy());
        final Matrix<Number> nary = Matrices.stackVertically(List.of(m, m.copy()));
        pairwise.set(0, 0, 2.5d);
        nary.set(0, 0, 2.5d);
        assertEquals(Number[].class, nary.unsafeBackingArray()[0].getClass());
        assertShape(nary, 4, 2);
    }

    /** M-005: toTransposedDataset must reject the shape it cannot represent, as toDataset does. */
    @Test
    void transposedDatasetRejectsShapesItCannotRepresent() {
        final Matrix<String> zeroByThree = Matrix.empty(String.class, 3);
        assertShape(zeroByThree, 0, 3);
        assertThrows(IllegalArgumentException.class, () -> zeroByThree.toTransposedDataset(List.of()));

        // The mirror case was already guarded, and both 0 x 0 forms still work.
        assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(new String[3][0]).toDataset(List.of()));
        assertEquals(0, Matrix.empty(String.class).toTransposedDataset(List.of()).size());
        assertEquals(0, Matrix.empty(String.class).toDataset(List.of()).size());
    }

    /** M-002: Matrix<T>'s in-place mutators follow the same parallel policy as the 8 primitive variants. */
    @Test
    void genericMatrixInPlaceMutatorsHonourTheParallelPolicy() {
        final int side = 40;

        // FORCE_OFF (the default) keeps every one of them on the calling thread.
        final Matrix<Integer> sequential = Matrix.wrap(Integer.class, new Integer[side][side]);
        sequential.updateAll(v -> 0);
        final Set<Thread> offThreads = ConcurrentHashMap.newKeySet();
        Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> sequential.updateAll((i, j) -> {
            offThreads.add(Thread.currentThread());
            return i * side + j;
        }));
        assertEquals(1, offThreads.size());
        assertEquals(Thread.currentThread(), offThreads.iterator().next());

        // FORCE_ON produces the same values, and may now use more than one thread.
        final Matrix<Integer> parallel = Matrix.wrap(Integer.class, new Integer[side][side]);
        final Set<Thread> onThreads = ConcurrentHashMap.newKeySet();
        Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> parallel.updateAll((i, j) -> {
            onThreads.add(Thread.currentThread());
            return i * side + j;
        }));
        assertEquals(sequential, parallel);
        assertEquals(side * side - 1, parallel.get(side - 1, side - 1));

        // All four mutators remain correct under forced parallelism.
        Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> {
            parallel.updateAll(v -> v + 1);
            parallel.replaceIf(v -> v == 1, -1);
            parallel.replaceIf((i, j) -> i == 0 && j == 1, -2);
        });
        assertEquals(-1, parallel.get(0, 0));
        assertEquals(-2, parallel.get(0, 1));
        assertEquals(side * side, parallel.get(side - 1, side - 1));

        // If the runtime really did run the callback on more than one thread, the sequential run
        // must still have been single-threaded -- that is the contract this test protects.
        if (onThreads.size() > 1) {
            assertEquals(1, offThreads.size());
        }
    }

    /** Canonicalisation is uniform: 0 x 0 results are the singleton, degenerate shapes are preserved. */
    @Test
    void conversionsCanonicaliseOnlyGenuinelyEmptyResults() {
        // Every toXxxMatrix conversion, from every source type, agrees.
        assertSame(IntMatrix.empty(), ByteMatrix.empty().toIntMatrix());
        assertSame(IntMatrix.empty(), CharMatrix.empty().toIntMatrix());
        assertSame(IntMatrix.empty(), ShortMatrix.empty().toIntMatrix());
        assertSame(IntMatrix.empty(), LongMatrix.empty().toIntMatrix());
        assertSame(IntMatrix.empty(), FloatMatrix.empty().toIntMatrix());
        assertSame(IntMatrix.empty(), DoubleMatrix.empty().toIntMatrix());
        assertSame(LongMatrix.empty(), ByteMatrix.empty().toLongMatrix());
        assertSame(LongMatrix.empty(), IntMatrix.empty().toLongMatrix());
        assertSame(FloatMatrix.empty(), IntMatrix.empty().toFloatMatrix());
        assertSame(DoubleMatrix.empty(), IntMatrix.empty().toDoubleMatrix());
        assertSame(DoubleMatrix.empty(), LongMatrix.empty().toDoubleMatrix());
        assertSame(DoubleMatrix.empty(), FloatMatrix.empty().toDoubleMatrix());
        assertSame(DoubleMatrix.empty(), ShortMatrix.empty().toDoubleMatrix());

        // ...but a degenerate-yet-shaped source keeps its shape and is NOT the singleton.
        assertShape(IntMatrix.random(0, 4).toLongMatrix(), 0, 4);
        assertShape(IntMatrix.random(0, 4).toDoubleMatrix(), 0, 4);
        assertShape(ByteMatrix.wrap(new byte[3][0]).toIntMatrix(), 3, 0);
        assertShape(ShortMatrix.wrap(new short[3][0]).toDoubleMatrix(), 3, 0);
        assertNotSame(IntMatrix.empty(), ByteMatrix.random(0, 4).toIntMatrix());
    }

    /** C-031: an array-typed element must survive element-type normalisation (Matrix<int[]>). */
    @Test
    void arrayTypedElementsAreNotRewrittenByElementTypeNormalisation() {
        // ClassUtil.wrap maps int[] to Integer[] as readily as it maps int to Integer, but the backing
        // rows of a Matrix<int[]> really are int[][] -- wrapping would make elementType disagree with them.
        final Matrix<int[]> m = Matrix.wrap(new int[][][] { { { 1, 2 }, { 3 } }, { { 4 }, { 5, 6 } } });
        assertEquals(int[].class, m.elementType());
        assertShape(m, 2, 2);

        assertArrayEquals(new int[] { 1, 2 }, m.copy().get(0, 0));
        assertArrayEquals(new int[] { 5, 6 }, m.transpose().get(1, 1));
        assertArrayEquals(new int[] { 5, 6 }, m.rotate180().get(0, 0));
        assertArrayEquals(new int[] { 3 }, m.rotate90().get(1, 1));
        assertArrayEquals(new int[] { 1, 2 }, m.rotate90().get(0, 1));
        assertEquals(int[].class, m.copy().elementType());
        assertEquals(int[].class, m.resize(3, 3).elementType());

        // The explicit-token factory agrees.
        final Matrix<int[]> viaToken = Matrix.wrap(int[].class, new int[][][] { { { 7 } } });
        assertEquals(int[].class, viaToken.elementType());
        assertArrayEquals(new int[] { 7 }, viaToken.copy().get(0, 0));

        // A primitive SCALAR token is still normalised to its wrapper.
        assertEquals(Integer.class, IntMatrix.wrap(new int[][] { { 1 } }).mapToObj(i -> i, int.class).elementType());
    }

    /** D2: an array element type must survive Matrices.newMatrixArray, not just the Matrix constructor. */
    @Test
    void arrayElementTypesSurviveEveryAllocationPath() {
        // newMatrixArray is the allocation path that copyOf / map / zipWith / mapToObj all go through;
        // wrapping int[] to Integer[] there produced storage that no longer matched the element type.
        final Object[][] raw = Matrices.newMatrixArray(2, 2, int[].class);
        assertEquals(int[][].class, raw.getClass().getComponentType());

        assertShape(IntMatrix.empty().mapToObj(x -> new int[] { x }, int[].class), 0, 0);
        assertShape(IntMatrix.random(0, 3).mapToObj(x -> new int[] { x }, int[].class), 0, 3);
        assertShape(LongMatrix.empty().mapToObj(x -> new int[] { (int) x }, int[].class), 0, 0);
        assertShape(Matrix.empty(String.class).map(v -> new int[0], int[].class), 0, 0);

        final Matrix<int[]> mapped = Matrix.wrap(new String[][] { { "a", "bb" } }).map(v -> new int[] { v.length() }, int[].class);
        assertEquals(int[].class, mapped.elementType());
        assertArrayEquals(new int[] { 2 }, mapped.get(0, 1));

        final Matrix<int[]> copied = Matrix.copyOf(int[].class, new int[][][] { { { 1 }, { 2 } } });
        assertEquals(int[].class, copied.elementType());
        assertArrayEquals(new int[] { 2 }, copied.get(0, 1));

        final Matrix<String> one = Matrix.wrap(new String[][] { { "a" } });
        assertArrayEquals(new int[] { 2 }, one.zipWith(one, (p, q) -> new int[] { p.length() + q.length() }, int[].class).get(0, 0));
        assertEquals(int[].class, Matrix.ofMainDiagonal(int[].class, new int[][] { { 1 }, { 2 } }).elementType());

        // A primitive SCALAR token must still be normalised to its wrapper.
        assertEquals(Integer.class, IntMatrix.wrap(new int[][] { { 1 } }).mapToObj(i -> i, int.class).elementType());
    }

    /** D4: "a 0 x 0 result is the shared singleton" must hold on the n-ary and widening paths too. */
    @Test
    void everyEmptyProducingPathReturnsTheSharedSingleton() {
        // These two bypassed the per-class canonicaliser, so the strengthened class-level claim was false.
        assertSame(IntMatrix.empty(), Matrices.stackVertically(List.of(IntMatrix.empty(), IntMatrix.empty())));
        assertSame(IntMatrix.empty(), Matrices.stackHorizontally(List.of(IntMatrix.empty(), IntMatrix.empty())));
        assertSame(DoubleMatrix.empty(), Matrices.stackVertically(List.of(DoubleMatrix.empty(), DoubleMatrix.empty())));
        assertSame(IntMatrix.empty(), IntMatrix.empty().stackVertically(IntMatrix.empty()));

        assertSame(LongMatrix.empty(), IntMatrix.empty().matrixMultiplyWidened(IntMatrix.empty()));
        assertSame(LongMatrix.empty(), IntMatrix.empty().matrixMultiplyWidenedExact(IntMatrix.empty()));
        assertSame(LongMatrix.empty(), ByteMatrix.empty().matrixMultiplyWidened(ByteMatrix.empty()));
        assertSame(LongMatrix.empty(), ShortMatrix.empty().matrixMultiplyWidened(ShortMatrix.empty()));

        // ...and canonicalisation must not swallow a degenerate-but-shaped result.
        assertShape(Matrices.stackVertically(List.of(IntMatrix.random(0, 3), IntMatrix.random(0, 3))), 0, 3);
        assertShape(Matrices.stackHorizontally(List.of(IntMatrix.wrap(new int[3][0]), IntMatrix.wrap(new int[3][0]))), 3, 0);

        // The zero-row hash must separate shapes that equals() separates, including 0 x 1 vs 1 x 0.
        assertNotEquals(IntMatrix.random(0, 1).hashCode(), IntMatrix.wrap(new int[1][0]).hashCode());
        assertEquals(1, IntMatrix.empty().hashCode());
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
