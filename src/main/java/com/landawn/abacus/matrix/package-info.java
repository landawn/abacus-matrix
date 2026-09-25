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
 * <p>All degenerate shapes are supported and preserved: {@code 0 x 0}, {@code N x 0} and {@code 0 x N}.
 * A Java array cannot itself encode a non-zero column count when it has no rows, so a matrix carries its
 * logical column count alongside the backing array. Every operation propagates it: an {@code N x 0} matrix
 * transposes to {@code 0 x N}, a conversion or element-wise map of a {@code 0 x N} matrix is {@code 0 x N},
 * an empty row slice keeps the column count, and a {@code 0 x N} matrix yields {@code N} empty column
 * streams. Only a result that is genuinely {@code 0 x 0} is canonicalized to the shared empty instance.</p>
 *
 * <p>Because a bare {@code T[][]} loses this information, a zero-row array obtained from
 * {@link com.landawn.abacus.matrix.Matrices#newMatrixArray(int, int, Class)} or from
 * {@link com.landawn.abacus.matrix.AbstractMatrix#unsafeBackingArray()} must be paired with the column
 * count by the caller; the {@code wrap(...)} factories, which take only an array, therefore report
 * {@code 0 x 0} for a zero-row input.</p>
 *
 * <h2>Storage ownership and mutation</h2>
 *
 * <p>Public constructors take a private snapshot of the <i>outer</i> array and validate its rows,
 * while sharing the row arrays themselves. Writing a cell through the caller's row is therefore visible through
 * the matrix and vice versa, but replacing a whole row in either outer array is not.
 * {@link com.landawn.abacus.matrix.Matrix#wrap(Class, Object[][])} does the same, as do the primitive
 * {@code wrap(...)} factories when the input has at least one row. A primitive {@code wrap(...)} factory
 * canonicalizes a zero-row input to its shared {@code 0 x 0} singleton, so the caller's empty outer-array
 * identity is not retained. Primitive {@code copyOf(...)} factories do the same for zero-row inputs and copy
 * every row of non-empty inputs. {@link com.landawn.abacus.matrix.Matrix#copyOf(Class, Object[][])} always creates an
 * independent outer array and independently copies every row, including a new outer array for a zero-row input.
 * Use {@code copyOf(...)} to copy an input array or {@link com.landawn.abacus.matrix.AbstractMatrix#copy()} to copy a matrix. For reference matrices,
 * these operations copy the array structure but not the referenced element objects.</p>
 *
 * <p>Rows must be rectangular <i>and</i> identity-distinct: no two logical rows may be the same array
 * object, because one physical row cannot represent two independently addressable logical rows. Construction
 * rejects a repeat with {@code IllegalArgumentException}, so {@code wrap(row, row)} throws while
 * {@code copyOf(row, row)} succeeds (it copies each row independently). Every logical coordinate therefore names
 * exactly one storage cell, and a value transformation visits each cell exactly once on successful completion,
 * whether it runs sequentially or in parallel.</p>
 *
 * <p>A caller-supplied array may still contain one of a matrix's live rows -- {@code m.setColumn(0, m.rowView(1))}
 * is legal -- so the writers that need it snapshot such a source before writing.</p>
 *
 * <p>{@link com.landawn.abacus.matrix.AbstractMatrix#unsafeBackingArray()} and methods whose names end
 * in {@code View}, such as {@link com.landawn.abacus.matrix.AbstractMatrix#rowView(int)}, expose live
 * storage. Methods whose names end in {@code Copy}, together with
 * {@link com.landawn.abacus.matrix.AbstractMatrix#flatten()}, return independent containers.</p>
 *
 * <p>On a matrix instance, methods named {@code set*}, {@code update*}, {@code fill}, {@code replaceIf},
 * {@code copyFrom}, or ending in {@code InPlace} modify the receiver. The specialized {@code mutateViaFlatArray} operation lets
 * its action modify the matrix through a temporary flattened array. Shape transformations whose names do not
 * end in {@code InPlace}, arithmetic operations, {@code map}, {@code zipWith}, and {@code copy} leave the
 * receiver unchanged and return a separate matrix instead, except that a degenerate empty result may be the
 * shared empty instance of the returned matrix type. Matrix instances are mutable and are not thread-safe.</p>
 *
 * <h2>Null handling</h2>
 *
 * <p>The public matrix operations reject a {@code null} argument they do not accept with {@code IllegalArgumentException}
 * rather than {@code NullPointerException}. The same applies to a {@code null} element of an array or collection
 * argument that must be null-free, such as a {@code null} row passed to a constructor or factory, or a
 * {@code null} matrix in a collection passed to the {@link com.landawn.abacus.matrix.Matrices} stacking and
 * zipping helpers.</p>
 *
 * <p>Some parameters deliberately accept {@code null}, and their documentation says so. {@code equals(null)}
 * returns {@code false}. {@code ofDiagonals(...)} accepts a {@code null} for one of its two diagonals.
 * {@code copyFrom(...)} skips {@code null} rows of its source.
 * {@link com.landawn.abacus.matrix.Matrices#isSameShape(java.util.Collection)} returns {@code true} for a
 * {@code null} collection and {@code false} when the collection contains a {@code null} element. A
 * {@link com.landawn.abacus.matrix.Matrix} stores {@code null} as an ordinary element value.</p>
 *
 * <p>{@code NullPointerException} is thrown when a caller-supplied function returns {@code null} and the
 * result has to be unboxed to a primitive. This applies to the primitive {@code updateAll(...)} overloads
 * whose mapper returns a boxed value and to {@code Matrices.zipToInt}, {@code zipToLong}, and
 * {@code zipToDouble}. Each of these methods documents the exception. The element iterators of the
 * object-valued streams also follow the inherited {@code toArray(A[])} contract and throw
 * {@code NullPointerException} for a {@code null} target array. Enum methods such as
 * {@link com.landawn.abacus.matrix.ParallelMode#valueOf(String)} retain their standard Java null handling.
 * A {@code NullPointerException} thrown by a caller-supplied callback or output destination is not
 * converted into an argument-validation exception.</p>
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
 * IntMatrix wrapped = IntMatrix.wrap(source);      // shares source
 * IntMatrix owned = IntMatrix.copyOf(source);    // owns its row arrays
 * source[0][0] = 10;
 *
 * wrapped.get(0, 0); // 10
 * owned.get(0, 0);   // 1
 *
 * IntMatrix right = IntMatrix.wrap(new int[][] {
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
