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
 * Thread-local parallelization policy consulted by {@link Matrices} when automatic matrix operations
 * decide whether to execute in parallel.
 *
 * <p>The active value for the current thread is set through
 * {@link Matrices#setParallelMode(ParallelMode)}, retrieved through {@link Matrices#getParallelMode()},
 * and consulted by helpers such as {@link Matrices#shouldRunInParallel(AbstractMatrix, long)}. The three
 * available policies trade off explicit control against automatic heuristics:</p>
 * <ul>
 *   <li>{@link #FORCE_ON} requests parallel execution irrespective of operation size when the required
 *       runtime support is present and the operation's safety restrictions permit it.</li>
 *   <li>{@link #FORCE_OFF} keeps automatic operations that consult this policy sequential.</li>
 *   <li>{@link #AUTO} combines runtime support, operation-specific safety restrictions, and the
 *       operation's work-count threshold.</li>
 * </ul>
 *
 * <p>Explicit overloads with an {@code inParallel} argument are controlled by that argument and do not
 * consult this policy.</p>
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
     * Requests parallel execution for automatic operations on the current thread regardless of operation
     * size, provided the runtime supports it and the operation's safety restrictions permit it.
     *
     * <p>This mode bypasses only the work-count threshold used by {@link #AUTO}, so even small matrices may
     * be processed in parallel when otherwise eligible. It is most useful when the per-element work is
     * expensive enough that the fixed overhead of parallel dispatch is worthwhile at small element counts.</p>
     */
    FORCE_ON,

    /**
     * Selects sequential execution for automatic operations that consult the current thread's policy.
     *
     * <p>Use this mode to keep those operations on the calling thread and retain their documented sequential
     * visitation order, for example when running inside a context that already manages its own parallelism or
     * when debugging. Explicit overloads with an {@code inParallel} argument remain controlled by that argument.</p>
     */
    FORCE_OFF,

    /**
     * Uses parallel execution for automatic operations on the current thread only when the runtime supports
     * it, the operation's safety restrictions permit it, and the operation-specific work count meets the
     * threshold checked by {@link Matrices#shouldRunInParallel(AbstractMatrix, long)}.
     *
     * <p>This is the default mode and is recommended for most workloads: small operations stay sequential
     * to avoid parallel-dispatch overhead, while larger eligible operations can run in parallel.</p>
     */
    AUTO
}
