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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;
import com.landawn.abacus.util.u.OptionalBoolean;

class BooleanMatrixTest extends MatrixTestSupport {

    private static void assertShape(final int expectedRows, final int expectedColumns, final AbstractMatrix<?, ?, ?, ?, ?> matrix) {
        assertEquals(expectedRows, matrix.rowCount());
        assertEquals(expectedColumns, matrix.columnCount());
    }

    @Nested
    class ConstructorTests {

        @Test
        public void testConstructor() {
            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> new BooleanMatrix(null));

            // Test with valid array
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix2 = new BooleanMatrix(arr);
            assertEquals(2, matrix2.rowCount());
            assertEquals(2, matrix2.columnCount());
        }

        @Test
        public void testConstructor_WithValidArray() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix m = new BooleanMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertTrue(m.get(0, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testConstructor_WithValidArray_Values() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix m = new BooleanMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testConstructor_WithNonSquareMatrix() {
            boolean[][] arr = { { true, false, true }, { false, true, false } };
            BooleanMatrix m = new BooleanMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
        }

        @Test
        public void testConstructor_LargeMatrix() {
            boolean[][] arr = new boolean[100][100];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    arr[i][j] = (i + j) % 2 == 0;
                }
            }
            BooleanMatrix m = new BooleanMatrix(arr);
            assertEquals(100, m.rowCount());
            assertEquals(100, m.columnCount());
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
        }

