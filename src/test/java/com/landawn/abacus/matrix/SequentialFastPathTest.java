package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * The element-wise operations (map, mapToXxx, zipWith, index-based updateAll/replaceIf and the Matrices zips) and
 * appendTo have direct sequential loops next to their parallel paths. These tests pin that the sequential loops
 * give the same results as the parallel path, visit cells in row-major order, keep completed updates when a callback
 * fails, and render exactly what the generic element-by-element path rendered.
 */
class SequentialFastPathTest extends MatrixTestSupport {

    private static final int[][] SHAPES = { { 0, 0 }, { 0, 3 }, { 3, 0 }, { 1, 1 }, { 1, 4 }, { 4, 1 }, { 3, 5 }, { 5, 3 } };

    @SuppressWarnings("unchecked")
    private static <T> T under(final ParallelMode mode, final Supplier<T> supplier) {
        final Object[] result = new Object[1];
        Matrices.runWithParallelMode(mode, () -> result[0] = supplier.get());
        return (T) result[0];
    }

    private static void assertSequentialMatchesParallel(final String label, final Supplier<?> operation) {
        assertEquals(under(ParallelMode.FORCE_ON, operation), under(ParallelMode.FORCE_OFF, operation), label);
    }

    @Test
    void elementwiseOperationsMatchParallelResultsForEveryVariantAndShape() {
        for (final int[] shape : SHAPES) {
            final int r = shape[0];
            final int c = shape[1];
            final String s = r + "x" + c;
            final IntMatrix im = IntMatrix.random(r, c, new Random(r * 31L + c));
            final IntMatrix im2 = IntMatrix.random(r, c, new Random(r * 37L + c));
            final IntMatrix im3 = IntMatrix.random(r, c, new Random(r * 41L + c));
            final LongMatrix lm = im.toLongMatrix();
            final FloatMatrix fm = im.toFloatMatrix();
            final DoubleMatrix dm = im.toDoubleMatrix();
            final ShortMatrix sm = im.mapToObj(x -> (short) x, Short.class).mapToShort(x -> x);
            final ByteMatrix bm = im.mapToObj(x -> (byte) x, Byte.class).mapToByte(x -> x);
            final CharMatrix cm = im.mapToObj(x -> (char) x, Character.class).mapToChar(x -> x);
            final BooleanMatrix zm = im.mapToObj(x -> (x & 1) == 0, Boolean.class).mapToBoolean(x -> x);
            final Matrix<String> om = im.mapToObj(x -> "s" + (x & 15), String.class);

            assertSequentialMatchesParallel(s + " int map", () -> im.map(x -> x ^ 0x5a5a));
            assertSequentialMatchesParallel(s + " int mapToLong", () -> im.mapToLong(x -> (long) x * 3));
            assertSequentialMatchesParallel(s + " int mapToDouble", () -> im.mapToDouble(x -> x / 7.0));
            assertSequentialMatchesParallel(s + " int mapToObj", () -> im.mapToObj(x -> "v" + (x & 7), String.class));
            assertSequentialMatchesParallel(s + " int zipWith", () -> im.zipWith(im2, (x, y) -> x - y));
            assertSequentialMatchesParallel(s + " int zipWith3", () -> im.zipWith(im2, im3, (x, y, z) -> x + y * z));
            assertSequentialMatchesParallel(s + " long",
                    () -> List.of(lm.map(x -> x * 7), lm.zipWith(lm, (x, y) -> x ^ y), lm.zipWith(lm, lm, (x, y, z) -> x + y - z),
                            lm.mapToInt(x -> (int) (x >> 3)), lm.mapToDouble(x -> x * 0.5), lm.mapToObj(x -> x % 3, Long.class)));
            assertSequentialMatchesParallel(s + " float",
                    () -> List.of(fm.map(x -> x / 3), fm.zipWith(fm, (x, y) -> x * y), fm.zipWith(fm, fm, (x, y, z) -> x - y + z),
                            fm.mapToInt(x -> (int) x >> 2), fm.mapToLong(x -> (long) x), fm.mapToDouble(x -> x * 2.0), fm.mapToObj(x -> x > 0, Boolean.class)));
            assertSequentialMatchesParallel(s + " double",
                    () -> List.of(dm.map(x -> x / 3), dm.zipWith(dm, (x, y) -> x * y), dm.zipWith(dm, dm, (x, y, z) -> x - y + z),
                            dm.mapToInt(x -> (int) x >> 2), dm.mapToLong(x -> (long) x), dm.mapToObj(x -> x > 0, Boolean.class)));
            assertSequentialMatchesParallel(s + " short", () -> List.of(sm.map(x -> (short) (x * 3)), sm.zipWith(sm, (x, y) -> (short) (x - y)),
                    sm.zipWith(sm, sm, (x, y, z) -> (short) (x ^ y ^ z)), sm.mapToObj(x -> x, Short.class)));
            assertSequentialMatchesParallel(s + " byte", () -> List.of(bm.map(x -> (byte) (x * 3)), bm.zipWith(bm, (x, y) -> (byte) (x - y)),
                    bm.zipWith(bm, bm, (x, y, z) -> (byte) (x ^ y ^ z)), bm.mapToObj(x -> x, Byte.class)));
            assertSequentialMatchesParallel(s + " char", () -> List.of(cm.map(x -> (char) (x + 1)), cm.zipWith(cm, (x, y) -> (char) (x ^ y)),
                    cm.zipWith(cm, cm, (x, y, z) -> (char) (x + y + z)), cm.mapToObj(x -> (int) x, Integer.class)));
            assertSequentialMatchesParallel(s + " boolean", () -> List.of(zm.map(x -> !x), zm.zipWith(zm, (x, y) -> x ^ y),
                    zm.zipWith(zm, zm, (x, y, z) -> x && y || z), zm.mapToObj(x -> x ? 1 : 0, Integer.class)));
            assertSequentialMatchesParallel(s + " object",
                    () -> List.of(om.map(x -> x + "!", String.class), om.zipWith(om, (x, y) -> x + y, String.class),
                            om.zipWith(om, om, (x, y, z) -> x + y + z, String.class), om.mapToInt(String::length), om.mapToLong(String::hashCode),
                            om.mapToDouble(x -> x.length() / 2.0), om.mapToFloat(x -> x.length()), om.mapToBoolean(x -> x.endsWith("1")),
                            om.mapToByte(x -> (byte) x.length()), om.mapToShort(x -> (short) x.charAt(1)), om.mapToChar(x -> x.charAt(1))));
            assertSequentialMatchesParallel(s + " Matrices zips",
                    () -> List.of(Matrices.zip(List.of(im, im2, im3, im), (x, y) -> x * 31 + y), Matrices.zip(List.of(lm, lm, lm), (x, y) -> x * 3 - y),
                            Matrices.zip(List.of(dm, dm, dm), (x, y) -> x * 0.5 + y), Matrices.zip(List.of(bm, bm, bm), (x, y) -> (byte) (x * 3 + y)),
                            Matrices.zip(List.of(om, om, om), (x, y) -> x + y, String.class), Matrices.zipToInt(bm, bm, (x, y) -> x * y),
                            Matrices.zipToInt(bm, bm, bm, (x, y, z) -> x * y + z), Matrices.zipToLong(im, im2, (x, y) -> (long) x * y),
                            Matrices.zipToLong(im, im2, im3, (x, y, z) -> (long) x * y - z), Matrices.zipToDouble(im, im2, (x, y) -> x * 1.5 + y),
                            Matrices.zipToDouble(im, im2, im3, (x, y, z) -> x + y + (double) z), Matrices.zipToDouble(lm, lm, (x, y) -> x / 2.0 + y),
                            Matrices.zipToDouble(lm, lm, lm, (x, y, z) -> x + y * 0.5 + z)));
            assertSequentialMatchesParallel(s + " index updates", () -> {
                final IntMatrix u = im.copy();
                u.updateAll((i, j) -> i * 100 + j);
                u.replaceIf((i, j) -> j == 1, -5);
                final Matrix<String> uo = om.copy();
                uo.updateAll((i, j) -> i + ":" + j);
                uo.replaceIf((i, j) -> i == j, "d");
                final BooleanMatrix uz = zm.copy();
                uz.updateAll((i, j) -> (i ^ j) == 1);
                uz.replaceIf((i, j) -> j == 0, true);
                final DoubleMatrix ud = dm.copy();
                ud.updateAll((i, j) -> i / (j + 1.0));
                ud.replaceIf((i, j) -> i == 0, -0.0);
                return List.of(u, uo, uz, ud);
            });
        }
    }

