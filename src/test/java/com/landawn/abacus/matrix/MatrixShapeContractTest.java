package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.util.Sheet.Point;

/** Shared contracts intentionally exercised across matrix implementations. */
class MatrixShapeContractTest extends MatrixTestSupport {

    private static java.util.stream.Stream<Arguments> emptyMatrices() {
        return java.util.stream.Stream.of(Arguments.of("boolean", BooleanMatrix.wrap(new boolean[0][0])), Arguments.of("byte", ByteMatrix.wrap(new byte[0][0])),
                Arguments.of("char", CharMatrix.wrap(new char[0][0])), Arguments.of("short", ShortMatrix.wrap(new short[0][0])),
                Arguments.of("int", IntMatrix.wrap(new int[0][0])), Arguments.of("long", LongMatrix.wrap(new long[0][0])),
                Arguments.of("float", FloatMatrix.wrap(new float[0][0])), Arguments.of("double", DoubleMatrix.wrap(new double[0][0])),
                Arguments.of("object", Matrix.wrap(new Integer[0][0])));
    }

    @Nested
    class ShapeIdentityTests {

        @ParameterizedTest(name = "{0}: equality and hashing retain degenerate dimensions")
        @MethodSource("com.landawn.abacus.matrix.MatrixShapeContractTest#emptyMatrices")
        public void testHashCode_DegenerateShapesPreserveEqualityAndHashing(final String kind, final AbstractMatrix<?, ?, ?, ?, ?> empty) {
            final var narrow = empty.resize(0, 4);
            final var equal = empty.resize(0, 4);
            final var wide = empty.resize(0, 5);
            final var threeColumns = empty.resize(0, 3);
            final var threeRows = empty.resize(3, 0);

            assertEquals(1, empty.hashCode());
            assertEquals(narrow, equal);
            assertEquals(narrow.hashCode(), equal.hashCode());
            assertNotEquals(narrow, wide);
            assertNotEquals(narrow.hashCode(), wide.hashCode());
            assertNotEquals(narrow.hashCode(), empty.hashCode());
            assertNotEquals(threeColumns, wide);
            assertNotEquals(threeColumns, empty);
            assertNotEquals(threeColumns.hashCode(), wide.hashCode());
            assertNotEquals(threeColumns.hashCode(), empty.hashCode());
            assertEquals(threeRows, empty.resize(3, 0));
            assertEquals(threeRows.hashCode(), empty.resize(3, 0).hashCode());
            assertNotEquals(threeRows.hashCode(), empty.hashCode());
        }
    }

    @Nested
    class CrossTypeOperations {

        private static void assertShape(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int rows, final int columns) {
            assertEquals(rows, matrix.rowCount());
            assertEquals(columns, matrix.columnCount());
        }

        @Test
        public void testZeroByNShapeSurvivesNumericOperations() {
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
        public void testRandomFactoriesAcceptReproducibleGeneratorsAndPreserveZeroByN() {
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
        public void testRectangularDiagonalsUseTheShorterDimension() {
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
        public void testNByZeroTransposeAndRotationPreserveTheColumnCountInEveryVariant() {
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

        @Test
        public void testZeroByNShapeSurvivesConversionsAndMapToObj() {
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

        @Test
        public void testColumnStreamsOfAZeroRowMatrixYieldOneEmptyStreamPerColumn() {
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

        @Test
        public void testZipResultsKeepTheColumnCountOfZeroRowInputs() {
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

        @Test
        public void testEmptyResultsAreTheSharedSingletonInEveryVariant() {
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

        @Test
        public void testMatrixMultiplyExactAcceptsCancellingIntermediateTerms() {
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

        @Test
        public void testMapToObjNormalisesAPrimitiveElementTypeToken() {
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

        @Test
        public void testConversionsCanonicaliseOnlyGenuinelyEmptyResults() {
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

        @Test
        public void testEveryEmptyProducingPathReturnsTheSharedSingleton() {
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
    }
}
