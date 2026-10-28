package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u;

class MatrixTest extends MatrixTestSupport {

    private static void assertCountConsumes(final Stream<?> stream, final long expectedCount) {
        final java.util.Iterator<?> rawIterator = stream.iterator();
        assertTrue(rawIterator instanceof ObjIteratorEx);
        final ObjIteratorEx<?> iterator = (ObjIteratorEx<?>) rawIterator;
        assertEquals(expectedCount, iterator.count());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    private static int[] toIntArray(final List<Integer> list) {
        final int[] a = new int[list.size()];

        for (int i = 0; i < list.size(); i++) {
            a[i] = list.get(i);
        }

        return a;
    }

    private static void assertTransformParity(final java.util.function.Supplier<IntMatrix> intTransform,
            final java.util.function.Supplier<Matrix<Integer>> genericTransform) {
        try {
            final IntMatrix intResult = intTransform.get();
            final Matrix<Integer> genericResult = genericTransform.get();
            assertMatrixEquals(intResult, genericResult);
        } catch (final IllegalArgumentException e) {
            try {
                genericTransform.get();
            } catch (final IllegalArgumentException expected) {
                return;
            }

            throw e;
        }
    }

    private static void assertMatrixEquals(final IntMatrix intMatrix, final Matrix<Integer> genericMatrix) {
        assertEquals(intMatrix.rowCount(), genericMatrix.rowCount());
        assertEquals(intMatrix.columnCount(), genericMatrix.columnCount());

        for (int i = 0; i < intMatrix.rowCount(); i++) {
            for (int j = 0; j < intMatrix.columnCount(); j++) {
                assertEquals(intMatrix.get(i, j), genericMatrix.get(i, j).intValue());
            }
        }
    }

    private static void assertShape(final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int rows, final int columns) {
        assertEquals(rows, matrix.rowCount());
        assertEquals(columns, matrix.columnCount());
    }

    @Nested
    class ConstructorTests {

        @Test
        public void testConstructor() {
            Integer[][] data = { { 1, 2, 3 }, { 4, 5, 6 } };
            Matrix<Integer> matrix = new Matrix<>(data);

            Assertions.assertEquals(2, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals(6, matrix.elementCount());
        }

        @Test
        public void testConstructorWithNonRectangularArray() {
            Integer[][] data = { { 1, 2 }, { 3, 4, 5 } };

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                new Matrix<>(data);
            });
        }

        @Test
        public void testGenericTypeWithCustomObject() {
            class CustomObject {
                String value;

                CustomObject(String value) {
                    this.value = value;
                }

                @Override
                public String toString() {
                    return value;
                }
            }

            CustomObject[][] data = { { new CustomObject("A"), new CustomObject("B") }, { new CustomObject("C"), new CustomObject("D") } };
            Matrix<CustomObject> m = Matrix.wrap(data);

            assertEquals("A", m.get(0, 0).value);
            assertEquals("D", m.get(1, 1).value);
        }

        @Test
        public void testConstructor_WithValidStringArray() {
            String[][] arr = { { "A", "B" }, { "C", "D" } };
            Matrix<String> m = new Matrix<>(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals("A", m.get(0, 0));
            assertEquals("D", m.get(1, 1));
        }

        @Test
        public void testConstructor_WithValidIntegerArray() {
            Integer[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            Matrix<Integer> m = new Matrix<>(arr);
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(6, m.get(1, 2));
        }

        @Test
        public void testConstructor_WithBigDecimal() {
            BigDecimal[][] arr = { { BigDecimal.ONE, BigDecimal.TEN }, { BigDecimal.ZERO, new BigDecimal("3.14") } };
            Matrix<BigDecimal> m = new Matrix<>(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(BigDecimal.ONE, m.get(0, 0));
            assertEquals(new BigDecimal("3.14"), m.get(1, 1));
        }

        @Test
        public void testConstructor_WithValidArray() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals("a", m.get(0, 0));
            assertEquals("d", m.get(1, 1));
        }

        @Test
        public void testConstructorWithEmptyArray() {
            Integer[][] data = new Integer[0][0];
            Matrix<Integer> matrix = new Matrix<>(data);

            Assertions.assertEquals(0, matrix.rowCount());
            Assertions.assertEquals(0, matrix.columnCount());
            Assertions.assertEquals(0, matrix.elementCount());
        }

        @Test
        public void testConstructor_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> new Matrix<>(null));
        }

        @Test
        public void testConstructor_WithSingleElement() {
            Matrix<String> m = new Matrix<>(new String[][] { { "X" } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals("X", m.get(0, 0));
        }

        @Test
        public void testConstructor_WithNullElements() {
            String[][] arr = { { "A", null }, { null, "D" } };
            Matrix<String> m = new Matrix<>(arr);
            assertEquals("A", m.get(0, 0));
            assertNull(m.get(0, 1));
            assertNull(m.get(1, 0));
            assertEquals("D", m.get(1, 1));
        }

        @Test
        public void testConstructor_WithNullElements_Shape2By2_NullInput() {
            Matrix<String> m = new Matrix<>(new String[][] { { null, "B" }, { "C", null } });
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertNull(m.get(0, 0));
            assertEquals("B", m.get(0, 1));
        }

        @Test
        public void testConstructor_WithEmptyArray() {
            Matrix<String> m = new Matrix<>(new String[0][0]);
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
            assertTrue(m.isEmpty());
        }

        @Test
        public void testConstructor_WithSingleElement_Shape1By1() {
            Matrix<Integer> m = new Matrix<>(new Integer[][] { { 42 } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42, m.get(0, 0));
        }

        @Test
        public void testConstructor_WithNullArray_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> new Matrix<>((String[][]) null));
        }
    }

    @Nested
    class EmptyTests {

        @Test
        public void testEmptyColumnAndDiagonalCopiesHaveIndependentArrayIdentities() {
            final Matrix<String> noRows = Matrix.empty(String.class, 3);
            final Matrix<String> noColumns = Matrix.copyOf(String.class, new String[3][0]);

            Assertions.assertAll(() -> assertNotSame(noRows.columnCopy(1), noRows.columnCopy(1), "columnCopy must return a new empty array"),
                    () -> assertNotSame(noRows.mainDiagonalCopy(), noRows.mainDiagonalCopy(), "mainDiagonalCopy must return a new empty array"),
                    () -> assertNotSame(noRows.antiDiagonalCopy(), noRows.antiDiagonalCopy(), "antiDiagonalCopy must return a new empty array"),
                    () -> assertNotSame(noColumns.mainDiagonalCopy(), noColumns.mainDiagonalCopy(), "Nx0 main-diagonal copies must be distinct"),
                    () -> assertNotSame(noColumns.antiDiagonalCopy(), noColumns.antiDiagonalCopy(), "Nx0 anti-diagonal copies must be distinct"));

            for (String[] copy : List.of(noRows.columnCopy(1), noRows.mainDiagonalCopy(), noRows.antiDiagonalCopy(), noColumns.mainDiagonalCopy(),
                    noColumns.antiDiagonalCopy())) {
                assertEquals(0, copy.length);
                assertEquals(String[].class, copy.getClass());
            }

            final Matrix<Object> objectElements = Matrix.empty(Object.class, 1);
            final Matrix<String[]> arrayElements = Matrix.empty(String[].class, 1);
            assertEquals(Object[].class, objectElements.columnCopy(0).getClass());
            assertNotSame(objectElements.columnCopy(0), objectElements.columnCopy(0));
            assertEquals(0, arrayElements.columnCopy(0).length);
            assertEquals(String[][].class, arrayElements.columnCopy(0).getClass());
            assertNotSame(arrayElements.columnCopy(0), arrayElements.columnCopy(0));
        }

        @Test
        public void testEmpty() {
            Matrix<String> empty = Matrix.empty(String.class);
            assertTrue(empty.isEmpty());
            assertEquals(0, empty.rowCount());
            assertEquals(0, empty.columnCount());
            assertEquals(String.class, empty.elementType());
            assertEquals(Object.class, Matrix.empty().elementType());
        }

        @Test
        public void testEmptyWithClassToken_SharedInstanceOnlyForObjectZeroColumns() {
            assertTrue(Matrix.empty(Object.class) == Matrix.empty());
            assertTrue(Matrix.empty(Object.class, 0) == Matrix.empty());

            final Matrix<Object> objectZeroByTwo = Matrix.empty(Object.class, 2);
            assertNotSame(Matrix.empty(), objectZeroByTwo);
            assertEquals(2, objectZeroByTwo.columnCount());

            final Matrix<String> strings = Matrix.empty(String.class);
            assertNotSame(Matrix.empty(), strings);
            assertNotSame(strings, Matrix.empty(String.class));
            assertEquals(String.class, strings.elementType());
        }

        @Test
        public void testEmptyStreamRangesValidateIndexesAndPreserveLogicalRowColumnStreams() {
            final Matrix<String> matrix = Matrix.wrap(String.class, new String[2][3]);
            assertEquals(0, matrix.rowMajorStream(1, 1).count());
            assertEquals(0, matrix.columnMajorStream(2, 2).count());
            assertEquals(0, matrix.rowStreams(1, 1).count());
            assertEquals(0, matrix.columnStreams(2, 2).count());
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(4, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(-1, -1));

            final Matrix<String> noColumns = Matrix.wrap(String.class, new String[3][0]);
            final Matrix<String> noRows = Matrix.empty(String.class, 3);
            assertEquals(0, noColumns.mainDiagonalStream().count());
            assertEquals(0, noRows.mainDiagonalStream().count());
            final List<Stream<String>> rowStreams = noColumns.rowStreams().toList();
            final List<Stream<String>> columnStreams = noRows.columnStreams().toList();
            assertEquals(3, rowStreams.size());
            assertEquals(3, columnStreams.size());
            for (final Stream<String> stream : rowStreams) {
                assertEquals(0, stream.count());
            }
            for (final Stream<String> stream : columnStreams) {
                assertEquals(0, stream.count());
            }
        }

        @Test
        public void testEmpty_RejectsNegativeLogicalWidth() {
            assertThrows(IllegalArgumentException.class, () -> Matrix.empty(String.class, -1));
        }
    }

    @Nested
    class WrapTests {

        @Test
        public void testWrappingSnapshotsOuterArrayButKeepsRowsLive() {
            Integer[] first = { 1, 2 };
            Integer[] second = { 3, 4 };
            Integer[][] source = { first, second };
            Matrix<Integer> matrix = Matrix.wrap(Integer.class, source);

            source[0] = new Integer[] { 9, 9 };
            assertEquals(1, matrix.get(0, 0));

            Integer[][] snapshot = matrix.unsafeBackingArray();
            snapshot[1] = new Integer[] { 8, 8 };
            assertEquals(3, matrix.get(1, 0));

            first[0] = 7;
            assertEquals(7, matrix.get(0, 0));
            snapshot[0][1] = 6;
            assertEquals(6, matrix.get(0, 1));
        }

        @Test
        public void testWrap() {
            Integer[][] data = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            Assertions.assertEquals(2, matrix.rowCount());
            Assertions.assertEquals(2, matrix.columnCount());
            Assertions.assertEquals(1, matrix.get(0, 0));
        }

        @Test
        public void testWrapVarargs() {
            Matrix<String> matrix = Matrix.wrap(new String[] { "a", "b" }, new String[] { "c", "d" });

            Assertions.assertEquals(2, matrix.rowCount());
            Assertions.assertEquals(2, matrix.columnCount());
            Assertions.assertEquals("a", matrix.get(0, 0));
        }

        @Test
        public void testWrap_Get() {
            String[][] data = { { "a", "b" }, { "c", "d" } };
            Matrix<String> matrix = Matrix.wrap(data);
            assertEquals("c", matrix.get(1, 0));
        }

        @Test
        public void testMatrixWrapGet() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(2, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertEquals(Integer.valueOf(1), matrix.get(0, 0));
            assertEquals(Integer.valueOf(6), matrix.get(1, 2));
        }

        @Test
        public void testWrap_WithVarargs() {
            Matrix<Integer> m = Matrix.wrap(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 });
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(Integer.valueOf(1), m.get(0, 0));
            assertEquals(Integer.valueOf(6), m.get(1, 2));
        }

        @Test
        public void testLargeMatrix() {
            Integer[][] data = new Integer[100][100];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    data[i][j] = i * 100 + j;
                }
            }
            Matrix<Integer> m = Matrix.wrap(data);

