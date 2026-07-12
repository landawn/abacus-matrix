/*
 * Copyright (C) 2016 HaiYang Li
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

/**
 * Array-backed, mutable matrices for Java primitive and reference values.
 *
 * <p>The package provides a common matrix API through {@link com.landawn.abacus.matrix.AbstractMatrix}
 * and specialized implementations for every primitive type, plus
 * {@link com.landawn.abacus.matrix.Matrix} for reference values. The matrix types support element and
 * row/column access, shape transformations, mapping, streaming, zipping, and copying. Numeric matrix
 * types also provide element-wise arithmetic and matrix multiplication. Cross-matrix operations and
 * shape utilities are available from {@link com.landawn.abacus.matrix.Matrices}.</p>
 *
 * <p>All matrix instances have a rectangular two-dimensional backing array. Constructors and
 * {@code of(...)} factories wrap a supplied array after validating its shape, so mutations made through
 * either the matrix or the original array are visible through the other. Use {@code copyOf(...)} or
 * {@code copy()} when the matrix must own independent storage. Methods whose names end in {@code View}
 * expose live storage, whereas methods whose names end in {@code Copy} return independent data.</p>
 *
 * <p>Matrix instances are mutable and are not thread-safe. Operations may execute sequentially or in
 * parallel according to the current thread's {@link com.landawn.abacus.matrix.ParallelMode}; callbacks
 * supplied to an operation that can run in parallel must therefore be thread-safe.</p>
 *
 * <p>For example:</p>
 * <pre>{@code
 * IntMatrix left = IntMatrix.of(new int[][] {
 *     { 1, 2, 3 },
 *     { 4, 5, 6 }
 * });
 * IntMatrix right = IntMatrix.of(new int[][] {
 *     { 7, 8 },
 *     { 9, 10 },
 *     { 11, 12 }
 * });
 * IntMatrix product = left.matrixMultiply(right);
 * }</pre>
 *
 * @see com.landawn.abacus.matrix.Matrix
 * @see com.landawn.abacus.matrix.IntMatrix
 * @see com.landawn.abacus.matrix.Matrices
 */
package com.landawn.abacus.matrix;
