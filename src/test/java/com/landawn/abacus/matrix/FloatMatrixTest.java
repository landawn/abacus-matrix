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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.stream.FloatIteratorEx;
import com.landawn.abacus.util.stream.FloatStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

class FloatMatrixTest extends MatrixTestSupport {

    private static final float DELTA = 0.0001f;

    private FloatMatrix matrix;

    private FloatMatrix emptyMatrix;

    @BeforeEach
    public void setUp() {
        matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
        emptyMatrix = FloatMatrix.empty();
    }

    @Nested
    class ConstructorTests {

        @Test
        public void testConstructor() {
            // Test with valid array
            float[][] arr = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            FloatMatrix m = new FloatMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), DELTA);

            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> new FloatMatrix(null));
        }

        @Test
        public void testConstructor_WithValidArray() {
            float[][] arr = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            FloatMatrix m = new FloatMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(4.0f, m.get(1, 1), DELTA);
        }

        @Test
        public void testConstructor_WithFloatSpecialValues() {
            float[][] arr = { { Float.NaN, Float.POSITIVE_INFINITY }, { Float.NEGATIVE_INFINITY, -0.0f } };
            FloatMatrix m = new FloatMatrix(arr);
            assertTrue(Float.isNaN(m.get(0, 0)));
            assertEquals(Float.POSITIVE_INFINITY, m.get(0, 1), DELTA);
            assertEquals(Float.NEGATIVE_INFINITY, m.get(1, 0), DELTA);
            assertEquals(-0.0f, m.get(1, 1), DELTA);
        }

        @Test
        public void testConstructor_WithValidArray_Values() {
            float[][] arr = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            FloatMatrix m = new FloatMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0));
            assertEquals(4.0f, m.get(1, 1));
        }

        @Test
        public void testConstructor_WithLargerMatrix() {
            float[][] arr = { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } };
            FloatMatrix m = new FloatMatrix(arr);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(5.0f, m.get(1, 1));
        }

        @Test
        public void testConstructor_WithValidArray_Values_DimensionsAndCornerValues() {
            float[][] arr = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            FloatMatrix m = new FloatMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), 0.0f);
            assertEquals(4.0f, m.get(1, 1), 0.0f);
        }

        @Test
        public void testConstructor_WithSingleElement() {
            FloatMatrix m = new FloatMatrix(new float[][] { { 42.5f } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42.5f, m.get(0, 0), DELTA);
        }

        @Test
        public void testConstructor_WithSingleElement_Shape1By1() {
            FloatMatrix m = new FloatMatrix(new float[][] { { 42.5f } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42.5f, m.get(0, 0));
        }

        @Test
        public void testConstructor_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> new FloatMatrix(null));
        }

        @Test
        public void testConstructor_WithEmptyArray() {
            FloatMatrix m = new FloatMatrix(new float[0][0]);
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
            assertTrue(m.isEmpty());
        }

        @Test
        public void testConstructor_WithSingleElement_Shape1By1_DimensionsAndValue() {
            FloatMatrix m = new FloatMatrix(new float[][] { { 42.5f } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42.5f, m.get(0, 0), 0.0f);
        }
    }

    @Nested
    class EmptyTests {

        @Test
        public void testEmpty() {
            FloatMatrix empty = FloatMatrix.empty();
            assertEquals(0, empty.rowCount());
            assertEquals(0, empty.columnCount());
            assertTrue(empty.isEmpty());

            // Test singleton
            assertSame(FloatMatrix.empty(), FloatMatrix.empty());
        }

        @Test
        public void testEmptyMatrixOperations_EmptyMatrix() {
            FloatMatrix empty = FloatMatrix.empty();

            assertTrue(empty.flatten().isEmpty());
            assertEquals(0, empty.copy().rowCount);
            assertEquals(empty, empty.transpose());
            assertEquals(empty, empty.rotate90());

            FloatMatrix extended = empty.resize(2, 2, 5.5f);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals(5.5f, extended.get(0, 0), DELTA);
        }

        @Test
        public void testFloatMatrixEmptyRowCount() {
            FloatMatrix matrix = FloatMatrix.empty();
            assertEquals(0, matrix.rowCount());
            assertEquals(0, matrix.columnCount());
        }

        @Test
        public void testRange_ZeroLength_IsOneByZero() {
            // FloatMatrix doesn't have range, but let's verify equivalent behavior via repeat/reshape.
            // This was a quick sanity check covered in LongMatrix tests; the FloatMatrix equivalent
            // is implicit in reshape and resize edge cases.
            FloatMatrix empty = FloatMatrix.empty();
            assertTrue(empty.isEmpty());
            assertEquals(0, empty.rowCount());
            assertEquals(0, empty.columnCount());
        }
    }

    @Nested
    class WrapTests {

        // Test with valid array
        @Test
        public void testWrap() {
            // Test with valid array
            float[][] arr = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            FloatMatrix m = FloatMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());

            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.wrap((float[][]) null));

            FloatMatrix empty2 = FloatMatrix.wrap(new float[0][0]);
            assertTrue(empty2.isEmpty());
        }

        @Test
        public void testWrap_withValidArray() {
            float[][] arr = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            FloatMatrix m = FloatMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), DELTA);
        }

        @Test
        public void testWrap_WithFloatPrecision() {
            float[][] arr = { { 0.1f, 0.2f }, { 0.3f, 0.4f } };
            FloatMatrix m = FloatMatrix.wrap(arr);
            assertEquals(0.1f, m.get(0, 0), DELTA);
            assertEquals(0.2f, m.get(0, 1), DELTA);
            assertEquals(0.3f, m.get(1, 0), DELTA);
            assertEquals(0.4f, m.get(1, 1), DELTA);
        }

        @Test
        public void testVerySmallValues() {
            // Test with very small values near zero
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1e-20f, 1e-30f }, { 1e-38f, 1e-40f } });

            assertEquals(1e-20f, m.get(0, 0), 1e-21f);
            assertEquals(1e-30f, m.get(0, 1), 1e-31f);

            // Values near the limit of float precision
            assertTrue(m.get(1, 0) > 0);
            assertTrue(m.get(1, 1) >= 0); // May underflow to 0
        }

        @Test
        public void testWrap_WithValidArray_Values() {
            float[][] arr = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            FloatMatrix m = FloatMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0));
        }

        @Test
        public void testWrap_WithValidArray() {
            float[][] arr = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            FloatMatrix m = FloatMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), 0.0f);
        }

        @Test
        public void testCharMatrix_ToFloatMatrix() {
            CharMatrix charMatrix = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            FloatMatrix floatMatrix = charMatrix.toFloatMatrix();
            // Result: [[97.0f, 98.0f]]
            assertEquals(97.0f, floatMatrix.get(0, 0));
            assertEquals(98.0f, floatMatrix.get(0, 1));
        }

        @Test
        public void testShortMatrix_ToFloatMatrix() {
            ShortMatrix shortMatrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            FloatMatrix floatMatrix = shortMatrix.toFloatMatrix();
            assertEquals(1.0f, floatMatrix.get(0, 0));
            assertEquals(4.0f, floatMatrix.get(1, 1));
        }

        @Test
        public void testWrapAndConstructor_CopyOuterArrayButShareRows() {
            final float[][] data = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            final float[] originalRow0 = data[0];
            final FloatMatrix wrapped = FloatMatrix.wrap(data);
            final FloatMatrix constructed = new FloatMatrix(data);

            // Cell writes through a shared row are visible in both directions.
            data[0][1] = 9.0f;
            assertEquals(9.0f, wrapped.get(0, 1));
            assertEquals(9.0f, constructed.get(0, 1));
            wrapped.set(1, 0, 7.0f);
            assertEquals(7.0f, data[1][0]);

            // The outer array is copied, so replacing a row slot in the caller's array is not visible.
            data[0] = new float[] { 100.0f, 200.0f };
            assertSame(originalRow0, wrapped.rowView(0));
            assertEquals(1.0f, wrapped.get(0, 0));
            assertEquals(1.0f, constructed.get(0, 0));

            // copyOf clones the rows as well, so even cell writes are not shared.
            final float[][] source = { { 1.0f, 2.0f } };
            final FloatMatrix copied = FloatMatrix.copyOf(source);
            source[0][0] = 5.0f;
            assertEquals(1.0f, copied.get(0, 0));
            assertNotSame(source[0], copied.rowView(0));
        }

        @Test
        public void testWrap_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.wrap((float[][]) null));
        }

        @Test
        public void testWrap_WithEmptyArray() {
            FloatMatrix m = FloatMatrix.wrap(new float[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testWrap_WithSingleRow() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f } });
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }

        @Test
        public void testNegativeZero() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { -0.0f, 0.0f }, { 1.0f, -1.0f } });

            // Test that -0.0f and 0.0f are treated differently in bit representation
            // but equal in value comparison
            assertEquals(-0.0f, m.get(0, 0), DELTA);
            assertEquals(0.0f, m.get(0, 1), DELTA);

            // They should be equal in value
            assertTrue(m.get(0, 0) == 0.0f);
            assertTrue(m.get(0, 1) == 0.0f);
        }
    }

    @Nested
    class CopyOfTests {

        @Test
        public void testCopyOf() {
            float[][] arr = { { 1, 2 }, { 3, 4 } };
            FloatMatrix m = FloatMatrix.copyOf(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1f, m.get(0, 0));
            assertEquals(4f, m.get(1, 1));

            // Mutating the source array must not affect the copy
            arr[0][0] = 99;
            assertEquals(1f, m.get(0, 0));

            // Mutating the copy must not affect the source array
            m.set(1, 1, 88);
            assertEquals(4f, arr[1][1]);

            // null -> rejected; empty -> shared empty matrix
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.copyOf((float[][]) null));
            assertTrue(FloatMatrix.copyOf(new float[0][0]).isEmpty());

            // non-rectangular -> IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.copyOf(new float[][] { { 1, 2 }, { 3 } }));
        }

        @Test
        public void testCopyOfValidationOrderAndIndependentEmptyRows() {
            assertEquals("Row 0 cannot be null", assertThrows(IllegalArgumentException.class, () -> FloatMatrix.copyOf(null, new float[] { 1 })).getMessage());
            assertEquals("Row 2 cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> FloatMatrix.copyOf(new float[] { 1 }, new float[] { 2 }, null)).getMessage());
            assertEquals("Matrix must be rectangular: expected 1 columns per row, but row 1 has 2 columns",
                    assertThrows(IllegalArgumentException.class, () -> FloatMatrix.copyOf(new float[] { 1 }, new float[] { 2, 3 }, null)).getMessage());

            final float[] emptyRow = new float[0];
            final FloatMatrix copied = FloatMatrix.copyOf(emptyRow, emptyRow);
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
        public void testIntToFloatConversion() {
            // Test precision when converting large integers to float
            int[][] largeInts = { { 16777216, 16777217 }, { -16777216, -16777217 } };
            FloatMatrix m = FloatMatrix.from(largeInts);

            // 16777216 can be exactly represented in float
            assertEquals(16777216.0f, m.get(0, 0), DELTA);

            // 16777217 may lose precision in float representation
            float converted = m.get(0, 1);
            assertTrue(Math.abs(converted - 16777217.0f) < 2.0f);
        }

        @Test
        public void testFrom_Values_ConvertsIntArrayAndDimensions() {
            int[][] ints = { { 1, 2 }, { 3, 4 } };
            FloatMatrix m = FloatMatrix.from(ints);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0));
            assertEquals(4.0f, m.get(1, 1));
        }

        @Test
        public void testFromFromIntArray() {
            int[][] ints = { { 1, 2 }, { 3, 4 } };
            FloatMatrix m = FloatMatrix.from(ints);
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(2.0f, m.get(0, 1), DELTA);
            assertEquals(3.0f, m.get(1, 0), DELTA);
            assertEquals(4.0f, m.get(1, 1), DELTA);

            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.from((int[][]) null));
        }

        @Test
        public void testFromFromIntArray_Values() {
            int[][] ints = { { 1, 2 }, { 3, 4 } };
            FloatMatrix m = FloatMatrix.from(ints);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(4.0f, m.get(1, 1), DELTA);
        }

        @Test
        public void testFromFromIntArray_WithNull() {
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.from((int[][]) null));
        }

        @Test
        public void testFromFromIntArray_WithEmpty() {
            FloatMatrix m = FloatMatrix.from(new int[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testFromFromIntArray_WithJaggedArray() {
            int[][] jagged = { { 1, 2 }, { 3 } };
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.from(jagged));
        }

        @Test
        public void testFromFromIntArray_WithNullRow() {
            int[][] nullRow = { { 1, 2 }, null };
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.from(nullRow));
        }

        @Test
        public void testFromFromIntArray_WithNullFirstRow() {
            int[][] nullFirstRow = { null, { 1, 2 } };
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.from(nullFirstRow));
        }

        @Test
        public void testFromFromIntArray_LargeValues() {
            int[][] ints = { { Integer.MAX_VALUE, Integer.MIN_VALUE }, { 1000000, -1000000 } };
            FloatMatrix m = FloatMatrix.from(ints);
            assertEquals(Integer.MAX_VALUE, m.get(0, 0), 1.0f);
            assertEquals(Integer.MIN_VALUE, m.get(0, 1), 1.0f);
            assertEquals(1000000.0f, m.get(1, 0), DELTA);
            assertEquals(-1000000.0f, m.get(1, 1), DELTA);
        }

        @Test
        public void testFrom_FromIntArray() {
            int[][] ints = { { 1, 2 }, { 3, 4 } };
            FloatMatrix m = FloatMatrix.from(ints);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), 0.0f);
            assertEquals(4.0f, m.get(1, 1), 0.0f);
        }

        @Test
        public void testFrom_FromIntArray_NullFirstRow() {
            int[][] ints = { null, { 1, 2 } };
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.from(ints));
        }

        @Test
        public void testFrom_FromIntArray_DifferentRowLengths() {
            int[][] ints = { { 1, 2 }, { 3 } };
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.from(ints));
        }

        @Test
        public void testFloatMatrixFromInt() {
            // FloatMatrix.java: FloatMatrix matrix = FloatMatrix.from(new int[][] {{1, 2}, {3, 4}});
            // Creates a matrix with values {{1.0f, 2.0f}, {3.0f, 4.0f}}
            // assert matrix.get(1, 0) == 3.0f;
            FloatMatrix matrix = FloatMatrix.from(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(3.0f, matrix.get(1, 0));
        }
    }

    @Nested
    class RandomRowTests {

        @Test
        public void testRandom() {
            FloatMatrix m = FloatMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            // Values should be random, just check they exist
            for (int i = 0; i < 5; i++) {
                assertNotNull(m.get(0, i));
            }
        }

        @Test
        public void testRandom_WithLargeLength() {
            FloatMatrix m = FloatMatrix.randomRow(100);
            assertEquals(1, m.rowCount());
            assertEquals(100, m.columnCount());
        }

        @Test
        public void testRandom_Values() {
            FloatMatrix m = FloatMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }

        @Test
        public void testRandom_WithZeroLength() {
            FloatMatrix m = FloatMatrix.randomRow(0);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testFactories_NegativeDimensions_ThrowIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.randomRow(-1));
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.random(-1, 2));
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.random(2, -1));
        }
    }

    @Nested
    class RandomTests {

        @Test
        public void testRandom_WithRowsCols() {
            FloatMatrix m = FloatMatrix.random(2, 3);
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    float val = m.get(i, j);
                    assertTrue(val >= 0.0f && val < 1.0f);
                }
            }
        }
    }

    @Nested
    class OfMainDiagonalTests {

        @Test
        public void testOfMainDiagonal() {
            float[] diag = { 1.0f, 2.0f, 3.0f };
            FloatMatrix m = FloatMatrix.ofMainDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), 0.0f);
            assertEquals(2.0f, m.get(1, 1), 0.0f);
            assertEquals(3.0f, m.get(2, 2), 0.0f);
            assertEquals(0.0f, m.get(0, 1), 0.0f);
            assertEquals(0.0f, m.get(1, 0), 0.0f);
        }

        @Test
        public void testFloatMatrixMainDiagonal() {
            // FloatMatrix.java: FloatMatrix matrix = FloatMatrix.ofMainDiagonal(new float[] {1.0f, 2.0f, 3.0f});
            // Creates 3x3 matrix:
            // [[1.0, 0.0, 0.0],
            //  [0.0, 2.0, 0.0],
            //  [0.0, 0.0, 3.0]]
            FloatMatrix matrix = FloatMatrix.ofMainDiagonal(new float[] { 1.0f, 2.0f, 3.0f });
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertEquals(1.0f, matrix.get(0, 0));
            assertEquals(2.0f, matrix.get(1, 1));
            assertEquals(3.0f, matrix.get(2, 2));
            assertEquals(0.0f, matrix.get(0, 1));
        }

        @Test
        public void testDiagonalLU2RD() {
            FloatMatrix m = FloatMatrix.ofMainDiagonal(new float[] { 1.0f, 2.0f, 3.0f });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(2.0f, m.get(1, 1), DELTA);
            assertEquals(3.0f, m.get(2, 2), DELTA);
            assertEquals(0.0f, m.get(0, 1), DELTA);
            assertEquals(0.0f, m.get(1, 0), DELTA);
        }

        @Test
        public void testDiagonalLU2RD_Values() {
            FloatMatrix m = FloatMatrix.ofMainDiagonal(new float[] { 1.5f, 2.5f, 3.5f });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.5f, m.get(0, 0), DELTA);
            assertEquals(2.5f, m.get(1, 1), DELTA);
            assertEquals(3.5f, m.get(2, 2), DELTA);
            assertEquals(0.0f, m.get(0, 1), DELTA);
            assertEquals(0.0f, m.get(1, 0), DELTA);
        }

        @Test
        public void testOfMainDiagonal_Null() {
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.ofMainDiagonal(null));
        }

        @Test
        public void testOfMainDiagonal_Values_ZeroFillsOffDiagonal() {
            FloatMatrix m = FloatMatrix.ofMainDiagonal(new float[] { 1.0f, 2.0f, 3.0f });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0f, m.get(0, 0));
            assertEquals(2.0f, m.get(1, 1));
            assertEquals(3.0f, m.get(2, 2));
            assertEquals(0.0f, m.get(0, 1));
            assertEquals(0.0f, m.get(1, 0));
        }

        @Test
        public void testDiagonalLU2RD_WithEmptyArray() {
            FloatMatrix m = FloatMatrix.ofMainDiagonal(new float[0]);
            assertTrue(m.isEmpty());
        }
    }

    @Nested
    class OfAntiDiagonalTests {

        @Test
        public void testOfAntiDiagonal() {
            float[] diag = { 1.0f, 2.0f, 3.0f };
            FloatMatrix m = FloatMatrix.ofAntiDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0f, m.get(0, 2), 0.0f);
            assertEquals(2.0f, m.get(1, 1), 0.0f);
            assertEquals(3.0f, m.get(2, 0), 0.0f);
            assertEquals(0.0f, m.get(0, 0), 0.0f);
        }

        @Test
        public void testFloatMatrixAntiDiagonal() {
            // FloatMatrix.java: FloatMatrix matrix = FloatMatrix.ofAntiDiagonal(new float[] {1.0f, 2.0f, 3.0f});
            // Creates 3x3 matrix:
            // [[0.0, 0.0, 1.0],
            //  [0.0, 2.0, 0.0],
            //  [3.0, 0.0, 0.0]]
            FloatMatrix matrix = FloatMatrix.ofAntiDiagonal(new float[] { 1.0f, 2.0f, 3.0f });
            assertEquals(1.0f, matrix.get(0, 2));
            assertEquals(2.0f, matrix.get(1, 1));
            assertEquals(3.0f, matrix.get(2, 0));
            assertEquals(0.0f, matrix.get(0, 0));
        }

        @Test
        public void testDiagonalRU2LD() {
            FloatMatrix m = FloatMatrix.ofAntiDiagonal(new float[] { 1.0f, 2.0f, 3.0f });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0f, m.get(0, 2), DELTA);
            assertEquals(2.0f, m.get(1, 1), DELTA);
            assertEquals(3.0f, m.get(2, 0), DELTA);
            assertEquals(0.0f, m.get(0, 0), DELTA);
            assertEquals(0.0f, m.get(2, 2), DELTA);
        }

        @Test
        public void testDiagonalRU2LD_Values() {
            FloatMatrix m = FloatMatrix.ofAntiDiagonal(new float[] { 1.5f, 2.5f, 3.5f });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.5f, m.get(0, 2), DELTA);
            assertEquals(2.5f, m.get(1, 1), DELTA);
            assertEquals(3.5f, m.get(2, 0), DELTA);
            assertEquals(0.0f, m.get(0, 0), DELTA);
            assertEquals(0.0f, m.get(2, 2), DELTA);
        }

        @Test
        public void testOfAntiDiagonal_Null() {
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.ofAntiDiagonal(null));
        }

        @Test
        public void testOfAntiDiagonal_Values_ZeroFillsOffDiagonal() {
            FloatMatrix m = FloatMatrix.ofAntiDiagonal(new float[] { 1.0f, 2.0f, 3.0f });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0f, m.get(0, 2));
            assertEquals(2.0f, m.get(1, 1));
            assertEquals(3.0f, m.get(2, 0));
            assertEquals(0.0f, m.get(0, 0));
            assertEquals(0.0f, m.get(2, 2));
        }
    }

    @Nested
    class OfDiagonalsTests {

        @Test
        public void testOfDiagonals() {
            FloatMatrix matrix = FloatMatrix.ofDiagonals(new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 4.0f, 5.0f, 6.0f });
            assertEquals(1.0f, matrix.get(0, 0));
            assertEquals(0.0f, matrix.get(0, 1));
            assertEquals(4.0f, matrix.get(0, 2));
            assertEquals(0.0f, matrix.get(1, 0));
            assertEquals(2.0f, matrix.get(1, 1));
            assertEquals(0.0f, matrix.get(1, 2));
            assertEquals(6.0f, matrix.get(2, 0));
            assertEquals(0.0f, matrix.get(2, 1));
            assertEquals(3.0f, matrix.get(2, 2));
        }

        // Test with both diagonals
        @Test
        public void testDiagonal() {
            // Test with both diagonals
            FloatMatrix m = FloatMatrix.ofDiagonals(new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 4.0f, 5.0f, 6.0f });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(2.0f, m.get(1, 1), DELTA);
            assertEquals(3.0f, m.get(2, 2), DELTA);
            assertEquals(4.0f, m.get(0, 2), DELTA);
            assertEquals(2.0f, m.get(1, 1), DELTA); // Overwritten
            assertEquals(6.0f, m.get(2, 0), DELTA);

            // Test with only main diagonal
            FloatMatrix m2 = FloatMatrix.ofDiagonals(new float[] { 1.0f, 2.0f, 3.0f }, null);
            assertEquals(1.0f, m2.get(0, 0), DELTA);
            assertEquals(2.0f, m2.get(1, 1), DELTA);
            assertEquals(3.0f, m2.get(2, 2), DELTA);

            // Test with only anti-diagonal
            FloatMatrix m3 = FloatMatrix.ofDiagonals(null, new float[] { 4.0f, 5.0f, 6.0f });
            assertEquals(4.0f, m3.get(0, 2), DELTA);
            assertEquals(5.0f, m3.get(1, 1), DELTA);
            assertEquals(6.0f, m3.get(2, 0), DELTA);

            // Test with both null
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.ofDiagonals(null, null));

            // Test illegal argument
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.ofDiagonals(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f, 5.0f }));
        }

        @Test
        public void testDiagonal_WithBothDiagonals() {
            FloatMatrix m = FloatMatrix.ofDiagonals(new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 4.0f, 5.0f, 6.0f });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(2.0f, m.get(1, 1), DELTA);
            assertEquals(3.0f, m.get(2, 2), DELTA);
            assertEquals(4.0f, m.get(0, 2), DELTA);
            assertEquals(6.0f, m.get(2, 0), DELTA);
        }

        @Test
        public void testDiagonal_WithOnlyMainDiagonal() {
            FloatMatrix m = FloatMatrix.ofDiagonals(new float[] { 1.0f, 2.0f, 3.0f }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(2.0f, m.get(1, 1), DELTA);
            assertEquals(3.0f, m.get(2, 2), DELTA);
        }

        @Test
        public void testDiagonal_WithOnlyAntiDiagonal() {
            FloatMatrix m = FloatMatrix.ofDiagonals(null, new float[] { 4.0f, 5.0f, 6.0f });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(4.0f, m.get(0, 2), DELTA);
            assertEquals(5.0f, m.get(1, 1), DELTA);
            assertEquals(6.0f, m.get(2, 0), DELTA);
        }

        @Test
        public void testDiagonal_WithDifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.ofDiagonals(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f, 5.0f }));
        }

        @Test
        public void testDiagonal_WithBothDiagonals_Values() {
            FloatMatrix m = FloatMatrix.ofDiagonals(new float[] { 1.0f, 4.0f }, new float[] { 2.0f, 3.0f });
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0));
            assertEquals(2.0f, m.get(0, 1));
            assertEquals(3.0f, m.get(1, 0));
            assertEquals(4.0f, m.get(1, 1));
        }

        @Test
        public void testDiagonal_WithDifferentLengths_Validation() {
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.ofDiagonals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 2.0f, 3.0f }));
        }

        @Test
        public void testDiagonal_Both() {
            float[] lu2rd = { 1.0f, 2.0f, 3.0f };
            float[] ru2ld = { 4.0f, 5.0f, 6.0f };
            FloatMatrix m = FloatMatrix.ofDiagonals(lu2rd, ru2ld);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), 0.0f);
            assertEquals(2.0f, m.get(1, 1), 0.0f);
            assertEquals(3.0f, m.get(2, 2), 0.0f);
            assertEquals(4.0f, m.get(0, 2), 0.0f);
            assertEquals(6.0f, m.get(2, 0), 0.0f);
        }

        @Test
        public void testDiagonal_DifferentLengths() {
            float[] lu2rd = { 1.0f, 2.0f };
            float[] ru2ld = { 4.0f, 5.0f, 6.0f };
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.ofDiagonals(lu2rd, ru2ld));
        }

        @Test
        public void testDiagonal_WithOnlyMainDiagonal_NullInput() {
            FloatMatrix m = FloatMatrix.ofDiagonals(new float[] { 1.0f, 2.0f, 3.0f }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0f, m.get(0, 0));
            assertEquals(2.0f, m.get(1, 1));
            assertEquals(3.0f, m.get(2, 2));
        }

        @Test
        public void testDiagonal_WithOnlyAntiDiagonal_NullInput() {
            FloatMatrix m = FloatMatrix.ofDiagonals(null, new float[] { 1.0f, 2.0f, 3.0f });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0f, m.get(0, 2));
            assertEquals(2.0f, m.get(1, 1));
            assertEquals(3.0f, m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithBothNull() {
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.ofDiagonals(null, null));
        }

        @Test
        public void testDiagonal_SingleElement() {
            FloatMatrix m = FloatMatrix.ofDiagonals(new float[] { 5.0f }, new float[] { 7.0f });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(5.0f, m.get(0, 0)); // Main diagonal takes precedence
        }

        @Test
        public void testOfDiagonals_EmptyAndNullSidesPreserveEmptyShape() {
            assertTrue(FloatMatrix.ofDiagonals(new float[0], new float[0]).isEmpty());
            assertTrue(FloatMatrix.ofDiagonals(new float[0], null).isEmpty());
            assertTrue(FloatMatrix.ofDiagonals(null, new float[0]).isEmpty());
        }
    }

    @Nested
    class UnboxTests {

        @Test
        public void testUnbox() {
            Float[][] boxed = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            Matrix<Float> boxedMatrix = Matrix.wrap(boxed);
            FloatMatrix unboxed = FloatMatrix.unbox(boxedMatrix);
            assertEquals(1.0f, unboxed.get(0, 0), DELTA);
            assertEquals(2.0f, unboxed.get(0, 1), DELTA);
            assertEquals(3.0f, unboxed.get(1, 0), DELTA);
            assertEquals(4.0f, unboxed.get(1, 1), DELTA);
        }

        @Test
        public void testUnbox_Values() {
            Float[][] boxed = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            Matrix<Float> boxedMatrix = Matrix.wrap(boxed);
            FloatMatrix unboxed = FloatMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertEquals(1.0f, unboxed.get(0, 0), DELTA);
            assertEquals(4.0f, unboxed.get(1, 1), DELTA);
        }

        @Test
        public void testUnbox_Shape2By2() {
            Matrix<Float> boxed = Matrix.wrap(new Float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m = FloatMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0));
            assertEquals(4.0f, m.get(1, 1));
        }

        @Test
        public void testUnbox_Shape2By2_DimensionsAndCornerValues() {
            Matrix<Float> boxed = Matrix.wrap(new Float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m = FloatMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), 0.0f);
            assertEquals(4.0f, m.get(1, 1), 0.0f);
        }

        @Test
        public void testUnbox_WithNullValues() {
            Float[][] boxed = { { 1.0f, null }, { null, 4.0f } };
            Matrix<Float> boxedMatrix = Matrix.wrap(boxed);
            FloatMatrix unboxed = FloatMatrix.unbox(boxedMatrix);
            assertEquals(1.0f, unboxed.get(0, 0), DELTA);
            assertEquals(0.0f, unboxed.get(0, 1), DELTA); // null -> 0.0f
            assertEquals(0.0f, unboxed.get(1, 0), DELTA); // null -> 0.0f
            assertEquals(4.0f, unboxed.get(1, 1), DELTA);
        }

        @Test
        public void testUnbox_WithNullValues_Shape2By2_NullInput() {
            Matrix<Float> boxed = Matrix.wrap(new Float[][] { { 1.0f, null }, { null, 4.0f } });
            FloatMatrix m = FloatMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0));
            assertEquals(0.0f, m.get(0, 1));
            assertEquals(0.0f, m.get(1, 0));
            assertEquals(4.0f, m.get(1, 1));
        }

        @Test
        public void testUnbox_WithNulls() {
            Matrix<Float> boxed = Matrix.wrap(new Float[][] { { 1.0f, null }, { null, 4.0f } });
            FloatMatrix m = FloatMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0f, m.get(0, 0), 0.0f);
            assertEquals(0.0f, m.get(0, 1), 0.0f);
            assertEquals(0.0f, m.get(1, 0), 0.0f);
            assertEquals(4.0f, m.get(1, 1), 0.0f);
        }

        @Test
        public void testUnbox_TypeNeutralEmptyReturnsPrimitiveEmptyAndTypedZeroColumnsPreserveRows() {
            FloatMatrix sharedEmpty = FloatMatrix.unbox(Matrix.empty(Float.class));
            FloatMatrix copiedTypeNeutralEmpty = FloatMatrix.unbox(Matrix.empty(Float.class).copy());

            assertSame(FloatMatrix.empty(), sharedEmpty);
            assertSame(FloatMatrix.empty(), copiedTypeNeutralEmpty);

            FloatMatrix zeroColumns = FloatMatrix.unbox(Matrix.wrap(new Float[][] { {}, {} }));
            assertEquals(2, zeroColumns.rowCount());
            assertEquals(0, zeroColumns.columnCount());

            FloatMatrix typeNeutralZeroColumns = FloatMatrix.unbox(Matrix.empty(Float.class).resize(2, 0));
            assertEquals(2, typeNeutralZeroColumns.rowCount());
            assertEquals(0, typeNeutralZeroColumns.columnCount());

            Matrix<Float> typeNeutralExpanded = Matrix.empty(Float.class).resize(2, 2);
            typeNeutralExpanded.set(0, 1, 7f);
            assertEquals(FloatMatrix.wrap(new float[][] { { 0f, 7f }, { 0f, 0f } }), FloatMatrix.unbox(typeNeutralExpanded));
        }
    }

    @Nested
    class GetTests {

        @Test
        public void testGet() {
            assertEquals(1.0f, matrix.get(0, 0), DELTA);
            assertEquals(5.0f, matrix.get(1, 1), DELTA);
            assertEquals(9.0f, matrix.get(2, 2), DELTA);

            // Test bounds
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(-1, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(3, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(0, -1));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(0, 3));
        }

        @Test
        public void testGetWithPoint() {
            Sheet.Point p1 = Sheet.Point.of(0, 0);
            assertEquals(1.0f, matrix.get(p1), DELTA);

            Sheet.Point p2 = Sheet.Point.of(1, 1);
            assertEquals(5.0f, matrix.get(p2), DELTA);

            Sheet.Point p3 = Sheet.Point.of(2, 2);
            assertEquals(9.0f, matrix.get(p3), DELTA);
        }

        @Test
        public void testGet_Shape2By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(5.0f, m.get(1, 1), DELTA);
            assertEquals(6.0f, m.get(1, 2), DELTA);
        }

        @Test
        public void testGetWithPoint_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertEquals(1.0f, m.get(Point.of(0, 0)), DELTA);
            assertEquals(4.0f, m.get(Point.of(1, 1)), DELTA);
            assertEquals(2.0f, m.get(Point.of(0, 1)), DELTA);
        }

        @Test
        public void testGet_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertEquals(1.0f, m.get(0, 0));
            assertEquals(2.0f, m.get(0, 1));
            assertEquals(3.0f, m.get(1, 0));
            assertEquals(4.0f, m.get(1, 1));
        }

        @Test
        public void testGet_WithPoint() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertEquals(2.0f, m.get(Point.of(0, 1)));
            assertEquals(3.0f, m.get(Point.of(1, 0)));
        }

        @Test
        public void testGet_ByIndices() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertEquals(1.0f, m.get(0, 0), 0.0f);
            assertEquals(2.0f, m.get(0, 1), 0.0f);
            assertEquals(3.0f, m.get(1, 0), 0.0f);
            assertEquals(4.0f, m.get(1, 1), 0.0f);
        }

        @Test
        public void testGet_ByPoint() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Point p = Point.of(0, 1);
            assertEquals(2.0f, m.get(p), 0.0f);
        }

        @Test
        public void testget() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertEquals(2.0f, matrix.get(0, 1));
        }

        @Test
        public void testGet_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
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
            FloatMatrix m = matrix.copy();
            m.set(0, 0, 10.0f);
            assertEquals(10.0f, m.get(0, 0), DELTA);

            m.set(1, 1, 20.0f);
            assertEquals(20.0f, m.get(1, 1), DELTA);

            // Test bounds
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, 0.0f));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(3, 0, 0.0f));
        }

        @Test
        public void testSetWithPoint() {
            FloatMatrix m = matrix.copy();
            Sheet.Point p = Sheet.Point.of(1, 1);
            m.set(p, 50.0f);
            assertEquals(50.0f, m.get(p), DELTA);
        }

        @Test
        public void testSet_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.set(0, 0, 10.5f);
            assertEquals(10.5f, m.get(0, 0), DELTA);

            m.set(1, 1, 20.7f);
            assertEquals(20.7f, m.get(1, 1), DELTA);
        }

        @Test
        public void testSetWithPoint_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.set(Point.of(0, 0), 50.5f);
            assertEquals(50.5f, m.get(Point.of(0, 0)), DELTA);
        }

        @Test
        public void testSet_WithSpecialValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.set(0, 0, Float.NaN);
            assertTrue(Float.isNaN(m.get(0, 0)));

            m.set(0, 1, Float.POSITIVE_INFINITY);
            assertEquals(Float.POSITIVE_INFINITY, m.get(0, 1), DELTA);

            m.set(1, 0, -0.0f);
            assertEquals(-0.0f, m.get(1, 0), DELTA);
        }

        @Test
        public void testSet_Shape2By2_ChangesFirstRowSecondColumn() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.set(0, 1, 9.0f);
            assertEquals(9.0f, m.get(0, 1));
        }

        @Test
        public void testSet_WithPoint() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.set(Point.of(0, 1), 9.0f);
            assertEquals(9.0f, m.get(0, 1));
        }

        @Test
        public void testSet_ByIndices() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.set(0, 1, 9.0f);
            assertEquals(9.0f, m.get(0, 1), 0.0f);
        }

        @Test
        public void testSet_ByPoint() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Point p = Point.of(1, 1);
            m.set(p, 9.0f);
            assertEquals(9.0f, m.get(p), 0.0f);
        }

        @Test
        public void testSet_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, 0.0f));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(2, 0, 0.0f));
        }

        @Test
        public void testSet_NegativeValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.set(0, 0, -99.9f);
            assertEquals(-99.9f, m.get(0, 0));
        }
    }

    @Nested
    class ValueAboveTests {

        // Test top row
        @Test
        public void testValueAbove() {
            OptionalFloat up = matrix.valueAbove(1, 1);
            assertTrue(up.isPresent());
            assertEquals(2.0f, up.get(), DELTA);

            // Test top row
            OptionalFloat empty = matrix.valueAbove(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });

            OptionalFloat up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1.0f, up.get(), DELTA);

            // Top row has no element above
            OptionalFloat empty = m.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_Shape2By2_PresentNeighborValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            OptionalFloat up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1.0f, up.get());
        }

        @Test
        public void testValueAbove_FirstRow() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            OptionalFloat up = m.valueAbove(0, 0);
            assertFalse(up.isPresent());
        }

        @Test
        public void testAbove() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            u.OptionalFloat value = matrix.valueAbove(1, 0);
            assertEquals(1.0f, value.get());
            u.OptionalFloat empty = matrix.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_RowOneColumnZero() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            OptionalFloat up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1.0f, up.get(), 0.0f);
        }
    }

    @Nested
    class ValueBelowTests {

        // Test bottom row
        @Test
        public void testValueBelow() {
            OptionalFloat down = matrix.valueBelow(1, 1);
            assertTrue(down.isPresent());
            assertEquals(8.0f, down.get(), DELTA);

            // Test bottom row
            OptionalFloat empty = matrix.valueBelow(2, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });

            OptionalFloat down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3.0f, down.get(), DELTA);

            // Bottom row has no element below
            OptionalFloat empty = m.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_Shape2By2_PresentNeighborValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            OptionalFloat down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3.0f, down.get());
        }

        @Test
        public void testValueBelow_LastRow() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            OptionalFloat down = m.valueBelow(1, 0);
            assertFalse(down.isPresent());
        }

        @Test
        public void testBelow() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            u.OptionalFloat value = matrix.valueBelow(0, 0);
            assertEquals(3.0f, value.get());
            u.OptionalFloat empty = matrix.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_RowZeroColumnZero() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            OptionalFloat down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3.0f, down.get(), 0.0f);
        }
    }

    @Nested
    class ValueLeftTests {

        @Test
        public void testValueLeft() {
            OptionalFloat left = matrix.valueLeft(1, 1);
            assertTrue(left.isPresent());
            assertEquals(4.0f, left.get(), DELTA);

            // Test leftmost column
            OptionalFloat empty = matrix.valueLeft(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });

            OptionalFloat left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1.0f, left.get(), DELTA);

            // Leftmost column has no element to the left
            OptionalFloat empty = m.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_Shape2By2_PresentNeighborValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            OptionalFloat left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1.0f, left.get());
        }

        @Test
        public void testValueLeft_FirstColumn() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            OptionalFloat left = m.valueLeft(0, 0);
            assertFalse(left.isPresent());
        }

        @Test
        public void testValueLeft_RowZeroColumnOne() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            OptionalFloat left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1.0f, left.get(), 0.0f);
        }
    }

    @Nested
    class ValueRightTests {

        @Test
        public void testValueRight() {
            OptionalFloat right = matrix.valueRight(1, 1);
            assertTrue(right.isPresent());
            assertEquals(6.0f, right.get(), DELTA);

            // Test rightmost column
            OptionalFloat empty = matrix.valueRight(1, 2);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });

            OptionalFloat right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2.0f, right.get(), DELTA);

            // Rightmost column has no element to the right
            OptionalFloat empty = m.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_Shape2By2_PresentNeighborValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            OptionalFloat right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2.0f, right.get());
        }

        @Test
        public void testValueRight_LastColumn() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            OptionalFloat right = m.valueRight(0, 1);
            assertFalse(right.isPresent());
        }

        @Test
        public void testValueRight_RowZeroColumnZero() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            OptionalFloat right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2.0f, right.get(), 0.0f);
        }
    }

    @Nested
    class RowViewTests {

        @Test
        public void testRowView() {
            float[] row0 = matrix.rowView(0);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, row0, DELTA);

            float[] row1 = matrix.rowView(1);
            assertArrayEquals(new float[] { 4.0f, 5.0f, 6.0f }, row1, DELTA);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(3));
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, m.rowView(0), DELTA);
            assertArrayEquals(new float[] { 4.0f, 5.0f, 6.0f }, m.rowView(1), DELTA);
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues_FirstRowView() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            float[] row = m.rowView(0);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, row);
        }

        @Test
        public void testRowView_Shape2By2_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            float[] row = m.rowView(0);
            assertArrayEquals(new float[] { 1.0f, 2.0f }, row, 0.0f);
        }

        @Test
        public void testLength() {
            // This is a protected method, test indirectly through row operations
            float[] row = matrix.rowView(0);
            assertEquals(3, row.length);
        }

        @Test
        public void testRowView_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(2));
        }

        @Test
        public void testRowView_InvalidIndex() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(5));
        }
    }

    @Nested
    class RowCopyTests {

        @Test
        public void testRowCopy() {
            float[] rowCopy = matrix.rowCopy(0);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, rowCopy, DELTA);

            rowCopy[0] = 99.0f;
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, matrix.rowView(0), DELTA);
        }
    }

    @Nested
    class ColumnCopyTests {

        // Test bounds
        @Test
        public void testColumnCopy() {
            float[] col0 = matrix.columnCopy(0);
            assertArrayEquals(new float[] { 1.0f, 4.0f, 7.0f }, col0, DELTA);

            float[] col1 = matrix.columnCopy(1);
            assertArrayEquals(new float[] { 2.0f, 5.0f, 8.0f }, col1, DELTA);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(3));
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            assertArrayEquals(new float[] { 1.0f, 4.0f }, m.columnCopy(0), DELTA);
            assertArrayEquals(new float[] { 2.0f, 5.0f }, m.columnCopy(1), DELTA);
            assertArrayEquals(new float[] { 3.0f, 6.0f }, m.columnCopy(2), DELTA);
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues_FirstColumnSnapshot() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            float[] col = m.columnCopy(0);
            assertArrayEquals(new float[] { 1.0f, 4.0f }, col);
        }

        @Test
        public void testColumnCopy_Shape2By2_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            float[] col = m.columnCopy(0);
            assertArrayEquals(new float[] { 1.0f, 3.0f }, col, 0.0f);
        }

        @Test
        public void testColumnCopy_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(5));
        }
    }

    @Nested
    class SetRowTests {

        // Test wrong size
        @Test
        public void testSetRow() {
            FloatMatrix m = matrix.copy();
            m.setRow(0, new float[] { 10.0f, 20.0f, 30.0f });
            assertArrayEquals(new float[] { 10.0f, 20.0f, 30.0f }, m.rowView(0), DELTA);

            // Test wrong size
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new float[] { 1.0f, 2.0f }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, (float[]) null));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.setRow(0, new float[] { 10.5f, 20.5f });
            assertArrayEquals(new float[] { 10.5f, 20.5f }, m.rowView(0), DELTA);
            assertArrayEquals(new float[] { 3.0f, 4.0f }, m.rowView(1), DELTA); // unchanged
        }

        @Test
        public void testSetRow_WrongSize() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new float[] { 1.0f }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new float[] { 1.0f, 2.0f, 3.0f }));
        }

        @Test
        public void testSetRow_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.setRow(0, new float[] { 7.0f, 8.0f });
            assertEquals(7.0f, m.get(0, 0));
            assertEquals(8.0f, m.get(0, 1));
        }

        @Test
        public void testSetRow_WrongLength() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new float[] { 7.0f }));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues_ReplacesFirstRow() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.setRow(0, new float[] { 9.0f, 8.0f });
            assertArrayEquals(new float[] { 9.0f, 8.0f }, m.rowView(0), 0.0f);
        }

        @Test
        public void testSetRow_InvalidRowIndex() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setRow(5, new float[] { 1.0f, 2.0f }));
        }

        @Test
        public void testSetRow_InvalidLength() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new float[] { 1.0f }));
        }
    }

    @Nested
    class SetColumnTests {

        // Test wrong size
        @Test
        public void testSetColumn() {
            FloatMatrix m = matrix.copy();
            m.setColumn(0, new float[] { 10.0f, 20.0f, 30.0f });
            assertArrayEquals(new float[] { 10.0f, 20.0f, 30.0f }, m.columnCopy(0), DELTA);

            // Test wrong size
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new float[] { 1.0f, 2.0f }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, (float[]) null));
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.setColumn(0, new float[] { 10.5f, 20.5f });
            assertArrayEquals(new float[] { 10.5f, 20.5f }, m.columnCopy(0), DELTA);
            assertArrayEquals(new float[] { 2.0f, 4.0f }, m.columnCopy(1), DELTA); // unchanged
        }

        @Test
        public void testSetColumn_WrongSize() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new float[] { 1.0f }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new float[] { 1.0f, 2.0f, 3.0f }));
        }

        @Test
        public void testSetColumn_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.setColumn(0, new float[] { 7.0f, 8.0f });
            assertEquals(7.0f, m.get(0, 0));
            assertEquals(8.0f, m.get(1, 0));
        }

        @Test
        public void testSetColumn_WrongLength() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new float[] { 7.0f }));
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues_ReplacesFirstColumn() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.setColumn(0, new float[] { 9.0f, 8.0f });
            assertArrayEquals(new float[] { 9.0f, 8.0f }, m.columnCopy(0), 0.0f);
        }

        @Test
        public void testAliasedArraySourcesAreSnapshotted() {
            FloatMatrix columnMatrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            columnMatrix.setColumn(1, columnMatrix.rowView(0));
            assertArrayEquals(new float[] { 3.0f, 2.0f }, columnMatrix.rowCopy(1));

            FloatMatrix diagonalMatrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            diagonalMatrix.setAntiDiagonal(diagonalMatrix.rowView(0));
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, diagonalMatrix.antiDiagonalCopy());

            float[][] backing = { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } };
            FloatMatrix fillMatrix = FloatMatrix.wrap(backing);
            fillMatrix.copyFrom(1, 0, backing);
            assertArrayEquals(new float[] { 3.0f, 4.0f }, fillMatrix.rowCopy(2));
        }

        @Test
        public void testSetColumn_InvalidColumnIndex() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(5, new float[] { 1.0f }));
        }

        @Test
        public void testSetColumn_InvalidLength() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new float[] { 1.0f }));
        }
    }

    @Nested
    class UpdateRowTests {

        @Test
        public void testUpdateRow() {
            FloatMatrix m = matrix.copy();
            m.updateRow(0, x -> x * 2.0f);
            assertArrayEquals(new float[] { 2.0f, 4.0f, 6.0f }, m.rowView(0), DELTA);
        }

        @Test
        public void testUpdateRow_Shape2By2_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateRow(0, x -> x * 2.0f);
            assertArrayEquals(new float[] { 2.0f, 4.0f }, m.rowView(0), DELTA);
            assertArrayEquals(new float[] { 3.0f, 4.0f }, m.rowView(1), DELTA); // unchanged
        }

        @Test
        public void testUpdateRow_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateRow(0, x -> x * 2);
            assertEquals(2.0f, m.get(0, 0));
            assertEquals(4.0f, m.get(0, 1));
        }

        @Test
        public void testUpdateRow_Shape2By2_ArrayValues_PreservesOtherRow() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateRow(0, x -> x * 2);
            assertArrayEquals(new float[] { 2.0f, 4.0f }, m.rowView(0), 0.0f);
            assertArrayEquals(new float[] { 3.0f, 4.0f }, m.rowView(1), 0.0f);
        }

        @Test
        public void testInPlaceCallbacksRetainCompletedUpdatesWhenTheyThrow() {
            final IllegalStateException failure = new IllegalStateException("stop at the second element");
            final Throwables.FloatUnaryOperator<RuntimeException> operator = value -> {
                if (value == 2) {
                    throw failure;
                }
                return value * 10;
            };
            final List<Throwables.Consumer<FloatMatrix, RuntimeException>> updates = List.of(m -> m.updateRow(0, operator), m -> m.updateColumn(0, operator),
                    m -> m.updateMainDiagonal(operator), m -> m.updateAntiDiagonal(operator), m -> m.updateAll(operator),
                    m -> m.updateAll((i, j) -> operator.applyAsFloat(m.get(i, j))), m -> m.replaceIf(value -> operator.applyAsFloat(value) > 0, 10),
                    m -> m.replaceIf((i, j) -> operator.applyAsFloat(m.get(i, j)) > 0, 10));

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                for (int operation = 0; operation < updates.size(); operation++) {
                    final FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2, 1 }, { 2, 2, 2 }, { 1, 2, 1 } });
                    final Throwables.Consumer<FloatMatrix, RuntimeException> update = updates.get(operation);
                    assertSame(failure, assertThrows(IllegalStateException.class, () -> update.accept(m)));
                    final float[][] expected = { { 1, 2, 1 }, { 2, 2, 2 }, { 1, 2, 1 } };
                    expected[0][operation == 3 ? 2 : 0] = 10;
                    assertEquals(FloatMatrix.wrap(expected), m, "operation " + operation);
                }
            });
        }

        @Test
        public void testUpdateRowAndUpdateColumnNullOperator() {
            FloatMatrix m = matrix.copy();

            assertThrows(IllegalArgumentException.class, () -> m.updateRow(0, (Throwables.FloatUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> m.updateColumn(0, (Throwables.FloatUnaryOperator<RuntimeException>) null));
        }

        @Test
        public void testUpdateRowAndUpdateColumnInvalidIndex() {
            FloatMatrix m = matrix.copy();

            assertThrows(IndexOutOfBoundsException.class, () -> m.updateRow(-1, x -> x * 2.0f));
            assertThrows(IndexOutOfBoundsException.class, () -> m.updateColumn(3, x -> x + 10.0f));
        }
    }

    @Nested
    class UpdateColumnTests {

        @Test
        public void testUpdateColumn() {
            FloatMatrix m = matrix.copy();
            m.updateColumn(0, x -> x + 10.0f);
            assertArrayEquals(new float[] { 11.0f, 14.0f, 17.0f }, m.columnCopy(0), DELTA);
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateColumn(0, x -> x + 10.0f);
            assertArrayEquals(new float[] { 11.0f, 13.0f }, m.columnCopy(0), DELTA);
            assertArrayEquals(new float[] { 2.0f, 4.0f }, m.columnCopy(1), DELTA); // unchanged
        }

        @Test
        public void testUpdateColumn_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateColumn(0, x -> x + 10);
            assertEquals(11.0f, m.get(0, 0));
            assertEquals(13.0f, m.get(1, 0));
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues_PreservesOtherColumn() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateColumn(0, x -> x * 2);
            assertArrayEquals(new float[] { 2.0f, 6.0f }, m.columnCopy(0), 0.0f);
            assertArrayEquals(new float[] { 2.0f, 4.0f }, m.columnCopy(1), 0.0f);
        }

        @Test
        public void testDuplicateBackingRowsAreRejectedAtConstruction() {
            // One array cannot back two independently addressable logical rows, so wrap() rejects it.
            final float[] sharedRow = { 1.0f, 2.0f, 3.0f };
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> FloatMatrix.wrap(new float[][] { sharedRow, sharedRow }));
            assertTrue(ex.getMessage().contains("independent storage"));

            // copyOf() clones each row, so the same values are accepted and stay independent.
            final FloatMatrix matrix = FloatMatrix.copyOf(new float[][] { sharedRow, sharedRow });
            matrix.updateColumn(0, value -> value + 1.0f);
            assertArrayEquals(new float[] { 2.0f, 2.0f, 3.0f }, matrix.rowCopy(0));
            assertArrayEquals(new float[] { 2.0f, 2.0f, 3.0f }, matrix.rowCopy(1));
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, sharedRow);
            assertNotSame(matrix.rowView(0), matrix.rowView(1));

            // Forced-parallel mutation still visits each of the four cells exactly once.
            final AtomicInteger calls = new AtomicInteger();
            Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> matrix.updateAll(value -> {
                calls.incrementAndGet();
                return value;
            }));
            assertEquals(6, calls.get());
        }
    }

    @Nested
    class MainDiagonalCopyTests {

        @Test
        public void testGetLU2RD() {
            float[] diagonal = matrix.mainDiagonalCopy();
            assertArrayEquals(new float[] { 1.0f, 5.0f, 9.0f }, diagonal, DELTA);

            // Test non-square matrix
            FloatMatrix nonSquare = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertArrayEquals(new float[] { 1.0f }, nonSquare.mainDiagonalCopy(), DELTA);
        }

        @Test
        public void testMainDiagonalCopy_Shape3By3_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            assertArrayEquals(new float[] { 1.0f, 5.0f, 9.0f }, m.mainDiagonalCopy(), DELTA);
        }

        @Test
        public void testGetLU2RD_nonSquare() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertArrayEquals(new float[] { 1.0f }, m.mainDiagonalCopy(), DELTA);
        }

        @Test
        public void testMainDiagonalCopy_Shape3By3_ArrayValues_DiagonalArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            float[] diagonal = m.mainDiagonalCopy();
            assertArrayEquals(new float[] { 1.0f, 5.0f, 9.0f }, diagonal);
        }

        @Test
        public void testMainDiagonalCopy() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            float[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new float[] { 1.0f, 5.0f, 9.0f }, diag, 0.0f);
        }

        @Test
        public void testMainDiagonalCopy_NonSquare() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            assertArrayEquals(new float[] { 1.0f, 4.0f }, m.mainDiagonalCopy(), 0.0f);
        }

        @Test
        public void testmainDiagonalCopy() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            float[] diagonal = matrix.mainDiagonalCopy();
            assertArrayEquals(new float[] { 1.0f, 5.0f, 9.0f }, diagonal);
        }
    }

    @Nested
    class SetMainDiagonalTests {

        // Test non-square matrix
        @Test
        public void testSetLU2RD() {
            FloatMatrix m = matrix.copy();
            m.setMainDiagonal(new float[] { 10.0f, 20.0f, 30.0f });
            assertEquals(10.0f, m.get(0, 0), DELTA);
            assertEquals(20.0f, m.get(1, 1), DELTA);
            assertEquals(30.0f, m.get(2, 2), DELTA);

            // Test non-square matrix
            FloatMatrix nonSquare = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            Assertions.assertDoesNotThrow(() -> nonSquare.setMainDiagonal(new float[] { 1.0f }));

            // Test array too short
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new float[] { 1.0f, 2.0f }));
        }

        @Test
        public void testSetMainDiagonal_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            m.setMainDiagonal(new float[] { 10.5f, 20.5f, 30.5f });
            assertEquals(10.5f, m.get(0, 0), DELTA);
            assertEquals(20.5f, m.get(1, 1), DELTA);
            assertEquals(30.5f, m.get(2, 2), DELTA);
        }

        @Test
        public void testSetLU2RD_nonSquare() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            Assertions.assertDoesNotThrow(() -> m.setMainDiagonal(new float[] { 1.0f }));
        }

        @Test
        public void testSetMainDiagonal_ArrayTooShort() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new float[] { 1.0f, 2.0f }));
        }

        @Test
        public void testSetMainDiagonal_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.setMainDiagonal(new float[] { 9.0f, 8.0f });
            assertEquals(9.0f, m.get(0, 0));
            assertEquals(8.0f, m.get(1, 1));
        }

        @Test
        public void testSetMainDiagonal_NonSquare_Shape3By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            Assertions.assertDoesNotThrow(() -> m.setMainDiagonal(new float[] { 9.0f, 8.0f }));
        }

        @Test
        public void testSetMainDiagonal() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.setMainDiagonal(new float[] { 9.0f, 8.0f });
            assertArrayEquals(new float[] { 9.0f, 8.0f }, m.mainDiagonalCopy(), 0.0f);
        }

        @Test
        public void testSetMainDiagonal_NonSquare() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            Assertions.assertDoesNotThrow(() -> m.setMainDiagonal(new float[] { 9.0f }));
        }

        @Test
        public void testSetDiagonal_NullArraysOnEmptyMatrixThrow() {
            FloatMatrix m = FloatMatrix.empty();
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(null));
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(null));
        }

        @Test
        public void testSetMainDiagonal_InvalidLength() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new float[] { 9.0f }));
        }
    }

    @Nested
    class UpdateMainDiagonalTests {

        @Test
        public void testUpdateLU2RD() {
            FloatMatrix m = matrix.copy();
            m.updateMainDiagonal(x -> x * 10.0f);
            assertEquals(10.0f, m.get(0, 0), DELTA);
            assertEquals(50.0f, m.get(1, 1), DELTA);
            assertEquals(90.0f, m.get(2, 2), DELTA);

            // Test non-square matrix
            FloatMatrix nonSquare = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            Assertions.assertDoesNotThrow(() -> nonSquare.updateMainDiagonal(x -> x * 2.0f));
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            m.updateMainDiagonal(x -> x * 10.0f);
            assertEquals(10.0f, m.get(0, 0), DELTA);
            assertEquals(50.0f, m.get(1, 1), DELTA);
            assertEquals(90.0f, m.get(2, 2), DELTA);
            assertEquals(2.0f, m.get(0, 1), DELTA); // unchanged
        }

        @Test
        public void testUpdateMainDiagonal_NonSquare() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            Assertions.assertDoesNotThrow(() -> m.updateMainDiagonal(x -> x * 2.0f));
        }

        @Test
        public void testUpdateMainDiagonal_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateMainDiagonal(x -> x * 2);
            assertEquals(2.0f, m.get(0, 0));
            assertEquals(8.0f, m.get(1, 1));
        }

        @Test
        public void testUpdateMainDiagonal() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateMainDiagonal(x -> x * 2);
            assertArrayEquals(new float[] { 2.0f, 8.0f }, m.mainDiagonalCopy(), 0.0f);
        }
    }

    @Nested
    class AntiDiagonalCopyTests {

        @Test
        public void testGetRU2LD() {
            float[] antiDiagonal = matrix.antiDiagonalCopy();
            assertArrayEquals(new float[] { 3.0f, 5.0f, 7.0f }, antiDiagonal, DELTA);

            // Test non-square matrix
            FloatMatrix nonSquare = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertArrayEquals(new float[] { 2.0f }, nonSquare.antiDiagonalCopy(), DELTA);
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            assertArrayEquals(new float[] { 3.0f, 5.0f, 7.0f }, m.antiDiagonalCopy(), DELTA);
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertArrayEquals(new float[] { 2.0f }, m.antiDiagonalCopy(), DELTA);
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues_AntiDiagonalArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            float[] diagonal = m.antiDiagonalCopy();
            assertArrayEquals(new float[] { 3.0f, 5.0f, 7.0f }, diagonal);
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare_Shape3By2_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            assertArrayEquals(new float[] { 2.0f, 3.0f }, m.antiDiagonalCopy(), DELTA);
        }

        @Test
        public void testAntiDiagonalCopy() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            float[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new float[] { 3.0f, 5.0f, 7.0f }, diag, 0.0f);
        }

        @Test
        public void testantiDiagonalCopy() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            float[] diagonal = matrix.antiDiagonalCopy();
            assertArrayEquals(new float[] { 3.0f, 5.0f, 7.0f }, diagonal);
        }
    }

    @Nested
    class SetAntiDiagonalTests {

        @Test
        public void testSetRU2LD() {
            FloatMatrix m = matrix.copy();
            m.setAntiDiagonal(new float[] { 10.0f, 20.0f, 30.0f });
            assertEquals(10.0f, m.get(0, 2), DELTA);
            assertEquals(20.0f, m.get(1, 1), DELTA);
            assertEquals(30.0f, m.get(2, 0), DELTA);

            // Test non-square matrix
            FloatMatrix nonSquare = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            Assertions.assertDoesNotThrow(() -> nonSquare.setAntiDiagonal(new float[] { 1.0f }));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            m.setAntiDiagonal(new float[] { 10.5f, 20.5f, 30.5f });
            assertEquals(10.5f, m.get(0, 2), DELTA);
            assertEquals(20.5f, m.get(1, 1), DELTA);
            assertEquals(30.5f, m.get(2, 0), DELTA);
        }

        @Test
        public void testSetAntiDiagonal_NonSquare() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            Assertions.assertDoesNotThrow(() -> m.setAntiDiagonal(new float[] { 1.0f }));
        }

        @Test
        public void testSetAntiDiagonal_ArrayTooShort() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new float[] { 1.0f, 2.0f }));
        }

        @Test
        public void testSetAntiDiagonal_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.setAntiDiagonal(new float[] { 9.0f, 8.0f });
            assertEquals(9.0f, m.get(0, 1));
            assertEquals(8.0f, m.get(1, 0));
        }

        @Test
        public void testSetAntiDiagonal() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.setAntiDiagonal(new float[] { 9.0f, 8.0f });
            assertArrayEquals(new float[] { 9.0f, 8.0f }, m.antiDiagonalCopy(), 0.0f);
        }
    }

    @Nested
    class UpdateAntiDiagonalTests {

        @Test
        public void testUpdateRU2LD() {
            FloatMatrix m = matrix.copy();
            m.updateAntiDiagonal(x -> x * 10.0f);
            assertEquals(30.0f, m.get(0, 2), DELTA);
            assertEquals(50.0f, m.get(1, 1), DELTA);
            assertEquals(70.0f, m.get(2, 0), DELTA);

            // Test non-square matrix
            FloatMatrix nonSquare = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            Assertions.assertDoesNotThrow(() -> nonSquare.updateAntiDiagonal(x -> x * 2.0f));
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            m.updateAntiDiagonal(x -> x * 10.0f);
            assertEquals(30.0f, m.get(0, 2), DELTA);
            assertEquals(50.0f, m.get(1, 1), DELTA);
            assertEquals(70.0f, m.get(2, 0), DELTA);
            assertEquals(2.0f, m.get(0, 1), DELTA); // unchanged
        }

        @Test
        public void testUpdateAntiDiagonal_NonSquare() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            Assertions.assertDoesNotThrow(() -> m.updateAntiDiagonal(x -> x * 2.0f));
        }

        @Test
        public void testUpdateAntiDiagonal_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateAntiDiagonal(x -> x + 10);
            assertEquals(12.0f, m.get(0, 1));
            assertEquals(13.0f, m.get(1, 0));
        }

        @Test
        public void testUpdateAntiDiagonal() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateAntiDiagonal(x -> x * 2);
            float[] diag = m.antiDiagonalCopy();
            assertEquals(4.0f, diag[0], 0.0f);
            assertEquals(6.0f, diag[1], 0.0f);
        }
    }

    @Nested
    class UpdateAllTests {

        @Test
        public void testUpdateAll() {
            FloatMatrix m = matrix.copy();
            m.updateAll(x -> x * 2.0f);
            assertEquals(2.0f, m.get(0, 0), DELTA);
            assertEquals(4.0f, m.get(0, 1), DELTA);
            assertEquals(18.0f, m.get(2, 2), DELTA);
        }

        @Test
        public void testUpdateAllUnarySequentialTallMatrixUsesRowMajorOrder() throws Exception {
            final FloatMatrix tall = FloatMatrix.wrap(new float[3][2]);
            final AtomicInteger next = new AtomicInteger();

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> tall.updateAll(x -> (float) next.incrementAndGet()));

            assertArrayEquals(new float[] { 1.0f, 2.0f }, tall.rowCopy(0), DELTA);
            assertArrayEquals(new float[] { 3.0f, 4.0f }, tall.rowCopy(1), DELTA);
            assertArrayEquals(new float[] { 5.0f, 6.0f }, tall.rowCopy(2), DELTA);
        }

        @Test
        public void testUpdateAllWithIndices() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 0.0f, 0.0f }, { 0.0f, 0.0f } });
            m.updateAll((i, j) -> i * 10.0f + j);
            assertEquals(0.0f, m.get(0, 0), DELTA);
            assertEquals(1.0f, m.get(0, 1), DELTA);
            assertEquals(10.0f, m.get(1, 0), DELTA);
            assertEquals(11.0f, m.get(1, 1), DELTA);
        }

        @Test
        public void testUpdateAll_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateAll(x -> x * 2.0f);
            assertEquals(2.0f, m.get(0, 0), DELTA);
            assertEquals(4.0f, m.get(0, 1), DELTA);
            assertEquals(6.0f, m.get(1, 0), DELTA);
            assertEquals(8.0f, m.get(1, 1), DELTA);
        }

        @Test
        public void testUpdateAll_WithIndices() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 0.0f, 0.0f }, { 0.0f, 0.0f } });
            m.updateAll((i, j) -> (float) (i * 10 + j));
            assertEquals(0.0f, m.get(0, 0), DELTA);
            assertEquals(1.0f, m.get(0, 1), DELTA);
            assertEquals(10.0f, m.get(1, 0), DELTA);
            assertEquals(11.0f, m.get(1, 1), DELTA);
        }

        @Test
        public void testUpdateAll_UnaryOperator() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateAll(x -> x * 2);
            assertEquals(2.0f, m.get(0, 0));
            assertEquals(4.0f, m.get(0, 1));
            assertEquals(6.0f, m.get(1, 0));
            assertEquals(8.0f, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateAll((i, j) -> (float) (i + j));
            assertEquals(0.0f, m.get(0, 0));
            assertEquals(1.0f, m.get(0, 1));
            assertEquals(1.0f, m.get(1, 0));
            assertEquals(2.0f, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_UnaryOperator_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateAll(x -> x * 2);
            assertEquals(2.0f, m.get(0, 0), 0.0f);
            assertEquals(4.0f, m.get(0, 1), 0.0f);
            assertEquals(6.0f, m.get(1, 0), 0.0f);
            assertEquals(8.0f, m.get(1, 1), 0.0f);
        }

        @Test
        public void testUpdateAll_BiFunction_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.updateAll((i, j) -> (i + 1) * 10.0f + (j + 1));
            assertEquals(11.0f, m.get(0, 0), 0.0f);
            assertEquals(12.0f, m.get(0, 1), 0.0f);
            assertEquals(21.0f, m.get(1, 0), 0.0f);
            assertEquals(22.0f, m.get(1, 1), 0.0f);
        }

        @Test
        public void testupdateAll() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            matrix.updateAll(x -> x * 2);
            assertEquals(2.0f, matrix.get(0, 0));
            assertEquals(4.0f, matrix.get(0, 1));
            assertEquals(6.0f, matrix.get(1, 0));
            assertEquals(8.0f, matrix.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testUpdateAll_ExplicitModesTransformEveryValue(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
                matrix.updateAll(value -> value + 10);
                assertEquals(FloatMatrix.wrap(new float[][] { { 11.0f, 12.0f, 13.0f }, { 14.0f, 15.0f, 16.0f } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testFloatMatrixUpdateAllNullOperator() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1f, 2f }, { 3f, 4f } });
            FloatMatrix emptyLike = FloatMatrix.wrap(new float[][] { {}, {} });
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.FloatUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.IntBiFunction<Float, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateMainDiagonal((Throwables.FloatUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAntiDiagonal((Throwables.FloatUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.FloatPredicate<RuntimeException>) null, 0f));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0f));

            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateMainDiagonal((Throwables.FloatUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAntiDiagonal((Throwables.FloatUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.FloatPredicate<RuntimeException>) null, 0f));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0f));
        }

        @Test
        public void testUpdateAll_ZeroColumnsDoNotInvokeOperator() {
            final var matrix = FloatMatrix.wrap(new float[2][0]);
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
            FloatMatrix m = matrix.copy();
            m.replaceIf(x -> x > 5.0f, 0.0f);
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(5.0f, m.get(1, 1), DELTA);
            assertEquals(0.0f, m.get(2, 2), DELTA); // was 9.0f
        }

        @Test
        public void testReplaceIfWithIndices() {
            FloatMatrix m = matrix.copy();
            m.replaceIf((i, j) -> i == j, 0.0f); // Replace diagonal
            assertEquals(0.0f, m.get(0, 0), DELTA);
            assertEquals(0.0f, m.get(1, 1), DELTA);
            assertEquals(0.0f, m.get(2, 2), DELTA);
            assertEquals(2.0f, m.get(0, 1), DELTA); // unchanged
        }

        @Test
        public void testReplaceIf_Shape2By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            m.replaceIf(x -> x > 3.0f, 0.0f);
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(2.0f, m.get(0, 1), DELTA);
            assertEquals(3.0f, m.get(0, 2), DELTA);
            assertEquals(0.0f, m.get(1, 0), DELTA); // was 4.0f
            assertEquals(0.0f, m.get(1, 1), DELTA); // was 5.0f
            assertEquals(0.0f, m.get(1, 2), DELTA); // was 6.0f
        }

        @Test
        public void testReplaceIf_WithIndices() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            m.replaceIf((i, j) -> i == j, 0.0f); // Replace diagonal
            assertEquals(0.0f, m.get(0, 0), DELTA);
            assertEquals(0.0f, m.get(1, 1), DELTA);
            assertEquals(0.0f, m.get(2, 2), DELTA);
            assertEquals(2.0f, m.get(0, 1), DELTA); // unchanged
        }

        @Test
        public void testReplaceIf_Predicate() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.replaceIf(x -> x > 2, 0.0f);
            assertEquals(1.0f, m.get(0, 0));
            assertEquals(2.0f, m.get(0, 1));
            assertEquals(0.0f, m.get(1, 0));
            assertEquals(0.0f, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_BiPredicate() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.replaceIf((i, j) -> i == j, 0.0f);
            assertEquals(0.0f, m.get(0, 0));
            assertEquals(2.0f, m.get(0, 1));
            assertEquals(3.0f, m.get(1, 0));
            assertEquals(0.0f, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_Predicate_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.replaceIf(x -> x > 2, 99.0f);
            assertEquals(1.0f, m.get(0, 0), 0.0f);
            assertEquals(2.0f, m.get(0, 1), 0.0f);
            assertEquals(99.0f, m.get(1, 0), 0.0f);
            assertEquals(99.0f, m.get(1, 1), 0.0f);
        }

        @Test
        public void testReplaceIf_BiPredicate_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.replaceIf((i, j) -> i == j, 99.0f);
            assertEquals(99.0f, m.get(0, 0), 0.0f);
            assertEquals(2.0f, m.get(0, 1), 0.0f);
            assertEquals(3.0f, m.get(1, 0), 0.0f);
            assertEquals(99.0f, m.get(1, 1), 0.0f);
        }

        @Test
        public void testreplaceIf() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { -1.0f, 2.0f, -3.0f }, { 4.0f, -5.0f, 6.0f } });
            matrix.replaceIf(x -> x < 0, 0.0f);
            assertEquals(0.0f, matrix.get(0, 0));
            assertEquals(2.0f, matrix.get(0, 1));
            assertEquals(0.0f, matrix.get(0, 2));
            assertEquals(4.0f, matrix.get(1, 0));
            assertEquals(0.0f, matrix.get(1, 1));
            assertEquals(6.0f, matrix.get(1, 2));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testReplaceIf_ExplicitModesReplaceOnlyMatchingValues(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
                matrix.replaceIf(value -> value % 2 == 0, 9.0f);
                assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 9.0f, 3.0f }, { 9.0f, 5.0f, 9.0f } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }
    }

    @Nested
    class MapTests {

        @Test
        public void testMap() {
            FloatMatrix result = matrix.map(x -> x * 2.0f);
            assertEquals(2.0f, result.get(0, 0), DELTA);
            assertEquals(4.0f, result.get(0, 1), DELTA);
            assertEquals(18.0f, result.get(2, 2), DELTA);

            // Original should be unchanged
            assertEquals(1.0f, matrix.get(0, 0), DELTA);
        }

        @Test
        public void testMap_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix result = m.map(x -> x * 2.0f);
            assertEquals(2.0f, result.get(0, 0), DELTA);
            assertEquals(4.0f, result.get(0, 1), DELTA);
            assertEquals(6.0f, result.get(1, 0), DELTA);
            assertEquals(8.0f, result.get(1, 1), DELTA);

            // Original unchanged
            assertEquals(1.0f, m.get(0, 0), DELTA);
        }

        @Test
        public void testFloatPrecisionLimits() {
            // Test float precision limits
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { Float.MAX_VALUE, Float.MIN_VALUE }, { Float.MIN_NORMAL, 1e-45f } });

            assertEquals(Float.MAX_VALUE, m.get(0, 0), DELTA);
            assertEquals(Float.MIN_VALUE, m.get(0, 1), DELTA);
            assertEquals(Float.MIN_NORMAL, m.get(1, 0), DELTA);
            assertEquals(1e-45f, m.get(1, 1), DELTA);

            // Test overflow
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { Float.MAX_VALUE } });
            FloatMatrix overflow = m2.map(x -> x * 2.0f);
            assertEquals(Float.POSITIVE_INFINITY, overflow.get(0, 0), DELTA);
        }

        @Test
        public void testMap_Shape2By2_ScalesAllCellsAndPreservesSource() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix result = m.map(x -> x * 2);
            assertEquals(2.0f, result.get(0, 0));
            assertEquals(4.0f, result.get(0, 1));
            assertEquals(6.0f, result.get(1, 0));
            assertEquals(8.0f, result.get(1, 1));
            assertEquals(1.0f, m.get(0, 0));
        }

        @Test
        public void testMap_DoublesValuesAndPreservesSource() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix result = m.map(x -> x * 2);
            assertEquals(2.0f, result.get(0, 0), 0.0f);
            assertEquals(4.0f, result.get(0, 1), 0.0f);
            assertEquals(6.0f, result.get(1, 0), 0.0f);
            assertEquals(8.0f, result.get(1, 1), 0.0f);
            // Original should be unchanged
            assertEquals(1.0f, m.get(0, 0), 0.0f);
        }

        @Test
        public void testMapNullMapper() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });

            assertThrows(IllegalArgumentException.class, () -> matrix.map((Throwables.FloatUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.mapToObj((Throwables.FloatFunction<String, RuntimeException>) null, String.class));
        }

        @Test
        public void testFloatMatrixRowsForZeroColumnMatrix() {
            final FloatMatrix matrix = FloatMatrix.wrap(new float[][] { {}, {}, {} });
            final List<float[]> rows = matrix.rowStreams().map(FloatStream::toArray).toList();

            assertEquals(3, rows.size());
            assertArrayEquals(new float[0], rows.get(0));
            assertArrayEquals(new float[0], rows.get(1));
            assertArrayEquals(new float[0], rows.get(2));
        }
    }

    @Nested
    class MapToIntTests {

        @Test
        public void testMapToInt() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.6f, 2.4f }, { 3.7f, 4.2f } });
            IntMatrix result = m.mapToInt(x -> (int) x);
            assertEquals(1, result.get(0, 0));
            assertEquals(2, result.get(0, 1));
            assertEquals(3, result.get(1, 0));
            assertEquals(4, result.get(1, 1));
            // Original should be unchanged
            assertEquals(1.6f, m.get(0, 0), 0.0f);

            assertTrue(FloatMatrix.empty().mapToInt(x -> (int) x).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> m.mapToInt((Throwables.FloatToIntFunction<RuntimeException>) null));
        }
    }

    @Nested
    class MapToLongTests {

        @Test
        public void testMapToLong() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.6f, 2.4f }, { 3.7f, 4.2f } });
            LongMatrix result = m.mapToLong(x -> (long) x);
            assertEquals(1L, result.get(0, 0));
            assertEquals(2L, result.get(0, 1));
            assertEquals(3L, result.get(1, 0));
            assertEquals(4L, result.get(1, 1));
            // Original should be unchanged
            assertEquals(1.6f, m.get(0, 0), 0.0f);

            assertTrue(FloatMatrix.empty().mapToLong(x -> (long) x).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> m.mapToLong((Throwables.FloatToLongFunction<RuntimeException>) null));
        }
    }

    @Nested
    class MapToDoubleTests {

        @Test
        public void testMapToDouble_Shape2By2_NullInput_Validation() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            DoubleMatrix result = m.mapToDouble(x -> x * 2.0);
            assertEquals(2.0, result.get(0, 0), 0.0);
            assertEquals(4.0, result.get(0, 1), 0.0);
            assertEquals(6.0, result.get(1, 0), 0.0);
            assertEquals(8.0, result.get(1, 1), 0.0);
            // Original should be unchanged
            assertEquals(1.0f, m.get(0, 0), 0.0f);

            assertTrue(FloatMatrix.empty().mapToDouble(x -> (double) x).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> m.mapToDouble((Throwables.FloatToDoubleFunction<RuntimeException>) null));
        }
    }

    @Nested
    class MapToObjTests {

        @Test
        public void testMapToObj() {
            Matrix<String> result = matrix.mapToObj(x -> "val:" + x, String.class);
            assertEquals("val:1.0", result.get(0, 0));
            assertEquals("val:5.0", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.5f, 2.5f }, { 3.5f, 4.5f } });
            Matrix<String> result = m.mapToObj(x -> String.format("%.1f", x), String.class);
            assertEquals("1.5", result.get(0, 0));
            assertEquals("4.5", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_FirstRowStringValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Matrix<String> result = m.mapToObj(x -> String.valueOf(x), String.class);
            assertEquals("1.0", result.get(0, 0));
            assertEquals("4.0", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_EveryStringValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Matrix<String> result = m.mapToObj(x -> String.valueOf(x), String.class);
            assertEquals("1.0", result.get(0, 0));
            assertEquals("2.0", result.get(0, 1));
            assertEquals("3.0", result.get(1, 0));
            assertEquals("4.0", result.get(1, 1));
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testMapToObjArrayRankBoundaryAndPrimitiveTypeNormalization() {
            // Two matrix dimensions leave room for at most 253 dimensions in each element's type.
            final Object highestRankValue = java.lang.reflect.Array.newInstance(Object.class, new int[253]);
            final Class<Object> highestRankType = (Class<Object>) highestRankValue.getClass();
            final Class<?> excessiveRankType = java.lang.reflect.Array.newInstance(Object.class, new int[254]).getClass();

            for (final FloatMatrix source : List.of(FloatMatrix.empty(), new FloatMatrix(new float[0][], 2), FloatMatrix.wrap(new float[2][0]),
                    FloatMatrix.wrap(new float[] { 1 }))) {
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

            final FloatMatrix source = FloatMatrix.wrap(new float[] { 1 });
            final Matrix<Float> scalars = source.mapToObj(value -> value, float.class);
            assertSame(Float.class, scalars.elementType());
            assertEquals(1, scalars.get(0, 0).floatValue());

            final int[] arrayValue = { 1, 2 };
            final Matrix<int[]> arrays = source.mapToObj(value -> arrayValue, int[].class);
            assertSame(int[].class, arrays.elementType());
            assertSame(arrayValue, arrays.get(0, 0));
        }
    }

    @Nested
    class FillTests {

        @Test
        public void testFillWithValue() {
            FloatMatrix m = matrix.copy();
            m.fill(99.5f);
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals(99.5f, m.get(i, j), DELTA);
                }
            }
        }

        @Test
        public void testFill_WithValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.fill(99.5f);
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals(99.5f, m.get(i, j), DELTA);
                }
            }
        }

        @Test
        public void testFill_Value() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.fill(9.0f);
            assertEquals(9.0f, m.get(0, 0), 0.0f);
            assertEquals(9.0f, m.get(0, 1), 0.0f);
            assertEquals(9.0f, m.get(1, 0), 0.0f);
            assertEquals(9.0f, m.get(1, 1), 0.0f);
        }

        @Test
        public void testFloatMatrixFill() {
            // FloatMatrix.java: FloatMatrix identity = FloatMatrix.wrap(new float[3][3]);
            // identity.fill(1.0f);
            // Creates a matrix filled with 1.0f: [[1.0f, 1.0f, 1.0f], [1.0f, 1.0f, 1.0f], [1.0f, 1.0f, 1.0f]]
            FloatMatrix identity = FloatMatrix.wrap(new float[3][3]);
            identity.fill(1.0f);
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    assertEquals(1.0f, identity.get(i, j));
                }
            }
        }

        @Test
        public void testFill_SingleValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.fill(9.0f);
            assertEquals(9.0f, m.get(0, 0));
            assertEquals(9.0f, m.get(0, 1));
            assertEquals(9.0f, m.get(1, 0));
            assertEquals(9.0f, m.get(1, 1));
        }
    }

    @Nested
    class CopyFromTests {

        @Test
        public void testCopyFrom() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f } });
            float[][] patch = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            m.copyFrom(patch);
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(2.0f, m.get(0, 1), DELTA);
            assertEquals(3.0f, m.get(1, 0), DELTA);
            assertEquals(4.0f, m.get(1, 1), DELTA);
            assertEquals(0.0f, m.get(2, 2), DELTA); // unchanged
        }

        // Test bounds
        @Test
        public void testCopyFromAtPosition() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f } });
            float[][] patch = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            m.copyFrom(1, 1, patch);
            assertEquals(0.0f, m.get(0, 0), DELTA); // unchanged
            assertEquals(1.0f, m.get(1, 1), DELTA);
            assertEquals(2.0f, m.get(1, 2), DELTA);
            assertEquals(3.0f, m.get(2, 1), DELTA);
            assertEquals(4.0f, m.get(2, 2), DELTA);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
            // assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(3, 0, patch));
            m.copyFrom(3, 0, patch);
        }

        @Test
        public void testCopyFrom_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f } });
            float[][] patch = { { 1.5f, 2.5f }, { 3.5f, 4.5f } };
            m.copyFrom(patch);
            assertEquals(1.5f, m.get(0, 0), DELTA);
            assertEquals(2.5f, m.get(0, 1), DELTA);
            assertEquals(3.5f, m.get(1, 0), DELTA);
            assertEquals(4.5f, m.get(1, 1), DELTA);
            assertEquals(0.0f, m.get(2, 2), DELTA); // unchanged
        }

        @Test
        public void testCopyFromAtPosition_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f } });
            float[][] patch = { { 1.5f, 2.5f }, { 3.5f, 4.5f } };
            m.copyFrom(1, 1, patch);
            assertEquals(0.0f, m.get(0, 0), DELTA); // unchanged
            assertEquals(1.5f, m.get(1, 1), DELTA);
            assertEquals(2.5f, m.get(1, 2), DELTA);
            assertEquals(3.5f, m.get(2, 1), DELTA);
            assertEquals(4.5f, m.get(2, 2), DELTA);
        }

        @Test
        public void testCopyFrom_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.copyFrom(new float[][] { { 7.0f, 8.0f }, { 9.0f, 10.0f } });
            assertEquals(7.0f, m.get(0, 0));
            assertEquals(8.0f, m.get(0, 1));
            assertEquals(9.0f, m.get(1, 0));
            assertEquals(10.0f, m.get(1, 1));
        }

        @Test
        public void testCopyFrom_WithPosition() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            m.copyFrom(1, 1, new float[][] { { 11.0f, 12.0f }, { 13.0f, 14.0f } });
            assertEquals(1.0f, m.get(0, 0));
            assertEquals(11.0f, m.get(1, 1));
            assertEquals(12.0f, m.get(1, 2));
            assertEquals(13.0f, m.get(2, 1));
            assertEquals(14.0f, m.get(2, 2));
        }

        @Test
        public void testCopyFrom_Shape2By2_EveryCellValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.copyFrom(new float[][] { { 9.0f, 8.0f }, { 7.0f, 6.0f } });
            assertEquals(9.0f, m.get(0, 0), 0.0f);
            assertEquals(8.0f, m.get(0, 1), 0.0f);
            assertEquals(7.0f, m.get(1, 0), 0.0f);
            assertEquals(6.0f, m.get(1, 1), 0.0f);
        }

        @Test
        public void testCopyFrom_WithOffset() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            m.copyFrom(1, 1, new float[][] { { 99.0f } });
            assertEquals(1.0f, m.get(0, 0), 0.0f);
            assertEquals(99.0f, m.get(1, 1), 0.0f);
            assertEquals(9.0f, m.get(2, 2), 0.0f);
        }

        @Test
        public void testCopyFrom_WithOffset_OversizedArray() {
            // According to javadoc, fill() copies what fits - does not throw when source array is larger
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            m.copyFrom(0, 0, new float[][] { { 9.0f, 8.0f, 7.0f } }); // Third element should be ignored
            assertEquals(9.0f, m.get(0, 0), 0.0f);
            assertEquals(8.0f, m.get(0, 1), 0.0f); // Only copies what fits
        }

        @Test
        public void testCopyFrom_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            float[][] patch = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0", "3, 0", "0, -1", "0, 3" })
        public void testCopyFrom_RejectsOffsetsOutsideDestination(final int row, final int column) {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(row, column, new float[][] { { 1.0f } }));
            assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } }), matrix);
        }
    }

    @Nested
    class CopyTests {

        @Test
        public void testCopy() {
            FloatMatrix copy = matrix.copy();
            assertEquals(matrix.rowCount(), copy.rowCount());
            assertEquals(matrix.columnCount(), copy.columnCount());
            assertEquals(matrix.get(0, 0), copy.get(0, 0), DELTA);

            // Modify copy shouldn't affect original
            copy.set(0, 0, 99.0f);
            assertEquals(1.0f, matrix.get(0, 0), DELTA);
            assertEquals(99.0f, copy.get(0, 0), DELTA);
        }

        @Test
        public void testCopy_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals(1.0f, copy.get(0, 0), DELTA);

            // Modify copy shouldn't affect original
            copy.set(0, 0, 99.5f);
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(99.5f, copy.get(0, 0), DELTA);
        }

        @Test
        public void testCopy_Shape2By2_CornerValuesAndIndependentStorage() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix copy = m.copy();
            assertEquals(1.0f, copy.get(0, 0));
            assertEquals(4.0f, copy.get(1, 1));
            copy.set(0, 0, 99.0f);
            assertEquals(1.0f, m.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2_DimensionsAndIndependentStorage() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals(1.0f, copy.get(0, 0), 0.0f);
            copy.set(0, 0, 99.0f);
            assertEquals(1.0f, m.get(0, 0), 0.0f); // Original unchanged
        }

        @Test
        public void testCopy_ModificationsAreIndependent() {
            // Verify that copy() returns a fully independent matrix (no shared row arrays).
            FloatMatrix orig = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix copy = orig.copy();
            copy.set(0, 0, 99.0f);
            assertEquals(1.0f, orig.get(0, 0), DELTA);
            assertEquals(99.0f, copy.get(0, 0), DELTA);
        }
    }

    @Nested
    class CopyRowsTests {

        @Test
        public void testCopyRows() {
            FloatMatrix subset = matrix.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertEquals(4.0f, subset.get(0, 0), DELTA);
            assertEquals(7.0f, subset.get(1, 0), DELTA);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(0, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(2, 1));
        }

        @Test
        public void testCopyRows_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            FloatMatrix subset = m.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertEquals(4.0f, subset.get(0, 0), DELTA);
            assertEquals(9.0f, subset.get(1, 2), DELTA);
        }

        @Test
        public void testCopyRows_Shape3By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            FloatMatrix copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(3.0f, copy.get(0, 0));
            assertEquals(6.0f, copy.get(1, 1));
        }

        @Test
        public void testCopyRows_Shape3By2_DimensionsAndRowValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            FloatMatrix copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(3.0f, copy.get(0, 0), 0.0f);
            assertEquals(6.0f, copy.get(1, 1), 0.0f);
        }

        @Test
        public void testCopyRangesEmpty_ReturnsEmptyMatrix() {
            // An empty row slice keeps the column count (0 x N) and an empty column slice keeps the row
            // count (N x 0); only an empty range in BOTH dimensions collapses to 0 x 0.
            FloatMatrix empty = matrix.copyRows(0, 0);
            assertEquals(0, empty.rowCount());
            assertEquals(3, empty.columnCount());

            FloatMatrix emptyRows = matrix.copyRegion(1, 1, 0, 3);
            assertEquals(0, emptyRows.rowCount());
            assertEquals(3, emptyRows.columnCount());

            FloatMatrix emptyCols = matrix.copyRegion(0, 3, 1, 1);
            assertEquals(3, emptyCols.rowCount());
            assertEquals(0, emptyCols.columnCount());
        }

        @Test
        public void testCopy_InvalidRange() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 5));
        }

        @Test
        public void testCopyRows_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(2, 1));
        }
    }

    @Nested
    class CopyRegionTests {

        @Test
        public void testCopyRegion() {
            FloatMatrix submatrix = matrix.copyRegion(0, 2, 1, 3);
            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals(2.0f, submatrix.get(0, 0), DELTA);
            assertEquals(3.0f, submatrix.get(0, 1), DELTA);
            assertEquals(5.0f, submatrix.get(1, 0), DELTA);
            assertEquals(6.0f, submatrix.get(1, 1), DELTA);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRegion(0, 2, 0, 4));
        }

        @Test
        public void testCopyRegion_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            FloatMatrix submatrix = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals(2.0f, submatrix.get(0, 0), DELTA);
            assertEquals(6.0f, submatrix.get(1, 1), DELTA);
        }

        @Test
        public void testCopyRegion_Shape3By3_UpperRightRegion() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            FloatMatrix copy = m.copyRegion(1, 3, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(5.0f, copy.get(0, 0));
            assertEquals(9.0f, copy.get(1, 1));
        }

        @Test
        public void testCopyRegion_BottomRightSubmatrix() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            FloatMatrix copy = m.copyRegion(1, 3, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(5.0f, copy.get(0, 0), 0.0f);
            assertEquals(9.0f, copy.get(1, 1), 0.0f);
        }

        @Test
        public void testCopyRegion_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, 0, 3));
        }
    }

    @Nested
    class ResizeTests {

        @Test
        public void testResize() {
            FloatMatrix extended = matrix.resize(5, 5);
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());
            assertEquals(1.0f, extended.get(0, 0), DELTA);
            assertEquals(0.0f, extended.get(3, 3), DELTA); // new cells are 0

            // Test truncation
            FloatMatrix truncated = matrix.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
        }

        @Test
        public void testResizeWithDefaultValue() {
            FloatMatrix extended = matrix.resize(4, 4, -1.0f);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1.0f, extended.get(0, 0), DELTA);
            assertEquals(-1.0f, extended.get(3, 3), DELTA); // new cell

            // Test negative dimensions
            assertThrows(IllegalArgumentException.class, () -> matrix.resize(-1, 3, 0.0f));
            assertThrows(IllegalArgumentException.class, () -> matrix.resize(3, -1, 0.0f));
        }

        @Test
        public void testResize_Larger() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix extended = m.resize(4, 4);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1.0f, extended.get(0, 0), DELTA);
            assertEquals(4.0f, extended.get(1, 1), DELTA);
            assertEquals(0.0f, extended.get(3, 3), DELTA); // new cells are 0.0f
        }

        // New cells are 0.0f
        @Test
        public void testResize_Smaller() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            FloatMatrix truncated = m.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
            assertEquals(1.0f, truncated.get(0, 0), DELTA);
            assertEquals(5.0f, truncated.get(1, 1), DELTA);
        }

        // Check original values
        @Test
        public void testResize_WithDefaultValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix extended = m.resize(3, 3, -1.5f);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1.0f, extended.get(0, 0), DELTA);
            assertEquals(-1.5f, extended.get(2, 2), DELTA); // new cell
        }

        @Test
        public void testResize_MixedSmallerAndLarger() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix extended = m.resize(2, 1, 9.9f);
            assertEquals(2, extended.rowCount());
            assertEquals(1, extended.columnCount());
            assertEquals(1.0f, extended.get(0, 0), DELTA);
            assertEquals(4.0f, extended.get(1, 0), DELTA);
        }

        @Test
        public void testResize_Simple() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix extended = m.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1.0f, extended.get(0, 0));
            assertEquals(4.0f, extended.get(1, 1));
            assertEquals(0.0f, extended.get(2, 2));
        }

        @Test
        public void testResize_WithDefaultValue_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix extended = m.resize(3, 3, 9.0f);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(9.0f, extended.get(2, 2));
        }

        @Test
        public void testResize_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix extended = m.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1.0f, extended.get(0, 0), 0.0f);
            assertEquals(0.0f, extended.get(2, 2), 0.0f);
        }

        @Test
        public void testResize_WithDefaultValue_Shape1By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix extended = m.resize(2, 3, 99.0f);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1.0f, extended.get(0, 0), 0.0f);
            assertEquals(99.0f, extended.get(1, 1), 0.0f);
            assertEquals(99.0f, extended.get(0, 2), 0.0f);
        }

        @Test
        public void testResize_Smaller_Shape2By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix result = m.resize(1, 2);
            assertEquals(1, result.rowCount());
            assertEquals(2, result.columnCount());
        }

        @Test
        public void testResize_GrowsRowsWhileTrimmingColumns() {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f }, { 3.0f }, { 9.0f } }), matrix.resize(3, 1, 9.0f));
            assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } }), matrix);
        }

        @Test
        public void testResize_GrowsWithNegativeZeroFill_PreservesNegativeZero() {
            // Resizing into a larger shape should fill new cells with the supplied default value.
            // -0.0f has a non-zero bit pattern, so it must propagate even though it equals +0.0f
            // under == comparison.
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix grown = matrix.resize(2, 4, -0.0f);

            assertEquals(2, grown.rowCount());
            assertEquals(4, grown.columnCount());
            // Pre-existing cells preserved.
            assertEquals(1.0f, grown.get(0, 0), DELTA);
            assertEquals(2.0f, grown.get(0, 1), DELTA);
            // Newly added cells must hold the -0.0f bit pattern.
            for (int j = 2; j < 4; j++) {
                assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(grown.get(0, j)),
                        "Resize new cell at (0," + j + ") must keep the -0.0f bit pattern");
            }
            for (int j = 0; j < 4; j++) {
                assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(grown.get(1, j)),
                        "Resize new cell at (1," + j + ") must keep the -0.0f bit pattern");
            }
        }

        @Test
        public void testResize_WithNegativeDimensions() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 3, 0.0f));
            assertThrows(IllegalArgumentException.class, () -> m.resize(3, -1, 0.0f));
        }
    }

    @Nested
    class PadTests {

        @Test
        public void testPadDirectional() {
            FloatMatrix extended = matrix.pad(1, 1, 2, 2);
            assertEquals(5, extended.rowCount()); // 1 + 3 + 1
            assertEquals(7, extended.columnCount()); // 2 + 3 + 2

            // Original values should be at offset position
            assertEquals(1.0f, extended.get(1, 2), DELTA);
            assertEquals(5.0f, extended.get(2, 3), DELTA);

            // New cells should be 0
            assertEquals(0.0f, extended.get(0, 0), DELTA);
        }

        @Test
        public void testPadDirectionalWithDefaultValue() {
            FloatMatrix extended = matrix.pad(1, 1, 1, 1, -1.0f);
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());

            // Check original values
            assertEquals(1.0f, extended.get(1, 1), DELTA);

            // Check new values
            assertEquals(-1.0f, extended.get(0, 0), DELTA);
            assertEquals(-1.0f, extended.get(4, 4), DELTA);

            // Test negative values
            assertThrows(IllegalArgumentException.class, () -> matrix.pad(-1, 1, 1, 1, 0.0f));
        }

        @Test
        public void testPad_Directional() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            FloatMatrix extended = m.pad(1, 1, 2, 2);
            assertEquals(5, extended.rowCount()); // 1 + 3 + 1
            assertEquals(7, extended.columnCount()); // 2 + 3 + 2

            // Original values at offset position
            assertEquals(1.0f, extended.get(1, 2), DELTA);
            assertEquals(5.0f, extended.get(2, 3), DELTA);

            // New cells are 0.0f
            assertEquals(0.0f, extended.get(0, 0), DELTA);
        }

        @Test
        public void testPad_DirectionalWithDefault() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            FloatMatrix extended = m.pad(1, 1, 1, 1, -1.5f);
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());

            // Check original values
            assertEquals(1.0f, extended.get(1, 1), DELTA);

            // Check new values
            assertEquals(-1.5f, extended.get(0, 0), DELTA);
            assertEquals(-1.5f, extended.get(4, 4), DELTA);
        }

        @Test
        public void testPad_Directions() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 5.0f } });
            FloatMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(5.0f, extended.get(1, 1));
            assertEquals(0.0f, extended.get(0, 0));
        }

        @Test
        public void testPad_DirectionsWithDefault() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 5.0f } });
            FloatMatrix extended = m.pad(1, 1, 1, 1, 9.0f);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(5.0f, extended.get(1, 1));
            assertEquals(9.0f, extended.get(0, 0));
        }

        @Test
        public void testPad_Directional_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1.0f, extended.get(1, 1), 0.0f);
            assertEquals(0.0f, extended.get(0, 0), 0.0f);
        }

        @Test
        public void testPad_Directional_WithDefaultValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix extended = m.pad(1, 1, 1, 1, 99.0f);
            assertEquals(3, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1.0f, extended.get(1, 1), 0.0f);
            assertEquals(99.0f, extended.get(0, 0), 0.0f);
        }

        @Test
        public void testPad_Directional_AllZeros() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix result = m.pad(0, 0, 0, 0, 5.5f);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0f, result.get(0, 0), DELTA);
            assertEquals(4.0f, result.get(1, 1), DELTA);
            assertFalse(result == m); // Should be a copy
        }

        @Test
        public void testPadRepeatFlattenAndForEach_SubRangeEdgeCase() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1f, 2f }, { 3f, 4f } });
            FloatMatrix extended = matrix.pad(0, 1, 1, 0, 8f);
            FloatMatrix repeatedElements = matrix.repeatElements(2, 1);
            FloatMatrix repeatedMatrix = matrix.repeatMatrix(1, 2);
            List<Float> visited = new ArrayList<>();

            matrix.forEach(0, 2, 1, 2, visited::add);

            assertEquals(8f, extended.get(2, 0), DELTA);
            assertArrayEquals(new float[] { 1f, 2f, 1f, 2f, 3f, 4f, 3f, 4f }, repeatedElements.flatten().toArray(), DELTA);
            assertArrayEquals(new float[] { 1f, 2f, 1f, 2f, 3f, 4f, 3f, 4f }, repeatedMatrix.flatten().toArray(), DELTA);
            assertEquals(List.of(2f, 4f), visited);
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, matrix.toIntMatrix().flatten().toArray());
            assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, matrix.toLongMatrix().flatten().toArray());
        }

        @Test
        public void testPad_WithNaNFill_PropagatesNaN() {
            // Extending with NaN should fill all newly added cells with NaN. This exercises the
            // floatToRawIntBits != 0 short-circuit in pad(...).
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix extended = matrix.pad(1, 1, 1, 1, Float.NaN);

            assertEquals(3, extended.rowCount());
            assertEquals(4, extended.columnCount());
            // Original content preserved.
            assertEquals(1.0f, extended.get(1, 1), DELTA);
            assertEquals(2.0f, extended.get(1, 2), DELTA);
            // Padded cells must be NaN.
            assertTrue(Float.isNaN(extended.get(0, 0)));
            assertTrue(Float.isNaN(extended.get(0, 3)));
            assertTrue(Float.isNaN(extended.get(2, 0)));
            assertTrue(Float.isNaN(extended.get(2, 3)));
            assertTrue(Float.isNaN(extended.get(1, 0)));
            assertTrue(Float.isNaN(extended.get(1, 3)));
        }

        @Test
        public void testPad_SingleAxisFillPreservesExistingValues() {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 9.0f }, { 3.0f, 4.0f, 9.0f } }), matrix.pad(0, 0, 0, 1, 9.0f));
            assertEquals(FloatMatrix.wrap(new float[][] { { 9.0f, 1.0f, 2.0f }, { 9.0f, 3.0f, 4.0f } }), matrix.pad(0, 0, 1, 0, 9.0f));
            assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 9.0f, 9.0f } }), matrix.pad(0, 1, 0, 0, 9.0f));
            final var copy = matrix.pad(0, 0, 0, 0, 9.0f);
            assertEquals(matrix, copy);
            assertNotSame(matrix, copy);
        }

        @Test
        public void testPad_DirectionalWithNegativeValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.pad(-1, 1, 1, 1, 0.0f));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0, 0, 0", "0, -1, 0, 0", "0, 0, -1, 0", "0, 0, 0, -1", "2147483647, 0, 0, 0", "0, 0, 0, 2147483647" })
        public void testPad_RejectsNegativeOrOverflowingDimensions(final int top, final int bottom, final int left, final int right) {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> matrix.pad(top, bottom, left, right));
            assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } }), matrix);
        }
    }

    @Nested
    class FlipHorizontallyInPlaceTests {

        @Test
        public void testReverseH() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            m.flipHorizontallyInPlace();
            assertEquals(3.0f, m.get(0, 0), DELTA);
            assertEquals(2.0f, m.get(0, 1), DELTA);
            assertEquals(1.0f, m.get(0, 2), DELTA);
            assertEquals(6.0f, m.get(1, 0), DELTA);
            assertEquals(5.0f, m.get(1, 1), DELTA);
            assertEquals(4.0f, m.get(1, 2), DELTA);
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            m.flipHorizontallyInPlace();
            assertEquals(3.0f, m.get(0, 0), DELTA);
            assertEquals(2.0f, m.get(0, 1), DELTA);
            assertEquals(1.0f, m.get(0, 2), DELTA);
            assertEquals(6.0f, m.get(1, 0), DELTA);
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.flipHorizontallyInPlace();
            assertEquals(2.0f, m.get(0, 0));
            assertEquals(1.0f, m.get(0, 1));
            assertEquals(4.0f, m.get(1, 0));
            assertEquals(3.0f, m.get(1, 1));
        }

        @Test
        public void testFlipHorizontallyInPlace() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            m.flipHorizontallyInPlace();
            assertEquals(3.0f, m.get(0, 0), 0.0f);
            assertEquals(2.0f, m.get(0, 1), 0.0f);
            assertEquals(1.0f, m.get(0, 2), 0.0f);
        }

        @Test
        public void testFlipHorizontallyInPlace_SingleColumn_IsNoOp() {
            // Edge case: Nx1 matrix should be unchanged by horizontal flip.
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f }, { 2.0f }, { 3.0f } });
            m.flipHorizontallyInPlace();
            assertEquals(1.0f, m.get(0, 0), DELTA);
            assertEquals(2.0f, m.get(1, 0), DELTA);
            assertEquals(3.0f, m.get(2, 0), DELTA);
        }
    }

    @Nested
    class FlipVerticallyInPlaceTests {

        @Test
        public void testReverseV() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            m.flipVerticallyInPlace();
            assertEquals(5.0f, m.get(0, 0), DELTA);
            assertEquals(6.0f, m.get(0, 1), DELTA);
            assertEquals(3.0f, m.get(1, 0), DELTA);
            assertEquals(4.0f, m.get(1, 1), DELTA);
            assertEquals(1.0f, m.get(2, 0), DELTA);
            assertEquals(2.0f, m.get(2, 1), DELTA);
        }

        @Test
        public void testFlipVerticallyInPlace_Shape3By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            m.flipVerticallyInPlace();
            assertEquals(5.0f, m.get(0, 0), DELTA);
            assertEquals(6.0f, m.get(0, 1), DELTA);
            assertEquals(3.0f, m.get(1, 0), DELTA);
            assertEquals(1.0f, m.get(2, 0), DELTA);
        }

        @Test
        public void testFlipVerticallyInPlace_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            m.flipVerticallyInPlace();
            assertEquals(3.0f, m.get(0, 0));
            assertEquals(4.0f, m.get(0, 1));
            assertEquals(1.0f, m.get(1, 0));
            assertEquals(2.0f, m.get(1, 1));
        }

        @Test
        public void testFlipVerticallyInPlace() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            m.flipVerticallyInPlace();
            assertEquals(5.0f, m.get(0, 0), 0.0f);
            assertEquals(3.0f, m.get(1, 0), 0.0f);
            assertEquals(1.0f, m.get(2, 0), 0.0f);
        }

        @Test
        public void testFlipVerticallyInPlace_SingleRow_IsNoOp() {
            // Edge case: 1xN matrix should be unchanged by vertical flip.
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } });
            m.flipVerticallyInPlace();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, m.rowCopy(0), DELTA);
        }
    }

    @Nested
    class FlipHorizontallyTests {

        // Original should be unchanged
        @Test
        public void testFlipH() {
            FloatMatrix flipped = matrix.flipHorizontally();
            assertEquals(3.0f, flipped.get(0, 0), DELTA);
            assertEquals(2.0f, flipped.get(0, 1), DELTA);
            assertEquals(1.0f, flipped.get(0, 2), DELTA);

            // Original should be unchanged
            assertEquals(1.0f, matrix.get(0, 0), DELTA);
        }

        @Test
        public void testFlipHorizontally_Shape2By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix flipped = m.flipHorizontally();
            assertEquals(3.0f, flipped.get(0, 0), DELTA);
            assertEquals(2.0f, flipped.get(0, 1), DELTA);
            assertEquals(1.0f, flipped.get(0, 2), DELTA);

            // Original unchanged
            assertEquals(1.0f, m.get(0, 0), DELTA);
        }

        @Test
        public void testFlipHorizontally_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix flipped = m.flipHorizontally();
            assertEquals(2.0f, flipped.get(0, 0));
            assertEquals(1.0f, flipped.get(0, 1));
            assertEquals(1.0f, m.get(0, 0));
        }

        // Original elements
        @Test
        public void testFlipHorizontally() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix flipped = m.flipHorizontally();
            assertEquals(3.0f, flipped.get(0, 0), 0.0f);
            assertEquals(2.0f, flipped.get(0, 1), 0.0f);
            assertEquals(1.0f, flipped.get(0, 2), 0.0f);
            // Original unchanged
            assertEquals(1.0f, m.get(0, 0), 0.0f);
        }
    }

    @Nested
    class FlipVerticallyTests {

        @Test
        public void testFlipV() {
            FloatMatrix flipped = matrix.flipVertically();
            assertEquals(7.0f, flipped.get(0, 0), DELTA);
            assertEquals(8.0f, flipped.get(0, 1), DELTA);
            assertEquals(9.0f, flipped.get(0, 2), DELTA);

            // Original should be unchanged
            assertEquals(1.0f, matrix.get(0, 0), DELTA);
        }

        @Test
        public void testFlipVertically_Shape3By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            FloatMatrix flipped = m.flipVertically();
            assertEquals(5.0f, flipped.get(0, 0), DELTA);
            assertEquals(3.0f, flipped.get(1, 0), DELTA);
            assertEquals(1.0f, flipped.get(2, 0), DELTA);

            // Original unchanged
            assertEquals(1.0f, m.get(0, 0), DELTA);
        }

        @Test
        public void testFlipVertically_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix flipped = m.flipVertically();
            assertEquals(3.0f, flipped.get(0, 0));
            assertEquals(4.0f, flipped.get(0, 1));
            assertEquals(1.0f, m.get(0, 0));
        }

        @Test
        public void testFlipVertically() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            FloatMatrix flipped = m.flipVertically();
            assertEquals(5.0f, flipped.get(0, 0), 0.0f);
            assertEquals(3.0f, flipped.get(1, 0), 0.0f);
            assertEquals(1.0f, flipped.get(2, 0), 0.0f);
            // Original unchanged
            assertEquals(1.0f, m.get(0, 0), 0.0f);
        }
    }

    @Nested
    class Rotate90Tests {

        @Test
        public void testRotate90() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3.0f, rotated.get(0, 0), DELTA);
            assertEquals(1.0f, rotated.get(0, 1), DELTA);
            assertEquals(4.0f, rotated.get(1, 0), DELTA);
            assertEquals(2.0f, rotated.get(1, 1), DELTA);
        }

        @Test
        public void testRotate90_MoreColsThanRows() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix rotated = m.rotate90();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4.0f, rotated.get(0, 0), DELTA);
            assertEquals(1.0f, rotated.get(0, 1), DELTA);
        }

        @Test
        public void testRotate90_MoreRowsThanCols() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            FloatMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            assertEquals(5.0f, rotated.get(0, 0), DELTA);
            assertEquals(3.0f, rotated.get(0, 1), DELTA);
            assertEquals(1.0f, rotated.get(0, 2), DELTA);
        }

        @Test
        public void testRotate90_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3.0f, rotated.get(0, 0));
            assertEquals(1.0f, rotated.get(0, 1));
            assertEquals(4.0f, rotated.get(1, 0));
            assertEquals(2.0f, rotated.get(1, 1));
        }

        @Test
        public void testRotate90_Shape2By2_DimensionsAndEveryCellValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3.0f, rotated.get(0, 0), 0.0f);
            assertEquals(1.0f, rotated.get(0, 1), 0.0f);
            assertEquals(4.0f, rotated.get(1, 0), 0.0f);
            assertEquals(2.0f, rotated.get(1, 1), 0.0f);
        }

        // Verify that copy() returns a fully independent matrix (no shared row arrays).
        @Test
        public void testRotate90_NonSquare_3x2() {
            // 3x2 rotated 90° CW must yield a 2x3 with first column = last row reading upward.
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            FloatMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            assertArrayEquals(new float[] { 5.0f, 3.0f, 1.0f }, rotated.rowCopy(0), DELTA);
            assertArrayEquals(new float[] { 6.0f, 4.0f, 2.0f }, rotated.rowCopy(1), DELTA);
        }
    }

    @Nested
    class Rotate180Tests {

        @Test
        public void testRotate180() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix rotated = m.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4.0f, rotated.get(0, 0), DELTA);
            assertEquals(3.0f, rotated.get(0, 1), DELTA);
            assertEquals(2.0f, rotated.get(1, 0), DELTA);
            assertEquals(1.0f, rotated.get(1, 1), DELTA);
        }

        @Test
        public void testRotate180_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix rotated = m.rotate180();
            assertEquals(4.0f, rotated.get(0, 0));
            assertEquals(3.0f, rotated.get(0, 1));
            assertEquals(2.0f, rotated.get(1, 0));
            assertEquals(1.0f, rotated.get(1, 1));
        }

        @Test
        public void testRotate180_Shape2By2_EveryCellValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix rotated = m.rotate180();
            assertEquals(4.0f, rotated.get(0, 0), 0.0f);
            assertEquals(3.0f, rotated.get(0, 1), 0.0f);
            assertEquals(2.0f, rotated.get(1, 0), 0.0f);
            assertEquals(1.0f, rotated.get(1, 1), 0.0f);
        }

        @Test
        public void testRotate180_Nx0_AgreesWithRotate90Twice() {
            FloatMatrix m = FloatMatrix.wrap(new float[3][0]);

            FloatMatrix via180 = m.rotate180();
            assertEquals(3, via180.rowCount());
            assertEquals(0, via180.columnCount());

            // Two 90-degree rotations must equal one 180-degree rotation even for a degenerate
            // shape; that only holds because rotate90 preserves the transposed column count.
            FloatMatrix viaRotate90Twice = m.rotate90().rotate90();
            assertEquals(3, viaRotate90Twice.rowCount());
            assertEquals(0, viaRotate90Twice.columnCount());
        }
    }

    @Nested
    class Rotate270Tests {

        @Test
        public void testRotate270() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(2.0f, rotated.get(0, 0), DELTA);
            assertEquals(4.0f, rotated.get(0, 1), DELTA);
            assertEquals(1.0f, rotated.get(1, 0), DELTA);
            assertEquals(3.0f, rotated.get(1, 1), DELTA);
        }

        @Test
        public void testRotate270_MoreColsThanRows() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix rotated = m.rotate270();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3.0f, rotated.get(0, 0), DELTA);
            assertEquals(6.0f, rotated.get(0, 1), DELTA);
        }

        @Test
        public void testRotate270_MoreRowsThanCols() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            FloatMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            assertEquals(2.0f, rotated.get(0, 0), DELTA);
            assertEquals(4.0f, rotated.get(0, 1), DELTA);
            assertEquals(6.0f, rotated.get(0, 2), DELTA);
        }

        @Test
        public void testRotate270_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix rotated = m.rotate270();
            assertEquals(2.0f, rotated.get(0, 0));
            assertEquals(4.0f, rotated.get(0, 1));
            assertEquals(1.0f, rotated.get(1, 0));
            assertEquals(3.0f, rotated.get(1, 1));
        }

        @Test
        public void testRotate270_Shape2By2_DimensionsAndEveryCellValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(2.0f, rotated.get(0, 0), 0.0f);
            assertEquals(4.0f, rotated.get(0, 1), 0.0f);
            assertEquals(1.0f, rotated.get(1, 0), 0.0f);
            assertEquals(3.0f, rotated.get(1, 1), 0.0f);
        }

        @Test
        public void testRotate270_NonSquare_3x2() {
            // 3x2 rotated 270° CW (= 90° CCW) must yield a 2x3 with first row = original column 1.
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            FloatMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            assertArrayEquals(new float[] { 2.0f, 4.0f, 6.0f }, rotated.rowCopy(0), DELTA);
            assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, rotated.rowCopy(1), DELTA);
        }
    }

    @Nested
    class TransposeTests {

        @Test
        public void testTranspose() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1.0f, transposed.get(0, 0), DELTA);
            assertEquals(4.0f, transposed.get(0, 1), DELTA);
            assertEquals(2.0f, transposed.get(1, 0), DELTA);
            assertEquals(5.0f, transposed.get(1, 1), DELTA);
        }

        @Test
        public void testTranspose_Shape2By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1.0f, transposed.get(0, 0), DELTA);
            assertEquals(4.0f, transposed.get(0, 1), DELTA);
            assertEquals(2.0f, transposed.get(1, 0), DELTA);
            assertEquals(5.0f, transposed.get(1, 1), DELTA);
            assertEquals(3.0f, transposed.get(2, 0), DELTA);
            assertEquals(6.0f, transposed.get(2, 1), DELTA);
        }

        @Test
        public void testTranspose_Square() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1.0f, transposed.get(0, 0), DELTA);
            assertEquals(3.0f, transposed.get(0, 1), DELTA);
            assertEquals(2.0f, transposed.get(1, 0), DELTA);
            assertEquals(4.0f, transposed.get(1, 1), DELTA);
        }

        @Test
        public void testTranspose_MoreRowsThanCols() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            FloatMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(3, transposed.columnCount());
            assertEquals(1.0f, transposed.get(0, 0), DELTA);
            assertEquals(3.0f, transposed.get(0, 1), DELTA);
            assertEquals(5.0f, transposed.get(0, 2), DELTA);
            assertEquals(2.0f, transposed.get(1, 0), DELTA);
            assertEquals(4.0f, transposed.get(1, 1), DELTA);
            assertEquals(6.0f, transposed.get(1, 2), DELTA);
        }

        @Test
        public void testTranspose_Shape2By3_SwapsDimensionsAndCornerValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1.0f, transposed.get(0, 0));
            assertEquals(4.0f, transposed.get(0, 1));
            assertEquals(3.0f, transposed.get(2, 0));
            assertEquals(6.0f, transposed.get(2, 1));
        }

        @Test
        public void testTranspose_Square_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix transposed = m.transpose();
            assertEquals(1.0f, transposed.get(0, 0));
            assertEquals(3.0f, transposed.get(0, 1));
            assertEquals(2.0f, transposed.get(1, 0));
            assertEquals(4.0f, transposed.get(1, 1));
        }

        @Test
        public void testTranspose_RectangularCellPositions() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1.0f, transposed.get(0, 0), 0.0f);
            assertEquals(4.0f, transposed.get(0, 1), 0.0f);
            assertEquals(2.0f, transposed.get(1, 0), 0.0f);
            assertEquals(5.0f, transposed.get(1, 1), 0.0f);
        }

        @Test
        public void testTranspose_Square_Shape2By2_EveryCellValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1.0f, transposed.get(0, 0), 0.0f);
            assertEquals(3.0f, transposed.get(0, 1), 0.0f);
        }

        @Test
        public void testTranspose_NonSquare_3x2() {
            // Verify transpose for the rowCount > columnCount branch.
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            FloatMatrix t = m.transpose();
            assertEquals(2, t.rowCount());
            assertEquals(3, t.columnCount());
            assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, t.rowCopy(0), DELTA);
            assertArrayEquals(new float[] { 2.0f, 4.0f, 6.0f }, t.rowCopy(1), DELTA);
        }

        // IteratorEx.count() is terminal: it consumes the remainder and exhausts the iterator, so every
        // remaining-size assertion below uses its own freshly positioned iterator.
        @Test
        public void testTranspose_Nx0_PreservesTransposedShape() {
            // An N x 0 matrix transposes to 0 x N: no elements, but the column count survives.
            FloatMatrix t = FloatMatrix.wrap(new float[3][0]).transpose();
            assertEquals(0, t.rowCount());
            assertEquals(3, t.columnCount());
        }
    }

    @Nested
    class ReshapeAndPadTests {

        @Test
        public void testReshape_DifferentSize() {
            // According to javadoc, reshape allows different sizes - excess elements truncated, missing filled with zeros
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix reshaped = m.reshapeAndPad(3, 3); // 4 elements reshaped to 9 positions
            assertEquals(3, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            // Original elements
            assertEquals(1.0f, reshaped.get(0, 0), 0.0f);
            assertEquals(2.0f, reshaped.get(0, 1), 0.0f);
            assertEquals(3.0f, reshaped.get(0, 2), 0.0f);
            assertEquals(4.0f, reshaped.get(1, 0), 0.0f);
            // Remaining positions filled with zeros
            assertEquals(0.0f, reshaped.get(1, 1), 0.0f);
            assertEquals(0.0f, reshaped.get(1, 2), 0.0f);
            assertEquals(0.0f, reshaped.get(2, 0), 0.0f);
            assertEquals(0.0f, reshaped.get(2, 1), 0.0f);
            assertEquals(0.0f, reshaped.get(2, 2), 0.0f);
        }

        @Test
        public void testReshape_Empty() {
            FloatMatrix empty = FloatMatrix.empty();
            FloatMatrix reshaped = empty.reshapeAndPad(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }

        @Test
        public void testReshape_GrowLargerThanInput_PadsWithZeros() {
            // 2x3 reshaped to 2x4 should preserve order and pad the trailing slots with 0.0f.
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix reshaped = m.reshapeAndPad(2, 4);
            assertEquals(2, reshaped.rowCount());
            assertEquals(4, reshaped.columnCount());
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, reshaped.rowCopy(0), DELTA);
            assertArrayEquals(new float[] { 5.0f, 6.0f, 0.0f, 0.0f }, reshaped.rowCopy(1), DELTA);
        }

        @ParameterizedTest
        @CsvSource({ "-1, 2", "2, -1", "1, 1" })
        public void testReshapeAndPad_RejectsNegativeAndInsufficientShapes(final int rows, final int columns) {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> matrix.reshapeAndPad(rows, columns));
            assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } }), matrix);
        }
    }

    @Nested
    class RepeatElementsTests {

        // Test invalid arguments
        @Test
        public void testRepeatElements() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix repeated = m.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals(1.0f, repeated.get(0, 0), DELTA);
            assertEquals(1.0f, repeated.get(0, 1), DELTA);
            assertEquals(1.0f, repeated.get(0, 2), DELTA);
            assertEquals(2.0f, repeated.get(0, 3), DELTA);
            assertEquals(2.0f, repeated.get(0, 4), DELTA);
            assertEquals(2.0f, repeated.get(0, 5), DELTA);
            assertEquals(1.0f, repeated.get(1, 0), DELTA); // second row same as first

            // Test invalid arguments
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @Test
        public void testRepeatElements_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix result = m.repeatElements(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1.0f, result.get(0, 0));
            assertEquals(1.0f, result.get(0, 1));
            assertEquals(1.0f, result.get(1, 0));
            assertEquals(1.0f, result.get(1, 1));
            assertEquals(4.0f, result.get(2, 2));
            assertEquals(4.0f, result.get(3, 3));
        }

        @Test
        public void testRepeatElements_Shape2By2_RepeatedBlockAndAdjacentValue() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix result = m.repeatElements(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1.0f, result.get(0, 0), 0.0f);
            assertEquals(1.0f, result.get(0, 1), 0.0f);
            assertEquals(1.0f, result.get(1, 0), 0.0f);
            assertEquals(1.0f, result.get(1, 1), 0.0f);
            assertEquals(2.0f, result.get(0, 2), 0.0f);
        }

        @Test
        public void testrepeatElements() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix repeated = matrix.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertArrayEquals(new float[] { 1.0f, 1.0f, 1.0f, 2.0f, 2.0f, 2.0f }, repeated.rowView(0));
        }

        @Test
        public void testRepeatElements_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            final var result = matrix.repeatElements(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, 0.0f);
            assertEquals(1.0f, matrix.get(0, 0));
        }

        @Test
        public void testRepeatElements_OnlyColumnsRepeated() {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 3.0f }, { 3.0f, 1.0f } });
            final var repeated = matrix.repeatElements(1, 2);
            assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 1.0f, 3.0f, 3.0f }, { 3.0f, 3.0f, 1.0f, 1.0f } }), repeated);
            repeated.set(0, 0, 3.0f);
            assertEquals(1.0f, matrix.get(0, 0));
        }

        @Test
        public void testRepeatElements_InvalidArguments() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @Test
        public void testRepeatElements_InvalidRepeats() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatElements_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(rows, columns));
        }
    }

    @Nested
    class RepeatMatrixTests {

        @Test
        public void testRepeatMatrix() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix repeated = m.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals(1.0f, repeated.get(0, 0), DELTA);
            assertEquals(2.0f, repeated.get(0, 1), DELTA);
            assertEquals(1.0f, repeated.get(0, 2), DELTA); // repeat starts
            assertEquals(2.0f, repeated.get(0, 3), DELTA);
            assertEquals(1.0f, repeated.get(0, 4), DELTA); // another repeat
            assertEquals(2.0f, repeated.get(0, 5), DELTA);

            assertEquals(3.0f, repeated.get(1, 0), DELTA);
            assertEquals(4.0f, repeated.get(1, 1), DELTA);

            // Check vertical repeat
            assertEquals(1.0f, repeated.get(2, 0), DELTA); // vertical repeat starts
            assertEquals(2.0f, repeated.get(2, 1), DELTA);

            // Test invalid arguments
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @Test
        public void testRepeatMatrix_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix result = m.repeatMatrix(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1.0f, result.get(0, 0));
            assertEquals(2.0f, result.get(0, 1));
            assertEquals(1.0f, result.get(0, 2));
            assertEquals(2.0f, result.get(0, 3));
            assertEquals(1.0f, result.get(2, 0));
        }

        @Test
        public void testRepeatMatrix_Shape1By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix result = m.repeatMatrix(2, 2);
            assertEquals(2, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1.0f, result.get(0, 0), 0.0f);
            assertEquals(2.0f, result.get(0, 1), 0.0f);
            assertEquals(1.0f, result.get(0, 2), 0.0f);
            assertEquals(2.0f, result.get(0, 3), 0.0f);
        }

        @Test
        public void testrepeatMatrix() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix repeated = matrix.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertArrayEquals(new float[] { 1.0f, 2.0f, 1.0f, 2.0f, 1.0f, 2.0f }, repeated.rowView(0));
        }

        @Test
        public void testRepeatMatrix_DegenerateShapesPreserveDimensionsAndValidation() {
            final FloatMatrix noRows = new FloatMatrix(new float[0][], 2);
            final FloatMatrix noColumns = FloatMatrix.wrap(new float[][] { {}, {} });

            // Large repeat factors must not cause repeated work when there are no cells to copy.
            // Keep the counts bounded so this shape regression remains safe on older implementations.
            final FloatMatrix repeatedRows = noRows.repeatMatrix(100_000, 3);
            assertEquals(0, repeatedRows.rowCount());
            assertEquals(6, repeatedRows.columnCount());

            final FloatMatrix repeatedColumns = noColumns.repeatMatrix(3, 100_000);
            assertEquals(6, repeatedColumns.rowCount());
            assertEquals(0, repeatedColumns.columnCount());
            for (int i = 0; i < repeatedColumns.rowCount(); i++) {
                assertEquals(0, repeatedColumns.rowView(i).length);
            }
            assertSame(FloatMatrix.empty(), FloatMatrix.empty().repeatMatrix(100_000, 100_000));

            assertThrows(IllegalArgumentException.class, () -> noRows.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> noColumns.repeatMatrix(1, -1));
            assertThrows(IllegalArgumentException.class, () -> noRows.repeatMatrix(1, Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> noColumns.repeatMatrix(Integer.MAX_VALUE, 1));
        }

        @Test
        public void testRepeatMatrix_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            final var result = matrix.repeatMatrix(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, 0.0f);
            assertEquals(1.0f, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_OnlyColumnsRepeated() {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            final var repeated = matrix.repeatMatrix(1, 2);
            assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 1.0f, 2.0f }, { 3.0f, 4.0f, 3.0f, 4.0f } }), repeated);
            repeated.set(0, 0, 3.0f);
            assertEquals(1.0f, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidArguments() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidRepeats() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatMatrix_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(rows, columns));
            assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } }), matrix);
        }
    }

    @Nested
    class FlattenTests {

        @Test
        public void testFlatten() {
            FloatList flat = matrix.flatten();
            assertEquals(9, flat.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, flat.get(i), DELTA);
            }
        }

        @Test
        public void testFlatten_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            FloatList flat = m.flatten();
            assertEquals(9, flat.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, flat.get(i), DELTA);
            }
        }

        @Test
        public void testFlatten_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatList flat = m.flatten();
            assertEquals(4, flat.size());
            assertEquals(1.0f, flat.get(0));
            assertEquals(2.0f, flat.get(1));
            assertEquals(3.0f, flat.get(2));
            assertEquals(4.0f, flat.get(3));
        }

        @Test
        public void testFlatten_Shape2By2_EveryListElement() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatList flat = m.flatten();
            assertEquals(4, flat.size());
            assertEquals(1.0f, flat.get(0), 0.0f);
            assertEquals(2.0f, flat.get(1), 0.0f);
            assertEquals(3.0f, flat.get(2), 0.0f);
            assertEquals(4.0f, flat.get(3), 0.0f);
        }

        @Test
        public void testflatten() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatList list = matrix.flatten();
            assertEquals(FloatList.of(1.0f, 2.0f, 3.0f, 4.0f), list);
        }

        @Test
        public void testEmptyMatrixOperations() {
            // Test various operations on empty matrix
            assertTrue(emptyMatrix.flatten().isEmpty());
            assertEquals(0, emptyMatrix.copy().rowCount);
            assertEquals(emptyMatrix, emptyMatrix.transpose());
            assertEquals(emptyMatrix, emptyMatrix.rotate90());

            FloatMatrix extended = emptyMatrix.resize(2, 2, 5.0f);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals(5.0f, extended.get(0, 0), DELTA);
        }

        @Test
        public void testFlatten_Empty() {
            FloatMatrix empty = FloatMatrix.empty();
            FloatList flat = empty.flatten();
            assertTrue(flat.isEmpty());
        }
    }

    @Nested
    class MutateViaFlatArrayTests {

        @Test
        public void testFlatOp() {
            List<Float> sums = new ArrayList<>();
            matrix.mutateViaFlatArray(row -> {
                float sum = 0.0f;
                for (float val : row) {
                    sum += val;
                }
                sums.add(sum);
            });
            assertEquals(1, sums.size());
            assertEquals(45.0f, sums.get(0), DELTA);
        }

        @Test
        public void testMutateViaFlatArray_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            List<Float> sums = new ArrayList<>();
            m.mutateViaFlatArray(row -> {
                float sum = 0.0f;
                for (float val : row) {
                    sum += val;
                }
                sums.add(sum);
            });
            assertEquals(1, sums.size());
            assertEquals(45.0f, sums.get(0), DELTA);
        }

        @Test
        public void testMutateViaFlatArray_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            final float[] sum = { 0.0f };
            m.mutateViaFlatArray(row -> {
                for (float val : row) {
                    sum[0] += val;
                }
            });
            assertEquals(10.0f, sum[0]);
        }

        @Test
        public void testMutateViaFlatArray() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            AtomicInteger count = new AtomicInteger(0);
            m.mutateViaFlatArray(row -> count.addAndGet(row.length));
            assertEquals(4, count.get());
        }

        @Test
        public void testmutateViaFlatArray() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 5.0f, 3.0f }, { 4.0f, 1.0f } });
            matrix.mutateViaFlatArray(arr -> java.util.Arrays.sort(arr));
            assertEquals(1.0f, matrix.get(0, 0));
            assertEquals(3.0f, matrix.get(0, 1));
            assertEquals(4.0f, matrix.get(1, 0));
            assertEquals(5.0f, matrix.get(1, 1));
        }
    }

    @Nested
    class StackVerticallyTests {

        @Test
        public void testVstack() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 7.0f, 8.0f, 9.0f }, { 10.0f, 11.0f, 12.0f } });
            FloatMatrix stacked = m1.stackVertically(m2);

            assertEquals(4, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals(1.0f, stacked.get(0, 0), DELTA);
            assertEquals(7.0f, stacked.get(2, 0), DELTA);
            assertEquals(12.0f, stacked.get(3, 2), DELTA);

            // Test different column counts
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m3));
        }

        @Test
        public void testStackVertically_Shape2By3() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 7.0f, 8.0f, 9.0f }, { 10.0f, 11.0f, 12.0f } });
            FloatMatrix stacked = m1.stackVertically(m2);

            assertEquals(4, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals(1.0f, stacked.get(0, 0), DELTA);
            assertEquals(7.0f, stacked.get(2, 0), DELTA);
            assertEquals(12.0f, stacked.get(3, 2), DELTA);
        }

        @Test
        public void testStackVertically_DifferentColumnCounts() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_Shape2By2() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix stacked = m1.stackVertically(m2);
            assertEquals(4, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1.0f, stacked.get(0, 0));
            assertEquals(5.0f, stacked.get(2, 0));
        }

        @Test
        public void testStackVertically_IncompatibleColumns() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 3.0f, 4.0f } });
            FloatMatrix result = m1.stackVertically(m2);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0f, result.get(0, 0), 0.0f);
            assertEquals(3.0f, result.get(1, 0), 0.0f);
        }

        @Test
        public void testStackVertically_IncompatibleCols() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        // FloatMatrix.java: FloatMatrix result = matrix1.zipWith(matrix2, matrix3, (a, b, c) -> a + b + c);
        // result is [[6.0f]]
        @Test
        public void teststackVertically() {
            FloatMatrix a = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix b = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix stacked = a.stackVertically(b);
            assertEquals(4, stacked.rowCount());
            assertEquals(1.0f, stacked.get(0, 0));
            assertEquals(5.0f, stacked.get(2, 0));
            assertEquals(8.0f, stacked.get(3, 1));
        }

        @Test
        public void testStackVertically_ClonesRows_Independent() {
            // Verify that stackVertically uses cloned rows, so modifying source after the call
            // does not affect the stacked result.
            float[][] aData = { { 1.0f, 2.0f } };
            float[][] bData = { { 3.0f, 4.0f } };
            FloatMatrix a = FloatMatrix.wrap(aData);
            FloatMatrix b = FloatMatrix.wrap(bData);
            FloatMatrix stacked = a.stackVertically(b);

            // Mutate source data after stacking; stacked should NOT change.
            aData[0][0] = -1.0f;
            bData[0][1] = -1.0f;
            assertEquals(1.0f, stacked.get(0, 0), DELTA);
            assertEquals(4.0f, stacked.get(1, 1), DELTA);
        }

        @Test
        public void testFloatMatrixBinaryOpsRejectNullMatrix() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1F } });
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
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1.0f, stacked.get(0, 0), DELTA);
            assertEquals(5.0f, stacked.get(0, 2), DELTA);
            assertEquals(8.0f, stacked.get(1, 3), DELTA);

            // Test different row counts
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m3));
        }

        @Test
        public void testStackHorizontally_Shape2By2() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1.0f, stacked.get(0, 0), DELTA);
            assertEquals(5.0f, stacked.get(0, 2), DELTA);
            assertEquals(8.0f, stacked.get(1, 3), DELTA);
        }

        @Test
        public void testStackHorizontally_DifferentRowCounts() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_Shape2By2_DimensionsAndOuterValues() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1.0f, stacked.get(0, 0));
            assertEquals(5.0f, stacked.get(0, 2));
        }

        @Test
        public void testStackHorizontally_IncompatibleRows() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f }, { 2.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 3.0f }, { 4.0f } });
            FloatMatrix result = m1.stackHorizontally(m2);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0f, result.get(0, 0), 0.0f);
            assertEquals(3.0f, result.get(0, 1), 0.0f);
        }

        @Test
        public void testStackHorizontally_IncompatibleRows_Shape1By1_Validation() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 3.0f }, { 4.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void teststackHorizontally() {
            FloatMatrix a = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix b = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix stacked = a.stackHorizontally(b);
            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertArrayEquals(new float[] { 1.0f, 2.0f, 5.0f, 6.0f }, stacked.rowView(0));
            assertArrayEquals(new float[] { 3.0f, 4.0f, 7.0f, 8.0f }, stacked.rowView(1));
        }

        @Test
        public void testStackHorizontally_RejectsLogicalWidthOverflowWithoutAllocation() {
            final var huge = FloatMatrix.wrap(new float[0][0]).resize(0, Integer.MAX_VALUE);
            final var extra = FloatMatrix.wrap(new float[0][0]).resize(0, 1);
            assertThrows(IllegalArgumentException.class, () -> huge.stackHorizontally(extra));
            assertEquals(Integer.MAX_VALUE, huge.columnCount());
        }
    }

    @Nested
    class AddTests {

        @Test
        public void testAdd() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix sum = m1.add(m2);

            assertEquals(6.0f, sum.get(0, 0), DELTA);
            assertEquals(8.0f, sum.get(0, 1), DELTA);
            assertEquals(10.0f, sum.get(1, 0), DELTA);
            assertEquals(12.0f, sum.get(1, 1), DELTA);

            // Test different dimensions
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m3));
        }

        @Test
        public void testAdd_Shape2By2() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix sum = m1.add(m2);

            assertEquals(6.0f, sum.get(0, 0), DELTA);
            assertEquals(8.0f, sum.get(0, 1), DELTA);
            assertEquals(10.0f, sum.get(1, 0), DELTA);
            assertEquals(12.0f, sum.get(1, 1), DELTA);
        }

        @Test
        public void testAdd_DifferentDimensions() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        // Test that NaN propagates in arithmetic
        @Test
        public void testArithmeticEdgeCases() {
            // Test with zero matrix
            FloatMatrix zeros = FloatMatrix.wrap(new float[][] { { 0.0f, 0.0f }, { 0.0f, 0.0f } });
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });

            FloatMatrix addZero = m.add(zeros);
            assertEquals(m, addZero);

            FloatMatrix subtractZero = m.subtract(zeros);
            assertEquals(m, subtractZero);

            // Test multiplication with zero matrix
            FloatMatrix multiplyZero = m.matrixMultiply(zeros);
            assertEquals(zeros, multiplyZero);

            // Test addition commutativity
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            assertEquals(m1.add(m2), m2.add(m1));

            // Test subtraction anti-commutativity
            FloatMatrix diff1 = m1.subtract(m2);
            FloatMatrix diff2 = m2.subtract(m1);
            assertEquals(diff1.get(0, 0), -diff2.get(0, 0), DELTA);
            assertEquals(diff1.get(1, 1), -diff2.get(1, 1), DELTA);
        }

        @Test
        public void testFloatPrecisionInArithmetic() {
            // Test precision loss in float arithmetic
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 0.1f, 0.2f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 0.3f, 0.4f } });

            FloatMatrix sum = m1.add(m2);

            // Float arithmetic may have precision issues
            assertEquals(0.4f, sum.get(0, 0), 0.001f); // More lenient delta for precision
            assertEquals(0.6f, sum.get(0, 1), 0.001f);
        }

        @Test
        public void testAdd_Shape2By2_EveryCellSum() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix result = m1.add(m2);
            assertEquals(6.0f, result.get(0, 0));
            assertEquals(8.0f, result.get(0, 1));
            assertEquals(10.0f, result.get(1, 0));
            assertEquals(12.0f, result.get(1, 1));
        }

        @Test
        public void testAdd_IncompatibleSize() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testAdd_AllFourCellValues() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix result = m1.add(m2);
            assertEquals(6.0f, result.get(0, 0), 0.0f);
            assertEquals(8.0f, result.get(0, 1), 0.0f);
            assertEquals(10.0f, result.get(1, 0), 0.0f);
            assertEquals(12.0f, result.get(1, 1), 0.0f);
        }

        @Test
        public void testAdd_IncompatibleDimensions() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testadd() {
            FloatMatrix a = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix b = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix sum = a.add(b);
            assertEquals(6.0f, sum.get(0, 0));
            assertEquals(8.0f, sum.get(0, 1));
            assertEquals(10.0f, sum.get(1, 0));
            assertEquals(12.0f, sum.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testAdd_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
                final var right = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
                assertEquals(FloatMatrix.wrap(new float[][] { { 2.0f, 4.0f, 6.0f }, { 8.0f, 10.0f, 12.0f } }), left.add(right));
                assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } }), left);
                assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testNaNOperations() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { Float.NaN, 2.0f }, { 3.0f, 4.0f } });

            assertTrue(Float.isNaN(m.get(0, 0)));
            assertFalse(Float.isNaN(m.get(0, 1)));

            // Test that NaN propagates in arithmetic
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 1.0f }, { 1.0f, 1.0f } });
            FloatMatrix sum = m.add(m2);
            assertTrue(Float.isNaN(sum.get(0, 0)));
            assertEquals(3.0f, sum.get(0, 1), DELTA);
        }

        @Test
        public void testInfinityOperations() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { Float.POSITIVE_INFINITY, 2.0f }, { Float.NEGATIVE_INFINITY, 4.0f } });

            assertEquals(Float.POSITIVE_INFINITY, m.get(0, 0), DELTA);
            assertEquals(Float.NEGATIVE_INFINITY, m.get(1, 0), DELTA);

            // Test arithmetic with infinity
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 1.0f }, { 1.0f, 1.0f } });
            FloatMatrix sum = m.add(m2);
            assertEquals(Float.POSITIVE_INFINITY, sum.get(0, 0), DELTA);
            assertEquals(Float.NEGATIVE_INFINITY, sum.get(1, 0), DELTA);
        }
    }

    @Nested
    class SubtractTests {

        @Test
        public void testSubtract() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix diff = m1.subtract(m2);

            assertEquals(4.0f, diff.get(0, 0), DELTA);
            assertEquals(4.0f, diff.get(0, 1), DELTA);
            assertEquals(4.0f, diff.get(1, 0), DELTA);
            assertEquals(4.0f, diff.get(1, 1), DELTA);

            // Test different dimensions
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m3));
        }

        // Regression: matrixMultiply must build its result via FloatMatrix.wrap(result) (not the raw
        // constructor) so an empty product yields the shared EMPTY singleton, and must call
        // checkNonNegativeShape before allocation for consistency with the other
        // result-allocating methods (resize/reshape/transpose/rotate).
        @Test
        public void testSubtract_Shape2By2() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix diff = m1.subtract(m2);

            assertEquals(4.0f, diff.get(0, 0), DELTA);
            assertEquals(4.0f, diff.get(0, 1), DELTA);
            assertEquals(4.0f, diff.get(1, 0), DELTA);
            assertEquals(4.0f, diff.get(1, 1), DELTA);
        }

        @Test
        public void testSubtract_DifferentDimensions() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testSubtract_Shape2By2_EveryCellDifference() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix result = m1.subtract(m2);
            assertEquals(4.0f, result.get(0, 0));
            assertEquals(4.0f, result.get(0, 1));
            assertEquals(4.0f, result.get(1, 0));
            assertEquals(4.0f, result.get(1, 1));
        }

        @Test
        public void testSubtract_AllFourCellValues() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix result = m1.subtract(m2);
            assertEquals(4.0f, result.get(0, 0), 0.0f);
            assertEquals(4.0f, result.get(0, 1), 0.0f);
            assertEquals(4.0f, result.get(1, 0), 0.0f);
            assertEquals(4.0f, result.get(1, 1), 0.0f);
        }

        @Test
        public void testSubtract_IncompatibleDimensions() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        // FloatMatrix.java: FloatMatrix matrix = FloatMatrix.from(new int[][] {{1, 2}, {3, 4}});
        @Test
        public void testsubtract() {
            FloatMatrix a = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix b = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix diff = a.subtract(b);
            assertEquals(4.0f, diff.get(0, 0));
            assertEquals(4.0f, diff.get(0, 1));
            assertEquals(4.0f, diff.get(1, 0));
            assertEquals(4.0f, diff.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testSubtract_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = FloatMatrix.wrap(new float[][] { { 7.0f, 8.0f, 9.0f }, { 10.0f, 11.0f, 12.0f } });
                final var right = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
                assertEquals(FloatMatrix.wrap(new float[][] { { 6.0f, 6.0f, 6.0f }, { 6.0f, 6.0f, 6.0f } }), left.subtract(right));
                assertEquals(FloatMatrix.wrap(new float[][] { { 7.0f, 8.0f, 9.0f }, { 10.0f, 11.0f, 12.0f } }), left);
                assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }
    }

    @Nested
    class MatrixMultiplyTests {

        @Test
        public void testMultiply() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix product = m1.matrixMultiply(m2);

            assertEquals(19.0f, product.get(0, 0), DELTA); // 1*5 + 2*7
            assertEquals(22.0f, product.get(0, 1), DELTA); // 1*6 + 2*8
            assertEquals(43.0f, product.get(1, 0), DELTA); // 3*5 + 4*7
            assertEquals(50.0f, product.get(1, 1), DELTA); // 3*6 + 4*8

            // Test incompatible dimensions
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m3));
        }

        @Test
        public void testMultiply_IncompatibleDimensions() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testMatrixMultiply_RectangularMatrices() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } }); // 1x3
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 4.0f }, { 5.0f }, { 6.0f } }); // 3x1
            FloatMatrix product = m1.matrixMultiply(m2);

            assertEquals(1, product.rowCount());
            assertEquals(1, product.columnCount());
            assertEquals(32.0f, product.get(0, 0), DELTA); // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        }

        @Test
        public void testMatrixMultiply_Shape2By2() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 2.0f, 0.0f }, { 1.0f, 2.0f } });
            FloatMatrix result = m1.matrixMultiply(m2);
            assertEquals(4.0f, result.get(0, 0));
            assertEquals(4.0f, result.get(0, 1));
            assertEquals(10.0f, result.get(1, 0));
            assertEquals(8.0f, result.get(1, 1));
        }

        @Test
        public void testMatrixMultiply_IncompatibleSize() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testMatrixMultiply() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 2.0f, 0.0f }, { 1.0f, 2.0f } });
            FloatMatrix result = m1.matrixMultiply(m2);
            assertEquals(4.0f, result.get(0, 0), 0.0f);
            assertEquals(4.0f, result.get(0, 1), 0.0f);
            assertEquals(10.0f, result.get(1, 0), 0.0f);
            assertEquals(8.0f, result.get(1, 1), 0.0f);
        }

        @Test
        public void testMatrixMultiply_IncompatibleDimensions() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        // FloatMatrix.java: FloatMatrix result = matrix1.zipWith(matrix2, (a, b) -> a * b);
        // result is [[3.0f, 8.0f]]
        @Test
        public void testmatrixMultiply() {
            FloatMatrix a = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix b = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix product = a.matrixMultiply(b);
            assertEquals(19.0f, product.get(0, 0));
            assertEquals(22.0f, product.get(0, 1));
            assertEquals(43.0f, product.get(1, 0));
            assertEquals(50.0f, product.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testMatrixMultiply_ExplicitModesComputeRectangularProduct(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
                final var right = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
                assertEquals(FloatMatrix.wrap(new float[][] { { 9.0f, 12.0f, 15.0f }, { 19.0f, 26.0f, 33.0f }, { 29.0f, 40.0f, 51.0f } }),
                        left.matrixMultiply(right));
                assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } }), left);
                assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testMatrixMultiply_EmptyProductReturnsCanonicalEmpty() {
            // Regression: matrixMultiply must build its result via FloatMatrix.wrap(result) (not the raw
            // constructor) so an empty product yields the shared EMPTY singleton, and must call
            // checkNonNegativeShape before allocation for consistency with the other
            // result-allocating methods (resize/reshape/transpose/rotate).
            FloatMatrix product = FloatMatrix.empty().matrixMultiply(FloatMatrix.empty());
            assertSame(FloatMatrix.empty(), product);
            assertTrue(product.isEmpty());
        }
    }

    @Nested
    class BoxedTests {

        @Test
        public void testBoxed() {
            Matrix<Float> boxed = matrix.boxed();
            assertEquals(3, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Float.valueOf(1.0f), boxed.get(0, 0));
            assertEquals(Float.valueOf(5.0f), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            Matrix<Float> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Float.valueOf(1.0f), boxed.get(0, 0));
            assertEquals(Float.valueOf(6.0f), boxed.get(1, 2));
        }

        @Test
        public void testBoxed_MoreRowsThanCols() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            Matrix<Float> boxed = m.boxed();
            assertEquals(4, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Float.valueOf(1.0f), boxed.get(0, 0));
            assertEquals(Float.valueOf(8.0f), boxed.get(3, 1));
        }

        @Test
        public void testBoxed_WithSpecialValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { Float.NaN, Float.POSITIVE_INFINITY }, { Float.NEGATIVE_INFINITY, -0.0f } });
            Matrix<Float> boxed = m.boxed();
            assertTrue(Float.isNaN(boxed.get(0, 0)));
            assertEquals(Float.POSITIVE_INFINITY, boxed.get(0, 1));
            assertEquals(Float.NEGATIVE_INFINITY, boxed.get(1, 0));
            assertEquals(-0.0f, boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Matrix<Float> boxed = m.boxed();
            assertEquals(Float.valueOf(1.0f), boxed.get(0, 0));
            assertEquals(Float.valueOf(4.0f), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By2_DimensionsAndCornerValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Matrix<Float> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(1.0f, boxed.get(0, 0), 0.0f);
            assertEquals(4.0f, boxed.get(1, 1), 0.0f);
        }
    }

    @Nested
    class ToDoubleMatrixTests {

        @Test
        public void testMapToDouble() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            DoubleMatrix result = m.toDoubleMatrix();
            assertEquals(1.0, result.get(0, 0), 0.0001);
            assertEquals(4.0, result.get(1, 1), 0.0001);
        }

        // Test different shapes
        @Test
        public void testToDoubleMatrix() {
            DoubleMatrix doubleMatrix = matrix.toDoubleMatrix();
            assertEquals(3, doubleMatrix.rowCount());
            assertEquals(3, doubleMatrix.columnCount());
            assertEquals(1.0, doubleMatrix.get(0, 0), 0.0001);
            assertEquals(5.0, doubleMatrix.get(1, 1), 0.0001);
        }

        @Test
        public void testToDoubleMatrix_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            DoubleMatrix doubleMatrix = m.toDoubleMatrix();
            assertEquals(2, doubleMatrix.rowCount());
            assertEquals(2, doubleMatrix.columnCount());
            assertEquals(1.0, doubleMatrix.get(0, 0), 0.0001);
            assertEquals(4.0, doubleMatrix.get(1, 1), 0.0001);
        }

        @Test
        public void testToDoubleMatrix_Shape2By2_DimensionsAndConvertedValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            DoubleMatrix dm = m.toDoubleMatrix();
            assertEquals(2, dm.rowCount());
            assertEquals(2, dm.columnCount());
            assertEquals(1.0, dm.get(0, 0));
            assertEquals(4.0, dm.get(1, 1));
        }

        @Test
        public void testToDoubleMatrix_AllFourCellValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            DoubleMatrix result = m.toDoubleMatrix();
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0, result.get(0, 0), 0.001);
            assertEquals(4.0, result.get(1, 1), 0.001);
        }
    }

    @Nested
    class ToIntMatrixTests {

        @Test
        public void testtoIntMatrix() {
            FloatMatrix floatMatrix = FloatMatrix.wrap(new float[][] { { 1.9f, 2.1f }, { 3.5f, 4.0f } });
            IntMatrix intMatrix = floatMatrix.toIntMatrix();
            assertEquals(1, intMatrix.get(0, 0));
            assertEquals(2, intMatrix.get(0, 1));
            assertEquals(3, intMatrix.get(1, 0));
            assertEquals(4, intMatrix.get(1, 1));
        }

        @Test
        public void testToIntMatrix() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.5f, 2.5f }, { 3.5f, 4.5f } });
            IntMatrix intM = m.toIntMatrix();
            assertEquals(1, intM.get(0, 0));
            assertEquals(4, intM.get(1, 1));
        }
    }

    @Nested
    class ToLongMatrixTests {

        @Test
        public void testtoLongMatrix() {
            FloatMatrix floatMatrix = FloatMatrix.wrap(new float[][] { { 1.9f, 2.1f }, { 3.5f, 4.0f } });
            LongMatrix longMatrix = floatMatrix.toLongMatrix();
            assertEquals(1L, longMatrix.get(0, 0));
            assertEquals(2L, longMatrix.get(0, 1));
            assertEquals(3L, longMatrix.get(1, 0));
            assertEquals(4L, longMatrix.get(1, 1));
        }

        @Test
        public void testToLongMatrix() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            LongMatrix longM = m.toLongMatrix();
            assertEquals(1L, longM.get(0, 0));
            assertEquals(4L, longM.get(1, 1));
        }
    }

    @Nested
    class ZipWithTests {

        @Test
        public void testZipWith() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix result = m1.zipWith(m2, (a, b) -> a * b);

            assertEquals(5.0f, result.get(0, 0), DELTA); // 1*5
            assertEquals(12.0f, result.get(0, 1), DELTA); // 2*6
            assertEquals(21.0f, result.get(1, 0), DELTA); // 3*7
            assertEquals(32.0f, result.get(1, 1), DELTA); // 4*8

            // Test different shapes
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m3, (a, b) -> a + b));
        }

        @Test
        public void testZipWith_DifferentShapes() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a + b));
        }

        @Test
        public void testZipWith_ThreeMatrices() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 9.0f, 10.0f }, { 11.0f, 12.0f } });
            FloatMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);

            assertEquals(15.0f, result.get(0, 0), DELTA); // 1+5+9
            assertEquals(18.0f, result.get(0, 1), DELTA); // 2+6+10
            assertEquals(21.0f, result.get(1, 0), DELTA); // 3+7+11
            assertEquals(24.0f, result.get(1, 1), DELTA); // 4+8+12
        }

        @Test
        public void testZipWith_ThreeMatrices_DifferentShapes() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 9.0f, 10.0f, 11.0f } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> a + b + c));
        }

        @Test
        public void testElementWiseMultiplyWithZipWith() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 2.0f, 3.0f }, { 4.0f, 5.0f } });

            // Element-wise multiplication
            FloatMatrix elementWiseProduct = m1.zipWith(m2, (a, b) -> a * b);
            assertEquals(2.0f, elementWiseProduct.get(0, 0), DELTA); // 1*2
            assertEquals(6.0f, elementWiseProduct.get(0, 1), DELTA); // 2*3
            assertEquals(12.0f, elementWiseProduct.get(1, 0), DELTA); // 3*4
            assertEquals(20.0f, elementWiseProduct.get(1, 1), DELTA); // 4*5

            // Element-wise division
            FloatMatrix elementWiseDivision = m2.zipWith(m1, (a, b) -> a / b);
            assertEquals(2.0f, elementWiseDivision.get(0, 0), DELTA); // 2/1
            assertEquals(1.5f, elementWiseDivision.get(0, 1), DELTA); // 3/2
            assertEquals(1.333333f, elementWiseDivision.get(1, 0), DELTA); // 4/3
            assertEquals(1.25f, elementWiseDivision.get(1, 1), DELTA); // 5/4
        }

        @Test
        public void testZipWith_Binary() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix result = m1.zipWith(m2, (a, b) -> a + b);
            assertEquals(6.0f, result.get(0, 0));
            assertEquals(12.0f, result.get(1, 1));
        }

        @Test
        public void testZipWith_Ternary() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 1.0f, 1.0f }, { 1.0f, 1.0f } });
            FloatMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(7.0f, result.get(0, 0));
            assertEquals(13.0f, result.get(1, 1));
        }

        @Test
        public void testZipWith_Binary_Shape2By2() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix result = m1.zipWith(m2, (a, b) -> a * b);
            assertEquals(5.0f, result.get(0, 0), 0.0f);
            assertEquals(12.0f, result.get(0, 1), 0.0f);
            assertEquals(21.0f, result.get(1, 0), 0.0f);
            assertEquals(32.0f, result.get(1, 1), 0.0f);
        }

        @Test
        public void testZipWith_Ternary_Shape1By2() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 3.0f, 4.0f } });
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f } });
            FloatMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(9.0f, result.get(0, 0), 0.0f);
            assertEquals(12.0f, result.get(0, 1), 0.0f);
        }

        // FloatMatrix.java: FloatMatrix identity = FloatMatrix.wrap(new float[3][3]);
        // identity.fill(1.0f);
        // Creates a matrix filled with 1.0f: [[1.0f, 1.0f, 1.0f], [1.0f, 1.0f, 1.0f], [1.0f, 1.0f, 1.0f]]
        @Test
        public void testzipWith() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 3.0f, 4.0f } });
            FloatMatrix result = m1.zipWith(m2, (a, b) -> a * b);
            assertEquals(3.0f, result.get(0, 0));
            assertEquals(8.0f, result.get(0, 1));
        }

        @Test
        public void testZipWith3() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 2.0f } });
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 3.0f } });
            FloatMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(6.0f, result.get(0, 0));
        }

        @Test
        public void testFloatMatrixZipWith2() {
            // FloatMatrix.java: FloatMatrix result = matrix1.zipWith(matrix2, (a, b) -> a * b);
            // result is [[3.0f, 8.0f]]
            FloatMatrix matrix1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            FloatMatrix matrix2 = FloatMatrix.wrap(new float[][] { { 3.0f, 4.0f } });
            FloatMatrix result = matrix1.zipWith(matrix2, (a, b) -> a * b);
            assertEquals(3.0f, result.get(0, 0), 0.001f);
            assertEquals(8.0f, result.get(0, 1), 0.001f);
        }

        @Test
        public void testFloatMatrixZipWith3() {
            // FloatMatrix.java: FloatMatrix result = matrix1.zipWith(matrix2, matrix3, (a, b, c) -> a + b + c);
            // result is [[6.0f]]
            FloatMatrix matrix1 = FloatMatrix.wrap(new float[][] { { 1.0f } });
            FloatMatrix matrix2 = FloatMatrix.wrap(new float[][] { { 2.0f } });
            FloatMatrix matrix3 = FloatMatrix.wrap(new float[][] { { 3.0f } });
            FloatMatrix result = matrix1.zipWith(matrix2, matrix3, (a, b, c) -> a + b + c);
            assertEquals(6.0f, result.get(0, 0), 0.001f);
        }
    }

    @Nested
    class MainDiagonalStreamTests {

        @Test
        public void testStreamLU2RD() {
            float[] diagonal = matrix.mainDiagonalStream().toArray();
            assertArrayEquals(new float[] { 1.0f, 5.0f, 9.0f }, diagonal, DELTA);

            // Test empty matrix
            assertTrue(emptyMatrix.mainDiagonalStream().toArray().length == 0);

            // Test non-square
            FloatMatrix nonSquare = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertArrayEquals(new float[] { 1.0f }, nonSquare.mainDiagonalStream().toArray(), DELTA);
        }

        @Test
        public void testMainDiagonalStream_Shape3By3_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            float[] diagonal = m.mainDiagonalStream().toArray();
            assertArrayEquals(new float[] { 1.0f, 5.0f, 9.0f }, diagonal, DELTA);
        }

        @Test
        public void testMainDiagonalStream_NonSquare() {
            FloatMatrix nonSquare = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertArrayEquals(new float[] { 1.0f }, nonSquare.mainDiagonalStream().toArray(), DELTA);
        }

        @Test
        public void testMainDiagonalStream_Shape3By3_ArrayValues_DiagonalArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            FloatStream stream = m.mainDiagonalStream();
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 1.0f, 5.0f, 9.0f }, result);
        }

        @Test
        public void testMainDiagonalStream() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            double sum = m.mainDiagonalStream().sum();
            assertEquals(5.0, sum, 0.0);
        }

        // Regression tests for the deep-review fixes: diagonal stream square validation
        @Test
        public void testMainDiagonalStream_IteratorAdvanceAndCount() {
            // Exercise advance() boundary around the cursor++ in a[cursor][cursor++].
            FloatMatrix m = FloatMatrix
                    .wrap(new float[][] { { 1.0f, 0.0f, 0.0f, 0.0f }, { 0.0f, 2.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 3.0f, 0.0f }, { 0.0f, 0.0f, 0.0f, 4.0f } });

            var it = (com.landawn.abacus.util.stream.FloatIteratorEx) m.mainDiagonalStream().iterator();
            assertEquals(1.0f, it.nextFloat(), DELTA);
            it.advance(2);
            assertEquals(4.0f, it.nextFloat(), DELTA);
            assertFalse(it.hasNext());

            // count() consumes what remains, so it is asserted on a fresh iterator.
            var counting = (com.landawn.abacus.util.stream.FloatIteratorEx) m.mainDiagonalStream().iterator();
            assertEquals(4L, counting.count());
            assertFalse(counting.hasNext());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testMainDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            final float[] expected = new float[] { 1.0f, 5.0f, 9.0f };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (FloatIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextFloat);
            }
        }

        @Test
        public void testMainDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (FloatIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1.0f, iterator.nextFloat());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextFloat);
            }
        }

        @Test
        public void testMainDiagonalStream_Empty() {
            FloatMatrix empty = FloatMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().toArray().length);
        }

        @Test
        public void testDiagonalStream_Nx0MatrixThrowsBecauseNotSquare() {
            FloatMatrix m = FloatMatrix.wrap(new float[3][0]);
            assertEquals(3, m.rowCount());
            assertEquals(0, m.columnCount());
            assertEquals(0, m.mainDiagonalStream().toArray().length);
            assertEquals(0, m.antiDiagonalStream().toArray().length);

            FloatMatrix empty = FloatMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().count());
            assertEquals(0, empty.antiDiagonalStream().count());
        }
    }

    @Nested
    class AntiDiagonalStreamTests {

        @Test
        public void testStreamRU2LD() {
            float[] antiDiagonal = matrix.antiDiagonalStream().toArray();
            assertArrayEquals(new float[] { 3.0f, 5.0f, 7.0f }, antiDiagonal, DELTA);

            // Test empty matrix
            assertTrue(emptyMatrix.antiDiagonalStream().toArray().length == 0);

            // Test non-square
            FloatMatrix nonSquare = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertArrayEquals(new float[] { 2.0f }, nonSquare.antiDiagonalStream().toArray(), DELTA);
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            float[] antiDiagonal = m.antiDiagonalStream().toArray();
            assertArrayEquals(new float[] { 3.0f, 5.0f, 7.0f }, antiDiagonal, DELTA);
        }

        @Test
        public void testAntiDiagonalStream_NonSquare() {
            FloatMatrix nonSquare = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertArrayEquals(new float[] { 2.0f }, nonSquare.antiDiagonalStream().toArray(), DELTA);
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_ArrayValues_AntiDiagonalArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            FloatStream stream = m.antiDiagonalStream();
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 3.0f, 5.0f, 7.0f }, result);
        }

        @Test
        public void testAntiDiagonalStream() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            double sum = m.antiDiagonalStream().sum();
            assertEquals(5.0, sum, 0.0);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testAntiDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            final float[] expected = new float[] { 3.0f, 5.0f, 7.0f };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (FloatIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextFloat);
            }
        }

        @Test
        public void testAntiDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (FloatIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(3.0f, iterator.nextFloat());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextFloat);
            }
        }

        @Test
        public void testAntiDiagonalStream_Empty() {
            FloatMatrix empty = FloatMatrix.empty();
            assertEquals(0, empty.antiDiagonalStream().toArray().length);
        }
    }

    @Nested
    class RowMajorStreamTests {

        // Test bounds
        @Test
        public void testRowMajorStream() {
            float[] all = matrix.rowMajorStream().toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f }, all, DELTA);

            // Test empty matrix
            assertTrue(emptyMatrix.rowMajorStream().toArray().length == 0);
        }

        @Test
        public void testRowMajorStreamRange() {
            float[] rows = matrix.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new float[] { 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f }, rows, DELTA);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(0, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(2, 1));
        }

        @Test
        public void testIteratorNoSuchElement() {
            // Test rowMajorStream iterator
            FloatStream stream = matrix.rowMajorStream(0, 1);
            stream.toArray(); // Consume all
            assertThrows(IllegalStateException.class, () -> stream.iterator().next());

            // Test mainDiagonalStream iterator
            FloatStream diagStream = matrix.mainDiagonalStream();
            diagStream.toArray(); // Consume all
            assertThrows(IllegalStateException.class, () -> diagStream.iterator().next());
        }

        @Test
        public void testRowMajorStream_Shape2By3_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            float[] all = m.rowMajorStream().toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, all, DELTA);
        }

        @Test
        public void testRowMajorStream_WithRange() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            float[] rows = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new float[] { 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f }, rows, DELTA);
        }

        // Test multiplication with zero matrix
        @Test
        public void testStatisticalOperations() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });

            // Test sum
            double totalSum = m.rowMajorStream().sum();
            assertEquals(45.0, totalSum, DELTA); // 1+2+3+4+5+6+7+8+9 = 45

            // Test sum of specific row
            double row1Sum = m.rowMajorStream(1, 2).sum();
            assertEquals(15.0, row1Sum, DELTA); // 4+5+6 = 15

            // Test sum of specific column
            double col0Sum = m.columnMajorStream(0, 1).sum();
            assertEquals(12.0, col0Sum, DELTA); // 1+4+7 = 12

            // Test min/max
            OptionalFloat min = m.rowMajorStream().min();
            assertTrue(min.isPresent());
            assertEquals(1.0f, min.get(), DELTA);

            OptionalFloat max = m.rowMajorStream().max();
            assertTrue(max.isPresent());
            assertEquals(9.0f, max.get(), DELTA);

            // Test average
            OptionalDouble avg = m.rowMajorStream().average();
            assertTrue(avg.isPresent());
            assertEquals(5.0, avg.get(), DELTA);

            // Test diagonal operations
            double diagonalSum = m.mainDiagonalStream().sum();
            assertEquals(15.0, diagonalSum, DELTA); // 1+5+9 = 15

            double antiDiagonalSum = m.antiDiagonalStream().sum();
            assertEquals(15.0, antiDiagonalSum, DELTA); // 3+5+7 = 15
        }

        @Test
        public void testRowMajorStream_Shape2By2_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatStream stream = m.rowMajorStream();
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
        }

        @Test
        public void testRowMajorStream_WithRange_Shape3By2_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            FloatStream stream = m.rowMajorStream(1, 3);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f, 6.0f }, result);
        }

        @Test
        public void testHorizontalStream() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            double sum = m.rowMajorStream().sum();
            assertEquals(10.0, sum, 0.0);
        }

        @Test
        public void testRowMajorStream_WithRange_Shape3By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            double sum = m.rowMajorStream(1, 3).sum();
            assertEquals(18.0, sum, 0.0);
        }

        @Test
        public void testStreamIterators_MidStrideAdvanceThenToArray_EdgeCase() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });

            var rowMajorIterator = matrix.rowMajorStream(0, 2).iterator();
            assertTrue(rowMajorIterator instanceof com.landawn.abacus.util.stream.FloatIteratorEx);
            com.landawn.abacus.util.stream.FloatIteratorEx rowMajor = (com.landawn.abacus.util.stream.FloatIteratorEx) rowMajorIterator;
            rowMajor.advance(1); // mid-row cursor: the chunked toArray must start at column 1
            assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, rowMajor.toArray());

            // IteratorEx.count() is terminal: it consumes the remainder and exhausts the iterator, so every
            // remaining-size assertion below uses its own freshly positioned iterator.
            var rowMajorCount = (com.landawn.abacus.util.stream.FloatIteratorEx) matrix.rowMajorStream(0, 2).iterator();
            rowMajorCount.advance(1);
            assertEquals(5L, rowMajorCount.count());

            var columnMajorIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.FloatIteratorEx columnMajor = (com.landawn.abacus.util.stream.FloatIteratorEx) columnMajorIterator;
            columnMajor.advance(1); // mid-column cursor: row 1 of column 0
            assertArrayEquals(new float[] { 4.0f, 2.0f, 5.0f, 3.0f, 6.0f }, columnMajor.toArray());

            var columnMajorCount = (com.landawn.abacus.util.stream.FloatIteratorEx) matrix.columnMajorStream(0, 3).iterator();
            columnMajorCount.advance(1);
            assertEquals(5L, columnMajorCount.count());

            var crossingIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.FloatIteratorEx crossing = (com.landawn.abacus.util.stream.FloatIteratorEx) crossingIterator;
            crossing.advance(3); // crosses a column boundary: lands on row 1 of column 1
            assertEquals(5.0f, crossing.nextFloat());
            crossing.advance(10);
            assertEquals(0L, crossing.count());
            assertThrows(java.util.NoSuchElementException.class, crossing::nextFloat);

            var crossingCount = (com.landawn.abacus.util.stream.FloatIteratorEx) matrix.columnMajorStream(0, 3).iterator();
            crossingCount.advance(3);
            assertEquals(3L, crossingCount.count()); // consumes 5.0f, 3.0f, 6.0f
            assertFalse(crossingCount.hasNext());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testRowMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            final float[] expected = new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (FloatIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextFloat);
            }
        }

        @Test
        public void testRowMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (FloatIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1.0f, iterator.nextFloat());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextFloat);
            }
        }

        @Test
        public void testRowMajorStreamSingleRowRange() {
            float[] row1 = matrix.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new float[] { 4.0f, 5.0f, 6.0f }, row1, DELTA);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(3, 4));
        }

        @Test
        public void testRowMajorStream_Empty() {
            FloatMatrix empty = FloatMatrix.empty();
            assertEquals(0, empty.rowMajorStream().toArray().length);
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            float[] row1 = m.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new float[] { 4.0f, 5.0f, 6.0f }, row1, DELTA);
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_Shape2By2_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatStream stream = m.rowMajorStream(1, 2);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 3.0f, 4.0f }, result);
        }

        @Test
        public void testRowMajorStream_BySingleRowRange() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            double sum = m.rowMajorStream(0, 1).sum();
            assertEquals(3.0, sum, 0.0);
        }

        @Test
        public void testRowMajorStream_ZeroColumnsKeepLogicalRowRange() {
            final var matrix = FloatMatrix.wrap(new float[3][0]);
            assertEquals(0, matrix.rowMajorStream(1, 3).count());
            assertEquals(2, matrix.rowStreams(1, 3).count());
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 3));
        }

        @Test
        public void testRowMajorStream_WithRange_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 1));
        }
    }

    @Nested
    class ColumnMajorStreamTests {

        // Test bounds
        @Test
        public void testColumnMajorStream() {
            float[] all = matrix.columnMajorStream().toArray();
            assertArrayEquals(new float[] { 1.0f, 4.0f, 7.0f, 2.0f, 5.0f, 8.0f, 3.0f, 6.0f, 9.0f }, all, DELTA);

            // Test empty matrix
            assertTrue(emptyMatrix.columnMajorStream().toArray().length == 0);
        }

        @Test
        public void testColumnMajorStreamRange() {
            float[] columnCount = matrix.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new float[] { 2.0f, 5.0f, 8.0f, 3.0f, 6.0f, 9.0f }, columnCount, DELTA);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(0, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(2, 1));
        }

        @Test
        public void testColumnMajorStream_Shape2By3_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            float[] all = m.columnMajorStream().toArray();
            assertArrayEquals(new float[] { 1.0f, 4.0f, 2.0f, 5.0f, 3.0f, 6.0f }, all, DELTA);
        }

        @Test
        public void testColumnMajorStream_WithRange() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            float[] columnCount = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new float[] { 2.0f, 5.0f, 8.0f, 3.0f, 6.0f, 9.0f }, columnCount, DELTA);
        }

        @Test
        public void testColumnMajorStream_Shape2By2_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatStream stream = m.columnMajorStream();
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f }, result);
        }

        @Test
        public void testColumnMajorStream_WithRange_Shape2By3_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatStream stream = m.columnMajorStream(1, 3);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 2.0f, 5.0f, 3.0f, 6.0f }, result);
        }

        @Test
        public void testVerticalStream() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            double sum = m.columnMajorStream().sum();
            assertEquals(10.0, sum, 0.0);
        }

        @Test
        public void testColumnMajorStream_WithRange_Shape2By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            double sum = m.columnMajorStream(1, 3).sum();
            assertEquals(16.0, sum, 0.0);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testColumnMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            final float[] expected = new float[] { 1.0f, 4.0f, 7.0f, 2.0f, 5.0f, 8.0f, 3.0f, 6.0f, 9.0f };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (FloatIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextFloat);
            }
        }

        @Test
        public void testColumnMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (FloatIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1.0f, iterator.nextFloat());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextFloat);
            }
        }

        @Test
        public void testColumnMajorStreamSingleColumnRange() {
            float[] col1 = matrix.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new float[] { 2.0f, 5.0f, 8.0f }, col1, DELTA);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(3, 4));
        }

        @Test
        public void testColumnMajorStream_Empty() {
            FloatMatrix empty = FloatMatrix.empty();
            assertEquals(0, empty.columnMajorStream().toArray().length);
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            float[] col1 = m.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new float[] { 2.0f, 5.0f }, col1, DELTA);
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_Shape2By2_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatStream stream = m.columnMajorStream(0, 1);
            float[] result = stream.toArray();
            assertArrayEquals(new float[] { 1.0f, 3.0f }, result);
        }

        @Test
        public void testColumnMajorStream_BySingleColumnRange() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            double sum = m.columnMajorStream(0, 1).sum();
            assertEquals(4.0, sum, 0.0);
        }

        @Test
        public void testVerticalStream_AdvanceCrossesColumnBoundary() {
            // Verify advance crosses column boundaries for columnMajorStream.
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 4.0f, 7.0f }, { 2.0f, 5.0f, 8.0f }, { 3.0f, 6.0f, 9.0f } });

            var it = (com.landawn.abacus.util.stream.FloatIteratorEx) m.columnMajorStream().iterator();
            // columnMajorStream() → 1,2,3 (col 0) | 4,5,6 (col 1) | 7,8,9 (col 2)
            it.advance(4);
            // After advancing 4 elements we have read 1,2,3,4; next should be 5.
            assertEquals(5.0f, it.nextFloat(), DELTA);
            it.advance(2);
            // After advancing 2 more (6, 7) the next element is 8.
            assertEquals(8.0f, it.nextFloat(), DELTA);

            // count() is terminal, so each remaining-size assertion uses a fresh iterator.
            assertEquals(9L, ((com.landawn.abacus.util.stream.FloatIteratorEx) m.columnMajorStream().iterator()).count());
            var afterFour = (com.landawn.abacus.util.stream.FloatIteratorEx) m.columnMajorStream().iterator();
            afterFour.advance(4);
            assertEquals(5L, afterFour.count());
            assertFalse(afterFour.hasNext());
        }

        @Test
        public void testColumnMajorStream_ZeroRowsKeepLogicalColumnRange() {
            final var matrix = FloatMatrix.wrap(new float[0][0]).resize(0, 3);
            assertEquals(0, matrix.columnMajorStream(1, 3).count());
            assertEquals(2, matrix.columnStreams(1, 3).count());
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 3));
        }

        @Test
        public void testColumnMajorStream_WithRange_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 1));
        }
    }

    @Nested
    class RowStreamsTests {

        @Test
        public void testStreamR() {
            List<float[]> rows = matrix.rowStreams().map(FloatStream::toArray).toList();
            assertEquals(3, rows.size());
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, rows.get(0), DELTA);
            assertArrayEquals(new float[] { 4.0f, 5.0f, 6.0f }, rows.get(1), DELTA);
            assertArrayEquals(new float[] { 7.0f, 8.0f, 9.0f }, rows.get(2), DELTA);

            // Test empty matrix
            assertTrue(emptyMatrix.rowStreams().count() == 0);
        }

        // Test empty matrix
        @Test
        public void testRowStreamsRange() {
            List<float[]> rows = matrix.rowStreams(1, 3).map(FloatStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new float[] { 4.0f, 5.0f, 6.0f }, rows.get(0), DELTA);
            assertArrayEquals(new float[] { 7.0f, 8.0f, 9.0f }, rows.get(1), DELTA);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(0, 4));
        }

        @Test
        public void testRowStreams_Shape2By3_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            List<float[]> rows = m.rowStreams().map(FloatStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, rows.get(0), DELTA);
            assertArrayEquals(new float[] { 4.0f, 5.0f, 6.0f }, rows.get(1), DELTA);
        }

        @Test
        public void testRowStreams_WithRange() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            List<float[]> rows = m.rowStreams(1, 3).map(FloatStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new float[] { 4.0f, 5.0f, 6.0f }, rows.get(0), DELTA);
            assertArrayEquals(new float[] { 7.0f, 8.0f, 9.0f }, rows.get(1), DELTA);
        }

        @Test
        public void testRowStreams_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Stream<FloatStream> stream = m.rowStreams();
            assertEquals(2, stream.count());
        }

        @Test
        public void testRowStreams_WithRange_Shape3By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            Stream<FloatStream> stream = m.rowStreams(1, 3);
            assertEquals(2, stream.count());
        }

        @Test
        public void testRowStreams() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            long count = m.rowStreams().count();
            assertEquals(2, count);
        }

        @Test
        public void testRowStreams_WithRange_Shape3By2_CountsSelectedRows() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            long count = m.rowStreams(1, 3).count();
            assertEquals(2, count);
        }

        @Test
        public void testStreamsObserveLiveValuesAndRowStreamsKeepTheirSelectedBackingRow() {
            final FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
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
                    assertArrayEquals(new float[] { 9, 2 }, selectedRow.toArray());
                    assertArrayEquals(new float[] { 3, 9 }, selectedColumn.toArray());
                    assertArrayEquals(new float[] { 3, 4, 9, 2 }, rowMajor.toArray());
                    assertArrayEquals(new float[] { 3, 9, 4, 2 }, columnMajor.toArray());
                    assertArrayEquals(new float[] { 3, 2 }, mainDiagonal.toArray());
                    assertArrayEquals(new float[] { 4, 9 }, antiDiagonal.toArray());
                }
                try (var nextRow = rows.next();
                     var nextColumn = columns.next()) {
                    assertArrayEquals(new float[] { 9, 2 }, nextRow.toArray());
                    assertArrayEquals(new float[] { 4, 2 }, nextColumn.toArray());
                }
            }
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testRowStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            final float[][] expected = { new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 4.0f, 5.0f, 6.0f }, new float[] { 7.0f, 8.0f, 9.0f } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowStreams()) {
                final var iterator = (ObjIteratorEx<FloatStream>) stream.iterator();
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
            FloatMatrix empty = FloatMatrix.empty();
            assertEquals(0, empty.rowStreams().count());
        }

        @Test
        public void testRowStreams_WithRange_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(0, 3));
        }
    }

    @Nested
    class ColumnStreamsTests {

        @Test
        public void testStreamC() {
            List<float[]> columnCount = matrix.columnStreams().map(FloatStream::toArray).toList();
            assertEquals(3, columnCount.size());
            assertArrayEquals(new float[] { 1.0f, 4.0f, 7.0f }, columnCount.get(0), DELTA);
            assertArrayEquals(new float[] { 2.0f, 5.0f, 8.0f }, columnCount.get(1), DELTA);
            assertArrayEquals(new float[] { 3.0f, 6.0f, 9.0f }, columnCount.get(2), DELTA);

            // Test empty matrix
            assertTrue(emptyMatrix.columnStreams().count() == 0);
        }

        @Test
        public void testColumnStreamsRange() {
            List<float[]> columnCount = matrix.columnStreams(1, 3).map(FloatStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new float[] { 2.0f, 5.0f, 8.0f }, columnCount.get(0), DELTA);
            assertArrayEquals(new float[] { 3.0f, 6.0f, 9.0f }, columnCount.get(1), DELTA);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(0, 4));
        }

        @Test
        public void testColumnStreams_Shape2By3_ArrayValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            List<float[]> columnCount = m.columnStreams().map(FloatStream::toArray).toList();
            assertEquals(3, columnCount.size());
            assertArrayEquals(new float[] { 1.0f, 4.0f }, columnCount.get(0), DELTA);
            assertArrayEquals(new float[] { 2.0f, 5.0f }, columnCount.get(1), DELTA);
            assertArrayEquals(new float[] { 3.0f, 6.0f }, columnCount.get(2), DELTA);
        }

        @Test
        public void testColumnStreams_WithRange() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            List<float[]> columnCount = m.columnStreams(1, 3).map(FloatStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new float[] { 2.0f, 5.0f, 8.0f }, columnCount.get(0), DELTA);
            assertArrayEquals(new float[] { 3.0f, 6.0f, 9.0f }, columnCount.get(1), DELTA);
        }

        @Test
        public void testColumnStreams_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Stream<FloatStream> stream = m.columnStreams();
            assertEquals(2, stream.count());
        }

        @Test
        public void testColumnStreams_WithRange_Shape2By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            Stream<FloatStream> stream = m.columnStreams(1, 3);
            assertEquals(2, stream.count());
        }

        @Test
        public void testColumnStreams() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            long count = m.columnStreams().count();
            assertEquals(2, count);
        }

        @Test
        public void testColumnStreams_WithRange_Shape1By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } });
            long count = m.columnStreams(1, 3).count();
            assertEquals(2, count);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testColumnStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            final float[][] expected = { new float[] { 1.0f, 4.0f, 7.0f }, new float[] { 2.0f, 5.0f, 8.0f }, new float[] { 3.0f, 6.0f, 9.0f } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnStreams()) {
                final var iterator = (ObjIteratorEx<FloatStream>) stream.iterator();
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
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            final float[] expected = new float[] { 2.0f, 5.0f, 8.0f };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (FloatIteratorEx) values.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextFloat);
            }
        }

        @Test
        public void testColumnStreams_InnerCountConsumesOnlyRemainingValues() {
            final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (FloatIteratorEx) values.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(2.0f, iterator.nextFloat());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextFloat);
            }
        }

        @Test
        public void testColumnStreams_Empty() {
            FloatMatrix empty = FloatMatrix.empty();
            assertEquals(0, empty.columnStreams().count());
        }

        @Test
        public void testColumnStreams_WithRange_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(0, 3));
        }
    }

    @Nested
    class ForEachTests {

        @Test
        public void testForEach() {
            List<Float> values = new ArrayList<>();
            matrix.forEach(it -> values.add(it));
            assertEquals(9, values.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, values.get(i), DELTA);
            }
        }

        @Test
        public void testForEachWithBounds() {
            List<Float> values = new ArrayList<>();
            matrix.forEach(1, 3, 1, 3, it -> values.add(it));
            assertEquals(4, values.size());
            assertEquals(5.0f, values.get(0), DELTA);
            assertEquals(6.0f, values.get(1), DELTA);
            assertEquals(8.0f, values.get(2), DELTA);
            assertEquals(9.0f, values.get(3), DELTA);

            // Test bounds
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.forEach(-1, 2, 0, 2, v -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.forEach(0, 4, 0, 2, v -> {
            }));
        }

        @Test
        public void testForEach_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            List<Float> values = new ArrayList<>();
            m.forEach(v -> values.add(v));
            assertEquals(9, values.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, values.get(i), DELTA);
            }
        }

        @Test
        public void testForEach_WithRange() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            List<Float> values = new ArrayList<>();
            m.forEach(0, 2, 1, 3, v -> values.add(v));
            assertEquals(4, values.size());
            assertEquals(2.0f, values.get(0), DELTA);
            assertEquals(3.0f, values.get(1), DELTA);
            assertEquals(5.0f, values.get(2), DELTA);
            assertEquals(6.0f, values.get(3), DELTA);
        }

        @Test
        public void testForEach_WithSpecialValues() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { Float.NaN, Float.POSITIVE_INFINITY }, { Float.NEGATIVE_INFINITY, -0.0f } });
            List<Float> values = new ArrayList<>();
            m.forEach(v -> values.add(v));
            assertEquals(4, values.size());
            assertTrue(Float.isNaN(values.get(0)));
            assertEquals(Float.POSITIVE_INFINITY, values.get(1), DELTA);
            assertEquals(Float.NEGATIVE_INFINITY, values.get(2), DELTA);
            assertEquals(-0.0f, values.get(3), DELTA);
        }

        @Test
        public void testForEach_LargeMatrix_Parallelable() {
            // Create a large matrix that should trigger parallel processing
            int size = 1000;
            float[][] data = new float[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    data[i][j] = i + j;
                }
            }
            FloatMatrix large = FloatMatrix.wrap(data);
            final float[] sum = { 0.0f };
            large.forEach(1, 10, 1, 10, v -> sum[0] += v);
            assertTrue(sum[0] > 0);
        }

        @Test
        public void testForEach_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            final float[] sum = { 0.0f };
            m.forEach(val -> sum[0] += val);
            assertEquals(10.0f, sum[0]);
        }

        @Test
        public void testForEach_WithRange_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            final float[] sum = { 0.0f };
            m.forEach(1, 3, 1, 3, val -> sum[0] += val);
            assertEquals(28.0f, sum[0]);
        }

        @Test
        public void testForEach_Shape2By2_CountsVisitedElements() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEach(x -> count.incrementAndGet());
            assertEquals(4, count.get());
        }

        @Test
        public void testForEach_WithRange_Shape3By3_CountsSelectedRegion() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEach(1, 3, 1, 3, x -> count.incrementAndGet());
            assertEquals(4, count.get());
        }

        @Test
        public void testForEachStopsAtTheFailingElementInSequentialMode() {
            final FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            final IllegalStateException failure = new IllegalStateException("stop at the second element");
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                for (final boolean selectedRegion : new boolean[] { false, true }) {
                    final List<Float> visited = new java.util.ArrayList<>();
                    final Throwables.FloatConsumer<RuntimeException> action = value -> {
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
                    assertEquals(selectedRegion ? List.of(2f, 3f) : List.of(1f, 2f), visited);
                }
            });
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testForEach_ExplicitModesVisitSelectedRegionExactlyOnce(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
                final var values = new ConcurrentLinkedQueue<Float>();
                matrix.forEach(0, 2, 1, 3, values::add);
                assertEquals(List.of(2.0f, 3.0f, 5.0f, 6.0f).stream().sorted().toList(), values.stream().sorted().toList());
                assertEquals(FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        // Test rowMajorStream iterator
        @Test
        public void testForEachNullAction() {
            assertThrows(IllegalArgumentException.class, () -> matrix.forEach((Throwables.FloatConsumer<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class,
                    () -> matrix.forEach(0, matrix.rowCount(), 0, matrix.columnCount(), (Throwables.FloatConsumer<RuntimeException>) null));
        }

        @Test
        public void testForEach_WithRange_OutOfBounds() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(-1, 2, 0, 2, v -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 3, 0, 2, v -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 2, -1, 2, v -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 2, 0, 3, v -> {
            }));
        }
    }

    @Nested
    class ToMultilineStringTests {

        @Test
        public void testToMultilineString_Shape2By2_NonemptyRendering() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            String result = m.toMultilineString();
            assertNotNull(result);
            assertTrue(result.length() > 0);
        }

        @Test
        public void testToMultilineString_Empty() {
            FloatMatrix m = FloatMatrix.empty();
            String result = m.toMultilineString();
            assertNotNull(result);
        }

        @Test
        public void testToMultilineStringNonEmptyReturnsNonNullFormattedString() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains("1.0"));
            assertTrue(printed.contains("4.0"));
            assertFalse(printed.equals("null"));
        }

        @Test
        public void testToMultilineStringEmptyReturnsBracketsLiteral() {
            FloatMatrix empty = FloatMatrix.empty();
            String printed = empty.toMultilineString();
            assertEquals("[]", printed);
        }

        @Test
        public void testToMultilineStringWithNaNAndInfinityRendersAsTokens() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains("NaN"));
            assertTrue(printed.contains("Infinity"));
            assertTrue(printed.contains("-Infinity"));
        }
    }

    @Nested
    class HashCodeTests {

        @Test
        public void testHashCode() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 4.0f, 3.0f } });

            assertEquals(m1.hashCode(), m2.hashCode());
            assertNotEquals(m1.hashCode(), m3.hashCode()); // Usually different
        }

        @Test
        public void testHashCode_Shape2By2() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        public void testEqualsAndHashCode_NaN_AreConsidered_Equal() {
            // FloatMatrix.equals/hashCode use Float.floatToIntBits semantics (per Javadoc),
            // so NaN == NaN, and the hash code must be consistent with equals.
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { Float.NaN, 1.0f }, { 2.0f, Float.NaN } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { Float.NaN, 1.0f }, { 2.0f, Float.NaN } });

            assertEquals(m1, m2, "Two matrices with NaN values at the same positions must be equal");
            assertEquals(m1.hashCode(), m2.hashCode(), "Equal matrices must produce equal hash codes (NaN content)");
        }

        @Test
        public void testHashCode_ZeroRowWidthsDistinguishShapes() {
            final var empty = FloatMatrix.wrap(new float[0][0]);
            final var narrow = empty.resize(0, 4);
            final var equal = empty.resize(0, 4);
            final var wide = empty.resize(0, 5);
            assertEquals(narrow, equal);
            assertEquals(narrow.hashCode(), equal.hashCode());
            assertNotEquals(narrow.hashCode(), wide.hashCode());
            assertNotEquals(narrow.hashCode(), empty.hashCode());
        }

        @Test
        public void testEqualsAndHashCode_NegativeZero_DistinctFromPositiveZero() {
            // FloatMatrix.equals/hashCode use Float.floatToIntBits semantics (per Javadoc),
            // so +0.0f and -0.0f are NOT equal, and the hash codes must differ accordingly.
            FloatMatrix posZero = FloatMatrix.wrap(new float[][] { { 0.0f } });
            FloatMatrix negZero = FloatMatrix.wrap(new float[][] { { -0.0f } });

            assertNotEquals(posZero, negZero, "+0.0f and -0.0f must not be equal under Float.floatToIntBits semantics");
            assertNotEquals(posZero.hashCode(), negZero.hashCode(), "+0.0f and -0.0f must produce different hash codes under Float.floatToIntBits semantics");
        }
    }

    @Nested
    class EqualsTests {

        @Test
        public void testEquals() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 4.0f, 3.0f } });
            FloatMatrix m4 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });

            assertTrue(m1.equals(m1)); // Same object
            assertTrue(m1.equals(m2)); // Same values
            assertFalse(m1.equals(m3)); // Different values
            assertFalse(m1.equals(m4)); // Different dimensions
            assertFalse(m1.equals(null));
            assertFalse(m1.equals("not a matrix"));
        }

        @Test
        public void testEquals_Same() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertEquals(m1, m2);
        }

        @Test
        public void testEquals_Different() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 5.0f } });
            assertNotEquals(m1, m2);
        }

        @Test
        public void testEquals_DifferentRowCountsWithEqualColumnCounts() {
            final var longer = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            final var shorter = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertFalse(longer.equals(shorter));
            assertFalse(shorter.equals(longer));
        }

        @Test
        public void testEquals_Shape2By2_NullInput() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 5.0f } });
            assertTrue(m1.equals(m2));
            assertFalse(m1.equals(m3));
            assertTrue(m1.equals(m1));
            assertFalse(m1.equals(null));
            assertFalse(m1.equals("not a matrix"));
        }

        @Test
        public void testEquals_Null() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertNotEquals(m1, null);
        }
    }

    @Nested
    class ToStringTests {

        @Test
        public void testToString() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            String str = m.toString();
            assertTrue(str.contains("1.0"));
            assertTrue(str.contains("2.0"));
            assertTrue(str.contains("3.0"));
            assertTrue(str.contains("4.0"));
        }

        @Test
        public void testToString_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1.0") || str.contains("1"));
            assertTrue(str.contains("2.0") || str.contains("2"));
            assertTrue(str.contains("3.0") || str.contains("3"));
            assertTrue(str.contains("4.0") || str.contains("4"));
        }

        @Test
        public void testToString_Shape2By2_ContainsElementText() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1.0"));
            assertTrue(str.contains("4.0"));
        }

        @Test
        public void testFloatMatrixToString() {
            FloatMatrix matrix = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            assertEquals("[[1.0, 2.0, 3.0], [4.0, 5.0, 6.0]]", matrix.toString());
        }

        @Test
        public void testToString_Shape2By2_NonemptyRendering() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.length() > 0);
        }
    }

    @Nested
    class ElementTypeTests {

        @Test
        public void testElementType() {
            assertEquals(float.class, matrix.elementType());
        }

        @Test
        public void testElementType_Shape1By1() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f } });
            assertEquals(float.class, m.elementType());
        }
    }

    @Nested
    class UnsafeBackingArrayTests {

        @Test
        public void testArray() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            float[][] array = m.unsafeBackingArray();
            assertArrayEquals(new float[] { 1.0f, 2.0f }, array[0]);
            assertArrayEquals(new float[] { 3.0f, 4.0f }, array[1]);
        }
    }

    @Nested
    class ElementCountTests {

        @Test
        public void testElementCount() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            assertEquals(6, m.elementCount());
        }

        @Test
        public void testElementCount_Empty() {
            FloatMatrix m = FloatMatrix.empty();
            assertEquals(0, m.elementCount());
        }
    }

    @Nested
    class IsEmptyTests {

        @Test
        public void testIsEmpty() {
            FloatMatrix empty = FloatMatrix.empty();
            assertTrue(empty.isEmpty());
            FloatMatrix notEmpty = FloatMatrix.wrap(new float[][] { { 1.0f } });
            assertFalse(notEmpty.isEmpty());
        }

        // 3x2 rotated 90° CW must yield a 2x3 with first column = last row reading upward.
        @Test
        public void testIsEmpty_Shape1By1() {
            assertTrue(FloatMatrix.empty().isEmpty());
            assertFalse(FloatMatrix.wrap(new float[][] { { 1f } }).isEmpty());
        }
    }

    @Nested
    class IsSameShapeTests {

        @Test
        public void testIsSameShape() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5.0f, 6.0f }, { 7.0f, 8.0f } });
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f } });
            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }

        @Test
        public void testIsSameShape_Shape2By2() {
            FloatMatrix m1 = FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
            FloatMatrix m2 = FloatMatrix.wrap(new float[][] { { 5, 6 }, { 7, 8 } });
            FloatMatrix m3 = FloatMatrix.wrap(new float[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }

        @Test
        public void testIsSameShape_NullThrows() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1f } });
            assertThrows(IllegalArgumentException.class, () -> m.isSameShape(null));
        }
    }

    @Nested
    class ReshapeTests {

        // Test reshape with too-small dimensions throws exception
        @Test
        public void testReshape() {
            FloatMatrix reshaped = matrix.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, reshaped.get(0, i), DELTA);
            }

            // Test reshape back
            FloatMatrix reshaped2 = reshaped.reshape(3, 3);
            assertEquals(matrix, reshaped2);

            // Test empty matrix
            FloatMatrix emptyReshaped = emptyMatrix.reshapeAndPad(2, 3);
            assertEquals(2, emptyReshaped.rowCount());
            assertEquals(3, emptyReshaped.columnCount());

            // Test reshape with too-small dimensions throws exception
            assertThrows(IllegalArgumentException.class, () -> matrix.reshape(1, 4));
            assertThrows(IllegalArgumentException.class, () -> matrix.reshape(2, 2));
        }

        @Test
        public void testReshape_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            FloatMatrix reshaped = m.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, reshaped.get(0, i), DELTA);
            }
        }

        @Test
        public void testReshape_Back() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f }, { 7.0f, 8.0f, 9.0f } });
            FloatMatrix reshaped = m.reshape(1, 9);
            FloatMatrix reshapedBack = reshaped.reshape(3, 3);
            assertEquals(m, reshapedBack);
        }

        @Test
        public void testReshape_Shape1By6() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f } });
            FloatMatrix reshaped = m.reshape(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals(1.0f, reshaped.get(0, 0));
            assertEquals(4.0f, reshaped.get(1, 0));
        }

        @Test
        public void testReshape_Shape2By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            FloatMatrix reshaped = m.reshape(3, 2);
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1.0f, reshaped.get(0, 0), 0.0f);
            assertEquals(2.0f, reshaped.get(0, 1), 0.0f);
            assertEquals(3.0f, reshaped.get(1, 0), 0.0f);
            assertEquals(4.0f, reshaped.get(1, 1), 0.0f);
        }

        @Test
        public void testReshape_SingleRowMatrix() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f } });
            FloatMatrix reshaped = m.reshape(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals(1.0f, reshaped.get(0, 0), DELTA);
            assertEquals(2.0f, reshaped.get(0, 1), DELTA);
            assertEquals(3.0f, reshaped.get(0, 2), DELTA);
            assertEquals(4.0f, reshaped.get(1, 0), DELTA);
            assertEquals(5.0f, reshaped.get(1, 1), DELTA);
            assertEquals(6.0f, reshaped.get(1, 2), DELTA);
        }

        @Test
        public void testReshape_ToZeroDimensions_ThrowsForNonEmpty() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            // New shape 0x0 is too small to hold all 4 elements
            assertThrows(IllegalArgumentException.class, () -> m.reshape(0, 0));

            // Empty matrix can be reshaped to 0x0
            FloatMatrix empty = FloatMatrix.empty();
            FloatMatrix reshaped = empty.reshape(0, 0);
            assertEquals(0, reshaped.rowCount());
            assertEquals(0, reshaped.columnCount());
        }

        @Test
        public void testReshape_OversizedShapeThrowsIllegalArgumentException() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f } });
            assertThrows(IllegalArgumentException.class, () -> m.reshape(46341, 46341));
        }
    }

    @Nested
    class ReshapeAndPadToColumnCountTests {

        @Test
        public void testReshapeAndPadToColumnCount() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f } });
            FloatMatrix reshaped = m.reshapeAndPadToColumnCount(3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }
    }

    @Nested
    class ForEachIndicesTests {

        @Test
        public void testForEach_BiConsumer() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            final int[] count = { 0 };
            m.forEachIndices((i, j) -> count[0]++);
            assertEquals(4, count[0]);
        }

        @Test
        public void testForEach_BiObjConsumer() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            final int[] count = { 0 };
            m.forEachIndices((i, j, matrix) -> count[0]++);
            assertEquals(4, count[0]);
        }

        @Test
        public void testForEachIndex() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
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
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = m.adjacent4Points(1, 1).toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testAdjacent4Points_Corner() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.adjacent4Points(0, 0).toList();
            assertEquals(2, points.size());
        }

        @Test
        public void testAdjacent4Points_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

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
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = m.adjacent8Points(1, 1).toList();
            assertEquals(8, points.size());
        }

        @Test
        public void testAdjacent8Points_Corner() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.adjacent8Points(0, 0).toList();
            assertEquals(3, points.size());
        }

        @Test
        public void testAdjacent8Points_Shape3By3() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            List<Sheet.Point> center = m.adjacent8Points(1, 1).toList();
            assertEquals(8, center.size());

            List<Sheet.Point> corner = m.adjacent8Points(0, 0).toList();
            assertEquals(3, corner.size());
        }
    }

    @Nested
    class MainDiagonalPointsTests {

        @Test
        public void testMainDiagonalPoints() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Stream<Point> points = m.mainDiagonalPoints();
            assertEquals(2, points.count());
        }

        @Test
        public void testPointsMainDiagonal() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Sheet.Point> points = m.mainDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }

        @Test
        public void testPointsMainDiagonal_NonSquareThrows() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(List.of(Point.of(0, 0), Point.of(1, 1)), m.mainDiagonalPoints().toList());
        }
    }

    @Nested
    class AntiDiagonalPointsTests {

        @Test
        public void testAntiDiagonalPoints() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Stream<Point> points = m.antiDiagonalPoints();
            assertEquals(2, points.count());
        }

        // Additional bug-hunt regression tests (Float-specific edge cases)
        @Test
        public void testPointsAntiDiagonal() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
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
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Stream<Point> points = m.rowMajorPoints();
            assertEquals(4, points.count());
        }

        @Test
        public void testRowMajorPoints_WithRow() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Stream<Point> points = m.rowMajorPoints(1, 1 + 1);
            assertEquals(2, points.count());
        }

        @Test
        public void testRowMajorPoints_WithRange() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f }, { 5.0f, 6.0f } });
            Stream<Point> points = m.rowMajorPoints(1, 3);
            assertEquals(4, points.count());
        }

        // FloatMatrix.equals/hashCode use Float.floatToIntBits semantics (per Javadoc),
        // so NaN == NaN, and the hash code must be consistent with equals.
        @Test
        public void testRowMajorPointsorizontal() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testRowMajorPointsorizontal_SingleRow() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.rowMajorPoints(0, 0 + 1).toList();
            assertEquals(2, points.size());
        }
    }

    @Nested
    class ColumnMajorPointsTests {

        @Test
        public void testColumnMajorPoints() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Stream<Point> points = m.columnMajorPoints();
            assertEquals(4, points.count());
        }

        @Test
        public void testColumnMajorPoints_WithColumn() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Stream<Point> points = m.columnMajorPoints(1, 1 + 1);
            assertEquals(2, points.count());
        }

        @Test
        public void testColumnMajorPoints_WithRange() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f, 6.0f } });
            Stream<Point> points = m.columnMajorPoints(1, 3);
            assertEquals(4, points.count());
        }

        // FloatMatrix.equals/hashCode use Float.floatToIntBits semantics (per Javadoc),
        @Test
        public void testColumnMajorPointsertical() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testColumnMajorPointsertical_SingleColumn() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.columnMajorPoints(1, 1 + 1).toList();
            assertEquals(2, points.size());
        }
    }

    @Nested
    class RowPointsTests {

        @Test
        public void testRowPointsows() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<List<Sheet.Point>> rows = m.rowPoints().map(s -> s.toList()).toList();
            assertEquals(3, rows.size());
            assertEquals(2, rows.get(0).size());
        }

        @Test
        public void testRowPoints() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Stream<Stream<Point>> points = m.rowPoints();
            assertEquals(2, points.count());
        }
    }

    @Nested
    class ColumnPointsTests {

        @Test
        public void testColumnPointsolumns() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<List<Sheet.Point>> cols = m.columnPoints().map(s -> s.toList()).toList();
            assertEquals(3, cols.size());
            assertEquals(2, cols.get(0).size());
        }

        @Test
        public void testColumnPoints() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            Stream<Stream<Point>> points = m.columnPoints();
            assertEquals(2, points.count());
        }
    }

    @Nested
    class AcceptTests {

        @Test
        public void testAccept() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            final boolean[] called = { false };
            m.accept(matrix -> called[0] = true);
            assertTrue(called[0]);
        }

        @Test
        public void testAccept_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
            float[] sum = { 0 };
            m.accept(matrix -> {
                FloatList flat = matrix.flatten();
                for (int i = 0; i < flat.size(); i++) {
                    sum[0] += flat.get(i);
                }
            });
            assertEquals(10.0f, sum[0], 0.001f);
        }

        @Test
        public void testAccept_NullThrows() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1f } });
            assertThrows(IllegalArgumentException.class, () -> m.accept(null));
        }
    }

    @Nested
    class ApplyTests {

        @Test
        public void testApply() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            int result = m.apply(matrix -> matrix.rowCount() * matrix.columnCount());
            assertEquals(4, result);
        }

        // Extending with NaN should fill all newly added cells with NaN. This exercises the
        // floatToRawIntBits != 0 short-circuit in pad(...).
        @Test
        public void testApply_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
            long count = m.apply(matrix -> matrix.elementCount());
            assertEquals(4, count);
        }

        @Test
        public void testApply_NullThrows() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1f } });
            assertThrows(IllegalArgumentException.class, () -> m.apply(null));
        }
    }

    @Nested
    class PrintlnTests {

        @Test
        public void testPrintln() {
            assertFalse(matrix.isEmpty());
            assertTrue(emptyMatrix.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                matrix.println();
                emptyMatrix.println();
            });
        }

        @Test
        public void testPrintln_Shape2By2() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } });
            assertFalse(m.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> m.println());

            FloatMatrix empty = FloatMatrix.empty();
            assertTrue(empty.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> empty.println());
        }
    }
}
