/*
 * Copyright (C) 2020 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.matrix;

/**
 * Thread-local parallelization policy consulted by {@link Matrices}.
 *
 * <p>The active value is set through {@link Matrices#setParallelMode(ParallelMode)} and consulted by
 * helpers such as {@link Matrices#isParallelizable(AbstractMatrix, long)}. {@link #FORCE_ON} requests
 * parallel execution whenever the required runtime support is present, {@link #FORCE_OFF} disables
 * it, and {@link #AUTO} combines runtime support with the built-in size heuristics.</p>
 *
 * @see Matrices#setParallelMode(ParallelMode)
 * @see Matrices#getParallelMode()
 */
public enum ParallelMode {
    /**
     * Request parallel execution for the current thread whenever the runtime supports it,
     * regardless of matrix size.
     */
    FORCE_ON,

    /**
     * Force sequential execution for the current thread regardless of matrix size or runtime support.
     */
    FORCE_OFF,

    /**
     * Use parallel execution for the current thread only when the runtime supports it
     * <i>and</i> the per-operation element count meets the size threshold checked by
     * {@link Matrices#isParallelizable(AbstractMatrix, long)}.
     */
    AUTO
}
