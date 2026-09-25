package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;

class ObjectExceptionReviewTest extends TestBase {

    @Test
    void diagonalTypeIsFullyValidatedBeforeTheLaterArrayArgument() {
        assertEquals("Element type cannot be void.class",
                assertThrows(IllegalArgumentException.class, () -> Matrix.ofMainDiagonal(void.class, null)).getMessage());
        assertEquals("Element type cannot be void.class",
                assertThrows(IllegalArgumentException.class, () -> Matrix.ofAntiDiagonal(void.class, null)).getMessage());
    }

    @Test
    void objectZipValidatesTheResultTypeBeforeAllocatingItsMatrixSnapshot() {
        final Throwables.ByteNFunction<Void, RuntimeException> byteFunction = values -> null;
        final Throwables.IntNFunction<Void, RuntimeException> intFunction = values -> null;
        final Throwables.LongNFunction<Void, RuntimeException> longFunction = values -> null;
        final Throwables.DoubleNFunction<Void, RuntimeException> doubleFunction = values -> null;
        final Throwables.Function<Object[], Void, RuntimeException> objectFunction = values -> null;

        assertEquals("Element type cannot be void.class", assertThrows(IllegalArgumentException.class,
                () -> Matrices.zipToObj(rejectSnapshot(ByteMatrix.wrap(new byte[][] { { 1 } })), byteFunction, void.class)).getMessage());
        assertEquals("Element type cannot be void.class", assertThrows(IllegalArgumentException.class,
                () -> Matrices.zipToObj(rejectSnapshot(IntMatrix.wrap(new int[][] { { 1 } })), intFunction, void.class)).getMessage());
        assertEquals("Element type cannot be void.class", assertThrows(IllegalArgumentException.class,
                () -> Matrices.zipToObj(rejectSnapshot(LongMatrix.wrap(new long[][] { { 1 } })), longFunction, void.class)).getMessage());
        assertEquals("Element type cannot be void.class", assertThrows(IllegalArgumentException.class,
                () -> Matrices.zipToObj(rejectSnapshot(DoubleMatrix.wrap(new double[][] { { 1 } })), doubleFunction, void.class)).getMessage());
        assertEquals("Element type cannot be void.class", assertThrows(IllegalArgumentException.class,
                () -> Matrices.zip(rejectSnapshot(Matrix.wrap(Object.class, new Object[][] { { 1 } })), objectFunction, void.class)).getMessage());
    }

    private static <T> Collection<T> rejectSnapshot(final T matrix) {
        return new AbstractCollection<>() {
            @Override
            public Iterator<T> iterator() {
                return List.of(matrix).iterator();
            }

            @Override
            public int size() {
                return 1;
            }

            @Override
            public <A> A[] toArray(final A[] destination) {
                throw new AssertionError("Result type must be validated before allocating the matrix snapshot");
            }
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void collectionZipChecksInputTypeCompatibilityBeforeTheLaterTargetType() {
        final Matrix<Number> matrix = Matrix.copyOf(Number.class, new Number[][] { { 1 } });
        final Throwables.Function<Number[], String, RuntimeException> function = values -> "value";
        final Class<Number> incompatibleInputType = (Class) Integer.class;

        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Matrices.zip(List.of(matrix), function, incompatibleInputType, (Class<String>) null));
        assertTrue(failure.getMessage().contains("not assignable to input element type java.lang.Integer"), failure.getMessage());
    }

    @SuppressWarnings("deprecation")
    @Test
    void explicitInputTypeAvoidsTheLegacyCallbackArrayCastFailure() {
        final Matrix<Serializable> text = Matrix.wrap(new String[][] { { "text" } });
        final Matrix<Serializable> builder = Matrix.wrap(new StringBuilder[][] { { new StringBuilder("builder") } });
        final Throwables.Function<Serializable[], Integer, RuntimeException> function = values -> values.length;

        assertThrows(ClassCastException.class, () -> Matrices.zip(List.of(text, builder), function, Integer.class));
        assertEquals(2, Matrices.zip(List.of(text, builder), function, Serializable.class, Integer.class).get(0, 0));
    }

    @Test
    void datasetNameChecksPreserveConstructorDiagnostics() {
        final Matrix<String> matrix = Matrix.copyOf(String.class, new String[][] { { "a", "b" }, { "c", "d" } });
        assertEquals("Duplicated column names found in: [name, name]",
                assertThrows(IllegalArgumentException.class, () -> matrix.toDataset(List.of("name", "name"))).getMessage());
        assertEquals("Empty column name found in: [name, ]",
                assertThrows(IllegalArgumentException.class, () -> matrix.toTransposedDataset(List.of("name", ""))).getMessage());
    }
}