    @Test
    void sequentialCallbacksRunInRowMajorOrder() {
        final IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        final IntMatrix other = IntMatrix.wrap(new int[][] { { 10, 20, 30 }, { 40, 50, 60 } });
        final List<String> expectedPositions = List.of("0,0", "0,1", "0,2", "1,0", "1,1", "1,2");
        final List<Integer> expectedValues = List.of(1, 2, 3, 4, 5, 6);

        final List<String> positions = new ArrayList<>();
        m.copy().updateAll((i, j) -> {
            positions.add(i + "," + j);
            return 0;
        });
        assertEquals(expectedPositions, positions);

        positions.clear();
        m.copy().replaceIf((i, j) -> positions.add(i + "," + j), 0);
        assertEquals(expectedPositions, positions);

        final List<Integer> values = new ArrayList<>();
        m.map(x -> {
            values.add(x);
            return x;
        });
        assertEquals(expectedValues, values);

        values.clear();
        m.zipWith(other, (x, y) -> {
            values.add(x);
            return y;
        });
        assertEquals(expectedValues, values);

        values.clear();
        m.boxed().mapToLong(x -> {
            values.add(x);
            return x;
        });
        assertEquals(expectedValues, values);

        // The collection zip folds each cell's inputs in collection order before moving to the next cell.
        final List<String> folds = new ArrayList<>();
        final IntMatrix zipped = Matrices.zip(List.of(m, other, m), (x, y) -> {
            folds.add(x + "+" + y);
            return x + y;
        });
        assertEquals(List.of("1+10", "11+1", "2+20", "22+2", "3+30", "33+3", "4+40", "44+4", "5+50", "55+5", "6+60", "66+6"), folds);
        assertEquals(IntMatrix.wrap(new int[][] { { 12, 24, 36 }, { 48, 60, 72 } }), zipped);
    }

