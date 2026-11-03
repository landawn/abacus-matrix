package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Pins the identity index behind the constructors' duplicate-row scan and copyFrom's aliasing snapshot, and the
 * observable behaviour of both callers.
 */
class RowIdentityIndexTest extends MatrixTestSupport {

    @Test
    void indexFindsEveryAddedRowByIdentityOnly() {
        for (final int n : new int[] { 1, 2, 3, 7, 64, 1000 }) {
            final Object[] rows = new Object[n];

            for (int i = 0; i < n; i++) {
                rows[i] = new int[] { i };
            }

            final AbstractMatrix.RowIdentityIndex index = new AbstractMatrix.RowIdentityIndex(rows, n);

            for (int i = 0; i < n; i++) {
                assertEquals(-1, index.indexOf(rows[i]));
                assertEquals(-1, index.add(i));
                assertEquals(i, index.add(i), "re-adding a position reports the indexed one");
            }

            for (int i = 0; i < n; i++) {
                assertEquals(i, index.indexOf(rows[i]));
                assertEquals(-1, index.indexOf(new int[] { i }), "an equal but distinct array is not found");
            }

            assertEquals(-1, index.indexOf(null));
        }
    }

    @Test
    void indexReportsTheFirstPositionOfARepeatedRow() {
        final int[] shared = { 1 };
        final Object[] rows = { new int[] { 0 }, shared, new int[] { 2 }, shared, shared };
        final AbstractMatrix.RowIdentityIndex index = new AbstractMatrix.RowIdentityIndex(rows, rows.length);

        assertEquals(-1, index.add(0));
        assertEquals(-1, index.add(1));
        assertEquals(-1, index.add(2));
        assertEquals(1, index.add(3));
        assertEquals(1, index.add(4));
        assertEquals(1, index.indexOf(shared));
    }

    @Test
    void chainedLayoutForInputsAboveTheTableCapacityKeepsTheSameContract() {
        // Above MAX_TABLE_CAPACITY the index used to fall back to an IdentityHashMap, which cannot hold more than
        // 2^29 - 1 entries and so failed on exactly the inputs it existed for. The chained layout that replaced it is
        // exercised here with tiny bucket counts, so every chain is long and every bucket collides.
        assertEquals(1 << 30, AbstractMatrix.RowIdentityIndex.MAX_CHAINED_BUCKET_COUNT);
        assertTrue(AbstractMatrix.RowIdentityIndex.MAX_CHAINED_BUCKET_COUNT >= 2 * AbstractMatrix.RowIdentityIndex.MAX_TABLE_CAPACITY);

        final Random random = new Random(27);

        for (final int bucketCount : new int[] { 2, 4, 8, 1024 }) {
            for (int trial = 0; trial < 200; trial++) {
                final int n = 1 + random.nextInt(300);
                final Object[] rows = new Object[n];
                final java.util.IdentityHashMap<Object, Integer> expected = new java.util.IdentityHashMap<>();

                for (int i = 0; i < n; i++) {
                    rows[i] = i > 0 && random.nextInt(4) == 0 ? rows[random.nextInt(i)] : new int[] { i };
                }

                final AbstractMatrix.RowIdentityIndex index = new AbstractMatrix.RowIdentityIndex(rows, n, bucketCount);

                for (int i = 0; i < n; i++) {
                    final Integer previous = expected.putIfAbsent(rows[i], i);
                    assertEquals(previous == null ? -1 : previous, index.add(i), "buckets " + bucketCount + ", trial " + trial + ", add " + i);
                }

                for (int i = 0; i < n; i++) {
                    assertEquals(expected.get(rows[i]), index.indexOf(rows[i]));
                }

                assertEquals(-1, index.indexOf(new int[] { 0 }), "an equal but distinct array is not found");
                assertEquals(-1, index.indexOf(null));
            }
        }
    }