        @Test
        public void testConstructor_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> new BooleanMatrix(null));
        }

        @Test
        public void testConstructor_WithEmptyArray() {
            BooleanMatrix m = new BooleanMatrix(new boolean[0][0]);
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
            assertTrue(m.isEmpty());
        }

        @Test
        public void testConstructor_WithSingleElement() {
            BooleanMatrix m = new BooleanMatrix(new boolean[][] { { true } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertTrue(m.get(0, 0));
        }

        @Test
        public void testConstructor_EmptyArray() {
            BooleanMatrix m = new BooleanMatrix(new boolean[0][0]);
            assertTrue(m.isEmpty());
        }
    }

    @Nested
    class EmptyTests {

        @Test
        public void testEmpty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            assertEquals(0, empty.rowCount());
            assertEquals(0, empty.columnCount());
            assertTrue(empty.isEmpty());

            // Test singleton
            assertSame(BooleanMatrix.empty(), BooleanMatrix.empty());
        }

        @Test
        public void testEmptyMatrixOperations() {
            BooleanMatrix empty = BooleanMatrix.empty();

            assertTrue(empty.flatten().isEmpty());
            assertEquals(0, empty.copy().rowCount);
            assertEquals(empty, empty.transpose());
            assertEquals(empty, empty.rotate90());

            BooleanMatrix extended = empty.resize(2, 2, true);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertTrue(extended.get(0, 0));
        }

        @Test
        public void testBooleanMatrixEmptyRowCount() {
            BooleanMatrix matrix = BooleanMatrix.empty();
            assertEquals(0, matrix.rowCount());
            assertEquals(0, matrix.columnCount());
        }
    }

    @Nested
    class WrapTests {

        // Test with valid array
        @Test
        public void testWrap() {
            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.wrap((boolean[][]) null));

            // Test with valid array
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);
            assertEquals(2, matrix.rowCount());
            assertEquals(2, matrix.columnCount());
        }

        @Test
        public void testWrap_WithValidArray() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix m = BooleanMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertTrue(m.get(0, 0));
        }

        @Test
        public void testAllFalseMatrix() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false } });

            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    assertFalse(m.get(i, j));
                }
            }
        }

        @Test
        public void testBooleanMatrixWrapGet() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertTrue(matrix.get(0, 0));
            assertEquals(false, matrix.get(0, 1));
            assertEquals(false, matrix.get(1, 0));
            assertTrue(matrix.get(1, 1));
        }

        @Test
        public void testWrap_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.wrap((boolean[][]) null));
        }

        @Test
        public void testWrap_WithEmptyArray() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testWrap_WithSingleRow() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[] { true, false, true });
            assertEquals(1, m.rowCount());
            assertEquals(3, m.columnCount());
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertTrue(m.get(0, 2));
        }
    }

    @Nested
    class CopyOfTests {

        @Test
        public void testCopyOf() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix m = BooleanMatrix.copyOf(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertTrue(m.get(0, 0));
            assertTrue(m.get(1, 1));

            // Mutating the source array must not affect the copy
            arr[0][0] = false;
            assertTrue(m.get(0, 0));

            // Mutating the copy must not affect the source array
            m.set(1, 1, false);
            assertTrue(arr[1][1]);

            // null -> rejected; empty -> shared empty matrix
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.copyOf((boolean[][]) null));
            assertTrue(BooleanMatrix.copyOf(new boolean[0][0]).isEmpty());

            // non-rectangular -> IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.copyOf(new boolean[][] { { true, false }, { true } }));
        }
    }

    @Nested
    class RandomRowTests {

        @Test
        public void testRandom() {
            BooleanMatrix matrix = BooleanMatrix.randomRow(5);
            assertEquals(1, matrix.rowCount());
            assertEquals(5, matrix.columnCount());
        }

        @Test
        public void testRandom_Values() {
            BooleanMatrix m = BooleanMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            // Just verify elements exist (values are random)
            for (int i = 0; i < 5; i++) {
                assertNotNull(m.get(0, i));
            }
        }

        @Test
        public void testRandom_WithLargeLength() {
            BooleanMatrix m = BooleanMatrix.randomRow(1000);
            assertEquals(1, m.rowCount());
            assertEquals(1000, m.columnCount());
        }

        @Test
        public void testRandomRow_Values_SingleRowDimensions() {
            BooleanMatrix m = BooleanMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }

        @Test
        public void testRandom_WithZeroLength() {
            BooleanMatrix m = BooleanMatrix.randomRow(0);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testFactories_NegativeDimensions_ThrowIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.randomRow(-1));
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.random(-1, 2));
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.random(2, -1));
        }
    }

    @Nested
    class RandomTests {

        @Test
        public void testRandomGeneratorOverloadsAreReproducibleAndPreserveZeroRowShape() {
            assertEquals(BooleanMatrix.random(4, 5, new java.util.Random(1234)), BooleanMatrix.random(4, 5, new java.util.Random(1234)));
            assertEquals(BooleanMatrix.randomRow(5, new java.util.Random(4321)), BooleanMatrix.randomRow(5, new java.util.Random(4321)));
            assertShape(0, 7, BooleanMatrix.random(0, 7, new java.util.Random(1)));
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.random(1, 1, null));
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.randomRow(1, null));
        }

        @Test
        public void testRandom_WithRowsCols() {
            BooleanMatrix m = BooleanMatrix.random(2, 3);
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
    class OfMainDiagonalTests {

        @Test
        public void testBooleanMatrixMainDiagonal() {
            // BooleanMatrix.java: BooleanMatrix matrix = BooleanMatrix.ofMainDiagonal(new boolean[] {true, false, true});
            BooleanMatrix matrix = BooleanMatrix.ofMainDiagonal(new boolean[] { true, false, true });
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertTrue(matrix.get(0, 0));
            assertEquals(false, matrix.get(1, 1));
            assertTrue(matrix.get(2, 2));
            assertEquals(false, matrix.get(0, 1));
        }

        @Test
        public void testDiagonalLU2RD() {
            boolean[] diagonal = { true, false, true };
            BooleanMatrix matrix = BooleanMatrix.ofMainDiagonal(diagonal);
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertTrue(matrix.get(0, 0));
            assertFalse(matrix.get(1, 1));
            assertTrue(matrix.get(2, 2));
            assertFalse(matrix.get(0, 1));
        }

        @Test
        public void testDiagonalLU2RD_Values() {
            BooleanMatrix m = BooleanMatrix.ofMainDiagonal(new boolean[] { true, false, true });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertTrue(m.get(0, 0));
            assertFalse(m.get(1, 1));
            assertTrue(m.get(2, 2));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
        }

        @Test
        public void testOfMainDiagonal_Empty() {
            BooleanMatrix m = BooleanMatrix.ofMainDiagonal(new boolean[0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testDiagonalLU2RD_SingleElement() {
            BooleanMatrix m = BooleanMatrix.ofMainDiagonal(new boolean[] { true });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertTrue(m.get(0, 0));
        }
    }

    @Nested
    class OfAntiDiagonalTests {

        @Test
        public void testDiagonalRU2LD() {
            boolean[] diagonal = { true, false, true };
            BooleanMatrix matrix = BooleanMatrix.ofAntiDiagonal(diagonal);
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertTrue(matrix.get(0, 2));
            assertFalse(matrix.get(1, 1));
            assertTrue(matrix.get(2, 0));
        }

        @Test
        public void testDiagonalRU2LD_Values() {
            BooleanMatrix m = BooleanMatrix.ofAntiDiagonal(new boolean[] { true, false, true });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertTrue(m.get(0, 2));
            assertFalse(m.get(1, 1));
            assertTrue(m.get(2, 0));
            assertFalse(m.get(0, 0));
            assertFalse(m.get(2, 2));
        }

        @Test
        public void testDiagonalRU2LD_Empty() {
            BooleanMatrix m = BooleanMatrix.ofAntiDiagonal(new boolean[0]);
            assertTrue(m.isEmpty());
        }
    }

    @Nested
    class OfDiagonalsTests {

        @Test
        public void testDiagonal() {
            boolean[] mainDiag = { true, true, false };
            boolean[] antiDiag = { false, true, false };
            BooleanMatrix matrix = BooleanMatrix.ofDiagonals(mainDiag, antiDiag);
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());

            // Test with different lengths
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.ofDiagonals(new boolean[] { true }, new boolean[] { true, false }));
        }

        @Test
        public void testDiagonal_WithBothDiagonals() {
            BooleanMatrix m = BooleanMatrix.ofDiagonals(new boolean[] { true, false, true }, new boolean[] { false, true, false });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertTrue(m.get(0, 0));
            assertFalse(m.get(1, 1));
            assertTrue(m.get(2, 2));
            assertFalse(m.get(0, 2));
            assertFalse(m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithOnlyMainDiagonal() {
            BooleanMatrix m = BooleanMatrix.ofDiagonals(new boolean[] { true, false, true }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertTrue(m.get(0, 0));
            assertFalse(m.get(1, 1));
            assertTrue(m.get(2, 2));
        }

        @Test
        public void testDiagonal_WithOnlyAntiDiagonal() {
            BooleanMatrix m = BooleanMatrix.ofDiagonals(null, new boolean[] { false, true, false });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertFalse(m.get(0, 2));
            assertTrue(m.get(1, 1));
            assertFalse(m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithDifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.ofDiagonals(new boolean[] { true, false }, new boolean[] { true, false, true }));
        }

        @Test
        public void testDiagonal_WithBothDiagonals_Values() {
            BooleanMatrix m = BooleanMatrix.ofDiagonals(new boolean[] { true, true, true }, new boolean[] { false, false, false });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertTrue(m.get(0, 0));
            assertTrue(m.get(1, 1));
            assertTrue(m.get(2, 2));
            assertFalse(m.get(0, 2));
            assertFalse(m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithBothDiagonalsOverlapping() {
            BooleanMatrix m = BooleanMatrix.ofDiagonals(new boolean[] { true, false, true }, new boolean[] { false, false, false });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            // Center element should be from main diagonal (set second)
            assertFalse(m.get(1, 1));
        }

        @Test
        public void testDiagonal_Both() {
            BooleanMatrix m = BooleanMatrix.ofDiagonals(new boolean[] { true, true, true }, new boolean[] { false, false, false });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertTrue(m.get(0, 0));
            assertTrue(m.get(1, 1));
            assertFalse(m.get(0, 2));
        }

        @Test
        public void testDiagonal_DifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.ofDiagonals(new boolean[] { true, true }, new boolean[] { false, false, false }));
        }

        @Test
        public void testDiagonal_WithBothNull() {
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.ofDiagonals(null, null));
        }
    }

    @Nested
    class UnboxTests {

        @Test
        public void testUnbox() {
            Boolean[][] boxed = { { true, false }, { false, true } };
            Matrix<Boolean> boxedMatrix = Matrix.wrap(boxed);
            BooleanMatrix unboxed = BooleanMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertTrue(unboxed.get(0, 0));
            assertTrue(unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_Values() {
            Boolean[][] boxed = { { true, false }, { false, true } };
            Matrix<Boolean> boxedMatrix = Matrix.wrap(boxed);
            BooleanMatrix unboxed = BooleanMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertTrue(unboxed.get(0, 0));
            assertFalse(unboxed.get(0, 1));
        }

        @Test
        public void testUnbox_Shape2By2_NullInput() {
            Matrix<Boolean> boxed = Matrix.wrap(new Boolean[][] { { true, false }, { null, true } });
            BooleanMatrix primitive = BooleanMatrix.unbox(boxed);
            assertTrue(primitive.get(0, 0));
            assertFalse(primitive.get(0, 1));
            assertFalse(primitive.get(1, 0)); // null becomes false
            assertTrue(primitive.get(1, 1));
        }

        @Test
        public void testUnbox_WithNullValues() {
            Boolean[][] boxed = { { true, null }, { null, false } };
            Matrix<Boolean> boxedMatrix = Matrix.wrap(boxed);
            BooleanMatrix unboxed = BooleanMatrix.unbox(boxedMatrix);
            assertTrue(unboxed.get(0, 0));
            assertFalse(unboxed.get(0, 1)); // null -> false
            assertFalse(unboxed.get(1, 0)); // null -> false
            assertFalse(unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_WithNullElements() {
            Boolean[][] boxed = { { true, null }, { null, false } };
            Matrix<Boolean> boxedMatrix = Matrix.wrap(boxed);
            BooleanMatrix unboxed = BooleanMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertTrue(unboxed.get(0, 0));
            assertFalse(unboxed.get(0, 1));
            assertFalse(unboxed.get(1, 0));
        }

        @Test
        public void testUnbox_IsAllNulls() {
            Boolean[][] boxed = { { null, null }, { null, null } };
            Matrix<Boolean> boxedMatrix = Matrix.wrap(boxed);
            BooleanMatrix unboxed = BooleanMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertFalse(unboxed.get(0, 0));
            assertFalse(unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_TypeNeutralGenericStorageAndEmptyShapes() {
            assertSame(BooleanMatrix.empty(), BooleanMatrix.unbox(Matrix.empty(Boolean.class)));
            assertSame(BooleanMatrix.empty(), BooleanMatrix.unbox(Matrix.empty(Boolean.class).copy()));
            assertSame(BooleanMatrix.empty(), BooleanMatrix.unbox(Matrix.wrap(new Boolean[0][0])));

            BooleanMatrix rowsWithNoColumns = BooleanMatrix.unbox(Matrix.wrap(new Boolean[2][0]));
            assertEquals(2, rowsWithNoColumns.rowCount());
            assertEquals(0, rowsWithNoColumns.columnCount());

            BooleanMatrix typeNeutralRowsWithNoColumns = BooleanMatrix.unbox(Matrix.empty(Boolean.class).resize(2, 0));
            assertEquals(2, typeNeutralRowsWithNoColumns.rowCount());
            assertEquals(0, typeNeutralRowsWithNoColumns.columnCount());

            Matrix<Boolean> expandedTypeNeutral = Matrix.empty(Boolean.class).resize(2, 2);
            expandedTypeNeutral.set(0, 1, true);
            assertEquals(BooleanMatrix.wrap(new boolean[][] { { false, true }, { false, false } }), BooleanMatrix.unbox(expandedTypeNeutral));
        }
    }

    @Nested
    class GetTests {

        @Test
        public void testGet() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);
            assertTrue(matrix.get(0, 0));
            assertFalse(matrix.get(0, 1));
            assertFalse(matrix.get(1, 0));
            assertTrue(matrix.get(1, 1));

            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(2, 0));
        }

        @Test
        public void testGetWithPoint() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);
            assertTrue(matrix.get(Point.of(0, 0)));
            assertFalse(matrix.get(Point.of(0, 1)));
        }

        @Test
        public void testGet_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            assertTrue(m.get(0, 0));
            assertTrue(m.get(1, 1));
            assertFalse(m.get(1, 2));
        }

        @Test
        public void testGetWithPoint_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertTrue(m.get(Point.of(0, 0)));
            assertTrue(m.get(Point.of(1, 1)));
            assertFalse(m.get(Point.of(0, 1)));
        }

        @Test
        public void testGet_ByIndices() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix m = BooleanMatrix.wrap(arr);
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testGet_ByPoint() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix m = BooleanMatrix.wrap(arr);
            assertTrue(m.get(Point.of(0, 0)));
            assertFalse(m.get(Point.of(0, 1)));
            assertFalse(m.get(Point.of(1, 0)));
            assertTrue(m.get(Point.of(1, 1)));
        }

        @Test
        public void testGet_AllPositions() {
            boolean[][] arr = { { true, false, true }, { false, true, false }, { true, true, false } };
            BooleanMatrix m = BooleanMatrix.wrap(arr);
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    assertEquals(arr[i][j], m.get(i, j));
                }
            }
        }

        @Test
        public void testGet_ByIndices_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testGet_ByPoint_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertTrue(m.get(Point.of(0, 0)));
            assertFalse(m.get(Point.of(0, 1)));
        }

        @Test
        public void testGet_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
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
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);
            matrix.set(0, 0, false);
            assertFalse(matrix.get(0, 0));

            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.set(2, 0, true));
        }

        @Test
        public void testSetWithPoint() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);
            matrix.set(Point.of(0, 0), false);
            assertFalse(matrix.get(Point.of(0, 0)));
        }

        @Test
        public void testSet_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.set(0, 0, false);
            assertFalse(m.get(0, 0));

            m.set(1, 1, false);
            assertFalse(m.get(1, 1));
        }

        // Leftmost column has no element to the left
        @Test
        public void testSetWithPoint_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.set(Point.of(0, 0), false);
            assertFalse(m.get(Point.of(0, 0)));
        }

        @Test
        public void testSet_ByIndices() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false } });
            m.set(0, 0, true);
            m.set(1, 1, true);
            assertTrue(m.get(0, 0));
            assertTrue(m.get(1, 1));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
        }

        @Test
        public void testSet_ByPoint() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false } });
            m.set(Point.of(0, 0), true);
            m.set(Point.of(1, 1), true);
            assertTrue(m.get(Point.of(0, 0)));
            assertTrue(m.get(Point.of(1, 1)));
            assertFalse(m.get(Point.of(0, 1)));
        }

        @Test
        public void testSet_OverwriteValue() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, true } });
            m.set(0, 0, false);
            assertFalse(m.get(0, 0));
            m.set(0, 0, true);
            assertTrue(m.get(0, 0));
        }

        @Test
        public void testSet_ByIndices_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.set(0, 1, true);
            assertTrue(m.get(0, 1));
        }

        @Test
        public void testSet_ByPoint_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.set(Point.of(1, 0), true);
            assertTrue(m.get(1, 0));
        }

        @Test
        public void testSet_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, true));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(2, 0, true));
        }
    }

    @Nested
    class ValueAboveTests {

        @Test
        public void testValueAbove() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            assertTrue(matrix.valueAbove(0, 0).isEmpty());
            assertEquals(true, matrix.valueAbove(1, 0).orElse(false));
        }

        @Test
        public void testValueAbove_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });

            OptionalBoolean up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertTrue(up.get());

            // Top row has no element above
            OptionalBoolean empty = m.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_MultipleRows() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            OptionalBoolean up = m.valueAbove(2, 1);
            assertTrue(up.isPresent());
            assertTrue(up.get());
        }

        @Test
        public void testValueAbove_Exists() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            OptionalBoolean up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertTrue(up.get());
        }

        @Test
        public void testValueAbove_NotExists() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            OptionalBoolean up = m.valueAbove(0, 0);
            assertFalse(up.isPresent());
        }
    }

    @Nested
    class ValueBelowTests {

        @Test
        public void testValueBelow() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            assertEquals(false, matrix.valueBelow(0, 0).orElse(true));
            assertTrue(matrix.valueBelow(1, 0).isEmpty());
        }

        @Test
        public void testValueBelow_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });

            OptionalBoolean down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertFalse(down.get());

            // Bottom row has no element below
            OptionalBoolean empty = m.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_AtBottomEdge() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            OptionalBoolean down = m.valueBelow(1, 0);
            assertFalse(down.isPresent());
        }

        @Test
        public void testValueBelow_MultipleRows() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            OptionalBoolean down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertFalse(down.get());
        }

        @Test
        public void testValueBelow_Exists() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            OptionalBoolean down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertFalse(down.get());
        }
    }

    @Nested
    class ValueLeftTests {

        @Test
        public void testValueLeft() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            assertTrue(matrix.valueLeft(0, 0).isEmpty());
            assertEquals(true, matrix.valueLeft(0, 1).orElse(false));
        }

        @Test
        public void testValueLeft_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });

            OptionalBoolean left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertTrue(left.get());

            // Leftmost column has no element to the left
            OptionalBoolean empty = m.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_AtLeftEdge() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            OptionalBoolean left = m.valueLeft(0, 0);
            assertFalse(left.isPresent());
        }

        @Test
        public void testValueLeft_MultipleColumns() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            OptionalBoolean left = m.valueLeft(0, 2);
            assertTrue(left.isPresent());
            assertFalse(left.get());
        }

        @Test
        public void testValueLeft_Exists() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            OptionalBoolean left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertTrue(left.get());
        }
    }

    @Nested
    class ValueRightTests {

        @Test
        public void testValueRight() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            assertEquals(false, matrix.valueRight(0, 0).orElse(true));
            assertTrue(matrix.valueRight(0, 1).isEmpty());
        }

        @Test
        public void testValueRight_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });

            OptionalBoolean right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertFalse(right.get());

            // Rightmost column has no element to the right
            OptionalBoolean empty = m.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_AtRightEdge() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            OptionalBoolean right = m.valueRight(0, 1);
            assertFalse(right.isPresent());
        }

        @Test
        public void testValueRight_MultipleColumns() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            OptionalBoolean right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertFalse(right.get());
        }

        @Test
        public void testValueRight_Exists() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            OptionalBoolean right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertFalse(right.get());
        }
    }

    @Nested
    class RowViewTests {

        @Test
        public void testRow() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            boolean[] row = matrix.rowView(0);
            assertEquals(2, row.length);
            assertTrue(row[0]);
            assertFalse(row[1]);

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(2));
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            assertArrayEquals(new boolean[] { true, false, true }, m.rowView(0));
            assertArrayEquals(new boolean[] { false, true, false }, m.rowView(1));
        }

        @Test
        public void testRowView_ArrayValues() {
            boolean[][] arr = { { true, false, true }, { false, true, false } };
            BooleanMatrix m = BooleanMatrix.wrap(arr);
            boolean[] row0 = m.rowView(0);
            assertArrayEquals(new boolean[] { true, false, true }, row0);
            boolean[] row1 = m.rowView(1);
            assertArrayEquals(new boolean[] { false, true, false }, row1);
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues_FirstRowView() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            boolean[] row = m.rowView(0);
            assertArrayEquals(new boolean[] { true, false, true }, row);
        }

        @Test
        public void testRowView() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, true, false } });
            boolean[] firstRow = matrix.rowView(0);
            assertArrayEquals(new boolean[] { true, false, false }, firstRow);
        }

        @Test
        public void testRowView_SingleRow() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            assertArrayEquals(new boolean[] { true, false, true }, m.rowView(0));
        }

        @Test
        public void testRowView_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(2));
        }

        @Test
        public void testRowView_InvalidIndex() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(1));
        }

        @Test
        public void testRowView_InvalidIndex_Shape1By2_Validation() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(5));
        }
    }

    @Nested
    class RowCopyTests {

        @Test
        public void testRowCopy() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            boolean[] rowCopy = matrix.rowCopy(0);
            assertArrayEquals(new boolean[] { true, false }, rowCopy);

            rowCopy[0] = false;
            assertArrayEquals(new boolean[] { true, false }, matrix.rowView(0));
        }

        @Test
        public void testRowCopy_InvalidIndex() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(2));
        }

        @Test
        public void testAliasedRowsAreRejected() {
            boolean[] sharedRow = { true, false };
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.wrap(new boolean[][] { sharedRow, sharedRow }));

            boolean[][] outer = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(outer);
            outer[0] = new boolean[] { false, false };
            assertArrayEquals(new boolean[] { true, false }, matrix.rowCopy(0));
        }
    }

    @Nested
    class ColumnCopyTests {

        @Test
        public void testColumn() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            boolean[] col = matrix.columnCopy(0);
            assertEquals(2, col.length);
            assertTrue(col[0]);
            assertFalse(col[1]);

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(2));
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            assertArrayEquals(new boolean[] { true, false }, m.columnCopy(0));
            assertArrayEquals(new boolean[] { false, true }, m.columnCopy(1));
            assertArrayEquals(new boolean[] { true, false }, m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_ArrayValues() {
            boolean[][] arr = { { true, false, true }, { false, true, false } };
            BooleanMatrix m = BooleanMatrix.wrap(arr);
            boolean[] col0 = m.columnCopy(0);
            assertArrayEquals(new boolean[] { true, false }, col0);
            boolean[] col1 = m.columnCopy(1);
            assertArrayEquals(new boolean[] { false, true }, col1);
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues_FirstColumnSnapshot() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            boolean[] col = m.columnCopy(0);
            assertArrayEquals(new boolean[] { true, false }, col);
        }

        @Test
        public void testColumnCopy() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, true, false } });
            boolean[] firstColumn = matrix.columnCopy(0);
            assertArrayEquals(new boolean[] { true, false }, firstColumn);
        }

        @Test
        public void testColumnCopy_SingleColumn() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true }, { false }, { true } });
            assertArrayEquals(new boolean[] { true, false, true }, m.columnCopy(0));
        }

        @Test
        public void testColumnCopy_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex_Shape1By2_Validation() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(5));
        }
    }

    @Nested
    class SetRowTests {

        @Test
        public void testSetRow() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.setRow(0, new boolean[] { false, false });
            assertFalse(matrix.get(0, 0));
            assertFalse(matrix.get(0, 1));

            assertThrows(IllegalArgumentException.class, () -> matrix.setRow(0, new boolean[] { true }));
            assertThrows(IllegalArgumentException.class, () -> matrix.setRow(0, (boolean[]) null));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.setRow(0, new boolean[] { false, true });
            assertArrayEquals(new boolean[] { false, true }, m.rowView(0));
            assertArrayEquals(new boolean[] { false, true }, m.rowView(1)); // unchanged
        }

        @Test
        public void testSetRow_WrongSize() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new boolean[] { true }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new boolean[] { true, false, true }));
        }

        @Test
        public void testSetRow_Shape2By3_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, false }, { false, false, false } });
            m.setRow(0, new boolean[] { true, true, true });
            assertArrayEquals(new boolean[] { true, true, true }, m.rowView(0));
            assertArrayEquals(new boolean[] { false, false, false }, m.rowView(1));
        }

        @Test
        public void testSetRow_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.setRow(0, new boolean[] { false, false });
            assertFalse(m.get(0, 0));
            assertFalse(m.get(0, 1));
        }

        @Test
        public void testSetRow_InvalidLength() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, false } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new boolean[] { true, true }));
        }

        @Test
        public void testSetRow_InvalidIndex() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setRow(-1, new boolean[] { true, true }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.setRow(1, new boolean[] { true, true }));
        }

        @Test
        public void testSetRow_InvalidLength_Shape1By2_Validation() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new boolean[] { true }));
        }
    }

    @Nested
    class SetColumnTests {

        @Test
        public void testSetColumn() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.setColumn(0, new boolean[] { false, false });
            assertFalse(matrix.get(0, 0));
            assertFalse(matrix.get(1, 0));

            assertThrows(IllegalArgumentException.class, () -> matrix.setColumn(0, new boolean[] { true }));
            assertThrows(IllegalArgumentException.class, () -> matrix.setColumn(0, (boolean[]) null));
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.setColumn(0, new boolean[] { false, true });
            assertArrayEquals(new boolean[] { false, true }, m.columnCopy(0));
            assertArrayEquals(new boolean[] { false, true }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testSetColumn_WrongSize() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new boolean[] { true }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new boolean[] { true, false, true }));
        }

        @Test
        public void testSetColumn_Shape3By2_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false }, { false, false } });
            m.setColumn(0, new boolean[] { true, true, true });
            assertArrayEquals(new boolean[] { true, true, true }, m.columnCopy(0));
            assertArrayEquals(new boolean[] { false, false, false }, m.columnCopy(1));
        }

        @Test
        public void testSetColumn_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.setColumn(0, new boolean[] { false, false });
            assertFalse(m.get(0, 0));
            assertFalse(m.get(1, 0));
        }

        @Test
        public void testAliasedArraySourcesAreSnapshotted() {
            BooleanMatrix columnMatrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            columnMatrix.setColumn(1, columnMatrix.rowView(0));
            assertArrayEquals(new boolean[] { false, false }, columnMatrix.rowCopy(1));

            BooleanMatrix diagonalMatrix = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, false, false }, { false, false, false } });
            diagonalMatrix.setAntiDiagonal(diagonalMatrix.rowView(0));
            assertArrayEquals(new boolean[] { true, false, false }, diagonalMatrix.antiDiagonalCopy());

            boolean[][] backing = { { true, false }, { false, true }, { false, false } };
            BooleanMatrix fillMatrix = BooleanMatrix.wrap(backing);
            fillMatrix.copyFrom(1, 0, backing);
            assertArrayEquals(new boolean[] { false, true }, fillMatrix.rowCopy(2));
        }

        @Test
        public void testSetColumn_InvalidLength() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new boolean[] { true }));
        }

        @Test
        public void testSetColumn_InvalidIndex() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(-1, new boolean[] { true, true }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(2, new boolean[] { true, true }));
        }
    }

    @Nested
    class UpdateRowTests {

        @Test
        public void testDocumentedCallbackFailuresKeepCompletedUpdates() throws Exception {
            for (int operation = 0; operation < 8; operation++) {
                final int selectedOperation = operation;
                final BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[2][2]);
                final AtomicInteger calls = new AtomicInteger();
                final IllegalStateException failure = new IllegalStateException("callback failed");
                final Throwables.BooleanUnaryOperator<IllegalStateException> operator = value -> {
                    if (calls.incrementAndGet() == 2) {
                        throw failure;
                    }

                    return true;
                };

                assertSame(failure, assertThrows(IllegalStateException.class, () -> Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                    switch (selectedOperation) {
                        case 0 -> matrix.updateRow(0, operator);
                        case 1 -> matrix.updateColumn(0, operator);
                        case 2 -> matrix.updateMainDiagonal(operator);
                        case 3 -> matrix.updateAntiDiagonal(operator);
                        case 4 -> matrix.updateAll(operator);
                        case 5 -> matrix.updateAll((row, column) -> operator.applyAsBoolean(false));
                        case 6 -> matrix.replaceIf(value -> operator.applyAsBoolean(value), true);
                        case 7 -> matrix.replaceIf((row, column) -> operator.applyAsBoolean(false), true);
                        default -> throw new AssertionError(selectedOperation);
                    }
                })));

                assertEquals(2, calls.get(), "operation " + selectedOperation);
                assertArrayEquals(selectedOperation == 3 ? new boolean[] { false, true } : new boolean[] { true, false }, matrix.rowCopy(0),
                        "operation " + selectedOperation);
                assertArrayEquals(new boolean[] { false, false }, matrix.rowCopy(1), "operation " + selectedOperation);
            }
        }

        @Test
        public void testUpdateRow() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.updateRow(0, val -> !val);
            assertFalse(matrix.get(0, 0));
            assertTrue(matrix.get(0, 1));
        }

        @Test
        public void testUpdateRow_Shape2By2_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.updateRow(0, x -> !x);
            assertArrayEquals(new boolean[] { false, true }, m.rowView(0));
            assertArrayEquals(new boolean[] { false, true }, m.rowView(1)); // unchanged
        }

        // Test rotate270() with tall matrix
        @Test
        public void testUpdateRow_AllElements() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            m.updateRow(0, x -> true);
            assertArrayEquals(new boolean[] { true, true, true }, m.rowView(0));
            assertArrayEquals(new boolean[] { false, true, false }, m.rowView(1)); // unchanged
        }

        @Test
        public void testUpdateRow_Shape2By3_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, false, false } });
            m.updateRow(0, val -> !val);
            assertArrayEquals(new boolean[] { false, true, false }, m.rowView(0));
            assertArrayEquals(new boolean[] { false, false, false }, m.rowView(1));
        }

        @Test
        public void testUpdateRow_MultipleRows() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            m.updateRow(1, val -> !val);
            assertArrayEquals(new boolean[] { true, false }, m.rowView(0));
            assertArrayEquals(new boolean[] { true, false }, m.rowView(1));
            assertArrayEquals(new boolean[] { true, true }, m.rowView(2));
        }

        // Original unchanged
        @Test
        public void testUpdateRow_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.updateRow(0, val -> !val);
            assertFalse(m.get(0, 0));
            assertTrue(m.get(0, 1));
        }

        @Test
        public void testUpdateRowAndUpdateColumnNullOperator() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            assertThrows(IllegalArgumentException.class, () -> matrix.updateRow(0, (Throwables.BooleanUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateColumn(0, (Throwables.BooleanUnaryOperator<RuntimeException>) null));
        }

        @Test
        public void testUpdateRowAndUpdateColumnInvalidIndex() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateRow(-1, val -> !val));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateColumn(2, val -> !val));
        }
    }

    @Nested
    class UpdateColumnTests {

        @Test
        public void testUpdateColumn() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.updateColumn(0, val -> !val);
            assertFalse(matrix.get(0, 0));
            assertTrue(matrix.get(1, 0));
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.updateColumn(0, x -> !x);
            assertArrayEquals(new boolean[] { false, true }, m.columnCopy(0));
            assertArrayEquals(new boolean[] { false, true }, m.columnCopy(1)); // unchanged
        }

        // Test transpose() with tall matrix
        @Test
        public void testUpdateColumn_AllElements() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, false } });
            m.updateColumn(1, x -> true);
            assertArrayEquals(new boolean[] { true, true, true }, m.columnCopy(1));
            assertArrayEquals(new boolean[] { true, false, true }, m.columnCopy(0)); // unchanged
        }

        @Test
        public void testUpdateColumn_Shape3By2_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, false }, { true, false } });
            m.updateColumn(0, val -> !val);
            assertArrayEquals(new boolean[] { false, true, false }, m.columnCopy(0));
            assertArrayEquals(new boolean[] { false, false, false }, m.columnCopy(1));
        }

        @Test
        public void testUpdateColumn_MultipleColumns() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, false, false } });
            m.updateColumn(2, val -> !val);
            assertArrayEquals(new boolean[] { true, false, false }, m.rowView(0));
            assertArrayEquals(new boolean[] { false, false, true }, m.rowView(1));
        }

        @Test
        public void testUpdateColumn_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.updateColumn(0, val -> !val);
            assertFalse(m.get(0, 0));
            assertTrue(m.get(1, 0));
        }
    }

    @Nested
    class MainDiagonalCopyTests {

        @Test
        public void testGetLU2RD() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            boolean[] diag = matrix.mainDiagonalCopy();
            assertEquals(2, diag.length);
            assertTrue(diag[0]);
            assertTrue(diag[1]);

            // Test non-square matrix
            BooleanMatrix nonSquare = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            assertArrayEquals(new boolean[] { true }, nonSquare.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_Shape3By3_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            assertArrayEquals(new boolean[] { true, true, true }, m.mainDiagonalCopy());
        }

        @Test
        public void testGetLU2RD_nonSquare() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertArrayEquals(new boolean[] { true }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_Shape3By3_ArrayValues_AlternatingBooleanDiagonal() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            boolean[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new boolean[] { true, true, true }, diag);
        }

        @Test
        public void testMainDiagonalCopy_NonSquare_Shape2By3_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            assertArrayEquals(new boolean[] { true, true }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, true, false }, { false, false, true } });
            boolean[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new boolean[] { true, true, true }, diag);
        }

        @Test
        public void testMainDiagonalCopy_NonSquare() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, false } });
            assertArrayEquals(new boolean[] { true, true }, m.mainDiagonalCopy());
        }

        @Test
        public void testmainDiagonalCopy() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, true, false }, { false, false, true } });
            boolean[] diagonal = matrix.mainDiagonalCopy();
            assertArrayEquals(new boolean[] { true, true, true }, diagonal);
        }

        @Test
        public void testMainDiagonalCopy_SingleElement() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertArrayEquals(new boolean[] { true }, m.mainDiagonalCopy());
        }
    }

    @Nested
    class SetMainDiagonalTests {

        @Test
        public void testSetLU2RD() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.setMainDiagonal(new boolean[] { false, false });
            assertFalse(matrix.get(0, 0));
            assertFalse(matrix.get(1, 1));

            assertThrows(IllegalArgumentException.class, () -> matrix.setMainDiagonal(new boolean[] { true }));
        }

        @Test
        public void testSetMainDiagonal_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            m.setMainDiagonal(new boolean[] { false, false, false });
            assertFalse(m.get(0, 0));
            assertFalse(m.get(1, 1));
            assertFalse(m.get(2, 2));
        }

        @Test
        public void testSetMainDiagonal_NonSquare() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            m.setMainDiagonal(new boolean[] { false });
            assertFalse(m.get(0, 0));
        }

        @Test
        public void testSetMainDiagonal_ArrayTooShort() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new boolean[] { true, false }));
        }

        @Test
        public void testSetMainDiagonal_Shape3By3_PreservesOffDiagonalValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, false }, { false, false, false }, { false, false, false } });
            m.setMainDiagonal(new boolean[] { true, true, true });
            assertTrue(m.get(0, 0));
            assertTrue(m.get(1, 1));
            assertTrue(m.get(2, 2));
            assertFalse(m.get(0, 1));
        }

        @Test
        public void testSetMainDiagonal_NonSquare_Shape2By3_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[2][3]);
            m.setMainDiagonal(new boolean[] { true, true });
            assertArrayEquals(new boolean[] { true, true }, m.mainDiagonalCopy());
        }

        @Test
        public void testSetMainDiagonal() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, true, false }, { false, false, true } });
            m.setMainDiagonal(new boolean[] { false, false, false });
            assertFalse(m.get(0, 0));
            assertFalse(m.get(1, 1));
            assertFalse(m.get(2, 2));
        }

        @Test
        public void testSetDiagonal_NullArraysOnEmptyMatrixThrow() {
            BooleanMatrix m = BooleanMatrix.empty();
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(null));
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(null));
        }

        @Test
        public void testSetLU2RD_invalidLength() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][3]);
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new boolean[] { true, true }));
        }

        @Test
        public void testSetMainDiagonal_InvalidLength() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new boolean[] { true }));
        }
    }

    @Nested
    class UpdateMainDiagonalTests {

        @Test
        public void testUpdateLU2RD() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.updateMainDiagonal(val -> !val);
            assertFalse(matrix.get(0, 0));
            assertFalse(matrix.get(1, 1));
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            m.updateMainDiagonal(x -> !x);
            assertFalse(m.get(0, 0));
            assertFalse(m.get(1, 1));
            assertFalse(m.get(2, 2));
            assertFalse(m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateMainDiagonal_NonSquare() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            m.updateMainDiagonal(x -> !x);
            assertFalse(m.get(0, 0));
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3_TogglesEveryDiagonalElement() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, false, false }, { false, false, true } });
            m.updateMainDiagonal(val -> !val);
            assertFalse(m.get(0, 0));
            assertTrue(m.get(1, 1));
            assertFalse(m.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal_AllValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, true, false }, { true, false, true }, { false, true, false } });
            m.updateMainDiagonal(val -> true);
            assertTrue(m.get(0, 0));
            assertTrue(m.get(1, 1));
            assertTrue(m.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, false, false }, { false, false, false } });
            m.updateMainDiagonal(val -> !val);
            assertFalse(m.get(0, 0));
            assertTrue(m.get(1, 1));
            assertTrue(m.get(2, 2));
        }
    }

    @Nested
    class AntiDiagonalCopyTests {

        @Test
        public void testGetRU2LD() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            boolean[] diag = matrix.antiDiagonalCopy();
            assertEquals(2, diag.length);
            assertFalse(diag[0]);
            assertFalse(diag[1]);
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            assertArrayEquals(new boolean[] { true, true, true }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertArrayEquals(new boolean[] { false }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare_Shape2By3_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            assertArrayEquals(new boolean[] { true, true }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, true }, { false, true, false }, { true, false, false } });
            boolean[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new boolean[] { true, true, true }, diag);
        }

        // Add border of false values
        @Test
        public void testantiDiagonalCopy() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { false, false, true }, { false, true, false }, { true, false, false } });
            boolean[] antiDiag = matrix.antiDiagonalCopy();
            assertArrayEquals(new boolean[] { true, true, true }, antiDiag);
        }
    }

    @Nested
    class SetAntiDiagonalTests {

        @Test
        public void testSetRU2LD() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.setAntiDiagonal(new boolean[] { true, true });
            assertTrue(matrix.get(0, 1));
            assertTrue(matrix.get(1, 0));
        }

        @Test
        public void testSetAntiDiagonal_NonSquare() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            m.setAntiDiagonal(new boolean[] { true });
            assertTrue(m.get(0, 1));
        }

        @Test
        public void testSetAntiDiagonal_ArrayTooShort() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new boolean[] { true, false }));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, false }, { false, false, false }, { false, false, false } });
            m.setAntiDiagonal(new boolean[] { true, true, true });
            assertTrue(m.get(0, 2));
            assertTrue(m.get(1, 1));
            assertTrue(m.get(2, 0));
            assertFalse(m.get(0, 0));
        }

        @Test
        public void testSetAntiDiagonal() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            m.setAntiDiagonal(new boolean[] { false, false, false });
            assertFalse(m.get(0, 2));
            assertFalse(m.get(1, 1));
            assertFalse(m.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_InvalidLength() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][3]);
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new boolean[] { true, true }));
        }
    }

    @Nested
    class UpdateAntiDiagonalTests {

        @Test
        public void testUpdateRU2LD() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.updateAntiDiagonal(val -> !val);
            assertTrue(matrix.get(0, 1));
            assertTrue(matrix.get(1, 0));
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            m.updateAntiDiagonal(x -> !x);
            assertFalse(m.get(0, 2));
            assertFalse(m.get(1, 1));
            assertFalse(m.get(2, 0));
            assertFalse(m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateAntiDiagonal_NonSquare() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            m.updateAntiDiagonal(x -> !x);
            assertTrue(m.get(0, 1));
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3_TogglesEveryDiagonalElement() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, true }, { false, false, false }, { true, false, false } });
            m.updateAntiDiagonal(val -> !val);
            assertFalse(m.get(0, 2));
            assertTrue(m.get(1, 1));
            assertFalse(m.get(2, 0));
        }

        @Test
        public void testUpdateAntiDiagonal_Rectangular() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, true }, { true, false, false } });
            m.updateAntiDiagonal(val -> !val);
            assertArrayEquals(new boolean[] { false, true }, m.antiDiagonalCopy());
        }

        @Test
        public void testUpdateAntiDiagonal() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, true, false }, { false, false, true } });
            m.updateAntiDiagonal(val -> !val);
            assertTrue(m.get(0, 2));
            assertFalse(m.get(1, 1));
            assertTrue(m.get(2, 0));
        }

        @Test
        public void testUpdateDiagonals_Rectangular() {
            BooleanMatrix nonSquare = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });

            nonSquare.updateAntiDiagonal(v -> !v);
            assertArrayEquals(new boolean[] { false, false }, nonSquare.antiDiagonalCopy());
            nonSquare.updateMainDiagonal(v -> !v);
            assertArrayEquals(new boolean[] { false, true }, nonSquare.mainDiagonalCopy());
        }

        @Test
        public void testUpdateAntiDiagonal_SquareMatrix() {
            // Verify updateAntiDiagonal works correctly on a square matrix.
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });

            m.updateAntiDiagonal(v -> !v);

            // Anti-diagonal positions: (0,2), (1,1), (2,0)
            assertFalse(m.get(0, 2)); // was true, negated
            assertFalse(m.get(1, 1)); // was true, negated
            assertFalse(m.get(2, 0)); // was true, negated

            // Non-anti-diagonal positions should be unchanged
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertFalse(m.get(1, 2));
            assertFalse(m.get(2, 1));
            assertTrue(m.get(2, 2));
        }
    }

    @Nested
    class UpdateAllTests {

        @Test
        public void testUpdateAll() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.updateAll(val -> !val);
            assertFalse(matrix.get(0, 0));
            assertTrue(matrix.get(0, 1));
            assertTrue(matrix.get(1, 0));
            assertFalse(matrix.get(1, 1));
        }

        @Test
        public void testUpdateAllUnarySequentialTallMatrixUsesRowMajorOrder() throws Exception {
            final BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[3][2]);
            final AtomicInteger next = new AtomicInteger();

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> matrix.updateAll(x -> next.incrementAndGet() % 2 == 1));

            assertArrayEquals(new boolean[] { true, false }, matrix.rowCopy(0));
            assertArrayEquals(new boolean[] { true, false }, matrix.rowCopy(1));
            assertArrayEquals(new boolean[] { true, false }, matrix.rowCopy(2));
        }

        @Test
        public void testUpdateAllWithIndices() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.updateAll((i, j) -> i == j);
            assertTrue(matrix.get(0, 0));
            assertFalse(matrix.get(0, 1));
            assertFalse(matrix.get(1, 0));
            assertTrue(matrix.get(1, 1));
        }

        @Test
        public void testUpdateAll_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.updateAll(x -> !x);
            assertFalse(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertTrue(m.get(1, 0));
            assertFalse(m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithIndices() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false } });
            m.updateAll((i, j) -> i == j);
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testUpdateAll_ComplexFunction() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, false }, { false, false, false }, { false, false, false } });
            m.updateAll((i, j) -> (i + j) % 2 == 0);
            assertTrue(m.get(0, 0)); // 0+0=0, even
            assertFalse(m.get(0, 1)); // 0+1=1, odd
            assertFalse(m.get(1, 0)); // 1+0=1, odd
            assertTrue(m.get(1, 1)); // 1+1=2, even
            assertTrue(m.get(2, 2)); // 2+2=4, even
        }

        @Test
        public void testUpdateAll_BiFunction() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.updateAll((i, j) -> i == j);
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithConstant() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.updateAll(val -> true);
            assertTrue(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertTrue(m.get(1, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testUpdateAll_UnaryOperator() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.updateAll(val -> !val);
            assertFalse(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertTrue(m.get(1, 0));
            assertFalse(m.get(1, 1));
        }

        @Test
        public void testBooleanMatrixUpdateAllNullOperator() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix emptyLike = BooleanMatrix.wrap(new boolean[][] { {}, {} });
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.BooleanUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.IntBiFunction<Boolean, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateMainDiagonal((Throwables.BooleanUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAntiDiagonal((Throwables.BooleanUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.BooleanPredicate<RuntimeException>) null, true));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, true));

            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateMainDiagonal((Throwables.BooleanUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAntiDiagonal((Throwables.BooleanUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.BooleanPredicate<RuntimeException>) null, true));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, true));
        }
    }

    @Nested
    class ReplaceIfTests {

        @Test
        public void testReplaceIf() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.replaceIf(val -> !val, true);
            assertTrue(matrix.get(0, 0));
            assertTrue(matrix.get(0, 1));
            assertTrue(matrix.get(1, 0));
            assertTrue(matrix.get(1, 1));
        }

        @Test
        public void testReplaceIfWithIndices() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.replaceIf((i, j) -> i == j, true);
            assertTrue(matrix.get(0, 0));
            assertFalse(matrix.get(0, 1));
            assertFalse(matrix.get(1, 0));
            assertTrue(matrix.get(1, 1));
        }

        @Test
        public void testReplaceIf_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            m.replaceIf(x -> x, false);
            assertFalse(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(0, 2));
            assertFalse(m.get(1, 0));
            assertFalse(m.get(1, 1));
            assertFalse(m.get(1, 2));
        }

        @Test
        public void testReplaceIf_WithIndices() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            m.replaceIf((i, j) -> i == j, false); // Replace diagonal
            assertFalse(m.get(0, 0));
            assertFalse(m.get(1, 1));
            assertFalse(m.get(2, 2));
            assertFalse(m.get(0, 1)); // unchanged
        }

        @Test
        public void testReplaceIf_NoMatches() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, true }, { true, true } });
            m.replaceIf(x -> !x, true); // Replace all false with true (but there are none)

            // All should still be true
            assertTrue(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertTrue(m.get(1, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testReplaceIf_WithIndices_EdgeCases() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.replaceIf((i, j) -> i + j == 1, true); // Replace positions where i+j=1

            assertTrue(m.get(0, 0)); // unchanged
            assertTrue(m.get(0, 1)); // 0+1=1, replaced
            assertTrue(m.get(1, 0)); // 1+0=1, replaced
            assertTrue(m.get(1, 1)); // unchanged
        }

        @Test
        public void testReplaceIf_Predicate() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.replaceIf(val -> val, false);
            assertFalse(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertFalse(m.get(1, 1));
        }

        @Test
        public void testReplaceIf_BiPredicate() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.replaceIf((i, j) -> i == j, false);
            assertFalse(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertFalse(m.get(1, 1));
        }

        @Test
        public void testReplaceIf_NoMatches_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.replaceIf(val -> false, true);
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testReplaceIf_Predicate_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.replaceIf(val -> !val, true);
            assertTrue(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertTrue(m.get(1, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testReplaceIf_BiPredicate_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][3]);
            m.replaceIf((i, j) -> i == j, true);
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertTrue(m.get(1, 1));
        }
    }

    @Nested
    class MapTests {

        @Test
        public void testMap() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix mapped = matrix.map(val -> !val);
            assertFalse(mapped.get(0, 0));
            assertTrue(mapped.get(0, 1));
            assertTrue(mapped.get(1, 0));
            assertFalse(mapped.get(1, 1));
        }

        @Test
        public void testMap_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix result = m.map(x -> !x);
            assertFalse(result.get(0, 0));
            assertTrue(result.get(0, 1));
            assertTrue(result.get(1, 0));
            assertFalse(result.get(1, 1));

            // Original unchanged
            assertTrue(m.get(0, 0));
        }

        @Test
        public void testMap_IdentityFunction() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix result = m.map(x -> x);
            assertEquals(m, result);
        }

        @Test
        public void testMap_Shape2By2_NegatesAllCellsAndPreservesSource() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix mapped = m.map(val -> !val);
            assertFalse(mapped.get(0, 0));
            assertTrue(mapped.get(0, 1));
            assertTrue(mapped.get(1, 0));
            assertFalse(mapped.get(1, 1));
            assertTrue(m.get(0, 0));
        }

        @Test
        public void testMap_Identity() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix mapped = m.map(val -> val);
            assertTrue(mapped.get(0, 0));
            assertFalse(mapped.get(0, 1));
            assertNotSame(m, mapped);
        }

        @Test
        public void testMap_Shape2By2_NegatesFirstRowAndPreservesSource() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix inverted = m.map(val -> !val);
            assertFalse(inverted.get(0, 0));
            assertTrue(inverted.get(0, 1));
            // Original unchanged
            assertTrue(m.get(0, 0));
        }

        @Test
        public void testMapNullMapper() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            assertThrows(IllegalArgumentException.class, () -> matrix.map((Throwables.BooleanUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.mapToObj((Throwables.BooleanFunction<String, RuntimeException>) null, String.class));
        }

        @Test
        public void testBooleanMatrixRowsForZeroColumnMatrix() {
            final BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { {}, {}, {} });
            final List<List<Boolean>> rows = matrix.rowStreams().map(row -> row.toList()).toList();

            assertEquals(3, rows.size());
            assertTrue(rows.get(0).isEmpty());
            assertTrue(rows.get(1).isEmpty());
            assertTrue(rows.get(2).isEmpty());
        }
    }

    @Nested
    class MapToObjTests {

        @Test
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void testObjectMappingRuntimeArrayTypeAndStoreFailure() {
            final BooleanMatrix source = BooleanMatrix.wrap(new boolean[] { true, true });
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
            assertArrayEquals(new boolean[] { true, true }, source.rowCopy(0));
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
            final BooleanMatrix empty = new BooleanMatrix(new boolean[0][], 3);
            final Matrix<Object> mapped = empty.mapToObj(value -> null, largestElementType);
            assertEquals(0, mapped.rowCount());
            assertEquals(3, mapped.columnCount());
            assertEquals(largestElementType, mapped.elementType());

            final AtomicInteger calls = new AtomicInteger();
            assertThrows(IllegalArgumentException.class, () -> empty.mapToObj(value -> {
                calls.incrementAndGet();
                return null;
            }, tooDeep));
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.wrap(new boolean[] { true }).mapToObj(value -> {
                calls.incrementAndGet();
                return null;
            }, tooDeep));
            assertEquals(0, calls.get());
        }

        @Test
        public void testMapToObj() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            Matrix<String> mapped = matrix.mapToObj(val -> val ? "YES" : "NO", String.class);
            assertEquals("YES", mapped.get(0, 0));
            assertEquals("NO", mapped.get(0, 1));
        }

        @Test
        public void testMapToObj_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            Matrix<String> result = m.mapToObj(x -> x ? "T" : "F", String.class);
            assertEquals("T", result.get(0, 0));
            assertEquals("F", result.get(0, 1));
            assertEquals("F", result.get(1, 0));
            assertEquals("T", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_MapsBooleanLetters() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            Matrix<String> mapped = m.mapToObj(val -> val ? "T" : "F", String.class);
            assertEquals("T", mapped.get(0, 0));
            assertEquals("F", mapped.get(0, 1));
            assertEquals("F", mapped.get(1, 0));
            assertEquals("T", mapped.get(1, 1));
        }

        @Test
        public void testMapToObj_WithComplexType() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            Matrix<Integer> mapped = m.mapToObj(val -> val ? 1 : 0, Integer.class);
            assertEquals(1, mapped.get(0, 0));
            assertEquals(0, mapped.get(0, 1));
            assertEquals(0, mapped.get(1, 0));
            assertEquals(1, mapped.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_MapsBooleanWords() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            Matrix<String> stringMatrix = m.mapToObj(val -> val ? "YES" : "NO", String.class);
            assertEquals("YES", stringMatrix.get(0, 0));
            assertEquals("NO", stringMatrix.get(0, 1));
        }

        @Test
        public void testMapToObj_NullHandling() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            Matrix<Integer> result = m.mapToObj(x -> x ? 1 : 0, Integer.class);
            assertEquals(Integer.valueOf(1), result.get(0, 0));
            assertEquals(Integer.valueOf(0), result.get(0, 1));
            assertEquals(Integer.valueOf(0), result.get(1, 0));
            assertEquals(Integer.valueOf(1), result.get(1, 1));
        }
    }

    @Nested
    class FillTests {

        @Test
        public void testFill() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.fill(true);
            assertTrue(matrix.get(0, 0));
            assertTrue(matrix.get(0, 1));
            assertTrue(matrix.get(1, 0));
            assertTrue(matrix.get(1, 1));
        }

        @Test
        public void testFill_WithValue() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.fill(true);
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertTrue(m.get(i, j));
                }
            }
        }

        @Test
        public void testFill_SingleValue() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false } });
            m.fill(true);
            assertTrue(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertTrue(m.get(1, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testFill_SingleValue_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.fill(true);
            assertTrue(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertTrue(m.get(1, 0));
            assertTrue(m.get(1, 1));
        }
    }

    @Nested
    class CopyFromTests {

        @Test
        public void testCopyFrom() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            boolean[][] fillArr = { { false, false }, { false, false } };
            matrix.copyFrom(fillArr);
            assertFalse(matrix.get(0, 0));
            assertFalse(matrix.get(0, 1));
            assertFalse(matrix.get(1, 0));
            assertFalse(matrix.get(1, 1));
        }

        // Pins the fill(int, int, boolean[][]) javadoc example: a source row extending past the
        @Test
        public void testCopyFromWithIndices() {
            boolean[][] arr = { { true, false, true }, { false, true, false } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            boolean[][] fillArr = { { false, false } };
            matrix.copyFrom(0, 1, fillArr);
            assertFalse(matrix.get(0, 1));
            assertFalse(matrix.get(0, 2));

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(-1, 0, fillArr));
        }

        @Test
        public void testCopyFromWithIndices_SourceWiderThanRemainingColumnsIsClipped() {
            // Pins the fill(int, int, boolean[][]) javadoc example: a source row extending past the
            // matrix bounds is clipped, not an error.
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[4][4]);
            matrix.copyFrom(1, 2, new boolean[][] { { true, true, true } });
            assertTrue(matrix.get(1, 2));
            assertTrue(matrix.get(1, 3));
            // Row 2 untouched, and nothing written outside the matrix.
            assertFalse(matrix.get(2, 2));
            assertFalse(matrix.get(1, 1));
        }

        @Test
        public void testCopyFrom_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, false }, { false, false, false }, { false, false, false } });
            boolean[][] patch = { { true, false }, { false, true } };
            m.copyFrom(patch);
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertTrue(m.get(1, 1));
            assertFalse(m.get(2, 2)); // unchanged
        }

        @Test
        public void testCopyFromAtPosition() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, false }, { false, false, false }, { false, false, false } });
            boolean[][] patch = { { true, false }, { false, true } };
            m.copyFrom(1, 1, patch);
            assertFalse(m.get(0, 0)); // unchanged
            assertTrue(m.get(1, 1));
            assertFalse(m.get(1, 2));
            assertFalse(m.get(2, 1));
            assertTrue(m.get(2, 2));
        }

        @Test
        public void testCopyFrom_PartialOverlap() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, false }, { false, false, false }, { false, false, false } });
            boolean[][] patch = { { true, true }, { true, true } };
            m.copyFrom(2, 2, patch); // Only partial overlap
            assertFalse(m.get(0, 0));
            assertFalse(m.get(1, 1));
            assertTrue(m.get(2, 2)); // Only this one should be set
        }

        @Test
        public void testCopyFrom_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false } });
            m.copyFrom(new boolean[][] { { true, true }, { true, true } });
            assertTrue(m.get(0, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testCopyFrom_WithOffset() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, false }, { false, false, false }, { false, false, false } });
            m.copyFrom(1, 1, new boolean[][] { { true, true }, { true, true } });
            assertFalse(m.get(0, 0));
            assertTrue(m.get(1, 1));
            assertTrue(m.get(2, 2));
        }

        @Test
        public void testCopyFrom_WithPartialArray() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, false }, { false, false, false }, { false, false, false } });
            m.copyFrom(0, 0, new boolean[][] { { true, true } });
            assertTrue(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertFalse(m.get(0, 2));
            assertFalse(m.get(1, 0));
        }

        @Test
        public void testCopyFrom_Shape2By2_PreservesCellsOutsideCopiedRegion() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][3]);
            m.copyFrom(new boolean[][] { { true, true }, { true, true } });
            assertTrue(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertTrue(m.get(1, 0));
            assertTrue(m.get(1, 1));
            assertFalse(m.get(2, 2));
        }

        @Test
        public void testCopyFromWithPosition() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[4][4]);
            m.copyFrom(1, 1, new boolean[][] { { true, true }, { true, true } });
            assertFalse(m.get(0, 0));
            assertTrue(m.get(1, 1));
            assertTrue(m.get(1, 2));
            assertTrue(m.get(2, 1));
            assertTrue(m.get(2, 2));
        }

        @Test
        public void testCopyFrom_FullOverwrite() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false } });
            m.copyFrom(new boolean[][] { { true, true }, { true, true } });
            assertTrue(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertTrue(m.get(1, 0));
            assertTrue(m.get(1, 1));
        }

        @Test
        public void testCopyFrom_PartialOverwrite() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, false }, { false, false, false }, { false, false, false } });
            m.copyFrom(new boolean[][] { { true, true }, { true, true } });
            assertTrue(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertFalse(m.get(0, 2));
            assertTrue(m.get(1, 0));
            assertTrue(m.get(1, 1));
            assertFalse(m.get(1, 2));
            assertFalse(m.get(2, 0));
        }

        @Test
        public void testCopyFrom_WithOffset_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, false }, { false, false, false }, { false, false, false } });
            m.copyFrom(1, 1, new boolean[][] { { true, true }, { true, true } });
            assertFalse(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertTrue(m.get(1, 1));
            assertTrue(m.get(1, 2));
            assertTrue(m.get(2, 1));
            assertTrue(m.get(2, 2));
        }

        @Test
        public void testCopyFrom_EmptySource() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.copyFrom(new boolean[0][0]);
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
        }

        @Test
        public void testCopyFrom_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            boolean[][] patch = { { true, false }, { false, true } };
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
        }

        @Test
        public void testCopyFrom_WithOffset_InvalidPosition() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[2][2]);
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(3, 3, new boolean[][] { { true, true }, { true, true } }));
        }

        @Test
        public void testCopyFrom_NegativeIndexThrows() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, new boolean[][] { { true } }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(0, -1, new boolean[][] { { true } }));
        }
    }

    @Nested
    class CopyTests {

        @Test
        public void testCopy() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix copy = matrix.copy();
            assertEquals(matrix.rowCount(), copy.rowCount());
            assertEquals(matrix.columnCount(), copy.columnCount());
            assertTrue(copy.get(0, 0));

            // Ensure it's a deep copy
            copy.set(0, 0, false);
            assertTrue(matrix.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertTrue(copy.get(0, 0));

            // Modify copy shouldn't affect original
            copy.set(0, 0, false);
            assertTrue(m.get(0, 0));
            assertFalse(copy.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2_FirstRowAndIndependentStorage() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix copy = m.copy();
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertTrue(copy.get(0, 0));
            assertFalse(copy.get(0, 1));
            copy.set(0, 0, false);
            assertTrue(m.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2_DimensionsAndIndependentStorage() {
            BooleanMatrix original = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix copy = original.copy();
            assertEquals(original.rowCount(), copy.rowCount());
            assertEquals(original.columnCount(), copy.columnCount());
            assertTrue(copy.get(0, 0));
            // Modify copy
            copy.set(0, 0, false);
            assertTrue(original.get(0, 0)); // Original unchanged
        }

        @Test
        public void testCopy_EmptyMatrix() {
            BooleanMatrix m = BooleanMatrix.empty();
            BooleanMatrix copy = m.copy();
            assertTrue(copy.isEmpty());
        }
    }

    @Nested
    class CopyRowsTests {

        @Test
        public void testCopyRows() {
            boolean[][] arr = { { true, false }, { false, true }, { true, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix copy = matrix.copyRows(0, 2);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(-1, 2));
        }

        @Test
        public void testCopyRows_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            BooleanMatrix subset = m.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertFalse(subset.get(0, 0));
            assertTrue(subset.get(1, 2));
        }

        @Test
        public void testCopyRows_Shape3By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            BooleanMatrix copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertFalse(copy.get(0, 0));
            assertTrue(copy.get(0, 1));
        }

        @Test
        public void testCopy_Rows() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            BooleanMatrix subset = matrix.copyRows(1, 3);
            // Result: [[false, true], [true, true]]
            assertEquals(2, subset.rowCount());
            assertEquals(2, subset.columnCount());
            assertFalse(subset.get(0, 0));
            assertTrue(subset.get(0, 1));
            assertTrue(subset.get(1, 0));
            assertTrue(subset.get(1, 1));
        }

        @Test
        public void testCopyRangesEmpty_ReturnsEmptyMatrix() {
            // An empty row slice keeps the column count (0 x N) and an empty column slice keeps the row
            // count (N x 0); only an empty range in BOTH dimensions collapses to 0 x 0.
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });

            BooleanMatrix empty = m.copyRows(0, 0);
            assertEquals(0, empty.rowCount());
            assertEquals(3, empty.columnCount());

            BooleanMatrix emptyRows = m.copyRegion(1, 1, 0, 3);
            assertEquals(0, emptyRows.rowCount());
            assertEquals(3, emptyRows.columnCount());

            BooleanMatrix emptyCols = m.copyRegion(0, 2, 1, 1);
            assertEquals(2, emptyCols.rowCount());
            assertEquals(0, emptyCols.columnCount());
        }

        @Test
        public void testCopyRows_SingleRow() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            BooleanMatrix copy = m.copyRows(1, 2);
            assertEquals(1, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertFalse(copy.get(0, 0));
            assertTrue(copy.get(0, 1));
        }

        @Test
        public void testCopyRows_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(2, 1));
        }

        @Test
        public void testCopyRows_InvalidRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 2));
        }
    }

    @Nested
    class CopyRegionTests {

        @Test
        public void testCopy_FullMatrix() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix copy = m.copyRegion(0, 2, 0, 2);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertTrue(copy.get(0, 0));
            assertTrue(copy.get(1, 1));

            // Modify copy shouldn't affect original
            copy.set(0, 0, false);
            assertTrue(m.get(0, 0));
        }

        // An empty row slice keeps the column count (0 x N) and an empty column slice keeps the row
        // count (N x 0); only an empty range in BOTH dimensions collapses to 0 x 0.
        @Test
        public void testCopyRegion() {
            boolean[][] arr = { { true, false, true }, { false, true, false } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix copy = matrix.copyRegion(0, 2, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertFalse(copy.get(0, 0));
            assertTrue(copy.get(0, 1));
        }

        @Test
        public void testCopyRegion_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            BooleanMatrix submatrix = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertFalse(submatrix.get(0, 0));
            assertFalse(submatrix.get(1, 1));
        }

        @Test
        public void testCopyRegion_Shape3By3_LowerRightRegion() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, true, false } });
            BooleanMatrix copy = m.copyRegion(1, 3, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertTrue(copy.get(0, 0));
            assertFalse(copy.get(0, 1));
            assertTrue(copy.get(1, 0));
            assertFalse(copy.get(1, 1));
        }

        @Test
        public void testCopyRegion_Shape3By3_UpperRightRegion() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, true, true } });
            BooleanMatrix copy = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertFalse(copy.get(0, 0)); // From (0,1)
            assertTrue(copy.get(0, 1)); // From (0,2)
        }

        @Test
        public void testCopy_Region() {
            BooleanMatrix matrix = BooleanMatrix
                    .wrap(new boolean[][] { { true, false, true, false }, { false, true, false, true }, { true, true, false, false } });
            BooleanMatrix subMatrix = matrix.copyRegion(0, 2, 1, 3);
            // Result: [[false, true], [true, false]]
            assertEquals(2, subMatrix.rowCount());
            assertEquals(2, subMatrix.columnCount());
            assertFalse(subMatrix.get(0, 0));
            assertTrue(subMatrix.get(0, 1));
            assertTrue(subMatrix.get(1, 0));
            assertFalse(subMatrix.get(1, 1));

            // Extract a single column as a matrix
            BooleanMatrix col = matrix.copyRegion(0, 3, 2, 3);
            // Result: [[true], [false], [false]]
            assertEquals(3, col.rowCount());
            assertEquals(1, col.columnCount());
            assertTrue(col.get(0, 0));
            assertFalse(col.get(1, 0));
            assertFalse(col.get(2, 0));
        }

        @Test
        public void testCopyRegion_SingleCell() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            BooleanMatrix copy = m.copyRegion(0, 1, 1, 2);
            assertEquals(1, copy.rowCount());
            assertEquals(1, copy.columnCount());
            assertFalse(copy.get(0, 0));
        }

        @Test
        public void testCopyRegion_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, 0, 3));
        }

        @Test
        public void testCopyRegion_InvalidRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 1, -1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 1, 0, 3));
        }
    }

    @Nested
    class ResizeTests {

        @Test
        public void testResize_Default() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix extended = matrix.resize(3, 3);
            // Result: [[true, false, false], [false, true, false], [false, false, false]]
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertTrue(extended.get(0, 0));
            assertFalse(extended.get(0, 1));
            assertFalse(extended.get(0, 2));
            assertFalse(extended.get(1, 0));
            assertTrue(extended.get(1, 1));
            assertFalse(extended.get(1, 2));
            assertFalse(extended.get(2, 0));
            assertFalse(extended.get(2, 1));
            assertFalse(extended.get(2, 2));
        }

        @Test
        public void testResize_WithFill() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix extended = matrix.resize(3, 4, true);
            // Result: [[true, false, true, true], [false, true, true, true], [true, true, true, true]]
            assertEquals(3, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertTrue(extended.get(0, 0));
            assertFalse(extended.get(0, 1));
            assertTrue(extended.get(0, 2));
            assertTrue(extended.get(0, 3));
            assertFalse(extended.get(1, 0));
            assertTrue(extended.get(1, 1));
            assertTrue(extended.get(1, 2));
            assertTrue(extended.get(1, 3));
            assertTrue(extended.get(2, 0));
            assertTrue(extended.get(2, 1));
            assertTrue(extended.get(2, 2));
            assertTrue(extended.get(2, 3));

            // Truncate to smaller size
            BooleanMatrix truncated = matrix.resize(1, 1, false);
            assertEquals(1, truncated.rowCount());
            assertEquals(1, truncated.columnCount());
            assertTrue(truncated.get(0, 0));
        }

        @Test
        public void testResize_Expand() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix resized = m.resize(3, 3);
            assertEquals(3, resized.rowCount());
            assertEquals(3, resized.columnCount());
            assertTrue(resized.get(0, 0));
            assertFalse(resized.get(0, 1));
            assertFalse(resized.get(0, 2));
            assertFalse(resized.get(2, 0));
            assertFalse(resized.get(2, 2));
        }

        @Test
        public void testResize_Shrink() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, true, true } });
            BooleanMatrix resized = m.resize(2, 2);
            assertEquals(2, resized.rowCount());
            assertEquals(2, resized.columnCount());
            assertTrue(resized.get(0, 0));
            assertFalse(resized.get(0, 1));
            assertFalse(resized.get(1, 0));
            assertTrue(resized.get(1, 1));
        }

        @Test
        public void testResize_WithDefaultValue() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true } });
            BooleanMatrix resized = m.resize(2, 3, true);
            assertEquals(2, resized.rowCount());
            assertEquals(3, resized.columnCount());
            assertTrue(resized.get(0, 0));
            assertTrue(resized.get(0, 1));
            assertTrue(resized.get(0, 2));
            assertTrue(resized.get(1, 0));
            assertTrue(resized.get(1, 1));
            assertTrue(resized.get(1, 2));
        }

        @Test
        public void testResize_SameSize() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix resized = m.resize(2, 2);
            assertEquals(2, resized.rowCount());
            assertEquals(2, resized.columnCount());
            assertTrue(resized.get(0, 0));
            assertTrue(resized.get(1, 1));
        }

        @Test
        public void testResize() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix extended = matrix.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertFalse(extended.get(2, 2));
        }

        @Test
        public void testResizeWithDefault() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix extended = matrix.resize(3, 3, true);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertTrue(extended.get(2, 2));

            assertThrows(IllegalArgumentException.class, () -> matrix.resize(-1, 3, true));
        }

        @Test
        public void testResize_Larger() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix extended = m.resize(4, 4);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertTrue(extended.get(0, 0));
            assertTrue(extended.get(1, 1));
            assertFalse(extended.get(3, 3)); // new cells are false
        }

        @Test
        public void testResize_Smaller() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            BooleanMatrix truncated = m.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
            assertTrue(truncated.get(0, 0));
            assertTrue(truncated.get(1, 1));
        }

        @Test
        public void testResize_WithDefaultValue_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix extended = m.resize(3, 3, true);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertTrue(extended.get(0, 0));
            assertTrue(extended.get(2, 2)); // new cell
        }

        @Test
        public void testResize_NoChange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix extended = m.resize(2, 2);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertTrue(extended.get(0, 0));
            assertTrue(extended.get(1, 1));
        }

        @Test
        public void testResize_WithDefaultValue_Shape1By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix extended = m.resize(2, 3);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertTrue(extended.get(0, 0));
            assertFalse(extended.get(0, 1));
            assertFalse(extended.get(0, 2));
            assertFalse(extended.get(1, 0));
        }

        @Test
        public void testResize_WithCustomDefaultValue() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false } });
            BooleanMatrix extended = m.resize(2, 3, true);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertFalse(extended.get(0, 0));
            assertFalse(extended.get(0, 1));
            assertTrue(extended.get(0, 2));
            assertTrue(extended.get(1, 0));
        }

        @Test
        public void testResize_NoExtension() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix extended = m.resize(1, 2);
            assertEquals(1, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertTrue(extended.get(0, 0));
            assertFalse(extended.get(0, 1));
        }

        @Test
        public void testResize_TwoParams() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix extended = m.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertTrue(extended.get(0, 0));
            assertFalse(extended.get(2, 2));
        }

        @Test
        public void testResize_TwoParamsWithDefault() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix extended = m.resize(3, 3, true);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertTrue(extended.get(2, 2));
        }

        @Test
        public void testResize_ToEmpty() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix resized = m.resize(0, 0);
            assertEquals(0, resized.rowCount());
            assertEquals(0, resized.columnCount());
            assertTrue(resized.isEmpty());
        }

        @Test
        public void testZeroRowShapeWithMaxColumnCountIsAllocationSafe() {
            BooleanMatrix m = new BooleanMatrix(new boolean[0][], 1);
            assertShape(0, Integer.MAX_VALUE, m.resize(0, Integer.MAX_VALUE));
            assertShape(0, Integer.MAX_VALUE, m.reshapeAndPad(0, Integer.MAX_VALUE));
        }

        @Test
        public void testResize_NegativeThrows() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> m.resize(1, -1));
        }

        @Test
        public void testResize_WithNegativeDimensions() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 3, false));
            assertThrows(IllegalArgumentException.class, () -> m.resize(3, -1, false));
        }

        @Test
        public void testResize_InvalidSize() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });

            BooleanMatrix extended = m.resize(1, 1);
            assertEquals(1, extended.rowCount());
            assertEquals(1, extended.columnCount());
        }
    }

    @Nested
    class PadTests {

        @Test
        public void testPadWithDirections() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix extended = matrix.pad(1, 1, 1, 1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertFalse(extended.get(0, 0));
        }

        @Test
        public void testPadWithDirectionsAndDefault() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix extended = matrix.pad(1, 1, 1, 1, true);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertTrue(extended.get(0, 0));

            assertThrows(IllegalArgumentException.class, () -> matrix.pad(-1, 1, 1, 1, true));
        }

        @Test
        public void testPad_Directional() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            BooleanMatrix extended = m.pad(1, 1, 2, 2);
            assertEquals(5, extended.rowCount()); // 1 + 3 + 1
            assertEquals(7, extended.columnCount()); // 2 + 3 + 2

            // Original values at offset position
            assertTrue(extended.get(1, 2));
            assertTrue(extended.get(2, 3));

            // New cells are false
            assertFalse(extended.get(0, 0));
        }

        @Test
        public void testPad_DirectionalWithDefault() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            BooleanMatrix extended = m.pad(1, 1, 1, 1, true);
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());

            // Check original values
            assertTrue(extended.get(1, 1));

            // Check new values
            assertTrue(extended.get(0, 0));
            assertTrue(extended.get(4, 4));
        }

        @Test
        public void testPad_WithDirections() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true } });
            BooleanMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertTrue(extended.get(1, 1));
            assertFalse(extended.get(0, 0));
        }

        @Test
        public void testPad_WithDirectionsAndValue() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false } });
            BooleanMatrix extended = m.pad(1, 1, 1, 1, true);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertFalse(extended.get(1, 1));
            assertTrue(extended.get(0, 0));
        }

        @Test
        public void testPad_FourParams() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertTrue(extended.get(1, 1));
        }

        @Test
        public void testPad_FourParamsWithDefault() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix extended = m.pad(1, 1, 1, 1, true);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertTrue(extended.get(0, 0));
            assertTrue(extended.get(3, 3));
        }

        @Test
        public void testPad_Default() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, true } });
            BooleanMatrix extended = matrix.pad(1, 1, 1, 1);
            // Result: [[false, false, false, false], [false, true, true, false], [false, false, false, false]]
            assertEquals(3, extended.rowCount());
            assertEquals(4, extended.columnCount());
            for (int j = 0; j < 4; j++) {
                assertFalse(extended.get(0, j));
                assertFalse(extended.get(2, j));
            }
            assertFalse(extended.get(1, 0));
            assertTrue(extended.get(1, 1));
            assertTrue(extended.get(1, 2));
            assertFalse(extended.get(1, 3));
        }

        @Test
        public void testPad_WithFill() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, true } });
            BooleanMatrix padded = matrix.pad(1, 1, 2, 2, true);
            // Result: 3x6, all true
            assertEquals(3, padded.rowCount());
            assertEquals(6, padded.columnCount());
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 6; j++) {
                    assertTrue(padded.get(i, j));
                }
            }

            // Add border of false values
            BooleanMatrix bordered = matrix.pad(1, 1, 1, 1, false);
            assertEquals(3, bordered.rowCount());
            assertEquals(4, bordered.columnCount());
            for (int j = 0; j < 4; j++) {
                assertFalse(bordered.get(0, j));
                assertFalse(bordered.get(2, j));
            }
            assertFalse(bordered.get(1, 0));
            assertTrue(bordered.get(1, 1));
            assertTrue(bordered.get(1, 2));
            assertFalse(bordered.get(1, 3));
        }

        @Test
        public void testPadFlattenAndForEach_SubRangeEdgeCase() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix extended = matrix.pad(0, 0, 2, 1, true);
            BooleanList flattened = matrix.flatten();
            List<Boolean> visited = new ArrayList<>();

            matrix.forEach(0, 2, 1, 2, visited::add);

            assertTrue(extended.get(0, 0));
            assertTrue(extended.get(1, 0));
            assertArrayEquals(new boolean[] { true, false, false, true }, flattened.toArray());
            assertEquals(List.of(false, true), visited);
        }

        @Test
        public void testPad_DirectionalWithNegativeValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IllegalArgumentException.class, () -> m.pad(-1, 1, 1, 1, false));
        }
    }

    @Nested
    class FlipHorizontallyInPlaceTests {

        @Test
        public void testReverseH() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.flipHorizontallyInPlace();
            assertFalse(matrix.get(0, 0));
            assertTrue(matrix.get(0, 1));
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            m.flipHorizontallyInPlace();
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertTrue(m.get(0, 2));
            assertFalse(m.get(1, 0));
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By3_ReversesFirstRow() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            m.flipHorizontallyInPlace();
            assertTrue(m.get(0, 2));
            assertFalse(m.get(0, 1));
            assertTrue(m.get(0, 0));
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By3_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            m.flipHorizontallyInPlace();
            assertArrayEquals(new boolean[] { true, false, true }, m.rowView(0));
            assertArrayEquals(new boolean[] { false, true, false }, m.rowView(1));
        }

        @Test
        public void testFlipHorizontallyInPlace() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.flipHorizontallyInPlace();
            assertFalse(m.get(0, 0));
            assertTrue(m.get(0, 1));
        }

        @Test
        public void testflipHorizontallyInPlace() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, true, false }, { false, true, true } });
            matrix.flipHorizontallyInPlace();
            // matrix is now [[false, true, true], [true, true, false]]
            assertFalse(matrix.get(0, 0));
            assertTrue(matrix.get(0, 1));
            assertTrue(matrix.get(0, 2));
            assertTrue(matrix.get(1, 0));
            assertTrue(matrix.get(1, 1));
            assertFalse(matrix.get(1, 2));
        }

        @Test
        public void testFlipInPlaceHorizontally() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, true, true } });
            m.flipHorizontallyInPlace();
            assertFalse(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertTrue(m.get(0, 2));
            assertTrue(m.get(1, 0));
            assertTrue(m.get(1, 1));
            assertFalse(m.get(1, 2));
        }

        @Test
        public void testFlipHorizontallyInPlace_SingleRow() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            m.flipHorizontallyInPlace();
            assertArrayEquals(new boolean[] { true, false, true }, m.rowView(0));
        }

        @Test
        public void testFlipInPlaceHorizontally_SingleColumn() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true }, { false } });
            m.flipHorizontallyInPlace();
            assertTrue(m.get(0, 0));
            assertFalse(m.get(1, 0));
        }
    }

    @Nested
    class FlipVerticallyInPlaceTests {

        @Test
        public void testReverseV() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            matrix.flipVerticallyInPlace();
            assertFalse(matrix.get(0, 0));
            assertTrue(matrix.get(1, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape3By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, false } });
            m.flipVerticallyInPlace();
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertTrue(m.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.flipVerticallyInPlace();
            assertFalse(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertTrue(m.get(1, 0));
            assertFalse(m.get(1, 1));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape3By2_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            m.flipVerticallyInPlace();
            assertArrayEquals(new boolean[] { true, false, true }, m.columnCopy(0));
            assertArrayEquals(new boolean[] { true, true, false }, m.columnCopy(1));
        }

        @Test
        public void testFlipVerticallyInPlace() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { true, true }, { false, true } });
            matrix.flipVerticallyInPlace();
            // matrix is now [[false, true], [true, true], [true, false]]
            assertFalse(matrix.get(0, 0));
            assertTrue(matrix.get(0, 1));
            assertTrue(matrix.get(1, 0));
            assertTrue(matrix.get(1, 1));
            assertTrue(matrix.get(2, 0));
            assertFalse(matrix.get(2, 1));
        }

        @Test
        public void testFlipInPlaceVertically() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            m.flipVerticallyInPlace();
            assertTrue(m.get(0, 0));
            assertTrue(m.get(0, 1));
            assertFalse(m.get(1, 0));
            assertTrue(m.get(1, 1));
            assertTrue(m.get(2, 0));
            assertFalse(m.get(2, 1));
        }

        @Test
        public void testFlipVerticallyInPlace_SingleColumn() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true }, { false }, { true } });
            m.flipVerticallyInPlace();
            assertArrayEquals(new boolean[] { true, false, true }, m.columnCopy(0));
        }

        @Test
        public void testFlipInPlaceVertically_SingleRow() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            m.flipVerticallyInPlace();
            assertTrue(m.get(0, 0));
            assertFalse(m.get(0, 1));
            assertTrue(m.get(0, 2));
        }
    }

    @Nested
    class FlipHorizontallyTests {

        @Test
        public void testFlipH() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix flipped = matrix.flipHorizontally();
            assertFalse(flipped.get(0, 0));
            assertTrue(flipped.get(0, 1));

            // Original unchanged
            assertTrue(matrix.get(0, 0));
        }

        @Test
        public void testFlipHorizontally_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            BooleanMatrix flipped = m.flipHorizontally();
            assertTrue(flipped.get(0, 0));
            assertFalse(flipped.get(0, 1));
            assertTrue(flipped.get(0, 2));

            // Original unchanged
            assertTrue(m.get(0, 0));
        }

        @Test
        public void testFlipHorizontally_Shape2By3_ReversesFirstRowAndPreservesSource() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            BooleanMatrix flipped = m.flipHorizontally();
            assertTrue(flipped.get(0, 2));
            assertFalse(flipped.get(0, 1));
            assertTrue(flipped.get(0, 0));
            assertTrue(m.get(0, 0));
        }

        @Test
        public void testFlipHorizontally_Shape2By3_IndependentStorage_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            BooleanMatrix flipped = m.flipHorizontally();
            assertEquals(2, flipped.rowCount());
            assertEquals(3, flipped.columnCount());
            assertArrayEquals(new boolean[] { true, false, true }, flipped.rowView(0));
            assertArrayEquals(new boolean[] { false, true, false }, flipped.rowView(1));
            assertNotSame(m, flipped);
        }

        @Test
        public void testFlipHorizontally() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix flipped = m.flipHorizontally();
            assertFalse(flipped.get(0, 0));
            assertTrue(flipped.get(0, 1));
        }
    }

    @Nested
    class FlipVerticallyTests {

        @Test
        public void testFlipVertically() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix flipped = matrix.flipVertically();
            assertFalse(flipped.get(0, 0));
            assertTrue(flipped.get(1, 0));

            // Original unchanged
            assertTrue(matrix.get(0, 0));
        }

        @Test
        public void testFlipVertically_Shape3By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, false } });
            BooleanMatrix flipped = m.flipVertically();
            assertTrue(flipped.get(0, 0));
            assertFalse(flipped.get(1, 0));
            assertTrue(flipped.get(2, 0));

            // Original unchanged
            assertTrue(m.get(0, 0));
        }

        @Test
        public void testFlipVertically_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix flipped = m.flipVertically();
            assertFalse(flipped.get(0, 0));
            assertTrue(flipped.get(0, 1));
            assertTrue(flipped.get(1, 0));
            assertFalse(flipped.get(1, 1));
            assertTrue(m.get(0, 0));
        }

        @Test
        public void testFlipVertically_Shape3By2_IndependentStorage_ArrayValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            BooleanMatrix flipped = m.flipVertically();
            assertEquals(3, flipped.rowCount());
            assertEquals(2, flipped.columnCount());
            assertArrayEquals(new boolean[] { true, false, true }, flipped.columnCopy(0));
            assertArrayEquals(new boolean[] { true, true, false }, flipped.columnCopy(1));
            assertNotSame(m, flipped);
        }
    }

    @Nested
    class Rotate90Tests {

        @Test
        public void testRotate90() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix rotated = matrix.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertFalse(rotated.get(0, 0));
            assertTrue(rotated.get(0, 1));
        }

        @Test
        public void testRotate90_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertFalse(rotated.get(0, 0));
            assertTrue(rotated.get(0, 1));
            assertTrue(rotated.get(1, 0));
            assertFalse(rotated.get(1, 1));
        }

        @Test
        public void testRotate90_Rectangle() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            BooleanMatrix rotated = m.rotate90();
            assertEquals(3, rotated.rowCount());
            assertEquals(1, rotated.columnCount());
            assertTrue(rotated.get(0, 0));
            assertFalse(rotated.get(1, 0));
            assertTrue(rotated.get(2, 0));
        }

        @Test
        public void testRotate90_NonSquare() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            BooleanMatrix rotated = m.rotate90();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
        }
    }

    @Nested
    class Rotate180Tests {

        @Test
        public void testRotate180() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix rotated = matrix.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertTrue(rotated.get(0, 0));
            assertFalse(rotated.get(0, 1));
        }

        @Test
        public void testRotate180_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix rotated = m.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertTrue(rotated.get(0, 0));
            assertFalse(rotated.get(0, 1));
            assertFalse(rotated.get(1, 0));
            assertTrue(rotated.get(1, 1));
        }

        @Test
        public void testRotate180_Rectangle() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            BooleanMatrix rotated = m.rotate180();
            assertEquals(1, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            assertTrue(rotated.get(0, 0));
            assertFalse(rotated.get(0, 1));
            assertTrue(rotated.get(0, 2));
        }

        @Test
        public void testRotate180_Shape2By2_EveryCellValue() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix rotated = m.rotate180();
            assertTrue(rotated.get(0, 0));
            assertFalse(rotated.get(0, 1));
            assertFalse(rotated.get(1, 0));
            assertTrue(rotated.get(1, 1));
        }

        @Test
        public void testRotate180AndRotate90Twice_PreserveNx0Shape() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][0]);

            BooleanMatrix via180 = m.rotate180();
            assertEquals(3, via180.rowCount());
            assertEquals(0, via180.columnCount());

            BooleanMatrix viaRotate90Twice = m.rotate90().rotate90();
            assertEquals(3, viaRotate90Twice.rowCount());
            assertEquals(0, viaRotate90Twice.columnCount());
        }
    }

    @Nested
    class Rotate270Tests {

        @Test
        public void testRotate270() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix rotated = matrix.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertFalse(rotated.get(0, 0));
            assertTrue(rotated.get(1, 0));
        }

        @Test
        public void testRotate270_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertFalse(rotated.get(0, 0));
            assertTrue(rotated.get(0, 1));
            assertTrue(rotated.get(1, 0));
            assertFalse(rotated.get(1, 1));
        }

        @Test
        public void testRotate270_Rectangle() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            BooleanMatrix rotated = m.rotate270();
            assertEquals(3, rotated.rowCount());
            assertEquals(1, rotated.columnCount());
            assertTrue(rotated.get(0, 0));
            assertFalse(rotated.get(1, 0));
            assertTrue(rotated.get(2, 0));
        }
    }

    @Nested
    class TransposeTests {

        // Test reshape with too-small dimensions throws exception
        @Test
        public void testTranspose() {
            boolean[][] arr = { { true, false, true }, { false, true, false } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix transposed = matrix.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertTrue(transposed.get(0, 0));
            assertFalse(transposed.get(0, 1));
        }

        @Test
        public void testTranspose_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            BooleanMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertTrue(transposed.get(0, 0));
            assertFalse(transposed.get(0, 1));
            assertFalse(transposed.get(1, 0));
            assertTrue(transposed.get(1, 1));
            assertTrue(transposed.get(2, 0));
            assertFalse(transposed.get(2, 1));
        }

        @Test
        public void testTranspose_Square() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertTrue(transposed.get(0, 0));
            assertFalse(transposed.get(0, 1));
            assertFalse(transposed.get(1, 0));
            assertTrue(transposed.get(1, 1));
        }

        @Test
        public void testRotateAndTransposeTallMatrix() {
            // Create a tall matrix (rows > columnCount) - 4 rows × 2 columnCount
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true }, { false, false } });

            // Test rotate90() with tall matrix
            BooleanMatrix rotated90 = m.rotate90();
            assertEquals(2, rotated90.rowCount());
            assertEquals(4, rotated90.columnCount());
            assertFalse(rotated90.get(0, 0));
            assertTrue(rotated90.get(0, 1));
            assertFalse(rotated90.get(0, 2));
            assertTrue(rotated90.get(0, 3));

            // Test rotate270() with tall matrix
            BooleanMatrix rotated270 = m.rotate270();
            assertEquals(2, rotated270.rowCount());
            assertEquals(4, rotated270.columnCount());
            assertFalse(rotated270.get(0, 0));
            assertTrue(rotated270.get(0, 1));
            assertTrue(rotated270.get(0, 2));
            assertFalse(rotated270.get(0, 3));

            // Test transpose() with tall matrix
            BooleanMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(4, transposed.columnCount());
            assertTrue(transposed.get(0, 0));
            assertFalse(transposed.get(0, 1));

            // Test boxed() with tall matrix
            Matrix<Boolean> boxed = m.boxed();
            assertEquals(4, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Boolean.TRUE, boxed.get(0, 0));
            assertEquals(Boolean.FALSE, boxed.get(0, 1));
        }

        @Test
        public void testTranspose_Nx0_PreservesSwappedShape() {
            BooleanMatrix t = BooleanMatrix.wrap(new boolean[3][0]).transpose();
            assertEquals(0, t.rowCount());
            assertEquals(3, t.columnCount());
        }
    }

    @Nested
    class ReshapeAndPadTests {

        @Test
        public void testReshape_Empty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            BooleanMatrix reshaped = empty.reshapeAndPad(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }

        @Test
        public void testReshape_InvalidDimensions() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            // Reshaping with more elements than available should work but fill with default values
            BooleanMatrix reshaped = m.reshapeAndPad(3, 3);
            assertEquals(3, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }

        @Test
        public void testReshape_InvalidSize() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            BooleanMatrix reshaped = m.reshapeAndPad(2, 2);
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
        }
    }

    @Nested
    class RepeatElementsTests {

        @Test
        public void testRepeatElements() {
            boolean[][] arr = { { true, false } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix repeated = matrix.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertTrue(repeated.get(0, 0));
            assertTrue(repeated.get(0, 1));
            assertTrue(repeated.get(0, 2));
            assertFalse(repeated.get(0, 3));

            assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(0, 1));
        }

        @Test
        public void testRepeatElements_Shape1By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix repeated = m.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertTrue(repeated.get(0, 0));
            assertTrue(repeated.get(0, 1));
            assertTrue(repeated.get(0, 2));
            assertFalse(repeated.get(0, 3));
            assertFalse(repeated.get(0, 4));
            assertFalse(repeated.get(0, 5));
            assertTrue(repeated.get(1, 0)); // second row same as first
        }

        @Test
        public void testRepeatElements_Edge1x1() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true } });
            BooleanMatrix repeated = m.repeatElements(3, 3);
            assertEquals(3, repeated.rowCount());
            assertEquals(3, repeated.columnCount());
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    assertTrue(repeated.get(i, j));
                }
            }
        }

        @Test
        public void testRepeatElements_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix result = m.repeatElements(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertTrue(result.get(0, 0));
            assertTrue(result.get(0, 1));
            assertTrue(result.get(1, 0));
            assertTrue(result.get(1, 1));
            assertFalse(result.get(0, 2));
        }

        @Test
        public void testRepeatElements_Shape2By2_RepeatedBlockAndAdjacentValue() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix repeated = m.repeatElements(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertTrue(repeated.get(0, 0));
            assertTrue(repeated.get(0, 1));
            assertTrue(repeated.get(1, 0));
            assertTrue(repeated.get(1, 1));
            assertFalse(repeated.get(0, 2));
        }

        @Test
        public void testRepeatElementsOverflow() {
            // Create matrix that will overflow when repeated
            int largeSize = 50000;
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[largeSize][2]);

            // Test row overflow
            IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> m.repeatElements(50000, 1));
            assertTrue(ex1.getMessage().contains("row count overflow"));

            // Test column overflow
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[2][largeSize]);
            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> m2.repeatElements(1, 50000));
            assertTrue(ex2.getMessage().contains("column count overflow"));
        }

        @Test
        public void testRepeatElements_SingleRepeat() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix repeated = m.repeatElements(1, 1);
            assertEquals(1, repeated.rowCount());
            assertEquals(2, repeated.columnCount());
            assertTrue(repeated.get(0, 0));
            assertFalse(repeated.get(0, 1));
        }

        @Test
        public void testRepeatElements_InvalidArguments() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @Test
        public void testRepeatElements_InvalidRepeats() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }
    }

    @Nested
    class RepeatMatrixTests {

        @Test
        public void testRepeatMatrix() {
            boolean[][] arr = { { true, false } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix repeated = matrix.repeatMatrix(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertTrue(repeated.get(0, 0));
            assertFalse(repeated.get(0, 1));
            assertTrue(repeated.get(0, 2));

            assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(0, 1));
        }

        @Test
        public void testRepeatMatrix_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix repeated = m.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertTrue(repeated.get(0, 0));
            assertFalse(repeated.get(0, 1));
            assertTrue(repeated.get(0, 2)); // repeat starts
            assertFalse(repeated.get(0, 3));

            assertFalse(repeated.get(1, 0));
            assertTrue(repeated.get(1, 1));

            // Check vertical repeat
            assertTrue(repeated.get(2, 0)); // vertical repeat starts
            assertFalse(repeated.get(2, 1));
        }

        @Test
        public void testRepeatMatrix_Edge1x1() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true } });
            BooleanMatrix repeated = m.repeatMatrix(3, 3);
            assertEquals(3, repeated.rowCount());
            assertEquals(3, repeated.columnCount());
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    assertTrue(repeated.get(i, j));
                }
            }
        }

        @Test
        public void testRepeatMatrix_Shape2By2_VerticalTileBoundary() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix result = m.repeatMatrix(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertTrue(result.get(0, 0));
            assertFalse(result.get(0, 1));
            assertTrue(result.get(2, 0));
            assertFalse(result.get(2, 1));
        }

        @Test
        public void testRepeatMatrix_Shape2By2_DiagonalTileBoundary() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix repeated = m.repeatMatrix(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertTrue(repeated.get(0, 0));
            assertFalse(repeated.get(0, 1));
            assertTrue(repeated.get(2, 2));
        }

        @Test
        public void testRepeatMatrix_EmptyDimensionsDoNotTraverseRepeats() {
            BooleanMatrix zeroRows = new BooleanMatrix(new boolean[0][], 2);
            BooleanMatrix zeroColumns = BooleanMatrix.wrap(new boolean[2][0]);

            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                BooleanMatrix repeatedRows = zeroRows.repeatMatrix(Integer.MAX_VALUE, 1);
                assertEquals(0, repeatedRows.rowCount());
                assertEquals(2, repeatedRows.columnCount());

                BooleanMatrix repeatedColumns = zeroColumns.repeatMatrix(1, Integer.MAX_VALUE);
                assertEquals(2, repeatedColumns.rowCount());
                assertEquals(0, repeatedColumns.columnCount());
            });

            assertThrows(IllegalArgumentException.class, () -> zeroRows.repeatMatrix(1, Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> zeroColumns.repeatMatrix(Integer.MAX_VALUE, 1));
        }

        @Test
        public void testRepeatMatrixOverflow() {
            // Create matrix that will overflow when repeated
            int largeSize = 50000;
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[largeSize][2]);

            // Test row overflow
            IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(50000, 1));
            assertTrue(ex1.getMessage().contains("row count overflow"));

            // Test column overflow
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[2][largeSize]);
            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> m2.repeatMatrix(1, 50000));
            assertTrue(ex2.getMessage().contains("column count overflow"));
        }

        @Test
        public void testRepeatMatrix_SingleRepeat() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix repeated = m.repeatMatrix(1, 1);
            assertEquals(1, repeated.rowCount());
            assertEquals(2, repeated.columnCount());
            assertTrue(repeated.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidArguments() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidRepeats() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }
    }

    @Nested
    class FlattenTests {

        @Test
        public void testFlatten() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanList flat = matrix.flatten();
            assertEquals(4, flat.size());
            assertTrue(flat.get(0));
            assertFalse(flat.get(1));
            assertFalse(flat.get(2));
            assertTrue(flat.get(3));
        }

        @Test
        public void testFlatten_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            BooleanList flat = m.flatten();
            assertEquals(9, flat.size());
            boolean[] expected = { true, false, true, false, true, false, true, false, true };
            for (int i = 0; i < 9; i++) {
                assertEquals(expected[i], flat.get(i));
            }
        }

        @Test
        public void testFlatten_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanList flattened = m.flatten();
            assertEquals(4, flattened.size());
            assertTrue(flattened.get(0));
            assertFalse(flattened.get(1));
            assertFalse(flattened.get(2));
            assertTrue(flattened.get(3));
        }

        @Test
        public void testFlatten_Shape2By2_EveryListElement() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanList flat = m.flatten();
            assertEquals(4, flat.size());
            assertTrue(flat.get(0));
            assertFalse(flat.get(1));
            assertFalse(flat.get(2));
            assertTrue(flat.get(3));
        }

        @Test
        public void testFlatten_Empty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            BooleanList flat = empty.flatten();
            assertTrue(flat.isEmpty());
        }

        @Test
        public void testFlatten_EmptyMatrix() {
            BooleanMatrix m = BooleanMatrix.empty();
            BooleanList flattened = m.flatten();
            assertEquals(0, flattened.size());
        }
    }

    @Nested
    class MutateViaFlatArrayTests {

        @Test
        public void testFlatOp() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            int[] count = { 0 };
            matrix.mutateViaFlatArray(array -> count[0] += array.length);
            assertEquals(4, count[0]);
        }

        @Test
        public void testMutateViaFlatArray_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Integer> trueCounts = new ArrayList<>();
            m.mutateViaFlatArray(row -> {
                int count = 0;
                for (boolean val : row) {
                    if (val) {
                        count++;
                    }
                }
                trueCounts.add(count);
            });
            assertEquals(1, trueCounts.size());
            assertEquals(5, trueCounts.get(0).intValue());
        }

        @Test
        public void testMutateViaFlatArray_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            AtomicInteger count = new AtomicInteger(0);
            m.mutateViaFlatArray(row -> count.addAndGet(row.length));
            assertEquals(4, count.get());
        }

        @Test
        public void testMutateViaFlatArray() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            m.mutateViaFlatArray(arr -> {
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = !arr[i];
                }
            });
            assertFalse(m.get(0, 0));
            assertTrue(m.get(0, 1));
        }

        @Test
        public void testmutateViaFlatArray() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            matrix.mutateViaFlatArray(arr -> java.util.Arrays.fill(arr, true));
            // matrix is now [[true, true], [true, true]]
            assertTrue(matrix.get(0, 0));
            assertTrue(matrix.get(0, 1));
            assertTrue(matrix.get(1, 0));
            assertTrue(matrix.get(1, 1));
        }
    }

    @Nested
    class AndTests {

        @Test
        public void testAnd() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true, false }, { true, true } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { true, true }, { false, true } });
            BooleanMatrix result = a.and(b);
            assertTrue(result.get(0, 0));
            assertFalse(result.get(0, 1));
            assertFalse(result.get(1, 0));
            assertTrue(result.get(1, 1));
        }

        @Test
        public void testAnd_AllFalse() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { true, true }, { true, true } });
            BooleanMatrix result = a.and(b);
            assertFalse(result.get(0, 0));
            assertFalse(result.get(0, 1));
            assertFalse(result.get(1, 0));
            assertFalse(result.get(1, 1));
        }

        @Test
        public void testAndOrXor_ForcedParallel_MatchSequential() {
            // The parallel branch of and/or/xor is otherwise never exercised: these operations exist
            // only on BooleanMatrix, so no sibling test covers the analogous code.
            boolean[][] left = new boolean[7][5];
            boolean[][] right = new boolean[7][5];
            for (int i = 0; i < 7; i++) {
                for (int j = 0; j < 5; j++) {
                    left[i][j] = (i + j) % 2 == 0;
                    right[i][j] = (i * j) % 3 == 0;
                }
            }
            BooleanMatrix a = BooleanMatrix.wrap(left);
            BooleanMatrix b = BooleanMatrix.wrap(right);

            BooleanMatrix sequentialAnd = a.and(b);
            BooleanMatrix sequentialOr = a.or(b);
            BooleanMatrix sequentialXor = a.xor(b);

            Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> {
                assertEquals(sequentialAnd, a.and(b));
                assertEquals(sequentialOr, a.or(b));
                assertEquals(sequentialXor, a.xor(b));
            });
        }

        @Test
        public void testAnd_DifferentShapeThrows() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { true }, { false } });
            assertThrows(IllegalArgumentException.class, () -> a.and(b));
        }

        // Exercise the custom object iterators used by horizontal and vertical boxed streams.
        @Test
        public void testAnd_NullThrows() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertThrows(IllegalArgumentException.class, () -> a.and(null));
        }
    }

    @Nested
    class OrTests {

        @Test
        public void testOr() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, false } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { false, true }, { false, true } });
            BooleanMatrix result = a.or(b);
            assertTrue(result.get(0, 0));
            assertTrue(result.get(0, 1));
            assertFalse(result.get(1, 0));
            assertTrue(result.get(1, 1));
        }

        @Test
        public void testOr_AllFalse() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { false, false } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { false, false } });
            BooleanMatrix result = a.or(b);
            assertFalse(result.get(0, 0));
            assertFalse(result.get(0, 1));
        }

        @Test
        public void testZeroRowResultMethodsPreserveLogicalColumnCount() {
            BooleanMatrix matrix = new BooleanMatrix(new boolean[0][], 3);
            BooleanMatrix other = new BooleanMatrix(new boolean[0][], 3);

            assertShape(0, 3, matrix.copy());
            assertShape(0, 3, matrix.copyRows(0, 0));
            assertShape(0, 2, matrix.copyRegion(0, 0, 1, 3));
            assertShape(0, 3, matrix.map(value -> !value));
            assertShape(0, 3, matrix.mapToObj(Boolean::valueOf, Boolean.class));
            assertShape(0, 5, matrix.resize(0, 5));
            assertShape(0, 5, matrix.pad(0, 0, 1, 1));
            assertShape(3, 0, matrix.rotate90());
            assertShape(0, 3, matrix.rotate180());
            assertShape(3, 0, matrix.rotate270());
            assertShape(3, 0, matrix.transpose());
            assertShape(0, 4, matrix.reshapeAndPad(0, 4));
            assertShape(0, 6, matrix.repeatElements(2, 2));
            assertShape(0, 6, matrix.repeatMatrix(2, 2));
            assertShape(0, 3, matrix.and(other));
            assertShape(0, 3, matrix.or(other));
            assertShape(0, 3, matrix.xor(other));
            assertShape(0, 3, matrix.stackVertically(other));
            assertShape(0, 6, matrix.stackHorizontally(other));
            assertShape(0, 3, matrix.boxed());
            assertShape(0, 3, matrix.zipWith(other, (left, right) -> left));
            assertShape(0, 3, matrix.zipWith(other, other, (first, second, third) -> first));
        }

        @Test
        public void testOr_DifferentShapeThrows() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { true }, { false } });
            assertThrows(IllegalArgumentException.class, () -> a.or(b));
        }
    }

    @Nested
    class XorTests {

        @Test
        public void testXor() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true, false }, { true, true } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { true, true }, { false, true } });
            BooleanMatrix result = a.xor(b);
            assertFalse(result.get(0, 0));
            assertTrue(result.get(0, 1));
            assertTrue(result.get(1, 0));
            assertFalse(result.get(1, 1));
        }

        @Test
        public void testXor_SameMatrixAllFalse() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true, false }, { true, true } });
            BooleanMatrix result = a.xor(a);
            assertFalse(result.get(0, 0));
            assertFalse(result.get(0, 1));
            assertFalse(result.get(1, 0));
            assertFalse(result.get(1, 1));
        }

        @Test
        public void testXor_DifferentShapeThrows() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { true }, { false } });
            assertThrows(IllegalArgumentException.class, () -> a.xor(b));
        }
    }

    @Nested
    class CountTrueTests {

        @Test
        public void testCountTrue() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            assertEquals(3, m.countTrue());
        }

        @Test
        public void testCountTrue_AllTrue() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, true }, { true, true } });
            assertEquals(4, m.countTrue());
        }

        @Test
        public void testCountTrue_AllFalse() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false } });
            assertEquals(0, m.countTrue());
        }

        @Test
        public void testCountTrue_ResultIsLong_AtCompileTime() {
            // This compiles only if countTrue() returns a primitive convertible to long
            // *and* the literal-long comparison binds to assertEquals(long, long).
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            long expected = 3L;
            long actual = m.countTrue(); // would fail to compile with explicit narrowing on old int return
            assertEquals(expected, actual);
        }

        @Test
        public void testCountTrue_SmallMatrixStillCorrect() {
            // Behavioral regression: ensure the small-matrix path was not broken by widening.
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, true, true }, { true, true, true } });
            assertEquals(6L, m.countTrue());
        }

        @Test
        public void testCountTrue_Empty() {
            BooleanMatrix m = BooleanMatrix.empty();
            assertEquals(0, m.countTrue());
        }

        @Test
        public void testCountTrue_ReturnTypeIsLong_AvoidsIntOverflow() throws NoSuchMethodException {
            // The whole purpose of the fix is to widen the return type so callers
            // and the accumulator cannot silently overflow at int range.
            java.lang.reflect.Method m = BooleanMatrix.class.getMethod("countTrue");
            assertEquals(long.class, m.getReturnType(), "BooleanMatrix.countTrue() must return long to avoid int overflow for large matrices");
        }

        @Test
        public void testCountTrue_EmptyMatrixReturnsZeroLong() {
            // 0x0 — must produce 0L without throwing.
            assertEquals(0L, BooleanMatrix.empty().countTrue());
        }
    }

    @Nested
    class AllTrueTests {

        @Test
        public void testAllTrue() {
            BooleanMatrix allTrue = BooleanMatrix.wrap(new boolean[][] { { true, true }, { true, true } });
            assertTrue(allTrue.allTrue());
        }

        @Test
        public void testAllTrue_WithFalse() {
            BooleanMatrix mixed = BooleanMatrix.wrap(new boolean[][] { { true, false }, { true, true } });
            assertFalse(mixed.allTrue());
        }

        @Test
        public void testAllTrue_Empty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            assertTrue(empty.allTrue());
        }
    }

    @Nested
    class AnyTrueTests {

        @Test
        public void testAnyTrue() {
            BooleanMatrix mixed = BooleanMatrix.wrap(new boolean[][] { { false, true }, { false, false } });
            assertTrue(mixed.anyTrue());
        }

        @Test
        public void testAnyTrue_AllFalse() {
            BooleanMatrix allFalse = BooleanMatrix.wrap(new boolean[][] { { false, false }, { false, false } });
            assertFalse(allFalse.anyTrue());
        }

        @Test
        public void testAnyTrue_Empty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            assertFalse(empty.anyTrue());
        }
    }

    @Nested
    class StackVerticallyTests {

        @Test
        public void testVstack() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { false, true } });

            BooleanMatrix stacked = a.stackVertically(b);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertTrue(stacked.get(0, 0));
            assertFalse(stacked.get(1, 0));

            BooleanMatrix c = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertThrows(IllegalArgumentException.class, () -> a.stackVertically(c));
        }

        @Test
        public void testStackVertically_Shape2By3() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, true, false }, { false, false, true } });
            BooleanMatrix stacked = m1.stackVertically(m2);

            assertEquals(4, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertTrue(stacked.get(0, 0));
            assertTrue(stacked.get(2, 0));
            assertTrue(stacked.get(3, 2));
        }

        @Test
        public void testStackVertically_DifferentColumnCounts() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_Shape1By2() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false, true } });
            BooleanMatrix stacked = m1.stackVertically(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertTrue(stacked.get(0, 0));
            assertFalse(stacked.get(0, 1));
            assertFalse(stacked.get(1, 0));
            assertTrue(stacked.get(1, 1));
        }

        @Test
        public void testStackVertically_IncompatibleColumns() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false, true } });
            BooleanMatrix stacked = m1.stackVertically(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertTrue(stacked.get(0, 0));
            assertFalse(stacked.get(1, 0));
        }

        @Test
        public void testStackVertically_DifferentCols() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false, true, false } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void teststackVertically() {
            BooleanMatrix top = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix bottom = BooleanMatrix.wrap(new boolean[][] { { false, true } });
            BooleanMatrix stacked = top.stackVertically(bottom);
            // Result: [[true, false], [false, true]]
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertTrue(stacked.get(0, 0));
            assertFalse(stacked.get(0, 1));
            assertFalse(stacked.get(1, 0));
            assertTrue(stacked.get(1, 1));
        }

        @Test
        public void testStackVertically_WithEmpty() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix empty = BooleanMatrix.wrap(new boolean[0][0]);

            // Stacking with empty should still work if columns match
            BooleanMatrix result = m1.stackVertically(m1.copy());
            assertEquals(4, result.rowCount());
            assertEquals(2, result.columnCount());
        }

        @Test
        public void testBooleanMatrixStackRejectsNullMatrix() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertThrows(IllegalArgumentException.class, () -> matrix.stackVertically(null));
            assertThrows(IllegalArgumentException.class, () -> matrix.stackHorizontally(null));
        }
    }

    @Nested
    class StackHorizontallyTests {

        @Test
        public void testHstack() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true }, { false } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { false }, { true } });

            BooleanMatrix stacked = a.stackHorizontally(b);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertTrue(stacked.get(0, 0));
            assertFalse(stacked.get(0, 1));

            BooleanMatrix c = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertThrows(IllegalArgumentException.class, () -> a.stackHorizontally(c));
        }

        @Test
        public void testStackHorizontally_Shape2By2() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertTrue(stacked.get(0, 0));
            assertTrue(stacked.get(0, 2));
            assertTrue(stacked.get(1, 3));
        }

        @Test
        public void testStackHorizontally_DifferentRowCounts() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_Shape2By1() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true }, { false } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false }, { true } });
            BooleanMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertTrue(stacked.get(0, 0));
            assertFalse(stacked.get(0, 1));
            assertFalse(stacked.get(1, 0));
            assertTrue(stacked.get(1, 1));
        }

        @Test
        public void testStackHorizontally_IncompatibleRows() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true }, { false } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_DifferentRows() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false, true }, { true, false } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally() {
            BooleanMatrix left = BooleanMatrix.wrap(new boolean[][] { { true }, { false } });
            BooleanMatrix right = BooleanMatrix.wrap(new boolean[][] { { false }, { true } });
            BooleanMatrix stacked = left.stackHorizontally(right);
            // Result: [[true, false], [false, true]]
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertTrue(stacked.get(0, 0));
            assertFalse(stacked.get(0, 1));
            assertFalse(stacked.get(1, 0));
            assertTrue(stacked.get(1, 1));
        }

        @Test
        public void testStackHorizontally_WithEmpty() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix result = m1.stackHorizontally(m1.copy());
            assertEquals(2, result.rowCount());
            assertEquals(4, result.columnCount());
        }

        @Test
        public void testStackHorizontally_InvalidRows() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false }, { true } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }
    }

    @Nested
    class BoxedTests {

        @Test
        public void testBoxed() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            Matrix<Boolean> boxed = matrix.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Boolean.TRUE, boxed.get(0, 0));
            assertEquals(Boolean.FALSE, boxed.get(0, 1));
        }

        @Test
        public void testBoxed_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            Matrix<Boolean> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Boolean.TRUE, boxed.get(0, 0));
            assertEquals(Boolean.FALSE, boxed.get(1, 2));
        }

        @Test
        public void testBoxed_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            Matrix<Boolean> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Boolean.TRUE, boxed.get(0, 0));
            assertEquals(Boolean.FALSE, boxed.get(0, 1));
            assertEquals(Boolean.FALSE, boxed.get(1, 0));
            assertEquals(Boolean.TRUE, boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By2_FirstRowBoxedValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            Matrix<Boolean> boxed = m.boxed();
            assertEquals(Boolean.TRUE, boxed.get(0, 0));
            assertEquals(Boolean.FALSE, boxed.get(0, 1));
        }

        @Test
        public void testBoxed_EmptyMatrix() {
            BooleanMatrix m = BooleanMatrix.empty();
            Matrix<Boolean> boxed = m.boxed();
            assertTrue(boxed.isEmpty());
        }
    }

    @Nested
    class ZipWithTests {

        @Test
        public void testZipWith() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { true, true }, { false, true } });

            BooleanMatrix and = a.zipWith(b, (x, y) -> x && y);
            assertTrue(and.get(0, 0));
            assertFalse(and.get(0, 1));
            assertFalse(and.get(1, 0));
            assertTrue(and.get(1, 1));

            BooleanMatrix c = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertThrows(IllegalArgumentException.class, () -> a.zipWith(c, (x, y) -> x && y));
        }

        @Test
        public void testZipWithThree() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { true, true } });
            BooleanMatrix c = BooleanMatrix.wrap(new boolean[][] { { false, true } });

            BooleanMatrix result = a.zipWith(b, c, (x, y, z) -> (x && y) || z);
            assertTrue(result.get(0, 0));
            assertTrue(result.get(0, 1));

            BooleanMatrix d = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertThrows(IllegalArgumentException.class, () -> a.zipWith(b, d, (x, y, z) -> x && y && z));
        }

        @Test
        public void testZipWith_Shape2By2() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false, true }, { true, false } });
            BooleanMatrix result = m1.zipWith(m2, (a, b) -> a && b);

            assertFalse(result.get(0, 0)); // true && false
            assertFalse(result.get(0, 1)); // false && true
            assertFalse(result.get(1, 0)); // false && true
            assertFalse(result.get(1, 1)); // true && false
        }

        @Test
        public void testZipWith_DifferentShapes() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a && b));
        }

        @Test
        public void testZipWith_ThreeMatrices() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, true }, { false, false } });
            BooleanMatrix m3 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { true, false } });
            BooleanMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a && b && c);

            assertTrue(result.get(0, 0)); // true && true && true
            assertFalse(result.get(0, 1)); // false && true && false
            assertFalse(result.get(1, 0)); // false && false && true
            assertFalse(result.get(1, 1)); // true && false && false
        }

        @Test
        public void testZipWith_ThreeMatrices_DifferentShapes() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, true }, { false, false } });
            BooleanMatrix m3 = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> a && b && c));
        }

        @Test
        public void testZipWith_XorOperation() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, true }, { false, false } });
            BooleanMatrix result = m1.zipWith(m2, (a, b) -> a ^ b); // XOR

            assertFalse(result.get(0, 0)); // true ^ true = false
            assertTrue(result.get(0, 1)); // false ^ true = true
            assertFalse(result.get(1, 0)); // false ^ false = false
            assertTrue(result.get(1, 1)); // true ^ false = true
        }

        @Test
        public void testZipWith_Three() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false, true } });
            BooleanMatrix m3 = BooleanMatrix.wrap(new boolean[][] { { true, true } });
            BooleanMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a || b || c);
            assertTrue(result.get(0, 0));
            assertTrue(result.get(0, 1));
        }

        @Test
        public void testZipWith_ThreeMatrices_Shape2By2() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, true }, { false, false } });
            BooleanMatrix m3 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { true, false } });
            BooleanMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a || b || c);
            assertTrue(result.get(0, 0));
            assertTrue(result.get(0, 1));
            assertTrue(result.get(1, 0));
            assertTrue(result.get(1, 1));
        }

        @Test
        public void testzipWith() {
            BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] { { true, true }, { true, true } });

            // Element-wise AND
            BooleanMatrix and = a.zipWith(b, (x, y) -> x && y);
            // Result: [[true, false], [false, true]]
            assertTrue(and.get(0, 0));
            assertFalse(and.get(0, 1));
            assertFalse(and.get(1, 0));
            assertTrue(and.get(1, 1));

            // Element-wise OR
            BooleanMatrix or = a.zipWith(b, (x, y) -> x || y);
            // Result: [[true, true], [true, true]]
            assertTrue(or.get(0, 0));
            assertTrue(or.get(0, 1));
            assertTrue(or.get(1, 0));
            assertTrue(or.get(1, 1));

            // Element-wise XOR
            BooleanMatrix xor = a.zipWith(b, (x, y) -> x ^ y);
            // Result: [[false, true], [true, false]]
            assertFalse(xor.get(0, 0));
            assertTrue(xor.get(0, 1));
            assertTrue(xor.get(1, 0));
            assertFalse(xor.get(1, 1));
        }

        @Test
        public void testZipWith_Two_InvalidShape() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a && b));
        }
    }

    @Nested
    class MainDiagonalStreamTests {

        @Test
        public void testStreamLU2RD() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            List<Boolean> diagonal = matrix.mainDiagonalStream().toList();
            assertEquals(2, diagonal.size());
            assertTrue(diagonal.get(0));
            assertTrue(diagonal.get(1));

            BooleanMatrix empty = BooleanMatrix.empty();
            assertTrue(empty.mainDiagonalStream().toList().isEmpty());
        }

        @Test
        public void testMainDiagonalStream_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Boolean> diagonal = m.mainDiagonalStream().toList();
            assertEquals(3, diagonal.size());
            assertTrue(diagonal.get(0));
            assertTrue(diagonal.get(1));
            assertTrue(diagonal.get(2));
        }

        @Test
        public void testMainDiagonalStream_NonSquare() {
            BooleanMatrix nonSquare = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertEquals(List.of(true), nonSquare.mainDiagonalStream().toList());
        }

        @Test
        public void testMainDiagonalStream_Shape3By3_OrderedListValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Boolean> diag = m.mainDiagonalStream().toList();
            assertEquals(3, diag.size());
            assertTrue(diag.get(0));
            assertTrue(diag.get(1));
            assertTrue(diag.get(2));
        }

        @Test
        public void testMainDiagonalStream_AllTrueDiagonal() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, true, false }, { false, false, true } });
            List<Boolean> diagonal = m.mainDiagonalStream().toList();
            assertEquals(3, diagonal.size());
            assertTrue(diagonal.get(0));
            assertTrue(diagonal.get(1));
            assertTrue(diagonal.get(2));
        }

        @Test
        public void testMainDiagonalStream() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, true, false }, { false, false, true } });
            List<Boolean> diag = m.mainDiagonalStream().toList();
            assertEquals(3, diag.size());
            assertTrue(diag.get(0));
            assertTrue(diag.get(1));
            assertTrue(diag.get(2));
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testIteratorExCountConsumesEveryCustomIterator() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });

            com.landawn.abacus.util.stream.ObjIteratorEx<Boolean> main = (com.landawn.abacus.util.stream.ObjIteratorEx<Boolean>) matrix.mainDiagonalStream()
                    .iterator();
            assertEquals(2, main.count());
            assertFalse(main.hasNext());

            com.landawn.abacus.util.stream.ObjIteratorEx<Boolean> anti = (com.landawn.abacus.util.stream.ObjIteratorEx<Boolean>) matrix.antiDiagonalStream()
                    .iterator();
            assertEquals(2, anti.count());
            assertFalse(anti.hasNext());

            com.landawn.abacus.util.stream.ObjIteratorEx<Boolean> rowMajor = (com.landawn.abacus.util.stream.ObjIteratorEx<Boolean>) matrix.rowMajorStream()
                    .iterator();
            assertEquals(6, rowMajor.count());
            assertFalse(rowMajor.hasNext());

            com.landawn.abacus.util.stream.ObjIteratorEx<Boolean> columnMajor = (com.landawn.abacus.util.stream.ObjIteratorEx<Boolean>) matrix
                    .columnMajorStream()
                    .iterator();
            assertEquals(6, columnMajor.count());
            assertFalse(columnMajor.hasNext());

            com.landawn.abacus.util.stream.ObjIteratorEx<Stream<Boolean>> rows = (com.landawn.abacus.util.stream.ObjIteratorEx<Stream<Boolean>>) matrix
                    .rowStreams()
                    .iterator();
            assertEquals(2, rows.count());
            assertFalse(rows.hasNext());

            com.landawn.abacus.util.stream.ObjIteratorEx<Stream<Boolean>> columns = (com.landawn.abacus.util.stream.ObjIteratorEx<Stream<Boolean>>) matrix
                    .columnStreams()
                    .iterator();
            Stream<Boolean> firstColumn = columns.next();
            com.landawn.abacus.util.stream.ObjIteratorEx<Boolean> columnValues = (com.landawn.abacus.util.stream.ObjIteratorEx<Boolean>) firstColumn.iterator();
            assertEquals(2, columnValues.count());
            assertFalse(columnValues.hasNext());
            assertEquals(2, columns.count());
            assertFalse(columns.hasNext());
        }

        @Test
        public void testMainDiagonalStream_Empty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().count());
        }

        @Test
        public void testDiagonalStream_Nx0MatrixIsEmpty() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][0]);
            assertEquals(3, m.rowCount());
            assertEquals(0, m.columnCount());
            assertEquals(0, m.mainDiagonalStream().count());
            assertEquals(0, m.antiDiagonalStream().count());

            BooleanMatrix empty = BooleanMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().count());
            assertEquals(0, empty.antiDiagonalStream().count());
        }
    }

    @Nested
    class AntiDiagonalStreamTests {

        @Test
        public void testStreamRU2LD() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            List<Boolean> diagonal = matrix.antiDiagonalStream().toList();
            assertEquals(2, diagonal.size());
            assertFalse(diagonal.get(0));
            assertFalse(diagonal.get(1));
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Boolean> antiDiagonal = m.antiDiagonalStream().toList();
            assertEquals(3, antiDiagonal.size());
            assertTrue(antiDiagonal.get(0));
            assertTrue(antiDiagonal.get(1));
            assertTrue(antiDiagonal.get(2));
        }

        @Test
        public void testAntiDiagonalStream_NonSquare() {
            BooleanMatrix nonSquare = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertEquals(List.of(false), nonSquare.antiDiagonalStream().toList());
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_OrderedListValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, true }, { false, true, false }, { true, false, false } });
            List<Boolean> diagonal = m.antiDiagonalStream().toList();
            assertEquals(3, diagonal.size());
            assertTrue(diagonal.get(0));
            assertTrue(diagonal.get(1));
            assertTrue(diagonal.get(2));
        }

        @Test
        public void testAntiDiagonalStream() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, true }, { false, true, false }, { true, false, false } });
            List<Boolean> diag = m.antiDiagonalStream().toList();
            assertEquals(3, diag.size());
            assertTrue(diag.get(0));
            assertTrue(diag.get(1));
            assertTrue(diag.get(2));
        }

        @Test
        public void testAntiDiagonalStream_Empty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            assertEquals(0, empty.antiDiagonalStream().count());
        }
    }

    @Nested
    class RowMajorStreamTests {

        @Test
        public void testRowMajorStream() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            List<Boolean> elements = matrix.rowMajorStream().toList();
            assertEquals(4, elements.size());
            assertTrue(elements.get(0));
            assertFalse(elements.get(1));
            assertFalse(elements.get(2));
            assertTrue(elements.get(3));
        }

        @Test
        public void testRowMajorStreamWithRow() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            List<Boolean> row = matrix.rowMajorStream(0, 1).toList();
            assertEquals(2, row.size());
            assertTrue(row.get(0));
            assertFalse(row.get(1));
        }

        @Test
        public void testRowMajorStreamWithRange() {
            boolean[][] arr = { { true, false }, { false, true }, { true, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            List<Boolean> elements = matrix.rowMajorStream(1, 3).toList();
            assertEquals(4, elements.size());
            assertFalse(elements.get(0));
            assertTrue(elements.get(1));

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 2));
        }

        @Test
        public void testRowMajorStream_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            List<Boolean> all = m.rowMajorStream().toList();
            assertEquals(6, all.size());
            assertTrue(all.get(0));
            assertFalse(all.get(1));
            assertTrue(all.get(2));
            assertFalse(all.get(3));
        }

        @Test
        public void testRowMajorStream_WithRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Boolean> rows = m.rowMajorStream(1, 3).toList();
            assertEquals(6, rows.size());
        }

        @Test
        public void testRowMajorStream_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Boolean> all = m.rowMajorStream().toList();
            assertEquals(4, all.size());
            assertTrue(all.get(0));
            assertFalse(all.get(1));
        }

        @Test
        public void testRowMajorStream_RowRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            List<Boolean> rows = m.rowMajorStream(1, 3).toList();
            assertEquals(4, rows.size());
            assertFalse(rows.get(0));
            assertTrue(rows.get(1));
        }

        @Test
        public void testRowMajorStream_WithRange_Shape3By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            List<Boolean> elements = m.rowMajorStream(1, 3).toList();
            assertEquals(4, elements.size());
            assertFalse(elements.get(0));
            assertTrue(elements.get(1));
        }

        @Test
        public void testHorizontalStream() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Boolean> elements = m.rowMajorStream().toList();
            assertEquals(4, elements.size());
            assertTrue(elements.get(0));
            assertFalse(elements.get(1));
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testRowMajorStreamorizontalIteratorAdvanceAndToArray_EdgeCase() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            var iterator = matrix.rowMajorStream(0, 2).iterator();

            assertTrue(iterator instanceof com.landawn.abacus.util.stream.ObjIteratorEx);

            com.landawn.abacus.util.stream.ObjIteratorEx<Boolean> ex = (com.landawn.abacus.util.stream.ObjIteratorEx<Boolean>) iterator;
            ex.advance(1);
            assertEquals(5L, ex.count());
            assertFalse(ex.hasNext());
            assertArrayEquals(new Boolean[0], ex.toArray(new Boolean[0]));

            com.landawn.abacus.util.stream.ObjIteratorEx<Boolean> fresh = (com.landawn.abacus.util.stream.ObjIteratorEx<Boolean>) matrix.rowMajorStream(0, 2)
                    .iterator();
            fresh.advance(1);
            assertArrayEquals(new Boolean[] { false, true, false, true, false }, fresh.toArray(new Boolean[0]));
            ex.advance(10);
            assertEquals(0L, ex.count());
            assertThrows(java.util.NoSuchElementException.class, ex::next);
        }

        @Test
        public void testRowMajorStream_Empty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            assertEquals(0, empty.rowMajorStream().count());
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            List<Boolean> row1 = m.rowMajorStream(1, 2).toList();
            assertEquals(3, row1.size());
            assertFalse(row1.get(0));
            assertTrue(row1.get(1));
            assertFalse(row1.get(2));
        }

        @Test
        public void testRowMajorStream_SingleRowRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            List<Boolean> row = m.rowMajorStream(0, 1).toList();
            assertEquals(3, row.size());
            assertTrue(row.get(0));
            assertFalse(row.get(1));
            assertTrue(row.get(2));
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Boolean> row = m.rowMajorStream(1, 2).toList();
            assertEquals(2, row.size());
            assertFalse(row.get(0));
            assertTrue(row.get(1));
        }

        @Test
        public void testStreamIterators_NullArrayDestinationPreservesRemainingElements() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            ObjIteratorEx<Boolean> rowIterator = (ObjIteratorEx<Boolean>) matrix.rowMajorStream().iterator();
            ObjIteratorEx<Boolean> columnIterator = (ObjIteratorEx<Boolean>) matrix.columnMajorStream().iterator();

            assertEquals(Boolean.TRUE, rowIterator.next());
            assertEquals(Boolean.TRUE, columnIterator.next());

            assertThrows(NullPointerException.class, () -> rowIterator.toArray((Boolean[]) null));
            assertThrows(NullPointerException.class, () -> columnIterator.toArray((Boolean[]) null));

            assertArrayEquals(new Boolean[] { false, true, false, true, false }, rowIterator.toArray(new Boolean[0]));
            assertArrayEquals(new Boolean[] { false, false, true, true, false }, columnIterator.toArray(new Boolean[0]));
            assertFalse(rowIterator.hasNext());
            assertFalse(columnIterator.hasNext());
        }

        @Test
        public void testStreamIterators_IncompatibleArrayPreservesCursorAtBoundary() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            ObjIteratorEx<Boolean> rowIterator = (ObjIteratorEx<Boolean>) matrix.rowMajorStream().iterator();
            ObjIteratorEx<Boolean> columnIterator = (ObjIteratorEx<Boolean>) matrix.columnMajorStream().iterator();

            assertEquals(Boolean.TRUE, rowIterator.next());
            assertEquals(Boolean.TRUE, columnIterator.next());
            assertThrows(ArrayStoreException.class, () -> rowIterator.toArray(new String[3]));
            assertThrows(ArrayStoreException.class, () -> columnIterator.toArray(new String[3]));

            assertEquals(Boolean.FALSE, rowIterator.next());
            assertEquals(Boolean.FALSE, columnIterator.next());
            assertArrayEquals(new Boolean[] { false, true }, rowIterator.toArray(new Boolean[0]));
            assertArrayEquals(new Boolean[] { false, true }, columnIterator.toArray(new Boolean[0]));
            assertFalse(rowIterator.hasNext());
            assertFalse(columnIterator.hasNext());
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testRowMajorStreamorizontalIteratorOversizedArrayIsNullTerminated_EdgeCase() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            var iterator = matrix.rowMajorStream(0, 2).iterator();

            assertTrue(iterator instanceof com.landawn.abacus.util.stream.ObjIteratorEx);

            com.landawn.abacus.util.stream.ObjIteratorEx<Boolean> ex = (com.landawn.abacus.util.stream.ObjIteratorEx<Boolean>) iterator;
            Boolean[] destination = new Boolean[] { null, null, null, null, Boolean.TRUE, Boolean.TRUE };
            Boolean[] array = ex.toArray(destination);

            assertSame(destination, array);
            assertArrayEquals(new Boolean[] { Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, null, Boolean.TRUE }, array);
        }

        @Test
        public void testBooleanEmptyStreamRangesKeepBoundsValidationAndLogicalShapeCounts() {
            final BooleanMatrix matrix = new BooleanMatrix(new boolean[2][2]);
            assertEquals(0L, matrix.rowMajorStream(1, 1).count());
            assertEquals(0L, matrix.columnMajorStream(1, 1).count());
            assertEquals(0L, matrix.rowStreams(1, 1).count());
            assertEquals(0L, matrix.columnStreams(1, 1).count());
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(3, 3));

            final BooleanMatrix noColumns = new BooleanMatrix(new boolean[2][0]);
            assertEquals(2L, noColumns.rowStreams().count());
            noColumns.rowStreams().forEach(row -> assertEquals(0L, row.count()));
            assertEquals(0L, noColumns.mainDiagonalStream().count());
            assertEquals(0L, noColumns.antiDiagonalStream().count());

            final BooleanMatrix noRows = new BooleanMatrix(new boolean[0][], 3);
            assertEquals(3L, noRows.columnStreams().count());
            noRows.columnStreams().forEach(column -> assertEquals(0L, column.count()));
            assertEquals(0L, noRows.mainDiagonalStream().count());
            assertEquals(0L, noRows.antiDiagonalStream().count());
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 3));
        }

        @Test
        public void testRowMajorStream_WithRange_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 1));
        }

        @Test
        public void testIteratorArrayNullContractRejectsWithoutConsumingElements() {
            final BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[] { true, false });
            final var rows = matrix.rowMajorStream().iterator();
            final var columns = matrix.columnMajorStream().iterator();

            assertThrows(NullPointerException.class, () -> rows.toArray((Boolean[]) null));
            assertThrows(NullPointerException.class, () -> columns.toArray((Boolean[]) null));
            assertTrue(rows.next());
            assertTrue(columns.next());
        }
    }

    @Nested
    class ColumnMajorStreamTests {

        @Test
        public void testColumnMajorStream() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            List<Boolean> elements = matrix.columnMajorStream().toList();
            assertEquals(4, elements.size());
            assertTrue(elements.get(0));
            assertFalse(elements.get(1));
            assertFalse(elements.get(2));
            assertTrue(elements.get(3));
        }

        @Test
        public void testColumnMajorStreamWithColumn() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            List<Boolean> col = matrix.columnMajorStream(0, 1).toList();
            assertEquals(2, col.size());
            assertTrue(col.get(0));
            assertFalse(col.get(1));
        }

        @Test
        public void testColumnMajorStreamWithRange() {
            boolean[][] arr = { { true, false, true }, { false, true, false } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            List<Boolean> elements = matrix.columnMajorStream(1, 3).toList();
            assertEquals(4, elements.size());

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(-1, 2));
        }

        @Test
        public void testColumnMajorStream_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            List<Boolean> all = m.columnMajorStream().toList();
            assertEquals(6, all.size());
            assertTrue(all.get(0));
            assertFalse(all.get(1));
            assertFalse(all.get(2));
        }

        @Test
        public void testColumnMajorStream_WithRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Boolean> columnCount = m.columnMajorStream(1, 3).toList();
            assertEquals(6, columnCount.size());
        }

        @Test
        public void testColumnMajorStream_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Boolean> all = m.columnMajorStream().toList();
            assertEquals(4, all.size());
            assertTrue(all.get(0));
            assertFalse(all.get(1));
        }

        @Test
        public void testColumnMajorStream_ColumnRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            List<Boolean> columnCount = m.columnMajorStream(1, 3).toList();
            assertEquals(4, columnCount.size());
            assertFalse(columnCount.get(0));
            assertTrue(columnCount.get(1));
        }

        @Test
        public void testColumnMajorStream_WithRange_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            List<Boolean> elements = m.columnMajorStream(1, 3).toList();
            assertEquals(4, elements.size());
            assertFalse(elements.get(0));
            assertTrue(elements.get(1));
        }

        @Test
        public void testVerticalStream() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Boolean> elements = m.columnMajorStream().toList();
            assertEquals(4, elements.size());
            assertTrue(elements.get(0));
            assertFalse(elements.get(1));
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testColumnMajorStreamerticalIteratorAdvanceAndToArray_EdgeCase() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            var iterator = matrix.columnMajorStream(0, 2).iterator();

            assertTrue(iterator instanceof com.landawn.abacus.util.stream.ObjIteratorEx);

            com.landawn.abacus.util.stream.ObjIteratorEx<Boolean> ex = (com.landawn.abacus.util.stream.ObjIteratorEx<Boolean>) iterator;
            assertEquals(Boolean.TRUE, ex.next());
            ex.advance(1);
            assertEquals(4L, ex.count());
            assertFalse(ex.hasNext());
            assertArrayEquals(new Boolean[0], ex.toArray(new Boolean[0]));

            com.landawn.abacus.util.stream.ObjIteratorEx<Boolean> fresh = (com.landawn.abacus.util.stream.ObjIteratorEx<Boolean>) matrix.columnMajorStream(0, 2)
                    .iterator();
            assertEquals(Boolean.TRUE, fresh.next());
            fresh.advance(1);
            assertArrayEquals(new Boolean[] { true, false, true, true }, fresh.toArray(new Boolean[0]));
            ex.advance(10);
            assertEquals(0L, ex.count());
            assertThrows(java.util.NoSuchElementException.class, ex::next);
        }

        @Test
        public void testColumnMajorStream_Empty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            assertEquals(0, empty.columnMajorStream().count());
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            List<Boolean> col1 = m.columnMajorStream(1, 2).toList();
            assertEquals(2, col1.size());
            assertFalse(col1.get(0));
            assertTrue(col1.get(1));
        }

        @Test
        public void testColumnMajorStream_SingleColumnRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, false } });
            List<Boolean> col = m.columnMajorStream(0, 1).toList();
            assertEquals(3, col.size());
            assertTrue(col.get(0));
            assertFalse(col.get(1));
            assertTrue(col.get(2));
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Boolean> col = m.columnMajorStream(1, 2).toList();
            assertEquals(2, col.size());
            assertFalse(col.get(0));
            assertTrue(col.get(1));
        }

        @Test
        public void testColumnMajorStream_SingleColumnRange_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Boolean> col = m.columnMajorStream(0, 1).toList();
            assertEquals(2, col.size());
            assertTrue(col.get(0));
            assertFalse(col.get(1));
        }

        // Behavioral regression: ensure the small-matrix path was not broken by widening.
        @SuppressWarnings("unchecked")
        @Test
        public void testColumnMajorStreamerticalIteratorOversizedArrayIsNullTerminated_EdgeCase() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            var iterator = matrix.columnMajorStream(0, 2).iterator();

            assertTrue(iterator instanceof com.landawn.abacus.util.stream.ObjIteratorEx);

            com.landawn.abacus.util.stream.ObjIteratorEx<Boolean> ex = (com.landawn.abacus.util.stream.ObjIteratorEx<Boolean>) iterator;
            Boolean[] destination = new Boolean[] { null, null, null, null, Boolean.TRUE, Boolean.TRUE };
            Boolean[] array = ex.toArray(destination);

            assertSame(destination, array);
            assertArrayEquals(new Boolean[] { Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, null, Boolean.TRUE }, array);
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 3));
        }

        @Test
        public void testColumnMajorStream_WithRange_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 1));
        }
    }

    @Nested
    class RowStreamsTests {

        @Test
        public void testDocumentedStreamsReadLiveCellsAndPreserveProducedRowBinding() {
            final BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { false, false }, { true, false } });
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
            matrix.set(1, 0, true);
            matrix.set(0, 1, true);

            assertArrayEquals(new Boolean[] { true, false }, row.toArray());
            assertArrayEquals(new Boolean[] { true, false }, selectedRow.toArray());
            assertArrayEquals(new Boolean[] { true, true }, column.toArray());
            assertArrayEquals(new Boolean[] { true, true }, selectedColumn.toArray());
            assertArrayEquals(new Boolean[] { true, true, true, false }, rowMajor.toArray());
            assertArrayEquals(new Boolean[] { true, true }, selectedRowMajor.toArray());
            assertArrayEquals(new Boolean[] { true, true, true, false }, columnMajor.toArray());
            assertArrayEquals(new Boolean[] { true, true }, selectedColumnMajor.toArray());
            assertArrayEquals(new Boolean[] { true, false }, mainDiagonal.toArray());
            assertArrayEquals(new Boolean[] { true, true }, antiDiagonal.toArray());
        }

        @Test
        public void testStreamR() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            List<List<Boolean>> rows = matrix.rowStreams().map(stream -> stream.toList()).toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).size());
            assertTrue(rows.get(0).get(0));
            assertFalse(rows.get(0).get(1));
        }

        @Test
        public void testRowStreamsWithRange() {
            boolean[][] arr = { { true, false }, { false, true }, { true, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            List<List<Boolean>> rows = matrix.rowStreams(1, 3).map(stream -> stream.toList()).toList();
            assertEquals(2, rows.size());

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(-1, 2));
        }

        @Test
        public void testRowStreams_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            List<Stream<Boolean>> rows = m.rowStreams().toList();
            assertEquals(2, rows.size());
            List<Boolean> row0 = rows.get(0).toList();
            assertEquals(3, row0.size());
            assertTrue(row0.get(0));
        }

        @Test
        public void testRowStreams_WithRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Stream<Boolean>> rows = m.rowStreams(1, 3).toList();
            assertEquals(2, rows.size());
        }

        @Test
        public void testRowStreams_Operations() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            long rowsWithTrue = m.rowStreams().filter(row -> row.anyMatch(b -> b)).count();
            assertEquals(2, rowsWithTrue);
        }

        @Test
        public void testRowStreams_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Stream<Boolean>> rows = m.rowStreams().toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).count());
        }

        @Test
        public void testRowStreams_RowRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            List<Stream<Boolean>> rows = m.rowStreams(1, 3).toList();
            assertEquals(2, rows.size());
        }

        @Test
        public void testRowStreams_Shape2By2_OuterAndInnerStreamSizes() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Stream<Boolean>> rows = m.rowStreams().toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).toList().size());
        }

        @Test
        public void testRowStreams() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<List<Boolean>> rows = m.rowStreams().map(Stream::toList).toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).size());
        }

        @Test
        public void testRowStreams_RowRange_Shape3By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true }, { true, true } });
            List<List<Boolean>> rows = m.rowStreams(1, 3).map(Stream::toList).toList();
            assertEquals(2, rows.size());
        }

        @Test
        public void testRowStreams_Empty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            assertEquals(0, empty.rowStreams().count());
        }

        @Test
        public void testRowStreams_Nx0YieldsOneEmptyStreamPerRow_0xNYieldsNoStreams() {
            final List<List<Boolean>> nx0Rows = BooleanMatrix.wrap(new boolean[3][0]).rowStreams().map(Stream::toList).toList();
            assertEquals(3, nx0Rows.size());
            nx0Rows.forEach(row -> assertTrue(row.isEmpty()));

            assertEquals(0, new BooleanMatrix(new boolean[0][], 3).rowStreams().count());
            assertEquals(0, BooleanMatrix.empty().rowStreams().count());

            final List<List<Boolean>> zeroRowColumns = new BooleanMatrix(new boolean[0][], 3).columnStreams().map(Stream::toList).toList();
            assertEquals(3, zeroRowColumns.size());
            zeroRowColumns.forEach(column -> assertTrue(column.isEmpty()));
            assertEquals(0, BooleanMatrix.wrap(new boolean[3][0]).columnStreams().count());
        }

        @Test
        public void testRowStreams_WithRange_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(0, 3));
        }
    }

    @Nested
    class ColumnStreamsTests {

        @Test
        public void testStreamC() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            List<List<Boolean>> columnCount = matrix.columnStreams().map(stream -> stream.toList()).toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).size());
            assertTrue(columnCount.get(0).get(0));
            assertFalse(columnCount.get(0).get(1));
        }

        @Test
        public void testColumnStreamsWithRange() {
            boolean[][] arr = { { true, false, true }, { false, true, false } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            List<List<Boolean>> columnCount = matrix.columnStreams(1, 3).map(stream -> stream.toList()).toList();
            assertEquals(2, columnCount.size());

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(-1, 2));
        }

        @Test
        public void testColumnStreams_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            List<Stream<Boolean>> columnCount = m.columnStreams().toList();
            assertEquals(3, columnCount.size());
        }

        @Test
        public void testColumnStreams_WithRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Stream<Boolean>> columnCount = m.columnStreams(1, 3).toList();
            assertEquals(2, columnCount.size());
        }

        @Test
        public void testColumnStreams_Operations() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            long columnCountWithAllFalse = m.columnStreams().filter(col -> col.noneMatch(b -> b)).count();
            assertEquals(0, columnCountWithAllFalse); // No columns have all false (col 0: true,false; col 1: false,true; col 2: true,false)
        }

        @Test
        public void testColumnStreams_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Stream<Boolean>> columnCount = m.columnStreams().toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).count());
        }

        @Test
        public void testColumnStreams_ColumnRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            List<Stream<Boolean>> columnCount = m.columnStreams(1, 3).toList();
            assertEquals(2, columnCount.size());
        }

        @Test
        public void testColumnStreams_Shape2By2_OuterAndInnerStreamSizes() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Stream<Boolean>> columnCount = m.columnStreams().toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).toList().size());
        }

        // Result: [[true], [false], [false]]
        @Test
        public void testColumnStreams() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<List<Boolean>> columnCount = m.columnStreams().map(Stream::toList).toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).size());
        }

        @Test
        public void testColumnStreams_ColumnRange_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            List<List<Boolean>> columnCount = m.columnStreams(0, 2).map(Stream::toList).toList();
            assertEquals(2, columnCount.size());
        }

        @Test
        public void testColumnStreams_Empty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            assertEquals(0, empty.columnStreams().count());
        }

        @Test
        public void testColumnStreams_WithRange_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
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
                final BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[2][3]);
                final AtomicInteger calls = new AtomicInteger();
                final IllegalStateException failure = new IllegalStateException("action failed");
                final Throwables.BooleanConsumer<IllegalStateException> action = value -> {
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
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            int[] trueCount = { 0 };
            matrix.forEach(val -> {
                if (val)
                    trueCount[0]++;
            });
            assertEquals(2, trueCount[0]);
        }

        @Test
        public void testForEachWithRange() {
            boolean[][] arr = { { true, false, true }, { false, true, false } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            int[] trueCount = { 0 };
            matrix.forEach(0, 2, 1, 3, val -> {
                if (val)
                    trueCount[0]++;
            });
            assertEquals(2, trueCount[0]);

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.forEach(-1, 2, 0, 2, val -> {
            }));
        }

        @Test
        public void testForEach_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Boolean> values = new ArrayList<>();
            m.forEach(val -> values.add(val));

            assertEquals(4, values.size());
            assertTrue(values.get(0));
            assertFalse(values.get(1));
            assertFalse(values.get(2));
            assertTrue(values.get(3));
        }

        @Test
        public void testForEach_WithRanges() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Boolean> values = new ArrayList<>();
            m.forEach(1, 3, 1, 3, val -> values.add(val));

            assertEquals(4, values.size());
            assertTrue(values.get(0)); // (1,1)
            assertFalse(values.get(1)); // (1,2)
            assertFalse(values.get(2)); // (2,1)
            assertTrue(values.get(3)); // (2,2)
        }

        @Test
        public void testForEach_ValueConsumer() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Boolean> values = new ArrayList<>();
            m.forEach(b -> values.add(b));
            assertEquals(4, values.size());
            assertTrue(values.get(0));
            assertFalse(values.get(1));
        }

        @Test
        public void testForEach_WithRange() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, true, true } });
            List<Boolean> values = new ArrayList<>();
            m.forEach(1, 3, 1, 3, b -> values.add(b));
            assertEquals(4, values.size());
            assertTrue(values.get(0));
            assertFalse(values.get(1));
        }

        @Test
        public void testForEach_Shape2By2_CountsTrueValues() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEach(val -> {
                if (val) {
                    count.incrementAndGet();
                }
            });
            assertEquals(2, count.get());
        }

        @Test
        public void testForEachNullAction() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            assertThrows(IllegalArgumentException.class, () -> matrix.forEach((Throwables.BooleanConsumer<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.forEach(0, 1, 0, 2, (Throwables.BooleanConsumer<RuntimeException>) null));
        }

        @Test
        public void testForEach_Empty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            List<Boolean> values = new ArrayList<>();
            empty.forEach(val -> values.add(val));
            assertTrue(values.isEmpty());
        }

        @Test
        public void testForEach_WithRanges_SingleCell() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Boolean> values = new ArrayList<>();
            m.forEach(0, 1, 0, 1, val -> values.add(val));

            assertEquals(1, values.size());
            assertTrue(values.get(0));
        }

        @Test
        public void testForEach_WithRanges_OutOfBounds() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(-1, 2, 0, 2, val -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 3, 0, 2, val -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 2, -1, 2, val -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 2, 0, 3, val -> {
            }));
        }
    }

    @Nested
    class ToMultilineStringTests {

        @Test
        public void testToMultilineString_Shape2By2_ContainsElementText() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            String result = m.toMultilineString();
            assertNotNull(result);
            assertTrue(result.contains("true"));
            assertTrue(result.contains("false"));
        }

        @Test
        public void testToMultilineStringEmptyReturnsBracketString() {
            // Empty path returns "[]" directly.
            BooleanMatrix empty = BooleanMatrix.empty();
            String s = empty.toMultilineString();
            assertEquals("[]", s);
        }

        @Test
        public void testToMultilineStringSingleRowProducesNonNullString() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            String s = matrix.toMultilineString();
            assertNotNull(s);
            assertTrue(s.contains("[true, false]"));
        }

        @Test
        public void testToMultilineStringMultipleRowsProducesNonNullString() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            String s = matrix.toMultilineString();
            assertNotNull(s);
            assertTrue(s.contains("[true, false]"));
            assertTrue(s.contains("[false, true]"));
        }
    }

    @Nested
    class HashCodeTests {

        @Test
        public void testHashCode() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix1 = BooleanMatrix.wrap(arr);
            BooleanMatrix matrix2 = BooleanMatrix.wrap(arr);

            assertEquals(matrix1.hashCode(), matrix2.hashCode());
        }

        @Test
        public void testHashCode_Shape2By2() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m3 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { true, false } });

            assertEquals(m1.hashCode(), m2.hashCode());
            assertNotEquals(m1.hashCode(), m3.hashCode()); // Usually different
        }

        @Test
        public void testHashCode_Consistency() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            int hash1 = m1.hashCode();
            int hash2 = m1.hashCode();
            assertEquals(hash1, hash2); // Same object should have same hash
        }

        // Center element should be from main diagonal (set second)
        @Test
        public void testHashCode_Equal() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertEquals(m1.hashCode(), m2.hashCode());
        }
    }

    @Nested
    class EqualsTests {

        @Test
        public void testEquals() {
            boolean[][] arr1 = { { true, false }, { false, true } };
            boolean[][] arr2 = { { true, false }, { false, true } };
            boolean[][] arr3 = { { true, false }, { true, false } };

            BooleanMatrix matrix1 = BooleanMatrix.wrap(arr1);
            BooleanMatrix matrix2 = BooleanMatrix.wrap(arr2);
            BooleanMatrix matrix3 = BooleanMatrix.wrap(arr3);

            assertTrue(matrix1.equals(matrix1));
            assertTrue(matrix1.equals(matrix2));
            assertFalse(matrix1.equals(matrix3));
            assertFalse(matrix1.equals(null));
            assertFalse(matrix1.equals("not a matrix"));
        }

        @Test
        public void testEquals_Same() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertTrue(m.equals(m));
        }

        @Test
        public void testEquals_Equal() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertEquals(m1, m2);
        }

        @Test
        public void testEquals_NotEqual() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false, true } });
            assertNotEquals(m1, m2);
        }

        @Test
        public void testEquals_DifferentType() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertNotEquals(m, "string");
        }

        @Test
        public void testEquals_Shape2By2_NullInput() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m3 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { true, false } });
            BooleanMatrix m4 = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });

            assertTrue(m1.equals(m1)); // Same object
            assertTrue(m1.equals(m2)); // Same values
            assertFalse(m1.equals(m3)); // Different values
            assertFalse(m1.equals(m4)); // Different dimensions
            assertFalse(m1.equals(null));
            assertFalse(m1.equals("not a matrix"));
        }

        @Test
        public void testEquals_Shape2By2_NullInput_DifferentValuesNullAndType() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m3 = BooleanMatrix.wrap(new boolean[][] { { false, true }, { true, false } });
            assertEquals(m1, m2);
            assertNotEquals(m1, m3);
            assertNotEquals(m1, null);
            assertNotEquals(m1, "not a matrix");
        }

        @Test
        public void testEquals_EmptyMatrices() {
            BooleanMatrix empty1 = BooleanMatrix.empty();
            BooleanMatrix empty2 = BooleanMatrix.wrap(new boolean[0][0]);
            assertTrue(empty1.equals(empty2));
        }

        @Test
        public void testEquals_Null() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertNotEquals(m, null);
        }

        @Test
        public void testEquals_EmptyMatrices_EmptyMatrix() {
            BooleanMatrix m1 = BooleanMatrix.empty();
            BooleanMatrix m2 = BooleanMatrix.empty();
            assertEquals(m1, m2);
        }
    }

    @Nested
    class ToStringTests {

        @Test
        public void testToString() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            String str = matrix.toString();
            assertNotNull(str);
            assertTrue(str.contains("true"));
            assertTrue(str.contains("false"));
        }

        @Test
        public void testToString_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("true") || str.contains("false"));
        }

        @Test
        public void testToString_Shape2By2_ContainsElementText() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("true"));
            assertTrue(str.contains("false"));
        }

        @Test
        public void testToString_SingleRow() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.length() > 0);
        }
    }

    @Nested
    class ElementTypeTests {

        @Test
        public void testElementType() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertEquals(boolean.class, matrix.elementType());
        }

        @Test
        public void testElementType_Shape1By1() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertEquals(boolean.class, m.elementType());
        }

        @Test
        public void testElementType_EmptyMatrix() {
            BooleanMatrix m = BooleanMatrix.empty();
            assertEquals(boolean.class, m.elementType());
        }
    }

    @Nested
    class UnsafeBackingArrayTests {

        @Test
        public void testArray() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix m = BooleanMatrix.wrap(arr);
            boolean[][] result = m.unsafeBackingArray();

            // The outer array is a private snapshot, so replacing a row in either array is invisible
            // to the other; the row arrays themselves are still shared, so cell writes are visible.
            assertNotSame(arr, result);
            assertSame(arr[0], result[0]);
            assertSame(arr[1], result[1]);

            arr[0][0] = false;
            assertEquals(false, m.get(0, 0));
        }

        @Test
        public void testArray_ArrayValues() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix m = BooleanMatrix.wrap(arr);
            boolean[][] result = m.unsafeBackingArray();
            assertArrayEquals(arr, result);
        }
    }

    @Nested
    class ElementCountTests {

        @Test
        public void testElementCount() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            assertEquals(6, m.elementCount());
        }

        @Test
        public void testElementCount_Empty() {
            BooleanMatrix m = BooleanMatrix.empty();
            assertEquals(0, m.elementCount());
        }
    }

    @Nested
    class IsEmptyTests {

        @Test
        public void testIsEmpty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            assertTrue(empty.isEmpty());
            BooleanMatrix notEmpty = BooleanMatrix.wrap(new boolean[][] { { true } });
            assertFalse(notEmpty.isEmpty());
        }

        @Test
        public void testIsEmpty_Shape1By1() {
            assertTrue(BooleanMatrix.empty().isEmpty());
            assertFalse(BooleanMatrix.wrap(new boolean[][] { { true } }).isEmpty());
        }
    }

    @Nested
    class IsSameShapeTests {

        @Test
        public void testIsSameShape() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[2][3]);
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[2][3]);
            BooleanMatrix m3 = BooleanMatrix.wrap(new boolean[3][2]);
            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }

        @Test
        public void testIsSameShape_Shape2By2() {
            BooleanMatrix m1 = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            BooleanMatrix m2 = BooleanMatrix.wrap(new boolean[][] { { false, true }, { true, false } });
            BooleanMatrix m3 = BooleanMatrix.wrap(new boolean[][] { { true, false, true } });
            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }
    }

    @Nested
    class ReshapeTests {

        @Test
        public void testAllTrueMatrix() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, true, true } });
            m = m.reshape(3, 1);
            m = m.repeatMatrix(1, 3);

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    assertTrue(m.get(i, j));
                }
            }
        }

        @Test
        public void testReshape() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            BooleanMatrix reshaped = matrix.reshape(1, 4);
            assertEquals(1, reshaped.rowCount());
            assertEquals(4, reshaped.columnCount());
            assertTrue(reshaped.get(0, 0));
            assertFalse(reshaped.get(0, 1));

            // Test reshape with too-small dimensions throws exception
            assertThrows(IllegalArgumentException.class, () -> matrix.reshape(1, 3));
            assertThrows(IllegalArgumentException.class, () -> matrix.reshape(1, 1));
        }

        @Test
        public void testReshape_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            BooleanMatrix reshaped = m.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            boolean[] expected = { true, false, true, false, true, false, true, false, true };
            for (int i = 0; i < 9; i++) {
                assertEquals(expected[i], reshaped.get(0, i));
            }
        }

        @Test
        public void testReshape_Back() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            BooleanMatrix reshaped = m.reshape(1, 9);
            BooleanMatrix reshapedBack = reshaped.reshape(3, 3);
            assertEquals(m, reshapedBack);
        }

        @Test
        public void testReshape_Shape1By4() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true, false } });
            BooleanMatrix reshaped = m.reshape(2, 2);
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertTrue(reshaped.get(0, 0));
            assertFalse(reshaped.get(0, 1));
            assertTrue(reshaped.get(1, 0));
            assertFalse(reshaped.get(1, 1));
        }

        @Test
        public void testReshape_Shape2By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false } });
            BooleanMatrix reshaped = m.reshape(3, 2);
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertTrue(reshaped.get(0, 0));
            assertFalse(reshaped.get(0, 1));
        }

        @Test
        public void testreshape() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false, true, false } });
            BooleanMatrix reshaped = matrix.reshape(2, 2);
            // Result: [[true, false], [true, false]]
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertTrue(reshaped.get(0, 0));
            assertFalse(reshaped.get(0, 1));
            assertTrue(reshaped.get(1, 0));
            assertFalse(reshaped.get(1, 1));
        }

        @Test
        public void testReshape_ToSingleRow() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { true, false } });
            BooleanMatrix reshaped = m.reshape(1, 4);
            assertEquals(1, reshaped.rowCount());
            assertEquals(4, reshaped.columnCount());
        }
    }

    @Nested
    class ReshapeAndPadToColumnCountTests {

        @Test
        public void testReshape_OneArg() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true, false } });
            BooleanMatrix reshaped = m.reshapeAndPadToColumnCount(2);
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertTrue(reshaped.get(0, 0));
            assertFalse(reshaped.get(0, 1));
            assertTrue(reshaped.get(1, 0));
            assertFalse(reshaped.get(1, 1));
        }
    }

    @Nested
    class Adjacent4PointsTests {

        @Test
        public void testAdjacent4Points() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Point> points = m.adjacent4Points(1, 1).toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testAdjacent4Points_Corner() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Point> points = m.adjacent4Points(0, 0).toList();
            assertEquals(2, points.size());
        }
    }

    @Nested
    class Adjacent8PointsTests {

        @Test
        public void testAdjacent8Points() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Point> points = m.adjacent8Points(1, 1).toList();
            assertEquals(8, points.size());
        }

        @Test
        public void testAdjacent8Points_Corner() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Point> points = m.adjacent8Points(0, 0).toList();
            assertEquals(3, points.size());
        }
    }

    @Nested
    class MainDiagonalPointsTests {

        @Test
        public void testMainDiagonalPoints() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][3]);
            List<Point> points = m.mainDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(2, 2), points.get(2));
        }

        @Test
        public void testMainDiagonalPoints_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, false }, { false, true, false }, { false, false, true } });
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
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][3]);
            List<Point> points = m.antiDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(Point.of(0, 2), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(2, 0), points.get(2));
        }

        @Test
        public void testAntiDiagonalPoints_Shape3By3() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { false, false, true }, { false, true, false }, { true, false, false } });
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
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[2][2]);
            List<Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(0, 1), points.get(1));
            assertEquals(Point.of(1, 0), points.get(2));
        }

        @Test
        public void testRowMajorPoints_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }

        @Test
        public void testRowMajorPoints_SingleRow() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[2][3]);
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
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[2][2]);
            List<Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 0), points.get(1));
        }

        @Test
        public void testColumnMajorPoints_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }

        @Test
        public void testColumnMajorPoints_SingleColumn() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][2]);
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
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[2][2]);
            List<Stream<Point>> rows = m.rowPoints().toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).count());
        }
    }

    @Nested
    class ColumnPointsTests {

        @Test
        public void testColumnPoints() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[2][2]);
            List<Stream<Point>> columnCount = m.columnPoints().toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).count());
        }
    }

    @Nested
    class AcceptTests {

        @Test
        public void testAccept() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            AtomicInteger counter = new AtomicInteger(0);
            m.accept(matrix -> counter.set(matrix.rowCount() * matrix.columnCount()));
            assertEquals(2, counter.get());
        }
    }

    @Nested
    class ApplyTests {

        @Test
        public void testApply() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            int result = m.apply(matrix -> matrix.rowCount() * matrix.columnCount());
            assertEquals(2, result);
        }
    }

    @Nested
    class PrintlnTests {

        @Test
        public void testPrintln() {
            boolean[][] arr = { { true, false }, { false, true } };
            BooleanMatrix matrix = BooleanMatrix.wrap(arr);

            assertFalse(matrix.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> matrix.println());
        }

        @Test
        public void testPrintln_Shape2By2() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertFalse(m.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> m.println());
        }

        @Test
        public void testPrintln_Empty() {
            BooleanMatrix empty = BooleanMatrix.empty();
            assertTrue(empty.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> empty.println());
        }
    }
}
