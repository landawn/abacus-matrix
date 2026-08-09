package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.IntStream;

class IntMatrixTest extends TestBase {

    private IntMatrix matrix;
    private IntMatrix emptyMatrix;

    @BeforeEach
    public void setUp() {
        matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
        emptyMatrix = IntMatrix.empty();
    }

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
    public void testCreateFromCharArray() {
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
    public void testCreateFromByteArray() {
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
    public void testCreateFromShortArray() {
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
    public void testComponentType() {
        assertEquals(int.class, matrix.elementType());
    }

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

    @Test
    public void testUpOf() {
        OptionalInt up = matrix.valueAbove(1, 1);
        assertTrue(up.isPresent());
        assertEquals(2, up.get());

        // Test top row
        OptionalInt empty = matrix.valueAbove(0, 1);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testDownOf() {
        OptionalInt down = matrix.valueBelow(1, 1);
        assertTrue(down.isPresent());
        assertEquals(8, down.get());

        // Test bottom row
        OptionalInt empty = matrix.valueBelow(2, 1);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testLeftOf() {
        OptionalInt left = matrix.valueLeft(1, 1);
        assertTrue(left.isPresent());
        assertEquals(4, left.get());

        // Test leftmost column
        OptionalInt empty = matrix.valueLeft(1, 0);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testRightOf() {
        OptionalInt right = matrix.valueRight(1, 1);
        assertTrue(right.isPresent());
        assertEquals(6, right.get());

        // Test rightmost column
        OptionalInt empty = matrix.valueRight(1, 2);
        assertFalse(empty.isPresent());
    }

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

    // Verify rowCopy returns a defensive row snapshot and enforces row bounds.
    @Test
    public void testRowCopy() {
        int[] rowCopy = matrix.rowCopy(0);
        assertArrayEquals(new int[] { 1, 2, 3 }, rowCopy);

        rowCopy[0] = 99;
        assertArrayEquals(new int[] { 1, 2, 3 }, matrix.rowView(0));
    }

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
    public void testSetRow() {
        IntMatrix m = matrix.copy();
        m.setRow(0, new int[] { 10, 20, 30 });
        assertArrayEquals(new int[] { 10, 20, 30 }, m.rowView(0));

        // Test wrong size
        assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new int[] { 1, 2 }));
        assertThrows(IllegalArgumentException.class, () -> m.setRow(0, (int[]) null));
    }

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
    public void testUpdateRow() {
        IntMatrix m = matrix.copy();
        m.updateRow(0, x -> x * 2);
        assertArrayEquals(new int[] { 2, 4, 6 }, m.rowView(0));
    }

    @Test
    public void testUpdateColumn() {
        IntMatrix m = matrix.copy();
        m.updateColumn(0, x -> x + 10);
        assertArrayEquals(new int[] { 11, 14, 17 }, m.columnCopy(0));
    }

    @Test
    public void testUpdateRowAndUpdateColumnInvalidIndex() {
        IntMatrix m = matrix.copy();

        assertThrows(IndexOutOfBoundsException.class, () -> m.updateRow(-1, x -> x * 2));
        assertThrows(IndexOutOfBoundsException.class, () -> m.updateColumn(3, x -> x + 10));
    }

    @Test
    public void testUpdateRowAndUpdateColumnNullOperator() {
        IntMatrix m = matrix.copy();

        assertThrows(IllegalArgumentException.class, () -> m.updateRow(0, (Throwables.IntUnaryOperator<RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> m.updateColumn(0, (Throwables.IntUnaryOperator<RuntimeException>) null));
    }

    @Test
    public void testGetLU2RD() {
        int[] diagonal = matrix.mainDiagonalCopy();
        assertArrayEquals(new int[] { 1, 5, 9 }, diagonal);

        // Test non-square matrix
        IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
        assertThrows(IllegalStateException.class, () -> nonSquare.mainDiagonalCopy());
    }

    @Test
    public void testSetLU2RD() {
        IntMatrix m = matrix.copy();
        m.setMainDiagonal(new int[] { 10, 20, 30 });
        assertEquals(10, m.get(0, 0));
        assertEquals(20, m.get(1, 1));
        assertEquals(30, m.get(2, 2));

        // Test non-square matrix
        IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
        assertThrows(IllegalStateException.class, () -> nonSquare.setMainDiagonal(new int[] { 1 }));

        // Test array too short
        assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new int[] { 1, 2 }));
    }

    @Test
    public void testUpdateLU2RD() {
        IntMatrix m = matrix.copy();
        m.updateMainDiagonal(x -> x * 10);
        assertEquals(10, m.get(0, 0));
        assertEquals(50, m.get(1, 1));
        assertEquals(90, m.get(2, 2));

        // Test non-square matrix
        IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
        assertThrows(IllegalStateException.class, () -> nonSquare.updateMainDiagonal(x -> x * 2));
    }

    @Test
    public void testGetRU2LD() {
        int[] antiDiagonal = matrix.antiDiagonalCopy();
        assertArrayEquals(new int[] { 3, 5, 7 }, antiDiagonal);

        // Test non-square matrix
        IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
        assertThrows(IllegalStateException.class, () -> nonSquare.antiDiagonalCopy());
    }

    @Test
    public void testSetRU2LD() {
        IntMatrix m = matrix.copy();
        m.setAntiDiagonal(new int[] { 10, 20, 30 });
        assertEquals(10, m.get(0, 2));
        assertEquals(20, m.get(1, 1));
        assertEquals(30, m.get(2, 0));

        // Test non-square matrix
        IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
        assertThrows(IllegalStateException.class, () -> nonSquare.setAntiDiagonal(new int[] { 1 }));
    }

    @Test
    public void testUpdateRU2LD() {
        IntMatrix m = matrix.copy();
        m.updateAntiDiagonal(x -> x * 10);
        assertEquals(30, m.get(0, 2));
        assertEquals(50, m.get(1, 1));
        assertEquals(70, m.get(2, 0));

        // Test non-square matrix
        IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
        assertThrows(IllegalStateException.class, () -> nonSquare.updateAntiDiagonal(x -> x * 2));
    }

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
    public void testUpdateAllNullOperator() {
        IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
        IntMatrix emptyLike = IntMatrix.wrap(new int[][] { {}, {} });

        assertThrows(IllegalArgumentException.class, () -> m.updateAll((Throwables.IntUnaryOperator<RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> m.updateAll((Throwables.IntBiFunction<Integer, RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAll((Throwables.IntUnaryOperator<RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAll((Throwables.IntBiFunction<Integer, RuntimeException>) null));
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
    public void testMap() {
        IntMatrix result = matrix.map(x -> x * 2);
        assertEquals(2, result.get(0, 0));
        assertEquals(4, result.get(0, 1));
        assertEquals(18, result.get(2, 2));

        // Original should be unchanged
        assertEquals(1, matrix.get(0, 0));
    }

    @Test
    public void testMapNullMapper() {
        IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

        assertThrows(IllegalArgumentException.class, () -> matrix.map((Throwables.IntUnaryOperator<RuntimeException>) null));
        assertThrows(IllegalArgumentException.class, () -> matrix.mapToObj((Throwables.IntFunction<String, RuntimeException>) null, String.class));
    }

    @Test
    public void testMapToLong() {
        LongMatrix result = matrix.mapToLong(x -> (long) x * 1000000);
        assertEquals(1000000L, result.get(0, 0));
        assertEquals(5000000L, result.get(1, 1));
    }

    @Test
    public void testMapToDouble() {
        DoubleMatrix result = matrix.mapToDouble(x -> x * 0.1);
        assertEquals(0.1, result.get(0, 0), 0.0001);
        assertEquals(0.5, result.get(1, 1), 0.0001);
    }

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
    public void testCopyRowsEmptyRange_returnsEmptyMatrix() {
        // Regression: copyRows(from, from) on a matrix with columns > 0 must not throw.
        IntMatrix empty = matrix.copyRows(0, 0);
        assertEquals(0, empty.rowCount());
        assertEquals(0, empty.columnCount());

        IntMatrix emptyAtEnd = matrix.copyRows(matrix.rowCount(), matrix.rowCount());
        assertEquals(0, emptyAtEnd.rowCount());
        assertEquals(0, emptyAtEnd.columnCount());
    }

    @Test
    public void testCopyRegionEmpty_returnsEmptyMatrix() {
        // Regression: copy with an empty range in either dimension must not throw.
        IntMatrix emptyRows = matrix.copyRegion(1, 1, 0, 3);
        assertEquals(0, emptyRows.rowCount());
        assertEquals(0, emptyRows.columnCount());

        IntMatrix emptyCols = matrix.copyRegion(0, 3, 1, 1);
        assertEquals(3, emptyCols.rowCount());
        assertEquals(0, emptyCols.columnCount());

        IntMatrix bothEmpty = matrix.copyRegion(0, 0, 0, 0);
        assertEquals(0, bothEmpty.rowCount());
        assertEquals(0, bothEmpty.columnCount());
    }

    @Test
    public void testPad() {
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
    public void testPadWithDefaultValue() {
        IntMatrix extended = matrix.resize(4, 4, -1);
        assertEquals(4, extended.rowCount());
        assertEquals(4, extended.columnCount());
        assertEquals(1, extended.get(0, 0));
        assertEquals(-1, extended.get(3, 3)); // new cell

        // Test negative dimensions
        assertThrows(IllegalArgumentException.class, () -> matrix.resize(-1, 3, 0));
        assertThrows(IllegalArgumentException.class, () -> matrix.resize(3, -1, 0));
    }

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
    public void testFlipH() {
        IntMatrix flipped = matrix.flipHorizontally();
        assertEquals(3, flipped.get(0, 0));
        assertEquals(2, flipped.get(0, 1));
        assertEquals(1, flipped.get(0, 2));

        // Original should be unchanged
        assertEquals(1, matrix.get(0, 0));
    }

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
    public void testFlatten() {
        IntList flat = matrix.flatten();
        assertEquals(9, flat.size());
        for (int i = 0; i < 9; i++) {
            assertEquals(i + 1, flat.get(i));
        }
    }

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
    public void testBoxed() {
        Matrix<Integer> boxed = matrix.boxed();
        assertEquals(3, boxed.rowCount());
        assertEquals(3, boxed.columnCount());
        assertEquals(Integer.valueOf(1), boxed.get(0, 0));
        assertEquals(Integer.valueOf(5), boxed.get(1, 1));
    }

    @Test
    public void testToLongMatrix() {
        LongMatrix longMatrix = matrix.toLongMatrix();
        assertEquals(3, longMatrix.rowCount());
        assertEquals(3, longMatrix.columnCount());
        assertEquals(1L, longMatrix.get(0, 0));
        assertEquals(5L, longMatrix.get(1, 1));
    }

    @Test
    public void testToFloatMatrix() {
        FloatMatrix floatMatrix = matrix.toFloatMatrix();
        assertEquals(3, floatMatrix.rowCount());
        assertEquals(3, floatMatrix.columnCount());
        assertEquals(1.0f, floatMatrix.get(0, 0), 0.0001f);
        assertEquals(5.0f, floatMatrix.get(1, 1), 0.0001f);
    }

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
    public void testStreamLU2RD() {
        int[] diagonal = matrix.mainDiagonalStream().toArray();
        assertArrayEquals(new int[] { 1, 5, 9 }, diagonal);

        // Test empty matrix
        assertTrue(emptyMatrix.mainDiagonalStream().toArray().length == 0);

        // Test non-square
        IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
        assertThrows(IllegalStateException.class, () -> nonSquare.mainDiagonalStream());
    }

    @Test
    public void testStreamRU2LD() {
        int[] antiDiagonal = matrix.antiDiagonalStream().toArray();
        assertArrayEquals(new int[] { 3, 5, 7 }, antiDiagonal);

        // Test empty matrix
        assertTrue(emptyMatrix.antiDiagonalStream().toArray().length == 0);

        // Test non-square
        IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
        assertThrows(IllegalStateException.class, () -> nonSquare.antiDiagonalStream());
    }

    @Test
    public void testStreamH() {
        int[] all = matrix.rowMajorStream().toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, all);

        // Test empty matrix
        assertTrue(emptyMatrix.rowMajorStream().toArray().length == 0);
    }

    @Test
    public void testStreamHSingleRowRange() {
        int[] row1 = matrix.rowMajorStream(1, 2).toArray();
        assertArrayEquals(new int[] { 4, 5, 6 }, row1);

        // Test bounds
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(3, 4));
    }

    @Test
    public void testStreamHRange() {
        int[] rows = matrix.rowMajorStream(1, 3).toArray();
        assertArrayEquals(new int[] { 4, 5, 6, 7, 8, 9 }, rows);

        // Test bounds
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(2, 1));
    }

    @Test
    public void testStreamV() {
        int[] all = matrix.columnMajorStream().toArray();
        assertArrayEquals(new int[] { 1, 4, 7, 2, 5, 8, 3, 6, 9 }, all);

        // Test empty matrix
        assertTrue(emptyMatrix.columnMajorStream().toArray().length == 0);
    }

    @Test
    public void testStreamVSingleColumnRange() {
        int[] col1 = matrix.columnMajorStream(1, 2).toArray();
        assertArrayEquals(new int[] { 2, 5, 8 }, col1);

        // Test bounds
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(3, 4));
    }

    @Test
    public void testStreamVRange() {
        int[] columnCount = matrix.columnMajorStream(1, 3).toArray();
        assertArrayEquals(new int[] { 2, 5, 8, 3, 6, 9 }, columnCount);

        // Test bounds
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(2, 1));
    }

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
    public void testStreamRWithZeroColumnRows() {
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
    public void testStreamRRange() {
        List<int[]> rows = matrix.rowStreams(1, 3).map(IntStream::toArray).toList();
        assertEquals(2, rows.size());
        assertArrayEquals(new int[] { 4, 5, 6 }, rows.get(0));
        assertArrayEquals(new int[] { 7, 8, 9 }, rows.get(1));

        // Test bounds
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(0, 4));
    }

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
    public void testStreamCRange() {
        List<int[]> columnCount = matrix.columnStreams(1, 3).map(IntStream::toArray).toList();
        assertEquals(2, columnCount.size());
        assertArrayEquals(new int[] { 2, 5, 8 }, columnCount.get(0));
        assertArrayEquals(new int[] { 3, 6, 9 }, columnCount.get(1));

        // Test bounds
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(0, 4));
    }

    @Test
    public void testLength() {
        // This is a protected method, test indirectly through row operations
        int[] row = matrix.rowView(0);
        assertEquals(3, row.length);
    }

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
    public void testForEachNullAction() {
        assertThrows(IllegalArgumentException.class, () -> matrix.forEach((Throwables.IntConsumer<RuntimeException>) null));
        assertThrows(IllegalArgumentException.class,
                () -> matrix.forEach(0, matrix.rowCount(), 0, matrix.columnCount(), (Throwables.IntConsumer<RuntimeException>) null));
    }

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
    public void testRejectUnrepresentableZeroRowNonZeroColumnShape() {
        IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

        // assertThrows(IllegalArgumentException.class, () -> matrix.copyRows(0, 0));
        // assertThrows(IllegalArgumentException.class, () -> matrix.copyRegion(0, 0, 0, 1));
        // assertThrows(IllegalArgumentException.class, () -> matrix.pad(0, 1));
        assertThrows(IllegalArgumentException.class, () -> matrix.reshape(0, 1));
    }

    @Nested
    @Tag("2025")
    class IntMatrix2025Test extends TestBase {
        // ============ Factory Method Tests ============

        @Test
        public void testEmpty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.rowCount());
            assertEquals(0, empty.columnCount());
            assertTrue(empty.isEmpty());

            // Test singleton
            assertSame(IntMatrix.empty(), IntMatrix.empty());
        }

        @Test
        public void testCreateFromCharArray_withNull() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from((char[][]) null));
        }

        @Test
        public void testCreateFromCharArray_withEmpty() {
            IntMatrix m = IntMatrix.from(new char[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testCreateFromCharArray_withJaggedArray() {
            char[][] jagged = { { 'A', 'B' }, { 'C' } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(jagged));
        }

        @Test
        public void testCreateFromCharArray_withNullRow() {
            char[][] nullRow = { { 'A', 'B' }, null };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(nullRow));
        }

        @Test
        public void testCreateFromCharArray_withNullFirstRow() {
            char[][] nullFirstRow = { null, { 'A', 'B' } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(nullFirstRow));
        }

        @Test
        public void testCreateFromByteArray_withNull() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from((byte[][]) null));
        }

        @Test
        public void testCreateFromByteArray_withJaggedArray() {
            byte[][] jagged = { { 1, 2 }, { 3 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(jagged));
        }

        @Test
        public void testCreateFromByteArray_withNullRow() {
            byte[][] nullRow = { { 1, 2 }, null };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(nullRow));
        }

        @Test
        public void testCreateFromShortArray_withNull() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from((short[][]) null));
        }

        @Test
        public void testCreateFromShortArray_withJaggedArray() {
            short[][] jagged = { { 1, 2 }, { 3 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(jagged));
        }

        @Test
        public void testCreateFromShortArray_withNullRow() {
            short[][] nullRow = { { 1, 2 }, null };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(nullRow));
        }

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
        public void testRandom_withRowsCols() {
            IntMatrix m = IntMatrix.random(2, 3);
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    assertNotNull(m.get(i, j));
                }
            }
        }

        @Test
        public void testRange_withNegativeStep() {
            IntMatrix m = IntMatrix.range(10, 0, -2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new int[] { 10, 8, 6, 4, 2 }, m.rowView(0));
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
        public void testDiagonal_withBothDiagonals() {
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
        public void testDiagonal_withOnlyMainDiagonal() {
            IntMatrix m = IntMatrix.ofDiagonals(new int[] { 1, 2, 3 }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(1, 1));
            assertEquals(3, m.get(2, 2));
        }

        @Test
        public void testDiagonal_withOnlyAntiDiagonal() {
            IntMatrix m = IntMatrix.ofDiagonals(null, new int[] { 4, 5, 6 });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(4, m.get(0, 2));
            assertEquals(5, m.get(1, 1));
            assertEquals(6, m.get(2, 0));
        }

        @Test
        public void testDiagonal_withBothNull() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofDiagonals(null, null));
        }

        @Test
        public void testDiagonal_withDifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofDiagonals(new int[] { 1, 2 }, new int[] { 3, 4, 5 }));
        }

        @Test
        public void testUnbox() {
            Integer[][] boxed = { { 1, 2 }, { 3, 4 } };
            Matrix<Integer> boxedMatrix = Matrix.wrap(boxed);
            IntMatrix unboxed = IntMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertEquals(1, unboxed.get(0, 0));
            assertEquals(4, unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_withNullValues() {
            Integer[][] boxed = { { 1, null }, { null, 4 } };
            Matrix<Integer> boxedMatrix = Matrix.wrap(boxed);
            IntMatrix unboxed = IntMatrix.unbox(boxedMatrix);
            assertEquals(1, unboxed.get(0, 0));
            assertEquals(0, unboxed.get(0, 1)); // null -> 0
            assertEquals(0, unboxed.get(1, 0)); // null -> 0
            assertEquals(4, unboxed.get(1, 1));
        }

        // ============ Component Type Tests ============

        @Test
        public void testComponentType() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 } });
            assertEquals(int.class, m.elementType());
        }

        // ============ Get/Set Tests ============

        @Test
        public void testGet() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(5, m.get(1, 1));
            assertEquals(6, m.get(1, 2));
        }

        @Test
        public void testGet_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(-1, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(2, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(0, -1));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.get(0, 2));
        }

        @Test
        public void testGetWithPoint() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(1, m.get(Point.of(0, 0)));
            assertEquals(4, m.get(Point.of(1, 1)));
            assertEquals(2, m.get(Point.of(0, 1)));
        }

        @Test
        public void testSet() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.set(0, 0, 10);
            assertEquals(10, m.get(0, 0));

            m.set(1, 1, 20);
            assertEquals(20, m.get(1, 1));
        }

        @Test
        public void testSet_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, 0));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(2, 0, 0));
        }

        @Test
        public void testSetWithPoint() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.set(Point.of(0, 0), 50);
            assertEquals(50, m.get(Point.of(0, 0)));
        }

        // ============ Adjacent Element Tests ============

        @Test
        public void testUpOf() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            OptionalInt up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1, up.get());

            // Top row has no element above
            OptionalInt empty = m.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testDownOf() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            OptionalInt down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3, down.get());

            // Bottom row has no element below
            OptionalInt empty = m.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testLeftOf() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            OptionalInt left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1, left.get());

            // Leftmost column has no element to the left
            OptionalInt empty = m.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testRightOf() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            OptionalInt right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2, right.get());

            // Rightmost column has no element to the right
            OptionalInt empty = m.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        // ============ Row/Column Operations Tests ============

        @Test
        public void testRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new int[] { 1, 2, 3 }, m.rowView(0));
            assertArrayEquals(new int[] { 4, 5, 6 }, m.rowView(1));
        }

        @Test
        public void testRow_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(2));
        }

        @Test
        public void testColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new int[] { 1, 4 }, m.columnCopy(0));
            assertArrayEquals(new int[] { 2, 5 }, m.columnCopy(1));
            assertArrayEquals(new int[] { 3, 6 }, m.columnCopy(2));
        }

        @Test
        public void testColumn_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testSetRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setRow(0, new int[] { 10, 20 });
            assertArrayEquals(new int[] { 10, 20 }, m.rowView(0));
            assertArrayEquals(new int[] { 3, 4 }, m.rowView(1)); // unchanged
        }

        @Test
        public void testSetRow_wrongSize() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new int[] { 1 }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new int[] { 1, 2, 3 }));
        }

        @Test
        public void testSetColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setColumn(0, new int[] { 10, 20 });
            assertArrayEquals(new int[] { 10, 20 }, m.columnCopy(0));
            assertArrayEquals(new int[] { 2, 4 }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testSetColumn_wrongSize() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new int[] { 1 }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new int[] { 1, 2, 3 }));
        }

        @Test
        public void testUpdateColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateColumn(0, x -> x + 10);
            assertArrayEquals(new int[] { 11, 13 }, m.columnCopy(0));
            assertArrayEquals(new int[] { 2, 4 }, m.columnCopy(1)); // unchanged
        }

        // ============ Diagonal Operations Tests ============

        @Test
        public void testGetLU2RD() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertArrayEquals(new int[] { 1, 5, 9 }, m.mainDiagonalCopy());
        }

        @Test
        public void testGetLU2RD_nonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalStateException.class, () -> m.mainDiagonalCopy());
        }

        @Test
        public void testSetLU2RD() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setMainDiagonal(new int[] { 10, 20, 30 });
            assertEquals(10, m.get(0, 0));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 2));
        }

        @Test
        public void testSetLU2RD_nonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalStateException.class, () -> m.setMainDiagonal(new int[] { 1 }));
        }

        @Test
        public void testSetLU2RD_arrayTooShort() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new int[] { 1, 2 }));
        }

        @Test
        public void testUpdateLU2RD() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateMainDiagonal(x -> x * 10);
            assertEquals(10, m.get(0, 0));
            assertEquals(50, m.get(1, 1));
            assertEquals(90, m.get(2, 2));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateLU2RD_nonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalStateException.class, () -> m.updateMainDiagonal(x -> x * 2));
        }

        @Test
        public void testGetRU2LD() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertArrayEquals(new int[] { 3, 5, 7 }, m.antiDiagonalCopy());
        }

        @Test
        public void testGetRU2LD_nonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalStateException.class, () -> m.antiDiagonalCopy());
        }

        @Test
        public void testSetRU2LD() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setAntiDiagonal(new int[] { 10, 20, 30 });
            assertEquals(10, m.get(0, 2));
            assertEquals(20, m.get(1, 1));
            assertEquals(30, m.get(2, 0));
        }

        @Test
        public void testSetRU2LD_nonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalStateException.class, () -> m.setAntiDiagonal(new int[] { 1 }));
        }

        @Test
        public void testSetRU2LD_arrayTooShort() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new int[] { 1, 2 }));
        }

        @Test
        public void testUpdateRU2LD() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateAntiDiagonal(x -> x * 10);
            assertEquals(30, m.get(0, 2));
            assertEquals(50, m.get(1, 1));
            assertEquals(70, m.get(2, 0));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateRU2LD_nonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalStateException.class, () -> m.updateAntiDiagonal(x -> x * 2));
        }

        @Test
        public void testUpdateAll_withIndices() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 0, 0 }, { 0, 0 } });
            m.updateAll((i, j) -> i * 10 + j);
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
            assertEquals(10, m.get(1, 0));
            assertEquals(11, m.get(1, 1));
        }

        @Test
        public void testReplaceIf() {
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
        public void testReplaceIf_withIndices() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.replaceIf((i, j) -> i == j, 0); // Replace diagonal
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(1, 1));
            assertEquals(0, m.get(2, 2));
            assertEquals(2, m.get(0, 1)); // unchanged
        }

        @Test
        public void testMapToLong() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.mapToLong(x -> (long) x * 1000000);
            assertEquals(1000000L, result.get(0, 0));
            assertEquals(4000000L, result.get(1, 1));
        }

        @Test
        public void testMapToDouble() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.mapToDouble(x -> x * 0.1);
            assertEquals(0.1, result.get(0, 0), 0.0001);
            assertEquals(0.4, result.get(1, 1), 0.0001);
        }

        @Test
        public void testMapToObj() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> result = m.mapToObj(x -> "val:" + x, String.class);
            assertEquals("val:1", result.get(0, 0));
            assertEquals("val:4", result.get(1, 1));
        }

        // ============ Fill Tests ============

        @Test
        public void testFill_withValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.fill(99);
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals(99, m.get(i, j));
                }
            }
        }

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
        public void testCopyFrom_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[][] patch = { { 1, 2 }, { 3, 4 } };
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
        }

        // ============ Copy Tests ============

        @Test
        public void testCopy() {
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
        public void testCopyRows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix subset = m.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertEquals(4, subset.get(0, 0));
            assertEquals(9, subset.get(1, 2));
        }

        @Test
        public void testCopyRows_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(2, 1));
        }

        @Test
        public void testCopyRegion() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix submatrix = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals(2, submatrix.get(0, 0));
            assertEquals(6, submatrix.get(1, 1));
        }

        @Test
        public void testCopyRegion_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, 0, 3));
        }

        // ============ Extend Tests ============

        @Test
        public void testPad_larger() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix extended = m.resize(4, 4);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(4, extended.get(1, 1));
            assertEquals(0, extended.get(3, 3)); // new cells are 0
        }

        @Test
        public void testPad_smaller() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix truncated = m.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
            assertEquals(1, truncated.get(0, 0));
            assertEquals(5, truncated.get(1, 1));
        }

        @Test
        public void testPad_withDefaultValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix extended = m.resize(3, 3, -1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(-1, extended.get(2, 2)); // new cell
        }

        @Test
        public void testPad_withNegativeDimensions() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 3, 0));
            assertThrows(IllegalArgumentException.class, () -> m.resize(3, -1, 0));
        }

        @Test
        public void testPad_directional() {
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
        public void testPad_directionalWithDefault() {
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
        public void testPad_directionalWithNegativeValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.pad(-1, 1, 1, 1, 0));
        }

        @Test
        public void testReverseV() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertEquals(5, m.get(0, 0));
            assertEquals(6, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(1, m.get(2, 0));
        }
        // ============ Rotation Tests ============

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
        // ============ Transpose Tests ============

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
        public void testTranspose_square() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(3, transposed.get(0, 1));
            assertEquals(2, transposed.get(1, 0));
            assertEquals(4, transposed.get(1, 1));
        }

        // ============ Reshape Tests ============

        @Test
        public void testReshape() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix reshaped = m.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, reshaped.get(0, i));
            }
        }

        @Test
        public void testReshape_back() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix reshaped = m.reshape(1, 9);
            IntMatrix reshapedBack = reshaped.reshape(3, 3);
            assertEquals(m, reshapedBack);
        }

        @Test
        public void testReshape_empty() {
            IntMatrix empty = IntMatrix.empty();
            IntMatrix reshaped = empty.reshapeAndPad(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }

        // ============ Repeat Tests ============

        @Test
        public void testRepeatElements_invalidArguments() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @Test
        public void testRepeatMatrix_invalidArguments() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        // ============ Flatten Tests ============

        @Test
        public void testFlatten() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntList flat = m.flatten();
            assertEquals(9, flat.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, flat.get(i));
            }
        }

        @Test
        public void testFlatten_empty() {
            IntMatrix empty = IntMatrix.empty();
            IntList flat = empty.flatten();
            assertTrue(flat.isEmpty());
        }

        @Test
        public void testFlatOp() {
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

        // ============ Stack Tests ============

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
        }

        @Test
        public void testVstack_differentColumnCounts() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

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
        }

        @Test
        public void testHstack_differentRowCounts() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        // ============ Arithmetic Operations Tests ============

        @Test
        public void testAdd() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix sum = m1.add(m2);

            assertEquals(6, sum.get(0, 0));
            assertEquals(8, sum.get(0, 1));
            assertEquals(10, sum.get(1, 0));
            assertEquals(12, sum.get(1, 1));
        }

        @Test
        public void testAdd_differentDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testSubtract() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix diff = m1.subtract(m2);

            assertEquals(4, diff.get(0, 0));
            assertEquals(4, diff.get(0, 1));
            assertEquals(4, diff.get(1, 0));
            assertEquals(4, diff.get(1, 1));
        }

        @Test
        public void testSubtract_differentDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testMultiply_incompatibleDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testMultiply_rectangularMatrices() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } }); // 1x3
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 4 }, { 5 }, { 6 } }); // 3x1
            IntMatrix product = m1.matrixMultiply(m2);

            assertEquals(1, product.rowCount());
            assertEquals(1, product.columnCount());
            assertEquals(32, product.get(0, 0)); // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        }

        @Test
        public void testmatrixMultiply_emptyProductReturnsCanonicalEmpty() {
            // Regression: matrixMultiply must build its result via IntMatrix.wrap(result) (not the raw
            // constructor) so an empty product yields the shared EMPTY singleton, and must call
            // checkRepresentableShape before allocation for consistency with the other
            // result-allocating methods (resize/reshape/transpose/rotate).
            IntMatrix product = IntMatrix.empty().matrixMultiply(IntMatrix.empty());
            assertSame(IntMatrix.empty(), product);
            assertTrue(product.isEmpty());
        }

        // ============ Conversion Tests ============

        @Test
        public void testBoxed() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Matrix<Integer> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Integer.valueOf(1), boxed.get(0, 0));
            assertEquals(Integer.valueOf(6), boxed.get(1, 2));
        }

        @Test
        public void testToLongMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix longMatrix = m.toLongMatrix();
            assertEquals(2, longMatrix.rowCount());
            assertEquals(2, longMatrix.columnCount());
            assertEquals(1L, longMatrix.get(0, 0));
            assertEquals(4L, longMatrix.get(1, 1));
        }

        @Test
        public void testToFloatMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            FloatMatrix floatMatrix = m.toFloatMatrix();
            assertEquals(2, floatMatrix.rowCount());
            assertEquals(2, floatMatrix.columnCount());
            assertEquals(1.0f, floatMatrix.get(0, 0), 0.0001f);
            assertEquals(4.0f, floatMatrix.get(1, 1), 0.0001f);
        }

        @Test
        public void testToDoubleMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix doubleMatrix = m.toDoubleMatrix();
            assertEquals(2, doubleMatrix.rowCount());
            assertEquals(2, doubleMatrix.columnCount());
            assertEquals(1.0, doubleMatrix.get(0, 0), 0.0001);
            assertEquals(4.0, doubleMatrix.get(1, 1), 0.0001);
        }

        // ============ ZipWith Tests ============

        @Test
        public void testZipWith_differentShapes() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a + b));
        }

        @Test
        public void testZipWith_threeMatrices_differentShapes() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 9, 10, 11 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> a + b + c));
        }

        // ============ Stream Tests ============

        @Test
        public void testStreamLU2RD() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diagonal = m.mainDiagonalStream().toArray();
            assertArrayEquals(new int[] { 1, 5, 9 }, diagonal);
        }

        @Test
        public void testStreamLU2RD_empty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().toArray().length);
        }

        @Test
        public void testStreamLU2RD_nonSquare() {
            IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalStateException.class, () -> nonSquare.mainDiagonalStream());
        }

        @Test
        public void testStreamRU2LD() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] antiDiagonal = m.antiDiagonalStream().toArray();
            assertArrayEquals(new int[] { 3, 5, 7 }, antiDiagonal);
        }

        @Test
        public void testStreamRU2LD_empty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.antiDiagonalStream().toArray().length);
        }

        @Test
        public void testStreamRU2LD_nonSquare() {
            IntMatrix nonSquare = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalStateException.class, () -> nonSquare.antiDiagonalStream());
        }

        @Test
        public void testStreamH() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] all = m.rowMajorStream().toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, all);
        }

        @Test
        public void testStreamH_empty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.rowMajorStream().toArray().length);
        }

        @Test
        public void testStreamH_withSingleRowRange_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 3));
        }

        @Test
        public void testStreamH_withRange_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 1));
        }

        @Test
        public void testStreamV() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] all = m.columnMajorStream().toArray();
            assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 6 }, all);
        }

        @Test
        public void testStreamV_empty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.columnMajorStream().toArray().length);
        }

        @Test
        public void testStreamV_withSingleColumnRange_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 3));
        }

        @Test
        public void testStreamV_withRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] columnCount = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new int[] { 2, 5, 8, 3, 6, 9 }, columnCount);
        }

        @Test
        public void testStreamV_withRange_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 1));
        }

        @Test
        public void testStreamR() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<int[]> rows = m.rowStreams().map(IntStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new int[] { 1, 2, 3 }, rows.get(0));
            assertArrayEquals(new int[] { 4, 5, 6 }, rows.get(1));
        }

        @Test
        public void testStreamR_empty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.rowStreams().count());
        }

        @Test
        public void testStreamR_withRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<int[]> rows = m.rowStreams(1, 3).map(IntStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new int[] { 4, 5, 6 }, rows.get(0));
            assertArrayEquals(new int[] { 7, 8, 9 }, rows.get(1));
        }

        @Test
        public void testStreamR_withRange_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(0, 3));
        }

        @Test
        public void testStreamC() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<int[]> columnCount = m.columnStreams().map(IntStream::toArray).toList();
            assertEquals(3, columnCount.size());
            assertArrayEquals(new int[] { 1, 4 }, columnCount.get(0));
            assertArrayEquals(new int[] { 2, 5 }, columnCount.get(1));
            assertArrayEquals(new int[] { 3, 6 }, columnCount.get(2));
        }

        @Test
        public void testStreamC_empty() {
            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.columnStreams().count());
        }

        @Test
        public void testStreamC_withRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<int[]> columnCount = m.columnStreams(1, 3).map(IntStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new int[] { 2, 5, 8 }, columnCount.get(0));
            assertArrayEquals(new int[] { 3, 6, 9 }, columnCount.get(1));
        }

        @Test
        public void testStreamC_withRange_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(0, 3));
        }

        // ============ ForEach Tests ============

        @Test
        public void testForEach() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Integer> values = new ArrayList<>();
            m.forEach(v -> values.add(v));
            assertEquals(9, values.size());
            for (int i = 0; i < 9; i++) {
                assertEquals(i + 1, values.get(i).intValue());
            }
        }

        @Test
        public void testForEach_withBounds() {
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
        public void testForEach_withBounds_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(-1, 2, 0, 2, v -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEach(0, 3, 0, 2, v -> {
            }));
        }

        // ============ Object Methods Tests ============

        @Test
        public void testPrintln() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertFalse(m.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> m.println());

            IntMatrix empty = IntMatrix.empty();
            assertTrue(empty.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> empty.println());
        }

        @Test
        public void testHashCode() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 4, 3 } });

            assertEquals(m1.hashCode(), m2.hashCode());
            assertNotEquals(m1.hashCode(), m3.hashCode()); // Usually different
        }

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
        public void testToString() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1"));
            assertTrue(str.contains("2"));
            assertTrue(str.contains("3"));
            assertTrue(str.contains("4"));
        }

        // ============ Statistical Operations Tests ============

        @Test
        public void testStatisticalOperations() {
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

        // ============ Edge Case Tests ============

        @Test
        public void testEmptyMatrixOperations() {
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
        // ============ Integer Overflow Tests ============

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

        // ============ Additional Coverage Tests ============

        @Test
        public void testBoxed_withMoreRowsThanCols() {
            // Test the rows > columnCount branch in boxed()
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 }, { 7, 8 } });
            Matrix<Integer> boxed = m.boxed();
            assertEquals(4, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Integer.valueOf(1), boxed.get(0, 0));
            assertEquals(Integer.valueOf(8), boxed.get(3, 1));
        }

        @Test
        public void testBoxed_withEqualRowsAndCols() {
            // Test the rows <= columnCount branch where rows == columnCount
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            Matrix<Integer> boxed = m.boxed();
            assertEquals(3, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Integer.valueOf(5), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_empty() {
            // Test boxed() with empty matrix
            IntMatrix empty = IntMatrix.empty();
            Matrix<Integer> boxed = empty.boxed();
            assertEquals(0, boxed.rowCount());
            assertEquals(0, boxed.columnCount());
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

        @Test
        public void testMultiplyWithOverflow() {
            // Test multiplication that causes overflow
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { Integer.MAX_VALUE / 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 } });
            IntMatrix product = m1.matrixMultiply(m2);

            // Result will overflow - verify it wraps around
            assertTrue(product.get(0, 0) != (Integer.MAX_VALUE / 2) * 3L);
        }

        @Test
        public void testFlatOpWithMultipleRows() {
            // Test mutateViaFlatArray to ensure it processes the flattened array correctly
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<Integer> maxValues = new ArrayList<>();

            m.mutateViaFlatArray(flatArray -> {
                int max = Integer.MIN_VALUE;
                for (int val : flatArray) {
                    if (val > max)
                        max = val;
                }
                maxValues.add(max);
            });

            // mutateViaFlatArray flattens all rows into one array, so there's only 1 result
            assertEquals(1, maxValues.size());
            assertEquals(6, maxValues.get(0).intValue()); // max of [1,2,3,4,5,6] is 6
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
    }

    @Nested
    @Tag("2510")
    class IntMatrix2510Test extends TestBase {
        @Test
        public void testCreate_fromShortArray_differentRowLengths() {
            short[][] shorts = { { 1, 2 }, { 3 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(shorts));
        }

        // ============ Flatten Tests ============

        @Test
        public void testFlatten() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntList flat = m.flatten();
            assertEquals(6, flat.size());
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, flat.toArray());
        }

        // ============ FlatOp Tests ============

        @Test
        public void testFlatOp() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            final int[] count = { 0 };
            m.mutateViaFlatArray(row -> count[0] += row.length);
            assertEquals(4, count[0]);
        }

        // ============ Stack Tests ============

        @Test
        public void testVstack() {
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
        public void testVstack_differentColumnCount() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4, 5 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testHstack() {
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
        public void testHstack_differentRowCount() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testAdd_differentDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testSubtract_differentDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testMultiply() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 2, 3 }, { 4, 5 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m1.matrixMultiply(m2);
            assertEquals(11, result.get(0, 0));
            assertEquals(16, result.get(0, 1));
            assertEquals(19, result.get(1, 0));
            assertEquals(28, result.get(1, 1));
        }

        // ============ Conversion Tests ============

        @Test
        public void testBoxed() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> boxed = m.boxed();
            assertEquals(Integer.valueOf(1), boxed.get(0, 0));
            assertEquals(Integer.valueOf(4), boxed.get(1, 1));
        }

        @Test
        public void testToLongMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.toLongMatrix();
            assertEquals(1L, result.get(0, 0));
            assertEquals(4L, result.get(1, 1));
        }

        @Test
        public void testToFloatMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            FloatMatrix result = m.toFloatMatrix();
            assertEquals(1.0f, result.get(0, 0), 0.001f);
            assertEquals(4.0f, result.get(1, 1), 0.001f);
        }

        @Test
        public void testToDoubleMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.toDoubleMatrix();
            assertEquals(1.0, result.get(0, 0), 0.001);
            assertEquals(4.0, result.get(1, 1), 0.001);
        }

        // ============ ZipWith Tests ============

        @Test
        public void testZipWith_twoMatrices() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix result = m1.zipWith(m2, (a, b) -> a + b);
            assertEquals(6, result.get(0, 0));
            assertEquals(8, result.get(0, 1));
            assertEquals(10, result.get(1, 0));
            assertEquals(12, result.get(1, 1));
        }

        @Test
        public void testZipWith_twoMatrices_differentDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a + b));
        }

        @Test
        public void testZipWith_threeMatrices() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 9, 10 }, { 11, 12 } });
            IntMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(15, result.get(0, 0)); // 1 + 5 + 9
            assertEquals(18, result.get(0, 1)); // 2 + 6 + 10
            assertEquals(21, result.get(1, 0)); // 3 + 7 + 11
            assertEquals(24, result.get(1, 1)); // 4 + 8 + 12
        }

        @Test
        public void testZipWith_threeMatrices_differentDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> a + b + c));
        }

        @Test
        public void testStreamLU2RD_nonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalStateException.class, () -> m.mainDiagonalStream().toArray());
        }

        @Test
        public void testStreamRU2LD() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diagonal = m.antiDiagonalStream().toArray();
            assertArrayEquals(new int[] { 3, 5, 7 }, diagonal);
        }

        @Test
        public void testStreamRU2LD_nonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalStateException.class, () -> m.antiDiagonalStream().toArray());
        }

        @Test
        public void testStreamH_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 2));
        }

        @Test
        public void testStreamV_outOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
        }

        // ============ Stream of Streams Tests ============

        @Test
        public void testStreamR() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<int[]> rows = m.rowStreams().map(IntStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new int[] { 1, 2 }, rows.get(0));
            assertArrayEquals(new int[] { 3, 4 }, rows.get(1));
        }

        @Test
        public void testStreamC() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<int[]> columnCount = m.columnStreams().map(IntStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new int[] { 1, 3 }, columnCount.get(0));
            assertArrayEquals(new int[] { 2, 4 }, columnCount.get(1));
        }

        // ============ Points Stream Tests ============

        // ============ Sum Tests ============
        // Note: sumByInt(), sumByLong(), sumByDouble() methods don't exist in IntMatrix

        // ============ Average Tests ============
        // Note: averageByInt(), averageByLong(), averageByDouble() methods don't exist in IntMatrix

        // ============ Empty Matrix Tests ============

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

        // ============ Equals and HashCode Tests ============

        @Test
        public void testEquals() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 5 } });

            assertEquals(m1, m2);
            assertNotEquals(m1, m3);
        }

        @Test
        public void testHashCode_equal() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            assertEquals(m1.hashCode(), m2.hashCode());
        }

        // ============ ToString Tests ============

        @Test
        public void testToString() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.length() > 0);
        }
    }

    @Nested
    @Tag("2511")
    class IntMatrix2511Test extends TestBase {
        @Test
        public void testConstructor_withLargeMatrix() {
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
        public void testWrap_withSingleColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 }, { 2 }, { 3 }, { 4 } });
            assertEquals(4, m.rowCount());
            assertEquals(1, m.columnCount());
        }

        public void testRandom() {
            IntMatrix m = IntMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertNotNull(m.rowView(0));
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
        }

        @Test
        public void testDiagonal_bothDiagonals() {
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
        public void testDiagonal_differentLengths() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofDiagonals(new int[] { 1, 2 }, new int[] { 1, 2, 3 }));
        }

        @Test
        public void testGet_withPoint() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Point p = Point.of(1, 0);
            assertEquals(3, m.get(p));
        }

        @Test
        public void testSet_withPoint() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Point p = Point.of(1, 0);
            m.set(p, 99);
            assertEquals(99, m.get(1, 0));
        }

        // ============ Directional Methods (up, down, left, right) ============

        @Test
        public void testUpOf() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1, up.get());

            OptionalInt noUp = m.valueAbove(0, 0);
            assertFalse(noUp.isPresent());
        }

        @Test
        public void testDownOf() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3, down.get());

            OptionalInt noDown = m.valueBelow(1, 0);
            assertFalse(noDown.isPresent());
        }

        @Test
        public void testLeftOf() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1, left.get());

            OptionalInt noLeft = m.valueLeft(0, 0);
            assertFalse(noLeft.isPresent());
        }

        @Test
        public void testRightOf() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2, right.get());

            OptionalInt noRight = m.valueRight(0, 1);
            assertFalse(noRight.isPresent());
        }

        @Test
        public void testRow_invalidIndex() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(1));
        }

        @Test
        public void testColumn_invalidIndex() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testSetRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setRow(0, new int[] { 10, 20 });
            assertArrayEquals(new int[] { 10, 20 }, m.rowView(0));
            assertEquals(3, m.get(1, 0));
        }

        @Test
        public void testSetRow_invalidLength() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new int[] { 1, 2, 3 }));
        }

        @Test
        public void testSetColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setColumn(0, new int[] { 10, 30 });
            assertEquals(10, m.get(0, 0));
            assertEquals(30, m.get(1, 0));
            assertEquals(2, m.get(0, 1));
        }

        @Test
        public void testSetColumn_invalidLength() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new int[] { 1, 2, 3 }));
        }

        @Test
        public void testUpdateColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateColumn(0, x -> x * 10);
            assertEquals(10, m.get(0, 0));
            assertEquals(30, m.get(1, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void testSetLU2RD_invalidLength() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new int[] { 1, 2, 3 }));
        }

        @Test
        public void testUpdateLU2RD() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateMainDiagonal(x -> x * 10);
            assertEquals(10, m.get(0, 0));
            assertEquals(50, m.get(1, 1));
            assertEquals(90, m.get(2, 2));
        }

        @Test
        public void testSetRU2LD() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.setAntiDiagonal(new int[] { 30, 50, 70 });
            assertEquals(30, m.get(0, 2));
            assertEquals(50, m.get(1, 1));
            assertEquals(70, m.get(2, 0));
        }

        @Test
        public void testUpdateRU2LD() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.updateAntiDiagonal(x -> x + 100);
            assertEquals(103, m.get(0, 2));
            assertEquals(105, m.get(1, 1));
            assertEquals(107, m.get(2, 0));
        }

        @Test
        public void testUpdateAll_biFunction() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll((i, j) -> i * 10 + j);
            assertEquals(0, m.get(0, 0));
            assertEquals(1, m.get(0, 1));
            assertEquals(10, m.get(1, 0));
            assertEquals(11, m.get(1, 1));
        }

        @Test
        public void testReplaceIf_predicate() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.replaceIf(x -> x % 2 == 0, 99);
            assertEquals(1, m.get(0, 0));
            assertEquals(99, m.get(0, 1));
            assertEquals(3, m.get(0, 2));
            assertEquals(99, m.get(1, 0));
        }

        @Test
        public void testReplaceIf_biPredicate() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf((i, j) -> i == j, 0);
            assertEquals(0, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(0, m.get(1, 1));
        }

        // ============ Map Methods ============

        @Test
        public void testMap() {
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
        public void testMapToLong() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.mapToLong(x -> x * 1000L);
            assertEquals(1000L, result.get(0, 0));
            assertEquals(4000L, result.get(1, 1));
        }

        @Test
        public void testMapToDouble() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.mapToDouble(x -> x * 0.5);
            assertEquals(0.5, result.get(0, 0), 0.0001);
            assertEquals(2.0, result.get(1, 1), 0.0001);
        }

        @Test
        public void testMapToObj() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> result = m.mapToObj(x -> "Value:" + x, String.class);
            assertEquals("Value:1", result.get(0, 0));
            assertEquals("Value:4", result.get(1, 1));
        }

        // ============ Fill Methods ============

        @Test
        public void testFill_value() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.fill(99);
            assertEquals(99, m.get(0, 0));
            assertEquals(99, m.get(0, 1));
            assertEquals(99, m.get(1, 0));
            assertEquals(99, m.get(1, 1));
        }

        @Test
        public void testCopyFrom() {
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

        // ============ Copy Methods ============

        @Test
        public void testCopy() {
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
        public void testCopyRegion() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix copy = m.copyRegion(1, 2, 1, 3);
            assertEquals(1, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(5, copy.get(0, 0));
            assertEquals(6, copy.get(0, 1));
        }

        // ============ Extend Methods ============

        @Test
        public void testPad_newSize() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix extended = m.resize(3, 3);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(0, extended.get(2, 2));
        }

        @Test
        public void testPad_withDefaultValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix extended = m.resize(2, 3, 99);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(99, extended.get(0, 2));
            assertEquals(99, extended.get(1, 0));
        }

        @Test
        public void testPad_directions() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 5 } });
            IntMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(5, extended.get(1, 1));
            assertEquals(0, extended.get(0, 0));
        }

        @Test
        public void testPad_directionsWithDefault() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 5 } });
            IntMatrix extended = m.pad(1, 1, 1, 1, 9);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(5, extended.get(1, 1));
            assertEquals(9, extended.get(0, 0));
            assertEquals(9, extended.get(2, 2));
        }

        // ============ Reverse and Flip Methods ============

        @Test
        public void testReverseH() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.flipHorizontallyInPlace();
            assertEquals(3, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(1, m.get(0, 2));
            assertEquals(6, m.get(1, 0));
        }

        @Test
        public void testReverseV() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertEquals(5, m.get(0, 0));
            assertEquals(6, m.get(0, 1));
            assertEquals(1, m.get(2, 0));
        }

        @Test
        public void testFlipH() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix flipped = m.flipHorizontally();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(1, flipped.get(0, 2));
            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void testFlipV() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix flipped = m.flipVertically();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(1, flipped.get(1, 0));
            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }

        // ============ Rotation Methods ============

        @Test
        public void testRotate90() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix rotated = m.rotate90();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4, rotated.get(0, 0));
            assertEquals(1, rotated.get(0, 1));
            assertEquals(6, rotated.get(2, 0));
        }

        @Test
        public void testRotate270() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix rotated = m.rotate270();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3, rotated.get(0, 0));
            assertEquals(6, rotated.get(0, 1));
        }

        // ============ Transpose and Reshape Methods ============

        @Test
        public void testTranspose() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(4, transposed.get(0, 1));
            assertEquals(6, transposed.get(2, 1));
        }

        @Test
        public void testReshape() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = m.reshape(3, 2);
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(6, reshaped.get(2, 1));
        }

        @Test
        public void testRepeatElements() {
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
        public void testRepeatMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix result = m.repeatMatrix(2, 2);
            assertEquals(2, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(2, result.get(0, 1));
            assertEquals(1, result.get(0, 2));
            assertEquals(1, result.get(1, 0));
        }

        // ============ Flatten and FlatOp Methods ============

        @Test
        public void testFlatten() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntList flattened = m.flatten();
            assertEquals(4, flattened.size());
            assertEquals(1, flattened.get(0));
            assertEquals(2, flattened.get(1));
            assertEquals(3, flattened.get(2));
            assertEquals(4, flattened.get(3));
        }

        @Test
        public void testFlatOp() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            final int[] sum = { 0 };
            m.mutateViaFlatArray(row -> {
                for (int val : row) {
                    sum[0] += val;
                }
            });
            assertEquals(10, sum[0]);
        }

        // ============ Stack Methods ============

        @Test
        public void testVstack() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 } });
            IntMatrix stacked = m1.stackVertically(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(3, stacked.get(1, 0));
        }

        @Test
        public void testHstack() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1 }, { 3 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 2 }, { 4 } });
            IntMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals(1, stacked.get(0, 0));
            assertEquals(2, stacked.get(0, 1));
        }

        @Test
        public void testHstack_incompatibleRows() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 2 }, { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        // ============ Arithmetic Methods ============

        @Test
        public void testAdd() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 10, 20 }, { 30, 40 } });
            IntMatrix result = m1.add(m2);
            assertEquals(11, result.get(0, 0));
            assertEquals(22, result.get(0, 1));
            assertEquals(33, result.get(1, 0));
            assertEquals(44, result.get(1, 1));
        }

        @Test
        public void testSubtract() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 10, 20 }, { 30, 40 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m1.subtract(m2);
            assertEquals(9, result.get(0, 0));
            assertEquals(18, result.get(0, 1));
            assertEquals(27, result.get(1, 0));
            assertEquals(36, result.get(1, 1));
        }

        @Test
        public void testMultiply() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 10, 20 }, { 30, 40 } });
            IntMatrix result = m1.matrixMultiply(m2);
            assertEquals(70, result.get(0, 0));
            assertEquals(100, result.get(0, 1));
            assertEquals(150, result.get(1, 0));
            assertEquals(220, result.get(1, 1));
        }

        // ============ Conversion Methods ============

        @Test
        public void testBoxed() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Integer.valueOf(1), boxed.get(0, 0));
            assertEquals(Integer.valueOf(4), boxed.get(1, 1));
        }

        @Test
        public void testToLongMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.mapToLong(i -> (long) i);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1L, result.get(0, 0));
            assertEquals(4L, result.get(1, 1));
        }

        // IntMatrix doesn't have mapToFloat method - removed test
        // @Test
        // public void testToFloatMatrix() {
        //     IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
        //     FloatMatrix result = m.mapToFloat(i -> (float) i);
        //     assertEquals(2, result.rowCount());
        //     assertEquals(2, result.columnCount());
        //     assertEquals(1.0f, result.get(0, 0), 0.0001f);
        //     assertEquals(4.0f, result.get(1, 1), 0.0001f);
        // }

        @Test
        public void testToDoubleMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.mapToDouble(i -> (double) i);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0, result.get(0, 0), 0.0001);
            assertEquals(4.0, result.get(1, 1), 0.0001);
        }

        // ============ ZipWith Methods ============

        @Test
        public void testZipWith_twoMatrices() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 10, 20 }, { 30, 40 } });
            IntMatrix result = m1.zipWith(m2, (a, b) -> a + b);
            assertEquals(11, result.get(0, 0));
            assertEquals(22, result.get(0, 1));
            assertEquals(33, result.get(1, 0));
            assertEquals(44, result.get(1, 1));
        }

        @Test
        public void testZipWith_threeMatrices() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 10, 20 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 100, 200 } });
            IntMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(111, result.get(0, 0));
            assertEquals(222, result.get(0, 1));
        }

        // ============ Stream Methods ============

        @Test
        public void testStreamH_singleRowRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] row1 = m.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new int[] { 4, 5, 6 }, row1);
        }

        @Test
        public void testStreamH_rowRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            int[] rows = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new int[] { 3, 4, 5, 6 }, rows);
        }

        @Test
        public void testStreamV_singleColumnRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] col1 = m.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new int[] { 2, 5 }, col1);
        }

        @Test
        public void testStreamV_columnRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] columnCount = m.columnMajorStream(0, 2).toArray();
            assertArrayEquals(new int[] { 1, 4, 2, 5 }, columnCount);
        }

        // ============ Stream of Streams Methods ============

        // ============ Inherited Methods from AbstractMatrix ============

        @Test
        public void testIsEmpty() {
            assertTrue(IntMatrix.empty().isEmpty());
            assertFalse(IntMatrix.wrap(new int[][] { { 1 } }).isEmpty());
        }

        @Test
        public void testEquals() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 5 } });

            assertEquals(m1, m2);
            assertNotEquals(m1, m3);
            assertNotEquals(m1, null);
            assertNotEquals(m1, "not a matrix");
        }

        @Test
        public void testToString() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("1"));
            assertTrue(str.contains("4"));
        }

        @Test
        public void testToArray() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[][] arr = m.unsafeBackingArray();
            assertEquals(2, arr.length);
            assertEquals(2, arr[0].length);
            assertArrayEquals(new int[] { 1, 2 }, arr[0]);
            assertArrayEquals(new int[] { 3, 4 }, arr[1]);
        }

        // ============ Edge Cases ============

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
    }

    @Nested
    @Tag("2512")
    class IntMatrix2512Test extends TestBase {

        // ============ Constructor Tests ============

        @Test
        public void test_constructor_withValidArray() {
            int[][] arr = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = new IntMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void test_constructor_withNullArray() {
            assertThrows(IllegalArgumentException.class, () -> new IntMatrix(null));
        }

        @Test
        public void test_constructor_withEmptyArray() {
            IntMatrix m = new IntMatrix(new int[0][0]);
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
            assertTrue(m.isEmpty());
        }

        @Test
        public void test_constructor_withSingleElement() {
            IntMatrix m = new IntMatrix(new int[][] { { 42 } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(42, m.get(0, 0));
        }

        @Test
        public void test_of_withValidArray() {
            int[][] arr = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void test_of_withNullArray() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap((int[][]) null));
        }

        @Test
        public void test_of_withSingleRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3, 4, 5 } });
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }

        // ============ Create Method Tests ============

        @Test
        public void test_create_fromCharArray() {
            char[][] chars = { { 'A', 'B' }, { 'C', 'D' } };
            IntMatrix m = IntMatrix.from(chars);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(65, m.get(0, 0)); // ASCII 'A'
            assertEquals(68, m.get(1, 1)); // ASCII 'D'
        }

        @Test
        public void test_create_fromCharArray_nullFirstRow() {
            char[][] chars = { null, { 'A', 'B' } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(chars));
        }

        @Test
        public void test_create_fromCharArray_differentRowLengths() {
            char[][] chars = { { 'A', 'B' }, { 'C' } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(chars));
        }

        @Test
        public void test_create_fromByteArray() {
            byte[][] bytes = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.from(bytes);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void test_create_fromByteArray_empty() {
            IntMatrix m = IntMatrix.from(new byte[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void test_create_fromByteArray_nullFirstRow() {
            byte[][] bytes = { null, { 1, 2 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(bytes));
        }

        @Test
        public void test_create_fromByteArray_differentRowLengths() {
            byte[][] bytes = { { 1, 2 }, { 3 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(bytes));
        }

        @Test
        public void test_create_fromShortArray() {
            short[][] shorts = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.from(shorts);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void test_create_fromShortArray_empty() {
            IntMatrix m = IntMatrix.from(new short[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void test_create_fromShortArray_nullFirstRow() {
            short[][] shorts = { null, { 1, 2 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(shorts));
        }

        // ============ Random Tests ============

        @Test
        public void test_random() {
            IntMatrix m = IntMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }

        @Test
        public void test_random_zeroLength() {
            IntMatrix m = IntMatrix.randomRow(0);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        // ============ Range Tests ============

        @Test
        public void test_range() {
            IntMatrix m = IntMatrix.range(0, 5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, m.rowView(0));
        }

        @Test
        public void test_range_withStep() {
            IntMatrix m = IntMatrix.range(0, 10, 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new int[] { 0, 2, 4, 6, 8 }, m.rowView(0));
        }

        @Test
        public void test_range_empty() {
            IntMatrix m = IntMatrix.range(5, 5);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void test_rangeClosed() {
            IntMatrix m = IntMatrix.rangeClosed(0, 4);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, m.rowView(0));
        }

        @Test
        public void test_rangeClosed_withStep() {
            IntMatrix m = IntMatrix.rangeClosed(0, 10, 2);
            assertEquals(1, m.rowCount());
            assertEquals(6, m.columnCount());
            assertArrayEquals(new int[] { 0, 2, 4, 6, 8, 10 }, m.rowView(0));
        }

        @Test
        public void test_rangeClosed_singleValue() {
            IntMatrix m = IntMatrix.rangeClosed(5, 5);
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(5, m.get(0, 0));
        }

        // ============ Diagonal Tests ============

        @Test
        public void test_ofMainDiagonal() {
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
        public void test_ofMainDiagonal_null() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofMainDiagonal(null));
        }

        @Test
        public void test_ofAntiDiagonal() {
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
        public void test_ofAntiDiagonal_null() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofAntiDiagonal(null));
        }

        @Test
        public void test_diagonal_both() {
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
        public void test_diagonal_differentLengths() {
            int[] lu2rd = { 1, 2 };
            int[] ru2ld = { 4, 5, 6 };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.ofDiagonals(lu2rd, ru2ld));
        }
        // ============ Unbox Test ============

        @Test
        public void test_unbox() {
            Matrix<Integer> boxed = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m = IntMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void test_unbox_withNulls() {
            Matrix<Integer> boxed = Matrix.wrap(new Integer[][] { { 1, null }, { null, 4 } });
            IntMatrix m = IntMatrix.unbox(boxed);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(1, m.get(0, 0));
            assertEquals(0, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
        }

        // ============ Component Type Test ============

        // ============ Get and Set Tests ============

        @Test
        public void test_get_byIndices() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(4, m.get(1, 1));
        }

        @Test
        public void test_get_byPoint() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Point p = Point.of(0, 1);
            assertEquals(2, m.get(p));
        }

        @Test
        public void test_set_byIndices() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.set(0, 1, 9);
            assertEquals(9, m.get(0, 1));
        }

        @Test
        public void test_set_byPoint() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Point p = Point.of(1, 1);
            m.set(p, 9);
            assertEquals(9, m.get(p));
        }

        // ============ Directional Access Tests ============

        @Test
        public void test_upOf() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals(1, up.get());
        }

        @Test
        public void test_upOf_firstRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt up = m.valueAbove(0, 0);
            assertFalse(up.isPresent());
        }

        @Test
        public void test_downOf() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals(3, down.get());
        }

        @Test
        public void test_downOf_lastRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt down = m.valueBelow(1, 0);
            assertFalse(down.isPresent());
        }

        @Test
        public void test_leftOf() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals(1, left.get());
        }

        @Test
        public void test_leftOf_firstColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt left = m.valueLeft(0, 0);
            assertFalse(left.isPresent());
        }

        @Test
        public void test_rightOf() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals(2, right.get());
        }

        @Test
        public void test_rightOf_lastColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            OptionalInt right = m.valueRight(0, 1);
            assertFalse(right.isPresent());
        }

        // ============ Row and Column Access Tests ============

        @Test
        public void test_row() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[] row = m.rowView(0);
            assertArrayEquals(new int[] { 1, 2 }, row);
        }

        @Test
        public void test_row_invalidIndex() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(5));
        }

        @Test
        public void test_column() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[] col = m.columnCopy(0);
            assertArrayEquals(new int[] { 1, 3 }, col);
        }

        @Test
        public void test_column_invalidIndex() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(5));
        }

        @Test
        public void test_setRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setRow(0, new int[] { 9, 8 });
            assertArrayEquals(new int[] { 9, 8 }, m.rowView(0));
        }

        @Test
        public void test_setRow_invalidRowIndex() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setRow(5, new int[] { 1, 2 }));
        }

        @Test
        public void test_setRow_invalidLength() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new int[] { 1 }));
        }

        @Test
        public void test_setColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setColumn(0, new int[] { 9, 8 });
            assertArrayEquals(new int[] { 9, 8 }, m.columnCopy(0));
        }

        @Test
        public void test_setColumn_invalidColumnIndex() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(5, new int[] { 1 }));
        }

        @Test
        public void test_setColumn_invalidLength() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new int[] { 1 }));
        }

        // ============ Update Row and Column Tests ============

        @Test
        public void test_updateRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateRow(0, x -> x * 2);
            assertArrayEquals(new int[] { 2, 4 }, m.rowView(0));
            assertArrayEquals(new int[] { 3, 4 }, m.rowView(1));
        }

        @Test
        public void test_updateColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateColumn(0, x -> x * 2);
            assertArrayEquals(new int[] { 2, 6 }, m.columnCopy(0));
            assertArrayEquals(new int[] { 2, 4 }, m.columnCopy(1));
        }

        // ============ Diagonal Get/Set Tests ============

        @Test
        public void test_mainDiagonalCopy() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new int[] { 1, 5, 9 }, diag);
        }

        @Test
        public void test_mainDiagonalCopy_nonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertThrows(IllegalStateException.class, () -> m.mainDiagonalCopy());
        }

        @Test
        public void test_setMainDiagonal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setMainDiagonal(new int[] { 9, 8 });
            assertArrayEquals(new int[] { 9, 8 }, m.mainDiagonalCopy());
        }

        @Test
        public void test_setMainDiagonal_nonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalStateException.class, () -> m.setMainDiagonal(new int[] { 9 }));
        }

        @Test
        public void test_setMainDiagonal_invalidLength() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new int[] { 9 }));
        }

        @Test
        public void test_updateMainDiagonal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateMainDiagonal(x -> x * 2);
            assertArrayEquals(new int[] { 2, 8 }, m.mainDiagonalCopy());
        }

        @Test
        public void test_antiDiagonalCopy() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new int[] { 3, 5, 7 }, diag);
        }

        @Test
        public void test_setAntiDiagonal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.setAntiDiagonal(new int[] { 9, 8 });
            assertArrayEquals(new int[] { 9, 8 }, m.antiDiagonalCopy());
        }

        @Test
        public void test_updateAntiDiagonal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateAntiDiagonal(x -> x * 2);
            int[] diag = m.antiDiagonalCopy();
            assertEquals(4, diag[0]);
            assertEquals(6, diag[1]);
        }

        // ============ Update All Tests ============

        @Test
        public void test_updateAll_unaryOperator() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll(x -> x * 2);
            assertEquals(2, m.get(0, 0));
            assertEquals(4, m.get(0, 1));
            assertEquals(6, m.get(1, 0));
            assertEquals(8, m.get(1, 1));
        }

        @Test
        public void test_updateAll_biFunction() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.updateAll((i, j) -> (i + 1) * 10 + (j + 1));
            assertEquals(11, m.get(0, 0));
            assertEquals(12, m.get(0, 1));
            assertEquals(21, m.get(1, 0));
            assertEquals(22, m.get(1, 1));
        }

        // ============ Replace If Tests ============

        @Test
        public void test_replaceIf_predicate() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf(x -> x > 2, 99);
            assertEquals(1, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(99, m.get(1, 0));
            assertEquals(99, m.get(1, 1));
        }

        @Test
        public void test_replaceIf_biPredicate() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.replaceIf((i, j) -> i == j, 99);
            assertEquals(99, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(3, m.get(1, 0));
            assertEquals(99, m.get(1, 1));
        }

        // ============ Map Tests ============

        @Test
        public void test_map() {
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
        public void test_mapToLong() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.mapToLong(x -> (long) x);
            assertEquals(1L, result.get(0, 0));
            assertEquals(2L, result.get(0, 1));
            assertEquals(3L, result.get(1, 0));
            assertEquals(4L, result.get(1, 1));
        }

        @Test
        public void test_mapToDouble() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.mapToDouble(x -> (double) x);
            assertEquals(1.0, result.get(0, 0), 0.0);
            assertEquals(2.0, result.get(0, 1), 0.0);
            assertEquals(3.0, result.get(1, 0), 0.0);
            assertEquals(4.0, result.get(1, 1), 0.0);
        }

        @Test
        public void test_mapToObj() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Matrix<String> result = m.mapToObj(x -> String.valueOf(x), String.class);
            assertEquals("1", result.get(0, 0));
            assertEquals("2", result.get(0, 1));
            assertEquals("3", result.get(1, 0));
            assertEquals("4", result.get(1, 1));
        }

        // ============ Fill Tests ============

        @Test
        public void test_fill_value() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.fill(9);
            assertEquals(9, m.get(0, 0));
            assertEquals(9, m.get(0, 1));
            assertEquals(9, m.get(1, 0));
            assertEquals(9, m.get(1, 1));
        }

        @Test
        public void testCopyFrom() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            m.copyFrom(new int[][] { { 9, 8 }, { 7, 6 } });
            assertEquals(9, m.get(0, 0));
            assertEquals(8, m.get(0, 1));
            assertEquals(7, m.get(1, 0));
            assertEquals(6, m.get(1, 1));
        }

        @Test
        public void testCopyFrom_withOffset() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            m.copyFrom(1, 1, new int[][] { { 99 } });
            assertEquals(1, m.get(0, 0));
            assertEquals(99, m.get(1, 1));
            assertEquals(9, m.get(2, 2));
        }

        @Test
        public void testCopyFrom_withOffset_partialFill() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            // When source array is larger than destination, only what fits is copied
            m.copyFrom(0, 0, new int[][] { { 9, 8, 7 } });
            assertEquals(9, m.get(0, 0));
            assertEquals(8, m.get(0, 1));
        }

        // ============ Copy Tests ============

        @Test
        public void test_copy() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals(1, copy.get(0, 0));
            copy.set(0, 0, 99);
            assertEquals(1, m.get(0, 0)); // Original unchanged
        }

        @Test
        public void test_copyRows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            IntMatrix copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(3, copy.get(0, 0));
            assertEquals(6, copy.get(1, 1));
        }

        @Test
        public void test_copyRegion() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix copy = m.copyRegion(1, 3, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(5, copy.get(0, 0));
            assertEquals(9, copy.get(1, 1));
        }

        @Test
        public void test_copy_invalidRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 5));
        }

        @Test
        public void test_pad_withDefaultValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix extended = m.resize(2, 3, 99);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals(1, extended.get(0, 0));
            assertEquals(99, extended.get(1, 1));
            assertEquals(99, extended.get(0, 2));
        }

        @Test
        public void test_pad_smaller() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix result = m.resize(1, 2);
            assertEquals(1, result.rowCount());
            assertEquals(2, result.columnCount());
        }

        @Test
        public void test_pad_directional() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1, extended.get(1, 1));
            assertEquals(0, extended.get(0, 0));
        }

        @Test
        public void test_pad_directional_withDefaultValue() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix extended = m.pad(1, 1, 1, 1, 99);
            assertEquals(3, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(1, extended.get(1, 1));
            assertEquals(99, extended.get(0, 0));
        }

        // ============ Reverse and Flip Tests ============

        @Test
        public void test_flipHorizontallyInPlace() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            m.flipHorizontallyInPlace();
            assertEquals(3, m.get(0, 0));
            assertEquals(2, m.get(0, 1));
            assertEquals(1, m.get(0, 2));
        }

        @Test
        public void test_flipVerticallyInPlace() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            m.flipVerticallyInPlace();
            assertEquals(5, m.get(0, 0));
            assertEquals(3, m.get(1, 0));
            assertEquals(1, m.get(2, 0));
        }

        @Test
        public void test_flipHorizontally() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix flipped = m.flipHorizontally();
            assertEquals(3, flipped.get(0, 0));
            assertEquals(2, flipped.get(0, 1));
            assertEquals(1, flipped.get(0, 2));
            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }

        @Test
        public void test_flipVertically() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            IntMatrix flipped = m.flipVertically();
            assertEquals(5, flipped.get(0, 0));
            assertEquals(3, flipped.get(1, 0));
            assertEquals(1, flipped.get(2, 0));
            // Original unchanged
            assertEquals(1, m.get(0, 0));
        }
        // ============ Transpose Test ============

        @Test
        public void test_transpose() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(4, transposed.get(0, 1));
            assertEquals(2, transposed.get(1, 0));
            assertEquals(5, transposed.get(1, 1));
        }

        @Test
        public void test_transpose_square() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(3, transposed.get(0, 1));
        }

        // ============ Reshape Test ============

        @Test
        public void test_reshape() {
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
        public void test_reshape_expandWithDefaults() {
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

        // ============ Repelem Test ============

        @Test
        public void test_repeatElements() {
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
        public void test_repeatElements_invalidRepeats() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
        }

        // ============ Repmat Test ============

        @Test
        public void test_repeatMatrix() {
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
        public void test_repeatMatrix_invalidRepeats() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
        }

        // ============ Flatten Test ============

        // ============ FlatOp Test ============

        @Test
        public void test_mutateViaFlatArray() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            AtomicInteger count = new AtomicInteger(0);
            m.mutateViaFlatArray(row -> count.addAndGet(row.length));
            assertEquals(4, count.get());
        }

        // ============ Vstack and Hstack Tests ============

        @Test
        public void test_stackVertically() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 } });
            IntMatrix result = m1.stackVertically(m2);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(3, result.get(1, 0));
        }

        @Test
        public void test_vstack_incompatibleCols() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void test_stackHorizontally() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1 }, { 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 }, { 4 } });
            IntMatrix result = m1.stackHorizontally(m2);
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(3, result.get(0, 1));
        }

        @Test
        public void test_hstack_incompatibleRows() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 }, { 4 } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        // ============ Arithmetic Operations Tests ============

        @Test
        public void test_add() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix result = m1.add(m2);
            assertEquals(6, result.get(0, 0));
            assertEquals(8, result.get(0, 1));
            assertEquals(10, result.get(1, 0));
            assertEquals(12, result.get(1, 1));
        }

        @Test
        public void test_add_incompatibleDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void test_subtract() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m1.subtract(m2);
            assertEquals(4, result.get(0, 0));
            assertEquals(4, result.get(0, 1));
            assertEquals(4, result.get(1, 0));
            assertEquals(4, result.get(1, 1));
        }

        @Test
        public void test_subtract_incompatibleDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void test_matrixMultiply() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 2, 0 }, { 1, 2 } });
            IntMatrix result = m1.matrixMultiply(m2);
            assertEquals(4, result.get(0, 0));
            assertEquals(4, result.get(0, 1));
            assertEquals(10, result.get(1, 0));
            assertEquals(8, result.get(1, 1));
        }

        @Test
        public void test_matrixMultiply_incompatibleDimensions() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        // ============ Boxed Test ============

        @Test
        public void test_boxed() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Matrix<Integer> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(1, boxed.get(0, 0));
            assertEquals(4, boxed.get(1, 1));
        }

        // ============ Conversion Tests ============

        @Test
        public void test_toLongMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix result = m.toLongMatrix();
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1L, result.get(0, 0));
            assertEquals(4L, result.get(1, 1));
        }

        @Test
        public void test_toFloatMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            FloatMatrix result = m.toFloatMatrix();
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0f, result.get(0, 0), 0.0f);
            assertEquals(4.0f, result.get(1, 1), 0.0f);
        }

        @Test
        public void test_toDoubleMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            DoubleMatrix result = m.toDoubleMatrix();
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1.0, result.get(0, 0), 0.0);
            assertEquals(4.0, result.get(1, 1), 0.0);
        }

        @Test
        public void test_zipWith_ternary() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 3, 4 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 5, 6 } });
            IntMatrix result = m1.zipWith(m2, m3, (a, b, c) -> a + b + c);
            assertEquals(9, result.get(0, 0));
            assertEquals(12, result.get(0, 1));
        }

        // ============ Stream Tests ============

        @Test
        public void test_horizontalStream() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long sum = m.rowMajorStream().sum();
            assertEquals(10L, sum);
        }

        @Test
        public void test_streamH_bySingleRowRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long sum = m.rowMajorStream(0, 1).sum();
            assertEquals(3L, sum);
        }

        @Test
        public void test_streamH_withRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            long sum = m.rowMajorStream(1, 3).sum();
            assertEquals(18L, sum);
        }

        @Test
        public void test_verticalStream() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long sum = m.columnMajorStream().sum();
            assertEquals(10L, sum);
        }

        @Test
        public void test_streamV_bySingleColumnRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long sum = m.columnMajorStream(0, 1).sum();
            assertEquals(4L, sum);
        }

        @Test
        public void test_streamV_withRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            long sum = m.columnMajorStream(1, 3).sum();
            assertEquals(16L, sum);
        }

        @Test
        public void test_mainDiagonalStream() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long sum = m.mainDiagonalStream().sum();
            assertEquals(5L, sum);
        }

        @Test
        public void test_antiDiagonalStream() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long sum = m.antiDiagonalStream().sum();
            assertEquals(5L, sum);
        }

        @Test
        public void test_rowStreams() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long count = m.rowStreams().count();
            assertEquals(2, count);
        }

        @Test
        public void test_streamR_withRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            long count = m.rowStreams(1, 3).count();
            assertEquals(2, count);
        }

        @Test
        public void test_columnStreams() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            long count = m.columnStreams().count();
            assertEquals(2, count);
        }

        @Test
        public void test_streamC_withRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            long count = m.columnStreams(1, 3).count();
            assertEquals(2, count);
        }

        // ============ ForEach Tests ============

        @Test
        public void test_forEach() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEach(x -> count.incrementAndGet());
            assertEquals(4, count.get());
        }

        @Test
        public void test_forEach_withRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEach(1, 3, 1, 3, x -> count.incrementAndGet());
            assertEquals(4, count.get());
        }

        // ============ Utility Tests ============

        @Test
        public void test_println() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String result = m.toMultilineString();
            assertNotNull(result);
            assertTrue(result.length() > 0);
        }

        @Test
        public void test_equals_same() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(m1, m2);
        }

        @Test
        public void test_equals_different() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 5 } });
            assertNotEquals(m1, m2);
        }

        @Test
        public void test_equals_null() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertNotEquals(m1, null);
        }
    }

    @Nested
    class JavadocExampleMatrixGroup2Test_IntMatrix extends TestBase {
        // ==================== IntMatrix ====================

        @Test
        public void testIntMatrix_ofDiagonals() {
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
        public void testIntMatrix_above() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            u.OptionalInt value = matrix.valueAbove(1, 0);
            assertEquals(1, value.get());
            u.OptionalInt empty = matrix.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testIntMatrix_below() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            u.OptionalInt value = matrix.valueBelow(0, 0);
            assertEquals(3, value.get());
            u.OptionalInt empty = matrix.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testIntMatrix_left() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            u.OptionalInt value = matrix.valueLeft(0, 1);
            assertEquals(1, value.get());
            u.OptionalInt empty = matrix.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testIntMatrix_right() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            u.OptionalInt value = matrix.valueRight(0, 0);
            assertEquals(2, value.get());
            u.OptionalInt empty = matrix.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testIntMatrix_rowView() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] firstRow = matrix.rowView(0);
            assertArrayEquals(new int[] { 1, 2, 3 }, firstRow);
        }

        @Test
        public void testIntMatrix_columnCopy() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] firstColumn = matrix.columnCopy(0);
            assertArrayEquals(new int[] { 1, 4 }, firstColumn);
        }

        @Test
        public void testIntMatrix_updateRow() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            matrix.updateRow(0, x -> x * 2);
            assertArrayEquals(new int[] { 2, 4, 6 }, matrix.rowView(0));
            assertArrayEquals(new int[] { 4, 5, 6 }, matrix.rowView(1));
        }

        @Test
        public void testIntMatrix_updateColumn() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            matrix.updateColumn(0, x -> x + 10);
            assertEquals(11, matrix.get(0, 0));
            assertEquals(2, matrix.get(0, 1));
            assertEquals(13, matrix.get(1, 0));
            assertEquals(4, matrix.get(1, 1));
            assertEquals(15, matrix.get(2, 0));
            assertEquals(6, matrix.get(2, 1));
        }

        @Test
        public void testIntMatrix_mainDiagonalCopy() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diagonal = matrix.mainDiagonalCopy();
            assertArrayEquals(new int[] { 1, 5, 9 }, diagonal);
        }

        @Test
        public void testIntMatrix_updateMainDiagonal() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            matrix.updateMainDiagonal(x -> x * x);
            // matrix is now {{1, 2}, {3, 16}}
            assertEquals(1, matrix.get(0, 0));
            assertEquals(2, matrix.get(0, 1));
            assertEquals(3, matrix.get(1, 0));
            assertEquals(16, matrix.get(1, 1));
        }

        @Test
        public void testIntMatrix_antiDiagonalCopy() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diagonal = matrix.antiDiagonalCopy();
            assertArrayEquals(new int[] { 3, 5, 7 }, diagonal);
        }

        @Test
        public void testIntMatrix_updateAntiDiagonal() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            matrix.updateAntiDiagonal(x -> -x);
            // matrix is now {{1, -2}, {-3, 4}}
            assertEquals(1, matrix.get(0, 0));
            assertEquals(-2, matrix.get(0, 1));
            assertEquals(-3, matrix.get(1, 0));
            assertEquals(4, matrix.get(1, 1));
        }

        @Test
        public void testIntMatrix_updateAll() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            matrix.updateAll(x -> x * 2);
            assertEquals(2, matrix.get(0, 0));
            assertEquals(4, matrix.get(0, 1));
            assertEquals(6, matrix.get(1, 0));
            assertEquals(8, matrix.get(1, 1));
        }

        @Test
        public void testIntMatrix_updateAllByIndex() {
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

        @Test
        public void testIntMatrix_replaceIf() {
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
        public void testIntMatrix_replaceIfByIndex() {
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

        @Test
        public void testIntMatrix_fill() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            matrix.fill(5);
            assertEquals(5, matrix.get(0, 0));
            assertEquals(5, matrix.get(0, 1));
            assertEquals(5, matrix.get(1, 0));
            assertEquals(5, matrix.get(1, 1));
        }

        @Test
        public void testIntMatrix_copyFrom() {
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
        public void testIntMatrix_copyFromWithOffset() {
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
        public void testIntMatrix_resize() {
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
        public void testIntMatrix_resizeWithFill() {
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
        public void testIntMatrix_pad() {
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
        public void testIntMatrix_padWithFill() {
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
        public void testIntMatrix_flipHorizontallyInPlace() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            matrix.flipHorizontallyInPlace();
            assertArrayEquals(new int[] { 3, 2, 1 }, matrix.rowView(0));
            assertArrayEquals(new int[] { 6, 5, 4 }, matrix.rowView(1));
        }

        @Test
        public void testIntMatrix_flipVerticallyInPlace() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            matrix.flipVerticallyInPlace();
            assertArrayEquals(new int[] { 5, 6 }, matrix.rowView(0));
            assertArrayEquals(new int[] { 3, 4 }, matrix.rowView(1));
            assertArrayEquals(new int[] { 1, 2 }, matrix.rowView(2));
        }

        @Test
        public void testIntMatrix_repeatElements() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix repeated = matrix.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertArrayEquals(new int[] { 1, 1, 1, 2, 2, 2 }, repeated.rowView(0));
            assertArrayEquals(new int[] { 1, 1, 1, 2, 2, 2 }, repeated.rowView(1));
        }

        @Test
        public void testIntMatrix_repeatMatrix() {
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
        public void testIntMatrix_flatten() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntList list = matrix.flatten();
            assertEquals(IntList.of(1, 2, 3, 4), list);
        }

        @Test
        public void testIntMatrix_mutateViaFlatArray() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 5, 3 }, { 4, 1 } });
            matrix.mutateViaFlatArray(arr -> java.util.Arrays.sort(arr));
            assertEquals(1, matrix.get(0, 0));
            assertEquals(3, matrix.get(0, 1));
            assertEquals(4, matrix.get(1, 0));
            assertEquals(5, matrix.get(1, 1));
        }

        @Test
        public void testIntMatrix_stackVertically() {
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
        public void testIntMatrix_stackHorizontally() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 7, 8, 9 }, { 10, 11, 12 } });
            IntMatrix c = a.stackHorizontally(b);
            assertEquals(2, c.rowCount());
            assertEquals(6, c.columnCount());
            assertArrayEquals(new int[] { 1, 2, 3, 7, 8, 9 }, c.rowView(0));
            assertArrayEquals(new int[] { 4, 5, 6, 10, 11, 12 }, c.rowView(1));
        }

        @Test
        public void testIntMatrix_add() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix sum = a.add(b);
            assertEquals(6, sum.get(0, 0));
            assertEquals(8, sum.get(0, 1));
            assertEquals(10, sum.get(1, 0));
            assertEquals(12, sum.get(1, 1));
        }

        @Test
        public void testIntMatrix_subtract() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix diff = a.subtract(b);
            assertEquals(4, diff.get(0, 0));
            assertEquals(4, diff.get(0, 1));
            assertEquals(4, diff.get(1, 0));
            assertEquals(4, diff.get(1, 1));
        }

        @Test
        public void testIntMatrix_matrixMultiply() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix product = a.matrixMultiply(b);
            assertEquals(19, product.get(0, 0));
            assertEquals(22, product.get(0, 1));
            assertEquals(43, product.get(1, 0));
            assertEquals(50, product.get(1, 1));
        }

        @Test
        public void testIntMatrix_horizontalStreamSum() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int sum = matrix.rowMajorStream().sum();
            assertEquals(10, sum);
            int[] array = matrix.rowMajorStream().toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, array);
        }

        @Test
        public void testIntMatrix_horizontalStreamSingleRowRange() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int rowSum = matrix.rowMajorStream(1, 2).sum();
            assertEquals(15, rowSum);
            int[] firstRow = matrix.rowMajorStream(0, 1).toArray();
            assertArrayEquals(new int[] { 1, 2, 3 }, firstRow);
        }

        @Test
        public void testIntMatrix_horizontalStreamRange() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            int[] subset = matrix.rowMajorStream(0, 2).toArray();
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, subset);
        }

        @Test
        public void testIntMatrix_verticalStream() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[] colMajor = matrix.columnMajorStream().toArray();
            assertArrayEquals(new int[] { 1, 3, 2, 4 }, colMajor);
        }

        @Test
        public void testIntMatrix_verticalStreamSingleColumnRange() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int colSum = matrix.columnMajorStream(0, 1).sum();
            assertEquals(5, colSum);
            int[] secondCol = matrix.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new int[] { 2, 5 }, secondCol);
        }

        @Test
        public void testIntMatrix_verticalStreamRange() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] subset = matrix.columnMajorStream(0, 2).toArray();
            assertArrayEquals(new int[] { 1, 4, 2, 5 }, subset);
        }

        @Test
        public void testIntMatrix_streamRowSums() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] rowSums = matrix.rowStreams().mapToInt(row -> row.sum()).toArray();
            assertArrayEquals(new int[] { 6, 15, 24 }, rowSums);
        }

        @Test
        public void testIntMatrix_streamColumnSums() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] colSums = matrix.columnStreams().mapToInt(col -> col.sum()).toArray();
            assertArrayEquals(new int[] { 5, 7, 9 }, colSums);
        }
    }

    @Nested
    class JavadocExampleMatrixTest_IntMatrix extends TestBase {
        // ==================== IntMatrix Javadoc Examples ====================

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

        @Test
        public void testIntMatrixWrapGet() {
            // IntMatrix.java: IntMatrix matrix = IntMatrix.wrap(new int[][] {{1, 2}, {3, 4}});
            // matrix.get(0, 1) returns 2
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(2, matrix.get(0, 1));
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
        public void testIntMatrixRange() {
            // IntMatrix.java: IntMatrix matrix = IntMatrix.range(0, 5);   // Creates [[0, 1, 2, 3, 4]]
            IntMatrix matrix = IntMatrix.range(0, 5);
            assertEquals("[[0, 1, 2, 3, 4]]", matrix.toString());
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

        // ==================== IntMatrix additional examples ====================

        @Test
        public void testIntMatrixToString() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals("[[1, 2, 3], [4, 5, 6]]", matrix.toString());
        }

        // ==================== IntMatrix range/rangeClosed examples ====================

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

        // ==================== IntMatrix diagonal examples ====================

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
        public void testIntMatrixAntiDiagonal() {
            IntMatrix matrix = IntMatrix.ofAntiDiagonal(new int[] { 1, 2, 3 });
            assertEquals(3, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
            assertEquals(1, matrix.get(0, 2));
            assertEquals(2, matrix.get(1, 1));
            assertEquals(3, matrix.get(2, 0));
            assertEquals(0, matrix.get(0, 0));
        }

        // ==================== IntMatrix forEach examples ====================

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

        // ==================== IntMatrix fill example ====================

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
    class MatrixZeroColumnRowStreamTest_IntMatrix extends TestBase {
        @Test
        public void testIntMatrixRowsForZeroColumnMatrix() {
            final IntMatrix matrix = IntMatrix.wrap(new int[][] { {}, {}, {} });
            final List<int[]> rows = matrix.rowStreams().map(IntStream::toArray).toList();

            assertEquals(3, rows.size());
            assertArrayEquals(new int[0], rows.get(0));
            assertArrayEquals(new int[0], rows.get(1));
            assertArrayEquals(new int[0], rows.get(2));
        }
    }

    @Nested
    class PrimitiveMatrixUpdateAllNullValidationTest_IntMatrix extends TestBase {
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

    // ============================================================
    // Tests for AbstractMatrix-inherited methods missing coverage
    // ============================================================

    @Nested
    class IntMatrixAbstractMethodsTest extends TestBase {

        @Test
        public void testIsSameShape() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }

        @Test
        public void testIsSameShape_NullThrows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.isSameShape(null));
        }

        @Test
        public void testIsSameShape_EmptyMatrices() {
            IntMatrix e1 = IntMatrix.empty();
            IntMatrix e2 = IntMatrix.empty();
            assertTrue(e1.isSameShape(e2));
        }

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
        public void testPointsMainDiagonal_NonSquareThrows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertThrows(IllegalStateException.class, () -> m.mainDiagonalPoints());
        }

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
        public void testPointsAntiDiagonal_NonSquareThrows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertThrows(IllegalStateException.class, () -> m.antiDiagonalPoints());
        }

        @Test
        public void testPointsHorizontal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
            assertEquals(1, points.get(3).rowIndex());
            assertEquals(1, points.get(3).columnIndex());
        }

        @Test
        public void testPointsHorizontal_SingleRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.rowMajorPoints(0, 0 + 1).toList();
            assertEquals(2, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }

        @Test
        public void testPointsHorizontal_Range() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<Sheet.Point> points = m.rowMajorPoints(1, 3).toList();
            assertEquals(4, points.size());
            assertEquals(1, points.get(0).rowIndex());
        }

        @Test
        public void testPointsVertical() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }

        @Test
        public void testPointsVertical_SingleColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = m.columnMajorPoints(1, 1 + 1).toList();
            assertEquals(2, points.size());
            assertEquals(1, points.get(0).columnIndex());
        }

        @Test
        public void testPointsVertical_Range() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Sheet.Point> points = m.columnMajorPoints(0, 2).toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testPointsRows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<List<Sheet.Point>> rows = m.rowPoints().map(s -> s.toList()).toList();
            assertEquals(3, rows.size());
            assertEquals(2, rows.get(0).size());
            assertEquals(0, rows.get(0).get(0).rowIndex());
            assertEquals(2, rows.get(2).get(0).rowIndex());
        }

        @Test
        public void testPointsRows_Range() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<List<Sheet.Point>> rows = m.rowPoints(1, 3).map(s -> s.toList()).toList();
            assertEquals(2, rows.size());
            assertEquals(1, rows.get(0).get(0).rowIndex());
        }

        @Test
        public void testPointsColumns() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<List<Sheet.Point>> cols = m.columnPoints().map(s -> s.toList()).toList();
            assertEquals(3, cols.size());
            assertEquals(2, cols.get(0).size());
            assertEquals(0, cols.get(0).get(0).columnIndex());
        }

        @Test
        public void testPointsColumns_Range() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<List<Sheet.Point>> cols = m.columnPoints(1, 3).map(s -> s.toList()).toList();
            assertEquals(2, cols.size());
            assertEquals(1, cols.get(0).get(0).columnIndex());
        }

        @Test
        public void testAccept() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[] sum = { 0 };
            m.accept(matrix -> {
                IntList flat = matrix.flatten();
                for (int i = 0; i < flat.size(); i++)
                    sum[0] += flat.get(i);
            });
            assertEquals(10, sum[0]);
        }

        @Test
        public void testAccept_NullThrows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.accept(null));
        }

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
    }

    // --- Bug fix tests ---

    @Test
    public void testSetColumn_throwsConsistentExceptions() {
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

    @Test
    public void testSetColumn_validInput() {
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
    public void testStreamHorizontalIteratorAdvanceAndExhaustion_EdgeCase() {
        IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
        var iterator = matrix.rowMajorStream(0, 2).iterator();

        assertTrue(iterator instanceof com.landawn.abacus.util.stream.IntIteratorEx);

        com.landawn.abacus.util.stream.IntIteratorEx ex = (com.landawn.abacus.util.stream.IntIteratorEx) iterator;
        ex.advance(2);
        assertEquals(2L, ex.count());
        assertEquals(3, ex.nextInt());
        ex.advance(10);
        assertEquals(0L, ex.count());
        assertThrows(java.util.NoSuchElementException.class, ex::nextInt);
    }

    // ============ Additional edge-case verifications (defensive tests) ============

    @Test
    public void testFromShortArray_signExtensionForNegativeValues() {
        // Verify that short -> int widening uses sign extension (not zero extension)
        short[][] src = { { (short) -1, Short.MIN_VALUE }, { (short) 0, Short.MAX_VALUE } };
        IntMatrix m = IntMatrix.from(src);
        assertEquals(-1, m.get(0, 0));
        assertEquals(Short.MIN_VALUE, m.get(0, 1));
        assertEquals(0, m.get(1, 0));
        assertEquals(Short.MAX_VALUE, m.get(1, 1));
    }

    @Test
    public void testFromByteArray_signExtensionForNegativeValues() {
        // Verify that byte -> int widening uses sign extension
        byte[][] src = { { (byte) -1, Byte.MIN_VALUE }, { (byte) 0, Byte.MAX_VALUE } };
        IntMatrix m = IntMatrix.from(src);
        assertEquals(-1, m.get(0, 0));
        assertEquals(Byte.MIN_VALUE, m.get(0, 1));
        assertEquals(0, m.get(1, 0));
        assertEquals(Byte.MAX_VALUE, m.get(1, 1));
    }

    @Test
    public void testFromCharArray_zeroExtensionForHighChars() {
        // Verify that char -> int widening uses zero extension (chars are unsigned)
        char[][] src = { { '\uFFFF', '\0' }, { '\u8000', 'A' } };
        IntMatrix m = IntMatrix.from(src);
        assertEquals(0xFFFF, m.get(0, 0));
        assertEquals(0, m.get(0, 1));
        assertEquals(0x8000, m.get(1, 0));
        assertEquals(65, m.get(1, 1));
    }

    @Test
    public void testReshape_singleRowSourceMultipleStrides() {
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
    public void testReshape_multiRowSourceRowMajorOrder() {
        // Reshape exercises the cnt-based branch (a.length > 1).
        IntMatrix src = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        IntMatrix r = src.reshapeAndPad(2, 4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, r.rowCopy(0));
        assertArrayEquals(new int[] { 5, 6, 0, 0 }, r.rowCopy(1));
    }

    @Test
    public void testReshape_overshootFillsTrailingRowsWithZeros() {
        // newRowCount * newColumnCount > elementCount: trailing rows should be all zeros.
        IntMatrix src = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        IntMatrix r = src.reshapeAndPad(4, 2);
        assertArrayEquals(new int[] { 1, 2 }, r.rowCopy(0));
        assertArrayEquals(new int[] { 3, 4 }, r.rowCopy(1));
        assertArrayEquals(new int[] { 5, 6 }, r.rowCopy(2));
        assertArrayEquals(new int[] { 0, 0 }, r.rowCopy(3));
    }

    @Test
    public void testEqualsHashCode_consistencyAndShape() {
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
        assertEquals(a1, a1);
        assertNotEquals(a1, null);
        assertNotEquals(a1, "not a matrix");
    }

    @Test
    public void testmatrixMultiply_mathematicalCorrectness() {
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
    public void testmatrixMultiply_incompatibleShapesThrows() {
        IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
        IntMatrix b = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
        // a is 2x2 and b is 1x3; columnCount(a)=2 != rowCount(b)=1
        assertThrows(IllegalArgumentException.class, () -> a.matrixMultiply(b));
    }

    @Test
    public void testTranspose_inverseProperty() {
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
    public void testRotate_compositionEqualsRotate180() {
        IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
        // 90 + 90 = 180
        assertEquals(m.rotate180(), m.rotate90().rotate90());
        // 90 + 90 + 90 = 270
        assertEquals(m.rotate270(), m.rotate90().rotate90().rotate90());
        // 90 * 4 = identity
        assertEquals(m, m.rotate90().rotate90().rotate90().rotate90());
    }

    @Test
    public void testFlipVerticallyInPlace_oddRowCount() {
        // The middle row should remain untouched; outer rows should be swapped.
        IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
        m.flipVerticallyInPlace();
        assertArrayEquals(new int[] { 5, 6 }, m.rowCopy(0));
        assertArrayEquals(new int[] { 3, 4 }, m.rowCopy(1));
        assertArrayEquals(new int[] { 1, 2 }, m.rowCopy(2));
    }

    @Test
    public void testPad_emptyMatrixWithPaddingFillsFully() {
        // Padding an empty matrix should produce a matrix entirely filled with defaultValue.
        IntMatrix empty = IntMatrix.empty();
        IntMatrix padded = empty.pad(1, 1, 1, 1, 9);
        assertEquals(2, padded.rowCount());
        assertEquals(2, padded.columnCount());
        assertArrayEquals(new int[] { 9, 9 }, padded.rowCopy(0));
        assertArrayEquals(new int[] { 9, 9 }, padded.rowCopy(1));
    }

    @Test
    public void testPad_emptyMatrixColumnOnlyPaddingNotRepresentable() {
        // Column-only padding of an empty matrix implies a 0 x N shape, which is not representable.
        IntMatrix empty = IntMatrix.empty();
        assertThrows(IllegalArgumentException.class, () -> empty.pad(0, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> empty.pad(0, 0, 0, 2, 9));
        // All-zero padding is a plain copy and stays representable.
        assertEquals(0, empty.pad(0, 0, 0, 0).rowCount());
    }

    @Test
    public void testMainDiagonalStream_iteratesEachDiagonalElementOnce() {
        IntMatrix m = IntMatrix.wrap(new int[][] { { 11, 12, 13 }, { 21, 22, 23 }, { 31, 32, 33 } });
        int[] diag = m.mainDiagonalStream().toArray();
        assertArrayEquals(new int[] { 11, 22, 33 }, diag);
    }

    @Test
    public void testAntiDiagonalStream_iteratesEachAntiDiagonalElementOnce() {
        IntMatrix m = IntMatrix.wrap(new int[][] { { 11, 12, 13 }, { 21, 22, 23 }, { 31, 32, 33 } });
        int[] antiDiag = m.antiDiagonalStream().toArray();
        assertArrayEquals(new int[] { 13, 22, 31 }, antiDiag);
    }

    @Test
    public void testHorizontalAndVerticalStream_traverseAllElementsExactlyOnce() {
        IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        // Horizontal: row-major order; element count must equal elementCount.
        int[] h = m.rowMajorStream().toArray();
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, h);
        // Vertical: column-major order.
        int[] v = m.columnMajorStream().toArray();
        assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 6 }, v);
    }

    @Test
    public void testCopyRegion_independentOfOriginal() {
        IntMatrix src = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
        IntMatrix region = src.copyRegion(0, 2, 1, 3);
        // Mutating the region must NOT change the original.
        region.set(0, 0, 999);
        assertEquals(2, src.get(0, 1));
        // Mutating the original must NOT change the region.
        src.set(0, 1, 888);
        assertEquals(999, region.get(0, 0));
    }

    @Nested
    class PrintlnStrInitFix_IntMatrix extends TestBase {

        @Test
        public void testPrintlnNonEmptyReturnsNonNullFormattedString() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains("1"));
            assertTrue(printed.contains("4"));
            assertFalse(printed.equals("null"));
        }

        @Test
        public void testPrintlnEmptyReturnsBracketsLiteral() {
            IntMatrix empty = IntMatrix.empty();
            String printed = empty.toMultilineString();
            assertEquals("[]", printed);
        }

        @Test
        public void testPrintlnWithExtremeIntValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { Integer.MIN_VALUE, 0, Integer.MAX_VALUE } });
            String printed = m.toMultilineString();
            assertNotNull(printed);
            assertTrue(printed.contains(String.valueOf(Integer.MIN_VALUE)));
            assertTrue(printed.contains(String.valueOf(Integer.MAX_VALUE)));
        }
    }

    @Nested
    class BugHuntRegression {

        // --- from(byte[]) / from(short[]) widening must sign-extend ---

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

        // --- from(char[]) widening must be unsigned (0..65535) ---

        @Test
        public void testFromCharUnsignedWidening() {
            IntMatrix m = IntMatrix.from(new char[][] { { 'A', '\uFFFF', '\0' } });
            assertArrayEquals(new int[] { 65, 65535, 0 }, m.rowCopy(0));
        }

        // --- from(...) rectangular validation rejects ragged input ---

        @Test
        public void testFromByteRaggedRejected() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(new byte[][] { { 1, 2 }, { 3 } }));
        }

        @Test
        public void testFromCharNullSecondRowRejected() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.from(new char[][] { { 'a', 'b' }, null }));
        }

        // --- empty diagonal streams on 0x0 square matrix ---

        @Test
        public void testMainDiagonalStreamEmptyMatrix() {
            assertEquals(0, IntMatrix.empty().mainDiagonalStream().count());
        }

        @Test
        public void testAntiDiagonalStreamEmptyMatrix() {
            assertEquals(0, IntMatrix.empty().antiDiagonalStream().count());
        }

        // --- diagonal stream values (main and anti) ---

        @Test
        public void testMainDiagonalStreamValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertArrayEquals(new int[] { 1, 5, 9 }, m.mainDiagonalStream().toArray());
        }

        @Test
        public void testAntiDiagonalStreamValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            assertArrayEquals(new int[] { 3, 5, 7 }, m.antiDiagonalStream().toArray());
        }

        // --- reshape to a column count of zero must not divide by zero ---

        @Test
        public void testReshapeToZeroColumnsFromEmpty() {
            IntMatrix reshaped = IntMatrix.empty().reshape(0, 0);
            assertEquals(0, reshaped.rowCount());
            assertEquals(0, reshaped.columnCount());
        }

        // --- reshape padding with trailing zeros ---

        @Test
        public void testReshapeWithTrailingZeroFill() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = m.reshapeAndPad(2, 4);
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, reshaped.rowCopy(0));
            assertArrayEquals(new int[] { 5, 6, 0, 0 }, reshaped.rowCopy(1));
        }

        // --- rotate90 / rotate270 / transpose index correctness (non-square) ---

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

        @Test
        public void testRotate270NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix r = m.rotate270();
            assertArrayEquals(new int[] { 3, 6 }, r.rowCopy(0));
            assertArrayEquals(new int[] { 2, 5 }, r.rowCopy(1));
            assertArrayEquals(new int[] { 1, 4 }, r.rowCopy(2));
        }

        @Test
        public void testTransposeNonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix t = m.transpose();
            assertArrayEquals(new int[] { 1, 4 }, t.rowCopy(0));
            assertArrayEquals(new int[] { 2, 5 }, t.rowCopy(1));
            assertArrayEquals(new int[] { 3, 6 }, t.rowCopy(2));
        }

        // --- matrixMultiply must reject incompatible shapes ---

        @Test
        public void testmatrixMultiplyIncompatibleShapesRejected() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2, 3 } }); // 1x3
            IntMatrix b = IntMatrix.wrap(new int[][] { { 1, 2 } }); // 1x2
            assertThrows(IllegalArgumentException.class, () -> a.matrixMultiply(b));
        }

        @Test
        public void testmatrixMultiplyProduct() {
            IntMatrix a = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix b = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            IntMatrix p = a.matrixMultiply(b);
            assertArrayEquals(new int[] { 19, 22 }, p.rowCopy(0));
            assertArrayEquals(new int[] { 43, 50 }, p.rowCopy(1));
        }

        // --- pad fills padding (interior left/right) correctly ---

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

        // --- vertical stream column-major traversal ---

        @Test
        public void testVerticalStreamColumnMajor() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertArrayEquals(new int[] { 1, 4, 2, 5, 3, 6 }, m.columnMajorStream().toArray());
        }
    }

    // Regression tests for the deep-review fixes: diagonal stream square validation,
    // N x 0 transpose/rotate shape contracts, and parallel forEach/forEachIndices ordering.
    @Nested
    public class ReviewBugfixTests {

        @Test
        public void testDiagonalStream_Nx0MatrixThrowsBecauseNotSquare() {
            IntMatrix m = IntMatrix.wrap(new int[3][0]);
            assertEquals(3, m.rowCount());
            assertEquals(0, m.columnCount());
            assertThrows(IllegalStateException.class, () -> m.mainDiagonalStream());
            assertThrows(IllegalStateException.class, () -> m.antiDiagonalStream());

            IntMatrix empty = IntMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().count());
            assertEquals(0, empty.antiDiagonalStream().count());
        }

        @Test
        public void testTranspose_Nx0_collapsesToEmpty() {
            IntMatrix t = IntMatrix.wrap(new int[3][0]).transpose();
            assertEquals(0, t.rowCount());
            assertEquals(0, t.columnCount());
        }

        @Test
        public void testRotate180_Nx0_preservesShape_whileRotate90TwiceCollapses() {
            IntMatrix m = IntMatrix.wrap(new int[3][0]);

            IntMatrix via180 = m.rotate180();
            assertEquals(3, via180.rowCount());
            assertEquals(0, via180.columnCount());

            IntMatrix viaRotate90Twice = m.rotate90().rotate90();
            assertEquals(0, viaRotate90Twice.rowCount());
            assertEquals(0, viaRotate90Twice.columnCount());
        }

        @Test
        public void testReshape_oversizedShapeThrowsIllegalArgumentException() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.reshape(46341, 46341));
        }

        @Test
        public void testForEachRange_sequentialRowMajor_andParallelCompleteness() {
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
        public void testForEachIndices_sequentialRowMajor_andParallelCompleteness() {
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

        @Test
        public void testSetDiagonal_nullArraysOnEmptyMatrixThrow() {
            IntMatrix m = IntMatrix.empty();
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(null));
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(null));
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
        public void testFlipHorizontallyInPlace_reversesSharedBackingRowOnce() {
            int[] sharedRow = { 1, 2, 3, 4 };
            IntMatrix matrix = IntMatrix.wrap(sharedRow, sharedRow);

            matrix.flipHorizontallyInPlace();

            assertArrayEquals(new int[] { 4, 3, 2, 1 }, sharedRow);
            assertSame(matrix.rowView(0), matrix.rowView(1));
        }

        @Test
        public void testUnaryUpdates_transformSharedBackingCellsOnce() {
            int[] sharedColumnRow = { 1, 2 };
            IntMatrix columnMatrix = IntMatrix.wrap(sharedColumnRow, sharedColumnRow);

            columnMatrix.updateColumn(0, value -> value + 1);

            assertArrayEquals(new int[] { 2, 2 }, sharedColumnRow);

            int[] sharedSequentialRow = { 1, 2 };
            IntMatrix sequentialMatrix = IntMatrix.wrap(sharedSequentialRow, sharedSequentialRow);
            AtomicInteger sequentialCalls = new AtomicInteger();

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> sequentialMatrix.updateAll(value -> {
                sequentialCalls.incrementAndGet();
                return value + 1;
            }));

            assertEquals(2, sequentialCalls.get());
            assertArrayEquals(new int[] { 2, 3 }, sharedSequentialRow);

            int[] sharedParallelRow = { 1, 2 };
            IntMatrix parallelMatrix = IntMatrix.wrap(sharedParallelRow, sharedParallelRow);
            AtomicInteger parallelCalls = new AtomicInteger();

            Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> parallelMatrix.updateAll(value -> {
                parallelCalls.incrementAndGet();
                return value + 1;
            }));

            assertEquals(2, parallelCalls.get());
            assertArrayEquals(new int[] { 2, 3 }, sharedParallelRow);

            int[] sharedReplaceRow = { 1, 2 };
            IntMatrix replaceMatrix = IntMatrix.wrap(sharedReplaceRow, sharedReplaceRow);
            AtomicInteger replaceCalls = new AtomicInteger();

            Matrices.runWithParallelMode(ParallelMode.FORCE_ON, () -> replaceMatrix.replaceIf(value -> {
                replaceCalls.incrementAndGet();
                return value > 0;
            }, 9));

            assertEquals(2, replaceCalls.get());
            assertArrayEquals(new int[] { 9, 9 }, sharedReplaceRow);
        }
    }

    @Test
    public void testStreamIterators_midStrideAdvanceThenToArray_EdgeCase() {
        IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });

        var rowMajorIterator = matrix.rowMajorStream(0, 2).iterator();
        assertTrue(rowMajorIterator instanceof com.landawn.abacus.util.stream.IntIteratorEx);
        com.landawn.abacus.util.stream.IntIteratorEx rowMajor = (com.landawn.abacus.util.stream.IntIteratorEx) rowMajorIterator;
        rowMajor.advance(1); // mid-row cursor: the chunked toArray must start at column 1
        assertEquals(5L, rowMajor.count());
        assertArrayEquals(new int[] { 2, 3, 4, 5, 6 }, rowMajor.toArray());

        var columnMajorIterator = matrix.columnMajorStream(0, 3).iterator();
        com.landawn.abacus.util.stream.IntIteratorEx columnMajor = (com.landawn.abacus.util.stream.IntIteratorEx) columnMajorIterator;
        columnMajor.advance(1); // mid-column cursor: row 1 of column 0
        assertEquals(5L, columnMajor.count());
        assertArrayEquals(new int[] { 4, 2, 5, 3, 6 }, columnMajor.toArray());

        var crossingIterator = matrix.columnMajorStream(0, 3).iterator();
        com.landawn.abacus.util.stream.IntIteratorEx crossing = (com.landawn.abacus.util.stream.IntIteratorEx) crossingIterator;
        crossing.advance(3); // crosses a column boundary: lands on row 1 of column 1
        assertEquals(3L, crossing.count());
        assertEquals(5, crossing.nextInt());
        crossing.advance(10);
        assertEquals(0L, crossing.count());
        assertThrows(java.util.NoSuchElementException.class, crossing::nextInt);
    }

    @Test
    public void testFactories_negativeDimensions_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.randomRow(-1));
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.random(-1, 2));
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.random(2, -1));
    }

}
