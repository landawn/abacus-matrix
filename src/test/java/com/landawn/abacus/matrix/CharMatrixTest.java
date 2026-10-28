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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.stream.CharIteratorEx;
import com.landawn.abacus.util.stream.CharStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;
import com.landawn.abacus.util.u.OptionalChar;

class CharMatrixTest extends MatrixTestSupport {

    private static void assertShape(final int expectedRows, final int expectedColumns, final AbstractMatrix<?, ?, ?, ?, ?> matrix) {
        assertEquals(expectedRows, matrix.rowCount());
        assertEquals(expectedColumns, matrix.columnCount());
    }

    @Nested
    class ConstructorTests {

        @Test
        public void testConstructor() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = new CharMatrix(a);
            Assertions.assertEquals(2, matrix.rowCount());
            Assertions.assertEquals(2, matrix.columnCount());

            assertThrows(IllegalArgumentException.class, () -> new CharMatrix(null));
        }

        @Test
        public void testConstructor_WithValidArray() {
            char[][] arr = { { 'A', 'B' }, { 'C', 'D' } };
            CharMatrix m = new CharMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals('A', m.get(0, 0));
            assertEquals('D', m.get(1, 1));
        }

        @Test
        public void testConstructor_WithValidArray_Values() {
            char[][] arr = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix m = new CharMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('c', m.get(1, 0));
            assertEquals('d', m.get(1, 1));
        }

        @Test
        public void testConstructor_WithNonSquareMatrix() {
            char[][] arr = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix m = new CharMatrix(arr);
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
        }

        @Test
        public void testConstructor_WithNumbers() {
            char[][] arr = { { '1', '2' }, { '3', '4' } };
            CharMatrix m = new CharMatrix(arr);
            assertEquals('1', m.get(0, 0));
            assertEquals('4', m.get(1, 1));
        }

