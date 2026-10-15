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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.stream.DoubleIteratorEx;
import com.landawn.abacus.util.stream.DoubleStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u;

class DoubleMatrixTest extends MatrixTestSupport {

    private static final double DELTA = 0.0001;

    @Nested
    class ConstructorTests {

        @Test
        public void testConstructor() {
            // Test with null array
            assertThrows(IllegalArgumentException.class, () -> new DoubleMatrix(null));

            // Test with valid array
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix2 = new DoubleMatrix(arr);
            assertEquals(2, matrix2.rowCount());
            assertEquals(2, matrix2.columnCount());
        }

        @Test
        public void testConstructor_WithValidArray() {
            double[][] arr = { { 1.5, 2.5 }, { 3.5, 4.5 } };
            DoubleMatrix m = new DoubleMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.5, m.get(0, 0), DELTA);
            assertEquals(4.5, m.get(1, 1), DELTA);
        }

        @Test
        public void testConstructor_WithValidArray_Values() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix m = new DoubleMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0));
            assertEquals(4.0, m.get(1, 1));
        }

        @Test
        public void testConstructor_WithLargerMatrix() {
            double[][] arr = { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } };
            DoubleMatrix m = new DoubleMatrix(arr);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(5.0, m.get(1, 1));
        }

        @Test
        public void testConstructor_WithValidArray_Values_DimensionsAndCornerValues() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix m = new DoubleMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0), 0.0);
            assertEquals(4.0, m.get(1, 1), 0.0);
        }

        @Test
        public void testConstructor_WithSingleElement() {
            DoubleMatrix m = new DoubleMatrix(new double[][] { { 42.75 } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42.75, m.get(0, 0), DELTA);
        }

        @Test
        public void testConstructor_WithSingleElement_Shape1By1() {
            DoubleMatrix m = new DoubleMatrix(new double[][] { { 42.5 } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42.5, m.get(0, 0));
        }

        @Test
        public void testConstructor_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> new DoubleMatrix(null));
        }

        @Test
        public void testConstructor_WithEmptyArray() {
            DoubleMatrix m = new DoubleMatrix(new double[0][0]);
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
            assertTrue(m.isEmpty());
        }

        @Test
        public void testConstructor_WithSingleElement_Shape1By1_DimensionsAndValue() {
            DoubleMatrix m = new DoubleMatrix(new double[][] { { 42.5 } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42.5, m.get(0, 0), 0.0);
        }
    }

    @Nested
    class EmptyTests {

        @Test
        public void testEmpty_EmptyMatrix_Identity() {
            DoubleMatrix empty = DoubleMatrix.empty();
            assertEquals(0, empty.rowCount());
            assertEquals(0, empty.columnCount());
            assertTrue(empty.isEmpty());

            // Test singleton
            assertSame(DoubleMatrix.empty(), DoubleMatrix.empty());
        }

        @Test
        public void testEmptyMatrixOperations() {
            DoubleMatrix empty = DoubleMatrix.empty();

            assertTrue(empty.flatten().isEmpty());
            assertEquals(0, empty.copy().rowCount);
            assertEquals(empty, empty.transpose());
            assertEquals(empty, empty.rotate90());

            DoubleMatrix extended = empty.resize(2, 2, 5.5);
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals(5.5, extended.get(0, 0), DELTA);
        }
    }

    @Nested
    class WrapTests {

        // Test with null/empty
        @Test
        public void testWrap() {
            // Test with null/empty
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.wrap((double[][]) null));

            // Test with valid array
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);
            assertEquals(2, matrix.rowCount());
            assertEquals(2, matrix.columnCount());
        }

        @Test
        public void testWrap_withValidArray() {
            double[][] arr = { { 1.5, 2.5 }, { 3.5, 4.5 } };
            DoubleMatrix m = DoubleMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.5, m.get(0, 0), DELTA);
        }

        @Test
        public void testPrecisionWithSmallValues() {
            // Test precision with small decimal values
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 0.1 + 0.2, 0.3 }, { 1.0 / 3.0, 0.333333 } });

            // Due to floating point precision, 0.1 + 0.2 != 0.3 exactly
            assertEquals(0.3, m.get(0, 0), DELTA);
            assertEquals(0.3, m.get(0, 0), 1e-10); // But close enough with tolerance

            assertEquals(0.3, m.get(0, 1), DELTA);
        }

        @Test
        public void testWrap_WithValidArray_Values() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix m = DoubleMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0));
        }

        @Test
        public void testWrap_WithValidArray() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix m = DoubleMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0), 0.0);
        }

        @Test
        public void testCharMatrix_ToDoubleMatrix() {
            CharMatrix charMatrix = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            DoubleMatrix doubleMatrix = charMatrix.toDoubleMatrix();
            // Result: [[97.0, 98.0]]
            assertEquals(97.0, doubleMatrix.get(0, 0));
            assertEquals(98.0, doubleMatrix.get(0, 1));
        }

        @Test
        public void testShortMatrix_ToDoubleMatrix() {
            ShortMatrix shortMatrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix doubleMatrix = shortMatrix.toDoubleMatrix();
            assertEquals(1.0, doubleMatrix.get(0, 0));
            assertEquals(4.0, doubleMatrix.get(1, 1));
        }

        @Test
        public void testWrapAndConstructor_CopyOuterArrayButShareRows() {
            // Pins the documented contract (class doc, wrap @return, copyOf prose): wrap and the constructor
            // snapshot the caller's OUTER array, so replacing a row there is not visible, while the row
            // arrays themselves stay shared, so cell writes are visible in both directions.
            final double[][] data = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            final DoubleMatrix wrapped = DoubleMatrix.wrap(data);
            final DoubleMatrix constructed = new DoubleMatrix(data);

            data[0] = new double[] { 9.0, 9.0 };
            assertEquals(1.0, wrapped.get(0, 0));
            assertEquals(1.0, constructed.get(0, 0));

            data[1][0] = 30.0;
            assertEquals(30.0, wrapped.get(1, 0));
            assertEquals(30.0, constructed.get(1, 0));
            assertSame(data[1], wrapped.rowView(1));

            wrapped.set(1, 1, 40.0);
            assertEquals(40.0, data[1][1]);

            // copyOf, by contrast, shares nothing with the caller.
            final DoubleMatrix copied = DoubleMatrix.copyOf(data);
            data[1][1] = -1.0;
            assertEquals(40.0, copied.get(1, 1));
            assertNotSame(data[1], copied.rowView(1));
        }

        @Test
        public void testWrap_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.wrap((double[][]) null));
        }

        @Test
        public void testWrap_WithEmptyArray() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testWrap_WithSingleRow() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0, 4.0, 5.0 } });
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }

        @Test
        public void testSpecialDoubleValues_NegativeZero() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { -0.0, 0.0 }, { 1.0, -1.0 } });

            assertEquals(-0.0, m.get(0, 0), DELTA);
            assertEquals(0.0, m.get(0, 1), DELTA);

            // Test that -0.0 and 0.0 are equal in value
            assertTrue(m.get(0, 0) == m.get(0, 1));
        }
    }

    @Nested
    class CopyOfTests {

        @Test
        public void testCopyOf() {
            double[][] arr = { { 1, 2 }, { 3, 4 } };
            DoubleMatrix m = DoubleMatrix.copyOf(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0));
            assertEquals(4.0, m.get(1, 1));

            // Mutating the source array must not affect the copy
            arr[0][0] = 99;
            assertEquals(1.0, m.get(0, 0));

            // Mutating the copy must not affect the source array
            m.set(1, 1, 88);
            assertEquals(4.0, arr[1][1]);

            // null -> rejected; empty -> shared empty matrix
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.copyOf((double[][]) null));
            assertTrue(DoubleMatrix.copyOf(new double[0][0]).isEmpty());

            // non-rectangular -> IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.copyOf(new double[][] { { 1, 2 }, { 3 } }));
        }

        @Test
        public void testCopyOfValidationOrderAndIndependentEmptyRows() {
            assertEquals("Row 0 cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.copyOf(null, new double[] { 1 })).getMessage());
            assertEquals("Row 2 cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.copyOf(new double[] { 1 }, new double[] { 2 }, null)).getMessage());
            assertEquals("Matrix must be rectangular: expected 1 columns per row, but row 1 has 2 columns",
                    assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.copyOf(new double[] { 1 }, new double[] { 2, 3 }, null)).getMessage());

            final double[] emptyRow = new double[0];
            final DoubleMatrix copied = DoubleMatrix.copyOf(emptyRow, emptyRow);
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
        public void testFromFromFloat() {
            float[][] floatArr = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            DoubleMatrix matrix = DoubleMatrix.from(floatArr);
            assertEquals(2, matrix.rowCount());
            assertEquals(2, matrix.columnCount());
            assertEquals(1.0, matrix.get(0, 0));
            assertEquals(4.0, matrix.get(1, 1));
        }

        @Test
        public void testFromFromLongArray() {
            long[][] longs = { { 1L, 2L }, { 3L, 4L } };
            DoubleMatrix m = DoubleMatrix.from(longs);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0), DELTA);
            assertEquals(4.0, m.get(1, 1), DELTA);
        }

        @Test
        public void testFromFromLongArray_WithJaggedArray() {
            long[][] jagged = { { 1L, 2L }, { 3L } };
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from(jagged));
        }

        @Test
        public void testFromFromFloatArray() {
            float[][] floats = { { 1.5f, 2.5f }, { 3.5f, 4.5f } };
            DoubleMatrix m = DoubleMatrix.from(floats);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.5, m.get(0, 0), DELTA);
            assertEquals(4.5, m.get(1, 1), DELTA);
        }

        @Test
        public void testFromFromFloatArray_WithJaggedArray() {
            float[][] jagged = { { 1.5f, 2.5f }, { 3.5f } };
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from(jagged));
        }

        // Test the else branch (rows > columnCount) in rotate270() method
        @Test
        public void testConversionAccuracy_FromLong() {
            // Test that long values convert to double (may lose precision for very large longs)
            long[][] longs = { { Long.MAX_VALUE, Long.MIN_VALUE }, { 1000000000000L, -1000000000000L } };
            DoubleMatrix m = DoubleMatrix.from(longs);

            // Note: Very large long values may lose precision when converted to double
            assertEquals(Long.MAX_VALUE, m.get(0, 0), 1.0); // Large tolerance for precision loss
            assertEquals(Long.MIN_VALUE, m.get(0, 1), 1.0);
            assertEquals(1000000000000.0, m.get(1, 0), DELTA);
            assertEquals(-1000000000000.0, m.get(1, 1), DELTA);
        }

        // Test the else branch (rows > columnCount) in transpose() method
        @Test
        public void testConversionAccuracy_FromFloat() {
            // Test that float values convert accurately to double
            float[][] floats = { { 1.5f, 2.5f }, { Float.MAX_VALUE, Float.MIN_VALUE } };
            DoubleMatrix m = DoubleMatrix.from(floats);

            assertEquals(1.5, m.get(0, 0), DELTA);
            assertEquals(2.5, m.get(0, 1), DELTA);
            assertEquals(Float.MAX_VALUE, m.get(1, 0), 1e30); // Large tolerance for float max
            assertEquals(Float.MIN_VALUE, m.get(1, 1), 1e-40);
        }

        @Test
        public void testFromFromLongArray_Values() {
            long[][] longs = { { 1L, 2L }, { 3L, 4L } };
            DoubleMatrix m = DoubleMatrix.from(longs);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0));
            assertEquals(4.0, m.get(1, 1));
        }

        @Test
        public void testFromFromFloatArray_Values() {
            float[][] floats = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            DoubleMatrix m = DoubleMatrix.from(floats);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0), 0.001);
            assertEquals(4.0, m.get(1, 1), 0.001);
        }

        @Test
        public void testFromFromFloatArray_WithJaggedArray_Validation() {
            float[][] jagged = { { 1.0f, 2.0f }, { 3.0f } };
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from(jagged));
        }

        @Test
        public void testFrom_FromLongArray() {
            long[][] longs = { { 1L, 2L }, { 3L, 4L } };
            DoubleMatrix m = DoubleMatrix.from(longs);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0), 0.0);
            assertEquals(4.0, m.get(1, 1), 0.0);
        }

        @Test
        public void testFrom_FromFloatArray() {
            float[][] floats = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            DoubleMatrix m = DoubleMatrix.from(floats);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0), 0.0);
            assertEquals(4.0, m.get(1, 1), 0.0);
        }

        @Test
        public void testFromFromInt() {
            int[][] intArr = { { 1, 2 }, { 3, 4 } };
            DoubleMatrix matrix = DoubleMatrix.from(intArr);
            assertEquals(2, matrix.rowCount());
            assertEquals(2, matrix.columnCount());
            assertEquals(1.0, matrix.get(0, 0));
            assertEquals(4.0, matrix.get(1, 1));

            // Test null
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from((int[][]) null));
        }

        @Test
        public void testFromFromIntArray() {
            int[][] ints = { { 1, 2 }, { 3, 4 } };
            DoubleMatrix m = DoubleMatrix.from(ints);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0), DELTA);
            assertEquals(2.0, m.get(0, 1), DELTA);
            assertEquals(3.0, m.get(1, 0), DELTA);
            assertEquals(4.0, m.get(1, 1), DELTA);
        }

        @Test
        public void testFromFromIntArray_WithNull() {
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from((int[][]) null));
        }

        @Test
        public void testFromFromIntArray_WithEmpty() {
            DoubleMatrix m = DoubleMatrix.from(new int[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testFromFromIntArray_WithNullRow() {
            int[][] nullRow = { { 1, 2 }, null };
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from(nullRow));
        }

        @Test
        public void testFromFromIntArray_WithNullFirstRow() {
            int[][] nullFirstRow = { null, { 1, 2 } };
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from(nullFirstRow));
        }

        @Test
        public void testFromFromLongArray_WithNull() {
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from((long[][]) null));
        }

        // Just verify elements exist (values are random between 0.0 and 1.0)
        @Test
        public void testFromFromLongArray_WithNullRow() {
            long[][] nullRow = { { 1L, 2L }, null };
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from(nullRow));
        }

        @Test
        public void testFromFromFloatArray_WithNull() {
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from((float[][]) null));
        }

        @Test
        public void testFromFromFloatArray_WithNullRow() {
            float[][] nullRow = { { 1.5f, 2.5f }, null };
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from(nullRow));
        }

        // After 90-degree clockwise rotation:
        // Original:     Rotated:
        // 1 2           5 3 1
        // 3 4    =>     6 4 2
        // 5 6
        @Test
        public void testConversionAccuracy_FromInt() {
            // Test that large int values convert accurately to double
            int[][] ints = { { Integer.MAX_VALUE, Integer.MIN_VALUE }, { 1000000, -1000000 } };
            DoubleMatrix m = DoubleMatrix.from(ints);

            assertEquals(Integer.MAX_VALUE, m.get(0, 0), DELTA);
            assertEquals(Integer.MIN_VALUE, m.get(0, 1), DELTA);
            assertEquals(1000000.0, m.get(1, 0), DELTA);
            assertEquals(-1000000.0, m.get(1, 1), DELTA);
        }

        @Test
        public void testFromFromIntArray_Values() {
            int[][] ints = { { 1, 2 }, { 3, 4 } };
            DoubleMatrix m = DoubleMatrix.from(ints);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0));
            assertEquals(4.0, m.get(1, 1));
        }

        @Test
        public void testFromFromFloatArray_WithNullRow_NullInput_Validation() {
            float[][] nullRow = { { 1.0f, 2.0f }, null };
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from(nullRow));
        }

        @Test
        public void testFrom_FromIntArray() {
            int[][] ints = { { 1, 2 }, { 3, 4 } };
            DoubleMatrix m = DoubleMatrix.from(ints);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0), 0.0);
            assertEquals(4.0, m.get(1, 1), 0.0);
        }

        @Test
        public void testFrom_FromIntArray_DifferentRowLengths() {
            int[][] ints = { { 1, 2 }, { 3 } };
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from(ints));
        }

        @Test
        public void testFrom_FromLongArray_Empty() {
            DoubleMatrix m = DoubleMatrix.from(new long[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testFrom_FromLongArray_NullFirstRow() {
            long[][] longs = { null, { 1L, 2L } };
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from(longs));
        }

        @Test
        public void testFrom_FromFloatArray_Empty() {
            DoubleMatrix m = DoubleMatrix.from(new float[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testFrom_FromFloatArray_NullFirstRow() {
            float[][] floats = { null, { 1.0f, 2.0f } };
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.from(floats));
        }
    }

    @Nested
    class RandomRowTests {

        @Test
        public void testRandom_Values() {
            DoubleMatrix m = DoubleMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            // Just verify elements exist (values are random between 0.0 and 1.0)
            for (int i = 0; i < 5; i++) {
                double val = m.get(0, i);
                assertTrue(val >= 0.0 && val < 1.0);
            }
        }

        @Test
        public void testRandom_WithLargeLength() {
            DoubleMatrix m = DoubleMatrix.randomRow(100);
            assertEquals(1, m.rowCount());
            assertEquals(100, m.columnCount());
        }

        @Test
        public void testRandomRow_Values_SingleRowDimensionsAndValues() {
            DoubleMatrix m = DoubleMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            for (int i = 0; i < 5; i++) {
                assertNotNull(m.get(0, i));
            }
        }

        @Test
        public void testRandomRow_Values_SingleRowDimensions() {
            DoubleMatrix m = DoubleMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }

        @Test
        public void testRandom_WithZeroLength() {
            DoubleMatrix m = DoubleMatrix.randomRow(0);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testFactories_NegativeDimensions_ThrowIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.randomRow(-1));
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.random(-1, 2));
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.random(2, -1));
        }
    }

    @Nested
    class RandomTests {

        @Test
        public void testRandom_WithRowsCols() {
            DoubleMatrix m = DoubleMatrix.random(2, 3);
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    double val = m.get(i, j);
                    assertTrue(val >= 0.0 && val < 1.0);
                }
            }
        }
    }

    @Nested
    class OfMainDiagonalTests {

        @Test
        public void testOfMainDiagonal() {
            double[] diag = { 1.0, 2.0, 3.0 };
            DoubleMatrix m = DoubleMatrix.ofMainDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0, m.get(0, 0), 0.0);
            assertEquals(2.0, m.get(1, 1), 0.0);
            assertEquals(3.0, m.get(2, 2), 0.0);
            assertEquals(0.0, m.get(0, 1), 0.0);
            assertEquals(0.0, m.get(1, 0), 0.0);
        }

        @Test
        public void testDiagonalLU2RD() {
            double[] diagonal = { 1.0, 2.0, 3.0 };
            DoubleMatrix matrix = DoubleMatrix.ofMainDiagonal(diagonal);
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertEquals(1.0, matrix.get(0, 0));
            assertEquals(2.0, matrix.get(1, 1));
            assertEquals(3.0, matrix.get(2, 2));
            assertEquals(0.0, matrix.get(0, 1));
        }

        @Test
        public void testDiagonalLU2RD_Values() {
            DoubleMatrix m = DoubleMatrix.ofMainDiagonal(new double[] { 1.5, 2.5, 3.5 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.5, m.get(0, 0), DELTA);
            assertEquals(2.5, m.get(1, 1), DELTA);
            assertEquals(3.5, m.get(2, 2), DELTA);
            assertEquals(0.0, m.get(0, 1), DELTA);
            assertEquals(0.0, m.get(1, 0), DELTA);
        }

        @Test
        public void testOfMainDiagonal_Null() {
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.ofMainDiagonal(null));
        }

        @Test
        public void testOfMainDiagonal_Values_ZeroFillsOffDiagonal() {
            DoubleMatrix m = DoubleMatrix.ofMainDiagonal(new double[] { 1.0, 2.0, 3.0 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0, m.get(0, 0));
            assertEquals(2.0, m.get(1, 1));
            assertEquals(3.0, m.get(2, 2));
            assertEquals(0.0, m.get(0, 1));
            assertEquals(0.0, m.get(1, 0));
        }

        @Test
        public void testDiagonalLU2RD_WithEmptyArray() {
            DoubleMatrix m = DoubleMatrix.ofMainDiagonal(new double[0]);
            assertTrue(m.isEmpty());
        }
    }

    @Nested
    class OfAntiDiagonalTests {

        @Test
        public void testOfAntiDiagonal() {
            double[] diag = { 1.0, 2.0, 3.0 };
            DoubleMatrix m = DoubleMatrix.ofAntiDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0, m.get(0, 2), 0.0);
            assertEquals(2.0, m.get(1, 1), 0.0);
            assertEquals(3.0, m.get(2, 0), 0.0);
            assertEquals(0.0, m.get(0, 0), 0.0);
        }

        @Test
        public void testDiagonalRU2LD() {
            double[] diagonal = { 1.0, 2.0, 3.0 };
            DoubleMatrix matrix = DoubleMatrix.ofAntiDiagonal(diagonal);
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertEquals(1.0, matrix.get(0, 2));
            assertEquals(2.0, matrix.get(1, 1));
            assertEquals(3.0, matrix.get(2, 0));
        }

        @Test
        public void testDiagonalRU2LD_Values() {
            DoubleMatrix m = DoubleMatrix.ofAntiDiagonal(new double[] { 1.5, 2.5, 3.5 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.5, m.get(0, 2), DELTA);
            assertEquals(2.5, m.get(1, 1), DELTA);
            assertEquals(3.5, m.get(2, 0), DELTA);
            assertEquals(0.0, m.get(0, 0), DELTA);
            assertEquals(0.0, m.get(2, 2), DELTA);
        }

        @Test
        public void testOfAntiDiagonal_Null() {
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.ofAntiDiagonal(null));
        }

        @Test
        public void testOfAntiDiagonal_Values_ZeroFillsOffDiagonal() {
            DoubleMatrix m = DoubleMatrix.ofAntiDiagonal(new double[] { 1.0, 2.0, 3.0 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0, m.get(0, 2));
            assertEquals(2.0, m.get(1, 1));
            assertEquals(3.0, m.get(2, 0));
            assertEquals(0.0, m.get(0, 0));
            assertEquals(0.0, m.get(2, 2));
        }
    }

    @Nested
    class OfDiagonalsTests {

        @Test
        public void testOfDiagonals() {
            DoubleMatrix matrix = DoubleMatrix.ofDiagonals(new double[] { 1.0, 2.0, 3.0 }, new double[] { 4.0, 5.0, 6.0 });
            assertEquals(1.0, matrix.get(0, 0));
            assertEquals(0.0, matrix.get(0, 1));
            assertEquals(4.0, matrix.get(0, 2));
            assertEquals(0.0, matrix.get(1, 0));
            assertEquals(2.0, matrix.get(1, 1));
            assertEquals(0.0, matrix.get(1, 2));
            assertEquals(6.0, matrix.get(2, 0));
            assertEquals(0.0, matrix.get(2, 1));
            assertEquals(3.0, matrix.get(2, 2));
        }

        @Test
        public void testDiagonal() {
            double[] mainDiag = { 1.0, 2.0, 3.0 };
            double[] antiDiag = { 7.0, 8.0, 9.0 };
            DoubleMatrix matrix = DoubleMatrix.ofDiagonals(mainDiag, antiDiag);
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertEquals(1.0, matrix.get(0, 0));
            assertEquals(7.0, matrix.get(0, 2));

            // Test with different lengths
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.ofDiagonals(new double[] { 1.0 }, new double[] { 2.0, 3.0 }));
        }

        @Test
        public void testDiagonal_WithBothDiagonals() {
            DoubleMatrix m = DoubleMatrix.ofDiagonals(new double[] { 1.0, 2.0, 3.0 }, new double[] { 4.0, 5.0, 6.0 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0, m.get(0, 0), DELTA);
            assertEquals(2.0, m.get(1, 1), DELTA);
            assertEquals(3.0, m.get(2, 2), DELTA);
            assertEquals(4.0, m.get(0, 2), DELTA);
            assertEquals(6.0, m.get(2, 0), DELTA);
        }

        @Test
        public void testDiagonal_WithOnlyMainDiagonal() {
            DoubleMatrix m = DoubleMatrix.ofDiagonals(new double[] { 1.5, 2.5, 3.5 }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.5, m.get(0, 0), DELTA);
            assertEquals(2.5, m.get(1, 1), DELTA);
            assertEquals(3.5, m.get(2, 2), DELTA);
        }

        @Test
        public void testDiagonal_WithOnlyAntiDiagonal() {
            DoubleMatrix m = DoubleMatrix.ofDiagonals(null, new double[] { 4.5, 5.5, 6.5 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(4.5, m.get(0, 2), DELTA);
            assertEquals(5.5, m.get(1, 1), DELTA);
            assertEquals(6.5, m.get(2, 0), DELTA);
        }

        @Test
        public void testDiagonal_WithDifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.ofDiagonals(new double[] { 1.0, 2.0 }, new double[] { 3.0, 4.0, 5.0 }));
        }

        @Test
        public void testDiagonal_WithBothDiagonals_Values() {
            DoubleMatrix m = DoubleMatrix.ofDiagonals(new double[] { 1.0, 4.0 }, new double[] { 2.0, 3.0 });
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0));
            assertEquals(2.0, m.get(0, 1));
            assertEquals(3.0, m.get(1, 0));
            assertEquals(4.0, m.get(1, 1));
        }

        @Test
        public void testDiagonal_WithDifferentLengths_Validation() {
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.ofDiagonals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 2.0, 3.0 }));
        }

        @Test
        public void testDiagonal_Both() {
            double[] lu2rd = { 1.0, 2.0, 3.0 };
            double[] ru2ld = { 4.0, 5.0, 6.0 };
            DoubleMatrix m = DoubleMatrix.ofDiagonals(lu2rd, ru2ld);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0, m.get(0, 0), 0.0);
            assertEquals(2.0, m.get(1, 1), 0.0);
            assertEquals(3.0, m.get(2, 2), 0.0);
            assertEquals(4.0, m.get(0, 2), 0.0);
            assertEquals(6.0, m.get(2, 0), 0.0);
        }

        @Test
        public void testDiagonal_DifferentLengths() {
            double[] lu2rd = { 1.0, 2.0 };
            double[] ru2ld = { 4.0, 5.0, 6.0 };
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.ofDiagonals(lu2rd, ru2ld));
        }

        @Test
        public void testDiagonal_WithOnlyMainDiagonal_NullInput() {
            DoubleMatrix m = DoubleMatrix.ofDiagonals(new double[] { 1.0, 2.0, 3.0 }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0, m.get(0, 0));
            assertEquals(2.0, m.get(1, 1));
            assertEquals(3.0, m.get(2, 2));
        }

        @Test
        public void testDiagonal_WithOnlyAntiDiagonal_NullInput() {
            DoubleMatrix m = DoubleMatrix.ofDiagonals(null, new double[] { 1.0, 2.0, 3.0 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1.0, m.get(0, 2));
            assertEquals(2.0, m.get(1, 1));
            assertEquals(3.0, m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithBothNull() {
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.ofDiagonals(null, null));
        }

        @Test
        public void testDiagonal_SingleElement() {
            DoubleMatrix m = DoubleMatrix.ofDiagonals(new double[] { 5.0 }, new double[] { 7.0 });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(5.0, m.get(0, 0)); // Main diagonal takes precedence
        }

        @Test
        public void testOfDiagonals_EmptyAndNullSidesPreserveEmptyShape() {
            assertTrue(DoubleMatrix.ofDiagonals(new double[0], new double[0]).isEmpty());
            assertTrue(DoubleMatrix.ofDiagonals(new double[0], null).isEmpty());
            assertTrue(DoubleMatrix.ofDiagonals(null, new double[0]).isEmpty());
        }
    }

    @Nested
    class UnboxTests {

        @Test
        public void testUnbox() {
            Double[][] boxed = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            Matrix<Double> boxedMatrix = Matrix.wrap(boxed);
            DoubleMatrix unboxed = DoubleMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertEquals(1.0, unboxed.get(0, 0));
            assertEquals(4.0, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_Values() {
            Double[][] boxed = { { 1.5, 2.5 }, { 3.5, 4.5 } };
            Matrix<Double> boxedMatrix = Matrix.wrap(boxed);
            DoubleMatrix unboxed = DoubleMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertEquals(1.5, unboxed.get(0, 0), DELTA);
            assertEquals(4.5, unboxed.get(1, 1), DELTA);
        }

        @Test
        public void testUnbox_Shape2By2() {
            Matrix<Double> boxed = Matrix.wrap(new Double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m = DoubleMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0));
            assertEquals(4.0, m.get(1, 1));
        }

        @Test
        public void testUnbox_Shape2By2_DimensionsAndCornerValues() {
            Matrix<Double> boxed = Matrix.wrap(new Double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m = DoubleMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0), 0.0);
            assertEquals(4.0, m.get(1, 1), 0.0);
        }

        @Test
        public void testUnbox_WithNullValues() {
            Double[][] boxed = { { 1.5, null }, { null, 4.5 } };
            Matrix<Double> boxedMatrix = Matrix.wrap(boxed);
            DoubleMatrix unboxed = DoubleMatrix.unbox(boxedMatrix);
            assertEquals(1.5, unboxed.get(0, 0), DELTA);
            assertEquals(0.0, unboxed.get(0, 1), DELTA); // null -> 0.0
            assertEquals(0.0, unboxed.get(1, 0), DELTA); // null -> 0.0
            assertEquals(4.5, unboxed.get(1, 1), DELTA);
        }

        @Test
        public void testUnbox_WithNullValues_Shape2By2_NullInput() {
            Matrix<Double> boxed = Matrix.wrap(new Double[][] { { 1.0, null }, { null, 4.0 } });
            DoubleMatrix m = DoubleMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0));
            assertEquals(0.0, m.get(0, 1));
            assertEquals(0.0, m.get(1, 0));
            assertEquals(4.0, m.get(1, 1));
        }

        @Test
        public void testUnbox_WithNulls() {
            Matrix<Double> boxed = Matrix.wrap(new Double[][] { { 1.0, null }, { null, 4.0 } });
            DoubleMatrix m = DoubleMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.0, m.get(0, 0), 0.0);
            assertEquals(0.0, m.get(0, 1), 0.0);
            assertEquals(0.0, m.get(1, 0), 0.0);
            assertEquals(4.0, m.get(1, 1), 0.0);
        }

        @Test
        public void testUnbox_TypeNeutralEmptyReturnsPrimitiveEmptyAndTypedZeroColumnsPreserveRows() {
            DoubleMatrix sharedEmpty = DoubleMatrix.unbox(Matrix.empty(Double.class));
            DoubleMatrix copiedTypeNeutralEmpty = DoubleMatrix.unbox(Matrix.empty(Double.class).copy());

            assertSame(DoubleMatrix.empty(), sharedEmpty);
            assertSame(DoubleMatrix.empty(), copiedTypeNeutralEmpty);

            DoubleMatrix zeroColumns = DoubleMatrix.unbox(Matrix.wrap(new Double[][] { {}, {} }));
            assertEquals(2, zeroColumns.rowCount());
            assertEquals(0, zeroColumns.columnCount());

            DoubleMatrix typeNeutralZeroColumns = DoubleMatrix.unbox(Matrix.empty(Double.class).resize(2, 0));
            assertEquals(2, typeNeutralZeroColumns.rowCount());
            assertEquals(0, typeNeutralZeroColumns.columnCount());

            Matrix<Double> typeNeutralExpanded = Matrix.empty(Double.class).resize(2, 2);
            typeNeutralExpanded.set(0, 1, 7d);
            assertEquals(DoubleMatrix.wrap(new double[][] { { 0d, 7d }, { 0d, 0d } }), DoubleMatrix.unbox(typeNeutralExpanded));
        }
    }

    @Nested
    class GetTests {

        @Test
        public void testGet() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);
            assertEquals(1.0, matrix.get(0, 0));
            assertEquals(2.0, matrix.get(0, 1));
            assertEquals(3.0, matrix.get(1, 0));
            assertEquals(4.0, matrix.get(1, 1));

            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(2, 0));
        }

        @Test
        public void testGetWithPoint() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);
            assertEquals(1.0, matrix.get(Point.of(0, 0)));
            assertEquals(4.0, matrix.get(Point.of(1, 1)));
        }

        @Test
        public void testGet_Shape2By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5, 3.5 }, { 4.5, 5.5, 6.5 } });
            assertEquals(1.5, m.get(0, 0), DELTA);
            assertEquals(5.5, m.get(1, 1), DELTA);
            assertEquals(6.5, m.get(1, 2), DELTA);
        }

        @Test
        public void testGetWithPoint_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            assertEquals(1.5, m.get(Point.of(0, 0)), DELTA);
            assertEquals(4.5, m.get(Point.of(1, 1)), DELTA);
            assertEquals(2.5, m.get(Point.of(0, 1)), DELTA);
        }

        @Test
        public void testGet_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertEquals(1.0, m.get(0, 0));
            assertEquals(2.0, m.get(0, 1));
            assertEquals(3.0, m.get(1, 0));
            assertEquals(4.0, m.get(1, 1));
        }

        @Test
        public void testGet_WithPoint() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertEquals(2.0, m.get(Point.of(0, 1)));
            assertEquals(3.0, m.get(Point.of(1, 0)));
        }

        @Test
        public void testGet_ByIndices() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertEquals(1.0, m.get(0, 0), 0.0);
            assertEquals(2.0, m.get(0, 1), 0.0);
            assertEquals(3.0, m.get(1, 0), 0.0);
            assertEquals(4.0, m.get(1, 1), 0.0);
        }

        @Test
        public void testGet_ByPoint() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Point p = Point.of(0, 1);
            assertEquals(2.0, m.get(p), 0.0);
        }

        @Test
        public void testGet_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
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
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);
            matrix.set(0, 0, 5.0);
            assertEquals(5.0, matrix.get(0, 0));

            assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.set(2, 0, 5.0));
        }

        @Test
        public void testSetWithPoint() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);
            matrix.set(Point.of(0, 0), 5.0);
            assertEquals(5.0, matrix.get(Point.of(0, 0)));
        }

        @Test
        public void testSet_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.set(0, 0, 10.5);
            assertEquals(10.5, m.get(0, 0), DELTA);

            m.set(1, 1, 20.5);
            assertEquals(20.5, m.get(1, 1), DELTA);
        }

        @Test
        public void testSetWithPoint_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.set(Point.of(0, 0), 50.5);
            assertEquals(50.5, m.get(Point.of(0, 0)), DELTA);
        }

        @Test
        public void testSet_Shape2By2_ChangesFirstRowSecondColumn() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.set(0, 1, 9.0);
            assertEquals(9.0, m.get(0, 1));
        }

        @Test
        public void testSet_WithPoint() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.set(Point.of(0, 1), 9.0);
            assertEquals(9.0, m.get(0, 1));
        }

        @Test
        public void testSet_ByIndices() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.set(0, 1, 9.0);
            assertEquals(9.0, m.get(0, 1), 0.0);
        }

        @Test
        public void testSet_ByPoint() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Point p = Point.of(1, 1);
            m.set(p, 9.0);
            assertEquals(9.0, m.get(p), 0.0);
        }

        @Test
        public void testSet_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, 0.0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(2, 0, 0.0));
        }

        @Test
        public void testSet_NegativeValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.set(0, 0, -99.9f);
            assertEquals(-99.9, m.get(0, 0), 0.0001);
        }
    }

    @Nested
    class ValueAboveTests {

        @Test
        public void testValueAbove() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            assertTrue(matrix.valueAbove(0, 0).isEmpty());
            assertEquals(1.0, matrix.valueAbove(1, 0).orElse(0.0));
        }

        @Test
        public void testValueAbove_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });

            OptionalDouble up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1.5, up.get(), DELTA);

            // Top row has no element above
            OptionalDouble empty = m.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_Shape2By2_PresentNeighborValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            OptionalDouble up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1.0, up.get());
        }

        @Test
        public void testValueAbove_FirstRow() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            OptionalDouble up = m.valueAbove(0, 0);
            assertFalse(up.isPresent());
        }

        @Test
        public void testAbove() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            u.OptionalDouble value = matrix.valueAbove(1, 0);
            assertEquals(1.0, value.get());
            u.OptionalDouble empty = matrix.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_RowOneColumnZero() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            OptionalDouble up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1.0, up.get(), 0.0);
        }
    }

    @Nested
    class ValueBelowTests {

        @Test
        public void testValueBelow() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            assertEquals(3.0, matrix.valueBelow(0, 0).orElse(0.0));
            assertTrue(matrix.valueBelow(1, 0).isEmpty());
        }

        @Test
        public void testValueBelow_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });

            OptionalDouble down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3.5, down.get(), DELTA);

            // Bottom row has no element below
            OptionalDouble empty = m.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_Shape2By2_PresentNeighborValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            OptionalDouble down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3.0, down.get());
        }

        @Test
        public void testValueBelow_LastRow() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            OptionalDouble down = m.valueBelow(1, 0);
            assertFalse(down.isPresent());
        }

        @Test
        public void testBelow() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            u.OptionalDouble value = matrix.valueBelow(0, 0);
            assertEquals(3.0, value.get());
            u.OptionalDouble empty = matrix.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_RowZeroColumnZero() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            OptionalDouble down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3.0, down.get(), 0.0);
        }
    }

    @Nested
    class ValueLeftTests {

        @Test
        public void testValueLeft() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            assertTrue(matrix.valueLeft(0, 0).isEmpty());
            assertEquals(1.0, matrix.valueLeft(0, 1).orElse(0.0));
        }

        @Test
        public void testValueLeft_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });

            OptionalDouble left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1.5, left.get(), DELTA);

            // Leftmost column has no element to the left
            OptionalDouble empty = m.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_Shape2By2_PresentNeighborValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            OptionalDouble left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1.0, left.get());
        }

        @Test
        public void testValueLeft_FirstColumn() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            OptionalDouble left = m.valueLeft(0, 0);
            assertFalse(left.isPresent());
        }

        @Test
        public void testLeft() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            u.OptionalDouble value = matrix.valueLeft(0, 1);
            assertEquals(1.0, value.get());
            u.OptionalDouble empty = matrix.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_RowZeroColumnOne() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            OptionalDouble left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1.0, left.get(), 0.0);
        }
    }

    @Nested
    class ValueRightTests {

        @Test
        public void testValueRight() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            assertEquals(2.0, matrix.valueRight(0, 0).orElse(0.0));
            assertTrue(matrix.valueRight(0, 1).isEmpty());
        }

        @Test
        public void testValueRight_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });

            OptionalDouble right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2.5, right.get(), DELTA);

            // Rightmost column has no element to the right
            OptionalDouble empty = m.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_Shape2By2_PresentNeighborValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            OptionalDouble right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2.0, right.get());
        }

        @Test
        public void testValueRight_LastColumn() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            OptionalDouble right = m.valueRight(0, 1);
            assertFalse(right.isPresent());
        }

        @Test
        public void testRight() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            u.OptionalDouble value = matrix.valueRight(0, 0);
            assertEquals(2.0, value.get());
            u.OptionalDouble empty = matrix.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_RowZeroColumnZero() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            OptionalDouble right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2.0, right.get(), 0.0);
        }
    }

    @Nested
    class RowViewTests {

        @Test
        public void testRowView() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] row = matrix.rowView(0);
            assertEquals(2, row.length);
            assertEquals(1.0, row[0]);
            assertEquals(2.0, row[1]);

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(2));
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5, 3.5 }, { 4.5, 5.5, 6.5 } });
            assertArrayEquals(new double[] { 1.5, 2.5, 3.5 }, m.rowView(0), DELTA);
            assertArrayEquals(new double[] { 4.5, 5.5, 6.5 }, m.rowView(1), DELTA);
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues_FirstRowView() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            double[] row = m.rowView(0);
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, row);
        }

        @Test
        public void testRowView_Shape2By2_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double[] row = m.rowView(0);
            assertArrayEquals(new double[] { 1.0, 2.0 }, row, 0.0);
        }

        @Test
        public void testRowView_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(2));
        }

        @Test
        public void testRowView_InvalidIndex() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(5));
        }
    }

    @Nested
    class RowCopyTests {

        @Test
        public void testRowCopy() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] rowCopy = matrix.rowCopy(0);
            assertArrayEquals(new double[] { 1.0, 2.0 }, rowCopy, 0.0);

            rowCopy[0] = 99.0;
            assertArrayEquals(new double[] { 1.0, 2.0 }, matrix.rowView(0), 0.0);
        }

        @Test
        public void testRowCopy_InvalidIndex() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(2));
        }
    }

    @Nested
    class ColumnCopyTests {

        @Test
        public void testColumnCopy() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] col = matrix.columnCopy(0);
            assertEquals(2, col.length);
            assertEquals(1.0, col[0]);
            assertEquals(3.0, col[1]);

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(2));
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5, 3.5 }, { 4.5, 5.5, 6.5 } });
            assertArrayEquals(new double[] { 1.5, 4.5 }, m.columnCopy(0), DELTA);
            assertArrayEquals(new double[] { 2.5, 5.5 }, m.columnCopy(1), DELTA);
            assertArrayEquals(new double[] { 3.5, 6.5 }, m.columnCopy(2), DELTA);
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues_FirstColumnSnapshot() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            double[] col = m.columnCopy(0);
            assertArrayEquals(new double[] { 1.0, 4.0 }, col);
        }

        @Test
        public void testColumnCopy_Shape2By2_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double[] col = m.columnCopy(0);
            assertArrayEquals(new double[] { 1.0, 3.0 }, col, 0.0);
        }

        @Test
        public void testColumnCopy_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(5));
        }
    }

    @Nested
    class SetRowTests {

        @Test
        public void testSetRow() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.setRow(0, new double[] { 5.0, 6.0 });
            assertEquals(5.0, matrix.get(0, 0));
            assertEquals(6.0, matrix.get(0, 1));

            assertThrows(IllegalArgumentException.class, () -> matrix.setRow(0, new double[] { 1.0 }));
            assertThrows(IllegalArgumentException.class, () -> matrix.setRow(0, (double[]) null));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.setRow(0, new double[] { 10.5, 20.5 });
            assertArrayEquals(new double[] { 10.5, 20.5 }, m.rowView(0), DELTA);
            assertArrayEquals(new double[] { 3.0, 4.0 }, m.rowView(1), DELTA); // unchanged
        }

        @Test
        public void testSetRow_WrongSize() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new double[] { 1.0 }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new double[] { 1.0, 2.0, 3.0 }));
        }

        @Test
        public void testSetRow_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.setRow(0, new double[] { 7.0, 8.0 });
            assertEquals(7.0, m.get(0, 0));
            assertEquals(8.0, m.get(0, 1));
        }

        @Test
        public void testSetRow_WrongLength() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new double[] { 7.0 }));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues_ReplacesFirstRow() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.setRow(0, new double[] { 9.0, 8.0 });
            assertArrayEquals(new double[] { 9.0, 8.0 }, m.rowView(0), 0.0);
        }

        @Test
        public void testSetRow_InvalidRowIndex() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setRow(5, new double[] { 1.0, 2.0 }));
        }

        @Test
        public void testSetRow_InvalidLength() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new double[] { 1.0 }));
        }
    }

    @Nested
    class SetColumnTests {

        @Test
        public void testSetColumn() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.setColumn(0, new double[] { 5.0, 6.0 });
            assertEquals(5.0, matrix.get(0, 0));
            assertEquals(6.0, matrix.get(1, 0));

            assertThrows(IllegalArgumentException.class, () -> matrix.setColumn(0, new double[] { 1.0 }));
            assertThrows(IllegalArgumentException.class, () -> matrix.setColumn(0, (double[]) null));
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.setColumn(0, new double[] { 10.5, 20.5 });
            assertArrayEquals(new double[] { 10.5, 20.5 }, m.columnCopy(0), DELTA);
            assertArrayEquals(new double[] { 2.0, 4.0 }, m.columnCopy(1), DELTA); // unchanged
        }

        @Test
        public void testSetColumn_WrongSize() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new double[] { 1.0 }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new double[] { 1.0, 2.0, 3.0 }));
        }

        @Test
        public void testSetColumn_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.setColumn(0, new double[] { 7.0, 8.0 });
            assertEquals(7.0, m.get(0, 0));
            assertEquals(8.0, m.get(1, 0));
        }

        @Test
        public void testSetColumn_WrongLength() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new double[] { 7.0 }));
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues_ReplacesFirstColumn() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.setColumn(0, new double[] { 9.0, 8.0 });
            assertArrayEquals(new double[] { 9.0, 8.0 }, m.columnCopy(0), 0.0);
        }

        @Test
        public void testAliasedArraySourcesAreSnapshotted() {
            DoubleMatrix columnMatrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            columnMatrix.setColumn(1, columnMatrix.rowView(0));
            assertArrayEquals(new double[] { 3.0, 2.0 }, columnMatrix.rowCopy(1));

            DoubleMatrix diagonalMatrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            diagonalMatrix.setAntiDiagonal(diagonalMatrix.rowView(0));
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, diagonalMatrix.antiDiagonalCopy());

            double[][] backing = { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } };
            DoubleMatrix fillMatrix = DoubleMatrix.wrap(backing);
            fillMatrix.copyFrom(1, 0, backing);
            assertArrayEquals(new double[] { 3.0, 4.0 }, fillMatrix.rowCopy(2));
        }

        @Test
        public void testSetColumn_InvalidColumnIndex() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(5, new double[] { 1.0 }));
        }

        @Test
        public void testSetColumn_InvalidLength() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new double[] { 1.0 }));
        }
    }

    @Nested
    class UpdateRowTests {

        @Test
        public void testUpdateRow() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.updateRow(0, d -> d * 2);
            assertEquals(2.0, matrix.get(0, 0));
            assertEquals(4.0, matrix.get(0, 1));
        }

        @Test
        public void testUpdateRow_Shape2By2_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateRow(0, x -> x * 2.0);
            assertArrayEquals(new double[] { 2.0, 4.0 }, m.rowView(0), DELTA);
            assertArrayEquals(new double[] { 3.0, 4.0 }, m.rowView(1), DELTA); // unchanged
        }

        @Test
        public void testUpdateRow_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateRow(0, x -> x * 2);
            assertEquals(2.0, m.get(0, 0));
            assertEquals(4.0, m.get(0, 1));
        }

        @Test
        public void testUpdateRow_Shape2By2_ArrayValues_PreservesOtherRow() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateRow(0, x -> x * 2);
            assertArrayEquals(new double[] { 2.0, 4.0 }, m.rowView(0), 0.0);
            assertArrayEquals(new double[] { 3.0, 4.0 }, m.rowView(1), 0.0);
        }

        @Test
        public void testUpdateRow_DoublesFirstRowOnly() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            matrix.updateRow(0, x -> x * 2);
            assertArrayEquals(new double[] { 2.0, 4.0, 6.0 }, matrix.rowView(0));
            assertArrayEquals(new double[] { 4.0, 5.0, 6.0 }, matrix.rowView(1));
        }

        @Test
        public void testInPlaceCallbacksRetainCompletedUpdatesWhenTheyThrow() {
            final IllegalStateException failure = new IllegalStateException("stop at the second element");
            final Throwables.DoubleUnaryOperator<RuntimeException> operator = value -> {
                if (value == 2) {
                    throw failure;
                }
                return value * 10;
            };
            final List<Throwables.Consumer<DoubleMatrix, RuntimeException>> updates = List.of(m -> m.updateRow(0, operator), m -> m.updateColumn(0, operator),
                    m -> m.updateMainDiagonal(operator), m -> m.updateAntiDiagonal(operator), m -> m.updateAll(operator),
                    m -> m.updateAll((i, j) -> operator.applyAsDouble(m.get(i, j))), m -> m.replaceIf(value -> operator.applyAsDouble(value) > 0, 10),
                    m -> m.replaceIf((i, j) -> operator.applyAsDouble(m.get(i, j)) > 0, 10));

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                for (int operation = 0; operation < updates.size(); operation++) {
                    final DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1, 2, 1 }, { 2, 2, 2 }, { 1, 2, 1 } });
                    final Throwables.Consumer<DoubleMatrix, RuntimeException> update = updates.get(operation);
                    assertSame(failure, assertThrows(IllegalStateException.class, () -> update.accept(m)));
                    final double[][] expected = { { 1, 2, 1 }, { 2, 2, 2 }, { 1, 2, 1 } };
                    expected[0][operation == 3 ? 2 : 0] = 10;
                    assertEquals(DoubleMatrix.wrap(expected), m, "operation " + operation);
                }
            });
        }

        @Test
        public void testUpdateRowAndUpdateColumnNullOperator() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            assertThrows(IllegalArgumentException.class, () -> matrix.updateRow(0, (Throwables.DoubleUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateColumn(0, (Throwables.DoubleUnaryOperator<RuntimeException>) null));
        }

        @Test
        public void testUpdateRowAndUpdateColumnInvalidIndex() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateRow(-1, d -> d * 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateColumn(2, d -> d * 2));
        }
    }

    @Nested
    class UpdateColumnTests {

        @Test
        public void testUpdateColumn() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.updateColumn(0, d -> d * 2);
            assertEquals(2.0, matrix.get(0, 0));
            assertEquals(6.0, matrix.get(1, 0));
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateColumn(0, x -> x + 10.5);
            assertArrayEquals(new double[] { 11.5, 13.5 }, m.columnCopy(0), DELTA);
            assertArrayEquals(new double[] { 2.0, 4.0 }, m.columnCopy(1), DELTA); // unchanged
        }

        @Test
        public void testUpdateColumn_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateColumn(0, x -> x + 10);
            assertEquals(11.0, m.get(0, 0));
            assertEquals(13.0, m.get(1, 0));
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues_PreservesOtherColumn() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateColumn(0, x -> x * 2);
            assertArrayEquals(new double[] { 2.0, 6.0 }, m.columnCopy(0), 0.0);
            assertArrayEquals(new double[] { 2.0, 4.0 }, m.columnCopy(1), 0.0);
        }

        @Test
        public void testUpdateColumn_AddsTenToFirstColumn() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            matrix.updateColumn(0, x -> x + 10.0);
            assertEquals(11.0, matrix.get(0, 0));
            assertEquals(2.0, matrix.get(0, 1));
            assertEquals(13.0, matrix.get(1, 0));
            assertEquals(15.0, matrix.get(2, 0));
        }

        @Test
        public void testDuplicateBackingRowsAreRejectedAtConstruction() {
            // One array cannot back two independently addressable logical rows, so wrap() rejects it.
            final double[] sharedRow = { 1.0, 2.0, 3.0 };
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.wrap(new double[][] { sharedRow, sharedRow }));
            assertTrue(ex.getMessage().contains("independent storage"));

            // copyOf() clones each row, so the same values are accepted and stay independent.
            final DoubleMatrix matrix = DoubleMatrix.copyOf(new double[][] { sharedRow, sharedRow });
            matrix.updateColumn(0, value -> value + 1.0);
            assertArrayEquals(new double[] { 2.0, 2.0, 3.0 }, matrix.rowCopy(0));
            assertArrayEquals(new double[] { 2.0, 2.0, 3.0 }, matrix.rowCopy(1));
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, sharedRow);
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

        // Test non-square matrix
        @Test
        public void testGetLU2RD() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] diag = matrix.mainDiagonalCopy();
            assertEquals(2, diag.length);
            assertEquals(1.0, diag[0]);
            assertEquals(4.0, diag[1]);

            // Test non-square matrix
            DoubleMatrix nonSquare = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } });
            assertArrayEquals(new double[] { 1.0 }, nonSquare.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_Shape3By3_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            assertArrayEquals(new double[] { 1.0, 5.0, 9.0 }, m.mainDiagonalCopy(), DELTA);
        }

        @Test
        public void testGetLU2RD_nonSquare() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertArrayEquals(new double[] { 1.0 }, m.mainDiagonalCopy(), DELTA);
        }

        @Test
        public void testMainDiagonalCopy_Shape3By3_ArrayValues_DiagonalArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            double[] diagonal = m.mainDiagonalCopy();
            assertArrayEquals(new double[] { 1.0, 5.0, 9.0 }, diagonal);
        }

        @Test
        public void testMainDiagonalCopy() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            double[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new double[] { 1.0, 5.0, 9.0 }, diag, 0.0);
        }

        @Test
        public void testMainDiagonalCopy_NonSquare() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            assertArrayEquals(new double[] { 1.0, 4.0 }, m.mainDiagonalCopy(), 0.0);
        }
    }

    @Nested
    class SetMainDiagonalTests {

        @Test
        public void testSetLU2RD() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.setMainDiagonal(new double[] { 5.0, 6.0 });
            assertEquals(5.0, matrix.get(0, 0));
            assertEquals(6.0, matrix.get(1, 1));

            assertThrows(IllegalArgumentException.class, () -> matrix.setMainDiagonal(new double[] { 1.0 }));
        }

        @Test
        public void testSetMainDiagonal_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            m.setMainDiagonal(new double[] { 10.5, 20.5, 30.5 });
            assertEquals(10.5, m.get(0, 0), DELTA);
            assertEquals(20.5, m.get(1, 1), DELTA);
            assertEquals(30.5, m.get(2, 2), DELTA);
        }

        @Test
        public void testSetLU2RD_nonSquare() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            Assertions.assertDoesNotThrow(() -> m.setMainDiagonal(new double[] { 1.0 }));
        }

        @Test
        public void testSetMainDiagonal_ArrayTooShort() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new double[] { 1.0, 2.0 }));
        }

        @Test
        public void testSetMainDiagonal_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.setMainDiagonal(new double[] { 9.0, 8.0 });
            assertEquals(9.0, m.get(0, 0));
            assertEquals(8.0, m.get(1, 1));
        }

        @Test
        public void testSetMainDiagonal_NonSquare_Shape3By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            Assertions.assertDoesNotThrow(() -> m.setMainDiagonal(new double[] { 9.0, 8.0 }));
        }

        @Test
        public void testSetMainDiagonal() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.setMainDiagonal(new double[] { 9.0, 8.0 });
            assertArrayEquals(new double[] { 9.0, 8.0 }, m.mainDiagonalCopy(), 0.0);
        }

        @Test
        public void testSetMainDiagonal_NonSquare() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            Assertions.assertDoesNotThrow(() -> m.setMainDiagonal(new double[] { 9.0 }));
        }

        @Test
        public void testSetDiagonal_NullArraysOnEmptyMatrixThrow() {
            DoubleMatrix m = DoubleMatrix.empty();
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(null));
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(null));
        }

        @Test
        public void testSetMainDiagonal_InvalidLength() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new double[] { 9.0 }));
        }
    }

    @Nested
    class UpdateMainDiagonalTests {

        @Test
        public void testUpdateLU2RD() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.updateMainDiagonal(d -> d * 2);
            assertEquals(2.0, matrix.get(0, 0));
            assertEquals(8.0, matrix.get(1, 1));
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            m.updateMainDiagonal(x -> x * 10.0);
            assertEquals(10.0, m.get(0, 0), DELTA);
            assertEquals(50.0, m.get(1, 1), DELTA);
            assertEquals(90.0, m.get(2, 2), DELTA);
            assertEquals(2.0, m.get(0, 1), DELTA); // unchanged
        }

        @Test
        public void testUpdateMainDiagonal_NonSquare() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            Assertions.assertDoesNotThrow(() -> m.updateMainDiagonal(x -> x * 2.0));
        }

        @Test
        public void testUpdateMainDiagonal_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateMainDiagonal(x -> x * 2);
            assertEquals(2.0, m.get(0, 0));
            assertEquals(8.0, m.get(1, 1));
        }

        @Test
        public void testUpdateMainDiagonal() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateMainDiagonal(x -> x * 2);
            assertArrayEquals(new double[] { 2.0, 8.0 }, m.mainDiagonalCopy(), 0.0);
        }
    }

    @Nested
    class AntiDiagonalCopyTests {

        @Test
        public void testGetRU2LD() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] diag = matrix.antiDiagonalCopy();
            assertEquals(2, diag.length);
            assertEquals(2.0, diag[0]);
            assertEquals(3.0, diag[1]);
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            assertArrayEquals(new double[] { 3.0, 5.0, 7.0 }, m.antiDiagonalCopy(), DELTA);
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertArrayEquals(new double[] { 2.0 }, m.antiDiagonalCopy(), DELTA);
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues_AntiDiagonalArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            double[] diagonal = m.antiDiagonalCopy();
            assertArrayEquals(new double[] { 3.0, 5.0, 7.0 }, diagonal);
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare_Shape3By2_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            assertArrayEquals(new double[] { 2.0, 3.0 }, m.antiDiagonalCopy(), DELTA);
        }

        @Test
        public void testAntiDiagonalCopy() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            double[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new double[] { 3.0, 5.0, 7.0 }, diag, 0.0);
        }
    }

    @Nested
    class SetAntiDiagonalTests {

        @Test
        public void testSetRU2LD() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.setAntiDiagonal(new double[] { 5.0, 6.0 });
            assertEquals(5.0, matrix.get(0, 1));
            assertEquals(6.0, matrix.get(1, 0));

            assertThrows(IllegalArgumentException.class, () -> matrix.setAntiDiagonal(new double[] { 1.0 }));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            m.setAntiDiagonal(new double[] { 10.5, 20.5, 30.5 });
            assertEquals(10.5, m.get(0, 2), DELTA);
            assertEquals(20.5, m.get(1, 1), DELTA);
            assertEquals(30.5, m.get(2, 0), DELTA);
        }

        @Test
        public void testSetAntiDiagonal_NonSquare() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            Assertions.assertDoesNotThrow(() -> m.setAntiDiagonal(new double[] { 1.0 }));
        }

        @Test
        public void testSetAntiDiagonal_ArrayTooShort() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new double[] { 1.0, 2.0 }));
        }

        @Test
        public void testSetAntiDiagonal_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.setAntiDiagonal(new double[] { 9.0, 8.0 });
            assertEquals(9.0, m.get(0, 1));
            assertEquals(8.0, m.get(1, 0));
        }

        @Test
        public void testSetAntiDiagonal() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.setAntiDiagonal(new double[] { 9.0, 8.0 });
            assertArrayEquals(new double[] { 9.0, 8.0 }, m.antiDiagonalCopy(), 0.0);
        }
    }

    @Nested
    class UpdateAntiDiagonalTests {

        @Test
        public void testUpdateRU2LD() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.updateAntiDiagonal(d -> d * 2);
            assertEquals(4.0, matrix.get(0, 1));
            assertEquals(6.0, matrix.get(1, 0));
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            m.updateAntiDiagonal(x -> x * 10.0);
            assertEquals(30.0, m.get(0, 2), DELTA);
            assertEquals(50.0, m.get(1, 1), DELTA);
            assertEquals(70.0, m.get(2, 0), DELTA);
            assertEquals(2.0, m.get(0, 1), DELTA); // unchanged
        }

        @Test
        public void testUpdateAntiDiagonal_NonSquare() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            Assertions.assertDoesNotThrow(() -> m.updateAntiDiagonal(x -> x * 2.0));
        }

        @Test
        public void testUpdateAntiDiagonal_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateAntiDiagonal(x -> x + 10);
            assertEquals(12.0, m.get(0, 1));
            assertEquals(13.0, m.get(1, 0));
        }

        @Test
        public void testUpdateAntiDiagonal() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateAntiDiagonal(x -> x * 2);
            double[] diag = m.antiDiagonalCopy();
            assertEquals(4.0, diag[0], 0.0);
            assertEquals(6.0, diag[1], 0.0);
        }
    }

    @Nested
    class UpdateAllTests {

        @Test
        public void testUpdateAll() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.updateAll(d -> d * 2);
            assertEquals(2.0, matrix.get(0, 0));
            assertEquals(4.0, matrix.get(0, 1));
            assertEquals(6.0, matrix.get(1, 0));
            assertEquals(8.0, matrix.get(1, 1));
        }

        @Test
        public void testUpdateAllUnarySequentialTallMatrixUsesRowMajorOrder() throws Exception {
            final DoubleMatrix tall = DoubleMatrix.wrap(new double[3][2]);
            final AtomicInteger next = new AtomicInteger();

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> tall.updateAll(x -> (double) next.incrementAndGet()));

            assertArrayEquals(new double[] { 1.0, 2.0 }, tall.rowCopy(0), 0.0);
            assertArrayEquals(new double[] { 3.0, 4.0 }, tall.rowCopy(1), 0.0);
            assertArrayEquals(new double[] { 5.0, 6.0 }, tall.rowCopy(2), 0.0);
        }

        @Test
        public void testUpdateAllWithIndices() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.updateAll((i, j) -> (double) (i * 10 + j));
            assertEquals(0.0, matrix.get(0, 0));
            assertEquals(1.0, matrix.get(0, 1));
            assertEquals(10.0, matrix.get(1, 0));
            assertEquals(11.0, matrix.get(1, 1));
        }

        @Test
        public void testUpdateAll_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateAll(x -> x * 2.0);
            assertEquals(2.0, m.get(0, 0), DELTA);
            assertEquals(4.0, m.get(0, 1), DELTA);
            assertEquals(6.0, m.get(1, 0), DELTA);
            assertEquals(8.0, m.get(1, 1), DELTA);
        }

        @Test
        public void testUpdateAll_WithIndices() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 0.0, 0.0 }, { 0.0, 0.0 } });
            m.updateAll((i, j) -> i * 10.0 + j);
            assertEquals(0.0, m.get(0, 0), DELTA);
            assertEquals(1.0, m.get(0, 1), DELTA);
            assertEquals(10.0, m.get(1, 0), DELTA);
            assertEquals(11.0, m.get(1, 1), DELTA);
        }

        @Test
        public void testUpdateAll_UnaryOperator() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateAll(x -> x * 2);
            assertEquals(2.0, m.get(0, 0));
            assertEquals(4.0, m.get(0, 1));
            assertEquals(6.0, m.get(1, 0));
            assertEquals(8.0, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateAll((i, j) -> (double) (i + j));
            assertEquals(0.0, m.get(0, 0));
            assertEquals(1.0, m.get(0, 1));
            assertEquals(1.0, m.get(1, 0));
            assertEquals(2.0, m.get(1, 1));
        }

        // fill method clips data to fit within matrix bounds, doesn't throw exception
        @Test
        public void testUpdateAll_UnaryOperator_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateAll(x -> x * 2);
            assertEquals(2.0, m.get(0, 0), 0.0);
            assertEquals(4.0, m.get(0, 1), 0.0);
            assertEquals(6.0, m.get(1, 0), 0.0);
            assertEquals(8.0, m.get(1, 1), 0.0);
        }

        @Test
        public void testUpdateAll_BiFunction_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.updateAll((i, j) -> (i + 1) * 10.0 + (j + 1));
            assertEquals(11.0, m.get(0, 0), 0.0);
            assertEquals(12.0, m.get(0, 1), 0.0);
            assertEquals(21.0, m.get(1, 0), 0.0);
            assertEquals(22.0, m.get(1, 1), 0.0);
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testUpdateAll_ExplicitModesTransformEveryValue(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d } });
                matrix.updateAll(value -> value + 10);
                assertEquals(DoubleMatrix.wrap(new double[][] { { 11.0d, 12.0d, 13.0d }, { 14.0d, 15.0d, 16.0d } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testDoubleMatrixUpdateAllNullOperator() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1d, 2d }, { 3d, 4d } });
            DoubleMatrix emptyLike = DoubleMatrix.wrap(new double[][] { {}, {} });
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.DoubleUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.IntBiFunction<Double, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateMainDiagonal((Throwables.DoubleUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAntiDiagonal((Throwables.DoubleUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.DoublePredicate<RuntimeException>) null, 0d));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0d));

            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateMainDiagonal((Throwables.DoubleUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAntiDiagonal((Throwables.DoubleUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.DoublePredicate<RuntimeException>) null, 0d));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0d));
        }

        @Test
        public void testUpdateAll_ZeroColumnsDoNotInvokeOperator() {
            final var matrix = DoubleMatrix.wrap(new double[2][0]);
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
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.replaceIf(d -> d < 3.0, 0.0);
            assertEquals(0.0, matrix.get(0, 0));
            assertEquals(0.0, matrix.get(0, 1));
            assertEquals(3.0, matrix.get(1, 0));
            assertEquals(4.0, matrix.get(1, 1));
        }

        @Test
        public void testReplaceIfWithIndices() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.replaceIf((i, j) -> i == j, 0.0);
            assertEquals(0.0, matrix.get(0, 0));
            assertEquals(2.0, matrix.get(0, 1));
            assertEquals(3.0, matrix.get(1, 0));
            assertEquals(0.0, matrix.get(1, 1));
        }

        @Test
        public void testReplaceIf_Shape2By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            m.replaceIf(x -> x > 3.0, 0.0);
            assertEquals(1.0, m.get(0, 0), DELTA);
            assertEquals(2.0, m.get(0, 1), DELTA);
            assertEquals(3.0, m.get(0, 2), DELTA);
            assertEquals(0.0, m.get(1, 0), DELTA); // was 4.0
            assertEquals(0.0, m.get(1, 1), DELTA); // was 5.0
            assertEquals(0.0, m.get(1, 2), DELTA); // was 6.0
        }

        @Test
        public void testReplaceIf_WithIndices() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            m.replaceIf((i, j) -> i == j, 0.0); // Replace diagonal
            assertEquals(0.0, m.get(0, 0), DELTA);
            assertEquals(0.0, m.get(1, 1), DELTA);
            assertEquals(0.0, m.get(2, 2), DELTA);
            assertEquals(2.0, m.get(0, 1), DELTA); // unchanged
        }

        @Test
        public void testReplaceIf_Predicate() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.replaceIf(x -> x > 2, 0.0);
            assertEquals(1.0, m.get(0, 0));
            assertEquals(2.0, m.get(0, 1));
            assertEquals(0.0, m.get(1, 0));
            assertEquals(0.0, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_BiPredicate() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.replaceIf((i, j) -> i == j, 0.0);
            assertEquals(0.0, m.get(0, 0));
            assertEquals(2.0, m.get(0, 1));
            assertEquals(3.0, m.get(1, 0));
            assertEquals(0.0, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_Predicate_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.replaceIf(x -> x > 2, 99.0);
            assertEquals(1.0, m.get(0, 0), 0.0);
            assertEquals(2.0, m.get(0, 1), 0.0);
            assertEquals(99.0, m.get(1, 0), 0.0);
            assertEquals(99.0, m.get(1, 1), 0.0);
        }

        @Test
        public void testReplaceIf_BiPredicate_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.replaceIf((i, j) -> i == j, 99.0);
            assertEquals(99.0, m.get(0, 0), 0.0);
            assertEquals(2.0, m.get(0, 1), 0.0);
            assertEquals(3.0, m.get(1, 0), 0.0);
            assertEquals(99.0, m.get(1, 1), 0.0);
        }

        @Test
        public void testReplaceIfByIndex() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            matrix.replaceIf((i, j) -> i == j, 0.0);
            assertEquals(0.0, matrix.get(0, 0));
            assertEquals(2.0, matrix.get(0, 1));
            assertEquals(0.0, matrix.get(1, 1));
            assertEquals(0.0, matrix.get(2, 2));

            matrix.replaceIf((i, j) -> i == 0 || j == 0, -1.0);
            assertEquals(-1.0, matrix.get(0, 0));
            assertEquals(-1.0, matrix.get(0, 1));
            assertEquals(-1.0, matrix.get(0, 2));
            assertEquals(-1.0, matrix.get(1, 0));
            assertEquals(0.0, matrix.get(1, 1));
            assertEquals(6.0, matrix.get(1, 2));
            assertEquals(-1.0, matrix.get(2, 0));
            assertEquals(8.0, matrix.get(2, 1));
            assertEquals(0.0, matrix.get(2, 2));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testReplaceIf_ExplicitModesReplaceOnlyMatchingValues(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d } });
                matrix.replaceIf(value -> value % 2 == 0, 9.0d);
                assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 9.0d, 3.0d }, { 9.0d, 5.0d, 9.0d } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testReplaceIf_ZerosNegativeCells() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { -1.0, 2.0, -3.0 }, { 4.0, -5.0, 6.0 } });
            matrix.replaceIf(x -> x < 0, 0.0);
            assertEquals(0.0, matrix.get(0, 0));
            assertEquals(2.0, matrix.get(0, 1));
            assertEquals(0.0, matrix.get(0, 2));
            assertEquals(4.0, matrix.get(1, 0));
            assertEquals(0.0, matrix.get(1, 1));
            assertEquals(6.0, matrix.get(1, 2));
        }
    }

    @Nested
    class MapTests {

        @Test
        public void testMap() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix mapped = matrix.map(d -> d * 2);
            assertEquals(2.0, mapped.get(0, 0));
            assertEquals(4.0, mapped.get(0, 1));
            assertEquals(6.0, mapped.get(1, 0));
            assertEquals(8.0, mapped.get(1, 1));
        }

        // Test scalar division
        @Test
        public void testScalarOperationsWithMap() {
            // Test scalar addition using map
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });

            DoubleMatrix addScalar = m.map(x -> x + 10.5);
            assertEquals(11.5, addScalar.get(0, 0), 0.0001);
            assertEquals(12.5, addScalar.get(0, 1), 0.0001);
            assertEquals(13.5, addScalar.get(1, 0), 0.0001);
            assertEquals(14.5, addScalar.get(1, 1), 0.0001);

            // Test scalar subtraction
            DoubleMatrix subtractScalar = m.map(x -> x - 0.5);
            assertEquals(0.5, subtractScalar.get(0, 0), 0.0001);
            assertEquals(1.5, subtractScalar.get(0, 1), 0.0001);
            assertEquals(2.5, subtractScalar.get(1, 0), 0.0001);
            assertEquals(3.5, subtractScalar.get(1, 1), 0.0001);

            // Test scalar multiplication
            DoubleMatrix multiplyScalar = m.map(x -> x * 2.5);
            assertEquals(2.5, multiplyScalar.get(0, 0), 0.0001);
            assertEquals(5.0, multiplyScalar.get(0, 1), 0.0001);
            assertEquals(7.5, multiplyScalar.get(1, 0), 0.0001);
            assertEquals(10.0, multiplyScalar.get(1, 1), 0.0001);

            // Test scalar division
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 10.0, 20.0 }, { 30.0, 40.0 } });
            DoubleMatrix divideScalar = m2.map(x -> x / 10.0);
            assertEquals(1.0, divideScalar.get(0, 0), 0.0001);
            assertEquals(2.0, divideScalar.get(0, 1), 0.0001);
            assertEquals(3.0, divideScalar.get(1, 0), 0.0001);
            assertEquals(4.0, divideScalar.get(1, 1), 0.0001);
        }

        @Test
        public void testSpecialFloatingPointValues() {
            // Test with NaN values
            DoubleMatrix nanMatrix = DoubleMatrix.wrap(new double[][] { { Double.NaN, 1.0 }, { 2.0, Double.NaN } });

            // NaN should remain NaN in operations
            DoubleMatrix nanPlusOne = nanMatrix.map(x -> x + 1.0);
            assertTrue(Double.isNaN(nanPlusOne.get(0, 0)));
            assertEquals(2.0, nanPlusOne.get(0, 1), 0.0001);
            assertEquals(3.0, nanPlusOne.get(1, 0), 0.0001);
            assertTrue(Double.isNaN(nanPlusOne.get(1, 1)));

            // Test with infinity values
            DoubleMatrix infMatrix = DoubleMatrix.wrap(new double[][] { { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }, { 1.0, 2.0 } });

            DoubleMatrix infPlusOne = infMatrix.map(x -> x + 1.0);
            assertEquals(Double.POSITIVE_INFINITY, infPlusOne.get(0, 0), 0.0);
            assertEquals(Double.NEGATIVE_INFINITY, infPlusOne.get(0, 1), 0.0);
            assertEquals(2.0, infPlusOne.get(1, 0), 0.0001);
            assertEquals(3.0, infPlusOne.get(1, 1), 0.0001);

            // Test infinity in arithmetic operations
            DoubleMatrix finite = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix inf = DoubleMatrix.wrap(new double[][] { { Double.POSITIVE_INFINITY, 3.0 } });

            DoubleMatrix infSum = finite.add(inf);
            assertEquals(Double.POSITIVE_INFINITY, infSum.get(0, 0), 0.0);
            assertEquals(5.0, infSum.get(0, 1), 0.0001);
        }

        @Test
        public void testMathematicalConstants() {
            // Test operations with mathematical constants
            DoubleMatrix constants = DoubleMatrix.wrap(new double[][] { { Math.PI, Math.E }, { Math.sqrt(2), Math.log(10) } });

            // Test trigonometric operations
            DoubleMatrix sinConstants = constants.map(Math::sin);
            assertEquals(Math.sin(Math.PI), sinConstants.get(0, 0), 0.0001);
            assertEquals(Math.sin(Math.E), sinConstants.get(0, 1), 0.0001);

            // Test logarithmic operations
            DoubleMatrix logConstants = constants.map(Math::log);
            assertEquals(Math.log(Math.PI), logConstants.get(0, 0), 0.0001);
            assertEquals(1.0, logConstants.get(0, 1), 0.0001); // log(e) = 1

            // Test power operations
            DoubleMatrix squared = constants.map(x -> x * x);
            assertEquals(Math.PI * Math.PI, squared.get(0, 0), 0.0001);
            assertEquals(Math.E * Math.E, squared.get(0, 1), 0.0001);
        }

        @Test
        public void testMap_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix result = m.map(x -> x * 2.0);
            assertEquals(2.0, result.get(0, 0), DELTA);
            assertEquals(4.0, result.get(0, 1), DELTA);
            assertEquals(6.0, result.get(1, 0), DELTA);
            assertEquals(8.0, result.get(1, 1), DELTA);

            // Original unchanged
            assertEquals(1.0, m.get(0, 0), DELTA);
        }

        @Test
        public void testScalarOperationsWithMap_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });

            // Scalar addition
            DoubleMatrix addScalar = m.map(x -> x + 10.0);
            assertEquals(11.0, addScalar.get(0, 0), DELTA);
            assertEquals(14.0, addScalar.get(1, 1), DELTA);

            // Scalar multiplication
            DoubleMatrix multiplyScalar = m.map(x -> x * 3.0);
            assertEquals(3.0, multiplyScalar.get(0, 0), DELTA);
            assertEquals(12.0, multiplyScalar.get(1, 1), DELTA);

            // Scalar division
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 10.0, 20.0 }, { 30.0, 40.0 } });
            DoubleMatrix divideScalar = m2.map(x -> x / 10.0);
            assertEquals(1.0, divideScalar.get(0, 0), DELTA);
            assertEquals(4.0, divideScalar.get(1, 1), DELTA);
        }

        @Test
        public void testSpecialDoubleValues_VerySmallAndLarge() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { Double.MAX_VALUE, Double.MIN_VALUE }, { Double.MIN_NORMAL, 1e-308 } });

            assertEquals(Double.MAX_VALUE, m.get(0, 0), DELTA);
            assertEquals(Double.MIN_VALUE, m.get(0, 1), DELTA);
            assertEquals(Double.MIN_NORMAL, m.get(1, 0), DELTA);

            // Test operations with very large values
            DoubleMatrix doubled = m.map(x -> x * 2.0);
            assertEquals(Double.POSITIVE_INFINITY, doubled.get(0, 0), DELTA); // Overflow to infinity
        }

        @Test
        public void testMap_Shape2By2_ScalesAllCellsAndPreservesSource() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix result = m.map(x -> x * 2);
            assertEquals(2.0, result.get(0, 0));
            assertEquals(4.0, result.get(0, 1));
            assertEquals(6.0, result.get(1, 0));
            assertEquals(8.0, result.get(1, 1));
            assertEquals(1.0, m.get(0, 0));
        }

        @Test
        public void testMap_DoublesValuesAndPreservesSource() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix result = m.map(x -> x * 2);
            assertEquals(2.0, result.get(0, 0), 0.0);
            assertEquals(4.0, result.get(0, 1), 0.0);
            assertEquals(6.0, result.get(1, 0), 0.0);
            assertEquals(8.0, result.get(1, 1), 0.0);
            // Original should be unchanged
            assertEquals(1.0, m.get(0, 0), 0.0);
        }

        @Test
        public void testMapNullMapper() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            assertThrows(IllegalArgumentException.class, () -> matrix.map((Throwables.DoubleUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.mapToObj((Throwables.DoubleFunction<String, RuntimeException>) null, String.class));
        }

        // Test that large int values convert accurately to double
        @Test
        public void testSpecialDoubleValues_NaN() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { Double.NaN, 2.0 }, { 3.0, Double.NaN } });

            assertTrue(Double.isNaN(m.get(0, 0)));
            assertEquals(2.0, m.get(0, 1), DELTA);
            assertTrue(Double.isNaN(m.get(1, 1)));

            // Test operations with NaN
            DoubleMatrix doubled = m.map(x -> x * 2.0);
            assertTrue(Double.isNaN(doubled.get(0, 0))); // NaN * 2 = NaN
            assertEquals(4.0, doubled.get(0, 1), DELTA);
        }

        @Test
        public void testNaNHandling() {
            // Test operations with NaN values
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, Double.NaN }, { Double.NaN, 4.0 } });

            // Test get
            assertEquals(1.0, m.get(0, 0), DELTA);
            assertTrue(Double.isNaN(m.get(0, 1)));
            assertTrue(Double.isNaN(m.get(1, 0)));
            assertEquals(4.0, m.get(1, 1), DELTA);

            // Test map with NaN
            DoubleMatrix mapped = m.map(x -> Double.isNaN(x) ? 0.0 : x * 2);
            assertEquals(2.0, mapped.get(0, 0), DELTA);
            assertEquals(0.0, mapped.get(0, 1), DELTA); // NaN replaced with 0
            assertEquals(0.0, mapped.get(1, 0), DELTA); // NaN replaced with 0
            assertEquals(8.0, mapped.get(1, 1), DELTA);

            // Test replaceIf with NaN
            DoubleMatrix replaced = DoubleMatrix.wrap(new double[][] { { 1.0, Double.NaN }, { Double.NaN, 4.0 } });
            replaced.replaceIf(Double::isNaN, -1.0);
            assertEquals(1.0, replaced.get(0, 0), DELTA);
            assertEquals(-1.0, replaced.get(0, 1), DELTA);
            assertEquals(-1.0, replaced.get(1, 0), DELTA);
            assertEquals(4.0, replaced.get(1, 1), DELTA);
        }

        @Test
        public void testDoubleMatrixRowsForZeroColumnMatrix() {
            final DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { {}, {}, {} });
            final List<double[]> rows = matrix.rowStreams().map(DoubleStream::toArray).toList();

            assertEquals(3, rows.size());
            assertArrayEquals(new double[0], rows.get(0));
            assertArrayEquals(new double[0], rows.get(1));
            assertArrayEquals(new double[0], rows.get(2));
        }
    }

    @Nested
    class MapToIntTests {

        @Test
        public void testMapToInt() {
            double[][] arr = { { 1.5, 2.5 }, { 3.5, 4.5 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            IntMatrix mapped = matrix.mapToInt(d -> (int) Math.round(d));
            assertEquals(2, mapped.get(0, 0));
            assertEquals(3, mapped.get(0, 1));
            assertEquals(4, mapped.get(1, 0));
            assertEquals(5, mapped.get(1, 1));
        }

        @Test
        public void testMapToInt_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            IntMatrix result = m.mapToInt(x -> (int) x);
            assertEquals(1, result.get(0, 0));
            assertEquals(2, result.get(0, 1));
            assertEquals(3, result.get(1, 0));
            assertEquals(4, result.get(1, 1));
        }

        @Test
        public void testMapToInt_Shape2By2_FirstRowIntegerValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            IntMatrix result = m.mapToInt(x -> (int) x);
            assertEquals(1, result.get(0, 0));
            assertEquals(4, result.get(1, 1));
        }

        @Test
        public void testMapToInt_Shape2By2_EveryIntegerValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.7 }, { 3.2, 4.9 } });
            IntMatrix result = m.mapToInt(x -> (int) x);
            assertEquals(1, result.get(0, 0));
            assertEquals(2, result.get(0, 1));
            assertEquals(3, result.get(1, 0));
            assertEquals(4, result.get(1, 1));
        }
    }

    @Nested
    class MapToLongTests {

        @Test
        public void testMapToLong() {
            double[][] arr = { { 1.5, 2.5 }, { 3.5, 4.5 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            LongMatrix mapped = matrix.mapToLong(d -> Math.round(d));
            assertEquals(2L, mapped.get(0, 0));
            assertEquals(3L, mapped.get(0, 1));
            assertEquals(4L, mapped.get(1, 0));
            assertEquals(5L, mapped.get(1, 1));
        }

        @Test
        public void testMapToLong_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            LongMatrix result = m.mapToLong(x -> (long) (x * 1000));
            assertEquals(1500L, result.get(0, 0));
            assertEquals(4500L, result.get(1, 1));
        }

        @Test
        public void testMapToLong_Shape2By2_FirstRowLongValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            LongMatrix result = m.mapToLong(x -> (long) x);
            assertEquals(1L, result.get(0, 0));
            assertEquals(4L, result.get(1, 1));
        }

        @Test
        public void testMapToLong_Shape2By2_EveryLongValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.7 }, { 3.2, 4.9 } });
            LongMatrix result = m.mapToLong(x -> (long) x);
            assertEquals(1L, result.get(0, 0));
            assertEquals(2L, result.get(0, 1));
            assertEquals(3L, result.get(1, 0));
            assertEquals(4L, result.get(1, 1));
        }
    }

    @Nested
    class MapToObjTests {

        @Test
        public void testMapToObj() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            Matrix<String> mapped = matrix.mapToObj(d -> String.format("%.1f", d), String.class);
            assertEquals("1.0", mapped.get(0, 0));
            assertEquals("2.0", mapped.get(0, 1));
            assertEquals("3.0", mapped.get(1, 0));
            assertEquals("4.0", mapped.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            Matrix<String> result = m.mapToObj(x -> "val:" + x, String.class);
            assertEquals("val:1.5", result.get(0, 0));
            assertEquals("val:4.5", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_EveryStringValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Matrix<String> result = m.mapToObj(x -> String.valueOf(x), String.class);
            assertEquals("1.0", result.get(0, 0));
            assertEquals("2.0", result.get(0, 1));
            assertEquals("3.0", result.get(1, 0));
            assertEquals("4.0", result.get(1, 1));
        }

        @Test
        public void testMapToObj_WithNullElements() {
            // Test mapToObj to ensure proper object type conversion
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            Matrix<String> stringMatrix = m.mapToObj(d -> String.format("%.1f", d), String.class);

            assertEquals(2, stringMatrix.rowCount());
            assertEquals(2, stringMatrix.columnCount());
            assertEquals("1.5", stringMatrix.get(0, 0));
            assertEquals("4.5", stringMatrix.get(1, 1));
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testMapToObjArrayRankBoundaryAndPrimitiveTypeNormalization() {
            // Two matrix dimensions leave room for at most 253 dimensions in each element's type.
            final Object highestRankValue = java.lang.reflect.Array.newInstance(Object.class, new int[253]);
            final Class<Object> highestRankType = (Class<Object>) highestRankValue.getClass();
            final Class<?> excessiveRankType = java.lang.reflect.Array.newInstance(Object.class, new int[254]).getClass();

            for (final DoubleMatrix source : List.of(DoubleMatrix.empty(), new DoubleMatrix(new double[0][], 2), DoubleMatrix.wrap(new double[2][0]),
                    DoubleMatrix.wrap(new double[] { 1 }))) {
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

            final DoubleMatrix source = DoubleMatrix.wrap(new double[] { 1 });
            final Matrix<Double> scalars = source.mapToObj(value -> value, double.class);
            assertSame(Double.class, scalars.elementType());
            assertEquals(1, scalars.get(0, 0).doubleValue());

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
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.fill(5.0);
            assertEquals(5.0, matrix.get(0, 0));
            assertEquals(5.0, matrix.get(0, 1));
            assertEquals(5.0, matrix.get(1, 0));
            assertEquals(5.0, matrix.get(1, 1));
        }

        @Test
        public void testFill_WithValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.fill(99.5);
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals(99.5, m.get(i, j), DELTA);
                }
            }
        }

        @Test
        public void testFill_Value() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.fill(9.0);
            assertEquals(9.0, m.get(0, 0), 0.0);
            assertEquals(9.0, m.get(0, 1), 0.0);
            assertEquals(9.0, m.get(1, 0), 0.0);
            assertEquals(9.0, m.get(1, 1), 0.0);
        }

        @Test
        public void testFill_SingleValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.fill(9.0);
            assertEquals(9.0, m.get(0, 0));
            assertEquals(9.0, m.get(0, 1));
            assertEquals(9.0, m.get(1, 0));
            assertEquals(9.0, m.get(1, 1));
        }
    }

    @Nested
    class CopyFromTests {

        @Test
        public void testCopyFrom() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[][] fillArr = { { 5.0, 6.0 }, { 7.0, 8.0 } };
            matrix.copyFrom(fillArr);
            assertEquals(5.0, matrix.get(0, 0));
            assertEquals(6.0, matrix.get(0, 1));
            assertEquals(7.0, matrix.get(1, 0));
            assertEquals(8.0, matrix.get(1, 1));
        }

        @Test
        public void testCopyFromWithIndices() {
            double[][] arr = { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);
            double[][] fillArr = { { 7.0, 8.0 } };
            matrix.copyFrom(0, 1, fillArr);
            assertEquals(7.0, matrix.get(0, 1));
            assertEquals(8.0, matrix.get(0, 2));

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(-1, 0, fillArr));
        }

        @Test
        public void testCopyFrom_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 0.0, 0.0, 0.0 }, { 0.0, 0.0, 0.0 }, { 0.0, 0.0, 0.0 } });
            double[][] patch = { { 1.5, 2.5 }, { 3.5, 4.5 } };
            m.copyFrom(patch);
            assertEquals(1.5, m.get(0, 0), DELTA);
            assertEquals(2.5, m.get(0, 1), DELTA);
            assertEquals(3.5, m.get(1, 0), DELTA);
            assertEquals(4.5, m.get(1, 1), DELTA);
            assertEquals(0.0, m.get(2, 2), DELTA); // unchanged
        }

        @Test
        public void testCopyFromAtPosition() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 0.0, 0.0, 0.0 }, { 0.0, 0.0, 0.0 }, { 0.0, 0.0, 0.0 } });
            double[][] patch = { { 1.5, 2.5 }, { 3.5, 4.5 } };
            m.copyFrom(1, 1, patch);
            assertEquals(0.0, m.get(0, 0), DELTA); // unchanged
            assertEquals(1.5, m.get(1, 1), DELTA);
            assertEquals(2.5, m.get(1, 2), DELTA);
            assertEquals(3.5, m.get(2, 1), DELTA);
            assertEquals(4.5, m.get(2, 2), DELTA);
        }

        @Test
        public void testCopyFrom_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.copyFrom(new double[][] { { 7.0, 8.0 }, { 9.0, 10.0 } });
            assertEquals(7.0, m.get(0, 0));
            assertEquals(8.0, m.get(0, 1));
            assertEquals(9.0, m.get(1, 0));
            assertEquals(10.0, m.get(1, 1));
        }

        @Test
        public void testCopyFrom_WithPosition() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            m.copyFrom(1, 1, new double[][] { { 11.0, 12.0 }, { 13.0, 14.0 } });
            assertEquals(1.0, m.get(0, 0));
            assertEquals(11.0, m.get(1, 1));
            assertEquals(12.0, m.get(1, 2));
            assertEquals(13.0, m.get(2, 1));
            assertEquals(14.0, m.get(2, 2));
        }

        @Test
        public void testCopyFrom_Shape2By2_EveryCellValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.copyFrom(new double[][] { { 9.0, 8.0 }, { 7.0, 6.0 } });
            assertEquals(9.0, m.get(0, 0), 0.0);
            assertEquals(8.0, m.get(0, 1), 0.0);
            assertEquals(7.0, m.get(1, 0), 0.0);
            assertEquals(6.0, m.get(1, 1), 0.0);
        }

        @Test
        public void testCopyFrom_WithOffset() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            m.copyFrom(1, 1, new double[][] { { 99.0 } });
            assertEquals(1.0, m.get(0, 0), 0.0);
            assertEquals(99.0, m.get(1, 1), 0.0);
            assertEquals(9.0, m.get(2, 2), 0.0);
        }

        @Test
        public void testCopyFrom_WithOffset_ClipsToFit() {
            // fill method clips data to fit within matrix bounds, doesn't throw exception
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            m.copyFrom(0, 0, new double[][] { { 9.0, 8.0, 7.0 } }); // Source has 3 elements but matrix only has 2 columns
            assertEquals(9.0, m.get(0, 0), 0.0);
            assertEquals(8.0, m.get(0, 1), 0.0); // Only first 2 elements are copied
        }

        @Test
        public void testCopyFrom_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double[][] patch = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0", "3, 0", "0, -1", "0, 3" })
        public void testCopyFrom_RejectsOffsetsOutsideDestination(final int row, final int column) {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } });
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(row, column, new double[][] { { 1.0d } }));
            assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } }), matrix);
        }
    }

    @Nested
    class CopyTests {

        @Test
        public void testCopy() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix copy = matrix.copy();
            assertEquals(matrix.rowCount(), copy.rowCount());
            assertEquals(matrix.columnCount(), copy.columnCount());
            assertEquals(1.0, copy.get(0, 0));

            // Ensure it's a deep copy
            copy.set(0, 0, 5.0);
            assertEquals(1.0, matrix.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals(1.0, copy.get(0, 0), DELTA);

            // Modify copy shouldn't affect original
            copy.set(0, 0, 99.5);
            assertEquals(1.0, m.get(0, 0), DELTA);
            assertEquals(99.5, copy.get(0, 0), DELTA);
        }

        @Test
        public void testCopy_Shape2By2_CornerValuesAndIndependentStorage() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix copy = m.copy();
            assertEquals(1.0, copy.get(0, 0));
            assertEquals(4.0, copy.get(1, 1));
            copy.set(0, 0, 99.0);
            assertEquals(1.0, m.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2_DimensionsAndIndependentStorage() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals(1.0, copy.get(0, 0), 0.0);
            copy.set(0, 0, 99.0);
            assertEquals(1.0, m.get(0, 0), 0.0); // Original unchanged
        }
    }

    @Nested
    class CopyRowsTests {

        @Test
        public void testCopyRows() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix copy = matrix.copyRows(0, 2);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(1.0, copy.get(0, 0));
            assertEquals(3.0, copy.get(1, 0));

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(-1, 2));
        }

        @Test
        public void testCopyRows_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            DoubleMatrix subset = m.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertEquals(4.0, subset.get(0, 0), DELTA);
            assertEquals(9.0, subset.get(1, 2), DELTA);
        }

        @Test
        public void testCopyRows_Shape3By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            DoubleMatrix copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(3.0, copy.get(0, 0));
            assertEquals(6.0, copy.get(1, 1));
        }

        @Test
        public void testCopyRows_Shape3By2_DimensionsAndRowValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            DoubleMatrix copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(3.0, copy.get(0, 0), 0.0);
            assertEquals(6.0, copy.get(1, 1), 0.0);
        }

        @Test
        public void testCopyRangesEmpty_ReturnsEmptyMatrix() {
            // An empty row slice keeps the column count (0 x N) and an empty column slice keeps the row
            // count (N x 0); only an empty range in BOTH dimensions collapses to 0 x 0.
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });

            DoubleMatrix empty = m.copyRows(0, 0);
            assertEquals(0, empty.rowCount());
            assertEquals(3, empty.columnCount());

            DoubleMatrix emptyRows = m.copyRegion(1, 1, 0, 3);
            assertEquals(0, emptyRows.rowCount());
            assertEquals(3, emptyRows.columnCount());

            DoubleMatrix emptyCols = m.copyRegion(0, 2, 1, 1);
            assertEquals(2, emptyCols.rowCount());
            assertEquals(0, emptyCols.columnCount());
        }

        @Test
        public void testCopy_InvalidRange() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 5));
        }

        @Test
        public void testCopyRows_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(2, 1));
        }
    }

    @Nested
    class CopyRegionTests {

        // An empty row slice keeps the column count (0 x N) and an empty column slice keeps the row
        @Test
        public void testCopyRegion() {
            double[][] arr = { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix copy = matrix.copyRegion(0, 2, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(2.0, copy.get(0, 0));
            assertEquals(5.0, copy.get(1, 0));
        }

        @Test
        public void testCopyRegion_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            DoubleMatrix submatrix = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals(2.0, submatrix.get(0, 0), DELTA);
            assertEquals(6.0, submatrix.get(1, 1), DELTA);
        }

        @Test
        public void testCopyRegion_Shape3By3_UpperRightRegion() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            DoubleMatrix copy = m.copyRegion(1, 3, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(5.0, copy.get(0, 0));
            assertEquals(9.0, copy.get(1, 1));
        }

        @Test
        public void testCopyRegion_BottomRightSubmatrix() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            DoubleMatrix copy = m.copyRegion(1, 3, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(5.0, copy.get(0, 0), 0.0);
            assertEquals(9.0, copy.get(1, 1), 0.0);
        }

        @Test
        public void testCopyRegion_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, 0, 3));
        }
    }

    @Nested
    class ResizeTests {

        @Test
        public void testResize() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix extended = matrix.resize(3, 3);
            assertEquals(1.0, extended.get(0, 0));
            assertEquals(2.0, extended.get(0, 1));
            assertEquals(0.0, extended.get(0, 2));
            assertEquals(0.0, extended.get(2, 2));
        }

        @Test
        public void testResizeWithFill() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix extended = matrix.resize(3, 4, 9.0);
            assertEquals(1.0, extended.get(0, 0));
            assertEquals(9.0, extended.get(0, 2));
            assertEquals(9.0, extended.get(2, 0));

            DoubleMatrix truncated = matrix.resize(1, 1, 0.0);
            assertEquals(1, truncated.rowCount());
            assertEquals(1.0, truncated.get(0, 0));
        }

        @Test
        public void testResize_Fixture() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix extended = matrix.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(0.0, extended.get(2, 2));
        }

        @Test
        public void testResizeWithDefault() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix extended = matrix.resize(3, 3, -1.0);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(-1.0, extended.get(2, 2));

            assertThrows(IllegalArgumentException.class, () -> matrix.resize(-1, 3, -1.0));
        }

        @Test
        public void testResize_Larger() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix extended = m.resize(4, 4);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1.0, extended.get(0, 0), DELTA);
            assertEquals(4.0, extended.get(1, 1), DELTA);
            assertEquals(0.0, extended.get(3, 3), DELTA); // new cells are 0.0
        }

        @Test
        public void testResize_Smaller() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            DoubleMatrix truncated = m.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
            assertEquals(1.0, truncated.get(0, 0), DELTA);
            assertEquals(5.0, truncated.get(1, 1), DELTA);
        }

        @Test
        public void testResize_WithDefaultValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix extended = m.resize(3, 3, -1.5);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1.0, extended.get(0, 0), DELTA);
            assertEquals(-1.5, extended.get(2, 2), DELTA); // new cell
        }

        @Test
        public void testResize_Simple() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix extended = m.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1.0, extended.get(0, 0));
            assertEquals(4.0, extended.get(1, 1));
            assertEquals(0.0, extended.get(2, 2));
        }

        @Test
        public void testResize_WithDefaultValue_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix extended = m.resize(3, 3, 9.0);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(9.0, extended.get(2, 2));
        }

        @Test
        public void testResize_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix extended = m.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1.0, extended.get(0, 0), 0.0);
            assertEquals(0.0, extended.get(2, 2), 0.0);
        }

        @Test
        public void testResize_WithDefaultValue_Shape1By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix extended = m.resize(2, 3, 99.0);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1.0, extended.get(0, 0), 0.0);
            assertEquals(99.0, extended.get(1, 1), 0.0);
            assertEquals(99.0, extended.get(0, 2), 0.0);
        }

        @Test
        public void testResize_Smaller_Shape2By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            DoubleMatrix result = m.resize(1, 2);
            assertEquals(1, result.rowCount());
            assertEquals(2, result.columnCount());
        }

        @Test
        public void testResize_SmallerWithZeroDefault() {
            // Test pad where newRows < rows and newColumnCount < columnCount with default value 0
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            DoubleMatrix extended = m.resize(1, 2, 0.0);
            assertEquals(1, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals(1.0, extended.get(0, 0), DELTA);
            assertEquals(2.0, extended.get(0, 1), DELTA);
        }

        @Test
        public void testResize_GrowsRowsWhileTrimmingColumns() {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } });
            assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d }, { 3.0d }, { 9.0d } }), matrix.resize(3, 1, 9.0d));
            assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } }), matrix);
        }

        @Test
        public void testResize_GrowsWithNegativeZeroFill_PreservesNegativeZero() {
            // -0.0 has a non-zero bit pattern, so the doubleToRawLongBits != 0 short-circuit in
            // resize(...) must fill new cells even though -0.0 == 0.0 under == comparison.
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix grown = matrix.resize(2, 4, -0.0);

            assertEquals(2, grown.rowCount());
            assertEquals(4, grown.columnCount());
            assertEquals(1.0, grown.get(0, 0));
            assertEquals(2.0, grown.get(0, 1));
            for (int j = 2; j < 4; j++) {
                assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(grown.get(0, j)),
                        "Resize new cell at (0," + j + ") must keep the -0.0 bit pattern");
            }
            for (int j = 0; j < 4; j++) {
                assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(grown.get(1, j)),
                        "Resize new cell at (1," + j + ") must keep the -0.0 bit pattern");
            }
        }

        @Test
        public void testResize_WithNegativeDimensions() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 3, 0.0));
            assertThrows(IllegalArgumentException.class, () -> m.resize(3, -1, 0.0));
        }
    }

    @Nested
    class PadTests {

        @Test
        public void testPadWithDirections() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix extended = matrix.pad(1, 1, 1, 1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(0.0, extended.get(0, 0));
        }

        @Test
        public void testPadWithDirectionsAndDefault() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix extended = matrix.pad(1, 1, 1, 1, -1.0);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(-1.0, extended.get(0, 0));

            assertThrows(IllegalArgumentException.class, () -> matrix.pad(-1, 1, 1, 1, -1.0));
        }

        @Test
        public void testPad_Directional() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            DoubleMatrix extended = m.pad(1, 1, 2, 2);
            assertEquals(5, extended.rowCount()); // 1 + 3 + 1
            assertEquals(7, extended.columnCount()); // 2 + 3 + 2

            // Original values at offset position
            assertEquals(1.0, extended.get(1, 2), DELTA);
            assertEquals(5.0, extended.get(2, 3), DELTA);

            // New cells are 0.0
            assertEquals(0.0, extended.get(0, 0), DELTA);
        }

        @Test
        public void testPad_DirectionalWithDefault() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            DoubleMatrix extended = m.pad(1, 1, 1, 1, -1.5);
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());

            // Check original values
            assertEquals(1.0, extended.get(1, 1), DELTA);

            // Check new values
            assertEquals(-1.5, extended.get(0, 0), DELTA);
            assertEquals(-1.5, extended.get(4, 4), DELTA);
        }

        @Test
        public void testPad_Directions() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 5.0 } });
            DoubleMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(5.0, extended.get(1, 1));
            assertEquals(0.0, extended.get(0, 0));
        }

        @Test
        public void testPad_DirectionsWithDefault() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 5.0 } });
            DoubleMatrix extended = m.pad(1, 1, 1, 1, 9.0);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(5.0, extended.get(1, 1));
            assertEquals(9.0, extended.get(0, 0));
        }

        @Test
        public void testPad_Directional_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1.0, extended.get(1, 1), 0.0);
            assertEquals(0.0, extended.get(0, 0), 0.0);
        }

        @Test
        public void testPad_Directional_WithDefaultValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix extended = m.pad(1, 1, 1, 1, 99.0);
            assertEquals(3, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1.0, extended.get(1, 1), 0.0);
            assertEquals(99.0, extended.get(0, 0), 0.0);
        }

        @Test
        public void testPadRepeatFlattenAndConversions_EdgeCase() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.5d, 2.5d } });

            DoubleMatrix extended = matrix.pad(1, 0, 1, 0, 9.5d);
            DoubleMatrix repeatedElements = matrix.repeatElements(1, 2);
            DoubleMatrix repeatedMatrix = matrix.repeatMatrix(2, 1);

            assertEquals(9.5d, extended.get(0, 0));
            assertArrayEquals(new double[] { 1.5d, 1.5d, 2.5d, 2.5d }, repeatedElements.flatten().toArray(), 0.0001d);
            assertArrayEquals(new double[] { 1.5d, 2.5d, 1.5d, 2.5d }, repeatedMatrix.flatten().toArray(), 0.0001d);
            assertArrayEquals(new int[] { 1, 2 }, matrix.toIntMatrix().flatten().toArray());
            assertArrayEquals(new long[] { 1L, 2L }, matrix.toLongMatrix().flatten().toArray());
            assertArrayEquals(new float[] { 1.5f, 2.5f }, matrix.toFloatMatrix().flatten().toArray(), 0.0001f);
        }

        @Test
        public void testPad_WithNaNFill_PropagatesNaN() {
            // Extending with NaN must fill all newly added cells with NaN. This exercises the
            // doubleToRawLongBits != 0 short-circuit in pad(...).
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix extended = matrix.pad(1, 1, 1, 1, Double.NaN);

            assertEquals(3, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1.0, extended.get(1, 1));
            assertEquals(2.0, extended.get(1, 2));
            assertTrue(Double.isNaN(extended.get(0, 0)));
            assertTrue(Double.isNaN(extended.get(0, 3)));
            assertTrue(Double.isNaN(extended.get(2, 0)));
            assertTrue(Double.isNaN(extended.get(2, 3)));
            assertTrue(Double.isNaN(extended.get(1, 0)));
            assertTrue(Double.isNaN(extended.get(1, 3)));
        }

        @Test
        public void testPad_SingleAxisFillPreservesExistingValues() {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } });
            assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 9.0d }, { 3.0d, 4.0d, 9.0d } }), matrix.pad(0, 0, 0, 1, 9.0d));
            assertEquals(DoubleMatrix.wrap(new double[][] { { 9.0d, 1.0d, 2.0d }, { 9.0d, 3.0d, 4.0d } }), matrix.pad(0, 0, 1, 0, 9.0d));
            assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d }, { 9.0d, 9.0d } }), matrix.pad(0, 1, 0, 0, 9.0d));
            final var copy = matrix.pad(0, 0, 0, 0, 9.0d);
            assertEquals(matrix, copy);
            assertNotSame(matrix, copy);
        }

        @Test
        public void testPad_DirectionalWithNegativeValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.pad(-1, 1, 1, 1, 0.0));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0, 0, 0", "0, -1, 0, 0", "0, 0, -1, 0", "0, 0, 0, -1", "2147483647, 0, 0, 0", "0, 0, 0, 2147483647" })
        public void testPad_RejectsNegativeOrOverflowingDimensions(final int top, final int bottom, final int left, final int right) {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } });
            assertThrows(IllegalArgumentException.class, () -> matrix.pad(top, bottom, left, right));
            assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } }), matrix);
        }
    }

    @Nested
    class FlipHorizontallyInPlaceTests {

        @Test
        public void testReverseH() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.flipHorizontallyInPlace();
            assertEquals(2.0, matrix.get(0, 0));
            assertEquals(1.0, matrix.get(0, 1));
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            m.flipHorizontallyInPlace();
            assertEquals(3.0, m.get(0, 0), DELTA);
            assertEquals(2.0, m.get(0, 1), DELTA);
            assertEquals(1.0, m.get(0, 2), DELTA);
            assertEquals(6.0, m.get(1, 0), DELTA);
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.flipHorizontallyInPlace();
            assertEquals(2.0, m.get(0, 0));
            assertEquals(1.0, m.get(0, 1));
            assertEquals(4.0, m.get(1, 0));
            assertEquals(3.0, m.get(1, 1));
        }

        @Test
        public void testFlipHorizontallyInPlace() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            m.flipHorizontallyInPlace();
            assertEquals(3.0, m.get(0, 0), 0.0);
            assertEquals(2.0, m.get(0, 1), 0.0);
            assertEquals(1.0, m.get(0, 2), 0.0);
        }

        @Test
        public void testFlipHorizontallyInPlace_ReversesEveryRowValue() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } });
            matrix.flipHorizontallyInPlace();
            assertArrayEquals(new double[] { 3.0, 2.0, 1.0 }, matrix.rowView(0));
        }
    }

    @Nested
    class FlipVerticallyInPlaceTests {

        @Test
        public void testReverseV() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            matrix.flipVerticallyInPlace();
            assertEquals(3.0, matrix.get(0, 0));
            assertEquals(1.0, matrix.get(1, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape3By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            m.flipVerticallyInPlace();
            assertEquals(5.0, m.get(0, 0), DELTA);
            assertEquals(6.0, m.get(0, 1), DELTA);
            assertEquals(3.0, m.get(1, 0), DELTA);
            assertEquals(1.0, m.get(2, 0), DELTA);
        }

        @Test
        public void testFlipVerticallyInPlace_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            m.flipVerticallyInPlace();
            assertEquals(3.0, m.get(0, 0));
            assertEquals(4.0, m.get(0, 1));
            assertEquals(1.0, m.get(1, 0));
            assertEquals(2.0, m.get(1, 1));
        }

        @Test
        public void testFlipVerticallyInPlace() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            m.flipVerticallyInPlace();
            assertEquals(5.0, m.get(0, 0), 0.0);
            assertEquals(3.0, m.get(1, 0), 0.0);
            assertEquals(1.0, m.get(2, 0), 0.0);
        }

        @Test
        public void testFlipVerticallyInPlace_ReversesSingleColumnValues() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0 }, { 2.0 }, { 3.0 } });
            matrix.flipVerticallyInPlace();
            assertEquals(3.0, matrix.get(0, 0));
            assertEquals(2.0, matrix.get(1, 0));
            assertEquals(1.0, matrix.get(2, 0));
        }
    }

    @Nested
    class FlipHorizontallyTests {

        // Original unchanged
        @Test
        public void testFlipH() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix flipped = matrix.flipHorizontally();
            assertEquals(2.0, flipped.get(0, 0));
            assertEquals(1.0, flipped.get(0, 1));

            // Original unchanged
            assertEquals(1.0, matrix.get(0, 0));
        }

        @Test
        public void testFlipHorizontally_Shape2By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            DoubleMatrix flipped = m.flipHorizontally();
            assertEquals(3.0, flipped.get(0, 0), DELTA);
            assertEquals(2.0, flipped.get(0, 1), DELTA);
            assertEquals(1.0, flipped.get(0, 2), DELTA);

            // Original unchanged
            assertEquals(1.0, m.get(0, 0), DELTA);
        }

        @Test
        public void testFlipHorizontally_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix flipped = m.flipHorizontally();
            assertEquals(2.0, flipped.get(0, 0));
            assertEquals(1.0, flipped.get(0, 1));
            assertEquals(1.0, m.get(0, 0));
        }

        @Test
        public void testFlipHorizontally() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            DoubleMatrix flipped = m.flipHorizontally();
            assertEquals(3.0, flipped.get(0, 0), 0.0);
            assertEquals(2.0, flipped.get(0, 1), 0.0);
            assertEquals(1.0, flipped.get(0, 2), 0.0);
            // Original unchanged
            assertEquals(1.0, m.get(0, 0), 0.0);
        }

        @Test
        public void testDoubleMatrixFlipH() {
            // DoubleMatrix.java: DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] {{1.0, 2.0, 3.0}});
            // DoubleMatrix flipped = matrix.flipHorizontally();   // returns [[3.0, 2.0, 1.0]], original unchanged
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } });
            DoubleMatrix flipped = matrix.flipHorizontally();
            assertEquals(3.0, flipped.get(0, 0));
            assertEquals(2.0, flipped.get(0, 1));
            assertEquals(1.0, flipped.get(0, 2));
            // Verify original unchanged
            assertEquals(1.0, matrix.get(0, 0));
        }

        @Test
        public void testFlipHorizontally_SingleRowPreservesSourceValues() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } });
            DoubleMatrix flipped = matrix.flipHorizontally();
            assertArrayEquals(new double[] { 3.0, 2.0, 1.0 }, flipped.rowView(0));
            // original unchanged
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, matrix.rowView(0));
        }
    }

    @Nested
    class FlipVerticallyTests {

        @Test
        public void testFlipV() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix flipped = matrix.flipVertically();
            assertEquals(3.0, flipped.get(0, 0));
            assertEquals(1.0, flipped.get(1, 0));

            // Original unchanged
            assertEquals(1.0, matrix.get(0, 0));
        }

        @Test
        public void testFlipVertically_Shape3By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            DoubleMatrix flipped = m.flipVertically();
            assertEquals(5.0, flipped.get(0, 0), DELTA);
            assertEquals(3.0, flipped.get(1, 0), DELTA);
            assertEquals(1.0, flipped.get(2, 0), DELTA);

            // Original unchanged
            assertEquals(1.0, m.get(0, 0), DELTA);
        }

        @Test
        public void testFlipVertically_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix flipped = m.flipVertically();
            assertEquals(3.0, flipped.get(0, 0));
            assertEquals(4.0, flipped.get(0, 1));
            assertEquals(1.0, m.get(0, 0));
        }

        @Test
        public void testFlipVertically() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            DoubleMatrix flipped = m.flipVertically();
            assertEquals(5.0, flipped.get(0, 0), 0.0);
            assertEquals(3.0, flipped.get(1, 0), 0.0);
            assertEquals(1.0, flipped.get(2, 0), 0.0);
            // Original unchanged
            assertEquals(1.0, m.get(0, 0), 0.0);
        }

        @Test
        public void testDoubleMatrixFlipV() {
            // DoubleMatrix.java: DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] {{1.0}, {2.0}, {3.0}});
            // DoubleMatrix flipped = matrix.flipVertically();   // returns [[3.0], [2.0], [1.0]], original unchanged
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0 }, { 2.0 }, { 3.0 } });
            DoubleMatrix flipped = matrix.flipVertically();
            assertEquals(3.0, flipped.get(0, 0));
            assertEquals(2.0, flipped.get(1, 0));
            assertEquals(1.0, flipped.get(2, 0));
            // Verify original unchanged
            assertEquals(1.0, matrix.get(0, 0));
        }
    }

    @Nested
    class Rotate90Tests {

        @Test
        public void testRotate90() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix rotated = matrix.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3.0, rotated.get(0, 0));
            assertEquals(1.0, rotated.get(0, 1));
        }

        @Test
        public void testRotate90_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3.0, rotated.get(0, 0), DELTA);
            assertEquals(1.0, rotated.get(0, 1), DELTA);
            assertEquals(4.0, rotated.get(1, 0), DELTA);
            assertEquals(2.0, rotated.get(1, 1), DELTA);
        }

        @Test
        public void testRotate90_MoreRowsThanCols() {
            // Test the else branch (rows > columnCount) in rotate90() method
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } }); // 3 rows, 2 columnCount
            DoubleMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            // After 90-degree clockwise rotation:
            // Original:     Rotated:
            // 1 2           5 3 1
            // 3 4    =>     6 4 2
            // 5 6
            assertEquals(5.0, rotated.get(0, 0), DELTA);
            assertEquals(3.0, rotated.get(0, 1), DELTA);
            assertEquals(1.0, rotated.get(0, 2), DELTA);
            assertEquals(6.0, rotated.get(1, 0), DELTA);
            assertEquals(4.0, rotated.get(1, 1), DELTA);
            assertEquals(2.0, rotated.get(1, 2), DELTA);
        }

        @Test
        public void testRotate90_Shape2By2_DimensionsAndEveryCellValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3.0, rotated.get(0, 0));
            assertEquals(1.0, rotated.get(0, 1));
            assertEquals(4.0, rotated.get(1, 0));
            assertEquals(2.0, rotated.get(1, 1));
        }

        @Test
        public void testRotate90_ClockwiseCellPositions() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3.0, rotated.get(0, 0), 0.0);
            assertEquals(1.0, rotated.get(0, 1), 0.0);
            assertEquals(4.0, rotated.get(1, 0), 0.0);
            assertEquals(2.0, rotated.get(1, 1), 0.0);
        }
    }

    @Nested
    class Rotate180Tests {

        @Test
        public void testRotate180() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix rotated = matrix.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4.0, rotated.get(0, 0));
            assertEquals(3.0, rotated.get(0, 1));
        }

        @Test
        public void testRotate180_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix rotated = m.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4.0, rotated.get(0, 0), DELTA);
            assertEquals(3.0, rotated.get(0, 1), DELTA);
            assertEquals(2.0, rotated.get(1, 0), DELTA);
            assertEquals(1.0, rotated.get(1, 1), DELTA);
        }

        @Test
        public void testRotate180_Shape2By2_EveryCellValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix rotated = m.rotate180();
            assertEquals(4.0, rotated.get(0, 0));
            assertEquals(3.0, rotated.get(0, 1));
            assertEquals(2.0, rotated.get(1, 0));
            assertEquals(1.0, rotated.get(1, 1));
        }

        @Test
        public void testRotate180_ReversesBothAxes() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix rotated = m.rotate180();
            assertEquals(4.0, rotated.get(0, 0), 0.0);
            assertEquals(3.0, rotated.get(0, 1), 0.0);
            assertEquals(2.0, rotated.get(1, 0), 0.0);
            assertEquals(1.0, rotated.get(1, 1), 0.0);
        }

        @Test
        public void testRotate180_Nx0_AgreesWithRotate90Twice() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[3][0]);

            DoubleMatrix via180 = m.rotate180();
            assertEquals(3, via180.rowCount());
            assertEquals(0, via180.columnCount());

            // Two 90-degree rotations must equal one 180-degree rotation even for a degenerate
            // shape; that only holds because rotate90 preserves the transposed column count.
            DoubleMatrix viaRotate90Twice = m.rotate90().rotate90();
            assertEquals(3, viaRotate90Twice.rowCount());
            assertEquals(0, viaRotate90Twice.columnCount());
        }
    }

    @Nested
    class Rotate270Tests {

        @Test
        public void testRotate270() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix rotated = matrix.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(2.0, rotated.get(0, 0));
            assertEquals(4.0, rotated.get(0, 1));
        }

        @Test
        public void testRotate270_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(2.0, rotated.get(0, 0), DELTA);
            assertEquals(4.0, rotated.get(0, 1), DELTA);
            assertEquals(1.0, rotated.get(1, 0), DELTA);
            assertEquals(3.0, rotated.get(1, 1), DELTA);
        }

        // Test addition with infinity
        @Test
        public void testRotate270_MoreRowsThanCols() {
            // Test the else branch (rows > columnCount) in rotate270() method
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } }); // 3 rows, 2 columnCount
            DoubleMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            // After 270-degree clockwise rotation (or 90 counter-clockwise):
            assertEquals(2.0, rotated.get(0, 0), DELTA);
            assertEquals(4.0, rotated.get(0, 1), DELTA);
            assertEquals(6.0, rotated.get(0, 2), DELTA);
        }

        @Test
        public void testRotate270_Shape2By2_EveryCellValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix rotated = m.rotate270();
            assertEquals(2.0, rotated.get(0, 0));
            assertEquals(4.0, rotated.get(0, 1));
            assertEquals(1.0, rotated.get(1, 0));
            assertEquals(3.0, rotated.get(1, 1));
        }

        @Test
        public void testRotate270_Shape2By2_DimensionsAndEveryCellValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(2.0, rotated.get(0, 0), 0.0);
            assertEquals(4.0, rotated.get(0, 1), 0.0);
            assertEquals(1.0, rotated.get(1, 0), 0.0);
            assertEquals(3.0, rotated.get(1, 1), 0.0);
        }
    }

    @Nested
    class TransposeTests {

        @Test
        public void testTranspose() {
            double[][] arr = { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix transposed = matrix.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1.0, transposed.get(0, 0));
            assertEquals(4.0, transposed.get(0, 1));
        }

        @Test
        public void testTranspose_Shape2By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            DoubleMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1.0, transposed.get(0, 0), DELTA);
            assertEquals(4.0, transposed.get(0, 1), DELTA);
            assertEquals(2.0, transposed.get(1, 0), DELTA);
            assertEquals(5.0, transposed.get(1, 1), DELTA);
            assertEquals(3.0, transposed.get(2, 0), DELTA);
            assertEquals(6.0, transposed.get(2, 1), DELTA);
        }

        @Test
        public void testTranspose_Square() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1.0, transposed.get(0, 0), DELTA);
            assertEquals(3.0, transposed.get(0, 1), DELTA);
            assertEquals(2.0, transposed.get(1, 0), DELTA);
            assertEquals(4.0, transposed.get(1, 1), DELTA);
        }

        @Test
        public void testTranspose_MoreRowsThanCols() {
            // Test the else branch (rows > columnCount) in transpose() method
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } }); // 3 rows, 2 columnCount
            DoubleMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(3, transposed.columnCount());
            assertEquals(1.0, transposed.get(0, 0), DELTA);
            assertEquals(3.0, transposed.get(0, 1), DELTA);
            assertEquals(5.0, transposed.get(0, 2), DELTA);
            assertEquals(2.0, transposed.get(1, 0), DELTA);
            assertEquals(4.0, transposed.get(1, 1), DELTA);
            assertEquals(6.0, transposed.get(1, 2), DELTA);
        }

        @Test
        public void testTranspose_Shape2By3_SwapsDimensionsAndCornerValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            DoubleMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1.0, transposed.get(0, 0));
            assertEquals(4.0, transposed.get(0, 1));
            assertEquals(3.0, transposed.get(2, 0));
            assertEquals(6.0, transposed.get(2, 1));
        }

        @Test
        public void testTranspose_Square_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix transposed = m.transpose();
            assertEquals(1.0, transposed.get(0, 0));
            assertEquals(3.0, transposed.get(0, 1));
            assertEquals(2.0, transposed.get(1, 0));
            assertEquals(4.0, transposed.get(1, 1));
        }

        @Test
        public void testTranspose_RectangularCellPositions() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            DoubleMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1.0, transposed.get(0, 0), 0.0);
            assertEquals(4.0, transposed.get(0, 1), 0.0);
            assertEquals(2.0, transposed.get(1, 0), 0.0);
            assertEquals(5.0, transposed.get(1, 1), 0.0);
        }

        @Test
        public void testTranspose_Square_Shape2By2_EveryCellValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1.0, transposed.get(0, 0), 0.0);
            assertEquals(3.0, transposed.get(0, 1), 0.0);
        }

        // DoubleMatrix.equals/hashCode use Double.doubleToLongBits semantics (per Javadoc),
        @Test
        public void testTranspose_Nx0_PreservesTransposedShape() {
            // An N x 0 matrix transposes to 0 x N: no elements, but the column count survives.
            DoubleMatrix t = DoubleMatrix.wrap(new double[3][0]).transpose();
            assertEquals(0, t.rowCount());
            assertEquals(3, t.columnCount());
        }
    }

    @Nested
    class ReshapeAndPadTests {

        @Test
        public void testReshape_LargerSize() {
            // reshape allows different total element count, fills extra positions with zeros
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix reshaped = m.reshapeAndPad(3, 3); // 4 elements -> 9 positions
            assertEquals(3, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals(1.0, reshaped.get(0, 0), 0.0);
            assertEquals(2.0, reshaped.get(0, 1), 0.0);
            assertEquals(3.0, reshaped.get(0, 2), 0.0);
            assertEquals(4.0, reshaped.get(1, 0), 0.0);
            assertEquals(0.0, reshaped.get(1, 1), 0.0); // Extra positions filled with zeros
            assertEquals(0.0, reshaped.get(1, 2), 0.0);
        }

        @Test
        public void testReshape_Empty() {
            DoubleMatrix empty = DoubleMatrix.empty();
            DoubleMatrix reshaped = empty.reshapeAndPad(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }

        @Test
        public void testReshape_InvalidSize() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix reshaped = m.reshapeAndPad(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals(1.0, reshaped.get(0, 0));
            assertEquals(4.0, reshaped.get(1, 0));

        }

        @ParameterizedTest
        @CsvSource({ "-1, 2", "2, -1", "1, 1" })
        public void testReshapeAndPad_RejectsNegativeAndInsufficientShapes(final int rows, final int columns) {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } });
            assertThrows(IllegalArgumentException.class, () -> matrix.reshapeAndPad(rows, columns));
            assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } }), matrix);
        }
    }

    @Nested
    class RepeatElementsTests {

        @Test
        public void testRepeatElements() {
            double[][] arr = { { 1.0, 2.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix repeated = matrix.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertEquals(1.0, repeated.get(0, 0));
            assertEquals(1.0, repeated.get(0, 1));
            assertEquals(1.0, repeated.get(0, 2));
            assertEquals(2.0, repeated.get(0, 3));

            assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(0, 1));
        }

        @Test
        public void testRepeatElements_Shape1By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 } });
            DoubleMatrix repeated = m.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals(1.5, repeated.get(0, 0), DELTA);
            assertEquals(1.5, repeated.get(0, 1), DELTA);
            assertEquals(1.5, repeated.get(0, 2), DELTA);
            assertEquals(2.5, repeated.get(0, 3), DELTA);
            assertEquals(2.5, repeated.get(0, 4), DELTA);
            assertEquals(2.5, repeated.get(0, 5), DELTA);
            assertEquals(1.5, repeated.get(1, 0), DELTA); // second row same as first
        }

        @Test
        public void testRepeatElements_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix result = m.repeatElements(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1.0, result.get(0, 0));
            assertEquals(1.0, result.get(0, 1));
            assertEquals(1.0, result.get(1, 0));
            assertEquals(1.0, result.get(1, 1));
            assertEquals(4.0, result.get(2, 2));
            assertEquals(4.0, result.get(3, 3));
        }

        @Test
        public void testRepeatElements_Shape2By2_RepeatedBlockAndAdjacentValue() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix result = m.repeatElements(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1.0, result.get(0, 0), 0.0);
            assertEquals(1.0, result.get(0, 1), 0.0);
            assertEquals(1.0, result.get(1, 0), 0.0);
            assertEquals(1.0, result.get(1, 1), 0.0);
            assertEquals(2.0, result.get(0, 2), 0.0);
        }

        @Test
        public void testRepeatElements_RepeatsCellsTwiceVerticallyAndThreeTimesHorizontally() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix repeated = matrix.repeatElements(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertArrayEquals(new double[] { 1.0, 1.0, 1.0, 2.0, 2.0, 2.0 }, repeated.rowView(0));
            assertArrayEquals(new double[] { 3.0, 3.0, 3.0, 4.0, 4.0, 4.0 }, repeated.rowView(2));
        }

        @Test
        public void testRepeatElements_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } });
            final var result = matrix.repeatElements(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, 0.0d);
            assertEquals(1.0d, matrix.get(0, 0));
        }

        @Test
        public void testRepeatElements_InvalidArguments() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @Test
        public void testRepeatElements_InvalidRepeats() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatElements_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(rows, columns));
        }
    }

    @Nested
    class RepeatMatrixTests {

        @Test
        public void testRepeatMatrix() {
            double[][] arr = { { 1.0, 2.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix repeated = matrix.repeatMatrix(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertEquals(1.0, repeated.get(0, 0));
            assertEquals(2.0, repeated.get(0, 1));
            assertEquals(1.0, repeated.get(0, 2));

            assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(0, 1));
        }

        @Test
        public void testRepeatMatrix_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            DoubleMatrix repeated = m.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals(1.5, repeated.get(0, 0), DELTA);
            assertEquals(2.5, repeated.get(0, 1), DELTA);
            assertEquals(1.5, repeated.get(0, 2), DELTA); // repeat starts
            assertEquals(2.5, repeated.get(0, 3), DELTA);

            assertEquals(3.5, repeated.get(1, 0), DELTA);
            assertEquals(4.5, repeated.get(1, 1), DELTA);

            // Check vertical repeat
            assertEquals(1.5, repeated.get(2, 0), DELTA); // vertical repeat starts
            assertEquals(2.5, repeated.get(2, 1), DELTA);
        }

        @Test
        public void testRepeatMatrix_Shape2By2_DimensionsAndTileCorners() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix result = m.repeatMatrix(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1.0, result.get(0, 0));
            assertEquals(2.0, result.get(0, 1));
            assertEquals(1.0, result.get(0, 2));
            assertEquals(2.0, result.get(0, 3));
            assertEquals(1.0, result.get(2, 0));
        }

        @Test
        public void testRepeatMatrix_Shape1By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix result = m.repeatMatrix(2, 2);
            assertEquals(2, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1.0, result.get(0, 0), 0.0);
            assertEquals(2.0, result.get(0, 1), 0.0);
            assertEquals(1.0, result.get(0, 2), 0.0);
            assertEquals(2.0, result.get(0, 3), 0.0);
        }

        @Test
        public void testRepeatMatrix_TilesTwiceVerticallyAndThreeTimesHorizontally() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix repeated = matrix.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertArrayEquals(new double[] { 1.0, 2.0, 1.0, 2.0, 1.0, 2.0 }, repeated.rowView(0));
            assertArrayEquals(new double[] { 3.0, 4.0, 3.0, 4.0, 3.0, 4.0 }, repeated.rowView(1));
        }

        @Test
        public void testRepeatMatrix_DegenerateShapesPreserveDimensionsAndValidation() {
            final DoubleMatrix noRows = new DoubleMatrix(new double[0][], 2);
            final DoubleMatrix noColumns = DoubleMatrix.wrap(new double[][] { {}, {} });

            // Large repeat factors must not cause repeated work when there are no cells to copy.
            // Keep the counts bounded so this shape regression remains safe on older implementations.
            final DoubleMatrix repeatedRows = noRows.repeatMatrix(100_000, 3);
            assertEquals(0, repeatedRows.rowCount());
            assertEquals(6, repeatedRows.columnCount());

            final DoubleMatrix repeatedColumns = noColumns.repeatMatrix(3, 100_000);
            assertEquals(6, repeatedColumns.rowCount());
            assertEquals(0, repeatedColumns.columnCount());
            for (int i = 0; i < repeatedColumns.rowCount(); i++) {
                assertEquals(0, repeatedColumns.rowView(i).length);
            }
            assertSame(DoubleMatrix.empty(), DoubleMatrix.empty().repeatMatrix(100_000, 100_000));

            assertThrows(IllegalArgumentException.class, () -> noRows.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> noColumns.repeatMatrix(1, -1));
            assertThrows(IllegalArgumentException.class, () -> noRows.repeatMatrix(1, Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> noColumns.repeatMatrix(Integer.MAX_VALUE, 1));
        }

        @Test
        public void testRepeatMatrix_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } });
            final var result = matrix.repeatMatrix(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, 0.0d);
            assertEquals(1.0d, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_OnlyColumnsRepeated() {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } });
            final var repeated = matrix.repeatMatrix(1, 2);
            assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 1.0d, 2.0d }, { 3.0d, 4.0d, 3.0d, 4.0d } }), repeated);
            repeated.set(0, 0, 3.0d);
            assertEquals(1.0d, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidArguments() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidRepeats() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatMatrix_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(rows, columns));
            assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } }), matrix);
        }
    }

    @Nested
    class FlattenTests {

        @Test
        public void testFlatten() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleList flat = matrix.flatten();
            assertEquals(4, flat.size());
            assertEquals(1.0, flat.get(0));
            assertEquals(2.0, flat.get(1));
            assertEquals(3.0, flat.get(2));
            assertEquals(4.0, flat.get(3));
        }

        @Test
        public void testFlatten_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            DoubleList flat = m.flatten();
            assertEquals(9, flat.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1.0, flat.get(i), DELTA);
            }
        }

        @Test
        public void testFlatten_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleList flat = m.flatten();
            assertEquals(4, flat.size());
            assertEquals(1.0, flat.get(0));
            assertEquals(2.0, flat.get(1));
            assertEquals(3.0, flat.get(2));
            assertEquals(4.0, flat.get(3));
        }

        @Test
        public void testFlatten_Shape2By2_EveryListElement() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleList flat = m.flatten();
            assertEquals(4, flat.size());
            assertEquals(1.0, flat.get(0), 0.0);
            assertEquals(2.0, flat.get(1), 0.0);
            assertEquals(3.0, flat.get(2), 0.0);
            assertEquals(4.0, flat.get(3), 0.0);
        }

        @Test
        public void testFlatten_SquareRowMajorList() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleList list = matrix.flatten();
            assertEquals(DoubleList.of(1.0, 2.0, 3.0, 4.0), list);
        }

        @Test
        public void testFlatten_Empty() {
            DoubleMatrix empty = DoubleMatrix.empty();
            DoubleList flat = empty.flatten();
            assertTrue(flat.isEmpty());
        }
    }

    @Nested
    class MutateViaFlatArrayTests {

        @Test
        public void testFlatOp() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            int[] count = { 0 };
            matrix.mutateViaFlatArray(array -> count[0] += array.length);
            assertEquals(4, count[0]);
        }

        @Test
        public void testMutateViaFlatArray_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            List<Double> sums = new ArrayList<>();
            m.mutateViaFlatArray(row -> {
                double sum = 0.0;
                for (double val : row) {
                    sum += val;
                }
                sums.add(sum);
            });
            assertEquals(1, sums.size());
            assertEquals(45.0, sums.get(0), DELTA);
        }

        @Test
        public void testMutateViaFlatArray_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            final double[] sum = { 0.0 };
            m.mutateViaFlatArray(row -> {
                for (double val : row) {
                    sum[0] += val;
                }
            });
            assertEquals(10.0, sum[0]);
        }

        @Test
        public void testMutateViaFlatArray() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            AtomicInteger count = new AtomicInteger(0);
            m.mutateViaFlatArray(row -> count.addAndGet(row.length));
            assertEquals(4, count.get());
        }

        @Test
        public void testMutateViaFlatArray_SortsAllCellsInRowOrder() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 5.0, 3.0 }, { 4.0, 1.0 } });
            matrix.mutateViaFlatArray(arr -> java.util.Arrays.sort(arr));
            assertEquals(1.0, matrix.get(0, 0));
            assertEquals(3.0, matrix.get(0, 1));
            assertEquals(4.0, matrix.get(1, 0));
            assertEquals(5.0, matrix.get(1, 1));
        }
    }

    @Nested
    class StackVerticallyTests {

        @Test
        public void testVstack() {
            DoubleMatrix a = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix b = DoubleMatrix.wrap(new double[][] { { 3.0, 4.0 } });

            DoubleMatrix stacked = a.stackVertically(b);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1.0, stacked.get(0, 0));
            assertEquals(3.0, stacked.get(1, 0));

            DoubleMatrix c = DoubleMatrix.wrap(new double[][] { { 5.0 } });
            assertThrows(IllegalArgumentException.class, () -> a.stackVertically(c));
        }

        @Test
        public void testStackVertically_Shape2By3() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 7.0, 8.0, 9.0 }, { 10.0, 11.0, 12.0 } });
            DoubleMatrix stacked = m1.stackVertically(m2);

            assertEquals(4, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals(1.0, stacked.get(0, 0), DELTA);
            assertEquals(7.0, stacked.get(2, 0), DELTA);
            assertEquals(12.0, stacked.get(3, 2), DELTA);
        }

        @Test
        public void testStackVertically_DifferentColumnCounts() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_Shape2By2() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix stacked = m1.stackVertically(m2);
            assertEquals(4, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1.0, stacked.get(0, 0));
            assertEquals(5.0, stacked.get(2, 0));
        }

        @Test
        public void testStackVertically_IncompatibleColumns() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 3.0, 4.0 } });
            DoubleMatrix result = m1.stackVertically(m2);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0, result.get(0, 0), 0.0);
            assertEquals(3.0, result.get(1, 0), 0.0);
        }

        @Test
        public void testStackVertically_IncompatibleCols() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 3.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_PreservesEveryAppendedRow() {
            DoubleMatrix a = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix b = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 } });
            DoubleMatrix stacked = a.stackVertically(b);
            assertEquals(3, stacked.rowCount());
            assertArrayEquals(new double[] { 1.0, 2.0 }, stacked.rowView(0));
            assertArrayEquals(new double[] { 3.0, 4.0 }, stacked.rowView(1));
            assertArrayEquals(new double[] { 5.0, 6.0 }, stacked.rowView(2));
        }

        @Test
        public void testDoubleMatrixBinaryOpsRejectNullMatrix() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1D } });
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
            DoubleMatrix a = DoubleMatrix.wrap(new double[][] { { 1.0 }, { 3.0 } });
            DoubleMatrix b = DoubleMatrix.wrap(new double[][] { { 2.0 }, { 4.0 } });

            DoubleMatrix stacked = a.stackHorizontally(b);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1.0, stacked.get(0, 0));
            assertEquals(2.0, stacked.get(0, 1));

            DoubleMatrix c = DoubleMatrix.wrap(new double[][] { { 5.0 } });
            assertThrows(IllegalArgumentException.class, () -> a.stackHorizontally(c));
        }

        @Test
        public void testStackHorizontally_Shape2By2() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1.0, stacked.get(0, 0), DELTA);
            assertEquals(5.0, stacked.get(0, 2), DELTA);
            assertEquals(8.0, stacked.get(1, 3), DELTA);
        }

        @Test
        public void testStackHorizontally_DifferentRowCounts() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_Shape2By2_DimensionsAndOuterValues() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals(1.0, stacked.get(0, 0));
            assertEquals(5.0, stacked.get(0, 2));
        }

        @Test
        public void testStackHorizontally_IncompatibleRows() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0 }, { 2.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 3.0 }, { 4.0 } });
            DoubleMatrix result = m1.stackHorizontally(m2);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0, result.get(0, 0), 0.0);
            assertEquals(3.0, result.get(0, 1), 0.0);
        }

        @Test
        public void testStackHorizontally_IncompatibleRows_Shape1By1_Validation() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 3.0 }, { 4.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_PreservesEveryAppendedColumn() {
            DoubleMatrix a = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix b = DoubleMatrix.wrap(new double[][] { { 5.0 }, { 6.0 } });
            DoubleMatrix stacked = a.stackHorizontally(b);
            assertEquals(2, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertArrayEquals(new double[] { 1.0, 2.0, 5.0 }, stacked.rowView(0));
            assertArrayEquals(new double[] { 3.0, 4.0, 6.0 }, stacked.rowView(1));
        }

        @Test
        public void testStackHorizontally_RejectsLogicalWidthOverflowWithoutAllocation() {
            final var huge = DoubleMatrix.wrap(new double[0][0]).resize(0, Integer.MAX_VALUE);
            final var extra = DoubleMatrix.wrap(new double[0][0]).resize(0, 1);
            assertThrows(IllegalArgumentException.class, () -> huge.stackHorizontally(extra));
            assertEquals(Integer.MAX_VALUE, huge.columnCount());
        }
    }

    @Nested
    class AddTests {

        @Test
        public void testAdd() {
            DoubleMatrix a = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix b = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });

            DoubleMatrix sum = a.add(b);
            assertEquals(6.0, sum.get(0, 0));
            assertEquals(8.0, sum.get(0, 1));
            assertEquals(10.0, sum.get(1, 0));
            assertEquals(12.0, sum.get(1, 1));

            DoubleMatrix c = DoubleMatrix.wrap(new double[][] { { 1.0 } });
            assertThrows(IllegalArgumentException.class, () -> a.add(c));
        }

        @Test
        public void testFloatingPointPrecision() {
            // Test operations that may introduce floating point precision issues
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 0.1, 0.2 }, { 0.3, 0.7 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 0.1, 0.1 }, { 0.2, 0.3 } });

            // Addition with potential precision issues
            DoubleMatrix sum = m1.add(m2);
            assertEquals(0.2, sum.get(0, 0), 0.0001);
            assertEquals(0.3, sum.get(0, 1), 0.0001);
            assertEquals(0.5, sum.get(1, 0), 0.0001);
            assertEquals(1.0, sum.get(1, 1), 0.0001);

            // Subtraction with potential precision issues
            DoubleMatrix diff = m1.subtract(m2);
            assertEquals(0.0, diff.get(0, 0), 0.0001);
            assertEquals(0.1, diff.get(0, 1), 0.0001);
            assertEquals(0.1, diff.get(1, 0), 0.0001);
            assertEquals(0.4, diff.get(1, 1), 0.0001);

            // Test operations that often reveal precision issues
            DoubleMatrix precisionTest = DoubleMatrix.wrap(new double[][] { { 1.0 / 3.0, 2.0 / 3.0 } });
            DoubleMatrix multiplyByThree = precisionTest.map(x -> x * 3.0);
            assertEquals(1.0, multiplyByThree.get(0, 0), 0.0001);
            assertEquals(2.0, multiplyByThree.get(0, 1), 0.0001);
        }

        // Test subtraction anti-commutativity
        @Test
        public void testArithmeticEdgeCases() {
            // Test with zero matrix
            DoubleMatrix zeros = DoubleMatrix.wrap(new double[][] { { 0.0, 0.0 }, { 0.0, 0.0 } });
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });

            DoubleMatrix addZero = m.add(zeros);
            assertEquals(m.get(0, 0), addZero.get(0, 0), 0.0001);
            assertEquals(m.get(1, 1), addZero.get(1, 1), 0.0001);

            DoubleMatrix subtractZero = m.subtract(zeros);
            assertEquals(m.get(0, 0), subtractZero.get(0, 0), 0.0001);
            assertEquals(m.get(1, 1), subtractZero.get(1, 1), 0.0001);

            // Test multiplication with zero matrix
            DoubleMatrix multiplyZero = m.matrixMultiply(zeros);
            assertEquals(0.0, multiplyZero.get(0, 0), 0.0001);
            assertEquals(0.0, multiplyZero.get(1, 1), 0.0001);

            // Test addition commutativity
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.5, 6.5 }, { 7.5, 8.5 } });
            DoubleMatrix sum1 = m1.add(m2);
            DoubleMatrix sum2 = m2.add(m1);

            assertEquals(sum1.get(0, 0), sum2.get(0, 0), 0.0001);
            assertEquals(sum1.get(1, 1), sum2.get(1, 1), 0.0001);

            // Test subtraction anti-commutativity
            DoubleMatrix diff1 = m1.subtract(m2);
            DoubleMatrix diff2 = m2.subtract(m1);
            assertEquals(diff1.get(0, 0), -diff2.get(0, 0), 0.0001);
            assertEquals(diff1.get(1, 1), -diff2.get(1, 1), 0.0001);
        }

        @Test
        public void testLargeMatrixArithmetic() {
            // Test with larger matrices to ensure performance and precision
            double[][] arr1 = new double[10][10];
            double[][] arr2 = new double[10][10];

            // Fill with test data
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    arr1[i][j] = (i * 10 + j + 1) * 0.1;
                    arr2[i][j] = (i * 10 + j + 1) * 0.2;
                }
            }

            DoubleMatrix large1 = DoubleMatrix.wrap(arr1);
            DoubleMatrix large2 = DoubleMatrix.wrap(arr2);

            // Test addition
            DoubleMatrix largeSum = large1.add(large2);
            assertEquals(0.3, largeSum.get(0, 0), 0.0001); // 0.1 + 0.2 = 0.3
            assertEquals(30.0, largeSum.get(9, 9), 0.0001); // 10.0 + 20.0 = 30.0

            // Test that sum of all elements is correct
            double totalSum = largeSum.rowMajorStream().sum();
            double expected = 3 * (1 + 2 + 3 + /* ... */ +100) * 0.1; // 3 * 5050 * 0.1 = 1515.0
            assertEquals(1515.0, totalSum, 0.1);
        }

        @Test
        public void testAdd_Shape2By2() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix sum = m1.add(m2);

            assertEquals(6.5, sum.get(0, 0), DELTA);
            assertEquals(8.5, sum.get(0, 1), DELTA);
            assertEquals(10.5, sum.get(1, 0), DELTA);
            assertEquals(12.5, sum.get(1, 1), DELTA);
        }

        @Test
        public void testAdd_DifferentDimensions() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testArithmeticEdgeCases_Shape2By2() {
            // Test with zero matrix
            DoubleMatrix zeros = DoubleMatrix.wrap(new double[][] { { 0.0, 0.0 }, { 0.0, 0.0 } });
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });

            DoubleMatrix addZero = m.add(zeros);
            assertEquals(m, addZero);

            DoubleMatrix subtractZero = m.subtract(zeros);
            assertEquals(m, subtractZero);

            // Test multiplication with zero matrix
            DoubleMatrix multiplyZero = m.matrixMultiply(zeros);
            assertEquals(zeros, multiplyZero);

            // Test addition commutativity
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            assertEquals(m1.add(m2), m2.add(m1));

            // Test subtraction anti-commutativity
            DoubleMatrix diff1 = m1.subtract(m2);
            DoubleMatrix diff2 = m2.subtract(m1);
            assertEquals(diff1.get(0, 0), -diff2.get(0, 0), DELTA);
            assertEquals(diff1.get(1, 1), -diff2.get(1, 1), DELTA);
        }

        @Test
        public void testLargeMatrixArithmetic_Shape10By10() {
            // Test with larger matrices
            double[][] arr1 = new double[10][10];
            double[][] arr2 = new double[10][10];

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    arr1[i][j] = i * 10.0 + j + 1.0;
                    arr2[i][j] = (i * 10.0 + j + 1.0) * 2.0;
                }
            }

            DoubleMatrix large1 = DoubleMatrix.wrap(arr1);
            DoubleMatrix large2 = DoubleMatrix.wrap(arr2);

            // Test addition
            DoubleMatrix largeSum = large1.add(large2);
            assertEquals(3.0, largeSum.get(0, 0), DELTA); // 1 + 2 = 3
            assertEquals(300.0, largeSum.get(9, 9), DELTA); // 100 + 200 = 300

            // Test sum of all elements
            double totalSum = largeSum.rowMajorStream().sum();
            assertEquals(15150.0, totalSum, DELTA); // 3*(1+2+...+100) = 3*5050 = 15150
        }

        @Test
        public void testAdd_Shape2By2_EveryCellSum() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix result = m1.add(m2);
            assertEquals(6.0, result.get(0, 0));
            assertEquals(8.0, result.get(0, 1));
            assertEquals(10.0, result.get(1, 0));
            assertEquals(12.0, result.get(1, 1));
        }

        @Test
        public void testAdd_IncompatibleSize() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testAdd_AllFourCellValues() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix result = m1.add(m2);
            assertEquals(6.0, result.get(0, 0), 0.0);
            assertEquals(8.0, result.get(0, 1), 0.0);
            assertEquals(10.0, result.get(1, 0), 0.0);
            assertEquals(12.0, result.get(1, 1), 0.0);
        }

        @Test
        public void testAdd_IncompatibleDimensions() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 3.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testAdd_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d } });
                final var right = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d } });
                assertEquals(DoubleMatrix.wrap(new double[][] { { 2.0d, 4.0d, 6.0d }, { 8.0d, 10.0d, 12.0d } }), left.add(right));
                assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d } }), left);
                assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        // Test that long values convert to double (may lose precision for very large longs)
        @Test
        public void testSpecialDoubleValues_Infinity() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }, { 3.0, 4.0 } });

            assertTrue(Double.isInfinite(m.get(0, 0)));
            assertEquals(Double.POSITIVE_INFINITY, m.get(0, 0), DELTA);
            assertTrue(Double.isInfinite(m.get(0, 1)));
            assertEquals(Double.NEGATIVE_INFINITY, m.get(0, 1), DELTA);

            // Test operations with infinity
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 1.0 }, { 1.0, 1.0 } });
            DoubleMatrix sum = m.add(m2);
            assertEquals(Double.POSITIVE_INFINITY, sum.get(0, 0), DELTA);
            assertEquals(Double.NEGATIVE_INFINITY, sum.get(0, 1), DELTA);
        }

        @Test
        public void testInfinityHandling() {
            // Test operations with Infinity values
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }, { 1.0, -1.0 } });

            assertEquals(Double.POSITIVE_INFINITY, m.get(0, 0), DELTA);
            assertEquals(Double.NEGATIVE_INFINITY, m.get(0, 1), DELTA);

            // Test addition with infinity
            DoubleMatrix result = m.add(DoubleMatrix.wrap(new double[][] { { 1.0, 1.0 }, { 1.0, 1.0 } }));
            assertEquals(Double.POSITIVE_INFINITY, result.get(0, 0), DELTA);
            assertEquals(Double.NEGATIVE_INFINITY, result.get(0, 1), DELTA);

            // Test replaceIf with infinity
            m.replaceIf(Double::isInfinite, 999.0);
            assertEquals(999.0, m.get(0, 0), DELTA);
            assertEquals(999.0, m.get(0, 1), DELTA);
            assertEquals(1.0, m.get(1, 0), DELTA);
            assertEquals(-1.0, m.get(1, 1), DELTA);
        }
    }

    @Nested
    class SubtractTests {

        @Test
        public void testSubtract() {
            DoubleMatrix a = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix b = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });

            DoubleMatrix diff = a.subtract(b);
            assertEquals(4.0, diff.get(0, 0));
            assertEquals(4.0, diff.get(0, 1));
            assertEquals(4.0, diff.get(1, 0));
            assertEquals(4.0, diff.get(1, 1));

            DoubleMatrix c = DoubleMatrix.wrap(new double[][] { { 1.0 } });
            assertThrows(IllegalArgumentException.class, () -> a.subtract(c));
        }

        // Regression: matrixMultiply must build its result via DoubleMatrix.wrap(result) (not the raw
        @Test
        public void testSubtract_Shape2By2() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 5.5, 6.5 }, { 7.5, 8.5 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix diff = m1.subtract(m2);

            assertEquals(4.5, diff.get(0, 0), DELTA);
            assertEquals(4.5, diff.get(0, 1), DELTA);
            assertEquals(4.5, diff.get(1, 0), DELTA);
            assertEquals(4.5, diff.get(1, 1), DELTA);
        }

        @Test
        public void testSubtract_DifferentDimensions() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testSubtract_Shape2By2_EveryCellDifference() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix result = m1.subtract(m2);
            assertEquals(4.0, result.get(0, 0));
            assertEquals(4.0, result.get(0, 1));
            assertEquals(4.0, result.get(1, 0));
            assertEquals(4.0, result.get(1, 1));
        }

        @Test
        public void testSubtract_AllFourCellValues() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix result = m1.subtract(m2);
            assertEquals(4.0, result.get(0, 0), 0.0);
            assertEquals(4.0, result.get(0, 1), 0.0);
            assertEquals(4.0, result.get(1, 0), 0.0);
            assertEquals(4.0, result.get(1, 1), 0.0);
        }

        @Test
        public void testSubtract_IncompatibleDimensions() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 3.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testSubtract_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = DoubleMatrix.wrap(new double[][] { { 7.0d, 8.0d, 9.0d }, { 10.0d, 11.0d, 12.0d } });
                final var right = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d } });
                assertEquals(DoubleMatrix.wrap(new double[][] { { 6.0d, 6.0d, 6.0d }, { 6.0d, 6.0d, 6.0d } }), left.subtract(right));
                assertEquals(DoubleMatrix.wrap(new double[][] { { 7.0d, 8.0d, 9.0d }, { 10.0d, 11.0d, 12.0d } }), left);
                assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }
    }

    @Nested
    class MatrixMultiplyTests {

        @Test
        public void testMultiply() {
            DoubleMatrix a = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix b = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });

            DoubleMatrix product = a.matrixMultiply(b);
            assertEquals(19.0, product.get(0, 0));
            assertEquals(22.0, product.get(0, 1));
            assertEquals(43.0, product.get(1, 0));
            assertEquals(50.0, product.get(1, 1));

            DoubleMatrix c = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } });
            assertThrows(IllegalArgumentException.class, () -> a.matrixMultiply(c));
        }

        @Test
        public void testMatrixMultiply_Shape2By2() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix product = m1.matrixMultiply(m2);

            assertEquals(19.0, product.get(0, 0), DELTA); // 1*5 + 2*7
            assertEquals(22.0, product.get(0, 1), DELTA); // 1*6 + 2*8
            assertEquals(43.0, product.get(1, 0), DELTA); // 3*5 + 4*7
            assertEquals(50.0, product.get(1, 1), DELTA); // 3*6 + 4*8
        }

        @Test
        public void testMultiply_IncompatibleDimensions() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testMatrixMultiply_RectangularMatrices() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } }); // 1x3
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 4.0 }, { 5.0 }, { 6.0 } }); // 3x1
            DoubleMatrix product = m1.matrixMultiply(m2);

            assertEquals(1, product.rowCount());
            assertEquals(1, product.columnCount());
            assertEquals(32.0, product.get(0, 0), DELTA); // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        }

        @Test
        public void testMatrixMultiply_Shape2By2_EveryCellProduct() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 2.0, 0.0 }, { 1.0, 2.0 } });
            DoubleMatrix result = m1.matrixMultiply(m2);
            assertEquals(4.0, result.get(0, 0));
            assertEquals(4.0, result.get(0, 1));
            assertEquals(10.0, result.get(1, 0));
            assertEquals(8.0, result.get(1, 1));
        }

        @Test
        public void testMatrixMultiply_IncompatibleSize() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testMatrixMultiply() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 2.0, 0.0 }, { 1.0, 2.0 } });
            DoubleMatrix result = m1.matrixMultiply(m2);
            assertEquals(4.0, result.get(0, 0), 0.0);
            assertEquals(4.0, result.get(0, 1), 0.0);
            assertEquals(10.0, result.get(1, 0), 0.0);
            assertEquals(8.0, result.get(1, 1), 0.0);
        }

        @Test
        public void testMatrixMultiply_IncompatibleDimensions() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 3.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testMatrixMultiply_ParallelAndSequentialAreBitwiseIdentical() {
            // Every path accumulates each cell from +0.0 in ascending k order, so forcing the parallel
            // path must not change a single bit of an FP result whose value depends on summation order.
            final int[][] shapes = { { 3, 7, 5 }, { 7, 3, 5 }, { 5, 7, 3 }, { 9, 1, 9 }, { 20, 30, 40 }, { 40, 30, 20 } };
            final java.util.Random random = new java.util.Random(42);

            for (final int[] shape : shapes) {
                final DoubleMatrix left = DoubleMatrix.random(shape[0], shape[1], random).map(x -> x * 1e6 - 5e5);
                final DoubleMatrix right = DoubleMatrix.random(shape[1], shape[2], random).map(x -> x * 1e-3 - 5e-4);
                final DoubleMatrix[] results = new DoubleMatrix[2];

                Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> results[0] = left.matrixMultiply(right));
                Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> results[1] = left.matrixMultiply(right));

                for (int i = 0; i < shape[0]; i++) {
                    for (int j = 0; j < shape[2]; j++) {
                        double expected = 0.0;

                        for (int k = 0; k < shape[1]; k++) {
                            expected += left.get(i, k) * right.get(k, j);
                        }

                        assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(results[0].get(i, j)));
                        assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(results[1].get(i, j)));
                    }
                }
            }
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testMatrixMultiply_ExplicitModesComputeRectangularProduct(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d }, { 5.0d, 6.0d } });
                final var right = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d } });
                assertEquals(DoubleMatrix.wrap(new double[][] { { 9.0d, 12.0d, 15.0d }, { 19.0d, 26.0d, 33.0d }, { 29.0d, 40.0d, 51.0d } }),
                        left.matrixMultiply(right));
                assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d }, { 5.0d, 6.0d } }), left);
                assertEquals(DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testMatrixMultiply_EmptyProductReturnsCanonicalEmpty() {
            // Regression: matrixMultiply must build its result via DoubleMatrix.wrap(result) (not the raw
            // constructor) so an empty product yields the shared EMPTY singleton, and must call
            // checkNonNegativeShape before allocation for consistency with the other
            // result-allocating methods (resize/reshape/transpose/rotate).
            DoubleMatrix product = DoubleMatrix.empty().matrixMultiply(DoubleMatrix.empty());
            assertSame(DoubleMatrix.empty(), product);
            assertTrue(product.isEmpty());
        }
    }

    @Nested
    class BoxedTests {

        @Test
        public void testBoxed() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            Matrix<Double> boxed = matrix.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Double.valueOf(1.0), boxed.get(0, 0));
            assertEquals(Double.valueOf(4.0), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5, 3.5 }, { 4.5, 5.5, 6.5 } });
            Matrix<Double> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Double.valueOf(1.5), boxed.get(0, 0));
            assertEquals(Double.valueOf(6.5), boxed.get(1, 2));
        }

        @Test
        public void testBoxed_MoreRowsThanCols() {
            // Test the else branch (rows > columnCount) in boxed() method
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 }, { 5.5, 6.5 }, { 7.5, 8.5 } }); // 4 rows, 2 columnCount
            Matrix<Double> boxed = m.boxed();
            assertEquals(4, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Double.valueOf(1.5), boxed.get(0, 0));
            assertEquals(Double.valueOf(8.5), boxed.get(3, 1));
            assertEquals(Double.valueOf(5.5), boxed.get(2, 0));
        }

        @Test
        public void testBoxed_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Matrix<Double> boxed = m.boxed();
            assertEquals(Double.valueOf(1.0), boxed.get(0, 0));
            assertEquals(Double.valueOf(4.0), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By2_DimensionsAndCornerValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Matrix<Double> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(1.0, boxed.get(0, 0), 0.0);
            assertEquals(4.0, boxed.get(1, 1), 0.0);
        }
    }

    @Nested
    class ToIntMatrixTests {

        @Test
        public void testToIntMatrix() {
            DoubleMatrix doubleMatrix = DoubleMatrix.wrap(new double[][] { { 1.9, 2.1 }, { 3.5, 4.0 } });
            IntMatrix intMatrix = doubleMatrix.toIntMatrix();
            assertEquals(1, intMatrix.get(0, 0));
            assertEquals(2, intMatrix.get(0, 1));
            assertEquals(3, intMatrix.get(1, 0));
            assertEquals(4, intMatrix.get(1, 1));
        }
    }

    @Nested
    class ToLongMatrixTests {

        @Test
        public void testToLongMatrix() {
            DoubleMatrix doubleMatrix = DoubleMatrix.wrap(new double[][] { { 1.9, 2.1 }, { 3.5, 4.0 } });
            LongMatrix longMatrix = doubleMatrix.toLongMatrix();
            assertEquals(1L, longMatrix.get(0, 0));
            assertEquals(2L, longMatrix.get(0, 1));
            assertEquals(3L, longMatrix.get(1, 0));
            assertEquals(4L, longMatrix.get(1, 1));
        }
    }

    @Nested
    class ToFloatMatrixTests {

        @Test
        public void testToFloatMatrix() {
            DoubleMatrix doubleMatrix = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.0, 4.0 } });
            FloatMatrix floatMatrix = doubleMatrix.toFloatMatrix();
            assertEquals(1.5f, floatMatrix.get(0, 0));
            assertEquals(2.5f, floatMatrix.get(0, 1));
            assertEquals(3.0f, floatMatrix.get(1, 0));
            assertEquals(4.0f, floatMatrix.get(1, 1));
        }

        @Test
        public void testToFloatMatrix_OverflowRounding() {
            // Far beyond the float range overflows to Infinity; slightly above Float.MAX_VALUE
            // rounds down to Float.MAX_VALUE; NaN stays NaN.
            double slightlyAboveFloatMax = 3.4028235E38; // > Float.MAX_VALUE but within float rounding range
            DoubleMatrix doubleMatrix = DoubleMatrix.wrap(new double[][] { { Double.MAX_VALUE, -Double.MAX_VALUE }, { slightlyAboveFloatMax, Double.NaN } });
            FloatMatrix floatMatrix = doubleMatrix.toFloatMatrix();
            assertEquals(Float.POSITIVE_INFINITY, floatMatrix.get(0, 0));
            assertEquals(Float.NEGATIVE_INFINITY, floatMatrix.get(0, 1));
            assertEquals(Float.MAX_VALUE, floatMatrix.get(1, 0));
            assertTrue(Float.isNaN(floatMatrix.get(1, 1)));
        }
    }

    @Nested
    class ZipWithTests {

        @Test
        public void testZipWith() {
            DoubleMatrix a = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix b = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });

            DoubleMatrix max = a.zipWith(b, Math::max);
            assertEquals(5.0, max.get(0, 0));
            assertEquals(6.0, max.get(0, 1));
            assertEquals(7.0, max.get(1, 0));
            assertEquals(8.0, max.get(1, 1));

            DoubleMatrix c = DoubleMatrix.wrap(new double[][] { { 1.0 } });
            assertThrows(IllegalArgumentException.class, () -> a.zipWith(c, Math::max));
        }

        @Test
        public void testZipWithThree() {
            DoubleMatrix a = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix b = DoubleMatrix.wrap(new double[][] { { 3.0, 4.0 } });
            DoubleMatrix c = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 } });

            DoubleMatrix result = a.zipWith(b, c, (x, y, z) -> x + y * z);
            assertEquals(16.0, result.get(0, 0));
            assertEquals(26.0, result.get(0, 1));

            DoubleMatrix d = DoubleMatrix.wrap(new double[][] { { 1.0 } });
            assertThrows(IllegalArgumentException.class, () -> a.zipWith(b, d, (x, y, z) -> x + y + z));
        }

        @Test
        public void testElementWiseOperationsWithZipWith() {
            // Test element-wise multiplication using zipWith
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 2.0, 3.0 }, { 4.0, 5.0 } });

            // Element-wise multiplication
            DoubleMatrix elementWiseProduct = m1.zipWith(m2, (a, b) -> a * b);
            assertEquals(2.0, elementWiseProduct.get(0, 0), 0.0001); // 1.0*2.0
            assertEquals(6.0, elementWiseProduct.get(0, 1), 0.0001); // 2.0*3.0
            assertEquals(12.0, elementWiseProduct.get(1, 0), 0.0001); // 3.0*4.0
            assertEquals(20.0, elementWiseProduct.get(1, 1), 0.0001); // 4.0*5.0

            // Element-wise division
            DoubleMatrix elementWiseDivision = m2.zipWith(m1, (a, b) -> a / b);
            assertEquals(2.0, elementWiseDivision.get(0, 0), 0.0001); // 2.0/1.0
            assertEquals(1.5, elementWiseDivision.get(0, 1), 0.0001); // 3.0/2.0
            assertEquals(1.333333, elementWiseDivision.get(1, 0), 0.0001); // 4.0/3.0
            assertEquals(1.25, elementWiseDivision.get(1, 1), 0.0001); // 5.0/4.0
        }

        @Test
        public void testZipWith_Shape2By2() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix result = m1.zipWith(m2, (a, b) -> a * b);

            assertEquals(5.0, result.get(0, 0), DELTA); // 1*5
            assertEquals(12.0, result.get(0, 1), DELTA); // 2*6
            assertEquals(21.0, result.get(1, 0), DELTA); // 3*7
            assertEquals(32.0, result.get(1, 1), DELTA); // 4*8
        }

        @Test
        public void testZipWith_DifferentShapes() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a + b));
        }

        @Test
        public void testZipWith_ThreeMatrices() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix m3 = DoubleMatrix.wrap(new double[][] { { 9.0, 10.0 }, { 11.0, 12.0 } });
            DoubleMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);

            assertEquals(15.0, result.get(0, 0), DELTA); // 1+5+9
            assertEquals(18.0, result.get(0, 1), DELTA); // 2+6+10
            assertEquals(21.0, result.get(1, 0), DELTA); // 3+7+11
            assertEquals(24.0, result.get(1, 1), DELTA); // 4+8+12
        }

        @Test
        public void testZipWith_ThreeMatrices_DifferentShapes() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix m3 = DoubleMatrix.wrap(new double[][] { { 9.0, 10.0, 11.0 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> a + b + c));
        }

        @Test
        public void testElementWiseMultiplyWithZipWith() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 2.0, 3.0 }, { 4.0, 5.0 } });

            // Element-wise multiplication
            DoubleMatrix elementWiseProduct = m1.zipWith(m2, (a, b) -> a * b);
            assertEquals(2.0, elementWiseProduct.get(0, 0), DELTA); // 1*2
            assertEquals(6.0, elementWiseProduct.get(0, 1), DELTA); // 2*3
            assertEquals(12.0, elementWiseProduct.get(1, 0), DELTA); // 3*4
            assertEquals(20.0, elementWiseProduct.get(1, 1), DELTA); // 4*5

            // Element-wise division
            DoubleMatrix elementWiseDivision = m2.zipWith(m1, (a, b) -> a / b);
            assertEquals(2.0, elementWiseDivision.get(0, 0), DELTA); // 2/1
            assertEquals(1.5, elementWiseDivision.get(0, 1), DELTA); // 3/2
            assertEquals(1.3333, elementWiseDivision.get(1, 0), 0.001); // 4/3
            assertEquals(1.25, elementWiseDivision.get(1, 1), DELTA); // 5/4
        }

        @Test
        public void testZipWith_Binary() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix result = m1.zipWith(m2, (a, b) -> a + b);
            assertEquals(6.0, result.get(0, 0));
            assertEquals(12.0, result.get(1, 1));
        }

        @Test
        public void testZipWith_Ternary() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix m3 = DoubleMatrix.wrap(new double[][] { { 1.0, 1.0 }, { 1.0, 1.0 } });
            DoubleMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(7.0, result.get(0, 0));
            assertEquals(13.0, result.get(1, 1));
        }

        @Test
        public void testZipWith_Binary_Shape2By2() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix result = m1.zipWith(m2, (a, b) -> a * b);
            assertEquals(5.0, result.get(0, 0), 0.0);
            assertEquals(12.0, result.get(0, 1), 0.0);
            assertEquals(21.0, result.get(1, 0), 0.0);
            assertEquals(32.0, result.get(1, 1), 0.0);
        }

        @Test
        public void testZipWith_Ternary_Shape1By2() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 3.0, 4.0 } });
            DoubleMatrix m3 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 } });
            DoubleMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(9.0, result.get(0, 0), 0.0);
            assertEquals(12.0, result.get(0, 1), 0.0);
        }

        @Test
        public void testDivisionByZero() {
            // Test division by zero behavior
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix zeros = DoubleMatrix.wrap(new double[][] { { 0.0, 0.0 }, { 0.0, 0.0 } });

            // Element-wise division by zero should produce infinity or NaN
            DoubleMatrix divByZero = m1.zipWith(zeros, (a, b) -> a / b);
            assertEquals(Double.POSITIVE_INFINITY, divByZero.get(0, 0), 0.0);
            assertEquals(Double.POSITIVE_INFINITY, divByZero.get(0, 1), 0.0);
            assertEquals(Double.POSITIVE_INFINITY, divByZero.get(1, 0), 0.0);
            assertEquals(Double.POSITIVE_INFINITY, divByZero.get(1, 1), 0.0);

            // Zero divided by zero should be NaN
            DoubleMatrix zeroByZero = zeros.zipWith(zeros, (a, b) -> a / b);
            assertTrue(Double.isNaN(zeroByZero.get(0, 0)));
            assertTrue(Double.isNaN(zeroByZero.get(1, 1)));
        }
    }

    @Nested
    class MainDiagonalStreamTests {

        @Test
        public void testStreamLU2RD() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] diagonal = matrix.mainDiagonalStream().toArray();
            assertEquals(2, diagonal.length);
            assertEquals(1.0, diagonal[0]);
            assertEquals(4.0, diagonal[1]);

            DoubleMatrix empty = DoubleMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().toArray().length);
        }

        @Test
        public void testMainDiagonalStream_Shape3By3_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            double[] diagonal = m.mainDiagonalStream().toArray();
            assertArrayEquals(new double[] { 1.0, 5.0, 9.0 }, diagonal, DELTA);
        }

        @Test
        public void testMainDiagonalStream_NonSquare() {
            DoubleMatrix nonSquare = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertArrayEquals(new double[] { 1.0 }, nonSquare.mainDiagonalStream().toArray(), DELTA);
        }

        @Test
        public void testMainDiagonalStream_Shape3By3_ArrayValues_DiagonalArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            DoubleStream stream = m.mainDiagonalStream();
            double[] result = stream.toArray();
            assertArrayEquals(new double[] { 1.0, 5.0, 9.0 }, result);
        }

        @Test
        public void testMainDiagonalStream() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double sum = m.mainDiagonalStream().sum();
            assertEquals(5.0, sum, 0.0);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testMainDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d }, { 7.0d, 8.0d, 9.0d } });
            final double[] expected = new double[] { 1.0d, 5.0d, 9.0d };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (DoubleIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextDouble);
            }
        }

        @Test
        public void testMainDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d }, { 7.0d, 8.0d, 9.0d } });
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (DoubleIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1.0d, iterator.nextDouble());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextDouble);
            }
        }

        @Test
        public void testMainDiagonalStream_Empty() {
            DoubleMatrix empty = DoubleMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().toArray().length);
        }

        // DoubleMatrix.equals/hashCode use Double.doubleToLongBits semantics (per Javadoc),
        // so NaN == NaN, and the hash code must be consistent with equals.
        @Test
        public void testDiagonalStream_Nx0MatrixThrowsBecauseNotSquare() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[3][0]);
            assertEquals(3, m.rowCount());
            assertEquals(0, m.columnCount());
            assertEquals(0, m.mainDiagonalStream().toArray().length);
            assertEquals(0, m.antiDiagonalStream().toArray().length);

            DoubleMatrix empty = DoubleMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().count());
            assertEquals(0, empty.antiDiagonalStream().count());
        }
    }

    @Nested
    class AntiDiagonalStreamTests {

        @Test
        public void testStreamRU2LD() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] diagonal = matrix.antiDiagonalStream().toArray();
            assertEquals(2, diagonal.length);
            assertEquals(2.0, diagonal[0]);
            assertEquals(3.0, diagonal[1]);
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            double[] antiDiagonal = m.antiDiagonalStream().toArray();
            assertArrayEquals(new double[] { 3.0, 5.0, 7.0 }, antiDiagonal, DELTA);
        }

        @Test
        public void testAntiDiagonalStream_NonSquare() {
            DoubleMatrix nonSquare = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertArrayEquals(new double[] { 2.0 }, nonSquare.antiDiagonalStream().toArray(), DELTA);
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_ArrayValues_AntiDiagonalArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            DoubleStream stream = m.antiDiagonalStream();
            double[] result = stream.toArray();
            assertArrayEquals(new double[] { 3.0, 5.0, 7.0 }, result);
        }

        @Test
        public void testAntiDiagonalStream() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double sum = m.antiDiagonalStream().sum();
            assertEquals(5.0, sum, 0.0);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testAntiDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d }, { 7.0d, 8.0d, 9.0d } });
            final double[] expected = new double[] { 3.0d, 5.0d, 7.0d };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (DoubleIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextDouble);
            }
        }

        @Test
        public void testAntiDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d }, { 7.0d, 8.0d, 9.0d } });
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (DoubleIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(3.0d, iterator.nextDouble());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextDouble);
            }
        }

        @Test
        public void testAntiDiagonalStream_Empty() {
            DoubleMatrix empty = DoubleMatrix.empty();
            assertEquals(0, empty.antiDiagonalStream().toArray().length);
        }
    }

    @Nested
    class RowMajorStreamTests {

        @Test
        public void testRowMajorStream() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] elements = matrix.rowMajorStream().toArray();
            assertEquals(4, elements.length);
            assertEquals(1.0, elements[0]);
            assertEquals(2.0, elements[1]);
            assertEquals(3.0, elements[2]);
            assertEquals(4.0, elements[3]);
        }

        @Test
        public void testRowMajorStreamWithRow() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] row = matrix.rowMajorStream(1, 2).toArray();
            assertEquals(2, row.length);
            assertEquals(3.0, row[0]);
            assertEquals(4.0, row[1]);
        }

        @Test
        public void testRowMajorStreamWithRange() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] elements = matrix.rowMajorStream(1, 3).toArray();
            assertEquals(4, elements.length);
            assertEquals(3.0, elements[0]);
            assertEquals(4.0, elements[1]);

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 2));
        }

        @Test
        public void testStatisticalOperations() {
            double[][] arr = { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            // Test sum operation on streams
            double totalSum = matrix.rowMajorStream().sum();
            assertEquals(45.0, totalSum, 0.0001); // 1+2+3+4+5+6+7+8+9 = 45

            // Test sum of specific row
            double row1Sum = matrix.rowMajorStream(1, 2).sum();
            assertEquals(15.0, row1Sum, 0.0001); // 4+5+6 = 15

            // Test sum of specific column
            double col0Sum = matrix.columnMajorStream(0, 1).sum();
            assertEquals(12.0, col0Sum, 0.0001); // 1+4+7 = 12

            // Test min/max on streams
            double min = matrix.rowMajorStream().min().orElse(0.0);
            assertEquals(1.0, min, 0.0001);

            double max = matrix.rowMajorStream().max().orElse(0.0);
            assertEquals(9.0, max, 0.0001);

            // Test average
            double avg = matrix.rowMajorStream().average().orElse(0.0);
            assertEquals(5.0, avg, 0.0001);

            // Test statistical operations on diagonal
            double diagonalSum = matrix.mainDiagonalStream().sum();
            assertEquals(15.0, diagonalSum, 0.0001); // 1+5+9 = 15

            double antiDiagonalSum = matrix.antiDiagonalStream().sum();
            assertEquals(15.0, antiDiagonalSum, 0.0001); // 3+5+7 = 15
        }

        @Test
        public void testRowMajorStream_Shape2By3_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            double[] all = m.rowMajorStream().toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }, all, DELTA);
        }

        @Test
        public void testRowMajorStream_WithRange() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            double[] rows = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new double[] { 4.0, 5.0, 6.0, 7.0, 8.0, 9.0 }, rows, DELTA);
        }

        @Test
        public void testStatisticalOperations_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });

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
            double min = m.rowMajorStream().min().orElse(0.0);
            assertEquals(1.0, min, DELTA);

            double max = m.rowMajorStream().max().orElse(0.0);
            assertEquals(9.0, max, DELTA);

            // Test average
            double avg = m.rowMajorStream().average().orElse(0.0);
            assertEquals(5.0, avg, DELTA);

            // Test diagonal operations
            double diagonalSum = m.mainDiagonalStream().sum();
            assertEquals(15.0, diagonalSum, DELTA); // 1+5+9 = 15

            double antiDiagonalSum = m.antiDiagonalStream().sum();
            assertEquals(15.0, antiDiagonalSum, DELTA); // 3+5+7 = 15
        }

        @Test
        public void testRowMajorStream_Shape2By2_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleStream stream = m.rowMajorStream();
            double[] result = stream.toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result);
        }

        @Test
        public void testRowMajorStream_WithRange_Shape3By2_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            DoubleStream stream = m.rowMajorStream(1, 3);
            double[] result = stream.toArray();
            assertArrayEquals(new double[] { 3.0, 4.0, 5.0, 6.0 }, result);
        }

        @Test
        public void testHorizontalStream() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double sum = m.rowMajorStream().sum();
            assertEquals(10.0, sum, 0.0);
        }

        @Test
        public void testRowMajorStream_WithRange_Shape3By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            double sum = m.rowMajorStream(1, 3).sum();
            assertEquals(18.0, sum, 0.0);
        }

        @Test
        public void testRowMajorStream_SquareSumAndRowMajorArray() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double sum = matrix.rowMajorStream().sum();
            assertEquals(10.0, sum);
            double[] array = matrix.rowMajorStream().toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, array);
        }

        @Test
        public void testHorizontalStreamRange() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            double[] subset = matrix.rowMajorStream(0, 2).toArray();
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, subset);
        }

        @Test
        public void testRowMajorStreamorizontalIteratorAdvanceAndExhaustion_EdgeCase() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1d, 2d }, { 3d, 4d } });
            var iterator = matrix.rowMajorStream(0, 2).iterator();

            assertTrue(iterator instanceof com.landawn.abacus.util.stream.DoubleIteratorEx);

            com.landawn.abacus.util.stream.DoubleIteratorEx ex = (com.landawn.abacus.util.stream.DoubleIteratorEx) iterator;
            ex.advance(2);
            assertEquals(3d, ex.nextDouble(), 0.0001d);
            ex.advance(10);
            assertFalse(ex.hasNext());
            assertThrows(java.util.NoSuchElementException.class, ex::nextDouble);

            // IteratorEx.count() is terminal: it consumes the remaining elements and leaves the iterator
            // exhausted, so a remaining-size assertion needs its own iterator.
            var counting = (com.landawn.abacus.util.stream.DoubleIteratorEx) matrix.rowMajorStream(0, 2).iterator();
            counting.advance(2);
            assertEquals(2L, counting.count());
            assertFalse(counting.hasNext());
        }

        @Test
        public void testStreamIterators_MidStrideAdvanceThenToArray_EdgeCase() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });

            var rowMajorIterator = matrix.rowMajorStream(0, 2).iterator();
            assertTrue(rowMajorIterator instanceof com.landawn.abacus.util.stream.DoubleIteratorEx);
            com.landawn.abacus.util.stream.DoubleIteratorEx rowMajor = (com.landawn.abacus.util.stream.DoubleIteratorEx) rowMajorIterator;
            rowMajor.advance(1); // mid-row cursor: the chunked toArray must start at column 1
            assertArrayEquals(new double[] { 2.0, 3.0, 4.0, 5.0, 6.0 }, rowMajor.toArray());

            // IteratorEx.count() is terminal: it consumes the remainder and exhausts the iterator, so every
            // remaining-size assertion below uses its own freshly positioned iterator.
            var rowMajorCount = (com.landawn.abacus.util.stream.DoubleIteratorEx) matrix.rowMajorStream(0, 2).iterator();
            rowMajorCount.advance(1);
            assertEquals(5L, rowMajorCount.count());

            var columnMajorIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.DoubleIteratorEx columnMajor = (com.landawn.abacus.util.stream.DoubleIteratorEx) columnMajorIterator;
            columnMajor.advance(1); // mid-column cursor: row 1 of column 0
            assertArrayEquals(new double[] { 4.0, 2.0, 5.0, 3.0, 6.0 }, columnMajor.toArray());

            var columnMajorCount = (com.landawn.abacus.util.stream.DoubleIteratorEx) matrix.columnMajorStream(0, 3).iterator();
            columnMajorCount.advance(1);
            assertEquals(5L, columnMajorCount.count());

            var crossingIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.DoubleIteratorEx crossing = (com.landawn.abacus.util.stream.DoubleIteratorEx) crossingIterator;
            crossing.advance(3); // crosses a column boundary: lands on row 1 of column 1
            assertEquals(5.0, crossing.nextDouble());
            crossing.advance(10);
            assertEquals(0L, crossing.count());
            assertThrows(java.util.NoSuchElementException.class, crossing::nextDouble);

            var crossingCount = (com.landawn.abacus.util.stream.DoubleIteratorEx) matrix.columnMajorStream(0, 3).iterator();
            crossingCount.advance(3);
            assertEquals(3L, crossingCount.count()); // consumes 5.0, 3.0, 6.0
            assertFalse(crossingCount.hasNext());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testRowMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d }, { 7.0d, 8.0d, 9.0d } });
            final double[] expected = new double[] { 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 6.0d, 7.0d, 8.0d, 9.0d };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (DoubleIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextDouble);
            }
        }

        @Test
        public void testRowMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d }, { 7.0d, 8.0d, 9.0d } });
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (DoubleIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1.0d, iterator.nextDouble());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextDouble);
            }
        }

        @Test
        public void testRowMajorStream_Empty() {
            DoubleMatrix empty = DoubleMatrix.empty();
            assertEquals(0, empty.rowMajorStream().toArray().length);
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            double[] row1 = m.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new double[] { 4.0, 5.0, 6.0 }, row1, DELTA);
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_Shape2By2_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleStream stream = m.rowMajorStream(1, 2);
            double[] result = stream.toArray();
            assertArrayEquals(new double[] { 3.0, 4.0 }, result);
        }

        @Test
        public void testRowMajorStream_BySingleRowRange() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double sum = m.rowMajorStream(0, 1).sum();
            assertEquals(3.0, sum, 0.0);
        }

        @Test
        public void testHorizontalStreamSingleRowRange() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double[] row1 = matrix.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new double[] { 3.0, 4.0 }, row1);
            double rowSum = matrix.rowMajorStream(1, 2).sum();
            assertEquals(7.0, rowSum);
        }

        @Test
        public void testRowMajorStream_ZeroColumnsKeepLogicalRowRange() {
            final var matrix = DoubleMatrix.wrap(new double[3][0]);
            assertEquals(0, matrix.rowMajorStream(1, 3).count());
            assertEquals(2, matrix.rowStreams(1, 3).count());
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 3));
        }

        @Test
        public void testRowMajorStream_WithRange_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 1));
        }
    }

    @Nested
    class ColumnMajorStreamTests {

        @Test
        public void testColumnMajorStream() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] elements = matrix.columnMajorStream().toArray();
            assertEquals(4, elements.length);
            assertEquals(1.0, elements[0]);
            assertEquals(3.0, elements[1]);
            assertEquals(2.0, elements[2]);
            assertEquals(4.0, elements[3]);
        }

        @Test
        public void testColumnMajorStreamWithColumn() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] col = matrix.columnMajorStream(1, 2).toArray();
            assertEquals(2, col.length);
            assertEquals(2.0, col[0]);
            assertEquals(4.0, col[1]);
        }

        @Test
        public void testColumnMajorStreamWithRange() {
            double[][] arr = { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] elements = matrix.columnMajorStream(1, 3).toArray();
            assertEquals(4, elements.length);

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(-1, 2));
        }

        @Test
        public void testColumnMajorStream_Shape2By3_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            double[] all = m.columnMajorStream().toArray();
            assertArrayEquals(new double[] { 1.0, 4.0, 2.0, 5.0, 3.0, 6.0 }, all, DELTA);
        }

        @Test
        public void testColumnMajorStream_WithRange() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            double[] columnCount = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new double[] { 2.0, 5.0, 8.0, 3.0, 6.0, 9.0 }, columnCount, DELTA);
        }

        @Test
        public void testColumnMajorStream_Shape2By2_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleStream stream = m.columnMajorStream();
            double[] result = stream.toArray();
            assertArrayEquals(new double[] { 1.0, 3.0, 2.0, 4.0 }, result);
        }

        @Test
        public void testColumnMajorStream_WithRange_Shape2By3_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            DoubleStream stream = m.columnMajorStream(1, 3);
            double[] result = stream.toArray();
            assertArrayEquals(new double[] { 2.0, 5.0, 3.0, 6.0 }, result);
        }

        @Test
        public void testVerticalStream() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double sum = m.columnMajorStream().sum();
            assertEquals(10.0, sum, 0.0);
        }

        @Test
        public void testColumnMajorStream_WithRange_Shape2By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            double sum = m.columnMajorStream(1, 3).sum();
            assertEquals(16.0, sum, 0.0);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testColumnMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d }, { 7.0d, 8.0d, 9.0d } });
            final double[] expected = new double[] { 1.0d, 4.0d, 7.0d, 2.0d, 5.0d, 8.0d, 3.0d, 6.0d, 9.0d };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (DoubleIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextDouble);
            }
        }

        @Test
        public void testColumnMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d }, { 7.0d, 8.0d, 9.0d } });
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (DoubleIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1.0d, iterator.nextDouble());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextDouble);
            }
        }

        @Test
        public void testColumnMajorStream_Empty() {
            DoubleMatrix empty = DoubleMatrix.empty();
            assertEquals(0, empty.columnMajorStream().toArray().length);
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            double[] col1 = m.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new double[] { 2.0, 5.0 }, col1, DELTA);
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_Shape2By2_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleStream stream = m.columnMajorStream(0, 1);
            double[] result = stream.toArray();
            assertArrayEquals(new double[] { 1.0, 3.0 }, result);
        }

        @Test
        public void testColumnMajorStream_BySingleColumnRange() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double sum = m.columnMajorStream(0, 1).sum();
            assertEquals(4.0, sum, 0.0);
        }

        @Test
        public void testVerticalStreamSingleColumnRange() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double[] column1 = matrix.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new double[] { 2.0, 4.0 }, column1);
        }

        @Test
        public void testColumnMajorStream_ZeroRowsKeepLogicalColumnRange() {
            final var matrix = DoubleMatrix.wrap(new double[0][0]).resize(0, 3);
            assertEquals(0, matrix.columnMajorStream(1, 3).count());
            assertEquals(2, matrix.columnStreams(1, 3).count());
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 3));
        }

        @Test
        public void testColumnMajorStream_WithRange_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 1));
        }
    }

    @Nested
    class RowStreamsTests {

        @Test
        public void testStreamR() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            List<double[]> rows = matrix.rowStreams().map(stream -> stream.toArray()).toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).length);
            assertEquals(1.0, rows.get(0)[0]);
            assertEquals(2.0, rows.get(0)[1]);
        }

        @Test
        public void testRowStreamsWithRange() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            List<double[]> rows = matrix.rowStreams(1, 3).map(stream -> stream.toArray()).toList();
            assertEquals(2, rows.size());

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(-1, 2));
        }

        @Test
        public void testRowStreams_Shape2By3_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            List<double[]> rows = m.rowStreams().map(DoubleStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, rows.get(0), DELTA);
            assertArrayEquals(new double[] { 4.0, 5.0, 6.0 }, rows.get(1), DELTA);
        }

        @Test
        public void testRowStreams_WithRange() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            List<double[]> rows = m.rowStreams(1, 3).map(DoubleStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new double[] { 4.0, 5.0, 6.0 }, rows.get(0), DELTA);
            assertArrayEquals(new double[] { 7.0, 8.0, 9.0 }, rows.get(1), DELTA);
        }

        @Test
        public void testRowStreams_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Stream<DoubleStream> stream = m.rowStreams();
            assertEquals(2, stream.count());
        }

        @Test
        public void testRowStreams_WithRange_Shape3By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            Stream<DoubleStream> stream = m.rowStreams(1, 3);
            assertEquals(2, stream.count());
        }

        @Test
        public void testRowStreams() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            long count = m.rowStreams().count();
            assertEquals(2, count);
        }

        @Test
        public void testRowStreams_WithRange_Shape3By2_CountsSelectedRows() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            long count = m.rowStreams(1, 3).count();
            assertEquals(2, count);
        }

        @Test
        public void testStreamsObserveLiveValuesAndRowStreamsKeepTheirSelectedBackingRow() {
            final DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1, 2 }, { 3, 4 } });
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
                    assertArrayEquals(new double[] { 9, 2 }, selectedRow.toArray());
                    assertArrayEquals(new double[] { 3, 9 }, selectedColumn.toArray());
                    assertArrayEquals(new double[] { 3, 4, 9, 2 }, rowMajor.toArray());
                    assertArrayEquals(new double[] { 3, 9, 4, 2 }, columnMajor.toArray());
                    assertArrayEquals(new double[] { 3, 2 }, mainDiagonal.toArray());
                    assertArrayEquals(new double[] { 4, 9 }, antiDiagonal.toArray());
                }
                try (var nextRow = rows.next();
                     var nextColumn = columns.next()) {
                    assertArrayEquals(new double[] { 9, 2 }, nextRow.toArray());
                    assertArrayEquals(new double[] { 4, 2 }, nextColumn.toArray());
                }
            }
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testRowStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d }, { 7.0d, 8.0d, 9.0d } });
            final double[][] expected = { new double[] { 1.0d, 2.0d, 3.0d }, new double[] { 4.0d, 5.0d, 6.0d }, new double[] { 7.0d, 8.0d, 9.0d } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowStreams()) {
                final var iterator = (ObjIteratorEx<DoubleStream>) stream.iterator();
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
            DoubleMatrix empty = DoubleMatrix.empty();
            assertEquals(0, empty.rowStreams().count());
        }

        @Test
        public void testRowStreams_WithRange_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(0, 3));
        }
    }

    @Nested
    class ColumnStreamsTests {

        @Test
        public void testStreamC() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            List<double[]> columnCount = matrix.columnStreams().map(stream -> stream.toArray()).toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).length);
            assertEquals(1.0, columnCount.get(0)[0]);
            assertEquals(3.0, columnCount.get(0)[1]);
        }

        @Test
        public void testColumnStreamsWithRange() {
            double[][] arr = { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            List<double[]> columnCount = matrix.columnStreams(1, 3).map(stream -> stream.toArray()).toList();
            assertEquals(2, columnCount.size());

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(-1, 2));
        }

        @Test
        public void testColumnStreams_Shape2By3_ArrayValues() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            List<double[]> columnCount = m.columnStreams().map(DoubleStream::toArray).toList();
            assertEquals(3, columnCount.size());
            assertArrayEquals(new double[] { 1.0, 4.0 }, columnCount.get(0), DELTA);
            assertArrayEquals(new double[] { 2.0, 5.0 }, columnCount.get(1), DELTA);
            assertArrayEquals(new double[] { 3.0, 6.0 }, columnCount.get(2), DELTA);
        }

        @Test
        public void testColumnStreams_WithRange() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            List<double[]> columnCount = m.columnStreams(1, 3).map(DoubleStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new double[] { 2.0, 5.0, 8.0 }, columnCount.get(0), DELTA);
            assertArrayEquals(new double[] { 3.0, 6.0, 9.0 }, columnCount.get(1), DELTA);
        }

        @Test
        public void testColumnStreams_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Stream<DoubleStream> stream = m.columnStreams();
            assertEquals(2, stream.count());
        }

        @Test
        public void testColumnStreams_WithRange_Shape2By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            Stream<DoubleStream> stream = m.columnStreams(1, 3);
            assertEquals(2, stream.count());
        }

        @Test
        public void testColumnStreams() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            long count = m.columnStreams().count();
            assertEquals(2, count);
        }

        @Test
        public void testColumnStreams_WithRange_Shape1By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } });
            long count = m.columnStreams(1, 3).count();
            assertEquals(2, count);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testColumnStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d }, { 7.0d, 8.0d, 9.0d } });
            final double[][] expected = { new double[] { 1.0d, 4.0d, 7.0d }, new double[] { 2.0d, 5.0d, 8.0d }, new double[] { 3.0d, 6.0d, 9.0d } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnStreams()) {
                final var iterator = (ObjIteratorEx<DoubleStream>) stream.iterator();
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
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d }, { 7.0d, 8.0d, 9.0d } });
            final double[] expected = new double[] { 2.0d, 5.0d, 8.0d };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (DoubleIteratorEx) values.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextDouble);
            }
        }

        @Test
        public void testColumnStreams_InnerCountConsumesOnlyRemainingValues() {
            final var matrix = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d, 3.0d }, { 4.0d, 5.0d, 6.0d }, { 7.0d, 8.0d, 9.0d } });
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (DoubleIteratorEx) values.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(2.0d, iterator.nextDouble());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextDouble);
            }
        }

        @Test
        public void testColumnStreams_Empty() {
            DoubleMatrix empty = DoubleMatrix.empty();
            assertEquals(0, empty.columnStreams().count());
        }

        @Test
        public void testColumnStreams_WithRange_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(0, 3));
        }
    }

    @Nested
    class ForEachTests {

        @Test
        public void testForEach() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] sum = { 0.0 };
            matrix.forEach(val -> sum[0] += val);
            assertEquals(10.0, sum[0]);
        }

        @Test
        public void testForEachWithRange() {
            double[][] arr = { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            double[] sum = { 0.0 };
            matrix.forEach(0, 2, 1, 3, val -> sum[0] += val);
            assertEquals(16.0, sum[0]); // 2 + 3 + 5 + 6

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.forEach(-1, 2, 0, 2, val -> {
            }));
        }

        @Test
        public void testForEach_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            List<Double> values = new ArrayList<>();
            m.forEach(v -> values.add(v));
            assertEquals(9, values.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1.0, values.get(i), DELTA);
            }
        }

        @Test
        public void testForEach_WithBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            List<Double> values = new ArrayList<>();
            m.forEach(1, 3, 1, 3, v -> values.add(v));
            assertEquals(4, values.size());
            assertEquals(5.0, values.get(0), DELTA);
            assertEquals(6.0, values.get(1), DELTA);
            assertEquals(8.0, values.get(2), DELTA);
            assertEquals(9.0, values.get(3), DELTA);
        }

        @Test
        public void testForEach_LargeMatrix() {
            // Test forEach with a large matrix to potentially trigger parallel execution path
            // Create a 100x100 matrix (10,000 elements should exceed parallel threshold)
            double[][] data = new double[100][100];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    data[i][j] = i * 100 + j;
                }
            }
            DoubleMatrix m = DoubleMatrix.wrap(data);

            List<Double> values = new ArrayList<>();
            m.forEach(v -> {
                synchronized (values) {
                    values.add(v);
                }
            });

            assertEquals(10000, values.size());
            // Verify sum to ensure all elements were processed
            double sum = values.stream().mapToDouble(Double::doubleValue).sum();
            assertEquals(49995000.0, sum, 1.0); // Sum of 0 to 9999
        }

        @Test
        public void testForEach_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            final double[] sum = { 0.0 };
            m.forEach(val -> sum[0] += val);
            assertEquals(10.0, sum[0]);
        }

        @Test
        public void testForEach_WithRange() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            final double[] sum = { 0.0 };
            m.forEach(1, 3, 1, 3, val -> sum[0] += val);
            assertEquals(28.0, sum[0]);
        }

        @Test
        public void testForEach_Shape2By2_CountsVisitedElements() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEach(x -> count.incrementAndGet());
            assertEquals(4, count.get());
        }

        @Test
        public void testForEach_WithRange_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEach(1, 3, 1, 3, x -> count.incrementAndGet());
            assertEquals(4, count.get());
        }

        @Test
        public void testForEachStopsAtTheFailingElementInSequentialMode() {
            final DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            final IllegalStateException failure = new IllegalStateException("stop at the second element");
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                for (final boolean selectedRegion : new boolean[] { false, true }) {
                    final List<Double> visited = new java.util.ArrayList<>();
                    final Throwables.DoubleConsumer<RuntimeException> action = value -> {
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
                    assertEquals(selectedRegion ? List.of(2d, 3d) : List.of(1d, 2d), visited);
                }
            });
        }

        @ParameterizedTest(name = "{0}: bounded traversal")
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testForEach_SubregionVisitsEachSelectedCellOnce(final ParallelMode mode) {
            final DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            final java.util.concurrent.ConcurrentLinkedQueue<Double> visited = new java.util.concurrent.ConcurrentLinkedQueue<>();

            Matrices.runWithParallelMode(mode, () -> matrix.forEach(0, 2, 1, 3, value -> visited.add(value)));

            final java.util.List<Double> sorted = new java.util.ArrayList<>(visited);
            java.util.Collections.sort(sorted);
            assertEquals(java.util.List.of(2.0, 3.0, 5.0, 6.0), sorted);
            assertArrayEquals(new double[] { 1, 2, 3 }, matrix.rowCopy(0));
            assertArrayEquals(new double[] { 4, 5, 6 }, matrix.rowCopy(1));
            assertEquals(ParallelMode.FORCE_OFF, Matrices.getParallelMode());
        }

        @Test
        public void testForEachNullAction() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            assertThrows(IllegalArgumentException.class, () -> matrix.forEach((Throwables.DoubleConsumer<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.forEach(0, 1, 0, 2, (Throwables.DoubleConsumer<RuntimeException>) null));
        }

        // Test sum
        @Test
        public void testForEach_WithBounds_OutOfBounds() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(-1, 2, 0, 2, v -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 3, 0, 2, v -> {
            }));
        }
    }

    @Nested
    class ToMultilineStringTests {

        @Test
        public void testToMultilineString_Shape2By2_NonemptyRendering() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            String result = m.toMultilineString();
            assertNotNull(result);
            assertTrue(result.length() > 0);
        }

        @Test
        public void testToMultilineString_Empty() {
            DoubleMatrix m = DoubleMatrix.empty();
            String result = m.toMultilineString();
            assertNotNull(result);
        }

        // copyOf() clones each row, so the same values are accepted and stay independent.
        @Test
        public void testToMultilineStringNonEmptyReturnsNonNullFormattedString() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains("1.0"));
            assertTrue(printed.contains("4.0"));
            assertFalse(printed.equals("null"));
        }

        @Test
        public void testToMultilineStringEmptyReturnsBracketsLiteral() {
            DoubleMatrix empty = DoubleMatrix.empty();
            String printed = empty.toMultilineString();
            assertEquals("[]", printed);
        }

        @Test
        public void testToMultilineStringWithNaNAndInfinityRendersAsTokens() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY } });
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
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix1 = DoubleMatrix.wrap(arr);
            DoubleMatrix matrix2 = DoubleMatrix.wrap(arr);

            assertEquals(matrix1.hashCode(), matrix2.hashCode());
        }

        @Test
        public void testHashCode_Shape2By2() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m3 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 4.0, 3.0 } });

            assertEquals(m1.hashCode(), m2.hashCode());
            assertNotEquals(m1.hashCode(), m3.hashCode()); // Usually different
        }

        @Test
        public void testHashCode_Shape2By2_EqualMatricesHaveEqualHashCodes() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        public void testEqualsAndHashCode_NaN_AreConsidered_Equal() {
            // DoubleMatrix.equals/hashCode use Double.doubleToLongBits semantics (per Javadoc),
            // so NaN == NaN, and the hash code must be consistent with equals.
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { Double.NaN, 1.0 }, { 2.0, Double.NaN } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { Double.NaN, 1.0 }, { 2.0, Double.NaN } });

            assertEquals(m1, m2, "Two matrices with NaN values at the same positions must be equal");
            assertEquals(m1.hashCode(), m2.hashCode(), "Equal matrices must produce equal hash codes (NaN content)");
        }

        @Test
        public void testEqualsAndHashCode_NegativeZero_DistinctFromPositiveZero() {
            // DoubleMatrix.equals/hashCode use Double.doubleToLongBits semantics (per Javadoc),
            // so +0.0 and -0.0 are NOT equal, and the hash codes must differ accordingly.
            DoubleMatrix posZero = DoubleMatrix.wrap(new double[][] { { 0.0 } });
            DoubleMatrix negZero = DoubleMatrix.wrap(new double[][] { { -0.0 } });

            assertNotEquals(posZero, negZero, "+0.0 and -0.0 must not be equal under Double.doubleToLongBits semantics");
            assertNotEquals(posZero.hashCode(), negZero.hashCode(), "+0.0 and -0.0 must produce different hash codes under Double.doubleToLongBits semantics");
        }
    }

    @Nested
    class EqualsTests {

        // Test sum operation on streams
        @Test
        public void testEquals() {
            double[][] arr1 = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            double[][] arr2 = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            double[][] arr3 = { { 1.0, 2.0 }, { 3.0, 5.0 } };

            DoubleMatrix matrix1 = DoubleMatrix.wrap(arr1);
            DoubleMatrix matrix2 = DoubleMatrix.wrap(arr2);
            DoubleMatrix matrix3 = DoubleMatrix.wrap(arr3);

            assertTrue(matrix1.equals(matrix1));
            assertTrue(matrix1.equals(matrix2));
            assertFalse(matrix1.equals(matrix3));
            assertFalse(matrix1.equals(null));
            assertFalse(matrix1.equals("not a matrix"));
        }

        @Test
        public void testEquals_Same() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertEquals(m1, m2);
        }

        @Test
        public void testEquals_Different() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 5.0 } });
            assertNotEquals(m1, m2);
        }

        @Test
        public void testEquals_DifferentRowCountsWithEqualColumnCounts() {
            final var longer = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d }, { 3.0d, 4.0d } });
            final var shorter = DoubleMatrix.wrap(new double[][] { { 1.0d, 2.0d } });
            assertFalse(longer.equals(shorter));
            assertFalse(shorter.equals(longer));
        }

        @Test
        public void testEquals_Shape2By2_NullInput() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m3 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 4.0, 3.0 } });
            DoubleMatrix m4 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });

            assertTrue(m1.equals(m1)); // Same object
            assertTrue(m1.equals(m2)); // Same values
            assertFalse(m1.equals(m3)); // Different values
            assertFalse(m1.equals(m4)); // Different dimensions
            assertFalse(m1.equals(null));
            assertFalse(m1.equals("not a matrix"));
        }

        @Test
        public void testEquals_Shape2By2_NullInput_ReflexivityDifferentValuesNullAndType() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m3 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 5.0 } });
            assertTrue(m1.equals(m2));
            assertFalse(m1.equals(m3));
            assertTrue(m1.equals(m1));
            assertFalse(m1.equals(null));
            assertFalse(m1.equals("not a matrix"));
        }

        @Test
        public void testEquals_Null() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertNotEquals(m1, null);
        }
    }

    @Nested
    class ToStringTests {

        @Test
        public void testToString() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            String str = matrix.toString();
            assertNotNull(str);
            assertTrue(str.contains("1.0"));
            assertTrue(str.contains("4.0"));
        }

        @Test
        public void testToString_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1"));
            assertTrue(str.contains("2"));
            assertTrue(str.contains("3"));
            assertTrue(str.contains("4"));
        }

        @Test
        public void testToString_Shape2By2_ContainsElementText() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1.0"));
            assertTrue(str.contains("4.0"));
        }

        @Test
        public void testDoubleMatrixToString() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            assertEquals("[[1.0, 2.0, 3.0], [4.0, 5.0, 6.0]]", matrix.toString());
        }

        @Test
        public void testToString_Shape2By2_NonemptyRendering() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.length() > 0);
        }
    }

    @Nested
    class ElementTypeTests {

        @Test
        public void testElementType_Shape1By1() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0 } });
            assertEquals(double.class, m.elementType());
        }
    }

    @Nested
    class UnsafeBackingArrayTests {

        @Test
        public void testArray() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            double[][] array = m.unsafeBackingArray();
            assertArrayEquals(new double[] { 1.0, 2.0 }, array[0]);
            assertArrayEquals(new double[] { 3.0, 4.0 }, array[1]);
        }
    }

    @Nested
    class ElementCountTests {

        @Test
        public void testElementCount() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            assertEquals(6, m.elementCount());
        }

        @Test
        public void testElementCount_Empty() {
            DoubleMatrix m = DoubleMatrix.empty();
            assertEquals(0, m.elementCount());
        }
    }

    @Nested
    class IsEmptyTests {

        @Test
        public void testIsEmpty() {
            DoubleMatrix empty = DoubleMatrix.empty();
            assertTrue(empty.isEmpty());
            DoubleMatrix notEmpty = DoubleMatrix.wrap(new double[][] { { 1.0 } });
            assertFalse(notEmpty.isEmpty());
        }
    }

    @Nested
    class IsSameShapeTests {

        @Test
        public void testIsSameShape() {
            DoubleMatrix m1 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            DoubleMatrix m2 = DoubleMatrix.wrap(new double[][] { { 5.0, 6.0 }, { 7.0, 8.0 } });
            DoubleMatrix m3 = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 } });
            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }
    }

    @Nested
    class ReshapeTests {

        @Test
        public void testReshape() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            DoubleMatrix reshaped = matrix.reshape(1, 4);
            assertEquals(1, reshaped.rowCount());
            assertEquals(4, reshaped.columnCount());
            assertEquals(1.0, reshaped.get(0, 0));
            assertEquals(2.0, reshaped.get(0, 1));
            assertEquals(3.0, reshaped.get(0, 2));
            assertEquals(4.0, reshaped.get(0, 3));

            // Test reshape with too-small dimensions throws exception
            assertThrows(IllegalArgumentException.class, () -> matrix.reshape(1, 3));
            assertThrows(IllegalArgumentException.class, () -> matrix.reshape(1, 1));
        }

        @Test
        public void testReshape_Shape3By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            DoubleMatrix reshaped = m.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1.0, reshaped.get(0, i), DELTA);
            }
        }

        @Test
        public void testReshape_Back() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 }, { 7.0, 8.0, 9.0 } });
            DoubleMatrix reshaped = m.reshape(1, 9);
            DoubleMatrix reshapedBack = reshaped.reshape(3, 3);
            assertEquals(m, reshapedBack);
        }

        @Test
        public void testReshape_Shape1By6() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 } });
            DoubleMatrix reshaped = m.reshape(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals(1.0, reshaped.get(0, 0));
            assertEquals(4.0, reshaped.get(1, 0));
        }

        @Test
        public void testReshape_Shape2By3() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            DoubleMatrix reshaped = m.reshape(3, 2);
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1.0, reshaped.get(0, 0), 0.0);
            assertEquals(2.0, reshaped.get(0, 1), 0.0);
            assertEquals(3.0, reshaped.get(1, 0), 0.0);
            assertEquals(4.0, reshaped.get(1, 1), 0.0);
        }

        @Test
        public void testReshape_OversizedShapeThrowsIllegalArgumentException() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0 } });
            assertThrows(IllegalArgumentException.class, () -> m.reshape(46341, 46341));
        }
    }

    @Nested
    class ReshapeAndPadToColumnCountTests {

        @Test
        public void testReshapeAndPadToColumnCount() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 } });
            DoubleMatrix reshaped = m.reshapeAndPadToColumnCount(3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }
    }

    @Nested
    class ForEachIndicesTests {

        @Test
        public void testForEach_BiConsumer() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            final int[] count = { 0 };
            m.forEachIndices((i, j) -> count[0]++);
            assertEquals(4, count[0]);
        }

        @Test
        public void testForEach_BiObjConsumer() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            final int[] count = { 0 };
            m.forEachIndices((i, j, matrix) -> count[0]++);
            assertEquals(4, count[0]);
        }
    }

    @Nested
    class Adjacent4PointsTests {

        @Test
        public void testAdjacent4Points() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = m.adjacent4Points(1, 1).toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testAdjacent4Points_Corner() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.adjacent4Points(0, 0).toList();
            assertEquals(2, points.size());
        }
    }

    @Nested
    class Adjacent8PointsTests {

        @Test
        public void testAdjacent8Points() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = m.adjacent8Points(1, 1).toList();
            assertEquals(8, points.size());
        }

        @Test
        public void testAdjacent8Points_Corner() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.adjacent8Points(0, 0).toList();
            assertEquals(3, points.size());
        }
    }

    @Nested
    class MainDiagonalPointsTests {

        @Test
        public void testMainDiagonalPoints() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Stream<Point> points = m.mainDiagonalPoints();
            assertEquals(2, points.count());
        }
    }

    @Nested
    class AntiDiagonalPointsTests {

        @Test
        public void testAntiDiagonalPoints() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Stream<Point> points = m.antiDiagonalPoints();
            assertEquals(2, points.count());
        }
    }

    @Nested
    class RowMajorPointsTests {

        @Test
        public void testRowMajorPoints() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Stream<Point> points = m.rowMajorPoints();
            assertEquals(4, points.count());
        }

        @Test
        public void testRowMajorPoints_WithRow() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Stream<Point> points = m.rowMajorPoints(1, 1 + 1);
            assertEquals(2, points.count());
        }

        @Test
        public void testRowMajorPoints_WithRange() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 }, { 5.0, 6.0 } });
            Stream<Point> points = m.rowMajorPoints(1, 3);
            assertEquals(4, points.count());
        }
    }

    @Nested
    class ColumnMajorPointsTests {

        @Test
        public void testColumnMajorPoints() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Stream<Point> points = m.columnMajorPoints();
            assertEquals(4, points.count());
        }

        @Test
        public void testColumnMajorPoints_WithColumn() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Stream<Point> points = m.columnMajorPoints(1, 1 + 1);
            assertEquals(2, points.count());
        }

        @Test
        public void testColumnMajorPoints_WithRange() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0, 3.0 }, { 4.0, 5.0, 6.0 } });
            Stream<Point> points = m.columnMajorPoints(1, 3);
            assertEquals(4, points.count());
        }
    }

    @Nested
    class RowPointsTests {

        @Test
        public void testRowPoints() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Stream<Stream<Point>> points = m.rowPoints();
            assertEquals(2, points.count());
        }
    }

    @Nested
    class ColumnPointsTests {

        @Test
        public void testColumnPoints() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            Stream<Stream<Point>> points = m.columnPoints();
            assertEquals(2, points.count());
        }
    }

    @Nested
    class AcceptTests {

        @Test
        public void testAccept() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            final boolean[] called = { false };
            m.accept(matrix -> called[0] = true);
            assertTrue(called[0]);
        }
    }

    @Nested
    class ApplyTests {

        @Test
        public void testApply() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            int result = m.apply(matrix -> matrix.rowCount() * matrix.columnCount());
            assertEquals(4, result);
        }
    }

    @Nested
    class PrintlnTests {

        @Test
        public void testPrintln() {
            double[][] arr = { { 1.0, 2.0 }, { 3.0, 4.0 } };
            DoubleMatrix matrix = DoubleMatrix.wrap(arr);

            assertFalse(matrix.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> matrix.println());
        }

        @Test
        public void testPrintln_Shape2By2() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertFalse(m.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> m.println());

            DoubleMatrix empty = DoubleMatrix.empty();
            assertTrue(empty.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> empty.println());
        }
    }
}
