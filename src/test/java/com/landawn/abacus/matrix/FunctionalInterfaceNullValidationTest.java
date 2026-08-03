package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class FunctionalInterfaceNullValidationTest extends TestBase {

    private static final List<Class<?>> MATRIX_CLASSES = List.of(AbstractMatrix.class, BooleanMatrix.class, ByteMatrix.class, CharMatrix.class,
            ShortMatrix.class, IntMatrix.class, LongMatrix.class, FloatMatrix.class, DoubleMatrix.class, Matrix.class, Matrices.class);

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
                    final InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> method.invoke(receiver, args), invocation);

                    assertInstanceOf(IllegalArgumentException.class, exception.getCause(), invocation);
                    validatedParameterCount++;
                }
            }
        }

        assertEquals(212, validatedParameterCount);
    }

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
}
