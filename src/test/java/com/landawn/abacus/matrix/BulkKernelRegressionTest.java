package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * Regression tests for the 2026-11-02 review: branch-free boolean kernels, the bounded unchecked kernel of
 * {@code LongMatrix.matrixMultiplyExact}, the direct char and object rows of {@code appendTo}, the row-hoisted
 * {@code Matrices} array-callback zips, and the common element type of the deprecated type-inferring zips.
 */
class BulkKernelRegressionTest extends MatrixTestSupport {

    @SuppressWarnings("unchecked")
    private static <T> T under(final ParallelMode mode, final Supplier<T> supplier) {
        final Object[] result = new Object[1];
        Matrices.runWithParallelMode(mode, () -> result[0] = supplier.get());
        return (T) result[0];
    }

    private static BooleanMatrix randomBooleans(final int rows, final int columns, final Random random, final double trueProbability) {
        final boolean[][] values = new boolean[rows][columns];

        for (final boolean[] row : values) {
            for (int j = 0; j < columns; j++) {
                row[j] = random.nextDouble() < trueProbability;
            }
        }

        return BooleanMatrix.wrap(values);
    }

    // ---- BooleanMatrix and / or / countTrue ----

    @Test
    void andOrMatchTheLogicalOperatorsInEveryModeAndShape() {
        final BooleanMatrix left = BooleanMatrix.wrap(new boolean[][] { { true, true, false, false } });
        final BooleanMatrix right = BooleanMatrix.wrap(new boolean[][] { { true, false, true, false } });

        for (final ParallelMode mode : new ParallelMode[] { ParallelMode.FORCE_OFF, ParallelMode.FORCE_ON }) {
            assertEquals(BooleanMatrix.wrap(new boolean[][] { { true, false, false, false } }), under(mode, () -> left.and(right)), mode.name());
            assertEquals(BooleanMatrix.wrap(new boolean[][] { { true, true, true, false } }), under(mode, () -> left.or(right)), mode.name());
        }

        final Random random = new Random(27);

        for (final int[] shape : new int[][] { { 0, 0 }, { 0, 4 }, { 4, 0 }, { 1, 1 }, { 3, 70 }, { 70, 3 }, { 33, 33 } }) {
            final BooleanMatrix a = randomBooleans(shape[0], shape[1], random, 0.5);
            final BooleanMatrix b = randomBooleans(shape[0], shape[1], random, 0.5);
            final BooleanMatrix expectedAnd = a.zipWith(b, (x, y) -> x && y);
            final BooleanMatrix expectedOr = a.zipWith(b, (x, y) -> x || y);

            for (final ParallelMode mode : new ParallelMode[] { ParallelMode.FORCE_OFF, ParallelMode.FORCE_ON }) {
                final String label = shape[0] + "x" + shape[1] + " " + mode;
                assertEquals(expectedAnd, under(mode, () -> a.and(b)), "and " + label);
                assertEquals(expectedOr, under(mode, () -> a.or(b)), "or " + label);
                assertEquals(a, under(mode, () -> a.and(a)), "self and " + label);
                assertEquals(a, under(mode, () -> a.or(a)), "self or " + label);
            }
        }
    }

    @Test
    void countTrueMatchesAnElementByElementCount() {
        final Random random = new Random(28);

        for (final int[] shape : new int[][] { { 0, 0 }, { 0, 5 }, { 5, 0 }, { 1, 1 }, { 7, 300 }, { 300, 7 }, { 64, 64 } }) {
            for (final double p : new double[] { 0, 0.05, 0.5, 0.95, 1 }) {
                final BooleanMatrix m = randomBooleans(shape[0], shape[1], random, p);
                final long[] expected = { 0 };
                m.forEach(value -> {
                    if (value) {
                        expected[0]++;
                    }
                });

                assertEquals(expected[0], m.countTrue(), shape[0] + "x" + shape[1] + " p=" + p);
            }
        }

        final boolean[][] allTrue = new boolean[3][100_000];

        for (final boolean[] row : allTrue) {
            Arrays.fill(row, true);
        }

        assertEquals(300_000L, BooleanMatrix.wrap(allTrue).countTrue());
    }

