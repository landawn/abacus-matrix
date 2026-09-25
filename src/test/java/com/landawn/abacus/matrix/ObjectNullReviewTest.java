package com.landawn.abacus.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class ObjectNullReviewTest extends TestBase {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void commonElementTypeRejectsNullInputsAndPreservesTheEmptyArrayFailure() {
        assertThrows(IllegalArgumentException.class, () -> Matrices.resolveCommonElementType(null));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Matrices.resolveCommonElementType(new Matrix[0]));

        final Matrix<String> matrix = Matrix.wrap(String.class, new String[][] { { null } });
        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> Matrices.resolveCommonElementType(new Matrix[] { matrix, null }));
        assertTrue(failure.getMessage().contains("matrices[1]"), failure.getMessage());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void commonElementTypeAcceptsMatricesContainingNullValues() {
        final Matrix<String> matrix = Matrix.wrap(String.class, new String[][] { { null } });
        assertEquals(String.class, Matrices.resolveCommonElementType(new Matrix[] { matrix, Matrix.empty(String.class) }));
        assertNull(matrix.get(0, 0));
    }

    @Test
    void optionalTypeTokensKeepTheirInferenceSemantics() {
        assertEquals(Object.class, Matrices.resolveCommonAssignableType(null, null));
        assertEquals(String.class, Matrices.resolveCommonAssignableType(String.class, null));
        assertEquals(String.class, Matrices.resolveCommonAssignableType(null, String.class));

        final Matrix<String> matrix = Matrix.newResult(new String[][] { { null } }, null, 1);
        assertEquals(String.class, matrix.elementType);
        assertNull(matrix.get(0, 0));
    }
}