            assertEquals(100, m.rowCount());
            assertEquals(100, m.columnCount());
            assertEquals(Integer.valueOf(9999), m.get(99, 99));
        }

        @Test
        public void testGenericTypeWithList() {
            List<String> list1 = Arrays.asList("a", "b");
            List<String> list2 = Arrays.asList("c", "d");
            List<String> list3 = Arrays.asList("e", "f");
            List<String> list4 = Arrays.asList("g", "h");

            @SuppressWarnings("unchecked")
            Matrix<List<String>> m = Matrix.wrap(new List[] { list1, list2 }, new List[] { list3, list4 });

            assertEquals(list1, m.get(0, 0));
            assertEquals(list4, m.get(1, 1));
        }

        @Test
        public void testWrap_WithValidStringArray() {
            String[][] arr = { { "A", "B" }, { "C", "D" } };
            Matrix<String> m = Matrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals("A", m.get(0, 0));
        }

        @Test
        public void testWrap_WithValidDoubleArray() {
            Double[][] arr = { { 1.5, 2.5 }, { 3.5, 4.5 } };
            Matrix<Double> m = Matrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1.5, m.get(0, 0));
            assertEquals(4.5, m.get(1, 1));
        }

        @Test
        public void testWrap_WithValidArray() {
            String[][] arr = { { "x", "y", "z" }, { "a", "b", "c" } };
            Matrix<String> m = Matrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals("x", m.get(0, 0));
            assertEquals("c", m.get(1, 2));
        }

        @Test
        public void testWrap_WithNullArray() {
            Matrix<String> m = Matrix.wrap(new String[][] {});
            assertTrue(m.isEmpty());

            assertThrows(IllegalArgumentException.class, () -> Matrix.wrap((String[][]) null));
        }

        @Test
        public void testWrap_WithEmptyArray() {
            Matrix<String> m = Matrix.wrap(new String[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testWrap_WithSingleRow() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "one", "two", "three" } });
            assertEquals(1, m.rowCount());
            assertEquals(3, m.columnCount());
        }

        @Test
        public void testWrap_WithSingleColumn() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a" }, { "b" }, { "c" } });
            assertEquals(3, m.rowCount());
            assertEquals(1, m.columnCount());
        }

        @Test
        public void testWrapRejectsAliasedLogicalRows() {
            Integer[] sharedRow = { 1, 2 };
            assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(Integer.class, sharedRow, sharedRow));
            assertThrows(IllegalArgumentException.class, () -> new Matrix<>(new Integer[][] { sharedRow, sharedRow }));
        }
    }

    @Nested
    class CopyOfTests {

        @Test
        public void testCopyOf() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = Matrix.copyOf(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals("a", m.get(0, 0));
            assertEquals("d", m.get(1, 1));

            // Mutating the source array must not affect the copy
            arr[0][0] = "x";
            assertEquals("a", m.get(0, 0));

            // Mutating the copy must not affect the source array
            m.set(1, 1, "z");
            assertEquals("d", arr[1][1]);

            // A zero-row input is copied too, including its outer array and runtime type.
            Integer[][] emptySource = new Integer[0][0];
            Matrix<Integer> emptyCopy = Matrix.copyOf(emptySource);
            assertTrue(emptyCopy.isEmpty());
            assertNotSame(emptySource, emptyCopy.unsafeBackingArray());
            assertEquals(Integer[][].class, emptyCopy.unsafeBackingArray().getClass());
            assertEquals(Integer.class, emptyCopy.elementType());

            // null -> IllegalArgumentException (consistent with Matrix.wrap(null))
            assertThrows(IllegalArgumentException.class, () -> Matrix.copyOf((String[][]) null));

            // non-rectangular -> IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> Matrix.copyOf(new String[] { "a" }, new String[] { "b", "c" }));
        }

        @Test
        public void testCopyOfDetachesAliasedSourceRows() {
            Integer[] sharedRow = { 1, 2 };
            Matrix<Integer> m = Matrix.copyOf(Integer.class, sharedRow, sharedRow);
            final int[] calls = { 0 };

            m.updateAll(value -> {
                calls[0]++;
                return value + 10;
            });

            assertEquals(4, calls[0]);
            assertEquals(List.of(11, 12, 11, 12), m.flatten());
            assertArrayEquals(new Integer[] { 1, 2 }, sharedRow);
        }

        @Test
        public void testCopyOf_ExplicitTypeRejectsNullFirstRow() {
            assertThrows(IllegalArgumentException.class, () -> Matrix.copyOf(String.class, new String[][] { null }));
        }
    }

    @Nested
    class OfMainDiagonalTests {

        @Test
        public void testOfMainDiagonal_CreatesMainDiagonal() {
            String[] diag = { "A", "B", "C" };
            Matrix<String> m = Matrix.ofMainDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals("A", m.get(0, 0));
            assertEquals("B", m.get(1, 1));
            assertEquals("C", m.get(2, 2));
            assertNull(m.get(0, 1));
            assertNull(m.get(1, 0));
        }

        @Test
        public void testDiagonalTypeIsFullyValidatedBeforeTheLaterArrayArgument() {
            assertEquals("Element type cannot be void.class",
                    assertThrows(IllegalArgumentException.class, () -> Matrix.ofMainDiagonal(void.class, null)).getMessage());
            assertEquals("Element type cannot be void.class",
                    assertThrows(IllegalArgumentException.class, () -> Matrix.ofAntiDiagonal(void.class, null)).getMessage());
        }

        @Test
        public void testDiagonalLU2RD() {
            Integer[] diagonal = { 1, 2, 3 };
            Matrix<Integer> matrix = Matrix.ofMainDiagonal(diagonal);

            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(1, 1));
            Assertions.assertEquals(3, matrix.get(2, 2));
            Assertions.assertNull(matrix.get(0, 1));
            Assertions.assertNull(matrix.get(1, 0));
        }

        @Test
        public void testDiagonalLU2RD_Values() {
            Matrix<Integer> m = Matrix.ofMainDiagonal(new Integer[] { 1, 2, 3 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(Integer.valueOf(1), m.get(0, 0));
            assertEquals(Integer.valueOf(2), m.get(1, 1));
            assertEquals(Integer.valueOf(3), m.get(2, 2));
            assertNull(m.get(0, 1));
            assertNull(m.get(1, 0));
        }

        @Test
        public void testDiagonalLU2RD_Integers() {
            Integer[] diag = { 1, 2, 3 };
            Matrix<Integer> m = Matrix.ofMainDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
            assertNull(m.get(0, 1));
            assertNull(m.get(1, 0));
        }

        @Test
        public void testDiagonalLU2RD_EmptyArray() {
            Matrix<String> m = Matrix.ofMainDiagonal(new String[0]);
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testElementTypeVoid_RejectedWithDescriptiveMessage() {
            final Matrix<Integer> m = Matrix.wrap(Integer.class, new Integer[][] { { 1, 2 }, { 3, 4 } });
            final List<org.junit.jupiter.api.function.Executable> calls = List.of( //
                    () -> Matrix.empty(void.class), //
                    () -> Matrix.empty(void.class, 2), //
                    () -> Matrix.wrap(void.class, new Void[0][0]), //
                    () -> new Matrix<>(void.class, new Void[0][0]), //
                    () -> Matrix.copyOf(void.class, new Void[][] { { null } }), //
                    () -> Matrix.ofMainDiagonal(void.class, new Void[] { null }), //
                    () -> Matrix.ofAntiDiagonal(void.class, new Void[] { null }), //
                    () -> Matrix.ofDiagonals(void.class, new Void[] { null }, null), //
                    () -> m.map(x -> null, void.class), //
                    () -> m.zipWith(m, (x, y) -> null, void.class), //
                    () -> m.zipWith(m, m, (x, y, z) -> null, void.class));

            for (final org.junit.jupiter.api.function.Executable call : calls) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
                assertNotNull(e.getMessage());
                assertTrue(e.getMessage().contains("void"), e.getMessage());
            }
        }
    }

    @Nested
    class OfAntiDiagonalTests {

        @Test
        public void testDiagonalRU2LD() {
            Integer[] diagonal = { 1, 2, 3 };
            Matrix<Integer> matrix = Matrix.ofAntiDiagonal(diagonal);

            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals(1, matrix.get(0, 2));
            Assertions.assertEquals(2, matrix.get(1, 1));
            Assertions.assertEquals(3, matrix.get(2, 0));
            Assertions.assertNull(matrix.get(0, 0));
            Assertions.assertNull(matrix.get(2, 2));
        }

        @Test
        public void testDiagonalRU2LD_Values() {
            Matrix<Integer> m = Matrix.ofAntiDiagonal(new Integer[] { 1, 2, 3 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(Integer.valueOf(1), m.get(0, 2));
            assertEquals(Integer.valueOf(2), m.get(1, 1));
            assertEquals(Integer.valueOf(3), m.get(2, 0));
            assertNull(m.get(0, 0));
            assertNull(m.get(2, 2));
        }

        @Test
        public void testDiagonalRU2LD_Integers() {
            Integer[] diag = { 1, 2, 3 };
            Matrix<Integer> m = Matrix.ofAntiDiagonal(diag);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 2));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 0));
            assertNull(m.get(0, 0));
            assertNull(m.get(2, 2));
        }

        @Test
        public void testDiagonalRU2LD_EmptyArray() {
            Matrix<String> m = Matrix.ofAntiDiagonal(new String[0]);
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
        }
    }

    @Nested
    class OfDiagonalsTests {

        @Test
        public void testOfDiagonalsPreservesRuntimeDimensionForArrayValuedElements() {
            final Object[][] mainDiagonal = new String[][] { { "main-0" }, { "main-1" } };
            final Object[][] antiDiagonal = new Integer[][] { { 10 }, { 20 } };

            final Matrix<Object[]> matrix = Matrix.ofDiagonals(mainDiagonal, antiDiagonal);

            final Object[][] firstRow = matrix.rowCopy(0);
            final Object[][] secondRow = matrix.rowCopy(1);
            assertArrayEquals(new Object[] { "main-0" }, firstRow[0]);
            assertArrayEquals(new Object[] { 10 }, firstRow[1]);
            assertArrayEquals(new Object[] { 20 }, secondRow[0]);
            assertArrayEquals(new Object[] { "main-1" }, secondRow[1]);
            assertTrue(matrix.elementType().isArray());
        }

        @Test
        public void testDiagonalBoth() {
            Integer[] mainDiag = { 1, 2, 3 };
            Integer[] antiDiag = { 7, 8, 9 };
            Matrix<Integer> matrix = Matrix.ofDiagonals(mainDiag, antiDiag);

            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(1, 1));
            Assertions.assertEquals(3, matrix.get(2, 2));
            Assertions.assertEquals(7, matrix.get(0, 2));
            Assertions.assertEquals(9, matrix.get(2, 0));
        }

        @Test
        public void testDiagonalWithIncompatibleRuntimeTypes() {
            Number[] mainDiag = new Integer[] { 1, 2 };
            Number[] antiDiag = new Double[] { 3.0, 4.0 };

            Matrix<Number> matrix = Matrix.ofDiagonals(mainDiag, antiDiag);
            Assertions.assertEquals(Number.class, matrix.elementType());
            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(1, 1));
            Assertions.assertEquals(3.0d, matrix.get(0, 1).doubleValue());
            Assertions.assertEquals(4.0d, matrix.get(1, 0).doubleValue());
        }

        @Test
        public void testDiagonalWithDifferentLengths() {
            Integer[] mainDiag = { 1, 2, 3 };
            Integer[] antiDiag = { 7, 8 };

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Matrix.ofDiagonals(mainDiag, antiDiag);
            });
        }

        @Test
        public void testDiagonal_WithBothDiagonals() {
            Matrix<String> m = Matrix.ofDiagonals(new String[] { "A", "B", "C" }, new String[] { "X", "Y", "Z" });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals("A", m.get(0, 0));
            assertEquals("B", m.get(1, 1));
            assertEquals("C", m.get(2, 2));
            assertEquals("X", m.get(0, 2));
            assertEquals("Z", m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithOnlyMainDiagonal() {
            Matrix<String> m = Matrix.ofDiagonals(new String[] { "A", "B", "C" }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals("A", m.get(0, 0));
            assertEquals("B", m.get(1, 1));
            assertEquals("C", m.get(2, 2));
        }

        @Test
        public void testDiagonal_WithOnlyAntiDiagonal() {
            Matrix<String> m = Matrix.ofDiagonals(null, new String[] { "X", "Y", "Z" });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals("X", m.get(0, 2));
            assertEquals("Y", m.get(1, 1));
            assertEquals("Z", m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithDifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> Matrix.ofDiagonals(new String[] { "A", "B" }, new String[] { "X", "Y", "Z" }));
        }

        @Test
        public void testOfDiagonals_MainDiagonalWinsAtSharedCenter() {
            final Matrix<Integer> matrix = Matrix.ofDiagonals(new Integer[] { 1, 2, 3 }, new Integer[] { 7, 8, 9 });
            // At the shared center, the main diagonal wins; other cells remain null.
            assertEquals(Matrix.wrap(new Integer[][] { { 1, null, 7 }, { null, 2, null }, { 9, null, 3 } }), matrix);
        }

        @Test
        public void testDiagonal_OnlyLU2RD() {
            Integer[] lu2rd = { 1, 2 };
            Matrix<Integer> m = Matrix.ofDiagonals(lu2rd, null);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
        }

        @Test
        public void testDiagonal_OnlyRU2LD() {
            Integer[] ru2ld = { 1, 2 };
            Matrix<Integer> m = Matrix.ofDiagonals(null, ru2ld);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 1));
            assertEquals(2, m.get(1, 0));
        }

        @Test
        public void testDiagonal_DifferentLengths() {
            Integer[] lu2rd = { 1, 2, 3 };
            Integer[] ru2ld = { 7, 8 };
            assertThrows(IllegalArgumentException.class, () -> Matrix.ofDiagonals(lu2rd, ru2ld));
        }

        @Test
        public void testDiagonal_BothDiagonals_Strings() {
            String[] lu2rd = { "A", "B" };
            String[] ru2ld = { "X", "Y" };
            Matrix<String> m = Matrix.ofDiagonals(lu2rd, ru2ld);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals("A", m.get(0, 0));
            assertEquals("B", m.get(1, 1));
            assertEquals("X", m.get(0, 1));
            assertEquals("Y", m.get(1, 0));
        }

        @Test
        public void testOfDiagonals_ExplicitTypeValidatesBothSources() {
            assertThrows(IllegalArgumentException.class, () -> Matrix.ofDiagonals(String.class, null, null));
            assertThrows(IllegalArgumentException.class, () -> Matrix.ofDiagonals(String.class, new String[] { "a" }, new String[] { "b", "c" }));
        }

        @Test
        public void testDiagonalWithEmptyMainDiagonalAndDifferentRuntimeType() {
            Number[] mainDiag = new Integer[0];
            Number[] antiDiag = new Double[] { 1.5, 2.5 };

            Matrix<Number> matrix = Matrix.ofDiagonals(mainDiag, antiDiag);

            Assertions.assertEquals(2, matrix.rowCount());
            Assertions.assertEquals(2, matrix.columnCount());
            Assertions.assertEquals(1.5d, matrix.get(0, 1).doubleValue());
            Assertions.assertEquals(2.5d, matrix.get(1, 0).doubleValue());
        }

        @Test
        public void testDiagonal_WithBothNull() {
            assertTrue(Matrix.ofDiagonals(new String[] {}, new String[] {}).isEmpty());
            assertTrue(Matrix.ofDiagonals(new String[] {}, null).isEmpty());
            assertTrue(Matrix.ofDiagonals(null, new String[] {}).isEmpty());

            assertThrows(IllegalArgumentException.class, () -> Matrix.ofDiagonals(null, null));
        }

        @Test
        public void testOfDiagonals_EmptyAndNullSidesPreserveEmptyShape() {
            assertTrue(Matrix.ofDiagonals(new Integer[0], new Integer[0]).isEmpty());
            assertTrue(Matrix.ofDiagonals(new Integer[0], null).isEmpty());
            assertTrue(Matrix.ofDiagonals(null, new Integer[0]).isEmpty());
        }

        @Test
        public void testDiagonal_WithDifferentLengths_ThrowsException() {
            Integer[] lu = { 1, 2 };
            Integer[] ru = { 3, 4, 5 };
            assertThrows(IllegalArgumentException.class, () -> Matrix.ofDiagonals(lu, ru));
        }

        @Test
        public void testDiagonal_WithBothNull_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> Matrix.ofDiagonals(null, null));
        }
    }

    @Nested
    class GetTests {

        @Test
        public void testGet() {
            Integer[][] data = { { 1, 2, 3 }, { 4, 5, 6 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(5, matrix.get(1, 1));
            Assertions.assertEquals(6, matrix.get(1, 2));
        }

        @Test
        public void testGetWithPoint() {
            Integer[][] data = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            Sheet.Point point = Sheet.Point.of(1, 1);
            Assertions.assertEquals(4, matrix.get(point));
        }

        @Test
        public void testGet_Shape2By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            assertEquals("A", m.get(0, 0));
            assertEquals("E", m.get(1, 1));
            assertEquals("F", m.get(1, 2));
        }

        @Test
        public void testGetWithPoint_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertEquals("A", m.get(Point.of(0, 0)));
            assertEquals("D", m.get(Point.of(1, 1)));
            assertEquals("B", m.get(Point.of(0, 1)));
        }

        @Test
        public void testGet_ValidIndices() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertEquals("A", m.get(0, 0));
            assertEquals("B", m.get(0, 1));
            assertEquals("C", m.get(1, 0));
            assertEquals("D", m.get(1, 1));
        }

        @Test
        public void testGet_WithPoint() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Point p = Point.of(1, 0);
            assertEquals("C", m.get(p));
        }

        @Test
        public void testGet_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 10, 20 }, { 30, 40 } });
            assertEquals(10, m.get(0, 0));
            assertEquals(40, m.get(1, 1));
        }

        @Test
        public void testGet_WithIndices() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            assertEquals("a", m.get(0, 0));
            assertEquals("e", m.get(1, 1));
            assertEquals("f", m.get(1, 2));
        }

        @Test
        public void testGet_WithPoint_Values() {
            String[][] arr = { { "x", "y" }, { "z", "w" } };
            Matrix<String> m = new Matrix<>(arr);
            Point p = Point.of(1, 0);
            assertEquals("z", m.get(p));
        }

        @Test
        public void testGet_CanReturnNull() {
            String[][] arr = { { "a", null }, { null, "d" } };
            Matrix<String> m = new Matrix<>(arr);
            assertNull(m.get(0, 1));
            assertNull(m.get(1, 0));
        }

        @Test
        public void testGetOutOfBounds() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 } });

            Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
                matrix.get(2, 0);
            });
        }

        // Bottom row has no element below
        @Test
        public void testGet_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
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
            Integer[][] data = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            matrix.set(0, 1, 10);
            Assertions.assertEquals(10, matrix.get(0, 1));
        }

        @Test
        public void testSetWithPoint() {
            Integer[][] data = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            Sheet.Point point = Sheet.Point.of(1, 0);
            matrix.set(point, 20);
            Assertions.assertEquals(20, matrix.get(1, 0));
        }

        @Test
        public void testSet_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.set(0, 0, "X");
            assertEquals("X", m.get(0, 0));

            m.set(1, 1, "Y");
            assertEquals("Y", m.get(1, 1));
        }

        @Test
        public void testSetWithPoint_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.set(Point.of(0, 0), "Z");
            assertEquals("Z", m.get(Point.of(0, 0)));
        }

        @Test
        public void testAdjacentPoints_WithPoint() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            Point p = Point.of(1, 1);
            assertEquals("E", m.get(p));

            // Test setting with point
            m.set(p, "X");
            assertEquals("X", m.get(1, 1));
        }

        @Test
        public void testSet_ValidIndices() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.set(0, 0, "X");
            m.set(1, 1, "Y");
            assertEquals("X", m.get(0, 0));
            assertEquals("Y", m.get(1, 1));
        }

        @Test
        public void testSet_WithPoint() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Point p = Point.of(1, 0);
            m.set(p, "Z");
            assertEquals("Z", m.get(1, 0));
        }

        @Test
        public void testSet_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            m.set(0, 1, 99);
            assertEquals(99, m.get(0, 1));
        }

        @Test
        public void testSet_WithIndices() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            m.set(0, 1, "NEW");
            assertEquals("NEW", m.get(0, 1));
        }

        @Test
        public void testSet_WithPoint_Values() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Point p = Point.of(1, 1);
            m.set(p, "UPDATED");
            assertEquals("UPDATED", m.get(p));
        }

        @Test
        public void testSetAndSetRow_UseCanonicalObjectStorageForMixedTypes() {
            Matrix<Object> matrix = Matrix.copyOf(Object.class, new Object[] { 1, 2 }, new Integer[] { 3, 4 });

            matrix.set(0, 0, 1.5d);
            Assertions.assertDoesNotThrow(() -> matrix.setRow(1, new Number[] { BigDecimal.TEN, 4.5d }));
            assertEquals(List.of(1.5d, 2, BigDecimal.TEN, 4.5d), matrix.flatten());
        }

        @Test
        public void testDatasetConversionsOwnMutableNamesAndColumns() {
            for (final boolean transposed : new boolean[] { false, true }) {
                final Object shared = new StringBuilder("shared");
                final Matrix<Object> matrix = Matrix.wrap(Object.class, new Object[][] { { shared, null }, { "c", "d" } });
                final List<String> names = new ArrayList<>(List.of("first", "second"));
                final Dataset dataset = transposed ? matrix.toTransposedDataset(names) : matrix.toDataset(names);

                assertSame(shared, dataset.get(0, 0));
                assertNull(dataset.get(transposed ? 1 : 0, transposed ? 0 : 1));
                assertEquals(transposed ? "c" : null, dataset.get(0, 1));
                names.set(0, "caller-only");
                assertEquals("first", dataset.getColumnName(0));
                dataset.set(0, 0, "dataset-only");
                assertSame(shared, matrix.get(0, 0));
                matrix.set(1, 1, "matrix-only");
                assertEquals("d", dataset.get(1, 1));
                dataset.addRow(new Object[] { "new-first", "new-second" });
                assertEquals(3, dataset.size());
                assertEquals("new-first", dataset.get(2, 0));
                assertEquals("new-second", dataset.get(2, 1));
                assertEquals(2, matrix.rowCount());
            }
        }

        @Test
        public void testTransposedDatasetBulkCopiesLongAndCovariantRowsIntoIndependentColumns() {
            final int width = 1_000;
            final Object[][] data = new Object[3][width];

            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < width; j++) {
                    data[i][j] = j % 7 == 0 ? null : i * width + j;
                }
            }

            final Matrix<Object> matrix = Matrix.wrap(Object.class, data);
            final Dataset dataset = matrix.toTransposedDataset(List.of("a", "b", "c"));

            assertEquals(3, dataset.columnCount());
            assertEquals(width, dataset.size());

            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < width; j++) {
                    assertEquals(data[i][j], dataset.get(j, i));
                }
            }

            // Each column owns its storage: writes reach neither the matrix nor a sibling column, and vice versa.
            dataset.set(1, 0, "dataset-only");
            assertEquals(1, matrix.get(0, 1));
            assertEquals(Integer.valueOf(width + 1), dataset.get(1, 1));
            matrix.set(0, 2, "matrix-only");
            assertEquals(Integer.valueOf(2), dataset.get(2, 0));
            dataset.addRow(new Object[] { "x", "y", "z" });
            assertEquals(width + 1, dataset.size());
            assertEquals(width, matrix.columnCount());

            // A legacy matrix may keep narrower String[] rows under an Object element type; the copied columns
            // must still accept any value rather than inherit the row's runtime array type.
            @SuppressWarnings("deprecation")
            final Matrix<Object> covariant = new Matrix<>(new Object[][] { new String[] { "p", "q" }, new String[] { "r", "s" } });
            final Dataset fromCovariant = covariant.toTransposedDataset(List.of("first", "second"));
            fromCovariant.set(0, 0, 42);
            assertEquals(Integer.valueOf(42), fromCovariant.get(0, 0));
            assertEquals("p", covariant.get(0, 0));
            assertEquals("s", fromCovariant.get(1, 1));
        }

        @Test
        public void testNullHandling() {
            Matrix<String> matrix = Matrix.wrap(new String[][] { { null, "a" }, { "b", null } });

            Assertions.assertNull(matrix.get(0, 0));
            Assertions.assertEquals("a", matrix.get(0, 1));
            Assertions.assertEquals("b", matrix.get(1, 0));
            Assertions.assertNull(matrix.get(1, 1));

            matrix.set(0, 0, "x");
            Assertions.assertEquals("x", matrix.get(0, 0));
        }

        @Test
        public void testSet_NullValue() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.set(0, 0, null);
            assertNull(m.get(0, 0));
        }

        @Test
        public void testSet_WithNull() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            m.set(0, 0, null);
            assertNull(m.get(0, 0));
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testIncompatibleWriteOnNarrowBackingArrayThrowsIllegalArgumentException() {
            Matrix<Integer> intMatrix = Matrix.wrap(new Integer[][] { { 1 } });
            Matrix<Number> numberView = (Matrix<Number>) (Matrix<?>) intMatrix;

            Assertions.assertThrows(ArrayStoreException.class, () -> numberView.set(0, 0, 2.5d));
        }

        @Test
        public void testSet_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, "X"));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(2, 0, "X"));
        }
    }

    @Nested
    class ValueAboveTests {

        @Test
        public void testValueAbove() {
            Integer[][] data = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            Assertions.assertEquals(1, matrix.valueAbove(1, 0).orElse(null));
            Assertions.assertFalse(matrix.valueAbove(0, 0).isPresent());
        }

        @Test
        public void testAbove() {
            Matrix<String> matrix = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            u.Nullable<String> value = matrix.valueAbove(1, 0);
            assertEquals("A", value.get());
            u.Nullable<String> empty = matrix.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });

            Nullable<String> up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals("A", up.get());

            // Top row has no element above
            Nullable<String> empty = m.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_ValidPosition() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Nullable<String> up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals("A", up.get());
        }

        @Test
        public void testValueAbove_TopEdge() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Nullable<String> up = m.valueAbove(0, 0);
            assertFalse(up.isPresent());
        }

        @Test
        public void testAdjacentMethods_WithIntegers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertEquals(5, m.valueAbove(2, 1).get());
            assertEquals(5, m.valueBelow(0, 1).get());
            assertEquals(5, m.valueLeft(1, 2).get());
            assertEquals(5, m.valueRight(1, 0).get());
        }

        @Test
        public void testValueAbove_WithElementAbove() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Nullable<String> result = m.valueAbove(1, 0);
            assertTrue(result.isPresent());
            assertEquals("a", result.get());
        }

        @Test
        public void testValueAbove_AtTopRow_ReturnsEmpty() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Nullable<String> result = m.valueAbove(0, 0);
            assertFalse(result.isPresent());
        }
    }

    @Nested
    class ValueBelowTests {

        @Test
        public void testValueBelow() {
            Integer[][] data = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            Assertions.assertEquals(3, matrix.valueBelow(0, 0).orElse(null));
            Assertions.assertFalse(matrix.valueBelow(1, 0).isPresent());
        }

        @Test
        public void testBelow() {
            Matrix<String> matrix = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            u.Nullable<String> value = matrix.valueBelow(0, 0);
            assertEquals("C", value.get());
            u.Nullable<String> empty = matrix.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });

            Nullable<String> down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals("C", down.get());

            // Bottom row has no element below
            Nullable<String> empty = m.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_ValidPosition() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Nullable<String> down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals("C", down.get());
        }

        @Test
        public void testValueBelow_BottomEdge() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Nullable<String> down = m.valueBelow(1, 0);
            assertFalse(down.isPresent());
        }

        @Test
        public void testValueBelow_WithElementBelow() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Nullable<String> result = m.valueBelow(0, 1);
            assertTrue(result.isPresent());
            assertEquals("d", result.get());
        }

        @Test
        public void testValueBelow_AtBottomRow_ReturnsEmpty() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Nullable<String> result = m.valueBelow(1, 1);
            assertFalse(result.isPresent());
        }
    }

    @Nested
    class ValueLeftTests {

        @Test
        public void testValueLeft() {
            Integer[][] data = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            Assertions.assertEquals(1, matrix.valueLeft(0, 1).orElse(null));
            Assertions.assertFalse(matrix.valueLeft(0, 0).isPresent());
        }

        @Test
        public void testLeft() {
            Matrix<String> matrix = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            u.Nullable<String> value = matrix.valueLeft(0, 1);
            assertEquals("A", value.get());
            u.Nullable<String> empty = matrix.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });

            Nullable<String> left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals("A", left.get());

            // Leftmost column has no element to the left
            Nullable<String> empty = m.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_ValidPosition() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Nullable<String> left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals("A", left.get());
        }

        @Test
        public void testValueLeft_LeftEdge() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Nullable<String> left = m.valueLeft(0, 0);
            assertFalse(left.isPresent());
        }

        @Test
        public void testValueLeft_WithElementToLeft() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Nullable<String> result = m.valueLeft(0, 1);
            assertTrue(result.isPresent());
            assertEquals("a", result.get());
        }

        @Test
        public void testValueLeft_AtLeftColumn_ReturnsEmpty() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Nullable<String> result = m.valueLeft(1, 0);
            assertFalse(result.isPresent());
        }
    }

    @Nested
    class ValueRightTests {

        @Test
        public void testValueRight() {
            Integer[][] data = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            Assertions.assertEquals(2, matrix.valueRight(0, 0).orElse(null));
            Assertions.assertFalse(matrix.valueRight(0, 1).isPresent());
        }

        @Test
        public void testRight() {
            Matrix<String> matrix = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            u.Nullable<String> value = matrix.valueRight(0, 0);
            assertEquals("B", value.get());
            u.Nullable<String> empty = matrix.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });

            Nullable<String> right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals("B", right.get());

            // Rightmost column has no element to the right
            Nullable<String> empty = m.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_ValidPosition() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Nullable<String> right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals("B", right.get());
        }

        @Test
        public void testValueRight_RightEdge() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Nullable<String> right = m.valueRight(0, 1);
            assertFalse(right.isPresent());
        }

        @Test
        public void testValueRight_WithElementToRight() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Nullable<String> result = m.valueRight(1, 0);
            assertTrue(result.isPresent());
            assertEquals("d", result.get());
        }

        @Test
        public void testValueRight_AtRightColumn_ReturnsEmpty() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Nullable<String> result = m.valueRight(0, 1);
            assertFalse(result.isPresent());
        }
    }

    @Nested
    class RowViewTests {

        @Test
        public void testExplicitElementTypeFactoriesProvideUniformWritableRows() {
            Number[][] covariant = new Integer[][] { { 1, 2 } };
            assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(Number.class, covariant));
            assertThrows(IllegalArgumentException.class, () -> new Matrix<>(Number.class, covariant));

            Matrix<Number> copied = Matrix.copyOf(Number.class, covariant);
            assertEquals(Number.class, copied.rowView(0).getClass().getComponentType());
            Assertions.assertDoesNotThrow(() -> copied.set(0, 0, 2.5d));
            assertEquals(2.5d, copied.get(0, 0).doubleValue());

            Matrix<Number> diagonal = Matrix.ofDiagonals(Number.class, new Integer[] { 1, 2 }, new Double[] { 3d, 4d });
            Assertions.assertDoesNotThrow(() -> diagonal.set(0, 0, BigDecimal.TEN));
            assertEquals(Number.class, diagonal.rowView(0).getClass().getComponentType());

            Number[][] otherCovariant = new Integer[][] { { 3, 4 } };
            Matrix<Number> legacyFirst = Matrix.wrap(covariant);
            Matrix<Number> legacySecond = Matrix.wrap(otherCovariant);
            Matrix<Number> vertical = legacyFirst.stackVertically(legacySecond, Number.class);
            Matrix<Number> horizontal = legacyFirst.stackHorizontally(legacySecond, Number.class);
            assertEquals(Number.class, vertical.rowView(0).getClass().getComponentType());
            assertEquals(Number.class, vertical.rowView(1).getClass().getComponentType());
            assertEquals(Number.class, horizontal.rowView(0).getClass().getComponentType());
            Assertions.assertDoesNotThrow(() -> vertical.set(0, 0, 2.5d));
            Assertions.assertDoesNotThrow(() -> vertical.set(1, 0, 3.5d));
            Assertions.assertDoesNotThrow(() -> horizontal.set(0, 2, 4.5d));
        }

        @Test
        public void testRowView() {
            Integer[][] data = { { 1, 2, 3 }, { 4, 5, 6 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            Integer[] row = matrix.rowView(0);
            Assertions.assertArrayEquals(new Integer[] { 1, 2, 3 }, row);

            row[0] = 10; // This modifies the matrix
            Assertions.assertEquals(10, matrix.get(0, 0));
        }

        @Test
        public void testRowViewOnObjectMatrixDoesNotNarrowInternalStorage() {
            Matrix<Object> matrix = Matrix.wrap(new Object[][] { { "a" } });
            Object[] row = matrix.rowView(0);

            Assertions.assertEquals(Object.class, row.getClass().getComponentType());

            matrix.set(0, 0, 1);
            Assertions.assertEquals(1, matrix.get(0, 0));
        }

        @Test
        public void testDiagonalSupportsWiderGenericTypeAfterRowView() {
            Number[] mainDiag = new Number[] { 1 };
            Matrix<Number> matrix = Matrix.ofMainDiagonal(mainDiag);

            Number[] row = matrix.rowView(0);
            Assertions.assertEquals(Number.class, row.getClass().getComponentType());

            matrix.set(0, 0, 2.5d);
            Assertions.assertEquals(2.5d, matrix.get(0, 0).doubleValue(), 0.000001d);
        }

        @Test
        public void testTransformsStillWorkAfterWideningElementTypeAtRuntime() {
            Matrix<Number> matrix = Matrix.<Number> wrap(new Double[][] { { 1D, 1D } });
            matrix.rowView(0);
            matrix.set(0, 1, 2.5d);

            Matrix<Number> transposed = matrix.transpose();
            Assertions.assertEquals(2.5d, transposed.get(1, 0).doubleValue(), 0.000001d);

            Matrix<Number> reshaped = matrix.reshape(2, 1);
            Assertions.assertEquals(2.5d, reshaped.get(1, 0).doubleValue(), 0.000001d);
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            assertArrayEquals(new String[] { "A", "B", "C" }, m.rowView(0));
            assertArrayEquals(new String[] { "D", "E", "F" }, m.rowView(1));
        }

        @Test
        public void testRowView_Shape2By2_ArrayValues() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String[] row = m.rowView(0);
            assertArrayEquals(new String[] { "A", "B" }, row);
        }

        @Test
        public void testRowView_LastRow() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String[] row = m.rowView(1);
            assertArrayEquals(new String[] { "C", "D" }, row);
        }

        @Test
        public void testRowView_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Integer[] row = m.rowView(1);
            assertArrayEquals(new Integer[] { 4, 5, 6 }, row);
        }

        @Test
        public void testRowView_ReturnsCorrectRow() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            String[] row = m.rowView(1);
            assertArrayEquals(new String[] { "d", "e", "f" }, row);
        }

        @Test
        public void testRowViewAndColumnCopyResolveSpecificArrayTypeFromObjectStorage_EdgeCase() {
            Matrix<Integer> matrix = Matrix.ofDiagonals(new Integer[] { 1, 2 }, new Integer[] { 3, 4 });

            Object[] row = matrix.rowCopy(0);
            Object[] column = matrix.columnCopy(0);
            Object[] rowView = matrix.rowView(0);

            Assertions.assertTrue(row.getClass().getComponentType().isAssignableFrom(Integer.class));
            Assertions.assertTrue(column.getClass().getComponentType().isAssignableFrom(Integer.class));
            Assertions.assertTrue(rowView.getClass().getComponentType().isAssignableFrom(Integer.class));
            Assertions.assertEquals(1, row[0]);
            Assertions.assertEquals(3, row[1]);
            Assertions.assertEquals(1, column[0]);
            Assertions.assertEquals(4, column[1]);
        }

        @Test
        public void testColumnMajorIteratorToArraySnapshotsAliasedDestination() {
            Matrix<Integer> matrix = Matrix.wrap(Integer.class, new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 });
            Integer[] destination = matrix.rowView(1);

            try (Stream<Integer> stream = matrix.columnMajorStream(0, 1)) {
                ObjIteratorEx<Integer> iterator = (ObjIteratorEx<Integer>) stream.iterator();

                Assertions.assertSame(destination, iterator.toArray(destination));
                assertArrayEquals(new Integer[] { 1, 4, null }, destination);
                assertFalse(iterator.hasNext());
            }
        }

        @Test
        public void testColumnMajorIteratorToArraySnapshotsAliasedDestinationAfterAdvance() {
            Matrix<Integer> matrix = Matrix.wrap(Integer.class, new Integer[] { 1, 2, 3, 4 }, new Integer[] { 5, 6, 7, 8 });
            Integer[] destination = matrix.rowView(1);

            try (Stream<Integer> stream = matrix.columnMajorStream(0, 3)) {
                ObjIteratorEx<Integer> iterator = (ObjIteratorEx<Integer>) stream.iterator();
                iterator.advance(2);

                Assertions.assertSame(destination, iterator.toArray(destination));
                assertArrayEquals(new Integer[] { 2, 6, 3, 7 }, destination);
                assertFalse(iterator.hasNext());
            }
        }

        @Test
        public void testRowViewInvalidIndex() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 } });

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
                matrix.rowView(-1);
            });

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
                matrix.rowView(2);
            });
        }

        @Test
        public void testRowView_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(2));
        }

        @Test
        public void testRowView_InvalidIndex() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(1));
        }

        @Test
        public void testRowView_OutOfBounds_ThrowsException() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(5));
        }

        @Test
        public void testTokenFactoriesRejectOrCanonicalizeHeterogeneousRowTypes() {
            Object[][] data = new Object[][] { new Integer[] { 1, 2 }, new String[] { "a", "b" } };
            assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(Object.class, data));

            Matrix<Object> matrix = Matrix.copyOf(Object.class, data);
            assertEquals(Object.class, matrix.rowView(0).getClass().getComponentType());
            assertEquals(Object.class, matrix.rowView(1).getClass().getComponentType());

            matrix.flipVerticallyInPlace();

            assertEquals("a", matrix.get(0, 0));
            assertEquals("b", matrix.get(0, 1));
            assertEquals(1, matrix.get(1, 0));
            assertEquals(2, matrix.get(1, 1));
        }
    }

    @Nested
    class RowCopyTests {

        @Test
        public void testRowCopy() {
            Integer[][] data = { { 1, 2, 3 }, { 4, 5, 6 } };
            Matrix<Integer> matrix = Matrix.wrap(data);
            Integer[] copy = matrix.rowCopy(0);
            assertArrayEquals(new Integer[] { 1, 2, 3 }, copy);
            // Verify it's a copy, not a reference
            copy[0] = 99;
            assertEquals(1, matrix.get(0, 0));
        }

        @Test
        public void testRowCopy_OutOfBounds() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(1));
        }
    }

    @Nested
    class ColumnCopyTests {

        @Test
        public void testColumnCopy() {
            Integer[][] data = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            Integer[] column = matrix.columnCopy(1);
            Assertions.assertArrayEquals(new Integer[] { 2, 4, 6 }, column);

            column[0] = 10; // This does not modify the matrix
            Assertions.assertEquals(2, matrix.get(0, 1));
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            assertArrayEquals(new String[] { "A", "D" }, m.columnCopy(0));
            assertArrayEquals(new String[] { "B", "E" }, m.columnCopy(1));
            assertArrayEquals(new String[] { "C", "F" }, m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_Shape2By2_ArrayValues() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String[] col = m.columnCopy(0);
            assertArrayEquals(new String[] { "A", "C" }, col);
        }

        @Test
        public void testColumnCopy_LastColumn() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String[] col = m.columnCopy(1);
            assertArrayEquals(new String[] { "B", "D" }, col);
        }

        @Test
        public void testColumnCopy_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            Integer[] col = m.columnCopy(1);
            assertArrayEquals(new Integer[] { 2, 4, 6 }, col);
        }

        @Test
        public void testColumnCopy_ReturnsCorrectColumn() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            String[] col = m.columnCopy(1);
            assertArrayEquals(new String[] { "b", "e" }, col);
        }

        @Test
        public void testColumnCopyInvalidIndex() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 } });

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
                matrix.columnCopy(-1);
            });

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
                matrix.columnCopy(2);
            });
        }

        @Test
        public void testColumnCopy_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_OutOfBounds_ThrowsException() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(5));
        }
    }

    @Nested
    class SetRowTests {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Test
        public void testSetRowWithObjectArrayWidening_EdgeCase() {
            Matrix<Number> matrix = Matrix.ofDiagonals(new Integer[] { 1, 2 }, new Double[] { 3.0, 4.0 });
            Matrix raw = matrix;

            raw.setRow(0, new Object[] { 1L, 2.5d });

            Number[] row = matrix.rowView(0);
            Assertions.assertEquals(Number.class, row.getClass().getComponentType());
            Assertions.assertEquals(1L, row[0]);
            Assertions.assertEquals(2.5d, row[1].doubleValue());
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Test
        public void testSetRowSupportsObjectArrayWhenMatrixStorageIsObject_EdgeCase() {
            Matrix<Object> matrix = Matrix.ofDiagonals(new Object[] { 1, 2 }, new Double[] { 3.0, 4.0 });
            Matrix raw = matrix;

            raw.setRow(0, new Object[] { "x", 2 });

            Object[] row = matrix.rowView(0);
            Assertions.assertTrue(row.getClass().getComponentType().isAssignableFrom(String.class));
            Assertions.assertTrue(row.getClass().getComponentType().isAssignableFrom(Integer.class));
            Assertions.assertEquals("x", row[0]);
            Assertions.assertEquals(2, row[1]);
        }

        @Test
        public void testSetRow() {
            Integer[][] data = { { 1, 2, 3 }, { 4, 5, 6 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            matrix.setRow(0, new Integer[] { 7, 8, 9 });
            Assertions.assertArrayEquals(new Integer[] { 7, 8, 9 }, matrix.rowView(0));
        }

        @Test
        public void testSetRowWrongSize() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                matrix.setRow(0, new Integer[] { 7, 8 });
            });
        }

        @Test
        public void testSetRowAndSetColumnWidenStorageWhenNeeded() {
            Matrix<Number> matrix = Matrix.<Number> wrap(new Double[][] { { 1D, 1D }, { 1D, 1D } });
            matrix.rowView(0);
            matrix.rowView(1);

            matrix.setRow(0, new Number[] { 1.5d, 2.5d });
            matrix.setColumn(1, new Number[] { 3.5d, 4.5d });

            Assertions.assertEquals(1.5d, matrix.get(0, 0).doubleValue(), 0.000001d);
            Assertions.assertEquals(3.5d, matrix.get(0, 1).doubleValue(), 0.000001d);
            Assertions.assertEquals(4.5d, matrix.get(1, 1).doubleValue(), 0.000001d);
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.setRow(0, new String[] { "X", "Y" });
            assertArrayEquals(new String[] { "X", "Y" }, m.rowView(0));
            assertArrayEquals(new String[] { "C", "D" }, m.rowView(1)); // unchanged
        }

        @Test
        public void testSetRow_WrongLength() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new String[] { "X" }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new String[] { "X", "Y", "Z" }));
        }

        @Test
        public void testSetRow_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            m.setRow(1, new Integer[] { 10, 20 });
            assertArrayEquals(new Integer[] { 10, 20 }, m.rowView(1));
        }

        @Test
        public void testSetRow_UpdatesRow() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            m.setRow(0, new String[] { "X", "Y" });
            assertEquals("X", m.get(0, 0));
            assertEquals("Y", m.get(0, 1));
        }

        @Test
        public void testSetRowColumnAndAntiDiagonalRejectNullArguments() {
            Matrix<String> matrix = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setRow(0, null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setColumn(0, null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setAntiDiagonal(null));
        }

        @Test
        public void testSetRow_WrongLength_ThrowsException() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new String[] { "x", "y", "z" }));
        }
    }

    @Nested
    class SetColumnTests {

        @Test
        public void testSetColumn() {
            Integer[][] data = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            Matrix<Integer> matrix = Matrix.wrap(data);

            matrix.setColumn(1, new Integer[] { 7, 8, 9 });
            Assertions.assertArrayEquals(new Integer[] { 7, 8, 9 }, matrix.columnCopy(1));
        }

        @Test
        public void testSetColumnWrongSize() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                matrix.setColumn(0, new Integer[] { 7, 8 });
            });
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.setColumn(0, new String[] { "X", "Y" });
            assertArrayEquals(new String[] { "X", "Y" }, m.columnCopy(0));
            assertArrayEquals(new String[] { "B", "D" }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testSetColumn_WrongLength() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new String[] { "X" }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new String[] { "X", "Y", "Z" }));
        }

        @Test
        public void testSetColumn_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            m.setColumn(0, new Integer[] { 100, 200 });
            assertArrayEquals(new Integer[] { 100, 200 }, m.columnCopy(0));
        }

        @Test
        public void testSetColumn_UpdatesColumn() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            m.setColumn(1, new String[] { "X", "Y" });
            assertEquals("X", m.get(0, 1));
            assertEquals("Y", m.get(1, 1));
        }

        @Test
        public void testAliasedArraySourcesAreSnapshotted() {
            Matrix<Integer> columnMatrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            columnMatrix.setColumn(1, columnMatrix.rowView(0));
            assertArrayEquals(new Integer[] { 3, 2 }, columnMatrix.rowCopy(1));

            Matrix<Integer> diagonalMatrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            diagonalMatrix.setAntiDiagonal(diagonalMatrix.rowView(0));
            assertArrayEquals(new Integer[] { 1, 2, 3 }, diagonalMatrix.antiDiagonalCopy());

            Integer[][] backing = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
            Matrix<Integer> fillMatrix = Matrix.wrap(backing);
            fillMatrix.copyFrom(1, 0, new Integer[][] { backing[0], backing[1] });
            assertArrayEquals(new Integer[] { 3, 4 }, fillMatrix.rowCopy(2));
        }

        @Test
        public void testSetColumn_WrongLength_ThrowsException() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new String[] { "x", "y", "z" }));
        }
    }

    @Nested
    class UpdateRowTests {

        @Test
        public void testUpdateRow() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            matrix.updateRow(0, x -> x * 2);
            Assertions.assertArrayEquals(new Integer[] { 2, 4, 6 }, matrix.rowView(0));
            Assertions.assertArrayEquals(new Integer[] { 4, 5, 6 }, matrix.rowView(1));
        }

        @Test
        public void testUpdateRow_Shape2By2_ArrayValues() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.updateRow(0, x -> x + "1");
            assertArrayEquals(new String[] { "A1", "B1" }, m.rowView(0));
            assertArrayEquals(new String[] { "C", "D" }, m.rowView(1)); // unchanged
        }

        @Test
        public void testUpdateRow_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            m.updateRow(0, String::toUpperCase);
            assertEquals("A", m.get(0, 0));
            assertEquals("B", m.get(0, 1));
            assertEquals("c", m.get(1, 0));
        }

        @Test
        public void testUpdateRow_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            m.updateRow(1, x -> x * 10);
            assertEquals(1, m.get(0, 0));
            assertEquals(30, m.get(1, 0));
            assertEquals(40, m.get(1, 1));
        }

        @Test
        public void testUpdateRow_AppliesFunction() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            m.updateRow(0, x -> x * 10);
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
        }

        @Test
        public void testUpdateRowNullOperator() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.updateRow(0, (Throwables.UnaryOperator<Integer, RuntimeException>) null));
        }

        @Test
        public void testUpdateRow_WithNullFunction() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.updateRow(0, x -> null);
            assertNull(m.get(0, 0));
            assertNull(m.get(0, 1));
            assertEquals("C", m.get(1, 0));
        }

        @Test
        public void testUpdateRowInvalidIndex() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateRow(-1, x -> x * 2));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateRow(2, x -> x * 2));
        }
    }

    @Nested
    class UpdateColumnTests {

        @Test
        public void testUpdateColumn() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });

            matrix.updateColumn(1, x -> x + 10);
            Assertions.assertArrayEquals(new Integer[] { 12, 14, 16 }, matrix.columnCopy(1));
            Assertions.assertArrayEquals(new Integer[] { 1, 3, 5 }, matrix.columnCopy(0));
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.updateColumn(0, x -> x + "2");
            assertArrayEquals(new String[] { "A2", "C2" }, m.columnCopy(0));
            assertArrayEquals(new String[] { "B", "D" }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testUpdateColumn_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            m.updateColumn(0, String::toUpperCase);
            assertEquals("A", m.get(0, 0));
            assertEquals("b", m.get(0, 1));
            assertEquals("C", m.get(1, 0));
        }

        @Test
        public void testUpdateColumn_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            m.updateColumn(1, x -> x + 100);
            assertEquals(1, m.get(0, 0));
            assertEquals(102, m.get(0, 1));
            assertEquals(104, m.get(1, 1));
        }

        @Test
        public void testUpdateColumn_AppliesFunction() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            m.updateColumn(0, String::toUpperCase);
            assertEquals("A", m.get(0, 0));
            assertEquals("C", m.get(1, 0));
            assertEquals("b", m.get(0, 1));
        }

        @Test
        public void testUpdateColumnNullOperator() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.updateColumn(0, (Throwables.UnaryOperator<Integer, RuntimeException>) null));
        }

        @Test
        public void testUpdateColumn_WithNullFunction() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.updateColumn(0, x -> null);
            assertNull(m.get(0, 0));
            assertNull(m.get(1, 0));
            assertEquals("B", m.get(0, 1));
        }

        @Test
        public void testUpdateColumnInvalidIndex() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateColumn(-1, x -> x + 10));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateColumn(2, x -> x + 10));
        }
    }

    @Nested
    class MainDiagonalCopyTests {

        @Test
        public void testMainDiagonalCopy() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            Integer[] diagonal = matrix.mainDiagonalCopy();
            Assertions.assertArrayEquals(new Integer[] { 1, 5, 9 }, diagonal);
        }

        @Test
        public void testMainDiagonalCopyRectangular() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Assertions.assertArrayEquals(new Integer[] { 1, 5 }, matrix.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_ThreeByThreeValues() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            Integer[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new Integer[] { 1, 5, 9 }, diag);
        }

        @Test
        public void testMainDiagonalCopy_Shape3By3_ArrayValues() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            assertArrayEquals(new String[] { "A", "E", "I" }, m.mainDiagonalCopy());
        }

        @Test
        public void testGetLU2RD_rectangular() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            assertArrayEquals(new String[] { "A" }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_Rectangular_Shape3By2_ArrayValues() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertArrayEquals(new Integer[] { 1, 4 }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new String[] { "A", "D" }, diag);
        }

        @Test
        public void testMainDiagonalCopy_ReturnsMainDiagonal() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" }, { "g", "h", "i" } };
            Matrix<String> m = new Matrix<>(arr);
            String[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new String[] { "a", "e", "i" }, diag);
        }

        @Test
        public void testMainDiagonalCopy_Rectangular() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b", "c" }, { "d", "e", "f" } });
            assertArrayEquals(new String[] { "a", "e" }, m.mainDiagonalCopy());
        }
    }

    @Nested
    class SetMainDiagonalTests {

        @Test
        public void testSetMainDiagonal() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            matrix.setMainDiagonal(new Integer[] { 10, 20, 30 });
            Assertions.assertEquals(10, matrix.get(0, 0));
            Assertions.assertEquals(20, matrix.get(1, 1));
            Assertions.assertEquals(30, matrix.get(2, 2));
        }

        @Test
        public void testSetMainDiagonalShortArray() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                matrix.setMainDiagonal(new Integer[] { 10 });
            });
        }

        @Test
        public void testSetMainDiagonal_Shape3By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            m.setMainDiagonal(new String[] { "X", "Y", "Z" });
            assertEquals("X", m.get(0, 0));
            assertEquals("Y", m.get(1, 1));
            assertEquals("Z", m.get(2, 2));
        }

        @Test
        public void testSetMainDiagonal_Rectangular() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            m.setMainDiagonal(new String[] { "X" });
            assertArrayEquals(new String[] { "X", "B" }, m.rowCopy(0));
        }

        @Test
        public void testSetMainDiagonal_ArrayTooShort() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new String[] { "X", "Y" }));
        }

        @Test
        public void testSetMainDiagonal_Rectangular_Shape3By2_ArrayValues() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.setMainDiagonal(new Integer[] { 10, 20 });
            assertArrayEquals(new Integer[] { 10, 20 }, m.mainDiagonalCopy());
        }

        @Test
        public void testSetMainDiagonal_WrongLength() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new Integer[] { 10 }));
        }

        @Test
        public void testSetMainDiagonal_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setMainDiagonal(new Integer[] { 10, 20, 30 });
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 2));
        }

        @Test
        public void testSetMainDiagonal_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.setMainDiagonal(new String[] { "X", "Y" });
            assertEquals("X", m.get(0, 0));
            assertEquals("Y", m.get(1, 1));
        }

        @Test
        public void testSetMainDiagonal_SetsMainDiagonal() {
            Integer[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            Matrix<Integer> m = new Matrix<>(arr);
            m.setMainDiagonal(new Integer[] { 11, 22, 33 });
            assertEquals(11, m.get(0, 0));
            assertEquals(22, m.get(1, 1));
            assertEquals(33, m.get(2, 2));
        }

        @Test
        public void testSetDiagonal_NullArraysOnEmptyMatrixThrow() {
            Matrix<Object> m = Matrix.empty();
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(null));
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(null));
        }

        @Test
        public void testSetMainDiagonal_WrongLength_ThrowsException() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new Integer[] { 1, 2, 3 }));
        }
    }

    @Nested
    class UpdateMainDiagonalTests {

        @Test
        public void testUpdateMainDiagonal() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            matrix.updateMainDiagonal(x -> x * 10);
            Assertions.assertEquals(10, matrix.get(0, 0));
            Assertions.assertEquals(50, matrix.get(1, 1));
            Assertions.assertEquals(90, matrix.get(2, 2));
            Assertions.assertEquals(2, matrix.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            m.updateMainDiagonal(x -> x + "1");
            assertEquals("A1", m.get(0, 0));
            assertEquals("E1", m.get(1, 1));
            assertEquals("I1", m.get(2, 2));
            assertEquals("B", m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateMainDiagonal_Rectangular() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            m.updateMainDiagonal(x -> x + "1");
            assertArrayEquals(new String[] { "A1" }, m.mainDiagonalCopy());
        }

        @Test
        public void testUpdateMainDiagonal_Rectangular_Shape3By2_ArrayValues() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.updateMainDiagonal(x -> x * 2);
            assertArrayEquals(new Integer[] { 2, 8 }, m.mainDiagonalCopy());
        }

        @Test
        public void testUpdateMainDiagonal_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateMainDiagonal(x -> x * 2);
            assertEquals(2, m.get(0, 0));
            assertEquals(10, m.get(1, 1));
            assertEquals(18, m.get(2, 2));
            assertEquals(2, m.get(0, 1)); // Non-diagonal unchanged
        }

        @Test
        public void testUpdateMainDiagonal_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            m.updateMainDiagonal(String::toUpperCase);
            assertEquals("A", m.get(0, 0));
            assertEquals("D", m.get(1, 1));
            assertEquals("b", m.get(0, 1)); // Non-diagonal unchanged
        }

        @Test
        public void testUpdateMainDiagonal_AppliesFunction() {
            Integer[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            Matrix<Integer> m = new Matrix<>(arr);
            m.updateMainDiagonal(x -> x * 100);
            assertEquals(100, m.get(0, 0));
            assertEquals(500, m.get(1, 1));
            assertEquals(900, m.get(2, 2));
        }
    }

    @Nested
    class AntiDiagonalCopyTests {

        @Test
        public void testAntiDiagonalCopy() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            Integer[] diagonal = matrix.antiDiagonalCopy();
            Assertions.assertArrayEquals(new Integer[] { 3, 5, 7 }, diagonal);
        }

        @Test
        public void testAntiDiagonalCopy_ThreeByThreeValues() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            Integer[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new Integer[] { 3, 5, 7 }, diag);
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            assertArrayEquals(new String[] { "C", "E", "G" }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_Rectangular() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            assertArrayEquals(new String[] { "B" }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_Rectangular_Shape3By2_ArrayValues() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertArrayEquals(new Integer[] { 2, 3 }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new String[] { "B", "C" }, diag);
        }

        @Test
        public void testAntiDiagonalCopy_ReturnsAntiDiagonal() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" }, { "g", "h", "i" } };
            Matrix<String> m = new Matrix<>(arr);
            String[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new String[] { "c", "e", "g" }, diag);
        }
    }

    @Nested
    class SetAntiDiagonalTests {

        @Test
        public void testSetAntiDiagonal() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            matrix.setAntiDiagonal(new Integer[] { 10, 20, 30 });
            Assertions.assertEquals(10, matrix.get(0, 2));
            Assertions.assertEquals(20, matrix.get(1, 1));
            Assertions.assertEquals(30, matrix.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            m.setAntiDiagonal(new String[] { "X", "Y", "Z" });
            assertEquals("X", m.get(0, 2));
            assertEquals("Y", m.get(1, 1));
            assertEquals("Z", m.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_Rectangular() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            m.setAntiDiagonal(new String[] { "X" });
            assertArrayEquals(new String[] { "A", "X" }, m.rowCopy(0));
        }

        @Test
        public void testSetAntiDiagonal_ArrayTooShort() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new String[] { "X", "Y" }));
        }

        @Test
        public void testSetAntiDiagonal_Rectangular_Shape3By2_ArrayValues() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.setAntiDiagonal(new Integer[] { 10, 20 });
            assertArrayEquals(new Integer[] { 10, 20 }, m.antiDiagonalCopy());
        }

        @Test
        public void testSetAntiDiagonal_WrongLength() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new Integer[] { 10 }));
        }

        @Test
        public void testSetAntiDiagonal_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setAntiDiagonal(new Integer[] { 10, 20, 30 });
            assertEquals(10, m.get(0, 2));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.setAntiDiagonal(new String[] { "X", "Y" });
            assertEquals("X", m.get(0, 1));
            assertEquals("Y", m.get(1, 0));
        }

        @Test
        public void testSetAntiDiagonal_SetsAntiDiagonal() {
            Integer[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            Matrix<Integer> m = new Matrix<>(arr);
            m.setAntiDiagonal(new Integer[] { 11, 22, 33 });
            assertEquals(11, m.get(0, 2));
            assertEquals(22, m.get(1, 1));
            assertEquals(33, m.get(2, 0));
        }
    }

    @Nested
    class UpdateAntiDiagonalTests {

        @Test
        public void testUpdateAntiDiagonal() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            matrix.updateAntiDiagonal(x -> x * -1);
            Assertions.assertEquals(-3, matrix.get(0, 2));
            Assertions.assertEquals(-5, matrix.get(1, 1));
            Assertions.assertEquals(-7, matrix.get(2, 0));
            Assertions.assertEquals(1, matrix.get(0, 0)); // unchanged
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            m.updateAntiDiagonal(x -> x + "2");
            assertEquals("C2", m.get(0, 2));
            assertEquals("E2", m.get(1, 1));
            assertEquals("G2", m.get(2, 0));
            assertEquals("B", m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateAntiDiagonal_Rectangular() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            m.updateAntiDiagonal(x -> x + "2");
            assertArrayEquals(new String[] { "B2" }, m.antiDiagonalCopy());
        }

        @Test
        public void testUpdateAntiDiagonal_Rectangular_Shape3By2_ArrayValues() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.updateAntiDiagonal(x -> x * 2);
            assertArrayEquals(new Integer[] { 4, 6 }, m.antiDiagonalCopy());
        }

        @Test
        public void testUpdateAntiDiagonal_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateAntiDiagonal(x -> x * 2);
            assertEquals(6, m.get(0, 2));
            assertEquals(10, m.get(1, 1));
            assertEquals(14, m.get(2, 0));
            assertEquals(2, m.get(0, 1)); // Non-diagonal unchanged
        }

        @Test
        public void testUpdateAntiDiagonal_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            m.updateAntiDiagonal(String::toUpperCase);
            assertEquals("B", m.get(0, 1));
            assertEquals("C", m.get(1, 0));
            assertEquals("a", m.get(0, 0)); // Non-diagonal unchanged
        }

        @Test
        public void testUpdateAntiDiagonal_AppliesFunction() {
            Integer[][] arr = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
            Matrix<Integer> m = new Matrix<>(arr);
            m.updateAntiDiagonal(x -> x + 1000);
            assertEquals(1003, m.get(0, 2));
            assertEquals(1005, m.get(1, 1));
            assertEquals(1007, m.get(2, 0));
        }
    }

    @Nested
    class UpdateAllTests {

        @Test
        public void testIndexedUpdateStopsAtFailingCallbackWithoutRollingBackEarlierWrites() {
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                final Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 } });
                final IllegalStateException failure = new IllegalStateException("stop");
                final List<Integer> visitedColumns = new ArrayList<>();

                Assertions.assertSame(failure, assertThrows(IllegalStateException.class, () -> matrix.updateAll((i, j) -> {
                    visitedColumns.add(j);
                    if (j == 1) {
                        throw failure;
                    }
                    return 10;
                })));

                assertEquals(List.of(0, 1), visitedColumns);
                assertArrayEquals(new Integer[] { 10, 2, 3 }, matrix.rowCopy(0));
            });
        }

        @Test
        public void testUpdateAll() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            matrix.updateAll(x -> x * x);
            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(4, matrix.get(0, 1));
            Assertions.assertEquals(9, matrix.get(1, 0));
            Assertions.assertEquals(16, matrix.get(1, 1));
        }

        @Test
        public void testUpdateAllWithIndices() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            matrix.updateAll((i, j) -> i + j);
            Assertions.assertEquals(0, matrix.get(0, 0));
            Assertions.assertEquals(1, matrix.get(0, 1));
            Assertions.assertEquals(1, matrix.get(1, 0));
            Assertions.assertEquals(2, matrix.get(1, 1));
        }

        @Test
        public void testUpdateAllByIndex() {
            Matrix<Integer> numMatrix = Matrix.wrap(new Integer[][] { { 0, 0 }, { 0, 0 } });
            numMatrix.updateAll((i, j) -> i * 10 + j);
            assertEquals(0, numMatrix.get(0, 0));
            assertEquals(1, numMatrix.get(0, 1));
            assertEquals(10, numMatrix.get(1, 0));
            assertEquals(11, numMatrix.get(1, 1));
        }

        @Test
        public void testUpdateAll_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.updateAll(x -> x + "1");
            assertEquals("A1", m.get(0, 0));
            assertEquals("B1", m.get(0, 1));
            assertEquals("C1", m.get(1, 0));
            assertEquals("D1", m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithIndices() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 0, 0 }, { 0, 0 } });
            m.updateAll((i, j) -> i * 10 + j);
            assertEquals(Integer.valueOf(0), m.get(0, 0));
            assertEquals(Integer.valueOf(1), m.get(0, 1));
            assertEquals(Integer.valueOf(10), m.get(1, 0));
            assertEquals(Integer.valueOf(11), m.get(1, 1));
        }

        @Test
        public void testUpdateAll_UnaryOperator_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            m.updateAll(x -> x.toUpperCase());
            assertEquals("A", m.get(0, 0));
            assertEquals("B", m.get(0, 1));
            assertEquals("C", m.get(1, 0));
            assertEquals("D", m.get(1, 1));
        }

        @Test
        public void testUpdateAll_UnaryOperator_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll(x -> x * 10);
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(0, 1));
            assertEquals(30, m.get(1, 0));
            assertEquals(40, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 0, 1 }, { 2, 3 } });
            m.updateAll((i, j) -> i * 10 + j);
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
            assertEquals(10, m.get(1, 0));
            assertEquals(11, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            m.updateAll((i, j) -> "" + i + j);
            assertEquals("00", m.get(0, 0));
            assertEquals("01", m.get(0, 1));
            assertEquals("10", m.get(1, 0));
            assertEquals("11", m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithUnaryOperator() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            m.updateAll(x -> x * 2);
            assertEquals(2, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
            assertEquals(6, m.get(1, 0));
            assertEquals(8, m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithBiFunction() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            m.updateAll((i, j) -> i + ":" + j);
            assertEquals("0:0", m.get(0, 0));
            assertEquals("0:1", m.get(0, 1));
            assertEquals("1:0", m.get(1, 0));
            assertEquals("1:1", m.get(1, 1));
        }

        @Test
        public void testUpdateAllUnary_TallMatrixTraversesRowMajor() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 0, 0 }, { 0, 0 }, { 0, 0 } });
            final int[] counter = { 0 };

            m.updateAll(value -> ++counter[0]);

            assertEquals(List.of(1, 2, 3, 4, 5, 6), m.flatten());
        }

        @Test
        public void testUpdateAllIndexed_TallMatrixTraversesRowMajor() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 0, 0 }, { 0, 0 }, { 0, 0 } });
            final int[] counter = { 0 };

            m.updateAll((i, j) -> ++counter[0]);

            assertEquals(List.of(1, 2, 3, 4, 5, 6), m.flatten());
        }

        @Test
        public void testGenericMatrixInPlaceMutatorsHonourTheParallelPolicy() {
            final int side = 40;

            // FORCE_OFF (the default) keeps every one of them on the calling thread.
            final Matrix<Integer> sequential = Matrix.wrap(Integer.class, new Integer[side][side]);
            sequential.updateAll(v -> 0);
            final Set<Thread> offThreads = ConcurrentHashMap.newKeySet();
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> sequential.updateAll((i, j) -> {
                offThreads.add(Thread.currentThread());
                return i * side + j;
            }));
            assertEquals(1, offThreads.size());
            assertEquals(Thread.currentThread(), offThreads.iterator().next());

            // FORCE_ON produces the same values, and may now use more than one thread.
            final Matrix<Integer> parallel = Matrix.wrap(Integer.class, new Integer[side][side]);
            final Set<Thread> onThreads = ConcurrentHashMap.newKeySet();
            Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> parallel.updateAll((i, j) -> {
                onThreads.add(Thread.currentThread());
                return i * side + j;
            }));
            assertEquals(sequential, parallel);
            assertEquals(side * side - 1, parallel.get(side - 1, side - 1));

            // All four mutators remain correct under forced parallelism.
            Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> {
                parallel.updateAll(v -> v + 1);
                parallel.replaceIf(v -> v == 1, -1);
                parallel.replaceIf((i, j) -> i == 0 && j == 1, -2);
            });
            assertEquals(-1, parallel.get(0, 0));
            assertEquals(-2, parallel.get(0, 1));
            assertEquals(side * side, parallel.get(side - 1, side - 1));

            // If the runtime really did run the callback on more than one thread, the sequential run
            // must still have been single-threaded -- that is the contract this test protects.
            if (onThreads.size() > 1) {
                assertEquals(1, offThreads.size());
            }
        }

        @Test
        public void testMap_WithIndices() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.updateAll((i, j) -> m.get(i, j) + i + j);
            assertEquals("A00", m.get(0, 0));
            assertEquals("B01", m.get(0, 1));
            assertEquals("C10", m.get(1, 0));
            assertEquals("D11", m.get(1, 1));
        }

        @Test
        public void testUpdateAllNullOperator() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> emptyLike = Matrix.wrap(new Integer[][] { {}, {} });

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.UnaryOperator<Integer, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.IntBiFunction<Integer, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAll((Throwables.UnaryOperator<Integer, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAll((Throwables.IntBiFunction<Integer, RuntimeException>) null));
        }

        @Test
        public void testNullHandling() {
            Matrix<String> m = Matrix.wrap(new String[][] { { null, "B" }, { "C", null } });

            assertNull(m.get(0, 0));
            assertEquals("B", m.get(0, 1));

            // Test operations with nulls
            m.updateAll(x -> x == null ? "NULL" : x);
            assertEquals("NULL", m.get(0, 0));
            assertEquals("B", m.get(0, 1));
        }

        @Test
        public void testUpdateAll_UnaryOperator_WithNull() {
            Matrix<String> m = Matrix.wrap(new String[][] { { null, "b" }, { "c", null } });
            m.updateAll(x -> x == null ? "NULL" : x.toUpperCase());
            assertEquals("NULL", m.get(0, 0));
            assertEquals("B", m.get(0, 1));
            assertEquals("C", m.get(1, 0));
            assertEquals("NULL", m.get(1, 1));
        }

        @Test
        public void testUpdateAll_ZeroColumnsDoNotInvokeOperator() {
            final var matrix = Matrix.wrap(new Integer[2][0]);
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
        public void testReplaceIf() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            matrix.replaceIf(x -> x % 2 == 0, 0);
            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(0, matrix.get(0, 1));
            Assertions.assertEquals(3, matrix.get(0, 2));
            Assertions.assertEquals(0, matrix.get(1, 0));
        }

        @Test
        public void testReplaceIfWithIndices() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            matrix.replaceIf((i, j) -> i == j, 0);
            Assertions.assertEquals(0, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(0, 1));
            Assertions.assertEquals(3, matrix.get(1, 0));
            Assertions.assertEquals(0, matrix.get(1, 1));
        }

        @Test
        public void testReplaceIf_Shape2By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "A" }, { "C", "A", "D" } });
            m.replaceIf(x -> "A".equals(x), "X");
            assertEquals("X", m.get(0, 0));
            assertEquals("B", m.get(0, 1));
            assertEquals("X", m.get(0, 2));
            assertEquals("C", m.get(1, 0));
            assertEquals("X", m.get(1, 1));
            assertEquals("D", m.get(1, 2));
        }

        @Test
        public void testReplaceIf_WithIndices() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            m.replaceIf((i, j) -> i == j, "X"); // Replace diagonal
            assertEquals("X", m.get(0, 0));
            assertEquals("X", m.get(1, 1));
            assertEquals("X", m.get(2, 2));
            assertEquals("B", m.get(0, 1)); // unchanged
        }

        @Test
        public void testReplaceIf_NoneMatch() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.replaceIf(x -> "Z".equals(x), "X");
            assertEquals("A", m.get(0, 0));
            assertEquals("D", m.get(1, 1));
        }

        @Test
        public void testReplaceIf_WithIndices_NoneMatch() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.replaceIf((i, j) -> i > 10, "X");
            assertEquals("A", m.get(0, 0));
            assertEquals("D", m.get(1, 1));
        }

        @Test
        public void testReplaceIf_PositionPredicate_UpperTriangle() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.replaceIf((i, j) -> i < j, 0);
            assertEquals(1, m.get(0, 0));
            assertEquals(0, m.get(0, 1));
            assertEquals(0, m.get(0, 2));
            assertEquals(4, m.get(1, 0));
            assertEquals(5, m.get(1, 1));
            assertEquals(0, m.get(1, 2));
        }

        @Test
        public void testReplaceIf_ValuePredicate_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, -2, 3 }, { -4, 5, -6 } });
            m.replaceIf(x -> x < 0, 0);
            assertEquals(1, m.get(0, 0));
            assertEquals(0, m.get(0, 1));
            assertEquals(3, m.get(0, 2));
            assertEquals(0, m.get(1, 0));
            assertEquals(5, m.get(1, 1));
            assertEquals(0, m.get(1, 2));
        }

        @Test
        public void testReplaceIf_ValuePredicate_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "short", "verylongstring" }, { "ok", "toolong" } });
            m.replaceIf(x -> x.length() > 5, "TOO_LONG");
            assertEquals("short", m.get(0, 0));
            assertEquals("TOO_LONG", m.get(0, 1));
            assertEquals("ok", m.get(1, 0));
            assertEquals("TOO_LONG", m.get(1, 1));
        }

        @Test
        public void testReplaceIf_PositionPredicate_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.replaceIf((i, j) -> i == j, 0); // Replace diagonal
            assertEquals(0, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(0, m.get(1, 1));
            assertEquals(0, m.get(2, 2));
        }

        @Test
        public void testReplaceIf_PositionPredicate_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            m.replaceIf((i, j) -> i < j, "X"); // Replace upper triangle
            assertEquals("A", m.get(0, 0));
            assertEquals("X", m.get(0, 1));
            assertEquals("X", m.get(0, 2));
            assertEquals("D", m.get(1, 0));
            assertEquals("E", m.get(1, 1));
            assertEquals("X", m.get(1, 2));
        }

        @Test
        public void testReplaceIf_WithPredicate() {
            Integer[][] arr = { { 1, 2, 3 }, { 4, 5, 6 } };
            Matrix<Integer> m = new Matrix<>(arr);
            m.replaceIf(x -> x > 3, 99);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(99, m.get(1, 0));
            assertEquals(99, m.get(1, 2));
        }

        @Test
        public void testReplaceIf_WithBiPredicate() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            m.replaceIf((i, j) -> i == j, 0);
            assertEquals(0, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(0, m.get(1, 1));
        }

        @Test
        public void testReplaceIfValue_TallMatrixTraversesRowMajor() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            final int[] counter = { 0 };

            m.replaceIf(value -> ++counter[0] <= 3, 9);

            assertEquals(List.of(9, 9, 9, 4, 5, 6), m.flatten());
        }

        @Test
        public void testReplaceIfIndexed_TallMatrixTraversesRowMajor() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            final int[] counter = { 0 };

            m.replaceIf((i, j) -> ++counter[0] <= 3, 9);

            assertEquals(List.of(9, 9, 9, 4, 5, 6), m.flatten());
        }

        @Test
        public void testReplaceIfVisitsDetachedRowsIndependently() {
            Integer[] sharedRow = { 1, 2 };
            Matrix<Integer> m = Matrix.copyOf(Integer.class, sharedRow, sharedRow);
            final int[] calls = { 0 };

            m.replaceIf(value -> {
                calls[0]++;
                return value > 0;
            }, 9);

            assertEquals(4, calls[0]);
            assertEquals(List.of(9, 9, 9, 9), m.flatten());
            assertArrayEquals(new Integer[] { 1, 2 }, sharedRow);
        }

        @Test
        public void testDiagonalAndReplaceIfNullCallbacks() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> emptyLike = Matrix.wrap(new Integer[][] { {}, {} });

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> matrix.updateMainDiagonal((Throwables.UnaryOperator<Integer, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> matrix.updateAntiDiagonal((Throwables.UnaryOperator<Integer, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.Predicate<Integer, RuntimeException>) null, 0));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0));

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> emptyLike.updateMainDiagonal((Throwables.UnaryOperator<Integer, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> emptyLike.updateAntiDiagonal((Throwables.UnaryOperator<Integer, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.Predicate<Integer, RuntimeException>) null, 0));
            Assertions.assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 0));
        }

        @Test
        public void testReplaceIf_ValuePredicate_Nulls() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", null, "c" }, { null, "e", null } });
            m.replaceIf(x -> x == null, "EMPTY");
            assertEquals("a", m.get(0, 0));
            assertEquals("EMPTY", m.get(0, 1));
            assertEquals("c", m.get(0, 2));
            assertEquals("EMPTY", m.get(1, 0));
        }
    }

    @Nested
    class MapTests {

        @Test
        public void testMap() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            Matrix<Integer> result = matrix.map(x -> x * 2);
            Assertions.assertEquals(2, result.get(0, 0));
            Assertions.assertEquals(4, result.get(0, 1));
            Assertions.assertEquals(6, result.get(1, 0));
            Assertions.assertEquals(8, result.get(1, 1));
        }

        @Test
        public void testMapToDifferentType() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            Matrix<String> result = matrix.map(Object::toString, String.class);
            Assertions.assertEquals("1", result.get(0, 0));
            Assertions.assertEquals("2", result.get(0, 1));
            Assertions.assertEquals("3", result.get(1, 0));
            Assertions.assertEquals("4", result.get(1, 1));
        }

        @Test
        public void testMap_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> result = m.map(x -> x + "1");
            assertEquals("A1", result.get(0, 0));
            assertEquals("B1", result.get(0, 1));
            assertEquals("C1", result.get(1, 0));
            assertEquals("D1", result.get(1, 1));

            // Original unchanged
            assertEquals("A", m.get(0, 0));
        }

        @Test
        public void testMap_WithTypeChange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<Integer> result = m.map(x -> x.charAt(0) - 'A', Integer.class);
            assertEquals(Integer.valueOf(0), result.get(0, 0));
            assertEquals(Integer.valueOf(1), result.get(0, 1));
            assertEquals(Integer.valueOf(2), result.get(1, 0));
            assertEquals(Integer.valueOf(3), result.get(1, 1));
        }

        @Test
        public void testMap_SameType_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            Matrix<String> mapped = m.map(String::toUpperCase);
            assertEquals("A", mapped.get(0, 0));
            assertEquals("B", mapped.get(0, 1));
            assertEquals("C", mapped.get(1, 0));
            assertEquals("D", mapped.get(1, 1));
        }

        @Test
        public void testMap_SameType_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> mapped = m.map(x -> x * 2);
            assertEquals(2, mapped.get(0, 0));
            assertEquals(4, mapped.get(0, 1));
            assertEquals(6, mapped.get(1, 0));
            assertEquals(8, mapped.get(1, 1));
        }

        @Test
        public void testMap_DifferentType_StringToInteger() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "ab", "abc" }, { "a", "abcd" } });
            Matrix<Integer> lengths = m.map(String::length, Integer.class);
            assertEquals(2, lengths.get(0, 0));
            assertEquals(3, lengths.get(0, 1));
            assertEquals(1, lengths.get(1, 0));
            assertEquals(4, lengths.get(1, 1));
        }

        @Test
        public void testMap_DifferentType_IntegerToString() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 22 }, { 333, 4444 } });
            Matrix<String> strings = m.map(String::valueOf, String.class);
            assertEquals("1", strings.get(0, 0));
            assertEquals("22", strings.get(0, 1));
            assertEquals("333", strings.get(1, 0));
            assertEquals("4444", strings.get(1, 1));
        }

        @Test
        public void testMap_DifferentType_IntegerToDouble() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Double> doubles = m.map(x -> x * 1.5, Double.class);
            assertEquals(1.5, doubles.get(0, 0));
            assertEquals(3.0, doubles.get(0, 1));
            assertEquals(4.5, doubles.get(1, 0));
            assertEquals(6.0, doubles.get(1, 1));
        }

        @Test
        public void testMap_CreatesNewMatrix() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            Matrix<Integer> result = m.map(x -> x * 3);
            assertEquals(3, result.get(0, 0));
            assertEquals(6, result.get(0, 1));
            assertEquals(9, result.get(1, 0));
            assertEquals(12, result.get(1, 1));
            assertEquals(1, m.get(0, 0)); // original unchanged
        }

        @Test
        public void testMap_WithTypeConversion() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            Matrix<String> result = m.map(x -> "val" + x, String.class);
            assertEquals("val1", result.get(0, 0));
            assertEquals("val4", result.get(1, 1));
        }

        @Test
        public void testMapNullMapper() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.map((Throwables.UnaryOperator<Integer, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.mapToBoolean((Throwables.ToBooleanFunction<Integer, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.map((Throwables.Function) null, String.class));
        }

        @Test
        public void testGenericMatrixRowsForZeroColumnMatrix() {
            final Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { {}, {}, {} });
            final List<List<Integer>> rows = matrix.rowStreams().map(row -> row.toList()).toList();

            assertEquals(3, rows.size());
            assertTrue(rows.get(0).isEmpty());
            assertTrue(rows.get(1).isEmpty());
            assertTrue(rows.get(2).isEmpty());
        }

        @Test
        public void testMap_SingleArg_AcceptsAnyDeclaredTypeWithCanonicalStorage() {
            Matrix<Object> m = Matrix.copyOf(Object.class, new String[][] { { "x" } });
            Matrix<Object> widened = m.map(o -> Integer.valueOf(5));
            assertEquals(Integer.valueOf(5), widened.get(0, 0));

            Matrix<Integer> ok = m.map(o -> Integer.valueOf(5), Integer.class);
            assertEquals(Integer.valueOf(5), ok.get(0, 0));
        }
    }

    @Nested
    class MapToBooleanTests {

        @Test
        public void testMapToBoolean() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            BooleanMatrix result = matrix.mapToBoolean(x -> x % 2 == 0);
            Assertions.assertFalse(result.get(0, 0));
            Assertions.assertTrue(result.get(0, 1));
            Assertions.assertFalse(result.get(1, 0));
            Assertions.assertTrue(result.get(1, 1));
        }

        @Test
        public void testMapToBoolean_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "true", "false" }, { "false", "true" } });
            BooleanMatrix result = m.mapToBoolean(x -> Boolean.parseBoolean(x));
            assertTrue(result.get(0, 0));
            assertFalse(result.get(0, 1));
            assertFalse(result.get(1, 0));
            assertTrue(result.get(1, 1));
        }

        @Test
        public void testMapToBoolean_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { -1, 2 }, { 3, -4 } });
            BooleanMatrix result = m.mapToBoolean(x -> x > 0);
            assertFalse(result.get(0, 0));
            assertTrue(result.get(0, 1));
            assertTrue(result.get(1, 0));
            assertFalse(result.get(1, 1));
        }

        @Test
        public void testMapToBoolean_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "hello", "hi" }, { "greetings", "hey" } });
            BooleanMatrix result = m.mapToBoolean(x -> x.length() > 3);
            assertTrue(result.get(0, 0));
            assertFalse(result.get(0, 1));
            assertTrue(result.get(1, 0));
            assertFalse(result.get(1, 1));
        }

        @Test
        public void testMapToBoolean_CreatesBooleanMatrix() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            BooleanMatrix result = m.mapToBoolean(x -> x > 2);
            assertFalse(result.get(0, 0));
            assertFalse(result.get(0, 1));
            assertTrue(result.get(1, 0));
            assertTrue(result.get(1, 1));
        }
    }

    @Nested
    class MapToByteTests {

        @Test
        public void testMapToByte() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            ByteMatrix result = matrix.mapToByte(Integer::byteValue);
            Assertions.assertEquals((byte) 1, result.get(0, 0));
            Assertions.assertEquals((byte) 2, result.get(0, 1));
            Assertions.assertEquals((byte) 3, result.get(1, 0));
            Assertions.assertEquals((byte) 4, result.get(1, 1));
        }

        @Test
        public void testMapToByte_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "1", "2" }, { "3", "4" } });
            ByteMatrix result = m.mapToByte(x -> Byte.parseByte(x));
            assertEquals((byte) 1, result.get(0, 0));
            assertEquals((byte) 2, result.get(0, 1));
            assertEquals((byte) 3, result.get(1, 0));
            assertEquals((byte) 4, result.get(1, 1));
        }

        @Test
        public void testMapToByte_Shape2By2_UnboxesEveryByteValue() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            ByteMatrix result = m.mapToByte(Integer::byteValue);
            assertEquals(1, result.get(0, 0));
            assertEquals(2, result.get(0, 1));
            assertEquals(3, result.get(1, 0));
            assertEquals(4, result.get(1, 1));
        }

        @Test
        public void testMapToByte_CreatesByteMatrix() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            ByteMatrix result = m.mapToByte(x -> (byte) x.intValue());
            assertEquals(1, result.get(0, 0));
            assertEquals(4, result.get(1, 1));
        }
    }

    @Nested
    class MapToCharTests {

        @Test
        public void testMapToChar() throws Exception {
            Matrix<String> matrix = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });

            CharMatrix result = matrix.mapToChar(s -> s.charAt(0));
            Assertions.assertEquals('a', result.get(0, 0));
            Assertions.assertEquals('b', result.get(0, 1));
            Assertions.assertEquals('c', result.get(1, 0));
            Assertions.assertEquals('d', result.get(1, 1));
        }

        @Test
        public void testMapToChar_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            CharMatrix result = m.mapToChar(x -> x.charAt(0));
            assertEquals('A', result.get(0, 0));
            assertEquals('B', result.get(0, 1));
            assertEquals('C', result.get(1, 0));
            assertEquals('D', result.get(1, 1));
        }

        @Test
        public void testMapToChar_Shape2By2_ConvertsEveryCharacterValue() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 65, 66 }, { 67, 68 } });
            CharMatrix result = m.mapToChar(x -> (char) x.intValue());
            assertEquals('A', result.get(0, 0));
            assertEquals('B', result.get(0, 1));
            assertEquals('C', result.get(1, 0));
            assertEquals('D', result.get(1, 1));
        }

        @Test
        public void testMapToChar_CreatesCharMatrix() {
            String[][] arr = { { "A", "B" }, { "C", "D" } };
            Matrix<String> m = new Matrix<>(arr);
            CharMatrix result = m.mapToChar(x -> x.charAt(0));
            assertEquals('A', result.get(0, 0));
            assertEquals('D', result.get(1, 1));
        }
    }

    @Nested
    class MapToShortTests {

        @Test
        public void testMapToShort() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            ShortMatrix result = matrix.mapToShort(Integer::shortValue);
            Assertions.assertEquals((short) 1, result.get(0, 0));
            Assertions.assertEquals((short) 2, result.get(0, 1));
            Assertions.assertEquals((short) 3, result.get(1, 0));
            Assertions.assertEquals((short) 4, result.get(1, 1));
        }

        @Test
        public void testMapToShort_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "10", "20" }, { "30", "40" } });
            ShortMatrix result = m.mapToShort(x -> Short.parseShort(x));
            assertEquals((short) 10, result.get(0, 0));
            assertEquals((short) 20, result.get(0, 1));
            assertEquals((short) 30, result.get(1, 0));
            assertEquals((short) 40, result.get(1, 1));
        }

        @Test
        public void testMapToShort_Shape2By2_UnboxesEveryShortValue() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            ShortMatrix result = m.mapToShort(Integer::shortValue);
            assertEquals(1, result.get(0, 0));
            assertEquals(2, result.get(0, 1));
            assertEquals(3, result.get(1, 0));
            assertEquals(4, result.get(1, 1));
        }

        @Test
        public void testMapToShort_CreatesShortMatrix() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            ShortMatrix result = m.mapToShort(x -> (short) x.intValue());
            assertEquals(1, result.get(0, 0));
            assertEquals(4, result.get(1, 1));
        }
    }

    @Nested
    class MapToIntTests {

        @Test
        public void testMapToInt() throws Exception {
            Matrix<String> matrix = Matrix.wrap(new String[][] { { "1", "12" }, { "123", "1234" } });

            IntMatrix result = matrix.mapToInt(String::length);
            Assertions.assertEquals(1, result.get(0, 0));
            Assertions.assertEquals(2, result.get(0, 1));
            Assertions.assertEquals(3, result.get(1, 0));
            Assertions.assertEquals(4, result.get(1, 1));
        }

        @Test
        public void testMapToInt_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "1", "2" }, { "3", "4" } });
            IntMatrix result = m.mapToInt(x -> Integer.parseInt(x));
            assertEquals(1, result.get(0, 0));
            assertEquals(2, result.get(0, 1));
            assertEquals(3, result.get(1, 0));
            assertEquals(4, result.get(1, 1));
        }

        @Test
        public void testMapToInt_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "ab", "abc" }, { "a", "abcd" } });
            IntMatrix result = m.mapToInt(String::length);
            assertEquals(2, result.get(0, 0));
            assertEquals(3, result.get(0, 1));
            assertEquals(1, result.get(1, 0));
            assertEquals(4, result.get(1, 1));
        }

        @Test
        public void testMapToInt_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m.mapToInt(x -> x * 10);
            assertEquals(10, result.get(0, 0));
            assertEquals(20, result.get(0, 1));
            assertEquals(30, result.get(1, 0));
            assertEquals(40, result.get(1, 1));
        }

        @Test
        public void testMapToInt_CreatesIntMatrix() {
            String[][] arr = { { "1", "2" }, { "3", "4" } };
            Matrix<String> m = new Matrix<>(arr);
            IntMatrix result = m.mapToInt(Integer::parseInt);
            assertEquals(1, result.get(0, 0));
            assertEquals(4, result.get(1, 1));
        }
    }

    @Nested
    class MapToLongTests {

        @Test
        public void testMapToLong() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            LongMatrix result = matrix.mapToLong(Integer::longValue);
            Assertions.assertEquals(1L, result.get(0, 0));
            Assertions.assertEquals(2L, result.get(0, 1));
            Assertions.assertEquals(3L, result.get(1, 0));
            Assertions.assertEquals(4L, result.get(1, 1));
        }

        @Test
        public void testMapToLong_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "100", "200" }, { "300", "400" } });
            LongMatrix result = m.mapToLong(x -> Long.parseLong(x));
            assertEquals(100L, result.get(0, 0));
            assertEquals(200L, result.get(0, 1));
            assertEquals(300L, result.get(1, 0));
            assertEquals(400L, result.get(1, 1));
        }

        @Test
        public void testMapToLong_Shape2By2_UnboxesEveryLongValue() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.mapToLong(Integer::longValue);
            assertEquals(1L, result.get(0, 0));
            assertEquals(2L, result.get(0, 1));
            assertEquals(3L, result.get(1, 0));
            assertEquals(4L, result.get(1, 1));
        }

        @Test
        public void testMapToLong_CreatesLongMatrix() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            LongMatrix result = m.mapToLong(x -> x.longValue());
            assertEquals(1L, result.get(0, 0));
            assertEquals(4L, result.get(1, 1));
        }
    }

    @Nested
    class MapToFloatTests {

        @Test
        public void testMapToFloat() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            FloatMatrix result = matrix.mapToFloat(Integer::floatValue);
            Assertions.assertEquals(1.0f, result.get(0, 0));
            Assertions.assertEquals(2.0f, result.get(0, 1));
            Assertions.assertEquals(3.0f, result.get(1, 0));
            Assertions.assertEquals(4.0f, result.get(1, 1));
        }

        @Test
        public void testMapToFloat_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "1.5", "2.5" }, { "3.5", "4.5" } });
            FloatMatrix result = m.mapToFloat(x -> Float.parseFloat(x));
            assertEquals(1.5f, result.get(0, 0), 0.001f);
            assertEquals(2.5f, result.get(0, 1), 0.001f);
            assertEquals(3.5f, result.get(1, 0), 0.001f);
            assertEquals(4.5f, result.get(1, 1), 0.001f);
        }

        @Test
        public void testMapToFloat_Shape2By2_UnboxesEveryFloatValue() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            FloatMatrix result = m.mapToFloat(Integer::floatValue);
            assertEquals(1.0f, result.get(0, 0), 0.001f);
            assertEquals(2.0f, result.get(0, 1), 0.001f);
            assertEquals(3.0f, result.get(1, 0), 0.001f);
            assertEquals(4.0f, result.get(1, 1), 0.001f);
        }

        @Test
        public void testMapToFloat_CreatesFloatMatrix() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            FloatMatrix result = m.mapToFloat(x -> x.floatValue());
            assertEquals(1.0f, result.get(0, 0));
            assertEquals(4.0f, result.get(1, 1));
        }
    }

    @Nested
    class MapToDoubleTests {

        @Test
        public void testMapToDouble() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            DoubleMatrix result = matrix.mapToDouble(Integer::doubleValue);
            Assertions.assertEquals(1.0, result.get(0, 0));
            Assertions.assertEquals(2.0, result.get(0, 1));
            Assertions.assertEquals(3.0, result.get(1, 0));
            Assertions.assertEquals(4.0, result.get(1, 1));
        }

        @Test
        public void testMapToDouble_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "1.5", "2.5" }, { "3.5", "4.5" } });
            DoubleMatrix result = m.mapToDouble(x -> Double.parseDouble(x));
            assertEquals(1.5, result.get(0, 0), 0.001);
            assertEquals(2.5, result.get(0, 1), 0.001);
            assertEquals(3.5, result.get(1, 0), 0.001);
            assertEquals(4.5, result.get(1, 1), 0.001);
        }

        @Test
        public void testMapToDouble_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.mapToDouble(Integer::doubleValue);
            assertEquals(1.0, result.get(0, 0), 0.001);
            assertEquals(2.0, result.get(0, 1), 0.001);
            assertEquals(3.0, result.get(1, 0), 0.001);
            assertEquals(4.0, result.get(1, 1), 0.001);
        }

        @Test
        public void testMapToDouble_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "1.5", "2.5" }, { "3.5", "4.5" } });
            DoubleMatrix result = m.mapToDouble(Double::parseDouble);
            assertEquals(1.5, result.get(0, 0), 0.001);
            assertEquals(2.5, result.get(0, 1), 0.001);
            assertEquals(3.5, result.get(1, 0), 0.001);
            assertEquals(4.5, result.get(1, 1), 0.001);
        }

        @Test
        public void testMapToDouble_CreatesDoubleMatrix() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            DoubleMatrix result = m.mapToDouble(x -> x.doubleValue());
            assertEquals(1.0, result.get(0, 0));
            assertEquals(4.0, result.get(1, 1));
        }
    }

    @Nested
    class FillTests {

        @Test
        public void testFill() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            matrix.fill(0);
            Assertions.assertEquals(0, matrix.get(0, 0));
            Assertions.assertEquals(0, matrix.get(0, 1));
            Assertions.assertEquals(0, matrix.get(1, 0));
            Assertions.assertEquals(0, matrix.get(1, 1));
        }

        @Test
        public void testFill_WithValue() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.fill("X");
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals("X", m.get(i, j));
                }
            }
        }

        @Test
        public void testFill_WithValue_Values() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            m.fill("X");
            assertEquals("X", m.get(0, 0));
            assertEquals("X", m.get(0, 1));
            assertEquals("X", m.get(1, 0));
            assertEquals("X", m.get(1, 1));
        }

        @Test
        public void testFill_SingleValue_Null() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.fill(null);
            assertNull(m.get(0, 0));
            assertNull(m.get(0, 1));
            assertNull(m.get(1, 0));
            assertNull(m.get(1, 1));
        }

        @Test
        public void testFill_SingleValue_String() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            m.fill("X");
            assertEquals("X", m.get(0, 0));
            assertEquals("X", m.get(0, 1));
            assertEquals("X", m.get(1, 0));
            assertEquals("X", m.get(1, 1));
        }

        @Test
        public void testFill_SingleValue_Integer() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            m.fill(99);
            assertEquals(99, m.get(0, 0));
            assertEquals(99, m.get(0, 1));
            assertEquals(99, m.get(1, 0));
            assertEquals(99, m.get(1, 1));
        }
    }

    @Nested
    class CopyFromTests {

        @Test
        public void testCopyFrom() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            Integer[][] patch = { { 7, 8 } };
            matrix.copyFrom(patch);
            Assertions.assertEquals(7, matrix.get(0, 0));
            Assertions.assertEquals(8, matrix.get(0, 1));
            Assertions.assertEquals(3, matrix.get(0, 2)); // unchanged
            Assertions.assertEquals(4, matrix.get(1, 0)); // unchanged
        }

        @Test
        public void testCopyFromWithPosition() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            Integer[][] patch = { { 10, 11 }, { 12, 13 } };
            matrix.copyFrom(1, 1, patch);
            Assertions.assertEquals(1, matrix.get(0, 0)); // unchanged
            Assertions.assertEquals(10, matrix.get(1, 1));
            Assertions.assertEquals(11, matrix.get(1, 2));
            Assertions.assertEquals(12, matrix.get(2, 1));
            Assertions.assertEquals(13, matrix.get(2, 2));
        }

        @Test
        public void testCopyFromWidensStorageWhenNeeded() {
            Matrix<Number> matrix = Matrix.<Number> wrap(new Double[][] { { 1D, 1D } });
            matrix.rowView(0);

            matrix.fill(2.5d);
            Assertions.assertEquals(2.5d, matrix.get(0, 0).doubleValue(), 0.000001d);
            Assertions.assertEquals(2.5d, matrix.get(0, 1).doubleValue(), 0.000001d);

            matrix.copyFrom(new Number[][] { { 3.5d, 4.5d } });
            Assertions.assertEquals(3.5d, matrix.get(0, 0).doubleValue(), 0.000001d);
            Assertions.assertEquals(4.5d, matrix.get(0, 1).doubleValue(), 0.000001d);
        }

        @Test
        public void testCopyFrom_Shape3By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "A", "A" }, { "A", "A", "A" }, { "A", "A", "A" } });
            String[][] patch = { { "X", "Y" }, { "Z", "W" } };
            m.copyFrom(patch);
            assertEquals("X", m.get(0, 0));
            assertEquals("Y", m.get(0, 1));
            assertEquals("Z", m.get(1, 0));
            assertEquals("W", m.get(1, 1));
            assertEquals("A", m.get(2, 2)); // unchanged
        }

        @Test
        public void testCopyFromAtPosition() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "A", "A" }, { "A", "A", "A" }, { "A", "A", "A" } });
            String[][] patch = { { "X", "Y" }, { "Z", "W" } };
            m.copyFrom(1, 1, patch);
            assertEquals("A", m.get(0, 0)); // unchanged
            assertEquals("X", m.get(1, 1));
            assertEquals("Y", m.get(1, 2));
            assertEquals("Z", m.get(2, 1));
            assertEquals("W", m.get(2, 2));
        }

        @Test
        public void testCopyFrom_LargerSource() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String[][] largeSource = { { "X", "Y", "Z" }, { "W", "V", "U" }, { "T", "S", "R" } };
            m.copyFrom(largeSource);
            assertEquals("X", m.get(0, 0));
            assertEquals("Y", m.get(0, 1));
            assertEquals("W", m.get(1, 0));
            assertEquals("V", m.get(1, 1));
        }

        @Test
        public void testCopyFrom_AtEdgePosition() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String[][] patch = { { "X", "Y" } };
            m.copyFrom(1, 0, patch);
            assertEquals("A", m.get(0, 0));
            assertEquals("X", m.get(1, 0));
            assertEquals("Y", m.get(1, 1));
        }

        @Test
        public void testCopyFrom_Partial() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            String[][] patch = { { "X", "Y" } };
            m.copyFrom(patch);
            assertEquals("X", m.get(0, 0));
            assertEquals("Y", m.get(0, 1));
            assertEquals("C", m.get(0, 2)); // Unchanged
            assertEquals("D", m.get(1, 0)); // Unchanged
        }

        @Test
        public void testCopyFrom_WithOffset() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            String[][] patch = { { "X", "Y" } };
            m.copyFrom(1, 1, patch);
            assertEquals("A", m.get(0, 0));
            assertEquals("X", m.get(1, 1));
            assertEquals("Y", m.get(1, 2));
            assertEquals("G", m.get(2, 0)); // Unchanged
        }

        @Test
        public void testCopyFrom_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String[][] newData = { { "X", "Y" }, { "Z", "W" } };
            m.copyFrom(newData);
            assertEquals("X", m.get(0, 0));
            assertEquals("Y", m.get(0, 1));
            assertEquals("Z", m.get(1, 0));
            assertEquals("W", m.get(1, 1));
        }

        @Test
        public void testCopyFrom_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Integer[][] newData = { { 10, 20 }, { 30, 40 } };
            m.copyFrom(newData);
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(0, 1));
            assertEquals(30, m.get(1, 0));
            assertEquals(40, m.get(1, 1));
        }

        @Test
        public void testCopyFrom_Values() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            String[][] b = { { "X", "Y" }, { "Z", "W" } };
            m.copyFrom(b);
            assertEquals("X", m.get(0, 0));
            assertEquals("W", m.get(1, 1));
        }

        @Test
        public void testCopyFromAndOffset() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" }, { "g", "h", "i" } };
            Matrix<String> m = new Matrix<>(arr);
            String[][] b = { { "X", "Y" } };
            m.copyFrom(1, 1, b);
            assertEquals("a", m.get(0, 0));
            assertEquals("X", m.get(1, 1));
            assertEquals("Y", m.get(1, 2));
        }

        // Regression tests for the deep-review fixes: diagonal stream square validation,
        @Test
        public void testCopyFrom_FullOverwrite() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "x", "x" }, { "x", "x" } });
            m.copyFrom(new String[][] { { "a", "b" }, { "c", "d" } });
            assertEquals("a", m.get(0, 0));
            assertEquals("b", m.get(0, 1));
            assertEquals("c", m.get(1, 0));
            assertEquals("d", m.get(1, 1));
        }

        @Test
        public void testCopyFrom_PartialOverwrite() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "x", "x", "x" }, { "x", "x", "x" }, { "x", "x", "x" } });
            m.copyFrom(new String[][] { { "a", "b" }, { "c", "d" } });
            assertEquals("a", m.get(0, 0));
            assertEquals("b", m.get(0, 1));
            assertEquals("x", m.get(0, 2));
            assertEquals("c", m.get(1, 0));
            assertEquals("d", m.get(1, 1));
            assertEquals("x", m.get(2, 0));
        }

        @Test
        public void testCopyFrom_WithOffset_Shape3By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "x", "x", "x" }, { "x", "x", "x" }, { "x", "x", "x" } });
            m.copyFrom(1, 1, new String[][] { { "a", "b" }, { "c", "d" } });
            assertEquals("x", m.get(0, 0));
            assertEquals("x", m.get(1, 0));
            assertEquals("a", m.get(1, 1));
            assertEquals("b", m.get(1, 2));
            assertEquals("c", m.get(2, 1));
            assertEquals("d", m.get(2, 2));
        }

        @Test
        public void testCopyFrom_EmptySource() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String[][] emptySource = {};
            m.copyFrom(emptySource);
            assertEquals("A", m.get(0, 0));
            assertEquals("D", m.get(1, 1));
        }

        @Test
        public void testCopyFrom_AtBoundary() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "A", "A" }, { "A", "A", "A" }, { "A", "A", "A" } });
            String[][] patch = { { "X" } };
            m.copyFrom(2, 2, patch);
            assertEquals("A", m.get(0, 0));
            assertEquals("X", m.get(2, 2));
        }

        @Test
        public void testCopyFrom_EmptySource_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            m.copyFrom(new String[0][0]);
            assertEquals("a", m.get(0, 0));
            assertEquals("d", m.get(1, 1));
        }

        @Test
        public void testCopyFrom_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String[][] patch = { { "X", "Y" }, { "Z", "W" } };
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
        }

        @Test
        public void testCopyFrom_WithOffset_InvalidIndices() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            String[][] patch = { { "X" } };
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(0, -1, patch));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(2, 0, patch));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(0, 3, patch));
        }

        @Test
        public void testCopyFrom_NegativeIndexThrows() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, new String[][] { { "b" } }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(0, -1, new String[][] { { "b" } }));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0", "3, 0", "0, -1", "0, 3" })
        public void testCopyFrom_RejectsOffsetsOutsideDestination(final int row, final int column) {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(row, column, new Integer[][] { { 1 } }));
            assertEquals(Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } }), matrix);
        }
    }

    @Nested
    class CopyTests {

        @Test
        public void testReplacementPredicatesMayStopBeforeVisitingEveryCell() {
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                final Matrix<Integer> byValue = Matrix.wrap(new Integer[][] { { 1, 2, 3 } });
                final Matrix<Integer> byPosition = byValue.copy();
                final IllegalStateException failure = new IllegalStateException("stop");
                final List<Integer> visitedValues = new ArrayList<>();
                final List<Integer> visitedColumns = new ArrayList<>();

                Assertions.assertSame(failure, assertThrows(IllegalStateException.class, () -> byValue.replaceIf(value -> {
                    visitedValues.add(value);
                    if (value == 2) {
                        throw failure;
                    }
                    return true;
                }, 10)));
                Assertions.assertSame(failure, assertThrows(IllegalStateException.class, () -> byPosition.replaceIf((i, j) -> {
                    visitedColumns.add(j);
                    if (j == 1) {
                        throw failure;
                    }
                    return true;
                }, 10)));

                assertEquals(List.of(1, 2), visitedValues);
                assertEquals(List.of(0, 1), visitedColumns);
                assertArrayEquals(new Integer[] { 10, 2, 3 }, byValue.rowCopy(0));
                assertArrayEquals(new Integer[] { 10, 2, 3 }, byPosition.rowCopy(0));
            });
        }

        @Test
        public void testCopy() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            Matrix<Integer> copy = matrix.copy();
            Assertions.assertEquals(matrix.get(0, 0), copy.get(0, 0));
            Assertions.assertEquals(matrix.get(1, 1), copy.get(1, 1));

            copy.set(0, 0, 10);
            Assertions.assertEquals(1, matrix.get(0, 0)); // original unchanged
            Assertions.assertEquals(10, copy.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals("A", copy.get(0, 0));

            // Modify copy shouldn't affect original
            copy.set(0, 0, "X");
            assertEquals("A", m.get(0, 0));
            assertEquals("X", copy.get(0, 0));
        }

        @Test
        public void testCopy_Strings() {
            Matrix<String> original = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> copy = original.copy();

            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals("A", copy.get(0, 0));
            assertEquals("D", copy.get(1, 1));

            copy.set(0, 0, "X");
            assertEquals("A", original.get(0, 0));
            assertEquals("X", copy.get(0, 0));
        }

        @Test
        public void testCopy_Integers() {
            Matrix<Integer> original = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> copy = original.copy();

            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(1, copy.get(0, 0));
            assertEquals(4, copy.get(1, 1));

            copy.set(0, 0, 99);
            assertEquals(1, original.get(0, 0));
            assertEquals(99, copy.get(0, 0));
        }

        @Test
        public void testCopy_CreatesIndependentCopy() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals("a", copy.get(0, 0));

            copy.set(0, 0, "CHANGED");
            assertEquals("a", m.get(0, 0)); // original unchanged
        }

        @Test
        public void testBoundedCopySnapshotsPreserveOverlappingSourceValuesAndNoOpValidation() {
            final Matrix<String> matrix = Matrix.wrap(String.class, new String[][] { { "a", "b", "c" }, { "d", "e", "f" }, { "g", "h", "i" } });
            matrix.copyFrom(1, 1, new String[][] { matrix.rowView(0), matrix.rowView(1), matrix.rowView(2) });
            assertArrayEquals(new String[] { "d", "a", "b" }, matrix.rowView(1));
            assertArrayEquals(new String[] { "g", "d", "e" }, matrix.rowView(2));

            final Matrix<String> beforeNoOps = matrix.copy();
            matrix.copyFrom(matrix.rowCount(), 0, new String[][] { matrix.rowView(0) });
            matrix.copyFrom(0, matrix.columnCount(), new String[][] { matrix.rowView(0) });
            matrix.copyFrom(0, 0, new String[0][]);
            assertEquals(beforeNoOps, matrix);
            assertThrows(IllegalArgumentException.class, () -> matrix.copyFrom(matrix.rowCount(), 0, null));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(-1, 0, null));

            final Matrix<String> diagonal = Matrix.wrap(String.class, new String[][] { { "a", "b", "c" }, { "d", "e", "f" }, { "g", "h", "i" } });
            diagonal.setAntiDiagonal(diagonal.rowView(0));
            assertArrayEquals(new String[] { "a", "b", "c" }, diagonal.antiDiagonalCopy());
            final Matrix<String> tall = Matrix.wrap(String.class, new String[][] { { "a", "b" }, { "c", "d" }, { "e", "f" } });
            tall.setAntiDiagonal(tall.rowView(2));
            assertArrayEquals(new String[] { "e", "f" }, tall.antiDiagonalCopy());
            assertArrayEquals(new String[] { "e", "f" }, tall.rowView(2));
        }

        @Test
        public void testCopy_EmptyMatrix() {
            Matrix<String> empty = Matrix.wrap(new String[0][0]);
            Matrix<String> copy = empty.copy();
            assertTrue(copy.isEmpty());
        }
    }

    @Nested
    class CopyRowsTests {

        @Test
        public void testCopyRows() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });

            Matrix<Integer> copy = matrix.copyRows(0, 2);
            Assertions.assertEquals(2, copy.rowCount());
            Assertions.assertEquals(2, copy.columnCount());
            Assertions.assertEquals(1, copy.get(0, 0));
            Assertions.assertEquals(4, copy.get(1, 1));
        }

        @Test
        public void testCopyRows_Shape3By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            Matrix<String> subset = m.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertEquals("D", subset.get(0, 0));
            assertEquals("I", subset.get(1, 2));
        }

        @Test
        public void testCopyRows_Shape3By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" }, { "E", "F" } });
            Matrix<String> subset = m.copyRows(1, 3);

            assertEquals(2, subset.rowCount());
            assertEquals(2, subset.columnCount());
            assertEquals("C", subset.get(0, 0));
            assertEquals("F", subset.get(1, 1));
        }

        @Test
        public void testCopyRows_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            Matrix<Integer> subset = m.copyRows(0, 2);

            assertEquals(2, subset.rowCount());
            assertEquals(2, subset.columnCount());
            assertEquals(1, subset.get(0, 0));
            assertEquals(4, subset.get(1, 1));
        }

        @Test
        public void testCopyRows_Values() {
            String[][] arr = { { "a", "b" }, { "c", "d" }, { "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals("c", copy.get(0, 0));
            assertEquals("f", copy.get(1, 1));
        }

        @Test
        public void testCopyRangesWithZeroRowsPreserveRequestedColumns() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            Matrix<Integer> empty = m.copyRows(0, 0);
            Assertions.assertEquals(0, empty.rowCount());
            Assertions.assertEquals(3, empty.columnCount());

            Matrix<Integer> emptyRows = m.copyRegion(1, 1, 0, 3);
            Assertions.assertEquals(0, emptyRows.rowCount());
            Assertions.assertEquals(3, emptyRows.columnCount());

            Matrix<Integer> emptyCols = m.copyRegion(0, 3, 1, 1);
            Assertions.assertEquals(3, emptyCols.rowCount());
            Assertions.assertEquals(0, emptyCols.columnCount());
        }

        @Test
        public void testCopy_SingleRow() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" } });
            Matrix<String> copy = m.copyRows(0, 1);
            assertEquals(1, copy.rowCount());
            assertEquals(3, copy.columnCount());
            assertEquals("A", copy.get(0, 0));
        }

        @Test
        public void testCopyRows_SingleRow() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" }, { "E", "F" } });
            Matrix<String> subset = m.copyRows(1, 2);

            assertEquals(1, subset.rowCount());
            assertEquals(2, subset.columnCount());
            assertEquals("C", subset.get(0, 0));
        }

        @Test
        public void testCopyRows_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(2, 1));
        }
    }

    @Nested
    class CopyRegionTests {

        @Test
        public void testCopyRegion() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            Matrix<Integer> copy = matrix.copyRegion(1, 3, 1, 3);
            Assertions.assertEquals(2, copy.rowCount());
            Assertions.assertEquals(2, copy.columnCount());
            Assertions.assertEquals(5, copy.get(0, 0));
            Assertions.assertEquals(6, copy.get(0, 1));
            Assertions.assertEquals(8, copy.get(1, 0));
            Assertions.assertEquals(9, copy.get(1, 1));
        }

        @Test
        public void testCopyRegion_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            Matrix<String> submatrix = m.copyRegion(0, 2, 1, 3);

            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals("B", submatrix.get(0, 0));
            assertEquals("C", submatrix.get(0, 1));
            assertEquals("E", submatrix.get(1, 0));
            assertEquals("F", submatrix.get(1, 1));
        }

        @Test
        public void testCopyRegion_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            Matrix<Integer> submatrix = m.copyRegion(1, 3, 1, 3);

            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals(5, submatrix.get(0, 0));
            assertEquals(6, submatrix.get(0, 1));
            assertEquals(8, submatrix.get(1, 0));
            assertEquals(9, submatrix.get(1, 1));
        }

        @Test
        public void testCopyRegion_Values() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> copy = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals("b", copy.get(0, 0));
            assertEquals("f", copy.get(1, 1));
        }

        @Test
        public void testCopy_SingleElement() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> copy = m.copyRegion(0, 1, 0, 1);
            assertEquals(1, copy.rowCount());
            assertEquals(1, copy.columnCount());
            assertEquals("A", copy.get(0, 0));
        }

        @Test
        public void testCopyRegion_SingleElement() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> single = m.copyRegion(1, 2, 1, 2);

            assertEquals(1, single.rowCount());
            assertEquals(1, single.columnCount());
            assertEquals("D", single.get(0, 0));
        }

        @Test
        public void testCopyRegionAndStackHorizontally_ResultRowsIndependentAndExactlyTyped() {
            final Matrix<Number> legacy = Matrix.wrap(new Number[][] { new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 5, 6 } });

            final Matrix<Number> zeroWidth = legacy.copyRegion(0, 3, 1, 1);
            assertEquals(3, zeroWidth.rowCount());
            assertEquals(0, zeroWidth.columnCount());
            assertNotSame(zeroWidth.rowView(0), zeroWidth.rowView(1));
            assertNotSame(zeroWidth.rowView(1), zeroWidth.rowView(2));

            final Matrix<Number> region = legacy.copyRegion(1, 3, 0, 2);
            assertEquals(Number[].class, region.rowView(0).getClass());
            region.set(0, 0, 2.5d);
            assertEquals(3, legacy.get(1, 0));

            final Matrix<Number> stacked = legacy.stackHorizontally(legacy, Number.class);
            assertEquals(4, stacked.columnCount());
            assertNotSame(stacked.rowView(0), stacked.rowView(1));
            assertEquals(Number[].class, stacked.rowView(2).getClass());
            stacked.set(2, 3, 7.5d);
            assertArrayEquals(new Number[] { 5, 6, 5, 7.5d }, stacked.rowCopy(2));
            assertArrayEquals(new Number[] { 5, 6 }, legacy.rowCopy(2));
        }

        @Test
        public void testCopyRegion_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, 0, 3));
        }
    }

    @Nested
    class ResizeTests {

        @Test
        public void testResize_Expand() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            Matrix<String> resized = m.resize(3, 3);
            assertEquals(3, resized.rowCount());
            assertEquals(3, resized.columnCount());
            assertEquals("a", resized.get(0, 0));
            assertEquals("b", resized.get(0, 1));
            assertNull(resized.get(0, 2));
            assertEquals("c", resized.get(1, 0));
            assertNull(resized.get(2, 0));
            assertNull(resized.get(2, 2));
        }

        @Test
        public void testResize_Shrink() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b", "c" }, { "d", "e", "f" }, { "g", "h", "i" } });
            Matrix<String> resized = m.resize(2, 2);
            assertEquals(2, resized.rowCount());
            assertEquals(2, resized.columnCount());
            assertEquals("a", resized.get(0, 0));
            assertEquals("b", resized.get(0, 1));
            assertEquals("d", resized.get(1, 0));
            assertEquals("e", resized.get(1, 1));
        }

        @Test
        public void testResize_WithDefaultValue() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1 } });
            Matrix<Integer> resized = m.resize(2, 3, 0);
            assertEquals(2, resized.rowCount());
            assertEquals(3, resized.columnCount());
            assertEquals(1, resized.get(0, 0));
            assertEquals(0, resized.get(0, 1));
            assertEquals(0, resized.get(0, 2));
            assertEquals(0, resized.get(1, 0));
            assertEquals(0, resized.get(1, 1));
            assertEquals(0, resized.get(1, 2));
        }

        @Test
        public void testResize_SameSize() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> resized = m.resize(2, 2);
            assertEquals(2, resized.rowCount());
            assertEquals(1, resized.get(0, 0));
            assertEquals(4, resized.get(1, 1));
        }

        @Test
        public void testResizeWithDefaultValue_WidensHeterogeneousRowsBeforeFilling() {
            Matrix<Object> m = Matrix.wrap(new Object[] { 1 }, new Integer[] { 2 });

            Matrix<Object> resized = m.resize(2, 2, "x");

            assertEquals(1, resized.get(0, 0));
            assertEquals("x", resized.get(0, 1));
            assertEquals(2, resized.get(1, 0));
            assertEquals("x", resized.get(1, 1));
            assertEquals(Object.class, resized.rowView(0).getClass().getComponentType());
            assertEquals(Object.class, resized.rowView(1).getClass().getComponentType());
        }

        @Test
        public void testResize() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            Matrix<Integer> extended = matrix.resize(3, 3);
            Assertions.assertEquals(3, extended.rowCount());
            Assertions.assertEquals(3, extended.columnCount());
            Assertions.assertEquals(1, extended.get(0, 0));
            Assertions.assertEquals(4, extended.get(1, 1));
            Assertions.assertNull(extended.get(2, 2));
        }

        @Test
        public void testResizeWithDefault() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            Matrix<Integer> extended = matrix.resize(3, 3, 0);
            Assertions.assertEquals(3, extended.rowCount());
            Assertions.assertEquals(3, extended.columnCount());
            Assertions.assertEquals(1, extended.get(0, 0));
            Assertions.assertEquals(4, extended.get(1, 1));
            Assertions.assertEquals(0, extended.get(2, 2));
            Assertions.assertEquals(0, extended.get(0, 2));
            Assertions.assertEquals(0, extended.get(2, 0));
        }

        @Test
        public void testResize_Larger() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> extended = m.resize(4, 4);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals("A", extended.get(0, 0));
            assertEquals("D", extended.get(1, 1));
            assertNull(extended.get(3, 3)); // new cells are null
        }

        @Test
        public void testResize_Smaller() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            Matrix<String> truncated = m.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
            assertEquals("A", truncated.get(0, 0));
            assertEquals("E", truncated.get(1, 1));
        }

        @Test
        public void testResize_Smaller_NonSquare() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            Matrix<String> result = m.resize(1, 2);
            assertEquals(1, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals("A", result.get(0, 0));
            assertEquals("B", result.get(0, 1));
        }

        @Test
        public void testResize_LargerRows_SmallerCols() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> result = m.resize(3, 1, "X");
            assertEquals(3, result.rowCount());
            assertEquals(1, result.columnCount());
            assertEquals("A", result.get(0, 0));
            assertEquals("C", result.get(1, 0));
            assertEquals("X", result.get(2, 0));
        }

        @Test
        public void testResize_Smaller_Shape3By3() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            Matrix<Integer> truncated = m.resize(2, 2);

            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
            assertEquals(1, truncated.get(0, 0));
            assertEquals(5, truncated.get(1, 1));
        }

        @Test
        public void testResize_Larger_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> extended = m.resize(3, 3);

            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(4, extended.get(1, 1));
            assertNull(extended.get(2, 2));
            assertNull(extended.get(0, 2));
        }

        @Test
        public void testResize_Larger_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> extended = m.resize(3, 3);

            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals("A", extended.get(0, 0));
            assertEquals("D", extended.get(1, 1));
            assertNull(extended.get(2, 2));
        }

        @Test
        public void testResize_Larger_WithDefault_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> extended = m.resize(3, 3, 0);

            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(4, extended.get(1, 1));
            assertEquals(0, extended.get(2, 2));
            assertEquals(0, extended.get(0, 2));
        }

        @Test
        public void testResize_Larger_WithDefault_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> extended = m.resize(3, 3, "X");

            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals("A", extended.get(0, 0));
            assertEquals("D", extended.get(1, 1));
            assertEquals("X", extended.get(2, 2));
            assertEquals("X", extended.get(0, 2));
        }

        @Test
        public void testResize_IncreasesSize() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> extended = m.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals("a", extended.get(0, 0));
            assertNull(extended.get(2, 2));
        }

        @Test
        public void testResize_WithDefaultValue_Values() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> extended = m.resize(3, 3, "X");
            assertEquals("X", extended.get(2, 2));
        }

        // Mirrors the defensive `String str = ""` guard applied to BooleanMatrix/ByteMatrix/CharMatrix/
        @Test
        public void testResize_ToEmpty() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            Matrix<String> resized = m.resize(0, 0);
            assertEquals(0, resized.rowCount());
            assertTrue(resized.isEmpty());
        }

        @Test
        public void testHorizontalStackTreatsSharedEmptyAsRuntimeTypeNeutral() {
            Matrix<String> empty = Matrix.empty(String.class);
            Matrix<String> typedEmpty = Matrix.wrap(new String[0][0]);

            Matrix<String> emptyFirst = empty.stackHorizontally(typedEmpty).resize(1, 0);
            Matrix<String> emptyLast = typedEmpty.stackHorizontally(empty).resize(1, 0);

            String[] firstRow = emptyFirst.rowView(0);
            String[] lastRow = emptyLast.rowView(0);
            assertEquals(String.class, firstRow.getClass().getComponentType());
            assertEquals(String.class, lastRow.getClass().getComponentType());
        }

        @Test
        public void testRepeatedSharedEmptyStacksRemainRuntimeTypeNeutral() {
            Matrix<String> typedEmpty = Matrix.wrap(new String[0][0]);

            Matrix<String> vertical = Matrix.empty(String.class).stackVertically(Matrix.empty(String.class)).stackVertically(typedEmpty).resize(1, 1);
            Matrix<String> horizontal = Matrix.empty(String.class).stackHorizontally(Matrix.empty(String.class)).stackHorizontally(typedEmpty).resize(1, 1);

            String[] verticalRow = vertical.rowView(0);
            String[] horizontalRow = horizontal.rowView(0);
            assertEquals(String.class, verticalRow.getClass().getComponentType());
            assertEquals(String.class, horizontalRow.getClass().getComponentType());
        }

        @Test
        public void testResize_GrowsRowsWhileTrimmingColumns() {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(Matrix.wrap(new Integer[][] { { 1 }, { 3 }, { 9 } }), matrix.resize(3, 1, 9));
            assertEquals(Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } }), matrix);
        }

        @Test
        public void testResize_NegativeThrows() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a" } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> m.resize(1, -1));
        }

        @Test
        public void testResize_WithNegativeDimensions() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 3, "X"));
            assertThrows(IllegalArgumentException.class, () -> m.resize(3, -1, "X"));
        }
    }

    @Nested
    class PadTests {

        @Test
        public void testPadDirectional() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            Matrix<Integer> extended = matrix.pad(1, 1, 1, 1, 0);
            Assertions.assertEquals(4, extended.rowCount());
            Assertions.assertEquals(4, extended.columnCount());
            Assertions.assertEquals(0, extended.get(0, 0));
            Assertions.assertEquals(1, extended.get(1, 1));
            Assertions.assertEquals(4, extended.get(2, 2));
            Assertions.assertEquals(0, extended.get(3, 3));
        }

        @Test
        public void testPad_Directional() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            m.println();
            Matrix<String> extended = m.pad(1, 1, 2, 2);
            assertEquals(5, extended.rowCount()); // 1 + 3 + 1
            assertEquals(7, extended.columnCount()); // 2 + 3 + 2

            extended.println();

            // Original values at offset position
            assertEquals("A", extended.get(1, 2));
            assertEquals("F", extended.get(2, 4));

            // New cells are null
            assertNull(extended.get(0, 0));
        }

        @Test
        public void testPad_DirectionalWithDefault() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            Matrix<String> extended = m.pad(1, 1, 1, 1, "X");
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());

            // Check original values
            assertEquals("A", extended.get(1, 1));

            // Check new values
            assertEquals("X", extended.get(0, 0));
            assertEquals("X", extended.get(4, 4));
        }

        @Test
        public void testPad_WithOnlyUp() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> extended = m.pad(2, 0, 0, 0, "X");
            assertEquals(4, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals("X", extended.get(0, 0));
            assertEquals("X", extended.get(1, 0));
            assertEquals("A", extended.get(2, 0));
        }

        @Test
        public void testPad_WithOnlyLeft() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> extended = m.pad(0, 0, 2, 0, "X");
            assertEquals(2, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals("X", extended.get(0, 0));
            assertEquals("X", extended.get(0, 1));
            assertEquals("A", extended.get(0, 2));
        }

        @Test
        public void testPad_DirectionBased_WithDefault() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> extended = m.pad(1, 1, 1, 1, 99);

            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(99, extended.get(0, 0)); // New top row
            assertEquals(1, extended.get(1, 1)); // Original top-left
            assertEquals(4, extended.get(2, 2)); // Original bottom-right
        }

        @Test
        public void testPad_DirectionBased_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> extended = m.pad(1, 1, 1, 1);

            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertNull(extended.get(0, 0)); // New top row
            assertEquals(1, extended.get(1, 1)); // Original top-left
            assertEquals(4, extended.get(2, 2)); // Original bottom-right
        }

        @Test
        public void testPad_DirectionBased_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A" } });
            Matrix<String> extended = m.pad(1, 1, 1, 1, "X");

            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals("X", extended.get(0, 0));
            assertEquals("A", extended.get(1, 1));
            assertEquals("X", extended.get(2, 2));
        }

        @Test
        public void testPad_WithDirections() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> extended = m.pad(1, 1, 1, 1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals("a", extended.get(1, 1));
        }

        @Test
        public void testPad_WithDirectionsAndDefault() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> extended = m.pad(1, 1, 1, 1, "Y");
            assertEquals("Y", extended.get(0, 0));
            assertEquals("a", extended.get(1, 1));
        }

        @Test
        public void testPad_AllZeros() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> result = m.pad(0, 0, 0, 0);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals("A", result.get(0, 0));
            assertEquals("D", result.get(1, 1));
        }

        @Test
        public void testPadRepeatAndFlatten_PreservesNulls() {
            Matrix<String> matrix = Matrix.wrap(new String[][] { { "a", null } });

            Matrix<String> extended = matrix.pad(0, 1, 1, 1, null);
            Matrix<String> repeatedElements = matrix.repeatElements(1, 2);
            Matrix<String> repeatedMatrix = matrix.repeatMatrix(2, 1);

            assertNull(extended.get(0, 0));
            assertNull(extended.get(0, 2));
            assertNull(extended.get(1, 1));
            assertNull(repeatedElements.get(0, 2));
            assertNull(repeatedElements.get(0, 3));
            assertNull(repeatedMatrix.get(1, 1));
            assertEquals(Arrays.asList("a", null), matrix.flatten());
        }

        @Test
        public void testPad_SingleAxisFillPreservesExistingValues() {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(Matrix.wrap(new Integer[][] { { 1, 2, 9 }, { 3, 4, 9 } }), matrix.pad(0, 0, 0, 1, 9));
            assertEquals(Matrix.wrap(new Integer[][] { { 9, 1, 2 }, { 9, 3, 4 } }), matrix.pad(0, 0, 1, 0, 9));
            assertEquals(Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 9, 9 } }), matrix.pad(0, 1, 0, 0, 9));
            final var copy = matrix.pad(0, 0, 0, 0, 9);
            assertEquals(matrix, copy);
            assertNotSame(matrix, copy);
        }

        @Test
        public void testPad_DirectionalWithNegativeValues() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IllegalArgumentException.class, () -> m.pad(-1, 1, 1, 1, "X"));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0, 0, 0", "0, -1, 0, 0", "0, 0, -1, 0", "0, 0, 0, -1", "2147483647, 0, 0, 0", "0, 0, 0, 2147483647" })
        public void testPad_RejectsNegativeOrOverflowingDimensions(final int top, final int bottom, final int left, final int right) {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.pad(top, bottom, left, right));
            assertEquals(Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } }), matrix);
        }
    }

    @Nested
    class FlipHorizontallyInPlaceTests {

        @Test
        public void testFlipHorizontallyInPlace() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            matrix.flipHorizontallyInPlace();
            Assertions.assertEquals(3, matrix.get(0, 0));
            Assertions.assertEquals(2, matrix.get(0, 1));
            Assertions.assertEquals(1, matrix.get(0, 2));
            Assertions.assertEquals(6, matrix.get(1, 0));
        }

        @Test
        public void testFlipHorizontallyInPlace_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            m.flipHorizontallyInPlace();
            assertEquals("C", m.get(0, 0));
            assertEquals("B", m.get(0, 1));
            assertEquals("A", m.get(0, 2));
            assertEquals("F", m.get(1, 0));
        }

        @Test
        public void testFlipHorizontallyInPlace_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            m.flipHorizontallyInPlace();
            assertEquals(2, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
            assertEquals(4, m.get(1, 0));
            assertEquals(3, m.get(1, 1));
        }

        @Test
        public void testFlipHorizontallyInPlace_ReversesHorizontally() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            m.flipHorizontallyInPlace();
            assertEquals("c", m.get(0, 0));
            assertEquals("a", m.get(0, 2));
            assertEquals("f", m.get(1, 0));
        }

        @Test
        public void testFlipInPlaceHorizontally() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b", "c" }, { "d", "e", "f" } });
            m.flipHorizontallyInPlace();
            assertEquals("c", m.get(0, 0));
            assertEquals("b", m.get(0, 1));
            assertEquals("a", m.get(0, 2));
            assertEquals("f", m.get(1, 0));
            assertEquals("e", m.get(1, 1));
            assertEquals("d", m.get(1, 2));
        }

        @Test
        public void testHorizontalInPlaceFlipOnDetachedRowsDoesNotMutateSource() {
            Integer[] sharedRow = { 1, 2, 3 };
            Matrix<Integer> m = Matrix.copyOf(Integer.class, sharedRow, sharedRow);

            m.flipHorizontallyInPlace();

            assertArrayEquals(new Integer[] { 3, 2, 1 }, m.rowCopy(0));
            assertArrayEquals(new Integer[] { 3, 2, 1 }, m.rowCopy(1));

            assertArrayEquals(new Integer[] { 1, 2, 3 }, sharedRow);
        }

        @Test
        public void testFlipHorizontallyInPlace_SingleColumn() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A" }, { "B" }, { "C" } });
            m.flipHorizontallyInPlace();
            assertEquals("A", m.get(0, 0));
            assertEquals("B", m.get(1, 0));
            assertEquals("C", m.get(2, 0));
        }

        @Test
        public void testFlipInPlaceHorizontally_SingleColumn() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1 }, { 2 } });
            m.flipHorizontallyInPlace();
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 0));
        }
    }

    @Nested
    class FlipVerticallyInPlaceTests {

        @Test
        public void testFlipVerticallyInPlace() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });

            matrix.flipVerticallyInPlace();
            Assertions.assertEquals(5, matrix.get(0, 0));
            Assertions.assertEquals(6, matrix.get(0, 1));
            Assertions.assertEquals(3, matrix.get(1, 0));
            Assertions.assertEquals(1, matrix.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" }, { "E", "F" } });
            m.flipVerticallyInPlace();
            assertEquals("E", m.get(0, 0));
            assertEquals("F", m.get(0, 1));
            assertEquals("C", m.get(1, 0));
            assertEquals("A", m.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            m.flipVerticallyInPlace();
            assertEquals(3, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
            assertEquals(1, m.get(1, 0));
            assertEquals(2, m.get(1, 1));
        }

        @Test
        public void testFlipVerticallyInPlace_ReversesVertically() {
            String[][] arr = { { "a", "b" }, { "c", "d" }, { "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            m.flipVerticallyInPlace();
            assertEquals("e", m.get(0, 0));
            assertEquals("f", m.get(0, 1));
            assertEquals("a", m.get(2, 0));
        }

        @Test
        public void testFlipInPlaceVertically() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" }, { "e", "f" } });
            m.flipVerticallyInPlace();
            assertEquals("e", m.get(0, 0));
            assertEquals("f", m.get(0, 1));
            assertEquals("c", m.get(1, 0));
            assertEquals("d", m.get(1, 1));
            assertEquals("a", m.get(2, 0));
            assertEquals("b", m.get(2, 1));
        }

        @Test
        public void testFlipVerticallyInPlace_HomogeneousRows_StillCorrect() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            matrix.flipVerticallyInPlace();

            assertArrayEquals(new Integer[] { 7, 8, 9 }, matrix.rowView(0));
            assertArrayEquals(new Integer[] { 4, 5, 6 }, matrix.rowView(1));
            assertArrayEquals(new Integer[] { 1, 2, 3 }, matrix.rowView(2));
        }

        @Test
        public void testFlipVerticallyInPlace_SingleRow() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" } });
            m.flipVerticallyInPlace();
            assertEquals("A", m.get(0, 0));
            assertEquals("B", m.get(0, 1));
            assertEquals("C", m.get(0, 2));
        }

        @Test
        public void testFlipInPlaceVertically_SingleRow() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b", "c" } });
            m.flipVerticallyInPlace();
            assertEquals("a", m.get(0, 0));
            assertEquals("b", m.get(0, 1));
            assertEquals("c", m.get(0, 2));
        }
    }

    @Nested
    class FlipHorizontallyTests {

        @Test
        public void testFlipH() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            Matrix<Integer> flipped = matrix.flipHorizontally();
            Assertions.assertEquals(3, flipped.get(0, 0));
            Assertions.assertEquals(2, flipped.get(0, 1));
            Assertions.assertEquals(1, flipped.get(0, 2));
            Assertions.assertEquals(6, flipped.get(1, 0));

            // Original unchanged
            Assertions.assertEquals(1, matrix.get(0, 0));
        }

        @Test
        public void testFlipHorizontally() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Matrix<Integer> flipped = matrix.flipHorizontally();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(2, flipped.get(0, 1));
            assertEquals(1, flipped.get(0, 2));
            assertEquals(6, flipped.get(1, 0));
            assertEquals(5, flipped.get(1, 1));
            assertEquals(4, flipped.get(1, 2));
        }

        @Test
        public void testFlipHorizontally_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            Matrix<String> flipped = m.flipHorizontally();

            assertEquals("C", flipped.get(0, 0));
            assertEquals("B", flipped.get(0, 1));
            assertEquals("A", flipped.get(0, 2));
            assertEquals("F", flipped.get(1, 0));
            assertEquals("A", m.get(0, 0)); // Original unchanged
        }

        @Test
        public void testFlipHorizontally_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> flipped = m.flipHorizontally();

            assertEquals(2, flipped.get(0, 0));
            assertEquals(1, flipped.get(0, 1));
            assertEquals(1, m.get(0, 0)); // Original unchanged
        }

        @Test
        public void testFlipHorizontally_CreatesNewHorizontallyFlipped() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> flipped = m.flipHorizontally();
            assertEquals("c", flipped.get(0, 0));
            assertEquals("a", m.get(0, 0)); // original unchanged
        }
    }

    @Nested
    class FlipVerticallyTests {

        @Test
        public void testFlipV() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });

            Matrix<Integer> flipped = matrix.flipVertically();
            Assertions.assertEquals(5, flipped.get(0, 0));
            Assertions.assertEquals(6, flipped.get(0, 1));
            Assertions.assertEquals(3, flipped.get(1, 0));
            Assertions.assertEquals(1, flipped.get(2, 0));

            // Original unchanged
            Assertions.assertEquals(1, matrix.get(0, 0));
        }

        @Test
        public void testFlipVertically() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            Matrix<Integer> flipped = matrix.flipVertically();
            assertEquals(5, flipped.get(0, 0));
            assertEquals(6, flipped.get(0, 1));
            assertEquals(3, flipped.get(1, 0));
            assertEquals(4, flipped.get(1, 1));
            assertEquals(1, flipped.get(2, 0));
            assertEquals(2, flipped.get(2, 1));
        }

        @Test
        public void testFlipVertically_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" }, { "E", "F" } });
            Matrix<String> flipped = m.flipVertically();

            assertEquals("E", flipped.get(0, 0));
            assertEquals("F", flipped.get(0, 1));
            assertEquals("C", flipped.get(1, 0));
            assertEquals("A", flipped.get(2, 0));
            assertEquals("A", m.get(0, 0)); // Original unchanged
        }

        @Test
        public void testFlipVertically_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> flipped = m.flipVertically();

            assertEquals(3, flipped.get(0, 0));
            assertEquals(1, m.get(0, 0)); // Original unchanged
        }

        @Test
        public void testFlipVertically_CreatesNewVerticallyFlipped() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> flipped = m.flipVertically();
            assertEquals("c", flipped.get(0, 0));
            assertEquals("a", m.get(0, 0)); // original unchanged
        }
    }

    @Nested
    class Rotate90Tests {

        @Test
        public void testRotate90() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });

            Matrix<Integer> rotated = matrix.rotate90();
            Assertions.assertEquals(2, rotated.rowCount());
            Assertions.assertEquals(3, rotated.columnCount());
            Assertions.assertEquals(5, rotated.get(0, 0));
            Assertions.assertEquals(3, rotated.get(0, 1));
            Assertions.assertEquals(1, rotated.get(0, 2));
            Assertions.assertEquals(6, rotated.get(1, 0));
        }

        // Check pattern
        @Test
        public void testRotate90_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals("C", rotated.get(0, 0));
            assertEquals("A", rotated.get(0, 1));
            assertEquals("D", rotated.get(1, 0));
            assertEquals("B", rotated.get(1, 1));
        }

        @Test
        public void testChainedOperations() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            Matrix<Integer> result = m.map(x -> x * 2).map(x -> x + 1).transpose().rotate90();

            assertNotNull(result);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
        }

        @Test
        public void testRotate90_NonSquare_RowsLessThanCols() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" } });
            Matrix<String> rotated = m.rotate90();
            assertEquals(3, rotated.rowCount());
            assertEquals(1, rotated.columnCount());
            assertEquals("A", rotated.get(0, 0));
            assertEquals("B", rotated.get(1, 0));
            assertEquals("C", rotated.get(2, 0));
        }

        @Test
        public void testRotate90_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            Matrix<String> rotated = m.rotate90();

            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals("D", rotated.get(0, 0));
            assertEquals("A", rotated.get(0, 1));
            assertEquals("F", rotated.get(2, 0));
            assertEquals("C", rotated.get(2, 1));
        }

        @Test
        public void testRotate90_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> rotated = m.rotate90();

            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3, rotated.get(0, 0));
            assertEquals(1, rotated.get(0, 1));
            assertEquals(4, rotated.get(1, 0));
            assertEquals(2, rotated.get(1, 1));
        }

        @Test
        public void testRotate90_RotatesClockwise() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals("c", rotated.get(0, 0));
            assertEquals("a", rotated.get(0, 1));
            assertEquals("d", rotated.get(1, 0));
            assertEquals("b", rotated.get(1, 1));
        }
    }

    @Nested
    class Rotate180Tests {

        @Test
        public void testRotate180() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            Matrix<Integer> rotated = matrix.rotate180();
            Assertions.assertEquals(6, rotated.get(0, 0));
            Assertions.assertEquals(5, rotated.get(0, 1));
            Assertions.assertEquals(4, rotated.get(0, 2));
            Assertions.assertEquals(3, rotated.get(1, 0));
            Assertions.assertEquals(2, rotated.get(1, 1));
            Assertions.assertEquals(1, rotated.get(1, 2));
        }

        @Test
        public void testRotate180_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> rotated = m.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals("D", rotated.get(0, 0));
            assertEquals("C", rotated.get(0, 1));
            assertEquals("B", rotated.get(1, 0));
            assertEquals("A", rotated.get(1, 1));
        }

        @Test
        public void testRotate180_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            Matrix<String> rotated = m.rotate180();

            assertEquals(2, rotated.rowCount());
            assertEquals(3, rotated.columnCount());
            assertEquals("F", rotated.get(0, 0));
            assertEquals("E", rotated.get(0, 1));
            assertEquals("D", rotated.get(0, 2));
            assertEquals("C", rotated.get(1, 0));
        }

        @Test
        public void testRotate180_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> rotated = m.rotate180();

            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4, rotated.get(0, 0));
            assertEquals(3, rotated.get(0, 1));
            assertEquals(2, rotated.get(1, 0));
            assertEquals(1, rotated.get(1, 1));
        }

        @Test
        public void testRotate180_Rotates180Degrees() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> rotated = m.rotate180();
            assertEquals("d", rotated.get(0, 0));
            assertEquals("c", rotated.get(0, 1));
            assertEquals("b", rotated.get(1, 0));
            assertEquals("a", rotated.get(1, 1));
        }
    }

    @Nested
    class Rotate270Tests {

        @Test
        public void testRotate270() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });

            Matrix<Integer> rotated = matrix.rotate270();
            Assertions.assertEquals(2, rotated.rowCount());
            Assertions.assertEquals(3, rotated.columnCount());
            Assertions.assertEquals(2, rotated.get(0, 0));
            Assertions.assertEquals(4, rotated.get(0, 1));
            Assertions.assertEquals(6, rotated.get(0, 2));
            Assertions.assertEquals(1, rotated.get(1, 0));
        }

        @Test
        public void testRotate270_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals("B", rotated.get(0, 0));
            assertEquals("D", rotated.get(0, 1));
            assertEquals("A", rotated.get(1, 0));
            assertEquals("C", rotated.get(1, 1));
        }

        @Test
        public void testRotate270_NonSquare_RowsLessThanCols() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" } });
            Matrix<String> rotated = m.rotate270();
            assertEquals(3, rotated.rowCount());
            assertEquals(1, rotated.columnCount());
            assertEquals("C", rotated.get(0, 0));
            assertEquals("B", rotated.get(1, 0));
            assertEquals("A", rotated.get(2, 0));
        }

        @Test
        public void testRotate270_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            Matrix<String> rotated = m.rotate270();

            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals("C", rotated.get(0, 0));
            assertEquals("F", rotated.get(0, 1));
            assertEquals("A", rotated.get(2, 0));
            assertEquals("D", rotated.get(2, 1));
        }

        @Test
        public void testRotate270_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> rotated = m.rotate270();

            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(2, rotated.get(0, 0));
            assertEquals(4, rotated.get(0, 1));
            assertEquals(1, rotated.get(1, 0));
            assertEquals(3, rotated.get(1, 1));
        }

        @Test
        public void testRotate270_RotatesCounterClockwise() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> rotated = m.rotate270();
            assertEquals("b", rotated.get(0, 0));
            assertEquals("d", rotated.get(0, 1));
            assertEquals("a", rotated.get(1, 0));
            assertEquals("c", rotated.get(1, 1));
        }
    }

    @Nested
    class TransposeTests {

        // Test reshape with too-small dimensions throws exception
        @Test
        public void testTranspose() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            Matrix<Integer> transposed = matrix.transpose();
            Assertions.assertEquals(3, transposed.rowCount());
            Assertions.assertEquals(2, transposed.columnCount());
            Assertions.assertEquals(1, transposed.get(0, 0));
            Assertions.assertEquals(4, transposed.get(0, 1));
            Assertions.assertEquals(2, transposed.get(1, 0));
            Assertions.assertEquals(5, transposed.get(1, 1));
        }

        @Test
        public void testImmutabilityOfDimensions() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            int originalRows = m.rowCount();
            int originalCols = m.columnCount();

            // Operations that return new matrices
            m.transpose();
            m.rotate90();
            m.resize(5, 5);

            // Original dimensions should not change
            assertEquals(originalRows, m.rowCount());
            assertEquals(originalCols, m.columnCount());
        }

        @Test
        public void testTranspose_NonSquare_RowsLessThanCols() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" } });
            Matrix<String> transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(1, transposed.columnCount());
            assertEquals("A", transposed.get(0, 0));
            assertEquals("B", transposed.get(1, 0));
            assertEquals("C", transposed.get(2, 0));
        }

        @Test
        public void testTranspose_SquareMatrix() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> transposed = m.transpose();

            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals("A", transposed.get(0, 0));
            assertEquals("C", transposed.get(0, 1));
            assertEquals("B", transposed.get(1, 0));
            assertEquals("D", transposed.get(1, 1));
        }

        @Test
        public void testTranspose_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            Matrix<String> transposed = m.transpose();

            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals("A", transposed.get(0, 0));
            assertEquals("D", transposed.get(0, 1));
            assertEquals("B", transposed.get(1, 0));
            assertEquals("E", transposed.get(1, 1));
            assertEquals("C", transposed.get(2, 0));
            assertEquals("F", transposed.get(2, 1));
        }

        @Test
        public void testTranspose_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Matrix<Integer> transposed = m.transpose();

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
        public void testTranspose_SwapsRowsAndColumns() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals("a", transposed.get(0, 0));
            assertEquals("d", transposed.get(0, 1));
            assertEquals("f", transposed.get(2, 1));
        }

        @Test
        public void testSingleElementMatrix() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "X" } });

            assertEquals("X", m.get(0, 0));
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());

            Matrix<String> transposed = m.transpose();
            assertEquals("X", transposed.get(0, 0));
        }
    }

    @Nested
    class ReshapeAndPadTests {

        @Test
        public void testReshape_LargerSize() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> reshaped = m.reshapeAndPad(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals("A", reshaped.get(0, 0));
            assertEquals("D", reshaped.get(1, 0));
            assertNull(reshaped.get(1, 2));
        }

        @Test
        public void testReshape_TwoParams_NeedsPadding() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Matrix<Integer> reshaped = m.reshapeAndPad(2, 4);

            assertEquals(2, reshaped.rowCount());
            assertEquals(4, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(4, reshaped.get(0, 3));
            assertEquals(5, reshaped.get(1, 0));
            assertEquals(6, reshaped.get(1, 1));
            assertNull(reshaped.get(1, 2)); // Padding
        }

        @Test
        public void testReshape_Empty() {
            Matrix<String> empty = Matrix.wrap(new String[0][0]);
            Matrix<String> reshaped = empty.reshapeAndPad(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }

        @Test
        public void testReshape_IncompatibleSize_FillsWithNull() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b", "c" } });
            Matrix<String> reshaped = m.reshapeAndPad(2, 2);
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals("a", reshaped.get(0, 0));
            assertEquals("b", reshaped.get(0, 1));
            assertEquals("c", reshaped.get(1, 0));
            assertNull(reshaped.get(1, 1)); // Not enough elements, should be null
        }

        @ParameterizedTest
        @CsvSource({ "-1, 2", "2, -1", "1, 1" })
        public void testReshapeAndPad_RejectsNegativeAndInsufficientShapes(final int rows, final int columns) {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.reshapeAndPad(rows, columns));
            assertEquals(Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } }), matrix);
        }
    }

    @Nested
    class RepeatElementsTests {

        @Test
        public void testRepeatElements() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            Matrix<Integer> repeated = matrix.repeatElements(2, 3);
            Assertions.assertEquals(4, repeated.rowCount());
            Assertions.assertEquals(6, repeated.columnCount());
            Assertions.assertEquals(1, repeated.get(0, 0));
            Assertions.assertEquals(1, repeated.get(0, 2));
            Assertions.assertEquals(1, repeated.get(1, 0));
            Assertions.assertEquals(2, repeated.get(0, 3));
            Assertions.assertEquals(3, repeated.get(2, 0));
            Assertions.assertEquals(4, repeated.get(3, 5));
        }

        @Test
        public void testRepeatElements_RepeatsCellsTwiceVerticallyAndThreeTimesHorizontally() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> repeated = matrix.repeatElements(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(1, repeated.get(0, 1));
            assertEquals(1, repeated.get(0, 2));
            assertEquals(2, repeated.get(0, 3));
            assertEquals(2, repeated.get(0, 4));
            assertEquals(2, repeated.get(0, 5));
            assertEquals(3, repeated.get(2, 0));
            assertEquals(4, repeated.get(2, 3));
        }

        @Test
        public void testRepeatElements_Shape1By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            Matrix<String> repeated = m.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals("A", repeated.get(0, 0));
            assertEquals("A", repeated.get(0, 1));
            assertEquals("A", repeated.get(0, 2));
            assertEquals("B", repeated.get(0, 3));
            assertEquals("B", repeated.get(0, 4));
            assertEquals("B", repeated.get(0, 5));
            assertEquals("A", repeated.get(1, 0)); // second row same as first
        }

        @Test
        public void testRepeatElements_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> repeated = m.repeatElements(2, 2);

            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals("A", repeated.get(0, 0));
            assertEquals("A", repeated.get(0, 1));
            assertEquals("A", repeated.get(1, 0));
            assertEquals("A", repeated.get(1, 1));
            assertEquals("D", repeated.get(2, 2));
            assertEquals("D", repeated.get(3, 3));
        }

        @Test
        public void testRepeatElements_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> repeated = m.repeatElements(2, 3);

            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(1, repeated.get(0, 1));
            assertEquals(1, repeated.get(1, 2));
            assertEquals(4, repeated.get(3, 5));
        }

        @Test
        public void testRepeatElements_RepeatsElements() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> repeated = m.repeatElements(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals("a", repeated.get(0, 0));
            assertEquals("a", repeated.get(1, 1));
            assertEquals("d", repeated.get(2, 2));
        }

        @Test
        public void testRepeatByOne_UsesUniformElementTypeStorageForHeterogeneousRows() {
            Matrix<Object> matrix = Matrix.wrap(new Object[][] { new Integer[] { 1 }, new String[] { "a" } });

            Matrix<Object> repeatedElements = matrix.repeatElements(1, 1);
            Matrix<Object> repeatedMatrix = matrix.repeatMatrix(1, 1);

            Assertions.assertDoesNotThrow(() -> repeatedElements.set(0, 0, "x"));
            Assertions.assertDoesNotThrow(() -> repeatedMatrix.set(0, 0, "x"));
        }

        @Test
        public void testRepeatElements_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            final var result = matrix.repeatElements(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, 0);
            assertEquals(1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatElements_InvalidArguments() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @Test
        public void testRepeatElements_InvalidArgs() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A" } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatElements_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(rows, columns));
        }
    }

    @Nested
    class RepeatMatrixTests {

        @Test
        public void testRepeatMatrix() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            Matrix<Integer> repeated = matrix.repeatMatrix(2, 3);
            Assertions.assertEquals(4, repeated.rowCount());
            Assertions.assertEquals(6, repeated.columnCount());
            Assertions.assertEquals(1, repeated.get(0, 0));
            Assertions.assertEquals(2, repeated.get(0, 1));
            Assertions.assertEquals(1, repeated.get(0, 2));
            Assertions.assertEquals(1, repeated.get(2, 0));
            Assertions.assertEquals(4, repeated.get(3, 5));
        }

        @Test
        public void testRepeatMatrix_TilesTwiceVerticallyAndThreeTimesHorizontally() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> repeated = matrix.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(2, repeated.get(0, 1));
            assertEquals(1, repeated.get(0, 2));
            assertEquals(2, repeated.get(0, 3));
            assertEquals(3, repeated.get(1, 0));
            assertEquals(4, repeated.get(1, 1));
        }

        @Test
        public void testRepeatMatrix_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> repeated = m.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals("A", repeated.get(0, 0));
            assertEquals("B", repeated.get(0, 1));
            assertEquals("A", repeated.get(0, 2)); // repeat starts
            assertEquals("B", repeated.get(0, 3));

            assertEquals("C", repeated.get(1, 0));
            assertEquals("D", repeated.get(1, 1));

            // Check vertical repeat
            assertEquals("A", repeated.get(2, 0)); // vertical repeat starts
            assertEquals("B", repeated.get(2, 1));
        }

        @Test
        public void testRepeatMatrix_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> repeated = m.repeatMatrix(2, 2);

            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals("A", repeated.get(0, 0));
            assertEquals("B", repeated.get(0, 1));
            assertEquals("A", repeated.get(0, 2)); // Repeated
            assertEquals("A", repeated.get(2, 0)); // Repeated
            assertEquals("D", repeated.get(3, 3));
        }

        @Test
        public void testRepeatMatrix_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> repeated = m.repeatMatrix(3, 2);

            assertEquals(6, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(2, repeated.get(0, 1));
            assertEquals(1, repeated.get(0, 2)); // Repeated horizontally
            assertEquals(1, repeated.get(2, 0)); // Repeated vertically
            assertEquals(4, repeated.get(5, 3));
        }

        @Test
        public void testRepeatMatrix_RepeatsMatrix() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Matrix<String> repeated = m.repeatMatrix(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals("a", repeated.get(0, 0));
            assertEquals("a", repeated.get(2, 2));
            assertEquals("d", repeated.get(3, 3));
        }

        @Test
        public void testRepeatMatrix_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            final var result = matrix.repeatMatrix(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, 0);
            assertEquals(1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrixWithEmptyDimensionsSkipsUnusedRepeats() {
            Matrix<Number> zeroRows = Matrix.empty(Number.class, 1);
            Matrix<Number> zeroColumns = Matrix.wrap(Number.class, new Number[0], new Number[0]);

            Assertions.assertTimeout(java.time.Duration.ofSeconds(5), () -> {
                Matrix<Number> wide = zeroRows.repeatMatrix(Integer.MAX_VALUE, Integer.MAX_VALUE);
                assertEquals(0, wide.rowCount());
                assertEquals(Integer.MAX_VALUE, wide.columnCount());
                assertEquals(Number.class, wide.elementType);

                Matrix<Number> narrow = zeroColumns.repeatMatrix(1, Integer.MAX_VALUE);
                assertEquals(2, narrow.rowCount());
                assertEquals(0, narrow.columnCount());
                assertEquals(Number.class, narrow.elementType);
                assertNotSame(narrow.rowView(0), narrow.rowView(1));
                assertNotSame(zeroColumns.rowView(0), narrow.rowView(0));
            });

            assertThrows(IllegalArgumentException.class, () -> zeroRows.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> zeroColumns.repeatMatrix(1, 0));
            assertThrows(IllegalArgumentException.class, () -> Matrix.empty(Number.class, 2).repeatMatrix(1, Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> zeroColumns.repeatMatrix(Integer.MAX_VALUE, 1));
        }

        @Test
        public void testRepeatMatrix_InvalidArguments() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidArgs() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A" } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatMatrix_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(rows, columns));
            assertEquals(Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } }), matrix);
        }
    }

    @Nested
    class FlattenTests {

        @Test
        public void testFlatten() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            List<Integer> flat = matrix.flatten();
            Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), flat);
        }

        @Test
        public void testFlatten_RectangleRowMajorList() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Integer> flat = matrix.flatten();
            assertEquals(java.util.Arrays.asList(1, 2, 3, 4, 5, 6), flat);
        }

        @Test
        public void testFlatten_Shape3By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            List<String> flat = m.flatten();
            assertEquals(9, flat.size());
            List<String> expected = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I");
            assertEquals(expected, flat);
        }

        @Test
        public void testFlatten_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            List<Integer> flat = m.flatten();

            assertEquals(4, flat.size());
            assertEquals(1, flat.get(0));
            assertEquals(2, flat.get(1));
            assertEquals(3, flat.get(2));
            assertEquals(4, flat.get(3));
        }

        @Test
        public void testFlatten_ReturnsAllElements() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            List<String> flattened = m.flatten();
            assertEquals(6, flattened.size());
            assertEquals("a", flattened.get(0));
            assertEquals("f", flattened.get(5));
        }

        @Test
        public void testFlattenIsGrowableAndOwnsSlotsWhileSharingElementObjects() {
            final StringBuilder value = new StringBuilder("shared");
            final Matrix<Object> matrix = Matrix.wrap(Object.class, new Object[][] { { value, null }, { "third", "fourth" } });
            final List<Object> flattened = matrix.flatten();

            assertEquals(Arrays.asList(value, null, "third", "fourth"), flattened);
            assertSame(value, flattened.get(0));
            flattened.set(1, "list-only");
            flattened.add("added");
            flattened.remove(3);
            assertNull(matrix.get(0, 1));
            assertEquals("fourth", matrix.get(1, 1));
            matrix.set(1, 0, "matrix-only");
            assertEquals("third", flattened.get(2));
            value.append("-changed");
            assertEquals("shared-changed", flattened.get(0).toString());

            for (final Matrix<Object> empty : List.of(Matrix.empty(), Matrix.empty(Object.class, 3), Matrix.wrap(Object.class, new Object[3][0]))) {
                final List<Object> emptyList = empty.flatten();
                emptyList.add("allowed");
                assertEquals(List.of("allowed"), emptyList);
                assertTrue(empty.isEmpty());
            }
        }

        @Test
        public void testEmptyMatrixOperations() {
            Matrix<String> empty = Matrix.wrap(new String[0][0]);

            assertTrue(empty.flatten().isEmpty());
            assertEquals(0, empty.copy().rowCount);
            assertEquals(empty, empty.transpose());
            assertEquals(empty, empty.rotate90());

            Matrix<String> extended = empty.resize(2, 2, "X");
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals("X", extended.get(0, 0));
        }

        @Test
        public void testFlatten_EmptyMatrix() {
            Matrix<String> m = Matrix.wrap(new String[0][0]);
            List<String> flat = m.flatten();
            assertTrue(flat.isEmpty());
        }
    }

    @Nested
    class MutateViaFlatArrayTests {

        @Test
        public void testMutateViaFlatArray() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 3, 1, 4 }, { 1, 5, 9 } });

            matrix.mutateViaFlatArray(arrays -> {
                Arrays.sort(arrays);
            });

            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(1, matrix.get(0, 1));
            Assertions.assertEquals(3, matrix.get(0, 2));
        }

        @Test
        public void testMutateFlattenedWriteBackContract() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            matrix.mutateViaFlatArray(flat -> {
                flat[0] = 9;
                assertEquals(1, matrix.get(0, 0)); // temporary buffer is not live
            });
            assertArrayEquals(new Integer[] { 9, 2 }, matrix.rowCopy(0));

            assertThrows(IllegalStateException.class, () -> matrix.mutateViaFlatArray(flat -> {
                flat[1] = 8;
                throw new IllegalStateException("abort");
            }));
            assertArrayEquals(new Integer[] { 9, 2 }, matrix.rowCopy(0)); // no copy-back after callback failure

            int[] zeroRowCalls = { 0 };
            Matrix.empty(Integer.class).mutateViaFlatArray(flat -> zeroRowCalls[0]++);
            assertEquals(0, zeroRowCalls[0]);

            int[] zeroColumnCalls = { 0 };
            Matrix.wrap(new Integer[2][0]).mutateViaFlatArray(flat -> {
                zeroColumnCalls[0]++;
                assertEquals(0, flat.length);
            });
            assertEquals(1, zeroColumnCalls[0]);

            Integer[] shared = { 1, 2 };
            assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(Integer.class, shared, shared));

            Matrix<Integer> detached = Matrix.copyOf(Integer.class, shared, shared);
            detached.mutateViaFlatArray(flat -> {
                flat[0] = 10;
                flat[1] = 20;
                flat[2] = 30;
                flat[3] = 40;
            });
            assertArrayEquals(new Integer[] { 10, 20 }, detached.rowCopy(0));
            assertArrayEquals(new Integer[] { 30, 40 }, detached.rowCopy(1));
            assertArrayEquals(new Integer[] { 1, 2 }, shared);

            Number[][] narrowRows = new Number[2][];
            narrowRows[0] = new Number[] { 1 };
            narrowRows[1] = new Integer[] { 2 };
            Matrix<Number> heterogeneous = Matrix.copyOf(Number.class, narrowRows);

            Assertions.assertDoesNotThrow(() -> heterogeneous.mutateViaFlatArray(flat -> {
                flat[0] = 10.5;
                flat[1] = 20.5;
            }));
            assertEquals(Double.valueOf(10.5), heterogeneous.get(0, 0));
            assertEquals(Double.valueOf(20.5), heterogeneous.get(1, 0));
        }

        @Test
        public void testMutateViaFlatArray_Shape3By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            List<Integer> rowLengths = new ArrayList<>();
            m.mutateViaFlatArray(row -> {
                rowLengths.add(row.length);
            });
            assertEquals(1, rowLengths.size());
            assertEquals(9, rowLengths.get(0).intValue());
        }

        @Test
        public void testMutateViaFlatArray_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            List<String> captured = new java.util.ArrayList<>();
            m.mutateViaFlatArray(arr -> {
                for (String val : arr) {
                    captured.add(val);
                }
            });
            assertEquals(4, captured.size());
            assertEquals("A", captured.get(0));
        }

        @Test
        public void testMutateViaFlatArray_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 3, 1 }, { 4, 2 } });
            List<Integer> captured = new java.util.ArrayList<>();
            m.mutateViaFlatArray(arr -> {
                for (Integer val : arr) {
                    captured.add(val);
                }
            });
            assertEquals(4, captured.size());
        }

        @Test
        public void testMutateViaFlatArray_AppliesOperationToEachRow() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            final int[] sum = { 0 };
            m.mutateViaFlatArray(row -> {
                for (Integer val : row) {
                    sum[0] += val;
                }
            });
            assertEquals(10, sum[0]);
        }

        @Test
        public void testMutateViaFlatArray_EmptyMatrix() {
            Matrix<String> empty = Matrix.wrap(new String[0][0]);
            List<Integer> lengths = new ArrayList<>();
            empty.mutateViaFlatArray(row -> lengths.add(row.length));
            assertEquals(0, lengths.size());
        }
    }

    @Nested
    class StackVerticallyTests {

        @Test
        public void testVstack() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 5, 6 }, { 7, 8 } });

            Matrix<Integer> stacked = m1.stackVertically(m2);
            Assertions.assertEquals(4, stacked.rowCount());
            Assertions.assertEquals(2, stacked.columnCount());
            Assertions.assertEquals(1, stacked.get(0, 0));
            Assertions.assertEquals(5, stacked.get(2, 0));
            Assertions.assertEquals(8, stacked.get(3, 1));
        }

        @Test
        public void testStackVerticallyDifferentCols() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 3, 4, 5 } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                m1.stackVertically(m2);
            });
        }

        @Test
        public void testStackVertically() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 5, 6 }, { 7, 8 } });
            Matrix<Integer> stacked = m1.stackVertically(m2);
            assertEquals(4, stacked.rowCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(5, stacked.get(2, 0));
            assertEquals(8, stacked.get(3, 1));
        }

        @Test
        public void testStackVertically_Shape2By3() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "G", "H", "I" }, { "J", "K", "L" } });
            Matrix<String> stacked = m1.stackVertically(m2);

            assertEquals(4, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals("A", stacked.get(0, 0));
            assertEquals("G", stacked.get(2, 0));
            assertEquals("L", stacked.get(3, 2));
        }

        @Test
        public void testStackVertically_DifferentColumnCounts() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "C", "D", "E" } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_WithSameMatrix() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" } });
            Matrix<String> result = m.stackVertically(m);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals("A", result.get(0, 0));
            assertEquals("A", result.get(1, 0));
        }

        @Test
        public void testStackVertically_DifferentColCount() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "C" } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_Strings() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "C", "D" } });
            Matrix<String> stacked = m1.stackVertically(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals("A", stacked.get(0, 0));
            assertEquals("B", stacked.get(0, 1));
            assertEquals("C", stacked.get(1, 0));
            assertEquals("D", stacked.get(1, 1));
        }

        @Test
        public void testStackVertically_Integers() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2, 3 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 4, 5, 6 } });
            Matrix<Integer> stacked = m1.stackVertically(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(6, stacked.get(1, 2));
        }

        @Test
        public void testStackVertically_StacksVertically() {
            String[][] arr1 = { { "a", "b" } };
            String[][] arr2 = { { "c", "d" } };
            Matrix<String> m1 = new Matrix<>(arr1);
            Matrix<String> m2 = new Matrix<>(arr2);
            Matrix<String> stacked = m1.stackVertically(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals("a", stacked.get(0, 0));
            assertEquals("c", stacked.get(1, 0));
        }

        @Test
        public void testStackPreservesRuntimeDimensionForArrayValuedElements() {
            final Object[][][] stringData = new String[][][] { { { "left" } } };
            final Object[][][] integerData = new Integer[][][] { { { 1 } } };
            final Matrix<Object[]> strings = Matrix.wrap(stringData);
            final Matrix<Object[]> integers = Matrix.wrap(integerData);

            final Matrix<Object[]> stacked = strings.stackVertically(integers);

            // The declared row type is Object[][]. Before the fix the common element type was
            // bare Comparable/Serializable, so rowCopy returned Comparable[]/Serializable[] and
            // this compiler-inserted cast threw ClassCastException.
            final Object[][] firstRow = stacked.rowCopy(0);
            final Object[][] secondRow = stacked.rowCopy(1);
            assertArrayEquals(new Object[] { "left" }, firstRow[0]);
            assertArrayEquals(new Object[] { 1 }, secondRow[0]);
            assertTrue(stacked.elementType().isArray());
        }

        @Test
        public void testVerticalStackTreatsZeroRowOperandAsRuntimeTypeNeutral() {
            Matrix<String> empty = Matrix.empty(String.class);
            Matrix<String> zeroWidthRows = Matrix.wrap(new String[][] { {}, {} });
            Matrix<String> typedEmpty = Matrix.wrap(new String[0][0]);

            Matrix<String> emptyFirst = empty.stackVertically(zeroWidthRows);
            Matrix<String> emptyLast = zeroWidthRows.stackVertically(empty);
            Matrix<String> zeroRowsEmptyFirst = empty.stackVertically(typedEmpty).resize(1, 0);
            Matrix<String> zeroRowsEmptyLast = typedEmpty.stackVertically(empty).resize(1, 0);

            String[] firstRow = emptyFirst.rowView(0);
            String[] lastRow = emptyLast.rowView(0);
            String[] zeroRowsFirstRow = zeroRowsEmptyFirst.rowView(0);
            String[] zeroRowsLastRow = zeroRowsEmptyLast.rowView(0);
            assertEquals(String.class, firstRow.getClass().getComponentType());
            assertEquals(String.class, lastRow.getClass().getComponentType());
            assertEquals(String.class, zeroRowsFirstRow.getClass().getComponentType());
            assertEquals(String.class, zeroRowsLastRow.getClass().getComponentType());
        }

        @Test
        public void testStackVertically_IncompatibleColumns_ThrowsException() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "a", "b" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "c", "d", "e" } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testObjectMatrixStackRejectsNullMatrix() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.stackVertically(null));
            assertThrows(IllegalArgumentException.class, () -> matrix.stackHorizontally(null));
        }
    }

    @Nested
    class StackHorizontallyTests {

        @Test
        public void testHstack() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 5 }, { 6 } });

            Matrix<Integer> stacked = m1.stackHorizontally(m2);
            Assertions.assertEquals(2, stacked.rowCount());
            Assertions.assertEquals(3, stacked.columnCount());
            Assertions.assertEquals(1, stacked.get(0, 0));
            Assertions.assertEquals(5, stacked.get(0, 2));
            Assertions.assertEquals(6, stacked.get(1, 2));
        }

        @Test
        public void testStackHorizontallyDifferentRows() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 3 }, { 4 } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                m1.stackHorizontally(m2);
            });
        }

        @Test
        public void testStackHorizontally() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 5 }, { 6 } });
            Matrix<Integer> stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(2, stacked.get(0, 1));
            assertEquals(5, stacked.get(0, 2));
            assertEquals(3, stacked.get(1, 0));
            assertEquals(6, stacked.get(1, 2));
        }

        @Test
        public void testStackHorizontally_Shape2By2() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "E", "F" }, { "G", "H" } });
            Matrix<String> stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals("A", stacked.get(0, 0));
            assertEquals("E", stacked.get(0, 2));
            assertEquals("H", stacked.get(1, 3));
        }

        @Test
        public void testStackHorizontally_DifferentRowCounts() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "E", "F" } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_WithSameMatrix() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A" }, { "B" } });
            Matrix<String> result = m.stackHorizontally(m);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals("A", result.get(0, 0));
            assertEquals("A", result.get(0, 1));
        }

        @Test
        public void testStackHorizontally_DifferentRowCount() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A" }, { "B" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "C" } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_Strings() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A" }, { "B" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "C" }, { "D" } });
            Matrix<String> stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals("A", stacked.get(0, 0));
            assertEquals("C", stacked.get(0, 1));
            assertEquals("B", stacked.get(1, 0));
            assertEquals("D", stacked.get(1, 1));
        }

        @Test
        public void testStackHorizontally_Integers() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1 }, { 2 }, { 3 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 4 }, { 5 }, { 6 } });
            Matrix<Integer> stacked = m1.stackHorizontally(m2);

            assertEquals(3, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(6, stacked.get(2, 1));
        }

        @Test
        public void testStackHorizontally_StacksHorizontally() {
            String[][] arr1 = { { "a" }, { "b" } };
            String[][] arr2 = { { "c" }, { "d" } };
            Matrix<String> m1 = new Matrix<>(arr1);
            Matrix<String> m2 = new Matrix<>(arr2);
            Matrix<String> stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals("a", stacked.get(0, 0));
            assertEquals("c", stacked.get(0, 1));
        }

        @Test
        public void testStackHorizontally_IncompatibleRows_ThrowsException() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "a" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "b" }, { "c" } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_RejectsLogicalWidthOverflowWithoutAllocation() {
            final var huge = Matrix.wrap(new Integer[0][0]).resize(0, Integer.MAX_VALUE);
            final var extra = Matrix.wrap(new Integer[0][0]).resize(0, 1);
            assertThrows(IllegalArgumentException.class, () -> huge.stackHorizontally(extra));
            assertEquals(Integer.MAX_VALUE, huge.columnCount());
        }
    }

    @Nested
    class ZipWithTests {

        @Test
        public void testZipWith() throws Exception {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 5, 6 }, { 7, 8 } });

            Matrix<Integer> sum = m1.zipWith(m2, (a, b) -> a + b);
            Assertions.assertEquals(6, sum.get(0, 0));
            Assertions.assertEquals(8, sum.get(0, 1));
            Assertions.assertEquals(10, sum.get(1, 0));
            Assertions.assertEquals(12, sum.get(1, 1));
        }

        @Test
        public void testZipWithDifferentType() throws Exception {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Double> m2 = Matrix.wrap(new Double[][] { { 0.5, 1.0 }, { 1.5, 2.0 } });

            Matrix<String> result = m1.zipWith(m2, (a, b) -> a + ":" + b, String.class);
            Assertions.assertEquals("1:0.5", result.get(0, 0));
            Assertions.assertEquals("2:1.0", result.get(0, 1));
            Assertions.assertEquals("3:1.5", result.get(1, 0));
            Assertions.assertEquals("4:2.0", result.get(1, 1));
        }

        @Test
        public void testZipWithDifferentShape() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 3, 4, 5 } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                m1.zipWith(m2, (a, b) -> a + b);
            });
        }

        @Test
        public void testZipWith3Matrices() throws Exception {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 5, 6 }, { 7, 8 } });
            Matrix<Integer> m3 = Matrix.wrap(new Integer[][] { { 9, 10 }, { 11, 12 } });

            Matrix<Integer> result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            Assertions.assertEquals(15, result.get(0, 0));
            Assertions.assertEquals(18, result.get(0, 1));
            Assertions.assertEquals(21, result.get(1, 0));
            Assertions.assertEquals(24, result.get(1, 1));
        }

        @Test
        public void testZipWith_AddsCorrespondingSquareCells() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 5, 6 }, { 7, 8 } });
            Matrix<Integer> sum = m1.zipWith(m2, (a, b) -> a + b);
            assertEquals(6, sum.get(0, 0));
            assertEquals(8, sum.get(0, 1));
            assertEquals(10, sum.get(1, 0));
            assertEquals(12, sum.get(1, 1));
        }

        @Test
        public void testZipWith3() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 5, 6 }, { 7, 8 } });
            Matrix<Integer> m3 = Matrix.wrap(new Integer[][] { { 9, 10 }, { 11, 12 } });
            Matrix<Integer> result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(15, result.get(0, 0));
            assertEquals(18, result.get(0, 1));
            assertEquals(21, result.get(1, 0));
            assertEquals(24, result.get(1, 1));
        }

        @Test
        public void testZipWith3_LocaleIndependentFormattingExample() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            Matrix<Double> m3 = Matrix.wrap(new Double[][] { { 0.1, 0.2 }, { 0.3, 0.4 } });

            Matrix<String> result = m1.zipWith(m2, m3, (i, s, d) -> i + s + String.format(java.util.Locale.ROOT, "%.1f", d), String.class);

            assertEquals("1a0.1", result.get(0, 0));
            assertEquals("4d0.4", result.get(1, 1));
        }

        @Test
        public void testZipWith_Shape2By2() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 10, 20 }, { 30, 40 } });
            Matrix<Integer> result = m1.zipWith(m2, (a, b) -> a + b);

            assertEquals(Integer.valueOf(11), result.get(0, 0));
            assertEquals(Integer.valueOf(22), result.get(0, 1));
            assertEquals(Integer.valueOf(33), result.get(1, 0));
            assertEquals(Integer.valueOf(44), result.get(1, 1));
        }

        @Test
        public void testZipWith_WithTypeChange() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 10, 20 }, { 30, 40 } });
            Matrix<String> result = m1.zipWith(m2, (a, b) -> a + "+" + b, String.class);

            assertEquals("1+10", result.get(0, 0));
            assertEquals("2+20", result.get(0, 1));
            assertEquals("3+30", result.get(1, 0));
            assertEquals("4+40", result.get(1, 1));
        }

        @Test
        public void testZipWith_DifferentShapes() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 10, 20, 30 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a + b));
        }

        @Test
        public void testZipWith_ThreeMatrices() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 10, 20 }, { 30, 40 } });
            Matrix<Integer> m3 = Matrix.wrap(new Integer[][] { { 100, 200 }, { 300, 400 } });
            Matrix<Integer> result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);

            assertEquals(Integer.valueOf(111), result.get(0, 0));
            assertEquals(Integer.valueOf(222), result.get(0, 1));
            assertEquals(Integer.valueOf(333), result.get(1, 0));
            assertEquals(Integer.valueOf(444), result.get(1, 1));
        }

        @Test
        public void testZipWith_ThreeMatrices_WithTypeChange() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 10, 20 }, { 30, 40 } });
            Matrix<Integer> m3 = Matrix.wrap(new Integer[][] { { 100, 200 }, { 300, 400 } });
            Matrix<String> result = m1.zipWith(m2, m3, (a, b, c) -> a + "+" + b + "+" + c, String.class);

            assertEquals("1+10+100", result.get(0, 0));
            assertEquals("2+20+200", result.get(0, 1));
            assertEquals("3+30+300", result.get(1, 0));
            assertEquals("4+40+400", result.get(1, 1));
        }

        @Test
        public void testZipWith_ThreeMatrices_DifferentShapes() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 10, 20 }, { 30, 40 } });
            Matrix<Integer> m3 = Matrix.wrap(new Integer[][] { { 100, 200, 300 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> a + b + c));
        }

        @Test
        public void testZipWith_TwoMatrices_DifferentType() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> result = m1.zipWith(m2, (a, b) -> a + b, String.class);

            assertEquals("1A", result.get(0, 0));
            assertEquals("2B", result.get(0, 1));
            assertEquals("3C", result.get(1, 0));
            assertEquals("4D", result.get(1, 1));
        }

        @Test
        public void testZipWith_ThreeMatrices_DifferentType() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 10, 20 } });
            Matrix<Integer> m3 = Matrix.wrap(new Integer[][] { { 100, 200 } });
            Matrix<String> result = m1.zipWith(m2, m3, (a, b, c) -> "" + a + b + c, String.class);

            assertEquals("110100", result.get(0, 0));
            assertEquals("220200", result.get(0, 1));
        }

        @Test
        public void testZipWith_TwoMatrices_SameType_Integers() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 10, 20 }, { 30, 40 } });
            Matrix<Integer> result = m1.zipWith(m2, (a, b) -> a + b);

            assertEquals(11, result.get(0, 0));
            assertEquals(22, result.get(0, 1));
            assertEquals(33, result.get(1, 0));
            assertEquals(44, result.get(1, 1));
        }

        @Test
        public void testZipWith_TwoMatrices_SameType_Strings() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "1", "2" }, { "3", "4" } });
            Matrix<String> result = m1.zipWith(m2, (a, b) -> a + b);

            assertEquals("A1", result.get(0, 0));
            assertEquals("B2", result.get(0, 1));
            assertEquals("C3", result.get(1, 0));
            assertEquals("D4", result.get(1, 1));
        }

        @Test
        public void testZipWith_ThreeMatrices_SameType_Integers() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 10, 20 } });
            Matrix<Integer> m3 = Matrix.wrap(new Integer[][] { { 100, 200 } });
            Matrix<Integer> result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);

            assertEquals(111, result.get(0, 0));
            assertEquals(222, result.get(0, 1));
        }

        @Test
        public void testZipWith_WithTwoMatrices() {
            Integer[][] arr1 = { { 1, 2 }, { 3, 4 } };
            Integer[][] arr2 = { { 5, 6 }, { 7, 8 } };
            Matrix<Integer> m1 = new Matrix<>(arr1);
            Matrix<Integer> m2 = new Matrix<>(arr2);
            Matrix<Integer> result = m1.zipWith(m2, (a, b) -> a + b);
            assertEquals(6, result.get(0, 0));
            assertEquals(12, result.get(1, 1));
        }

        @Test
        public void testZipWith_TwoArg_AcceptsDeclaredTypeWithCanonicalStorage() {
            Matrix<Object> a = Matrix.copyOf(Object.class, new String[][] { { "x" } });
            Matrix<Object> b = Matrix.copyOf(Object.class, new String[][] { { "y" } });
            Matrix<Object> widened = a.zipWith(b, (x, y) -> Integer.valueOf(1));
            assertEquals(Integer.valueOf(1), widened.get(0, 0));

            Matrix<Integer> ok = a.zipWith(b, (x, y) -> Integer.valueOf(1), Integer.class);
            assertEquals(Integer.valueOf(1), ok.get(0, 0));
        }

        @Test
        public void testZipWith_ThreeMatrix_AcceptsDeclaredTypeWithCanonicalStorage() {
            Matrix<Object> a = Matrix.copyOf(Object.class, new String[][] { { "x" } });
            Matrix<Object> b = Matrix.copyOf(Object.class, new String[][] { { "y" } });
            Matrix<Object> c = Matrix.copyOf(Object.class, new String[][] { { "z" } });
            Matrix<Object> widened = a.zipWith(b, c, (x, y, z) -> Integer.valueOf(0));
            assertEquals(Integer.valueOf(0), widened.get(0, 0));
        }

        @Test
        public void testZipWithNullArguments() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 5, 6 }, { 7, 8 } });
            Matrix<Integer> m3 = Matrix.wrap(new Integer[][] { { 9, 10 }, { 11, 12 } });

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> m1.zipWith(m2, (Throwables.BiFunction<Integer, Integer, Integer, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> m1.zipWith(m2, (Throwables.BiFunction<Integer, Integer, String, RuntimeException>) null, String.class));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a + b, (Class<Integer>) null));

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> m1.zipWith(m2, m3, (Throwables.TriFunction<Integer, Integer, Integer, Integer, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> m1.zipWith(m2, m3, (Throwables.TriFunction<Integer, Integer, Integer, String, RuntimeException>) null, String.class));
            Assertions.assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> a + b + c, (Class<Integer>) null));
        }
    }

    @Nested
    class MainDiagonalStreamTests {

        @Test
        public void testMainDiagonalStream() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            List<Integer> diagonal = matrix.mainDiagonalStream().toList();
            Assertions.assertEquals(Arrays.asList(1, 5, 9), diagonal);
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testAllExtendedIteratorCountsConsumeRemainingElements() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            assertCountConsumes(matrix.mainDiagonalStream(), 2);
            assertCountConsumes(matrix.antiDiagonalStream(), 2);
            assertCountConsumes(matrix.rowMajorStream(), 4);
            assertCountConsumes(matrix.columnMajorStream(), 4);
            assertCountConsumes(matrix.rowStreams(), 2);

            ObjIteratorEx<Stream<Integer>> columns = (ObjIteratorEx<Stream<Integer>>) matrix.columnStreams().iterator();
            Stream<Integer> firstColumn = columns.next();
            assertCountConsumes(firstColumn, 2);
            assertEquals(1L, columns.count());
            assertFalse(columns.hasNext());
        }

        @Test
        public void testMainDiagonalStream_ThreeByThreeArrayValues() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            Object[] diag = matrix.mainDiagonalStream().toArray();
            assertArrayEquals(new Object[] { 1, 5, 9 }, diag);
        }

        @Test
        public void testMainDiagonalStream_Shape3By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            List<String> diagonal = m.mainDiagonalStream().toList();
            assertEquals(3, diagonal.size());
            assertEquals("A", diagonal.get(0));
            assertEquals("E", diagonal.get(1));
            assertEquals("I", diagonal.get(2));
        }

        @Test
        public void testMainDiagonalStream_Rectangular() {
            Matrix<String> nonSquare = Matrix.wrap(new String[][] { { "A", "B" } });
            assertEquals(List.of("A"), nonSquare.mainDiagonalStream().toList());
        }

        @Test
        public void testMainDiagonalStream_Rectangular_Shape3By2() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertEquals(List.of(1, 4), m.mainDiagonalStream().toList());
        }

        @Test
        public void testMainDiagonalStream_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Integer> diagonal = m.mainDiagonalStream().toList();

            assertEquals(3, diagonal.size());
            assertEquals(1, diagonal.get(0));
            assertEquals(5, diagonal.get(1));
            assertEquals(9, diagonal.get(2));
        }

        @Test
        public void testMainDiagonalStream_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            List<String> diagonal = m.mainDiagonalStream().toList();

            assertEquals(2, diagonal.size());
            assertEquals("A", diagonal.get(0));
            assertEquals("D", diagonal.get(1));
        }

        @Test
        public void testMainDiagonalStream_StreamsDiagonal() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" }, { "g", "h", "i" } };
            Matrix<String> m = new Matrix<>(arr);
            List<String> result = m.mainDiagonalStream().toList();
            assertEquals(Arrays.asList("a", "e", "i"), result);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testMainDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final Integer[] expected = new Integer[] { 1, 5, 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (ObjIteratorEx<Integer>) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }

        @Test
        public void testMainDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (ObjIteratorEx<Integer>) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1, iterator.next());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }

        @Test
        public void testMainDiagonalStreamEmpty() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[0][0]);

            List<Integer> diagonal = matrix.mainDiagonalStream().toList();
            Assertions.assertTrue(diagonal.isEmpty());
        }

        @Test
        public void testMainDiagonalStream_Empty() {
            Matrix<String> empty = Matrix.wrap(new String[0][0]);
            assertEquals(0, empty.mainDiagonalStream().count());
        }

        @Test
        public void testDiagonalStream_Nx0MatrixIsEmpty() {
            Matrix<Integer> m = Matrix.wrap(new Integer[3][0]);
            assertEquals(3, m.rowCount());
            assertEquals(0, m.columnCount());
            assertEquals(0, m.mainDiagonalStream().count());
            assertEquals(0, m.antiDiagonalStream().count());

            Matrix<Integer> empty = Matrix.empty(Integer.class);
            assertEquals(0, empty.mainDiagonalStream().count());
            assertEquals(0, empty.antiDiagonalStream().count());
        }
    }

    @Nested
    class AntiDiagonalStreamTests {

        @Test
        public void testAntiDiagonalStream() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            List<Integer> diagonal = matrix.antiDiagonalStream().toList();
            Assertions.assertEquals(Arrays.asList(3, 5, 7), diagonal);
        }

        @Test
        public void testAntiDiagonalStream_ThreeByThreeArrayValues() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            Object[] diag = matrix.antiDiagonalStream().toArray();
            assertArrayEquals(new Object[] { 3, 5, 7 }, diag);
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            List<String> antiDiagonal = m.antiDiagonalStream().toList();
            assertEquals(3, antiDiagonal.size());
            assertEquals("C", antiDiagonal.get(0));
            assertEquals("E", antiDiagonal.get(1));
            assertEquals("G", antiDiagonal.get(2));
        }

        @Test
        public void testAntiDiagonalStream_Rectangular() {
            Matrix<String> nonSquare = Matrix.wrap(new String[][] { { "A", "B" } });
            assertEquals(List.of("B"), nonSquare.antiDiagonalStream().toList());
        }

        @Test
        public void testAntiDiagonalStream_Rectangular_Shape3By2() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertEquals(List.of(2, 3), m.antiDiagonalStream().toList());
        }

        @Test
        public void testAntiDiagonalStream_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Integer> diagonal = m.antiDiagonalStream().toList();

            assertEquals(3, diagonal.size());
            assertEquals(3, diagonal.get(0));
            assertEquals(5, diagonal.get(1));
            assertEquals(7, diagonal.get(2));
        }

        @Test
        public void testAntiDiagonalStream_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            List<String> diagonal = m.antiDiagonalStream().toList();

            assertEquals(2, diagonal.size());
            assertEquals("B", diagonal.get(0));
            assertEquals("C", diagonal.get(1));
        }

        @Test
        public void testAntiDiagonalStream_StreamsAntiDiagonal() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" }, { "g", "h", "i" } };
            Matrix<String> m = new Matrix<>(arr);
            List<String> result = m.antiDiagonalStream().toList();
            assertEquals(Arrays.asList("c", "e", "g"), result);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testAntiDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final Integer[] expected = new Integer[] { 3, 5, 7 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (ObjIteratorEx<Integer>) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }

        @Test
        public void testAntiDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (ObjIteratorEx<Integer>) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(3, iterator.next());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }

        @Test
        public void testAntiDiagonalStream_Empty() {
            Matrix<String> empty = Matrix.wrap(new String[0][0]);
            assertEquals(0, empty.antiDiagonalStream().count());
        }
    }

    @Nested
    class RowMajorStreamTests {

        @Test
        public void testRowMajorStream() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            List<Integer> elements = matrix.rowMajorStream().toList();
            Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), elements);
        }

        @Test
        public void testRowMajorStreamRange() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });

            List<Integer> rows = matrix.rowMajorStream(1, 3).toList();
            Assertions.assertEquals(Arrays.asList(3, 4, 5, 6), rows);
        }

        @Test
        public void testStreamAdvance() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            var stream = matrix.rowMajorStream();
            var iterator = stream.iterator();

            if (iterator instanceof ObjIteratorEx) {
                ObjIteratorEx<Integer> ex = (ObjIteratorEx<Integer>) iterator;
                ex.advance(2);
                Assertions.assertEquals(3, ex.next());
                Assertions.assertEquals(3, ex.count());
                Assertions.assertFalse(ex.hasNext());
                Assertions.assertThrows(NoSuchElementException.class, ex::next);
            }
        }

        @Test
        public void testStreamToArray() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            var stream = matrix.rowMajorStream(0, 2);
            var iterator = stream.iterator();

            if (iterator instanceof ObjIteratorEx) {
                ObjIteratorEx<Integer> ex = (ObjIteratorEx<Integer>) iterator;
                Integer[] array = ex.toArray(new Integer[0]);
                Assertions.assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, array);
            }
        }

        @Test
        public void testRowMajorIteratorArrayStoreFailureKeepsValidCursor() {
            Matrix<Number> matrix = Matrix.wrap(Number.class, new Number[] { 1 }, new Number[] { 2.5d }, new Number[] { 3 });

            try (Stream<Number> stream = matrix.rowMajorStream()) {
                ObjIteratorEx<Number> iterator = (ObjIteratorEx<Number>) stream.iterator();
                Integer[] destination = new Integer[3];

                assertThrows(ArrayStoreException.class, () -> iterator.toArray(destination));
                assertEquals(1, destination[0]);
                assertTrue(iterator.hasNext());
                assertEquals(2.5d, iterator.next());
                assertEquals(3, iterator.next());
                assertFalse(iterator.hasNext());
            }
        }

        @Test
        public void testHorizontalStream() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Object[] array = matrix.rowMajorStream().toArray();
            assertArrayEquals(new Object[] { 1, 2, 3, 4 }, array);
        }

        @Test
        public void testHorizontalStreamRange() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            Object[] subArray = matrix.rowMajorStream(0, 2).toArray();
            assertArrayEquals(new Object[] { 1, 2, 3, 4 }, subArray);
        }

        @Test
        public void testRowMajorStream_Shape2By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            List<String> all = m.rowMajorStream().toList();
            assertEquals(6, all.size());
            assertEquals("A", all.get(0));
            assertEquals("B", all.get(1));
            assertEquals("C", all.get(2));
            assertEquals("D", all.get(3));
        }

        @Test
        public void testRowMajorStream_WithRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            List<String> rows = m.rowMajorStream(1, 3).toList();
            assertEquals(6, rows.size());
        }

        @Test
        public void testRowMajorStream_RowRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" }, { "E", "F" } });
            List<String> elements = m.rowMajorStream(1, 3).toList();

            assertEquals(4, elements.size());
            assertEquals("C", elements.get(0));
            assertEquals("D", elements.get(1));
            assertEquals("E", elements.get(2));
            assertEquals("F", elements.get(3));
        }

        @Test
        public void testRowMajorStream_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            List<String> elements = m.rowMajorStream().toList();

            assertEquals(4, elements.size());
            assertEquals("A", elements.get(0));
            assertEquals("B", elements.get(1));
            assertEquals("C", elements.get(2));
            assertEquals("D", elements.get(3));
        }

        @Test
        public void testRowMajorStream_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Integer> elements = m.rowMajorStream().toList();

            assertEquals(6, elements.size());
            assertEquals(1, elements.get(0));
            assertEquals(6, elements.get(5));
        }

        @Test
        public void testRowMajorStream_StreamsAllElements() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            List<String> result = m.rowMajorStream().toList();
            assertEquals(Arrays.asList("a", "b", "c", "d"), result);
        }

        @Test
        public void testRowMajorStream_WithRowRange() {
            String[][] arr = { { "a", "b" }, { "c", "d" }, { "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            List<String> result = m.rowMajorStream(1, 3).toList();
            assertEquals(Arrays.asList("c", "d", "e", "f"), result);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testRowMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final Integer[] expected = new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (ObjIteratorEx<Integer>) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }

        @Test
        public void testRowMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (ObjIteratorEx<Integer>) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1, iterator.next());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }

        @Test
        public void testRowMajorStreamSingleRowRange() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            List<Integer> row1 = matrix.rowMajorStream(1, 2).toList();
            Assertions.assertEquals(Arrays.asList(4, 5, 6), row1);
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testRowMajorStreamorizontalToArrayNullTerminatesOversizedArray() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            var iterator = matrix.rowMajorStream(0, 2).iterator();

            Assertions.assertTrue(iterator instanceof ObjIteratorEx);

            ObjIteratorEx<Integer> ex = (ObjIteratorEx<Integer>) iterator;
            Integer[] destination = new Integer[] { -1, -1, -1, -1, -1, -1 };
            Integer[] array = ex.toArray(destination);

            Assertions.assertSame(destination, array);
            Assertions.assertArrayEquals(new Integer[] { 1, 2, 3, 4, null, -1 }, array);
        }

        @Test
        public void testRowMajorIteratorNullArrayRetainsInheritedContract() {
            Matrix<Integer> matrix = Matrix.wrap(Integer.class, new Integer[] { 1, 2 }, new Integer[] { 3, 4 });

            try (Stream<Integer> stream = matrix.rowMajorStream()) {
                ObjIteratorEx<Integer> iterator = (ObjIteratorEx<Integer>) stream.iterator();
                assertEquals(1, iterator.next());

                assertThrows(NullPointerException.class, () -> iterator.toArray((Integer[]) null));
                assertArrayEquals(new Integer[] { 2, 3, 4 }, iterator.toArray(new Integer[0]));
                assertFalse(iterator.hasNext());
                assertThrows(NullPointerException.class, () -> iterator.toArray((Integer[]) null));
            }
        }

        @Test
        public void testHorizontalStreamSingleRowRange() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            Object[] firstRow = matrix.rowMajorStream(0, 1).toArray();
            assertArrayEquals(new Object[] { 1, 2, 3 }, firstRow);
        }

        @Test
        public void testRowMajorStream_Empty() {
            Matrix<String> empty = Matrix.wrap(new String[0][0]);
            assertEquals(0, empty.rowMajorStream().count());
        }

        @Test
        public void testRowMajorStream_SingleRowRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" } });
            List<String> result = m.rowMajorStream(0, 1).toList();
            assertEquals(3, result.size());
            assertEquals("A", result.get(0));
            assertEquals("C", result.get(2));
        }

        @Test
        public void testRowMajorStream_SingleRowRange_Shape2By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            List<String> row = m.rowMajorStream(1, 2).toList();

            assertEquals(3, row.size());
            assertEquals("D", row.get(0));
            assertEquals("E", row.get(1));
            assertEquals("F", row.get(2));
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_Values() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            List<String> result = m.rowMajorStream(1, 2).toList();
            assertEquals(Arrays.asList("d", "e", "f"), result);
        }

        @Test
        public void testRowMajorStream_ZeroColumnsKeepLogicalRowRange() {
            final var matrix = Matrix.wrap(new Integer[3][0]);
            assertEquals(0, matrix.rowMajorStream(1, 3).count());
            assertEquals(2, matrix.rowStreams(1, 3).count());
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 3));
        }

        @Test
        public void testRowMajorStream_WithRange_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 1));
        }
    }

    @Nested
    class ColumnMajorStreamTests {

        @Test
        public void testColumnMajorStream() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            List<Integer> elements = matrix.columnMajorStream().toList();
            Assertions.assertEquals(Arrays.asList(1, 3, 2, 4), elements);
        }

        @Test
        public void testColumnMajorStreamRange() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            List<Integer> columnCount = matrix.columnMajorStream(1, 3).toList();
            Assertions.assertEquals(Arrays.asList(2, 5, 3, 6), columnCount);
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testColumnMajorStreamerticalIteratorAdvanceAndExhaustion_EdgeCase() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            var iterator = matrix.columnMajorStream(0, 2).iterator();

            Assertions.assertTrue(iterator instanceof ObjIteratorEx);

            ObjIteratorEx<Integer> ex = (ObjIteratorEx<Integer>) iterator;
            ex.advance(0);
            Assertions.assertEquals(4L, ex.count());
            Assertions.assertFalse(ex.hasNext());
            Assertions.assertThrows(NoSuchElementException.class, ex::next);

            ObjIteratorEx<Integer> advanced = (ObjIteratorEx<Integer>) matrix.columnMajorStream(0, 2).iterator();
            advanced.advance(3);
            Assertions.assertEquals(4, advanced.next());
            Assertions.assertEquals(0L, advanced.count());
            Assertions.assertFalse(advanced.hasNext());
        }

        @Test
        public void testColumnMajorIteratorArrayStoreFailureKeepsValidCursor() {
            Matrix<Number> matrix = Matrix.wrap(Number.class, new Number[] { 1, 2.5d, 3 });

            try (Stream<Number> stream = matrix.columnMajorStream()) {
                ObjIteratorEx<Number> iterator = (ObjIteratorEx<Number>) stream.iterator();
                Integer[] destination = new Integer[3];

                assertThrows(ArrayStoreException.class, () -> iterator.toArray(destination));
                assertEquals(1, destination[0]);
                assertTrue(iterator.hasNext());
                assertEquals(2.5d, iterator.next());
                assertEquals(3, iterator.next());
                assertFalse(iterator.hasNext());
            }
        }

        @Test
        public void testVerticalStream() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Object[] colMajor = matrix.columnMajorStream().toArray();
            assertArrayEquals(new Object[] { 1, 3, 2, 4 }, colMajor);
        }

        @Test
        public void testVerticalStreamRange() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Object[] colMajor = matrix.columnMajorStream(0, 2).toArray();
            assertArrayEquals(new Object[] { 1, 4, 2, 5 }, colMajor);
        }

        @Test
        public void testColumnMajorStream_Shape2By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            List<String> all = m.columnMajorStream().toList();
            assertEquals(6, all.size());
            assertEquals("A", all.get(0));
            assertEquals("D", all.get(1));
            assertEquals("B", all.get(2));
        }

        @Test
        public void testColumnMajorStream_WithRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            List<String> columnCount = m.columnMajorStream(1, 3).toList();
            assertEquals(6, columnCount.size());
        }

        @Test
        public void testColumnMajorStream_ColumnRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            List<String> elements = m.columnMajorStream(1, 3).toList();

            assertEquals(4, elements.size());
            assertEquals("B", elements.get(0));
            assertEquals("E", elements.get(1));
            assertEquals("C", elements.get(2));
            assertEquals("F", elements.get(3));
        }

        @Test
        public void testColumnMajorStream_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            List<String> elements = m.columnMajorStream().toList();

            assertEquals(4, elements.size());
            assertEquals("A", elements.get(0));
            assertEquals("C", elements.get(1));
            assertEquals("B", elements.get(2));
            assertEquals("D", elements.get(3));
        }

        @Test
        public void testColumnMajorStream_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Integer> elements = m.columnMajorStream().toList();

            assertEquals(6, elements.size());
            assertEquals(1, elements.get(0));
            assertEquals(4, elements.get(1));
            assertEquals(2, elements.get(2));
        }

        @Test
        public void testColumnMajorStream_StreamsAllElementsVertically() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            List<String> result = m.columnMajorStream().toList();
            assertEquals(Arrays.asList("a", "c", "b", "d"), result);
        }

        @Test
        public void testColumnMajorStream_WithColumnRange() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            List<String> result = m.columnMajorStream(1, 3).toList();
            assertEquals(Arrays.asList("b", "e", "c", "f"), result);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testColumnMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final Integer[] expected = new Integer[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (ObjIteratorEx<Integer>) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }

        @Test
        public void testColumnMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (ObjIteratorEx<Integer>) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(1, iterator.next());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }

        @Test
        public void testColumnMajorStreamSingleColumnRange() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });

            List<Integer> col1 = matrix.columnMajorStream(1, 2).toList();
            Assertions.assertEquals(Arrays.asList(2, 5, 8), col1);
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testColumnMajorStreamerticalToArrayNullTerminatesOversizedArray_EdgeCase() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            var iterator = matrix.columnMajorStream(0, 2).iterator();

            Assertions.assertTrue(iterator instanceof ObjIteratorEx);

            ObjIteratorEx<Integer> ex = (ObjIteratorEx<Integer>) iterator;
            Integer[] destination = new Integer[] { -1, -1, -1, -1, -1, -1 };
            Integer[] array = ex.toArray(destination);

            Assertions.assertSame(destination, array);
            Assertions.assertArrayEquals(new Integer[] { 1, 3, 2, 4, null, -1 }, array);
        }

        @Test
        public void testColumnMajorIteratorNullArrayRetainsInheritedContract() {
            Matrix<Integer> matrix = Matrix.wrap(Integer.class, new Integer[] { 1, 2 }, new Integer[] { 3, 4 });

            try (Stream<Integer> stream = matrix.columnMajorStream()) {
                ObjIteratorEx<Integer> iterator = (ObjIteratorEx<Integer>) stream.iterator();
                assertEquals(1, iterator.next());

                assertThrows(NullPointerException.class, () -> iterator.toArray((Integer[]) null));
                assertArrayEquals(new Integer[] { 3, 2, 4 }, iterator.toArray(new Integer[0]));
                assertFalse(iterator.hasNext());
                assertThrows(NullPointerException.class, () -> iterator.toArray((Integer[]) null));
            }
        }

        @Test
        public void testVerticalStreamSingleColumnRange() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            Object[] secondCol = matrix.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new Object[] { 2, 5, 8 }, secondCol);
        }

        @Test
        public void testColumnMajorStream_Empty() {
            Matrix<String> empty = Matrix.wrap(new String[0][0]);
            assertEquals(0, empty.columnMajorStream().count());
        }

        @Test
        public void testColumnMajorStream_SingleColumnRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A" }, { "B" }, { "C" } });
            List<String> result = m.columnMajorStream(0, 1).toList();
            assertEquals(3, result.size());
            assertEquals("A", result.get(0));
            assertEquals("C", result.get(2));
        }

        @Test
        public void testColumnMajorStream_SingleColumnRange_Shape2By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            List<String> col = m.columnMajorStream(1, 2).toList();

            assertEquals(2, col.size());
            assertEquals("B", col.get(0));
            assertEquals("E", col.get(1));
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_Values() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            List<String> result = m.columnMajorStream(1, 2).toList();
            assertEquals(Arrays.asList("b", "e"), result);
        }

        @Test
        public void testColumnMajorStream_ZeroRowsKeepLogicalColumnRange() {
            final var matrix = Matrix.wrap(new Integer[0][0]).resize(0, 3);
            assertEquals(0, matrix.columnMajorStream(1, 3).count());
            assertEquals(2, matrix.columnStreams(1, 3).count());
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 3));
        }

        @Test
        public void testColumnMajorStream_WithRange_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 1));
        }
    }

    @Nested
    class RowStreamsTests {

        @Test
        public void testStreamsObserveLiveValuesWhileProducedRowStreamsKeepTheirBackingRow() {
            final Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            try (var rowStreams = matrix.rowStreams(0, 2);
                 var columnStreams = matrix.columnStreams(0, 2);
                 var rowMajor = matrix.rowMajorStream(0, 2);
                 var columnMajor = matrix.columnMajorStream(0, 2);
                 var mainDiagonal = matrix.mainDiagonalStream();
                 var antiDiagonal = matrix.antiDiagonalStream()) {
                final var rows = rowStreams.iterator();
                final var columns = columnStreams.iterator();
                try (var selectedRow = rows.next();
                     var selectedColumn = columns.next()) {
                    matrix.flipVerticallyInPlace();
                    matrix.set(1, 0, 9);
                    assertEquals(List.of(9, 2), selectedRow.toList());
                    assertEquals(List.of(3, 9), selectedColumn.toList());
                    assertEquals(List.of(3, 4, 9, 2), rowMajor.toList());
                    assertEquals(List.of(3, 9, 4, 2), columnMajor.toList());
                    assertEquals(List.of(3, 2), mainDiagonal.toList());
                    assertEquals(List.of(4, 9), antiDiagonal.toList());
                }
                try (var nextRow = rows.next();
                     var nextColumn = columns.next()) {
                    assertEquals(List.of(9, 2), nextRow.toList());
                    assertEquals(List.of(4, 2), nextColumn.toList());
                }
            }
        }

        @Test
        public void testRowStreams() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });

            List<List<Integer>> rows = matrix.rowStreams().map(stream -> stream.toList()).toList();

            Assertions.assertEquals(3, rows.size());
            Assertions.assertEquals(Arrays.asList(1, 2), rows.get(0));
            Assertions.assertEquals(Arrays.asList(3, 4), rows.get(1));
            Assertions.assertEquals(Arrays.asList(5, 6), rows.get(2));
        }

        @Test
        public void testRowStreamsRange() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });

            List<List<Integer>> rows = matrix.rowStreams(1, 3).map(stream -> stream.toList()).toList();

            Assertions.assertEquals(2, rows.size());
            Assertions.assertEquals(Arrays.asList(3, 4), rows.get(0));
            Assertions.assertEquals(Arrays.asList(5, 6), rows.get(1));
        }

        @Test
        public void testRowStreams_Shape2By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            List<Stream<String>> rows = m.rowStreams().toList();
            assertEquals(2, rows.size());
            List<String> row0 = rows.get(0).toList();
            assertEquals(3, row0.size());
            assertEquals("A", row0.get(0));
        }

        @Test
        public void testRowStreams_WithRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            List<Stream<String>> rows = m.rowStreams(1, 3).toList();
            assertEquals(2, rows.size());
        }

        @Test
        public void testRowStreams_RowRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" }, { "E", "F" } });
            List<List<String>> rows = m.rowStreams(1, 3).map(Stream::toList).toList();

            assertEquals(2, rows.size());
            assertEquals("C", rows.get(0).get(0));
            assertEquals("E", rows.get(1).get(0));
        }

        @Test
        public void testRowStreams_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            List<List<String>> rows = m.rowStreams().map(Stream::toList).toList();

            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).size());
            assertEquals("A", rows.get(0).get(0));
            assertEquals("B", rows.get(0).get(1));
            assertEquals("C", rows.get(1).get(0));
            assertEquals("D", rows.get(1).get(1));
        }

        @Test
        public void testRowStreams_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<List<Integer>> rows = m.rowStreams().map(Stream::toList).toList();

            assertEquals(2, rows.size());
            assertEquals(3, rows.get(0).size());
            assertEquals(1, rows.get(0).get(0));
            assertEquals(6, rows.get(1).get(2));
        }

        @Test
        public void testRowStreams_StreamsRows() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Stream<Stream<String>> streams = m.rowStreams();
            assertEquals(2, streams.count());
        }

        @Test
        public void testRowStreams_WithRange_Values() {
            String[][] arr = { { "a", "b" }, { "c", "d" }, { "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            Stream<Stream<String>> streams = m.rowStreams(1, 3);
            assertEquals(2, streams.count());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testRowStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final Integer[][] expected = { new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 }, new Integer[] { 7, 8, 9 } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowStreams()) {
                final var iterator = (ObjIteratorEx<Stream<Integer>>) stream.iterator();
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
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { {}, {}, {} });

            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(0, matrix.columnCount());
            Assertions.assertTrue(matrix.isEmpty());

            List<List<Integer>> rows = matrix.rowStreams().map(stream -> stream.toList()).toList();

            Assertions.assertEquals(3, rows.size());
            Assertions.assertTrue(rows.get(0).isEmpty());
            Assertions.assertTrue(rows.get(1).isEmpty());
            Assertions.assertTrue(rows.get(2).isEmpty());
        }

        @Test
        public void testRowStreams_Empty() {
            Matrix<String> empty = Matrix.wrap(new String[0][0]);
            assertEquals(0, empty.rowStreams().count());
        }

        @Test
        public void testRowStreams_WithRange_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(0, 3));
        }
    }

    @Nested
    class ColumnStreamsTests {

        @Test
        public void testColumnStreams() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            List<List<Integer>> columnCount = matrix.columnStreams().map(stream -> stream.toList()).toList();

            Assertions.assertEquals(3, columnCount.size());
            Assertions.assertEquals(Arrays.asList(1, 4), columnCount.get(0));
            Assertions.assertEquals(Arrays.asList(2, 5), columnCount.get(1));
            Assertions.assertEquals(Arrays.asList(3, 6), columnCount.get(2));
        }

        @Test
        public void testColumnStreamsRange() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            List<List<Integer>> columnCount = matrix.columnStreams(1, 3).map(stream -> stream.toList()).toList();

            Assertions.assertEquals(2, columnCount.size());
            Assertions.assertEquals(Arrays.asList(2, 5), columnCount.get(0));
            Assertions.assertEquals(Arrays.asList(3, 6), columnCount.get(1));
        }

        @Test
        public void testColumnStreams_Shape2By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            List<Stream<String>> columnCount = m.columnStreams().toList();
            assertEquals(3, columnCount.size());
        }

        @Test
        public void testColumnStreams_WithRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            List<Stream<String>> columnCount = m.columnStreams(1, 3).toList();
            assertEquals(2, columnCount.size());
        }

        @Test
        public void testColumnStreams_ColumnRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            List<List<String>> columnCount = m.columnStreams(1, 3).map(Stream::toList).toList();

            assertEquals(2, columnCount.size());
            assertEquals("B", columnCount.get(0).get(0));
            assertEquals("C", columnCount.get(1).get(0));
        }

        @Test
        public void testColumnStreams_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            List<List<String>> columnCount = m.columnStreams().map(Stream::toList).toList();

            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).size());
            assertEquals("A", columnCount.get(0).get(0));
            assertEquals("C", columnCount.get(0).get(1));
            assertEquals("B", columnCount.get(1).get(0));
            assertEquals("D", columnCount.get(1).get(1));
        }

        @Test
        public void testColumnStreams_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<List<Integer>> columnCount = m.columnStreams().map(Stream::toList).toList();

            assertEquals(3, columnCount.size());
            assertEquals(2, columnCount.get(0).size());
            assertEquals(1, columnCount.get(0).get(0));
            assertEquals(6, columnCount.get(2).get(1));
        }

        @Test
        public void testColumnStreams_StreamsColumns() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Stream<Stream<String>> streams = m.columnStreams();
            assertEquals(2, streams.count());
        }

        @Test
        public void testColumnStreams_WithRange_Values() {
            String[][] arr = { { "a", "b", "c" }, { "d", "e", "f" } };
            Matrix<String> m = new Matrix<>(arr);
            Stream<Stream<String>> streams = m.columnStreams(1, 3);
            assertEquals(2, streams.count());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testColumnStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final Integer[][] expected = { new Integer[] { 1, 4, 7 }, new Integer[] { 2, 5, 8 }, new Integer[] { 3, 6, 9 } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnStreams()) {
                final var iterator = (ObjIteratorEx<Stream<Integer>>) stream.iterator();
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
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            final Integer[] expected = new Integer[] { 2, 5, 8 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (ObjIteratorEx<Integer>) values.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }

        @Test
        public void testColumnStreams_InnerCountConsumesOnlyRemainingValues() {
            final var matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (ObjIteratorEx<Integer>) values.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(2, iterator.next());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
        }

        @Test
        public void testColumnStreams_Empty() {
            Matrix<String> empty = Matrix.wrap(new String[0][0]);
            assertEquals(0, empty.columnStreams().count());
        }

        @Test
        public void testColumnStreams_WithRange_OutOfBounds() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(0, 3));
        }
    }

    @Nested
    class LengthTests {

        @Test
        public void testLength() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 } });
            Assertions.assertEquals(3, matrix.length(matrix.a[0]));
            Assertions.assertEquals(0, matrix.length(null));
        }

        @Test
        public void testLength_WithNullArray() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A" } });
            assertEquals(0, m.length(null));
        }
    }

    @Nested
    class ForEachTests {

        @Test
        public void testElementActionsMayStopBeforeVisitingEveryCell() {
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                final Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 } });
                final IllegalStateException failure = new IllegalStateException("stop");
                final List<Integer> visitedAll = new ArrayList<>();
                final List<Integer> visitedRegion = new ArrayList<>();

                Assertions.assertSame(failure, assertThrows(IllegalStateException.class, () -> matrix.forEach(value -> {
                    visitedAll.add(value);
                    if (value == 2) {
                        throw failure;
                    }
                })));
                Assertions.assertSame(failure, assertThrows(IllegalStateException.class, () -> matrix.forEach(0, 1, 0, 3, value -> {
                    visitedRegion.add(value);
                    if (value == 2) {
                        throw failure;
                    }
                })));

                assertEquals(List.of(1, 2), visitedAll);
                assertEquals(List.of(1, 2), visitedRegion);
            });
        }

        @Test
        public void testForEach() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            List<Integer> values = new ArrayList<>();

            matrix.forEach(it -> values.add(it));

            Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), values);
        }

        @Test
        public void testForEachRegion() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Integer> values = new ArrayList<>();

            matrix.forEach(1, 3, 1, 3, it -> values.add(it));

            Assertions.assertEquals(Arrays.asList(5, 6, 8, 9), values);
        }

        @Test
        public void testForEachWithForcedParallelMode_EdgeCase() throws Exception {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Integer> values = java.util.Collections.synchronizedList(new ArrayList<>());
            List<Integer> regionValues = java.util.Collections.synchronizedList(new ArrayList<>());

            Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> matrix.forEach(values::add));
            Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> matrix.forEach(0, 2, 1, 3, regionValues::add));

            Assertions.assertEquals(6, values.size());
            Assertions.assertTrue(values.containsAll(Arrays.asList(1, 2, 3, 4, 5, 6)));
            Assertions.assertEquals(4, regionValues.size());
            Assertions.assertTrue(regionValues.containsAll(Arrays.asList(2, 3, 5, 6)));
        }

        @Test
        public void testForEach_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            List<String> values = new ArrayList<>();
            m.forEach(v -> values.add(v));
            assertEquals(4, values.size());
            assertTrue(values.contains("A"));
            assertTrue(values.contains("D"));
        }

        @Test
        public void testForEach_WithRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            List<String> values = new ArrayList<>();
            m.forEach(1, 3, 1, 3, v -> values.add(v));
            assertEquals(4, values.size());
            assertTrue(values.contains("E"));
            assertTrue(values.contains("I"));
            assertFalse(values.contains("A"));
        }

        @Test
        public void testForEach_FullMatrix() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            List<Integer> collected = new ArrayList<>();
            m.forEach(0, 2, 0, 2, (Throwables.Consumer<Integer, RuntimeException>) collected::add);
            assertEquals(4, collected.size());
            assertTrue(collected.contains(1));
            assertTrue(collected.contains(4));
        }

        @Test
        public void testForEach_IteratesAllElements() {
            Integer[][] arr = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> m = new Matrix<>(arr);
            final int[] sum = { 0 };
            m.forEach(val -> sum[0] += val);
            assertEquals(10, sum[0]);
        }

        @Test
        public void testForEachNullAction() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.forEach((Throwables.Consumer<Integer, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.forEach(0, 2, 0, 2, (Throwables.Consumer<Integer, RuntimeException>) null));
        }

        @Test
        public void testForEach_EmptyRange() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            List<Integer> collected = new ArrayList<>();
            m.forEach(0, 0, 0, 0, (Throwables.Consumer<Integer, RuntimeException>) collected::add);
            assertEquals(0, collected.size());
        }
    }

    @Nested
    class ToDatasetTests {

        @Test
        public void testToDatasetH() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<String> columnNames = Arrays.asList("A", "B", "C");

            Dataset dataset = matrix.toDataset(columnNames);

            Assertions.assertEquals(2, dataset.size());
            Assertions.assertEquals(3, dataset.columnCount());
            Assertions.assertEquals(Arrays.asList(1, 4), dataset.getColumn("A"));
        }

        @Test
        public void testToDatasetHWrongColumnCount() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 } });
            List<String> columnNames = Arrays.asList("A", "B");

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                matrix.toDataset(columnNames);
            });
        }

        @Test
        public void testToDatasetH_Shape2By3() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<String> columnNames = Arrays.asList("A", "B", "C");
            Dataset dataset = m.toDataset(columnNames);

            assertNotNull(dataset);
            assertEquals(3, dataset.columnNames().size());
            assertEquals(2, dataset.size());
            assertEquals(Integer.valueOf(1), dataset.getColumn("A").get(0));
            assertEquals(Integer.valueOf(6), dataset.getColumn("C").get(1));
        }

        @Test
        public void testToDatasetH_WrongColumnCount() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<String> columnNames = Arrays.asList("A", "B");
            assertThrows(IllegalArgumentException.class, () -> m.toDataset(columnNames));
        }

        @Test
        public void testToDatasetH_ConvertsToDataset() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Dataset ds = m.toDataset(Arrays.asList("col1", "col2"));
            assertNotNull(ds);
            assertEquals(2, ds.size());
        }

        @Test
        public void testDatasetNameChecksPreserveConstructorDiagnostics() {
            final Matrix<String> matrix = Matrix.copyOf(String.class, new String[][] { { "a", "b" }, { "c", "d" } });
            assertEquals("Duplicated column names found in: [name, name]",
                    assertThrows(IllegalArgumentException.class, () -> matrix.toDataset(List.of("name", "name"))).getMessage());
            assertEquals("Empty column name found in: [name, ]",
                    assertThrows(IllegalArgumentException.class, () -> matrix.toTransposedDataset(List.of("name", ""))).getMessage());
        }

        @Test
        public void testEmptyMatrixDatasetConversions() {
            Matrix<Integer> matrix = Matrix.copyOf(new Integer[0][0]);

            Dataset rowDataset = matrix.toDataset(List.of());
            Dataset transposedDataset = matrix.toTransposedDataset(List.of());

            Assertions.assertEquals(0, rowDataset.columnCount());
            Assertions.assertEquals(0, rowDataset.size());
            Assertions.assertEquals(0, transposedDataset.columnCount());
            Assertions.assertEquals(0, transposedDataset.size());
        }

        @Test
        public void testToDatasetHNullColumnNames() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.toDataset(null));
        }

        @Test
        public void testDatasetConversionsRetainHeaderValidationAndDegenerateShapes() {
            final Matrix<Object> matrix = Matrix.wrap(Object.class, new Object[2][2]);
            for (final List<String> names : Arrays.asList(null, List.of("one"), List.of("same", "same"), Arrays.asList("valid", null), List.of("valid", ""))) {
                assertThrows(IllegalArgumentException.class, () -> matrix.toDataset(names));
                assertThrows(IllegalArgumentException.class, () -> matrix.toTransposedDataset(names));
            }

            final Matrix<Object> zeroByTwo = Matrix.empty(Object.class, 2);
            final Matrix<Object> twoByZero = Matrix.wrap(Object.class, new Object[2][0]);
            assertEquals(0, zeroByTwo.toDataset(List.of("a", "b")).size());
            assertEquals(2, zeroByTwo.toDataset(List.of("a", "b")).columnCount());
            assertEquals(0, twoByZero.toTransposedDataset(List.of("a", "b")).size());
            assertEquals(2, twoByZero.toTransposedDataset(List.of("a", "b")).columnCount());
            assertThrows(IllegalArgumentException.class, () -> twoByZero.toDataset(List.of()));
            assertThrows(IllegalArgumentException.class, () -> zeroByTwo.toTransposedDataset(List.of()));
        }

        @Test
        public void testToDatasetHRejectsRowsWithZeroColumns() {
            Matrix<String> matrix = Matrix.wrap(new String[3][0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.toDataset(List.of()));
        }
    }

    @Nested
    class ToTransposedDatasetTests {

        @Test
        public void testToDatasetV() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<String> columnNames = Arrays.asList("Row1", "Row2");

            Dataset dataset = matrix.toTransposedDataset(columnNames);

            Assertions.assertEquals(3, dataset.size());
            Assertions.assertEquals(2, dataset.columnCount());
            Assertions.assertEquals(Arrays.asList(1, 2, 3), dataset.getColumn("Row1"));
        }

        @Test
        public void testToDatasetVWrongColumnCount() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 } });
            List<String> columnNames = Arrays.asList("Row1", "Row2");

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                matrix.toTransposedDataset(columnNames);
            });
        }

        @Test
        public void testToDatasetV_Shape2By3() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<String> columnNames = Arrays.asList("Row1", "Row2");
            Dataset dataset = m.toTransposedDataset(columnNames);

            assertNotNull(dataset);
            assertEquals(2, dataset.columnNames().size());
            assertEquals(3, dataset.size());
            assertEquals(Integer.valueOf(1), dataset.getColumn("Row1").get(0));
            assertEquals(Integer.valueOf(6), dataset.getColumn("Row2").get(2));
        }

        @Test
        public void testToDatasetV_WrongColumnCount() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<String> columnNames = Arrays.asList("A", "B", "C");
            assertThrows(IllegalArgumentException.class, () -> m.toTransposedDataset(columnNames));
        }

        @Test
        public void testToDatasetV_ConvertsToDataset() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            Dataset ds = m.toTransposedDataset(Arrays.asList("col1", "col2"));
            assertNotNull(ds);
            assertEquals(2, ds.size());
        }

        @Test
        public void testToDatasetVNullColumnNames() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 } });

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.toTransposedDataset(null));
        }

        @Test
        public void testTransposedDatasetRejectsShapesItCannotRepresent() {
            final Matrix<String> zeroByThree = Matrix.empty(String.class, 3);
            assertShape(zeroByThree, 0, 3);
            assertThrows(IllegalArgumentException.class, () -> zeroByThree.toTransposedDataset(List.of()));

            // The mirror case was already guarded, and both 0 x 0 forms still work.
            assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(new String[3][0]).toDataset(List.of()));
            assertEquals(0, Matrix.empty(String.class).toTransposedDataset(List.of()).size());
            assertEquals(0, Matrix.empty(String.class).toDataset(List.of()).size());
        }
    }

    @Nested
    class ToMultilineStringTests {

        @Test
        public void testToMultilineString_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            String output = m.toMultilineString();
            assertNotNull(output);
            assertTrue(output.contains("1"));
            assertTrue(output.contains("4"));
        }

        @Test
        public void testToMultilineString_ReturnsString() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            String result = m.toMultilineString();
            assertNotNull(result);
            assertTrue(result.length() > 0);
        }

        @Test
        public void testToMultilineString_NonEmpty_ReturnsNonNullString() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            String result = matrix.toMultilineString();
            assertNotNull(result);
            assertTrue(result.contains("1") && result.contains("4"));
        }

        @Test
        public void testToMultilineString_Empty_ReturnsNonNullString() {
            Matrix<String> empty = Matrix.empty(String.class);
            String result = empty.toMultilineString();
            assertNotNull(result);
            assertEquals("[]", result);
        }
    }

    @Nested
    class HashCodeTests {

        @Test
        public void testHashCode() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m3 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 5 } });

            Assertions.assertEquals(m1.hashCode(), m2.hashCode());
            Assertions.assertNotEquals(m1.hashCode(), m3.hashCode());
        }

        @Test
        public void testHashCode_Shape2By2() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> m3 = Matrix.wrap(new String[][] { { "A", "B" }, { "X", "D" } });

            assertEquals(m1.hashCode(), m2.hashCode());
            assertNotEquals(m1.hashCode(), m3.hashCode()); // Usually different
        }

        @Test
        public void testHashCode_Strings() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        public void testHashCode_Integers() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        public void testHashCode_ConsistentWithEquals() {
            String[][] arr1 = { { "a", "b" }, { "c", "d" } };
            String[][] arr2 = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m1 = new Matrix<>(arr1);
            Matrix<String> m2 = new Matrix<>(arr2);
            assertEquals(m1.hashCode(), m2.hashCode());
        }

        @Test
        public void testHashCode_ArrayTypedElements_AreContentBased() {
            Matrix<int[]> m1 = Matrix.wrap(new int[][][] { { { 1, 2 } } });
            Matrix<int[]> m2 = Matrix.wrap(new int[][][] { { { 1, 2 } } });
            assertTrue(m1.get(0, 0) != m2.get(0, 0)); // distinct int[] instances
            assertEquals(m1, m2); // equals is deep (content-based)
            assertEquals(m1.hashCode(), m2.hashCode()); // hashCode is consistently deep
        }

        @Test
        public void testHashCode_NullFilledRectanglesHaveEqualHashes() {
            final Matrix<String> first = Matrix.wrap(new String[2][3]);
            final Matrix<String> equal = Matrix.wrap(new String[2][3]);
            assertEquals(first, equal);
            assertEquals(first.hashCode(), equal.hashCode());
        }
    }

    @Nested
    class EqualsTests {

        @Test
        public void testEquals() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m3 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 5 } });
            Matrix<Integer> m4 = Matrix.wrap(new Integer[][] { { 1, 2 } });

            assertTrue(m1.equals(m1)); // same instance
            Assertions.assertEquals(m1, m2); // equal content
            Assertions.assertNotEquals(m1, m3); // different content
            Assertions.assertNotEquals(m1, m4); // different dimensions
            Assertions.assertNotEquals(m1, null);
            Assertions.assertNotEquals(m1, "not a matrix");
        }

        @Test
        public void testEquals_Same_Strings() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            assertEquals(m1, m2);
        }

        @Test
        public void testEquals_Same_Integers() {
            Matrix<Integer> m1 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(m1, m2);
        }

        @Test
        public void testEquals_Different() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "X" } });
            assertNotSame(m1, m2);
        }

        @Test
        public void testEquals_SameContent() {
            String[][] arr1 = { { "a", "b" }, { "c", "d" } };
            String[][] arr2 = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m1 = new Matrix<>(arr1);
            Matrix<String> m2 = new Matrix<>(arr2);
            assertEquals(m1, m2);
        }

        @Test
        public void testEquals_DifferentContent() {
            String[][] arr1 = { { "a", "b" }, { "c", "d" } };
            String[][] arr2 = { { "a", "b" }, { "c", "X" } };
            Matrix<String> m1 = new Matrix<>(arr1);
            Matrix<String> m2 = new Matrix<>(arr2);
            assertNotEquals(m1, m2);
        }

        @Test
        public void testEquals_DifferentDimensions() {
            String[][] arr1 = { { "a", "b" }, { "c", "d" } };
            String[][] arr2 = { { "a", "b", "c" } };
            Matrix<String> m1 = new Matrix<>(arr1);
            Matrix<String> m2 = new Matrix<>(arr2);
            assertNotEquals(m1, m2);
        }

        @Test
        public void testEquals_DifferentRowCountsWithEqualColumnCounts() {
            final var longer = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            final var shorter = Matrix.wrap(new Integer[][] { { 1, 2 } });
            assertFalse(longer.equals(shorter));
            assertFalse(shorter.equals(longer));
        }

        @Test
        public void testEquals_Shape2By2_NullInput() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> m3 = Matrix.wrap(new String[][] { { "A", "B" }, { "X", "D" } });
            Matrix<String> m4 = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });

            assertTrue(m1.equals(m1)); // Same object
            assertTrue(m1.equals(m2)); // Same values
            assertFalse(m1.equals(m3)); // Different values
            assertFalse(m1.equals(m4)); // Different dimensions
            assertFalse(m1.equals(null));
            assertFalse(m1.equals("not a matrix"));
        }
    }

    @Nested
    class ToStringTests {

        @Test
        public void testToString() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            String str = matrix.toString();

            Assertions.assertTrue(str.contains("1"));
            Assertions.assertTrue(str.contains("2"));
            Assertions.assertTrue(str.contains("3"));
            Assertions.assertTrue(str.contains("4"));
        }

        @Test
        public void testToString_Shape2By2() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("A") || str.contains("["));
        }

        @Test
        public void testToString_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("A"));
            assertTrue(str.contains("D"));
        }

        @Test
        public void testToString_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1"));
            assertTrue(str.contains("4"));
        }

        @Test
        public void testToString_ReturnsStringRepresentation() {
            String[][] arr = { { "a", "b" }, { "c", "d" } };
            Matrix<String> m = new Matrix<>(arr);
            String result = m.toString();
            assertNotNull(result);
            assertTrue(result.contains("a"));
            assertTrue(result.contains("d"));
        }
    }

    @Nested
    class ElementTypeTests {

        @Test
        public void testElementType() {
            Matrix<String> matrix = Matrix.wrap(new String[][] { { "a", "b" } });
            Assertions.assertEquals(String.class, matrix.elementType());

            Matrix<Integer> intMatrix = Matrix.wrap(new Integer[][] { { 1, 2 } });
            Assertions.assertEquals(Integer.class, intMatrix.elementType());

            Matrix<String> repeated = Matrix.wrap(new String[][] { { "x", "x" } });
            Assertions.assertEquals(String.class, repeated.elementType());
        }

        @Test
        public void testElementType_Shape1By1() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A" } });
            assertEquals(String.class, m.elementType());
        }

        @Test
        public void testElementType_WithInteger() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1 } });
            assertEquals(Integer.class, m.elementType());
        }

        @Test
        public void testElementType_Integer() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 } });
            assertEquals(Integer.class, m.elementType());
        }

        @Test
        public void testElementType_BigDecimal() {
            Matrix<BigDecimal> m = Matrix.wrap(new BigDecimal[][] { { BigDecimal.ONE, BigDecimal.TEN } });
            assertEquals(BigDecimal.class, m.elementType());
        }

        @Test
        public void testElementType_ReturnsElementClass() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a" } });
            assertEquals(String.class, m.elementType());

            Matrix<Integer> m2 = Matrix.wrap(new Integer[][] { { 1 } });
            assertEquals(Integer.class, m2.elementType());
        }

        @Test
        public void testExactStorageValidationKeepsMessagesAndValidationOrder() {
            final IllegalArgumentException outerFailure = assertThrows(IllegalArgumentException.class,
                    () -> Matrix.wrap(Number.class, new Integer[][] { { 1 } }));
            assertEquals("Matrix outer array must have exact element type java.lang.Number but has java.lang.Integer. "
                    + "Use copyOf(Class, Object[][]) to widen covariant input", outerFailure.getMessage());

            final Number[][] covariantRows = { new Integer[] { 1 }, null };
            final IllegalArgumentException rowFailure = assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(Number.class, covariantRows));
            assertEquals("Row 0 must have exact runtime type java.lang.Number[] but has java.lang.Integer[]. "
                    + "Use copyOf(Class, Object[][]) to canonicalize covariant rows", rowFailure.getMessage());
            assertEquals("Row 0 cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(Number.class, new Number[][] { null, new Integer[] { 1 } })).getMessage());

            final Matrix<Integer> normalized = Matrix.wrap(int.class, new Integer[][] { { 1 } });
            assertSame(Integer.class, normalized.elementType());
            final int[] element = { 1, 2 };
            final Matrix<int[]> arrays = Matrix.wrap(int[].class, new int[][][] { { element } });
            assertSame(int[].class, arrays.elementType());
            assertSame(element, arrays.get(0, 0));
        }

        @Test
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void testWrapWithElementType_AcceptsTheErasedHolderOfAGenericVarargsCall() {
            // Called from generic code, wrap(Class, T[]...) receives an Object[][] holder even though both rows are exactly String[].
            final String[] first = { "a", "b" };
            final String[] second = { "c", "d" };
            final Matrix<String> matrix = wrapTwoRowsGenerically(String.class, first, second);

            assertSame(String.class, matrix.elementType());
            assertEquals("c", matrix.get(1, 0));
            first[0] = "x";
            assertEquals("x", matrix.get(0, 0));
            matrix.set(0, 1, "y");
            assertEquals("y", first[1]);
            assertEquals(Matrix.wrap(String.class, new String[][] { { "x", "c" }, { "y", "d" } }), matrix.transpose());

            final Matrix<String> noRows = wrapNoRowsGenerically(String.class);
            assertEquals(0, noRows.rowCount());
            assertSame(String.class, noRows.elementType());

            // The rows themselves must still be exact.
            assertEquals(
                    "Row 0 must have exact runtime type java.lang.Number[] but has java.lang.Integer[]. "
                            + "Use copyOf(Class, Object[][]) to canonicalize covariant rows",
                    assertThrows(IllegalArgumentException.class, () -> wrapTwoRowsGenerically(Number.class, new Integer[] { 1 }, new Number[] { 2 }))
                            .getMessage());
            assertEquals("Row 1 cannot be null",
                    assertThrows(IllegalArgumentException.class, () -> wrapTwoRowsGenerically(String.class, first, null)).getMessage());
            assertThrows(IllegalArgumentException.class, () -> wrapTwoRowsGenerically(String.class, first, first));

            // The non-varargs constructor keeps requiring an exactly typed outer array.
            assertEquals(
                    "Matrix outer array must have exact element type java.lang.String but has java.lang.Object. "
                            + "Use copyOf(Class, Object[][]) to widen covariant input",
                    assertThrows(IllegalArgumentException.class, () -> new Matrix((Class) String.class, new Object[][] { second })).getMessage());
        }

        private static <X> Matrix<X> wrapTwoRowsGenerically(final Class<X> elementType, final X[] first, final X[] second) {
            return Matrix.wrap(elementType, first, second);
        }

        private static <X> Matrix<X> wrapNoRowsGenerically(final Class<X> elementType) {
            return Matrix.wrap(elementType);
        }

        @Test
        public void testExactStorageKeepsTheRowsItValidatedWhileTheCallerReplacesThem() throws InterruptedException {
            // A writer keeps swapping row 0 of the caller's outer array between an exact Object[] row and a covariant
            // String[] row. Every matrix built meanwhile must be rejected or hold the exact row: validating the caller's
            // array but storing a later snapshot of it let the String[] row in, so set(0, 0, 42) threw ArrayStoreException.
            final Object[] exactRow = { "e" };
            final Object[] covariantRow = new String[] { "c" };
            final Object[][] holder = { exactRow, new Object[] { "x" } };
            final java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(true);
            final Thread writer = new Thread(() -> {
                for (int n = 0; running.get(); n++) {
                    holder[0] = (n & 1) == 0 ? covariantRow : exactRow;
                }
            });
            writer.setDaemon(true);
            writer.start();
            int built = 0;

            try {
                for (int i = 0; i < 200_000; i++) {
                    try {
                        final Matrix<Object> matrix = (i & 1) == 0 ? Matrix.wrap(Object.class, holder) : new Matrix<>(Object.class, holder);
                        assertSame(Object[].class, matrix.rowView(0).getClass());
                        built++;
                    } catch (final IllegalArgumentException covariantRowSeen) {
                        // The String[] row was the one read, and it was rejected.
                    }
                }
            } finally {
                running.set(false);
                writer.join();
            }

            assertTrue(built > 0);
        }

        @Test
        @SuppressWarnings("deprecation")
        public void testRuntimeTypedVarargsFactoriesAreNotSafeVarargs() throws NoSuchMethodException {
            // Both factories take the element type from the varargs array's runtime type, so a generic caller's erased
            // Object[][] gives a Matrix<String> whose returned T[] arrays are Object[]; the compiler must keep warning there.
            assertFalse(Matrix.class.getMethod("wrap", Object[][].class).isAnnotationPresent(SafeVarargs.class));
            assertFalse(Matrix.class.getMethod("copyOf", Object[][].class).isAnnotationPresent(SafeVarargs.class));
            assertSame(Object.class, wrapGenericallyWithoutElementType(new String[] { "a" }).elementType());

            // The explicit-type overloads re-home or copy into exactly typed storage, so they stay @SafeVarargs.
            assertTrue(Matrix.class.getMethod("wrap", Class.class, Object[][].class).isAnnotationPresent(SafeVarargs.class));
            assertTrue(Matrix.class.getMethod("copyOf", Class.class, Object[][].class).isAnnotationPresent(SafeVarargs.class));
        }

        @SuppressWarnings({ "deprecation", "unchecked" })
        private static <X> Matrix<X> wrapGenericallyWithoutElementType(final X[] row) {
            return Matrix.wrap(row);
        }

        @Test
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void testStorabilityAndStackElementTypeChecksKeepTheirMessages() {
            final IllegalArgumentException storability = assertThrows(IllegalArgumentException.class, () -> new Matrix(new String[0][], Integer.class, 0));
            assertEquals("Element type java.lang.Integer is not storable in backing rows of type java.lang.String[]", storability.getMessage());

            final Matrix numbers = Matrix.wrap(Number.class, new Number[][] { { 1 } });
            final String thisMessage = "Target element type java.lang.Integer cannot safely store this matrix's runtime element type java.lang.Number";
            assertEquals(thisMessage, assertThrows(IllegalArgumentException.class, () -> numbers.stackVertically(numbers, Integer.class)).getMessage());
            assertEquals(thisMessage, assertThrows(IllegalArgumentException.class, () -> numbers.stackHorizontally(numbers, Integer.class)).getMessage());

            final Matrix strings = Matrix.wrap(String.class, new String[][] { { "a" } });
            final Matrix objects = Matrix.wrap(Object.class, new Object[][] { { 1 } });
            final String otherMessage = "Target element type java.lang.String cannot safely store the other matrix's runtime element type java.lang.Object";
            assertEquals(otherMessage, assertThrows(IllegalArgumentException.class, () -> strings.stackVertically(objects, String.class)).getMessage());
            assertEquals(otherMessage, assertThrows(IllegalArgumentException.class, () -> strings.stackHorizontally(objects, String.class)).getMessage());

            final Matrix<Object> stacked = objects.stackVertically(strings, Object.class);
            assertSame(Object.class, stacked.elementType());
            assertEquals(Arrays.asList(1, "a"), stacked.flatten());
            assertEquals(Arrays.asList(1, "a"), objects.stackHorizontally(strings, Object.class).flatten());
        }

        @Test
        public void testTransformsProduceUniformlyWritableRowsForCovariantStorage() {
            final Number[][] backing = new Number[][] { new Long[] { 1L, 2L }, new Long[] { 3L, 4L } };
            final Matrix<Number> m = Matrix.wrap(backing);
            assertEquals(Number.class, m.elementType());

            // rotate180 used to clone the source row and so kept Long[], unlike every sibling transform.
            m.copy().set(0, 0, 2.5d);
            m.transpose().set(0, 0, 2.5d);
            m.rotate90().set(0, 0, 2.5d);
            m.rotate180().set(0, 0, 2.5d);
            m.rotate270().set(0, 0, 2.5d);
            m.flipHorizontally().set(0, 0, 2.5d);
            m.flipVertically().set(0, 0, 2.5d);
            assertEquals(Number[].class, m.rotate180().unsafeBackingArray()[0].getClass());
            // and it still reverses in both directions
            assertArrayEquals(new Number[] { 4L, 3L }, m.rotate180().rowCopy(0));

            // The n-ary stack used to disagree with the pairwise one.
            final Matrix<Number> pairwise = m.stackVertically(m.copy());
            final Matrix<Number> nary = Matrices.stackVertically(List.of(m, m.copy()));
            pairwise.set(0, 0, 2.5d);
            nary.set(0, 0, 2.5d);
            assertEquals(Number[].class, nary.unsafeBackingArray()[0].getClass());
            assertShape(nary, 4, 2);
        }

        @Test
        public void testArrayTypedElementsAreNotRewrittenByElementTypeNormalisation() {
            // ClassUtil.wrap maps int[] to Integer[] as readily as it maps int to Integer, but the backing
            // rows of a Matrix<int[]> really are int[][] -- wrapping would make elementType disagree with them.
            final Matrix<int[]> m = Matrix.wrap(new int[][][] { { { 1, 2 }, { 3 } }, { { 4 }, { 5, 6 } } });
            assertEquals(int[].class, m.elementType());
            assertShape(m, 2, 2);

            assertArrayEquals(new int[] { 1, 2 }, m.copy().get(0, 0));
            assertArrayEquals(new int[] { 5, 6 }, m.transpose().get(1, 1));
            assertArrayEquals(new int[] { 5, 6 }, m.rotate180().get(0, 0));
            assertArrayEquals(new int[] { 3 }, m.rotate90().get(1, 1));
            assertArrayEquals(new int[] { 1, 2 }, m.rotate90().get(0, 1));
            assertEquals(int[].class, m.copy().elementType());
            assertEquals(int[].class, m.resize(3, 3).elementType());

            // The explicit-token factory agrees.
            final Matrix<int[]> viaToken = Matrix.wrap(int[].class, new int[][][] { { { 7 } } });
            assertEquals(int[].class, viaToken.elementType());
            assertArrayEquals(new int[] { 7 }, viaToken.copy().get(0, 0));

            // A primitive SCALAR token is still normalised to its wrapper.
            assertEquals(Integer.class, IntMatrix.wrap(new int[][] { { 1 } }).mapToObj(i -> i, int.class).elementType());
        }

        @Test
        public void testArrayElementTypesSurviveEveryAllocationPath() {
            // newMatrixArray is the allocation path that copyOf / map / zipWith / mapToObj all go through;
            // wrapping int[] to Integer[] there produced storage that no longer matched the element type.
            final Object[][] raw = Matrices.newMatrixArray(2, 2, int[].class);
            assertEquals(int[][].class, raw.getClass().getComponentType());

            assertShape(IntMatrix.empty().mapToObj(x -> new int[] { x }, int[].class), 0, 0);
            assertShape(IntMatrix.random(0, 3).mapToObj(x -> new int[] { x }, int[].class), 0, 3);
            assertShape(LongMatrix.empty().mapToObj(x -> new int[] { (int) x }, int[].class), 0, 0);
            assertShape(Matrix.empty(String.class).map(v -> new int[0], int[].class), 0, 0);

            final Matrix<int[]> mapped = Matrix.wrap(new String[][] { { "a", "bb" } }).map(v -> new int[] { v.length() }, int[].class);
            assertEquals(int[].class, mapped.elementType());
            assertArrayEquals(new int[] { 2 }, mapped.get(0, 1));

            final Matrix<int[]> copied = Matrix.copyOf(int[].class, new int[][][] { { { 1 }, { 2 } } });
            assertEquals(int[].class, copied.elementType());
            assertArrayEquals(new int[] { 2 }, copied.get(0, 1));

            final Matrix<String> one = Matrix.wrap(new String[][] { { "a" } });
            assertArrayEquals(new int[] { 2 }, one.zipWith(one, (p, q) -> new int[] { p.length() + q.length() }, int[].class).get(0, 0));
            assertEquals(int[].class, Matrix.ofMainDiagonal(int[].class, new int[][] { { 1 }, { 2 } }).elementType());

            // A primitive SCALAR token must still be normalised to its wrapper.
            assertEquals(Integer.class, IntMatrix.wrap(new int[][] { { 1 } }).mapToObj(i -> i, int.class).elementType());
        }

        @Test
        public void testTypedEmptyPreservesZeroByNShapeAcrossResults() {
            Matrix<String> empty = Matrix.empty(String.class, 3);

            assertEquals(0, empty.rowCount());
            assertEquals(3, empty.columnCount());
            assertEquals(String.class, empty.elementType());
            assertEquals(3, empty.copy().columnCount());
            assertEquals(2, empty.copyRegion(0, 0, 1, 3).columnCount());
            assertEquals(3, empty.map(String::toUpperCase).columnCount());
            assertEquals(3, empty.mapToBoolean(value -> true).columnCount());
            assertEquals(3, empty.mapToByte(value -> (byte) 1).columnCount());
            assertEquals(3, empty.mapToChar(value -> 'x').columnCount());
            assertEquals(3, empty.mapToShort(value -> (short) 1).columnCount());
            assertEquals(3, empty.mapToInt(String::length).columnCount());
            assertEquals(3, empty.mapToLong(String::length).columnCount());
            assertEquals(3, empty.mapToFloat(value -> 1f).columnCount());
            assertEquals(3, empty.mapToDouble(value -> 1d).columnCount());
            assertEquals(3, empty.flipHorizontally().columnCount());
            assertEquals(3, empty.flipVertically().columnCount());
            assertEquals(3, empty.rotate180().columnCount());
            assertEquals(6, empty.repeatMatrix(2, 2).columnCount());
            assertEquals(6, empty.stackHorizontally(Matrix.empty(String.class, 3), String.class).columnCount());
            assertEquals(3, empty.zipWith(Matrix.empty(String.class, 3), (left, right) -> left).columnCount());

            Matrix<String> transposed = empty.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(0, transposed.columnCount());
            assertEquals(3, empty.columnStreams().count());

            Matrix<String> roundTrip = transposed.transpose();
            assertEquals(0, roundTrip.rowCount());
            assertEquals(3, roundTrip.columnCount());
        }
    }

    @Nested
    class UnsafeBackingArrayTests {

        @Test
        public void testArray_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            String[][] array = m.unsafeBackingArray();
            assertArrayEquals(new String[] { "A", "B" }, array[0]);
            assertArrayEquals(new String[] { "C", "D" }, array[1]);
        }

        @Test
        public void testArray_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            Integer[][] array = m.unsafeBackingArray();
            assertArrayEquals(new Integer[] { 1, 2 }, array[0]);
            assertArrayEquals(new Integer[] { 3, 4 }, array[1]);
        }
    }

    @Nested
    class ElementCountTests {

        @Test
        public void testLargeMatrix() {
            int size = 100;
            Integer[][] data = new Integer[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    data[i][j] = i * size + j;
                }
            }

            Matrix<Integer> matrix = Matrix.wrap(data);
            Assertions.assertEquals(size, matrix.rowCount());
            Assertions.assertEquals(size, matrix.columnCount());
            Assertions.assertEquals(size * size, matrix.elementCount());
            Assertions.assertEquals(5050, matrix.get(50, 50));
        }

        @Test
        public void testIntAndGenericMatrixParity() {
            final Random random = new Random(123456789L);

            for (int n = 0; n < 400; n++) {
                final int rows = random.nextInt(5);
                final int cols = rows == 0 ? 0 : random.nextInt(5);
                final int[][] source = new int[rows][cols];
                final Integer[][] boxedSource = new Integer[rows][cols];

                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        final int v = random.nextInt(101) - 50;
                        source[i][j] = v;
                        boxedSource[i][j] = v;
                    }
                }

                final IntMatrix intMatrix = IntMatrix.wrap(source);
                final Matrix<Integer> genericMatrix = Matrix.wrap(boxedSource);

                assertEquals(intMatrix.rowCount(), genericMatrix.rowCount());
                assertEquals(intMatrix.columnCount(), genericMatrix.columnCount());
                assertEquals(intMatrix.elementCount(), genericMatrix.elementCount());
                assertEquals(intMatrix.isEmpty(), genericMatrix.isEmpty());

                assertArrayEquals(intMatrix.rowMajorStream().toArray(), toIntArray(genericMatrix.rowMajorStream().toList()));
                assertArrayEquals(intMatrix.columnMajorStream().toArray(), toIntArray(genericMatrix.columnMajorStream().toList()));

                final List<int[]> intRows = intMatrix.rowStreams().map(IntStream::toArray).toList();
                final List<int[]> genericRows = genericMatrix.rowStreams().map(row -> toIntArray(row.toList())).toList();
                assertEquals(intRows.size(), genericRows.size());
                for (int i = 0; i < intRows.size(); i++) {
                    assertArrayEquals(intRows.get(i), genericRows.get(i));
                }

                final List<int[]> intCols = intMatrix.columnStreams().map(IntStream::toArray).toList();
                final List<int[]> genericCols = genericMatrix.columnStreams().map(col -> toIntArray(col.toList())).toList();
                assertEquals(intCols.size(), genericCols.size());
                for (int i = 0; i < intCols.size(); i++) {
                    assertArrayEquals(intCols.get(i), genericCols.get(i));
                }

                assertTransformParity(intMatrix::transpose, genericMatrix::transpose);
                assertTransformParity(intMatrix::rotate90, genericMatrix::rotate90);
                assertMatrixEquals(intMatrix.rotate180(), genericMatrix.rotate180());
                assertTransformParity(intMatrix::rotate270, genericMatrix::rotate270);
                assertMatrixEquals(intMatrix.flipHorizontally(), genericMatrix.flipHorizontally());
                assertMatrixEquals(intMatrix.flipVertically(), genericMatrix.flipVertically());

                final int rowRepeats = random.nextInt(3) + 1;
                final int columnRepeats = random.nextInt(3) + 1;
                assertMatrixEquals(intMatrix.repeatElements(rowRepeats, columnRepeats), genericMatrix.repeatElements(rowRepeats, columnRepeats));
                assertMatrixEquals(intMatrix.repeatMatrix(rowRepeats, columnRepeats), genericMatrix.repeatMatrix(rowRepeats, columnRepeats));

                final int elements = (int) intMatrix.elementCount();
                int newRows = 0;
                int newCols = 0;

                if (elements == 0) {
                    newRows = random.nextInt(5);
                    newCols = newRows == 0 ? 0 : random.nextInt(5);
                } else {
                    int divisor;
                    do {
                        divisor = random.nextInt(elements) + 1;
                    } while (elements % divisor != 0);

                    newRows = divisor;
                    newCols = elements / divisor;
                }

                final IntMatrix reshapedInt = intMatrix.reshapeAndPad(newRows, newCols);
                final Matrix<Integer> reshapedGeneric = genericMatrix.reshapeAndPad(newRows, newCols);

                if (elements == 0) {
                    assertEquals(reshapedInt.rowCount(), reshapedGeneric.rowCount());
                    assertEquals(reshapedInt.columnCount(), reshapedGeneric.columnCount());
                } else {
                    assertMatrixEquals(reshapedInt, reshapedGeneric);
                }
            }
        }
    }

    @Nested
    class IsEmptyTests {

        @Test
        public void testIsEmpty() {
            Matrix<String> empty = Matrix.wrap(new String[0][0]);
            assertTrue(empty.isEmpty());
            Matrix<String> notEmpty = Matrix.wrap(new String[][] { { "A" } });
            assertFalse(notEmpty.isEmpty());
        }
    }

    @Nested
    class IsSameShapeTests {

        @Test
        public void testIsSameShape() {
            Matrix<String> m1 = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Matrix<String> m2 = Matrix.wrap(new String[][] { { "X", "Y" }, { "Z", "W" } });
            Matrix<String> m3 = Matrix.wrap(new String[][] { { "A", "B", "C" } });
            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }
    }

    @Nested
    class ReshapeTests {

        @Test
        public void testReshape() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            Matrix<Integer> reshaped = matrix.reshape(3, 2);
            Assertions.assertEquals(3, reshaped.rowCount());
            Assertions.assertEquals(2, reshaped.columnCount());
            Assertions.assertEquals(1, reshaped.get(0, 0));
            Assertions.assertEquals(2, reshaped.get(0, 1));
            Assertions.assertEquals(3, reshaped.get(1, 0));
            Assertions.assertEquals(4, reshaped.get(1, 1));

            // Test reshape with too-small dimensions throws exception
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.reshape(1, 4));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.reshape(2, 2));
        }

        @Test
        public void testReshape_Shape3By3() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            Matrix<String> reshaped = m.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            String[] expected = { "A", "B", "C", "D", "E", "F", "G", "H", "I" };
            for (int i = 0; i < 9; i++) {
                assertEquals(expected[i], reshaped.get(0, i));
            }
        }

        @Test
        public void testReshape_Back() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" }, { "G", "H", "I" } });
            Matrix<String> reshaped = m.reshape(1, 9);
            Matrix<String> reshapedBack = reshaped.reshape(3, 3);
            assertEquals(m, reshapedBack);
        }

        @Test
        public void testReshape_TwoParams_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Matrix<Integer> reshaped = m.reshape(3, 2);

            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(3, reshaped.get(1, 0));
            assertEquals(6, reshaped.get(2, 1));
        }

        @Test
        public void testReshape_TwoParams_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            Matrix<String> reshaped = m.reshape(2, 3);

            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals("A", reshaped.get(0, 0));
            assertEquals("F", reshaped.get(1, 2));
        }

        @Test
        public void testReshape_ChangesShape() {
            Integer[][] arr = { { 1, 2, 3, 4, 5, 6 } };
            Matrix<Integer> m = new Matrix<>(arr);
            Matrix<Integer> reshaped = m.reshape(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(6, reshaped.get(1, 2));
        }

        @Test
        public void testRepresentableZeroRowNonZeroColumnShape() {
            Matrix<Integer> matrix = Matrix.empty(Integer.class).reshape(0, 3);

            assertEquals(0, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertEquals(3, matrix.copyRows(0, 0).columnCount());
            assertEquals(2, matrix.copyRegion(0, 0, 1, 3).columnCount());
        }

        @Test
        public void testReshape_SingleRow() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C", "D" } });
            Matrix<String> reshaped = m.reshape(2, 2);
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals("A", reshaped.get(0, 0));
            assertEquals("B", reshaped.get(0, 1));
            assertEquals("C", reshaped.get(1, 0));
            assertEquals("D", reshaped.get(1, 1));
        }
    }

    @Nested
    class ReshapeAndPadToColumnCountTests {

        @Test
        public void testReshapeAndPadToColumnCount_Integers() {
            Matrix<Integer> m = Matrix.wrap(new Integer[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Matrix<Integer> reshaped = m.reshapeAndPadToColumnCount(2);

            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(3, reshaped.get(1, 0));
            assertEquals(6, reshaped.get(2, 1));
        }

        @Test
        public void testReshapeAndPadToColumnCount_Strings() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            Matrix<String> reshaped = m.reshapeAndPadToColumnCount(3);

            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals("A", reshaped.get(0, 0));
            assertEquals("C", reshaped.get(0, 2));
            assertEquals("F", reshaped.get(1, 2));
        }
    }

    @Nested
    class ForEachIndicesTests {

        @Test
        public void testForEach_BiConsumer() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            final int[] count = { 0 };
            m.forEachIndices((i, j) -> count[0]++);
            assertEquals(4, count[0]);
        }

        @Test
        public void testForEach_BiObjConsumer() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            final int[] count = { 0 };
            m.forEachIndices((i, j, matrix) -> count[0]++);
            assertEquals(4, count[0]);
        }
    }

    @Nested
    class MainDiagonalPointsTests {

        @Test
        public void testMainDiagonalPoints() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Stream<Point> points = m.mainDiagonalPoints();
            assertEquals(2, points.count());
        }
    }

    @Nested
    class AntiDiagonalPointsTests {

        @Test
        public void testAntiDiagonalPoints() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Stream<Point> points = m.antiDiagonalPoints();
            assertEquals(2, points.count());
        }
    }

    @Nested
    class RowMajorPointsTests {

        @Test
        public void testRowMajorPoints() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Stream<Point> points = m.rowMajorPoints();
            assertEquals(4, points.count());
        }

        @Test
        public void testRowMajorPoints_WithRow() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Stream<Point> points = m.rowMajorPoints(1, 1 + 1);
            assertEquals(2, points.count());
        }

        @Test
        public void testRowMajorPoints_WithRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" }, { "E", "F" } });
            Stream<Point> points = m.rowMajorPoints(1, 3);
            assertEquals(4, points.count());
        }
    }

    @Nested
    class ColumnMajorPointsTests {

        @Test
        public void testColumnMajorPoints() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Stream<Point> points = m.columnMajorPoints();
            assertEquals(4, points.count());
        }

        @Test
        public void testColumnMajorPoints_WithColumn() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Stream<Point> points = m.columnMajorPoints(1, 1 + 1);
            assertEquals(2, points.count());
        }

        @Test
        public void testColumnMajorPoints_WithRange() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B", "C" }, { "D", "E", "F" } });
            Stream<Point> points = m.columnMajorPoints(1, 3);
            assertEquals(4, points.count());
        }
    }

    @Nested
    class RowPointsTests {

        @Test
        public void testRowPoints() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Stream<Stream<Point>> points = m.rowPoints();
            assertEquals(2, points.count());
        }
    }

    @Nested
    class ColumnPointsTests {

        @Test
        public void testColumnPoints() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            Stream<Stream<Point>> points = m.columnPoints();
            assertEquals(2, points.count());
        }
    }

    @Nested
    class AcceptTests {

        @Test
        public void testAccept() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            final boolean[] called = { false };
            m.accept(matrix -> called[0] = true);
            assertTrue(called[0]);
        }
    }

    @Nested
    class ApplyTests {

        @Test
        public void testApply() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            int result = m.apply(matrix -> matrix.rowCount() * matrix.columnCount());
            assertEquals(4, result);
        }
    }

    @Nested
    class PrintlnTests {

        @Test
        public void testPrintln() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            assertFalse(matrix.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> matrix.println());
        }
    }
}