    @Test
    void sequentialIndexUpdatesKeepTheRowMajorPrefixWhenTheCallbackFails() {
        final IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
        final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> m.updateAll((i, j) -> {
            if (i == 1 && j == 1) {
                throw new IllegalStateException("stop");
            }
            return 0;
        }));
        assertEquals("stop", failure.getMessage());
        assertEquals(IntMatrix.wrap(new int[][] { { 0, 0, 0 }, { 0, 5, 6 }, { 7, 8, 9 } }), m);

        final Matrix<String> om = Matrix.wrap(String.class, new String[][] { { "a", "b" }, { "c", "d" } });
        assertThrows(IllegalStateException.class, () -> om.replaceIf((i, j) -> {
            if (i == 1) {
                throw new IllegalStateException();
            }
            return true;
        }, "x"));
        assertEquals(Matrix.wrap(String.class, new String[][] { { "x", "x" }, { "c", "d" } }), om);
    }

    @Test
    void sequentialMapRethrowsCheckedCallbackExceptionsUnwrapped() {
        final DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1, 2 }, { 3, 4 } });
        final IOException failure = new IOException("io");
        assertSame(failure, assertThrows(IOException.class, () -> m.map(x -> {
            if (x == 3) {
                throw failure;
            }
            return x;
        })));
        assertSame(failure, assertThrows(IOException.class, () -> m.zipWith(m, (x, y) -> {
            throw failure;
        })));
    }

    @Test
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    void sequentialObjectCollectionZipAcceptsInputsWhoseRowArraysHaveDifferentRuntimeTypes() {
        // The deprecated overload infers a common element type (Number here), so the inputs keep Integer[] and Long[]
        // rows; the sequential fold buffers them side by side, which a buffer typed by the first input would reject.
        final Matrix<Number> ints = (Matrix) Matrix.wrap(Integer.class, new Integer[][] { { 1, 2 }, { 3, 4 } });
        final Matrix<Number> longs = (Matrix) Matrix.wrap(Long.class, new Long[][] { { 10L, 20L }, { 30L, 40L } });
        final Supplier<Matrix<Number>> zip = () -> Matrices.zip(List.of(ints, longs, ints), (x, y) -> x.longValue() + y.longValue());

        final Matrix<Number> sequential = under(ParallelMode.FORCE_OFF, zip);
        assertEquals(under(ParallelMode.FORCE_ON, zip), sequential);
        assertEquals(12L, sequential.get(0, 0));
        assertEquals(48L, sequential.get(1, 1));
    }

    @Test
    void appendToPrimitiveRowsRendersTheSameTextAsToMultilineString() {
        final List<AbstractMatrix<?, ?, ?, ?, ?>> matrices = List.of(
                DoubleMatrix.wrap(new double[][] { { Double.NaN, -0.0, 1e10, Double.MIN_VALUE }, { Double.NEGATIVE_INFINITY, 1e-7, 100.0, 0.1 } }),
                FloatMatrix.wrap(new float[][] { { Float.NaN, -0.0f, 1e10f, Float.MIN_VALUE, 1.1f, 16777217f } }),
                LongMatrix.wrap(new long[][] { { Long.MIN_VALUE, Long.MAX_VALUE }, { 0, -1 } }),
                IntMatrix.wrap(new int[][] { { Integer.MIN_VALUE, Integer.MAX_VALUE, 0 } }), ShortMatrix.wrap(new short[][] { { -32768, 32767 } }),
                ByteMatrix.wrap(new byte[][] { { -128, 127 } }), BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } }),
                CharMatrix.wrap(new char[][] { { '\n', 'a', (char) 39, (char) 0 } }),
                Matrix.wrap(Object.class, new Object[][] { { null, new int[] { 1, 2 }, "x", 1.5f } }), IntMatrix.wrap(new int[3][0]), IntMatrix.empty());

        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices) {
            final StringBuilder sb = new StringBuilder();
            matrix.appendTo(sb);
            assertEquals(matrix.toMultilineString(), sb.toString());
        }

        final StringBuilder sb = new StringBuilder();
        DoubleMatrix.wrap(new double[][] { { 1, -0.0 }, { Double.NaN, 2.5 } }).appendTo(sb);
        assertEquals("[1.0, -0.0]\n[NaN, 2.5]", sb.toString());
    }
}
