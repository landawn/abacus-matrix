package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.stream.DoubleIteratorEx;
import com.landawn.abacus.util.stream.DoubleStream;
import com.landawn.abacus.util.stream.FloatIteratorEx;
import com.landawn.abacus.util.stream.FloatStream;
import com.landawn.abacus.util.stream.IntIteratorEx;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.LongIteratorEx;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.Stream;

/** Shared contracts intentionally exercised across matrix implementations. */
class MatrixTraversalContractTest extends MatrixTestSupport {

    @Nested
    class NumericCursors {

        private static List<AbstractMatrix<?, ?, ?, ?, ?>> matrices(final int rows, final int columns) {
            final IntMatrix ints = new IntMatrix(new int[rows][columns], columns);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    ints.set(i, j, i * columns + j + 1);
                }
            }
            return List.of(ints, ints.toLongMatrix(), ints.toFloatMatrix(), ints.toDoubleMatrix());
        }

        private static void setCell(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int row, final int column, final int value) {
            Array.setInt(matrix.rowView(row), column, value);
        }

        private static Object iterator(final Object stream) {
            if (stream instanceof IntStream s) {
                return s.iterator();
            }
            if (stream instanceof LongStream s) {
                return s.iterator();
            }
            if (stream instanceof FloatStream s) {
                return s.iterator();
            }
            if (stream instanceof DoubleStream s) {
                return s.iterator();
            }
            throw new AssertionError(stream.getClass());
        }

        private static double next(final Object iterator) {
            return ((Number) ((Iterator<?>) iterator).next()).doubleValue();
        }

        private static void advance(final Object iterator, final long count) {
            if (iterator instanceof IntIteratorEx i) {
                i.advance(count);
            } else if (iterator instanceof LongIteratorEx i) {
                i.advance(count);
            } else if (iterator instanceof FloatIteratorEx i) {
                i.advance(count);
            } else if (iterator instanceof DoubleIteratorEx i) {
                i.advance(count);
            } else {
                throw new AssertionError(iterator.getClass());
            }
        }

        private static Object toArray(final Object iterator) {
            if (iterator instanceof IntIterator i) {
                return i.toArray();
            }
            if (iterator instanceof LongIterator i) {
                return i.toArray();
            }
            if (iterator instanceof FloatIterator i) {
                return i.toArray();
            }
            if (iterator instanceof DoubleIterator i) {
                return i.toArray();
            }
            throw new AssertionError(iterator.getClass());
        }

        private static Object toListArray(final Object iterator) {
            if (iterator instanceof IntIterator i) {
                return i.toList().toArray();
            }
            if (iterator instanceof LongIterator i) {
                return i.toList().toArray();
            }
            if (iterator instanceof FloatIterator i) {
                return i.toList().toArray();
            }
            if (iterator instanceof DoubleIterator i) {
                return i.toList().toArray();
            }
            throw new AssertionError(iterator.getClass());
        }

        private static void assertValues(final double[] expected, final Object actual) {
            assertEquals(expected.length, Array.getLength(actual));
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], ((Number) Array.get(actual, i)).doubleValue(), "element " + i);
            }
        }

        private static void assertExhausted(final Object iterator) {
            assertFalse(((Iterator<?>) iterator).hasNext());
            assertValues(new double[0], toArray(iterator));
            assertValues(new double[0], toListArray(iterator));
            assertThrows(NoSuchElementException.class, () -> ((Iterator<?>) iterator).next());
        }

        private static void assertCountExhausts(final LongIterator iterator, final long expected) {
            assertEquals(expected, iterator.count());
            assertFalse(iterator.hasNext());
        }

        private static void assertCountExhausts(final ObjIterator<?> iterator, final long expected) {
            assertEquals(expected, iterator.count());
            assertFalse(iterator.hasNext());
        }

        @Test
        public void testIteratorCountConsumesTheRemainingElementsAndToArrayStillWorks() {
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
        public void testDiagonalMaterializationUsesOnlyRemainingLiveCellsAndExhaustsIterators() {
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
        public void testColumnMaterializationObservesLaterRowReorderingAndCellChanges() {
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
        public void testRowAndColumnMajorListsRespectPartiallyConsumedCursors() {
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
        public void testEmptyRangesValidateBoundsAndPreserveStreamsOfEmptyRowsOrColumns() {
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
                for (final Object row : rows) {
                    assertExhausted(iterator(row));
                }
                assertExhausted(iterator(matrix.mainDiagonalStream()));
                assertExhausted(iterator(matrix.antiDiagonalStream()));
            }
            for (final AbstractMatrix<?, ?, ?, ?, ?> matrix : matrices(0, 3)) {
                final List<?> columns = ((Stream<?>) matrix.columnStreams()).toList();
                assertEquals(3, columns.size());
                for (final Object column : columns) {
                    assertExhausted(iterator(column));
                }
                assertExhausted(iterator(matrix.mainDiagonalStream()));
                assertExhausted(iterator(matrix.antiDiagonalStream()));
            }
        }
    }
}
