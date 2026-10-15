package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Sheet.Point;
import com.landawn.abacus.util.Throwables;

/** Shared contracts intentionally exercised across matrix implementations. */
class MatrixNullContractTest extends MatrixTestSupport {

    @Nested
    class FunctionalArguments {

        private static final List<Class<?>> MATRIX_CLASSES = List.of(AbstractMatrix.class, BooleanMatrix.class, ByteMatrix.class, CharMatrix.class,
                ShortMatrix.class, IntMatrix.class, LongMatrix.class, FloatMatrix.class, DoubleMatrix.class, Matrix.class, Matrices.class);

        private static boolean isFunctionalInterface(final Class<?> type) {
            return type.isInterface() && type.getName().startsWith("com.landawn.abacus.util.Throwables$");
        }

        private static Object[] newArguments(final Method method, final int nullParameterIndex) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final Type[] genericParameterTypes = method.getGenericParameterTypes();
            final Object[] args = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                args[i] = i == nullParameterIndex ? null : newArgument(parameterTypes[i], genericParameterTypes[i]);
            }

            return args;
        }

        private static Object newArgument(final Class<?> type, final Type genericType) {
            if (isFunctionalInterface(type)) {
                return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> defaultValue(method.getReturnType()));
            } else if (type == boolean.class) {
                return false;
            } else if (type == byte.class) {
                return (byte) 0;
            } else if (type == char.class) {
                return (char) 0;
            } else if (type == short.class) {
                return (short) 0;
            } else if (type == int.class) {
                return 0;
            } else if (type == long.class) {
                return 0L;
            } else if (type == float.class) {
                return 0F;
            } else if (type == double.class) {
                return 0D;
            } else if (type == Class.class) {
                return Object.class;
            } else if (type == ParallelMode.class) {
                return ParallelMode.AUTO;
            } else if (Collection.class.isAssignableFrom(type)) {
                final Class<?> elementType = collectionElementType(genericType);
                return List.of(newMatrix(elementType), newMatrix(elementType));
            } else if (AbstractMatrix.class.isAssignableFrom(type)) {
                return newMatrix(type);
            }

            return null;
        }

        private static Class<?> collectionElementType(final Type genericType) {
            if (genericType instanceof final ParameterizedType parameterizedType) {
                final String typeName = parameterizedType.getActualTypeArguments()[0].getTypeName();

                for (final Class<?> matrixClass : MATRIX_CLASSES) {
                    if (typeName.contains(matrixClass.getName())) {
                        return matrixClass;
                    }
                }
            }

            return Matrix.class;
        }

        private static Object newMatrix(final Class<?> type) {
            if (type == BooleanMatrix.class) {
                return BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            } else if (type == ByteMatrix.class) {
                return ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            } else if (type == CharMatrix.class) {
                return CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            } else if (type == ShortMatrix.class) {
                return ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            } else if (type == LongMatrix.class) {
                return LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            } else if (type == FloatMatrix.class) {
                return FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
            } else if (type == DoubleMatrix.class) {
                return DoubleMatrix.wrap(new double[][] { { 1, 2 }, { 3, 4 } });
            } else if (type == Matrix.class) {
                return Matrix.wrap(new Object[][] { { 1, 2 }, { 3, 4 } });
            }

            return IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
        }

        private static Object defaultValue(final Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            } else if (type == boolean.class) {
                return false;
            } else if (type == byte.class) {
                return (byte) 0;
            } else if (type == char.class) {
                return (char) 0;
            } else if (type == short.class) {
                return (short) 0;
            } else if (type == int.class) {
                return 0;
            } else if (type == long.class) {
                return 0L;
            } else if (type == float.class) {
                return 0F;
            } else if (type == double.class) {
                return 0D;
            }

            return null;
        }

        @Test
        public void testEveryPublicMethodRejectsNullFunctionalInterfaceArguments() {
            int validatedParameterCount = 0;

            for (final Class<?> matrixClass : MATRIX_CLASSES) {
                for (final Method method : matrixClass.getDeclaredMethods()) {
                    if (!Modifier.isPublic(method.getModifiers()) || Modifier.isAbstract(method.getModifiers()) || method.isSynthetic()) {
                        continue;
                    }

                    final Class<?>[] parameterTypes = method.getParameterTypes();

                    for (int functionalParameterIndex = 0; functionalParameterIndex < parameterTypes.length; functionalParameterIndex++) {
                        if (!isFunctionalInterface(parameterTypes[functionalParameterIndex])) {
                            continue;
                        }

                        final Object receiver = Modifier.isStatic(method.getModifiers()) ? null : newMatrix(matrixClass);
                        final Object[] args = newArguments(method, functionalParameterIndex);
                        final String invocation = method.toGenericString() + " with parameter " + functionalParameterIndex + " set to null";
                        final InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> method.invoke(receiver, args),
                                invocation);

                        assertInstanceOf(IllegalArgumentException.class, exception.getCause(), invocation);
                        validatedParameterCount++;
                    }
                }
            }

            // Tripwire: bump this whenever a public method gains a functional-interface parameter, so that a
            // silently non-matching reflection filter cannot make this test vacuous.
            assertEquals(215, validatedParameterCount);
        }
    }

    @Nested
    class ReferenceArguments {

        private static final List<Class<?>> CLASSES = List.of(AbstractMatrix.class, BooleanMatrix.class, ByteMatrix.class, CharMatrix.class, ShortMatrix.class,
                IntMatrix.class, LongMatrix.class, FloatMatrix.class, DoubleMatrix.class, Matrix.class, Matrices.class);

        private static boolean nullIsAccepted(final Class<?> c, final Executable e, final int index) {
            final String name = e.getName();
            final Class<?> type = e.getParameterTypes()[index];

            if (name.equals("equals")) {
                return true; // equals(null) returns false
            } else if (name.equals("ofDiagonals")) {
                return type.isArray(); // one of the two diagonals may be null (only one is nulled at a time here)
            } else if (c == Matrices.class && name.equals("isSameShape") && Collection.class.isAssignableFrom(type)) {
                return true; // null collection is trivially same-shape
            } else if (c == Matrix.class && type == Object.class) {
                return true; // Matrix<T> element values are nullable (set, fill, replaceIf, resize, pad)
            }

            return false;
        }

        private static List<Executable> publicExecutables(final Class<?> c) {
            final List<Executable> result = new ArrayList<>();

            for (final Method m : c.getDeclaredMethods()) {
                if (Modifier.isPublic(m.getModifiers()) && !Modifier.isAbstract(m.getModifiers()) && !m.isSynthetic()) {
                    result.add(m);
                }
            }

            for (final Constructor<?> k : c.getDeclaredConstructors()) {
                if (Modifier.isPublic(k.getModifiers())) {
                    result.add(k);
                }
            }

            return result;
        }

        private static Throwable invoke(final Class<?> c, final Executable e, final Object[] args) {
            try {
                if (e instanceof final Method m) {
                    m.invoke(Modifier.isStatic(m.getModifiers()) ? null : newMatrix(c), args);
                } else {
                    ((Constructor<?>) e).newInstance(args);
                }

                return null;
            } catch (final InvocationTargetException ex) {
                return ex.getCause();
            } catch (final ReflectiveOperationException ex) {
                throw new AssertionError(ex);
            }
        }

        private static Object[] arguments(final Executable e, final int target, final boolean nullElement) {
            final Class<?>[] pts = e.getParameterTypes();
            final Type[] gts = e.getGenericParameterTypes();
            final Object[] args = new Object[pts.length];

            for (int i = 0; i < pts.length; i++) {
                if (i == target && !nullElement) {
                    continue;
                }

                args[i] = argument(pts[i], gts[i]);

                if (i == target) {
                    if (args[i] instanceof final Object[] array && array.length > 0) {
                        array[array.length - 1] = null;
                    } else if (args[i] instanceof final List<?> list && !list.isEmpty()) {
                        final List<Object> copy = new ArrayList<>(list);
                        copy.set(copy.size() - 1, null);
                        args[i] = copy;
                    } else {
                        return null;
                    }
                }
            }

            return args;
        }

        private static Object argument(final Class<?> t, final Type g) {
            if (t.isPrimitive()) {
                return primitive(t);
            } else if (t == Class.class) {
                return Object.class;
            } else if (t == ParallelMode.class) {
                return ParallelMode.AUTO;
            } else if (t == Point.class) {
                return Point.of(0, 0);
            } else if (t == RandomGenerator.class) {
                return new Random(1);
            } else if (t == Appendable.class) {
                return new StringBuilder();
            } else if (t == Object.class) {
                return 1;
            } else if (t == String.class) {
                return "s";
            } else if (t.isInterface() && (t.getName().startsWith("com.landawn.abacus.util.Throwables$") || t.getName().startsWith("java.util.function"))) {
                return Proxy.newProxyInstance(t.getClassLoader(), new Class<?>[] { t }, (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.getName().equals("equals") ? proxy == args[0] : method.getName().equals("hashCode") ? 1 : "proxy";
                    }

                    return method.getReturnType().isPrimitive() ? primitive(method.getReturnType()) : null;
                });
            } else if (Collection.class.isAssignableFrom(t)) {
                final Class<?> element = collectionElementType(g);
                return element == String.class ? new ArrayList<>(List.of("a", "b")) : new ArrayList<>(List.of(newMatrix(element), newMatrix(element)));
            } else if (AbstractMatrix.class.isAssignableFrom(t)) {
                return newMatrix(t);
            } else if (t.isArray()) {
                final Class<?> ct = t.getComponentType();
                final Object array = Array.newInstance(ct, 2);

                for (int i = 0; i < 2; i++) {
                    Array.set(array, i, ct.isArray() || ct.isPrimitive() ? argument(ct, ct)
                            : AbstractMatrix.class.isAssignableFrom(ct) ? newMatrix(ct) : ct == String.class ? "s" + i : Integer.valueOf(i));
                }

                return array;
            }

            return null;
        }

        private static Object primitive(final Class<?> t) {
            if (t == boolean.class) {
                return false;
            } else if (t == byte.class) {
                return (byte) 1;
            } else if (t == char.class) {
                return 'a';
            } else if (t == short.class) {
                return (short) 1;
            } else if (t == int.class) {
                return 1;
            } else if (t == long.class) {
                return 1L;
            } else if (t == float.class) {
                return 1F;
            } else if (t == double.class) {
                return 1D;
            }

            return null;
        }

        private static Class<?> collectionElementType(final Type g) {
            if (g instanceof final ParameterizedType p) {
                final String name = p.getActualTypeArguments()[0].getTypeName();

                if (name.contains("String")) {
                    return String.class;
                }

                for (final Class<?> c : CLASSES) {
                    if (name.startsWith(c.getName())) {
                        return c;
                    }
                }
            }

            return Matrix.class;
        }

        private static Object newMatrix(final Class<?> type) {
            if (type == BooleanMatrix.class) {
                return BooleanMatrix.wrap(new boolean[][] { { true, false }, { false, true } });
            } else if (type == ByteMatrix.class) {
                return ByteMatrix.wrap(new byte[][] { { 1, 2 }, { 3, 4 } });
            } else if (type == CharMatrix.class) {
                return CharMatrix.wrap(new char[][] { { 'a', 'b' }, { 'c', 'd' } });
            } else if (type == ShortMatrix.class) {
                return ShortMatrix.wrap(new short[][] { { 1, 2 }, { 3, 4 } });
            } else if (type == IntMatrix.class) {
                return IntMatrix.wrap(new int[][] { { 1, 2 }, { 3, 4 } });
            } else if (type == LongMatrix.class) {
                return LongMatrix.wrap(new long[][] { { 1, 2 }, { 3, 4 } });
            } else if (type == FloatMatrix.class) {
                return FloatMatrix.wrap(new float[][] { { 1, 2 }, { 3, 4 } });
            } else if (type == DoubleMatrix.class) {
                return DoubleMatrix.wrap(new double[][] { { 1, 2 }, { 3, 4 } });
            }

            return Matrix.wrap(Integer.class, new Integer[][] { { 1, null }, { 3, 4 } });
        }

        @Test
        public void testEveryNonNullableReferenceParameterRejectsNullWithIllegalArgumentException() {
            int rejected = 0;
            int accepted = 0;

            for (final Class<?> c : CLASSES) {
                for (final Executable e : publicExecutables(c)) {
                    final Class<?>[] pts = e.getParameterTypes();

                    for (int i = 0; i < pts.length; i++) {
                        if (pts[i].isPrimitive()) {
                            continue;
                        }

                        final String call = c.getSimpleName() + "." + e + " with parameter " + i + " = null";
                        final Throwable t = invoke(c, e, arguments(e, i, false));

                        if (nullIsAccepted(c, e, i)) {
                            if (t != null) {
                                fail(call + " is documented to accept null but threw " + t);
                            }

                            accepted++;
                        } else {
                            if (!(t instanceof IllegalArgumentException)) {
                                fail(call + " should throw IllegalArgumentException but got " + t);
                            }

                            rejected++;
                        }
                    }
                }
            }

            // Tripwire: update when public signatures change, so a silently non-matching filter cannot make this test vacuous.
            assertEquals(599, rejected);
            assertEquals(37, accepted);
        }

        @Test
        public void testNullElementsOfArrayOrCollectionArgumentsNeverCauseNullPointerException() {
            int checked = 0;

            for (final Class<?> c : CLASSES) {
                for (final Executable e : publicExecutables(c)) {
                    final Class<?>[] pts = e.getParameterTypes();

                    for (int i = 0; i < pts.length; i++) {
                        if (!(pts[i].isArray() && !pts[i].getComponentType().isPrimitive()) && !Collection.class.isAssignableFrom(pts[i])) {
                            continue;
                        }

                        final Object[] args = arguments(e, i, true);

                        if (args == null) {
                            continue;
                        }

                        final Throwable t = invoke(c, e, args);

                        if (t instanceof NullPointerException) {
                            fail(c.getSimpleName() + "." + e + " with a null element in parameter " + i + " threw " + t);
                        }

                        checked++;
                    }
                }
            }

            assertTrue(checked >= 90, "checked " + checked);
        }

        @Test
        public void testBoxedMapperReturningNullThrowsNullPointerExceptionInBothModes() {
            for (final ParallelMode mode : new ParallelMode[] { ParallelMode.FORCE_OFF, ParallelMode.FORCE_ON }) {
                Matrices.runWithParallelMode(mode, () -> {
                    assertThrows(NullPointerException.class, () -> new BooleanMatrix(new boolean[4][4]).updateAll((i, j) -> (Boolean) null));
                    assertThrows(NullPointerException.class, () -> new ByteMatrix(new byte[4][4]).updateAll((i, j) -> (Byte) null));
                    assertThrows(NullPointerException.class, () -> new CharMatrix(new char[4][4]).updateAll((i, j) -> (Character) null));
                    assertThrows(NullPointerException.class, () -> new ShortMatrix(new short[4][4]).updateAll((i, j) -> (Short) null));
                    assertThrows(NullPointerException.class, () -> new IntMatrix(new int[4][4]).updateAll((i, j) -> (Integer) null));
                    assertThrows(NullPointerException.class, () -> new LongMatrix(new long[4][4]).updateAll((i, j) -> (Long) null));
                    assertThrows(NullPointerException.class, () -> new FloatMatrix(new float[4][4]).updateAll((i, j) -> (Float) null));
                    assertThrows(NullPointerException.class, () -> new DoubleMatrix(new double[4][4]).updateAll((i, j) -> (Double) null));

                    final IntMatrix a = new IntMatrix(new int[4][4]);
                    assertThrows(NullPointerException.class, () -> Matrices.zipToLong(a, a, a, (x, y, z) -> (Long) null));
                    assertThrows(NullPointerException.class, () -> Matrices.zipToLong(List.of(a, a), xs -> (Long) null));
                    assertThrows(NullPointerException.class, () -> Matrices.zipToDouble(List.of(a, a), xs -> (Double) null));

                    final ByteMatrix b = new ByteMatrix(new byte[4][4]);
                    assertThrows(NullPointerException.class, () -> Matrices.zipToInt(b, b, b, (x, y, z) -> (Integer) null));
                });
            }
        }

        @Test
        public void testObjectValuedFunctionsStoreNullResultsAndNullElementsFlowThrough() {
            final Matrix<Integer> m = Matrix.wrap(Integer.class, new Integer[][] { { null, 2 }, { 3, null } });

            final Matrix<Integer> mapped = m.map(v -> null, Integer.class);
            assertNull(mapped.get(0, 1));
            assertNull(m.zipWith(m, (x, y) -> null).get(1, 0));
            assertEquals(List.of(4, 4), Matrices.mapIndices(2, 2, (i, j) -> null, false).map(v -> 4).limit(2).toList());
            assertTrue(m.toMultilineString().contains("null"));

            // a caller function that unboxes a null element fails in the caller's code
            assertThrows(NullPointerException.class, () -> m.mapToInt(Integer::intValue));

            // null or duplicate column names are rejected by the Dataset contract, not by an NPE
            assertThrows(IllegalArgumentException.class, () -> m.toDataset(Arrays.asList("A", null)));
            assertThrows(IllegalArgumentException.class, () -> m.toTransposedDataset(Arrays.asList("A", null)));
        }
    }

    @Nested
    class NumericValues {

        @Test
        public void testResultFactoriesRejectNullArraysConsistentlyWithPublicFactories() {
            assertThrows(IllegalArgumentException.class, () -> IntMatrix.wrapResult(null, 0));
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.wrapResult(null, 0));
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.wrapResult(null, 0));
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.wrapResult(null, 0));

            assertSame(IntMatrix.empty(), IntMatrix.wrapResult(new int[0][], 0));
            assertSame(LongMatrix.empty(), LongMatrix.wrapResult(new long[0][], 0));
            assertSame(DoubleMatrix.empty(), DoubleMatrix.wrapResult(new double[0][], 0));
            assertSame(FloatMatrix.empty(), FloatMatrix.wrapResult(new float[0][], 0));
            assertEquals(3, IntMatrix.wrapResult(new int[0][], 3).columnCount());
            assertEquals(3, LongMatrix.wrapResult(new long[0][], 3).columnCount());
            assertEquals(3, DoubleMatrix.wrapResult(new double[0][], 3).columnCount());
            assertEquals(3, FloatMatrix.wrapResult(new float[0][], 3).columnCount());
        }

        @Test
        public void testNullCallbackResultsStillFailOnlyWhenUnboxed() {
            assertThrows(NullPointerException.class, () -> IntMatrix.wrap(new int[] { 1 }).updateAll((i, j) -> (Integer) null));
            assertThrows(NullPointerException.class, () -> LongMatrix.wrap(new long[] { 1 }).updateAll((i, j) -> (Long) null));
            assertThrows(NullPointerException.class, () -> DoubleMatrix.wrap(new double[] { 1 }).updateAll((i, j) -> (Double) null));
            assertThrows(NullPointerException.class, () -> FloatMatrix.wrap(new float[] { 1 }).updateAll((i, j) -> (Float) null));
            assertDoesNotThrow(() -> IntMatrix.empty().updateAll((i, j) -> (Integer) null));
            assertDoesNotThrow(() -> LongMatrix.empty().updateAll((i, j) -> (Long) null));
            assertDoesNotThrow(() -> DoubleMatrix.empty().updateAll((i, j) -> (Double) null));
            assertDoesNotThrow(() -> FloatMatrix.empty().updateAll((i, j) -> (Float) null));

            assertThrows(IllegalArgumentException.class, () -> IntMatrix.empty().updateAll((Throwables.IntBiFunction<Integer, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> LongMatrix.empty().updateAll((Throwables.IntBiFunction<Long, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> DoubleMatrix.empty().updateAll((Throwables.IntBiFunction<Double, RuntimeException>) null));
            assertThrows(IllegalArgumentException.class, () -> FloatMatrix.empty().updateAll((Throwables.IntBiFunction<Float, RuntimeException>) null));
        }

        @Test
        public void testObjectMappingAndUnboxingKeepTheirDifferentNullValueContracts() {
            assertNull(IntMatrix.wrap(new int[] { 1 }).mapToObj(value -> null, Object.class).get(0, 0));
            assertNull(LongMatrix.wrap(new long[] { 1 }).mapToObj(value -> null, Object.class).get(0, 0));
            assertNull(DoubleMatrix.wrap(new double[] { 1 }).mapToObj(value -> null, Object.class).get(0, 0));
            assertNull(FloatMatrix.wrap(new float[] { 1 }).mapToObj(value -> null, Object.class).get(0, 0));
            assertEquals(0, IntMatrix.unbox(Matrix.wrap(new Integer[][] { { null } })).get(0, 0));
            assertEquals(0L, LongMatrix.unbox(Matrix.wrap(new Long[][] { { null } })).get(0, 0));
            assertEquals(0d, DoubleMatrix.unbox(Matrix.wrap(new Double[][] { { null } })).get(0, 0));
            assertEquals(0f, FloatMatrix.unbox(Matrix.wrap(new Float[][] { { null } })).get(0, 0));
        }

        @Test
        public void testOptionalDiagonalsAndSkippedSourceRowsStillAcceptNull() {
            final IntMatrix ints = IntMatrix.ofDiagonals(null, new int[] { 7 });
            final LongMatrix longs = LongMatrix.ofDiagonals(null, new long[] { 7 });
            final DoubleMatrix doubles = DoubleMatrix.ofDiagonals(new double[] { 7 }, null);
            final FloatMatrix floats = FloatMatrix.ofDiagonals(new float[] { 7 }, null);
            ints.copyFrom(new int[][] { null });
            longs.copyFrom(new long[][] { null });
            doubles.copyFrom(new double[][] { null });
            floats.copyFrom(new float[][] { null });
            assertEquals(7, ints.get(0, 0));
            assertEquals(7L, longs.get(0, 0));
            assertEquals(7d, doubles.get(0, 0));
            assertEquals(7f, floats.get(0, 0));
        }

        @Test
        public void testNullableObjectAndRowHooksKeepTheirExistingResults() {
            assertFalse(IntMatrix.empty().equals(null));
            assertFalse(LongMatrix.empty().equals(null));
            assertFalse(DoubleMatrix.empty().equals(null));
            assertFalse(FloatMatrix.empty().equals(null));
            assertEquals(0, IntMatrix.empty().length(null));
            assertEquals(0, LongMatrix.empty().length(null));
            assertEquals(0, DoubleMatrix.empty().length(null));
            assertEquals(0, FloatMatrix.empty().length(null));
        }
    }

    @Nested
    class PrimitiveValues {

        @Test
        public void testResultFactoriesRejectNullArraysBeforeInspectingTheirShape() {
            assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.wrapResult(null, 0));
            assertThrows(IllegalArgumentException.class, () -> ByteMatrix.wrapResult(null, 0));
            assertThrows(IllegalArgumentException.class, () -> CharMatrix.wrapResult(null, 0));
            assertThrows(IllegalArgumentException.class, () -> ShortMatrix.wrapResult(null, 0));

            final String nullArrayMessage = assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.wrapResult(null, 0)).getMessage();
            assertEquals(nullArrayMessage, assertThrows(IllegalArgumentException.class, () -> BooleanMatrix.wrapResult(null, -1)).getMessage());
            assertEquals(nullArrayMessage, assertThrows(IllegalArgumentException.class, () -> ByteMatrix.wrapResult(null, -1)).getMessage());
            assertEquals(nullArrayMessage, assertThrows(IllegalArgumentException.class, () -> CharMatrix.wrapResult(null, -1)).getMessage());
            assertEquals(nullArrayMessage, assertThrows(IllegalArgumentException.class, () -> ShortMatrix.wrapResult(null, -1)).getMessage());
        }

        @Test
        public void testNullCoordinateMapperResultsKeepTheirUnboxingFailure() {
            assertThrows(NullPointerException.class, () -> BooleanMatrix.wrap(new boolean[] { true }).updateAll((row, column) -> null));
            assertThrows(NullPointerException.class, () -> ByteMatrix.wrap(new byte[] { 1 }).updateAll((row, column) -> null));
            assertThrows(NullPointerException.class, () -> CharMatrix.wrap(new char[] { 'a' }).updateAll((row, column) -> null));
            assertThrows(NullPointerException.class, () -> ShortMatrix.wrap(new short[] { 1 }).updateAll((row, column) -> null));
        }

        @Test
        public void testObjectMappingCanStillProduceNullElements() {
            assertNull(BooleanMatrix.wrap(new boolean[] { true }).mapToObj(value -> null, String.class).get(0, 0));
            assertNull(ByteMatrix.wrap(new byte[] { 1 }).mapToObj(value -> null, String.class).get(0, 0));
            assertNull(CharMatrix.wrap(new char[] { 'a' }).mapToObj(value -> null, String.class).get(0, 0));
            assertNull(ShortMatrix.wrap(new short[] { 1 }).mapToObj(value -> null, String.class).get(0, 0));
        }

        @Test
        public void testDiagonalFactoriesStillAllowOneMissingDiagonal() {
            assertTrue(BooleanMatrix.ofDiagonals(null, new boolean[] { true }).get(0, 0));
            assertTrue(BooleanMatrix.ofDiagonals(new boolean[] { true }, null).get(0, 0));
            assertEquals((byte) 1, ByteMatrix.ofDiagonals(null, new byte[] { 1 }).get(0, 0));
            assertEquals((byte) 1, ByteMatrix.ofDiagonals(new byte[] { 1 }, null).get(0, 0));
            assertEquals('a', CharMatrix.ofDiagonals(null, new char[] { 'a' }).get(0, 0));
            assertEquals('a', CharMatrix.ofDiagonals(new char[] { 'a' }, null).get(0, 0));
            assertEquals((short) 1, ShortMatrix.ofDiagonals(null, new short[] { 1 }).get(0, 0));
            assertEquals((short) 1, ShortMatrix.ofDiagonals(new short[] { 1 }, null).get(0, 0));
        }

        @Test
        public void testUnboxingStillMapsNullElementsToPrimitiveDefaults() {
            assertFalse(BooleanMatrix.unbox(new Matrix<>(new Boolean[][] { { null } })).get(0, 0));
            assertEquals((byte) 0, ByteMatrix.unbox(new Matrix<>(new Byte[][] { { null } })).get(0, 0));
            assertEquals('\0', CharMatrix.unbox(new Matrix<>(new Character[][] { { null } })).get(0, 0));
            assertEquals((short) 0, ShortMatrix.unbox(new Matrix<>(new Short[][] { { null } })).get(0, 0));
        }

        @Test
        public void testCopyingStillSkipsNullRows() {
            final BooleanMatrix booleans = BooleanMatrix.wrap(new boolean[] { true });
            final ByteMatrix bytes = ByteMatrix.wrap(new byte[] { 1 });
            final CharMatrix chars = CharMatrix.wrap(new char[] { 'a' });
            final ShortMatrix shorts = ShortMatrix.wrap(new short[] { 1 });
            booleans.copyFrom(new boolean[][] { null });
            bytes.copyFrom(new byte[][] { null });
            chars.copyFrom(new char[][] { null });
            shorts.copyFrom(new short[][] { null });
            assertTrue(booleans.get(0, 0));
            assertEquals((byte) 1, bytes.get(0, 0));
            assertEquals('a', chars.get(0, 0));
            assertEquals((short) 1, shorts.get(0, 0));
        }
    }
}
