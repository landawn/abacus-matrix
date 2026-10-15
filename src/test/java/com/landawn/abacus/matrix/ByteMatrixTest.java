package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
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

import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.stream.ByteIteratorEx;
import com.landawn.abacus.util.stream.ByteStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

class ByteMatrixTest extends MatrixTestSupport {

    private static void assertShape(final int expectedRows, final int expectedColumns, final AbstractMatrix<?, ?, ?, ?, ?> matrix) {
        assertEquals(expectedRows, matrix.rowCount());
        assertEquals(expectedColumns, matrix.columnCount());
    }

    @Nested
    class ConstructorTests {

        @Test
        public void testConstructor() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = new ByteMatrix(a);
            Assertions.assertEquals(2, matrix.rowCount());
            Assertions.assertEquals(2, matrix.columnCount());

            assertThrows(IllegalArgumentException.class, () -> new ByteMatrix(null));
        }

        @Test
        public void testConstructor_WithValidArray() {
            byte[][] arr = { { 1, 2 }, { 3, 4 } };
            ByteMatrix m = new ByteMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testConstructor_WithValidArray_Values() {
            byte[][] arr = { { 1, 2 }, { 3, 4 } };
            ByteMatrix m = new ByteMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testConstructor_WithNonSquareMatrix() {
            byte[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix m = new ByteMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
        }

        @Test
        public void testConstructor_LargeMatrix() {
            byte[][] arr = new byte[100][100];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    arr[i][j] = (byte) ((i + j) % 128);
                }
            }
            ByteMatrix m = new ByteMatrix(arr);
            assertEquals(100, m.rowCount());
            assertEquals(100, m.columnCount());
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
        }

        @Test
        public void testConstructor_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> new ByteMatrix(null));
        }

        @Test
        public void testConstructor_WithEmptyArray() {
            ByteMatrix m = new ByteMatrix(new byte[0][0]);
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
            assertTrue(m.isEmpty());
        }

        // Just verify elements exist (values are random)
        @Test
        public void testConstructor_WithSingleElement() {
            ByteMatrix m = new ByteMatrix(new byte[][] { { 42 } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42, m.get(0, 0));
        }

        @Test
        public void testConstructor_EmptyArray() {
            ByteMatrix m = new ByteMatrix(new byte[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testConstructor_SingleElement() {
            ByteMatrix m = new ByteMatrix(new byte[][] { { 5 } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(5, m.get(0, 0));
        }

        @Test
        public void testConstructor_WithNegativeValues() {
            byte[][] arr = { { -1, -2 }, { -3, -4 } };
            ByteMatrix m = new ByteMatrix(arr);
            assertEquals(-1, m.get(0, 0));
            assertEquals(-4, m.get(1, 1));
        }
    }

    @Nested
    class EmptyTests {

        @Test
        public void testEmpty() {
            ByteMatrix empty = ByteMatrix.empty();
            Assertions.assertEquals(0, empty.rowCount());
            Assertions.assertEquals(0, empty.columnCount());
            Assertions.assertTrue(empty.isEmpty());
        }

        @Test
        public void testEmpty_EmptyMatrix_Identity() {
            ByteMatrix empty = ByteMatrix.empty();
            assertEquals(0, empty.rowCount());
            assertEquals(0, empty.columnCount());
            assertTrue(empty.isEmpty());

            // Test singleton
            assertSame(ByteMatrix.empty(), ByteMatrix.empty());
        }

        @Test
        public void testEmptyMatrixOperations() {
            ByteMatrix empty = ByteMatrix.empty();

            assertTrue(empty.flatten().isEmpty());
            assertEquals(0, empty.copy().rowCount);
            assertEquals(empty, empty.transpose());
            assertEquals(empty, empty.rotate90());

            ByteMatrix extended = empty.resize(2, 2, (byte) 5);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals(5, extended.get(0, 0));
        }

        @Test
        public void testByteMatrixEmptyRowCount() {
            ByteMatrix matrix = ByteMatrix.empty();
            assertEquals(0, matrix.rowCount());
        }

        @Test
        public void testByteMatrixEmptyToString() {
            // ByteMatrix.java: System.out.println(empty.toString());   // Output: []
            ByteMatrix empty = ByteMatrix.empty();
            assertEquals("[]", empty.toString());
        }
    }

    @Nested
    class WrapTests {

        @Test
        public void testWrap() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);
            Assertions.assertEquals(2, matrix.rowCount());
            Assertions.assertEquals(2, matrix.columnCount());

            ByteMatrix emptyMatrix = ByteMatrix.wrap();
            Assertions.assertTrue(emptyMatrix.isEmpty());

            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.wrap((byte[][]) null));
        }

        @Test
        public void testWrap_WithValidArray() {
            byte[][] arr = { { 1, 2 }, { 3, 4 } };
            ByteMatrix m = ByteMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testByteMatrixWrapGet() {
            // ByteMatrix.java: ByteMatrix matrix = ByteMatrix.wrap(new byte[][] {{1, 2, 3}, {4, 5, 6}});
            // matrix.get(1, 2) returns 6
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals((byte) 6, matrix.get(1, 2));
        }

        @Test
        public void testWrap_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.wrap((byte[][]) null));
        }

        @Test
        public void testWrap_WithEmptyArray() {
            ByteMatrix m = ByteMatrix.wrap(new byte[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testByteMinMaxValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { Byte.MAX_VALUE, Byte.MIN_VALUE } });
            assertEquals(Byte.MAX_VALUE, m.get(0, 0));
            assertEquals(Byte.MIN_VALUE, m.get(0, 1));
        }

        @Test
        public void testWrap_WithSingleRow() {
            ByteMatrix m = ByteMatrix.wrap(new byte[] { 1, 2, 3 });
            assertEquals(1, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(0, 2));
        }
    }

    @Nested
    class CopyOfTests {

        @Test
        public void testCopyOf() {
            byte[][] arr = { { 1, 2 }, { 3, 4 } };
            ByteMatrix m = ByteMatrix.copyOf(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals((byte) 1, m.get(0, 0));
            assertEquals((byte) 4, m.get(1, 1));

            // Mutating the source array must not affect the copy
            arr[0][0] = 99;
            assertEquals((byte) 1, m.get(0, 0));

            // Mutating the copy must not affect the source array
            m.set(1, 1, (byte) 88);
            assertEquals((byte) 4, arr[1][1]);

            // null -> rejected; empty -> shared empty matrix
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.copyOf((byte[][]) null));
            assertTrue(ByteMatrix.copyOf(new byte[0][0]).isEmpty());

            // non-rectangular -> IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.copyOf(new byte[][] { { 1, 2 }, { 3 } }));
        }
    }

    @Nested
    class RandomRowTests {

        @Test
        public void testRandom() {
            ByteMatrix matrix = ByteMatrix.randomRow(5);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(5, matrix.columnCount());
        }

        @Test
        public void testRandom_Values() {
            ByteMatrix m = ByteMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            // Just verify elements exist (values are random)
            for (int i = 0; i < 5; i++) {
                assertNotNull(m.get(0, i));
            }
        }

        @Test
        public void testRandom_WithLargeLength() {
            ByteMatrix m = ByteMatrix.randomRow(1000);
            assertEquals(1, m.rowCount());
            assertEquals(1000, m.columnCount());
        }

        @Test
        public void testRandomRow_Values_SingleRowDimensions() {
            ByteMatrix m = ByteMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }

        @Test
        public void testRandom_WithZeroLength() {
            ByteMatrix m = ByteMatrix.randomRow(0);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testFactories_NegativeDimensions_ThrowIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.randomRow(-1));
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.random(-1, 2));
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.random(2, -1));
        }
    }

    @Nested
    class RandomTests {

        @Test
        public void testRandomGeneratorOverloadsAreReproducibleAndPreserveZeroRowShape() {
            assertEquals(ByteMatrix.random(4, 5, new java.util.Random(1234)), ByteMatrix.random(4, 5, new java.util.Random(1234)));
            assertEquals(ByteMatrix.randomRow(5, new java.util.Random(4321)), ByteMatrix.randomRow(5, new java.util.Random(4321)));
            assertShape(0, 7, ByteMatrix.random(0, 7, new java.util.Random(1)));
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.random(1, 1, null));
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.randomRow(1, null));
        }

        @Test
        public void testByteDiagonalStreamsAreEmptyWhenEitherDimensionIsZero() {
            for (final ByteMatrix matrix : new ByteMatrix[] { ByteMatrix.empty(), ByteMatrix.random(0, 3), ByteMatrix.random(3, 0) }) {
                assertEquals(0, matrix.mainDiagonalStream().count());
                assertEquals(0, matrix.antiDiagonalStream().count());
            }
        }

        @Test
        public void testRandom_WithRowsCols() {
            ByteMatrix m = ByteMatrix.random(2, 3);
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
            ByteMatrix matrix = ByteMatrix.range((byte) 1, (byte) 5);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(4, matrix.columnCount());
            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(0, 1));
            Assertions.assertEquals(3, matrix.get(0, 2));
            Assertions.assertEquals(4, matrix.get(0, 3));
        }

        @Test
        public void testRangeWithStep() {
            ByteMatrix matrix = ByteMatrix.range((byte) 0, (byte) 10, (byte) 2);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(5, matrix.columnCount());
            Assertions.assertEquals(0, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(0, 1));
            Assertions.assertEquals(4, matrix.get(0, 2));
            Assertions.assertEquals(6, matrix.get(0, 3));
            Assertions.assertEquals(8, matrix.get(0, 4));
        }

        @Test
        public void testRange_ArrayValues() {
            ByteMatrix m = ByteMatrix.range((byte) 0, (byte) 5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new byte[] { 0, 1, 2, 3, 4 }, m.rowView(0));
        }

        @Test
        public void testRange_WithStep() {
            ByteMatrix m = ByteMatrix.range((byte) 0, (byte) 10, (byte) 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new byte[] { 0, 2, 4, 6, 8 }, m.rowView(0));
        }

        @Test
        public void testRange_Values() {
            ByteMatrix m = ByteMatrix.range((byte) 1, (byte) 5);
            assertEquals(1, m.rowCount());
            assertEquals(4, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(0, 2));
            assertEquals(4, m.get(0, 3));
        }

        @Test
        public void testRange_WithStep_Values() {
            ByteMatrix m = ByteMatrix.range((byte) 0, (byte) 10, (byte) 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals(0, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(4, m.get(0, 2));
            assertEquals(6, m.get(0, 3));
            assertEquals(8, m.get(0, 4));
        }

        @Test
        public void testRange_Values_ExclusiveEndpoints() {
            ByteMatrix m = ByteMatrix.range((byte) 0, (byte) 5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals(0, m.get(0, 0));
            assertEquals(4, m.get(0, 4));
        }

        @Test
        public void testRange_WithBy() {
            ByteMatrix m = ByteMatrix.range((byte) 0, (byte) 10, (byte) 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals(0, m.get(0, 0));
            assertEquals(8, m.get(0, 4));
        }

        @Test
        public void testByteMatrixRange() {
            // ByteMatrix.java: ByteMatrix range = ByteMatrix.range((byte)1, (byte)5);
            // Creates matrix: [[1, 2, 3, 4]]
            ByteMatrix range = ByteMatrix.range((byte) 1, (byte) 5);
            assertEquals("[[1, 2, 3, 4]]", range.toString());
        }

        @Test
        public void testByteMatrixRangeWithStep() {
            // ByteMatrix.java: ByteMatrix range = ByteMatrix.range((byte)0, (byte)10, (byte)2);    // Creates [[0, 2, 4, 6, 8]]
            ByteMatrix range = ByteMatrix.range((byte) 0, (byte) 10, (byte) 2);
            assertEquals("[[0, 2, 4, 6, 8]]", range.toString());
        }

        @Test
        public void testByteMatrixRangeDescending() {
            // ByteMatrix.java: ByteMatrix desc = ByteMatrix.range((byte)10, (byte)0, (byte)-2);    // Creates [[10, 8, 6, 4, 2]]
            ByteMatrix desc = ByteMatrix.range((byte) 10, (byte) 0, (byte) -2);
            assertEquals("[[10, 8, 6, 4, 2]]", desc.toString());
        }

        @Test
        public void testRange_EmptyRange() {
            ByteMatrix m = ByteMatrix.range((byte) 5, (byte) 5);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testRange_WithNegativeStep() {
            ByteMatrix m = ByteMatrix.range((byte) 10, (byte) 0, (byte) -2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new byte[] { 10, 8, 6, 4, 2 }, m.rowView(0));
        }

        @Test
        public void testRange_NegativeValues() {
            ByteMatrix m = ByteMatrix.range((byte) -5, (byte) 0);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals(-5, m.get(0, 0));
            assertEquals(-1, m.get(0, 4));
        }
    }

    @Nested
    class RangeClosedTests {

        @Test
        public void testRangeClosed() {
            ByteMatrix matrix = ByteMatrix.rangeClosed((byte) 1, (byte) 4);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(4, matrix.columnCount());
            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(4, matrix.get(0, 3));
        }

        @Test
        public void testRangeClosedWithStep() {
            ByteMatrix matrix = ByteMatrix.rangeClosed((byte) 0, (byte) 9, (byte) 3);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(4, matrix.columnCount());
            Assertions.assertEquals(0, matrix.get(0, 0));
            Assertions.assertEquals(3, matrix.get(0, 1));
            Assertions.assertEquals(4, matrix.columnCount());
            Assertions.assertEquals(0, matrix.get(0, 0));
            Assertions.assertEquals(3, matrix.get(0, 1));
            Assertions.assertEquals(6, matrix.get(0, 2));
            Assertions.assertEquals(9, matrix.get(0, 3));
        }

        @Test
        public void testRangeClosed_ArrayValues() {
            ByteMatrix m = ByteMatrix.rangeClosed((byte) 0, (byte) 4);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new byte[] { 0, 1, 2, 3, 4 }, m.rowView(0));
        }

        @Test
        public void testRangeClosed_WithStep() {
            ByteMatrix m = ByteMatrix.rangeClosed((byte) 0, (byte) 10, (byte) 2);
            assertEquals(1, m.rowCount());
            assertEquals(6, m.columnCount());
            assertArrayEquals(new byte[] { 0, 2, 4, 6, 8, 10 }, m.rowView(0));
        }

        @Test
        public void testRangeClosed_Values() {
            ByteMatrix m = ByteMatrix.rangeClosed((byte) 1, (byte) 4);
            assertEquals(1, m.rowCount());
            assertEquals(4, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(0, 3));
        }

        @Test
        public void testRangeClosed_WithStep_Values() {
            ByteMatrix m = ByteMatrix.rangeClosed((byte) 0, (byte) 9, (byte) 3);
            assertEquals(1, m.rowCount());
            assertEquals(4, m.columnCount());
            assertEquals(0, m.get(0, 0));
            assertEquals(3, m.get(0, 1));
            assertEquals(6, m.get(0, 2));
            assertEquals(9, m.get(0, 3));
        }

        @Test
        public void testRangeClosed_Values_InclusiveEndpoints() {
            ByteMatrix m = ByteMatrix.rangeClosed((byte) 0, (byte) 5);
            assertEquals(1, m.rowCount());
            assertEquals(6, m.columnCount());
            assertEquals(0, m.get(0, 0));
            assertEquals(5, m.get(0, 5));
        }

        @Test
        public void testRangeClosed_WithBy() {
            ByteMatrix m = ByteMatrix.rangeClosed((byte) 0, (byte) 10, (byte) 2);
            assertEquals(1, m.rowCount());
            assertEquals(6, m.columnCount());
            assertEquals(0, m.get(0, 0));
            assertEquals(10, m.get(0, 5));
        }

        @Test
        public void testByteMatrixRangeClosed() {
            // ByteMatrix.java: ByteMatrix range = ByteMatrix.rangeClosed((byte)1, (byte)4);
            // Creates matrix: [[1, 2, 3, 4]]
            ByteMatrix range = ByteMatrix.rangeClosed((byte) 1, (byte) 4);
            assertEquals("[[1, 2, 3, 4]]", range.toString());
        }

        @Test
        public void testByteMatrixRangeClosedWithStep() {
            // ByteMatrix.java: ByteMatrix range = ByteMatrix.rangeClosed((byte)0, (byte)8, (byte)2);     // Creates [[0, 2, 4, 6, 8]]
            ByteMatrix range = ByteMatrix.rangeClosed((byte) 0, (byte) 8, (byte) 2);
            assertEquals("[[0, 2, 4, 6, 8]]", range.toString());
        }

        @Test
        public void testByteMatrixRangeClosedPartial() {
            // ByteMatrix.java: ByteMatrix partial = ByteMatrix.rangeClosed((byte)0, (byte)9, (byte)2);   // Creates [[0, 2, 4, 6, 8]] (9 not reachable)
            ByteMatrix partial = ByteMatrix.rangeClosed((byte) 0, (byte) 9, (byte) 2);
            assertEquals("[[0, 2, 4, 6, 8]]", partial.toString());
        }

        @Test
        public void testByteMatrixRangeClosedDescending() {
            // ByteMatrix.java: ByteMatrix desc = ByteMatrix.rangeClosed((byte)10, (byte)0, (byte)-2);    // Creates [[10, 8, 6, 4, 2, 0]]
            ByteMatrix desc = ByteMatrix.rangeClosed((byte) 10, (byte) 0, (byte) -2);
            assertEquals("[[10, 8, 6, 4, 2, 0]]", desc.toString());
        }

        @Test
        public void testRangeClosed_SingleElement() {
            ByteMatrix m = ByteMatrix.rangeClosed((byte) 5, (byte) 5);
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(5, m.get(0, 0));
        }
    }

    @Nested
    class OfMainDiagonalTests {

        @Test
        public void testByteMatrixMainDiagonal() {
            // ByteMatrix.java: ByteMatrix matrix = ByteMatrix.ofMainDiagonal(new byte[] {1, 2, 3});
            // Creates 3x3 matrix with diagonal [1, 2, 3] and zeros elsewhere
            ByteMatrix matrix = ByteMatrix.ofMainDiagonal(new byte[] { 1, 2, 3 });
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertEquals((byte) 1, matrix.get(0, 0));
            assertEquals((byte) 2, matrix.get(1, 1));
            assertEquals((byte) 3, matrix.get(2, 2));
            assertEquals((byte) 0, matrix.get(0, 1));
            assertEquals((byte) 0, matrix.get(1, 0));
        }

        @Test
        public void testDiagonalLU2RD() {
            byte[] diagonal = { 1, 2, 3 };
            ByteMatrix matrix = ByteMatrix.ofMainDiagonal(diagonal);
            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(1, 1));
            Assertions.assertEquals(3, matrix.get(2, 2));
            Assertions.assertEquals(0, matrix.get(0, 1));
        }

        @Test
        public void testDiagonalLU2RD_Values() {
            ByteMatrix m = ByteMatrix.ofMainDiagonal(new byte[] { 1, 2, 3 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
            assertEquals(0, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
        }

        @Test
        public void testOfMainDiagonal_Empty() {
            ByteMatrix m = ByteMatrix.ofMainDiagonal(new byte[0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testDiagonalLU2RD_SingleElement() {
            ByteMatrix m = ByteMatrix.ofMainDiagonal(new byte[] { 42 });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42, m.get(0, 0));
        }
    }

    @Nested
    class OfAntiDiagonalTests {

        @Test
        public void testByteMatrixAntiDiagonal() {
            // ByteMatrix.java: ByteMatrix matrix = ByteMatrix.ofAntiDiagonal(new byte[] {1, 2, 3});
            // Creates 3x3 matrix with anti-diagonal [1, 2, 3] and zeros elsewhere
            ByteMatrix matrix = ByteMatrix.ofAntiDiagonal(new byte[] { 1, 2, 3 });
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertEquals((byte) 1, matrix.get(0, 2));
            assertEquals((byte) 2, matrix.get(1, 1));
            assertEquals((byte) 3, matrix.get(2, 0));
            assertEquals((byte) 0, matrix.get(0, 0));
        }

        @Test
        public void testDiagonalRU2LD() {
            byte[] diagonal = { 1, 2, 3 };
            ByteMatrix matrix = ByteMatrix.ofAntiDiagonal(diagonal);
            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals(1, matrix.get(0, 2));
            Assertions.assertEquals(2, matrix.get(1, 1));
            Assertions.assertEquals(3, matrix.get(2, 0));
            Assertions.assertEquals(0, matrix.get(0, 0));
        }

        @Test
        public void testDiagonalRU2LD_Values() {
            ByteMatrix m = ByteMatrix.ofAntiDiagonal(new byte[] { 1, 2, 3 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 2));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 0));
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(2, 2));
        }

        @Test
        public void testDiagonalRU2LD_Empty() {
            ByteMatrix m = ByteMatrix.ofAntiDiagonal(new byte[0]);
            assertTrue(m.isEmpty());
        }
    }

    @Nested
    class OfDiagonalsTests {

        @Test
        public void testDiagonal() {
            byte[] main = { 1, 2, 3 };
            byte[] anti = { 4, 5, 6 };
            ByteMatrix matrix = ByteMatrix.ofDiagonals(main, anti);
            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(1, 1));
            Assertions.assertEquals(3, matrix.get(2, 2));
            Assertions.assertEquals(4, matrix.get(0, 2));
            Assertions.assertEquals(2, matrix.get(1, 1));
            Assertions.assertEquals(6, matrix.get(2, 0));

            Assertions.assertThrows(IllegalArgumentException.class, () -> ByteMatrix.ofDiagonals(null, null));

            Assertions.assertThrows(IllegalArgumentException.class, () -> ByteMatrix.ofDiagonals(new byte[] { 1 }, new byte[] { 2, 3 }));
        }

        @Test
        public void testDiagonal_WithBothDiagonals() {
            ByteMatrix m = ByteMatrix.ofDiagonals(new byte[] { 1, 2, 3 }, new byte[] { 4, 5, 6 });
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
            ByteMatrix m = ByteMatrix.ofDiagonals(new byte[] { 1, 2, 3 }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
        }

        @Test
        public void testDiagonal_WithOnlyAntiDiagonal() {
            ByteMatrix m = ByteMatrix.ofDiagonals(null, new byte[] { 4, 5, 6 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(4, m.get(0, 2));
            assertEquals(5, m.get(1, 1));
            assertEquals(6, m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithDifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.ofDiagonals(new byte[] { 1, 2 }, new byte[] { 3, 4, 5 }));
        }

        @Test
        public void testDiagonal_WithDifferentLengths_Validation() {
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.ofDiagonals(new byte[] { 1, 2 }, new byte[] { 1, 2, 3 }));
        }

        @Test
        public void testDiagonal_WithBothDiagonalsOverlapping() {
            ByteMatrix m = ByteMatrix.ofDiagonals(new byte[] { 1, 2, 3 }, new byte[] { 4, 5, 6 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            // Center element should be from main diagonal (set second)
            assertEquals(2, m.get(1, 1));
        }

        @Test
        public void testDiagonal_Both() {
            ByteMatrix m = ByteMatrix.ofDiagonals(new byte[] { 1, 2, 3 }, new byte[] { 4, 5, 6 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(0, 2));
        }

        @Test
        public void testDiagonal_WithBothNull() {
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.ofDiagonals(null, null));
        }

        @Test
        public void testOfDiagonals_EmptyAndNullSidesPreserveEmptyShape() {
            assertTrue(ByteMatrix.ofDiagonals(new byte[0], new byte[0]).isEmpty());
            assertTrue(ByteMatrix.ofDiagonals(new byte[0], null).isEmpty());
            assertTrue(ByteMatrix.ofDiagonals(null, new byte[0]).isEmpty());
        }
    }

    @Nested
    class UnboxTests {

        @Test
        public void testUnbox() {
            Byte[][] boxed = { { 1, 2 }, { 3, 4 } };
            Matrix<Byte> boxedMatrix = Matrix.wrap(boxed);
            ByteMatrix unboxed = ByteMatrix.unbox(boxedMatrix);
            Assertions.assertEquals(2, unboxed.rowCount());
            Assertions.assertEquals(2, unboxed.columnCount());
            Assertions.assertEquals(1, unboxed.get(0, 0));
            Assertions.assertEquals(4, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_Values() {
            Byte[][] boxed = { { 1, 2 }, { 3, 4 } };
            Matrix<Byte> boxedMatrix = Matrix.wrap(boxed);
            ByteMatrix unboxed = ByteMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertEquals(1, unboxed.get(0, 0));
            assertEquals(4, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_Values_DimensionsAndFirstRowValues() {
            Byte[][] boxed = { { 1, 2 }, { 3, 4 } };
            Matrix<Byte> boxedMatrix = Matrix.wrap(boxed);
            ByteMatrix unboxed = ByteMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertEquals(1, unboxed.get(0, 0));
            assertEquals(2, unboxed.get(0, 1));
        }

        @Test
        public void testUnbox_Shape2By2_NullInput() {
            Matrix<Byte> boxed = Matrix.wrap(new Byte[][] { { 1, 2 }, { null, 4 } });
            ByteMatrix primitive = ByteMatrix.unbox(boxed);
            assertEquals(1, primitive.get(0, 0));
            assertEquals(2, primitive.get(0, 1));
            assertEquals(0, primitive.get(1, 0)); // null becomes 0
            assertEquals(4, primitive.get(1, 1));
        }

        @Test
        public void testUnbox_WithNullValues() {
            Byte[][] boxed = { { 1, null }, { null, 4 } };
            Matrix<Byte> boxedMatrix = Matrix.wrap(boxed);
            ByteMatrix unboxed = ByteMatrix.unbox(boxedMatrix);
            assertEquals(1, unboxed.get(0, 0));
            assertEquals(0, unboxed.get(0, 1)); // null -> 0
            assertEquals(0, unboxed.get(1, 0)); // null -> 0
            assertEquals(4, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_WithNullElements() {
            Byte[][] boxed = { { 1, null }, { null, 4 } };
            Matrix<Byte> boxedMatrix = Matrix.wrap(boxed);
            ByteMatrix unboxed = ByteMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(1, unboxed.get(0, 0));
            assertEquals(0, unboxed.get(0, 1));
            assertEquals(0, unboxed.get(1, 0));
        }

        @Test
        public void testUnbox_IsAllNulls() {
            Byte[][] boxed = { { null, null }, { null, null } };
            Matrix<Byte> boxedMatrix = Matrix.wrap(boxed);
            ByteMatrix unboxed = ByteMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertEquals(0, unboxed.get(0, 0));
            assertEquals(0, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_TypeNeutralGenericStorageAndEmptyShapes() {
            assertSame(ByteMatrix.empty(), ByteMatrix.unbox(Matrix.empty(Byte.class)));
            assertSame(ByteMatrix.empty(), ByteMatrix.unbox(Matrix.empty(Byte.class).copy()));
            assertSame(ByteMatrix.empty(), ByteMatrix.unbox(Matrix.wrap(new Byte[0][0])));

            ByteMatrix rowsWithNoColumns = ByteMatrix.unbox(Matrix.wrap(new Byte[2][0]));
            assertEquals(2, rowsWithNoColumns.rowCount());
            assertEquals(0, rowsWithNoColumns.columnCount());

            ByteMatrix typeNeutralRowsWithNoColumns = ByteMatrix.unbox(Matrix.empty(Byte.class).resize(2, 0));
            assertEquals(2, typeNeutralRowsWithNoColumns.rowCount());
            assertEquals(0, typeNeutralRowsWithNoColumns.columnCount());

            Matrix<Byte> expandedTypeNeutral = Matrix.empty(Byte.class).resize(2, 2);
            expandedTypeNeutral.set(0, 1, (byte) 7);
            assertEquals(ByteMatrix.wrap(new byte[][] { { 0, 7 }, { 0, 0 } }), ByteMatrix.unbox(expandedTypeNeutral));
        }
    }

    @Nested
    class GetTests {

        @Test
        public void testGet() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);
            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(0, 1));
            Assertions.assertEquals(3, matrix.get(1, 0));
            Assertions.assertEquals(4, matrix.get(1, 1));

            Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(2, 0));
            Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(0, 2));
        }

        @Test
        public void testGetWithPoint() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);
            Assertions.assertEquals(1, matrix.get(Point.of(0, 0)));
            Assertions.assertEquals(4, matrix.get(Point.of(1, 1)));
        }

        @Test
        public void testGet_Shape2By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(5, m.get(1, 1));
            assertEquals(6, m.get(1, 2));
        }

        @Test
        public void testGetWithPoint_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(1, m.get(Point.of(0, 0)));
            assertEquals(4, m.get(Point.of(1, 1)));
            assertEquals(2, m.get(Point.of(0, 1)));
        }

        @Test
        public void testGet_ByIndices() {
            byte[][] arr = { { 1, 2 }, { 3, 4 } };
            ByteMatrix m = ByteMatrix.wrap(arr);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testGet_ByPoint() {
            byte[][] arr = { { 1, 2 }, { 3, 4 } };
            ByteMatrix m = ByteMatrix.wrap(arr);
            assertEquals(1, m.get(Point.of(0, 0)));
            assertEquals(2, m.get(Point.of(0, 1)));
            assertEquals(3, m.get(Point.of(1, 0)));
            assertEquals(4, m.get(Point.of(1, 1)));
        }

        @Test
        public void testGet_AllPositions() {
            byte[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ByteMatrix m = ByteMatrix.wrap(arr);
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    assertEquals(arr[i][j], m.get(i, j));
                }
            }
        }

        @Test
        public void testGet_ByIndices_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testGet_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
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
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);
            matrix.set(0, 0, (byte) 5);
            Assertions.assertEquals(5, matrix.get(0, 0));

            Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.set(2, 0, (byte) 5));
        }

        @Test
        public void testSetWithPoint() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);
            matrix.set(Point.of(1, 1), (byte) 5);
            Assertions.assertEquals(5, matrix.get(1, 1));
        }

        @Test
        public void testSet_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.set(0, 0, (byte) 10);
            assertEquals(10, m.get(0, 0));

            m.set(1, 1, (byte) 20);
            assertEquals(20, m.get(1, 1));
        }

        @Test
        public void testSetWithPoint_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.set(Point.of(0, 0), (byte) 50);
            assertEquals(50, m.get(Point.of(0, 0)));
        }

        @Test
        public void testSet_ByIndices() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0 }, { 0, 0 } });
            m.set(0, 0, (byte) 1);
            m.set(1, 1, (byte) 4);
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
            assertEquals(0, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
        }

        @Test
        public void testSet_ByPoint() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0 }, { 0, 0 } });
            m.set(Point.of(0, 0), (byte) 1);
            m.set(Point.of(1, 1), (byte) 4);
            assertEquals(1, m.get(Point.of(0, 0)));
            assertEquals(4, m.get(Point.of(1, 1)));
            assertEquals(0, m.get(Point.of(0, 1)));
        }

        @Test
        public void testSet_OverwriteValue() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            m.set(0, 0, (byte) 10);
            assertEquals(10, m.get(0, 0));
            m.set(0, 0, (byte) 20);
            assertEquals(20, m.get(0, 0));
        }

        @Test
        public void testSet_ByIndices_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.set(0, 1, (byte) 10);
            assertEquals(10, m.get(0, 1));
        }

        @Test
        public void testSet_ByPoint_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.set(Point.of(1, 0), (byte) 20);
            assertEquals(20, m.get(1, 0));
        }

        // Leftmost column has no element to the left
        @Test
        public void testSet_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, (byte) 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(2, 0, (byte) 0));
        }

        @Test
        public void testSet_NegativeValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0 } });
            m.set(0, 0, (byte) -5);
            assertEquals(-5, m.get(0, 0));
        }
    }

    @Nested
    class ValueAboveTests {

        @Test
        public void testValueAbove() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            OptionalByte up = matrix.valueAbove(1, 0);
            Assertions.assertTrue(up.isPresent());
            Assertions.assertEquals(1, up.get());

            OptionalByte empty = matrix.valueAbove(0, 0);
            Assertions.assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });

            OptionalByte up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1, up.get());

            // Top row has no element above
            OptionalByte empty = m.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_MultipleRows() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            OptionalByte up = m.valueAbove(2, 1);
            assertTrue(up.isPresent());
            assertEquals(4, up.get());
        }

        @Test
        public void testValueAbove_Exists() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            OptionalByte up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1, up.get());
        }

        @Test
        public void testValueAbove_NotExists() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            OptionalByte up = m.valueAbove(0, 0);
            assertFalse(up.isPresent());
        }
    }

    @Nested
    class ValueBelowTests {

        @Test
        public void testValueBelow() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            OptionalByte down = matrix.valueBelow(0, 0);
            Assertions.assertTrue(down.isPresent());
            Assertions.assertEquals(3, down.get());

            OptionalByte empty = matrix.valueBelow(1, 0);
            Assertions.assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });

            OptionalByte down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3, down.get());

            // Bottom row has no element below
            OptionalByte empty = m.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_AtBottomEdge() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            OptionalByte down = m.valueBelow(1, 0);
            assertFalse(down.isPresent());
        }

        @Test
        public void testValueBelow_MultipleRows() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            OptionalByte down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3, down.get());
        }

        @Test
        public void testValueBelow_Exists() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            OptionalByte down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3, down.get());
        }
    }

    @Nested
    class ValueLeftTests {

        @Test
        public void testValueLeft() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            OptionalByte left = matrix.valueLeft(0, 1);
            Assertions.assertTrue(left.isPresent());
            Assertions.assertEquals(1, left.get());

            OptionalByte empty = matrix.valueLeft(0, 0);
            Assertions.assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });

            OptionalByte left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1, left.get());

            // Leftmost column has no element to the left
            OptionalByte empty = m.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_AtLeftEdge() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            OptionalByte left = m.valueLeft(0, 0);
            assertFalse(left.isPresent());
        }

        @Test
        public void testValueLeft_MultipleColumns() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            OptionalByte left = m.valueLeft(0, 2);
            assertTrue(left.isPresent());
            assertEquals(2, left.get());
        }

        @Test
        public void testValueLeft_Exists() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            OptionalByte left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1, left.get());
        }
    }

    @Nested
    class ValueRightTests {

        @Test
        public void testValueRight() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            OptionalByte right = matrix.valueRight(0, 0);
            Assertions.assertTrue(right.isPresent());
            Assertions.assertEquals(2, right.get());

            OptionalByte empty = matrix.valueRight(0, 1);
            Assertions.assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });

            OptionalByte right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2, right.get());

            // Rightmost column has no element to the right
            OptionalByte empty = m.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_AtRightEdge() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            OptionalByte right = m.valueRight(0, 1);
            assertFalse(right.isPresent());
        }

        @Test
        public void testValueRight_MultipleColumns() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            OptionalByte right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2, right.get());
        }

        @Test
        public void testValueRight_Exists() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            OptionalByte right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2, right.get());
        }
    }

    @Nested
    class RowViewTests {

        @Test
        public void testRow() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] row0 = matrix.rowView(0);
            Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, row0);

            byte[] row1 = matrix.rowView(1);
            Assertions.assertArrayEquals(new byte[] { 4, 5, 6 }, row1);

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(-1));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(2));
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new byte[] { 1, 2, 3 }, m.rowView(0));
            assertArrayEquals(new byte[] { 4, 5, 6 }, m.rowView(1));
        }

        @Test
        public void testRowView_ArrayValues() {
            byte[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix m = ByteMatrix.wrap(arr);
            byte[] row0 = m.rowView(0);
            assertArrayEquals(new byte[] { 1, 2, 3 }, row0);
            byte[] row1 = m.rowView(1);
            assertArrayEquals(new byte[] { 4, 5, 6 }, row1);
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues_FirstRowView() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            byte[] row = m.rowView(0);
            assertArrayEquals(new byte[] { 1, 2, 3 }, row);
        }

        @Test
        public void testRowView() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            byte[] firstRow = matrix.rowView(0);
            assertArrayEquals(new byte[] { 1, 2, 3 }, firstRow);
        }

        @Test
        public void testRowView_SingleRow() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            assertArrayEquals(new byte[] { 1, 2, 3 }, m.rowView(0));
        }

        @Test
        public void testRowView_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(2));
        }

        @Test
        public void testRowView_InvalidIndex() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(1));
        }

        @Test
        public void testRowView_InvalidIndex_Shape1By2_Validation() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(5));
        }
    }

    @Nested
    class RowCopyTests {

        @Test
        public void testRowCopy() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] rowCopy = matrix.rowCopy(0);
            Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, rowCopy);

            rowCopy[0] = 9;
            Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, matrix.rowView(0));
        }

        @Test
        public void testWidenedAndExactArithmetic() {
            ByteMatrix maxAndMin = ByteMatrix.wrap(new byte[][] { { Byte.MAX_VALUE, Byte.MIN_VALUE } });
            ByteMatrix ones = ByteMatrix.wrap(new byte[][] { { 1, 1 } });

            assertArrayEquals(new int[] { 128, -127 }, maxAndMin.addWidened(ones).rowCopy(0));
            ArithmeticException addOverflow = assertThrows(ArithmeticException.class, () -> maxAndMin.addExact(ones));
            assertEquals("byte overflow: 128", addOverflow.getMessage());

            assertArrayEquals(new int[] { 126, -129 }, maxAndMin.subtractWidened(ones).rowCopy(0));
            ArithmeticException subtractOverflow = assertThrows(ArithmeticException.class, () -> maxAndMin.subtractExact(ones));
            assertEquals("byte overflow: -129", subtractOverflow.getMessage());

            assertEquals(ByteMatrix.wrap(new byte[][] { { 3, 5 } }),
                    ByteMatrix.wrap(new byte[][] { { 1, 2 } }).addExact(ByteMatrix.wrap(new byte[][] { { 2, 3 } })));

            ByteMatrix left = ByteMatrix.wrap(new byte[][] { { Byte.MAX_VALUE, Byte.MAX_VALUE } });
            ByteMatrix right = ByteMatrix.wrap(new byte[][] { { Byte.MAX_VALUE }, { Byte.MAX_VALUE } });
            assertEquals(32258L, left.matrixMultiplyWidened(right).get(0, 0));
            assertThrows(ArithmeticException.class, () -> left.matrixMultiplyExact(right));

            ByteMatrix cancellationLeft = ByteMatrix.wrap(new byte[][] { { 100, 100 } });
            ByteMatrix cancellationRight = ByteMatrix.wrap(new byte[][] { { 1 }, { -1 } });
            assertEquals((byte) 0, cancellationLeft.matrixMultiplyExact(cancellationRight).get(0, 0));
        }

        @Test
        public void testRowCopy_InvalidIndex() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(-1));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(2));
        }

        @Test
        public void testAliasedRowsAreRejected() {
            byte[] sharedRow = { 1, 2 };
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.wrap(new byte[][] { sharedRow, sharedRow }));

            byte[][] outer = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(outer);
            outer[0] = new byte[] { 9, 9 };
            assertArrayEquals(new byte[] { 1, 2 }, matrix.rowCopy(0));
        }
    }

    @Nested
    class ColumnCopyTests {

        @Test
        public void testColumn() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] col0 = matrix.columnCopy(0);
            Assertions.assertArrayEquals(new byte[] { 1, 4 }, col0);

            byte[] col1 = matrix.columnCopy(1);
            Assertions.assertArrayEquals(new byte[] { 2, 5 }, col1);

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(-1));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(3));
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new byte[] { 1, 4 }, m.columnCopy(0));
            assertArrayEquals(new byte[] { 2, 5 }, m.columnCopy(1));
            assertArrayEquals(new byte[] { 3, 6 }, m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_ArrayValues() {
            byte[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix m = ByteMatrix.wrap(arr);
            byte[] col0 = m.columnCopy(0);
            assertArrayEquals(new byte[] { 1, 4 }, col0);
            byte[] col1 = m.columnCopy(1);
            assertArrayEquals(new byte[] { 2, 5 }, col1);
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues_FirstColumnSnapshot() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            byte[] col = m.columnCopy(0);
            assertArrayEquals(new byte[] { 1, 4 }, col);
        }

        @Test
        public void testColumnCopy() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            byte[] firstColumn = matrix.columnCopy(0);
            assertArrayEquals(new byte[] { 1, 4 }, firstColumn);
        }

        @Test
        public void testColumnCopy_SingleColumn() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1 }, { 2 }, { 3 } });
            assertArrayEquals(new byte[] { 1, 2, 3 }, m.columnCopy(0));
        }

        @Test
        public void testColumnCopy_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex_Shape1By2_Validation() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(5));
        }
    }

    @Nested
    class SetRowTests {

        @Test
        public void testSetRow() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.setRow(0, new byte[] { 7, 8, 9 });
            Assertions.assertArrayEquals(new byte[] { 7, 8, 9 }, matrix.rowView(0));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setRow(0, new byte[] { 1, 2 }));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setRow(0, (byte[]) null));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.setRow(0, new byte[] { 10, 20 });
            assertArrayEquals(new byte[] { 10, 20 }, m.rowView(0));
            assertArrayEquals(new byte[] { 3, 4 }, m.rowView(1)); // unchanged
        }

        @Test
        public void testSetRow_WrongSize() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new byte[] { 1 }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new byte[] { 1, 2, 3 }));
        }

        @Test
        public void testSetRow_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 0 }, { 0, 0, 0 } });
            m.setRow(0, new byte[] { 1, 2, 3 });
            assertArrayEquals(new byte[] { 1, 2, 3 }, m.rowView(0));
            assertArrayEquals(new byte[] { 0, 0, 0 }, m.rowView(1));
        }

        @Test
        public void testSetRow_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.setRow(0, new byte[] { 10, 20 });
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(0, 1));
        }

        @Test
        public void testSetRow_InvalidLength() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 0 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new byte[] { 1, 2 }));
        }

        @Test
        public void testSetRow_InvalidIndex() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setRow(-1, new byte[] { 1, 2 }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.setRow(1, new byte[] { 1, 2 }));
        }

        @Test
        public void testSetRow_InvalidLength_Shape1By2_Validation() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new byte[] { 1 }));
        }
    }

    @Nested
    class SetColumnTests {

        @Test
        public void testSetColumn() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.setColumn(0, new byte[] { 7, 8 });
            Assertions.assertArrayEquals(new byte[] { 7, 8 }, matrix.columnCopy(0));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setColumn(0, new byte[] { 1 }));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setColumn(0, (byte[]) null));
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.setColumn(0, new byte[] { 10, 20 });
            assertArrayEquals(new byte[] { 10, 20 }, m.columnCopy(0));
            assertArrayEquals(new byte[] { 2, 4 }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testSetColumn_WrongSize() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new byte[] { 1 }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new byte[] { 1, 2, 3 }));
        }

        @Test
        public void testSetColumn_Shape3By2_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0 }, { 0, 0 }, { 0, 0 } });
            m.setColumn(0, new byte[] { 1, 2, 3 });
            assertArrayEquals(new byte[] { 1, 2, 3 }, m.columnCopy(0));
            assertArrayEquals(new byte[] { 0, 0, 0 }, m.columnCopy(1));
        }

        @Test
        public void testSetColumn_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.setColumn(0, new byte[] { 10, 20 });
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(1, 0));
        }

        @Test
        public void testAliasedArraySourcesAreSnapshotted() {
            ByteMatrix columnMatrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            columnMatrix.setColumn(1, columnMatrix.rowView(0));
            assertArrayEquals(new byte[] { 3, 2 }, columnMatrix.rowCopy(1));

            ByteMatrix diagonalMatrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            diagonalMatrix.setAntiDiagonal(diagonalMatrix.rowView(0));
            assertArrayEquals(new byte[] { 1, 2, 3 }, diagonalMatrix.antiDiagonalCopy());

            byte[][] backing = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            ByteMatrix fillMatrix = ByteMatrix.wrap(backing);
            fillMatrix.copyFrom(1, 0, backing);
            assertArrayEquals(new byte[] { 3, 4 }, fillMatrix.rowCopy(2));
        }

        @Test
        public void testSetColumn_InvalidLength() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0 }, { 0, 0 } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new byte[] { 1 }));
        }

        @Test
        public void testSetColumn_InvalidIndex() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0 }, { 0, 0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(-1, new byte[] { 1, 2 }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(2, new byte[] { 1, 2 }));
        }
    }

    @Nested
    class UpdateRowTests {

        @Test
        public void testDocumentedCallbackFailuresKeepCompletedUpdates() throws Exception {
            for (int operation = 0; operation < 8; operation++) {
                final int selectedOperation = operation;
                final ByteMatrix matrix = ByteMatrix.wrap(new byte[2][2]);
                final AtomicInteger calls = new AtomicInteger();
                final IllegalStateException failure = new IllegalStateException("callback failed");
                final Throwables.ByteUnaryOperator<IllegalStateException> operator = value -> {
                    if (calls.incrementAndGet() == 2) {
                        throw failure;
                    }

                    return (byte) 1;
                };

                assertSame(failure, assertThrows(IllegalStateException.class, () -> Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                    switch (selectedOperation) {
                        case 0 -> matrix.updateRow(0, operator);
                        case 1 -> matrix.updateColumn(0, operator);
                        case 2 -> matrix.updateMainDiagonal(operator);
                        case 3 -> matrix.updateAntiDiagonal(operator);
                        case 4 -> matrix.updateAll(operator);
                        case 5 -> matrix.updateAll((row, column) -> operator.applyAsByte((byte) 0));
                        case 6 -> matrix.replaceIf(value -> operator.applyAsByte(value) != 0, (byte) 1);
                        case 7 -> matrix.replaceIf((row, column) -> operator.applyAsByte((byte) 0) != 0, (byte) 1);
                        default -> throw new AssertionError(selectedOperation);
                    }
                })));

                assertEquals(2, calls.get(), "operation " + selectedOperation);
                assertArrayEquals(selectedOperation == 3 ? new byte[] { (byte) 0, (byte) 1 } : new byte[] { (byte) 1, (byte) 0 }, matrix.rowCopy(0),
                        "operation " + selectedOperation);
                assertArrayEquals(new byte[] { (byte) 0, (byte) 0 }, matrix.rowCopy(1), "operation " + selectedOperation);
            }
        }

        @Test
        public void testUpdateRow() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.updateRow(0, b -> (byte) (b * 2));
            Assertions.assertArrayEquals(new byte[] { 2, 4, 6 }, matrix.rowView(0));
        }

        @Test
        public void testUpdateRow_Shape2By2_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.updateRow(0, x -> (byte) (x * 2));
            assertArrayEquals(new byte[] { 2, 4 }, m.rowView(0));
            assertArrayEquals(new byte[] { 3, 4 }, m.rowView(1)); // unchanged
        }

        @Test
        public void testUpdateRow_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.updateRow(0, val -> (byte) (val * 2));
            assertArrayEquals(new byte[] { 2, 4, 6 }, m.rowView(0));
            assertArrayEquals(new byte[] { 4, 5, 6 }, m.rowView(1));
        }

        @Test
        public void testUpdateRow_Shape2By3_ArrayValues_PreservesOtherRow() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 0, 0, 0 } });
            m.updateRow(0, val -> (byte) (val * 2));
            assertArrayEquals(new byte[] { 2, 4, 6 }, m.rowView(0));
            assertArrayEquals(new byte[] { 0, 0, 0 }, m.rowView(1));
        }

        @Test
        public void testUpdateRow_MultipleRows() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.updateRow(1, val -> (byte) (val + 10));
            assertArrayEquals(new byte[] { 1, 2 }, m.rowView(0));
            assertArrayEquals(new byte[] { 13, 14 }, m.rowView(1));
            assertArrayEquals(new byte[] { 5, 6 }, m.rowView(2));
        }

        @Test
        public void testUpdateRow_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.updateRow(0, val -> (byte) (val * 2));
            assertEquals(2, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
        }

        @Test
        public void testUpdateRowAndUpdateColumnNullOperator() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.updateRow(0, (Throwables.ByteUnaryOperator<RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.updateColumn(0, (Throwables.ByteUnaryOperator<RuntimeException>) null));
        }

        @Test
        public void testUpdateRowAndUpdateColumnInvalidIndex() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateRow(-1, b -> (byte) (b * 2)));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateColumn(3, b -> (byte) (b + 10)));
        }
    }

    @Nested
    class UpdateColumnTests {

        @Test
        public void testUpdateColumn() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.updateColumn(1, b -> (byte) (b + 10));
            Assertions.assertArrayEquals(new byte[] { 12, 15 }, matrix.columnCopy(1));
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.updateColumn(0, x -> (byte) (x + 10));
            assertArrayEquals(new byte[] { 11, 13 }, m.columnCopy(0));
            assertArrayEquals(new byte[] { 2, 4 }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testUpdateColumn_Shape3By2_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.updateColumn(0, val -> (byte) (val + 10));
            assertArrayEquals(new byte[] { 11, 13, 15 }, m.columnCopy(0));
            assertArrayEquals(new byte[] { 2, 4, 6 }, m.columnCopy(1));
        }

        @Test
        public void testUpdateColumn_Shape3By2_ArrayValues_PreservesOtherColumn() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 0 }, { 2, 0 }, { 3, 0 } });
            m.updateColumn(0, val -> (byte) (val * 2));
            assertArrayEquals(new byte[] { 2, 4, 6 }, m.columnCopy(0));
            assertArrayEquals(new byte[] { 0, 0, 0 }, m.columnCopy(1));
        }

        @Test
        public void testUpdateColumn_MultipleColumns() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.updateColumn(2, val -> (byte) (val + 10));
            assertArrayEquals(new byte[] { 1, 2, 13 }, m.rowView(0));
            assertArrayEquals(new byte[] { 4, 5, 16 }, m.rowView(1));
        }

        @Test
        public void testUpdateColumn_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.updateColumn(0, val -> (byte) (val * 2));
            assertEquals(2, m.get(0, 0));
            assertEquals(6, m.get(1, 0));
        }
    }

    @Nested
    class MainDiagonalCopyTests {

        @Test
        public void testGetLU2RD() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] diagonal = matrix.mainDiagonalCopy();
            Assertions.assertArrayEquals(new byte[] { 1, 5, 9 }, diagonal);

            ByteMatrix nonSquare = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            Assertions.assertArrayEquals(new byte[] { 1 }, nonSquare.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_Shape3By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertArrayEquals(new byte[] { 1, 5, 9 }, m.mainDiagonalCopy());
        }

        @Test
        public void testGetLU2RD_nonSquare() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertArrayEquals(new byte[] { 1 }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_NonSquare_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new byte[] { 1, 5 }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            byte[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new byte[] { 1, 5, 9 }, diag);
        }

        @Test
        public void testMainDiagonalCopy_NonSquare() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertArrayEquals(new byte[] { 1, 4 }, m.mainDiagonalCopy());
        }

        @Test
        public void testmainDiagonalCopy() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            byte[] diagonal = matrix.mainDiagonalCopy();
            assertArrayEquals(new byte[] { 1, 5, 9 }, diagonal);
        }

        @Test
        public void testMainDiagonalCopy_SingleElement() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 42 } });
            assertArrayEquals(new byte[] { 42 }, m.mainDiagonalCopy());
        }
    }

    @Nested
    class SetMainDiagonalTests {

        @Test
        public void testSetLU2RD() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.setMainDiagonal(new byte[] { 10, 11, 12 });
            Assertions.assertEquals(10, matrix.get(0, 0));
            Assertions.assertEquals(11, matrix.get(1, 1));
            Assertions.assertEquals(12, matrix.get(2, 2));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setMainDiagonal(new byte[] { 1, 2 }));
        }

        @Test
        public void testSetMainDiagonal_NonSquare() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            m.setMainDiagonal(new byte[] { 9 });
            assertEquals((byte) 9, m.get(0, 0));
        }

        @Test
        public void testSetMainDiagonal_ArrayTooShort() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new byte[] { 1, 2 }));
        }

        @Test
        public void testSetMainDiagonal_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            m.setMainDiagonal(new byte[] { 1, 2, 3 });
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
            assertEquals(0, m.get(0, 1));
        }

        @Test
        public void testSetMainDiagonal_NonSquare_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[2][3]);
            m.setMainDiagonal(new byte[] { 1, 2 });
            assertArrayEquals(new byte[] { 1, 2 }, m.mainDiagonalCopy());
        }

        @Test
        public void testSetMainDiagonal() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setMainDiagonal(new byte[] { 10, 20, 30 });
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 2));
        }

        @Test
        public void testSetDiagonal_NullArraysOnEmptyMatrixThrow() {
            ByteMatrix m = ByteMatrix.empty();
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(null));
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(null));
        }

        @Test
        public void testSetLU2RD_invalidLength() {
            ByteMatrix m = ByteMatrix.wrap(new byte[3][3]);
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new byte[] { 1, 2 }));
        }

        @Test
        public void testSetMainDiagonal_InvalidLength() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new byte[] { 1 }));
        }
    }

    @Nested
    class UpdateMainDiagonalTests {

        @Test
        public void testUpdateLU2RD() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.updateMainDiagonal(b -> (byte) (b * 2));
            Assertions.assertEquals(2, matrix.get(0, 0));
            Assertions.assertEquals(10, matrix.get(1, 1));
            Assertions.assertEquals(18, matrix.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateMainDiagonal(x -> (byte) (x * 10));
            assertEquals(10, m.get(0, 0));
            assertEquals(50, m.get(1, 1));
            assertEquals(90, m.get(2, 2));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateMainDiagonal_NonSquare() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            m.updateMainDiagonal(x -> (byte) (x * 2));
            assertEquals((byte) 2, m.get(0, 0));
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3_ScalesEveryDiagonalElement() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 0, 0 }, { 0, 2, 0 }, { 0, 0, 3 } });
            m.updateMainDiagonal(val -> (byte) (val * 10));
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal_DoublesValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 0, 0 }, { 0, 2, 0 }, { 0, 0, 3 } });
            m.updateMainDiagonal(val -> (byte) (val * 2));
            assertEquals(2, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
            assertEquals(6, m.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal_AllValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 1, 0 }, { 1, 0, 1 }, { 0, 1, 0 } });
            m.updateMainDiagonal(val -> (byte) 9);
            assertEquals(9, m.get(0, 0));
            assertEquals(9, m.get(1, 1));
            assertEquals(9, m.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateMainDiagonal(val -> (byte) (val * 2));
            assertEquals(2, m.get(0, 0));
            assertEquals(10, m.get(1, 1));
            assertEquals(18, m.get(2, 2));
        }
    }

    @Nested
    class AntiDiagonalCopyTests {

        @Test
        public void testGetRU2LD() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] diagonal = matrix.antiDiagonalCopy();
            Assertions.assertArrayEquals(new byte[] { 3, 5, 7 }, diagonal);
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertArrayEquals(new byte[] { 3, 5, 7 }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertArrayEquals(new byte[] { 2 }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new byte[] { 3, 5 }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            byte[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new byte[] { 3, 5, 7 }, diag);
        }

        @Test
        public void testantiDiagonalCopy() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            byte[] diagonal = matrix.antiDiagonalCopy();
            assertArrayEquals(new byte[] { 3, 5, 7 }, diagonal);
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues_NonzeroAntiDiagonal() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 1 }, { 0, 2, 0 }, { 3, 0, 0 } });
            byte[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new byte[] { 1, 2, 3 }, diag);
        }
    }

    @Nested
    class SetAntiDiagonalTests {

        @Test
        public void testSetRU2LD() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.setAntiDiagonal(new byte[] { 10, 11, 12 });
            Assertions.assertEquals(10, matrix.get(0, 2));
            Assertions.assertEquals(11, matrix.get(1, 1));
            Assertions.assertEquals(12, matrix.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_NonSquare() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            m.setAntiDiagonal(new byte[] { 9 });
            assertEquals((byte) 9, m.get(0, 1));
        }

        @Test
        public void testSetAntiDiagonal_ArrayTooShort() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new byte[] { 1, 2 }));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            m.setAntiDiagonal(new byte[] { 1, 2, 3 });
            assertEquals(1, m.get(0, 2));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 0));
            assertEquals(0, m.get(0, 0));
        }

        @Test
        public void testSetAntiDiagonal() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setAntiDiagonal(new byte[] { 10, 20, 30 });
            assertEquals(10, m.get(0, 2));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_InvalidLength() {
            ByteMatrix m = ByteMatrix.wrap(new byte[3][3]);
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new byte[] { 1, 2 }));
        }
    }

    @Nested
    class UpdateAntiDiagonalTests {

        @Test
        public void testUpdateRU2LD() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.updateAntiDiagonal(b -> (byte) (b + 1));
            Assertions.assertEquals(4, matrix.get(0, 2));
            Assertions.assertEquals(6, matrix.get(1, 1));
            Assertions.assertEquals(8, matrix.get(2, 0));
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateAntiDiagonal(x -> (byte) (x * 10));
            assertEquals(30, m.get(0, 2));
            assertEquals(50, m.get(1, 1));
            assertEquals(70, m.get(2, 0));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateAntiDiagonal_NonSquare() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            m.updateAntiDiagonal(x -> (byte) (x * 2));
            assertEquals((byte) 4, m.get(0, 1));
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3_ScalesEveryDiagonalElement() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 1 }, { 0, 2, 0 }, { 3, 0, 0 } });
            m.updateAntiDiagonal(val -> (byte) (val * 5));
            assertEquals(5, m.get(0, 2));
            assertEquals(10, m.get(1, 1));
            assertEquals(15, m.get(2, 0));
        }

        @Test
        public void testUpdateAntiDiagonal_TriplesValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 1 }, { 0, 2, 0 }, { 3, 0, 0 } });
            m.updateAntiDiagonal(val -> (byte) (val * 3));
            assertEquals(3, m.get(0, 2));
            assertEquals(6, m.get(1, 1));
            assertEquals(9, m.get(2, 0));
        }

        @Test
        public void testUpdateAntiDiagonal_Rectangular() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 1 }, { 2, 0, 0 } });
            m.updateAntiDiagonal(val -> (byte) (val + 10));
            assertArrayEquals(new byte[] { 11, 10 }, m.antiDiagonalCopy());
        }

        @Test
        public void testUpdateAntiDiagonal() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateAntiDiagonal(val -> (byte) (val * 2));
            assertEquals(6, m.get(0, 2));
            assertEquals(10, m.get(1, 1));
            assertEquals(14, m.get(2, 0));
        }
    }

    @Nested
    class UpdateAllTests {

        @Test
        public void testUpdateAll() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.updateAll(b -> (byte) (b * 2));
            Assertions.assertEquals(2, matrix.get(0, 0));
            Assertions.assertEquals(4, matrix.get(0, 1));
            Assertions.assertEquals(6, matrix.get(1, 0));
            Assertions.assertEquals(8, matrix.get(1, 1));
        }

        @Test
        public void testUpdateAllUnarySequentialTallMatrixUsesRowMajorOrder() throws Exception {
            final ByteMatrix matrix = ByteMatrix.wrap(new byte[3][2]);
            final AtomicInteger next = new AtomicInteger();

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> matrix.updateAll(x -> (byte) next.incrementAndGet()));

            assertArrayEquals(new byte[] { 1, 2 }, matrix.rowCopy(0));
            assertArrayEquals(new byte[] { 3, 4 }, matrix.rowCopy(1));
            assertArrayEquals(new byte[] { 5, 6 }, matrix.rowCopy(2));
        }

        @Test
        public void testUpdateAllWithIndices() {
            byte[][] a = { { 0, 0 }, { 0, 0 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.updateAll((i, j) -> (byte) (i + j));
            Assertions.assertEquals(0, matrix.get(0, 0));
            Assertions.assertEquals(1, matrix.get(0, 1));
            Assertions.assertEquals(1, matrix.get(1, 0));
            Assertions.assertEquals(2, matrix.get(1, 1));
        }

        // Original unchanged
        @Test
        public void testUpdateAll_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll(x -> (byte) (x * 2));
            assertEquals(2, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
            assertEquals(6, m.get(1, 0));
            assertEquals(8, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithIndices() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0 }, { 0, 0 } });
            m.updateAll((i, j) -> (byte) (i * 10 + j));
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
            assertEquals(10, m.get(1, 0));
            assertEquals(11, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll((i, j) -> (byte) (i + j));
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
            assertEquals(1, m.get(1, 0));
            assertEquals(2, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithConstant() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll(val -> (byte) 10);
            assertEquals(10, m.get(0, 0));
            assertEquals(10, m.get(0, 1));
            assertEquals(10, m.get(1, 0));
            assertEquals(10, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_UnaryOperator() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll(val -> (byte) (val * 2));
            assertEquals(2, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
            assertEquals(6, m.get(1, 0));
            assertEquals(8, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0 }, { 0, 0 } });
            m.updateAll((i, j) -> (byte) (i + j));
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
            assertEquals(1, m.get(1, 0));
            assertEquals(2, m.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testUpdateAll_ExplicitModesTransformEveryValue(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 } });
                matrix.updateAll(value -> (byte) (value + 10));
                assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 11, (byte) 12, (byte) 13 }, { (byte) 14, (byte) 15, (byte) 16 } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testByteMatrixUpdateAllNullOperator() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix emptyLike = ByteMatrix.wrap(new byte[][] { {}, {} });
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.ByteUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.IntBiFunction<Byte, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateMainDiagonal((Throwables.ByteUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAntiDiagonal((Throwables.ByteUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.BytePredicate<RuntimeException>) null, (byte) 0));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, (byte) 0));

            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateMainDiagonal((Throwables.ByteUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAntiDiagonal((Throwables.ByteUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.BytePredicate<RuntimeException>) null, (byte) 0));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, (byte) 0));
        }

        @Test
        public void testUpdateAll_ZeroColumnsDoNotInvokeOperator() {
            final var matrix = ByteMatrix.wrap(new byte[2][0]);
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
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.replaceIf(b -> b % 2 == 0, (byte) 0);
            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(0, matrix.get(0, 1));
            Assertions.assertEquals(3, matrix.get(0, 2));
            Assertions.assertEquals(0, matrix.get(1, 0));
        }

        @Test
        public void testReplaceIfWithIndices() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.replaceIf((i, j) -> i == j, (byte) 0);
            Assertions.assertEquals(0, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(0, 1));
            Assertions.assertEquals(3, matrix.get(1, 0));
            Assertions.assertEquals(0, matrix.get(1, 1));
        }

        @Test
        public void testReplaceIf_Shape2By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.replaceIf(x -> x > 3, (byte) 0);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(0, 2));
            assertEquals(0, m.get(1, 0)); // was 4
            assertEquals(0, m.get(1, 1)); // was 5
            assertEquals(0, m.get(1, 2)); // was 6
        }

        @Test
        public void testReplaceIf_WithIndices() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.replaceIf((i, j) -> i == j, (byte) 0); // Replace diagonal
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(1, 1));
            assertEquals(0, m.get(2, 2));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testReplaceIf_Predicate() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf(val -> val > 2, (byte) 0);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
            assertEquals(0, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_BiPredicate() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf((i, j) -> i == j, (byte) 0);
            assertEquals(0, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(0, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_NoMatches() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf(val -> val > 10, (byte) 0);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_Predicate_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf(val -> val < 3, (byte) 0);
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_BiPredicate_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[3][3]);
            m.replaceIf((i, j) -> i == j, (byte) 1);
            assertEquals(1, m.get(0, 0));
            assertEquals(0, m.get(0, 1));
            assertEquals(1, m.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testReplaceIf_ExplicitModesReplaceOnlyMatchingValues(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 } });
                matrix.replaceIf(value -> value % 2 == 0, (byte) 9);
                assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 9, (byte) 3 }, { (byte) 9, (byte) 5, (byte) 9 } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }
    }

    @Nested
    class MapTests {

        @Test
        public void testMap() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix doubled = matrix.map(b -> (byte) (b * 2));
            Assertions.assertEquals(2, doubled.get(0, 0));
            Assertions.assertEquals(4, doubled.get(0, 1));
            Assertions.assertEquals(6, doubled.get(1, 0));
            Assertions.assertEquals(8, doubled.get(1, 1));
        }

        @Test
        public void testMap_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix result = m.map(x -> (byte) (x * 2));
            assertEquals(2, result.get(0, 0));
            assertEquals(4, result.get(0, 1));
            assertEquals(6, result.get(1, 0));
            assertEquals(8, result.get(1, 1));

            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testMap_Shape2By2_ScalesAllCellsAndPreservesSource() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix mapped = m.map(val -> (byte) (val * 10));
            assertEquals(10, mapped.get(0, 0));
            assertEquals(20, mapped.get(0, 1));
            assertEquals(30, mapped.get(1, 0));
            assertEquals(40, mapped.get(1, 1));
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testMap_DoublesValuesAndPreservesSource() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix mapped = m.map(val -> (byte) (val * 2));
            assertEquals(2, mapped.get(0, 0));
            assertEquals(4, mapped.get(0, 1));
            assertEquals(6, mapped.get(1, 0));
            assertEquals(8, mapped.get(1, 1));
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testMap_Identity() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix mapped = m.map(val -> val);
            assertEquals(1, mapped.get(0, 0));
            assertEquals(2, mapped.get(0, 1));
            assertNotSame(m, mapped);
        }

        @Test
        public void testMap_Shape2By2_ScalesFirstRowAndPreservesSource() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix doubled = m.map(val -> (byte) (val * 2));
            assertEquals(2, doubled.get(0, 0));
            assertEquals(4, doubled.get(0, 1));
            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testMapNullMapper() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.map((Throwables.ByteUnaryOperator<RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> matrix.mapToObj((Throwables.ByteFunction<String, RuntimeException>) null, String.class));
        }

        @Test
        public void testByteMatrixRowsForZeroColumnMatrix() {
            final ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { {}, {}, {} });
            final List<byte[]> rows = matrix.rowStreams().map(ByteStream::toArray).toList();

            assertEquals(3, rows.size());
            assertArrayEquals(new byte[0], rows.get(0));
            assertArrayEquals(new byte[0], rows.get(1));
            assertArrayEquals(new byte[0], rows.get(2));
        }

        @Test
        public void testNegativeBytes() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { -1, -2 }, { -3, -4 } });

            assertEquals(-1, m.get(0, 0));
            assertEquals(-4, m.get(1, 1));

            ByteMatrix doubled = m.map(x -> (byte) (x * 2));
            assertEquals(-2, doubled.get(0, 0));
            assertEquals(-8, doubled.get(1, 1));
        }
    }

    @Nested
    class MapToObjTests {

        @Test
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void testObjectMappingRuntimeArrayTypeAndStoreFailure() {
            final ByteMatrix source = ByteMatrix.wrap(new byte[] { 1, 1 });
            final Matrix<Integer> boxed = source.mapToObj(value -> 7, int.class);
            assertEquals(Integer.class, boxed.elementType());
            assertEquals(Integer[].class, boxed.rowView(0).getClass());
            assertArrayEquals(new Integer[] { 7, 7 }, boxed.rowCopy(0));

            final Class<Number> narrowType = (Class) Integer.class;
            final AtomicInteger calls = new AtomicInteger();
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                assertThrows(ArrayStoreException.class, () -> source.mapToObj(value -> {
                    if (calls.incrementAndGet() == 1) {
                        return Integer.valueOf(1);
                    }
                    return Double.valueOf(2);
                }, narrowType));
            });
            assertEquals(2, calls.get());
            assertArrayEquals(new byte[] { 1, 1 }, source.rowCopy(0));
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testObjectMappingArrayDimensionLimitBeforeInvokingMapper() {
            Class<?> elementType = Object.class;
            for (int i = 0; i < 253; i++) {
                elementType = elementType.arrayType();
            }
            final Class<Object> largestElementType = (Class<Object>) elementType;
            final Class<Object> tooDeep = (Class<Object>) elementType.arrayType();
            final ByteMatrix empty = new ByteMatrix(new byte[0][], 3);
            final Matrix<Object> mapped = empty.mapToObj(value -> null, largestElementType);
            assertEquals(0, mapped.rowCount());
            assertEquals(3, mapped.columnCount());
            assertEquals(largestElementType, mapped.elementType());

            final AtomicInteger calls = new AtomicInteger();
            assertThrows(IllegalArgumentException.class, () -> empty.mapToObj(value -> {
                calls.incrementAndGet();
                return null;
            }, tooDeep));
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.wrap(new byte[] { 1 }).mapToObj(value -> {
                calls.incrementAndGet();
                return null;
            }, tooDeep));
            assertEquals(0, calls.get());
        }

        @Test
        public void testMapToObj() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            Matrix<String> stringMatrix = matrix.mapToObj(b -> "Value: " + b, String.class);
            Assertions.assertEquals("Value: 1", stringMatrix.get(0, 0));
            Assertions.assertEquals("Value: 4", stringMatrix.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> result = m.mapToObj(x -> "val:" + x, String.class);
            assertEquals("val:1", result.get(0, 0));
            assertEquals("val:4", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_AddsStringPrefixToEveryCell() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> mapped = m.mapToObj(val -> "V" + val, String.class);
            assertEquals("V1", mapped.get(0, 0));
            assertEquals("V2", mapped.get(0, 1));
            assertEquals("V3", mapped.get(1, 0));
            assertEquals("V4", mapped.get(1, 1));
        }

        @Test
        public void testMapToObj_DecimalStringValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> mapped = m.mapToObj(val -> String.valueOf(val), String.class);
            assertEquals("1", mapped.get(0, 0));
            assertEquals("2", mapped.get(0, 1));
            assertEquals("3", mapped.get(1, 0));
            assertEquals("4", mapped.get(1, 1));
        }

        @Test
        public void testMapToObj_WithComplexType() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> mapped = m.mapToObj(val -> val * 10, Integer.class);
            assertEquals(10, mapped.get(0, 0));
            assertEquals(20, mapped.get(0, 1));
            assertEquals(30, mapped.get(1, 0));
            assertEquals(40, mapped.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_AddsStringPrefixToFirstRow() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> stringMatrix = m.mapToObj(val -> "B" + val, String.class);
            assertEquals("B1", stringMatrix.get(0, 0));
            assertEquals("B2", stringMatrix.get(0, 1));
        }
    }

    @Nested
    class FillTests {

        @Test
        public void testFill() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.fill((byte) 5);
            Assertions.assertEquals(5, matrix.get(0, 0));
            Assertions.assertEquals(5, matrix.get(0, 1));
            Assertions.assertEquals(5, matrix.get(1, 0));
            Assertions.assertEquals(5, matrix.get(1, 1));
        }

        @Test
        public void testFill_WithValue() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.fill((byte) 99);
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals(99, m.get(i, j));
                }
            }
        }

        @Test
        public void testFill_SingleValue() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0 }, { 0, 0 } });
            m.fill((byte) 5);
            assertEquals(5, m.get(0, 0));
            assertEquals(5, m.get(0, 1));
            assertEquals(5, m.get(1, 0));
            assertEquals(5, m.get(1, 1));
        }

        @Test
        public void testFill_SingleValue_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.fill((byte) 0);
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
            assertEquals(0, m.get(1, 1));
        }
    }

    @Nested
    class CopyFromTests {

        @Test
        public void testCopyFrom() {
            byte[][] a = { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[][] b = { { 1, 2 }, { 3, 4 } };
            matrix.copyFrom(b);
            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(0, 1));
            Assertions.assertEquals(3, matrix.get(1, 0));
            Assertions.assertEquals(4, matrix.get(1, 1));
            Assertions.assertEquals(0, matrix.get(2, 2)); // unchanged
        }

        @Test
        public void testCopyFromWithIndices() {
            byte[][] a = { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[][] b = { { 1, 2 } };
            matrix.copyFrom(1, 1, b);
            Assertions.assertEquals(0, matrix.get(0, 0)); // unchanged
            Assertions.assertEquals(1, matrix.get(1, 1));
            Assertions.assertEquals(2, matrix.get(1, 2));

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(-1, 0, b));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(0, -1, b));
        }

        @Test
        public void testCopyFrom_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            byte[][] patch = { { 1, 2 }, { 3, 4 } };
            m.copyFrom(patch);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
            assertEquals(0, m.get(2, 2)); // unchanged
        }

        @Test
        public void testCopyFromAtPosition() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            byte[][] patch = { { 1, 2 }, { 3, 4 } };
            m.copyFrom(1, 1, patch);
            assertEquals(0, m.get(0, 0)); // unchanged
            assertEquals(1, m.get(1, 1));
            assertEquals(2, m.get(1, 2));
            assertEquals(3, m.get(2, 1));
            assertEquals(4, m.get(2, 2));
        }

        @Test
        public void testCopyFrom_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0 }, { 0, 0 } });
            m.copyFrom(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testCopyFrom_WithOffset() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            m.copyFrom(1, 1, new byte[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(1, 1));
            assertEquals(4, m.get(2, 2));
        }

        @Test
        public void testCopyFrom_WithPartialArray() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            m.copyFrom(0, 0, new byte[][] { { 1, 2 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(0, m.get(0, 2));
            assertEquals(0, m.get(1, 0));
        }

        @Test
        public void testCopyFrom_Shape2By2_PreservesCellsOutsideCopiedRegion() {
            ByteMatrix m = ByteMatrix.wrap(new byte[3][3]);
            m.copyFrom(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
            assertEquals(0, m.get(2, 2));
        }

        @Test
        public void testCopyFromWithPosition() {
            ByteMatrix m = ByteMatrix.wrap(new byte[4][4]);
            m.copyFrom(1, 1, new byte[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(1, 1));
            assertEquals(2, m.get(1, 2));
            assertEquals(3, m.get(2, 1));
            assertEquals(4, m.get(2, 2));
        }

        @Test
        public void testCopyFrom_FullOverwrite() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0 }, { 0, 0 } });
            m.copyFrom(new byte[][] { { 10, 20 }, { 30, 40 } });
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(0, 1));
            assertEquals(30, m.get(1, 0));
            assertEquals(40, m.get(1, 1));
        }

        @Test
        public void testCopyFrom_PartialOverwrite() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            m.copyFrom(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(0, m.get(0, 2));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
            assertEquals(0, m.get(2, 0));
        }

        @Test
        public void testCopyFrom_WithOffset_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            m.copyFrom(1, 1, new byte[][] { { 5, 6 }, { 7, 8 } });
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(1, 0));
            assertEquals(5, m.get(1, 1));
            assertEquals(6, m.get(1, 2));
            assertEquals(7, m.get(2, 1));
            assertEquals(8, m.get(2, 2));
        }

        @Test
        public void testCopyFrom_EmptySource() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.copyFrom(new byte[0][0]);
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testCopyFrom_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            byte[][] patch = { { 1, 2 }, { 3, 4 } };
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
        }

        @Test
        public void testCopyFrom_WithOffset_InvalidPosition() {
            ByteMatrix m = ByteMatrix.wrap(new byte[2][2]);
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(3, 3, new byte[][] { { 1, 2 }, { 3, 4 } }));
        }

        @Test
        public void testCopyFrom_NegativeIndexThrows() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, new byte[][] { { 1 } }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(0, -1, new byte[][] { { 1 } }));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0", "3, 0", "0, -1", "0, 3" })
        public void testCopyFrom_RejectsOffsetsOutsideDestination(final int row, final int column) {
            final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(row, column, new byte[][] { { (byte) 1 } }));
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } }), matrix);
        }
    }

    @Nested
    class CopyTests {

        @Test
        public void testCopy() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);
            ByteMatrix copy = matrix.copy();

            Assertions.assertEquals(matrix.rowCount(), copy.rowCount());
            Assertions.assertEquals(matrix.columnCount(), copy.columnCount());
            Assertions.assertEquals(1, copy.get(0, 0));
            Assertions.assertEquals(4, copy.get(1, 1));

            copy.set(0, 0, (byte) 9);
            Assertions.assertEquals(1, matrix.get(0, 0)); // original unchanged
        }

        @Test
        public void testCopy_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals(1, copy.get(0, 0));

            // Modify copy shouldn't affect original
            copy.set(0, 0, (byte) 99);
            assertEquals(1, m.get(0, 0));
            assertEquals(99, copy.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2_FirstRowAndIndependentStorage() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix copy = m.copy();
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(1, copy.get(0, 0));
            assertEquals(2, copy.get(0, 1));
            copy.set(0, 0, (byte) 99);
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testCopy_ResultMutationPreservesSource() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix copy = m.copy();
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(1, copy.get(0, 0));
            assertEquals(2, copy.get(0, 1));
            copy.set(0, 0, (byte) 10);
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2_DimensionsAndIndependentStorage() {
            ByteMatrix original = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix copy = original.copy();
            assertEquals(original.rowCount(), copy.rowCount());
            assertEquals(original.columnCount(), copy.columnCount());
            assertEquals(1, copy.get(0, 0));
            // Modify copy
            copy.set(0, 0, (byte) 99);
            assertEquals(1, original.get(0, 0)); // Original unchanged
        }

        @Test
        public void testCopy_EmptyMatrix() {
            ByteMatrix m = ByteMatrix.empty();
            ByteMatrix copy = m.copy();
            assertTrue(copy.isEmpty());
        }

        @Test
        public void testZeroRowResultMethodsPreserveLogicalColumnCount() {
            ByteMatrix matrix = new ByteMatrix(new byte[0][], 3);
            ByteMatrix other = new ByteMatrix(new byte[0][], 3);

            assertShape(0, 3, matrix.copy());
            assertShape(0, 3, matrix.copyRows(0, 0));
            assertShape(0, 2, matrix.copyRegion(0, 0, 1, 3));
            assertShape(0, 3, matrix.map(value -> (byte) (value + 1)));
            assertShape(0, 3, matrix.mapToObj(Byte::valueOf, Byte.class));
            assertShape(0, 5, matrix.resize(0, 5));
            assertShape(0, 5, matrix.pad(0, 0, 1, 1));
            assertShape(3, 0, matrix.rotate90());
            assertShape(0, 3, matrix.rotate180());
            assertShape(3, 0, matrix.rotate270());
            assertShape(3, 0, matrix.transpose());
            assertShape(0, 4, matrix.reshapeAndPad(0, 4));
            assertShape(0, 6, matrix.repeatElements(2, 2));
            assertShape(0, 6, matrix.repeatMatrix(2, 2));
            assertShape(0, 3, matrix.stackVertically(other));
            assertShape(0, 6, matrix.stackHorizontally(other));
            assertShape(0, 3, matrix.add(other));
            assertShape(0, 3, matrix.addWidened(other));
            assertShape(0, 3, matrix.addExact(other));
            assertShape(0, 3, matrix.subtract(other));
            assertShape(0, 3, matrix.subtractWidened(other));
            assertShape(0, 3, matrix.subtractExact(other));
            ByteMatrix right = ByteMatrix.wrap(new byte[3][2]);
            assertShape(0, 2, matrix.matrixMultiply(right));
            assertShape(0, 2, matrix.matrixMultiplyWidened(right));
            assertShape(0, 2, matrix.matrixMultiplyExact(right));
            assertShape(0, 3, matrix.boxed());
            assertShape(0, 3, matrix.toIntMatrix());
            assertShape(0, 3, matrix.toLongMatrix());
            assertShape(0, 3, matrix.toFloatMatrix());
            assertShape(0, 3, matrix.toDoubleMatrix());
            assertShape(0, 3, matrix.zipWith(other, (left, rightValue) -> left));
            assertShape(0, 3, matrix.zipWith(other, other, (first, second, third) -> first));
        }
    }

    @Nested
    class CopyRowsTests {

        @Test
        public void testCopyRows() {
            byte[][] a = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);
            ByteMatrix copy = matrix.copyRows(0, 2);

            Assertions.assertEquals(2, copy.rowCount());
            Assertions.assertEquals(2, copy.columnCount());
            Assertions.assertEquals(1, copy.get(0, 0));
            Assertions.assertEquals(4, copy.get(1, 1));

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(-1, 2));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(0, 4));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(2, 1));
        }

        @Test
        public void testCopyRows_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ByteMatrix subset = m.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertEquals(4, subset.get(0, 0));
            assertEquals(9, subset.get(1, 2));
        }

        @Test
        public void testCopyRows_Shape3By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            ByteMatrix copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(3, copy.get(0, 0));
            assertEquals(4, copy.get(0, 1));
        }

        @Test
        public void testCopyRangesEmpty_ReturnsEmptyMatrix() {
            // An empty row slice keeps the column count (0 x N) and an empty column slice keeps the row
            // count (N x 0); only an empty range in BOTH dimensions collapses to 0 x 0.
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            ByteMatrix empty = m.copyRows(0, 0);
            Assertions.assertEquals(0, empty.rowCount());
            Assertions.assertEquals(3, empty.columnCount());

            ByteMatrix emptyRows = m.copyRegion(1, 1, 0, 3);
            Assertions.assertEquals(0, emptyRows.rowCount());
            Assertions.assertEquals(3, emptyRows.columnCount());

            ByteMatrix emptyCols = m.copyRegion(0, 3, 1, 1);
            Assertions.assertEquals(3, emptyCols.rowCount());
            Assertions.assertEquals(0, emptyCols.columnCount());
        }

        @Test
        public void testCopyRows_SingleRow() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            ByteMatrix copy = m.copyRows(1, 2);
            assertEquals(1, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(3, copy.get(0, 0));
            assertEquals(4, copy.get(0, 1));
        }

        @Test
        public void testCopyRows_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(2, 1));
        }

        @Test
        public void testCopyRows_InvalidRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 2));
        }
    }

    @Nested
    class CopyRegionTests {

        // An empty row slice keeps the column count (0 x N) and an empty column slice keeps the row
        // count (N x 0); only an empty range in BOTH dimensions collapses to 0 x 0.
        @Test
        public void testCopyRegion() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);
            ByteMatrix copy = matrix.copyRegion(0, 2, 1, 3);

            Assertions.assertEquals(2, copy.rowCount());
            Assertions.assertEquals(2, copy.columnCount());
            Assertions.assertEquals(2, copy.get(0, 0));
            Assertions.assertEquals(3, copy.get(0, 1));
            Assertions.assertEquals(5, copy.get(1, 0));
            Assertions.assertEquals(6, copy.get(1, 1));
        }

        @Test
        public void testCopyRegion_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ByteMatrix submatrix = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals(2, submatrix.get(0, 0));
            assertEquals(6, submatrix.get(1, 1));
        }

        @Test
        public void testCopyRegion_Shape3By3_LowerRightRegion() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ByteMatrix copy = m.copyRegion(1, 3, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(5, copy.get(0, 0));
            assertEquals(6, copy.get(0, 1));
            assertEquals(8, copy.get(1, 0));
            assertEquals(9, copy.get(1, 1));
        }

        @Test
        public void testCopyRegion_Shape3By3_UpperRightRegion() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ByteMatrix copy = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(2, copy.get(0, 0)); // From (0,1)
            assertEquals(3, copy.get(0, 1)); // From (0,2)
        }

        @Test
        public void testCopyRegion_SingleCell() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ByteMatrix copy = m.copyRegion(0, 1, 1, 2);
            assertEquals(1, copy.rowCount());
            assertEquals(1, copy.columnCount());
            assertEquals(2, copy.get(0, 0));
        }

        @Test
        public void testCopyRegion_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, 0, 3));
        }

        @Test
        public void testCopyRegion_InvalidRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 1, -1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 1, 0, 3));
        }
    }

    @Nested
    class ResizeTests {

        @Test
        public void testResize_Default() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix extended = matrix.resize(3, 3);
            // Result: [[1, 2, 0], [3, 4, 0], [0, 0, 0]]
            assertEquals((byte) 1, extended.get(0, 0));
            assertEquals((byte) 2, extended.get(0, 1));
            assertEquals((byte) 0, extended.get(0, 2));
            assertEquals((byte) 3, extended.get(1, 0));
            assertEquals((byte) 4, extended.get(1, 1));
            assertEquals((byte) 0, extended.get(1, 2));
            assertEquals((byte) 0, extended.get(2, 0));
            assertEquals((byte) 0, extended.get(2, 1));
            assertEquals((byte) 0, extended.get(2, 2));
        }

        @Test
        public void testResize_WithFill() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix extended = matrix.resize(3, 4, (byte) 9);
            // Result: [[1, 2, 9, 9], [3, 4, 9, 9], [9, 9, 9, 9]]
            assertEquals((byte) 1, extended.get(0, 0));
            assertEquals((byte) 2, extended.get(0, 1));
            assertEquals((byte) 9, extended.get(0, 2));
            assertEquals((byte) 9, extended.get(0, 3));
            assertEquals((byte) 3, extended.get(1, 0));
            assertEquals((byte) 4, extended.get(1, 1));
            assertEquals((byte) 9, extended.get(1, 2));
            assertEquals((byte) 9, extended.get(1, 3));
            for (int j = 0; j < 4; j++) {
                assertEquals((byte) 9, extended.get(2, j));
            }

            // Truncate
            ByteMatrix truncated = matrix.resize(1, 1, (byte) 0);
            assertEquals((byte) 1, truncated.get(0, 0));
        }

        @Test
        public void testResize_Expand() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix resized = m.resize(3, 3);
            assertEquals(3, resized.rowCount());
            assertEquals(3, resized.columnCount());
            assertEquals(1, resized.get(0, 0));
            assertEquals(2, resized.get(0, 1));
            assertEquals(0, resized.get(0, 2));
            assertEquals(3, resized.get(1, 0));
            assertEquals(0, resized.get(2, 0));
            assertEquals(0, resized.get(2, 2));
        }

        @Test
        public void testResize_Shrink() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ByteMatrix resized = m.resize(2, 2);
            assertEquals(2, resized.rowCount());
            assertEquals(2, resized.columnCount());
            assertEquals(1, resized.get(0, 0));
            assertEquals(2, resized.get(0, 1));
            assertEquals(4, resized.get(1, 0));
            assertEquals(5, resized.get(1, 1));
        }

        @Test
        public void testResize_WithDefaultValue() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1 } });
            ByteMatrix resized = m.resize(2, 3, (byte) 99);
            assertEquals(2, resized.rowCount());
            assertEquals(3, resized.columnCount());
            assertEquals(1, resized.get(0, 0));
            assertEquals(99, resized.get(0, 1));
            assertEquals(99, resized.get(0, 2));
            assertEquals(99, resized.get(1, 0));
            assertEquals(99, resized.get(1, 1));
            assertEquals(99, resized.get(1, 2));
        }

        @Test
        public void testResize() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix extended = matrix.resize(3, 3);
            Assertions.assertEquals(3, extended.rowCount());
            Assertions.assertEquals(3, extended.columnCount());
            Assertions.assertEquals(1, extended.get(0, 0));
            Assertions.assertEquals(4, extended.get(1, 1));
            Assertions.assertEquals(0, extended.get(2, 2));

            ByteMatrix truncated = matrix.resize(1, 1);
            Assertions.assertEquals(1, truncated.rowCount());
            Assertions.assertEquals(1, truncated.columnCount());
            Assertions.assertEquals(1, truncated.get(0, 0));
        }

        @Test
        public void testResizeWithDefaultValue() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix extended = matrix.resize(3, 3, (byte) 9);
            Assertions.assertEquals(9, extended.get(2, 2));
            Assertions.assertEquals(9, extended.get(0, 2));
            Assertions.assertEquals(9, extended.get(2, 0));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.resize(-1, 2, (byte) 0));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.resize(2, -1, (byte) 0));
        }

        // New cells are 0
        @Test
        public void testResize_Larger() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix extended = m.resize(4, 4);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(4, extended.get(1, 1));
            assertEquals(0, extended.get(3, 3)); // new cells are 0
        }

        // Check new values
        @Test
        public void testResize_Smaller() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ByteMatrix truncated = m.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
            assertEquals(1, truncated.get(0, 0));
            assertEquals(5, truncated.get(1, 1));
        }

        @Test
        public void testResize_WithDefaultValue_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix extended = m.resize(3, 3, (byte) -1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(-1, extended.get(2, 2)); // new cell
        }

        @Test
        public void testResize_WithDefaultValue_Shape1By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix extended = m.resize(2, 3);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(2, extended.get(0, 1));
            assertEquals(0, extended.get(0, 2));
            assertEquals(0, extended.get(1, 0));
        }

        @Test
        public void testResize_WithCustomDefaultValue() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix extended = m.resize(2, 3, (byte) 9);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(2, extended.get(0, 1));
            assertEquals(9, extended.get(0, 2));
            assertEquals(9, extended.get(1, 0));
        }

        @Test
        public void testResize_NoExtension() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix extended = m.resize(1, 2);
            assertEquals(1, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(2, extended.get(0, 1));
        }

        // Result: [[9, 9, 9, 9, 9, 9], [9, 9, 1, 2, 9, 9], [9, 9, 9, 9, 9, 9]]
        @Test
        public void testResize_TwoParams() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix extended = m.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(0, extended.get(2, 2));
        }

        @Test
        public void testResize_TwoParamsWithDefault() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix extended = m.resize(3, 3, (byte) 9);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(9, extended.get(2, 2));
        }

        @Test
        public void testResize_ToEmpty() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix resized = m.resize(0, 0);
            assertEquals(0, resized.rowCount());
            assertTrue(resized.isEmpty());
        }

        @Test
        public void testZeroRowShapeWithMaxColumnCountIsAllocationSafe() {
            ByteMatrix m = new ByteMatrix(new byte[0][], 1);
            assertShape(0, Integer.MAX_VALUE, m.resize(0, Integer.MAX_VALUE));
            assertShape(0, Integer.MAX_VALUE, m.reshapeAndPad(0, Integer.MAX_VALUE));
        }

        @Test
        public void testResize_GrowsRowsWhileTrimmingColumns() {
            final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } });
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1 }, { (byte) 3 }, { (byte) 9 } }), matrix.resize(3, 1, (byte) 9));
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } }), matrix);
        }

        @Test
        public void testResize_NegativeThrows() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> m.resize(1, -1));
        }

        @Test
        public void testResize_WithNegativeDimensions() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 3, (byte) 0));
            assertThrows(IllegalArgumentException.class, () -> m.resize(3, -1, (byte) 0));
        }

        @Test
        public void testResize_InvalidSize() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });

            ByteMatrix extended = m.resize(1, 1);
            assertEquals(1, extended.rowCount());
            assertEquals(1, extended.columnCount());
        }
    }

    @Nested
    class PadTests {

        @Test
        public void testPadInAllDirections() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix extended = matrix.pad(1, 1, 1, 1);
            Assertions.assertEquals(4, extended.rowCount());
            Assertions.assertEquals(4, extended.columnCount());
            Assertions.assertEquals(1, extended.get(1, 1));
            Assertions.assertEquals(4, extended.get(2, 2));
            Assertions.assertEquals(0, extended.get(0, 0));
        }

        @Test
        public void testPadInAllDirectionsWithDefaultValue() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix extended = matrix.pad(1, 1, 1, 1, (byte) 9);
            Assertions.assertEquals(9, extended.get(0, 0));
            Assertions.assertEquals(9, extended.get(0, 3));
            Assertions.assertEquals(9, extended.get(3, 0));
            Assertions.assertEquals(9, extended.get(3, 3));
            Assertions.assertEquals(1, extended.get(1, 1));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.pad(-1, 0, 0, 0, (byte) 0));
        }

        @Test
        public void testPad_Directional() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ByteMatrix extended = m.pad(1, 1, 2, 2);
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
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ByteMatrix extended = m.pad(1, 1, 1, 1, (byte) -1);
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());

            // Check original values
            assertEquals(1, extended.get(1, 1));

            // Check new values
            assertEquals(-1, extended.get(0, 0));
            assertEquals(-1, extended.get(4, 4));
        }

        @Test
        public void testPad_WithDirections() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 5 } });
            ByteMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(5, extended.get(1, 1));
            assertEquals(0, extended.get(0, 0));
        }

        @Test
        public void testPad_WithDirectionsAndValue() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 5 } });
            ByteMatrix extended = m.pad(1, 1, 1, 1, (byte) 7);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(5, extended.get(1, 1));
            assertEquals(7, extended.get(0, 0));
        }

        // matrix is now: [[3, 2, 1], [6, 5, 4]]
        @Test
        public void testPad_FourParams() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1, extended.get(1, 1));
        }

        @Test
        public void testPad_FourParamsWithDefault() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix extended = m.pad(1, 1, 1, 1, (byte) 9);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(9, extended.get(0, 0));
            assertEquals(9, extended.get(3, 3));
        }

        @Test
        public void testPad_Default() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix extended = matrix.pad(1, 1, 1, 1);
            // Result: [[0, 0, 0, 0], [0, 1, 2, 0], [0, 0, 0, 0]]
            assertEquals(3, extended.rowCount());
            assertEquals(4, extended.columnCount());
            for (int j = 0; j < 4; j++) {
                assertEquals((byte) 0, extended.get(0, j));
                assertEquals((byte) 0, extended.get(2, j));
            }
            assertEquals((byte) 0, extended.get(1, 0));
            assertEquals((byte) 1, extended.get(1, 1));
            assertEquals((byte) 2, extended.get(1, 2));
            assertEquals((byte) 0, extended.get(1, 3));
        }

        @Test
        public void testPad_WithFill() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix padded = matrix.pad(1, 1, 2, 2, (byte) 9);
            // Result: [[9, 9, 9, 9, 9, 9], [9, 9, 1, 2, 9, 9], [9, 9, 9, 9, 9, 9]]
            assertEquals(3, padded.rowCount());
            assertEquals(6, padded.columnCount());
            assertEquals((byte) 9, padded.get(0, 0));
            assertEquals((byte) 9, padded.get(1, 0));
            assertEquals((byte) 9, padded.get(1, 1));
            assertEquals((byte) 1, padded.get(1, 2));
            assertEquals((byte) 2, padded.get(1, 3));
            assertEquals((byte) 9, padded.get(1, 4));
            assertEquals((byte) 9, padded.get(1, 5));
        }

        @Test
        public void testPad_SingleAxisFillPreservesExistingValues() {
            final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } });
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 9 }, { (byte) 3, (byte) 4, (byte) 9 } }),
                    matrix.pad(0, 0, 0, 1, (byte) 9));
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 9, (byte) 1, (byte) 2 }, { (byte) 9, (byte) 3, (byte) 4 } }),
                    matrix.pad(0, 0, 1, 0, (byte) 9));
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 }, { (byte) 9, (byte) 9 } }),
                    matrix.pad(0, 1, 0, 0, (byte) 9));
            final var copy = matrix.pad(0, 0, 0, 0, (byte) 9);
            assertEquals(matrix, copy);
            assertNotSame(matrix, copy);
        }

        @Test
        public void testPad_DirectionalWithNegativeValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.pad(-1, 1, 1, 1, (byte) 0));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0, 0, 0", "0, -1, 0, 0", "0, 0, -1, 0", "0, 0, 0, -1", "2147483647, 0, 0, 0", "0, 0, 0, 2147483647" })
        public void testPad_RejectsNegativeOrOverflowingDimensions(final int top, final int bottom, final int left, final int right) {
            final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.pad(top, bottom, left, right));
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } }), matrix);
        }
    }

    @Nested
    class FlipHorizontallyInPlaceTests {

        @Test
        public void testReverseH() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.flipHorizontallyInPlace();
            Assertions.assertEquals(3, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(0, 1));
            Assertions.assertEquals(1, matrix.get(0, 2));
            Assertions.assertEquals(6, matrix.get(1, 0));
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.flipHorizontallyInPlace();
            assertArrayEquals(new byte[] { 3, 2, 1 }, m.rowView(0));
            assertArrayEquals(new byte[] { 6, 5, 4 }, m.rowView(1));
        }

        @Test
        public void testFlipHorizontallyInPlace() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.flipHorizontallyInPlace();
            assertEquals(2, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
        }

        @Test
        public void testflipHorizontallyInPlace() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            matrix.flipHorizontallyInPlace();
            // matrix is now: [[3, 2, 1], [6, 5, 4]]
            assertArrayEquals(new byte[] { 3, 2, 1 }, matrix.rowView(0));
            assertArrayEquals(new byte[] { 6, 5, 4 }, matrix.rowView(1));
        }

        @Test
        public void testFlipInPlaceHorizontally() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.flipHorizontallyInPlace();
            assertEquals(3, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(1, m.get(0, 2));
            assertEquals(6, m.get(1, 0));
            assertEquals(5, m.get(1, 1));
            assertEquals(4, m.get(1, 2));
        }

        @Test
        public void testFlipHorizontallyInPlace_SingleRow() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            m.flipHorizontallyInPlace();
            assertArrayEquals(new byte[] { 3, 2, 1 }, m.rowView(0));
        }

        @Test
        public void testFlipInPlaceHorizontally_SingleColumn() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1 }, { 2 } });
            m.flipHorizontallyInPlace();
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 0));
        }
    }

    @Nested
    class FlipVerticallyInPlaceTests {

        @Test
        public void testReverseV() {
            byte[][] a = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            matrix.flipVerticallyInPlace();
            Assertions.assertEquals(5, matrix.get(0, 0));
            Assertions.assertEquals(6, matrix.get(0, 1));
            Assertions.assertEquals(3, matrix.get(1, 0));
            Assertions.assertEquals(1, matrix.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape3By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertEquals(5, m.get(0, 0));
            assertEquals(6, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(1, m.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.flipVerticallyInPlace();
            assertEquals(3, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
            assertEquals(1, m.get(1, 0));
            assertEquals(2, m.get(1, 1));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape3By2_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertArrayEquals(new byte[] { 5, 3, 1 }, m.columnCopy(0));
            assertArrayEquals(new byte[] { 6, 4, 2 }, m.columnCopy(1));
        }

        @Test
        public void testFlipVerticallyInPlace() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            matrix.flipVerticallyInPlace();
            // matrix is now: [[5, 6], [3, 4], [1, 2]]
            assertArrayEquals(new byte[] { 5, 6 }, matrix.rowView(0));
            assertArrayEquals(new byte[] { 3, 4 }, matrix.rowView(1));
            assertArrayEquals(new byte[] { 1, 2 }, matrix.rowView(2));
        }

        @Test
        public void testFlipInPlaceVertically() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertEquals(5, m.get(0, 0));
            assertEquals(6, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
            assertEquals(1, m.get(2, 0));
            assertEquals(2, m.get(2, 1));
        }

        @Test
        public void testFlipVerticallyInPlace_SingleColumn() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1 }, { 2 }, { 3 } });
            m.flipVerticallyInPlace();
            assertArrayEquals(new byte[] { 3, 2, 1 }, m.columnCopy(0));
        }

        @Test
        public void testFlipInPlaceVertically_SingleRow() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            m.flipVerticallyInPlace();
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(0, 2));
        }
    }

    @Nested
    class FlipHorizontallyTests {

        @Test
        public void testFlipH() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix flipped = matrix.flipHorizontally();
            Assertions.assertEquals(3, flipped.get(0, 0));
            Assertions.assertEquals(1, flipped.get(0, 2));
            Assertions.assertEquals(1, matrix.get(0, 0)); // original unchanged
        }

        @Test
        public void testFlipHorizontally_Shape2By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ByteMatrix flipped = m.flipHorizontally();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(2, flipped.get(0, 1));
            assertEquals(1, flipped.get(0, 2));

            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testFlipHorizontally_Shape2By3_IndependentStorage_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ByteMatrix flipped = m.flipHorizontally();
            assertEquals(2, flipped.rowCount());
            assertEquals(3, flipped.columnCount());
            assertArrayEquals(new byte[] { 3, 2, 1 }, flipped.rowView(0));
            assertArrayEquals(new byte[] { 6, 5, 4 }, flipped.rowView(1));
            assertNotSame(m, flipped);
        }

        @Test
        public void testFlipHorizontally() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix flipped = m.flipHorizontally();
            assertEquals(2, flipped.get(0, 0));
            assertEquals(1, flipped.get(0, 1));
        }
    }

    @Nested
    class FlipVerticallyTests {

        @Test
        public void testFlipVertically() {
            byte[][] a = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix flipped = matrix.flipVertically();
            Assertions.assertEquals(5, flipped.get(0, 0));
            Assertions.assertEquals(1, flipped.get(2, 0));
            Assertions.assertEquals(1, matrix.get(0, 0)); // original unchanged
        }

        @Test
        public void testFlipVertically_Shape3By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            ByteMatrix flipped = m.flipVertically();
            assertEquals(5, flipped.get(0, 0));
            assertEquals(3, flipped.get(1, 0));
            assertEquals(1, flipped.get(2, 0));

            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testFlipVertically_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix flipped = m.flipVertically();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(4, flipped.get(0, 1));
            assertEquals(1, flipped.get(1, 0));
            assertEquals(2, flipped.get(1, 1));
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testFlipVertically_Shape3By2_IndependentStorage_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            ByteMatrix flipped = m.flipVertically();
            assertEquals(3, flipped.rowCount());
            assertEquals(2, flipped.columnCount());
            assertArrayEquals(new byte[] { 5, 3, 1 }, flipped.columnCopy(0));
            assertArrayEquals(new byte[] { 6, 4, 2 }, flipped.columnCopy(1));
            assertNotSame(m, flipped);
        }
    }

    @Nested
    class Rotate90Tests {

        @Test
        public void testRotate90() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix rotated = matrix.rotate90();
            Assertions.assertEquals(2, rotated.rowCount());
            Assertions.assertEquals(2, rotated.columnCount());
            Assertions.assertEquals(3, rotated.get(0, 0));
            Assertions.assertEquals(1, rotated.get(0, 1));
            Assertions.assertEquals(4, rotated.get(1, 0));
            Assertions.assertEquals(2, rotated.get(1, 1));
        }

        @Test
        public void testRotate90_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3, rotated.get(0, 0));
            assertEquals(1, rotated.get(0, 1));
            assertEquals(4, rotated.get(1, 0));
            assertEquals(2, rotated.get(1, 1));
        }

        @Test
        public void testRotate90_NonSquare() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ByteMatrix rotated = m.rotate90();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
        }
    }

    @Nested
    class Rotate180Tests {

        @Test
        public void testRotate180() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix rotated = matrix.rotate180();
            Assertions.assertEquals(2, rotated.rowCount());
            Assertions.assertEquals(2, rotated.columnCount());
            Assertions.assertEquals(4, rotated.get(0, 0));
            Assertions.assertEquals(3, rotated.get(0, 1));
            Assertions.assertEquals(2, rotated.get(1, 0));
            Assertions.assertEquals(1, rotated.get(1, 1));
        }

        @Test
        public void testRotate180_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix rotated = m.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4, rotated.get(0, 0));
            assertEquals(3, rotated.get(0, 1));
            assertEquals(2, rotated.get(1, 0));
            assertEquals(1, rotated.get(1, 1));
        }

        @Test
        public void testRotate180_Shape2By2_EveryCellValue() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix rotated = m.rotate180();
            assertEquals(4, rotated.get(0, 0));
            assertEquals(3, rotated.get(0, 1));
            assertEquals(2, rotated.get(1, 0));
            assertEquals(1, rotated.get(1, 1));
        }

        @Test
        public void testRotate180AndRotate90Twice_PreserveNx0Shape() {
            ByteMatrix m = ByteMatrix.wrap(new byte[3][0]);

            ByteMatrix via180 = m.rotate180();
            assertEquals(3, via180.rowCount());
            assertEquals(0, via180.columnCount());

            ByteMatrix viaRotate90Twice = m.rotate90().rotate90();
            assertEquals(3, viaRotate90Twice.rowCount());
            assertEquals(0, viaRotate90Twice.columnCount());
        }
    }

    @Nested
    class Rotate270Tests {

        @Test
        public void testRotate270() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix rotated = matrix.rotate270();
            Assertions.assertEquals(2, rotated.rowCount());
            Assertions.assertEquals(2, rotated.columnCount());
            Assertions.assertEquals(2, rotated.get(0, 0));
            Assertions.assertEquals(4, rotated.get(0, 1));
            Assertions.assertEquals(1, rotated.get(1, 0));
            Assertions.assertEquals(3, rotated.get(1, 1));
        }

        @Test
        public void testRotate270_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(2, rotated.get(0, 0));
            assertEquals(4, rotated.get(0, 1));
            assertEquals(1, rotated.get(1, 0));
            assertEquals(3, rotated.get(1, 1));
        }
    }

    @Nested
    class TransposeTests {

        @Test
        public void testTranspose() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix transposed = matrix.transpose();
            Assertions.assertEquals(3, transposed.rowCount());
            Assertions.assertEquals(2, transposed.columnCount());
            Assertions.assertEquals(1, transposed.get(0, 0));
            Assertions.assertEquals(4, transposed.get(0, 1));
            Assertions.assertEquals(2, transposed.get(1, 0));
            Assertions.assertEquals(5, transposed.get(1, 1));
        }

        @Test
        public void testTranspose_Shape2By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ByteMatrix transposed = m.transpose();
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
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(3, transposed.get(0, 1));
            assertEquals(2, transposed.get(1, 0));
            assertEquals(4, transposed.get(1, 1));
        }

        @Test
        public void testRotateTransposeAndConvertTallMatrix() {
            // Create a tall matrix (rows > columnCount) - 5 rows × 3 columnCount
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 }, { 10, 11, 12 }, { 13, 14, 15 } });

            // Test rotate90() with tall matrix
            ByteMatrix rotated90 = m.rotate90();
            assertEquals(3, rotated90.rowCount());
            assertEquals(5, rotated90.columnCount());
            assertEquals(13, rotated90.get(0, 0));
            assertEquals(1, rotated90.get(0, 4));

            // Test rotate270() with tall matrix
            ByteMatrix rotated270 = m.rotate270();
            assertEquals(3, rotated270.rowCount());
            assertEquals(5, rotated270.columnCount());
            assertEquals(3, rotated270.get(0, 0));

            // Test transpose() with tall matrix
            ByteMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(5, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(15, transposed.get(2, 4));

            // Test boxed() with tall matrix
            Matrix<Byte> boxed = m.boxed();
            assertEquals(5, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Byte.valueOf((byte) 1), boxed.get(0, 0));

            // Test toLongMatrix() with tall matrix
            LongMatrix longMat = m.toLongMatrix();
            assertEquals(5, longMat.rowCount());
            assertEquals(3, longMat.columnCount());
            assertEquals(1L, longMat.get(0, 0));
            assertEquals(15L, longMat.get(4, 2));

            // Test toFloatMatrix() with tall matrix
            FloatMatrix floatMat = m.toFloatMatrix();
            assertEquals(5, floatMat.rowCount());
            assertEquals(3, floatMat.columnCount());
            assertEquals(1.0f, floatMat.get(0, 0), 0.001f);

            // Test toDoubleMatrix() with tall matrix
            DoubleMatrix doubleMat = m.toDoubleMatrix();
            assertEquals(5, doubleMat.rowCount());
            assertEquals(3, doubleMat.columnCount());
            assertEquals(1.0, doubleMat.get(0, 0), 0.001);
        }

        @Test
        public void testTranspose_Nx0_PreservesSwappedShape() {
            ByteMatrix t = ByteMatrix.wrap(new byte[3][0]).transpose();
            assertEquals(0, t.rowCount());
            assertEquals(3, t.columnCount());
        }
    }

    @Nested
    class ReshapeAndPadTests {

        @Test
        public void testReshape_Empty() {
            ByteMatrix empty = ByteMatrix.empty();
            ByteMatrix reshaped = empty.reshapeAndPad(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }

        @Test
        public void testReshape_InvalidSize() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            ByteMatrix reshaped = m.reshapeAndPad(2, 2);
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
        }

        @ParameterizedTest
        @CsvSource({ "-1, 2", "2, -1", "1, 1" })
        public void testReshapeAndPad_RejectsNegativeAndInsufficientShapes(final int rows, final int columns) {
            final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.reshapeAndPad(rows, columns));
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } }), matrix);
        }
    }

    @Nested
    class RepeatElementsTests {

        @Test
        public void testRepeatElements() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix repeated = matrix.repeatElements(2, 3);
            Assertions.assertEquals(4, repeated.rowCount());
            Assertions.assertEquals(6, repeated.columnCount());
            Assertions.assertEquals(1, repeated.get(0, 0));
            Assertions.assertEquals(1, repeated.get(0, 2));
            Assertions.assertEquals(1, repeated.get(1, 0));
            Assertions.assertEquals(2, repeated.get(0, 3));
            Assertions.assertEquals(4, repeated.get(3, 5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(0, 1));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(1, 0));
        }

        @Test
        public void testRepeatElements_Shape1By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix repeated = m.repeatElements(2, 3);
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
        }

        @Test
        public void testRepeatElements_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix result = m.repeatElements(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(1, result.get(0, 1));
            assertEquals(1, result.get(1, 0));
            assertEquals(1, result.get(1, 1));
            assertEquals(2, result.get(0, 2));
        }

        @Test
        public void testRepeatElements_Shape2By2_RepeatedBlockAndAdjacentValue() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix repeated = m.repeatElements(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(1, repeated.get(0, 1));
            assertEquals(1, repeated.get(1, 0));
            assertEquals(1, repeated.get(1, 1));
            assertEquals(2, repeated.get(0, 2));
        }

        @Test
        public void testRepeatElements_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } });
            final var result = matrix.repeatElements(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, (byte) 0);
            assertEquals((byte) 1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatElements_OnlyColumnsRepeated() {
            final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 3 }, { (byte) 3, (byte) 1 } });
            final var repeated = matrix.repeatElements(1, 2);
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 1, (byte) 3, (byte) 3 }, { (byte) 3, (byte) 3, (byte) 1, (byte) 1 } }), repeated);
            repeated.set(0, 0, (byte) 3);
            assertEquals((byte) 1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatElementsOverflow() {
            int largeSize = 50000;
            ByteMatrix m = ByteMatrix.wrap(new byte[largeSize][2]);

            IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> m.repeatElements(50000, 1));
            assertTrue(ex1.getMessage().contains("row count overflow"));

            ByteMatrix m2 = ByteMatrix.wrap(new byte[2][largeSize]);
            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> m2.repeatElements(1, 50000));
            assertTrue(ex2.getMessage().contains("column count overflow"));
        }

        @Test
        public void testRepeatElements_SingleRepeat() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix repeated = m.repeatElements(1, 1);
            assertEquals(1, repeated.rowCount());
            assertEquals(2, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(2, repeated.get(0, 1));
        }

        @Test
        public void testRepeatElements_InvalidArguments() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @Test
        public void testRepeatElements_InvalidRepeats() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatElements_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(rows, columns));
        }
    }

    @Nested
    class RepeatMatrixTests {

        @Test
        public void testRepeatMatrix() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix repeated = matrix.repeatMatrix(2, 3);
            Assertions.assertEquals(4, repeated.rowCount());
            Assertions.assertEquals(6, repeated.columnCount());
            Assertions.assertEquals(1, repeated.get(0, 0));
            Assertions.assertEquals(1, repeated.get(0, 2));
            Assertions.assertEquals(1, repeated.get(0, 4));
            Assertions.assertEquals(1, repeated.get(2, 0));
            Assertions.assertEquals(4, repeated.get(3, 5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(0, 1));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(1, 0));
        }

        @Test
        public void testRepeatMatrix_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix repeated = m.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals(1, repeated.get(0, 0));
            assertEquals(2, repeated.get(0, 1));
            assertEquals(1, repeated.get(0, 2)); // repeat starts
            assertEquals(2, repeated.get(0, 3));

            assertEquals(3, repeated.get(1, 0));
            assertEquals(4, repeated.get(1, 1));

            // Check vertical repeat
            assertEquals(1, repeated.get(2, 0)); // vertical repeat starts
            assertEquals(2, repeated.get(2, 1));
        }

        @Test
        public void testRepeatMatrix_Shape2By2_LastTileCorner() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix repeated = m.repeatMatrix(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(2, repeated.get(0, 1));
            assertEquals(4, repeated.get(3, 3));
        }

        @Test
        public void testRepeatMatrix_TwoByTwoTiling() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix repeated = m.repeatMatrix(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(2, repeated.get(0, 1));
            assertEquals(1, repeated.get(2, 2));
        }

        @Test
        public void testRepeatMatrix_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } });
            final var result = matrix.repeatMatrix(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, (byte) 0);
            assertEquals((byte) 1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_OnlyColumnsRepeated() {
            final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } });
            final var repeated = matrix.repeatMatrix(1, 2);
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4, (byte) 3, (byte) 4 } }), repeated);
            repeated.set(0, 0, (byte) 3);
            assertEquals((byte) 1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_Shape2By2_VerticalTileBoundary() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix result = m.repeatMatrix(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(2, result.get(0, 1));
            assertEquals(1, result.get(2, 0));
            assertEquals(2, result.get(2, 1));
        }

        @Test
        public void testRepeatMatrix_EmptyDimensionsDoNotTraverseRepeats() {
            ByteMatrix zeroRows = new ByteMatrix(new byte[0][], 2);
            ByteMatrix zeroColumns = ByteMatrix.wrap(new byte[2][0]);

            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                ByteMatrix repeatedRows = zeroRows.repeatMatrix(Integer.MAX_VALUE, 1);
                assertEquals(0, repeatedRows.rowCount());
                assertEquals(2, repeatedRows.columnCount());

                ByteMatrix repeatedColumns = zeroColumns.repeatMatrix(1, Integer.MAX_VALUE);
                assertEquals(2, repeatedColumns.rowCount());
                assertEquals(0, repeatedColumns.columnCount());
            });

            assertThrows(IllegalArgumentException.class, () -> zeroRows.repeatMatrix(1, Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> zeroColumns.repeatMatrix(Integer.MAX_VALUE, 1));
        }

        @Test
        public void testRepeatMatrixOverflow() {
            int largeSize = 50000;
            ByteMatrix m = ByteMatrix.wrap(new byte[largeSize][2]);

            IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(50000, 1));
            assertTrue(ex1.getMessage().contains("row count overflow"));

            ByteMatrix m2 = ByteMatrix.wrap(new byte[2][largeSize]);
            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> m2.repeatMatrix(1, 50000));
            assertTrue(ex2.getMessage().contains("column count overflow"));
        }

        @Test
        public void testRepeatMatrix_SingleRepeat() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix repeated = m.repeatMatrix(1, 1);
            assertEquals(1, repeated.rowCount());
            assertEquals(2, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidArguments() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidRepeats() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatMatrix_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(rows, columns));
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } }), matrix);
        }
    }

    @Nested
    class FlattenTests {

        @Test
        public void testFlatten() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteList flat = matrix.flatten();
            Assertions.assertEquals(6, flat.size());
            Assertions.assertEquals(1, flat.get(0));
            Assertions.assertEquals(2, flat.get(1));
            Assertions.assertEquals(3, flat.get(2));
            Assertions.assertEquals(4, flat.get(3));
            Assertions.assertEquals(5, flat.get(4));
            Assertions.assertEquals(6, flat.get(5));
        }

        @Test
        public void testFlatten_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ByteList flat = m.flatten();
            assertEquals(9, flat.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, flat.get(i));
            }
        }

        @Test
        public void testFlatten_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteList flattened = m.flatten();
            assertEquals(4, flattened.size());
            assertEquals(1, flattened.get(0));
            assertEquals(2, flattened.get(1));
            assertEquals(3, flattened.get(2));
            assertEquals(4, flattened.get(3));
        }

        @Test
        public void testFlatten_Shape2By2_EveryListElement() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteList flat = m.flatten();
            assertEquals(4, flat.size());
            assertEquals(1, flat.get(0));
            assertEquals(2, flat.get(1));
            assertEquals(3, flat.get(2));
            assertEquals(4, flat.get(3));
        }

        // ByteMatrix.java: System.out.println(matrix.toString());   // Output: [[1, 2], [3, 4]]
        @Test
        public void testflatten() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteList list = matrix.flatten();
            // Returns ByteList containing [1, 2, 3, 4]
            assertEquals(4, list.size());
            assertEquals((byte) 1, list.get(0));
            assertEquals((byte) 2, list.get(1));
            assertEquals((byte) 3, list.get(2));
            assertEquals((byte) 4, list.get(3));
        }

        @Test
        public void testFlatten_Empty() {
            ByteMatrix empty = ByteMatrix.empty();
            ByteList flat = empty.flatten();
            assertTrue(flat.isEmpty());
        }

        @Test
        public void testFlatten_EmptyMatrix() {
            ByteMatrix m = ByteMatrix.empty();
            ByteList flattened = m.flatten();
            assertEquals(0, flattened.size());
        }
    }

    @Nested
    class MutateViaFlatArrayTests {

        @Test
        public void testFlatOp() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            List<Byte> collected = new ArrayList<>();
            matrix.mutateViaFlatArray(row -> {
                for (byte b : row) {
                    collected.add(b);
                }
            });

            Assertions.assertEquals(4, collected.size());
            Assertions.assertTrue(collected.contains((byte) 1));
            Assertions.assertTrue(collected.contains((byte) 4));
        }

        @Test
        public void testMutateViaFlatArray_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Integer> sums = new ArrayList<>();
            m.mutateViaFlatArray(row -> {
                int sum = 0;
                for (byte val : row) {
                    sum += val;
                }
                sums.add(sum);
            });
            assertEquals(1, sums.size());
            assertEquals(45, sums.get(0).intValue());
        }

        @Test
        public void testMutateViaFlatArray_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            AtomicInteger count = new AtomicInteger(0);
            m.mutateViaFlatArray(row -> count.addAndGet(row.length));
            assertEquals(4, count.get());
        }

        @Test
        public void testMutateViaFlatArray() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            m.mutateViaFlatArray(arr -> {
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = (byte) (arr[i] * 2);
                }
            });
            assertEquals(2, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
        }

        @Test
        public void testmutateViaFlatArray() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 5, 3 }, { 4, 1 } });
            matrix.mutateViaFlatArray(arr -> java.util.Arrays.sort(arr));
            // matrix is now [[1, 3], [4, 5]]
            assertEquals((byte) 1, matrix.get(0, 0));
            assertEquals((byte) 3, matrix.get(0, 1));
            assertEquals((byte) 4, matrix.get(1, 0));
            assertEquals((byte) 5, matrix.get(1, 1));
        }
    }

    @Nested
    class StackVerticallyTests {

        @Test
        public void testVstack() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            byte[][] b = { { 5, 6 }, { 7, 8 } };
            ByteMatrix matrixA = ByteMatrix.wrap(a);
            ByteMatrix matrixB = ByteMatrix.wrap(b);

            ByteMatrix stacked = matrixA.stackVertically(matrixB);
            Assertions.assertEquals(4, stacked.rowCount());
            Assertions.assertEquals(2, stacked.columnCount());
            Assertions.assertEquals(1, stacked.get(0, 0));
            Assertions.assertEquals(5, stacked.get(2, 0));

            ByteMatrix differentCols = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.stackVertically(differentCols));
        }

        @Test
        public void testStackVertically_Shape2By3() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 7, 8, 9 }, { 10, 11, 12 } });
            ByteMatrix stacked = m1.stackVertically(m2);

            assertEquals(4, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(7, stacked.get(2, 0));
            assertEquals(12, stacked.get(3, 2));
        }

        @Test
        public void testStackVertically_DifferentColumnCounts() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_Shape1By2() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 3, 4 } });
            ByteMatrix stacked = m1.stackVertically(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(2, stacked.get(0, 1));
            assertEquals(3, stacked.get(1, 0));
            assertEquals(4, stacked.get(1, 1));
        }

        @Test
        public void testStackVertically_IncompatibleColumns() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 3, 4 } });
            ByteMatrix stacked = m1.stackVertically(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(3, stacked.get(1, 0));
        }

        @Test
        public void testStackVertically_DifferentCols() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 3, 4, 5 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testByteMatrixStackAndMultiplyRejectNullMatrix() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
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
        public void testStackHorizontally() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            byte[][] b = { { 5, 6 }, { 7, 8 } };
            ByteMatrix matrixA = ByteMatrix.wrap(a);
            ByteMatrix matrixB = ByteMatrix.wrap(b);

            ByteMatrix stacked = matrixA.stackHorizontally(matrixB);
            Assertions.assertEquals(2, stacked.rowCount());
            Assertions.assertEquals(4, stacked.columnCount());
            Assertions.assertEquals(1, stacked.get(0, 0));
            Assertions.assertEquals(5, stacked.get(0, 2));

            ByteMatrix differentRows = ByteMatrix.wrap(new byte[][] { { 1 }, { 2 }, { 3 } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.stackHorizontally(differentRows));
        }

        @Test
        public void testStackHorizontally_Shape2By2() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(5, stacked.get(0, 2));
            assertEquals(8, stacked.get(1, 3));
        }

        @Test
        public void testStackHorizontally_DifferentRowCounts() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_Shape2By1() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1 }, { 3 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 2 }, { 4 } });
            ByteMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(2, stacked.get(0, 1));
            assertEquals(3, stacked.get(1, 0));
            assertEquals(4, stacked.get(1, 1));
        }

        @Test
        public void testStackHorizontally_IncompatibleRows() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1 }, { 3 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 2 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_DifferentRows() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_InvalidRows() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 2 }, { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_RejectsLogicalWidthOverflowWithoutAllocation() {
            final var huge = ByteMatrix.wrap(new byte[0][0]).resize(0, Integer.MAX_VALUE);
            final var extra = ByteMatrix.wrap(new byte[0][0]).resize(0, 1);
            assertThrows(IllegalArgumentException.class, () -> huge.stackHorizontally(extra));
            assertEquals(Integer.MAX_VALUE, huge.columnCount());
        }
    }

    @Nested
    class AddTests {

        @Test
        public void testAdd() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            byte[][] b = { { 5, 6 }, { 7, 8 } };
            ByteMatrix matrixA = ByteMatrix.wrap(a);
            ByteMatrix matrixB = ByteMatrix.wrap(b);

            ByteMatrix sum = matrixA.add(matrixB);
            Assertions.assertEquals(6, sum.get(0, 0));
            Assertions.assertEquals(8, sum.get(0, 1));
            Assertions.assertEquals(10, sum.get(1, 0));
            Assertions.assertEquals(12, sum.get(1, 1));

            ByteMatrix differentShape = ByteMatrix.wrap(new byte[][] { { 1 } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.add(differentShape));
        }

        @Test
        public void testAdd_Shape2By2() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix sum = m1.add(m2);

            assertEquals(6, sum.get(0, 0));
            assertEquals(8, sum.get(0, 1));
            assertEquals(10, sum.get(1, 0));
            assertEquals(12, sum.get(1, 1));
        }

        @Test
        public void testAdd_DifferentDimensions() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testAdd_Shape2By2_EveryCellSum() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix result = m1.add(m2);
            assertEquals(6, result.get(0, 0));
            assertEquals(8, result.get(0, 1));
            assertEquals(10, result.get(1, 0));
            assertEquals(12, result.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testAdd_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 } });
                final var right = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 } });
                assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 2, (byte) 4, (byte) 6 }, { (byte) 8, (byte) 10, (byte) 12 } }), left.add(right));
                assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 } }), left);
                assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testByteOverflow() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { Byte.MAX_VALUE, 1 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, Byte.MAX_VALUE } });

            // Addition overflow wraps around
            ByteMatrix sum = m1.add(m2);
            assertTrue(sum.get(0, 0) < 0); // Overflow
        }
    }

    @Nested
    class SubtractTests {

        @Test
        public void testSubtract() {
            byte[][] a = { { 5, 6 }, { 7, 8 } };
            byte[][] b = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrixA = ByteMatrix.wrap(a);
            ByteMatrix matrixB = ByteMatrix.wrap(b);

            ByteMatrix diff = matrixA.subtract(matrixB);
            Assertions.assertEquals(4, diff.get(0, 0));
            Assertions.assertEquals(4, diff.get(0, 1));
            Assertions.assertEquals(4, diff.get(1, 0));
            Assertions.assertEquals(4, diff.get(1, 1));

            ByteMatrix differentShape = ByteMatrix.wrap(new byte[][] { { 1 } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.subtract(differentShape));
        }

        // Regression: matrixMultiply must build its result via ByteMatrix.wrap(result) (not the raw
        // constructor) so an empty product yields the shared EMPTY singleton, and must call
        // checkNonNegativeShape before allocation for consistency with the other
        // result-allocating methods (resize/reshape/transpose/rotate).
        @Test
        public void testSubtract_Shape2By2() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix diff = m1.subtract(m2);

            assertEquals(4, diff.get(0, 0));
            assertEquals(4, diff.get(0, 1));
            assertEquals(4, diff.get(1, 0));
            assertEquals(4, diff.get(1, 1));
        }

        @Test
        public void testSubtract_DifferentDimensions() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testSubtract_Shape2By2_EveryCellDifference() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix result = m1.subtract(m2);
            assertEquals(4, result.get(0, 0));
            assertEquals(4, result.get(0, 1));
            assertEquals(4, result.get(1, 0));
            assertEquals(4, result.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testSubtract_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = ByteMatrix.wrap(new byte[][] { { (byte) 7, (byte) 8, (byte) 9 }, { (byte) 10, (byte) 11, (byte) 12 } });
                final var right = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 } });
                assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 6, (byte) 6, (byte) 6 }, { (byte) 6, (byte) 6, (byte) 6 } }), left.subtract(right));
                assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 7, (byte) 8, (byte) 9 }, { (byte) 10, (byte) 11, (byte) 12 } }), left);
                assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }
    }

    @Nested
    class SubtractExactTests {

        @Test
        public void testSubtractExact_ValidRectangularOperandsPreserveValues() {
            final var left = ByteMatrix.wrap(new byte[][] { { (byte) 7, (byte) 8, (byte) 9 }, { (byte) 10, (byte) 11, (byte) 12 } });
            final var right = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 } });
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 6, (byte) 6, (byte) 6 }, { (byte) 6, (byte) 6, (byte) 6 } }), left.subtractExact(right));
            assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 7, (byte) 8, (byte) 9 }, { (byte) 10, (byte) 11, (byte) 12 } }), left);
        }
    }

    @Nested
    class MatrixMultiplyTests {

        @Test
        public void testMultiply() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            byte[][] b = { { 5, 6 }, { 7, 8 } };
            ByteMatrix matrixA = ByteMatrix.wrap(a);
            ByteMatrix matrixB = ByteMatrix.wrap(b);

            ByteMatrix product = matrixA.matrixMultiply(matrixB);
            Assertions.assertEquals(2, product.rowCount());
            Assertions.assertEquals(2, product.columnCount());
            Assertions.assertEquals(19, product.get(0, 0));
            Assertions.assertEquals(22, product.get(0, 1));
            Assertions.assertEquals(43, product.get(1, 0));
            Assertions.assertEquals(50, product.get(1, 1));

            ByteMatrix incompatible = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.matrixMultiply(incompatible));
        }

        @Test
        public void testMatrixMultiply_Shape2By2() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix product = m1.matrixMultiply(m2);

            assertEquals(19, product.get(0, 0)); // 1*5 + 2*7
            assertEquals(22, product.get(0, 1)); // 1*6 + 2*8
            assertEquals(43, product.get(1, 0)); // 3*5 + 4*7
            assertEquals(50, product.get(1, 1)); // 3*6 + 4*8
        }

        @Test
        public void testMatrixMultiply_IncompatibleDimensions() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testMatrixMultiply_RectangularMatrices() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } }); // 1x3
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 4 }, { 5 }, { 6 } }); // 3x1
            ByteMatrix product = m1.matrixMultiply(m2);

            assertEquals(1, product.rowCount());
            assertEquals(1, product.columnCount());
            assertEquals(32, product.get(0, 0)); // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        }

        @Test
        public void testMatrixMultiply() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 2, 3 }, { 4, 5 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix result = m1.matrixMultiply(m2);
            assertEquals(11, result.get(0, 0)); // 2*1 + 3*3 = 11
            assertEquals(16, result.get(0, 1)); // 2*2 + 3*4 = 16
            assertEquals(19, result.get(1, 0)); // 4*1 + 5*3 = 19
            assertEquals(28, result.get(1, 1)); // 4*2 + 5*4 = 28
        }

        @ParameterizedTest(name = "{0}: rectangular multiplication")
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testMatrixMultiply_ExplicitModesPreserveInputs(final ParallelMode mode) {
            final ByteMatrix left = ByteMatrix.wrap(new byte[][] { { 2, -1, 3 }, { 4, 0, -2 } });
            final ByteMatrix right = ByteMatrix.wrap(new byte[][] { { 1, 5 }, { 2, -3 }, { -1, 4 } });
            final ByteMatrix leftBefore = left.copy();
            final ByteMatrix rightBefore = right.copy();

            Matrices.runWithParallelMode(mode, () -> {
                final ByteMatrix product = left.matrixMultiply(right);
                assertEquals(2, product.rowCount());
                assertEquals(2, product.columnCount());
                assertArrayEquals(new byte[] { -3, 25 }, product.rowCopy(0));
                assertArrayEquals(new byte[] { 6, 12 }, product.rowCopy(1));
            });

            assertEquals(leftBefore, left);
            assertEquals(rightBefore, right);
            assertEquals(ParallelMode.FORCE_OFF, Matrices.getParallelMode());
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testMatrixMultiply_ExplicitModesComputeRectangularProduct(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 }, { (byte) 5, (byte) 6 } });
                final var right = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 } });
                assertEquals(
                        ByteMatrix.wrap(
                                new byte[][] { { (byte) 9, (byte) 12, (byte) 15 }, { (byte) 19, (byte) 26, (byte) 33 }, { (byte) 29, (byte) 40, (byte) 51 } }),
                        left.matrixMultiply(right));
                assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 }, { (byte) 5, (byte) 6 } }), left);
                assertEquals(ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testMatrixMultiply_EmptyProductReturnsCanonicalEmpty() {
            // Regression: matrixMultiply must build its result via ByteMatrix.wrap(result) (not the raw
            // constructor) so an empty product yields the shared EMPTY singleton, and must call
            // checkNonNegativeShape before allocation for consistency with the other
            // result-allocating methods (resize/reshape/transpose/rotate).
            ByteMatrix product = ByteMatrix.empty().matrixMultiply(ByteMatrix.empty());
            assertSame(ByteMatrix.empty(), product);
            assertTrue(product.isEmpty());
        }
    }

    @Nested
    class MatrixMultiplyWidenedTests {

        @Test
        public void testMatrixMultiplyWidened_DotProductBeyondIntRangeIsExact() {
            // 140000 * (-128 * -128) = 2_293_760_000 > Integer.MAX_VALUE: the documented long result must not wrap.
            final int n = 140_000;
            final byte[] row = new byte[n];
            java.util.Arrays.fill(row, Byte.MIN_VALUE);
            final ByteMatrix left = ByteMatrix.wrap(new byte[][] { row });
            final ByteMatrix right = left.transpose();

            assertEquals(2_293_760_000L, left.matrixMultiplyWidened(right).get(0, 0));

            final ArithmeticException overflow = assertThrows(ArithmeticException.class, () -> left.matrixMultiplyExact(right));
            assertEquals("byte overflow: 2293760000", overflow.getMessage());
            assertEquals((byte) 2_293_760_000L, left.matrixMultiply(right).get(0, 0));
        }

        @Test
        public void testMatrixMultiplyWidened_RejectsIncompatibleInnerDimensions() {
            final var left = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 } });
            final var right = ByteMatrix.wrap(new byte[][] { { (byte) 3, (byte) 4 } });
            assertThrows(IllegalArgumentException.class, () -> left.matrixMultiplyWidened(right));
        }
    }

    @Nested
    class MatrixMultiplyExactTests {

        @Test
        public void testMatrixMultiplyExact_RejectsIncompatibleInnerDimensions() {
            final var left = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 } });
            final var right = ByteMatrix.wrap(new byte[][] { { (byte) 3, (byte) 4 } });
            assertThrows(IllegalArgumentException.class, () -> left.matrixMultiplyExact(right));
        }
    }

    @Nested
    class BoxedTests {

        @Test
        public void testBoxed() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            Matrix<Byte> boxed = matrix.boxed();
            Assertions.assertEquals(2, boxed.rowCount());
            Assertions.assertEquals(2, boxed.columnCount());
            Assertions.assertEquals(Byte.valueOf((byte) 1), boxed.get(0, 0));
            Assertions.assertEquals(Byte.valueOf((byte) 4), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Matrix<Byte> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Byte.valueOf((byte) 1), boxed.get(0, 0));
            assertEquals(Byte.valueOf((byte) 6), boxed.get(1, 2));
        }

        @Test
        public void testBoxed_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Byte> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Byte.valueOf((byte) 1), boxed.get(0, 0));
            assertEquals(Byte.valueOf((byte) 2), boxed.get(0, 1));
            assertEquals(Byte.valueOf((byte) 3), boxed.get(1, 0));
            assertEquals(Byte.valueOf((byte) 4), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By2_FirstRowBoxedValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Byte> boxed = m.boxed();
            assertEquals((byte) 1, boxed.get(0, 0));
            assertEquals((byte) 2, boxed.get(0, 1));
        }

        @Test
        public void testBoxed_EmptyMatrix() {
            ByteMatrix m = ByteMatrix.empty();
            Matrix<Byte> boxed = m.boxed();
            assertTrue(boxed.isEmpty());
        }
    }

    @Nested
    class ToIntMatrixTests {

        @Test
        public void testToIntMatrix() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            IntMatrix intMatrix = matrix.toIntMatrix();
            Assertions.assertEquals(2, intMatrix.rowCount());
            Assertions.assertEquals(2, intMatrix.columnCount());
            Assertions.assertEquals(1, intMatrix.get(0, 0));
            Assertions.assertEquals(4, intMatrix.get(1, 1));
        }

        @Test
        public void testToIntMatrix_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix intMatrix = m.toIntMatrix();
            assertEquals(2, intMatrix.rowCount());
            assertEquals(2, intMatrix.columnCount());
            assertEquals(1, intMatrix.get(0, 0));
            assertEquals(4, intMatrix.get(1, 1));
        }

        @Test
        public void testToIntMatrix_Shape2By2_FirstRowConvertedValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix intMatrix = m.toIntMatrix();
            assertEquals(1, intMatrix.get(0, 0));
            assertEquals(2, intMatrix.get(0, 1));
        }
    }

    @Nested
    class ToLongMatrixTests {

        @Test
        public void testToLongMatrix() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            LongMatrix longMatrix = matrix.toLongMatrix();
            Assertions.assertEquals(2, longMatrix.rowCount());
            Assertions.assertEquals(2, longMatrix.columnCount());
            Assertions.assertEquals(1L, longMatrix.get(0, 0));
            Assertions.assertEquals(4L, longMatrix.get(1, 1));
        }

        @Test
        public void testToLongMatrix_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix longMatrix = m.toLongMatrix();
            assertEquals(2, longMatrix.rowCount());
            assertEquals(2, longMatrix.columnCount());
            assertEquals(1L, longMatrix.get(0, 0));
            assertEquals(4L, longMatrix.get(1, 1));
        }

        @Test
        public void testToLongMatrix_Shape2By2_FirstRowConvertedValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix longMatrix = m.toLongMatrix();
            assertEquals(1L, longMatrix.get(0, 0));
            assertEquals(2L, longMatrix.get(0, 1));
        }
    }

    @Nested
    class ToFloatMatrixTests {

        @Test
        public void testToFloatMatrix() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            FloatMatrix floatMatrix = matrix.toFloatMatrix();
            Assertions.assertEquals(2, floatMatrix.rowCount());
            Assertions.assertEquals(2, floatMatrix.columnCount());
            Assertions.assertEquals(1.0f, floatMatrix.get(0, 0));
            Assertions.assertEquals(4.0f, floatMatrix.get(1, 1));
        }

        @Test
        public void testToFloatMatrix_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            FloatMatrix floatMatrix = m.toFloatMatrix();
            assertEquals(2, floatMatrix.rowCount());
            assertEquals(2, floatMatrix.columnCount());
            assertEquals(1.0f, floatMatrix.get(0, 0), 0.0001f);
            assertEquals(4.0f, floatMatrix.get(1, 1), 0.0001f);
        }

        @Test
        public void testToFloatMatrix_Shape2By2_FirstRowConvertedValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            FloatMatrix floatMatrix = m.toFloatMatrix();
            assertEquals(1.0f, floatMatrix.get(0, 0));
            assertEquals(2.0f, floatMatrix.get(0, 1));
        }
    }

    @Nested
    class ToDoubleMatrixTests {

        @Test
        public void testToDoubleMatrix() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            DoubleMatrix doubleMatrix = matrix.toDoubleMatrix();
            Assertions.assertEquals(2, doubleMatrix.rowCount());
            Assertions.assertEquals(2, doubleMatrix.columnCount());
            Assertions.assertEquals(1.0, doubleMatrix.get(0, 0));
            Assertions.assertEquals(4.0, doubleMatrix.get(1, 1));
        }

        @Test
        public void testToDoubleMatrix_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix doubleMatrix = m.toDoubleMatrix();
            assertEquals(2, doubleMatrix.rowCount());
            assertEquals(2, doubleMatrix.columnCount());
            assertEquals(1.0, doubleMatrix.get(0, 0), 0.0001);
            assertEquals(4.0, doubleMatrix.get(1, 1), 0.0001);
        }

        @Test
        public void testToDoubleMatrix_Shape2By2_FirstRowConvertedValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix doubleMatrix = m.toDoubleMatrix();
            assertEquals(1.0, doubleMatrix.get(0, 0));
            assertEquals(2.0, doubleMatrix.get(0, 1));
        }
    }

    @Nested
    class ZipWithTests {

        @Test
        public void testZipWith() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            byte[][] b = { { 5, 6 }, { 7, 8 } };
            ByteMatrix matrixA = ByteMatrix.wrap(a);
            ByteMatrix matrixB = ByteMatrix.wrap(b);

            ByteMatrix result = matrixA.zipWith(matrixB, (x, y) -> (byte) (x * y));
            Assertions.assertEquals(5, result.get(0, 0));
            Assertions.assertEquals(12, result.get(0, 1));
            Assertions.assertEquals(21, result.get(1, 0));
            Assertions.assertEquals(32, result.get(1, 1));

            ByteMatrix differentShape = ByteMatrix.wrap(new byte[][] { { 1 } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.zipWith(differentShape, (x, y) -> x));
        }

        @Test
        public void testZipWith3() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            byte[][] b = { { 5, 6 }, { 7, 8 } };
            byte[][] c = { { 9, 10 }, { 11, 12 } };
            ByteMatrix matrixA = ByteMatrix.wrap(a);
            ByteMatrix matrixB = ByteMatrix.wrap(b);
            ByteMatrix matrixC = ByteMatrix.wrap(c);

            ByteMatrix result = matrixA.zipWith(matrixB, matrixC, (x, y, z) -> (byte) (x + y + z));
            Assertions.assertEquals(15, result.get(0, 0));
            Assertions.assertEquals(18, result.get(0, 1));
            Assertions.assertEquals(21, result.get(1, 0));
            Assertions.assertEquals(24, result.get(1, 1));
        }

        @Test
        public void testZipWith_Shape2By2() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix result = m1.zipWith(m2, (a, b) -> (byte) (a * b));

            assertEquals(5, result.get(0, 0)); // 1*5
            assertEquals(12, result.get(0, 1)); // 2*6
            assertEquals(21, result.get(1, 0)); // 3*7
            assertEquals(32, result.get(1, 1)); // 4*8
        }

        @Test
        public void testZipWith_DifferentShapes() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> (byte) (a + b)));
        }

        @Test
        public void testZipWith_ThreeMatrices() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix m3 = ByteMatrix.wrap(new byte[][] { { 9, 10 }, { 11, 12 } });
            ByteMatrix result = m1.zipWith(m2, m3, (a, b, c) -> (byte) (a + b + c));

            assertEquals(15, result.get(0, 0)); // 1+5+9
            assertEquals(18, result.get(0, 1)); // 2+6+10
            assertEquals(21, result.get(1, 0)); // 3+7+11
            assertEquals(24, result.get(1, 1)); // 4+8+12
        }

        @Test
        public void testZipWith_ThreeMatrices_DifferentShapes() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix m3 = ByteMatrix.wrap(new byte[][] { { 9, 10, 11 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> (byte) (a + b + c)));
        }

        @Test
        public void testZipWith_Two() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix result = m1.zipWith(m2, (a, b) -> (byte) (a + b));
            assertEquals(6, result.get(0, 0));
            assertEquals(8, result.get(0, 1));
            assertEquals(10, result.get(1, 0));
            assertEquals(12, result.get(1, 1));
        }

        @Test
        public void testZipWith_Three() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 3, 4 } });
            ByteMatrix m3 = ByteMatrix.wrap(new byte[][] { { 5, 6 } });
            ByteMatrix result = m1.zipWith(m2, m3, (a, b, c) -> (byte) (a + b + c));
            assertEquals(9, result.get(0, 0));
            assertEquals(12, result.get(0, 1));
        }

        @Test
        public void testZipWith_ThreeMatrices_Shape2By2() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 1 }, { 1, 1 } });
            ByteMatrix m3 = ByteMatrix.wrap(new byte[][] { { 2, 2 }, { 2, 2 } });
            ByteMatrix result = m1.zipWith(m2, m3, (a, b, c) -> (byte) (a + b + c));
            assertEquals(4, result.get(0, 0));
            assertEquals(5, result.get(0, 1));
        }

        @Test
        public void testByteMatrixZipWith2() {
            // ByteMatrix.java: ByteMatrix result = matrix1.zipWith(matrix2, (a, b) -> (byte)(a * b));
            // result is: [[5, 12], [21, 32]]
            ByteMatrix matrix1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix matrix2 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix result = matrix1.zipWith(matrix2, (a, b) -> (byte) (a * b));
            assertEquals("[[5, 12], [21, 32]]", result.toString());
        }

        @Test
        public void testByteMatrixZipWith3() {
            // ByteMatrix.java: result is: [[15, 18], [21, 24]]
            ByteMatrix matrix1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix matrix2 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix matrix3 = ByteMatrix.wrap(new byte[][] { { 9, 10 }, { 11, 12 } });
            ByteMatrix result = matrix1.zipWith(matrix2, matrix3, (a, b, c) -> (byte) (a + b + c));
            assertEquals("[[15, 18], [21, 24]]", result.toString());
        }

        @Test
        public void testZipWith_Two_InvalidShape() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> (byte) (a + b)));
        }
    }

    @Nested
    class MainDiagonalStreamTests {

        @Test
        public void testStreamLU2RD() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] diagonal = matrix.mainDiagonalStream().toArray();
            Assertions.assertArrayEquals(new byte[] { 1, 5, 9 }, diagonal);

            ByteMatrix empty = ByteMatrix.empty();
            Assertions.assertTrue(empty.mainDiagonalStream().toList().isEmpty());

            ByteMatrix nonSquare = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            Assertions.assertArrayEquals(new byte[] { 1 }, nonSquare.mainDiagonalStream().toArray());
        }

        @Test
        public void testMainDiagonalStream_Shape3By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            byte[] diagonal = m.mainDiagonalStream().toArray();
            assertArrayEquals(new byte[] { 1, 5, 9 }, diagonal);
        }

        @Test
        public void testMainDiagonalStream_NonSquare() {
            ByteMatrix nonSquare = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertArrayEquals(new byte[] { 1 }, nonSquare.mainDiagonalStream().toArray());
        }

        @Test
        public void testMainDiagonalStream_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Byte> diag = m.mainDiagonalStream().boxed().toList();
            assertEquals(3, diag.size());
            assertEquals((byte) 1, diag.get(0));
            assertEquals((byte) 5, diag.get(1));
            assertEquals((byte) 9, diag.get(2));
        }

        @Test
        public void testMainDiagonalStream_Shape3By3_OrderedListValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Byte> diagonal = m.mainDiagonalStream().boxed().toList();
            assertEquals(3, diagonal.size());
            assertEquals(1, diagonal.get(0).byteValue());
            assertEquals(5, diagonal.get(1).byteValue());
            assertEquals(9, diagonal.get(2).byteValue());
        }

        @Test
        public void testMainDiagonalStream() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            byte[] diag = m.mainDiagonalStream().toArray();
            assertArrayEquals(new byte[] { 1, 5, 9 }, diag);
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testIteratorExCountConsumesEveryCustomIterator() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            com.landawn.abacus.util.stream.ByteIteratorEx main = (com.landawn.abacus.util.stream.ByteIteratorEx) matrix.mainDiagonalStream().iterator();
            assertEquals(2, main.count());
            assertFalse(main.hasNext());

            com.landawn.abacus.util.stream.ByteIteratorEx anti = (com.landawn.abacus.util.stream.ByteIteratorEx) matrix.antiDiagonalStream().iterator();
            assertEquals(2, anti.count());
            assertFalse(anti.hasNext());

            com.landawn.abacus.util.stream.ByteIteratorEx rowMajor = (com.landawn.abacus.util.stream.ByteIteratorEx) matrix.rowMajorStream().iterator();
            assertEquals(6, rowMajor.count());
            assertFalse(rowMajor.hasNext());

            com.landawn.abacus.util.stream.ByteIteratorEx columnMajor = (com.landawn.abacus.util.stream.ByteIteratorEx) matrix.columnMajorStream().iterator();
            assertEquals(6, columnMajor.count());
            assertFalse(columnMajor.hasNext());

            ObjIteratorEx<ByteStream> rows = (ObjIteratorEx<ByteStream>) matrix.rowStreams().iterator();
            assertEquals(2, rows.count());
            assertFalse(rows.hasNext());

            ObjIteratorEx<ByteStream> columns = (ObjIteratorEx<ByteStream>) matrix.columnStreams().iterator();
            ByteIteratorEx columnValues = (ByteIteratorEx) columns.next().iterator();
            assertEquals(2, columnValues.count());
            assertFalse(columnValues.hasNext());
            assertEquals(2, columns.count());
            assertFalse(columns.hasNext());
        }

        @Test
        public void testByteIteratorMaterializationConsumesOnlyTheRemainingLiveValues() {
            for (final boolean asList : new boolean[] { false, true }) {
                final ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
                final ByteIteratorEx main = (ByteIteratorEx) matrix.mainDiagonalStream().iterator();
                assertEquals((byte) 1, main.nextByte());
                matrix.set(1, 1, (byte) 50);
                final byte[] mainRemaining = asList ? main.toList().toArray() : main.toArray();
                assertArrayEquals(new byte[] { 50, 9 }, mainRemaining);
                assertFalse(main.hasNext());
                assertEquals(0L, main.count());
                assertArrayEquals(new byte[0], main.toArray());
                mainRemaining[0] = (byte) 99;
                assertEquals((byte) 50, matrix.get(1, 1));
                matrix.set(1, 1, (byte) 5);

                final ByteIteratorEx anti = (ByteIteratorEx) matrix.antiDiagonalStream().iterator();
                anti.advance(1);
                matrix.set(2, 0, (byte) 70);
                assertArrayEquals(new byte[] { 5, 70 }, asList ? anti.toList().toArray() : anti.toArray());
                assertFalse(anti.hasNext());
                assertEquals(0L, anti.count());
                assertEquals(0, anti.toList().size());

                final ByteIteratorEx column = (ByteIteratorEx) matrix.columnStreams(1, 2).iterator().next().iterator();
                assertEquals((byte) 2, column.nextByte());
                matrix.flipVerticallyInPlace();
                matrix.set(1, 1, (byte) 55);
                assertArrayEquals(new byte[] { 55, 2 }, asList ? column.toList().toArray() : column.toArray());
                assertFalse(column.hasNext());
                assertEquals(0L, column.count());
                assertArrayEquals(new byte[0], column.toArray());

                final ByteIteratorEx rows = (ByteIteratorEx) matrix.rowMajorStream().iterator();
                assertEquals((byte) 70, rows.nextByte());
                rows.advance(3);
                matrix.set(1, 1, (byte) 56);
                assertArrayEquals(new byte[] { 56, 6, 1, 2, 3 }, asList ? rows.toList().toArray() : rows.toArray());
                assertFalse(rows.hasNext());
                assertEquals(0L, rows.count());
                assertEquals(0, rows.toList().size());

                final ByteIteratorEx columns = (ByteIteratorEx) matrix.columnMajorStream().iterator();
                assertEquals((byte) 70, columns.nextByte());
                columns.advance(3);
                matrix.set(1, 1, (byte) 57);
                assertArrayEquals(new byte[] { 57, 2, 9, 6, 3 }, asList ? columns.toList().toArray() : columns.toArray());
                assertFalse(columns.hasNext());
                assertEquals(0L, columns.count());
                columns.advance(Long.MAX_VALUE);
                assertEquals(0, columns.toList().size());
            }
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testMainDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = ByteMatrix
                    .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
            final byte[] expected = new byte[] { (byte) 1, (byte) 5, (byte) 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (ByteIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextByte);
            }
        }

        @Test
        public void testMainDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = ByteMatrix
                    .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (ByteIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((byte) 1, iterator.nextByte());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextByte);
            }
        }

        @Test
        public void testMainDiagonalStream_Empty() {
            ByteMatrix empty = ByteMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().toArray().length);
        }

        @Test
        public void testDiagonalStream_Nx0MatrixIsEmpty() {
            ByteMatrix m = ByteMatrix.wrap(new byte[3][0]);
            assertEquals(3, m.rowCount());
            assertEquals(0, m.columnCount());
            assertEquals(0, m.mainDiagonalStream().count());
            assertEquals(0, m.antiDiagonalStream().count());

            ByteMatrix empty = ByteMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().count());
            assertEquals(0, empty.antiDiagonalStream().count());
        }
    }

    @Nested
    class AntiDiagonalStreamTests {

        @Test
        public void testStreamRU2LD() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] diagonal = matrix.antiDiagonalStream().toArray();
            Assertions.assertArrayEquals(new byte[] { 3, 5, 7 }, diagonal);
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            byte[] antiDiagonal = m.antiDiagonalStream().toArray();
            assertArrayEquals(new byte[] { 3, 5, 7 }, antiDiagonal);
        }

        @Test
        public void testAntiDiagonalStream_NonSquare() {
            ByteMatrix nonSquare = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertArrayEquals(new byte[] { 2 }, nonSquare.antiDiagonalStream().toArray());
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Byte> diag = m.antiDiagonalStream().boxed().toList();
            assertEquals(3, diag.size());
            assertEquals((byte) 3, diag.get(0));
            assertEquals((byte) 5, diag.get(1));
            assertEquals((byte) 7, diag.get(2));
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_OrderedListValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Byte> diagonal = m.antiDiagonalStream().boxed().toList();
            assertEquals(3, diagonal.size());
            assertEquals(3, diagonal.get(0).byteValue());
            assertEquals(5, diagonal.get(1).byteValue());
            assertEquals(7, diagonal.get(2).byteValue());
        }

        @Test
        public void testAntiDiagonalStream() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            byte[] diag = m.antiDiagonalStream().toArray();
            assertArrayEquals(new byte[] { 3, 5, 7 }, diag);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testAntiDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = ByteMatrix
                    .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
            final byte[] expected = new byte[] { (byte) 3, (byte) 5, (byte) 7 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (ByteIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextByte);
            }
        }

        @Test
        public void testAntiDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = ByteMatrix
                    .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (ByteIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((byte) 3, iterator.nextByte());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextByte);
            }
        }

        @Test
        public void testAntiDiagonalStream_Empty() {
            ByteMatrix empty = ByteMatrix.empty();
            assertEquals(0, empty.antiDiagonalStream().toArray().length);
        }
    }

    @Nested
    class RowMajorStreamTests {

        @Test
        public void testRowMajorStream() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] all = matrix.rowMajorStream().toArray();
            Assertions.assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, all);

            ByteMatrix empty = ByteMatrix.empty();
            Assertions.assertTrue(empty.rowMajorStream().toList().isEmpty());
        }

        @Test
        public void testRowMajorStreamRange() {
            byte[][] a = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] range = matrix.rowMajorStream(1, 3).toArray();
            Assertions.assertArrayEquals(new byte[] { 3, 4, 5, 6 }, range);

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 2));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(0, 4));
        }

        @Test
        public void testRowMajorStream_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            byte[] all = m.rowMajorStream().toArray();
            assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, all);
        }

        @Test
        public void testRowMajorStream_WithRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            byte[] rows = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new byte[] { 4, 5, 6, 7, 8, 9 }, rows);
        }

        @Test
        public void testRowMajorStream_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<Byte> all = m.rowMajorStream().boxed().toList();
            assertEquals(4, all.size());
            assertEquals((byte) 1, all.get(0));
            assertEquals((byte) 2, all.get(1));
        }

        @Test
        public void testRowMajorStream_RowRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<Byte> rows = m.rowMajorStream(1, 3).boxed().toList();
            assertEquals(4, rows.size());
            assertEquals((byte) 3, rows.get(0));
            assertEquals((byte) 4, rows.get(1));
        }

        @Test
        public void testRowMajorStream_Shape2By2_SizeAndFirstValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<Byte> elements = m.rowMajorStream().boxed().toList();
            assertEquals(4, elements.size());
            assertEquals(1, elements.get(0).byteValue());
            assertEquals(2, elements.get(1).byteValue());
        }

        @Test
        public void testRowMajorStream_WithRange_Shape3By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<Byte> elements = m.rowMajorStream(1, 3).boxed().toList();
            assertEquals(4, elements.size());
            assertEquals(3, elements.get(0).byteValue());
            assertEquals(4, elements.get(1).byteValue());
        }

        @Test
        public void testHorizontalStream() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            byte[] elements = m.rowMajorStream().toArray();
            assertArrayEquals(new byte[] { 1, 2, 3, 4 }, elements);
        }

        // Result: [[1, 2, 0], [3, 4, 0], [0, 0, 0]]
        @Test
        public void testRowMajorStream_RowRange_Shape3By2_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            byte[] elements = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new byte[] { 3, 4, 5, 6 }, elements);
        }

        @Test
        public void testStreamIterators_MidStrideAdvanceThenToArray_EdgeCase() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            var rowMajorIterator = matrix.rowMajorStream(0, 2).iterator();
            assertTrue(rowMajorIterator instanceof com.landawn.abacus.util.stream.ByteIteratorEx);
            com.landawn.abacus.util.stream.ByteIteratorEx rowMajor = (com.landawn.abacus.util.stream.ByteIteratorEx) rowMajorIterator;
            rowMajor.advance(1); // mid-row cursor: the chunked toArray must start at column 1
            assertEquals(5L, rowMajor.count());
            assertArrayEquals(new byte[0], rowMajor.toArray());

            com.landawn.abacus.util.stream.ByteIteratorEx rowMajorFresh = (com.landawn.abacus.util.stream.ByteIteratorEx) matrix.rowMajorStream(0, 2)
                    .iterator();
            rowMajorFresh.advance(1);
            assertArrayEquals(new byte[] { 2, 3, 4, 5, 6 }, rowMajorFresh.toArray());

            var columnMajorIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.ByteIteratorEx columnMajor = (com.landawn.abacus.util.stream.ByteIteratorEx) columnMajorIterator;
            columnMajor.advance(1); // mid-column cursor: row 1 of column 0
            assertEquals(5L, columnMajor.count());
            assertArrayEquals(new byte[0], columnMajor.toArray());

            com.landawn.abacus.util.stream.ByteIteratorEx columnMajorFresh = (com.landawn.abacus.util.stream.ByteIteratorEx) matrix.columnMajorStream(0, 3)
                    .iterator();
            columnMajorFresh.advance(1);
            assertArrayEquals(new byte[] { 4, 2, 5, 3, 6 }, columnMajorFresh.toArray());

            var crossingIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.ByteIteratorEx crossing = (com.landawn.abacus.util.stream.ByteIteratorEx) crossingIterator;
            crossing.advance(3); // crosses a column boundary: lands on row 1 of column 1
            assertEquals(3L, crossing.count());
            assertFalse(crossing.hasNext());
            crossing.advance(10);
            assertEquals(0L, crossing.count());
            assertThrows(java.util.NoSuchElementException.class, crossing::nextByte);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testRowMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = ByteMatrix
                    .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
            final byte[] expected = new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (ByteIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextByte);
            }
        }

        @Test
        public void testRowMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = ByteMatrix
                    .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (ByteIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((byte) 1, iterator.nextByte());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextByte);
            }
        }

        @Test
        public void testRowMajorStreamSingleRowRange() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] row1 = matrix.rowMajorStream(1, 2).toArray();
            Assertions.assertArrayEquals(new byte[] { 4, 5, 6 }, row1);
        }

        @Test
        public void testRowMajorStream_Empty() {
            ByteMatrix empty = ByteMatrix.empty();
            assertEquals(0, empty.rowMajorStream().toArray().length);
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            byte[] row1 = m.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new byte[] { 4, 5, 6 }, row1);
        }

        @Test
        public void testRowMajorStream_SingleRowRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Byte> row = m.rowMajorStream(0, 1).boxed().toList();
            assertEquals(3, row.size());
            assertEquals((byte) 1, row.get(0));
            assertEquals((byte) 2, row.get(1));
            assertEquals((byte) 3, row.get(2));
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<Byte> row = m.rowMajorStream(1, 2).boxed().toList();
            assertEquals(2, row.size());
            assertEquals(3, row.get(0).byteValue());
            assertEquals(4, row.get(1).byteValue());
        }

        @Test
        public void testRowMajorStream_SingleRowRange_Shape2By2_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            byte[] row = m.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new byte[] { 3, 4 }, row);
        }

        @Test
        public void testRowMajorStreamorizontalIteratorAdvanceAndExhaustion_SingleValueRemaining() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            var iterator = matrix.rowMajorStream(0, 2).iterator();

            assertTrue(iterator instanceof com.landawn.abacus.util.stream.ByteIteratorEx);

            com.landawn.abacus.util.stream.ByteIteratorEx ex = (com.landawn.abacus.util.stream.ByteIteratorEx) iterator;
            ex.advance(3);
            assertEquals(1L, ex.count());
            assertFalse(ex.hasNext());

            com.landawn.abacus.util.stream.ByteIteratorEx fresh = (com.landawn.abacus.util.stream.ByteIteratorEx) matrix.rowMajorStream(0, 2).iterator();
            fresh.advance(3);
            assertEquals((byte) 4, fresh.nextByte());
            ex.advance(10);
            assertEquals(0L, ex.count());
            assertThrows(java.util.NoSuchElementException.class, ex::nextByte);
        }

        @Test
        public void testByteEmptyStreamRangesKeepBoundsValidationAndLogicalShapeCounts() {
            final ByteMatrix matrix = new ByteMatrix(new byte[2][2]);
            assertEquals(0L, matrix.rowMajorStream(1, 1).count());
            assertEquals(0L, matrix.columnMajorStream(1, 1).count());
            assertEquals(0L, matrix.rowStreams(1, 1).count());
            assertEquals(0L, matrix.columnStreams(1, 1).count());
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(3, 3));

            final ByteMatrix noColumns = new ByteMatrix(new byte[2][0]);
            assertEquals(2L, noColumns.rowStreams().count());
            noColumns.rowStreams().forEach(row -> assertEquals(0L, row.count()));
            assertEquals(0L, noColumns.mainDiagonalStream().count());
            assertEquals(0L, noColumns.antiDiagonalStream().count());

            final ByteMatrix noRows = new ByteMatrix(new byte[0][], 3);
            assertEquals(3L, noRows.columnStreams().count());
            noRows.columnStreams().forEach(column -> assertEquals(0L, column.count()));
            assertEquals(0L, noRows.mainDiagonalStream().count());
            assertEquals(0L, noRows.antiDiagonalStream().count());
        }

        @Test
        public void testRowMajorStream_ZeroColumnsKeepLogicalRowRange() {
            final var matrix = ByteMatrix.wrap(new byte[3][0]);
            assertEquals(0, matrix.rowMajorStream(1, 3).count());
            assertEquals(2, matrix.rowStreams(1, 3).count());
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 3));
        }

        @Test
        public void testRowMajorStream_WithRange_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 1));
        }
    }

    @Nested
    class ColumnMajorStreamTests {

        @Test
        public void testColumnMajorStream() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] all = matrix.columnMajorStream().toArray();
            Assertions.assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 6 }, all);
        }

        // Pins the rowStreams(int, int) javadoc example: OptionalByte.orElse requires a byte argument.
        @Test
        public void testColumnMajorStreamRange() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] range = matrix.columnMajorStream(1, 3).toArray();
            Assertions.assertArrayEquals(new byte[] { 2, 5, 3, 6 }, range);
        }

        @Test
        public void testColumnMajorStream_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            byte[] all = m.columnMajorStream().toArray();
            assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 6 }, all);
        }

        @Test
        public void testColumnMajorStream_WithRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            byte[] columnCount = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new byte[] { 2, 5, 8, 3, 6, 9 }, columnCount);
        }

        @Test
        public void testColumnMajorStream_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<Byte> all = m.columnMajorStream().boxed().toList();
            assertEquals(4, all.size());
            assertEquals((byte) 1, all.get(0));
            assertEquals((byte) 3, all.get(1));
        }

        @Test
        public void testColumnMajorStream_ColumnRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Byte> columnCount = m.columnMajorStream(1, 3).boxed().toList();
            assertEquals(4, columnCount.size());
            assertEquals((byte) 2, columnCount.get(0));
            assertEquals((byte) 5, columnCount.get(1));
        }

        @Test
        public void testColumnMajorStream_Shape2By2_SizeAndFirstValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<Byte> elements = m.columnMajorStream().boxed().toList();
            assertEquals(4, elements.size());
            assertEquals(1, elements.get(0).byteValue());
            assertEquals(3, elements.get(1).byteValue());
        }

        @Test
        public void testColumnMajorStream_WithRange_Shape2By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Byte> elements = m.columnMajorStream(1, 3).boxed().toList();
            assertEquals(4, elements.size());
            assertEquals(2, elements.get(0).byteValue());
            assertEquals(5, elements.get(1).byteValue());
        }

        @Test
        public void testVerticalStream() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            byte[] elements = m.columnMajorStream().toArray();
            assertArrayEquals(new byte[] { 1, 3, 2, 4 }, elements);
        }

        @Test
        public void testColumnMajorStream_ColumnRange_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            byte[] elements = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new byte[] { 2, 5, 3, 6 }, elements);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testColumnMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = ByteMatrix
                    .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
            final byte[] expected = new byte[] { (byte) 1, (byte) 4, (byte) 7, (byte) 2, (byte) 5, (byte) 8, (byte) 3, (byte) 6, (byte) 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (ByteIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextByte);
            }
        }

        @Test
        public void testColumnMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = ByteMatrix
                    .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (ByteIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((byte) 1, iterator.nextByte());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextByte);
            }
        }

        @Test
        public void testColumnMajorStreamSingleColumnRange() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            byte[] col1 = matrix.columnMajorStream(1, 2).toArray();
            Assertions.assertArrayEquals(new byte[] { 2, 5 }, col1);
        }

        @Test
        public void testColumnMajorStream_Empty() {
            ByteMatrix empty = ByteMatrix.empty();
            assertEquals(0, empty.columnMajorStream().toArray().length);
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            byte[] col1 = m.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new byte[] { 2, 5 }, col1);
        }

        @Test
        public void testColumnMajorStream_SingleColumnRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<Byte> col = m.columnMajorStream(0, 1).boxed().toList();
            assertEquals(3, col.size());
            assertEquals((byte) 1, col.get(0));
            assertEquals((byte) 3, col.get(1));
            assertEquals((byte) 5, col.get(2));
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<Byte> col = m.columnMajorStream(1, 2).boxed().toList();
            assertEquals(2, col.size());
            assertEquals(2, col.get(0).byteValue());
            assertEquals(4, col.get(1).byteValue());
        }

        @Test
        public void testColumnMajorStream_SingleColumnRange_Shape2By2_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            byte[] col = m.columnMajorStream(0, 1).toArray();
            assertArrayEquals(new byte[] { 1, 3 }, col);
        }

        @Test
        public void testColumnMajorStream_ZeroRowsKeepLogicalColumnRange() {
            final var matrix = ByteMatrix.wrap(new byte[0][0]).resize(0, 3);
            assertEquals(0, matrix.columnMajorStream(1, 3).count());
            assertEquals(2, matrix.columnStreams(1, 3).count());
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 3));
        }

        @Test
        public void testColumnMajorStream_WithRange_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 1));
        }
    }

    @Nested
    class RowStreamsTests {

        @Test
        public void testDocumentedStreamsReadLiveCellsAndPreserveProducedRowBinding() {
            final ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            final var row = matrix.rowStreams().iterator().next();
            final var selectedRow = matrix.rowStreams(0, 1).iterator().next();
            final var column = matrix.columnStreams().iterator().next();
            final var selectedColumn = matrix.columnStreams(0, 1).iterator().next();
            final var rowMajor = matrix.rowMajorStream();
            final var selectedRowMajor = matrix.rowMajorStream(0, 1);
            final var columnMajor = matrix.columnMajorStream();
            final var selectedColumnMajor = matrix.columnMajorStream(0, 1);
            final var mainDiagonal = matrix.mainDiagonalStream();
            final var antiDiagonal = matrix.antiDiagonalStream();

            matrix.flipVerticallyInPlace();
            matrix.set(1, 0, (byte) 9);
            matrix.set(0, 1, (byte) 8);

            assertArrayEquals(new byte[] { 9, 2 }, row.toArray());
            assertArrayEquals(new byte[] { 9, 2 }, selectedRow.toArray());
            assertArrayEquals(new byte[] { 3, 9 }, column.toArray());
            assertArrayEquals(new byte[] { 3, 9 }, selectedColumn.toArray());
            assertArrayEquals(new byte[] { 3, 8, 9, 2 }, rowMajor.toArray());
            assertArrayEquals(new byte[] { 3, 8 }, selectedRowMajor.toArray());
            assertArrayEquals(new byte[] { 3, 9, 8, 2 }, columnMajor.toArray());
            assertArrayEquals(new byte[] { 3, 9 }, selectedColumnMajor.toArray());
            assertArrayEquals(new byte[] { 3, 2 }, mainDiagonal.toArray());
            assertArrayEquals(new byte[] { 8, 9 }, antiDiagonal.toArray());
        }

        @Test
        public void testStreamR() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            List<ByteStream> rows = matrix.rowStreams().toList();
            Assertions.assertEquals(2, rows.size());
            Assertions.assertArrayEquals(new byte[] { 1, 2 }, rows.get(0).toArray());
            Assertions.assertArrayEquals(new byte[] { 3, 4 }, rows.get(1).toArray());
        }

        @Test
        public void testRowStreamsRange() {
            byte[][] a = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            List<ByteStream> rows = matrix.rowStreams(1, 3).toList();
            Assertions.assertEquals(2, rows.size());
            Assertions.assertArrayEquals(new byte[] { 3, 4 }, rows.get(0).toArray());
            Assertions.assertArrayEquals(new byte[] { 5, 6 }, rows.get(1).toArray());
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testRowStreamsowsAndColumnsIteratorAdvanceAndExhaustion_EdgeCase() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            var rowIterator = matrix.rowStreams(0, 2).iterator();

            Assertions.assertTrue(rowIterator instanceof ObjIteratorEx);

            ObjIteratorEx<ByteStream> rowEx = (ObjIteratorEx<ByteStream>) rowIterator;
            rowEx.advance(0);
            Assertions.assertEquals(2L, rowEx.count());
            Assertions.assertFalse(rowEx.hasNext());
            rowEx.advance(10);
            Assertions.assertEquals(0L, rowEx.count());
            Assertions.assertThrows(NoSuchElementException.class, rowEx::next);

            var columnIterator = matrix.columnStreams(0, 1).iterator();

            Assertions.assertTrue(columnIterator instanceof ObjIteratorEx);

            ObjIteratorEx<ByteStream> columnEx = (ObjIteratorEx<ByteStream>) columnIterator;
            ByteStream firstColumn = columnEx.next();
            var valueIterator = firstColumn.iterator();

            Assertions.assertTrue(valueIterator instanceof ByteIteratorEx);

            ByteIteratorEx byteEx = (ByteIteratorEx) valueIterator;
            byteEx.advance(0);
            Assertions.assertEquals(2L, byteEx.count());
            Assertions.assertFalse(byteEx.hasNext());
            byteEx.advance(10);
            Assertions.assertEquals(0L, byteEx.count());
            Assertions.assertThrows(NoSuchElementException.class, byteEx::nextByte);
        }

        @Test
        public void testRowStreams_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<byte[]> rows = m.rowStreams().map(ByteStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new byte[] { 1, 2, 3 }, rows.get(0));
            assertArrayEquals(new byte[] { 4, 5, 6 }, rows.get(1));
        }

        @Test
        public void testRowStreams_WithRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<byte[]> rows = m.rowStreams(1, 3).map(ByteStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new byte[] { 4, 5, 6 }, rows.get(0));
            assertArrayEquals(new byte[] { 7, 8, 9 }, rows.get(1));
        }

        @Test
        public void testRowStreams_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<ByteStream> rows = m.rowStreams().toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).count());
        }

        @Test
        public void testRowStreams_RowRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<ByteStream> rows = m.rowStreams(1, 3).toList();
            assertEquals(2, rows.size());
        }

        @Test
        public void testRowStreams_Shape2By2_OuterAndInnerStreamSizes() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<ByteStream> rows = m.rowStreams().toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).toArray().length);
        }

        @Test
        public void testRowStreams() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<byte[]> rows = m.rowStreams().map(ByteStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new byte[] { 1, 2 }, rows.get(0));
        }

        @Test
        public void testRowStreams_RowRange_Shape3By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<byte[]> rows = m.rowStreams(1, 3).map(ByteStream::toArray).toList();
            assertEquals(2, rows.size());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testRowStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = ByteMatrix
                    .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
            final byte[][] expected = { new byte[] { (byte) 1, (byte) 2, (byte) 3 }, new byte[] { (byte) 4, (byte) 5, (byte) 6 },
                    new byte[] { (byte) 7, (byte) 8, (byte) 9 } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowStreams()) {
                final var iterator = (ObjIteratorEx<ByteStream>) stream.iterator();
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
        public void testRowStreamsRange_JavadocExampleRowMaxes() {
            // Pins the rowStreams(int, int) javadoc example: OptionalByte.orElse requires a byte argument.
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            int[] maxes = matrix.rowStreams(0, 2).mapToInt(row -> row.max().orElse((byte) 0)).toArray();
            Assertions.assertArrayEquals(new int[] { 2, 4 }, maxes);
        }

        @Test
        public void testRowStreams_Empty() {
            ByteMatrix empty = ByteMatrix.empty();
            assertEquals(0, empty.rowStreams().count());
        }

        @Test
        public void testRowStreams_WithRange_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(0, 3));
        }
    }

    @Nested
    class ColumnStreamsTests {

        @Test
        public void testStreamC() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            List<ByteStream> columnCount = matrix.columnStreams().toList();
            Assertions.assertEquals(3, columnCount.size());
            Assertions.assertArrayEquals(new byte[] { 1, 4 }, columnCount.get(0).toArray());
            Assertions.assertArrayEquals(new byte[] { 2, 5 }, columnCount.get(1).toArray());
            Assertions.assertArrayEquals(new byte[] { 3, 6 }, columnCount.get(2).toArray());
        }

        @Test
        public void testColumnStreamsRange() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            List<ByteStream> columnCount = matrix.columnStreams(1, 3).toList();
            Assertions.assertEquals(2, columnCount.size());
            Assertions.assertArrayEquals(new byte[] { 2, 5 }, columnCount.get(0).toArray());
            Assertions.assertArrayEquals(new byte[] { 3, 6 }, columnCount.get(1).toArray());
        }

        @Test
        public void testColumnStreams_Shape2By3_ArrayValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<byte[]> columnCount = m.columnStreams().map(ByteStream::toArray).toList();
            assertEquals(3, columnCount.size());
            assertArrayEquals(new byte[] { 1, 4 }, columnCount.get(0));
            assertArrayEquals(new byte[] { 2, 5 }, columnCount.get(1));
            assertArrayEquals(new byte[] { 3, 6 }, columnCount.get(2));
        }

        @Test
        public void testColumnStreams_WithRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<byte[]> columnCount = m.columnStreams(1, 3).map(ByteStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new byte[] { 2, 5, 8 }, columnCount.get(0));
            assertArrayEquals(new byte[] { 3, 6, 9 }, columnCount.get(1));
        }

        @Test
        public void testColumnStreams_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<ByteStream> columnCount = m.columnStreams().toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).count());
        }

        @Test
        public void testColumnStreams_ColumnRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<ByteStream> columnCount = m.columnStreams(1, 3).toList();
            assertEquals(2, columnCount.size());
        }

        @Test
        public void testColumnStreams_Shape2By2_OuterAndInnerStreamSizes() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<ByteStream> columnCount = m.columnStreams().toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).toArray().length);
        }

        @Test
        public void testColumnStreams() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<byte[]> columnCount = m.columnStreams().map(ByteStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new byte[] { 1, 3 }, columnCount.get(0));
        }

        @Test
        public void testColumnStreams_ColumnRange_Shape2By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<byte[]> columnCount = m.columnStreams(0, 2).map(ByteStream::toArray).toList();
            assertEquals(2, columnCount.size());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testColumnStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = ByteMatrix
                    .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
            final byte[][] expected = { new byte[] { (byte) 1, (byte) 4, (byte) 7 }, new byte[] { (byte) 2, (byte) 5, (byte) 8 },
                    new byte[] { (byte) 3, (byte) 6, (byte) 9 } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnStreams()) {
                final var iterator = (ObjIteratorEx<ByteStream>) stream.iterator();
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
            final var matrix = ByteMatrix
                    .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
            final byte[] expected = new byte[] { (byte) 2, (byte) 5, (byte) 8 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (ByteIteratorEx) values.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextByte);
            }
        }

        @Test
        public void testColumnStreams_InnerCountConsumesOnlyRemainingValues() {
            final var matrix = ByteMatrix
                    .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (ByteIteratorEx) values.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((byte) 2, iterator.nextByte());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextByte);
            }
        }

        @Test
        public void testColumnStreams_Empty() {
            ByteMatrix empty = ByteMatrix.empty();
            assertEquals(0, empty.columnStreams().count());
        }

        @Test
        public void testColumnStreams_WithRange_OutOfBounds() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(0, 3));
        }
    }

    @Nested
    class LengthTests {

        @Test
        public void testLength() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            // length is protected, cannot test directly from here
            // It's tested indirectly through other operations
            Assertions.assertEquals(2, matrix.columnCount());
        }
    }

    @Nested
    class ForEachTests {

        @Test
        public void testDocumentedForEachStopsAfterCallbackFailure() throws Exception {
            for (int operation = 0; operation < 2; operation++) {
                final int selectedOperation = operation;
                final ByteMatrix matrix = ByteMatrix.wrap(new byte[2][3]);
                final AtomicInteger calls = new AtomicInteger();
                final IllegalStateException failure = new IllegalStateException("action failed");
                final Throwables.ByteConsumer<IllegalStateException> action = value -> {
                    if (calls.incrementAndGet() == 2) {
                        throw failure;
                    }
                };

                assertSame(failure, assertThrows(IllegalStateException.class, () -> Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                    if (selectedOperation == 0) {
                        matrix.forEach(action);
                    } else {
                        matrix.forEach(0, 2, 1, 3, action);
                    }
                })));

                assertEquals(2, calls.get(), "operation " + selectedOperation);
            }
        }

        @Test
        public void testForEach() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            List<Byte> collected = new ArrayList<>();
            matrix.forEach(b -> collected.add(b));

            Assertions.assertEquals(4, collected.size());
            Assertions.assertTrue(collected.contains((byte) 1));
            Assertions.assertTrue(collected.contains((byte) 2));
            Assertions.assertTrue(collected.contains((byte) 3));
            Assertions.assertTrue(collected.contains((byte) 4));
        }

        @Test
        public void testForEachWithRange() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            List<Byte> collected = new ArrayList<>();
            matrix.forEach(1, 3, 1, 3, b -> collected.add(b));

            Assertions.assertEquals(4, collected.size());
            Assertions.assertTrue(collected.contains((byte) 5));
            Assertions.assertTrue(collected.contains((byte) 6));
            Assertions.assertTrue(collected.contains((byte) 8));
            Assertions.assertTrue(collected.contains((byte) 9));

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.forEach(0, 4, 0, 3, b -> {
            }));
        }

        @Test
        public void testForEachWithForcedParallelMode_EdgeCase() throws Exception {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Byte> allValues = new ArrayList<>();
            List<Byte> regionValues = new ArrayList<>();

            Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> matrix.forEach(e -> {
                synchronized (allValues) {
                    allValues.add(e);
                }
            }));

            Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> matrix.forEach(0, 2, 1, 3, e -> {
                synchronized (regionValues) {
                    regionValues.add(e);
                }
            }));

            Assertions.assertEquals(6, allValues.size());
            Assertions.assertTrue(allValues.containsAll(List.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6)));
            Assertions.assertEquals(4, regionValues.size());
            Assertions.assertTrue(regionValues.containsAll(List.of((byte) 2, (byte) 3, (byte) 5, (byte) 6)));
        }

        @Test
        public void testForEach_ValueConsumer() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<Byte> values = new ArrayList<>();
            m.forEach(b -> values.add(b));
            assertEquals(4, values.size());
            assertEquals((byte) 1, values.get(0));
            assertEquals((byte) 2, values.get(1));
        }

        @Test
        public void testForEach_WithRange() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Byte> values = new ArrayList<>();
            m.forEach(1, 3, 1, 3, b -> values.add(b));
            assertEquals(4, values.size());
            assertEquals((byte) 5, values.get(0));
            assertEquals((byte) 6, values.get(1));
        }

        @Test
        public void testForEach_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            AtomicInteger sum = new AtomicInteger(0);
            m.forEach(val -> sum.addAndGet(val));
            assertEquals(10, sum.get());
        }

        @Test
        public void testByteMatrixForEach() {
            // ByteMatrix.java: matrix.forEach(value -> System.out.print(value + " "));
            // Output: 1 2 3 4
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<Byte> values = new ArrayList<>();
            matrix.forEach(value -> values.add(value));
            assertEquals(4, values.size());
            assertEquals((byte) 1, values.get(0));
            assertEquals((byte) 2, values.get(1));
            assertEquals((byte) 3, values.get(2));
            assertEquals((byte) 4, values.get(3));
        }

        @Test
        public void testByteMatrixForEachWithRange() {
            // ByteMatrix.java: matrix.forEach(1, 3, 1, 3, value -> System.out.print(value + " "));
            // Output: 5 6 8 9  (processes elements in rows 1-2, columns 1-2)
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Byte> values = new ArrayList<>();
            matrix.forEach(1, 3, 1, 3, value -> values.add(value));
            assertEquals(4, values.size());
            assertEquals((byte) 5, values.get(0));
            assertEquals((byte) 6, values.get(1));
            assertEquals((byte) 8, values.get(2));
            assertEquals((byte) 9, values.get(3));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testForEach_ExplicitModesVisitSelectedRegionExactlyOnce(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = ByteMatrix
                        .wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } });
                final var values = new ConcurrentLinkedQueue<Byte>();
                matrix.forEach(0, 2, 1, 3, values::add);
                assertEquals(List.of((byte) 2, (byte) 3, (byte) 5, (byte) 6).stream().sorted().toList(), values.stream().sorted().toList());
                assertEquals(
                        ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2, (byte) 3 }, { (byte) 4, (byte) 5, (byte) 6 }, { (byte) 7, (byte) 8, (byte) 9 } }),
                        matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testForEachNullAction() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.forEach((Throwables.ByteConsumer<RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.forEach(0, 1, 0, 2, (Throwables.ByteConsumer<RuntimeException>) null));
        }
    }

    @Nested
    class ToMultilineStringTests {

        @Test
        public void testToMultilineString_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            String result = m.toMultilineString();
            assertNotNull(result);
            assertTrue(result.contains("1"));
            assertTrue(result.contains("4"));
        }

        @Test
        public void testToMultilineStringEmptyMatrix_EdgeCase() {
            Assertions.assertEquals("[]", ByteMatrix.empty().toMultilineString());
        }

        @Test
        public void testToMultilineStringNonEmptyReturnsNonNullFormattedString() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains("[1, 2]"));
            assertTrue(printed.contains("[3, 4]"));
            assertFalse(printed.equals("null"));
        }

        @Test
        public void testToMultilineStringEmptyReturnsBracketsLiteral() {
            ByteMatrix empty = ByteMatrix.empty();
            String printed = empty.toMultilineString();
            assertEquals("[]", printed);
        }

        @Test
        public void testToMultilineStringWithMinAndMaxByteValues() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { Byte.MIN_VALUE, Byte.MAX_VALUE } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains(String.valueOf(Byte.MIN_VALUE)));
            assertTrue(printed.contains(String.valueOf(Byte.MAX_VALUE)));
        }
    }

    @Nested
    class HashCodeTests {

        @Test
        public void testHashCode() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix1 = ByteMatrix.wrap(a);
            ByteMatrix matrix2 = ByteMatrix.wrap(a.clone());

            // Same content should have same hash code
            Assertions.assertEquals(matrix1.hashCode(), matrix2.hashCode());
        }

        @Test
        public void testHashCode_Shape2By2() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m3 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 4, 3 } });

            assertEquals(m1.hashCode(), m2.hashCode());
            assertNotEquals(m1.hashCode(), m3.hashCode()); // Usually different
        }

        @Test
        public void testHashCode_Equal() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        public void testHashCode_ZeroRowWidthsDistinguishShapes() {
            final var empty = ByteMatrix.wrap(new byte[0][0]);
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
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix1 = ByteMatrix.wrap(a);
            ByteMatrix matrix2 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix matrix3 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 5 } });
            ByteMatrix matrix4 = ByteMatrix.wrap(new byte[][] { { 1, 2 } });

            assertTrue(matrix1.equals(matrix1)); // same instance
            Assertions.assertEquals(matrix1, matrix2); // same content
            Assertions.assertNotEquals(matrix1, matrix3); // different content
            Assertions.assertNotEquals(matrix1, matrix4); // different dimensions
            Assertions.assertNotEquals(matrix1, null);
            Assertions.assertNotEquals(matrix1, "not a matrix");
        }

        @Test
        public void testEquals_Same() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertTrue(m.equals(m));
        }

        @Test
        public void testEquals_Equal() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(m1, m2);
        }

        @Test
        public void testEquals_NotEqual() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 3, 4 } });
            assertNotEquals(m1, m2);
        }

        @Test
        public void testEquals_DifferentType() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1 } });
            assertNotEquals(m, "string");
        }

        @Test
        public void testEquals_DifferentRowCountsWithEqualColumnCounts() {
            final var longer = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 }, { (byte) 3, (byte) 4 } });
            final var shorter = ByteMatrix.wrap(new byte[][] { { (byte) 1, (byte) 2 } });
            assertFalse(longer.equals(shorter));
            assertFalse(shorter.equals(longer));
        }

        @Test
        public void testEquals_Shape2By2_NullInput() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m3 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 4, 3 } });
            ByteMatrix m4 = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            assertTrue(m1.equals(m1)); // Same object
            assertTrue(m1.equals(m2)); // Same values
            assertFalse(m1.equals(m3)); // Different values
            assertFalse(m1.equals(m4)); // Different dimensions
            assertFalse(m1.equals(null));
            assertFalse(m1.equals("not a matrix"));
        }

        @Test
        public void testEquals_Shape2By2_NullInput_DifferentValuesNullAndType() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m3 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            assertEquals(m1, m2);
            assertNotEquals(m1, m3);
            assertNotEquals(m1, null);
            assertNotEquals(m1, "not a matrix");
        }

        @Test
        public void testEquals_Null() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1 } });
            assertNotEquals(m, null);
        }

        @Test
        public void testEquals_EmptyMatrices() {
            ByteMatrix m1 = ByteMatrix.empty();
            ByteMatrix m2 = ByteMatrix.empty();
            assertEquals(m1, m2);
        }
    }

    @Nested
    class ToStringTests {

        @Test
        public void testToString() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            String str = matrix.toString();
            Assertions.assertNotNull(str);
            Assertions.assertTrue(str.contains("1"));
            Assertions.assertTrue(str.contains("4"));
        }

        @Test
        public void testToString_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1"));
            assertTrue(str.contains("2"));
            assertTrue(str.contains("3"));
            assertTrue(str.contains("4"));
        }

        @Test
        public void testToString_Shape2By2_ContainsElementText() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1"));
            assertTrue(str.contains("4"));
        }

        @Test
        public void testtoString() {
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertEquals("[[1, 2], [3, 4]]", matrix.toString());

            ByteMatrix empty = ByteMatrix.empty();
            assertEquals("[]", empty.toString());
        }

        @Test
        public void testByteMatrixToString() {
            // ByteMatrix.java: System.out.println(matrix.toString());   // Output: [[1, 2], [3, 4]]
            ByteMatrix matrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            assertEquals("[[1, 2], [3, 4]]", matrix.toString());
        }
    }

    @Nested
    class ElementTypeTests {

        @Test
        public void testElementType() {
            ByteMatrix matrix = ByteMatrix.empty();
            Assertions.assertEquals(byte.class, matrix.elementType());
        }

        @Test
        public void testElementType_Shape1By1() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1 } });
            assertEquals(byte.class, m.elementType());
        }

        @Test
        public void testElementType_EmptyMatrix() {
            ByteMatrix m = ByteMatrix.empty();
            assertEquals(byte.class, m.elementType());
        }
    }

    @Nested
    class UnsafeBackingArrayTests {

        @Test
        public void testArray() {
            byte[][] arr = { { 1, 2 }, { 3, 4 } };
            ByteMatrix m = ByteMatrix.wrap(arr);
            byte[][] result = m.unsafeBackingArray();

            // The outer array is a private snapshot, so replacing a row in either array is invisible
            // to the other; the row arrays themselves are still shared, so cell writes are visible.
            assertNotSame(arr, result);
            assertSame(arr[0], result[0]);
            assertSame(arr[1], result[1]);

            arr[0][0] = (byte) 9;
            assertEquals((byte) 9, m.get(0, 0));
        }

        @Test
        public void testArray_ArrayValues() {
            byte[][] arr = { { 1, 2 }, { 3, 4 } };
            ByteMatrix m = ByteMatrix.wrap(arr);
            byte[][] result = m.unsafeBackingArray();
            assertArrayEquals(arr, result);
        }
    }

    @Nested
    class ElementCountTests {

        @Test
        public void testElementCount() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(6, m.elementCount());
        }

        @Test
        public void testElementCount_Empty() {
            ByteMatrix m = ByteMatrix.empty();
            assertEquals(0, m.elementCount());
        }
    }

    @Nested
    class IsEmptyTests {

        @Test
        public void testIsEmpty() {
            ByteMatrix empty = ByteMatrix.empty();
            assertTrue(empty.isEmpty());
            ByteMatrix notEmpty = ByteMatrix.wrap(new byte[][] { { 1 } });
            assertFalse(notEmpty.isEmpty());
        }

        @Test
        public void testIsEmpty_Shape1By1() {
            assertTrue(ByteMatrix.empty().isEmpty());
            assertFalse(ByteMatrix.wrap(new byte[][] { { 1 } }).isEmpty());
        }
    }

    @Nested
    class IsSameShapeTests {

        @Test
        public void testIsSameShape() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[2][3]);
            ByteMatrix m2 = ByteMatrix.wrap(new byte[2][3]);
            ByteMatrix m3 = ByteMatrix.wrap(new byte[3][2]);
            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }

        @Test
        public void testIsSameShape_Shape2By2() {
            ByteMatrix m1 = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix m2 = ByteMatrix.wrap(new byte[][] { { 5, 6 }, { 7, 8 } });
            ByteMatrix m3 = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 } });
            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }
    }

    @Nested
    class ReshapeTests {

        @Test
        public void testReshape() {
            byte[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            ByteMatrix reshaped = matrix.reshape(3, 2);
            Assertions.assertEquals(3, reshaped.rowCount());
            Assertions.assertEquals(2, reshaped.columnCount());
            Assertions.assertEquals(1, reshaped.get(0, 0));
            Assertions.assertEquals(2, reshaped.get(0, 1));
            Assertions.assertEquals(3, reshaped.get(1, 0));
            Assertions.assertEquals(4, reshaped.get(1, 1));

            ByteMatrix empty = ByteMatrix.empty().reshapeAndPad(2, 3);
            Assertions.assertEquals(2, empty.rowCount());
            Assertions.assertEquals(3, empty.columnCount());

            // Test reshape with too-small dimensions throws exception
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.reshape(1, 4));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.reshape(2, 2));
        }

        @Test
        public void testReshape_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ByteMatrix reshaped = m.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, reshaped.get(0, i));
            }
        }

        // Check vertical repeat
        @Test
        public void testReshape_Back() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ByteMatrix reshaped = m.reshape(1, 9);
            ByteMatrix reshapedBack = reshaped.reshape(3, 3);
            assertEquals(m, reshapedBack);
        }

        @Test
        public void testReshape_Shape1By4() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3, 4 } });
            ByteMatrix reshaped = m.reshape(2, 2);
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(3, reshaped.get(1, 0));
            assertEquals(4, reshaped.get(1, 1));
        }

        @Test
        public void testReshape_Shape2By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ByteMatrix reshaped = m.reshape(3, 2);
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
        }

        @Test
        public void testReshape_ToSingleRow() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix reshaped = m.reshape(1, 4);
            assertEquals(1, reshaped.rowCount());
            assertEquals(4, reshaped.columnCount());
        }
    }

    @Nested
    class ReshapeAndPadToColumnCountTests {

        @Test
        public void testReshape_OneArg() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3, 4 } });
            ByteMatrix reshaped = m.reshapeAndPadToColumnCount(2);
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(3, reshaped.get(1, 0));
            assertEquals(4, reshaped.get(1, 1));
        }
    }

    @Nested
    class Adjacent4PointsTests {

        @Test
        public void testAdjacent4Points() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = m.adjacent4Points(1, 1).toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testAdjacent4Points_Corner() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.adjacent4Points(0, 0).toList();
            assertEquals(2, points.size());
        }
    }

    @Nested
    class Adjacent8PointsTests {

        @Test
        public void testAdjacent8Points() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = m.adjacent8Points(1, 1).toList();
            assertEquals(8, points.size());
        }

        @Test
        public void testAdjacent8Points_Corner() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.adjacent8Points(0, 0).toList();
            assertEquals(3, points.size());
        }
    }

    @Nested
    class MainDiagonalPointsTests {

        // The outer array is a private snapshot, so replacing a row in either array is invisible
        @Test
        public void testMainDiagonalPoints() {
            ByteMatrix m = ByteMatrix.wrap(new byte[3][3]);
            List<Point> points = m.mainDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(2, 2), points.get(2));
        }

        @Test
        public void testMainDiagonalPoints_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = m.mainDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }
    }

    @Nested
    class AntiDiagonalPointsTests {

        @Test
        public void testAntiDiagonalPoints() {
            ByteMatrix m = ByteMatrix.wrap(new byte[3][3]);
            List<Point> points = m.antiDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(Point.of(0, 2), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(2, 0), points.get(2));
        }

        @Test
        public void testAntiDiagonalPoints_Shape3By3() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = m.antiDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(2, points.get(0).columnIndex());
        }
    }

    @Nested
    class RowMajorPointsTests {

        @Test
        public void testRowMajorPoints() {
            ByteMatrix m = ByteMatrix.wrap(new byte[2][2]);
            List<Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(0, 1), points.get(1));
            assertEquals(Point.of(1, 0), points.get(2));
        }

        @Test
        public void testRowMajorPoints_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }

        @Test
        public void testRowMajorPoints_SingleRow() {
            ByteMatrix m = ByteMatrix.wrap(new byte[2][3]);
            List<Point> points = m.rowMajorPoints(0, 0 + 1).toList();
            assertEquals(3, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(0, 2), points.get(2));
        }
    }

    @Nested
    class ColumnMajorPointsTests {

        @Test
        public void testColumnMajorPoints() {
            ByteMatrix m = ByteMatrix.wrap(new byte[2][2]);
            List<Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 0), points.get(1));
        }

        @Test
        public void testColumnMajorPoints_Shape2By2() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }

        @Test
        public void testColumnMajorPoints_SingleColumn() {
            ByteMatrix m = ByteMatrix.wrap(new byte[3][2]);
            List<Point> points = m.columnMajorPoints(0, 0 + 1).toList();
            assertEquals(3, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(2, 0), points.get(2));
        }
    }

    @Nested
    class RowPointsTests {

        @Test
        public void testRowPoints() {
            ByteMatrix m = ByteMatrix.wrap(new byte[2][2]);
            List<Stream<Point>> rows = m.rowPoints().toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).count());
        }
    }

    @Nested
    class ColumnPointsTests {

        @Test
        public void testColumnPoints() {
            ByteMatrix m = ByteMatrix.wrap(new byte[2][2]);
            List<Stream<Point>> columnCount = m.columnPoints().toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).count());
        }
    }

    @Nested
    class AcceptTests {

        @Test
        public void testAccept() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            AtomicInteger counter = new AtomicInteger(0);
            m.accept(matrix -> counter.set(matrix.rowCount() * matrix.columnCount()));
            assertEquals(2, counter.get());
        }
    }

    @Nested
    class ApplyTests {

        @Test
        public void testApply() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            int result = m.apply(matrix -> matrix.rowCount() * matrix.columnCount());
            assertEquals(2, result);
        }
    }

    @Nested
    class PrintlnTests {

        @Test
        public void testPrintln() {
            byte[][] a = { { 1, 2 }, { 3, 4 } };
            ByteMatrix matrix = ByteMatrix.wrap(a);

            assertFalse(matrix.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> matrix.println());
        }
    }
}
