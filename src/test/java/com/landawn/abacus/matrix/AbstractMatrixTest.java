package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.stream.Stream;

class AbstractMatrixTest extends MatrixTestSupport {

    private IntMatrix createTestMatrix() {
        return IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
    }

    private IntMatrix createTestMatrix2x3() {
        return IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
    }

    private IntMatrix createTestMatrix3x2() {
        return IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
    }

    @SuppressWarnings("deprecation")
    private static List<AbstractMatrix<?, ?, ?, ?, ?>> allNineTypes(final int rowCount, final int columnCount) {
        // copyRows(0, rowCount) keeps the logical column count even when rowCount == 0.
        final int backingRows = Math.max(rowCount, 1);
        final List<AbstractMatrix<?, ?, ?, ?, ?>> result = new ArrayList<>();
        result.add(BooleanMatrix.wrap(new boolean[backingRows][columnCount]).copyRows(0, rowCount));
        result.add(ByteMatrix.wrap(new byte[backingRows][columnCount]).copyRows(0, rowCount));
        result.add(CharMatrix.wrap(new char[backingRows][columnCount]).copyRows(0, rowCount));
        result.add(ShortMatrix.wrap(new short[backingRows][columnCount]).copyRows(0, rowCount));
        result.add(IntMatrix.wrap(new int[backingRows][columnCount]).copyRows(0, rowCount));
        result.add(LongMatrix.wrap(new long[backingRows][columnCount]).copyRows(0, rowCount));
        result.add(FloatMatrix.wrap(new float[backingRows][columnCount]).copyRows(0, rowCount));
        result.add(DoubleMatrix.wrap(new double[backingRows][columnCount]).copyRows(0, rowCount));
        result.add(Matrix.wrap(Integer.class, new Integer[backingRows][columnCount]).copyRows(0, rowCount));
        return result;
    }

    private static void assertShape(final String context, final AbstractMatrix<?, ?, ?, ?, ?> matrix, final int rowCount, final int columnCount) {
        assertEquals(rowCount + "x" + columnCount, matrix.rowCount() + "x" + matrix.columnCount(), context);
    }

    @Nested
    class ConstructorTests {

        @Test
        public void testConstructor() {
            IntMatrix matrix = createTestMatrix();
            Assertions.assertEquals(3, matrix.rowCount());
            Assertions.assertEquals(3, matrix.columnCount());
            Assertions.assertEquals(9, matrix.elementCount());
        }

