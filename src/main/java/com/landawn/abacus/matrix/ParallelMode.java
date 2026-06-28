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
 * Thread-local parallelization policy consulted by {@link Matrices} when deciding whether to execute
 * matrix operations in parallel.
 *
 * <p>The active value for the current thread is set through
 * {@link Matrices#setParallelMode(ParallelMode)}, retrieved through {@link Matrices#getParallelMode()},
 * and consulted by helpers such as {@link Matrices#shouldRunInParallel(AbstractMatrix, long)}. The three
 * available policies trade off explicit control against automatic heuristics:</p>
 * <ul>
 *   <li>{@link #FORCE_ON} requests parallel execution whenever the required runtime support is present,
 *       irrespective of matrix size.</li>
 *   <li>{@link #FORCE_OFF} disables parallel execution unconditionally.</li>
 *   <li>{@link #AUTO} combines runtime support with the built-in size heuristics so that small
 *       operations stay sequential while larger ones run in parallel.</li>
 * </ul>
 *
 * <p>Because the setting is stored in a {@link ThreadLocal}, changing it on one thread does not affect
 * other threads. {@link #AUTO} is the default value for every thread.</p>
 *
 * @see Matrices#setParallelMode(ParallelMode)
 * @see Matrices#getParallelMode()
 * @see Matrices#shouldRunInParallel(AbstractMatrix, long)
 * @see Matrices#runWithParallelMode(ParallelMode, com.landawn.abacus.util.Throwables.Runnable)
 */
public enum ParallelMode {
    /**
     * Requests parallel execution for the current thread whenever the runtime supports it, regardless
     * of matrix size.
     *
     * <p>This mode bypasses the size threshold used by {@link #AUTO}, so even small matrices may be
     * processed in parallel. It is most useful when the per-element work is expensive enough that the
     * fixed overhead of parallel dispatch is worthwhile even at small element counts.</p>
     */
    FORCE_ON,

    /**
     * Forces sequential execution for the current thread regardless of matrix size or runtime support.
     *
     * <p>Use this mode to guarantee deterministic, single-threaded behavior, for example when running
     * inside a context that already manages its own parallelism or when debugging.</p>
     */
    FORCE_OFF,

    /**
     * Uses parallel execution for the current thread only when the runtime supports it <i>and</i> the
     * per-operation element count meets the size threshold checked by
     * {@link Matrices#shouldRunInParallel(AbstractMatrix, long)}.
     *
     * <p>This is the default mode and is recommended for most workloads: small operations stay
     * sequential to avoid parallel-dispatch overhead, while larger operations automatically benefit
     * from parallel execution.</p>
     */
    AUTO
}
