package com.landawn.abacus.matrix;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;

/** Starts each test with the public default policy and restores the caller's thread-local state. */
abstract class MatrixTestSupport extends TestBase {

    private ParallelMode previousParallelMode;

    @BeforeEach
    final void preserveParallelMode() {
        previousParallelMode = Matrices.getParallelMode();
        Matrices.setParallelMode(ParallelMode.FORCE_OFF);
    }

    @AfterEach
    final void restoreParallelMode() {
        Matrices.setParallelMode(previousParallelMode);
    }
}
