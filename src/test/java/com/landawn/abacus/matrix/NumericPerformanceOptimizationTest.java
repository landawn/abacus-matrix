package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.stream.DoubleIteratorEx;
import com.landawn.abacus.util.stream.DoubleStream;
import com.landawn.abacus.util.stream.FloatIteratorEx;
import com.landawn.abacus.util.stream.FloatStream;
import com.landawn.abacus.util.stream.IntIteratorEx;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.LongIteratorEx;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.Stream;

class NumericPerformanceOptimizationTest extends TestBase {
    @Test
    void exactIntMultiplicationNarrowsFinishedCellsAndResetsEveryScratchRow() {
        final IntMatrix left = IntMatrix
                .wrap(new int[][] { { 2_000_000_000, 2_000_000_000, -2_000_000_000 }, { -2_000_000_000, -2_000_000_000, 2_000_000_000 }, { 1, 2, 3 } });
        final IntMatrix right = IntMatrix.wrap(new int[][] { { 1, -1 }, { 1, -1 }, { 1, -1 } });
        final IntMatrix leftBefore = left.copy();
        final IntMatrix rightBefore = right.copy();

        final IntMatrix product = left.matrixMultiplyExact(right);

        assertEquals(IntMatrix.wrap(new int[][] { { 2_000_000_000, -2_000_000_000 }, { -2_000_000_000, 2_000_000_000 }, { 6, -6 } }), product);
        assertEquals(leftBefore, left);
        assertEquals(rightBefore, right);
        assertNotSame(product.rowView(0), product.rowView(1));
        assertEquals(0,
                IntMatrix.wrap(new int[] { 100_000, 100_000 }).matrixMultiplyExact(IntMatrix.wrap(new int[][] { { 100_000 }, { -100_000 } })).get(0, 0));
    }