    // ---- LongMatrix.matrixMultiplyExact ----

    /** Exact reference: the product, or null if a product or an ascending-k partial sum of some cell leaves the long range. */
    private static long[][] referenceExactProduct(final long[][] left, final long[][] right, final int inner, final int columns) {
        final BigInteger min = BigInteger.valueOf(Long.MIN_VALUE);
        final BigInteger max = BigInteger.valueOf(Long.MAX_VALUE);
        final long[][] result = new long[left.length][columns];

        for (int i = 0; i < left.length; i++) {
            for (int j = 0; j < columns; j++) {
                BigInteger sum = BigInteger.ZERO;

                for (int k = 0; k < inner; k++) {
                    final BigInteger product = BigInteger.valueOf(left[i][k]).multiply(BigInteger.valueOf(right[k][j]));
                    sum = sum.add(product);

                    if (product.compareTo(min) < 0 || product.compareTo(max) > 0 || sum.compareTo(min) < 0 || sum.compareTo(max) > 0) {
                        return null;
                    }
                }

                result[i][j] = sum.longValueExact();
            }
        }

        return result;
    }

    private static void assertExactProductMatchesReference(final String label, final long[][] left, final long[][] right, final int inner, final int columns) {
        final long[][] expected = referenceExactProduct(left, right, inner, columns);
        // wrap canonicalizes a rowless array to 0 x 0, while random keeps the 0 x N shape the product needs.
        final LongMatrix l = left.length == 0 ? LongMatrix.random(0, inner, new Random(0)) : LongMatrix.wrap(left);
        final LongMatrix r = inner == 0 ? LongMatrix.random(0, columns, new Random(0)) : LongMatrix.wrap(right);

        if (expected == null) {
            assertThrows(ArithmeticException.class, () -> l.matrixMultiplyExact(r), label);
        } else {
            final LongMatrix actual = l.matrixMultiplyExact(r);
            assertEquals(left.length, actual.rowCount(), label);
            assertEquals(columns, actual.columnCount(), label);

            for (int i = 0; i < expected.length; i++) {
                assertArrayEquals(expected[i], actual.rowView(i), label + " row " + i);
            }
        }
    }

