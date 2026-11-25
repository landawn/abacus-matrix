package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.stream.IntIteratorEx;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * Regression tests for the deep line-by-line review fixes and optimizations across all matrix types.
 */
class DeepReviewFixesRegressionTest extends MatrixTestSupport {

    @SuppressWarnings("unchecked")
    private static <T> T under(final ParallelMode mode, final Supplier<T> supplier) {
        final Object[] result = new Object[1];
        Matrices.runWithParallelMode(mode, () -> result[0] = supplier.get());
        return (T) result[0];
    }

    private static boolean retains(final Object root, final Object target) throws IllegalAccessException {
        final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        final ArrayDeque<Object> pending = new ArrayDeque<>();
        pending.add(root);

        while (!pending.isEmpty()) {
            final Object value = pending.removeFirst();

            if (value == target) {
                return true;
            }

            if (!seen.add(value) || !value.getClass().getName().startsWith("com.landawn.")) {
                continue;
            }

            for (Class<?> type = value.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                for (final Field field : type.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive() && field.trySetAccessible()) {
                        final Object child = field.get(value);

                        if (child != null) {
                            pending.add(child);
                        }
                    }
                }
            }
        }

        return false;
    }

    // ---- Matrices.newMatrixArray freshness & dimension limits ----

    @Test
    void newMatrixArrayAlwaysAllocatesFreshZeroRowOuterArrayAndDistinctZeroColRows() {
        final String[][] zeroByZeroA = Matrices.newMatrixArray(0, 0, String.class);
        final String[][] zeroByZeroB = Matrices.newMatrixArray(0, 0, String.class);
        assertNotSame(zeroByZeroA, zeroByZeroB);
        assertEquals(0, zeroByZeroA.length);

        final Integer[][] zeroByHugeA = Matrices.newMatrixArray(0, Integer.MAX_VALUE, int.class);
        final Integer[][] zeroByHugeB = Matrices.newMatrixArray(0, Integer.MAX_VALUE, int.class);
        assertNotSame(zeroByHugeA, zeroByHugeB);
        assertEquals(Integer[][].class, zeroByHugeA.getClass());

        final String[][] threeByZero = Matrices.newMatrixArray(3, 0, String.class);
        assertEquals(3, threeByZero.length);
        assertNotSame(threeByZero[0], threeByZero[1]);
        assertNotSame(threeByZero[1], threeByZero[2]);
        assertNotSame(threeByZero[0], threeByZero[2]);

        final Class<?> excessiveArrayType = Array.newInstance(Object.class, new int[254]).getClass();
        assertThrows(IllegalArgumentException.class, () -> Matrices.newMatrixArray(0, 0, void.class));
        assertThrows(IllegalArgumentException.class, () -> Matrices.newMatrixArray(0, 0, excessiveArrayType));
    }

    // ---- Matrices.resolveCommonType multiplicity invariance ----

    interface NearForA {
        void near();
    }

    interface NearForB {
        void other();
    }

    static class TypeA1 implements NearForB {
        @Override
        public void other() {
        }
    }

    static class TypeA2 extends TypeA1 {
    }

    /** NearForA at distance 1, NearForB at distance 3 (via TypeA2, TypeA1). */
    static class TypeA extends TypeA2 implements NearForA {
        @Override
        public void near() {
        }
    }

    static class TypeB2 implements NearForA {
        @Override
        public void near() {
        }
    }

    static class TypeB1 extends TypeB2 implements NearForB {
        @Override
        public void other() {
        }
    }

    /** NearForA at distance 3 (via TypeB1, TypeB2), NearForB at distance 2 (via TypeB1). */
    static class TypeB extends TypeB1 {
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
    void resolveCommonElementTypeIsInvariantToDuplicateInputTypes() {
        // The two inputs favour different candidates. Summed over the DISTINCT types {TypeA, TypeB}: NearForA = 1 + 3 = 4,
        // NearForB = 3 + 2 = 5, so NearForA wins. Summing over occurrences instead (the old behaviour) lets TypeB x 3 flip it:
        // NearForA = 1 + 9 = 10, NearForB = 3 + 6 = 9 -> NearForB. Object is farther for both (3 + 3) and never competes.
        final Matrix a = Matrix.empty(TypeA.class);
        final Matrix b = Matrix.empty(TypeB.class);

        assertEquals(NearForA.class, Matrices.resolveCommonElementType(new Matrix[] { a, b }));
        assertEquals(NearForA.class, Matrices.resolveCommonElementType(new Matrix[] { b, a }));
        assertEquals(NearForA.class, Matrices.resolveCommonElementType(new Matrix[] { a, a, a, b }));
        assertEquals(NearForA.class, Matrices.resolveCommonElementType(new Matrix[] { a, b, b, b }));
        assertEquals(NearForA.class, Matrices.resolveCommonElementType(new Matrix[] { b, b, a, b, b }));
    }

    // ---- Single-snapshot Collection TOCTOU protection in Matrices ----

    private static final class SnapshotMutatingCollection<E> extends AbstractCollection<E> {
        private final List<E> advertised;
        private final List<E> snapshotData;
        final AtomicInteger toArrayCalls = new AtomicInteger();

        SnapshotMutatingCollection(final List<E> advertised, final List<E> snapshotData) {
            this.advertised = advertised;
            this.snapshotData = snapshotData;
        }

        @Override
        public Iterator<E> iterator() {
            return advertised.iterator();
        }

        @Override
        public int size() {
            return advertised.size();
        }

        @Override
        public <T> T[] toArray(final T[] a) {
            toArrayCalls.incrementAndGet();
            return snapshotData.toArray(a);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void collectionZipsAndStacksSnapshotOnceAndHandleSizeDriftWithoutNpeOrAioobe() {
        final IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
        final IntMatrix m2 = IntMatrix.wrap(new int[][] { { 10, 20 }, { 30, 40 } });
        final IntMatrix m3 = IntMatrix.wrap(new int[][] { { 100, 200 }, { 300, 400 } });

        // Concurrent shrink to empty between isEmpty() and toArray(): must throw IllegalArgumentException.
        final SnapshotMutatingCollection<IntMatrix> shrunkToEmpty = new SnapshotMutatingCollection<>(List.of(m1, m2), List.of());
        assertThrows(IllegalArgumentException.class, () -> Matrices.zip(shrunkToEmpty, Integer::sum));
        assertEquals(1, shrunkToEmpty.toArrayCalls.get());

        final SnapshotMutatingCollection<IntMatrix> stackShrunkToEmpty = new SnapshotMutatingCollection<>(List.of(m1, m2), List.of());
        assertThrows(IllegalArgumentException.class, () -> Matrices.stackVertically(stackShrunkToEmpty));
        assertEquals(1, stackShrunkToEmpty.toArrayCalls.get());

        // Concurrent shrink from 3 to 2: must combine only the 2 snapshotted matrices (no trailing null NPE).
        final SnapshotMutatingCollection<IntMatrix> shrunk = new SnapshotMutatingCollection<>(List.of(m1, m2, m3), List.of(m1, m2));
        assertEquals(IntMatrix.wrap(new int[][] { { 11, 22 }, { 33, 44 } }), Matrices.zip(shrunk, Integer::sum));
        assertEquals(1, shrunk.toArrayCalls.get());

        // Concurrent grow from 2 to 3: must combine all 3 snapshotted matrices (not truncate at stale size 2).
        final SnapshotMutatingCollection<IntMatrix> grown = new SnapshotMutatingCollection<>(List.of(m1, m2), List.of(m1, m2, m3));
        assertEquals(IntMatrix.wrap(new int[][] { { 111, 222 }, { 333, 444 } }), Matrices.zip(grown, Integer::sum));
        assertEquals(1, grown.toArrayCalls.get());

        // Deprecated Matrix<T> collection zips must snapshot coll only once.
        final Matrix<String> s1 = Matrix.wrap(String.class, new String[][] { { "a", "b" } });
        final Matrix<String> s2 = Matrix.wrap(String.class, new String[][] { { "1", "2" } });
        final SnapshotMutatingCollection<Matrix<String>> deprecatedBinary = new SnapshotMutatingCollection<>(List.of(s1, s2), List.of(s1, s2));
        assertEquals(Matrix.wrap(String.class, new String[][] { { "a1", "b2" } }), Matrices.zip(deprecatedBinary, (x, y) -> x + y));
        assertEquals(1, deprecatedBinary.toArrayCalls.get());

        final SnapshotMutatingCollection<Matrix<String>> deprecatedArray = new SnapshotMutatingCollection<>(List.of(s1, s2), List.of(s1, s2));
        assertEquals(Matrix.wrap(String.class, new String[][] { { "a:1", "b:2" } }),
                Matrices.zip(deprecatedArray, arr -> arr[0] + ":" + arr[1], true, String.class));
        assertEquals(1, deprecatedArray.toArrayCalls.get());

        // Single-element Matrix<T> binary zip fast path preserves explicit elementType and returns an independent copy.
        final Matrix<CharSequence> singleExplicit = Matrices.zip(List.of(Matrix.copyOf(CharSequence.class, new CharSequence[][] { { "x", "y" } })), (x, y) -> x,
                CharSequence.class);
        assertEquals(CharSequence.class, singleExplicit.elementType());
        assertArrayEquals(new CharSequence[] { "x", "y" }, singleExplicit.rowCopy(0));
    }

    // ---- Matrix.toDataset / toTransposedDataset TOCTOU & column isolation ----

    @Test
    void toDatasetAndToTransposedDatasetRejectConcurrentColumnNameSizeMismatchAndIsolateRows() {
        final Matrix<String> m = Matrix.wrap(String.class, new String[][] { { "a", "b", "c" }, { "d", null, "f" } });

        final Collection<String> driftingForDataset = new AbstractCollection<>() {
            @Override
            public int size() {
                return 3; // matches columnCount
            }

            @Override
            public Iterator<String> iterator() {
                return List.of("c0", "c1").iterator(); // only 2 elements on copy
            }
        };
        assertThrows(IllegalArgumentException.class, () -> m.toDataset(driftingForDataset));

        final Collection<String> driftingForTransposed = new AbstractCollection<>() {
            @Override
            public int size() {
                return 2; // matches rowCount
            }

            @Override
            public Iterator<String> iterator() {
                return List.of("r0", "r1", "r2").iterator(); // 3 elements on copy
            }
        };
        assertThrows(IllegalArgumentException.class, () -> m.toTransposedDataset(driftingForTransposed));

        final Dataset ds = m.toDataset(List.of("c0", "c1", "c2"));
        assertEquals(2, ds.size());
        assertEquals(List.of("a", "d"), ds.getColumn("c0"));
        assertEquals(Arrays.asList("b", null), ds.getColumn("c1"));

        final Dataset transposed = m.toTransposedDataset(List.of("r0", "r1"));
        assertEquals(3, transposed.size());
        assertEquals(List.of("a", "b", "c"), transposed.getColumn("r0"));
        assertEquals(Arrays.asList("d", null, "f"), transposed.getColumn("r1"));

        // Mutating matrix after toTransposedDataset must not mutate dataset columns.
        m.set(0, 0, "mutated");
        assertEquals("a", transposed.getColumn("r0").get(0));
    }

    // ---- AbstractMatrix small-matrix duplicate scan & RowIdentityIndex guards ----

    @Test
    void duplicateRowValidationAcrossSmallPairwiseAndHashIndexThresholdsAndIndexGuards() {
        for (final int rowCount : new int[] { 2, 3, 15, 16, 17, 18, 64 }) {
            final int[][] rows = new int[rowCount][];

            for (int i = 0; i < rowCount; i++) {
                rows[i] = new int[] { i };
            }

            assertEquals(rowCount, IntMatrix.wrap(rows).rowCount());

            final int firstIndex = rowCount == 2 ? 0 : 1;
            final int[][] dup = rows.clone();
            dup[rowCount - 1] = dup[firstIndex];
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(dup));
            assertEquals("Rows must have independent storage: row " + (rowCount - 1) + " is the same array as row " + firstIndex, ex.getMessage());
        }

        final Object[] sample = { new int[0], new int[0] };
        assertThrows(IllegalArgumentException.class, () -> new AbstractMatrix.RowIdentityIndex(sample, 2, -2));
        assertThrows(IllegalArgumentException.class, () -> new AbstractMatrix.RowIdentityIndex(sample, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> new AbstractMatrix.RowIdentityIndex(sample, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> new AbstractMatrix.RowIdentityIndex(sample, 2, 6));
    }

    // ---- AbstractMatrix point streams ObjIteratorEx contract & non-retention ----

    @Test
    void pointStreamsSupportAdvanceCountExhaustionAndDoNotRetainMatrix() throws Exception {
        final IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

        try (Stream<Point> rowMajor = matrix.rowMajorPoints(1, 3);
             Stream<Point> colMajor = matrix.columnMajorPoints(1, 3)) {
            assertFalse(retains(rowMajor, matrix));
            assertFalse(retains(colMajor, matrix));

            final ObjIteratorEx<Point> rIt = (ObjIteratorEx<Point>) rowMajor.iterator();
            rIt.advance(0);
            rIt.advance(-5);
            assertEquals(Point.of(1, 0), rIt.next());
            rIt.advance(2); // skip (1,1), (1,2)
            assertEquals(Point.of(2, 0), rIt.next());
            assertEquals(2L, rIt.count());
            assertFalse(rIt.hasNext());
            assertThrows(NoSuchElementException.class, rIt::next);

            final ObjIteratorEx<Point> cIt = (ObjIteratorEx<Point>) colMajor.iterator();
            assertEquals(Point.of(0, 1), cIt.next());
            cIt.advance(3); // skip (1,1), (2,1), (0,2)
            assertEquals(Point.of(1, 2), cIt.next());
            cIt.advance(Long.MAX_VALUE);
            assertFalse(cIt.hasNext());
            assertEquals(0L, cIt.count());
            assertThrows(NoSuchElementException.class, cIt::next);
        }

        assertEquals(0L, matrix.rowMajorPoints(1, 1).count());
        assertEquals(0L, matrix.columnMajorPoints(2, 2).count());
        assertEquals(3L, IntMatrix.wrap(new int[3][0]).rowPoints().count());
        assertEquals(0L, IntMatrix.wrap(new int[3][0]).rowPoints().flatMap(s -> s).count());
        assertEquals(4L, IntMatrix.empty().resize(0, 4).columnPoints().count());
        assertEquals(0L, IntMatrix.empty().resize(0, 4).columnPoints().flatMap(s -> s).count());
    }

    // ---- Matrices.mapIndices & mapIndicesToInt sequential cursor + advance ----

    @Test
    void mapIndicesAndMapIndicesToIntTrackSequentialRowColAndAdvanceAcrossRowWraps() {
        final ObjIteratorEx<String> objIt = (ObjIteratorEx<String>) Matrices.mapIndices(2, 5, 1, 4, (r, c) -> r + ":" + c, false).iterator();
        objIt.advance(0);
        objIt.advance(-1);
        assertEquals("2:1", objIt.next());
        objIt.advance(3); // skips 2:2, 2:3, 3:1 -> lands on 3:2
        assertEquals("3:2", objIt.next());
        assertEquals("3:3", objIt.next());
        assertEquals("4:1", objIt.next());
        assertEquals(2L, objIt.count());
        assertFalse(objIt.hasNext());
        assertThrows(NoSuchElementException.class, objIt::next);

        final IntIteratorEx intIt = (IntIteratorEx) Matrices.mapIndicesToInt(1, 4, 2, 5, (r, c) -> r * 10 + c, false).iterator();
        assertEquals(12, intIt.nextInt());
        intIt.advance(4); // skips 13, 14, 22, 23 -> lands on 24
        assertEquals(24, intIt.nextInt());
        assertArrayEquals(new int[] { 32, 33, 34 }, intIt.toArray());
        assertFalse(intIt.hasNext());
        assertThrows(NoSuchElementException.class, intIt::nextInt);
    }

    // ---- Matrices.forEachBlockInParallel & matrixMultiplyInParallel guards ----

    @Test
    void forEachBlockInParallelCoversSubregionsExactlyOnceAndPropagatesCheckedExceptions() throws Exception {
        for (final int elementBytes : new int[] { 1, 2, 4, 8 }) {
            final int minBandWidth = Matrices.MATRIX_MULTIPLY_MIN_BAND_BYTES / elementBytes;
            final int fromRow = 3;
            final int toRow = 7;
            final int fromCol = 5;
            final int toCol = 310;
            final int rows = toRow - fromRow;
            final int cols = toCol - fromCol;
            final AtomicIntegerArray visits = new AtomicIntegerArray(rows * cols);

            Matrices.forEachBlockInParallel(fromRow, toRow, fromCol, toCol, elementBytes, (r0, r1, c0, c1) -> {
                assertTrue(r0 >= fromRow && r1 <= toRow && r0 < r1);
                assertTrue(c0 >= fromCol && c1 <= toCol && c0 < c1);
                assertTrue((c1 - c0) >= Math.min(minBandWidth, cols));

                for (int i = r0; i < r1; i++) {
                    for (int j = c0; j < c1; j++) {
                        visits.incrementAndGet((i - fromRow) * cols + (j - fromCol));
                    }
                }
            });

            for (int idx = 0; idx < rows * cols; idx++) {
                assertEquals(1, visits.get(idx), "elementBytes=" + elementBytes + " idx=" + idx);
            }
        }

        final IOException failure = new IOException("boom");
        assertSame(failure, assertThrows(IOException.class, () -> Matrices.forEachBlockInParallel(0, 4, 0, 200, Integer.BYTES, (r0, r1, c0, c1) -> {
            if (r0 <= 2 && 2 < r1) {
                throw failure;
            }
        })));

        assertThrows(IllegalArgumentException.class, () -> Matrices.matrixMultiplyInParallel(-1, 1, 4, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Matrices.matrixMultiplyInParallel(1, -1, 4, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Matrices.matrixMultiplyInParallel(1, 1, 0, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Matrices.matrixMultiplyInParallel(1, 1, 4, null));
    }

    /**
     * {@code forEachBlockInParallel} must hand out blocks of at least {@code MIN_PARALLEL_BLOCK_WORK} work (cells x cellCost), about
     * {@code MATRIX_MULTIPLY_TARGET_TASK_COUNT} of them: one task per row made a {@code 100000 x 3} map/add/updateAll 4-40x slower than
     * sequential, and rounding the bands per row UP made 64-core runs hand out 1,020-cell bands of a {@code 2 x 100000} region. Every
     * assertion except the exact counts (given only where they cannot depend on the core count) holds for any processor count, so
     * this test also runs meaningfully under {@code -XX:ActiveProcessorCount=64}.
     */
    @Test
    void forEachBlockInParallelKeepsEveryBlockAboveTheMinimumWorkOnAnyCoreCount() throws Exception {
        final int target = Matrices.MATRIX_MULTIPLY_TARGET_TASK_COUNT;
        final int minWork = Matrices.MIN_PARALLEL_BLOCK_WORK;

        // { rows, cols, elementBytes, cellCost, exact task count, or -1 where it depends on the core count }
        final int[][] cases = { { 100_000, 3, 8, 1, -1 }, // tall and narrow: full-width row groups
                { 2_000, 2_000, 4, 1, -1 }, // square: row groups
                { 2, 100_000, 8, 1, -1 }, // few wide rows: column bands (the 64-core 1,020-cell case)
                { 2, 20_000, 8, 1, 38 }, // 39 tasks wanted; 19 bands per row keep each band >= 1,024 cells
                { 5, 1_000, 8, 1, 2 }, // 4 tasks wanted, but one-row groups would be 1,000 cells: two groups
                { 4, 200, 4, 1, 2 }, // small: the two-task floor (two row groups)
                { 1, 300, 1, 1, 2 }, // one row: two 150-byte bands (>= the 128-byte floor)
                { 1, 100, 1, 1, 1 }, // one row narrower than two 128-byte bands: a single block
                { 1, 1, 8, 1, 1 }, // a single cell runs on the calling thread
                { 1, 64, 1, 128, 8 }, // a 128-matrix byte fold: 8,192 work in 8 bands of 8 cells
                { 16, 64, 1, 128, -1 }, // a taller fold: bands sized by work, not by output cells
                { 3, 7, 8, Integer.MAX_VALUE, 21 }, // a huge per-cell cost: one-cell bands, no int overflow in the sizing
                { 3, 800, 8, 1, 2 }, // row groups would be 800 + 1600: two full-height 3 x 400 bands instead
                { 3, 683, 8, 1, 2 }, // no two rectangles reach 1,024: the floor's most even cut is 3 x 341 + 3 x 342
                { 7, 300, 8, 1, 2 }, // 2,100 work: two 3- and 4-row groups would be 900 + 1200; two 7 x 150 bands reach it
                { 1, 84, 1, 3, 2 } }; // cost 3: bands may be 128 / 1 / 3 = 42 columns (rounded down), so two 42-byte bands

        for (final int[] c : cases) {
            final int rows = c[0];
            final int cols = c[1];
            final int elementBytes = c[2];
            final int cellCost = c[3];
            final String label = rows + "x" + cols + " cost " + cellCost;
            final long work = (long) rows * cols * cellCost; // no case here comes near Long.MAX_VALUE
            final int desired = (int) Math.min(target, Math.max(2, work / minWork));
            final int minBandWidth = Math.max(1, Matrices.MATRIX_MULTIPLY_MIN_BAND_BYTES / elementBytes / cellCost);
            final int minTaskCells = (minWork - 1) / cellCost + 1;
            // The contract: a block may fall below the minimum only when NO single row or column cut yields two rectangles
            // that both reach it (two rectangles that tile a rectangle are always one straight cut).
            final boolean rowCutQualifies = rows >= 2 * ((minTaskCells - 1) / cols + 1);
            final boolean columnCutQualifies = cols >= 2 * Math.max(minBandWidth, (minTaskCells - 1) / rows + 1);
            final boolean minimumReachable = rowCutQualifies || columnCutQualifies;
            final AtomicIntegerArray visits = new AtomicIntegerArray(rows * cols);
            final AtomicInteger blocks = new AtomicInteger();
            final java.util.concurrent.atomic.AtomicLong smallestBlock = new java.util.concurrent.atomic.AtomicLong(Long.MAX_VALUE);
            final Thread caller = Thread.currentThread();
            final AtomicInteger onCaller = new AtomicInteger();

            Matrices.forEachBlockInParallel(0, rows, 0, cols, elementBytes, cellCost, (r0, r1, c0, c1) -> {
                blocks.incrementAndGet();

                if (Thread.currentThread() == caller) {
                    onCaller.incrementAndGet();
                }

                final long cells = (long) (r1 - r0) * (c1 - c0);
                smallestBlock.accumulateAndGet(cells, Math::min);

                if (minimumReachable) {
                    assertTrue(cells * cellCost >= minWork, label + " block too small: " + cells + " cells");
                }

                if (c1 - c0 != cols) {
                    assertTrue(r1 - r0 == 1 || r1 - r0 == rows, label + " a banded block must be one row or the full height");
                    assertTrue(c1 - c0 >= minBandWidth, label + " band narrower than the cache-line minimum: " + (c1 - c0));
                }

                for (int i = r0; i < r1; i++) {
                    for (int j = c0; j < c1; j++) {
                        visits.incrementAndGet(i * cols + j);
                    }
                }
            });

            for (int idx = 0; idx < rows * cols; idx++) {
                assertEquals(1, visits.get(idx), label + " idx=" + idx);
            }

            assertTrue(blocks.get() <= target, label + " more tasks than the target: " + blocks.get());

            if (work >= 2L * minWork) {
                // Enough work for two tasks: never serialized, and at least half the wanted parallelism unless the shape
                // has fewer cache-line-sized blocks than that.
                final long minTasks = Math.max(2, Math.min(desired / 2, (long) rows * (cols / minBandWidth)));
                assertTrue(blocks.get() >= minTasks, label + " too few tasks: " + blocks.get() + " of " + desired);
            } else {
                assertTrue(blocks.get() <= 2, label + " the two-task floor must not split further: " + blocks.get());
            }

            if (!minimumReachable && blocks.get() == 2) {
                // The two-task floor takes whichever single cut leaves the larger smaller half.
                final long rowHalf = (long) (rows / 2) * cols;
                final long columnHalf = cols / 2 >= minBandWidth ? (long) rows * (cols / 2) : -1;
                assertEquals(Math.max(rowHalf, columnHalf), smallestBlock.get(), label + " the floor's cut is not the most even");
            }

            if (c[4] >= 0) {
                assertEquals(c[4], blocks.get(), label + " task count");
            }

            if (blocks.get() == 1) {
                assertEquals(1, onCaller.get(), label + " single block runs on the calling thread");
            }
        }

        assertThrows(IllegalArgumentException.class, () -> Matrices.forEachBlockInParallel(0, 2, 0, 2, 4, 0, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Matrices.forEachBlockInParallel(2, 2, 4, -1, (r0, r1, c0, c1) -> {
        }));

        // Element-wise results stay identical to sequential on the coarsened split, including a range that does not start at 0.
        final LongMatrix tall = LongMatrix.random(100_000, 3, new Random(7));
        assertEquals(under(ParallelMode.FORCE_OFF, () -> tall.map(v -> v ^ 0x5a)), under(ParallelMode.FORCE_ON, () -> tall.map(v -> v ^ 0x5a)));
        assertEquals(under(ParallelMode.FORCE_OFF, () -> tall.add(tall)), under(ParallelMode.FORCE_ON, () -> tall.add(tall)));
        final AtomicInteger regionVisits = new AtomicInteger();
        under(ParallelMode.FORCE_ON, () -> {
            tall.forEach(10, 99_990, 1, 3, v -> regionVisits.incrementAndGet());
            return null;
        });
        assertEquals((99_990 - 10) * 2, regionVisits.get());
    }

    /**
     * A collection zip qualifies for {@code AUTO} parallelism on output cells x matrix count, and its blocks must be sized the
     * same way: sizing them by the 64 output cells of 128 {@code 1 x 64} byte matrices alone ran the whole fold on the calling
     * thread. The callback waits (bounded) until a second thread has joined, so a serialized fold fails instead of passing by luck.
     */
    @Test
    void collectionZipFoldOfManySmallMatricesStillRunsOnSeveralThreadsUnderAuto() {
        org.junit.jupiter.api.Assumptions.assumeTrue(Runtime.getRuntime().availableProcessors() > 1, "needs two processors");

        final Random random = new Random(99);
        final List<ByteMatrix> byteInputs = new ArrayList<>();
        final List<LongMatrix> longInputs = new ArrayList<>();

        for (int k = 0; k < 128; k++) {
            byteInputs.add(ByteMatrix.random(1, 64, random));
            longInputs.add(LongMatrix.random(1, 64, random));
        }

        assertTrue(under(ParallelMode.AUTO, () -> Matrices.shouldRunInParallel(byteInputs.get(0), 64L * 128)), "the fold must qualify for AUTO parallelism");

        for (final boolean bytes : new boolean[] { true, false }) {
            final Set<Thread> threads = java.util.concurrent.ConcurrentHashMap.newKeySet();
            final java.util.concurrent.CountDownLatch secondThread = new java.util.concurrent.CountDownLatch(2);
            final java.util.concurrent.atomic.AtomicBoolean gaveUp = new java.util.concurrent.atomic.AtomicBoolean();

            // Waits at most once per fold: after one timeout every later callback runs straight through, so a serialized
            // fold fails after ~5 seconds instead of waiting once per callback.
            final Runnable waitForSecondThread = () -> {
                if (threads.add(Thread.currentThread())) {
                    secondThread.countDown();
                }

                if (secondThread.getCount() > 0 && !gaveUp.get()) {
                    try {
                        if (!secondThread.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                            gaveUp.set(true);
                        }
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        gaveUp.set(true);
                    }
                }
            };

            if (bytes) {
                final ByteMatrix parallel = under(ParallelMode.AUTO, () -> Matrices.zip(byteInputs, (x, y) -> {
                    waitForSecondThread.run();
                    return (byte) (x ^ y);
                }));
                assertEquals(under(ParallelMode.FORCE_OFF, () -> Matrices.zip(byteInputs, (x, y) -> (byte) (x ^ y))), parallel);
            } else {
                final LongMatrix parallel = under(ParallelMode.AUTO, () -> Matrices.zip(longInputs, (x, y) -> {
                    waitForSecondThread.run();
                    return x ^ y;
                }));
                assertEquals(under(ParallelMode.FORCE_OFF, () -> Matrices.zip(longInputs, (x, y) -> x ^ y)), parallel);
            }

            assertTrue(threads.size() >= 2, (bytes ? "byte" : "long") + " fold ran on " + threads.size() + " thread(s)");
        }
    }

    /**
     * Parallel collection folds hoist one row reference per input per task only up to {@code MAX_HOISTED_FOLD_INPUTS} inputs
     * (an unbounded buffer ran a 16-thread fold of 1,500,000 matrices out of a 64 MB heap); above it they index the matrices
     * directly. All 14 parallel folds (5 binary, 9 N-function) have their own copy of both paths, and each must compute every cell
     * as the sequential left fold: order-sensitive, non-commutative functions (and floating-point ones, where evaluation order
     * changes bits) must match FORCE_OFF exactly, for both split shapes (row groups and bands), below and above the limit.
     */
    @Test
    void parallelCollectionFoldsMatchTheSequentialLeftFoldAboveAndBelowTheHoistLimit() {
        final Random random = new Random(2026);

        for (final int count : new int[] { 3, 17, Matrices.MAX_HOISTED_FOLD_INPUTS + 7 }) {
            for (final int[] shape : new int[][] { { 3, 400 }, { 300, 5 } }) {
                final String label = count + " x (" + shape[0] + "x" + shape[1] + ") ";
                final List<ByteMatrix> bytes = new ArrayList<>();
                final List<IntMatrix> ints = new ArrayList<>();
                final List<LongMatrix> longs = new ArrayList<>();
                final List<DoubleMatrix> doubles = new ArrayList<>();
                final List<Matrix<Integer>> boxed = new ArrayList<>();

                for (int k = 0; k < count; k++) {
                    final IntMatrix im = IntMatrix.random(shape[0], shape[1], random);
                    bytes.add(ByteMatrix.random(shape[0], shape[1], random));
                    ints.add(im);
                    longs.add(im.toLongMatrix());
                    doubles.add(DoubleMatrix.random(shape[0], shape[1], random));
                    boxed.add(im.boxed());
                }

                // The five binary folds.
                assertParallelMatchesSequential(label + "byte zip", () -> Matrices.zip(bytes, (x, y) -> (byte) (x * 31 + y)));
                assertParallelMatchesSequential(label + "int zip", () -> Matrices.zip(ints, (x, y) -> x * 31 + y));
                assertParallelMatchesSequential(label + "long zip", () -> Matrices.zip(longs, (x, y) -> x * 31 + y));
                assertParallelMatchesSequential(label + "double zip", () -> Matrices.zip(doubles, (x, y) -> x * 0.75 + y));
                assertParallelMatchesSequential(label + "Integer zip", () -> Matrices.zip(boxed, (x, y) -> x * 31 + y));

                // The nine N-function folds.
                assertParallelMatchesSequential(label + "byte zipToInt", () -> Matrices.zipToInt(bytes, DeepReviewFixesRegressionTest::orderedByteHash, false));
                assertParallelMatchesSequential(label + "byte zipToObj",
                        () -> Matrices.zipToObj(bytes, DeepReviewFixesRegressionTest::orderedByteHash, false, Integer.class));
                assertParallelMatchesSequential(label + "int zipToLong", () -> Matrices.zipToLong(ints, DeepReviewFixesRegressionTest::orderedIntHash, false));
                assertParallelMatchesSequential(label + "int zipToDouble", () -> Matrices.zipToDouble(ints, v -> orderedIntHash(v).doubleValue(), false));
                assertParallelMatchesSequential(label + "int zipToObj",
                        () -> Matrices.zipToObj(ints, DeepReviewFixesRegressionTest::orderedIntHash, false, Long.class));
                assertParallelMatchesSequential(label + "long zipToDouble", () -> Matrices.zipToDouble(longs, v -> orderedLongHash(v).doubleValue(), false));
                assertParallelMatchesSequential(label + "long zipToObj",
                        () -> Matrices.zipToObj(longs, DeepReviewFixesRegressionTest::orderedLongHash, false, Long.class));
                assertParallelMatchesSequential(label + "double zipToObj",
                        () -> Matrices.zipToObj(doubles, DeepReviewFixesRegressionTest::orderedDoubleHash, false, Double.class));
                assertParallelMatchesSequential(label + "Integer[] zip",
                        () -> Matrices.zip(boxed, DeepReviewFixesRegressionTest::orderedBoxedHash, false, Integer.class, Long.class));
            }
        }

        // The out-of-memory report's shape: one 1 x 64 matrix repeated, far above the hoist limit.
        final List<ByteMatrix> repeated = Collections.nCopies(20_000, ByteMatrix.random(1, 64, random));
        assertParallelMatchesSequential("repeated byte zip", () -> Matrices.zip(repeated, (x, y) -> (byte) (x * 31 + y)));
        assertParallelMatchesSequential("repeated byte zipToInt", () -> Matrices.zipToInt(repeated, DeepReviewFixesRegressionTest::orderedByteHash));
    }

    /**
     * Pins the memory bound itself, which the equivalence test above cannot see (20,000 inputs fit with or without it): above
     * {@code MAX_HOISTED_FOLD_INPUTS} inputs a parallel fold task must not allocate a buffer of one row reference per input.
     * Under FORCE_ON a {@code 1 x 1} fold is a single block, which runs on the calling thread, so that thread's allocation
     * counter sees every per-task buffer. The measure is relative, so validation, type resolution and callback boxing cancel
     * out: the sequential (FORCE_OFF) path does the same work and still hoists exactly one such buffer for the whole call, so
     * the parallel path must allocate at least half a {@code count}-reference array less. Without the limit, the two match.
     */
    @Test
    void parallelCollectionFoldsAllocateNoPerInputRowBufferAboveTheHoistLimit() {
        final java.lang.management.ThreadMXBean bean = java.lang.management.ManagementFactory.getThreadMXBean();
        org.junit.jupiter.api.Assumptions
                .assumeTrue(bean instanceof com.sun.management.ThreadMXBean && ((com.sun.management.ThreadMXBean) bean).isThreadAllocatedMemorySupported()
                        && ((com.sun.management.ThreadMXBean) bean).isThreadAllocatedMemoryEnabled(), "needs per-thread allocation counters");
        final com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) bean;

        final int count = 200_000;
        assertTrue(count > Matrices.MAX_HOISTED_FOLD_INPUTS);
        final Random random = new Random(7);
        final List<ByteMatrix> bytes = Collections.nCopies(count, ByteMatrix.random(1, 1, random));
        final List<IntMatrix> ints = Collections.nCopies(count, IntMatrix.random(1, 1, random));
        final List<LongMatrix> longs = Collections.nCopies(count, LongMatrix.random(1, 1, random));
        final List<DoubleMatrix> doubles = Collections.nCopies(count, DoubleMatrix.random(1, 1, random));
        final List<Matrix<Integer>> boxed = Collections.nCopies(count, IntMatrix.random(1, 1, random).boxed());

        // One array of count references: the snapshot every fold takes, and the unit of the per-input buffer under test.
        bytes.toArray(new ByteMatrix[0]); // warm up
        final long refs = allocatedBy(threads, () -> bytes.toArray(new ByteMatrix[0]));
        assertTrue(refs >= 4L * count, "reference array smaller than expected: " + refs);

        // { label, fold }
        final Object[][] cases = { { "byte zip", (Runnable) () -> Matrices.zip(bytes, (x, y) -> (byte) (x ^ y)) },
                { "int zip", (Runnable) () -> Matrices.zip(ints, (x, y) -> x ^ y) }, { "long zip", (Runnable) () -> Matrices.zip(longs, (x, y) -> x ^ y) },
                { "double zip", (Runnable) () -> Matrices.zip(doubles, (x, y) -> x + y) },
                { "Integer zip", (Runnable) () -> Matrices.zip(boxed, (x, y) -> x ^ y) },
                { "byte zipToInt", (Runnable) () -> Matrices.zipToInt(bytes, v -> v.length, false) },
                { "byte zipToObj", (Runnable) () -> Matrices.zipToObj(bytes, v -> v.length, false, Integer.class) },
                { "int zipToLong", (Runnable) () -> Matrices.zipToLong(ints, v -> (long) v.length, false) },
                { "int zipToDouble", (Runnable) () -> Matrices.zipToDouble(ints, v -> (double) v.length, false) },
                { "int zipToObj", (Runnable) () -> Matrices.zipToObj(ints, v -> v.length, false, Integer.class) },
                { "long zipToDouble", (Runnable) () -> Matrices.zipToDouble(longs, v -> (double) v.length, false) },
                { "long zipToObj", (Runnable) () -> Matrices.zipToObj(longs, v -> v.length, false, Integer.class) },
                { "double zipToObj", (Runnable) () -> Matrices.zipToObj(doubles, v -> v.length, false, Integer.class) },
                { "Integer[] zip", (Runnable) () -> Matrices.zip(boxed, v -> v.length, false, Integer.class, Integer.class) } };

        for (final Object[] c : cases) {
            final String label = (String) c[0];
            final Runnable fold = (Runnable) c[1];
            final Runnable sequential = () -> Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, fold::run);
            final Runnable forcedParallel = () -> Matrices.runWithParallelMode(ParallelMode.FORCE_ON, fold::run);
            sequential.run(); // warm up: class initialization and lambda linkage allocate on first use
            forcedParallel.run();
            final long sequentialBytes = allocatedBy(threads, sequential);
            final long parallelBytes = allocatedBy(threads, forcedParallel);

            assertTrue(sequentialBytes - parallelBytes > refs / 2, label + ": parallel allocated " + parallelBytes + " bytes vs sequential " + sequentialBytes
                    + "; the parallel task must skip the per-input row buffer (one reference array = " + refs + ")");
        }
    }

    private static long allocatedBy(final com.sun.management.ThreadMXBean threads, final Runnable action) {
        final long before = threads.getCurrentThreadAllocatedBytes();
        action.run();
        return threads.getCurrentThreadAllocatedBytes() - before;
    }

    private static void assertParallelMatchesSequential(final String label, final Supplier<Object> fold) {
        assertEquals(under(ParallelMode.FORCE_OFF, fold), under(ParallelMode.FORCE_ON, fold), label);
    }

    /** An order-sensitive hash of the values, so a gather that permutes or drops inputs changes the result. */
    private static Integer orderedByteHash(final byte... values) {
        int hash = 17;

        for (final byte value : values) {
            hash = hash * 31 + value;
        }

        return hash;
    }

    /** An order-sensitive hash of the values, so a gather that permutes or drops inputs changes the result. */
    private static Long orderedIntHash(final int... values) {
        long hash = 17;

        for (final int value : values) {
            hash = hash * 1_000_003 + value;
        }

        return hash;
    }

    /** An order-sensitive hash of the values, so a gather that permutes or drops inputs changes the result. */
    private static Long orderedLongHash(final long... values) {
        long hash = 17;

        for (final long value : values) {
            hash = hash * 1_000_003 + value;
        }

        return hash;
    }

    /** An order-sensitive floating-point fold of the values: permuting them changes the bits of the result. */
    private static Double orderedDoubleHash(final double... values) {
        double hash = 0.1;

        for (final double value : values) {
            hash = hash * 0.75 + value;
        }

        return hash;
    }

    /** An order-sensitive hash of the values, so a gather that permutes or drops inputs changes the result. */
    private static Long orderedBoxedHash(final Integer[] values) {
        long hash = 17;

        for (final Integer value : values) {
            hash = hash * 1_000_003 + value;
        }

        return hash;
    }

    // ---- Tiled transpose / rotate90 / rotate270 & single-pass flips above 64x64 threshold ----

    @Test
    void tiledTransposeRotationsAndSinglePassFlipsMatchReferenceAcrossAllVariants() {
        final Random random = new Random(20261121L);

        for (final int[] shape : new int[][] { { 63, 65 }, { 64, 64 }, { 65, 70 }, { 70, 65 }, { 100, 73 } }) {
            final int r = shape[0];
            final int c = shape[1];
            final IntMatrix im = IntMatrix.random(r, c, random);
            final List<AbstractMatrix<?, ?, ?, ?, ?>> variants = List.of(im, im.toLongMatrix(), im.toFloatMatrix(), im.toDoubleMatrix(),
                    im.mapToObj(x -> (short) x, Short.class).mapToShort(x -> x), im.mapToObj(x -> (byte) x, Byte.class).mapToByte(x -> x),
                    im.mapToObj(x -> (char) x, Character.class).mapToChar(x -> x), im.mapToObj(x -> (x & 1) == 0, Boolean.class).mapToBoolean(x -> x),
                    im.mapToObj(x -> "v" + (x & 31), String.class));

            for (final AbstractMatrix<?, ?, ?, ?, ?> m : variants) {
                final String label = m.getClass().getSimpleName() + " " + r + "x" + c;
                final AbstractMatrix<?, ?, ?, ?, ?> transposed = m.transpose();
                assertEquals(c, transposed.rowCount(), label);
                assertEquals(r, transposed.columnCount(), label);
                assertEquals(m, transposed.transpose(), label + " transpose^2");

                final AbstractMatrix<?, ?, ?, ?, ?> r90 = m.rotate90();
                final AbstractMatrix<?, ?, ?, ?, ?> r180 = m.rotate180();
                final AbstractMatrix<?, ?, ?, ?, ?> r270 = m.rotate270();
                assertEquals(r180, r90.rotate90(), label + " r90^2 == r180");
                assertEquals(r270, r180.rotate90(), label + " r90^3 == r270");
                assertEquals(m, r90.rotate270(), label + " r90*r270 == id");
                assertEquals(m, r180.rotate180(), label + " r180^2 == id");

                final AbstractMatrix<?, ?, ?, ?, ?> fh = m.flipHorizontally();
                final AbstractMatrix<?, ?, ?, ?, ?> fv = m.flipVertically();
                assertEquals(m, fh.flipHorizontally(), label + " fh^2 == id");
                assertEquals(m, fv.flipVertically(), label + " fv^2 == id");
                assertEquals(r180, fh.flipVertically(), label + " fh*fv == r180");
            }

            // Exact coordinate check of every cell for tiled transpose/rotate90/rotate270 and the single-pass ops.
            final IntMatrix t = im.transpose();
            final IntMatrix r90 = im.rotate90();
            final IntMatrix r180 = im.rotate180();
            final IntMatrix r270 = im.rotate270();
            final IntMatrix fh = im.flipHorizontally();
            final IntMatrix fv = im.flipVertically();

            for (int i = 0; i < r; i++) {
                for (int j = 0; j < c; j++) {
                    final int v = im.get(i, j);
                    assertEquals(v, t.get(j, i));
                    assertEquals(v, r90.get(j, r - 1 - i));
                    assertEquals(v, r180.get(r - 1 - i, c - 1 - j));
                    assertEquals(v, r270.get(c - 1 - j, i));
                    assertEquals(v, fh.get(i, c - 1 - j));
                    assertEquals(v, fv.get(r - 1 - i, j));
                }
            }

            // Every other variant must place each cell exactly where IntMatrix does: the element-wise conversion
            // commutes with any permutation of cells, so f(op(im)) == op(f(im)) pins the coordinates exactly
            // (a self-inverse coordinate bug would survive the round-trip identities above).
            final List<java.util.function.Function<IntMatrix, AbstractMatrix<?, ?, ?, ?, ?>>> conversions = List.of(IntMatrix::toLongMatrix,
                    IntMatrix::toFloatMatrix, IntMatrix::toDoubleMatrix, x -> x.mapToObj(v -> (short) v, Short.class).mapToShort(v -> v),
                    x -> x.mapToObj(v -> (byte) v, Byte.class).mapToByte(v -> v), x -> x.mapToObj(v -> (char) v, Character.class).mapToChar(v -> v),
                    x -> x.mapToObj(v -> (v & 1) == 0, Boolean.class).mapToBoolean(v -> v), x -> x.mapToObj(v -> "v" + v, String.class));

            for (final java.util.function.Function<IntMatrix, AbstractMatrix<?, ?, ?, ?, ?>> f : conversions) {
                final AbstractMatrix<?, ?, ?, ?, ?> m = f.apply(im);
                final String label = m.getClass().getSimpleName() + " " + r + "x" + c;
                assertEquals(f.apply(t), m.transpose(), label + " transpose coordinates");
                assertEquals(f.apply(r90), m.rotate90(), label + " rotate90 coordinates");
                assertEquals(f.apply(r180), m.rotate180(), label + " rotate180 coordinates");
                assertEquals(f.apply(r270), m.rotate270(), label + " rotate270 coordinates");
                assertEquals(f.apply(fh), m.flipHorizontally(), label + " flipHorizontally coordinates");
                assertEquals(f.apply(fv), m.flipVertically(), label + " flipVertically coordinates");
            }
        }
    }

    // ---- Wide Float/Double matrixMultiply IEEE-754 bit-exactness ----

    @Test
    void wideFloatAndDoubleMatrixMultiplyPreserveIeee754BitExactness() {
        final Random random = new Random(99L);
        final int rows = 5;
        final int inner = 17;
        final int doubleCols = 300;
        final int floatCols = 600;

        final double[][] dLeft = new double[rows][inner];
        final double[][] dRight = new double[inner][doubleCols];

        for (int i = 0; i < rows; i++) {
            for (int k = 0; k < inner; k++) {
                dLeft[i][k] = pickSpecialDouble(random);
            }
        }

        for (int k = 0; k < inner; k++) {
            for (int j = 0; j < doubleCols; j++) {
                dRight[k][j] = pickSpecialDouble(random);
            }
        }

        final DoubleMatrix dmLeft = DoubleMatrix.wrap(dLeft);
        final DoubleMatrix dmRight = DoubleMatrix.wrap(dRight);
        final DoubleMatrix dmSeq = under(ParallelMode.FORCE_OFF, () -> dmLeft.matrixMultiply(dmRight));
        final DoubleMatrix dmPar = under(ParallelMode.FORCE_ON, () -> dmLeft.matrixMultiply(dmRight));
        assertEquals(dmSeq, dmPar);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < doubleCols; j++) {
                double expected = 0.0;

                for (int k = 0; k < inner; k++) {
                    expected += dLeft[i][k] * dRight[k][j];
                }

                assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(dmSeq.get(i, j)), "double (" + i + "," + j + ")");
            }
        }

        final float[][] fLeft = new float[rows][inner];
        final float[][] fRight = new float[inner][floatCols];

        for (int i = 0; i < rows; i++) {
            for (int k = 0; k < inner; k++) {
                fLeft[i][k] = (float) pickSpecialDouble(random);
            }
        }

        for (int k = 0; k < inner; k++) {
            for (int j = 0; j < floatCols; j++) {
                fRight[k][j] = (float) pickSpecialDouble(random);
            }
        }

        final FloatMatrix fmLeft = FloatMatrix.wrap(fLeft);
        final FloatMatrix fmRight = FloatMatrix.wrap(fRight);
        final FloatMatrix fmSeq = under(ParallelMode.FORCE_OFF, () -> fmLeft.matrixMultiply(fmRight));
        final FloatMatrix fmPar = under(ParallelMode.FORCE_ON, () -> fmLeft.matrixMultiply(fmRight));
        assertEquals(fmSeq, fmPar);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < floatCols; j++) {
                float expected = 0.0f;

                for (int k = 0; k < inner; k++) {
                    expected += fLeft[i][k] * fRight[k][j];
                }

                assertEquals(Float.floatToIntBits(expected), Float.floatToIntBits(fmSeq.get(i, j)), "float (" + i + "," + j + ")");
            }
        }
    }

    /**
     * The Float/Double multiply kernel must stay bit-exact for wide results both when each parallel task owns full-width rows
     * (row count at or above the task target) and when few rows are split into column bands. A column-tiled kernel was removed
     * because it measured ~2x slower on results wider than the tile; this pins the semantics of the plain kernel in both splits.
     */
    @Test
    void floatAndDoubleMatrixMultiplyAreBitExactForFullWidthRowTasksAndColumnBands() {
        final Random random = new Random(20261125L);
        final int inner = 9;
        final int cols = 700;

        for (final int rows : new int[] { 3, 300 }) {
            final double[][] dLeft = new double[rows][inner];
            final double[][] dRight = new double[inner][cols];
            final float[][] fLeft = new float[rows][inner];
            final float[][] fRight = new float[inner][cols];

            for (int i = 0; i < rows; i++) {
                for (int k = 0; k < inner; k++) {
                    dLeft[i][k] = pickSpecialDouble(random);
                    fLeft[i][k] = (float) pickSpecialDouble(random);
                }
            }

            for (int k = 0; k < inner; k++) {
                for (int j = 0; j < cols; j++) {
                    dRight[k][j] = pickSpecialDouble(random);
                    fRight[k][j] = (float) pickSpecialDouble(random);
                }
            }

            final DoubleMatrix dmLeft = DoubleMatrix.wrap(dLeft);
            final DoubleMatrix dmRight = DoubleMatrix.wrap(dRight);
            final FloatMatrix fmLeft = FloatMatrix.wrap(fLeft);
            final FloatMatrix fmRight = FloatMatrix.wrap(fRight);

            for (final ParallelMode mode : new ParallelMode[] { ParallelMode.FORCE_OFF, ParallelMode.FORCE_ON }) {
                final DoubleMatrix dProduct = under(mode, () -> dmLeft.matrixMultiply(dmRight));
                final FloatMatrix fProduct = under(mode, () -> fmLeft.matrixMultiply(fmRight));

                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        double dExpected = 0.0;
                        float fExpected = 0.0f;

                        for (int k = 0; k < inner; k++) {
                            dExpected += dLeft[i][k] * dRight[k][j];
                            fExpected += fLeft[i][k] * fRight[k][j];
                        }

                        if (Double.isNaN(dExpected)) {
                            assertTrue(Double.isNaN(dProduct.get(i, j)), mode + " double NaN (" + i + "," + j + ")");
                        } else {
                            assertEquals(Double.doubleToRawLongBits(dExpected), Double.doubleToRawLongBits(dProduct.get(i, j)),
                                    mode + " double " + rows + " rows (" + i + "," + j + ")");
                        }

                        if (Float.isNaN(fExpected)) {
                            assertTrue(Float.isNaN(fProduct.get(i, j)), mode + " float NaN (" + i + "," + j + ")");
                        } else {
                            assertEquals(Float.floatToRawIntBits(fExpected), Float.floatToRawIntBits(fProduct.get(i, j)),
                                    mode + " float " + rows + " rows (" + i + "," + j + ")");
                        }
                    }
                }
            }
        }
    }

    private static double pickSpecialDouble(final Random random) {
        return switch (random.nextInt(14)) {
            case 0 -> 0.0;
            case 1 -> -0.0;
            case 2 -> Double.NaN;
            case 3 -> Double.POSITIVE_INFINITY;
            case 4 -> Double.NEGATIVE_INFINITY;
            default -> (random.nextDouble() - 0.5) * Math.pow(10, random.nextInt(20) - 10);
        };
    }

    // ---- Integer matrixMultiply / widened / exact fast paths ----

    @Test
    void integerWidenedExactAndZeroInnerMultiplyPathsMatchReferenceInBothModes() {
        final ByteMatrix b1 = ByteMatrix.wrap(new byte[][] { { 120, 0, -120 }, { 0, 50, 0 } });
        final ByteMatrix b2 = ByteMatrix.wrap(new byte[][] { { 10, 0 }, { 0, 20 }, { -10, 5 } });
        assertEquals(under(ParallelMode.FORCE_OFF, () -> b1.addWidened(b1)), under(ParallelMode.FORCE_ON, () -> b1.addWidened(b1)));
        assertEquals(under(ParallelMode.FORCE_OFF, () -> b1.subtractWidened(b1)), under(ParallelMode.FORCE_ON, () -> b1.subtractWidened(b1)));
        assertEquals(under(ParallelMode.FORCE_OFF, () -> b1.matrixMultiplyWidened(b2)), under(ParallelMode.FORCE_ON, () -> b1.matrixMultiplyWidened(b2)));
        assertThrows(ArithmeticException.class, () -> ByteMatrix.wrap(new byte[][] { { Byte.MAX_VALUE } }).addExact(ByteMatrix.wrap(new byte[][] { { 1 } })));
        assertThrows(ArithmeticException.class,
                () -> ByteMatrix.wrap(new byte[][] { { Byte.MIN_VALUE } }).subtractExact(ByteMatrix.wrap(new byte[][] { { 1 } })));
        assertEquals(ByteMatrix.wrap(new byte[][] { { 0, 100 }, { 0, 0 } }),
                ByteMatrix.wrap(new byte[][] { { 100, -100 }, { 0, 0 } }).matrixMultiplyExact(ByteMatrix.wrap(new byte[][] { { 1, 1 }, { 1, 0 } })));

        final ShortMatrix s1 = ShortMatrix.wrap(new short[][] { { 30000, 0 }, { 0, -30000 } });
        assertEquals(under(ParallelMode.FORCE_OFF, () -> s1.addWidened(s1)), under(ParallelMode.FORCE_ON, () -> s1.addWidened(s1)));
        assertEquals(under(ParallelMode.FORCE_OFF, () -> s1.subtractWidened(s1)), under(ParallelMode.FORCE_ON, () -> s1.subtractWidened(s1)));
        assertEquals(under(ParallelMode.FORCE_OFF, () -> s1.matrixMultiplyWidened(s1)), under(ParallelMode.FORCE_ON, () -> s1.matrixMultiplyWidened(s1)));
        assertThrows(ArithmeticException.class,
                () -> ShortMatrix.wrap(new short[][] { { Short.MAX_VALUE } }).addExact(ShortMatrix.wrap(new short[][] { { 1 } })));
        assertThrows(ArithmeticException.class,
                () -> ShortMatrix.wrap(new short[][] { { Short.MIN_VALUE } }).subtractExact(ShortMatrix.wrap(new short[][] { { 1 } })));

        final IntMatrix i1 = IntMatrix.wrap(new int[][] { { 1_000_000_000, 0 }, { 0, -1_000_000_000 } });
        assertEquals(under(ParallelMode.FORCE_OFF, () -> i1.addWidened(i1)), under(ParallelMode.FORCE_ON, () -> i1.addWidened(i1)));
        assertEquals(under(ParallelMode.FORCE_OFF, () -> i1.subtractWidened(i1)), under(ParallelMode.FORCE_ON, () -> i1.subtractWidened(i1)));
        assertEquals(under(ParallelMode.FORCE_OFF, () -> i1.matrixMultiplyWidened(i1)), under(ParallelMode.FORCE_ON, () -> i1.matrixMultiplyWidened(i1)));

        // LongMatrix.matrixMultiplyExact all-zero shortcut & self-squaring shortcut.
        final LongMatrix zeros = LongMatrix.wrap(new long[4][5]);
        final LongMatrix extreme = LongMatrix.wrap(new long[][] { { Long.MIN_VALUE, Long.MAX_VALUE }, { 1, -1 }, { 0, 0 }, { 2, 3 }, { -4, 5 } });
        assertEquals(LongMatrix.wrap(new long[4][2]), zeros.matrixMultiplyExact(extreme));
        assertEquals(LongMatrix.wrap(new long[5][4]), LongMatrix.wrap(new long[5][5]).matrixMultiplyExact(LongMatrix.wrap(new long[5][4])));

        final LongMatrix selfSquare = LongMatrix.wrap(new long[][] { { 2, -3 }, { 4, 5 } });
        assertEquals(selfSquare.matrixMultiply(selfSquare), selfSquare.matrixMultiplyExact(selfSquare));

        // M x 0 * 0 x N under FORCE_ON produces M x N zeros across all numeric types.
        final IntMatrix mBy0 = IntMatrix.wrap(new int[3][0]);
        final IntMatrix zeroByN = IntMatrix.empty().resize(0, 4);
        assertEquals(IntMatrix.wrap(new int[3][4]), under(ParallelMode.FORCE_ON, () -> mBy0.matrixMultiply(zeroByN)));
        assertEquals(LongMatrix.wrap(new long[3][4]), under(ParallelMode.FORCE_ON, () -> mBy0.matrixMultiplyWidened(zeroByN)));
    }

    // ---- Direct equals / hashCode / toString / appendTo consistency ----

    @Test
    void directEqualsHashCodeToStringAndAppendToMatchDeepArraysContracts() {
        final List<AbstractMatrix<?, ?, ?, ?, ?>> nonCharPrimitives = List.of(BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } }),
                BooleanMatrix.empty(), BooleanMatrix.wrap(new boolean[2][0]), BooleanMatrix.empty().resize(0, 3),
                ByteMatrix.wrap(new byte[][] { { Byte.MIN_VALUE, 0, Byte.MAX_VALUE } }),
                ShortMatrix.wrap(new short[][] { { Short.MIN_VALUE, 0, Short.MAX_VALUE } }),
                IntMatrix.wrap(new int[][] { { Integer.MIN_VALUE, 0, Integer.MAX_VALUE } }),
                LongMatrix.wrap(new long[][] { { Long.MIN_VALUE, 0L, Long.MAX_VALUE } }),
                FloatMatrix.wrap(new float[][] { { Float.NaN, -0.0f, Float.POSITIVE_INFINITY, 1.5f } }),
                DoubleMatrix.wrap(new double[][] { { Double.NaN, -0.0d, Double.NEGATIVE_INFINITY, 2.5d } }));

        for (final AbstractMatrix<?, ?, ?, ?, ?> m : nonCharPrimitives) {
            final Object[] backing = m.unsafeBackingArray();

            if (m.rowCount() > 0 || m.columnCount() == 0) {
                assertEquals(Arrays.deepHashCode(backing), m.hashCode(), m.getClass().getSimpleName());
            } else {
                assertEquals(961 + m.columnCount(), m.hashCode(), m.getClass().getSimpleName());
            }

            assertEquals(Arrays.deepToString(backing), m.toString(), m.getClass().getSimpleName());
            assertEquals(m, m.copy(), m.getClass().getSimpleName());

            final StringBuilder sb = new StringBuilder();
            final StringWriter writer = new StringWriter();
            m.appendTo(sb);
            m.appendTo(writer);
            assertEquals(m.toMultilineString(), sb.toString(), m.getClass().getSimpleName());
            assertEquals(m.toMultilineString(), writer.toString(), m.getClass().getSimpleName());
        }

        // CharMatrix escapedString & appendTo consistency.
        final CharMatrix cm = CharMatrix.wrap(new char[][] { { 'a', '\'', '\\', '\n', '\0', '\uD800' } });
        final StringBuilder csb = new StringBuilder();
        final StringWriter cwriter = new StringWriter();
        cm.appendTo(csb);
        cm.appendTo(cwriter);
        assertEquals(cm.toMultilineString(), csb.toString());
        assertEquals(cm.toMultilineString(), cwriter.toString());
        assertEquals(Arrays.deepHashCode(cm.unsafeBackingArray()), cm.hashCode());
    }

    // ---- Stream iterator toArray / toList & Matrix aliasing / ArrayStoreException ----

    @Test
    void streamIteratorsToArrayAndToListHandleAliasingUndersizedTargetAndArrayStoreException() {
        final Matrix<Object> m = Matrix.wrap(Object.class, new Object[][] { { "a", "b", "c" }, { "d", "e", "f" }, { "g", "h", "i" } });

        // Destination array aliases a backing row in mainDiagonal / antiDiagonal / rowMajor / columnMajor / columnStreams.
        final Object[] diagTarget = m.rowView(0);
        final Object[] diagOut = ((ObjIteratorEx<Object>) m.mainDiagonalStream().iterator()).toArray(diagTarget);
        assertSame(diagTarget, diagOut);
        assertArrayEquals(new Object[] { "a", "e", "i" }, diagOut);

        final Matrix<Object> m2 = Matrix.wrap(Object.class, new Object[][] { { "a", "b", "c" }, { "d", "e", "f" }, { "g", "h", "i" } });
        final Object[] antiTarget = m2.rowView(0);
        final Object[] antiOut = ((ObjIteratorEx<Object>) m2.antiDiagonalStream().iterator()).toArray(antiTarget);
        assertSame(antiTarget, antiOut);
        assertArrayEquals(new Object[] { "c", "e", "g" }, antiOut);

        final Matrix<Object> m3 = Matrix.wrap(Object.class, new Object[][] { { "a", "b", "c" }, { "d", "e", "f" }, { "g", "h", "i" } });
        final Object[] colTarget = m3.rowView(0);
        final Object[] colOut = ((ObjIteratorEx<Object>) m3.columnStreams(1, 2).iterator().next().iterator()).toArray(colTarget);
        assertSame(colTarget, colOut);
        assertArrayEquals(new Object[] { "b", "e", "h" }, colOut);

        // Partial consumption preserved on ArrayStoreException in rowMajorStream.toArray(String[]).
        final Matrix<Object> mixed = Matrix.wrap(Object.class, new Object[][] { { "a", "b" }, { 42, "d" } });
        final ObjIteratorEx<Object> it = (ObjIteratorEx<Object>) mixed.rowMajorStream().iterator();
        assertThrows(ArrayStoreException.class, () -> it.toArray(new String[0]));
        assertEquals(42, it.next());
        assertEquals("d", it.next());
        assertFalse(it.hasNext());

        // BooleanMatrix toArray with undersized target allocates exact component type without copying caller garbage.
        final BooleanMatrix bm = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
        assertArrayEquals(new Boolean[] { true, true }, bm.mainDiagonalStream().toArray(Boolean[]::new));
        assertArrayEquals(new Boolean[] { false, false }, bm.antiDiagonalStream().toArray(Boolean[]::new));
        assertEquals(2, bm.rowStreams().toList().size());
        assertEquals(2, bm.columnStreams().toList().size());
        assertEquals(3, BooleanMatrix.empty().resize(0, 3).columnStreams().toList().size());
        assertEquals(3, IntMatrix.empty().resize(0, 3).columnStreams().toList().size());
    }

    // ---- BooleanMatrix self-ops & Float/Double -0.0/NaN pad/resize ----

    @Test
    void booleanSelfOpsAndFloatingPointSpecialDefaultPadAndResize() {
        final BooleanMatrix bm = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
        final BooleanMatrix selfAnd = bm.and(bm);
        final BooleanMatrix selfOr = bm.or(bm);
        final BooleanMatrix selfXor = bm.xor(bm);
        assertEquals(bm, selfAnd);
        assertEquals(bm, selfOr);
        assertNotSame(bm.rowView(0), selfAnd.rowView(0));
        assertNotSame(bm.rowView(0), selfOr.rowView(0));
        assertEquals(BooleanMatrix.wrap(new boolean[2][3]), selfXor);

        // Float/Double pad and resize with -0.0 and NaN must actually fill those values.
        final DoubleMatrix dPadded = DoubleMatrix.wrap(new double[][] { { 1.0 } }).pad(1, 1, 1, 1, -0.0);
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(dPadded.get(0, 0)));
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(dPadded.get(2, 2)));
        assertEquals(1.0, dPadded.get(1, 1));

        final FloatMatrix fResized = FloatMatrix.wrap(new float[][] { { 1.0f } }).resize(2, 2, -0.0f);
        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(fResized.get(0, 1)));
        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(fResized.get(1, 0)));
        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(fResized.get(1, 1)));

        final FloatMatrix fPadded = FloatMatrix.wrap(new float[][] { { 2.0f } }).pad(1, 1, 1, 1, -0.0f);
        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(fPadded.get(0, 0)));
        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(fPadded.get(2, 2)));
        assertEquals(2.0f, fPadded.get(1, 1));

        final DoubleMatrix dResizedNegZero = DoubleMatrix.wrap(new double[][] { { 2.0 } }).resize(2, 2, -0.0);
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(dResizedNegZero.get(0, 1)));
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(dResizedNegZero.get(1, 1)));

        final FloatMatrix fResizedNan = FloatMatrix.wrap(new float[][] { { 1.0f } }).resize(2, 2, Float.NaN);
        assertTrue(Float.isNaN(fResizedNan.get(0, 1)));
        assertTrue(Float.isNaN(fResizedNan.get(1, 1)));

        final DoubleMatrix dResizedNan = DoubleMatrix.wrap(new double[][] { { 1.0 } }).resize(2, 2, Double.NaN);
        assertTrue(Double.isNaN(dResizedNan.get(0, 1)));
        assertTrue(Double.isNaN(dResizedNan.get(1, 1)));
    }

    // ---- All 9 matrix classes: rowStreams / columnStreams toArray ArrayStoreException & contract ----

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void rowStreamsAndColumnStreamsToArrayPreserveCursorOnArrayStoreExceptionAcrossAllNineMatrices() {
        final List<AbstractMatrix<?, ?, ?, ?, ?>> matrices = List.of(BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } }),
                ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } }), CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } }),
                ShortMatrix.wrap(new short[][] { { 10, 20 }, { 30, 40 } }), IntMatrix.wrap(new int[][] { { 100, 200 }, { 300, 400 } }),
                LongMatrix.wrap(new long[][] { { 1000L, 2000L }, { 3000L, 4000L } }), FloatMatrix.wrap(new float[][] { { 1.5f, 2.5f }, { 3.5f, 4.5f } }),
                DoubleMatrix.wrap(new double[][] { { 1.25, 2.25 }, { 3.25, 4.25 } }), Matrix.wrap(String.class, new String[][] { { "p", "q" }, { "r", "s" } }));

        for (final AbstractMatrix<?, ?, ?, ?, ?> m : matrices) {
            final String name = m.getClass().getSimpleName();

            for (final Supplier<Stream<?>> streamSupplier : List.<Supplier<Stream<?>>> of(() -> (Stream) m.rowStreams(), () -> (Stream) m.columnStreams())) {
                final ObjIteratorEx<Object> it = (ObjIteratorEx<Object>) streamSupplier.get().iterator();

                // Null target throws NullPointerException without consuming elements.
                assertThrows(NullPointerException.class, () -> it.toArray((Object[]) null), name);
                assertTrue(it.hasNext(), name);

                // Incompatible target types (both undersized and sufficient) throw ArrayStoreException without advancing cursor.
                assertThrows(ArrayStoreException.class, () -> it.toArray(new String[0]), name);
                assertThrows(ArrayStoreException.class, () -> it.toArray(new String[3]), name);

                // Both elements must still remain: consume one with next(), then materialize remaining 1 into an oversized Object[].
                assertNotNull(it.next(), name);
                final Object[] oversized = { "keep0", "sentinel1", "keep2" };
                final Object[] out = it.toArray(oversized);
                assertSame(oversized, out, name);
                assertNotNull(out[0], name);
                assertNull(out[1], name + " null terminator");
                assertEquals("keep2", out[2], name);
                assertFalse(it.hasNext(), name);

                // Undersized non-empty target allocates fresh array without failing or losing elements.
                final ObjIteratorEx<Object> it2 = (ObjIteratorEx<Object>) streamSupplier.get().iterator();
                final Object[] undersized = { "garbage" };
                final Object[] allocated = it2.toArray(undersized);
                assertNotSame(undersized, allocated, name);
                assertEquals(2, allocated.length, name);
                assertNotNull(allocated[0], name);
                assertNotNull(allocated[1], name);
                assertFalse(it2.hasNext(), name);

                // toList() after partial consumption materializes only the remaining element and exhausts the iterator.
                final ObjIteratorEx<Object> it3 = (ObjIteratorEx<Object>) streamSupplier.get().iterator();
                assertNotNull(it3.next(), name);
                assertEquals(1, it3.toList().size(), name);
                assertFalse(it3.hasNext(), name);
            }
        }
    }

    // ---- Matrix covariant row aliasing ArrayStoreException & shifted-cursor aliasing in toArray ----

    @Test
    @SuppressWarnings("deprecation")
    void matrixAliasedToArrayPreservesPartiallyConsumedCursorOnCovariantArrayStoreExceptionAndShiftedCursor() {
        // Shifted cursor (after next()) with destination aliasing the last backing row.
        final Matrix<Object> m = Matrix.wrap(Object.class, new Object[][] { { "a", "b", "c" }, { "d", "e", "f" }, { "g", "h", "i" } });
        final ObjIteratorEx<Object> antiIt = (ObjIteratorEx<Object>) m.antiDiagonalStream().iterator();
        assertEquals("c", antiIt.next());
        final Object[] lastRow = m.rowView(2);
        final Object[] antiOut = antiIt.toArray(lastRow);
        assertSame(lastRow, antiOut);
        assertEquals("e", antiOut[0]);
        assertEquals("g", antiOut[1]);
        assertNull(antiOut[2]);

        // Covariant Matrix<Number> where row 0 is Integer[] and row 1 is Long[]: passing rowView(0) to toArray
        // stores index 0 (Integer) and fails at index 1 (Long -> Integer[]), leaving element 1 unconsumed.
        final Matrix<Number> cov1 = new Matrix<>(new Number[][] { new Integer[] { 1, 2 }, new Long[] { 3L, 4L } });
        final ObjIteratorEx<Number> mainDiagIt = (ObjIteratorEx<Number>) cov1.mainDiagonalStream().iterator();
        assertThrows(ArrayStoreException.class, () -> mainDiagIt.toArray(cov1.rowView(0)));
        assertTrue(mainDiagIt.hasNext());
        assertEquals(4L, mainDiagIt.next());
        assertFalse(mainDiagIt.hasNext());

        final Matrix<Number> cov2 = new Matrix<>(new Number[][] { new Integer[] { 1, 2 }, new Long[] { 3L, 4L } });
        final ObjIteratorEx<Number> antiDiagIt = (ObjIteratorEx<Number>) cov2.antiDiagonalStream().iterator();
        assertThrows(ArrayStoreException.class, () -> antiDiagIt.toArray(cov2.rowView(0)));
        assertTrue(antiDiagIt.hasNext());
        assertEquals(3L, antiDiagIt.next());
        assertFalse(antiDiagIt.hasNext());

        final Matrix<Number> cov3 = new Matrix<>(new Number[][] { new Integer[] { 1, 2 }, new Long[] { 3L, 4L } });
        final ObjIteratorEx<Number> colMajorIt = (ObjIteratorEx<Number>) cov3.columnMajorStream(0, 1).iterator();
        assertThrows(ArrayStoreException.class, () -> colMajorIt.toArray(cov3.rowView(0)));
        assertTrue(colMajorIt.hasNext());
        assertEquals(3L, colMajorIt.next());
        assertFalse(colMajorIt.hasNext());

        final Matrix<Number> cov4 = new Matrix<>(new Number[][] { new Integer[] { 1, 2 }, new Long[] { 3L, 4L } });
        final ObjIteratorEx<Number> colStreamIt = (ObjIteratorEx<Number>) cov4.columnStreams(1, 2).iterator().next().iterator();
        assertThrows(ArrayStoreException.class, () -> colStreamIt.toArray(cov4.rowView(0)));
        assertTrue(colStreamIt.hasNext());
        assertEquals(4L, colStreamIt.next());
        assertFalse(colStreamIt.hasNext());
    }

    // ---- ByteMatrix.matrixMultiplyExact long[] fallback (> 131_071 inner dimension) & toByteExact(long) ----

    @Test
    void byteMatrixMultiplyExactLongFallbackBoundaryAndOverflow() {
        // Boundary: k = 131_071 (MAX_EXACT_INT_DOT_LENGTH) on the int-accumulator path: cancelling pairs 100*100 + (-100)*100
        // plus a final 7*6, so the exact result is 42.
        final int boundary = 131_071;
        final byte[][] leftBoundary = new byte[1][boundary];
        final byte[][] rightBoundary = new byte[boundary][1];
        for (int k = 0; k < boundary - 1; k += 2) {
            leftBoundary[0][k] = 100;
            rightBoundary[k][0] = 100;
            leftBoundary[0][k + 1] = -100;
            rightBoundary[k + 1][0] = 100;
        }
        leftBoundary[0][boundary - 1] = 7;
        rightBoundary[boundary - 1][0] = 6;
        assertEquals((byte) 42, ByteMatrix.wrap(leftBoundary).matrixMultiplyExact(ByteMatrix.wrap(rightBoundary)).get(0, 0));

        // Worst-case magnitudes on the int-accumulator path at exactly k = 131_071: every partial sum must stay exact
        // (131_071 * 16_384 = 2_147_467_264 <= Integer.MAX_VALUE and 131_071 * -16_256 = -2_130_690_176). A wrapping int
        // accumulator, or a limit constant one larger, would report a different (wrapped) value here.
        final byte[][] leftWorst = new byte[1][boundary];
        final byte[][] rightWorst = new byte[boundary][1];
        Arrays.fill(leftWorst[0], Byte.MIN_VALUE);
        for (int k = 0; k < boundary; k++) {
            rightWorst[k][0] = Byte.MIN_VALUE;
        }
        assertEquals("byte overflow: 2147467264",
                assertThrows(ArithmeticException.class, () -> ByteMatrix.wrap(leftWorst).matrixMultiplyExact(ByteMatrix.wrap(rightWorst))).getMessage());

        for (int k = 0; k < boundary; k++) {
            rightWorst[k][0] = Byte.MAX_VALUE;
        }
        assertEquals("byte overflow: -2130690176",
                assertThrows(ArithmeticException.class, () -> ByteMatrix.wrap(leftWorst).matrixMultiplyExact(ByteMatrix.wrap(rightWorst))).getMessage());

        // Fallback: k = 131_072 (> MAX_EXACT_INT_DOT_LENGTH): 131_072 terms of (-128)*(-128) = +2_147_483_648L (exceeds Integer.MAX_VALUE!),
        // which must throw ArithmeticException via toByteExact(long) rather than wrapping int to negative.
        final int overBoundary = 131_072;
        final byte[][] leftOverflow = new byte[1][overBoundary];
        final byte[][] rightOverflow = new byte[overBoundary][1];
        Arrays.fill(leftOverflow[0], Byte.MIN_VALUE);
        for (int k = 0; k < overBoundary; k++) {
            rightOverflow[k][0] = Byte.MIN_VALUE;
        }
        final ArithmeticException overflowEx = assertThrows(ArithmeticException.class,
                () -> ByteMatrix.wrap(leftOverflow).matrixMultiplyExact(ByteMatrix.wrap(rightOverflow)));
        assertEquals("byte overflow: 2147483648", overflowEx.getMessage());

        // Fallback with cancellation back into byte range after exceeding Integer.MAX_VALUE:
        // 131_072 * ((-128)*(-128)) + 131_072 * (128 * (-128) via (-128)*127 + (-128)*1) -> exact cancellation!
        final int cancelLen = overBoundary * 3;
        final byte[][] leftCancel = new byte[1][cancelLen];
        final byte[][] rightCancel = new byte[cancelLen][1];
        Arrays.fill(leftCancel[0], Byte.MIN_VALUE);
        for (int k = 0; k < overBoundary; k++) {
            rightCancel[k][0] = Byte.MIN_VALUE; // -128 * -128 = +16384 (subtotal +2_147_483_648L > Integer.MAX_VALUE)
        }
        for (int k = overBoundary; k < overBoundary * 2; k++) {
            rightCancel[k][0] = Byte.MAX_VALUE; // -128 * 127 = -16256
        }
        for (int k = overBoundary * 2; k < cancelLen; k++) {
            rightCancel[k][0] = 1; // -128 * 1 = -128 (total 0)
        }
        assertEquals((byte) 0, ByteMatrix.wrap(leftCancel).matrixMultiplyExact(ByteMatrix.wrap(rightCancel)).get(0, 0));
    }

    // ---- Matrices.forEachBlockInParallel validation, estimateStringCapacity overflow, RowIdentityIndex & Point streams ----

    @Test
    void forEachBlockInParallelValidationEstimateStringCapacityAndPointStreamToArray() {
        assertThrows(IllegalArgumentException.class, () -> Matrices.forEachBlockInParallel(-1, 2, 4, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Matrices.forEachBlockInParallel(2, -1, 4, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IndexOutOfBoundsException.class, () -> Matrices.forEachBlockInParallel(-1, 2, 0, 2, 4, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IndexOutOfBoundsException.class, () -> Matrices.forEachBlockInParallel(3, 2, 0, 2, 4, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IndexOutOfBoundsException.class, () -> Matrices.forEachBlockInParallel(0, 2, -1, 2, 4, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IndexOutOfBoundsException.class, () -> Matrices.forEachBlockInParallel(0, 2, 3, 2, 4, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Matrices.forEachBlockInParallel(0, 2, 0, 2, 0, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Matrices.forEachBlockInParallel(0, 2, 0, 2, -4, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Matrices.forEachBlockInParallel(0, 0, 0, 0, 0, (r0, r1, c0, c1) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Matrices.forEachBlockInParallel(0, 0, 0, 0, 4, null));
        assertThrows(IllegalArgumentException.class, () -> Matrices.forEachBlockInParallel(0, 2, 0, 2, 4, null));

        // estimateStringCapacity must never overflow long to negative even at max dimensions.
        assertEquals(1 << 24, AbstractMatrix.estimateStringCapacity(Integer.MAX_VALUE, (long) Integer.MAX_VALUE * Integer.MAX_VALUE, 10));
        assertEquals(1 << 24, AbstractMatrix.estimateStringCapacity(10, Long.MAX_VALUE / 2, 10));
        assertEquals(2, AbstractMatrix.estimateStringCapacity(0, 0, 10));
        assertEquals(16, AbstractMatrix.estimateStringCapacity(3, 0, 8));

        // RowIdentityIndex 3-arg constructor guards for null rows and non-positive capacity.
        assertThrows(IllegalArgumentException.class, () -> new AbstractMatrix.RowIdentityIndex(null, 2, 0));
        assertThrows(IllegalArgumentException.class, () -> new AbstractMatrix.RowIdentityIndex(new Object[] { new int[0] }, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new AbstractMatrix.RowIdentityIndex(new Object[] { new int[0] }, -5, 2));

        // rowMajorPoints & columnMajorPoints toList / toArray / ArrayStoreException / NoSuchElementException message.
        final IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
        assertEquals(List.of(Point.of(0, 0), Point.of(0, 1), Point.of(1, 0), Point.of(1, 1)), m.rowMajorPoints().toList());
        assertEquals(List.of(Point.of(0, 0), Point.of(1, 0), Point.of(0, 1), Point.of(1, 1)), m.columnMajorPoints().toList());

        final ObjIteratorEx<Point> ptIt = (ObjIteratorEx<Point>) m.rowMajorPoints().iterator();
        assertThrows(NullPointerException.class, () -> ptIt.toArray((Point[]) null));
        assertThrows(ArrayStoreException.class, () -> ptIt.toArray(new String[2]));
        assertEquals(Point.of(0, 0), ptIt.next());
        final Point[] ptBuf = { Point.of(9, 9), Point.of(9, 9), Point.of(9, 9), Point.of(9, 9), Point.of(9, 9) };
        assertSame(ptBuf, ptIt.toArray(ptBuf));
        assertEquals(Point.of(0, 1), ptBuf[0]);
        assertEquals(Point.of(1, 0), ptBuf[1]);
        assertEquals(Point.of(1, 1), ptBuf[2]);
        assertNull(ptBuf[3]);
        final NoSuchElementException nsee = assertThrows(NoSuchElementException.class, ptIt::next);
        assertNotNull(nsee.getMessage());

        final ObjIteratorEx<Point> colPtIt = (ObjIteratorEx<Point>) m.columnMajorPoints().iterator();
        assertThrows(ArrayStoreException.class, () -> colPtIt.toArray(new String[2]));
        assertEquals(Point.of(0, 0), colPtIt.next());
        assertArrayEquals(new Point[] { Point.of(1, 0), Point.of(0, 1), Point.of(1, 1) }, colPtIt.toArray(new Point[0]));
    }

    // ---- Matrix wider outer array rejection, BooleanMatrix.mapToObj parallel bands & LongMatrix extreme*zeros ----

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void matrixRejectsWiderOuterArrayAndBooleanMapToObjSplitsWideRowAndLongMultiplyExactRightZeros() {
        assertThrows(IllegalArgumentException.class, () -> new Matrix(new Object[0][0], String.class, 0));
        assertThrows(IllegalArgumentException.class, () -> new Matrix(new Object[][] { { "a" } }, String.class, 1));
        assertThrows(IllegalArgumentException.class, () -> Matrix.newResult(new Object[][] { { "a" } }, (Class) String.class, 1));

        // BooleanMatrix.mapToObj on a 1 x 200 matrix uses Long.BYTES (minBandWidth = 16 -> 12 bands) under FORCE_ON.
        final boolean[][] wideBool = new boolean[1][200];
        for (int j = 0; j < 200; j++) {
            wideBool[0][j] = (j & 1) == 0;
        }
        final BooleanMatrix bmWide = BooleanMatrix.wrap(wideBool);
        final Matrix<String> mappedSeq = under(ParallelMode.FORCE_OFF, () -> bmWide.mapToObj(b -> b ? "T" : "F", String.class));
        final Matrix<String> mappedPar = under(ParallelMode.FORCE_ON, () -> bmWide.mapToObj(b -> b ? "T" : "F", String.class));
        assertEquals(mappedSeq, mappedPar);

        // LongMatrix.matrixMultiplyExact with Long.MIN_VALUE in left operand and all-zero multi-column right operand.
        final LongMatrix extremeLeft = LongMatrix.wrap(new long[][] { { Long.MIN_VALUE, Long.MAX_VALUE }, { -1L, 42L } });
        final LongMatrix zeroRight = LongMatrix.wrap(new long[2][3]);
        assertEquals(LongMatrix.wrap(new long[2][3]), extremeLeft.matrixMultiplyExact(zeroRight));
    }

    // ---- Matrices.zip* validation precedence, rejectSnapshot, parallel array isolation & typed stack* ----

    private static <T> Collection<T> rejectToArraySnapshot(final List<T> items) {
        return new AbstractCollection<>() {
            @Override
            public Iterator<T> iterator() {
                return items.iterator();
            }

            @Override
            public int size() {
                return items.size();
            }

            @Override
            public <A> A[] toArray(final A[] destination) {
                throw new AssertionError("Snapshot toArray must not be called before scalar/type validation");
            }
        };
    }

    @Test
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    void collectionZipValidationPrecedenceRejectSnapshotParallelIsolationAndTypedCollectionStacks() {
        // 1. Signature-order validation: null element or shape mismatch in coll reported BEFORE null zipFunction or void.class.
        final IntMatrix i1x2 = IntMatrix.wrap(new int[][] { { 1, 2 } });
        final IntMatrix i2x2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
        assertEquals("coll[0] is null",
                assertThrows(IllegalArgumentException.class, () -> Matrices.zipToObj(Arrays.asList((IntMatrix) null), null, String.class)).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Matrices.zipToObj(List.of(i1x2, i2x2), null, void.class)).getMessage()
                .contains("different shapes"));
        assertEquals("coll[0] is null",
                assertThrows(IllegalArgumentException.class, () -> Matrices.zipToObj(Arrays.asList((ByteMatrix) null), null, String.class)).getMessage());
        assertEquals("coll[0] is null",
                assertThrows(IllegalArgumentException.class, () -> Matrices.zipToObj(Arrays.asList((LongMatrix) null), null, String.class)).getMessage());
        assertEquals("coll[0] is null",
                assertThrows(IllegalArgumentException.class, () -> Matrices.zipToObj(Arrays.asList((DoubleMatrix) null), null, String.class)).getMessage());

        final Matrix<String> s1x2 = Matrix.wrap(String.class, new String[][] { { "a", "b" } });
        final Matrix<String> s2x2 = Matrix.wrap(String.class, new String[][] { { "a", "b" }, { "c", "d" } });
        assertEquals("coll[0] is null",
                assertThrows(IllegalArgumentException.class,
                        () -> Matrices.zip(Arrays.asList((Matrix<String>) null), (Throwables.BinaryOperator<String, RuntimeException>) null, String.class))
                                .getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Matrices.zip(List.of(s1x2, s2x2), (Throwables.Function) null, (Class) void.class))
                .getMessage()
                .contains("different shapes"));

        // 2. rejectSnapshot: scalar/type arguments validated before coll.toArray(...) on 4-arg/5-arg zip, 3-arg zip, and primitive zip.
        assertThrows(IllegalArgumentException.class,
                () -> Matrices.zip(rejectToArraySnapshot(List.of(i1x2)), (Throwables.IntBinaryOperator<RuntimeException>) null));
        assertEquals("Element type cannot be void.class", assertThrows(IllegalArgumentException.class,
                () -> Matrices.zip(rejectToArraySnapshot(List.of(s1x2)), arr -> "x", true, (Class) String.class, (Class) void.class)).getMessage());
        assertEquals("Element type cannot be void.class", assertThrows(IllegalArgumentException.class,
                () -> Matrices.zip(rejectToArraySnapshot(List.of(s1x2)), (Throwables.BinaryOperator) ((a, b) -> a), (Class) void.class)).getMessage());

        // 3. 3-arg Matrices.zip(coll, BinaryOperator, elementType) element-type compatibility message & singleton covariant widening.
        final Matrix<Number> numbers = Matrix.copyOf(Number.class, new Number[][] { { 1 } });
        final IllegalArgumentException incomp3Arg = assertThrows(IllegalArgumentException.class,
                () -> Matrices.zip(List.of(numbers), (a, b) -> a, (Class<Number>) (Class) Integer.class));
        assertTrue(incomp3Arg.getMessage().contains("not assignable to element type java.lang.Integer"), incomp3Arg.getMessage());

        final Matrix<Number> covariantLongs = new Matrix<>(new Number[][] { new Long[] { 10L, 20L } });
        final Matrix<Number> widenedCopy = Matrices.zip(List.of(covariantLongs), (a, b) -> a, Number.class);
        widenedCopy.set(0, 0, 2.5d);
        assertEquals(2.5d, widenedCopy.get(0, 0));

        // 4. Parallel callback-array isolation on 5-arg Matrices.zip(..., shareIntermediateArray = true, inputType, targetType).
        final Matrix<Integer> p1 = IntMatrix.random(8, 32, new Random(1)).boxed();
        final Matrix<Integer> p2 = IntMatrix.random(8, 32, new Random(2)).boxed();
        final Set<Integer[]> seenArrays = Collections.newSetFromMap(new IdentityHashMap<>());
        final Object lock = new Object();
        final Matrix<Integer> zippedPar = under(ParallelMode.FORCE_ON, () -> Matrices.zip(List.of(p1, p2), arr -> {
            synchronized (lock) {
                assertTrue(seenArrays.add(arr), "Parallel 5-arg zip must never share callback arrays");
            }
            return arr[0] + arr[1];
        }, true, Integer.class, Integer.class));
        assertEquals(8 * 32, seenArrays.size());
        assertEquals(p1.get(3, 7) + p2.get(3, 7), zippedPar.get(3, 7));

        // 5. Explicit-type Matrices.stackVertically / stackHorizontally(Collection, Class) & covariant stackHorizontally.
        final Matrix<Integer> mInt = Matrix.wrap(Integer.class, new Integer[][] { { 1, 2 }, { 3, 4 } });
        final Matrix<Double> mDbl = Matrix.wrap(Double.class, new Double[][] { { 0.5, 1.5 }, { 2.5, 3.5 } });
        final Matrix<Number> vStacked = Matrices.stackVertically(List.of(mInt, mDbl), Number.class);
        assertEquals(Number.class, vStacked.elementType());
        assertEquals(4, vStacked.rowCount());
        vStacked.set(0, 0, 9.25f);
        assertEquals(9.25f, vStacked.get(0, 0));

        final Matrix<Number> hStacked = Matrices.stackHorizontally(List.of(mInt, mDbl), Number.class);
        assertEquals(Number.class, hStacked.elementType());
        assertEquals(4, hStacked.columnCount());
        assertArrayEquals(new Number[] { 1, 2, 0.5, 1.5 }, hStacked.rowCopy(0));
        hStacked.set(0, 0, 9.25f);

        assertThrows(IllegalArgumentException.class, () -> Matrices.stackVertically(rejectToArraySnapshot(List.of(mInt)), (Class) void.class));
        assertThrows(IllegalArgumentException.class, () -> Matrices.stackHorizontally(List.of(numbers), (Class<Number>) (Class) Integer.class));

        final Matrix<Number> covHoriz = Matrices.stackHorizontally(List.of(covariantLongs, covariantLongs.copy()));
        covHoriz.set(0, 0, 3.14d);
        assertEquals(Number[].class, covHoriz.unsafeBackingArray()[0].getClass());
    }

    // ---- Collection zips: every element is null-checked before any shape is compared ----

    @Test
    @SuppressWarnings("unchecked")
    void collectionZipsReportLaterNullBeforeEarlierShapeMismatchAndNameNullColl() {
        final IntMatrix i2x2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
        final IntMatrix i3x3 = IntMatrix.wrap(new int[3][3]);
        final List<IntMatrix> ints = Arrays.asList(i2x2, i3x3, null);
        assertEquals("coll[2] is null", assertThrows(IllegalArgumentException.class, () -> Matrices.zip(ints, Integer::sum)).getMessage());
        assertEquals("coll[2] is null", assertThrows(IllegalArgumentException.class, () -> Matrices.zipToLong(ints, arr -> 0L)).getMessage());
        assertEquals("coll[2] is null", assertThrows(IllegalArgumentException.class, () -> Matrices.zipToObj(ints, arr -> "x", String.class)).getMessage());

        final List<DoubleMatrix> doubles = Arrays.asList(DoubleMatrix.wrap(new double[2][2]), DoubleMatrix.wrap(new double[3][3]), null);
        assertEquals("coll[2] is null", assertThrows(IllegalArgumentException.class, () -> Matrices.zip(doubles, Double::sum)).getMessage());

        final List<Matrix<String>> strings = Arrays.asList(Matrix.wrap(String.class, new String[][] { { "a" } }),
                Matrix.wrap(String.class, new String[][] { { "a", "b" } }), null);
        assertEquals("coll[2] is null", assertThrows(IllegalArgumentException.class, () -> Matrices.zip(strings, (a, b) -> a + b, String.class)).getMessage());

        // Without a null, the shape mismatch is still reported.
        assertTrue(
                assertThrows(IllegalArgumentException.class, () -> Matrices.zip(List.of(i2x2, i3x3), Integer::sum)).getMessage().contains("different shapes"));

        // A null collection names the parameter without the "or empty" wording.
        assertEquals("'coll' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> Matrices.zip((Collection<IntMatrix>) null, Integer::sum)).getMessage());
        assertEquals("'coll' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> Matrices.zip((Collection<Matrix<String>>) null, (a, b) -> a + b, String.class))
                        .getMessage());
    }

    // ---- Explicit-type Matrices.stackVertically / stackHorizontally(Collection, Class) edge cases ----

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void typedCollectionStacksValidateInSignatureOrderAndHandleSingleAndDegenerateInputs() {
        final Matrix<Integer> m2x2 = Matrix.wrap(Integer.class, new Integer[][] { { 1, 2 }, { 3, 4 } });
        final Matrix<Integer> m1x3 = Matrix.wrap(Integer.class, new Integer[][] { { 1, 2, 3 } });

        // Collection problems: null / empty collection and null elements (reported by index).
        assertEquals("'matrices' cannot be null or empty",
                assertThrows(IllegalArgumentException.class, () -> Matrices.stackVertically((Collection<Matrix<Integer>>) null, Number.class)).getMessage());
        assertEquals("'matrices' cannot be null or empty",
                assertThrows(IllegalArgumentException.class, () -> Matrices.stackHorizontally(new ArrayList<Matrix<Integer>>(), Number.class)).getMessage());
        assertEquals("matrices[1] is null",
                assertThrows(IllegalArgumentException.class, () -> Matrices.stackVertically(Arrays.asList(m2x2, null), Number.class)).getMessage());
        assertEquals("matrices[1] is null",
                assertThrows(IllegalArgumentException.class, () -> Matrices.stackHorizontally(Arrays.asList(m2x2, null), Number.class)).getMessage());

        // Shape mismatch is reported before a null or incompatible target type (signature order: matrices, then type).
        assertEquals("Column count mismatch for stackVertically: this matrix has 2 columns but other has 3",
                assertThrows(IllegalArgumentException.class, () -> Matrices.stackVertically(List.of(m2x2, m1x3), (Class<Number>) null)).getMessage());
        assertEquals("Column count mismatch for stackVertically: this matrix has 2 columns but other has 3",
                assertThrows(IllegalArgumentException.class, () -> Matrices.stackVertically(List.of(m2x2, m1x3), (Class) String.class)).getMessage());
        assertEquals("Row count mismatch for stackHorizontally: this matrix has 2 rows but other has 1",
                assertThrows(IllegalArgumentException.class, () -> Matrices.stackHorizontally(List.of(m2x2, m1x3), (Class<Number>) null)).getMessage());
        assertEquals("'targetElementType' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> Matrices.stackVertically(List.of(m2x2), (Class<Number>) null)).getMessage());

        // Matrix.empty() is Object-typed, so a narrower target is rejected exactly like the instance overload.
        final Matrix<Object> empty = Matrix.empty();
        assertEquals("Target element type java.lang.Integer cannot safely store matrices[0]'s runtime element type java.lang.Object",
                assertThrows(IllegalArgumentException.class, () -> Matrices.stackVertically(List.of(empty), (Class) Integer.class)).getMessage());

        // A single input is copied into the exact target type: independent rows, never the input or the shared empty matrix.
        final Matrix<Number> single = Matrices.stackVertically(List.of(m2x2), Number.class);
        assertEquals(Number.class, single.elementType());
        assertEquals(Number[].class, single.unsafeBackingArray()[0].getClass());
        assertNotSame(m2x2.unsafeBackingArray()[0], single.unsafeBackingArray()[0]);
        single.set(0, 0, 0.5d);
        assertEquals(1, m2x2.get(0, 0));

        final Matrix<Object> singleEmpty = Matrices.stackHorizontally(List.of(empty), Object.class);
        assertNotSame(empty, singleEmpty);
        assertEquals(0, singleEmpty.rowCount());
        assertEquals(0, singleEmpty.columnCount());

        // Degenerate shapes keep their non-zero dimension.
        final Matrix<Integer> threeByZero = Matrix.wrap(Integer.class, new Integer[3][0]);
        final Matrix<Number> v6x0 = Matrices.stackVertically(List.of(threeByZero, threeByZero), Number.class);
        assertEquals(6, v6x0.rowCount());
        assertEquals(0, v6x0.columnCount());
        assertEquals(Number.class, v6x0.elementType());
        final Matrix<Number> h3x0 = Matrices.stackHorizontally(List.of(threeByZero, threeByZero), Number.class);
        assertEquals(3, h3x0.rowCount());
        assertEquals(0, h3x0.columnCount());

        final Matrix<Integer> zeroByThree = Matrix.empty(Integer.class, 3);
        final Matrix<Number> v0x3 = Matrices.stackVertically(List.of(zeroByThree, zeroByThree), Number.class);
        assertEquals(0, v0x3.rowCount());
        assertEquals(3, v0x3.columnCount());
        final Matrix<Number> h0x6 = Matrices.stackHorizontally(List.of(zeroByThree, zeroByThree), Number.class);
        assertEquals(0, h0x6.rowCount());
        assertEquals(6, h0x6.columnCount());
    }

    // ---- Matrix row-major stream iterator toArray: narrow element loop vs. wide per-row bulk copy ----

    /**
     * {@code Matrix.rowMajorStream().toArray(...)} uses a flat element loop for narrow matrices (fewer than 16 columns) and a
     * per-row {@code System.arraycopy} otherwise; a per-row copy on 2_000_000 x 1 had measured ~7x slower than the element loop.
     * Both paths must produce identical contents, honour range streams and partial consumption, and leave the failing element
     * unconsumed on {@link ArrayStoreException}.
     */
    @Test
    @SuppressWarnings("deprecation")
    void matrixRowMajorToArrayNarrowAndWidePathsMatchElementOrderAndPreserveCursorOnArrayStoreException() {
        for (final int cols : new int[] { 1, 2, 15, 16, 17, 40 }) {
            final int rows = 5;
            final Integer[][] data = new Integer[rows][cols];

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    data[i][j] = i * 1000 + j;
                }
            }

            final Matrix<Integer> m = Matrix.wrap(Integer.class, data);
            final Integer[] expected = new Integer[rows * cols];

            for (int i = 0; i < rows; i++) {
                System.arraycopy(data[i], 0, expected, i * cols, cols);
            }

            final String label = rows + "x" + cols;
            assertArrayEquals(expected, m.rowMajorStream().toArray(), label);
            assertArrayEquals(expected, m.rowMajorStream().toArray(Integer[]::new), label);
            assertArrayEquals(Arrays.copyOfRange(expected, cols, 4 * cols), m.rowMajorStream(1, 4).toArray(Integer[]::new), label + " range");

            // Partially consumed iterator: first remaining row is a suffix (shorter than the bulk-copy threshold for most widths).
            final int consumed = cols / 2 + 1;
            final ObjIteratorEx<Integer> it = (ObjIteratorEx<Integer>) m.rowMajorStream().iterator();

            for (int n = 0; n < consumed; n++) {
                assertEquals(expected[n], it.next(), label);
            }

            final Integer[] oversized = new Integer[expected.length - consumed + 2];
            Arrays.fill(oversized, -1);
            assertSame(oversized, it.toArray(oversized), label);
            assertArrayEquals(Arrays.copyOfRange(expected, consumed, expected.length), Arrays.copyOf(oversized, expected.length - consumed), label);
            assertNull(oversized[expected.length - consumed], label + " null terminator");
            assertEquals(-1, oversized[expected.length - consumed + 1], label);
            assertFalse(it.hasNext(), label);

            // Object-typed rows into a String[] destination: the element at (2, cols - 1) is an Integer and must stay unconsumed.
            final Object[][] mixed = new Object[rows][cols];

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    mixed[i][j] = "s" + i + "_" + j;
                }
            }

            mixed[2][cols - 1] = 42;
            final ObjIteratorEx<Object> mixedIt = (ObjIteratorEx<Object>) Matrix.wrap(Object.class, mixed).rowMajorStream().iterator();
            final String[] strings = new String[rows * cols];
            assertThrows(ArrayStoreException.class, () -> mixedIt.toArray(strings), label);
            assertEquals("s0_0", strings[0], label + " elements before the failure were copied");
            assertEquals(42, mixedIt.next(), label + " failing element must remain unconsumed");
            assertEquals(rows * cols - 2 * cols - cols, mixedIt.toList().size(), label + " remaining after the failure");
        }

        // Covariant rows (Integer[] then Long[]) into an Integer[] destination on the wide path: row 0 is bulk-copied, the
        // storability verdict is re-evaluated for the Long[] row class, and its first element fails unconsumed.
        final Integer[] intRow = new Integer[20];
        final Long[] longRow = new Long[20];

        for (int j = 0; j < 20; j++) {
            intRow[j] = j;
            longRow[j] = 100L + j;
        }

        final Matrix<Number> covariant = new Matrix<>(new Number[][] { intRow, longRow, intRow.clone() });
        final ObjIteratorEx<Number> covIt = (ObjIteratorEx<Number>) covariant.rowMajorStream().iterator();
        final Integer[] intTarget = new Integer[60];
        assertThrows(ArrayStoreException.class, () -> covIt.toArray(intTarget));
        assertArrayEquals(intRow, Arrays.copyOf(intTarget, 20));
        assertEquals(100L, covIt.next());

        // Alternating row classes into a Number[] destination are all bulk-copied correctly.
        final Number[] all = covariant.rowMajorStream().toArray(Number[]::new);
        assertEquals(60, all.length);
        assertEquals(19, all[19]);
        assertEquals(100L, all[20]);
        assertEquals(119L, all[39]);
        assertEquals(0, all[40]);
    }

    // ---- Duplicate / ragged row validation above the small pairwise-scan limit through Matrix<T> factories ----

    @Test
    void matrixClassFactoriesValidateNullRaggedAndDuplicateRowsAboveSmallScanLimitInOrder() {
        final int rows = AbstractMatrix.SMALL_ROW_IDENTITY_SCAN_LIMIT + 4;
        final Supplier<Integer[][]> distinct = () -> {
            final Integer[][] d = new Integer[rows][];

            for (int i = 0; i < rows; i++) {
                d[i] = new Integer[] { i, i };
            }

            return d;
        };

        final Integer[][] duplicate = distinct.get();
        duplicate[rows - 2] = duplicate[3];
        final String duplicateMessage = "Rows must have independent storage: row " + (rows - 2) + " is the same array as row 3";
        assertEquals(duplicateMessage, assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(Integer.class, duplicate)).getMessage());
        assertEquals(duplicateMessage, assertThrows(IllegalArgumentException.class, () -> new Matrix<>(Integer.class, duplicate)).getMessage());

        // Rectangularity is checked before duplicates, and null rows before both.
        final Integer[][] raggedAndDuplicate = duplicate.clone();
        raggedAndDuplicate[rows - 3] = new Integer[] { 1 };
        assertEquals("Matrix must be rectangular: expected 2 columns per row, but row " + (rows - 3) + " has 1 columns",
                assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(Integer.class, raggedAndDuplicate)).getMessage());

        final Integer[][] nullRaggedAndDuplicate = raggedAndDuplicate.clone();
        nullRaggedAndDuplicate[5] = null;
        assertEquals("Row 5 cannot be null",
                assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(Integer.class, nullRaggedAndDuplicate)).getMessage());

        // Distinct rows above the limit are accepted and wrapped without copying.
        final Integer[][] ok = distinct.get();
        assertSame(ok[rows - 1], Matrix.wrap(Integer.class, ok).unsafeBackingArray()[rows - 1]);
    }
}