    @Test
    void chainedLayoutIndexesNullAndPositionsAddedOutOfOrder() {
        final int[] shared = { 1 };
        final Object[] rows = { null, shared, new int[] { 2 }, shared, null, new int[0] };
        final AbstractMatrix.RowIdentityIndex index = new AbstractMatrix.RowIdentityIndex(rows, rows.length, 2);

        // copyFrom adds destination positions that need not start at 0.
        assertEquals(-1, index.add(5));
        assertEquals(-1, index.add(3));
        assertEquals(-1, index.add(2));
        assertEquals(3, index.add(1));
        assertEquals(-1, index.indexOf(null));
        assertEquals(-1, index.add(0));
        assertEquals(0, index.add(4));
        assertEquals(0, index.indexOf(null));
        assertEquals(5, index.indexOf(rows[5]));
    }

    @Test
    void constructorReportsTheLaterAndFirstIndexOfADuplicateRowInTallInputs() {
        final int n = 70_000;
        final int[][] rows = new int[n][];

        for (int i = 0; i < n; i++) {
            rows[i] = new int[] { i, -i };
        }

        assertEquals(n, IntMatrix.wrap(rows).rowCount());

        for (final int[] pair : new int[][] { { 0, 1 }, { 0, n - 1 }, { n / 2, n - 1 }, { n - 2, n - 1 }, { 128, 129 } }) {
            final int[][] withDuplicate = rows.clone();
            withDuplicate[pair[1]] = withDuplicate[pair[0]];
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(withDuplicate));
            assertEquals("Rows must have independent storage: row " + pair[1] + " is the same array as row " + pair[0], e.getMessage());
        }

        final int[][] empties = new int[n][];

        for (int i = 0; i < n; i++) {
            empties[i] = new int[0];
        }

        assertEquals(n, IntMatrix.wrap(empties).rowCount());
        empties[n - 1] = empties[3];
        assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(empties));
    }

    @Test
    void constructorStillReportsWhicheverRowProblemComesFirst() {
        final int[] shared = { 1 };
        final IllegalArgumentException duplicateFirst = assertThrows(IllegalArgumentException.class,
                () -> IntMatrix.wrap(new int[][] { shared, shared, null }));
        assertEquals("Rows must have independent storage: row 1 is the same array as row 0", duplicateFirst.getMessage());

        final IllegalArgumentException nullFirst = assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(new int[][] { shared, null, shared }));
        assertEquals("Row 1 cannot be null", nullFirst.getMessage());
    }

    @Test
    void copyFromWithAliasedSourceRowsMatchesCopyingFromAnIndependentSnapshot() {
        final Random random = new Random(26);

        for (int trial = 0; trial < 3000; trial++) {
            final int rowCount = 1 + random.nextInt(6);
            final int columnCount = 1 + random.nextInt(5);
            final IntMatrix matrix = IntMatrix.random(rowCount, columnCount, random);
            final int[][] source = new int[1 + random.nextInt(rowCount + 2)][];

            for (int i = 0; i < source.length; i++) {
                final int pick = random.nextInt(5);
                source[i] = pick == 0 ? null : pick == 1 ? new int[random.nextInt(columnCount + 2)] : matrix.rowView(random.nextInt(rowCount));
            }

            final int destRow = random.nextInt(rowCount);
            final int destColumn = random.nextInt(columnCount);

            // Reference: the same copy into a deep copy, reading from deep copies of the source rows.
            final IntMatrix expected = matrix.copy();
            final int[][] independentSource = new int[source.length][];

            for (int i = 0; i < source.length; i++) {
                independentSource[i] = source[i] == null ? null : source[i].clone();
            }

            expected.copyFrom(destRow, destColumn, independentSource);
            matrix.copyFrom(destRow, destColumn, source);
            assertEquals(expected, matrix, "trial " + trial);
        }
    }

    @Test
    void copyFromShiftingATallMatrixOntoItselfUsesTheOriginalRows() {
        final int n = 5000;
        final IntMatrix matrix = IntMatrix.range(0, n).reshape(n, 1);
        final int[][] ownRows = new int[n][];

        for (int i = 0; i < n; i++) {
            ownRows[i] = matrix.rowView(i);
        }

        // Shift every row down by one and repeat row 0 as the last source row: each written row is also read later.
        final int[][] source = new int[n][];
        source[0] = ownRows[n - 1];
        System.arraycopy(ownRows, 0, source, 1, n - 1);
        matrix.copyFrom(source);

        assertEquals(n - 1, matrix.get(0, 0));

        for (int i = 1; i < n; i++) {
            assertEquals(i - 1, matrix.get(i, 0));
        }
    }
}