        @Test
        public void testConstructor_LargeMatrix() {
            char[][] arr = new char[100][100];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    arr[i][j] = (char) ('a' + ((i + j) % 26));
                }
            }
            CharMatrix m = new CharMatrix(arr);
            assertEquals(100, m.rowCount());
            assertEquals(100, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
        }

        @Test
        public void testConstructor_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> new CharMatrix(null));
        }

        @Test
        public void testConstructor_WithEmptyArray() {
            CharMatrix m = new CharMatrix(new char[0][0]);
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
            assertTrue(m.isEmpty());
        }

        @Test
        public void testConstructor_WithSingleElement() {
            CharMatrix m = new CharMatrix(new char[][] { { 'X' } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals('X', m.get(0, 0));
        }

        @Test
        public void testConstructor_WithSingleElement_Shape1By1() {
            CharMatrix m = new CharMatrix(new char[][] { { 'x' } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals('x', m.get(0, 0));
        }
    }

    @Nested
    class EmptyTests {

        @Test
        public void testEmpty() {
            CharMatrix empty = CharMatrix.empty();
            Assertions.assertEquals(0, empty.rowCount());
            Assertions.assertEquals(0, empty.columnCount());
            Assertions.assertTrue(empty.isEmpty());
        }

        @Test
        public void testEmpty_EmptyMatrix_Identity() {
            CharMatrix empty = CharMatrix.empty();
            assertEquals(0, empty.rowCount());
            assertEquals(0, empty.columnCount());
            assertTrue(empty.isEmpty());

            // Test singleton
            assertSame(CharMatrix.empty(), CharMatrix.empty());
        }

        @Test
        public void testEmptyMatrixOperations() {
            CharMatrix empty = CharMatrix.empty();

            assertTrue(empty.flatten().isEmpty());
            assertEquals(0, empty.copy().rowCount);
            assertEquals(empty, empty.transpose());
            assertEquals(empty, empty.rotate90());

            CharMatrix extended = empty.resize(2, 2, 'X');
            assertEquals(2, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals('X', extended.get(0, 0));
        }
    }

    @Nested
    class WrapTests {

        @Test
        public void testWrap() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);
            Assertions.assertEquals(2, matrix.rowCount());
            Assertions.assertEquals(2, matrix.columnCount());

            CharMatrix emptyMatrix = CharMatrix.wrap();
            Assertions.assertTrue(emptyMatrix.isEmpty());

            assertThrows(IllegalArgumentException.class, () -> CharMatrix.wrap((char[][]) null));
        }

        @Test
        public void testWrap_WithValidArray() {
            char[][] arr = { { 'A', 'B' }, { 'C', 'D' } };
            CharMatrix m = CharMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals('A', m.get(0, 0));
        }

        @Test
        public void testUnicodeCharacters() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\u0041', '\u0042' }, { '\u4E2D', '\u6587' } });
            assertEquals('A', m.get(0, 0)); // Unicode for 'A'
            assertEquals('B', m.get(0, 1)); // Unicode for 'B'
            assertEquals('\u4E2D', m.get(1, 0)); // Chinese character
            assertEquals('\u6587', m.get(1, 1)); // Chinese character
        }

        // Test toFloatMatrix() with tall matrix
        @Test
        public void testSpecialCharacters() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\n', '\t' }, { ' ', '!' } });
            assertEquals('\n', m.get(0, 0));
            assertEquals('\t', m.get(0, 1));
            assertEquals(' ', m.get(1, 0));
            assertEquals('!', m.get(1, 1));
        }

        @Test
        public void testWrap_WithValidArray_Values() {
            char[][] arr = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix m = CharMatrix.wrap(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals('a', m.get(0, 0));
        }

        @Test
        public void testCharMatrixWrapGet() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            assertEquals('a', matrix.get(0, 0));
            assertEquals('f', matrix.get(1, 2));
            assertEquals(2, matrix.rowCount());
            assertEquals(3, matrix.columnCount());
        }

        @Test
        public void testWrap_WithNullArray() {
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.wrap((char[][]) null));
        }

        @Test
        public void testWrap_WithEmptyArray() {
            CharMatrix m = CharMatrix.wrap(new char[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testCharMinMaxValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { Character.MAX_VALUE, Character.MIN_VALUE } });
            assertEquals(Character.MAX_VALUE, m.get(0, 0));
            assertEquals(Character.MIN_VALUE, m.get(0, 1));
        }

        @Test
        public void testWrap_WithSingleRow() {
            CharMatrix m = CharMatrix.wrap(new char[] { 'a', 'b', 'c' });
            assertEquals(1, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('c', m.get(0, 2));
        }
    }

    @Nested
    class CopyOfTests {

        @Test
        public void testCopyOf() {
            char[][] arr = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix m = CharMatrix.copyOf(arr);
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('d', m.get(1, 1));

            // Mutating the source array must not affect the copy
            arr[0][0] = 'x';
            assertEquals('a', m.get(0, 0));

            // Mutating the copy must not affect the source array
            m.set(1, 1, 'z');
            assertEquals('d', arr[1][1]);

            // null -> rejected; empty -> shared empty matrix
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.copyOf((char[][]) null));
            assertTrue(CharMatrix.copyOf(new char[0][0]).isEmpty());

            // non-rectangular -> IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.copyOf(new char[][] { { 'a', 'b' }, { 'c' } }));
        }
    }

    @Nested
    class RandomRowTests {

        @Test
        public void testRandom() {
            CharMatrix matrix = CharMatrix.randomRow(5);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(5, matrix.columnCount());
        }

        @Test
        public void testRandom_Values() {
            CharMatrix m = CharMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            // Just verify elements exist (values are random)
            for (int i = 0; i < 5; i++) {
                assertNotNull(m.get(0, i));
            }
        }

        @Test
        public void testRandom_WithLargeLength() {
            CharMatrix m = CharMatrix.randomRow(1000);
            assertEquals(1, m.rowCount());
            assertEquals(1000, m.columnCount());
        }

        @Test
        public void testRandomRow_Values_SingleRowDimensions() {
            CharMatrix m = CharMatrix.randomRow(5);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
        }

        @Test
        public void testRandom_WithZeroLength() {
            CharMatrix m = CharMatrix.randomRow(0);
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testFactories_NegativeDimensions_ThrowIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.randomRow(-1));
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.random(-1, 2));
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.random(2, -1));
        }
    }

    @Nested
    class RandomTests {

        @Test
        public void testRandomGeneratorOverloadsAreReproducibleAndPreserveZeroRowShape() {
            assertEquals(CharMatrix.random(4, 5, new java.util.Random(1234)), CharMatrix.random(4, 5, new java.util.Random(1234)));
            assertEquals(CharMatrix.randomRow(5, new java.util.Random(4321)), CharMatrix.randomRow(5, new java.util.Random(4321)));
            assertShape(0, 7, CharMatrix.random(0, 7, new java.util.Random(1)));
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.random(1, 1, null));
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.randomRow(1, null));
        }

        @Test
        public void testRandom_WithRowsCols() {
            CharMatrix m = CharMatrix.random(2, 3);
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
        public void testEmptyRangesOwnTheirRow() {
            // Array.range/rangeClosed return a shared empty-array constant for an empty range; each matrix must own
            // its row, or rows of independently created empty range matrices could not be wrapped together.
            final char[][] rows = { CharMatrix.range('e', 'e').rowView(0), CharMatrix.range('e', 'a', 1).rowView(0),
                    CharMatrix.rangeClosed('z', 'a').rowView(0), CharMatrix.rangeClosed('a', 'z', -1).rowView(0) };

            for (final char[] row : rows) {
                assertEquals(0, row.length);
                assertNotSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, row);
            }

            final CharMatrix combined = CharMatrix.wrap(rows);
            assertEquals(4, combined.rowCount());
            assertEquals(0, combined.columnCount());
        }

        @Test
        public void testRange() {
            CharMatrix matrix = CharMatrix.range('a', 'e');
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(4, matrix.columnCount());
            Assertions.assertEquals('a', matrix.get(0, 0));
            Assertions.assertEquals('b', matrix.get(0, 1));
            Assertions.assertEquals('c', matrix.get(0, 2));
            Assertions.assertEquals('d', matrix.get(0, 3));
        }

        @Test
        public void testRangeWithStep() {
            CharMatrix matrix = CharMatrix.range('a', 'g', 2);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals('a', matrix.get(0, 0));
            Assertions.assertEquals('e', matrix.get(0, 2));
        }

        @Test
        public void testRange_ArrayValues() {
            CharMatrix m = CharMatrix.range('A', 'F');
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new char[] { 'A', 'B', 'C', 'D', 'E' }, m.rowView(0));
        }

        @Test
        public void testRange_WithStep() {
            CharMatrix m = CharMatrix.range('A', 'K', 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new char[] { 'A', 'C', 'E', 'G', 'I' }, m.rowView(0));
        }

        @Test
        public void testRange_Values() {
            CharMatrix m = CharMatrix.range('a', 'e');
            assertEquals(1, m.rowCount());
            assertEquals(4, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('c', m.get(0, 2));
            assertEquals('d', m.get(0, 3));
        }

        @Test
        public void testRange_WithStep_Values() {
            CharMatrix m = CharMatrix.range('a', 'j', 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('c', m.get(0, 1));
            assertEquals('e', m.get(0, 2));
            assertEquals('g', m.get(0, 3));
            assertEquals('i', m.get(0, 4));
        }

        @Test
        public void testRange_WithStep_Values_SteppedAlphabet() {
            CharMatrix m = CharMatrix.range('a', 'k', 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('c', m.get(0, 1));
            assertEquals('e', m.get(0, 2));
            assertEquals('g', m.get(0, 3));
            assertEquals('i', m.get(0, 4));
        }

        @Test
        public void testRange_Digits() {
            CharMatrix m = CharMatrix.range('0', '5');
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals('0', m.get(0, 0));
            assertEquals('4', m.get(0, 4));
        }

        @Test
        public void testRange_Values_ExclusiveAlphabetEndpoint() {
            CharMatrix m = CharMatrix.range('a', 'f');
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('e', m.get(0, 4));
        }

        @Test
        public void testRange_EmptyRange() {
            CharMatrix m = CharMatrix.range('a', 'a');
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testRange_EmptyRange_Values() {
            CharMatrix m = CharMatrix.range('e', 'e');
            assertEquals(1, m.rowCount());
            assertEquals(0, m.columnCount());
        }

        @Test
        public void testRange_WithNegativeStep() {
            CharMatrix m = CharMatrix.range('J', 'A', -2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new char[] { 'J', 'H', 'F', 'D', 'B' }, m.rowView(0));
        }
    }

    @Nested
    class RangeClosedTests {

        @Test
        public void testRangeClosed() {
            CharMatrix matrix = CharMatrix.rangeClosed('a', 'd');
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(4, matrix.columnCount());
            Assertions.assertEquals('a', matrix.get(0, 0));
            Assertions.assertEquals('d', matrix.get(0, 3));
        }

        @Test
        public void testRangeClosedWithStep() {
            CharMatrix matrix = CharMatrix.rangeClosed('a', 'g', 2);
            Assertions.assertEquals(1, matrix.rowCount());
            Assertions.assertEquals(4, matrix.columnCount());
            Assertions.assertEquals('a', matrix.get(0, 0));
            Assertions.assertEquals('g', matrix.get(0, 3));
        }

        @Test
        public void testRangeClosed_ArrayValues() {
            CharMatrix m = CharMatrix.rangeClosed('A', 'E');
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertArrayEquals(new char[] { 'A', 'B', 'C', 'D', 'E' }, m.rowView(0));
        }

        @Test
        public void testRangeClosed_WithStep() {
            CharMatrix m = CharMatrix.rangeClosed('A', 'K', 2);
            assertEquals(1, m.rowCount());
            assertEquals(6, m.columnCount());
            assertArrayEquals(new char[] { 'A', 'C', 'E', 'G', 'I', 'K' }, m.rowView(0));
        }

        @Test
        public void testRangeClosed_Values() {
            CharMatrix m = CharMatrix.rangeClosed('a', 'd');
            assertEquals(1, m.rowCount());
            assertEquals(4, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('d', m.get(0, 3));
        }

        @Test
        public void testRangeClosed_WithStep_Values() {
            CharMatrix m = CharMatrix.rangeClosed('a', 'j', 3);
            assertEquals(1, m.rowCount());
            assertEquals(4, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('d', m.get(0, 1));
            assertEquals('g', m.get(0, 2));
            assertEquals('j', m.get(0, 3));
        }

        @Test
        public void testRangeClosed_WithStep_Values_SteppedAlphabet() {
            CharMatrix m = CharMatrix.rangeClosed('a', 'i', 3);
            assertEquals(1, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('d', m.get(0, 1));
            assertEquals('g', m.get(0, 2));
        }

        @Test
        public void testRangeClosed_Values_InclusiveAlphabetEndpoint() {
            CharMatrix m = CharMatrix.rangeClosed('a', 'e');
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('e', m.get(0, 4));
        }

        @Test
        public void testRangeClosed_WithBy() {
            CharMatrix m = CharMatrix.rangeClosed('a', 'i', 2);
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('i', m.get(0, 4));
        }

        @Test
        public void testRangeClosed_SingleElement() {
            CharMatrix m = CharMatrix.rangeClosed('x', 'x');
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals('x', m.get(0, 0));
        }

        @Test
        public void testRangeClosed_SingleElement_Values() {
            CharMatrix m = CharMatrix.rangeClosed('z', 'z');
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals('z', m.get(0, 0));
        }
    }

    @Nested
    class OfMainDiagonalTests {

        @Test
        public void testDiagonalLU2RD() {
            char[] diagonal = { 'a', 'b', 'c' };
            CharMatrix matrix = CharMatrix.ofMainDiagonal(diagonal);
            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals('a', matrix.get(0, 0));
            Assertions.assertEquals('b', matrix.get(1, 1));
            Assertions.assertEquals('c', matrix.get(2, 2));
            Assertions.assertEquals((char) 0, matrix.get(0, 1));
        }

        @Test
        public void testDiagonalLU2RD_Values() {
            CharMatrix m = CharMatrix.ofMainDiagonal(new char[] { 'A', 'B', 'C' });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals('A', m.get(0, 0));
            assertEquals('B', m.get(1, 1));
            assertEquals('C', m.get(2, 2));
            assertEquals(0, m.get(0, 1));
            assertEquals(0, m.get(1, 0));
        }

        @Test
        public void testOfMainDiagonal_Empty() {
            CharMatrix m = CharMatrix.ofMainDiagonal(new char[0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testOfMainDiagonal_Values_ZeroFillsOffDiagonal() {
            CharMatrix m = CharMatrix.ofMainDiagonal(new char[] { 'a', 'b', 'c' });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(1, 1));
            assertEquals('c', m.get(2, 2));
            assertEquals('\0', m.get(0, 1));
            assertEquals('\0', m.get(1, 0));
        }

        @Test
        public void testDiagonalLU2RD_SingleElement() {
            CharMatrix m = CharMatrix.ofMainDiagonal(new char[] { 'x' });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals('x', m.get(0, 0));
        }
    }

    @Nested
    class OfAntiDiagonalTests {

        @Test
        public void testDiagonalRU2LD() {
            char[] diagonal = { 'a', 'b', 'c' };
            CharMatrix matrix = CharMatrix.ofAntiDiagonal(diagonal);
            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals('a', matrix.get(0, 2));
            Assertions.assertEquals('b', matrix.get(1, 1));
            Assertions.assertEquals('c', matrix.get(2, 0));
            Assertions.assertEquals((char) 0, matrix.get(0, 0));
        }

        @Test
        public void testDiagonalRU2LD_Values() {
            CharMatrix m = CharMatrix.ofAntiDiagonal(new char[] { 'X', 'Y', 'Z' });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals('X', m.get(0, 2));
            assertEquals('Y', m.get(1, 1));
            assertEquals('Z', m.get(2, 0));
            assertEquals(0, m.get(0, 0));
            assertEquals(0, m.get(2, 2));
        }

        @Test
        public void testOfAntiDiagonal_Values_ZeroFillsOffDiagonal() {
            CharMatrix m = CharMatrix.ofAntiDiagonal(new char[] { 'a', 'b', 'c' });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals('a', m.get(0, 2));
            assertEquals('b', m.get(1, 1));
            assertEquals('c', m.get(2, 0));
            assertEquals('\0', m.get(0, 0));
            assertEquals('\0', m.get(2, 2));
        }

        @Test
        public void testDiagonalRU2LD_Empty() {
            CharMatrix m = CharMatrix.ofAntiDiagonal(new char[0]);
            assertTrue(m.isEmpty());
        }
    }

    @Nested
    class OfDiagonalsTests {

        @Test
        public void testDiagonal() {
            char[] main = { 'a', 'b', 'c' };
            char[] anti = { 'x', 'y', 'z' };
            CharMatrix matrix = CharMatrix.ofDiagonals(main, anti);
            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals('a', matrix.get(0, 0));
            Assertions.assertEquals('b', matrix.get(1, 1));
            Assertions.assertEquals('c', matrix.get(2, 2));
            Assertions.assertEquals('x', matrix.get(0, 2));
            Assertions.assertEquals('b', matrix.get(1, 1));
            Assertions.assertEquals('z', matrix.get(2, 0));

            Assertions.assertThrows(IllegalArgumentException.class, () -> CharMatrix.ofDiagonals(null, null));

            Assertions.assertThrows(IllegalArgumentException.class, () -> CharMatrix.ofDiagonals(new char[] { 'a' }, new char[] { 'x', 'y' }));
        }

        @Test
        public void testDiagonal_WithBothDiagonals() {
            CharMatrix m = CharMatrix.ofDiagonals(new char[] { 'A', 'B', 'C' }, new char[] { 'X', 'Y', 'Z' });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals('A', m.get(0, 0));
            assertEquals('B', m.get(1, 1));
            assertEquals('C', m.get(2, 2));
            assertEquals('X', m.get(0, 2));
            assertEquals('Z', m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithOnlyMainDiagonal() {
            CharMatrix m = CharMatrix.ofDiagonals(new char[] { 'P', 'Q', 'R' }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals('P', m.get(0, 0));
            assertEquals('Q', m.get(1, 1));
            assertEquals('R', m.get(2, 2));
        }

        @Test
        public void testDiagonal_WithOnlyAntiDiagonal() {
            CharMatrix m = CharMatrix.ofDiagonals(null, new char[] { 'X', 'Y', 'Z' });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals('X', m.get(0, 2));
            assertEquals('Y', m.get(1, 1));
            assertEquals('Z', m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithDifferentLengths() {
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.ofDiagonals(new char[] { 'A', 'B' }, new char[] { 'X', 'Y', 'Z' }));
        }

        @Test
        public void testDiagonal_WithBothDiagonals_Values() {
            CharMatrix m = CharMatrix.ofDiagonals(new char[] { 'a', 'b', 'c' }, new char[] { 'x', 'y', 'z' });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(1, 1));
            assertEquals('c', m.get(2, 2));
            assertEquals('x', m.get(0, 2));
            assertEquals('z', m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithDifferentLengths_Validation() {
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.ofDiagonals(new char[] { 'a', 'b' }, new char[] { 'x', 'y', 'z' }));
        }

        @Test
        public void testDiagonal_WithOnlyMainDiagonal_NullInput() {
            CharMatrix m = CharMatrix.ofDiagonals(new char[] { 'a', 'b', 'c' }, null);
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(1, 1));
            assertEquals('c', m.get(2, 2));
        }

        @Test
        public void testDiagonal_WithOnlyAntiDiagonal_NullInput() {
            CharMatrix m = CharMatrix.ofDiagonals(null, new char[] { 'x', 'y', 'z' });
            assertEquals(3, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals('x', m.get(0, 2));
            assertEquals('y', m.get(1, 1));
            assertEquals('z', m.get(2, 0));
        }

        @Test
        public void testDiagonal_WithBothNull() {
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.ofDiagonals(null, null));
        }

        @Test
        public void testOfDiagonals_EmptyAndNullSidesPreserveEmptyShape() {
            assertTrue(CharMatrix.ofDiagonals(new char[0], new char[0]).isEmpty());
            assertTrue(CharMatrix.ofDiagonals(new char[0], null).isEmpty());
            assertTrue(CharMatrix.ofDiagonals(null, new char[0]).isEmpty());
        }
    }

    @Nested
    class UnboxTests {

        @Test
        public void testUnbox() {
            Character[][] boxed = { { 'a', 'b' }, { 'c', 'd' } };
            Matrix<Character> boxedMatrix = Matrix.wrap(boxed);
            CharMatrix unboxed = CharMatrix.unbox(boxedMatrix);
            Assertions.assertEquals(2, unboxed.rowCount());
            Assertions.assertEquals(2, unboxed.columnCount());
            Assertions.assertEquals('a', unboxed.get(0, 0));
            Assertions.assertEquals('d', unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_Values() {
            Character[][] boxed = { { 'A', 'B' }, { 'C', 'D' } };
            Matrix<Character> boxedMatrix = Matrix.wrap(boxed);
            CharMatrix unboxed = CharMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertEquals('A', unboxed.get(0, 0));
            assertEquals('D', unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_Values_DimensionsAndFirstRowValues() {
            Character[][] boxed = { { 'a', 'b' }, { 'c', 'd' } };
            Matrix<Character> boxedMatrix = Matrix.wrap(boxed);
            CharMatrix unboxed = CharMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertEquals('a', unboxed.get(0, 0));
            assertEquals('b', unboxed.get(0, 1));
        }

        @Test
        public void testUnbox_Shape2By2_NullInput() {
            Matrix<Character> boxed = Matrix.wrap(new Character[][] { { 'a', 'b' }, { null, 'd' } });
            CharMatrix primitive = CharMatrix.unbox(boxed);
            assertEquals('a', primitive.get(0, 0));
            assertEquals('b', primitive.get(0, 1));
            assertEquals('\0', primitive.get(1, 0)); // null becomes '\0'
            assertEquals('d', primitive.get(1, 1));
        }

        @Test
        public void testUnboxSharedEmptyAndPreserveZeroColumnRows() {
            CharMatrix sharedEmpty = CharMatrix.unbox(Matrix.empty(Character.class));
            Assertions.assertSame(CharMatrix.empty(), sharedEmpty);

            CharMatrix zeroColumns = CharMatrix.unbox(Matrix.wrap(new Character[][] { {}, {} }));
            Assertions.assertEquals(2, zeroColumns.rowCount());
            Assertions.assertEquals(0, zeroColumns.columnCount());

            Matrix<Character> typeNeutralZeroColumns = Matrix.empty(Character.class).resize(2, 0);
            CharMatrix unboxedTypeNeutralZeroColumns = CharMatrix.unbox(typeNeutralZeroColumns);
            Assertions.assertEquals(2, unboxedTypeNeutralZeroColumns.rowCount());
            Assertions.assertEquals(0, unboxedTypeNeutralZeroColumns.columnCount());

            Matrix<Character> typeNeutralValues = Matrix.empty(Character.class).resize(2, 2);
            typeNeutralValues.set(0, 1, 'x');
            CharMatrix unboxedTypeNeutralValues = CharMatrix.unbox(typeNeutralValues);
            Assertions.assertArrayEquals(new char[] { 0, 'x' }, unboxedTypeNeutralValues.rowCopy(0));
            Assertions.assertArrayEquals(new char[] { 0, 0 }, unboxedTypeNeutralValues.rowCopy(1));
        }

        @Test
        public void testUnbox_WithNullValues() {
            Character[][] boxed = { { 'A', null }, { null, 'D' } };
            Matrix<Character> boxedMatrix = Matrix.wrap(boxed);
            CharMatrix unboxed = CharMatrix.unbox(boxedMatrix);
            assertEquals('A', unboxed.get(0, 0));
            assertEquals(0, unboxed.get(0, 1)); // null -> 0
            assertEquals(0, unboxed.get(1, 0)); // null -> 0
            assertEquals('D', unboxed.get(1, 1));
        }

        @Test
        public void testUnbox_WithNullElements() {
            Character[][] boxed = { { 'a', null }, { null, 'd' } };
            Matrix<Character> boxedMatrix = Matrix.wrap(boxed);
            CharMatrix unboxed = CharMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals('a', unboxed.get(0, 0));
            assertEquals('\0', unboxed.get(0, 1));
            assertEquals('\0', unboxed.get(1, 0));
        }

        @Test
        public void testUnbox_IsAllNulls() {
            Character[][] boxed = { { null, null }, { null, null } };
            Matrix<Character> boxedMatrix = Matrix.wrap(boxed);
            CharMatrix unboxed = CharMatrix.unbox(boxedMatrix);
            assertEquals(2, unboxed.rowCount());
            assertEquals(2, unboxed.columnCount());
            assertEquals('\u0000', unboxed.get(0, 0));
            assertEquals('\u0000', unboxed.get(1, 1));
        }
    }

    @Nested
    class GetTests {

        @Test
        public void testGet() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);
            Assertions.assertEquals('a', matrix.get(0, 0));
            Assertions.assertEquals('b', matrix.get(0, 1));
            Assertions.assertEquals('c', matrix.get(1, 0));
            Assertions.assertEquals('d', matrix.get(1, 1));

            Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(2, 0));
            Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.get(0, 2));
        }

        @Test
        public void testGetWithPoint() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);
            Assertions.assertEquals('a', matrix.get(Point.of(0, 0)));
            Assertions.assertEquals('d', matrix.get(Point.of(1, 1)));
        }

        @Test
        public void testGet_Shape2By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            assertEquals('A', m.get(0, 0));
            assertEquals('E', m.get(1, 1));
            assertEquals('F', m.get(1, 2));
        }

        @Test
        public void testGetWithPoint_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertEquals('A', m.get(Point.of(0, 0)));
            assertEquals('D', m.get(Point.of(1, 1)));
            assertEquals('B', m.get(Point.of(0, 1)));
        }

        @Test
        public void testGet_ByIndices() {
            char[][] arr = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix m = CharMatrix.wrap(arr);
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('c', m.get(1, 0));
            assertEquals('d', m.get(1, 1));
        }

        @Test
        public void testGet_ByPoint() {
            char[][] arr = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix m = CharMatrix.wrap(arr);
            assertEquals('a', m.get(Point.of(0, 0)));
            assertEquals('b', m.get(Point.of(0, 1)));
            assertEquals('c', m.get(Point.of(1, 0)));
            assertEquals('d', m.get(Point.of(1, 1)));
        }

        @Test
        public void testGet_AllPositions() {
            char[][] arr = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix m = CharMatrix.wrap(arr);
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    assertEquals(arr[i][j], m.get(i, j));
                }
            }
        }

        @Test
        public void testGet_ByIndices_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('c', m.get(1, 0));
            assertEquals('d', m.get(1, 1));
        }

        @Test
        public void testGet_ByPoint_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertEquals('a', m.get(Point.of(0, 0)));
            assertEquals('d', m.get(Point.of(1, 1)));
        }

        @Test
        public void testGet_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
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
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);
            matrix.set(0, 0, 'x');
            Assertions.assertEquals('x', matrix.get(0, 0));

            Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.set(2, 0, 'y'));
        }

        @Test
        public void testSetWithPoint() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);
            matrix.set(Point.of(1, 1), 'x');
            Assertions.assertEquals('x', matrix.get(1, 1));
        }

        @Test
        public void testSet_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            m.set(0, 0, 'X');
            assertEquals('X', m.get(0, 0));

            m.set(1, 1, 'Y');
            assertEquals('Y', m.get(1, 1));
        }

        @Test
        public void testSetWithPoint_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            m.set(Point.of(0, 0), 'Z');
            assertEquals('Z', m.get(Point.of(0, 0)));
        }

        @Test
        public void testSet_ByIndices() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0' }, { '\0', '\0' } });
            m.set(0, 0, 'a');
            m.set(1, 1, 'd');
            assertEquals('a', m.get(0, 0));
            assertEquals('d', m.get(1, 1));
            assertEquals('\0', m.get(0, 1));
            assertEquals('\0', m.get(1, 0));
        }

        @Test
        public void testSet_ByPoint() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0' }, { '\0', '\0' } });
            m.set(Point.of(0, 0), 'a');
            m.set(Point.of(1, 1), 'd');
            assertEquals('a', m.get(Point.of(0, 0)));
            assertEquals('d', m.get(Point.of(1, 1)));
            assertEquals('\0', m.get(Point.of(0, 1)));
        }

        @Test
        public void testSet_ByIndices_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ' }, { ' ', ' ' } });
            m.set(0, 0, 'a');
            m.set(1, 1, 'd');
            assertEquals('a', m.get(0, 0));
            assertEquals('d', m.get(1, 1));
            assertEquals(' ', m.get(0, 1));
            assertEquals(' ', m.get(1, 0));
        }

        @Test
        public void testSet_ByPoint_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ' }, { ' ', ' ' } });
            m.set(Point.of(0, 0), 'a');
            m.set(Point.of(1, 1), 'd');
            assertEquals('a', m.get(Point.of(0, 0)));
            assertEquals('d', m.get(Point.of(1, 1)));
            assertEquals(' ', m.get(Point.of(0, 1)));
        }

        @Test
        public void testSet_OverwriteValue() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            m.set(0, 0, 'x');
            assertEquals('x', m.get(0, 0));
            m.set(0, 0, 'y');
            assertEquals('y', m.get(0, 0));
        }

        @Test
        public void testSet_SpecialCharacters() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ' } });
            m.set(0, 0, '\n');
            assertEquals('\n', m.get(0, 0));
        }

        @Test
        public void testSet_ByIndices_Shape2By2_ChangesFirstRowSecondColumn() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.set(0, 1, 'x');
            assertEquals('x', m.get(0, 1));
        }

        @Test
        public void testSet_ByPoint_Shape2By2_ChangesSecondRowFirstColumn() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.set(Point.of(1, 0), 'y');
            assertEquals('y', m.get(1, 0));
        }

        @Test
        public void testSet_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(-1, 0, 'X'));
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.set(2, 0, 'X'));
        }
    }

    @Nested
    class ValueAboveTests {

        @Test
        public void testValueAbove() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            OptionalChar up = matrix.valueAbove(1, 0);
            Assertions.assertTrue(up.isPresent());
            Assertions.assertEquals('a', up.get());

            OptionalChar empty = matrix.valueAbove(0, 0);
            Assertions.assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });

            OptionalChar up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals('A', up.get());

            // Top row has no element above
            OptionalChar empty = m.valueAbove(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueAbove_MultipleRows() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            OptionalChar up = m.valueAbove(2, 1);
            assertTrue(up.isPresent());
            assertEquals('d', up.get());
        }

        @Test
        public void testValueAbove_Exists() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            OptionalChar up = m.valueAbove(1, 0);
            assertTrue(up.isPresent());
            assertEquals('a', up.get());
        }

        @Test
        public void testValueAbove_NotExists() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            OptionalChar up = m.valueAbove(0, 0);
            assertFalse(up.isPresent());
        }
    }

    @Nested
    class ValueBelowTests {

        @Test
        public void testValueBelow() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            OptionalChar down = matrix.valueBelow(0, 0);
            Assertions.assertTrue(down.isPresent());
            Assertions.assertEquals('c', down.get());

            OptionalChar empty = matrix.valueBelow(1, 0);
            Assertions.assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });

            OptionalChar down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals('C', down.get());

            // Bottom row has no element below
            OptionalChar empty = m.valueBelow(1, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueBelow_AtBottomEdge() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            OptionalChar down = m.valueBelow(1, 0);
            assertFalse(down.isPresent());
        }

        @Test
        public void testValueBelow_MultipleRows() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            OptionalChar down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals('c', down.get());
        }

        @Test
        public void testValueBelow_Exists() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            OptionalChar down = m.valueBelow(0, 0);
            assertTrue(down.isPresent());
            assertEquals('c', down.get());
        }
    }

    @Nested
    class ValueLeftTests {

        @Test
        public void testValueLeft() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            OptionalChar left = matrix.valueLeft(0, 1);
            Assertions.assertTrue(left.isPresent());
            Assertions.assertEquals('a', left.get());

            OptionalChar empty = matrix.valueLeft(0, 0);
            Assertions.assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });

            OptionalChar left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals('A', left.get());

            // Leftmost column has no element to the left
            OptionalChar empty = m.valueLeft(0, 0);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueLeft_AtLeftEdge() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            OptionalChar left = m.valueLeft(0, 0);
            assertFalse(left.isPresent());
        }

        @Test
        public void testValueLeft_MultipleColumns() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' } });
            OptionalChar left = m.valueLeft(0, 2);
            assertTrue(left.isPresent());
            assertEquals('b', left.get());
        }

        @Test
        public void testValueLeft_Exists() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            OptionalChar left = m.valueLeft(0, 1);
            assertTrue(left.isPresent());
            assertEquals('a', left.get());
        }
    }

    @Nested
    class ValueRightTests {

        @Test
        public void testValueRight() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            OptionalChar right = matrix.valueRight(0, 0);
            Assertions.assertTrue(right.isPresent());
            Assertions.assertEquals('b', right.get());

            OptionalChar empty = matrix.valueRight(0, 1);
            Assertions.assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });

            OptionalChar right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals('B', right.get());

            // Rightmost column has no element to the right
            OptionalChar empty = m.valueRight(0, 1);
            assertFalse(empty.isPresent());
        }

        @Test
        public void testValueRight_AtRightEdge() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            OptionalChar right = m.valueRight(0, 1);
            assertFalse(right.isPresent());
        }

        @Test
        public void testValueRight_MultipleColumns() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' } });
            OptionalChar right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals('b', right.get());
        }

        @Test
        public void testValueRight_Exists() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            OptionalChar right = m.valueRight(0, 0);
            assertTrue(right.isPresent());
            assertEquals('b', right.get());
        }
    }

    @Nested
    class RowViewTests {

        @Test
        public void testRowView() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] row0 = matrix.rowView(0);
            Assertions.assertArrayEquals(new char[] { 'a', 'b', 'c' }, row0);

            char[] row1 = matrix.rowView(1);
            Assertions.assertArrayEquals(new char[] { 'd', 'e', 'f' }, row1);

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(-1));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowView(2));
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            assertArrayEquals(new char[] { 'A', 'B', 'C' }, m.rowView(0));
            assertArrayEquals(new char[] { 'D', 'E', 'F' }, m.rowView(1));
        }

        @Test
        public void testRowView_ArrayValues() {
            char[][] arr = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix m = CharMatrix.wrap(arr);
            char[] row0 = m.rowView(0);
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, row0);
            char[] row1 = m.rowView(1);
            assertArrayEquals(new char[] { 'd', 'e', 'f' }, row1);
        }

        @Test
        public void testRowView_Shape2By3_ArrayValues_FirstRowView() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            char[] row = m.rowView(0);
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, row);
        }

        @Test
        public void testRowView_SingleRow() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' } });
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, m.rowView(0));
        }

        @Test
        public void testRowView_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(2));
        }

        @Test
        public void testRowView_InvalidIndex() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(1));
        }

        @Test
        public void testRowView_InvalidIndex_Shape1By2_Validation() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(5));
        }
    }

    @Nested
    class RowCopyTests {

        @Test
        public void testRowCopy() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] rowCopy = matrix.rowCopy(0);
            Assertions.assertArrayEquals(new char[] { 'a', 'b', 'c' }, rowCopy);

            rowCopy[0] = 'z';
            Assertions.assertArrayEquals(new char[] { 'a', 'b', 'c' }, matrix.rowView(0));
        }

        @Test
        public void testRowCopy_InvalidIndex() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(-1));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowCopy(2));
        }

        @Test
        public void testAliasedRowsAreRejected() {
            char[] sharedRow = { 'a', 'b' };
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.wrap(new char[][] { sharedRow, sharedRow }));

            char[][] outer = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(outer);
            outer[0] = new char[] { 'x', 'y' };
            assertArrayEquals(new char[] { 'a', 'b' }, matrix.rowCopy(0));
        }
    }

    @Nested
    class ColumnCopyTests {

        @Test
        public void testColumnCopy() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] col0 = matrix.columnCopy(0);
            Assertions.assertArrayEquals(new char[] { 'a', 'd' }, col0);

            char[] col1 = matrix.columnCopy(1);
            Assertions.assertArrayEquals(new char[] { 'b', 'e' }, col1);

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(-1));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnCopy(3));
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            assertArrayEquals(new char[] { 'A', 'D' }, m.columnCopy(0));
            assertArrayEquals(new char[] { 'B', 'E' }, m.columnCopy(1));
            assertArrayEquals(new char[] { 'C', 'F' }, m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_ArrayValues() {
            char[][] arr = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix m = CharMatrix.wrap(arr);
            char[] col0 = m.columnCopy(0);
            assertArrayEquals(new char[] { 'a', 'd' }, col0);
            char[] col1 = m.columnCopy(1);
            assertArrayEquals(new char[] { 'b', 'e' }, col1);
        }

        @Test
        public void testColumnCopy_Shape2By3_ArrayValues_FirstColumnSnapshot() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            char[] col = m.columnCopy(0);
            assertArrayEquals(new char[] { 'a', 'd' }, col);
        }

        // matrix is now [['c', 'b', 'a'], ['f', 'e', 'd']]
        @Test
        public void testColumnCopy_SingleColumn() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a' }, { 'b' }, { 'c' } });
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, m.columnCopy(0));
        }

        @Test
        public void testColumnCopy_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
        }

        @Test
        public void testColumnCopy_InvalidIndex_Shape1By2_Validation() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(5));
        }
    }

    @Nested
    class SetRowTests {

        @Test
        public void testSetRow() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.setRow(0, new char[] { 'x', 'y', 'z' });
            Assertions.assertArrayEquals(new char[] { 'x', 'y', 'z' }, matrix.rowView(0));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setRow(0, new char[] { 'x', 'y' }));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setRow(0, (char[]) null));
        }

        @Test
        public void testSetRow_Shape2By2_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            m.setRow(0, new char[] { 'X', 'Y' });
            assertArrayEquals(new char[] { 'X', 'Y' }, m.rowView(0));
            assertArrayEquals(new char[] { 'C', 'D' }, m.rowView(1)); // unchanged
        }

        @Test
        public void testSetRow_WrongSize() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new char[] { 'A' }));
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new char[] { 'A', 'B', 'C' }));
        }

        @Test
        public void testSetRow_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0', '\0' }, { '\0', '\0', '\0' } });
            m.setRow(0, new char[] { 'a', 'b', 'c' });
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, m.rowView(0));
            assertArrayEquals(new char[] { '\0', '\0', '\0' }, m.rowView(1));
        }

        @Test
        public void testSetRow_Shape2By3_ArrayValues_PreservesOtherRow() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ', ' ' }, { ' ', ' ', ' ' } });
            m.setRow(0, new char[] { 'a', 'b', 'c' });
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, m.rowView(0));
            assertArrayEquals(new char[] { ' ', ' ', ' ' }, m.rowView(1));
        }

        @Test
        public void testSetRow_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.setRow(0, new char[] { 'x', 'y' });
            assertEquals('x', m.get(0, 0));
            assertEquals('y', m.get(0, 1));
        }

        @Test
        public void testSetRow_InvalidLength() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0', '\0' } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new char[] { 'a', 'b' }));
        }

        @Test
        public void testSetRow_InvalidLength_Shape1By3_Validation() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ', ' ' } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new char[] { 'a', 'b' }));
        }

        @Test
        public void testSetRow_InvalidIndex() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setRow(-1, new char[] { 'a', 'b' }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.setRow(1, new char[] { 'a', 'b' }));
        }

        @Test
        public void testSetRow_InvalidLength_Shape1By2_Validation() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            assertThrows(IllegalArgumentException.class, () -> m.setRow(0, new char[] { 'x' }));
        }
    }

    @Nested
    class SetColumnTests {

        @Test
        public void testSetColumn() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.setColumn(0, new char[] { 'x', 'y' });
            Assertions.assertArrayEquals(new char[] { 'x', 'y' }, matrix.columnCopy(0));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setColumn(0, new char[] { 'x' }));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setColumn(0, (char[]) null));
        }

        @Test
        public void testSetColumn_Shape2By2_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            m.setColumn(0, new char[] { 'X', 'Y' });
            assertArrayEquals(new char[] { 'X', 'Y' }, m.columnCopy(0));
            assertArrayEquals(new char[] { 'B', 'D' }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testSetColumn_WrongSize() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new char[] { 'A' }));
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new char[] { 'A', 'B', 'C' }));
        }

        @Test
        public void testSetColumn_Shape3By2_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0' }, { '\0', '\0' }, { '\0', '\0' } });
            m.setColumn(0, new char[] { 'a', 'b', 'c' });
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, m.columnCopy(0));
            assertArrayEquals(new char[] { '\0', '\0', '\0' }, m.columnCopy(1));
        }

        @Test
        public void testSetColumn_Shape3By2_ArrayValues_PreservesOtherColumn() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ' }, { ' ', ' ' }, { ' ', ' ' } });
            m.setColumn(0, new char[] { 'a', 'b', 'c' });
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, m.columnCopy(0));
            assertArrayEquals(new char[] { ' ', ' ', ' ' }, m.columnCopy(1));
        }

        @Test
        public void testSetColumn_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.setColumn(0, new char[] { 'x', 'y' });
            assertEquals('x', m.get(0, 0));
            assertEquals('y', m.get(1, 0));
        }

        @Test
        public void testAliasedArraySourcesAreSnapshotted() {
            CharMatrix columnMatrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            columnMatrix.setColumn(1, columnMatrix.rowView(0));
            assertArrayEquals(new char[] { 'c', 'b' }, columnMatrix.rowCopy(1));

            CharMatrix diagonalMatrix = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            diagonalMatrix.setAntiDiagonal(diagonalMatrix.rowView(0));
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, diagonalMatrix.antiDiagonalCopy());

            char[][] backing = { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } };
            CharMatrix fillMatrix = CharMatrix.wrap(backing);
            fillMatrix.copyFrom(1, 0, backing);
            assertArrayEquals(new char[] { 'c', 'd' }, fillMatrix.rowCopy(2));
        }

        @Test
        public void testSetColumn_InvalidLength() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0' }, { '\0', '\0' } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new char[] { 'a' }));
        }

        @Test
        public void testSetColumn_InvalidLength_Shape2By2_Validation() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ' }, { ' ', ' ' } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new char[] { 'a' }));
        }

        @Test
        public void testSetColumn_InvalidIndex() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ' }, { ' ', ' ' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(-1, new char[] { 'a', 'b' }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.setColumn(2, new char[] { 'a', 'b' }));
        }

        // Original unchanged
        @Test
        public void testSetColumn_InvalidLength_Shape2By2_Validation_RejectsShortColumn() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertThrows(IllegalArgumentException.class, () -> m.setColumn(0, new char[] { 'x' }));
        }
    }

    @Nested
    class UpdateRowTests {

        @Test
        public void testDocumentedCallbackFailuresKeepCompletedUpdates() throws Exception {
            for (int operation = 0; operation < 8; operation++) {
                final int selectedOperation = operation;
                final CharMatrix matrix = CharMatrix.wrap(new char[2][2]);
                final AtomicInteger calls = new AtomicInteger();
                final IllegalStateException failure = new IllegalStateException("callback failed");
                final Throwables.CharUnaryOperator<IllegalStateException> operator = value -> {
                    if (calls.incrementAndGet() == 2) {
                        throw failure;
                    }

                    return (char) 1;
                };

                assertSame(failure, assertThrows(IllegalStateException.class, () -> Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                    switch (selectedOperation) {
                        case 0 -> matrix.updateRow(0, operator);
                        case 1 -> matrix.updateColumn(0, operator);
                        case 2 -> matrix.updateMainDiagonal(operator);
                        case 3 -> matrix.updateAntiDiagonal(operator);
                        case 4 -> matrix.updateAll(operator);
                        case 5 -> matrix.updateAll((row, column) -> operator.applyAsChar((char) 0));
                        case 6 -> matrix.replaceIf(value -> operator.applyAsChar(value) != 0, (char) 1);
                        case 7 -> matrix.replaceIf((row, column) -> operator.applyAsChar((char) 0) != 0, (char) 1);
                        default -> throw new AssertionError(selectedOperation);
                    }
                })));

                assertEquals(2, calls.get(), "operation " + selectedOperation);
                assertArrayEquals(selectedOperation == 3 ? new char[] { (char) 0, (char) 1 } : new char[] { (char) 1, (char) 0 }, matrix.rowCopy(0),
                        "operation " + selectedOperation);
                assertArrayEquals(new char[] { (char) 0, (char) 0 }, matrix.rowCopy(1), "operation " + selectedOperation);
            }
        }

        @Test
        public void testUpdateRow() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.updateRow(0, c -> Character.toUpperCase(c));
            Assertions.assertArrayEquals(new char[] { 'A', 'B', 'C' }, matrix.rowView(0));
        }

        @Test
        public void testUpdateRow_Shape2By2_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            m.updateRow(0, x -> (char) (x + 1));
            assertArrayEquals(new char[] { 'B', 'C' }, m.rowView(0));
            assertArrayEquals(new char[] { 'C', 'D' }, m.rowView(1)); // unchanged
        }

        @Test
        public void testUpdateRow_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            m.updateRow(0, val -> (char) (val + 1));
            assertArrayEquals(new char[] { 'b', 'c', 'd' }, m.rowView(0));
            assertArrayEquals(new char[] { 'd', 'e', 'f' }, m.rowView(1));
        }

        @Test
        public void testUpdateRow_Shape2By3_ArrayValues_UppercaseFirstRow() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { ' ', ' ', ' ' } });
            m.updateRow(0, val -> Character.toUpperCase(val));
            assertArrayEquals(new char[] { 'A', 'B', 'C' }, m.rowView(0));
            assertArrayEquals(new char[] { ' ', ' ', ' ' }, m.rowView(1));
        }

        @Test
        public void testUpdateRow_MultipleRows() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            m.updateRow(1, val -> Character.toUpperCase(val));
            assertArrayEquals(new char[] { 'a', 'b' }, m.rowView(0));
            assertArrayEquals(new char[] { 'C', 'D' }, m.rowView(1));
            assertArrayEquals(new char[] { 'e', 'f' }, m.rowView(2));
        }

        @Test
        public void testUpdateRow_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.updateRow(0, val -> (char) (val + 1));
            assertEquals('b', m.get(0, 0));
            assertEquals('c', m.get(0, 1));
        }

        // original unchanged
        @Test
        public void testUpdateRow_UppercasesFirstRowOnly() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            matrix.updateRow(0, c -> Character.toUpperCase(c));
            // matrix is now [['A', 'B', 'C'], ['d', 'e', 'f']]
            assertArrayEquals(new char[] { 'A', 'B', 'C' }, matrix.rowView(0));
            assertArrayEquals(new char[] { 'd', 'e', 'f' }, matrix.rowView(1));
        }

        @Test
        public void testUpdateRowAndUpdateColumnNullOperator() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.updateRow(0, (Throwables.CharUnaryOperator<RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.updateColumn(0, (Throwables.CharUnaryOperator<RuntimeException>) null));
        }

        @Test
        public void testUpdateRowAndUpdateColumnInvalidIndex() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateRow(-1, c -> Character.toUpperCase(c)));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateColumn(3, c -> Character.toUpperCase(c)));
        }
    }

    @Nested
    class UpdateColumnTests {

        @Test
        public void testUpdateColumn() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.updateColumn(1, c -> Character.toUpperCase(c));
            Assertions.assertArrayEquals(new char[] { 'B', 'E' }, matrix.columnCopy(1));
        }

        @Test
        public void testUpdateColumn_Shape2By2_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            m.updateColumn(0, x -> (char) (x + 1));
            assertArrayEquals(new char[] { 'B', 'D' }, m.columnCopy(0));
            assertArrayEquals(new char[] { 'B', 'D' }, m.columnCopy(1)); // unchanged
        }

        @Test
        public void testUpdateColumn_Shape3By2_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            m.updateColumn(0, val -> (char) (val + 2));
            assertArrayEquals(new char[] { 'c', 'e', 'g' }, m.columnCopy(0));
            assertArrayEquals(new char[] { 'b', 'd', 'f' }, m.columnCopy(1));
        }

        @Test
        public void testUpdateColumn_Shape3By2_ArrayValues_UppercaseFirstColumn() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', ' ' }, { 'b', ' ' }, { 'c', ' ' } });
            m.updateColumn(0, val -> Character.toUpperCase(val));
            assertArrayEquals(new char[] { 'A', 'B', 'C' }, m.columnCopy(0));
            assertArrayEquals(new char[] { ' ', ' ', ' ' }, m.columnCopy(1));
        }

        @Test
        public void testUpdateColumn_MultipleColumns() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            m.updateColumn(2, val -> Character.toUpperCase(val));
            assertArrayEquals(new char[] { 'a', 'b', 'C' }, m.rowView(0));
            assertArrayEquals(new char[] { 'd', 'e', 'F' }, m.rowView(1));
        }

        @Test
        public void testUpdateColumn_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.updateColumn(0, val -> (char) (val + 1));
            assertEquals('b', m.get(0, 0));
            assertEquals('d', m.get(1, 0));
        }

        @Test
        public void testUpdateColumn_LowercasesSecondColumnOnly() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            matrix.updateColumn(1, c -> Character.toLowerCase(c));
            // matrix is now [['A', 'b'], ['C', 'd']]
            assertEquals('A', matrix.get(0, 0));
            assertEquals('b', matrix.get(0, 1));
            assertEquals('C', matrix.get(1, 0));
            assertEquals('d', matrix.get(1, 1));
        }
    }

    @Nested
    class MainDiagonalCopyTests {

        @Test
        public void testGetLU2RD() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] diagonal = matrix.mainDiagonalCopy();
            Assertions.assertArrayEquals(new char[] { 'a', 'e', 'i' }, diagonal);

            CharMatrix nonSquare = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            Assertions.assertArrayEquals(new char[] { 'a' }, nonSquare.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_Shape3By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            assertArrayEquals(new char[] { 'A', 'E', 'I' }, m.mainDiagonalCopy());
        }

        @Test
        public void testGetLU2RD_nonSquare() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            assertArrayEquals(new char[] { 'A' }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_NonSquare_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            assertArrayEquals(new char[] { 'a', 'e' }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            char[] diag = m.mainDiagonalCopy();
            assertArrayEquals(new char[] { 'a', 'e', 'i' }, diag);
        }

        @Test
        public void testMainDiagonalCopy_NonSquare() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            assertArrayEquals(new char[] { 'a', 'd' }, m.mainDiagonalCopy());
        }

        @Test
        public void testMainDiagonalCopy_SingleElement() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'x' } });
            assertArrayEquals(new char[] { 'x' }, m.mainDiagonalCopy());
        }
    }

    @Nested
    class SetMainDiagonalTests {

        @Test
        public void testSetLU2RD() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.setMainDiagonal(new char[] { 'x', 'y', 'z' });
            Assertions.assertEquals('x', matrix.get(0, 0));
            Assertions.assertEquals('y', matrix.get(1, 1));
            Assertions.assertEquals('z', matrix.get(2, 2));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.setMainDiagonal(new char[] { 'x', 'y' }));
        }

        @Test
        public void testSetMainDiagonal_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            m.setMainDiagonal(new char[] { 'X', 'Y', 'Z' });
            assertEquals('X', m.get(0, 0));
            assertEquals('Y', m.get(1, 1));
            assertEquals('Z', m.get(2, 2));
        }

        @Test
        public void testSetMainDiagonal_NonSquare() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            m.setMainDiagonal(new char[] { 'X' });
            assertEquals('X', m.get(0, 0));
        }

        @Test
        public void testSetMainDiagonal_ArrayTooShort() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new char[] { 'X', 'Y' }));
        }

        @Test
        public void testSetMainDiagonal_Shape3By3_PreservesOffDiagonalValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0', '\0' }, { '\0', '\0', '\0' }, { '\0', '\0', '\0' } });
            m.setMainDiagonal(new char[] { 'a', 'b', 'c' });
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(1, 1));
            assertEquals('c', m.get(2, 2));
            assertEquals('\0', m.get(0, 1));
        }

        @Test
        public void testSetMainDiagonal_AbcValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ', ' ' }, { ' ', ' ', ' ' }, { ' ', ' ', ' ' } });
            m.setMainDiagonal(new char[] { 'a', 'b', 'c' });
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(1, 1));
            assertEquals('c', m.get(2, 2));
            assertEquals(' ', m.get(0, 1));
        }

        @Test
        public void testSetMainDiagonal_NonSquare_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[2][3]);
            m.setMainDiagonal(new char[] { 'a', 'b' });
            assertArrayEquals(new char[] { 'a', 'b' }, m.mainDiagonalCopy());
        }

        @Test
        public void testSetMainDiagonal() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            m.setMainDiagonal(new char[] { 'x', 'y', 'z' });
            assertEquals('x', m.get(0, 0));
            assertEquals('y', m.get(1, 1));
            assertEquals('z', m.get(2, 2));
        }

        @Test
        public void testSetDiagonal_NullArraysOnEmptyMatrixThrow() {
            CharMatrix m = CharMatrix.empty();
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(null));
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(null));
        }

        @Test
        public void testSetLU2RD_invalidLength() {
            CharMatrix m = CharMatrix.wrap(new char[3][3]);
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new char[] { 'a', 'b' }));
        }

        @Test
        public void testSetMainDiagonal_InvalidLength() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertThrows(IllegalArgumentException.class, () -> m.setMainDiagonal(new char[] { 'x' }));
        }
    }

    @Nested
    class UpdateMainDiagonalTests {

        @Test
        public void testUpdateLU2RD() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.updateMainDiagonal(c -> Character.toUpperCase(c));
            Assertions.assertEquals('A', matrix.get(0, 0));
            Assertions.assertEquals('E', matrix.get(1, 1));
            Assertions.assertEquals('I', matrix.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            m.updateMainDiagonal(x -> (char) (x + 1));
            assertEquals('B', m.get(0, 0));
            assertEquals('F', m.get(1, 1));
            assertEquals('J', m.get(2, 2));
            assertEquals('B', m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateMainDiagonal_NonSquare() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            m.updateMainDiagonal(x -> (char) (x + 1));
            assertEquals('B', m.get(0, 0));
        }

        @Test
        public void testUpdateMainDiagonal_Shape3By3_IncrementsEveryDiagonalElement() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', '\0', '\0' }, { '\0', 'b', '\0' }, { '\0', '\0', 'c' } });
            m.updateMainDiagonal(val -> (char) (val + 1));
            assertEquals('b', m.get(0, 0));
            assertEquals('c', m.get(1, 1));
            assertEquals('d', m.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal_UppercasesValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', ' ', ' ' }, { ' ', 'b', ' ' }, { ' ', ' ', 'c' } });
            m.updateMainDiagonal(val -> Character.toUpperCase(val));
            assertEquals('A', m.get(0, 0));
            assertEquals('B', m.get(1, 1));
            assertEquals('C', m.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal_AllValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', 'x', ' ' }, { 'x', ' ', 'x' }, { ' ', 'x', ' ' } });
            m.updateMainDiagonal(val -> 'z');
            assertEquals('z', m.get(0, 0));
            assertEquals('z', m.get(1, 1));
            assertEquals('z', m.get(2, 2));
        }

        @Test
        public void testUpdateMainDiagonal() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            m.updateMainDiagonal(val -> (char) (val + 1));
            assertEquals('b', m.get(0, 0));
            assertEquals('f', m.get(1, 1));
            assertEquals('j', m.get(2, 2));
        }
    }

    @Nested
    class AntiDiagonalCopyTests {

        @Test
        public void testGetRU2LD() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] diagonal = matrix.antiDiagonalCopy();
            Assertions.assertArrayEquals(new char[] { 'c', 'e', 'g' }, diagonal);
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            assertArrayEquals(new char[] { 'C', 'E', 'G' }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            assertArrayEquals(new char[] { 'B' }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy_NonSquare_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            assertArrayEquals(new char[] { 'c', 'e' }, m.antiDiagonalCopy());
        }

        @Test
        public void testAntiDiagonalCopy() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            char[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new char[] { 'c', 'e', 'g' }, diag);
        }

        @Test
        public void testAntiDiagonalCopy_Shape3By3_ArrayValues_NonzeroAntiDiagonal() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ', 'a' }, { ' ', 'b', ' ' }, { 'c', ' ', ' ' } });
            char[] diag = m.antiDiagonalCopy();
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, diag);
        }
    }

    @Nested
    class SetAntiDiagonalTests {

        @Test
        public void testSetRU2LD() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.setAntiDiagonal(new char[] { 'x', 'y', 'z' });
            Assertions.assertEquals('x', matrix.get(0, 2));
            Assertions.assertEquals('y', matrix.get(1, 1));
            Assertions.assertEquals('z', matrix.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            m.setAntiDiagonal(new char[] { 'X', 'Y', 'Z' });
            assertEquals('X', m.get(0, 2));
            assertEquals('Y', m.get(1, 1));
            assertEquals('Z', m.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_NonSquare() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            m.setAntiDiagonal(new char[] { 'X' });
            assertEquals('X', m.get(0, 1));
        }

        @Test
        public void testSetAntiDiagonal_ArrayTooShort() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new char[] { 'X', 'Y' }));
        }

        @Test
        public void testSetAntiDiagonal_Shape3By3_PreservesOffDiagonalValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0', '\0' }, { '\0', '\0', '\0' }, { '\0', '\0', '\0' } });
            m.setAntiDiagonal(new char[] { 'a', 'b', 'c' });
            assertEquals('a', m.get(0, 2));
            assertEquals('b', m.get(1, 1));
            assertEquals('c', m.get(2, 0));
            assertEquals('\0', m.get(0, 0));
        }

        @Test
        public void testSetAntiDiagonal_AbcValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ', ' ' }, { ' ', ' ', ' ' }, { ' ', ' ', ' ' } });
            m.setAntiDiagonal(new char[] { 'a', 'b', 'c' });
            assertEquals('a', m.get(0, 2));
            assertEquals('b', m.get(1, 1));
            assertEquals('c', m.get(2, 0));
            assertEquals(' ', m.get(0, 0));
        }

        @Test
        public void testSetAntiDiagonal() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            m.setAntiDiagonal(new char[] { 'x', 'y', 'z' });
            assertEquals('x', m.get(0, 2));
            assertEquals('y', m.get(1, 1));
            assertEquals('z', m.get(2, 0));
        }

        @Test
        public void testSetAntiDiagonal_InvalidLength() {
            CharMatrix m = CharMatrix.wrap(new char[3][3]);
            assertThrows(IllegalArgumentException.class, () -> m.setAntiDiagonal(new char[] { 'a', 'b' }));
        }
    }

    @Nested
    class UpdateAntiDiagonalTests {

        @Test
        public void testUpdateRU2LD() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.updateAntiDiagonal(c -> Character.toUpperCase(c));
            Assertions.assertEquals('C', matrix.get(0, 2));
            Assertions.assertEquals('E', matrix.get(1, 1));
            Assertions.assertEquals('G', matrix.get(2, 0));
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            m.updateAntiDiagonal(x -> (char) (x + 1));
            assertEquals('D', m.get(0, 2));
            assertEquals('F', m.get(1, 1));
            assertEquals('H', m.get(2, 0));
            assertEquals('B', m.get(0, 1)); // unchanged
        }

        @Test
        public void testUpdateAntiDiagonal_NonSquare() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            m.updateAntiDiagonal(x -> (char) (x + 1));
            assertEquals('C', m.get(0, 1));
        }

        @Test
        public void testUpdateAntiDiagonal_Shape3By3_IncrementsEveryDiagonalElement() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0', 'a' }, { '\0', 'b', '\0' }, { 'c', '\0', '\0' } });
            m.updateAntiDiagonal(val -> (char) (val + 1));
            assertEquals('b', m.get(0, 2));
            assertEquals('c', m.get(1, 1));
            assertEquals('d', m.get(2, 0));
        }

        @Test
        public void testUpdateAntiDiagonal_UppercasesValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ', 'a' }, { ' ', 'b', ' ' }, { 'c', ' ', ' ' } });
            m.updateAntiDiagonal(val -> Character.toUpperCase(val));
            assertEquals('A', m.get(0, 2));
            assertEquals('B', m.get(1, 1));
            assertEquals('C', m.get(2, 0));
        }

        @Test
        public void testUpdateAntiDiagonal_Rectangular() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ', 'a' }, { 'b', ' ', ' ' } });
            m.updateAntiDiagonal(val -> Character.toUpperCase(val));
            assertArrayEquals(new char[] { 'A', ' ' }, m.antiDiagonalCopy());
        }

        @Test
        public void testUpdateAntiDiagonal() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            m.updateAntiDiagonal(val -> (char) (val + 1));
            assertEquals('d', m.get(0, 2));
            assertEquals('f', m.get(1, 1));
            assertEquals('h', m.get(2, 0));
        }
    }

    @Nested
    class UpdateAllTests {

        @Test
        public void testUpdateAll() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.updateAll(c -> Character.toUpperCase(c));
            Assertions.assertEquals('A', matrix.get(0, 0));
            Assertions.assertEquals('B', matrix.get(0, 1));
            Assertions.assertEquals('C', matrix.get(1, 0));
            Assertions.assertEquals('D', matrix.get(1, 1));
        }

        @Test
        public void testUpdateAllUnarySequentialTallMatrixUsesRowMajorOrder() throws Exception {
            final CharMatrix matrix = CharMatrix.wrap(new char[3][2]);
            final AtomicInteger next = new AtomicInteger();

            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> matrix.updateAll(x -> (char) next.incrementAndGet()));

            assertArrayEquals(new char[] { 1, 2 }, matrix.rowCopy(0));
            assertArrayEquals(new char[] { 3, 4 }, matrix.rowCopy(1));
            assertArrayEquals(new char[] { 5, 6 }, matrix.rowCopy(2));
        }

        @Test
        public void testUpdateAllWithIndices() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.updateAll((i, j) -> (char) ('0' + i * 2 + j));
            Assertions.assertEquals('0', matrix.get(0, 0));
            Assertions.assertEquals('1', matrix.get(0, 1));
            Assertions.assertEquals('2', matrix.get(1, 0));
            Assertions.assertEquals('3', matrix.get(1, 1));
        }

        @Test
        public void testUpdateAll_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            m.updateAll(x -> (char) (x + 1));
            assertEquals('B', m.get(0, 0));
            assertEquals('C', m.get(0, 1));
            assertEquals('D', m.get(1, 0));
            assertEquals('E', m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithIndices() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'A' }, { 'A', 'A' } });
            m.updateAll((i, j) -> (char) ('A' + i * 10 + j));
            assertEquals('A', m.get(0, 0));
            assertEquals('B', m.get(0, 1));
            assertEquals('K', m.get(1, 0));
            assertEquals('L', m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.updateAll((i, j) -> (char) ('0' + i + j));
            assertEquals('0', m.get(0, 0));
            assertEquals('1', m.get(0, 1));
            assertEquals('1', m.get(1, 0));
            assertEquals('2', m.get(1, 1));
        }

        @Test
        public void testUpdateAll_Unary() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.updateAll(val -> Character.toUpperCase(val));
            assertEquals('A', m.get(0, 0));
            assertEquals('B', m.get(0, 1));
            assertEquals('C', m.get(1, 0));
            assertEquals('D', m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.updateAll((i, j) -> i == j ? 'x' : 'o');
            assertEquals('x', m.get(0, 0));
            assertEquals('o', m.get(0, 1));
            assertEquals('o', m.get(1, 0));
            assertEquals('x', m.get(1, 1));
        }

        @Test
        public void testUpdateAll_WithConstant() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.updateAll(val -> '*');
            assertEquals('*', m.get(0, 0));
            assertEquals('*', m.get(0, 1));
            assertEquals('*', m.get(1, 0));
            assertEquals('*', m.get(1, 1));
        }

        @Test
        public void testUpdateAll_UnaryOperator() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.updateAll(val -> (char) (val + 1));
            assertEquals('b', m.get(0, 0));
            assertEquals('c', m.get(0, 1));
            assertEquals('d', m.get(1, 0));
            assertEquals('e', m.get(1, 1));
        }

        @Test
        public void testUpdateAll_BiFunction_Shape2By2_GeneratesEveryCellFromCoordinates() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0' }, { '\0', '\0' } });
            m.updateAll((i, j) -> (char) ('a' + i + j));
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('b', m.get(1, 0));
            assertEquals('c', m.get(1, 1));
        }

        @Test
        public void testUpdateAll_AssignsRowMajorAlphabetPositions() {
            CharMatrix matrix = CharMatrix.wrap(new char[3][4]);
            matrix.updateAll((i, j) -> (char) ('a' + i * 4 + j));
            // Result: [['a', 'b', 'c', 'd'], ['e', 'f', 'g', 'h'], ['i', 'j', 'k', 'l']]
            assertEquals('a', matrix.get(0, 0));
            assertEquals('d', matrix.get(0, 3));
            assertEquals('e', matrix.get(1, 0));
            assertEquals('h', matrix.get(1, 3));
            assertEquals('i', matrix.get(2, 0));
            assertEquals('l', matrix.get(2, 3));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testUpdateAll_ExplicitModesTransformEveryValue(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 } });
                matrix.updateAll(value -> (char) (value + 10));
                assertEquals(CharMatrix.wrap(new char[][] { { (char) 11, (char) 12, (char) 13 }, { (char) 14, (char) 15, (char) 16 } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testCharMatrixUpdateAllNullOperator() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix emptyLike = CharMatrix.wrap(new char[][] { {}, {} });
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.CharUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAll((Throwables.IntBiFunction<Character, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateMainDiagonal((Throwables.CharUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.updateAntiDiagonal((Throwables.CharUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.CharPredicate<RuntimeException>) null, 'x'));
            assertThrows(IllegalArgumentException.class, () -> matrix.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 'x'));

            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateMainDiagonal((Throwables.CharUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.updateAntiDiagonal((Throwables.CharUnaryOperator<RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.CharPredicate<RuntimeException>) null, 'x'));
            assertThrows(IllegalArgumentException.class, () -> emptyLike.replaceIf((Throwables.IntBiPredicate<RuntimeException>) null, 'x'));
        }

        @Test
        public void testUpdateAll_ZeroColumnsDoNotInvokeOperator() {
            final var matrix = CharMatrix.wrap(new char[2][0]);
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
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.replaceIf(c -> c < 'c', 'x');
            Assertions.assertEquals('x', matrix.get(0, 0));
            Assertions.assertEquals('x', matrix.get(0, 1));
            Assertions.assertEquals('c', matrix.get(1, 0));
            Assertions.assertEquals('d', matrix.get(1, 1));
        }

        @Test
        public void testReplaceIfWithIndices() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.replaceIf((i, j) -> i == j, 'x');
            Assertions.assertEquals('x', matrix.get(0, 0));
            Assertions.assertEquals('b', matrix.get(0, 1));
            Assertions.assertEquals('c', matrix.get(1, 0));
            Assertions.assertEquals('x', matrix.get(1, 1));
        }

        @Test
        public void testReplaceIf_Shape2By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            m.replaceIf(x -> x > 'C', '?');
            assertEquals('A', m.get(0, 0));
            assertEquals('B', m.get(0, 1));
            assertEquals('C', m.get(0, 2));
            assertEquals('?', m.get(1, 0)); // was D
            assertEquals('?', m.get(1, 1)); // was E
            assertEquals('?', m.get(1, 2)); // was F
        }

        @Test
        public void testReplaceIf_WithIndices() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            m.replaceIf((i, j) -> i == j, 'X'); // Replace diagonal
            assertEquals('X', m.get(0, 0));
            assertEquals('X', m.get(1, 1));
            assertEquals('X', m.get(2, 2));
            assertEquals('B', m.get(0, 1)); // unchanged
        }

        @Test
        public void testReplaceIf_Predicate() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.replaceIf(val -> val > 'b', 'x');
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('x', m.get(1, 0));
            assertEquals('x', m.get(1, 1));
        }

        @Test
        public void testReplaceIf_BiPredicate() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.replaceIf((i, j) -> i == j, 'x');
            assertEquals('x', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('c', m.get(1, 0));
            assertEquals('x', m.get(1, 1));
        }

        @Test
        public void testReplaceIf_Predicate_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'B' }, { 'c', 'D' } });
            m.replaceIf(val -> Character.isUpperCase(val), '*');
            assertEquals('a', m.get(0, 0));
            assertEquals('*', m.get(0, 1));
            assertEquals('c', m.get(1, 0));
            assertEquals('*', m.get(1, 1));
        }

        @Test
        public void testReplaceIf_BiPredicate_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.replaceIf((i, j) -> i == j, 'X');
            assertEquals('X', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('c', m.get(1, 0));
            assertEquals('X', m.get(1, 1));
        }

        @Test
        public void testReplaceIf_NoMatches() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.replaceIf(val -> Character.isDigit(val), 'X');
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('c', m.get(1, 0));
            assertEquals('d', m.get(1, 1));
        }

        @Test
        public void testReplaceIf_Predicate_Shape2By2_ReplacesMatchingCharacters() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.replaceIf(val -> val < 'c', 'x');
            assertEquals('x', m.get(0, 0));
            assertEquals('x', m.get(0, 1));
            assertEquals('c', m.get(1, 0));
            assertEquals('d', m.get(1, 1));
        }

        @Test
        public void testReplaceIf_BiPredicate_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[3][3]);
            m.replaceIf((i, j) -> i == j, 'x');
            assertEquals('x', m.get(0, 0));
            assertEquals('\0', m.get(0, 1));
            assertEquals('x', m.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testReplaceIf_ExplicitModesReplaceOnlyMatchingValues(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 } });
                matrix.replaceIf(value -> value % 2 == 0, (char) 9);
                assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 9, (char) 3 }, { (char) 9, (char) 5, (char) 9 } }), matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }
    }

    @Nested
    class MapTests {

        @Test
        public void testMap() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix upper = matrix.map(c -> Character.toUpperCase(c));
            Assertions.assertEquals('A', upper.get(0, 0));
            Assertions.assertEquals('B', upper.get(0, 1));
            Assertions.assertEquals('C', upper.get(1, 0));
            Assertions.assertEquals('D', upper.get(1, 1));
        }

        @Test
        public void testMap_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix result = m.map(x -> (char) (x + 1));
            assertEquals('B', result.get(0, 0));
            assertEquals('C', result.get(0, 1));
            assertEquals('D', result.get(1, 0));
            assertEquals('E', result.get(1, 1));

            // Original unchanged
            assertEquals('A', m.get(0, 0));
        }

        @Test
        public void testMap_Shape2By2_IncrementsAllCellsAndPreservesSource() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix mapped = m.map(val -> (char) (val + 1));
            assertEquals('b', mapped.get(0, 0));
            assertEquals('c', mapped.get(0, 1));
            assertEquals('d', mapped.get(1, 0));
            assertEquals('e', mapped.get(1, 1));
            assertEquals('a', m.get(0, 0));
        }

        @Test
        public void testMap_UppercasesValuesAndPreservesSource() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix mapped = m.map(val -> Character.toUpperCase(val));
            assertEquals('A', mapped.get(0, 0));
            assertEquals('B', mapped.get(0, 1));
            assertEquals('C', mapped.get(1, 0));
            assertEquals('D', mapped.get(1, 1));
            assertEquals('a', m.get(0, 0));
        }

        @Test
        public void testMap_Identity() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix mapped = m.map(val -> val);
            assertEquals('a', mapped.get(0, 0));
            assertEquals('b', mapped.get(0, 1));
            assertNotSame(m, mapped);
        }

        @Test
        public void testMapNullMapper() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.map((Throwables.CharUnaryOperator<RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> matrix.mapToObj((Throwables.CharFunction<String, RuntimeException>) null, String.class));
        }

        @Test
        public void testCharMatrixRowsForZeroColumnMatrix() {
            final CharMatrix matrix = CharMatrix.wrap(new char[][] { {}, {}, {} });
            final List<char[]> rows = matrix.rowStreams().map(CharStream::toArray).toList();

            assertEquals(3, rows.size());
            assertArrayEquals(new char[0], rows.get(0));
            assertArrayEquals(new char[0], rows.get(1));
            assertArrayEquals(new char[0], rows.get(2));
        }
    }

    @Nested
    class MapToObjTests {

        @Test
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void testObjectMappingRuntimeArrayTypeAndStoreFailure() {
            final CharMatrix source = CharMatrix.wrap(new char[] { 'a', 'a' });
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
            assertArrayEquals(new char[] { 'a', 'a' }, source.rowCopy(0));
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
            final CharMatrix empty = new CharMatrix(new char[0][], 3);
            final Matrix<Object> mapped = empty.mapToObj(value -> null, largestElementType);
            assertEquals(0, mapped.rowCount());
            assertEquals(3, mapped.columnCount());
            assertEquals(largestElementType, mapped.elementType());

            final AtomicInteger calls = new AtomicInteger();
            assertThrows(IllegalArgumentException.class, () -> empty.mapToObj(value -> {
                calls.incrementAndGet();
                return null;
            }, tooDeep));
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.wrap(new char[] { 'a' }).mapToObj(value -> {
                calls.incrementAndGet();
                return null;
            }, tooDeep));
            assertEquals(0, calls.get());
        }

        @Test
        public void testMapToObj() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            Matrix<String> stringMatrix = matrix.mapToObj(c -> String.valueOf(c), String.class);
            Assertions.assertEquals("a", stringMatrix.get(0, 0));
            Assertions.assertEquals("d", stringMatrix.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            Matrix<String> result = m.mapToObj(x -> "char:" + x, String.class);
            assertEquals("char:A", result.get(0, 0));
            assertEquals("char:D", result.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_ConvertsEveryCharacterToString() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            Matrix<String> mapped = m.mapToObj(val -> String.valueOf(val), String.class);
            assertEquals("a", mapped.get(0, 0));
            assertEquals("b", mapped.get(0, 1));
            assertEquals("c", mapped.get(1, 0));
            assertEquals("d", mapped.get(1, 1));
        }

        @Test
        public void testMapToObj_WithComplexType() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            Matrix<Integer> mapped = m.mapToObj(val -> (int) val, Integer.class);
            assertEquals(97, mapped.get(0, 0));
            assertEquals(98, mapped.get(0, 1));
            assertEquals(99, mapped.get(1, 0));
            assertEquals(100, mapped.get(1, 1));
        }

        @Test
        public void testMapToObj_Shape2By2_AddsStringPrefixToFirstRow() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            Matrix<String> stringMatrix = m.mapToObj(val -> "C" + val, String.class);
            assertEquals("Ca", stringMatrix.get(0, 0));
            assertEquals("Cb", stringMatrix.get(0, 1));
        }
    }

    @Nested
    class FillTests {

        @Test
        public void testFill() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.fill('x');
            Assertions.assertEquals('x', matrix.get(0, 0));
            Assertions.assertEquals('x', matrix.get(0, 1));
            Assertions.assertEquals('x', matrix.get(1, 0));
            Assertions.assertEquals('x', matrix.get(1, 1));
        }

        @Test
        public void testFill_WithValue() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            m.fill('Z');
            for (int i = 0; i < m.rowCount(); i++) {
                for (int j = 0; j < m.columnCount(); j++) {
                    assertEquals('Z', m.get(i, j));
                }
            }
        }

        @Test
        public void testFill_SingleValue() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0' }, { '\0', '\0' } });
            m.fill('x');
            assertEquals('x', m.get(0, 0));
            assertEquals('x', m.get(0, 1));
            assertEquals('x', m.get(1, 0));
            assertEquals('x', m.get(1, 1));
        }

        @Test
        public void testFill_SingleValue_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ' }, { ' ', ' ' } });
            m.fill('x');
            assertEquals('x', m.get(0, 0));
            assertEquals('x', m.get(0, 1));
            assertEquals('x', m.get(1, 0));
            assertEquals('x', m.get(1, 1));
        }

        @Test
        public void testFill_SingleValue_Shape2By2_FillsEveryCell() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.fill('x');
            assertEquals('x', m.get(0, 0));
            assertEquals('x', m.get(0, 1));
            assertEquals('x', m.get(1, 0));
            assertEquals('x', m.get(1, 1));
        }
    }

    @Nested
    class CopyFromTests {

        @Test
        public void testCopyFrom() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[][] b = { { 'x', 'y' }, { 'z', 'w' } };
            matrix.copyFrom(b);
            Assertions.assertEquals('x', matrix.get(0, 0));
            Assertions.assertEquals('y', matrix.get(0, 1));
            Assertions.assertEquals('z', matrix.get(1, 0));
            Assertions.assertEquals('w', matrix.get(1, 1));
            Assertions.assertEquals('i', matrix.get(2, 2)); // unchanged
        }

        @Test
        public void testCopyFromWithIndices() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[][] b = { { 'x', 'y' } };
            matrix.copyFrom(1, 1, b);
            Assertions.assertEquals('a', matrix.get(0, 0)); // unchanged
            Assertions.assertEquals('x', matrix.get(1, 1));
            Assertions.assertEquals('y', matrix.get(1, 2));

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(-1, 0, b));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(0, -1, b));
        }

        @Test
        public void testCopyFrom_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'A', 'A' }, { 'A', 'A', 'A' }, { 'A', 'A', 'A' } });
            char[][] patch = { { 'X', 'Y' }, { 'Z', 'W' } };
            m.copyFrom(patch);
            assertEquals('X', m.get(0, 0));
            assertEquals('Y', m.get(0, 1));
            assertEquals('Z', m.get(1, 0));
            assertEquals('W', m.get(1, 1));
            assertEquals('A', m.get(2, 2)); // unchanged
        }

        @Test
        public void testCopyFromAtPosition() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'A', 'A' }, { 'A', 'A', 'A' }, { 'A', 'A', 'A' } });
            char[][] patch = { { 'X', 'Y' }, { 'Z', 'W' } };
            m.copyFrom(1, 1, patch);
            assertEquals('A', m.get(0, 0)); // unchanged
            assertEquals('X', m.get(1, 1));
            assertEquals('Y', m.get(1, 2));
            assertEquals('Z', m.get(2, 1));
            assertEquals('W', m.get(2, 2));
        }

        @Test
        public void testCopyFrom_WithOffset() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0', '\0' }, { '\0', '\0', '\0' }, { '\0', '\0', '\0' } });
            m.copyFrom(1, 1, new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertEquals('\0', m.get(0, 0));
            assertEquals('a', m.get(1, 1));
            assertEquals('d', m.get(2, 2));
        }

        @Test
        public void testCopyFrom_Shape2By2_CopiesCornerValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ' }, { ' ', ' ' } });
            m.copyFrom(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertEquals('a', m.get(0, 0));
            assertEquals('d', m.get(1, 1));
        }

        @Test
        public void testCopyFrom_WithOffset_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ', ' ' }, { ' ', ' ', ' ' }, { ' ', ' ', ' ' } });
            m.copyFrom(1, 1, new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertEquals(' ', m.get(0, 0));
            assertEquals('a', m.get(1, 1));
            assertEquals('d', m.get(2, 2));
        }

        @Test
        public void testCopyFrom_WithPartialArray() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { ' ', ' ', ' ' }, { ' ', ' ', ' ' }, { ' ', ' ', ' ' } });
            m.copyFrom(0, 0, new char[][] { { 'a', 'b' } });
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals(' ', m.get(0, 2));
            assertEquals(' ', m.get(1, 0));
        }

        @Test
        public void testCopyFrom_Shape2By2_PreservesCellsOutsideCopiedRegion() {
            CharMatrix m = CharMatrix.wrap(new char[3][3]);
            m.copyFrom(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('c', m.get(1, 0));
            assertEquals('d', m.get(1, 1));
            assertEquals('\0', m.get(2, 2));
        }

        @Test
        public void testCopyFromWithPosition() {
            CharMatrix m = CharMatrix.wrap(new char[4][4]);
            m.copyFrom(1, 1, new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertEquals('\0', m.get(0, 0));
            assertEquals('a', m.get(1, 1));
            assertEquals('b', m.get(1, 2));
            assertEquals('c', m.get(2, 1));
            assertEquals('d', m.get(2, 2));
        }

        @Test
        public void testCopyFrom_OffsetCopyPreservesSurroundingCells() {
            CharMatrix matrix = CharMatrix.wrap(new char[3][3]);
            matrix.copyFrom(1, 1, new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            // Result: [[0, 0, 0], [0, 'a', 'b'], [0, 'c', 'd']]
            assertEquals('\0', matrix.get(0, 0));
            assertEquals('\0', matrix.get(1, 0));
            assertEquals('a', matrix.get(1, 1));
            assertEquals('b', matrix.get(1, 2));
            assertEquals('c', matrix.get(2, 1));
            assertEquals('d', matrix.get(2, 2));
        }

        @Test
        public void testCopyFrom_FullOverwrite() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0' }, { '\0', '\0' } });
            m.copyFrom(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('c', m.get(1, 0));
            assertEquals('d', m.get(1, 1));
        }

        @Test
        public void testCopyFrom_PartialOverwrite() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0', '\0' }, { '\0', '\0', '\0' }, { '\0', '\0', '\0' } });
            m.copyFrom(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('\0', m.get(0, 2));
            assertEquals('c', m.get(1, 0));
            assertEquals('d', m.get(1, 1));
            assertEquals('\0', m.get(2, 0));
        }

        // Empty path returns "[]" directly.
        @Test
        public void testCopyFrom_WithOffset_Shape3By3_OffsetsSourceAndPreservesOutsideCells() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { '\0', '\0', '\0' }, { '\0', '\0', '\0' }, { '\0', '\0', '\0' } });
            m.copyFrom(1, 1, new char[][] { { 'x', 'y' }, { 'z', 'w' } });
            assertEquals('\0', m.get(0, 0));
            assertEquals('\0', m.get(1, 0));
            assertEquals('x', m.get(1, 1));
            assertEquals('y', m.get(1, 2));
            assertEquals('z', m.get(2, 1));
            assertEquals('w', m.get(2, 2));
        }

        @Test
        public void testCopyFrom_EmptySource() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.copyFrom(new char[0][0]);
            assertEquals('a', m.get(0, 0));
            assertEquals('d', m.get(1, 1));
        }

        @Test
        public void testCopyFrom_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            char[][] patch = { { 'X', 'Y' }, { 'Z', 'W' } };
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, patch));
        }

        @Test
        public void testCopyFrom_WithOffset_InvalidPosition() {
            CharMatrix m = CharMatrix.wrap(new char[2][2]);
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(3, 3, new char[][] { { 'a', 'b' }, { 'c', 'd' } }));
        }

        @Test
        public void testCopyFrom_NegativeIndexThrows() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(-1, 0, new char[][] { { 'b' } }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyFrom(0, -1, new char[][] { { 'b' } }));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0", "3, 0", "0, -1", "0, 3" })
        public void testCopyFrom_RejectsOffsetsOutsideDestination(final int row, final int column) {
            final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyFrom(row, column, new char[][] { { (char) 1 } }));
            assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } }), matrix);
        }
    }

    @Nested
    class CopyTests {

        @Test
        public void testCopy() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);
            CharMatrix copy = matrix.copy();

            Assertions.assertEquals(matrix.rowCount(), copy.rowCount());
            Assertions.assertEquals(matrix.columnCount(), copy.columnCount());
            Assertions.assertEquals('a', copy.get(0, 0));
            Assertions.assertEquals('d', copy.get(1, 1));

            copy.set(0, 0, 'x');
            Assertions.assertEquals('a', matrix.get(0, 0)); // original unchanged
        }

        @Test
        public void testCopy_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix copy = m.copy();
            assertEquals(m.rowCount(), copy.rowCount());
            assertEquals(m.columnCount(), copy.columnCount());
            assertEquals('A', copy.get(0, 0));

            // Modify copy shouldn't affect original
            copy.set(0, 0, 'Z');
            assertEquals('A', m.get(0, 0));
            assertEquals('Z', copy.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2_FirstRowAndIndependentStorage() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix copy = m.copy();
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals('a', copy.get(0, 0));
            assertEquals('b', copy.get(0, 1));
            copy.set(0, 0, 'x');
            assertEquals('a', m.get(0, 0));
        }

        @Test
        public void testCopy_Shape2By2_DimensionsAndIndependentStorage() {
            CharMatrix original = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix copy = original.copy();
            assertEquals(original.rowCount(), copy.rowCount());
            assertEquals(original.columnCount(), copy.columnCount());
            assertEquals('a', copy.get(0, 0));
            // Modify copy
            copy.set(0, 0, 'z');
            assertEquals('a', original.get(0, 0)); // Original unchanged
        }

        @Test
        public void testCopy_EmptyMatrix() {
            CharMatrix m = CharMatrix.empty();
            CharMatrix copy = m.copy();
            assertTrue(copy.isEmpty());
        }

        @Test
        public void testZeroRowResultMethodsPreserveLogicalColumnCount() {
            CharMatrix matrix = new CharMatrix(new char[0][], 3);
            CharMatrix other = new CharMatrix(new char[0][], 3);

            assertShape(0, 3, matrix.copy());
            assertShape(0, 3, matrix.copyRows(0, 0));
            assertShape(0, 2, matrix.copyRegion(0, 0, 1, 3));
            assertShape(0, 3, matrix.map(value -> Character.toUpperCase(value)));
            assertShape(0, 3, matrix.mapToObj(Character::valueOf, Character.class));
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
            assertShape(0, 3, matrix.subtract(other));
            CharMatrix right = CharMatrix.wrap(new char[3][2]);
            assertShape(0, 2, matrix.matrixMultiply(right));
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
            char[][] a = { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);
            CharMatrix copy = matrix.copyRows(0, 2);

            Assertions.assertEquals(2, copy.rowCount());
            Assertions.assertEquals(2, copy.columnCount());
            Assertions.assertEquals('a', copy.get(0, 0));
            Assertions.assertEquals('d', copy.get(1, 1));

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(-1, 2));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(0, 4));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyRows(2, 1));
        }

        @Test
        public void testCopyRows_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            CharMatrix subset = m.copyRows(1, 3);
            assertEquals(2, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertEquals('D', subset.get(0, 0));
            assertEquals('I', subset.get(1, 2));
        }

        @Test
        public void testCopyRows_Shape3By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            CharMatrix copy = m.copyRows(1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals('c', copy.get(0, 0));
            assertEquals('d', copy.get(0, 1));
        }

        @Test
        public void testCopy_Rows() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            CharMatrix copy = matrix.copyRows(1, 3);
            // Returns [['c', 'd'], ['e', 'f']]
            assertEquals(2, copy.rowCount());
            assertEquals('c', copy.get(0, 0));
            assertEquals('d', copy.get(0, 1));
            assertEquals('e', copy.get(1, 0));
            assertEquals('f', copy.get(1, 1));
        }

        @Test
        public void testCopyRangesEmpty_ReturnsEmptyMatrix() {
            // An empty row slice keeps the column count (0 x N) and an empty column slice keeps the row
            // count (N x 0); only an empty range in BOTH dimensions collapses to 0 x 0.
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });

            CharMatrix empty = m.copyRows(0, 0);
            Assertions.assertEquals(0, empty.rowCount());
            Assertions.assertEquals(3, empty.columnCount());

            CharMatrix emptyRows = m.copyRegion(1, 1, 0, 3);
            Assertions.assertEquals(0, emptyRows.rowCount());
            Assertions.assertEquals(3, emptyRows.columnCount());

            CharMatrix emptyCols = m.copyRegion(0, 3, 1, 1);
            Assertions.assertEquals(3, emptyCols.rowCount());
            Assertions.assertEquals(0, emptyCols.columnCount());
        }

        @Test
        public void testCopyRows_SingleRow() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            CharMatrix copy = m.copyRows(1, 2);
            assertEquals(1, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals('c', copy.get(0, 0));
            assertEquals('d', copy.get(0, 1));
        }

        @Test
        public void testCopyRows_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(2, 1));
        }

        @Test
        public void testCopyRows_InvalidRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRows(0, 2));
        }
    }

    @Nested
    class CopyRegionTests {

        // An empty row slice keeps the column count (0 x N) and an empty column slice keeps the row
        @Test
        public void testCopyRegion() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix matrix = CharMatrix.wrap(a);
            CharMatrix copy = matrix.copyRegion(0, 2, 1, 3);

            Assertions.assertEquals(2, copy.rowCount());
            Assertions.assertEquals(2, copy.columnCount());
            Assertions.assertEquals('b', copy.get(0, 0));
            Assertions.assertEquals('c', copy.get(0, 1));
            Assertions.assertEquals('e', copy.get(1, 0));
            Assertions.assertEquals('f', copy.get(1, 1));
        }

        @Test
        public void testCopyRegion_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            CharMatrix submatrix = m.copyRegion(0, 2, 1, 3);
            assertEquals(2, submatrix.rowCount());
            assertEquals(2, submatrix.columnCount());
            assertEquals('B', submatrix.get(0, 0));
            assertEquals('F', submatrix.get(1, 1));
        }

        @Test
        public void testCopyRegion_Shape3By3_LowerRightRegion() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            CharMatrix copy = m.copyRegion(1, 3, 1, 3);
            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals('e', copy.get(0, 0));
            assertEquals('f', copy.get(0, 1));
            assertEquals('h', copy.get(1, 0));
            assertEquals('i', copy.get(1, 1));
        }

        @Test
        public void testCopy_Region() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            CharMatrix sub = matrix.copyRegion(0, 2, 1, 3);
            // Result: [['b', 'c'], ['e', 'f']]
            assertEquals(2, sub.rowCount());
            assertEquals(2, sub.columnCount());
            assertEquals('b', sub.get(0, 0));
            assertEquals('c', sub.get(0, 1));
            assertEquals('e', sub.get(1, 0));
            assertEquals('f', sub.get(1, 1));
        }

        @Test
        public void testCopyRegion_SingleCell() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            CharMatrix copy = m.copyRegion(0, 1, 1, 2);
            assertEquals(1, copy.rowCount());
            assertEquals(1, copy.columnCount());
            assertEquals('b', copy.get(0, 0));
        }

        @Test
        public void testCopyRegion_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, 0, 3));
        }

        @Test
        public void testCopyRegion_InvalidRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 1, -1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 1, 0, 3));
        }
    }

    @Nested
    class ResizeTests {

        @Test
        public void testResize_WithFill() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix extended = matrix.resize(3, 4, 'x');
            // Result: [['a', 'b', 'x', 'x'], ['c', 'd', 'x', 'x'], ['x', 'x', 'x', 'x']]
            assertEquals('a', extended.get(0, 0));
            assertEquals('b', extended.get(0, 1));
            assertEquals('x', extended.get(0, 2));
            assertEquals('x', extended.get(0, 3));
            assertEquals('c', extended.get(1, 0));
            assertEquals('d', extended.get(1, 1));
            assertEquals('x', extended.get(1, 2));
            assertEquals('x', extended.get(2, 0));

            // Truncate
            CharMatrix truncated = matrix.resize(1, 1, '\u0000');
            assertEquals('a', truncated.get(0, 0));
        }

        @Test
        public void testResize_Expand() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix resized = m.resize(3, 3);
            assertEquals(3, resized.rowCount());
            assertEquals(3, resized.columnCount());
            assertEquals('a', resized.get(0, 0));
            assertEquals('b', resized.get(0, 1));
            assertEquals('\0', resized.get(0, 2));
            assertEquals('c', resized.get(1, 0));
            assertEquals('\0', resized.get(2, 0));
            assertEquals('\0', resized.get(2, 2));
        }

        // 300*300 + 300*300 = 180000
        @Test
        public void testResize_Shrink() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            CharMatrix resized = m.resize(2, 2);
            assertEquals(2, resized.rowCount());
            assertEquals(2, resized.columnCount());
            assertEquals('a', resized.get(0, 0));
            assertEquals('b', resized.get(0, 1));
            assertEquals('d', resized.get(1, 0));
            assertEquals('e', resized.get(1, 1));
        }

        @Test
        public void testResize_WithDefaultValue() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'x' } });
            CharMatrix resized = m.resize(2, 3, 'z');
            assertEquals(2, resized.rowCount());
            assertEquals(3, resized.columnCount());
            assertEquals('x', resized.get(0, 0));
            assertEquals('z', resized.get(0, 1));
            assertEquals('z', resized.get(0, 2));
            assertEquals('z', resized.get(1, 0));
            assertEquals('z', resized.get(1, 1));
            assertEquals('z', resized.get(1, 2));
        }

        @Test
        public void testResize() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix extended = matrix.resize(3, 3);
            Assertions.assertEquals(3, extended.rowCount());
            Assertions.assertEquals(3, extended.columnCount());
            Assertions.assertEquals('a', extended.get(0, 0));
            Assertions.assertEquals('d', extended.get(1, 1));
            Assertions.assertEquals((char) 0, extended.get(2, 2));

            CharMatrix truncated = matrix.resize(1, 1);
            Assertions.assertEquals(1, truncated.rowCount());
            Assertions.assertEquals(1, truncated.columnCount());
            Assertions.assertEquals('a', truncated.get(0, 0));
        }

        @Test
        public void testResizeWithDefaultValue() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix extended = matrix.resize(3, 3, 'x');
            Assertions.assertEquals('x', extended.get(2, 2));
            Assertions.assertEquals('x', extended.get(0, 2));
            Assertions.assertEquals('x', extended.get(2, 0));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.resize(-1, 2, 'x'));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.resize(2, -1, 'x'));
        }

        @Test
        public void testResize_Larger() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix extended = m.resize(4, 4);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals('A', extended.get(0, 0));
            assertEquals('D', extended.get(1, 1));
            assertEquals(0, extended.get(3, 3)); // new cells are 0
        }

        // Check original values
        @Test
        public void testResize_Smaller() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            CharMatrix truncated = m.resize(2, 2);
            assertEquals(2, truncated.rowCount());
            assertEquals(2, truncated.columnCount());
            assertEquals('A', truncated.get(0, 0));
            assertEquals('E', truncated.get(1, 1));
        }

        @Test
        public void testResize_WithDefaultValue_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix extended = m.resize(3, 3, '?');
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals('A', extended.get(0, 0));
            assertEquals('?', extended.get(2, 2)); // new cell
        }

        @Test
        public void testResize_WithDefaultValue_Shape1By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix extended = m.resize(2, 3);
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals('a', extended.get(0, 0));
            assertEquals('b', extended.get(0, 1));
            assertEquals('\0', extended.get(0, 2));
            assertEquals('\0', extended.get(1, 0));
        }

        @Test
        public void testResize_WithCustomDefaultValue() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix extended = m.resize(2, 3, 'x');
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals('a', extended.get(0, 0));
            assertEquals('b', extended.get(0, 1));
            assertEquals('x', extended.get(0, 2));
            assertEquals('x', extended.get(1, 0));
        }

        @Test
        public void testResize_WithCustomDefaultValue_Shape1By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix extended = m.resize(2, 3, '*');
            assertEquals(2, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals('a', extended.get(0, 0));
            assertEquals('b', extended.get(0, 1));
            assertEquals('*', extended.get(0, 2));
            assertEquals('*', extended.get(1, 0));
        }

        @Test
        public void testResize_NoExtension() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix extended = m.resize(1, 2);
            assertEquals(1, extended.rowCount());
            assertEquals(2, extended.columnCount());
            assertEquals('a', extended.get(0, 0));
            assertEquals('b', extended.get(0, 1));
        }

        @Test
        public void testResize_TwoParamsWithDefault() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix extended = m.resize(3, 3, 'x');
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals('x', extended.get(2, 2));
        }

        @Test
        public void testResize_ToEmpty() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix resized = m.resize(0, 0);
            assertEquals(0, resized.rowCount());
            assertTrue(resized.isEmpty());
        }

        @Test
        public void testZeroRowShapeWithMaxColumnCountIsAllocationSafe() {
            CharMatrix m = new CharMatrix(new char[0][], 1);
            assertShape(0, Integer.MAX_VALUE, m.resize(0, Integer.MAX_VALUE));
            assertShape(0, Integer.MAX_VALUE, m.reshapeAndPad(0, Integer.MAX_VALUE));
        }

        @Test
        public void testResize_GrowsRowsWhileTrimmingColumns() {
            final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } });
            assertEquals(CharMatrix.wrap(new char[][] { { (char) 1 }, { (char) 3 }, { (char) 9 } }), matrix.resize(3, 1, (char) 9));
            assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } }), matrix);
        }

        @Test
        public void testResize_NegativeThrows() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a' } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> m.resize(1, -1));
        }

        @Test
        public void testResize_WithNegativeDimensions() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IllegalArgumentException.class, () -> m.resize(-1, 3, '?'));
            assertThrows(IllegalArgumentException.class, () -> m.resize(3, -1, '?'));
        }

        @Test
        public void testResize_InvalidSize() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });

            CharMatrix extended = m.resize(1, 1);
            assertEquals(1, extended.rowCount());
            assertEquals(1, extended.columnCount());
        }
    }

    @Nested
    class PadTests {

        @Test
        public void testPadInAllDirections() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix extended = matrix.pad(1, 1, 1, 1);
            Assertions.assertEquals(4, extended.rowCount());
            Assertions.assertEquals(4, extended.columnCount());
            Assertions.assertEquals('a', extended.get(1, 1));
            Assertions.assertEquals('d', extended.get(2, 2));
            Assertions.assertEquals((char) 0, extended.get(0, 0));
        }

        @Test
        public void testPadInAllDirectionsWithDefaultValue() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix extended = matrix.pad(1, 1, 1, 1, 'x');
            Assertions.assertEquals('x', extended.get(0, 0));
            Assertions.assertEquals('x', extended.get(0, 3));
            Assertions.assertEquals('x', extended.get(3, 0));
            Assertions.assertEquals('x', extended.get(3, 3));
            Assertions.assertEquals('a', extended.get(1, 1));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.pad(-1, 0, 0, 0, 'x'));
        }

        @Test
        public void testPad_Directional() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            CharMatrix extended = m.pad(1, 1, 2, 2);
            assertEquals(5, extended.rowCount()); // 1 + 3 + 1
            assertEquals(7, extended.columnCount()); // 2 + 3 + 2

            // Original values at offset position
            assertEquals('A', extended.get(1, 2));
            assertEquals('E', extended.get(2, 3));

            // New cells are 0
            assertEquals(0, extended.get(0, 0));
        }

        @Test
        public void testPad_DirectionalWithDefault() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            CharMatrix extended = m.pad(1, 1, 1, 1, '?');
            assertEquals(5, extended.rowCount());
            assertEquals(5, extended.columnCount());

            // Check original values
            assertEquals('A', extended.get(1, 1));

            // Check new values
            assertEquals('?', extended.get(0, 0));
            assertEquals('?', extended.get(4, 4));
        }

        @Test
        public void testPad_WithDirections() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'x' } });
            CharMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals('x', extended.get(1, 1));
            assertEquals('\0', extended.get(0, 0));
        }

        @Test
        public void testPad_WithDirectionsAndValue() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'x' } });
            CharMatrix extended = m.pad(1, 1, 1, 1, 'o');
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals('x', extended.get(1, 1));
            assertEquals('o', extended.get(0, 0));
        }

        @Test
        public void testPad_WithDirectionsAndValue_Shape1By1() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'x' } });
            CharMatrix extended = m.pad(1, 1, 1, 1, '*');
            assertEquals(3, extended.rowCount());
            assertEquals(3, extended.columnCount());
            assertEquals('x', extended.get(1, 1));
            assertEquals('*', extended.get(0, 0));
        }

        @Test
        public void testPad_FourParams() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix extended = m.pad(1, 1, 1, 1);
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals('a', extended.get(1, 1));
        }

        @Test
        public void testPad_FourParamsWithDefault() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix extended = m.pad(1, 1, 1, 1, 'x');
            assertEquals(4, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals('x', extended.get(0, 0));
            assertEquals('x', extended.get(3, 3));
        }

        @Test
        public void testPad_WithFill() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix padded = matrix.pad(1, 1, 2, 2, 'x');
            // Result: 3x6, border x, middle has a,b
            assertEquals(3, padded.rowCount());
            assertEquals(6, padded.columnCount());
            assertEquals('x', padded.get(0, 0));
            assertEquals('x', padded.get(1, 0));
            assertEquals('x', padded.get(1, 1));
            assertEquals('a', padded.get(1, 2));
            assertEquals('b', padded.get(1, 3));
            assertEquals('x', padded.get(1, 4));
            assertEquals('x', padded.get(1, 5));
            assertEquals('x', padded.get(2, 0));
        }

        @Test
        public void testPadFlattenForEachAndPrint_EdgeCase() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix extended = matrix.pad(0, 1, 1, 0, 'z');
            CharList flattened = matrix.flatten();
            List<Character> visited = new ArrayList<>();
            String printed = matrix.toMultilineString();

            matrix.forEach(0, 2, 1, 2, visited::add);

            assertEquals('z', extended.get(0, 0));
            assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, flattened.toArray());
            assertEquals(List.of('b', 'd'), visited);
            assertTrue(printed.contains("['a', 'b']"));
        }

        @Test
        public void testPad_SingleAxisFillPreservesExistingValues() {
            final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } });
            assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 9 }, { (char) 3, (char) 4, (char) 9 } }),
                    matrix.pad(0, 0, 0, 1, (char) 9));
            assertEquals(CharMatrix.wrap(new char[][] { { (char) 9, (char) 1, (char) 2 }, { (char) 9, (char) 3, (char) 4 } }),
                    matrix.pad(0, 0, 1, 0, (char) 9));
            assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 }, { (char) 9, (char) 9 } }),
                    matrix.pad(0, 1, 0, 0, (char) 9));
            final var copy = matrix.pad(0, 0, 0, 0, (char) 9);
            assertEquals(matrix, copy);
            assertNotSame(matrix, copy);
        }

        @Test
        public void testPad_DirectionalWithNegativeValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IllegalArgumentException.class, () -> m.pad(-1, 1, 1, 1, '?'));
        }

        @ParameterizedTest
        @CsvSource({ "-1, 0, 0, 0", "0, -1, 0, 0", "0, 0, -1, 0", "0, 0, 0, -1", "2147483647, 0, 0, 0", "0, 0, 0, 2147483647" })
        public void testPad_RejectsNegativeOrOverflowingDimensions(final int top, final int bottom, final int left, final int right) {
            final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.pad(top, bottom, left, right));
            assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } }), matrix);
        }
    }

    @Nested
    class FlipHorizontallyInPlaceTests {

        @Test
        public void testReverseH() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.flipHorizontallyInPlace();
            Assertions.assertEquals('c', matrix.get(0, 0));
            Assertions.assertEquals('b', matrix.get(0, 1));
            Assertions.assertEquals('a', matrix.get(0, 2));
            Assertions.assertEquals('f', matrix.get(1, 0));
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            m.flipHorizontallyInPlace();
            assertEquals('C', m.get(0, 0));
            assertEquals('B', m.get(0, 1));
            assertEquals('A', m.get(0, 2));
            assertEquals('F', m.get(1, 0));
        }

        @Test
        public void testFlipHorizontallyInPlace_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            m.flipHorizontallyInPlace();
            assertArrayEquals(new char[] { 'c', 'b', 'a' }, m.rowView(0));
            assertArrayEquals(new char[] { 'f', 'e', 'd' }, m.rowView(1));
        }

        @Test
        public void testFlipHorizontallyInPlace() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.flipHorizontallyInPlace();
            assertEquals('b', m.get(0, 0));
            assertEquals('a', m.get(0, 1));
        }

        @Test
        public void testFlipInPlaceHorizontally() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            m.flipHorizontallyInPlace();
            assertEquals('c', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('a', m.get(0, 2));
            assertEquals('f', m.get(1, 0));
            assertEquals('e', m.get(1, 1));
            assertEquals('d', m.get(1, 2));
        }

        @Test
        public void testFlipHorizontallyInPlace_SingleRow() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' } });
            m.flipHorizontallyInPlace();
            assertArrayEquals(new char[] { 'c', 'b', 'a' }, m.rowView(0));
        }

        @Test
        public void testFlipInPlaceHorizontally_SingleColumn() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a' }, { 'b' } });
            m.flipHorizontallyInPlace();
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(1, 0));
        }
    }

    @Nested
    class FlipVerticallyInPlaceTests {

        @Test
        public void testReverseV() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            matrix.flipVerticallyInPlace();
            Assertions.assertEquals('e', matrix.get(0, 0));
            Assertions.assertEquals('f', matrix.get(0, 1));
            Assertions.assertEquals('c', matrix.get(1, 0));
            Assertions.assertEquals('a', matrix.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape3By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' }, { 'E', 'F' } });
            m.flipVerticallyInPlace();
            assertEquals('E', m.get(0, 0));
            assertEquals('F', m.get(0, 1));
            assertEquals('C', m.get(1, 0));
            assertEquals('A', m.get(2, 0));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.flipVerticallyInPlace();
            assertEquals('c', m.get(0, 0));
            assertEquals('d', m.get(0, 1));
            assertEquals('a', m.get(1, 0));
            assertEquals('b', m.get(1, 1));
        }

        @Test
        public void testFlipVerticallyInPlace_Shape3By2_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            m.flipVerticallyInPlace();
            assertArrayEquals(new char[] { 'e', 'c', 'a' }, m.columnCopy(0));
            assertArrayEquals(new char[] { 'f', 'd', 'b' }, m.columnCopy(1));
        }

        @Test
        public void testFlipVerticallyInPlace() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            matrix.flipVerticallyInPlace();
            // matrix is now [['e', 'f'], ['c', 'd'], ['a', 'b']]
            assertArrayEquals(new char[] { 'e', 'f' }, matrix.rowView(0));
            assertArrayEquals(new char[] { 'c', 'd' }, matrix.rowView(1));
            assertArrayEquals(new char[] { 'a', 'b' }, matrix.rowView(2));
        }

        @Test
        public void testFlipInPlaceVertically() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            m.flipVerticallyInPlace();
            assertEquals('e', m.get(0, 0));
            assertEquals('f', m.get(0, 1));
            assertEquals('c', m.get(1, 0));
            assertEquals('d', m.get(1, 1));
            assertEquals('a', m.get(2, 0));
            assertEquals('b', m.get(2, 1));
        }

        @Test
        public void testFlipVerticallyInPlace_SingleColumn() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a' }, { 'b' }, { 'c' } });
            m.flipVerticallyInPlace();
            assertArrayEquals(new char[] { 'c', 'b', 'a' }, m.columnCopy(0));
        }

        @Test
        public void testFlipInPlaceVertically_SingleRow() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' } });
            m.flipVerticallyInPlace();
            assertEquals('a', m.get(0, 0));
            assertEquals('b', m.get(0, 1));
            assertEquals('c', m.get(0, 2));
        }
    }

    @Nested
    class FlipHorizontallyTests {

        @Test
        public void testFlipH() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix flipped = matrix.flipHorizontally();
            Assertions.assertEquals('c', flipped.get(0, 0));
            Assertions.assertEquals('a', flipped.get(0, 2));
            Assertions.assertEquals('a', matrix.get(0, 0)); // original unchanged
        }

        @Test
        public void testFlipHorizontally_Shape2By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            CharMatrix flipped = m.flipHorizontally();
            assertEquals('C', flipped.get(0, 0));
            assertEquals('B', flipped.get(0, 1));
            assertEquals('A', flipped.get(0, 2));

            // Original unchanged
            assertEquals('A', m.get(0, 0));
        }

        @Test
        public void testFlipHorizontally_Shape2By3_ReversesFirstRowAndPreservesSource() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            CharMatrix flipped = m.flipHorizontally();
            assertEquals('c', flipped.get(0, 0));
            assertEquals('b', flipped.get(0, 1));
            assertEquals('a', flipped.get(0, 2));
            assertEquals('a', m.get(0, 0));
        }

        @Test
        public void testFlipHorizontally_Shape2By3_IndependentStorage_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            CharMatrix flipped = m.flipHorizontally();
            assertEquals(2, flipped.rowCount());
            assertEquals(3, flipped.columnCount());
            assertArrayEquals(new char[] { 'c', 'b', 'a' }, flipped.rowView(0));
            assertArrayEquals(new char[] { 'f', 'e', 'd' }, flipped.rowView(1));
            assertNotSame(m, flipped);
        }

        @Test
        public void testFlipHorizontally() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix flipped = m.flipHorizontally();
            assertEquals('b', flipped.get(0, 0));
            assertEquals('a', flipped.get(0, 1));
        }

        @Test
        public void testFlipHorizontally_SingleRowPreservesSourceValues() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' } });
            CharMatrix flipped = matrix.flipHorizontally();
            assertArrayEquals(new char[] { 'c', 'b', 'a' }, flipped.rowView(0));
            // original unchanged
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, matrix.rowView(0));
        }
    }

    @Nested
    class FlipVerticallyTests {

        @Test
        public void testFlipV() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix flipped = matrix.flipVertically();
            Assertions.assertEquals('e', flipped.get(0, 0));
            Assertions.assertEquals('a', flipped.get(2, 0));
            Assertions.assertEquals('a', matrix.get(0, 0)); // original unchanged
        }

        @Test
        public void testFlipVertically_Shape3By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' }, { 'E', 'F' } });
            CharMatrix flipped = m.flipVertically();
            assertEquals('E', flipped.get(0, 0));
            assertEquals('C', flipped.get(1, 0));
            assertEquals('A', flipped.get(2, 0));

            // Original unchanged
            assertEquals('A', m.get(0, 0));
        }

        @Test
        public void testFlipVertically_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix flipped = m.flipVertically();
            assertEquals('c', flipped.get(0, 0));
            assertEquals('d', flipped.get(0, 1));
            assertEquals('a', flipped.get(1, 0));
            assertEquals('b', flipped.get(1, 1));
            assertEquals('a', m.get(0, 0));
        }

        @Test
        public void testFlipVertically_Shape3By2_IndependentStorage_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            CharMatrix flipped = m.flipVertically();
            assertEquals(3, flipped.rowCount());
            assertEquals(2, flipped.columnCount());
            assertArrayEquals(new char[] { 'e', 'c', 'a' }, flipped.columnCopy(0));
            assertArrayEquals(new char[] { 'f', 'd', 'b' }, flipped.columnCopy(1));
            assertNotSame(m, flipped);
        }

        @Test
        public void testFlipVertically() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            CharMatrix flipped = matrix.flipVertically();
            // Returns [['e', 'f'], ['c', 'd'], ['a', 'b']]
            assertArrayEquals(new char[] { 'e', 'f' }, flipped.rowView(0));
            assertArrayEquals(new char[] { 'c', 'd' }, flipped.rowView(1));
            assertArrayEquals(new char[] { 'a', 'b' }, flipped.rowView(2));
        }
    }

    @Nested
    class Rotate90Tests {

        @Test
        public void testRotate90() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix rotated = matrix.rotate90();
            Assertions.assertEquals(2, rotated.rowCount());
            Assertions.assertEquals(2, rotated.columnCount());
            Assertions.assertEquals('c', rotated.get(0, 0));
            Assertions.assertEquals('a', rotated.get(0, 1));
            Assertions.assertEquals('d', rotated.get(1, 0));
            Assertions.assertEquals('b', rotated.get(1, 1));
        }

        @Test
        public void testRotate90_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals('C', rotated.get(0, 0));
            assertEquals('A', rotated.get(0, 1));
            assertEquals('D', rotated.get(1, 0));
            assertEquals('B', rotated.get(1, 1));
        }

        @Test
        public void testRotate90_Shape2By2_DimensionsAndEveryCellValue() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix rotated = m.rotate90();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals('c', rotated.get(0, 0));
            assertEquals('a', rotated.get(0, 1));
            assertEquals('d', rotated.get(1, 0));
            assertEquals('b', rotated.get(1, 1));
        }

        @Test
        public void testRotate90_NonSquare() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            CharMatrix rotated = m.rotate90();
            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
        }
    }

    @Nested
    class Rotate180Tests {

        @Test
        public void testRotate180() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix rotated = matrix.rotate180();
            Assertions.assertEquals(2, rotated.rowCount());
            Assertions.assertEquals(2, rotated.columnCount());
            Assertions.assertEquals('d', rotated.get(0, 0));
            Assertions.assertEquals('c', rotated.get(0, 1));
            Assertions.assertEquals('b', rotated.get(1, 0));
            Assertions.assertEquals('a', rotated.get(1, 1));
        }

        @Test
        public void testRotate180_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix rotated = m.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals('D', rotated.get(0, 0));
            assertEquals('C', rotated.get(0, 1));
            assertEquals('B', rotated.get(1, 0));
            assertEquals('A', rotated.get(1, 1));
        }

        @Test
        public void testRotate180_Shape2By2_DimensionsAndEveryCellValue() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix rotated = m.rotate180();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals('d', rotated.get(0, 0));
            assertEquals('c', rotated.get(0, 1));
            assertEquals('b', rotated.get(1, 0));
            assertEquals('a', rotated.get(1, 1));
        }

        @Test
        public void testRotate180AndRotate90Twice_PreserveNx0Shape() {
            CharMatrix m = CharMatrix.wrap(new char[3][0]);

            CharMatrix via180 = m.rotate180();
            assertEquals(3, via180.rowCount());
            assertEquals(0, via180.columnCount());

            CharMatrix viaRotate90Twice = m.rotate90().rotate90();
            assertEquals(3, viaRotate90Twice.rowCount());
            assertEquals(0, viaRotate90Twice.columnCount());
        }
    }

    @Nested
    class Rotate270Tests {

        @Test
        public void testRotate270() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix rotated = matrix.rotate270();
            Assertions.assertEquals(2, rotated.rowCount());
            Assertions.assertEquals(2, rotated.columnCount());
            Assertions.assertEquals('b', rotated.get(0, 0));
            Assertions.assertEquals('d', rotated.get(0, 1));
            Assertions.assertEquals('a', rotated.get(1, 0));
            Assertions.assertEquals('c', rotated.get(1, 1));
        }

        @Test
        public void testRotate270_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals('B', rotated.get(0, 0));
            assertEquals('D', rotated.get(0, 1));
            assertEquals('A', rotated.get(1, 0));
            assertEquals('C', rotated.get(1, 1));
        }

        @Test
        public void testRotate270_Shape2By2_DimensionsAndEveryCellValue() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix rotated = m.rotate270();
            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals('b', rotated.get(0, 0));
            assertEquals('d', rotated.get(0, 1));
            assertEquals('a', rotated.get(1, 0));
            assertEquals('c', rotated.get(1, 1));
        }
    }

    @Nested
    class TransposeTests {

        @Test
        public void testTranspose() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix transposed = matrix.transpose();
            Assertions.assertEquals(3, transposed.rowCount());
            Assertions.assertEquals(2, transposed.columnCount());
            Assertions.assertEquals('a', transposed.get(0, 0));
            Assertions.assertEquals('d', transposed.get(0, 1));
            Assertions.assertEquals('b', transposed.get(1, 0));
            Assertions.assertEquals('e', transposed.get(1, 1));
        }

        @Test
        public void testTranspose_Shape2By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            CharMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals('A', transposed.get(0, 0));
            assertEquals('D', transposed.get(0, 1));
            assertEquals('B', transposed.get(1, 0));
            assertEquals('E', transposed.get(1, 1));
            assertEquals('C', transposed.get(2, 0));
            assertEquals('F', transposed.get(2, 1));
        }

        @Test
        public void testTranspose_Square() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals('A', transposed.get(0, 0));
            assertEquals('C', transposed.get(0, 1));
            assertEquals('B', transposed.get(1, 0));
            assertEquals('D', transposed.get(1, 1));
        }

        @Test
        public void testRotateTransposeAndConvertTallMatrix() {
            // Create a tall matrix (rows > columnCount) - 4 rows × 2 columnCount
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' }, { 'g', 'h' } });

            // Test rotate90() with tall matrix
            CharMatrix rotated90 = m.rotate90();
            assertEquals(2, rotated90.rowCount());
            assertEquals(4, rotated90.columnCount());
            assertEquals('g', rotated90.get(0, 0));

            // Test rotate270() with tall matrix
            CharMatrix rotated270 = m.rotate270();
            assertEquals(2, rotated270.rowCount());
            assertEquals(4, rotated270.columnCount());
            assertEquals('b', rotated270.get(0, 0));

            // Test transpose() with tall matrix
            CharMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(4, transposed.columnCount());
            assertEquals('a', transposed.get(0, 0));
            assertEquals('h', transposed.get(1, 3));

            // Test boxed() with tall matrix
            Matrix<Character> boxed = m.boxed();
            assertEquals(4, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Character.valueOf('a'), boxed.get(0, 0));

            // Test toLongMatrix() with tall matrix
            LongMatrix longMat = m.toLongMatrix();
            assertEquals(4, longMat.rowCount());
            assertEquals(2, longMat.columnCount());
            assertEquals('a', longMat.get(0, 0));

            // Test toFloatMatrix() with tall matrix
            FloatMatrix floatMat = m.toFloatMatrix();
            assertEquals(4, floatMat.rowCount());
            assertEquals(2, floatMat.columnCount());

            // Test toDoubleMatrix() with tall matrix
            DoubleMatrix doubleMat = m.toDoubleMatrix();
            assertEquals(4, doubleMat.rowCount());
            assertEquals(2, doubleMat.columnCount());
        }

        @Test
        public void testTranspose_Square_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix transposed = m.transpose();
            assertEquals(2, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals('a', transposed.get(0, 0));
            assertEquals('c', transposed.get(0, 1));
            assertEquals('b', transposed.get(1, 0));
            assertEquals('d', transposed.get(1, 1));
        }

        @Test
        public void testTranspose_Shape2By3_SwapsDimensionsAndCornerValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            CharMatrix transposed = m.transpose();
            assertEquals(3, transposed.rowCount());
            assertEquals(2, transposed.columnCount());
            assertEquals('a', transposed.get(0, 0));
            assertEquals('d', transposed.get(0, 1));
            assertEquals('b', transposed.get(1, 0));
            assertEquals('e', transposed.get(1, 1));
        }

        @Test
        public void testTranspose_Nx0_PreservesSwappedShape() {
            CharMatrix t = CharMatrix.wrap(new char[3][0]).transpose();
            assertEquals(0, t.rowCount());
            assertEquals(3, t.columnCount());
        }
    }

    @Nested
    class ReshapeAndPadTests {

        @Test
        public void testReshape_Empty() {
            CharMatrix empty = CharMatrix.empty();
            CharMatrix reshaped = empty.reshapeAndPad(2, 3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
        }

        @Test
        public void testReshape_InvalidSize() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' } });
            CharMatrix reshaped = m.reshapeAndPad(2, 2);
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
        }

        @ParameterizedTest
        @CsvSource({ "-1, 2", "2, -1", "1, 1" })
        public void testReshapeAndPad_RejectsNegativeAndInsufficientShapes(final int rows, final int columns) {
            final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.reshapeAndPad(rows, columns));
            assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } }), matrix);
        }
    }

    @Nested
    class RepeatElementsTests {

        @Test
        public void testRepeatElements() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix repeated = matrix.repeatElements(2, 3);
            Assertions.assertEquals(4, repeated.rowCount());
            Assertions.assertEquals(6, repeated.columnCount());
            Assertions.assertEquals('a', repeated.get(0, 0));
            Assertions.assertEquals('a', repeated.get(0, 2));
            Assertions.assertEquals('a', repeated.get(1, 0));
            Assertions.assertEquals('b', repeated.get(0, 3));
            Assertions.assertEquals('d', repeated.get(3, 5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(0, 1));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(1, 0));
        }

        @Test
        public void testRepeatElements_Shape1By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            CharMatrix repeated = m.repeatElements(2, 3);
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals('A', repeated.get(0, 0));
            assertEquals('A', repeated.get(0, 1));
            assertEquals('A', repeated.get(0, 2));
            assertEquals('B', repeated.get(0, 3));
            assertEquals('B', repeated.get(0, 4));
            assertEquals('B', repeated.get(0, 5));
            assertEquals('A', repeated.get(1, 0)); // second row same as first
        }

        @Test
        public void testRepeatElements_Shape2By2_RepeatedBlockAndAdjacentValue() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix repeated = m.repeatElements(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals('a', repeated.get(0, 0));
            assertEquals('a', repeated.get(0, 1));
            assertEquals('a', repeated.get(1, 0));
            assertEquals('a', repeated.get(1, 1));
            assertEquals('b', repeated.get(0, 2));
        }

        @Test
        public void testRepeatElements_RepeatsCellsTwiceVerticallyAndThreeTimesHorizontally() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix repeated = matrix.repeatElements(2, 3);
            // Result: [['a', 'a', 'a', 'b', 'b', 'b'], ['a', 'a', 'a', 'b', 'b', 'b']]
            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertEquals('a', repeated.get(0, 0));
            assertEquals('a', repeated.get(0, 1));
            assertEquals('a', repeated.get(0, 2));
            assertEquals('b', repeated.get(0, 3));
            assertEquals('b', repeated.get(0, 4));
            assertEquals('b', repeated.get(0, 5));
            assertEquals('a', repeated.get(1, 0));
        }

        @Test
        public void testRepeatElements_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } });
            final var result = matrix.repeatElements(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, (char) 0);
            assertEquals((char) 1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatElements_OnlyColumnsRepeated() {
            final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 3 }, { (char) 3, (char) 1 } });
            final var repeated = matrix.repeatElements(1, 2);
            assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 1, (char) 3, (char) 3 }, { (char) 3, (char) 3, (char) 1, (char) 1 } }), repeated);
            repeated.set(0, 0, (char) 3);
            assertEquals((char) 1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatElementsOverflow() {
            int largeSize = 50000;
            CharMatrix m = CharMatrix.wrap(new char[largeSize][2]);

            IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> m.repeatElements(50000, 1));
            assertTrue(ex1.getMessage().contains("row count overflow"));

            CharMatrix m2 = CharMatrix.wrap(new char[2][largeSize]);
            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> m2.repeatElements(1, 50000));
            assertTrue(ex2.getMessage().contains("column count overflow"));
        }

        @Test
        public void testRepeatElements_SingleRepeat() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix repeated = m.repeatElements(1, 1);
            assertEquals(1, repeated.rowCount());
            assertEquals(2, repeated.columnCount());
            assertEquals('a', repeated.get(0, 0));
            assertEquals('b', repeated.get(0, 1));
        }

        @Test
        public void testRepeatElements_InvalidArguments() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @Test
        public void testRepeatElements_InvalidRepeats() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a' } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatElements_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatElements(rows, columns));
        }
    }

    @Nested
    class RepeatMatrixTests {

        @Test
        public void testRepeatMatrix() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix repeated = matrix.repeatMatrix(2, 3);
            Assertions.assertEquals(4, repeated.rowCount());
            Assertions.assertEquals(6, repeated.columnCount());
            Assertions.assertEquals('a', repeated.get(0, 0));
            Assertions.assertEquals('a', repeated.get(0, 2));
            Assertions.assertEquals('a', repeated.get(0, 4));
            Assertions.assertEquals('a', repeated.get(2, 0));
            Assertions.assertEquals('d', repeated.get(3, 5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(0, 1));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(1, 0));
        }

        @Test
        public void testRepeatMatrix_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix repeated = m.repeatMatrix(2, 3);
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());

            // Check pattern
            assertEquals('A', repeated.get(0, 0));
            assertEquals('B', repeated.get(0, 1));
            assertEquals('A', repeated.get(0, 2)); // repeat starts
            assertEquals('B', repeated.get(0, 3));

            assertEquals('C', repeated.get(1, 0));
            assertEquals('D', repeated.get(1, 1));

            // Check vertical repeat
            assertEquals('A', repeated.get(2, 0)); // vertical repeat starts
            assertEquals('B', repeated.get(2, 1));
        }

        @Test
        public void testRepeatMatrix_Shape2By2_LastTileCorner() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix repeated = m.repeatMatrix(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals('a', repeated.get(0, 0));
            assertEquals('b', repeated.get(0, 1));
            assertEquals('d', repeated.get(3, 3));
        }

        @Test
        public void testRepeatMatrix_TwoByTwoTiling() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix repeated = m.repeatMatrix(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals('a', repeated.get(0, 0));
            assertEquals('b', repeated.get(0, 1));
            assertEquals('a', repeated.get(2, 2));
        }

        @Test
        public void testRepeatMatrix_TilesTwiceVerticallyAndThreeTimesHorizontally() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix repeated = matrix.repeatMatrix(2, 3);
            // Result: [['a', 'b', 'a', 'b', 'a', 'b'], ['c', 'd', 'c', 'd', 'c', 'd'],
            //          ['a', 'b', 'a', 'b', 'a', 'b'], ['c', 'd', 'c', 'd', 'c', 'd']]
            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertEquals('a', repeated.get(0, 0));
            assertEquals('b', repeated.get(0, 1));
            assertEquals('a', repeated.get(0, 2));
            assertEquals('d', repeated.get(1, 1));
            assertEquals('a', repeated.get(2, 0));
            assertEquals('d', repeated.get(3, 5));
        }

        @Test
        public void testRepeatMatrix_UnitRepetitionReturnsIndependentMatrix() {
            final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } });
            final var result = matrix.repeatMatrix(1, 1);
            assertEquals(matrix, result);
            assertNotSame(matrix, result);
            result.set(0, 0, (char) 0);
            assertEquals((char) 1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_OnlyColumnsRepeated() {
            final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } });
            final var repeated = matrix.repeatMatrix(1, 2);
            assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 1, (char) 2 }, { (char) 3, (char) 4, (char) 3, (char) 4 } }), repeated);
            repeated.set(0, 0, (char) 3);
            assertEquals((char) 1, matrix.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_Shape2By2_VerticalTileBoundary() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix result = m.repeatMatrix(2, 2);
            assertEquals(4, result.rowCount());
            assertEquals(4, result.columnCount());
            assertEquals('a', result.get(0, 0));
            assertEquals('b', result.get(0, 1));
            assertEquals('a', result.get(2, 0));
            assertEquals('b', result.get(2, 1));
        }

        @Test
        public void testRepeatMatrix_EmptyDimensionsDoNotTraverseRepeats() {
            CharMatrix zeroRows = new CharMatrix(new char[0][], 2);
            CharMatrix zeroColumns = CharMatrix.wrap(new char[2][0]);

            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                CharMatrix repeatedRows = zeroRows.repeatMatrix(Integer.MAX_VALUE, 1);
                assertEquals(0, repeatedRows.rowCount());
                assertEquals(2, repeatedRows.columnCount());

                CharMatrix repeatedColumns = zeroColumns.repeatMatrix(1, Integer.MAX_VALUE);
                assertEquals(2, repeatedColumns.rowCount());
                assertEquals(0, repeatedColumns.columnCount());
            });

            assertThrows(IllegalArgumentException.class, () -> zeroRows.repeatMatrix(1, Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> zeroColumns.repeatMatrix(Integer.MAX_VALUE, 1));
        }

        @Test
        public void testRepeatMatrixOverflow() {
            int largeSize = 50000;
            CharMatrix m = CharMatrix.wrap(new char[largeSize][2]);

            IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(50000, 1));
            assertTrue(ex1.getMessage().contains("row count overflow"));

            CharMatrix m2 = CharMatrix.wrap(new char[2][largeSize]);
            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> m2.repeatMatrix(1, 50000));
            assertTrue(ex2.getMessage().contains("column count overflow"));
        }

        @Test
        public void testRepeatMatrix_SingleRepeat() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix repeated = m.repeatMatrix(1, 1);
            assertEquals(1, repeated.rowCount());
            assertEquals(2, repeated.columnCount());
            assertEquals('a', repeated.get(0, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidArguments() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @Test
        public void testRepeatMatrix_InvalidRepeats() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a' } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
        }

        @ParameterizedTest
        @CsvSource({ "2147483647, 1", "1, 2147483647" })
        public void testRepeatMatrix_RejectsOverflowBeforeAllocation(final int rows, final int columns) {
            final var matrix = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } });
            assertThrows(IllegalArgumentException.class, () -> matrix.repeatMatrix(rows, columns));
            assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } }), matrix);
        }
    }

    @Nested
    class FlattenTests {

        @Test
        public void testFlatten() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharList flat = matrix.flatten();
            Assertions.assertEquals(6, flat.size());
            Assertions.assertEquals('a', flat.get(0));
            Assertions.assertEquals('b', flat.get(1));
            Assertions.assertEquals('c', flat.get(2));
            Assertions.assertEquals('d', flat.get(3));
            Assertions.assertEquals('e', flat.get(4));
            Assertions.assertEquals('f', flat.get(5));
        }

        @Test
        public void testFlatten_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            CharList flat = m.flatten();
            assertEquals(9, flat.size());
            char[] expected = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I' };
            for (int i = 0; i < 9; i++) {
                assertEquals(expected[i], flat.get(i));
            }
        }

        @Test
        public void testFlatten_Shape2By2_EveryListElement() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharList flat = m.flatten();
            assertEquals(4, flat.size());
            assertEquals('a', flat.get(0));
            assertEquals('b', flat.get(1));
            assertEquals('c', flat.get(2));
            assertEquals('d', flat.get(3));
        }

        @Test
        public void testFlatten_ThreeRowsInRowMajorOrder() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            CharList list = matrix.flatten();
            assertEquals(6, list.size());
            assertEquals('a', list.get(0));
            assertEquals('b', list.get(1));
            assertEquals('c', list.get(2));
            assertEquals('d', list.get(3));
            assertEquals('e', list.get(4));
            assertEquals('f', list.get(5));
        }

        @Test
        public void testFlatten_Empty() {
            CharMatrix empty = CharMatrix.empty();
            CharList flat = empty.flatten();
            assertTrue(flat.isEmpty());
        }

        @Test
        public void testFlatten_EmptyMatrix() {
            CharMatrix m = CharMatrix.empty();
            CharList flattened = m.flatten();
            assertEquals(0, flattened.size());
        }
    }

    @Nested
    class MutateViaFlatArrayTests {

        @Test
        public void testFlatOp() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            List<Character> collected = new ArrayList<>();
            matrix.mutateViaFlatArray(row -> {
                for (char c : row) {
                    collected.add(c);
                }
            });

            Assertions.assertEquals(4, collected.size());
            Assertions.assertTrue(collected.contains('a'));
            Assertions.assertTrue(collected.contains('d'));
        }

        @Test
        public void testMutateViaFlatArray_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            List<Integer> sums = new ArrayList<>();
            m.mutateViaFlatArray(row -> {
                int sum = 0;
                for (char val : row) {
                    sum += val;
                }
                sums.add(sum);
            });
            assertEquals(1, sums.size());
            assertTrue(sums.get(0) > 0);
        }

        @Test
        public void testMutateViaFlatArray_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            AtomicInteger count = new AtomicInteger(0);
            m.mutateViaFlatArray(row -> count.addAndGet(row.length));
            assertEquals(4, count.get());
        }

        @Test
        public void testMutateViaFlatArray() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            m.mutateViaFlatArray(arr -> {
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = (char) (arr[i] + 1);
                }
            });
            assertEquals('b', m.get(0, 0));
            assertEquals('c', m.get(0, 1));
        }

        @Test
        public void testMutateViaFlatArray_SortsAllCellsInRowOrder() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'd', 'b' }, { 'c', 'a' } });
            matrix.mutateViaFlatArray(arr -> java.util.Arrays.sort(arr));
            // matrix is now [['a', 'b'], ['c', 'd']]
            assertEquals('a', matrix.get(0, 0));
            assertEquals('b', matrix.get(0, 1));
            assertEquals('c', matrix.get(1, 0));
            assertEquals('d', matrix.get(1, 1));
        }
    }

    @Nested
    class StackVerticallyTests {

        @Test
        public void testStackVertically() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            char[][] b = { { 'e', 'f' }, { 'g', 'h' } };
            CharMatrix matrixA = CharMatrix.wrap(a);
            CharMatrix matrixB = CharMatrix.wrap(b);

            CharMatrix stacked = matrixA.stackVertically(matrixB);
            Assertions.assertEquals(4, stacked.rowCount());
            Assertions.assertEquals(2, stacked.columnCount());
            Assertions.assertEquals('a', stacked.get(0, 0));
            Assertions.assertEquals('e', stacked.get(2, 0));

            CharMatrix differentCols = CharMatrix.wrap(new char[][] { { 'x', 'y', 'z' } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.stackVertically(differentCols));
        }

        @Test
        public void testStackVertically_Shape2By3() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'G', 'H', 'I' }, { 'J', 'K', 'L' } });
            CharMatrix stacked = m1.stackVertically(m2);

            assertEquals(4, stacked.rowCount());
            assertEquals(3, stacked.columnCount());
            assertEquals('A', stacked.get(0, 0));
            assertEquals('G', stacked.get(2, 0));
            assertEquals('L', stacked.get(3, 2));
        }

        @Test
        public void testStackVertically_DifferentColumnCounts() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_Shape1By2() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'c', 'd' } });
            CharMatrix stacked = m1.stackVertically(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals('a', stacked.get(0, 0));
            assertEquals('b', stacked.get(0, 1));
            assertEquals('c', stacked.get(1, 0));
            assertEquals('d', stacked.get(1, 1));
        }

        @Test
        public void testStackVertically_IncompatibleColumns() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'c' } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        @Test
        public void testStackVertically_DifferentCols() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'c', 'd', 'e' } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackVertically(m2));
        }

        // Bug fix: CharMatrix.matrixMultiply() had an unnecessary (char) cast on each intermediate
        @Test
        public void testCharMatrixStackAndMultiplyRejectNullMatrix() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
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
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            char[][] b = { { 'e', 'f' }, { 'g', 'h' } };
            CharMatrix matrixA = CharMatrix.wrap(a);
            CharMatrix matrixB = CharMatrix.wrap(b);

            CharMatrix stacked = matrixA.stackHorizontally(matrixB);
            Assertions.assertEquals(2, stacked.rowCount());
            Assertions.assertEquals(4, stacked.columnCount());
            Assertions.assertEquals('a', stacked.get(0, 0));
            Assertions.assertEquals('e', stacked.get(0, 2));

            CharMatrix differentRows = CharMatrix.wrap(new char[][] { { 'x' }, { 'y' }, { 'z' } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.stackHorizontally(differentRows));
        }

        @Test
        public void testStackHorizontally_Shape2By2() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'E', 'F' }, { 'G', 'H' } });
            CharMatrix stacked = m1.stackHorizontally(m2);

            assertEquals(2, stacked.rowCount());
            assertEquals(4, stacked.columnCount());
            assertEquals('A', stacked.get(0, 0));
            assertEquals('E', stacked.get(0, 2));
            assertEquals('H', stacked.get(1, 3));
        }

        @Test
        public void testStackHorizontally_DifferentRowCounts() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'E', 'F' } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_Shape2By1() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a' }, { 'c' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'b' }, { 'd' } });
            CharMatrix stacked = m1.stackHorizontally(m2);
            assertEquals(2, stacked.rowCount());
            assertEquals(2, stacked.columnCount());
            assertEquals('a', stacked.get(0, 0));
            assertEquals('b', stacked.get(0, 1));
            assertEquals('c', stacked.get(1, 0));
            assertEquals('d', stacked.get(1, 1));
        }

        @Test
        public void testStackHorizontally_IncompatibleRows() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a' }, { 'c' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'b' } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_DifferentRows() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'c', 'd' }, { 'e', 'f' } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally() {
            CharMatrix a = CharMatrix.wrap(new char[][] { { 'a' }, { 'b' } });
            CharMatrix b = CharMatrix.wrap(new char[][] { { 'c' }, { 'd' } });
            CharMatrix stacked = a.stackHorizontally(b);
            // Result: [['a', 'c'], ['b', 'd']]
            assertEquals('a', stacked.get(0, 0));
            assertEquals('c', stacked.get(0, 1));
            assertEquals('b', stacked.get(1, 0));
            assertEquals('d', stacked.get(1, 1));
        }

        @Test
        public void testStackHorizontally_InvalidRows() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'b' }, { 'c' } });
            assertThrows(IllegalArgumentException.class, () -> m1.stackHorizontally(m2));
        }

        @Test
        public void testStackHorizontally_RejectsLogicalWidthOverflowWithoutAllocation() {
            final var huge = CharMatrix.wrap(new char[0][0]).resize(0, Integer.MAX_VALUE);
            final var extra = CharMatrix.wrap(new char[0][0]).resize(0, 1);
            assertThrows(IllegalArgumentException.class, () -> huge.stackHorizontally(extra));
            assertEquals(Integer.MAX_VALUE, huge.columnCount());
        }
    }

    @Nested
    class AddTests {

        @Test
        public void testAdd() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            char[][] b = { { 1, 2 }, { 3, 4 } };
            CharMatrix matrixA = CharMatrix.wrap(a);
            CharMatrix matrixB = CharMatrix.wrap(b);

            CharMatrix sum = matrixA.add(matrixB);
            Assertions.assertEquals('b', sum.get(0, 0)); // 'a' + 1
            Assertions.assertEquals('d', sum.get(0, 1)); // 'b' + 2
            Assertions.assertEquals('f', sum.get(1, 0)); // 'c' + 3
            Assertions.assertEquals('h', sum.get(1, 1)); // 'd' + 4

            CharMatrix differentShape = CharMatrix.wrap(new char[][] { { 'x' } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.add(differentShape));
        }

        @Test
        public void testAdd_WrapsModulo65536() {
            // Pins the class-javadoc claim: adding 1 to a cell holding (char) 65535 wraps the cell to 0.
            CharMatrix max = CharMatrix.wrap(new char[][] { { (char) 65535 } });
            CharMatrix one = CharMatrix.wrap(new char[][] { { (char) 1 } });
            Assertions.assertEquals((char) 0, max.add(one).get(0, 0));
            Assertions.assertEquals((char) 65535, CharMatrix.wrap(new char[][] { { (char) 0 } }).subtract(one).get(0, 0));
        }

        @Test
        public void testAdd_Shape2By2() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 1, 2 }, { 3, 4 } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 5, 6 }, { 7, 8 } });
            CharMatrix sum = m1.add(m2);

            assertEquals(6, sum.get(0, 0));
            assertEquals(8, sum.get(0, 1));
            assertEquals(10, sum.get(1, 0));
            assertEquals(12, sum.get(1, 1));
        }

        @Test
        public void testAdd_DifferentDimensions() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 1, 2 }, { 3, 4 } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.add(m2));
        }

        @Test
        public void testAdd_Shape2By2_EveryCellSum() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { '\1', '\1' }, { '\1', '\1' } });
            CharMatrix result = m1.add(m2);
            assertEquals('b', result.get(0, 0));
            assertEquals('c', result.get(0, 1));
            assertEquals('d', result.get(1, 0));
            assertEquals('e', result.get(1, 1));
        }

        @Test
        public void testAdd_AddsCharacterCodeOffsets() {
            CharMatrix a = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix b = CharMatrix.wrap(new char[][] { { 1, 2 } });
            CharMatrix sum = a.add(b);
            // Result: [['b', 'd']] (a+1=98, b+2=100)
            assertEquals('b', sum.get(0, 0));
            assertEquals('d', sum.get(0, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testAdd_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 } });
                final var right = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 } });
                assertEquals(CharMatrix.wrap(new char[][] { { (char) 2, (char) 4, (char) 6 }, { (char) 8, (char) 10, (char) 12 } }), left.add(right));
                assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 } }), left);
                assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }
    }

    @Nested
    class SubtractTests {

        @Test
        public void testSubtract() {
            char[][] a = { { 'd', 'e' }, { 'f', 'g' } };
            char[][] b = { { 1, 2 }, { 3, 4 } };
            CharMatrix matrixA = CharMatrix.wrap(a);
            CharMatrix matrixB = CharMatrix.wrap(b);

            CharMatrix diff = matrixA.subtract(matrixB);
            Assertions.assertEquals('c', diff.get(0, 0)); // 'd' - 1
            Assertions.assertEquals('c', diff.get(0, 1)); // 'e' - 2
            Assertions.assertEquals('c', diff.get(1, 0)); // 'f' - 3
            Assertions.assertEquals('c', diff.get(1, 1)); // 'g' - 4

            CharMatrix differentShape = CharMatrix.wrap(new char[][] { { 'x' } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.subtract(differentShape));
        }

        // Regression: matrixMultiply must build its result via CharMatrix.wrap(result) (not the raw
        // constructor) so an empty product yields the shared EMPTY singleton, and must call
        // checkNonNegativeShape before allocation for consistency with the other
        @Test
        public void testSubtract_Shape2By2() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'E', 'F' }, { 'G', 'H' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 1, 2 }, { 3, 4 } });
            CharMatrix diff = m1.subtract(m2);

            assertEquals('E' - 1, diff.get(0, 0));
            assertEquals('F' - 2, diff.get(0, 1));
            assertEquals('G' - 3, diff.get(1, 0));
            assertEquals('H' - 4, diff.get(1, 1));
        }

        @Test
        public void testSubtract_DifferentDimensions() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 5, 6 }, { 7, 8 } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.subtract(m2));
        }

        @Test
        public void testSubtract_Shape2By2_EveryCellDifference() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'b', 'c' }, { 'd', 'e' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { '\1', '\1' }, { '\1', '\1' } });
            CharMatrix result = m1.subtract(m2);
            assertEquals('a', result.get(0, 0));
            assertEquals('b', result.get(0, 1));
            assertEquals('c', result.get(1, 0));
            assertEquals('d', result.get(1, 1));
        }

        @Test
        public void testSubtract_SubtractsCharacterCodeOffsets() {
            CharMatrix a = CharMatrix.wrap(new char[][] { { 'd', 'e' } });
            CharMatrix b = CharMatrix.wrap(new char[][] { { 1, 2 } });
            CharMatrix diff = a.subtract(b);
            // Result: [['c', 'c']] (d-1=99, e-2=99)
            assertEquals('c', diff.get(0, 0));
            assertEquals('c', diff.get(0, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testSubtract_ExplicitModesPreserveBothOperands(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = CharMatrix.wrap(new char[][] { { (char) 7, (char) 8, (char) 9 }, { (char) 10, (char) 11, (char) 12 } });
                final var right = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 } });
                assertEquals(CharMatrix.wrap(new char[][] { { (char) 6, (char) 6, (char) 6 }, { (char) 6, (char) 6, (char) 6 } }), left.subtract(right));
                assertEquals(CharMatrix.wrap(new char[][] { { (char) 7, (char) 8, (char) 9 }, { (char) 10, (char) 11, (char) 12 } }), left);
                assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }
    }

    @Nested
    class MatrixMultiplyTests {

        @Test
        public void testMultiply() {
            char[][] a = { { 2, 3 }, { 4, 5 } };
            char[][] b = { { 1, 2 }, { 3, 4 } };
            CharMatrix matrixA = CharMatrix.wrap(a);
            CharMatrix matrixB = CharMatrix.wrap(b);

            CharMatrix product = matrixA.matrixMultiply(matrixB);
            Assertions.assertEquals(2, product.rowCount());
            Assertions.assertEquals(2, product.columnCount());
            // Results will be char values from multiplication

            CharMatrix incompatible = CharMatrix.wrap(new char[][] { { 'x', 'y', 'z' } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.matrixMultiply(incompatible));
        }

        @Test
        public void testMatrixMultiply_Shape2By2() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 1, 2 }, { 3, 4 } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 5, 6 }, { 7, 8 } });
            CharMatrix product = m1.matrixMultiply(m2);

            assertEquals(19, product.get(0, 0)); // 1*5 + 2*7
            assertEquals(22, product.get(0, 1)); // 1*6 + 2*8
            assertEquals(43, product.get(1, 0)); // 3*5 + 4*7
            assertEquals(50, product.get(1, 1)); // 3*6 + 4*8
        }

        @Test
        public void testMatrixMultiply_IncompatibleDimensions() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 1, 2 }, { 3, 4 } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.matrixMultiply(m2));
        }

        @Test
        public void testMatrixMultiply_RectangularMatrices() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 1, 2, 3 } }); // 1x3
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 4 }, { 5 }, { 6 } }); // 3x1
            CharMatrix product = m1.matrixMultiply(m2);

            assertEquals(1, product.rowCount());
            assertEquals(1, product.columnCount());
            assertEquals(32, product.get(0, 0)); // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        }

        @Test
        public void testMatrixMultiply() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { '\2', '\3' }, { '\4', '\5' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { '\2', '\2' }, { '\2', '\2' } });
            CharMatrix result = m1.matrixMultiply(m2);
            assertEquals('\12', result.get(0, 0)); // 2*2 + 3*2 = 10
            assertEquals('\12', result.get(0, 1)); // 2*2 + 3*2 = 10
            assertEquals('\22', result.get(1, 0)); // 4*2 + 5*2 = 18
            assertEquals('\22', result.get(1, 1)); // 4*2 + 5*2 = 18
        }

        @Test
        public void testMatrixMultiply_NoIntermediateCharCast() {
            // Bug fix: CharMatrix.matrixMultiply() had an unnecessary (char) cast on each intermediate
            // product, inconsistent with ByteMatrix and ShortMatrix. The fix removes it.
            // Verify that matrix multiplication still produces correct results.
            CharMatrix a = CharMatrix.wrap(new char[][] { { 2, 3 }, { 4, 5 } });
            CharMatrix b = CharMatrix.wrap(new char[][] { { 1, 2 }, { 3, 4 } });

            CharMatrix product = a.matrixMultiply(b);

            // product[0][0] = 2*1 + 3*3 = 11
            // product[0][1] = 2*2 + 3*4 = 16
            // product[1][0] = 4*1 + 5*3 = 19
            // product[1][1] = 4*2 + 5*4 = 28
            assertEquals(11, product.get(0, 0));
            assertEquals(16, product.get(0, 1));
            assertEquals(19, product.get(1, 0));
            assertEquals(28, product.get(1, 1));
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testMatrixMultiply_ExplicitModesComputeRectangularProduct(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var left = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 }, { (char) 5, (char) 6 } });
                final var right = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 } });
                assertEquals(
                        CharMatrix.wrap(
                                new char[][] { { (char) 9, (char) 12, (char) 15 }, { (char) 19, (char) 26, (char) 33 }, { (char) 29, (char) 40, (char) 51 } }),
                        left.matrixMultiply(right));
                assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 }, { (char) 5, (char) 6 } }), left);
                assertEquals(CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 } }), right);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testMatrixMultiply_OverflowAdvice_LongIsExactButIntOverflows() {
            // Pins the matrixMultiply javadoc: the char product wraps modulo 65536, widening via toLongMatrix()
            // gives the exact product, and toIntMatrix() is NOT enough (a single product above 46340^2 overflows int).
            CharMatrix left = CharMatrix.wrap(new char[][] { { (char) 50000, (char) 65535 } });
            CharMatrix right = CharMatrix.wrap(new char[][] { { (char) 50000 }, { (char) 65535 } });
            long exact = 50000L * 50000L + 65535L * 65535L;

            assertEquals((char) (exact & 0xFFFF), left.matrixMultiply(right).get(0, 0));
            assertEquals(exact, left.toLongMatrix().matrixMultiply(right.toLongMatrix()).get(0, 0));
            assertNotEquals(exact, left.toIntMatrix().matrixMultiply(right.toIntMatrix()).get(0, 0));

            CharMatrix single = CharMatrix.wrap(new char[][] { { (char) 46341 } });
            assertTrue(single.toIntMatrix().matrixMultiply(single.toIntMatrix()).get(0, 0) < 0);
            assertEquals(46341L * 46341L, single.toLongMatrix().matrixMultiply(single.toLongMatrix()).get(0, 0));
        }

        @Test
        public void testMatrixMultiply_EmptyProductReturnsCanonicalEmpty() {
            // Regression: matrixMultiply must build its result via CharMatrix.wrap(result) (not the raw
            // constructor) so an empty product yields the shared EMPTY singleton, and must call
            // checkNonNegativeShape before allocation for consistency with the other
            // result-allocating methods (resize/reshape/transpose/rotate).
            CharMatrix product = CharMatrix.empty().matrixMultiply(CharMatrix.empty());
            assertSame(CharMatrix.empty(), product);
            assertTrue(product.isEmpty());
        }

        @Test
        public void testMatrixMultiply_LargeValuesOverflow() {
            // Test with larger char values to verify behavior with values that overflow char range
            // during intermediate multiplication. The result should be the same whether or not
            // intermediate values are truncated, due to modular arithmetic.
            CharMatrix a = CharMatrix.wrap(new char[][] { { 300, 300 } });
            CharMatrix b = CharMatrix.wrap(new char[][] { { 300 }, { 300 } });

            CharMatrix product = a.matrixMultiply(b);

            // 300*300 + 300*300 = 180000
            // (char) 180000 = 180000 % 65536 = 49928 -- but wait, let's compute step by step
            // Due to char truncation on +=:
            // step k=0: result[0][0] = (char)(0 + 300*300) = (char)(90000) = (char)(90000 % 65536) = 24464
            // step k=1: result[0][0] = (char)(24464 + 300*300) = (char)(24464 + 90000) = (char)(114464) = (char)(114464 % 65536) = 48928
            // Without intermediate cast, same result due to modular arithmetic
            assertEquals((char) (300 * 300 + 300 * 300), product.get(0, 0));
        }

        @Test
        public void testMatrixMultiply_DimensionMismatchThrows() {
            CharMatrix a = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' } });
            CharMatrix b = CharMatrix.wrap(new char[][] { { 'x', 'y' } });

            assertThrows(IllegalArgumentException.class, () -> a.matrixMultiply(b));
        }
    }

    @Nested
    class BoxedTests {

        @Test
        public void testBoxed() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            Matrix<Character> boxed = matrix.boxed();
            Assertions.assertEquals(2, boxed.rowCount());
            Assertions.assertEquals(2, boxed.columnCount());
            Assertions.assertEquals(Character.valueOf('a'), boxed.get(0, 0));
            Assertions.assertEquals(Character.valueOf('d'), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            Matrix<Character> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(3, boxed.columnCount());
            assertEquals(Character.valueOf('A'), boxed.get(0, 0));
            assertEquals(Character.valueOf('F'), boxed.get(1, 2));
        }

        @Test
        public void testBoxed_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            Matrix<Character> boxed = m.boxed();
            assertEquals(2, boxed.rowCount());
            assertEquals(2, boxed.columnCount());
            assertEquals(Character.valueOf('a'), boxed.get(0, 0));
            assertEquals(Character.valueOf('b'), boxed.get(0, 1));
            assertEquals(Character.valueOf('c'), boxed.get(1, 0));
            assertEquals(Character.valueOf('d'), boxed.get(1, 1));
        }

        @Test
        public void testBoxed_Shape2By2_FirstRowBoxedValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            Matrix<Character> boxed = m.boxed();
            assertEquals('a', boxed.get(0, 0));
            assertEquals('b', boxed.get(0, 1));
        }

        @Test
        public void testBoxed_EmptyMatrix() {
            CharMatrix m = CharMatrix.empty();
            Matrix<Character> boxed = m.boxed();
            assertTrue(boxed.isEmpty());
        }
    }

    @Nested
    class ToIntMatrixTests {

        @Test
        public void testToIntMatrix() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            IntMatrix intMatrix = matrix.toIntMatrix();
            Assertions.assertEquals(2, intMatrix.rowCount());
            Assertions.assertEquals(2, intMatrix.columnCount());
            Assertions.assertEquals(97, intMatrix.get(0, 0)); // 'a'
            Assertions.assertEquals(100, intMatrix.get(1, 1)); // 'd'
        }

        @Test
        public void testToIntMatrix_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            IntMatrix intMatrix = m.toIntMatrix();
            assertEquals(2, intMatrix.rowCount());
            assertEquals(2, intMatrix.columnCount());
            assertEquals(65, intMatrix.get(0, 0)); // 'A' = 65
            assertEquals(68, intMatrix.get(1, 1)); // 'D' = 68
        }

        @Test
        public void testToIntMatrix_Shape2By2_FirstRowConvertedValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            IntMatrix intMatrix = m.toIntMatrix();
            assertEquals('a', intMatrix.get(0, 0));
            assertEquals('b', intMatrix.get(0, 1));
        }

        @Test
        public void testToIntMatrix_PreservesAlphabeticCodePoints() {
            CharMatrix charMatrix = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            IntMatrix intMatrix = charMatrix.toIntMatrix();
            // Result: [[97, 98]]
            assertEquals(97, intMatrix.get(0, 0));
            assertEquals(98, intMatrix.get(0, 1));
        }
    }

    @Nested
    class ToLongMatrixTests {

        @Test
        public void testToLongMatrix() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            LongMatrix longMatrix = matrix.toLongMatrix();
            Assertions.assertEquals(2, longMatrix.rowCount());
            Assertions.assertEquals(2, longMatrix.columnCount());
            Assertions.assertEquals(97L, longMatrix.get(0, 0)); // 'a'
            Assertions.assertEquals(100L, longMatrix.get(1, 1)); // 'd'
        }

        @Test
        public void testToLongMatrix_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            LongMatrix longMatrix = m.toLongMatrix();
            assertEquals(2, longMatrix.rowCount());
            assertEquals(2, longMatrix.columnCount());
            assertEquals(65L, longMatrix.get(0, 0));
            assertEquals(68L, longMatrix.get(1, 1));
        }

        @Test
        public void testToLongMatrix_Shape2By2_FirstRowConvertedValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            LongMatrix longMatrix = m.toLongMatrix();
            assertEquals('a', longMatrix.get(0, 0));
            assertEquals('b', longMatrix.get(0, 1));
        }

        @Test
        public void testToLongMatrix_PreservesAlphabeticCodePoints() {
            CharMatrix charMatrix = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            LongMatrix longMatrix = charMatrix.toLongMatrix();
            // Result: [[97L, 98L]]
            assertEquals(97L, longMatrix.get(0, 0));
            assertEquals(98L, longMatrix.get(0, 1));
        }
    }

    @Nested
    class ToFloatMatrixTests {

        @Test
        public void testToFloatMatrix() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            FloatMatrix floatMatrix = matrix.toFloatMatrix();
            Assertions.assertEquals(2, floatMatrix.rowCount());
            Assertions.assertEquals(2, floatMatrix.columnCount());
            Assertions.assertEquals(97.0f, floatMatrix.get(0, 0)); // 'a'
            Assertions.assertEquals(100.0f, floatMatrix.get(1, 1)); // 'd'
        }

        @Test
        public void testToFloatMatrix_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            FloatMatrix floatMatrix = m.toFloatMatrix();
            assertEquals(2, floatMatrix.rowCount());
            assertEquals(2, floatMatrix.columnCount());
            assertEquals(65.0f, floatMatrix.get(0, 0), 0.0001f);
            assertEquals(68.0f, floatMatrix.get(1, 1), 0.0001f);
        }

        @Test
        public void testToFloatMatrix_Shape2By2_FirstRowConvertedValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            FloatMatrix floatMatrix = m.toFloatMatrix();
            assertEquals('a', floatMatrix.get(0, 0));
            assertEquals('b', floatMatrix.get(0, 1));
        }
    }

    @Nested
    class ToDoubleMatrixTests {

        @Test
        public void testToDoubleMatrix() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            DoubleMatrix doubleMatrix = matrix.toDoubleMatrix();
            Assertions.assertEquals(2, doubleMatrix.rowCount());
            Assertions.assertEquals(2, doubleMatrix.columnCount());
            Assertions.assertEquals(97.0, doubleMatrix.get(0, 0)); // 'a'
            Assertions.assertEquals(100.0, doubleMatrix.get(1, 1)); // 'd'
        }

        @Test
        public void testToDoubleMatrix_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            DoubleMatrix doubleMatrix = m.toDoubleMatrix();
            assertEquals(2, doubleMatrix.rowCount());
            assertEquals(2, doubleMatrix.columnCount());
            assertEquals(65.0, doubleMatrix.get(0, 0), 0.0001);
            assertEquals(68.0, doubleMatrix.get(1, 1), 0.0001);
        }

        @Test
        public void testToDoubleMatrix_Shape2By2_FirstRowConvertedValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            DoubleMatrix doubleMatrix = m.toDoubleMatrix();
            assertEquals('a', doubleMatrix.get(0, 0));
            assertEquals('b', doubleMatrix.get(0, 1));
        }
    }

    @Nested
    class ZipWithTests {

        @Test
        public void testZipWith() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            char[][] b = { { 'A', 'B' }, { 'C', 'D' } };
            CharMatrix matrixA = CharMatrix.wrap(a);
            CharMatrix matrixB = CharMatrix.wrap(b);

            CharMatrix result = matrixA.zipWith(matrixB, (x, y) -> (char) Math.max(x, y));
            Assertions.assertEquals('a', result.get(0, 0));
            Assertions.assertEquals('b', result.get(0, 1));
            Assertions.assertEquals('c', result.get(1, 0));
            Assertions.assertEquals('d', result.get(1, 1));

            CharMatrix differentShape = CharMatrix.wrap(new char[][] { { 'x' } });
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrixA.zipWith(differentShape, (x, y) -> x));
        }

        @Test
        public void testZipWith3() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            char[][] b = { { 'e', 'f' }, { 'g', 'h' } };
            char[][] c = { { 'i', 'j' }, { 'k', 'l' } };
            CharMatrix matrixA = CharMatrix.wrap(a);
            CharMatrix matrixB = CharMatrix.wrap(b);
            CharMatrix matrixC = CharMatrix.wrap(c);

            CharMatrix result = matrixA.zipWith(matrixB, matrixC, (x, y, z) -> (char) Math.max(Math.max(x, y), z));
            Assertions.assertEquals('i', result.get(0, 0));
            Assertions.assertEquals('j', result.get(0, 1));
            Assertions.assertEquals('k', result.get(1, 0));
            Assertions.assertEquals('l', result.get(1, 1));
        }

        @Test
        public void testZipWith_Shape2By2() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 1, 2 }, { 3, 4 } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 5, 6 }, { 7, 8 } });
            CharMatrix result = m1.zipWith(m2, (a, b) -> (char) (a * b));

            assertEquals(5, result.get(0, 0)); // 1*5
            assertEquals(12, result.get(0, 1)); // 2*6
            assertEquals(21, result.get(1, 0)); // 3*7
            assertEquals(32, result.get(1, 1)); // 4*8
        }

        @Test
        public void testZipWith_DifferentShapes() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 1, 2 }, { 3, 4 } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 1, 2, 3 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> (char) (a + b)));
        }

        @Test
        public void testZipWith_ThreeMatrices() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 1, 2 }, { 3, 4 } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 5, 6 }, { 7, 8 } });
            CharMatrix m3 = CharMatrix.wrap(new char[][] { { 9, 10 }, { 11, 12 } });
            CharMatrix result = m1.zipWith(m2, m3, (a, b, c) -> (char) (a + b + c));

            assertEquals(15, result.get(0, 0)); // 1+5+9
            assertEquals(18, result.get(0, 1)); // 2+6+10
            assertEquals(21, result.get(1, 0)); // 3+7+11
            assertEquals(24, result.get(1, 1)); // 4+8+12
        }

        @Test
        public void testZipWith_ThreeMatrices_DifferentShapes() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 1, 2 }, { 3, 4 } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 5, 6 }, { 7, 8 } });
            CharMatrix m3 = CharMatrix.wrap(new char[][] { { 9, 10, 11 } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, m3, (a, b, c) -> (char) (a + b + c)));
        }

        @Test
        public void testZipWith_Two() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { '1', '2' }, { '3', '4' } });
            CharMatrix result = m1.zipWith(m2, (a, b) -> (char) (a + b - a));
            assertEquals(('1'), result.get(0, 0));
            assertEquals(('2'), result.get(0, 1));
        }

        @Test
        public void testZipWith_Three() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'c', 'd' } });
            CharMatrix m3 = CharMatrix.wrap(new char[][] { { 'e', 'f' } });
            CharMatrix result = m1.zipWith(m2, m3, (a, b, c) -> c);
            assertEquals('e', result.get(0, 0));
            assertEquals('f', result.get(0, 1));
        }

        @Test
        public void testZipWith_TwoMatrices() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { '\1', '\1' }, { '\1', '\1' } });
            CharMatrix result = m1.zipWith(m2, (a, b) -> (char) (a + b));
            assertEquals('b', result.get(0, 0));
            assertEquals('c', result.get(0, 1));
        }

        @Test
        public void testZipWith_ThreeMatrices_Shape2By2() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { '\1', '\1' }, { '\1', '\1' } });
            CharMatrix m3 = CharMatrix.wrap(new char[][] { { '\1', '\1' }, { '\1', '\1' } });
            CharMatrix result = m1.zipWith(m2, m3, (a, b, c) -> (char) (a + b + c));
            assertEquals('c', result.get(0, 0));
            assertEquals('d', result.get(0, 1));
        }

        @Test
        public void testZipWith_MaximumOfCorrespondingCells() {
            CharMatrix a = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix b = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            CharMatrix result = a.zipWith(b, (x, y) -> (char) Math.max(x, y));
            // Result: [['a', 'b']] (max of each pair)
            assertEquals('a', result.get(0, 0));
            assertEquals('b', result.get(0, 1));
        }

        @Test
        public void testZipWith_MaximumOfThreeCorrespondingCells() {
            CharMatrix a = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix b = CharMatrix.wrap(new char[][] { { 'c', 'd' } });
            CharMatrix c = CharMatrix.wrap(new char[][] { { 'e', 'f' } });
            CharMatrix result = a.zipWith(b, c, (x, y, z) -> (char) Math.max(Math.max(x, y), z));
            // Result: [['e', 'f']] (max of each triple)
            assertEquals('e', result.get(0, 0));
            assertEquals('f', result.get(0, 1));
        }

        @Test
        public void testZipWith_Two_InvalidShape() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'c' } });
            assertThrows(IllegalArgumentException.class, () -> m1.zipWith(m2, (a, b) -> a));
        }
    }

    @Nested
    class MainDiagonalStreamTests {

        @Test
        public void testStreamLU2RD() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] diagonal = matrix.mainDiagonalStream().toArray();
            Assertions.assertArrayEquals(new char[] { 'a', 'e', 'i' }, diagonal);

            CharMatrix empty = CharMatrix.empty();
            Assertions.assertTrue(empty.mainDiagonalStream().toList().isEmpty());

            CharMatrix nonSquare = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            Assertions.assertArrayEquals(new char[] { 'a' }, nonSquare.mainDiagonalStream().toArray());
        }

        @Test
        public void testMainDiagonalStream_Shape3By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            char[] diagonal = m.mainDiagonalStream().toArray();
            assertArrayEquals(new char[] { 'A', 'E', 'I' }, diagonal);
        }

        @Test
        public void testMainDiagonalStream_NonSquare() {
            CharMatrix nonSquare = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            assertArrayEquals(new char[] { 'A' }, nonSquare.mainDiagonalStream().toArray());
        }

        @Test
        public void testMainDiagonalStream_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            List<Character> diag = m.mainDiagonalStream().boxed().toList();
            assertEquals(3, diag.size());
            assertEquals('a', diag.get(0));
            assertEquals('e', diag.get(1));
            assertEquals('i', diag.get(2));
        }

        @Test
        public void testMainDiagonalStream_Shape3By3_OrderedListValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            List<Character> diagonal = m.mainDiagonalStream().boxed().toList();
            assertEquals(3, diagonal.size());
            assertEquals('a', diagonal.get(0).charValue());
            assertEquals('e', diagonal.get(1).charValue());
            assertEquals('i', diagonal.get(2).charValue());
        }

        @Test
        public void testMainDiagonalStream() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            char[] diag = m.mainDiagonalStream().toArray();
            assertArrayEquals(new char[] { 'a', 'e', 'i' }, diag);
        }

        @SuppressWarnings("unchecked")
        @Test
        public void testIteratorExCountConsumesEveryCustomIterator() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });

            com.landawn.abacus.util.stream.CharIteratorEx main = (com.landawn.abacus.util.stream.CharIteratorEx) matrix.mainDiagonalStream().iterator();
            assertEquals(2, main.count());
            assertFalse(main.hasNext());

            com.landawn.abacus.util.stream.CharIteratorEx anti = (com.landawn.abacus.util.stream.CharIteratorEx) matrix.antiDiagonalStream().iterator();
            assertEquals(2, anti.count());
            assertFalse(anti.hasNext());

            com.landawn.abacus.util.stream.CharIteratorEx rowMajor = (com.landawn.abacus.util.stream.CharIteratorEx) matrix.rowMajorStream().iterator();
            assertEquals(6, rowMajor.count());
            assertFalse(rowMajor.hasNext());

            com.landawn.abacus.util.stream.CharIteratorEx columnMajor = (com.landawn.abacus.util.stream.CharIteratorEx) matrix.columnMajorStream().iterator();
            assertEquals(6, columnMajor.count());
            assertFalse(columnMajor.hasNext());

            com.landawn.abacus.util.stream.ObjIteratorEx<CharStream> rows = (com.landawn.abacus.util.stream.ObjIteratorEx<CharStream>) matrix.rowStreams()
                    .iterator();
            assertEquals(2, rows.count());
            assertFalse(rows.hasNext());

            com.landawn.abacus.util.stream.ObjIteratorEx<CharStream> columns = (com.landawn.abacus.util.stream.ObjIteratorEx<CharStream>) matrix.columnStreams()
                    .iterator();
            com.landawn.abacus.util.stream.CharIteratorEx columnValues = (com.landawn.abacus.util.stream.CharIteratorEx) columns.next().iterator();
            assertEquals(2, columnValues.count());
            assertFalse(columnValues.hasNext());
            assertEquals(2, columns.count());
            assertFalse(columns.hasNext());
        }

        @Test
        public void testCharIteratorMaterializationConsumesOnlyTheRemainingLiveValues() {
            for (final boolean asList : new boolean[] { false, true }) {
                final CharMatrix matrix = CharMatrix.wrap(new char[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
                final CharIteratorEx main = (CharIteratorEx) matrix.mainDiagonalStream().iterator();
                assertEquals((char) 1, main.nextChar());
                matrix.set(1, 1, (char) 50);
                final char[] mainRemaining = asList ? main.toList().toArray() : main.toArray();
                assertArrayEquals(new char[] { 50, 9 }, mainRemaining);
                assertFalse(main.hasNext());
                assertEquals(0L, main.count());
                assertArrayEquals(new char[0], main.toArray());
                mainRemaining[0] = (char) 99;
                assertEquals((char) 50, matrix.get(1, 1));
                matrix.set(1, 1, (char) 5);

                final CharIteratorEx anti = (CharIteratorEx) matrix.antiDiagonalStream().iterator();
                anti.advance(1);
                matrix.set(2, 0, (char) 70);
                assertArrayEquals(new char[] { 5, 70 }, asList ? anti.toList().toArray() : anti.toArray());
                assertFalse(anti.hasNext());
                assertEquals(0L, anti.count());
                assertEquals(0, anti.toList().size());

                final CharIteratorEx column = (CharIteratorEx) matrix.columnStreams(1, 2).iterator().next().iterator();
                assertEquals((char) 2, column.nextChar());
                matrix.flipVerticallyInPlace();
                matrix.set(1, 1, (char) 55);
                assertArrayEquals(new char[] { 55, 2 }, asList ? column.toList().toArray() : column.toArray());
                assertFalse(column.hasNext());
                assertEquals(0L, column.count());
                assertArrayEquals(new char[0], column.toArray());

                final CharIteratorEx rows = (CharIteratorEx) matrix.rowMajorStream().iterator();
                assertEquals((char) 70, rows.nextChar());
                rows.advance(3);
                matrix.set(1, 1, (char) 56);
                assertArrayEquals(new char[] { 56, 6, 1, 2, 3 }, asList ? rows.toList().toArray() : rows.toArray());
                assertFalse(rows.hasNext());
                assertEquals(0L, rows.count());
                assertEquals(0, rows.toList().size());

                final CharIteratorEx columns = (CharIteratorEx) matrix.columnMajorStream().iterator();
                assertEquals((char) 70, columns.nextChar());
                columns.advance(3);
                matrix.set(1, 1, (char) 57);
                assertArrayEquals(new char[] { 57, 2, 9, 6, 3 }, asList ? columns.toList().toArray() : columns.toArray());
                assertFalse(columns.hasNext());
                assertEquals(0L, columns.count());
                columns.advance(Long.MAX_VALUE);
                assertEquals(0, columns.toList().size());
            }
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testMainDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = CharMatrix
                    .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
            final char[] expected = new char[] { (char) 1, (char) 5, (char) 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (CharIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextChar);
            }
        }

        @Test
        public void testMainDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = CharMatrix
                    .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
            try (var stream = matrix.mainDiagonalStream()) {
                final var iterator = (CharIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((char) 1, iterator.nextChar());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextChar);
            }
        }

        @Test
        public void testMainDiagonalStream_Empty() {
            CharMatrix empty = CharMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().toArray().length);
        }

        @Test
        public void testDiagonalStream_Nx0MatrixIsEmpty() {
            CharMatrix m = CharMatrix.wrap(new char[3][0]);
            assertEquals(3, m.rowCount());
            assertEquals(0, m.columnCount());
            assertEquals(0, m.mainDiagonalStream().count());
            assertEquals(0, m.antiDiagonalStream().count());

            CharMatrix empty = CharMatrix.empty();
            assertEquals(0, empty.mainDiagonalStream().count());
            assertEquals(0, empty.antiDiagonalStream().count());
        }
    }

    @Nested
    class AntiDiagonalStreamTests {

        @Test
        public void testStreamRU2LD() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] diagonal = matrix.antiDiagonalStream().toArray();
            Assertions.assertArrayEquals(new char[] { 'c', 'e', 'g' }, diagonal);
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            char[] antiDiagonal = m.antiDiagonalStream().toArray();
            assertArrayEquals(new char[] { 'C', 'E', 'G' }, antiDiagonal);
        }

        @Test
        public void testAntiDiagonalStream_NonSquare() {
            CharMatrix nonSquare = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            assertArrayEquals(new char[] { 'B' }, nonSquare.antiDiagonalStream().toArray());
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            List<Character> diag = m.antiDiagonalStream().boxed().toList();
            assertEquals(3, diag.size());
            assertEquals('c', diag.get(0));
            assertEquals('e', diag.get(1));
            assertEquals('g', diag.get(2));
        }

        @Test
        public void testAntiDiagonalStream_Shape3By3_OrderedListValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            List<Character> diagonal = m.antiDiagonalStream().boxed().toList();
            assertEquals(3, diagonal.size());
            assertEquals('c', diagonal.get(0).charValue());
            assertEquals('e', diagonal.get(1).charValue());
            assertEquals('g', diagonal.get(2).charValue());
        }

        @Test
        public void testAntiDiagonalStream() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            char[] diag = m.antiDiagonalStream().toArray();
            assertArrayEquals(new char[] { 'c', 'e', 'g' }, diag);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testAntiDiagonalStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = CharMatrix
                    .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
            final char[] expected = new char[] { (char) 3, (char) 5, (char) 7 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (CharIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextChar);
            }
        }

        @Test
        public void testAntiDiagonalStream_CountConsumesOnlyRemainingValues() {
            final var matrix = CharMatrix
                    .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
            try (var stream = matrix.antiDiagonalStream()) {
                final var iterator = (CharIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((char) 3, iterator.nextChar());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextChar);
            }
        }

        @Test
        public void testAntiDiagonalStream_Empty() {
            CharMatrix empty = CharMatrix.empty();
            assertEquals(0, empty.antiDiagonalStream().toArray().length);
        }
    }

    @Nested
    class RowMajorStreamTests {

        @Test
        public void testRowMajorStream() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] all = matrix.rowMajorStream().toArray();
            Assertions.assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, all);

            CharMatrix empty = CharMatrix.empty();
            Assertions.assertTrue(empty.rowMajorStream().toList().isEmpty());
        }

        @Test
        public void testRowMajorStreamRange() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] range = matrix.rowMajorStream(1, 3).toArray();
            Assertions.assertArrayEquals(new char[] { 'c', 'd', 'e', 'f' }, range);

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(-1, 2));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(0, 4));
        }

        @Test
        public void testRowMajorStream_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            char[] all = m.rowMajorStream().toArray();
            assertArrayEquals(new char[] { 'A', 'B', 'C', 'D', 'E', 'F' }, all);
        }

        @Test
        public void testRowMajorStream_WithRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            char[] rows = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new char[] { 'D', 'E', 'F', 'G', 'H', 'I' }, rows);
        }

        @Test
        public void testRowMajorStream_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<Character> all = m.rowMajorStream().boxed().toList();
            assertEquals(4, all.size());
            assertEquals('a', all.get(0));
            assertEquals('b', all.get(1));
        }

        @Test
        public void testRowMajorStream_RowRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            List<Character> rows = m.rowMajorStream(1, 3).boxed().toList();
            assertEquals(4, rows.size());
            assertEquals('c', rows.get(0));
            assertEquals('d', rows.get(1));
        }

        @Test
        public void testRowMajorStream_Shape2By2_SizeAndFirstValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<Character> elements = m.rowMajorStream().boxed().toList();
            assertEquals(4, elements.size());
            assertEquals('a', elements.get(0).charValue());
            assertEquals('b', elements.get(1).charValue());
        }

        @Test
        public void testRowMajorStream_WithRange_Shape3By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            List<Character> elements = m.rowMajorStream(1, 3).boxed().toList();
            assertEquals(4, elements.size());
            assertEquals('c', elements.get(0).charValue());
            assertEquals('d', elements.get(1).charValue());
        }

        @Test
        public void testHorizontalStream() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            char[] elements = m.rowMajorStream().toArray();
            assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, elements);
        }

        @Test
        public void testRowMajorStream_RowRange_Shape3By2_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            char[] elements = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new char[] { 'c', 'd', 'e', 'f' }, elements);
        }

        @Test
        public void testRowMajorStreamorizontalIteratorAdvanceAndExhaustion_EdgeCase() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            var iterator = matrix.rowMajorStream(0, 2).iterator();

            assertTrue(iterator instanceof com.landawn.abacus.util.stream.CharIteratorEx);

            com.landawn.abacus.util.stream.CharIteratorEx ex = (com.landawn.abacus.util.stream.CharIteratorEx) iterator;
            ex.advance(2);
            assertEquals(2L, ex.count());
            assertFalse(ex.hasNext());

            com.landawn.abacus.util.stream.CharIteratorEx fresh = (com.landawn.abacus.util.stream.CharIteratorEx) matrix.rowMajorStream(0, 2).iterator();
            fresh.advance(2);
            assertEquals('c', fresh.nextChar());
            ex.advance(10);
            assertEquals(0L, ex.count());
            assertThrows(java.util.NoSuchElementException.class, ex::nextChar);
        }

        @Test
        public void testStreamIterators_MidStrideAdvanceThenToArray_EdgeCase() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });

            var rowMajorIterator = matrix.rowMajorStream(0, 2).iterator();
            assertTrue(rowMajorIterator instanceof com.landawn.abacus.util.stream.CharIteratorEx);
            com.landawn.abacus.util.stream.CharIteratorEx rowMajor = (com.landawn.abacus.util.stream.CharIteratorEx) rowMajorIterator;
            rowMajor.advance(1); // mid-row cursor: the chunked toArray must start at column 1
            assertEquals(5L, rowMajor.count());
            assertArrayEquals(new char[0], rowMajor.toArray());

            com.landawn.abacus.util.stream.CharIteratorEx rowMajorFresh = (com.landawn.abacus.util.stream.CharIteratorEx) matrix.rowMajorStream(0, 2)
                    .iterator();
            rowMajorFresh.advance(1);
            assertArrayEquals(new char[] { 'b', 'c', 'd', 'e', 'f' }, rowMajorFresh.toArray());

            var columnMajorIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.CharIteratorEx columnMajor = (com.landawn.abacus.util.stream.CharIteratorEx) columnMajorIterator;
            columnMajor.advance(1); // mid-column cursor: row 1 of column 0
            assertEquals(5L, columnMajor.count());
            assertArrayEquals(new char[0], columnMajor.toArray());

            com.landawn.abacus.util.stream.CharIteratorEx columnMajorFresh = (com.landawn.abacus.util.stream.CharIteratorEx) matrix.columnMajorStream(0, 3)
                    .iterator();
            columnMajorFresh.advance(1);
            assertArrayEquals(new char[] { 'd', 'b', 'e', 'c', 'f' }, columnMajorFresh.toArray());

            var crossingIterator = matrix.columnMajorStream(0, 3).iterator();
            com.landawn.abacus.util.stream.CharIteratorEx crossing = (com.landawn.abacus.util.stream.CharIteratorEx) crossingIterator;
            crossing.advance(3); // crosses a column boundary: lands on row 1 of column 1
            assertEquals(3L, crossing.count());
            assertFalse(crossing.hasNext());
            crossing.advance(10);
            assertEquals(0L, crossing.count());
            assertThrows(java.util.NoSuchElementException.class, crossing::nextChar);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testRowMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = CharMatrix
                    .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
            final char[] expected = new char[] { (char) 1, (char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8, (char) 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (CharIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextChar);
            }
        }

        @Test
        public void testRowMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = CharMatrix
                    .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
            try (var stream = matrix.rowMajorStream()) {
                final var iterator = (CharIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((char) 1, iterator.nextChar());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextChar);
            }
        }

        @Test
        public void testRowMajorStreamSingleRowRange() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] row1 = matrix.rowMajorStream(1, 2).toArray();
            Assertions.assertArrayEquals(new char[] { 'd', 'e', 'f' }, row1);
        }

        @Test
        public void testRowMajorStream_Empty() {
            CharMatrix empty = CharMatrix.empty();
            assertEquals(0, empty.rowMajorStream().toArray().length);
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            char[] row1 = m.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new char[] { 'D', 'E', 'F' }, row1);
        }

        @Test
        public void testRowMajorStream_SingleRowRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            List<Character> row = m.rowMajorStream(0, 1).boxed().toList();
            assertEquals(3, row.size());
            assertEquals('a', row.get(0));
            assertEquals('b', row.get(1));
            assertEquals('c', row.get(2));
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<Character> row = m.rowMajorStream(1, 2).boxed().toList();
            assertEquals(2, row.size());
            assertEquals('c', row.get(0).charValue());
            assertEquals('d', row.get(1).charValue());
        }

        @Test
        public void testRowMajorStream_SingleRowRange_Shape2By2_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            char[] row = m.rowMajorStream(1, 2).toArray();
            assertArrayEquals(new char[] { 'c', 'd' }, row);
        }

        @Test
        public void testCharEmptyStreamRangesKeepBoundsValidationAndLogicalShapeCounts() {
            final CharMatrix matrix = new CharMatrix(new char[2][2]);
            assertEquals(0L, matrix.rowMajorStream(1, 1).count());
            assertEquals(0L, matrix.columnMajorStream(1, 1).count());
            assertEquals(0L, matrix.rowStreams(1, 1).count());
            assertEquals(0L, matrix.columnStreams(1, 1).count());
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowMajorStream(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnMajorStream(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.rowStreams(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.columnStreams(3, 3));

            final CharMatrix noColumns = new CharMatrix(new char[2][0]);
            assertEquals(2L, noColumns.rowStreams().count());
            noColumns.rowStreams().forEach(row -> assertEquals(0L, row.count()));
            assertEquals(0L, noColumns.mainDiagonalStream().count());
            assertEquals(0L, noColumns.antiDiagonalStream().count());

            final CharMatrix noRows = new CharMatrix(new char[0][], 3);
            assertEquals(3L, noRows.columnStreams().count());
            noRows.columnStreams().forEach(column -> assertEquals(0L, column.count()));
            assertEquals(0L, noRows.mainDiagonalStream().count());
            assertEquals(0L, noRows.antiDiagonalStream().count());
        }

        @Test
        public void testRowMajorStream_ZeroColumnsKeepLogicalRowRange() {
            final var matrix = CharMatrix.wrap(new char[3][0]);
            assertEquals(0, matrix.rowMajorStream(1, 3).count());
            assertEquals(2, matrix.rowStreams(1, 3).count());
        }

        @Test
        public void testRowMajorStream_WithSingleRowRange_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 3));
        }

        @Test
        public void testRowMajorStream_WithRange_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorStream(2, 1));
        }
    }

    @Nested
    class ColumnMajorStreamTests {

        @Test
        public void testColumnMajorStream() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] all = matrix.columnMajorStream().toArray();
            Assertions.assertArrayEquals(new char[] { 'a', 'd', 'b', 'e', 'c', 'f' }, all);
        }

        @Test
        public void testColumnMajorStreamRange() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] range = matrix.columnMajorStream(1, 3).toArray();
            Assertions.assertArrayEquals(new char[] { 'b', 'e', 'c', 'f' }, range);
        }

        @Test
        public void testColumnMajorStream_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            char[] all = m.columnMajorStream().toArray();
            assertArrayEquals(new char[] { 'A', 'D', 'B', 'E', 'C', 'F' }, all);
        }

        @Test
        public void testColumnMajorStream_WithRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            char[] columnCount = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new char[] { 'B', 'E', 'H', 'C', 'F', 'I' }, columnCount);
        }

        @Test
        public void testColumnMajorStream_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<Character> all = m.columnMajorStream().boxed().toList();
            assertEquals(4, all.size());
            assertEquals('a', all.get(0));
            assertEquals('c', all.get(1));
        }

        @Test
        public void testColumnMajorStream_ColumnRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            List<Character> columnCount = m.columnMajorStream(1, 3).boxed().toList();
            assertEquals(4, columnCount.size());
            assertEquals('b', columnCount.get(0));
            assertEquals('e', columnCount.get(1));
        }

        @Test
        public void testColumnMajorStream_Shape2By2_SizeAndFirstValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<Character> elements = m.columnMajorStream().boxed().toList();
            assertEquals(4, elements.size());
            assertEquals('a', elements.get(0).charValue());
            assertEquals('c', elements.get(1).charValue());
        }

        @Test
        public void testColumnMajorStream_WithRange_Shape2By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            List<Character> elements = m.columnMajorStream(1, 3).boxed().toList();
            assertEquals(4, elements.size());
            assertEquals('b', elements.get(0).charValue());
            assertEquals('e', elements.get(1).charValue());
        }

        // matrix is now [['A', 'b'], ['C', 'd']]
        @Test
        public void testVerticalStream() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            char[] elements = m.columnMajorStream().toArray();
            assertArrayEquals(new char[] { 'a', 'c', 'b', 'd' }, elements);
        }

        @Test
        public void testColumnMajorStream_ColumnRange_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            char[] elements = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new char[] { 'b', 'e', 'c', 'f' }, elements);
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 2, 3, 8, 9, Long.MAX_VALUE })
        public void testColumnMajorStream_AdvanceClampsToRemainingValues(final long amount) {
            final var matrix = CharMatrix
                    .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
            final char[] expected = new char[] { (char) 1, (char) 4, (char) 7, (char) 2, (char) 5, (char) 8, (char) 3, (char) 6, (char) 9 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (CharIteratorEx) stream.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextChar);
            }
        }

        @Test
        public void testColumnMajorStream_CountConsumesOnlyRemainingValues() {
            final var matrix = CharMatrix
                    .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
            try (var stream = matrix.columnMajorStream()) {
                final var iterator = (CharIteratorEx) stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((char) 1, iterator.nextChar());
                assertEquals(8, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextChar);
            }
        }

        @Test
        public void testColumnMajorStreamSingleColumnRange() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            char[] col1 = matrix.columnMajorStream(1, 2).toArray();
            Assertions.assertArrayEquals(new char[] { 'b', 'e' }, col1);
        }

        @Test
        public void testColumnMajorStream_Empty() {
            CharMatrix empty = CharMatrix.empty();
            assertEquals(0, empty.columnMajorStream().toArray().length);
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            char[] col1 = m.columnMajorStream(1, 2).toArray();
            assertArrayEquals(new char[] { 'B', 'E' }, col1);
        }

        @Test
        public void testColumnMajorStream_SingleColumnRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            List<Character> col = m.columnMajorStream(0, 1).boxed().toList();
            assertEquals(3, col.size());
            assertEquals('a', col.get(0));
            assertEquals('c', col.get(1));
            assertEquals('e', col.get(2));
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<Character> col = m.columnMajorStream(1, 2).boxed().toList();
            assertEquals(2, col.size());
            assertEquals('b', col.get(0).charValue());
            assertEquals('d', col.get(1).charValue());
        }

        @Test
        public void testColumnMajorStream_SingleColumnRange_Shape2By2_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            char[] col = m.columnMajorStream(0, 1).toArray();
            assertArrayEquals(new char[] { 'a', 'c' }, col);
        }

        @Test
        public void testColumnMajorStream_ZeroRowsKeepLogicalColumnRange() {
            final var matrix = CharMatrix.wrap(new char[0][0]).resize(0, 3);
            assertEquals(0, matrix.columnMajorStream(1, 3).count());
            assertEquals(2, matrix.columnStreams(1, 3).count());
        }

        @Test
        public void testColumnMajorStream_WithSingleColumnRange_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 3));
        }

        @Test
        public void testColumnMajorStream_WithRange_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorStream(2, 1));
        }
    }

    @Nested
    class RowStreamsTests {

        @Test
        public void testDocumentedStreamsReadLiveCellsAndPreserveProducedRowBinding() {
            final CharMatrix matrix = CharMatrix.wrap(new char[][] { { 1, 2 }, { 3, 4 } });
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
            matrix.set(1, 0, (char) 9);
            matrix.set(0, 1, (char) 8);

            assertArrayEquals(new char[] { 9, 2 }, row.toArray());
            assertArrayEquals(new char[] { 9, 2 }, selectedRow.toArray());
            assertArrayEquals(new char[] { 3, 9 }, column.toArray());
            assertArrayEquals(new char[] { 3, 9 }, selectedColumn.toArray());
            assertArrayEquals(new char[] { 3, 8, 9, 2 }, rowMajor.toArray());
            assertArrayEquals(new char[] { 3, 8 }, selectedRowMajor.toArray());
            assertArrayEquals(new char[] { 3, 9, 8, 2 }, columnMajor.toArray());
            assertArrayEquals(new char[] { 3, 9 }, selectedColumnMajor.toArray());
            assertArrayEquals(new char[] { 3, 2 }, mainDiagonal.toArray());
            assertArrayEquals(new char[] { 8, 9 }, antiDiagonal.toArray());
        }

        @Test
        public void testStreamR() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            List<CharStream> rows = matrix.rowStreams().toList();
            Assertions.assertEquals(2, rows.size());
            Assertions.assertArrayEquals(new char[] { 'a', 'b' }, rows.get(0).toArray());
            Assertions.assertArrayEquals(new char[] { 'c', 'd' }, rows.get(1).toArray());
        }

        @Test
        public void testRowStreamsRange() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            List<CharStream> rows = matrix.rowStreams(1, 3).toList();
            Assertions.assertEquals(2, rows.size());
            Assertions.assertArrayEquals(new char[] { 'c', 'd' }, rows.get(0).toArray());
            Assertions.assertArrayEquals(new char[] { 'e', 'f' }, rows.get(1).toArray());
        }

        @Test
        public void testRowStreams_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            List<char[]> rows = m.rowStreams().map(CharStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new char[] { 'A', 'B', 'C' }, rows.get(0));
            assertArrayEquals(new char[] { 'D', 'E', 'F' }, rows.get(1));
        }

        @Test
        public void testRowStreams_WithRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            List<char[]> rows = m.rowStreams(1, 3).map(CharStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new char[] { 'D', 'E', 'F' }, rows.get(0));
            assertArrayEquals(new char[] { 'G', 'H', 'I' }, rows.get(1));
        }

        @Test
        public void testRowStreams_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<CharStream> rows = m.rowStreams().toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).count());
        }

        @Test
        public void testRowStreams_RowRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            List<CharStream> rows = m.rowStreams(1, 3).toList();
            assertEquals(2, rows.size());
        }

        @Test
        public void testRowStreams_Shape2By2_OuterAndInnerStreamSizes() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<CharStream> rows = m.rowStreams().toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).toArray().length);
        }

        @Test
        public void testRowStreams() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<char[]> rows = m.rowStreams().map(CharStream::toArray).toList();
            assertEquals(2, rows.size());
            assertArrayEquals(new char[] { 'a', 'b' }, rows.get(0));
        }

        @Test
        public void testRowStreams_RowRange_Shape3By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' }, { 'e', 'f' } });
            List<char[]> rows = m.rowStreams(1, 3).map(CharStream::toArray).toList();
            assertEquals(2, rows.size());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testRowStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = CharMatrix
                    .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
            final char[][] expected = { new char[] { (char) 1, (char) 2, (char) 3 }, new char[] { (char) 4, (char) 5, (char) 6 },
                    new char[] { (char) 7, (char) 8, (char) 9 } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.rowStreams()) {
                final var iterator = (ObjIteratorEx<CharStream>) stream.iterator();
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
            CharMatrix empty = CharMatrix.empty();
            assertEquals(0, empty.rowStreams().count());
        }

        @Test
        public void testRowStreams_WithRange_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowStreams(0, 3));
        }
    }

    @Nested
    class ColumnStreamsTests {

        @Test
        public void testStreamC() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            List<CharStream> columnCount = matrix.columnStreams().toList();
            Assertions.assertEquals(3, columnCount.size());
            Assertions.assertArrayEquals(new char[] { 'a', 'd' }, columnCount.get(0).toArray());
            Assertions.assertArrayEquals(new char[] { 'b', 'e' }, columnCount.get(1).toArray());
            Assertions.assertArrayEquals(new char[] { 'c', 'f' }, columnCount.get(2).toArray());
        }

        @Test
        public void testColumnStreamsRange() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            List<CharStream> columnCount = matrix.columnStreams(1, 3).toList();
            Assertions.assertEquals(2, columnCount.size());
            Assertions.assertArrayEquals(new char[] { 'b', 'e' }, columnCount.get(0).toArray());
            Assertions.assertArrayEquals(new char[] { 'c', 'f' }, columnCount.get(1).toArray());
        }

        @Test
        public void testColumnStreams_Shape2By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });
            List<char[]> columnCount = m.columnStreams().map(CharStream::toArray).toList();
            assertEquals(3, columnCount.size());
            assertArrayEquals(new char[] { 'A', 'D' }, columnCount.get(0));
            assertArrayEquals(new char[] { 'B', 'E' }, columnCount.get(1));
            assertArrayEquals(new char[] { 'C', 'F' }, columnCount.get(2));
        }

        @Test
        public void testColumnStreams_WithRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            List<char[]> columnCount = m.columnStreams(1, 3).map(CharStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new char[] { 'B', 'E', 'H' }, columnCount.get(0));
            assertArrayEquals(new char[] { 'C', 'F', 'I' }, columnCount.get(1));
        }

        @Test
        public void testColumnStreams_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<CharStream> columnCount = m.columnStreams().toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).count());
        }

        @Test
        public void testColumnStreams_ColumnRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            List<CharStream> columnCount = m.columnStreams(1, 3).toList();
            assertEquals(2, columnCount.size());
        }

        @Test
        public void testColumnStreams_Shape2By2_OuterAndInnerStreamSizes() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<CharStream> columnCount = m.columnStreams().toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).toArray().length);
        }

        @Test
        public void testColumnStreams() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<char[]> columnCount = m.columnStreams().map(CharStream::toArray).toList();
            assertEquals(2, columnCount.size());
            assertArrayEquals(new char[] { 'a', 'c' }, columnCount.get(0));
        }

        @Test
        public void testColumnStreams_ColumnRange_Shape2By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            List<char[]> columnCount = m.columnStreams(0, 2).map(CharStream::toArray).toList();
            assertEquals(2, columnCount.size());
        }

        @ParameterizedTest
        @ValueSource(longs = { -1, 0, 1, 3, Long.MAX_VALUE })
        public void testColumnStreams_OuterCursorAdvanceCountAndExhaustion(final long amount) {
            final var matrix = CharMatrix
                    .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
            final char[][] expected = { new char[] { (char) 1, (char) 4, (char) 7 }, new char[] { (char) 2, (char) 5, (char) 8 },
                    new char[] { (char) 3, (char) 6, (char) 9 } };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var stream = matrix.columnStreams()) {
                final var iterator = (ObjIteratorEx<CharStream>) stream.iterator();
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
            final var matrix = CharMatrix
                    .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
            final char[] expected = new char[] { (char) 2, (char) 5, (char) 8 };
            final int skipped = (int) Math.max(0, Math.min(amount, expected.length));
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (CharIteratorEx) values.iterator();
                iterator.advance(amount);
                assertArrayEquals(Arrays.copyOfRange(expected, skipped, expected.length), iterator.toArray());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextChar);
            }
        }

        @Test
        public void testColumnStreams_InnerCountConsumesOnlyRemainingValues() {
            final var matrix = CharMatrix
                    .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
            try (var columns = matrix.columnStreams(1, 2);
                 var values = columns.iterator().next()) {
                final var iterator = (CharIteratorEx) values.iterator();
                assertTrue(iterator.hasNext());
                assertEquals((char) 2, iterator.nextChar());
                assertEquals(2, iterator.count());
                assertEquals(0, iterator.count());
                assertFalse(iterator.hasNext());
                assertThrows(NoSuchElementException.class, iterator::nextChar);
            }
        }

        @Test
        public void testColumnStreams_Empty() {
            CharMatrix empty = CharMatrix.empty();
            assertEquals(0, empty.columnStreams().count());
        }

        @Test
        public void testColumnStreams_WithRange_OutOfBounds() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnStreams(0, 3));
        }
    }

    @Nested
    class LengthTests {

        @Test
        public void testLength() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
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
                final CharMatrix matrix = CharMatrix.wrap(new char[2][3]);
                final AtomicInteger calls = new AtomicInteger();
                final IllegalStateException failure = new IllegalStateException("action failed");
                final Throwables.CharConsumer<IllegalStateException> action = value -> {
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
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            List<Character> collected = new ArrayList<>();
            matrix.forEach(c -> collected.add(c));

            Assertions.assertEquals(4, collected.size());
            Assertions.assertTrue(collected.contains('a'));
            Assertions.assertTrue(collected.contains('b'));
            Assertions.assertTrue(collected.contains('c'));
            Assertions.assertTrue(collected.contains('d'));
        }

        @Test
        public void testForEachWithRange() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            List<Character> collected = new ArrayList<>();
            matrix.forEach(1, 3, 1, 3, c -> collected.add(c));

            Assertions.assertEquals(4, collected.size());
            Assertions.assertTrue(collected.contains('e'));
            Assertions.assertTrue(collected.contains('f'));
            Assertions.assertTrue(collected.contains('h'));
            Assertions.assertTrue(collected.contains('i'));

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.forEach(0, 4, 0, 3, c -> {
            }));
        }

        @Test
        public void testForEach_ValueConsumer() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<Character> values = new ArrayList<>();
            m.forEach(c -> values.add(c));
            assertEquals(4, values.size());
            assertEquals('a', values.get(0));
            assertEquals('b', values.get(1));
        }

        @Test
        public void testForEach_WithRange() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            List<Character> values = new ArrayList<>();
            m.forEach(1, 3, 1, 3, c -> values.add(c));
            assertEquals(4, values.size());
            assertEquals('e', values.get(0));
            assertEquals('f', values.get(1));
        }

        @Test
        public void testForEach_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            StringBuilder sb = new StringBuilder();
            m.forEach(val -> sb.append(val));
            assertEquals("abcd", sb.toString());
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testForEach_ExplicitModesVisitSelectedRegionExactlyOnce(final ParallelMode mode) {
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                final var matrix = CharMatrix
                        .wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } });
                final var values = new ConcurrentLinkedQueue<Character>();
                matrix.forEach(0, 2, 1, 3, values::add);
                assertEquals(List.of((char) 2, (char) 3, (char) 5, (char) 6).stream().sorted().toList(), values.stream().sorted().toList());
                assertEquals(
                        CharMatrix.wrap(new char[][] { { (char) 1, (char) 2, (char) 3 }, { (char) 4, (char) 5, (char) 6 }, { (char) 7, (char) 8, (char) 9 } }),
                        matrix);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testForEachNullAction() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.forEach((Throwables.CharConsumer<RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.forEach(0, 1, 0, 2, (Throwables.CharConsumer<RuntimeException>) null));
        }
    }

    @Nested
    class ToMultilineStringTests {

        @Test
        public void testToMultilineString_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            String result = m.toMultilineString();
            assertNotNull(result);
            assertTrue(result.contains("a"));
            assertTrue(result.contains("d"));
        }

        @Test
        public void testToMultilineStringEmptyReturnsBracketString() {
            // Empty path returns "[]" directly.
            CharMatrix empty = CharMatrix.empty();
            String s = empty.toMultilineString();
            Assertions.assertEquals("[]", s);
        }

        @Test
        public void testToMultilineStringSingleRowProducesNonNullString() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            String s = matrix.toMultilineString();
            Assertions.assertNotNull(s);
            Assertions.assertTrue(s.contains("['a', 'b']"));
        }

        @Test
        public void testToMultilineStringMultipleRowsProducesNonNullString() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            String s = matrix.toMultilineString();
            Assertions.assertNotNull(s);
            Assertions.assertTrue(s.contains("['a', 'b']"));
            Assertions.assertTrue(s.contains("['c', 'd']"));
        }
    }

    @Nested
    class AppendElementForOutputTests {

        @Test
        public void testCharacterOutputChecksDestinationThenRequiredCharacterWithoutWriting() {
            final CharMatrix matrix = CharMatrix.empty();
            final String nullOutputMessage = assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(null, 'a')).getMessage();
            assertEquals(nullOutputMessage, assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(null, null)).getMessage());
            assertEquals(nullOutputMessage, assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(null, "wrong type")).getMessage());

            final StringBuilder output = new StringBuilder("untouched");
            assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(output, null));
            assertThrows(ClassCastException.class, () -> matrix.appendElementForOutput(output, "wrong type"));
            assertEquals("untouched", output.toString());
        }

        @Test
        public void testCharacterRenderingValidatesInputsBeforeConversionAndOutput() {
            final CharMatrix matrix = CharMatrix.empty();
            assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(null, "not a character"));
            assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(new StringBuilder(), null));
            assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(null, 'a'));
            assertThrows(ClassCastException.class, () -> matrix.appendElementForOutput(new StringBuilder(), "not a character"));

            final IOException failure = new IOException("destination unavailable");
            final Appendable output = new Appendable() {
                @Override
                public Appendable append(final CharSequence value) throws IOException {
                    throw failure;
                }

                @Override
                public Appendable append(final CharSequence value, final int start, final int end) throws IOException {
                    throw failure;
                }

                @Override
                public Appendable append(final char value) throws IOException {
                    throw failure;
                }
            };
            assertSame(failure, assertThrows(IOException.class, () -> matrix.appendElementForOutput(output, 'a')));
        }
    }

    @Nested
    class HashCodeTests {

        @Test
        public void testHashCode() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix1 = CharMatrix.wrap(a);
            CharMatrix matrix2 = CharMatrix.wrap(a.clone());

            // Same content should have same hash code
            Assertions.assertEquals(matrix1.hashCode(), matrix2.hashCode());
        }

        @Test
        public void testHashCode_Shape2By2() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix m3 = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'D', 'C' } });

            assertEquals(m1.hashCode(), m2.hashCode());
            assertNotEquals(m1.hashCode(), m3.hashCode()); // Usually different
        }

        @Test
        public void testHashCode_Equal() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertEquals(m1.hashCode(), m2.hashCode());
        }
    }

    @Nested
    class EqualsTests {

        @Test
        public void testEquals() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix1 = CharMatrix.wrap(a);
            CharMatrix matrix2 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix matrix3 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'e' } });
            CharMatrix matrix4 = CharMatrix.wrap(new char[][] { { 'a', 'b' } });

            assertTrue(matrix1.equals(matrix1)); // same instance
            Assertions.assertEquals(matrix1, matrix2); // same content
            Assertions.assertNotEquals(matrix1, matrix3); // different content
            Assertions.assertNotEquals(matrix1, matrix4); // different dimensions
            Assertions.assertNotEquals(matrix1, null);
            Assertions.assertNotEquals(matrix1, "not a matrix");
        }

        @Test
        public void testEquals_Same() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            assertTrue(m.equals(m));
        }

        @Test
        public void testEquals_Equal() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            assertEquals(m1, m2);
        }

        @Test
        public void testEquals_NotEqual() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'c', 'd' } });
            assertNotEquals(m1, m2);
        }

        @Test
        public void testEquals_DifferentType() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a' } });
            assertNotEquals(m, "string");
        }

        @Test
        public void testEquals_DifferentRowCountsWithEqualColumnCounts() {
            final var longer = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 }, { (char) 3, (char) 4 } });
            final var shorter = CharMatrix.wrap(new char[][] { { (char) 1, (char) 2 } });
            assertFalse(longer.equals(shorter));
            assertFalse(shorter.equals(longer));
        }

        @Test
        public void testEquals_Shape2By2_NullInput() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            CharMatrix m3 = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'D', 'C' } });
            CharMatrix m4 = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' } });

            assertTrue(m1.equals(m1)); // Same object
            assertTrue(m1.equals(m2)); // Same values
            assertFalse(m1.equals(m3)); // Different values
            assertFalse(m1.equals(m4)); // Different dimensions
            assertFalse(m1.equals(null));
            assertFalse(m1.equals("not a matrix"));
        }

        @Test
        public void testEquals_Shape2By2_NullInput_DifferentValuesNullAndType() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix m3 = CharMatrix.wrap(new char[][] { { 'w', 'x' }, { 'y', 'z' } });
            assertEquals(m1, m2);
            assertNotEquals(m1, m3);
            assertNotEquals(m1, null);
            assertNotEquals(m1, "not a matrix");
        }

        @Test
        public void testEquals_Null() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a' } });
            assertNotEquals(m, null);
        }

        @Test
        public void testEquals_EmptyMatrices() {
            CharMatrix m1 = CharMatrix.empty();
            CharMatrix m2 = CharMatrix.empty();
            assertEquals(m1, m2);
        }
    }

    @Nested
    class ToStringTests {

        @Test
        public void testToString() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            String str = matrix.toString();
            Assertions.assertNotNull(str);
            Assertions.assertTrue(str.contains("a"));
            Assertions.assertTrue(str.contains("d"));
        }

        @Test
        public void testToString_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' }, { 'C', 'D' } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("A"));
            assertTrue(str.contains("B"));
            assertTrue(str.contains("C"));
            assertTrue(str.contains("D"));
        }

        @Test
        public void testToString_Shape2By2_ContainsElementText() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            String str = m.toString();
            assertNotNull(str);
            assertTrue(str.contains("a"));
            assertTrue(str.contains("d"));
        }

        @Test
        public void testCharacterEscapingMatchesTheEstablishedFormatForEveryCodeUnit() {
            final char[] values = new char[Character.MAX_VALUE + 1];
            final StringBuilder row = new StringBuilder("[");

            for (int i = 0; i < values.length; i++) {
                final char value = (char) i;
                values[i] = value;

                if (i > 0) {
                    row.append(", ");
                }

                row.append('\'');

                final String special = switch (value) {
                    case '\b' -> "\\b";
                    case '\t' -> "\\t";
                    case '\n' -> "\\n";
                    case '\f' -> "\\f";
                    case '\r' -> "\\r";
                    case '\'' -> "\\'";
                    case '\\' -> "\\\\";
                    default -> null;
                };

                if (special != null) {
                    row.append(special);
                } else if (value >= 0x20 && value <= 0x7e) {
                    row.append(value);
                } else {
                    // An independent formatter supplies the oracle for all four uppercase hex digits.
                    row.append('\\').append('u').append(String.format(Locale.ROOT, "%04X", i));
                }

                row.append('\'');
            }

            row.append(']');
            final String expectedRow = row.toString();
            final CharMatrix matrix = CharMatrix.wrap(values);
            assertEquals("[" + expectedRow + "]", matrix.toString());
            assertEquals(expectedRow, matrix.toMultilineString());

            final StringBuilder appended = new StringBuilder("prefix:");
            matrix.appendTo(appended);
            assertEquals("prefix:" + expectedRow, appended.toString());
        }
    }

    @Nested
    class ElementTypeTests {

        @Test
        public void testComponentType() {
            CharMatrix matrix = CharMatrix.empty();
            Assertions.assertEquals(char.class, matrix.elementType());
        }

        @Test
        public void testElementType_Shape1By1() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A' } });
            assertEquals(char.class, m.elementType());
        }

        @Test
        public void testElementType_Shape1By1_PrimitiveElementClass() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a' } });
            assertEquals(char.class, m.elementType());
        }

        @Test
        public void testElementType() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            assertEquals(char.class, m.elementType());
        }

        @Test
        public void testElementType_EmptyMatrix() {
            CharMatrix m = CharMatrix.empty();
            assertEquals(char.class, m.elementType());
        }
    }

    @Nested
    class UnsafeBackingArrayTests {

        @Test
        public void testArray() {
            char[][] arr = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix m = CharMatrix.wrap(arr);
            char[][] result = m.unsafeBackingArray();

            // The outer array is a private snapshot, so replacing a row in either array is invisible
            // to the other; the row arrays themselves are still shared, so cell writes are visible.
            assertNotSame(arr, result);
            assertSame(arr[0], result[0]);
            assertSame(arr[1], result[1]);

            arr[0][0] = 'z';
            assertEquals('z', m.get(0, 0));
        }

        @Test
        public void testArray_ArrayValues() {
            char[][] arr = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix m = CharMatrix.wrap(arr);
            char[][] result = m.unsafeBackingArray();
            assertArrayEquals(arr, result);
        }
    }

    @Nested
    class ElementCountTests {

        @Test
        public void testElementCount() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            assertEquals(6, m.elementCount());
        }

        @Test
        public void testElementCount_Empty() {
            CharMatrix m = CharMatrix.empty();
            assertEquals(0, m.elementCount());
        }
    }

    @Nested
    class IsEmptyTests {

        @Test
        public void testIsEmpty() {
            CharMatrix empty = CharMatrix.empty();
            assertTrue(empty.isEmpty());
            CharMatrix notEmpty = CharMatrix.wrap(new char[][] { { 'a' } });
            assertFalse(notEmpty.isEmpty());
        }

        @Test
        public void testIsEmpty_Shape1By1() {
            assertTrue(CharMatrix.empty().isEmpty());
            assertFalse(CharMatrix.wrap(new char[][] { { 'a' } }).isEmpty());
        }
    }

    @Nested
    class IsSameShapeTests {

        @Test
        public void testIsSameShape() {
            CharMatrix m1 = CharMatrix.wrap(new char[2][3]);
            CharMatrix m2 = CharMatrix.wrap(new char[2][3]);
            CharMatrix m3 = CharMatrix.wrap(new char[3][2]);
            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }

        @Test
        public void testIsSameShape_Shape2By2() {
            CharMatrix m1 = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix m2 = CharMatrix.wrap(new char[][] { { 'w', 'x' }, { 'y', 'z' } });
            CharMatrix m3 = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' } });
            assertTrue(m1.isSameShape(m2));
            assertFalse(m1.isSameShape(m3));
        }
    }

    @Nested
    class ReshapeTests {

        @Test
        public void testReshape() {
            char[][] a = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            CharMatrix reshaped = matrix.reshape(3, 2);
            Assertions.assertEquals(3, reshaped.rowCount());
            Assertions.assertEquals(2, reshaped.columnCount());
            Assertions.assertEquals('a', reshaped.get(0, 0));
            Assertions.assertEquals('b', reshaped.get(0, 1));
            Assertions.assertEquals('c', reshaped.get(1, 0));
            Assertions.assertEquals('d', reshaped.get(1, 1));

            CharMatrix empty = CharMatrix.empty().reshapeAndPad(2, 3);
            Assertions.assertEquals(2, empty.rowCount());
            Assertions.assertEquals(3, empty.columnCount());

            // Test reshape with too-small dimensions throws exception
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.reshape(1, 4));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.reshape(2, 2));
        }

        @Test
        public void testReshape_Shape3By3_ArrayValues() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            CharMatrix reshaped = m.reshape(1, 9);
            assertEquals(1, reshaped.rowCount());
            assertEquals(9, reshaped.columnCount());
            assertArrayEquals(new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I' }, reshaped.rowView(0));
        }

        @Test
        public void testReshape_Back() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B', 'C' }, { 'D', 'E', 'F' }, { 'G', 'H', 'I' } });
            CharMatrix reshaped = m.reshape(1, 9);
            CharMatrix reshapedBack = reshaped.reshape(3, 3);
            assertEquals(m, reshapedBack);
        }

        @Test
        public void testReshape_Shape1By4() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c', 'd' } });
            CharMatrix reshaped = m.reshape(2, 2);
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals('a', reshaped.get(0, 0));
            assertEquals('b', reshaped.get(0, 1));
            assertEquals('c', reshaped.get(1, 0));
            assertEquals('d', reshaped.get(1, 1));
        }

        @Test
        public void testReshape_Shape2By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            CharMatrix reshaped = m.reshape(3, 2);
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals('a', reshaped.get(0, 0));
            assertEquals('b', reshaped.get(0, 1));
        }

        @Test
        public void testReshape_ExactAndPaddedRectangleValues() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } });
            CharMatrix reshaped1 = matrix.reshape(3, 2);
            // Result: [['a', 'b'], ['c', 'd'], ['e', 'f']]
            assertEquals('a', reshaped1.get(0, 0));
            assertEquals('b', reshaped1.get(0, 1));
            assertEquals('c', reshaped1.get(1, 0));
            assertEquals('d', reshaped1.get(1, 1));
            assertEquals('e', reshaped1.get(2, 0));
            assertEquals('f', reshaped1.get(2, 1));

            CharMatrix reshaped2 = matrix.reshapeAndPad(2, 4);
            // Result: [['a', 'b', 'c', 'd'], ['e', 'f', '\u0000', '\u0000']]
            assertEquals('a', reshaped2.get(0, 0));
            assertEquals('d', reshaped2.get(0, 3));
            assertEquals('e', reshaped2.get(1, 0));
            assertEquals('f', reshaped2.get(1, 1));
            assertEquals('\u0000', reshaped2.get(1, 2));
            assertEquals('\u0000', reshaped2.get(1, 3));
        }

        @Test
        public void testReshape_ToSingleRow() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            CharMatrix reshaped = m.reshape(1, 4);
            assertEquals(1, reshaped.rowCount());
            assertEquals(4, reshaped.columnCount());
        }
    }

    @Nested
    class ReshapeAndPadToColumnCountTests {

        @Test
        public void testReshape_OneArg() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c', 'd' } });
            CharMatrix reshaped = m.reshapeAndPadToColumnCount(2);
            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals('a', reshaped.get(0, 0));
            assertEquals('b', reshaped.get(0, 1));
            assertEquals('c', reshaped.get(1, 0));
            assertEquals('d', reshaped.get(1, 1));
        }
    }

    @Nested
    class ForEachIndicesTests {

        @Test
        public void testForEach_BiConsumer() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            StringBuilder diagonalStr = new StringBuilder();
            m.forEachIndices((i, j, val) -> {
                if (i == j) {
                    diagonalStr.append(val.get(i, j));
                }
            });
            assertEquals("ad", diagonalStr.toString());
        }
    }

    @Nested
    class Adjacent4PointsTests {

        @Test
        public void testAdjacent4Points() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            List<Point> points = m.adjacent4Points(1, 1).toList();
            assertEquals(4, points.size());
        }

        @Test
        public void testAdjacent4Points_Corner() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<Point> points = m.adjacent4Points(0, 0).toList();
            assertEquals(2, points.size());
        }
    }

    @Nested
    class Adjacent8PointsTests {

        @Test
        public void testAdjacent8Points() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
            List<Point> points = m.adjacent8Points(1, 1).toList();
            assertEquals(8, points.size());
        }

        @Test
        public void testAdjacent8Points_Corner() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<Point> points = m.adjacent8Points(0, 0).toList();
            assertEquals(3, points.size());
        }
    }

    @Nested
    class MainDiagonalPointsTests {

        @Test
        public void testMainDiagonalPoints() {
            CharMatrix m = CharMatrix.wrap(new char[3][3]);
            List<Point> points = m.mainDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(2, 2), points.get(2));
        }

        @Test
        public void testMainDiagonalPoints_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
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
            CharMatrix m = CharMatrix.wrap(new char[3][3]);
            List<Point> points = m.antiDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(Point.of(0, 2), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(2, 0), points.get(2));
        }

        @Test
        public void testAntiDiagonalPoints_Shape3By3() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' }, { 'g', 'h', 'i' } });
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
            CharMatrix m = CharMatrix.wrap(new char[2][2]);
            List<Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(0, 1), points.get(1));
            assertEquals(Point.of(1, 0), points.get(2));
        }

        @Test
        public void testRowMajorPoints_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<Point> points = m.rowMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }

        @Test
        public void testRowMajorPoints_SingleRow() {
            CharMatrix m = CharMatrix.wrap(new char[2][3]);
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
            CharMatrix m = CharMatrix.wrap(new char[2][2]);
            List<Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 0), points.get(1));
        }

        @Test
        public void testColumnMajorPoints_Shape2By2() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            List<Point> points = m.columnMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(0, points.get(0).rowIndex());
            assertEquals(0, points.get(0).columnIndex());
        }

        @Test
        public void testColumnMajorPoints_SingleColumn() {
            CharMatrix m = CharMatrix.wrap(new char[3][2]);
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
            CharMatrix m = CharMatrix.wrap(new char[2][2]);
            List<Stream<Point>> rows = m.rowPoints().toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).count());
        }
    }

    @Nested
    class ColumnPointsTests {

        @Test
        public void testColumnPoints() {
            CharMatrix m = CharMatrix.wrap(new char[2][2]);
            List<Stream<Point>> columnCount = m.columnPoints().toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).count());
        }
    }

    @Nested
    class AcceptTests {

        @Test
        public void testAccept() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            AtomicInteger counter = new AtomicInteger(0);
            m.accept(matrix -> counter.set(matrix.rowCount() * matrix.columnCount()));
            assertEquals(2, counter.get());
        }
    }

    @Nested
    class ApplyTests {

        @Test
        public void testApply() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            int result = m.apply(matrix -> matrix.rowCount() * matrix.columnCount());
            assertEquals(2, result);
        }
    }

    @Nested
    class AppendToTests {

        @Test
        public void testEscapedDiagnosticsAndAppendTo() {
            CharMatrix matrix = CharMatrix.wrap(new char[][] { { '\n', '\t', '\'', '\\', (char) 0, (char) 0xd800, (char) 0x00e9 } });
            String multiline = matrix.toMultilineString();

            assertFalse(multiline.contains("\n"));
            assertTrue(multiline.contains("\\n"));
            assertTrue(multiline.contains("\\t"));
            assertTrue(multiline.contains("\\'"));
            assertTrue(multiline.contains("\\\\"));
            assertTrue(multiline.contains("\\" + "u0000"));
            assertTrue(multiline.contains("\\" + "uD800"));
            assertTrue(multiline.contains("\\" + "u00E9"));
            assertEquals("[" + multiline + "]", matrix.toString());

            StringBuilder output = new StringBuilder("prefix:");
            matrix.appendTo(output);
            assertEquals("prefix:" + multiline, output.toString());
        }
    }

    @Nested
    class PrintlnTests {

        @Test
        public void testPrintln() {
            char[][] a = { { 'a', 'b' }, { 'c', 'd' } };
            CharMatrix matrix = CharMatrix.wrap(a);

            assertFalse(matrix.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> matrix.println());
        }
    }
}
