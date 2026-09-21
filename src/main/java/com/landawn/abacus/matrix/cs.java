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
    /** Parameter name for the first matrix operand. */
    static final String a = "a";

    /** Parameter name for an action callback. */
    static final String action = "action";

    /** Parameter name for the anti-diagonal values. */
    static final String antiDiagonal = "antiDiagonal";

    /** Parameter name for the second matrix operand. */
    static final String b = "b";

    /** Parameter name for the third matrix operand. */
    static final String c = "c";

    /** Parameter name for a collection. */
    static final String coll = "coll";

    /** Parameter name for column values. */
    static final String column = "column";

    /** Parameter name for the number of columns. */
    static final String columnCount = "columnCount";

    /** Parameter name for dataset column names. */
    static final String columnNames = "columnNames";

    /** Parameter name for a work-item count. */
    static final String count = "count";

    /** Parameter name for a matrix element type. */
    static final String elementType = "elementType";

    /** Parameter name for the input element type of a zip operation. */
    static final String inputElementType = "inputElementType";

    /** Parameter name for a matrix whose parallelization policy is evaluated. */
    static final String m = "m";

    /** Parameter name for the main-diagonal values. */
    static final String mainDiagonal = "mainDiagonal";

    /** Parameter name for a mapping callback. */
    static final String mapper = "mapper";

    /** Parameter name for a collection of matrices. */
    static final String matrices = "matrices";

    /** Parameter name for the requested number of columns. */
    static final String newColumnCount = "newColumnCount";

    /** Parameter name for the requested number of rows. */
    static final String newRowCount = "newRowCount";

    /** Parameter name for an operator callback. */
    static final String operator = "operator";

    /** Parameter name for another operand. */
    static final String other = "other";

    /** Parameter name for an output destination. */
    static final String output = "output";

    /** Parameter name for padding below a matrix. */
    static final String padBottom = "padBottom";

    /** Parameter name for padding to the left of a matrix. */
    static final String padLeft = "padLeft";

    /** Parameter name for padding to the right of a matrix. */
    static final String padRight = "padRight";

    /** Parameter name for padding above a matrix. */
    static final String padTop = "padTop";

    /** Parameter name for a parallelization policy. */
    static final String parallelMode = "parallelMode";

    /** Parameter name for a matrix coordinate. */
    static final String point = "point";

    /** Parameter name for a predicate callback. */
    static final String predicate = "predicate";

    /** Parameter name for a random-number generator. */
    static final String randomGenerator = "randomGenerator";

    /** Parameter name for the number of columns in a matrix product. */
    static final String resultColumnCount = "resultColumnCount";

    /** Parameter name for row values. */
    static final String row = "row";

    /** Parameter name for the number of rows. */
    static final String rowCount = "rowCount";

    /** Parameter name for a source array. */
    static final String source = "source";

    /** Parameter name for a requested result element type. */
    static final String targetElementType = "targetElementType";

    /** Parameter name for the third matrix operand. */
    static final String third = "third";

    /** Parameter name for a matrix to unbox. */
    static final String x = "x";

    /** Parameter name for a matrix-combining callback. */
    static final String zipFunction = "zipFunction";

    /** Prevents instantiation of this constants class. */
    private cs() {
        // Utility class for constant string values.
    }
}
