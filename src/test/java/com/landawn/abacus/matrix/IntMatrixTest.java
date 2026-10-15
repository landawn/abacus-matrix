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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.IntIteratorEx;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;

class IntMatrixTest extends MatrixTestSupport {

    private IntMatrix matrix;

    private IntMatrix emptyMatrix;

    @BeforeEach
    public void setUp() {
        matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
        emptyMatrix = IntMatrix.empty();
    }

    private static void assertShape(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int rows, final int columns) {
        assertEquals(rows, matrix.rowCount());
        assertEquals(columns, matrix.columnCount());
    }

    @Nested
    class ConstructorTests {

        @Test
        public void testConstructor() {
            // Test with valid array
            int[][] arr = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = new IntMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));

            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> new IntMatrix(null));
        }

        @Test
        public void testConstructor_WithLargeMatrix() {
            int[][] arr = new int[10][10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    arr[i][j] = i * 10 + j;
                }
            }
            IntMatrix m = new IntMatrix(arr);
            assertEquals(10, m.rowCount());
            assertEquals(10, m.columnCount());
            assertEquals(0, m.get(0, 0));
            assertEquals(99, m.get(9, 9));
            assertEquals(55, m.get(5, 5));
        }

        @Test
        public void testConstructor_WithValidArray() {
            int[][] arr = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = new IntMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testConstructor_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> new IntMatrix(null));
        }

        @Test
        public void testConstructor_WithEmptyArray() {
            IntMatrix m = new IntMatrix(new int[0][0]);
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
            assertTrue(m.isEmpty());
        }

        @Test
        public void testConstructor_WithSingleElement() {
            IntMatrix m = new IntMatrix(new int[][] { { 42 } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42, m.get(0, 0));
        }

        @Test
        public void testDuplicateRowsAreRejected() {
            int[] sharedRow = { 1, 2, 3, 4 };
            assertThrows(IllegalArgumentException.class, () -> new IntMatrix(new int[][] { sharedRow, sharedRow }));
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(sharedRow, sharedRow));
        }
    }

    @Nested
    class EmptyTests {

        @Test
        public void testEmpty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.rowCount());
            assertEquals(0, empty.columnCount());
            assertTrue(empty.isEmpty());

            // Test singleton
            assertSame(IntMatrix.empty(), IntMatrix.empty());
        }

        // Test operations with negative values
        @Test
        public void testEmptyMatrixOperations_EmptyMatrix() {
            IntMatrix empty = IntMatrix.empty();

            assertTrue(empty.flatten().isEmpty());
            assertEquals(0, empty.copy().rowCount);
            assertEquals(empty, empty.transpose());
            assertEquals(empty, empty.rotate90());

            IntMatrix extended = empty.resize(2, 2, 5);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals(5, extended.get(0, 0));
        }

        @Test
        public void testIntMatrixEmptyRowCount() {
            // IntMatrix.java: IntMatrix matrix = IntMatrix.empty();
            // matrix.rowCount() returns 0
            IntMatrix matrix = IntMatrix.empty();
            assertEquals(0, matrix.rowCount());
        }

        @Test
        public void testIntMatrixEmptyColumnCount() {
            // IntMatrix.java: matrix.columnCount() returns 0
            IntMatrix matrix = IntMatrix.empty();
            assertEquals(0, matrix.columnCount());
        }
    }

    @Nested
    class WrapTests {

        // Test with valid array
        @Test
        public void testWrap() {
            // Test with valid array
            int[][] arr = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());

            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap((int[][]) null));

            IntMatrix empty2 = IntMatrix.wrap(new int[0][0]);
            assertTrue(empty2.isEmpty());
        }

        @Test
        public void testLargeDimensions() {
            int[][] data = new int[100][50];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 50; j++) {
                    data[i][j] = i + j;
                }
            }
            IntMatrix m = IntMatrix.wrap(data);
            assertEquals(100, m.rowCount());
            assertEquals(50, m.columnCount());
            assertEquals(0, m.get(0, 0));
            assertEquals(148, m.get(99, 49));
        }

        @Test
        public void testWrap_WithValidArray() {
            int[][] arr = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testIntMatrixWrapGet() {
            // IntMatrix.java: IntMatrix matrix = IntMatrix.wrap(new int[][] {{1, 2}, {3, 4}});
            // matrix.get(0, 1) returns 2
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(2, matrix.get(0, 1));
        }

        @Test
        public void testWrapSnapshotsTheOuterArrayButSharesTheRows() {
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

        @Test
        public void testWrap_WithSingleColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 }, { 2 }, { 3 }, { 4 } });
            assertEquals(4, m.rowCount());
            assertEquals(1, m.columnCount());
        }

        @Test
        public void testWrap_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap((int[][]) null));
        }

        @Test
        public void testWrap_WithSingleRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3, 4, 5 } });
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }
    }

    @Nested
    class CopyOfTests {

        @Test
        public void testCopyOf() {
            int[][] arr = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.copyOf(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));

            // Mutating the source array must not affect the copy
            arr[0][0] = 99;
            assertEquals(1, m.get(0, 0));

            // Mutating the copy must not affect the source array
            m.set(1, 1, 88);
            assertEquals(4, arr[1][1]);

            // null -> rejected; empty -> shared empty matrix
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.copyOf((int[][]) null));
            assertTrue(IntMatrix.copyOf(new int[0][0]).isEmpty());

            // non-rectangular -> IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.copyOf(new int[][] { { 1, 2 }, { 3 } }));
        }

        @Test
        public void testCopyOfSeparatesDuplicateSourceRows() {
            int[] sharedRow = { 1, 2 };
            IntMatrix matrix = IntMatrix.copyOf(sharedRow, sharedRow);
            matrix.set(0, 0, 9);
            assertEquals(1, matrix.get(1, 0));
        }

        @Test
        public void testCopyOfValidationOrderAndIndependentEmptyRows() {
            assertEquals("Row 0 cannot be null", assertThrows(IllegalArgumentException.class, () -> IntMatrix.copyOf(null, new int[] { 1 })).getMessage());
            assertEquals("Row 2 cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> IntMatrix.copyOf(new int[] { 1 }, new int[] { 2 }, null)).getMessage());
            assertEquals("Matrix must be rectangular: expected 1 columns per row, but row 1 has 2 columns",
                    assertThrows(IllegalArgumentException.class, () -> IntMatrix.copyOf(new int[] { 1 }, new int[] { 2, 3 }, null)).getMessage());

            final int[] emptyRow = new int[0];
            final IntMatrix copied = IntMatrix.copyOf(emptyRow, emptyRow);
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
        public void testFromFromCharArray() {
            char[][] chars = { { 'A', 'B' }, { 'C', 'D' } };
            IntMatrix m = IntMatrix.from(chars);
            assertEquals(65, m.get(0, 0)); // 'A'
            assertEquals(66, m.get(0, 1)); // 'B'
            assertEquals(67, m.get(1, 0)); // 'C'
            assertEquals(68, m.get(1, 1)); // 'D'

            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from((char[][]) null));
        }

        @Test
        public void testFromFromByteArray() {
            byte[][] bytes = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.from(bytes);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));

            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from((byte[][]) null));
        }

        @Test
        public void testFromFromShortArray() {
            short[][] shorts = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.from(shorts);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));

            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from((short[][]) null));
        }

        @Test
        public void testFromFromCharArray_WithJaggedArray() {
            char[][] jagged = { { 'A', 'B' }, { 'C' } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(jagged));
        }

        @Test
        public void testFromFromByteArray_WithJaggedArray() {
            byte[][] jagged = { { 1, 2 }, { 3 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(jagged));
        }

        @Test
        public void testFromFromShortArray_WithJaggedArray() {
            short[][] jagged = { { 1, 2 }, { 3 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(jagged));
        }

        @Test
        public void testFrom_FromShortArray_DifferentRowLengths() {
            short[][] shorts = { { 1, 2 }, { 3 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(shorts));
        }

        @Test
        public void testFrom_FromCharArray() {
            char[][] chars = { { 'A', 'B' }, { 'C', 'D' } };
            IntMatrix m = IntMatrix.from(chars);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(65, m.get(0, 0)); // ASCII 'A'
            assertEquals(68, m.get(1, 1)); // ASCII 'D'
        }

        @Test
        public void testFrom_FromCharArray_DifferentRowLengths() {
            char[][] chars = { { 'A', 'B' }, { 'C' } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(chars));
        }

        @Test
        public void testFrom_FromByteArray() {
            byte[][] bytes = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.from(bytes);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testFrom_FromByteArray_DifferentRowLengths() {
            byte[][] bytes = { { 1, 2 }, { 3 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(bytes));
        }

        @Test
        public void testFrom_FromShortArray() {
            short[][] shorts = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.from(shorts);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testIntMatrixFromChar() {
            // IntMatrix.java: IntMatrix matrix = IntMatrix.from(new char[][] {{'A', 'B'}, {'C', 'D'}});
            // matrix.get(0, 0) returns 65
            IntMatrix matrix = IntMatrix.from(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertEquals(65, matrix.get(0, 0));
        }

        @Test
        public void testIntMatrixFromByte() {
            // IntMatrix.java: IntMatrix matrix = IntMatrix.from(new byte[][] {{1, 2}, {3, 4}});
            // matrix.get(1, 0) returns 3
            IntMatrix matrix = IntMatrix.from(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(3, matrix.get(1, 0));
        }

        @Test
        public void testIntMatrixFromShort() {
            // IntMatrix.java: IntMatrix matrix = IntMatrix.from(new short[][] {{1, 2}, {3, 4}});
            // matrix.get(1, 1) returns 4
            IntMatrix matrix = IntMatrix.from(new short[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(4, matrix.get(1, 1));
        }

        @Test
        public void testFromByteSignExtends() {
            IntMatrix m = IntMatrix.from(new byte[][] { { -1, -128, 127 } });
            assertArrayEquals(new int[] { -1, -128, 127 }, m.rowCopy(0));
        }

        @Test
        public void testFromShortSignExtends() {
            IntMatrix m = IntMatrix.from(new short[][] { { -1, Short.MIN_VALUE, Short.MAX_VALUE } });
            assertArrayEquals(new int[] { -1, Short.MIN_VALUE, Short.MAX_VALUE }, m.rowCopy(0));
        }

        @Test
        public void testFromCharUnsignedWidening() {
            IntMatrix m = IntMatrix.from(new char[][] { { 'A', '\uFFFF', '\0' } });
            assertArrayEquals(new int[] { 65, 65535, 0 }, m.rowCopy(0));
        }

        @Test
        public void testFromFromCharArray_WithNull() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from((char[][]) null));
        }

        @Test
        public void testFromFromCharArray_WithEmpty() {
            IntMatrix m = IntMatrix.from(new char[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testFromFromCharArray_WithNullRow() {
            char[][] nullRow = { { 'A', 'B' }, null };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(nullRow));
        }

        @Test
        public void testFromFromCharArray_WithNullFirstRow() {
            char[][] nullFirstRow = { null, { 'A', 'B' } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(nullFirstRow));
        }

        @Test
        public void testFromFromByteArray_WithNull() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from((byte[][]) null));
        }

        @Test
        public void testFromFromByteArray_WithNullRow() {
            byte[][] nullRow = { { 1, 2 }, null };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(nullRow));
        }

        @Test
        public void testFromFromShortArray_WithNull() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from((short[][]) null));
        }

        @Test
        public void testFromFromShortArray_WithNullRow() {
            short[][] nullRow = { { 1, 2 }, null };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(nullRow));
        }

        @Test
        public void testFrom_FromCharArray_NullFirstRow() {
            char[][] chars = { null, { 'A', 'B' } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(chars));
        }

        @Test
        public void testFrom_FromByteArray_Empty() {
            IntMatrix m = IntMatrix.from(new byte[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testFrom_FromByteArray_NullFirstRow() {
            byte[][] bytes = { null, { 1, 2 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(bytes));
        }

        @Test
        public void testFrom_FromShortArray_Empty() {
            IntMatrix m = IntMatrix.from(new short[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testFrom_FromShortArray_NullFirstRow() {
            short[][] shorts = { null, { 1, 2 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(shorts));
        }

        @Test
        public void testFromCharArray_ZeroExtensionForHighChars() {
            // Verify that char -> int widening uses zero extension (chars are unsigned)
            char[][] src = { { '\uFFFF', '\0' }, { '\u8000', 'A' } };
            IntMatrix m = IntMatrix.from(src);
            assertEquals(0xFFFF, m.get(0, 0));
            assertEquals(0, m.get(0, 1));
            assertEquals(0x8000, m.get(1, 0));
            assertEquals(65, m.get(1, 1));
        }

        @Test
        public void testFromShortArray_SignExtensionForNegativeValues() {
            // Verify that short -> int widening uses sign extension (not zero extension)
            short[][] src = { { (short) -1, Short.MIN_VALUE }, { (short) 0, Short.MAX_VALUE } };
            IntMatrix m = IntMatrix.from(src);
            assertEquals(-1, m.get(0, 0));
            assertEquals(Short.MIN_VALUE, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
            assertEquals(Short.MAX_VALUE, m.get(1, 1));
        }

        @Test
        public void testFromByteArray_SignExtensionForNegativeValues() {
            // Verify that byte -> int widening uses sign extension
            byte[][] src = { { (byte) -1, Byte.MIN_VALUE }, { (byte) 0, Byte.MAX_VALUE } };
            IntMatrix m = IntMatrix.from(src);
            assertEquals(-1, m.get(0, 0));
            assertEquals(Byte.MIN_VALUE, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
            assertEquals(Byte.MAX_VALUE, m.get(1, 1));
        }

        @Test
        public void testFromByteRaggedRejected() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(new byte[][] { { 1, 2 }, { 3 } }));
        }

        @Test
        public void testFromCharNullSecondRowRejected() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(new char[][] { { 'a', 'b' }, null }));
        }
    }

    @Nested
    class RandomRowTests {

        @Test
        public void testRandom() {
            IntMatrix m = IntMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            // Just verify elements exist (values are random)
            for (int i = 0; i < 5; i++) {
                assertNotNull(m.get(0, i));
            }
        }

        @Test
        public void testRandom_Values() {
            IntMatrix m = IntMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }

        @Test
        public void testRandom_ZeroLength() {
            IntMatrix m = IntMatrix.randomRow(0);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        // Documented: every single int*int product fits in a long, but two terms already overflow it.
        @Test
        public void testFactories_NegativeDimensions_ThrowIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.randomRow(-1));
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.random(-1, 2));
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.random(2, -1));
        }
    }

    @Nested
    class RandomTests {

        @Test
        public void testRandomGeneratorOverloads_AreDeterministicAndPreserveZeroByN() {
            IntMatrix first = IntMatrix.random(2, 3, new java.util.Random(12345L));
            IntMatrix second = IntMatrix.random(2, 3, new java.util.Random(12345L));
            assertEquals(first, second);
            assertEquals(IntMatrix.randomRow(4, new java.util.Random(7L)), IntMatrix.randomRow(4, new java.util.Random(7L)));

            IntMatrix zeroByFive = IntMatrix.random(0, 5, new java.util.Random(1L));
            assertEquals(0, zeroByFive.rowCount());
            assertEquals(5, zeroByFive.columnCount());
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.random(1, 1, (java.util.random.RandomGenerator) null));
        }

        @Test
        public void testRandom_WithRowsCols() {
            IntMatrix m = IntMatrix.random(2, 3);
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
        public void testRange() {
            IntMatrix m = IntMatrix.range(0, 5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            for (int i = 0; i < 5; i++) {
                assertEquals(i, m.get(0, i));
            }
        }

        @Test
        public void testRangeWithStep() {
            IntMatrix m = IntMatrix.range(0, 10, 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals(0, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(4, m.get(0, 2));
            assertEquals(6, m.get(0, 3));
            assertEquals(8, m.get(0, 4));
        }

        @Test
        public void testRange_ArrayValues() {
            IntMatrix m = IntMatrix.range(0, 5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, m.rowView(0));
        }

        @Test
        public void testRange_WithStep() {
            IntMatrix m = IntMatrix.range(0, 10, 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new int[] { 0, 2, 4, 6, 8 }, m.rowView(0));
        }

        @Test
        public void testIntMatrixRange() {
            // IntMatrix.java: IntMatrix matrix = IntMatrix.range(0, 5);   // Creates [[0, 1, 2, 3, 4]]
            IntMatrix matrix = IntMatrix.range(0, 5);
            assertEquals("[[0, 1, 2, 3, 4]]", matrix.toString());
        }

        @Test
        public void testIntMatrixRangeWithStep() {
            // IntMatrix.java: IntMatrix matrix = IntMatrix.range(0, 10, 2);   // Creates [[0, 2, 4, 6, 8]]
            IntMatrix matrix = IntMatrix.range(0, 10, 2);
            assertEquals("[[0, 2, 4, 6, 8]]", matrix.toString());
        }

        @Test
        public void testIntMatrixRangeDescending() {
            // IntMatrix.java: IntMatrix desc = IntMatrix.range(10, 0, -2);    // Creates [[10, 8, 6, 4, 2]]
            IntMatrix desc = IntMatrix.range(10, 0, -2);
            assertEquals("[[10, 8, 6, 4, 2]]", desc.toString());
        }

        @Test
        public void testRange_IntegerBoundsAndExtremeSteps() {
            final int max = Integer.MAX_VALUE;
            final int min = Integer.MIN_VALUE;

            // Endpoints at the int bounds must not overflow the stepping loop.
            assertArrayEquals(new int[] { max - 2, max - 1 }, IntMatrix.range(max - 2, max).rowView(0));
            assertArrayEquals(new int[] { max - 1, max }, IntMatrix.rangeClosed(max - 1, max).rowView(0));
            assertArrayEquals(new int[] { min, min + 1 }, IntMatrix.rangeClosed(min, min + 1).rowView(0));
            assertArrayEquals(new int[] { max - 4, max - 2, max }, IntMatrix.rangeClosed(max - 4, max, 2).rowView(0));
            assertArrayEquals(new int[] { min + 4, min + 2, min }, IntMatrix.rangeClosed(min + 4, min, -2).rowView(0));
            assertArrayEquals(new int[] { max }, IntMatrix.rangeClosed(max, max, min).rowView(0));

            // Extreme steps: the next value would overflow int, so iteration stops.
            assertArrayEquals(new int[] { min, -1, max - 1 }, IntMatrix.range(min, max, max).rowView(0));
            assertArrayEquals(new int[] { min, -1, max - 1 }, IntMatrix.rangeClosed(min, max, max).rowView(0));
            assertArrayEquals(new int[] { max, -1 }, IntMatrix.range(max, min, min).rowView(0));
            assertArrayEquals(new int[] { max, -1 }, IntMatrix.rangeClosed(max, min, min).rowView(0));
            assertEquals(0, IntMatrix.range(0, 10, min).columnCount());
            assertEquals(1, IntMatrix.range(0, 10, min).rowCount());

            // More than Integer.MAX_VALUE elements is rejected with IllegalArgumentException, not wrapped.
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.range(min, max));
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.rangeClosed(0, max));
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.rangeClosed(min, max, 1));
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.range(max, min, -1));
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.rangeClosed(min, max, 2));
        }

        @Test
        public void testRange_Empty() {
            IntMatrix m = IntMatrix.range(5, 5);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testIntMatrixRangeEmpty() {
            // IntMatrix.java: IntMatrix empty = IntMatrix.range(5, 0);    // Creates an empty matrix
            // range(5,0) wraps Array.range(5,0) which returns int[0], so result is 1 row with 0 columns
            IntMatrix empty = IntMatrix.range(5, 0);
            assertEquals(1, empty.rowCount());
            assertEquals(0, empty.columnCount());
        }

        @Test
        public void testRange_WithNegativeStep() {
            IntMatrix m = IntMatrix.range(10, 0, -2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new int[] { 10, 8, 6, 4, 2 }, m.rowView(0));
        }
    }

    @Nested
    class RangeClosedTests {

        @Test
        public void testRangeClosed() {
            IntMatrix m = IntMatrix.rangeClosed(0, 4);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            for (int i = 0; i < 5; i++) {
                assertEquals(i, m.get(0, i));
            }
        }

        @Test
        public void testRangeClosedWithStep() {
            IntMatrix m = IntMatrix.rangeClosed(0, 10, 2);
            assertEquals(1, m.rowCount());
            assertEquals(6, m.columnCount());
            assertEquals(0, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(4, m.get(0, 2));
            assertEquals(6, m.get(0, 3));
            assertEquals(8, m.get(0, 4));
            assertEquals(10, m.get(0, 5));
        }

        @Test
        public void testRangeClosed_ArrayValues() {
            IntMatrix m = IntMatrix.rangeClosed(0, 4);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, m.rowView(0));
        }

        @Test
        public void testRangeClosed_WithStep() {
            IntMatrix m = IntMatrix.rangeClosed(0, 10, 2);
            assertEquals(1, m.rowCount());
            assertEquals(6, m.columnCount());
            assertArrayEquals(new int[] { 0, 2, 4, 6, 8, 10 }, m.rowView(0));
        }

        @Test
        public void testIntMatrixRangeClosed() {
            // Similar to range but inclusive end
            IntMatrix matrix = IntMatrix.rangeClosed(1, 5);
            assertEquals("[[1, 2, 3, 4, 5]]", matrix.toString());
        }

        @Test
        public void testIntMatrixRangeClosedWithStep() {
            IntMatrix matrix = IntMatrix.rangeClosed(0, 10, 2);
            assertEquals("[[0, 2, 4, 6, 8, 10]]", matrix.toString());
        }

        @Test
        public void testIntClosedRangeIncludesEqualEndpointsForEitherStepDirection() {
            for (int endpoint : new int[] { Integer.MIN_VALUE, 0, Integer.MAX_VALUE }) {
                for (int step : new int[] { Integer.MIN_VALUE, -1, 1, Integer.MAX_VALUE }) {
                    final IntMatrix result = IntMatrix.rangeClosed(endpoint, endpoint, step);
                    assertEquals(1, result.rowCount());
                    assertArrayEquals(new int[] { endpoint }, result.rowCopy(0));
                }
                assertThrows(IllegalArgumentException.class, () -> IntMatrix.rangeClosed(endpoint, endpoint, 0));
            }
            assertEquals(0, IntMatrix.rangeClosed(1, 2, -1).columnCount());
            assertEquals(0, IntMatrix.rangeClosed(2, 1, 1).columnCount());
        }

        @Test
        public void testRangeClosed_SingleValue() {
            IntMatrix m = IntMatrix.rangeClosed(5, 5);
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(5, m.get(0, 0));
        }
    }

    @Nested
    class OfMainDiagonalTests {

        @Test
        public void testOfMainDiagonal() {
            int[] diag = { 1, 2, 3 };
            IntMatrix m = IntMatrix.ofMainDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
            assertEquals(0, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
        }

        @Test
        public void testIntMatrixMainDiagonal() {
            // IntMatrix mainDiagonal
            IntMatrix matrix = IntMatrix.ofMainDiagonal(new int[] { 1, 2, 3 });
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertEquals(1, matrix.get(0, 0));
            assertEquals(2, matrix.get(1, 1));
            assertEquals(3, matrix.get(2, 2));
            assertEquals(0, matrix.get(0, 1));
            assertEquals(0, matrix.get(1, 0));
        }

        @Test
        public void testDiagonalLU2RD() {
            IntMatrix m = IntMatrix.ofMainDiagonal(new int[] { 1, 2, 3 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
            assertEquals(0, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
        }

        @Test
        public void testOfMainDiagonal_Null() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofMainDiagonal(null));
        }
    }

    @Nested
    class OfAntiDiagonalTests {

        @Test
        public void testOfAntiDiagonal() {
            int[] diag = { 1, 2, 3 };
            IntMatrix m = IntMatrix.ofAntiDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 2));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 0));
            assertEquals(0, m.get(0, 0));
        }

        @Test
        public void testIntMatrixAntiDiagonal() {
            IntMatrix matrix = IntMatrix.ofAntiDiagonal(new int[] { 1, 2, 3 });
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertEquals(1, matrix.get(0, 2));
            assertEquals(2, matrix.get(1, 1));
            assertEquals(3, matrix.get(2, 0));
            assertEquals(0, matrix.get(0, 0));
        }

        @Test
        public void testDiagonalRU2LD() {
            IntMatrix m = IntMatrix.ofAntiDiagonal(new int[] { 1, 2, 3 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 2));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 0));
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(2, 2));
        }

        @Test
        public void testDiagonalRU2LD_Values() {
            IntMatrix m = IntMatrix.ofAntiDiagonal(new int[] { 1, 2, 3 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 2));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 0));
            assertEquals(0, m.get(0, 0));
        }

        @Test
        public void testOfAntiDiagonal_Null() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofAntiDiagonal(null));
        }
    }

    @Nested
    class OfDiagonalsTests {

        @Test
        public void testOfDiagonals() {
            IntMatrix matrix = IntMatrix.ofDiagonals(new int[] { 1, 2, 3 }, new int[] { 4, 5, 6 });
            // Resulting matrix:
            //   {1, 0, 4},
            //   {0, 2, 0},
            //   {6, 0, 3}
            assertEquals(1, matrix.get(0, 0));
            assertEquals(0, matrix.get(0, 1));
            assertEquals(4, matrix.get(0, 2));
            assertEquals(0, matrix.get(1, 0));
            assertEquals(2, matrix.get(1, 1));
            assertEquals(0, matrix.get(1, 2));
            assertEquals(6, matrix.get(2, 0));
            assertEquals(0, matrix.get(2, 1));
            assertEquals(3, matrix.get(2, 2));
        }

        @Test
        public void testDiagonal() {
            // Test with both diagonals
            IntMatrix m = IntMatrix.ofDiagonals(new int[] { 1, 2, 3 }, new int[] { 4, 5, 6 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
            assertEquals(4, m.get(0, 2));
            assertEquals(2, m.get(1, 1)); // Overwritten
            assertEquals(6, m.get(2, 0));

            // Test with only main diagonal
            IntMatrix m2 = IntMatrix.ofDiagonals(new int[] { 1, 2, 3 }, null);
            assertEquals(1, m2.get(0, 0));
            assertEquals(2, m2.get(1, 1));
            assertEquals(3, m2.get(2, 2));

            // Test with only anti-diagonal
            IntMatrix m3 = IntMatrix.ofDiagonals(null, new int[] { 4, 5, 6 });
            assertEquals(4, m3.get(0, 2));
            assertEquals(5, m3.get(1, 1));
            assertEquals(6, m3.get(2, 0));

            // Test with both null
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofDiagonals(null, null));

            // Test illegal argument
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofDiagonals(new int[] { 1, 2 }, new int[] { 3, 4, 5 }));
        }

        @Test
        public void testDiagonal_WithBothDiagonals() {
            IntMatrix m = IntMatrix.ofDiagonals(new int[] { 1, 2, 3 }, new int[] { 4, 5, 6 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
            assertEquals(4, m.get(0, 2));
            assertEquals(6, m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithOnlyMainDiagonal() {
            IntMatrix m = IntMatrix.ofDiagonals(new int[] { 1, 2, 3 }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
        }

        @Test
        public void testDiagonal_WithOnlyAntiDiagonal() {
            IntMatrix m = IntMatrix.ofDiagonals(null, new int[] { 4, 5, 6 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(4, m.get(0, 2));
            assertEquals(5, m.get(1, 1));
            assertEquals(6, m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithDifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofDiagonals(new int[] { 1, 2 }, new int[] { 3, 4, 5 }));
        }

        @Test
        public void testDiagonal_BothDiagonals() {
            IntMatrix m = IntMatrix.ofDiagonals(new int[] { 1, 2, 3 }, new int[] { 4, 5, 6 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
            assertEquals(4, m.get(0, 2));
            assertEquals(2, m.get(1, 1));
            assertEquals(6, m.get(2, 0));
        }

        @Test
        public void testDiagonal_DifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofDiagonals(new int[] { 1, 2 }, new int[] { 1, 2, 3 }));
        }

        @Test
        public void testDiagonal_Both() {
            int[] lu2rd = { 1, 2, 3 };
            int[] ru2ld = { 4, 5, 6 };
            IntMatrix m = IntMatrix.ofDiagonals(lu2rd, ru2ld);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
            assertEquals(4, m.get(0, 2));
            assertEquals(6, m.get(2, 0));
        }

        @Test
        public void testDiagonal_DifferentLengths_Validation() {
            int[] lu2rd = { 1, 2 };
            int[] ru2ld = { 4, 5, 6 };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofDiagonals(lu2rd, ru2ld));
        }

        @Test
        public void testDiagonal_WithBothNull() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofDiagonals(null, null));
        }

        @Test
        public void testOfDiagonals_EmptyAndNullSidesPreserveEmptyShape() {
            assertTrue(IntMatrix.ofDiagonals(new int[0], new int[0]).isEmpty());
            assertTrue(IntMatrix.ofDiagonals(new int[0], null).isEmpty());
            assertTrue(IntMatrix.ofDiagonals(null, new int[0]).isEmpty());
        }
    }

    @Nested
    class UnboxTests {

        @Test
        public void testUnbox() {
            Integer[][] boxed = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> boxedMatrix = Matrix.wrap(boxed);
            IntMatrix unboxed = IntMatrix.unbox(boxedMatrix);
            assertEquals(1, unboxed.get(0, 0));
            assertEquals(2, unboxed.get(0, 1));
            assertEquals(3, unboxed.get(1, 0));
            assertEquals(4, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_Values() {
            Integer[][] boxed = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> boxedMatrix = Matrix.wrap(boxed);
            IntMatrix unboxed = IntMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertEquals(1, unboxed.get(0, 0));
            assertEquals(4, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_Shape2By2() {
            Matrix<Integer> boxed = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m = IntMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testUnboxSharedEmptyAndPreserveZeroColumnRows() {
            IntMatrix sharedEmpty = IntMatrix.unbox(Matrix.empty(Integer.class));
            assertEquals(IntMatrix.empty(), sharedEmpty);

            IntMatrix zeroColumns = IntMatrix.unbox(Matrix.wrap(new Integer[][] { {}, {} }));
            assertEquals(2, zeroColumns.rowCount());
            assertEquals(0, zeroColumns.columnCount());

            Matrix<Integer> typeNeutralZeroColumns = Matrix.empty(Integer.class).resize(2, 0);
            IntMatrix unboxedTypeNeutralZeroColumns = IntMatrix.unbox(typeNeutralZeroColumns);
            assertEquals(2, unboxedTypeNeutralZeroColumns.rowCount());
            assertEquals(0, unboxedTypeNeutralZeroColumns.columnCount());

            Matrix<Integer> typeNeutralValues = Matrix.empty(Integer.class).resize(2, 2);
            typeNeutralValues.set(0, 1, 7);
            IntMatrix unboxedTypeNeutralValues = IntMatrix.unbox(typeNeutralValues);
            assertArrayEquals(new int[] { 0, 7 }, unboxedTypeNeutralValues.rowCopy(0));
            assertArrayEquals(new int[] { 0, 0 }, unboxedTypeNeutralValues.rowCopy(1));
        }

        @Test
        public void testUnbox_WithNullValues() {
            Integer[][] boxed = { { 1, null }, { null, 4 } };
            Matrix<Integer> boxedMatrix = Matrix.wrap(boxed);
            IntMatrix unboxed = IntMatrix.unbox(boxedMatrix);
            assertEquals(1, unboxed.get(0, 0));
            assertEquals(0, unboxed.get(0, 1)); // null -> 0
            assertEquals(0, unboxed.get(1, 0)); // null -> 0
            assertEquals(4, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_WithNulls() {
            Matrix<Integer> boxed = Matrix.wrap(new Integer[][] { { 1, null }, { null, 4 } });
            IntMatrix m = IntMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(0, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
        }
    }

    @Nested
    class GetTests {

        // Test bounds
        @Test
        public void testGet() {
            assertEquals(1, matrix.get(0, 0));
            assertEquals(5, matrix.get(1, 1));
            assertEquals(9, matrix.get(2, 2));

            // Test bounds
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(-1, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(3, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(0, -1));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(0, 3));
        }

        @Test
        public void testGetWithPoint() {
            Sheet.Point p1 = Sheet.Point.of(0, 0);
            assertEquals(1, matrix.get(p1));

            Sheet.Point p2 = Sheet.Point.of(1, 1);
            assertEquals(5, matrix.get(p2));

            Sheet.Point p3 = Sheet.Point.of(2, 2);
            assertEquals(9, matrix.get(p3));
        }

        @Test
        public void testGet_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(5, m.get(1, 1));
            assertEquals(6, m.get(1, 2));
        }

        @Test
        public void testGetWithPoint_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(1, m.get(Point.of(0, 0)));
            assertEquals(4, m.get(Point.of(1, 1)));
            assertEquals(2, m.get(Point.of(0, 1)));
        }

        @Test
        public void testGet_WithPoint() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Point p = Point.of(1, 0);
            assertEquals(3, m.get(p));
        }

        @Test
        public void testGet_ByIndices() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testGet_ByPoint() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Point p = Point.of(0, 1);
            assertEquals(2, m.get(p));
        }

        @Test
        public void testGet_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(-1, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(2, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(0, -1));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(0, 2));
        }
    }

    @Nested
    class SetTests {

        // Test bounds
        @Test
        public void testSet() {
            IntMatrix m = matrix.copy();
            m.set(0, 0, 10);
            assertEquals(10, m.get(0, 0));

            m.set(1, 1, 20);
            assertEquals(20, m.get(1, 1));

            // Test bounds
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(3, 0, 0));
        }

        @Test
        public void testSetWithPoint() {
            IntMatrix m = matrix.copy();
            Sheet.Point p = Sheet.Point.of(1, 1);
            m.set(p, 50);
            assertEquals(50, m.get(p));
        }

        // Bottom row has no element below
        @Test
        public void testSet_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.set(0, 0, 10);
            assertEquals(10, m.get(0, 0));

            m.set(1, 1, 20);
            assertEquals(20, m.get(1, 1));
        }

        @Test
        public void testSetWithPoint_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.set(Point.of(0, 0), 50);
            assertEquals(50, m.get(Point.of(0, 0)));
        }

        @Test
        public void testSet_WithPoint() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Point p = Point.of(1, 0);
            m.set(p, 99);
            assertEquals(99, m.get(1, 0));
        }

        @Test
        public void testSet_ByIndices() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.set(0, 1, 9);
            assertEquals(9, m.get(0, 1));
        }

        @Test
        public void testSet_ByPoint() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Point p = Point.of(1, 1);
            m.set(p, 9);
            assertEquals(9, m.get(p));
        }

        @Test
        public void testSet_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(2, 0, 0));
        }
    }

    @Nested
    class ValueAboveTests {

        @Test
        public void testValueAbove() {
            OptionalInt up = matrix.valueAbove(1, 1);
            assertTrue(up.isPresent());
            assertEquals(2, up.get());

            // Test top row
            OptionalInt empty = matrix.valueAbove(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            OptionalInt up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1, up.get());

            // Top row has no element above
            OptionalInt empty = m.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_Shape2By2_PresentNeighborValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1, up.get());
        }

        @Test
        public void testValueAbove_FirstRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt up = m.valueAbove(0, 0);
            assertFalse(up.isPresent());
        }

        @Test
        public void testAbove() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            u.OptionalInt value = matrix.valueAbove(1, 0);
            assertEquals(1, value.get());
            u.OptionalInt empty = matrix.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_Shape2By2_InteriorValueAndMissingBoundary() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1, up.get());

            OptionalInt noUp = m.valueAbove(0, 0);
            assertFalse(noUp.isPresent());
        }
    }

    @Nested
    class ValueBelowTests {

        @Test
        public void testValueBelow() {
            OptionalInt down = matrix.valueBelow(1, 1);
            assertTrue(down.isPresent());
            assertEquals(8, down.get());

            // Test bottom row
            OptionalInt empty = matrix.valueBelow(2, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            OptionalInt down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3, down.get());

            // Bottom row has no element below
            OptionalInt empty = m.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_Shape2By2_PresentNeighborValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3, down.get());
        }

        @Test
        public void testValueBelow_LastRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt down = m.valueBelow(1, 0);
            assertFalse(down.isPresent());
        }

        @Test
        public void testBelow() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            u.OptionalInt value = matrix.valueBelow(0, 0);
            assertEquals(3, value.get());
            u.OptionalInt empty = matrix.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_Shape2By2_InteriorValueAndMissingBoundary() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3, down.get());

            OptionalInt noDown = m.valueBelow(1, 0);
            assertFalse(noDown.isPresent());
        }
    }

    @Nested
    class ValueLeftTests {

        @Test
        public void testValueLeft() {
            OptionalInt left = matrix.valueLeft(1, 1);
            assertTrue(left.isPresent());
            assertEquals(4, left.get());

            // Test leftmost column
            OptionalInt empty = matrix.valueLeft(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            OptionalInt left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1, left.get());

            // Leftmost column has no element to the left
            OptionalInt empty = m.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_Shape2By2_PresentNeighborValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1, left.get());
        }

        @Test
        public void testValueLeft_FirstColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt left = m.valueLeft(0, 0);
            assertFalse(left.isPresent());
        }

        @Test
        public void testLeft() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            u.OptionalInt value = matrix.valueLeft(0, 1);
            assertEquals(1, value.get());
            u.OptionalInt empty = matrix.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_Shape2By2_InteriorValueAndMissingBoundary() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1, left.get());

            OptionalInt noLeft = m.valueLeft(0, 0);
            assertFalse(noLeft.isPresent());
        }
    }

    @Nested
    class ValueRightTests {

        @Test
        public void testValueRight() {
            OptionalInt right = matrix.valueRight(1, 1);
            assertTrue(right.isPresent());
            assertEquals(6, right.get());

            // Test rightmost column
            OptionalInt empty = matrix.valueRight(1, 2);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            OptionalInt right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2, right.get());

            // Rightmost column has no element to the right
            OptionalInt empty = m.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_Shape2By2_PresentNeighborValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2, right.get());
        }

        @Test
        public void testValueRight_LastColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt right = m.valueRight(0, 1);
            assertFalse(right.isPresent());
        }

        @Test
        public void testRight() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            u.OptionalInt value = matrix.valueRight(0, 0);
            assertEquals(2, value.get());
            u.OptionalInt empty = matrix.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_Shape2By2_InteriorValueAndMissingBoundary() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2, right.get());

            OptionalInt noRight = m.valueRight(0, 1);
            assertFalse(noRight.isPresent());
        }
    }

    @Nested
    class RowViewTests {

        @Test
        public void testRow() {
            int[] row0 = matrix.rowView(0);
            assertArrayEquals(new int[] { 1, 2, 3 }, row0);

            int[] row1 = matrix.rowView(1);
            assertArrayEquals(new int[] { 4, 5, 6 }, row1);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(3));
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new int[] { 1, 2, 3 }, m.rowView(0));
            assertArrayEquals(new int[] { 4, 5, 6 }, m.rowView(1));
        }

        @Test
        public void testRowView_Shape2By2_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[] row = m.rowView(0);
            assertArrayEquals(new int[] { 1, 2 }, row);
        }

        @Test
        public void testRowView() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] firstRow = matrix.rowView(0);
            assertArrayEquals(new int[] { 1, 2, 3 }, firstRow);
        }

        @Test
        public void testLength() {
            // This is a protected method, test indirectly through row operations
            int[] row = matrix.rowView(0);
            assertEquals(3, row.length);
        }

        @Test
        public void testRowView_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(2));
        }

        @Test
        public void testRowView_InvalidIndex() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(1));
        }

        @Test
        public void testRowView_InvalidIndex_Shape1By2_Validation() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(5));
        }
    }

    @Nested
    class RowCopyTests {

        @Test
        public void testRowCopy() {
            int[] rowCopy = matrix.rowCopy(0);
            assertArrayEquals(new int[] { 1, 2, 3 }, rowCopy);

            rowCopy[0] = 99;
            assertArrayEquals(new int[] { 1, 2, 3 }, matrix.rowView(0));
        }
    }

    @Nested
    class ColumnCopyTests {

        @Test
        public void testColumn() {
            int[] col0 = matrix.columnCopy(0);
            assertArrayEquals(new int[] { 1, 4, 7 }, col0);

            int[] col1 = matrix.columnCopy(1);
            assertArrayEquals(new int[] { 2, 5, 8 }, col1);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(3));
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new int[] { 1, 4 }, m.columnCopy(0));
            assertArrayEquals(new int[] { 2, 5 }, m.columnCopy(1));
            assertArrayEquals(new int[] { 3, 6 }, m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_Shape2By2_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[] col = m.columnCopy(0);
            assertArrayEquals(new int[] { 1, 3 }, col);
        }

        @Test
        public void testColumnCopy() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] firstColumn = matrix.columnCopy(0);
            assertArrayEquals(new int[] { 1, 4 }, firstColumn);
        }

        @Test
        public void testColumnCopy_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex_Shape1By2_Validation() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(5));
        }
    }

    @Nested
    class SetRowTests {

        @Test
        public void testSetRow() {
            IntMatrix m = matrix.copy();
            m.setRow(0, new int[] { 10, 20, 30 });
            assertArrayEquals(new int[] { 10, 20, 30 }, m.rowView(0));

            // Test wrong size
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new int[] { 1, 2 }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, (int[]) null));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setRow(0, new int[] { 10, 20 });
            assertArrayEquals(new int[] { 10, 20 }, m.rowView(0));
            assertArrayEquals(new int[] { 3, 4 }, m.rowView(1)); // unchanged
        }

        @Test
        public void testSetRow_WrongSize() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new int[] { 1 }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new int[] { 1, 2, 3 }));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues_RowReplacementAndNeighborPreserved() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setRow(0, new int[] { 10, 20 });
            assertArrayEquals(new int[] { 10, 20 }, m.rowView(0));
            assertEquals(3, m.get(1, 0));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues_ReplacesFirstRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setRow(0, new int[] { 9, 8 });
            assertArrayEquals(new int[] { 9, 8 }, m.rowView(0));
        }

        @Test
        public void testSetRow_InvalidLength() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new int[] { 1, 2, 3 }));
        }

        @Test
        public void testSetRow_InvalidRowIndex() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setRow(5, new int[] { 1, 2 }));
        }

        @Test
        public void testSetRow_InvalidLength_Shape1By2_Validation() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new int[] { 1 }));
        }
    }

    @Nested
    class SetColumnTests {

        @Test
        public void testSetColumn() {
            IntMatrix m = matrix.copy();
            m.setColumn(0, new int[] { 10, 20, 30 });
            assertArrayEquals(new int[] { 10, 20, 30 }, m.columnCopy(0));

            // Test wrong size
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new int[] { 1, 2 }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, (int[]) null));
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setColumn(0, new int[] { 10, 20 });
            assertArrayEquals(new int[] { 10, 20 }, m.columnCopy(0));
            assertArrayEquals(new int[] { 2, 4 }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testSetColumn_WrongSize() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new int[] { 1 }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new int[] { 1, 2, 3 }));
        }

        @Test
        public void testSetColumn_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setColumn(0, new int[] { 10, 30 });
            assertEquals(10, m.get(0, 0));
            assertEquals(30, m.get(1, 0));
            assertEquals(2, m.get(0, 1));
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues_ReplacesFirstColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setColumn(0, new int[] { 9, 8 });
            assertArrayEquals(new int[] { 9, 8 }, m.columnCopy(0));
        }

        // Identity multiplication.
        @Test
        public void testSetColumn_ValidInput() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            m.setColumn(1, new int[] { 20, 50 });

            assertEquals(20, m.get(0, 1));
            assertEquals(50, m.get(1, 1));
            // Other columns unchanged
            assertEquals(1, m.get(0, 0));
            assertEquals(3, m.get(0, 2));
            assertEquals(4, m.get(1, 0));
            assertEquals(6, m.get(1, 2));
        }

        @Test
        public void testAliasedArraySourcesAreSnapshotted() {
            IntMatrix columnMatrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            columnMatrix.setColumn(1, columnMatrix.rowView(0));
            assertArrayEquals(new int[] { 3, 2 }, columnMatrix.rowCopy(1));

            IntMatrix diagonalMatrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            diagonalMatrix.setAntiDiagonal(diagonalMatrix.rowView(0));
            assertArrayEquals(new int[] { 1, 2, 3 }, diagonalMatrix.antiDiagonalCopy());

            int[][] backing = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            IntMatrix fillMatrix = IntMatrix.wrap(backing);
            fillMatrix.copyFrom(1, 0, backing);
            assertArrayEquals(new int[] { 3, 4 }, fillMatrix.rowCopy(2));
        }

        @Test
        public void testSetColumn_InvalidLength() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new int[] { 1, 2, 3 }));
        }

        @Test
        public void testSetColumn_InvalidColumnIndex() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(5, new int[] { 1 }));
        }

        @Test
        public void testSetColumn_ThrowsConsistentExceptions() {
            // Bug fix: IntMatrix.setColumn() had extra 'ArrayIndexOutOfBoundsException' in throws
            // declaration, inconsistent with all other matrix classes. Verify it throws
            // IllegalArgumentException for invalid inputs, consistent with the other matrix classes.
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            // Out-of-bounds column index should throw IndexOutOfBoundsException
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(-1, new int[] { 10, 20 }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(3, new int[] { 10, 20 }));

            // Column length mismatch should throw IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new int[] { 10, 20, 30 }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new int[] { 10 }));
        }
    }

    @Nested
    class UpdateRowTests {

        @Test
        public void testUpdateRow() {
            IntMatrix m = matrix.copy();
            m.updateRow(0, x -> x * 2);
            assertArrayEquals(new int[] { 2, 4, 6 }, m.rowView(0));
        }

        @Test
        public void testUpdateRow_Shape2By2_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateRow(0, x -> x * 2);
            assertArrayEquals(new int[] { 2, 4 }, m.rowView(0));
            assertArrayEquals(new int[] { 3, 4 }, m.rowView(1));
        }

        @Test
        public void testupdateRow() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            matrix.updateRow(0, x -> x * 2);
            assertArrayEquals(new int[] { 2, 4, 6 }, matrix.rowView(0));
            assertArrayEquals(new int[] { 4, 5, 6 }, matrix.rowView(1));
        }

        @Test
        public void testInPlaceCallbacksRetainCompletedUpdatesWhenTheyThrow() {
            final IllegalStateException failure = new IllegalStateException("stop at the second element");
            final Throwables.IntUnaryOperator<RuntimeException> operator = value -> {
                if (value == 2) {
                    throw failure;
                }
                return value * 10;
            };
            final List<Throwables.Consumer<IntMatrix, RuntimeException>> updates = List.of(m -> m.updateRow(0, operator), m -> m.updateColumn(0, operator),
                    m -> m.updateMainDiagonal(operator), m -> m.updateAntiDiagonal(operator), m -> m.updateAll(operator),
                    m -> m.updateAll((i, j) -> operator.applyAsInt(m.get(i, j))), m -> m.replaceIf(value -> operator.applyAsInt(value) > 0, 10),
                    m -> m.replaceIf((i, j) -> operator.applyAsInt(m.get(i, j)) > 0, 10));

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                for (int operation = 0; operation < updates.size(); operation++) {
                    final IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 1 }, { 2, 2, 2 }, { 1, 2, 1 } });
                    final Throwables.Consumer<IntMatrix, RuntimeException> update = updates.get(operation);
                    assertSame(failure, assertThrows(IllegalStateException.class, () -> update.accept(m)));
                    final int[][] expected = { { 1, 2, 1 }, { 2, 2, 2 }, { 1, 2, 1 } };
                    expected[0][operation == 3 ? 2 : 0] = 10;
                    assertEquals(IntMatrix.wrap(expected), m, "operation " + operation);
                }
            });
        }

        @Test
        public void testUpdateRowAndUpdateColumnNullOperator() {
            IntMatrix m = matrix.copy();

            assertThrows(IllegalArgumentException.class, () -> m.updateRow(0, (Throwables.IntUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> m.updateColumn(0, (Throwables.IntUnaryOperator<RuntimeException>) null));
        }

        @Test
        public void testUpdateRowAndUpdateColumnInvalidIndex() {
            IntMatrix m = matrix.copy();

            assertThrows(IndexOutOfBoundsException.class, () -> m.updateRow(-1, x -> x * 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.updateColumn(3, x -> x + 10));
        }
    }

    @Nested
    class UpdateColumnTests {

        @Test
        public void testUpdateColumn() {
            IntMatrix m = matrix.copy();
            m.updateColumn(0, x -> x + 10);
            assertArrayEquals(new int[] { 11, 14, 17 }, m.columnCopy(0));
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateColumn(0, x -> x + 10);
            assertArrayEquals(new int[] { 11, 13 }, m.columnCopy(0));
            assertArrayEquals(new int[] { 2, 4 }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testUpdateColumn_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateColumn(0, x -> x * 10);
            assertEquals(10, m.get(0, 0));
            assertEquals(30, m.get(1, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues_PreservesOtherColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateColumn(0, x -> x * 2);
            assertArrayEquals(new int[] { 2, 6 }, m.columnCopy(0));
            assertArrayEquals(new int[] { 2, 4 }, m.columnCopy(1));
        }

        @Test
        public void testupdateColumn() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            matrix.updateColumn(0, x -> x + 10);
            assertEquals(11, matrix.get(0, 0));
            assertEquals(2, matrix.get(0, 1));
            assertEquals(13, matrix.get(1, 0));
            assertEquals(4, matrix.get(1, 1));
            assertEquals(15, matrix.get(2, 0));
            assertEquals(6, matrix.get(2, 1));
        }
    }

    @Nested
    class MainDiagonalCopyTests {

        @Test
        public void testGetLU2RD() {
            int[] diagonal = matrix.mainDiagonalCopy();
            assertArrayEquals(new int[] { 1, 5, 9 }, diagonal);

            // Test non-square matrix
            IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertArrayEquals(new int[] { 1 }, nonSquare.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_Shape3By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertArrayEquals(new int[] { 1, 5, 9 }, m.mainDiagonalCopy());
        }

        @Test
        public void testGetLU2RD_nonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertArrayEquals(new int[] { 1 }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new int[] { 1, 5, 9 }, diag);
        }

        @Test
        public void testMainDiagonalCopy_NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertArrayEquals(new int[] { 1, 4 }, m.mainDiagonalCopy());
        }

        @Test
        public void testmainDiagonalCopy() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diagonal = matrix.mainDiagonalCopy();
            assertArrayEquals(new int[] { 1, 5, 9 }, diagonal);
        }
    }

    @Nested
    class SetMainDiagonalTests {

        @Test
        public void testSetLU2RD() {
            IntMatrix m = matrix.copy();
            m.setMainDiagonal(new int[] { 10, 20, 30 });
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 2));

            // Test non-square matrix
            IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
            nonSquare.setMainDiagonal(new int[] { 9 });
            assertEquals(9, nonSquare.get(0, 0));

            // Test array too short
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new int[] { 1, 2 }));
        }

        @Test
        public void testSetMainDiagonal_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setMainDiagonal(new int[] { 10, 20, 30 });
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 2));
        }

        @Test
        public void testSetMainDiagonal_NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            m.setMainDiagonal(new int[] { 9 });
            assertEquals(9, m.get(0, 0));
        }

        @Test
        public void testSetMainDiagonal_ArrayTooShort() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new int[] { 1, 2 }));
        }

        @Test
        public void testSetMainDiagonal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setMainDiagonal(new int[] { 9, 8 });
            assertArrayEquals(new int[] { 9, 8 }, m.mainDiagonalCopy());
        }

        @Test
        public void testSetDiagonal_NullArraysOnEmptyMatrixThrow() {
            IntMatrix m = IntMatrix.empty();
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(null));
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(null));
        }

        @Test
        public void testSetLU2RD_invalidLength() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new int[] { 1, 2, 3 }));
        }

        @Test
        public void testSetMainDiagonal_InvalidLength() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new int[] { 9 }));
        }
    }

    @Nested
    class UpdateMainDiagonalTests {

        @Test
        public void testUpdateLU2RD() {
            IntMatrix m = matrix.copy();
            m.updateMainDiagonal(x -> x * 10);
            assertEquals(10, m.get(0, 0));
            assertEquals(50, m.get(1, 1));
            assertEquals(90, m.get(2, 2));

            // Test non-square matrix
            IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
            nonSquare.updateMainDiagonal(x -> x * 2);
            assertArrayEquals(new int[] { 2 }, nonSquare.mainDiagonalCopy());
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateMainDiagonal(x -> x * 10);
            assertEquals(10, m.get(0, 0));
            assertEquals(50, m.get(1, 1));
            assertEquals(90, m.get(2, 2));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateMainDiagonal_NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            m.updateMainDiagonal(x -> x * 2);
            assertArrayEquals(new int[] { 2 }, m.mainDiagonalCopy());
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3_ScalesEveryDiagonalElement() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateMainDiagonal(x -> x * 10);
            assertEquals(10, m.get(0, 0));
            assertEquals(50, m.get(1, 1));
            assertEquals(90, m.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateMainDiagonal(x -> x * 2);
            assertArrayEquals(new int[] { 2, 8 }, m.mainDiagonalCopy());
        }

        @Test
        public void testupdateMainDiagonal() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            matrix.updateMainDiagonal(x -> x * x);
            // matrix is now {{1, 2}, {3, 16}}
            assertEquals(1, matrix.get(0, 0));
            assertEquals(2, matrix.get(0, 1));
            assertEquals(3, matrix.get(1, 0));
            assertEquals(16, matrix.get(1, 1));
        }
    }

    @Nested
    class AntiDiagonalCopyTests {

        @Test
        public void testGetRU2LD() {
            int[] antiDiagonal = matrix.antiDiagonalCopy();
            assertArrayEquals(new int[] { 3, 5, 7 }, antiDiagonal);

            // Test non-square matrix
            IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertArrayEquals(new int[] { 2 }, nonSquare.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertArrayEquals(new int[] { 3, 5, 7 }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertArrayEquals(new int[] { 2 }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new int[] { 3, 5, 7 }, diag);
        }

        @Test
        public void testantiDiagonalCopy() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diagonal = matrix.antiDiagonalCopy();
            assertArrayEquals(new int[] { 3, 5, 7 }, diagonal);
        }
    }

    @Nested
    class SetAntiDiagonalTests {

        @Test
        public void testSetRU2LD() {
            IntMatrix m = matrix.copy();
            m.setAntiDiagonal(new int[] { 10, 20, 30 });
            assertEquals(10, m.get(0, 2));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 0));

            // Test non-square matrix
            IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
            nonSquare.setAntiDiagonal(new int[] { 9 });
            assertEquals(9, nonSquare.get(0, 1));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setAntiDiagonal(new int[] { 10, 20, 30 });
            assertEquals(10, m.get(0, 2));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            m.setAntiDiagonal(new int[] { 9 });
            assertEquals(9, m.get(0, 1));
        }

        @Test
        public void testSetAntiDiagonal_ArrayTooShort() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new int[] { 1, 2 }));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3_ReplacesEveryDiagonalElement() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setAntiDiagonal(new int[] { 30, 50, 70 });
            assertEquals(30, m.get(0, 2));
            assertEquals(50, m.get(1, 1));
            assertEquals(70, m.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setAntiDiagonal(new int[] { 9, 8 });
            assertArrayEquals(new int[] { 9, 8 }, m.antiDiagonalCopy());
        }
    }

    @Nested
    class UpdateAntiDiagonalTests {

        @Test
        public void testUpdateRU2LD() {
            IntMatrix m = matrix.copy();
            m.updateAntiDiagonal(x -> x * 10);
            assertEquals(30, m.get(0, 2));
            assertEquals(50, m.get(1, 1));
            assertEquals(70, m.get(2, 0));

            // Test non-square matrix
            IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
            nonSquare.updateAntiDiagonal(x -> x * 2);
            assertArrayEquals(new int[] { 4 }, nonSquare.antiDiagonalCopy());
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateAntiDiagonal(x -> x * 10);
            assertEquals(30, m.get(0, 2));
            assertEquals(50, m.get(1, 1));
            assertEquals(70, m.get(2, 0));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateAntiDiagonal_NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            m.updateAntiDiagonal(x -> x * 2);
            assertArrayEquals(new int[] { 4 }, m.antiDiagonalCopy());
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3_OffsetsEveryDiagonalElement() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateAntiDiagonal(x -> x + 100);
            assertEquals(103, m.get(0, 2));
            assertEquals(105, m.get(1, 1));
            assertEquals(107, m.get(2, 0));
        }

        @Test
        public void testUpdateAntiDiagonal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateAntiDiagonal(x -> x * 2);
            int[] diag = m.antiDiagonalCopy();
            assertEquals(4, diag[0]);
            assertEquals(6, diag[1]);
        }

        @Test
        public void testupdateAntiDiagonal() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            matrix.updateAntiDiagonal(x -> -x);
            // matrix is now {{1, -2}, {-3, 4}}
            assertEquals(1, matrix.get(0, 0));
            assertEquals(-2, matrix.get(0, 1));
            assertEquals(-3, matrix.get(1, 0));
            assertEquals(4, matrix.get(1, 1));
        }
    }

    @Nested
    class UpdateAllTests {

        @Test
        public void testUpdateAll() {
            IntMatrix m = matrix.copy();
            m.updateAll(x -> x * 2);
            assertEquals(2, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
            assertEquals(18, m.get(2, 2));
        }

        @Test
        public void testUpdateAllUnarySequentialTallMatrixUsesRowMajorOrder() throws Exception {
            final IntMatrix m = IntMatrix.wrap(new int[3][2]);
            final AtomicInteger next = new AtomicInteger();

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> m.updateAll(x -> next.incrementAndGet()));

            assertArrayEquals(new int[] { 1, 2 }, m.rowCopy(0));
            assertArrayEquals(new int[] { 3, 4 }, m.rowCopy(1));
            assertArrayEquals(new int[] { 5, 6 }, m.rowCopy(2));
        }

        @Test
        public void testUpdateAll_WithIndices() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 0, 0 }, { 0, 0 } });
            m.updateAll((i, j) -> i * 10 + j);
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
            assertEquals(10, m.get(1, 0));
            assertEquals(11, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll((i, j) -> i * 10 + j);
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
            assertEquals(10, m.get(1, 0));
            assertEquals(11, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_UnaryOperator() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll(x -> x * 2);
            assertEquals(2, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
            assertEquals(6, m.get(1, 0));
            assertEquals(8, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll((i, j) -> (i + 1) * 10 + (j + 1));
            assertEquals(11, m.get(0, 0));
            assertEquals(12, m.get(0, 1));
            assertEquals(21, m.get(1, 0));
            assertEquals(22, m.get(1, 1));
        }

        @Test
        public void testupdateAll() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            matrix.updateAll(x -> x * 2);
            assertEquals(2, matrix.get(0, 0));
            assertEquals(4, matrix.get(0, 1));
            assertEquals(6, matrix.get(1, 0));
            assertEquals(8, matrix.get(1, 1));
        }

        @Test
        public void testUpdateAllByIndex() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 0, 0, 0 }, { 0, 0, 0 } });
            matrix.updateAll((i, j) -> i + j);
            assertEquals(0, matrix.get(0, 0));
            assertEquals(1, matrix.get(0, 1));
            assertEquals(2, matrix.get(0, 2));
            assertEquals(1, matrix.get(1, 0));
            assertEquals(2, matrix.get(1, 1));
            assertEquals(3, matrix.get(1, 2));

            matrix.updateAll((i, j) -> i * 10 + j);
            assertEquals(0, matrix.get(0, 0));
            assertEquals(1, matrix.get(0, 1));
            assertEquals(2, matrix.get(0, 2));
            assertEquals(10, matrix.get(1, 0));
            assertEquals(11, matrix.get(1, 1));
            assertEquals(12, matrix.get(1, 2));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testUpdateAll_ExplicitModesTransformEveryValue(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
                matrix.updateAll(value -> value + 10);
                assertEquals(IntMatrix.wrap(new int[][] { { 11, 12, 13 }, { 14, 15, 16 } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testUpdateAllNullOperator() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix emptyLike = IntMatrix.wrap(new int[][] { {}, {} });

            assertThrows(IllegalArgumentException.class, () -> m.updateAll((Throwables.IntUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> m.updateAll((Throwables.IntBiFunction<Integer, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAll((Throwables.IntUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAll((Throwables.IntBiFunction<Integer, RuntimeException>) null));
        }

        @Test
        public void testUpdateAll_ZeroColumnsDoNotInvokeOperator() {
            final var matrix = IntMatrix.wrap(new int[2][0]);
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
            IntMatrix m = matrix.copy();
            m.replaceIf(x -> x > 5, 0);
            assertEquals(1, m.get(0, 0));
            assertEquals(5, m.get(1, 1));
            assertEquals(0, m.get(2, 2)); // was 9
        }

        @Test
        public void testReplaceIfWithIndices() {
            IntMatrix m = matrix.copy();
            m.replaceIf((i, j) -> i == j, 0); // Replace diagonal
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(1, 1));
            assertEquals(0, m.get(2, 2));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testReplaceIf_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.replaceIf(x -> x > 3, 0);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(0, 2));
            assertEquals(0, m.get(1, 0)); // was 4
            assertEquals(0, m.get(1, 1)); // was 5
            assertEquals(0, m.get(1, 2)); // was 6
        }

        @Test
        public void testReplaceIf_WithIndices() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.replaceIf((i, j) -> i == j, 0); // Replace diagonal
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(1, 1));
            assertEquals(0, m.get(2, 2));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testReplaceIf_Predicate() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.replaceIf(x -> x % 2 == 0, 99);
            assertEquals(1, m.get(0, 0));
            assertEquals(99, m.get(0, 1));
            assertEquals(3, m.get(0, 2));
            assertEquals(99, m.get(1, 0));
        }

        // Verify it's a deep copy
        @Test
        public void testReplaceIf_BiPredicate() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf((i, j) -> i == j, 0);
            assertEquals(0, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(0, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_Predicate_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf(x -> x > 2, 99);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(99, m.get(1, 0));
            assertEquals(99, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_BiPredicate_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf((i, j) -> i == j, 99);
            assertEquals(99, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(99, m.get(1, 1));
        }

        @Test
        public void testreplaceIf() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { -1, 2, -3 }, { 4, -5, 6 } });
            matrix.replaceIf(x -> x < 0, 0);
            assertEquals(0, matrix.get(0, 0));
            assertEquals(2, matrix.get(0, 1));
            assertEquals(0, matrix.get(0, 2));
            assertEquals(4, matrix.get(1, 0));
            assertEquals(0, matrix.get(1, 1));
            assertEquals(6, matrix.get(1, 2));
        }

        @Test
        public void testReplaceIfByIndex() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            matrix.replaceIf((i, j) -> i == j, 0);
            assertEquals(0, matrix.get(0, 0));
            assertEquals(2, matrix.get(0, 1));
            assertEquals(3, matrix.get(0, 2));
            assertEquals(4, matrix.get(1, 0));
            assertEquals(0, matrix.get(1, 1));
            assertEquals(6, matrix.get(1, 2));
            assertEquals(7, matrix.get(2, 0));
            assertEquals(8, matrix.get(2, 1));
            assertEquals(0, matrix.get(2, 2));

            matrix.replaceIf((i, j) -> i == 0 || j == 0, -1);
            assertEquals(-1, matrix.get(0, 0));
            assertEquals(-1, matrix.get(0, 1));
            assertEquals(-1, matrix.get(0, 2));
            assertEquals(-1, matrix.get(1, 0));
            assertEquals(0, matrix.get(1, 1));
            assertEquals(6, matrix.get(1, 2));
            assertEquals(-1, matrix.get(2, 0));
            assertEquals(8, matrix.get(2, 1));
            assertEquals(0, matrix.get(2, 2));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testReplaceIf_ExplicitModesReplaceOnlyMatchingValues(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
                matrix.replaceIf(value -> value % 2 == 0, 9);
                assertEquals(IntMatrix.wrap(new int[][] { { 1, 9, 3 }, { 9, 5, 9 } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testDiagonalAndReplaceIfNullCallbacks() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix emptyLike = IntMatrix.wrap(new int[][] { {}, {} });

            assertThrows(IllegalArgumentException.class, () -> m.updateMainDiagonal((Throwables.IntUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> m.updateAntiDiagonal((Throwables.IntUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> m.replaceIf((Throwables.IntPredicate<RuntimeException>) null, 0));
            assertThrows(IllegalArgumentException.class, () -> m.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0));

            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateMainDiagonal((Throwables.IntUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAntiDiagonal((Throwables.IntUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> m.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0));
        }
    }

    @Nested
    class MapTests {

        @Test
        public void testMap() {
            IntMatrix result = matrix.map(x -> x * 2);
            assertEquals(2, result.get(0, 0));
            assertEquals(4, result.get(0, 1));
            assertEquals(18, result.get(2, 2));

            // Original should be unchanged
            assertEquals(1, matrix.get(0, 0));
        }

        @Test
        public void testRowColumnStatistics() {
            // Test statistics on individual rows
            List<Integer> rowSums = matrix.rowStreams().map(row -> row.sum()).toList();
            assertEquals(3, rowSums.size());
            assertEquals(6, rowSums.get(0).intValue()); // 1+2+3
            assertEquals(15, rowSums.get(1).intValue()); // 4+5+6
            assertEquals(24, rowSums.get(2).intValue()); // 7+8+9

            // Test statistics on individual columns
            List<Integer> colSums = matrix.columnStreams().map(col -> col.sum()).toList();
            assertEquals(3, colSums.size());
            assertEquals(12, colSums.get(0).intValue()); // 1+4+7
            assertEquals(15, colSums.get(1).intValue()); // 2+5+8
            assertEquals(18, colSums.get(2).intValue()); // 3+6+9

            // Test min/max per row
            List<Integer> rowMins = matrix.rowStreams().map(row -> row.min().orElse(0)).toList();
            assertEquals(1, rowMins.get(0).intValue());
            assertEquals(4, rowMins.get(1).intValue());
            assertEquals(7, rowMins.get(2).intValue());

            List<Integer> rowMaxs = matrix.rowStreams().map(row -> row.max().orElse(0)).toList();
            assertEquals(3, rowMaxs.get(0).intValue());
            assertEquals(6, rowMaxs.get(1).intValue());
            assertEquals(9, rowMaxs.get(2).intValue());
        }

        @Test
        public void testScalarOperationsWithMap() {
            // Test scalar addition using map
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            IntMatrix addScalar = m.map(x -> x + 10);
            assertEquals(11, addScalar.get(0, 0));
            assertEquals(12, addScalar.get(0, 1));
            assertEquals(13, addScalar.get(1, 0));
            assertEquals(14, addScalar.get(1, 1));

            // Test scalar subtraction
            IntMatrix subtractScalar = m.map(x -> x - 1);
            assertEquals(0, subtractScalar.get(0, 0));
            assertEquals(1, subtractScalar.get(0, 1));
            assertEquals(2, subtractScalar.get(1, 0));
            assertEquals(3, subtractScalar.get(1, 1));

            // Test scalar multiplication
            IntMatrix multiplyScalar = m.map(x -> x * 3);
            assertEquals(3, multiplyScalar.get(0, 0));
            assertEquals(6, multiplyScalar.get(0, 1));
            assertEquals(9, multiplyScalar.get(1, 0));
            assertEquals(12, multiplyScalar.get(1, 1));

            // Test scalar division
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 10, 20 }, { 30, 40 } });
            IntMatrix divideScalar = m2.map(x -> x / 10);
            assertEquals(1, divideScalar.get(0, 0));
            assertEquals(2, divideScalar.get(0, 1));
            assertEquals(3, divideScalar.get(1, 0));
            assertEquals(4, divideScalar.get(1, 1));
        }

        @Test
        public void testMap_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m.map(x -> x * 10);
            assertEquals(10, result.get(0, 0));
            assertEquals(20, result.get(0, 1));
            assertEquals(30, result.get(1, 0));
            assertEquals(40, result.get(1, 1));
            // Original should be unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testChainedOperations() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m.map(x -> x * 2).transpose().map(x -> x + 1);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(3, result.get(0, 0)); // (1*2)+1
            assertEquals(7, result.get(0, 1)); // (3*2)+1
            assertEquals(5, result.get(1, 0)); // (2*2)+1
            assertEquals(9, result.get(1, 1)); // (4*2)+1
        }

        @Test
        public void testMap_Shape2By2_ScalesAllCellsAndPreservesSource() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m.map(x -> x * 2);
            assertEquals(2, result.get(0, 0));
            assertEquals(4, result.get(0, 1));
            assertEquals(6, result.get(1, 0));
            assertEquals(8, result.get(1, 1));
            // Original should be unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testMapNullMapper() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            assertThrows(IllegalArgumentException.class, () -> matrix.map((Throwables.IntUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.mapToObj((Throwables.IntFunction<String, RuntimeException>) null, String.class));
        }

        @Test
        public void testIntMatrixRowsForZeroColumnMatrix() {
            final IntMatrix matrix = IntMatrix.wrap(new int[][] { {}, {}, {} });
            final List<int[]> rows = matrix.rowStreams().map(IntStream::toArray).toList();

            assertEquals(3, rows.size());
            assertArrayEquals(new int[0], rows.get(0));
            assertArrayEquals(new int[0], rows.get(1));
            assertArrayEquals(new int[0], rows.get(2));
        }

        @Test
        public void testNegativeValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { -1, -2 }, { -3, -4 } });

            assertEquals(-1, m.get(0, 0));
            assertEquals(-4, m.get(1, 1));

            // Test operations with negative values
            IntMatrix doubled = m.map(x -> x * 2);
            assertEquals(-2, doubled.get(0, 0));
            assertEquals(-8, doubled.get(1, 1));
        }
    }

    @Nested
    class MapToLongTests {

        @Test
        public void testMapToLong() {
            LongMatrix result = matrix.mapToLong(x -> (long) x * 1000000);
            assertEquals(1000000L, result.get(0, 0));
            assertEquals(5000000L, result.get(1, 1));
        }

        @Test
        public void testMapToLong_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.mapToLong(x -> (long) x * 1000000);
            assertEquals(1000000L, result.get(0, 0));
            assertEquals(4000000L, result.get(1, 1));
        }

        @Test
        public void testMapToLong_Shape2By2_ScalesFirstRowToLong() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.mapToLong(x -> x * 1000L);
            assertEquals(1000L, result.get(0, 0));
            assertEquals(4000L, result.get(1, 1));
        }

        @Test
        public void testMapToLong_Shape2By2_EveryLongValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.mapToLong(x -> (long) x);
            assertEquals(1L, result.get(0, 0));
            assertEquals(2L, result.get(0, 1));
            assertEquals(3L, result.get(1, 0));
            assertEquals(4L, result.get(1, 1));
        }

        @Test
        public void testMapToLong_Shape2By2_ConvertsEveryCellToLong() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.mapToLong(i -> (long) i);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1L, result.get(0, 0));
            assertEquals(4L, result.get(1, 1));
        }
    }

    @Nested
    class MapToDoubleTests {

        @Test
        public void testMapToDouble() {
            DoubleMatrix result = matrix.mapToDouble(x -> x * 0.1);
            assertEquals(0.1, result.get(0, 0), 0.0001);
            assertEquals(0.5, result.get(1, 1), 0.0001);
        }

        @Test
        public void testMapToDouble_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.mapToDouble(x -> x * 0.1);
            assertEquals(0.1, result.get(0, 0), 0.0001);
            assertEquals(0.4, result.get(1, 1), 0.0001);
        }

        @Test
        public void testMapToDouble_Shape2By2_ScalesFirstRowToDouble() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.mapToDouble(x -> x * 0.5);
            assertEquals(0.5, result.get(0, 0), 0.0001);
            assertEquals(2.0, result.get(1, 1), 0.0001);
        }

        @Test
        public void testMapToDouble_Shape2By2_ConvertsEveryCellToDouble() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.mapToDouble(x -> (double) x);
            assertEquals(1.0, result.get(0, 0), 0.0);
            assertEquals(2.0, result.get(0, 1), 0.0);
            assertEquals(3.0, result.get(1, 0), 0.0);
            assertEquals(4.0, result.get(1, 1), 0.0);
        }

        @Test
        public void testMapToDouble_Shape2By2_ConvertsEveryCellToDouble_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.mapToDouble(i -> (double) i);
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
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> result = m.mapToObj(x -> "val:" + x, String.class);
            assertEquals("val:1", result.get(0, 0));
            assertEquals("val:4", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> result = m.mapToObj(x -> "Value:" + x, String.class);
            assertEquals("Value:1", result.get(0, 0));
            assertEquals("Value:4", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_EveryStringValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
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

            for (final IntMatrix source : List.of(IntMatrix.empty(), new IntMatrix(new int[0][], 2), IntMatrix.wrap(new int[2][0]),
                    IntMatrix.wrap(new int[] { 1 }))) {
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

            final IntMatrix source = IntMatrix.wrap(new int[] { 1 });
            final Matrix<Integer> scalars = source.mapToObj(value -> value, int.class);
            assertSame(Integer.class, scalars.elementType());
            assertEquals(1, scalars.get(0, 0).intValue());

            final int[] arrayValue = { 1, 2 };
            final Matrix<int[]> arrays = source.mapToObj(value -> arrayValue, int[].class);
            assertSame(int[].class, arrays.elementType());
            assertSame(arrayValue, arrays.get(0, 0));
        }
    }

    @Nested
    class FillTests {

        // Modify copy shouldn't affect original
        @Test
        public void testFillWithValue() {
            IntMatrix m = matrix.copy();
            m.fill(99);
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals(99, m.get(i, j));
                }
            }
        }

        @Test
        public void testFill_WithValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.fill(99);
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals(99, m.get(i, j));
                }
            }
        }

        @Test
        public void testFill_Value() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.fill(99);
            assertEquals(99, m.get(0, 0));
            assertEquals(99, m.get(0, 1));
            assertEquals(99, m.get(1, 0));
            assertEquals(99, m.get(1, 1));
        }

        @Test
        public void testFill_Value_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.fill(9);
            assertEquals(9, m.get(0, 0));
            assertEquals(9, m.get(0, 1));
            assertEquals(9, m.get(1, 0));
            assertEquals(9, m.get(1, 1));
        }

        @Test
        public void testFill() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            matrix.fill(5);
            assertEquals(5, matrix.get(0, 0));
            assertEquals(5, matrix.get(0, 1));
            assertEquals(5, matrix.get(1, 0));
            assertEquals(5, matrix.get(1, 1));
        }

        @Test
        public void testIntMatrixFill() {
            IntMatrix matrix = IntMatrix.wrap(new int[2][3]);
            matrix.fill(7);
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    assertEquals(7, matrix.get(i, j));
                }
            }
        }
    }

    @Nested
    class CopyFromTests {

        @Test
        public void testCopyFrom() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            int[][] patch = { { 1, 2 }, { 3, 4 } };
            m.copyFrom(patch);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
            assertEquals(0, m.get(2, 2)); // unchanged
        }

        @Test
        public void testCopyFromAtPosition() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            int[][] patch = { { 1, 2 }, { 3, 4 } };
            m.copyFrom(1, 1, patch);
            assertEquals(0, m.get(0, 0)); // unchanged
            assertEquals(1, m.get(1, 1));
            assertEquals(2, m.get(1, 2));
            assertEquals(3, m.get(2, 1));
            assertEquals(4, m.get(2, 2));
        }

        @Test
        public void testCopyFrom_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.copyFrom(new int[][] { { 10, 20 }, { 30, 40 } });
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(0, 1));
            assertEquals(30, m.get(1, 0));
            assertEquals(40, m.get(1, 1));
        }

        @Test
        public void testCopyFromWithOffset() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.copyFrom(1, 1, new int[][] { { 99, 88 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(99, m.get(1, 1));
            assertEquals(88, m.get(1, 2));
        }

        @Test
        public void testCopyFrom_Shape2By2_EveryCellValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.copyFrom(new int[][] { { 9, 8 }, { 7, 6 } });
            assertEquals(9, m.get(0, 0));
            assertEquals(8, m.get(0, 1));
            assertEquals(7, m.get(1, 0));
            assertEquals(6, m.get(1, 1));
        }

        @Test
        public void testCopyFrom_WithOffset() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.copyFrom(1, 1, new int[][] { { 99 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(99, m.get(1, 1));
            assertEquals(9, m.get(2, 2));
        }

        // Original unchanged
        @Test
        public void testCopyFrom_WithOffset_PartialFill() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            // When source array is larger than destination, only what fits is copied
            m.copyFrom(0, 0, new int[][] { { 9, 8, 7 } });
            assertEquals(9, m.get(0, 0));
            assertEquals(8, m.get(0, 1));
        }

        @Test
        public void testcopyFrom() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 0, 0, 0 }, { 0, 0, 0 } });
            matrix.copyFrom(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(1, matrix.get(0, 0));
            assertEquals(2, matrix.get(0, 1));
            assertEquals(0, matrix.get(0, 2));
            assertEquals(3, matrix.get(1, 0));
            assertEquals(4, matrix.get(1, 1));
            assertEquals(0, matrix.get(1, 2));
        }

        @Test
        public void testcopyFromWithOffset() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            matrix.copyFrom(1, 1, new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(0, matrix.get(0, 0));
            assertEquals(0, matrix.get(0, 1));
            assertEquals(0, matrix.get(0, 2));
            assertEquals(0, matrix.get(1, 0));
            assertEquals(1, matrix.get(1, 1));
            assertEquals(2, matrix.get(1, 2));
            assertEquals(0, matrix.get(2, 0));
            assertEquals(3, matrix.get(2, 1));
            assertEquals(4, matrix.get(2, 2));
        }

        @Test
        public void testCopyFrom_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[][] patch = { { 1, 2 }, { 3, 4 } };
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0", "3, 0", "0, -1", "0, 3" })
        public void testCopyFrom_RejectsOffsetsOutsideDestination(final int row, final int column) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(row, column, new int[][] { { 1 } }));
            assertEquals(IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } }), matrix);
        }
    }

    @Nested
    class CopyTests {

        @Test
        public void testCopy() {
            IntMatrix copy = matrix.copy();
            assertEquals(matrix.rowCount(), copy.rowCount());
            assertEquals(matrix.columnCount(), copy.columnCount());
            assertEquals(matrix.get(0, 0), copy.get(0, 0));

            // Modify copy shouldn't affect original
            copy.set(0, 0, 99);
            assertEquals(1, matrix.get(0, 0));
            assertEquals(99, copy.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals(1, copy.get(0, 0));

            // Modify copy shouldn't affect original
            copy.set(0, 0, 99);
            assertEquals(1, m.get(0, 0));
            assertEquals(99, copy.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2_DimensionsAndIndependentStorage() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix copy = m.copy();
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(1, copy.get(0, 0));

            // Verify it's a deep copy
            copy.set(0, 0, 99);
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testExactIntMultiplicationNarrowsFinishedCellsAndResetsEveryScratchRow() {
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
    }

    @Nested
    class CopyRowsTests {

        @Test
        public void testCopyRows() {
            IntMatrix subset = matrix.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertEquals(4, subset.get(0, 0));
            assertEquals(7, subset.get(1, 0));

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(0, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(2, 1));
        }

        @Test
        public void testCopyRows_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix subset = m.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertEquals(4, subset.get(0, 0));
            assertEquals(9, subset.get(1, 2));
        }

        @Test
        public void testCopyRows_Shape3By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            IntMatrix copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(3, copy.get(0, 0));
            assertEquals(6, copy.get(1, 1));
        }

        // Regression: copy with an empty range in either dimension must not throw.
        @Test
        public void testCopyRowsEmptyRange_ReturnsEmptyMatrix() {
            // Regression: copyRows(from, from) on a matrix with columns > 0 must not throw.
            IntMatrix empty = matrix.copyRows(0, 0);
            assertEquals(0, empty.rowCount());
            assertEquals(3, empty.columnCount());

            IntMatrix emptyAtEnd = matrix.copyRows(matrix.rowCount(), matrix.rowCount());
            assertEquals(0, emptyAtEnd.rowCount());
            assertEquals(3, emptyAtEnd.columnCount());
        }

        @Test
        public void testCopy_InvalidRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 5));
        }

        @Test
        public void testCopyRows_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(2, 1));
        }
    }

    @Nested
    class CopyRegionTests {

        @Test
        public void testCopyRegion() {
            IntMatrix submatrix = matrix.copyRegion(0, 2, 1, 3);
            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals(2, submatrix.get(0, 0));
            assertEquals(3, submatrix.get(0, 1));
            assertEquals(5, submatrix.get(1, 0));
            assertEquals(6, submatrix.get(1, 1));

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRegion(0, 2, 0, 4));
        }

        @Test
        public void testCopyRegion_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix submatrix = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals(2, submatrix.get(0, 0));
            assertEquals(6, submatrix.get(1, 1));
        }

        @Test
        public void testCopyRegion_Shape3By3_UpperRightRegion() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix copy = m.copyRegion(1, 2, 1, 3);
            assertEquals(1, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(5, copy.get(0, 0));
            assertEquals(6, copy.get(0, 1));
        }

        @Test
        public void testCopyRegion_BottomRightSubmatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix copy = m.copyRegion(1, 3, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(5, copy.get(0, 0));
            assertEquals(9, copy.get(1, 1));
        }

        @Test
        public void testCopyRegion_IndependentOfOriginal() {
            IntMatrix src = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix region = src.copyRegion(0, 2, 1, 3);
            // Mutating the region must NOT change the original.
            region.set(0, 0, 999);
            assertEquals(2, src.get(0, 1));
            // Mutating the original must NOT change the region.
            src.set(0, 1, 888);
            assertEquals(999, region.get(0, 0));
        }

        @Test
        public void testCopyRegionEmpty_ReturnsEmptyMatrix() {
            // Regression: copy with an empty range in either dimension must not throw.
            IntMatrix emptyRows = matrix.copyRegion(1, 1, 0, 3);
            assertEquals(0, emptyRows.rowCount());
            assertEquals(3, emptyRows.columnCount());

            IntMatrix emptyCols = matrix.copyRegion(0, 3, 1, 1);
            assertEquals(3, emptyCols.rowCount());
            assertEquals(0, emptyCols.columnCount());

            IntMatrix bothEmpty = matrix.copyRegion(0, 0, 0, 0);
            assertEquals(0, bothEmpty.rowCount());
            assertEquals(0, bothEmpty.columnCount());
        }

        @Test
        public void testCopyRegion_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, 0, 3));
        }
    }

    @Nested
    class ResizeTests {

        @Test
        public void testResize() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix extended = matrix.resize(3, 3);
            assertEquals(1, extended.get(0, 0));
            assertEquals(2, extended.get(0, 1));
            assertEquals(0, extended.get(0, 2));
            assertEquals(3, extended.get(1, 0));
            assertEquals(4, extended.get(1, 1));
            assertEquals(0, extended.get(1, 2));
            assertEquals(0, extended.get(2, 0));
            assertEquals(0, extended.get(2, 1));
            assertEquals(0, extended.get(2, 2));
        }

        @Test
        public void testResizeWithFill() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix extended = matrix.resize(3, 4, 9);
            assertEquals(1, extended.get(0, 0));
            assertEquals(2, extended.get(0, 1));
            assertEquals(9, extended.get(0, 2));
            assertEquals(9, extended.get(0, 3));
            assertEquals(3, extended.get(1, 0));
            assertEquals(4, extended.get(1, 1));
            assertEquals(9, extended.get(2, 0));
            assertEquals(9, extended.get(2, 3));

            IntMatrix truncated = matrix.resize(1, 1, 0);
            assertEquals(1, truncated.rowCount());
            assertEquals(1, truncated.columnCount());
            assertEquals(1, truncated.get(0, 0));
        }

        @Test
        public void testResize_Fixture() {
            IntMatrix extended = matrix.resize(5, 5);
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(0, extended.get(3, 3)); // new cells are 0

            // Test truncation
            IntMatrix truncated = matrix.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
        }

        @Test
        public void testResizeWithDefaultValue() {
            IntMatrix extended = matrix.resize(4, 4, -1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(-1, extended.get(3, 3)); // new cell

            // Test negative dimensions
            assertThrows(IllegalArgumentException.class, () -> matrix.resize(-1, 3, 0));
            assertThrows(IllegalArgumentException.class, () -> matrix.resize(3, -1, 0));
        }

        // Original values at offset position
        @Test
        public void testResize_Larger() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix extended = m.resize(4, 4);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(4, extended.get(1, 1));
            assertEquals(0, extended.get(3, 3)); // new cells are 0
        }

        @Test
        public void testResize_Smaller() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix truncated = m.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
            assertEquals(1, truncated.get(0, 0));
            assertEquals(5, truncated.get(1, 1));
        }

        @Test
        public void testResize_WithDefaultValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix extended = m.resize(3, 3, -1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(-1, extended.get(2, 2)); // new cell
        }

        @Test
        public void testResize_NewSize() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix extended = m.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(0, extended.get(2, 2));
        }

        @Test
        public void testResize_WithDefaultValue_Shape1By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix extended = m.resize(2, 3, 99);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(99, extended.get(0, 2));
            assertEquals(99, extended.get(1, 0));
        }

        @Test
        public void testResize_WithDefaultValue_Shape1By2_CustomPaddingValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix extended = m.resize(2, 3, 99);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(99, extended.get(1, 1));
            assertEquals(99, extended.get(0, 2));
        }

        @Test
        public void testResize_Smaller_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix result = m.resize(1, 2);
            assertEquals(1, result.rowCount());
            assertEquals(2, result.columnCount());
        }

        @Test
        public void testResize_GrowsRowsWhileTrimmingColumns() {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(IntMatrix.wrap(new int[][] { { 1 }, { 3 }, { 9 } }), matrix.resize(3, 1, 9));
            assertEquals(IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } }), matrix);
        }

        @Test
        public void testResize_WithNegativeDimensions() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 3, 0));
            assertThrows(IllegalArgumentException.class, () -> m.resize(3, -1, 0));
        }
    }

    @Nested
    class PadTests {

        // Check original values
        @Test
        public void testPadDirectional() {
            IntMatrix extended = matrix.pad(1, 1, 2, 2);
            assertEquals(5, extended.rowCount()); // 1 + 3 + 1
            assertEquals(7, extended.columnCount()); // 2 + 3 + 2

            // Original values should be at offset position
            assertEquals(1, extended.get(1, 2));
            assertEquals(5, extended.get(2, 3));

            // New cells should be 0
            assertEquals(0, extended.get(0, 0));
        }

        @Test
        public void testPadDirectionalWithDefaultValue() {
            IntMatrix extended = matrix.pad(1, 1, 1, 1, -1);
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());

            // Check original values
            assertEquals(1, extended.get(1, 1));

            // Check new values
            assertEquals(-1, extended.get(0, 0));
            assertEquals(-1, extended.get(4, 4));

            // Test negative values
            assertThrows(IllegalArgumentException.class, () -> matrix.pad(-1, 1, 1, 1, 0));
        }

        @Test
        public void testPad_Directional() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix extended = m.pad(1, 1, 2, 2);
            assertEquals(5, extended.rowCount()); // 1 + 3 + 1
            assertEquals(7, extended.columnCount()); // 2 + 3 + 2

            // Original values at offset position
            assertEquals(1, extended.get(1, 2));
            assertEquals(5, extended.get(2, 3));

            // New cells are 0
            assertEquals(0, extended.get(0, 0));
        }

        @Test
        public void testPad_DirectionalWithDefault() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix extended = m.pad(1, 1, 1, 1, -1);
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());

            // Check original values
            assertEquals(1, extended.get(1, 1));

            // Check new values
            assertEquals(-1, extended.get(0, 0));
            assertEquals(-1, extended.get(4, 4));
        }

        @Test
        public void testPad_Directions() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 5 } });
            IntMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(5, extended.get(1, 1));
            assertEquals(0, extended.get(0, 0));
        }

        @Test
        public void testPad_DirectionsWithDefault() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 5 } });
            IntMatrix extended = m.pad(1, 1, 1, 1, 9);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(5, extended.get(1, 1));
            assertEquals(9, extended.get(0, 0));
            assertEquals(9, extended.get(2, 2));
        }

        @Test
        public void testPad_Directional_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1, extended.get(1, 1));
            assertEquals(0, extended.get(0, 0));
        }

        @Test
        public void testPad_Directional_WithDefaultValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix extended = m.pad(1, 1, 1, 1, 99);
            assertEquals(3, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1, extended.get(1, 1));
            assertEquals(99, extended.get(0, 0));
        }

        @Test
        public void testPad() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix extended = matrix.pad(1, 1, 1, 1);
            assertEquals(3, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(0, extended.get(0, 0));
            assertEquals(0, extended.get(0, 3));
            assertEquals(0, extended.get(1, 0));
            assertEquals(1, extended.get(1, 1));
            assertEquals(2, extended.get(1, 2));
            assertEquals(0, extended.get(1, 3));
            assertEquals(0, extended.get(2, 0));
        }

        @Test
        public void testPadWithFill() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix padded = matrix.pad(1, 1, 2, 2, 9);
            // Result: [[9, 9, 9, 9, 9, 9],
            //          [9, 9, 1, 2, 9, 9],
            //          [9, 9, 9, 9, 9, 9]]
            assertEquals(3, padded.rowCount());
            assertEquals(6, padded.columnCount());
            assertEquals(9, padded.get(0, 0));
            assertEquals(9, padded.get(1, 0));
            assertEquals(9, padded.get(1, 1));
            assertEquals(1, padded.get(1, 2));
            assertEquals(2, padded.get(1, 3));
            assertEquals(9, padded.get(1, 4));
            assertEquals(9, padded.get(2, 5));
        }

        @Test
        public void testPadAsymmetricFill() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix e = m.pad(1, 1, 2, 1, 9);
            assertEquals(3, e.rowCount());
            assertEquals(5, e.columnCount());
            assertArrayEquals(new int[] { 9, 9, 9, 9, 9 }, e.rowCopy(0));
            assertArrayEquals(new int[] { 9, 9, 1, 2, 9 }, e.rowCopy(1));
            assertArrayEquals(new int[] { 9, 9, 9, 9, 9 }, e.rowCopy(2));
        }

        @Test
        public void testPadRepeatFlattenAndForEach_SubRangeEdgeCase() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix extended = matrix.pad(0, 1, 2, 0, 9);
            IntMatrix repeatedElements = matrix.repeatElements(1, 2);
            IntMatrix repeatedMatrix = matrix.repeatMatrix(2, 1);
            IntList flattened = matrix.flatten();
            List<Integer> visited = new ArrayList<>();

            matrix.forEach(0, 2, 1, 2, visited::add);

            assertEquals(9, extended.get(0, 0));
            assertEquals(9, extended.get(2, 1));
            assertArrayEquals(new int[] { 1, 1, 2, 2, 3, 3, 4, 4 }, repeatedElements.flatten().toArray());
            assertArrayEquals(new int[] { 1, 2, 3, 4, 1, 2, 3, 4 }, repeatedMatrix.flatten().toArray());
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, flattened.toArray());
            assertEquals(List.of(2, 4), visited);
        }

        @Test
        public void testPad_EmptyMatrixWithPaddingFillsFully() {
            // Padding an empty matrix should produce a matrix entirely filled with defaultValue.
            IntMatrix empty = IntMatrix.empty();
            IntMatrix padded = empty.pad(1, 1, 1, 1, 9);
            assertEquals(2, padded.rowCount());
            assertEquals(2, padded.columnCount());
            assertArrayEquals(new int[] { 9, 9 }, padded.rowCopy(0));
            assertArrayEquals(new int[] { 9, 9 }, padded.rowCopy(1));
        }

        @Test
        public void testPad_EmptyMatrixColumnOnlyPaddingPreservesZeroByNShape() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(1, empty.pad(0, 0, 1, 0).columnCount());
            assertEquals(2, empty.pad(0, 0, 0, 2, 9).columnCount());
            assertEquals(0, empty.pad(0, 0, 0, 0).rowCount());
        }

        @Test
        public void testPad_SingleAxisFillPreservesExistingValues() {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(IntMatrix.wrap(new int[][] { { 1, 2, 9 }, { 3, 4, 9 } }), matrix.pad(0, 0, 0, 1, 9));
            assertEquals(IntMatrix.wrap(new int[][] { { 9, 1, 2 }, { 9, 3, 4 } }), matrix.pad(0, 0, 1, 0, 9));
            assertEquals(IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 9, 9 } }), matrix.pad(0, 1, 0, 0, 9));
            final var copy = matrix.pad(0, 0, 0, 0, 9);
            assertEquals(matrix, copy);
            assertNotSame(matrix, copy);
        }

        @Test
        public void testPad_DirectionalWithNegativeValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.pad(-1, 1, 1, 1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0, 0, 0", "0, -1, 0, 0", "0, 0, -1, 0", "0, 0, 0, -1", "2147483647, 0, 0, 0", "0, 0, 0, 2147483647" })
        public void testPad_RejectsNegativeOrOverflowingDimensions(final int top, final int bottom, final int left, final int right) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.pad(top, bottom, left, right));
            assertEquals(IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } }), matrix);
        }
    }

    @Nested
    class FlipHorizontallyInPlaceTests {

        @Test
        public void testReverseH() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.flipHorizontallyInPlace();
            assertEquals(3, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(1, m.get(0, 2));
            assertEquals(6, m.get(1, 0));
            assertEquals(5, m.get(1, 1));
            assertEquals(4, m.get(1, 2));
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.flipHorizontallyInPlace();
            assertEquals(3, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(1, m.get(0, 2));
            assertEquals(6, m.get(1, 0));
        }

        @Test
        public void testFlipHorizontallyInPlace() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            matrix.flipHorizontallyInPlace();
            assertArrayEquals(new int[] { 3, 2, 1 }, matrix.rowView(0));
            assertArrayEquals(new int[] { 6, 5, 4 }, matrix.rowView(1));
        }
    }

    @Nested
    class FlipVerticallyInPlaceTests {

        @Test
        public void testReverseV() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertEquals(5, m.get(0, 0));
            assertEquals(6, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
            assertEquals(1, m.get(2, 0));
            assertEquals(2, m.get(2, 1));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape3By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertEquals(5, m.get(0, 0));
            assertEquals(6, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(1, m.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape3By2_ReversesFirstColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertEquals(5, m.get(0, 0));
            assertEquals(6, m.get(0, 1));
            assertEquals(1, m.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertEquals(5, m.get(0, 0));
            assertEquals(3, m.get(1, 0));
            assertEquals(1, m.get(2, 0));
        }

        @Test
        public void testflipVerticallyInPlace() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            matrix.flipVerticallyInPlace();
            assertArrayEquals(new int[] { 5, 6 }, matrix.rowView(0));
            assertArrayEquals(new int[] { 3, 4 }, matrix.rowView(1));
            assertArrayEquals(new int[] { 1, 2 }, matrix.rowView(2));
        }

        @Test
        public void testFlipVerticallyInPlace_OddRowCount() {
            // The middle row should remain untouched; outer rows should be swapped.
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertArrayEquals(new int[] { 5, 6 }, m.rowCopy(0));
            assertArrayEquals(new int[] { 3, 4 }, m.rowCopy(1));
            assertArrayEquals(new int[] { 1, 2 }, m.rowCopy(2));
        }
    }

    @Nested
    class FlipHorizontallyTests {

        @Test
        public void testFlipH() {
            IntMatrix flipped = matrix.flipHorizontally();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(2, flipped.get(0, 1));
            assertEquals(1, flipped.get(0, 2));

            // Original should be unchanged
            assertEquals(1, matrix.get(0, 0));
        }

        @Test
        public void testFlipHorizontally_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix flipped = m.flipHorizontally();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(1, flipped.get(0, 2));
            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testFlipHorizontally() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix flipped = m.flipHorizontally();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(2, flipped.get(0, 1));
            assertEquals(1, flipped.get(0, 2));
            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }
    }

    @Nested
    class FlipVerticallyTests {

        // Test empty matrix
        @Test
        public void testFlipV() {
            IntMatrix flipped = matrix.flipVertically();
            assertEquals(7, flipped.get(0, 0));
            assertEquals(8, flipped.get(0, 1));
            assertEquals(9, flipped.get(0, 2));

            // Original should be unchanged
            assertEquals(1, matrix.get(0, 0));
        }

        @Test
        public void testFlipVertically_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix flipped = m.flipVertically();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(1, flipped.get(1, 0));
            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testFlipVertically() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            IntMatrix flipped = m.flipVertically();
            assertEquals(5, flipped.get(0, 0));
            assertEquals(3, flipped.get(1, 0));
            assertEquals(1, flipped.get(2, 0));
            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }
    }

    @Nested
    class Rotate90Tests {

        @Test
        public void testRotate90() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3, rotated.get(0, 0));
            assertEquals(1, rotated.get(0, 1));
            assertEquals(4, rotated.get(1, 0));
            assertEquals(2, rotated.get(1, 1));
        }

        @Test
        public void testRotate90_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix rotated = m.rotate90();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4, rotated.get(0, 0));
            assertEquals(1, rotated.get(0, 1));
            assertEquals(6, rotated.get(2, 0));
        }

        @Test
        public void testRotate90NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix r = m.rotate90();
            assertEquals(3, r.rowCount());
            assertEquals(2, r.columnCount());
            assertArrayEquals(new int[] { 4, 1 }, r.rowCopy(0));
            assertArrayEquals(new int[] { 5, 2 }, r.rowCopy(1));
            assertArrayEquals(new int[] { 6, 3 }, r.rowCopy(2));
        }
    }

    @Nested
    class Rotate180Tests {

        @Test
        public void testRotate180() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix rotated = m.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4, rotated.get(0, 0));
            assertEquals(3, rotated.get(0, 1));
            assertEquals(2, rotated.get(1, 0));
            assertEquals(1, rotated.get(1, 1));
        }

        @Test
        public void testRotate_CompositionEqualsRotate180() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            // 90 + 90 = 180
            assertEquals(m.rotate180(), m.rotate90().rotate90());
            // 90 + 90 + 90 = 270
            assertEquals(m.rotate270(), m.rotate90().rotate90().rotate90());
            // 90 * 4 = identity
            assertEquals(m, m.rotate90().rotate90().rotate90().rotate90());
        }

        @Test
        public void testRotations_Nx0_PreserveMathematicalShape() {
            IntMatrix m = IntMatrix.wrap(new int[3][0]);

            IntMatrix via180 = m.rotate180();
            assertEquals(3, via180.rowCount());
            assertEquals(0, via180.columnCount());

            IntMatrix viaRotate90Twice = m.rotate90().rotate90();
            assertEquals(3, viaRotate90Twice.rowCount());
            assertEquals(0, viaRotate90Twice.columnCount());
        }
    }

    @Nested
    class Rotate270Tests {

        @Test
        public void testRotate270() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix rotated = m.rotate270();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3, rotated.get(0, 0));
            assertEquals(6, rotated.get(0, 1));
        }

        @Test
        public void testRotate270NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix r = m.rotate270();
            assertArrayEquals(new int[] { 3, 6 }, r.rowCopy(0));
            assertArrayEquals(new int[] { 2, 5 }, r.rowCopy(1));
            assertArrayEquals(new int[] { 1, 4 }, r.rowCopy(2));
        }
    }

    @Nested
    class TransposeTests {

        @Test
        public void testTranspose() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(4, transposed.get(0, 1));
            assertEquals(2, transposed.get(1, 0));
            assertEquals(5, transposed.get(1, 1));
            assertEquals(3, transposed.get(2, 0));
            assertEquals(6, transposed.get(2, 1));
        }

        @Test
        public void testTranspose_Square() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(3, transposed.get(0, 1));
            assertEquals(2, transposed.get(1, 0));
            assertEquals(4, transposed.get(1, 1));
        }

        @Test
        public void testTranspose_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(4, transposed.get(0, 1));
            assertEquals(6, transposed.get(2, 1));
        }

        @Test
        public void testTranspose_InverseProperty() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3, 4 }, { 5, 6, 7, 8 } });
            IntMatrix t = m.transpose();
            assertEquals(4, t.rowCount());
            assertEquals(2, t.columnCount());
            // (i,j) in m -> (j,i) in t
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals(m.get(i, j), t.get(j, i));
                }
            }
            // Transpose-of-transpose returns to original.
            assertEquals(m, t.transpose());
        }

        @Test
        public void testTransposeNonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix t = m.transpose();
            assertArrayEquals(new int[] { 1, 4 }, t.rowCopy(0));
            assertArrayEquals(new int[] { 2, 5 }, t.rowCopy(1));
            assertArrayEquals(new int[] { 3, 6 }, t.rowCopy(2));
        }

        @Test
        public void testTranspose_Nx0_PreservesSwappedShape() {
            IntMatrix t = IntMatrix.wrap(new int[3][0]).transpose();
            assertEquals(0, t.rowCount());
            assertEquals(3, t.columnCount());
            assertEquals(IntMatrix.wrap(new int[3][0]), t.transpose());
        }

        @Test
        public void testSingleRowOperations() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3, 4, 5 } });
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());

            IntMatrix transposed = m.transpose();
            assertEquals(5, transposed.rowCount());
            assertEquals(1, transposed.columnCount());
        }

        @Test
        public void testSingleColumnOperations() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 }, { 2 }, { 3 } });
            assertEquals(3, m.rowCount());
            assertEquals(1, m.columnCount());

            IntMatrix transposed = m.transpose();
            assertEquals(1, transposed.rowCount());
            assertEquals(3, transposed.columnCount());
        }
    }

    @Nested
    class ReshapeAndPadTests {

        @Test
        public void testReshape_ExpandWithDefaults() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            // reshape allows different sizes; extra positions are filled with default values (0)
            IntMatrix reshaped = m.reshapeAndPad(3, 3);
            assertEquals(3, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(3, reshaped.get(0, 2));
            assertEquals(4, reshaped.get(1, 0));
            // Extra positions filled with 0
            assertEquals(0, reshaped.get(1, 1));
            assertEquals(0, reshaped.get(2, 2));
        }

        @Test
        public void testReshape_MultiRowSourceRowMajorOrder() {
            // Reshape exercises the cnt-based branch (a.length > 1).
            IntMatrix src = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix r = src.reshapeAndPad(2, 4);
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, r.rowCopy(0));
            assertArrayEquals(new int[] { 5, 6, 0, 0 }, r.rowCopy(1));
        }

        @Test
        public void testReshape_Empty() {
            IntMatrix empty = IntMatrix.empty();
            IntMatrix reshaped = empty.reshapeAndPad(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }

        @Test
        public void testReshape_SingleRowSourceMultipleStrides() {
            // Reshape a 1xN matrix into multiple smaller rows; covers the a.length == 1 branch.
            IntMatrix src = IntMatrix.wrap(new int[][] { { 1, 2, 3, 4, 5, 6, 7 } });
            IntMatrix r = src.reshapeAndPad(3, 3);
            assertEquals(3, r.rowCount());
            assertEquals(3, r.columnCount());
            assertArrayEquals(new int[] { 1, 2, 3 }, r.rowCopy(0));
            assertArrayEquals(new int[] { 4, 5, 6 }, r.rowCopy(1));
            // Last cell of the source goes into row 2; the extra cells should be zero-padded.
            assertArrayEquals(new int[] { 7, 0, 0 }, r.rowCopy(2));
        }

        @Test
        public void testReshape_OvershootFillsTrailingRowsWithZeros() {
            // newRowCount * newColumnCount > elementCount: trailing rows should be all zeros.
            IntMatrix src = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix r = src.reshapeAndPad(4, 2);
            assertArrayEquals(new int[] { 1, 2 }, r.rowCopy(0));
            assertArrayEquals(new int[] { 3, 4 }, r.rowCopy(1));
            assertArrayEquals(new int[] { 5, 6 }, r.rowCopy(2));
            assertArrayEquals(new int[] { 0, 0 }, r.rowCopy(3));
        }

        @Test
        public void testReshapeWithTrailingZeroFill() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = m.reshapeAndPad(2, 4);
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, reshaped.rowCopy(0));
            assertArrayEquals(new int[] { 5, 6, 0, 0 }, reshaped.rowCopy(1));
        }

        @Test
        public void testReshape_ZeroRowsPreservesLargeColumnDimension() {
            IntMatrix m = IntMatrix.empty().reshapeAndPad(0, 46_341);
            assertEquals(0, m.rowCount());
            assertEquals(46_341, m.columnCount());
        }

        @ParameterizedTest
        @CsvSource({ "-1, 2", "2, -1", "1, 1" })
        public void testReshapeAndPad_RejectsNegativeAndInsufficientShapes(final int rows, final int columns) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.reshapeAndPad(rows, columns));
            assertEquals(IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } }), matrix);
        }
    }

    @Nested
    class RepeatElementsTests {

        @Test
        public void testRepeatElements() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix repeated = m.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals(1, repeated.get(0, 0));
            assertEquals(1, repeated.get(0, 1));
            assertEquals(1, repeated.get(0, 2));
            assertEquals(2, repeated.get(0, 3));
            assertEquals(2, repeated.get(0, 4));
            assertEquals(2, repeated.get(0, 5));
            assertEquals(1, repeated.get(1, 0)); // second row same as first

            // Test invalid arguments
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @Test
        public void testRepeatElements_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m.repeatElements(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(1, result.get(0, 1));
            assertEquals(1, result.get(1, 0));
            assertEquals(4, result.get(3, 3));
        }

        @Test
        public void testRepeatElements_Shape2By2_RepeatedBlockAndAdjacentValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m.repeatElements(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(1, result.get(0, 1));
            assertEquals(1, result.get(1, 0));
            assertEquals(1, result.get(1, 1));
            assertEquals(2, result.get(0, 2));
        }

        @Test
        public void testrepeatElements() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix repeated = matrix.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertArrayEquals(new int[] { 1, 1, 1, 2, 2, 2 }, repeated.rowView(0));
            assertArrayEquals(new int[] { 1, 1, 1, 2, 2, 2 }, repeated.rowView(1));
        }

        @Test
        public void testRepeatElements_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            final var result = matrix.repeatElements(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, 0);
            assertEquals(1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatElements_InvalidArguments() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatElements_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(rows, columns));
        }
    }

    @Nested
    class RepeatMatrixTests {

        @Test
        public void testRepeatMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix repeated = m.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals(1, repeated.get(0, 0));
            assertEquals(2, repeated.get(0, 1));
            assertEquals(1, repeated.get(0, 2)); // repeat starts
            assertEquals(2, repeated.get(0, 3));
            assertEquals(1, repeated.get(0, 4)); // another repeat
            assertEquals(2, repeated.get(0, 5));

            assertEquals(3, repeated.get(1, 0));
            assertEquals(4, repeated.get(1, 1));

            // Check vertical repeat
            assertEquals(1, repeated.get(2, 0)); // vertical repeat starts
            assertEquals(2, repeated.get(2, 1));

            // Test invalid arguments
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @Test
        public void testRepeatMatrix_Shape1By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix result = m.repeatMatrix(2, 2);
            assertEquals(2, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(2, result.get(0, 1));
            assertEquals(1, result.get(0, 2));
            assertEquals(1, result.get(1, 0));
        }

        @Test
        public void testRepeatMatrix_Shape1By2_RepeatsRowInBothDirections() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix result = m.repeatMatrix(2, 2);
            assertEquals(2, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(2, result.get(0, 1));
            assertEquals(1, result.get(0, 2));
            assertEquals(2, result.get(0, 3));
        }

        @Test
        public void testrepeatMatrix() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix repeated = matrix.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertArrayEquals(new int[] { 1, 2, 1, 2, 1, 2 }, repeated.rowView(0));
            assertArrayEquals(new int[] { 3, 4, 3, 4, 3, 4 }, repeated.rowView(1));
            assertArrayEquals(new int[] { 1, 2, 1, 2, 1, 2 }, repeated.rowView(2));
            assertArrayEquals(new int[] { 3, 4, 3, 4, 3, 4 }, repeated.rowView(3));
        }

        @Test
        public void testRepeatMatrix_DegenerateShapesPreserveDimensionsAndValidation() {
            final IntMatrix noRows = new IntMatrix(new int[0][], 2);
            final IntMatrix noColumns = IntMatrix.wrap(new int[][] { {}, {} });

            // Large repeat factors must not cause repeated work when there are no cells to copy.
            // Keep the counts bounded so this shape regression remains safe on older implementations.
            final IntMatrix repeatedRows = noRows.repeatMatrix(100_000, 3);
            assertEquals(0, repeatedRows.rowCount());
            assertEquals(6, repeatedRows.columnCount());

            final IntMatrix repeatedColumns = noColumns.repeatMatrix(3, 100_000);
            assertEquals(6, repeatedColumns.rowCount());
            assertEquals(0, repeatedColumns.columnCount());
            for (int i = 0; i < repeatedColumns.rowCount(); i++) {
                assertEquals(0, repeatedColumns.rowView(i).length);
            }
            assertSame(IntMatrix.empty(), IntMatrix.empty().repeatMatrix(100_000, 100_000));

            assertThrows(IllegalArgumentException.class, () -> noRows.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> noColumns.repeatMatrix(1, -1));
            assertThrows(IllegalArgumentException.class, () -> noRows.repeatMatrix(1, Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> noColumns.repeatMatrix(Integer.MAX_VALUE, 1));
        }

        @Test
        public void testRepeatMatrix_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            final var result = matrix.repeatMatrix(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, 0);
            assertEquals(1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidArguments() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatMatrix_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(rows, columns));
            assertEquals(IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } }), matrix);
        }
    }

    @Nested
    class FlattenTests {

        @Test
        public void testFlatten() {
            IntList flat = matrix.flatten();
            assertEquals(9, flat.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, flat.get(i));
            }
        }

        @Test
        public void testFlatten_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntList flat = m.flatten();
            assertEquals(9, flat.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, flat.get(i));
            }
        }

        @Test
        public void testFlatten_Shape2By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntList flat = m.flatten();
            assertEquals(6, flat.size());
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, flat.toArray());
        }

        @Test
        public void testFlatten_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntList flattened = m.flatten();
            assertEquals(4, flattened.size());
            assertEquals(1, flattened.get(0));
            assertEquals(2, flattened.get(1));
            assertEquals(3, flattened.get(2));
            assertEquals(4, flattened.get(3));
        }

        @Test
        public void testflatten() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntList list = matrix.flatten();
            assertEquals(IntList.of(1, 2, 3, 4), list);
        }

        @Test
        public void testEmptyMatrixOperations() {
            // Test various operations on empty matrix
            assertTrue(emptyMatrix.flatten().isEmpty());
            assertEquals(0, emptyMatrix.copy().rowCount);
            assertEquals(emptyMatrix, emptyMatrix.transpose());
            assertEquals(emptyMatrix, emptyMatrix.rotate90());

            IntMatrix extended = emptyMatrix.resize(2, 2, 5);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals(5, extended.get(0, 0));
        }

        @Test
        public void testFlatten_Empty() {
            IntMatrix empty = IntMatrix.empty();
            IntList flat = empty.flatten();
            assertTrue(flat.isEmpty());
        }
    }

    @Nested
    class MutateViaFlatArrayTests {

        @Test
        public void testFlatOp() {
            List<Integer> sums = new ArrayList<>();
            matrix.mutateViaFlatArray(row -> {
                int sum = 0;
                for (int val : row) {
                    sum += val;
                }
                sums.add(sum);
            });
            assertEquals(1, sums.size());
            assertEquals(45, sums.get(0).intValue());
        }

        @Test
        public void testMutateViaFlatArray_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Integer> sums = new ArrayList<>();
            m.mutateViaFlatArray(row -> {
                int sum = 0;
                for (int val : row) {
                    sum += val;
                }
                sums.add(sum);
            });
            assertEquals(1, sums.size());
            assertEquals(45, sums.get(0).intValue());
        }

        @Test
        public void testMutateViaFlatArrayWithMultipleRows() {
            // Test mutateViaFlatArray to ensure it processes the flattened array correctly
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<Integer> maxValues = new ArrayList<>();

            m.mutateViaFlatArray(flatArray -> {
                int max = Integer.MIN_VALUE;
                for (int val : flatArray) {
                    if (val > max) {
                        max = val;
                    }
                }
                maxValues.add(max);
            });

            // mutateViaFlatArray flattens all rows into one array, so there's only 1 result
            assertEquals(1, maxValues.size());
            assertEquals(6, maxValues.get(0).intValue()); // max of [1,2,3,4,5,6] is 6
        }

        @Test
        public void testMutateViaFlatArray_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            final int[] count = { 0 };
            m.mutateViaFlatArray(row -> count[0] += row.length);
            assertEquals(4, count[0]);
        }

        @Test
        public void testMutateViaFlatArray_Shape2By2_CallbackSumsFlatValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            final int[] sum = { 0 };
            m.mutateViaFlatArray(row -> {
                for (int val : row) {
                    sum[0] += val;
                }
            });
            assertEquals(10, sum[0]);
        }

        @Test
        public void testMutateViaFlatArray() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            AtomicInteger count = new AtomicInteger(0);
            m.mutateViaFlatArray(row -> count.addAndGet(row.length));
            assertEquals(4, count.get());
        }

        @Test
        public void testmutateViaFlatArray() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 5, 3 }, { 4, 1 } });
            matrix.mutateViaFlatArray(arr -> java.util.Arrays.sort(arr));
            assertEquals(1, matrix.get(0, 0));
            assertEquals(3, matrix.get(0, 1));
            assertEquals(4, matrix.get(1, 0));
            assertEquals(5, matrix.get(1, 1));
        }
    }

    @Nested
    class StackVerticallyTests {

        @Test
        public void testVstack() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 7, 8, 9 }, { 10, 11, 12 } });
            IntMatrix stacked = m1.stackVertically(m2);

            assertEquals(4, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(7, stacked.get(2, 0));
            assertEquals(12, stacked.get(3, 2));

            // Test different column counts
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m3));
        }

        @Test
        public void testStackVertically_Shape2By3() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 7, 8, 9 }, { 10, 11, 12 } });
            IntMatrix stacked = m1.stackVertically(m2);

            assertEquals(4, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(7, stacked.get(2, 0));
            assertEquals(12, stacked.get(3, 2));
        }

        @Test
        public void testStackVertically_DifferentColumnCounts() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_Shape2By2() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix stacked = m1.stackVertically(m2);
            assertEquals(4, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(4, stacked.get(1, 1));
            assertEquals(5, stacked.get(2, 0));
            assertEquals(8, stacked.get(3, 1));
        }

        @Test
        public void testStackVertically_DifferentColumnCount() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4, 5 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        // IntMatrix doesn't have mapToFloat method - removed test
        // @Test
        // public void testToFloatMatrix() {
        @Test
        public void testStackVertically_Shape1By2() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 } });
            IntMatrix stacked = m1.stackVertically(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(3, stacked.get(1, 0));
        }

        @Test
        public void testStackVertically() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 } });
            IntMatrix result = m1.stackVertically(m2);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(3, result.get(1, 0));
        }

        @Test
        public void testStackVertically_IncompatibleCols() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void teststackVertically() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 7, 8, 9 }, { 10, 11, 12 } });
            IntMatrix c = a.stackVertically(b);
            assertEquals(4, c.rowCount());
            assertEquals(3, c.columnCount());
            assertArrayEquals(new int[] { 1, 2, 3 }, c.rowView(0));
            assertArrayEquals(new int[] { 4, 5, 6 }, c.rowView(1));
            assertArrayEquals(new int[] { 7, 8, 9 }, c.rowView(2));
            assertArrayEquals(new int[] { 10, 11, 12 }, c.rowView(3));
        }

        @Test
        public void testIntMatrixBinaryOpsRejectNullMatrix() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1 } });
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
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(5, stacked.get(0, 2));
            assertEquals(8, stacked.get(1, 3));

            // Test different row counts
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m3));
        }

        @Test
        public void testStackHorizontally_Shape2By2() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(5, stacked.get(0, 2));
            assertEquals(8, stacked.get(1, 3));
        }

        @Test
        public void testStackHorizontally_DifferentRowCounts() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_Shape2By2_DimensionsAndBothInputs() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(2, stacked.get(0, 1));
            assertEquals(5, stacked.get(0, 2));
            assertEquals(6, stacked.get(0, 3));
        }

        @Test
        public void testStackHorizontally_DifferentRowCount() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_Shape2By1() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1 }, { 3 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 2 }, { 4 } });
            IntMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(2, stacked.get(0, 1));
        }

        @Test
        public void testStackHorizontally_IncompatibleRows() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 2 }, { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1 }, { 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 }, { 4 } });
            IntMatrix result = m1.stackHorizontally(m2);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(3, result.get(0, 1));
        }

        @Test
        public void testStackHorizontally_IncompatibleRows_Shape1By1_Validation() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 }, { 4 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void teststackHorizontally() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 7, 8, 9 }, { 10, 11, 12 } });
            IntMatrix c = a.stackHorizontally(b);
            assertEquals(2, c.rowCount());
            assertEquals(6, c.columnCount());
            assertArrayEquals(new int[] { 1, 2, 3, 7, 8, 9 }, c.rowView(0));
            assertArrayEquals(new int[] { 4, 5, 6, 10, 11, 12 }, c.rowView(1));
        }

        @Test
        public void testStackHorizontally_RejectsLogicalWidthOverflowWithoutAllocation() {
            final var huge = IntMatrix.wrap(new int[0][0]).resize(0, Integer.MAX_VALUE);
            final var extra = IntMatrix.wrap(new int[0][0]).resize(0, 1);
            assertThrows(IllegalArgumentException.class, () -> huge.stackHorizontally(extra));
            assertEquals(Integer.MAX_VALUE, huge.columnCount());
        }
    }

    @Nested
    class AddTests {

        @Test
        public void testAdd() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix sum = m1.add(m2);

            assertEquals(6, sum.get(0, 0));
            assertEquals(8, sum.get(0, 1));
            assertEquals(10, sum.get(1, 0));
            assertEquals(12, sum.get(1, 1));

            // Test different dimensions
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m3));
        }

        // assertThrows(IllegalArgumentException.class, () -> matrix.copyRows(0, 0));
        // assertThrows(IllegalArgumentException.class, () -> matrix.copyRegion(0, 0, 0, 1));
        @Test
        public void testLargeMatrixArithmetic() {
            // Test with larger matrices to ensure performance
            int[][] arr1 = new int[10][10];
            int[][] arr2 = new int[10][10];

            // Fill with test data
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    arr1[i][j] = i * 10 + j + 1;
                    arr2[i][j] = (i * 10 + j + 1) * 2;
                }
            }

            IntMatrix large1 = IntMatrix.wrap(arr1);
            IntMatrix large2 = IntMatrix.wrap(arr2);

            // Test addition
            IntMatrix largeSum = large1.add(large2);
            assertEquals(3, largeSum.get(0, 0)); // 1 + 2 = 3
            assertEquals(300, largeSum.get(9, 9)); // 100 + 200 = 300

            // Test that sum of all elements is correct
            long totalSum = largeSum.rowMajorStream().asLongStream().sum();
            assertEquals(15150, totalSum); // (1+2+...+100) + 2*(1+2+...+100) = 3*(1+2+...+100) = 3*5050 = 15150
        }

        @Test
        public void testAdd_Shape2By2() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix sum = m1.add(m2);

            assertEquals(6, sum.get(0, 0));
            assertEquals(8, sum.get(0, 1));
            assertEquals(10, sum.get(1, 0));
            assertEquals(12, sum.get(1, 1));
        }

        @Test
        public void testAdd_DifferentDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testArithmeticEdgeCases() {
            // Test with zero matrix
            IntMatrix zeros = IntMatrix.wrap(new int[][] { { 0, 0 }, { 0, 0 } });
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            IntMatrix addZero = m.add(zeros);
            assertEquals(m, addZero);

            IntMatrix subtractZero = m.subtract(zeros);
            assertEquals(m, subtractZero);

            // Test multiplication with zero matrix
            IntMatrix multiplyZero = m.matrixMultiply(zeros);
            assertEquals(zeros, multiplyZero);

            // Test addition commutativity
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            assertEquals(m1.add(m2), m2.add(m1));

            // Test subtraction anti-commutativity
            IntMatrix diff1 = m1.subtract(m2);
            IntMatrix diff2 = m2.subtract(m1);
            assertEquals(diff1.get(0, 0), -diff2.get(0, 0));
            assertEquals(diff1.get(1, 1), -diff2.get(1, 1));
        }

        @Test
        public void testAdd_DifferentDimensions_Shape1By2_Validation() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testAdd_Shape2By2_EveryCellSum() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 10, 20 }, { 30, 40 } });
            IntMatrix result = m1.add(m2);
            assertEquals(11, result.get(0, 0));
            assertEquals(22, result.get(0, 1));
            assertEquals(33, result.get(1, 0));
            assertEquals(44, result.get(1, 1));
        }

        @Test
        public void testAdd_AllFourCellValues() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix result = m1.add(m2);
            assertEquals(6, result.get(0, 0));
            assertEquals(8, result.get(0, 1));
            assertEquals(10, result.get(1, 0));
            assertEquals(12, result.get(1, 1));
        }

        @Test
        public void testAdd_IncompatibleDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        // IntMatrix.java: IntMatrix matrix = IntMatrix.from(new char[][] {{'A', 'B'}, {'C', 'D'}});
        // matrix.get(0, 0) returns 65
        @Test
        public void testadd() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix sum = a.add(b);
            assertEquals(6, sum.get(0, 0));
            assertEquals(8, sum.get(0, 1));
            assertEquals(10, sum.get(1, 0));
            assertEquals(12, sum.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testAdd_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
                final var right = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
                assertEquals(IntMatrix.wrap(new int[][] { { 2, 4, 6 }, { 8, 10, 12 } }), left.add(right));
                assertEquals(IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } }), left);
                assertEquals(IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testIntegerOverflow() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { Integer.MAX_VALUE, 1 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, Integer.MAX_VALUE } });

            // Addition overflow
            IntMatrix sum = m1.add(m2);
            // Overflow wraps around to negative
            assertTrue(sum.get(0, 0) < 0);

            // Test max and min values
            IntMatrix extremes = IntMatrix.wrap(new int[][] { { Integer.MAX_VALUE, Integer.MIN_VALUE } });
            assertEquals(Integer.MAX_VALUE, extremes.get(0, 0));
            assertEquals(Integer.MIN_VALUE, extremes.get(0, 1));
        }
    }

    @Nested
    class AddWidenedTests {

        @Test
        public void testExactAndWidenedArithmetic() {
            IntMatrix max = IntMatrix.wrap(new int[][] { { Integer.MAX_VALUE } });
            IntMatrix one = IntMatrix.wrap(new int[][] { { 1 } });
            assertEquals(2_147_483_648L, max.addWidened(one).get(0, 0));
            assertThrows(ArithmeticException.class, () -> max.addExact(one));

            IntMatrix min = IntMatrix.wrap(new int[][] { { Integer.MIN_VALUE } });
            assertEquals(-2_147_483_649L, min.subtractWidened(one).get(0, 0));
            assertThrows(ArithmeticException.class, () -> min.subtractExact(one));
            assertEquals(IntMatrix.wrap(new int[][] { { 2 } }), one.addExact(one));

            IntMatrix fiftyThousand = IntMatrix.wrap(new int[][] { { 50_000 } });
            assertEquals(2_500_000_000L, fiftyThousand.matrixMultiplyWidened(fiftyThousand).get(0, 0));
            assertThrows(ArithmeticException.class, () -> fiftyThousand.matrixMultiplyExact(fiftyThousand));

            IntMatrix threeMax = IntMatrix.wrap(new int[][] { { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE } });
            IntMatrix maxColumn = IntMatrix.wrap(new int[][] { { Integer.MAX_VALUE }, { Integer.MAX_VALUE }, { Integer.MAX_VALUE } });
            assertThrows(ArithmeticException.class, () -> threeMax.matrixMultiplyWidenedExact(maxColumn));
        }
    }

    @Nested
    class SubtractTests {

        @Test
        public void testSubtract() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix diff = m1.subtract(m2);

            assertEquals(4, diff.get(0, 0));
            assertEquals(4, diff.get(0, 1));
            assertEquals(4, diff.get(1, 0));
            assertEquals(4, diff.get(1, 1));

            // Test different dimensions
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m3));
        }

        @Test
        public void testSubtract_Shape2By2() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix diff = m1.subtract(m2);

            assertEquals(4, diff.get(0, 0));
            assertEquals(4, diff.get(0, 1));
            assertEquals(4, diff.get(1, 0));
            assertEquals(4, diff.get(1, 1));
        }

        @Test
        public void testSubtract_DifferentDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testSubtract_DifferentDimensions_Shape1By2_Validation() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testSubtract_Shape2By2_EveryCellDifference() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 10, 20 }, { 30, 40 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m1.subtract(m2);
            assertEquals(9, result.get(0, 0));
            assertEquals(18, result.get(0, 1));
            assertEquals(27, result.get(1, 0));
            assertEquals(36, result.get(1, 1));
        }

        @Test
        public void testSubtract_AllFourCellValues() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m1.subtract(m2);
            assertEquals(4, result.get(0, 0));
            assertEquals(4, result.get(0, 1));
            assertEquals(4, result.get(1, 0));
            assertEquals(4, result.get(1, 1));
        }

        @Test
        public void testSubtract_IncompatibleDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testsubtract() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix diff = a.subtract(b);
            assertEquals(4, diff.get(0, 0));
            assertEquals(4, diff.get(0, 1));
            assertEquals(4, diff.get(1, 0));
            assertEquals(4, diff.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testSubtract_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = IntMatrix.wrap(new int[][] { { 7, 8, 9 }, { 10, 11, 12 } });
                final var right = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
                assertEquals(IntMatrix.wrap(new int[][] { { 6, 6, 6 }, { 6, 6, 6 } }), left.subtract(right));
                assertEquals(IntMatrix.wrap(new int[][] { { 7, 8, 9 }, { 10, 11, 12 } }), left);
                assertEquals(IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }
    }

    @Nested
    class SubtractExactTests {

        @Test
        public void testSubtractExact_ValidRectangularOperandsPreserveValues() {
            final var left = IntMatrix.wrap(new int[][] { { 7, 8, 9 }, { 10, 11, 12 } });
            final var right = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(IntMatrix.wrap(new int[][] { { 6, 6, 6 }, { 6, 6, 6 } }), left.subtractExact(right));
            assertEquals(IntMatrix.wrap(new int[][] { { 7, 8, 9 }, { 10, 11, 12 } }), left);
        }
    }

    @Nested
    class MatrixMultiplyTests {

        @Test
        public void testMultiply() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix product = m1.matrixMultiply(m2);

            assertEquals(19, product.get(0, 0)); // 1*5 + 2*7
            assertEquals(22, product.get(0, 1)); // 1*6 + 2*8
            assertEquals(43, product.get(1, 0)); // 3*5 + 4*7
            assertEquals(50, product.get(1, 1)); // 3*6 + 4*8

            // Test incompatible dimensions
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m3));
        }

        @Test
        public void testMultiply_IncompatibleDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testMatrixMultiply_RectangularMatrices() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } }); // 1x3
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 4 }, { 5 }, { 6 } }); // 3x1
            IntMatrix product = m1.matrixMultiply(m2);

            assertEquals(1, product.rowCount());
            assertEquals(1, product.columnCount());
            assertEquals(32, product.get(0, 0)); // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        }

        @Test
        public void testMatrixMultiply_Shape2By2() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 2, 3 }, { 4, 5 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m1.matrixMultiply(m2);
            assertEquals(11, result.get(0, 0));
            assertEquals(16, result.get(0, 1));
            assertEquals(19, result.get(1, 0));
            assertEquals(28, result.get(1, 1));
        }

        @Test
        public void testMatrixMultiply_Shape2By2_EveryCellProduct() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 10, 20 }, { 30, 40 } });
            IntMatrix result = m1.matrixMultiply(m2);
            assertEquals(70, result.get(0, 0));
            assertEquals(100, result.get(0, 1));
            assertEquals(150, result.get(1, 0));
            assertEquals(220, result.get(1, 1));
        }

        @Test
        public void testMatrixMultiply() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 2, 0 }, { 1, 2 } });
            IntMatrix result = m1.matrixMultiply(m2);
            assertEquals(4, result.get(0, 0));
            assertEquals(4, result.get(0, 1));
            assertEquals(10, result.get(1, 0));
            assertEquals(8, result.get(1, 1));
        }

        @Test
        public void testMatrixMultiply_IncompatibleDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testmatrixMultiply() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix product = a.matrixMultiply(b);
            assertEquals(19, product.get(0, 0));
            assertEquals(22, product.get(0, 1));
            assertEquals(43, product.get(1, 0));
            assertEquals(50, product.get(1, 1));
        }

        @Test
        public void testMatrixMultiply_MathematicalCorrectness() {
            // Identity multiplication.
            IntMatrix m = IntMatrix.wrap(new int[][] { { 2, 3 }, { 4, 5 } });
            IntMatrix id = IntMatrix.wrap(new int[][] { { 1, 0 }, { 0, 1 } });
            assertEquals(m, m.matrixMultiply(id));
            assertEquals(m, id.matrixMultiply(m));

            // Non-square shapes: 2x3 * 3x4 -> 2x4
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 1, 0, 1, 1 }, { 0, 1, 1, 1 }, { 1, 1, 0, 1 } });
            IntMatrix product = a.matrixMultiply(b);
            assertEquals(2, product.rowCount());
            assertEquals(4, product.columnCount());
            // row 0 = [1*1+2*0+3*1, 1*0+2*1+3*1, 1*1+2*1+3*0, 1*1+2*1+3*1] = [4, 5, 3, 6]
            assertArrayEquals(new int[] { 4, 5, 3, 6 }, product.rowCopy(0));
            // row 1 = [4+0+6, 0+5+6, 4+5+0, 4+5+6] = [10, 11, 9, 15]
            assertArrayEquals(new int[] { 10, 11, 9, 15 }, product.rowCopy(1));
        }

        @Test
        public void testMatrixMultiplyProduct() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix p = a.matrixMultiply(b);
            assertArrayEquals(new int[] { 19, 22 }, p.rowCopy(0));
            assertArrayEquals(new int[] { 43, 50 }, p.rowCopy(1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testMatrixMultiply_ExplicitModesComputeRectangularProduct(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
                final var right = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
                assertEquals(IntMatrix.wrap(new int[][] { { 9, 12, 15 }, { 19, 26, 33 }, { 29, 40, 51 } }), left.matrixMultiply(right));
                assertEquals(IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } }), left);
                assertEquals(IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testMatrixMultiply_EmptyProductPreservesShape() {
            IntMatrix product = IntMatrix.empty().matrixMultiply(IntMatrix.empty());
            assertTrue(product.isEmpty());
            assertEquals(0, product.columnCount());
        }

        @Test
        public void testMatrixMultiplyWithOverflow() {
            // Test multiplication that causes overflow
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { Integer.MAX_VALUE / 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 } });
            IntMatrix product = m1.matrixMultiply(m2);

            // Result will overflow - verify it wraps around
            assertTrue(product.get(0, 0) != (Integer.MAX_VALUE / 2) * 3L);
        }

        @Test
        public void testMatrixMultiply_IncompatibleShapesThrows() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            // a is 2x2 and b is 1x3; columnCount(a)=2 != rowCount(b)=1
            assertThrows(IllegalArgumentException.class, () -> a.matrixMultiply(b));
        }

        @Test
        public void testMatrixMultiplyIncompatibleShapesRejected() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2, 3 } }); // 1x3
            IntMatrix b = IntMatrix.wrap(new int[][] { { 1, 2 } }); // 1x2
            assertThrows(IllegalArgumentException.class, () -> a.matrixMultiply(b));
        }
    }

    @Nested
    class MatrixMultiplyWidenedTests {

        @Test
        public void testMatrixMultiplyWidened_TwoMinValueTerms_WrapsLong() {
            // Documented: every single int*int product fits in a long, but two terms already overflow it.
            final IntMatrix mins = IntMatrix.wrap(new int[][] { { Integer.MIN_VALUE, Integer.MIN_VALUE } });
            final IntMatrix minsColumn = mins.transpose();

            assertEquals(Long.MIN_VALUE, mins.matrixMultiplyWidened(minsColumn).get(0, 0));
            assertThrows(ArithmeticException.class, () -> mins.matrixMultiplyWidenedExact(minsColumn));
            assertThrows(ArithmeticException.class, () -> mins.matrixMultiplyExact(minsColumn));

            // A single extreme product is representable in long (2^62) but not in int.
            final IntMatrix min = IntMatrix.wrap(new int[][] { { Integer.MIN_VALUE } });
            assertEquals(1L << 62, min.matrixMultiplyWidened(min).get(0, 0));
            assertEquals(1L << 62, min.matrixMultiplyWidenedExact(min).get(0, 0));
            assertThrows(ArithmeticException.class, () -> min.matrixMultiplyExact(min));
        }

        @Test
        public void testMatrixMultiplyWidened_RejectsIncompatibleInnerDimensions() {
            final var left = IntMatrix.wrap(new int[][] { { 1, 2 } });
            final var right = IntMatrix.wrap(new int[][] { { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> left.matrixMultiplyWidened(right));
        }
    }

    @Nested
    class MatrixMultiplyWidenedExactTests {

        @Test
        public void testExactIntMultiplicationMatchesTheCheckedWidenedProductForRandomRectangularShapes() {
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
        public void testExactIntMultiplicationPreservesLongOverflowPrecedenceAcrossRows() {
            final IntMatrix left = IntMatrix.wrap(new int[][] { { Integer.MAX_VALUE, 0, 0 }, { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE } });
            final IntMatrix right = IntMatrix.wrap(new int[][] { { Integer.MIN_VALUE }, { Integer.MIN_VALUE }, { Integer.MIN_VALUE } });
            final ArithmeticException expected = assertThrows(ArithmeticException.class, () -> left.matrixMultiplyWidenedExact(right));
            final ArithmeticException actual = assertThrows(ArithmeticException.class, () -> left.matrixMultiplyExact(right));
            assertEquals(expected.getMessage(), actual.getMessage());
        }

        @Test
        public void testMatrixMultiplyWidenedExact_RejectsIncompatibleInnerDimensions() {
            final var left = IntMatrix.wrap(new int[][] { { 1, 2 } });
            final var right = IntMatrix.wrap(new int[][] { { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> left.matrixMultiplyWidenedExact(right));
        }
    }

    @Nested
    class MatrixMultiplyExactTests {

        @Test
        public void testMatrixMultiplyExact_CancellingIntermediateSumsAccepted() {
            // Documented example: the partial sum 4000000000 exceeds int, but the finished cell fits.
            final IntMatrix row = IntMatrix.wrap(new int[][] { { 2_000_000_000, 2_000_000_000, -2_000_000_000 } });
            final IntMatrix ones = IntMatrix.wrap(new int[][] { { 1 }, { 1 }, { 1 } });

            assertEquals(IntMatrix.wrap(new int[][] { { 2_000_000_000 } }), row.matrixMultiplyExact(ones));
            assertEquals(row.matrixMultiply(ones), row.matrixMultiplyExact(ones));
        }

        @Test
        public void testExactIntMultiplicationPreservesAllEmptyProductShapes() {
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
        public void testExactIntMultiplicationStillRejectsIntermediateLongAndFinishedIntOverflow() {
            final IntMatrix longOverflow = IntMatrix.wrap(new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE });
            assertThrows(ArithmeticException.class, () -> longOverflow.matrixMultiplyExact(longOverflow.transpose()));
            assertThrows(ArithmeticException.class, () -> IntMatrix.wrap(new int[] { 50_000 }).matrixMultiplyExact(IntMatrix.wrap(new int[] { 50_000 })));
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.empty().matrixMultiplyExact(null));
            assertThrows(IllegalArgumentException.class, () -> new IntMatrix(new int[0][], 2).matrixMultiplyExact(new IntMatrix(new int[3][0])));
        }

        @Test
        public void testMatrixMultiplyExact_RejectsFinalIntOverflowAfterValidLongSum() {
            final var left = IntMatrix.wrap(new int[][] { { Integer.MAX_VALUE, 1 } });
            final var right = IntMatrix.wrap(new int[][] { { 1 }, { 1 } });
            assertEquals(2147483648L, left.matrixMultiplyWidenedExact(right).get(0, 0));
            assertThrows(ArithmeticException.class, () -> left.matrixMultiplyExact(right));
        }
    }

    @Nested
    class BoxedTests {

        @Test
        public void testBoxed() {
            Matrix<Integer> boxed = matrix.boxed();
            assertEquals(3, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Integer.valueOf(1), boxed.get(0, 0));
            assertEquals(Integer.valueOf(5), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Matrix<Integer> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Integer.valueOf(1), boxed.get(0, 0));
            assertEquals(Integer.valueOf(6), boxed.get(1, 2));
        }

        @Test
        public void testBoxed_WithMoreRowsThanCols() {
            // Test the rows > columnCount branch in boxed()
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 }, { 7, 8 } });
            Matrix<Integer> boxed = m.boxed();
            assertEquals(4, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Integer.valueOf(1), boxed.get(0, 0));
            assertEquals(Integer.valueOf(8), boxed.get(3, 1));
        }

        // Test forEach on empty matrix
        @Test
        public void testBoxed_WithEqualRowsAndCols() {
            // Test the rows <= columnCount branch where rows == columnCount
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            Matrix<Integer> boxed = m.boxed();
            assertEquals(3, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Integer.valueOf(5), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> boxed = m.boxed();
            assertEquals(Integer.valueOf(1), boxed.get(0, 0));
            assertEquals(Integer.valueOf(4), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By2_DimensionsAndBoxedCornerValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Integer.valueOf(1), boxed.get(0, 0));
            assertEquals(Integer.valueOf(4), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_AllFourCellValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(1, boxed.get(0, 0));
            assertEquals(4, boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Empty() {
            // Test boxed() with empty matrix
            IntMatrix empty = IntMatrix.empty();
            Matrix<Integer> boxed = empty.boxed();
            assertEquals(0, boxed.rowCount());
            assertEquals(0, boxed.columnCount());
        }
    }

    @Nested
    class ToLongMatrixTests {

        @Test
        public void testToLongMatrix() {
            LongMatrix longMatrix = matrix.toLongMatrix();
            assertEquals(3, longMatrix.rowCount());
            assertEquals(3, longMatrix.columnCount());
            assertEquals(1L, longMatrix.get(0, 0));
            assertEquals(5L, longMatrix.get(1, 1));
        }

        @Test
        public void testToLongMatrix_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix longMatrix = m.toLongMatrix();
            assertEquals(2, longMatrix.rowCount());
            assertEquals(2, longMatrix.columnCount());
            assertEquals(1L, longMatrix.get(0, 0));
            assertEquals(4L, longMatrix.get(1, 1));
        }

        @Test
        public void testToLongMatrix_Shape2By2_FirstRowConvertedValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.toLongMatrix();
            assertEquals(1L, result.get(0, 0));
            assertEquals(4L, result.get(1, 1));
        }

        @Test
        public void testToLongMatrix_AllFourCellValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.toLongMatrix();
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1L, result.get(0, 0));
            assertEquals(4L, result.get(1, 1));
        }

        @Test
        public void testIntegerBoundaryValues() {
            // Test with Integer.MAX_VALUE and Integer.MIN_VALUE
            IntMatrix m = IntMatrix.wrap(new int[][] { { Integer.MAX_VALUE, Integer.MIN_VALUE }, { 0, -1 } });

            assertEquals(Integer.MAX_VALUE, m.get(0, 0));
            assertEquals(Integer.MIN_VALUE, m.get(0, 1));

            // Test conversion to long to avoid overflow
            LongMatrix longM = m.toLongMatrix();
            assertEquals(Integer.MAX_VALUE, longM.get(0, 0));
            assertEquals(Integer.MIN_VALUE, longM.get(0, 1));
        }
    }

    @Nested
    class ToFloatMatrixTests {

        @Test
        public void testToFloatMatrix() {
            FloatMatrix floatMatrix = matrix.toFloatMatrix();
            assertEquals(3, floatMatrix.rowCount());
            assertEquals(3, floatMatrix.columnCount());
            assertEquals(1.0f, floatMatrix.get(0, 0), 0.0001f);
            assertEquals(5.0f, floatMatrix.get(1, 1), 0.0001f);
        }

        @Test
        public void testToFloatMatrix_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            FloatMatrix floatMatrix = m.toFloatMatrix();
            assertEquals(2, floatMatrix.rowCount());
            assertEquals(2, floatMatrix.columnCount());
            assertEquals(1.0f, floatMatrix.get(0, 0), 0.0001f);
            assertEquals(4.0f, floatMatrix.get(1, 1), 0.0001f);
        }

        @Test
        public void testToFloatMatrix_Shape2By2_FirstRowConvertedValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            FloatMatrix result = m.toFloatMatrix();
            assertEquals(1.0f, result.get(0, 0), 0.001f);
            assertEquals(4.0f, result.get(1, 1), 0.001f);
        }

        @Test
        public void testToFloatMatrix_Shape2By2_DimensionsAndConvertedValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            FloatMatrix result = m.toFloatMatrix();
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0f, result.get(0, 0), 0.0f);
            assertEquals(4.0f, result.get(1, 1), 0.0f);
        }
    }

    @Nested
    class ToDoubleMatrixTests {

        @Test
        public void testToDoubleMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix doubleMatrix = m.toDoubleMatrix();
            assertEquals(2, doubleMatrix.rowCount());
            assertEquals(2, doubleMatrix.columnCount());
            assertEquals(1.0, doubleMatrix.get(0, 0), 0.0001);
            assertEquals(4.0, doubleMatrix.get(1, 1), 0.0001);
        }

        @Test
        public void testToDoubleMatrix_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.toDoubleMatrix();
            assertEquals(1.0, result.get(0, 0), 0.001);
            assertEquals(4.0, result.get(1, 1), 0.001);
        }

        @Test
        public void testToDoubleMatrix_AllFourCellValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.toDoubleMatrix();
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0, result.get(0, 0), 0.0);
            assertEquals(4.0, result.get(1, 1), 0.0);
        }
    }

    @Nested
    class ZipWithTests {

        // Test non-square
        @Test
        public void testZipWith() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix result = m1.zipWith(m2, (a, b) -> a * b);

            assertEquals(5, result.get(0, 0)); // 1*5
            assertEquals(12, result.get(0, 1)); // 2*6
            assertEquals(21, result.get(1, 0)); // 3*7
            assertEquals(32, result.get(1, 1)); // 4*8

            // Test different shapes
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m3, (a, b) -> a + b));
        }

        @Test
        public void testElementWiseMultiplyWithZipWith() {
            // Test element-wise multiplication using zipWith (since there's no multiply scalar method)
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 2, 3 }, { 4, 5 } });

            // Element-wise multiplication
            IntMatrix elementWiseProduct = m1.zipWith(m2, (a, b) -> a * b);
            assertEquals(2, elementWiseProduct.get(0, 0)); // 1*2
            assertEquals(6, elementWiseProduct.get(0, 1)); // 2*3
            assertEquals(12, elementWiseProduct.get(1, 0)); // 3*4
            assertEquals(20, elementWiseProduct.get(1, 1)); // 4*5

            // Element-wise division
            IntMatrix elementWiseDivision = m2.zipWith(m1, (a, b) -> a / b);
            assertEquals(2, elementWiseDivision.get(0, 0)); // 2/1
            assertEquals(1, elementWiseDivision.get(0, 1)); // 3/2 (integer division)
            assertEquals(1, elementWiseDivision.get(1, 0)); // 4/3 (integer division)
            assertEquals(1, elementWiseDivision.get(1, 1)); // 5/4 (integer division)
        }

        @Test
        public void testZipWith_DifferentShapes() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a + b));
        }

        @Test
        public void testZipWith_ThreeMatrices_DifferentShapes() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 9, 10, 11 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> a + b + c));
        }

        @Test
        public void testZipWith_TwoMatrices() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix result = m1.zipWith(m2, (a, b) -> a + b);
            assertEquals(6, result.get(0, 0));
            assertEquals(8, result.get(0, 1));
            assertEquals(10, result.get(1, 0));
            assertEquals(12, result.get(1, 1));
        }

        @Test
        public void testZipWith_TwoMatrices_DifferentDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a + b));
        }

        @Test
        public void testZipWith_ThreeMatrices() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 9, 10 }, { 11, 12 } });
            IntMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(15, result.get(0, 0)); // 1 + 5 + 9
            assertEquals(18, result.get(0, 1)); // 2 + 6 + 10
            assertEquals(21, result.get(1, 0)); // 3 + 7 + 11
            assertEquals(24, result.get(1, 1)); // 4 + 8 + 12
        }

        // Note: averageByInt(), averageByLong(), averageByDouble() methods don't exist in IntMatrix
        @Test
        public void testZipWith_ThreeMatrices_DifferentDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> a + b + c));
        }

        @Test
        public void testZipWith_TwoMatrices_Shape2By2() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 10, 20 }, { 30, 40 } });
            IntMatrix result = m1.zipWith(m2, (a, b) -> a + b);
            assertEquals(11, result.get(0, 0));
            assertEquals(22, result.get(0, 1));
            assertEquals(33, result.get(1, 0));
            assertEquals(44, result.get(1, 1));
        }

        @Test
        public void testZipWith_ThreeMatrices_Shape1By2() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 10, 20 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 100, 200 } });
            IntMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(111, result.get(0, 0));
            assertEquals(222, result.get(0, 1));
        }

        @Test
        public void testZipWith_Ternary() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 5, 6 } });
            IntMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(9, result.get(0, 0));
            assertEquals(12, result.get(0, 1));
        }
    }

    @Nested
    class MainDiagonalStreamTests {

        @Test
        public void testStreamLU2RD() {
            int[] diagonal = matrix.mainDiagonalStream().toArray();
            assertArrayEquals(new int[] { 1, 5, 9 }, diagonal);

            // Test empty matrix
            assertTrue(emptyMatrix.mainDiagonalStream().toArray().length == 0);

            // Test non-square
            IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertArrayEquals(new int[] { 1 }, nonSquare.mainDiagonalStream().toArray());
        }

        @Test
        public void testMainDiagonalStream_Shape3By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diagonal = m.mainDiagonalStream().toArray();
            assertArrayEquals(new int[] { 1, 5, 9 }, diagonal);
        }

        @Test
        public void testMainDiagonalStream_NonSquare() {
            IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertArrayEquals(new int[] { 1 }, nonSquare.mainDiagonalStream().toArray());
        }

        @Test
        public void testMainDiagonalStream_NonSquare_Shape1By2_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertArrayEquals(new int[] { 1 }, m.mainDiagonalStream().toArray());
        }

        @Test
        public void testMainDiagonalStream() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long sum = m.mainDiagonalStream().sum();
            assertEquals(5L, sum);
        }

        @Test
        public void testMainDiagonalStream_IteratesEachDiagonalElementOnce() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 11, 12, 13 }, { 21, 22, 23 }, { 31, 32, 33 } });
            int[] diag = m.mainDiagonalStream().toArray();
            assertArrayEquals(new int[] { 11, 22, 33 }, diag);
        }

        @Test
        public void testMainDiagonalStreamValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertArrayEquals(new int[] { 1, 5, 9 }, m.mainDiagonalStream().toArray());
        }

        @Test
        public void testAllCustomIteratorCountsConsumeTheirIterators() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            com.landawn.abacus.util.stream.IntIteratorEx diagonal = (com.landawn.abacus.util.stream.IntIteratorEx) matrix.mainDiagonalStream().iterator();
            assertEquals(2L, diagonal.count());
            assertFalse(diagonal.hasNext());

            com.landawn.abacus.util.stream.ObjIteratorEx<IntStream> rows = (com.landawn.abacus.util.stream.ObjIteratorEx<IntStream>) matrix.rowStreams()
                    .iterator();
            assertEquals(2L, rows.count());
            assertFalse(rows.hasNext());

            com.landawn.abacus.util.stream.ObjIteratorEx<IntStream> columns = (com.landawn.abacus.util.stream.ObjIteratorEx<IntStream>) matrix.columnStreams()
                    .iterator();
            com.landawn.abacus.util.stream.IntIteratorEx firstColumn = (com.landawn.abacus.util.stream.IntIteratorEx) columns.next().iterator();
            assertEquals(2L, firstColumn.count());
            assertFalse(firstColumn.hasNext());
            assertEquals(1L, columns.count());
            assertFalse(columns.hasNext());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testMainDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final int[] expected = new int[] { 1, 5, 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (IntIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextInt);
            }
        }

        @Test
        public void testMainDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (IntIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1, iterator.nextInt());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextInt);
            }
        }

        @Test
        public void testMainDiagonalStream_Empty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().toArray().length);
        }

        @Test
        public void testMainDiagonalStreamEmptyMatrix() {
            assertEquals(0, IntMatrix.empty().mainDiagonalStream().count());
        }

        @Test
        public void testDiagonalStream_Nx0MatrixIsEmpty() {
            IntMatrix m = IntMatrix.wrap(new int[3][0]);
            assertEquals(3, m.rowCount());
            assertEquals(0, m.columnCount());
            assertEquals(0, m.mainDiagonalStream().count());
            assertEquals(0, m.antiDiagonalStream().count());

            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().count());
            assertEquals(0, empty.antiDiagonalStream().count());
        }
    }

    @Nested
    class AntiDiagonalStreamTests {

        // Test bounds
        @Test
        public void testStreamRU2LD() {
            int[] antiDiagonal = matrix.antiDiagonalStream().toArray();
            assertArrayEquals(new int[] { 3, 5, 7 }, antiDiagonal);

            // Test empty matrix
            assertTrue(emptyMatrix.antiDiagonalStream().toArray().length == 0);

            // Test non-square
            IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertArrayEquals(new int[] { 2 }, nonSquare.antiDiagonalStream().toArray());
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] antiDiagonal = m.antiDiagonalStream().toArray();
            assertArrayEquals(new int[] { 3, 5, 7 }, antiDiagonal);
        }

        @Test
        public void testAntiDiagonalStream_NonSquare() {
            IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertArrayEquals(new int[] { 2 }, nonSquare.antiDiagonalStream().toArray());
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_ArrayValues_AntiDiagonalArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diagonal = m.antiDiagonalStream().toArray();
            assertArrayEquals(new int[] { 3, 5, 7 }, diagonal);
        }

        @Test
        public void testAntiDiagonalStream_NonSquare_Shape1By2_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertArrayEquals(new int[] { 2 }, m.antiDiagonalStream().toArray());
        }

        @Test
        public void testAntiDiagonalStream() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long sum = m.antiDiagonalStream().sum();
            assertEquals(5L, sum);
        }

        @Test
        public void testAntiDiagonalStream_IteratesEachAntiDiagonalElementOnce() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 11, 12, 13 }, { 21, 22, 23 }, { 31, 32, 33 } });
            int[] antiDiag = m.antiDiagonalStream().toArray();
            assertArrayEquals(new int[] { 13, 22, 31 }, antiDiag);
        }

        @Test
        public void testAntiDiagonalStreamValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertArrayEquals(new int[] { 3, 5, 7 }, m.antiDiagonalStream().toArray());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testAntiDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final int[] expected = new int[] { 3, 5, 7 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (IntIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextInt);
            }
        }

        @Test
        public void testAntiDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (IntIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(3, iterator.nextInt());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextInt);
            }
        }

        @Test
        public void testAntiDiagonalStream_Empty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.antiDiagonalStream().toArray().length);
        }

        @Test
        public void testAntiDiagonalStreamEmptyMatrix() {
            assertEquals(0, IntMatrix.empty().antiDiagonalStream().count());
        }
    }

    @Nested
    class RowMajorStreamTests {

        @Test
        public void testRowMajorStream() {
            int[] all = matrix.rowMajorStream().toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, all);

            // Test empty matrix
            assertTrue(emptyMatrix.rowMajorStream().toArray().length == 0);
        }

        @Test
        public void testRowMajorStreamRange() {
            int[] rows = matrix.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new int[] { 4, 5, 6, 7, 8, 9 }, rows);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(0, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(2, 1));
        }

        @Test
        public void testIteratorNoSuchElement() {
            // Test rowMajorStream iterator
            IntStream stream = matrix.rowMajorStream(0, 1);
            stream.toArray(); // Consume all
            assertThrows(IllegalStateException.class, () -> stream.iterator().next());

            // Test mainDiagonalStream iterator
            IntStream diagStream = matrix.mainDiagonalStream();
            diagStream.toArray(); // Consume all
            assertThrows(IllegalStateException.class, () -> diagStream.iterator().next());
        }

        @Test
        public void testStatisticalOperations() {
            // Test sum operation on streams
            int totalSum = matrix.rowMajorStream().sum();
            assertEquals(45, totalSum); // 1+2+3+4+5+6+7+8+9 = 45

            // Test sum of specific row
            int row1Sum = matrix.rowMajorStream(1, 2).sum();
            assertEquals(15, row1Sum); // 4+5+6 = 15

            // Test sum of specific column
            int col0Sum = matrix.columnMajorStream(0, 1).sum();
            assertEquals(12, col0Sum); // 1+4+7 = 12

            // Test min/max on streams
            int min = matrix.rowMajorStream().min().orElse(0);
            assertEquals(1, min);

            int max = matrix.rowMajorStream().max().orElse(0);
            assertEquals(9, max);

            // Test average
            double avg = matrix.rowMajorStream().average().orElse(0.0);
            assertEquals(5.0, avg, 0.0001);

            // Test statistical operations on diagonal
            int diagonalSum = matrix.mainDiagonalStream().sum();
            assertEquals(15, diagonalSum); // 1+5+9 = 15

            int antiDiagonalSum = matrix.antiDiagonalStream().sum();
            assertEquals(15, antiDiagonalSum); // 3+5+7 = 15
        }

        @Test
        public void testRowMajorStream_Shape2By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] all = m.rowMajorStream().toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, all);
        }

        @Test
        public void testStatisticalOperations_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            // Test sum
            int totalSum = m.rowMajorStream().sum();
            assertEquals(45, totalSum); // 1+2+3+4+5+6+7+8+9 = 45

            // Test sum of specific row
            int row1Sum = m.rowMajorStream(1, 2).sum();
            assertEquals(15, row1Sum); // 4+5+6 = 15

            // Test sum of specific column
            int col0Sum = m.columnMajorStream(0, 1).sum();
            assertEquals(12, col0Sum); // 1+4+7 = 12

            // Test min/max
            int min = m.rowMajorStream().min().orElse(0);
            assertEquals(1, min);

            int max = m.rowMajorStream().max().orElse(0);
            assertEquals(9, max);

            // Test average
            double avg = m.rowMajorStream().average().orElse(0.0);
            assertEquals(5.0, avg, 0.0001);

            // Test diagonal operations
            int diagonalSum = m.mainDiagonalStream().sum();
            assertEquals(15, diagonalSum); // 1+5+9 = 15

            int antiDiagonalSum = m.antiDiagonalStream().sum();
            assertEquals(15, antiDiagonalSum); // 3+5+7 = 15
        }

        @Test
        public void testRowMajorStream_RowRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            int[] rows = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new int[] { 3, 4, 5, 6 }, rows);
        }

        // Resulting matrix:
        @Test
        public void testHorizontalStream() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long sum = m.rowMajorStream().sum();
            assertEquals(10L, sum);
        }

        @Test
        public void testRowMajorStream_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            long sum = m.rowMajorStream(1, 3).sum();
            assertEquals(18L, sum);
        }

        @Test
        public void testHorizontalStreamSum() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int sum = matrix.rowMajorStream().sum();
            assertEquals(10, sum);
            int[] array = matrix.rowMajorStream().toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, array);
        }

        @Test
        public void testHorizontalStreamRange() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            int[] subset = matrix.rowMajorStream(0, 2).toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, subset);
        }

        @Test
        public void testRowMajorStreamorizontalIteratorAdvanceAndExhaustion_EdgeCase() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            var iterator = matrix.rowMajorStream(0, 2).iterator();

            assertTrue(iterator instanceof com.landawn.abacus.util.stream.IntIteratorEx);

            com.landawn.abacus.util.stream.IntIteratorEx ex = (com.landawn.abacus.util.stream.IntIteratorEx) iterator;
            ex.advance(2);
            assertEquals(2L, ex.count());
            assertEquals(0L, ex.count());
            assertThrows(java.util.NoSuchElementException.class, ex::nextInt);
        }

        @Test
        public void testHorizontalAndVerticalStream_TraverseAllElementsExactlyOnce() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            // Horizontal: row-major order; element count must equal elementCount.
            int[] h = m.rowMajorStream().toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, h);
            // Vertical: column-major order.
            int[] v = m.columnMajorStream().toArray();
            assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 6 }, v);
        }

        @Test
        public void testStreamIterators_MidStrideAdvanceThenToArray_EdgeCase() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            var rowMajorIterator = matrix.rowMajorStream(0, 2).iterator();
            assertTrue(rowMajorIterator instanceof com.landawn.abacus.util.stream.IntIteratorEx);
            com.landawn.abacus.util.stream.IntIteratorEx rowMajor = (com.landawn.abacus.util.stream.IntIteratorEx) rowMajorIterator;
            rowMajor.advance(1); // mid-row cursor: the chunked toArray must start at column 1
            assertArrayEquals(new int[] { 2, 3, 4, 5, 6 }, rowMajor.toArray());

            com.landawn.abacus.util.stream.IntIteratorEx rowCount = (com.landawn.abacus.util.stream.IntIteratorEx) matrix.rowMajorStream(0, 2).iterator();
            rowCount.advance(1);
            assertEquals(5L, rowCount.count());
            assertFalse(rowCount.hasNext());

            var columnMajorIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.IntIteratorEx columnMajor = (com.landawn.abacus.util.stream.IntIteratorEx) columnMajorIterator;
            columnMajor.advance(1); // mid-column cursor: row 1 of column 0
            assertArrayEquals(new int[] { 4, 2, 5, 3, 6 }, columnMajor.toArray());

            com.landawn.abacus.util.stream.IntIteratorEx columnCount = (com.landawn.abacus.util.stream.IntIteratorEx) matrix.columnMajorStream(0, 3).iterator();
            columnCount.advance(1);
            assertEquals(5L, columnCount.count());
            assertFalse(columnCount.hasNext());

            var crossingIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.IntIteratorEx crossing = (com.landawn.abacus.util.stream.IntIteratorEx) crossingIterator;
            crossing.advance(3); // crosses a column boundary: lands on row 1 of column 1
            assertEquals(5, crossing.nextInt());
            assertEquals(2L, crossing.count());
            assertEquals(0L, crossing.count());
            assertThrows(java.util.NoSuchElementException.class, crossing::nextInt);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testRowMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final int[] expected = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (IntIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextInt);
            }
        }

        @Test
        public void testRowMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (IntIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1, iterator.nextInt());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextInt);
            }
        }

        @Test
        public void testRowMajorStreamSingleRowRange() {
            int[] row1 = matrix.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new int[] { 4, 5, 6 }, row1);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(3, 4));
        }

        @Test
        public void testRowMajorStream_Empty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.rowMajorStream().toArray().length);
        }

        @Test
        public void testRowMajorStream_SingleRowRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] row1 = m.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new int[] { 4, 5, 6 }, row1);
        }

        @Test
        public void testRowMajorStream_BySingleRowRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long sum = m.rowMajorStream(0, 1).sum();
            assertEquals(3L, sum);
        }

        @Test
        public void testHorizontalStreamSingleRowRange() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int rowSum = matrix.rowMajorStream(1, 2).sum();
            assertEquals(15, rowSum);
            int[] firstRow = matrix.rowMajorStream(0, 1).toArray();
            assertArrayEquals(new int[] { 1, 2, 3 }, firstRow);
        }

        @Test
        public void testRowMajorStream_ZeroColumnsKeepLogicalRowRange() {
            final var matrix = IntMatrix.wrap(new int[3][0]);
            assertEquals(0, matrix.rowMajorStream(1, 3).count());
            assertEquals(2, matrix.rowStreams(1, 3).count());
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 3));
        }

        @Test
        public void testRowMajorStream_WithRange_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 1));
        }

        @Test
        public void testRowMajorStream_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 2));
        }
    }

    @Nested
    class ColumnMajorStreamTests {

        @Test
        public void testColumnMajorStream() {
            int[] all = matrix.columnMajorStream().toArray();
            assertArrayEquals(new int[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 }, all);

            // Test empty matrix
            assertTrue(emptyMatrix.columnMajorStream().toArray().length == 0);
        }

        @Test
        public void testColumnMajorStreamRange() {
            int[] columnCount = matrix.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new int[] { 2, 5, 8, 3, 6, 9 }, columnCount);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(0, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(2, 1));
        }

        @Test
        public void testColumnMajorStream_Shape2By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] all = m.columnMajorStream().toArray();
            assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 6 }, all);
        }

        @Test
        public void testColumnMajorStream_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] columnCount = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new int[] { 2, 5, 8, 3, 6, 9 }, columnCount);
        }

        @Test
        public void testColumnMajorStream_ColumnRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] columnCount = m.columnMajorStream(0, 2).toArray();
            assertArrayEquals(new int[] { 1, 4, 2, 5 }, columnCount);
        }

        @Test
        public void testVerticalStream() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long sum = m.columnMajorStream().sum();
            assertEquals(10L, sum);
        }

        @Test
        public void testColumnMajorStream_WithRange_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            long sum = m.columnMajorStream(1, 3).sum();
            assertEquals(16L, sum);
        }

        @Test
        public void testverticalStream() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[] colMajor = matrix.columnMajorStream().toArray();
            assertArrayEquals(new int[] { 1, 3, 2, 4 }, colMajor);
        }

        @Test
        public void testVerticalStreamRange() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] subset = matrix.columnMajorStream(0, 2).toArray();
            assertArrayEquals(new int[] { 1, 4, 2, 5 }, subset);
        }

        @Test
        public void testVerticalStreamColumnMajor() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 6 }, m.columnMajorStream().toArray());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testColumnMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final int[] expected = new int[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (IntIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextInt);
            }
        }

        @Test
        public void testColumnMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (IntIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1, iterator.nextInt());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextInt);
            }
        }

        @Test
        public void testColumnMajorStreamSingleColumnRange() {
            int[] col1 = matrix.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new int[] { 2, 5, 8 }, col1);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(3, 4));
        }

        @Test
        public void testColumnMajorStream_Empty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.columnMajorStream().toArray().length);
        }

        @Test
        public void testColumnMajorStream_SingleColumnRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] col1 = m.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new int[] { 2, 5 }, col1);
        }

        @Test
        public void testColumnMajorStream_BySingleColumnRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long sum = m.columnMajorStream(0, 1).sum();
            assertEquals(4L, sum);
        }

        @Test
        public void testVerticalStreamSingleColumnRange() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int colSum = matrix.columnMajorStream(0, 1).sum();
            assertEquals(5, colSum);
            int[] secondCol = matrix.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new int[] { 2, 5 }, secondCol);
        }

        @Test
        public void testColumnMajorStream_ZeroRowsKeepLogicalColumnRange() {
            final var matrix = IntMatrix.wrap(new int[0][0]).resize(0, 3);
            assertEquals(0, matrix.columnMajorStream(1, 3).count());
            assertEquals(2, matrix.columnStreams(1, 3).count());
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 3));
        }

        @Test
        public void testColumnMajorStream_WithRange_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 1));
        }

        @Test
        public void testColumnMajorStream_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
        }
    }

    @Nested
    class RowStreamsTests {

        @Test
        public void testStreamR() {
            List<int[]> rows = matrix.rowStreams().map(IntStream::toArray).toList();
            assertEquals(3, rows.size());
            assertArrayEquals(new int[] { 1, 2, 3 }, rows.get(0));
            assertArrayEquals(new int[] { 4, 5, 6 }, rows.get(1));
            assertArrayEquals(new int[] { 7, 8, 9 }, rows.get(2));

            // Test empty matrix
            assertTrue(emptyMatrix.rowStreams().count() == 0);
        }

        @Test
        public void testRowStreamsRange() {
            List<int[]> rows = matrix.rowStreams(1, 3).map(IntStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new int[] { 4, 5, 6 }, rows.get(0));
            assertArrayEquals(new int[] { 7, 8, 9 }, rows.get(1));

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(0, 4));
        }

        @Test
        public void testRowStreams_Shape2By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<int[]> rows = m.rowStreams().map(IntStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new int[] { 1, 2, 3 }, rows.get(0));
            assertArrayEquals(new int[] { 4, 5, 6 }, rows.get(1));
        }

        @Test
        public void testRowStreams_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<int[]> rows = m.rowStreams(1, 3).map(IntStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new int[] { 4, 5, 6 }, rows.get(0));
            assertArrayEquals(new int[] { 7, 8, 9 }, rows.get(1));
        }

        @Test
        public void testRowStreams_Shape2By2_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<int[]> rows = m.rowStreams().map(IntStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new int[] { 1, 2 }, rows.get(0));
            assertArrayEquals(new int[] { 3, 4 }, rows.get(1));
        }

        @Test
        public void testRowStreams() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long count = m.rowStreams().count();
            assertEquals(2, count);
        }

        @Test
        public void testRowStreams_WithRange_Shape3By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            long count = m.rowStreams(1, 3).count();
            assertEquals(2, count);
        }

        @Test
        public void testRowStreamsowSums() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] rowSums = matrix.rowStreams().mapToInt(row -> row.sum()).toArray();
            assertArrayEquals(new int[] { 6, 15, 24 }, rowSums);
        }

        @Test
        public void testStreamsObserveLiveValuesAndRowStreamsKeepTheirSelectedBackingRow() {
            final IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
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
                    assertArrayEquals(new int[] { 9, 2 }, selectedRow.toArray());
                    assertArrayEquals(new int[] { 3, 9 }, selectedColumn.toArray());
                    assertArrayEquals(new int[] { 3, 4, 9, 2 }, rowMajor.toArray());
                    assertArrayEquals(new int[] { 3, 9, 4, 2 }, columnMajor.toArray());
                    assertArrayEquals(new int[] { 3, 2 }, mainDiagonal.toArray());
                    assertArrayEquals(new int[] { 4, 9 }, antiDiagonal.toArray());
                }
                try (var nextRow = rows.next();
                     var nextColumn = columns.next()) {
                    assertArrayEquals(new int[] { 9, 2 }, nextRow.toArray());
                    assertArrayEquals(new int[] { 4, 2 }, nextColumn.toArray());
                }
            }
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testRowStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final int[][] expected = { new int[] { 1, 2, 3 }, new int[] { 4, 5, 6 }, new int[] { 7, 8, 9 } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowStreams()) {
                final var iterator = (ObjIteratorEx<IntStream>) stream.iterator();
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
        public void testRowStreamsWithZeroColumnRows() {
            IntMatrix zeroColumnMatrix = IntMatrix.wrap(new int[][] { {}, {}, {} });

            assertEquals(3, zeroColumnMatrix.rowCount());
            assertEquals(0, zeroColumnMatrix.columnCount());
            assertTrue(zeroColumnMatrix.isEmpty());

            List<int[]> rows = zeroColumnMatrix.rowStreams().map(IntStream::toArray).toList();
            assertEquals(3, rows.size());
            assertArrayEquals(new int[0], rows.get(0));
            assertArrayEquals(new int[0], rows.get(1));
            assertArrayEquals(new int[0], rows.get(2));
        }

        @Test
        public void testRowStreams_Empty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.rowStreams().count());
        }

        @Test
        public void testRowStreams_WithRange_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(0, 3));
        }
    }

    @Nested
    class ColumnStreamsTests {

        @Test
        public void testStreamC() {
            List<int[]> columnCount = matrix.columnStreams().map(IntStream::toArray).toList();
            assertEquals(3, columnCount.size());
            assertArrayEquals(new int[] { 1, 4, 7 }, columnCount.get(0));
            assertArrayEquals(new int[] { 2, 5, 8 }, columnCount.get(1));
            assertArrayEquals(new int[] { 3, 6, 9 }, columnCount.get(2));

            // Test empty matrix
            assertTrue(emptyMatrix.columnStreams().count() == 0);
        }

        @Test
        public void testColumnStreamsRange() {
            List<int[]> columnCount = matrix.columnStreams(1, 3).map(IntStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new int[] { 2, 5, 8 }, columnCount.get(0));
            assertArrayEquals(new int[] { 3, 6, 9 }, columnCount.get(1));

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(0, 4));
        }

        @Test
        public void testColumnStreams_Shape2By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<int[]> columnCount = m.columnStreams().map(IntStream::toArray).toList();
            assertEquals(3, columnCount.size());
            assertArrayEquals(new int[] { 1, 4 }, columnCount.get(0));
            assertArrayEquals(new int[] { 2, 5 }, columnCount.get(1));
            assertArrayEquals(new int[] { 3, 6 }, columnCount.get(2));
        }

        @Test
        public void testColumnStreams_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<int[]> columnCount = m.columnStreams(1, 3).map(IntStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new int[] { 2, 5, 8 }, columnCount.get(0));
            assertArrayEquals(new int[] { 3, 6, 9 }, columnCount.get(1));
        }

        @Test
        public void testColumnStreams_Shape2By2_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<int[]> columnCount = m.columnStreams().map(IntStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new int[] { 1, 3 }, columnCount.get(0));
            assertArrayEquals(new int[] { 2, 4 }, columnCount.get(1));
        }

        @Test
        public void testColumnStreams() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long count = m.columnStreams().count();
            assertEquals(2, count);
        }

        @Test
        public void testColumnStreams_WithRange_Shape1By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            long count = m.columnStreams(1, 3).count();
            assertEquals(2, count);
        }

        @Test
        public void testColumnStreamsolumnSums() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] colSums = matrix.columnStreams().mapToInt(col -> col.sum()).toArray();
            assertArrayEquals(new int[] { 5, 7, 9 }, colSums);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testColumnStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final int[][] expected = { new int[] { 1, 4, 7 }, new int[] { 2, 5, 8 }, new int[] { 3, 6, 9 } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnStreams()) {
                final var iterator = (ObjIteratorEx<IntStream>) stream.iterator();
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
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final int[] expected = new int[] { 2, 5, 8 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (IntIteratorEx) values.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextInt);
            }
        }

        @Test
        public void testColumnStreams_InnerCountConsumesOnlyRemainingValues() {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (IntIteratorEx) values.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(2, iterator.nextInt());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextInt);
            }
        }

        @Test
        public void testColumnStreams_Empty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.columnStreams().count());
        }

        @Test
        public void testZeroByNShapeFlowsThroughResultsAndColumnStreams() {
            IntMatrix zeroByThree = IntMatrix.random(0, 3, new java.util.Random(1L));
            assertEquals(3, zeroByThree.copy().columnCount());
            assertEquals(3, zeroByThree.copyRows(0, 0).columnCount());
            assertEquals(2, zeroByThree.copyRegion(0, 0, 1, 3).columnCount());
            assertEquals(5, zeroByThree.resize(0, 5).columnCount());
            assertEquals(6, zeroByThree.pad(0, 0, 1, 2).columnCount());
            assertEquals(3, zeroByThree.map(v -> v).columnCount());
            assertEquals(3, zeroByThree.boxed().columnCount());
            assertEquals(3, zeroByThree.toLongMatrix().columnCount());
            assertEquals(6, zeroByThree.repeatElements(2, 2).columnCount());

            java.util.List<IntStream> columns = zeroByThree.columnStreams().toList();
            assertEquals(3, columns.size());
            columns.forEach(column -> assertEquals(0L, column.count()));

            IntMatrix zeroProduct = IntMatrix.random(0, 2).matrixMultiply(IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } }));
            assertEquals(0, zeroProduct.rowCount());
            assertEquals(3, zeroProduct.columnCount());
            assertEquals(3, IntMatrix.random(0, 2).matrixMultiplyWidened(IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } })).columnCount());
        }

        @Test
        public void testColumnStreams_WithRange_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(0, 3));
        }
    }

    @Nested
    class ForEachTests {

        @Test
        public void testForEach() {
            List<Integer> values = new ArrayList<>();
            matrix.forEach(it -> values.add(it));
            assertEquals(9, values.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, values.get(i).intValue());
            }
        }

        @Test
        public void testForEachWithBounds() {
            List<Integer> values = new ArrayList<>();
            matrix.forEach(1, 3, 1, 3, it -> values.add(it));
            assertEquals(4, values.size());
            assertEquals(5, values.get(0).intValue());
            assertEquals(6, values.get(1).intValue());
            assertEquals(8, values.get(2).intValue());
            assertEquals(9, values.get(3).intValue());

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.forEach(-1, 2, 0, 2, v -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.forEach(0, 4, 0, 2, v -> {
            }));
        }

        @Test
        public void testForEach_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Integer> values = new ArrayList<>();
            m.forEach(v -> values.add(v));
            assertEquals(9, values.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, values.get(i).intValue());
            }
        }

        @Test
        public void testForEach_WithBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Integer> values = new ArrayList<>();
            m.forEach(1, 3, 1, 3, v -> values.add(v));
            assertEquals(4, values.size());
            assertEquals(5, values.get(0).intValue());
            assertEquals(6, values.get(1).intValue());
            assertEquals(8, values.get(2).intValue());
            assertEquals(9, values.get(3).intValue());
        }

        @Test
        public void testForEach_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEach(x -> count.incrementAndGet());
            assertEquals(4, count.get());
        }

        @Test
        public void testForEach_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEach(1, 3, 1, 3, x -> count.incrementAndGet());
            assertEquals(4, count.get());
        }

        @Test
        public void testIntMatrixForEach() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Integer> values = new ArrayList<>();
            matrix.forEach(value -> values.add(value));
            assertEquals(4, values.size());
            assertEquals(1, values.get(0));
            assertEquals(2, values.get(1));
            assertEquals(3, values.get(2));
            assertEquals(4, values.get(3));
        }

        @Test
        public void testForEachRange_SequentialRowMajor_AndParallelCompleteness() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            final ParallelMode prev = Matrices.getParallelMode();
            try {
                Matrices.setParallelMode(ParallelMode.FORCE_OFF);
                final List<Integer> seq = new ArrayList<>();
                m.forEach(0, 3, 0, 2, v -> seq.add(v));
                assertEquals(List.of(1, 2, 3, 4, 5, 6), seq);

                // Parallel: order is unspecified, but every element is visited exactly once.
                Matrices.setParallelMode(ParallelMode.FORCE_ON);
                final AtomicInteger count = new AtomicInteger();
                final java.util.concurrent.atomic.AtomicLong sum = new java.util.concurrent.atomic.AtomicLong();
                m.forEach(0, 3, 0, 2, v -> {
                    count.incrementAndGet();
                    sum.addAndGet(v);
                });
                assertEquals(6, count.get());
                assertEquals(21L, sum.get());
            } finally {
                Matrices.setParallelMode(prev);
            }
        }

        @Test
        public void testForEachStopsAtTheFailingElementInSequentialMode() {
            final IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            final IllegalStateException failure = new IllegalStateException("stop at the second element");
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                for (final boolean selectedRegion : new boolean[] { false, true }) {
                    final List<Integer> visited = new java.util.ArrayList<>();
                    final Throwables.IntConsumer<RuntimeException> action = value -> {
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
                    assertEquals(selectedRegion ? List.of(2, 3) : List.of(1, 2), visited);
                }
            });
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testForEach_ExplicitModesVisitSelectedRegionExactlyOnce(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
                final var values = new ConcurrentLinkedQueue<Integer>();
                matrix.forEach(0, 2, 1, 3, values::add);
                assertEquals(List.of(2, 3, 5, 6).stream().sorted().toList(), values.stream().sorted().toList());
                assertEquals(IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testForEachNullAction() {
            assertThrows(IllegalArgumentException.class, () -> matrix.forEach((Throwables.IntConsumer<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class,
                    () -> matrix.forEach(0, matrix.rowCount(), 0, matrix.columnCount(), (Throwables.IntConsumer<RuntimeException>) null));
        }

        @Test
        public void testForEachWithEmptyMatrix() {
            // Test forEach on empty matrix
            IntMatrix empty = IntMatrix.empty();
            List<Integer> values = new ArrayList<>();
            empty.forEach(v -> values.add(v));
            assertTrue(values.isEmpty());
        }

        @Test
        public void testForEachWithSingleElement() {
            // Test forEach with single element matrix
            IntMatrix single = IntMatrix.wrap(new int[][] { { 42 } });
            List<Integer> values = new ArrayList<>();
            single.forEach(v -> values.add(v));
            assertEquals(1, values.size());
            assertEquals(42, values.get(0).intValue());
        }

        @Test
        public void testForEach_WithBounds_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(-1, 2, 0, 2, v -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 3, 0, 2, v -> {
            }));
        }
    }

    @Nested
    class ToMultilineStringTests {

        @Test
        public void testToMultilineString_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String result = m.toMultilineString();
            assertNotNull(result);
            assertTrue(result.length() > 0);
        }

        @Test
        public void testToMultilineStringWithExtremeIntValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { Integer.MIN_VALUE, 0, Integer.MAX_VALUE } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains(String.valueOf(Integer.MIN_VALUE)));
            assertTrue(printed.contains(String.valueOf(Integer.MAX_VALUE)));
        }

        @Test
        public void testToMultilineStringNonEmptyReturnsNonNullFormattedString() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains("1"));
            assertTrue(printed.contains("4"));
            assertFalse(printed.equals("null"));
        }

        @Test
        public void testToMultilineStringEmptyReturnsBracketsLiteral() {
            IntMatrix empty = IntMatrix.empty();
            String printed = empty.toMultilineString();
            assertEquals("[]", printed);
        }
    }

    @Nested
    class HashCodeTests {

        // Test diagonal operations
        @Test
        public void testHashCode() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 4, 3 } });

            assertEquals(m1.hashCode(), m2.hashCode());
            assertNotEquals(m1.hashCode(), m3.hashCode()); // Usually different
        }

        @Test
        public void testHashCode_Equal() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            assertEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        public void testHashCode_ZeroRowWidthsDistinguishShapes() {
            final var empty = IntMatrix.wrap(new int[0][0]);
            final var narrow = empty.resize(0, 4);
            final var equal = empty.resize(0, 4);
            final var wide = empty.resize(0, 5);
            assertEquals(narrow, equal);
            assertEquals(narrow.hashCode(), equal.hashCode());
            assertNotEquals(narrow.hashCode(), wide.hashCode());
            assertNotEquals(narrow.hashCode(), empty.hashCode());
        }
    }

    @Nested
    class EqualsTests {

        @Test
        public void testEquals() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 4, 3 } });
            IntMatrix m4 = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            assertTrue(m1.equals(m1)); // Same object
            assertTrue(m1.equals(m2)); // Same values
            assertFalse(m1.equals(m3)); // Different values
            assertFalse(m1.equals(m4)); // Different dimensions
            assertFalse(m1.equals(null));
            assertFalse(m1.equals("not a matrix"));
        }

        @Test
        public void testEquals_Shape2By2() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 5 } });

            assertEquals(m1, m2);
            assertNotEquals(m1, m3);
        }

        @Test
        public void testEquals_Same() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(m1, m2);
        }

        @Test
        public void testEquals_Different() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 5 } });
            assertNotEquals(m1, m2);
        }

        // Mutating the original must NOT change the region.
        @Test
        public void testEqualsHashCode_ConsistencyAndShape() {
            IntMatrix a1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix a2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            assertEquals(a1, a2);
            assertEquals(a1.hashCode(), a2.hashCode());
            assertNotEquals(a1, b);

            // Different content yields not-equal.
            IntMatrix differentValue = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 99 } });
            assertNotEquals(a1, differentValue);

            // Self-equality, null, other type.
            assertTrue(a1.equals(a1));
            assertNotEquals(a1, null);
            assertNotEquals(a1, "not a matrix");
        }

        @Test
        public void testEquals_DifferentRowCountsWithEqualColumnCounts() {
            final var longer = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            final var shorter = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertFalse(longer.equals(shorter));
            assertFalse(shorter.equals(longer));
        }

        @Test
        public void testEquals_Shape2By2_NullInput() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 5 } });

            assertEquals(m1, m2);
            assertNotEquals(m1, m3);
            assertNotEquals(m1, null);
            assertNotEquals(m1, "not a matrix");
        }

        @Test
        public void testEquals_Null() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertNotEquals(m1, null);
        }
    }

    @Nested
    class ToStringTests {

        @Test
        public void testToString() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String str = m.toString();
            assertTrue(str.contains("1"));
            assertTrue(str.contains("2"));
            assertTrue(str.contains("3"));
            assertTrue(str.contains("4"));
        }

        @Test
        public void testToString_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1"));
            assertTrue(str.contains("2"));
            assertTrue(str.contains("3"));
            assertTrue(str.contains("4"));
        }

        @Test
        public void testToString_Shape2By2_ContainsElementText() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1"));
            assertTrue(str.contains("4"));
        }

        @Test
        public void testIntMatrixToString() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals("[[1, 2, 3], [4, 5, 6]]", matrix.toString());
        }

        @Test
        public void testToString_Shape2By2_NonemptyRendering() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.length() > 0);
        }
    }

    @Nested
    class ElementTypeTests {

        @Test
        public void testElementType() {
            assertEquals(int.class, matrix.elementType());
        }

        @Test
        public void testElementType_Shape1By1() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 } });
            assertEquals(int.class, m.elementType());
        }
    }

    @Nested
    class UnsafeBackingArrayTests {

        @Test
        public void testToArray() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[][] arr = m.unsafeBackingArray();
            assertEquals(2, arr.length);
            assertEquals(2, arr[0].length);
            assertArrayEquals(new int[] { 1, 2 }, arr[0]);
            assertArrayEquals(new int[] { 3, 4 }, arr[1]);
        }
    }

    @Nested
    class ElementCountTests {

        @Test
        public void testElementCount() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(6, m.elementCount());
        }

        @Test
        public void testElementCount_Empty() {
            IntMatrix m = IntMatrix.empty();
            assertEquals(0, m.elementCount());
        }
    }

    @Nested
    class IsEmptyTests {

        @Test
        public void testIsEmpty() {
            assertTrue(IntMatrix.empty().isEmpty());
            assertFalse(IntMatrix.wrap(new int[][] { { 1 } }).isEmpty());
        }
    }

    @Nested
    class IsSameShapeTests {

        @Test
        public void testIsSameShape() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }

        @Test
        public void testIsSameShape_EmptyMatrices() {
            IntMatrix e1 = IntMatrix.empty();
            IntMatrix e2 = IntMatrix.empty();
            assertTrue(e1.isSameShape(e2));
        }

        @Test
        public void testIsSameShape_NullThrows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.isSameShape(null));
        }
    }

    @Nested
    class ReshapeTests {

        @Test
        public void testReshape() {
            IntMatrix reshaped = matrix.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, reshaped.get(0, i));
            }

            // Test reshape back
            IntMatrix reshaped2 = reshaped.reshape(3, 3);
            assertEquals(matrix, reshaped2);

            // Test empty matrix
            IntMatrix emptyReshaped = emptyMatrix.reshapeAndPad(2, 3);
            assertEquals(2, emptyReshaped.rowCount());
            assertEquals(3, emptyReshaped.columnCount());

            // Test reshape with too-small dimensions throws exception
            assertThrows(IllegalArgumentException.class, () -> matrix.reshape(1, 4));
            assertThrows(IllegalArgumentException.class, () -> matrix.reshape(2, 2));
        }

        @Test
        public void testReshape_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix reshaped = m.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, reshaped.get(0, i));
            }
        }

        @Test
        public void testReshape_Back() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix reshaped = m.reshape(1, 9);
            IntMatrix reshapedBack = reshaped.reshape(3, 3);
            assertEquals(m, reshapedBack);
        }

        @Test
        public void testReshape_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = m.reshape(3, 2);
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(6, reshaped.get(2, 1));
        }

        @Test
        public void testReshape_Shape2By3_DimensionsAndRowMajorValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = m.reshape(3, 2);
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(3, reshaped.get(1, 0));
            assertEquals(4, reshaped.get(1, 1));
        }

        @Test
        public void testReshapeToZeroColumnsFromEmpty() {
            IntMatrix reshaped = IntMatrix.empty().reshape(0, 0);
            assertEquals(0, reshaped.rowCount());
            assertEquals(0, reshaped.columnCount());
        }

        @Test
        public void testRejectUnrepresentableZeroRowNonZeroColumnShape() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            // assertThrows(IllegalArgumentException.class, () -> matrix.copyRows(0, 0));
            // assertThrows(IllegalArgumentException.class, () -> matrix.copyRegion(0, 0, 0, 1));
            // assertThrows(IllegalArgumentException.class, () -> matrix.pad(0, 1));
            assertThrows(IllegalArgumentException.class, () -> matrix.reshape(0, 1));
        }
    }

    @Nested
    class ForEachIndicesTests {

        @Test
        public void testForEachIndex() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<String> indices = new ArrayList<>();
            m.forEachIndices((r, c) -> indices.add(r + "," + c));
            assertEquals(4, indices.size());
            assertTrue(indices.contains("0,0"));
            assertTrue(indices.contains("0,1"));
            assertTrue(indices.contains("1,0"));
            assertTrue(indices.contains("1,1"));
        }

        @Test
        public void testForEachIndices_SequentialRowMajor_AndParallelCompleteness() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            final ParallelMode prev = Matrices.getParallelMode();
            try {
                Matrices.setParallelMode(ParallelMode.FORCE_OFF);
                final List<String> seq = new ArrayList<>();
                m.forEachIndices((i, j) -> seq.add(i + "," + j));
                assertEquals(List.of("0,0", "0,1", "1,0", "1,1", "2,0", "2,1"), seq);

                Matrices.setParallelMode(ParallelMode.FORCE_ON);
                final AtomicInteger count = new AtomicInteger();
                final java.util.Set<String> visited = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
                m.forEachIndices((i, j) -> {
                    count.incrementAndGet();
                    visited.add(i + "," + j);
                });
                assertEquals(6, count.get());
                assertEquals(6, visited.size());
            } finally {
                Matrices.setParallelMode(prev);
            }
        }
    }

    @Nested
    class MainDiagonalPointsTests {

        @Test
        public void testPointsMainDiagonal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Sheet.Point> points = m.mainDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
            assertEquals(2, points.get(2).rowIndex());
            assertEquals(2, points.get(2).columnIndex());
        }

        @Test
        public void testPointsMainDiagonal_NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Sheet.Point> points = m.mainDiagonalPoints().toList();
            assertEquals(2, points.size());
            assertEquals(1, points.get(1).rowIndex());
            assertEquals(1, points.get(1).columnIndex());
        }
    }

    @Nested
    class AntiDiagonalPointsTests {

        @Test
        public void testPointsAntiDiagonal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Sheet.Point> points = m.antiDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(2, points.get(0).columnIndex());
            assertEquals(2, points.get(2).rowIndex());
            assertEquals(0, points.get(2).columnIndex());
        }

        @Test
        public void testPointsAntiDiagonal_NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Sheet.Point> points = m.antiDiagonalPoints().toList();
            assertEquals(2, points.size());
            assertEquals(2, points.get(0).columnIndex());
            assertEquals(1, points.get(1).columnIndex());
        }
    }

    @Nested
    class RowMajorPointsTests {

        @Test
        public void testRowMajorPointsorizontal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
            assertEquals(1, points.get(3).rowIndex());
            assertEquals(1, points.get(3).columnIndex());
        }

        @Test
        public void testRowMajorPointsorizontal_Range() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<Sheet.Point> points = m.rowMajorPoints(1, 3).toList();
            assertEquals(4, points.size());
            assertEquals(1, points.get(0).rowIndex());
        }

        @Test
        public void testRowMajorPointsorizontal_SingleRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.rowMajorPoints(0, 0 + 1).toList();
            assertEquals(2, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }
    }

    @Nested
    class ColumnMajorPointsTests {

        @Test
        public void testColumnMajorPointsertical() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }

        @Test
        public void testColumnMajorPointsertical_Range() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Sheet.Point> points = m.columnMajorPoints(0, 2).toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testColumnMajorPointsertical_SingleColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.columnMajorPoints(1, 1 + 1).toList();
            assertEquals(2, points.size());
            assertEquals(1, points.get(0).columnIndex());
        }
    }

    @Nested
    class RowPointsTests {

        @Test
        public void testRowPointsows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<List<Sheet.Point>> rows = m.rowPoints().map(s -> s.toList()).toList();
            assertEquals(3, rows.size());
            assertEquals(2, rows.get(0).size());
            assertEquals(0, rows.get(0).get(0).rowIndex());
            assertEquals(2, rows.get(2).get(0).rowIndex());
        }

        @Test
        public void testRowPointsows_Range() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<List<Sheet.Point>> rows = m.rowPoints(1, 3).map(s -> s.toList()).toList();
            assertEquals(2, rows.size());
            assertEquals(1, rows.get(0).get(0).rowIndex());
        }
    }

    @Nested
    class ColumnPointsTests {

        @Test
        public void testColumnPointsolumns() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<List<Sheet.Point>> cols = m.columnPoints().map(s -> s.toList()).toList();
            assertEquals(3, cols.size());
            assertEquals(2, cols.get(0).size());
            assertEquals(0, cols.get(0).get(0).columnIndex());
        }

        @Test
        public void testColumnPointsolumns_Range() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<List<Sheet.Point>> cols = m.columnPoints(1, 3).map(s -> s.toList()).toList();
            assertEquals(2, cols.size());
            assertEquals(1, cols.get(0).get(0).columnIndex());
        }
    }

    @Nested
    class AcceptTests {

        @Test
        public void testAccept() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[] sum = { 0 };
            m.accept(matrix -> {
                IntList flat = matrix.flatten();
                for (int i = 0; i < flat.size(); i++) {
                    sum[0] += flat.get(i);
                }
            });
            assertEquals(10, sum[0]);
        }

        @Test
        public void testAccept_NullThrows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.accept(null));
        }
    }

    @Nested
    class ApplyTests {

        @Test
        public void testApply() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long count = m.apply(matrix -> matrix.elementCount());
            assertEquals(4, count);
        }

        @Test
        public void testApply_NullThrows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.apply(null));
        }
    }

    @Nested
    class PrintlnTests {

        @Test
        public void testPrintln() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertFalse(m.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> m.println());

            IntMatrix empty = IntMatrix.empty();
            assertTrue(empty.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> empty.println());
        }
    }
}