    @Test
    void matrixMultiplyExactAgreesWithBigIntegerOnBothSidesOfTheNoOverflowBound() {
        final long boundFactor = Long.MAX_VALUE / 49; // 49 divides Long.MAX_VALUE, so 7 * 7 * boundFactor == Long.MAX_VALUE

        // Exactly on the bound (inner 7, |left| <= 7, |right| <= boundFactor): the unchecked kernel is used and must be exact.
        final long[][] sevens = { { 7, 7, 7, 7, 7, 7, 7 }, { -7, -7, -7, -7, -7, -7, -7 } };
        final long[][] onBound = new long[7][2];

        for (final long[] row : onBound) {
            row[0] = boundFactor;
            row[1] = -boundFactor;
        }

        assertExactProductMatchesReference("on the bound", sevens, onBound, 7, 2);
        assertEquals(Long.MAX_VALUE, LongMatrix.wrap(sevens).matrixMultiplyExact(LongMatrix.wrap(onBound)).get(0, 0));

        // One past the bound: the sum overflows, so the checked kernel must still throw.
        final long[][] pastBound = new long[7][2];

        for (final long[] row : pastBound) {
            row[0] = boundFactor + 1;
            row[1] = 1;
        }

        assertThrows(ArithmeticException.class, () -> LongMatrix.wrap(sevens).matrixMultiplyExact(LongMatrix.wrap(pastBound)));

        // Each product fits, only the sum overflows; then a cancelling term that a wider accumulator would accept.
        final long half = Long.MAX_VALUE / 2 + 1;
        assertThrows(ArithmeticException.class,
                () -> LongMatrix.wrap(new long[][] { { half, half } }).matrixMultiplyExact(LongMatrix.wrap(new long[][] { { 1, 1 }, { 1, 1 } })));
        assertThrows(ArithmeticException.class, () -> LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, 1, -1 } })
                .matrixMultiplyExact(LongMatrix.wrap(new long[][] { { 1, 1 }, { 1, 1 }, { 1, 1 } })));

        // Long.MIN_VALUE has no positive magnitude, so it must keep the checked kernel on either side.
        assertExactProductMatchesReference("MIN on the left", new long[][] { { Long.MIN_VALUE, 0 } }, new long[][] { { 1, 1 }, { 0, 0 } }, 2, 2);
        assertThrows(ArithmeticException.class,
                () -> LongMatrix.wrap(new long[][] { { Long.MIN_VALUE, 0 } }).matrixMultiplyExact(LongMatrix.wrap(new long[][] { { -1, 1 }, { 0, 0 } })));
        assertThrows(ArithmeticException.class,
                () -> LongMatrix.wrap(new long[][] { { -1, 0 } }).matrixMultiplyExact(LongMatrix.wrap(new long[][] { { 1, Long.MIN_VALUE }, { 0, 0 } })));
    }

    @Test
    void matrixMultiplyExactAgreesWithBigIntegerOnRandomMagnitudes() {
        final Random random = new Random(29);
        final long[] specials = { 0, 1, -1, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE / 2, Long.MIN_VALUE / 2, 1L << 31, -(1L << 32), 3037000499L,
                -3037000500L };

        for (int trial = 0; trial < 4000; trial++) {
            final int rows = random.nextInt(4);
            final int inner = random.nextInt(5);
            final int columns = random.nextInt(4);
            final int magnitude = random.nextInt(5);
            final long[][] left = new long[rows][inner];
            final long[][] right = new long[inner][columns];

            for (final long[][] side : new long[][][] { left, right }) {
                for (final long[] row : side) {
                    for (int j = 0; j < row.length; j++) {
                        row[j] = switch (magnitude) {
                            case 0 -> random.nextInt(2001) - 1000;
                            case 1 -> random.nextInt();
                            case 2 -> random.nextLong() >> 20;
                            case 3 -> random.nextLong();
                            default -> specials[random.nextInt(specials.length)];
                        };
                    }
                }
            }

            assertExactProductMatchesReference("trial " + trial, left, right, inner, columns);
        }
    }

    // ---- appendTo ----

    /** Records every append as one token, so the exact call sequence (and therefore partial output) can be compared. */
    private static final class RecordingAppendable implements Appendable {
        final List<String> tokens = new ArrayList<>();
        int remainingBeforeFailure;

        RecordingAppendable(final int failAfter) {
            remainingBeforeFailure = failAfter;
        }

        private Appendable record(final String token) throws IOException {
            if (remainingBeforeFailure-- == 0) {
                throw new IOException("append " + tokens.size());
            }

            tokens.add(token);
            return this;
        }

        @Override
        public Appendable append(final CharSequence csq) throws IOException {
            return record("s:" + csq);
        }

        @Override
        public Appendable append(final CharSequence csq, final int start, final int end) throws IOException {
            return record("s:" + csq.subSequence(start, end));
        }

        @Override
        public Appendable append(final char c) throws IOException {
            return record("c:" + c);
        }
    }

    /** The element-by-element rendering appendTo used for every char and object row before the direct loops. */
    private static void appendElementByElement(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final Appendable output, final Object[][] cells) throws IOException {
        if (cells.length == 0) {
            output.append("[]");
            return;
        }

        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                output.append(AbstractMatrix.ARRAY_PRINT_SEPARATOR);
            }

            output.append('[');

            for (int j = 0; j < cells[i].length; j++) {
                if (j > 0) {
                    output.append(", ");
                }

                matrix.appendElementForOutput(output, cells[i][j]);
            }

            output.append(']');
        }
    }

    @Test
    void charAndObjectRowsRenderWithTheSameAppendCallsAsTheElementByElementPath() throws IOException {
        final char[][] everyCodeUnit = new char[256][256];

        for (int c = 0; c <= Character.MAX_VALUE; c++) {
            everyCodeUnit[c >>> 8][c & 0xff] = (char) c;
        }

        final CharMatrix chars = CharMatrix.wrap(everyCodeUnit);
        final StringBuilder rendered = new StringBuilder();
        chars.appendTo(rendered);
        assertEquals(chars.toMultilineString(), rendered.toString());

        final CharMatrix smallChars = CharMatrix.wrap(new char[][] { { 'a', '\n', (char) 0 }, { '\'', '\\', (char) 0xd800 } });
        final Matrix<Object> objects = Matrix.wrap(Object.class, new Object[][] { { 1, null, "x" }, { new int[] { 2 }, 3.5, 'c' } });

        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : List.<AbstractMatrix<?, ?, ?, ?, ?>> of(smallChars, objects, CharMatrix.empty(),
                CharMatrix.wrap(new char[2][0]))) {
            final Object[][] cells = new Object[matrix.rowCount()][matrix.columnCount()];

            for (int i = 0; i < cells.length; i++) {
                for (int j = 0; j < cells[i].length; j++) {
                    cells[i][j] = matrix instanceof CharMatrix cm ? (Object) cm.get(i, j) : objects.get(i, j);
                }
            }

            final RecordingAppendable expected = new RecordingAppendable(-1);
            appendElementByElement(matrix, expected, cells);

            for (int failAfter = -1; failAfter <= expected.tokens.size(); failAfter++) {
                final RecordingAppendable actual = new RecordingAppendable(failAfter);

                if (failAfter < 0 || failAfter >= expected.tokens.size()) {
                    matrix.appendTo(actual);
                    assertEquals(expected.tokens, actual.tokens);
                } else {
                    final UncheckedIOException failure = assertThrows(UncheckedIOException.class, () -> matrix.appendTo(actual));
                    assertEquals("append " + failAfter, failure.getCause().getMessage());
                    assertEquals(expected.tokens.subList(0, failAfter), actual.tokens, "partial output before append " + failAfter);
                }
            }
        }
    }

    // ---- Matrices array-callback zips ----

    private static final class CallLog {
        final Set<Object> distinctArrays = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<String> contents = new ArrayList<>();

        void record(final Object array, final String content) {
            distinctArrays.add(array);
            contents.add(content);
        }
    }

    private static void assertCallLog(final String label, final CallLog log, final boolean shared, final List<String> expectedContents) {
        assertEquals(expectedContents, log.contents, label + " visits cells in row-major order with every input's value");
        assertEquals(shared ? 1 : expectedContents.size(), log.distinctArrays.size(), label + (shared ? " reuses one array" : " gets a fresh array per cell"));
    }

    @Test
    void arrayCallbackZipsVisitRowMajorAndHonourTheSharingFlag() {
        final IntMatrix base = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        final List<IntMatrix> ints = List.of(base, base.map(x -> x * 10), base.map(x -> -x));
        final List<String> expectedContents = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                final int v = base.get(i, j);
                expectedContents.add(v + "," + v * 10 + "," + -v);
            }
        }

        final IntMatrix expectedSums = base.map(x -> x * 10);
        final List<ByteMatrix> bytes = List.of(ints.get(0).mapToObj(x -> (byte) x, Byte.class).mapToByte(x -> x),
                ints.get(1).mapToObj(x -> (byte) x, Byte.class).mapToByte(x -> x), ints.get(2).mapToObj(x -> (byte) x, Byte.class).mapToByte(x -> x));
        final List<LongMatrix> longs = List.of(ints.get(0).toLongMatrix(), ints.get(1).toLongMatrix(), ints.get(2).toLongMatrix());
        final List<DoubleMatrix> doubles = List.of(ints.get(0).toDoubleMatrix(), ints.get(1).toDoubleMatrix(), ints.get(2).toDoubleMatrix());

        for (final boolean shared : new boolean[] { false, true }) {
            final String mode = shared ? " shared" : " unshared";

            final CallLog byteToInt = new CallLog();
            assertEquals(expectedSums, Matrices.zipToInt(bytes, arr -> {
                byteToInt.record(arr, arr[0] + "," + arr[1] + "," + arr[2]);
                return arr[0] + arr[1] + arr[2];
            }, shared), "byte->int" + mode);
            assertCallLog("byte->int" + mode, byteToInt, shared, expectedContents);

            final CallLog byteToObj = new CallLog();
            assertEquals(expectedSums.boxed(), Matrices.zipToObj(bytes, arr -> {
                byteToObj.record(arr, arr[0] + "," + arr[1] + "," + arr[2]);
                return arr[0] + arr[1] + arr[2];
            }, shared, Integer.class), "byte->obj" + mode);
            assertCallLog("byte->obj" + mode, byteToObj, shared, expectedContents);

            final CallLog intToLong = new CallLog();
            assertEquals(expectedSums.toLongMatrix(), Matrices.zipToLong(ints, arr -> {
                intToLong.record(arr, arr[0] + "," + arr[1] + "," + arr[2]);
                return (long) arr[0] + arr[1] + arr[2];
            }, shared), "int->long" + mode);
            assertCallLog("int->long" + mode, intToLong, shared, expectedContents);

            final CallLog intToDouble = new CallLog();
            assertEquals(expectedSums.toDoubleMatrix(), Matrices.zipToDouble(ints, arr -> {
                intToDouble.record(arr, arr[0] + "," + arr[1] + "," + arr[2]);
                return (double) arr[0] + arr[1] + arr[2];
            }, shared), "int->double" + mode);
            assertCallLog("int->double" + mode, intToDouble, shared, expectedContents);

            final CallLog intToObj = new CallLog();
            assertEquals(expectedSums.boxed(), Matrices.zipToObj(ints, arr -> {
                intToObj.record(arr, arr[0] + "," + arr[1] + "," + arr[2]);
                return arr[0] + arr[1] + arr[2];
            }, shared, Integer.class), "int->obj" + mode);
            assertCallLog("int->obj" + mode, intToObj, shared, expectedContents);

            final CallLog longToDouble = new CallLog();
            assertEquals(expectedSums.toDoubleMatrix(), Matrices.zipToDouble(longs, arr -> {
                longToDouble.record(arr, arr[0] + "," + arr[1] + "," + arr[2]);
                return (double) (arr[0] + arr[1] + arr[2]);
            }, shared), "long->double" + mode);
            assertCallLog("long->double" + mode, longToDouble, shared, expectedContents);

            final CallLog longToObj = new CallLog();
            assertEquals(expectedSums.toLongMatrix().boxed(), Matrices.zipToObj(longs, arr -> {
                longToObj.record(arr, arr[0] + "," + arr[1] + "," + arr[2]);
                return arr[0] + arr[1] + arr[2];
            }, shared, Long.class), "long->obj" + mode);
            assertCallLog("long->obj" + mode, longToObj, shared, expectedContents);

            final CallLog doubleToObj = new CallLog();
            assertEquals(expectedSums.toDoubleMatrix().boxed(), Matrices.zipToObj(doubles, arr -> {
                doubleToObj.record(arr, (int) arr[0] + "," + (int) arr[1] + "," + (int) arr[2]);
                return arr[0] + arr[1] + arr[2];
            }, shared, Double.class), "double->obj" + mode);
            assertCallLog("double->obj" + mode, doubleToObj, shared, expectedContents);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void genericArrayCallbackZipKeepsTheCallbackTypeWithInputsOfDifferentRowTypes() {
        // A legacy covariant Matrix<Number> keeps Integer[] / Double[] rows; the hoisted row buffer must accept both.
        final Matrix<Number> integers = new Matrix<>(new Integer[][] { { 1, 2 }, { 3, 4 } });
        final Matrix<Number> doubles = new Matrix<>(new Double[][] { { 0.5, 1.5 }, { 2.5, 3.5 } });
        final List<Matrix<Number>> inputs = List.of(integers, doubles);

        for (final boolean shared : new boolean[] { false, true }) {
            final CallLog log = new CallLog();
            final Matrix<String> zipped = Matrices.zip(inputs, arr -> {
                assertSame(Number[].class, arr.getClass());
                log.record(arr, arr[0] + "+" + arr[1]);
                return arr[0] + "+" + arr[1];
            }, shared, Number.class, String.class);

            assertEquals(Matrix.wrap(String.class, new String[][] { { "1+0.5", "2+1.5" }, { "3+2.5", "4+3.5" } }), zipped);
            assertCallLog("generic" + (shared ? " shared" : " unshared"), log, shared, List.of("1+0.5", "2+1.5", "3+2.5", "4+3.5"));
        }
    }

    @Test
    void arrayCallbackZipsStopAtTheFirstFailingCellInRowMajorOrder() {
        final IntMatrix base = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        final List<IntMatrix> ints = List.of(base, base);
        final List<Integer> visited = new ArrayList<>();

        final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> Matrices.zipToLong(ints, arr -> {
            visited.add(arr[0]);

            if (arr[0] == 4) {
                throw new IllegalStateException("cell 4");
            }

            return (long) arr[0];
        }));

        assertEquals("cell 4", failure.getMessage());
        assertEquals(List.of(1, 2, 3, 4), visited);
        assertThrows(NullPointerException.class, () -> Matrices.zipToDouble(ints, arr -> arr[0] == 5 ? null : 1.0, false));
    }

    // ---- common element type of the deprecated type-inferring zips ----

    interface Marker {
    }

    static class Base implements Marker {
    }

    static class Middle extends Base {
    }

    static class Lower extends Middle {
    }

    static final class Leaf extends Lower implements Marker {
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
    void commonElementTypeIsAnInputTypeThatIsAssignableFromEveryOtherInput() {
        // Leaf reaches Marker in one step and Base in three, so the distance ranking alone prefers Marker, an
        // interface wider than both inputs. The two-type resolution returns Base, and the n-ary one must agree.
        assertEquals(Base.class, Matrices.resolveCommonAssignableType(Leaf.class, Base.class));
        assertEquals(Base.class, Matrices.resolveCommonElementType(new Matrix[] { Matrix.empty(Leaf.class), Matrix.empty(Base.class) }));
        assertEquals(Base.class, Matrices.resolveCommonElementType(new Matrix[] { Matrix.empty(Base.class), Matrix.empty(Leaf.class) }));
        assertEquals(Base.class,
                Matrices.resolveCommonElementType(new Matrix[] { Matrix.empty(Leaf.class), Matrix.empty(Base.class), Matrix.empty(Middle.class) }));
        assertEquals(Base[].class, Matrices.resolveCommonElementType(new Matrix[] { Matrix.empty(Leaf[].class), Matrix.empty(Base[].class) }));
        assertEquals(Leaf.class, Matrices.resolveCommonElementType(new Matrix[] { Matrix.empty(Leaf.class), Matrix.empty(Leaf.class) }));

        final Base[] baseRow = { new Base(), new Leaf() };
        final Matrix<Base> leaves = new Matrix<>(new Leaf[][] { { new Leaf(), new Leaf() } });
        final Matrix<Base> bases = new Matrix<>(Base.class, new Base[][] { baseRow });
        final Matrix<Base> zipped = Matrices.zip(List.of(leaves, bases), (x, y) -> y);

        // Before the fix the result rows were Marker[], so this assignment threw ClassCastException.
        final Base[] row = zipped.rowCopy(0);
        assertArrayEquals(baseRow, row);
    }
}
