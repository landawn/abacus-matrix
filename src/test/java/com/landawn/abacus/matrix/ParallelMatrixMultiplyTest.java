package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * The parallel matrixMultiply path runs a per-class block kernel over disjoint row or column bands
 * ({@link Matrices#matrixMultiplyInParallel}) instead of one lambda call per multiply-add. These tests pin that every
 * result cell is computed exactly once and that the parallel result is identical to the sequential one, bit for bit
 * for floating point, on tall, wide, square and degenerate shapes.
 */
class ParallelMatrixMultiplyTest extends MatrixTestSupport {

    /** {rows, inner, columns}: tall, wide (more and fewer than one 64-column band), square, and degenerate products. */
    private static final int[][] SHAPES = { { 0, 3, 4 }, { 3, 0, 4 }, { 3, 4, 0 }, { 1, 1, 1 }, { 1, 300, 1 }, { 5, 7, 3 }, { 3, 7, 5 }, { 2, 10, 200 },
            { 7, 3, 130 }, { 130, 3, 7 }, { 64, 5, 64 }, { 65, 9, 129 }, { 40, 50, 40 }, { 1, 20, 1000 } };

    @SuppressWarnings("unchecked")
    private static <T> T under(final ParallelMode mode, final Supplier<T> supplier) {
        final Object[] result = new Object[1];
        Matrices.runWithParallelMode(mode, () -> result[0] = supplier.get());
        return (T) result[0];
    }

    private static void assertParallelMatchesSequential(final String label, final Supplier<?> product) {
        assertEquals(under(ParallelMode.FORCE_OFF, product), under(ParallelMode.FORCE_ON, product), label);
    }

    /** Doubles spanning many magnitudes (and some -0.0), so a different accumulation order would change the rounding. */
    private static DoubleMatrix mixedMagnitudes(final int rows, final int columns, final Random random) {
        if (rows == 0) {
            return DoubleMatrix.random(0, columns, random); // wrap would canonicalize a rowless array to 0 x 0
        }

        final double[][] values = new double[rows][columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                final int pick = random.nextInt(10);
                values[i][j] = pick == 0 ? -0.0 : (random.nextDouble() - 0.5) * Math.pow(10, random.nextInt(33) - 16);
            }
        }

        return DoubleMatrix.wrap(values);
    }

    @Test
    void parallelProductEqualsTheSequentialProductForEveryNumericVariant() {
        final Random random = new Random(27);

        for (final int[] shape : SHAPES) {
            final String label = shape[0] + "x" + shape[1] + " * " + shape[1] + "x" + shape[2];
            final IntMatrix left = IntMatrix.random(shape[0], shape[1], random);
            final IntMatrix right = IntMatrix.random(shape[1], shape[2], random);
            final DoubleMatrix leftDouble = mixedMagnitudes(shape[0], shape[1], random);
            final DoubleMatrix rightDouble = mixedMagnitudes(shape[1], shape[2], random);
            final FloatMatrix leftFloat = leftDouble.toFloatMatrix();
            final FloatMatrix rightFloat = rightDouble.toFloatMatrix();
            final ShortMatrix leftShort = left.mapToObj(x -> (short) x, Short.class).mapToShort(x -> x);
            final ShortMatrix rightShort = right.mapToObj(x -> (short) x, Short.class).mapToShort(x -> x);
            final ByteMatrix leftByte = left.mapToObj(x -> (byte) x, Byte.class).mapToByte(x -> x);
            final ByteMatrix rightByte = right.mapToObj(x -> (byte) x, Byte.class).mapToByte(x -> x);
            final CharMatrix leftChar = left.mapToObj(x -> (char) x, Character.class).mapToChar(x -> x);
            final CharMatrix rightChar = right.mapToObj(x -> (char) x, Character.class).mapToChar(x -> x);
            final LongMatrix leftLong = left.toLongMatrix().map(x -> x * 4_000_000_003L);
            final LongMatrix rightLong = right.toLongMatrix();

            assertParallelMatchesSequential("int " + label, () -> left.matrixMultiply(right));
            assertParallelMatchesSequential("long " + label, () -> leftLong.matrixMultiply(rightLong));
            assertParallelMatchesSequential("double " + label, () -> leftDouble.matrixMultiply(rightDouble));
            assertParallelMatchesSequential("float " + label, () -> leftFloat.matrixMultiply(rightFloat));
            assertParallelMatchesSequential("short " + label, () -> leftShort.matrixMultiply(rightShort));
            assertParallelMatchesSequential("byte " + label, () -> leftByte.matrixMultiply(rightByte));
            assertParallelMatchesSequential("char " + label, () -> leftChar.matrixMultiply(rightChar));
        }
    }

    @Test
    void parallelProductMatchesAPlainAscendingKDotProduct() {
        final Random random = new Random(28);
        final DoubleMatrix left = mixedMagnitudes(9, 31, random);
        final DoubleMatrix right = mixedMagnitudes(31, 150, random);
        final DoubleMatrix product = under(ParallelMode.FORCE_ON, () -> left.matrixMultiply(right));

        for (int i = 0; i < left.rowCount(); i++) {
            for (int j = 0; j < right.columnCount(); j++) {
                double sum = 0;

                for (int k = 0; k < left.columnCount(); k++) {
                    sum += left.get(i, k) * right.get(k, j);
                }

                assertEquals(Double.doubleToLongBits(sum), Double.doubleToLongBits(product.get(i, j)), "cell " + i + ", " + j);
            }
        }
    }

    /** Runs the block split and returns the blocks it handed out, as {fromRow, toRow, fromColumn, toColumn}. */
    private static List<int[]> blocksOf(final int rows, final int columns, final int elementBytes) {
        final List<int[]> blocks = Collections.synchronizedList(new ArrayList<>());
        final AtomicIntegerArray visits = new AtomicIntegerArray(rows * columns);

        Matrices.matrixMultiplyInParallel(rows, columns, elementBytes, (fromRow, toRow, fromColumn, toColumn) -> {
            blocks.add(new int[] { fromRow, toRow, fromColumn, toColumn });

            for (int i = fromRow; i < toRow; i++) {
                for (int j = fromColumn; j < toColumn; j++) {
                    visits.incrementAndGet(i * columns + j);
                }
            }
        });

        for (int cell = 0; cell < rows * columns; cell++) {
            assertEquals(1, visits.get(cell), rows + "x" + columns + " (" + elementBytes + "-byte elements) cell " + cell);
        }

        return blocks;
    }

    @Test
    void blockSplitCoversEveryResultCellExactlyOnceWithBandsNoNarrowerThanTwoCacheLines() {
        for (final int elementBytes : new int[] { 1, 2, 4, 8 }) {
            final int minBandWidth = Matrices.MATRIX_MULTIPLY_MIN_BAND_BYTES / elementBytes;

            for (final int[] shape : new int[][] { { 0, 5 }, { 5, 0 }, { 1, 1 }, { 100, 3 }, { 3, 100 }, { 2, 63 }, { 2, 64 }, { 2, 65 }, { 1, 129 },
                    { 7, 1000 }, { 64, 64 }, { 1, 100_000 }, { 63, 300 }, { 200, 7 } }) {
                for (final int[] block : blocksOf(shape[0], shape[1], elementBytes)) {
                    final int width = block[3] - block[2];
                    assertEquals(1, block[1] - block[0], "every block is a single result row");
                    assertTrue(width >= Math.min(minBandWidth, shape[1]), shape[0] + "x" + shape[1] + " band of " + width + " " + elementBytes + "-byte cells");
                }
            }
        }
    }

    @Test
    void productsWithFewResultRowsStillSplitIntoSeveralTasks() {
        final int target = Matrices.MATRIX_MULTIPLY_TARGET_TASK_COUNT;

        // A single result row of 64 doubles used to be one 64-column block, so all of a 1 x 100000 * 100000 x 64
        // product ran on one worker. It now gets 64 / 16 bands, the most that keep every band two cache lines wide.
        assertEquals(4, blocksOf(1, 64, Double.BYTES).size());
        assertEquals(2, blocksOf(1, 64, Integer.BYTES).size());
        assertEquals(1, blocksOf(1, 64, Byte.BYTES).size(), "a narrower band would share result cache lines with its neighbour");
        assertEquals(target, blocksOf(1, 100_000, Integer.BYTES).size());
        assertEquals(8 * Math.min((target - 1) / 8 + 1, 600 / 16), blocksOf(8, 600, Double.BYTES).size(), "8 rows, each split toward the target");
        assertEquals(8 * 4, blocksOf(8, 600, Byte.BYTES).size(), "600 bytes hold four 128-byte bands per row");
        assertEquals(2, blocksOf(2, 200, Byte.BYTES).size(), "200 bytes hold only one 128-byte band per row");

        // Products with at least the target number of rows keep one full-width task per row.
        assertEquals(target, blocksOf(target, 5, Double.BYTES).size());
        assertEquals(target + 1, blocksOf(target + 1, 1000, Double.BYTES).size());

        // A single block runs on the calling thread instead of a one-task parallel stream.
        final Thread caller = Thread.currentThread();
        final Thread[] runner = new Thread[1];
        Matrices.matrixMultiplyInParallel(1, 64, Byte.BYTES, (fromRow, toRow, fromColumn, toColumn) -> runner[0] = Thread.currentThread());
        assertSame(caller, runner[0]);
    }
}
