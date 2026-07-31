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
 * Mutable, rectangular, array-backed matrices for Java primitive and reference values.
 *
 * <h2>Matrix types</h2>
 *
 * <p>{@link com.landawn.abacus.matrix.Matrix} stores reference values. The primitive-specialized
 * implementations are {@link com.landawn.abacus.matrix.BooleanMatrix},
 * {@link com.landawn.abacus.matrix.ByteMatrix}, {@link com.landawn.abacus.matrix.CharMatrix},
 * {@link com.landawn.abacus.matrix.ShortMatrix}, {@link com.landawn.abacus.matrix.IntMatrix},
 * {@link com.landawn.abacus.matrix.LongMatrix}, {@link com.landawn.abacus.matrix.FloatMatrix}, and
 * {@link com.landawn.abacus.matrix.DoubleMatrix}. They share shape, traversal, transformation, and
 * rendering behavior through {@link com.landawn.abacus.matrix.AbstractMatrix}. Static operations that
 * combine matrices or control execution policy are provided by {@link com.landawn.abacus.matrix.Matrices}.</p>
 *
 * <p>All concrete types support element access, row and column access, copying, resizing, reshaping,
 * transposition, rotation, repetition, stacking, mapping, zipping, and row-major and column-major
 * traversal. Every primitive specialization supports boxing. The non-boolean primitive types also
 * support element-wise addition and subtraction, matrix multiplication, and numeric conversion.
 * {@link com.landawn.abacus.matrix.BooleanMatrix} provides boolean operations and reductions.</p>
 *
 * <h2>Shape and indexing</h2>
 *
 * <p>A matrix is backed by a rectangular two-dimensional array: every row is non-{@code null} and has
 * the same length. Rows and columns use zero-based indexes. Parameters named {@code fromRowIndex},
 * {@code toRowIndex}, {@code fromColumnIndex}, and {@code toColumnIndex} describe half-open ranges; the
 * {@code from} index is included and the {@code to} index is excluded.</p>
 *
 * <p>Java arrays can represent {@code N x 0} matrices but cannot retain a non-zero column count when
 * there are no rows. Consequently, {@code 0 x 0} and {@code N x 0} shapes are supported, while a
 * conceptual {@code 0 x N} shape for {@code N > 0} is rejected or, where documented for a
 * transformation, collapses to {@code 0 x 0}.</p>
 *
 * <h2>Storage ownership and mutation</h2>
 *
 * <p>Public constructors validate and then wrap the supplied array; they do not make a defensive copy.
 * {@link com.landawn.abacus.matrix.Matrix#of(Object[][])} does the same, as do the primitive
 * {@code of(...)} factories when the input has at least one row. A primitive {@code of(...)} factory
 * canonicalizes a zero-row input to its shared {@code 0 x 0} singleton, so the caller's empty outer-array
 * identity is not retained. Primitive {@code copyOf(...)} factories do the same for zero-row inputs and copy
 * every row of non-empty inputs. {@link com.landawn.abacus.matrix.Matrix#copyOf(Object[][])} always clones the
 * outer array and every row, including the outer array of a zero-row input. Use {@code copyOf(...)} to copy an
 * input array or {@link com.landawn.abacus.matrix.AbstractMatrix#copy()} to copy a matrix. For reference matrices,
 * these operations copy the array structure but not the referenced element objects.</p>
 *
 * <p>Rows are required to be rectangular but need not be identity-distinct. If the same row array
 * appears multiple times in wrapped storage, all of those logical rows remain aliases. Unary value
 * transformations process each distinct backing row once so an operation does not compound merely because
 * a reference is repeated. Coordinate traversal that writes a position-dependent value runs sequentially
 * when rows are aliased, so a later logical row deterministically overwrites an earlier one; {@code replaceIf}
 * writes the same value at every matching coordinate and may still run in parallel. {@code mutateFlattened}
 * copies its temporary array back in row-major order, so values from a later logical row win when aliased
 * rows conflict.</p>
 *
 * <p>{@link com.landawn.abacus.matrix.AbstractMatrix#unsafeBackingArray()} and methods whose names end
 * in {@code View}, such as {@link com.landawn.abacus.matrix.AbstractMatrix#rowView(int)}, expose live
 * storage. Methods whose names end in {@code Copy}, together with
 * {@link com.landawn.abacus.matrix.AbstractMatrix#flatten()}, return independent containers.</p>
 *
 * <p>On a matrix instance, methods named {@code set*}, {@code update*}, {@code fill}, {@code replaceIf},
 * or ending in {@code InPlace} modify the receiver. The specialized {@code mutateFlattened} operation lets
 * its action modify the matrix through a temporary flattened array. Shape transformations whose names do not
 * end in {@code InPlace}, arithmetic operations, {@code map}, {@code zipWith}, and {@code copy} leave the
 * receiver unchanged and return a separate matrix instead, except that a degenerate empty result may be the
 * shared empty instance of the returned matrix type. Matrix instances are mutable and are not thread-safe.</p>
 *
 * <h2>Traversal and parallel execution</h2>
 *
 * <p>Element streams are available in row-major and column-major order. Streams of individual rows or
 * columns and streams of coordinate points are also available. Sequential callback-based operations
 * visit elements in the order documented by the method. An operation may instead execute in parallel
 * according to the current thread's {@link com.landawn.abacus.matrix.ParallelMode}, runtime support,
 * and the amount of work involved. In that case, callback invocation order is unspecified and the
 * callback must be thread-safe.</p>
 *
 * <p>Use {@link com.landawn.abacus.matrix.Matrices#setParallelMode(ParallelMode)} to configure the
 * current thread, or
 * {@link com.landawn.abacus.matrix.Matrices#runWithParallelMode(ParallelMode, com.landawn.abacus.util.Throwables.Runnable)}
 * to apply a mode for the duration of an action and restore the previous mode afterward.</p>
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * int[][] source = {
 *     { 1, 2, 3 },
 *     { 4, 5, 6 }
 * };
 *
 * IntMatrix wrapped = IntMatrix.of(source);      // shares source
 * IntMatrix owned = IntMatrix.copyOf(source);    // owns its row arrays
 * source[0][0] = 10;
 *
 * wrapped.get(0, 0); // 10
 * owned.get(0, 0);   // 1
 *
 * IntMatrix right = IntMatrix.of(new int[][] {
 *     { 7, 8 },
 *     { 9, 10 },
 *     { 11, 12 }
 * });
 * IntMatrix product = owned.matrixMultiply(right);
 * }</pre>
 *
 * @see com.landawn.abacus.matrix.AbstractMatrix
 * @see com.landawn.abacus.matrix.Matrix
 * @see com.landawn.abacus.matrix.Matrices
 * @see com.landawn.abacus.matrix.ParallelMode
 */
package com.landawn.abacus.matrix;
