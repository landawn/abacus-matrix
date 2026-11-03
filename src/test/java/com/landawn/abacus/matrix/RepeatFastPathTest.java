package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Pins the bulk-copy paths of {@code repeatElements(rowRepeats, 1)} (one row copy instead of a one-cell fill per
 * element) and {@code repeatMatrix} (rows shorter than 64 elements are tiled by doubling the filled prefix) against a
 * cell-by-cell reference, for every variant and across the narrow/wide tiling threshold.
 */
class RepeatFastPathTest extends MatrixTestSupport {

    private static final int[][] SHAPES = { { 0, 0 }, { 0, 3 }, { 3, 0 }, { 1, 1 }, { 2, 3 }, { 1, 62 }, { 3, 63 }, { 2, 64 }, { 4, 65 }, { 5, 1 } };

    private static final int[][] REPEATS = { { 1, 1 }, { 2, 1 }, { 3, 1 }, { 1, 2 }, { 2, 3 }, { 1, 7 }, { 1, 64 }, { 2, 100 } };

    /** wrap canonicalizes a rowless array to 0 x 0, while random keeps the 0 x N shape the operations preserve. */
    private static IntMatrix shaped(final int[][] c, final int columnCount) {
        return c.length == 0 ? IntMatrix.random(0, columnCount, new Random(0)) : IntMatrix.wrap(c);
    }

