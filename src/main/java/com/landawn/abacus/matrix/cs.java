/*
 * Copyright (c) 2026, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.matrix;

/**
 * Defines the canonical parameter names used by this package when reporting argument-validation
 * failures. Keeping these values centralized makes equivalent validation errors use the same name.
 */
final class cs { // NOSONAR
    /** Parameter name for an action callback. */
    static final String action = com.landawn.abacus.util.cs.action;

    /** Parameter name for a collection. */
    static final String coll = com.landawn.abacus.util.cs.coll;

    /** Parameter name for a mapping callback. */
    static final String mapper = com.landawn.abacus.util.cs.mapper;

    /** Parameter name for an operator callback. */
    static final String operator = "operator";

    /** Parameter name for another operand. */
    static final String other = com.landawn.abacus.util.cs.other;

    /** Parameter name for a parallelization policy. */
    static final String parallelMode = "parallelMode";

    /** Parameter name for a predicate callback. */
    static final String predicate = com.landawn.abacus.util.cs.predicate;

    /** Parameter name for a requested result element type. */
    static final String targetElementType = "targetElementType";

    /** Parameter name for the third matrix operand. */
    static final String third = "third";

    /** Parameter name for a matrix-combining callback. */
    static final String zipFunction = "zipFunction";

    /** Prevents instantiation of this constants class. */
    private cs() {
        // Utility class for constant string values.
    }
}
