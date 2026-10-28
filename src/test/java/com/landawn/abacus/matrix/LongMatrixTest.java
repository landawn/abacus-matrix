package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.stream.LongIteratorEx;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u;

class LongMatrixTest extends MatrixTestSupport {

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

    @Nested
    class ConstructorTests {

        @Test
        public void testConstructor() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = new LongMatrix(a);
            Assertions.assertEquals(2, matrix.rowCount());
            Assertions.assertEquals(2, matrix.columnCount());

            assertThrows(IllegalArgumentException.class, () -> new LongMatrix(null));
        }

        @Test
        public void testConstructor_WithLargeValues() {
            long[][] arr = { { Long.MAX_VALUE, Long.MIN_VALUE }, { 1000000000000L, -1000000000000L } };
            LongMatrix m = new LongMatrix(arr);
            assertEquals(Long.MAX_VALUE, m.get(0, 0));
            assertEquals(Long.MIN_VALUE, m.get(0, 1));
            assertEquals(1000000000000L, m.get(1, 0));
            assertEquals(-1000000000000L, m.get(1, 1));
        }

        @Test
        public void testConstructor_WithLargeMatrix() {
            long[][] arr = new long[10][10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    arr[i][j] = i * 10L + j;
                }
            }
            LongMatrix m = new LongMatrix(arr);
            assertEquals(10, m.rowCount());
            assertEquals(10, m.columnCount());
            assertEquals(0L, m.get(0, 0));
            assertEquals(99L, m.get(9, 9));
            assertEquals(55L, m.get(5, 5));
        }

        @Test
        public void testConstructor_WithValidArray() {
            long[][] arr = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix m = new LongMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1L, m.get(0, 0));
            assertEquals(4L, m.get(1, 1));
        }

        @Test
        public void testConstructor_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> new LongMatrix(null));
        }

        @Test
        public void testConstructor_WithEmptyArray() {
            LongMatrix m = new LongMatrix(new long[0][0]);
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
            assertTrue(m.isEmpty());
        }

        @Test
        public void testConstructor_WithSingleElement() {
            LongMatrix m = new LongMatrix(new long[][] { { 42L } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42L, m.get(0, 0));
        }
    }

    @Nested
    class EmptyTests {

        @Test
        public void testEmpty() {
            LongMatrix empty = LongMatrix.empty();
            Assertions.assertEquals(0, empty.rowCount());
            Assertions.assertEquals(0, empty.columnCount());
            Assertions.assertTrue(empty.isEmpty());
        }

        @Test
        public void testEmpty_EmptyMatrix_Identity() {
            LongMatrix empty = LongMatrix.empty();
            assertEquals(0, empty.rowCount());
            assertEquals(0, empty.columnCount());
            assertTrue(empty.isEmpty());

            // Test singleton
            assertSame(LongMatrix.empty(), LongMatrix.empty());
        }

        // Scalar division
        @Test
        public void testEmptyMatrixOperations() {
            LongMatrix empty = LongMatrix.empty();

            assertTrue(empty.flatten().isEmpty());
            assertEquals(0, empty.copy().rowCount);
            assertEquals(empty, empty.transpose());
            assertEquals(empty, empty.rotate90());

            LongMatrix extended = empty.resize(2, 2, 5L);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals(5L, extended.get(0, 0));
        }
    }

    @Nested
    class WrapTests {

        @Test
        public void testWrap() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);
            Assertions.assertEquals(2, matrix.rowCount());
            Assertions.assertEquals(2, matrix.columnCount());

            LongMatrix emptyMatrix = LongMatrix.wrap();
            Assertions.assertTrue(emptyMatrix.isEmpty());

            assertThrows(IllegalArgumentException.class, () -> LongMatrix.wrap((long[][]) null));
        }

        @Test
        public void testLargeDimensions() {
            long[][] data = new long[100][50];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 50; j++) {
                    data[i][j] = i + j;
                }
            }
            LongMatrix m = LongMatrix.wrap(data);
            assertEquals(100, m.rowCount());
            assertEquals(50, m.columnCount());
            assertEquals(0L, m.get(0, 0));
            assertEquals(148L, m.get(99, 49));
        }

        @Test
        public void testWrap_WithValidArray() {
            long[][] arr = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix m = LongMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1L, m.get(0, 0));
        }

        @Test
        public void testWrapAndConstructor_CopyOuterArrayButShareRows() {
            final long[][] wrapped = { { 1L, 2L }, { 3L, 4L } };
            final LongMatrix viaWrap = LongMatrix.wrap(wrapped);
            final long[][] constructed = { { 1L, 2L }, { 3L, 4L } };
            final LongMatrix viaConstructor = new LongMatrix(constructed);

            // A cell write through a caller-held row is visible through the matrix (rows are shared) ...
            wrapped[0][0] = 10L;
            constructed[0][0] = 10L;
            assertEquals(10L, viaWrap.get(0, 0));
            assertEquals(10L, viaConstructor.get(0, 0));
            assertSame(wrapped[1], viaWrap.rowView(1));
            assertSame(constructed[1], viaConstructor.rowView(1));

            // ... but replacing a slot of the caller's outer array is not (the outer array is copied).
            wrapped[1] = new long[] { 99L, 99L };
            constructed[1] = new long[] { 99L, 99L };
            assertEquals(3L, viaWrap.get(1, 0));
            assertEquals(3L, viaConstructor.get(1, 0));

            // copyOf shares neither the outer array nor the rows.
            final long[][] copied = { { 1L, 2L } };
            final LongMatrix viaCopyOf = LongMatrix.copyOf(copied);
            copied[0][0] = 10L;
            assertEquals(1L, viaCopyOf.get(0, 0));
        }

        @Test
        public void testWrap_WithSingleColumn() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L }, { 2L }, { 3L }, { 4L } });
            assertEquals(4, m.rowCount());
            assertEquals(1, m.columnCount());
        }

        @Test
        public void testWrap_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.wrap((long[][]) null));
        }

        @Test
        public void testWrap_WithEmptyArray() {
            LongMatrix m = LongMatrix.wrap(new long[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testWrap_WithSingleRow() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L, 4L, 5L } });
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }

        @Test
        public void testWrapRejectsDuplicateRowIdentities() {
            // Every logical row must own its storage: one array cannot back two independently
            // addressable rows. wrap() shares the caller's rows, so it rejects a repeat...
            final long[] sharedRow = { 1L, 2L, 3L, 4L };
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> LongMatrix.wrap(sharedRow, sharedRow));
            assertTrue(ex.getMessage().contains("independent storage"));

            // ...while copyOf() clones each row, so the same input is accepted.
            final LongMatrix copied = LongMatrix.copyOf(sharedRow, sharedRow);
            assertEquals(2, copied.rowCount());
            assertEquals(4, copied.columnCount());
            assertNotSame(copied.rowView(0), copied.rowView(1));

            copied.updateRow(0, value -> value + 10L);
            assertArrayEquals(new long[] { 11L, 12L, 13L, 14L }, copied.rowCopy(0));
            assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, copied.rowCopy(1));
            assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, sharedRow);
        }
    }

    @Nested
    class CopyOfTests {

        @Test
        public void testCopyOf() {
            long[][] arr = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix m = LongMatrix.copyOf(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1L, m.get(0, 0));
            assertEquals(4L, m.get(1, 1));

            // Mutating the source array must not affect the copy
            arr[0][0] = 99L;
            assertEquals(1L, m.get(0, 0));

            // Mutating the copy must not affect the source array
            m.set(1, 1, 88L);
            assertEquals(4L, arr[1][1]);

            // null -> rejected; empty -> shared empty matrix
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.copyOf((long[][]) null));
            assertTrue(LongMatrix.copyOf(new long[0][0]).isEmpty());

            // non-rectangular -> IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.copyOf(new long[][] { { 1L, 2L }, { 3L } }));
        }

        @Test
        public void testCopyOfValidationOrderAndIndependentEmptyRows() {
            assertEquals("Row 0 cannot be null", assertThrows(IllegalArgumentException.class, () -> LongMatrix.copyOf(null, new long[] { 1 })).getMessage());
            assertEquals("Row 2 cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> LongMatrix.copyOf(new long[] { 1 }, new long[] { 2 }, null)).getMessage());
            assertEquals("Matrix must be rectangular: expected 1 columns per row, but row 1 has 2 columns",
                    assertThrows(IllegalArgumentException.class, () -> LongMatrix.copyOf(new long[] { 1 }, new long[] { 2, 3 }, null)).getMessage());

            final long[] emptyRow = new long[0];
            final LongMatrix copied = LongMatrix.copyOf(emptyRow, emptyRow);
            assertEquals(2, copied.rowCount());
            assertEquals(0, copied.columnCount());
            assertNotSame(emptyRow, copied.rowView(0));
            assertNotSame(emptyRow, copied.rowView(1));
            assertNotSame(copied.rowView(0), copied.rowView(1));
        }
    }

    @Nested
    class FromTests {

        @Test
        public void testFrom() {
            int[][] a = { { 1, 2 }, { 3, 4 } };
            LongMatrix matrix = LongMatrix.from(a);
            Assertions.assertEquals(2, matrix.rowCount());
            Assertions.assertEquals(2, matrix.columnCount());
            Assertions.assertEquals(1L, matrix.get(0, 0));
            Assertions.assertEquals(4L, matrix.get(1, 1));

            assertThrows(IllegalArgumentException.class, () -> LongMatrix.from((int[][]) null));
        }

        // Just verify elements exist (values are random)
        @Test
        public void testFromFromIntArray_WithNull() {
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.from((int[][]) null));
        }

        @Test
        public void testFromFromIntArray_WithEmpty() {
            LongMatrix m = LongMatrix.from(new int[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testFromFromIntArray_WithNullRow() {
            int[][] nullRow = { { 1, 2 }, null };
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.from(nullRow));
        }

        @Test
        public void testFromFromIntArray_WithNullFirstRow() {
            int[][] nullFirstRow = { null, { 1, 2 } };
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.from(nullFirstRow));
        }

        @Test
        public void testFromFromIntArray_WithLargeInts() {
            int[][] ints = { { Integer.MAX_VALUE, Integer.MIN_VALUE }, { 1000000, -1000000 } };
            LongMatrix m = LongMatrix.from(ints);
            assertEquals(Integer.MAX_VALUE, m.get(0, 0));
            assertEquals(Integer.MIN_VALUE, m.get(0, 1));
            assertEquals(1000000L, m.get(1, 0));
            assertEquals(-1000000L, m.get(1, 1));
        }

        @Test
        public void testConversionFromIntWithMaxMin() {
            int[][] ints = { { Integer.MAX_VALUE, Integer.MIN_VALUE }, { 0, -1 } };
            LongMatrix m = LongMatrix.from(ints);

            // Verify proper conversion
            assertEquals(Integer.MAX_VALUE, m.get(0, 0));
            assertEquals(Integer.MIN_VALUE, m.get(0, 1));
            assertEquals(0L, m.get(1, 0));
            assertEquals(-1L, m.get(1, 1));

            // Verify that long values are different from int values (no truncation)
            assertTrue(m.get(0, 0) < Long.MAX_VALUE);
            assertTrue(m.get(0, 1) > Long.MIN_VALUE);
        }

        @Test
        public void testFrom_FromIntArray() {
            int[][] ints = { { 1, 2 }, { 3, 4 } };
            LongMatrix m = LongMatrix.from(ints);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1L, m.get(0, 0));
            assertEquals(4L, m.get(1, 1));
        }

        @Test
        public void testFrom_FromIntArray_DifferentRowLengths() {
            int[][] ints = { { 1, 2 }, { 3 } };
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.from(ints));
        }
    }

    @Nested
    class RandomRowTests {

        @Test
        public void testRandom() {
            LongMatrix matrix = LongMatrix.randomRow(5);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(5, matrix.columnCount());
        }

        @Test
        public void testRandom_Values() {
            LongMatrix m = LongMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            // Just verify elements exist (values are random)
            for (int i = 0; i < 5; i++) {
                assertNotNull(m.get(0, i));
            }
        }

        @Test
        public void testRandomRow_Values_SingleRowDimensionsAndValues() {
            LongMatrix m = LongMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertNotNull(m.rowView(0));
        }

        @Test
        public void testRandom_ZeroLength() {
            LongMatrix m = LongMatrix.randomRow(0);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        // The same terms in an order that never leaves the long range succeed.
        @Test
        public void testFactories_NegativeDimensions_ThrowIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.randomRow(-1));
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.random(-1, 2));
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.random(2, -1));
        }
    }

    @Nested
    class RandomTests {

        @Test
        public void testRandom_WithRowsCols() {
            LongMatrix m = LongMatrix.random(2, 3);
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    assertNotNull(m.get(i, j));
                }
            }
        }
    }

    @Nested
    class RangeTests {

        @Test
        public void testEmptyRangesOwnTheirRow() {
            // Array.range/rangeClosed return a shared empty-array constant for an empty range; each matrix must own
            // its row, or rows of independently created empty range matrices could not be wrapped together.
            final long[][] rows = { LongMatrix.range(5L, 5L).rowView(0), LongMatrix.range(5L, 1L, 1L).rowView(0), LongMatrix.rangeClosed(9L, 1L).rowView(0),
                    LongMatrix.rangeClosed(1L, 9L, -1L).rowView(0) };

            for (final long[] row : rows) {
                assertEquals(0, row.length);
                assertNotSame(com.landawn.abacus.util.N.EMPTY_LONG_ARRAY, row);
            }

            final LongMatrix combined = LongMatrix.wrap(rows);
            assertEquals(4, combined.rowCount());
            assertEquals(0, combined.columnCount());
        }

        @Test
        public void testRange() {
            LongMatrix matrix = LongMatrix.range(0L, 5L);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(5, matrix.columnCount());
            for (int i = 0; i < 5; i++) {
                Assertions.assertEquals(i, matrix.get(0, i));
            }
        }

        @Test
        public void testRangeWithStep() {
            LongMatrix matrix = LongMatrix.range(0L, 10L, 2L);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(5, matrix.columnCount());
            for (int i = 0; i < 5; i++) {
                Assertions.assertEquals(i * 2L, matrix.get(0, i));
            }
        }

        @Test
        public void testLongMatrixRange() {
            // LongMatrix range examples similar to IntMatrix
            LongMatrix matrix = LongMatrix.range(0, 5);
            assertEquals("[[0, 1, 2, 3, 4]]", matrix.toString());
        }

        @Test
        public void testLongMatrixRangeWithStep() {
            LongMatrix matrix = LongMatrix.range(0, 10, 2);
            assertEquals("[[0, 2, 4, 6, 8]]", matrix.toString());
        }

        @Test
        public void testRange_WithLargeValues() {
            LongMatrix m = LongMatrix.range(1000000000000L, 1000000000005L);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new long[] { 1000000000000L, 1000000000001L, 1000000000002L, 1000000000003L, 1000000000004L }, m.rowView(0));
        }

        @Test
        public void testRangeWithLargeStep() {
            LongMatrix m = LongMatrix.range(0L, 10000000000L, 2000000000L);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new long[] { 0L, 2000000000L, 4000000000L, 6000000000L, 8000000000L }, m.rowView(0));
        }

        @Test
        public void testRange_ArrayValues() {
            LongMatrix m = LongMatrix.range(0L, 5L);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new long[] { 0L, 1L, 2L, 3L, 4L }, m.rowView(0));
        }

        @Test
        public void testRange_WithStep() {
            LongMatrix m = LongMatrix.range(0L, 10L, 2L);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new long[] { 0L, 2L, 4L, 6L, 8L }, m.rowView(0));
        }

        @Test
        public void testRange_Empty() {
            LongMatrix m = LongMatrix.range(5L, 5L);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testRange_EmptyAndStepWrongDirection() {
            // Per Javadoc, range with start >= end returns a 1x0 matrix (1 row, 0 columns).
            LongMatrix r = LongMatrix.range(0L, 0L);
            assertEquals(1, r.rowCount());
            assertEquals(0, r.columnCount());
            assertTrue(r.isEmpty());

            LongMatrix r2 = LongMatrix.range(5L, 0L);
            assertEquals(1, r2.rowCount());
            assertEquals(0, r2.columnCount());
            assertTrue(r2.isEmpty());

            LongMatrix r3 = LongMatrix.range(0L, 10L, -1L);
            assertEquals(1, r3.rowCount());
            assertEquals(0, r3.columnCount());
        }

        @Test
        public void testRange_LongBoundaryEdgeCases() {
            assertArrayEquals(new long[] { Long.MAX_VALUE - 1, Long.MAX_VALUE }, LongMatrix.rangeClosed(Long.MAX_VALUE - 1, Long.MAX_VALUE).rowView(0));
            assertArrayEquals(new long[] { Long.MAX_VALUE - 2 }, LongMatrix.range(Long.MAX_VALUE - 2, Long.MAX_VALUE, 5L).rowView(0));
            assertArrayEquals(new long[] { 0L, Long.MAX_VALUE }, LongMatrix.rangeClosed(0L, Long.MAX_VALUE, Long.MAX_VALUE).rowView(0));
            assertArrayEquals(new long[] { Long.MIN_VALUE, -1L, Long.MAX_VALUE - 1 },
                    LongMatrix.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE).rowView(0));
            assertArrayEquals(new long[] { Long.MAX_VALUE, -1L }, LongMatrix.range(Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE).rowView(0));
            assertArrayEquals(new long[] { Long.MIN_VALUE + 2, Long.MIN_VALUE + 1, Long.MIN_VALUE },
                    LongMatrix.rangeClosed(Long.MIN_VALUE + 2, Long.MIN_VALUE, -1L).rowView(0));

            final LongMatrix wrongDirection = LongMatrix.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, -1L);
            assertEquals(1, wrongDirection.rowCount());
            assertEquals(0, wrongDirection.columnCount());

            assertThrows(IllegalArgumentException.class, () -> LongMatrix.range(Long.MIN_VALUE, Long.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.rangeClosed(0L, Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.range(0L, Long.MAX_VALUE, 2L));
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, 1L));
        }

        @Test
        public void testRange_WithNegativeStep() {
            LongMatrix m = LongMatrix.range(10L, 0L, -2L);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new long[] { 10L, 8L, 6L, 4L, 2L }, m.rowView(0));
        }
    }

    @Nested
    class RangeClosedTests {

        @Test
        public void testRangeClosed() {
            LongMatrix matrix = LongMatrix.rangeClosed(0L, 4L);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(5, matrix.columnCount());
            for (int i = 0; i < 5; i++) {
                Assertions.assertEquals(i, matrix.get(0, i));
            }
        }

        @Test
        public void testRangeClosedWithStep() {
            LongMatrix matrix = LongMatrix.rangeClosed(0L, 9L, 2L);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(5, matrix.columnCount());
            Assertions.assertEquals(0L, matrix.get(0, 0));
            Assertions.assertEquals(2L, matrix.get(0, 1));
            Assertions.assertEquals(4L, matrix.get(0, 2));
            Assertions.assertEquals(6L, matrix.get(0, 3));
            Assertions.assertEquals(8L, matrix.get(0, 4));
        }

        @Test
        public void testRangeClosed_ArrayValues() {
            LongMatrix m = LongMatrix.rangeClosed(0L, 4L);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new long[] { 0L, 1L, 2L, 3L, 4L }, m.rowView(0));
        }

        @Test
        public void testRangeClosed_WithStep() {
            LongMatrix m = LongMatrix.rangeClosed(0L, 10L, 2L);
            assertEquals(1, m.rowCount());
            assertEquals(6, m.columnCount());
            assertArrayEquals(new long[] { 0L, 2L, 4L, 6L, 8L, 10L }, m.rowView(0));
        }

        @Test
        public void testLongClosedRangeIncludesEqualEndpointsForEitherStepDirection() {
            for (long endpoint : new long[] { Long.MIN_VALUE, 0L, Long.MAX_VALUE }) {
                for (long step : new long[] { Long.MIN_VALUE, -1L, 1L, Long.MAX_VALUE }) {
                    final LongMatrix result = LongMatrix.rangeClosed(endpoint, endpoint, step);
                    assertEquals(1, result.rowCount());
                    assertArrayEquals(new long[] { endpoint }, result.rowCopy(0));
                }
                assertThrows(IllegalArgumentException.class, () -> LongMatrix.rangeClosed(endpoint, endpoint, 0L));
            }
            assertEquals(0, LongMatrix.rangeClosed(1L, 2L, -1L).columnCount());
            assertEquals(0, LongMatrix.rangeClosed(2L, 1L, 1L).columnCount());
        }

        @Test
        public void testRangeClosed_SingleValue() {
            LongMatrix m = LongMatrix.rangeClosed(5L, 5L);
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(5L, m.get(0, 0));
        }
    }

    @Nested
    class OfMainDiagonalTests {

        @Test
        public void testOfMainDiagonal() {
            long[] diag = { 1L, 2L, 3L };
            LongMatrix m = LongMatrix.ofMainDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1L, m.get(0, 0));
            assertEquals(2L, m.get(1, 1));
            assertEquals(3L, m.get(2, 2));
            assertEquals(0L, m.get(0, 1));
            assertEquals(0L, m.get(1, 0));
        }

        @Test
        public void testDiagonalLU2RD() {
            long[] diagonal = { 1L, 2L, 3L };
            LongMatrix matrix = LongMatrix.ofMainDiagonal(diagonal);
            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals(1L, matrix.get(0, 0));
            Assertions.assertEquals(2L, matrix.get(1, 1));
            Assertions.assertEquals(3L, matrix.get(2, 2));
            Assertions.assertEquals(0L, matrix.get(0, 1));
        }

        @Test
        public void testDiagonalLU2RD_Values() {
            LongMatrix m = LongMatrix.ofMainDiagonal(new long[] { 1L, 2L, 3L });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1L, m.get(0, 0));
            assertEquals(2L, m.get(1, 1));
            assertEquals(3L, m.get(2, 2));
            assertEquals(0L, m.get(0, 1));
            assertEquals(0L, m.get(1, 0));
        }

        @Test
        public void testOfMainDiagonal_Null() {
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.ofMainDiagonal(null));
        }
    }

    @Nested
    class OfAntiDiagonalTests {

        @Test
        public void testOfAntiDiagonal() {
            long[] diag = { 1L, 2L, 3L };
            LongMatrix m = LongMatrix.ofAntiDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1L, m.get(0, 2));
            assertEquals(2L, m.get(1, 1));
            assertEquals(3L, m.get(2, 0));
            assertEquals(0L, m.get(0, 0));
        }

        @Test
        public void testDiagonalRU2LD() {
            long[] diagonal = { 1L, 2L, 3L };
            LongMatrix matrix = LongMatrix.ofAntiDiagonal(diagonal);
            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals(1L, matrix.get(0, 2));
            Assertions.assertEquals(2L, matrix.get(1, 1));
            Assertions.assertEquals(3L, matrix.get(2, 0));
            Assertions.assertEquals(0L, matrix.get(0, 0));
        }

        @Test
        public void testDiagonalRU2LD_Values() {
            LongMatrix m = LongMatrix.ofAntiDiagonal(new long[] { 1L, 2L, 3L });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1L, m.get(0, 2));
            assertEquals(2L, m.get(1, 1));
            assertEquals(3L, m.get(2, 0));
            assertEquals(0L, m.get(0, 0));
            assertEquals(0L, m.get(2, 2));
        }

        @Test
        public void testOfAntiDiagonal_Null() {
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.ofAntiDiagonal(null));
        }
    }

    @Nested
    class OfDiagonalsTests {

        @Test
        public void testOfDiagonals() {
            LongMatrix matrix = LongMatrix.ofDiagonals(new long[] { 1, 2, 3 }, new long[] { 4, 5, 6 });
            assertEquals(1L, matrix.get(0, 0));
            assertEquals(0L, matrix.get(0, 1));
            assertEquals(4L, matrix.get(0, 2));
            assertEquals(0L, matrix.get(1, 0));
            assertEquals(2L, matrix.get(1, 1));
            assertEquals(0L, matrix.get(1, 2));
            assertEquals(6L, matrix.get(2, 0));
            assertEquals(0L, matrix.get(2, 1));
            assertEquals(3L, matrix.get(2, 2));
        }

        @Test
        public void testDiagonal() {
            long[] main = { 1L, 2L, 3L };
            long[] anti = { 4L, 5L, 6L };
            LongMatrix matrix = LongMatrix.ofDiagonals(main, anti);
            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals(1L, matrix.get(0, 0));
            Assertions.assertEquals(2L, matrix.get(1, 1));
            Assertions.assertEquals(3L, matrix.get(2, 2));
            Assertions.assertEquals(4L, matrix.get(0, 2));
            Assertions.assertEquals(2L, matrix.get(1, 1));
            Assertions.assertEquals(6L, matrix.get(2, 0));

            Assertions.assertThrows(IllegalArgumentException.class, () -> LongMatrix.ofDiagonals(null, null));

            Assertions.assertThrows(IllegalArgumentException.class, () -> LongMatrix.ofDiagonals(new long[] { 1L }, new long[] { 4L, 5L }));
        }

        @Test
        public void testDiagonal_WithOnlyMainDiagonal() {
            LongMatrix m = LongMatrix.ofDiagonals(new long[] { 1L, 2L, 3L }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1L, m.get(0, 0));
            assertEquals(2L, m.get(1, 1));
            assertEquals(3L, m.get(2, 2));
        }

        @Test
        public void testDiagonal_WithOnlyAntiDiagonal() {
            LongMatrix m = LongMatrix.ofDiagonals(null, new long[] { 4L, 5L, 6L });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(4L, m.get(0, 2));
            assertEquals(5L, m.get(1, 1));
            assertEquals(6L, m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithDifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.ofDiagonals(new long[] { 1L, 2L }, new long[] { 3L, 4L, 5L }));
        }

        @Test
        public void testOfDiagonals_MainDiagonalWinsAtSharedCenter() {
            final LongMatrix matrix = LongMatrix.ofDiagonals(new long[] { 1, 2, 3 }, new long[] { 4, 5, 6 });
            // At the shared center, the main diagonal wins.
            assertEquals(LongMatrix.wrap(new long[][] { { 1, 0, 4 }, { 0, 2, 0 }, { 6, 0, 3 } }), matrix);
        }

        @Test
        public void testDiagonal_DifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.ofDiagonals(new long[] { 1L, 2L }, new long[] { 1L, 2L, 3L }));
        }

        @Test
        public void testDiagonal_Both() {
            long[] lu2rd = { 1L, 2L, 3L };
            long[] ru2ld = { 4L, 5L, 6L };
            LongMatrix m = LongMatrix.ofDiagonals(lu2rd, ru2ld);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1L, m.get(0, 0));
            assertEquals(2L, m.get(1, 1));
            assertEquals(3L, m.get(2, 2));
            assertEquals(4L, m.get(0, 2));
            assertEquals(6L, m.get(2, 0));
        }

        @Test
        public void testDiagonal_DifferentLengths_Validation() {
            long[] lu2rd = { 1L, 2L };
            long[] ru2ld = { 4L, 5L, 6L };
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.ofDiagonals(lu2rd, ru2ld));
        }

        @Test
        public void testDiagonal_WithBothNull() {
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.ofDiagonals(null, null));
        }

        @Test
        public void testOfDiagonals_EmptyAndNullSidesPreserveEmptyShape() {
            assertTrue(LongMatrix.ofDiagonals(new long[0], new long[0]).isEmpty());
            assertTrue(LongMatrix.ofDiagonals(new long[0], null).isEmpty());
            assertTrue(LongMatrix.ofDiagonals(null, new long[0]).isEmpty());
        }
    }

    @Nested
    class UnboxTests {

        @Test
        public void testUnbox() {
            Long[][] boxed = { { 1L, 2L }, { 3L, 4L } };
            Matrix<Long> boxedMatrix = Matrix.wrap(boxed);
            LongMatrix unboxed = LongMatrix.unbox(boxedMatrix);
            Assertions.assertEquals(2, unboxed.rowCount());
            Assertions.assertEquals(2, unboxed.columnCount());
            Assertions.assertEquals(1L, unboxed.get(0, 0));
            Assertions.assertEquals(4L, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_Values() {
            Long[][] boxed = { { 1L, 2L }, { 3L, 4L } };
            Matrix<Long> boxedMatrix = Matrix.wrap(boxed);
            LongMatrix unboxed = LongMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertEquals(1L, unboxed.get(0, 0));
            assertEquals(4L, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_VerticalMatrix() {
            // Test unbox with rows > columnCount (3 rows, 2 columnCount)
            Matrix<Long> boxedMatrix = Matrix.wrap(new Long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            LongMatrix m = LongMatrix.unbox(boxedMatrix);
            assertEquals(3, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1L, m.get(0, 0));
            assertEquals(6L, m.get(2, 1));
        }

        @Test
        public void testUnbox_Shape2By2() {
            Matrix<Long> boxed = Matrix.wrap(new Long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m = LongMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1L, m.get(0, 0));
            assertEquals(4L, m.get(1, 1));
        }

        @Test
        public void testUnbox_WithNullValues() {
            Long[][] boxed = { { 1L, null }, { null, 4L } };
            Matrix<Long> boxedMatrix = Matrix.wrap(boxed);
            LongMatrix unboxed = LongMatrix.unbox(boxedMatrix);
            assertEquals(1L, unboxed.get(0, 0));
            assertEquals(0L, unboxed.get(0, 1)); // null -> 0
            assertEquals(0L, unboxed.get(1, 0)); // null -> 0
            assertEquals(4L, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_WithNulls() {
            Matrix<Long> boxed = Matrix.wrap(new Long[][] { { 1L, null }, { null, 4L } });
            LongMatrix m = LongMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1L, m.get(0, 0));
            assertEquals(0L, m.get(0, 1));
            assertEquals(0L, m.get(1, 0));
            assertEquals(4L, m.get(1, 1));
        }

        @Test
        public void testUnbox_TypeNeutralEmptyReturnsPrimitiveEmptyAndTypedZeroColumnsPreserveRows() {
            LongMatrix sharedEmpty = LongMatrix.unbox(Matrix.empty(Long.class));
            LongMatrix copiedTypeNeutralEmpty = LongMatrix.unbox(Matrix.empty(Long.class).copy());

            assertSame(LongMatrix.empty(), sharedEmpty);
            assertSame(LongMatrix.empty(), copiedTypeNeutralEmpty);

            LongMatrix zeroColumns = LongMatrix.unbox(Matrix.wrap(new Long[][] { {}, {} }));
            assertEquals(2, zeroColumns.rowCount());
            assertEquals(0, zeroColumns.columnCount());

            LongMatrix typeNeutralZeroColumns = LongMatrix.unbox(Matrix.empty(Long.class).resize(2, 0));
            assertEquals(2, typeNeutralZeroColumns.rowCount());
            assertEquals(0, typeNeutralZeroColumns.columnCount());

            Matrix<Long> typeNeutralExpanded = Matrix.empty(Long.class).resize(2, 2);
            typeNeutralExpanded.set(0, 1, 7L);
            assertEquals(LongMatrix.wrap(new long[][] { { 0L, 7L }, { 0L, 0L } }), LongMatrix.unbox(typeNeutralExpanded));
        }
    }

    @Nested
    class GetTests {

        @Test
        public void testGet() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);
            Assertions.assertEquals(1L, matrix.get(0, 0));
            Assertions.assertEquals(2L, matrix.get(0, 1));
            Assertions.assertEquals(3L, matrix.get(1, 0));
            Assertions.assertEquals(4L, matrix.get(1, 1));

            Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(2, 0));
            Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(0, 2));
        }

        @Test
        public void testGetWithPoint() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);
            Assertions.assertEquals(1L, matrix.get(Point.of(0, 0)));
            Assertions.assertEquals(4L, matrix.get(Point.of(1, 1)));
        }

        @Test
        public void testGet_Shape2By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            assertEquals(1L, m.get(0, 0));
            assertEquals(5L, m.get(1, 1));
            assertEquals(6L, m.get(1, 2));
        }

        @Test
        public void testGetWithPoint_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertEquals(1L, m.get(Point.of(0, 0)));
            assertEquals(4L, m.get(Point.of(1, 1)));
            assertEquals(2L, m.get(Point.of(0, 1)));
        }

        @Test
        public void testGet_WithPoint() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            Point p = Point.of(1, 0);
            assertEquals(3L, m.get(p));
        }

        @Test
        public void testGet_ByIndices() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertEquals(1L, m.get(0, 0));
            assertEquals(2L, m.get(0, 1));
            assertEquals(3L, m.get(1, 0));
            assertEquals(4L, m.get(1, 1));
        }

        @Test
        public void testGet_ByPoint() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            Point p = Point.of(0, 1);
            assertEquals(2L, m.get(p));
        }

        @Test
        public void testGet_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(-1, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(2, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(0, -1));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(0, 2));
        }
    }

    @Nested
    class SetTests {

        @Test
        public void testSet() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);
            matrix.set(0, 0, 10L);
            Assertions.assertEquals(10L, matrix.get(0, 0));

            Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.set(2, 0, 5L));
        }

        @Test
        public void testSetWithPoint() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);
            matrix.set(Point.of(1, 1), 10L);
            Assertions.assertEquals(10L, matrix.get(1, 1));
        }

        @Test
        public void testSet_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.set(0, 0, 10L);
            assertEquals(10L, m.get(0, 0));

            m.set(1, 1, 20L);
            assertEquals(20L, m.get(1, 1));
        }

        @Test
        public void testSetWithPoint_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.set(Point.of(0, 0), 50L);
            assertEquals(50L, m.get(Point.of(0, 0)));
        }

        @Test
        public void testSet_WithLargeValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.set(0, 0, Long.MAX_VALUE);
            m.set(1, 1, Long.MIN_VALUE);
            assertEquals(Long.MAX_VALUE, m.get(0, 0));
            assertEquals(Long.MIN_VALUE, m.get(1, 1));
        }

        @Test
        public void testSet_WithPoint() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            Point p = Point.of(1, 0);
            m.set(p, 99L);
            assertEquals(99L, m.get(1, 0));
        }

        @Test
        public void testSet_ByIndices() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.set(0, 1, 9L);
            assertEquals(9L, m.get(0, 1));
        }

        @Test
        public void testSet_ByPoint() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            Point p = Point.of(1, 1);
            m.set(p, 9L);
            assertEquals(9L, m.get(p));
        }

        @Test
        public void testSet_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, 0L));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(2, 0, 0L));
        }
    }

    @Nested
    class ValueAboveTests {

        @Test
        public void testValueAbove() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            OptionalLong up = matrix.valueAbove(1, 0);
            Assertions.assertTrue(up.isPresent());
            Assertions.assertEquals(1L, up.getAsLong());

            OptionalLong empty = matrix.valueAbove(0, 0);
            Assertions.assertFalse(empty.isPresent());
        }

        @Test
        public void testAbove() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            u.OptionalLong value = matrix.valueAbove(1, 0);
            assertEquals(1L, value.get());
            u.OptionalLong empty = matrix.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_Shape2By2_PresentNeighborValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            OptionalLong up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1L, up.get());
        }

        @Test
        public void testValueAbove_FirstRow() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            OptionalLong up = m.valueAbove(0, 0);
            assertFalse(up.isPresent());
        }

        @Test
        public void testValueAbove_Shape2By2_InteriorValueAndMissingBoundary() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            OptionalLong up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1L, up.get());

            OptionalLong noUp = m.valueAbove(0, 0);
            assertFalse(noUp.isPresent());
        }
    }

    @Nested
    class ValueBelowTests {

        @Test
        public void testValueBelow() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            OptionalLong down = matrix.valueBelow(0, 0);
            Assertions.assertTrue(down.isPresent());
            Assertions.assertEquals(3L, down.getAsLong());

            OptionalLong empty = matrix.valueBelow(1, 0);
            Assertions.assertFalse(empty.isPresent());
        }

        @Test
        public void testBelow() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            u.OptionalLong value = matrix.valueBelow(0, 0);
            assertEquals(3L, value.get());
            u.OptionalLong empty = matrix.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_Shape2By2_PresentNeighborValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            OptionalLong down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3L, down.get());
        }

        @Test
        public void testValueBelow_LastRow() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            OptionalLong down = m.valueBelow(1, 0);
            assertFalse(down.isPresent());
        }

        @Test
        public void testValueBelow_Shape2By2_InteriorValueAndMissingBoundary() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            OptionalLong down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3L, down.get());

            OptionalLong noDown = m.valueBelow(1, 0);
            assertFalse(noDown.isPresent());
        }
    }

    @Nested
    class ValueLeftTests {

        @Test
        public void testValueLeft() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            OptionalLong left = matrix.valueLeft(0, 1);
            Assertions.assertTrue(left.isPresent());
            Assertions.assertEquals(1L, left.getAsLong());

            OptionalLong empty = matrix.valueLeft(0, 0);
            Assertions.assertFalse(empty.isPresent());
        }

        @Test
        public void testLeft() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            u.OptionalLong value = matrix.valueLeft(0, 1);
            assertEquals(1L, value.get());
            u.OptionalLong empty = matrix.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_Shape2By2_PresentNeighborValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            OptionalLong left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1L, left.get());
        }

        @Test
        public void testValueLeft_FirstColumn() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            OptionalLong left = m.valueLeft(0, 0);
            assertFalse(left.isPresent());
        }

        @Test
        public void testValueLeft_Shape2By2_InteriorValueAndMissingBoundary() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            OptionalLong left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1L, left.get());

            OptionalLong noLeft = m.valueLeft(0, 0);
            assertFalse(noLeft.isPresent());
        }
    }

    @Nested
    class ValueRightTests {

        @Test
        public void testValueRight() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            OptionalLong right = matrix.valueRight(0, 0);
            Assertions.assertTrue(right.isPresent());
            Assertions.assertEquals(2L, right.getAsLong());

            OptionalLong empty = matrix.valueRight(0, 1);
            Assertions.assertFalse(empty.isPresent());
        }

        @Test
        public void testRight() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            u.OptionalLong value = matrix.valueRight(0, 0);
            assertEquals(2L, value.get());
            u.OptionalLong empty = matrix.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_Shape2By2_PresentNeighborValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            OptionalLong right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2L, right.get());
        }

        @Test
        public void testValueRight_LastColumn() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            OptionalLong right = m.valueRight(0, 1);
            assertFalse(right.isPresent());
        }

        @Test
        public void testValueRight_Shape2By2_InteriorValueAndMissingBoundary() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            OptionalLong right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2L, right.get());

            OptionalLong noRight = m.valueRight(0, 1);
            assertFalse(noRight.isPresent());
        }
    }

    @Nested
    class RowViewTests {

        @Test
        public void testRow() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] row0 = matrix.rowView(0);
            Assertions.assertArrayEquals(new long[] { 1L, 2L, 3L }, row0);

            long[] row1 = matrix.rowView(1);
            Assertions.assertArrayEquals(new long[] { 4L, 5L, 6L }, row1);

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(-1));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(2));
        }

        @Test
        public void testRowView() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            long[] firstRow = matrix.rowView(0);
            assertArrayEquals(new long[] { 1L, 2L, 3L }, firstRow);
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            assertArrayEquals(new long[] { 1L, 2L, 3L }, m.rowView(0));
            assertArrayEquals(new long[] { 4L, 5L, 6L }, m.rowView(1));
        }

        @Test
        public void testRowView_Shape2By2_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            long[] row = m.rowView(0);
            assertArrayEquals(new long[] { 1L, 2L }, row);
        }

        @Test
        public void testRowView_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(2));
        }

        @Test
        public void testRowView_InvalidIndex() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(1));
        }

        @Test
        public void testRowView_InvalidIndex_Shape1By2_Validation() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(5));
        }
    }

    @Nested
    class RowCopyTests {

        @Test
        public void testRowCopy() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] rowCopy = matrix.rowCopy(0);
            Assertions.assertArrayEquals(new long[] { 1L, 2L, 3L }, rowCopy);

            rowCopy[0] = 99L;
            Assertions.assertArrayEquals(new long[] { 1L, 2L, 3L }, matrix.rowView(0));
        }

        @Test
        public void testRowCopy_InvalidIndex() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(-1));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(2));
        }
    }

    @Nested
    class ColumnCopyTests {

        @Test
        public void testColumn() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] col0 = matrix.columnCopy(0);
            Assertions.assertArrayEquals(new long[] { 1L, 4L }, col0);

            long[] col1 = matrix.columnCopy(1);
            Assertions.assertArrayEquals(new long[] { 2L, 5L }, col1);

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(-1));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(3));
        }

        @Test
        public void testColumnCopy() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            long[] firstColumn = matrix.columnCopy(0);
            assertArrayEquals(new long[] { 1L, 4L }, firstColumn);
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            assertArrayEquals(new long[] { 1L, 4L }, m.columnCopy(0));
            assertArrayEquals(new long[] { 2L, 5L }, m.columnCopy(1));
            assertArrayEquals(new long[] { 3L, 6L }, m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_Shape2By2_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            long[] col = m.columnCopy(0);
            assertArrayEquals(new long[] { 1L, 3L }, col);
        }

        @Test
        public void testColumnCopy_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex_Shape1By2_Validation() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(5));
        }
    }

    @Nested
    class SetRowTests {

        @Test
        public void testSetRow() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.setRow(0, new long[] { 10L, 20L, 30L });
            Assertions.assertArrayEquals(new long[] { 10L, 20L, 30L }, matrix.rowView(0));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setRow(0, new long[] { 1L, 2L }));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setRow(0, (long[]) null));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.setRow(0, new long[] { 10L, 20L });
            assertArrayEquals(new long[] { 10L, 20L }, m.rowView(0));
            assertArrayEquals(new long[] { 3L, 4L }, m.rowView(1)); // unchanged
        }

        @Test
        public void testSetRow_WrongSize() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new long[] { 1L }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new long[] { 1L, 2L, 3L }));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues_RowReplacementAndNeighborPreserved() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.setRow(0, new long[] { 10L, 20L });
            assertArrayEquals(new long[] { 10L, 20L }, m.rowView(0));
            assertEquals(3L, m.get(1, 0));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues_ReplacesFirstRow() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.setRow(0, new long[] { 9L, 8L });
            assertArrayEquals(new long[] { 9L, 8L }, m.rowView(0));
        }

        @Test
        public void testSetRow_InvalidLength() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new long[] { 1L, 2L, 3L }));
        }

        @Test
        public void testSetRow_InvalidRowIndex() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setRow(5, new long[] { 1L, 2L }));
        }

        @Test
        public void testSetRow_InvalidLength_Shape1By2_Validation() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new long[] { 1L }));
        }
    }

    @Nested
    class SetColumnTests {

        @Test
        public void testSetColumn() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.setColumn(0, new long[] { 10L, 20L });
            Assertions.assertArrayEquals(new long[] { 10L, 20L }, matrix.columnCopy(0));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setColumn(0, new long[] { 1L }));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setColumn(0, (long[]) null));
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.setColumn(0, new long[] { 10L, 20L });
            assertArrayEquals(new long[] { 10L, 20L }, m.columnCopy(0));
            assertArrayEquals(new long[] { 2L, 4L }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testSetColumn_WrongSize() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new long[] { 1L }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new long[] { 1L, 2L, 3L }));
        }

        @Test
        public void testSetColumn_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.setColumn(0, new long[] { 10L, 30L });
            assertEquals(10L, m.get(0, 0));
            assertEquals(30L, m.get(1, 0));
            assertEquals(2L, m.get(0, 1));
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues_ReplacesFirstColumn() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.setColumn(0, new long[] { 9L, 8L });
            assertArrayEquals(new long[] { 9L, 8L }, m.columnCopy(0));
        }

        @Test
        public void testAliasedArraySourcesAreSnapshotted() {
            LongMatrix columnMatrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            columnMatrix.setColumn(1, columnMatrix.rowView(0));
            assertArrayEquals(new long[] { 3L, 2L }, columnMatrix.rowCopy(1));

            LongMatrix diagonalMatrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            diagonalMatrix.setAntiDiagonal(diagonalMatrix.rowView(0));
            assertArrayEquals(new long[] { 1L, 2L, 3L }, diagonalMatrix.antiDiagonalCopy());

            long[][] backing = { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } };
            LongMatrix fillMatrix = LongMatrix.wrap(backing);
            fillMatrix.copyFrom(1, 0, backing);
            assertArrayEquals(new long[] { 3L, 4L }, fillMatrix.rowCopy(2));
        }

        @Test
        public void testSetColumn_InvalidLength() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new long[] { 1L, 2L, 3L }));
        }

        @Test
        public void testSetColumn_InvalidColumnIndex() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(5, new long[] { 1L }));
        }
    }

    @Nested
    class UpdateRowTests {

        @Test
        public void testUpdateRow() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.updateRow(0, x -> x * 2);
            Assertions.assertArrayEquals(new long[] { 2L, 4L, 6L }, matrix.rowView(0));
        }

        @Test
        public void testUpdateRow_DoublesFirstRowOnly() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            matrix.updateRow(0, x -> x * 2);
            assertArrayEquals(new long[] { 2L, 4L, 6L }, matrix.rowView(0));
            assertArrayEquals(new long[] { 4L, 5L, 6L }, matrix.rowView(1));
        }

        @Test
        public void testUpdateRow_Shape2By2_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.updateRow(0, x -> x * 2L);
            assertArrayEquals(new long[] { 2L, 4L }, m.rowView(0));
            assertArrayEquals(new long[] { 3L, 4L }, m.rowView(1)); // unchanged
        }

        @Test
        public void testUpdateRow_Shape2By2_ArrayValues_PreservesOtherRow() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.updateRow(0, x -> x * 2);
            assertArrayEquals(new long[] { 2L, 4L }, m.rowView(0));
            assertArrayEquals(new long[] { 3L, 4L }, m.rowView(1));
        }

        @Test
        public void testInPlaceCallbacksRetainCompletedUpdatesWhenTheyThrow() {
            final IllegalStateException failure = new IllegalStateException("stop at the second element");
            final Throwables.LongUnaryOperator<RuntimeException> operator = value -> {
                if (value == 2) {
                    throw failure;
                }
                return value * 10;
            };
            final List<Throwables.Consumer<LongMatrix, RuntimeException>> updates = List.of(m -> m.updateRow(0, operator), m -> m.updateColumn(0, operator),
                    m -> m.updateMainDiagonal(operator), m -> m.updateAntiDiagonal(operator), m -> m.updateAll(operator),
                    m -> m.updateAll((i, j) -> operator.applyAsLong(m.get(i, j))), m -> m.replaceIf(value -> operator.applyAsLong(value) > 0, 10),
                    m -> m.replaceIf((i, j) -> operator.applyAsLong(m.get(i, j)) > 0, 10));

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                for (int operation = 0; operation < updates.size(); operation++) {
                    final LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2, 1 }, { 2, 2, 2 }, { 1, 2, 1 } });
                    final Throwables.Consumer<LongMatrix, RuntimeException> update = updates.get(operation);
                    assertSame(failure, assertThrows(IllegalStateException.class, () -> update.accept(m)));
                    final long[][] expected = { { 1, 2, 1 }, { 2, 2, 2 }, { 1, 2, 1 } };
                    expected[0][operation == 3 ? 2 : 0] = 10;
                    assertEquals(LongMatrix.wrap(expected), m, "operation " + operation);
                }
            });
        }

        @Test
        public void testUpdateRowAndUpdateColumnNullOperator() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.updateRow(0, (Throwables.LongUnaryOperator<RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.updateColumn(0, (Throwables.LongUnaryOperator<RuntimeException>) null));
        }

        @Test
        public void testUpdateRowAndUpdateColumnInvalidIndex() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateRow(-1, x -> x * 2));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateColumn(3, x -> x + 10));
        }
    }

    @Nested
    class UpdateColumnTests {

        @Test
        public void testUpdateColumn() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.updateColumn(1, x -> x + 10);
            Assertions.assertArrayEquals(new long[] { 12L, 15L }, matrix.columnCopy(1));
        }

        @Test
        public void testUpdateColumn_AddsTenToFirstColumn() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            matrix.updateColumn(0, x -> x + 10L);
            assertEquals(11L, matrix.get(0, 0));
            assertEquals(2L, matrix.get(0, 1));
            assertEquals(13L, matrix.get(1, 0));
            assertEquals(15L, matrix.get(2, 0));
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.updateColumn(0, x -> x + 10L);
            assertArrayEquals(new long[] { 11L, 13L }, m.columnCopy(0));
            assertArrayEquals(new long[] { 2L, 4L }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testUpdateColumn_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.updateColumn(0, x -> x * 10);
            assertEquals(10L, m.get(0, 0));
            assertEquals(30L, m.get(1, 0));
            assertEquals(2L, m.get(0, 1));
            assertEquals(4L, m.get(1, 1));
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues_PreservesOtherColumn() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.updateColumn(0, x -> x * 2);
            assertArrayEquals(new long[] { 2L, 6L }, m.columnCopy(0));
            assertArrayEquals(new long[] { 2L, 4L }, m.columnCopy(1));
        }

        @Test
        public void testUnaryUpdatesTouchEveryCellExactlyOnce() {
            // With independent rows guaranteed, every cell is visited exactly once, sequentially
            // or in parallel, and each row is updated independently of the others.
            final LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 1L, 2L } });

            matrix.updateColumn(0, value -> value + 1L);
            assertArrayEquals(new long[] { 2L, 2L }, matrix.rowCopy(0));
            assertArrayEquals(new long[] { 2L, 2L }, matrix.rowCopy(1));

            final AtomicInteger sequentialCalls = new AtomicInteger();
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> matrix.updateAll(value -> {
                sequentialCalls.incrementAndGet();
                return value + 1L;
            }));
            assertEquals(4, sequentialCalls.get());

            final AtomicInteger parallelCalls = new AtomicInteger();
            Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> matrix.updateAll(value -> {
                parallelCalls.incrementAndGet();
                return value + 1L;
            }));
            assertEquals(4, parallelCalls.get());
            assertArrayEquals(new long[] { 4L, 4L }, matrix.rowCopy(0));
            assertArrayEquals(new long[] { 4L, 4L }, matrix.rowCopy(1));
        }
    }

    @Nested
    class MainDiagonalCopyTests {

        @Test
        public void testGetLU2RD() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] diagonal = matrix.mainDiagonalCopy();
            Assertions.assertArrayEquals(new long[] { 1L, 5L, 9L }, diagonal);

            LongMatrix nonSquare = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            Assertions.assertArrayEquals(new long[] { 1L }, nonSquare.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_Shape3By3_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            assertArrayEquals(new long[] { 1L, 5L, 9L }, m.mainDiagonalCopy());
        }

        @Test
        public void testGetLU2RD_nonSquare() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertArrayEquals(new long[] { 1L }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            long[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new long[] { 1L, 5L, 9L }, diag);
        }

        @Test
        public void testMainDiagonalCopy_NonSquare() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            assertArrayEquals(new long[] { 1L, 4L }, m.mainDiagonalCopy());
        }
    }

    @Nested
    class SetMainDiagonalTests {

        @Test
        public void testSetLU2RD() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.setMainDiagonal(new long[] { 10L, 20L, 30L });
            Assertions.assertEquals(10L, matrix.get(0, 0));
            Assertions.assertEquals(20L, matrix.get(1, 1));
            Assertions.assertEquals(30L, matrix.get(2, 2));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setMainDiagonal(new long[] { 1L, 2L }));
        }

        @Test
        public void testSetMainDiagonal_Shape3By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            m.setMainDiagonal(new long[] { 10L, 20L, 30L });
            assertEquals(10L, m.get(0, 0));
            assertEquals(20L, m.get(1, 1));
            assertEquals(30L, m.get(2, 2));
        }

        @Test
        public void testSetLU2RD_nonSquare() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            Assertions.assertDoesNotThrow(() -> m.setMainDiagonal(new long[] { 1L }));
        }

        @Test
        public void testSetMainDiagonal_ArrayTooShort() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new long[] { 1L, 2L }));
        }

        @Test
        public void testSetMainDiagonal() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.setMainDiagonal(new long[] { 9L, 8L });
            assertArrayEquals(new long[] { 9L, 8L }, m.mainDiagonalCopy());
        }

        @Test
        public void testSetMainDiagonal_NonSquare() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            Assertions.assertDoesNotThrow(() -> m.setMainDiagonal(new long[] { 9L }));
        }

        @Test
        public void testSetDiagonal_NullArraysOnEmptyMatrixThrow() {
            LongMatrix m = LongMatrix.empty();
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(null));
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(null));
        }

        @Test
        public void testSetLU2RD_invalidLength() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new long[] { 1L, 2L, 3L }));
        }

        @Test
        public void testSetMainDiagonal_InvalidLength() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new long[] { 9L }));
        }
    }

    @Nested
    class UpdateMainDiagonalTests {

        @Test
        public void testUpdateLU2RD() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.updateMainDiagonal(x -> x * x);
            Assertions.assertEquals(1L, matrix.get(0, 0));
            Assertions.assertEquals(25L, matrix.get(1, 1));
            Assertions.assertEquals(81L, matrix.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal_SquaresOnlyDiagonalCells() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            matrix.updateMainDiagonal(x -> x * x);
            assertEquals(1L, matrix.get(0, 0));
            assertEquals(2L, matrix.get(0, 1));
            assertEquals(3L, matrix.get(1, 0));
            assertEquals(16L, matrix.get(1, 1));
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            m.updateMainDiagonal(x -> x * 10L);
            assertEquals(10L, m.get(0, 0));
            assertEquals(50L, m.get(1, 1));
            assertEquals(90L, m.get(2, 2));
            assertEquals(2L, m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateMainDiagonal_NonSquare() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            Assertions.assertDoesNotThrow(() -> m.updateMainDiagonal(x -> x * 2L));
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3_ScalesEveryDiagonalElement() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            m.updateMainDiagonal(x -> x * 10);
            assertEquals(10L, m.get(0, 0));
            assertEquals(50L, m.get(1, 1));
            assertEquals(90L, m.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.updateMainDiagonal(x -> x * 2);
            assertArrayEquals(new long[] { 2L, 8L }, m.mainDiagonalCopy());
        }
    }

    @Nested
    class AntiDiagonalCopyTests {

        @Test
        public void testGetRU2LD() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] diagonal = matrix.antiDiagonalCopy();
            Assertions.assertArrayEquals(new long[] { 3L, 5L, 7L }, diagonal);
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            assertArrayEquals(new long[] { 3L, 5L, 7L }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertArrayEquals(new long[] { 2L }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            long[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new long[] { 3L, 5L, 7L }, diag);
        }
    }

    @Nested
    class SetAntiDiagonalTests {

        @Test
        public void testSetRU2LD() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.setAntiDiagonal(new long[] { 10L, 20L, 30L });
            Assertions.assertEquals(10L, matrix.get(0, 2));
            Assertions.assertEquals(20L, matrix.get(1, 1));
            Assertions.assertEquals(30L, matrix.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            m.setAntiDiagonal(new long[] { 10L, 20L, 30L });
            assertEquals(10L, m.get(0, 2));
            assertEquals(20L, m.get(1, 1));
            assertEquals(30L, m.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_NonSquare() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            Assertions.assertDoesNotThrow(() -> m.setAntiDiagonal(new long[] { 1L }));
        }

        @Test
        public void testSetAntiDiagonal_ArrayTooShort() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new long[] { 1L, 2L }));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3_ReplacesEveryDiagonalElement() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            m.setAntiDiagonal(new long[] { 30L, 50L, 70L });
            assertEquals(30L, m.get(0, 2));
            assertEquals(50L, m.get(1, 1));
            assertEquals(70L, m.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.setAntiDiagonal(new long[] { 9L, 8L });
            assertArrayEquals(new long[] { 9L, 8L }, m.antiDiagonalCopy());
        }
    }

    @Nested
    class UpdateAntiDiagonalTests {

        @Test
        public void testUpdateRU2LD() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.updateAntiDiagonal(x -> -x);
            Assertions.assertEquals(-3L, matrix.get(0, 2));
            Assertions.assertEquals(-5L, matrix.get(1, 1));
            Assertions.assertEquals(-7L, matrix.get(2, 0));
        }

        @Test
        public void testUpdateAntiDiagonal_NegatesOnlyDiagonalCells() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            matrix.updateAntiDiagonal(x -> -x);
            assertEquals(1L, matrix.get(0, 0));
            assertEquals(-2L, matrix.get(0, 1));
            assertEquals(-3L, matrix.get(1, 0));
            assertEquals(4L, matrix.get(1, 1));
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            m.updateAntiDiagonal(x -> x * 10L);
            assertEquals(30L, m.get(0, 2));
            assertEquals(50L, m.get(1, 1));
            assertEquals(70L, m.get(2, 0));
            assertEquals(2L, m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateAntiDiagonal_NonSquare() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            Assertions.assertDoesNotThrow(() -> m.updateAntiDiagonal(x -> x * 2L));
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3_OffsetsEveryDiagonalElement() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            m.updateAntiDiagonal(x -> x + 100L);
            assertEquals(103L, m.get(0, 2));
            assertEquals(105L, m.get(1, 1));
            assertEquals(107L, m.get(2, 0));
        }

        @Test
        public void testUpdateAntiDiagonal() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.updateAntiDiagonal(x -> x * 2);
            long[] diag = m.antiDiagonalCopy();
            assertEquals(4L, diag[0]);
            assertEquals(6L, diag[1]);
        }
    }

    @Nested
    class UpdateAllTests {

        @Test
        public void testUpdateAll() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.updateAll(x -> x * 2);
            Assertions.assertEquals(2L, matrix.get(0, 0));
            Assertions.assertEquals(4L, matrix.get(0, 1));
            Assertions.assertEquals(6L, matrix.get(1, 0));
            Assertions.assertEquals(8L, matrix.get(1, 1));
        }

        @Test
        public void testUpdateAllUnarySequentialTallMatrixUsesRowMajorOrder() throws Exception {
            final LongMatrix matrix = LongMatrix.wrap(new long[3][2]);
            final AtomicInteger next = new AtomicInteger();

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> matrix.updateAll(x -> next.incrementAndGet()));

            assertArrayEquals(new long[] { 1L, 2L }, matrix.rowCopy(0));
            assertArrayEquals(new long[] { 3L, 4L }, matrix.rowCopy(1));
            assertArrayEquals(new long[] { 5L, 6L }, matrix.rowCopy(2));
        }

        @Test
        public void testUpdateAllWithIndices() {
            long[][] a = { { 0L, 0L }, { 0L, 0L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.updateAll((i, j) -> (long) (i + j));
            Assertions.assertEquals(0L, matrix.get(0, 0));
            Assertions.assertEquals(1L, matrix.get(0, 1));
            Assertions.assertEquals(1L, matrix.get(1, 0));
            Assertions.assertEquals(2L, matrix.get(1, 1));
        }

        @Test
        public void testUpdateAll_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.updateAll(x -> x * 2L);
            assertEquals(2L, m.get(0, 0));
            assertEquals(4L, m.get(0, 1));
            assertEquals(6L, m.get(1, 0));
            assertEquals(8L, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithIndices() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 0L, 0L }, { 0L, 0L } });
            m.updateAll((i, j) -> (long) (i * 10 + j));
            assertEquals(0L, m.get(0, 0));
            assertEquals(1L, m.get(0, 1));
            assertEquals(10L, m.get(1, 0));
            assertEquals(11L, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.updateAll((i, j) -> i * 10L + j);
            assertEquals(0L, m.get(0, 0));
            assertEquals(1L, m.get(0, 1));
            assertEquals(10L, m.get(1, 0));
            assertEquals(11L, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_UnaryOperator() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.updateAll(x -> x * 2);
            assertEquals(2L, m.get(0, 0));
            assertEquals(4L, m.get(0, 1));
            assertEquals(6L, m.get(1, 0));
            assertEquals(8L, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.updateAll((i, j) -> (i + 1) * 10L + (j + 1));
            assertEquals(11L, m.get(0, 0));
            assertEquals(12L, m.get(0, 1));
            assertEquals(21L, m.get(1, 0));
            assertEquals(22L, m.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testUpdateAll_ExplicitModesTransformEveryValue(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
                matrix.updateAll(value -> value + 10);
                assertEquals(LongMatrix.wrap(new long[][] { { 11L, 12L, 13L }, { 14L, 15L, 16L } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testLongMatrixUpdateAllNullOperator() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix emptyLike = LongMatrix.wrap(new long[][] { {}, {} });
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.LongUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.IntBiFunction<Long, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateMainDiagonal((Throwables.LongUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAntiDiagonal((Throwables.LongUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.LongPredicate<RuntimeException>) null, 0L));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0L));

            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateMainDiagonal((Throwables.LongUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAntiDiagonal((Throwables.LongUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.LongPredicate<RuntimeException>) null, 0L));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0L));
        }

        @Test
        public void testUpdateAll_ZeroColumnsDoNotInvokeOperator() {
            final var matrix = LongMatrix.wrap(new long[2][0]);
            matrix.updateAll(value -> {
                throw new AssertionError("No values should be visited");
            });
            assertEquals(2, matrix.rowCount());
            assertEquals(0, matrix.columnCount());
        }
    }

    @Nested
    class ReplaceIfTests {

        @Test
        public void testReplaceIf() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.replaceIf(x -> x < 3, 0L);
            Assertions.assertEquals(0L, matrix.get(0, 0));
            Assertions.assertEquals(0L, matrix.get(0, 1));
            Assertions.assertEquals(3L, matrix.get(1, 0));
            Assertions.assertEquals(4L, matrix.get(1, 1));
        }

        @Test
        public void testReplaceIfWithIndices() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.replaceIf((i, j) -> i == j, 1L);
            Assertions.assertEquals(1L, matrix.get(0, 0));
            Assertions.assertEquals(2L, matrix.get(0, 1));
            Assertions.assertEquals(3L, matrix.get(1, 0));
            Assertions.assertEquals(1L, matrix.get(1, 1));
        }

        @Test
        public void testReplaceIf_Shape2By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            m.replaceIf(x -> x > 3L, 0L);
            assertEquals(1L, m.get(0, 0));
            assertEquals(2L, m.get(0, 1));
            assertEquals(3L, m.get(0, 2));
            assertEquals(0L, m.get(1, 0)); // was 4
            assertEquals(0L, m.get(1, 1)); // was 5
            assertEquals(0L, m.get(1, 2)); // was 6
        }

        @Test
        public void testReplaceIf_WithIndices() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            m.replaceIf((i, j) -> i == j, 0L); // Replace diagonal
            assertEquals(0L, m.get(0, 0));
            assertEquals(0L, m.get(1, 1));
            assertEquals(0L, m.get(2, 2));
            assertEquals(2L, m.get(0, 1)); // unchanged
        }

        @Test
        public void testReplaceIf_Predicate() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            m.replaceIf(x -> x % 2 == 0, 99L);
            assertEquals(1L, m.get(0, 0));
            assertEquals(99L, m.get(0, 1));
            assertEquals(3L, m.get(0, 2));
            assertEquals(99L, m.get(1, 0));
        }

        @Test
        public void testReplaceIf_BiPredicate() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.replaceIf((i, j) -> i == j, 0L);
            assertEquals(0L, m.get(0, 0));
            assertEquals(2L, m.get(0, 1));
            assertEquals(3L, m.get(1, 0));
            assertEquals(0L, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_Predicate_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.replaceIf(x -> x > 2, 99L);
            assertEquals(1L, m.get(0, 0));
            assertEquals(2L, m.get(0, 1));
            assertEquals(99L, m.get(1, 0));
            assertEquals(99L, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_BiPredicate_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.replaceIf((i, j) -> i == j, 99L);
            assertEquals(99L, m.get(0, 0));
            assertEquals(2L, m.get(0, 1));
            assertEquals(3L, m.get(1, 0));
            assertEquals(99L, m.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testReplaceIf_ExplicitModesReplaceOnlyMatchingValues(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
                matrix.replaceIf(value -> value % 2 == 0, 9L);
                assertEquals(LongMatrix.wrap(new long[][] { { 1L, 9L, 3L }, { 9L, 5L, 9L } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testReplaceIf_ZerosNegativeCells() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { -1L, 2L, -3L }, { 4L, -5L, 6L } });
            matrix.replaceIf(x -> x < 0, 0L);
            assertEquals(0L, matrix.get(0, 0));
            assertEquals(2L, matrix.get(0, 1));
            assertEquals(0L, matrix.get(0, 2));
            assertEquals(4L, matrix.get(1, 0));
            assertEquals(0L, matrix.get(1, 1));
            assertEquals(6L, matrix.get(1, 2));
        }
    }

    @Nested
    class MapTests {

        @Test
        public void testMap() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix squared = matrix.map(x -> x * x);
            Assertions.assertEquals(1L, squared.get(0, 0));
            Assertions.assertEquals(4L, squared.get(0, 1));
            Assertions.assertEquals(9L, squared.get(1, 0));
            Assertions.assertEquals(16L, squared.get(1, 1));
        }

        @Test
        public void testMap_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix result = m.map(x -> x * 2L);
            assertEquals(2L, result.get(0, 0));
            assertEquals(4L, result.get(0, 1));
            assertEquals(6L, result.get(1, 0));
            assertEquals(8L, result.get(1, 1));

            // Original unchanged
            assertEquals(1L, m.get(0, 0));
        }

        @Test
        public void testScalarOperationsWithMap() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });

            // Scalar addition
            LongMatrix addScalar = m.map(x -> x + 10L);
            assertEquals(11L, addScalar.get(0, 0));
            assertEquals(14L, addScalar.get(1, 1));

            // Scalar multiplication
            LongMatrix multiplyScalar = m.map(x -> x * 3L);
            assertEquals(3L, multiplyScalar.get(0, 0));
            assertEquals(12L, multiplyScalar.get(1, 1));

            // Scalar division
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 10L, 20L }, { 30L, 40L } });
            LongMatrix divideScalar = m2.map(x -> x / 10L);
            assertEquals(1L, divideScalar.get(0, 0));
            assertEquals(4L, divideScalar.get(1, 1));
        }

        @Test
        public void testVeryLargeValues() {
            long largeValue = 1_000_000_000_000L; // 1 trillion
            LongMatrix m = LongMatrix.wrap(new long[][] { { largeValue, largeValue * 2 }, { largeValue * 3, largeValue * 4 } });

            assertEquals(largeValue, m.get(0, 0));
            assertEquals(largeValue * 2, m.get(0, 1));
            assertEquals(largeValue * 3, m.get(1, 0));
            assertEquals(largeValue * 4, m.get(1, 1));

            // Test operations with large values
            LongMatrix doubled = m.map(x -> x * 2L);
            assertEquals(largeValue * 2, doubled.get(0, 0));
            assertEquals(largeValue * 8, doubled.get(1, 1));
        }

        @Test
        public void testMap_Shape2By2_ScalesAllCellsAndPreservesSource() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix result = m.map(x -> x * 10);
            assertEquals(10L, result.get(0, 0));
            assertEquals(20L, result.get(0, 1));
            assertEquals(30L, result.get(1, 0));
            assertEquals(40L, result.get(1, 1));
            // Original should be unchanged
            assertEquals(1L, m.get(0, 0));
        }

        @Test
        public void testChainedOperations() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix result = m.map(x -> x * 2).transpose().map(x -> x + 1);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(3L, result.get(0, 0)); // (1*2)+1
            assertEquals(7L, result.get(0, 1)); // (3*2)+1
            assertEquals(5L, result.get(1, 0)); // (2*2)+1
            assertEquals(9L, result.get(1, 1)); // (4*2)+1
        }

        @Test
        public void testMap_DoublesValuesAndPreservesSource() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix result = m.map(x -> x * 2);
            assertEquals(2L, result.get(0, 0));
            assertEquals(4L, result.get(0, 1));
            assertEquals(6L, result.get(1, 0));
            assertEquals(8L, result.get(1, 1));
            // Original should be unchanged
            assertEquals(1L, m.get(0, 0));
        }

        @Test
        public void testMapNullMapper() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.map((Throwables.LongUnaryOperator<RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> matrix.mapToObj((Throwables.LongFunction<String, RuntimeException>) null, String.class));
        }

        @Test
        public void testLongMatrixRowsForZeroColumnMatrix() {
            final LongMatrix matrix = LongMatrix.wrap(new long[][] { {}, {}, {} });
            final List<long[]> rows = matrix.rowStreams().map(LongStream::toArray).toList();

            assertEquals(3, rows.size());
            assertArrayEquals(new long[0], rows.get(0));
            assertArrayEquals(new long[0], rows.get(1));
            assertArrayEquals(new long[0], rows.get(2));
        }

        @Test
        public void testNegativeValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { -1L, -2L }, { -3L, -4L } });

            assertEquals(-1L, m.get(0, 0));
            assertEquals(-4L, m.get(1, 1));

            // Test operations with negative values
            LongMatrix doubled = m.map(x -> x * 2L);
            assertEquals(-2L, doubled.get(0, 0));
            assertEquals(-8L, doubled.get(1, 1));
        }
    }

    @Nested
    class MapToIntTests {

        @Test
        public void testMapToInt() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            IntMatrix intMatrix = matrix.mapToInt(x -> (int) (x % 100));
            Assertions.assertEquals(1, intMatrix.get(0, 0));
            Assertions.assertEquals(4, intMatrix.get(1, 1));
        }

        @Test
        public void testMapToInt_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            IntMatrix result = m.mapToInt(x -> (int) (x * 10));
            assertEquals(10, result.get(0, 0));
            assertEquals(40, result.get(1, 1));
        }

        @Test
        public void testMapToInt_Shape2By2_EveryIntegerValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            IntMatrix result = m.mapToInt(x -> (int) x);
            assertEquals(1, result.get(0, 0));
            assertEquals(2, result.get(0, 1));
            assertEquals(3, result.get(1, 0));
            assertEquals(4, result.get(1, 1));
        }

        @Test
        public void testToIntMatrix_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            IntMatrix result = m.mapToInt(l -> (int) l);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(4, result.get(1, 1));
        }
    }

    @Nested
    class MapToDoubleTests {

        @Test
        public void testMapToDouble() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            DoubleMatrix doubleMatrix = matrix.mapToDouble(x -> Math.sqrt(x));
            Assertions.assertEquals(1.0, doubleMatrix.get(0, 0));
            Assertions.assertEquals(2.0, doubleMatrix.get(1, 1));
        }

        @Test
        public void testMapToDouble_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            DoubleMatrix result = m.mapToDouble(x -> x * 0.1);
            assertEquals(0.1, result.get(0, 0), 0.0001);
            assertEquals(0.4, result.get(1, 1), 0.0001);
        }

        @Test
        public void testMapToDouble_Shape2By2_ScalesFirstRowToDouble() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            DoubleMatrix result = m.mapToDouble(x -> x * 0.5);
            assertEquals(0.5, result.get(0, 0), 0.0001);
            assertEquals(2.0, result.get(1, 1), 0.0001);
        }

        @Test
        public void testMapToDouble_Shape2By2_ConvertsEveryCellToDouble() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            DoubleMatrix result = m.mapToDouble(x -> (double) x);
            assertEquals(1.0, result.get(0, 0), 0.0);
            assertEquals(2.0, result.get(0, 1), 0.0);
            assertEquals(3.0, result.get(1, 0), 0.0);
            assertEquals(4.0, result.get(1, 1), 0.0);
        }

        @Test
        public void testMapToDouble_Shape2By2_ConvertsEveryCellToDouble_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            DoubleMatrix result = m.mapToDouble(l -> (double) l);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0, result.get(0, 0), 0.0001);
            assertEquals(4.0, result.get(1, 1), 0.0001);
        }
    }

    @Nested
    class MapToObjTests {

        @Test
        public void testMapToObj() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            Matrix<String> stringMatrix = matrix.mapToObj(Long::toString, String.class);
            Assertions.assertEquals("1", stringMatrix.get(0, 0));
            Assertions.assertEquals("4", stringMatrix.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            Matrix<String> result = m.mapToObj(x -> "val:" + x, String.class);
            assertEquals("val:1", result.get(0, 0));
            assertEquals("val:4", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_AddsValuePrefixToFirstRow() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            Matrix<String> result = m.mapToObj(x -> "Value:" + x, String.class);
            assertEquals("Value:1", result.get(0, 0));
            assertEquals("Value:4", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_EveryStringValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            Matrix<String> result = m.mapToObj(x -> String.valueOf(x), String.class);
            assertEquals("1", result.get(0, 0));
            assertEquals("2", result.get(0, 1));
            assertEquals("3", result.get(1, 0));
            assertEquals("4", result.get(1, 1));
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testMapToObjArrayRankBoundaryAndPrimitiveTypeNormalization() {
            // Two matrix dimensions leave room for at most 253 dimensions in each element's type.
            final Object highestRankValue = java.lang.reflect.Array.newInstance(Object.class, new int[253]);
            final Class<Object> highestRankType = (Class<Object>) highestRankValue.getClass();
            final Class<?> excessiveRankType = java.lang.reflect.Array.newInstance(Object.class, new int[254]).getClass();

            for (final LongMatrix source : List.of(LongMatrix.empty(), new LongMatrix(new long[0][], 2), LongMatrix.wrap(new long[2][0]),
                    LongMatrix.wrap(new long[] { 1 }))) {
                final AtomicInteger calls = new AtomicInteger();
                for (final Class<?> invalidType : new Class<?>[] { void.class, excessiveRankType }) {
                    assertThrows(IllegalArgumentException.class, () -> source.mapToObj(value -> {
                        calls.incrementAndGet();
                        return null;
                    }, invalidType));
                }
                assertEquals(0, calls.get(), "invalid target types must be rejected before invoking the mapper");

                final Matrix<Object> mapped = source.mapToObj(value -> {
                    calls.incrementAndGet();
                    return highestRankValue;
                }, highestRankType);
                assertEquals(source.rowCount(), mapped.rowCount());
                assertEquals(source.columnCount(), mapped.columnCount());
                assertSame(highestRankType, mapped.elementType());
                assertEquals(source.rowCount() * source.columnCount(), calls.get());
                if (!source.isEmpty()) {
                    assertSame(highestRankValue, mapped.get(0, 0));
                }
            }

            final LongMatrix source = LongMatrix.wrap(new long[] { 1 });
            final Matrix<Long> scalars = source.mapToObj(value -> value, long.class);
            assertSame(Long.class, scalars.elementType());
            assertEquals(1, scalars.get(0, 0).longValue());

            final int[] arrayValue = { 1, 2 };
            final Matrix<int[]> arrays = source.mapToObj(value -> arrayValue, int[].class);
            assertSame(int[].class, arrays.elementType());
            assertSame(arrayValue, arrays.get(0, 0));
        }
    }

    @Nested
    class FillTests {

        @Test
        public void testFill() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.fill(0L);
            Assertions.assertEquals(0L, matrix.get(0, 0));
            Assertions.assertEquals(0L, matrix.get(0, 1));
            Assertions.assertEquals(0L, matrix.get(1, 0));
            Assertions.assertEquals(0L, matrix.get(1, 1));
        }

        @Test
        public void testFill_FillsSquareWithFive() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            matrix.fill(5L);
            assertEquals(5L, matrix.get(0, 0));
            assertEquals(5L, matrix.get(1, 1));
        }

        @Test
        public void testFill_WithValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.fill(99L);
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals(99L, m.get(i, j));
                }
            }
        }

        @Test
        public void testFill_Value() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.fill(99L);
            assertEquals(99L, m.get(0, 0));
            assertEquals(99L, m.get(0, 1));
            assertEquals(99L, m.get(1, 0));
            assertEquals(99L, m.get(1, 1));
        }

        @Test
        public void testFill_Value_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.fill(9L);
            assertEquals(9L, m.get(0, 0));
            assertEquals(9L, m.get(0, 1));
            assertEquals(9L, m.get(1, 0));
            assertEquals(9L, m.get(1, 1));
        }
    }

    @Nested
    class CopyFromTests {

        @Test
        public void testCopyFrom() {
            long[][] a = { { 0L, 0L, 0L }, { 0L, 0L, 0L }, { 0L, 0L, 0L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[][] b = { { 1L, 2L }, { 3L, 4L } };
            matrix.copyFrom(b);
            Assertions.assertEquals(1L, matrix.get(0, 0));
            Assertions.assertEquals(2L, matrix.get(0, 1));
            Assertions.assertEquals(3L, matrix.get(1, 0));
            Assertions.assertEquals(4L, matrix.get(1, 1));
            Assertions.assertEquals(0L, matrix.get(2, 2)); // unchanged
        }

        @Test
        public void testCopyFromWithIndices() {
            long[][] a = { { 0L, 0L, 0L }, { 0L, 0L, 0L }, { 0L, 0L, 0L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[][] b = { { 1L, 2L } };
            matrix.copyFrom(1, 1, b);
            Assertions.assertEquals(0L, matrix.get(0, 0)); // unchanged
            Assertions.assertEquals(1L, matrix.get(1, 1));
            Assertions.assertEquals(2L, matrix.get(1, 2));

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(-1, 0, b));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(0, -1, b));
        }

        @Test
        public void testCopyFrom_Shape3By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 0L, 0L, 0L }, { 0L, 0L, 0L }, { 0L, 0L, 0L } });
            long[][] patch = { { 1L, 2L }, { 3L, 4L } };
            m.copyFrom(patch);
            assertEquals(1L, m.get(0, 0));
            assertEquals(2L, m.get(0, 1));
            assertEquals(3L, m.get(1, 0));
            assertEquals(4L, m.get(1, 1));
            assertEquals(0L, m.get(2, 2)); // unchanged
        }

        @Test
        public void testCopyFromAtPosition() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 0L, 0L, 0L }, { 0L, 0L, 0L }, { 0L, 0L, 0L } });
            long[][] patch = { { 1L, 2L }, { 3L, 4L } };
            m.copyFrom(1, 1, patch);
            assertEquals(0L, m.get(0, 0)); // unchanged
            assertEquals(1L, m.get(1, 1));
            assertEquals(2L, m.get(1, 2));
            assertEquals(3L, m.get(2, 1));
            assertEquals(4L, m.get(2, 2));
        }

        @Test
        public void testCopyFrom_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.copyFrom(new long[][] { { 10L, 20L }, { 30L, 40L } });
            assertEquals(10L, m.get(0, 0));
            assertEquals(20L, m.get(0, 1));
            assertEquals(30L, m.get(1, 0));
            assertEquals(40L, m.get(1, 1));
        }

        // Original unchanged
        @Test
        public void testCopyFromWithOffset() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            m.copyFrom(1, 1, new long[][] { { 99L, 88L } });
            assertEquals(1L, m.get(0, 0));
            assertEquals(99L, m.get(1, 1));
            assertEquals(88L, m.get(1, 2));
        }

        @Test
        public void testCopyFrom_Shape2By2_EveryCellValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            m.copyFrom(new long[][] { { 9L, 8L }, { 7L, 6L } });
            assertEquals(9L, m.get(0, 0));
            assertEquals(8L, m.get(0, 1));
            assertEquals(7L, m.get(1, 0));
            assertEquals(6L, m.get(1, 1));
        }

        @Test
        public void testCopyFrom_WithOffset() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            m.copyFrom(1, 1, new long[][] { { 99L } });
            assertEquals(1L, m.get(0, 0));
            assertEquals(99L, m.get(1, 1));
            assertEquals(9L, m.get(2, 2));
        }

        @Test
        public void testCopyFrom_WithOffset_ArrayLargerThanMatrix() {
            // Test that fill() gracefully handles source arrays larger than the target matrix
            // by copying only what fits (as documented in the javadoc)
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            m.copyFrom(0, 0, new long[][] { { 9L, 8L, 7L } }); // Source array has 3 elements, matrix has 2
            // Should copy only the first 2 elements that fit
            assertEquals(9L, m.get(0, 0));
            assertEquals(8L, m.get(0, 1));
        }

        @Test
        public void testCopyFrom_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            long[][] patch = { { 1L, 2L }, { 3L, 4L } };
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0", "3, 0", "0, -1", "0, 3" })
        public void testCopyFrom_RejectsOffsetsOutsideDestination(final int row, final int column) {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(row, column, new long[][] { { 1L } }));
            assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } }), matrix);
        }
    }

    @Nested
    class CopyTests {

        @Test
        public void testCopy() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);
            LongMatrix copy = matrix.copy();

            Assertions.assertEquals(matrix.rowCount(), copy.rowCount());
            Assertions.assertEquals(matrix.columnCount(), copy.columnCount());
            Assertions.assertEquals(1L, copy.get(0, 0));
            Assertions.assertEquals(4L, copy.get(1, 1));

            copy.set(0, 0, 100L);
            Assertions.assertEquals(1L, matrix.get(0, 0)); // original unchanged
        }

        @Test
        public void testCopy_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals(1L, copy.get(0, 0));

            // Modify copy shouldn't affect original
            copy.set(0, 0, 99L);
            assertEquals(1L, m.get(0, 0));
            assertEquals(99L, copy.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2_DimensionsAndIndependentStorage() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix copy = m.copy();
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(1L, copy.get(0, 0));

            // Verify it's a deep copy
            copy.set(0, 0, 99L);
            assertEquals(1L, m.get(0, 0));
        }

        @Test
        public void testCopy_ModificationsAreIndependent() {
            // Verify that copy() returns a fully independent matrix (no shared row arrays).
            LongMatrix orig = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix copy = orig.copy();
            copy.set(0, 0, 99L);
            assertEquals(1L, orig.get(0, 0));
            assertEquals(99L, copy.get(0, 0));
        }
    }

    @Nested
    class CopyRowsTests {

        @Test
        public void testCopyRows() {
            long[][] a = { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);
            LongMatrix copy = matrix.copyRows(1, 3);

            Assertions.assertEquals(2, copy.rowCount());
            Assertions.assertEquals(2, copy.columnCount());
            Assertions.assertEquals(3L, copy.get(0, 0));
            Assertions.assertEquals(6L, copy.get(1, 1));

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(-1, 2));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(0, 4));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(2, 1));
        }

        @Test
        public void testCopyRows_Shape3By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            LongMatrix subset = m.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertEquals(4L, subset.get(0, 0));
            assertEquals(9L, subset.get(1, 2));
        }

        @Test
        public void testCopyRows_Shape3By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            LongMatrix copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(3L, copy.get(0, 0));
            assertEquals(6L, copy.get(1, 1));
        }

        @Test
        public void testCopyRangesEmpty_ReturnsEmptyMatrix() {
            // An empty row slice keeps the column count (0 x N) and an empty column slice keeps the row
            // count (N x 0); only an empty range in BOTH dimensions collapses to 0 x 0.
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });

            LongMatrix empty = m.copyRows(0, 0);
            Assertions.assertEquals(0, empty.rowCount());
            Assertions.assertEquals(3, empty.columnCount());

            LongMatrix emptyRows = m.copyRegion(1, 1, 0, 3);
            Assertions.assertEquals(0, emptyRows.rowCount());
            Assertions.assertEquals(3, emptyRows.columnCount());

            LongMatrix emptyCols = m.copyRegion(0, 3, 1, 1);
            Assertions.assertEquals(3, emptyCols.rowCount());
            Assertions.assertEquals(0, emptyCols.columnCount());
        }

        @Test
        public void testCopy_InvalidRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 5));
        }

        @Test
        public void testCopyRows_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(2, 1));
        }
    }

    @Nested
    class CopyRegionTests {

        @Test
        public void testCopyRegion() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } };
            LongMatrix matrix = LongMatrix.wrap(a);
            LongMatrix copy = matrix.copyRegion(1, 3, 0, 2);

            Assertions.assertEquals(2, copy.rowCount());
            Assertions.assertEquals(2, copy.columnCount());
            Assertions.assertEquals(4L, copy.get(0, 0));
            Assertions.assertEquals(5L, copy.get(0, 1));
            Assertions.assertEquals(7L, copy.get(1, 0));
            Assertions.assertEquals(8L, copy.get(1, 1));
        }

        @Test
        public void testCopyRegion_Shape3By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            LongMatrix submatrix = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals(2L, submatrix.get(0, 0));
            assertEquals(6L, submatrix.get(1, 1));
        }

        @Test
        public void testCopyRegion_Shape3By3_UpperRightRegion() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            LongMatrix copy = m.copyRegion(1, 2, 1, 3);
            assertEquals(1, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(5L, copy.get(0, 0));
            assertEquals(6L, copy.get(0, 1));
        }

        @Test
        public void testCopyRegion_BottomRightSubmatrix() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            LongMatrix copy = m.copyRegion(1, 3, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(5L, copy.get(0, 0));
            assertEquals(9L, copy.get(1, 1));
        }

        @Test
        public void testCopyRegion_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, 0, 3));
        }
    }

    @Nested
    class ResizeTests {

        @Test
        public void testResize() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix extended = matrix.resize(3, 3);
            assertEquals(1L, extended.get(0, 0));
            assertEquals(2L, extended.get(0, 1));
            assertEquals(0L, extended.get(0, 2));
            assertEquals(0L, extended.get(2, 2));
        }

        @Test
        public void testResizeWithFill() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix extended = matrix.resize(3, 4, 9L);
            assertEquals(1L, extended.get(0, 0));
            assertEquals(9L, extended.get(0, 2));
            assertEquals(9L, extended.get(2, 0));

            LongMatrix truncated = matrix.resize(1, 1, 0L);
            assertEquals(1, truncated.rowCount());
            assertEquals(1, truncated.columnCount());
            assertEquals(1L, truncated.get(0, 0));
        }

        @Test
        public void testResize_Fixture() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix extended = matrix.resize(3, 3);
            Assertions.assertEquals(3, extended.rowCount());
            Assertions.assertEquals(3, extended.columnCount());
            Assertions.assertEquals(1L, extended.get(0, 0));
            Assertions.assertEquals(4L, extended.get(1, 1));
            Assertions.assertEquals(0L, extended.get(2, 2));

            LongMatrix truncated = matrix.resize(1, 1);
            Assertions.assertEquals(1, truncated.rowCount());
            Assertions.assertEquals(1, truncated.columnCount());
            Assertions.assertEquals(1L, truncated.get(0, 0));
        }

        @Test
        public void testResizeWithDefaultValue() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix extended = matrix.resize(3, 3, -1L);
            Assertions.assertEquals(-1L, extended.get(2, 2));
            Assertions.assertEquals(-1L, extended.get(0, 2));
            Assertions.assertEquals(-1L, extended.get(2, 0));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.resize(-1, 2, 0L));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.resize(2, -1, 0L));
        }

        @Test
        public void testResize_Larger() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix extended = m.resize(4, 4);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1L, extended.get(0, 0));
            assertEquals(4L, extended.get(1, 1));
            assertEquals(0L, extended.get(3, 3)); // new cells are 0
        }

        @Test
        public void testResize_Smaller() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            LongMatrix truncated = m.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
            assertEquals(1L, truncated.get(0, 0));
            assertEquals(5L, truncated.get(1, 1));
        }

        @Test
        public void testResize_WithDefaultValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix extended = m.resize(3, 3, -1L);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1L, extended.get(0, 0));
            assertEquals(-1L, extended.get(2, 2)); // new cell
        }

        @Test
        public void testResize_NewSize() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix extended = m.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1L, extended.get(0, 0));
            assertEquals(0L, extended.get(2, 2));
        }

        @Test
        public void testResize_WithDefaultValue_Shape1By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix extended = m.resize(2, 3, 99L);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1L, extended.get(0, 0));
            assertEquals(99L, extended.get(0, 2));
            assertEquals(99L, extended.get(1, 0));
        }

        @Test
        public void testResize_WithDefaultValue_Shape1By2_CustomPaddingValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix extended = m.resize(2, 3, 99L);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1L, extended.get(0, 0));
            assertEquals(99L, extended.get(1, 1));
            assertEquals(99L, extended.get(0, 2));
        }

        @Test
        public void testResize_Smaller_Shape2By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            LongMatrix result = m.resize(1, 2);
            assertEquals(1, result.rowCount());
            assertEquals(2, result.columnCount());
        }

        @Test
        public void testResize_GrowsRowsWhileTrimmingColumns() {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertEquals(LongMatrix.wrap(new long[][] { { 1L }, { 3L }, { 9L } }), matrix.resize(3, 1, 9L));
            assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } }), matrix);
        }

        @Test
        public void testResize_WithNegativeDimensions() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 3, 0L));
            assertThrows(IllegalArgumentException.class, () -> m.resize(3, -1, 0L));
        }
    }

    @Nested
    class PadTests {

        @Test
        public void testPadInAllDirections() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix extended = matrix.pad(1, 1, 1, 1);
            Assertions.assertEquals(4, extended.rowCount());
            Assertions.assertEquals(4, extended.columnCount());
            Assertions.assertEquals(1L, extended.get(1, 1));
            Assertions.assertEquals(4L, extended.get(2, 2));
            Assertions.assertEquals(0L, extended.get(0, 0));
        }

        @Test
        public void testPadInAllDirectionsWithDefaultValue() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix extended = matrix.pad(1, 1, 1, 1, 99L);
            Assertions.assertEquals(99L, extended.get(0, 0));
            Assertions.assertEquals(99L, extended.get(0, 3));
            Assertions.assertEquals(99L, extended.get(3, 0));
            Assertions.assertEquals(99L, extended.get(3, 3));
            Assertions.assertEquals(1L, extended.get(1, 1));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.pad(-1, 0, 0, 0, 0L));
        }

        @Test
        public void testPad_Directional() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            LongMatrix extended = m.pad(1, 1, 2, 2);
            assertEquals(5, extended.rowCount()); // 1 + 3 + 1
            assertEquals(7, extended.columnCount()); // 2 + 3 + 2

            // Original values at offset position
            assertEquals(1L, extended.get(1, 2));
            assertEquals(5L, extended.get(2, 3));

            // New cells are 0
            assertEquals(0L, extended.get(0, 0));
        }

        // Test the rows > columnCount branch (3 rows, 2 columnCount)
        @Test
        public void testPad_DirectionalWithDefault() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            LongMatrix extended = m.pad(1, 1, 1, 1, -1L);
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());

            // Check original values
            assertEquals(1L, extended.get(1, 1));

            // Check new values
            assertEquals(-1L, extended.get(0, 0));
            assertEquals(-1L, extended.get(4, 4));
        }

        @Test
        public void testPad_Directions() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 5L } });
            LongMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(5L, extended.get(1, 1));
            assertEquals(0L, extended.get(0, 0));
        }

        @Test
        public void testPad_DirectionsWithDefault() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 5L } });
            LongMatrix extended = m.pad(1, 1, 1, 1, 9L);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(5L, extended.get(1, 1));
            assertEquals(9L, extended.get(0, 0));
            assertEquals(9L, extended.get(2, 2));
        }

        @Test
        public void testPad_Directional_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1L, extended.get(1, 1));
            assertEquals(0L, extended.get(0, 0));
        }

        @Test
        public void testPad_Directional_WithDefaultValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix extended = m.pad(1, 1, 1, 1, 99L);
            assertEquals(3, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1L, extended.get(1, 1));
            assertEquals(99L, extended.get(0, 0));
        }

        @Test
        public void testPadRepeatFlattenAndForEach_SubRangeEdgeCase() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix extended = matrix.pad(1, 0, 1, 0, 8L);
            LongMatrix repeatedElements = matrix.repeatElements(1, 2);
            LongMatrix repeatedMatrix = matrix.repeatMatrix(2, 1);
            List<Long> visited = new ArrayList<>();

            matrix.forEach(0, 2, 1, 2, visited::add);

            assertEquals(8L, extended.get(0, 0));
            assertArrayEquals(new long[] { 1L, 1L, 2L, 2L, 3L, 3L, 4L, 4L }, repeatedElements.flatten().toArray());
            assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L }, repeatedMatrix.flatten().toArray());
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, matrix.toIntMatrix().flatten().toArray());
            assertEquals(List.of(2L, 4L), visited);
        }

        @Test
        public void testPad_SingleAxisFillPreservesExistingValues() {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L, 9L }, { 3L, 4L, 9L } }), matrix.pad(0, 0, 0, 1, 9L));
            assertEquals(LongMatrix.wrap(new long[][] { { 9L, 1L, 2L }, { 9L, 3L, 4L } }), matrix.pad(0, 0, 1, 0, 9L));
            assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 9L, 9L } }), matrix.pad(0, 1, 0, 0, 9L));
            final var copy = matrix.pad(0, 0, 0, 0, 9L);
            assertEquals(matrix, copy);
            assertNotSame(matrix, copy);
        }

        @Test
        public void testPad_DirectionalWithNegativeValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> m.pad(-1, 1, 1, 1, 0L));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0, 0, 0", "0, -1, 0, 0", "0, 0, -1, 0", "0, 0, 0, -1", "2147483647, 0, 0, 0", "0, 0, 0, 2147483647" })
        public void testPad_RejectsNegativeOrOverflowingDimensions(final int top, final int bottom, final int left, final int right) {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> matrix.pad(top, bottom, left, right));
            assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } }), matrix);
        }
    }

    @Nested
    class FlipHorizontallyInPlaceTests {

        @Test
        public void testReverseH() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.flipHorizontallyInPlace();
            Assertions.assertEquals(3L, matrix.get(0, 0));
            Assertions.assertEquals(2L, matrix.get(0, 1));
            Assertions.assertEquals(1L, matrix.get(0, 2));
            Assertions.assertEquals(6L, matrix.get(1, 0));
        }

        @Test
        public void testFlipHorizontallyInPlace() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            matrix.flipHorizontallyInPlace();
            assertArrayEquals(new long[] { 3, 2, 1 }, matrix.rowView(0));
            assertArrayEquals(new long[] { 6, 5, 4 }, matrix.rowView(1));
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            m.flipHorizontallyInPlace();
            assertEquals(3L, m.get(0, 0));
            assertEquals(2L, m.get(0, 1));
            assertEquals(1L, m.get(0, 2));
            assertEquals(6L, m.get(1, 0));
        }

        @Test
        public void testFlipHorizontallyInPlace_SingleColumn_IsNoOp() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L }, { 2L }, { 3L } });
            m.flipHorizontallyInPlace();
            assertEquals(1L, m.get(0, 0));
            assertEquals(2L, m.get(1, 0));
            assertEquals(3L, m.get(2, 0));
        }
    }

    @Nested
    class FlipVerticallyInPlaceTests {

        @Test
        public void testReverseV() {
            long[][] a = { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            matrix.flipVerticallyInPlace();
            Assertions.assertEquals(5L, matrix.get(0, 0));
            Assertions.assertEquals(6L, matrix.get(0, 1));
            Assertions.assertEquals(3L, matrix.get(1, 0));
            Assertions.assertEquals(1L, matrix.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            matrix.flipVerticallyInPlace();
            assertArrayEquals(new long[] { 7, 8, 9 }, matrix.rowView(0));
            assertArrayEquals(new long[] { 4, 5, 6 }, matrix.rowView(1));
            assertArrayEquals(new long[] { 1, 2, 3 }, matrix.rowView(2));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape3By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            m.flipVerticallyInPlace();
            assertEquals(5L, m.get(0, 0));
            assertEquals(6L, m.get(0, 1));
            assertEquals(3L, m.get(1, 0));
            assertEquals(1L, m.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_SingleRow_IsNoOp() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L } });
            m.flipVerticallyInPlace();
            assertArrayEquals(new long[] { 1L, 2L, 3L }, m.rowCopy(0));
        }
    }

    @Nested
    class FlipHorizontallyTests {

        @Test
        public void testFlipH() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix flipped = matrix.flipHorizontally();
            Assertions.assertEquals(3L, flipped.get(0, 0));
            Assertions.assertEquals(1L, flipped.get(0, 2));
            Assertions.assertEquals(1L, matrix.get(0, 0)); // original unchanged
        }

        @Test
        public void testFlipHorizontally() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            LongMatrix flipped = m.flipHorizontally();
            assertEquals(3L, flipped.get(0, 0));
            assertEquals(2L, flipped.get(0, 1));
            assertEquals(1L, flipped.get(0, 2));
            // Original unchanged
            assertEquals(1L, m.get(0, 0));
        }
    }

    @Nested
    class FlipVerticallyTests {

        @Test
        public void testFlipV() {
            long[][] a = { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix flipped = matrix.flipVertically();
            Assertions.assertEquals(5L, flipped.get(0, 0));
            Assertions.assertEquals(1L, flipped.get(2, 0));
            Assertions.assertEquals(1L, matrix.get(0, 0)); // original unchanged
        }

        @Test
        public void testFlipVertically_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix flipped = m.flipVertically();
            assertEquals(3L, flipped.get(0, 0));
            assertEquals(1L, flipped.get(1, 0));
            // Original unchanged
            assertEquals(1L, m.get(0, 0));
        }

        @Test
        public void testFlipVertically() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            LongMatrix flipped = m.flipVertically();
            assertEquals(5L, flipped.get(0, 0));
            assertEquals(3L, flipped.get(1, 0));
            assertEquals(1L, flipped.get(2, 0));
            // Original unchanged
            assertEquals(1L, m.get(0, 0));
        }
    }

    @Nested
    class Rotate90Tests {

        @Test
        public void testRotate90() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix rotated = matrix.rotate90();
            Assertions.assertEquals(2, rotated.rowCount());
            Assertions.assertEquals(2, rotated.columnCount());
            Assertions.assertEquals(3L, rotated.get(0, 0));
            Assertions.assertEquals(1L, rotated.get(0, 1));
            Assertions.assertEquals(4L, rotated.get(1, 0));
            Assertions.assertEquals(2L, rotated.get(1, 1));
        }

        @Test
        public void testRotate90_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3L, rotated.get(0, 0));
            assertEquals(1L, rotated.get(0, 1));
            assertEquals(4L, rotated.get(1, 0));
            assertEquals(2L, rotated.get(1, 1));
        }

        @Test
        public void testRotate90_VerticalMatrix() {
            // Test the rows > columnCount branch (3 rows, 2 columnCount)
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            LongMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            assertEquals(5L, rotated.get(0, 0));
            assertEquals(3L, rotated.get(0, 1));
            assertEquals(1L, rotated.get(0, 2));
            assertEquals(6L, rotated.get(1, 0));
            assertEquals(4L, rotated.get(1, 1));
            assertEquals(2L, rotated.get(1, 2));
        }

        @Test
        public void testRotate90_Shape2By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            LongMatrix rotated = m.rotate90();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4L, rotated.get(0, 0));
            assertEquals(1L, rotated.get(0, 1));
            assertEquals(6L, rotated.get(2, 0));
        }

        @Test
        public void testRotate90_NonSquare_3x2() {
            // 3x2 rotated 90° CW must yield a 2x3 with first column = last row reading upward.
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            LongMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            assertArrayEquals(new long[] { 5L, 3L, 1L }, rotated.rowCopy(0));
            assertArrayEquals(new long[] { 6L, 4L, 2L }, rotated.rowCopy(1));
        }
    }

    @Nested
    class Rotate180Tests {

        @Test
        public void testRotate180() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix rotated = matrix.rotate180();
            Assertions.assertEquals(2, rotated.rowCount());
            Assertions.assertEquals(2, rotated.columnCount());
            Assertions.assertEquals(4L, rotated.get(0, 0));
            Assertions.assertEquals(3L, rotated.get(0, 1));
            Assertions.assertEquals(2L, rotated.get(1, 0));
            Assertions.assertEquals(1L, rotated.get(1, 1));
        }

        @Test
        public void testRotate180_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix rotated = m.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4L, rotated.get(0, 0));
            assertEquals(3L, rotated.get(0, 1));
            assertEquals(2L, rotated.get(1, 0));
            assertEquals(1L, rotated.get(1, 1));
        }

        @Test
        public void testRotate180_Nx0_AgreesWithRotate90Twice() {
            LongMatrix m = LongMatrix.wrap(new long[3][0]);

            LongMatrix via180 = m.rotate180();
            assertEquals(3, via180.rowCount());
            assertEquals(0, via180.columnCount());

            // Two 90-degree rotations must equal one 180-degree rotation even for a degenerate
            // shape; that only holds because rotate90 preserves the transposed column count.
            LongMatrix viaRotate90Twice = m.rotate90().rotate90();
            assertEquals(3, viaRotate90Twice.rowCount());
            assertEquals(0, viaRotate90Twice.columnCount());
        }
    }

    @Nested
    class Rotate270Tests {

        @Test
        public void testRotate270() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix rotated = matrix.rotate270();
            Assertions.assertEquals(2, rotated.rowCount());
            Assertions.assertEquals(2, rotated.columnCount());
            Assertions.assertEquals(2L, rotated.get(0, 0));
            Assertions.assertEquals(4L, rotated.get(0, 1));
            Assertions.assertEquals(1L, rotated.get(1, 0));
            Assertions.assertEquals(3L, rotated.get(1, 1));
        }

        @Test
        public void testRotate270_VerticalMatrix() {
            // Test the rows > columnCount branch (3 rows, 2 columnCount)
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            LongMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            assertEquals(2L, rotated.get(0, 0));
            assertEquals(4L, rotated.get(0, 1));
            assertEquals(6L, rotated.get(0, 2));
            assertEquals(1L, rotated.get(1, 0));
            assertEquals(3L, rotated.get(1, 1));
            assertEquals(5L, rotated.get(1, 2));
        }

        @Test
        public void testRotate270_Shape2By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            LongMatrix rotated = m.rotate270();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3L, rotated.get(0, 0));
            assertEquals(6L, rotated.get(0, 1));
        }

        @Test
        public void testRotate270_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(2L, rotated.get(0, 0));
            assertEquals(4L, rotated.get(0, 1));
            assertEquals(1L, rotated.get(1, 0));
            assertEquals(3L, rotated.get(1, 1));
        }

        @Test
        public void testRotate270_NonSquare_3x2() {
            // 3x2 rotated 270° CW (= 90° CCW) must yield a 2x3 with first row = original column 1.
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            LongMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            assertArrayEquals(new long[] { 2L, 4L, 6L }, rotated.rowCopy(0));
            assertArrayEquals(new long[] { 1L, 3L, 5L }, rotated.rowCopy(1));
        }
    }

    @Nested
    class TransposeTests {

        @Test
        public void testTranspose() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix transposed = matrix.transpose();
            Assertions.assertEquals(3, transposed.rowCount());
            Assertions.assertEquals(2, transposed.columnCount());
            Assertions.assertEquals(1L, transposed.get(0, 0));
            Assertions.assertEquals(4L, transposed.get(0, 1));
            Assertions.assertEquals(2L, transposed.get(1, 0));
            Assertions.assertEquals(5L, transposed.get(1, 1));
        }

        @Test
        public void testTranspose_Shape2By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            LongMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1L, transposed.get(0, 0));
            assertEquals(4L, transposed.get(0, 1));
            assertEquals(2L, transposed.get(1, 0));
            assertEquals(5L, transposed.get(1, 1));
            assertEquals(3L, transposed.get(2, 0));
            assertEquals(6L, transposed.get(2, 1));
        }

        @Test
        public void testTranspose_VerticalMatrix() {
            // Test the rows > columnCount branch (3 rows, 2 columnCount)
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            LongMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(3, transposed.columnCount());
            assertEquals(1L, transposed.get(0, 0));
            assertEquals(3L, transposed.get(0, 1));
            assertEquals(5L, transposed.get(0, 2));
            assertEquals(2L, transposed.get(1, 0));
            assertEquals(4L, transposed.get(1, 1));
            assertEquals(6L, transposed.get(1, 2));
        }

        @Test
        public void testTranspose_Square() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1L, transposed.get(0, 0));
            assertEquals(3L, transposed.get(0, 1));
            assertEquals(2L, transposed.get(1, 0));
            assertEquals(4L, transposed.get(1, 1));
        }

        @Test
        public void testTranspose_NonSquare_3x2() {
            // Verify transpose for the rowCount > columnCount branch.
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            LongMatrix t = m.transpose();
            assertEquals(2, t.rowCount());
            assertEquals(3, t.columnCount());
            assertArrayEquals(new long[] { 1L, 3L, 5L }, t.rowCopy(0));
            assertArrayEquals(new long[] { 2L, 4L, 6L }, t.rowCopy(1));
        }

        @Test
        public void testTranspose_Nx0_PreservesTransposedShape() {
            // An N x 0 matrix transposes to 0 x N: no elements, but the column count survives.
            LongMatrix t = LongMatrix.wrap(new long[3][0]).transpose();
            assertEquals(0, t.rowCount());
            assertEquals(3, t.columnCount());
        }

        @Test
        public void testSingleRowOperations() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L, 4L, 5L } });
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());

            LongMatrix transposed = m.transpose();
            assertEquals(5, transposed.rowCount());
            assertEquals(1, transposed.columnCount());
        }

        @Test
        public void testSingleColumnOperations() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L }, { 2L }, { 3L } });
            assertEquals(3, m.rowCount());
            assertEquals(1, m.columnCount());

            LongMatrix transposed = m.transpose();
            assertEquals(1, transposed.rowCount());
            assertEquals(3, transposed.columnCount());
        }
    }

    @Nested
    class ReshapeAndPadTests {

        @Test
        public void testReshape_Empty() {
            LongMatrix empty = LongMatrix.empty();
            LongMatrix reshaped = empty.reshapeAndPad(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }

        @Test
        public void testReshape_ExpandWithZeroFill() {
            // Test that reshape() can expand to larger dimensions, filling new cells with zeros
            // as documented in the javadoc
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix reshaped = m.reshapeAndPad(3, 3); // Expand from 2x2 (4 elements) to 3x3 (9 elements)
            assertEquals(3, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            // Verify original elements are preserved
            assertEquals(1L, reshaped.get(0, 0));
            assertEquals(2L, reshaped.get(0, 1));
            assertEquals(3L, reshaped.get(0, 2));
            assertEquals(4L, reshaped.get(1, 0));
            // Verify new cells are filled with zeros
            assertEquals(0L, reshaped.get(1, 1));
            assertEquals(0L, reshaped.get(1, 2));
            assertEquals(0L, reshaped.get(2, 0));
            assertEquals(0L, reshaped.get(2, 1));
            assertEquals(0L, reshaped.get(2, 2));
        }

        @Test
        public void testReshape_GrowLargerThanInput_PadsWithZeros() {
            // 2x3 reshaped to 2x4 should preserve order and pad the trailing slots with 0L.
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            LongMatrix reshaped = m.reshapeAndPad(2, 4);
            assertEquals(2, reshaped.rowCount());
            assertEquals(4, reshaped.columnCount());
            assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, reshaped.rowCopy(0));
            assertArrayEquals(new long[] { 5L, 6L, 0L, 0L }, reshaped.rowCopy(1));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 2", "2, -1", "1, 1" })
        public void testReshapeAndPad_RejectsNegativeAndInsufficientShapes(final int rows, final int columns) {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> matrix.reshapeAndPad(rows, columns));
            assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } }), matrix);
        }
    }

    @Nested
    class RepeatElementsTests {

        @Test
        public void testRepeatElements() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix repeated = matrix.repeatElements(2, 3);
            Assertions.assertEquals(4, repeated.rowCount());
            Assertions.assertEquals(6, repeated.columnCount());
            Assertions.assertEquals(1L, repeated.get(0, 0));
            Assertions.assertEquals(1L, repeated.get(0, 2));
            Assertions.assertEquals(1L, repeated.get(1, 0));
            Assertions.assertEquals(2L, repeated.get(0, 3));
            Assertions.assertEquals(4L, repeated.get(3, 5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(0, 1));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(1, 0));
        }

        @Test
        public void testRepeatElements_RepeatsCellsTwiceVerticallyAndThreeTimesHorizontally() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix repeated = matrix.repeatElements(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertArrayEquals(new long[] { 1, 1, 1, 2, 2, 2 }, repeated.rowView(0));
            assertArrayEquals(new long[] { 1, 1, 1, 2, 2, 2 }, repeated.rowView(1));
            assertArrayEquals(new long[] { 3, 3, 3, 4, 4, 4 }, repeated.rowView(2));
            assertArrayEquals(new long[] { 3, 3, 3, 4, 4, 4 }, repeated.rowView(3));
        }

        @Test
        public void testRepeatElements_Shape1By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix repeated = m.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals(1L, repeated.get(0, 0));
            assertEquals(1L, repeated.get(0, 1));
            assertEquals(1L, repeated.get(0, 2));
            assertEquals(2L, repeated.get(0, 3));
            assertEquals(2L, repeated.get(0, 4));
            assertEquals(2L, repeated.get(0, 5));
            assertEquals(1L, repeated.get(1, 0)); // second row same as first
        }

        @Test
        public void testRepeatElements_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix result = m.repeatElements(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1L, result.get(0, 0));
            assertEquals(1L, result.get(0, 1));
            assertEquals(1L, result.get(1, 0));
            assertEquals(4L, result.get(3, 3));
        }

        @Test
        public void testRepeatElements_Shape2By2_RepeatedBlockAndAdjacentValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix result = m.repeatElements(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1L, result.get(0, 0));
            assertEquals(1L, result.get(0, 1));
            assertEquals(1L, result.get(1, 0));
            assertEquals(1L, result.get(1, 1));
            assertEquals(2L, result.get(0, 2));
        }

        @Test
        public void testRepeatElements_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            final var result = matrix.repeatElements(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, 0L);
            assertEquals(1L, matrix.get(0, 0));
        }

        @Test
        public void testRepeatElements_InvalidArguments() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatElements_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(rows, columns));
        }
    }

    @Nested
    class RepeatMatrixTests {

        @Test
        public void testRepeatMatrix() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix repeated = matrix.repeatMatrix(2, 3);
            Assertions.assertEquals(4, repeated.rowCount());
            Assertions.assertEquals(6, repeated.columnCount());
            Assertions.assertEquals(1L, repeated.get(0, 0));
            Assertions.assertEquals(1L, repeated.get(0, 2));
            Assertions.assertEquals(1L, repeated.get(0, 4));
            Assertions.assertEquals(1L, repeated.get(2, 0));
            Assertions.assertEquals(4L, repeated.get(3, 5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(0, 1));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(1, 0));
        }

        @Test
        public void testRepeatMatrix_TilesTwiceVerticallyAndThreeTimesHorizontally() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix repeated = matrix.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertArrayEquals(new long[] { 1, 2, 1, 2, 1, 2 }, repeated.rowView(0));
            assertArrayEquals(new long[] { 3, 4, 3, 4, 3, 4 }, repeated.rowView(1));
        }

        @Test
        public void testRepeatMatrix_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix repeated = m.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals(1L, repeated.get(0, 0));
            assertEquals(2L, repeated.get(0, 1));
            assertEquals(1L, repeated.get(0, 2)); // repeat starts
            assertEquals(2L, repeated.get(0, 3));

            assertEquals(3L, repeated.get(1, 0));
            assertEquals(4L, repeated.get(1, 1));

            // Check vertical repeat
            assertEquals(1L, repeated.get(2, 0)); // vertical repeat starts
            assertEquals(2L, repeated.get(2, 1));
        }

        @Test
        public void testRepeatMatrix_Shape1By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix result = m.repeatMatrix(2, 2);
            assertEquals(2, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1L, result.get(0, 0));
            assertEquals(2L, result.get(0, 1));
            assertEquals(1L, result.get(0, 2));
            assertEquals(1L, result.get(1, 0));
        }

        @Test
        public void testRepeatMatrix_Shape1By2_RepeatsRowInBothDirections() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix result = m.repeatMatrix(2, 2);
            assertEquals(2, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1L, result.get(0, 0));
            assertEquals(2L, result.get(0, 1));
            assertEquals(1L, result.get(0, 2));
            assertEquals(2L, result.get(0, 3));
        }

        @Test
        public void testRepeatMatrix_DegenerateShapesPreserveDimensionsAndValidation() {
            final LongMatrix noRows = new LongMatrix(new long[0][], 2);
            final LongMatrix noColumns = LongMatrix.wrap(new long[][] { {}, {} });

            // Large repeat factors must not cause repeated work when there are no cells to copy.
            // Keep the counts bounded so this shape regression remains safe on older implementations.
            final LongMatrix repeatedRows = noRows.repeatMatrix(100_000, 3);
            assertEquals(0, repeatedRows.rowCount());
            assertEquals(6, repeatedRows.columnCount());

            final LongMatrix repeatedColumns = noColumns.repeatMatrix(3, 100_000);
            assertEquals(6, repeatedColumns.rowCount());
            assertEquals(0, repeatedColumns.columnCount());
            for (int i = 0; i < repeatedColumns.rowCount(); i++) {
                assertEquals(0, repeatedColumns.rowView(i).length);
            }
            assertSame(LongMatrix.empty(), LongMatrix.empty().repeatMatrix(100_000, 100_000));

            assertThrows(IllegalArgumentException.class, () -> noRows.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> noColumns.repeatMatrix(1, -1));
            assertThrows(IllegalArgumentException.class, () -> noRows.repeatMatrix(1, Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> noColumns.repeatMatrix(Integer.MAX_VALUE, 1));
        }

        @Test
        public void testRepeatMatrix_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            final var result = matrix.repeatMatrix(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, 0L);
            assertEquals(1L, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_OnlyColumnsRepeated() {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            final var repeated = matrix.repeatMatrix(1, 2);
            assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L, 1L, 2L }, { 3L, 4L, 3L, 4L } }), repeated);
            repeated.set(0, 0, 3L);
            assertEquals(1L, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidArguments() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatMatrix_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(rows, columns));
            assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } }), matrix);
        }
    }

    @Nested
    class FlattenTests {

        @Test
        public void testFlatten() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongList flat = matrix.flatten();
            Assertions.assertEquals(6, flat.size());
            Assertions.assertEquals(1L, flat.get(0));
            Assertions.assertEquals(2L, flat.get(1));
            Assertions.assertEquals(3L, flat.get(2));
            Assertions.assertEquals(4L, flat.get(3));
            Assertions.assertEquals(5L, flat.get(4));
            Assertions.assertEquals(6L, flat.get(5));
        }

        @Test
        public void testFlatten_SquareRowMajorList() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongList list = matrix.flatten();
            assertEquals(LongList.of(1L, 2L, 3L, 4L), list);
        }

        @Test
        public void testFlatten_Shape3By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            LongList flat = m.flatten();
            assertEquals(9, flat.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1L, flat.get(i));
            }
        }

        @Test
        public void testFlatten_Shape2By3_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            LongList flat = m.flatten();
            assertEquals(6, flat.size());
            assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, flat.toArray());
        }

        @Test
        public void testFlatten_Shape2By2_EveryListElement() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongList flat = m.flatten();
            assertEquals(4, flat.size());
            assertEquals(1L, flat.get(0));
            assertEquals(2L, flat.get(1));
            assertEquals(3L, flat.get(2));
            assertEquals(4L, flat.get(3));
        }

        @Test
        public void testFlatten_Empty() {
            LongMatrix empty = LongMatrix.empty();
            LongList flat = empty.flatten();
            assertTrue(flat.isEmpty());
        }

        @Test
        public void testFlatten_Empty_EmptyMatrix() {
            LongMatrix m = LongMatrix.empty();
            LongList flat = m.flatten();
            assertEquals(0, flat.size());
        }
    }

    @Nested
    class MutateViaFlatArrayTests {

        @Test
        public void testFlatOp() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            List<Long> collected = new ArrayList<>();
            matrix.mutateViaFlatArray(row -> {
                for (long val : row) {
                    collected.add(val);
                }
            });

            Assertions.assertEquals(4, collected.size());
            Assertions.assertTrue(collected.contains(1L));
            Assertions.assertTrue(collected.contains(4L));
        }

        @Test
        public void testMutateViaFlatArray_SortsAllCellsInRowOrder() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 5, 3 }, { 4, 1 } });
            matrix.mutateViaFlatArray(arr -> java.util.Arrays.sort(arr));
            assertEquals(1L, matrix.get(0, 0));
            assertEquals(3L, matrix.get(0, 1));
            assertEquals(4L, matrix.get(1, 0));
            assertEquals(5L, matrix.get(1, 1));
        }

        @Test
        public void testMutateViaFlatArray_Shape3By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            List<Long> sums = new ArrayList<>();
            m.mutateViaFlatArray(row -> {
                long sum = 0;
                for (long val : row) {
                    sum += val;
                }
                sums.add(sum);
            });
            assertEquals(1, sums.size());
            assertEquals(45L, sums.get(0).longValue());
        }

        @Test
        public void testMutateViaFlatArray_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            final int[] count = { 0 };
            m.mutateViaFlatArray(row -> count[0] += row.length);
            assertEquals(4, count[0]);
        }

        // LongMatrix doesn't have mapToFloat method - removed test
        // @Test
        // public void testToFloatMatrix() {
        //     LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
        //     FloatMatrix result = m.mapToFloat(l -> (float) l);
        @Test
        public void testMutateViaFlatArray_Shape2By2_CallbackSumsFlatValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            final long[] sum = { 0L };
            m.mutateViaFlatArray(row -> {
                for (long val : row) {
                    sum[0] += val;
                }
            });
            assertEquals(10L, sum[0]);
        }

        @Test
        public void testMutateViaFlatArray() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            AtomicInteger count = new AtomicInteger(0);
            m.mutateViaFlatArray(row -> count.addAndGet(row.length));
            assertEquals(4, count.get());
        }
    }

    @Nested
    class StackVerticallyTests {

        @Test
        public void testVstack() {
            long[][] a = { { 1L, 2L, 3L } };
            long[][] b = { { 4L, 5L, 6L }, { 7L, 8L, 9L } };
            LongMatrix matrixA = LongMatrix.wrap(a);
            LongMatrix matrixB = LongMatrix.wrap(b);

            LongMatrix stacked = matrixA.stackVertically(matrixB);
            Assertions.assertEquals(3, stacked.rowCount());
            Assertions.assertEquals(3, stacked.columnCount());
            Assertions.assertEquals(1L, stacked.get(0, 0));
            Assertions.assertEquals(4L, stacked.get(1, 0));
            Assertions.assertEquals(9L, stacked.get(2, 2));

            LongMatrix differentCols = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.stackVertically(differentCols));
        }

        @Test
        public void testStackVertically() {
            LongMatrix matrix1 = LongMatrix.wrap(new long[][] { { 1, 2, 3 } });
            LongMatrix matrix2 = LongMatrix.wrap(new long[][] { { 4, 5, 6 }, { 7, 8, 9 } });
            LongMatrix stacked = matrix1.stackVertically(matrix2);
            assertEquals(3, stacked.rowCount());
            assertArrayEquals(new long[] { 1, 2, 3 }, stacked.rowView(0));
            assertArrayEquals(new long[] { 4, 5, 6 }, stacked.rowView(1));
            assertArrayEquals(new long[] { 7, 8, 9 }, stacked.rowView(2));
        }

        @Test
        public void testStackVertically_Shape2By3() {
            final LongMatrix top = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            final LongMatrix bottom = LongMatrix.wrap(new long[][] { { 7, 8, 9 }, { 10, 11, 12 } });
            final LongMatrix stacked = top.stackVertically(bottom);
            assertEquals(LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 }, { 10, 11, 12 } }), stacked);
            assertEquals(LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 } }), top);
            assertEquals(LongMatrix.wrap(new long[][] { { 7, 8, 9 }, { 10, 11, 12 } }), bottom);
        }

        @Test
        public void testStackVertically_DifferentColumnCounts() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_Shape2By2() {
            final LongMatrix top = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            final LongMatrix bottom = LongMatrix.wrap(new long[][] { { 5, 6 }, { 7, 8 } });
            final LongMatrix stacked = top.stackVertically(bottom);
            assertEquals(LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 }, { 5, 6 }, { 7, 8 } }), stacked);
            assertEquals(LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } }), top);
            assertEquals(LongMatrix.wrap(new long[][] { { 5, 6 }, { 7, 8 } }), bottom);
        }

        @Test
        public void testStackVertically_DifferentColumnCount() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L, 4L, 5L } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_Shape1By2() {
            final LongMatrix top = LongMatrix.wrap(new long[][] { { 1, 2 } });
            final LongMatrix bottom = LongMatrix.wrap(new long[][] { { 3, 4 } });
            final LongMatrix stacked = top.stackVertically(bottom);
            assertEquals(LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } }), stacked);
            assertEquals(LongMatrix.wrap(new long[][] { { 1, 2 } }), top);
            assertEquals(LongMatrix.wrap(new long[][] { { 3, 4 } }), bottom);
        }

        @Test
        public void testStackVertically_IncompatibleCols() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_ClonesRows_Independent() {
            // Verify that stackVertically uses cloned rows, so modifying source after the call
            // does not affect the stacked result.
            long[][] aData = { { 1L, 2L } };
            long[][] bData = { { 3L, 4L } };
            LongMatrix a = LongMatrix.wrap(aData);
            LongMatrix b = LongMatrix.wrap(bData);
            LongMatrix stacked = a.stackVertically(b);

            // Mutate source data after stacking; stacked should NOT change.
            aData[0][0] = -1L;
            bData[0][1] = -1L;
            assertEquals(1L, stacked.get(0, 0));
            assertEquals(4L, stacked.get(1, 1));
        }

        @Test
        public void testLongMatrixBinaryOpsRejectNullMatrix() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L } });
            assertThrows(IllegalArgumentException.class, () -> matrix.stackVertically(null));
            assertThrows(IllegalArgumentException.class, () -> matrix.stackHorizontally(null));
            assertThrows(IllegalArgumentException.class, () -> matrix.add(null));
            assertThrows(IllegalArgumentException.class, () -> matrix.subtract(null));
            assertThrows(IllegalArgumentException.class, () -> matrix.matrixMultiply(null));
        }
    }

    @Nested
    class StackHorizontallyTests {

        @Test
        public void testHstack() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            long[][] b = { { 5L }, { 6L } };
            LongMatrix matrixA = LongMatrix.wrap(a);
            LongMatrix matrixB = LongMatrix.wrap(b);

            LongMatrix stacked = matrixA.stackHorizontally(matrixB);
            Assertions.assertEquals(2, stacked.rowCount());
            Assertions.assertEquals(3, stacked.columnCount());
            Assertions.assertEquals(1L, stacked.get(0, 0));
            Assertions.assertEquals(5L, stacked.get(0, 2));
            Assertions.assertEquals(6L, stacked.get(1, 2));

            LongMatrix differentRows = LongMatrix.wrap(new long[][] { { 1L } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.stackHorizontally(differentRows));
        }

        @Test
        public void testStackHorizontally_PreservesEveryAppendedColumn() {
            LongMatrix matrix1 = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix matrix2 = LongMatrix.wrap(new long[][] { { 5 }, { 6 } });
            LongMatrix stacked = matrix1.stackHorizontally(matrix2);
            assertEquals(2, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertArrayEquals(new long[] { 1, 2, 5 }, stacked.rowView(0));
            assertArrayEquals(new long[] { 3, 4, 6 }, stacked.rowView(1));
        }

        @Test
        public void testStackHorizontally_Shape2By2() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5L, 6L }, { 7L, 8L } });
            LongMatrix stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1L, stacked.get(0, 0));
            assertEquals(5L, stacked.get(0, 2));
            assertEquals(8L, stacked.get(1, 3));
        }

        @Test
        public void testStackHorizontally_DifferentRowCounts() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5L, 6L } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_Shape2By2_DimensionsAndBothInputs() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5L, 6L }, { 7L, 8L } });
            LongMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1L, stacked.get(0, 0));
            assertEquals(2L, stacked.get(0, 1));
            assertEquals(5L, stacked.get(0, 2));
            assertEquals(6L, stacked.get(0, 3));
        }

        @Test
        public void testStackHorizontally_DifferentRowCount() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L, 4L }, { 5L, 6L } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_Shape2By1() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L }, { 3L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 2L }, { 4L } });
            LongMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1L, stacked.get(0, 0));
            assertEquals(2L, stacked.get(0, 1));
        }

        @Test
        public void testStackHorizontally_IncompatibleRows() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 2L }, { 3L } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L }, { 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L }, { 4L } });
            LongMatrix result = m1.stackHorizontally(m2);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1L, result.get(0, 0));
            assertEquals(3L, result.get(0, 1));
        }

        @Test
        public void testStackHorizontally_IncompatibleRows_Shape1By1_Validation() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L }, { 4L } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_RejectsLogicalWidthOverflowWithoutAllocation() {
            final var huge = LongMatrix.wrap(new long[0][0]).resize(0, Integer.MAX_VALUE);
            final var extra = LongMatrix.wrap(new long[0][0]).resize(0, 1);
            assertThrows(IllegalArgumentException.class, () -> huge.stackHorizontally(extra));
            assertEquals(Integer.MAX_VALUE, huge.columnCount());
        }
    }

    @Nested
    class AddTests {

        @Test
        public void testAdd() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            long[][] b = { { 5L, 6L }, { 7L, 8L } };
            LongMatrix matrixA = LongMatrix.wrap(a);
            LongMatrix matrixB = LongMatrix.wrap(b);

            LongMatrix sum = matrixA.add(matrixB);
            Assertions.assertEquals(6L, sum.get(0, 0));
            Assertions.assertEquals(8L, sum.get(0, 1));
            Assertions.assertEquals(10L, sum.get(1, 0));
            Assertions.assertEquals(12L, sum.get(1, 1));

            LongMatrix differentShape = LongMatrix.wrap(new long[][] { { 1L } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.add(differentShape));
        }

        @Test
        public void testAdd_SquareAdditionChecksEveryCell() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5, 6 }, { 7, 8 } });
            LongMatrix sum = m1.add(m2);
            assertEquals(6L, sum.get(0, 0));
            assertEquals(8L, sum.get(0, 1));
            assertEquals(10L, sum.get(1, 0));
            assertEquals(12L, sum.get(1, 1));
        }

        @Test
        public void testAdd_DifferentDimensions() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testAdd_WithLargeValues() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1000000000000L, 2000000000000L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3000000000000L, 4000000000000L } });
            LongMatrix sum = m1.add(m2);
            assertEquals(4000000000000L, sum.get(0, 0));
            assertEquals(6000000000000L, sum.get(0, 1));
        }

        // Element-wise multiplication
        @Test
        public void testArithmeticEdgeCases() {
            // Test with zero matrix
            LongMatrix zeros = LongMatrix.wrap(new long[][] { { 0L, 0L }, { 0L, 0L } });
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });

            LongMatrix addZero = m.add(zeros);
            assertEquals(m, addZero);

            LongMatrix subtractZero = m.subtract(zeros);
            assertEquals(m, subtractZero);

            // Test multiplication with zero matrix
            LongMatrix multiplyZero = m.matrixMultiply(zeros);
            assertEquals(zeros, multiplyZero);

            // Test addition commutativity
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5L, 6L }, { 7L, 8L } });
            assertEquals(m1.add(m2), m2.add(m1));

            // Test subtraction anti-commutativity
            LongMatrix diff1 = m1.subtract(m2);
            LongMatrix diff2 = m2.subtract(m1);
            assertEquals(diff1.get(0, 0), -diff2.get(0, 0));
            assertEquals(diff1.get(1, 1), -diff2.get(1, 1));
        }

        // Overflow wraps around to negative
        @Test
        public void testLargeMatrixArithmetic() {
            // Test with larger matrices
            long[][] arr1 = new long[10][10];
            long[][] arr2 = new long[10][10];

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    arr1[i][j] = i * 10 + j + 1;
                    arr2[i][j] = (i * 10 + j + 1) * 2;
                }
            }

            LongMatrix large1 = LongMatrix.wrap(arr1);
            LongMatrix large2 = LongMatrix.wrap(arr2);

            // Test addition
            LongMatrix largeSum = large1.add(large2);
            assertEquals(3L, largeSum.get(0, 0)); // 1 + 2 = 3
            assertEquals(300L, largeSum.get(9, 9)); // 100 + 200 = 300

            // Test sum of all elements
            long totalSum = largeSum.rowMajorStream().sum();
            assertEquals(15150L, totalSum); // 3*(1+2+...+100) = 3*5050 = 15150
        }

        @Test
        public void testLargeValueArithmetic() {
            // Test addition with large values
            long billion = 1_000_000_000L;
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { billion * 100, billion * 200 } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { billion * 300, billion * 400 } });

            LongMatrix sum = m1.add(m2);
            assertEquals(billion * 400, sum.get(0, 0));
            assertEquals(billion * 600, sum.get(0, 1));

            // Test subtraction with large values
            LongMatrix diff = m2.subtract(m1);
            assertEquals(billion * 200, diff.get(0, 0));
            assertEquals(billion * 200, diff.get(0, 1));
        }

        @Test
        public void testAdd_DifferentDimensions_Shape1By2_Validation() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L, 4L }, { 5L, 6L } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testAdd_Shape2By2_EveryCellSum() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 10L, 20L }, { 30L, 40L } });
            LongMatrix result = m1.add(m2);
            assertEquals(11L, result.get(0, 0));
            assertEquals(22L, result.get(0, 1));
            assertEquals(33L, result.get(1, 0));
            assertEquals(44L, result.get(1, 1));
        }

        @Test
        public void testAdd_AllFourCellValues() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5L, 6L }, { 7L, 8L } });
            LongMatrix result = m1.add(m2);
            assertEquals(6L, result.get(0, 0));
            assertEquals(8L, result.get(0, 1));
            assertEquals(10L, result.get(1, 0));
            assertEquals(12L, result.get(1, 1));
        }

        @Test
        public void testAdd_IncompatibleDimensions() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testAdd_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
                final var right = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
                assertEquals(LongMatrix.wrap(new long[][] { { 2L, 4L, 6L }, { 8L, 10L, 12L } }), left.add(right));
                assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } }), left);
                assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testLongOverflow() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, 1L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, Long.MAX_VALUE } });

            // Addition overflow
            LongMatrix sum = m1.add(m2);
            // Overflow wraps around to negative
            assertTrue(sum.get(0, 0) < 0);

            // Test max and min values
            LongMatrix extremes = LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, Long.MIN_VALUE } });
            assertEquals(Long.MAX_VALUE, extremes.get(0, 0));
            assertEquals(Long.MIN_VALUE, extremes.get(0, 1));
        }
    }

    @Nested
    class AddExactTests {

        @Test
        public void testAddExactSubtractExact_ShapeMismatchMessageMatchesAddAndSubtract() {
            final LongMatrix noRows = new LongMatrix(new long[0][], 3);
            final LongMatrix noColumns = LongMatrix.wrap(new long[][] { {}, {} });

            final IllegalArgumentException addExact = assertThrows(IllegalArgumentException.class, () -> noRows.addExact(noColumns));
            final IllegalArgumentException add = assertThrows(IllegalArgumentException.class, () -> noRows.add(noColumns));
            assertEquals(add.getMessage(), addExact.getMessage());
            assertTrue(addExact.getMessage().startsWith("Cannot add matrices with different shapes"), addExact.getMessage());

            final IllegalArgumentException subtractExact = assertThrows(IllegalArgumentException.class, () -> noRows.subtractExact(noColumns));
            final IllegalArgumentException subtract = assertThrows(IllegalArgumentException.class, () -> noRows.subtract(noColumns));
            assertEquals(subtract.getMessage(), subtractExact.getMessage());
            assertTrue(subtractExact.getMessage().startsWith("Cannot subtract matrices with different shapes"), subtractExact.getMessage());

            assertThrows(IllegalArgumentException.class, () -> noRows.addExact(null));
            assertThrows(IllegalArgumentException.class, () -> noRows.subtractExact(null));
        }

        @Test
        public void testAddExactSubtractExact_DegenerateShapesAndOverflow() {
            final LongMatrix noRows = new LongMatrix(new long[0][], 3);
            assertEquals(0, noRows.addExact(noRows).rowCount());
            assertEquals(3, noRows.addExact(noRows).columnCount());
            assertEquals(3, noRows.subtractExact(noRows).columnCount());

            final LongMatrix noColumns = LongMatrix.wrap(new long[][] { {}, {} });
            assertEquals(2, noColumns.addExact(noColumns).rowCount());
            assertEquals(0, noColumns.subtractExact(noColumns).columnCount());
            assertSame(LongMatrix.empty(), LongMatrix.empty().addExact(LongMatrix.empty()));

            final LongMatrix max = LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, 0L } });
            final LongMatrix one = LongMatrix.wrap(new long[][] { { 1L, 1L } });
            assertThrows(ArithmeticException.class, () -> max.addExact(one));
            assertEquals(Long.MAX_VALUE - 1, max.subtractExact(one).get(0, 0));
            assertThrows(ArithmeticException.class, () -> LongMatrix.wrap(new long[][] { { Long.MIN_VALUE, 0L } }).subtractExact(one));
            // the inputs are never modified, even when an overflow is reported
            assertEquals(Long.MAX_VALUE, max.get(0, 0));
        }

        @Test
        public void testLongExactArithmeticReportsEveryOverflowSite() {
            LongMatrix one = LongMatrix.wrap(new long[][] { { 1 } });
            assertEquals(LongMatrix.wrap(new long[][] { { 3 } }), one.addExact(LongMatrix.wrap(new long[][] { { 2 } })));
            assertEquals(LongMatrix.wrap(new long[][] { { -1 } }), one.subtractExact(LongMatrix.wrap(new long[][] { { 2 } })));
            assertEquals(LongMatrix.wrap(new long[][] { { 11 } }),
                    LongMatrix.wrap(new long[][] { { 1, 2 } }).matrixMultiplyExact(LongMatrix.wrap(new long[][] { { 3 }, { 4 } })));

            assertThrows(ArithmeticException.class, () -> LongMatrix.wrap(new long[][] { { Long.MAX_VALUE } }).addExact(one));
            assertThrows(ArithmeticException.class, () -> LongMatrix.wrap(new long[][] { { Long.MIN_VALUE } }).subtractExact(one));
            assertThrows(ArithmeticException.class,
                    () -> LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, 0 } }).matrixMultiplyExact(LongMatrix.wrap(new long[][] { { 2 }, { 0 } })));
            assertThrows(ArithmeticException.class,
                    () -> LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, 1 } }).matrixMultiplyExact(LongMatrix.wrap(new long[][] { { 1 }, { 1 } })));
        }
    }

    @Nested
    class SubtractTests {

        @Test
        public void testSubtract() {
            long[][] a = { { 5L, 6L }, { 7L, 8L } };
            long[][] b = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrixA = LongMatrix.wrap(a);
            LongMatrix matrixB = LongMatrix.wrap(b);

            LongMatrix diff = matrixA.subtract(matrixB);
            Assertions.assertEquals(4L, diff.get(0, 0));
            Assertions.assertEquals(4L, diff.get(0, 1));
            Assertions.assertEquals(4L, diff.get(1, 0));
            Assertions.assertEquals(4L, diff.get(1, 1));

            LongMatrix differentShape = LongMatrix.wrap(new long[][] { { 1L } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.subtract(differentShape));
        }

        @Test
        public void testSubtract_SquareSubtractionChecksEveryCell() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 5, 6 }, { 7, 8 } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix diff = m1.subtract(m2);
            assertEquals(4L, diff.get(0, 0));
            assertEquals(4L, diff.get(0, 1));
            assertEquals(4L, diff.get(1, 0));
            assertEquals(4L, diff.get(1, 1));
        }

        @Test
        public void testSubtract_DifferentDimensions() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 5L, 6L }, { 7L, 8L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testSubtract_DifferentDimensions_Shape1By2_Validation() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L, 4L }, { 5L, 6L } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testSubtract_Shape2By2_EveryCellDifference() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 10L, 20L }, { 30L, 40L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix result = m1.subtract(m2);
            assertEquals(9L, result.get(0, 0));
            assertEquals(18L, result.get(0, 1));
            assertEquals(27L, result.get(1, 0));
            assertEquals(36L, result.get(1, 1));
        }

        @Test
        public void testSubtract_AllFourCellValues() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 5L, 6L }, { 7L, 8L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix result = m1.subtract(m2);
            assertEquals(4L, result.get(0, 0));
            assertEquals(4L, result.get(0, 1));
            assertEquals(4L, result.get(1, 0));
            assertEquals(4L, result.get(1, 1));
        }

        @Test
        public void testSubtract_IncompatibleDimensions() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testSubtract_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = LongMatrix.wrap(new long[][] { { 7L, 8L, 9L }, { 10L, 11L, 12L } });
                final var right = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
                assertEquals(LongMatrix.wrap(new long[][] { { 6L, 6L, 6L }, { 6L, 6L, 6L } }), left.subtract(right));
                assertEquals(LongMatrix.wrap(new long[][] { { 7L, 8L, 9L }, { 10L, 11L, 12L } }), left);
                assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }
    }

    @Nested
    class MatrixMultiplyTests {

        @Test
        public void testMultiply() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            long[][] b = { { 5L, 6L }, { 7L, 8L } };
            LongMatrix matrixA = LongMatrix.wrap(a);
            LongMatrix matrixB = LongMatrix.wrap(b);

            LongMatrix product = matrixA.matrixMultiply(matrixB);
            Assertions.assertEquals(2, product.rowCount());
            Assertions.assertEquals(2, product.columnCount());
            Assertions.assertEquals(19L, product.get(0, 0));
            Assertions.assertEquals(22L, product.get(0, 1));
            Assertions.assertEquals(43L, product.get(1, 0));
            Assertions.assertEquals(50L, product.get(1, 1));

            LongMatrix incompatible = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.matrixMultiply(incompatible));
        }

        @Test
        public void testMatrixMultiply_SquareProductChecksEveryCell() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5, 6 }, { 7, 8 } });
            LongMatrix product = m1.matrixMultiply(m2);
            assertEquals(19L, product.get(0, 0));
            assertEquals(22L, product.get(0, 1));
            assertEquals(43L, product.get(1, 0));
            assertEquals(50L, product.get(1, 1));
        }

        @Test
        public void testMatrixMultiply_Shape2By2() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5L, 6L }, { 7L, 8L } });
            LongMatrix product = m1.matrixMultiply(m2);

            assertEquals(19L, product.get(0, 0)); // 1*5 + 2*7
            assertEquals(22L, product.get(0, 1)); // 1*6 + 2*8
            assertEquals(43L, product.get(1, 0)); // 3*5 + 4*7
            assertEquals(50L, product.get(1, 1)); // 3*6 + 4*8
        }

        @Test
        public void testMultiply_IncompatibleDimensions() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testMatrixMultiply_RectangularMatrices() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L } }); // 1x3
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 4L }, { 5L }, { 6L } }); // 3x1
            LongMatrix product = m1.matrixMultiply(m2);

            assertEquals(1, product.rowCount());
            assertEquals(1, product.columnCount());
            assertEquals(32L, product.get(0, 0)); // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        }

        @Test
        public void testMatrixMultiply_WithLargeValues() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1000000L, 2000000L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3000000L }, { 4000000L } });
            LongMatrix product = m1.matrixMultiply(m2);
            assertEquals(11000000000000L, product.get(0, 0)); // 1000000*3000000 + 2000000*4000000
        }

        @Test
        public void testMatrixMultiply_Shape2By2_EveryCellProduct() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 2L, 3L }, { 4L, 5L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix result = m1.matrixMultiply(m2);
            assertEquals(11L, result.get(0, 0));
            assertEquals(16L, result.get(0, 1));
            assertEquals(19L, result.get(1, 0));
            assertEquals(28L, result.get(1, 1));
        }

        @Test
        public void testMatrixMultiply_AllFourCellValues() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 10L, 20L }, { 30L, 40L } });
            LongMatrix result = m1.matrixMultiply(m2);
            assertEquals(70, result.get(0, 0));
            assertEquals(100, result.get(0, 1));
            assertEquals(150, result.get(1, 0));
            assertEquals(220, result.get(1, 1));
        }

        @Test
        public void testMatrixMultiply() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 2L, 0L }, { 1L, 2L } });
            LongMatrix result = m1.matrixMultiply(m2);
            assertEquals(4L, result.get(0, 0));
            assertEquals(4L, result.get(0, 1));
            assertEquals(10L, result.get(1, 0));
            assertEquals(8L, result.get(1, 1));
        }

        @Test
        public void testMatrixMultiply_IncompatibleDimensions() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testMatrixMultiply_LargeLongValues_DoesNotTruncateToInt() {
            // Verify matrix multiplication keeps values in long domain (no int truncation).
            long large = 1_000_000_000L;
            LongMatrix a = LongMatrix.wrap(new long[][] { { large, large }, { 0L, 0L } });
            LongMatrix b = LongMatrix.wrap(new long[][] { { 2L, 0L }, { 3L, 0L } });
            LongMatrix product = a.matrixMultiply(b);
            // (1e9 * 2) + (1e9 * 3) = 5e9, which exceeds Integer.MAX_VALUE.
            assertEquals(5_000_000_000L, product.get(0, 0));
            assertEquals(0L, product.get(0, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testMatrixMultiply_ExplicitModesComputeRectangularProduct(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
                final var right = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
                assertEquals(LongMatrix.wrap(new long[][] { { 9L, 12L, 15L }, { 19L, 26L, 33L }, { 29L, 40L, 51L } }), left.matrixMultiply(right));
                assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } }), left);
                assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testMatrixMultiply_EmptyProductReturnsCanonicalEmpty() {
            // Regression: matrixMultiply must build its result via LongMatrix.wrap(result) (not the raw
            // constructor) so an empty product yields the shared EMPTY singleton, and must call
            // checkNonNegativeShape before allocation for consistency with the other
            // result-allocating methods (resize/reshape/transpose/rotate).
            LongMatrix product = LongMatrix.empty().matrixMultiply(LongMatrix.empty());
            assertSame(LongMatrix.empty(), product);
            assertTrue(product.isEmpty());
        }
    }

    @Nested
    class MatrixMultiplyExactTests {

        @Test
        public void testMatrixMultiplyExact_IntermediateOverflowThrowsEvenWhenTermsCancel() {
            final LongMatrix column = LongMatrix.wrap(new long[][] { { 1L }, { 1L }, { 1L } });

            // The documented limitation: the running sum MAX + 1 overflows before the -1 term cancels it.
            final LongMatrix cancellingSum = LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, 1L, -1L } });
            assertThrows(ArithmeticException.class, () -> cancellingSum.matrixMultiplyExact(column));
            assertEquals(Long.MAX_VALUE, cancellingSum.matrixMultiply(column).get(0, 0));

            // The same terms in an order that never leaves the long range succeed.
            final LongMatrix reordered = LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, -1L, 1L } });
            assertEquals(Long.MAX_VALUE, reordered.matrixMultiplyExact(column).get(0, 0));

            // A single overflowing product throws even if a later product would cancel it.
            final LongMatrix bigTerms = LongMatrix.wrap(new long[][] { { 1L << 62, -(1L << 62) } });
            final LongMatrix fours = LongMatrix.wrap(new long[][] { { 4L }, { 4L } });
            assertThrows(ArithmeticException.class, () -> bigTerms.matrixMultiplyExact(fours));

            // Degenerate shapes keep the exact product's shape.
            final LongMatrix nx0 = LongMatrix.wrap(new long[][] { {}, {} });
            final LongMatrix zx3 = new LongMatrix(new long[0][], 3);
            final LongMatrix product = nx0.matrixMultiplyExact(zx3);
            assertEquals(2, product.rowCount());
            assertEquals(3, product.columnCount());
            assertEquals(nx0.matrixMultiply(zx3), product);
        }

        @Test
        public void testMatrixMultiplyExact_RejectsIncompatibleInnerDimensions() {
            final var left = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            final var right = LongMatrix.wrap(new long[][] { { 3L, 4L } });
            assertThrows(IllegalArgumentException.class, () -> left.matrixMultiplyExact(right));
        }

        @Test
        public void testMatrixMultiplyExact_MatchesWrappingProductWhenNothingOverflows() {
            // The kernel accumulates a whole result row at a time (i-k-j); every cell must still equal its own dot product.
            final java.util.Random random = new java.util.Random(20261027L);

            for (int trial = 0; trial < 300; trial++) {
                final int rows = random.nextInt(6);
                final int inner = random.nextInt(6);
                final int cols = random.nextInt(6);
                final long[][] left = new long[rows][inner];
                final long[][] right = new long[inner][cols];

                for (final long[][] array : java.util.List.of(left, right)) {
                    for (final long[] row : array) {
                        for (int j = 0; j < row.length; j++) {
                            row[j] = random.nextInt(2001) - 1000;
                        }
                    }
                }

                final LongMatrix a = new LongMatrix(left, inner);
                final LongMatrix b = new LongMatrix(right, cols);
                final LongMatrix exact = a.matrixMultiplyExact(b);

                assertEquals(a.matrixMultiply(b), exact);
                assertEquals(rows, exact.rowCount());
                assertEquals(cols, exact.columnCount());
            }
        }

        @Test
        public void testMatrixMultiplyExact_DetectsOverflowInAnyCell() {
            final LongMatrix maxInLastColumn = LongMatrix.wrap(new long[][] { { 1L, Long.MAX_VALUE }, { 1L, 1L } });

            // Only cell (2, 1) overflows: its running sum is MAX + 1 after the second term.
            final LongMatrix lastRowOnly = LongMatrix.wrap(new long[][] { { 0L, 0L }, { 0L, 0L }, { 1L, 1L } });
            assertThrows(ArithmeticException.class, () -> lastRowOnly.matrixMultiplyExact(maxInLastColumn));

            // A single overflowing product in a later column of the first row.
            final LongMatrix two = LongMatrix.wrap(new long[][] { { 2L } });
            assertThrows(ArithmeticException.class, () -> two.matrixMultiplyExact(LongMatrix.wrap(new long[][] { { 1L, Long.MAX_VALUE } })));

            // The same operands without the overflowing cell succeed.
            final LongMatrix firstRowsOnly = LongMatrix.wrap(new long[][] { { 0L, 0L }, { 0L, 0L } });
            assertEquals(LongMatrix.wrap(new long[][] { { 0L, 0L }, { 0L, 0L } }), firstRowsOnly.matrixMultiplyExact(maxInLastColumn));
        }
    }

    @Nested
    class BoxedTests {

        @Test
        public void testBoxed() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            Matrix<Long> boxed = matrix.boxed();
            Assertions.assertEquals(2, boxed.rowCount());
            Assertions.assertEquals(2, boxed.columnCount());
            Assertions.assertEquals(Long.valueOf(1L), boxed.get(0, 0));
            Assertions.assertEquals(Long.valueOf(4L), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            Matrix<Long> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Long.valueOf(1L), boxed.get(0, 0));
            assertEquals(Long.valueOf(6L), boxed.get(1, 2));
        }

        @Test
        public void testBoxed_VerticalMatrix() {
            // Test the rows > columnCount branch (3 rows, 2 columnCount)
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            Matrix<Long> boxed = m.boxed();
            assertEquals(3, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Long.valueOf(1L), boxed.get(0, 0));
            assertEquals(Long.valueOf(4L), boxed.get(1, 1));
            assertEquals(Long.valueOf(6L), boxed.get(2, 1));
        }

        // Verify the values are present (may not be exact due to precision)
        @Test
        public void testBoxed_ExtremeValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, Long.MIN_VALUE }, { 0L, -1L } });
            Matrix<Long> boxed = m.boxed();
            assertEquals(Long.valueOf(Long.MAX_VALUE), boxed.get(0, 0));
            assertEquals(Long.valueOf(Long.MIN_VALUE), boxed.get(0, 1));
            assertEquals(Long.valueOf(0L), boxed.get(1, 0));
            assertEquals(Long.valueOf(-1L), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By2_DimensionsAndBoxedCornerValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            Matrix<Long> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Long.valueOf(1L), boxed.get(0, 0));
            assertEquals(Long.valueOf(4L), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_AllFourCellValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            Matrix<Long> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(1L, boxed.get(0, 0));
            assertEquals(4L, boxed.get(1, 1));
        }
    }

    @Nested
    class ToIntMatrixTests {

        @Test
        public void testToIntMatrix() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            IntMatrix intMatrix = matrix.toIntMatrix();
            assertEquals(2, intMatrix.rowCount());
            assertEquals(2, intMatrix.columnCount());
            assertEquals(1, intMatrix.get(0, 0));
            assertEquals(4, intMatrix.get(1, 1));
        }
    }

    @Nested
    class ToFloatMatrixTests {

        @Test
        public void testToFloatMatrix() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            FloatMatrix floatMatrix = matrix.toFloatMatrix();
            Assertions.assertEquals(2, floatMatrix.rowCount());
            Assertions.assertEquals(2, floatMatrix.columnCount());
            Assertions.assertEquals(1.0f, floatMatrix.get(0, 0));
            Assertions.assertEquals(4.0f, floatMatrix.get(1, 1));
        }

        @Test
        public void testToFloatMatrix_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            FloatMatrix floatMatrix = m.toFloatMatrix();
            assertEquals(2, floatMatrix.rowCount());
            assertEquals(2, floatMatrix.columnCount());
            assertEquals(1.0f, floatMatrix.get(0, 0), 0.0001f);
            assertEquals(4.0f, floatMatrix.get(1, 1), 0.0001f);
        }

        @Test
        public void testToFloatMatrix_VerticalMatrix() {
            // Test the rows > columnCount branch (4 rows, 2 columnCount)
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L }, { 7L, 8L } });
            FloatMatrix floatMatrix = m.toFloatMatrix();
            assertEquals(4, floatMatrix.rowCount());
            assertEquals(2, floatMatrix.columnCount());
            assertEquals(1.0f, floatMatrix.get(0, 0), 0.0001f);
            assertEquals(8.0f, floatMatrix.get(3, 1), 0.0001f);
        }

        @Test
        public void testToFloatMatrix_ExtremeValues() {
            // Test conversion with extreme long values - may lose precision
            LongMatrix m = LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, Long.MIN_VALUE }, { 1000000000000L, -1000000000000L } });
            FloatMatrix floatMatrix = m.toFloatMatrix();
            // Verify conversion happened (exact values may lose precision)
            assertNotNull(floatMatrix);
            assertEquals(2, floatMatrix.rowCount());
            assertEquals(2, floatMatrix.columnCount());
        }

        @Test
        public void testToFloatMatrix_Shape2By2_FirstRowConvertedValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            FloatMatrix result = m.toFloatMatrix();
            assertEquals(1.0f, result.get(0, 0), 0.001f);
            assertEquals(4.0f, result.get(1, 1), 0.001f);
        }

        @Test
        public void testToFloatMatrix_Shape2By2_DimensionsAndConvertedValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            FloatMatrix result = m.toFloatMatrix();
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0f, result.get(0, 0), 0.0f);
            assertEquals(4.0f, result.get(1, 1), 0.0f);
        }

        @Test
        public void testToFloatMatrix_PreservesValuesWithinPrecision() {
            // Long values with absolute value <= 2^24 are exact in float.
            LongMatrix lm = LongMatrix.wrap(new long[][] { { 0L, 1L, 16777216L }, { -16777216L, 100L, -100L } });
            FloatMatrix fm = lm.toFloatMatrix();
            assertEquals(2, fm.rowCount());
            assertEquals(3, fm.columnCount());
            assertEquals(0.0f, fm.get(0, 0));
            assertEquals(1.0f, fm.get(0, 1));
            assertEquals(16777216.0f, fm.get(0, 2));
            assertEquals(-16777216.0f, fm.get(1, 0));
        }
    }

    @Nested
    class ToDoubleMatrixTests {

        @Test
        public void testToDoubleMatrix() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            DoubleMatrix doubleMatrix = matrix.toDoubleMatrix();
            Assertions.assertEquals(2, doubleMatrix.rowCount());
            Assertions.assertEquals(2, doubleMatrix.columnCount());
            Assertions.assertEquals(1.0, doubleMatrix.get(0, 0));
            Assertions.assertEquals(4.0, doubleMatrix.get(1, 1));
        }

        // Test the rows > columnCount branch via create() (3 rows, 2 columnCount)
        @Test
        public void testToDoubleMatrix_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            DoubleMatrix doubleMatrix = m.toDoubleMatrix();
            assertEquals(2, doubleMatrix.rowCount());
            assertEquals(2, doubleMatrix.columnCount());
            assertEquals(1.0, doubleMatrix.get(0, 0), 0.0001);
            assertEquals(4.0, doubleMatrix.get(1, 1), 0.0001);
        }

        @Test
        public void testToDoubleMatrix_VerticalMatrix() {
            // Test the rows > columnCount branch via create() (3 rows, 2 columnCount)
            LongMatrix m = LongMatrix.wrap(new long[][] { { 10L, 20L }, { 30L, 40L }, { 50L, 60L } });
            DoubleMatrix doubleMatrix = m.toDoubleMatrix();
            assertEquals(3, doubleMatrix.rowCount());
            assertEquals(2, doubleMatrix.columnCount());
            assertEquals(10.0, doubleMatrix.get(0, 0), 0.0001);
            assertEquals(60.0, doubleMatrix.get(2, 1), 0.0001);
        }

        @Test
        public void testToDoubleMatrix_ExtremeValues() {
            // Test with extreme long values - check precision
            LongMatrix m = LongMatrix.wrap(new long[][] { { Long.MAX_VALUE, Long.MIN_VALUE }, { 9007199254740992L, -9007199254740992L } });
            DoubleMatrix doubleMatrix = m.toDoubleMatrix();
            // Double has 53 bits of precision, so very large longs may lose precision
            assertNotNull(doubleMatrix);
            assertEquals(2, doubleMatrix.rowCount());
            assertEquals(2, doubleMatrix.columnCount());
            // Verify the values are present (may not be exact due to precision)
            assertTrue(doubleMatrix.get(0, 0) > 0);
            assertTrue(doubleMatrix.get(0, 1) < 0);
        }

        @Test
        public void testToDoubleMatrix_Shape2By2_FirstRowConvertedValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            DoubleMatrix result = m.toDoubleMatrix();
            assertEquals(1.0, result.get(0, 0), 0.001);
            assertEquals(4.0, result.get(1, 1), 0.001);
        }

        @Test
        public void testToDoubleMatrix_AllFourCellValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            DoubleMatrix result = m.toDoubleMatrix();
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0, result.get(0, 0), 0.0);
            assertEquals(4.0, result.get(1, 1), 0.0);
        }
    }

    @Nested
    class ZipWithTests {

        @Test
        public void testZipWith() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            long[][] b = { { 5L, 6L }, { 7L, 8L } };
            LongMatrix matrixA = LongMatrix.wrap(a);
            LongMatrix matrixB = LongMatrix.wrap(b);

            LongMatrix result = matrixA.zipWith(matrixB, Math::max);
            Assertions.assertEquals(5L, result.get(0, 0));
            Assertions.assertEquals(6L, result.get(0, 1));
            Assertions.assertEquals(7L, result.get(1, 0));
            Assertions.assertEquals(8L, result.get(1, 1));

            LongMatrix differentShape = LongMatrix.wrap(new long[][] { { 1L } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.zipWith(differentShape, (x, y) -> x));
        }

        @Test
        public void testZipWith3() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            long[][] b = { { 5L, 6L }, { 7L, 8L } };
            long[][] c = { { 9L, 10L }, { 11L, 12L } };
            LongMatrix matrixA = LongMatrix.wrap(a);
            LongMatrix matrixB = LongMatrix.wrap(b);
            LongMatrix matrixC = LongMatrix.wrap(c);

            LongMatrix result = matrixA.zipWith(matrixB, matrixC, (x, y, z) -> (x + y + z) / 3);
            Assertions.assertEquals(5L, result.get(0, 0));
            Assertions.assertEquals(6L, result.get(0, 1));
            Assertions.assertEquals(7L, result.get(1, 0));
            Assertions.assertEquals(8L, result.get(1, 1));
        }

        @Test
        public void testZipWith_AverageOfThreeCorrespondingCells() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5, 6 }, { 7, 8 } });
            LongMatrix m3 = LongMatrix.wrap(new long[][] { { 9, 10 }, { 11, 12 } });
            LongMatrix average = m1.zipWith(m2, m3, (a, b, c) -> (a + b + c) / 3);
            assertEquals(5L, average.get(0, 0));
            assertEquals(6L, average.get(0, 1));
            assertEquals(7L, average.get(1, 0));
            assertEquals(8L, average.get(1, 1));
        }

        @Test
        public void testZipWith_Shape2By2() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5L, 6L }, { 7L, 8L } });
            LongMatrix result = m1.zipWith(m2, (a, b) -> a * b);

            assertEquals(5L, result.get(0, 0)); // 1*5
            assertEquals(12L, result.get(0, 1)); // 2*6
            assertEquals(21L, result.get(1, 0)); // 3*7
            assertEquals(32L, result.get(1, 1)); // 4*8
        }

        @Test
        public void testZipWith_DifferentShapes() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a + b));
        }

        @Test
        public void testZipWith_ThreeMatrices_DifferentShapes() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5L, 6L }, { 7L, 8L } });
            LongMatrix m3 = LongMatrix.wrap(new long[][] { { 9L, 10L, 11L } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> a + b + c));
        }

        @Test
        public void testElementWiseMultiplyWithZipWith() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 2L, 3L }, { 4L, 5L } });

            // Element-wise multiplication
            LongMatrix elementWiseProduct = m1.zipWith(m2, (a, b) -> a * b);
            assertEquals(2L, elementWiseProduct.get(0, 0)); // 1*2
            assertEquals(6L, elementWiseProduct.get(0, 1)); // 2*3
            assertEquals(12L, elementWiseProduct.get(1, 0)); // 3*4
            assertEquals(20L, elementWiseProduct.get(1, 1)); // 4*5

            // Element-wise division
            LongMatrix elementWiseDivision = m2.zipWith(m1, (a, b) -> a / b);
            assertEquals(2L, elementWiseDivision.get(0, 0)); // 2/1
            assertEquals(1L, elementWiseDivision.get(0, 1)); // 3/2 (integer division)
            assertEquals(1L, elementWiseDivision.get(1, 0)); // 4/3 (integer division)
            assertEquals(1L, elementWiseDivision.get(1, 1)); // 5/4 (integer division)
        }

        @Test
        public void testZipWith_TwoMatrices() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5L, 6L }, { 7L, 8L } });
            LongMatrix result = m1.zipWith(m2, (a, b) -> a + b);
            assertEquals(6L, result.get(0, 0));
            assertEquals(8L, result.get(0, 1));
            assertEquals(10L, result.get(1, 0));
            assertEquals(12L, result.get(1, 1));
        }

        @Test
        public void testZipWith_TwoMatrices_DifferentDimensions() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L, 4L }, { 5L, 6L } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a + b));
        }

        @Test
        public void testZipWith_ThreeMatrices() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5L, 6L }, { 7L, 8L } });
            LongMatrix m3 = LongMatrix.wrap(new long[][] { { 9L, 10L }, { 11L, 12L } });
            LongMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(15L, result.get(0, 0)); // 1 + 5 + 9
            assertEquals(18L, result.get(0, 1)); // 2 + 6 + 10
            assertEquals(21L, result.get(1, 0)); // 3 + 7 + 11
            assertEquals(24L, result.get(1, 1)); // 4 + 8 + 12
        }

        @Test
        public void testZipWith_ThreeMatrices_DifferentDimensions() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L, 4L } });
            LongMatrix m3 = LongMatrix.wrap(new long[][] { { 5L, 6L }, { 7L, 8L } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> a + b + c));
        }

        @Test
        public void testZipWith_TwoMatrices_Shape2By2() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 10L, 20L }, { 30L, 40L } });
            LongMatrix result = m1.zipWith(m2, (a, b) -> a + b);
            assertEquals(11L, result.get(0, 0));
            assertEquals(22L, result.get(0, 1));
            assertEquals(33L, result.get(1, 0));
            assertEquals(44L, result.get(1, 1));
        }

        @Test
        public void testZipWith_ThreeMatrices_Shape1By2() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 10L, 20L } });
            LongMatrix m3 = LongMatrix.wrap(new long[][] { { 100L, 200L } });
            LongMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(111L, result.get(0, 0));
            assertEquals(222L, result.get(0, 1));
        }

        @Test
        public void testZipWith_Ternary() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 3L, 4L } });
            LongMatrix m3 = LongMatrix.wrap(new long[][] { { 5L, 6L } });
            LongMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(9L, result.get(0, 0));
            assertEquals(12L, result.get(0, 1));
        }

        // LongMatrix.java: matrix.forEach(value -> System.out.print(value + " "));
        // Output: 1 2 3 4
        @Test
        public void testZipWith_MaximumOfCorrespondingCells() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5, 6 }, { 7, 8 } });
            LongMatrix max = m1.zipWith(m2, Math::max);
            assertEquals(5L, max.get(0, 0));
            assertEquals(6L, max.get(0, 1));
            assertEquals(7L, max.get(1, 0));
            assertEquals(8L, max.get(1, 1));
        }
    }

    @Nested
    class MainDiagonalStreamTests {

        @Test
        public void testStreamLU2RD() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] diagonal = matrix.mainDiagonalStream().toArray();
            Assertions.assertArrayEquals(new long[] { 1L, 5L, 9L }, diagonal);

            LongMatrix empty = LongMatrix.empty();
            Assertions.assertTrue(empty.mainDiagonalStream().toList().isEmpty());

            LongMatrix nonSquare = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            Assertions.assertArrayEquals(new long[] { 1L }, nonSquare.mainDiagonalStream().toArray());
        }

        @Test
        public void testMainDiagonalStream_Shape3By3_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            long[] diagonal = m.mainDiagonalStream().toArray();
            assertArrayEquals(new long[] { 1L, 5L, 9L }, diagonal);
        }

        @Test
        public void testMainDiagonalStream_NonSquare_Shape1By2_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertArrayEquals(new long[] { 1L }, m.mainDiagonalStream().toArray());
        }

        @Test
        public void testMainDiagonalStream() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            long sum = m.mainDiagonalStream().sum();
            assertEquals(5L, sum);
        }

        @Test
        public void testMainDiagonalStream_IteratorAdvanceAndCount() {
            // Exercise advance() boundary around the `a[cursor][cursor++]` access pattern.
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 0L, 0L, 0L }, { 0L, 2L, 0L, 0L }, { 0L, 0L, 3L, 0L }, { 0L, 0L, 0L, 4L } });

            var it = (com.landawn.abacus.util.stream.LongIteratorEx) m.mainDiagonalStream().iterator();
            assertEquals(1L, it.nextLong());
            it.advance(2);
            assertEquals(4L, it.nextLong());
            assertFalse(it.hasNext());

            // count() consumes what remains, so it is asserted on a fresh iterator.
            var counting = (com.landawn.abacus.util.stream.LongIteratorEx) m.mainDiagonalStream().iterator();
            assertEquals(4L, counting.count());
            assertFalse(counting.hasNext());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testMainDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            final long[] expected = new long[] { 1L, 5L, 9L };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (LongIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextLong);
            }
        }

        @Test
        public void testMainDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (LongIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1L, iterator.nextLong());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextLong);
            }
        }

        @Test
        public void testMainDiagonalStream_Empty() {
            LongMatrix empty = LongMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().toArray().length);
        }

        @Test
        public void testDiagonalStream_Nx0MatrixThrowsBecauseNotSquare() {
            LongMatrix m = LongMatrix.wrap(new long[3][0]);
            assertEquals(3, m.rowCount());
            assertEquals(0, m.columnCount());
            assertEquals(0, m.mainDiagonalStream().toArray().length);
            assertEquals(0, m.antiDiagonalStream().toArray().length);

            LongMatrix empty = LongMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().count());
            assertEquals(0, empty.antiDiagonalStream().count());
        }
    }

    @Nested
    class AntiDiagonalStreamTests {

        @Test
        public void testStreamRU2LD() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] diagonal = matrix.antiDiagonalStream().toArray();
            Assertions.assertArrayEquals(new long[] { 3L, 5L, 7L }, diagonal);
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_ArrayValues_AntiDiagonalArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            long[] diagonal = m.antiDiagonalStream().toArray();
            assertArrayEquals(new long[] { 3L, 5L, 7L }, diagonal);
        }

        @Test
        public void testAntiDiagonalStream_NonSquare_Shape1By2_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertArrayEquals(new long[] { 2L }, m.antiDiagonalStream().toArray());
        }

        @Test
        public void testAntiDiagonalStream() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            long sum = m.antiDiagonalStream().sum();
            assertEquals(5L, sum);
        }

        @Test
        public void testAntiDiagonalStream_IteratorAdvanceAndCount() {
            // Anti-diagonal of 4x4: (0,3)=4, (1,2)=3, (2,1)=2, (3,0)=1.
            LongMatrix m = LongMatrix.wrap(new long[][] { { 0L, 0L, 0L, 4L }, { 0L, 0L, 3L, 0L }, { 0L, 2L, 0L, 0L }, { 1L, 0L, 0L, 0L } });

            long[] anti = m.antiDiagonalStream().toArray();
            assertArrayEquals(new long[] { 4L, 3L, 2L, 1L }, anti);

            var it = (com.landawn.abacus.util.stream.LongIteratorEx) m.antiDiagonalStream().iterator();
            it.advance(1);
            assertEquals(3L, it.nextLong());

            // count() consumes the remaining 3, 2, 1 and exhausts the iterator.
            var counting = (com.landawn.abacus.util.stream.LongIteratorEx) m.antiDiagonalStream().iterator();
            counting.advance(1);
            assertEquals(3L, counting.count());
            assertFalse(counting.hasNext());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testAntiDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            final long[] expected = new long[] { 3L, 5L, 7L };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (LongIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextLong);
            }
        }

        @Test
        public void testAntiDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (LongIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(3L, iterator.nextLong());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextLong);
            }
        }

        @Test
        public void testAntiDiagonalStream_Empty() {
            LongMatrix empty = LongMatrix.empty();
            assertEquals(0, empty.antiDiagonalStream().toArray().length);
        }
    }

    @Nested
    class RowMajorStreamTests {

        @Test
        public void testRowMajorStream() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] all = matrix.rowMajorStream().toArray();
            Assertions.assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, all);

            LongMatrix empty = LongMatrix.empty();
            Assertions.assertTrue(empty.rowMajorStream().toList().isEmpty());
        }

        @Test
        public void testRowMajorStreamRange() {
            long[][] a = { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] range = matrix.rowMajorStream(1, 3).toArray();
            Assertions.assertArrayEquals(new long[] { 3L, 4L, 5L, 6L }, range);

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 2));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(0, 4));
        }

        @Test
        public void testHorizontalStreamSum() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            long sum = matrix.rowMajorStream().sum();
            assertEquals(21L, sum);
        }

        @Test
        public void testHorizontalStreamRange() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            long[] subset = matrix.rowMajorStream(0, 2).toArray();
            assertArrayEquals(new long[] { 1, 2, 3, 4 }, subset);
        }

        @Test
        public void testRowMajorStream_Shape2By3_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            long[] all = m.rowMajorStream().toArray();
            assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L, 6L }, all);
        }

        @Test
        public void testStatisticalOperations() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });

            // Test sum
            long totalSum = m.rowMajorStream().sum();
            assertEquals(45L, totalSum); // 1+2+3+4+5+6+7+8+9 = 45

            // Test sum of specific row
            long row1Sum = m.rowMajorStream(1, 2).sum();
            assertEquals(15L, row1Sum); // 4+5+6 = 15

            // Test sum of specific column
            long col0Sum = m.columnMajorStream(0, 1).sum();
            assertEquals(12L, col0Sum); // 1+4+7 = 12

            // Test min/max
            long min = m.rowMajorStream().min().orElse(0L);
            assertEquals(1L, min);

            long max = m.rowMajorStream().max().orElse(0L);
            assertEquals(9L, max);

            // Test average
            double avg = m.rowMajorStream().average().orElse(0.0);
            assertEquals(5.0, avg, 0.0001);

            // Test diagonal operations
            long diagonalSum = m.mainDiagonalStream().sum();
            assertEquals(15L, diagonalSum); // 1+5+9 = 15

            long antiDiagonalSum = m.antiDiagonalStream().sum();
            assertEquals(15L, antiDiagonalSum); // 3+5+7 = 15
        }

        @Test
        public void testRowMajorStream_RowRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            long[] rows = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new long[] { 3L, 4L, 5L, 6L }, rows);
        }

        @Test
        public void testHorizontalStream() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            long sum = m.rowMajorStream().sum();
            assertEquals(10L, sum);
        }

        @Test
        public void testRowMajorStream_WithRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            long sum = m.rowMajorStream(1, 3).sum();
            assertEquals(18L, sum);
        }

        @Test
        public void testRowMajorStreamorizontalIteratorAdvanceAndExhaustion_EdgeCase() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            var iterator = matrix.rowMajorStream(0, 2).iterator();

            assertTrue(iterator instanceof com.landawn.abacus.util.stream.LongIteratorEx);

            com.landawn.abacus.util.stream.LongIteratorEx ex = (com.landawn.abacus.util.stream.LongIteratorEx) iterator;
            ex.advance(2);
            assertEquals(3L, ex.nextLong());
            ex.advance(10);
            assertFalse(ex.hasNext());
            assertThrows(java.util.NoSuchElementException.class, ex::nextLong);

            // IteratorEx.count() is terminal: it consumes the remaining elements and leaves the iterator
            // exhausted, so a remaining-size assertion needs its own iterator.
            var counting = (com.landawn.abacus.util.stream.LongIteratorEx) matrix.rowMajorStream(0, 2).iterator();
            counting.advance(2);
            assertEquals(2L, counting.count());
            assertFalse(counting.hasNext());
        }

        @Test
        public void testStreamIterators_MidStrideAdvanceThenToArray_EdgeCase() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });

            var rowMajorIterator = matrix.rowMajorStream(0, 2).iterator();
            assertTrue(rowMajorIterator instanceof com.landawn.abacus.util.stream.LongIteratorEx);
            com.landawn.abacus.util.stream.LongIteratorEx rowMajor = (com.landawn.abacus.util.stream.LongIteratorEx) rowMajorIterator;
            rowMajor.advance(1); // mid-row cursor: the chunked toArray must start at column 1
            assertArrayEquals(new long[] { 2L, 3L, 4L, 5L, 6L }, rowMajor.toArray());

            // IteratorEx.count() is terminal: it consumes the remainder and exhausts the iterator, so every
            // remaining-size assertion below uses its own freshly positioned iterator.
            var rowMajorCount = (com.landawn.abacus.util.stream.LongIteratorEx) matrix.rowMajorStream(0, 2).iterator();
            rowMajorCount.advance(1);
            assertEquals(5L, rowMajorCount.count());

            var columnMajorIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.LongIteratorEx columnMajor = (com.landawn.abacus.util.stream.LongIteratorEx) columnMajorIterator;
            columnMajor.advance(1); // mid-column cursor: row 1 of column 0
            assertArrayEquals(new long[] { 4L, 2L, 5L, 3L, 6L }, columnMajor.toArray());

            var columnMajorCount = (com.landawn.abacus.util.stream.LongIteratorEx) matrix.columnMajorStream(0, 3).iterator();
            columnMajorCount.advance(1);
            assertEquals(5L, columnMajorCount.count());

            var crossingIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.LongIteratorEx crossing = (com.landawn.abacus.util.stream.LongIteratorEx) crossingIterator;
            crossing.advance(3); // crosses a column boundary: lands on row 1 of column 1
            assertEquals(5L, crossing.nextLong());
            crossing.advance(10);
            assertEquals(0L, crossing.count());
            assertThrows(java.util.NoSuchElementException.class, crossing::nextLong);

            var crossingCount = (com.landawn.abacus.util.stream.LongIteratorEx) matrix.columnMajorStream(0, 3).iterator();
            crossingCount.advance(3);
            assertEquals(3L, crossingCount.count()); // consumes 5L, 3L, 6L
            assertFalse(crossingCount.hasNext());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testRowMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            final long[] expected = new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (LongIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextLong);
            }
        }

        @Test
        public void testRowMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (LongIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1L, iterator.nextLong());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextLong);
            }
        }

        @Test
        public void testRowMajorStreamSingleRowRange() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] row1 = matrix.rowMajorStream(1, 2).toArray();
            Assertions.assertArrayEquals(new long[] { 4L, 5L, 6L }, row1);
        }

        @Test
        public void testHorizontalStreamSingleRowRange() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            long rowSum = matrix.rowMajorStream(1, 2).sum();
            assertEquals(15L, rowSum);
        }

        @Test
        public void testRowMajorStream_Empty() {
            LongMatrix empty = LongMatrix.empty();
            assertEquals(0, empty.rowMajorStream().toArray().length);
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            long[] row1 = m.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new long[] { 4L, 5L, 6L }, row1);
        }

        // Note: sumByLong(), sumByDouble() methods don't exist in LongMatrix
        @Test
        public void testRowMajorStream_WithSingleRowRangeRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            long[] rows = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new long[] { 4L, 5L, 6L, 7L, 8L, 9L }, rows);
        }

        @Test
        public void testRowMajorStream_BySingleRowRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            long sum = m.rowMajorStream(0, 1).sum();
            assertEquals(3L, sum);
        }

        @Test
        public void testRowMajorStream_ZeroColumnsKeepLogicalRowRange() {
            final var matrix = LongMatrix.wrap(new long[3][0]);
            assertEquals(0, matrix.rowMajorStream(1, 3).count());
            assertEquals(2, matrix.rowStreams(1, 3).count());
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 3));
        }

        @Test
        public void testRowMajorStream_WithRange_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 1));
        }

        @Test
        public void testRowMajorStream_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 2));
        }
    }

    @Nested
    class ColumnMajorStreamTests {

        @Test
        public void testColumnMajorStream() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] all = matrix.columnMajorStream().toArray();
            Assertions.assertArrayEquals(new long[] { 1L, 4L, 2L, 5L, 3L, 6L }, all);
        }

        @Test
        public void testColumnMajorStreamRange() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] range = matrix.columnMajorStream(1, 3).toArray();
            Assertions.assertArrayEquals(new long[] { 2L, 5L, 3L, 6L }, range);
        }

        @Test
        public void testColumnMajorStream_Shape2By3_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            long[] all = m.columnMajorStream().toArray();
            assertArrayEquals(new long[] { 1L, 4L, 2L, 5L, 3L, 6L }, all);
        }

        @Test
        public void testColumnMajorStream_WithRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            long[] columnCount = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new long[] { 2L, 5L, 8L, 3L, 6L, 9L }, columnCount);
        }

        @Test
        public void testColumnMajorStream_ColumnRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            long[] columnCount = m.columnMajorStream(0, 2).toArray();
            assertArrayEquals(new long[] { 1L, 4L, 2L, 5L }, columnCount);
        }

        @Test
        public void testVerticalStream() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            long sum = m.columnMajorStream().sum();
            assertEquals(10L, sum);
        }

        @Test
        public void testColumnMajorStream_WithRange_Shape2By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            long sum = m.columnMajorStream(1, 3).sum();
            assertEquals(16L, sum);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testColumnMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            final long[] expected = new long[] { 1L, 4L, 7L, 2L, 5L, 8L, 3L, 6L, 9L };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (LongIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextLong);
            }
        }

        @Test
        public void testColumnMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (LongIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1L, iterator.nextLong());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextLong);
            }
        }

        @Test
        public void testColumnMajorStreamSingleColumnRange() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            long[] col1 = matrix.columnMajorStream(1, 2).toArray();
            Assertions.assertArrayEquals(new long[] { 2L, 5L }, col1);
        }

        @Test
        public void testColumnMajorStream_Empty() {
            LongMatrix empty = LongMatrix.empty();
            assertEquals(0, empty.columnMajorStream().toArray().length);
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            long[] col1 = m.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new long[] { 2L, 5L }, col1);
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRangeRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            long[] columnCount = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new long[] { 2L, 5L, 3L, 6L }, columnCount);
        }

        @Test
        public void testColumnMajorStream_BySingleColumnRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            long sum = m.columnMajorStream(0, 1).sum();
            assertEquals(4L, sum);
        }

        // An N x 0 matrix transposes to 0 x N: no elements, but the column count survives.
        @Test
        public void testVerticalStream_AdvanceCrossesColumnBoundary() {
            // Verify advance crosses column boundaries for columnMajorStream.
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 4L, 7L }, { 2L, 5L, 8L }, { 3L, 6L, 9L } });

            var it = (com.landawn.abacus.util.stream.LongIteratorEx) m.columnMajorStream().iterator();
            // columnMajorStream() → 1,2,3 (col 0) | 4,5,6 (col 1) | 7,8,9 (col 2)
            it.advance(4);
            // After advancing 4 elements we have skipped 1,2,3,4; next should be 5.
            assertEquals(5L, it.nextLong());
            it.advance(2);
            // After advancing 2 more (6, 7) the next element is 8.
            assertEquals(8L, it.nextLong());

            // count() is terminal, so each remaining-size assertion uses a fresh iterator.
            assertEquals(9L, ((com.landawn.abacus.util.stream.LongIteratorEx) m.columnMajorStream().iterator()).count());
            var afterFour = (com.landawn.abacus.util.stream.LongIteratorEx) m.columnMajorStream().iterator();
            afterFour.advance(4);
            assertEquals(5L, afterFour.count());
            assertFalse(afterFour.hasNext());
        }

        @Test
        public void testColumnMajorStream_ZeroRowsKeepLogicalColumnRange() {
            final var matrix = LongMatrix.wrap(new long[0][0]).resize(0, 3);
            assertEquals(0, matrix.columnMajorStream(1, 3).count());
            assertEquals(2, matrix.columnStreams(1, 3).count());
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 3));
        }

        @Test
        public void testColumnMajorStream_WithRange_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 1));
        }

        @Test
        public void testColumnMajorStream_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
        }
    }

    @Nested
    class RowStreamsTests {

        @Test
        public void testStreamR() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            List<LongStream> rows = matrix.rowStreams().toList();
            Assertions.assertEquals(2, rows.size());
            Assertions.assertArrayEquals(new long[] { 1L, 2L }, rows.get(0).toArray());
            Assertions.assertArrayEquals(new long[] { 3L, 4L }, rows.get(1).toArray());
        }

        @Test
        public void testRowStreamsRange() {
            long[][] a = { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            List<LongStream> rows = matrix.rowStreams(1, 3).toList();
            Assertions.assertEquals(2, rows.size());
            Assertions.assertArrayEquals(new long[] { 3L, 4L }, rows.get(0).toArray());
            Assertions.assertArrayEquals(new long[] { 5L, 6L }, rows.get(1).toArray());
        }

        @Test
        public void testRowStreams_Shape2By3_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            List<long[]> rows = m.rowStreams().map(LongStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new long[] { 1L, 2L, 3L }, rows.get(0));
            assertArrayEquals(new long[] { 4L, 5L, 6L }, rows.get(1));
        }

        @Test
        public void testRowStreams_WithRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            List<long[]> rows = m.rowStreams(1, 3).map(LongStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new long[] { 4L, 5L, 6L }, rows.get(0));
            assertArrayEquals(new long[] { 7L, 8L, 9L }, rows.get(1));
        }

        @Test
        public void testRowStreams_Shape2By2_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            List<long[]> rows = m.rowStreams().map(LongStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new long[] { 1L, 2L }, rows.get(0));
            assertArrayEquals(new long[] { 3L, 4L }, rows.get(1));
        }

        @Test
        public void testRowStreams() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            long count = m.rowStreams().count();
            assertEquals(2, count);
        }

        // Tests for AbstractMatrix-inherited methods missing coverage
        @Test
        public void testRowStreams_WithRange_Shape3By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L }, { 5L, 6L } });
            long count = m.rowStreams(1, 3).count();
            assertEquals(2, count);
        }

        @Test
        public void testStreamsObserveLiveValuesAndRowStreamsKeepTheirSelectedBackingRow() {
            final LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            try (var rowStreams = m.rowStreams(0, 2);
                 var columnStreams = m.columnStreams(0, 2);
                 var rowMajor = m.rowMajorStream(0, 2);
                 var columnMajor = m.columnMajorStream(0, 2);
                 var mainDiagonal = m.mainDiagonalStream();
                 var antiDiagonal = m.antiDiagonalStream()) {
                final var rows = rowStreams.iterator();
                final var columns = columnStreams.iterator();
                try (var selectedRow = rows.next();
                     var selectedColumn = columns.next()) {
                    m.flipVerticallyInPlace();
                    m.set(1, 0, 9);
                    assertArrayEquals(new long[] { 9, 2 }, selectedRow.toArray());
                    assertArrayEquals(new long[] { 3, 9 }, selectedColumn.toArray());
                    assertArrayEquals(new long[] { 3, 4, 9, 2 }, rowMajor.toArray());
                    assertArrayEquals(new long[] { 3, 9, 4, 2 }, columnMajor.toArray());
                    assertArrayEquals(new long[] { 3, 2 }, mainDiagonal.toArray());
                    assertArrayEquals(new long[] { 4, 9 }, antiDiagonal.toArray());
                }
                try (var nextRow = rows.next();
                     var nextColumn = columns.next()) {
                    assertArrayEquals(new long[] { 9, 2 }, nextRow.toArray());
                    assertArrayEquals(new long[] { 4, 2 }, nextColumn.toArray());
                }
            }
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testRowStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            final long[][] expected = { new long[] { 1L, 2L, 3L }, new long[] { 4L, 5L, 6L }, new long[] { 7L, 8L, 9L } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowStreams()) {
                final var iterator = (ObjIteratorEx<LongStream>) stream.iterator();
                iterator.advance(amount);
                if (skipped < expected.length) {
                    assertTrue(iterator.hasNext());
                    try (var values = iterator.next()) {
                        assertArrayEquals(expected[skipped], values.toArray());
                    }
                }
                assertEquals(Math.max(0, expected.length - skipped - 1), iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }

        @Test
        public void testRowStreams_Empty() {
            LongMatrix empty = LongMatrix.empty();
            assertEquals(0, empty.rowStreams().count());
        }

        @Test
        public void testRowStreams_WithRange_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(0, 3));
        }
    }

    @Nested
    class ColumnStreamsTests {

        // length is protected, cannot test directly from here
        @Test
        public void testStreamC() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            List<LongStream> columnCount = matrix.columnStreams().toList();
            Assertions.assertEquals(3, columnCount.size());
            Assertions.assertArrayEquals(new long[] { 1L, 4L }, columnCount.get(0).toArray());
            Assertions.assertArrayEquals(new long[] { 2L, 5L }, columnCount.get(1).toArray());
            Assertions.assertArrayEquals(new long[] { 3L, 6L }, columnCount.get(2).toArray());
        }

        @Test
        public void testColumnStreamsRange() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            List<LongStream> columnCount = matrix.columnStreams(1, 3).toList();
            Assertions.assertEquals(2, columnCount.size());
            Assertions.assertArrayEquals(new long[] { 2L, 5L }, columnCount.get(0).toArray());
            Assertions.assertArrayEquals(new long[] { 3L, 6L }, columnCount.get(1).toArray());
        }

        @Test
        public void testColumnStreams_Shape2By3_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            List<long[]> columnCount = m.columnStreams().map(LongStream::toArray).toList();
            assertEquals(3, columnCount.size());
            assertArrayEquals(new long[] { 1L, 4L }, columnCount.get(0));
            assertArrayEquals(new long[] { 2L, 5L }, columnCount.get(1));
            assertArrayEquals(new long[] { 3L, 6L }, columnCount.get(2));
        }

        @Test
        public void testColumnStreams_WithRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            List<long[]> columnCount = m.columnStreams(1, 3).map(LongStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new long[] { 2L, 5L, 8L }, columnCount.get(0));
            assertArrayEquals(new long[] { 3L, 6L, 9L }, columnCount.get(1));
        }

        @Test
        public void testColumnStreams_Shape2By2_ArrayValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            List<long[]> columnCount = m.columnStreams().map(LongStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new long[] { 1L, 3L }, columnCount.get(0));
            assertArrayEquals(new long[] { 2L, 4L }, columnCount.get(1));
        }

        @Test
        public void testColumnStreams() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            long count = m.columnStreams().count();
            assertEquals(2, count);
        }

        @Test
        public void testColumnStreams_WithRange_Shape1By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L } });
            long count = m.columnStreams(1, 3).count();
            assertEquals(2, count);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testColumnStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            final long[][] expected = { new long[] { 1L, 4L, 7L }, new long[] { 2L, 5L, 8L }, new long[] { 3L, 6L, 9L } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnStreams()) {
                final var iterator = (ObjIteratorEx<LongStream>) stream.iterator();
                iterator.advance(amount);
                if (skipped < expected.length) {
                    assertTrue(iterator.hasNext());
                    try (var values = iterator.next()) {
                        assertArrayEquals(expected[skipped], values.toArray());
                    }
                }
                assertEquals(Math.max(0, expected.length - skipped - 1), iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testColumnStreams_InnerCursorAdvanceClampsToRemainingValues(final long amount) {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            final long[] expected = new long[] { 2L, 5L, 8L };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (LongIteratorEx) values.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextLong);
            }
        }

        @Test
        public void testColumnStreams_InnerCountConsumesOnlyRemainingValues() {
            final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (LongIteratorEx) values.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(2L, iterator.nextLong());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextLong);
            }
        }

        @Test
        public void testColumnStreams_Empty() {
            LongMatrix empty = LongMatrix.empty();
            assertEquals(0, empty.columnStreams().count());
        }

        @Test
        public void testColumnStreams_WithRange_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(0, 3));
        }
    }

    @Nested
    class LengthTests {

        @Test
        public void testLength() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            // length is protected, cannot test directly from here
            // It's tested indirectly through other operations
            Assertions.assertEquals(2, matrix.columnCount());
        }
    }

    @Nested
    class ForEachTests {

        @Test
        public void testForEach() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            List<Long> collected = new ArrayList<>();
            matrix.forEach(val -> collected.add(val));

            Assertions.assertEquals(4, collected.size());
            Assertions.assertTrue(collected.contains(1L));
            Assertions.assertTrue(collected.contains(2L));
            Assertions.assertTrue(collected.contains(3L));
            Assertions.assertTrue(collected.contains(4L));
        }

        @Test
        public void testForEachWithRange() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            List<Long> collected = new ArrayList<>();
            matrix.forEach(1, 3, 1, 3, val -> collected.add(val));

            Assertions.assertEquals(4, collected.size());
            Assertions.assertTrue(collected.contains(5L));
            Assertions.assertTrue(collected.contains(6L));
            Assertions.assertTrue(collected.contains(8L));
            Assertions.assertTrue(collected.contains(9L));

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.forEach(0, 4, 0, 3, val -> {
            }));
        }

        @Test
        public void testLongMatrixForEach() {
            // LongMatrix.java: matrix.forEach(value -> System.out.print(value + " "));
            // Output: 1 2 3 4
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            List<Long> values = new ArrayList<>();
            matrix.forEach(value -> values.add(value));
            assertEquals(4, values.size());
            assertEquals(1L, values.get(0));
            assertEquals(2L, values.get(1));
            assertEquals(3L, values.get(2));
            assertEquals(4L, values.get(3));
        }

        @Test
        public void testLongMatrixForEachWithRange() {
            // LongMatrix.java: matrix.forEach(1, 3, 1, 3, value -> System.out.print(value + " "));
            // Output: 5 6 8 9 (elements from rows 1-2, columns 1-2)
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Long> values = new ArrayList<>();
            matrix.forEach(1, 3, 1, 3, value -> values.add(value));
            assertEquals(4, values.size());
            assertEquals(5L, values.get(0));
            assertEquals(6L, values.get(1));
            assertEquals(8L, values.get(2));
            assertEquals(9L, values.get(3));
        }

        @Test
        public void testForEach_Shape3By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            List<Long> values = new ArrayList<>();
            m.forEach(v -> values.add(v));
            assertEquals(9, values.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1L, values.get(i).longValue());
            }
        }

        @Test
        public void testForEach_WithBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            List<Long> values = new ArrayList<>();
            m.forEach(1, 3, 1, 3, v -> values.add(v));
            assertEquals(4, values.size());
            assertEquals(5L, values.get(0).longValue());
            assertEquals(6L, values.get(1).longValue());
            assertEquals(8L, values.get(2).longValue());
            assertEquals(9L, values.get(3).longValue());
        }

        @Test
        public void testForEach_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEach(x -> count.incrementAndGet());
            assertEquals(4, count.get());
        }

        @Test
        public void testForEach_WithRange() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEach(1, 3, 1, 3, x -> count.incrementAndGet());
            assertEquals(4, count.get());
        }

        @Test
        public void testForEachStopsAtTheFailingElementInSequentialMode() {
            final LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            final IllegalStateException failure = new IllegalStateException("stop at the second element");
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                for (final boolean selectedRegion : new boolean[] { false, true }) {
                    final List<Long> visited = new java.util.ArrayList<>();
                    final Throwables.LongConsumer<RuntimeException> action = value -> {
                        visited.add(value);
                        if (visited.size() == 2) {
                            throw failure;
                        }
                    };
                    assertSame(failure, assertThrows(IllegalStateException.class, () -> {
                        if (selectedRegion) {
                            matrix.forEach(0, 2, 1, 3, action);
                        } else {
                            matrix.forEach(action);
                        }
                    }));
                    assertEquals(selectedRegion ? List.of(2L, 3L) : List.of(1L, 2L), visited);
                }
            });
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testForEach_ExplicitModesVisitSelectedRegionExactlyOnce(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
                final var values = new ConcurrentLinkedQueue<Long>();
                matrix.forEach(0, 2, 1, 3, values::add);
                assertEquals(List.of(2L, 3L, 5L, 6L).stream().sorted().toList(), values.stream().sorted().toList());
                assertEquals(LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testForEachNullAction() {
            long[][] a = { { 1L, 2 }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.forEach((Throwables.LongConsumer<RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.forEach(0, 1, 0, 2, (Throwables.LongConsumer<RuntimeException>) null));
        }

        @Test
        public void testForEach_WithBounds_OutOfBounds() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(-1, 2, 0, 2, v -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 3, 0, 2, v -> {
            }));
        }
    }

    @Nested
    class ToMultilineStringTests {

        // With independent rows guaranteed, every cell is visited exactly once, sequentially
        @Test
        public void testToMultilineStringWithExtremeLongValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { Long.MIN_VALUE, 0L, Long.MAX_VALUE } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains(String.valueOf(Long.MIN_VALUE)));
            assertTrue(printed.contains(String.valueOf(Long.MAX_VALUE)));
        }

        @Test
        public void testToMultilineString_Shape2By2_NonemptyRendering() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            String result = m.toMultilineString();
            assertNotNull(result);
            assertTrue(result.length() > 0);
        }

        @Test
        public void testToMultilineStringNonEmptyReturnsNonNullFormattedString() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains("1"));
            assertTrue(printed.contains("4"));
            assertFalse(printed.equals("null"));
        }

        @Test
        public void testToMultilineStringEmptyReturnsBracketsLiteral() {
            LongMatrix empty = LongMatrix.empty();
            String printed = empty.toMultilineString();
            assertEquals("[]", printed);
        }
    }

    @Nested
    class HashCodeTests {

        @Test
        public void testHashCode() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix1 = LongMatrix.wrap(a);
            LongMatrix matrix2 = LongMatrix.wrap(a.clone());

            // Same content should have same hash code
            Assertions.assertEquals(matrix1.hashCode(), matrix2.hashCode());
        }

        @Test
        public void testHashCode_Shape2By2() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m3 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 4L, 3L } });

            assertEquals(m1.hashCode(), m2.hashCode());
            assertNotEquals(m1.hashCode(), m3.hashCode()); // Usually different
        }

        @Test
        public void testHashCode_Equal() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });

            assertEquals(m1.hashCode(), m2.hashCode());
        }
    }

    @Nested
    class EqualsTests {

        @Test
        public void testEquals() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix1 = LongMatrix.wrap(a);
            LongMatrix matrix2 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix matrix3 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 5L } });
            LongMatrix matrix4 = LongMatrix.wrap(new long[][] { { 1L, 2L } });

            assertTrue(matrix1.equals(matrix1)); // same instance
            Assertions.assertEquals(matrix1, matrix2); // same content
            Assertions.assertNotEquals(matrix1, matrix3); // different content
            Assertions.assertNotEquals(matrix1, matrix4); // different dimensions
            Assertions.assertNotEquals(matrix1, null);
            Assertions.assertNotEquals(matrix1, "not a matrix");
        }

        @Test
        public void testEquals_Same() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertEquals(m1, m2);
        }

        @Test
        public void testEquals_Different() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 5L } });
            assertNotEquals(m1, m2);
        }

        @Test
        public void testEquals_DifferentRowCountsWithEqualColumnCounts() {
            final var longer = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            final var shorter = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertFalse(longer.equals(shorter));
            assertFalse(shorter.equals(longer));
        }

        // Test with zero matrix
        @Test
        public void testEquals_Shape2By2_NullInput() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m3 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 4L, 3L } });
            LongMatrix m4 = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });

            assertTrue(m1.equals(m1)); // Same object
            assertTrue(m1.equals(m2)); // Same values
            assertFalse(m1.equals(m3)); // Different values
            assertFalse(m1.equals(m4)); // Different dimensions
            assertFalse(m1.equals(null));
            assertFalse(m1.equals("not a matrix"));
        }

        @Test
        public void testEquals_Shape2By2_NullInput_DifferentValuesNullAndType() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            LongMatrix m3 = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 5L } });

            assertEquals(m1, m2);
            assertNotEquals(m1, m3);
            assertNotEquals(m1, null);
            assertNotEquals(m1, "not a matrix");
        }

        @Test
        public void testEquals_Null() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertNotEquals(m1, null);
        }
    }

    @Nested
    class ToStringTests {

        @Test
        public void testToString() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            String str = matrix.toString();
            Assertions.assertNotNull(str);
            Assertions.assertTrue(str.contains("1"));
            Assertions.assertTrue(str.contains("4"));
        }

        @Test
        public void testLongMatrixToString() {
            LongMatrix matrix = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals("[[1, 2, 3], [4, 5, 6]]", matrix.toString());
        }

        @Test
        public void testToString_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1"));
            assertTrue(str.contains("2"));
            assertTrue(str.contains("3"));
            assertTrue(str.contains("4"));
        }

        @Test
        public void testToString_Shape2By2_NonemptyRendering() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.length() > 0);
        }
    }

    @Nested
    class ElementTypeTests {

        @Test
        public void testElementType() {
            LongMatrix matrix = LongMatrix.empty();
            Assertions.assertEquals(long.class, matrix.elementType());
        }

        @Test
        public void testElementType_Shape1By1() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L } });
            assertEquals(long.class, m.elementType());
        }
    }

    @Nested
    class UnsafeBackingArrayTests {

        @Test
        public void testToArray() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            long[][] arr = m.unsafeBackingArray();
            assertEquals(2, arr.length);
            assertEquals(2, arr[0].length);
            assertArrayEquals(new long[] { 1L, 2L }, arr[0]);
            assertArrayEquals(new long[] { 3L, 4L }, arr[1]);
        }
    }

    @Nested
    class ElementCountTests {

        @Test
        public void testElementCount() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            assertEquals(6, m.elementCount());
        }

        @Test
        public void testElementCount_Empty() {
            LongMatrix m = LongMatrix.empty();
            assertEquals(0, m.elementCount());
        }
    }

    @Nested
    class IsEmptyTests {

        @Test
        public void testIsEmpty_False() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L } });
            assertFalse(m.isEmpty());
        }

        @Test
        public void testIsEmpty() {
            assertTrue(LongMatrix.empty().isEmpty());
            assertFalse(LongMatrix.wrap(new long[][] { { 1L } }).isEmpty());
        }
    }

    @Nested
    class IsSameShapeTests {

        @Test
        public void testIsSameShape() {
            LongMatrix m1 = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix m2 = LongMatrix.wrap(new long[][] { { 5, 6 }, { 7, 8 } });
            LongMatrix m3 = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }

        @Test
        public void testIsSameShape_NullThrows() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L } });
            assertThrows(IllegalArgumentException.class, () -> m.isSameShape(null));
        }
    }

    @Nested
    class ReshapeTests {

        // Test reshape with too-small dimensions throws exception
        @Test
        public void testReshape() {
            long[][] a = { { 1L, 2L, 3L }, { 4L, 5L, 6L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            LongMatrix reshaped = matrix.reshape(3, 2);
            Assertions.assertEquals(3, reshaped.rowCount());
            Assertions.assertEquals(2, reshaped.columnCount());
            Assertions.assertEquals(1L, reshaped.get(0, 0));
            Assertions.assertEquals(2L, reshaped.get(0, 1));
            Assertions.assertEquals(3L, reshaped.get(1, 0));
            Assertions.assertEquals(4L, reshaped.get(1, 1));

            LongMatrix empty = LongMatrix.empty().reshapeAndPad(2, 3);
            Assertions.assertEquals(2, empty.rowCount());
            Assertions.assertEquals(3, empty.columnCount());

            // Test reshape with too-small dimensions throws exception
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.reshape(1, 4));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.reshape(2, 2));
        }

        @Test
        public void testReshape_Shape3By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            LongMatrix reshaped = m.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1L, reshaped.get(0, i));
            }
        }

        @Test
        public void testReshape_Back() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            LongMatrix reshaped = m.reshape(1, 9);
            LongMatrix reshapedBack = reshaped.reshape(3, 3);
            assertEquals(m, reshapedBack);
        }

        @Test
        public void testReshape_Shape2By3() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            LongMatrix reshaped = m.reshape(3, 2);
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1L, reshaped.get(0, 0));
            assertEquals(2L, reshaped.get(0, 1));
            assertEquals(6L, reshaped.get(2, 1));
        }

        @Test
        public void testReshape_Shape2By3_DimensionsAndRowMajorValues() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L } });
            LongMatrix reshaped = m.reshape(3, 2);
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1L, reshaped.get(0, 0));
            assertEquals(2L, reshaped.get(0, 1));
            assertEquals(3L, reshaped.get(1, 0));
            assertEquals(4L, reshaped.get(1, 1));
        }

        @Test
        public void testReshape_Shape3By3_SingleRowDimensionsAndLastValue() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L }, { 4L, 5L, 6L }, { 7L, 8L, 9L } });
            LongMatrix reshaped = m.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, reshaped.get(0, i));
            }
        }

        @Test
        public void testReshape_SingleRowToMultiRow_PreservesOrder() {
            // Exercise the `a.length == 1` branch of reshape.
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L, 3L, 4L, 5L, 6L } });
            LongMatrix reshaped = m.reshape(3, 2);
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertArrayEquals(new long[] { 1L, 2L }, reshaped.rowCopy(0));
            assertArrayEquals(new long[] { 3L, 4L }, reshaped.rowCopy(1));
            assertArrayEquals(new long[] { 5L, 6L }, reshaped.rowCopy(2));
        }

        @Test
        public void testReshape_OversizedShapeThrowsIllegalArgumentException() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L } });
            assertThrows(IllegalArgumentException.class, () -> m.reshape(46341, 46341));
        }
    }

    @Nested
    class ReshapeAndPadToColumnCountTests {

        @ParameterizedTest(name = "pad into {0} columns")
        @ValueSource(ints = { 1, 3, 7 })
        public void testReshapeAndPadToColumnCount_PreservesOrderAndPadsTrailingCells(final int columns) {
            final LongMatrix matrix = LongMatrix.wrap(new long[] { 1, 2, 3, 4, 5 });
            final LongMatrix result = matrix.reshapeAndPadToColumnCount(columns);
            final int rows = (5 + columns - 1) / columns;
            assertEquals(rows, result.rowCount());
            assertEquals(columns, result.columnCount());
            for (int index = 0; index < rows * columns; index++) {
                assertEquals(index < 5 ? index + 1 : 0, result.get(index / columns, index % columns));
            }
            result.set(0, 0, 99);
            assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, matrix.rowCopy(0));
        }

        @ParameterizedTest
        @ValueSource(ints = { 0, -1 })
        public void testReshapeAndPadToColumnCount_InvalidColumnCount(final int columns) {
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.wrap(new long[] { 1 }).reshapeAndPadToColumnCount(columns));
        }
    }

    @Nested
    class ForEachIndicesTests {

        @Test
        public void testForEachIndex() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            List<String> indices = new ArrayList<>();
            m.forEachIndices((r, c) -> indices.add(r + "," + c));
            assertEquals(4, indices.size());
            assertTrue(indices.contains("0,0"));
            assertTrue(indices.contains("1,1"));
        }
    }

    @Nested
    class Adjacent4PointsTests {

        @Test
        public void testAdjacent4Points() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            List<Sheet.Point> center = m.adjacent4Points(1, 1).toList();
            assertEquals(4, center.size());

            List<Sheet.Point> corner = m.adjacent4Points(0, 0).toList();
            assertEquals(2, corner.size());

            List<Sheet.Point> edge = m.adjacent4Points(0, 1).toList();
            assertEquals(3, edge.size());
        }
    }

    @Nested
    class Adjacent8PointsTests {

        @Test
        public void testAdjacent8Points() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            List<Sheet.Point> center = m.adjacent8Points(1, 1).toList();
            assertEquals(8, center.size());

            List<Sheet.Point> corner = m.adjacent8Points(0, 0).toList();
            assertEquals(3, corner.size());
        }
    }

    @Nested
    class MainDiagonalPointsTests {

        @Test
        public void testPointsMainDiagonal() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Sheet.Point> points = m.mainDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }

        @Test
        public void testPointsMainDiagonal_NonSquareThrows() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(List.of(Point.of(0, 0), Point.of(1, 1)), m.mainDiagonalPoints().toList());
        }
    }

    @Nested
    class AntiDiagonalPointsTests {

        @Test
        public void testPointsAntiDiagonal() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Sheet.Point> points = m.antiDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(2, points.get(0).columnIndex());
        }
    }

    @Nested
    class RowMajorPointsTests {

        @Test
        public void testRowMajorPoints() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            List<Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(0, 1), points.get(1));
            assertEquals(Point.of(1, 0), points.get(2));
            assertEquals(Point.of(1, 1), points.get(3));
        }

        @Test
        public void testRowMajorPointsorizontal() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testRowMajorPointsorizontal_SingleRow() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.rowMajorPoints(0, 0 + 1).toList();
            assertEquals(2, points.size());
        }
    }

    @Nested
    class ColumnMajorPointsTests {

        @Test
        public void testColumnMajorPoints() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            List<Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 0), points.get(1));
            assertEquals(Point.of(0, 1), points.get(2));
            assertEquals(Point.of(1, 1), points.get(3));
        }

        @Test
        public void testColumnMajorPointsertical() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testColumnMajorPointsertical_SingleColumn() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.columnMajorPoints(1, 1 + 1).toList();
            assertEquals(2, points.size());
        }
    }

    @Nested
    class RowPointsTests {

        @Test
        public void testRowPointsows() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<List<Sheet.Point>> rows = m.rowPoints().map(s -> s.toList()).toList();
            assertEquals(3, rows.size());
            assertEquals(2, rows.get(0).size());
        }
    }

    @Nested
    class ColumnPointsTests {

        // Verify transpose for the rowCount > columnCount branch.
        @Test
        public void testColumnPointsolumns() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<List<Sheet.Point>> cols = m.columnPoints().map(s -> s.toList()).toList();
            assertEquals(3, cols.size());
            assertEquals(2, cols.get(0).size());
        }
    }

    @Nested
    class AcceptTests {

        @Test
        public void testAccept() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            long[] sum = { 0 };
            m.accept(matrix -> {
                LongList flat = matrix.flatten();
                for (int i = 0; i < flat.size(); i++) {
                    sum[0] += flat.get(i);
                }
            });
            assertEquals(10L, sum[0]);
        }

        @Test
        public void testAccept_NullThrows() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L } });
            assertThrows(IllegalArgumentException.class, () -> m.accept(null));
        }
    }

    @Nested
    class ApplyTests {

        @Test
        public void testApply() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            long count = m.apply(matrix -> matrix.elementCount());
            assertEquals(4, count);
        }

        @Test
        public void testApply_NullThrows() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L } });
            assertThrows(IllegalArgumentException.class, () -> m.apply(null));
        }
    }

    @Nested
    class AppendToTests {

        @Test
        public void testAppendToWritesIncrementallyAndWrapsSinkFailures() {
            CountingAppendable output = new CountingAppendable(false);
            LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } }).appendTo(output);
            assertEquals("[1, 2]\n[3, 4]", output.toString());
            assertTrue(output.appendCalls > 1);

            assertThrows(UncheckedIOException.class, () -> LongMatrix.wrap(new long[][] { { 1 } }).appendTo(new CountingAppendable(true)));
        }
    }

    @Nested
    class PrintlnTests {

        @Test
        public void testPrintln() {
            long[][] a = { { 1L, 2L }, { 3L, 4L } };
            LongMatrix matrix = LongMatrix.wrap(a);

            assertFalse(matrix.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> matrix.println());
        }

        @Test
        public void testPrintln_Shape2By2() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            assertFalse(m.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> m.println());

            LongMatrix empty = LongMatrix.empty();
            assertTrue(empty.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> empty.println());
        }
    }
}
