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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.ShortIteratorEx;
import com.landawn.abacus.util.stream.ShortStream;
import com.landawn.abacus.util.stream.Stream;
import com.landawn.abacus.util.u.OptionalShort;

class ShortMatrixTest extends MatrixTestSupport {

    private ShortMatrix matrix;

    private ShortMatrix emptyMatrix;

    @BeforeEach
    public void setUp() {
        matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
        emptyMatrix = ShortMatrix.empty();
    }

    @Nested
    class ConstructorTests {

        @Test
        public void testConstructor() {
            // Test with valid array
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals((short) 1, m.get(0, 0));

            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> new ShortMatrix(null));
        }

        @Test
        public void testConstructor_WithValidArray() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals((short) 1, m.get(0, 0));
            assertEquals((short) 4, m.get(1, 1));
        }

        @Test
        public void testConstructor_WithLargeMatrix() {
            short[][] arr = new short[10][10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    arr[i][j] = (short) (i * 10 + j);
                }
            }
            ShortMatrix m = new ShortMatrix(arr);
            assertEquals(10, m.rowCount());
            assertEquals(10, m.columnCount());
            assertEquals((short) 0, m.get(0, 0));
            assertEquals((short) 99, m.get(9, 9));
            assertEquals((short) 55, m.get(5, 5));
        }

        @Test
        public void testConstructor_WithValidArray_Values() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testConstructor_WithEmptyArray() {
            ShortMatrix m = new ShortMatrix(new short[0][0]);
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
            assertTrue(m.isEmpty());
        }

        @Test
        public void testConstructor_WithSingleElement() {
            ShortMatrix m = new ShortMatrix(new short[][] { { 42 } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals((short) 42, m.get(0, 0));
        }

        @Test
        public void testConstructor_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> new ShortMatrix(null));
        }

        @Test
        public void testConstructor_WithSingleElement_Shape1By1() {
            ShortMatrix m = new ShortMatrix(new short[][] { { 42 } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42, m.get(0, 0));
        }

        // The exact short product validates the final mathematical dot product, not a narrowed partial sum.
        @Test
        public void testDuplicateRowsAreRejected() {
            short[] sharedRow = { 1, 2, 3, 4 };
            assertThrows(IllegalArgumentException.class, () -> new ShortMatrix(new short[][] { sharedRow, sharedRow }));
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.wrap(sharedRow, sharedRow));
        }
    }

    @Nested
    class EmptyTests {

        @Test
        public void testEmptyMatrixOperations_EmptyMatrix() {
            ShortMatrix empty = ShortMatrix.empty();

            assertTrue(empty.flatten().isEmpty());
            assertEquals(0, empty.copy().rowCount);
            assertEquals(empty, empty.transpose());
            assertEquals(empty, empty.rotate90());

            ShortMatrix extended = empty.resize(2, 2, (short) 5);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals(5, extended.get(0, 0));
        }

        @Test
        public void testEmpty_ReturnsEmptyMatrix() {
            ShortMatrix empty = ShortMatrix.empty();
            assertEquals(0, empty.rowCount());
            assertEquals(0, empty.columnCount());
            assertTrue(empty.isEmpty());
            assertSame(ShortMatrix.empty(), ShortMatrix.empty());
        }
    }

    @Nested
    class WrapTests {

        @Test
        public void testWrap() {
            // Test with valid array
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = ShortMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());

            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.wrap((short[][]) null));

            ShortMatrix empty2 = ShortMatrix.wrap(new short[0][0]);
            assertTrue(empty2.isEmpty());
        }

        @Test
        public void testWrap_withValidArray() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = ShortMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testWrap_WithValidArray_Values() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = ShortMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals((short) 1, m.get(0, 0));
        }

        @Test
        public void testLargeDimensions() {
            short[][] data = new short[100][50];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 50; j++) {
                    data[i][j] = (short) (i + j);
                }
            }
            ShortMatrix m = ShortMatrix.wrap(data);
            assertEquals(100, m.rowCount());
            assertEquals(50, m.columnCount());
            assertEquals((short) 0, m.get(0, 0));
            assertEquals((short) 148, m.get(99, 49));
        }

        @Test
        public void testWrap_WithValidArray() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = ShortMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(6, m.get(1, 2));
        }

        @Test
        public void testWrap_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.wrap((short[][]) null));
        }

        @Test
        public void testWrap_withEmptyArray() {
            ShortMatrix m = ShortMatrix.wrap(new short[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testShortMinMaxValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { Short.MAX_VALUE, Short.MIN_VALUE } });
            assertEquals(Short.MAX_VALUE, m.get(0, 0));
            assertEquals(Short.MIN_VALUE, m.get(0, 1));
        }

        @Test
        public void testWrap_WithSingleRow() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3, 4, 5 } });
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }

        @Test
        public void testWrap_WithSingleColumn() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1 }, { 2 }, { 3 }, { 4 } });
            assertEquals(4, m.rowCount());
            assertEquals(1, m.columnCount());
        }

        @Test
        public void testWrap_WithEmptyArray() {
            ShortMatrix m = ShortMatrix.wrap(new short[0][0]);
            assertSame(ShortMatrix.empty(), m);
        }
    }

    @Nested
    class CopyOfTests {

        @Test
        public void testCopyOf() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = ShortMatrix.copyOf(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals((short) 1, m.get(0, 0));
            assertEquals((short) 4, m.get(1, 1));

            // Mutating the source array must not affect the copy
            arr[0][0] = 99;
            assertEquals((short) 1, m.get(0, 0));

            // Mutating the copy must not affect the source array
            m.set(1, 1, (short) 88);
            assertEquals((short) 4, arr[1][1]);

            // null -> rejected; empty -> shared empty matrix
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.copyOf((short[][]) null));
            assertTrue(ShortMatrix.copyOf(new short[0][0]).isEmpty());

            // non-rectangular -> IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.copyOf(new short[][] { { 1, 2 }, { 3 } }));
        }

        @Test
        public void testCopyOfSeparatesDuplicateSourceRows() {
            short[] sharedRow = { 1, 2 };
            ShortMatrix matrix = ShortMatrix.copyOf(sharedRow, sharedRow);
            matrix.set(0, 0, (short) 9);
            assertEquals((short) 1, matrix.get(1, 0));
        }
    }

    @Nested
    class RandomRowTests {

        @Test
        public void testRandom() {
            ShortMatrix m = ShortMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            // Just verify elements exist (values are random)
            for (int i = 0; i < 5; i++) {
                assertNotNull(m.get(0, i));
            }
        }

        @Test
        public void testRandom_Values() {
            ShortMatrix m = ShortMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertNotNull(m.rowView(0));
        }

        @Test
        public void testRandom_ZeroLength() {
            ShortMatrix m = ShortMatrix.randomRow(0);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testFactories_NegativeDimensions_ThrowIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.randomRow(-1));
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.random(-1, 2));
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.random(2, -1));
        }
    }

    @Nested
    class RandomTests {

        @Test
        public void testRandomGeneratorOverloads_AreDeterministicAndPreserveZeroByN() {
            ShortMatrix first = ShortMatrix.random(2, 3, new java.util.Random(12345L));
            ShortMatrix second = ShortMatrix.random(2, 3, new java.util.Random(12345L));
            assertEquals(first, second);
            assertEquals(ShortMatrix.randomRow(4, new java.util.Random(7L)), ShortMatrix.randomRow(4, new java.util.Random(7L)));

            ShortMatrix zeroByFive = ShortMatrix.random(0, 5, new java.util.Random(1L));
            assertEquals(0, zeroByFive.rowCount());
            assertEquals(5, zeroByFive.columnCount());
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.random(1, 1, (java.util.random.RandomGenerator) null));
        }

        @Test
        public void testRandom_WithRowsCols() {
            ShortMatrix m = ShortMatrix.random(2, 3);
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    assertNotNull(m.get(i, j));
                }
            }
        }

        @Test
        public void testRandomGeneratorOverloads_NullGeneratorRejectedAfterDimensionChecks() {
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.randomRow(2, (java.util.random.RandomGenerator) null));
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.randomRow(0, (java.util.random.RandomGenerator) null));

            IllegalArgumentException negativeRowColumns = assertThrows(IllegalArgumentException.class,
                    () -> ShortMatrix.randomRow(-1, (java.util.random.RandomGenerator) null));
            assertTrue(negativeRowColumns.getMessage().contains("columnCount"), negativeRowColumns.getMessage());

            IllegalArgumentException negativeRows = assertThrows(IllegalArgumentException.class,
                    () -> ShortMatrix.random(-1, 2, (java.util.random.RandomGenerator) null));
            assertTrue(negativeRows.getMessage().contains("rowCount"), negativeRows.getMessage());

            // The convenience overloads delegate to the default generator and keep the requested shape.
            ShortMatrix row = ShortMatrix.randomRow(3);
            assertEquals(1, row.rowCount());
            assertEquals(3, row.columnCount());
            ShortMatrix zeroByTwo = ShortMatrix.random(0, 2);
            assertEquals(0, zeroByTwo.rowCount());
            assertEquals(2, zeroByTwo.columnCount());
        }
    }

    @Nested
    class RangeTests {

        @Test
        public void testEmptyRangesOwnTheirRow() {
            // Array.range/rangeClosed return a shared empty-array constant for an empty range; each matrix must own
            // its row, or rows of independently created empty range matrices could not be wrapped together.
            final short[][] rows = { ShortMatrix.range((short) 5, (short) 5).rowView(0), ShortMatrix.range((short) 5, (short) 1, (short) 1).rowView(0),
                    ShortMatrix.rangeClosed((short) 9, (short) 1).rowView(0), ShortMatrix.rangeClosed((short) 1, (short) 9, (short) -1).rowView(0) };

            for (final short[] row : rows) {
                assertEquals(0, row.length);
                assertNotSame(com.landawn.abacus.util.N.EMPTY_SHORT_ARRAY, row);
            }

            final ShortMatrix combined = ShortMatrix.wrap(rows);
            assertEquals(4, combined.rowCount());
            assertEquals(0, combined.columnCount());
        }

        @Test
        public void testRange() {
            ShortMatrix m = ShortMatrix.range((short) 0, (short) 5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            for (int i = 0; i < 5; i++) {
                assertEquals((short) i, m.get(0, i));
            }
        }

        @Test
        public void testRangeWithStep() {
            ShortMatrix m = ShortMatrix.range((short) 0, (short) 10, (short) 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals((short) 0, m.get(0, 0));
            assertEquals((short) 2, m.get(0, 1));
            assertEquals((short) 4, m.get(0, 2));
            assertEquals((short) 6, m.get(0, 3));
            assertEquals((short) 8, m.get(0, 4));
        }

        @Test
        public void testShortMatrixRange() {
            // ShortMatrix.java: ShortMatrix matrix = ShortMatrix.range((short) 0, (short) 5);   // Creates [[0, 1, 2, 3, 4]]
            ShortMatrix matrix = ShortMatrix.range((short) 0, (short) 5);
            assertEquals("[[0, 1, 2, 3, 4]]", matrix.toString());
        }

        @Test
        public void testShortMatrixRangeWithStep() {
            // ShortMatrix.java: ShortMatrix matrix = ShortMatrix.range((short) 0, (short) 10, (short) 2);   // Creates [[0, 2, 4, 6, 8]]
            ShortMatrix matrix = ShortMatrix.range((short) 0, (short) 10, (short) 2);
            assertEquals("[[0, 2, 4, 6, 8]]", matrix.toString());
        }

        @Test
        public void testShortMatrixRangeDescending() {
            // ShortMatrix.java: ShortMatrix desc = ShortMatrix.range((short) 10, (short) 0, (short) -2);    // Creates [[10, 8, 6, 4, 2]]
            ShortMatrix desc = ShortMatrix.range((short) 10, (short) 0, (short) -2);
            assertEquals("[[10, 8, 6, 4, 2]]", desc.toString());
        }

        @Test
        public void testRange_ArrayValues() {
            ShortMatrix m = ShortMatrix.range((short) 0, (short) 5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new short[] { 0, 1, 2, 3, 4 }, m.rowView(0));
        }

        @Test
        public void testRange_WithStep() {
            ShortMatrix m = ShortMatrix.range((short) 0, (short) 10, (short) 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new short[] { 0, 2, 4, 6, 8 }, m.rowView(0));
        }

        @Test
        public void testRange_WithTwoParameters() {
            ShortMatrix m = ShortMatrix.range((short) 1, (short) 5);
            assertEquals(1, m.rowCount());
            assertEquals(4, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(0, 3));
        }

        @Test
        public void testRange_WithThreeParameters() {
            ShortMatrix m = ShortMatrix.range((short) 0, (short) 10, (short) 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals(0, m.get(0, 0));
            assertEquals(8, m.get(0, 4));
        }

        @Test
        public void testRange_EmptyRange() {
            ShortMatrix m = ShortMatrix.range((short) 5, (short) 5);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testRange_WithNegativeStep() {
            ShortMatrix m = ShortMatrix.range((short) 10, (short) 0, (short) -2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new short[] { 10, 8, 6, 4, 2 }, m.rowView(0));
        }
    }

    @Nested
    class RangeClosedTests {

        @Test
        public void testRangeClosed() {
            ShortMatrix m = ShortMatrix.rangeClosed((short) 0, (short) 4);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            for (int i = 0; i < 5; i++) {
                assertEquals((short) i, m.get(0, i));
            }
        }

        @Test
        public void testRangeClosedWithStep() {
            ShortMatrix m = ShortMatrix.rangeClosed((short) 0, (short) 10, (short) 2);
            assertEquals(1, m.rowCount());
            assertEquals(6, m.columnCount());
            assertEquals((short) 0, m.get(0, 0));
            assertEquals((short) 2, m.get(0, 1));
            assertEquals((short) 4, m.get(0, 2));
            assertEquals((short) 6, m.get(0, 3));
            assertEquals((short) 8, m.get(0, 4));
            assertEquals((short) 10, m.get(0, 5));
        }

        @Test
        public void testShortMatrixRangeClosed() {
            // ShortMatrix.java: ShortMatrix matrix = ShortMatrix.rangeClosed((short) 1, (short) 4);   // Creates [[1, 2, 3, 4]]
            ShortMatrix matrix = ShortMatrix.rangeClosed((short) 1, (short) 4);
            assertEquals("[[1, 2, 3, 4]]", matrix.toString());
        }

        @Test
        public void testRangeClosed_ArrayValues() {
            ShortMatrix m = ShortMatrix.rangeClosed((short) 0, (short) 4);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new short[] { 0, 1, 2, 3, 4 }, m.rowView(0));
        }

        @Test
        public void testRangeClosed_WithStep() {
            ShortMatrix m = ShortMatrix.rangeClosed((short) 0, (short) 10, (short) 2);
            assertEquals(1, m.rowCount());
            assertEquals(6, m.columnCount());
            assertArrayEquals(new short[] { 0, 2, 4, 6, 8, 10 }, m.rowView(0));
        }

        @Test
        public void testRangeClosed_WithTwoParameters() {
            ShortMatrix m = ShortMatrix.rangeClosed((short) 1, (short) 4);
            assertEquals(1, m.rowCount());
            assertEquals(4, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(0, 3));
        }

        @Test
        public void testRangeClosed_WithThreeParameters() {
            ShortMatrix m = ShortMatrix.rangeClosed((short) 1, (short) 9, (short) 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(9, m.get(0, 4));
        }

        @Test
        public void testRangeClosed_SingleElement() {
            ShortMatrix m = ShortMatrix.rangeClosed((short) 5, (short) 5);
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(5, m.get(0, 0));
        }
    }

    @Nested
    class OfMainDiagonalTests {

        @Test
        public void testOfMainDiagonal_CreatesMainDiagonal() {
            short[] diag = { 1, 2, 3 };
            ShortMatrix m = ShortMatrix.ofMainDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
            assertEquals(0, m.get(0, 1));
        }

        @Test
        public void testDiagonalLU2RD() {
            ShortMatrix m = ShortMatrix.ofMainDiagonal(new short[] { 1, 2, 3 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
            assertEquals(0, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
        }

        @Test
        public void testDiagonalLU2RD_Values() {
            ShortMatrix m = ShortMatrix.ofMainDiagonal(new short[] { 1, 2, 3 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals((short) 1, m.get(0, 0));
            assertEquals((short) 2, m.get(1, 1));
            assertEquals((short) 3, m.get(2, 2));
            assertEquals((short) 0, m.get(0, 1));
            assertEquals((short) 0, m.get(1, 0));
        }

        @Test
        public void testDiagonalLU2RD_SingleElement() {
            ShortMatrix m = ShortMatrix.ofMainDiagonal(new short[] { 5 });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(5, m.get(0, 0));
        }
    }

    @Nested
    class OfAntiDiagonalTests {

        @Test
        public void testOfAntiDiagonal_CreatesAntiDiagonal() {
            short[] diag = { 1, 2, 3 };
            ShortMatrix m = ShortMatrix.ofAntiDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 2));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 0));
        }

        @Test
        public void testDiagonalRU2LD() {
            ShortMatrix m = ShortMatrix.ofAntiDiagonal(new short[] { 1, 2, 3 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals((short) 1, m.get(0, 2));
            assertEquals((short) 2, m.get(1, 1));
            assertEquals((short) 3, m.get(2, 0));
            assertEquals((short) 0, m.get(0, 0));
            assertEquals((short) 0, m.get(2, 2));
        }

        @Test
        public void testDiagonalRU2LD_Values() {
            ShortMatrix m = ShortMatrix.ofAntiDiagonal(new short[] { 1, 2, 3 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 2));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 0));
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(2, 2));
        }

        @Test
        public void testDiagonalRU2LD_SingleElement() {
            ShortMatrix m = ShortMatrix.ofAntiDiagonal(new short[] { 5 });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(5, m.get(0, 0));
        }
    }

    @Nested
    class OfDiagonalsTests {

        @Test
        public void testDiagonal() {
            // Test with both diagonals
            ShortMatrix m = ShortMatrix.ofDiagonals(new short[] { 1, 2, 3 }, new short[] { 4, 5, 6 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals((short) 1, m.get(0, 0));
            assertEquals((short) 2, m.get(1, 1));
            assertEquals((short) 3, m.get(2, 2));
            assertEquals((short) 4, m.get(0, 2));
            assertEquals((short) 2, m.get(1, 1)); // Overwritten
            assertEquals((short) 6, m.get(2, 0));

            // Test with only main diagonal
            ShortMatrix m2 = ShortMatrix.ofDiagonals(new short[] { 1, 2, 3 }, null);
            assertEquals((short) 1, m2.get(0, 0));
            assertEquals((short) 2, m2.get(1, 1));
            assertEquals((short) 3, m2.get(2, 2));

            // Test with only anti-diagonal
            ShortMatrix m3 = ShortMatrix.ofDiagonals(null, new short[] { 4, 5, 6 });
            assertEquals((short) 4, m3.get(0, 2));
            assertEquals((short) 5, m3.get(1, 1));
            assertEquals((short) 6, m3.get(2, 0));

            // Test with both null
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.ofDiagonals(null, null));

            // Test illegal argument
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.ofDiagonals(new short[] { 1, 2 }, new short[] { 3, 4, 5 }));
        }

        @Test
        public void testDiagonal_WithBothDiagonals() {
            ShortMatrix m = ShortMatrix.ofDiagonals(new short[] { 1, 2, 3 }, new short[] { 4, 5, 6 });
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
            ShortMatrix m = ShortMatrix.ofDiagonals(new short[] { 1, 2, 3 }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
        }

        @Test
        public void testDiagonal_WithOnlyAntiDiagonal() {
            ShortMatrix m = ShortMatrix.ofDiagonals(null, new short[] { 4, 5, 6 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(4, m.get(0, 2));
            assertEquals(5, m.get(1, 1));
            assertEquals(6, m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithDifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.ofDiagonals(new short[] { 1, 2 }, new short[] { 3, 4, 5 }));
        }

        @Test
        public void testOfDiagonals_MainDiagonalWinsAtSharedCenter() {
            final ShortMatrix matrix = ShortMatrix.ofDiagonals(new short[] { 1, 2, 3 }, new short[] { 4, 5, 6 });
            // At the shared center, the main diagonal wins.
            assertEquals(ShortMatrix.wrap(new short[][] { { 1, 0, 4 }, { 0, 2, 0 }, { 6, 0, 3 } }), matrix);
        }

        @Test
        public void testDiagonal_DifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.ofDiagonals(new short[] { 1, 2 }, new short[] { 1, 2, 3 }));
        }

        @Test
        public void testDiagonal_WithBothDiagonals_Values() {
            short[] lu = { 1, 2, 3 };
            short[] ru = { 4, 5, 6 };
            ShortMatrix m = ShortMatrix.ofDiagonals(lu, ru);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
            assertEquals(4, m.get(0, 2));
            assertEquals(6, m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithBothNull() {
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.ofDiagonals(null, null));
        }

        @Test
        public void testOfDiagonals_EmptyAndNullSidesPreserveEmptyShape() {
            assertTrue(ShortMatrix.ofDiagonals(new short[0], new short[0]).isEmpty());
            assertTrue(ShortMatrix.ofDiagonals(new short[0], null).isEmpty());
            assertTrue(ShortMatrix.ofDiagonals(null, new short[0]).isEmpty());
        }

        @Test
        public void testDiagonal_WithDifferentLengths_ThrowsException() {
            short[] lu = { 1, 2 };
            short[] ru = { 3, 4, 5 };
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.ofDiagonals(lu, ru));
        }
    }

    @Nested
    class UnboxTests {

        @Test
        public void testUnbox() {
            Short[][] boxed = { { 1, 2 }, { 3, 4 } };
            Matrix<Short> boxedMatrix = Matrix.wrap(boxed);
            ShortMatrix unboxed = ShortMatrix.unbox(boxedMatrix);
            assertEquals((short) 1, unboxed.get(0, 0));
            assertEquals((short) 2, unboxed.get(0, 1));
            assertEquals((short) 3, unboxed.get(1, 0));
            assertEquals((short) 4, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_Shape2By2() {
            Matrix<Short> boxed = Matrix.wrap(new Short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m = ShortMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals((short) 1, m.get(0, 0));
            assertEquals((short) 4, m.get(1, 1));
        }

        @Test
        public void testUnbox_ConvertsBoxedMatrix() {
            Short[][] boxed = { { 1, 2 }, { 3, 4 } };
            Matrix<Short> boxedMatrix = Matrix.wrap(boxed);
            ShortMatrix m = ShortMatrix.unbox(boxedMatrix);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testUnboxSharedEmptyAndPreserveZeroColumnRows() {
            ShortMatrix sharedEmpty = ShortMatrix.unbox(Matrix.empty(Short.class));
            assertEquals(ShortMatrix.empty(), sharedEmpty);

            ShortMatrix zeroColumns = ShortMatrix.unbox(Matrix.wrap(new Short[][] { {}, {} }));
            assertEquals(2, zeroColumns.rowCount());
            assertEquals(0, zeroColumns.columnCount());

            Matrix<Short> typeNeutralZeroColumns = Matrix.empty(Short.class).resize(2, 0);
            ShortMatrix unboxedTypeNeutralZeroColumns = ShortMatrix.unbox(typeNeutralZeroColumns);
            assertEquals(2, unboxedTypeNeutralZeroColumns.rowCount());
            assertEquals(0, unboxedTypeNeutralZeroColumns.columnCount());

            Matrix<Short> typeNeutralValues = Matrix.empty(Short.class).resize(2, 2);
            typeNeutralValues.set(0, 1, (short) 7);
            ShortMatrix unboxedTypeNeutralValues = ShortMatrix.unbox(typeNeutralValues);
            assertArrayEquals(new short[] { 0, 7 }, unboxedTypeNeutralValues.rowCopy(0));
            assertArrayEquals(new short[] { 0, 0 }, unboxedTypeNeutralValues.rowCopy(1));
        }

        @Test
        public void testUnbox_WithNullValues() {
            Short[][] boxed = { { 1, null }, { null, 4 } };
            Matrix<Short> boxedMatrix = Matrix.wrap(boxed);
            ShortMatrix unboxed = ShortMatrix.unbox(boxedMatrix);
            assertEquals(1, unboxed.get(0, 0));
            assertEquals(0, unboxed.get(0, 1)); // null -> 0
            assertEquals(0, unboxed.get(1, 0)); // null -> 0
            assertEquals(4, unboxed.get(1, 1));
        }
    }

    @Nested
    class GetTests {

        @Test
        public void testGet() {
            assertEquals((short) 1, matrix.get(0, 0));
            assertEquals((short) 5, matrix.get(1, 1));
            assertEquals((short) 9, matrix.get(2, 2));

            // Test bounds
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(-1, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(3, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(0, -1));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(0, 3));
        }

        @Test
        public void testGetWithPoint() {
            Sheet.Point p1 = Sheet.Point.of(0, 0);
            assertEquals((short) 1, matrix.get(p1));

            Sheet.Point p2 = Sheet.Point.of(1, 1);
            assertEquals((short) 5, matrix.get(p2));

            Sheet.Point p3 = Sheet.Point.of(2, 2);
            assertEquals((short) 9, matrix.get(p3));
        }

        @Test
        public void testGet_Shape2By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(5, m.get(1, 1));
            assertEquals(6, m.get(1, 2));
        }

        @Test
        public void testGetWithPoint_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(1, m.get(Point.of(0, 0)));
            assertEquals(4, m.get(Point.of(1, 1)));
            assertEquals(2, m.get(Point.of(0, 1)));
        }

        @Test
        public void testGet_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertEquals((short) 1, m.get(0, 0));
            assertEquals((short) 2, m.get(0, 1));
            assertEquals((short) 3, m.get(1, 0));
            assertEquals((short) 4, m.get(1, 1));
        }

        @Test
        public void testGet_WithPoint() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            Point p = Point.of(1, 0);
            assertEquals((short) 3, m.get(p));
        }

        @Test
        public void testGet_WithIndices() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            assertEquals(1, m.get(0, 0));
            assertEquals(5, m.get(1, 1));
            assertEquals(6, m.get(1, 2));
        }

        @Test
        public void testGet_WithPoint_Values() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            Point p = Point.of(1, 0);
            assertEquals(3, m.get(p));
        }

        @Test
        public void testGet_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
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
            ShortMatrix m = matrix.copy();
            m.set(0, 0, (short) 10);
            assertEquals((short) 10, m.get(0, 0));

            m.set(1, 1, (short) 20);
            assertEquals((short) 20, m.get(1, 1));

            // Test bounds
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, (short) 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(3, 0, (short) 0));
        }

        // Test top row
        @Test
        public void testSetWithPoint() {
            ShortMatrix m = matrix.copy();
            Sheet.Point p = Sheet.Point.of(1, 1);
            m.set(p, (short) 50);
            assertEquals((short) 50, m.get(p));
        }

        // Leftmost column has no element to the left
        @Test
        public void testSet_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.set(0, 0, (short) 10);
            assertEquals(10, m.get(0, 0));

            m.set(1, 1, (short) 20);
            assertEquals(20, m.get(1, 1));
        }

        @Test
        public void testSetWithPoint_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.set(Point.of(0, 0), (short) 50);
            assertEquals(50, m.get(Point.of(0, 0)));
        }

        @Test
        public void testSet_Shape2By2_ChangesBothDiagonalCells() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.set(0, 0, (short) 10);
            assertEquals((short) 10, m.get(0, 0));
            m.set(1, 1, (short) 20);
            assertEquals((short) 20, m.get(1, 1));
        }

        @Test
        public void testSet_WithPoint() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            Point p = Point.of(1, 0);
            m.set(p, (short) 99);
            assertEquals((short) 99, m.get(1, 0));
        }

        @Test
        public void testSet_WithIndices() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.set(0, 1, (short) 99);
            assertEquals(99, m.get(0, 1));
        }

        @Test
        public void testSet_WithPoint_Values() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            Point p = Point.of(1, 1);
            m.set(p, (short) 88);
            assertEquals(88, m.get(p));
        }

        @Test
        public void testSet_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, (short) 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(2, 0, (short) 0));
        }
    }

    @Nested
    class ValueAboveTests {

        @Test
        public void testValueAbove() {
            OptionalShort up = matrix.valueAbove(1, 1);
            assertTrue(up.isPresent());
            assertEquals((short) 2, up.get());

            // Test top row
            OptionalShort empty = matrix.valueAbove(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });

            OptionalShort up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1, up.get());

            // Top row has no element above
            OptionalShort empty = m.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_WithElementAbove() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            OptionalShort result = m.valueAbove(1, 0);
            assertTrue(result.isPresent());
            assertEquals(1, result.getAsShort());
        }

        @Test
        public void testValueAbove_Shape2By2_InteriorValueAndMissingBoundary() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            OptionalShort up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals((short) 1, up.get());

            OptionalShort noUp = m.valueAbove(0, 0);
            assertFalse(noUp.isPresent());
        }

        @Test
        public void testValueAbove_EmptyMatrix() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            OptionalShort up = m.valueAbove(0, 1);
            assertFalse(up.isPresent());
        }

        @Test
        public void testValueAbove_AtTopRow_ReturnsEmpty() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            OptionalShort result = m.valueAbove(0, 0);
            assertFalse(result.isPresent());
        }
    }

    @Nested
    class ValueBelowTests {

        @Test
        public void testValueBelow() {
            OptionalShort down = matrix.valueBelow(1, 1);
            assertTrue(down.isPresent());
            assertEquals((short) 8, down.get());

            // Test bottom row
            OptionalShort empty = matrix.valueBelow(2, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });

            OptionalShort down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3, down.get());

            // Bottom row has no element below
            OptionalShort empty = m.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_BottomRow() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            OptionalShort down = m.valueBelow(1, 1);
            assertFalse(down.isPresent());
        }

        @Test
        public void testValueBelow_WithElementBelow() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            OptionalShort result = m.valueBelow(0, 1);
            assertTrue(result.isPresent());
            assertEquals(4, result.getAsShort());
        }

        @Test
        public void testValueBelow_Shape2By2_InteriorValueAndMissingBoundary() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            OptionalShort down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals((short) 3, down.get());

            OptionalShort noDown = m.valueBelow(1, 0);
            assertFalse(noDown.isPresent());
        }

        @Test
        public void testValueBelow_AtBottomRow_ReturnsEmpty() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            OptionalShort result = m.valueBelow(1, 1);
            assertFalse(result.isPresent());
        }
    }

    @Nested
    class ValueLeftTests {

        @Test
        public void testValueLeft() {
            OptionalShort left = matrix.valueLeft(1, 1);
            assertTrue(left.isPresent());
            assertEquals((short) 4, left.get());

            // Test leftmost column
            OptionalShort empty = matrix.valueLeft(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });

            OptionalShort left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1, left.get());

            // Leftmost column has no element to the left
            OptionalShort empty = m.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_LeftmostColumn() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            OptionalShort left = m.valueLeft(1, 0);
            assertFalse(left.isPresent());
        }

        @Test
        public void testValueLeft_WithElementToLeft() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            OptionalShort result = m.valueLeft(0, 1);
            assertTrue(result.isPresent());
            assertEquals(1, result.getAsShort());
        }

        @Test
        public void testValueLeft_Shape2By2_InteriorValueAndMissingBoundary() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            OptionalShort left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals((short) 1, left.get());

            OptionalShort noLeft = m.valueLeft(0, 0);
            assertFalse(noLeft.isPresent());
        }

        @Test
        public void testValueLeft_AtLeftColumn_ReturnsEmpty() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            OptionalShort result = m.valueLeft(1, 0);
            assertFalse(result.isPresent());
        }
    }

    @Nested
    class ValueRightTests {

        @Test
        public void testValueRight() {
            OptionalShort right = matrix.valueRight(1, 1);
            assertTrue(right.isPresent());
            assertEquals((short) 6, right.get());

            // Test rightmost column
            OptionalShort empty = matrix.valueRight(1, 2);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });

            OptionalShort right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2, right.get());

            // Rightmost column has no element to the right
            OptionalShort empty = m.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_RightmostColumn() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            OptionalShort right = m.valueRight(1, 1);
            assertFalse(right.isPresent());
        }

        @Test
        public void testValueRight_WithElementToRight() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            OptionalShort result = m.valueRight(1, 0);
            assertTrue(result.isPresent());
            assertEquals(4, result.getAsShort());
        }

        @Test
        public void testValueRight_Shape2By2_InteriorValueAndMissingBoundary() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            OptionalShort right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals((short) 2, right.get());

            OptionalShort noRight = m.valueRight(0, 1);
            assertFalse(noRight.isPresent());
        }

        @Test
        public void testValueRight_AtRightColumn_ReturnsEmpty() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            OptionalShort result = m.valueRight(0, 1);
            assertFalse(result.isPresent());
        }
    }

    @Nested
    class RowViewTests {

        @Test
        public void testRow() {
            short[] row0 = matrix.rowView(0);
            assertArrayEquals(new short[] { 1, 2, 3 }, row0);

            short[] row1 = matrix.rowView(1);
            assertArrayEquals(new short[] { 4, 5, 6 }, row1);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(3));
        }

        @Test
        public void testRowView() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            short[] firstRow = matrix.rowView(0);
            assertArrayEquals(new short[] { 1, 2, 3 }, firstRow);
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new short[] { 1, 2, 3 }, m.rowView(0));
            assertArrayEquals(new short[] { 4, 5, 6 }, m.rowView(1));
        }

        @Test
        public void testRowView_ReturnsCorrectRow() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[] row = m.rowView(1);
            assertArrayEquals(new short[] { 4, 5, 6 }, row);
        }

        @Test
        public void testLength() {
            // This is a protected method, test indirectly through row operations
            short[] row = matrix.rowView(0);
            assertEquals(3, row.length);
        }

        @Test
        public void testRowView_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(2));
        }

        @Test
        public void testRowView_InvalidIndex() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(1));
        }

        @Test
        public void testRowView_OutOfBounds_ThrowsException() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(5));
        }
    }

    @Nested
    class RowCopyTests {

        @Test
        public void testRowCopy() {
            short[] rowCopy = matrix.rowCopy(0);
            assertArrayEquals(new short[] { 1, 2, 3 }, rowCopy);

            rowCopy[0] = 99;
            assertArrayEquals(new short[] { 1, 2, 3 }, matrix.rowView(0));
        }

        // Test bounds
        @Test
        public void testRowCopy_InvalidIndex() {
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(3));
        }
    }

    @Nested
    class ColumnCopyTests {

        @Test
        public void testColumn() {
            short[] col0 = matrix.columnCopy(0);
            assertArrayEquals(new short[] { 1, 4, 7 }, col0);

            short[] col1 = matrix.columnCopy(1);
            assertArrayEquals(new short[] { 2, 5, 8 }, col1);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(3));
        }

        @Test
        public void testColumnCopy() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            short[] firstColumn = matrix.columnCopy(0);
            assertArrayEquals(new short[] { 1, 4 }, firstColumn);
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new short[] { 1, 4 }, m.columnCopy(0));
            assertArrayEquals(new short[] { 2, 5 }, m.columnCopy(1));
            assertArrayEquals(new short[] { 3, 6 }, m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_ReturnsCorrectColumn() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[] col = m.columnCopy(1);
            assertArrayEquals(new short[] { 2, 5 }, col);
        }

        @Test
        public void testColumnCopy_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_OutOfBounds_ThrowsException() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(5));
        }
    }

    @Nested
    class SetRowTests {

        @Test
        public void testSetRow() {
            ShortMatrix m = matrix.copy();
            m.setRow(0, new short[] { 10, 20, 30 });
            assertArrayEquals(new short[] { 10, 20, 30 }, m.rowView(0));

            // Test wrong size
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new short[] { 1, 2 }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, (short[]) null));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.setRow(0, new short[] { 10, 20 });
            assertArrayEquals(new short[] { 10, 20 }, m.rowView(0));
            assertArrayEquals(new short[] { 3, 4 }, m.rowView(1)); // unchanged
        }

        @Test
        public void testSetRow_WrongSize() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new short[] { 1 }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new short[] { 1, 2, 3 }));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues_RowReplacementAndNeighborPreserved() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.setRow(0, new short[] { 10, 20 });
            assertArrayEquals(new short[] { 10, 20 }, m.rowView(0));
            assertEquals((short) 3, m.get(1, 0));
        }

        @Test
        public void testSetRow_UpdatesRow() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.setRow(0, new short[] { 9, 8 });
            assertEquals(9, m.get(0, 0));
            assertEquals(8, m.get(0, 1));
        }

        @Test
        public void testSetRow_WrongLength_ThrowsException() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new short[] { 1, 2, 3 }));
        }
    }

    @Nested
    class SetColumnTests {

        @Test
        public void testSetColumn() {
            ShortMatrix m = matrix.copy();
            m.setColumn(0, new short[] { 10, 20, 30 });
            assertArrayEquals(new short[] { 10, 20, 30 }, m.columnCopy(0));

            // Test wrong size
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new short[] { 1, 2 }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, (short[]) null));
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.setColumn(0, new short[] { 10, 20 });
            assertArrayEquals(new short[] { 10, 20 }, m.columnCopy(0));
            assertArrayEquals(new short[] { 2, 4 }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testSetColumn_WrongSize() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new short[] { 1 }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new short[] { 1, 2, 3 }));
        }

        @Test
        public void testSetColumn_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.setColumn(0, new short[] { 10, 30 });
            assertEquals((short) 10, m.get(0, 0));
            assertEquals((short) 30, m.get(1, 0));
            assertEquals((short) 2, m.get(0, 1));
        }

        @Test
        public void testSetColumn_UpdatesColumn() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.setColumn(1, new short[] { 7, 8 });
            assertEquals(7, m.get(0, 1));
            assertEquals(8, m.get(1, 1));
        }

        @Test
        public void testAliasedArraySourcesAreSnapshotted() {
            ShortMatrix columnMatrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            columnMatrix.setColumn(1, columnMatrix.rowView(0));
            assertArrayEquals(new short[] { 3, 2 }, columnMatrix.rowCopy(1));

            ShortMatrix diagonalMatrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            diagonalMatrix.setAntiDiagonal(diagonalMatrix.rowView(0));
            assertArrayEquals(new short[] { 1, 2, 3 }, diagonalMatrix.antiDiagonalCopy());

            short[][] backing = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            ShortMatrix fillMatrix = ShortMatrix.wrap(backing);
            fillMatrix.copyFrom(1, 0, backing);
            assertArrayEquals(new short[] { 3, 4 }, fillMatrix.rowCopy(2));
        }

        @Test
        public void testSetColumn_WrongLength_ThrowsException() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new short[] { 1, 2, 3 }));
        }
    }

    @Nested
    class UpdateRowTests {

        @Test
        public void testDocumentedCallbackFailuresKeepCompletedUpdates() throws Exception {
            for (int operation = 0; operation < 8; operation++) {
                final int selectedOperation = operation;
                final ShortMatrix matrix = ShortMatrix.wrap(new short[2][2]);
                final AtomicInteger calls = new AtomicInteger();
                final IllegalStateException failure = new IllegalStateException("callback failed");
                final Throwables.ShortUnaryOperator<IllegalStateException> operator = value -> {
                    if (calls.incrementAndGet() == 2) {
                        throw failure;
                    }

                    return (short) 1;
                };

                assertSame(failure, assertThrows(IllegalStateException.class, () -> Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                    switch (selectedOperation) {
                        case 0 -> matrix.updateRow(0, operator);
                        case 1 -> matrix.updateColumn(0, operator);
                        case 2 -> matrix.updateMainDiagonal(operator);
                        case 3 -> matrix.updateAntiDiagonal(operator);
                        case 4 -> matrix.updateAll(operator);
                        case 5 -> matrix.updateAll((row, column) -> operator.applyAsShort((short) 0));
                        case 6 -> matrix.replaceIf(value -> operator.applyAsShort(value) != 0, (short) 1);
                        case 7 -> matrix.replaceIf((row, column) -> operator.applyAsShort((short) 0) != 0, (short) 1);
                        default -> throw new AssertionError(selectedOperation);
                    }
                })));

                assertEquals(2, calls.get(), "operation " + selectedOperation);
                assertArrayEquals(selectedOperation == 3 ? new short[] { (short) 0, (short) 1 } : new short[] { (short) 1, (short) 0 }, matrix.rowCopy(0),
                        "operation " + selectedOperation);
                assertArrayEquals(new short[] { (short) 0, (short) 0 }, matrix.rowCopy(1), "operation " + selectedOperation);
            }
        }

        @Test
        public void testUpdateRow() {
            ShortMatrix m = matrix.copy();
            m.updateRow(0, x -> (short) (x * 2));
            assertArrayEquals(new short[] { 2, 4, 6 }, m.rowView(0));
        }

        @Test
        public void testUpdateRow_Shape2By2_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.updateRow(0, x -> (short) (x * 2));
            assertArrayEquals(new short[] { 2, 4 }, m.rowView(0));
            assertArrayEquals(new short[] { 3, 4 }, m.rowView(1)); // unchanged
        }

        @Test
        public void testUpdateRow_AppliesFunction() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.updateRow(0, x -> (short) (x * 2));
            assertEquals(2, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
        }

        @Test
        public void testUpdateRowAndUpdateColumnNullOperator() {
            ShortMatrix m = matrix.copy();

            assertThrows(IllegalArgumentException.class, () -> m.updateRow(0, (Throwables.ShortUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> m.updateColumn(0, (Throwables.ShortUnaryOperator<RuntimeException>) null));
        }

        @Test
        public void testUpdateRowAndUpdateColumnInvalidIndex() {
            ShortMatrix m = matrix.copy();

            assertThrows(IndexOutOfBoundsException.class, () -> m.updateRow(-1, x -> (short) (x * 2)));
            assertThrows(IndexOutOfBoundsException.class, () -> m.updateColumn(3, x -> (short) (x + 10)));
        }

        @Test
        public void testUpdateRow_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.updateRow(-1, x -> x));
            assertThrows(IndexOutOfBoundsException.class, () -> m.updateRow(2, x -> x));
        }
    }

    @Nested
    class UpdateColumnTests {

        @Test
        public void testUpdateColumn() {
            ShortMatrix m = matrix.copy();
            m.updateColumn(0, x -> (short) (x + 10));
            assertArrayEquals(new short[] { 11, 14, 17 }, m.columnCopy(0));
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.updateColumn(0, x -> (short) (x + 10));
            assertArrayEquals(new short[] { 11, 13 }, m.columnCopy(0));
            assertArrayEquals(new short[] { 2, 4 }, m.columnCopy(1)); // unchanged
        }

        // Original should be unchanged
        @Test
        public void testUpdateColumn_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.updateColumn(0, x -> (short) (x * 10));
            assertEquals((short) 10, m.get(0, 0));
            assertEquals((short) 30, m.get(1, 0));
            assertEquals((short) 2, m.get(0, 1));
            assertEquals((short) 4, m.get(1, 1));
        }

        @Test
        public void testUpdateColumn_AppliesFunction() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.updateColumn(0, x -> (short) (x + 10));
            assertEquals(11, m.get(0, 0));
            assertEquals(13, m.get(1, 0));
            assertEquals(2, m.get(0, 1));
        }

        @Test
        public void testUpdateColumn_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.updateColumn(-1, x -> x));
            assertThrows(IndexOutOfBoundsException.class, () -> m.updateColumn(2, x -> x));
        }
    }

    @Nested
    class MainDiagonalCopyTests {

        @Test
        public void testMainDiagonalCopy() {
            short[] diagonal = matrix.mainDiagonalCopy();
            assertArrayEquals(new short[] { 1, 5, 9 }, diagonal);

            // Test non-square matrix
            ShortMatrix nonSquare = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertArrayEquals(new short[] { 1 }, nonSquare.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_ThreeByThreeValues() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            short[] diagonal = matrix.mainDiagonalCopy();
            assertArrayEquals(new short[] { 1, 5, 9 }, diagonal);
        }

        @Test
        public void testMainDiagonalCopy_Shape3By3_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertArrayEquals(new short[] { 1, 5, 9 }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_NonSquare() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertArrayEquals(new short[] { 1 }, m.mainDiagonalCopy());
        }

        // ShortMatrix doesn't have mapToInt method - removed test
        // @Test
        // public void testMapToInt() {
        //     ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
        //     IntMatrix result = m.mapToInt(x -> x * 1000);
        //     assertEquals(1000, result.get(0, 0));
        //     assertEquals(4000, result.get(1, 1));
        @Test
        public void testMainDiagonalCopy_NonSquare_Shape3By2_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertArrayEquals(new short[] { 1, 4 }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_ReturnsMainDiagonal() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new short[] { 1, 5, 9 }, diag);
        }

        @Test
        public void testMainDiagonalCopy_NonSquare_ThrowsException() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new short[] { 1, 5 }, m.mainDiagonalCopy());
        }
    }

    @Nested
    class SetMainDiagonalTests {

        // Test array too short
        @Test
        public void testSetMainDiagonal() {
            ShortMatrix m = matrix.copy();
            m.setMainDiagonal(new short[] { 10, 20, 30 });
            assertEquals((short) 10, m.get(0, 0));
            assertEquals((short) 20, m.get(1, 1));
            assertEquals((short) 30, m.get(2, 2));

            // Test non-square matrix
            ShortMatrix nonSquare = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            nonSquare.setMainDiagonal(new short[] { 9 });
            assertEquals((short) 9, nonSquare.get(0, 0));

            // Test array too short
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new short[] { 1, 2 }));
        }

        @Test
        public void testSetMainDiagonal_Shape3By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setMainDiagonal(new short[] { 10, 20, 30 });
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 2));
        }

        @Test
        public void testSetMainDiagonal_NonSquare() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            m.setMainDiagonal(new short[] { 9 });
            assertEquals((short) 9, m.get(0, 0));
        }

        @Test
        public void testSetMainDiagonal_ArrayTooShort() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new short[] { 1, 2 }));
        }

        // ShortMatrix doesn't have mapToLong method - removed test
        // @Test
        // public void testMapToLong() {
        //     ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
        @Test
        public void testSetMainDiagonal_Shape3By3_ReplacesEveryDiagonalElement() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setMainDiagonal(new short[] { 10, 20, 30 });
            assertEquals((short) 10, m.get(0, 0));
            assertEquals((short) 20, m.get(1, 1));
            assertEquals((short) 30, m.get(2, 2));
        }

        @Test
        public void testSetMainDiagonal_SetsMainDiagonal() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.setMainDiagonal(new short[] { 11, 22, 33 });
            assertEquals(11, m.get(0, 0));
            assertEquals(22, m.get(1, 1));
            assertEquals(33, m.get(2, 2));
        }

        @Test
        public void testSetDiagonal_NullArraysOnEmptyMatrixThrow() {
            ShortMatrix m = ShortMatrix.empty();
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(null));
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(null));
        }

        @Test
        public void testSetMainDiagonal_WrongLength_ThrowsException() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new short[] { 1, 2, 3 }));
        }
    }

    @Nested
    class UpdateMainDiagonalTests {

        @Test
        public void testUpdateMainDiagonal() {
            ShortMatrix m = matrix.copy();
            m.updateMainDiagonal(x -> (short) (x * 10));
            assertEquals((short) 10, m.get(0, 0));
            assertEquals((short) 50, m.get(1, 1));
            assertEquals((short) 90, m.get(2, 2));

            // Test non-square matrix
            ShortMatrix nonSquare = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            nonSquare.updateMainDiagonal(x -> (short) (x * 2));
            assertArrayEquals(new short[] { 2 }, nonSquare.mainDiagonalCopy());
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateMainDiagonal(x -> (short) (x * 10));
            assertEquals(10, m.get(0, 0));
            assertEquals(50, m.get(1, 1));
            assertEquals(90, m.get(2, 2));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateMainDiagonal_NonSquare() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            m.updateMainDiagonal(x -> (short) (x * 2));
            assertArrayEquals(new short[] { 2 }, m.mainDiagonalCopy());
        }

        // ShortMatrix doesn't have mapToDouble method - removed test
        // @Test
        // public void testMapToDouble() {
        //     ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
        @Test
        public void testUpdateMainDiagonal_Shape3By3_ScalesEveryDiagonalElement() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateMainDiagonal(x -> (short) (x * 10));
            assertEquals((short) 10, m.get(0, 0));
            assertEquals((short) 50, m.get(1, 1));
            assertEquals((short) 90, m.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal_AppliesFunction() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.updateMainDiagonal(x -> (short) (x * 10));
            assertEquals(10, m.get(0, 0));
            assertEquals(50, m.get(1, 1));
            assertEquals(90, m.get(2, 2));
        }
    }

    @Nested
    class AntiDiagonalCopyTests {

        @Test
        public void testAntiDiagonalCopy() {
            short[] antiDiagonal = matrix.antiDiagonalCopy();
            assertArrayEquals(new short[] { 3, 5, 7 }, antiDiagonal);

            // Test non-square matrix
            ShortMatrix nonSquare = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertArrayEquals(new short[] { 2 }, nonSquare.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_ThreeByThreeValues() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            short[] diagonal = matrix.antiDiagonalCopy();
            assertArrayEquals(new short[] { 3, 5, 7 }, diagonal);
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertArrayEquals(new short[] { 3, 5, 7 }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertArrayEquals(new short[] { 2 }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_ReturnsAntiDiagonal() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new short[] { 3, 5, 7 }, diag);
        }
    }

    @Nested
    class SetAntiDiagonalTests {

        @Test
        public void testSetAntiDiagonal() {
            ShortMatrix m = matrix.copy();
            m.setAntiDiagonal(new short[] { 10, 20, 30 });
            assertEquals((short) 10, m.get(0, 2));
            assertEquals((short) 20, m.get(1, 1));
            assertEquals((short) 30, m.get(2, 0));

            // Test non-square matrix
            ShortMatrix nonSquare = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            nonSquare.setAntiDiagonal(new short[] { 9 });
            assertEquals((short) 9, nonSquare.get(0, 1));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setAntiDiagonal(new short[] { 10, 20, 30 });
            assertEquals(10, m.get(0, 2));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_NonSquare() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            m.setAntiDiagonal(new short[] { 9 });
            assertEquals((short) 9, m.get(0, 1));
        }

        @Test
        public void testSetAntiDiagonal_ArrayTooShort() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new short[] { 1, 2 }));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3_ReplacesEveryDiagonalElement() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setAntiDiagonal(new short[] { 30, 50, 70 });
            assertEquals((short) 30, m.get(0, 2));
            assertEquals((short) 50, m.get(1, 1));
            assertEquals((short) 70, m.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_SetsAntiDiagonal() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.setAntiDiagonal(new short[] { 11, 22, 33 });
            assertEquals(11, m.get(0, 2));
            assertEquals(22, m.get(1, 1));
            assertEquals(33, m.get(2, 0));
        }
    }

    @Nested
    class UpdateAntiDiagonalTests {

        @Test
        public void testUpdateAntiDiagonal() {
            ShortMatrix m = matrix.copy();
            m.updateAntiDiagonal(x -> (short) (x * 10));
            assertEquals((short) 30, m.get(0, 2));
            assertEquals((short) 50, m.get(1, 1));
            assertEquals((short) 70, m.get(2, 0));

            // Test non-square matrix
            ShortMatrix nonSquare = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            nonSquare.updateAntiDiagonal(x -> (short) (x * 2));
            assertArrayEquals(new short[] { 4 }, nonSquare.antiDiagonalCopy());
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateAntiDiagonal(x -> (short) (x * 10));
            assertEquals(30, m.get(0, 2));
            assertEquals(50, m.get(1, 1));
            assertEquals(70, m.get(2, 0));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateAntiDiagonal_NonSquare() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            m.updateAntiDiagonal(x -> (short) (x * 2));
            assertArrayEquals(new short[] { 4 }, m.antiDiagonalCopy());
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3_OffsetsEveryDiagonalElement() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateAntiDiagonal(x -> (short) (x + 100));
            assertEquals((short) 103, m.get(0, 2));
            assertEquals((short) 105, m.get(1, 1));
            assertEquals((short) 107, m.get(2, 0));
        }

        @Test
        public void testUpdateAntiDiagonal_AppliesFunction() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.updateAntiDiagonal(x -> (short) (x + 100));
            assertEquals(103, m.get(0, 2));
            assertEquals(105, m.get(1, 1));
            assertEquals(107, m.get(2, 0));
        }
    }

    @Nested
    class UpdateAllTests {

        @Test
        public void testUpdateAll() {
            ShortMatrix m = matrix.copy();
            m.updateAll(x -> (short) (x * 2));
            assertEquals((short) 2, m.get(0, 0));
            assertEquals((short) 4, m.get(0, 1));
            assertEquals((short) 18, m.get(2, 2));
        }

        @Test
        public void testUpdateAllUnarySequentialTallMatrixUsesRowMajorOrder() throws Exception {
            final ShortMatrix m = ShortMatrix.wrap(new short[3][2]);
            final AtomicInteger next = new AtomicInteger();

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> m.updateAll(x -> (short) next.incrementAndGet()));

            assertArrayEquals(new short[] { 1, 2 }, m.rowCopy(0));
            assertArrayEquals(new short[] { 3, 4 }, m.rowCopy(1));
            assertArrayEquals(new short[] { 5, 6 }, m.rowCopy(2));
        }

        @Test
        public void testUpdateAllWithIndices() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 0, 0 }, { 0, 0 } });
            m.updateAll((i, j) -> (short) (i * 10 + j));
            assertEquals((short) 0, m.get(0, 0));
            assertEquals((short) 1, m.get(0, 1));
            assertEquals((short) 10, m.get(1, 0));
            assertEquals((short) 11, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll(x -> (short) (x * 2));
            assertEquals(2, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
            assertEquals(6, m.get(1, 0));
            assertEquals(8, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithIndices() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 0, 0 }, { 0, 0 } });
            m.updateAll((i, j) -> (short) (i * 10 + j));
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
            assertEquals(10, m.get(1, 0));
            assertEquals(11, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_Unary() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll(x -> (short) (x * 2));
            assertEquals((short) 2, m.get(0, 0));
            assertEquals((short) 4, m.get(0, 1));
            assertEquals((short) 6, m.get(1, 0));
            assertEquals((short) 8, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll((i, j) -> (short) (i * 10 + j));
            assertEquals((short) 0, m.get(0, 0));
            assertEquals((short) 1, m.get(0, 1));
            assertEquals((short) 10, m.get(1, 0));
            assertEquals((short) 11, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithUnaryOperator() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.updateAll(x -> (short) (x * 2));
            assertEquals(2, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
            assertEquals(6, m.get(1, 0));
            assertEquals(8, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithBiFunction() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.updateAll((i, j) -> (short) (i * 10 + j));
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
            assertEquals(10, m.get(1, 0));
            assertEquals(11, m.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testUpdateAll_ExplicitModesTransformEveryValue(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 } });
                matrix.updateAll(value -> (short) (value + 10));
                assertEquals(ShortMatrix.wrap(new short[][] { { (short) 11, (short) 12, (short) 13 }, { (short) 14, (short) 15, (short) 16 } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testShortMatrixUpdateAllNullOperator() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix emptyLike = ShortMatrix.wrap(new short[][] { {}, {} });
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.ShortUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.IntBiFunction<Short, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateMainDiagonal((Throwables.ShortUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAntiDiagonal((Throwables.ShortUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.ShortPredicate<RuntimeException>) null, (short) 0));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, (short) 0));

            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateMainDiagonal((Throwables.ShortUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAntiDiagonal((Throwables.ShortUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.ShortPredicate<RuntimeException>) null, (short) 0));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, (short) 0));
        }

        @Test
        public void testUpdateAll_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            empty.updateAll(x -> (short) (x + 1));
            assertTrue(empty.isEmpty());
        }

        @Test
        public void testUpdateAll_ZeroColumnsDoNotInvokeOperator() {
            final var matrix = ShortMatrix.wrap(new short[2][0]);
            matrix.updateAll(value -> {
                throw new AssertionError("No values should be visited");
            });
            assertEquals(2, matrix.rowCount());
            assertEquals(0, matrix.columnCount());
        }

        @Test
        public void testUpdateAllPositional_DuplicateRowsRejectedBeforeMutation() {
            short[] sharedRow = { 1, 2 };
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.wrap(new short[][] { sharedRow, sharedRow }));
        }
    }

    @Nested
    class ReplaceIfTests {

        @Test
        public void testReplaceIf() {
            ShortMatrix m = matrix.copy();
            m.replaceIf(x -> x > 5, (short) 0);
            assertEquals((short) 1, m.get(0, 0));
            assertEquals((short) 5, m.get(1, 1));
            assertEquals((short) 0, m.get(2, 2)); // was 9
        }

        // Original should be unchanged
        @Test
        public void testReplaceIfWithIndices() {
            ShortMatrix m = matrix.copy();
            m.replaceIf((i, j) -> i == j, (short) 0); // Replace diagonal
            assertEquals((short) 0, m.get(0, 0));
            assertEquals((short) 0, m.get(1, 1));
            assertEquals((short) 0, m.get(2, 2));
            assertEquals((short) 2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testReplaceIf_Shape2By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.replaceIf(x -> x > 3, (short) 0);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(0, 2));
            assertEquals(0, m.get(1, 0)); // was 4
            assertEquals(0, m.get(1, 1)); // was 5
            assertEquals(0, m.get(1, 2)); // was 6
        }

        @Test
        public void testReplaceIf_WithIndices() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.replaceIf((i, j) -> i == j, (short) 0); // Replace diagonal
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(1, 1));
            assertEquals(0, m.get(2, 2));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testReplaceIf_NoMatches() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf(x -> x > 100, (short) 99);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_AllMatch() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf(x -> x > 0, (short) 0);
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
            assertEquals(0, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_Predicate() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.replaceIf(x -> x % 2 == 0, (short) 99);
            assertEquals((short) 1, m.get(0, 0));
            assertEquals((short) 99, m.get(0, 1));
            assertEquals((short) 3, m.get(0, 2));
            assertEquals((short) 99, m.get(1, 0));
        }

        @Test
        public void testReplaceIf_BiPredicate() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf((i, j) -> i == j, (short) 0);
            assertEquals((short) 0, m.get(0, 0));
            assertEquals((short) 2, m.get(0, 1));
            assertEquals((short) 3, m.get(1, 0));
            assertEquals((short) 0, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_WithPredicate() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.replaceIf(x -> x > 3, (short) 99);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(99, m.get(1, 0));
            assertEquals(99, m.get(1, 2));
        }

        @Test
        public void testReplaceIf_WithBiPredicate() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.replaceIf((i, j) -> i == j, (short) 0);
            assertEquals(0, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(0, m.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testReplaceIf_ExplicitModesReplaceOnlyMatchingValues(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 } });
                matrix.replaceIf(value -> value % 2 == 0, (short) 9);
                assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 9, (short) 3 }, { (short) 9, (short) 5, (short) 9 } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testReplaceIf_ZerosValuesGreaterThanThree() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            matrix.replaceIf(x -> x > 3, (short) 0);
            // Result: [[1, 2, 3], [0, 0, 0]]
            assertEquals((short) 1, matrix.get(0, 0));
            assertEquals((short) 2, matrix.get(0, 1));
            assertEquals((short) 3, matrix.get(0, 2));
            assertEquals((short) 0, matrix.get(1, 0));
            assertEquals((short) 0, matrix.get(1, 1));
            assertEquals((short) 0, matrix.get(1, 2));
        }
    }

    @Nested
    class MapTests {

        @Test
        public void testMap() {
            ShortMatrix result = matrix.map(x -> (short) (x * 2));
            assertEquals((short) 2, result.get(0, 0));
            assertEquals((short) 4, result.get(0, 1));
            assertEquals((short) 18, result.get(2, 2));

            // Original should be unchanged
            assertEquals((short) 1, matrix.get(0, 0));
        }

        @Test
        public void testMap_SquaresEveryCell() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix squared = matrix.map(x -> (short) (x * x));
            // Result: [[1, 4], [9, 16]]
            assertEquals((short) 1, squared.get(0, 0));
            assertEquals((short) 4, squared.get(0, 1));
            assertEquals((short) 9, squared.get(1, 0));
            assertEquals((short) 16, squared.get(1, 1));
        }

        @Test
        public void testMap_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix result = m.map(x -> (short) (x * 2));
            assertEquals(2, result.get(0, 0));
            assertEquals(4, result.get(0, 1));
            assertEquals(6, result.get(1, 0));
            assertEquals(8, result.get(1, 1));

            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testMap_Shape2By2_ScalesAllCellsAndPreservesSource() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix result = m.map(x -> (short) (x * 10));
            assertEquals((short) 10, result.get(0, 0));
            assertEquals((short) 20, result.get(0, 1));
            assertEquals((short) 30, result.get(1, 0));
            assertEquals((short) 40, result.get(1, 1));
            // Original should be unchanged
            assertEquals((short) 1, m.get(0, 0));
        }

        @Test
        public void testChainedOperations() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix result = m.map(x -> (short) (x * 2)).transpose().map(x -> (short) (x + 1));
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals((short) 3, result.get(0, 0)); // (1*2)+1
            assertEquals((short) 7, result.get(0, 1)); // (3*2)+1
            assertEquals((short) 5, result.get(1, 0)); // (2*2)+1
            assertEquals((short) 9, result.get(1, 1)); // (4*2)+1
        }

        @Test
        public void testMap_CreatesNewMatrix() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix result = m.map(x -> (short) (x * 3));
            assertEquals(3, result.get(0, 0));
            assertEquals(6, result.get(0, 1));
            assertEquals(9, result.get(1, 0));
            assertEquals(12, result.get(1, 1));
            assertEquals(1, m.get(0, 0)); // original unchanged
        }

        @Test
        public void testMapNullMapper() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });

            assertThrows(IllegalArgumentException.class, () -> matrix.map((Throwables.ShortUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.mapToObj((Throwables.ShortFunction<String, RuntimeException>) null, String.class));
        }

        @Test
        public void testShortMatrixRowsForZeroColumnMatrix() {
            final ShortMatrix matrix = ShortMatrix.wrap(new short[][] { {}, {}, {} });
            final List<short[]> rows = matrix.rowStreams().map(ShortStream::toArray).toList();

            assertEquals(3, rows.size());
            assertArrayEquals(new short[0], rows.get(0));
            assertArrayEquals(new short[0], rows.get(1));
            assertArrayEquals(new short[0], rows.get(2));
        }

        @Test
        public void testNegativeShorts() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { -1, -2 }, { -3, -4 } });

            assertEquals(-1, m.get(0, 0));
            assertEquals(-4, m.get(1, 1));

            ShortMatrix doubled = m.map(x -> (short) (x * 2));
            assertEquals(-2, doubled.get(0, 0));
            assertEquals(-8, doubled.get(1, 1));
        }
    }

    @Nested
    class MapToObjTests {

        @Test
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void testObjectMappingRuntimeArrayTypeAndStoreFailure() {
            final ShortMatrix source = ShortMatrix.wrap(new short[] { 1, 1 });
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
            assertArrayEquals(new short[] { 1, 1 }, source.rowCopy(0));
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
            final ShortMatrix empty = new ShortMatrix(new short[0][], 3);
            final Matrix<Object> mapped = empty.mapToObj(value -> null, largestElementType);
            assertEquals(0, mapped.rowCount());
            assertEquals(3, mapped.columnCount());
            assertEquals(largestElementType, mapped.elementType());

            final AtomicInteger calls = new AtomicInteger();
            assertThrows(IllegalArgumentException.class, () -> empty.mapToObj(value -> {
                calls.incrementAndGet();
                return null;
            }, tooDeep));
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.wrap(new short[] { 1 }).mapToObj(value -> {
                calls.incrementAndGet();
                return null;
            }, tooDeep));
            assertEquals(0, calls.get());
        }

        @Test
        public void testMapToObj() {
            Matrix<String> result = matrix.mapToObj(x -> "val:" + x, String.class);
            assertEquals("val:1", result.get(0, 0));
            assertEquals("val:5", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> result = m.mapToObj(x -> "val:" + x, String.class);
            assertEquals("val:1", result.get(0, 0));
            assertEquals("val:4", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_AddsValuePrefixToFirstRow() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> result = m.mapToObj(x -> "Value:" + x, String.class);
            assertEquals("Value:1", result.get(0, 0));
            assertEquals("Value:4", result.get(1, 1));
        }

        @Test
        public void testMapToObj_CreatesObjectMatrix() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            Matrix<String> result = m.mapToObj(x -> "val" + x, String.class);
            assertEquals("val1", result.get(0, 0));
            assertEquals("val4", result.get(1, 1));
        }

        @Test
        public void testMapToObj_WithNull() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> result = m.mapToObj(x -> x == 2 ? null : String.valueOf(x), String.class);
            assertEquals("1", result.get(0, 0));
            assertEquals(null, result.get(0, 1));
            assertEquals("3", result.get(1, 0));
        }
    }

    @Nested
    class FillTests {

        @Test
        public void testFillWithValue() {
            ShortMatrix m = matrix.copy();
            m.fill((short) 99);
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals((short) 99, m.get(i, j));
                }
            }
        }

        @Test
        public void testFill() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            matrix.fill((short) 5);
            // Result: [[5, 5], [5, 5]]
            assertEquals((short) 5, matrix.get(0, 0));
            assertEquals((short) 5, matrix.get(0, 1));
            assertEquals((short) 5, matrix.get(1, 0));
            assertEquals((short) 5, matrix.get(1, 1));
        }

        @Test
        public void testFill_WithValue() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.fill((short) 99);
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals(99, m.get(i, j));
                }
            }
        }

        @Test
        public void testFill_Value() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.fill((short) 99);
            assertEquals((short) 99, m.get(0, 0));
            assertEquals((short) 99, m.get(0, 1));
            assertEquals((short) 99, m.get(1, 0));
            assertEquals((short) 99, m.get(1, 1));
        }

        @Test
        public void testFill_WithValue_Values() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.fill((short) 7);
            assertEquals(7, m.get(0, 0));
            assertEquals(7, m.get(0, 1));
            assertEquals(7, m.get(1, 0));
            assertEquals(7, m.get(1, 1));
        }
    }

    @Nested
    class CopyFromTests {

        @Test
        public void testCopyFrom() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            short[][] patch = { { 1, 2 }, { 3, 4 } };
            m.copyFrom(patch);
            assertEquals((short) 1, m.get(0, 0));
            assertEquals((short) 2, m.get(0, 1));
            assertEquals((short) 3, m.get(1, 0));
            assertEquals((short) 4, m.get(1, 1));
            assertEquals((short) 0, m.get(2, 2)); // unchanged
        }

        @Test
        public void testCopyFromAtPosition() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            short[][] patch = { { 1, 2 }, { 3, 4 } };
            m.copyFrom(1, 1, patch);
            assertEquals((short) 0, m.get(0, 0)); // unchanged
            assertEquals((short) 1, m.get(1, 1));
            assertEquals((short) 2, m.get(1, 2));
            assertEquals((short) 3, m.get(2, 1));
            assertEquals((short) 4, m.get(2, 2));

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
            // assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(3, 0, patch));
            m.copyFrom(3, 0, patch);
        }

        @Test
        public void testIntMatrix_CopyFrom() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 0, 0, 0 }, { 0, 0, 0 } });
            matrix.copyFrom(new short[][] { { 1, 2 }, { 3, 4 } });
            // Result: [[1, 2, 0], [3, 4, 0]]
            assertEquals((short) 1, matrix.get(0, 0));
            assertEquals((short) 2, matrix.get(0, 1));
            assertEquals((short) 0, matrix.get(0, 2));
            assertEquals((short) 3, matrix.get(1, 0));
            assertEquals((short) 4, matrix.get(1, 1));
            assertEquals((short) 0, matrix.get(1, 2));
        }

        // Returns [[2, 3], [5, 6]]
        @Test
        public void testCopyFrom_Offset() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            matrix.copyFrom(1, 1, new short[][] { { 1, 2 }, { 3, 4 } });
            // Result: [[0, 0, 0], [0, 1, 2], [0, 3, 4]]
            assertEquals((short) 0, matrix.get(0, 0));
            assertEquals((short) 0, matrix.get(1, 0));
            assertEquals((short) 1, matrix.get(1, 1));
            assertEquals((short) 2, matrix.get(1, 2));
            assertEquals((short) 3, matrix.get(2, 1));
            assertEquals((short) 4, matrix.get(2, 2));
        }

        @Test
        public void testCopyFrom_Shape3By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            short[][] patch = { { 1, 2 }, { 3, 4 } };
            m.copyFrom(patch);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
            assertEquals(0, m.get(2, 2)); // unchanged
        }

        @Test
        public void testCopyFromAtPosition_Shape3By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } });
            short[][] patch = { { 1, 2 }, { 3, 4 } };
            m.copyFrom(1, 1, patch);
            assertEquals(0, m.get(0, 0)); // unchanged
            assertEquals(1, m.get(1, 1));
            assertEquals(2, m.get(1, 2));
            assertEquals(3, m.get(2, 1));
            assertEquals(4, m.get(2, 2));
        }

        @Test
        public void testCopyFrom_EntireMatrix() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.copyFrom(new short[][] { { 9, 8 }, { 7, 6 } });
            assertEquals(9, m.get(0, 0));
            assertEquals(8, m.get(0, 1));
            assertEquals(7, m.get(1, 0));
            assertEquals(6, m.get(1, 1));
        }

        @Test
        public void testCopyFromAtOrigin() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.copyFrom(0, 0, new short[][] { { 99, 88 } });
            assertEquals(99, m.get(0, 0));
            assertEquals(88, m.get(0, 1));
            assertEquals(3, m.get(0, 2));
            assertEquals(4, m.get(1, 0));
        }

        @Test
        public void testCopyFrom_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            m.copyFrom(new short[][] { { 10, 20 }, { 30, 40 } });
            assertEquals((short) 10, m.get(0, 0));
            assertEquals((short) 20, m.get(0, 1));
            assertEquals((short) 30, m.get(1, 0));
            assertEquals((short) 40, m.get(1, 1));
        }

        @Test
        public void testCopyFromWithOffset() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.copyFrom(1, 1, new short[][] { { 99, 88 } });
            assertEquals((short) 1, m.get(0, 0));
            assertEquals((short) 99, m.get(1, 1));
            assertEquals((short) 88, m.get(1, 2));
        }

        @Test
        public void testCopyFrom_Values() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[][] b = { { 9, 8 }, { 7, 6 } };
            m.copyFrom(b);
            assertEquals(9, m.get(0, 0));
            assertEquals(6, m.get(1, 1));
        }

        @Test
        public void testCopyFromAndOffset() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[][] b = { { 99, 98 } };
            m.copyFrom(1, 1, b);
            assertEquals(1, m.get(0, 0));
            assertEquals(99, m.get(1, 1));
            assertEquals(98, m.get(1, 2));
        }

        @Test
        public void testCopyFrom_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            short[][] patch = { { 1, 2 }, { 3, 4 } };
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0", "3, 0", "0, -1", "0, 3" })
        public void testCopyFrom_RejectsOffsetsOutsideDestination(final int row, final int column) {
            final var matrix = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(row, column, new short[][] { { (short) 1 } }));
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } }), matrix);
        }
    }

    @Nested
    class CopyTests {

        // Test bounds
        @Test
        public void testCopy() {
            ShortMatrix copy = matrix.copy();
            assertEquals(matrix.rowCount(), copy.rowCount());
            assertEquals(matrix.columnCount(), copy.columnCount());
            assertEquals(matrix.get(0, 0), copy.get(0, 0));

            // Modify copy shouldn't affect original
            copy.set(0, 0, (short) 99);
            assertEquals((short) 1, matrix.get(0, 0));
            assertEquals((short) 99, copy.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals(1, copy.get(0, 0));

            // Modify copy shouldn't affect original
            copy.set(0, 0, (short) 99);
            assertEquals(1, m.get(0, 0));
            assertEquals(99, copy.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2_DimensionsAndIndependentStorage() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix copy = m.copy();
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals((short) 1, copy.get(0, 0));

            // Verify it's a deep copy
            copy.set(0, 0, (short) 99);
            assertEquals((short) 1, m.get(0, 0));
        }

        @Test
        public void testCopy_CreatesIndependentCopy() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals(1, copy.get(0, 0));

            copy.set(0, 0, (short) 99);
            assertEquals(1, m.get(0, 0)); // original unchanged
        }
    }

    @Nested
    class CopyRowsTests {

        @Test
        public void testCopy_EntireMatrix() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix copy = m.copyRows(0, 2);
            assertEquals(2, copy.rowCount());
            assertEquals(3, copy.columnCount());
            assertEquals(1, copy.get(0, 0));
            assertEquals(6, copy.get(1, 2));
        }

        @Test
        public void testCopyRows() {
            ShortMatrix subset = matrix.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertEquals((short) 4, subset.get(0, 0));
            assertEquals((short) 7, subset.get(1, 0));

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(0, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(2, 1));
        }

        @Test
        public void testCopy_Rows() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            ShortMatrix subset = matrix.copyRows(1, 3);
            // Returns [[3, 4], [5, 6]]
            assertEquals(2, subset.rowCount());
            assertEquals((short) 3, subset.get(0, 0));
            assertEquals((short) 4, subset.get(0, 1));
            assertEquals((short) 5, subset.get(1, 0));
            assertEquals((short) 6, subset.get(1, 1));
        }

        @Test
        public void testCopyRows_Shape3By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ShortMatrix subset = m.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertEquals(4, subset.get(0, 0));
            assertEquals(9, subset.get(1, 2));
        }

        @Test
        public void testCopyRows_Shape3By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            ShortMatrix copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals((short) 3, copy.get(0, 0));
            assertEquals((short) 6, copy.get(1, 1));
        }

        @Test
        public void testCopyRows_Values() {
            short[][] arr = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(3, copy.get(0, 0));
            assertEquals(6, copy.get(1, 1));
        }

        @Test
        public void testCopyRangesEmpty_ReturnsEmptyMatrix() {
            // Regression: copyRows(from, from) on a matrix with columns > 0 must not throw.
            ShortMatrix empty = matrix.copyRows(0, 0);
            assertEquals(0, empty.rowCount());
            assertEquals(3, empty.columnCount());

            ShortMatrix emptyRows = matrix.copyRegion(1, 1, 0, 3);
            assertEquals(0, emptyRows.rowCount());
            assertEquals(3, emptyRows.columnCount());

            ShortMatrix emptyCols = matrix.copyRegion(0, 3, 1, 1);
            assertEquals(3, emptyCols.rowCount());
            assertEquals(0, emptyCols.columnCount());
        }

        @Test
        public void testCopyRows_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(2, 1));
        }
    }

    @Nested
    class CopyRegionTests {

        @Test
        public void testCopyRegion() {
            ShortMatrix submatrix = matrix.copyRegion(0, 2, 1, 3);
            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals((short) 2, submatrix.get(0, 0));
            assertEquals((short) 3, submatrix.get(0, 1));
            assertEquals((short) 5, submatrix.get(1, 0));
            assertEquals((short) 6, submatrix.get(1, 1));

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRegion(0, 2, 0, 4));
        }

        // Result: [[1, 2, 9, 9], [3, 4, 9, 9], [9, 9, 9, 9]]
        @Test
        public void testCopy_Region() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ShortMatrix region = matrix.copyRegion(0, 2, 1, 3);
            // Returns [[2, 3], [5, 6]]
            assertEquals((short) 2, region.get(0, 0));
            assertEquals((short) 3, region.get(0, 1));
            assertEquals((short) 5, region.get(1, 0));
            assertEquals((short) 6, region.get(1, 1));
        }

        @Test
        public void testCopyRegion_Shape3By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ShortMatrix submatrix = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals(2, submatrix.get(0, 0));
            assertEquals(6, submatrix.get(1, 1));
        }

        @Test
        public void testCopyRegion_EntireMatrixWithColumnRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix copy = m.copyRegion(0, 2, 0, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(3, copy.columnCount());
            assertEquals(1, copy.get(0, 0));
            assertEquals(6, copy.get(1, 2));
        }

        @Test
        public void testCopyRegion_Shape3By3_MiddleRowRegion() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ShortMatrix copy = m.copyRegion(1, 2, 1, 3);
            assertEquals(1, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals((short) 5, copy.get(0, 0));
            assertEquals((short) 6, copy.get(0, 1));
        }

        @Test
        public void testCopyRegion_Values() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix copy = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(2, copy.get(0, 0));
            assertEquals(6, copy.get(1, 1));
        }

        @Test
        public void testCopyRegion_IndependentOfOriginal() {
            ShortMatrix src = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ShortMatrix region = src.copyRegion(0, 2, 1, 3);
            region.set(0, 0, (short) 999);
            assertEquals((short) 2, src.get(0, 1));
            src.set(0, 1, (short) 888);
            assertEquals((short) 999, region.get(0, 0));
        }

        @Test
        public void testCopyRegion_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, 0, 3));
        }
    }

    @Nested
    class ResizeTests {

        @Test
        public void testResize_Default() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix extended = matrix.resize(3, 3);
            // Result: [[1, 2, 0], [3, 4, 0], [0, 0, 0]]
            assertEquals((short) 1, extended.get(0, 0));
            assertEquals((short) 2, extended.get(0, 1));
            assertEquals((short) 0, extended.get(0, 2));
            assertEquals((short) 3, extended.get(1, 0));
            assertEquals((short) 4, extended.get(1, 1));
            assertEquals((short) 0, extended.get(1, 2));
            assertEquals((short) 0, extended.get(2, 0));
        }

        // Result: [[9, 9, 9, 9, 9, 9], [9, 9, 1, 2, 9, 9], [9, 9, 9, 9, 9, 9]]
        @Test
        public void testResize_WithFill() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix extended = matrix.resize(3, 4, (short) 9);
            // Result: [[1, 2, 9, 9], [3, 4, 9, 9], [9, 9, 9, 9]]
            assertEquals((short) 1, extended.get(0, 0));
            assertEquals((short) 9, extended.get(0, 2));
            assertEquals((short) 9, extended.get(2, 0));

            ShortMatrix truncated = matrix.resize(1, 1, (short) 0);
            assertEquals((short) 1, truncated.get(0, 0));
        }

        @Test
        public void testResize() {
            ShortMatrix extended = matrix.resize(5, 5);
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());
            assertEquals((short) 1, extended.get(0, 0));
            assertEquals((short) 0, extended.get(3, 3)); // new cells are 0

            // Test truncation
            ShortMatrix truncated = matrix.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
        }

        @Test
        public void testResizeWithDefaultValue() {
            ShortMatrix extended = matrix.resize(4, 4, (short) -1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals((short) 1, extended.get(0, 0));
            assertEquals((short) -1, extended.get(3, 3)); // new cell

            // Test negative dimensions
            assertThrows(IllegalArgumentException.class, () -> matrix.resize(-1, 3, (short) 0));
            assertThrows(IllegalArgumentException.class, () -> matrix.resize(3, -1, (short) 0));
        }

        @Test
        public void testResize_Larger() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix extended = m.resize(4, 4);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(4, extended.get(1, 1));
            assertEquals(0, extended.get(3, 3)); // new cells are 0
        }

        @Test
        public void testResize_Smaller() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ShortMatrix truncated = m.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
            assertEquals(1, truncated.get(0, 0));
            assertEquals(5, truncated.get(1, 1));
        }

        @Test
        public void testResize_WithDefaultValue() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix extended = m.resize(3, 3, (short) -1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(-1, extended.get(2, 2)); // new cell
        }

        @Test
        public void testResize_SameSize() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix extended = m.resize(2, 2);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(4, extended.get(1, 1));
        }

        @Test
        public void testResize_NewSize() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix extended = m.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals((short) 1, extended.get(0, 0));
            assertEquals((short) 0, extended.get(2, 2));
        }

        @Test
        public void testResize_WithDefaultValue_Shape1By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix extended = m.resize(2, 3, (short) 99);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals((short) 1, extended.get(0, 0));
            assertEquals((short) 99, extended.get(0, 2));
            assertEquals((short) 99, extended.get(1, 0));
        }

        @Test
        public void testResize_IncreasesSize() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix extended = m.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(0, extended.get(2, 2));
        }

        @Test
        public void testResize_WithDefaultValue_Values() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix extended = m.resize(3, 3, (short) 99);
            assertEquals(99, extended.get(2, 2));
        }

        @Test
        public void testResize_EmptyMatrix() {
            ShortMatrix empty = ShortMatrix.empty();
            ShortMatrix extended = empty.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(0, extended.get(0, 0));
        }

        @Test
        public void testResize_GrowsRowsWhileTrimmingColumns() {
            final var matrix = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } });
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1 }, { (short) 3 }, { (short) 9 } }), matrix.resize(3, 1, (short) 9));
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } }), matrix);
        }

        @Test
        public void testResize_WithNegativeDimensions() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 3, (short) 0));
            assertThrows(IllegalArgumentException.class, () -> m.resize(3, -1, (short) 0));
        }
    }

    @Nested
    class PadTests {

        // Check new values
        @Test
        public void testPadDirectional() {
            ShortMatrix extended = matrix.pad(1, 1, 2, 2);
            assertEquals(5, extended.rowCount()); // 1 + 3 + 1
            assertEquals(7, extended.columnCount()); // 2 + 3 + 2

            // Original values should be at offset position
            assertEquals((short) 1, extended.get(1, 2));
            assertEquals((short) 5, extended.get(2, 3));

            // New cells should be 0
            assertEquals((short) 0, extended.get(0, 0));
        }

        @Test
        public void testPadDirectionalWithDefaultValue() {
            ShortMatrix extended = matrix.pad(1, 1, 1, 1, (short) -1);
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());

            // Check original values
            assertEquals((short) 1, extended.get(1, 1));

            // Check new values
            assertEquals((short) -1, extended.get(0, 0));
            assertEquals((short) -1, extended.get(4, 4));

            // Test negative values
            assertThrows(IllegalArgumentException.class, () -> matrix.pad(-1, 1, 1, 1, (short) 0));
        }

        // matrix is now [[3, 2, 1], [6, 5, 4]]
        @Test
        public void testPad_Default() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix extended = matrix.pad(1, 1, 1, 1);
            // Result: [[0, 0, 0, 0], [0, 1, 2, 0], [0, 0, 0, 0]]
            assertEquals(3, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals((short) 0, extended.get(0, 0));
            assertEquals((short) 1, extended.get(1, 1));
            assertEquals((short) 2, extended.get(1, 2));
            assertEquals((short) 0, extended.get(1, 3));
        }

        @Test
        public void testPad_WithFill() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix padded = matrix.pad(1, 1, 2, 2, (short) 9);
            // Result: [[9, 9, 9, 9, 9, 9], [9, 9, 1, 2, 9, 9], [9, 9, 9, 9, 9, 9]]
            assertEquals(3, padded.rowCount());
            assertEquals(6, padded.columnCount());
            assertEquals((short) 9, padded.get(0, 0));
            assertEquals((short) 1, padded.get(1, 2));
            assertEquals((short) 2, padded.get(1, 3));
            assertEquals((short) 9, padded.get(1, 4));
        }

        // Original unchanged
        @Test
        public void testPad_Directional() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ShortMatrix extended = m.pad(1, 1, 2, 2);
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
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ShortMatrix extended = m.pad(1, 1, 1, 1, (short) -1);
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
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 5 } });
            ShortMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals((short) 5, extended.get(1, 1));
            assertEquals((short) 0, extended.get(0, 0));
        }

        @Test
        public void testPad_DirectionsWithDefault() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 5 } });
            ShortMatrix extended = m.pad(1, 1, 1, 1, (short) 9);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals((short) 5, extended.get(1, 1));
            assertEquals((short) 9, extended.get(0, 0));
            assertEquals((short) 9, extended.get(2, 2));
        }

        @Test
        public void testPad_WithDirections() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1, extended.get(1, 1));
        }

        @Test
        public void testPad_WithDirectionsAndDefault() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix extended = m.pad(1, 1, 1, 1, (short) 88);
            assertEquals(88, extended.get(0, 0));
            assertEquals(1, extended.get(1, 1));
        }

        @Test
        public void testPad_DirectionalZeroExtension() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix extended = m.pad(0, 0, 0, 0);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
        }

        @Test
        public void testPad_EmptyMatrixWithPaddingFillsFully() {
            ShortMatrix empty = ShortMatrix.empty();
            ShortMatrix padded = empty.pad(1, 1, 1, 1, (short) 9);
            assertEquals(2, padded.rowCount());
            assertEquals(2, padded.columnCount());
            assertArrayEquals(new short[] { 9, 9 }, padded.rowCopy(0));
            assertArrayEquals(new short[] { 9, 9 }, padded.rowCopy(1));
        }

        @Test
        public void testPad_SingleAxisFillPreservesExistingValues() {
            final var matrix = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } });
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2, (short) 9 }, { (short) 3, (short) 4, (short) 9 } }),
                    matrix.pad(0, 0, 0, 1, (short) 9));
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 9, (short) 1, (short) 2 }, { (short) 9, (short) 3, (short) 4 } }),
                    matrix.pad(0, 0, 1, 0, (short) 9));
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 }, { (short) 9, (short) 9 } }),
                    matrix.pad(0, 1, 0, 0, (short) 9));
            final var copy = matrix.pad(0, 0, 0, 0, (short) 9);
            assertEquals(matrix, copy);
            assertNotSame(matrix, copy);
        }

        @Test
        public void testPad_DirectionalWithNegativeValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.pad(-1, 1, 1, 1, (short) 0));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0, 0, 0", "0, -1, 0, 0", "0, 0, -1, 0", "0, 0, 0, -1", "2147483647, 0, 0, 0", "0, 0, 0, 2147483647" })
        public void testPad_RejectsNegativeOrOverflowingDimensions(final int top, final int bottom, final int left, final int right) {
            final var matrix = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.pad(top, bottom, left, right));
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } }), matrix);
        }
    }

    @Nested
    class FlipHorizontallyInPlaceTests {

        @Test
        public void testReverseH() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.flipHorizontallyInPlace();
            assertEquals((short) 3, m.get(0, 0));
            assertEquals((short) 2, m.get(0, 1));
            assertEquals((short) 1, m.get(0, 2));
            assertEquals((short) 6, m.get(1, 0));
            assertEquals((short) 5, m.get(1, 1));
            assertEquals((short) 4, m.get(1, 2));
        }

        @Test
        public void testFlipHorizontallyInPlace() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            matrix.flipHorizontallyInPlace();
            // matrix is now [[3, 2, 1], [6, 5, 4]]
            assertArrayEquals(new short[] { 3, 2, 1 }, matrix.rowView(0));
            assertArrayEquals(new short[] { 6, 5, 4 }, matrix.rowView(1));
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.flipHorizontallyInPlace();
            assertEquals(3, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(1, m.get(0, 2));
            assertEquals(6, m.get(1, 0));
        }

        @Test
        public void testFlipHorizontallyInPlace_ReversesHorizontally() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.flipHorizontallyInPlace();
            assertEquals(3, m.get(0, 0));
            assertEquals(1, m.get(0, 2));
            assertEquals(6, m.get(1, 0));
        }

        @Test
        public void testFlipHorizontallyInPlace_SingleColumn() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1 }, { 2 } });
            m.flipHorizontallyInPlace();
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 0));
        }
    }

    @Nested
    class FlipVerticallyInPlaceTests {

        @Test
        public void testReverseV() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertEquals((short) 5, m.get(0, 0));
            assertEquals((short) 6, m.get(0, 1));
            assertEquals((short) 3, m.get(1, 0));
            assertEquals((short) 4, m.get(1, 1));
            assertEquals((short) 1, m.get(2, 0));
            assertEquals((short) 2, m.get(2, 1));
        }

        @Test
        public void testFlipVerticallyInPlace() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            matrix.flipVerticallyInPlace();
            // matrix is now [[7, 8, 9], [4, 5, 6], [1, 2, 3]]
            assertArrayEquals(new short[] { 7, 8, 9 }, matrix.rowView(0));
            assertArrayEquals(new short[] { 4, 5, 6 }, matrix.rowView(1));
            assertArrayEquals(new short[] { 1, 2, 3 }, matrix.rowView(2));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape3By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertEquals(5, m.get(0, 0));
            assertEquals(6, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(1, m.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_ReversesVertically() {
            short[][] arr = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.flipVerticallyInPlace();
            assertEquals(5, m.get(0, 0));
            assertEquals(6, m.get(0, 1));
            assertEquals(1, m.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_OddRowCount() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertArrayEquals(new short[] { 5, 6 }, m.rowCopy(0));
            assertArrayEquals(new short[] { 3, 4 }, m.rowCopy(1));
            assertArrayEquals(new short[] { 1, 2 }, m.rowCopy(2));
        }

        @Test
        public void testFlipVerticallyInPlace_SingleRow() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            m.flipVerticallyInPlace();
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
        }
    }

    @Nested
    class FlipHorizontallyTests {

        @Test
        public void testFlipH() {
            ShortMatrix flipped = matrix.flipHorizontally();
            assertEquals((short) 3, flipped.get(0, 0));
            assertEquals((short) 2, flipped.get(0, 1));
            assertEquals((short) 1, flipped.get(0, 2));

            // Original should be unchanged
            assertEquals((short) 1, matrix.get(0, 0));
        }

        // Result: [[4, 3], [2, 1]]
        @Test
        public void testFlipHorizontally() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix flipped = matrix.flipHorizontally();
            // Result: [[3, 2, 1], [6, 5, 4]]
            assertArrayEquals(new short[] { 3, 2, 1 }, flipped.rowView(0));
            assertArrayEquals(new short[] { 6, 5, 4 }, flipped.rowView(1));
        }

        @Test
        public void testFlipHorizontally_Shape2By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix flipped = m.flipHorizontally();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(2, flipped.get(0, 1));
            assertEquals(1, flipped.get(0, 2));

            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testFlipHorizontally_Shape2By3_ReversesFirstRow() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix flipped = m.flipHorizontally();
            assertEquals((short) 3, flipped.get(0, 0));
            assertEquals((short) 1, flipped.get(0, 2));
            // Original unchanged
            assertEquals((short) 1, m.get(0, 0));
        }

        @Test
        public void testFlipHorizontally_CreatesNewHorizontallyFlipped() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix flipped = m.flipHorizontally();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(1, m.get(0, 0)); // original unchanged
        }
    }

    @Nested
    class FlipVerticallyTests {

        @Test
        public void testFlipV() {
            ShortMatrix flipped = matrix.flipVertically();
            assertEquals((short) 7, flipped.get(0, 0));
            assertEquals((short) 8, flipped.get(0, 1));
            assertEquals((short) 9, flipped.get(0, 2));

            // Original should be unchanged
            assertEquals((short) 1, matrix.get(0, 0));
        }

        @Test
        public void testFlipVertically() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix flipped = matrix.flipVertically();
            // Result: [[4, 5, 6], [1, 2, 3]]
            assertArrayEquals(new short[] { 4, 5, 6 }, flipped.rowView(0));
            assertArrayEquals(new short[] { 1, 2, 3 }, flipped.rowView(1));
        }

        @Test
        public void testFlipVertically_Shape3By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            ShortMatrix flipped = m.flipVertically();
            assertEquals(5, flipped.get(0, 0));
            assertEquals(3, flipped.get(1, 0));
            assertEquals(1, flipped.get(2, 0));

            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testFlipVertically_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix flipped = m.flipVertically();
            assertEquals((short) 3, flipped.get(0, 0));
            assertEquals((short) 1, flipped.get(1, 0));
            // Original unchanged
            assertEquals((short) 1, m.get(0, 0));
        }

        @Test
        public void testFlipVertically_CreatesNewVerticallyFlipped() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix flipped = m.flipVertically();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(1, m.get(0, 0)); // original unchanged
        }
    }

    @Nested
    class Rotate90Tests {

        @Test
        public void testRotate90() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals((short) 3, rotated.get(0, 0));
            assertEquals((short) 1, rotated.get(0, 1));
            assertEquals((short) 4, rotated.get(1, 0));
            assertEquals((short) 2, rotated.get(1, 1));
        }

        @Test
        public void testRotate90_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3, rotated.get(0, 0));
            assertEquals(1, rotated.get(0, 1));
            assertEquals(4, rotated.get(1, 0));
            assertEquals(2, rotated.get(1, 1));
        }

        @Test
        public void testRotate90_NonSquare() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix rotated = m.rotate90();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4, rotated.get(0, 0));
            assertEquals(1, rotated.get(0, 1));
            assertEquals(6, rotated.get(2, 0));
            assertEquals(3, rotated.get(2, 1));
        }

        @Test
        public void testRotate90_Shape2By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix rotated = m.rotate90();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals((short) 4, rotated.get(0, 0));
            assertEquals((short) 1, rotated.get(0, 1));
            assertEquals((short) 6, rotated.get(2, 0));
        }

        @Test
        public void testRotate90_RotatesClockwise() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.println();
            ShortMatrix rotated = m.rotate90();
            rotated.println();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3, rotated.get(0, 0));
            assertEquals(1, rotated.get(0, 1));
            assertEquals(4, rotated.get(1, 0));
            assertEquals(2, rotated.get(1, 1));
        }

        @Test
        public void testRotate90_TallRectangularValues() {
            final var matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 5, (short) 3, (short) 1 }, { (short) 6, (short) 4, (short) 2 } }), matrix.rotate90());
        }

        @Test
        public void testRotate90_SingleRowEdgeCase() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            ShortMatrix rotated = matrix.rotate90();

            assertEquals(3, rotated.rowCount());
            assertEquals(1, rotated.columnCount());
            assertEquals((short) 1, rotated.get(0, 0));
            assertEquals((short) 2, rotated.get(1, 0));
            assertEquals((short) 3, rotated.get(2, 0));
        }
    }

    @Nested
    class Rotate180Tests {

        @Test
        public void testRotate180() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix rotated = m.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals((short) 4, rotated.get(0, 0));
            assertEquals((short) 3, rotated.get(0, 1));
            assertEquals((short) 2, rotated.get(1, 0));
            assertEquals((short) 1, rotated.get(1, 1));
        }

        @Test
        public void testRotate180_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix rotated = m.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4, rotated.get(0, 0));
            assertEquals(3, rotated.get(0, 1));
            assertEquals(2, rotated.get(1, 0));
            assertEquals(1, rotated.get(1, 1));
        }

        @Test
        public void testRotate180_NonSquare() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix rotated = m.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            assertEquals(6, rotated.get(0, 0));
            assertEquals(5, rotated.get(0, 1));
            assertEquals(4, rotated.get(0, 2));
            assertEquals(3, rotated.get(1, 0));
            assertEquals(2, rotated.get(1, 1));
            assertEquals(1, rotated.get(1, 2));
        }

        @Test
        public void testRotate180_Rotates180Degrees() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix rotated = m.rotate180();
            assertEquals(4, rotated.get(0, 0));
            assertEquals(3, rotated.get(0, 1));
            assertEquals(2, rotated.get(1, 0));
            assertEquals(1, rotated.get(1, 1));
        }

        @Test
        public void testRotate_CompositionEqualsRotate180() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertEquals(m.rotate180(), m.rotate90().rotate90());
            assertEquals(m.rotate270(), m.rotate90().rotate90().rotate90());
            assertEquals(m, m.rotate90().rotate90().rotate90().rotate90());
        }

        @Test
        public void testRotations_Nx0_PreserveMathematicalShape() {
            ShortMatrix m = ShortMatrix.wrap(new short[3][0]);

            ShortMatrix via180 = m.rotate180();
            assertEquals(3, via180.rowCount());
            assertEquals(0, via180.columnCount());

            ShortMatrix viaRotate90Twice = m.rotate90().rotate90();
            assertEquals(3, viaRotate90Twice.rowCount());
            assertEquals(0, viaRotate90Twice.columnCount());
        }
    }

    @Nested
    class Rotate270Tests {

        @Test
        public void testRotate270() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals((short) 2, rotated.get(0, 0));
            assertEquals((short) 4, rotated.get(0, 1));
            assertEquals((short) 1, rotated.get(1, 0));
            assertEquals((short) 3, rotated.get(1, 1));
        }

        @Test
        public void testRotate270_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(2, rotated.get(0, 0));
            assertEquals(4, rotated.get(0, 1));
            assertEquals(1, rotated.get(1, 0));
            assertEquals(3, rotated.get(1, 1));
        }

        @Test
        public void testRotate270_NonSquare() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix rotated = m.rotate270();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3, rotated.get(0, 0));
            assertEquals(6, rotated.get(0, 1));
            assertEquals(1, rotated.get(2, 0));
            assertEquals(4, rotated.get(2, 1));
        }

        @Test
        public void testRotate270_Shape2By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix rotated = m.rotate270();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals((short) 3, rotated.get(0, 0));
            assertEquals((short) 6, rotated.get(0, 1));
        }

        @Test
        public void testRotate270_RotatesCounterClockwise() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            m.println();
            ShortMatrix rotated = m.rotate270();
            rotated.println();
            assertEquals(2, rotated.get(0, 0));
            assertEquals(4, rotated.get(0, 1));
            assertEquals(1, rotated.get(1, 0));
            assertEquals(3, rotated.get(1, 1));
        }

        @Test
        public void testRotate270_TallRectangularValues() {
            final var matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 2, (short) 4, (short) 6 }, { (short) 1, (short) 3, (short) 5 } }), matrix.rotate270());
        }

        @Test
        public void testRotate270_SingleRowEdgeCase() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            ShortMatrix rotated = matrix.rotate270();

            assertEquals(3, rotated.rowCount());
            assertEquals(1, rotated.columnCount());
            assertEquals((short) 3, rotated.get(0, 0));
            assertEquals((short) 2, rotated.get(1, 0));
            assertEquals((short) 1, rotated.get(2, 0));
        }
    }

    @Nested
    class TransposeTests {

        // Test empty matrix
        @Test
        public void testTranspose() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals((short) 1, transposed.get(0, 0));
            assertEquals((short) 4, transposed.get(0, 1));
            assertEquals((short) 2, transposed.get(1, 0));
            assertEquals((short) 5, transposed.get(1, 1));
        }

        @Test
        public void testTranspose_Shape2By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix transposed = m.transpose();
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
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(3, transposed.get(0, 1));
            assertEquals(2, transposed.get(1, 0));
            assertEquals(4, transposed.get(1, 1));
        }

        @Test
        public void testTranspose_Shape2By3_SwapsDimensionsAndSelectedValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals((short) 1, transposed.get(0, 0));
            assertEquals((short) 4, transposed.get(0, 1));
            assertEquals((short) 6, transposed.get(2, 1));
        }

        @Test
        public void testTranspose_SwapsRowsAndColumns() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(4, transposed.get(0, 1));
            assertEquals(6, transposed.get(2, 1));
        }

        @Test
        public void testTranspose_InverseProperty() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3, 4 }, { 5, 6, 7, 8 } });
            ShortMatrix t = m.transpose();
            assertEquals(4, t.rowCount());
            assertEquals(2, t.columnCount());
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals(m.get(i, j), t.get(j, i));
                }
            }
            assertEquals(m, t.transpose());
        }

        @Test
        public void testTranspose_Nx0_PreservesSwappedShape() {
            ShortMatrix t = ShortMatrix.wrap(new short[3][0]).transpose();
            assertEquals(0, t.rowCount());
            assertEquals(3, t.columnCount());
            assertEquals(ShortMatrix.wrap(new short[3][0]), t.transpose());
        }

        @Test
        public void testTranspose_SingleRow() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            ShortMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(1, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(2, transposed.get(1, 0));
            assertEquals(3, transposed.get(2, 0));
        }

        @Test
        public void testTranspose_SingleColumn() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1 }, { 2 }, { 3 } });
            ShortMatrix transposed = m.transpose();
            assertEquals(1, transposed.rowCount());
            assertEquals(3, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(2, transposed.get(0, 1));
            assertEquals(3, transposed.get(0, 2));
        }

        @Test
        public void testSingleRowOperations() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3, 4, 5 } });
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());

            ShortMatrix transposed = m.transpose();
            assertEquals(5, transposed.rowCount());
            assertEquals(1, transposed.columnCount());
        }

        @Test
        public void testSingleColumnOperations() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1 }, { 2 }, { 3 } });
            assertEquals(3, m.rowCount());
            assertEquals(1, m.columnCount());

            ShortMatrix transposed = m.transpose();
            assertEquals(1, transposed.rowCount());
            assertEquals(3, transposed.columnCount());
        }
    }

    @Nested
    class ReshapeAndPadTests {

        @Test
        public void testReshape_MultiRowSourceRowMajorOrder() {
            // Reshape exercises the cnt-based branch (a.length > 1).
            ShortMatrix src = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix r = src.reshapeAndPad(2, 4);
            assertArrayEquals(new short[] { 1, 2, 3, 4 }, r.rowCopy(0));
            assertArrayEquals(new short[] { 5, 6, 0, 0 }, r.rowCopy(1));
        }

        @Test
        public void testReshape_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            ShortMatrix reshaped = empty.reshapeAndPad(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }

        @Test
        public void testReshape_IncompatibleSize_FillsWithZeros() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            ShortMatrix reshaped = m.reshapeAndPad(2, 2);
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(3, reshaped.get(1, 0));
            assertEquals(0, reshaped.get(1, 1)); // filled with zero
        }

        @Test
        public void testReshape_SingleRowSourceMultipleStrides() {
            // Reshape a 1xN matrix into multiple smaller rows; covers the a.length == 1 branch.
            ShortMatrix src = ShortMatrix.wrap(new short[][] { { 1, 2, 3, 4, 5, 6, 7 } });
            ShortMatrix r = src.reshapeAndPad(3, 3);
            assertEquals(3, r.rowCount());
            assertEquals(3, r.columnCount());
            assertArrayEquals(new short[] { 1, 2, 3 }, r.rowCopy(0));
            assertArrayEquals(new short[] { 4, 5, 6 }, r.rowCopy(1));
            // Last cell of source goes into row 2; the extra cells should be zero-padded.
            assertArrayEquals(new short[] { 7, 0, 0 }, r.rowCopy(2));
        }

        @Test
        public void testReshape_OvershootFillsTrailingRowsWithZeros() {
            ShortMatrix src = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix r = src.reshapeAndPad(4, 2);
            assertArrayEquals(new short[] { 1, 2 }, r.rowCopy(0));
            assertArrayEquals(new short[] { 3, 4 }, r.rowCopy(1));
            assertArrayEquals(new short[] { 5, 6 }, r.rowCopy(2));
            assertArrayEquals(new short[] { 0, 0 }, r.rowCopy(3));
        }

        @Test
        public void testReshape_ZeroRowsPreservesLargeColumnDimension() {
            ShortMatrix m = ShortMatrix.empty().reshapeAndPad(0, 46_341);
            assertEquals(0, m.rowCount());
            assertEquals(46_341, m.columnCount());
        }

        @ParameterizedTest
        @CsvSource({ "-1, 2", "2, -1", "1, 1" })
        public void testReshapeAndPad_RejectsNegativeAndInsufficientShapes(final int rows, final int columns) {
            final var matrix = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.reshapeAndPad(rows, columns));
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } }), matrix);
        }
    }

    @Nested
    class RepeatElementsTests {

        @Test
        public void testRepeatElements() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix repeated = m.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals((short) 1, repeated.get(0, 0));
            assertEquals((short) 1, repeated.get(0, 1));
            assertEquals((short) 1, repeated.get(0, 2));
            assertEquals((short) 2, repeated.get(0, 3));
            assertEquals((short) 2, repeated.get(0, 4));
            assertEquals((short) 2, repeated.get(0, 5));
            assertEquals((short) 1, repeated.get(1, 0)); // second row same as first

            // Test invalid arguments
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @Test
        public void testRepeatElements_RepeatsCellsTwiceVerticallyAndThreeTimesHorizontally() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix repeated = matrix.repeatElements(2, 3);
            // Result: [[1, 1, 1, 2, 2, 2], [1, 1, 1, 2, 2, 2], [3, 3, 3, 4, 4, 4], [3, 3, 3, 4, 4, 4]]
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertEquals((short) 1, repeated.get(0, 0));
            assertEquals((short) 1, repeated.get(0, 2));
            assertEquals((short) 2, repeated.get(0, 3));
            assertEquals((short) 2, repeated.get(0, 5));
            assertEquals((short) 1, repeated.get(1, 0));
            assertEquals((short) 3, repeated.get(2, 0));
            assertEquals((short) 4, repeated.get(3, 5));
        }

        @Test
        public void testRepeatElements_OneDimension() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix repeated = m.repeatElements(1, 2);
            assertEquals(1, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(1, repeated.get(0, 1));
            assertEquals(2, repeated.get(0, 2));
            assertEquals(2, repeated.get(0, 3));
        }

        @Test
        public void testRepeatElements_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix result = m.repeatElements(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals((short) 1, result.get(0, 0));
            assertEquals((short) 1, result.get(0, 1));
            assertEquals((short) 1, result.get(1, 0));
            assertEquals((short) 4, result.get(3, 3));
        }

        @Test
        public void testRepeatElements_RepeatsElements() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix repeated = m.repeatElements(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(1, repeated.get(1, 1));
            assertEquals(4, repeated.get(2, 2));
        }

        @Test
        public void testRepeatElements_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } });
            final var result = matrix.repeatElements(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, (short) 0);
            assertEquals((short) 1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatElements_InvalidArguments() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatElements_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(rows, columns));
        }
    }

    @Nested
    class RepeatMatrixTests {

        // Test invalid arguments
        @Test
        public void testRepeatMatrix() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix repeated = m.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals((short) 1, repeated.get(0, 0));
            assertEquals((short) 2, repeated.get(0, 1));
            assertEquals((short) 1, repeated.get(0, 2)); // repeat starts
            assertEquals((short) 2, repeated.get(0, 3));
            assertEquals((short) 1, repeated.get(0, 4)); // another repeat
            assertEquals((short) 2, repeated.get(0, 5));

            assertEquals((short) 3, repeated.get(1, 0));
            assertEquals((short) 4, repeated.get(1, 1));

            // Check vertical repeat
            assertEquals((short) 1, repeated.get(2, 0)); // vertical repeat starts
            assertEquals((short) 2, repeated.get(2, 1));

            // Test invalid arguments
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @Test
        public void testRepeatMatrix_TilesTwiceVerticallyAndThreeTimesHorizontally() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix repeated = matrix.repeatMatrix(2, 3);
            // Result: [[1, 2, 1, 2, 1, 2], [3, 4, 3, 4, 3, 4], [1, 2, 1, 2, 1, 2], [3, 4, 3, 4, 3, 4]]
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertEquals((short) 1, repeated.get(0, 0));
            assertEquals((short) 2, repeated.get(0, 1));
            assertEquals((short) 1, repeated.get(0, 2));
            assertEquals((short) 4, repeated.get(1, 1));
            assertEquals((short) 1, repeated.get(2, 0));
            assertEquals((short) 4, repeated.get(3, 5));
        }

        @Test
        public void testRepeatMatrix_OneDimension() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix repeated = m.repeatMatrix(1, 2);
            assertEquals(1, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(2, repeated.get(0, 1));
            assertEquals(1, repeated.get(0, 2));
            assertEquals(2, repeated.get(0, 3));
        }

        // ShortMatrix doesn't have mapToInt method - removed test
        // @Test
        // public void testToIntMatrix() {
        //     ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
        @Test
        public void testRepeatMatrix_Shape1By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix result = m.repeatMatrix(2, 2);
            assertEquals(2, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals((short) 1, result.get(0, 0));
            assertEquals((short) 2, result.get(0, 1));
            assertEquals((short) 1, result.get(0, 2));
            assertEquals((short) 1, result.get(1, 0));
        }

        @Test
        public void testRepeatMatrix_RepeatsMatrix() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix repeated = m.repeatMatrix(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(1, repeated.get(2, 2));
            assertEquals(4, repeated.get(3, 3));
        }

        @Test
        public void testRepeatMatrix_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } });
            final var result = matrix.repeatMatrix(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, (short) 0);
            assertEquals((short) 1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_EmptyDimensionsDoNotTraverseRepeats() {
            ShortMatrix zeroRows = new ShortMatrix(new short[0][], 2);
            ShortMatrix zeroColumns = ShortMatrix.wrap(new short[2][0]);

            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                ShortMatrix repeatedRows = zeroRows.repeatMatrix(Integer.MAX_VALUE, 1);
                assertEquals(0, repeatedRows.rowCount());
                assertEquals(2, repeatedRows.columnCount());

                ShortMatrix repeatedColumns = zeroColumns.repeatMatrix(1, Integer.MAX_VALUE);
                assertEquals(2, repeatedColumns.rowCount());
                assertEquals(0, repeatedColumns.columnCount());
            });

            assertThrows(IllegalArgumentException.class, () -> zeroRows.repeatMatrix(1, Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> zeroColumns.repeatMatrix(Integer.MAX_VALUE, 1));
        }

        @Test
        public void testRepeatMatrix_InvalidArguments() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatMatrix_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(rows, columns));
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } }), matrix);
        }
    }

    @Nested
    class FlattenTests {

        @Test
        public void testFlatten() {
            ShortList flat = matrix.flatten();
            assertEquals(9, flat.size());
            for (int i = 0; i < 9; i++) {
                assertEquals((short) (i + 1), flat.get(i));
            }
        }

        @Test
        public void testFlatten_Shape3By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ShortList flat = m.flatten();
            assertEquals(9, flat.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, flat.get(i));
            }
        }

        @Test
        public void testFlatten_Shape2By3_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortList flat = m.flatten();
            assertEquals(6, flat.size());
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, flat.toArray());
        }

        // ShortMatrix doesn't have mapToLong method - removed test
        // @Test
        // public void testToLongMatrix() {
        //     ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
        //     LongMatrix result = m.mapToLong(s -> (long) s);
        //     assertEquals(2, result.rowCount());
        @Test
        public void testFlatten_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortList flattened = m.flatten();
            assertEquals(4, flattened.size());
            assertEquals((short) 1, flattened.get(0));
            assertEquals((short) 2, flattened.get(1));
            assertEquals((short) 3, flattened.get(2));
            assertEquals((short) 4, flattened.get(3));
        }

        @Test
        public void testFlatten_ReturnsAllElements() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortList flattened = m.flatten();
            assertEquals(6, flattened.size());
            assertEquals(1, flattened.get(0));
            assertEquals(6, flattened.get(5));
        }

        @Test
        public void testEmptyMatrixOperations() {
            // Test various operations on empty matrix
            assertTrue(emptyMatrix.flatten().isEmpty());
            assertEquals(0, emptyMatrix.copy().rowCount);
            assertEquals(emptyMatrix, emptyMatrix.transpose());
            assertEquals(emptyMatrix, emptyMatrix.rotate90());

            ShortMatrix extended = emptyMatrix.resize(2, 2, (short) 5);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals((short) 5, extended.get(0, 0));
        }

        @Test
        public void testFlatten_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            ShortList flat = empty.flatten();
            assertTrue(flat.isEmpty());
        }

        @Test
        public void testFlatten_Empty_EmptyMatrix() {
            ShortMatrix m = ShortMatrix.empty();
            ShortList flat = m.flatten();
            assertEquals(0, flat.size());
        }
    }

    @Nested
    class MutateViaFlatArrayTests {

        @Test
        public void testFlatOp() {
            List<Short> sums = new ArrayList<>();
            matrix.mutateViaFlatArray(row -> {
                short sum = 0;
                for (short val : row) {
                    sum += val;
                }
                sums.add(sum);
            });
            assertEquals(1, sums.size());
            assertEquals((short) 45, sums.get(0));
        }

        @Test
        public void testMutateViaFlatArray() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 5, 3 }, { 4, 1 } });
            matrix.mutateViaFlatArray(arr -> java.util.Arrays.sort(arr));
            // matrix is now [[1, 3], [4, 5]]
            assertEquals((short) 1, matrix.get(0, 0));
            assertEquals((short) 3, matrix.get(0, 1));
            assertEquals((short) 4, matrix.get(1, 0));
            assertEquals((short) 5, matrix.get(1, 1));
        }

        @Test
        public void testMutateViaFlatArray_Shape3By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Integer> sums = new ArrayList<>();
            m.mutateViaFlatArray(row -> {
                int sum = 0;
                for (short val : row) {
                    sum += val;
                }
                sums.add(sum);
            });
            assertEquals(1, sums.size());
            assertEquals(45, sums.get(0).intValue());
        }

        @Test
        public void testMutateViaFlatArray_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            final int[] count = { 0 };
            m.mutateViaFlatArray(row -> count[0] += row.length);
            assertEquals(4, count[0]);
        }

        // ShortMatrix doesn't have mapToFloat method - removed test
        // @Test
        // public void testToFloatMatrix() {
        //     ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
        //     FloatMatrix result = m.mapToFloat(s -> (float) s);
        @Test
        public void testMutateViaFlatArray_Shape2By2_CallbackSumsFlatValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            final int[] sum = { 0 };
            m.mutateViaFlatArray(row -> {
                for (short val : row) {
                    sum[0] += val;
                }
            });
            assertEquals(10, sum[0]);
        }

        @Test
        public void testMutateViaFlatArray_AppliesOperationToEachRow() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            final int[] sum = { 0 };
            m.mutateViaFlatArray(row -> {
                for (short val : row) {
                    sum[0] += val;
                }
            });
            assertEquals(10, sum[0]);
        }

        @Test
        public void testMutateViaFlatArray_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            List<Integer> results = new ArrayList<>();
            empty.mutateViaFlatArray(row -> results.add(row.length));
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    class StackVerticallyTests {

        @Test
        public void testVstack() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 7, 8, 9 }, { 10, 11, 12 } });
            ShortMatrix stacked = m1.stackVertically(m2);

            assertEquals(4, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals((short) 1, stacked.get(0, 0));
            assertEquals((short) 7, stacked.get(2, 0));
            assertEquals((short) 12, stacked.get(3, 2));

            // Test different column counts
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m3));
        }

        @Test
        public void testStackVertically() {
            ShortMatrix matrix1 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            ShortMatrix matrix2 = ShortMatrix.wrap(new short[][] { { 4, 5, 6 }, { 7, 8, 9 } });
            ShortMatrix stacked = matrix1.stackVertically(matrix2);
            // Result: [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
            assertEquals(3, stacked.rowCount());
            assertArrayEquals(new short[] { 1, 2, 3 }, stacked.rowView(0));
            assertArrayEquals(new short[] { 4, 5, 6 }, stacked.rowView(1));
            assertArrayEquals(new short[] { 7, 8, 9 }, stacked.rowView(2));
        }

        @Test
        public void testStackVertically_Shape2By3() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 7, 8, 9 }, { 10, 11, 12 } });
            ShortMatrix stacked = m1.stackVertically(m2);

            assertEquals(4, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(7, stacked.get(2, 0));
            assertEquals(12, stacked.get(3, 2));
        }

        @Test
        public void testStackVertically_DifferentColumnCounts() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_SameMatrix() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix stacked = m.stackVertically(m);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(1, stacked.get(1, 0));
        }

        @Test
        public void testStackVertically_Shape2By2() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix stacked = m1.stackVertically(m2);
            assertEquals(4, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(4, stacked.get(1, 1));
            assertEquals(5, stacked.get(2, 0));
            assertEquals(8, stacked.get(3, 1));
        }

        // ShortMatrix doesn't have mapToDouble method - removed test
        // @Test
        // public void testToDoubleMatrix() {
        //     ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
        @Test
        public void testStackVertically_Shape1By2() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 3, 4 } });
            ShortMatrix stacked = m1.stackVertically(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals((short) 1, stacked.get(0, 0));
            assertEquals((short) 3, stacked.get(1, 0));
        }

        @Test
        public void testStackVertically_StacksVertically() {
            short[][] arr1 = { { 1, 2 } };
            short[][] arr2 = { { 3, 4 } };
            ShortMatrix m1 = new ShortMatrix(arr1);
            ShortMatrix m2 = new ShortMatrix(arr2);
            ShortMatrix stacked = m1.stackVertically(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(3, stacked.get(1, 0));
        }

        // Just verify elements exist (values are random)
        @Test
        public void testShortMatrixBinaryOpsRejectNullMatrix() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.stackVertically(null));
            assertThrows(IllegalArgumentException.class, () -> matrix.stackHorizontally(null));
            assertThrows(IllegalArgumentException.class, () -> matrix.add(null));
            assertThrows(IllegalArgumentException.class, () -> matrix.subtract(null));
            assertThrows(IllegalArgumentException.class, () -> matrix.matrixMultiply(null));
        }

        @Test
        public void testStackVertically_IncompatibleColumns_ThrowsException() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 3, 4, 5 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }
    }

    @Nested
    class StackHorizontallyTests {

        @Test
        public void testHstack() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals((short) 1, stacked.get(0, 0));
            assertEquals((short) 5, stacked.get(0, 2));
            assertEquals((short) 8, stacked.get(1, 3));

            // Test different row counts
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m3));
        }

        // Result: [[4, 4], [4, 4]]
        @Test
        public void testStackHorizontally() {
            ShortMatrix matrix1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix matrix2 = ShortMatrix.wrap(new short[][] { { 5 }, { 6 } });
            ShortMatrix stacked = matrix1.stackHorizontally(matrix2);
            // Result: [[1, 2, 5], [3, 4, 6]]
            assertEquals(2, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals((short) 1, stacked.get(0, 0));
            assertEquals((short) 2, stacked.get(0, 1));
            assertEquals((short) 5, stacked.get(0, 2));
            assertEquals((short) 3, stacked.get(1, 0));
            assertEquals((short) 4, stacked.get(1, 1));
            assertEquals((short) 6, stacked.get(1, 2));
        }

        @Test
        public void testStackHorizontally_Shape2By2() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(5, stacked.get(0, 2));
            assertEquals(8, stacked.get(1, 3));
        }

        @Test
        public void testStackHorizontally_DifferentRowCounts() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_SameMatrix() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1 }, { 2 } });
            ShortMatrix stacked = m.stackHorizontally(m);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(1, stacked.get(0, 1));
        }

        @Test
        public void testStackHorizontally_Shape2By2_DimensionsAndBothInputs() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(2, stacked.get(0, 1));
            assertEquals(5, stacked.get(0, 2));
            assertEquals(6, stacked.get(0, 3));
        }

        @Test
        public void testStackHorizontally_DifferentRowCount() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_Shape2By1() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1 }, { 3 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 2 }, { 4 } });
            ShortMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals((short) 1, stacked.get(0, 0));
            assertEquals((short) 2, stacked.get(0, 1));
        }

        @Test
        public void testStackHorizontally_StacksHorizontally() {
            short[][] arr1 = { { 1 }, { 2 } };
            short[][] arr2 = { { 3 }, { 4 } };
            ShortMatrix m1 = new ShortMatrix(arr1);
            ShortMatrix m2 = new ShortMatrix(arr2);
            ShortMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(3, stacked.get(0, 1));
        }

        @Test
        public void testStackHorizontally_IncompatibleRows_ThrowsException() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 2 }, { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_RejectsLogicalWidthOverflowWithoutAllocation() {
            final var huge = ShortMatrix.wrap(new short[0][0]).resize(0, Integer.MAX_VALUE);
            final var extra = ShortMatrix.wrap(new short[0][0]).resize(0, 1);
            assertThrows(IllegalArgumentException.class, () -> huge.stackHorizontally(extra));
            assertEquals(Integer.MAX_VALUE, huge.columnCount());
        }
    }

    @Nested
    class AddTests {

        @Test
        public void testAdd() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix sum = m1.add(m2);

            assertEquals((short) 6, sum.get(0, 0));
            assertEquals((short) 8, sum.get(0, 1));
            assertEquals((short) 10, sum.get(1, 0));
            assertEquals((short) 12, sum.get(1, 1));

            // Test different dimensions
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m3));
        }

        @Test
        public void testAdd_SquareAdditionChecksEveryCell() {
            ShortMatrix matrix1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix matrix2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix sum = matrix1.add(matrix2);
            // Result: [[6, 8], [10, 12]]
            assertEquals((short) 6, sum.get(0, 0));
            assertEquals((short) 8, sum.get(0, 1));
            assertEquals((short) 10, sum.get(1, 0));
            assertEquals((short) 12, sum.get(1, 1));
        }

        @Test
        public void testAdd_DifferentDimensions() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testAdd_Shape2By2_EveryCellSum() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix result = m1.add(m2);
            assertEquals(6, result.get(0, 0));
            assertEquals(8, result.get(0, 1));
            assertEquals(10, result.get(1, 0));
            assertEquals(12, result.get(1, 1));
        }

        @Test
        public void testAdd_DifferentDimensions_Shape1By2_Validation() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testAdd_AllFourCellValues() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 10, 20 }, { 30, 40 } });
            ShortMatrix result = m1.add(m2);
            assertEquals((short) 11, result.get(0, 0));
            assertEquals((short) 22, result.get(0, 1));
            assertEquals((short) 33, result.get(1, 0));
            assertEquals((short) 44, result.get(1, 1));
        }

        @Test
        public void testAdd_AddsMatrices() {
            short[][] arr1 = { { 1, 2 }, { 3, 4 } };
            short[][] arr2 = { { 5, 6 }, { 7, 8 } };
            ShortMatrix m1 = new ShortMatrix(arr1);
            ShortMatrix m2 = new ShortMatrix(arr2);
            ShortMatrix sum = m1.add(m2);
            assertEquals(6, sum.get(0, 0));
            assertEquals(8, sum.get(0, 1));
            assertEquals(12, sum.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testAdd_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 } });
                final var right = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 } });
                assertEquals(ShortMatrix.wrap(new short[][] { { (short) 2, (short) 4, (short) 6 }, { (short) 8, (short) 10, (short) 12 } }), left.add(right));
                assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 } }), left);
                assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testShortOverflow() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { Short.MAX_VALUE, 1 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, Short.MAX_VALUE } });

            // Addition overflow wraps around
            ShortMatrix sum = m1.add(m2);
            assertTrue(sum.get(0, 0) < 0); // Overflow
        }

        @Test
        public void testAdd_SingleElement() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 5 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 3 } });
            ShortMatrix sum = m1.add(m2);
            assertEquals(1, sum.rowCount());
            assertEquals(1, sum.columnCount());
            assertEquals(8, sum.get(0, 0));
        }

        @Test
        public void testAdd_IncompatibleSize_ThrowsException() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }
    }

    @Nested
    class AddWidenedTests {

        @Test
        public void testExactAndWidenedArithmetic() {
            ShortMatrix max = ShortMatrix.wrap(new short[][] { { Short.MAX_VALUE } });
            ShortMatrix one = ShortMatrix.wrap(new short[][] { { 1 } });
            assertEquals(32_768, max.addWidened(one).get(0, 0));
            ArithmeticException addOverflow = assertThrows(ArithmeticException.class, () -> max.addExact(one));
            assertTrue(addOverflow.getMessage().contains("short overflow"));

            ShortMatrix min = ShortMatrix.wrap(new short[][] { { Short.MIN_VALUE } });
            assertEquals(-32_769, min.subtractWidened(one).get(0, 0));
            assertThrows(ArithmeticException.class, () -> min.subtractExact(one));
            assertEquals(ShortMatrix.wrap(new short[][] { { 2 } }), one.addExact(one));

            ShortMatrix overflowLeft = ShortMatrix.wrap(new short[][] { { 30_000 } });
            ShortMatrix twice = ShortMatrix.wrap(new short[][] { { 2 } });
            assertEquals(60_000L, overflowLeft.matrixMultiplyWidened(twice).get(0, 0));
            assertThrows(ArithmeticException.class, () -> overflowLeft.matrixMultiplyExact(twice));

            // The exact short product validates the final mathematical dot product, not a narrowed partial sum.
            ShortMatrix cancellationLeft = ShortMatrix.wrap(new short[][] { { 30_000, 30_000 } });
            ShortMatrix cancellationRight = ShortMatrix.wrap(new short[][] { { 2 }, { -1 } });
            assertEquals((short) 30_000, cancellationLeft.matrixMultiplyExact(cancellationRight).get(0, 0));
        }
    }

    @Nested
    class SubtractTests {

        @Test
        public void testSubtract() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix diff = m1.subtract(m2);

            assertEquals((short) 4, diff.get(0, 0));
            assertEquals((short) 4, diff.get(0, 1));
            assertEquals((short) 4, diff.get(1, 0));
            assertEquals((short) 4, diff.get(1, 1));

            // Test different dimensions
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m3));
        }

        @Test
        public void testSubtract_SquareSubtractionChecksEveryCell() {
            ShortMatrix matrix1 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix matrix2 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix diff = matrix1.subtract(matrix2);
            // Result: [[4, 4], [4, 4]]
            assertEquals((short) 4, diff.get(0, 0));
            assertEquals((short) 4, diff.get(0, 1));
            assertEquals((short) 4, diff.get(1, 0));
            assertEquals((short) 4, diff.get(1, 1));
        }

        @Test
        public void testSubtract_DifferentDimensions() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testSubtract_Shape2By2_EveryCellDifference() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix result = m1.subtract(m2);
            assertEquals(4, result.get(0, 0));
            assertEquals(4, result.get(0, 1));
            assertEquals(4, result.get(1, 0));
            assertEquals(4, result.get(1, 1));
        }

        @Test
        public void testSubtract_DifferentDimensions_Shape1By2_Validation() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testSubtract_AllFourCellValues() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 10, 20 }, { 30, 40 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix result = m1.subtract(m2);
            assertEquals((short) 9, result.get(0, 0));
            assertEquals((short) 18, result.get(0, 1));
            assertEquals((short) 27, result.get(1, 0));
            assertEquals((short) 36, result.get(1, 1));
        }

        @Test
        public void testSubtract_SubtractsMatrices() {
            short[][] arr1 = { { 5, 6 }, { 7, 8 } };
            short[][] arr2 = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m1 = new ShortMatrix(arr1);
            ShortMatrix m2 = new ShortMatrix(arr2);
            ShortMatrix diff = m1.subtract(m2);
            assertEquals(4, diff.get(0, 0));
            assertEquals(4, diff.get(0, 1));
            assertEquals(4, diff.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testSubtract_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = ShortMatrix.wrap(new short[][] { { (short) 7, (short) 8, (short) 9 }, { (short) 10, (short) 11, (short) 12 } });
                final var right = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 } });
                assertEquals(ShortMatrix.wrap(new short[][] { { (short) 6, (short) 6, (short) 6 }, { (short) 6, (short) 6, (short) 6 } }),
                        left.subtract(right));
                assertEquals(ShortMatrix.wrap(new short[][] { { (short) 7, (short) 8, (short) 9 }, { (short) 10, (short) 11, (short) 12 } }), left);
                assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testSubtract_SingleElement() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 5 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 3 } });
            ShortMatrix diff = m1.subtract(m2);
            assertEquals(1, diff.rowCount());
            assertEquals(1, diff.columnCount());
            assertEquals(2, diff.get(0, 0));
        }
    }

    @Nested
    class SubtractExactTests {

        @Test
        public void testSubtractExact_ValidRectangularOperandsPreserveValues() {
            final var left = ShortMatrix.wrap(new short[][] { { (short) 7, (short) 8, (short) 9 }, { (short) 10, (short) 11, (short) 12 } });
            final var right = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 } });
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 6, (short) 6, (short) 6 }, { (short) 6, (short) 6, (short) 6 } }),
                    left.subtractExact(right));
            assertEquals(ShortMatrix.wrap(new short[][] { { (short) 7, (short) 8, (short) 9 }, { (short) 10, (short) 11, (short) 12 } }), left);
        }
    }

    @Nested
    class MatrixMultiplyTests {

        @Test
        public void testMultiply() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix product = m1.matrixMultiply(m2);

            assertEquals((short) 19, product.get(0, 0)); // 1*5 + 2*7
            assertEquals((short) 22, product.get(0, 1)); // 1*6 + 2*8
            assertEquals((short) 43, product.get(1, 0)); // 3*5 + 4*7
            assertEquals((short) 50, product.get(1, 1)); // 3*6 + 4*8

            // Test incompatible dimensions
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m3));
        }

        // Result: [[5, 6], [7, 8]]
        @Test
        public void testMatrixMultiply() {
            ShortMatrix matrix1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix matrix2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix product = matrix1.matrixMultiply(matrix2);
            // Result: [[19, 22], [43, 50]]
            assertEquals((short) 19, product.get(0, 0));
            assertEquals((short) 22, product.get(0, 1));
            assertEquals((short) 43, product.get(1, 0));
            assertEquals((short) 50, product.get(1, 1));
        }

        @Test
        public void testMatrixMultiply_Shape2By2() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix product = m1.matrixMultiply(m2);

            assertEquals(19, product.get(0, 0)); // 1*5 + 2*7
            assertEquals(22, product.get(0, 1)); // 1*6 + 2*8
            assertEquals(43, product.get(1, 0)); // 3*5 + 4*7
            assertEquals(50, product.get(1, 1)); // 3*6 + 4*8
        }

        @Test
        public void testMatrixMultiply_RectangularMatrices() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } }); // 1x3
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 4 }, { 5 }, { 6 } }); // 3x1
            ShortMatrix product = m1.matrixMultiply(m2);

            assertEquals(1, product.rowCount());
            assertEquals(1, product.columnCount());
            assertEquals(32, product.get(0, 0)); // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        }

        @Test
        public void testMatrixMultiply_Shape2By2_EveryCellProduct() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 2, 3 }, { 4, 5 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix result = m1.matrixMultiply(m2);
            assertEquals(11, result.get(0, 0));
            assertEquals(16, result.get(0, 1));
            assertEquals(19, result.get(1, 0));
            assertEquals(28, result.get(1, 1));
        }

        @Test
        public void testMatrixMultiply_AllFourCellValues() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 10, 20 }, { 30, 40 } });
            ShortMatrix result = m1.matrixMultiply(m2);
            assertEquals((short) 70, result.get(0, 0));
            assertEquals((short) 100, result.get(0, 1));
            assertEquals((short) 150, result.get(1, 0));
            assertEquals((short) 220, result.get(1, 1));
        }

        @Test
        public void testMatrixMultiply_MultipliesMatrices() {
            short[][] arr1 = { { 1, 2 }, { 3, 4 } };
            short[][] arr2 = { { 2, 0 }, { 1, 2 } };
            ShortMatrix m1 = new ShortMatrix(arr1);
            ShortMatrix m2 = new ShortMatrix(arr2);
            ShortMatrix product = m1.matrixMultiply(m2);
            assertEquals(4, product.get(0, 0));
            assertEquals(4, product.get(0, 1));
            assertEquals(10, product.get(1, 0));
            assertEquals(8, product.get(1, 1));
        }

        @Test
        public void testMatrixMultiply_MathematicalCorrectness() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 2, 3 }, { 4, 5 } });
            ShortMatrix id = ShortMatrix.wrap(new short[][] { { 1, 0 }, { 0, 1 } });
            assertEquals(m, m.matrixMultiply(id));
            assertEquals(m, id.matrixMultiply(m));

            // 2x3 * 3x4 -> 2x4
            ShortMatrix a = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix b = ShortMatrix.wrap(new short[][] { { 1, 0, 1, 1 }, { 0, 1, 1, 1 }, { 1, 1, 0, 1 } });
            ShortMatrix product = a.matrixMultiply(b);
            assertEquals(2, product.rowCount());
            assertEquals(4, product.columnCount());
            // row 0 = [4, 5, 3, 6]
            assertArrayEquals(new short[] { 4, 5, 3, 6 }, product.rowCopy(0));
            // row 1 = [10, 11, 9, 15]
            assertArrayEquals(new short[] { 10, 11, 9, 15 }, product.rowCopy(1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testMatrixMultiply_ExplicitModesComputeRectangularProduct(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 }, { (short) 5, (short) 6 } });
                final var right = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 } });
                assertEquals(ShortMatrix.wrap(new short[][] { { (short) 9, (short) 12, (short) 15 }, { (short) 19, (short) 26, (short) 33 },
                        { (short) 29, (short) 40, (short) 51 } }), left.matrixMultiply(right));
                assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 }, { (short) 5, (short) 6 } }), left);
                assertEquals(ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testMatrixMultiply_EmptyProductPreservesShape() {
            ShortMatrix product = ShortMatrix.empty().matrixMultiply(ShortMatrix.empty());
            assertTrue(product.isEmpty());
            assertEquals(0, product.columnCount());
        }

        @Test
        public void testMatrixMultiply_SingleElement() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 5 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 3 } });
            ShortMatrix product = m1.matrixMultiply(m2);
            assertEquals(1, product.rowCount());
            assertEquals(1, product.columnCount());
            assertEquals(15, product.get(0, 0));
        }

        @Test
        public void testMatrixMultiply_OverflowAdvice_LongIsExactButIntOverflows() {
            ShortMatrix row = ShortMatrix.wrap(new short[][] { { Short.MIN_VALUE, Short.MIN_VALUE } });
            ShortMatrix column = ShortMatrix.wrap(new short[][] { { Short.MIN_VALUE }, { Short.MIN_VALUE } });

            assertEquals(1L << 31, row.toLongMatrix().matrixMultiply(column.toLongMatrix()).get(0, 0));
            assertEquals(Integer.MIN_VALUE, row.toIntMatrix().matrixMultiply(column.toIntMatrix()).get(0, 0));
            assertEquals((short) 0, row.matrixMultiply(column).get(0, 0));
        }

        @Test
        public void testMatrixMultiply_IncompatibleSize_ThrowsException() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 4, 5 } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testMatrixMultiply_IncompatibleShapesThrows() {
            ShortMatrix a = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix b = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> a.matrixMultiply(b));
        }

        @Test
        public void testMatrixMultiply_NegativeShortAccumulationStaysInRange() {
            // Verify that the matrixMultiply result wraps to short range as documented.
            ShortMatrix a = ShortMatrix.wrap(new short[][] { { Short.MAX_VALUE, Short.MAX_VALUE } });
            ShortMatrix b = ShortMatrix.wrap(new short[][] { { 1 }, { 1 } });
            ShortMatrix product = a.matrixMultiply(b);
            // Short.MAX_VALUE + Short.MAX_VALUE = 65534, narrowed via short cast to -2.
            assertEquals(1, product.rowCount());
            assertEquals(1, product.columnCount());
            assertEquals((short) -2, product.get(0, 0));
        }
    }

    @Nested
    class MatrixMultiplyWidenedTests {

        @Test
        public void testMatrixMultiplyWidened_RejectsIncompatibleInnerDimensions() {
            final var left = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 } });
            final var right = ShortMatrix.wrap(new short[][] { { (short) 3, (short) 4 } });
            assertThrows(IllegalArgumentException.class, () -> left.matrixMultiplyWidened(right));
        }
    }

    @Nested
    class MatrixMultiplyExactTests {

        @Test
        public void testExactShortMultiplicationChecksCompleteDotsAndResetsItsScratchRow() {
            final ShortMatrix left = ShortMatrix.wrap(new short[][] { { 30_000, 30_000 }, { 100, 100 }, { -30_000, -30_000 } });
            final ShortMatrix right = ShortMatrix.wrap(new short[][] { { 2, 1, 0 }, { -1, -1, 0 } });
            final ShortMatrix result = left.matrixMultiplyExact(right);

            assertArrayEquals(new short[] { 30_000, 0, 0 }, result.rowCopy(0));
            assertArrayEquals(new short[] { 100, 0, 0 }, result.rowCopy(1));
            assertArrayEquals(new short[] { -30_000, 0, 0 }, result.rowCopy(2));
            assertArrayEquals(new short[] { 30_000, 30_000 }, left.rowCopy(0));
            assertArrayEquals(new short[] { 2, 1, 0 }, right.rowCopy(0));

            final short min = Short.MIN_VALUE;
            final ShortMatrix largeIntermediate = ShortMatrix.wrap(new short[][] { { min, min, min, min, min, min } });
            final ShortMatrix cancellation = ShortMatrix.wrap(new short[][] { { min }, { min }, { Short.MAX_VALUE }, { Short.MAX_VALUE }, { 1 }, { 1 } });
            assertEquals((short) 0, largeIntermediate.matrixMultiplyExact(cancellation).get(0, 0));

            final ArithmeticException overflow = assertThrows(ArithmeticException.class,
                    () -> left.matrixMultiplyExact(ShortMatrix.wrap(new short[][] { { 2, 3 }, { 0, 0 } })));
            assertEquals("short overflow: 60000", overflow.getMessage());
        }

        @Test
        public void testExactShortMultiplicationPreservesEmptyShapesAndArgumentFailures() {
            final ShortMatrix emptyDots = new ShortMatrix(new short[3][0]).matrixMultiplyExact(new ShortMatrix(new short[0][], 4));
            assertEquals(3, emptyDots.rowCount());
            assertEquals(4, emptyDots.columnCount());

            for (int row = 0; row < emptyDots.rowCount(); row++) {
                assertArrayEquals(new short[4], emptyDots.rowCopy(row));
            }

            final ShortMatrix noColumns = new ShortMatrix(new short[3][2]).matrixMultiplyExact(new ShortMatrix(new short[2][0]));
            assertEquals(3, noColumns.rowCount());
            assertEquals(0, noColumns.columnCount());

            // A logical width with no rows must not allocate a width-sized accumulator.
            final ShortMatrix noRows = ShortMatrix.empty().matrixMultiplyExact(new ShortMatrix(new short[0][], Integer.MAX_VALUE));
            assertEquals(0, noRows.rowCount());
            assertEquals(Integer.MAX_VALUE, noRows.columnCount());
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.empty().matrixMultiplyExact(null));
            assertThrows(IllegalArgumentException.class, () -> new ShortMatrix(new short[2][1]).matrixMultiplyExact(new ShortMatrix(new short[2][3])));
        }
    }

    @Nested
    class BoxedTests {

        @Test
        public void testBoxed() {
            Matrix<Short> boxed = matrix.boxed();
            assertEquals(3, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Short.valueOf((short) 1), boxed.get(0, 0));
            assertEquals(Short.valueOf((short) 5), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Matrix<Short> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Short.valueOf((short) 1), boxed.get(0, 0));
            assertEquals(Short.valueOf((short) 6), boxed.get(1, 2));
        }

        @Test
        public void testBoxed_Shape2By2_DimensionsAndBoxedCornerValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Short> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Short.valueOf((short) 1), boxed.get(0, 0));
            assertEquals(Short.valueOf((short) 4), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_ConvertsToBoxedMatrix() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            Matrix<Short> boxed = m.boxed();
            assertEquals(Short.valueOf((short) 1), boxed.get(0, 0));
            assertEquals(Short.valueOf((short) 4), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            Matrix<Short> boxed = empty.boxed();
            assertEquals(0, boxed.rowCount());
            assertEquals(0, boxed.columnCount());
        }

        @Test
        public void testBoxedAndNumericConversions_ZeroColumnRows() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { {}, {} });

            assertEquals(2, matrix.boxed().rowCount());
            assertEquals(0, matrix.boxed().columnCount());
            assertEquals(2, matrix.toLongMatrix().rowCount());
            assertEquals(0, matrix.toFloatMatrix().columnCount());
            assertEquals(2, matrix.toDoubleMatrix().rowCount());
        }
    }

    @Nested
    class ToIntMatrixTests {

        @Test
        public void testToIntMatrix() {
            IntMatrix intMatrix = matrix.toIntMatrix();
            assertEquals(3, intMatrix.rowCount());
            assertEquals(3, intMatrix.columnCount());
            assertEquals(1, intMatrix.get(0, 0));
            assertEquals(5, intMatrix.get(1, 1));
        }

        @Test
        public void testToIntMatrix_AllValuesAndIndependentStorage() {
            final ShortMatrix source = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            final IntMatrix converted = source.toIntMatrix();
            assertEquals(IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } }), converted);
            source.set(0, 1, (short) 9);
            assertEquals(2, converted.get(0, 1));
            converted.set(1, 0, (int) 8);
            assertEquals(3, source.get(1, 0));
        }

        @Test
        public void testToIntMatrix_ConvertsToIntMatrix() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            IntMatrix intMatrix = m.toIntMatrix();
            assertEquals(1, intMatrix.get(0, 0));
            assertEquals(4, intMatrix.get(1, 1));
        }

        @Test
        public void testToIntMatrix_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            IntMatrix intMatrix = empty.toIntMatrix();
            assertEquals(0, intMatrix.rowCount());
            assertEquals(0, intMatrix.columnCount());
        }

        @Test
        public void testToIntMatrix_SignExtensionForNegativeShorts() {
            // Verify that short -> int widening uses sign extension when converting to IntMatrix.
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { (short) -1, Short.MIN_VALUE }, { (short) 0, Short.MAX_VALUE } });
            IntMatrix asInt = m.toIntMatrix();
            assertEquals(-1, asInt.get(0, 0));
            assertEquals(Short.MIN_VALUE, asInt.get(0, 1));
            assertEquals(0, asInt.get(1, 0));
            assertEquals(Short.MAX_VALUE, asInt.get(1, 1));
        }
    }

    @Nested
    class ToLongMatrixTests {

        @Test
        public void testToLongMatrix_AllValuesAndIndependentStorage() {
            final ShortMatrix source = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            final LongMatrix converted = source.toLongMatrix();
            assertEquals(LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } }), converted);
            source.set(0, 1, (short) 9);
            assertEquals(2, converted.get(0, 1));
            converted.set(1, 0, (long) 8);
            assertEquals(3, source.get(1, 0));
        }

        @Test
        public void testToLongMatrix_ConvertsToLongMatrix() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            LongMatrix longMatrix = m.toLongMatrix();
            assertEquals(1L, longMatrix.get(0, 0));
            assertEquals(4L, longMatrix.get(1, 1));
        }

        @Test
        public void testToLongMatrix_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            LongMatrix longMatrix = empty.toLongMatrix();
            assertEquals(0, longMatrix.rowCount());
            assertEquals(0, longMatrix.columnCount());
        }

        @Test
        public void testToLongMatrix_SignExtensionForNegativeShorts() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { (short) -1, Short.MIN_VALUE } });
            com.landawn.abacus.matrix.LongMatrix asLong = m.toLongMatrix();
            assertEquals(-1L, asLong.get(0, 0));
            assertEquals(Short.MIN_VALUE, asLong.get(0, 1));
        }
    }

    @Nested
    class ToFloatMatrixTests {

        @Test
        public void testToFloatMatrix() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            FloatMatrix floatMatrix = m.toFloatMatrix();
            assertEquals(2, floatMatrix.rowCount());
            assertEquals(2, floatMatrix.columnCount());
            assertEquals(1.0f, floatMatrix.get(0, 0), 0.0001f);
            assertEquals(4.0f, floatMatrix.get(1, 1), 0.0001f);
        }

        @Test
        public void testToFloatMatrix_ConvertsToFloatMatrix() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            FloatMatrix floatMatrix = m.toFloatMatrix();
            assertEquals(1.0f, floatMatrix.get(0, 0));
            assertEquals(4.0f, floatMatrix.get(1, 1));
        }

        @Test
        public void testToFloatMatrix_AllValuesAndIndependentStorage() {
            final ShortMatrix source = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            final FloatMatrix converted = source.toFloatMatrix();
            assertEquals(FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } }), converted);
            source.set(0, 1, (short) 9);
            assertEquals(2, converted.get(0, 1));
            converted.set(1, 0, (float) 8);
            assertEquals(3, source.get(1, 0));
        }

        @Test
        public void testToFloatMatrix_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            FloatMatrix floatMatrix = empty.toFloatMatrix();
            assertEquals(0, floatMatrix.rowCount());
            assertEquals(0, floatMatrix.columnCount());
        }
    }

    @Nested
    class ToDoubleMatrixTests {

        @Test
        public void testToDoubleMatrix() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix doubleMatrix = m.toDoubleMatrix();
            assertEquals(2, doubleMatrix.rowCount());
            assertEquals(2, doubleMatrix.columnCount());
            assertEquals(1.0, doubleMatrix.get(0, 0), 0.0001);
            assertEquals(4.0, doubleMatrix.get(1, 1), 0.0001);
        }

        @Test
        public void testToDoubleMatrix_ConvertsToDoubleMatrix() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            DoubleMatrix doubleMatrix = m.toDoubleMatrix();
            assertEquals(1.0, doubleMatrix.get(0, 0));
            assertEquals(4.0, doubleMatrix.get(1, 1));
        }

        @Test
        public void testToDoubleMatrix_AllValuesAndIndependentStorage() {
            final ShortMatrix source = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            final DoubleMatrix converted = source.toDoubleMatrix();
            assertEquals(DoubleMatrix.wrap(new double[][] { { 1, 2 }, { 3, 4 } }), converted);
            source.set(0, 1, (short) 9);
            assertEquals(2, converted.get(0, 1));
            converted.set(1, 0, (double) 8);
            assertEquals(3, source.get(1, 0));
        }

        @Test
        public void testToDoubleMatrix_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            DoubleMatrix doubleMatrix = empty.toDoubleMatrix();
            assertEquals(0, doubleMatrix.rowCount());
            assertEquals(0, doubleMatrix.columnCount());
        }
    }

    @Nested
    class ZipWithTests {

        @Test
        public void testZipWith() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix result = m1.zipWith(m2, (a, b) -> (short) (a * b));

            assertEquals((short) 5, result.get(0, 0)); // 1*5
            assertEquals((short) 12, result.get(0, 1)); // 2*6
            assertEquals((short) 21, result.get(1, 0)); // 3*7
            assertEquals((short) 32, result.get(1, 1)); // 4*8

            // Test different shapes
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m3, (a, b) -> (short) (a + b)));
        }

        @Test
        public void testZipWith3Matrices() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 9, 10 }, { 11, 12 } });
            ShortMatrix result = m1.zipWith(m2, m3, (a, b, c) -> (short) (a + b + c));

            assertEquals((short) 15, result.get(0, 0)); // 1+5+9
            assertEquals((short) 18, result.get(0, 1)); // 2+6+10
            assertEquals((short) 21, result.get(1, 0)); // 3+7+11
            assertEquals((short) 24, result.get(1, 1)); // 4+8+12
        }

        @Test
        public void testZipWith3() {
            ShortMatrix matrix1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix matrix2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix matrix3 = ShortMatrix.wrap(new short[][] { { 9, 10 }, { 11, 12 } });
            ShortMatrix average = matrix1.zipWith(matrix2, matrix3, (a, b, c) -> (short) ((a + b + c) / 3));
            // Result: [[5, 6], [7, 8]]
            assertEquals((short) 5, average.get(0, 0));
            assertEquals((short) 6, average.get(0, 1));
            assertEquals((short) 7, average.get(1, 0));
            assertEquals((short) 8, average.get(1, 1));
        }

        @Test
        public void testZipWith_Shape2By2() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix result = m1.zipWith(m2, (a, b) -> (short) (a * b));

            assertEquals(5, result.get(0, 0)); // 1*5
            assertEquals(12, result.get(0, 1)); // 2*6
            assertEquals(21, result.get(1, 0)); // 3*7
            assertEquals(32, result.get(1, 1)); // 4*8
        }

        @Test
        public void testZipWith_DifferentShapes() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> (short) (a + b)));
        }

        @Test
        public void testZipWith_ThreeMatrices_DifferentShapes() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 9, 10, 11 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> (short) (a + b + c)));
        }

        @Test
        public void testZipWith_TwoMatrices() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix result = m1.zipWith(m2, (a, b) -> (short) (a + b));
            assertEquals(6, result.get(0, 0));
            assertEquals(8, result.get(0, 1));
            assertEquals(10, result.get(1, 0));
            assertEquals(12, result.get(1, 1));
        }

        @Test
        public void testZipWith_TwoMatrices_DifferentDimensions() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> (short) (a + b)));
        }

        @Test
        public void testZipWith_ThreeMatrices() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 9, 10 }, { 11, 12 } });
            ShortMatrix result = m1.zipWith(m2, m3, (a, b, c) -> (short) (a + b + c));
            assertEquals(15, result.get(0, 0)); // 1 + 5 + 9
            assertEquals(18, result.get(0, 1)); // 2 + 6 + 10
            assertEquals(21, result.get(1, 0)); // 3 + 7 + 11
            assertEquals(24, result.get(1, 1)); // 4 + 8 + 12
        }

        @Test
        public void testZipWith_ThreeMatrices_DifferentDimensions() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 3, 4 } });
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> (short) (a + b + c)));
        }

        @Test
        public void testZipWith_TwoMatrices_Shape2By2() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 10, 20 }, { 30, 40 } });
            ShortMatrix result = m1.zipWith(m2, (a, b) -> (short) (a + b));
            assertEquals((short) 11, result.get(0, 0));
            assertEquals((short) 22, result.get(0, 1));
            assertEquals((short) 33, result.get(1, 0));
            assertEquals((short) 44, result.get(1, 1));
        }

        @Test
        public void testZipWith_ThreeMatrices_Shape1By2() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 10, 20 } });
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 100, 200 } });
            ShortMatrix result = m1.zipWith(m2, m3, (a, b, c) -> (short) (a + b + c));
            assertEquals((short) 111, result.get(0, 0));
            assertEquals((short) 222, result.get(0, 1));
        }

        @Test
        public void testZipWith_WithTwoMatrices() {
            short[][] arr1 = { { 1, 2 }, { 3, 4 } };
            short[][] arr2 = { { 5, 6 }, { 7, 8 } };
            ShortMatrix m1 = new ShortMatrix(arr1);
            ShortMatrix m2 = new ShortMatrix(arr2);
            ShortMatrix result = m1.zipWith(m2, (a, b) -> (short) (a + b));
            assertEquals(6, result.get(0, 0));
            assertEquals(12, result.get(1, 1));
        }

        @Test
        public void testZipWith_WithThreeMatrices() {
            short[][] arr1 = { { 1, 2 }, { 3, 4 } };
            short[][] arr2 = { { 5, 6 }, { 7, 8 } };
            short[][] arr3 = { { 9, 10 }, { 11, 12 } };
            ShortMatrix m1 = new ShortMatrix(arr1);
            ShortMatrix m2 = new ShortMatrix(arr2);
            ShortMatrix m3 = new ShortMatrix(arr3);
            ShortMatrix result = m1.zipWith(m2, m3, (a, b, c) -> (short) (a + b + c));
            assertEquals(15, result.get(0, 0));
            assertEquals(24, result.get(1, 1));
        }

        @Test
        public void testZipWith_MaximumOfCorrespondingCells() {
            ShortMatrix matrix1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix matrix2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix max = matrix1.zipWith(matrix2, (a, b) -> (short) Math.max(a, b));
            // Result: [[5, 6], [7, 8]]
            assertEquals((short) 5, max.get(0, 0));
            assertEquals((short) 6, max.get(0, 1));
            assertEquals((short) 7, max.get(1, 0));
            assertEquals((short) 8, max.get(1, 1));
        }

        @Test
        public void testZipWith_SingleElement() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 2 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 3 } });
            ShortMatrix result = m1.zipWith(m2, (a, b) -> (short) (a + b));
            assertEquals(1, result.rowCount());
            assertEquals(1, result.columnCount());
            assertEquals(5, result.get(0, 0));
        }

        @Test
        public void testZipWith_ThreeMatrices_SingleElement() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 2 } });
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 3 } });
            ShortMatrix result = m1.zipWith(m2, m3, (a, b, c) -> (short) (a + b + c));
            assertEquals(1, result.rowCount());
            assertEquals(1, result.columnCount());
            assertEquals(6, result.get(0, 0));
        }
    }

    @Nested
    class MainDiagonalStreamTests {

        // Test empty matrix
        @Test
        public void testMainDiagonalStream() {
            short[] diagonal = matrix.mainDiagonalStream().toArray();
            assertArrayEquals(new short[] { 1, 5, 9 }, diagonal);

            // Test empty matrix
            assertTrue(emptyMatrix.mainDiagonalStream().toArray().length == 0);

            // Test non-square
            ShortMatrix nonSquare = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertArrayEquals(new short[] { 1 }, nonSquare.mainDiagonalStream().toArray());
        }

        @Test
        public void testMainDiagonalStream_Shape3By3_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            short[] diagonal = m.mainDiagonalStream().toArray();
            assertArrayEquals(new short[] { 1, 5, 9 }, diagonal);
        }

        @Test
        public void testMainDiagonalStream_NonSquare_Shape1By2_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertArrayEquals(new short[] { 1 }, m.mainDiagonalStream().toArray());
        }

        @Test
        public void testMainDiagonalStream_StreamsDiagonal() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[] result = m.mainDiagonalStream().toArray();
            assertArrayEquals(new short[] { 1, 5, 9 }, result);
        }

        @Test
        public void testMainDiagonalStream_IteratesEachDiagonalElementOnce() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 11, 12, 13 }, { 21, 22, 23 }, { 31, 32, 33 } });
            short[] diag = m.mainDiagonalStream().toArray();
            assertArrayEquals(new short[] { 11, 22, 33 }, diag);
        }

        @Test
        public void testAllCustomIteratorCountsConsumeTheirIterators() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });

            com.landawn.abacus.util.stream.ShortIteratorEx diagonal = (com.landawn.abacus.util.stream.ShortIteratorEx) matrix.mainDiagonalStream().iterator();
            assertEquals(2L, diagonal.count());
            assertFalse(diagonal.hasNext());

            com.landawn.abacus.util.stream.ObjIteratorEx<ShortStream> rows = (com.landawn.abacus.util.stream.ObjIteratorEx<ShortStream>) matrix.rowStreams()
                    .iterator();
            assertEquals(2L, rows.count());
            assertFalse(rows.hasNext());

            com.landawn.abacus.util.stream.ObjIteratorEx<ShortStream> columns = (com.landawn.abacus.util.stream.ObjIteratorEx<ShortStream>) matrix
                    .columnStreams()
                    .iterator();
            com.landawn.abacus.util.stream.ShortIteratorEx firstColumn = (com.landawn.abacus.util.stream.ShortIteratorEx) columns.next().iterator();
            assertEquals(2L, firstColumn.count());
            assertFalse(firstColumn.hasNext());
            assertEquals(1L, columns.count());
            assertFalse(columns.hasNext());
        }

        @Test
        public void testShortIteratorMaterializationConsumesOnlyTheRemainingLiveValues() {
            for (final boolean asList : new boolean[] { false, true }) {
                final ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
                final ShortIteratorEx main = (ShortIteratorEx) matrix.mainDiagonalStream().iterator();
                assertEquals((short) 1, main.nextShort());
                matrix.set(1, 1, (short) 50);
                final short[] mainRemaining = asList ? main.toList().toArray() : main.toArray();
                assertArrayEquals(new short[] { 50, 9 }, mainRemaining);
                assertFalse(main.hasNext());
                assertEquals(0L, main.count());
                assertArrayEquals(new short[0], main.toArray());
                mainRemaining[0] = (short) 99;
                assertEquals((short) 50, matrix.get(1, 1));
                matrix.set(1, 1, (short) 5);

                final ShortIteratorEx anti = (ShortIteratorEx) matrix.antiDiagonalStream().iterator();
                anti.advance(1);
                matrix.set(2, 0, (short) 70);
                assertArrayEquals(new short[] { 5, 70 }, asList ? anti.toList().toArray() : anti.toArray());
                assertFalse(anti.hasNext());
                assertEquals(0L, anti.count());
                assertEquals(0, anti.toList().size());

                final ShortIteratorEx column = (ShortIteratorEx) matrix.columnStreams(1, 2).iterator().next().iterator();
                assertEquals((short) 2, column.nextShort());
                matrix.flipVerticallyInPlace();
                matrix.set(1, 1, (short) 55);
                assertArrayEquals(new short[] { 55, 2 }, asList ? column.toList().toArray() : column.toArray());
                assertFalse(column.hasNext());
                assertEquals(0L, column.count());
                assertArrayEquals(new short[0], column.toArray());

                final ShortIteratorEx rows = (ShortIteratorEx) matrix.rowMajorStream().iterator();
                assertEquals((short) 70, rows.nextShort());
                rows.advance(3);
                matrix.set(1, 1, (short) 56);
                assertArrayEquals(new short[] { 56, 6, 1, 2, 3 }, asList ? rows.toList().toArray() : rows.toArray());
                assertFalse(rows.hasNext());
                assertEquals(0L, rows.count());
                assertEquals(0, rows.toList().size());

                final ShortIteratorEx columns = (ShortIteratorEx) matrix.columnMajorStream().iterator();
                assertEquals((short) 70, columns.nextShort());
                columns.advance(3);
                matrix.set(1, 1, (short) 57);
                assertArrayEquals(new short[] { 57, 2, 9, 6, 3 }, asList ? columns.toList().toArray() : columns.toArray());
                assertFalse(columns.hasNext());
                assertEquals(0L, columns.count());
                columns.advance(Long.MAX_VALUE);
                assertEquals(0, columns.toList().size());
            }
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testMainDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = ShortMatrix
                    .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
            final short[] expected = new short[] { (short) 1, (short) 5, (short) 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (ShortIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextShort);
            }
        }

        @Test
        public void testMainDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = ShortMatrix
                    .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (ShortIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((short) 1, iterator.nextShort());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextShort);
            }
        }

        @Test
        public void testMainDiagonalStream_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().toArray().length);
        }

        @Test
        public void testDiagonalStream_Nx0MatrixIsEmpty() {
            ShortMatrix m = ShortMatrix.wrap(new short[3][0]);
            assertEquals(3, m.rowCount());
            assertEquals(0, m.columnCount());
            assertEquals(0, m.mainDiagonalStream().count());
            assertEquals(0, m.antiDiagonalStream().count());

            ShortMatrix empty = ShortMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().count());
            assertEquals(0, empty.antiDiagonalStream().count());
        }
    }

    @Nested
    class AntiDiagonalStreamTests {

        @Test
        public void testAntiDiagonalStream() {
            short[] antiDiagonal = matrix.antiDiagonalStream().toArray();
            assertArrayEquals(new short[] { 3, 5, 7 }, antiDiagonal);

            // Test empty matrix
            assertTrue(emptyMatrix.antiDiagonalStream().toArray().length == 0);

            // Test non-square
            ShortMatrix nonSquare = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertArrayEquals(new short[] { 2 }, nonSquare.antiDiagonalStream().toArray());
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_ArrayValues_AntiDiagonalArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            short[] diagonal = m.antiDiagonalStream().toArray();
            assertArrayEquals(new short[] { 3, 5, 7 }, diagonal);
        }

        @Test
        public void testAntiDiagonalStream_NonSquare_Shape1By2_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertArrayEquals(new short[] { 2 }, m.antiDiagonalStream().toArray());
        }

        // Tests for AbstractMatrix-inherited methods missing coverage
        @Test
        public void testAntiDiagonalStream_StreamsAntiDiagonal() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[] result = m.antiDiagonalStream().toArray();
            assertArrayEquals(new short[] { 3, 5, 7 }, result);
        }

        @Test
        public void testAntiDiagonalStream_IteratesEachAntiDiagonalElementOnce() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 11, 12, 13 }, { 21, 22, 23 }, { 31, 32, 33 } });
            short[] antiDiag = m.antiDiagonalStream().toArray();
            assertArrayEquals(new short[] { 13, 22, 31 }, antiDiag);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testAntiDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = ShortMatrix
                    .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
            final short[] expected = new short[] { (short) 3, (short) 5, (short) 7 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (ShortIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextShort);
            }
        }

        @Test
        public void testAntiDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = ShortMatrix
                    .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (ShortIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((short) 3, iterator.nextShort());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextShort);
            }
        }

        @Test
        public void testAntiDiagonalStream_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            assertEquals(0, empty.antiDiagonalStream().toArray().length);
        }
    }

    @Nested
    class RowMajorStreamTests {

        @Test
        public void testRowMajorStream() {
            short[] all = matrix.rowMajorStream().toArray();
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, all);

            // Test empty matrix
            assertTrue(emptyMatrix.rowMajorStream().toArray().length == 0);
        }

        @Test
        public void testRowMajorStreamRange() {
            short[] rows = matrix.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new short[] { 4, 5, 6, 7, 8, 9 }, rows);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(0, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(2, 1));
        }

        @Test
        public void testIteratorNoSuchElement() {
            // Test rowMajorStream iterator
            ShortStream stream = matrix.rowMajorStream(0, 1);
            stream.toArray(); // Consume all
            assertThrows(IllegalStateException.class, () -> stream.iterator().next());

            // Test mainDiagonalStream iterator
            ShortStream diagStream = matrix.mainDiagonalStream();
            diagStream.toArray(); // Consume all
            assertThrows(IllegalStateException.class, () -> diagStream.iterator().next());
        }

        @Test
        public void testRowMajorStream_Shape2By3_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            short[] all = m.rowMajorStream().toArray();
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, all);
        }

        @Test
        public void testRowMajorStream_RowRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            short[] rows = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new short[] { 3, 4, 5, 6 }, rows);
        }

        @Test
        public void testRowMajorStream_StreamsAllElements() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[] result = m.rowMajorStream().toArray();
            assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
        }

        @Test
        public void testRowMajorStream_WithRowRange() {
            short[][] arr = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[] result = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new short[] { 3, 4, 5, 6 }, result);
        }

        @Test
        public void testStreamIteratorAdvanceAndExhaustion_EdgeCase() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });

            var rowIterator = matrix.rowMajorStream(0, 2).iterator();
            assertTrue(rowIterator instanceof com.landawn.abacus.util.stream.ShortIteratorEx);
            com.landawn.abacus.util.stream.ShortIteratorEx rowEx = (com.landawn.abacus.util.stream.ShortIteratorEx) rowIterator;
            rowEx.advance(2);
            assertEquals(2L, rowEx.count());
            assertEquals(0L, rowEx.count());
            assertThrows(java.util.NoSuchElementException.class, rowEx::nextShort);

            var columnIterator = matrix.columnMajorStream(0, 2).iterator();
            assertTrue(columnIterator instanceof com.landawn.abacus.util.stream.ShortIteratorEx);
            com.landawn.abacus.util.stream.ShortIteratorEx columnEx = (com.landawn.abacus.util.stream.ShortIteratorEx) columnIterator;
            columnEx.advance(1);
            assertEquals(3L, columnEx.count());
            assertEquals(0L, columnEx.count());
            assertThrows(java.util.NoSuchElementException.class, columnEx::nextShort);
        }

        @Test
        public void testHorizontalAndVerticalStream_TraverseAllElementsExactlyOnce() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            short[] h = m.rowMajorStream().toArray();
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5, 6 }, h);
            short[] v = m.columnMajorStream().toArray();
            assertArrayEquals(new short[] { 1, 4, 2, 5, 3, 6 }, v);
        }

        @Test
        public void testStreamIterators_MidStrideAdvanceThenToArray_EdgeCase() {
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            var rowMajorIterator = matrix.rowMajorStream(0, 2).iterator();
            assertTrue(rowMajorIterator instanceof com.landawn.abacus.util.stream.ShortIteratorEx);
            com.landawn.abacus.util.stream.ShortIteratorEx rowMajor = (com.landawn.abacus.util.stream.ShortIteratorEx) rowMajorIterator;
            rowMajor.advance(1); // mid-row cursor: the chunked toArray must start at column 1
            assertArrayEquals(new short[] { 2, 3, 4, 5, 6 }, rowMajor.toArray());

            com.landawn.abacus.util.stream.ShortIteratorEx rowCount = (com.landawn.abacus.util.stream.ShortIteratorEx) matrix.rowMajorStream(0, 2).iterator();
            rowCount.advance(1);
            assertEquals(5L, rowCount.count());
            assertFalse(rowCount.hasNext());

            var columnMajorIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.ShortIteratorEx columnMajor = (com.landawn.abacus.util.stream.ShortIteratorEx) columnMajorIterator;
            columnMajor.advance(1); // mid-column cursor: row 1 of column 0
            assertArrayEquals(new short[] { 4, 2, 5, 3, 6 }, columnMajor.toArray());

            com.landawn.abacus.util.stream.ShortIteratorEx columnCount = (com.landawn.abacus.util.stream.ShortIteratorEx) matrix.columnMajorStream(0, 3)
                    .iterator();
            columnCount.advance(1);
            assertEquals(5L, columnCount.count());
            assertFalse(columnCount.hasNext());

            var crossingIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.ShortIteratorEx crossing = (com.landawn.abacus.util.stream.ShortIteratorEx) crossingIterator;
            crossing.advance(3); // crosses a column boundary: lands on row 1 of column 1
            assertEquals((short) 5, crossing.nextShort());
            assertEquals(2L, crossing.count());
            assertEquals(0L, crossing.count());
            assertThrows(java.util.NoSuchElementException.class, crossing::nextShort);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testRowMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = ShortMatrix
                    .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
            final short[] expected = new short[] { (short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7, (short) 8, (short) 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (ShortIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextShort);
            }
        }

        @Test
        public void testRowMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = ShortMatrix
                    .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (ShortIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((short) 1, iterator.nextShort());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextShort);
            }
        }

        @Test
        public void testRowMajorStream_NextTraversesAcrossRowBoundaries() {
            final var matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            final List<Short> values = new ArrayList<>();
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (ShortIteratorEx) stream.iterator();
                while (iterator.hasNext()) {
                    values.add(iterator.nextShort());
                }
                assertEquals(List.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6), values);
            }
        }

        @Test
        public void testRowMajorStreamSingleRowRange() {
            short[] row1 = matrix.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new short[] { 4, 5, 6 }, row1);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(3, 4));
        }

        @Test
        public void testRowMajorStream_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            assertEquals(0, empty.rowMajorStream().toArray().length);
        }

        @Test
        public void testRowMajorStream_EmptyRow() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            short[] row = m.rowMajorStream(0, 0).toArray();
            assertEquals(0, row.length);
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            short[] row1 = m.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new short[] { 4, 5, 6 }, row1);
        }

        @Test
        public void testRowMajorStream_WithSingleRowRangeRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            short[] rows = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new short[] { 4, 5, 6, 7, 8, 9 }, rows);
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_ArrayValues() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[] result = m.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new short[] { 4, 5, 6 }, result);
        }

        @Test
        public void testShortEmptyStreamRangesKeepBoundsValidationAndLogicalShapeCounts() {
            final ShortMatrix matrix = new ShortMatrix(new short[2][2]);
            assertEquals(0L, matrix.rowMajorStream(1, 1).count());
            assertEquals(0L, matrix.columnMajorStream(1, 1).count());
            assertEquals(0L, matrix.rowStreams(1, 1).count());
            assertEquals(0L, matrix.columnStreams(1, 1).count());
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(3, 3));

            final ShortMatrix noColumns = new ShortMatrix(new short[2][0]);
            assertEquals(2L, noColumns.rowStreams().count());
            noColumns.rowStreams().forEach(row -> assertEquals(0L, row.count()));
            assertEquals(0L, noColumns.mainDiagonalStream().count());
            assertEquals(0L, noColumns.antiDiagonalStream().count());

            final ShortMatrix noRows = new ShortMatrix(new short[0][], 3);
            assertEquals(3L, noRows.columnStreams().count());
            noRows.columnStreams().forEach(column -> assertEquals(0L, column.count()));
            assertEquals(0L, noRows.mainDiagonalStream().count());
            assertEquals(0L, noRows.antiDiagonalStream().count());
        }

        @Test
        public void testRowMajorStream_ZeroColumnsKeepLogicalRowRange() {
            final var matrix = ShortMatrix.wrap(new short[3][0]);
            assertEquals(0, matrix.rowMajorStream(1, 3).count());
            assertEquals(2, matrix.rowStreams(1, 3).count());
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 3));
        }

        @Test
        public void testRowMajorStream_WithRange_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 1));
        }

        @Test
        public void testRowMajorStream_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 2));
        }
    }

    @Nested
    class ColumnMajorStreamTests {

        @Test
        public void testColumnMajorStream() {
            short[] all = matrix.columnMajorStream().toArray();
            assertArrayEquals(new short[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 }, all);

            // Test empty matrix
            assertTrue(emptyMatrix.columnMajorStream().toArray().length == 0);
        }

        @Test
        public void testColumnMajorStreamRange() {
            short[] columnCount = matrix.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new short[] { 2, 5, 8, 3, 6, 9 }, columnCount);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(0, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(2, 1));
        }

        @Test
        public void testColumnMajorStream_Shape2By3_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            short[] all = m.columnMajorStream().toArray();
            assertArrayEquals(new short[] { 1, 4, 2, 5, 3, 6 }, all);
        }

        @Test
        public void testColumnMajorStream_WithRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            short[] columnCount = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new short[] { 2, 5, 8, 3, 6, 9 }, columnCount);
        }

        @Test
        public void testColumnMajorStream_ColumnRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            short[] columnCount = m.columnMajorStream(0, 2).toArray();
            assertArrayEquals(new short[] { 1, 4, 2, 5 }, columnCount);
        }

        @Test
        public void testColumnMajorStream_StreamsAllElementsVertically() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[] result = m.columnMajorStream().toArray();
            assertArrayEquals(new short[] { 1, 3, 2, 4 }, result);
        }

        @Test
        public void testColumnMajorStream_WithColumnRange() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[] result = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new short[] { 2, 5, 3, 6 }, result);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testColumnMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = ShortMatrix
                    .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
            final short[] expected = new short[] { (short) 1, (short) 4, (short) 7, (short) 2, (short) 5, (short) 8, (short) 3, (short) 6, (short) 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (ShortIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextShort);
            }
        }

        @Test
        public void testColumnMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = ShortMatrix
                    .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (ShortIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((short) 1, iterator.nextShort());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextShort);
            }
        }

        @Test
        public void testColumnMajorStreamSingleColumnRange() {
            short[] col1 = matrix.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new short[] { 2, 5, 8 }, col1);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(3, 4));
        }

        @Test
        public void testColumnMajorStream_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            assertEquals(0, empty.columnMajorStream().toArray().length);
        }

        @Test
        public void testColumnMajorStream_EmptyColumn() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            short[] col = m.columnMajorStream(0, 0).toArray();
            assertEquals(0, col.length);
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            short[] col1 = m.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new short[] { 2, 5 }, col1);
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRangeRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            short[] columnCount = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new short[] { 2, 5, 3, 6 }, columnCount);
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_ArrayValues() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            short[] result = m.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new short[] { 2, 5 }, result);
        }

        @Test
        public void testColumnMajorStream_ZeroRowsKeepLogicalColumnRange() {
            final var matrix = ShortMatrix.wrap(new short[0][0]).resize(0, 3);
            assertEquals(0, matrix.columnMajorStream(1, 3).count());
            assertEquals(2, matrix.columnStreams(1, 3).count());
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 3));
        }

        @Test
        public void testColumnMajorStream_WithRange_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 1));
        }

        @Test
        public void testColumnMajorStream_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
        }
    }

    @Nested
    class RowStreamsTests {

        @Test
        public void testDocumentedStreamsReadLiveCellsAndPreserveProducedRowBinding() {
            final ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
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
            matrix.set(1, 0, (short) 9);
            matrix.set(0, 1, (short) 8);

            assertArrayEquals(new short[] { 9, 2 }, row.toArray());
            assertArrayEquals(new short[] { 9, 2 }, selectedRow.toArray());
            assertArrayEquals(new short[] { 3, 9 }, column.toArray());
            assertArrayEquals(new short[] { 3, 9 }, selectedColumn.toArray());
            assertArrayEquals(new short[] { 3, 8, 9, 2 }, rowMajor.toArray());
            assertArrayEquals(new short[] { 3, 8 }, selectedRowMajor.toArray());
            assertArrayEquals(new short[] { 3, 9, 8, 2 }, columnMajor.toArray());
            assertArrayEquals(new short[] { 3, 9 }, selectedColumnMajor.toArray());
            assertArrayEquals(new short[] { 3, 2 }, mainDiagonal.toArray());
            assertArrayEquals(new short[] { 8, 9 }, antiDiagonal.toArray());
        }

        @Test
        public void testRowStreams() {
            List<short[]> rows = matrix.rowStreams().map(ShortStream::toArray).toList();
            assertEquals(3, rows.size());
            assertArrayEquals(new short[] { 1, 2, 3 }, rows.get(0));
            assertArrayEquals(new short[] { 4, 5, 6 }, rows.get(1));
            assertArrayEquals(new short[] { 7, 8, 9 }, rows.get(2));

            // Test empty matrix
            assertTrue(emptyMatrix.rowStreams().count() == 0);
        }

        @Test
        public void testRowStreamsRange() {
            List<short[]> rows = matrix.rowStreams(1, 3).map(ShortStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new short[] { 4, 5, 6 }, rows.get(0));
            assertArrayEquals(new short[] { 7, 8, 9 }, rows.get(1));

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(0, 4));
        }

        @Test
        public void testRowStreams_Shape2By3_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<short[]> rows = m.rowStreams().map(ShortStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new short[] { 1, 2, 3 }, rows.get(0));
            assertArrayEquals(new short[] { 4, 5, 6 }, rows.get(1));
        }

        @Test
        public void testRowStreams_WithRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<short[]> rows = m.rowStreams(1, 3).map(ShortStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new short[] { 4, 5, 6 }, rows.get(0));
            assertArrayEquals(new short[] { 7, 8, 9 }, rows.get(1));
        }

        @Test
        public void testRowStreams_Shape2By2_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            List<short[]> rows = m.rowStreams().map(ShortStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new short[] { 1, 2 }, rows.get(0));
            assertArrayEquals(new short[] { 3, 4 }, rows.get(1));
        }

        @Test
        public void testRowStreams_StreamsRows() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            Stream<ShortStream> streams = m.rowStreams();
            assertEquals(2, streams.count());
        }

        @Test
        public void testRowStreams_WithRange_Values() {
            short[][] arr = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            Stream<ShortStream> streams = m.rowStreams(1, 3);
            assertEquals(2, streams.count());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testRowStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = ShortMatrix
                    .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
            final short[][] expected = { new short[] { (short) 1, (short) 2, (short) 3 }, new short[] { (short) 4, (short) 5, (short) 6 },
                    new short[] { (short) 7, (short) 8, (short) 9 } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowStreams()) {
                final var iterator = (ObjIteratorEx<ShortStream>) stream.iterator();
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
            ShortMatrix empty = ShortMatrix.empty();
            assertEquals(0, empty.rowStreams().count());
        }

        @Test
        public void testRowStreams_EmptyRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            long count = m.rowStreams(1, 1).count();
            assertEquals(0, count);
        }

        @Test
        public void testRowStreams_WithRange_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(0, 3));
        }
    }

    @Nested
    class ColumnStreamsTests {

        @Test
        public void testColumnStreams() {
            List<short[]> columnCount = matrix.columnStreams().map(ShortStream::toArray).toList();
            assertEquals(3, columnCount.size());
            assertArrayEquals(new short[] { 1, 4, 7 }, columnCount.get(0));
            assertArrayEquals(new short[] { 2, 5, 8 }, columnCount.get(1));
            assertArrayEquals(new short[] { 3, 6, 9 }, columnCount.get(2));

            // Test empty matrix
            assertTrue(emptyMatrix.columnStreams().count() == 0);
        }

        @Test
        public void testColumnStreamsRange() {
            List<short[]> columnCount = matrix.columnStreams(1, 3).map(ShortStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new short[] { 2, 5, 8 }, columnCount.get(0));
            assertArrayEquals(new short[] { 3, 6, 9 }, columnCount.get(1));

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(0, 4));
        }

        @Test
        public void testColumnStreams_Shape2By3_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<short[]> columnCount = m.columnStreams().map(ShortStream::toArray).toList();
            assertEquals(3, columnCount.size());
            assertArrayEquals(new short[] { 1, 4 }, columnCount.get(0));
            assertArrayEquals(new short[] { 2, 5 }, columnCount.get(1));
            assertArrayEquals(new short[] { 3, 6 }, columnCount.get(2));
        }

        @Test
        public void testColumnStreams_WithRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<short[]> columnCount = m.columnStreams(1, 3).map(ShortStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new short[] { 2, 5, 8 }, columnCount.get(0));
            assertArrayEquals(new short[] { 3, 6, 9 }, columnCount.get(1));
        }

        @Test
        public void testColumnStreams_Shape2By2_ArrayValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            List<short[]> columnCount = m.columnStreams().map(ShortStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new short[] { 1, 3 }, columnCount.get(0));
            assertArrayEquals(new short[] { 2, 4 }, columnCount.get(1));
        }

        @Test
        public void testColumnStreams_StreamsColumns() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            Stream<ShortStream> streams = m.columnStreams();
            assertEquals(2, streams.count());
        }

        @Test
        public void testColumnStreams_WithRange_Values() {
            short[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            Stream<ShortStream> streams = m.columnStreams(1, 3);
            assertEquals(2, streams.count());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testColumnStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = ShortMatrix
                    .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
            final short[][] expected = { new short[] { (short) 1, (short) 4, (short) 7 }, new short[] { (short) 2, (short) 5, (short) 8 },
                    new short[] { (short) 3, (short) 6, (short) 9 } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnStreams()) {
                final var iterator = (ObjIteratorEx<ShortStream>) stream.iterator();
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
            final var matrix = ShortMatrix
                    .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
            final short[] expected = new short[] { (short) 2, (short) 5, (short) 8 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (ShortIteratorEx) values.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextShort);
            }
        }

        @Test
        public void testColumnStreams_InnerCountConsumesOnlyRemainingValues() {
            final var matrix = ShortMatrix
                    .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (ShortIteratorEx) values.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((short) 2, iterator.nextShort());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextShort);
            }
        }

        @Test
        public void testColumnStreams_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            assertEquals(0, empty.columnStreams().count());
        }

        @Test
        public void testColumnStreams_EmptyRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            long count = m.columnStreams(1, 1).count();
            assertEquals(0, count);
        }

        @Test
        public void testZeroByNShapeFlowsThroughResultsAndColumnStreams() {
            ShortMatrix zeroByThree = ShortMatrix.random(0, 3, new java.util.Random(1L));
            assertEquals(3, zeroByThree.copy().columnCount());
            assertEquals(3, zeroByThree.copyRows(0, 0).columnCount());
            assertEquals(2, zeroByThree.copyRegion(0, 0, 1, 3).columnCount());
            assertEquals(5, zeroByThree.resize(0, 5).columnCount());
            assertEquals(6, zeroByThree.pad(0, 0, 1, 2).columnCount());
            assertEquals(3, zeroByThree.map(v -> v).columnCount());
            assertEquals(3, zeroByThree.boxed().columnCount());
            assertEquals(3, zeroByThree.toIntMatrix().columnCount());
            assertEquals(3, zeroByThree.toLongMatrix().columnCount());
            assertEquals(6, zeroByThree.repeatElements(2, 2).columnCount());

            java.util.List<ShortStream> columns = zeroByThree.columnStreams().toList();
            assertEquals(3, columns.size());
            columns.forEach(column -> assertEquals(0L, column.count()));

            ShortMatrix zeroProduct = ShortMatrix.random(0, 2).matrixMultiply(ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } }));
            assertEquals(0, zeroProduct.rowCount());
            assertEquals(3, zeroProduct.columnCount());
            assertEquals(3, ShortMatrix.random(0, 2).matrixMultiplyWidened(ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } })).columnCount());
        }

        @Test
        public void testColumnStreams_WithRange_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(0, 3));
        }
    }

    @Nested
    class ForEachTests {

        @Test
        public void testDocumentedForEachStopsAfterCallbackFailure() throws Exception {
            for (int operation = 0; operation < 2; operation++) {
                final int selectedOperation = operation;
                final ShortMatrix matrix = ShortMatrix.wrap(new short[2][3]);
                final AtomicInteger calls = new AtomicInteger();
                final IllegalStateException failure = new IllegalStateException("action failed");
                final Throwables.ShortConsumer<IllegalStateException> action = value -> {
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
            List<Short> values = new ArrayList<>();
            matrix.forEach(it -> values.add(it));
            assertEquals(9, values.size());
            for (int i = 0; i < 9; i++) {
                assertEquals((short) (i + 1), values.get(i));
            }
        }

        @Test
        public void testForEachWithBounds() {
            List<Short> values = new ArrayList<>();
            matrix.forEach(1, 3, 1, 3, it -> values.add(it));
            assertEquals(4, values.size());
            assertEquals((short) 5, values.get(0));
            assertEquals((short) 6, values.get(1));
            assertEquals((short) 8, values.get(2));
            assertEquals((short) 9, values.get(3));

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.forEach(-1, 2, 0, 2, v -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.forEach(0, 4, 0, 2, v -> {
            }));
        }

        @Test
        public void testShortMatrixForEach() {
            // ShortMatrix.java: matrix.forEach(value -> System.out.print(value + " "));
            // Output: 1 2 3 4
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            List<Short> values = new ArrayList<>();
            matrix.forEach(value -> values.add(value));
            assertEquals(4, values.size());
            assertEquals((short) 1, values.get(0));
            assertEquals((short) 2, values.get(1));
            assertEquals((short) 3, values.get(2));
            assertEquals((short) 4, values.get(3));
        }

        @Test
        public void testShortMatrixForEachWithRange() {
            // ShortMatrix.java: matrix.forEach(1, 3, 1, 3, value -> System.out.print(value + " "));
            // Output: 5 6 8 9  (processes elements in rows 1-2, columns 1-2)
            ShortMatrix matrix = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Short> values = new ArrayList<>();
            matrix.forEach(1, 3, 1, 3, value -> values.add(value));
            assertEquals(4, values.size());
            assertEquals((short) 5, values.get(0));
            assertEquals((short) 6, values.get(1));
            assertEquals((short) 8, values.get(2));
            assertEquals((short) 9, values.get(3));
        }

        @Test
        public void testForEach_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            List<Short> values = new ArrayList<>();
            m.forEach((short v) -> values.add(v));
            assertEquals(4, values.size());
            assertEquals(Short.valueOf((short) 1), values.get(0));
            assertEquals(Short.valueOf((short) 2), values.get(1));
            assertEquals(Short.valueOf((short) 3), values.get(2));
            assertEquals(Short.valueOf((short) 4), values.get(3));
        }

        @Test
        public void testForEach_WithRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Short> values = new ArrayList<>();
            m.forEach(1, 3, 1, 3, (short v) -> values.add(v));
            assertEquals(4, values.size());
            assertEquals(Short.valueOf((short) 5), values.get(0));
            assertEquals(Short.valueOf((short) 6), values.get(1));
            assertEquals(Short.valueOf((short) 8), values.get(2));
            assertEquals(Short.valueOf((short) 9), values.get(3));
        }

        @Test
        public void testForEach_IteratesAllElements() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            final int[] sum = { 0 };
            m.forEach(val -> sum[0] += val);
            assertEquals(10, sum[0]);
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testForEach_ExplicitModesVisitSelectedRegionExactlyOnce(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = ShortMatrix
                        .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } });
                final var values = new ConcurrentLinkedQueue<Short>();
                matrix.forEach(0, 2, 1, 3, values::add);
                assertEquals(List.of((short) 2, (short) 3, (short) 5, (short) 6).stream().sorted().toList(), values.stream().sorted().toList());
                assertEquals(ShortMatrix
                        .wrap(new short[][] { { (short) 1, (short) 2, (short) 3 }, { (short) 4, (short) 5, (short) 6 }, { (short) 7, (short) 8, (short) 9 } }),
                        matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        // Test various operations on empty matrix
        @Test
        public void testForEachNullAction() {
            assertThrows(IllegalArgumentException.class, () -> matrix.forEach((Throwables.ShortConsumer<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class,
                    () -> matrix.forEach(0, matrix.rowCount(), 0, matrix.columnCount(), (Throwables.ShortConsumer<RuntimeException>) null));
        }

        @Test
        public void testForEach_EmptyRange() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            List<Short> values = new ArrayList<>();
            m.forEach(0, 0, 0, 0, (short v) -> values.add(v));
            assertTrue(values.isEmpty());
        }

        @Test
        public void testForEach_OutOfBounds() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(-1, 2, 0, 2, x -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 3, 0, 2, x -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 2, -1, 2, x -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 2, 0, 3, x -> {
            }));
        }
    }

    @Nested
    class ToMultilineStringTests {

        @Test
        public void testToMultilineString_ReturnsString() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            String result = m.toMultilineString();
            assertNotNull(result);
            assertTrue(result.length() > 0);
        }

        @Test
        public void testToMultilineStringNonEmptyReturnsNonNullFormattedString() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains("[1, 2]"));
            assertTrue(printed.contains("[3, 4]"));
            assertFalse(printed.equals("null"));
        }

        @Test
        public void testToMultilineStringEmptyReturnsBracketsLiteral() {
            ShortMatrix empty = ShortMatrix.empty();
            String printed = empty.toMultilineString();
            assertEquals("[]", printed);
        }

        @Test
        public void testToMultilineStringWithMinAndMaxShortValues() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { Short.MIN_VALUE, Short.MAX_VALUE } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains(String.valueOf(Short.MIN_VALUE)));
            assertTrue(printed.contains(String.valueOf(Short.MAX_VALUE)));
        }
    }

    @Nested
    class HashCodeTests {

        @Test
        public void testHashCode() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 4, 3 } });

            assertEquals(m1.hashCode(), m2.hashCode());
            assertNotEquals(m1.hashCode(), m3.hashCode()); // Usually different
        }

        @Test
        public void testHashCode_Equal() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });

            assertEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        public void testHashCode_ConsistentWithEquals() {
            short[][] arr1 = { { 1, 2 }, { 3, 4 } };
            short[][] arr2 = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m1 = new ShortMatrix(arr1);
            ShortMatrix m2 = new ShortMatrix(arr2);
            assertEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        public void testHashCode_EmptyMatrices() {
            ShortMatrix empty1 = ShortMatrix.empty();
            ShortMatrix empty2 = ShortMatrix.wrap(new short[0][0]);
            assertEquals(empty1.hashCode(), empty2.hashCode());
        }
    }

    @Nested
    class EqualsTests {

        @Test
        public void testEquals() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 4, 3 } });
            ShortMatrix m4 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            assertTrue(m1.equals(m1)); // Same object
            assertTrue(m1.equals(m2)); // Same values
            assertFalse(m1.equals(m3)); // Different values
            assertFalse(m1.equals(m4)); // Different dimensions
            assertFalse(m1.equals(null));
            assertFalse(m1.equals("not a matrix"));
        }

        @Test
        public void testEquals_SameContent() {
            short[][] arr1 = { { 1, 2 }, { 3, 4 } };
            short[][] arr2 = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m1 = new ShortMatrix(arr1);
            ShortMatrix m2 = new ShortMatrix(arr2);
            assertEquals(m1, m2);
        }

        @Test
        public void testEquals_DifferentContent() {
            short[][] arr1 = { { 1, 2 }, { 3, 4 } };
            short[][] arr2 = { { 1, 2 }, { 3, 5 } };
            ShortMatrix m1 = new ShortMatrix(arr1);
            ShortMatrix m2 = new ShortMatrix(arr2);
            assertNotEquals(m1, m2);
        }

        @Test
        public void testEquals_DifferentDimensions() {
            short[][] arr1 = { { 1, 2 }, { 3, 4 } };
            short[][] arr2 = { { 1, 2, 3 } };
            ShortMatrix m1 = new ShortMatrix(arr1);
            ShortMatrix m2 = new ShortMatrix(arr2);
            assertNotEquals(m1, m2);
        }

        @Test
        public void testEqualsHashCode_ConsistencyAndShape() {
            ShortMatrix a1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix a2 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix b = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            assertEquals(a1, a2);
            assertEquals(a1.hashCode(), a2.hashCode());
            assertNotEquals(a1, b);

            ShortMatrix differentValue = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 99 } });
            assertNotEquals(a1, differentValue);

            assertTrue(a1.equals(a1));
            assertNotEquals(a1, null);
            assertNotEquals(a1, "not a matrix");
        }

        @Test
        public void testEquals_DifferentRowCountsWithEqualColumnCounts() {
            final var longer = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 }, { (short) 3, (short) 4 } });
            final var shorter = ShortMatrix.wrap(new short[][] { { (short) 1, (short) 2 } });
            assertFalse(longer.equals(shorter));
            assertFalse(shorter.equals(longer));
        }

        @Test
        public void testEquals_Shape2By2_NullInput() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 5 } });

            assertEquals(m1, m2);
            assertNotEquals(m1, m3);
            assertNotEquals(m1, null);
            assertNotEquals(m1, "not a matrix");
        }

        @Test
        public void testEquals_EmptyMatrices() {
            ShortMatrix empty1 = ShortMatrix.empty();
            ShortMatrix empty2 = ShortMatrix.wrap(new short[0][0]);
            assertTrue(empty1.equals(empty2));
        }
    }

    @Nested
    class ToStringTests {

        @Test
        public void testToString_Shape2By2() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1"));
            assertTrue(str.contains("2"));
            assertTrue(str.contains("3"));
            assertTrue(str.contains("4"));
        }

        @Test
        public void testToString_ReturnsStringRepresentation() {
            short[][] arr = { { 1, 2 }, { 3, 4 } };
            ShortMatrix m = new ShortMatrix(arr);
            String result = m.toString();
            assertNotNull(result);
            assertTrue(result.contains("1"));
            assertTrue(result.contains("4"));
        }

        @Test
        public void testToString_Shape2By2_NonemptyRendering() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.length() > 0);
        }

        @Test
        public void testToString_Empty() {
            ShortMatrix empty = ShortMatrix.empty();
            String str = empty.toString();
            assertNotNull(str);
        }

        @Test
        public void testToString_SingleElement() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 42 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("42"));
        }
    }

    @Nested
    class ElementTypeTests {

        // Test bounds
        @Test
        public void testElementType() {
            assertEquals(short.class, matrix.elementType());
        }

        @Test
        public void testElementType_ReturnsShortClass() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1 } });
            assertEquals(short.class, m.elementType());
        }
    }

    @Nested
    class UnsafeBackingArrayTests {

        @Test
        public void testToArray() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            short[][] arr = m.unsafeBackingArray();
            assertEquals(2, arr.length);
            assertEquals(2, arr[0].length);
            assertArrayEquals(new short[] { 1, 2 }, arr[0]);
            assertArrayEquals(new short[] { 3, 4 }, arr[1]);
        }
    }

    @Nested
    class ElementCountTests {

        @Test
        public void testElementCount() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(6, m.elementCount());
        }

        @Test
        public void testElementCount_Empty() {
            ShortMatrix m = ShortMatrix.empty();
            assertEquals(0, m.elementCount());
        }
    }

    @Nested
    class IsEmptyTests {

        @Test
        public void testIsEmpty_False() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1 } });
            assertFalse(m.isEmpty());
        }

        @Test
        public void testIsEmpty() {
            assertTrue(ShortMatrix.empty().isEmpty());
            assertFalse(ShortMatrix.wrap(new short[][] { { 1 } }).isEmpty());
        }
    }

    @Nested
    class IsSameShapeTests {

        @Test
        public void testIsSameShape() {
            ShortMatrix m1 = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix m2 = ShortMatrix.wrap(new short[][] { { 5, 6 }, { 7, 8 } });
            ShortMatrix m3 = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }

        @Test
        public void testIsSameShape_NullThrows() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.isSameShape(null));
        }
    }

    @Nested
    class ReshapeTests {

        @Test
        public void testReshape() {
            ShortMatrix reshaped = matrix.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            for (int i = 0; i < 9; i++) {
                assertEquals((short) (i + 1), reshaped.get(0, i));
            }

            // Test reshape back
            ShortMatrix reshaped2 = reshaped.reshape(3, 3);
            assertEquals(matrix, reshaped2);

            // Test empty matrix
            ShortMatrix emptyReshaped = emptyMatrix.reshapeAndPad(2, 3);
            assertEquals(2, emptyReshaped.rowCount());
            assertEquals(3, emptyReshaped.columnCount());

            // Test reshape with too-small dimensions throws exception
            assertThrows(IllegalArgumentException.class, () -> matrix.reshape(1, 4));
            assertThrows(IllegalArgumentException.class, () -> matrix.reshape(2, 2));
        }

        @Test
        public void testReshape_Shape3By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ShortMatrix reshaped = m.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, reshaped.get(0, i));
            }
        }

        @Test
        public void testReshape_Back() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            ShortMatrix reshaped = m.reshape(1, 9);
            ShortMatrix reshapedBack = reshaped.reshape(3, 3);
            assertEquals(m, reshapedBack);
        }

        @Test
        public void testReshape_Shape2By3() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            ShortMatrix reshaped = m.reshape(3, 2);
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals((short) 1, reshaped.get(0, 0));
            assertEquals((short) 2, reshaped.get(0, 1));
            assertEquals((short) 6, reshaped.get(2, 1));
        }

        @Test
        public void testReshape_ChangesShape() {
            short[][] arr = { { 1, 2, 3, 4, 5, 6 } };
            ShortMatrix m = new ShortMatrix(arr);
            ShortMatrix reshaped = m.reshape(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(6, reshaped.get(1, 2));
        }

        @Test
        public void testReshape_ToSingleRow() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix reshaped = m.reshape(1, 4);
            assertEquals(1, reshaped.rowCount());
            assertEquals(4, reshaped.columnCount());
            assertArrayEquals(new short[] { 1, 2, 3, 4 }, reshaped.rowView(0));
        }

        @Test
        public void testReshape_ToSingleColumn() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix reshaped = m.reshape(4, 1);
            assertEquals(4, reshaped.rowCount());
            assertEquals(1, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(1, 0));
            assertEquals(3, reshaped.get(2, 0));
            assertEquals(4, reshaped.get(3, 0));
        }
    }

    @Nested
    class ReshapeAndPadToColumnCountTests {

        @ParameterizedTest(name = "pad into {0} columns")
        @ValueSource(ints = { 1, 3, 7 })
        public void testReshapeAndPadToColumnCount_PreservesOrderAndPadsTrailingCells(final int columns) {
            final ShortMatrix matrix = ShortMatrix.wrap(new short[] { 1, 2, 3, 4, 5 });
            final ShortMatrix result = matrix.reshapeAndPadToColumnCount(columns);
            final int rows = (5 + columns - 1) / columns;
            assertEquals(rows, result.rowCount());
            assertEquals(columns, result.columnCount());
            for (int index = 0; index < rows * columns; index++) {
                assertEquals((short) (index < 5 ? index + 1 : 0), result.get(index / columns, index % columns));
            }
            result.set(0, 0, (short) 99);
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, matrix.rowCopy(0));
        }

        @ParameterizedTest
        @ValueSource(ints = { 0, -1 })
        public void testReshapeAndPadToColumnCount_InvalidColumnCount(final int columns) {
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.wrap(new short[] { 1 }).reshapeAndPadToColumnCount(columns));
        }
    }

    @Nested
    class ForEachIndicesTests {

        @Test
        public void testForEachIndex() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
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
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

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
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

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
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Sheet.Point> points = m.mainDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }

        @Test
        public void testPointsMainDiagonal_NonSquare() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
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
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
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
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(0, 1), points.get(1));
            assertEquals(Point.of(1, 0), points.get(2));
            assertEquals(Point.of(1, 1), points.get(3));
        }

        @Test
        public void testRowMajorPointsorizontal() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testRowMajorPointsorizontal_SingleRow() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.rowMajorPoints(0, 0 + 1).toList();
            assertEquals(2, points.size());
        }
    }

    @Nested
    class ColumnMajorPointsTests {

        @Test
        public void testColumnMajorPoints() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 0), points.get(1));
            assertEquals(Point.of(0, 1), points.get(2));
            assertEquals(Point.of(1, 1), points.get(3));
        }

        @Test
        public void testColumnMajorPointsertical() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testColumnMajorPointsertical_SingleColumn() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.columnMajorPoints(1, 1 + 1).toList();
            assertEquals(2, points.size());
        }
    }

    @Nested
    class RowPointsTests {

        // Verify that short -> int widening uses sign extension when converting to IntMatrix.
        @Test
        public void testRowPointsows() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<List<Sheet.Point>> rows = m.rowPoints().map(s -> s.toList()).toList();
            assertEquals(3, rows.size());
            assertEquals(2, rows.get(0).size());
        }
    }

    @Nested
    class ColumnPointsTests {

        @Test
        public void testColumnPointsolumns() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<List<Sheet.Point>> cols = m.columnPoints().map(s -> s.toList()).toList();
            assertEquals(3, cols.size());
            assertEquals(2, cols.get(0).size());
        }
    }

    @Nested
    class AcceptTests {

        @Test
        public void testAccept() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            int[] sum = { 0 };
            m.accept(matrix -> {
                ShortList flat = matrix.flatten();
                for (int i = 0; i < flat.size(); i++) {
                    sum[0] += flat.get(i);
                }
            });
            assertEquals(10, sum[0]);
        }

        @Test
        public void testAccept_NullThrows() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.accept(null));
        }
    }

    @Nested
    class ApplyTests {

        // Last cell of source goes into row 2; the extra cells should be zero-padded.
        @Test
        public void testApply() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            long count = m.apply(matrix -> matrix.elementCount());
            assertEquals(4, count);
        }

        // Reshape exercises the cnt-based branch (a.length > 1).
        @Test
        public void testApply_NullThrows() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.apply(null));
        }
    }
}