    private static IntMatrix referenceRepeatElements(final IntMatrix m, final int rowRepeats, final int columnRepeats) {
        final int[][] c = new int[m.rowCount() * rowRepeats][m.columnCount() * columnRepeats];

        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < c[i].length; j++) {
                c[i][j] = m.get(i / rowRepeats, j / columnRepeats);
            }
        }

        return shaped(c, m.columnCount() * columnRepeats);
    }

    private static IntMatrix referenceRepeatMatrix(final IntMatrix m, final int rowRepeats, final int columnRepeats) {
        final int[][] c = new int[m.rowCount() * rowRepeats][m.columnCount() * columnRepeats];

        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < c[i].length; j++) {
                c[i][j] = m.get(i % m.rowCount(), j % m.columnCount());
            }
        }

        return shaped(c, m.columnCount() * columnRepeats);
    }

    private static ShortMatrix toShort(final IntMatrix m) {
        return m.mapToObj(x -> (short) x, Short.class).mapToShort(x -> x);
    }

    private static ByteMatrix toByte(final IntMatrix m) {
        return m.mapToObj(x -> (byte) x, Byte.class).mapToByte(x -> x);
    }

    private static CharMatrix toChar(final IntMatrix m) {
        return m.mapToObj(x -> (char) x, Character.class).mapToChar(x -> x);
    }

    private static BooleanMatrix toBoolean(final IntMatrix m) {
        return m.mapToObj(x -> (x & 1) == 0, Boolean.class).mapToBoolean(x -> x);
    }

    private static Matrix<String> toStrings(final IntMatrix m) {
        return m.mapToObj(x -> "s" + (x & 15), String.class);
    }

    @Test
    void repeatElementsAndRepeatMatrixMatchACellByCellReferenceForEveryVariant() {
        for (final int[] shape : SHAPES) {
            final IntMatrix m = IntMatrix.random(shape[0], shape[1], new Random(shape[0] * 131L + shape[1]));

            for (final int[] repeats : REPEATS) {
                final int rr = repeats[0];
                final int cr = repeats[1];
                final String label = shape[0] + "x" + shape[1] + " repeated (" + rr + ", " + cr + ")";
                final IntMatrix elements = referenceRepeatElements(m, rr, cr);
                final IntMatrix tiled = referenceRepeatMatrix(m, rr, cr);

                assertEquals(elements, m.repeatElements(rr, cr), "int repeatElements " + label);
                assertEquals(tiled, m.repeatMatrix(rr, cr), "int repeatMatrix " + label);
                assertEquals(elements.toLongMatrix(), m.toLongMatrix().repeatElements(rr, cr), "long repeatElements " + label);
                assertEquals(tiled.toLongMatrix(), m.toLongMatrix().repeatMatrix(rr, cr), "long repeatMatrix " + label);
                assertEquals(elements.toFloatMatrix(), m.toFloatMatrix().repeatElements(rr, cr), "float repeatElements " + label);
                assertEquals(tiled.toFloatMatrix(), m.toFloatMatrix().repeatMatrix(rr, cr), "float repeatMatrix " + label);
                assertEquals(elements.toDoubleMatrix(), m.toDoubleMatrix().repeatElements(rr, cr), "double repeatElements " + label);
                assertEquals(tiled.toDoubleMatrix(), m.toDoubleMatrix().repeatMatrix(rr, cr), "double repeatMatrix " + label);
                assertEquals(toShort(elements), toShort(m).repeatElements(rr, cr), "short repeatElements " + label);
                assertEquals(toShort(tiled), toShort(m).repeatMatrix(rr, cr), "short repeatMatrix " + label);
                assertEquals(toByte(elements), toByte(m).repeatElements(rr, cr), "byte repeatElements " + label);
                assertEquals(toByte(tiled), toByte(m).repeatMatrix(rr, cr), "byte repeatMatrix " + label);
                assertEquals(toChar(elements), toChar(m).repeatElements(rr, cr), "char repeatElements " + label);
                assertEquals(toChar(tiled), toChar(m).repeatMatrix(rr, cr), "char repeatMatrix " + label);
                assertEquals(toBoolean(elements), toBoolean(m).repeatElements(rr, cr), "boolean repeatElements " + label);
                assertEquals(toBoolean(tiled), toBoolean(m).repeatMatrix(rr, cr), "boolean repeatMatrix " + label);
                assertEquals(toStrings(elements), toStrings(m).repeatElements(rr, cr), "Matrix repeatElements " + label);
                assertEquals(toStrings(tiled), toStrings(m).repeatMatrix(rr, cr), "Matrix repeatMatrix " + label);
            }
        }
    }

    @Test
    void rowsRepeatedByRepeatElementsAreIndependentCopies() {
        final IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
        final IntMatrix repeated = m.repeatElements(3, 1);

        repeated.set(0, 0, 100);
        repeated.set(4, 2, 600);

        assertEquals(IntMatrix.wrap(new int[][] { { 100, 2, 3 }, { 1, 2, 3 }, { 1, 2, 3 }, { 4, 5, 6 }, { 4, 5, 600 }, { 4, 5, 6 } }), repeated);
        assertEquals(IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } }), m, "the source is untouched");

        final Matrix<String> strings = Matrix.wrap(String.class, new String[][] { { "a", "b" } });
        final Matrix<String> repeatedStrings = strings.repeatElements(2, 1);
        repeatedStrings.set(1, 1, "z");
        assertEquals("b", repeatedStrings.get(0, 1));
        assertEquals("b", strings.get(0, 1));
    }

    @Test
    void tileRowWritesExactlyTheRequestedTilesForNarrowAndWideRows() {
        final Random random = new Random(27);

        for (int length = 0; length <= 70; length++) {
            for (final int repeats : new int[] { 1, 2, 3, 5, 8, 17, 64, 65 }) {
                final int[] source = random.ints(length).toArray();
                final int[] target = new int[length * repeats + 3];
                java.util.Arrays.fill(target, -7);

                AbstractMatrix.tileRow(source, target, length, repeats);

                for (int p = 0; p < length * repeats; p++) {
                    assertEquals(source[p % length], target[p], "length " + length + ", repeats " + repeats + ", position " + p);
                }

                assertArrayEquals(new int[] { -7, -7, -7 }, java.util.Arrays.copyOfRange(target, length * repeats, target.length),
                        "no write past the tiles for length " + length + ", repeats " + repeats);
            }
        }

        final String[] words = { "x", null, "z" };
        final String[] tiledWords = new String[7];
        AbstractMatrix.tileRow(words, tiledWords, 2, 3);
        assertArrayEquals(new String[] { "x", null, "x", null, "x", null, null }, tiledWords);
    }

    @Test
    void repeatMatrixOfAColumnVectorAcrossManyColumns() {
        final IntMatrix column = IntMatrix.range(0, 1000).reshape(1000, 1);
        final IntMatrix tiled = column.repeatMatrix(1, 1000);

        assertEquals(1000, tiled.rowCount());
        assertEquals(1000, tiled.columnCount());

        for (int i = 0; i < 1000; i += 37) {
            final int[] row = tiled.rowView(i);

            for (int j = 0; j < 1000; j++) {
                assertEquals(i, row[j]);
            }
        }

        assertNotSame(tiled.rowView(0), tiled.rowView(1));
    }
}
