package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Sheet.Point;

/**
 * Pins the null contract of the public API: a {@code null} argument that a method does not accept is
 * rejected with {@link IllegalArgumentException}; {@link NullPointerException} is reserved for a
 * caller-supplied function whose {@code null} result must be unboxed (the inherited iterator
 * {@code toArray(A[])} contract is covered in {@code MatrixTest} / {@code BooleanMatrixTest}).
 */
class NullArgumentValidationTest extends TestBase {

    private static final List<Class<?>> CLASSES = List.of(AbstractMatrix.class, BooleanMatrix.class, ByteMatrix.class, CharMatrix.class, ShortMatrix.class,
            IntMatrix.class, LongMatrix.class, FloatMatrix.class, DoubleMatrix.class, Matrix.class, Matrices.class);

    @Test
    void everyNonNullableReferenceParameterRejectsNullWithIllegalArgumentException() {
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
    void nullElementsOfArrayOrCollectionArgumentsNeverCauseNullPointerException() {
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
    void boxedMapperReturningNullThrowsNullPointerExceptionInBothModes() {
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
    void objectValuedFunctionsStoreNullResultsAndNullElementsFlowThrough() {
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

    // ---------------------------------------------------------------------------------------------

    /** The parameters that deliberately accept {@code null} (documented null-as-value / null-as-empty behaviour). */
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
}
