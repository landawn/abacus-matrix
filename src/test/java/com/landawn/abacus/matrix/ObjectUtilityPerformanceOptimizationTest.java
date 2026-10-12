package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.stream.IntIteratorEx;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

class ObjectUtilityPerformanceOptimizationTest extends TestBase {

    @Test
    void flattenIsGrowableAndOwnsSlotsWhileSharingElementObjects() {
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
    void datasetConversionsOwnMutableNamesAndColumns() {
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
    void transposedDatasetBulkCopiesLongAndCovariantRowsIntoIndependentColumns() {
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
    void datasetConversionsRetainHeaderValidationAndDegenerateShapes() {
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
    void exactStorageValidationKeepsMessagesAndValidationOrder() {
        final IllegalArgumentException outerFailure = assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(Number.class, new Integer[][] { { 1 } }));
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
    void storabilityAndStackElementTypeChecksKeepTheirMessages() {
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
    void allObjectCollectionZipFactoriesKeepShapeTypeAndIndependentRows() {
        final Throwables.ByteNFunction<String, RuntimeException> bytes = values -> "source";
        final Throwables.IntNFunction<String, RuntimeException> ints = values -> "source";
        final Throwables.LongNFunction<String, RuntimeException> longs = values -> "source";
        final Throwables.DoubleNFunction<String, RuntimeException> doubles = values -> "source";
        final Throwables.BinaryOperator<String, RuntimeException> fold = (left, right) -> left;
        final Throwables.Function<String[], String, RuntimeException> objects = values -> values[0];

        for (final int[] shape : new int[][] { { 2, 2 }, { 3, 0 }, { 0, 3 } }) {
            final int rows = shape[0];
            final int columns = shape[1];
            final Matrix<String> input = Matrix.empty(String.class).resize(rows, columns, "source");
            final List<Matrix<String>> results = List.of(Matrices.zipToObj(List.of(ByteMatrix.empty().resize(rows, columns)), bytes, true, String.class),
                    Matrices.zipToObj(List.of(IntMatrix.empty().resize(rows, columns)), ints, true, String.class),
                    Matrices.zipToObj(List.of(LongMatrix.empty().resize(rows, columns)), longs, true, String.class),
                    Matrices.zipToObj(List.of(DoubleMatrix.empty().resize(rows, columns)), doubles, true, String.class),
                    Matrices.zip(List.of(input), fold, String.class), Matrices.zip(List.of(input), objects, true, String.class, String.class));

            for (final Matrix<String> result : results) {
                assertEquals(rows, result.rowCount());
                assertEquals(columns, result.columnCount());
                assertSame(String.class, result.elementType());
                if (rows > 1) {
                    assertNotSame(result.rowView(0), result.rowView(1));
                }
                if (rows > 0 && columns > 0) {
                    result.set(0, 0, "result-only");
                    assertEquals("source", result.get(1, 0));
                    assertEquals("source", input.get(0, 0));
                }
            }
        }
    }

    @Test
    void boundedCopySnapshotsPreserveOverlappingSourceValuesAndNoOpValidation() {
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
    void emptyStreamRangesValidateIndexesAndPreserveLogicalRowColumnStreams() {
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
    void indexedStreamsBypassMappersForLongCountsAndDirectAdvancesInBothModes() {
        final int dimension = 65_536;
        final long total = (long) dimension * dimension;
        for (final boolean parallel : new boolean[] { false, true }) {
            final AtomicInteger calls = new AtomicInteger();
            final Throwables.IntBiFunction<Integer, RuntimeException> objectMapper = (i, j) -> {
                calls.incrementAndGet();
                return j;
            };
            final Throwables.IntBinaryOperator<RuntimeException> intMapper = (i, j) -> {
                calls.incrementAndGet();
                return j;
            };
            assertEquals(total, Matrices.mapIndices(dimension, dimension, objectMapper, parallel).count());
            assertEquals(total, Matrices.mapIndicesToInt(dimension, dimension, intMapper, parallel).count());
            assertEquals(0, calls.get());
            try (Stream<Integer> stream = Matrices.mapIndices(dimension, dimension, objectMapper, parallel)) {
                final ObjIteratorEx<Integer> iterator = (ObjIteratorEx<Integer>) stream.iterator();
                iterator.advance(total - 2);
                assertEquals(0, calls.get());
                assertEquals(dimension - 2, iterator.next());
                assertEquals(dimension - 1, iterator.next());
                assertFalse(iterator.hasNext());
            }
            assertEquals(2, calls.get());
            try (IntStream stream = Matrices.mapIndicesToInt(dimension, dimension, intMapper, parallel)) {
                final IntIteratorEx iterator = (IntIteratorEx) stream.iterator();
                iterator.advance(total - 2);
                assertEquals(2, calls.get());
                assertEquals(dimension - 2, iterator.nextInt());
                assertEquals(dimension - 1, iterator.nextInt());
                assertFalse(iterator.hasNext());
            }
            assertEquals(4, calls.get());
            assertEquals(List.of("2,5", "3,3", "3,4", "3,5"), Matrices.mapIndices(2, 4, 3, 6, (i, j) -> i + "," + j, parallel).skip(2).toList());
        }
    }

    @Test
    void indexedStreamCursorsRemainLazyAndExceptionsStayDeferred() {
        for (final boolean parallel : new boolean[] { false, true }) {
            final AtomicInteger calls = new AtomicInteger();
            try (Stream<Integer> stream = Matrices.mapIndices(2, 2, (i, j) -> {
                calls.incrementAndGet();
                return i * 2 + j;
            }, parallel)) {
                final var iterator = stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals(0, calls.get());
                assertEquals(0, iterator.next());
                assertEquals(3, iterator.count());
                assertEquals(1, calls.get());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::next);
            }
            final IOException checked = new IOException("deferred");
            final RuntimeException runtime = new IllegalStateException("same-instance");
            try (Stream<Integer> stream = Matrices.mapIndices(1, 1, (Throwables.IntBiFunction<Integer, IOException>) (i, j) -> {
                throw checked;
            }, parallel)) {
                assertSame(checked, assertThrows(RuntimeException.class, stream::toList).getCause());
            }
            try (IntStream stream = Matrices.mapIndicesToInt(1, 1, (i, j) -> {
                throw runtime;
            }, parallel)) {
                assertSame(runtime, assertThrows(RuntimeException.class, stream::toArray));
            }
        }
    }
}