    @Test
    void exactIntMultiplicationStillRejectsIntermediateLongAndFinishedIntOverflow() {
        final IntMatrix longOverflow = IntMatrix.wrap(new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE });
        assertThrows(ArithmeticException.class, () -> longOverflow.matrixMultiplyExact(longOverflow.transpose()));
        assertThrows(ArithmeticException.class, () -> IntMatrix.wrap(new int[] { 50_000 }).matrixMultiplyExact(IntMatrix.wrap(new int[] { 50_000 })));
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.empty().matrixMultiplyExact(null));
        assertThrows(IllegalArgumentException.class, () -> new IntMatrix(new int[0][], 2).matrixMultiplyExact(new IntMatrix(new int[3][0])));
    }

    @Test
    void exactIntMultiplicationPreservesLongOverflowPrecedenceAcrossRows() {
        final IntMatrix left = IntMatrix.wrap(new int[][] { { Integer.MAX_VALUE, 0, 0 }, { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE } });
        final IntMatrix right = IntMatrix.wrap(new int[][] { { Integer.MIN_VALUE }, { Integer.MIN_VALUE }, { Integer.MIN_VALUE } });
        final ArithmeticException expected = assertThrows(ArithmeticException.class, () -> left.matrixMultiplyWidenedExact(right));
        final ArithmeticException actual = assertThrows(ArithmeticException.class, () -> left.matrixMultiplyExact(right));
        assertEquals(expected.getMessage(), actual.getMessage());
    }

    @Test
    void exactIntMultiplicationMatchesTheCheckedWidenedProductForRandomRectangularShapes() {
        final Random random = new Random(20261011L);
        int exactProducts = 0;
        int narrowingFailures = 0;
        int longOverflows = 0;

        for (int iteration = 0; iteration < 500; iteration++) {
            final int rows = 1 + random.nextInt(5);
            final int inner = 1 + random.nextInt(5);
            final int columns = 1 + random.nextInt(5);
            // Small values always fit; full-range values exercise both narrowing and long overflow.
            final boolean fullRange = iteration % 2 == 0;
            final int[][] leftCells = new int[rows][inner];
            final int[][] rightCells = new int[inner][columns];

            for (final int[] row : leftCells) {
                for (int j = 0; j < inner; j++) {
                    row[j] = fullRange ? random.nextInt() : random.nextInt(2_001) - 1_000;
                }
            }

            for (final int[] row : rightCells) {
                for (int j = 0; j < columns; j++) {
                    row[j] = fullRange ? random.nextInt() : random.nextInt(2_001) - 1_000;
                }
            }

            final IntMatrix left = IntMatrix.wrap(leftCells);
            final IntMatrix right = IntMatrix.wrap(rightCells);
            final LongMatrix widened;

            try {
                widened = left.matrixMultiplyWidenedExact(right);
            } catch (final ArithmeticException longOverflow) {
                // Long overflow must win over any int narrowing failure. Only the narrowing message is compared:
                // HotSpot may drop the message of a frequently thrown Math.addExact overflow.
                assertNotEquals("integer overflow", assertThrows(ArithmeticException.class, () -> left.matrixMultiplyExact(right)).getMessage());
                longOverflows++;
                continue;
            }

            final int[][] expected = new int[rows][columns];
            boolean fits = true;

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    final long value = widened.get(i, j);
                    fits &= value == (int) value;
                    expected[i][j] = (int) value;
                }
            }

            if (fits) {
                assertEquals(IntMatrix.wrap(expected), left.matrixMultiplyExact(right));
                exactProducts++;
            } else {
                assertEquals("integer overflow", assertThrows(ArithmeticException.class, () -> left.matrixMultiplyExact(right)).getMessage());
                narrowingFailures++;
            }
        }

        // The seed must keep every outcome covered.
        assertTrue(exactProducts > 0 && narrowingFailures > 0 && longOverflows > 0,
                exactProducts + " exact, " + narrowingFailures + " narrowing, " + longOverflows + " long overflow");
    }

    @Test
    void exactIntMultiplicationPreservesAllEmptyProductShapes() {
        final IntMatrix noInnerCells = IntMatrix.wrap(new int[][] { {}, {} }).matrixMultiplyExact(new IntMatrix(new int[0][], 3));
        assertShape(noInnerCells, 2, 3);
        assertArrayEquals(new int[3], noInnerCells.rowView(0));
        assertArrayEquals(new int[3], noInnerCells.rowView(1));
        assertNotSame(noInnerCells.rowView(0), noInnerCells.rowView(1));
        assertShape(new IntMatrix(new int[0][], 3).matrixMultiplyExact(IntMatrix.wrap(new int[3][4])), 0, 4);
        assertShape(IntMatrix.wrap(new int[2][3]).matrixMultiplyExact(IntMatrix.wrap(new int[3][0])), 2, 0);
        assertShape(IntMatrix.empty().matrixMultiplyExact(new IntMatrix(new int[0][], Integer.MAX_VALUE)), 0, Integer.MAX_VALUE);
        assertSame(IntMatrix.empty(), IntMatrix.empty().matrixMultiplyExact(IntMatrix.empty()));
    }

    @Test
    void trustedCopyAndWideningFactoriesStillOwnIndependentRows() {
        final int[] ints = { 7, 8 };
        final long[] longs = { 7, 8 };
        final float[] floats = { 7, 8 };
        final double[] doubles = { 7, 8 };
        final List<AbstractMatrix<?, ?, ?, ?, ?>> copies = List.of(IntMatrix.copyOf(ints, ints), LongMatrix.copyOf(longs, longs),
                FloatMatrix.copyOf(floats, floats), DoubleMatrix.copyOf(doubles, doubles));
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : copies) {
            assertNotSame(matrix.rowView(0), matrix.rowView(1));
            setCell(matrix, 0, 0, 99);
            assertEquals(7d, cell(matrix, 1, 0));
        }
        assertEquals(7, ints[0]);
        assertEquals(7L, longs[0]);
        assertEquals(7f, floats[0]);
        assertEquals(7d, doubles[0]);

        final char[] chars = { 1, 2 };
        final byte[] bytes = { 1, 2 };
        final short[] shorts = { 1, 2 };
        final int[] smallInts = { 1, 2 };
        final long[] smallLongs = { 1, 2 };
        final float[] smallFloats = { 1, 2 };
        final List<AbstractMatrix<?, ?, ?, ?, ?>> widened = List.of(IntMatrix.from(chars, chars), IntMatrix.from(bytes, bytes), IntMatrix.from(shorts, shorts),
                LongMatrix.from(smallInts, smallInts), FloatMatrix.from(smallInts, smallInts), DoubleMatrix.from(smallInts, smallInts),
                DoubleMatrix.from(smallLongs, smallLongs), DoubleMatrix.from(smallFloats, smallFloats));
        chars[0] = 9;
        bytes[0] = 9;
        shorts[0] = 9;
        smallInts[0] = 9;
        smallLongs[0] = 9;
        smallFloats[0] = 9;
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : widened) {
            assertEquals(1d, cell(matrix, 0, 0));
            assertNotSame(matrix.rowView(0), matrix.rowView(1));
            setCell(matrix, 0, 0, 99);
            assertEquals(1d, cell(matrix, 1, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(ints, ints));
        assertThrows(IllegalArgumentException.class, () -> LongMatrix.wrap(longs, longs));
        assertThrows(IllegalArgumentException.class, () -> FloatMatrix.wrap(floats, floats));
        assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.wrap(doubles, doubles));
    }

    @Test
    void trustedDiagonalFactoriesRetainPrecedenceAndIndependentStorage() {
        final int[] ints = { 1, 2, 3 };
        final long[] longs = { 1, 2, 3 };
        final float[] floats = { 1, 2, 3 };
        final double[] doubles = { 1, 2, 3 };
        final List<AbstractMatrix<?, ?, ?, ?, ?>> matrices = List.of(IntMatrix.ofDiagonals(ints, new int[] { 4, 5, 6 }),
                LongMatrix.ofDiagonals(longs, new long[] { 4, 5, 6 }), FloatMatrix.ofDiagonals(floats, new float[] { 4, 5, 6 }),
                DoubleMatrix.ofDiagonals(doubles, new double[] { 4, 5, 6 }));
        ints[0] = 99;
        longs[0] = 99;
        floats[0] = 99;
        doubles[0] = 99;
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices) {
            assertEquals(1d, cell(matrix, 0, 0));
            assertEquals(2d, cell(matrix, 1, 1));
            assertEquals(4d, cell(matrix, 0, 2));
            assertEquals(6d, cell(matrix, 2, 0));
            assertNotSame(matrix.rowView(0), matrix.rowView(1));
        }
    }

    @Test
    void trustedBoxedAndMappedResultsKeepTypesShapesAndIndependentRows() {
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(4, 3)) {
            final Matrix<?> boxed = boxed(matrix);
            final Matrix<?> mapped = mapped(matrix);
            assertShape(boxed, 4, 3);
            assertShape(mapped, 4, 3);
            assertEquals(boxed.get(0, 0).getClass(), boxed.elementType());
            assertEquals(boxed.elementType(), mapped.elementType());
            assertNotSame(boxed.rowView(0), boxed.rowView(1));
            assertNotSame(mapped.rowView(0), mapped.rowView(1));
            Array.set(boxed.rowView(0), 0, null);
            Array.set(mapped.rowView(0), 0, null);
            assertEquals(1d, cell(matrix, 0, 0));
            assertEquals(4d, ((Number) boxed.get(1, 0)).doubleValue());
            assertEquals(5d, ((Number) mapped.get(1, 0)).doubleValue());
        }
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(0, 7)) {
            final Matrix<?> boxed = boxed(matrix);
            final Matrix<?> mapped = mapped(matrix);
            assertShape(boxed, 0, 7);
            assertShape(mapped, 0, 7);
            assertEquals(boxed.elementType(), mapped.elementType());
            assertNotSame(Object.class, boxed.elementType());
        }
    }

    @Test
    void diagonalMaterializationUsesOnlyRemainingLiveCellsAndExhaustsIterators() {
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(4, 3)) {
            final Object main = iterator(matrix.mainDiagonalStream());
            assertEquals(1d, next(main));
            advance(main, 1);
            setCell(matrix, 2, 2, 19);
            assertValues(new double[] { 19 }, toArray(main));
            assertExhausted(main);

            final Object anti = iterator(matrix.antiDiagonalStream());
            assertEquals(3d, next(anti));
            advance(anti, 1);
            setCell(matrix, 2, 0, 17);
            assertValues(new double[] { 17 }, toListArray(anti));
            assertExhausted(anti);
        }
    }

    @Test
    void columnMaterializationObservesLaterRowReorderingAndCellChanges() {
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(4, 3)) {
            final Object column = iterator(((Stream<?>) matrix.columnStreams(1, 2)).iterator().next());
            assertEquals(2d, next(column));
            advance(column, 1);
            setCell(matrix, 1, 1, 15);
            matrix.flipVerticallyInPlace();
            assertValues(new double[] { 15, 2 }, toArray(column));
            assertExhausted(column);

            final Object remainingColumn = iterator(((Stream<?>) matrix.columnStreams(2, 3)).iterator().next());
            advance(remainingColumn, 2);
            assertValues(new double[] { 6, 3 }, toListArray(remainingColumn));
            assertExhausted(remainingColumn);
        }
    }

    @Test
    void rowAndColumnMajorListsRespectPartiallyConsumedCursors() {
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(4, 3)) {
            final Object rows = iterator(matrix.rowMajorStream(1, 4));
            advance(rows, 0);
            advance(rows, -1);
            assertEquals(4d, next(rows));
            advance(rows, 3);
            setCell(matrix, 2, 1, 18);
            assertValues(new double[] { 18, 9, 10, 11, 12 }, toListArray(rows));
            assertExhausted(rows);

            final Object columns = iterator(matrix.columnMajorStream(1, 3));
            assertEquals(2d, next(columns));
            advance(columns, 4);
            setCell(matrix, 1, 2, 16);
            assertValues(new double[] { 16, 9, 12 }, toListArray(columns));
            assertExhausted(columns);

            final Object skipped = iterator(matrix.mainDiagonalStream());
            advance(skipped, Long.MAX_VALUE);
            assertExhausted(skipped);
        }
    }

    @Test
    void emptyRangesValidateBoundsAndPreserveStreamsOfEmptyRowsOrColumns() {
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(4, 3)) {
            assertExhausted(iterator(matrix.rowMajorStream(1, 1)));
            assertExhausted(iterator(matrix.columnMajorStream(2, 2)));
            assertEquals(0L, ((Stream<?>) matrix.rowStreams(4, 4)).count());
            assertEquals(0L, ((Stream<?>) matrix.columnStreams(3, 3)).count());
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(4, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(5, 5));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(-1, -1));
        }
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(3, 0)) {
            final List<?> rows = ((Stream<?>) matrix.rowStreams()).toList();
            assertEquals(3, rows.size());
            for (final Object row : rows)
                assertExhausted(iterator(row));
            assertExhausted(iterator(matrix.mainDiagonalStream()));
            assertExhausted(iterator(matrix.antiDiagonalStream()));
        }
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(0, 3)) {
            final List<?> columns = ((Stream<?>) matrix.columnStreams()).toList();
            assertEquals(3, columns.size());
            for (final Object column : columns)
                assertExhausted(iterator(column));
            assertExhausted(iterator(matrix.mainDiagonalStream()));
            assertExhausted(iterator(matrix.antiDiagonalStream()));
        }
    }

    @Test
    void boundedCopyFromKeepsOverlappingSourceValuesAndNoOpValidation() {
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(4, 3)) {
            final Object source = backingRows(matrix, 1, 0);
            copyFrom(matrix, 0, 1, source);
            assertValues(new double[] { 1, 4, 5 }, matrix.rowView(0));
            assertValues(new double[] { 4, 1, 2 }, matrix.rowView(1));
            copyFrom(matrix, matrix.rowCount(), 0, source);
            copyFrom(matrix, 0, matrix.columnCount(), source);
            assertValues(new double[] { 1, 4, 5 }, matrix.rowView(0));
            assertThrows(IllegalArgumentException.class, () -> copyFrom(matrix, matrix.rowCount(), 0, null));
        }
    }

    @Test
    void boundedAntiDiagonalSnapshotProtectsAliasedRowsInsideTheWriteRegion() {
        for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(4, 3)) {
            setAntiDiagonal(matrix, matrix.rowView(0));
            assertValues(new double[] { 1, 2, 3 }, matrix.antiDiagonalCopy());
            setAntiDiagonal(matrix, matrix.rowView(3));
            assertValues(new double[] { 10, 11, 12 }, matrix.antiDiagonalCopy());
        }
    }

    private static List<AbstractMatrix<?, ?, ?, ?, ?>> matrices(final int rows, final int columns) {
        final IntMatrix ints = new IntMatrix(new int[rows][columns], columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++)
                ints.set(i, j, i * columns + j + 1);
        }
        return List.of(ints, ints.toLongMatrix(), ints.toFloatMatrix(), ints.toDoubleMatrix());
    }

    private static void assertShape(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int rows, final int columns) {
        assertEquals(rows, matrix.rowCount());
        assertEquals(columns, matrix.columnCount());
    }

    private static double cell(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int row, final int column) {
        return ((Number) Array.get(matrix.rowView(row), column)).doubleValue();
    }

    private static void setCell(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int row, final int column, final int value) {
        Array.setInt(matrix.rowView(row), column, value);
    }

    private static Object backingRows(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int... indices) {
        final Object result = Array.newInstance(matrix.rowView(0).getClass(), indices.length);
        for (int i = 0; i < indices.length; i++)
            Array.set(result, i, matrix.rowView(indices[i]));
        return result;
    }

    private static void copyFrom(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int row, final int column, final Object source) {
        if (matrix instanceof IntMatrix m)
            m.copyFrom(row, column, (int[][]) source);
        else if (matrix instanceof LongMatrix m)
            m.copyFrom(row, column, (long[][]) source);
        else if (matrix instanceof FloatMatrix m)
            m.copyFrom(row, column, (float[][]) source);
        else if (matrix instanceof DoubleMatrix m)
            m.copyFrom(row, column, (double[][]) source);
        else
            throw new AssertionError(matrix.getClass());
    }

    private static void setAntiDiagonal(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final Object values) {
        if (matrix instanceof IntMatrix m)
            m.setAntiDiagonal((int[]) values);
        else if (matrix instanceof LongMatrix m)
            m.setAntiDiagonal((long[]) values);
        else if (matrix instanceof FloatMatrix m)
            m.setAntiDiagonal((float[]) values);
        else if (matrix instanceof DoubleMatrix m)
            m.setAntiDiagonal((double[]) values);
        else
            throw new AssertionError(matrix.getClass());
    }

    private static Matrix<?> boxed(final AbstractMatrix<?, ?, ?, ?, ?> matrix) {
        if (matrix instanceof IntMatrix m)
            return m.boxed();
        if (matrix instanceof LongMatrix m)
            return m.boxed();
        if (matrix instanceof FloatMatrix m)
            return m.boxed();
        if (matrix instanceof DoubleMatrix m)
            return m.boxed();
        throw new AssertionError(matrix.getClass());
    }

    private static Matrix<?> mapped(final AbstractMatrix<?, ?, ?, ?, ?> matrix) {
        if (matrix instanceof IntMatrix m)
            return m.mapToObj(value -> value + 1, int.class);
        if (matrix instanceof LongMatrix m)
            return m.mapToObj(value -> value + 1, long.class);
        if (matrix instanceof FloatMatrix m)
            return m.mapToObj(value -> value + 1, float.class);
        if (matrix instanceof DoubleMatrix m)
            return m.mapToObj(value -> value + 1, double.class);
        throw new AssertionError(matrix.getClass());
    }

    private static Object iterator(final Object stream) {
        if (stream instanceof IntStream s)
            return s.iterator();
        if (stream instanceof LongStream s)
            return s.iterator();
        if (stream instanceof FloatStream s)
            return s.iterator();
        if (stream instanceof DoubleStream s)
            return s.iterator();
        throw new AssertionError(stream.getClass());
    }

    private static double next(final Object iterator) {
        return ((Number) ((Iterator<?>) iterator).next()).doubleValue();
    }

    private static void advance(final Object iterator, final long count) {
        if (iterator instanceof IntIteratorEx i)
            i.advance(count);
        else if (iterator instanceof LongIteratorEx i)
            i.advance(count);
        else if (iterator instanceof FloatIteratorEx i)
            i.advance(count);
        else if (iterator instanceof DoubleIteratorEx i)
            i.advance(count);
        else
            throw new AssertionError(iterator.getClass());
    }

    private static Object toArray(final Object iterator) {
        if (iterator instanceof IntIterator i)
            return i.toArray();
        if (iterator instanceof LongIterator i)
            return i.toArray();
        if (iterator instanceof FloatIterator i)
            return i.toArray();
        if (iterator instanceof DoubleIterator i)
            return i.toArray();
        throw new AssertionError(iterator.getClass());
    }

    private static Object toListArray(final Object iterator) {
        if (iterator instanceof IntIterator i)
            return i.toList().toArray();
        if (iterator instanceof LongIterator i)
            return i.toList().toArray();
        if (iterator instanceof FloatIterator i)
            return i.toList().toArray();
        if (iterator instanceof DoubleIterator i)
            return i.toList().toArray();
        throw new AssertionError(iterator.getClass());
    }

    private static void assertValues(final double[] expected, final Object actual) {
        assertEquals(expected.length, Array.getLength(actual));
        for (int i = 0; i < expected.length; i++)
            assertEquals(expected[i], ((Number) Array.get(actual, i)).doubleValue(), "element " + i);
    }

    private static void assertExhausted(final Object iterator) {
        assertFalse(((Iterator<?>) iterator).hasNext());
        assertValues(new double[0], toArray(iterator));
        assertValues(new double[0], toListArray(iterator));
        assertThrows(NoSuchElementException.class, () -> ((Iterator<?>) iterator).next());
    }
}