        @Test
        public void testConstructorWithInconsistentRows() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4, 5 } });
            });
        }

        @Test
        public void testConstructor_InconsistentRowLengths() {
            int[][] jagged = new int[][] { { 1, 2, 3 }, { 4, 5 }, { 7, 8, 9 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(jagged));
        }

        @Test
        public void testConstructor_InconsistentRowLengths_ThreeRows() {
            int[][] jagged = new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6, 7 } };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(jagged));
        }

        @Test
        public void testFamilyConventions_StaticZipCounterpartsCoverOnlyByteIntLongDoubleAndMatrix() {
            final java.util.Set<Class<?>> zipOperandTypes = new java.util.HashSet<>();

            for (final java.lang.reflect.Method method : Matrices.class.getMethods()) {
                if (!method.getName().startsWith("zip")) {
                    continue;
                }

                for (final java.lang.reflect.Type type : method.getGenericParameterTypes()) {
                    java.lang.reflect.Type operand = type;

                    if (type instanceof java.lang.reflect.ParameterizedType p && p.getRawType() == java.util.Collection.class) {
                        operand = p.getActualTypeArguments()[0];
                    }

                    if (operand instanceof java.lang.reflect.ParameterizedType p) {
                        operand = p.getRawType();
                    }

                    if (operand instanceof Class<?> cls && AbstractMatrix.class.isAssignableFrom(cls)) {
                        zipOperandTypes.add(cls);
                    }
                }
            }

            assertEquals(java.util.Set.of(ByteMatrix.class, IntMatrix.class, LongMatrix.class, DoubleMatrix.class, Matrix.class), zipOperandTypes);

            for (final Class<?> cls : List.of(BooleanMatrix.class, CharMatrix.class, ShortMatrix.class, FloatMatrix.class)) {
                assertTrue(java.util.Arrays.stream(cls.getMethods()).filter(m -> m.getName().equals("zipWith")).count() >= 2, cls.getSimpleName());
            }
        }

        @Test
        public void testConstructor_SingleRowArray() {
            int[][] singleRow = new int[][] { { 1, 2, 3 } };
            IntMatrix m = IntMatrix.wrap(singleRow);
            assertEquals(1, m.rowCount());
            assertEquals(3, m.columnCount());
        }

        @Test
        public void testConstructor_EmptyRowsInArray() {
            int[][] emptyRows = new int[][] { {}, {} };
            IntMatrix m = IntMatrix.wrap(emptyRows);
            assertEquals(2, m.rowCount());
            assertEquals(0, m.columnCount());
            assertEquals(0, m.elementCount());
        }

        @Test
        public void testWrapRejectsDuplicateRows() {
            int[] sharedRow = { 1 };
            int[][] backingArray = { sharedRow, sharedRow };

            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(backingArray));
            assertThrows(IllegalArgumentException.class, () -> new IntMatrix(backingArray));
        }

        @Test
        public void testConstructor_RejectsNegativeExplicitColumnCount() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrapResult(new int[0][], -1));
        }
    }

    @Nested
    class SnapshotRowsIfBackingRowsTests {

        @Test
        public void testRowSnapshotRejectsNullOnlyWhenThereAreBackingRows() {
            assertNull(IntMatrix.empty().snapshotRowsIfBackingRows(null, 0, 0));
            assertNull(IntMatrix.empty().resize(0, 3).snapshotRowsIfBackingRows(null, 0, 0));

            final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> IntMatrix.wrap(new int[] { 1 }).snapshotRowsIfBackingRows(null, 0, 0));
            assertTrue(failure.getMessage().contains(cs.source));
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(new int[0]).snapshotRowsIfBackingRows(null, 0, 0));

            // Two destination rows, so the multi-row path (not the single-row shortcut) must hand back the
            // original array when its null and foreign rows alias nothing.
            final IntMatrix matrix = IntMatrix.wrap(new int[] { 1 }, new int[] { 3 });
            final int[][] rows = { null, new int[] { 2 } };
            assertSame(rows, matrix.snapshotRowsIfBackingRows(rows, 0, 0));
            assertNull(matrix.snapshotIfBackingRow(null));
        }
    }

    @Nested
    class ToArrayLengthTests {

        @Test
        public void testToArrayLength_ReportsNegativeAndOversizedCountsPrecisely() {
            IllegalStateException negative = assertThrows(IllegalStateException.class, () -> AbstractMatrix.toArrayLength(-1));
            IllegalStateException oversized = assertThrows(IllegalStateException.class, () -> AbstractMatrix.toArrayLength((long) Integer.MAX_VALUE + 1));

            assertEquals("Matrix stream element count cannot be negative: -1", negative.getMessage());
            assertEquals("Matrix stream too large to convert to array: 2147483648 elements", oversized.getMessage());
        }
    }

    @Nested
    class CheckNonNegativeShapeTests {

        @Test
        public void testRepresentableShapeRejectsNegativeDimensions() {
            assertThrows(IllegalArgumentException.class, () -> AbstractMatrix.checkNonNegativeShape(-1, 0));
            assertThrows(IllegalArgumentException.class, () -> AbstractMatrix.checkNonNegativeShape(1, -1));
        }
    }

    @Nested
    class FormatMsgTests {

        @Test
        public void testFormatMsg_RendersOnlyArgumentsWithMatchingPlaceholders() {
            final IllegalStateException failure = new IllegalStateException("argument rendering failed");
            final Object failingArgument = new Object() {
                @Override
                public String toString() {
                    throw failure;
                }
            };

            // A diagnostic must not render surplus arguments or arguments of a null template.
            Assertions.assertNull(AbstractMatrix.formatMsg(null, failingArgument));
            assertEquals("literal", AbstractMatrix.formatMsg("literal", failingArgument));
            assertEquals("value: 7", AbstractMatrix.formatMsg("value: {}", 7, failingArgument));
            assertEquals("null / {}", AbstractMatrix.formatMsg("{} / {}", (Object) null));
            assertSame(failure, assertThrows(IllegalStateException.class, () -> AbstractMatrix.formatMsg("value: {}", failingArgument)));
        }

        @Test
        public void testFormatMsg_NullTemplateOrArgumentsRemainUnchanged() {
            assertNull(AbstractMatrix.formatMsg(null, "unused"));
            assertEquals("value {}", AbstractMatrix.formatMsg("value {}", (Object[]) null));
            assertEquals("value {}", AbstractMatrix.formatMsg("value {}"));
        }
    }

    @Nested
    class ElementTypeTests {

        @Test
        public void testElementType() {
            IntMatrix matrix = createTestMatrix();
            Assertions.assertEquals(int.class, matrix.elementType());
        }

        @Test
        public void testComponentType_doubleMatrix() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 } });
            assertEquals(double.class, m.elementType());
        }

        @Test
        public void testElementType_ObjectMatrix() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "a", "b" } });
            assertEquals(String.class, m.elementType());
        }

        @Test
        public void testComponentType_intMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertEquals(int.class, m.elementType());
        }

        @Test
        public void testElementType_LongMatrix() {
            LongMatrix m = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            assertEquals(long.class, m.elementType());
        }

        @Test
        public void testElementType_FloatMatrix() {
            FloatMatrix m = FloatMatrix.wrap(new float[][] { { 1.0f, 2.0f } });
            assertEquals(float.class, m.elementType());
        }

        @Test
        public void testElementType_ByteMatrix() {
            ByteMatrix m = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            assertEquals(byte.class, m.elementType());
        }

        @Test
        public void testElementType_ShortMatrix() {
            ShortMatrix m = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            assertEquals(short.class, m.elementType());
        }

        @Test
        public void testElementType_CharMatrix() {
            CharMatrix m = CharMatrix.wrap(new char[][] { { 'A', 'B' } });
            assertEquals(char.class, m.elementType());
        }

        @Test
        public void testComponentType_booleanMatrix() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            assertEquals(boolean.class, m.elementType());
        }

        @Test
        public void testElementType_IntMatrix() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(int.class, matrix.elementType());
        }

        // From elementType Javadoc
        @Test
        public void testElementType_DoubleMatrix() {
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            assertEquals(double.class, matrix.elementType());
        }

        // From rowView Javadoc
        @Test
        public void testElementType_BooleanMatrix() {
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            assertEquals(boolean.class, matrix.elementType());
        }

        @Test
        public void testelementType() {
            // From elementType Javadoc
            IntMatrix intMatrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(int.class, intMatrix.elementType()); // Returns int.class
        }
    }

    @Nested
    class UnsafeBackingArrayTests {

        @Test
        public void testArray() {
            IntMatrix matrix = createTestMatrix();
            int[][] array = matrix.unsafeBackingArray();
            Assertions.assertEquals(3, array.length);
            Assertions.assertEquals(3, array[0].length);
            Assertions.assertEquals(1, array[0][0]);
        }

        @Test
        public void testArray_IntMatrix() {
            int[][] arr = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.wrap(arr);
            int[][] returnedArray = m.unsafeBackingArray();

            assertNotNull(returnedArray);
            assertEquals(2, returnedArray.length);
            assertArrayEquals(new int[] { 1, 2 }, returnedArray[0]);
            assertArrayEquals(new int[] { 3, 4 }, returnedArray[1]);
        }

        @Test
        public void testArray_DoubleMatrix() {
            double[][] arr = { { 1.5, 2.5 }, { 3.5, 4.5 } };
            DoubleMatrix m = DoubleMatrix.wrap(arr);
            double[][] returnedArray = m.unsafeBackingArray();

            assertNotNull(returnedArray);
            assertEquals(2, returnedArray.length);
            assertArrayEquals(new double[] { 1.5, 2.5 }, returnedArray[0]);
            assertArrayEquals(new double[] { 3.5, 4.5 }, returnedArray[1]);
        }

        @Test
        public void testArray_ObjectMatrix() {
            String[][] arr = { { "A", "B" }, { "C", "D" } };
            Matrix<String> m = Matrix.wrap(arr);
            String[][] returnedArray = m.unsafeBackingArray();

            assertNotNull(returnedArray);
            assertEquals(2, returnedArray.length);
            assertArrayEquals(new String[] { "A", "B" }, returnedArray[0]);
            assertArrayEquals(new String[] { "C", "D" }, returnedArray[1]);
        }

        @Test
        public void testArray_ReturnsStructuralSnapshotSharingRows() {
            int[][] arr = { { 1, 2 }, { 3, 4 } };
            IntMatrix matrix = IntMatrix.wrap(arr);
            int[][] returned = matrix.unsafeBackingArray();
            assertNotSame(arr, returned);
            assertSame(arr[0], returned[0]);
            returned[0][0] = 999;
            assertEquals(999, matrix.get(0, 0));
            returned[1] = returned[0];
            assertArrayEquals(new int[] { 3, 4 }, matrix.rowView(1));
        }

        // From rotate180 Javadoc
        @Test
        public void testUnsafeBackingArray() {
            // From unsafeBackingArray Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[][] array = matrix.unsafeBackingArray();
            array[0][0] = 10; // This WILL modify the matrix!
            assertEquals(10, matrix.get(0, 0));
        }

        @Test
        public void testBackingArraySharesRowsButNotOuterStructure() {
            int[][] data = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.wrap(data);
            int[][] exposed = m.unsafeBackingArray();

            assertNotSame(data, exposed);
            assertSame(data[0], exposed[0]);
            exposed[1] = exposed[0];
            assertArrayEquals(new int[] { 3, 4 }, m.rowView(1));

            exposed[0][0] = 99;
            assertEquals(99, m.get(0, 0));
        }
    }

    @Nested
    class RowViewTests {

        @Test
        public void testWrapTakesStructuralSnapshotOfOuterArray() {
            int[][] backingArray = { { 1 }, { 10 } };
            IntMatrix matrix = IntMatrix.wrap(backingArray);

            backingArray[1] = backingArray[0];
            AtomicInteger calls = new AtomicInteger();
            matrix.updateAll(value -> {
                calls.incrementAndGet();
                return value + 1;
            });

            assertEquals(2, calls.get());
            assertArrayEquals(new int[] { 2 }, backingArray[0]);
            assertArrayEquals(new int[] { 2 }, backingArray[1]);
            assertArrayEquals(new int[] { 11 }, matrix.rowView(1));
        }

        @Test
        public void testOuterArrayMutationDuringTraversalDoesNotChangeStructure() {
            int[][] backingArray = { { 1 }, { 10 } };
            IntMatrix matrix = IntMatrix.wrap(backingArray);
            AtomicInteger calls = new AtomicInteger();

            matrix.updateAll(value -> {
                calls.incrementAndGet();

                if (value == 1) {
                    backingArray[1] = backingArray[0];
                }

                return value + 1;
            });

            assertEquals(2, calls.get());
            assertArrayEquals(new int[] { 2 }, backingArray[0]);
            assertArrayEquals(new int[] { 2 }, backingArray[1]);
            assertArrayEquals(new int[] { 11 }, matrix.rowView(1));
        }

        @Test
        public void testRowView() {
            // From rowView Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] row0 = matrix.rowView(0); // Returns [1, 2, 3] (direct reference)
            assertArrayEquals(new int[] { 1, 2, 3 }, row0);
            row0[0] = 99; // Also changes matrix element at (0, 0) to 99
            assertEquals(99, matrix.get(0, 0));
        }

        @Test
        public void testSetWideningSupportsCovariantRows() {
            Number[][] backing = new Number[][] { new Long[] { 1L, 2L } };
            Matrix<Number> matrix = Matrix.wrap(backing);

            matrix.set(0, 1, 2L);

            assertEquals(2L, matrix.get(0, 1));
            assertEquals(Long.class, matrix.rowView(0).getClass().getComponentType());
        }

        @Test
        public void testRowViewColumnAccessorsThrowIndexOutOfBoundsThroughBaseType() {
            List<AbstractMatrix<?, ?, ?, ?, ?>> matrices = new ArrayList<>();
            matrices.add(BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } }));
            matrices.add(ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } }));
            matrices.add(CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } }));
            matrices.add(ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } }));
            matrices.add(IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } }));
            matrices.add(LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } }));
            matrices.add(FloatMatrix.wrap(new float[][] { { 1f, 2f }, { 3f, 4f } }));
            matrices.add(DoubleMatrix.wrap(new double[][] { { 1d, 2d }, { 3d, 4d } }));
            matrices.add(Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } }));

            for (AbstractMatrix<?, ?, ?, ?, ?> m : matrices) {
                // rowView
                assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(-1));
                assertThrows(IndexOutOfBoundsException.class, () -> m.rowView(2));
                // rowCopy
                assertThrows(IndexOutOfBoundsException.class, () -> m.rowCopy(-1));
                assertThrows(IndexOutOfBoundsException.class, () -> m.rowCopy(2));
                // columnCopy
                assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(-1));
                assertThrows(IndexOutOfBoundsException.class, () -> m.columnCopy(2));
            }
        }
    }

    @Nested
    class RowCopyTests {

        @Test
        public void testRowCopy() {
            // From rowCopy Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] rowCopy = matrix.rowCopy(0); // Returns [1, 2, 3] (independent copy)
            assertArrayEquals(new int[] { 1, 2, 3 }, rowCopy);
            rowCopy[0] = 99; // Does NOT affect the original matrix
            assertEquals(1, matrix.get(0, 0));
        }
    }

    @Nested
    class ColumnCopyTests {

        @Test
        public void testColumnCopy() {
            // From columnCopy Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] colCopy = matrix.columnCopy(1); // Returns [2, 5] (independent copy)
            assertArrayEquals(new int[] { 2, 5 }, colCopy);
            colCopy[0] = 99; // Does NOT affect the original matrix
            assertEquals(2, matrix.get(0, 1));
        }

        @Test
        public void testColumnCopyAndDiagonalReadsAreTypeSafeForObjectBackedMatrices() {
            Matrix<String> repeated = Matrix.wrap(new String[][] { { "a", "a" }, { "a", "a" } });
            String[] col = repeated.columnCopy(0);
            assertEquals(String.class, col.getClass().getComponentType());
            assertEquals("a", col[0]);

            Matrix<String> diagonal = Matrix.ofMainDiagonal(new String[] { "x", "y" });
            String[] main = diagonal.mainDiagonalCopy();
            String[] anti = diagonal.antiDiagonalCopy();
            assertEquals(String.class, main.getClass().getComponentType());
            assertEquals(String.class, anti.getClass().getComponentType());
            assertEquals("x", main[0]);
            assertEquals(null, anti[0]);
        }
    }

    @Nested
    class RowCountTests {

        @Test
        public void testRowCount() {
            // From rowCount Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(2, matrix.rowCount()); // Returns 2
        }

        @Test
        public void testPackageContract_FactoryStorageOwnershipAcrossTypes() {
            final boolean[] z = new boolean[2];
            final byte[] y = new byte[2];
            final char[] ch = new char[2];
            final short[] s = new short[2];
            final int[] i = new int[2];
            final long[] l = new long[2];
            final float[] f = new float[2];
            final double[] d = new double[2];
            final String[] o = new String[2];

            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.wrap(z, z));
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.wrap(y, y));
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.wrap(ch, ch));
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.wrap(s, s));
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(i, i));
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.wrap(l, l));
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.wrap(f, f));
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.wrap(d, d));
            assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(String.class, o, o));

            assertEquals(2, BooleanMatrix.copyOf(z, z).rowCount());
            assertEquals(2, ByteMatrix.copyOf(y, y).rowCount());
            assertEquals(2, CharMatrix.copyOf(ch, ch).rowCount());
            assertEquals(2, ShortMatrix.copyOf(s, s).rowCount());
            assertEquals(2, IntMatrix.copyOf(i, i).rowCount());
            assertEquals(2, LongMatrix.copyOf(l, l).rowCount());
            assertEquals(2, FloatMatrix.copyOf(f, f).rowCount());
            assertEquals(2, DoubleMatrix.copyOf(d, d).rowCount());
            assertEquals(2, Matrix.copyOf(String.class, o, o).rowCount());

            assertSame(IntMatrix.empty(), IntMatrix.wrap(new int[0][]));
            assertSame(IntMatrix.empty(), IntMatrix.copyOf(new int[0][]));
            assertSame(CharMatrix.empty(), CharMatrix.wrap(new char[0][]));
            assertSame(BooleanMatrix.empty(), BooleanMatrix.copyOf(new boolean[0][]));

            final String[][] zeroRows = new String[0][];
            final Matrix<String> copied = Matrix.copyOf(String.class, zeroRows);
            assertNotSame(Matrix.empty(), copied);
            assertShape("Matrix.copyOf zero rows", copied, 0, 0);

            final String[][] source = { { "a" }, { "b" } };
            final Matrix<String> wrapped = Matrix.wrap(String.class, source);
            source[0][0] = "z";
            source[1] = new String[] { "q" };
            assertEquals("z", wrapped.get(0, 0)); // row storage shared
            assertEquals("b", wrapped.get(1, 0)); // outer array snapshotted

            final IntMatrix target = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            target.copyFrom(new int[][] { null, { 7, 8 } });
            assertEquals(IntMatrix.wrap(new int[][] { { 1, 2 }, { 7, 8 } }), target);
        }
    }

    @Nested
    class ColumnCountTests {

        @Test
        public void testColumnCount() {
            // From columnCount Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(3, matrix.columnCount()); // Returns 3
        }

        @Test
        public void testReshape_SameColumnCountSameMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = m.reshapeAndPadToColumnCount(3);
            assertEquals(2, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(6, reshaped.get(1, 2));
        }

        @Test
        public void testRandomPreservesZeroRowNonZeroColumnShape() {
            assertEquals(1, BooleanMatrix.random(0, 1).columnCount());
            assertEquals(1, ByteMatrix.random(0, 1).columnCount());
            assertEquals(1, CharMatrix.random(0, 1).columnCount());
            assertEquals(1, ShortMatrix.random(0, 1).columnCount());
            assertEquals(1, IntMatrix.random(0, 1).columnCount());
            assertEquals(1, LongMatrix.random(0, 1).columnCount());
            assertEquals(1, FloatMatrix.random(0, 1).columnCount());
            assertEquals(1, DoubleMatrix.random(0, 1).columnCount());
        }
    }

    @Nested
    class ElementCountTests {

        @Test
        public void testLargeMatrix() {
            int size = 100;
            int[][] data = new int[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    data[i][j] = i * size + j;
                }
            }

            IntMatrix matrix = IntMatrix.wrap(data);
            Assertions.assertEquals(size, matrix.rowCount());
            Assertions.assertEquals(size, matrix.columnCount());
            Assertions.assertEquals(size * size, matrix.elementCount());
            Assertions.assertEquals(5050, matrix.get(50, 50));

            // Test operations on large matrix
            IntMatrix transposed = matrix.transpose();
            Assertions.assertEquals(matrix.get(10, 20), transposed.get(20, 10));

            IntMatrix copy = matrix.copyRegion(0, 10, 0, 10);
            Assertions.assertEquals(10, copy.rowCount());
            Assertions.assertEquals(10, copy.columnCount());
        }

        @Test
        public void testRowsColsCount_SquareMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertEquals(2, m.rowCount());
            assertEquals(2, m.columnCount());
            assertEquals(4, m.elementCount());
        }

        @Test
        public void testLargeMatrix_Count() {
            int[][] large = new int[1000][1000];
            IntMatrix m = IntMatrix.wrap(large);

            assertEquals(1000, m.rowCount());
            assertEquals(1000, m.columnCount());
            assertEquals(1000000L, m.elementCount());
        }

        @Test
        public void testRowsColsCount_Rectangular() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(2, m.rowCount());
            assertEquals(3, m.columnCount());
            assertEquals(6, m.elementCount());
        }

        @Test
        public void testRowsColsCount_LargeMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[100][50]);
            assertEquals(100, m.rowCount());
            assertEquals(50, m.columnCount());
            assertEquals(5000, m.elementCount());
        }

        // From rotate90 Javadoc
        // Original:    Rotated 90 degrees clockwise:
        // 1 2 3        7 4 1
        // 4 5 6   =>   8 5 2
        // 7 8 9        9 6 3
        @Test
        public void testElementCount() {
            // From elementCount Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertEquals(6, matrix.elementCount()); // Returns 6
        }

        @Test
        public void testEmptyMatrix() {
            IntMatrix empty = IntMatrix.wrap(new int[0][0]);

            Assertions.assertEquals(0, empty.rowCount());
            Assertions.assertEquals(0, empty.columnCount());
            Assertions.assertEquals(0, empty.elementCount());
            Assertions.assertTrue(empty.isEmpty());

            IntList flatList = empty.flatten();
            Assertions.assertTrue(flatList.isEmpty());

            List<Integer> streamList = empty.rowMajorStream().boxed().collect(Collectors.toList());
            Assertions.assertTrue(streamList.isEmpty());
        }

        @Test
        public void testSingleElementMatrix() {
            IntMatrix single = IntMatrix.wrap(new int[][] { { 42 } });

            Assertions.assertEquals(1, single.rowCount());
            Assertions.assertEquals(1, single.columnCount());
            Assertions.assertEquals(1, single.elementCount());
            Assertions.assertFalse(single.isEmpty());
            Assertions.assertEquals(42, single.get(0, 0));

            List<Integer> diagonal = single.mainDiagonalStream().boxed().collect(Collectors.toList());
            Assertions.assertEquals(1, diagonal.size());
            Assertions.assertEquals(42, diagonal.get(0));
        }

        @Test
        public void testRowsColsCount_SingleElement() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 42 } });
            assertEquals(1, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(1, m.elementCount());
        }

        @Test
        public void testRowsColsCount_EmptyMatrix() {
            IntMatrix m = IntMatrix.empty();
            assertEquals(0, m.rowCount());
            assertEquals(0, m.columnCount());
            assertEquals(0, m.elementCount());
        }

        @Test
        public void testRowsColsCount_SingleRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3, 4, 5 } });
            assertEquals(1, m.rowCount());
            assertEquals(5, m.columnCount());
            assertEquals(5, m.elementCount());
        }

        @Test
        public void testRowsColsCount_SingleColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 }, { 2 }, { 3 }, { 4 } });
            assertEquals(4, m.rowCount());
            assertEquals(1, m.columnCount());
            assertEquals(4, m.elementCount());
        }

        @Test
        public void testElementCount_Empty() {
            IntMatrix m = IntMatrix.wrap(new int[0][0]);
            assertEquals(0, m.elementCount());
        }

        @Test
        public void testElementCount_SingleElement() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 42 } });
            assertEquals(1, m.elementCount());
        }

        @Test
        public void testElementCount_SingleRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3, 4, 5 } });
            assertEquals(5, m.elementCount());
        }

        @Test
        public void testElementCount_SingleColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 }, { 2 }, { 3 } });
            assertEquals(3, m.elementCount());
        }
    }

    @Nested
    class IsEmptyTests {

        @Test
        public void testIsEmpty() {
            IntMatrix matrix = createTestMatrix();
            Assertions.assertFalse(matrix.isEmpty());

            IntMatrix emptyMatrix = IntMatrix.wrap(new int[0][0]);
            Assertions.assertTrue(emptyMatrix.isEmpty());
        }

        @Test
        public void testIsEmpty_NonEmptyMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 } });
            assertFalse(m.isEmpty());
        }

        @Test
        public void testIsEmpty_ZeroRows() {
            IntMatrix m = IntMatrix.wrap(new int[0][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testIsEmpty_ZeroColumns() {
            IntMatrix m = IntMatrix.wrap(new int[5][0]);
            assertTrue(m.isEmpty());
        }

        @Test
        public void testIsEmpty_LargeMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[100][100]);
            assertFalse(m.isEmpty());
        }

        @Test
        public void testIsEmpty_EmptyMatrix() {
            IntMatrix matrix = IntMatrix.wrap(new int[0][0]);
            assertTrue(matrix.isEmpty());
        }

        @Test
        public void testIsEmpty_NonEmptyMatrix_Shape2By2() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertFalse(matrix.isEmpty());
        }

        @Test
        public void testIsEmpty_ZeroRows_Shape0By5() {
            IntMatrix matrix = IntMatrix.wrap(new int[0][5]);
            assertTrue(matrix.isEmpty());
        }

        @Test
        public void testIsEmpty_ZeroCols() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] {});
            assertTrue(matrix.isEmpty());
        }

        @Test
        public void testisEmpty() {
            // From isEmpty Javadoc
            IntMatrix empty = IntMatrix.wrap(new int[0][0]);
            assertTrue(empty.isEmpty()); // Returns true (0x0)

            IntMatrix notEmpty = IntMatrix.wrap(new int[][] { { 1 } });
            assertFalse(notEmpty.isEmpty()); // Returns false (1x1)
        }
    }

    @Nested
    class CopyTests {

        @Test
        public void testCopy() {
            IntMatrix matrix = createTestMatrix();
            IntMatrix copy = matrix.copy();

            Assertions.assertEquals(matrix.rowCount(), copy.rowCount());
            Assertions.assertEquals(matrix.columnCount(), copy.columnCount());
            Assertions.assertEquals(matrix.get(0, 0), copy.get(0, 0));
            Assertions.assertEquals(matrix.get(2, 2), copy.get(2, 2));

            // Verify they are independent
            copy.set(0, 0, 100);
            Assertions.assertNotEquals(matrix.get(0, 0), copy.get(0, 0));
        }

        @Test
        public void testCopy_IntMatrix() {
            IntMatrix original = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix copy = original.copy();

            assertEquals(original.rowCount(), copy.rowCount());
            assertEquals(original.columnCount(), copy.columnCount());
            assertEquals(1, copy.get(0, 0));
            assertEquals(4, copy.get(1, 1));

            // Modify copy should not affect original
            copy.set(0, 0, 99);
            assertEquals(1, original.get(0, 0));
            assertEquals(99, copy.get(0, 0));
        }

        @Test
        public void testCopy_DoubleMatrix() {
            DoubleMatrix original = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            DoubleMatrix copy = original.copy();

            assertEquals(original.rowCount(), copy.rowCount());
            assertEquals(original.columnCount(), copy.columnCount());
            assertEquals(1.5, copy.get(0, 0), 0.0001);
            assertEquals(4.5, copy.get(1, 1), 0.0001);
        }

        @Test
        public void testCopy_ObjectMatrix() {
            Matrix<String> original = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            Matrix<String> copy = original.copy();

            assertEquals(original.rowCount(), copy.rowCount());
            assertEquals(original.columnCount(), copy.columnCount());
            assertEquals("a", copy.get(0, 0));
            assertEquals("d", copy.get(1, 1));
        }

        @Test
        public void testCopy_DoubleMatrix_Shape2By2() {
            DoubleMatrix original = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            DoubleMatrix copy = original.copy();

            assertEquals(original.rowCount(), copy.rowCount());
            assertEquals(original.columnCount(), copy.columnCount());
            assertEquals(1.5, copy.get(0, 0), 0.0001);
            assertEquals(4.5, copy.get(1, 1), 0.0001);

            // Verify independence
            copy.set(0, 0, 99.5);
            assertEquals(1.5, original.get(0, 0), 0.0001);
            assertEquals(99.5, copy.get(0, 0), 0.0001);
        }

        @Test
        public void testCopy_ObjectMatrix_Shape2By2() {
            Matrix<String> original = Matrix.wrap(new String[][] { { "a", "b" }, { "c", "d" } });
            Matrix<String> copy = original.copy();

            assertEquals(original.rowCount(), copy.rowCount());
            assertEquals(original.columnCount(), copy.columnCount());
            assertEquals("a", copy.get(0, 0));
            assertEquals("d", copy.get(1, 1));

            // Verify independence
            copy.set(0, 0, "X");
            assertEquals("a", original.get(0, 0));
            assertEquals("X", copy.get(0, 0));
        }

        @Test
        public void testcopy() {
            // From copy Javadoc
            IntMatrix original = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix copy = original.copy();
            copy.set(0, 0, 10); // Original matrix remains unchanged
            assertEquals(1, original.get(0, 0));
            assertEquals(10, copy.get(0, 0));
        }

        @Test
        public void testCopy_EmptyMatrix() {
            IntMatrix empty = IntMatrix.empty();
            IntMatrix copy = empty.copy();
            assertTrue(copy.isEmpty());
        }

        @Test
        public void testCopy_SingleElement() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 42 } });
            IntMatrix copy = m.copy();
            assertEquals(1, copy.rowCount());
            assertEquals(1, copy.columnCount());
            assertEquals(42, copy.get(0, 0));
        }
    }

    @Nested
    class CopyRowsTests {

        @Test
        public void testCopyRows() {
            IntMatrix matrix = createTestMatrix();
            IntMatrix copy = matrix.copyRows(0, 2);

            Assertions.assertEquals(2, copy.rowCount());
            Assertions.assertEquals(3, copy.columnCount());
            Assertions.assertEquals(1, copy.get(0, 0));
            Assertions.assertEquals(6, copy.get(1, 2));
        }

        @Test
        public void testCopyRows_AllRows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix copy = m.copyRows(0, 2);

            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(1, copy.get(0, 0));
            assertEquals(4, copy.get(1, 1));
        }

        @Test
        public void testcopyRows() {
            // From copyRows(fromRowIndex, toRowIndex) Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            IntMatrix subMatrix = matrix.copyRows(0, 2); // Contains rows 0 and 1
            assertEquals(2, subMatrix.rowCount());
            assertEquals(2, subMatrix.columnCount());
            assertEquals(1, subMatrix.get(0, 0));
            assertEquals(4, subMatrix.get(1, 1));

            IntMatrix lastRow = matrix.copyRows(2, 3); // Contains only row 2
            assertEquals(1, lastRow.rowCount());
            assertEquals(5, lastRow.get(0, 0));
            assertEquals(6, lastRow.get(0, 1));
        }

        @Test
        public void testCopyRangesEmptyReturnsEmptyMatrix() {
            // copy with an empty row range is a valid empty slice across every matrix type.
            // The slice retains the selected column dimension even though it has no rows.
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 } });
            BooleanMatrix booleanMatrix = BooleanMatrix.wrap(new boolean[][] { { true, false } });
            ByteMatrix byteMatrix = ByteMatrix.wrap(new byte[][] { { 1, 2 } });
            CharMatrix charMatrix = CharMatrix.wrap(new char[][] { { 'a', 'b' } });
            ShortMatrix shortMatrix = ShortMatrix.wrap(new short[][] { { 1, 2 } });
            IntMatrix intMatrix = IntMatrix.wrap(new int[][] { { 1, 2 } });
            LongMatrix longMatrix = LongMatrix.wrap(new long[][] { { 1L, 2L } });
            FloatMatrix floatMatrix = FloatMatrix.wrap(new float[][] { { 1F, 2F } });
            DoubleMatrix doubleMatrix = DoubleMatrix.wrap(new double[][] { { 1D, 2D } });

            assertEquals(0, matrix.copyRows(0, 0).rowCount());
            assertEquals(2, matrix.copyRows(0, 0).columnCount());
            assertEquals(0, booleanMatrix.copyRows(0, 0).rowCount());
            assertEquals(2, booleanMatrix.copyRows(0, 0).columnCount());
            assertEquals(0, byteMatrix.copyRows(0, 0).rowCount());
            assertEquals(2, byteMatrix.copyRows(0, 0).columnCount());
            assertEquals(0, charMatrix.copyRows(0, 0).rowCount());
            assertEquals(2, charMatrix.copyRows(0, 0).columnCount());
            assertEquals(0, shortMatrix.copyRows(0, 0).rowCount());
            assertEquals(2, shortMatrix.copyRows(0, 0).columnCount());
            assertEquals(0, intMatrix.copyRows(0, 0).rowCount());
            assertEquals(2, intMatrix.copyRows(0, 0).columnCount());
            assertEquals(0, longMatrix.copyRows(0, 0).rowCount());
            assertEquals(2, longMatrix.copyRows(0, 0).columnCount());
            assertEquals(0, floatMatrix.copyRows(0, 0).rowCount());
            assertEquals(2, floatMatrix.copyRows(0, 0).columnCount());
            assertEquals(0, doubleMatrix.copyRows(0, 0).rowCount());
            assertEquals(2, doubleMatrix.copyRows(0, 0).columnCount());

            assertEquals(0, matrix.copyRegion(0, 0, 0, 1).rowCount());
            assertEquals(1, matrix.copyRegion(0, 0, 0, 1).columnCount());
            assertEquals(0, booleanMatrix.copyRegion(0, 0, 0, 1).rowCount());
            assertEquals(1, booleanMatrix.copyRegion(0, 0, 0, 1).columnCount());
            assertEquals(0, byteMatrix.copyRegion(0, 0, 0, 1).rowCount());
            assertEquals(1, byteMatrix.copyRegion(0, 0, 0, 1).columnCount());
            assertEquals(0, charMatrix.copyRegion(0, 0, 0, 1).rowCount());
            assertEquals(1, charMatrix.copyRegion(0, 0, 0, 1).columnCount());
            assertEquals(0, shortMatrix.copyRegion(0, 0, 0, 1).rowCount());
            assertEquals(1, shortMatrix.copyRegion(0, 0, 0, 1).columnCount());
            assertEquals(0, intMatrix.copyRegion(0, 0, 0, 1).rowCount());
            assertEquals(1, intMatrix.copyRegion(0, 0, 0, 1).columnCount());
            assertEquals(0, longMatrix.copyRegion(0, 0, 0, 1).rowCount());
            assertEquals(1, longMatrix.copyRegion(0, 0, 0, 1).columnCount());
            assertEquals(0, floatMatrix.copyRegion(0, 0, 0, 1).rowCount());
            assertEquals(1, floatMatrix.copyRegion(0, 0, 0, 1).columnCount());
            assertEquals(0, doubleMatrix.copyRegion(0, 0, 0, 1).rowCount());
            assertEquals(1, doubleMatrix.copyRegion(0, 0, 0, 1).columnCount());

            assertEquals(1, matrix.copyColumns(1, 1).rowCount());
            assertEquals(0, matrix.copyColumns(1, 1).columnCount());
            assertEquals(1, booleanMatrix.copyColumns(1, 1).rowCount());
            assertEquals(0, booleanMatrix.copyColumns(1, 1).columnCount());
            assertEquals(1, byteMatrix.copyColumns(1, 1).rowCount());
            assertEquals(0, byteMatrix.copyColumns(1, 1).columnCount());
            assertEquals(1, charMatrix.copyColumns(1, 1).rowCount());
            assertEquals(0, charMatrix.copyColumns(1, 1).columnCount());
            assertEquals(1, shortMatrix.copyColumns(1, 1).rowCount());
            assertEquals(0, shortMatrix.copyColumns(1, 1).columnCount());
            assertEquals(1, intMatrix.copyColumns(1, 1).rowCount());
            assertEquals(0, intMatrix.copyColumns(1, 1).columnCount());
            assertEquals(1, longMatrix.copyColumns(1, 1).rowCount());
            assertEquals(0, longMatrix.copyColumns(1, 1).columnCount());
            assertEquals(1, floatMatrix.copyColumns(1, 1).rowCount());
            assertEquals(0, floatMatrix.copyColumns(1, 1).columnCount());
            assertEquals(1, doubleMatrix.copyColumns(1, 1).rowCount());
            assertEquals(0, doubleMatrix.copyColumns(1, 1).columnCount());
        }

        @Test
        public void testCopyRows_SingleRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix subset = m.copyRows(1, 2);

            assertEquals(1, subset.rowCount());
            assertEquals(3, subset.columnCount());
            assertArrayEquals(new int[] { 4, 5, 6 }, subset.rowView(0));
        }

        @Test
        public void testCopyRows_EmptyRange() {
            // copy with from == to is a valid empty row slice; the column count is preserved.
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            IntMatrix empty = m.copyRows(1, 1);
            assertEquals(0, empty.rowCount());
            assertEquals(2, empty.columnCount());
        }

        @Test
        public void testCopyRowsInvalidIndices() {
            IntMatrix matrix = createTestMatrix();

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
                matrix.copyRows(-1, 2);
            });

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
                matrix.copyRows(0, 4);
            });

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
                matrix.copyRows(2, 1);
            });
        }
    }

    @Nested
    class CopyColumnsTests {

        @Test
        public void testCopyColumns() {
            IntMatrix matrix = createTestMatrix();
            IntMatrix copy = matrix.copyColumns(1, 3);

            Assertions.assertEquals(3, copy.rowCount());
            Assertions.assertEquals(2, copy.columnCount());
            Assertions.assertArrayEquals(new int[] { 2, 3 }, copy.rowView(0));
            Assertions.assertArrayEquals(new int[] { 5, 6 }, copy.rowView(1));
            Assertions.assertArrayEquals(new int[] { 8, 9 }, copy.rowView(2));

            // A range copy owns its storage independently of the source matrix.
            copy.set(0, 0, 100);
            Assertions.assertEquals(2, matrix.get(0, 1));
        }

        @Test
        public void testCopyColumnsEmptyRange() {
            IntMatrix matrix = createTestMatrix();
            IntMatrix copy = matrix.copyColumns(1, 1);

            Assertions.assertEquals(3, copy.rowCount());
            Assertions.assertEquals(0, copy.columnCount());
        }

        @Test
        public void testCopyColumnsInvalidIndices() {
            IntMatrix matrix = createTestMatrix();

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyColumns(-1, 2));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyColumns(0, 4));
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> matrix.copyColumns(2, 1));
        }
    }

    @Nested
    class CopyRegionTests {

        @Test
        public void testCopyRegion() {
            IntMatrix matrix = createTestMatrix();
            IntMatrix copy = matrix.copyRegion(1, 3, 1, 3);

            Assertions.assertEquals(2, copy.rowCount());
            Assertions.assertEquals(2, copy.columnCount());
            Assertions.assertEquals(5, copy.get(0, 0));
            Assertions.assertEquals(6, copy.get(0, 1));
            Assertions.assertEquals(8, copy.get(1, 0));
            Assertions.assertEquals(9, copy.get(1, 1));
        }

        @Test
        public void testCopyRegion_EntireMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix copy = m.copyRegion(0, 2, 0, 2);

            assertEquals(m, copy);
        }

        @Test
        public void testCopyRegion_EntireMatrix_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix copy = m.copyRegion(0, 2, 0, 2);

            assertEquals(2, copy.rowCount());
            assertEquals(2, copy.columnCount());
            assertEquals(1, copy.get(0, 0));
            assertEquals(4, copy.get(1, 1));
        }

        @Test
        public void testcopyRegion() {
            // From copyRegion(fromRowIndex, toRowIndex, fromColumnIndex, toColumnIndex) Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix subMatrix = matrix.copyRegion(0, 2, 1, 3);
            // subMatrix: {{2, 3}, {5, 6}} (rows 0-1, columns 1-2)
            assertEquals(2, subMatrix.rowCount());
            assertEquals(2, subMatrix.columnCount());
            assertEquals(2, subMatrix.get(0, 0));
            assertEquals(3, subMatrix.get(0, 1));
            assertEquals(5, subMatrix.get(1, 0));
            assertEquals(6, subMatrix.get(1, 1));

            IntMatrix centerElement = matrix.copyRegion(1, 2, 1, 2);
            // centerElement: {{5}} (just the center element)
            assertEquals(1, centerElement.rowCount());
            assertEquals(1, centerElement.columnCount());
            assertEquals(5, centerElement.get(0, 0));
        }

        @Test
        public void testCopyRegion_EmptyRange() {
            // copy with an empty row sub-range is a valid empty slice; the column sub-range width survives.
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix empty = m.copyRegion(1, 1, 0, 2);
            assertEquals(0, empty.rowCount());
            assertEquals(2, empty.columnCount());
        }

        @Test
        public void testCopyRegion_SingleElement() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix single = m.copyRegion(1, 2, 1, 2);

            assertEquals(1, single.rowCount());
            assertEquals(1, single.columnCount());
            assertEquals(5, single.get(0, 0));
        }

        @Test
        public void testCopyRegionInvalidIndices() {
            IntMatrix matrix = createTestMatrix();

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
                matrix.copyRegion(0, 2, -1, 2);
            });

            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
                matrix.copyRegion(0, 2, 0, 4);
            });
        }

        @Test
        public void testCopyRegion_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, 0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(-1, 2, 0, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 3, 0, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(2, 1, 0, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.copyRegion(0, 2, 2, 1));
        }
    }

    @Nested
    class Rotate90Tests {

        @Test
        public void testRotate90() {
            IntMatrix matrix = createTestMatrix2x3();
            IntMatrix rotated = matrix.rotate90();

            Assertions.assertEquals(3, rotated.rowCount());
            Assertions.assertEquals(2, rotated.columnCount());
            Assertions.assertEquals(4, rotated.get(0, 0));
            Assertions.assertEquals(1, rotated.get(0, 1));
            Assertions.assertEquals(5, rotated.get(1, 0));
            Assertions.assertEquals(2, rotated.get(1, 1));
        }

        @Test
        public void testRotate90_Rectangular() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix rotated = m.rotate90();

            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(4, rotated.get(0, 0));
            assertEquals(1, rotated.get(0, 1));
            assertEquals(6, rotated.get(2, 0));
            assertEquals(3, rotated.get(2, 1));
        }

        // From reshape(int) Javadoc
        @Test
        public void testrotate90() {
            // From rotate90 Javadoc
            // Original:    Rotated 90 degrees clockwise:
            // 1 2 3        7 4 1
            // 4 5 6   =>   8 5 2
            // 7 8 9        9 6 3
            IntMatrix original = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix rotated = original.rotate90();
            assertEquals(7, rotated.get(0, 0));
            assertEquals(4, rotated.get(0, 1));
            assertEquals(1, rotated.get(0, 2));
            assertEquals(8, rotated.get(1, 0));
            assertEquals(5, rotated.get(1, 1));
            assertEquals(2, rotated.get(1, 2));
            assertEquals(9, rotated.get(2, 0));
            assertEquals(6, rotated.get(2, 1));
            assertEquals(3, rotated.get(2, 2));
        }

        @Test
        public void testRotate90_SingleElement() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 42 } });
            IntMatrix rotated = m.rotate90();

            assertEquals(1, rotated.rowCount());
            assertEquals(1, rotated.columnCount());
            assertEquals(42, rotated.get(0, 0));
        }

        @Test
        public void testRotate90_SingleRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            IntMatrix rotated = m.rotate90();

            assertEquals(3, rotated.rowCount());
            assertEquals(1, rotated.columnCount());
            assertEquals(1, rotated.get(0, 0));
            assertEquals(2, rotated.get(1, 0));
            assertEquals(3, rotated.get(2, 0));
        }
    }

    @Nested
    class Rotate180Tests {

        @Test
        public void testRotate180() {
            IntMatrix matrix = createTestMatrix2x3();
            IntMatrix rotated = matrix.rotate180();

            Assertions.assertEquals(2, rotated.rowCount());
            Assertions.assertEquals(3, rotated.columnCount());
            Assertions.assertEquals(6, rotated.get(0, 0));
            Assertions.assertEquals(5, rotated.get(0, 1));
            Assertions.assertEquals(4, rotated.get(0, 2));
            Assertions.assertEquals(3, rotated.get(1, 0));
        }

        @Test
        public void testRotate180_SquareMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix rotated = m.rotate180();

            assertEquals(4, rotated.get(0, 0));
            assertEquals(3, rotated.get(0, 1));
            assertEquals(2, rotated.get(1, 0));
            assertEquals(1, rotated.get(1, 1));
        }

        @Test
        public void testRotate180_Rectangular() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix rotated = m.rotate180();

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
        public void testrotate180() {
            // From rotate180 Javadoc
            // Original:    Rotated 180 degrees:
            // 1 2 3        9 8 7
            // 4 5 6   =>   6 5 4
            // 7 8 9        3 2 1
            // Note: the Javadoc uses a 2x3 matrix for the code but shows 3x3 in diagram.
            // Let's test the 3x3 diagram.
            IntMatrix original = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix rotated = original.rotate180();
            assertEquals(9, rotated.get(0, 0));
            assertEquals(8, rotated.get(0, 1));
            assertEquals(7, rotated.get(0, 2));
            assertEquals(6, rotated.get(1, 0));
            assertEquals(5, rotated.get(1, 1));
            assertEquals(4, rotated.get(1, 2));
            assertEquals(3, rotated.get(2, 0));
            assertEquals(2, rotated.get(2, 1));
            assertEquals(1, rotated.get(2, 2));
        }

        @Test
        public void testRotate180_SingleElement() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 42 } });
            IntMatrix rotated = m.rotate180();

            assertEquals(1, rotated.rowCount());
            assertEquals(1, rotated.columnCount());
            assertEquals(42, rotated.get(0, 0));
        }
    }

    @Nested
    class Rotate270Tests {

        @Test
        public void testRotate270() {
            IntMatrix matrix = createTestMatrix2x3();
            IntMatrix rotated = matrix.rotate270();

            Assertions.assertEquals(3, rotated.rowCount());
            Assertions.assertEquals(2, rotated.columnCount());
            Assertions.assertEquals(3, rotated.get(0, 0));
            Assertions.assertEquals(6, rotated.get(0, 1));
            Assertions.assertEquals(2, rotated.get(1, 0));
            Assertions.assertEquals(5, rotated.get(1, 1));
        }

        @Test
        public void testRotate270_SquareMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix rotated = m.rotate270();

            assertEquals(2, rotated.get(0, 0));
            assertEquals(4, rotated.get(0, 1));
            assertEquals(1, rotated.get(1, 0));
            assertEquals(3, rotated.get(1, 1));
        }

        @Test
        public void testRotate270_Rectangular() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix rotated = m.rotate270();

            assertEquals(3, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(3, rotated.get(0, 0));
            assertEquals(6, rotated.get(0, 1));
            assertEquals(1, rotated.get(2, 0));
            assertEquals(4, rotated.get(2, 1));
        }

        @Test
        public void testRotate270_Square() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix rotated = m.rotate270();

            assertEquals(2, rotated.rowCount());
            assertEquals(2, rotated.columnCount());
            assertEquals(2, rotated.get(0, 0));
            assertEquals(4, rotated.get(0, 1));
            assertEquals(1, rotated.get(1, 0));
            assertEquals(3, rotated.get(1, 1));
        }

        @Test
        public void testrotate270() {
            // From rotate270 Javadoc
            // Original:    Rotated 270 degrees clockwise:
            // 1 2 3        3 6 9
            // 4 5 6   =>   2 5 8
            // 7 8 9        1 4 7
            IntMatrix original = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            IntMatrix rotated = original.rotate270();
            assertEquals(3, rotated.get(0, 0));
            assertEquals(6, rotated.get(0, 1));
            assertEquals(9, rotated.get(0, 2));
            assertEquals(2, rotated.get(1, 0));
            assertEquals(5, rotated.get(1, 1));
            assertEquals(8, rotated.get(1, 2));
            assertEquals(1, rotated.get(2, 0));
            assertEquals(4, rotated.get(2, 1));
            assertEquals(7, rotated.get(2, 2));
        }

        @Test
        public void testRotate270_SingleElement() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 42 } });
            IntMatrix rotated = m.rotate270();

            assertEquals(1, rotated.rowCount());
            assertEquals(1, rotated.columnCount());
            assertEquals(42, rotated.get(0, 0));
        }
    }

    @Nested
    class TransposeTests {

        @Test
        public void testTranspose() {
            IntMatrix matrix = createTestMatrix2x3();
            IntMatrix transposed = matrix.transpose();

            Assertions.assertEquals(3, transposed.rowCount());
            Assertions.assertEquals(2, transposed.columnCount());
            Assertions.assertEquals(1, transposed.get(0, 0));
            Assertions.assertEquals(4, transposed.get(0, 1));
            Assertions.assertEquals(2, transposed.get(1, 0));
            Assertions.assertEquals(5, transposed.get(1, 1));
        }

        @Test
        public void testTranspose_DoubleTranspose() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix transposed = m.transpose().transpose();

            assertEquals(m, transposed);
        }

        @Test
        public void testtranspose() {
            // From transpose Javadoc
            // 2x3 becomes 3x2
            IntMatrix original = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix transposed = original.transpose();
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
        public void testFailedSetDoesNotCorruptElementTypeForLaterAllocations() {
            Serializable[][] backing = new String[][] { { "a" } };
            Matrix<Serializable> matrix = new Matrix<>(backing);

            assertThrows(ArrayStoreException.class, () -> matrix.set(0, 0, 1));

            Matrix<Serializable> transposed = matrix.transpose();
            assertEquals("a", transposed.get(0, 0));
        }

        @Test
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void testPackageContract_DegenerateShapesPreservedAcrossAllNineTypes() throws Exception {
            for (final int[] shape : new int[][] { { 0, 3 }, { 3, 0 } }) {
                final int r = shape[0];
                final int c = shape[1];

                for (final AbstractMatrix m : allNineTypes(r, c)) {
                    final String ctx = m.getClass().getSimpleName() + " " + r + "x" + c;
                    assertShape(ctx, m, r, c);

                    final List<AbstractMatrix> results = new ArrayList<>();
                    results.add(m.transpose());
                    results.add(m.rotate90());
                    results.add(m.rotate270());
                    assertShape(ctx + " transpose", results.get(0), c, r);
                    assertShape(ctx + " rotate90", results.get(1), c, r);
                    assertShape(ctx + " rotate270", results.get(2), c, r);

                    final AbstractMatrix[] sameShape = { m.rotate180(), m.flipHorizontally(), m.flipVertically(), m.copy(), m.copyRows(0, r),
                            m.copyColumns(0, c), m.copyRegion(0, r, 0, c), m.resize(r, c), m.pad(0, 0, 0, 0), m.repeatElements(1, 1), m.repeatMatrix(1, 1),
                            m.reshape(r, c) };

                    for (final AbstractMatrix x : sameShape) {
                        assertShape(ctx, x, r, c);
                        results.add(x);
                    }

                    for (final AbstractMatrix x : results) {
                        assertNotSame(m, x, ctx + ": a non-0x0 result must be a separate matrix");
                    }

                    assertShape(ctx + " copyRows(0, 0)", m.copyRows(0, 0), 0, c);
                    assertShape(ctx + " copyColumns(0, 0)", m.copyColumns(0, 0), r, 0);
                    assertShape(ctx + " resize(2, 4)", m.resize(2, 4), 2, 4);
                    assertShape(ctx + " pad", m.pad(1, 2, 3, 4), r + 3, c + 7);
                    assertShape(ctx + " repeatElements", m.repeatElements(2, 3), 2 * r, 3 * c);
                    assertShape(ctx + " repeatMatrix", m.repeatMatrix(2, 3), 2 * r, 3 * c);
                    assertShape(ctx + " stackVertically", m.stackVertically(m), 2 * r, c);
                    assertShape(ctx + " stackHorizontally", m.stackHorizontally(m), r, 2 * c);
                    assertShape(ctx + " reshapeAndPad", m.reshapeAndPad(0, 2), 0, 2);
                    assertShape(ctx + " reshapeAndPadToColumnCount", m.reshapeAndPadToColumnCount(2), 0, 2);

                    assertEquals(r, ((Stream) m.rowStreams()).count(), ctx + " rowStreams");
                    assertEquals(c, ((Stream) m.columnStreams()).count(), ctx + " columnStreams");

                    // A zero-row matrix does not invoke the action; N x 0 invokes it once with an empty array.
                    final AtomicInteger calls = new AtomicInteger();
                    final int[] lastLength = { -1 };
                    m.mutateViaFlatArray(flat -> {
                        calls.incrementAndGet();
                        lastLength[0] = java.lang.reflect.Array.getLength(flat);
                    });
                    assertEquals(r == 0 ? 0 : 1, calls.get(), ctx + " mutateViaFlatArray calls");
                    if (r > 0) {
                        assertEquals(0, lastLength[0], ctx + " mutateViaFlatArray length");
                    }

                    // appendTo and println share one rendering: "[]" for zero rows, one "[]" per row for N x 0.
                    final StringBuilder sb = new StringBuilder();
                    m.appendTo(sb);
                    assertEquals(r == 0 ? "[]" : "[]\n[]\n[]", sb.toString(), ctx + " appendTo");
                    assertEquals(m.toMultilineString(), sb.toString(), ctx + " appendTo vs toMultilineString");
                }
            }
        }

        @Test
        public void testTranspose_SingleElement() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 42 } });
            IntMatrix transposed = m.transpose();

            assertEquals(1, transposed.rowCount());
            assertEquals(1, transposed.columnCount());
            assertEquals(42, transposed.get(0, 0));
        }

        @Test
        public void testTranspose_SingleRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            IntMatrix transposed = m.transpose();

            assertEquals(3, transposed.rowCount());
            assertEquals(1, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(2, transposed.get(1, 0));
            assertEquals(3, transposed.get(2, 0));
        }

        @Test
        public void testTranspose_SingleColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 }, { 2 }, { 3 } });
            IntMatrix transposed = m.transpose();

            assertEquals(1, transposed.rowCount());
            assertEquals(3, transposed.columnCount());
            assertEquals(1, transposed.get(0, 0));
            assertEquals(2, transposed.get(0, 1));
            assertEquals(3, transposed.get(0, 2));
        }

        @Test
        public void testRotateAndTransposeOnNxZeroMatricesPreserveTransformedShape() {
            Matrix<String> objectMatrix = Matrix.wrap(new String[][] { {}, {} });
            assertEquals(0, objectMatrix.rotate90().rowCount());
            assertEquals(2, objectMatrix.rotate90().columnCount());
            assertEquals(0, objectMatrix.rotate270().rowCount());
            assertEquals(2, objectMatrix.rotate270().columnCount());
            assertEquals(0, objectMatrix.transpose().rowCount());
            assertEquals(2, objectMatrix.transpose().columnCount());

            BooleanMatrix booleanMatrix = BooleanMatrix.wrap(new boolean[][] { {}, {} });
            ByteMatrix byteMatrix = ByteMatrix.wrap(new byte[][] { {}, {} });
            CharMatrix charMatrix = CharMatrix.wrap(new char[][] { {}, {} });
            ShortMatrix shortMatrix = ShortMatrix.wrap(new short[][] { {}, {} });
            IntMatrix intMatrix = IntMatrix.wrap(new int[][] { {}, {} });
            LongMatrix longMatrix = LongMatrix.wrap(new long[][] { {}, {} });
            FloatMatrix floatMatrix = FloatMatrix.wrap(new float[][] { {}, {} });
            DoubleMatrix doubleMatrix = DoubleMatrix.wrap(new double[][] { {}, {} });

            assertEquals(0, booleanMatrix.transpose().rowCount());
            assertEquals(2, booleanMatrix.transpose().columnCount());
            assertEquals(0, booleanMatrix.rotate90().rowCount());
            assertEquals(2, booleanMatrix.rotate90().columnCount());
            assertEquals(0, booleanMatrix.rotate270().rowCount());
            assertEquals(2, booleanMatrix.rotate270().columnCount());
            assertEquals(0, byteMatrix.transpose().rowCount());
            assertEquals(2, byteMatrix.transpose().columnCount());
            assertEquals(0, byteMatrix.rotate90().rowCount());
            assertEquals(2, byteMatrix.rotate90().columnCount());
            assertEquals(0, byteMatrix.rotate270().rowCount());
            assertEquals(2, byteMatrix.rotate270().columnCount());
            assertEquals(0, charMatrix.transpose().rowCount());
            assertEquals(2, charMatrix.transpose().columnCount());
            assertEquals(0, charMatrix.rotate90().rowCount());
            assertEquals(2, charMatrix.rotate90().columnCount());
            assertEquals(0, charMatrix.rotate270().rowCount());
            assertEquals(2, charMatrix.rotate270().columnCount());
            assertEquals(0, shortMatrix.transpose().rowCount());
            assertEquals(2, shortMatrix.transpose().columnCount());
            assertEquals(0, shortMatrix.rotate90().rowCount());
            assertEquals(2, shortMatrix.rotate90().columnCount());
            assertEquals(0, shortMatrix.rotate270().rowCount());
            assertEquals(2, shortMatrix.rotate270().columnCount());
            assertEquals(0, intMatrix.transpose().rowCount());
            assertEquals(2, intMatrix.transpose().columnCount());
            assertEquals(0, intMatrix.rotate90().rowCount());
            assertEquals(2, intMatrix.rotate90().columnCount());
            assertEquals(0, intMatrix.rotate270().rowCount());
            assertEquals(2, intMatrix.rotate270().columnCount());
            assertEquals(0, longMatrix.transpose().rowCount());
            assertEquals(2, longMatrix.transpose().columnCount());
            assertEquals(0, longMatrix.rotate90().rowCount());
            assertEquals(2, longMatrix.rotate90().columnCount());
            assertEquals(0, longMatrix.rotate270().rowCount());
            assertEquals(2, longMatrix.rotate270().columnCount());
            assertEquals(0, floatMatrix.transpose().rowCount());
            assertEquals(2, floatMatrix.transpose().columnCount());
            assertEquals(0, floatMatrix.rotate90().rowCount());
            assertEquals(2, floatMatrix.rotate90().columnCount());
            assertEquals(0, floatMatrix.rotate270().rowCount());
            assertEquals(2, floatMatrix.rotate270().columnCount());
            assertEquals(0, doubleMatrix.transpose().rowCount());
            assertEquals(2, doubleMatrix.transpose().columnCount());
            assertEquals(0, doubleMatrix.rotate90().rowCount());
            assertEquals(2, doubleMatrix.rotate90().columnCount());
            assertEquals(0, doubleMatrix.rotate270().rowCount());
            assertEquals(2, doubleMatrix.rotate270().columnCount());
        }
    }

    @Nested
    class IsSameShapeTests {

        @Test
        public void testIsSameShape() {
            IntMatrix matrix1 = createTestMatrix();
            IntMatrix matrix2 = IntMatrix.wrap(new int[][] { { 10, 20, 30 }, { 40, 50, 60 }, { 70, 80, 90 } });
            IntMatrix matrix3 = createTestMatrix2x3();

            Assertions.assertTrue(matrix1.isSameShape(matrix2));
            Assertions.assertFalse(matrix1.isSameShape(matrix3));
        }

        @Test
        public void testIsSameShape_DifferentShape() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 } });
            assertFalse(m1.isSameShape(m2));
        }

        @Test
        public void testIsSameShape_DifferentRows() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertFalse(m1.isSameShape(m2));
        }

        @Test
        public void testIsSameShape_DifferentCols() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertFalse(m1.isSameShape(m2));
        }

        @Test
        public void testIsSameShape_IdenticalMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertTrue(m.isSameShape(m));
        }

        @Test
        public void testIsSameShape_DifferentRows_Shape1By2() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            assertFalse(m1.isSameShape(m2));
        }

        @Test
        public void testIsSameShape_DifferentCols_Shape2By3() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            assertFalse(m1.isSameShape(m2));
        }

        @Test
        public void testIsSameShape_SameShape() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            assertTrue(m1.isSameShape(m2));
        }

        @Test
        public void testIsSameShape_DifferentRows_Shape2By2() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 } });
            assertFalse(m1.isSameShape(m2));
        }

        @Test
        public void testIsSameShape_DifferentCols_Shape2By2() {
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6, 7 }, { 8, 9, 10 } });
            assertFalse(m1.isSameShape(m2));
        }

        @Test
        public void testisSameShape() {
            // From isSameShape Javadoc
            IntMatrix m1 = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix m2 = IntMatrix.wrap(new int[][] { { 5, 6 }, { 7, 8 } });
            assertTrue(m1.isSameShape(m2)); // Returns true (both are 2x2)

            IntMatrix m3 = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertFalse(m1.isSameShape(m3)); // Returns false (2x2 vs 2x3)
        }

        @Test
        public void testIsSameShape_EmptyMatrices() {
            IntMatrix m1 = IntMatrix.empty();
            IntMatrix m2 = IntMatrix.empty();
            assertTrue(m1.isSameShape(m2));
        }

        @Test
        public void testIsSameShape_NullThrows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.isSameShape(null));
        }
    }

    @Nested
    class ReshapeTests {

        @Test
        public void testReshapeRequiresExactElementCount() {
            IntMatrix matrix = createTestMatrix2x3();

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.reshape(2, 4));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.reshape(1, 4));
        }

        @Test
        public void testReshape_SameShape() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix reshaped = m.reshape(2, 2);

            assertEquals(2, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(m, reshaped);
        }

        @Test
        public void testReshape_TwoParams() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = m.reshape(3, 2);

            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(3, reshaped.get(1, 0));
            assertEquals(6, reshaped.get(2, 1));
        }

        @Test
        public void testReshape_TwoArgs() {
            // From reshape(int, int) Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = matrix.reshape(3, 2); // Becomes [[1, 2], [3, 4], [5, 6]]
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(3, reshaped.get(1, 0));
            assertEquals(4, reshaped.get(1, 1));
            assertEquals(5, reshaped.get(2, 0));
            assertEquals(6, reshaped.get(2, 1));

            IntMatrix extended = matrix.reshapeAndPad(2, 4); // Becomes [[1, 2, 3, 4], [5, 6, 0, 0]]
            assertEquals(2, extended.rowCount());
            assertEquals(4, extended.columnCount());
            assertEquals(4, extended.get(0, 3));
            assertEquals(0, extended.get(1, 2));
        }

        @Test
        public void testRectangularValidationMessageInterpolateValues() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(new Integer[][] { { 1, 2 }, { 3 } }));

            assertFalse(ex.getMessage().contains("{}"));
            // The message names the expected width and the offending row, and must not attribute the
            // expected width to a row: on the explicit-columnCount path it does not come from row 0.
            assertTrue(ex.getMessage().contains("expected 2 columns per row"), ex.getMessage());
            assertTrue(ex.getMessage().contains("row 1"), ex.getMessage());

            IllegalArgumentException explicit = assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(new int[][] { { 1, 2 } }).reshape(1, 3));
            assertFalse(explicit.getMessage().contains("{}"));
        }

        @Test
        public void testReshape_ToSingleRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix reshaped = m.reshape(1, 4);

            assertEquals(1, reshaped.rowCount());
            assertEquals(4, reshaped.columnCount());
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, reshaped.rowView(0));
        }

        @Test
        public void testReshape_ToSingleColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix reshaped = m.reshape(4, 1);

            assertEquals(4, reshaped.rowCount());
            assertEquals(1, reshaped.columnCount());
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, reshaped.columnCopy(0));
        }

        @Test
        public void testEmptyPointStreamsSkipTheNonEmptyAxis() {
            // This logical width requires no row arrays or element storage.
            IntMatrix zeroRows = IntMatrix.empty().reshape(0, Integer.MAX_VALUE);
            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                assertEquals(0L, zeroRows.columnMajorPoints().count());
                assertEquals(0L, zeroRows.columnMajorPoints(1, Integer.MAX_VALUE).count());
            });
            assertThrows(IndexOutOfBoundsException.class, () -> zeroRows.columnMajorPoints(-1, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> zeroRows.columnMajorPoints(2, 1));

            IntMatrix zeroColumns = IntMatrix.wrap(new int[3][0]);
            assertEquals(0L, zeroColumns.rowMajorPoints().count());
            assertEquals(0L, zeroColumns.rowMajorPoints(1, 3).count());
            assertThrows(IndexOutOfBoundsException.class, () -> zeroColumns.rowMajorPoints(0, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> zeroColumns.rowMajorPoints(2, 1));

            // Streams of rows/columns still expose their empty inner streams.
            assertEquals(3L, zeroColumns.rowPoints().count());
            assertEquals(4L, zeroRows.columnPoints(0, 4).count());
        }

        @Test
        public void testReshapeWithRowsAndCols() {
            IntMatrix matrix = createTestMatrix2x3();
            IntMatrix reshaped = matrix.reshape(3, 2);

            Assertions.assertEquals(3, reshaped.rowCount());
            Assertions.assertEquals(2, reshaped.columnCount());
            Assertions.assertEquals(1, reshaped.get(0, 0));
            Assertions.assertEquals(2, reshaped.get(0, 1));
            Assertions.assertEquals(3, reshaped.get(1, 0));
            Assertions.assertEquals(4, reshaped.get(1, 1));
        }

        @Test
        public void testReshape_WithNegativeDimensions_ThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(new String[][] { { "a" } }).reshape(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> Matrix.wrap(new String[][] { { "a" } }).reshape(1, -1));

            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.wrap(new boolean[][] { { true } }).reshape(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.wrap(new byte[][] { { 1 } }).reshape(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.wrap(new char[][] { { 'a' } }).reshape(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.wrap(new short[][] { { 1 } }).reshape(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(new int[][] { { 1 } }).reshape(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.wrap(new long[][] { { 1L } }).reshape(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.wrap(new float[][] { { 1F } }).reshape(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.wrap(new double[][] { { 1D } }).reshape(-1, 1));
        }

        @Test
        public void testReshape_WithRowsAndCols() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = m.reshape(3, 2);

            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(3, reshaped.get(1, 0));
            assertEquals(4, reshaped.get(1, 1));
            assertEquals(5, reshaped.get(2, 0));
            assertEquals(6, reshaped.get(2, 1));
        }

        @Test
        public void testReshape_TwoParams_TooSmallThrows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });

            // New shape is too small to hold all 6 elements
            assertThrows(IllegalArgumentException.class, () -> m.reshape(1, 3));
            assertThrows(IllegalArgumentException.class, () -> m.reshape(2, 2));
        }

        @Test
        public void testReshape_TwoArgs_TooSmallThrows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            assertThrows(IllegalArgumentException.class, () -> m.reshape(1, 2));
        }
    }

    @Nested
    class ReshapeAndPadTests {

        @Test
        public void testReshapeAndPad() {
            IntMatrix matrix = createTestMatrix2x3();
            IntMatrix reshaped = matrix.reshapeAndPad(2, 4);

            Assertions.assertEquals(2, reshaped.rowCount());
            Assertions.assertEquals(4, reshaped.columnCount());
            Assertions.assertEquals(6, reshaped.get(1, 1));
            Assertions.assertEquals(0, reshaped.get(1, 2));
            Assertions.assertEquals(0, reshaped.get(1, 3));
        }

        @Test
        public void testReshapeAndPad_LargerThanOriginal() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix reshaped = m.reshapeAndPad(3, 3);

            assertEquals(3, reshaped.rowCount());
            assertEquals(3, reshaped.columnCount());
            assertEquals(0, reshaped.get(2, 2));
        }
    }

    @Nested
    class ReshapeAndPadToColumnCountTests {

        @Test
        public void testReshape_Padding() {
            // From reshape(int) Javadoc: padding
            IntMatrix matrix2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped2 = matrix2.reshapeAndPadToColumnCount(4); // Becomes [[1, 2, 3, 4], [5, 6, 0, 0]]
            assertEquals(2, reshaped2.rowCount());
            assertEquals(4, reshaped2.columnCount());
            assertEquals(1, reshaped2.get(0, 0));
            assertEquals(2, reshaped2.get(0, 1));
            assertEquals(3, reshaped2.get(0, 2));
            assertEquals(4, reshaped2.get(0, 3));
            assertEquals(5, reshaped2.get(1, 0));
            assertEquals(6, reshaped2.get(1, 1));
            assertEquals(0, reshaped2.get(1, 2));
            assertEquals(0, reshaped2.get(1, 3));
        }

        @Test
        public void testReshapeAndPadToColumnCount() {
            IntMatrix matrix = createTestMatrix2x3();
            IntMatrix reshaped = matrix.reshapeAndPadToColumnCount(2);

            Assertions.assertEquals(3, reshaped.rowCount());
            Assertions.assertEquals(2, reshaped.columnCount());
            Assertions.assertEquals(1, reshaped.get(0, 0));
            Assertions.assertEquals(2, reshaped.get(0, 1));
            Assertions.assertEquals(3, reshaped.get(1, 0));
        }

        @Test
        public void testReshapeAndPadToColumnCount_Shape2By4() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3, 4 }, { 5, 6, 7, 8 } });
            IntMatrix reshaped = m.reshapeAndPadToColumnCount(2);

            assertEquals(4, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
        }

        @Test
        public void testReshapeAndPadToColumnCount_EvenlyDivisible() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            IntMatrix reshaped = m.reshapeAndPadToColumnCount(2);

            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
        }

        @Test
        public void testReshapeAndPadToColumnCount_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = m.reshapeAndPadToColumnCount(2);

            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(3, reshaped.get(1, 0));
            assertEquals(6, reshaped.get(2, 1));
        }

        @Test
        public void testReshapeAndPadToColumnCount_NeedsPadding() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = m.reshapeAndPadToColumnCount(4);

            assertEquals(2, reshaped.rowCount());
            assertEquals(4, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(4, reshaped.get(0, 3));
            assertEquals(5, reshaped.get(1, 0));
            assertEquals(0, reshaped.get(1, 3)); // Padding with default value
        }

        @Test
        public void testReshapeAndPadToColumnCount_PadsWithEachTypesDefaultValue() {
            assertEquals(CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', (char) 0 } }),
                    CharMatrix.wrap(new char[][] { { 'a', 'b', 'c' } }).reshapeAndPadToColumnCount(2));
            assertEquals(BooleanMatrix.wrap(new boolean[][] { { true, true }, { true, false } }),
                    BooleanMatrix.wrap(new boolean[][] { { true, true, true } }).reshapeAndPadToColumnCount(2));
            assertEquals(Matrix.wrap(String.class, new String[][] { { "a", "b" }, { "c", null } }),
                    Matrix.wrap(String.class, new String[][] { { "a", "b", "c" } }).reshapeAndPadToColumnCount(2));
        }

        // AbstractMatrix.java: DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
        @Test
        public void testReshape_SingleParameter_WithPadding() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = matrix.reshapeAndPadToColumnCount(4);
            assertEquals(2, reshaped.rowCount());
            assertEquals(4, reshaped.columnCount());
            assertEquals(5, reshaped.get(1, 0));
            assertEquals(6, reshaped.get(1, 1));
            assertEquals(0, reshaped.get(1, 2)); // Padded
            assertEquals(0, reshaped.get(1, 3)); // Padded
        }

        @Test
        public void testReshape_SingleArg() {
            // From reshape(int) Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntMatrix reshaped = matrix.reshapeAndPadToColumnCount(2); // Becomes [[1, 2], [3, 4], [5, 6]]
            assertEquals(3, reshaped.rowCount());
            assertEquals(2, reshaped.columnCount());
            assertEquals(1, reshaped.get(0, 0));
            assertEquals(2, reshaped.get(0, 1));
            assertEquals(3, reshaped.get(1, 0));
            assertEquals(4, reshaped.get(1, 1));
            assertEquals(5, reshaped.get(2, 0));
            assertEquals(6, reshaped.get(2, 1));
        }

        @Test
        public void testReshape_SingleColumnCount_ZeroThrows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.reshapeAndPadToColumnCount(0));
        }

        @Test
        public void testReshape_SingleColumnCount_NegativeThrows() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IllegalArgumentException.class, () -> m.reshapeAndPadToColumnCount(-1));
        }
    }

    @Nested
    class ResizeTests {

        // The message names the expected width and the offending row, and must not attribute the
        // expected width to a row: on the explicit-columnCount path it does not come from row 0.
        @Test
        public void testResizePreservesZeroRowNonZeroColumnShape() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });
            BooleanMatrix booleanMatrix = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            ByteMatrix byteMatrix = ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            CharMatrix charMatrix = CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            ShortMatrix shortMatrix = ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix intMatrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            LongMatrix longMatrix = LongMatrix.wrap(new long[][] { { 1L, 2L }, { 3L, 4L } });
            FloatMatrix floatMatrix = FloatMatrix.wrap(new float[][] { { 1F, 2F }, { 3F, 4F } });
            DoubleMatrix doubleMatrix = DoubleMatrix.wrap(new double[][] { { 1D, 2D }, { 3D, 4D } });

            assertEquals(1, matrix.resize(0, 1).columnCount());
            assertEquals(1, booleanMatrix.resize(0, 1).columnCount());
            assertEquals(1, byteMatrix.resize(0, 1).columnCount());
            assertEquals(1, charMatrix.resize(0, 1).columnCount());
            assertEquals(1, shortMatrix.resize(0, 1).columnCount());
            assertEquals(1, intMatrix.resize(0, 1).columnCount());
            assertEquals(1, longMatrix.resize(0, 1).columnCount());
            assertEquals(1, floatMatrix.resize(0, 1).columnCount());
            assertEquals(1, doubleMatrix.resize(0, 1).columnCount());
        }
    }

    @Nested
    class RepeatElementsTests {

        @Test
        public void testRepeatElements() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix repeated = matrix.repeatElements(2, 3);

            Assertions.assertEquals(4, repeated.rowCount());
            Assertions.assertEquals(6, repeated.columnCount());
            Assertions.assertEquals(1, repeated.get(0, 0));
            Assertions.assertEquals(1, repeated.get(0, 2));
            Assertions.assertEquals(1, repeated.get(1, 0));
            Assertions.assertEquals(2, repeated.get(0, 3));
        }

        @Test
        public void testRepeatElements_Shape1By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix repeated = m.repeatElements(2, 3);

            assertEquals(2, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(1, repeated.get(0, 1));
            assertEquals(1, repeated.get(0, 2));
            assertEquals(2, repeated.get(0, 3));
            assertEquals(2, repeated.get(0, 4));
            assertEquals(2, repeated.get(0, 5));
        }

        @Test
        public void testRepeatElements_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix repeated = m.repeatElements(2, 2);

            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(1, repeated.get(0, 1));
            assertEquals(1, repeated.get(1, 0));
            assertEquals(1, repeated.get(1, 1));
            assertEquals(4, repeated.get(2, 2));
            assertEquals(4, repeated.get(3, 3));
        }

        @Test
        public void testRepeatElements_Asymmetric() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix repeated = m.repeatElements(3, 2);

            assertEquals(3, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(1, repeated.get(0, 1));
            assertEquals(1, repeated.get(1, 0));
            assertEquals(1, repeated.get(2, 0));
            assertEquals(2, repeated.get(2, 3));
        }

        @Test
        public void testrepeatElements() {
            // From repeatElements Javadoc
            // Original:    repeatElements(2, 2):
            // 1 2          1 1 2 2
            // 3 4     =>   1 1 2 2
            //              3 3 4 4
            //              3 3 4 4
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix repeated = matrix.repeatElements(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(1, repeated.get(0, 1));
            assertEquals(2, repeated.get(0, 2));
            assertEquals(2, repeated.get(0, 3));
            assertEquals(1, repeated.get(1, 0));
            assertEquals(1, repeated.get(1, 1));
            assertEquals(2, repeated.get(1, 2));
            assertEquals(2, repeated.get(1, 3));
            assertEquals(3, repeated.get(2, 0));
            assertEquals(3, repeated.get(2, 1));
            assertEquals(4, repeated.get(2, 2));
            assertEquals(4, repeated.get(2, 3));
            assertEquals(3, repeated.get(3, 0));
            assertEquals(3, repeated.get(3, 1));
            assertEquals(4, repeated.get(3, 2));
            assertEquals(4, repeated.get(3, 3));
        }

        @Test
        public void testRepeatElementsInvalidArgs() {
            IntMatrix matrix = createTestMatrix();

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                matrix.repeatElements(0, 1);
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                matrix.repeatElements(1, 0);
            });
        }

        @Test
        public void testRepeatElements_InvalidArguments() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(-1, 1));
        }

        @Test
        public void testRepeatElements_InvalidArgs() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(1, 0));
            assertThrows(IllegalArgumentException.class, () -> m.repeatElements(-1, 1));
        }
    }

    @Nested
    class RepeatMatrixTests {

        @Test
        public void testRepeatMatrix() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix repeated = matrix.repeatMatrix(2, 3);

            repeated.println();

            Assertions.assertEquals(4, repeated.rowCount());
            Assertions.assertEquals(6, repeated.columnCount());
            Assertions.assertEquals(1, repeated.get(0, 0));
            Assertions.assertEquals(2, repeated.get(0, 1));
            Assertions.assertEquals(1, repeated.get(0, 2));
            Assertions.assertEquals(3, repeated.get(1, 0));
        }

        @Test
        public void testRepeatMatrix_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix repeated = m.repeatMatrix(2, 3);

            assertEquals(4, repeated.rowCount());
            assertEquals(6, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(2, repeated.get(0, 1));
            assertEquals(1, repeated.get(0, 2));
            assertEquals(1, repeated.get(2, 0));
        }

        @Test
        public void testRepeatMatrix_Shape2By2_TilesAndCornerValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix repeated = m.repeatMatrix(2, 2);

            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(2, repeated.get(0, 1));
            assertEquals(1, repeated.get(0, 2)); // Repeated
            assertEquals(1, repeated.get(2, 0)); // Repeated
            assertEquals(4, repeated.get(3, 3));
        }

        @Test
        public void testRepeatMatrix_Asymmetric() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            IntMatrix repeated = m.repeatMatrix(3, 2);

            assertEquals(3, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(2, repeated.get(0, 1));
            assertEquals(1, repeated.get(0, 2)); // Second repeatMatrix
            assertEquals(1, repeated.get(1, 0)); // Second row repeatMatrix
        }

        @Test
        public void testrepeatMatrix() {
            // From repeatMatrix Javadoc
            // Original:    repeatMatrix(2, 2):
            // 1 2          1 2 1 2
            // 3 4     =>   3 4 3 4
            //              1 2 1 2
            //              3 4 3 4
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix repeated = matrix.repeatMatrix(2, 2);
            assertEquals(4, repeated.rowCount());
            assertEquals(4, repeated.columnCount());
            assertEquals(1, repeated.get(0, 0));
            assertEquals(2, repeated.get(0, 1));
            assertEquals(1, repeated.get(0, 2));
            assertEquals(2, repeated.get(0, 3));
            assertEquals(3, repeated.get(1, 0));
            assertEquals(4, repeated.get(1, 1));
            assertEquals(3, repeated.get(1, 2));
            assertEquals(4, repeated.get(1, 3));
            assertEquals(1, repeated.get(2, 0));
            assertEquals(2, repeated.get(2, 1));
            assertEquals(1, repeated.get(2, 2));
            assertEquals(2, repeated.get(2, 3));
            assertEquals(3, repeated.get(3, 0));
            assertEquals(4, repeated.get(3, 1));
            assertEquals(3, repeated.get(3, 2));
            assertEquals(4, repeated.get(3, 3));
        }

        @Test
        public void testRepeatMatrixInvalidArgs() {
            IntMatrix matrix = createTestMatrix();

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                matrix.repeatMatrix(0, 1);
            });

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                matrix.repeatMatrix(1, -1);
            });
        }

        @Test
        public void testRepeatMatrix_InvalidArguments() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(-1, 1));
        }

        @Test
        public void testRepeatMatrix_InvalidArgs() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1 } });
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(0, 1));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(1, 0));
            assertThrows(IllegalArgumentException.class, () -> m.repeatMatrix(-1, 1));
        }
    }

    @Nested
    class PadTests {

        @Test
        public void testDirectionalPadPreservesZeroRowNonZeroColumnShapeOnEmptyMatrix() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[0][0]);
            BooleanMatrix booleanMatrix = BooleanMatrix.empty();
            ByteMatrix byteMatrix = ByteMatrix.empty();
            CharMatrix charMatrix = CharMatrix.empty();
            ShortMatrix shortMatrix = ShortMatrix.empty();
            IntMatrix intMatrix = IntMatrix.empty();
            LongMatrix longMatrix = LongMatrix.empty();
            FloatMatrix floatMatrix = FloatMatrix.empty();
            DoubleMatrix doubleMatrix = DoubleMatrix.empty();

            assertEquals(1, matrix.pad(0, 0, 0, 1).columnCount());
            assertEquals(1, booleanMatrix.pad(0, 0, 0, 1).columnCount());
            assertEquals(1, byteMatrix.pad(0, 0, 0, 1).columnCount());
            assertEquals(1, charMatrix.pad(0, 0, 0, 1).columnCount());
            assertEquals(1, shortMatrix.pad(0, 0, 0, 1).columnCount());
            assertEquals(1, intMatrix.pad(0, 0, 0, 1).columnCount());
            assertEquals(1, longMatrix.pad(0, 0, 0, 1).columnCount());
            assertEquals(1, floatMatrix.pad(0, 0, 0, 1).columnCount());
            assertEquals(1, doubleMatrix.pad(0, 0, 0, 1).columnCount());
        }
    }

    @Nested
    class FlattenTests {

        @Test
        public void testFlatten() {
            IntMatrix matrix = createTestMatrix2x3();
            IntList flat = matrix.flatten();

            Assertions.assertEquals(6, flat.size());
            Assertions.assertEquals(1, flat.get(0));
            Assertions.assertEquals(2, flat.get(1));
            Assertions.assertEquals(3, flat.get(2));
            Assertions.assertEquals(4, flat.get(3));
            Assertions.assertEquals(5, flat.get(4));
            Assertions.assertEquals(6, flat.get(5));
        }

        @Test
        public void testFlatten_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntList flat = m.flatten();

            assertEquals(6, flat.size());
            for (int i = 0; i < 6; i++) {
                assertEquals(i + 1, flat.get(i));
            }
        }

        @Test
        public void testFlatten_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntList flat = m.flatten();

            assertEquals(4, flat.size());
            assertEquals(1, flat.get(0));
            assertEquals(2, flat.get(1));
            assertEquals(3, flat.get(2));
            assertEquals(4, flat.get(3));
        }

        @Test
        public void testFlatten_DoubleMatrix() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            DoubleList flat = m.flatten();

            assertEquals(4, flat.size());
            assertEquals(1.5, flat.get(0), 0.001);
            assertEquals(2.5, flat.get(1), 0.001);
            assertEquals(3.5, flat.get(2), 0.001);
            assertEquals(4.5, flat.get(3), 0.001);
        }

        @Test
        public void testFlatten_ObjectMatrix() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            List<String> flat = m.flatten();

            assertEquals(4, flat.size());
            assertEquals("A", flat.get(0));
            assertEquals("B", flat.get(1));
            assertEquals("C", flat.get(2));
            assertEquals("D", flat.get(3));
        }

        @Test
        public void testflatten() {
            // From flatten Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntList flat = matrix.flatten(); // Returns [1, 2, 3, 4]
            assertEquals("[1, 2, 3, 4]", flat.toString());

            IntMatrix matrix2 = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            IntList flat2 = matrix2.flatten(); // Returns [1, 2, 3, 4, 5, 6]
            assertEquals("[1, 2, 3, 4, 5, 6]", flat2.toString());
        }

        @Test
        public void testFlatten_SingleElement() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 42 } });
            IntList flat = m.flatten();

            assertEquals(1, flat.size());
            assertEquals(42, flat.get(0));
        }

        @Test
        public void testEmptyMatrix_Operations() {
            IntMatrix empty = IntMatrix.empty();

            assertTrue(empty.isEmpty());
            assertTrue(empty.flatten().isEmpty());
            assertEquals(0, empty.rowMajorStream().count());
            assertEquals(0, empty.columnMajorStream().count());
            assertEquals(0, empty.rowStreams().count());
            assertEquals(0, empty.columnStreams().count());
        }

        @Test
        public void testSingleElement_Operations() {
            IntMatrix single = IntMatrix.wrap(new int[][] { { 42 } });

            assertFalse(single.isEmpty());
            assertEquals(1, single.flatten().size());
            assertEquals(42, single.rowMajorStream().findFirst().orElse(0));
        }

        @Test
        public void testFlatten_EmptyMatrix() {
            IntMatrix m = IntMatrix.empty();
            IntList flat = m.flatten();
            assertEquals(0, flat.size());
        }
    }

    @Nested
    class MutateViaFlatArrayTests {

        @Test
        public void testFlatOp() throws Exception {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 3, 1, 4 }, { 1, 5, 9 } });

            matrix.mutateViaFlatArray(arrays -> java.util.Arrays.sort(arrays));

            Assertions.assertEquals(1, matrix.get(0, 0));
            Assertions.assertEquals(1, matrix.get(0, 1));
            Assertions.assertEquals(3, matrix.get(0, 2));
        }

        @Test
        public void testMutateViaFlatArray_Shape2By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Integer> values = new ArrayList<>();

            m.mutateViaFlatArray(arr -> {
                for (int val : arr) {
                    values.add(val);
                }
            });

            assertEquals(6, values.size());
            assertEquals(1, values.get(0).intValue());
        }

        @Test
        public void testMutateViaFlatArray_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 3, 1 }, { 4, 2 } });
            AtomicInteger sum = new AtomicInteger(0);
            m.mutateViaFlatArray(arr -> {
                for (int val : arr) {
                    sum.addAndGet(val);
                }
            });
            assertEquals(10, sum.get());
        }

        @Test
        public void testMutateViaFlatArray_DoubleMatrix() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            final double[] sum = { 0.0 };
            m.mutateViaFlatArray(arr -> {
                for (double val : arr) {
                    sum[0] += val;
                }
            });
            assertEquals(12.0, sum[0], 0.001);
        }

        @Test
        public void testMutateViaFlatArray_ObjectMatrix() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "A", "B" }, { "C", "D" } });
            StringBuilder sb = new StringBuilder();
            m.mutateViaFlatArray(arr -> {
                for (String val : arr) {
                    sb.append(val);
                }
            });
            assertEquals("ABCD", sb.toString());
        }

        @Test
        public void testMutateViaFlatArray() {
            // From mutateViaFlatArray Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 3, 1, 4 }, { 1, 5, 9 } });
            matrix.mutateViaFlatArray(a -> java.util.Arrays.sort(a)); // Sorts all elements
            // Matrix becomes [[1, 1, 3], [4, 5, 9]] (elements sorted in row-major order)
            assertEquals(1, matrix.get(0, 0));
            assertEquals(1, matrix.get(0, 1));
            assertEquals(3, matrix.get(0, 2));
            assertEquals(4, matrix.get(1, 0));
            assertEquals(5, matrix.get(1, 1));
            assertEquals(9, matrix.get(1, 2));

            IntMatrix temporary = IntMatrix.wrap(new int[][] { { 1, 2 } });
            temporary.mutateViaFlatArray(flattened -> {
                flattened[0] = 9;
                assertEquals(1, temporary.get(0, 0));
            });
            assertArrayEquals(new int[] { 9, 2 }, temporary.rowCopy(0));

            assertThrows(IllegalStateException.class, () -> temporary.mutateViaFlatArray(flattened -> {
                flattened[1] = 8;
                throw new IllegalStateException("abort");
            }));
            assertArrayEquals(new int[] { 9, 2 }, temporary.rowCopy(0));

            AtomicInteger zeroRowCalls = new AtomicInteger();
            IntMatrix.empty().mutateViaFlatArray(flattened -> zeroRowCalls.incrementAndGet());
            assertEquals(0, zeroRowCalls.get());

            AtomicInteger zeroColumnCalls = new AtomicInteger();
            IntMatrix.wrap(new int[2][0]).mutateViaFlatArray(flattened -> {
                zeroColumnCalls.incrementAndGet();
                assertEquals(0, flattened.length);
            });
            assertEquals(1, zeroColumnCalls.get());

            int[] shared = { 1, 2 };
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrap(new int[][] { shared, shared }));
        }

        @Test
        public void testMutateViaFlatArray_FailureSkipsCopyBackWithoutRollingBackOtherReferences() {
            final IOException failure = new IOException("do not copy back");
            final IntMatrix integers = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertSame(failure, assertThrows(IOException.class, () -> integers.mutateViaFlatArray(flat -> {
                flat[0] = 99;
                integers.set(0, 1, 7);
                throw failure;
            })));
            assertEquals(IntMatrix.wrap(new int[][] { { 1, 7 } }), integers);

            final StringBuilder first = new StringBuilder("first");
            final StringBuilder second = new StringBuilder("second");
            final Matrix<StringBuilder> objects = Matrix.wrap(StringBuilder.class, new StringBuilder[][] { { first, second } });
            assertSame(failure, assertThrows(IOException.class, () -> objects.mutateViaFlatArray(flat -> {
                flat[0].append(" changed");
                flat[1] = new StringBuilder("replacement");
                throw failure;
            })));
            assertSame(first, objects.get(0, 0));
            assertEquals("first changed", objects.get(0, 0).toString());
            assertSame(second, objects.get(0, 1));
        }
    }

    @Nested
    class ForEachIndicesTests {

        @Test
        public void testForEachWithIndices() throws Exception {
            IntMatrix matrix = createTestMatrix2x3();
            List<String> positions = new ArrayList<>();

            matrix.forEachIndices((i, j) -> positions.add(i + "," + j));

            Assertions.assertEquals(6, positions.size());
            Assertions.assertTrue(positions.contains("0,0"));
            Assertions.assertTrue(positions.contains("1,2"));
        }

        @Test
        public void testForEachWithIndicesRegion() throws Exception {
            IntMatrix matrix = createTestMatrix();
            List<String> positions = new ArrayList<>();

            matrix.forEachIndices(1, 3, 1, 3, (i, j) -> positions.add(i + "," + j));

            Assertions.assertEquals(4, positions.size());
            Assertions.assertTrue(positions.contains("1,1"));
            Assertions.assertTrue(positions.contains("1,2"));
            Assertions.assertTrue(positions.contains("2,1"));
            Assertions.assertTrue(positions.contains("2,2"));
        }

        @Test
        public void testForEachWithMatrix() throws Exception {
            IntMatrix matrix = createTestMatrix2x3();
            List<Integer> values = new ArrayList<>();

            matrix.forEachIndices((i, j, m) -> values.add(m.get(i, j)));

            Assertions.assertEquals(6, values.size());
            Assertions.assertEquals(1, values.get(0));
            Assertions.assertEquals(6, values.get(5));
        }

        @Test
        public void testForEachWithMatrixRegion() throws Exception {
            IntMatrix matrix = createTestMatrix();
            List<Integer> values = new ArrayList<>();

            matrix.forEachIndices(0, 2, 0, 2, (i, j, m) -> values.add(m.get(i, j)));

            Assertions.assertEquals(4, values.size());
            Assertions.assertEquals(1, values.get(0));
            Assertions.assertEquals(2, values.get(1));
            Assertions.assertEquals(4, values.get(2));
            Assertions.assertEquals(5, values.get(3));
        }

        @Test
        public void testForEachIndicesUsesRowMajorOrderWhenParallelIsForcedOff() throws Exception {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 0, 0 }, { 0, 0 }, { 0, 0 } });
            List<Integer> expectedFull = new ArrayList<>();

            for (int i = 0; i < matrix.rowCount(); i++) {
                for (int j = 0; j < matrix.columnCount(); j++) {
                    expectedFull.add(i * matrix.columnCount() + j);
                }
            }

            List<Integer> visits = new ArrayList<>();
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> matrix.forEachIndices((i, j) -> {
                visits.add(i * matrix.columnCount() + j);
                matrix.set(i, j, i);
            }));
            assertEquals(expectedFull, visits);
            assertArrayEquals(new int[][] { { 0, 0 }, { 1, 1 }, { 2, 2 } }, matrix.unsafeBackingArray());

            visits.clear();
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> matrix.forEachIndices((i, j, m) -> {
                visits.add(i * m.columnCount() + j);
                m.set(i, j, i + 100);
            }));
            assertEquals(expectedFull, visits);
            assertArrayEquals(new int[][] { { 100, 100 }, { 101, 101 }, { 102, 102 } }, matrix.unsafeBackingArray());

            List<Integer> expectedRegion = new ArrayList<>();

            for (int i = 1; i < 3; i++) {
                for (int j = 0; j < matrix.columnCount(); j++) {
                    expectedRegion.add(i * matrix.columnCount() + j);
                }
            }

            visits.clear();
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> matrix.forEachIndices(1, 3, 0, matrix.columnCount(), (i, j) -> {
                visits.add(i * matrix.columnCount() + j);
                matrix.set(i, j, i + 200);
            }));
            assertEquals(expectedRegion, visits);
            assertArrayEquals(new int[][] { { 100, 100 }, { 201, 201 }, { 202, 202 } }, matrix.unsafeBackingArray());

            visits.clear();
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> matrix.forEachIndices(1, 3, 0, matrix.columnCount(), (i, j, m) -> {
                visits.add(i * m.columnCount() + j);
                m.set(i, j, i + 300);
            }));
            assertEquals(expectedRegion, visits);
            assertArrayEquals(new int[][] { { 100, 100 }, { 301, 301 }, { 302, 302 } }, matrix.unsafeBackingArray());
        }

        @Test
        public void testForEach_WithIndices() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<String> positions = new ArrayList<>();

            m.forEachIndices((i, j) -> positions.add(i + "," + j));

            assertEquals(4, positions.size());
            assertEquals("0,0", positions.get(0));
            assertEquals("0,1", positions.get(1));
            assertEquals("1,0", positions.get(2));
            assertEquals("1,1", positions.get(3));
        }

        @Test
        public void testForEach_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<String> positions = new ArrayList<>();

            m.forEachIndices(1, 3, 1, 3, (i, j) -> positions.add(i + "," + j));

            assertEquals(4, positions.size());
            assertEquals("1,1", positions.get(0));
            assertEquals("1,2", positions.get(1));
            assertEquals("2,1", positions.get(2));
            assertEquals("2,2", positions.get(3));
        }

        @Test
        public void testForEach_WithMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Integer> values = new ArrayList<>();

            m.forEachIndices((i, j, matrix) -> values.add(matrix.get(i, j)));

            assertEquals(4, values.size());
            assertEquals(1, values.get(0).intValue());
            assertEquals(2, values.get(1).intValue());
            assertEquals(3, values.get(2).intValue());
            assertEquals(4, values.get(3).intValue());
        }

        @Test
        public void testForEach_WithMatrixAndRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Integer> values = new ArrayList<>();

            m.forEachIndices(1, 3, 1, 3, (i, j, matrix) -> values.add(matrix.get(i, j)));

            assertEquals(4, values.size());
            assertEquals(5, values.get(0).intValue());
            assertEquals(6, values.get(1).intValue());
            assertEquals(8, values.get(2).intValue());
            assertEquals(9, values.get(3).intValue());
        }

        @Test
        public void testForEach_BiConsumer() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEachIndices((i, j) -> count.incrementAndGet());
            assertEquals(4, count.get());
        }

        @Test
        public void testForEach_BiConsumer_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            AtomicInteger count = new AtomicInteger(0);
            m.forEachIndices(1, 3, 1, 3, (i, j) -> count.incrementAndGet());
            assertEquals(4, count.get());
        }

        @Test
        public void testForEach_WithMatrix_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            AtomicInteger sum = new AtomicInteger(0);
            m.forEachIndices((i, j, matrix) -> sum.addAndGet(matrix.get(i, j)));
            assertEquals(10, sum.get());
        }

        @Test
        public void testForEach_WithMatrix_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            AtomicInteger sum = new AtomicInteger(0);
            m.forEachIndices(1, 3, 1, 3, (i, j, matrix) -> sum.addAndGet(matrix.get(i, j)));
            assertEquals(28, sum.get()); // 5 + 6 + 8 + 9
        }

        @Test
        public void testForEach_SimpleIteration() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            AtomicInteger sum = new AtomicInteger(0);
            matrix.forEachIndices((i, j) -> sum.addAndGet(matrix.get(i, j)));
            assertEquals(10, sum.get());
        }

        @Test
        public void testForEachIndices_WithMatrix_Shape2By2_SumsValuesUsingMatrixArgument() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            AtomicInteger sum = new AtomicInteger(0);
            matrix.forEachIndices((i, j, m) -> sum.addAndGet(m.get(i, j)));
            assertEquals(10, sum.get());
        }

        @Test
        public void testForEach_WithRegion() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            AtomicInteger sum = new AtomicInteger(0);
            matrix.forEachIndices(0, 2, 0, 2, (i, j) -> sum.addAndGet(matrix.get(i, j)));
            assertEquals(12, sum.get()); // 1+2+4+5
        }

        @Test
        public void testForEachIndices_SequentialFailureStopsTraversalAndPreservesCompletedWrites() {
            Matrices.runWithParallelMode(ParallelMode.FORCE_OFF, () -> {
                for (int variant = 0; variant < 4; variant++) {
                    final IntMatrix matrix = IntMatrix.wrap(new int[3][3]);
                    final List<Point> visited = new ArrayList<>();
                    final IOException failure = new IOException("stop after two positions");
                    final Throwables.IntBiConsumer<IOException> action = (row, column) -> {
                        visited.add(Point.of(row, column));
                        matrix.set(row, column, 9);
                        if (visited.size() == 2) {
                            throw failure;
                        }
                    };
                    final int selectedVariant = variant;

                    assertSame(failure, assertThrows(IOException.class, () -> {
                        switch (selectedVariant) {
                            case 0 -> matrix.forEachIndices(action);
                            case 1 -> matrix.forEachIndices(1, 3, 1, 3, action);
                            case 2 -> matrix.forEachIndices((row, column, suppliedMatrix) -> {
                                assertSame(matrix, suppliedMatrix);
                                action.accept(row, column);
                            });
                            case 3 -> matrix.forEachIndices(1, 3, 1, 3, (row, column, suppliedMatrix) -> {
                                assertSame(matrix, suppliedMatrix);
                                action.accept(row, column);
                            });
                            default -> throw new AssertionError("unexpected variant");
                        }
                    }));

                    if (variant % 2 == 0) {
                        assertEquals(List.of(Point.of(0, 0), Point.of(0, 1)), visited);
                        assertEquals(IntMatrix.wrap(new int[][] { { 9, 9, 0 }, { 0, 0, 0 }, { 0, 0, 0 } }), matrix);
                    } else {
                        assertEquals(List.of(Point.of(1, 1), Point.of(1, 2)), visited);
                        assertEquals(IntMatrix.wrap(new int[][] { { 0, 0, 0 }, { 0, 9, 9 }, { 0, 0, 0 } }), matrix);
                    }
                }
            });
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testForEachIndices_RegionInExplicitModes(final ParallelMode mode) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            final var visits = ConcurrentHashMap.<String> newKeySet();
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                matrix.forEachIndices(0, 2, 1, 3, (i, j) -> assertTrue(visits.add(i + "," + j)));
                assertEquals(Set.of("0,1", "0,2", "1,1", "1,2"), visits);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testForEachIndices_MatrixArgumentInExplicitModes(final ParallelMode mode) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            final var visits = ConcurrentHashMap.<String> newKeySet();
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                matrix.forEachIndices((i, j, original) -> {
                    assertSame(matrix, original);
                    assertTrue(visits.add(i + "," + j));
                });
                assertEquals(Set.of("0,0", "0,1", "0,2", "1,0", "1,1", "1,2"), visits);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @ParameterizedTest
        @EnumSource(value = ParallelMode.class, names = { "FORCE_OFF", "FORCE_ON" })
        public void testForEachIndices_RegionWithMatrixArgumentInExplicitModes(final ParallelMode mode) {
            final var matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            final var visits = ConcurrentHashMap.<String> newKeySet();
            final ParallelMode previous = Matrices.getParallelMode();
            Matrices.setParallelMode(mode);
            try {
                matrix.forEachIndices(0, 2, 1, 3, (i, j, original) -> {
                    assertSame(matrix, original);
                    assertTrue(visits.add(i + "," + j));
                });
                assertEquals(Set.of("0,1", "0,2", "1,1", "1,2"), visits);
            } finally {
                Matrices.setParallelMode(previous);
            }
        }

        @Test
        public void testForEachNullAction() {
            IntMatrix matrix = createTestMatrix();

            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.forEachIndices((Throwables.IntBiConsumer<RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> matrix.forEachIndices(0, matrix.rowCount(), 0, matrix.columnCount(), (Throwables.IntBiConsumer<RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> matrix.forEachIndices((Throwables.BiIntObjConsumer<IntMatrix, RuntimeException>) null));
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.forEachIndices(0, matrix.rowCount(), 0, matrix.columnCount(),
                    (Throwables.BiIntObjConsumer<IntMatrix, RuntimeException>) null));
        }

        @Test
        public void testForEach_EmptyMatrix() {
            IntMatrix empty = IntMatrix.empty();
            List<String> positions = new ArrayList<>();

            empty.forEachIndices((i, j) -> positions.add(i + "," + j));

            assertTrue(positions.isEmpty());
        }

        @Test
        public void testForEach_EmptyRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<String> positions = new ArrayList<>();

            m.forEachIndices(1, 1, 0, 2, (i, j) -> positions.add(i + "," + j));

            assertTrue(positions.isEmpty());
        }

        @Test
        public void testForEach_WithMatrix_EmptyMatrix() {
            IntMatrix empty = IntMatrix.empty();
            List<Integer> values = new ArrayList<>();

            empty.forEachIndices((i, j, matrix) -> values.add(matrix.get(i, j)));

            assertTrue(values.isEmpty());
        }

        @Test
        public void testForEach_WithMatrixAndRange_EmptyRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Integer> values = new ArrayList<>();

            m.forEachIndices(0, 0, 1, 1, (i, j, matrix) -> values.add(matrix.get(i, j)));

            assertTrue(values.isEmpty());
        }

        @Test
        public void testForEach_WithRegionAndMatrix() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            AtomicInteger sum = new AtomicInteger(0);
            matrix.forEachIndices(1, 3, 1, 3, (i, j, m) -> sum.addAndGet(m.get(i, j)));
            assertEquals(28, sum.get()); // 5+6+8+9
        }

        @Test
        public void testForEach_EmptyMatrix_Shape0By0() {
            IntMatrix matrix = IntMatrix.wrap(new int[0][0]);
            AtomicInteger count = new AtomicInteger(0);
            matrix.forEachIndices((i, j) -> count.incrementAndGet());
            assertEquals(0, count.get());
        }

        @Test
        public void testForEach_WithRange_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEachIndices(-1, 2, 0, 2, (i, j) -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEachIndices(0, 3, 0, 2, (i, j) -> {
            }));
        }

        @Test
        public void testForEach_BiConsumer_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEachIndices(-1, 2, 0, 2, (i, j) -> {
            }));
            assertThrows(IndexOutOfBoundsException.class, () -> m.forEachIndices(0, 3, 0, 2, (i, j) -> {
            }));
        }
    }

    @Nested
    class Adjacent4PointsTests {

        @Test
        public void testAdjacent4Points_Center() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][3]);
            List<Point> points = m.adjacent4Points(1, 1).toList();
            assertEquals(4, points.size());
            assertTrue(points.contains(Point.of(0, 1)));
            assertTrue(points.contains(Point.of(1, 2)));
            assertTrue(points.contains(Point.of(2, 1)));
            assertTrue(points.contains(Point.of(1, 0)));
        }

        @Test
        public void testAdjacent4Points_Corner() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[2][2]);
            List<Point> points = m.adjacent4Points(0, 0).toList();
            assertEquals(2, points.size());
            assertTrue(points.contains(Point.of(0, 1)));
            assertTrue(points.contains(Point.of(1, 0)));
        }

        @Test
        public void testAdjacent4Points_VisitOrder() {
            // The Javadoc promises the visit order up, right, down, left, with out-of-bounds
            // neighbors skipped without disturbing the relative order.
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][3]);
            assertEquals(List.of(Point.of(0, 1), Point.of(1, 2), Point.of(2, 1), Point.of(1, 0)), m.adjacent4Points(1, 1).toList());
            assertEquals(List.of(Point.of(0, 1), Point.of(1, 0)), m.adjacent4Points(0, 0).toList());
        }

        @Test
        public void testAdjacent4Points_CenterElement() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Point> points = m.adjacent4Points(1, 1).toList();
            assertEquals(4, points.size());
            assertNotNull(points.get(0)); // up
            assertNotNull(points.get(1)); // right
            assertNotNull(points.get(2)); // down
            assertNotNull(points.get(3)); // left
        }

        @Test
        public void testAdjacent4Points_CornerElement() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Point> points = m.adjacent4Points(0, 0).toList();
            assertEquals(2, points.size());
            assertNotNull(points.get(0)); // right
            assertNotNull(points.get(1)); // down
        }

        @Test
        public void testAdjacent4Points_CenterPosition() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = matrix.adjacent4Points(1, 1).toList();
            assertEquals(4, points.size());
            assertTrue(points.contains(Point.of(0, 1))); // up
            assertTrue(points.contains(Point.of(1, 2))); // right
            assertTrue(points.contains(Point.of(2, 1))); // down
            assertTrue(points.contains(Point.of(1, 0))); // left
        }

        @Test
        public void testAdjacent4Points_TopLeftCorner() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = matrix.adjacent4Points(0, 0).toList();
            assertEquals(2, points.size());
            assertTrue(points.contains(Point.of(0, 1))); // right
            assertTrue(points.contains(Point.of(1, 0))); // down
        }

        @Test
        public void testAdjacent4Points_BottomRightCorner() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = matrix.adjacent4Points(1, 1).toList();
            assertEquals(2, points.size());
            assertTrue(points.contains(Point.of(0, 1))); // up
            assertTrue(points.contains(Point.of(1, 0))); // left
        }

        @Test
        public void testadjacent4Points_Corner() {
            // From adjacent4Points Javadoc
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Stream<Point> adjacent = matrix.adjacent4Points(0, 0);
            // Returns stream of Point(0, 1) and Point(1, 0) - only right and down exist
            List<Point> points = adjacent.toList();
            assertEquals(2, points.size());
            assertTrue(points.contains(Point.of(0, 1)));
            assertTrue(points.contains(Point.of(1, 0)));
        }

        @Test
        public void testadjacent4Points_Center() {
            // From adjacent4Points Javadoc
            IntMatrix larger = IntMatrix.wrap(new int[3][3]);
            Stream<Point> centerAdj = larger.adjacent4Points(1, 1);
            // Returns all 4 adjacent points: (0,1), (1,2), (2,1), (1,0)
            List<Point> points = centerAdj.toList();
            assertEquals(4, points.size());
            assertTrue(points.contains(Point.of(0, 1)));
            assertTrue(points.contains(Point.of(1, 2)));
            assertTrue(points.contains(Point.of(2, 1)));
            assertTrue(points.contains(Point.of(1, 0)));
        }
    }

    @Nested
    class Adjacent8PointsTests {

        @Test
        public void testAdjacent8Points_Center() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][3]);
            List<Point> points = m.adjacent8Points(1, 1).toList();
            assertEquals(8, points.size());
            assertTrue(points.contains(Point.of(0, 0)));
            assertTrue(points.contains(Point.of(0, 1)));
            assertTrue(points.contains(Point.of(0, 2)));
            assertTrue(points.contains(Point.of(1, 0)));
            assertTrue(points.contains(Point.of(1, 2)));
            assertTrue(points.contains(Point.of(2, 0)));
            assertTrue(points.contains(Point.of(2, 1)));
            assertTrue(points.contains(Point.of(2, 2)));
        }

        @Test
        public void testAdjacent8Points_Corner() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[2][2]);
            List<Point> points = m.adjacent8Points(0, 0).toList();
            assertEquals(3, points.size());
            assertTrue(points.contains(Point.of(0, 1)));
            assertTrue(points.contains(Point.of(1, 0)));
            assertTrue(points.contains(Point.of(1, 1)));
        }

        @Test
        public void testAdjacent8Points_VisitOrder() {
            // The Javadoc promises clockwise order starting from the top-left: leftUp, up,
            // rightUp, right, rightDown, down, leftDown, left.
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[3][3]);
            assertEquals(
                    List.of(Point.of(0, 0), Point.of(0, 1), Point.of(0, 2), Point.of(1, 2), Point.of(2, 2), Point.of(2, 1), Point.of(2, 0), Point.of(1, 0)),
                    m.adjacent8Points(1, 1).toList());
            assertEquals(List.of(Point.of(0, 1), Point.of(1, 1), Point.of(1, 0)), m.adjacent8Points(0, 0).toList());
        }

        @Test
        public void testAdjacent8Points_CenterElement() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            List<Point> points = m.adjacent8Points(1, 1).toList();
            assertEquals(8, points.size());
            // All should be non-null for center element
            for (Point p : points) {
                assertNotNull(p);
            }
        }

        @Test
        public void testAdjacent8Points_CornerElement() {
            BooleanMatrix m = BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            List<Point> points = m.adjacent8Points(0, 0).toList();
            assertEquals(3, points.size());
            assertNotNull(points.get(0)); // right
            assertNotNull(points.get(1)); // right-down
            assertNotNull(points.get(2)); // down
        }

        @Test
        public void testAdjacent8Points_CenterPosition() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = matrix.adjacent8Points(1, 1).toList();
            assertEquals(8, points.size());
        }

        @Test
        public void testAdjacent8Points_TopLeftCorner() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = matrix.adjacent8Points(0, 0).toList();
            assertEquals(3, points.size());
            assertTrue(points.contains(Point.of(0, 1))); // right
            assertTrue(points.contains(Point.of(1, 1))); // rightDown
            assertTrue(points.contains(Point.of(1, 0))); // down
        }

        @Test
        public void testAdjacent8Points_BottomRightCorner() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = matrix.adjacent8Points(1, 1).toList();
            assertEquals(3, points.size());
            assertTrue(points.contains(Point.of(0, 0))); // leftUp
            assertTrue(points.contains(Point.of(0, 1))); // up
            assertTrue(points.contains(Point.of(1, 0))); // left
        }

        @Test
        public void testadjacent8Points_Corner() {
            // From adjacent8Points Javadoc
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            Stream<Point> corner = matrix.adjacent8Points(0, 0);
            // Returns 3 points: (0,1), (1,1), (1,0)
            List<Point> points = corner.toList();
            assertEquals(3, points.size());
            assertTrue(points.contains(Point.of(0, 1)));
            assertTrue(points.contains(Point.of(1, 1)));
            assertTrue(points.contains(Point.of(1, 0)));
        }

        @Test
        public void testadjacent8Points_Center() {
            // From adjacent8Points Javadoc
            BooleanMatrix matrix = BooleanMatrix.wrap(new boolean[][] { { true, false, true }, { false, true, false }, { true, false, true } });
            Stream<Point> adjacent = matrix.adjacent8Points(1, 1);
            // Returns stream of all 8 surrounding points for the center position
            List<Point> points = adjacent.toList();
            assertEquals(8, points.size());
        }

        @Test
        public void testAdjacent8Points_EdgeMidpointsExcludeOutsideCoordinates() {
            final var matrix = IntMatrix.wrap(new int[3][3]);
            assertEquals(List.of(Point.of(0, 0), Point.of(0, 1), Point.of(1, 1), Point.of(2, 1), Point.of(2, 0)), matrix.adjacent8Points(1, 0).toList());
            assertEquals(List.of(Point.of(0, 1), Point.of(0, 2), Point.of(2, 2), Point.of(2, 1), Point.of(1, 1)), matrix.adjacent8Points(1, 2).toList());
        }
    }

    @Nested
    class MainDiagonalPointsTests {

        @Test
        public void testMainDiagonalPoints() {
            IntMatrix matrix = createTestMatrix();
            List<Sheet.Point> points = matrix.mainDiagonalPoints().collect(Collectors.toList());

            Assertions.assertEquals(3, points.size());
            Assertions.assertEquals(Sheet.Point.of(0, 0), points.get(0));
            Assertions.assertEquals(Sheet.Point.of(1, 1), points.get(1));
            Assertions.assertEquals(Sheet.Point.of(2, 2), points.get(2));
        }

        @Test
        public void testMainDiagonalPointsNonSquare() {
            IntMatrix matrix = createTestMatrix2x3();

            List<Sheet.Point> points = matrix.mainDiagonalPoints().collect(Collectors.toList());
            Assertions.assertEquals(java.util.Arrays.asList(Sheet.Point.of(0, 0), Sheet.Point.of(1, 1)), points);
        }

        @Test
        public void testMainDiagonalPoints_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = m.mainDiagonalPoints().toList();

            assertEquals(3, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(2, 2), points.get(2));
        }

        @Test
        public void testMainDiagonalPoints_NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertEquals(java.util.Arrays.asList(Point.of(0, 0)), m.mainDiagonalPoints().toList());
        }

        @Test
        public void testMainDiagonalPoints_Shape3By3_CoordinatesInDiagonalOrder() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            Stream<Point> points = m.mainDiagonalPoints();
            List<Point> list = points.toList();

            assertEquals(3, list.size());
            assertEquals(0, list.get(0).rowIndex());
            assertEquals(0, list.get(0).columnIndex());
            assertEquals(1, list.get(1).rowIndex());
            assertEquals(1, list.get(1).columnIndex());
            assertEquals(2, list.get(2).rowIndex());
            assertEquals(2, list.get(2).columnIndex());
        }

        @Test
        public void testMainDiagonalPoints_NonSquare_Shape3By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertEquals(java.util.Arrays.asList(Point.of(0, 0), Point.of(1, 1)), m.mainDiagonalPoints().toList());
        }

        @Test
        public void testMainDiagonalPoints_SquareMatrix() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = matrix.mainDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(2, 2), points.get(2));
        }

        @Test
        public void testMainDiagonalPoints_NonSquareMatrix() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertEquals(java.util.Arrays.asList(Point.of(0, 0), Point.of(1, 1)), matrix.mainDiagonalPoints().toList());
        }

        @Test
        public void testPointsMainDiagonal_EmptyMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[0][0]);
            assertEquals(0, m.mainDiagonalPoints().count());
        }
    }

    @Nested
    class AntiDiagonalPointsTests {

        @Test
        public void testAntiDiagonalPoints() {
            IntMatrix matrix = createTestMatrix();
            List<Sheet.Point> points = matrix.antiDiagonalPoints().collect(Collectors.toList());

            Assertions.assertEquals(3, points.size());
            Assertions.assertEquals(Sheet.Point.of(0, 2), points.get(0));
            Assertions.assertEquals(Sheet.Point.of(1, 1), points.get(1));
            Assertions.assertEquals(Sheet.Point.of(2, 0), points.get(2));
        }

        @Test
        public void testAntiDiagonalPoints_Shape3By3() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = m.antiDiagonalPoints().toList();

            assertEquals(3, points.size());
            assertEquals(Point.of(0, 2), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(2, 0), points.get(2));
        }

        @Test
        public void testAntiDiagonalPoints_NonSquare() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 } });
            assertEquals(java.util.Arrays.asList(Point.of(0, 1)), m.antiDiagonalPoints().toList());
        }

        @Test
        public void testAntiDiagonalPoints_Shape3By3_CoordinatesInDiagonalOrder() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            Stream<Point> points = m.antiDiagonalPoints();
            List<Point> list = points.toList();

            assertEquals(3, list.size());
            assertEquals(0, list.get(0).rowIndex());
            assertEquals(2, list.get(0).columnIndex());
            assertEquals(1, list.get(1).rowIndex());
            assertEquals(1, list.get(1).columnIndex());
            assertEquals(2, list.get(2).rowIndex());
            assertEquals(0, list.get(2).columnIndex());
        }

        @Test
        public void testAntiDiagonalPoints_NonSquare_Shape3By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertEquals(java.util.Arrays.asList(Point.of(0, 1), Point.of(1, 0)), m.antiDiagonalPoints().toList());
        }

        @Test
        public void testAntiDiagonalPoints_SquareMatrix() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<Point> points = matrix.antiDiagonalPoints().toList();
            assertEquals(3, points.size());
            assertEquals(Point.of(0, 2), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(2, 0), points.get(2));
        }

        @Test
        public void testAntiDiagonalPoints_NonSquareMatrix() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            assertEquals(java.util.Arrays.asList(Point.of(0, 1), Point.of(1, 0)), matrix.antiDiagonalPoints().toList());
        }

        @Test
        public void testPointsAntiDiagonal_EmptyMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[0][0]);
            assertEquals(0, m.antiDiagonalPoints().count());
        }
    }

    @Nested
    class RowMajorPointsTests {

        @Test
        public void testRowMajorPoints() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = matrix.rowMajorPoints().collect(Collectors.toList());

            Assertions.assertEquals(4, points.size());
            Assertions.assertEquals(Sheet.Point.of(0, 0), points.get(0));
            Assertions.assertEquals(Sheet.Point.of(0, 1), points.get(1));
            Assertions.assertEquals(Sheet.Point.of(1, 0), points.get(2));
            Assertions.assertEquals(Sheet.Point.of(1, 1), points.get(3));
        }

        @Test
        public void testRowMajorPointsRow() {
            IntMatrix matrix = createTestMatrix2x3();
            List<Sheet.Point> points = matrix.rowMajorPoints(1, 1 + 1).collect(Collectors.toList());

            Assertions.assertEquals(3, points.size());
            Assertions.assertEquals(Sheet.Point.of(1, 0), points.get(0));
            Assertions.assertEquals(Sheet.Point.of(1, 1), points.get(1));
            Assertions.assertEquals(Sheet.Point.of(1, 2), points.get(2));
        }

        @Test
        public void testRowMajorPointsRange() {
            IntMatrix matrix = createTestMatrix();
            List<Sheet.Point> points = matrix.rowMajorPoints(1, 3).collect(Collectors.toList());

            Assertions.assertEquals(6, points.size());
            Assertions.assertEquals(Sheet.Point.of(1, 0), points.get(0));
            Assertions.assertEquals(Sheet.Point.of(2, 2), points.get(5));
        }

        @Test
        public void testRowMajorPoints_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.rowMajorPoints().toList();

            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(0, 1), points.get(1));
            assertEquals(Point.of(1, 0), points.get(2));
            assertEquals(Point.of(1, 1), points.get(3));
        }

        @Test
        public void testRowMajorPoints_WithRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.rowMajorPoints(1, 1 + 1).toList();

            assertEquals(2, points.size());
            assertEquals(Point.of(1, 0), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
        }

        @Test
        public void testRowMajorPoints_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<Point> points = m.rowMajorPoints(1, 3).toList();

            assertEquals(4, points.size());
            assertEquals(Point.of(1, 0), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(2, 0), points.get(2));
            assertEquals(Point.of(2, 1), points.get(3));
        }

        @Test
        public void testRowMajorPoints_Shape2By2_CoordinatesInRowOrder() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Stream<Point> points = m.rowMajorPoints();
            List<Point> list = points.toList();

            assertEquals(4, list.size());
            assertEquals(0, list.get(0).rowIndex());
            assertEquals(0, list.get(0).columnIndex());
            assertEquals(0, list.get(1).rowIndex());
            assertEquals(1, list.get(1).columnIndex());
            assertEquals(1, list.get(2).rowIndex());
            assertEquals(0, list.get(2).columnIndex());
        }

        @Test
        public void testRowMajorPoints_RowRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            Stream<Point> points = m.rowMajorPoints(1, 3);
            List<Point> list = points.toList();

            assertEquals(4, list.size());
            assertEquals(1, list.get(0).rowIndex());
            assertEquals(2, list.get(2).rowIndex());
        }

        @Test
        public void testRowMajorPoints_AllPoints() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = matrix.rowMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(0, 1), points.get(1));
            assertEquals(Point.of(1, 0), points.get(2));
            assertEquals(Point.of(1, 1), points.get(3));
        }

        @Test
        public void testRowMajorPoints_RowRange_Shape3By2() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<Point> points = matrix.rowMajorPoints(1, 3).toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(1, 0), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(2, 0), points.get(2));
            assertEquals(Point.of(2, 1), points.get(3));
        }

        // copy with from == to is a valid empty row slice; the column count is preserved.
        @Test
        public void testRowMajorPoints_EmptyMatrix() {
            IntMatrix empty = IntMatrix.empty();
            long count = empty.rowMajorPoints().count();
            assertEquals(0, count);
        }

        @Test
        public void testRowMajorPoints_SingleRow() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Stream<Point> points = m.rowMajorPoints(1, 1 + 1);
            List<Point> list = points.toList();

            assertEquals(3, list.size());
            assertEquals(1, list.get(0).rowIndex());
            assertEquals(0, list.get(0).columnIndex());
            assertEquals(1, list.get(2).rowIndex());
            assertEquals(2, list.get(2).columnIndex());
        }

        @Test
        public void testRowMajorPoints_SingleRow_Shape2By2() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = matrix.rowMajorPoints(1, 1 + 1).toList();
            assertEquals(2, points.size());
            assertEquals(Point.of(1, 0), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
        }

        @Test
        public void testRowMajorPoints_WithRange_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorPoints(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowMajorPoints(0, 3));
        }
    }

    @Nested
    class ColumnMajorPointsTests {

        @Test
        public void testColumnMajorPoints() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Sheet.Point> points = matrix.columnMajorPoints().collect(Collectors.toList());

            Assertions.assertEquals(4, points.size());
            Assertions.assertEquals(Sheet.Point.of(0, 0), points.get(0));
            Assertions.assertEquals(Sheet.Point.of(1, 0), points.get(1));
            Assertions.assertEquals(Sheet.Point.of(0, 1), points.get(2));
            Assertions.assertEquals(Sheet.Point.of(1, 1), points.get(3));
        }

        @Test
        public void testColumnMajorPointsColumn() {
            IntMatrix matrix = createTestMatrix2x3();
            List<Sheet.Point> points = matrix.columnMajorPoints(1, 1 + 1).collect(Collectors.toList());

            Assertions.assertEquals(2, points.size());
            Assertions.assertEquals(Sheet.Point.of(0, 1), points.get(0));
            Assertions.assertEquals(Sheet.Point.of(1, 1), points.get(1));
        }

        @Test
        public void testColumnMajorPointsRange() {
            IntMatrix matrix = createTestMatrix2x3();
            List<Sheet.Point> points = matrix.columnMajorPoints(1, 3).collect(Collectors.toList());

            Assertions.assertEquals(4, points.size());
            Assertions.assertEquals(Sheet.Point.of(0, 1), points.get(0));
            Assertions.assertEquals(Sheet.Point.of(1, 2), points.get(3));
        }

        @Test
        public void testColumnMajorPoints_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.columnMajorPoints().toList();

            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 0), points.get(1));
            assertEquals(Point.of(0, 1), points.get(2));
            assertEquals(Point.of(1, 1), points.get(3));
        }

        @Test
        public void testColumnMajorPoints_WithColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = m.columnMajorPoints(1, 1 + 1).toList();

            assertEquals(2, points.size());
            assertEquals(Point.of(0, 1), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
        }

        @Test
        public void testColumnMajorPoints_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Point> points = m.columnMajorPoints(1, 3).toList();

            assertEquals(4, points.size());
            assertEquals(Point.of(0, 1), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(0, 2), points.get(2));
            assertEquals(Point.of(1, 2), points.get(3));
        }

        @Test
        public void testColumnMajorPoints_Shape2By2_CoordinatesInColumnOrder() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Stream<Point> points = m.columnMajorPoints();
            List<Point> list = points.toList();

            assertEquals(4, list.size());
            assertEquals(0, list.get(0).rowIndex());
            assertEquals(0, list.get(0).columnIndex());
            assertEquals(1, list.get(1).rowIndex());
            assertEquals(0, list.get(1).columnIndex());
            assertEquals(0, list.get(2).rowIndex());
            assertEquals(1, list.get(2).columnIndex());
        }

        @Test
        public void testColumnMajorPoints_ColumnRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Stream<Point> points = m.columnMajorPoints(1, 3);
            List<Point> list = points.toList();

            assertEquals(4, list.size());
            assertEquals(1, list.get(0).columnIndex());
            assertEquals(2, list.get(2).columnIndex());
        }

        @Test
        public void testColumnMajorPoints_AllPoints() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = matrix.columnMajorPoints().toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 0), points.get(1));
            assertEquals(Point.of(0, 1), points.get(2));
            assertEquals(Point.of(1, 1), points.get(3));
        }

        @Test
        public void testColumnMajorPoints_ColumnRange_Shape2By3() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Point> points = matrix.columnMajorPoints(1, 3).toList();
            assertEquals(4, points.size());
            assertEquals(Point.of(0, 1), points.get(0));
            assertEquals(Point.of(1, 1), points.get(1));
            assertEquals(Point.of(0, 2), points.get(2));
            assertEquals(Point.of(1, 2), points.get(3));
        }

        @Test
        public void testColumnMajorPoints_EmptyMatrix() {
            IntMatrix empty = IntMatrix.empty();
            long count = empty.columnMajorPoints().count();
            assertEquals(0, count);
        }

        @Test
        public void testColumnMajorPoints_SingleColumn() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Stream<Point> points = m.columnMajorPoints(1, 1 + 1);
            List<Point> list = points.toList();

            assertEquals(2, list.size());
            assertEquals(0, list.get(0).rowIndex());
            assertEquals(1, list.get(0).columnIndex());
            assertEquals(1, list.get(1).rowIndex());
            assertEquals(1, list.get(1).columnIndex());
        }

        @Test
        public void testColumnMajorPoints_SingleColumn_Shape2By2() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Point> points = matrix.columnMajorPoints(0, 0 + 1).toList();
            assertEquals(2, points.size());
            assertEquals(Point.of(0, 0), points.get(0));
            assertEquals(Point.of(1, 0), points.get(1));
        }

        @Test
        public void testColumnMajorPoints_WithRange_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorPoints(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnMajorPoints(0, 3));
        }
    }

    @Nested
    class RowPointsTests {

        @Test
        public void testRowPoints() {
            IntMatrix matrix = createTestMatrix2x3();
            List<List<Sheet.Point>> rows = matrix.rowPoints().map(stream -> stream.collect(Collectors.toList())).collect(Collectors.toList());

            Assertions.assertEquals(2, rows.size());
            Assertions.assertEquals(3, rows.get(0).size());
            Assertions.assertEquals(Sheet.Point.of(0, 0), rows.get(0).get(0));
            Assertions.assertEquals(Sheet.Point.of(1, 2), rows.get(1).get(2));
        }

        @Test
        public void testRowPointsRange() {
            IntMatrix matrix = createTestMatrix();
            List<List<Sheet.Point>> rows = matrix.rowPoints(1, 3).map(stream -> stream.collect(Collectors.toList())).collect(Collectors.toList());

            Assertions.assertEquals(2, rows.size());
            Assertions.assertEquals(3, rows.get(0).size());
            Assertions.assertEquals(Sheet.Point.of(1, 0), rows.get(0).get(0));
        }

        @Test
        public void testRowPoints_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<List<Point>> rowPoints = m.rowPoints().map(s -> s.toList()).toList();

            assertEquals(2, rowPoints.size());
            assertEquals(2, rowPoints.get(0).size());
            assertEquals(Point.of(0, 0), rowPoints.get(0).get(0));
            assertEquals(Point.of(0, 1), rowPoints.get(0).get(1));
        }

        @Test
        public void testRowPoints_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<List<Point>> rowPoints = m.rowPoints(1, 3).map(s -> s.toList()).toList();

            assertEquals(2, rowPoints.size());
            assertEquals(Point.of(1, 0), rowPoints.get(0).get(0));
            assertEquals(Point.of(2, 0), rowPoints.get(1).get(0));
        }

        @Test
        public void testRowPoints_Shape2By2_GroupedRowCoordinates() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Stream<Stream<Point>> rows = m.rowPoints();
            List<List<Point>> list = rows.map(Stream::toList).toList();

            assertEquals(2, list.size());
            assertEquals(2, list.get(0).size());
            assertEquals(0, list.get(0).get(0).rowIndex());
            assertEquals(1, list.get(1).get(0).rowIndex());
        }

        @Test
        public void testRowPoints_RowRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            Stream<Stream<Point>> rows = m.rowPoints(1, 3);
            List<List<Point>> list = rows.map(Stream::toList).toList();

            assertEquals(2, list.size());
            assertEquals(1, list.get(0).get(0).rowIndex());
        }

        @Test
        public void testRowPoints_AllRows() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<List<Point>> rows = matrix.rowPoints().map(Stream::toList).toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).size());
            assertEquals(Point.of(0, 0), rows.get(0).get(0));
            assertEquals(Point.of(0, 1), rows.get(0).get(1));
        }

        @Test
        public void testRowPoints_RowRange_Shape3By2() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<List<Point>> rows = matrix.rowPoints(1, 3).map(Stream::toList).toList();
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(0).size());
        }

        @Test
        public void testRowPoints_EmptyMatrix() {
            IntMatrix empty = IntMatrix.empty();
            long count = empty.rowPoints().count();
            assertEquals(0, count);
        }

        @Test
        public void testRowPoints_WithRange_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowPoints(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.rowPoints(0, 3));
        }
    }

    @Nested
    class ColumnPointsTests {

        @Test
        public void testColumnPoints() {
            IntMatrix matrix = createTestMatrix2x3();
            List<List<Sheet.Point>> columnCount = matrix.columnPoints().map(stream -> stream.collect(Collectors.toList())).collect(Collectors.toList());

            Assertions.assertEquals(3, columnCount.size());
            Assertions.assertEquals(2, columnCount.get(0).size());
            Assertions.assertEquals(Sheet.Point.of(0, 0), columnCount.get(0).get(0));
            Assertions.assertEquals(Sheet.Point.of(1, 2), columnCount.get(2).get(1));
        }

        @Test
        public void testColumnPointsRange() {
            IntMatrix matrix = createTestMatrix2x3();
            List<List<Sheet.Point>> columnCount = matrix.columnPoints(1, 3).map(stream -> stream.collect(Collectors.toList())).collect(Collectors.toList());

            Assertions.assertEquals(2, columnCount.size());
            Assertions.assertEquals(2, columnCount.get(0).size());
            Assertions.assertEquals(Sheet.Point.of(0, 1), columnCount.get(0).get(0));
        }

        @Test
        public void testColumnPoints_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<List<Point>> colPoints = m.columnPoints().map(s -> s.toList()).toList();

            assertEquals(2, colPoints.size());
            assertEquals(2, colPoints.get(0).size());
            assertEquals(Point.of(0, 0), colPoints.get(0).get(0));
            assertEquals(Point.of(1, 0), colPoints.get(0).get(1));
        }

        @Test
        public void testColumnPoints_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<List<Point>> colPoints = m.columnPoints(1, 3).map(s -> s.toList()).toList();

            assertEquals(2, colPoints.size());
            assertEquals(Point.of(0, 1), colPoints.get(0).get(0));
            assertEquals(Point.of(0, 2), colPoints.get(1).get(0));
        }

        @Test
        public void testColumnPoints_Shape2By2_GroupedColumnCoordinates() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Stream<Stream<Point>> columnCount = m.columnPoints();
            List<List<Point>> list = columnCount.map(Stream::toList).toList();

            assertEquals(2, list.size());
            assertEquals(2, list.get(0).size());
            assertEquals(0, list.get(0).get(0).columnIndex());
            assertEquals(1, list.get(1).get(0).columnIndex());
        }

        @Test
        public void testColumnPoints_ColumnRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            Stream<Stream<Point>> columnCount = m.columnPoints(1, 3);
            List<List<Point>> list = columnCount.map(Stream::toList).toList();

            assertEquals(2, list.size());
            assertEquals(1, list.get(0).get(0).columnIndex());
        }

        @Test
        public void testColumnPoints_AllColumns() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<List<Point>> columnCount = matrix.columnPoints().map(Stream::toList).toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).size());
            assertEquals(Point.of(0, 0), columnCount.get(0).get(0));
            assertEquals(Point.of(1, 0), columnCount.get(0).get(1));
        }

        @Test
        public void testColumnPoints_ColumnRange_Shape2By3() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<List<Point>> columnCount = matrix.columnPoints(0, 2).map(Stream::toList).toList();
            assertEquals(2, columnCount.size());
            assertEquals(2, columnCount.get(0).size());
        }

        @Test
        public void testColumnPoints_EmptyMatrix() {
            IntMatrix empty = IntMatrix.empty();
            long count = empty.columnPoints().count();
            assertEquals(0, count);
        }

        @Test
        public void testColumnPoints_WithRange_OutOfBounds() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnPoints(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> m.columnPoints(0, 3));
        }
    }

    @Nested
    class MainDiagonalStreamTests {

        @Test
        public void testMainDiagonalStream() {
            IntMatrix matrix = createTestMatrix();
            List<Integer> diagonal = matrix.mainDiagonalStream().boxed().collect(Collectors.toList());

            Assertions.assertEquals(3, diagonal.size());
            Assertions.assertEquals(1, diagonal.get(0));
            Assertions.assertEquals(5, diagonal.get(1));
            Assertions.assertEquals(9, diagonal.get(2));
        }

        @Test
        public void testMainDiagonalStream_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diagonal = m.mainDiagonalStream().toArray();

            assertEquals(3, diagonal.length);
            assertEquals(1, diagonal[0]);
            assertEquals(5, diagonal[1]);
            assertEquals(9, diagonal[2]);
        }
    }

    @Nested
    class AntiDiagonalStreamTests {

        @Test
        public void testAntiDiagonalStream() {
            IntMatrix matrix = createTestMatrix();
            List<Integer> diagonal = matrix.antiDiagonalStream().boxed().collect(Collectors.toList());

            Assertions.assertEquals(3, diagonal.size());
            Assertions.assertEquals(3, diagonal.get(0));
            Assertions.assertEquals(5, diagonal.get(1));
            Assertions.assertEquals(7, diagonal.get(2));
        }

        @Test
        public void testAntiDiagonalStream_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] diagonal = m.antiDiagonalStream().toArray();

            assertEquals(3, diagonal.length);
            assertEquals(3, diagonal[0]);
            assertEquals(5, diagonal[1]);
            assertEquals(7, diagonal[2]);
        }
    }

    @Nested
    class RowMajorStreamTests {

        @Test
        public void testRowMajorStream() {
            IntMatrix matrix = createTestMatrix2x3();
            List<Integer> elements = matrix.rowMajorStream().boxed().collect(Collectors.toList());

            Assertions.assertEquals(6, elements.size());
            Assertions.assertEquals(1, elements.get(0));
            Assertions.assertEquals(6, elements.get(5));
        }

        @Test
        public void testRowMajorStreamRange() {
            IntMatrix matrix = createTestMatrix();
            List<Integer> rows = matrix.rowMajorStream(1, 3).boxed().collect(Collectors.toList());

            Assertions.assertEquals(6, rows.size());
            Assertions.assertEquals(4, rows.get(0));
            Assertions.assertEquals(9, rows.get(5));
        }

        @Test
        public void testRowMajorStream_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            int[] rows = m.rowMajorStream(1, 3).toArray();
            assertArrayEquals(new int[] { 4, 5, 6, 7, 8, 9 }, rows);
        }

        @Test
        public void testRowMajorStream_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[] elements = m.rowMajorStream().toArray();

            assertEquals(4, elements.length);
            assertEquals(1, elements[0]);
            assertEquals(2, elements[1]);
            assertEquals(3, elements[2]);
            assertEquals(4, elements[3]);
        }

        @Test
        public void testRowMajorStream_RowRange_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            int[] elements = m.rowMajorStream(1, 3).toArray();

            assertEquals(4, elements.length);
            assertEquals(3, elements[0]);
            assertEquals(4, elements[1]);
            assertEquals(5, elements[2]);
            assertEquals(6, elements[3]);
        }

        @Test
        public void testRowMajorStreamSingleRowRange() {
            IntMatrix matrix = createTestMatrix();
            List<Integer> row = matrix.rowMajorStream(1, 2).boxed().collect(Collectors.toList());

            Assertions.assertEquals(3, row.size());
            Assertions.assertEquals(4, row.get(0));
            Assertions.assertEquals(5, row.get(1));
            Assertions.assertEquals(6, row.get(2));
        }

        @Test
        public void testRowMajorStream_EmptyMatrix() {
            IntMatrix empty = IntMatrix.empty();
            long count = empty.rowMajorStream().count();
            assertEquals(0, count);
        }

        @Test
        public void testRowMajorStream_SingleRowRange_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] row = m.rowMajorStream(1, 2).toArray();

            assertEquals(3, row.length);
            assertEquals(4, row[0]);
            assertEquals(5, row[1]);
            assertEquals(6, row[2]);
        }
    }

    @Nested
    class ColumnMajorStreamTests {

        @Test
        public void testColumnMajorStream() {
            IntMatrix matrix = createTestMatrix2x3();
            List<Integer> elements = matrix.columnMajorStream().boxed().collect(Collectors.toList());

            Assertions.assertEquals(6, elements.size());
            Assertions.assertEquals(1, elements.get(0));
            Assertions.assertEquals(4, elements.get(1));
            Assertions.assertEquals(2, elements.get(2));
        }

        @Test
        public void testColumnMajorStreamRange() {
            IntMatrix matrix = createTestMatrix();
            List<Integer> columnCount = matrix.columnMajorStream(1, 3).boxed().collect(Collectors.toList());

            Assertions.assertEquals(6, columnCount.size());
            Assertions.assertEquals(2, columnCount.get(0));
            Assertions.assertEquals(5, columnCount.get(1));
        }

        @Test
        public void testColumnMajorStream_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] columnCount = m.columnMajorStream(1, 3).toArray();
            assertArrayEquals(new int[] { 2, 5, 3, 6 }, columnCount);
        }

        @Test
        public void testColumnMajorStream_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int[] elements = m.columnMajorStream().toArray();

            assertEquals(4, elements.length);
            assertEquals(1, elements[0]);
            assertEquals(3, elements[1]);
            assertEquals(2, elements[2]);
            assertEquals(4, elements[3]);
        }

        @Test
        public void testColumnMajorStream_ColumnRange_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] elements = m.columnMajorStream(1, 3).toArray();

            assertEquals(4, elements.length);
            assertEquals(2, elements[0]);
            assertEquals(5, elements[1]);
            assertEquals(3, elements[2]);
            assertEquals(6, elements[3]);
        }

        @Test
        public void testColumnMajorStreamSingleColumnRange() {
            IntMatrix matrix = createTestMatrix();
            List<Integer> col = matrix.columnMajorStream(1, 2).boxed().collect(Collectors.toList());

            Assertions.assertEquals(3, col.size());
            Assertions.assertEquals(2, col.get(0));
            Assertions.assertEquals(5, col.get(1));
            Assertions.assertEquals(8, col.get(2));
        }

        @Test
        public void testColumnMajorStream_EmptyMatrix() {
            IntMatrix empty = IntMatrix.empty();
            long count = empty.columnMajorStream().count();
            assertEquals(0, count);
        }

        @Test
        public void testColumnMajorStream_SingleColumnRange_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            int[] col = m.columnMajorStream(1, 2).toArray();

            assertEquals(2, col.length);
            assertEquals(2, col[0]);
            assertEquals(5, col[1]);
        }
    }

    @Nested
    class RowStreamsTests {

        @Test
        public void testRowStreams() {
            IntMatrix matrix = createTestMatrix2x3();
            List<List<Integer>> rows = matrix.rowStreams().map(stream -> stream.boxed().collect(Collectors.toList())).collect(Collectors.toList());

            Assertions.assertEquals(2, rows.size());
            Assertions.assertEquals(3, rows.get(0).size());
            Assertions.assertEquals(1, rows.get(0).get(0));
            Assertions.assertEquals(6, rows.get(1).get(2));
        }

        @Test
        public void testRowStreamsRange() {
            IntMatrix matrix = createTestMatrix();
            List<List<Integer>> rows = matrix.rowStreams(1, 3).map(stream -> stream.boxed().collect(Collectors.toList())).collect(Collectors.toList());

            Assertions.assertEquals(2, rows.size());
            Assertions.assertEquals(3, rows.get(0).size());
            Assertions.assertEquals(4, rows.get(0).get(0));
        }

        @Test
        public void testRowStreams_Shape2By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<int[]> rows = m.rowStreams().map(s -> s.toArray()).toList();

            assertEquals(2, rows.size());
            assertArrayEquals(new int[] { 1, 2, 3 }, rows.get(0));
            assertArrayEquals(new int[] { 4, 5, 6 }, rows.get(1));
        }

        @Test
        public void testRowStreams_WithRange() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
            List<int[]> rows = m.rowStreams(1, 3).map(s -> s.toArray()).toList();

            assertEquals(2, rows.size());
            assertArrayEquals(new int[] { 4, 5, 6 }, rows.get(0));
            assertArrayEquals(new int[] { 7, 8, 9 }, rows.get(1));
        }

        @Test
        public void testRowStreams_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<int[]> rows = m.rowStreams().map(s -> s.toArray()).toList();

            assertEquals(2, rows.size());
            assertArrayEquals(new int[] { 1, 2 }, rows.get(0));
            assertArrayEquals(new int[] { 3, 4 }, rows.get(1));
        }

        @Test
        public void testRowStreams_RowRange_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
            List<int[]> rows = m.rowStreams(1, 3).map(s -> s.toArray()).toList();

            assertEquals(2, rows.size());
            assertArrayEquals(new int[] { 3, 4 }, rows.get(0));
            assertArrayEquals(new int[] { 5, 6 }, rows.get(1));
        }

        // From rowCount Javadoc
        @Test
        public void testAbstractMatrixStreamRRowSums() {
            // AbstractMatrix.java: IntMatrix matrix = IntMatrix.wrap(new int[][] {{1, 2, 3}, {4, 5, 6}});
            // Row sums: 6 and 15
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<Integer> rowSums = new ArrayList<>();
            matrix.rowStreams().forEach(rowStream -> {
                int sum = rowStream.sum();
                rowSums.add(sum);
            });
            assertEquals(2, rowSums.size());
            assertEquals(6, rowSums.get(0));
            assertEquals(15, rowSums.get(1));
        }

        @Test
        public void testRowStreams_EmptyMatrix() {
            IntMatrix empty = IntMatrix.empty();
            long count = empty.rowStreams().count();
            assertEquals(0, count);
        }
    }

    @Nested
    class ColumnStreamsTests {

        @Test
        public void testColumnStreams() {
            IntMatrix matrix = createTestMatrix2x3();
            List<List<Integer>> columnCount = matrix.columnStreams().map(stream -> stream.boxed().collect(Collectors.toList())).collect(Collectors.toList());

            Assertions.assertEquals(3, columnCount.size());
            Assertions.assertEquals(2, columnCount.get(0).size());
            Assertions.assertEquals(1, columnCount.get(0).get(0));
            Assertions.assertEquals(6, columnCount.get(2).get(1));
        }

        @Test
        public void testColumnStreamsRange() {
            IntMatrix matrix = createTestMatrix();
            List<List<Integer>> columnCount = matrix.columnStreams(1, 3)
                    .map(stream -> stream.boxed().collect(Collectors.toList()))
                    .collect(Collectors.toList());

            Assertions.assertEquals(2, columnCount.size());
            Assertions.assertEquals(3, columnCount.get(0).size());
            Assertions.assertEquals(2, columnCount.get(0).get(0));
        }

        @Test
        public void testColumnStreams_Shape2By3_ArrayValues() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<int[]> columnCount = m.columnStreams().map(s -> s.toArray()).toList();

            assertEquals(3, columnCount.size());
            assertArrayEquals(new int[] { 1, 4 }, columnCount.get(0));
            assertArrayEquals(new int[] { 2, 5 }, columnCount.get(1));
            assertArrayEquals(new int[] { 3, 6 }, columnCount.get(2));
        }

        @Test
        public void testColumnStreams_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<int[]> columnCount = m.columnStreams().map(s -> s.toArray()).toList();

            assertEquals(2, columnCount.size());
            assertArrayEquals(new int[] { 1, 3 }, columnCount.get(0));
            assertArrayEquals(new int[] { 2, 4 }, columnCount.get(1));
        }

        @Test
        public void testColumnStreams_ColumnRange_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } });
            List<int[]> columnCount = m.columnStreams(1, 3).map(s -> s.toArray()).toList();

            assertEquals(2, columnCount.size());
            assertArrayEquals(new int[] { 2, 5 }, columnCount.get(0));
            assertArrayEquals(new int[] { 3, 6 }, columnCount.get(1));
        }

        @Test
        public void testAbstractMatrixStreamCColumnAverages() {
            // AbstractMatrix.java: DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] {{1.0, 2.0}, {3.0, 4.0}});
            // Column averages: 2.0 and 3.0
            DoubleMatrix matrix = DoubleMatrix.wrap(new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } });
            List<Double> colAvgs = new ArrayList<>();
            matrix.columnStreams().forEach(colStream -> {
                double avg = colStream.average().orElse(0);
                colAvgs.add(avg);
            });
            assertEquals(2, colAvgs.size());
            assertEquals(2.0, colAvgs.get(0), 0.001);
            assertEquals(3.0, colAvgs.get(1), 0.001);
        }

        @Test
        public void testColumnStreams_EmptyMatrix() {
            IntMatrix empty = IntMatrix.empty();
            long count = empty.columnStreams().count();
            assertEquals(0, count);
        }
    }

    @Nested
    class AcceptTests {

        @Test
        public void testAccept() throws Exception {
            IntMatrix matrix = createTestMatrix();
            List<Integer> values = new ArrayList<>();

            matrix.accept(m -> {
                values.add(m.get(0, 0));
                values.add(m.get(2, 2));
            });

            Assertions.assertEquals(2, values.size());
            Assertions.assertEquals(1, values.get(0));
            Assertions.assertEquals(9, values.get(1));
        }

        @Test
        public void testAccept_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            List<Integer> sums = new ArrayList<>();

            m.accept(matrix -> {
                int sum = 0;
                for (int i = 0; i < matrix.rowCount(); i++) {
                    for (int j = 0; j < matrix.columnCount(); j++) {
                        sum += matrix.get(i, j);
                    }
                }
                sums.add(sum);
            });

            assertEquals(1, sums.size());
            assertEquals(10, sums.get(0).intValue());
        }

        @Test
        public void testAccept_Shape2By2_ConsumerSumsAllElements() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            AtomicInteger sum = new AtomicInteger(0);
            m.accept(matrix -> {
                for (int i = 0; i < matrix.rowCount(); i++) {
                    for (int j = 0; j < matrix.columnCount(); j++) {
                        sum.addAndGet(matrix.get(i, j));
                    }
                }
            });
            assertEquals(10, sum.get());
        }

        @Test
        public void testAccept_ReceivesMatrixWithTwoRows() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            AtomicInteger rows = new AtomicInteger(0);
            matrix.accept(m -> rows.set(m.rowCount()));
            assertEquals(2, rows.get());
        }

        @Test
        public void testAcceptNullAction() {
            IntMatrix matrix = createTestMatrix();
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.accept((Throwables.Consumer<IntMatrix, RuntimeException>) null));
        }

        @Test
        public void testAccept_WithException() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            assertThrows(RuntimeException.class, () -> m.accept(matrix -> {
                throw new RuntimeException("Test exception");
            }));
        }
    }

    @Nested
    class ApplyTests {

        @Test
        public void testApply() throws Exception {
            IntMatrix matrix = createTestMatrix();

            String result = matrix.apply(m -> "Matrix " + m.rowCount() + "x" + m.columnCount());
            Assertions.assertEquals("Matrix 3x3", result);

            int sum = matrix.apply(m -> {
                int s = 0;
                for (int i = 0; i < m.rowCount(); i++) {
                    for (int j = 0; j < m.columnCount(); j++) {
                        s += m.get(i, j);
                    }
                }
                return s;
            });
            Assertions.assertEquals(45, sum);
        }

        @Test
        public void testApply_Shape2By2() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            Integer sum = m.apply(matrix -> {
                int total = 0;
                for (int i = 0; i < matrix.rowCount(); i++) {
                    for (int j = 0; j < matrix.columnCount(); j++) {
                        total += matrix.get(i, j);
                    }
                }
                return total;
            });

            assertEquals(10, sum.intValue());
        }

        @Test
        public void testApply_ReturnString() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String result = m.apply(matrix -> "Matrix is " + matrix.rowCount() + "x" + matrix.columnCount());
            assertEquals("Matrix is 2x2", result);
        }

        @Test
        public void testApply_Shape2By2_FunctionSumsAllElements() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int sum = m.apply(matrix -> {
                int total = 0;
                for (int i = 0; i < matrix.rowCount(); i++) {
                    for (int j = 0; j < matrix.columnCount(); j++) {
                        total += matrix.get(i, j);
                    }
                }
                return total;
            });
            assertEquals(10, sum);
        }

        @Test
        public void testApply_ReturnsMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            IntMatrix result = m.apply(matrix -> matrix.transpose());
            assertEquals(2, result.rowCount());
            assertEquals(2, result.columnCount());
            assertEquals(1, result.get(0, 0));
            assertEquals(3, result.get(0, 1));
        }

        @Test
        public void testApply_ComputesElementCount() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            int totalElements = matrix.apply(m -> m.rowCount() * m.columnCount());
            assertEquals(4, totalElements);
        }

        @Test
        public void testApply_ReturnString_Shape2By2() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String result = matrix.apply(m -> "Matrix " + m.rowCount() + "x" + m.columnCount());
            assertEquals("Matrix 2x2", result);
        }

        @Test
        public void testApplyNullAction() {
            IntMatrix matrix = createTestMatrix();
            Assertions.assertThrows(IllegalArgumentException.class, () -> matrix.apply((Throwables.Function<IntMatrix, String, RuntimeException>) null));
        }

        @Test
        public void testApply_WithException() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            assertThrows(RuntimeException.class, () -> m.apply(matrix -> {
                throw new RuntimeException("Test exception");
            }));
        }
    }

    @Nested
    class AppendToTests {

        @Test
        public void testAppendTo() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            StringBuilder sb = new StringBuilder();
            m.appendTo(sb);
            assertEquals(m.toMultilineString(), sb.toString());
            assertTrue(sb.toString().contains("1"));
            assertTrue(sb.toString().contains("4"));
        }

        @Test
        public void testAppendTo_MatchesPrintlnRenderingForEveryType() {
            final List<AbstractMatrix<?, ?, ?, ?, ?>> matrices = List.of(BooleanMatrix.wrap(new boolean[][] { { true, false } }),
                    ByteMatrix.wrap(new byte[][] { { Byte.MIN_VALUE, Byte.MAX_VALUE } }), CharMatrix.wrap(new char[][] { { 'a', '\n', '\'', (char) 0 } }),
                    ShortMatrix.wrap(new short[][] { { Short.MIN_VALUE } }), IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } }),
                    LongMatrix.wrap(new long[][] { { Long.MIN_VALUE, Long.MAX_VALUE } }), FloatMatrix.wrap(new float[][] { { Float.NaN, -0.0f, 0.1f, 1e10f } }),
                    DoubleMatrix.wrap(new double[][] { { Double.NaN, -0.0, 1e-10, Double.NEGATIVE_INFINITY } }),
                    Matrix.wrap(Object.class, new Object[][] { { null, "x", new int[] { 1, 2 } } }));

            for (final AbstractMatrix<?, ?, ?, ?, ?> m : matrices) {
                final StringBuilder sb = new StringBuilder();
                m.appendTo(sb);
                assertEquals(m.toMultilineString(), sb.toString(), m.getClass().getSimpleName());
            }

            assertEquals("['a', '\\n', '\\'']", CharMatrix.wrap(new char[][] { { 'a', '\n', '\'' } }).toMultilineString());
            assertEquals("[null, x]", Matrix.wrap(String.class, new String[][] { { null, "x" } }).toMultilineString());
        }

        @Test
        public void testRendering_PropagatesElementFailureWithoutWrappingOrDiscardingWrittenPrefix() {
            final IllegalStateException failure = new IllegalStateException("element rendering failed");
            final Object failingElement = new Object() {
                @Override
                public String toString() {
                    throw failure;
                }
            };
            final Matrix<Object> matrix = Matrix.wrap(Object.class, new Object[][] { { "first", failingElement } });
            final StringBuilder output = new StringBuilder();

            // appendTo writes incrementally; println renders first and propagates the same element failure.
            assertSame(failure, assertThrows(IllegalStateException.class, () -> matrix.appendTo(output)));
            assertEquals("[first, ", output.toString());
            assertSame(failure, assertThrows(IllegalStateException.class, matrix::toMultilineString));
            assertSame(failure, assertThrows(IllegalStateException.class, matrix::println));
        }

        @Test
        public void testAppendTo_EmptyMatrix() {
            StringBuilder sb = new StringBuilder();
            IntMatrix.empty().appendTo(sb);
            assertEquals("[]", sb.toString());
        }

        @Test
        public void testAppendTo_Null() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> m.appendTo((Appendable) null));
        }

        @Test
        public void testAppendTo_PropagatesIOException() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            Appendable throwing = new Appendable() {
                @Override
                public Appendable append(CharSequence csq) throws java.io.IOException {
                    throw new java.io.IOException("boom");
                }

                @Override
                public Appendable append(CharSequence csq, int start, int end) throws java.io.IOException {
                    throw new java.io.IOException("boom");
                }

                @Override
                public Appendable append(char c) throws java.io.IOException {
                    throw new java.io.IOException("boom");
                }
            };
            org.junit.jupiter.api.Assertions.assertThrows(java.io.UncheckedIOException.class, () -> m.appendTo(throwing));
        }
    }

    @Nested
    class AppendElementForOutputTests {

        @Test
        public void testElementOutputValidatesDestinationBeforeRenderingAndPreservesNullableValues() throws Exception {
            final Matrix<Object> matrix = Matrix.empty();
            final NullPointerException renderingFailure = new NullPointerException("element rendering failed");
            final Object value = new Object() {
                @Override
                public String toString() {
                    throw renderingFailure;
                }
            };

            final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> matrix.appendElementForOutput(null, value));
            assertTrue(failure.getMessage().contains(cs.output));
            assertSame(renderingFailure, assertThrows(NullPointerException.class, () -> matrix.appendElementForOutput(new StringBuilder(), value)));

            final StringBuilder output = new StringBuilder();
            matrix.appendElementForOutput(output, null);
            assertEquals("null", output.toString());
        }

        @Test
        public void testOutputImplementationNullPointerExceptionIsNotTranslated() {
            final NullPointerException failure = new NullPointerException("output implementation failed");
            final Appendable output = new Appendable() {
                @Override
                public Appendable append(final CharSequence value) {
                    throw failure;
                }

                @Override
                public Appendable append(final CharSequence value, final int start, final int end) {
                    throw failure;
                }

                @Override
                public Appendable append(final char value) {
                    throw failure;
                }
            };

            assertSame(failure, assertThrows(NullPointerException.class, () -> Matrix.empty().appendElementForOutput(output, null)));
            assertSame(failure, assertThrows(NullPointerException.class, () -> IntMatrix.empty().appendTo(output)));
        }
    }

    @Nested
    class PrintlnTests {

        @Test
        public void testPrintln() {
            IntMatrix matrix = createTestMatrix();
            assertFalse(matrix.isEmpty());
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> matrix.println());
        }
    }

    @Nested
    class ToMultilineStringTests {

        @Test
        public void testToMultilineString_IntMatrix() {
            IntMatrix m = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String output = m.toMultilineString();
            assertNotNull(output);
            assertTrue(output.contains("1"));
            assertTrue(output.contains("4"));
        }

        @Test
        public void testToMultilineString_DoubleMatrix() {
            DoubleMatrix m = DoubleMatrix.wrap(new double[][] { { 1.5, 2.5 }, { 3.5, 4.5 } });
            String output = m.toMultilineString();
            assertNotNull(output);
            assertTrue(output.contains("1.5") || output.contains("1"));
        }

        @Test
        public void testToMultilineString_ObjectMatrix() {
            Matrix<String> m = Matrix.wrap(new String[][] { { "Bob海😀洋", "B" }, { "C", "Bob海😀洋" } });
            String output = m.toMultilineString();
            assertNotNull(output);
            assertTrue(output.contains("😀"));
            assertTrue(output.contains("😀"));
        }

        // From columnCopy Javadoc
        @Test
        public void testToMultilineString_ReturnsString() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            String result = matrix.toMultilineString();
            assertNotNull(result);
            assertTrue(result.contains("1"));
            assertTrue(result.contains("2"));
        }

        @Test
        public void testToMultilineString_EmptyMatrix() {
            IntMatrix m = IntMatrix.empty();
            String output = m.toMultilineString();
            assertNotNull(output);
        }
    }

    @Nested
    class CheckSameShapeTests {

        @Test
        public void testCheckSameShape() {
            IntMatrix matrix1 = createTestMatrix();
            IntMatrix matrix2 = IntMatrix.wrap(new int[][] { { 10, 20, 30 }, { 40, 50, 60 }, { 70, 80, 90 } });
            IntMatrix matrix3 = createTestMatrix2x3();

            // This should not throw
            matrix1.checkSameShape(matrix2);

            // This should throw
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                matrix1.checkSameShape(matrix3);
            });
        }
    }

    @Nested
    class ContractsTests {

        @Test
        public void testIndexErrorMessagesInterpolateValues() {
            Matrix<Integer> matrix = Matrix.wrap(new Integer[][] { { 1, 2 }, { 3, 4 } });

            IndexOutOfBoundsException ex = assertThrows(IndexOutOfBoundsException.class, () -> matrix.updateRow(5, value -> value));

            assertFalse(ex.getMessage().contains("{}"));
            assertTrue(ex.getMessage().contains("5"));
            assertTrue(ex.getMessage().contains("2"));
        }

        @Test
        public void testMapValidatesTargetElementType() {
            Matrix<String> matrix = Matrix.wrap(new String[][] { { "x" } });

            assertThrows(IllegalArgumentException.class, () -> matrix.map(v -> v, null));
        }

        @Test
        public void testBackingArray_MutationAffectsMatrix() {
            int[][] data = { { 1, 2 }, { 3, 4 } };
            IntMatrix m = IntMatrix.wrap(data);
            data[0][0] = 99;
            assertEquals(99, m.get(0, 0));
        }

        @Test
        public void testFamilyConventions_PrimitiveConversionTable() throws Exception {
            final List<Class<?>> nonBoolean = List.of(ByteMatrix.class, CharMatrix.class, ShortMatrix.class, IntMatrix.class, LongMatrix.class,
                    FloatMatrix.class, DoubleMatrix.class);
            final List<Class<?>> provided = List.of(IntMatrix.class, LongMatrix.class, FloatMatrix.class, DoubleMatrix.class);
            final List<String> omitted = List.of("toByteMatrix", "toCharMatrix", "toShortMatrix", "toBooleanMatrix");

            for (final Class<?> source : nonBoolean) {
                for (final Class<?> target : provided) {
                    final String name = "to" + target.getSimpleName();

                    if (target == source) {
                        assertThrows(NoSuchMethodException.class, () -> source.getMethod(name), source.getSimpleName() + "." + name);
                    } else {
                        assertEquals(target, source.getMethod(name).getReturnType(), source.getSimpleName() + "." + name);
                    }
                }
            }

            for (final Class<?> source : List.of(BooleanMatrix.class, ByteMatrix.class, CharMatrix.class, ShortMatrix.class, IntMatrix.class, LongMatrix.class,
                    FloatMatrix.class, DoubleMatrix.class, Matrix.class)) {
                for (final java.lang.reflect.Method method : source.getMethods()) {
                    assertFalse(omitted.contains(method.getName()), source.getSimpleName() + "." + method.getName());

                    if (source == BooleanMatrix.class) {
                        assertFalse(method.getName().matches("to[A-Z][a-z]+Matrix"), "BooleanMatrix." + method.getName());
                    }
                }
            }

            // The provided conversions are plain Java casts, so narrowing ones may lose information.
            assertEquals(-1, LongMatrix.wrap(new long[][] { { Long.MAX_VALUE } }).toIntMatrix().get(0, 0));
            assertEquals(-2L, DoubleMatrix.wrap(new double[][] { { -2.9 } }).toLongMatrix().get(0, 0));

            // The documented route to an omitted target type.
            final ByteMatrix bytes = IntMatrix.wrap(new int[][] { { 1, 300 } }).boxed().mapToByte(Integer::byteValue);
            assertEquals(ByteMatrix.wrap(new byte[][] { { 1, 44 } }), bytes);
        }

        @Test
        public void testAdjacencyChecks_InvalidColumnIndexEdgeCase() {
            IntMatrix matrix = IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });

            assertThrows(IndexOutOfBoundsException.class, () -> matrix.valueAbove(0, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.valueBelow(1, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.valueLeft(0, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> matrix.valueRight(1, -1));
        }
    }
}
